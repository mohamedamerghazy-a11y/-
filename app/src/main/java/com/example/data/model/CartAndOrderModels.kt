package com.example.data.model

import com.example.core.model.CouponDiscountType
import com.example.core.model.FulfillmentMethod
import com.example.core.model.OrderStatus
import com.example.core.model.PaymentMethod
import com.example.core.model.PaymentStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.min
import kotlin.math.roundToLong

fun buildCartItemId(
  productId: String,
  selectedColor: String? = null,
  selectedSize: String? = null,
): String {
  val cleanId = productId.trim()
  val cleanColor = selectedColor?.trim()?.takeIf { it.isNotEmpty() }
  val cleanSize = selectedSize?.trim()?.takeIf { it.isNotEmpty() }
  if (cleanColor == null && cleanSize == null) return cleanId
  val raw = "${cleanColor.orEmpty()}|${cleanSize.orEmpty()}"
  var hash = 0x811c9dc5.toInt()
  raw.forEach { ch ->
    hash = (hash xor ch.code) * 16777619
  }
  return "${cleanId}__v_${(hash.toLong() and 0xffffffffL).toString(36)}"
}

fun normalizeEgyptianPhone(raw: String): String {
  val normalizedDigits = buildString {
    raw.forEach { ch ->
      when (ch) {
        in '\u0660'..'\u0669' -> append(('0'.code + (ch.code - '\u0660'.code)).toChar())
        in '\u06F0'..'\u06F9' -> append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
        in '0'..'9' -> append(ch)
      }
    }
  }
  return when {
    normalizedDigits.startsWith("00201") && normalizedDigits.length >= 14 -> "0" + normalizedDigits.drop(4)
    normalizedDigits.startsWith("201") && normalizedDigits.length >= 12 -> "0" + normalizedDigits.drop(2)
    normalizedDigits.length == 10 && normalizedDigits.startsWith("1") -> "0$normalizedDigits"
    else -> normalizedDigits
  }
}

fun generateDeliveryOtp(): String = (1000..9999).random().toString()

fun normalizeOtpDigits(raw: String): String = buildString {
  raw.forEach { ch ->
    when (ch) {
      in '\u0660'..'\u0669' -> append(('0'.code + (ch.code - '\u0660'.code)).toChar())
      in '\u06F0'..'\u06F9' -> append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
      in '0'..'9' -> append(ch)
    }
  }
}

data class FreeShippingEvaluation(
  val deliveryFee: Double = 0.0,
  val freeShippingEnabled: Boolean = false,
  val freeShippingThreshold: Double = 0.0,
  val remainingForFreeShipping: Double = 0.0,
  val freeShippingProgress: Float = 0f,
  val qualifiesForFreeShipping: Boolean = false,
)

fun computeFreeShipping(
  netMerchandise: Double,
  isStorePickup: Boolean,
  hasItems: Boolean,
  baseZoneFee: Double,
  enabled: Boolean,
  threshold: Double,
): FreeShippingEvaluation {
  val cleanNet = netMerchandise.coerceAtLeast(0.0)
  val cleanThreshold = threshold.coerceAtLeast(0.0)
  val isFeatureActive = enabled && cleanThreshold > 0.0
  val qualifies = !isStorePickup && hasItems && isFeatureActive && cleanNet >= cleanThreshold
  val remaining = if (isFeatureActive && cleanNet < cleanThreshold) {
    ((cleanThreshold - cleanNet) * 100.0).roundToLong() / 100.0
  } else {
    0.0
  }
  val progress = if (isFeatureActive && cleanThreshold > 0.0) {
    (cleanNet / cleanThreshold).toFloat().coerceIn(0f, 1f)
  } else {
    0f
  }
  val finalFee = if (!hasItems || isStorePickup || qualifies) {
    0.0
  } else {
    (baseZoneFee.coerceAtLeast(0.0) * 100.0).roundToLong() / 100.0
  }
  return FreeShippingEvaluation(
    deliveryFee = finalFee,
    freeShippingEnabled = isFeatureActive,
    freeShippingThreshold = cleanThreshold,
    remainingForFreeShipping = remaining,
    freeShippingProgress = progress,
    qualifiesForFreeShipping = qualifies,
  )
}

@IgnoreExtraProperties
data class CartItemRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productId: String = "",
  val quantity: Int = 1,
  val flashSaleId: String? = null,
  val selectedColor: String? = null,
  val selectedSize: String? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Coupon(
  @DocumentId val id: String = "",
  val code: String = "",
  val discountType: String = "fixed",
  val discountValue: Double = 0.0,
  val minOrderAmount: Double = 0.0,
  val maxDiscountAmount: Double? = null,
  val usageLimit: Int? = null,
  val usedCount: Int = 0,
  val expiresAt: Timestamp? = null,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val discountTypeEnum: CouponDiscountType
    get() = CouponDiscountType.fromCode(discountType)

  @Exclude
  fun calculateDiscount(subtotal: Double): Double {
    if (subtotal <= 0.0 || subtotal < minOrderAmount) return 0.0
    val raw = when (discountTypeEnum) {
      CouponDiscountType.PERCENT -> (subtotal * (discountValue / 100.0))
      CouponDiscountType.FIXED -> discountValue
    }
    val capped = if (maxDiscountAmount != null && maxDiscountAmount > 0.0) {
      min(raw, maxDiscountAmount)
    } else {
      raw
    }
    return ((min(capped, subtotal) * 100.0).roundToLong()) / 100.0
  }
}

