package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.media.CloudinaryFolder
import com.example.data.media.CloudinaryUploadService
import com.example.data.model.deriveVideoPosterUrl
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Zero-permission Android video picker (PickVisualMedia.VideoOnly) + Cloudinary uploader
 * with automatic duration detection, poster thumbnail preview, and direct URL fallback.
 */
@Composable
fun VideoPickerUploadField(
  videoUrl: String,
  detectedDurationSeconds: Int,
  buttonLabel: String,
  urlFieldLabel: String,
  folder: CloudinaryFolder = CloudinaryFolder.VIDEOS,
  testTagPrefix: String = "video_picker",
  onVideoChanged: (videoUrl: String, detectedDurationSeconds: Int?, posterUrl: String?) -> Unit,
  onClearVideo: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uploader = remember { CloudinaryUploadService() }
  var isUploading by remember { mutableStateOf(false) }
  var uploadError by remember { mutableStateOf<String?>(null) }

  val pickVideoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      isUploading = true
      uploadError = null
      val result = uploader.uploadVideoUri(context = context, uri = uri, folder = folder)
      isUploading = false
      result.fold(
        onSuccess = { uploaded ->
          onVideoChanged(uploaded.secureUrl, uploaded.durationSeconds, uploaded.posterUrl)
        },
        onFailure = { err ->
          uploadError = err.localizedMessage ?: "تعذر رفع الفيديو، حاول مرة أخرى."
        },
      )
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (videoUrl.trim().length >= 5) {
      val posterUrl = deriveVideoPosterUrl(videoUrl)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = CharcoalBlack,
        border = BorderStroke(1.dp, BorderNeutral),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(width = 78.dp, height = 54.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(CharcoalBlack),
            contentAlignment = Alignment.Center,
          ) {
            if (posterUrl.isNotBlank()) {
              SubcomposeAsyncImage(
                model = posterUrl,
                contentDescription = "معاينة الفيديو",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
              )
            }
            Icon(
              imageVector = Icons.Filled.PlayCircle,
              contentDescription = null,
              tint = PrimaryYellow,
              modifier = Modifier.size(28.dp),
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "تم إرفاق الفيديو بنجاح",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = PrimaryYellow,
            )
            if (detectedDurationSeconds > 0) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Schedule,
                  contentDescription = null,
                  tint = PureWhite.copy(alpha = 0.85f),
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "المدة المكتشفة تلقائيًا: $detectedDurationSeconds ثانية",
                  style = MaterialTheme.typography.labelSmall,
                  color = PureWhite,
                )
              }
            }
            Text(
              text = videoUrl.trim(),
              style = MaterialTheme.typography.labelSmall,
              color = PureWhite.copy(alpha = 0.7f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (onClearVideo != null) {
            IconButton(
              onClick = {
                uploadError = null
                onClearVideo()
              },
              modifier = Modifier.testTag("${testTagPrefix}_clear_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "إزالة الفيديو",
                tint = PrimaryYellow,
              )
            }
          }
        }
      }
    }

    OutlinedButton(
      onClick = {
        pickVideoLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
        )
      },
      enabled = !isUploading,
      border = BorderStroke(1.dp, CharcoalBlack),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("${testTagPrefix}_upload_button"),
    ) {
      if (isUploading) {
        CircularProgressIndicator(
          color = CharcoalBlack,
          strokeWidth = 2.dp,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("جاري رفع الفيديو...", color = CharcoalBlack, fontWeight = FontWeight.Bold)
      } else {
        Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = CharcoalBlack)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (videoUrl.isBlank()) buttonLabel else "تغيير الفيديو من المعرض",
          color = CharcoalBlack,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    if (!uploadError.isNullOrBlank()) {
      Text(
        text = uploadError.orEmpty(),
        color = DiscountRed,
        style = MaterialTheme.typography.bodySmall,
      )
    }

    OutlinedTextField(
      value = videoUrl,
      onValueChange = { nextUrl ->
        uploadError = null
        onVideoChanged(nextUrl, null, deriveVideoPosterUrl(nextUrl).ifBlank { null })
      },
      label = { Text(urlFieldLabel) },
      placeholder = { Text("https://...mp4") },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("${testTagPrefix}_url_input"),
      singleLine = true,
    )

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = PrimaryYellowSoft,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = "يفضل رفع فيديو قصير بصيغة MP4 (من ٥ إلى ٣٠ ثانية) لضمان سرعة التشغيل الفوري للعملاء.",
        style = MaterialTheme.typography.labelSmall,
        color = CharcoalBlack,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      )
    }
  }
}
