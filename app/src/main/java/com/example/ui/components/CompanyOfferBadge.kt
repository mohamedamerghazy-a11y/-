package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.ArabicFormatter
import com.example.data.model.CompanyOfferBadgeStyle
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite

/**
 * شارة «عرض خاص» المخصصة لعروض الشركات مع 5 تصميمات مميزة يمكن للتاجر التبديل بينها في أي وقت من الإعدادات.
 */
@Composable
fun CompanySpecialOfferBadge(
  discountPercent: Int,
  style: CompanyOfferBadgeStyle,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  if (discountPercent <= 0) return
  val formattedPct = ArabicFormatter.formatNumber(discountPercent)
  val tagModifier = modifier.testTag("company_offer_badge_${style.code}")

  when (style) {
    CompanyOfferBadgeStyle.GOLD_TICKET -> {
      // 1. تذكرة ذهبية فاخرة بخلفية داكنة وحواف ذهبية وشريط نسبة بارز
      val shape = RoundedCornerShape(10.dp)
      Surface(
        shape = shape,
        color = Color(0xFF171A22),
        border = BorderStroke(1.2.dp, Color(0xFFFFD000)),
        shadowElevation = 3.dp,
        modifier = tagModifier,
      ) {
        Box(modifier = Modifier.height(IntrinsicSize.Min)) {
          Canvas(modifier = Modifier.matchParentSize()) {
            val notchR = size.height * 0.22f
            drawCircle(
              color = Color(0xFFFFD000).copy(alpha = 0.22f),
              radius = notchR,
              center = Offset(0f, size.height / 2f),
            )
            drawCircle(
              color = Color(0xFFFFD000).copy(alpha = 0.22f),
              radius = notchR,
              center = Offset(size.width, size.height / 2f),
            )
          }
          Row(
            modifier = Modifier.padding(
              horizontal = if (compact) 8.dp else 10.dp,
              vertical = if (compact) 4.dp else 5.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = Color(0xFFFFD000),
              modifier = Modifier.size(if (compact) 12.dp else 14.dp),
            )
            if (!compact) {
              Text(
                text = "عرض خاص",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                ),
                color = PureWhite,
                maxLines = 1,
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFFFD000))
                .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
              Text(
                text = "خصم $formattedPct٪",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = if (compact) 10.sp else 11.sp,
                ),
                color = CharcoalBlack,
                maxLines = 1,
              )
            }
          }
        }
      }
    }

    CompanyOfferBadgeStyle.CRIMSON_RIBBON -> {
      // 2. شريط عروض ناري بتدرج أحمر وبرتقالي ولمعة مائلة
      val shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp)
      Surface(
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 3.dp,
        modifier = tagModifier
          .clip(shape)
          .background(
            Brush.horizontalGradient(
              colors = listOf(Color(0xFFC62828), Color(0xFFE53935), Color(0xFFFB8C00)),
            ),
          )
          .border(1.dp, PureWhite.copy(alpha = 0.45f), shape),
      ) {
        Box {
          Canvas(modifier = Modifier.matchParentSize()) {
            val sheen = Path().apply {
              moveTo(size.width * 0.12f, 0f)
              lineTo(size.width * 0.28f, 0f)
              lineTo(size.width * 0.16f, size.height)
              lineTo(0f, size.height)
              close()
            }
            drawPath(path = sheen, color = Color.White.copy(alpha = 0.14f))
          }
          Row(
            modifier = Modifier.padding(
              horizontal = if (compact) 8.dp else 10.dp,
              vertical = if (compact) 4.dp else 5.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.LocalOffer,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(if (compact) 12.dp else 13.dp),
            )
            if (!compact) {
              Text(
                text = "عرض خاص",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 11.sp,
                ),
                color = PureWhite,
                maxLines = 1,
              )
            }
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(PureWhite)
                .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
              Text(
                text = "-$formattedPct٪",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = if (compact) 10.sp else 11.sp,
                ),
                color = Color(0xFFC62828),
                maxLines = 1,
              )
            }
          }
        }
      }
    }

    CompanyOfferBadgeStyle.EMERALD_GLASS -> {
      // 3. شارة زمردية عصرية بكبسولة ناعمة ونسبة خصم بيضاء
      val shape = CircleShape
      Surface(
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 2.dp,
        modifier = tagModifier
          .clip(shape)
          .background(
            Brush.horizontalGradient(
              colors = listOf(Color(0xFF005B4F), Color(0xFF00897B), Color(0xFF00BFA5)),
            ),
          )
          .border(1.dp, Color(0xFFA7FFEB).copy(alpha = 0.7f), shape),
      ) {
        Row(
          modifier = Modifier.padding(
            horizontal = if (compact) 8.dp else 10.dp,
            vertical = if (compact) 4.dp else 5.dp,
          ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            tint = Color(0xFFFFEA00),
            modifier = Modifier.size(if (compact) 13.dp else 14.dp),
          )
          if (!compact) {
            Text(
              text = "عرض خاص",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
              ),
              color = PureWhite,
              maxLines = 1,
            )
          }
          Box(
            modifier = Modifier
              .clip(CircleShape)
              .background(PureWhite.copy(alpha = 0.94f))
              .padding(horizontal = 6.dp, vertical = 1.dp),
          ) {
            Text(
              text = "خصم $formattedPct٪",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (compact) 10.sp else 10.5.sp,
              ),
              color = Color(0xFF00695C),
              maxLines = 1,
            )
          }
        }
      }
    }

    CompanyOfferBadgeStyle.ROYAL_SHIELD -> {
      // 4. وسام ملكي بنفسجي بإطار ذهبي ونجمة معتمدة
      val shape = CutCornerShape(8.dp)
      Surface(
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 3.dp,
        modifier = tagModifier
          .clip(shape)
          .background(
            Brush.horizontalGradient(
              colors = listOf(Color(0xFF1F1663), Color(0xFF4527A0), Color(0xFF7B1FA2)),
            ),
          )
          .border(1.2.dp, Color(0xFFFFD54F), shape),
      ) {
        Row(
          modifier = Modifier.padding(
            horizontal = if (compact) 9.dp else 11.dp,
            vertical = if (compact) 4.dp else 5.dp,
          ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            tint = Color(0xFFFFD54F),
            modifier = Modifier.size(if (compact) 13.dp else 14.dp),
          )
          Text(
            text = if (compact) "خصم $formattedPct٪" else "عرض خاص • $formattedPct٪",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              fontSize = if (compact) 10.5.sp else 11.sp,
            ),
            color = Color(0xFFFFF8E1),
            maxLines = 1,
          )
        }
      }
    }

    CompanyOfferBadgeStyle.SUNBURST_STAMP -> {
      // 5. ختم خصم أصفر جريء بحدود داكنة تلفت النظر فورًا
      val shape = RoundedCornerShape(10.dp)
      Surface(
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 2.dp,
        modifier = tagModifier
          .clip(shape)
          .background(
            Brush.horizontalGradient(
              colors = listOf(PrimaryYellow, Color(0xFFFFC107)),
            ),
          )
          .border(1.5.dp, CharcoalBlack, shape),
      ) {
        Row(
          modifier = Modifier.padding(
            horizontal = if (compact) 7.dp else 9.dp,
            vertical = if (compact) 3.5.dp else 4.5.dp,
          ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          if (!compact) {
            Text(
              text = "عرض خاص",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
              ),
              color = CharcoalBlack,
              maxLines = 1,
            )
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(CharcoalBlack)
              .padding(horizontal = 6.dp, vertical = 1.5.dp),
          ) {
            Text(
              text = "-$formattedPct٪",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = if (compact) 10.sp else 11.sp,
              ),
              color = PrimaryYellow,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
