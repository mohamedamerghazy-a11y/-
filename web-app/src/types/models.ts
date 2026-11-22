import type { Timestamp } from 'firebase/firestore';

export type AccountType = 'retail' | 'wholesale';
export type WholesaleStatus = 'none' | 'pending' | 'approved' | 'rejected';

export type StaffRole =
  | 'owner'
  | 'manager'
  | 'product_manager'
  | 'order_manager'
  | 'inventory_manager'
  | 'accountant'
  | 'customer_service'
  | 'delivery_driver'
  | 'customer';

export type StaffPermission =
  | 'view_sales_summary'
  | 'manage_products'
  | 'manage_categories'
  | 'manage_customers'
  | 'approve_wholesale'
  | 'manage_orders'
  | 'manage_payments'
  | 'manage_inventory'
  | 'manage_delivery'
  | 'manage_coupons'
  | 'manage_offers'
  | 'manage_flash_sales'
  | 'manage_banners'
  | 'manage_reviews'
  | 'view_reports'
  | 'manage_staff'
  | 'manage_store_settings'
  | 'preview_wholesale_store'
  | 'deliver_orders';

export const STAFF_ROLE_META: Record<
  StaffRole,
  { label: string; permissions: StaffPermission[] }
> = {
  owner: {
    label: 'مالك المتجر',
    permissions: [
      'view_sales_summary',
      'manage_products',
      'manage_categories',
      'manage_customers',
      'approve_wholesale',
      'manage_orders',
      'manage_payments',
      'manage_inventory',
      'manage_delivery',
      'manage_coupons',
      'manage_offers',
      'manage_flash_sales',
      'manage_banners',
      'manage_reviews',
      'view_reports',
      'manage_staff',
      'manage_store_settings',
      'preview_wholesale_store',
      'deliver_orders',
    ],
  },
  manager: {
    label: 'مدير',
    permissions: [
      'view_sales_summary',
      'manage_products',
      'manage_categories',
      'manage_customers',
      'approve_wholesale',
      'manage_orders',
      'manage_payments',
      'manage_inventory',
      'manage_delivery',
      'manage_coupons',
      'manage_offers',
      'manage_flash_sales',
      'manage_banners',
      'manage_reviews',
      'view_reports',
      'manage_store_settings',
      'preview_wholesale_store',
    ],
  },
  product_manager: {
    label: 'مدير المنتجات',
    permissions: [
      'manage_products',
      'manage_categories',
      'manage_offers',
      'manage_flash_sales',
      'manage_banners',
      'preview_wholesale_store',
    ],
  },
  order_manager: {
    label: 'مسؤول الطلبات',
    permissions: ['manage_orders', 'manage_delivery'],
  },
  inventory_manager: {
    label: 'مسؤول المخزون',
    permissions: ['manage_inventory', 'manage_products'],
  },
  accountant: {
    label: 'المحاسب',
    permissions: ['view_sales_summary', 'manage_payments', 'manage_coupons', 'view_reports'],
  },
  customer_service: {
    label: 'خدمة العملاء',
    permissions: ['manage_customers', 'manage_orders', 'manage_reviews'],
  },
  delivery_driver: {
    label: 'موظف توصيل',
    permissions: ['deliver_orders'],
  },
  customer: {
    label: 'عميل',
    permissions: [],
  },
};

export const STAFF_PERMISSION_LABELS: Record<StaffPermission, string> = {
  view_sales_summary: 'ملخص المبيعات',
  manage_products: 'إدارة المنتجات',
  manage_categories: 'إدارة الأقسام',
  manage_customers: 'إدارة العملاء',
  approve_wholesale: 'اعتماد حسابات الجملة',
  manage_orders: 'إدارة الطلبات',
  manage_payments: 'إدارة المدفوعات',
  manage_inventory: 'إدارة المخزون',
  manage_delivery: 'إدارة التوصيل',
  manage_coupons: 'إدارة الكوبونات',
  manage_offers: 'إدارة العروض',
  manage_flash_sales: 'إدارة الفلاش سيل',
  manage_banners: 'إدارة البنرات',
  manage_reviews: 'إدارة التقييمات',
  view_reports: 'التقارير والإحصائيات',
  manage_staff: 'إدارة الموظفين والصلاحيات',
  manage_store_settings: 'إعدادات المتجر',
  preview_wholesale_store: 'معاينة أسعار الجملة والقطاعي',
  deliver_orders: 'توصيل الطلبات (المندوب)',
};

export type FulfillmentMethod = 'delivery' | 'pickup';

export type OrderStatus =
  | 'new_order'
  | 'under_review'
  | 'confirmed'
  | 'preparing'
  | 'ready_for_delivery'
  | 'out_for_delivery'
  | 'delivered'
  | 'cancelled'
  | 'returned';

export const ORDER_STATUS_META: Record<
  OrderStatus,
  { label: string; pickupLabel?: string; stepIndex: number }
> = {
  new_order: { label: 'طلب جديد', stepIndex: 0 },
  under_review: { label: 'قيد المراجعة', stepIndex: 1 },
  confirmed: { label: 'تم التأكيد', stepIndex: 2 },
  preparing: { label: 'جاري التجهيز', stepIndex: 3 },
  ready_for_delivery: {
    label: 'جاهز للتوصيل',
    pickupLabel: 'جاهز للاستلام من المتجر',
    stepIndex: 4,
  },
  out_for_delivery: { label: 'خرج مع المندوب', stepIndex: 5 },
  delivered: {
    label: 'تم التوصيل',
    pickupLabel: 'تم الاستلام من المتجر',
    stepIndex: 6,
  },
  cancelled: { label: 'ملغي', stepIndex: -1 },
  returned: { label: 'مرتجع', stepIndex: -1 },
};

