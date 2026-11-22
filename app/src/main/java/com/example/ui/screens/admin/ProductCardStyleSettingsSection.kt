package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.ProductCardBorderStyle
import com.example.data.model.ProductCardShape
import com.example.data.model.ProductCardStyleConfig
import com.example.data.model.ProductDiscountBadgeStyle
import com.example.data.model.sanitizeCardHexColor
import com.example.ui.components.PRODUCT_CARD_CUSTOM_COLOR_SWATCHES
import com.example.ui.components.ProductCardBorderSwatch
import com.example.ui.components.ProductCardDiscountBadge
import com.example.ui.components.ProductStoreCard
import com.example.ui.components.SuccessBadge
import com.example.ui.components.parseCardHexColor
import com.example.ui.components.productCardBorderStroke
import com.example.ui.components.productCardOuterShape
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductCardStyleSettingsContent(
  borderStyle: ProductCardBorderStyle,
  customBorderHex: String,
  cardShape: ProductCardShape,
  discountBadgeStyle: ProductDiscountBadgeStyle,
  isSubmitting: Boolean,
  onBorderStyleChange: (ProductCardBorderStyle) -> Unit,
  onCustomBorderHexChange: (String) -> Unit,
  onCardShapeChange: (ProductCardShape) -> Unit,
  onDiscountBadgeStyleChange: (ProductDiscountBadgeStyle) -> Unit,
  onSaveClick: () -> Unit,
) {
  val liveConfig = remember(borderStyle, customBorderHex, cardShape, discountBadgeStyle) {
    ProductCardStyleConfig(
      borderStyle = borderStyle,
      customBorderHex = sanitizeCardHexColor(customBorderHex),
      shape = cardShape,
      discountBadgeStyle = discountBadgeStyle,
    )
  }

  val sampleProduct = remember {
    Product(
      id = "preview_sample",
      name = "مسحوق غسيل أوتوماتيك 5 كيلو - عرض التوفير",
      retailPrice = 250.0,
      retailDiscountPrice = 185.0,
      wholesalePrice = 165.0,
      wholesaleMinQty = 6,
      unit = "عبوة",
      stockQuantity = 24,
      averageRating = 4.9,
      reviewCount = 38,
      isFeatured = true,
      isOffer = true,
    )
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Live Preview Stage
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = GeneralBackground,
      border = BorderStroke(1.dp, BorderNeutral),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = RoyalBlue,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "معاينة مباشرة لشكل كارت المنتج",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
          }
          Text(
            text = "يتطبق على الأندرويد والويب",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }

        Box(
          modifier = Modifier.widthIn(max = 212.dp),
          contentAlignment = Alignment.Center,
        ) {
          ProductStoreCard(
            product = sampleProduct,
            isWholesaleApproved = false,
            onClick = {},
            onAddToCart = {},
            isWishlisted = true,
            onToggleWishlist = {},
            cardStyle = liveConfig,
          )
        }
      }
    }

    // 1. Border Style & Color
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(Icons.Filled.BorderColor, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
      Text(
        text = "1) إطار ولون حدود كارت المنتج",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ProductCardBorderStyle.entries.chunked(2).forEach { rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          rowItems.forEach { option ->
            val selected = borderStyle == option
            Surface(
              onClick = { onBorderStyleChange(option) },
              shape = RoundedCornerShape(12.dp),
              color = if (selected) PrimaryYellowSoft else PureWhite,
              border = BorderStroke(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) CharcoalBlack else BorderNeutral,
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("product_card_border_option_${option.code}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ProductCardBorderSwatch(
                  style = option,
                  customHex = customBorderHex,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                  Text(
                    text = option.arabicLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
          if (rowItems.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }

    if (borderStyle == ProductCardBorderStyle.CUSTOM) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = GeneralBackground,
        border = BorderStroke(1.dp, BorderNeutral),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "اختر لون الإطار المخصص أو أدخل كود اللون (HEX):",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PRODUCT_CARD_CUSTOM_COLOR_SWATCHES.forEach { hex ->
              val isPicked = sanitizeCardHexColor(customBorderHex).equals(hex, ignoreCase = true)
              val swatchColor = parseCardHexColor(hex)
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(swatchColor)
                  .border(
                    width = if (isPicked) 2.5.dp else 1.dp,
                    color = if (isPicked) CharcoalBlack else PureWhite,
                    shape = CircleShape,
                  )
                  .clickable { onCustomBorderHexChange(hex) }
                  .testTag("custom_border_swatch_${hex.removePrefix("#")}"),
                contentAlignment = Alignment.Center,
              ) {
                if (isPicked) {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "اللون المختار",
                    tint = PureWhite,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
          OutlinedTextField(
            value = customBorderHex,
            onValueChange = { onCustomBorderHexChange(it.take(9)) },
            label = { Text("كود اللون المخصص (مثال: #D4AF37)") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("custom_border_hex_input"),
          )
        }
      }
    }

    HorizontalDivider(color = BorderNeutral)

    // 2. Card Shape / Geometry
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(Icons.Filled.RoundedCorner, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
      Text(
        text = "2) شكل الكارت (دائري / مستطيل / هندسي)",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ProductCardShape.entries.chunked(2).forEach { rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          rowItems.forEach { option ->
            val selected = cardShape == option
            val miniShape = productCardOuterShape(option, compact = true)
            val miniBorder = productCardBorderStroke(liveConfig, compact = true)
              ?: BorderStroke(1.2.dp, CharcoalBlack.copy(alpha = 0.45f))
            Surface(
              onClick = { onCardShapeChange(option) },
              shape = RoundedCornerShape(12.dp),
              color = if (selected) PrimaryYellowSoft else PureWhite,
              border = BorderStroke(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) CharcoalBlack else BorderNeutral,
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("product_card_shape_option_${option.code}"),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Surface(
                    shape = miniShape,
                    color = PureWhite,
                    border = miniBorder,
                    modifier = Modifier.size(width = 34.dp, height = 28.dp),
                  ) {}
                  Text(
                    text = option.arabicLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                }
                Text(
                  text = option.subtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
          if (rowItems.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }

    HorizontalDivider(color = BorderNeutral)

    // 3. Discount Badge Shape
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
      Text(
        text = "3) شكل شارة الخصم على المنتج",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ProductDiscountBadgeStyle.entries.chunked(2).forEach { rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          rowItems.forEach { option ->
            val selected = discountBadgeStyle == option
            Surface(
              onClick = { onDiscountBadgeStyleChange(option) },
              shape = RoundedCornerShape(12.dp),
              color = if (selected) PrimaryYellowSoft else PureWhite,
              border = BorderStroke(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) CharcoalBlack else BorderNeutral,
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("product_discount_badge_option_${option.code}"),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = option.arabicLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  if (selected) {
                    SuccessBadge("مفعل")
                  }
                }
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GeneralBackground),
                  contentAlignment = Alignment.Center,
                ) {
                  ProductCardDiscountBadge(
                    discountPercent = 25,
                    style = option,
                    compact = false,
                  )
                }
                Text(
                  text = option.subtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
          if (rowItems.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }

    Button(
      onClick = onSaveClick,
      enabled = !isSubmitting,
      shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
      colors = ButtonDefaults.buttonColors(
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("admin_save_product_card_style_button"),
    ) {
      if (isSubmitting) {
        CircularProgressIndicator(color = CharcoalBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = "حفظ تصميم كروت المنتجات",
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
