package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.ArabicFormatter
import com.example.data.model.ProductCardBorderStyle
import com.example.data.model.ProductCardShape
import com.example.data.model.ProductCardStyleConfig
import com.example.data.model.ProductDiscountBadgeStyle
import com.example.data.model.sanitizeCardHexColor
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalProductCardStyle = compositionLocalOf { ProductCardStyleConfig() }

val PRODUCT_CARD_CUSTOM_COLOR_SWATCHES = listOf(
  "#D4AF37",
  "#E67E22",
  "#E91E63",
  "#00897B",
  "#0288D1",
  "#5E35B1",
  "#C62828",
  "#2E7D32",
  "#455A64",
  "#8D6E63",
)

fun parseCardHexColor(hex: String?, fallback: Color = Color(0xFFD4AF37)): Color {
  val safe = sanitizeCardHexColor(hex)
  return try {
    Color(android.graphics.Color.parseColor(safe))
  } catch (_: Exception) {
    fallback
  }
}

fun productCardOuterShape(shape: ProductCardShape, compact: Boolean = false): Shape {
  return when (shape) {
    ProductCardShape.ROUNDED -> RoundedCornerShape(if (compact) 14.dp else 16.dp)
    ProductCardShape.CIRCULAR -> RoundedCornerShape(if (compact) 22.dp else 28.dp)
    ProductCardShape.RECTANGLE -> RoundedCornerShape(3.dp)
    ProductCardShape.CUT_CORNER -> CutCornerShape(if (compact) 12.dp else 16.dp)
    ProductCardShape.ASYMMETRIC -> RoundedCornerShape(
      topStart = if (compact) 20.dp else 26.dp,
      topEnd = if (compact) 4.dp else 5.dp,
      bottomEnd = if (compact) 20.dp else 26.dp,
      bottomStart = if (compact) 4.dp else 5.dp,
    )
    ProductCardShape.ARCH -> RoundedCornerShape(
      topStart = if (compact) 24.dp else 32.dp,
      topEnd = if (compact) 24.dp else 32.dp,
      bottomEnd = if (compact) 8.dp else 10.dp,
      bottomStart = if (compact) 8.dp else 10.dp,
    )
  }
}

fun productCardImageClipShape(shape: ProductCardShape, compact: Boolean = false): Shape {
  return when (shape) {
    ProductCardShape.ROUNDED -> RoundedCornerShape(if (compact) 10.dp else 0.dp)
    ProductCardShape.CIRCULAR -> CircleShape
    ProductCardShape.RECTANGLE -> RoundedCornerShape(2.dp)
    ProductCardShape.CUT_CORNER -> CutCornerShape(if (compact) 9.dp else 12.dp)
    ProductCardShape.ASYMMETRIC -> RoundedCornerShape(
      topStart = if (compact) 16.dp else 22.dp,
      topEnd = 4.dp,
      bottomEnd = if (compact) 16.dp else 18.dp,
      bottomStart = 4.dp,
    )
    ProductCardShape.ARCH -> RoundedCornerShape(
      topStart = if (compact) 20.dp else 28.dp,
      topEnd = if (compact) 20.dp else 28.dp,
      bottomEnd = 6.dp,
      bottomStart = 6.dp,
    )
  }
}

