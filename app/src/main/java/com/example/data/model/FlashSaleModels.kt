package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class FlashSaleStatus(val arabicLabel: String) {
  UPCOMING("قريبًا"),
  ACTIVE("سارٍ الآن"),
  ENDED("تم انتهاء العرض"),
}

@Keep
@IgnoreExtraProperties
data class FlashSaleProductItem(
  val productId: String = "",
  val flashRetailPrice: Double = 0.0,
  val flashWholesalePrice: Double? = null,
  val maxQtyPerOrder: Int? = null,
)

@Keep
@IgnoreExtraProperties
data class FlashSale(
  @DocumentId val id: String = "",
  val title: String = "",
  val badgeText: String = "",
  val startsAt: Timestamp? = null,
  val endsAt: Timestamp? = null,
  @get:PropertyName("isActive") @PropertyName("isActive")
  val isActive: Boolean = true,
  val items: List<FlashSaleProductItem> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @Exclude
  fun statusAt(nowMillis: Long = System.currentTimeMillis()): FlashSaleStatus {
    val startMs = startsAt?.toDate()?.time ?: 0L
    val endMs = endsAt?.toDate()?.time ?: Long.MAX_VALUE
    return when {
      nowMillis < startMs -> FlashSaleStatus.UPCOMING
      nowMillis >= endMs -> FlashSaleStatus.ENDED
      else -> FlashSaleStatus.ACTIVE
    }
  }

  @Exclude
  fun remainingMillisAt(nowMillis: Long = System.currentTimeMillis()): Long {
    val startMs = startsAt?.toDate()?.time ?: 0L
    val endMs = endsAt?.toDate()?.time ?: 0L
    return when (statusAt(nowMillis)) {
      FlashSaleStatus.UPCOMING -> (startMs - nowMillis).coerceAtLeast(0L)
      FlashSaleStatus.ACTIVE -> (endMs - nowMillis).coerceAtLeast(0L)
      FlashSaleStatus.ENDED -> 0L
    }
  }

  @Exclude
  fun itemFor(productId: String): FlashSaleProductItem? {
    return items.firstOrNull { it.productId == productId }
  }

  @Exclude
  fun flashUnitPrice(productId: String, isWholesaleActive: Boolean): Double? {
    val item = itemFor(productId) ?: return null
    return if (isWholesaleActive && item.flashWholesalePrice != null && item.flashWholesalePrice > 0.0) {
      item.flashWholesalePrice
    } else if (item.flashRetailPrice > 0.0) {
      item.flashRetailPrice
    } else {
      null
    }
  }
}
