package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

enum class CropAspectPreset(val arabicLabel: String, val ratio: Float?) {
  FULL("كامل الصورة", null),
  SQUARE("مربع 1:1", 1f),
  RATIO_4_3("4:3", 4f / 3f),
  FREE("قص حر", null),
}

data class NormalizedCropRect(
  val left: Float = 0f,
  val top: Float = 0f,
  val right: Float = 1f,
  val bottom: Float = 1f,
) {
  fun normalized(): NormalizedCropRect {
    val l = left.coerceIn(0f, 0.95f)
    val t = top.coerceIn(0f, 0.95f)
    val r = right.coerceIn((l + 0.05f).coerceAtMost(1f), 1f)
    val b = bottom.coerceIn((t + 0.05f).coerceAtMost(1f), 1f)
    return NormalizedCropRect(l, t, r, b)
  }
}

class ProcessedImagePayload(
  val bytes: ByteArray,
  val width: Int,
  val height: Int,
  val mimeType: String,
  val fileExtension: String,
) {
  val sizeKb: Int
    get() = max(1, (bytes.size / 1024f).roundToInt())
}

object ImageProcessor {

  fun createCameraTempUri(context: Context): Pair<File, Uri> {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val imageFile = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      imageFile,
    )
    return imageFile to uri
  }

  suspend fun loadPreviewBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1280,
  ): Bitmap? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver

    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOptions)
    }
    val rawW = boundsOptions.outWidth
    val rawH = boundsOptions.outHeight
    if (rawW <= 0 || rawH <= 0) return@withContext null

    var sampleSize = 1
    while ((rawW / sampleSize) > maxDimension * 2 || (rawH / sampleSize) > maxDimension * 2) {
      sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = resolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, decodeOptions)
    } ?: return@withContext null

    val exifRotation = readExifRotation(context, uri)
    val oriented = if (exifRotation != 0) {
      rotateBitmap(decoded, exifRotation).also {
        if (it !== decoded) decoded.recycle()
      }
    } else {
      decoded
    }

    scaleDownIfNeeded(oriented, maxDimension)
  }

  suspend fun transformAndCompress(
    sourceBitmap: Bitmap,
    rotationDegrees: Int = 0,
    cropRect: NormalizedCropRect = NormalizedCropRect(),
    maxDimension: Int = 1200,
    quality: Int = 82,
  ): ProcessedImagePayload = withContext(Dispatchers.Default) {
    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
    val rotated = if (normalizedRotation != 0) {
      rotateBitmap(sourceBitmap, normalizedRotation)
    } else {
      sourceBitmap
    }

    val safeCrop = cropRect.normalized()
    val startX = (safeCrop.left * rotated.width).roundToInt().coerceIn(0, (rotated.width - 1).coerceAtLeast(0))
    val startY = (safeCrop.top * rotated.height).roundToInt().coerceIn(0, (rotated.height - 1).coerceAtLeast(0))
    val cropW = ((safeCrop.right - safeCrop.left) * rotated.width).roundToInt()
      .coerceIn(1, (rotated.width - startX).coerceAtLeast(1))
    val cropH = ((safeCrop.bottom - safeCrop.top) * rotated.height).roundToInt()
      .coerceIn(1, (rotated.height - startY).coerceAtLeast(1))

    val cropped = if (startX == 0 && startY == 0 && cropW == rotated.width && cropH == rotated.height) {
      rotated
    } else {
      Bitmap.createBitmap(rotated, startX, startY, cropW, cropH)
    }

    val scaled = scaleDownIfNeeded(cropped, maxDimension)

    val out = ByteArrayOutputStream()
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Bitmap.CompressFormat.WEBP_LOSSY
    } else {
      @Suppress("DEPRECATION")
      Bitmap.CompressFormat.WEBP
    }
    scaled.compress(format, quality.coerceIn(50, 95), out)
    val bytes = out.toByteArray()

    val resultWidth = scaled.width
    val resultHeight = scaled.height

    if (scaled !== cropped && scaled !== rotated && scaled !== sourceBitmap) scaled.recycle()
    if (cropped !== rotated && cropped !== sourceBitmap) cropped.recycle()
    if (rotated !== sourceBitmap) rotated.recycle()

    ProcessedImagePayload(
      bytes = bytes,
      width = resultWidth,
      height = resultHeight,
      mimeType = "image/webp",
      fileExtension = "webp",
    )
  }

  suspend fun processUriDirectly(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1200,
    quality: Int = 82,
  ): ProcessedImagePayload? {
    val preview = loadPreviewBitmap(context, uri, maxDimension = maxDimension) ?: return null
    return try {
      transformAndCompress(
        sourceBitmap = preview,
        rotationDegrees = 0,
        cropRect = NormalizedCropRect(),
        maxDimension = maxDimension,
        quality = quality,
      )
    } finally {
      if (!preview.isRecycled) preview.recycle()
    }
  }

  fun computeCenteredAspectCrop(
    bitmapWidth: Int,
    bitmapHeight: Int,
    targetAspectRatio: Float,
  ): NormalizedCropRect {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || targetAspectRatio <= 0f) {
      return NormalizedCropRect()
    }
    val currentRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
    return if (currentRatio > targetAspectRatio) {
      val cropWidthFraction = (targetAspectRatio / currentRatio).coerceIn(0.15f, 1f)
      val margin = (1f - cropWidthFraction) / 2f
      NormalizedCropRect(left = margin, top = 0f, right = 1f - margin, bottom = 1f)
    } else {
      val cropHeightFraction = (currentRatio / targetAspectRatio).coerceIn(0.15f, 1f)
      val margin = (1f - cropHeightFraction) / 2f
      NormalizedCropRect(left = 0f, top = margin, right = 1f, bottom = 1f - margin)
    }
  }

  private fun readExifRotation(context: Context, uri: Uri): Int {
    return try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        val exif = ExifInterface(input)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90
          ExifInterface.ORIENTATION_ROTATE_180 -> 180
          ExifInterface.ORIENTATION_ROTATE_270 -> 270
          else -> 0
        }
      } ?: 0
    } catch (_: Exception) {
      0
    }
  }

  private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val maxSide = max(w, h)
    if (maxSide <= maxDimension || maxDimension <= 0) return bitmap
    val scale = maxDimension.toFloat() / maxSide.toFloat()
    val targetW = (w * scale).roundToInt().coerceAtLeast(1)
    val targetH = (h * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    if (scaled !== bitmap) bitmap.recycle()
    return scaled
  }
}
