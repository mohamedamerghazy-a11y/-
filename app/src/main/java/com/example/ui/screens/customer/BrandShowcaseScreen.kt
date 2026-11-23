package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.core.util.ArabicFormatter
import com.example.data.model.BrandShowcase
import com.example.data.model.BrandShowcaseCategory
import com.example.data.model.CompanyOfferBadgeStyle
import com.example.data.model.Product
import com.example.data.model.effectiveDiscountPercentWithShowcases
import com.example.data.model.effectiveRetailPriceWithShowcases
import com.example.data.model.ProductCardShape
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.CompanySpecialOfferBadge
import com.example.ui.components.LocalProductCardStyle
import com.example.ui.components.ProductCardDiscountBadge
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.productCardBorderStroke
import com.example.ui.components.productCardElevation
import com.example.ui.components.productCardImageClipShape
import com.example.ui.components.productCardOuterShape
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CatalogSortOption
import com.example.ui.viewmodel.CatalogUiState
import kotlin.math.roundToInt

private enum class BrandShowcaseFilterMode(val arabicLabel: String) {
  ALL("الكل"),
  OFFERS_ONLY("العروض فقط"),
  IN_STOCK_ONLY("المتوفر فقط"),
}

@Composable
fun BrandShowcaseScreen(
  showcaseId: String,
  catalogState: CatalogUiState,
  isWholesaleApproved: Boolean,
  feedbackMessage: String?,
  errorMessage: String?,
  onClearMessages: () -> Unit,
  onClearWishlistMessage: () -> Unit,
  onBackClick: () -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
  onToggleWishlist: (String) -> Unit,
) {
  val showcase = remember(catalogState.brandShowcases, catalogState.companies, showcaseId) {
    catalogState.resolveBrandShowcase(showcaseId)
  }
  var selectedCategoryId by rememberSaveable(showcaseId) { mutableStateOf<String?>(null) }
  var searchQuery by rememberSaveable(showcaseId) { mutableStateOf("") }
  var sortOption by rememberSaveable(showcaseId) { mutableStateOf(CatalogSortOption.NEWEST) }
  var filterMode by rememberSaveable(showcaseId) { mutableStateOf(BrandShowcaseFilterMode.ALL) }
  var showSortMenu by remember { mutableStateOf(false) }
  var showFilterMenu by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  val combinedFeedback = catalogState.wishlistMessage ?: feedbackMessage

  LaunchedEffect(combinedFeedback, errorMessage) {
    val msg = combinedFeedback ?: errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
      onClearWishlistMessage()
    }
  }

  val railCategories = remember(showcase, catalogState.products, catalogState.categories) {
    showcase?.let { catalogState.showcaseRailCategories(it) }.orEmpty()
  }

  val displayedProducts = remember(
    catalogState.products,
    catalogState.brandShowcases,
    showcaseId,
    selectedCategoryId,
    searchQuery,
    sortOption,
    filterMode,
    isWholesaleApproved,
  ) {
    val base = catalogState.productsForBrandShowcase(showcaseId, selectedCategoryId)
    val q = searchQuery.trim().lowercase()
    val filtered = base.filter { p ->
      val matchesQuery = q.isEmpty() ||
        p.name.lowercase().contains(q) ||
        p.description.lowercase().contains(q) ||
        p.sku.lowercase().contains(q)
      val effectiveDiscount = p.effectiveDiscountPercentWithShowcases(catalogState.brandShowcases)
      val matchesFilter = when (filterMode) {
        BrandShowcaseFilterMode.ALL -> true
        BrandShowcaseFilterMode.OFFERS_ONLY -> p.isOffer || effectiveDiscount > 0
        BrandShowcaseFilterMode.IN_STOCK_ONLY -> !p.isOutOfStock
      }
      matchesQuery && matchesFilter
    }
    when (sortOption) {
      CatalogSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
      CatalogSortOption.BESTSELLER -> filtered.sortedByDescending { it.salesCount }
      CatalogSortOption.PRICE_ASC -> filtered.sortedBy {
        if (isWholesaleApproved && it.wholesalePrice > 0.0) {
          it.wholesalePrice
        } else {
          it.effectiveRetailPriceWithShowcases(catalogState.brandShowcases)
        }
      }
      CatalogSortOption.PRICE_DESC -> filtered.sortedByDescending {
        if (isWholesaleApproved && it.wholesalePrice > 0.0) {
          it.wholesalePrice
        } else {
          it.effectiveRetailPriceWithShowcases(catalogState.brandShowcases)
        }
      }
      CatalogSortOption.HIGHEST_RATED -> filtered.sortedByDescending { it.averageRating }
    }
  }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
      containerColor = PureWhite,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
      topBar = {
        Surface(color = PureWhite, shadowElevation = 1.dp) {
          Column(modifier = Modifier.statusBarsPadding()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = PureWhite,
                border = BorderStroke(1.dp, BorderNeutral),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("brand_showcase_search_bar"),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("brand_showcase_back_button"),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "رجوع",
                      tint = CharcoalBlack,
                    )
                  }

                  Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                  ) {
                    if (searchQuery.isEmpty()) {
                      Text(
                        text = "دوّر على ${showcase?.title ?: "منتجات الشركة"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    BasicTextField(
                      value = searchQuery,
                      onValueChange = { searchQuery = it },
                      singleLine = true,
                      textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                      ),
                      modifier = Modifier.fillMaxWidth(),
                    )
                  }

                  if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                      Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "مسح البحث",
                        tint = TextSecondary,
                      )
                    }
                  } else {
                    Box(
                      modifier = Modifier.size(40.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp),
                      )
                    }
                  }
                }
              }
            }
            HorizontalDivider(color = BorderNeutral)
          }
        }
      },
    ) { innerPadding ->
      if (showcase == null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
          contentAlignment = Alignment.Center,
        ) {
          StoreEmptyStateCard(
            icon = Icons.Filled.Storefront,
            title = "عرض الشركة غير متوفر حاليًا",
            description = "ربما تم إيقاف هذا العرض مؤقتًا من الإدارة.",
            actionLabel = "العودة للمتجر",
            onActionClick = onBackClick,
          )
        }
        return@Scaffold
      }

      // RTL Row: First child is on the Right side (Vertical Category Rail), Second child fills remaining width on the Left
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(PureWhite),
      ) {
        // 1. العمود الأيمن: الكل بالأعلى + كل فئة من فئات الشركة تحت بعضها بصورتها
        Column(
          modifier = Modifier
            .width(78.dp)
            .fillMaxHeight()
            .background(Color(0xFFFAFAFC))
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
            .testTag("brand_showcase_category_rail"),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          BrandRailAllItem(
            isSelected = selectedCategoryId == null,
            onClick = { selectedCategoryId = null },
          )

          railCategories.forEach { cat ->
            BrandRailCategoryItem(
              category = cat,
              isSelected = selectedCategoryId == cat.id,
              onClick = { selectedCategoryId = cat.id },
            )
          }
        }

        VerticalDivider(color = BorderNeutral)

        // 2. المنطقة الرئيسية (ترويسة الشركة + شريط الترتيب والتصنيف + شبكة المنتجات)
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(PureWhite),
        ) {
          BrandShowcaseCompanyHeaderBanner(
            showcase = showcase,
            productsCount = displayedProducts.size,
            offerBadgeStyle = catalogState.storeSettings.companyOfferBadgeStyleEnum,
          )

          // شريط الترتيب والتصنيف
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box {
              Surface(
                onClick = { showSortMenu = true },
                shape = RoundedCornerShape(10.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderNeutral),
                modifier = Modifier
                  .height(40.dp)
                  .testTag("brand_showcase_sort_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "الترتيب",
                    tint = CharcoalBlack,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = if (sortOption == CatalogSortOption.NEWEST) "الترتيب" else sortOption.arabicLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                    maxLines = 1,
                  )
                }
              }
              DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                containerColor = PureWhite,
              ) {
                CatalogSortOption.entries.forEach { option ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = option.arabicLabel,
                        fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                      )
                    },
                    onClick = {
                      sortOption = option
                      showSortMenu = false
                    },
                  )
                }
              }
            }

            Box {
              Surface(
                onClick = { showFilterMenu = true },
                shape = RoundedCornerShape(10.dp),
                color = if (filterMode == BrandShowcaseFilterMode.ALL) PureWhite else PrimaryYellow.copy(alpha = 0.28f),
                border = BorderStroke(1.dp, if (filterMode == BrandShowcaseFilterMode.ALL) BorderNeutral else PrimaryYellow),
                modifier = Modifier
                  .height(40.dp)
                  .testTag("brand_showcase_filter_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "التصنيف",
                    tint = CharcoalBlack,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = if (filterMode == BrandShowcaseFilterMode.ALL) "التصنيف" else filterMode.arabicLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                    maxLines = 1,
                  )
                }
              }
              DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false },
                containerColor = PureWhite,
              ) {
                BrandShowcaseFilterMode.entries.forEach { mode ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = mode.arabicLabel,
                        fontWeight = if (mode == filterMode) FontWeight.Bold else FontWeight.Normal,
                      )
                    },
                    onClick = {
                      filterMode = mode
                      showFilterMenu = false
                    },
                  )
                }
              }
            }
          }

          if (displayedProducts.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              contentAlignment = Alignment.Center,
            ) {
              StoreEmptyStateCard(
                icon = Icons.Filled.Inventory2,
                title = "لا توجد منتجات في هذه الفئة",
                description = "اختر فئة أخرى من القائمة الجانبية أو امسح نص البحث.",
                actionLabel = if (selectedCategoryId != null || searchQuery.isNotEmpty() || filterMode != BrandShowcaseFilterMode.ALL) {
                  "عرض الكل"
                } else {
                  null
                },
                onActionClick = {
                  selectedCategoryId = null
                  searchQuery = ""
                  filterMode = BrandShowcaseFilterMode.ALL
                },
              )
            }
          } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
              val gridColumns = if (maxWidth >= 520.dp) {
                GridCells.Adaptive(minSize = 150.dp)
              } else {
                GridCells.Fixed(2)
              }
              LazyVerticalGrid(
                columns = gridColumns,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
              ) {
                items(displayedProducts, key = { it.id }) { product ->
                  val colorsCount = if (catalogState.storeSettings.showProductColors) product.availableColors.size else 0
                  val sizesCount = if (catalogState.storeSettings.showProductSizes) product.availableSizes.size else 0
                  BrandShowcaseGridProductCard(
                    product = product,
                    brandShowcases = catalogState.brandShowcases,
                    isWholesaleApproved = isWholesaleApproved,
                    isWishlisted = product.id in catalogState.wishlistProductIds,
                    onClick = { onOpenProduct(product.id) },
                    onToggleWishlist = { onToggleWishlist(product.id) },
                    onQuickAdd = {
                      if (colorsCount > 0 || sizesCount > 0) {
                        onOpenProduct(product.id)
                      } else {
                        onAddToCart(product)
                      }
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BrandRailAllItem(
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp)
      .testTag("brand_rail_all"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // شريط التحديد الجانبي على الحافة اليمنى في RTL
    Box(
      modifier = Modifier
        .width(3.5.dp)
        .height(54.dp)
        .background(
          color = if (isSelected) Color(0xFFE53935) else Color.Transparent,
          shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
        ),
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFDECEF) else PureWhite,
        border = BorderStroke(
          width = if (isSelected) 1.5.dp else 1.dp,
          color = if (isSelected) SuccessGreen else BorderNeutral,
        ),
        modifier = Modifier.size(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.ShoppingBasket,
            contentDescription = "الكل",
            tint = SuccessGreen,
            modifier = Modifier.size(24.dp),
          )
        }
      }
      Text(
        text = "الكل",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        ),
        color = if (isSelected) Color(0xFFD32F2F) else TextPrimary,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun BrandRailCategoryItem(
  category: BrandShowcaseCategory,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp)
      .testTag("brand_rail_cat_${category.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .width(3.5.dp)
        .height(48.dp)
        .background(
          color = if (isSelected) Color(0xFFE53935) else Color.Transparent,
          shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
        ),
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(
          width = if (isSelected) 1.5.dp else 1.dp,
          color = if (isSelected) Color(0xFFE53935) else BorderNeutral,
        ),
        modifier = Modifier.size(48.dp),
      ) {
        if (category.imageUrl.isNotBlank()) {
          SubcomposeAsyncImage(
            model = category.imageUrl,
            contentDescription = category.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .padding(4.dp),
            error = {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = TextSecondary)
              }
            },
          )
        } else {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = category.name.take(2),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
          }
        }
      }
      Text(
        text = category.name,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          lineHeight = 14.sp,
        ),
        color = if (isSelected) Color(0xFFD32F2F) else TextSecondary,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun BrandShowcaseCompanyHeaderBanner(
  showcase: BrandShowcase,
  productsCount: Int,
  offerBadgeStyle: CompanyOfferBadgeStyle,
) {
  val accentColor = remember(showcase.accentColorHex) {
    val clean = showcase.accentColorHex.trim()
    if (!clean.startsWith("#")) Color(0xFFFCE4EC)
    else try {
      Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
      Color(0xFFFCE4EC)
    }
  }
  val discountPct = showcase.cleanDiscountPercent

  Surface(
    color = Color.Transparent,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(14.dp))
      .border(1.dp, CharcoalBlack.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              accentColor.copy(alpha = 0.92f),
              accentColor.copy(alpha = 0.45f),
            ),
          ),
        )
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderNeutral),
        modifier = Modifier.size(42.dp),
      ) {
        if (!showcase.companyImageUrl.isNullOrBlank()) {
          SubcomposeAsyncImage(
            model = showcase.companyImageUrl,
            contentDescription = showcase.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .padding(4.dp),
          )
        } else {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
              text = showcase.title.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
          }
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = showcase.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = CharcoalBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val subText = showcase.subtitle?.takeIf { it.isNotBlank() }
          ?: "${ArabicFormatter.formatNumber(productsCount)} منتج متوفر"
        Text(
          text = subText,
          style = MaterialTheme.typography.labelSmall,
          color = CharcoalBlack.copy(alpha = 0.75f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (discountPct > 0) {
        CompanySpecialOfferBadge(
          discountPercent = discountPct,
          style = offerBadgeStyle,
          compact = true,
        )
      }
    }
  }
}

