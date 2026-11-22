package com.example.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.BuildConfig
import com.example.data.model.deriveVideoPosterUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

enum class CloudinaryFolder(val path: String, val maxDimension: Int) {
  PRODUCTS("aleman/products", 1200),
  CATEGORIES("aleman/categories", 800),
  RECEIPTS("aleman/receipts", 1080),
  BANNERS("aleman/banners", 1600),
  VIDEOS("aleman/videos", 0),
}

data class CloudinaryUploadResult(
  val secureUrl: String,
  val publicId: String,
  val bytes: Long,
  val deleteToken: String? = null,
  val durationSeconds: Int? = null,
  val posterUrl: String? = null,
)

class CloudinaryUploadService(
  private val cloudName: String = BuildConfig.CLOUDINARY_CLOUD_NAME,
  private val uploadPreset: String = BuildConfig.CLOUDINARY_UPLOAD_PRESET,
) {
  private val json = Json { ignoreUnknownKeys = true }

  private val client = OkHttpClient.Builder()
    .connectTimeout(25, TimeUnit.SECONDS)
    .writeTimeout(45, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .build()

  private val videoClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(120, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .build()

  fun isConfigured(): Boolean {
    val cleanCloud = cloudName.trim()
    val cleanPreset = uploadPreset.trim()
    return cleanCloud.isNotEmpty() &&
      cleanPreset.isNotEmpty() &&
      !cleanCloud.startsWith("your_", ignoreCase = true) &&
      !cleanPreset.startsWith("your_", ignoreCase = true)
  }

  suspend fun processAndUploadUri(
    context: Context,
    uri: Uri,
    folder: CloudinaryFolder,
  ): Result<CloudinaryUploadResult> {
    if (!isConfigured()) {
      return Result.failure(IllegalStateException(MISSING_CONFIG_ARABIC))
    }
    val payload = ImageProcessor.processUriDirectly(
      context = context,
      uri = uri,
      maxDimension = folder.maxDimension,
    ) ?: return Result.failure(IllegalArgumentException("تعذر قراءة الصورة المختارة أو معالجتها."))
    return uploadProcessedImage(payload = payload, folder = folder)
  }

  /**
   * Uploads an animated image (GIF / Animated WebP) directly as raw bytes without Bitmap
   * re-encoding so all animation frames are preserved intact.
   */
  suspend fun uploadAnimatedImageUri(
    context: Context,
    uri: Uri,
    folder: CloudinaryFolder = CloudinaryFolder.BANNERS,
  ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(IllegalStateException(MISSING_CONFIG_ARABIC))
    }

    val rawBytes = try {
      readBytesWithLimit(
        context = context,
        uri = uri,
        maxBytes = MAX_ANIMATED_IMAGE_BYTES,
        tooLargeMessage = "حجم الصورة المتحركة كبير جدًا (الحد الأقصى ١٥ ميجابايت). اختر ملف GIF أو WebP أصغر.",
      )
    } catch (e: IllegalArgumentException) {
      return@withContext Result.failure(e)
    } catch (e: Exception) {
      return@withContext Result.failure(
        IllegalArgumentException("تعذر قراءة الصورة المتحركة المختارة (${e.localizedMessage ?: "خطأ"}).")
      )
    }

    if (rawBytes.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("ملف الصورة المتحركة فارغ أو غير صالح."))
    }

    val detectedMime = detectAnimatedMimeType(
      contentResolverMime = context.contentResolver.getType(uri),
      bytes = rawBytes,
    )
    val extension = when (detectedMime) {
      "image/gif" -> "gif"
      "image/webp" -> "webp"
      "image/png" -> "png"
      else -> "gif"
    }

    val payload = ProcessedImagePayload(
      bytes = rawBytes,
      mimeType = detectedMime,
      fileExtension = extension,
      width = 0,
      height = 0,
    )
    return@withContext uploadProcessedImage(payload = payload, folder = folder)
  }

  private fun detectAnimatedMimeType(contentResolverMime: String?, bytes: ByteArray): String {
    if (bytes.size >= 6) {
      val header6 = String(bytes, 0, 6, Charsets.US_ASCII)
      if (header6 == "GIF87a" || header6 == "GIF89a") return "image/gif"
    }
    if (bytes.size >= 12) {
      val riff = String(bytes, 0, 4, Charsets.US_ASCII)
      val webp = String(bytes, 8, 4, Charsets.US_ASCII)
      if (riff == "RIFF" && webp == "WEBP") return "image/webp"
    }
    val lower = contentResolverMime?.lowercase().orEmpty()
    return when {
      lower.contains("gif") -> "image/gif"
      lower.contains("webp") -> "image/webp"
      lower.contains("png") -> "image/png"
      else -> "image/gif"
    }
  }

  suspend fun uploadVideoUri(
    context: Context,
    uri: Uri,
    folder: CloudinaryFolder = CloudinaryFolder.VIDEOS,
  ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(IllegalStateException(MISSING_CONFIG_ARABIC))
    }

    val localDurationSec = extractLocalVideoDurationSeconds(context, uri)
    val mimeType = context.contentResolver.getType(uri)?.takeIf { it.startsWith("video/") } ?: "video/mp4"
    val extension = when {
      mimeType.contains("webm", ignoreCase = true) -> "webm"
      mimeType.contains("quicktime", ignoreCase = true) -> "mov"
      else -> "mp4"
    }

    val videoBytes = try {
      readBytesWithLimit(context, uri, MAX_VIDEO_BYTES)
    } catch (e: IllegalArgumentException) {
      return@withContext Result.failure(e)
    } catch (e: Exception) {
      return@withContext Result.failure(
        IllegalArgumentException("تعذر قراءة ملف الفيديو المختار (${e.localizedMessage ?: "خطأ"}).")
      )
    }

    if (videoBytes.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("ملف الفيديو فارغ أو غير صالح."))
    }

    val cleanCloud = cloudName.trim()
    val cleanPreset = uploadPreset.trim()
    val endpoint = "https://api.cloudinary.com/v1_1/$cleanCloud/video/upload"
    val fileName = "aleman_vid_${System.currentTimeMillis()}.$extension"

    return@withContext try {
      val fileBody = videoBytes.toRequestBody(mimeType.toMediaType())
      val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", fileName, fileBody)
        .addFormDataPart("upload_preset", cleanPreset)
        .addFormDataPart("folder", folder.path)
        .build()

      val request = Request.Builder()
        .url(endpoint)
        .post(requestBody)
        .build()

      videoClient.newCall(request).execute().use { response ->
        val rawBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          val apiMsg = runCatching {
            json.parseToJsonElement(rawBody).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
          }.getOrNull()
          val friendly = when {
            apiMsg?.contains("preset", ignoreCase = true) == true ->
              "إعداد Upload Preset في Cloudinary غير صحيح أو لا يدعم رفع الفيديو."
            apiMsg?.contains("size", ignoreCase = true) == true ->
              "حجم الفيديو أكبر من الحد المسموح به على الخادم. اختر فيديو قصيرًا بحجم أصغر."
            !apiMsg.isNullOrBlank() -> "فشل رفع الفيديو: $apiMsg"
            else -> "فشل رفع الفيديو على الخادم (رمز ${response.code})."
          }
          return@withContext Result.failure(IllegalStateException(friendly))
        }

        val obj = json.parseToJsonElement(rawBody).jsonObject
        val secureUrl = obj["secure_url"]?.jsonPrimitive?.content.orEmpty()
        val publicId = obj["public_id"]?.jsonPrimitive?.content.orEmpty()
        val bytes = obj["bytes"]?.jsonPrimitive?.longOrNull ?: videoBytes.size.toLong()
        val deleteToken = obj["delete_token"]?.jsonPrimitive?.content
        val cloudDuration = obj["duration"]?.jsonPrimitive?.doubleOrNull
          ?.takeIf { it > 0.0 }
          ?.let { ceil(it).toInt().coerceIn(1, 120) }

        if (secureUrl.isBlank()) {
          return@withContext Result.failure(IllegalStateException("لم يُرجِع الخادم رابط الفيديو المرفوع."))
        }

        val resolvedDuration = cloudDuration ?: localDurationSec
        val posterUrl = deriveVideoPosterUrl(secureUrl).ifBlank { null }

        Result.success(
          CloudinaryUploadResult(
            secureUrl = secureUrl,
            publicId = publicId,
            bytes = bytes,
            deleteToken = deleteToken,
            durationSeconds = resolvedDuration,
            posterUrl = posterUrl,
          ),
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(
        IllegalStateException(
          "تعذر الاتصال بخدمة رفع الفيديو، تحقق من الإنترنت (${e.localizedMessage ?: "خطأ شبكة"}).",
        ),
      )
    }
  }

  private fun extractLocalVideoDurationSeconds(context: Context, uri: Uri): Int? {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
      if (durationMs != null && durationMs > 0L) {
        ceil(durationMs / 1000.0).toInt().coerceIn(1, 120)
      } else {
        null
      }
    } catch (_: Exception) {
      null
    } finally {
      runCatching { retriever.release() }
    }
  }

  private fun readBytesWithLimit(
    context: Context,
    uri: Uri,
    maxBytes: Long,
    tooLargeMessage: String = "حجم الفيديو كبير جدًا (الحد الأقصى ٤٠ ميجابايت). اختر فيديو قصيرًا ومضغوطًا.",
  ): ByteArray {
    val input = context.contentResolver.openInputStream(uri)
      ?: throw IllegalArgumentException("تعذر فتح الملف المختار.")
    input.use { stream ->
      val out = ByteArrayOutputStream()
      val buffer = ByteArray(16 * 1024)
      var total = 0L
      while (true) {
        val read = stream.read(buffer)
        if (read == -1) break
        total += read
        if (total > maxBytes) {
          throw IllegalArgumentException(tooLargeMessage)
        }
        out.write(buffer, 0, read)
      }
      return out.toByteArray()
    }
  }

  suspend fun uploadProcessedImage(
    payload: ProcessedImagePayload,
    folder: CloudinaryFolder,
  ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext Result.failure(IllegalStateException(MISSING_CONFIG_ARABIC))
    }

    val cleanCloud = cloudName.trim()
    val cleanPreset = uploadPreset.trim()
    val endpoint = "https://api.cloudinary.com/v1_1/$cleanCloud/image/upload"
    val fileName = "aleman_${System.currentTimeMillis()}.${payload.fileExtension}"

    return@withContext try {
      val fileBody = payload.bytes.toRequestBody(payload.mimeType.toMediaType())
      val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", fileName, fileBody)
        .addFormDataPart("upload_preset", cleanPreset)
        .addFormDataPart("folder", folder.path)
        .build()

      val request = Request.Builder()
        .url(endpoint)
        .post(requestBody)
        .build()

      client.newCall(request).execute().use { response ->
        val rawBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          val apiMsg = runCatching {
            json.parseToJsonElement(rawBody).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
          }.getOrNull()
          val friendly = when {
            apiMsg?.contains("preset", ignoreCase = true) == true ->
              "إعداد Upload Preset في Cloudinary غير صحيح أو ليس Unsigned."
            apiMsg?.contains("cloud_name", ignoreCase = true) == true ->
              "اسم السحابة CLOUDINARY_CLOUD_NAME غير صحيح."
            !apiMsg.isNullOrBlank() -> "فشل رفع الصورة: $apiMsg"
            else -> "فشل رفع الصورة على الخادم (رمز ${response.code})."
          }
          return@withContext Result.failure(IllegalStateException(friendly))
        }

        val obj = json.parseToJsonElement(rawBody).jsonObject
        val secureUrl = obj["secure_url"]?.jsonPrimitive?.content.orEmpty()
        val publicId = obj["public_id"]?.jsonPrimitive?.content.orEmpty()
        val bytes = obj["bytes"]?.jsonPrimitive?.longOrNull ?: payload.bytes.size.toLong()
        val deleteToken = obj["delete_token"]?.jsonPrimitive?.content

        if (secureUrl.isBlank()) {
          return@withContext Result.failure(IllegalStateException("لم يُرجِع الخادم رابط الصورة المرفوعة."))
        }

        Result.success(
          CloudinaryUploadResult(
            secureUrl = secureUrl,
            publicId = publicId,
            bytes = bytes,
            deleteToken = deleteToken,
          ),
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(
        IllegalStateException(
          "تعذر الاتصال بخدمة رفع الصور، تحقق من الإنترنت (${e.localizedMessage ?: "خطأ شبكة"}).",
        ),
      )
    }
  }

  companion object {
    const val MAX_VIDEO_BYTES = 40L * 1024L * 1024L
    const val MAX_ANIMATED_IMAGE_BYTES = 15L * 1024L * 1024L
    const val MISSING_CONFIG_ARABIC =
      "إعدادات Cloudinary غير مكتملة. أضف CLOUDINARY_CLOUD_NAME وCLOUDINARY_UPLOAD_PRESET (Unsigned) من لوحة Secrets في AI Studio."
  }
}
