package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.util.ArabicFormatter
import com.example.data.model.Product
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminOffersUiState
import com.example.ui.viewmodel.OfferFilterMode
import kotlin.math.roundToInt

@Composable
fun AdminOffersScreen(
  uiState: AdminOffersUiState,
  onSelectFilter: (OfferFilterMode) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onUpdateOfferSettings: (Product, Double?, Boolean, Boolean) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingProduct by remember { mutableStateOf<Product?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
    val msg = uiState.feedbackMessage ?: uiState.errorMessage
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
        title = "إدارة العروض والمنتجات المميزة",
        subtitle = "تفعيل الخصومات وتحديد أسعار العروض اليومية",
        showBack = true,
        onBackClick = onBackClick,
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
        uiState.isLoading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CharcoalBlack)
          }
        }

        else -> {
          LazyColumn(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
            contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                OfferMetricBadge("العروض النشطة: ${uiState.activeOffersCount}", PrimaryYellowSoft, Modifier.weight(1f))
                OfferMetricBadge("المنتجات المميزة: ${uiState.featuredCount}", PureWhite, Modifier.weight(1f))
              }
            }

            item {
              OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("ابحث باسم المنتج أو SKU...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_offers_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              )
            }

            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(OfferFilterMode.entries) { mode ->
                  val selected = uiState.selectedFilter == mode
                  FilterChip(
                    selected = selected,
                    onClick = { onSelectFilter(mode) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = PrimaryYellow,
                      selectedLabelColor = CharcoalBlack,
                    ),
                    label = {
                      Text(mode.arabicLabel, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    },
                  )
                }
              }
            }

            if (uiState.filteredProducts.isEmpty()) {
              item {
                StoreEmptyStateCard(
                  icon = Icons.Filled.LocalOffer,
                  title = "لا توجد منتجات مطابقة",
                  description = "أضف منتجات أو غيّر معايير التصفية لتعديل أسعار العروض.",
                )
              }
            } else {
              items(uiState.filteredProducts, key = { it.id }) { product ->
                ProductOfferAdminCard(
                  product = product,
                  isSubmitting = uiState.isSubmitting,
                  onToggleOffer = {
                    onUpdateOfferSettings(
                      product,
                      product.retailDiscountPrice,
                      !product.isOffer,
                      product.isFeatured,
                    )
                  },
                  onToggleFeatured = {
                    onUpdateOfferSettings(
                      product,
                      product.retailDiscountPrice,
                      product.isOffer,
                      !product.isFeatured,
                    )
                  },
                  onEditDiscount = { editingProduct = product },
                )
              }
            }
          }
        }
      }
    }
  }

  val targetProduct = editingProduct
  if (targetProduct != null) {
    OfferDiscountDialog(
      product = targetProduct,
      onDismiss = { editingProduct = null },
      onConfirm = { discountPrice, isOffer, isFeatured ->
        onUpdateOfferSettings(targetProduct, discountPrice, isOffer, isFeatured)
        editingProduct = null
      },
    )
  }
}

@Composable
private fun OfferMetricBadge(
  text: String,
  containerColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
    border = BorderStroke(1.dp, BorderNeutral),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = CharcoalBlack,
      modifier = Modifier.padding(14.dp),
    )
  }
}

