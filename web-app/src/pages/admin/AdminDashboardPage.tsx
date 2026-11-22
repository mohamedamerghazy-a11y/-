import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  BarChart3,
  Bolt,
  CheckCircle2,
  CreditCard,
  LayoutGrid,
  LayoutDashboard,
  MapPin,
  Package,
  Settings,
  Shield,
  ShoppingBag,
  Sparkles,
  Star,
  Store,
  ArrowLeftRight,
  Ticket,
  Truck,
  Users,
  View,
  Warehouse,
} from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import type {
  Banner,
  BrandShowcase,
  CashVerificationLog,
  Category,
  Coupon,
  DeliveryZone,
  FlashSale,
  Order,
  Product,
  ProductReview,
  SearchMiss,
  StaffMember,
  StaffPermission,
  StoreCompany,
  UserProfile,
} from '../../types/models';
import {
  subscribeAllCoupons,
  subscribeAllOrders,
  subscribeAllReviews,
  subscribeAllStaff,
  subscribeAllStockAlertCounts,
  subscribeAllUsers,
  subscribeBanners,
  subscribeBrandShowcases,
  subscribeCashVerificationLogs,
  subscribeCategories,
  subscribeCompanies,
  subscribeDeliveryZones,
  subscribeDriverOrders,
  subscribeFlashSales,
  subscribeProducts,
  subscribeSearchMisses,
} from '../../services/firestoreService';
import { formatCurrency, formatNumber } from '../../lib/formatters';
import {
  AdminCategoriesTab,
  AdminInventoryTab,
  AdminOffersTab,
  AdminProductsTab,
} from './AdminCatalogTabs';
import {
  AdminCouponsTab,
  AdminDeliveryZonesTab,
  AdminDriverTab,
  AdminOrdersTab,
} from './AdminOrdersAndDeliveryTabs';
import {
  AdminBannersTab,
  AdminCustomersTab,
  AdminFlashSalesTab,
  AdminStaffTab,
} from './AdminMarketingAndCustomersTabs';
import {
  AdminPaymentsTab,
  AdminReportsTab,
  AdminReviewsTab,
  AdminStoreSettingsTab,
} from './AdminFinanceAndSettingsTabs';
import { AdminBrandShowcasesTab } from './AdminBrandShowcasesTab';

type AdminTabKey =
  | 'overview'
  | 'orders'
  | 'driver'
  | 'payments'
  | 'products'
  | 'categories'
  | 'inventory'
  | 'customers'
  | 'staff'
  | 'flash_sales'
  | 'offers'
  | 'banners'
  | 'brand_showcases'
  | 'coupons'
  | 'delivery_zones'
  | 'reviews'
  | 'reports'
  | 'settings';

