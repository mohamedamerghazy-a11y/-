import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { onAuthStateChanged, signInWithPopup, signOut, type User } from 'firebase/auth';
import { auth, googleProvider } from '../lib/firebase';
import {
  effectiveRetailPriceWithShowcases,
  getFlashSaleStatus,
  getFlashUnitPrice,
  isExclusiveHiddenFromStore,
  roundTwo,
  setGlobalNumeralFormat,
  toArabicErrorMessage,
} from '../lib/formatters';
import {
  mergeLocalCartToFirestore,
  setFirestoreCartQuantity,
  subscribeAddresses,
  subscribeBanners,
  subscribeBrandShowcases,
  subscribeCategories,
  subscribeCompanies,
  subscribeFlashSales,
  subscribeDeliveryZones,
  subscribePaymentSettings,
  subscribeProducts,
  subscribeStaffMember,
  subscribeStoreSettings,
  subscribeUserCart,
  subscribeUserNotifications,
  subscribeUserOrders,
  subscribeUserProfile,
  subscribeUserStockAlertProductIds,
  subscribeWishlistProductIds,
  toggleStockAlertItem,
  toggleWishlistItem,
} from '../services/firestoreService';
import {
  BOOTSTRAPPED_OWNER_EMAIL,
  DEFAULT_PAYMENT_SETTINGS,
  DEFAULT_STORE_SETTINGS,
  STAFF_ROLE_META,
  buildCartItemId,
  type AppNotification,
  type Banner,
  type BrandShowcase,
  type Category,
  type DeliveryAddress,
  type DeliveryZone,
  type FlashSale,
  type LocalCartEntry,
  type Order,
  type Product,
  type ResolvedCartLine,
  type StaffMember,
  type StaffPermission,
  type StaffRole,
  type StoreCompany,
  type StoreGeneralSettings,
  type StorePaymentSettings,
  type UserProfile,
} from '../types/models';

const GUEST_CART_STORAGE_KEY = 'aleman_guest_cart_v1';
const GUEST_WISHLIST_STORAGE_KEY = 'aleman_guest_wishlist_v1';

export type CustomerRoute =
  | { name: 'home' }
  | { name: 'categories'; initialCategoryId?: string }
  | { name: 'search'; initialQuery?: string; initialSort?: string; onlyOffers?: boolean; initialCategoryId?: string }
  | { name: 'wishlist' }
  | { name: 'cart' }
  | { name: 'product'; productId: string; flashSaleId?: string }
  | { name: 'brandShowcase'; showcaseId: string }
  | { name: 'orders' }
  | { name: 'account' }
  | { name: 'addresses' }
  | { name: 'about' }
  | { name: 'policies'; initialTab?: 'privacy' | 'return' | 'terms' }
  | { name: 'notifications' }
  | { name: 'admin'; initialSection?: StaffPermission };

interface ToastState {
  id: number;
  message: string;
  tone: 'success' | 'error' | 'info';
}

interface StoreContextValue {
  route: CustomerRoute;
  navigate: (next: CustomerRoute) => void;
  goBack: () => void;

  // Auth & Profile
  firebaseUser: User | null;
  authReady: boolean;
  userProfile: UserProfile | null;
  profileLoaded: boolean;
  needsProfileSetup: boolean;
  staffMember: StaffMember | null;
  isBootstrappedOwner: boolean;
  effectiveRole: StaffRole;
  effectiveRoleLabel: string;
  effectivePermissions: Set<StaffPermission>;
  canAccessAdmin: boolean;
  canToggleWholesalePreview: boolean;
  isWholesalePreviewActive: boolean;
  toggleWholesalePreview: () => void;
  effectiveWholesaleActive: boolean;
  signInWithGoogle: () => Promise<void>;
  signOutUser: () => Promise<void>;

  // Live Store Data
  storeSettings: StoreGeneralSettings;
  settingsLoaded: boolean;
  paymentSettings: StorePaymentSettings;
  categories: Category[];
  companies: StoreCompany[];
  products: Product[];
  storefrontProducts: Product[];
  banners: Banner[];
  brandShowcases: BrandShowcase[];
  flashSales: FlashSale[];
  deliveryZones: DeliveryZone[];
  catalogLoading: boolean;

