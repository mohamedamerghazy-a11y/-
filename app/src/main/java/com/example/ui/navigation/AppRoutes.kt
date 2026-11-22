package com.example.ui.navigation

import kotlinx.serialization.Serializable

/**
 * مفاتيح التوجيه الآمنة بالأنواع (Type-Safe Navigation Routes).
 */
sealed interface AppRoute {
  @Serializable
  data object Splash : AppRoute

  @Serializable
  data object CustomerShell : AppRoute

  @Serializable
  data object SignIn : AppRoute

  @Serializable
  data object Addresses : AppRoute

  @Serializable
  data class ProductDetails(val productId: String) : AppRoute

  @Serializable
  data object Wishlist : AppRoute

  @Serializable
  data object Checkout : AppRoute

  @Serializable
  data object MyOrders : AppRoute

  @Serializable
  data class OrderDetails(val orderId: String) : AppRoute

  @Serializable
  data object Notifications : AppRoute

  @Serializable
  data object AdminDashboard : AppRoute

  @Serializable
  data object AdminCategories : AppRoute

  @Serializable
  data object AdminProducts : AppRoute

  @Serializable
  data object AdminStaff : AppRoute

  @Serializable
  data object AdminCustomers : AppRoute

  @Serializable
  data object AdminOrders : AppRoute

  @Serializable
  data object AdminInventory : AppRoute

  @Serializable
  data object AdminReports : AppRoute

  @Serializable
  data object AdminCoupons : AppRoute

  @Serializable
  data object AdminDeliveryZones : AppRoute

  @Serializable
  data object AdminPayments : AppRoute

  @Serializable
  data object AdminBanners : AppRoute

  @Serializable
  data object AdminReviews : AppRoute

  @Serializable
  data object AdminOffers : AppRoute

  @Serializable
  data object AdminFlashSales : AppRoute

  @Serializable
  data class DeliveryDriverOrders(val focusedOrderId: String? = null) : AppRoute

  @Serializable
  data object AdminStoreSettings : AppRoute

  @Serializable
  data class BrandShowcase(val showcaseId: String) : AppRoute

  @Serializable
  data object AdminBrandShowcases : AppRoute

  @Serializable
  data class StorePolicies(val initialTab: String = "privacy") : AppRoute
}

/**
 * تبويبات واجهة العميل الرئيسية.
 */
enum class CustomerTab(val label: String) {
  HOME("الرئيسية"),
  CATEGORIES("الفئات"),
  SEARCH("البحث"),
  CART("السلة"),
  ACCOUNT("حسابي"),
}
