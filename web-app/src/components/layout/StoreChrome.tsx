import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Bell,
  ChevronDown,
  Heart,
  LayoutGrid,
  Home,
  LayoutDashboard,
  MapPin,
  Package,
  Phone,
  Search,
  ShoppingBag,
  ShoppingCart,
  Sparkles,
  Store,
  Truck,
  User,
  X,
  CheckCircle2,
  AlertCircle,
  Info,
} from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import {
  effectiveRetailPriceWithShowcases,
  formatCurrency,
  formatNumber,
} from '../../lib/formatters';

export const StoreHeader: React.FC = () => {
  const {
    route,
    navigate,
    storeSettings,
    categories,
    storefrontProducts,
    brandShowcases,
    cartTotalCount,
    cartSubtotal,
    wishlistProductIds,
    unreadNotificationsCount,
    canAccessAdmin,
    firebaseUser,
    defaultAddress,
    effectiveWholesaleActive,
  } = useStore();

  const [searchInput, setSearchInput] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [megaMenuOpen, setMegaMenuOpen] = useState(false);
  const searchBoxRef = useRef<HTMLDivElement | null>(null);
  const megaMenuRef = useRef<HTMLDivElement | null>(null);

  const mainCategories = useMemo(
    () => categories.filter((c) => !c.parentId),
    [categories],
  );

  const subCategoriesByParent = useMemo(() => {
    const map = new Map<string, typeof categories>();
    for (const c of categories) {
      if (c.parentId) {
        const list = map.get(c.parentId) || [];
        list.push(c);
        map.set(c.parentId, list);
      }
    }
    return map;
  }, [categories]);

  const liveSuggestions = useMemo(() => {
    const q = searchInput.trim().toLowerCase();
    if (q.length < 2) return { matchedProducts: [], matchedCategories: [] };
    const matchedProducts = storefrontProducts
      .filter(
        (p) =>
          p.name.toLowerCase().includes(q) ||
          p.sku.toLowerCase().includes(q) ||
          (p.barcode || '').toLowerCase().includes(q),
      )
      .slice(0, 5);
    const matchedCategories = mainCategories
      .filter((c) => c.name.toLowerCase().includes(q))
      .slice(0, 3);
    return { matchedProducts, matchedCategories };
  }, [searchInput, storefrontProducts, mainCategories]);

  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      if (searchBoxRef.current && !searchBoxRef.current.contains(e.target as Node)) {
        setSearchOpen(false);
      }
      if (megaMenuRef.current && !megaMenuRef.current.contains(e.target as Node)) {
        setMegaMenuOpen(false);
      }
    };
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setSearchOpen(false);
        setMegaMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
      document.removeEventListener('keydown', handleKey);
    };
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchOpen(false);
    navigate({ name: 'search', initialQuery: searchInput.trim() });
  };

  const showFreeShippingUtility =
    storeSettings.freeShippingEnabled && storeSettings.freeShippingThreshold > 0;
  const primaryPhone =
    storeSettings.retailPhones?.[0]?.phoneNumber || storeSettings.supportPhone || '';

  return (
    <header className="sticky top-0 z-40 bg-white border-b border-[#EBECEF] shadow-xs print:hidden">
      {/* Desktop Top Utility Bar (International E-Commerce Polish) */}
      <div className="hidden md:block bg-[#121419] text-white/85 text-[11px] border-b border-white/10">
        <div className="max-w-[1480px] mx-auto px-4 sm:px-6 lg:px-8 h-8 flex items-center justify-between gap-4">
          <div className="flex items-center gap-4 min-w-0">
            {showFreeShippingUtility ? (
              <span className="inline-flex items-center gap-1.5 text-[#FFD000] font-bold truncate">
                <Truck className="w-3.5 h-3.5 shrink-0" />
                <span>
                  شحن مجاني للطلبات فوق {formatCurrency(storeSettings.freeShippingThreshold)}
                </span>
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 text-white/80 font-semibold truncate">
                <Sparkles className="w-3.5 h-3.5 text-[#FFD000] shrink-0" />
                <span>{storeSettings.tagline}</span>
              </span>
            )}
            <span className="text-white/20">|</span>
            <span className="inline-flex items-center gap-1 text-white/75 truncate">
              <Store className="w-3.5 h-3.5 text-[#FFD000]" />
              <span>توصيل سريع واستلام فوري من المتجر</span>
            </span>
          </div>

          <div className="flex items-center gap-4 shrink-0 font-semibold">
            {primaryPhone && (
              <a
                href={`tel:${primaryPhone}`}
                className="inline-flex items-center gap-1 text-white/80 hover:text-[#FFD000] transition"
              >
                <Phone className="w-3 h-3" />
                <span dir="ltr">{primaryPhone}</span>
              </a>
            )}
            <button
              type="button"
              onClick={() => navigate({ name: 'orders' })}
              className="hover:text-[#FFD000] transition"
            >
              تتبع طلباتك
            </button>
            <button
              type="button"
              onClick={() => navigate({ name: 'about' })}
              className="hover:text-[#FFD000] transition"
            >
              تواصل معنا
            </button>
          </div>
        </div>
      </div>

      {/* Main Signature Header Strip */}
      <div className="bg-[#FFD000] text-[#181A20] border-b border-[#E5B800]/40">
        <div className="max-w-[1480px] mx-auto px-3 sm:px-5 lg:px-8 h-16 lg:h-[68px] flex items-center justify-between gap-2.5 sm:gap-4">
          {/* Brand Logo */}
          <button
            type="button"
            onClick={() => navigate({ name: 'home' })}
            className="flex items-center gap-2.5 shrink-0 text-right group"
          >
            <div className="w-10 h-10 lg:w-11 lg:h-11 rounded-xl bg-[#181A20] text-[#FFD000] flex items-center justify-center shadow-sm group-hover:scale-105 transition-transform">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <div className="hidden sm:block">
              <div className="font-black text-base lg:text-lg text-[#181A20] leading-tight">
                {storeSettings.storeName}
              </div>
              <div className="text-[11px] font-semibold text-[#181A20]/75 leading-tight max-w-[170px] truncate">
                {storeSettings.tagline}
              </div>
            </div>
          </button>

          {/* Delivery Location Selector (Desktop rich pill / Mobile compact icon) */}
          <button
            type="button"
            aria-label={
              defaultAddress
                ? `عنوان التوصيل: ${defaultAddress.governorate} - ${defaultAddress.city}`
                : 'عناوين التوصيل'
            }
            title={
              defaultAddress
                ? `التوصيل إلى: ${defaultAddress.governorate} - ${defaultAddress.city}`
                : 'اختر عنوان التوصيل'
            }
            onClick={() => navigate({ name: firebaseUser ? 'addresses' : 'account' })}
            className="hidden lg:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/75 hover:bg-white border border-[#181A20]/10 text-right shrink-0 transition max-w-[190px]"
          >
            <div className="relative w-8 h-8 rounded-lg bg-[#FFF9DB] flex items-center justify-center shrink-0">
              <MapPin className="w-4 h-4 text-[#1565C0]" />
              {defaultAddress && (
                <span className="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-[#219653]" />
              )}
            </div>
            <div className="min-w-0">
              <div className="text-[10px] font-bold text-[#181A20]/65 leading-none">التوصيل إلى</div>
              <div className="text-xs font-extrabold text-[#181A20] truncate mt-0.5">
                {defaultAddress ? `${defaultAddress.governorate} - ${defaultAddress.city}` : 'حدد عنوانك'}
              </div>
            </div>
          </button>

          {/* Smart Live Search Bar */}
          <div ref={searchBoxRef} className="relative flex-1 max-w-3xl">
            <form onSubmit={handleSearchSubmit}>
              <div className="relative flex items-center">
                <input
                  type="search"
                  value={searchInput}
                  onFocus={() => setSearchOpen(true)}
                  onChange={(e) => {
                    setSearchInput(e.target.value);
                    setSearchOpen(true);
                  }}
                  placeholder="ابحث عن منتج، شركة، فئة، SKU أو باركود..."
                  className="w-full h-10 lg:h-11 pr-4 pl-12 rounded-xl bg-white border border-[#181A20]/15 text-xs sm:text-sm text-[#181A20] placeholder:text-[#717784] shadow-2xs focus:ring-2 focus:ring-[#181A20]/25 focus:outline-none transition"
                />
                {searchInput && (
                  <button
                    type="button"
                    aria-label="مسح البحث"
                    onClick={() => setSearchInput('')}
                    className="absolute left-11 w-6 h-6 rounded-full text-[#717784] hover:text-[#181A20] flex items-center justify-center"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
                <button
                  type="submit"
                  aria-label="بحث"
                  className="absolute left-1.5 w-8 h-8 lg:w-9 lg:h-9 rounded-lg bg-[#181A20] text-[#FFD000] flex items-center justify-center hover:bg-[#282B33] transition"
                >
                  <Search className="w-4 h-4" />
                </button>
              </div>
            </form>

            {/* Instant Search Suggestions Dropdown */}
            {searchOpen &&
              searchInput.trim().length >= 2 &&
              (liveSuggestions.matchedProducts.length > 0 ||
                liveSuggestions.matchedCategories.length > 0) && (
                <div className="absolute inset-x-0 top-full mt-2 bg-white rounded-2xl border border-[#EBECEF] shadow-2xl overflow-hidden z-50">
                  {liveSuggestions.matchedCategories.length > 0 && (
                    <div className="px-3 py-2 border-b border-[#EBECEF] bg-[#F9FAFB]">
                      <div className="text-[10px] font-extrabold text-[#717784] mb-1.5">
                        فئات مطابقة
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {liveSuggestions.matchedCategories.map((cat) => (
                          <button
                            key={cat.id}
                            type="button"
                            onClick={() => {
                              setSearchOpen(false);
                              navigate({ name: 'categories', initialCategoryId: cat.id });
                            }}
                            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-white border border-[#EBECEF] hover:border-[#FFD000] text-xs font-bold text-[#181A20] transition"
                          >
                            <LayoutGrid className="w-3 h-3 text-[#1565C0]" />
                            <span>{cat.name}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {liveSuggestions.matchedProducts.length > 0 && (
                    <div className="divide-y divide-[#EBECEF]">
                      {liveSuggestions.matchedProducts.map((p) => {
                        const displayPrice =
                          effectiveWholesaleActive && p.wholesalePrice > 0
                            ? p.wholesalePrice
                            : effectiveRetailPriceWithShowcases(p, brandShowcases);
                        return (
                          <button
                            key={p.id}
                            type="button"
                            onClick={() => {
                              setSearchOpen(false);
                              navigate({ name: 'product', productId: p.id });
                            }}
                            className="w-full px-3.5 py-2.5 flex items-center gap-3 text-right hover:bg-[#FFF9DB]/50 transition"
                          >
                            <div className="w-11 h-11 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] p-1 flex items-center justify-center shrink-0">
                              {p.imageUrls[0] ? (
                                <img
                                  src={p.imageUrls[0]}
                                  alt={p.name}
                                  className="w-full h-full object-contain"
                                />
                              ) : (
                                <Package className="w-5 h-5 text-[#C4C9D2]" />
                              )}
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="text-xs sm:text-sm font-bold text-[#181A20] truncate">
                                {p.name}
                              </div>
                              <div className="text-[11px] text-[#717784] truncate">
                                {p.unit} • {p.stockQuantity > 0 ? 'متوفر' : 'نفدت الكمية'}
                              </div>
                            </div>
                            <div className="text-xs sm:text-sm font-black text-[#181A20] shrink-0">
                              {formatCurrency(displayPrice)}
                            </div>
                          </button>
                        );
                      })}
                    </div>
                  )}

                  <button
                    type="button"
                    onClick={() => {
                      setSearchOpen(false);
                      navigate({ name: 'search', initialQuery: searchInput.trim() });
                    }}
                    className="w-full py-2.5 text-center bg-[#F9FAFB] hover:bg-[#FFF9DB] text-xs font-extrabold text-[#1565C0] border-t border-[#EBECEF] transition"
                  >
                    عرض كل النتائج لـ «{searchInput.trim()}»
                  </button>
                </div>
              )}
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
            {canAccessAdmin && (
              <button
                type="button"
                onClick={() => navigate({ name: 'admin' })}
                className={`inline-flex items-center gap-1.5 px-2.5 sm:px-3.5 h-10 rounded-xl text-xs font-extrabold transition shadow-2xs ${
                  route.name === 'admin'
                    ? 'bg-white text-[#181A20]'
                    : 'bg-[#181A20] text-[#FFD000] hover:bg-[#282B33]'
                }`}
              >
                <LayoutDashboard className="w-4 h-4" />
                <span className="hidden sm:inline">لوحة الإدارة</span>
              </button>
            )}

            {/* Mobile/Tablet Location Icon Button */}
            <button
              type="button"
              aria-label={
                defaultAddress
                  ? `عنوان التوصيل: ${defaultAddress.governorate} - ${defaultAddress.city}`
                  : 'عناوين التوصيل'
              }
              title={
                defaultAddress
                  ? `التوصيل إلى: ${defaultAddress.governorate} - ${defaultAddress.city}`
                  : 'عناوين التوصيل'
              }
              onClick={() => navigate({ name: firebaseUser ? 'addresses' : 'account' })}
              className="lg:hidden relative w-10 h-10 rounded-xl bg-white/90 hover:bg-white border border-[#181A20]/10 flex items-center justify-center transition"
            >
              <MapPin className="w-5 h-5 text-[#1565C0]" />
              {defaultAddress && (
                <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-[#219653]" />
              )}
            </button>

            {firebaseUser && (
              <button
                type="button"
                aria-label="الإشعارات"
                onClick={() => navigate({ name: 'notifications' })}
                className="relative w-10 h-10 rounded-xl bg-white/90 hover:bg-white border border-[#181A20]/10 flex items-center justify-center transition"
              >
                <Bell className="w-5 h-5 text-[#181A20]" />
                {unreadNotificationsCount > 0 && (
                  <span className="absolute -top-1 -left-1 min-w-[18px] h-[18px] px-1 rounded-full bg-[#E53935] text-white text-[10px] font-black flex items-center justify-center">
                    {formatNumber(unreadNotificationsCount)}
                  </span>
                )}
              </button>
            )}

            <button
              type="button"
              aria-label="المفضلة"
              onClick={() => navigate({ name: 'wishlist' })}
              className={`relative w-10 h-10 rounded-xl border flex items-center justify-center transition ${
                route.name === 'wishlist'
                  ? 'bg-[#181A20] border-[#181A20] text-[#FFD000]'
                  : 'bg-white/90 hover:bg-white border-[#181A20]/10 text-[#181A20]'
              }`}
            >
              <Heart
                className={`w-5 h-5 ${
                  wishlistProductIds.length > 0 && route.name !== 'wishlist'
                    ? 'fill-[#E53935] text-[#E53935]'
                    : ''
                }`}
              />
              {wishlistProductIds.length > 0 && (
                <span className="absolute -top-1 -left-1 min-w-[18px] h-[18px] px-1 rounded-full bg-[#181A20] text-[#FFD000] text-[10px] font-black flex items-center justify-center border border-[#FFD000]">
                  {formatNumber(wishlistProductIds.length)}
                </span>
              )}
            </button>

            <button
              type="button"
              aria-label="عربة التسوق"
              onClick={() => navigate({ name: 'cart' })}
              className={`relative h-10 px-3 rounded-xl border flex items-center gap-2 font-extrabold text-xs transition ${
                route.name === 'cart'
                  ? 'bg-[#181A20] border-[#181A20] text-[#FFD000]'
                  : 'bg-white/95 hover:bg-white border-[#181A20]/10 text-[#181A20]'
              }`}
            >
              <div className="relative">
                <ShoppingCart className="w-4 h-4" />
                {cartTotalCount > 0 && (
                  <span className="md:hidden absolute -top-2 -left-2 min-w-[16px] h-[16px] px-1 rounded-full bg-[#E53935] text-white text-[9px] font-black flex items-center justify-center">
                    {formatNumber(cartTotalCount)}
                  </span>
                )}
              </div>
              <span className="hidden md:inline">السلة</span>
              {cartTotalCount > 0 && (
                <span className="hidden md:inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-[#E53935] text-white text-[10px] font-black">
                  {formatNumber(cartTotalCount)}
                </span>
              )}
              {cartSubtotal > 0 && (
                <span className="hidden xl:inline text-[11px] font-black opacity-85 border-r border-current/20 pr-2">
                  {formatCurrency(cartSubtotal)}
                </span>
              )}
            </button>

            <button
              type="button"
              aria-label="حسابي"
              onClick={() => navigate({ name: 'account' })}
              className={`hidden md:inline-flex h-10 px-3.5 rounded-xl border items-center gap-1.5 font-extrabold text-xs transition ${
                route.name === 'account'
                  ? 'bg-[#181A20] border-[#181A20] text-[#FFD000]'
                  : 'bg-white/90 hover:bg-white border-[#181A20]/10 text-[#181A20]'
              }`}
            >
              <User className="w-4 h-4" />
              <span>{firebaseUser ? 'حسابي' : 'تسجيل الدخول'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Secondary Department & Mega-Menu Navigation Bar */}
      <div ref={megaMenuRef} className="relative bg-[#181A20] text-white/90">
        <div className="max-w-[1480px] mx-auto px-3 sm:px-5 lg:px-8 h-11 flex items-center gap-1.5 sm:gap-2 overflow-x-auto no-scrollbar text-xs font-bold">
          {/* Interactive Mega-Menu Trigger */}
          <button
            type="button"
            onClick={() => {
              if (window.innerWidth < 768) {
                navigate({ name: 'categories' });
              } else {
                setMegaMenuOpen((v) => !v);
              }
            }}
            className={`shrink-0 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition ${
              megaMenuOpen || route.name === 'categories'
                ? 'bg-[#FFD000] text-[#181A20]'
                : 'bg-white/10 text-[#FFD000] hover:bg-white/15'
            }`}
          >
            <LayoutGrid className="w-3.5 h-3.5" />
            <span>جميع الفئات</span>
            <ChevronDown
              className={`hidden md:block w-3.5 h-3.5 transition-transform ${
                megaMenuOpen ? 'rotate-180' : ''
              }`}
            />
          </button>

          <span className="text-white/20">|</span>

          <button
            type="button"
            onClick={() => {
              setMegaMenuOpen(false);
              navigate({ name: 'home' });
            }}
            className={`shrink-0 px-2.5 py-1.5 rounded-lg transition ${
              route.name === 'home' ? 'bg-white/15 text-white' : 'hover:bg-white/10'
            }`}
          >
            الرئيسية
          </button>

          <button
            type="button"
            onClick={() => {
              setMegaMenuOpen(false);
              navigate({ name: 'search', onlyOffers: true });
            }}
            className="shrink-0 px-2.5 py-1.5 rounded-lg text-[#FFD000] hover:bg-white/10 transition"
          >
            العروض والخصومات
          </button>

          <button
            type="button"
            onClick={() => {
              setMegaMenuOpen(false);
              navigate({ name: 'search', initialSort: 'bestselling' });
            }}
            className="hidden sm:inline-block shrink-0 px-2.5 py-1.5 rounded-lg text-white/85 hover:text-[#FFD000] hover:bg-white/10 transition"
          >
            الأكثر مبيعًا
          </button>

          <button
            type="button"
            onClick={() => {
              setMegaMenuOpen(false);
              navigate({ name: 'search', initialSort: 'newest' });
            }}
            className="hidden lg:inline-block shrink-0 px-2.5 py-1.5 rounded-lg text-white/85 hover:text-[#FFD000] hover:bg-white/10 transition"
          >
            وصل حديثًا
          </button>

          {mainCategories.slice(0, 7).map((cat) => (
            <button
              key={cat.id}
              type="button"
              onClick={() => {
                setMegaMenuOpen(false);
                navigate({ name: 'categories', initialCategoryId: cat.id });
              }}
              className="hidden md:inline-block shrink-0 px-2.5 py-1.5 rounded-lg text-white/80 hover:text-[#FFD000] hover:bg-white/10 transition"
            >
              {cat.name}
            </button>
          ))}

          <button
            type="button"
            onClick={() => {
              setMegaMenuOpen(false);
              navigate({ name: 'orders' });
            }}
            className={`md:hidden shrink-0 px-2.5 py-1.5 rounded-lg transition ${
              route.name === 'orders' ? 'bg-white/15 text-white' : 'hover:bg-white/10'
            }`}
          >
            طلباتي
          </button>

          <button
            type="button"
            onClick={() => {
              setMegaMenuOpen(false);
              navigate({ name: 'about' });
            }}
            className={`shrink-0 mr-auto px-2.5 py-1.5 rounded-lg transition ${
              route.name === 'about' ? 'bg-white/15 text-white' : 'text-white/75 hover:bg-white/10'
            }`}
          >
            عن المتجر والتواصل
          </button>
        </div>

        {/* Desktop Categories Mega-Menu Panel */}
        {megaMenuOpen && (
          <div className="hidden md:block absolute inset-x-0 top-full bg-white text-[#181A20] border-b border-[#EBECEF] shadow-2xl z-50">
            <div className="max-w-[1480px] mx-auto px-6 lg:px-8 py-5 space-y-4">
              <div className="flex items-center justify-between border-b border-[#EBECEF] pb-3">
                <div>
                  <h3 className="text-sm font-black text-[#181A20]">تصفح جميع أقسام المتجر</h3>
                  <p className="text-[11px] text-[#717784]">
                    انتقل مباشرة إلى أي قسم رئيسي أو فرعي
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setMegaMenuOpen(false);
                    navigate({ name: 'categories' });
                  }}
                  className="px-3.5 py-1.5 rounded-xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] text-xs font-black transition"
                >
                  عرض صفحة الفئات الكاملة
                </button>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4 max-h-[60vh] overflow-y-auto">
                {mainCategories.map((cat) => {
                  const subs = subCategoriesByParent.get(cat.id) || [];
                  return (
                    <div
                      key={cat.id}
                      className="rounded-2xl border border-[#EBECEF] bg-[#F9FAFB]/70 hover:border-[#FFD000] hover:bg-white p-3 space-y-2 transition"
                    >
                      <button
                        type="button"
                        onClick={() => {
                          setMegaMenuOpen(false);
                          navigate({ name: 'categories', initialCategoryId: cat.id });
                        }}
                        className="w-full flex items-center gap-2.5 text-right group"
                      >
                        <div className="w-10 h-10 rounded-xl bg-white border border-[#EBECEF] p-1 flex items-center justify-center shrink-0 overflow-hidden">
                          {cat.imageUrl ? (
                            <img
                              src={cat.imageUrl}
                              alt={cat.name}
                              className="w-full h-full object-contain"
                            />
                          ) : (
                            <LayoutGrid className="w-4 h-4 text-[#717784]" />
                          )}
                        </div>
                        <span className="font-extrabold text-xs sm:text-sm text-[#181A20] group-hover:text-[#1565C0] line-clamp-1">
                          {cat.name}
                        </span>
                      </button>

                      {subs.length > 0 && (
                        <div className="flex flex-wrap gap-1 pt-1 border-t border-[#EBECEF]">
                          {subs.slice(0, 6).map((sub) => (
                            <button
                              key={sub.id}
                              type="button"
                              onClick={() => {
                                setMegaMenuOpen(false);
                                navigate({ name: 'search', initialCategoryId: sub.id });
                              }}
                              className="px-2 py-0.5 rounded-md bg-white border border-[#EBECEF] hover:border-[#FFD000] text-[11px] font-semibold text-[#5B606E] hover:text-[#181A20] transition"
                            >
                              {sub.name}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </div>
    </header>
  );
};

export const MobileBottomNav: React.FC = () => {
  const { route, navigate, cartTotalCount } = useStore();

  const items: Array<{
    name: 'home' | 'categories' | 'search' | 'cart' | 'account';
    label: string;
    icon: React.ReactNode;
    badge?: number;
  }> = [
    { name: 'home', label: 'الرئيسية', icon: <Home className="w-5 h-5" /> },
    { name: 'categories', label: 'الفئات', icon: <LayoutGrid className="w-5 h-5" /> },
    { name: 'search', label: 'البحث', icon: <Search className="w-5 h-5" /> },
    { name: 'cart', label: 'السلة', icon: <ShoppingCart className="w-5 h-5" />, badge: cartTotalCount },
    { name: 'account', label: 'حسابي', icon: <User className="w-5 h-5" /> },
  ];

  return (
    <nav className="md:hidden fixed bottom-0 inset-x-0 z-40 bg-white/95 backdrop-blur border-t border-[#EBECEF] px-2 py-1.5 flex items-center justify-around shadow-lg print:hidden">
      {items.map((it) => {
        const active = route.name === it.name;
        return (
          <button
            key={it.name}
            type="button"
            onClick={() => navigate({ name: it.name })}
            className={`relative flex flex-col items-center gap-0.5 px-3 py-1.5 rounded-xl text-[11px] font-bold transition ${
              active ? 'text-[#181A20] bg-[#FFF9DB]' : 'text-[#717784]'
            }`}
          >
            <div className="relative">
              {it.icon}
              {it.badge && it.badge > 0 ? (
                <span className="absolute -top-1.5 -left-2 min-w-[16px] h-[16px] px-1 rounded-full bg-[#181A20] text-[#FFD000] text-[9px] font-black flex items-center justify-center">
                  {formatNumber(it.badge)}
                </span>
              ) : null}
            </div>
            <span>{it.label}</span>
          </button>
        );
      })}
    </nav>
  );
};

export const StoreFooter: React.FC = () => {
  const { navigate, storeSettings, categories } = useStore();
  const topCats = categories.filter((c) => !c.parentId).slice(0, 6);
  const footerRetailPhone =
    storeSettings.retailPhones?.[0]?.phoneNumber || storeSettings.supportPhone || '';
  const footerWholesalePhone = storeSettings.wholesalePhones?.[0]?.phoneNumber || '';

  return (
    <footer className="bg-[#181A20] text-white mt-14 pb-20 md:pb-8 print:hidden border-t-4 border-[#FFD000]">
      <div className="max-w-[1480px] mx-auto px-4 sm:px-6 lg:px-8 py-10 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <div className="space-y-3">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[#FFD000] text-[#181A20] flex items-center justify-center">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <div className="font-black text-lg text-[#FFD000]">{storeSettings.storeName}</div>
          </div>
          <p className="text-xs text-white/75 leading-relaxed">
            {storeSettings.aboutStoreDescription || storeSettings.tagline}
          </p>
          {(footerRetailPhone || footerWholesalePhone) && (
            <div className="pt-1 space-y-1 text-xs text-white/80">
              {footerRetailPhone && (
                <div>
                  القطاعي:{' '}
                  <a
                    href={`tel:${footerRetailPhone}`}
                    dir="ltr"
                    className="text-[#FFD000] font-bold hover:underline"
                  >
                    {footerRetailPhone}
                  </a>
                </div>
              )}
              {footerWholesalePhone && (
                <div>
                  الجملة:{' '}
                  <a
                    href={`tel:${footerWholesalePhone}`}
                    dir="ltr"
                    className="text-[#FFD000] font-bold hover:underline"
                  >
                    {footerWholesalePhone}
                  </a>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="space-y-2.5 text-xs">
          <div className="font-black text-sm text-[#FFD000]">تسوق سريع</div>
          <div className="flex flex-col items-start gap-2 text-white/80">
            <button type="button" onClick={() => navigate({ name: 'home' })} className="hover:text-[#FFD000] transition">
              الصفحة الرئيسية
            </button>
            <button type="button" onClick={() => navigate({ name: 'search', onlyOffers: true })} className="hover:text-[#FFD000] transition">
              أقوى العروض والخصومات
            </button>
            <button type="button" onClick={() => navigate({ name: 'search', initialSort: 'bestselling' })} className="hover:text-[#FFD000] transition">
              الأكثر مبيعًا
            </button>
            <button type="button" onClick={() => navigate({ name: 'categories' })} className="hover:text-[#FFD000] transition">
              جميع الأقسام والفئات
            </button>
          </div>
        </div>

        <div className="space-y-2.5 text-xs">
          <div className="font-black text-sm text-[#FFD000]">أبرز الأقسام</div>
          <div className="flex flex-col items-start gap-2 text-white/80">
            {topCats.length > 0 ? (
              topCats.map((cat) => (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => navigate({ name: 'categories', initialCategoryId: cat.id })}
                  className="hover:text-[#FFD000] transition"
                >
                  {cat.name}
                </button>
              ))
            ) : (
              <button type="button" onClick={() => navigate({ name: 'categories' })} className="hover:text-[#FFD000] transition">
                تصفح الفئات
              </button>
            )}
          </div>
        </div>

        <div className="space-y-2.5 text-xs">
          <div className="font-black text-sm text-[#FFD000]">خدمة العملاء والسياسات</div>
          <div className="flex flex-col items-start gap-2 text-white/80">
            <button type="button" onClick={() => navigate({ name: 'orders' })} className="hover:text-[#FFD000] transition">
              تتبع الطلبات
            </button>
            <button type="button" onClick={() => navigate({ name: 'about' })} className="hover:text-[#FFD000] transition">
              عن المتجر والتواصل
            </button>
            <button type="button" onClick={() => navigate({ name: 'policies', initialTab: 'privacy' })} className="hover:text-[#FFD000] transition">
              سياسة الخصوصية
            </button>
            <button type="button" onClick={() => navigate({ name: 'policies', initialTab: 'return' })} className="hover:text-[#FFD000] transition">
              سياسة الاسترجاع والاستبدال
            </button>
            <button type="button" onClick={() => navigate({ name: 'policies', initialTab: 'terms' })} className="hover:text-[#FFD000] transition">
              الشروط والأحكام
            </button>
          </div>
        </div>
      </div>

      <div className="border-t border-white/10">
        <div className="max-w-[1480px] mx-auto px-4 sm:px-6 lg:px-8 py-4 flex flex-col sm:flex-row items-center justify-between gap-2 text-[11px] text-white/60">
          <span>جميع الحقوق محفوظة © {storeSettings.storeName}</span>
          <span>تجربة تسوق متكاملة — قطاعي وجملة</span>
        </div>
      </div>
    </footer>
  );
};

export const BrandedToast: React.FC = () => {
  const { toast, dismissToast } = useStore();
  if (!toast) return null;

  const icon =
    toast.tone === 'success' ? (
      <CheckCircle2 className="w-5 h-5 text-[#219653] shrink-0" />
    ) : toast.tone === 'error' ? (
      <AlertCircle className="w-5 h-5 text-[#E53935] shrink-0" />
    ) : (
      <Info className="w-5 h-5 text-[#FFD000] shrink-0" />
    );

  return (
    <div className="fixed bottom-20 md:bottom-6 left-1/2 -translate-x-1/2 z-50 w-[92%] max-w-md print:hidden">
      <div className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-[#181A20] text-white border border-[#FFD000]/60 shadow-2xl">
        {icon}
        <p className="flex-1 text-xs sm:text-sm font-bold leading-snug">{toast.message}</p>
        <button
          type="button"
          aria-label="إغلاق التنبيه"
          onClick={dismissToast}
          className="text-white/70 hover:text-white"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