  // Customer Data
  addresses: DeliveryAddress[];
  defaultAddress: DeliveryAddress | null;
  userOrders: Order[];
  notifications: AppNotification[];
  unreadNotificationsCount: number;

  // Wishlist & Back-in-Stock Alerts
  wishlistProductIds: string[];
  toggleWishlist: (productId: string) => Promise<void>;
  stockAlertProductIds: string[];
  toggleStockAlert: (product: Product) => Promise<void>;

  // Cart
  cartEntries: LocalCartEntry[];
  resolvedCartLines: ResolvedCartLine[];
  cartSubtotal: number;
  cartTotalCount: number;
  hasCartStockIssues: boolean;
  addToCart: (
    product: Product,
    qty?: number,
    flashSaleId?: string,
    selectedColor?: string,
    selectedSize?: string,
  ) => Promise<void>;
  updateCartQty: (
    productId: string,
    newQty: number,
    flashSaleId?: string,
    selectedColor?: string,
    selectedSize?: string,
  ) => Promise<void>;
  updateCartQtyById: (cartItemId: string, newQty: number) => Promise<void>;
  removeFromCart: (cartItemIdOrProductId: string) => Promise<void>;
  reorderItemsToCart: (
    items: Array<{ productId: string; quantity: number; selectedColor?: string; selectedSize?: string }>,
  ) => Promise<void>;

  // Branded Toast
  toast: ToastState | null;
  showToast: (message: string, tone?: 'success' | 'error' | 'info') => void;
  dismissToast: () => void;
}

const StoreContext = createContext<StoreContextValue | null>(null);

function loadGuestCart(): LocalCartEntry[] {
  try {
    const raw = localStorage.getItem(GUEST_CART_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((x) => x && typeof x.productId === 'string' && Number(x.quantity) > 0);
  } catch {
    return [];
  }
}

function saveGuestCart(entries: LocalCartEntry[]) {
  try {
    localStorage.setItem(GUEST_CART_STORAGE_KEY, JSON.stringify(entries));
  } catch {
    // ignore storage quota
  }
}

function loadGuestWishlist(): string[] {
  try {
    const raw = localStorage.getItem(GUEST_WISHLIST_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((x) => typeof x === 'string') : [];
  } catch {
    return [];
  }
}

function saveGuestWishlist(ids: string[]) {
  try {
    localStorage.setItem(GUEST_WISHLIST_STORAGE_KEY, JSON.stringify(ids));
  } catch {
    // ignore
  }
}

function playNotificationChime() {
  try {
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const now = ctx.currentTime;
    [587.33, 880].forEach((freq, idx) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, now + idx * 0.11);
      gain.gain.setValueAtTime(0.08, now + idx * 0.11);
      gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.11 + 0.25);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now + idx * 0.11);
      osc.stop(now + idx * 0.11 + 0.26);
    });
  } catch {
    // ignore autoplay restriction
  }
}

