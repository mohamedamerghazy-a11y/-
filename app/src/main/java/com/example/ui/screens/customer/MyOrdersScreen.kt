package com.example.ui.screens.customer

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.OrderStatus
import com.example.core.util.ArabicFormatter
import com.example.data.model.Order
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CartAndOrdersUiState
import java.text.SimpleDateFormat
import java.util.Locale

private enum class OrdersFilterTab(val label: String) {
  ALL("الكل"),
  ACTIVE("الحالية"),
  DELIVERED("المكتملة"),
  CANCELLED_RETURNED("الملغاة والمرتجعة"),
}

@Composable
fun MyOrdersScreen(
  uiState: CartAndOrdersUiState,
  onOpenOrderDetails: (String) -> Unit,
  onReorder: (Order) -> Unit,
  onShopNow: () -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var selectedFilter by rememberSaveable { mutableStateOf(OrdersFilterTab.ALL) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
    val msg = uiState.feedbackMessage ?: uiState.errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
    }
  }

  val filteredOrders = remember(uiState.orders, selectedFilter) {
    when (selectedFilter) {
      OrdersFilterTab.ALL -> uiState.orders
      OrdersFilterTab.ACTIVE -> uiState.orders.filter { it.statusEnum.isActiveProgression }
      OrdersFilterTab.DELIVERED -> uiState.orders.filter { it.statusEnum == OrderStatus.DELIVERED }
      OrdersFilterTab.CANCELLED_RETURNED -> uiState.orders.filter { it.statusEnum.isTerminalException }
    }
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = "طلباتي",
        subtitle = "متابعة الطلبات الحالية والسابقة",
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
      Column(
        modifier = Modifier
          .widthIn(max = 840.dp)
          .fillMaxSize(),
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = AlEmanDimens.SpaceLg, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(OrdersFilterTab.entries) { filter ->
            val selected = filter == selectedFilter
            FilterChip(
              selected = selected,
              onClick = { selectedFilter = filter },
              label = { Text(filter.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RoyalBlue,
                selectedLabelColor = PureWhite,
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

        when {
          uiState.isOrdersLoading && uiState.orders.isEmpty() -> {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = RoyalBlue)
            }
          }

          filteredOrders.isEmpty() -> {
            Box(modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg)) {
              StoreEmptyStateCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "لا توجد طلبات في هذا القسم",
                description = "عند إتمام أي عملية شراء ستظهر تفاصيل طلبك وتتبع حالته هنا.",
                actionLabel = "تصفح المنتجات",
                onActionClick = onShopNow,
              )
            }
          }

          else -> {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(
                start = AlEmanDimens.SpaceLg,
                end = AlEmanDimens.SpaceLg,
                bottom = AlEmanDimens.SpaceLg,
              ),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(filteredOrders, key = { it.id }) { order ->
                OrderSummaryCard(
                  order = order,
                  onOpenDetails = { onOpenOrderDetails(order.id) },
                  onReorder = { onReorder(order) },
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
private fun OrderSummaryCard(
  order: Order,
  onOpenDetails: () -> Unit,
  onReorder: () -> Unit,
) {
  val dateText = remember(order.createdAt) {
    order.createdAt?.toDate()?.let { date ->
      SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.forLanguageTag("ar-EG")).format(date)
    } ?: "الآن"
  }

  AlEmanCard(
    onClick = onOpenDetails,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("order_card_${order.id}"),
  ) {
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "طلب #${order.orderNumber}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = RoyalBlue,
            )
            if (order.isStorePickup) {
              BrandPillBadge(
                text = "استلام من المتجر",
                containerColor = PrimaryYellowSoft,
                contentColor = CharcoalBlack,
              )
            }
          }
          Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
        OrderStatusBadge(status = order.statusEnum, isStorePickup = order.isStorePickup)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${order.items.size} صنف (${order.totalUnitsCount} وحدة) • ${order.paymentMethodEnum.arabicLabel}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          modifier = Modifier.weight(1f),
        )
        PaymentStatusBadge(status = order.paymentStatusEnum)
      }

      if (order.items.isNotEmpty()) {
        val previewNames = order.items.take(2).joinToString("، ") { it.productName }
        val suffix = if (order.items.size > 2) " +${order.items.size - 2} أخرى" else ""
        Text(
          text = "$previewNames$suffix",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = TextPrimary,
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
            text = "الإجمالي",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
          Text(
            text = ArabicFormatter.formatCurrency(order.totalAmount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = RoyalBlue,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = onReorder,
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.45f)),
            modifier = Modifier
              .height(42.dp)
              .testTag("order_reorder_${order.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.Replay,
              contentDescription = null,
              tint = RoyalBlue,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("إعادة الطلب", color = RoyalBlue)
          }

          Button(
            onClick = onOpenDetails,
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            colors = ButtonDefaults.buttonColors(
              containerColor = RoyalBlue,
              contentColor = PureWhite,
            ),
            modifier = Modifier
              .height(42.dp)
              .testTag("order_details_btn_${order.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.Visibility,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("التفاصيل")
          }
        }
      }
    }
  }
}
