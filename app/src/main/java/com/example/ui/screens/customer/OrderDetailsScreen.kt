package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.location.LocationHelper
import com.example.core.util.ArabicFormatter
import com.example.data.model.Order
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanPrimaryButton
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.OrderTimelineStepper
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.PriceSummaryRows
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.ZoomableImagePreviewDialog
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CartAndOrdersUiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrderDetailsScreen(
  orderId: String,
  uiState: CartAndOrdersUiState,
  onReorder: (Order) -> Unit,
  onCancelOrder: (orderId: String, reason: String?) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  val order = remember(uiState.orders, orderId) {
    uiState.orders.firstOrNull { it.id == orderId }
  }
  val context = LocalContext.current
  var showCancelDialog by remember { mutableStateOf(false) }
  var showReceiptZoom by remember { mutableStateOf(false) }
  var cancelReason by remember { mutableStateOf("") }
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
        title = order?.let { "طلب #${it.orderNumber}" } ?: "تفاصيل الطلب",
        subtitle = "التتبع والفاتورة المحفوظة",
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
      if (order == null) {
        Column(
          modifier = Modifier
            .widthIn(max = 600.dp)
            .padding(AlEmanDimens.SpaceLg),
        ) {
          StoreEmptyStateCard(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = "جاري البحث عن الطلب...",
            description = "إذا استمر عدم ظهور الطلب، تأكد من اتصالك بالإنترنت أو ارجع لقائمة الطلبات.",
            actionLabel = "العودة",
            onActionClick = onBackClick,
          )
        }
        return@Box
      }

      val dateText = remember(order.createdAt) {
        order.createdAt?.toDate()?.let { date ->
          SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.forLanguageTag("ar-EG")).format(date)
        } ?: "الآن"
      }

      LazyColumn(
        modifier = Modifier
          .widthIn(max = 840.dp)
          .fillMaxSize(),
        contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // 1. تتبع المراحل
        item {
          OrderTimelineStepper(
            currentStatus = order.statusEnum,
            isStorePickup = order.isStorePickup,
          )
        }

        // 2. ملاحظة الحالة أو الإلغاء إن وُجدت
        if (!order.statusNote.isNullOrBlank()) {
          item {
            Surface(
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              color = GoldenYellowSoft,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "تحديث حول الطلب",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimary,
                )
                Text(
                  text = order.statusNote,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextPrimary,
                )
              }
            }
          }
        }

        if (order.hasPendingDeliveryOtp) {
          item {
            val formattedOtp = ArabicFormatter.formatDigits(order.deliveryOtp.orEmpty()).toList().joinToString("  ")
            Surface(
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              color = PrimaryYellowSoft,
              border = BorderStroke(1.5.dp, PrimaryYellow),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("order_delivery_otp_card"),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = CharcoalBlack,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "كود تأكيد الاستلام للمندوب",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                }
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = PureWhite,
                  border = BorderStroke(1.dp, CharcoalBlack.copy(alpha = 0.25f)),
                ) {
                  Text(
                    text = formattedOtp,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = RoyalBlue,
                    modifier = Modifier
                      .padding(horizontal = 24.dp, vertical = 8.dp)
                      .testTag("order_delivery_otp_value"),
                  )
                }
                Text(
                  text = "أعطِ هذا الكود للمندوب فقط بعد استلامك للطلب والتأكد من سلامة المنتجات.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextPrimary,
                )
              }
            }
          }
        } else if (order.deliveryOtpVerified && !order.isStorePickup) {
          item {
            Surface(
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              color = SuccessGreen.copy(alpha = 0.1f),
              border = BorderStroke(1.dp, SuccessGreen),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.VerifiedUser,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "تم تأكيد الاستلام بكود التحقق بنجاح ✓",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = SuccessGreen,
                )
              }
            }
          }
        }

        // 3. بيانات التوصيل أو الاستلام من المتجر
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.LocationOn,
                  contentDescription = null,
                  tint = RoyalBlue,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = if (order.isStorePickup) "بيانات الاستلام من المتجر" else "بيانات التوصيل المسجلة",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = RoyalBlue,
                )
              }
              Text(
                text = "${order.deliveryLabel} • ${order.recipientName}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
              )
              Text(
                text = "الهاتف: ${order.phone}",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
              )
              Text(
                text = if (order.isStorePickup) {
                  "مقر الاستلام: ${order.streetAddress}"
                } else {
                  "${order.governorate} - ${order.city} - ${order.streetAddress}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
              if (!order.landmark.isNullOrBlank()) {
                Text(
                  text = "علامة مميزة: ${order.landmark}",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              }
              if (!order.isStorePickup && !order.deliveryZoneName.isNullOrBlank()) {
                Text(
                  text = "منطقة الشحن: ${order.deliveryZoneName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = RoyalBlue,
                )
              }
              if (!order.notes.isNullOrBlank()) {
                HorizontalDivider(color = BorderNeutral)
                Text(
                  text = "ملاحظاتك: ${order.notes}",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextPrimary,
                )
              }
              if (!order.isStorePickup && (order.hasCoordinates || order.streetAddress.isNotBlank())) {
                Spacer(modifier = Modifier.height(4.dp))
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
                  modifier = Modifier.fillMaxWidth().height(44.dp),
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.45f)),
                ) {
                  Icon(Icons.Filled.Map, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("عرض الموقع على الخريطة", color = RoyalBlue, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        if (!order.assignedDriverName.isNullOrBlank()) {
          item {
            AlEmanCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.LocalShipping,
                  contentDescription = null,
                  tint = RoyalBlue,
                  modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "مندوب التوصيل المعيّن",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                  Text(
                    text = order.assignedDriverName.orEmpty(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                  if (!order.assignedDriverPhone.isNullOrBlank()) {
                    Text(
                      text = order.assignedDriverPhone.orEmpty(),
                      style = MaterialTheme.typography.bodySmall,
                      color = RoyalBlue,
                    )
                  }
                }
                if (!order.assignedDriverPhone.isNullOrBlank()) {
                  OutlinedButton(
                    onClick = {
                      runCatching {
                        context.startActivity(
                          Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.assignedDriverPhone}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                          },
                        )
                      }
                    },
                    shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                    border = BorderStroke(1.dp, CharcoalBlack.copy(alpha = 0.4f)),
                  ) {
                    Icon(Icons.Filled.Phone, contentDescription = "اتصال بالمندوب", tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("اتصال", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        // 4. بيانات الدفع
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
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
                  Icon(
                    imageVector = Icons.Filled.Payments,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "معلومات الدفع",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoyalBlue,
                  )
                }
                PaymentStatusBadge(status = order.paymentStatusEnum)
              }

              Text(
                text = "الطريقة: ${order.paymentMethodEnum.arabicLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
              )
              if (!order.displaySenderPhone.isNullOrBlank()) {
                Text(
                  text = "الرقم المحوَّل منه: ${order.displaySenderPhone}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                  color = CharcoalBlack,
                )
              }
              if (!order.receiptImageUrl.isNullOrBlank()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
                    .background(GeneralBackground)
                    .clickable { showReceiptZoom = true }
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ProductThumbnail(
                    imageUrl = order.receiptImageUrl,
                    contentDescription = "إيصال التحويل المرفق",
                    modifier = Modifier
                      .size(56.dp)
                      .clip(RoundedCornerShape(6.dp)),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "إيصال التحويل المرفق",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                    Text(
                      text = "اضغط لعرض الإيصال بالحجم الكامل",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                  }
                  Icon(
                    imageVector = Icons.Filled.ZoomIn,
                    contentDescription = "تكبير الإيصال",
                    tint = CharcoalBlack,
                  )
                }
              }
              Text(
                text = "تاريخ الطلب: $dateText",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }
          }
        }

        // 5. الأصناف والفاتورة (الأسعار المحفوظة)
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "الأصناف المطلوبة (${order.items.size})",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = RoyalBlue,
                )
                if (order.accountTypeAtOrder == "wholesale") {
                  BrandPillBadge(
                    text = "أسعار جملة",
                    containerColor = GoldenYellowSoft,
                    contentColor = TextPrimary,
                  )
                }
              }

              order.items.forEachIndexed { idx, item ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  ProductThumbnail(
                    imageUrl = item.imageUrl,
                    contentDescription = item.productName,
                    modifier = Modifier
                      .size(56.dp)
                      .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
                      .background(GeneralBackground),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = item.productName,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                      color = TextPrimary,
                    )
                    val variantLabel = listOfNotNull(
                      item.selectedColor?.takeIf { it.isNotBlank() }?.let { "اللون: $it" },
                      item.selectedSize?.takeIf { it.isNotBlank() }?.let { "المقاس: $it" },
                    ).joinToString(" • ")
                    if (variantLabel.isNotBlank()) {
                      Text(
                        text = variantLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = RoyalBlue,
                      )
                    }
                    val modeTag = buildString {
                      if (item.pricingMode == "wholesale") append(" (جملة)")
                      if (!item.flashSaleId.isNullOrBlank()) append(" • فلاش سيل ⚡")
                    }
                    Text(
                      text = "${item.quantity} ${item.unit} × ${ArabicFormatter.formatCurrency(item.unitPrice)}$modeTag",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                  }
                  Text(
                    text = ArabicFormatter.formatCurrency(item.lineTotal),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                  )
                }
                if (idx < order.items.lastIndex) {
                  HorizontalDivider(color = BorderNeutral)
                }
              }

              HorizontalDivider(color = BorderNeutral)

              PriceSummaryRows(
                subtotal = order.subtotal,
                discountAmount = order.discountAmount,
                deliveryFee = order.deliveryFee,
                totalAmount = order.totalAmount,
                isStorePickup = order.isStorePickup,
              )
            }
          }
        }

        // 6. إجراءات العميل: إعادة الطلب + الإلغاء (متاح فقط في البداية)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AlEmanPrimaryButton(
              text = "إعادة طلب نفس المنتجات",
              onClick = { onReorder(order) },
              icon = Icons.Filled.Replay,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("order_details_reorder_button"),
            )

            if (order.canCustomerCancel) {
              OutlinedButton(
                onClick = { showCancelDialog = true },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("order_details_cancel_button"),
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.55f)),
              ) {
                Icon(
                  imageVector = Icons.Filled.Cancel,
                  contentDescription = null,
                  tint = DiscountRed,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "إلغاء الطلب",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = DiscountRed,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCancelDialog && order != null) {
    AlertDialog(
      onDismissRequest = { showCancelDialog = false },
      containerColor = PureWhite,
      title = {
        Text(
          text = "تأكيد إلغاء الطلب #${order.orderNumber}",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = DiscountRed,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "هل أنت متأكد من رغبتك في إلغاء هذا الطلب؟ لا يمكن التراجع بعد الإلغاء.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
          )
          OutlinedTextField(
            value = cancelReason,
            onValueChange = { cancelReason = it },
            label = { Text("سبب الإلغاء (اختياري)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showCancelDialog = false
            onCancelOrder(order.id, cancelReason.trim().takeIf { it.isNotEmpty() })
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
          modifier = Modifier.testTag("confirm_cancel_order_button"),
        ) {
          Text("نعم، إلغاء الطلب")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelDialog = false }) {
          Text("تراجع", color = RoyalBlue)
        }
      },
    )
  }

  val receiptUrl = order?.receiptImageUrl
  if (showReceiptZoom && !receiptUrl.isNullOrBlank()) {
    ZoomableImagePreviewDialog(
      imageUrl = receiptUrl,
      title = "إيصال التحويل • طلب #${order?.orderNumber.orEmpty()}",
      onDismiss = { showReceiptZoom = false },
    )
  }
}
