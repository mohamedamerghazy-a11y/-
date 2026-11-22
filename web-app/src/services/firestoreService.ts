import {
  Timestamp,
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  getDocs,
  onSnapshot,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
  type DocumentReference,
  type Unsubscribe,
} from 'firebase/firestore';
import { deleteUser, reauthenticateWithPopup } from 'firebase/auth';
import { auth, db, googleProvider } from '../lib/firebase';
import {
  BOOTSTRAPPED_OWNER_EMAIL,
  DEFAULT_PAYMENT_SETTINGS,
  DEFAULT_PRIVACY_POLICY_TEXT,
  DEFAULT_RETURN_POLICY_TEXT,
  DEFAULT_STORE_SETTINGS,
  DEFAULT_TERMS_OF_SERVICE_TEXT,
  STAFF_ROLE_META,
  buildCartItemId,
  buildSearchMissDocId,
  computeFreeShipping,
  generateDeliveryOtp,
  normalizeEgyptianPhone,
  normalizeSearchQuery,
  type AppNotification,
  type Banner,
  type BrandShowcase,
  type CashVerificationLog,
  type Category,
  type Coupon,
  type DeliveryAddress,
  type DeliveryZone,
  type FlashSale,
  type FulfillmentMethod,
  type LocalCartEntry,
  type NotificationType,
  type Order,
  type OrderStatus,
  type PaymentMethod,
  type PaymentStatus,
  type Product,
  type ProductReview,
  type SearchMiss,
  type StaffMember,
  type StaffPermission,
  type StaffRole,
  type StoreCompany,
  type StoreGeneralSettings,
  type StorePaymentSettings,
  type UserProfile,
} from '../types/models';
import {
  effectiveRetailPriceWithShowcases,
  getFlashSaleStatus,
  getFlashUnitPrice,
  roundTwo,
  toMillis,
} from '../lib/formatters';
import { deriveVideoPosterUrl } from '../lib/cloudinary';

export const STARTING_ORDER_NUMBER = 1000;

export function formatSequentialOrderNumber(seq: number): string {
  return `ET-${Math.max(STARTING_ORDER_NUMBER, Math.floor(seq))}`;
}

// --- Real-time Subscriptions ---

export function subscribeStoreSettings(cb: (settings: StoreGeneralSettings) => void): Unsubscribe {
  return onSnapshot(
    doc(db, 'settings', 'store'),
    (snap) => {
      if (!snap.exists()) {
        cb(DEFAULT_STORE_SETTINGS);
        return;
      }
      const d = snap.data();
      cb({
        ...DEFAULT_STORE_SETTINGS,
        id: snap.id,
        storeName: d.storeName || DEFAULT_STORE_SETTINGS.storeName,
        tagline: d.tagline || DEFAULT_STORE_SETTINGS.tagline,
        ownerEmail: d.ownerEmail || BOOTSTRAPPED_OWNER_EMAIL,
        supportPhone: d.supportPhone || '',
        whatsappNumber: d.whatsappNumber || '',
        storeAddress: d.storeAddress || '',
        announcementText: d.announcementText || '',
        aboutStoreDescription: d.aboutStoreDescription || '',
        retailFacebookUrl: d.retailFacebookUrl || '',
        wholesaleFacebookUrl: d.wholesaleFacebookUrl || '',
        socialPages: Array.isArray(d.socialPages) ? d.socialPages : [],
        retailPhones: Array.isArray(d.retailPhones) ? d.retailPhones : [],
        wholesalePhones: Array.isArray(d.wholesalePhones) ? d.wholesalePhones : [],
        showFeaturedSection: d.showFeaturedSection ?? true,
        showDealsSection: d.showDealsSection ?? true,
        showBestsellersSection: d.showBestsellersSection ?? true,
        showRecommendedSection: d.showRecommendedSection ?? true,
        showHeroSection: d.showHeroSection ?? true,
        heroBadgeText: d.heroBadgeText || DEFAULT_STORE_SETTINGS.heroBadgeText,
        heroTitleText: d.heroTitleText || DEFAULT_STORE_SETTINGS.heroTitleText,
        heroSubtitleText: d.heroSubtitleText || DEFAULT_STORE_SETTINGS.heroSubtitleText,
        heroCtaText: d.heroCtaText || DEFAULT_STORE_SETTINGS.heroCtaText,
        homeCategoryStyle: d.homeCategoryStyle || 'circular',
        categoriesTabStyle: d.categoriesTabStyle || 'noon_portrait',
        companyOfferBadgeStyle: d.companyOfferBadgeStyle || 'gold_ticket',
        numeralFormat: d.numeralFormat || 'arabic',
        privacyPolicyUrl: d.privacyPolicyUrl || '',
        returnPolicyUrl: d.returnPolicyUrl || '',
        accountDeletionUrl: d.accountDeletionUrl || '',
        privacyPolicyText: d.privacyPolicyText?.trim() || DEFAULT_PRIVACY_POLICY_TEXT,
        returnPolicyText: d.returnPolicyText?.trim() || DEFAULT_RETURN_POLICY_TEXT,
        termsOfServiceText: d.termsOfServiceText?.trim() || DEFAULT_TERMS_OF_SERVICE_TEXT,
        showProductColors: d.showProductColors ?? true,
        showProductSizes: d.showProductSizes ?? true,
        freeShippingEnabled: Boolean(d.freeShippingEnabled ?? false),
        freeShippingThreshold: Number(d.freeShippingThreshold ?? 500),
        deliveryOtpEnabled: d.deliveryOtpEnabled ?? true,
        introVideoEnabled: Boolean(d.introVideoEnabled ?? false),
        introMediaType: d.introMediaType === 'animated_image' ? 'animated_image' : 'video',
        introVideoUrl: String(d.introVideoUrl || ''),
        introImageUrl: String(d.introImageUrl || ''),
        introVideoDurationSeconds: Math.min(120, Math.max(0, Number(d.introVideoDurationSeconds || 0))),
        introVideoTitle: String(d.introVideoTitle || ''),
        introVideoMuted: Boolean(d.introVideoMuted ?? false),
        tickerEnabled: Boolean(d.tickerEnabled ?? false),
        tickerText: String(d.tickerText || ''),
        tickerPlacement: (
          [
            'top_home',
            'below_hero',
            'below_categories',
            'below_featured',
            'below_deals',
            'below_newest',
            'below_bestsellers',
            'below_recommended',
          ].includes(String(d.tickerPlacement || ''))
            ? d.tickerPlacement
            : 'top_home'
        ),
        tickerBackgroundColor:
          typeof d.tickerBackgroundColor === 'string' && /^#[0-9A-Fa-f]{6}$/.test(d.tickerBackgroundColor.trim())
            ? d.tickerBackgroundColor.trim().toUpperCase()
            : '#181A20',
        tickerBorderColor:
          typeof d.tickerBorderColor === 'string' && /^#[0-9A-Fa-f]{6}$/.test(d.tickerBorderColor.trim())
            ? d.tickerBorderColor.trim().toUpperCase()
            : '#FFD000',
        tickerTextColor:
          typeof d.tickerTextColor === 'string' && /^#[0-9A-Fa-f]{6}$/.test(d.tickerTextColor.trim())
            ? d.tickerTextColor.trim().toUpperCase()
            : '#FFFFFF',
        tickerFontSizeSp: Math.min(24, Math.max(11, Math.round(Number(d.tickerFontSizeSp || 14)))),
        tickerFontWeight: (
          ['light', 'normal', 'bold', 'extra_bold'].includes(String(d.tickerFontWeight || ''))
            ? d.tickerFontWeight
            : 'bold'
        ),
        tickerItalic: Boolean(d.tickerItalic ?? false),
        tickerInlineImages: Array.isArray(d.tickerInlineImages)
          ? d.tickerInlineImages
              .map((img: { id?: unknown; url?: unknown }) => ({
                id: String(img?.id || '').trim(),
                url: String(img?.url || '').trim(),
              }))
              .filter((img: { id: string; url: string }) => img.id && img.url)
              .slice(0, 10)
          : [],
        appIconTheme:
          d.appIconTheme === 'offers' || d.appIconTheme === 'ramadan' || d.appIconTheme === 'eid'
            ? d.appIconTheme
            : 'default',
        productCardBorderStyle: (
          ['default', 'gold', 'emerald', 'royal_blue', 'ruby', 'purple', 'charcoal', 'custom', 'none'].includes(
            String(d.productCardBorderStyle || ''),
          )
            ? d.productCardBorderStyle
            : 'default'
        ),
        productCardBorderColor:
          typeof d.productCardBorderColor === 'string' && /^#[0-9A-Fa-f]{6}$/.test(d.productCardBorderColor.trim())
            ? d.productCardBorderColor.trim().toUpperCase()
            : '#D4AF37',
        productCardShape: (
          ['rounded', 'circular', 'rectangle', 'cut_corner', 'asymmetric', 'arch'].includes(
            String(d.productCardShape || ''),
          )
            ? d.productCardShape
            : 'rounded'
        ),
        productDiscountBadgeStyle: (
          ['pill', 'ribbon', 'starburst', 'price_tag', 'gold_luxury', 'capsule_outline'].includes(
            String(d.productDiscountBadgeStyle || ''),
          )
            ? d.productDiscountBadgeStyle
            : 'pill'
        ),
        updatedAt: d.updatedAt || null,
      });
    },
    () => cb(DEFAULT_STORE_SETTINGS),
  );
}

export function subscribePaymentSettings(cb: (settings: StorePaymentSettings) => void): Unsubscribe {
  return onSnapshot(
    doc(db, 'settings', 'payments'),
    (snap) => {
      if (!snap.exists()) {
        cb(DEFAULT_PAYMENT_SETTINGS);
        return;
      }
      const d = snap.data();
      cb({
        id: snap.id,
        vodafoneCashNumber: d.vodafoneCashNumber || '',
        instaPayAddress: d.instaPayAddress || '',
        paymentInstructions: d.paymentInstructions || '',
        codEnabled: d.codEnabled ?? true,
        instaPayEnabled: d.instaPayEnabled ?? true,
        vodafoneCashEnabled: d.vodafoneCashEnabled ?? true,
        updatedAt: d.updatedAt || null,
      });
    },
    () => cb(DEFAULT_PAYMENT_SETTINGS),
  );
}

export function subscribeCategories(includeInactive: boolean, cb: (list: Category[]) => void): Unsubscribe {
  const ref = includeInactive
    ? collection(db, 'categories')
    : query(collection(db, 'categories'), where('isActive', '==', true));
  return onSnapshot(ref, (snap) => {
    const items = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<Category, 'id'>) }))
      .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || a.name.localeCompare(b.name, 'ar'));
    cb(items);
  });
}

