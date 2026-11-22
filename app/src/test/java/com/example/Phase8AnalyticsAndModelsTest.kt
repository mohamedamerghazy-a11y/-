package com.example

import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.example.data.model.Banner
import com.example.data.model.BannerPlacement
import com.example.data.model.BannerStyle
import com.example.data.model.BannerTargetType
import com.example.data.model.BrandShowcase
import com.example.data.model.BrandShowcaseCategory
import com.example.data.model.CategoriesTabStyle
import com.example.data.model.FlashSale
import com.example.data.model.FlashSaleProductItem
import com.example.data.model.FlashSaleStatus
import com.example.data.model.HomeCategoryStyle
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.OrderItemSnapshot
import com.example.data.model.Product
import com.example.data.model.StoreContactChannel
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.StorePhoneEntry
import com.example.data.model.StoreSocialLink
import com.example.ui.viewmodel.CatalogUiState
import com.example.ui.viewmodel.ReportAnalyticsCalculator
import com.example.ui.viewmodel.ReportPeriod
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class Phase8AnalyticsAndModelsTest {

  private val fixedNowMillis = 1_750_000_000_000L
  private val millisPerDay = 86_400_000L

  private fun sampleOrder(
    id: String,
    daysOld: Long,
    userId: String = "u1",
    customerName: String = "أحمد محمد",
    accountType: String = "retail",
    orderStatus: String = "delivered",
    subtotal: Double = 100.0,
    discountAmount: Double = 0.0,
    deliveryFee: Double = 20.0,
    totalAmount: Double = 120.0,
    items: List<OrderItemSnapshot> = listOf(
      OrderItemSnapshot(
        productId = "p1",
        productName = "خلاط كهربائي",
        unitPrice = 100.0,
        quantity = 1,
        lineTotal = 100.0,
      ),
    ),
  ): Order {
    val ts = Timestamp(Date(fixedNowMillis - (daysOld * millisPerDay)))
    return Order(
      id = id,
      userId = userId,
      customerName = customerName,
      accountTypeAtOrder = accountType,
      orderStatus = orderStatus,
      subtotal = subtotal,
      discountAmount = discountAmount,
      deliveryFee = deliveryFee,
      totalAmount = totalAmount,
      items = items,
      createdAt = ts,
      updatedAt = ts,
    )
  }

  @Test
  fun reportAnalyticsCalculator_excludesCancelledFromRevenueAndSplitsChannels() {
    val orders = listOf(
      sampleOrder(
        id = "o1",
        daysOld = 0,
        userId = "u1",
        customerName = "أحمد",
        accountType = "retail",
        orderStatus = "delivered",
        discountAmount = 10.0,
        deliveryFee = 25.0,
        totalAmount = 215.0,
        items = listOf(
          OrderItemSnapshot(productId = "p1", productName = "منتج أ", quantity = 2, lineTotal = 200.0),
        ),
      ),
      sampleOrder(
        id = "o2",
        daysOld = 3,
        userId = "u2",
        customerName = "شركة النور",
        accountType = "wholesale",
        orderStatus = "confirmed",
        discountAmount = 50.0,
        deliveryFee = 0.0,
        totalAmount = 950.0,
        items = listOf(
          OrderItemSnapshot(productId = "p2", productName = "منتج ب", quantity = 10, lineTotal = 1000.0),
        ),
      ),
      sampleOrder(
        id = "o3",
        daysOld = 2,
        userId = "u3",
        customerName = "عميل ملغي",
        accountType = "retail",
        orderStatus = "cancelled",
        discountAmount = 30.0,
        deliveryFee = 20.0,
        totalAmount = 500.0,
        items = listOf(
          OrderItemSnapshot(productId = "p3", productName = "منتج ج", quantity = 5, lineTotal = 500.0),
        ),
      ),
      sampleOrder(
        id = "o4",
        daysOld = 45,
        userId = "u1",
        customerName = "أحمد",
        accountType = "retail",
        orderStatus = "delivered",
        discountAmount = 0.0,
        deliveryFee = 15.0,
        totalAmount = 115.0,
        items = listOf(
          OrderItemSnapshot(productId = "p1", productName = "منتج أ", quantity = 1, lineTotal = 100.0),
        ),
      ),
    )

    val allTime = ReportAnalyticsCalculator.compute(orders, ReportPeriod.ALL_TIME, fixedNowMillis)
    assertEquals(4, allTime.totalOrdersCount)
    assertEquals(1, allTime.cancelledOrdersCount)
    assertEquals(2, allTime.completedOrdersCount)
    assertEquals(1, allTime.pendingOrdersCount)
    assertEquals(1280.0, allTime.totalRevenue, 0.001)
    assertEquals(330.0, allTime.retailRevenue, 0.001)
    assertEquals(950.0, allTime.wholesaleRevenue, 0.001)
    assertEquals(60.0, allTime.totalDiscounts, 0.001)
    assertEquals(40.0, allTime.totalDeliveryFees, 0.001)
    assertEquals(7, allTime.dailyRevenueBuckets.size)
    assertEquals("p2", allTime.topProducts.first().productId)
    assertEquals("u2", allTime.topCustomers.first().userId)

    val monthly = ReportAnalyticsCalculator.compute(orders, ReportPeriod.MONTHLY, fixedNowMillis)
    assertEquals(3, monthly.totalOrdersCount)
    assertEquals(1165.0, monthly.totalRevenue, 0.001)

    val daily = ReportAnalyticsCalculator.compute(orders, ReportPeriod.DAILY, fixedNowMillis)
    assertEquals(1, daily.totalOrdersCount)
    assertEquals(215.0, daily.totalRevenue, 0.001)
  }

  @Test
  fun reportAnalyticsCalculator_limitsTopListsToFiveAndHandlesEmpty() {
    val empty = ReportAnalyticsCalculator.compute(emptyList(), ReportPeriod.ALL_TIME, fixedNowMillis)
    assertEquals(0, empty.totalOrdersCount)
    assertEquals(0.0, empty.totalRevenue, 0.001)
    assertEquals(0.0, empty.averageOrderValue, 0.001)
    assertEquals(7, empty.dailyRevenueBuckets.size)

    val orders = (1..7).map { idx ->
      sampleOrder(
        id = "ord_$idx",
        daysOld = 1,
        userId = "user_$idx",
        customerName = "عميل $idx",
        totalAmount = idx * 100.0,
        items = listOf(
          OrderItemSnapshot(
            productId = "prod_$idx",
            productName = "صنف $idx",
            quantity = idx,
            lineTotal = idx * 100.0,
          ),
        ),
      )
    }
    val computed = ReportAnalyticsCalculator.compute(orders, ReportPeriod.WEEKLY, fixedNowMillis)
    assertEquals(5, computed.topProducts.size)
    assertEquals(5, computed.topCustomers.size)
    assertEquals("prod_7", computed.topProducts.first().productId)
    assertEquals("user_7", computed.topCustomers.first().userId)
  }

  @Test
  fun phase8EnumMappers_parseKnownAndFallbackSafely() {
    assertEquals(BannerTargetType.OFFERS, BannerTargetType.fromCode("offers"))
    assertEquals(BannerTargetType.CATEGORY, BannerTargetType.fromCode("category"))
    assertEquals(BannerTargetType.PRODUCT, BannerTargetType.fromCode("product"))
    assertEquals(BannerTargetType.NONE, BannerTargetType.fromCode("unknown_value"))
    assertEquals(BannerTargetType.NONE, BannerTargetType.fromCode(null))

    assertEquals(NotificationType.ORDER_STATUS, NotificationType.fromCode("order_status"))
    assertEquals(NotificationType.PAYMENT_STATUS, NotificationType.fromCode("payment_status"))
    assertEquals(NotificationType.WHOLESALE_STATUS, NotificationType.fromCode("wholesale_status"))
    assertEquals(NotificationType.OFFER_PROMO, NotificationType.fromCode("offer_promo"))
    assertEquals(NotificationType.GENERAL, NotificationType.fromCode("anything_else"))
  }

  @Test
  fun productDiscountComputation_andStaffPermissionsCompleteness() {
    val discounted = Product(retailPrice = 200.0, retailDiscountPrice = 150.0)
    assertTrue(discounted.hasDiscount)
    assertEquals(150.0, discounted.effectiveRetailPrice, 0.001)
    assertEquals(25, discounted.discountPercent)

    val invalidDiscount = Product(retailPrice = 200.0, retailDiscountPrice = 250.0)
    assertFalse(invalidDiscount.hasDiscount)
    assertEquals(200.0, invalidDiscount.effectiveRetailPrice, 0.001)
    assertEquals(0, invalidDiscount.discountPercent)

    assertEquals(19, StaffPermission.entries.size)
    assertEquals(StaffPermission.entries.toSet(), StaffRole.OWNER.permissions)
  }

  @Test
  fun bannerPlacementStyleDurationAndCategoryDisplayEnums_mapSafely() {
    assertEquals(BannerPlacement.TOP_HOME, BannerPlacement.fromCode("unknown"))
    assertEquals(BannerPlacement.BELOW_FEATURED, BannerPlacement.fromCode("below_featured"))
    assertEquals(BannerPlacement.BELOW_NEWEST, BannerPlacement.fromCode("below_newest"))

    assertEquals(BannerStyle.NORMAL, BannerStyle.fromCode(null))
    assertEquals(BannerStyle.SLIM, BannerStyle.fromCode("slim"))

    assertEquals(HomeCategoryStyle.CIRCULAR, HomeCategoryStyle.fromCode("invalid"))
    assertEquals(HomeCategoryStyle.DOUBLE_GRID, HomeCategoryStyle.fromCode("double_grid"))

    assertEquals(CategoriesTabStyle.NOON_PORTRAIT, CategoriesTabStyle.fromCode("invalid"))
    assertEquals(CategoriesTabStyle.HEXAGON_GRID, CategoriesTabStyle.fromCode("hexagon_grid"))
    assertEquals(CategoriesTabStyle.CUT_CORNER_GRID, CategoriesTabStyle.fromCode("cut_corner_grid"))

    assertEquals(1, Banner(title = "", durationSeconds = 0).safeDurationSeconds)
    assertEquals(120, Banner(title = "", durationSeconds = 240).safeDurationSeconds)
    assertEquals(12, Banner(title = "", durationSeconds = 12).safeDurationSeconds)

    val settings = StoreGeneralSettings(
      showHeroSection = false,
      homeCategoryStyle = HomeCategoryStyle.NOON_PORTRAIT.code,
      categoriesTabStyle = CategoriesTabStyle.HEXAGON_GRID.code,
      retailFacebookUrl = "https://facebook.com/retail",
      wholesaleFacebookUrl = "https://facebook.com/wholesale",
      retailPhones = listOf(StorePhoneEntry("فرع 1", "01000000001", StoreContactChannel.RETAIL.code)),
      wholesalePhones = listOf(StorePhoneEntry("مبيعات جملة", "01000000002", StoreContactChannel.WHOLESALE.code)),
      socialPages = listOf(StoreSocialLink("إنستغرام", "https://instagram.com/aleman", StoreContactChannel.BOTH.code)),
    )
    assertFalse(settings.showHeroSection)
    assertEquals(HomeCategoryStyle.NOON_PORTRAIT, settings.homeCategoryStyleEnum)
    assertEquals(CategoriesTabStyle.HEXAGON_GRID, settings.categoriesTabStyleEnum)
    assertEquals(StoreContactChannel.RETAIL, StoreContactChannel.fromCode("unknown"))
    assertEquals(StoreContactChannel.WHOLESALE, StoreContactChannel.fromCode("wholesale"))
    assertEquals(StoreContactChannel.BOTH, settings.socialPages.first().channelEnum)
    assertEquals(1, settings.retailPhones.size)
    assertEquals(1, settings.wholesalePhones.size)
  }

  @Test
  fun flashSaleStatus_priceIsolation_andDeliveryDriverRolePermissions() {
    val startMs = fixedNowMillis + 60_000L
    val endMs = fixedNowMillis + 3_600_000L
    val sale = FlashSale(
      id = "fs_1",
      title = "عرض الساعة الذهبية",
      badgeText = "خصم خاص",
      startsAt = Timestamp(Date(startMs)),
      endsAt = Timestamp(Date(endMs)),
      isActive = true,
      items = listOf(
        FlashSaleProductItem(
          productId = "p_detergent",
          flashRetailPrice = 85.0,
          flashWholesalePrice = 72.0,
          maxQtyPerOrder = 4,
        ),
      ),
    )

    // قبل وقت البداية -> Upcoming + المتبقي = 60 ثانية
    assertEquals(FlashSaleStatus.UPCOMING, sale.statusAt(fixedNowMillis))
    assertEquals(60_000L, sale.remainingMillisAt(fixedNowMillis))

    // أثناء وقت العرض -> Active
    assertEquals(FlashSaleStatus.ACTIVE, sale.statusAt(startMs + 5_000L))
    assertEquals(endMs - (startMs + 5_000L), sale.remainingMillisAt(startMs + 5_000L))

    // بعد وقت الانتهاء -> Ended
    assertEquals(FlashSaleStatus.ENDED, sale.statusAt(endMs + 1_000L))
    assertEquals(0L, sale.remainingMillisAt(endMs + 1_000L))

    // عزل أسعار الفلاش سيل عن أسعار المنتج العادي
    val regularProduct = Product(id = "p_detergent", retailPrice = 120.0, wholesalePrice = 100.0)
    assertEquals(85.0, sale.flashUnitPrice("p_detergent", isWholesaleActive = false) ?: 0.0, 0.001)
    assertEquals(72.0, sale.flashUnitPrice("p_detergent", isWholesaleActive = true) ?: 0.0, 0.001)
    assertNull(sale.flashUnitPrice("other_prod", isWholesaleActive = false))
    assertEquals(120.0, regularProduct.effectiveRetailPrice, 0.001)
    assertEquals(100.0, regularProduct.wholesalePrice, 0.001)

    // صلاحيات مندوب التوصيل ومعاينة الجملة وتخصيص بريد المالك
    assertEquals(setOf(StaffPermission.DELIVER_ORDERS), StaffRole.DELIVERY_DRIVER.permissions)
    assertTrue(StaffRole.PRODUCT_MANAGER.permissions.contains(StaffPermission.MANAGE_FLASH_SALES))
    assertTrue(StaffRole.PRODUCT_MANAGER.permissions.contains(StaffPermission.PREVIEW_WHOLESALE_STORE))

    val customOwner = StoreGeneralSettings(ownerEmail = "boss@aleman.store")
    assertEquals("boss@aleman.store", customOwner.ownerEmail)
  }

  @Test
  fun numeralFormatSwitch_sequentialOrderNumberEt1000_andWhatsAppPhoneNormalization() {
    val previousFormat = com.example.core.util.ArabicFormatter.numeralFormat
    try {
      com.example.core.util.ArabicFormatter.setNumeralFormat(com.example.data.model.NumeralFormat.ARABIC)
      assertEquals("١٬٢٥٠ ج.م", com.example.core.util.ArabicFormatter.formatCurrency(1250.0))
      assertEquals("٢٥", com.example.core.util.ArabicFormatter.formatNumber(25))

      com.example.core.util.ArabicFormatter.setNumeralFormat(com.example.data.model.NumeralFormat.ENGLISH)
      assertEquals("1,250 ج.م", com.example.core.util.ArabicFormatter.formatCurrency(1250.0))
      assertEquals("25", com.example.core.util.ArabicFormatter.formatNumber(25))
      assertEquals("ET-1042", com.example.core.util.ArabicFormatter.formatDigits("ET-١٠٤٢"))
    } finally {
      com.example.core.util.ArabicFormatter.setNumeralFormat(previousFormat)
    }

    assertEquals(
      "ET-1000",
      com.example.data.repository.OrderRepository.formatSequentialOrderNumber(500L),
    )
    assertEquals(
      "ET-1000",
      com.example.data.repository.OrderRepository.formatSequentialOrderNumber(1000L),
    )
    assertEquals(
      "ET-1042",
      com.example.data.repository.OrderRepository.formatSequentialOrderNumber(1042L),
    )

    assertEquals(
      "201012345678",
      com.example.core.printing.ThermalReceiptRenderer.normalizeWhatsAppPhone("01012345678"),
    )
    assertEquals(
      "201012345678",
      com.example.core.printing.ThermalReceiptRenderer.normalizeWhatsAppPhone("٠١٠١٢٣٤٥٦٧٨"),
    )
  }

  @Test
  fun fulfillmentMethod_pickupStatusLabels_andTimelineSteps() {
    assertEquals(
      com.example.core.model.FulfillmentMethod.DELIVERY,
      com.example.core.model.FulfillmentMethod.fromCode(null),
    )
    assertEquals(
      com.example.core.model.FulfillmentMethod.STORE_PICKUP,
      com.example.core.model.FulfillmentMethod.fromCode("pickup"),
    )

    val pickupOrder = Order(id = "o_pickup", fulfillmentType = "pickup", orderStatus = "ready_for_delivery")
    assertTrue(pickupOrder.isStorePickup)
    assertEquals("جاهز للاستلام من المتجر", pickupOrder.statusEnum.labelFor(isStorePickup = true))
    assertEquals(
      "تم الاستلام من المتجر",
      com.example.core.model.OrderStatus.DELIVERED.labelFor(isStorePickup = true),
    )

    val pickupSteps = com.example.core.model.OrderStatus.activeTimelineStepsFor(isStorePickup = true)
    assertFalse(pickupSteps.contains(com.example.core.model.OrderStatus.OUT_FOR_DELIVERY))
    assertEquals(6, pickupSteps.size)
  }

  @Test
  fun storePolicies_defaultsAndEffectiveFallback_andPolicyTabs() {
    assertEquals(
      com.example.data.model.StorePolicyTab.PRIVACY,
      com.example.data.model.StorePolicyTab.fromCode(null),
    )
    assertEquals(
      com.example.data.model.StorePolicyTab.RETURN,
      com.example.data.model.StorePolicyTab.fromCode("return"),
    )
    assertEquals(
      com.example.data.model.StorePolicyTab.TERMS,
      com.example.data.model.StorePolicyTab.fromCode("terms"),
    )

    val blankSettings = StoreGeneralSettings(
      privacyPolicyText = "   ",
      returnPolicyText = "",
      termsOfServiceText = "",
      privacyPolicyUrl = "https://aleman.example/privacy",
      accountDeletionUrl = "https://aleman.example/delete-account",
    )
    assertTrue(blankSettings.effectivePrivacyPolicyText.contains("حذف الحساب"))
    assertTrue(blankSettings.effectiveReturnPolicyText.contains("١٤ يومًا"))
    assertTrue(blankSettings.effectiveTermsOfServiceText.contains("بالجنيه المصري"))
    assertEquals("https://aleman.example/privacy", blankSettings.privacyPolicyUrl)
    assertEquals("https://aleman.example/delete-account", blankSettings.accountDeletionUrl)
  }

  @Test
  fun brandShowcase_sortsCategoriesAndScopesExclusiveProducts() {
    val showcase = BrandShowcase(
      id = "brand_oxi",
      title = "أوكسي",
      categories = listOf(
        BrandShowcaseCategory(id = "cat_2", name = "منعم أقمشة", imageUrl = "https://ex/2.png", displayOrder = 2),
        BrandShowcaseCategory(id = "cat_1", name = "مسحوق غسيل", imageUrl = "https://ex/1.png", displayOrder = 1),
      ),
    )
    assertEquals(listOf("cat_1", "cat_2"), showcase.sortedCategories.map { it.id })

    val regular = Product(id = "p_reg", name = "صابون يدين", categoryId = "cat_main")
    val publicBrand = Product(
      id = "p_pub",
      name = "أوكسي لافندر",
      categoryId = "cat_main",
      brandShowcaseId = "brand_oxi",
      brandCategoryId = "cat_2",
      exclusiveToBrandShowcase = false,
    )
    val exclusiveBrand = Product(
      id = "p_exc",
      name = "أوكسي حصري",
      categoryId = "cat_main",
      brandShowcaseId = "brand_oxi",
      brandCategoryId = "cat_1",
      exclusiveToBrandShowcase = true,
    )

    val state = CatalogUiState(
      isLoading = false,
      products = listOf(regular, publicBrand, exclusiveBrand),
      brandShowcases = listOf(showcase),
    )

    // Storefront hides exclusive products while keeping public brand products
    assertEquals(listOf("p_reg", "p_pub"), state.storefrontProducts.map { it.id })
    assertEquals(listOf("p_reg", "p_pub"), state.filteredProducts(isWholesaleApproved = false).map { it.id })

    // Showcase returns both public and exclusive products and filters accurately by brandCategoryId
    assertEquals(listOf("p_pub", "p_exc"), state.productsForBrandShowcase("brand_oxi").map { it.id })
    assertEquals(listOf("p_exc"), state.productsForBrandShowcase("brand_oxi", "cat_1").map { it.id })
    assertEquals(listOf("p_pub"), state.productsForBrandShowcase("brand_oxi", "cat_2").map { it.id })

    // Similar products for exclusive item stay scoped to its brand showcase and exclude unrelated items
    assertEquals(listOf("p_pub"), state.similarProducts(exclusiveBrand).map { it.id })
    // Similar products for storefront item exclude exclusive brand items
    assertEquals(listOf("p_pub"), state.similarProducts(regular).map { it.id })
  }
}

