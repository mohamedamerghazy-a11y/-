package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * ماسح ضوئي احترافي لرموز QR والباركود عبر CameraX + ML Kit.
 * قابل لإعادة الاستخدام في بحث العملاء وإدخال كود المنتج في لوحة الإدارة.
 */
@Composable
fun QrBarcodeScannerDialog(
  title: String = "مسح رمز QR أو الباركود",
  subtitle: String = "وجّه الكاميرا نحو رمز الباركود أو QR لقراءته تلقائيًا",
  onCodeScanned: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED,
    )
  }
  var permissionRequested by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    hasCameraPermission = granted
    permissionRequested = true
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Scaffold(
      containerColor = GeneralBackground,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        AlEmanTopBar(
          title = title,
          subtitle = subtitle,
          showBack = false,
          trailingContent = {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("scanner_close_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "إغلاق الماسح",
                tint = RoyalBlue,
              )
            }
          },
        )
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        if (hasCameraPermission) {
          CameraBarcodePreview(
            onCodeScanned = onCodeScanned,
            onCancel = onDismiss,
          )
        } else {
          Column(
            modifier = Modifier
              .widthIn(max = 520.dp)
              .padding(AlEmanDimens.SpaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.QrCodeScanner,
              title = "صلاحية الكاميرا مطلوبة للمسح",
              description = if (permissionRequested) {
                "يرجى السماح للتطبيق باستخدام الكاميرا لقراءة رموز الباركود وQR، أو إدخال الرمز يدويًا في حقل البحث."
              } else {
                "جاري طلب إذن استخدام الكاميرا لقراءة الباركود وQR."
              },
              actionLabel = "منح صلاحية الكاميرا",
              onActionClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
            TextButton(
              onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                  data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
              },
              modifier = Modifier.testTag("scanner_open_settings_button"),
            ) {
              Text(
                text = "فتح إعدادات التطبيق",
                color = RoyalBlue,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CameraBarcodePreview(
  onCodeScanned: (String) -> Unit,
  onCancel: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

  var isTorchOn by remember { mutableStateOf(false) }
  var hasDeliveredResult by remember { mutableStateOf(false) }

  val barcodeScanner = remember {
    val options = BarcodeScannerOptions.Builder()
      .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
      .build()
    BarcodeScanning.getClient(options)
  }

  val cameraController = remember(context) {
    LifecycleCameraController(context).apply {
      cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
      setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
      setImageAnalysisAnalyzer(
        mainExecutor,
        MlKitAnalyzer(
          listOf(barcodeScanner),
          CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
          mainExecutor,
        ) { result ->
          if (hasDeliveredResult) return@MlKitAnalyzer
          val barcodes = result?.getValue(barcodeScanner).orEmpty()
          val detected = barcodes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf { v -> v.isNotEmpty() } }
          if (detected != null) {
            hasDeliveredResult = true
            onCodeScanned(detected)
          }
        },
      )
    }
  }

  DisposableEffect(lifecycleOwner, cameraController) {
    cameraController.bindToLifecycle(lifecycleOwner)
    onDispose {
      cameraController.unbind()
      barcodeScanner.close()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      factory = { ctx ->
        PreviewView(ctx).apply {
          controller = cameraController
          scaleType = PreviewView.ScaleType.FILL_CENTER
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // إطار تحديد المسح الواضح
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Box(
          modifier = Modifier
            .size(250.dp)
            .border(3.dp, GoldenYellow, RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
        )
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          color = PureWhite.copy(alpha = 0.94f),
          border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.35f)),
        ) {
          Text(
            text = "ضع رمز الباركود أو QR داخل الإطار الذهبي",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          )
        }
      }
    }

    // أزرار الفلاش والإلغاء أسفل الكاميرا
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(AlEmanDimens.SpaceLg),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        onClick = {
          val next = !isTorchOn
          isTorchOn = next
          cameraController.enableTorch(next)
        },
        shape = CircleShape,
        color = if (isTorchOn) GoldenYellow else PureWhite,
        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f)),
        modifier = Modifier.testTag("scanner_torch_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
            contentDescription = if (isTorchOn) "إطفاء الفلاش" else "تشغيل الفلاش",
            tint = if (isTorchOn) TextPrimary else RoyalBlue,
          )
          Text(
            text = if (isTorchOn) "إيقاف الفلاش" else "تشغيل الفلاش",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
          )
        }
      }

      Surface(
        onClick = onCancel,
        shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
        color = PureWhite,
        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f)),
        modifier = Modifier
          .height(48.dp)
          .testTag("scanner_cancel_button"),
      ) {
        Box(
          modifier = Modifier.padding(horizontal = 20.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "إلغاء",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary,
          )
        }
      }
    }
  }
}
