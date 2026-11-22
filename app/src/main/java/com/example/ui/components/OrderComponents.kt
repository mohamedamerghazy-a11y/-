package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.model.OrderStatus
import com.example.core.model.PaymentStatus
import com.example.core.util.ArabicFormatter
import com.example.data.model.CartSummary
import com.example.data.model.Coupon
import com.example.data.model.ResolvedCartLine
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OrderStatusBadge(
  status: OrderStatus,
  modifier: Modifier = Modifier,
  isStorePickup: Boolean = false,
) {
  val (bg, fg) = when (status) {
    OrderStatus.NEW_ORDER, OrderStatus.CONFIRMED, OrderStatus.OUT_FOR_DELIVERY -> RoyalBlueSoft to RoyalBlue
    OrderStatus.UNDER_REVIEW, OrderStatus.PREPARING, OrderStatus.READY_FOR_DELIVERY -> GoldenYellowSoft to TextPrimary
    OrderStatus.DELIVERED -> SuccessGreenSoft to SuccessGreen
    OrderStatus.CANCELLED, OrderStatus.RETURNED -> DiscountRedSoft to DiscountRed
  }
  BrandPillBadge(
    text = status.labelFor(isStorePickup),
    containerColor = bg,
    contentColor = fg,
    modifier = modifier,
  )
}

@Composable
fun PaymentStatusBadge(
  status: PaymentStatus,
  modifier: Modifier = Modifier,
) {
  val (bg, fg) = when (status) {
    PaymentStatus.VERIFIED -> SuccessGreenSoft to SuccessGreen
    PaymentStatus.PENDING_REVIEW -> GoldenYellowSoft to TextPrimary
    PaymentStatus.REJECTED -> DiscountRedSoft to DiscountRed
  }
  BrandPillBadge(
    text = status.arabicLabel,
    containerColor = bg,
    contentColor = fg,
    modifier = modifier,
  )
}

