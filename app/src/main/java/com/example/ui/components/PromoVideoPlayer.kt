package com.example.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import com.example.core.util.ArabicFormatter
import com.example.data.media.VideoDiskCache
import com.example.data.model.IntroMediaType
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay
import kotlin.math.ceil

enum class VideoScaleMode {
  CENTER_CROP,
  SMART_REEL,
  FIT_CENTER,
}

private fun applyTextureVideoTransform(
  textureView: TextureView,
  videoWidth: Int,
  videoHeight: Int,
  scaleMode: VideoScaleMode,
) {
  val viewWidth = textureView.width.toFloat()
  val viewHeight = textureView.height.toFloat()
  if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) return

  val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
  val viewAspect = viewWidth / viewHeight

  val useCrop = when (scaleMode) {
    VideoScaleMode.CENTER_CROP -> true
    VideoScaleMode.FIT_CENTER -> false
    VideoScaleMode.SMART_REEL -> {
      // Portrait video on portrait screen OR landscape video on landscape screen => edge-to-edge crop.
      val bothPortrait = videoAspect <= 1.05f && viewAspect <= 1.05f
      val bothLandscape = videoAspect >= 1.05f && viewAspect >= 1.05f
      bothPortrait || bothLandscape
    }
  }

  val scaleX: Float
  val scaleY: Float
  if (useCrop) {
    if (videoAspect > viewAspect) {
      scaleX = videoAspect / viewAspect
      scaleY = 1f
    } else {
      scaleX = 1f
      scaleY = viewAspect / videoAspect
    }
  } else {
    if (videoAspect > viewAspect) {
      scaleX = 1f
      scaleY = viewAspect / videoAspect
    } else {
      scaleX = videoAspect / viewAspect
      scaleY = 1f
    }
  }

  val pivotX = viewWidth / 2f
  val pivotY = viewHeight / 2f
  val matrix = Matrix()
  matrix.setScale(scaleX, scaleY, pivotX, pivotY)
  textureView.setTransform(matrix)
}

private class ManagedVideoController {
  var mediaPlayer: MediaPlayer? = null
  var surface: Surface? = null
  var isPrepared: Boolean = false
  var isReleased: Boolean = false
  var videoWidth: Int = 0
  var videoHeight: Int = 0
  var textureView: TextureView? = null
  var scaleMode: VideoScaleMode = VideoScaleMode.CENTER_CROP

  fun applyMute(muted: Boolean) {
    val vol = if (muted) 0f else 1f
    runCatching { mediaPlayer?.setVolume(vol, vol) }
  }

  fun setPlaying(play: Boolean) {
    val mp = mediaPlayer ?: return
    if (!isPrepared || isReleased) return
    runCatching {
      if (play && !mp.isPlaying) {
        mp.start()
      } else if (!play && mp.isPlaying) {
        mp.pause()
      }
    }
  }

  fun updateTransform() {
    val tv = textureView ?: return
    if (videoWidth > 0 && videoHeight > 0) {
      applyTextureVideoTransform(tv, videoWidth, videoHeight, scaleMode)
    }
  }

  fun release() {
    isReleased = true
    isPrepared = false
    runCatching {
      mediaPlayer?.let { mp ->
        if (mp.isPlaying) mp.stop()
        mp.reset()
        mp.release()
      }
    }
    mediaPlayer = null
    runCatching { surface?.release() }
    surface = null
  }
}

