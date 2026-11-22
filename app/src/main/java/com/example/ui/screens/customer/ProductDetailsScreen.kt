package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.util.ArabicFormatter
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.ProductStoreCard
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.QuantityStepperPill
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowSoft
import com.example.data.model.effectiveDiscountPercentWithShowcases
import com.example.data.model.effectiveRetailPriceWithShowcases
import com.example.data.model.effectiveUnitPriceWithShowcases
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CatalogUiState
import com.example.ui.viewmodel.ProductReviewsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailsScreen(
  productId: String,
  catalogState: CatalogUiState,
  isWholesaleApproved: Boolean,
  cartQuantity: Int,
  feedbackMessage: String?,
  errorMessage: String?,
  onClearMessages: () -> Unit,
  onProductViewed: (String) -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product, Int, String?, String?) -> Unit,
  onBackClick: () -> Unit,
  reviewsState: ProductReviewsUiState = ProductReviewsUiState(isLoading = false),
  currentUserId: String? = null,
  onSubmitReview: (productName: String, rating: Int, comment: String) -> Unit = { _, _, _ -> },
  onDeleteReview: (reviewId: String) -> Unit = {},
  onRequireSignIn: () -> Unit = {},
  onToggleWishlist: (String) -> Unit = {},
  onClearWishlistMessage: () -> Unit = {},
  onToggleStockAlert: (Product) -> Unit = {},
  onClearStockAlertMessage: () -> Unit = {},
) {
  val product = remember(catalogState.products, productId) {
    catalogState.products.firstOrNull { it.id == productId }
  }
  val storeSettings = catalogState.storeSettings
  val showColors = storeSettings.showProductColors && (product?.availableColors?.isNotEmpty() == true)
  val showSizes = storeSettings.showProductSizes && (product?.availableSizes?.isNotEmpty() == true)
  val isWishlisted = catalogState.isWishlisted(productId)
  val isStockAlertEnabled = catalogState.isStockAlertEnabled(productId)

  var selectedColor by remember(productId, showColors, product?.availableColors) {
    mutableStateOf(if (showColors) product?.availableColors?.firstOrNull() else null)
  }
  var selectedSize by remember(productId, showSizes, product?.availableSizes) {
    mutableStateOf(if (showSizes) product?.availableSizes?.firstOrNull() else null)
  }

  LaunchedEffect(productId, product != null) {
    if (product != null) {
      onProductViewed(productId)
    }
  }

  var zoomImageIndex by remember { mutableStateOf<Int?>(null) }
  var selectedQty by remember(productId) { mutableIntStateOf(1) }
  var showReviewDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  val combinedFeedback = catalogState.stockAlertMessage
    ?: catalogState.wishlistMessage
    ?: feedbackMessage
    ?: reviewsState.feedbackMessage
  val combinedError = errorMessage ?: reviewsState.errorMessage

  LaunchedEffect(combinedFeedback, combinedError) {
    val msg = combinedFeedback ?: combinedError
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
      onClearWishlistMessage()
      onClearStockAlertMessage()
    }
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = product?.name ?: "تفاصيل المنتج",
        subtitle = product?.let { catalogState.categoryById[it.categoryId]?.name } ?: "الإيمان للتجارة",
        showBack = true,
        onBackClick = onBackClick,
        trailingContent = {
          if (product != null) {
            IconButton(
              onClick = { onToggleWishlist(product.id) },
              modifier = Modifier.testTag("details_wishlist_button"),
            ) {
              Icon(
                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isWishlisted) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                tint = if (isWishlisted) DiscountRed else CharcoalBlack,
              )
            }
          }
        },
      )
    },
    bottomBar = {
      if (product != null) {
        Surface(
          color = PureWhite,
          shadowElevation = 6.dp,
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = BorderNeutral)
            val totalPreviewUnits = cartQuantity + selectedQty
            val activeUnitPrice = product.effectiveUnitPriceWithShowcases(
              isWholesaleApproved = isWholesaleApproved,
              quantity = totalPreviewUnits,
              brandShowcases = catalogState.brandShowcases,
            )
            val isWholesaleAppliedNow = isWholesaleApproved &&
              product.wholesalePrice > 0.0 &&
              totalPreviewUnits >= product.wholesaleMinQty

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AlEmanDimens.SpaceLg, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (!product.isOutOfStock) {
                QuantityStepperPill(
                  quantity = selectedQty,
                  canIncrement = selectedQty < product.stockQuantity,
                  onDecrement = { if (selectedQty > 1) selectedQty-- },
                  onIncrement = { if (selectedQty < product.stockQuantity) selectedQty++ },
                  tagPrefix = "details_qty",
                )
              }

              if (product.isOutOfStock) {
                Button(
                  onClick = { onToggleStockAlert(product) },
                  shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStockAlertEnabled) SuccessGreen else PrimaryYellow,
                    contentColor = if (isStockAlertEnabled) PureWhite else CharcoalBlack,
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("details_stock_alert_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isStockAlertEnabled) "مفعّل: سننبّهك عند التوفر ✓" else "نبّهني عند التوفر",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  )
                }
              } else {
                Button(
                  onClick = {
                    onAddToCart(
                      product,
                      selectedQty,
                      selectedColor?.takeIf { showColors },
                      selectedSize?.takeIf { showSizes },
                    )
                  },
                  shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = PureWhite,
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("details_add_to_cart_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.AddShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "أضف للسلة • ${ArabicFormatter.formatCurrency(activeUnitPrice * selectedQty)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  )
                }
              }
            }

            if (cartQuantity > 0 || isWholesaleAppliedNow) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = AlEmanDimens.SpaceLg, end = AlEmanDimens.SpaceLg, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                if (cartQuantity > 0) {
                  Text(
                    text = "متواجد في السلة حاليًا: $cartQuantity ${product.unit}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = RoyalBlue,
                  )
                } else {
                  Spacer(modifier = Modifier.width(1.dp))
                }
                if (isWholesaleAppliedNow) {
                  Text(
                    text = "سعر الجملة مفعّل",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                  )
                }
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (product == null) {
        Column(
          modifier = Modifier
            .widthIn(max = 600.dp)
            .padding(AlEmanDimens.SpaceLg),
        ) {
          StoreEmptyStateCard(
            icon = Icons.Filled.Inventory2,
            title = "المنتج غير متاح حاليًا",
            description = "ربما تم إخفاء المنتج أو إزالته من الكتالوج مؤقتًا.",
            actionLabel = "العودة للمتجر",
            onActionClick = onBackClick,
          )
        }
      } else {
        val categoryName = catalogState.categoryById[product.categoryId]?.name.orEmpty()
        val subCategoryName = product.subCategoryId?.let { catalogState.categoryById[it]?.name }.orEmpty()
        val similarProducts = remember(catalogState.products, product) {
          catalogState.similarProducts(product)
        }

        LazyColumn(
          modifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxSize()
            .testTag("product_details_lazy_column"),
          contentPadding = PaddingValues(vertical = AlEmanDimens.SpaceLg),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // 1. معرض الصور المتعدد القابل للتقليب والتكبير
          item {
            ProductImageGalleryCard(
              product = product,
              displayDiscountPercent = product.effectiveDiscountPercentWithShowcases(catalogState.brandShowcases),
              onImageClick = { idx -> zoomImageIndex = idx },
              modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
            )
          }

          // 2. العنوان والتصنيف والحالة والتقييم
          item {
            AlEmanCard(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AlEmanDimens.SpaceLg),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  val breadcrumb = listOf(categoryName, subCategoryName)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                  if (breadcrumb.isNotBlank()) {
                    BrandPillBadge(
                      text = breadcrumb,
                      containerColor = RoyalBlueSoft,
                      contentColor = RoyalBlue,
                    )
                  }
                  when {
                    product.isOutOfStock -> DiscountBadge(text = "نفد المخزون")
                    product.isLowStock -> BrandPillBadge(
                      text = "متبقي ${product.stockQuantity} فقط",
                      containerColor = GoldenYellowSoft,
                      contentColor = TextPrimary,
                    )
                    else -> SuccessBadge(text = "متوفر في المخزون")
                  }
                }

                Text(
                  text = product.name,
                  style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimary,
                )

                Row(
                  horizontalArrangement = Arrangement.spacedBy(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  val displayAvg = if (reviewsState.reviewCount > 0) reviewsState.averageRating else product.averageRating
                  val displayCount = if (reviewsState.reviewCount > 0) reviewsState.reviewCount else product.reviewCount
                  if (displayAvg > 0.0) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = GoldenYellow,
                        modifier = Modifier.size(18.dp),
                      )
                      Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", displayAvg)} ($displayCount تقييم)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                      )
                    }
                  }
                  Text(
                    text = "وحدة البيع: ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                  )
                }
              }
            }
          }

          // 3. بطاقة التسعير المزدوج (قطاعي + جملة)
          item {
            ProductPricingCard(
              product = product,
              effectiveRetailPrice = product.effectiveRetailPriceWithShowcases(catalogState.brandShowcases),
              effectiveDiscountPercent = product.effectiveDiscountPercentWithShowcases(catalogState.brandShowcases),
              isWholesaleApproved = isWholesaleApproved,
              modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
            )
          }

          if (product.isOutOfStock) {
            item {
              Surface(
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                color = if (isStockAlertEnabled) SuccessGreen.copy(alpha = 0.1f) else GoldenYellowSoft,
                border = BorderStroke(
                  width = 1.dp,
                  color = if (isStockAlertEnabled) SuccessGreen else GoldenYellow,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = AlEmanDimens.SpaceLg)
                  .clickable { onToggleStockAlert(product) },
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = if (isStockAlertEnabled) SuccessGreen else CharcoalBlack,
                    modifier = Modifier.size(22.dp),
                  )
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = if (isStockAlertEnabled) {
                        "تنبيه التوفر مفعّل لهذا المنتج ✓"
                      } else {
                        "المنتج غير متوفر حاليًا — نبّهني عند التوفر"
                      },
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (isStockAlertEnabled) SuccessGreen else CharcoalBlack,
                    )
                    Text(
                      text = if (isStockAlertEnabled) {
                        "سنرسل لك إشعارًا فوريًا بمجرد إعادة توفيره. اضغط لإلغاء التنبيه."
                      } else {
                        "اضغط لتسجيل اهتمامك وسنُشعرك فور عودته للمخزون."
                      },
                      style = MaterialTheme.typography.bodySmall,
                      color = TextPrimary,
                    )
                  }
                }
              }
            }
          }

          // 3.5 بطاقة اختيار الألوان والمقاسات (تظهر حسب إعدادات المتجر)
          if (showColors || showSizes) {
            item {
              AlEmanCard(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = AlEmanDimens.SpaceLg),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  if (showColors) {
                    Text(
                      text = "اختر اللون",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      product.availableColors.forEach { colorName ->
                        val isSelected = selectedColor == colorName
                        FilterChip(
                          selected = isSelected,
                          onClick = { selectedColor = colorName },
                          label = {
                            Text(
                              text = colorName,
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                          },
                          colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryYellow,
                            selectedLabelColor = CharcoalBlack,
                            containerColor = GeneralBackground,
                            labelColor = TextPrimary,
                          ),
                          border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderNeutral,
                            selectedBorderColor = CharcoalBlack,
                          ),
                          modifier = Modifier.testTag("details_color_chip_$colorName"),
                        )
                      }
                    }
                  }

                  if (showSizes) {
                    if (showColors) {
                      HorizontalDivider(color = BorderNeutral)
                    }
                    Text(
                      text = "اختر المقاس / الحجم",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      product.availableSizes.forEach { sizeName ->
                        val isSelected = selectedSize == sizeName
                        FilterChip(
                          selected = isSelected,
                          onClick = { selectedSize = sizeName },
                          label = {
                            Text(
                              text = sizeName,
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                          },
                          colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue,
                            selectedLabelColor = PureWhite,
                            containerColor = GeneralBackground,
                            labelColor = TextPrimary,
                          ),
                          border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderNeutral,
                            selectedBorderColor = RoyalBlue,
                          ),
                          modifier = Modifier.testTag("details_size_chip_$sizeName"),
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // 4. الوصف والمواصفات والأكواد (SKU / الباركود / QR)
          item {
            AlEmanCard(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AlEmanDimens.SpaceLg),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "وصف المنتج والمواصفات",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = RoyalBlue,
                )
                Text(
                  text = product.description.ifBlank { "لا يوجد وصف إضافي لهذا المنتج." },
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextPrimary,
                )
                HorizontalDivider(color = BorderNeutral)
                ProductSpecRow(label = "رمز الصنف (SKU)", value = product.sku)
                if (!product.barcode.isNullOrBlank()) {
                  ProductSpecRow(
                    label = "الباركود",
                    value = product.barcode,
                    icon = Icons.Filled.QrCodeScanner,
                  )
                }
                if (!product.qrCode.isNullOrBlank()) {
                  ProductSpecRow(
                    label = "رمز الاستجابة السريعة (QR)",
                    value = product.qrCode,
                    icon = Icons.Filled.QrCode2,
                  )
                }
                ProductSpecRow(label = "الكمية المتاحة", value = "${product.stockQuantity} ${product.unit}")
                if (product.wholesalePrice > 0.0) {
                  ProductSpecRow(
                    label = "الحد الأدنى للجملة",
                    value = "${product.wholesaleMinQty} ${product.unit}",
                  )
                }
              }
            }
          }

          // 5. المنتجات المشابهة
          if (similarProducts.isNotEmpty()) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderRow(
                  title = "منتجات مشابهة",
                  modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
                )
                LazyRow(
                  contentPadding = PaddingValues(horizontal = AlEmanDimens.SpaceLg),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  items(similarProducts, key = { it.id }) { itemProduct ->
                    val colorsCnt = if (storeSettings.showProductColors) itemProduct.availableColors.size else 0
                    val sizesCnt = if (storeSettings.showProductSizes) itemProduct.availableSizes.size else 0
                    val effectiveRetailSim = itemProduct.effectiveRetailPriceWithShowcases(catalogState.brandShowcases)
                    val effectiveDiscountSim = itemProduct.effectiveDiscountPercentWithShowcases(catalogState.brandShowcases)
                    ProductStoreCard(
                      product = itemProduct,
                      isWholesaleApproved = isWholesaleApproved,
                      onClick = { onOpenProduct(itemProduct.id) },
                      isWishlisted = catalogState.isWishlisted(itemProduct.id),
                      onToggleWishlist = { onToggleWishlist(itemProduct.id) },
                      visibleColorsCount = colorsCnt,
                      visibleSizesCount = sizesCnt,
                      overrideRetailPrice = effectiveRetailSim,
                      overrideDiscountPercent = effectiveDiscountSim,
                      onAddToCart = {
                        if (colorsCnt > 0 || sizesCnt > 0) {
                          onOpenProduct(itemProduct.id)
                        } else {
                          onAddToCart(itemProduct, 1, null, null)
                        }
                      },
                      modifier = Modifier.width(172.dp),
                    )
                  }
                }
              }
            }
          }

          // 6. تقييمات ومراجعات العملاء
          item {
            val ownReview = remember(reviewsState.reviews, currentUserId) {
              currentUserId?.let { uid -> reviewsState.reviews.firstOrNull { it.userId == uid } }
            }
            ProductReviewsSectionCard(
              reviewsState = reviewsState,
              currentUserId = currentUserId,
              hasOwnReview = ownReview != null,
              onWriteOrEditClick = {
                if (currentUserId == null) {
                  onRequireSignIn()
                } else {
                  showReviewDialog = true
                }
              },
              onDeleteOwnReview = onDeleteReview,
              modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
            )
          }
        }
      }
    }
  }

  if (showReviewDialog && product != null) {
    val existingOwn = currentUserId?.let { uid -> reviewsState.reviews.firstOrNull { it.userId == uid } }
    ReviewFormDialog(
      productName = product.name,
      existingReview = existingOwn,
      isSubmitting = reviewsState.isSubmitting,
      onDismiss = { showReviewDialog = false },
      onConfirm = { rating, comment ->
        onSubmitReview(product.name, rating, comment)
        showReviewDialog = false
      },
    )
  }

  val activeZoomIndex = zoomImageIndex
  if (product != null && activeZoomIndex != null && product.imageUrls.isNotEmpty()) {
    FullScreenImageDialog(
      imageUrls = product.imageUrls,
      initialPage = activeZoomIndex.coerceIn(0, product.imageUrls.lastIndex),
      productName = product.name,
      onDismiss = { zoomImageIndex = null },
    )
  }
}