export function orderStatusLabelFor(status: OrderStatus, isStorePickup: boolean): string {
  const meta = ORDER_STATUS_META[status] || ORDER_STATUS_META.new_order;
  if (isStorePickup && meta.pickupLabel) return meta.pickupLabel;
  return meta.label;
}

export function activeTimelineStepsFor(isStorePickup: boolean): OrderStatus[] {
  if (isStorePickup) {
    return ['new_order', 'under_review', 'confirmed', 'preparing', 'ready_for_delivery', 'delivered'];
  }
  return [
    'new_order',
    'under_review',
    'confirmed',
    'preparing',
    'ready_for_delivery',
    'out_for_delivery',
    'delivered',
  ];
}

export type PaymentMethod = 'cod' | 'instapay' | 'vodafone_cash';
export type PaymentStatus = 'pending_review' | 'verified' | 'rejected';

export const PAYMENT_METHOD_META: Record<
  PaymentMethod,
  { label: string; subtitle: string; requiresTransferReference: boolean }
> = {
  cod: {
    label: 'الدفع عند الاستلام',
    subtitle: 'ادفع نقدًا للمندوب عند استلام طلبك وفحصه أو عند الاستلام من الفرع.',
    requiresTransferReference: false,
  },
  instapay: {
    label: 'إنستاباي (InstaPay)',
    subtitle: 'تحويل فوري على حساب المتجر مع إدخال الرقم المحوَّل منه وإيصال التحويل.',
    requiresTransferReference: true,
  },
  vodafone_cash: {
    label: 'فودافون كاش',
    subtitle: 'تحويل على محفظة المتجر المعتمدة وإدخال الرقم المحوَّل منه.',
    requiresTransferReference: true,
  },
};

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  pending_review: 'بانتظار مراجعة الدفع',
  verified: 'تم تأكيد الدفع',
  rejected: 'مرفوض — يرجى المراجعة',
};

export type BannerMediaType = 'image' | 'animated_image' | 'video';
export type IntroMediaType = 'video' | 'animated_image';
export type TickerFontWeight = 'light' | 'normal' | 'bold' | 'extra_bold';

export interface TickerInlineImage {
  id: string;
  url: string;
}

export const TICKER_FONT_WEIGHT_OPTIONS: Array<{
  code: TickerFontWeight;
  label: string;
  cssWeight: number;
}> = [
  { code: 'light', label: 'رفيع', cssWeight: 300 },
  { code: 'normal', label: 'عادي', cssWeight: 400 },
  { code: 'bold', label: 'عريض', cssWeight: 700 },
  { code: 'extra_bold', label: 'عريض جدًا', cssWeight: 800 },
];

export type BannerTargetType = 'none' | 'offers' | 'category' | 'product';

export type AppIconTheme = 'default' | 'offers' | 'ramadan' | 'eid';

export const APP_ICON_THEME_OPTIONS: Array<{
  code: AppIconTheme;
  label: string;
  subtitle: string;
  previewSrc: string;
}> = [
  {
    code: 'default',
    label: 'الأيقونة الأساسية',
    subtitle: 'شعار الإيمان للتجارة الافتراضي',
    previewSrc: '/icons/app_icon_default.png',
  },
  {
    code: 'offers',
    label: 'أيقونة العروض',
    subtitle: 'نفس الشعار مع شارة «عروض» بارزة',
    previewSrc: '/icons/app_icon_offers.png',
  },
  {
    code: 'ramadan',
    label: 'أيقونة رمضان',
    subtitle: 'نفس الشعار مع فانوس وهلال رمضان',
    previewSrc: '/icons/app_icon_ramadan.png',
  },
  {
    code: 'eid',
    label: 'أيقونة العيد',
    subtitle: 'نفس الشعار مع بالونات وزينة «عيد سعيد»',
    previewSrc: '/icons/app_icon_eid.png',
  },
];

export type ProductCardBorderStyle =
  | 'default'
  | 'gold'
  | 'emerald'
  | 'royal_blue'
  | 'ruby'
  | 'purple'
  | 'charcoal'
  | 'custom'
  | 'none';

export type ProductCardShape =
  | 'rounded'
  | 'circular'
  | 'rectangle'
  | 'cut_corner'
  | 'asymmetric'
  | 'arch';

export type ProductDiscountBadgeStyle =
  | 'pill'
  | 'ribbon'
  | 'starburst'
  | 'price_tag'
  | 'gold_luxury'
  | 'capsule_outline';

