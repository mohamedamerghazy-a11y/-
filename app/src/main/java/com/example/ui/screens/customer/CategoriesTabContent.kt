package com.example.ui.screens.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CategoriesTabStyle
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.effectiveDiscountPercentWithShowcases
import com.example.data.model.effectiveRetailPriceWithShowcases
import com.example.ui.components.AlEmanCard
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.CategoryArtworkBox
import com.example.ui.components.ProductStoreCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.resolveCategoryFallbackDrawable
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CatalogUiState
import kotlin.math.cos
import kotlin.math.sin

private val NoonCategoryCardSurface = Color(0xFFF1F3F6)

private val HexagonShape: Shape = GenericShape { size, _ ->
  val cx = size.width / 2f
  val cy = size.height / 2f
  val radius = minOf(cx, cy) * 0.96f
  for (i in 0 until 6) {
    val angleRad = Math.toRadians((60 * i - 90).toDouble())
    val x = cx + (radius * cos(angleRad)).toFloat()
    val y = cy + (radius * sin(angleRad)).toFloat()
    if (i == 0) moveTo(x, y) else lineTo(x, y)
  }
  close()
}

@Composable
fun CategoriesTabContent(
  catalogState: CatalogUiState,
  selectedMainCategoryId: String?,
  isWholesaleApproved: Boolean,
  onSelectMainCategory: (String?) -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
  onExploreHome: () -> Unit,
  onToggleWishlist: (String) -> Unit = {},
) {
  val mainCategories = catalogState.mainCategories
  val matchedSelectedCategory = selectedMainCategoryId?.let { id ->
    catalogState.categories.firstOrNull { it.id == id }
  }
  val activeMainCategory = when {
    matchedSelectedCategory == null -> null
    matchedSelectedCategory.isMainCategory -> matchedSelectedCategory
    else -> mainCategories.firstOrNull { it.id == matchedSelectedCategory.parentId }
  }
  val initialSubCategoryId = matchedSelectedCategory?.takeUnless { it.isMainCategory }?.id
  val displayStyle = catalogState.storeSettings.categoriesTabStyleEnum

  if (mainCategories.isEmpty()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(PureWhite)
        .padding(AlEmanDimens.SpaceLg),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(id = R.string.nav_categories),
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp,
        ),
        color = CharcoalBlack,
      )
      StoreEmptyStateCard(
        icon = Icons.Filled.Category,
        title = stringResource(id = R.string.categories_empty_title),
        description = stringResource(id = R.string.categories_empty_desc),
        actionLabel = stringResource(id = R.string.nav_home),
        onActionClick = onExploreHome,
      )
    }
    return
  }

  if (activeMainCategory == null) {
    CategoriesOverviewGrid(
      categories = mainCategories,
      products = catalogState.storefrontProducts,
      style = displayStyle,
      onCategoryClick = { category -> onSelectMainCategory(category.id) },
    )
  } else {
    BackHandler {
      onSelectMainCategory(null)
    }
    CategoryProductsDetailContent(
      catalogState = catalogState,
      mainCategories = mainCategories,
      activeMainCategory = activeMainCategory,
      initialSubCategoryId = initialSubCategoryId,
      isWholesaleApproved = isWholesaleApproved,
      onSelectMainCategory = onSelectMainCategory,
      onBackToAllCategories = { onSelectMainCategory(null) },
      onOpenProduct = onOpenProduct,
      onAddToCart = onAddToCart,
      onToggleWishlist = onToggleWishlist,
    )
  }
}

@Composable
private fun CategoriesOverviewGrid(
  categories: List<Category>,
  products: List<Product>,
  style: CategoriesTabStyle,
  onCategoryClick: (Category) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(PureWhite)
      .padding(horizontal = AlEmanDimens.SpaceLg, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = stringResource(id = R.string.nav_categories),
      style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
      ),
      color = CharcoalBlack,
      modifier = Modifier.fillMaxWidth(),
    )

    if (style == CategoriesTabStyle.DETAILED_CARDS) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        categories.forEachIndexed { index, category ->
          val count = products.count { it.categoryId == category.id }
          DetailedCategoryHorizontalCard(
            category = category,
            productCount = count,
            fallbackIndex = index,
            onClick = { onCategoryClick(category) },
          )
        }
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        categories.chunked(3).forEachIndexed { rowIndex, rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            rowItems.forEachIndexed { colIndex, category ->
              val absoluteIndex = rowIndex * 3 + colIndex
              CategoryStyledGridTile(
                category = category,
                style = style,
                fallbackIndex = absoluteIndex,
                onClick = { onCategoryClick(category) },
                modifier = Modifier.weight(1f),
              )
            }
            repeat(3 - rowItems.size) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
  }
}