@Composable
fun OrderTimelineStepper(
  currentStatus: OrderStatus,
  modifier: Modifier = Modifier,
  isStorePickup: Boolean = false,
) {
  AlEmanCard(modifier = modifier.fillMaxWidth()) {
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
        Text(
          text = "مراحل تنفيذ الطلب",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = RoyalBlue,
        )
        OrderStatusBadge(status = currentStatus, isStorePickup = isStorePickup)
      }

      if (currentStatus.isTerminalException) {
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = DiscountRedSoft,
          border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = if (currentStatus == OrderStatus.RETURNED) {
                Icons.AutoMirrored.Filled.KeyboardReturn
              } else {
                Icons.Filled.Cancel
              },
              contentDescription = currentStatus.labelFor(isStorePickup),
              tint = DiscountRed,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = currentStatus.labelFor(isStorePickup),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = DiscountRed,
              )
              Text(
                text = if (currentStatus == OrderStatus.RETURNED) {
                  "تم تسجيل الطلب كمرتجع، ويمكنك إعادة طلب نفس المنتجات بنقرة واحدة."
                } else {
                  "تم إلغاء هذا الطلب ولن يتم تجهيزه."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
              )
            }
          }
        }
      } else {
        val steps = OrderStatus.activeTimelineStepsFor(isStorePickup)
        steps.forEachIndexed { idx, step ->
          val isCompleted = step.stepIndex < currentStatus.stepIndex ||
            (currentStatus == OrderStatus.DELIVERED && step == OrderStatus.DELIVERED)
          val isCurrent = step == currentStatus && currentStatus != OrderStatus.DELIVERED

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = when {
                  isCompleted -> SuccessGreen
                  isCurrent -> RoyalBlue
                  else -> GeneralBackground
                },
                border = if (!isCompleted && !isCurrent) BorderStroke(1.dp, BorderNeutral) else null,
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (isCompleted) {
                    Icon(
                      imageVector = Icons.Filled.Check,
                      contentDescription = null,
                      tint = PureWhite,
                      modifier = Modifier.size(15.dp),
                    )
                  } else {
                    Text(
                      text = "${idx + 1}",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (isCurrent) PureWhite else TextSecondary,
                    )
                  }
                }
              }
              if (idx < steps.lastIndex) {
                Box(
                  modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(
                      if (step.stepIndex < currentStatus.stepIndex) SuccessGreen else BorderNeutral,
                    ),
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
              modifier = Modifier.padding(top = 2.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = step.labelFor(isStorePickup),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                ),
                color = when {
                  isCompleted -> SuccessGreen
                  isCurrent -> RoyalBlue
                  else -> TextSecondary
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CartLineItemCard(
  line: ResolvedCartLine,
  isWholesaleApproved: Boolean,
  onOpenProduct: () -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onApplyWholesaleMinQty: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val product = line.product
  AlEmanCard(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
      ) {
        ProductThumbnail(
          imageUrl = product.imageUrls.firstOrNull(),
          contentDescription = product.name,
          modifier = Modifier
            .size(82.dp)
            .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
            .background(GeneralBackground)
            .clickable(onClick = onOpenProduct),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = product.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onOpenProduct),
          )

          if (!line.selectedColor.isNullOrBlank() || !line.selectedSize.isNullOrBlank()) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (!line.selectedColor.isNullOrBlank()) {
                BrandPillBadge(
                  text = "اللون: ${line.selectedColor}",
                  containerColor = RoyalBlueSoft,
                  contentColor = RoyalBlue,
                )
              }
              if (!line.selectedSize.isNullOrBlank()) {
                BrandPillBadge(
                  text = "المقاس: ${line.selectedSize}",
                  containerColor = GoldenYellowSoft,
                  contentColor = TextPrimary,
                )
              }
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "${ArabicFormatter.formatCurrency(line.unitPrice)} / ${product.unit}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = if (line.appliedPricingMode == "flash_sale") DiscountRed else RoyalBlue,
            )
            if (line.appliedPricingMode == "flash_sale") {
              DiscountBadge(text = "فلاش سيل ⚡")
            } else if (line.appliedPricingMode == "wholesale") {
              BrandPillBadge(
                text = "سعر جملة",
                containerColor = GoldenYellow,
                contentColor = TextPrimary,
              )
            }
          }

          Text(
            text = "الإجمالي: ${ArabicFormatter.formatCurrency(line.lineTotal)}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
        }

        IconButton(
          onClick = onRemove,
          modifier = Modifier.testTag("cart_remove_${line.effectiveCartItemId}"),
        ) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "حذف من السلة",
            tint = DiscountRed,
          )
        }
      }

      if (line.hasStockIssue) {
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = DiscountRedSoft,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.WarningAmber,
              contentDescription = null,
              tint = DiscountRed,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = if (product.isOutOfStock) {
                "نفد المخزون حاليًا — يرجى الحذف للمتابعة"
              } else {
                "الكمية المطلوبة (${line.quantity}) تتجاوز المتاح (${product.stockQuantity})"
              },
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = DiscountRed,
            )
          }
        }
      }

      if (isWholesaleApproved && line.remainingForWholesale > 0 && product.stockQuantity >= product.wholesaleMinQty) {
        Surface(
          onClick = onApplyWholesaleMinQty,
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = GoldenYellowSoft,
          border = BorderStroke(1.dp, GoldenYellow),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_wholesale_upsell_${product.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Filled.LocalOffer,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "أضف ${line.remainingForWholesale} ${product.unit} لتفعيل سعر الجملة (${ArabicFormatter.formatCurrency(product.wholesalePrice)})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
              )
            }
            Text(
              text = "تطبيق",
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
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "المتاح في المخزون: ${product.stockQuantity}",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )

        QuantityStepperPill(
          quantity = line.quantity,
          canIncrement = line.quantity < product.stockQuantity,
          onDecrement = onDecrement,
          onIncrement = onIncrement,
          tagPrefix = "cart_qty_${product.id}",
        )
      }
    }
  }
}

@Composable
fun QuantityStepperPill(
  quantity: Int,
  canIncrement: Boolean,
  onDecrement: () -> Unit,
  onIncrement: () -> Unit,
  tagPrefix: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
    color = GeneralBackground,
    border = BorderStroke(1.dp, BorderNeutral),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onDecrement,
        modifier = Modifier
          .size(40.dp)
          .testTag("${tagPrefix}_dec"),
      ) {
        Icon(
          imageVector = Icons.Filled.Remove,
          contentDescription = "إنقاص الكمية",
          tint = RoyalBlue,
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = quantity.toString(),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
        modifier = Modifier.padding(horizontal = 10.dp),
      )
      IconButton(
        onClick = onIncrement,
        enabled = canIncrement,
        modifier = Modifier
          .size(40.dp)
          .testTag("${tagPrefix}_inc"),
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = "زيادة الكمية",
          tint = if (canIncrement) RoyalBlue else TextSecondary.copy(alpha = 0.45f),
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
fun CouponInputCard(
  couponInput: String,
  appliedCoupon: Coupon?,
  isValidating: Boolean,
  couponError: String?,
  discountAmount: Double,
  onInputChange: (String) -> Unit,
  onApplyClick: () -> Unit,
  onRemoveClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlEmanCard(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.ConfirmationNumber,
          contentDescription = null,
          tint = RoyalBlue,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "كود الخصم (الكوبون)",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
      }

      if (appliedCoupon != null) {
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = SuccessGreenSoft,
          border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(18.dp),
              )
              Column {
                Text(
                  text = "تم تطبيق الكود: ${appliedCoupon.code}",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = SuccessGreen,
                )
                Text(
                  text = "وفرّت ${ArabicFormatter.formatCurrency(discountAmount)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextPrimary,
                )
              }
            }
            OutlinedButton(
              onClick = onRemoveClick,
              shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
              border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.5f)),
              modifier = Modifier.testTag("remove_coupon_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "إزالة الكوبون",
                tint = DiscountRed,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("إزالة", color = DiscountRed)
            }
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = couponInput,
            onValueChange = onInputChange,
            placeholder = { Text("مثال: ALEMAN10") },
            singleLine = true,
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              focusedBorderColor = RoyalBlue,
              unfocusedBorderColor = BorderNeutral,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("coupon_code_input"),
          )
          Button(
            onClick = onApplyClick,
            enabled = !isValidating && couponInput.trim().length >= 2,
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            colors = ButtonDefaults.buttonColors(
              containerColor = RoyalBlue,
              contentColor = PureWhite,
            ),
            modifier = Modifier
              .height(52.dp)
              .testTag("apply_coupon_button"),
          ) {
            if (isValidating) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = PureWhite,
                strokeWidth = 2.dp,
              )
            } else {
              Text("تطبيق", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      if (!couponError.isNullOrBlank()) {
        Text(
          text = couponError,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
          color = DiscountRed,
        )
      }
    }
  }
}