export const AdminDashboardPage: React.FC<{ initialSection?: StaffPermission }> = ({ initialSection }) => {
  const {
    firebaseUser,
    canAccessAdmin,
    effectivePermissions,
    effectiveRoleLabel,
    canToggleWholesalePreview,
    isWholesalePreviewActive,
    toggleWholesalePreview,
    storeSettings,
    paymentSettings,
    navigate,
  } = useStore();

  const [activeTab, setActiveTab] = useState<AdminTabKey>('overview');

  const [allOrders, setAllOrders] = useState<Order[]>([]);
  const [driverOrders, setDriverOrders] = useState<Order[]>([]);
  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [allCategories, setAllCategories] = useState<Category[]>([]);
  const [allCompanies, setAllCompanies] = useState<StoreCompany[]>([]);
  const [allBanners, setAllBanners] = useState<Banner[]>([]);
  const [allBrandShowcases, setAllBrandShowcases] = useState<BrandShowcase[]>([]);
  const [allFlashSales, setAllFlashSales] = useState<FlashSale[]>([]);
  const [allZones, setAllZones] = useState<DeliveryZone[]>([]);
  const [allCoupons, setAllCoupons] = useState<Coupon[]>([]);
  const [allUsers, setAllUsers] = useState<UserProfile[]>([]);
  const [allStaff, setAllStaff] = useState<StaffMember[]>([]);
  const [allReviews, setAllReviews] = useState<ProductReview[]>([]);
  const [cashLogs, setCashLogs] = useState<CashVerificationLog[]>([]);
  const [stockAlertCounts, setStockAlertCounts] = useState<Record<string, number>>({});
  const [searchMisses, setSearchMisses] = useState<SearchMiss[]>([]);

  useEffect(() => {
    if (!canAccessAdmin || !firebaseUser) return;
    const unsubs: Array<() => void> = [];

    if (
      effectivePermissions.has('manage_orders') ||
      effectivePermissions.has('manage_payments') ||
      effectivePermissions.has('manage_delivery') ||
      effectivePermissions.has('view_reports') ||
      effectivePermissions.has('view_sales_summary')
    ) {
      unsubs.push(subscribeAllOrders(setAllOrders));
    }
    if (effectivePermissions.has('deliver_orders')) {
      unsubs.push(subscribeDriverOrders(firebaseUser.uid, setDriverOrders));
    }
    if (
      effectivePermissions.has('manage_products') ||
      effectivePermissions.has('manage_inventory') ||
      effectivePermissions.has('manage_offers') ||
      effectivePermissions.has('view_reports')
    ) {
      unsubs.push(subscribeProducts(true, setAllProducts));
    } else {
      unsubs.push(subscribeProducts(false, setAllProducts));
    }
    if (effectivePermissions.has('manage_categories') || effectivePermissions.has('manage_products')) {
      unsubs.push(subscribeCategories(true, setAllCategories));
    } else {
      unsubs.push(subscribeCategories(false, setAllCategories));
    }
    if (effectivePermissions.has('manage_banners')) {
      unsubs.push(subscribeBanners(true, setAllBanners));
    }
    if (effectivePermissions.has('manage_banners') || effectivePermissions.has('manage_products')) {
      unsubs.push(subscribeBrandShowcases(true, setAllBrandShowcases));
    }
    if (
      effectivePermissions.has('manage_store_settings') ||
      effectivePermissions.has('manage_products') ||
      effectivePermissions.has('manage_banners')
    ) {
      unsubs.push(subscribeCompanies(true, setAllCompanies));
    } else {
      unsubs.push(subscribeCompanies(false, setAllCompanies));
    }
    if (effectivePermissions.has('manage_flash_sales') || effectivePermissions.has('manage_offers')) {
      unsubs.push(subscribeFlashSales(true, setAllFlashSales));
    }
    if (effectivePermissions.has('manage_delivery')) {
      unsubs.push(subscribeDeliveryZones(true, setAllZones));
    }
    if (effectivePermissions.has('manage_coupons')) {
      unsubs.push(subscribeAllCoupons(setAllCoupons));
    }
    if (
      effectivePermissions.has('manage_customers') ||
      effectivePermissions.has('approve_wholesale') ||
      effectivePermissions.has('manage_staff') ||
      effectivePermissions.has('manage_store_settings')
    ) {
      unsubs.push(subscribeAllUsers(setAllUsers));
    }
    if (effectivePermissions.has('manage_staff') || effectivePermissions.has('manage_orders') || effectivePermissions.has('manage_delivery')) {
      unsubs.push(subscribeAllStaff(setAllStaff));
    }
    if (effectivePermissions.has('manage_reviews')) {
      unsubs.push(subscribeAllReviews(setAllReviews));
    }
    if (effectivePermissions.has('manage_payments')) {
      unsubs.push(subscribeCashVerificationLogs(setCashLogs));
    }
    if (effectivePermissions.has('manage_products') || effectivePermissions.has('manage_inventory')) {
      unsubs.push(subscribeAllStockAlertCounts(setStockAlertCounts));
    }
    if (effectivePermissions.has('view_reports')) {
      unsubs.push(subscribeSearchMisses(setSearchMisses));
    }

    return () => unsubs.forEach((u) => u());
  }, [canAccessAdmin, firebaseUser, effectivePermissions]);

  const navItems = useMemo(() => {
    const items: Array<{ key: AdminTabKey; label: string; icon: React.ReactNode; perm?: StaffPermission }> = [
      { key: 'overview', label: 'ملخص اللوحة', icon: <LayoutDashboard className="w-4 h-4" /> },
      { key: 'orders', label: 'إدارة الطلبات', icon: <ShoppingBag className="w-4 h-4" />, perm: 'manage_orders' },
      { key: 'driver', label: 'بوابة المندوب', icon: <Truck className="w-4 h-4" />, perm: 'deliver_orders' },
      { key: 'payments', label: 'المدفوعات والإيصالات', icon: <CreditCard className="w-4 h-4" />, perm: 'manage_payments' },
      { key: 'products', label: 'المنتجات', icon: <Package className="w-4 h-4" />, perm: 'manage_products' },
      { key: 'categories', label: 'الأقسام والفئات', icon: <LayoutGrid className="w-4 h-4" />, perm: 'manage_categories' },
      { key: 'inventory', label: 'المخزون والتنبيهات', icon: <Warehouse className="w-4 h-4" />, perm: 'manage_inventory' },
      { key: 'customers', label: 'العملاء والجملة', icon: <Users className="w-4 h-4" />, perm: 'manage_customers' },
      { key: 'staff', label: 'الموظفون والصلاحيات', icon: <Shield className="w-4 h-4" />, perm: 'manage_staff' },
      { key: 'flash_sales', label: 'الفلاش سيل', icon: <Bolt className="w-4 h-4" />, perm: 'manage_flash_sales' },
      { key: 'offers', label: 'العروض والتمييز', icon: <Sparkles className="w-4 h-4" />, perm: 'manage_offers' },
      { key: 'banners', label: 'البنرات الإعلانية', icon: <View className="w-4 h-4" />, perm: 'manage_banners' },
      { key: 'brand_showcases', label: 'منتجات الشركات', icon: <Store className="w-4 h-4" />, perm: 'manage_banners' },
      { key: 'coupons', label: 'الكوبونات', icon: <Ticket className="w-4 h-4" />, perm: 'manage_coupons' },
      { key: 'delivery_zones', label: 'مناطق التوصيل', icon: <MapPin className="w-4 h-4" />, perm: 'manage_delivery' },
      { key: 'reviews', label: 'التقييمات', icon: <Star className="w-4 h-4" />, perm: 'manage_reviews' },
      { key: 'reports', label: 'التقارير والأرباح', icon: <BarChart3 className="w-4 h-4" />, perm: 'view_reports' },
      { key: 'settings', label: 'إعدادات المتجر', icon: <Settings className="w-4 h-4" />, perm: 'manage_store_settings' },
    ];
    return items.filter((item) => {
      if (!item.perm) return true;
      if (item.key === 'customers') {
        return effectivePermissions.has('manage_customers') || effectivePermissions.has('approve_wholesale');
      }
      if (item.key === 'brand_showcases') {
        return effectivePermissions.has('manage_banners') || effectivePermissions.has('manage_products');
      }
      return effectivePermissions.has(item.perm);
    });
  }, [effectivePermissions]);

  useEffect(() => {
    if (!initialSection) return;
    const matched = navItems.find((i) => i.perm === initialSection);
    if (matched) setActiveTab(matched.key);
  }, [initialSection, navItems]);

  if (!canAccessAdmin) {
    return (
      <div className="max-w-lg mx-auto px-4 py-14 text-center space-y-4">
        <Shield className="w-12 h-12 mx-auto text-[#E53935]" />
        <h2 className="text-lg font-black">هذه اللوحة مخصصة للإدارة والموظفين فقط</h2>
        <button
          type="button"
          onClick={() => navigate({ name: 'home' })}
          className="px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
        >
          العودة للمتجر
        </button>
      </div>
    );
  }

  const todaySales = useMemo(() => {
    const startOfDay = new Date().setHours(0, 0, 0, 0);
    return allOrders
      .filter((o) => o.orderStatus !== 'cancelled' && o.orderStatus !== 'returned')
      .filter((o) => {
        const ts = o.createdAt && 'toDate' in o.createdAt ? o.createdAt.toDate().getTime() : 0;
        return ts >= startOfDay;
      })
      .reduce((s, o) => s + o.totalAmount, 0);
  }, [allOrders]);

  const pendingOrdersCount = allOrders.filter(
    (o) => o.orderStatus === 'new_order' || o.orderStatus === 'under_review',
  ).length;
  const lowStockCount = allProducts.filter((p) => p.stockQuantity <= p.lowStockThreshold).length;
  const pendingWholesaleCount = allUsers.filter((u) => u.wholesaleStatus === 'pending').length;

  return (
    <div className="max-w-7xl mx-auto px-3 sm:px-6 py-5 space-y-5">
      {/* Admin Top Header */}
      <div className="bg-[#181A20] text-white rounded-3xl p-5 flex flex-wrap items-center justify-between gap-4 border border-[#FFD000]/30">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => navigate({ name: 'home' })}
            className="w-10 h-10 rounded-xl bg-white/10 hover:bg-white/20 flex items-center justify-center"
          >
            <ArrowRight className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-lg sm:text-xl font-black text-[#FFD000]">لوحة إدارة متجر الإيمان</h1>
            <p className="text-xs text-white/75">
              {effectiveRoleLabel} • {formatNumber(effectivePermissions.size)} صلاحية مفعلة • مزامنة حية مع الأندرويد
            </p>
          </div>
        </div>

        {canToggleWholesalePreview && (
          <button
            type="button"
            onClick={toggleWholesalePreview}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-black transition ${
              isWholesalePreviewActive ? 'bg-[#FFD000] text-[#181A20]' : 'bg-white/10 text-white hover:bg-white/20'
            }`}
          >
            <ArrowLeftRight className="w-4 h-4" />
            <span>{isWholesalePreviewActive ? 'معاينة المتجر: جملة' : 'معاينة المتجر: قطاعي'}</span>
          </button>
        )}
      </div>

      {/* Main Split: Sidebar Tabs + Content */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
        {/* Sidebar / Horizontal Tabs on mobile */}
        <aside className="lg:col-span-3 bg-white border border-[#EBECEF] rounded-2xl p-2 flex lg:flex-col gap-1 overflow-x-auto lg:sticky lg:top-20">
          {navItems.map((item) => {
            const active = activeTab === item.key;
            return (
              <button
                key={item.key}
                type="button"
                onClick={() => setActiveTab(item.key)}
                className={`whitespace-nowrap flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl text-xs font-bold transition text-right ${
                  active
                    ? 'bg-[#181A20] text-[#FFD000]'
                    : 'text-[#181A20] hover:bg-[#FFF9DB]'
                }`}
              >
                {item.icon}
                <span>{item.label}</span>
              </button>
            );
          })}
        </aside>

        {/* Active Panel */}
        <main className="lg:col-span-9 space-y-4">
          {activeTab === 'overview' && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="bg-white border border-[#EBECEF] rounded-2xl p-4">
                  <div className="text-xs text-[#717784] font-bold">مبيعات اليوم</div>
                  <div className="text-xl font-black mt-1">{formatCurrency(todaySales)}</div>
                </div>
                <div className="bg-white border border-[#EBECEF] rounded-2xl p-4">
                  <div className="text-xs text-[#717784] font-bold">طلبات جديدة</div>
                  <div className="text-xl font-black text-[#1565C0] mt-1">{formatNumber(pendingOrdersCount)}</div>
                </div>
                <div className="bg-white border border-[#EBECEF] rounded-2xl p-4">
                  <div className="text-xs text-[#717784] font-bold">نواقص المخزون</div>
                  <div className="text-xl font-black text-[#E53935] mt-1">{formatNumber(lowStockCount)}</div>
                </div>
                <div className="bg-white border border-[#EBECEF] rounded-2xl p-4">
                  <div className="text-xs text-[#717784] font-bold">طلبات جملة معلقة</div>
                  <div className="text-xl font-black text-amber-600 mt-1">{formatNumber(pendingWholesaleCount)}</div>
                </div>
              </div>

              <div className="bg-white border border-[#EBECEF] rounded-2xl p-5">
                <h2 className="font-black text-sm mb-3">الأقسام المتاحة لصلاحياتك</h2>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                  {navItems
                    .filter((i) => i.key !== 'overview')
                    .map((item) => (
                      <button
                        key={item.key}
                        type="button"
                        onClick={() => setActiveTab(item.key)}
                        className="p-3.5 rounded-2xl bg-[#F9FAFB] border border-[#EBECEF] hover:border-[#FFD000] flex items-center gap-2.5 text-xs font-extrabold text-right"
                      >
                        <div className="w-8 h-8 rounded-xl bg-[#FFF9DB] text-[#181A20] flex items-center justify-center shrink-0">
                          {item.icon}
                        </div>
                        <span>{item.label}</span>
                      </button>
                    ))}
                </div>
              </div>

              <div className="bg-[#E8F6EE] border border-[#219653]/30 rounded-2xl p-4 flex items-center gap-3 text-xs font-bold text-[#219653]">
                <CheckCircle2 className="w-5 h-5 shrink-0" />
                <span>
                  كل تعديل تقوم به هنا (منتجات، أسعار، فلاش سيل، بنرات، طلبات، إعدادات) ينعكس فورًا على تطبيق الأندرويد والعكس.
                </span>
              </div>
            </div>
          )}

          {activeTab === 'orders' && <AdminOrdersTab orders={allOrders} staffList={allStaff} />}
          {activeTab === 'driver' && <AdminDriverTab driverOrders={driverOrders} />}
          {activeTab === 'payments' && <AdminPaymentsTab orders={allOrders} paymentSettings={paymentSettings} cashLogs={cashLogs} />}
          {activeTab === 'products' && (
            <AdminProductsTab
              products={allProducts}
              categories={allCategories}
              companies={allCompanies}
              stockAlertCounts={stockAlertCounts}
            />
          )}
          {activeTab === 'categories' && <AdminCategoriesTab categories={allCategories} />}
          {activeTab === 'inventory' && (
            <AdminInventoryTab products={allProducts} stockAlertCounts={stockAlertCounts} />
          )}
          {activeTab === 'customers' && (
            <AdminCustomersTab
              users={allUsers}
              canApproveWholesale={effectivePermissions.has('approve_wholesale')}
            />
          )}
          {activeTab === 'staff' && <AdminStaffTab users={allUsers} staffList={allStaff} />}
          {activeTab === 'flash_sales' && <AdminFlashSalesTab flashSales={allFlashSales} products={allProducts} />}
          {activeTab === 'offers' && <AdminOffersTab products={allProducts} />}
          {activeTab === 'banners' && (
            <AdminBannersTab banners={allBanners} categories={allCategories} products={allProducts} />
          )}
          {activeTab === 'brand_showcases' && (
            <AdminBrandShowcasesTab
              brandShowcases={allBrandShowcases}
              companies={allCompanies}
              products={allProducts}
              categories={allCategories}
            />
          )}
          {activeTab === 'coupons' && <AdminCouponsTab coupons={allCoupons} />}
          {activeTab === 'delivery_zones' && <AdminDeliveryZonesTab zones={allZones} />}
          {activeTab === 'reviews' && <AdminReviewsTab reviews={allReviews} />}
          {activeTab === 'reports' && (
            <AdminReportsTab
              orders={allOrders}
              products={allProducts}
              searchMisses={searchMisses}
              onOpenProducts={
                effectivePermissions.has('manage_products') ? () => setActiveTab('products') : undefined
              }
            />
          )}
          {activeTab === 'settings' && (
            <AdminStoreSettingsTab
              storeSettings={storeSettings}
              users={allUsers}
              companies={allCompanies}
            />
          )}
        </main>
      </div>
    </div>
  );
};
