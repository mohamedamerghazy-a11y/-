package com.example.data.media

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Persistent on-device disk cache for promotional banner videos and the post-splash intro reel.
 * - Saves each active video URL locally once (keyed by SHA-256 of the URL) so subsequent views
 *   play instantly from the device without consuming mobile data.
 * - Automatically prunes old cached video files whenever the store admin replaces or removes a video.
 */
object VideoDiskCache {

  private const val CACHE_DIR_NAME = "promo_videos_cache"
  private const val MIN_VALID_VIDEO_BYTES = 1024L
  private const val MAX_CACHE_VIDEO_BYTES = 50L * 1024L * 1024L

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val inFlight = ConcurrentHashMap<String, Deferred<File?>>()

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .followRedirects(true)
      .build()
  }

  fun cacheDirectory(context: Context): File {
    val dir = File(context.applicationContext.filesDir, CACHE_DIR_NAME)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  fun cacheFileNameForUrl(url: String): String {
    val normalized = url.trim()
    val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
    val hex = digest.joinToString("") { "%02x".format(it) }
    return "$hex.mp4"
  }

  fun getCachedFile(context: Context, url: String): File? {
    val clean = url.trim()
    if (clean.length < 5) return null
    val file = File(cacheDirectory(context), cacheFileNameForUrl(clean))
    return if (file.exists() && file.length() >= MIN_VALID_VIDEO_BYTES) file else null
  }

  /**
   * Returns the local cached file immediately if present, or downloads and saves it to device storage.
   * Deduplicates concurrent downloads for the same URL.
   */
  suspend fun ensureCached(context: Context, url: String): File? {
    val clean = url.trim()
    if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
      return null
    }
    getCachedFile(context, clean)?.let { return it }

    val appContext = context.applicationContext
    val deferred = inFlight.computeIfAbsent(clean) {
      scope.async {
        try {
          downloadToCache(appContext, clean)
        } finally {
          inFlight.remove(clean)
        }
      }
    }
    return try {
      deferred.await()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Synchronizes the local video cache with the currently active store video URLs:
   * 1. Deletes any previously cached video files that are no longer active (replaced or deleted by admin).
   * 2. Downloads any newly added video URLs in the background so they are ready locally on the phone.
   */
  suspend fun syncActiveVideos(context: Context, activeUrls: Collection<String>) = withContext(Dispatchers.IO) {
    val cleanUrls = activeUrls
      .map { it.trim() }
      .filter { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
      .distinct()

    val expectedFileNames = cleanUrls.mapTo(HashSet()) { cacheFileNameForUrl(it) }
    pruneStaleFiles(cacheDirectory(context), expectedFileNames)

    val appContext = context.applicationContext
    cleanUrls.forEach { url ->
      if (getCachedFile(appContext, url) == null) {
        runCatching { ensureCached(appContext, url) }
      }
    }
  }

  internal fun pruneStaleFiles(dir: File, expectedFileNames: Set<String>) {
    if (!dir.exists() || !dir.isDirectory) return
    val files = dir.listFiles() ?: return
    for (file in files) {
      if (!file.isFile) continue
      val name = file.name
      if (name.endsWith(".tmp")) {
        // Remove orphaned partial downloads older than 10 minutes
        val isStaleTmp = (System.currentTimeMillis() - file.lastModified()) > 10L * 60L * 1000L
        if (isStaleTmp) runCatching { file.delete() }
        continue
      }
      if (name !in expectedFileNames) {
        runCatching { file.delete() }
      }
    }
  }

  private fun downloadToCache(context: Context, url: String): File? {
    val dir = cacheDirectory(context)
    val finalFile = File(dir, cacheFileNameForUrl(url))
    if (finalFile.exists() && finalFile.length() >= MIN_VALID_VIDEO_BYTES) {
      return finalFile
    }

    val tmpFile = File(dir, "${finalFile.name}.${System.currentTimeMillis()}.tmp")
    return try {
      val request = Request.Builder().url(url).get().build()
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body ?: return null
        val declaredLen = body.contentLength()
        if (declaredLen > MAX_CACHE_VIDEO_BYTES) return null

        var written = 0L
        body.byteStream().use { input ->
          FileOutputStream(tmpFile).use { output ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
              val read = input.read(buffer)
              if (read == -1) break
              written += read
              if (written > MAX_CACHE_VIDEO_BYTES) {
                runCatching { tmpFile.delete() }
                return null
              }
              output.write(buffer, 0, read)
            }
            output.flush()
          }
        }

        if (written < MIN_VALID_VIDEO_BYTES) {
          runCatching { tmpFile.delete() }
          return null
        }

        if (finalFile.exists()) {
          runCatching { finalFile.delete() }
        }
        if (tmpFile.renameTo(finalFile)) {
          finalFile
        } else {
          // Fallback copy if rename across filesystem fails
          tmpFile.copyTo(finalFile, overwrite = true)
          runCatching { tmpFile.delete() }
          finalFile
        }
      }
    } catch (_: Exception) {
      runCatching { tmpFile.delete() }
      null
    }
  }
}
