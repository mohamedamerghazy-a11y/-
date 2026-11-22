package com.example.ui.screens.admin

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.location.LocationHelper
import com.example.core.model.OrderStatus
import com.example.core.util.ArabicFormatter
import com.example.data.model.Order
import com.example.data.model.normalizeOtpDigits
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DeliveryDriverUiState

@Composable
fun DeliveryDriverOrdersScreen(
  uiState: DeliveryDriverUiState,
  focusedOrderId: String? = null,
  onToggleShowCompleted: (Boolean) -> Unit,
  onConfirmDelivered: (Order, String?, String?) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var confirmingOrder by remember { mutableStateOf<Order?>(null) }
  var deliveryNote by remember { mutableStateOf("") }
  var otpInput by remember { mutableStateOf("") }
  var otpInlineError by remember { mutableStateOf<String?>(null) }

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
        title = "طلبات التوصيل — المندوب",
        subtitle = "الأوردرات الموجهة إليك ومواقع التسليم والتحصيل",
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
          Column(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
          ) {
            // شريط التبويب بين الطلبات الحالية والمكتملة
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = !uiState.showCompleted,
                onClick = { onToggleShowCompleted(false) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryYellow,
                  selectedLabelColor = CharcoalBlack,
                ),
                label = {
                  Text(
                    text = "طلبات جارية (${uiState.activeOrders.size})",
                    fontWeight = if (!uiState.showCompleted) FontWeight.Bold else FontWeight.Normal,
                  )
                },
              )
              FilterChip(
                selected = uiState.showCompleted,
                onClick = { onToggleShowCompleted(true) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryYellow,
                  selectedLabelColor = CharcoalBlack,
                ),
                label = {
                  Text(
                    text = "تم تسليمها / منتهية (${uiState.completedOrders.size})",
                    fontWeight = if (uiState.showCompleted) FontWeight.Bold else FontWeight.Normal,
                  )
                },
              )
            }

            if (uiState.visibleOrders.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(AlEmanDimens.SpaceLg),
                contentAlignment = Alignment.Center,
              ) {
                StoreEmptyStateCard(
                  icon = Icons.Filled.DeliveryDining,
                  title = if (uiState.showCompleted) "لا توجد طلبات مكتملة بعد" else "لا توجد طلبات توصيل موجهة إليك حاليًا",
                  description = "سيصلك إشعار صوتي فوري بمجرد توجيه أي طلب جديد لك من الإدارة.",
                  actionLabel = "العودة",
                  onActionClick = onBackClick,
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("driver_orders_lazy_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                val sorted = if (!focusedOrderId.isNullOrBlank()) {
                  uiState.visibleOrders.sortedByDescending { it.id == focusedOrderId }
                } else {
                  uiState.visibleOrders
                }
                items(sorted, key = { it.id }) { order ->
                  DriverOrderCard(
                    order = order,
                    isFocused = order.id == focusedOrderId,
                    isSubmitting = uiState.isSubmitting,
                    onCallCustomer = {
                      runCatching {
                        context.startActivity(
                          Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                          },
                        )
                      }
                    },
                    onOpenMap = {
                      LocationHelper.openLocationInMaps(
                        context = context,
                        latitude = order.latitude,
                        longitude = order.longitude,
                        fallbackAddress = "${order.governorate} - ${order.city} - ${order.streetAddress}",
                        label = "طلب #${order.orderNumber} - ${order.recipientName}",
                      )
                    },
                      onMarkDeliveredClick = {
                        deliveryNote = ""
                        otpInput = ""
                        otpInlineError = null
                        confirmingOrder = order
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

  val targetOrder = confirmingOrder
  if (targetOrder != null) {
    val requiresOtp = !targetOrder.deliveryOtp.isNullOrBlank()
    AlertDialog(
      onDismissRequest = { confirmingOrder = null },
      containerColor = PureWhite,
      title = {
        Text(
          text = "تأكيد تسليم الطلب #${targetOrder.orderNumber}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "الإجمالي المطلوب تحصيله: ${ArabicFormatter.formatCurrency(targetOrder.totalAmount)} (${targetOrder.paymentMethodEnum.arabicLabel})",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = RoyalBlue,
          )
          if (requiresOtp) {
            OutlinedTextField(
              value = otpInput,
              onValueChange = { raw ->
                otpInput = normalizeOtpDigits(raw).take(4)
                otpInlineError = null
              },
              label = { Text("كود تأكيد الاستلام من العميل (4 أرقام)") },
              placeholder = { Text("مثال: 4821") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              isError = otpInlineError != null,
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("driver_delivery_otp_input"),
            )
            if (!otpInlineError.isNullOrBlank()) {
              Text(
                text = otpInlineError.orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = DiscountRed,
              )
            }
          }
          OutlinedTextField(
            value = deliveryNote,
            onValueChange = { deliveryNote = it },
            label = { Text("ملاحظة التسليم (اختياري)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (requiresOtp) {
              val cleanEntered = normalizeOtpDigits(otpInput)
              if (cleanEntered.length != 4 || cleanEntered != normalizeOtpDigits(targetOrder.deliveryOtp.orEmpty())) {
                otpInlineError = "كود التأكيد غير صحيح، اطلب الكود الصحيح (4 أرقام) من العميل."
                return@Button
              }
            }
            onConfirmDelivered(
              targetOrder,
              deliveryNote.trim().takeIf { it.isNotEmpty() },
              otpInput.trim().takeIf { it.isNotEmpty() },
            )
            confirmingOrder = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
          modifier = Modifier.testTag("driver_confirm_delivered_dialog_btn"),
        ) {
          Text("تم التسليم بنجاح", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmingOrder = null }) {
          Text("تراجع", color = TextSecondary)
        }
      },
    )
  }
}

@Composable
private fun DriverOrderCard(
  order: Order,
  isFocused: Boolean,
  isSubmitting: Boolean,
  onCallCustomer: () -> Unit,
  onOpenMap: () -> Unit,
  onMarkDeliveredClick: () -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (isFocused) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = PrimaryYellowSoft,
          border = BorderStroke(1.dp, PrimaryYellow),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "الطلب المحدد من الإشعار",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "طلب #${order.orderNumber.ifBlank { order.id.take(8) }}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
          Text(
            text = "المستلم: ${order.recipientName.ifBlank { order.customerName }}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
          )
        }
        if (order.statusEnum == OrderStatus.DELIVERED) {
          SuccessBadge("تم التسليم")
        } else {
          BrandPillBadge(
            text = order.statusEnum.arabicLabel,
            containerColor = PrimaryYellow,
            contentColor = CharcoalBlack,
          )
        }
      }

      if (order.hasPendingDeliveryOtp) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = PrimaryYellowSoft,
          border = BorderStroke(1.dp, PrimaryYellow),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(15.dp))
            Text(
              text = "يتطلب إدخال كود تأكيد الاستلام من العميل عند التسليم",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
          }
        }
      }

      // الإجمالي وطريقة الدفع
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = RoyalBlueSoft,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("المطلوب عند التسليم", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
              text = ArabicFormatter.formatCurrency(order.totalAmount),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = RoyalBlue,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(order.paymentMethodEnum.arabicLabel, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = CharcoalBlack)
            Text(
              text = order.paymentStatusEnum.arabicLabel,
              style = MaterialTheme.typography.labelSmall,
              color = if (order.paymentStatus == "verified") SuccessGreen else TextSecondary,
            )
          }
        }
      }

      // موقع التوصيل وتفاصيل العنوان
      Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "${order.deliveryLabel.ifBlank { "العنوان" }} • ${order.governorate} - ${order.city}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
          )
          Text(
            text = order.streetAddress,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
          if (!order.landmark.isNullOrBlank()) {
            Text("علامة مميزة: ${order.landmark}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          }
          if (!order.notes.isNullOrBlank()) {
            Text("ملاحظات العميل: ${order.notes}", style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
          }
        }
      }

      HorizontalDivider(color = BorderNeutral)

      // تفاصيل الأصناف باختصار منظم
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "تفاصيل الطلب (${order.totalUnitsCount} وحدة):",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        order.items.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${item.quantity} ${item.unit} × ${item.productName}",
              style = MaterialTheme.typography.bodySmall,
              color = TextPrimary,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = ArabicFormatter.formatCurrency(item.lineTotal),
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = CharcoalBlack,
            )
          }
        }
      }

      // أزرار الموقع والاتصال بالعميل
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onCallCustomer,
          modifier = Modifier.weight(1f).height(44.dp),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          border = BorderStroke(1.dp, CharcoalBlack.copy(alpha = 0.45f)),
        ) {
          Icon(Icons.Filled.Phone, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("اتصال بالعميل", color = CharcoalBlack, fontWeight = FontWeight.Bold)
        }

        if (order.hasCoordinates || order.streetAddress.isNotBlank()) {
          OutlinedButton(
            onClick = onOpenMap,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.6f)),
          ) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("عرض الخريطة", color = RoyalBlue, fontWeight = FontWeight.Bold)
          }
        }
      }

      if (order.statusEnum != OrderStatus.DELIVERED && !order.statusEnum.isTerminalException) {
        Button(
          onClick = onMarkDeliveredClick,
          enabled = !isSubmitting,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("driver_deliver_${order.id}"),
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryYellow,
            contentColor = CharcoalBlack,
          ),
        ) {
          Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("تأكيد تسليم الطلب", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