export const PRODUCT_CARD_BORDER_STYLE_OPTIONS: Array<{
  code: ProductCardBorderStyle;
  label: string;
  subtitle: string;
  swatchBackground: string;
}> = [
  { code: 'default', label: 'كلاسيكي ناعم', subtitle: 'إطار رمادي خفيف ونظيف', swatchBackground: 'linear-gradient(135deg,#F3F4F6,#D1D5DB)' },
  { code: 'gold', label: 'إطار ذهبي فاخر', subtitle: 'تدرج ذهبي معدني لامع حول الكارت', swatchBackground: 'linear-gradient(135deg,#F9E58B,#D4AF37,#9C6D11)' },
  { code: 'emerald', label: 'إطار زمردي', subtitle: 'أخضر زمردي أنيق وواضح', swatchBackground: 'linear-gradient(135deg,#34D399,#059669)' },
  { code: 'royal_blue', label: 'إطار أزرق ملكي', subtitle: 'أزرق ملكي متدرج بارز', swatchBackground: 'linear-gradient(135deg,#60A5FA,#1D4ED8)' },
  { code: 'ruby', label: 'إطار ياقوتي', subtitle: 'أحمر عروض جذاب ومتدرج', swatchBackground: 'linear-gradient(135deg,#F87171,#DC2626)' },
  { code: 'purple', label: 'إطار بنفسجي', subtitle: 'بنفسجي عصري مميز', swatchBackground: 'linear-gradient(135deg,#A78BFA,#7C3AED)' },
  { code: 'charcoal', label: 'إطار أسود عصري', subtitle: 'تحديد داكن حاد وأنيق', swatchBackground: 'linear-gradient(135deg,#181A20,#3A3F4B)' },
  { code: 'custom', label: 'لون مخصص', subtitle: 'اختر أي لون خاص لإطار الكارت', swatchBackground: 'conic-gradient(#D4AF37,#E91E63,#1D4ED8,#059669,#D4AF37)' },
  { code: 'none', label: 'بدون إطار', subtitle: 'مظهر ناعم بظل خفيف بدون حدود', swatchBackground: '#FFFFFF' },
];

export const PRODUCT_CARD_SHAPE_OPTIONS: Array<{
  code: ProductCardShape;
  label: string;
  subtitle: string;
}> = [
  { code: 'rounded', label: 'زوايا دائرية ناعمة', subtitle: 'الشكل الكلاسيكي بزوايا مستديرة متوازنة' },
  { code: 'circular', label: 'دائري / بيضاوي ناعم', subtitle: 'حواف دائرية واسعة مع إطار دائري للصورة' },
  { code: 'rectangle', label: 'مستطيل حاد', subtitle: 'خطوط مستقيمة أنيقة بزوايا شبه قائمة' },
  { code: 'cut_corner', label: 'هندسي مشطوف', subtitle: 'زوايا هندسية مشطوفة (مثمن عصري)' },
  { code: 'asymmetric', label: 'هندسي مائل (ورقة)', subtitle: 'زاويتان دائريتان متقابلتان وزاويتان حادتان' },
  { code: 'arch', label: 'قوس علوي فاخر', subtitle: 'قوس علوي بارز يمنح الكارت طابعاً فاخراً' },
];

export const PRODUCT_DISCOUNT_BADGE_STYLE_OPTIONS: Array<{
  code: ProductDiscountBadgeStyle;
  label: string;
  subtitle: string;
}> = [
  { code: 'pill', label: 'كبسولة كلاسيكية', subtitle: 'شارة حمراء مستديرة واضحة بنسبة الخصم' },
  { code: 'ribbon', label: 'شريط ركن مائل', subtitle: 'شريط خصم أنيق على زاوية الصورة' },
  { code: 'starburst', label: 'ختم نجمة العروض', subtitle: 'ختم نجمي هندسي يلفت الانتباه للخصم' },
  { code: 'price_tag', label: 'بطاقة سعر (تاج)', subtitle: 'شكل تاج سعر مميز بحلقة جانبية' },
  { code: 'gold_luxury', label: 'شارة ذهبية فاخرة', subtitle: 'خلفية داكنة بحافة ذهبية وأيقونة بريق' },
  { code: 'capsule_outline', label: 'كبسولة مؤطرة عصرية', subtitle: 'خلفية بيضاء نظيفة بإطار أحمر بارز' },
];

export const PRODUCT_CARD_CUSTOM_SWATCHES = [
  '#D4AF37',
  '#E67E22',
  '#E91E63',
  '#00897B',
  '#0288D1',
  '#5E35B1',
  '#C62828',
  '#2E7D32',
  '#455A64',
  '#8D6E63',
];

export function sanitizeCardHexColor(raw?: string | null, fallback = '#D4AF37'): string {
  const trimmed = (raw || '').trim();
  const normalized = trimmed.startsWith('#') ? trimmed : `#${trimmed}`;
  return /^#[0-9A-Fa-f]{6}$/.test(normalized) ? normalized.toUpperCase() : fallback;
}
export type BannerPlacement =
  | 'top_home'
  | 'below_hero'
  | 'below_categories'
  | 'below_featured'
  | 'below_deals'
  | 'below_newest'
  | 'below_bestsellers'
  | 'below_recommended';

export const BANNER_PLACEMENT_ORDER: BannerPlacement[] = [
  'top_home',
  'below_hero',
  'below_categories',
  'below_featured',
  'below_deals',
  'below_newest',
  'below_bestsellers',
  'below_recommended',
];

export const BANNER_PLACEMENT_LABELS: Record<BannerPlacement, string> = {
  top_home: 'أعلى الرئيسية (الافتراضي)',
  below_hero: 'أسفل بطاقة الترحيب الرئيسية',
  below_categories: 'أسفل شريط الفئات',
  below_featured: 'أسفل المنتجات المميزة',
  below_deals: 'أسفل العروض والخصومات',
  below_newest: 'أسفل المنتجات التي وصلت حديثًا',
  below_bestsellers: 'أسفل الأكثر مبيعًا',
  below_recommended: 'أسفل المنتجات المقترحة',
};

