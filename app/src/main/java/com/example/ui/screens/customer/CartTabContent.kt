package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Product
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanPrimaryButton
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.CartLineItemCard
import com.example.ui.components.CouponInputCard
import com.example.ui.components.EmptyCartCleaningHero
import com.example.ui.components.FreeShippingProgressBanner
import com.example.ui.components.PriceSummaryRows
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CartAndOrdersUiState

@Composable
fun CartTabContent(
  cartUiState: CartAndOrdersUiState,
  isSignedIn: Boolean,
  isWholesaleApproved: Boolean,
  onShopNow: () -> Unit,
  onOpenProduct: (String) -> Unit,
  onUpdateQuantity: (Product, Int, String) -> Unit,
  onRemoveItem: (String) -> Unit,
  onCouponInputChange: (String) -> Unit,
  onApplyCoupon: () -> Unit,
  onRemoveCoupon: () -> Unit,
  onRequireSignIn: () -> Unit,
  onProceedToCheckout: () -> Unit,
) {
  val summary = cartUiState.cartSummary

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(AlEmanDimens.SpaceLg),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SectionHeaderRow(
        title = stringResource(id = R.string.nav_cart),
        modifier = Modifier.weight(1f),
      )
      if (summary.totalItemsCount > 0) {
        BrandPillBadge(
          text = "${summary.totalItemsCount} قطعة",
          containerColor = RoyalBlueSoft,
          contentColor = RoyalBlue,
        )
      }
    }

    if (isWholesaleApproved) {
      Surface(
        shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
        color = GoldenYellowSoft,
        border = BorderStroke(1.dp, GoldenYellow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "حساب جملة معتمد: تُحتسب أسعار الجملة تلقائيًا عند بلوغ الحد الأدنى لكل منتج.",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
          )
        }
      }
    }

    if (summary.lines.isEmpty()) {
      EmptyCartCleaningHero(onShopNow = onShopNow)
      return
    }

    summary.lines.forEach { line ->
      CartLineItemCard(
        line = line,
        isWholesaleApproved = isWholesaleApproved,
        onOpenProduct = { onOpenProduct(line.product.id) },
        onIncrement = { onUpdateQuantity(line.product, line.quantity + 1, line.effectiveCartItemId) },
        onDecrement = { onUpdateQuantity(line.product, line.quantity - 1, line.effectiveCartItemId) },
        onApplyWholesaleMinQty = {
          onUpdateQuantity(line.product, line.product.wholesaleMinQty, line.effectiveCartItemId)
        },
        onRemove = { onRemoveItem(line.effectiveCartItemId) },
      )
    }

    if (isSignedIn) {
      CouponInputCard(
        couponInput = cartUiState.couponCodeInput,
        appliedCoupon = cartUiState.appliedCoupon,
        isValidating = cartUiState.isCouponValidating,
        couponError = cartUiState.couponError,
        discountAmount = summary.discountAmount,
        onInputChange = onCouponInputChange,
        onApplyClick = onApplyCoupon,
        onRemoveClick = onRemoveCoupon,
      )
    } else {
      Surface(
        shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
        color = RoyalBlueSoft,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.LockOpen,
            contentDescription = null,
            tint = RoyalBlue,
          )
          Text(
            text = "أضف ما تشاء كزائر؛ وعند تسجيل الدخول ستُحفظ سلتك تلقائيًا ويمكنك تطبيق أكواد الخصم وإتمام الطلب.",
            style = MaterialTheme.typography.bodySmall,
            color = RoyalBlue,
          )
        }
      }
    }

    AlEmanCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "ملخص عربة التسوق",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = RoyalBlue,
        )

        FreeShippingProgressBanner(
          summary = summary,
          isStorePickup = false,
        )

        PriceSummaryRows(
          subtotal = summary.subtotal,
          discountAmount = summary.discountAmount,
          deliveryFee = if (cartUiState.deliveryZones.isNotEmpty() && isSignedIn) summary.deliveryFee else null,
          totalAmount = if (cartUiState.deliveryZones.isNotEmpty() && isSignedIn) {
            summary.totalAmount
          } else {
            (summary.subtotal - summary.discountAmount).coerceAtLeast(0.0)
          },
        )

        if (!isSignedIn || cartUiState.deliveryZones.isEmpty()) {
          Text(
            text = "تُضاف رسوم التوصيل حسب المنطقة المختارة في خطوة إتمام الطلب.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        if (!isSignedIn) {
          AlEmanPrimaryButton(
            text = "تسجيل الدخول وإتمام الطلب",
            onClick = onRequireSignIn,
            icon = Icons.AutoMirrored.Filled.Login,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("cart_guest_sign_in_checkout_button"),
          )
        } else {
          AlEmanPrimaryButton(
            text = if (summary.hasStockConflicts) {
              "راجع الكميات المتوفرة أولًا"
            } else {
              "متابعة لإتمام الطلب"
            },
            onClick = onProceedToCheckout,
            enabled = !summary.hasStockConflicts,
            icon = Icons.Filled.ShoppingBag,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("cart_proceed_to_checkout_button"),
          )
        }
      }
    }
  }
}
