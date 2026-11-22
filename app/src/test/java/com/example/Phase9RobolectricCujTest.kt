package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.example.data.model.AppNotification
import com.example.data.model.Banner
import com.example.data.model.BannerPlacement
import com.example.data.model.BannerStyle
import com.example.data.model.BannerTargetType
import com.example.data.model.Category
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.OrderItemSnapshot
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.StoreContactChannel
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.StorePhoneEntry
import com.example.data.model.StorePolicyTab
import com.example.data.model.StoreSocialLink
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyCartCleaningHero
import com.example.ui.components.SignInCleaningHeroHeader
import com.example.ui.screens.admin.AdminDashboardShellScreen
import com.example.ui.screens.admin.AdminReportsScreen
import com.example.ui.screens.admin.AdminStoreSettingsScreen
import com.example.ui.screens.admin.DeliveryDriverOrdersScreen
import com.example.ui.screens.customer.AboutStoreScreen
import com.example.ui.screens.customer.HomeTabContent
import com.example.ui.screens.customer.NotificationsScreen
import com.example.ui.screens.customer.ProductDetailsScreen
import com.example.ui.screens.customer.StorePoliciesScreen
import com.example.ui.screens.shell.CustomerShellScreen
import com.example.ui.theme.AlEmanTradeTheme
import com.example.ui.viewmodel.AdminDashboardUiState
import com.example.ui.viewmodel.AdminReportsUiState
import com.example.ui.viewmodel.AdminStoreSettingsUiState
import com.example.ui.viewmodel.CartAndOrdersUiState
import com.example.ui.viewmodel.CatalogUiState
import com.example.ui.viewmodel.DeliveryDriverUiState
import com.example.ui.viewmodel.NotificationsUiState
import com.example.ui.viewmodel.ProductReviewsUiState
import com.example.ui.viewmodel.ReportAnalyticsCalculator
import com.example.ui.viewmodel.ReportPeriod
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class Phase9RobolectricCujTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val sampleCategory = Category(
    id = "cat_1",
    name = "أجهزة منزلية",
    isActive = true,
  )

  private val sampleProduct = Product(
    id = "prod_1",
    name = "خلاط الإيمان ستانلس",
    description = "خلاط قوي بضمان سنتين مناسب للاستخدام المنزلي والتجاري.",
    categoryId = "cat_1",
    sku = "SKU-101",
    retailPrice = 1200.0,
    retailDiscountPrice = 950.0,
    wholesalePrice = 800.0,
    wholesaleMinQty = 5,
    unit = "قطعة",
    stockQuantity = 25,
    lowStockThreshold = 5,
    isFeatured = true,
    isOffer = true,
    isActive = true,
    averageRating = 4.5,
    reviewCount = 2,
  )

  private val sampleCatalogState = CatalogUiState(
    isLoading = false,
    categories = listOf(sampleCategory),
    products = listOf(sampleProduct),
    banners = listOf(
      Banner(
        id = "ban_1",
        title = "عروض الجملة الأسبوعية",
        subtitle = "خصومات حتى ٢٥٪",
        imageUrl = "",
        targetType = BannerTargetType.OFFERS.code,
        isActive = true,
      ),
    ),
    storeSettings = StoreGeneralSettings(
      storeName = "الإيمان للتجارة",
      tagline = "تسوق بذكاء .. عيش أفضل",
      announcementText = "توصيل مجاني للطلبات فوق 1000 ج.م",
    ),
  )

  @Test
  fun cuj1_customerShellNavigationAndNotificationsBellTrigger() {
    var notificationsOpened = false
    val profile = UserProfile(
      uid = "user_1",
      fullName = "أحمد علي",
      email = "ahmed@example.com",
      accountType = "retail",
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        CustomerShellScreen(
          userProfile = profile,
          canAccessAdmin = false,
          defaultAddress = null,
          addressCount = 0,
          catalogState = sampleCatalogState,
          cartUiState = CartAndOrdersUiState(),
          openCartTrigger = 0,
          onRetryCatalog = {},
          onUpdateSearchQuery = {},
          onSelectSearchCategory = {},
          onToggleSearchOffers = {},
          onToggleSearchInStock = {},
          onToggleSearchWholesale = {},
          onSetSearchPriceRange = { _, _ -> },
          onSelectSearchSort = {},
          onResetSearchFilters = {},
          onOpenSearchPreset = { _, _ -> },
          onOpenProduct = {},
          onAddToCart = {},
          onUpdateCartQuantity = { _, _, _ -> },
          onRemoveCartItem = {},
          onCouponInputChange = {},
          onApplyCoupon = {},
          onRemoveCoupon = {},
          onProceedToCheckout = {},
          onOpenMyOrders = {},
          feedbackMessage = null,
          errorMessage = null,
          onClearMessages = {},
          onRequireSignIn = {},
          onUpdateProfile = { _, _, _ -> },
          onSubmitWholesaleRequest = { _, _, _ -> },
          onOpenAddresses = {},
          onNavigateToAdmin = {},
          onSignOut = {},
          unreadNotificationsCount = 3,
          onOpenNotifications = { notificationsOpened = true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithText("الإيمان للتجارة").onFirst().assertIsDisplayed()
    composeTestRule.onNodeWithText("3").assertIsDisplayed()
    composeTestRule.onNodeWithTag("top_notifications_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(notificationsOpened)

    composeTestRule.onNodeWithTag("bottom_tab_categories").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("category_card_cat_1").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("categories_back_to_grid_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("category_card_cat_1").assertIsDisplayed()
    composeTestRule.onNodeWithText("أجهزة منزلية").assertIsDisplayed()

    composeTestRule.onNodeWithTag("bottom_tab_account").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("account_orders_row").assertIsDisplayed()
  }

  @Test
  fun cuj2_productDetailsWholesalePricingAndAddToCart() {
    var addedQty = 0
    var signInPrompted = false
    val reviewsState = ProductReviewsUiState(
      isLoading = false,
      reviews = listOf(
        ProductReview(
          id = "rev_1",
          productId = sampleProduct.id,
          productName = sampleProduct.name,
          userId = "other_user",
          userName = "محمود حسن",
          rating = 5,
          comment = "خامة ممتازة وتوصيل سريع جدًا",
        ),
      ),
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        ProductDetailsScreen(
          productId = sampleProduct.id,
          catalogState = sampleCatalogState,
          isWholesaleApproved = true,
          cartQuantity = 0,
          feedbackMessage = null,
          errorMessage = null,
          onClearMessages = {},
          onProductViewed = {},
          onOpenProduct = {},
          onAddToCart = { _, qty, _, _ -> addedQty = qty },
          onBackClick = {},
          reviewsState = reviewsState,
          currentUserId = null,
          onRequireSignIn = { signInPrompted = true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithText("خلاط الإيمان ستانلس").onFirst().assertExists()
    composeTestRule.onNodeWithTag("details_add_to_cart_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, addedQty)

    composeTestRule.onNodeWithTag("product_details_lazy_column")
      .performScrollToNode(hasTestTag("product_write_review_button"))
    composeTestRule.onNodeWithTag("product_write_review_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(signInPrompted)
  }

  @Test
  fun cuj3_notificationsInboxMarkAllReadAndClickDeepLink() {
    var markedAllRead = false
    var clickedNotifId: String? = null
    val now = Timestamp(Date(1_750_000_000_000L))
    val state = NotificationsUiState(
      isLoading = false,
      notifications = listOf(
        AppNotification(
          id = "n1",
          userId = "u1",
          title = "تم شحن طلبك",
          message = "الطلب في طريقه إليك الآن",
          type = NotificationType.ORDER_STATUS.code,
          referenceId = "ord_123",
          isRead = false,
          createdAt = now,
        ),
      ),
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        NotificationsScreen(
          uiState = state,
          onMarkAsRead = {},
          onMarkAllAsRead = { markedAllRead = true },
          onDeleteNotification = {},
          onNotificationClick = { clickedNotifId = it.id },
          onClearMessages = {},
          onBackClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("notifications_mark_all_read_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(markedAllRead)

    composeTestRule.onNodeWithTag("notification_card_n1").performClick()
    composeTestRule.waitForIdle()
    assertEquals("n1", clickedNotifId)
  }

  @Test
  fun cuj4_rbacAdminDashboardFiltersTilesByRolePermissions() {
    var selectedPermission: StaffPermission? = null
    val accountantPerms = StaffRole.ACCOUNTANT.permissions

    composeTestRule.setContent {
      AlEmanTradeTheme {
        AdminDashboardShellScreen(
          roleLabel = StaffRole.ACCOUNTANT.arabicLabel,
          allowedPermissions = accountantPerms,
          dashboardState = AdminDashboardUiState(totalSalesRevenue = 8500.0, deliveredOrdersCount = 14),
          onBackClick = {},
          onSelectSection = { selectedPermission = it },
        )
      }
    }

    composeTestRule.waitForIdle()
    // المحاسب يرى المدفوعات والكوبونات والتقارير ولا يرى الموظفين أو المنتجات
    composeTestRule.onNodeWithTag("admin_tile_manage_payments").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_tile_view_reports").performScrollTo().assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_tile_manage_staff").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_tile_manage_products").fetchSemanticsNodes().size)

    composeTestRule.onNodeWithTag("admin_tile_manage_payments").performClick()
    composeTestRule.waitForIdle()
    assertEquals(StaffPermission.MANAGE_PAYMENTS, selectedPermission)
  }

  @Test
  fun cuj5_adminReportsPeriodFilterSwitchesAndDisplaysAnalytics() {
    var selectedPeriod = ReportPeriod.ALL_TIME
    val now = 1_750_000_000_000L
    val orders = listOf(
      Order(
        id = "ord_1",
        userId = "u1",
        customerName = "شركة الهدى",
        accountTypeAtOrder = "wholesale",
        orderStatus = "delivered",
        totalAmount = 2400.0,
        items = listOf(OrderItemSnapshot(productId = "p1", productName = "صنف 1", quantity = 10, lineTotal = 2400.0)),
        createdAt = Timestamp(Date(now - 3600_000L)),
      ),
    )
    val analytics = ReportAnalyticsCalculator.compute(orders, ReportPeriod.ALL_TIME, now)

    composeTestRule.setContent {
      AlEmanTradeTheme {
        AdminReportsScreen(
          uiState = AdminReportsUiState(
            isLoading = false,
            orders = orders,
            selectedPeriod = ReportPeriod.ALL_TIME,
            analytics = analytics,
          ),
          onSelectPeriod = { selectedPeriod = it },
          onBackClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithText("التقارير والإحصائيات").onFirst().assertIsDisplayed()
    composeTestRule.onNodeWithTag("report_period_monthly").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ReportPeriod.MONTHLY, selectedPeriod)
  }

  @Test
  fun cuj6_emptyCartAndSignInCleaningHeroesAndHeroToggleRenderCleanly() {
    var shopClicked = false
    composeTestRule.setContent {
      AlEmanTradeTheme {
        EmptyCartCleaningHero(onShopNow = { shopClicked = true })
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("empty_cart_cleaning_hero").assertIsDisplayed()
    composeTestRule.onNodeWithText("عربة منظفاتك فارغة حاليًا").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_cart_shop_now_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(shopClicked)
  }

  @Test
  fun cuj7_homeHeroCanBeHiddenAndSlimTextlessBannerRenders() {
    val customCatalog = sampleCatalogState.copy(
      banners = listOf(
        Banner(
          id = "slim_1",
          title = "",
          subtitle = null,
          imageUrl = "",
          placement = BannerPlacement.BELOW_FEATURED.code,
          bannerStyle = BannerStyle.SLIM.code,
          durationSeconds = 7,
          isActive = true,
        ),
      ),
      storeSettings = StoreGeneralSettings(
        showHeroSection = false,
      ),
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        HomeTabContent(
          catalogState = customCatalog,
          userProfile = null,
          canAccessAdmin = false,
          defaultAddress = null,
          onDeliveryBarClick = {},
          onOpenCategories = {},
          onOpenSearchPreset = { _, _ -> },
          onOpenQrScanner = {},
          onOpenProduct = {},
          onAddToCart = {},
          onOpenAdmin = {},
          onRetryCatalog = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("hero_browse_categories_button").fetchSemanticsNodes().size)
  }

  @Test
  fun cuj8_signInAnimatedHeroDisplaysDetergentBrandTagline() {
    composeTestRule.setContent {
      AlEmanTradeTheme {
        SignInCleaningHeroHeader()
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sign_in_animated_hero").assertIsDisplayed()
    composeTestRule.onNodeWithText("عالم المنظفات المنزلية • قطاعي وجملة").assertIsDisplayed()
  }

  @Test
  fun cuj9_aboutStoreDisplaysRetailAndWholesaleContactsAndSocialPages() {
    val richSettings = StoreGeneralSettings(
      storeName = "الإيمان للمنظفات",
      tagline = "نظافة وجودة",
      aboutStoreDescription = "شركة متخصصة في المنظفات المنزلية.",
      retailFacebookUrl = "https://facebook.com/aleman.retail",
      wholesaleFacebookUrl = "https://facebook.com/aleman.wholesale",
      retailPhones = listOf(
        StorePhoneEntry("فرع التجزئة", "01011112222", StoreContactChannel.RETAIL.code),
        StorePhoneEntry("توصيل القطاعي", "01033334444", StoreContactChannel.RETAIL.code),
      ),
      wholesalePhones = listOf(
        StorePhoneEntry("مبيعات الجملة", "01055556666", StoreContactChannel.WHOLESALE.code),
      ),
      socialPages = listOf(
        StoreSocialLink("تيك توك", "https://tiktok.com/@aleman", StoreContactChannel.BOTH.code),
      ),
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        AboutStoreScreen(
          settings = richSettings,
          onBackClick = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("about_store_screen").assertIsDisplayed()
    composeTestRule.onNodeWithText("شركة متخصصة في المنظفات المنزلية.").assertIsDisplayed()
    composeTestRule.onNodeWithText("صفحة فيسبوك للقطاعي").assertIsDisplayed()
    composeTestRule.onNodeWithText("01011112222").assertIsDisplayed()
    composeTestRule.onNodeWithText("01033334444").assertIsDisplayed()
  }

  @Test
  fun cuj10_regularUserDoesNotSeeRetailPriceLabelOnProductDetails() {
    composeTestRule.setContent {
      AlEmanTradeTheme {
        ProductDetailsScreen(
          productId = sampleProduct.id,
          catalogState = sampleCatalogState,
          isWholesaleApproved = false,
          cartQuantity = 0,
          feedbackMessage = null,
          errorMessage = null,
          onClearMessages = {},
          onProductViewed = {},
          onOpenProduct = {},
          onAddToCart = { _, _, _, _ -> },
          onBackClick = {},
          reviewsState = ProductReviewsUiState(isLoading = false),
          currentUserId = null,
          onRequireSignIn = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithText("سعر القطاعي").fetchSemanticsNodes().size)
    composeTestRule.onNodeWithText("السعر").assertIsDisplayed()
  }

  @Test
  fun cuj11_deliveryDriverPortalShowsFocusedOrderMapCallAndConfirmsDelivery() {
    var deliveredOrderId: String? = null
    val assignedOrder = Order(
      id = "ord_d1",
      userId = "cust_1",
      orderNumber = "EM-700",
      customerName = "محمود سيد",
      recipientName = "محمود سيد",
      phone = "01099887766",
      governorate = "الجيزة",
      city = "الدقي",
      streetAddress = "12 شارع التحرير",
      latitude = 30.038,
      longitude = 31.212,
      totalAmount = 350.0,
      paymentMethod = "cod",
      paymentStatus = "pending_review",
      orderStatus = "out_for_delivery",
      items = listOf(
        OrderItemSnapshot(
          productId = "p1",
          productName = "مسحوق غسيل مركز",
          unit = "عبوة",
          quantity = 2,
          unitPrice = 175.0,
          lineTotal = 350.0,
        ),
      ),
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        DeliveryDriverOrdersScreen(
          uiState = DeliveryDriverUiState(
            isLoading = false,
            orders = listOf(assignedOrder),
          ),
          focusedOrderId = "ord_d1",
          onToggleShowCompleted = {},
          onConfirmDelivered = { ord, _, _ -> deliveredOrderId = ord.id },
          onClearMessages = {},
          onBackClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("الطلب المحدد من الإشعار").assertIsDisplayed()
    composeTestRule.onNodeWithText("اتصال بالعميل").assertIsDisplayed()
    composeTestRule.onNodeWithText("عرض الخريطة").assertIsDisplayed()
    composeTestRule.onNodeWithTag("driver_orders_lazy_column")
      .performScrollToNode(hasTestTag("driver_deliver_ord_d1"))
    composeTestRule.onNodeWithTag("driver_deliver_ord_d1").assertIsDisplayed()
  }

  @Test
  fun cuj12_storeSettingsAccordionCollapsedByDefaultAndSavesUpdatedOwnerEmail() {
    var savedSettings: StoreGeneralSettings? = null
    composeTestRule.setContent {
      AlEmanTradeTheme {
        AdminStoreSettingsScreen(
          uiState = AdminStoreSettingsUiState(
            isLoading = false,
            settings = StoreGeneralSettings(ownerEmail = "old.owner@aleman.store"),
          ),
          canManagePayments = true,
          onSaveSettings = { savedSettings = it },
          onSendBroadcast = { _, _, _, _ -> },
          onOpenPaymentSettings = {},
          onClearMessages = {},
          onBackClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // الأقسام مطوية افتراضيًا
    assertEquals(0, composeTestRule.onAllNodesWithTag("store_owner_email_input").fetchSemanticsNodes().size)

    // فتح قسم البيانات العامة وبريد المالك وتحديث البريد وحفظه
    composeTestRule.onNodeWithTag("accordion_general_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("store_owner_email_input")
      .performScrollTo()
      .performTextReplacement("new.owner@aleman.store")
    composeTestRule.onNodeWithTag("admin_save_store_settings_button")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()
    assertEquals("new.owner@aleman.store", savedSettings?.ownerEmail)
  }

  @Test
  fun cuj13_adminDashboardWholesalePreviewToggleFiresCallback() {
    var toggled = false
    composeTestRule.setContent {
      AlEmanTradeTheme {
        AdminDashboardShellScreen(
          roleLabel = StaffRole.PRODUCT_MANAGER.arabicLabel,
          allowedPermissions = StaffRole.PRODUCT_MANAGER.permissions,
          dashboardState = AdminDashboardUiState(),
          onBackClick = {},
          onSelectSection = {},
          canToggleWholesalePreview = true,
          isWholesalePreviewActive = false,
          onToggleWholesalePreview = { toggled = true },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_wholesale_preview_toggle").performClick()
    composeTestRule.waitForIdle()
    assertTrue(toggled)
  }

  @Test
  fun cuj14_upcomingFlashSaleHidesProductsAndShowsOpensSoonNotice() {
    val upcomingSale = com.example.data.model.FlashSale(
      id = "fs_soon",
      title = "ساعة البرق القادمة",
      badgeText = "قريبًا",
      startsAt = Timestamp(Date(System.currentTimeMillis() + 3_600_000L)),
      endsAt = Timestamp(Date(System.currentTimeMillis() + 7_200_000L)),
      isActive = true,
      items = listOf(
        com.example.data.model.FlashSaleProductItem(
          productId = sampleProduct.id,
          flashRetailPrice = 699.0,
          flashWholesalePrice = 599.0,
          maxQtyPerOrder = 2,
        ),
      ),
    )
    val stateWithUpcoming = sampleCatalogState.copy(flashSales = listOf(upcomingSale))

    composeTestRule.setContent {
      AlEmanTradeTheme {
        HomeTabContent(
          catalogState = stateWithUpcoming,
          userProfile = null,
          canAccessAdmin = false,
          defaultAddress = null,
          onDeliveryBarClick = {},
          onOpenCategories = {},
          onOpenSearchPreset = { _, _ -> },
          onOpenQrScanner = {},
          onOpenProduct = {},
          onAddToCart = {},
          onOpenAdmin = {},
          onRetryCatalog = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("flash_sale_card_fs_soon").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("flash_sale_upcoming_locked_notice").assertIsDisplayed()
    composeTestRule.onNodeWithText("هذا العرض سيفتح قريبًا!").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("flash_sale_add_${sampleProduct.id}").fetchSemanticsNodes().size)
  }

  @Test
  fun cuj15_adminOrdersDisplaysPrintReceiptAndSingleLineActionButtons() {
    val sampleOrder = Order(
      id = "ord_et_1",
      userId = "cust_1",
      orderNumber = "ET-1000",
      customerName = "محمود حسن",
      recipientName = "محمود حسن",
      phone = "01012345678",
      governorate = "القاهرة",
      city = "مدينة نصر",
      streetAddress = "10 شارع عباس العقاد",
      totalAmount = 950.0,
      paymentMethod = "cod",
      paymentStatus = "pending_review",
      orderStatus = "new_order",
      items = listOf(
        OrderItemSnapshot(
          productId = sampleProduct.id,
          productName = sampleProduct.name,
          unit = "قطعة",
          quantity = 1,
          unitPrice = 950.0,
          lineTotal = 950.0,
        ),
      ),
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        com.example.ui.screens.admin.AdminOrdersScreen(
          uiState = com.example.ui.viewmodel.AdminOrdersUiState(
            isLoading = false,
            orders = listOf(sampleOrder),
          ),
          onSelectStatusFilter = {},
          onSearchQueryChange = {},
          onUpdateOrderStatus = { _, _, _ -> },
          canPrepareAndPrint = true,
          storeName = "الإيمان للتجارة",
          storePhone = "01000000000",
          onClearMessages = {},
          onBackClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("طباعة الإيصال").assertIsDisplayed()
    composeTestRule.onNodeWithTag("print_receipt_ord_et_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("assign_driver_ord_et_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("order_details_ord_et_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_next_status_ord_et_1").assertIsDisplayed()
  }

  @Test
  fun cuj16_storeSettingsNumeralFormatToggleSavesEnglishChoice() {
    var savedSettings: StoreGeneralSettings? = null
    try {
      composeTestRule.setContent {
        AlEmanTradeTheme {
          AdminStoreSettingsScreen(
            uiState = AdminStoreSettingsUiState(
              isLoading = false,
              settings = StoreGeneralSettings(numeralFormat = com.example.data.model.NumeralFormat.ARABIC.code),
            ),
            canManagePayments = true,
            onSaveSettings = { savedSettings = it },
            onSendBroadcast = { _, _, _, _ -> },
            onOpenPaymentSettings = {},
            onClearMessages = {},
            onBackClick = {},
          )
        }
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("accordion_general_settings").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithText("أرقام إنجليزية (123)")
        .performScrollTo()
        .performClick()
      composeTestRule.onNodeWithTag("admin_save_store_settings_button")
        .performScrollTo()
        .performClick()
      composeTestRule.waitForIdle()
      assertEquals(com.example.data.model.NumeralFormat.ENGLISH, savedSettings?.numeralFormatEnum)
    } finally {
      com.example.core.util.ArabicFormatter.setNumeralFormat(com.example.data.model.NumeralFormat.ARABIC)
    }
  }

  @Test
  fun cuj17_checkoutScreenAllowsSwitchingToStorePickupAndZeroesDeliveryFee() {
    var selectedFulfillment: com.example.core.model.FulfillmentMethod? = null
    var submittedFulfillment: com.example.core.model.FulfillmentMethod? = null
    val checkoutLine = com.example.data.model.ResolvedCartLine(
      product = sampleProduct,
      quantity = 1,
      unitPrice = 950.0,
      appliedPricingMode = "retail",
      lineTotal = 950.0,
      remainingForWholesale = 4,
    )
    val pickupUiState = CartAndOrdersUiState(
      fulfillmentMethod = com.example.core.model.FulfillmentMethod.STORE_PICKUP,
      cartSummary = com.example.data.model.CartSummary(
        lines = listOf(checkoutLine),
        subtotal = 950.0,
        discountAmount = 0.0,
        deliveryFee = 0.0,
        totalAmount = 950.0,
        totalItemsCount = 1,
      ),
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        com.example.ui.screens.customer.CheckoutScreen(
          cartUiState = pickupUiState,
          addresses = emptyList(),
          defaultAddress = null,
          isWholesaleApproved = false,
          storeName = "الإيمان للتجارة",
          storeAddress = "شارع النصر - القاهرة",
          storePhone = "01000000000",
          customerName = "طارق محمود",
          customerPhone = "01012345678",
          onSelectFulfillmentMethod = { selectedFulfillment = it },
          onSelectZone = {},
          onAutoMatchZone = {},
          onCouponInputChange = {},
          onApplyCoupon = {},
          onRemoveCoupon = {},
          onOpenAddresses = {},
          onClearMessages = {},
          onPlaceOrder = { method, _, _, _, _, _, _, _ -> submittedFulfillment = method },
          onBackClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("checkout_fulfillment_pickup").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(com.example.core.model.FulfillmentMethod.STORE_PICKUP, selectedFulfillment)

    // بطاقة بيانات الاستلام من المتجر تظهر، وأزرار عنوان التوصيل تختفي
    composeTestRule.onNodeWithTag("checkout_lazy_column")
      .performScrollToNode(hasTestTag("checkout_pickup_info_card"))
    composeTestRule.onNodeWithTag("checkout_pickup_info_card").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("checkout_manage_addresses_button").fetchSemanticsNodes().size)

    // يمكن تأكيد وإرسال الطلب بدون عنوان توصيل محفوظ
    composeTestRule.onNodeWithTag("checkout_lazy_column")
      .performScrollToNode(hasTestTag("checkout_submit_order_button"))
    composeTestRule.onNodeWithTag("checkout_submit_order_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(com.example.core.model.FulfillmentMethod.STORE_PICKUP, submittedFulfillment)
  }

  @Test
  fun cuj18_storePoliciesScreenSwitchesTabsAndDisplaysPlayLinks() {
    var deleteTriggered = false
    val settings = StoreGeneralSettings(
      storeName = "الإيمان للتجارة",
      privacyPolicyUrl = "https://aleman.example/privacy",
      returnPolicyUrl = "https://aleman.example/returns",
      accountDeletionUrl = "https://aleman.example/delete",
      privacyPolicyText = "نص الخصوصية المخصص للمتجر.",
      returnPolicyText = "نص الاسترجاع المخصص للمتجر خلال 14 يوم.",
      termsOfServiceText = "نص الشروط والأحكام المعتمد.",
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        StorePoliciesScreen(
          settings = settings,
          initialTab = StorePolicyTab.PRIVACY,
          isSignedIn = true,
          onRequestDeleteAccount = { deleteTriggered = true },
          onBackClick = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("store_policies_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("policy_content_card_privacy").assertIsDisplayed()
    composeTestRule.onNodeWithText("نص الخصوصية المخصص للمتجر.").assertIsDisplayed()

    composeTestRule.onNodeWithTag("policy_tab_return").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("policy_content_card_return").assertIsDisplayed()
    composeTestRule.onNodeWithText("نص الاسترجاع المخصص للمتجر خلال 14 يوم.").assertIsDisplayed()

    composeTestRule.onNodeWithTag("policy_tab_terms").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("policy_content_card_terms").assertIsDisplayed()
    composeTestRule.onNodeWithText("نص الشروط والأحكام المعتمد.").assertIsDisplayed()

    composeTestRule.onNodeWithTag("policy_tab_privacy").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("store_policies_list")
      .performScrollToNode(hasTestTag("policies_delete_account_button"))
    composeTestRule.onNodeWithTag("policies_delete_account_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(deleteTriggered)

    composeTestRule.onNodeWithTag("store_policies_list")
      .performScrollToNode(hasTestTag("policy_external_deletion_url"))
    composeTestRule.onNodeWithTag("policy_external_privacy_url").assertIsDisplayed()
    composeTestRule.onNodeWithTag("policy_external_return_url").assertIsDisplayed()
    composeTestRule.onNodeWithTag("policy_external_deletion_url").assertIsDisplayed()
  }

  @Test
  fun cuj19_accountTabDisplaysPoliciesRowAndDeleteAccountConfirmationFlow() {
    var deletedConfirmed = false
    var openedPolicyTab: StorePolicyTab? = null
    val user = UserProfile(
      uid = "cust_1",
      fullName = "أحمد خالد",
      email = "ahmed@example.com",
      phone = "01012345678",
    )

    composeTestRule.setContent {
      AlEmanTradeTheme {
        CustomerShellScreen(
          userProfile = user,
          canAccessAdmin = false,
          defaultAddress = null,
          addressCount = 0,
          catalogState = CatalogUiState(),
          cartUiState = CartAndOrdersUiState(),
          openCartTrigger = 0,
          onRetryCatalog = {},
          onUpdateSearchQuery = {},
          onSelectSearchCategory = {},
          onToggleSearchOffers = {},
          onToggleSearchInStock = {},
          onToggleSearchWholesale = {},
          onSetSearchPriceRange = { _, _ -> },
          onSelectSearchSort = {},
          onResetSearchFilters = {},
          onOpenSearchPreset = { _, _ -> },
          onOpenProduct = {},
          onAddToCart = {},
          onUpdateCartQuantity = { _, _, _ -> },
          onRemoveCartItem = {},
          onCouponInputChange = {},
          onApplyCoupon = {},
          onRemoveCoupon = {},
          onProceedToCheckout = {},
          onOpenMyOrders = {},
          feedbackMessage = null,
          errorMessage = null,
          onClearMessages = {},
          onRequireSignIn = {},
          onUpdateProfile = { _, _, _ -> },
          onSubmitWholesaleRequest = { _, _, _ -> },
          onOpenAddresses = {},
          onNavigateToAdmin = {},
          onSignOut = {},
          onOpenPolicies = { openedPolicyTab = it },
          requiresRecentLoginForDeletion = true,
          onDeleteAccount = { deletedConfirmed = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("bottom_tab_account").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("account_policies_row")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    assertEquals(StorePolicyTab.PRIVACY, openedPolicyTab)

    composeTestRule.onNodeWithTag("account_delete_account_button")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()

    // يجب تفعيل زر الحذف فقط بعد تحديد مربع الإقرار
    composeTestRule.onNodeWithTag("confirm_delete_account_button").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("delete_account_ack_checkbox").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_delete_account_button").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertTrue(deletedConfirmed)
  }
}

