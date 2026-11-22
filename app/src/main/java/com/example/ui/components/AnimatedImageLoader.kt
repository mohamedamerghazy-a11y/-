package com.example.ui.components

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import com.example.data.media.CloudinaryFolder
import com.example.data.media.CloudinaryUploadService
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

/**
 * Shared Coil [ImageLoader] configured with Android's native [ImageDecoderDecoder] (API 28+,
 * supports both Animated GIF and Animated WebP) and [GifDecoder] fallback (API 24–27), backed by
 * a dedicated persistent disk cache so animated banners stay cached locally without re-downloading.
 */
object AnimatedBannerImageLoaderProvider {
  @Volatile
  private var cachedLoader: ImageLoader? = null

  fun get(context: Context): ImageLoader {
    cachedLoader?.let { return it }
    return synchronized(this) {
      cachedLoader?.let { return it }
      val appCtx = context.applicationContext
      val cacheDir = File(appCtx.filesDir, "animated_banners_cache").apply {
        if (!exists()) mkdirs()
      }
      val loader = ImageLoader.Builder(appCtx)
        .components {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(ImageDecoderDecoder.Factory())
          } else {
            add(GifDecoder.Factory())
          }
        }
        .diskCache {
          DiskCache.Builder()
            .directory(cacheDir)
            .maxSizeBytes(50L * 1024L * 1024L)
            .build()
        }
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .crossfade(false)
        .build()
      cachedLoader = loader
      loader
    }
  }
}

/**
 * Renders an animated GIF / Animated WebP promotional banner with smooth infinite looping and
 * dedicated Normal vs. Slim banner aspect ratios.
 */
@Composable
fun InlineAnimatedPromoBanner(
  imageUrl: String,
  contentDescription: String,
  isSlim: Boolean,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val imageLoader = remember(context) { AnimatedBannerImageLoaderProvider.get(context) }
  val aspect = if (isSlim) 2.95f else 1.85f

  val request = remember(imageUrl, context) {
    ImageRequest.Builder(context)
      .data(imageUrl)
      .size(CoilSize.ORIGINAL)
      .diskCachePolicy(CachePolicy.ENABLED)
      .build()
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(aspect)
      .background(GeneralBackground),
    contentAlignment = Alignment.Center,
  ) {
    SubcomposeAsyncImage(
      model = request,
      imageLoader = imageLoader,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
      loading = {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(GeneralBackground),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            color = RoyalBlue,
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
          )
        }
      },
      error = {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(GeneralBackground),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Animation,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(34.dp),
          )
        }
      },
    )
  }
}

/**
 * Zero-permission Android Photo Picker uploader specifically for Animated GIF / WebP banners
 * (preserves animation frames without bitmap re-compression).
 */
@Composable
fun AnimatedImagePickerUploadField(
  imageUrl: String,
  isSlimPreview: Boolean,
  onImageUrlChanged: (String) -> Unit,
  onClearImage: () -> Unit,
  folder: CloudinaryFolder = CloudinaryFolder.BANNERS,
  testTagPrefix: String = "banner_animated",
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uploader = remember { CloudinaryUploadService() }
  var isUploading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      isUploading = true
      errorMsg = null
      val result = uploader.uploadAnimatedImageUri(
        context = context,
        uri = uri,
        folder = folder,
      )
      isUploading = false
      result.fold(
        onSuccess = { uploaded ->
          onImageUrlChanged(uploaded.secureUrl)
        },
        onFailure = { err ->
          errorMsg = err.localizedMessage ?: "تعذر رفع الصورة المتحركة."
        },
      )
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = PrimaryYellowSoft,
      border = BorderStroke(1.dp, PrimaryYellow),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = "يدعم الصور المتحركة بصيغة GIF أو Animated WebP مع الحفاظ على الحركة كاملة والتكرار التلقائي وحفظها على جهاز العميل.",
        style = MaterialTheme.typography.labelSmall,
        color = CharcoalBlack,
        modifier = Modifier.padding(10.dp),
      )
    }

    if (imageUrl.isNotBlank()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp)),
      ) {
        InlineAnimatedPromoBanner(
          imageUrl = imageUrl,
          contentDescription = "معاينة الصورة المتحركة",
          isSlim = isSlimPreview,
        )
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CharcoalBlack.copy(alpha = 0.82f),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Filled.Animation, contentDescription = null, tint = PrimaryYellow, modifier = Modifier.size(14.dp))
            Text(
              text = "صورة متحركة",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = PrimaryYellow,
            )
          }
        }
        IconButton(
          onClick = onClearImage,
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
            .testTag("${testTagPrefix}_clear_button"),
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CharcoalBlack.copy(alpha = 0.78f),
          ) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "حذف الصورة المتحركة",
              tint = DiscountRed,
              modifier = Modifier.padding(6.dp).size(18.dp),
            )
          }
        }
      }
    }

    OutlinedButton(
      onClick = {
        photoPickerLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
      },
      enabled = !isUploading,
      border = BorderStroke(1.dp, if (isUploading) BorderNeutral else CharcoalBlack),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("${testTagPrefix}_pick_button"),
    ) {
      if (isUploading) {
        CircularProgressIndicator(
          color = CharcoalBlack,
          strokeWidth = 2.dp,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("جاري رفع الصورة المتحركة...", color = CharcoalBlack, fontWeight = FontWeight.Bold)
      } else {
        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = CharcoalBlack)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (imageUrl.isBlank()) "رفع صورة متحركة (GIF / WebP) من المعرض" else "تغيير الصورة المتحركة",
          color = CharcoalBlack,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    if (!errorMsg.isNullOrBlank()) {
      Text(
        text = errorMsg.orEmpty(),
        color = DiscountRed,
        style = MaterialTheme.typography.bodySmall,
      )
    }

    OutlinedTextField(
      value = imageUrl,
      onValueChange = onImageUrlChanged,
      label = { Text("رابط الصورة المتحركة (GIF أو WebP)") },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("${testTagPrefix}_url_input"),
      singleLine = true,
    )
  }
}
