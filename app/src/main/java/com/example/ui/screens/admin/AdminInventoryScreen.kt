package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.util.ArabicFormatter
import com.example.data.model.Product
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.QrBarcodeScannerDialog
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminInventoryUiState

@Composable
fun AdminInventoryScreen(
  uiState: AdminInventoryUiState,
  onSearchQueryChange: (String) -> Unit,
  onToggleLowStockFilter: () -> Unit,
  onUpdateStock: (productId: String, newStock: Int, lowStockThreshold: Int, isActive: Boolean) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var showScanner by remember { mutableStateOf(false) }
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
        title = "إدارة المخزون",
        subtitle = "متابعة الكميات وحد التنبيه ونفاد المخزون",
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

        uiState.products.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.Inventory,
              title = "لا توجد منتجات في المخزون",
              description = "أضف منتجات جديدة من قسم إدارة المنتجات لتظهر هنا.",
              actionLabel = "العودة",
              onActionClick = onBackClick,
            )
          }
        }

        else -> {
          Column(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                InventorySummaryChip(
                  label = "إجمالي المنتجات",
                  value = uiState.products.size.toString(),
                  containerColor = PureWhite,
                  valueColor = CharcoalBlack,
                  modifier = Modifier.weight(1f),
                )
                InventorySummaryChip(
                  label = "مخزون منخفض",
                  value = uiState.lowStockCount.toString(),
                  containerColor = if (uiState.lowStockCount > 0) PrimaryYellowSoft else PureWhite,
                  valueColor = CharcoalBlack,
                  modifier = Modifier.weight(1f),
                )
                InventorySummaryChip(
                  label = "نفد المخزون",
                  value = uiState.outOfStockCount.toString(),
                  containerColor = if (uiState.outOfStockCount > 0) DiscountRedSoft else PureWhite,
                  valueColor = if (uiState.outOfStockCount > 0) DiscountRed else SuccessGreen,
                  modifier = Modifier.weight(1f),
                )
              }

              OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("بحث بالاسم، SKU، الباركود أو QR...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = CharcoalBlack) },
                trailingIcon = {
                  IconButton(
                    onClick = { showScanner = true },
                    modifier = Modifier.testTag("admin_inventory_scan_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.QrCodeScanner,
                      contentDescription = "مسح الباركود أو QR للبحث",
                      tint = CharcoalBlack,
                    )
                  }
                },
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_inventory_search"),
                singleLine = true,
              )

              val alertsCount = uiState.lowStockCount + uiState.outOfStockCount
              FilterChip(
                selected = uiState.onlyLowStock,
                onClick = onToggleLowStockFilter,
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryYellow,
                  selectedLabelColor = CharcoalBlack,
                ),
                label = {
                  Text(
                    text = "تنبيهات الانخفاض والنفاد ($alertsCount)",
                    fontWeight = if (uiState.onlyLowStock) FontWeight.Bold else FontWeight.Normal,
                  )
                },
                modifier = Modifier.testTag("admin_inventory_low_stock_chip"),
              )
            }

            if (uiState.filteredProducts.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(AlEmanDimens.SpaceLg),
                contentAlignment = Alignment.Center,
              ) {
                StoreEmptyStateCard(
                  icon = Icons.Filled.Inventory,
                  title = "لا توجد منتجات مطابقة",
                  description = "جرّب تغيير عبارة البحث أو إلغاء فلتر التنبيهات.",
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(uiState.filteredProducts, key = { it.id }) { product ->
                  InventoryProductCard(
                    product = product,
                    stockAlertCount = uiState.stockAlertCounts[product.id] ?: 0,
                    onEditStock = { editingProduct = product },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showScanner) {
    QrBarcodeScannerDialog(
      title = "مسح باركود أو QR المنتج",
      onCodeScanned = { code ->
        onSearchQueryChange(code)
        showScanner = false
      },
      onDismiss = { showScanner = false },
    )
  }

  val targetProduct = editingProduct
  if (targetProduct != null) {
    EditStockDialog(
      product = targetProduct,
      onDismiss = { editingProduct = null },
      onConfirm = { newStock, threshold, isActive ->
        onUpdateStock(targetProduct.id, newStock, threshold, isActive)
        editingProduct = null
      },
    )
  }
}

@Composable
private fun InventorySummaryChip(
  label: String,
  value: String,
  containerColor: Color,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
    border = BorderStroke(AlEmanDimens.BorderWidth, BorderNeutral),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = valueColor,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun InventoryProductCard(
  product: Product,
  stockAlertCount: Int = 0,
  onEditStock: () -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = product.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            if (!product.isActive) {
              DiscountBadge("مخفي")
            }
          }
          Text(
            text = "SKU: ${product.sku} • حد التنبيه: ${product.lowStockThreshold} ${product.unit}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }

        when {
          product.isOutOfStock -> DiscountBadge(text = "نفد المخزون (0)")
          product.isLowStock -> BrandPillBadge(
            text = "منخفض (${product.stockQuantity})",
            containerColor = PrimaryYellow,
            contentColor = CharcoalBlack,
          )
          else -> SuccessBadge(text = "متوفر (${product.stockQuantity})")
        }
      }

      if (stockAlertCount > 0) {
        BrandPillBadge(
          text = "في قائمة الانتظار للتوفر: $stockAlertCount عميل 🔔",
          containerColor = PrimaryYellowSoft,
          contentColor = CharcoalBlack,
        )
      }

      HorizontalDivider(color = BorderNeutral)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "قطاعي: ${ArabicFormatter.formatCurrency(product.effectiveRetailPrice)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
          )
          if (product.wholesalePrice > 0.0) {
            Text(
              text = "جملة: ${ArabicFormatter.formatCurrency(product.wholesalePrice)} (${product.wholesaleMinQty}+)",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }
        Button(
          onClick = onEditStock,
          modifier = Modifier.testTag("edit_stock_${product.id}"),
          colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryYellow,
            contentColor = CharcoalBlack,
          ),
        ) {
          Text("تعديل المخزون", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun EditStockDialog(
  product: Product,
  onDismiss: () -> Unit,
  onConfirm: (newStock: Int, lowStockThreshold: Int, isActive: Boolean) -> Unit,
) {
  var stockText by remember(product) { mutableStateOf(product.stockQuantity.toString()) }
  var thresholdText by remember(product) { mutableStateOf(product.lowStockThreshold.toString()) }
  var isActive by remember(product) { mutableStateOf(product.isActive) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "تعديل مخزون (${product.name})",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = stockText,
          onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
          label = { Text("الكمية المتاحة حاليًا (${product.unit})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_stock_qty_input"),
          singleLine = true,
        )
        OutlinedTextField(
          value = thresholdText,
          onValueChange = { thresholdText = it.filter { ch -> ch.isDigit() } },
          label = { Text("حد تنبيه انخفاض المخزون") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_threshold_input"),
          singleLine = true,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("إظهار المنتج للعملاء في المتجر", style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = isActive,
            onCheckedChange = { isActive = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CharcoalBlack,
              checkedTrackColor = PrimaryYellow,
            ),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val qty = (stockText.toIntOrNull() ?: product.stockQuantity).coerceAtLeast(0)
          val threshold = (thresholdText.toIntOrNull() ?: product.lowStockThreshold).coerceAtLeast(0)
          onConfirm(qty, threshold, isActive)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryYellow,
          contentColor = CharcoalBlack,
        ),
        modifier = Modifier.testTag("inventory_save_stock_button"),
      ) {
        Text("حفظ التحديث", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}