@Composable
private fun ProductOfferAdminCard(
  product: Product,
  isSubmitting: Boolean,
  onToggleOffer: () -> Unit,
  onToggleFeatured: () -> Unit,
  onEditDiscount: () -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ProductThumbnail(
          imageUrl = product.imageUrls.firstOrNull(),
          contentDescription = product.name,
          modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = product.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = ArabicFormatter.formatCurrency(product.effectiveRetailPrice),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = SuccessGreen,
            )
            if (product.hasDiscount) {
              Text(
                text = ArabicFormatter.formatCurrency(product.retailPrice),
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                color = TextSecondary,
              )
              DiscountBadge("خصم ${product.discountPercent}%")
            }
          }
        }
      }

      HorizontalDivider(color = BorderNeutral)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilterChip(
          selected = product.isOffer,
          onClick = onToggleOffer,
          enabled = !isSubmitting,
          leadingIcon = { Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp)) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryYellow,
            selectedLabelColor = CharcoalBlack,
            selectedLeadingIconColor = CharcoalBlack,
          ),
          label = { Text("عرض") },
        )

        FilterChip(
          selected = product.isFeatured,
          onClick = onToggleFeatured,
          enabled = !isSubmitting,
          leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CharcoalBlack,
            selectedLabelColor = PrimaryYellow,
            selectedLeadingIconColor = PrimaryYellow,
          ),
          label = { Text("مميز") },
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
          onClick = onEditDiscount,
          enabled = !isSubmitting,
          border = BorderStroke(1.dp, CharcoalBlack),
          modifier = Modifier.testTag("edit_offer_price_${product.id}"),
        ) {
          Text("سعر الخصم", color = CharcoalBlack, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun OfferDiscountDialog(
  product: Product,
  onDismiss: () -> Unit,
  onConfirm: (Double?, Boolean, Boolean) -> Unit,
) {
  var discountInput by remember { mutableStateOf(product.retailDiscountPrice?.toString().orEmpty()) }
  var isOffer by remember { mutableStateOf(product.isOffer) }
  var isFeatured by remember { mutableStateOf(product.isFeatured) }

  val parsedDiscount = discountInput.toDoubleOrNull()
  val validDiscount = parsedDiscount != null && parsedDiscount > 0.0 && parsedDiscount < product.retailPrice
  val previewPercent = if (validDiscount && product.retailPrice > 0.0) {
    (((product.retailPrice - parsedDiscount!!) / product.retailPrice) * 100.0).roundToInt().coerceIn(1, 99)
  } else null

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text("ضبط عرض: ${product.name}", fontWeight = FontWeight.Bold, color = CharcoalBlack, maxLines = 2)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "السعر الأصلي للقطاعي: ${ArabicFormatter.formatCurrency(product.retailPrice)}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = CharcoalBlack,
        )

        OutlinedTextField(
          value = discountInput,
          onValueChange = { discountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("سعر العرض بعد الخصم (اتركه فارغًا لإلغاء الخصم)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        if (previewPercent != null) {
          BrandPillBadge(
            text = "نسبة الخصم المحسوبة: $previewPercent%",
            containerColor = PrimaryYellowSoft,
            contentColor = CharcoalBlack,
          )
        } else if (discountInput.isNotBlank()) {
          Text(
            text = "يجب أن يكون السعر أقل من ${ArabicFormatter.formatCurrency(product.retailPrice)}",
            color = DiscountRed,
            style = MaterialTheme.typography.bodySmall,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("إظهار في قسم العروض")
          Switch(
            checked = isOffer,
            onCheckedChange = { isOffer = it },
            colors = SwitchDefaults.colors(checkedThumbColor = CharcoalBlack, checkedTrackColor = PrimaryYellow),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("تمييز المنتج كمنتج مميز")
          Switch(
            checked = isFeatured,
            onCheckedChange = { isFeatured = it },
            colors = SwitchDefaults.colors(checkedThumbColor = CharcoalBlack, checkedTrackColor = PrimaryYellow),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanVal = if (discountInput.isBlank()) null else parsedDiscount
          onConfirm(cleanVal, isOffer, isFeatured)
        },
        enabled = discountInput.isBlank() || validDiscount,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
      ) {
        Text("حفظ العرض", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      Row {
        if (product.hasDiscount) {
          TextButton(onClick = { onConfirm(null, false, isFeatured) }) {
            Text("إزالة الخصم", color = DiscountRed)
          }
        }
        TextButton(onClick = onDismiss) {
          Text("إلغاء", color = TextSecondary)
        }
      }
    },
  )
}