@Composable
private fun ProductImageGalleryCard(
  product: Product,
  displayDiscountPercent: Int,
  onImageClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val images = product.imageUrls
  AlEmanCard(modifier = modifier.fillMaxWidth()) {
    if (images.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(4f / 3f),
      ) {
        ProductThumbnail(
          imageUrl = null,
          contentDescription = product.name,
          modifier = Modifier.fillMaxSize(),
        )
      }
    } else {
      val pagerState = rememberPagerState(pageCount = { images.size })
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(4f / 3f)
          .background(GeneralBackground),
      ) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize(),
        ) { page ->
          ProductThumbnail(
            imageUrl = images[page],
            contentDescription = "${product.name} - صورة ${page + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .clickable { onImageClick(page) },
          )
        }

        if (displayDiscountPercent > 0) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DiscountRed,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(12.dp),
          ) {
            Text(
              text = "خصم $displayDiscountPercent%",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = PureWhite,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        if (images.size > 1) {
          Surface(
            shape = CircleShape,
            color = CharcoalBlack.copy(alpha = 0.85f),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(12.dp),
          ) {
            Text(
              text = "${pagerState.currentPage + 1} / ${images.size}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = PrimaryYellow,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }

          Surface(
            shape = CircleShape,
            color = PureWhite.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, BorderNeutral),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(10.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              repeat(images.size) { index ->
                Box(
                  modifier = Modifier
                    .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                      if (pagerState.currentPage == index) CharcoalBlack else TextSecondary.copy(alpha = 0.35f),
                    ),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProductPricingCard(
  product: Product,
  effectiveRetailPrice: Double,
  effectiveDiscountPercent: Int,
  isWholesaleApproved: Boolean,
  modifier: Modifier = Modifier,
) {
  AlEmanCard(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (isWholesaleApproved && product.wholesalePrice > 0.0) {
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          color = GoldenYellowSoft,
          border = BorderStroke(1.dp, GoldenYellow),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "سعر الجملة المعتمد لحسابك",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
              )
            }
            Text(
              text = "${ArabicFormatter.formatCurrency(product.wholesalePrice)} / ${product.unit}",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = RoyalBlue,
            )
            Text(
              text = "يُطبَّق هذا السعر عند طلب ${product.wholesaleMinQty} ${product.unit} فأكثر",
              style = MaterialTheme.typography.bodySmall,
              color = TextPrimary,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = if (isWholesaleApproved) "سعر القطاعي" else "السعر",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
          )
          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = ArabicFormatter.formatCurrency(effectiveRetailPrice),
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = RoyalBlue,
            )
            if (effectiveDiscountPercent > 0 && effectiveRetailPrice < product.retailPrice) {
              Text(
                text = ArabicFormatter.formatCurrency(product.retailPrice),
                style = MaterialTheme.typography.bodyMedium.copy(
                  textDecoration = TextDecoration.LineThrough,
                ),
                color = TextSecondary,
              )
            }
          }
        }
        Text(
          text = "لكل ${product.unit}",
          style = MaterialTheme.typography.labelLarge,
          color = TextSecondary,
        )
      }

      if (!isWholesaleApproved && product.wholesalePrice > 0.0) {
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = RoyalBlueSoft,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "متوفر بيع بالجملة للتجار المعتمدين بدءًا من ${product.wholesaleMinQty} ${product.unit}. اطلب اعتماد الجملة من صفحة حسابك.",
            style = MaterialTheme.typography.bodySmall,
            color = RoyalBlue,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ProductSpecRow(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = RoyalBlue,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
      )
    }
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = TextPrimary,
    )
  }
}

@Composable
private fun FullScreenImageDialog(
  imageUrls: List<String>,
  initialPage: Int,
  productName: String,
  onDismiss: () -> Unit,
) {
  val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { imageUrls.size })

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black),
    ) {
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
      ) { page ->
        ProductThumbnail(
          imageUrl = imageUrls[page],
          contentDescription = productName,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize(),
        )
      }

      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(16.dp)
          .background(PureWhite.copy(alpha = 0.2f), CircleShape)
          .testTag("product_gallery_close_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Close,
          contentDescription = "إغلاق المعرض",
          tint = PureWhite,
        )
      }
    }
  }
}