fun productCardBorderStroke(
  config: ProductCardStyleConfig,
  compact: Boolean = false,
): BorderStroke? {
  val accentWidth = if (compact) 1.7.dp else 2.2.dp
  return when (config.borderStyle) {
    ProductCardBorderStyle.DEFAULT -> BorderStroke(1.dp, BorderNeutral)
    ProductCardBorderStyle.GOLD -> BorderStroke(
      width = accentWidth,
      brush = Brush.linearGradient(
        colors = listOf(
          Color(0xFFF9E58B),
          Color(0xFFD4AF37),
          Color(0xFF9C6D11),
          Color(0xFFEAC857),
        ),
      ),
    )
    ProductCardBorderStyle.EMERALD -> BorderStroke(
      width = accentWidth,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF065F46)),
      ),
    )
    ProductCardBorderStyle.ROYAL_BLUE -> BorderStroke(
      width = accentWidth,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8), Color(0xFF1E3A8A)),
      ),
    )
    ProductCardBorderStyle.RUBY -> BorderStroke(
      width = accentWidth,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFFF87171), Color(0xFFDC2626), Color(0xFF991B1B)),
      ),
    )
    ProductCardBorderStyle.PURPLE -> BorderStroke(
      width = accentWidth,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFFA78BFA), Color(0xFF7C3AED), Color(0xFF5B21B6)),
      ),
    )
    ProductCardBorderStyle.CHARCOAL -> BorderStroke(
      width = if (compact) 1.5.dp else 1.8.dp,
      color = CharcoalBlack,
    )
    ProductCardBorderStyle.CUSTOM -> {
      val base = parseCardHexColor(config.safeCustomBorderHex)
      val lighter = Color(
        red = (base.red + 0.16f).coerceAtMost(1f),
        green = (base.green + 0.16f).coerceAtMost(1f),
        blue = (base.blue + 0.16f).coerceAtMost(1f),
        alpha = 1f,
      )
      val darker = Color(
        red = (base.red * 0.72f).coerceAtLeast(0f),
        green = (base.green * 0.72f).coerceAtLeast(0f),
        blue = (base.blue * 0.72f).coerceAtLeast(0f),
        alpha = 1f,
      )
      BorderStroke(
        width = accentWidth,
        brush = Brush.linearGradient(listOf(lighter, base, darker)),
      )
    }
    ProductCardBorderStyle.NONE -> null
  }
}

fun productCardElevation(config: ProductCardStyleConfig, compact: Boolean = false): Dp {
  return when (config.borderStyle) {
    ProductCardBorderStyle.NONE -> if (compact) 3.dp else 4.dp
    ProductCardBorderStyle.GOLD -> if (compact) 2.dp else 3.dp
    ProductCardBorderStyle.DEFAULT -> if (compact) 2.dp else 0.dp
    else -> if (compact) 2.dp else 1.5.dp
  }
}

fun productCardDividerColor(config: ProductCardStyleConfig): Color {
  return when (config.borderStyle) {
    ProductCardBorderStyle.GOLD -> Color(0xFFD4AF37).copy(alpha = 0.35f)
    ProductCardBorderStyle.EMERALD -> Color(0xFF059669).copy(alpha = 0.25f)
    ProductCardBorderStyle.ROYAL_BLUE -> Color(0xFF1D4ED8).copy(alpha = 0.25f)
    ProductCardBorderStyle.RUBY -> Color(0xFFDC2626).copy(alpha = 0.25f)
    ProductCardBorderStyle.PURPLE -> Color(0xFF7C3AED).copy(alpha = 0.25f)
    ProductCardBorderStyle.CUSTOM -> parseCardHexColor(config.safeCustomBorderHex).copy(alpha = 0.28f)
    ProductCardBorderStyle.NONE -> BorderNeutral.copy(alpha = 0.45f)
    else -> BorderNeutral.copy(alpha = 0.7f)
  }
}

