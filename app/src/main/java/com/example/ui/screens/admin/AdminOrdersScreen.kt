package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.location.LocationHelper
import com.example.core.model.OrderStatus
import com.example.core.util.ArabicFormatter
import com.example.data.model.Order
import com.example.data.model.StaffMember
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.OrderReceiptPrintDialog
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminOrdersUiState

@Composable
fun AdminOrdersScreen(
  uiState: AdminOrdersUiState,
  onSelectStatusFilter: (OrderStatus?) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onUpdateOrderStatus: (String, OrderStatus, String?) -> Unit,
  onAssignDriver: (Order, StaffMember) -> Unit = { _, _ -> },
  canPrepareAndPrint: Boolean = true,
  storeName: String = "الإيمان للتجارة",
  storePhone: String = "",
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingOrder by remember { mutableStateOf<Order?>(null) }
  var viewingOrder by remember { mutableStateOf<Order?>(null) }
  var assigningOrder by remember { mutableStateOf<Order?>(null) }
  var printingOrder by remember { mutableStateOf<Order?>(null) }
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
        title = "إدارة طلبات المتجر",
        subtitle = "دورة الطلبات الـ 9 وتفاصيل الشحن والمنتجات",
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

        uiState.orders.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.AutoMirrored.Filled.ReceiptLong,
              title = "لا توجد طلبات واردة حتى الآن",
              description = "ستظهر طلبات العملاء هنا بمجرد إتمام عمليات الشراء.",
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
              OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("بحث برقم الطلب، اسم العميل، الهاتف أو المحافظة...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = CharcoalBlack) },
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_orders_search"),
                singleLine = true,
              )

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = uiState.selectedStatusFilter == null,
                  onClick = { onSelectStatusFilter(null) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryYellow,
                    selectedLabelColor = CharcoalBlack,
                  ),
                  label = {
                    Text(
                      text = "الكل (${uiState.orders.size})",
                      fontWeight = if (uiState.selectedStatusFilter == null) FontWeight.Bold else FontWeight.Normal,
                    )
                  },
                )
                OrderStatus.entries.forEach { status ->
                  val count = uiState.orders.count { it.statusEnum == status }
                  val selected = uiState.selectedStatusFilter == status
                  FilterChip(
                    selected = selected,
                    onClick = { onSelectStatusFilter(status) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = PrimaryYellow,
                      selectedLabelColor = CharcoalBlack,
                    ),
                    label = {
                      Text(
                        text = "${status.arabicLabel} ($count)",
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                      )
                    },
                  )
                }
              }
            }

            if (uiState.filteredOrders.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(AlEmanDimens.SpaceLg),
                contentAlignment = Alignment.Center,
              ) {
                StoreEmptyStateCard(
                  icon = Icons.AutoMirrored.Filled.ReceiptLong,
                  title = "لا توجد طلبات مطابقة",
                  description = "جرّب تحديد حالة أخرى أو تعديل عبارة البحث.",
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(uiState.filteredOrders, key = { it.id }) { order ->
                  AdminOrderCard(
                    order = order,
                    canPrepareAndPrint = canPrepareAndPrint,
                    onViewDetailsClick = { viewingOrder = order },
                    onPrintReceiptClick = { printingOrder = order },
                    onAssignDriverClick = { assigningOrder = order },
                    onQuickNextStatus = { nextStatus ->
                      onUpdateOrderStatus(order.id, nextStatus, null)
                    },
                    onUpdateStatusClick = { editingOrder = order },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  val detailOrder = viewingOrder
  if (detailOrder != null) {
    OrderDetailsAdminDialog(
      order = detailOrder,
      canPrepareAndPrint = canPrepareAndPrint,
      onDismiss = { viewingOrder = null },
      onPrintReceipt = {
        viewingOrder = null
        printingOrder = detailOrder
      },
      onAssignDriver = {
        viewingOrder = null
        assigningOrder = detailOrder
      },
      onUpdateStatus = {
        viewingOrder = null
        editingOrder = detailOrder
      },
    )
  }

  val printTargetOrder = printingOrder
  if (printTargetOrder != null) {
    OrderReceiptPrintDialog(
      order = printTargetOrder,
      storeName = storeName,
      storePhone = storePhone,
      onDismiss = { printingOrder = null },
    )
  }

  val driverTargetOrder = assigningOrder
  if (driverTargetOrder != null) {
    AssignDriverDialog(
      order = driverTargetOrder,
      drivers = uiState.deliveryDrivers,
      onDismiss = { assigningOrder = null },
      onSelectDriver = { driver ->
        assigningOrder = null
        onAssignDriver(driverTargetOrder, driver)
      },
    )
  }

  val targetOrder = editingOrder
  if (targetOrder != null) {
    UpdateOrderStatusDialog(
      order = targetOrder,
      onDismiss = { editingOrder = null },
      onConfirm = { newStatus, note ->
        onUpdateOrderStatus(targetOrder.id, newStatus, note)
        editingOrder = null
      },
    )
  }
}

@Composable
private fun AdminOrderCard(
  order: Order,
  canPrepareAndPrint: Boolean,
  onViewDetailsClick: () -> Unit,
  onPrintReceiptClick: () -> Unit,
  onAssignDriverClick: () -> Unit,
  onQuickNextStatus: (OrderStatus) -> Unit,
  onUpdateStatusClick: () -> Unit,
) {
  val context = LocalContext.current
  val timeline = OrderStatus.activeTimelineStepsFor(order.isStorePickup)
  val currentIdx = timeline.indexOf(order.statusEnum)
  val nextStep = if (currentIdx in 0 until timeline.lastIndex) timeline[currentIdx + 1] else null

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
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = order.orderNumber.ifBlank { order.id.take(8) },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
            BrandPillBadge(
              text = if (order.accountTypeAtOrder == "wholesale") "جملة" else "قطاعي",
              containerColor = if (order.accountTypeAtOrder == "wholesale") PrimaryYellow else PrimaryYellowSoft,
              contentColor = CharcoalBlack,
            )
            if (order.isStorePickup) {
              BrandPillBadge(
                text = "استلام من المتجر",
                containerColor = SuccessGreenSoft,
                contentColor = SuccessGreen,
              )
            }
          }
          Text(
            text = "${order.customerName} • ${order.phone}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
          )
          val locationSummary = if (order.isStorePickup) {
            "الاستلام المباشر من مقر المتجر"
          } else {
            listOfNotNull(
              order.governorate.takeIf { it.isNotBlank() },
              order.city.takeIf { it.isNotBlank() },
              order.deliveryZoneName?.takeIf { it.isNotBlank() },
            ).joinToString(" - ")
          }
          if (locationSummary.isNotBlank()) {
            Text(
              text = locationSummary,
              style = MaterialTheme.typography.bodySmall,
              color = if (order.isStorePickup) SuccessGreen else TextSecondary,
            )
          }
        }

        val statusLabel = order.statusEnum.labelFor(order.isStorePickup)
        when {
          order.statusEnum == OrderStatus.DELIVERED -> SuccessBadge(statusLabel)
          order.statusEnum.isTerminalException -> DiscountBadge(statusLabel)
          else -> BrandPillBadge(
            text = statusLabel,
            containerColor = PrimaryYellow,
            contentColor = CharcoalBlack,
          )
        }
      }

      if (!order.assignedDriverName.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = RoyalBlueSoft,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
            Text(
              text = "المندوب المعيّن: ${order.assignedDriverName}" +
                (order.assignedDriverPhone?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = RoyalBlue,
            )
          }
        }
      }

      HorizontalDivider(color = BorderNeutral)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "الإجمالي: ${ArabicFormatter.formatCurrency(order.totalAmount)}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        Text(
          text = "${order.items.size} صنف (${order.totalUnitsCount} وحدة)",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "الدفع: ${order.paymentMethodEnum.arabicLabel}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
        Text(
          text = order.paymentStatusEnum.arabicLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = if (order.paymentStatus == "verified") SuccessGreen else TextSecondary,
        )
      }

      if (!order.statusNote.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = PrimaryYellowSoft,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "ملاحظة: ${order.statusNote}",
            style = MaterialTheme.typography.bodySmall,
            color = CharcoalBlack,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }

      // صف 1: توجيه لمندوب + الخريطة (تُخفى تلقائيًا لطلبات الاستلام من المتجر)
      if (!order.isStorePickup) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onAssignDriverClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("assign_driver_${order.id}"),
            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.5f)),
          ) {
            Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (order.assignedDriverId.isNullOrBlank()) "توجيه لمندوب" else "تغيير المندوب",
              color = RoyalBlue,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (order.hasCoordinates || order.streetAddress.isNotBlank()) {
            OutlinedButton(
              onClick = {
                LocationHelper.openLocationInMaps(
                  context = context,
                  latitude = order.latitude,
                  longitude = order.longitude,
                  fallbackAddress = "${order.governorate} - ${order.city} - ${order.streetAddress}",
                  label = "طلب #${order.orderNumber}",
                )
              },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("admin_order_map_${order.id}"),
              border = BorderStroke(1.dp, CharcoalBlack.copy(alpha = 0.4f)),
            ) {
              Icon(Icons.Filled.Map, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "عرض الخريطة",
                color = CharcoalBlack,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      // صف 2: طباعة/مشاركة الإيصال + التفاصيل
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (canPrepareAndPrint) {
          OutlinedButton(
            onClick = onPrintReceiptClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("print_receipt_${order.id}"),
            border = BorderStroke(1.dp, CharcoalBlack),
          ) {
            Icon(Icons.Filled.Print, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "طباعة الإيصال",
              color = CharcoalBlack,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        OutlinedButton(
          onClick = onViewDetailsClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("order_details_${order.id}"),
          border = BorderStroke(1.dp, BorderNeutral),
        ) {
          Icon(Icons.Filled.Info, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "التفاصيل",
            color = CharcoalBlack,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // صف 3: زر ترقية الحالة + زر تغيير الحالة المرن بسطر كامل بدون أي تقطيع للكلمات
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (nextStep != null) {
          Button(
            onClick = { onQuickNextStatus(nextStep) },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("quick_next_status_${order.id}"),
            colors = ButtonDefaults.buttonColors(
              containerColor = PrimaryYellow,
              contentColor = CharcoalBlack,
            ),
          ) {
            Text(
              text = "نقل إلى: ${nextStep.labelFor(order.isStorePickup)}",
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Button(
          onClick = onUpdateStatusClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("update_status_${order.id}"),
          colors = ButtonDefaults.buttonColors(
            containerColor = CharcoalBlack,
            contentColor = PureWhite,
          ),
        ) {
          Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "اختيار حالة أخرى",
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderDetailsAdminDialog(
  order: Order,
  canPrepareAndPrint: Boolean,
  onDismiss: () -> Unit,
  onPrintReceipt: () -> Unit,
  onAssignDriver: () -> Unit,
  onUpdateStatus: () -> Unit,
) {
  val context = LocalContext.current
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "تفاصيل الطلب (${order.orderNumber})",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = if (order.isStorePickup) "بيانات المستلم والاستلام من المتجر" else "بيانات المستلم والعنوان",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        Text(
          text = if (order.isStorePickup) {
            "الاسم: ${order.recipientName.ifBlank { order.customerName }}\n" +
              "الهاتف: ${order.phone}\n" +
              "طريقة الاستلام: الاستلام المباشر من المتجر\n" +
              "مقر الاستلام: ${order.streetAddress.ifBlank { "المقر الرئيسي للمتجر" }}"
          } else {
            "الاسم: ${order.recipientName.ifBlank { order.customerName }}\n" +
              "الهاتف: ${order.phone}\n" +
              "العنوان: ${order.governorate} - ${order.city}، ${order.streetAddress}" +
              (order.landmark?.takeIf { it.isNotBlank() }?.let { "\nعلامة مميزة: $it" } ?: "")
          },
          style = MaterialTheme.typography.bodySmall,
          color = TextPrimary,
        )
        if (!order.isStorePickup && (order.hasCoordinates || order.streetAddress.isNotBlank())) {
          OutlinedButton(
            onClick = {
              LocationHelper.openLocationInMaps(
                context = context,
                latitude = order.latitude,
                longitude = order.longitude,
                fallbackAddress = "${order.governorate} - ${order.city} - ${order.streetAddress}",
                label = "طلب #${order.orderNumber}",
              )
            },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.45f)),
          ) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("عرض موقع التوصيل على الخريطة", color = RoyalBlue, fontWeight = FontWeight.Bold)
          }
        }
        if (!order.assignedDriverName.isNullOrBlank()) {
          Text(
            text = "مندوب التوصيل: ${order.assignedDriverName} (${order.assignedDriverPhone.orEmpty()})",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = RoyalBlue,
          )
        }
        if (!order.notes.isNullOrBlank()) {
          Text(
            text = "ملاحظات العميل: ${order.notes}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        if (!order.displaySenderPhone.isNullOrBlank()) {
          Text(
            text = "الرقم المحوَّل منه: ${order.displaySenderPhone}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
        }

        HorizontalDivider(color = BorderNeutral)

        Text(
          text = "المنتجات (${order.items.size})",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        order.items.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.productName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
              )
              val variantText = listOfNotNull(
                item.selectedColor?.takeIf { it.isNotBlank() }?.let { "اللون: $it" },
                item.selectedSize?.takeIf { it.isNotBlank() }?.let { "المقاس: $it" },
              ).joinToString(" • ")
              if (variantText.isNotBlank()) {
                Text(
                  text = variantText,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  color = RoyalBlue,
                )
              }
              Text(
                text = "${item.quantity} × ${ArabicFormatter.formatCurrency(item.unitPrice)} (${if (item.pricingMode == "wholesale") "جملة" else "قطاعي"})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }
            Text(
              text = ArabicFormatter.formatCurrency(item.lineTotal),
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
          }
        }

        HorizontalDivider(color = BorderNeutral)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("المجموع الفرعي", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
          Text(ArabicFormatter.formatCurrency(order.subtotal), style = MaterialTheme.typography.bodySmall)
        }
        if (order.discountAmount > 0.0) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الخصم (${order.couponCode.orEmpty()})", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
            Text("- ${ArabicFormatter.formatCurrency(order.discountAmount)}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            text = if (order.isStorePickup) "الاستلام من المتجر" else "رسوم التوصيل",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
          Text(
            text = if (order.isStorePickup) "مجانًا" else ArabicFormatter.formatCurrency(order.deliveryFee),
            style = MaterialTheme.typography.bodySmall,
            color = if (order.isStorePickup) SuccessGreen else TextPrimary,
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("الإجمالي النهائي", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CharcoalBlack)
          Text(ArabicFormatter.formatCurrency(order.totalAmount), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CharcoalBlack)
        }
      }
    },
    confirmButton = {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (canPrepareAndPrint) {
          OutlinedButton(
            onClick = onPrintReceipt,
            border = BorderStroke(1.dp, CharcoalBlack),
          ) {
            Icon(Icons.Filled.Print, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("طباعة الإيصال", color = CharcoalBlack, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }
        }
        if (!order.isStorePickup) {
          OutlinedButton(
            onClick = onAssignDriver,
            border = BorderStroke(1.dp, RoyalBlue),
          ) {
            Text("توجيه لمندوب", color = RoyalBlue, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }
        }
        Button(
          onClick = onUpdateStatus,
          colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryYellow,
            contentColor = CharcoalBlack,
          ),
        ) {
          Text("تعديل الحالة", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إغلاق", color = TextSecondary)
      }
    },
  )
}

@Composable
private fun AssignDriverDialog(
  order: Order,
  drivers: List<StaffMember>,
  onDismiss: () -> Unit,
  onSelectDriver: (StaffMember) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "توجيه الطلب #${order.orderNumber} لموظف التوصيل",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (drivers.isEmpty()) {
          Text(
            text = "لا يوجد موظفو توصيل مسجلون حاليًا.\nيمكنك تعيين موظف بدور «موظف توصيل» أو منحه صلاحية توصيل الطلبات من قسم إدارة الموظفين.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        } else {
          Text(
            text = "اختر المندوب لإرسال إشعار له وتحديث حالة الطلب إلى «خرج للتوصيل»:",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
          drivers.forEach { driver ->
            val isCurrent = driver.uid == order.assignedDriverId
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isCurrent) PrimaryYellowSoft else GeneralBackground,
              border = BorderStroke(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) CharcoalBlack else BorderNeutral,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectDriver(driver) }
                .testTag("select_driver_${driver.uid}"),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = CharcoalBlack)
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = driver.fullName.ifBlank { driver.email },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                  if (driver.email.isNotBlank()) {
                    Text(driver.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                  }
                }
                if (isCurrent) {
                  SuccessBadge("معيّن حاليًا")
                } else {
                  Text("توجيه", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = RoyalBlue)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("إغلاق", color = TextSecondary)
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateOrderStatusDialog(
  order: Order,
  onDismiss: () -> Unit,
  onConfirm: (OrderStatus, String?) -> Unit,
) {
  var selectedStatus by remember(order) { mutableStateOf(order.statusEnum) }
  var noteText by remember(order) { mutableStateOf(order.statusNote.orEmpty()) }
  var statusExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "تحديث حالة الطلب (${order.orderNumber})",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        ExposedDropdownMenuBox(
          expanded = statusExpanded,
          onExpandedChange = { statusExpanded = it },
        ) {
          OutlinedTextField(
            value = selectedStatus.labelFor(order.isStorePickup),
            onValueChange = {},
            readOnly = true,
            label = {
              Text(if (order.isStorePickup) "حالة الطلب الجديدة (استلام من المتجر)" else "حالة الطلب الجديدة (من 9 حالات)")
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = statusExpanded,
            onDismissRequest = { statusExpanded = false },
            containerColor = PureWhite,
          ) {
            OrderStatus.entries
              .filter { !(order.isStorePickup && it == OrderStatus.OUT_FOR_DELIVERY) }
              .forEach { status ->
                DropdownMenuItem(
                  text = { Text(status.labelFor(order.isStorePickup)) },
                  onClick = {
                    selectedStatus = status
                    statusExpanded = false
                  },
                )
              }
          }
        }

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("ملاحظة الحالة للعميل أو الفريق (اختياري)") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedStatus, noteText.ifBlank { null }) },
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryYellow,
          contentColor = CharcoalBlack,
        ),
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