@Composable
private fun ProductReviewsSectionCard(
  reviewsState: ProductReviewsUiState,
  currentUserId: String?,
  hasOwnReview: Boolean,
  onWriteOrEditClick: () -> Unit,
  onDeleteOwnReview: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  AlEmanCard(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.RateReview, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(20.dp))
          Text(
            text = "تقييمات العملاء (${reviewsState.reviewCount})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
        }
        OutlinedButton(
          onClick = onWriteOrEditClick,
          border = BorderStroke(1.dp, CharcoalBlack),
          modifier = Modifier.testTag("product_write_review_button"),
        ) {
          Icon(
            imageVector = if (hasOwnReview) Icons.Filled.Edit else Icons.Filled.Star,
            contentDescription = null,
            tint = CharcoalBlack,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = when {
              currentUserId == null -> "سجّل لتقييم المنتج"
              hasOwnReview -> "تعديل تقييمك"
              else -> "أضف تقييمك"
            },
            color = CharcoalBlack,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (reviewsState.reviews.isEmpty()) {
        Text(
          text = "لا توجد تقييمات ظاهرة لهذا المنتج حتى الآن. كن أول من يشارك تجربته!",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      } else {
        reviewsState.reviews.forEachIndexed { idx, review ->
          if (idx > 0) HorizontalDivider(color = BorderNeutral)
          ProductReviewItemRow(
            review = review,
            isOwnReview = currentUserId != null && review.userId == currentUserId,
            onDelete = { onDeleteOwnReview(review.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun ProductReviewItemRow(
  review: ProductReview,
  isOwnReview: Boolean,
  onDelete: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = review.userName,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        if (isOwnReview) {
          BrandPillBadge(
            text = "تقييمك",
            containerColor = GoldenYellowSoft,
            contentColor = CharcoalBlack,
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { idx ->
          Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = if (idx < review.rating) PrimaryYellow else BorderNeutral,
            modifier = Modifier.size(15.dp),
          )
        }
        if (isOwnReview) {
          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = "حذف تقييمك",
              tint = DiscountRed,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
    Text(
      text = review.comment,
      style = MaterialTheme.typography.bodySmall,
      color = TextPrimary,
    )
  }
}

@Composable
private fun ReviewFormDialog(
  productName: String,
  existingReview: ProductReview?,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (rating: Int, comment: String) -> Unit,
) {
  var selectedRating by remember { mutableIntStateOf(existingReview?.rating ?: 5) }
  var comment by remember { mutableStateOf(existingReview?.comment.orEmpty()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existingReview == null) "تقييم المنتج" else "تعديل تقييمك",
        fontWeight = FontWeight.Bold,
        color = CharcoalBlack,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = productName,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = CharcoalBlack,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          for (star in 1..5) {
            IconButton(
              onClick = { selectedRating = star },
              modifier = Modifier.testTag("review_star_$star"),
            ) {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "$star نجوم",
                tint = if (star <= selectedRating) PrimaryYellow else BorderNeutral,
                modifier = Modifier.size(32.dp),
              )
            }
          }
        }
        OutlinedTextField(
          value = comment,
          onValueChange = { comment = it },
          label = { Text("اكتب رأيك في المنتج (جودة، تغليف، سعر)") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("review_comment_input"),
          minLines = 3,
          maxLines = 5,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedRating, comment) },
        enabled = !isSubmitting && comment.trim().length >= 2,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
        modifier = Modifier.testTag("submit_review_confirm_button"),
      ) {
        Text("إرسال التقييم", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}