export type BannerStyle = 'normal' | 'slim';

export type HomeCategoryStyle =
  | 'circular'
  | 'rounded_square'
  | 'noon_portrait'
  | 'pill_chips'
  | 'double_grid';

export const HOME_CATEGORY_STYLE_OPTIONS: Array<{
  code: HomeCategoryStyle;
  label: string;
  subtitle: string;
}> = [
  { code: 'circular', label: 'دائري', subtitle: 'أيقونات وصور دائرية متجاورة في شريط أفقي' },
  { code: 'rounded_square', label: 'مربعات ناعمة', subtitle: 'كروت مربعة بزوايا دائرية وإطار أنيق' },
  { code: 'noon_portrait', label: 'طولي حديث', subtitle: 'كروت طولية مائلة بأسلوب متاجر نون' },
  { code: 'pill_chips', label: 'كبسولات أفقية', subtitle: 'أزرار كبسولية مع صورة مصغرة وعدد المنتجات' },
  { code: 'double_grid', label: 'شبكة متكاملة', subtitle: 'شبكة منظمة من أعمدة واضحة داخل الرئيسية' },
];

export type CategoriesTabStyle =
  | 'noon_portrait'
  | 'circular_grid'
  | 'hexagon_grid'
  | 'cut_corner_grid'
  | 'detailed_cards';

export const CATEGORIES_TAB_STYLE_OPTIONS: Array<{
  code: CategoriesTabStyle;
  label: string;
  subtitle: string;
}> = [
  { code: 'noon_portrait', label: 'كروت طولية', subtitle: 'تصميم طولي بحواف دائرية وخلفية ناعمة' },
  { code: 'circular_grid', label: 'دائري', subtitle: 'صور دائرية بإطار ذهبي وأنيق' },
  { code: 'hexagon_grid', label: 'سداسي هندسي', subtitle: 'إطارات سداسية مميزة' },
  { code: 'cut_corner_grid', label: 'هندسي مشطوف', subtitle: 'كروت بزوايا مشطوفة عصرية' },
  { code: 'detailed_cards', label: 'بطاقات أفقية مفصلة', subtitle: 'عرض أفقي يوضح الصورة والوصف وعدد المنتجات' },
];

export type CompanyOfferBadgeStyle =
  | 'gold_ticket'
  | 'crimson_ribbon'
  | 'emerald_glass'
  | 'royal_shield'
  | 'sunburst_stamp';

export const COMPANY_OFFER_BADGE_STYLE_OPTIONS: Array<{
  code: CompanyOfferBadgeStyle;
  label: string;
  subtitle: string;
}> = [
  { code: 'gold_ticket', label: 'تذكرة ذهبية فاخرة', subtitle: 'بطاقة داكنة بحافة ذهبية وشكل تذكرة خصم' },
  { code: 'crimson_ribbon', label: 'شريط عروض ناري', subtitle: 'تدرج أحمر وبرتقالي جذاب مع نسبة خصم واضحة' },
  { code: 'emerald_glass', label: 'شارة زمردية عصرية', subtitle: 'تدرج أخضر زمردي أنيق مع شارة خصم بيضاء' },
  { code: 'royal_shield', label: 'وسام ملكي فاخر', subtitle: 'تدرج ملكي بنفسجي بإطار ذهبي ونجمة مميزة' },
  { code: 'sunburst_stamp', label: 'ختم خصم أصفر بارز', subtitle: 'شارة صفراء جريئة بإطار داكن تلفت الانتباه' },
];

export type NumeralFormat = 'arabic' | 'english';
export type StorePolicyTab = 'privacy' | 'return' | 'terms';
export type StoreContactChannel = 'retail' | 'wholesale' | 'both';
export type NotificationType =
  | 'order_status'
  | 'payment_status'
  | 'wholesale_status'
  | 'offer_promo'
  | 'stock_alert'
  | 'general';