@Composable
private fun TextureVideoSurface(
  videoUrl: String,
  isPlaying: Boolean,
  isMuted: Boolean,
  isLooping: Boolean,
  scaleMode: VideoScaleMode,
  modifier: Modifier = Modifier,
  onPrepared: (durationMs: Long) -> Unit = {},
  onProgress: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
  onCompleted: () -> Unit = {},
  onError: () -> Unit = {},
) {
  val context = LocalContext.current
  val controller = remember(videoUrl) { ManagedVideoController() }
  val latestIsPlaying by rememberUpdatedState(isPlaying)
  val latestIsMuted by rememberUpdatedState(isMuted)
  val latestIsLooping by rememberUpdatedState(isLooping)
  val latestOnPrepared by rememberUpdatedState(onPrepared)
  val latestOnProgress by rememberUpdatedState(onProgress)
  val latestOnCompleted by rememberUpdatedState(onCompleted)
  val latestOnError by rememberUpdatedState(onError)
  val lifecycleOwner = LocalLifecycleOwner.current
  var isLifecycleResumed by remember { mutableStateOf(true) }

  controller.scaleMode = scaleMode

  // Ensure the video is saved locally on the device so future plays work instantly without re-downloading.
  LaunchedEffect(videoUrl) {
    if (videoUrl.trim().length >= 5 && VideoDiskCache.getCachedFile(context, videoUrl) == null) {
      VideoDiskCache.ensureCached(context, videoUrl)
    }
  }

  LaunchedEffect(isMuted) {
    controller.applyMute(isMuted)
  }

  LaunchedEffect(isPlaying, isLifecycleResumed) {
    controller.setPlaying(isPlaying && isLifecycleResumed)
  }

  LaunchedEffect(videoUrl, onProgress != null) {
    if (onProgress == null) return@LaunchedEffect
    while (true) {
      delay(120L)
      val mp = controller.mediaPlayer
      if (mp != null && controller.isPrepared && !controller.isReleased) {
        val pos = runCatching { mp.currentPosition.toLong() }.getOrDefault(0L)
        val dur = runCatching { mp.duration.toLong() }.getOrDefault(0L)
        if (dur > 0L) {
          latestOnProgress?.invoke(pos.coerceIn(0L, dur), dur)
        }
      }
    }
  }

  DisposableEffect(lifecycleOwner, controller) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> {
          isLifecycleResumed = false
          controller.setPlaying(false)
        }
        Lifecycle.Event.ON_RESUME -> {
          isLifecycleResumed = true
          controller.setPlaying(latestIsPlaying)
        }
        Lifecycle.Event.ON_DESTROY -> {
          controller.release()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      controller.release()
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { context ->
      TextureView(context).apply {
        isOpaque = false
        controller.textureView = this
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
          override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
            if (controller.isReleased) return
            controller.surface = Surface(st)
            val mp = MediaPlayer()
            controller.mediaPlayer = mp
            try {
              mp.setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_MEDIA)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                  .build(),
              )
              val localFile = VideoDiskCache.getCachedFile(context, videoUrl)
              val effectiveSource = localFile?.absolutePath ?: videoUrl
              mp.setDataSource(effectiveSource)
              mp.setSurface(controller.surface)
              mp.isLooping = latestIsLooping
              mp.setOnVideoSizeChangedListener { _, vw, vh ->
                controller.videoWidth = vw
                controller.videoHeight = vh
                post { controller.updateTransform() }
              }
              mp.setOnPreparedListener { preparedMp ->
                if (controller.isReleased) return@setOnPreparedListener
                controller.isPrepared = true
                controller.videoWidth = preparedMp.videoWidth
                controller.videoHeight = preparedMp.videoHeight
                post { controller.updateTransform() }
                controller.applyMute(latestIsMuted)
                val durMs = runCatching { preparedMp.duration.toLong() }.getOrDefault(0L)
                latestOnPrepared(durMs)
                if (latestIsPlaying && isLifecycleResumed) {
                  runCatching { preparedMp.start() }
                }
              }
              mp.setOnCompletionListener { completedMp ->
                if (!controller.isReleased) {
                  if (latestIsLooping) {
                    runCatching {
                      completedMp.seekTo(0)
                      if (latestIsPlaying && isLifecycleResumed) {
                        completedMp.start()
                      }
                    }
                  } else {
                    latestOnCompleted()
                  }
                }
              }
              mp.setOnErrorListener { _, _, _ ->
                if (!controller.isReleased) {
                  latestOnError()
                }
                true
              }
              mp.prepareAsync()
            } catch (_: Exception) {
              latestOnError()
            }
          }

          override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
            controller.updateTransform()
          }

          override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
            controller.release()
            return true
          }

          override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }
      }
    },
    update = { tv ->
      controller.textureView = tv
      controller.updateTransform()
    },
  )
}

/**
 * Inline auto-playing short promotional video banner for Home carousels.
 * Renders cleanly inside LazyColumn & HorizontalPager without surface clipping.
 */
