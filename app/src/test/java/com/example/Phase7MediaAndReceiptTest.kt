package com.example

import com.example.data.media.CloudinaryUploadService
import com.example.data.media.ImageProcessor
import com.example.data.media.NormalizedCropRect
import com.example.data.model.Order
import com.example.ui.viewmodel.AdminPaymentsUiState
import com.example.ui.viewmodel.ReceiptCleanupEvaluator
import com.example.ui.viewmodel.ReceiptCleanupScope
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class Phase7MediaAndReceiptTest {

  private val fixedNowMillis = 1_750_000_000_000L
  private val millisPerDay = 86_400_000L

  private fun orderWithAge(
    id: String,
    daysOld: Long,
    receiptUrl: String?,
    paymentMethod: String = "instapay",
    paymentStatus: String = "pending_review",
  ): Order {
    val created = Timestamp(Date(fixedNowMillis - (daysOld * millisPerDay)))
    return Order(
      id = id,
      orderNumber = "EM-$id",
      paymentMethod = paymentMethod,
      paymentStatus = paymentStatus,
      receiptImageUrl = receiptUrl,
      createdAt = created,
      updatedAt = created,
    )
  }

  @Test
  fun cropRect_normalizationAndAspectComputation() {
    val clamped = NormalizedCropRect(-0.5f, 0.99f, 1.4f, 0.2f).normalized()
    assertTrue(clamped.left >= 0f && clamped.left < clamped.right)
    assertTrue(clamped.top < clamped.bottom && clamped.bottom <= 1f)

    // Wide 1600x800 cropped to 1:1 square should leave centered horizontal 0.25..0.75
    val square = ImageProcessor.computeCenteredAspectCrop(1600, 800, 1f)
    assertEquals(0.25f, square.left, 0.001f)
    assertEquals(0.75f, square.right, 0.001f)
    assertEquals(0f, square.top, 0.001f)
    assertEquals(1f, square.bottom, 0.001f)
  }

  @Test
  fun cloudinaryConfigurationValidation_rejectsPlaceholders() {
    assertFalse(CloudinaryUploadService("your_cloudinary_cloud_name", "your_unsigned_upload_preset").isConfigured())
    assertFalse(CloudinaryUploadService("", "aleman_unsigned").isConfigured())
    assertFalse(CloudinaryUploadService("aleman-cloud", "").isConfigured())
    assertTrue(CloudinaryUploadService("aleman-cloud", "aleman_unsigned").isConfigured())
  }

  @Test
  fun receiptCleanupEvaluator_filtersByTimePeriodsAndStatuses() {
    val orders = listOf(
      orderWithAge("o_2d_pending", daysOld = 2, receiptUrl = "https://res.cloudinary.com/demo/1.webp", paymentStatus = "pending_review"),
      orderWithAge("o_10d_verified", daysOld = 10, receiptUrl = "https://res.cloudinary.com/demo/2.webp", paymentStatus = "verified"),
      orderWithAge("o_35d_rejected", daysOld = 35, receiptUrl = "https://res.cloudinary.com/demo/3.webp", paymentStatus = "rejected"),
      orderWithAge("o_95d_verified", daysOld = 95, receiptUrl = "https://res.cloudinary.com/demo/4.webp", paymentStatus = "verified"),
      orderWithAge("o_120d_no_receipt", daysOld = 120, receiptUrl = null, paymentStatus = "verified"),
    )

    val older7 = ReceiptCleanupEvaluator.filterMatchingOrders(
      orders = orders,
      scope = ReceiptCleanupScope.OLDER_THAN_7_DAYS,
      nowMillis = fixedNowMillis,
    )
    assertEquals(listOf("o_10d_verified", "o_35d_rejected", "o_95d_verified"), older7.map { it.id })

    val older30 = ReceiptCleanupEvaluator.filterMatchingOrders(
      orders = orders,
      scope = ReceiptCleanupScope.OLDER_THAN_30_DAYS,
      nowMillis = fixedNowMillis,
    )
    assertEquals(listOf("o_35d_rejected", "o_95d_verified"), older30.map { it.id })

    val older90 = ReceiptCleanupEvaluator.filterMatchingOrders(
      orders = orders,
      scope = ReceiptCleanupScope.OLDER_THAN_90_DAYS,
      nowMillis = fixedNowMillis,
    )
    assertEquals(listOf("o_95d_verified"), older90.map { it.id })

    val custom15 = ReceiptCleanupEvaluator.filterMatchingOrders(
      orders = orders,
      scope = ReceiptCleanupScope.CUSTOM_DAYS,
      customDays = 15,
      nowMillis = fixedNowMillis,
    )
    assertEquals(listOf("o_35d_rejected", "o_95d_verified"), custom15.map { it.id })

    val reviewedOnly = ReceiptCleanupEvaluator.filterMatchingOrders(
      orders = orders,
      scope = ReceiptCleanupScope.REVIEWED_ONLY,
      nowMillis = fixedNowMillis,
    )
    assertEquals(listOf("o_10d_verified", "o_35d_rejected", "o_95d_verified"), reviewedOnly.map { it.id })

    val allReceipts = ReceiptCleanupEvaluator.filterMatchingOrders(
      orders = orders,
      scope = ReceiptCleanupScope.ALL_RECEIPTS,
      nowMillis = fixedNowMillis,
    )
    assertEquals(4, allReceipts.size)
  }

  @Test
  fun adminPaymentsUiState_filtersHasReceiptCorrectly() {
    val orders = listOf(
      orderWithAge("1", 1, receiptUrl = "https://res.cloudinary.com/demo/r1.webp", paymentMethod = "instapay"),
      orderWithAge("2", 1, receiptUrl = null, paymentMethod = "vodafone_cash"),
      orderWithAge("3", 1, receiptUrl = null, paymentMethod = "cod"),
    )
    val state = AdminPaymentsUiState(
      isLoading = false,
      orders = orders,
      selectedPaymentStatusFilter = "has_receipt",
    )
    assertEquals(1, state.ordersWithReceiptsCount)
    assertEquals(listOf("1"), state.transferOrders.map { it.id })
  }
}
