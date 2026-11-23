package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.core.util.ArabicFormatter
import com.example.data.model.Order
import com.example.ui.components.AlEmanCard
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
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
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun PaymentReviewCard(
  order: Order,
  isSubmitting: Boolean,
  onApprove: () -> Unit,
  onReject: () -> Unit,
  onOpenFullDetails: () -> Unit,
  onViewReceipt: () -> Unit,
  onDeleteReceipt: () -> Unit,
) {
  val dateText = remember(order.createdAt) {
    order.createdAt?.toDate()?.let { date ->
      SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.forLanguageTag("ar-EG")).format(date)
    } ?: "حديث"
  }
  val hasReceipt = !order.receiptImageUrl.isNullOrBlank()
  val senderPhone = order.displaySenderPhone?.trim().orEmpty()
  val transferRef = order.transferReference?.trim().orEmpty()
  val showDistinctRef = transferRef.isNotBlank() && transferRef != senderPhone

  AlEmanCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("payment_review_card_${order.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 1. الترويسة: رقم الطلب + حالة الدفع
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "طلب #${order.orderNumber}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = CharcoalBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        PaymentStatusPill(paymentStatus = order.paymentStatus)
      }

      // شارة حالة الطلب الحالية
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = GeneralBackground,
        border = BorderStroke(1.dp, BorderNeutral),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "حالة الطلب الحالية",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = order.statusEnum.labelFor(order.isStorePickup),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // 2. بيانات الدفع
      PaymentSectionContainer(
        title = "بيانات الدفع",
        icon = Icons.Filled.Payments,
        containerColor = PrimaryYellowSoft.copy(alpha = 0.55f),
        borderColor = PrimaryYellow.copy(alpha = 0.7f),
      ) {
        PaymentKeyValueRow(
          label = "وسيلة الدفع",
          value = order.paymentMethodEnum.arabicLabel,
          valueColor = CharcoalBlack,
          valueBold = true,
        )
        PaymentKeyValueRow(
          label = "المبلغ المطلوب",
          value = ArabicFormatter.formatCurrency(order.totalAmount),
          valueColor = SuccessGreen,
          valueBold = true,
        )
        if (senderPhone.isNotBlank()) {
          PaymentKeyValueRow(
            label = "الرقم المحوَّل منه",
            value = senderPhone,
            valueColor = CharcoalBlack,
            valueBold = true,
          )
        }
        if (showDistinctRef) {
          PaymentKeyValueRow(
            label = "مرجع التحويل",
            value = transferRef,
            valueColor = TextPrimary,
            valueBold = false,
          )
        }
        if (order.deliveryFee > 0.0 || order.discountAmount > 0.0) {
          HorizontalDivider(color = PrimaryYellow.copy(alpha = 0.45f))
          PaymentKeyValueRow(
            label = "المنتجات",
            value = ArabicFormatter.formatCurrency(order.subtotal),
          )
          if (order.deliveryFee > 0.0) {
            PaymentKeyValueRow(
              label = "التوصيل",
              value = ArabicFormatter.formatCurrency(order.deliveryFee),
            )
          }
          if (order.discountAmount > 0.0) {
            PaymentKeyValueRow(
              label = "الخصم",
              value = "- ${ArabicFormatter.formatCurrency(order.discountAmount)}",
              valueColor = DiscountRed,
            )
          }
        }
      }

      if (!order.verifiedByDeviceName.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = SuccessGreenSoft,
          border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.PhoneAndroid,
              contentDescription = null,
              tint = SuccessGreen,
              modifier = Modifier.size(16.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "تم التأكيد على هاتف: ${order.verifiedByDeviceName}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = SuccessGreen,
              )
              if (!order.verifiedByEmail.isNullOrBlank()) {
                Text(
                  text = order.verifiedByEmail,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      // 3. ملخص بيانات الطلب والعميل
      PaymentSectionContainer(
        title = "بيانات الطلب والعميل",
        icon = Icons.Filled.ShoppingBag,
        containerColor = GeneralBackground,
        borderColor = BorderNeutral,
      ) {
        PaymentKeyValueRow(
          label = "العميل",
          value = order.customerName.ifBlank { order.recipientName },
          valueColor = CharcoalBlack,
          valueBold = true,
        )
        if (order.phone.isNotBlank()) {
          PaymentKeyValueRow(
            label = "رقم الهاتف",
            value = order.phone,
            valueColor = TextPrimary,
          )
        }
        val locationLabel = if (order.isStorePickup) {
          "استلام مباشر من المتجر"
        } else {
          listOf(order.governorate, order.city, order.streetAddress)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
        }
        if (locationLabel.isNotBlank()) {
          PaymentKeyValueRow(
            label = if (order.isStorePickup) "الاستلام" else "العنوان",
            value = locationLabel,
            valueColor = TextSecondary,
          )
        }
        if (order.items.isNotEmpty()) {
          HorizontalDivider(color = BorderNeutral)
          Text(
            text = "الأصناف (${order.items.size} صنف • ${order.totalUnitsCount} قطعة)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
          order.items.take(2).forEach { item ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "• ${item.productName}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                  color = TextPrimary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                val variant = listOfNotNull(
                  item.selectedColor?.takeIf { it.isNotBlank() }?.let { "اللون: $it" },
                  item.selectedSize?.takeIf { it.isNotBlank() }?.let { "المقاس: $it" },
                ).joinToString(" • ")
                if (variant.isNotBlank()) {
                  Text(
                    text = variant,
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalBlue,
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${item.quantity} × ${ArabicFormatter.formatCurrency(item.unitPrice)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
          if (order.items.size > 2) {
            Text(
              text = "+ ${order.items.size - 2} أصناف أخرى (اضغط «عرض كامل التفاصيل»)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = RoyalBlue,
            )
          }
        }
        if (!order.notes.isNullOrBlank()) {
          Text(
            text = "ملاحظة العميل: ${order.notes}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
      }

      // 4. قسم صورة إيصال التحويل (عرض واضح كامل داخل الكارت)
      if (hasReceipt) {
        PaymentSectionContainer(
          title = "صورة إيصال التحويل المرفقة",
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          containerColor = PureWhite,
          borderColor = CharcoalBlack.copy(alpha = 0.2f),
        ) {
          PaymentReceiptImagePreview(
            receiptImageUrl = order.receiptImageUrl.orEmpty(),
            orderNumber = order.orderNumber,
            height = 190.dp,
            onZoomClick = onViewReceipt,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onViewReceipt,
              shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
              border = BorderStroke(1.dp, CharcoalBlack),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("admin_view_receipt_${order.id}"),
            ) {
              Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "تكبير الإيصال",
                color = CharcoalBlack,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
            OutlinedButton(
              onClick = onDeleteReceipt,
              enabled = !isSubmitting,
              shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
              border = BorderStroke(1.dp, DiscountRed),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("admin_delete_receipt_${order.id}"),
            ) {
              Icon(Icons.Filled.Delete, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "حذف الصورة",
                color = DiscountRed,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Info,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "لم يرفق العميل صورة إيصال — يرجى التحقق من رقم المحوّل منه والمبلغ.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
        }
      }

      // 5. أزرار الإجراءات (عريضة ومنظمة بدون تقطيع كلمات)
      OutlinedButton(
        onClick = onOpenFullDetails,
        shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.55f)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("admin_payment_full_details_${order.id}"),
      ) {
        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "عرض كامل تفاصيل الطلب والإيصال",
          color = RoyalBlue,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (order.paymentStatus == "pending_review") {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onApprove,
            enabled = !isSubmitting,
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureWhite),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("admin_approve_payment_${order.id}"),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "تأكيد الدفع وبدء التجهيز",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
          OutlinedButton(
            onClick = onReject,
            enabled = !isSubmitting,
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            border = BorderStroke(1.dp, DiscountRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DiscountRed),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("admin_reject_payment_${order.id}"),
          ) {
            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "رفض التحويل",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun PaymentOrderReviewDialog(
  order: Order,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onApprove: () -> Unit,
  onReject: () -> Unit,
  onViewReceiptFullscreen: () -> Unit,
  onDeleteReceipt: () -> Unit,
) {
  val dateText = remember(order.createdAt) {
    order.createdAt?.toDate()?.let { date ->
      SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.forLanguageTag("ar-EG")).format(date)
    } ?: "حديث"
  }
  val hasReceipt = !order.receiptImageUrl.isNullOrBlank()
  val senderPhone = order.displaySenderPhone?.trim().orEmpty()
  val transferRef = order.transferReference?.trim().orEmpty()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = PureWhite,
      tonalElevation = 4.dp,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 16.dp)
        .testTag("payment_order_review_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // الترويسة العلوية
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryYellowSoft)
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "مراجعة الدفع والطلب #${order.orderNumber}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = CharcoalBlack,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = dateText,
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          PaymentStatusPill(paymentStatus = order.paymentStatus)
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = CharcoalBlack)
          }
        }

        // المحتوى القابل للتمرير
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // 1. الإيصال المرفق
          PaymentSectionContainer(
            title = "إيصال التحويل",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            containerColor = GeneralBackground,
            borderColor = BorderNeutral,
          ) {
            if (hasReceipt) {
              PaymentReceiptImagePreview(
                receiptImageUrl = order.receiptImageUrl.orEmpty(),
                orderNumber = order.orderNumber,
                height = 230.dp,
                onZoomClick = onViewReceiptFullscreen,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = onViewReceiptFullscreen,
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  border = BorderStroke(1.dp, CharcoalBlack),
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                ) {
                  Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "تكبير كامل",
                    color = CharcoalBlack,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
                OutlinedButton(
                  onClick = onDeleteReceipt,
                  enabled = !isSubmitting,
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  border = BorderStroke(1.dp, DiscountRed),
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                ) {
                  Icon(Icons.Filled.Delete, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "حذف الصورة",
                    color = DiscountRed,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            } else {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Filled.ImageNotSupported, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Text(
                  text = "لم يقم العميل بإرفاق صورة إيصال لهذا الطلب.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              }
            }
          }

          // 2. بيانات الدفع والفاتورة
          PaymentSectionContainer(
            title = "بيانات الدفع والفاتورة",
            icon = Icons.Filled.Payments,
            containerColor = PrimaryYellowSoft.copy(alpha = 0.55f),
            borderColor = PrimaryYellow.copy(alpha = 0.7f),
          ) {
            PaymentKeyValueRow("وسيلة الدفع", order.paymentMethodEnum.arabicLabel, CharcoalBlack, true)
            if (senderPhone.isNotBlank()) {
              PaymentKeyValueRow("الرقم المحوَّل منه", senderPhone, CharcoalBlack, true)
            }
            if (transferRef.isNotBlank() && transferRef != senderPhone) {
              PaymentKeyValueRow("مرجع التحويل", transferRef, TextPrimary, false)
            }
            HorizontalDivider(color = PrimaryYellow.copy(alpha = 0.45f))
            PaymentKeyValueRow("إجمالي المنتجات", ArabicFormatter.formatCurrency(order.subtotal))
            PaymentKeyValueRow(
              label = "رسوم التوصيل",
              value = if (order.deliveryFee <= 0.0) "مجاني" else ArabicFormatter.formatCurrency(order.deliveryFee),
            )
            if (order.discountAmount > 0.0) {
              val couponSuffix = order.couponCode?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
              PaymentKeyValueRow(
                label = "الخصم$couponSuffix",
                value = "- ${ArabicFormatter.formatCurrency(order.discountAmount)}",
                valueColor = DiscountRed,
                valueBold = true,
              )
            }
            HorizontalDivider(color = PrimaryYellow.copy(alpha = 0.65f))
            PaymentKeyValueRow(
              label = "الإجمالي المطلوب",
              value = ArabicFormatter.formatCurrency(order.totalAmount),
              valueColor = SuccessGreen,
              valueBold = true,
            )
            if (!order.verifiedByDeviceName.isNullOrBlank()) {
              PaymentKeyValueRow(
                label = "تم التأكيد على هاتف",
                value = order.verifiedByDeviceName,
                valueColor = SuccessGreen,
                valueBold = true,
              )
            }
          }

          // 3. بيانات العميل والتوصيل
          PaymentSectionContainer(
            title = "بيانات العميل والتوصيل",
            icon = if (order.isStorePickup) Icons.Filled.Person else Icons.Filled.LocalShipping,
            containerColor = GeneralBackground,
            borderColor = BorderNeutral,
          ) {
            PaymentKeyValueRow("العميل", order.customerName.ifBlank { order.recipientName }, CharcoalBlack, true)
            if (order.recipientName.isNotBlank() && order.recipientName != order.customerName) {
              PaymentKeyValueRow("المستلم", order.recipientName, TextPrimary)
            }
            if (order.phone.isNotBlank()) {
              PaymentKeyValueRow("رقم الهاتف", order.phone, CharcoalBlack, true)
            }
            PaymentKeyValueRow("حالة الطلب", order.statusEnum.labelFor(order.isStorePickup), CharcoalBlack, true)
            if (order.isStorePickup) {
              PaymentKeyValueRow("طريقة الاستلام", "استلام مباشر من المتجر", RoyalBlue, true)
            } else {
              val addressText = listOf(order.governorate, order.city, order.streetAddress)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
              if (addressText.isNotBlank()) {
                PaymentKeyValueRow("العنوان", addressText, TextPrimary)
              }
              if (!order.landmark.isNullOrBlank()) {
                PaymentKeyValueRow("علامة مميزة", order.landmark, TextSecondary)
              }
            }
            if (!order.notes.isNullOrBlank()) {
              PaymentKeyValueRow("ملاحظات العميل", order.notes, TextSecondary)
            }
          }

          // 4. قائمة المنتجات بالكامل
          PaymentSectionContainer(
            title = "المنتجات المطلوبة (${order.items.size})",
            icon = Icons.Filled.ShoppingBag,
            containerColor = PureWhite,
            borderColor = BorderNeutral,
          ) {
            order.items.forEachIndexed { idx, item ->
              if (idx > 0) HorizontalDivider(color = BorderNeutral.copy(alpha = 0.7f))
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Text(
                  text = item.productName,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = CharcoalBlack,
                )
                val variantText = listOfNotNull(
                  item.selectedColor?.takeIf { it.isNotBlank() }?.let { "اللون: $it" },
                  item.selectedSize?.takeIf { it.isNotBlank() }?.let { "المقاس: $it" },
                ).joinToString(" • ")
                if (variantText.isNotBlank()) {
                  Text(
                    text = variantText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = RoyalBlue,
                  )
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  val modeLabel = if (item.pricingMode == "wholesale") "جملة" else "قطاعي"
                  Text(
                    text = "${item.quantity} × ${ArabicFormatter.formatCurrency(item.unitPrice)} ($modeLabel)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                  )
                  val computedLineTotal = if (item.lineTotal > 0.0) item.lineTotal else item.unitPrice * item.quantity
                  Text(
                    text = ArabicFormatter.formatCurrency(computedLineTotal),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }

        // شريط الإجراءات السفلي
        HorizontalDivider(color = BorderNeutral)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (order.paymentStatus == "pending_review") {
            Button(
              onClick = onApprove,
              enabled = !isSubmitting,
              shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
              colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureWhite),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("dialog_approve_payment_${order.id}"),
            ) {
              Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "تأكيد الدفع وبدء التجهيز",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
              )
            }
            OutlinedButton(
              onClick = onReject,
              enabled = !isSubmitting,
              shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
              border = BorderStroke(1.dp, DiscountRed),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = DiscountRed),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("dialog_reject_payment_${order.id}"),
            ) {
              Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "رفض التحويل",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
              )
            }
          } else {
            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
              colors = ButtonDefaults.buttonColors(containerColor = CharcoalBlack, contentColor = PrimaryYellow),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            ) {
              Text(
                text = "إغلاق",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun PaymentReceiptImagePreview(
  receiptImageUrl: String,
  orderNumber: String,
  height: Dp,
  onZoomClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(10.dp))
      .background(GeneralBackground)
      .clickable(onClick = onZoomClick),
    contentAlignment = Alignment.Center,
  ) {
    SubcomposeAsyncImage(
      model = receiptImageUrl,
      contentDescription = "إيصال طلب #$orderNumber",
      contentScale = ContentScale.Fit,
      modifier = Modifier
        .fillMaxSize()
        .padding(6.dp),
      loading = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = CharcoalBlack, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
        }
      },
      error = {
        Column(
          modifier = Modifier.padding(12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(Icons.Filled.ImageNotSupported, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(28.dp))
          Text(
            text = "تعذر تحميل صورة الإيصال، اضغط لمحاولة فتحها بحجم كامل",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
          )
        }
      },
    )

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = CharcoalBlack.copy(alpha = 0.85f),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(8.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = PrimaryYellow, modifier = Modifier.size(14.dp))
        Text(
          text = "اضغط للتكبير",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = PrimaryYellow,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }
}

@Composable
private fun PaymentStatusPill(paymentStatus: String) {
  when (paymentStatus) {
    "verified" -> SuccessBadge("تم التأكيد")
    "rejected" -> DiscountBadge("مرفوض")
    else -> BrandPillBadge(
      text = "بانتظار التأكيد",
      containerColor = PrimaryYellow,
      contentColor = CharcoalBlack,
    )
  }
}

@Composable
private fun PaymentSectionContainer(
  title: String,
  icon: ImageVector,
  containerColor: Color,
  borderColor: Color,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(icon, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(17.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
          color = CharcoalBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      content()
    }
  }
}

@Composable
private fun PaymentKeyValueRow(
  label: String,
  value: String,
  valueColor: Color = TextPrimary,
  valueBold: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary,
      maxLines = 1,
      softWrap = false,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(
        fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Medium,
      ),
      color = valueColor,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(1f),
    )
  }
}