export function subscribeProducts(includeInactive: boolean, cb: (list: Product[]) => void): Unsubscribe {
  const ref = includeInactive
    ? collection(db, 'products')
    : query(collection(db, 'products'), where('isActive', '==', true));
  return onSnapshot(ref, (snap) => {
    const items = snap.docs
      .map((d) => {
        const data = d.data() as Omit<Product, 'id'>;
        return {
          ...data,
          id: d.id,
          imageUrls: Array.isArray(data.imageUrls) ? data.imageUrls : [],
          retailPrice: Number(data.retailPrice || 0),
          retailDiscountPrice: data.retailDiscountPrice != null ? Number(data.retailDiscountPrice) : undefined,
          wholesalePrice: Number(data.wholesalePrice || 0),
          wholesaleMinQty: Number(data.wholesaleMinQty || 1),
          stockQuantity: Number(data.stockQuantity || 0),
          lowStockThreshold: Number(data.lowStockThreshold || 5),
          salesCount: Number(data.salesCount || 0),
          averageRating: Number(data.averageRating || 0),
          reviewCount: Number(data.reviewCount || 0),
          availableColors: Array.isArray(data.availableColors) ? data.availableColors.map(String) : [],
          availableSizes: Array.isArray(data.availableSizes) ? data.availableSizes.map(String) : [],
          companyId: data.companyId ? String(data.companyId) : undefined,
          brandShowcaseId: data.brandShowcaseId ? String(data.brandShowcaseId) : undefined,
          brandCategoryId: data.brandCategoryId ? String(data.brandCategoryId) : undefined,
          exclusiveToBrandShowcase: Boolean(data.exclusiveToBrandShowcase),
        } satisfies Product;
      })
      .sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt));
    cb(items);
  });
}

export function subscribeCompanies(
  includeInactive: boolean,
  cb: (list: StoreCompany[]) => void,
): Unsubscribe {
  const ref = includeInactive
    ? collection(db, 'companies')
    : query(collection(db, 'companies'), where('isActive', '==', true));
  return onSnapshot(
    ref,
    (snap) => {
      const items = snap.docs
        .map((d) => {
          const data = d.data() as Omit<StoreCompany, 'id'>;
          return {
            ...data,
            id: d.id,
            name: String(data.name || ''),
            imageUrl: data.imageUrl ? String(data.imageUrl) : '',
            displayOrder: Number(data.displayOrder || 0),
            isActive: data.isActive ?? true,
          } satisfies StoreCompany;
        })
        .sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name, 'ar'));
      cb(items);
    },
    () => cb([]),
  );
}

export function subscribeBanners(includeInactive: boolean, cb: (list: Banner[]) => void): Unsubscribe {
  const ref = includeInactive
    ? collection(db, 'banners')
    : query(collection(db, 'banners'), where('isActive', '==', true));
  return onSnapshot(ref, (snap) => {
    const items = snap.docs
      .map((d) => {
        const data = d.data() as Omit<Banner, 'id'>;
        return {
          ...data,
          id: d.id,
          mediaType:
            data.mediaType === 'video'
              ? 'video'
              : data.mediaType === 'animated_image'
              ? 'animated_image'
              : 'image',
          videoUrl: data.videoUrl ? String(data.videoUrl) : '',
          videoMuted: Boolean(data.videoMuted ?? false),
          placement: data.placement || 'top_home',
          bannerStyle: data.bannerStyle || 'normal',
          durationSeconds: Number(data.durationSeconds || 5),
          displayOrder: Number(data.displayOrder || 0),
        } satisfies Banner;
      })
      .sort((a, b) => a.displayOrder - b.displayOrder || toMillis(b.createdAt) - toMillis(a.createdAt));
    cb(items);
  });
}

export function subscribeBrandShowcases(
  includeInactive: boolean,
  cb: (list: BrandShowcase[]) => void,
): Unsubscribe {
  const ref = includeInactive
    ? collection(db, 'brandShowcases')
    : query(collection(db, 'brandShowcases'), where('isActive', '==', true));
  return onSnapshot(
    ref,
    (snap) => {
      const items = snap.docs
        .map((d) => {
          const data = d.data() as Omit<BrandShowcase, 'id'>;
          return {
            ...data,
            id: d.id,
            companyId: data.companyId ? String(data.companyId) : undefined,
            companyImageUrl: data.companyImageUrl ? String(data.companyImageUrl) : undefined,
            bannerImageUrl: data.bannerImageUrl ? String(data.bannerImageUrl) : '',
            discountPercent: Math.min(95, Math.max(0, Number(data.discountPercent || 0))),
            accentColorHex: data.accentColorHex || '#FCE4EC',
            placement: data.placement || 'below_categories',
            displayOrder: Number(data.displayOrder || 0),
            categories: Array.isArray(data.categories)
              ? [...data.categories].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
              : [],
          } satisfies BrandShowcase;
        })
        .sort((a, b) => a.displayOrder - b.displayOrder || toMillis(b.createdAt) - toMillis(a.createdAt));
      cb(items);
    },
    () => cb([]),
  );
}

export function subscribeFlashSales(includeInactive: boolean, cb: (list: FlashSale[]) => void): Unsubscribe {
  const ref = includeInactive
    ? collection(db, 'flashSales')
    : query(collection(db, 'flashSales'), where('isActive', '==', true));
  return onSnapshot(ref, (snap) => {
    const items = snap.docs
      .map((d) => {
        const data = d.data() as Omit<FlashSale, 'id'>;
        return {
          ...data,
          id: d.id,
          items: Array.isArray(data.items) ? data.items : [],
        } satisfies FlashSale;
      })
      .sort((a, b) => toMillis(a.startsAt) - toMillis(b.startsAt));
    cb(items);
  });
}

export function subscribeDeliveryZones(includeInactive: boolean, cb: (list: DeliveryZone[]) => void): Unsubscribe {
  const ref = includeInactive
    ? collection(db, 'deliveryZones')
    : query(collection(db, 'deliveryZones'), where('isActive', '==', true));
  return onSnapshot(ref, (snap) => {
    const items = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<DeliveryZone, 'id'>) }))
      .sort((a, b) => a.governorate.localeCompare(b.governorate, 'ar') || a.name.localeCompare(b.name, 'ar'));
    cb(items);
  });
}

export function subscribeUserProfile(uid: string, cb: (profile: UserProfile | null) => void): Unsubscribe {
  return onSnapshot(doc(db, 'users', uid), (snap) => {
    if (!snap.exists()) {
      cb(null);
      return;
    }
    cb({ id: snap.id, ...(snap.data() as Omit<UserProfile, 'id'>) });
  });
}

export function subscribeStaffMember(uid: string, cb: (staff: StaffMember | null) => void): Unsubscribe {
  return onSnapshot(
    doc(db, 'staff', uid),
    (snap) => {
      if (!snap.exists()) {
        cb(null);
        return;
      }
      cb({ id: snap.id, ...(snap.data() as Omit<StaffMember, 'id'>) });
    },
    () => cb(null),
  );
}

export function subscribeAddresses(uid: string, cb: (list: DeliveryAddress[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'users', uid, 'addresses'), (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<DeliveryAddress, 'id'>) }))
      .sort((a, b) => Number(b.isDefault) - Number(a.isDefault));
    cb(list);
  });
}

export function subscribeUserCart(uid: string, cb: (list: LocalCartEntry[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'users', uid, 'cart'), (snap) => {
    const list = snap.docs.map((d) => {
      const data = d.data();
      return {
        productId: String(data.productId || d.id),
        quantity: Number(data.quantity || 1),
        flashSaleId: data.flashSaleId ? String(data.flashSaleId) : undefined,
        selectedColor: data.selectedColor ? String(data.selectedColor) : undefined,
        selectedSize: data.selectedSize ? String(data.selectedSize) : undefined,
      } satisfies LocalCartEntry;
    });
    cb(list);
  });
}

export function subscribeWishlistProductIds(uid: string, cb: (ids: string[]) => void): Unsubscribe {
  return onSnapshot(
    collection(db, 'users', uid, 'wishlist'),
    (snap) => {
      const ids = snap.docs.map((d) => String(d.data()?.productId || d.id)).filter(Boolean);
      cb(ids);
    },
    () => cb([]),
  );
}

export async function toggleWishlistItem(
  uid: string,
  productId: string,
  currentlyInWishlist: boolean,
): Promise<boolean> {
  const cleanId = productId.trim();
  const ref = doc(db, 'users', uid, 'wishlist', cleanId);
  if (currentlyInWishlist) {
    await deleteDoc(ref);
    return false;
  }
  await setDoc(ref, {
    userId: uid,
    productId: cleanId,
    createdAt: serverTimestamp(),
  });
  return true;
}

export function subscribeUserOrders(uid: string, cb: (list: Order[]) => void): Unsubscribe {
  const q = query(collection(db, 'orders'), where('userId', '==', uid));
  return onSnapshot(q, (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<Order, 'id'>) }))
      .sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt));
    cb(list);
  });
}

export function subscribeUserNotifications(uid: string, cb: (list: AppNotification[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'users', uid, 'notifications'), (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<AppNotification, 'id'>) }))
      .sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt));
    cb(list);
  });
}

export function subscribeProductReviews(productId: string, cb: (list: ProductReview[]) => void): Unsubscribe {
  const q = query(
    collection(db, 'reviews'),
    where('productId', '==', productId),
    where('isHidden', '==', false),
  );
  return onSnapshot(q, (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<ProductReview, 'id'>) }))
      .sort((a, b) => toMillis(b.updatedAt || b.createdAt) - toMillis(a.updatedAt || a.createdAt));
    cb(list);
  });
}

// --- Admin Subscriptions ---

export function subscribeAllOrders(cb: (list: Order[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'orders'), (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<Order, 'id'>) }))
      .sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt));
    cb(list);
  });
}

export function subscribeDriverOrders(driverUid: string, cb: (list: Order[]) => void): Unsubscribe {
  const q = query(collection(db, 'orders'), where('assignedDriverId', '==', driverUid));
  return onSnapshot(q, (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<Order, 'id'>) }))
      .sort((a, b) => toMillis(b.assignedAt || b.createdAt) - toMillis(a.assignedAt || a.createdAt));
    cb(list);
  });
}

export function subscribeAllUsers(cb: (list: UserProfile[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'users'), (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<UserProfile, 'id'>) }))
      .sort((a, b) => a.fullName.localeCompare(b.fullName, 'ar'));
    cb(list);
  });
}

export function subscribeAllStaff(cb: (list: StaffMember[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'staff'), (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<StaffMember, 'id'>) }))
      .sort((a, b) => a.fullName.localeCompare(b.fullName, 'ar'));
    cb(list);
  });
}

export function subscribeAllCoupons(cb: (list: Coupon[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'coupons'), (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<Coupon, 'id'>) }))
      .sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt));
    cb(list);
  });
}

export function subscribeAllReviews(cb: (list: ProductReview[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'reviews'), (snap) => {
    const list = snap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<ProductReview, 'id'>) }))
      .sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt));
    cb(list);
  });
}

export function subscribeCashVerificationLogs(cb: (list: CashVerificationLog[]) => void): Unsubscribe {
  return onSnapshot(
    collection(db, 'cashVerificationLogs'),
    (snap) => {
      const list = snap.docs
        .map((d) => ({ id: d.id, ...(d.data() as Omit<CashVerificationLog, 'id'>) }))
        .sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt));
      cb(list);
    },
    () => cb([]),
  );
}

// --- User Profile, Auth & Account Deletion ---

