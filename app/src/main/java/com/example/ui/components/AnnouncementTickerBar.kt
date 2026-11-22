package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.AnnouncementTickerConfig
import com.example.data.model.TickerFontWeight
import com.example.data.model.TickerInlineImage
import kotlin.math.roundToInt

sealed interface TickerSegment {
  data class TextSegment(val text: String) : TickerSegment
  data class ImageSegment(val id: String, val url: String) : TickerSegment
}

private val TICKER_IMAGE_TOKEN_REGEX = Regex("\\[\\[img:([a-zA-Z0-9_-]+)\\]\\]")

fun parseTickerSegments(
  text: String,
  images: List<TickerInlineImage>,
): List<TickerSegment> {
  val imageMap = images
    .filter { it.id.isNotBlank() && it.url.isNotBlank() }
    .associate { it.id.trim() to it.url.trim() }

  if (text.isBlank()) {
    return images
      .filter { it.id.isNotBlank() && it.url.isNotBlank() }
      .map { TickerSegment.ImageSegment(it.id.trim(), it.url.trim()) }
  }

  val result = mutableListOf<TickerSegment>()
  var lastIndex = 0
  for (match in TICKER_IMAGE_TOKEN_REGEX.findAll(text)) {
    val start = match.range.first
    if (start > lastIndex) {
      val chunk = text.substring(lastIndex, start)
      if (chunk.isNotEmpty()) {
        result.add(TickerSegment.TextSegment(chunk))
      }
    }
    val imgId = match.groupValues.getOrNull(1).orEmpty().trim()
    val url = imageMap[imgId]
    if (!url.isNullOrBlank()) {
      result.add(TickerSegment.ImageSegment(imgId, url))
    }
    lastIndex = match.range.last + 1
  }
  if (lastIndex < text.length) {
    val tail = text.substring(lastIndex)
    if (tail.isNotEmpty()) {
      result.add(TickerSegment.TextSegment(tail))
    }
  }
  return result
}

fun parseTickerHexColor(hex: String, fallback: Color): Color {
  return try {
    Color(android.graphics.Color.parseColor(hex))
  } catch (_: Exception) {
    fallback
  }
}

fun TickerFontWeight.toComposeFontWeight(): FontWeight = when (this) {
  TickerFontWeight.LIGHT -> FontWeight.Light
  TickerFontWeight.NORMAL -> FontWeight.Normal
  TickerFontWeight.BOLD -> FontWeight.Bold
  TickerFontWeight.EXTRA_BOLD -> FontWeight.ExtraBold
}

/**
 * Polished scrolling Announcement Marquee Ticker moving smoothly from LEFT to RIGHT
 * (من اليسار إلى اليمين), supporting custom background/border/text colors, font size,
 * weight (light/normal/bold/extra-bold), italic style, emojis, and inline thumbnails anywhere
 * inside the text.
 */
