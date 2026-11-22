package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import com.example.data.media.CloudinaryFolder
import com.example.data.media.CloudinaryUploadService
import com.example.data.media.CropAspectPreset
import com.example.data.media.ImageProcessor
import com.example.data.media.NormalizedCropRect
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * عنصر تحكم متكامل لاختيار صورة (أو عدة صور للمنتجات) من المعرض أو الكاميرا،
 * مع محرر القص والتدوير والضغط التلقائي والرفع إلى Cloudinary.
 */
@Composable
fun ImagePickerUploadModal(
  visible: Boolean,
  title: String,
  folder: CloudinaryFolder,
  allowMultiple: Boolean = false,
  maxMultipleCount: Int = 10,
  onUploadedUrls: (List<String>) -> Unit,
  onError: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  if (!visible) return

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uploader = remember { CloudinaryUploadService() }

  var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
  var editingQueueIndex by remember { mutableStateOf<Int?>(null) }
  var uploadedBatchUrls by remember { mutableStateOf<List<String>>(emptyList()) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var isBatchUploading by remember { mutableStateOf(false) }
  var batchProgressText by remember { mutableStateOf("") }

  val singlePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      pendingUris = listOf(uri)
      uploadedBatchUrls = emptyList()
      editingQueueIndex = 0
    } else {
      onDismiss()
    }
  }

  val multiPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxMultipleCount.coerceIn(2, 10)),
  ) { uris ->
    if (uris.isEmpty()) {
      onDismiss()
    } else if (uris.size == 1) {
      pendingUris = uris
      uploadedBatchUrls = emptyList()
      editingQueueIndex = 0
    } else {
      pendingUris = uris
      uploadedBatchUrls = emptyList()
      editingQueueIndex = null
    }
  }

  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    val captured = pendingCameraUri
    if (success && captured != null) {
      pendingUris = listOf(captured)
      uploadedBatchUrls = emptyList()
      editingQueueIndex = 0
    } else {
      onDismiss()
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      runCatching { ImageProcessor.createCameraTempUri(context).second }
        .onSuccess { uri ->
          pendingCameraUri = uri
          takePictureLauncher.launch(uri)
        }
        .onFailure {
          onError("تعذر تجهيز ملف الكاميرا المؤقت.")
          onDismiss()
        }
    } else {
      onError("يرجى السماح بصلاحية الكاميرا لالتقاط صورة، أو اختر من المعرض.")
      onDismiss()
    }
  }

  fun launchCameraCapture() {
    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED
    if (hasPerm) {
      runCatching { ImageProcessor.createCameraTempUri(context).second }
        .onSuccess { uri ->
          pendingCameraUri = uri
          takePictureLauncher.launch(uri)
        }
        .onFailure {
          onError("تعذر تجهيز الكاميرا حاليًا.")
          onDismiss()
        }
    } else {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  val activeEditIdx = editingQueueIndex
  if (activeEditIdx != null && activeEditIdx in pendingUris.indices) {
    val currentUri = pendingUris[activeEditIdx]
    ImageCropRotateDialog(
      imageUri = currentUri,
      folder = folder,
      queueLabel = if (pendingUris.size > 1) "صورة ${activeEditIdx + 1} من ${pendingUris.size}" else null,
      uploader = uploader,
      onUploaded = { uploadedUrl ->
        val accumulated = uploadedBatchUrls + uploadedUrl
        uploadedBatchUrls = accumulated
        val nextIdx = activeEditIdx + 1
        if (nextIdx < pendingUris.size) {
          editingQueueIndex = nextIdx
        } else {
          editingQueueIndex = null
          pendingUris = emptyList()
          onUploadedUrls(accumulated)
        }
      },
      onSkipOrCancel = {
        val nextIdx = activeEditIdx + 1
        if (pendingUris.size > 1 && nextIdx < pendingUris.size) {
          editingQueueIndex = nextIdx
        } else {
          val partial = uploadedBatchUrls
          editingQueueIndex = null
          pendingUris = emptyList()
          if (partial.isNotEmpty()) {
            onUploadedUrls(partial)
          } else {
            onDismiss()
          }
        }
      },
    )
    return
  }

  if (pendingUris.size > 1) {
    AlertDialog(
      onDismissRequest = { if (!isBatchUploading) onDismiss() },
      containerColor = PureWhite,
      title = {
        Text(
          text = "تم اختيار ${pendingUris.size} صور",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "يمكنك ضغط ورفع جميع الصور مباشرة إلى السحابة، أو قص وتدوير كل صورة قبل الرفع.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
          ) {
            itemsIndexed(pendingUris) { idx, uri ->
              Box(
                modifier = Modifier
                  .size(72.dp)
                  .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
                  .background(GeneralBackground),
              ) {
                SubcomposeAsyncImage(
                  model = uri,
                  contentDescription = "صورة ${idx + 1}",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
          }
          if (isBatchUploading) {
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth(),
              color = PrimaryYellow,
              trackColor = BorderNeutral,
            )
            Text(
              text = batchProgressText,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = CharcoalBlack,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (!uploader.isConfigured()) {
              onError(CloudinaryUploadService.MISSING_CONFIG_ARABIC)
              onDismiss()
              return@Button
            }
            scope.launch {
              isBatchUploading = true
              val successes = mutableListOf<String>()
              var firstError: String? = null
              pendingUris.forEachIndexed { idx, uri ->
                batchProgressText = "جاري ضغط ورفع صورة ${idx + 1} من ${pendingUris.size}..."
                uploader.processAndUploadUri(context, uri, folder)
                  .onSuccess { res -> successes.add(res.secureUrl) }
                  .onFailure { err ->
                    if (firstError == null) firstError = err.localizedMessage
                  }
              }
              isBatchUploading = false
              pendingUris = emptyList()
              if (successes.isNotEmpty()) {
                if (firstError != null) {
                  onError("تم رفع ${successes.size} صورة، وتعذر رفع البقية: $firstError")
                }
                onUploadedUrls(successes)
              } else {
                onError(firstError ?: "تعذر رفع الصور المختارة.")
                onDismiss()
              }
            }
          },
          enabled = !isBatchUploading,
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
          modifier = Modifier.testTag("batch_auto_upload_button"),
        ) {
          Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("ضغط ورفع الكل", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(
            onClick = { editingQueueIndex = 0 },
            enabled = !isBatchUploading,
            modifier = Modifier.testTag("batch_edit_each_button"),
          ) {
            Text("قص/تعديل كل صورة", color = CharcoalBlack, fontWeight = FontWeight.SemiBold)
          }
          TextButton(
            onClick = {
              pendingUris = emptyList()
              onDismiss()
            },
            enabled = !isBatchUploading,
          ) {
            Text("إلغاء", color = TextSecondary)
          }
        }
      },
    )
    return
  }

  // حوار اختيار مصدر الصورة (المعرض / الكاميرا)
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!uploader.isConfigured()) {
          Surface(
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            color = PrimaryYellowSoft,
            border = BorderStroke(1.dp, PrimaryYellow),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "تنبيه الإعداد: تأكد من إضافة CLOUDINARY_CLOUD_NAME وCLOUDINARY_UPLOAD_PRESET (Unsigned) في لوحة Secrets قبل الرفع.",
              style = MaterialTheme.typography.bodySmall,
              color = CharcoalBlack,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        Surface(
          onClick = {
            if (allowMultiple && maxMultipleCount > 1) {
              multiPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
              )
            } else {
              singlePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
              )
            }
          },
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          color = PrimaryYellowSoft,
          border = BorderStroke(1.dp, PrimaryYellow),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pick_image_gallery_option"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = PrimaryYellow,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = CharcoalBlack)
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (allowMultiple && maxMultipleCount > 1) {
                  "اختيار صورة أو عدة صور من المعرض"
                } else {
                  "اختيار صورة من المعرض"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
              Text(
                text = if (allowMultiple && maxMultipleCount > 1) {
                  "بدون أي صلاحيات تخزين إضافية (حتى $maxMultipleCount صور)"
                } else {
                  "اختيار آمن عبر منتقي الصور الرسمي"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            }
          }
        }

        Surface(
          onClick = { launchCameraCapture() },
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pick_image_camera_option"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = CharcoalBlack,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = PrimaryYellow)
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "التقاط صورة بالكاميرا",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
              Text(
                text = "تصوير مباشر مع إمكانية القص والتدوير قبل الرفع",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}

/**
 * محرر القص والتدوير والضغط التلقائي لصورة قبل رفعها إلى Cloudinary.
 */
@Composable
fun ImageCropRotateDialog(
  imageUri: Uri,
  folder: CloudinaryFolder,
  uploader: CloudinaryUploadService,
  queueLabel: String? = null,
  onUploaded: (String) -> Unit,
  onSkipOrCancel: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var sourceBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
  var isLoadingBitmap by remember(imageUri) { mutableStateOf(true) }
  var loadError by remember(imageUri) { mutableStateOf<String?>(null) }
  var rotationDegrees by remember(imageUri) { mutableIntStateOf(0) }
  var selectedPreset by remember(imageUri) { mutableStateOf(CropAspectPreset.FULL) }
  var cropRect by remember(imageUri) { mutableStateOf(NormalizedCropRect()) }
  var isUploading by remember { mutableStateOf(false) }
  var uploadError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(imageUri) {
    isLoadingBitmap = true
    loadError = null
    val bmp = ImageProcessor.loadPreviewBitmap(context, imageUri, maxDimension = 1280)
    if (bmp == null) {
      loadError = "تعذر فتح الصورة المختارة."
    } else {
      sourceBitmap = bmp
    }
    isLoadingBitmap = false
  }

  val rotatedBitmap = remember(sourceBitmap, rotationDegrees) {
    val base = sourceBitmap ?: return@remember null
    val norm = ((rotationDegrees % 360) + 360) % 360
    if (norm == 0) {
      base
    } else {
      val matrix = Matrix().apply { postRotate(norm.toFloat()) }
      Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
    }
  }

  fun applyPreset(preset: CropAspectPreset) {
    selectedPreset = preset
    val bmp = rotatedBitmap ?: return
    cropRect = when (preset) {
      CropAspectPreset.FULL -> NormalizedCropRect(0f, 0f, 1f, 1f)
      CropAspectPreset.SQUARE -> ImageProcessor.computeCenteredAspectCrop(bmp.width, bmp.height, 1f)
      CropAspectPreset.RATIO_4_3 -> ImageProcessor.computeCenteredAspectCrop(bmp.width, bmp.height, 4f / 3f)
      CropAspectPreset.FREE -> if (cropRect == NormalizedCropRect()) {
        NormalizedCropRect(0.08f, 0.08f, 0.92f, 0.92f)
      } else {
        cropRect
      }
    }
  }

  Dialog(
    onDismissRequest = { if (!isUploading) onSkipOrCancel() },
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Scaffold(
      containerColor = CharcoalBlack,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        Surface(color = CharcoalBlack) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            IconButton(
              onClick = { if (!isUploading) onSkipOrCancel() },
              enabled = !isUploading,
              modifier = Modifier.testTag("image_editor_close_button"),
            ) {
              Icon(Icons.Filled.Close, contentDescription = "إغلاق المحرر", tint = PureWhite)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "تحرير وضغط الصورة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite,
              )
              if (queueLabel != null) {
                Text(
                  text = queueLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = PrimaryYellow,
                )
              }
            }
            IconButton(
              onClick = {
                rotationDegrees = 0
                selectedPreset = CropAspectPreset.FULL
                cropRect = NormalizedCropRect()
                uploadError = null
              },
              enabled = !isUploading,
              modifier = Modifier.testTag("image_editor_reset_button"),
            ) {
              Icon(Icons.Filled.RestartAlt, contentDescription = "إعادة ضبط", tint = PrimaryYellow)
            }
          }
        }
      },
      bottomBar = {
        Surface(color = PureWhite, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (!uploadError.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                color = DiscountRedSoft,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = uploadError.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = DiscountRed,
                  modifier = Modifier.padding(10.dp),
                )
              }
            }

            // أزرار التدوير ونسب القص
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = {
                  rotationDegrees = (rotationDegrees - 90) % 360
                  if (selectedPreset != CropAspectPreset.FREE) applyPreset(selectedPreset)
                },
                enabled = !isUploading && rotatedBitmap != null,
                modifier = Modifier.weight(1f).testTag("editor_rotate_left_button"),
              ) {
                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تدوير يسار", color = CharcoalBlack, style = MaterialTheme.typography.labelMedium)
              }
              OutlinedButton(
                onClick = {
                  rotationDegrees = (rotationDegrees + 90) % 360
                  if (selectedPreset != CropAspectPreset.FREE) applyPreset(selectedPreset)
                },
                enabled = !isUploading && rotatedBitmap != null,
                modifier = Modifier.weight(1f).testTag("editor_rotate_right_button"),
              ) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تدوير يمين", color = CharcoalBlack, style = MaterialTheme.typography.labelMedium)
              }
            }

            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              itemsIndexed(CropAspectPreset.entries) { _, preset ->
                val active = selectedPreset == preset
                FilterChip(
                  selected = active,
                  onClick = { applyPreset(preset) },
                  enabled = !isUploading && rotatedBitmap != null,
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryYellow,
                    selectedLabelColor = CharcoalBlack,
                  ),
                  leadingIcon = if (active) {
                    { Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp), tint = CharcoalBlack) }
                  } else null,
                  label = {
                    Text(
                      text = preset.arabicLabel,
                      fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                  },
                )
              }
            }

            Text(
              text = "اسحب زوايا الإطار الأصفر لتحديد جزء القص، وسيتم ضغط الصورة تلقائيًا بصيغة WebP لتوفير المساحة.",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )

            Button(
              onClick = {
                val bmp = sourceBitmap ?: return@Button
                scope.launch {
                  isUploading = true
                  uploadError = null
                  val payload = ImageProcessor.transformAndCompress(
                    sourceBitmap = bmp,
                    rotationDegrees = rotationDegrees,
                    cropRect = cropRect,
                    maxDimension = folder.maxDimension,
                    quality = 82,
                  )
                  uploader.uploadProcessedImage(payload, folder)
                    .onSuccess { res ->
                      isUploading = false
                      onUploaded(res.secureUrl)
                    }
                    .onFailure { err ->
                      isUploading = false
                      uploadError = err.localizedMessage ?: "تعذر رفع الصورة."
                    }
                }
              },
              enabled = !isUploading && sourceBitmap != null,
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("editor_upload_confirm_button"),
            ) {
              if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CharcoalBlack, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("جاري القص والضغط والرفع...", fontWeight = FontWeight.Bold)
              } else {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تطبيق وحفظ ورفع الصورة", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        when {
          isLoadingBitmap -> {
            CircularProgressIndicator(color = PrimaryYellow)
          }
          loadError != null || rotatedBitmap == null -> {
            StoreEmptyStateCard(
              icon = Icons.Filled.AddAPhoto,
              title = "تعذر تحميل الصورة",
              description = loadError ?: "صيغة الملف غير مدعومة.",
              actionLabel = "إغلاق",
              onActionClick = onSkipOrCancel,
            )
          }
          else -> {
            InteractiveCropPreview(
              bitmap = rotatedBitmap,
              cropRect = cropRect,
              onCropChange = { updated ->
                cropRect = updated
                if (selectedPreset != CropAspectPreset.FREE) {
                  selectedPreset = CropAspectPreset.FREE
                }
              },
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }
  }
}

