package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Product
import com.example.data.model.effectiveDiscountPercentWithShowcases
import com.example.data.model.effectiveRetailPriceWithShowcases
import com.example.ui.components.ProductStoreCard
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CatalogSortOption
import com.example.ui.viewmodel.CatalogUiState

@Composable
fun SearchTabContent(
  catalogState: CatalogUiState,
  isWholesaleApproved: Boolean,
  onQueryChange: (String) -> Unit,
  onOpenQrScanner: () -> Unit,
  onSelectCategory: (String?) -> Unit,
  onToggleOffers: () -> Unit,
  onToggleInStock: () -> Unit,
  onToggleWholesale: () -> Unit,
  onSetPriceRange: (Double?, Double?) -> Unit,
  onSelectSort: (CatalogSortOption) -> Unit,
  onResetFilters: () -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
  onToggleWishlist: (String) -> Unit = {},
  onLogZeroResultSearch: (String) -> Unit = {},
) {
  val filter = catalogState.filter
  val results = remember(catalogState, isWholesaleApproved) {
    catalogState.filteredProducts(isWholesaleApproved)
  }

  LaunchedEffect(
    filter.query,
    filter.activeFilterCount,
    results.size,
    catalogState.isLoading,
    catalogState.products.isEmpty(),
  ) {
    if (
      !catalogState.isLoading &&
      catalogState.products.isNotEmpty() &&
      results.isEmpty() &&
      filter.activeFilterCount == 0 &&
      filter.query.trim().length >= 2
    ) {
      delay(1200)
      onLogZeroResultSearch(filter.query)
    }
  }

  var showSortMenu by remember { mutableStateOf(false) }
  var showPriceDialog by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.padding(vertical = AlEmanDimens.SpaceLg),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // حقل البحث المتقدم + زر مسح QR والباركود بالكاميرا
    OutlinedTextField(
      value = filter.query,
      onValueChange = onQueryChange,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AlEmanDimens.SpaceLg)
        .testTag("customer_search_input"),
      placeholder = {
        Text(
          text = "ابحث بالاسم أو القسم أو SKU أو الباركود أو QR",
          style = MaterialTheme.typography.bodySmall,
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          contentDescription = stringResource(id = R.string.nav_search),
          tint = RoyalBlue,
        )
      },
      trailingIcon = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (filter.query.isNotEmpty()) {
            IconButton(
              onClick = { onQueryChange("") },
              modifier = Modifier.testTag("search_clear_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "مسح البحث",
                tint = TextSecondary,
              )
            }
          }
          IconButton(
            onClick = onOpenQrScanner,
            modifier = Modifier.testTag("search_scan_qr_barcode_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.QrCodeScanner,
              contentDescription = "البحث بمسح QR أو الباركود",
              tint = RoyalBlue,
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = PureWhite,
        unfocusedContainerColor = PureWhite,
        focusedBorderColor = RoyalBlue,
        unfocusedBorderColor = BorderNeutral,
      ),
    )

    // شريط الفلاتر المتعددة (العروض، المتوفر، الجملة، نطاق السعر، الأقسام)
    LazyRow(
      contentPadding = PaddingValues(horizontal = AlEmanDimens.SpaceLg),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        FilterChip(
          selected = filter.activeFilterCount == 0,
          onClick = onResetFilters,
          label = { Text("الكل") },
          colors = searchChipColors(),
          border = searchChipBorder(filter.activeFilterCount == 0),
        )
      }
      item {
        FilterChip(
          selected = filter.onlyOffers,
          onClick = onToggleOffers,
          label = { Text("العروض") },
          colors = searchChipColors(),
          border = searchChipBorder(filter.onlyOffers),
        )
      }
      item {
        FilterChip(
          selected = filter.onlyInStock,
          onClick = onToggleInStock,
          label = { Text("المتوفر فقط") },
          colors = searchChipColors(),
          border = searchChipBorder(filter.onlyInStock),
        )
      }
      item {
        FilterChip(
          selected = filter.onlyWholesaleEligible,
          onClick = onToggleWholesale,
          label = { Text("أسعار الجملة") },
          colors = searchChipColors(),
          border = searchChipBorder(filter.onlyWholesaleEligible),
        )
      }
      item {
        val hasPriceFilter = filter.minPrice != null || filter.maxPrice != null
        FilterChip(
          selected = hasPriceFilter,
          onClick = { showPriceDialog = true },
          leadingIcon = {
            Icon(
              imageVector = Icons.Filled.Tune,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          label = {
            Text(
              if (hasPriceFilter) "السعر محدد" else "نطاق السعر",
            )
          },
          colors = searchChipColors(),
          border = searchChipBorder(hasPriceFilter),
        )
      }
      items(catalogState.mainCategories, key = { it.id }) { category ->
        val selected = filter.categoryId == category.id
        FilterChip(
          selected = selected,
          onClick = {
            onSelectCategory(if (selected) null else category.id)
          },
          label = { Text(category.name) },
          colors = searchChipColors(),
          border = searchChipBorder(selected),
        )
      }
    }

    // شريط النتائج + الترتيب
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AlEmanDimens.SpaceLg),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "النتائج: ${results.size} منتج",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (filter.activeFilterCount > 0 || filter.query.isNotBlank()) {
          Surface(
            onClick = onResetFilters,
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            color = GoldenYellowSoft,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.FilterAltOff,
                contentDescription = "إعادة تعيين الفلاتر",
                tint = TextPrimary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "مسح الكل",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
              )
            }
          }
        }

        androidx.compose.foundation.layout.Box {
          Surface(
            onClick = { showSortMenu = true },
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderNeutral),
            modifier = Modifier.testTag("search_sort_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "ترتيب النتائج",
                tint = RoyalBlue,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = filter.sortOption.arabicLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RoyalBlue,
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
                    fontWeight = if (option == filter.sortOption) FontWeight.Bold else FontWeight.Normal,
                    color = if (option == filter.sortOption) RoyalBlue else TextPrimary,
                  )
                },
                onClick = {
                  showSortMenu = false
                  onSelectSort(option)
                },
              )
            }
          }
        }
      }
    }

    // شبكة عرض النتائج
    if (results.isEmpty()) {
      androidx.compose.foundation.layout.Box(
        modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
      ) {
        StoreEmptyStateCard(
          icon = Icons.Filled.Search,
          title = if (catalogState.products.isEmpty()) {
            stringResource(id = R.string.catalog_empty_title)
          } else {
            "لا توجد نتائج مطابقة لبحثك"
          },
          description = stringResource(id = R.string.search_empty_desc),
          actionLabel = if (filter.query.isNotBlank() || filter.activeFilterCount > 0) "إعادة ضبط البحث" else null,
          onActionClick = if (filter.query.isNotBlank() || filter.activeFilterCount > 0) onResetFilters else null,
        )
      }
    } else {
      Column(
        modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        results.chunked(2).forEach { pair ->
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

  if (showPriceDialog) {
    PriceRangeFilterDialog(
      currentMin = filter.minPrice,
      currentMax = filter.maxPrice,
      onDismiss = { showPriceDialog = false },
      onApply = { minP, maxP ->
        showPriceDialog = false
        onSetPriceRange(minP, maxP)
      },
    )
  }
}

@Composable
private fun PriceRangeFilterDialog(
  currentMin: Double?,
  currentMax: Double?,
  onDismiss: () -> Unit,
  onApply: (Double?, Double?) -> Unit,
) {
  var minText by remember { mutableStateOf(currentMin?.toInt()?.toString().orEmpty()) }
  var maxText by remember { mutableStateOf(currentMax?.toInt()?.toString().orEmpty()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "تحديد نطاق السعر (ج.م)",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = RoyalBlue,
      )
    },
    text = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = minText,
          onValueChange = { minText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("من") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = maxText,
          onValueChange = { maxText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("إلى") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onApply(minText.toDoubleOrNull(), maxText.toDoubleOrNull())
        },
        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = PureWhite),
      ) {
        Text("تطبيق")
      }
    },
    dismissButton = {
      Row {
        TextButton(onClick = { onApply(null, null) }) {
          Text("إلغاء النطاق", color = TextSecondary)
        }
        TextButton(onClick = onDismiss) {
          Text("إغلاق", color = RoyalBlue)
        }
      }
    },
  )
}

@Composable
private fun searchChipColors() = FilterChipDefaults.filterChipColors(
  selectedContainerColor = RoyalBlue,
  selectedLabelColor = PureWhite,
  selectedLeadingIconColor = PureWhite,
  containerColor = PureWhite,
  labelColor = TextPrimary,
  iconColor = RoyalBlue,
)

@Composable
private fun searchChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
  enabled = true,
  selected = selected,
  borderColor = BorderNeutral,
  selectedBorderColor = RoyalBlue,
)