@Composable
fun AnnouncementTickerBar(
  config: AnnouncementTickerConfig,
  modifier: Modifier = Modifier,
) {
  val segments = remember(config.text, config.inlineImages) {
    parseTickerSegments(config.text, config.inlineImages)
  }
  if (segments.isEmpty()) return

  val bgColor = remember(config.safeBackgroundHex) {
    parseTickerHexColor(config.safeBackgroundHex, Color(0xFF181A20))
  }
  val borderColor = remember(config.safeBorderHex) {
    parseTickerHexColor(config.safeBorderHex, Color(0xFFFFD000))
  }
  val textColor = remember(config.safeTextHex) {
    parseTickerHexColor(config.safeTextHex, Color.White)
  }
  val fontSizeSp = config.safeFontSizeSp
  val fontWeight = config.fontWeight.toComposeFontWeight()
  val fontStyle = if (config.isItalic) FontStyle.Italic else FontStyle.Normal
  val thumbSizeDp = (fontSizeSp * 1.65f).coerceIn(20f, 38f).dp
  val minBarHeightDp = (fontSizeSp * 2.5f).coerceIn(38f, 62f).dp

  var containerWidthPx by remember { mutableIntStateOf(0) }
  var unitWidthPx by remember(segments, fontSizeSp, fontWeight, fontStyle) { mutableIntStateOf(0) }

  val density = LocalDensity.current
  val speedPxPerSec = with(density) { 54.dp.toPx() }
  val durationMs = remember(unitWidthPx, speedPxPerSec) {
    if (unitWidthPx <= 0 || speedPxPerSec <= 0f) {
      9_000
    } else {
      ((unitWidthPx / speedPxPerSec) * 1000f).roundToInt().coerceIn(4_500, 32_000)
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "ticker_marquee_ltr")
  val progress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = durationMs, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "ticker_progress",
  )

  val copies = remember(containerWidthPx, unitWidthPx) {
    if (unitWidthPx <= 0 || containerWidthPx <= 0) {
      2
    } else {
      ((containerWidthPx / unitWidthPx) + 3).coerceIn(2, 8)
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("announcement_ticker_bar"),
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    border = BorderStroke(1.8.dp, borderColor),
    shadowElevation = 2.dp,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(minBarHeightDp)
        .clipToBounds()
        .onSizeChanged { containerWidthPx = it.width },
      contentAlignment = Alignment.CenterStart,
    ) {
      // Subtle top & bottom accent inner glow matching the strip border color
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .align(Alignment.TopCenter)
          .background(borderColor.copy(alpha = 0.28f)),
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .align(Alignment.BottomCenter)
          .background(borderColor.copy(alpha = 0.28f)),
      )

      // LTR coordinate space so translationX moves strictly from LEFT to RIGHT (-unitWidth -> 0)
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
          modifier = Modifier
            .wrapContentWidth(unbounded = true, align = Alignment.Start)
            .graphicsLayer {
              translationX = if (unitWidthPx > 0) {
                -unitWidthPx.toFloat() + (progress * unitWidthPx.toFloat())
              } else {
                0f
              }
            },
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(copies) { index ->
            val measureMod = if (index == 0) {
              Modifier.onSizeChanged { size ->
                if (size.width > 0 && size.width != unitWidthPx) {
                  unitWidthPx = size.width
                }
              }
            } else {
              Modifier
            }
            TickerSinglePassRow(
              segments = segments,
              textColor = textColor,
              accentColor = borderColor,
              fontSizeSp = fontSizeSp,
              fontWeight = fontWeight,
              fontStyle = fontStyle,
              thumbSizeDp = thumbSizeDp,
              modifier = measureMod,
            )
          }
        }
      }

      // Soft left & right edge fade masks for a polished broadcast ticker look
      Box(
        modifier = Modifier
          .width(18.dp)
          .fillMaxHeight()
          .align(Alignment.CenterStart)
          .background(
            Brush.horizontalGradient(
              colors = listOf(bgColor, Color.Transparent),
            ),
          ),
      )
      Box(
        modifier = Modifier
          .width(18.dp)
          .fillMaxHeight()
          .align(Alignment.CenterEnd)
          .background(
            Brush.horizontalGradient(
              colors = listOf(Color.Transparent, bgColor),
            ),
          ),
      )
    }
  }
}

@Composable
private fun TickerSinglePassRow(
  segments: List<TickerSegment>,
  textColor: Color,
  accentColor: Color,
  fontSizeSp: Int,
  fontWeight: FontWeight,
  fontStyle: FontStyle,
  thumbSizeDp: androidx.compose.ui.unit.Dp,
  modifier: Modifier = Modifier,
) {
  // Keep each Arabic sentence + inline images flowing naturally RTL inside the LTR gliding track
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Row(
      modifier = modifier.padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      segments.forEach { segment ->
        when (segment) {
          is TickerSegment.TextSegment -> {
            Text(
              text = segment.text,
              style = TextStyle(
                color = textColor,
                fontSize = fontSizeSp.sp,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                lineHeight = (fontSizeSp + 4).sp,
              ),
              maxLines = 1,
              softWrap = false,
            )
          }

          is TickerSegment.ImageSegment -> {
            SubcomposeAsyncImage(
              model = segment.url,
              contentDescription = "صورة مصغرة داخل الشريط الإعلاني",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(thumbSizeDp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp)),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(18.dp))
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.85f)),
      )
      Spacer(modifier = Modifier.width(18.dp))
    }
  }
}