private enum class DragHandle {
  TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE, NONE
}

@Composable
private fun InteractiveCropPreview(
  bitmap: Bitmap,
  cropRect: NormalizedCropRect,
  onCropChange: (NormalizedCropRect) -> Unit,
  modifier: Modifier = Modifier,
) {
  val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
  val density = LocalDensity.current
  val hitRadiusPx = with(density) { 28.dp.toPx() }
  val handleRadiusPx = with(density) { 8.dp.toPx() }

  var currentCrop by remember(cropRect) { mutableStateOf(cropRect) }

  BoxWithConstraints(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    val bmpRatio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
    val maxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val maxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
    val containerRatio = maxW / maxH

    val displaySize = if (bmpRatio > containerRatio) {
      IntSize(maxW.toInt(), (maxW / bmpRatio).toInt().coerceAtLeast(1))
    } else {
      IntSize((maxH * bmpRatio).toInt().coerceAtLeast(1), maxH.toInt())
    }

    val widthDp = with(density) { displaySize.width.toDp() }
    val heightDp = with(density) { displaySize.height.toDp() }

    Box(modifier = Modifier.size(widthDp, heightDp)) {
      Image(
        bitmap = imageBitmap,
        contentDescription = "معاينة الصورة للتعديل",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize(),
      )

      var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(displaySize) {
            detectDragGestures(
              onDragStart = { startOffset ->
                val w = size.width.toFloat().coerceAtLeast(1f)
                val h = size.height.toFloat().coerceAtLeast(1f)
                val l = currentCrop.left * w
                val t = currentCrop.top * h
                val r = currentCrop.right * w
                val b = currentCrop.bottom * h

                fun near(ax: Float, ay: Float): Boolean {
                  return abs(startOffset.x - ax) <= hitRadiusPx && abs(startOffset.y - ay) <= hitRadiusPx
                }

                activeHandle = when {
                  near(l, t) -> DragHandle.TOP_LEFT
                  near(r, t) -> DragHandle.TOP_RIGHT
                  near(l, b) -> DragHandle.BOTTOM_LEFT
                  near(r, b) -> DragHandle.BOTTOM_RIGHT
                  startOffset.x in l..r && startOffset.y in t..b -> DragHandle.MOVE
                  else -> DragHandle.NONE
                }
              },
              onDragEnd = {
                activeHandle = DragHandle.NONE
                onCropChange(currentCrop)
              },
              onDragCancel = {
                activeHandle = DragHandle.NONE
              },
              onDrag = { change, dragAmount ->
                change.consume()
                val w = size.width.toFloat().coerceAtLeast(1f)
                val h = size.height.toFloat().coerceAtLeast(1f)
                val dx = dragAmount.x / w
                val dy = dragAmount.y / h
                val minSpan = 0.06f

                val updated = when (activeHandle) {
                  DragHandle.TOP_LEFT -> currentCrop.copy(
                    left = (currentCrop.left + dx).coerceIn(0f, currentCrop.right - minSpan),
                    top = (currentCrop.top + dy).coerceIn(0f, currentCrop.bottom - minSpan),
                  )
                  DragHandle.TOP_RIGHT -> currentCrop.copy(
                    right = (currentCrop.right + dx).coerceIn(currentCrop.left + minSpan, 1f),
                    top = (currentCrop.top + dy).coerceIn(0f, currentCrop.bottom - minSpan),
                  )
                  DragHandle.BOTTOM_LEFT -> currentCrop.copy(
                    left = (currentCrop.left + dx).coerceIn(0f, currentCrop.right - minSpan),
                    bottom = (currentCrop.bottom + dy).coerceIn(currentCrop.top + minSpan, 1f),
                  )
                  DragHandle.BOTTOM_RIGHT -> currentCrop.copy(
                    right = (currentCrop.right + dx).coerceIn(currentCrop.left + minSpan, 1f),
                    bottom = (currentCrop.bottom + dy).coerceIn(currentCrop.top + minSpan, 1f),
                  )
                  DragHandle.MOVE -> {
                    val spanX = currentCrop.right - currentCrop.left
                    val spanY = currentCrop.bottom - currentCrop.top
                    val nextL = (currentCrop.left + dx).coerceIn(0f, 1f - spanX)
                    val nextT = (currentCrop.top + dy).coerceIn(0f, 1f - spanY)
                    NormalizedCropRect(nextL, nextT, nextL + spanX, nextT + spanY)
                  }
                  DragHandle.NONE -> currentCrop
                }
                currentCrop = updated
                onCropChange(updated)
              },
            )
          },
      ) {
        val w = size.width
        val h = size.height
        val l = currentCrop.left * w
        val t = currentCrop.top * h
        val r = currentCrop.right * w
        val b = currentCrop.bottom * h
        val dimColor = Color.Black.copy(alpha = 0.56f)

        // التعتيم حول منطقة القص
        if (t > 0f) drawRect(dimColor, Offset.Zero, Size(w, t))
        if (b < h) drawRect(dimColor, Offset(0f, b), Size(w, h - b))
        if (l > 0f) drawRect(dimColor, Offset(0f, t), Size(l, b - t))
        if (r < w) drawRect(dimColor, Offset(r, t), Size(w - r, b - t))

        // الإطار الأصفر
        drawRect(
          color = PrimaryYellow,
          topLeft = Offset(l, t),
          size = Size(r - l, b - t),
          style = Stroke(width = 3f),
        )

        // شبكة التثليث
        val thirdW = (r - l) / 3f
        val thirdH = (b - t) / 3f
        val gridColor = PureWhite.copy(alpha = 0.45f)
        for (i in 1..2) {
          drawLine(gridColor, Offset(l + thirdW * i, t), Offset(l + thirdW * i, b), strokeWidth = 1.2f)
          drawLine(gridColor, Offset(l, t + thirdH * i), Offset(r, t + thirdH * i), strokeWidth = 1.2f)
        }

        // مقابض الزوايا الأربع
        listOf(
          Offset(l, t),
          Offset(r, t),
          Offset(l, b),
          Offset(r, b),
        ).forEach { corner ->
          drawCircle(color = CharcoalBlack, radius = handleRadiusPx + 2.5f, center = corner)
          drawCircle(color = PrimaryYellow, radius = handleRadiusPx, center = corner)
        }
      }
    }
  }
}

