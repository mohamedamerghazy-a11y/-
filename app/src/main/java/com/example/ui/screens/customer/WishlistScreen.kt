package com.example.ui.screens.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.effectiveDiscountPercentWithShowcases
import com.example.data.model.effectiveRetailPriceWithShowcases
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.ProductStoreCard
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.viewmodel.CatalogUiState

@Composable
fun WishlistScreen(
  catalogState: CatalogUiState,
  isSignedIn: Boolean,
  isWholesaleApproved: Boolean,
  feedbackMessage: String?,
  errorMessage: String?,
  onClearMessages: () -> Unit,
  onOpenProduct: (String) -> Unit,
  onToggleWishlist: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
  onRequireSignIn: () -> Unit,
  onBackClick: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val combinedFeedback = catalogState.wishlistMessage ?: feedbackMessage
  val products = catalogState.wishlistProducts
  val settings = catalogState.storeSettings

  LaunchedEffect(combinedFeedback, errorMessage) {
    val msg = combinedFeedback ?: errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
    }
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = "المفضلة",
        subtitle = if (isSignedIn) "المنتجات المحفوظة للشراء لاحقًا" else "سجّل الدخول لحفظ منتجاتك المفضلة",
        showBack = true,
        onBackClick = onBackClick,
        trailingContent = {
          if (isSignedIn && products.isNotEmpty()) {
            BrandPillBadge(
              text = "${products.size} منتج",
              containerColor = PrimaryYellow,
              contentColor = CharcoalBlack,
              modifier = Modifier.padding(end = 10.dp),
            )
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when {
        !isSignedIn -> {
          Column(
            modifier = Modifier
              .widthIn(max = 600.dp)
              .padding(AlEmanDimens.SpaceLg),
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.FavoriteBorder,
              title = "سجّل الدخول لعرض المفضلة",
              description = "احفظ المنتجات التي تهمك في قائمة المفضلة لتصل إليها من الهاتف والويب في أي وقت.",
              actionLabel = "تسجيل الدخول",
              onActionClick = onRequireSignIn,
            )
          }
        }

        products.isEmpty() -> {
          Column(
            modifier = Modifier
              .widthIn(max = 600.dp)
              .padding(AlEmanDimens.SpaceLg),
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.FavoriteBorder,
              title = "قائمة المفضلة فارغة حاليًا",
              description = "اضغط على علامة القلب فوق أي منتج لإضافته هنا ومراجعته بسرعة لاحقًا.",
              actionLabel = "تصفح المنتجات",
              onActionClick = onBackClick,
            )
          }
        }

        else -> {
          val rows = remember(products) { products.chunked(2) }
          LazyColumn(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize()
              .testTag("wishlist_lazy_column"),
            contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(rows) { pair ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                pair.forEach { product ->
                  val colorsCount = if (settings.showProductColors) product.availableColors.size else 0
                  val sizesCount = if (settings.showProductSizes) product.availableSizes.size else 0
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
  }
}