@Composable
private fun BrandShowcaseGridProductCard(
  product: Product,
  brandShowcases: List<BrandShowcase>,
  isWholesaleApproved: Boolean,
  isWishlisted: Boolean,
  onClick: () -> Unit,
  onToggleWishlist: () -> Unit,
  onQuickAdd: () -> Unit,
) {
  val retailWithShowcase = remember(product, brandShowcases) {
    product.effectiveRetailPriceWithShowcases(brandShowcases)
  }
  val displayDiscountPct = remember(product, brandShowcases) {
    product.effectiveDiscountPercentWithShowcases(brandShowcases)
  }
  val hasDisplayedDiscount = displayDiscountPct > 0 && retailWithShowcase < product.retailPrice
  val effectivePrice = if (isWholesaleApproved && product.wholesalePrice > 0.0) {
    product.wholesalePrice
  } else {
    retailWithShowcase
  }

  val cardStyle = LocalProductCardStyle.current
  val outerShape = productCardOuterShape(cardStyle.shape, compact = false)
  val borderStroke = productCardBorderStroke(cardStyle, compact = false)
  val cardElevation = productCardElevation(cardStyle, compact = false)
  val isCircularCard = cardStyle.shape == ProductCardShape.CIRCULAR
  val imageShape = productCardImageClipShape(cardStyle.shape, compact = true)

  Surface(
    onClick = onClick,
    shape = outerShape,
    color = PureWhite,
    border = borderStroke,
    shadowElevation = cardElevation,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("brand_grid_product_${product.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(0.95f)
          .clip(if (isCircularCard) CircleShape else imageShape)
          .background(Color(0xFFF6F7FA)),
      ) {
        ProductThumbnail(
          imageUrl = product.imageUrls.firstOrNull(),
          contentDescription = product.name,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        )

        if (hasDisplayedDiscount && !isWholesaleApproved) {
          ProductCardDiscountBadge(
            discountPercent = displayDiscountPct,
            style = cardStyle.discountBadgeStyle,
            compact = true,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(6.dp),
          )
        }

      IconButton(
        onClick = onToggleWishlist,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(36.dp),
      ) {
        Icon(
          imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
          contentDescription = if (isWishlisted) "إزالة من المفضلة" else "إضافة إلى المفضلة",
          tint = if (isWishlisted) DiscountRed else TextSecondary,
          modifier = Modifier.size(18.dp),
        )
      }

      if (!product.isOutOfStock) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
            .size(48.dp)
            .clickable(onClick = onQuickAdd)
            .testTag("brand_grid_quick_add_${product.id}"),
          contentAlignment = Alignment.BottomEnd,
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = PureWhite,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, BorderNeutral),
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "إضافة للسلة",
                tint = Color(0xFFB71C1C),
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = CharcoalBlack.copy(alpha = 0.85f),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(6.dp),
        ) {
          Text(
            text = "نفدت الكمية",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PureWhite,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
    }

    Text(
      text = product.name,
      style = MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeight = 17.sp,
      ),
      color = TextPrimary,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 2.dp),
    )

    if (product.unit.isNotBlank()) {
      Text(
        text = product.unit,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 2.dp),
      )
    }

    Row(
      modifier = Modifier.padding(horizontal = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = ArabicFormatter.formatCurrency(effectivePrice),
        style = MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.5.sp,
        ),
        color = CharcoalBlack,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
        if (hasDisplayedDiscount && !isWholesaleApproved) {
          Text(
            text = ArabicFormatter.formatNumber(product.retailPrice.toInt()),
            style = MaterialTheme.typography.labelSmall.copy(
              textDecoration = TextDecoration.LineThrough,
              fontSize = 10.sp,
            ),
            color = TextSecondary,
            maxLines = 1,
          )
        }
      }
    }
  }
}
