package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

@IgnoreExtraProperties
data class Category(
  @DocumentId val id: String = "",
  val name: String = "",
  val description: String? = null,
  val parentId: String? = null,
  val imageUrl: String? = null,
  val displayOrder: Int = 0,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val isMainCategory: Boolean
    get() = parentId.isNullOrBlank()
}

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val name: String = "",
  val description: String = "",
  val categoryId: String = "",
  val subCategoryId: String? = null,
  val sku: String = "",
  val barcode: String? = null,
  val qrCode: String? = null,
  val imageUrls: List<String> = emptyList(),
  val retailPrice: Double = 0.0,
  val retailDiscountPrice: Double? = null,
  val wholesalePrice: Double = 0.0,
  val wholesaleMinQty: Int = 1,
  val unit: String = "قطعة",
  val stockQuantity: Int = 0,
  val lowStockThreshold: Int = 5,
  val isFeatured: Boolean = false,
  val isOffer: Boolean = false,
  val isActive: Boolean = true,
  val companyId: String? = null,
  val brandShowcaseId: String? = null,
  val brandCategoryId: String? = null,
  val exclusiveToBrandShowcase: Boolean = false,
  val availableColors: List<String> = emptyList(),
  val availableSizes: List<String> = emptyList(),
  val salesCount: Long = 0L,
  val averageRating: Double = 0.0,
  val reviewCount: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val hasDiscount: Boolean
    get() = retailDiscountPrice != null && retailDiscountPrice > 0.0 && retailDiscountPrice < retailPrice

  @get:Exclude
  val effectiveRetailPrice: Double
    get() = if (hasDiscount) retailDiscountPrice!! else retailPrice

  @get:Exclude
  val discountPercent: Int
    get() = if (hasDiscount && retailPrice > 0.0) {
      (((retailPrice - retailDiscountPrice!!) / retailPrice) * 100.0).roundToInt().coerceIn(1, 99)
    } else {
      0
    }

  @get:Exclude
  val isOutOfStock: Boolean
    get() = stockQuantity <= 0

  @get:Exclude
  val isLowStock: Boolean
    get() = stockQuantity in 1..lowStockThreshold

  @Exclude
  fun effectiveUnitPrice(isWholesaleApproved: Boolean, quantity: Int = wholesaleMinQty): Double {
    return if (isWholesaleApproved && wholesalePrice > 0.0 && quantity >= wholesaleMinQty) {
      wholesalePrice
    } else {
      effectiveRetailPrice
    }
  }
}
