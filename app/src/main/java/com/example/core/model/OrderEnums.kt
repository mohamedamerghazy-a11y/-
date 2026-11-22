package com.example.core.model

enum class FulfillmentMethod(
  val code: String,
  val arabicLabel: String,
  val subtitle: String,
) {
  DELIVERY(
    code = "delivery",
    arabicLabel = "توصيل إلى العنوان",
    subtitle = "يصل الطلب إلى عنوانك عبر مندوب التوصيل.",
  ),
  STORE_PICKUP(
    code = "pickup",
    arabicLabel = "الاستلام من المتجر",
    subtitle = "استلم طلبك مباشرة من مقر المتجر مجانًا بدون رسوم توصيل.",
  );

  companion object {
    fun fromCode(code: String?): FulfillmentMethod =
      entries.firstOrNull { it.code == code } ?: DELIVERY
  }
}

/**
 * حالات الطلب الـ 9 المعتمدة في متجر الإيمان للتجارة.
 */
enum class OrderStatus(
  val code: String,
  val arabicLabel: String,
  val stepIndex: Int,
) {
  NEW_ORDER("new_order", "طلب جديد", 0),
  UNDER_REVIEW("under_review", "قيد المراجعة", 1),
  CONFIRMED("confirmed", "تم التأكيد", 2),
  PREPARING("preparing", "جاري التجهيز", 3),
  READY_FOR_DELIVERY("ready_for_delivery", "جاهز للتوصيل", 4),
  OUT_FOR_DELIVERY("out_for_delivery", "خرج مع المندوب", 5),
  DELIVERED("delivered", "تم التوصيل", 6),
  CANCELLED("cancelled", "ملغي", -1),
  RETURNED("returned", "مرتجع", -1);

  val isTerminalException: Boolean
    get() = this == CANCELLED || this == RETURNED

  val isActiveProgression: Boolean
    get() = !isTerminalException && this != DELIVERED

  fun labelFor(isStorePickup: Boolean): String {
    if (!isStorePickup) return arabicLabel
    return when (this) {
      READY_FOR_DELIVERY -> "جاهز للاستلام من المتجر"
      DELIVERED -> "تم الاستلام من المتجر"
      else -> arabicLabel
    }
  }

  companion object {
    val activeTimelineSteps: List<OrderStatus> = listOf(
      NEW_ORDER,
      UNDER_REVIEW,
      CONFIRMED,
      PREPARING,
      READY_FOR_DELIVERY,
      OUT_FOR_DELIVERY,
      DELIVERED,
    )

    val pickupTimelineSteps: List<OrderStatus> = listOf(
      NEW_ORDER,
      UNDER_REVIEW,
      CONFIRMED,
      PREPARING,
      READY_FOR_DELIVERY,
      DELIVERED,
    )

    fun activeTimelineStepsFor(isStorePickup: Boolean): List<OrderStatus> =
      if (isStorePickup) pickupTimelineSteps else activeTimelineSteps

    fun fromCode(code: String?): OrderStatus =
      entries.firstOrNull { it.code == code } ?: NEW_ORDER
  }
}

enum class PaymentMethod(
  val code: String,
  val arabicLabel: String,
  val subtitle: String,
  val requiresTransferReference: Boolean,
) {
  COD(
    code = "cod",
    arabicLabel = "الدفع عند الاستلام",
    subtitle = "ادفع نقدًا للمندوب عند استلام طلبك وفحصه.",
    requiresTransferReference = false,
  ),
  INSTAPAY(
    code = "instapay",
    arabicLabel = "إنستاباي (InstaPay)",
    subtitle = "تحويل فوري على حساب المتجر مع إدخال رقم المرجع.",
    requiresTransferReference = true,
  ),
  VODAFONE_CASH(
    code = "vodafone_cash",
    arabicLabel = "فودافون كاش",
    subtitle = "تحويل على محفظة المتجر المعتمدة وإرفاق المرجع.",
    requiresTransferReference = true,
  );

  companion object {
    fun fromCode(code: String?): PaymentMethod =
      entries.firstOrNull { it.code == code } ?: COD
  }
}

enum class PaymentStatus(
  val code: String,
  val arabicLabel: String,
) {
  PENDING_REVIEW("pending_review", "بانتظار مراجعة الدفع"),
  VERIFIED("verified", "تم تأكيد الدفع"),
  REJECTED("rejected", "مرفوض — يرجى المراجعة");

  companion object {
    fun fromCode(code: String?): PaymentStatus =
      entries.firstOrNull { it.code == code } ?: PENDING_REVIEW
  }
}

enum class CouponDiscountType(val code: String, val arabicLabel: String) {
  PERCENT("percent", "نسبة مئوية"),
  FIXED("fixed", "مبلغ ثابت");

  companion object {
    fun fromCode(code: String?): CouponDiscountType =
      entries.firstOrNull { it.code == code } ?: FIXED
  }
}