export async function createUserProfileDoc(params: {
  fullName: string;
  email: string;
  phone: string;
  photoUrl?: string;
  requestWholesale: boolean;
  companyName?: string;
}) {
  const user = auth.currentUser;
  if (!user) throw new Error('يجب تسجيل الدخول أولًا.');
  const cleanEmail = params.email.trim();
  let configuredOwnerEmail: string | null = null;
  if (cleanEmail.toLowerCase() !== BOOTSTRAPPED_OWNER_EMAIL.toLowerCase()) {
    try {
      const storeSnap = await getDoc(doc(db, 'settings', 'store'));
      configuredOwnerEmail = (storeSnap.data()?.ownerEmail as string) || null;
    } catch {
      configuredOwnerEmail = null;
    }
  }
  const isOwner =
    cleanEmail.toLowerCase() === BOOTSTRAPPED_OWNER_EMAIL.toLowerCase() ||
    (!!configuredOwnerEmail && cleanEmail.toLowerCase() === configuredOwnerEmail.trim().toLowerCase());

  const payload: Record<string, unknown> = {
    uid: user.uid,
    fullName: params.fullName.trim(),
    email: cleanEmail,
    accountType: params.requestWholesale ? 'wholesale' : 'retail',
    wholesaleStatus: params.requestWholesale ? 'pending' : 'none',
    role: isOwner ? 'owner' : 'customer',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  if (params.phone.trim()) payload.phone = params.phone.trim();
  if (params.photoUrl?.trim()) payload.photoUrl = params.photoUrl.trim();
  if (params.companyName?.trim()) payload.wholesaleCompanyName = params.companyName.trim();

  await setDoc(doc(db, 'users', user.uid), payload);
}

export async function updateUserProfileDetails(params: {
  fullName: string;
  phone: string;
  companyName?: string;
}) {
  const user = auth.currentUser;
  if (!user) throw new Error('يجب تسجيل الدخول أولًا.');
  const updates: Record<string, unknown> = {
    fullName: params.fullName.trim(),
    phone: params.phone.trim(),
    updatedAt: serverTimestamp(),
  };
  if (params.companyName?.trim()) {
    updates.wholesaleCompanyName = params.companyName.trim();
  }
  await updateDoc(doc(db, 'users', user.uid), updates);
}

export async function submitWholesaleUpgradeRequest(params: {
  fullName: string;
  phone: string;
  companyName: string;
}) {
  const user = auth.currentUser;
  if (!user) throw new Error('يجب تسجيل الدخول أولًا.');
  await updateDoc(doc(db, 'users', user.uid), {
    fullName: params.fullName.trim(),
    phone: params.phone.trim(),
    wholesaleCompanyName: params.companyName.trim(),
    accountType: 'wholesale',
    wholesaleStatus: 'pending',
    updatedAt: serverTimestamp(),
  });
}

export async function deleteCurrentAccountWithReauth(): Promise<void> {
  let user = auth.currentUser;
  if (!user) throw new Error('لا يوجد حساب مسجل حاليًا.');

  const lastSignInMs = user.metadata?.lastSignInTime ? new Date(user.metadata.lastSignInTime).getTime() : 0;
  if (!lastSignInMs || Date.now() - lastSignInMs > 4 * 60 * 1000) {
    const res = await reauthenticateWithPopup(user, googleProvider);
    user = res.user;
  }

  const uid = user.uid;
  const userDocRef = doc(db, 'users', uid);
  const refsToDelete: DocumentReference[] = [];

  for (const subCol of ['addresses', 'cart', 'notifications', 'wishlist']) {
    const snap = await getDocs(collection(db, 'users', uid, subCol));
    snap.docs.forEach((d) => refsToDelete.push(d.ref));
  }
  try {
    const reviewsSnap = await getDocs(query(collection(db, 'reviews'), where('userId', '==', uid)));
    reviewsSnap.docs.forEach((d) => refsToDelete.push(d.ref));
  } catch {
    // Ignore non-fatal review lookup issues
  }

  for (let i = 0; i < refsToDelete.length; i += 400) {
    const batch = writeBatch(db);
    refsToDelete.slice(i, i + 400).forEach((r) => batch.delete(r));
    await batch.commit();
  }

  const userSnap = await getDoc(userDocRef);
  if (userSnap.exists()) {
    await deleteDoc(userDocRef);
  }

  await deleteUser(user);
}

// --- Addresses CRUD ---

export async function saveDeliveryAddress(address: Omit<DeliveryAddress, 'id' | 'userId'> & { id?: string }) {
  const user = auth.currentUser;
  if (!user) throw new Error('يجب تسجيل الدخول أولًا.');
  const colRef = collection(db, 'users', user.uid, 'addresses');
  const isNew = !address.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, address.id);

  const batch = writeBatch(db);
  if (address.isDefault) {
    const existingSnap = await getDocs(colRef);
    existingSnap.docs.forEach((d) => {
      if (d.id !== docRef.id && d.data().isDefault === true) {
        batch.update(d.ref, { isDefault: false, updatedAt: serverTimestamp() });
      }
    });
  }

  const cleanLandmark = address.landmark?.trim() || '';
  const hasCoords = address.latitude != null && address.longitude != null;

  if (isNew) {
    const payload: Record<string, unknown> = {
      userId: user.uid,
      label: address.label.trim(),
      recipientName: address.recipientName.trim(),
      phone: address.phone.trim(),
      governorate: address.governorate.trim(),
      city: address.city.trim(),
      streetAddress: address.streetAddress.trim(),
      isDefault: address.isDefault,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (cleanLandmark) payload.landmark = cleanLandmark;
    if (hasCoords) {
      payload.latitude = address.latitude;
      payload.longitude = address.longitude;
    }
    batch.set(docRef, payload);
  } else {
    const updates: Record<string, unknown> = {
      label: address.label.trim(),
      recipientName: address.recipientName.trim(),
      phone: address.phone.trim(),
      governorate: address.governorate.trim(),
      city: address.city.trim(),
      streetAddress: address.streetAddress.trim(),
      isDefault: address.isDefault,
      updatedAt: serverTimestamp(),
    };
    updates.landmark = cleanLandmark || deleteField();
    if (hasCoords) {
      updates.latitude = address.latitude;
      updates.longitude = address.longitude;
    } else {
      updates.latitude = deleteField();
      updates.longitude = deleteField();
    }
    batch.update(docRef, updates);
  }

  await batch.commit();
  return docRef.id;
}

export async function deleteDeliveryAddress(addressId: string) {
  const user = auth.currentUser;
  if (!user) throw new Error('يجب تسجيل الدخول أولًا.');
  await deleteDoc(doc(db, 'users', user.uid, 'addresses', addressId));
}

// --- Cart Firestore Sync ---

export async function setFirestoreCartQuantity(
  uid: string,
  productId: string,
  quantity: number,
  flashSaleId?: string,
  selectedColor?: string,
  selectedSize?: string,
) {
  const docId = buildCartItemId(productId, selectedColor, selectedSize);
  const docRef = doc(db, 'users', uid, 'cart', docId);
  if (quantity <= 0) {
    await deleteDoc(docRef);
    return;
  }
  const cleanFlash = flashSaleId?.trim() || '';
  const cleanColor = selectedColor?.trim() || '';
  const cleanSize = selectedSize?.trim() || '';
  const existing = await getDoc(docRef);
  if (existing.exists()) {
    const updates: Record<string, unknown> = {
      quantity: Math.min(100000, quantity),
      updatedAt: serverTimestamp(),
    };
    if (cleanFlash) updates.flashSaleId = cleanFlash;
    if (cleanColor) updates.selectedColor = cleanColor;
    if (cleanSize) updates.selectedSize = cleanSize;
    await updateDoc(docRef, updates);
  } else {
    const payload: Record<string, unknown> = {
      userId: uid,
      productId: productId.trim(),
      quantity: Math.min(100000, quantity),
      updatedAt: serverTimestamp(),
    };
    if (cleanFlash) payload.flashSaleId = cleanFlash;
    if (cleanColor) payload.selectedColor = cleanColor;
    if (cleanSize) payload.selectedSize = cleanSize;
    await setDoc(docRef, payload);
  }
}

export async function mergeLocalCartToFirestore(uid: string, entries: LocalCartEntry[]) {
  if (!entries.length) return;
  const colRef = collection(db, 'users', uid, 'cart');
  const existingSnap = await getDocs(colRef);
  const existingMap = new Map(existingSnap.docs.map((d) => [d.id, d.data()]));
  const batch = writeBatch(db);

  for (const entry of entries) {
    const pid = entry.productId.trim();
    if (!pid || entry.quantity <= 0) continue;
    const docId = buildCartItemId(pid, entry.selectedColor, entry.selectedSize);
    const docRef = doc(colRef, docId);
    const prev = existingMap.get(docId);
    const cleanFlash = entry.flashSaleId?.trim() || '';
    const cleanColor = entry.selectedColor?.trim() || '';
    const cleanSize = entry.selectedSize?.trim() || '';
    if (prev) {
      const merged = Math.min(100000, Number(prev.quantity || 0) + entry.quantity);
      const updates: Record<string, unknown> = {
        quantity: merged,
        updatedAt: serverTimestamp(),
      };
      if (cleanFlash) updates.flashSaleId = cleanFlash;
      if (cleanColor) updates.selectedColor = cleanColor;
      if (cleanSize) updates.selectedSize = cleanSize;
      batch.update(docRef, updates);
    } else {
      const payload: Record<string, unknown> = {
        userId: uid,
        productId: pid,
        quantity: Math.min(100000, entry.quantity),
        updatedAt: serverTimestamp(),
      };
      if (cleanFlash) payload.flashSaleId = cleanFlash;
      if (cleanColor) payload.selectedColor = cleanColor;
      if (cleanSize) payload.selectedSize = cleanSize;
      batch.set(docRef, payload);
    }
  }
  await batch.commit();
}

// --- Coupons ---

export async function validateCouponCode(rawCode: string, subtotal: number): Promise<Coupon> {
  if (!auth.currentUser) throw new Error('يرجى تسجيل الدخول لتفعيل كود الخصم.');
  const normalized = rawCode.trim().toUpperCase();
  if (normalized.length < 2) throw new Error('يرجى إدخال كود خصم صحيح.');

  const snap = await getDocs(query(collection(db, 'coupons'), where('isActive', '==', true)));
  const coupons = snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Coupon, 'id'>) }));
  const matched = coupons.find((c) => c.code.trim().toUpperCase() === normalized);
  if (!matched) throw new Error('كود الخصم غير موجود أو غير مفعّل حاليًا.');

  const exp = toMillis(matched.expiresAt);
  if (exp > 0 && exp < Date.now()) throw new Error('انتهت صلاحية كود الخصم.');
  if (matched.usageLimit && matched.usedCount >= matched.usageLimit) {
    throw new Error('تم استهلاك الحد الأقصى لاستخدام هذا الكود.');
  }
  if (subtotal < matched.minOrderAmount) {
    throw new Error(`الحد الأدنى لتطبيق هذا الكود هو ${Math.round(matched.minOrderAmount)} ج.م.`);
  }
  return matched;
}