/**
 * عارض صورة بملء الشاشة مع دعم التكبير والتمرير (مناسب لمعاينة إيصالات الدفع وصور الأقسام).
 */
@Composable
fun ZoomableImagePreviewDialog(
  imageUrl: String,
  title: String,
  onDismiss: () -> Unit,
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Scaffold(
      containerColor = Color.Black,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PureWhite,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = {
              scale = 1f
              offset = Offset.Zero
            },
          ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = "إعادة الحجم", tint = PrimaryYellow)
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("zoom_image_close_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = PureWhite)
          }
        }
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
              val nextScale = (scale * zoom).coerceIn(1f, 4f)
              scale = nextScale
              offset = if (nextScale <= 1.01f) {
                Offset.Zero
              } else {
                Offset(
                  x = (offset.x + pan.x).coerceIn(-600f, 600f),
                  y = (offset.y + pan.y).coerceIn(-600f, 600f),
                )
              }
            }
          },
        contentAlignment = Alignment.Center,
      ) {
        SubcomposeAsyncImage(
          model = imageUrl,
          contentDescription = title,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
              translationX = offset.x
              translationY = offset.y
            },
          loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = PrimaryYellow)
            }
          },
          error = {
            Text(
              text = "تعذر تحميل الصورة. ربما تم حذفها أو لا يوجد اتصال.",
              color = PureWhite,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(24.dp),
            )
          },
        )
      }
    }
  }
}