@Composable
private fun CategoryStyledGridTile(
  category: Category,
  style: CategoriesTabStyle,
  fallbackIndex: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val fallbackRes = resolveCategoryFallbackDrawable(category.name, fallbackIndex)

  val frameShape: Shape = when (style) {
    CategoriesTabStyle.CIRCULAR_GRID -> CircleShape
    CategoriesTabStyle.HEXAGON_GRID -> HexagonShape
    CategoriesTabStyle.CUT_CORNER_GRID -> CutCornerShape(topStart = 20.dp, bottomEnd = 20.dp, topEnd = 6.dp, bottomStart = 6.dp)
    else -> RoundedCornerShape(16.dp)
  }

  val frameAspect = when (style) {
    CategoriesTabStyle.CIRCULAR_GRID,
    CategoriesTabStyle.HEXAGON_GRID -> 1f
    CategoriesTabStyle.CUT_CORNER_GRID -> 0.88f
    else -> 0.76f
  }

  val frameBorder = when (style) {
    CategoriesTabStyle.CIRCULAR_GRID -> BorderStroke(2.dp, GoldenYellow)
    CategoriesTabStyle.HEXAGON_GRID -> BorderStroke(2.dp, PrimaryYellow)
    CategoriesTabStyle.CUT_CORNER_GRID -> BorderStroke(1.5.dp, BorderNeutral)
    else -> null
  }

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("category_card_${category.id}"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(frameAspect),
      shape = frameShape,
      color = if (!category.imageUrl.isNullOrBlank()) PureWhite else NoonCategoryCardSurface,
      border = frameBorder ?: if (!category.imageUrl.isNullOrBlank()) BorderStroke(1.dp, BorderNeutral) else null,
    ) {
      CategoryArtworkBox(
        imageUrl = category.imageUrl,
        contentDescription = category.name,
        fallbackRes = fallbackRes,
        innerPadding = if (
          style == CategoriesTabStyle.CIRCULAR_GRID || style == CategoriesTabStyle.HEXAGON_GRID
        ) 9.dp else 5.dp,
        modifier = Modifier.fillMaxSize(),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = category.name,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
      ),
      color = CharcoalBlack,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 2.dp),
    )
  }
}