export function calculateCouponDiscount(coupon: Coupon, subtotal: number): number {
  if (subtotal <= 0 || subtotal < coupon.minOrderAmount) return 0;
  const raw =
    coupon.discountType === 'percent'
      ? subtotal * (coupon.discountValue / 100)
      : coupon.discountValue;
  const capped = coupon.maxDiscountAmount && coupon.maxDiscountAmount > 0
    ? Math.min(raw, coupon.maxDiscountAmount)
    : raw;
  return roundTwo(Math.min(capped, subtotal));
}

// --- Atomic Order Transaction (Matches Android OrderRepository 100%) ---

export async function placeOrderTransaction(params: {
  lines: LocalCartEntry[];
  fulfillmentMethod: FulfillmentMethod;
  address: DeliveryAddress | null;
  deliveryZone: DeliveryZone | null;
  couponId?: string | null;
  paymentMethod: PaymentMethod;
  transferReference?: string;
  senderPhone?: string;
  receiptImageUrl?: string;
  notes?: string;
  pickupRecipientName?: string;
  pickupPhone?: string;
  storeAddress?: string;
}): Promise<{ orderId: string; orderNumber: string }> {
  const user = auth.currentUser;
  if (!user) throw new Error('يجب تسجيل الدخول أولًا لإتمام الطلب.');
  const validLines = params.lines.filter((l) => l.productId.trim() && l.quantity > 0);
  if (!validLines.length) throw new Error('عربة التسوق فارغة، أضف منتجًا واحدًا على الأقل.');
  const isPickup = params.fulfillmentMethod === 'pickup';
  if (!isPickup && !params.address) {
    throw new Error('يرجى اختيار عنوان التوصيل أو اختيار الاستلام من المتجر.');
  }

  const orderRef = doc(collection(db, 'orders'));
  const userRef = doc(db, 'users', user.uid);
  const counterRef = doc(db, 'counters', 'orders');

  // Pre-discover active brand showcase IDs so we can read them transactionally inside Phase 1
  let candidateShowcaseIds: string[] = [];
  try {
    const activeShowcasesSnap = await getDocs(
      query(collection(db, 'brandShowcases'), where('isActive', '==', true)),
    );
    candidateShowcaseIds = activeShowcasesSnap.docs.map((d) => d.id);
  } catch {
    candidateShowcaseIds = [];
  }

  const result = await runTransaction(db, async (tx) => {
    // 1. ALL READS FIRST
    const userSnap = await tx.get(userRef);
    if (!userSnap.exists()) throw new Error('تعذر العثور على ملف حسابك الشخصي.');

    const counterSnap = await tx.get(counterRef);
    const assignedSeq = counterSnap.exists()
      ? Math.max(STARTING_ORDER_NUMBER, Number(counterSnap.data()?.nextNumber || STARTING_ORDER_NUMBER))
      : STARTING_ORDER_NUMBER;
    const orderNumber = formatSequentialOrderNumber(assignedSeq);

    const productSnaps = await Promise.all(
      validLines.map(async (line) => ({
        line,
        snap: await tx.get(doc(db, 'products', line.productId)),
      })),
    );

    const couponRef = params.couponId?.trim() ? doc(db, 'coupons', params.couponId.trim()) : null;
    const couponSnap = couponRef ? await tx.get(couponRef) : null;

    const flashSaleIds = Array.from(
      new Set(validLines.map((l) => l.flashSaleId?.trim() || '').filter(Boolean)),
    );
    const flashSnaps = await Promise.all(
      flashSaleIds.map(async (fid) => ({
        fid,
        snap: await tx.get(doc(db, 'flashSales', fid)),
      })),
    );

    const showcaseSnaps = await Promise.all(
      candidateShowcaseIds.map((sid) => tx.get(doc(db, 'brandShowcases', sid))),
    );

    const storeSnap = await tx.get(doc(db, 'settings', 'store'));

    // 2. VALIDATIONS & CALCULATIONS
    const userData = userSnap.data();
    const isWholesaleApproved =
      userData.accountType === 'wholesale' && userData.wholesaleStatus === 'approved';
    const accountTypeAtOrder = isWholesaleApproved ? 'wholesale' : 'retail';

    const customerName =
      String(userData.fullName || '').trim().length >= 2
        ? String(userData.fullName).trim()
        : params.pickupRecipientName?.trim() || params.address?.recipientName?.trim() || 'عميل المتجر';
    const rawEmail = String(userData.email || '').trim();
    const customerEmail = rawEmail.length >= 3 ? rawEmail : user.email?.trim() || 'customer@aleman.store';

    const nowMillis = Date.now();
    const activeFlashSalesById = new Map<string, FlashSale>();
    for (const { fid, snap } of flashSnaps) {
      if (!snap.exists()) continue;
      const sale = { id: snap.id, ...(snap.data() as Omit<FlashSale, 'id'>) };
      if (sale.isActive && getFlashSaleStatus(sale, nowMillis) === 'active') {
        activeFlashSalesById.set(fid, sale);
      }
    }

    const activeBrandShowcases: BrandShowcase[] = showcaseSnaps
      .filter((s) => s.exists())
      .map((s) => {
        const d = s.data() as Omit<BrandShowcase, 'id'>;
        return {
          ...d,
          id: s.id,
          discountPercent: Math.min(95, Math.max(0, Number(d.discountPercent || 0))),
          categories: Array.isArray(d.categories) ? d.categories : [],
        } satisfies BrandShowcase;
      })
      .filter((s) => s.isActive && (s.discountPercent || 0) > 0);

    const itemMaps: Record<string, unknown>[] = [];
    const stockUpdates: Array<{ ref: DocumentReference; stockQuantity: number; salesCount: number }> = [];
    let subtotal = 0;

    for (const { line, snap } of productSnaps) {
      if (!snap.exists()) throw new Error('أحد المنتجات لم يعد متوفرًا في المتجر.');
      const prod = { id: snap.id, ...(snap.data() as Omit<Product, 'id'>) };
      if (!prod.isActive) throw new Error(`المنتج «${prod.name}» غير متاح حاليًا للطلب.`);
      if (prod.stockQuantity < line.quantity) {
        throw new Error(`الكمية المطلوبة من «${prod.name}» غير متوفرة (المتاح: ${prod.stockQuantity}).`);
      }

      const activeFlash = line.flashSaleId ? activeFlashSalesById.get(line.flashSaleId.trim()) : undefined;
      const flashItem = activeFlash?.items.find((i) => i.productId === prod.id);
      if (flashItem?.maxQtyPerOrder && flashItem.maxQtyPerOrder > 0 && line.quantity > flashItem.maxQtyPerOrder) {
        throw new Error(`الحد الأقصى للطلب من «${prod.name}» في الفلاش سيل هو ${flashItem.maxQtyPerOrder}.`);
      }

      const flashPrice = activeFlash ? getFlashUnitPrice(activeFlash, prod.id, isWholesaleApproved) : null;
      const useWholesale =
        flashPrice == null &&
        isWholesaleApproved &&
        prod.wholesalePrice > 0 &&
        line.quantity >= Math.max(1, prod.wholesaleMinQty);

      const unitPrice = roundTwo(
        flashPrice ??
          (useWholesale
            ? prod.wholesalePrice
            : effectiveRetailPriceWithShowcases(prod, activeBrandShowcases)),
      );
      const pricingMode = flashPrice != null ? 'flash_sale' : useWholesale ? 'wholesale' : 'retail';
      const lineTotal = roundTwo(unitPrice * line.quantity);
      subtotal = roundTwo(subtotal + lineTotal);

      const itemSnapshot: Record<string, unknown> = {
        productId: prod.id,
        productName: prod.name.trim() || 'منتج',
        sku: prod.sku.trim() || 'SKU',
        unit: prod.unit.trim() || 'قطعة',
        pricingMode,
        unitPrice,
        quantity: line.quantity,
        lineTotal,
      };
      if (prod.imageUrls?.[0]) itemSnapshot.imageUrl = prod.imageUrls[0];
      if (flashPrice != null && activeFlash) itemSnapshot.flashSaleId = activeFlash.id;
      if (line.selectedColor?.trim()) itemSnapshot.selectedColor = line.selectedColor.trim();
      if (line.selectedSize?.trim()) itemSnapshot.selectedSize = line.selectedSize.trim();
      itemMaps.push(itemSnapshot);

      stockUpdates.push({
        ref: snap.ref,
        stockQuantity: prod.stockQuantity - line.quantity,
        salesCount: Number(prod.salesCount || 0) + line.quantity,
      });
    }

    let appliedCouponCode = '';
    let discountAmount = 0;
    let shouldIncrementCoupon = false;
    let couponNextUsedCount = 0;

    if (couponSnap && couponSnap.exists()) {
      const c = { id: couponSnap.id, ...(couponSnap.data() as Omit<Coupon, 'id'>) };
      const notExpired = !c.expiresAt || toMillis(c.expiresAt) >= nowMillis;
      const withinLimit = !c.usageLimit || c.usedCount < c.usageLimit;
      if (c.isActive && notExpired && withinLimit && subtotal >= c.minOrderAmount) {
        discountAmount = calculateCouponDiscount(c, subtotal);
        if (discountAmount > 0) {
          appliedCouponCode = c.code.trim().toUpperCase();
          shouldIncrementCoupon = true;
          couponNextUsedCount = Number(c.usedCount || 0) + 1;
        }
      }
    }

    const storeData = storeSnap.exists() ? storeSnap.data() : null;
    const freeShippingEnabled = Boolean(storeData?.freeShippingEnabled ?? false);
    const freeShippingThreshold = Math.max(0, Number(storeData?.freeShippingThreshold ?? 500) || 0);
    const baseZoneFee = isPickup ? 0 : roundTwo(Math.max(0, Number(params.deliveryZone?.fee || 0)));
    const freeShippingEval = computeFreeShipping({
      netMerchandise: Math.max(0, subtotal - discountAmount),
      isStorePickup: isPickup,
      hasItems: itemMaps.length > 0,
      baseZoneFee,
      enabled: freeShippingEnabled,
      threshold: freeShippingThreshold,
    });
    const deliveryFee = freeShippingEval.deliveryFee;
    const totalAmount = roundTwo(Math.max(0, subtotal - discountAmount + deliveryFee));
    const deliveryOtpEnabled =
      storeData?.deliveryOtpEnabled !== undefined ? Boolean(storeData.deliveryOtpEnabled) : true;
    const deliveryOtp = !isPickup && deliveryOtpEnabled ? generateDeliveryOtp() : null;

    const rawTransferRef = params.transferReference?.trim() || '';
    const normalizedPhone = normalizeEgyptianPhone(params.senderPhone || rawTransferRef);
    const effectiveSenderPhone =
      normalizedPhone.length >= 8 ? normalizedPhone : (params.senderPhone?.trim() || '');
    const cleanTransferRef = effectiveSenderPhone || rawTransferRef;
    const cleanReceiptUrl = params.receiptImageUrl?.trim() || '';
    const cleanNotes = params.notes?.trim() || '';
    const cleanLandmark = isPickup ? '' : params.address?.landmark?.trim() || '';

    const resolvedRecipientName = isPickup
      ? params.pickupRecipientName?.trim() || customerName
      : params.address?.recipientName?.trim() || customerName;
    const resolvedPhone = isPickup
      ? params.pickupPhone?.trim() || String(userData.phone || '').trim() || '01000000000'
      : params.address?.phone?.trim() || '';
    const resolvedPickupStreet =
      params.storeAddress?.trim() && params.storeAddress.trim().length >= 4
        ? params.storeAddress.trim()
        : 'الاستلام مباشرة من مقر المتجر الرئيسي';

    const orderPayload: Record<string, unknown> = {
      userId: user.uid,
      orderNumber,
      customerName,
      customerEmail,
      accountTypeAtOrder,
      items: itemMaps,
      subtotal,
      discountAmount,
      deliveryFee,
      totalAmount,
      paymentMethod: params.paymentMethod,
      paymentStatus: 'pending_review',
      orderStatus: 'new_order',
      fulfillmentType: params.fulfillmentMethod,
      deliveryLabel: isPickup ? 'استلام من المتجر' : params.address?.label?.trim() || 'العنوان',
      recipientName: resolvedRecipientName,
      phone: resolvedPhone,
      governorate: isPickup ? 'المتجر' : params.address?.governorate?.trim() || '',
      city: isPickup ? 'الفرع الرئيسي' : params.address?.city?.trim() || '',
      streetAddress: isPickup ? resolvedPickupStreet : params.address?.streetAddress?.trim() || '',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };

    if (deliveryOtp) {
      orderPayload.deliveryOtp = deliveryOtp;
      orderPayload.deliveryOtpVerified = false;
    }
    if (appliedCouponCode) orderPayload.couponCode = appliedCouponCode;
    if (!isPickup && params.deliveryZone?.id) {
      orderPayload.deliveryZoneId = params.deliveryZone.id;
      orderPayload.deliveryZoneName = params.deliveryZone.name.trim();
    }
    if (cleanTransferRef) orderPayload.transferReference = cleanTransferRef;
    if (effectiveSenderPhone) orderPayload.senderPhone = effectiveSenderPhone;
    if (cleanReceiptUrl) orderPayload.receiptImageUrl = cleanReceiptUrl;
    if (cleanLandmark) orderPayload.landmark = cleanLandmark;
    if (!isPickup && params.address?.latitude != null && params.address?.longitude != null) {
      orderPayload.latitude = params.address.latitude;
      orderPayload.longitude = params.address.longitude;
    }
    if (cleanNotes) orderPayload.notes = cleanNotes;

    // 3. ATOMIC WRITES
    tx.set(orderRef, orderPayload);
    if (counterSnap.exists()) {
      tx.update(counterRef, { nextNumber: assignedSeq + 1, updatedAt: serverTimestamp() });
    } else {
      tx.set(counterRef, { nextNumber: assignedSeq + 1, updatedAt: serverTimestamp() });
    }
    tx.update(userRef, { lastOrderId: orderRef.id, updatedAt: serverTimestamp() });
    for (const s of stockUpdates) {
      tx.update(s.ref, {
        stockQuantity: s.stockQuantity,
        salesCount: s.salesCount,
        updatedAt: serverTimestamp(),
      });
    }
    if (shouldIncrementCoupon && couponSnap) {
      tx.update(couponSnap.ref, {
        usedCount: couponNextUsedCount,
        updatedAt: serverTimestamp(),
      });
    }
    for (const l of validLines) {
      tx.delete(
        doc(db, 'users', user.uid, 'cart', buildCartItemId(l.productId, l.selectedColor, l.selectedSize)),
      );
    }

    return { orderId: orderRef.id, orderNumber };
  });

  return result;
}