@Composable
fun FreeShippingProgressBanner(
  summary: CartSummary,
  isStorePickup: Boolean = false,
  modifier: Modifier = Modifier,
) {
  if (!summary.freeShippingEnabled || summary.freeShippingThreshold <= 0.0 || isStorePickup) return

  val qualified = summary.qualifiesForFreeShipping
  Surface(
    shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
    color = if (qualified) SuccessGreenSoft else PrimaryYellowSoft,
    border = BorderStroke(1.dp, if (qualified) SuccessGreen else PrimaryYellow),
    modifier = modifier
      .fillMaxWidth()
      .testTag("free_shipping_progress_banner"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = if (qualified) Icons.Filled.CheckCircle else Icons.Filled.LocalShipping,
          contentDescription = null,
          tint = if (qualified) SuccessGreen else CharcoalBlack,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = if (qualified) {
            "مبروك! طلبك مؤهل الآن للشحن المجاني 🎉"
          } else {
            "أضف منتجات بقيمة ${ArabicFormatter.formatCurrency(summary.remainingForFreeShipping)} إضافية للحصول على شحن مجاني"
          },
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
          color = if (qualified) SuccessGreen else CharcoalBlack,
        )
      }
      LinearProgressIndicator(
        progress = { summary.freeShippingProgress },
        color = if (qualified) SuccessGreen else CharcoalBlack,
        trackColor = PureWhite,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(50)),
      )
    }
  }
}

@Composable
fun PriceSummaryRows(
  subtotal: Double,
  discountAmount: Double,
  deliveryFee: Double?,
  totalAmount: Double,
  isStorePickup: Boolean = false,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SummaryKeyValueRow(
      label = "المجموع الفرعي للمنتجات",
      value = ArabicFormatter.formatCurrency(subtotal),
      valueColor = TextPrimary,
    )
    if (discountAmount > 0.0) {
      SummaryKeyValueRow(
        label = "خصم الكوبون",
        value = "- ${ArabicFormatter.formatCurrency(discountAmount)}",
        valueColor = SuccessGreen,
      )
    }
    if (isStorePickup) {
      SummaryKeyValueRow(
        label = "الاستلام من المتجر",
        value = "مجانًا",
        valueColor = SuccessGreen,
      )
    } else if (deliveryFee != null) {
      SummaryKeyValueRow(
        label = "رسوم التوصيل",
        value = if (deliveryFee == 0.0) "مجاني" else ArabicFormatter.formatCurrency(deliveryFee),
        valueColor = if (deliveryFee == 0.0) SuccessGreen else TextPrimary,
      )
    }
    HorizontalDivider(color = BorderNeutral)
    SummaryKeyValueRow(
      label = "الإجمالي المستحق",
      value = ArabicFormatter.formatCurrency(totalAmount),
      valueColor = RoyalBlue,
      isBold = true,
    )
  }
}

@Composable
private fun SummaryKeyValueRow(
  label: String,
  value: String,
  valueColor: Color,
  isBold: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = if (isBold) {
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      } else {
        MaterialTheme.typography.bodyMedium
      },
      color = if (isBold) TextPrimary else TextSecondary,
    )
    Text(
      text = value,
      style = if (isBold) {
        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      } else {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
      },
      color = valueColor,
    )
  }
}