@IgnoreExtraProperties
data class DeliveryZone(
  @DocumentId val id: String = "",
  val name: String = "",
  val governorate: String = "",
  val fee: Double = 0.0,
  val estimatedDuration: String = "خلال ٢٤ - ٤٨ ساعة",
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class StorePaymentSettings(
  @DocumentId val id: String = "payments",
  val vodafoneCashNumber: String = "",
  val instaPayAddress: String = "",
  val paymentInstructions: String = "",
  val codEnabled: Boolean = true,
  val instaPayEnabled: Boolean = true,
  val vodafoneCashEnabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class OrderItemSnapshot(
  val productId: String = "",
  val productName: String = "",
  val sku: String = "",
  val imageUrl: String? = null,
  val unit: String = "قطعة",
  val pricingMode: String = "retail",
  val flashSaleId: String? = null,
  val unitPrice: Double = 0.0,
  val quantity: Int = 1,
  val lineTotal: Double = 0.0,
  val selectedColor: String? = null,
  val selectedSize: String? = null,
)

@IgnoreExtraProperties
data class Order(
  @DocumentId val id: String = "",
  val userId: String = "",
  val orderNumber: String = "",
  val customerName: String = "",
  val customerEmail: String = "",
  val accountTypeAtOrder: String = "retail",
  val items: List<OrderItemSnapshot> = emptyList(),
  val subtotal: Double = 0.0,
  val discountAmount: Double = 0.0,
  val couponCode: String? = null,
  val deliveryZoneId: String? = null,
  val deliveryZoneName: String? = null,
  val deliveryFee: Double = 0.0,
  val totalAmount: Double = 0.0,
  val paymentMethod: String = "cod",
  val paymentStatus: String = "pending_review",
  val transferReference: String? = null,
  val senderPhone: String? = null,
  val receiptImageUrl: String? = null,
  val orderStatus: String = "new_order",
  val deliveryLabel: String = "",
  val fulfillmentType: String = FulfillmentMethod.DELIVERY.code,
  val deliveryOtp: String? = null,
  val deliveryOtpVerified: Boolean = false,
  val recipientName: String = "",
  val phone: String = "",
  val governorate: String = "",
  val city: String = "",
  val streetAddress: String = "",
  val landmark: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val assignedDriverId: String? = null,
  val assignedDriverName: String? = null,
  val assignedDriverPhone: String? = null,
  val assignedAt: Timestamp? = null,
  val notes: String? = null,
  val statusNote: String? = null,
  val verifiedByDeviceName: String? = null,
  val verifiedByEmail: String? = null,
  val verifiedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val statusEnum: OrderStatus
    get() = OrderStatus.fromCode(orderStatus)

  @get:Exclude
  val fulfillmentMethodEnum: FulfillmentMethod
    get() = FulfillmentMethod.fromCode(fulfillmentType)

  @get:Exclude
  val isStorePickup: Boolean
    get() = fulfillmentMethodEnum == FulfillmentMethod.STORE_PICKUP

  @get:Exclude
  val paymentMethodEnum: PaymentMethod
    get() = PaymentMethod.fromCode(paymentMethod)

  @get:Exclude
  val paymentStatusEnum: PaymentStatus
    get() = PaymentStatus.fromCode(paymentStatus)

  @get:Exclude
  val totalUnitsCount: Int
    get() = items.sumOf { it.quantity }

  @get:Exclude
  val canCustomerCancel: Boolean
    get() = statusEnum == OrderStatus.NEW_ORDER || statusEnum == OrderStatus.UNDER_REVIEW

  @get:Exclude
  val hasCoordinates: Boolean
    get() = latitude != null && longitude != null

  @get:Exclude
  val hasPendingDeliveryOtp: Boolean
    get() = !isStorePickup &&
      !deliveryOtp.isNullOrBlank() &&
      !deliveryOtpVerified &&
      statusEnum != OrderStatus.DELIVERED &&
      !statusEnum.isTerminalException

  @get:Exclude
  val displaySenderPhone: String?
    get() = senderPhone?.trim()?.takeIf { it.isNotEmpty() }
      ?: transferReference?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * سطر سلة محسوب بلحظته من بيانات المنتج وحالة حساب العميل.
 */
data class ResolvedCartLine(
  val cartItemId: String = "",
  val product: Product,
  val quantity: Int,
  val unitPrice: Double,
  val appliedPricingMode: String,
  val lineTotal: Double,
  val remainingForWholesale: Int,
  val flashSaleId: String? = null,
  val flashSaleTitle: String? = null,
  val selectedColor: String? = null,
  val selectedSize: String? = null,
) {
  val hasStockIssue: Boolean
    get() = product.isOutOfStock || quantity > product.stockQuantity

  val effectiveCartItemId: String
    get() = cartItemId.ifBlank { buildCartItemId(product.id, selectedColor, selectedSize) }
}

data class CartSummary(
  val lines: List<ResolvedCartLine> = emptyList(),
  val subtotal: Double = 0.0,
  val discountAmount: Double = 0.0,
  val deliveryFee: Double = 0.0,
  val totalAmount: Double = 0.0,
  val totalItemsCount: Int = 0,
  val hasStockConflicts: Boolean = false,
  val freeShippingEnabled: Boolean = false,
  val freeShippingThreshold: Double = 0.0,
  val remainingForFreeShipping: Double = 0.0,
  val freeShippingProgress: Float = 0f,
  val qualifiesForFreeShipping: Boolean = false,
)