export interface UserProfile {
  id: string;
  uid: string;
  fullName: string;
  email: string;
  phone: string;
  photoUrl?: string;
  accountType: AccountType;
  wholesaleStatus: WholesaleStatus;
  wholesaleCompanyName?: string;
  role: StaffRole;
  lastOrderId?: string;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface StaffMember {
  id: string;
  uid: string;
  fullName: string;
  email: string;
  role: StaffRole;
  permissions: StaffPermission[];
  isActive: boolean;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface DeliveryAddress {
  id: string;
  userId: string;
  label: string;
  recipientName: string;
  phone: string;
  governorate: string;
  city: string;
  streetAddress: string;
  landmark?: string;
  latitude?: number;
  longitude?: number;
  isDefault: boolean;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface Category {
  id: string;
  name: string;
  description?: string;
  parentId?: string;
  imageUrl?: string;
  displayOrder: number;
  isActive: boolean;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  categoryId: string;
  subCategoryId?: string;
  sku: string;
  barcode?: string;
  qrCode?: string;
  imageUrls: string[];
  retailPrice: number;
  retailDiscountPrice?: number;
  wholesalePrice: number;
  wholesaleMinQty: number;
  unit: string;
  stockQuantity: number;
  lowStockThreshold: number;
  isFeatured: boolean;
  isOffer: boolean;
  isActive: boolean;
  companyId?: string;
  brandShowcaseId?: string;
  brandCategoryId?: string;
  exclusiveToBrandShowcase?: boolean;
  salesCount: number;
  averageRating: number;
  reviewCount: number;
  availableColors?: string[];
  availableSizes?: string[];
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface FlashSaleProductItem {
  productId: string;
  flashRetailPrice: number;
  flashWholesalePrice?: number;
  maxQtyPerOrder?: number;
}

export type FlashSaleStatus = 'upcoming' | 'active' | 'ended';

export interface FlashSale {
  id: string;
  title: string;
  badgeText: string;
  startsAt?: Timestamp | Date | null;
  endsAt?: Timestamp | Date | null;
  isActive: boolean;
  items: FlashSaleProductItem[];
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface Banner {
  id: string;
  title: string;
  subtitle?: string;
  imageUrl: string;
  mediaType?: BannerMediaType;
  videoUrl?: string;
  videoMuted?: boolean;
  targetType: BannerTargetType;
  targetId?: string;
  placement: BannerPlacement;
  bannerStyle: BannerStyle;
  durationSeconds: number;
  displayOrder: number;
  isActive: boolean;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface StoreCompany {
  id: string;
  name: string;
  imageUrl?: string;
  displayOrder: number;
  isActive: boolean;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface BrandShowcaseCategory {
  id: string;
  name: string;
  imageUrl: string;
  displayOrder: number;
}

export interface BrandShowcase {
  id: string;
  title: string;
  subtitle?: string;
  companyId?: string;
  companyImageUrl?: string;
  bannerImageUrl?: string;
  discountPercent?: number;
  accentColorHex: string;
  placement: BannerPlacement;
  displayOrder: number;
  isActive: boolean;
  categories: BrandShowcaseCategory[];
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface Coupon {
  id: string;
  code: string;
  discountType: 'percent' | 'fixed';
  discountValue: number;
  minOrderAmount: number;
  maxDiscountAmount?: number;
  usageLimit?: number;
  usedCount: number;
  expiresAt?: Timestamp | Date | null;
  isActive: boolean;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface DeliveryZone {
  id: string;
  name: string;
  governorate: string;
  fee: number;
  estimatedDuration: string;
  isActive: boolean;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface StorePaymentSettings {
  id: string;
  vodafoneCashNumber: string;
  instaPayAddress: string;
  paymentInstructions: string;
  codEnabled: boolean;
  instaPayEnabled: boolean;
  vodafoneCashEnabled: boolean;
  updatedAt?: Timestamp | Date | null;
}

export interface StoreSocialLink {
  title: string;
  url: string;
  channel: StoreContactChannel;
}

export interface StorePhoneEntry {
  label: string;
  phoneNumber: string;
  channel: StoreContactChannel;
}

export interface StoreGeneralSettings {
  id: string;
  storeName: string;
  tagline: string;
  ownerEmail: string;
  supportPhone: string;
  whatsappNumber: string;
  storeAddress: string;
  announcementText: string;
  aboutStoreDescription: string;
  retailFacebookUrl: string;
  wholesaleFacebookUrl: string;
  socialPages: StoreSocialLink[];
  retailPhones: StorePhoneEntry[];
  wholesalePhones: StorePhoneEntry[];
  showFeaturedSection: boolean;
  showDealsSection: boolean;
  showBestsellersSection: boolean;
  showRecommendedSection: boolean;
  showHeroSection: boolean;
  heroBadgeText: string;
  heroTitleText: string;
  heroSubtitleText: string;
  heroCtaText: string;
  homeCategoryStyle: HomeCategoryStyle;
  categoriesTabStyle: CategoriesTabStyle;
  companyOfferBadgeStyle: CompanyOfferBadgeStyle;
  numeralFormat: NumeralFormat;
  privacyPolicyUrl: string;
  returnPolicyUrl: string;
  accountDeletionUrl: string;
  privacyPolicyText: string;
  returnPolicyText: string;
  termsOfServiceText: string;
  showProductColors: boolean;
  showProductSizes: boolean;
  freeShippingEnabled: boolean;
  freeShippingThreshold: number;
  deliveryOtpEnabled: boolean;
  introVideoEnabled: boolean;
  introMediaType: IntroMediaType;
  introVideoUrl: string;
  introImageUrl: string;
  introVideoDurationSeconds: number;
  introVideoTitle: string;
  introVideoMuted: boolean;
  tickerEnabled: boolean;
  tickerText: string;
  tickerPlacement: BannerPlacement;
  tickerBackgroundColor: string;
  tickerBorderColor: string;
  tickerTextColor: string;
  tickerFontSizeSp: number;
  tickerFontWeight: TickerFontWeight;
  tickerItalic: boolean;
  tickerInlineImages: TickerInlineImage[];
  appIconTheme: AppIconTheme;
  productCardBorderStyle: ProductCardBorderStyle;
  productCardBorderColor: string;
  productCardShape: ProductCardShape;
  productDiscountBadgeStyle: ProductDiscountBadgeStyle;
  updatedAt?: Timestamp | Date | null;
}

export interface CashVerificationLog {
  id: string;
  transactionId: string;
  providerName: string;
  senderAddress: string;
  senderPhone: string;
  amount: number;
  matchedOrderId?: string | null;
  matchedOrderNumber?: string | null;
  status: 'matched' | 'unmatched' | 'duplicate' | 'ignored_sender' | string;
  reason: string;
  rawBodyPreview: string;
  verifiedByUid: string;
  createdAt?: Timestamp | Date | null;
}

export interface OrderItemSnapshot {
  productId: string;
  productName: string;
  sku: string;
  imageUrl?: string;
  unit: string;
  pricingMode: 'retail' | 'wholesale' | 'flash_sale' | string;
  flashSaleId?: string;
  selectedColor?: string;
  selectedSize?: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  userId: string;
  orderNumber: string;
  customerName: string;
  customerEmail: string;
  accountTypeAtOrder: AccountType;
  items: OrderItemSnapshot[];
  subtotal: number;
  discountAmount: number;
  couponCode?: string;
  deliveryZoneId?: string;
  deliveryZoneName?: string;
  deliveryFee: number;
  totalAmount: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  transferReference?: string;
  senderPhone?: string;
  receiptImageUrl?: string;
  orderStatus: OrderStatus;
  deliveryLabel: string;
  fulfillmentType: FulfillmentMethod;
  deliveryOtp?: string;
  deliveryOtpVerified?: boolean;
  recipientName: string;
  phone: string;
  governorate: string;
  city: string;
  streetAddress: string;
  landmark?: string;
  latitude?: number;
  longitude?: number;
  assignedDriverId?: string;
  assignedDriverName?: string;
  assignedDriverPhone?: string;
  assignedAt?: Timestamp | Date | null;
  notes?: string;
  statusNote?: string;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface ProductReview {
  id: string;
  productId: string;
  productName: string;
  userId: string;
  userName: string;
  rating: number;
  comment: string;
  isHidden: boolean;
  createdAt?: Timestamp | Date | null;
  updatedAt?: Timestamp | Date | null;
}

export interface AppNotification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: NotificationType;
  referenceId?: string;
  isRead: boolean;
  createdAt?: Timestamp | Date | null;
}

export interface LocalCartEntry {
  productId: string;
  quantity: number;
  flashSaleId?: string;
  selectedColor?: string;
  selectedSize?: string;
}

export interface ResolvedCartLine {
  cartItemId: string;
  product: Product;
  quantity: number;
  unitPrice: number;
  appliedPricingMode: 'retail' | 'wholesale' | 'flash_sale';
  lineTotal: number;
  remainingForWholesale: number;
  flashSaleId?: string;
  flashSaleTitle?: string;
  selectedColor?: string;
  selectedSize?: string;
  hasStockIssue: boolean;
}

export function buildCartItemId(
  productId: string,
  selectedColor?: string | null,
  selectedSize?: string | null,
): string {
  const cleanId = (productId || '').trim();
  const color = (selectedColor || '').trim();
  const size = (selectedSize || '').trim();
  if (!color && !size) return cleanId;
  const raw = `${color}|${size}`;
  let hash = 0x811c9dc5 | 0;
  for (let i = 0; i < raw.length; i++) {
    hash = Math.imul(hash ^ raw.charCodeAt(i), 16777619);
  }
  return `${cleanId}__v_${(hash >>> 0).toString(36)}`;
}

export function normalizeEgyptianPhone(raw?: string | null): string {
  if (!raw) return '';
  const ascii = raw.replace(/[٠-٩۰-۹]/g, (ch) => {
    const code = ch.charCodeAt(0);
    if (code >= 0x0660 && code <= 0x0669) return String(code - 0x0660);
    if (code >= 0x06f0 && code <= 0x06f9) return String(code - 0x06f0);
    return ch;
  });
  let digits = ascii.replace(/\D/g, '');
  if (digits.startsWith('0020')) digits = `0${digits.slice(4)}`;
  else if (digits.startsWith('20') && digits.length >= 12) digits = `0${digits.slice(2)}`;
  if (digits.length === 10 && digits.startsWith('1')) digits = `0${digits}`;
  if (digits.length > 11 && digits.slice(-11).startsWith('01')) digits = digits.slice(-11);
  return digits;
}

export function displaySenderPhone(order: Pick<Order, 'senderPhone' | 'transferReference'>): string {
  return (order.senderPhone || order.transferReference || '').trim();
}

export interface StockAlert {
  id: string;
  productId: string;
  productName: string;
  userId: string;
  createdAt?: Timestamp | Date | null;
}

export interface SearchMiss {
  id: string;
  query: string;
  normalizedQuery: string;
  missCount: number;
  lastSource: 'android' | 'web' | string;
  createdAt?: Timestamp | Date | null;
  lastSearchedAt?: Timestamp | Date | null;
}

export function generateDeliveryOtp(): string {
  return String(Math.floor(1000 + Math.random() * 9000));
}

export function normalizeOtpDigits(raw?: string | null): string {
  if (!raw) return '';
  return raw
    .replace(/[٠-٩۰-۹]/g, (ch) => {
      const code = ch.charCodeAt(0);
      if (code >= 0x0660 && code <= 0x0669) return String(code - 0x0660);
      if (code >= 0x06f0 && code <= 0x06f9) return String(code - 0x06f0);
      return ch;
    })
    .replace(/\D/g, '');
}

export function hasPendingDeliveryOtp(
  order: Pick<Order, 'fulfillmentType' | 'deliveryOtp' | 'deliveryOtpVerified' | 'orderStatus'>,
): boolean {
  return (
    order.fulfillmentType !== 'pickup' &&
    Boolean(order.deliveryOtp?.trim()) &&
    !order.deliveryOtpVerified &&
    order.orderStatus !== 'delivered' &&
    order.orderStatus !== 'cancelled' &&
    order.orderStatus !== 'returned'
  );
}

export function normalizeSearchQuery(raw: string): string {
  return raw.trim().replace(/\s+/g, ' ').toLowerCase();
}

export function buildSearchMissDocId(normalizedQuery: string): string {
  let hash = 0x811c9dc5 | 0;
  for (let i = 0; i < normalizedQuery.length; i++) {
    hash = Math.imul(hash ^ normalizedQuery.charCodeAt(i), 16777619);
  }
  return `sm_${(hash >>> 0).toString(36)}`;
}

export interface FreeShippingEvaluation {
  deliveryFee: number;
  freeShippingEnabled: boolean;
  freeShippingThreshold: number;
  remainingForFreeShipping: number;
  freeShippingProgress: number;
  qualifiesForFreeShipping: boolean;
}

export function computeFreeShipping(params: {
  netMerchandise: number;
  isStorePickup: boolean;
  hasItems: boolean;
  baseZoneFee: number;
  enabled: boolean;
  threshold: number;
}): FreeShippingEvaluation {
  const cleanNet = Math.max(0, params.netMerchandise);
  const cleanThreshold = Math.max(0, params.threshold);
  const isFeatureActive = Boolean(params.enabled) && cleanThreshold > 0;
  const qualifies =
    !params.isStorePickup && params.hasItems && isFeatureActive && cleanNet >= cleanThreshold;
  const remaining =
    isFeatureActive && cleanNet < cleanThreshold
      ? Math.round((cleanThreshold - cleanNet) * 100) / 100
      : 0;
  const progress =
    isFeatureActive && cleanThreshold > 0
      ? Math.min(1, Math.max(0, cleanNet / cleanThreshold))
      : 0;
  const finalFee =
    !params.hasItems || params.isStorePickup || qualifies
      ? 0
      : Math.round(Math.max(0, params.baseZoneFee) * 100) / 100;

  return {
    deliveryFee: finalFee,
    freeShippingEnabled: isFeatureActive,
    freeShippingThreshold: cleanThreshold,
    remainingForFreeShipping: remaining,
    freeShippingProgress: progress,
    qualifiesForFreeShipping: qualifies,
  };
}

export const BOOTSTRAPPED_OWNER_EMAIL = 'mohamedamerghazy@gmail.com';

export const DEFAULT_PRIVACY_POLICY_TEXT = `نحن في متجر الإيمان للتجارة نحترم خصوصيتك ونلتزم بحماية بياناتك الشخصية وفقًا لأفضل معايير الأمان.

١. البيانات التي نجمعها عند استخدامك للتطبيق والموقع:
• بيانات الحساب: الاسم الكامل، البريد الإلكتروني، وصورة الحساب عند تسجيل الدخول بحساب Google، بالإضافة إلى رقم الهاتف واسم النشاط التجاري (لحسابات الجملة).
• بيانات التوصيل والطلبات: عناوين التوصيل التي تضيفها، وإحداثيات الموقع الجغرافي (GPS) عند اختيارك تحديد موقعك لتسهيل وصول المندوب، وسجل الطلبات وإيصالات التحويل المالية التي ترفعها طوعًا.

٢. أذونات الجهاز وكيفية استخدامها:
• الكاميرا: تُستخدم فقط عند رغبتك في مسح الباركود أو رمز QR للبحث السريع عن المنتجات.
• الموقع الجغرافي (اختياري): يُستخدم فقط عند ضغطك على زر تحديد الموقع الحالي لحفظ عنوان التوصيل بدقة.
• الصور والوسائط: تُستخدم فقط عند اختيارك رفع إيصال التحويل أو صور المنتجات.
• البلوتوث (للموظفين والإدارة فقط): يُستخدم فقط للاتصال بالطابعة الحرارية لطباعة فواتير تجهيز الطلبات.

٣. مشاركة البيانات وحمايتها:
• يتم تشفير جميع البيانات أثناء النقل باستخدام بروتوكول HTTPS عبر خوادم Google Firebase.
• لا نبيع بياناتك ولا نشاركها مع جهات إعلانية أو أطراف ثالثة.

٤. حذف الحساب والبيانات (Account Deletion):
• يمكنك في أي وقت حذف حسابك نهائيًا عبر: حسابي ← حذف الحساب نهائيًا.
• عند تأكيد الحذف يتم مسح ملفك الشخصي، عناوين التوصيل، سلة التسوق، إشعاراتك، وتقييماتك فورًا مع إزالة حساب الدخول الخاص بك.`;

export const DEFAULT_RETURN_POLICY_TEXT = `نحرص في متجر الإيمان للتجارة على رضاكم التام عن جميع منتجات المنظفات ومستلزمات العناية المنزلية سواء في طلبات القطاعي أو الجملة.

١. شروط الاسترجاع والاستبدال:
• يحق للعميل طلب استبدال أو استرجاع المنتج خلال ١٤ يومًا من تاريخ الاستلام إذا كان بحالته الأصلية، مغلفًا بغلاف المصنع الأصلي، وغير مفتوح أو مستخدم.
• في حال وجود تلف في العبوة أو خطأ في الصنف أو الكمية المسلمة، يتحمل المتجر كامل تكلفة الاستبدال أو رد القيمة فور الفحص.

٢. المنتجات غير القابلة للاسترجاع:
• العبوات المفتوحة أو المستخدمة جزئيًا أو المنتجات السائبة بعد الاستلام والمطابقة، ما لم يثبت وجود عيب تصنيع ظاهر.

٣. استرداد المبالغ المالية:
• عند إلغاء طلب قبل بدء التجهيز أو قبول طلب استرجاع مستوفٍ للشروط، يتم رد المبلغ بنفس وسيلة الدفع (نقدًا، إنستاباي، أو فودافون كاش) خلال ٢ إلى ٥ أيام عمل.
• الطلبات بنظام «الاستلام من المتجر» يمكن مراجعتها وفحصها مباشرة عند الاستلام في الفرع.

٤. طريقة تقديم طلب الاسترجاع:
• تواصل مع خدمة العملاء عبر الأرقام أو الواتساب الموضحين في صفحة «عن المتجر» مع ذكر رقم الطلب (مثل ET-1000).`;

export const DEFAULT_TERMS_OF_SERVICE_TEXT = `باستخدامك لمتجر الإيمان للتجارة فأنت توافق على الشروط والأحكام التالية:

١. الأسعار والطلبات:
• تُعرض الأسعار بالجنيه المصري، وتُطبق أسعار القطاعي للعملاء الأفراد وأسعار الجملة للتجار المعتمدين وفق الحد الأدنى للكمية المحدد لكل منتج.
• عروض الفلاش سيل تخضع للمدة الزمنية المعلنة والكمية المتاحة ولا تؤثر أسعارها على الكتالوج خارج وقت العرض.

٢. الدفع والتحويلات:
• يتحمل العميل مسؤولية صحة بيانات الحوالة وإيصال الدفع المرفق عند اختيار الدفع الإلكتروني (إنستاباي أو فودافون كاش)، وتتم مراجعة الإيصال قبل الشحن.
• في الدفع عند الاستلام أو عند الاستلام من المتجر يتم سداد إجمالي الفاتورة كاملة للمندوب أو في الفرع.

٣. التوصيل والاستلام:
• يحق للعميل إلغاء الطلب ذاتيًا من شاشة «طلباتي» طالما كانت حالة الطلب «طلب جديد» أو «قيد المراجعة».
• يلتزم العميل بإدخال رقم هاتف وعنوان صحيحين لتجنب تأخر أو إلغاء التوصيل.`;

export const DEFAULT_STORE_SETTINGS: StoreGeneralSettings = {
  id: 'store',
  storeName: 'الإيمان للتجارة',
  tagline: 'تسوق بذكاء .. عيش أفضل',
  ownerEmail: BOOTSTRAPPED_OWNER_EMAIL,
  supportPhone: '',
  whatsappNumber: '',
  storeAddress: '',
  announcementText: '',
  aboutStoreDescription: '',
  retailFacebookUrl: '',
  wholesaleFacebookUrl: '',
  socialPages: [],
  retailPhones: [],
  wholesalePhones: [],
  showFeaturedSection: true,
  showDealsSection: true,
  showBestsellersSection: true,
  showRecommendedSection: true,
  showHeroSection: true,
  heroBadgeText: 'عروض القطاعي والجملة',
  heroTitleText: 'كل ما تحتاجه بأفضل سعر للجملة والقطاعي',
  heroSubtitleText: 'تصفح الأقسام وتابع العروض اليومية مع توصيل سريع لكل المناطق.',
  heroCtaText: 'تسوق الآن',
  homeCategoryStyle: 'circular',
  categoriesTabStyle: 'noon_portrait',
  companyOfferBadgeStyle: 'gold_ticket',
  numeralFormat: 'arabic',
  privacyPolicyUrl: '',
  returnPolicyUrl: '',
  accountDeletionUrl: '',
  privacyPolicyText: DEFAULT_PRIVACY_POLICY_TEXT,
  returnPolicyText: DEFAULT_RETURN_POLICY_TEXT,
  termsOfServiceText: DEFAULT_TERMS_OF_SERVICE_TEXT,
  showProductColors: true,
  showProductSizes: true,
  freeShippingEnabled: false,
  freeShippingThreshold: 500,
  deliveryOtpEnabled: true,
  introVideoEnabled: false,
  introMediaType: 'video',
  introVideoUrl: '',
  introImageUrl: '',
  introVideoDurationSeconds: 0,
  introVideoTitle: '',
  introVideoMuted: false,
  tickerEnabled: false,
  tickerText: '',
  tickerPlacement: 'top_home',
  tickerBackgroundColor: '#181A20',
  tickerBorderColor: '#FFD000',
  tickerTextColor: '#FFFFFF',
  tickerFontSizeSp: 14,
  tickerFontWeight: 'bold',
  tickerItalic: false,
  tickerInlineImages: [],
  appIconTheme: 'default',
  productCardBorderStyle: 'default',
  productCardBorderColor: '#D4AF37',
  productCardShape: 'rounded',
  productDiscountBadgeStyle: 'pill',
};

export const DEFAULT_PAYMENT_SETTINGS: StorePaymentSettings = {
  id: 'payments',
  vodafoneCashNumber: '',
  instaPayAddress: '',
  paymentInstructions: '',
  codEnabled: true,
  instaPayEnabled: true,
  vodafoneCashEnabled: true,
};
