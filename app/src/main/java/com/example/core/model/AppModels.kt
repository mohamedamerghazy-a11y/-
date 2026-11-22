package com.example.core.model

/**
 * نوع حساب العميل المطلوب أو المعتمد.
 */
enum class AccountType(val firestoreKey: String, val arabicLabel: String) {
  RETAIL("retail", "قطاعي"),
  WHOLESALE("wholesale", "جملة"),
}

/**
 * حالة طلب تحويل الحساب إلى حساب جملة.
 */
enum class WholesaleStatus(val firestoreKey: String, val arabicLabel: String) {
  NONE("none", "لا يوجد طلب"),
  PENDING("pending", "قيد المراجعة"),
  APPROVED("approved", "معتمد للجملة"),
  REJECTED("rejected", "مرفوض"),
}

/**
 * أدوار الإدارة والموظفين داخل التطبيق.
 */
enum class StaffRole(
  val firestoreKey: String,
  val arabicLabel: String,
  val permissions: Set<StaffPermission>,
) {
  OWNER("owner", "مالك المتجر", StaffPermission.entries.toSet()),
  MANAGER(
    "manager",
    "مدير",
    setOf(
      StaffPermission.VIEW_SALES_SUMMARY,
      StaffPermission.MANAGE_PRODUCTS,
      StaffPermission.MANAGE_CATEGORIES,
      StaffPermission.MANAGE_CUSTOMERS,
      StaffPermission.APPROVE_WHOLESALE,
      StaffPermission.MANAGE_ORDERS,
      StaffPermission.MANAGE_PAYMENTS,
      StaffPermission.MANAGE_INVENTORY,
      StaffPermission.MANAGE_DELIVERY,
      StaffPermission.MANAGE_COUPONS,
      StaffPermission.MANAGE_OFFERS,
      StaffPermission.MANAGE_FLASH_SALES,
      StaffPermission.MANAGE_BANNERS,
      StaffPermission.MANAGE_REVIEWS,
      StaffPermission.VIEW_REPORTS,
      StaffPermission.MANAGE_STORE_SETTINGS,
      StaffPermission.PREVIEW_WHOLESALE_STORE,
    ),
  ),
  PRODUCT_MANAGER(
    "product_manager",
    "مدير المنتجات",
    setOf(
      StaffPermission.MANAGE_PRODUCTS,
      StaffPermission.MANAGE_CATEGORIES,
      StaffPermission.MANAGE_OFFERS,
      StaffPermission.MANAGE_FLASH_SALES,
      StaffPermission.MANAGE_BANNERS,
      StaffPermission.PREVIEW_WHOLESALE_STORE,
    ),
  ),
  ORDER_MANAGER(
    "order_manager",
    "مسؤول الطلبات",
    setOf(
      StaffPermission.MANAGE_ORDERS,
      StaffPermission.MANAGE_DELIVERY,
    ),
  ),
  INVENTORY_MANAGER(
    "inventory_manager",
    "مسؤول المخزون",
    setOf(
      StaffPermission.MANAGE_INVENTORY,
      StaffPermission.MANAGE_PRODUCTS,
    ),
  ),
  ACCOUNTANT(
    "accountant",
    "المحاسب",
    setOf(
      StaffPermission.VIEW_SALES_SUMMARY,
      StaffPermission.MANAGE_PAYMENTS,
      StaffPermission.MANAGE_COUPONS,
      StaffPermission.VIEW_REPORTS,
    ),
  ),
  CUSTOMER_SERVICE(
    "customer_service",
    "خدمة العملاء",
    setOf(
      StaffPermission.MANAGE_CUSTOMERS,
      StaffPermission.MANAGE_ORDERS,
      StaffPermission.MANAGE_REVIEWS,
    ),
  ),
  DELIVERY_DRIVER(
    "delivery_driver",
    "موظف توصيل",
    setOf(
      StaffPermission.DELIVER_ORDERS,
    ),
  ),
  CUSTOMER("customer", "عميل", emptySet());

  companion object {
    fun fromKey(key: String?): StaffRole {
      return entries.firstOrNull { it.firestoreKey == key } ?: CUSTOMER
    }
  }
}

enum class StaffPermission(val firestoreKey: String, val arabicTitle: String) {
  VIEW_SALES_SUMMARY("view_sales_summary", "ملخص المبيعات"),
  MANAGE_PRODUCTS("manage_products", "إدارة المنتجات"),
  MANAGE_CATEGORIES("manage_categories", "إدارة الأقسام"),
  MANAGE_CUSTOMERS("manage_customers", "إدارة العملاء"),
  APPROVE_WHOLESALE("approve_wholesale", "اعتماد حسابات الجملة"),
  MANAGE_ORDERS("manage_orders", "إدارة الطلبات"),
  MANAGE_PAYMENTS("manage_payments", "إدارة المدفوعات"),
  MANAGE_INVENTORY("manage_inventory", "إدارة المخزون"),
  MANAGE_DELIVERY("manage_delivery", "إدارة التوصيل"),
  MANAGE_COUPONS("manage_coupons", "إدارة الكوبونات"),
  MANAGE_OFFERS("manage_offers", "إدارة العروض"),
  MANAGE_FLASH_SALES("manage_flash_sales", "إدارة الفلاش سيل"),
  MANAGE_BANNERS("manage_banners", "إدارة البنرات"),
  MANAGE_REVIEWS("manage_reviews", "إدارة التقييمات"),
  VIEW_REPORTS("view_reports", "التقارير والإحصائيات"),
  MANAGE_STAFF("manage_staff", "إدارة الموظفين والصلاحيات"),
  MANAGE_STORE_SETTINGS("manage_store_settings", "إعدادات المتجر"),
  PREVIEW_WHOLESALE_STORE("preview_wholesale_store", "معاينة أسعار الجملة والقطاعي"),
  DELIVER_ORDERS("deliver_orders", "توصيل الطلبات (المندوب)");

  companion object {
    fun fromKey(key: String?): StaffPermission? {
      return entries.firstOrNull { it.firestoreKey == key }
    }
  }
}