@Composable
fun InlinePromoVideoBanner(
  videoUrl: String,
  posterUrl: String,
  isSlim: Boolean,
  isActivePage: Boolean,
  forceMuted: Boolean = false,
  modifier: Modifier = Modifier,
) {
  var isMuted by remember(videoUrl, forceMuted) { mutableStateOf(forceMuted) }
  val effectiveMuted = forceMuted || isMuted
  var isPrepared by remember(videoUrl) { mutableStateOf(false) }
  var hasError by remember(videoUrl) { mutableStateOf(false) }
  val ratio = if (isSlim) 2.35f else 16f / 9f

  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(ratio)
      .background(Color(0xFF101218)),
  ) {
    if (!hasError && videoUrl.trim().length >= 5) {
      TextureVideoSurface(
        videoUrl = videoUrl.trim(),
        isPlaying = isActivePage,
        isMuted = effectiveMuted,
        isLooping = true,
        scaleMode = VideoScaleMode.CENTER_CROP,
        modifier = Modifier.fillMaxSize(),
        onPrepared = { isPrepared = true },
        onError = { hasError = true },
      )
    }

    // Poster frame visible until the first video frame is prepared (or on error)
    if (!isPrepared || hasError) {
      if (posterUrl.isNotBlank()) {
        SubcomposeAsyncImage(
          model = posterUrl,
          contentDescription = "معاينة الفيديو",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(CharcoalBlack.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
      ) {
        if (!hasError) {
          CircularProgressIndicator(
            color = PrimaryYellow,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(30.dp),
          )
        } else {
          Surface(
            shape = CircleShape,
            color = CharcoalBlack.copy(alpha = 0.72f),
          ) {
            Icon(
              imageVector = Icons.Filled.PlayArrow,
              contentDescription = "تشغيل الفيديو",
              tint = PrimaryYellow,
              modifier = Modifier
                .padding(10.dp)
                .size(28.dp),
            )
          }
        }
      }
    }

    // 1-tap Mute/Unmute toggle (hidden completely when admin uploaded the video as muted/silent)
    if (!hasError && !forceMuted) {
      Surface(
        shape = CircleShape,
        color = CharcoalBlack.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.2f)),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(horizontal = 10.dp, vertical = 8.dp)
          .size(36.dp)
          .clickable { isMuted = !isMuted }
          .testTag("promo_video_mute_toggle"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isMuted) "تشغيل الصوت" else "كتم الصوت",
            tint = PureWhite,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

/**
 * Full-screen Reels-style introductory media (Short Video or Animated GIF/WebP) shown right after
 * the Splash screen on Android. Respects admin's sound/mute choice for video, shows only the clean
 * "تخطى" (Skip) button at the top (no progress bar, no bottom store name overlay), and auto-dismisses
 * when the media completes.
 */
@Composable
fun IntroReelVideoOverlay(
  videoUrl: String,
  mediaType: IntroMediaType = IntroMediaType.VIDEO,
  isVideoMuted: Boolean = false,
  title: String = "",
  storeName: String = "",
  fallbackDurationSeconds: Int,
  onFinish: () -> Unit,
) {
  val context = LocalContext.current
  val isAnimatedImage = mediaType == IntroMediaType.ANIMATED_IMAGE
  val effectiveAnimatedSeconds = fallbackDurationSeconds.takeIf { it > 0 }?.coerceIn(1, 120) ?: 5

  var finished by remember { mutableStateOf(false) }
  var isPrepared by remember(videoUrl, mediaType) { mutableStateOf(false) }
  var detectedDurationMs by remember(videoUrl, mediaType) {
    mutableLongStateOf(
      if (isAnimatedImage) effectiveAnimatedSeconds * 1000L else fallbackDurationSeconds.coerceIn(0, 120) * 1000L,
    )
  }
  var remainingSeconds by remember(videoUrl, mediaType) {
    mutableIntStateOf(
      if (isAnimatedImage) effectiveAnimatedSeconds else fallbackDurationSeconds.coerceIn(0, 120),
    )
  }
  var animatedScaleMode by remember(videoUrl) { mutableStateOf(ContentScale.Crop) }

  val finishOnce = remember(onFinish) {
    {
      if (!finished) {
        finished = true
        onFinish()
      }
    }
  }

  BackHandler {
    finishOnce()
  }

  // Startup safety guard: if media cannot load within 10s (offline / bad link), open the store.
  LaunchedEffect(videoUrl, mediaType, isPrepared) {
    if (!isPrepared) {
      delay(10_000L)
      if (!isPrepared) {
        finishOnce()
      }
    }
  }

  // Auto-dismiss countdown for Animated Image (GIF / WebP) intro reels.
  LaunchedEffect(videoUrl, mediaType, isPrepared, effectiveAnimatedSeconds) {
    if (isAnimatedImage && isPrepared) {
      var secsLeft = effectiveAnimatedSeconds
      remainingSeconds = secsLeft
      while (secsLeft > 0 && !finished) {
        delay(1_000L)
        secsLeft--
        remainingSeconds = secsLeft
      }
      finishOnce()
    }
  }

  // Hard duration safety guard once prepared for video: ensures auto-dismiss right after video ends.
  LaunchedEffect(videoUrl, mediaType, isPrepared, detectedDurationMs) {
    if (!isAnimatedImage && isPrepared && detectedDurationMs > 0L) {
      delay(detectedDurationMs + 2_000L)
      finishOnce()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0A0C10))
      .testTag("intro_reel_video_overlay"),
  ) {
    if (isAnimatedImage) {
      val imageLoader = remember(context) { AnimatedBannerImageLoaderProvider.get(context) }
      val request = remember(videoUrl, context) {
        ImageRequest.Builder(context)
          .data(videoUrl.trim())
          .size(CoilSize.ORIGINAL)
          .diskCachePolicy(CachePolicy.ENABLED)
          .build()
      }
      SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = "عرض ترحيبي متحرك",
        contentScale = animatedScaleMode,
        modifier = Modifier.fillMaxSize(),
        onSuccess = { success ->
          val dw = success.result.drawable.intrinsicWidth
          val dh = success.result.drawable.intrinsicHeight
          if (dw > 0 && dh > 0) {
            animatedScaleMode = if (dh >= dw) ContentScale.Crop else ContentScale.Fit
          }
          isPrepared = true
        },
        onError = {
          finishOnce()
        },
      )
    } else {
      TextureVideoSurface(
        videoUrl = videoUrl.trim(),
        isPlaying = !finished,
        isMuted = isVideoMuted,
        isLooping = false,
        scaleMode = VideoScaleMode.SMART_REEL,
        modifier = Modifier.fillMaxSize(),
        onPrepared = { durMs ->
          isPrepared = true
          if (durMs > 0L) {
            detectedDurationMs = durMs
            remainingSeconds = ceil(durMs / 1000.0).toInt().coerceAtLeast(1)
          }
        },
        onProgress = { posMs, durMs ->
          if (durMs > 0L) {
            detectedDurationMs = durMs
            val remMs = (durMs - posMs).coerceAtLeast(0L)
            remainingSeconds = ceil(remMs / 1000.0).toInt()
            if (posMs >= durMs - 180L && posMs > 500L) {
              finishOnce()
            }
          }
        },
        onCompleted = {
          remainingSeconds = 0
          finishOnce()
        },
        onError = {
          finishOnce()
        },
      )
    }

    // Subtle top gradient for legibility of the Skip button only
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(110.dp)
        .align(Alignment.TopCenter)
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.55f),
              Color.Transparent,
            ),
          ),
        ),
    )

    // Buffering indicator while preparing
    AnimatedVisibility(
      visible = !isPrepared,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CircularProgressIndicator(
          color = PrimaryYellow,
          strokeWidth = 3.dp,
          modifier = Modifier.size(40.dp),
        )
        Text(
          text = "جاري تجهيز العرض...",
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite.copy(alpha = 0.85f),
        )
      }
    }

    // Clean top bar: ONLY the prominent "تخطى" (Skip) button (no progress bar, no store name)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      contentAlignment = Alignment.TopStart,
    ) {
      Surface(
        onClick = { finishOnce() },
        shape = RoundedCornerShape(50),
        color = CharcoalBlack.copy(alpha = 0.78f),
        border = BorderStroke(1.2.dp, PrimaryYellow.copy(alpha = 0.85f)),
        modifier = Modifier.testTag("intro_video_skip_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "تخطي والدخول للمتجر",
            tint = PrimaryYellow,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "تخطى",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = PureWhite,
          )
          if (remainingSeconds > 0) {
            Text(
              text = "• ${ArabicFormatter.formatNumber(remainingSeconds)}ث",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = PrimaryYellow,
            )
          }
        }
      }
    }
  }
}