@Composable
private fun DetailedCategoryHorizontalCard(
  category: Category,
  productCount: Int,
  fallbackIndex: Int,
  onClick: () -> Unit,
) {
  val fallbackRes = resolveCategoryFallbackDrawable(category.name, fallbackIndex)
  AlEmanCard(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("category_card_${category.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        modifier = Modifier.size(68.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (!category.imageUrl.isNullOrBlank()) PureWhite else NoonCategoryCardSurface,
        border = if (!category.imageUrl.isNullOrBlank()) BorderStroke(1.dp, BorderNeutral) else null,
      ) {
        CategoryArtworkBox(
          imageUrl = category.imageUrl,
          contentDescription = category.name,
          fallbackRes = fallbackRes,
          innerPadding = 4.dp,
          modifier = Modifier.fillMaxSize(),
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = category.name,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!category.description.isNullOrBlank()) {
          Text(
            text = category.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BrandPillBadge(
          text = "$productCount منتج متوفر",
          containerColor = PrimaryYellow.copy(alpha = 0.25f),
          contentColor = CharcoalBlack,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        contentDescription = null,
        tint = TextSecondary,
      )
    }
  }
}

@Composable
private fun CategoryProductsDetailContent(
  catalogState: CatalogUiState,
  mainCategories: List<Category>,
  activeMainCategory: Category,
  initialSubCategoryId: String? = null,
  isWholesaleApproved: Boolean,
  onSelectMainCategory: (String?) -> Unit,
  onBackToAllCategories: () -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
  onToggleWishlist: (String) -> Unit,
) {
  val activeMainId = activeMainCategory.id
  var selectedSubCategoryId by rememberSaveable(activeMainId, initialSubCategoryId) {
    mutableStateOf(initialSubCategoryId)
  }
  val subCategories = catalogState.subCategoriesFor(activeMainId)
  val displayedProducts = catalogState.storefrontProducts.filter { product ->
    product.categoryId == activeMainId &&
      (selectedSubCategoryId == null || product.subCategoryId == selectedSubCategoryId)
  }
  val activeCategoryFallbackRes = resolveCategoryFallbackDrawable(
    categoryName = activeMainCategory.name,
    fallbackIndex = mainCategories.indexOfFirst { it.id == activeMainId }.coerceAtLeast(0),
  )

  Column(
    modifier = Modifier.padding(top = 8.dp, bottom = AlEmanDimens.SpaceMd),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // رأس الفئة المنسق والمصغر: زر رجوع + صورة الفئة المصغرة + اسم الفئة فقط (بدون شريط باقي الفئات)
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderNeutral),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AlEmanDimens.SpaceLg),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onBackToAllCategories)
            .testTag("categories_back_to_grid_button"),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = CircleShape,
            color = NoonCategoryCardSurface,
            modifier = Modifier.size(32.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back_button_desc),
                tint = CharcoalBlack,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = PureWhite,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.size(34.dp),
        ) {
          CategoryArtworkBox(
            imageUrl = activeMainCategory.imageUrl,
            contentDescription = activeMainCategory.name,
            fallbackRes = activeCategoryFallbackRes,
            innerPadding = 3.dp,
            modifier = Modifier.fillMaxSize(),
          )
        }

        Text(
          text = activeMainCategory.name,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          ),
          color = CharcoalBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )

        Surface(
          shape = RoundedCornerShape(50),
          color = PrimaryYellow.copy(alpha = 0.22f),
          modifier = Modifier.padding(end = 6.dp),
        ) {
          Text(
            text = "${displayedProducts.size} منتج",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }

    if (!activeMainCategory.description.isNullOrBlank()) {
      AlEmanCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = AlEmanDimens.SpaceLg),
      ) {
        Text(
          text = activeMainCategory.description,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(14.dp),
        )
      }
    }

    if (subCategories.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "الأقسام الفرعية",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = TextSecondary,
          modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
        )
        LazyRow(
          contentPadding = PaddingValues(horizontal = AlEmanDimens.SpaceLg),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            FilterChip(
              selected = selectedSubCategoryId == null,
              onClick = { selectedSubCategoryId = null },
              label = { Text("الكل") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GoldenYellowSoft,
                selectedLabelColor = TextPrimary,
                containerColor = PureWhite,
                labelColor = TextSecondary,
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedSubCategoryId == null,
                borderColor = BorderNeutral,
                selectedBorderColor = RoyalBlue,
              ),
            )
          }
          items(subCategories, key = { it.id }) { sub ->
            val selected = selectedSubCategoryId == sub.id
            FilterChip(
              selected = selected,
              onClick = { selectedSubCategoryId = sub.id },
              label = { Text(sub.name) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RoyalBlueSoft,
                selectedLabelColor = RoyalBlue,
                containerColor = PureWhite,
                labelColor = TextPrimary,
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = BorderNeutral,
                selectedBorderColor = RoyalBlue,
              ),
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

      if (displayedProducts.isEmpty()) {
        StoreEmptyStateCard(
          icon = Icons.Filled.Storefront,
          title = "لا توجد منتجات في هذه الفئة حاليًا",
          description = "جرّب اختيار فئة أخرى أو تصفح الصفحة الرئيسية.",
          actionLabel = "كل الفئات",
          onActionClick = onBackToAllCategories,
        )
      } else {
        displayedProducts.chunked(2).forEach { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            pair.forEach { product ->
              val colorsCount = if (catalogState.storeSettings.showProductColors) product.availableColors.size else 0
              val sizesCount = if (catalogState.storeSettings.showProductSizes) product.availableSizes.size else 0
              val effectiveRetail = product.effectiveRetailPriceWithShowcases(catalogState.brandShowcases)
              val effectiveDiscountPct = product.effectiveDiscountPercentWithShowcases(catalogState.brandShowcases)
              ProductStoreCard(
                product = product,
                isWholesaleApproved = isWholesaleApproved,
                onClick = { onOpenProduct(product.id) },
                isWishlisted = catalogState.isWishlisted(product.id),
                onToggleWishlist = { onToggleWishlist(product.id) },
                visibleColorsCount = colorsCount,
                visibleSizesCount = sizesCount,
                overrideRetailPrice = effectiveRetail,
                overrideDiscountPercent = effectiveDiscountPct,
                onAddToCart = {
                  if (colorsCount > 0 || sizesCount > 0) {
                    onOpenProduct(product.id)
                  } else {
                    onAddToCart(product)
                  }
                },
                modifier = Modifier.weight(1f),
              )
            }
            if (pair.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}
