package com.example

import com.example.core.model.FulfillmentMethod
import com.example.core.model.OrderStatus
import com.example.data.model.Order
import com.example.data.model.buildSearchMissDocId
import com.example.data.model.computeFreeShipping
import com.example.data.model.generateDeliveryOtp
import com.example.data.model.normalizeOtpDigits
import com.example.data.model.normalizeSearchQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10EngagementFeaturesTest {

  @Test
  fun computeFreeShipping_whenDisabled_keepsBaseZoneFee() {
    val res = computeFreeShipping(
      netMerchandise = 900.0,
      isStorePickup = false,
      hasItems = true,
      baseZoneFee = 45.0,
      enabled = false,
      threshold = 500.0,
    )
    assertFalse(res.freeShippingEnabled)
    assertFalse(res.qualifiesForFreeShipping)
    assertEquals(45.0, res.deliveryFee, 0.001)
  }

  @Test
  fun computeFreeShipping_whenEnabledBelowThreshold_computesRemainingAndProgress() {
    val res = computeFreeShipping(
      netMerchandise = 350.0,
      isStorePickup = false,
      hasItems = true,
      baseZoneFee = 50.0,
      enabled = true,
      threshold = 500.0,
    )
    assertTrue(res.freeShippingEnabled)
    assertFalse(res.qualifiesForFreeShipping)
    assertEquals(50.0, res.deliveryFee, 0.001)
    assertEquals(150.0, res.remainingForFreeShipping, 0.001)
    assertEquals(0.7f, res.freeShippingProgress, 0.001f)
  }

  @Test
  fun computeFreeShipping_whenEnabledMeetsThreshold_setsDeliveryFeeZero() {
    val res = computeFreeShipping(
      netMerchandise = 500.0,
      isStorePickup = false,
      hasItems = true,
      baseZoneFee = 50.0,
      enabled = true,
      threshold = 500.0,
    )
    assertTrue(res.freeShippingEnabled)
    assertTrue(res.qualifiesForFreeShipping)
    assertEquals(0.0, res.deliveryFee, 0.001)
    assertEquals(0.0, res.remainingForFreeShipping, 0.001)
    assertEquals(1.0f, res.freeShippingProgress, 0.001f)
  }

  @Test
  fun computeFreeShipping_storePickup_alwaysHasZeroFee() {
    val res = computeFreeShipping(
      netMerchandise = 120.0,
      isStorePickup = true,
      hasItems = true,
      baseZoneFee = 40.0,
      enabled = true,
      threshold = 500.0,
    )
    assertFalse(res.qualifiesForFreeShipping)
    assertEquals(0.0, res.deliveryFee, 0.001)
  }

  @Test
  fun normalizeOtpDigits_normalizesArabicPersianAndAscii() {
    assertEquals("4821", normalizeOtpDigits("٤٨٢١"))
    assertEquals("9053", normalizeOtpDigits("۹۰۵۳"))
    assertEquals("1739", normalizeOtpDigits(" 1-٧٣۹ "))
  }

  @Test
  fun generateDeliveryOtp_generatesFourDigitNumericCode() {
    repeat(30) {
      val otp = generateDeliveryOtp()
      assertEquals(4, otp.length)
      val num = otp.toInt()
      assertTrue(num in 1000..9999)
    }
  }

  @Test
  fun orderHasPendingDeliveryOtp_respectsFulfillmentAndVerificationState() {
    val activeDeliveryOrder = Order(
      id = "o1",
      fulfillmentType = FulfillmentMethod.DELIVERY.code,
      orderStatus = OrderStatus.OUT_FOR_DELIVERY.code,
      deliveryOtp = "4821",
      deliveryOtpVerified = false,
    )
    assertTrue(activeDeliveryOrder.hasPendingDeliveryOtp)

    val verifiedOrder = activeDeliveryOrder.copy(deliveryOtpVerified = true)
    assertFalse(verifiedOrder.hasPendingDeliveryOtp)

    val deliveredOrder = activeDeliveryOrder.copy(orderStatus = OrderStatus.DELIVERED.code)
    assertFalse(deliveredOrder.hasPendingDeliveryOtp)

    val cancelledOrder = activeDeliveryOrder.copy(orderStatus = OrderStatus.CANCELLED.code)
    assertFalse(cancelledOrder.hasPendingDeliveryOtp)

    val pickupOrder = activeDeliveryOrder.copy(fulfillmentType = FulfillmentMethod.STORE_PICKUP.code)
    assertFalse(pickupOrder.hasPendingDeliveryOtp)
  }

  @Test
  fun searchMissHelpers_normalizeAndBuildDeterministicAsciiDocIds() {
    val q1 = normalizeSearchQuery("  منظف   أرضيات  لافندر ")
    val q2 = normalizeSearchQuery("منظف أرضيات لافندر")
    assertEquals("منظف أرضيات لافندر", q1)
    assertEquals(q1, q2)

    val id1 = buildSearchMissDocId(q1)
    val id2 = buildSearchMissDocId(q2)
    assertEquals(id1, id2)
    assertTrue(id1.startsWith("sm_"))
    assertTrue(id1.matches(Regex("^sm_[a-z0-9]+$")))
  }

  @Test
  fun storeGeneralSettings_deliveryOtpEnabledDefaultsTrueAndToggles() {
    val defaultSettings = com.example.data.model.StoreGeneralSettings()
    assertTrue(defaultSettings.deliveryOtpEnabled)
    val disabledSettings = defaultSettings.copy(deliveryOtpEnabled = false)
    assertFalse(disabledSettings.deliveryOtpEnabled)
  }
}