export async function cancelOrderByCustomer(orderId: string, reason?: string) {
  await updateDoc(doc(db, 'orders', orderId), {
    orderStatus: 'cancelled',
    statusNote: reason?.trim() || 'تم الإلغاء بناءً على طلب العميل',
    updatedAt: serverTimestamp(),
  });
}

// --- Reviews ---

export async function submitOrUpdateReview(params: {
  productId: string;
  productName: string;
  userName: string;
  rating: number;
  comment: string;
}) {
  const user = auth.currentUser;
  if (!user) throw new Error('يجب تسجيل الدخول أولًا.');
  const reviewId = `${params.productId.trim()}_${user.uid}`;
  const docRef = doc(db, 'reviews', reviewId);
  const existing = await getDoc(docRef);
  if (existing.exists()) {
    await updateDoc(docRef, {
      userName: params.userName.trim() || 'عميل المتجر',
      rating: Math.min(5, Math.max(1, params.rating)),
      comment: params.comment.trim(),
      updatedAt: serverTimestamp(),
    });
  } else {
    await setDoc(docRef, {
      productId: params.productId.trim(),
      productName: params.productName.trim() || 'منتج',
      userId: user.uid,
      userName: params.userName.trim() || 'عميل المتجر',
      rating: Math.min(5, Math.max(1, params.rating)),
      comment: params.comment.trim(),
      isHidden: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
}

export async function deleteReviewDoc(reviewId: string) {
  await deleteDoc(doc(db, 'reviews', reviewId));
}

export async function setReviewHidden(reviewId: string, hidden: boolean) {
  await updateDoc(doc(db, 'reviews', reviewId), {
    isHidden: hidden,
    updatedAt: serverTimestamp(),
  });
}

// --- Notifications ---

export async function sendNotificationToUser(params: {
  targetUid: string;
  title: string;
  message: string;
  type: NotificationType;
  referenceId?: string;
}) {
  if (!auth.currentUser || !params.targetUid.trim()) return;
  const docRef = doc(collection(db, 'users', params.targetUid.trim(), 'notifications'));
  const payload: Record<string, unknown> = {
    userId: params.targetUid.trim(),
    title: params.title.trim().slice(0, 120) || 'إشعار من المتجر',
    message: params.message.trim().slice(0, 500) || 'لديك تحديث جديد في حسابك.',
    type: params.type,
    isRead: false,
    createdAt: serverTimestamp(),
  };
  if (params.referenceId?.trim()) payload.referenceId = params.referenceId.trim().slice(0, 128);
  await setDoc(docRef, payload);
}

export async function broadcastNotificationToUsers(params: {
  targetUids: string[];
  title: string;
  message: string;
  type?: NotificationType;
  referenceId?: string;
}): Promise<number> {
  const cleanUids = Array.from(new Set(params.targetUids.map((u) => u.trim()).filter(Boolean)));
  if (!cleanUids.length) return 0;
  const cleanRef = params.referenceId?.trim().slice(0, 128) || '';
  let count = 0;
  for (let i = 0; i < cleanUids.length; i += 400) {
    const chunk = cleanUids.slice(i, i + 400);
    const batch = writeBatch(db);
    chunk.forEach((uid) => {
      const ref = doc(collection(db, 'users', uid, 'notifications'));
      const payload: Record<string, unknown> = {
        userId: uid,
        title: params.title.trim().slice(0, 120),
        message: params.message.trim().slice(0, 500),
        type: params.type || 'offer_promo',
        isRead: false,
        createdAt: serverTimestamp(),
      };
      if (cleanRef) payload.referenceId = cleanRef;
      batch.set(ref, payload);
    });
    await batch.commit();
    count += chunk.length;
  }
  return count;
}

export async function markNotificationRead(uid: string, notifId: string) {
  await updateDoc(doc(db, 'users', uid, 'notifications', notifId), { isRead: true });
}

export async function markAllNotificationsRead(uid: string, unreadIds: string[]) {
  const clean = Array.from(new Set(unreadIds.filter(Boolean)));
  for (let i = 0; i < clean.length; i += 400) {
    const batch = writeBatch(db);
    clean.slice(i, i + 400).forEach((id) => {
      batch.update(doc(db, 'users', uid, 'notifications', id), { isRead: true });
    });
    await batch.commit();
  }
}

export async function deleteNotificationDoc(uid: string, notifId: string) {
  await deleteDoc(doc(db, 'users', uid, 'notifications', notifId));
}

// --- Admin CRUD Functions ---

export async function saveProductDoc(product: Partial<Product> & { id?: string }) {
  const colRef = collection(db, 'products');
  const isNew = !product.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, product.id);
  const cleanSub = product.subCategoryId?.trim() || '';
  const cleanBarcode = product.barcode?.trim() || '';
  const cleanQr = product.qrCode?.trim() || '';
  const retailPrice = Number(product.retailPrice || 0);
  const rawDiscount = product.retailDiscountPrice != null ? Number(product.retailDiscountPrice) : null;
  const validDiscount = rawDiscount && rawDiscount > 0 && rawDiscount < retailPrice ? rawDiscount : null;

  const cleanColors = Array.from(
    new Set((product.availableColors || []).map((c) => c.trim()).filter(Boolean)),
  ).slice(0, 30);
  const cleanSizes = Array.from(
    new Set((product.availableSizes || []).map((s) => s.trim()).filter(Boolean)),
  ).slice(0, 30);
  const cleanCompanyId = product.companyId?.trim() || '';
  const cleanBrandShowcaseId = product.brandShowcaseId?.trim() || '';
  const cleanBrandCategoryId = cleanBrandShowcaseId ? product.brandCategoryId?.trim() || '' : '';
  const isExclusiveBrand = Boolean(
    product.exclusiveToBrandShowcase && (cleanCompanyId || cleanBrandShowcaseId),
  );

  if (isNew) {
    const payload: Record<string, unknown> = {
      name: (product.name || '').trim(),
      description: (product.description || '').trim(),
      categoryId: (product.categoryId || '').trim(),
      sku: (product.sku || '').trim(),
      imageUrls: (product.imageUrls || []).slice(0, 10),
      retailPrice,
      wholesalePrice: Math.max(0, Number(product.wholesalePrice || 0)),
      wholesaleMinQty: Math.max(1, Number(product.wholesaleMinQty || 1)),
      unit: (product.unit || 'قطعة').trim(),
      stockQuantity: Math.max(0, Number(product.stockQuantity || 0)),
      lowStockThreshold: Math.max(0, Number(product.lowStockThreshold ?? 5)),
      isFeatured: Boolean(product.isFeatured),
      isOffer: Boolean(product.isOffer),
      isActive: product.isActive ?? true,
      exclusiveToBrandShowcase: isExclusiveBrand,
      salesCount: Math.max(0, Number(product.salesCount || 0)),
      averageRating: Math.min(5, Math.max(0, Number(product.averageRating || 0))),
      reviewCount: Math.max(0, Number(product.reviewCount || 0)),
      availableColors: cleanColors,
      availableSizes: cleanSizes,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (cleanSub) payload.subCategoryId = cleanSub;
    if (cleanBarcode) payload.barcode = cleanBarcode;
    if (cleanQr) payload.qrCode = cleanQr;
    if (validDiscount) payload.retailDiscountPrice = validDiscount;
    if (cleanCompanyId) payload.companyId = cleanCompanyId;
    if (cleanBrandShowcaseId) payload.brandShowcaseId = cleanBrandShowcaseId;
    if (cleanBrandCategoryId) payload.brandCategoryId = cleanBrandCategoryId;
    await setDoc(docRef, payload);
  } else {
    const updates: Record<string, unknown> = {
      name: (product.name || '').trim(),
      description: (product.description || '').trim(),
      categoryId: (product.categoryId || '').trim(),
      subCategoryId: cleanSub || deleteField(),
      sku: (product.sku || '').trim(),
      barcode: cleanBarcode || deleteField(),
      qrCode: cleanQr || deleteField(),
      imageUrls: (product.imageUrls || []).slice(0, 10),
      retailPrice,
      retailDiscountPrice: validDiscount || deleteField(),
      wholesalePrice: Math.max(0, Number(product.wholesalePrice || 0)),
      wholesaleMinQty: Math.max(1, Number(product.wholesaleMinQty || 1)),
      unit: (product.unit || 'قطعة').trim(),
      stockQuantity: Math.max(0, Number(product.stockQuantity || 0)),
      lowStockThreshold: Math.max(0, Number(product.lowStockThreshold ?? 5)),
      isFeatured: Boolean(product.isFeatured),
      isOffer: Boolean(product.isOffer),
      isActive: product.isActive ?? true,
      companyId: cleanCompanyId || deleteField(),
      brandShowcaseId: cleanBrandShowcaseId || deleteField(),
      brandCategoryId: cleanBrandCategoryId || deleteField(),
      exclusiveToBrandShowcase: isExclusiveBrand,
      availableColors: cleanColors,
      availableSizes: cleanSizes,
      updatedAt: serverTimestamp(),
    };
    await updateDoc(docRef, updates);
  }
  return docRef.id;
}

export async function updateProductStock(
  productId: string,
  stockQuantity: number,
  lowStockThreshold: number,
  isActive: boolean,
) {
  await updateDoc(doc(db, 'products', productId), {
    stockQuantity: Math.max(0, stockQuantity),
    lowStockThreshold: Math.max(0, lowStockThreshold),
    isActive,
    updatedAt: serverTimestamp(),
  });
}

export async function updateProductOfferSettings(
  productId: string,
  retailDiscountPrice: number | null,
  isOffer: boolean,
  isFeatured: boolean,
) {
  await updateDoc(doc(db, 'products', productId), {
    retailDiscountPrice: retailDiscountPrice && retailDiscountPrice > 0 ? retailDiscountPrice : deleteField(),
    isOffer,
    isFeatured,
    updatedAt: serverTimestamp(),
  });
}

export async function deleteProductDoc(productId: string) {
  await deleteDoc(doc(db, 'products', productId));
}

export async function saveCategoryDoc(cat: Partial<Category> & { id?: string }) {
  const colRef = collection(db, 'categories');
  const isNew = !cat.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, cat.id);
  const cleanDesc = cat.description?.trim() || '';
  const cleanParent = cat.parentId?.trim() || '';
  const cleanImg = cat.imageUrl?.trim() || '';

  if (isNew) {
    const payload: Record<string, unknown> = {
      name: (cat.name || '').trim(),
      displayOrder: Math.max(0, Number(cat.displayOrder || 0)),
      isActive: cat.isActive ?? true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (cleanDesc) payload.description = cleanDesc;
    if (cleanParent) payload.parentId = cleanParent;
    if (cleanImg) payload.imageUrl = cleanImg;
    await setDoc(docRef, payload);
  } else {
    await updateDoc(docRef, {
      name: (cat.name || '').trim(),
      description: cleanDesc || deleteField(),
      parentId: cleanParent || deleteField(),
      imageUrl: cleanImg || deleteField(),
      displayOrder: Math.max(0, Number(cat.displayOrder || 0)),
      isActive: cat.isActive ?? true,
      updatedAt: serverTimestamp(),
    });
  }
  return docRef.id;
}

export async function deleteCategoryDoc(catId: string) {
  await deleteDoc(doc(db, 'categories', catId));
}

export async function saveBannerDoc(banner: Partial<Banner> & { id?: string }) {
  const colRef = collection(db, 'banners');
  const isNew = !banner.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, banner.id);
  const cleanSubtitle = banner.subtitle?.trim() || '';
  const mediaType =
    banner.mediaType === 'video'
      ? 'video'
      : banner.mediaType === 'animated_image'
      ? 'animated_image'
      : 'image';
  const cleanVideoUrl = mediaType === 'video' ? (banner.videoUrl || '').trim() : '';
  const cleanImageUrl =
    mediaType === 'video'
      ? (banner.imageUrl || '').trim() || deriveVideoPosterUrl(cleanVideoUrl)
      : (banner.imageUrl || '').trim();
  const targetType = banner.targetType || 'none';
  const cleanTargetId =
    targetType === 'category' || targetType === 'product' ? banner.targetId?.trim() || '' : '';
  const cleanDuration = Math.min(120, Math.max(1, Math.round(Number(banner.durationSeconds || (mediaType === 'video' ? 15 : 5)))));

  if (isNew) {
    const payload: Record<string, unknown> = {
      title: (banner.title || '').trim(),
      imageUrl: cleanImageUrl,
      mediaType,
      videoMuted: mediaType === 'video' && Boolean(banner.videoMuted),
      targetType,
      placement: banner.placement || 'top_home',
      bannerStyle: banner.bannerStyle || 'normal',
      durationSeconds: cleanDuration,
      displayOrder: Math.min(1000, Math.max(0, Number(banner.displayOrder || 0))),
      isActive: banner.isActive ?? true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (cleanSubtitle) payload.subtitle = cleanSubtitle;
    if (cleanVideoUrl) payload.videoUrl = cleanVideoUrl;
    if (cleanTargetId) payload.targetId = cleanTargetId;
    await setDoc(docRef, payload);
  } else {
    await updateDoc(docRef, {
      title: (banner.title || '').trim(),
      subtitle: cleanSubtitle || deleteField(),
      imageUrl: cleanImageUrl,
      mediaType,
      videoUrl: cleanVideoUrl || deleteField(),
      videoMuted: mediaType === 'video' && Boolean(banner.videoMuted),
      targetType,
      targetId: cleanTargetId || deleteField(),
      placement: banner.placement || 'top_home',
      bannerStyle: banner.bannerStyle || 'normal',
      durationSeconds: cleanDuration,
      displayOrder: Math.min(1000, Math.max(0, Number(banner.displayOrder || 0))),
      isActive: banner.isActive ?? true,
      updatedAt: serverTimestamp(),
    });
  }
  return docRef.id;
}

export async function deleteBannerDoc(bannerId: string) {
  await deleteDoc(doc(db, 'banners', bannerId));
}

export async function saveCompanyDoc(company: Partial<StoreCompany> & { id?: string }) {
  const colRef = collection(db, 'companies');
  const isNew = !company.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, company.id);
  const cleanName = (company.name || '').trim();
  const cleanImg = company.imageUrl?.trim() || '';
  const displayOrder = Math.min(1000, Math.max(0, Number(company.displayOrder || 0)));

  if (isNew) {
    const payload: Record<string, unknown> = {
      name: cleanName,
      displayOrder,
      isActive: company.isActive ?? true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (cleanImg) payload.imageUrl = cleanImg;
    await setDoc(docRef, payload);
  } else {
    await updateDoc(docRef, {
      name: cleanName,
      imageUrl: cleanImg || deleteField(),
      displayOrder,
      isActive: company.isActive ?? true,
      updatedAt: serverTimestamp(),
    });
  }
  return docRef.id;
}

export async function deleteCompanyDoc(companyId: string) {
  const cleanId = companyId.trim();
  if (!cleanId) return;
  try {
    const linkedSnap = await getDocs(
      query(collection(db, 'products'), where('companyId', '==', cleanId)),
    );
    for (let i = 0; i < linkedSnap.docs.length; i += 400) {
      const batch = writeBatch(db);
      linkedSnap.docs.slice(i, i + 400).forEach((d) => {
        const hasLegacyShowcase = Boolean(d.data()?.brandShowcaseId);
        batch.update(d.ref, {
          companyId: deleteField(),
          ...(hasLegacyShowcase ? {} : { exclusiveToBrandShowcase: false }),
          updatedAt: serverTimestamp(),
        });
      });
      await batch.commit();
    }
  } catch {
    // non-fatal cleanup fallback
  }
  await deleteDoc(doc(db, 'companies', cleanId));
}

export async function saveBrandShowcaseDoc(showcase: Partial<BrandShowcase> & { id?: string }) {
  const colRef = collection(db, 'brandShowcases');
  const isNew = !showcase.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, showcase.id);
  const cleanSubtitle = showcase.subtitle?.trim() || '';
  const cleanCompanyId = showcase.companyId?.trim() || '';
  const cleanCompanyImageUrl = showcase.companyImageUrl?.trim() || '';
  const cleanBannerImageUrl = showcase.bannerImageUrl?.trim() || '';
  const safeDiscount = Math.min(95, Math.max(0, roundTwo(Number(showcase.discountPercent || 0))));
  const cleanCategories = (showcase.categories || [])
    .filter((c) => c.name?.trim())
    .slice(0, 40)
    .map((c, idx) => ({
      id: (c.id || '').trim() || `bcat_${Date.now()}_${idx}`,
      name: c.name.trim(),
      imageUrl: (c.imageUrl || '').trim(),
      displayOrder: Math.max(0, Number(c.displayOrder ?? idx)),
    }));

  if (isNew) {
    const payload: Record<string, unknown> = {
      title: (showcase.title || '').trim(),
      accentColorHex: (showcase.accentColorHex || '#FCE4EC').trim(),
      placement: showcase.placement || 'below_categories',
      displayOrder: Math.min(1000, Math.max(0, Number(showcase.displayOrder || 0))),
      discountPercent: safeDiscount,
      isActive: showcase.isActive ?? true,
      categories: cleanCategories,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (cleanSubtitle) payload.subtitle = cleanSubtitle;
    if (cleanCompanyId) payload.companyId = cleanCompanyId;
    if (cleanCompanyImageUrl) payload.companyImageUrl = cleanCompanyImageUrl;
    if (cleanBannerImageUrl) payload.bannerImageUrl = cleanBannerImageUrl;
    await setDoc(docRef, payload);
  } else {
    await updateDoc(docRef, {
      title: (showcase.title || '').trim(),
      subtitle: cleanSubtitle || deleteField(),
      companyId: cleanCompanyId || deleteField(),
      companyImageUrl: cleanCompanyImageUrl || deleteField(),
      bannerImageUrl: cleanBannerImageUrl || deleteField(),
      discountPercent: safeDiscount,
      accentColorHex: (showcase.accentColorHex || '#FCE4EC').trim(),
      placement: showcase.placement || 'below_categories',
      displayOrder: Math.min(1000, Math.max(0, Number(showcase.displayOrder || 0))),
      isActive: showcase.isActive ?? true,
      categories: cleanCategories,
      updatedAt: serverTimestamp(),
    });
  }
  return docRef.id;
}

export async function deleteBrandShowcaseDoc(showcaseId: string, linkedProducts: Product[] = []) {
  for (const prod of linkedProducts) {
    await saveProductDoc({
      ...prod,
      brandShowcaseId: undefined,
      brandCategoryId: undefined,
      exclusiveToBrandShowcase: Boolean(prod.companyId && prod.exclusiveToBrandShowcase),
    });
  }
  await deleteDoc(doc(db, 'brandShowcases', showcaseId));
}

export async function saveFlashSaleDoc(sale: {
  id?: string;
  title: string;
  badgeText: string;
  startsAtDate: Date;
  endsAtDate: Date;
  isActive: boolean;
  items: FlashSale['items'];
}) {
  if (sale.endsAtDate.getTime() <= sale.startsAtDate.getTime()) {
    throw new Error('يجب أن يكون وقت الانتهاء بعد وقت البداية.');
  }
  const validItems = sale.items
    .filter((i) => i.productId.trim() && i.flashRetailPrice > 0)
    .map((i) => {
      const m: Record<string, unknown> = {
        productId: i.productId.trim(),
        flashRetailPrice: Number(i.flashRetailPrice),
      };
      if (i.flashWholesalePrice && i.flashWholesalePrice > 0) {
        m.flashWholesalePrice = Number(i.flashWholesalePrice);
      }
      if (i.maxQtyPerOrder && i.maxQtyPerOrder > 0) {
        m.maxQtyPerOrder = Math.floor(i.maxQtyPerOrder);
      }
      return m;
    });

  if (!validItems.length) throw new Error('اختر منتجًا واحدًا على الأقل بسعر فلاش سيل صالح.');

  const colRef = collection(db, 'flashSales');
  const isNew = !sale.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, sale.id);

  if (isNew) {
    await setDoc(docRef, {
      title: sale.title.trim(),
      badgeText: sale.badgeText.trim(),
      startsAt: Timestamp.fromDate(sale.startsAtDate),
      endsAt: Timestamp.fromDate(sale.endsAtDate),
      isActive: sale.isActive,
      items: validItems,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } else {
    await updateDoc(docRef, {
      title: sale.title.trim(),
      badgeText: sale.badgeText.trim(),
      startsAt: Timestamp.fromDate(sale.startsAtDate),
      endsAt: Timestamp.fromDate(sale.endsAtDate),
      isActive: sale.isActive,
      items: validItems,
      updatedAt: serverTimestamp(),
    });
  }
  return docRef.id;
}

export async function toggleFlashSaleActive(flashSaleId: string, isActive: boolean) {
  await updateDoc(doc(db, 'flashSales', flashSaleId), {
    isActive,
    updatedAt: serverTimestamp(),
  });
}

export async function deleteFlashSaleDoc(flashSaleId: string) {
  await deleteDoc(doc(db, 'flashSales', flashSaleId));
}

export async function updateOrderStatusAdmin(params: {
  orderId: string;
  newStatus: OrderStatus;
  statusNote?: string;
}) {
  const updates: Record<string, unknown> = {
    orderStatus: params.newStatus,
    updatedAt: serverTimestamp(),
  };
  if (params.statusNote?.trim()) updates.statusNote = params.statusNote.trim();
  await updateDoc(doc(db, 'orders', params.orderId), updates);
}

export async function updatePaymentStatusAdmin(params: {
  orderId: string;
  paymentStatus: PaymentStatus;
  statusNote?: string;
}) {
  const updates: Record<string, unknown> = {
    paymentStatus: params.paymentStatus,
    updatedAt: serverTimestamp(),
  };
  if (params.statusNote?.trim()) updates.statusNote = params.statusNote.trim();
  await updateDoc(doc(db, 'orders', params.orderId), updates);
}

export async function assignOrderToDriverAdmin(params: {
  orderId: string;
  driverId: string;
  driverName: string;
  driverPhone?: string;
}) {
  const cleanName = params.driverName.trim() || 'مندوب التوصيل';
  await updateDoc(doc(db, 'orders', params.orderId), {
    assignedDriverId: params.driverId.trim(),
    assignedDriverName: cleanName,
    assignedDriverPhone: params.driverPhone?.trim() || deleteField(),
    assignedAt: serverTimestamp(),
    orderStatus: 'out_for_delivery',
    statusNote: `تم توجيه الطلب للتوصيل مع المندوب: ${cleanName}`,
    updatedAt: serverTimestamp(),
  });
}

export async function markOrderDeliveredByDriver(
  orderId: string,
  note?: string,
  deliveryOtpVerified?: boolean,
) {
  const updates: Record<string, unknown> = {
    orderStatus: 'delivered',
    statusNote: note?.trim() || 'تم تسليم الطلب للعميل بواسطة موظف التوصيل',
    updatedAt: serverTimestamp(),
  };
  if (deliveryOtpVerified === true) {
    updates.deliveryOtpVerified = true;
  }
  await updateDoc(doc(db, 'orders', orderId), updates);
}

export async function deleteOrderReceiptImage(orderId: string) {
  await updateDoc(doc(db, 'orders', orderId), {
    receiptImageUrl: deleteField(),
    updatedAt: serverTimestamp(),
  });
}

export async function deleteOrderReceiptsBatch(orderIds: string[]) {
  const clean = Array.from(new Set(orderIds.filter(Boolean)));
  for (let i = 0; i < clean.length; i += 400) {
    const batch = writeBatch(db);
    clean.slice(i, i + 400).forEach((id) => {
      batch.update(doc(db, 'orders', id), {
        receiptImageUrl: deleteField(),
        updatedAt: serverTimestamp(),
      });
    });
    await batch.commit();
  }
  return clean.length;
}

export async function updateWholesaleDecision(targetUid: string, approved: boolean) {
  await updateDoc(doc(db, 'users', targetUid), {
    accountType: approved ? 'wholesale' : 'retail',
    wholesaleStatus: approved ? 'approved' : 'rejected',
    updatedAt: serverTimestamp(),
  });
}

export async function assignStaffMemberDoc(params: {
  targetUser: UserProfile;
  role: StaffRole;
  customPermissions?: StaffPermission[];
  isActive?: boolean;
}) {
  if (params.role === 'customer') throw new Error('استخدم إلغاء الصلاحية بدلًا من دور عميل.');
  const staffRef = doc(db, 'staff', params.targetUser.uid);
  const userRef = doc(db, 'users', params.targetUser.uid);
  const permissions = params.customPermissions ?? STAFF_ROLE_META[params.role].permissions;
  const isActive = params.isActive ?? true;
  const existing = await getDoc(staffRef);
  const batch = writeBatch(db);

  if (existing.exists()) {
    batch.update(staffRef, {
      fullName: params.targetUser.fullName.trim(),
      email: params.targetUser.email.trim(),
      role: params.role,
      permissions,
      isActive,
      updatedAt: serverTimestamp(),
    });
  } else {
    batch.set(staffRef, {
      uid: params.targetUser.uid,
      fullName: params.targetUser.fullName.trim(),
      email: params.targetUser.email.trim(),
      role: params.role,
      permissions,
      isActive,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }

  batch.update(userRef, {
    role: isActive ? params.role : 'customer',
    updatedAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function removeStaffMemberDoc(targetUid: string) {
  const batch = writeBatch(db);
  batch.delete(doc(db, 'staff', targetUid));
  batch.update(doc(db, 'users', targetUid), {
    role: 'customer',
    updatedAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function saveCouponDoc(coupon: Partial<Coupon> & { id?: string }) {
  const colRef = collection(db, 'coupons');
  const isNew = !coupon.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, coupon.id);

  const payload: Record<string, unknown> = {
    code: (coupon.code || '').trim().toUpperCase(),
    discountType: coupon.discountType === 'percent' ? 'percent' : 'fixed',
    discountValue: Math.max(0.01, Number(coupon.discountValue || 0)),
    minOrderAmount: Math.max(0, Number(coupon.minOrderAmount || 0)),
    usedCount: Number(coupon.usedCount || 0),
    isActive: coupon.isActive ?? true,
    updatedAt: serverTimestamp(),
  };

  if (coupon.maxDiscountAmount && coupon.maxDiscountAmount > 0) {
    payload.maxDiscountAmount = Number(coupon.maxDiscountAmount);
  } else if (!isNew) {
    payload.maxDiscountAmount = deleteField();
  }

  if (coupon.usageLimit && coupon.usageLimit > 0) {
    payload.usageLimit = Math.floor(coupon.usageLimit);
  } else if (!isNew) {
    payload.usageLimit = deleteField();
  }

  if (coupon.expiresAt) {
    payload.expiresAt = coupon.expiresAt;
  } else if (!isNew) {
    payload.expiresAt = deleteField();
  }

  if (isNew) {
    payload.createdAt = serverTimestamp();
    await setDoc(docRef, payload);
  } else {
    await updateDoc(docRef, payload);
  }
}

export async function deleteCouponDoc(couponId: string) {
  await deleteDoc(doc(db, 'coupons', couponId));
}

export async function saveDeliveryZoneDoc(zone: Partial<DeliveryZone> & { id?: string }) {
  const colRef = collection(db, 'deliveryZones');
  const isNew = !zone.id;
  const docRef = isNew ? doc(colRef) : doc(colRef, zone.id);

  const payload: Record<string, unknown> = {
    name: (zone.name || '').trim(),
    governorate: (zone.governorate || '').trim(),
    fee: Math.max(0, Number(zone.fee || 0)),
    estimatedDuration: (zone.estimatedDuration || 'خلال ٢٤ - ٤٨ ساعة').trim(),
    isActive: zone.isActive ?? true,
    updatedAt: serverTimestamp(),
  };
  if (isNew) {
    payload.createdAt = serverTimestamp();
    await setDoc(docRef, payload);
  } else {
    await updateDoc(docRef, payload);
  }
}

export async function deleteDeliveryZoneDoc(zoneId: string) {
  await deleteDoc(doc(db, 'deliveryZones', zoneId));
}

export async function savePaymentSettingsDoc(settings: StorePaymentSettings) {
  await setDoc(
    doc(db, 'settings', 'payments'),
    {
      vodafoneCashNumber: settings.vodafoneCashNumber.trim(),
      instaPayAddress: settings.instaPayAddress.trim(),
      paymentInstructions: settings.paymentInstructions.trim(),
      codEnabled: settings.codEnabled,
      instaPayEnabled: settings.instaPayEnabled,
      vodafoneCashEnabled: settings.vodafoneCashEnabled,
      updatedAt: serverTimestamp(),
    },
    { merge: true },
  );
}

export async function saveStoreGeneralSettingsDoc(settings: StoreGeneralSettings) {
  const cleanSocial = settings.socialPages
    .filter((s) => s.title.trim() && s.url.trim())
    .map((s) => ({ title: s.title.trim(), url: s.url.trim(), channel: s.channel || 'retail' }));
  const cleanRetailPhones = settings.retailPhones
    .filter((p) => p.phoneNumber.trim())
    .map((p) => ({ label: p.label.trim(), phoneNumber: p.phoneNumber.trim(), channel: 'retail' }));
  const cleanWholesalePhones = settings.wholesalePhones
    .filter((p) => p.phoneNumber.trim())
    .map((p) => ({ label: p.label.trim(), phoneNumber: p.phoneNumber.trim(), channel: 'wholesale' }));

  await setDoc(doc(db, 'settings', 'store'), {
    storeName: settings.storeName.trim() || 'الإيمان للتجارة',
    tagline: settings.tagline.trim() || 'تسوق بذكاء .. عيش أفضل',
    ownerEmail: settings.ownerEmail.trim().toLowerCase() || BOOTSTRAPPED_OWNER_EMAIL,
    supportPhone: settings.supportPhone.trim(),
    whatsappNumber: settings.whatsappNumber.trim(),
    storeAddress: settings.storeAddress.trim(),
    announcementText: settings.announcementText.trim(),
    aboutStoreDescription: settings.aboutStoreDescription.trim(),
    retailFacebookUrl: settings.retailFacebookUrl.trim(),
    wholesaleFacebookUrl: settings.wholesaleFacebookUrl.trim(),
    socialPages: cleanSocial,
    retailPhones: cleanRetailPhones,
    wholesalePhones: cleanWholesalePhones,
    showFeaturedSection: settings.showFeaturedSection,
    showDealsSection: settings.showDealsSection,
    showBestsellersSection: settings.showBestsellersSection,
    showRecommendedSection: settings.showRecommendedSection,
    showHeroSection: settings.showHeroSection,
    heroBadgeText: settings.heroBadgeText.trim(),
    heroTitleText: settings.heroTitleText.trim(),
    heroSubtitleText: settings.heroSubtitleText.trim(),
    heroCtaText: settings.heroCtaText.trim() || 'تسوق الآن',
    homeCategoryStyle: settings.homeCategoryStyle,
    categoriesTabStyle: settings.categoriesTabStyle,
    companyOfferBadgeStyle: settings.companyOfferBadgeStyle || 'gold_ticket',
    numeralFormat: settings.numeralFormat,
    privacyPolicyUrl: settings.privacyPolicyUrl.trim(),
    returnPolicyUrl: settings.returnPolicyUrl.trim(),
    accountDeletionUrl: settings.accountDeletionUrl.trim(),
    privacyPolicyText: settings.privacyPolicyText.trim() || DEFAULT_PRIVACY_POLICY_TEXT,
    returnPolicyText: settings.returnPolicyText.trim() || DEFAULT_RETURN_POLICY_TEXT,
    termsOfServiceText: settings.termsOfServiceText.trim() || DEFAULT_TERMS_OF_SERVICE_TEXT,
    showProductColors: settings.showProductColors ?? true,
    showProductSizes: settings.showProductSizes ?? true,
    freeShippingEnabled: Boolean(settings.freeShippingEnabled),
    freeShippingThreshold: Math.max(0, Number(settings.freeShippingThreshold || 0)),
    deliveryOtpEnabled: settings.deliveryOtpEnabled ?? true,
    introVideoEnabled: Boolean(settings.introVideoEnabled ?? false),
    introMediaType: settings.introMediaType === 'animated_image' ? 'animated_image' : 'video',
    introVideoUrl: (settings.introVideoUrl || '').trim(),
    introImageUrl: (settings.introImageUrl || '').trim(),
    introVideoDurationSeconds: Math.min(120, Math.max(0, Math.round(Number(settings.introVideoDurationSeconds || 0)))),
    introVideoTitle: (settings.introVideoTitle || '').trim().slice(0, 100),
    introVideoMuted: Boolean(settings.introVideoMuted ?? false),
    tickerEnabled: Boolean(settings.tickerEnabled ?? false),
    tickerText: (settings.tickerText || '').trim().slice(0, 1000),
    tickerPlacement: [
      'top_home',
      'below_hero',
      'below_categories',
      'below_featured',
      'below_deals',
      'below_newest',
      'below_bestsellers',
      'below_recommended',
    ].includes(settings.tickerPlacement)
      ? settings.tickerPlacement
      : 'top_home',
    tickerBackgroundColor: /^#[0-9A-Fa-f]{6}$/.test((settings.tickerBackgroundColor || '').trim())
      ? settings.tickerBackgroundColor.trim().toUpperCase()
      : '#181A20',
    tickerBorderColor: /^#[0-9A-Fa-f]{6}$/.test((settings.tickerBorderColor || '').trim())
      ? settings.tickerBorderColor.trim().toUpperCase()
      : '#FFD000',
    tickerTextColor: /^#[0-9A-Fa-f]{6}$/.test((settings.tickerTextColor || '').trim())
      ? settings.tickerTextColor.trim().toUpperCase()
      : '#FFFFFF',
    tickerFontSizeSp: Math.min(24, Math.max(11, Math.round(Number(settings.tickerFontSizeSp || 14)))),
    tickerFontWeight: ['light', 'normal', 'bold', 'extra_bold'].includes(settings.tickerFontWeight)
      ? settings.tickerFontWeight
      : 'bold',
    tickerItalic: Boolean(settings.tickerItalic ?? false),
    tickerInlineImages: (settings.tickerInlineImages || [])
      .map((img) => ({ id: (img.id || '').trim(), url: (img.url || '').trim() }))
      .filter((img) => img.id && img.url)
      .slice(0, 10),
    appIconTheme:
      settings.appIconTheme === 'offers' ||
      settings.appIconTheme === 'ramadan' ||
      settings.appIconTheme === 'eid'
        ? settings.appIconTheme
        : 'default',
    productCardBorderStyle: [
      'default',
      'gold',
      'emerald',
      'royal_blue',
      'ruby',
      'purple',
      'charcoal',
      'custom',
      'none',
    ].includes(settings.productCardBorderStyle)
      ? settings.productCardBorderStyle
      : 'default',
    productCardBorderColor: /^#[0-9A-Fa-f]{6}$/.test((settings.productCardBorderColor || '').trim())
      ? settings.productCardBorderColor.trim().toUpperCase()
      : '#D4AF37',
    productCardShape: ['rounded', 'circular', 'rectangle', 'cut_corner', 'asymmetric', 'arch'].includes(
      settings.productCardShape,
    )
      ? settings.productCardShape
      : 'rounded',
    productDiscountBadgeStyle: [
      'pill',
      'ribbon',
      'starburst',
      'price_tag',
      'gold_luxury',
      'capsule_outline',
    ].includes(settings.productDiscountBadgeStyle)
      ? settings.productDiscountBadgeStyle
      : 'pill',
    updatedAt: serverTimestamp(),
  });
}

// --- Back-in-Stock Alerts & Zero-Result Search Misses ---

export function subscribeUserStockAlertProductIds(
  uid: string,
  cb: (productIds: string[]) => void,
): Unsubscribe {
  const q = query(collection(db, 'stockAlerts'), where('userId', '==', uid));
  return onSnapshot(
    q,
    (snap) => {
      const ids = snap.docs
        .map((d) => String(d.data()?.productId || '').trim())
        .filter(Boolean);
      cb(Array.from(new Set(ids)));
    },
    () => cb([]),
  );
}

export async function toggleStockAlertItem(
  uid: string,
  product: Pick<Product, 'id' | 'name'>,
  currentlySubscribed: boolean,
): Promise<boolean> {
  const cleanProductId = product.id.trim();
  if (!uid.trim() || !cleanProductId) {
    throw new Error('تعذر تحديد المنتج أو الحساب.');
  }
  const alertId = `${cleanProductId}_${uid.trim()}`;
  const docRef = doc(db, 'stockAlerts', alertId);
  if (currentlySubscribed) {
    await deleteDoc(docRef);
    return false;
  }
  await setDoc(docRef, {
    productId: cleanProductId,
    productName: (product.name || 'المنتج').trim().slice(0, 120) || 'المنتج',
    userId: uid.trim(),
    createdAt: serverTimestamp(),
  });
  return true;
}

export function subscribeAllStockAlertCounts(
  cb: (counts: Record<string, number>) => void,
): Unsubscribe {
  return onSnapshot(
    collection(db, 'stockAlerts'),
    (snap) => {
      const counts: Record<string, number> = {};
      snap.docs.forEach((d) => {
        const pid = String(d.data()?.productId || '').trim();
        if (pid) counts[pid] = (counts[pid] || 0) + 1;
      });
      cb(counts);
    },
    () => cb({}),
  );
}

export async function dispatchRestockNotificationsAndClearAlerts(
  productId: string,
  productName: string,
): Promise<number> {
  const cleanId = productId.trim();
  if (!auth.currentUser || !cleanId) return 0;
  const snap = await getDocs(query(collection(db, 'stockAlerts'), where('productId', '==', cleanId)));
  if (snap.empty) return 0;

  const userIds = Array.from(
    new Set(
      snap.docs
        .map((d) => String(d.data()?.userId || '').trim())
        .filter(Boolean),
    ),
  );
  const cleanName = (productName || 'المنتج').trim().slice(0, 100) || 'المنتج';

  if (userIds.length > 0) {
    await broadcastNotificationToUsers({
      targetUids: userIds,
      title: 'توفر المنتج من جديد!',
      message: `المنتج «${cleanName}» أصبح متوفرًا الآن في المتجر — اطلبه قبل نفاد الكمية.`,
      type: 'stock_alert',
      referenceId: cleanId,
    });
  }

  for (let i = 0; i < snap.docs.length; i += 400) {
    const batch = writeBatch(db);
    snap.docs.slice(i, i + 400).forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }
  return userIds.length;
}

export async function logSearchMiss(
  rawQuery: string,
  source: 'android' | 'web' = 'web',
): Promise<void> {
  if (!auth.currentUser) return;
  const cleanQuery = rawQuery.trim().replace(/\s+/g, ' ').slice(0, 120);
  const normalized = normalizeSearchQuery(cleanQuery).slice(0, 120);
  if (normalized.length < 2) return;

  const docId = buildSearchMissDocId(normalized);
  const docRef = doc(db, 'searchMisses', docId);
  const cleanSource = source === 'android' ? 'android' : 'web';

  await runTransaction(db, async (tx) => {
    const snap = await tx.get(docRef);
    if (!snap.exists()) {
      tx.set(docRef, {
        query: cleanQuery,
        normalizedQuery: normalized,
        missCount: 1,
        lastSource: cleanSource,
        createdAt: serverTimestamp(),
        lastSearchedAt: serverTimestamp(),
      });
    } else {
      const currentCount = Math.max(1, Number(snap.data()?.missCount || 1));
      tx.update(docRef, {
        query: cleanQuery,
        missCount: currentCount + 1,
        lastSource: cleanSource,
        lastSearchedAt: serverTimestamp(),
      });
    }
  });
}

export function subscribeSearchMisses(cb: (items: SearchMiss[]) => void): Unsubscribe {
  return onSnapshot(
    collection(db, 'searchMisses'),
    (snap) => {
      const items = snap.docs
        .map((d) => {
          const data = d.data() as Omit<SearchMiss, 'id'>;
          return {
            ...data,
            id: d.id,
            missCount: Number(data.missCount || 1),
          } satisfies SearchMiss;
        })
        .sort(
          (a, b) =>
            b.missCount - a.missCount || toMillis(b.lastSearchedAt) - toMillis(a.lastSearchedAt),
        );
      cb(items);
    },
    () => cb([]),
  );
}

export async function deleteSearchMiss(missId: string): Promise<void> {
  if (!auth.currentUser || !missId.trim()) return;
  await deleteDoc(doc(db, 'searchMisses', missId.trim()));
}