export const StoreProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [routeHistory, setRouteHistory] = useState<CustomerRoute[]>([{ name: 'home' }]);
  const route = routeHistory[routeHistory.length - 1] || { name: 'home' };

  const navigate = useCallback((next: CustomerRoute) => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setRouteHistory((prev) => {
      const top = prev[prev.length - 1];
      if (top && JSON.stringify(top) === JSON.stringify(next)) return prev;
      return [...prev.slice(-20), next];
    });
  }, []);

  const goBack = useCallback(() => {
    setRouteHistory((prev) => (prev.length > 1 ? prev.slice(0, -1) : [{ name: 'home' }]));
  }, []);

  const [firebaseUser, setFirebaseUser] = useState<User | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [userProfile, setUserProfile] = useState<UserProfile | null>(null);
  const [profileLoaded, setProfileLoaded] = useState(false);
  const [staffMember, setStaffMember] = useState<StaffMember | null>(null);
  const [isWholesalePreviewActive, setIsWholesalePreviewActive] = useState(false);

  const [storeSettings, setStoreSettings] = useState<StoreGeneralSettings>(DEFAULT_STORE_SETTINGS);
  const [settingsLoaded, setSettingsLoaded] = useState(false);
  const [paymentSettings, setPaymentSettings] = useState<StorePaymentSettings>(DEFAULT_PAYMENT_SETTINGS);
  const [categories, setCategories] = useState<Category[]>([]);
  const [companies, setCompanies] = useState<StoreCompany[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [banners, setBanners] = useState<Banner[]>([]);
  const [brandShowcases, setBrandShowcases] = useState<BrandShowcase[]>([]);
  const [flashSales, setFlashSales] = useState<FlashSale[]>([]);
  const [deliveryZones, setDeliveryZones] = useState<DeliveryZone[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);

  const [addresses, setAddresses] = useState<DeliveryAddress[]>([]);
  const [remoteCart, setRemoteCart] = useState<LocalCartEntry[]>([]);
  const [guestCart, setGuestCart] = useState<LocalCartEntry[]>(() => loadGuestCart());
  const [remoteWishlist, setRemoteWishlist] = useState<string[]>([]);
  const [guestWishlist, setGuestWishlist] = useState<string[]>(() => loadGuestWishlist());
  const [stockAlertProductIds, setStockAlertProductIds] = useState<string[]>([]);
  const [userOrders, setUserOrders] = useState<Order[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);

  const [toast, setToast] = useState<ToastState | null>(null);
  const toastTimerRef = useRef<number | null>(null);
  const seenNotifIdsRef = useRef<Set<string> | null>(null);

  const showToast = useCallback((message: string, tone: 'success' | 'error' | 'info' = 'info') => {
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
    setToast({ id: Date.now(), message, tone });
    toastTimerRef.current = window.setTimeout(() => setToast(null), 3800);
  }, []);

  const dismissToast = useCallback(() => {
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
    setToast(null);
  }, []);

  // Update global numeral format whenever storeSettings changes
  useEffect(() => {
    setGlobalNumeralFormat(storeSettings.numeralFormat);
  }, [storeSettings.numeralFormat]);

  // Public storefront listeners (real-time synced with Android)
  useEffect(() => {
    const unsubs = [
      subscribeStoreSettings((s) => {
        setStoreSettings(s);
        setSettingsLoaded(true);
      }),
      subscribePaymentSettings(setPaymentSettings),
      subscribeCategories(false, setCategories),
      subscribeCompanies(false, setCompanies),
      subscribeProducts(false, (list) => {
        setProducts(list);
        setCatalogLoading(false);
      }),
      subscribeBanners(false, setBanners),
      subscribeBrandShowcases(false, setBrandShowcases),
      subscribeFlashSales(false, setFlashSales),
      subscribeDeliveryZones(false, setDeliveryZones),
    ];
    return () => unsubs.forEach((u) => u());
  }, []);

  // Auth listener
  useEffect(() => {
    return onAuthStateChanged(auth, (user) => {
      setFirebaseUser(user);
      setAuthReady(true);
      if (!user) {
        setUserProfile(null);
        setProfileLoaded(true);
        setStaffMember(null);
        setAddresses([]);
        setRemoteCart([]);
        setRemoteWishlist([]);
        setStockAlertProductIds([]);
        setUserOrders([]);
        setNotifications([]);
        seenNotifIdsRef.current = null;
      } else {
        setProfileLoaded(false);
      }
    });
  }, []);

  // Signed-in user document listeners + guest cart merge
  useEffect(() => {
    if (!firebaseUser) return;
    const uid = firebaseUser.uid;

    const u1 = subscribeUserProfile(uid, (p) => {
      setUserProfile(p);
      setProfileLoaded(true);
    });
    const u2 = subscribeStaffMember(uid, setStaffMember);
    const u3 = subscribeAddresses(uid, setAddresses);
    const u4 = subscribeUserCart(uid, setRemoteCart);
    const u5 = subscribeUserOrders(uid, setUserOrders);
    const u7 = subscribeWishlistProductIds(uid, setRemoteWishlist);
    const u8 = subscribeUserStockAlertProductIds(uid, setStockAlertProductIds);
    const u6 = subscribeUserNotifications(uid, (list) => {
      setNotifications(list);
      if (seenNotifIdsRef.current === null) {
        seenNotifIdsRef.current = new Set(list.map((n) => n.id));
      } else {
        const newlyAdded = list.filter((n) => !seenNotifIdsRef.current!.has(n.id) && !n.isRead);
        list.forEach((n) => seenNotifIdsRef.current!.add(n.id));
        if (newlyAdded.length > 0) {
          playNotificationChime();
          const latest = newlyAdded[0];
          showToast(`${latest.title}: ${latest.message}`, 'info');
          if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(latest.title, { body: latest.message });
          }
        }
      }
    });

    // Merge guest cart into Firestore once logged in
    const pendingGuest = loadGuestCart();
    if (pendingGuest.length > 0) {
      mergeLocalCartToFirestore(uid, pendingGuest)
        .then(() => {
          saveGuestCart([]);
          setGuestCart([]);
        })
        .catch(() => {
          // ignore if profile hasn't been created yet
        });
    }

    return () => {
      u1();
      u2();
      u3();
      u4();
      u5();
      u6();
      u7();
      u8();
    };
  }, [firebaseUser, showToast]);

  const isBootstrappedOwner = useMemo(() => {
    const email = firebaseUser?.email?.trim().toLowerCase() || '';
    if (!email) return false;
    const dynamicOwner = storeSettings.ownerEmail?.trim().toLowerCase() || '';
    return email === BOOTSTRAPPED_OWNER_EMAIL.toLowerCase() || (dynamicOwner.length >= 3 && email === dynamicOwner);
  }, [firebaseUser?.email, storeSettings.ownerEmail]);

  const effectiveRole: StaffRole = useMemo(() => {
    if (isBootstrappedOwner) return 'owner';
    if (staffMember?.isActive) return staffMember.role;
    if (userProfile?.role && userProfile.role !== 'customer') return userProfile.role;
    return 'customer';
  }, [isBootstrappedOwner, staffMember, userProfile?.role]);

  const effectivePermissions = useMemo(() => {
    if (isBootstrappedOwner || effectiveRole === 'owner') {
      return new Set<StaffPermission>(STAFF_ROLE_META.owner.permissions);
    }
    if (staffMember?.isActive && staffMember.permissions?.length) {
      return new Set<StaffPermission>(staffMember.permissions);
    }
    return new Set<StaffPermission>(STAFF_ROLE_META[effectiveRole]?.permissions || []);
  }, [isBootstrappedOwner, effectiveRole, staffMember]);

  const effectiveRoleLabel = STAFF_ROLE_META[effectiveRole]?.label || 'عميل';
  const canAccessAdmin = effectiveRole !== 'customer' && effectivePermissions.size > 0;
  const canToggleWholesalePreview =
    isBootstrappedOwner || effectivePermissions.has('preview_wholesale_store');

  const toggleWholesalePreview = useCallback(() => {
    setIsWholesalePreviewActive((v) => !v);
  }, []);

  const effectiveWholesaleActive =
    (userProfile?.accountType === 'wholesale' && userProfile?.wholesaleStatus === 'approved') ||
    (canToggleWholesalePreview && isWholesalePreviewActive);

  const signInWithGoogle = useCallback(async () => {
    try {
      await signInWithPopup(auth, googleProvider);
      showToast('تم تسجيل الدخول بحساب Google بنجاح', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر تسجيل الدخول بحساب Google.'), 'error');
    }
  }, [showToast]);

  const signOutUser = useCallback(async () => {
    await signOut(auth);
    setIsWholesalePreviewActive(false);
    showToast('تم تسجيل الخروج من الحساب', 'info');
  }, [showToast]);

  const wishlistProductIds = firebaseUser ? remoteWishlist : guestWishlist;

  const toggleWishlist = useCallback(
    async (productId: string) => {
      const cleanId = productId.trim();
      if (!cleanId) return;
      const inList = wishlistProductIds.includes(cleanId);
      if (firebaseUser) {
        try {
          const added = await toggleWishlistItem(firebaseUser.uid, cleanId, inList);
          showToast(added ? 'تمت الإضافة إلى المفضلة ❤️' : 'تمت الإزالة من المفضلة', 'info');
        } catch (err) {
          showToast(toArabicErrorMessage(err, 'تعذر تحديث المفضلة.'), 'error');
        }
      } else {
        const next = inList
          ? guestWishlist.filter((id) => id !== cleanId)
          : [...guestWishlist, cleanId];
        setGuestWishlist(next);
        saveGuestWishlist(next);
        showToast(inList ? 'تمت الإزالة من المفضلة' : 'تمت الإضافة إلى المفضلة ❤️', 'info');
      }
    },
    [firebaseUser, guestWishlist, showToast, wishlistProductIds],
  );

  const toggleStockAlert = useCallback(
    async (product: Product) => {
      if (!firebaseUser) {
        showToast('يرجى تسجيل الدخول أولًا لتفعيل تنبيه التوفر.', 'info');
        navigate({ name: 'account' });
        return;
      }
      const isSubscribed = stockAlertProductIds.includes(product.id);
      try {
        const added = await toggleStockAlertItem(firebaseUser.uid, product, isSubscribed);
        showToast(
          added
            ? 'تم تفعيل التنبيه — سنرسل لك إشعارًا فور توفر المنتج'
            : 'تم إلغاء تنبيه توفر المنتج',
          added ? 'success' : 'info',
        );
      } catch (err) {
        showToast(toArabicErrorMessage(err, 'تعذر تحديث تنبيه التوفر.'), 'error');
      }
    },
    [firebaseUser, navigate, showToast, stockAlertProductIds],
  );

  const cartEntries = firebaseUser ? remoteCart : guestCart;

  const resolvedCartLines: ResolvedCartLine[] = useMemo(() => {
    const productMap = new Map(products.map((p) => [p.id, p]));
    const nowMillis = Date.now();
    const activeFlashMap = new Map(
      flashSales
        .filter((f) => f.isActive && getFlashSaleStatus(f, nowMillis) === 'active')
        .map((f) => [f.id, f]),
    );

    const lines: ResolvedCartLine[] = [];
    for (const entry of cartEntries) {
      const product = productMap.get(entry.productId);
      if (!product) continue;
      const qty = Math.max(1, entry.quantity);
      const minQty = Math.max(1, product.wholesaleMinQty);
      const activeFlash = entry.flashSaleId ? activeFlashMap.get(entry.flashSaleId) : undefined;
      const flashPrice = activeFlash ? getFlashUnitPrice(activeFlash, product.id, effectiveWholesaleActive) : null;

      const useWholesale =
        flashPrice == null && effectiveWholesaleActive && product.wholesalePrice > 0 && qty >= minQty;

      const unitPrice = roundTwo(
        flashPrice ??
          (useWholesale
            ? product.wholesalePrice
            : effectiveRetailPriceWithShowcases(product, brandShowcases)),
      );
      const appliedPricingMode = flashPrice != null ? 'flash_sale' : useWholesale ? 'wholesale' : 'retail';
      const remainingForWholesale =
        effectiveWholesaleActive && product.wholesalePrice > 0 && qty < minQty ? minQty - qty : 0;

      lines.push({
        cartItemId: buildCartItemId(product.id, entry.selectedColor, entry.selectedSize),
        product,
        quantity: qty,
        unitPrice,
        appliedPricingMode,
        lineTotal: roundTwo(unitPrice * qty),
        remainingForWholesale,
        flashSaleId: flashPrice != null && activeFlash ? activeFlash.id : undefined,
        flashSaleTitle: flashPrice != null && activeFlash ? activeFlash.title : undefined,
        selectedColor: entry.selectedColor?.trim() || undefined,
        selectedSize: entry.selectedSize?.trim() || undefined,
        hasStockIssue: product.stockQuantity <= 0 || qty > product.stockQuantity,
      });
    }
    return lines;
  }, [cartEntries, products, flashSales, brandShowcases, effectiveWholesaleActive]);

  const cartSubtotal = useMemo(
    () => roundTwo(resolvedCartLines.reduce((sum, l) => sum + l.lineTotal, 0)),
    [resolvedCartLines],
  );
  const cartTotalCount = useMemo(
    () => resolvedCartLines.reduce((sum, l) => sum + l.quantity, 0),
    [resolvedCartLines],
  );
  const hasCartStockIssues = useMemo(
    () => resolvedCartLines.some((l) => l.hasStockIssue),
    [resolvedCartLines],
  );

  const addToCart = useCallback(
    async (
      product: Product,
      qty = 1,
      flashSaleId?: string,
      selectedColor?: string,
      selectedSize?: string,
    ) => {
      if (product.stockQuantity <= 0) {
        showToast('المنتج غير متوفر في المخزون حاليًا.', 'error');
        return;
      }
      const cleanColor = selectedColor?.trim() || undefined;
      const cleanSize = selectedSize?.trim() || undefined;
      const targetId = buildCartItemId(product.id, cleanColor, cleanSize);
      const existing = cartEntries.find(
        (c) => buildCartItemId(c.productId, c.selectedColor, c.selectedSize) === targetId,
      );
      const nextQty = Math.min(product.stockQuantity, (existing?.quantity || 0) + qty);
      const resolvedFlash = flashSaleId || existing?.flashSaleId;

      if (firebaseUser) {
        try {
          await setFirestoreCartQuantity(
            firebaseUser.uid,
            product.id,
            nextQty,
            resolvedFlash,
            cleanColor,
            cleanSize,
          );
          showToast(`تمت إضافة «${product.name}» إلى السلة`, 'success');
        } catch (err) {
          showToast(toArabicErrorMessage(err, 'تعذر إضافة المنتج للسلة.'), 'error');
        }
      } else {
        const updated = existing
          ? guestCart.map((item) =>
              buildCartItemId(item.productId, item.selectedColor, item.selectedSize) === targetId
                ? {
                    ...item,
                    quantity: nextQty,
                    flashSaleId: resolvedFlash,
                    selectedColor: cleanColor,
                    selectedSize: cleanSize,
                  }
                : item,
            )
          : [
              ...guestCart,
              {
                productId: product.id,
                quantity: nextQty,
                flashSaleId: resolvedFlash,
                selectedColor: cleanColor,
                selectedSize: cleanSize,
              },
            ];
        setGuestCart(updated);
        saveGuestCart(updated);
        showToast(`تمت إضافة «${product.name}» إلى السلة`, 'success');
      }
    },
    [cartEntries, firebaseUser, guestCart, showToast],
  );

  const updateCartQty = useCallback(
    async (
      productId: string,
      newQty: number,
      flashSaleId?: string,
      selectedColor?: string,
      selectedSize?: string,
    ) => {
      const targetId = buildCartItemId(productId, selectedColor, selectedSize);
      if (firebaseUser) {
        try {
          await setFirestoreCartQuantity(
            firebaseUser.uid,
            productId,
            newQty,
            flashSaleId,
            selectedColor,
            selectedSize,
          );
        } catch (err) {
          showToast(toArabicErrorMessage(err, 'تعذر تحديث الكمية.'), 'error');
        }
      } else {
        const updated =
          newQty <= 0
            ? guestCart.filter(
                (c) => buildCartItemId(c.productId, c.selectedColor, c.selectedSize) !== targetId,
              )
            : guestCart.map((c) =>
                buildCartItemId(c.productId, c.selectedColor, c.selectedSize) === targetId
                  ? { ...c, quantity: newQty }
                  : c,
              );
        setGuestCart(updated);
        saveGuestCart(updated);
      }
    },
    [firebaseUser, guestCart, showToast],
  );

  const updateCartQtyById = useCallback(
    async (cartItemId: string, newQty: number) => {
      const entry = cartEntries.find(
        (c) => buildCartItemId(c.productId, c.selectedColor, c.selectedSize) === cartItemId,
      );
      if (!entry) return;
      await updateCartQty(
        entry.productId,
        newQty,
        entry.flashSaleId,
        entry.selectedColor,
        entry.selectedSize,
      );
    },
    [cartEntries, updateCartQty],
  );

  const removeFromCart = useCallback(
    async (cartItemIdOrProductId: string) => {
      const entry = cartEntries.find(
        (c) =>
          buildCartItemId(c.productId, c.selectedColor, c.selectedSize) === cartItemIdOrProductId ||
          c.productId === cartItemIdOrProductId,
      );
      if (entry) {
        await updateCartQty(
          entry.productId,
          0,
          entry.flashSaleId,
          entry.selectedColor,
          entry.selectedSize,
        );
      } else {
        await updateCartQty(cartItemIdOrProductId, 0);
      }
      showToast('تمت إزالة المنتج من السلة', 'info');
    },
    [cartEntries, updateCartQty, showToast],
  );

  const reorderItemsToCart = useCallback(
    async (
      items: Array<{ productId: string; quantity: number; selectedColor?: string; selectedSize?: string }>,
    ) => {
      const prodMap = new Map(products.map((p) => [p.id, p]));
      let added = 0;
      for (const item of items) {
        const prod = prodMap.get(item.productId);
        if (prod && prod.isActive && prod.stockQuantity > 0) {
          await addToCart(
            prod,
            Math.min(item.quantity, prod.stockQuantity),
            undefined,
            item.selectedColor,
            item.selectedSize,
          );
          added++;
        }
      }
      if (added > 0) {
        navigate({ name: 'cart' });
      } else {
        showToast('المنتجات المطلوبة غير متوفرة حاليًا.', 'error');
      }
    },
    [products, addToCart, navigate, showToast],
  );

  const defaultAddress = useMemo(
    () => addresses.find((a) => a.isDefault) || addresses[0] || null,
    [addresses],
  );

  const unreadNotificationsCount = useMemo(
    () => notifications.filter((n) => !n.isRead).length,
    [notifications],
  );

  const needsProfileSetup = Boolean(firebaseUser && profileLoaded && !userProfile);

  const storefrontProducts = useMemo(
    () => products.filter((p) => !isExclusiveHiddenFromStore(p)),
    [products],
  );

  const value: StoreContextValue = {
    route,
    navigate,
    goBack,
    firebaseUser,
    authReady,
    userProfile,
    profileLoaded,
    needsProfileSetup,
    staffMember,
    isBootstrappedOwner,
    effectiveRole,
    effectiveRoleLabel,
    effectivePermissions,
    canAccessAdmin,
    canToggleWholesalePreview,
    isWholesalePreviewActive,
    toggleWholesalePreview,
    effectiveWholesaleActive,
    signInWithGoogle,
    signOutUser,
    storeSettings,
    settingsLoaded,
    paymentSettings,
    categories,
    companies,
    products,
    storefrontProducts,
    banners,
    brandShowcases,
    flashSales,
    deliveryZones,
    catalogLoading,
    addresses,
    defaultAddress,
    userOrders,
    notifications,
    unreadNotificationsCount,
    wishlistProductIds,
    toggleWishlist,
    stockAlertProductIds,
    toggleStockAlert,
    cartEntries,
    resolvedCartLines,
    cartSubtotal,
    cartTotalCount,
    hasCartStockIssues,
    addToCart,
    updateCartQty,
    updateCartQtyById,
    removeFromCart,
    reorderItemsToCart,
    toast,
    showToast,
    dismissToast,
  };

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
};

export function useStore(): StoreContextValue {
  const ctx = useContext(StoreContext);
  if (!ctx) throw new Error('useStore must be used inside StoreProvider');
  return ctx;
}
