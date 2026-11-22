package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import com.example.R
import com.example.core.util.ArabicFormatter
import com.example.data.model.Category
import com.example.data.model.HomeCategoryStyle
import com.example.data.model.Product
import com.example.data.model.ProductCardShape
import com.example.data.model.ProductCardStyleConfig
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.sp

@Composable
fun ProductStoreCard(
  product: Product,
  isWholesaleApproved: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onAddToCart: (() -> Unit)? = null,
  isWishlisted: Boolean = false,
  onToggleWishlist: (() -> Unit)? = null,
  visibleColorsCount: Int = 0,
  visibleSizesCount: Int = 0,
  overrideRetailPrice: Double? = null,
  overrideDiscountPercent: Int? = null,
  cardStyle: ProductCardStyleConfig = LocalProductCardStyle.current,
) {
  val hasVisibleVariants = visibleColorsCount > 0 || visibleSizesCount > 0
  val displayRetailPrice = overrideRetailPrice ?: product.effectiveRetailPrice
  val displayDiscountPercent = overrideDiscountPercent ?: if (product.hasDiscount) product.discountPercent else 0
  val hasDisplayedDiscount = displayDiscountPercent > 0 && displayRetailPrice < product.retailPrice

  val outerShape = productCardOuterShape(cardStyle.shape, compact = false)
  val borderStroke = productCardBorderStroke(cardStyle, compact = false)
  val cardElevation = productCardElevation(cardStyle, compact = false)
  val dividerColor = productCardDividerColor(cardStyle)
  val isCircularCard = cardStyle.shape == ProductCardShape.CIRCULAR

  Card(
    modifier = modifier
      .clip(outerShape)
      .clickable(onClick = onClick)
      .testTag("product_card_${product.id}"),
    shape = outerShape,
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = borderStroke,
    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // 1. إطار الصورة العلوي المنظم بشارات غير متزاحمة
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.12f)
          .background(if (isCircularCard) PureWhite else Color(0xFFF6F7F9)),
      ) {
        if (isCircularCard) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              shape = CircleShape,
              color = Color(0xFFF6F7F9),
              border = borderStroke ?: BorderStroke(1.dp, BorderNeutral),
              modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f),
            ) {
              ProductThumbnail(
                imageUrl = product.imageUrls.firstOrNull(),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        } else {
          ProductThumbnail(
            imageUrl = product.imageUrls.firstOrNull(),
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize(),
          )
        }

        if (hasDisplayedDiscount) {
          ProductCardDiscountBadge(
            discountPercent = displayDiscountPercent,
            style = cardStyle.discountBadgeStyle,
            compact = false,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp),
          )
        } else if (product.isFeatured) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PrimaryYellow,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp),
          ) {
            Text(
              text = "مميز",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        if (onToggleWishlist != null) {
          Surface(
            shape = CircleShape,
            color = PureWhite.copy(alpha = 0.94f),
            border = BorderStroke(0.5.dp, BorderNeutral),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp)
              .size(34.dp),
          ) {
            IconButton(
              onClick = onToggleWishlist,
              modifier = Modifier
                .fillMaxSize()
                .testTag("product_wishlist_toggle_${product.id}"),
            ) {
              Icon(
                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isWishlisted) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                tint = if (isWishlisted) DiscountRed else CharcoalBlack,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        if (product.averageRating > 0.0) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PureWhite.copy(alpha = 0.95f),
            border = BorderStroke(0.5.dp, BorderNeutral),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = PrimaryYellow,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = String.format(java.util.Locale.US, "%.1f", product.averageRating),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
            }
          }
        }

        if (product.isOutOfStock) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DiscountRedSoft,
            border = BorderStroke(0.5.dp, DiscountRed.copy(alpha = 0.35f)),
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(8.dp),
          ) {
            Text(
              text = "نفد المخزون",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = DiscountRed,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        } else if (product.isLowStock) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PrimaryYellowSoft,
            border = BorderStroke(0.5.dp, PrimaryYellow),
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(8.dp),
          ) {
            Text(
              text = "متبقي ${product.stockQuantity}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }

      HorizontalDivider(color = dividerColor)

      // 2. بيانات المنتج والأسعار مقسمة بارتفاع موحد متناسق
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            lineHeight = 19.sp,
          ),
          color = TextPrimary,
          maxLines = 2,
          minLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = GeneralBackground,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            if (isWholesaleApproved && product.wholesalePrice > 0.0) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = ArabicFormatter.formatCurrency(product.wholesalePrice),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                  ),
                  color = CharcoalBlack,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                  shape = RoundedCornerShape(5.dp),
                  color = GoldenYellowSoft,
                  border = BorderStroke(0.5.dp, GoldenYellow),
                ) {
                  Text(
                    text = "جملة ${product.wholesaleMinQty}+",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                  )
                }
              }
              Text(
                text = "القطاعي: ${ArabicFormatter.formatCurrency(displayRetailPrice)} / ${product.unit}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = ArabicFormatter.formatCurrency(displayRetailPrice),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                  ),
                  color = CharcoalBlack,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = product.unit,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                  maxLines = 1,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                if (hasDisplayedDiscount) {
                  Text(
                    text = ArabicFormatter.formatCurrency(product.retailPrice),
                    style = MaterialTheme.typography.labelSmall.copy(
                      textDecoration = TextDecoration.LineThrough,
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                  )
                } else {
                  Text(
                    text = if (product.isOutOfStock) "غير متاح الآن" else "متوفر",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (product.isOutOfStock) DiscountRed else SuccessGreen,
                    maxLines = 1,
                  )
                }
                if (product.wholesalePrice > 0.0 && product.wholesaleMinQty > 0) {
                  Text(
                    text = "جملة من ${product.wholesaleMinQty}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }

        if (hasVisibleVariants) {
          val parts = buildList {
            if (visibleColorsCount > 0) add("$visibleColorsCount لون")
            if (visibleSizesCount > 0) add("$visibleSizesCount مقاس")
          }
          Text(
            text = "متوفر: ${parts.joinToString(" • ")}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = RoyalBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (onAddToCart != null) {
          Button(
            onClick = onAddToCart,
            enabled = !product.isOutOfStock,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = PrimaryYellow,
              contentColor = CharcoalBlack,
              disabledContainerColor = BorderNeutral,
              disabledContentColor = TextSecondary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(40.dp)
              .testTag("product_card_add_cart_${product.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.AddShoppingCart,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when {
                product.isOutOfStock -> "نفد المخزون"
                hasVisibleVariants -> "اختر الخيارات"
                else -> "أضف للسلة"
              },
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
fun CategoryQuickTile(
  category: Category,
  productCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: HomeCategoryStyle = HomeCategoryStyle.CIRCULAR,
  fallbackIndex: Int = 0,
) {
  val fallbackRes = resolveCategoryFallbackDrawable(category.name, fallbackIndex)

  when (style) {
    HomeCategoryStyle.PILL_CHIPS -> {
      Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderNeutral),
        modifier = modifier.testTag("home_category_tile_${category.id}"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = PureWhite,
            border = BorderStroke(1.dp, BorderNeutral),
          ) {
            CategoryArtworkBox(
              imageUrl = category.imageUrl,
              contentDescription = category.name,
              fallbackRes = fallbackRes,
              innerPadding = 4.dp,
              modifier = Modifier.fillMaxSize(),
            )
          }
          Column {
            Text(
              text = category.name,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (productCount > 0) {
              Text(
                text = "$productCount منتج",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }
          }
        }
      }
    }

    HomeCategoryStyle.NOON_PORTRAIT -> {
      Column(
        modifier = modifier
          .width(96.dp)
          .clip(RoundedCornerShape(16.dp))
          .clickable(onClick = onClick)
          .testTag("home_category_tile_${category.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f),
          shape = RoundedCornerShape(16.dp),
          color = if (!category.imageUrl.isNullOrBlank()) PureWhite else Color(0xFFF1F3F6),
          border = BorderStroke(1.dp, BorderNeutral.copy(alpha = 0.6f)),
        ) {
          CategoryArtworkBox(
            imageUrl = category.imageUrl,
            contentDescription = category.name,
            fallbackRes = fallbackRes,
            innerPadding = 5.dp,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Text(
          text = category.name,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
      }
    }

    HomeCategoryStyle.ROUNDED_SQUARE -> {
      Column(
        modifier = modifier
          .width(88.dp)
          .clip(RoundedCornerShape(16.dp))
          .clickable(onClick = onClick)
          .padding(vertical = 4.dp)
          .testTag("home_category_tile_${category.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          modifier = Modifier.size(68.dp),
          shape = RoundedCornerShape(16.dp),
          color = PureWhite,
          border = BorderStroke(1.5.dp, GoldenYellow),
        ) {
          CategoryArtworkBox(
            imageUrl = category.imageUrl,
            contentDescription = category.name,
            fallbackRes = fallbackRes,
            innerPadding = 5.dp,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Text(
          text = category.name,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
        if (productCount > 0) {
          Text(
            text = "$productCount منتج",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
      }
    }

    HomeCategoryStyle.DOUBLE_GRID -> {
      Column(
        modifier = modifier
          .clip(RoundedCornerShape(14.dp))
          .clickable(onClick = onClick)
          .padding(vertical = 4.dp)
          .testTag("home_category_tile_${category.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
          shape = RoundedCornerShape(14.dp),
          color = if (!category.imageUrl.isNullOrBlank()) PureWhite else Color(0xFFF1F3F6),
          border = BorderStroke(1.dp, BorderNeutral),
        ) {
          CategoryArtworkBox(
            imageUrl = category.imageUrl,
            contentDescription = category.name,
            fallbackRes = fallbackRes,
            innerPadding = 6.dp,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Text(
          text = category.name,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
      }
    }

    HomeCategoryStyle.CIRCULAR -> {
      Column(
        modifier = modifier
          .width(84.dp)
          .clip(RoundedCornerShape(AlEmanDimens.RadiusMd))
          .clickable(onClick = onClick)
          .padding(vertical = 2.dp)
          .testTag("home_category_tile_${category.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Surface(
          modifier = Modifier.size(66.dp),
          shape = CircleShape,
          color = PureWhite,
          border = BorderStroke(1.dp, BorderNeutral),
        ) {
          CategoryArtworkBox(
            imageUrl = category.imageUrl,
            contentDescription = category.name,
            fallbackRes = fallbackRes,
            innerPadding = 8.dp,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Text(
          text = category.name,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
        if (productCount > 0) {
          Text(
            text = "$productCount منتج",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
      }
    }
  }
}

@Composable
fun CategoryArtworkBox(
  imageUrl: String?,
  contentDescription: String,
  @DrawableRes fallbackRes: Int,
  modifier: Modifier = Modifier,
  innerPadding: androidx.compose.ui.unit.Dp = 5.dp,
) {
  if (!imageUrl.isNullOrBlank()) {
    Box(
      modifier = modifier.background(PureWhite),
      contentAlignment = Alignment.Center,
    ) {
      SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        loading = {
          Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        },
        error = {
          Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        },
      )
    }
  } else {
    Image(
      painter = painterResource(id = fallbackRes),
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = modifier,
    )
  }
}

@DrawableRes
fun resolveCategoryFallbackDrawable(categoryName: String, fallbackIndex: Int = 0): Int {
  val normalized = categoryName.trim().lowercase()
  return when {
    normalized.contains("إلكترون") || normalized.contains("الكترون") || normalized.contains("تلفاز") || normalized.contains("شاش") ->
      R.drawable.noon_cat_electronics
    normalized.contains("كبير") || normalized.contains("ثلاج") || normalized.contains("غسال") || normalized.contains("بوتاجاز") ->
      R.drawable.noon_cat_large_appliances
    normalized.contains("جمال") || normalized.contains("تجميل") || normalized.contains("عناي") || normalized.contains("مكياج") || normalized.contains("عطور") ->
      R.drawable.noon_cat_beauty
    normalized.contains("رجال") || normalized.contains("رجالي") ->
      R.drawable.noon_cat_men_fashion
    normalized.contains("نساء") || normalized.contains("نسائي") || normalized.contains("حريمي") || normalized.contains("سيدات") ->
      R.drawable.noon_cat_women_fashion
    normalized.contains("أطفال") || normalized.contains("اطفال") || normalized.contains("طفل") ->
      R.drawable.noon_cat_kids_fashion
    normalized.contains("موبايل") || normalized.contains("هاتف") || normalized.contains("جوال") || normalized.contains("تليفون") || normalized.contains("اكسسوار") ->
      R.drawable.noon_cat_mobiles
    normalized.contains("مطبخ") || normalized.contains("بيت") || (normalized.contains("منزل") && !normalized.contains("أجهز") && !normalized.contains("اجهز")) ->
      R.drawable.noon_cat_kitchen
    normalized.contains("صغير") || normalized.contains("أجهز") || normalized.contains("اجهز") || normalized.contains("خلاط") || normalized.contains("كاتل") ->
      R.drawable.noon_cat_small_appliances
    else -> FALLBACK_CATEGORY_DRAWABLES[fallbackIndex.mod(FALLBACK_CATEGORY_DRAWABLES.size)]
  }
}

private val FALLBACK_CATEGORY_DRAWABLES = listOf(
  R.drawable.noon_cat_electronics,
  R.drawable.noon_cat_large_appliances,
  R.drawable.noon_cat_beauty,
  R.drawable.noon_cat_men_fashion,
  R.drawable.noon_cat_women_fashion,
  R.drawable.noon_cat_kids_fashion,
  R.drawable.noon_cat_mobiles,
  R.drawable.noon_cat_kitchen,
  R.drawable.noon_cat_small_appliances,
)

@Composable
fun ProductThumbnail(
  imageUrl: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
) {
  if (!imageUrl.isNullOrBlank()) {
    SubcomposeAsyncImage(
      model = imageUrl,
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = modifier,
      loading = {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(GeneralBackground),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Inventory2,
            contentDescription = null,
            tint = RoyalBlue.copy(alpha = 0.35f),
            modifier = Modifier.size(34.dp),
          )
        }
      },
      error = {
        ProductFallbackBox(modifier = Modifier.fillMaxSize())
      },
    )
  } else {
    ProductFallbackBox(modifier = modifier)
  }
}

@Composable
private fun ProductFallbackBox(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.background(RoyalBlueSoft.copy(alpha = 0.55f)),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Inventory2,
        contentDescription = null,
        tint = RoyalBlue.copy(alpha = 0.65f),
        modifier = Modifier.size(36.dp),
      )
      Spacer(modifier = Modifier.height(2.dp))
    }
  }
}

@Composable
fun FullAspectBannerImage(
  imageUrl: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  placeholderMinHeight: Dp = 140.dp,
  placeholderBackground: Color = GeneralBackground,
  errorContent: @Composable () -> Unit = {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(placeholderMinHeight)
        .background(placeholderBackground),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Filled.Inventory2,
        contentDescription = null,
        tint = RoyalBlue.copy(alpha = 0.35f),
        modifier = Modifier.size(34.dp),
      )
    }
  },
) {
  if (imageUrl.isNullOrBlank()) {
    Box(modifier = modifier.fillMaxWidth()) {
      errorContent()
    }
    return
  }

  val context = LocalContext.current
  val request = remember(imageUrl) {
    ImageRequest.Builder(context)
      .data(imageUrl)
      .size(CoilSize.ORIGINAL)
      .crossfade(true)
      .build()
  }
  val painter = rememberAsyncImagePainter(model = request)
  val state = painter.state
  val intrinsicSize = painter.intrinsicSize
  val naturalRatio = if (
    intrinsicSize.isSpecified &&
    intrinsicSize.width > 0f &&
    intrinsicSize.height > 0f
  ) {
    (intrinsicSize.width / intrinsicSize.height).coerceIn(0.25f, 12f)
  } else {
    null
  }

  when {
    state is AsyncImagePainter.State.Error -> {
      Box(modifier = modifier.fillMaxWidth()) {
        errorContent()
      }
    }
    naturalRatio != null -> {
      Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
          .fillMaxWidth()
          .aspectRatio(naturalRatio),
      )
    }
    else -> {
      Box(
        modifier = modifier
          .fillMaxWidth()
          .height(placeholderMinHeight)
          .background(placeholderBackground),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          color = RoyalBlue,
          modifier = Modifier.size(28.dp),
          strokeWidth = 2.5.dp,
        )
      }
    }
  }
}