@Composable
fun ProductCardDiscountBadge(
  discountPercent: Int,
  style: ProductDiscountBadgeStyle,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  if (discountPercent <= 0) return
  val formattedPct = ArabicFormatter.formatNumber(discountPercent)
  val labelText = if (compact) "-$formattedPct%" else "خصم $formattedPct%"

  when (style) {
    ProductDiscountBadgeStyle.PILL -> {
      Surface(
        shape = RoundedCornerShape(if (compact) 7.dp else 9.dp),
        color = DiscountRed,
        shadowElevation = 1.dp,
        modifier = modifier,
      ) {
        Text(
          text = labelText,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 10.sp else 11.sp,
          ),
          color = PureWhite,
          maxLines = 1,
          modifier = Modifier.padding(
            horizontal = if (compact) 6.dp else 8.dp,
            vertical = if (compact) 2.dp else 3.5.dp,
          ),
        )
      }
    }

    ProductDiscountBadgeStyle.RIBBON -> {
      // Swallowtail ribbon banner with gold inner accent line
      Box(
        modifier = modifier.defaultMinSize(minHeight = if (compact) 20.dp else 24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val w = size.width
          val h = size.height
          val notch = (h * 0.36f).coerceAtMost(w * 0.22f)
          val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w - notch, h / 2f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
          }
          drawPath(
            path = path,
            brush = Brush.horizontalGradient(
              listOf(Color(0xFFB71C1C), Color(0xFFE53935), Color(0xFFFF5252)),
            ),
          )
          drawLine(
            color = Color(0xFFF9E58B).copy(alpha = 0.75f),
            start = Offset(0f, h - 2.dp.toPx()),
            end = Offset(w - notch * 0.5f, h - 2.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
          )
        }
        Text(
          text = labelText,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 9.5.sp else 10.5.sp,
          ),
          color = PureWhite,
          maxLines = 1,
          modifier = Modifier.padding(
            start = if (compact) 6.dp else 8.dp,
            end = if (compact) 11.dp else 14.dp,
            top = if (compact) 2.dp else 3.dp,
            bottom = if (compact) 2.5.dp else 4.dp,
          ),
        )
      }
    }

    ProductDiscountBadgeStyle.STARBURST -> {
      val badgeSize = if (compact) 36.dp else 44.dp
      Box(
        modifier = modifier.size(badgeSize),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val cx = size.width / 2f
          val cy = size.height / 2f
          val outerR = size.minDimension / 2f
          val innerR = outerR * 0.76f
          val points = 12
          val path = Path()
          for (i in 0 until points * 2) {
            val angle = (i * PI / points) - (PI / 2.0)
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          path.close()
          drawPath(
            path = path,
            brush = Brush.radialGradient(
              colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F), Color(0xFF991B1B)),
              center = Offset(cx, cy),
              radius = outerR,
            ),
          )
          drawPath(
            path = path,
            color = Color(0xFFFFD54F),
            style = Stroke(width = 1.2.dp.toPx()),
          )
        }
        Text(
          text = if (compact) "-$formattedPct%" else "$formattedPct%\nخصم",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 8.5.sp else 9.5.sp,
            lineHeight = if (compact) 9.sp else 10.5.sp,
          ),
          color = PureWhite,
          textAlign = TextAlign.Center,
          maxLines = if (compact) 1 else 2,
        )
      }
    }

    ProductDiscountBadgeStyle.PRICE_TAG -> {
      Box(
        modifier = modifier.defaultMinSize(minHeight = if (compact) 21.dp else 25.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val w = size.width
          val h = size.height
          val tip = (h * 0.42f).coerceAtMost(w * 0.24f)
          val path = Path().apply {
            moveTo(tip, 0f)
            lineTo(w - 4.dp.toPx(), 0f)
            lineTo(w, 4.dp.toPx())
            lineTo(w, h - 4.dp.toPx())
            lineTo(w - 4.dp.toPx(), h)
            lineTo(tip, h)
            lineTo(0f, h / 2f)
            close()
          }
          drawPath(
            path = path,
            brush = Brush.horizontalGradient(
              listOf(Color(0xFF047857), Color(0xFF059669), Color(0xFF10B981)),
            ),
          )
          // Tag eyelet punch-hole
          drawCircle(
            color = PureWhite,
            radius = if (compact) 2.2.dp.toPx() else 2.6.dp.toPx(),
            center = Offset(tip * 0.72f, h / 2f),
          )
        }
        Text(
          text = labelText,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 9.5.sp else 10.5.sp,
          ),
          color = PureWhite,
          maxLines = 1,
          modifier = Modifier.padding(
            start = if (compact) 12.dp else 15.dp,
            end = if (compact) 6.dp else 8.dp,
            top = if (compact) 2.dp else 3.dp,
            bottom = if (compact) 2.dp else 3.dp,
          ),
        )
      }
    }

    ProductDiscountBadgeStyle.GOLD_LUXURY -> {
      Surface(
        shape = RoundedCornerShape(50),
        color = CharcoalBlack,
        border = BorderStroke(
          width = 1.2.dp,
          brush = Brush.linearGradient(
            listOf(Color(0xFFF9E58B), Color(0xFFD4AF37), Color(0xFFB38728)),
          ),
        ),
        shadowElevation = 2.dp,
        modifier = modifier,
      ) {
        Row(
          modifier = Modifier.padding(
            horizontal = if (compact) 6.dp else 8.dp,
            vertical = if (compact) 2.dp else 3.dp,
          ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = PrimaryYellow,
            modifier = Modifier.size(if (compact) 10.dp else 12.dp),
          )
          Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              fontSize = if (compact) 9.5.sp else 10.5.sp,
            ),
            color = Color(0xFFF9E58B),
            maxLines = 1,
          )
        }
      }
    }

    ProductDiscountBadgeStyle.CAPSULE_OUTLINE -> {
      Surface(
        shape = RoundedCornerShape(50),
        color = PureWhite.copy(alpha = 0.96f),
        border = BorderStroke(1.5.dp, DiscountRed),
        shadowElevation = 1.dp,
        modifier = modifier,
      ) {
        Row(
          modifier = Modifier.padding(
            horizontal = if (compact) 6.dp else 8.dp,
            vertical = if (compact) 2.dp else 3.dp,
          ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          if (!compact) {
            Icon(
              imageVector = Icons.Filled.LocalOffer,
              contentDescription = null,
              tint = DiscountRed,
              modifier = Modifier.size(11.dp),
            )
          }
          Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              fontSize = if (compact) 9.5.sp else 10.5.sp,
            ),
            color = DiscountRed,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
fun ProductCardBorderSwatch(
  style: ProductCardBorderStyle,
  customHex: String,
  modifier: Modifier = Modifier,
) {
  val stroke = productCardBorderStroke(
    ProductCardStyleConfig(borderStyle = style, customBorderHex = customHex),
    compact = false,
  )
  Box(
    modifier = modifier
      .size(26.dp)
      .clip(CircleShape)
      .background(
        when (style) {
          ProductCardBorderStyle.GOLD -> Brush.linearGradient(
            listOf(Color(0xFFF9E58B), Color(0xFFD4AF37), Color(0xFF9C6D11)),
          )
          ProductCardBorderStyle.EMERALD -> Brush.linearGradient(
            listOf(Color(0xFF34D399), Color(0xFF059669)),
          )
          ProductCardBorderStyle.ROYAL_BLUE -> Brush.linearGradient(
            listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)),
          )
          ProductCardBorderStyle.RUBY -> Brush.linearGradient(
            listOf(Color(0xFFF87171), Color(0xFFDC2626)),
          )
          ProductCardBorderStyle.PURPLE -> Brush.linearGradient(
            listOf(Color(0xFFA78BFA), Color(0xFF7C3AED)),
          )
          ProductCardBorderStyle.CHARCOAL -> Brush.linearGradient(
            listOf(CharcoalBlack, Color(0xFF3A3F4B)),
          )
          ProductCardBorderStyle.CUSTOM -> {
            val c = parseCardHexColor(customHex)
            Brush.linearGradient(listOf(c, c))
          }
          ProductCardBorderStyle.DEFAULT -> Brush.linearGradient(
            listOf(Color(0xFFF3F4F6), Color(0xFFD1D5DB)),
          )
          ProductCardBorderStyle.NONE -> Brush.linearGradient(
            listOf(PureWhite, Color(0xFFF9FAFB)),
          )
        },
      )
      .then(
        if (stroke != null) {
          Modifier.border(stroke, CircleShape)
        } else {
          Modifier.border(1.dp, BorderNeutral, CircleShape)
        },
      ),
  )
}
