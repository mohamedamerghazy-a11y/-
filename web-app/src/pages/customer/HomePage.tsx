import React, { useMemo } from 'react';
import {
  ArrowLeft,
  BadgeCheck,
  Building2,
  ChevronLeft,
  LayoutDashboard,
  LayoutGrid,
  Megaphone,
  Package,
  Plus,
  ShieldCheck,
  Sparkles,
  Store,
  Truck,
  Wallet,
} from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import { AnnouncementTickerBar } from '../../components/common/AnnouncementTickerBar';
import { BannerCarousel } from '../../components/common/BannerCarousel';
import { CompanyOfferBadge } from '../../components/common/CompanyOfferBadge';
import { FlashSaleSection } from '../../components/common/FlashSaleSection';
import { HorizontalScroller } from '../../components/common/HorizontalScroller';
import { ProductCard } from '../../components/common/ProductCard';
import type { BannerPlacement, BrandShowcase, Category, Product } from '../../types/models';
import {
  computeBannerlessShowcaseAlignLeftMap,
  effectiveDiscountPercentWithShowcases,
  effectiveRetailPriceWithShowcases,
  formatCurrency,
  formatNumber,
  matchesBrandShowcase,
  safeShowcaseDiscountPercent,
} from '../../lib/formatters';

export const HomePage: React.FC = () => {
  const {
    navigate,
    storeSettings,
    settingsLoaded,
    categories,
    companies,
    products,
    storefrontProducts,
    banners,
    brandShowcases,
    catalogLoading,
    canAccessAdmin,
    effectiveWholesaleActive,
    addToCart,
  } = useStore();

  const companyMap = useMemo(() => {
    const map = new Map<string, (typeof companies)[number]>();
    for (const c of companies) map.set(c.id, c);
    return map;
  }, [companies]);

  const showcaseByCompanyId = useMemo(() => {
    const map = new Map<string, BrandShowcase>();
    for (const s of brandShowcases) {
      if (s.companyId && !map.has(s.companyId)) {
        map.set(s.companyId, s);
      }
    }
    return map;
  }, [brandShowcases]);

  const bannerlessAlignLeftMap = useMemo(
    () => computeBannerlessShowcaseAlignLeftMap(brandShowcases),
    [brandShowcases],
  );

  const mainCategories = useMemo(
    () => categories.filter((c) => !c.parentId),
    [categories],
  );

  const productCountByCat = useMemo(() => {
    const map = new Map<string, number>();
    for (const p of storefrontProducts) {
      map.set(p.categoryId, (map.get(p.categoryId) || 0) + 1);
      if (p.subCategoryId) map.set(p.subCategoryId, (map.get(p.subCategoryId) || 0) + 1);
    }
    return map;
  }, [storefrontProducts]);

  const bannersFor = (placement: BannerPlacement) =>
    banners.filter((b) => b.placement === placement);

  const showcasesFor = (placement: BannerPlacement) =>
    brandShowcases.filter((s) => s.placement === placement);

  const hasTickerAt = (placement: BannerPlacement) =>
    settingsLoaded &&
    Boolean(storeSettings.tickerEnabled) &&
    (storeSettings.tickerPlacement || 'top_home') === placement &&
    (Boolean(storeSettings.tickerText?.trim()) || (storeSettings.tickerInlineImages?.length || 0) > 0);

  const renderTickerSlot = (placement: BannerPlacement) =>
    hasTickerAt(placement) ? <AnnouncementTickerBar settings={storeSettings} /> : null;

  // Use 12 items per shelf so 2, 3, 4, and 6-column desktop/tablet/mobile grids form balanced rows
  const featuredProducts = useMemo(
    () => storefrontProducts.filter((p) => p.isFeatured).slice(0, 12),
    [storefrontProducts],
  );
  const dealsProducts = useMemo(
    () =>
      storefrontProducts
        .filter((p) => p.isOffer || effectiveDiscountPercentWithShowcases(p, brandShowcases) > 0)
        .slice(0, 12),
    [storefrontProducts, brandShowcases],
  );
  const bestsellersProducts = useMemo(
    () => [...storefrontProducts].sort((a, b) => b.salesCount - a.salesCount).slice(0, 12),
    [storefrontProducts],
  );
  const newestProducts = useMemo(() => storefrontProducts.slice(0, 12), [storefrontProducts]);
  const recommendedProducts = useMemo(
    () => [...storefrontProducts].sort((a, b) => b.averageRating - a.averageRating).slice(0, 12),
    [storefrontProducts],
  );

  const renderCompactShowcaseCard = (p: Product, variant: 'banner' | 'card') => {
    const price =
      effectiveWholesaleActive && p.wholesalePrice > 0
        ? p.wholesalePrice
        : effectiveRetailPriceWithShowcases(p, brandShowcases);
    const discountPct = !effectiveWholesaleActive
      ? effectiveDiscountPercentWithShowcases(p, brandShowcases)
      : 0;
    const hasVariants =
      (storeSettings.showProductColors && (p.availableColors?.length || 0) > 0) ||
      (storeSettings.showProductSizes && (p.availableSizes?.length || 0) > 0);

    const cardWidth =
      variant === 'banner'
        ? 'w-[126px] sm:w-[152px] md:w-[172px] lg:w-[186px]'
        : 'w-[144px] sm:w-[172px] md:w-[190px] lg:w-[204px]';

    const imgHeight =
      variant === 'banner'
        ? 'h-[76px] sm:h-[96px] md:h-[112px] lg:h-[124px]'
        : 'h-[104px] sm:h-[126px] md:h-[140px]';

    return (
      <div
        key={p.id}
        onClick={() => navigate({ name: 'product', productId: p.id })}
        className={`shrink-0 ${cardWidth} bg-white rounded-2xl p-2 sm:p-2.5 shadow-sm border border-[#EBECEF] flex flex-col gap-1.5 cursor-pointer hover:border-[#FFD000] hover:shadow-md hover:-translate-y-0.5 transition-all`}
      >
        <div
          className={`relative ${imgHeight} rounded-xl bg-[#F7F8FA] overflow-hidden p-1.5 sm:p-2 flex items-center justify-center`}
        >
          {p.imageUrls[0] ? (
            <img src={p.imageUrls[0]} alt={p.name} className="w-full h-full object-contain" />
          ) : (
            <Package className="w-7 h-7 text-[#C4C9D2]" />
          )}
          {discountPct > 0 && (
            <span className="absolute top-1 right-1 px-1.5 py-0.5 rounded-md bg-[#00965E] text-white text-[9px] sm:text-[10px] font-black">
              -{formatNumber(discountPct)}%
            </span>
          )}
          {p.stockQuantity > 0 && (
            <button
              type="button"
              aria-label="إضافة للسلة"
              onClick={(e) => {
                e.stopPropagation();
                if (hasVariants) {
                  navigate({ name: 'product', productId: p.id });
                } else {
                  addToCart(p, 1);
                }
              }}
              className="absolute bottom-1 left-1 w-7 h-7 rounded-lg bg-white border border-[#EBECEF] shadow-xs flex items-center justify-center text-[#181A20] hover:bg-[#FFD000] hover:border-[#FFD000] transition"
            >
              <Plus className="w-4 h-4 stroke-[2.5]" />
            </button>
          )}
        </div>
        <div className="text-[11px] sm:text-xs font-bold text-[#181A20] line-clamp-2 leading-tight min-h-[1.85rem] sm:min-h-[2.1rem]">
          {p.name}
        </div>
        <div className="hidden sm:block text-[10px] text-[#717784] truncate">{p.unit}</div>
        <div className="flex items-baseline gap-1.5 flex-wrap mt-auto">
          <span className="text-xs sm:text-sm font-black text-[#181A20]">
            {formatCurrency(price)}
          </span>
          {discountPct > 0 && (
            <span className="text-[10px] text-[#8B919E] line-through">
              {formatNumber(Math.round(p.retailPrice))}
            </span>
          )}
        </div>
      </div>
    );
  };

  const renderBrandShowcasesSlot = (placement: BannerPlacement) => {
    const list = showcasesFor(placement);
    if (!list.length) return null;
    return (
      <div className="space-y-6">
        {list.map((showcase: BrandShowcase) => {
          const showcaseProducts = products.filter((p) => matchesBrandShowcase(p, showcase));
          const linkedCompany = showcase.companyId ? companyMap.get(showcase.companyId) : undefined;
          const companyLogo = linkedCompany?.imageUrl || showcase.companyImageUrl || '';
          const showcaseDiscPct = safeShowcaseDiscountPercent(showcase);
          const hasBanner = Boolean(showcase.bannerImageUrl && showcase.bannerImageUrl.trim().length > 0);
          const accent = showcase.accentColorHex || '#FCE4EC';

          if (hasBanner) {
            return (
              <section
                key={showcase.id}
                style={{ backgroundColor: accent }}
                onClick={() => navigate({ name: 'brandShowcase', showcaseId: showcase.id })}
                className="relative w-full rounded-3xl overflow-hidden shadow-sm border border-black/5 cursor-pointer"
              >
                <img
                  src={showcase.bannerImageUrl}
                  alt={showcase.title}
                  className="w-full h-auto block"
                />

                {showcaseDiscPct > 0 && (
                  <div className="absolute top-3 left-3 z-10">
                    <CompanyOfferBadge
                      discountPercent={showcaseDiscPct}
                      style={storeSettings.companyOfferBadgeStyle}
                    />
                  </div>
                )}

                <div className="absolute inset-0 flex flex-col">
                  <div className="h-[25%] w-full shrink-0" />

                  <div className="h-[65%] w-full flex items-center px-3 sm:px-5 overflow-hidden">
                    {showcaseProducts.length > 0 && (
                      <div onClick={(e) => e.stopPropagation()} className="w-full">
                        <HorizontalScroller
                          className="gap-2.5 sm:gap-3.5 py-1 items-center"
                          ariaLabel={`منتجات ${showcase.title}`}
                        >
                          {showcaseProducts.map((p) => renderCompactShowcaseCard(p, 'banner'))}
                        </HorizontalScroller>
                      </div>
                    )}
                  </div>

                  <div className="h-[10%] w-full px-3 sm:px-5 flex items-center justify-center pb-1.5">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate({ name: 'brandShowcase', showcaseId: showcase.id });
                      }}
                      className="w-full max-w-md h-full max-h-11 min-h-[28px] rounded-xl sm:rounded-2xl bg-white/95 hover:bg-white text-[#181A20] font-black text-xs sm:text-sm shadow-sm border border-black/5 flex items-center justify-center gap-1 transition"
                    >
                      <span>عرض الكل</span>
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </section>
            );
          }

          const alignLeft = Boolean(bannerlessAlignLeftMap[showcase.id]);
          return (
            <section
              key={showcase.id}
              onClick={() => navigate({ name: 'brandShowcase', showcaseId: showcase.id })}
              style={{
                background: `linear-gradient(150deg, ${accent} 0%, ${accent}70 42%, #FFFFFF 100%)`,
              }}
              className="relative w-full rounded-3xl overflow-hidden shadow-sm border border-black/8 cursor-pointer p-4 sm:p-6 space-y-4"
            >
              <div
                aria-hidden="true"
                className="pointer-events-none absolute -top-12 -left-10 w-52 h-52 rounded-full bg-white/45 blur-2xl"
              />
              <div
                aria-hidden="true"
                className="pointer-events-none absolute -bottom-16 -right-10 w-56 h-56 rounded-full bg-white/40 blur-2xl"
              />

              {/* Header Row */}
              <div
                className={`relative z-10 flex items-center justify-between gap-3 flex-wrap ${
                  alignLeft ? 'flex-row-reverse' : 'flex-row'
                }`}
              >
                <div
                  className={`flex items-center gap-3.5 min-w-0 ${
                    alignLeft ? 'flex-row-reverse text-left' : 'flex-row text-right'
                  }`}
                >
                  <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-2xl bg-white shadow-sm border border-black/8 p-2 flex items-center justify-center shrink-0 overflow-hidden">
                    {companyLogo ? (
                      <img
                        src={companyLogo}
                        alt={showcase.title}
                        className="w-full h-full object-contain"
                      />
                    ) : (
                      <Building2 className="w-7 h-7 text-[#181A20]" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <h3 className="text-lg sm:text-2xl font-black text-[#181A20] leading-tight truncate">
                      {showcase.title}
                    </h3>
                    {showcase.subtitle ? (
                      <p className="text-xs sm:text-sm font-medium text-[#4A4F5C] line-clamp-1 mt-0.5">
                        {showcase.subtitle}
                      </p>
                    ) : (
                      <p className="text-xs font-bold text-[#5B606E] mt-0.5">
                        {formatNumber(showcaseProducts.length)} منتج متاح من تشكيلة الشركة
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2.5">
                  {showcaseDiscPct > 0 ? (
                    <CompanyOfferBadge
                      discountPercent={showcaseDiscPct}
                      style={storeSettings.companyOfferBadgeStyle}
                    />
                  ) : (
                    <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/85 backdrop-blur-xs border border-black/8 text-[#181A20] text-xs font-extrabold shrink-0">
                      <Store className="w-3.5 h-3.5 text-[#1565C0]" />
                      <span>تشكيلة الشركة</span>
                    </div>
                  )}

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate({ name: 'brandShowcase', showcaseId: showcase.id });
                    }}
                    className="hidden md:inline-flex items-center gap-1 px-4 py-2 rounded-xl bg-[#181A20] hover:bg-[#282B33] text-[#FFD000] font-black text-xs shadow-xs transition"
                  >
                    <span>عرض الكل</span>
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Scrollable Product Rail with Desktop Arrows */}
              {showcaseProducts.length > 0 ? (
                <div onClick={(e) => e.stopPropagation()} className="relative z-10">
                  <HorizontalScroller
                    className="gap-3 sm:gap-3.5 pb-1 items-stretch"
                    ariaLabel={`منتجات ${showcase.title}`}
                  >
                    {showcaseProducts.map((p) => renderCompactShowcaseCard(p, 'card'))}
                  </HorizontalScroller>
                </div>
              ) : (
                <div className="relative z-10 rounded-2xl bg-white/80 border border-black/5 py-7 text-center text-xs font-bold text-[#717784]">
                  اضغط لاستعراض منتجات {showcase.title}
                </div>
              )}

              {/* Mobile / Tablet Bottom Full-Width CTA */}
              <div className="relative z-10 pt-0.5 md:hidden">
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    navigate({ name: 'brandShowcase', showcaseId: showcase.id });
                  }}
                  className="w-full py-2.5 rounded-2xl bg-[#181A20] hover:bg-[#282B33] text-[#FFD000] font-black text-xs sm:text-sm shadow-xs flex items-center justify-center gap-1.5 transition"
                >
                  <span>عرض كل منتجات {showcase.title}</span>
                  <ChevronLeft className="w-4 h-4" />
                </button>
              </div>
            </section>
          );
        })}
      </div>
    );
  };

  const renderSection = (
    title: string,
    subtitle: string,
    items: Product[],
    seeAllAction?: () => void,
    afterPlacement?: BannerPlacement,
  ) => {
    const afterBanners = afterPlacement ? bannersFor(afterPlacement) : [];
    const afterShowcases = afterPlacement ? showcasesFor(afterPlacement) : [];
    if (!items.length && !afterBanners.length && !afterShowcases.length) return null;

    return (
      <div className="space-y-6">
        {items.length > 0 && (
          <section className="space-y-4">
            <div className="flex items-end justify-between gap-3 border-b border-[#EBECEF] pb-3">
              <div className="flex items-center gap-2.5 min-w-0">
                <span className="w-1.5 h-7 rounded-full bg-[#FFD000] shrink-0" />
                <div className="min-w-0">
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-black text-[#181A20] leading-tight truncate">
                    {title}
                  </h2>
                  <p className="hidden sm:block text-xs text-[#717784] mt-0.5">{subtitle}</p>
                </div>
              </div>
              {seeAllAction && (
                <button
                  type="button"
                  onClick={seeAllAction}
                  className="shrink-0 inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-[#EBECEF] hover:border-[#FFD000] hover:bg-[#FFF9DB] text-xs font-extrabold text-[#181A20] transition shadow-2xs"
                >
                  <span>عرض الكل</span>
                  <ArrowLeft className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Screen-Adaptive World-Class Product Grid: 2 (mobile) -> 3 (sm) -> 4 (md) -> 5 (lg) -> 6 (2xl) */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 2xl:grid-cols-6 gap-2.5 sm:gap-3.5 lg:gap-4">
              {items.map((p) => (
                <ProductCard
                  key={p.id}
                  product={p}
                  onOpen={() => navigate({ name: 'product', productId: p.id })}
                />
              ))}
            </div>
          </section>
        )}
        {afterBanners.length > 0 && <BannerCarousel banners={afterBanners} />}
        {afterPlacement && renderBrandShowcasesSlot(afterPlacement)}
      </div>
    );
  };

  const renderWebCategories = (cats: Category[]) => {
    if (!cats.length) return null;
    // Web-independent world-class responsive category grid that adapts gracefully to every viewport width
    return (
      <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 xl:grid-cols-10 gap-2.5 sm:gap-3.5">
        {cats.map((c) => {
          const count = productCountByCat.get(c.id) || 0;
          return (
            <button
              key={c.id}
              type="button"
              onClick={() => navigate({ name: 'categories', initialCategoryId: c.id })}
              className="group bg-white border border-[#E8EAEE] hover:border-[#FFD000] rounded-2xl p-2.5 sm:p-3 flex flex-col items-center text-center gap-2 hover:shadow-md hover:-translate-y-0.5 transition-all"
            >
              <div className="w-14 h-14 sm:w-16 sm:h-16 lg:w-[70px] lg:h-[70px] rounded-2xl bg-[#F8F9FB] border border-[#F0F1F4] group-hover:bg-[#FFFDF2] p-2 overflow-hidden flex items-center justify-center transition">
                {c.imageUrl ? (
                  <img
                    src={c.imageUrl}
                    alt={c.name}
                    loading="lazy"
                    className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
                  />
                ) : (
                  <LayoutGrid className="w-6 h-6 text-[#717784]" />
                )}
              </div>
              <div className="w-full space-y-0.5">
                <div className="text-[11px] sm:text-xs font-extrabold text-[#181A20] line-clamp-2 leading-tight group-hover:text-[#1565C0] transition-colors">
                  {c.name}
                </div>
                <div className="hidden sm:block text-[10px] font-semibold text-[#8B919E]">
                  {formatNumber(count)} منتج
                </div>
              </div>
            </button>
          );
        })}
      </div>
    );
  };

  return (
    <div className="max-w-[1480px] mx-auto px-3 sm:px-5 lg:px-8 py-4 sm:py-5 space-y-6 sm:space-y-8">
      {/* Store Announcement Strip */}
      {storeSettings.announcementText && (
        <div className="bg-[#FFF9DB] border border-[#FFD000] rounded-2xl px-4 py-2.5 flex items-center gap-2.5 text-xs sm:text-sm font-bold text-[#181A20] shadow-2xs">
          <Megaphone className="w-4 h-4 text-[#181A20] shrink-0" />
          <span>{storeSettings.announcementText}</span>
        </div>
      )}

      {/* Staff / Owner Quick Banner */}
      {canAccessAdmin && (
        <div className="bg-[#181A20] text-white rounded-2xl px-4 py-2.5 flex flex-wrap items-center justify-between gap-3 shadow-xs">
          <div className="flex items-center gap-2.5">
            <ShieldCheck className="w-5 h-5 text-[#FFD000] shrink-0" />
            <span className="text-xs sm:text-sm font-bold">
              حساب إداري مفعل — جميع التعديلات متزامنة لحظيًا مع قاعدة البيانات
            </span>
          </div>
          <button
            type="button"
            onClick={() => navigate({ name: 'admin' })}
            className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black hover:bg-[#E5B800] transition"
          >
            <LayoutDashboard className="w-4 h-4" />
            <span>فتح لوحة الإدارة</span>
          </button>
        </div>
      )}

      {/* Placement: TOP_HOME Ticker, Banners & Brand Showcases */}
      {renderTickerSlot('top_home')}
      <BannerCarousel banners={bannersFor('top_home')} />
      {renderBrandShowcasesSlot('top_home')}

      {/* Hero Section (gated on settingsLoaded to prevent startup flash when hidden) */}
      {settingsLoaded && storeSettings.showHeroSection && (
        <section className="rounded-3xl bg-gradient-to-l from-[#181A20] via-[#22252E] to-[#0F172A] text-white p-6 sm:p-8 lg:p-10 border border-[#FFD000]/30 shadow-lg flex flex-col lg:flex-row lg:items-center justify-between gap-8 overflow-hidden relative">
          <div className="space-y-4 max-w-2xl relative z-10">
            {storeSettings.heroBadgeText && (
              <span className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-[#FFD000] text-[#181A20] text-xs font-black shadow-xs">
                <Sparkles className="w-3.5 h-3.5" />
                {storeSettings.heroBadgeText}
              </span>
            )}
            <h1 className="text-2xl sm:text-3xl lg:text-5xl font-black leading-tight text-white">
              {storeSettings.heroTitleText}
            </h1>
            <p className="text-sm sm:text-base lg:text-lg text-white/80 leading-relaxed max-w-xl">
              {storeSettings.heroSubtitleText}
            </p>
            <div className="flex flex-wrap items-center gap-3 pt-1">
              <button
                type="button"
                onClick={() => navigate({ name: 'categories' })}
                className="px-7 py-3 rounded-2xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-black text-sm shadow-lg transition"
              >
                {storeSettings.heroCtaText || 'تسوق الآن'}
              </button>
              <button
                type="button"
                onClick={() => navigate({ name: 'search', onlyOffers: true })}
                className="px-6 py-3 rounded-2xl bg-white/10 hover:bg-white/15 border border-white/15 text-white font-bold text-sm transition"
              >
                استكشف العروض
              </button>
            </div>
          </div>

          {storefrontProducts.length > 0 && (
            <div className="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-2 xl:grid-cols-3 gap-3 lg:w-[400px] xl:w-[460px] shrink-0 relative z-10">
              {storefrontProducts.slice(0, 3).map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => navigate({ name: 'product', productId: item.id })}
                  className="bg-white/95 rounded-2xl p-2.5 text-right hover:scale-[1.03] transition-transform shadow-md flex flex-col gap-1.5"
                >
                  <div className="aspect-square w-full rounded-xl bg-[#F9FAFB] overflow-hidden flex items-center justify-center p-1.5">
                    {item.imageUrls[0] ? (
                      <img src={item.imageUrls[0]} alt={item.name} className="w-full h-full object-contain" />
                    ) : (
                      <LayoutGrid className="w-6 h-6 text-[#717784]" />
                    )}
                  </div>
                  <div className="text-[11px] font-bold text-[#181A20] line-clamp-1">{item.name}</div>
                </button>
              ))}
            </div>
          )}
        </section>
      )}

      {/* Store Trust Pillars Strip */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-2.5 sm:gap-4">
        {[
          { icon: <Truck className="w-5 h-5 text-[#1565C0]" />, title: 'توصيل سريع وآمن', sub: 'لمحافظات ومراكز مصر' },
          { icon: <Store className="w-5 h-5 text-[#219653]" />, title: 'استلام من المتجر', sub: 'تجهيز فوري بدون رسوم شحن' },
          { icon: <Wallet className="w-5 h-5 text-[#D99B00]" />, title: 'دفع إلكتروني ومباشر', sub: 'فودافون وأورنج واتصالات كاش' },
          { icon: <BadgeCheck className="w-5 h-5 text-[#181A20]" />, title: 'أسعار قطاعي وجملة', sub: 'عروض خاصة للتجار والعملاء' },
        ].map((pillar) => (
          <div
            key={pillar.title}
            className="bg-white border border-[#E8EAEE] rounded-2xl px-3.5 py-3 flex items-center gap-3 shadow-2xs hover:border-[#FFD000]/70 transition"
          >
            <div className="w-10 h-10 rounded-xl bg-[#F7F8FA] border border-[#EBECEF] flex items-center justify-center shrink-0">
              {pillar.icon}
            </div>
            <div className="min-w-0">
              <div className="text-xs sm:text-sm font-extrabold text-[#181A20] truncate">{pillar.title}</div>
              <div className="text-[11px] text-[#717784] truncate">{pillar.sub}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Placement: BELOW_HERO */}
      {renderTickerSlot('below_hero')}
      <BannerCarousel banners={bannersFor('below_hero')} />
      {renderBrandShowcasesSlot('below_hero')}

      {/* Flash Sales Section */}
      <FlashSaleSection />

      {/* Web Categories Showcase */}
      {mainCategories.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-end justify-between gap-3 border-b border-[#EBECEF] pb-3">
            <div className="flex items-center gap-2.5">
              <span className="w-1.5 h-7 rounded-full bg-[#FFD000] shrink-0" />
              <div>
                <h2 className="text-lg sm:text-xl lg:text-2xl font-black text-[#181A20] leading-tight">
                  تسوق حسب الفئة
                </h2>
                <p className="hidden sm:block text-xs text-[#717784] mt-0.5">
                  تصفح أقسام المتجر بسهولة للوصول السريع لاحتياجاتك
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => navigate({ name: 'categories' })}
              className="shrink-0 inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-[#EBECEF] hover:border-[#FFD000] hover:bg-[#FFF9DB] text-xs font-extrabold text-[#181A20] transition shadow-2xs"
            >
              <span>كل الفئات</span>
              <ArrowLeft className="w-3.5 h-3.5" />
            </button>
          </div>
          {renderWebCategories(mainCategories)}
        </section>
      )}

      {/* Shop by Brands / Companies Strip (International Store Highlight) */}
      {companies.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-end justify-between gap-3 border-b border-[#EBECEF] pb-3">
            <div className="flex items-center gap-2.5">
              <span className="w-1.5 h-7 rounded-full bg-[#181A20] shrink-0" />
              <div>
                <h2 className="text-lg sm:text-xl lg:text-2xl font-black text-[#181A20] leading-tight">
                  الشركات والعلامات التجارية
                </h2>
                <p className="hidden sm:block text-xs text-[#717784] mt-0.5">
                  اختر شركتك المفضلة لاستعراض كامل منتجاتها وعروضها
                </p>
              </div>
            </div>
          </div>

          <HorizontalScroller className="gap-3 pb-1" ariaLabel="الشركات والعلامات التجارية">
            {companies.map((comp) => {
              const linkedShowcase = showcaseByCompanyId.get(comp.id);
              return (
                <button
                  key={comp.id}
                  type="button"
                  onClick={() => {
                    if (linkedShowcase) {
                      navigate({ name: 'brandShowcase', showcaseId: linkedShowcase.id });
                    } else {
                      navigate({ name: 'search', initialQuery: comp.name });
                    }
                  }}
                  className="group shrink-0 w-32 sm:w-36 lg:w-40 bg-white border border-[#E8EAEE] hover:border-[#FFD000] rounded-2xl p-3 flex flex-col items-center gap-2 shadow-2xs hover:shadow-md hover:-translate-y-0.5 transition-all"
                >
                  <div className="w-16 h-14 sm:w-20 sm:h-16 rounded-xl bg-[#F8F9FB] border border-[#F0F1F4] p-2 flex items-center justify-center overflow-hidden">
                    {comp.imageUrl ? (
                      <img
                        src={comp.imageUrl}
                        alt={comp.name}
                        loading="lazy"
                        className="w-full h-full object-contain group-hover:scale-105 transition-transform"
                      />
                    ) : (
                      <Building2 className="w-6 h-6 text-[#717784]" />
                    )}
                  </div>
                  <span className="text-xs font-extrabold text-[#181A20] truncate w-full text-center group-hover:text-[#1565C0]">
                    {comp.name}
                  </span>
                </button>
              );
            })}
          </HorizontalScroller>
        </section>
      )}

      {/* Placement: BELOW_CATEGORIES */}
      {renderTickerSlot('below_categories')}
      <BannerCarousel banners={bannersFor('below_categories')} />
      {renderBrandShowcasesSlot('below_categories')}

      {catalogLoading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 2xl:grid-cols-6 gap-3 sm:gap-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="h-72 rounded-2xl bg-white border border-[#EBECEF] animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="space-y-8 sm:space-y-10">
          {storeSettings.showFeaturedSection &&
            renderSection(
              'المنتجات المميزة',
              'اختياراتنا الأفضل جودة وطلبًا في المتجر',
              featuredProducts,
              () => navigate({ name: 'search' }),
              'below_featured',
            )}
          {renderTickerSlot('below_featured')}

          {storeSettings.showDealsSection &&
            renderSection(
              'العروض والخصومات',
              'أقوى التخفيضات والأسعار التنافسية المتاحة اليوم',
              dealsProducts,
              () => navigate({ name: 'search', onlyOffers: true }),
              'below_deals',
            )}
          {renderTickerSlot('below_deals')}

          {renderSection(
            'وصل حديثًا',
            'أحدث المنتجات المضافة مؤخرًا إلى المتجر',
            newestProducts,
            () => navigate({ name: 'search', initialSort: 'newest' }),
            'below_newest',
          )}
          {renderTickerSlot('below_newest')}

          {storeSettings.showBestsellersSection &&
            renderSection(
              'الأكثر مبيعًا',
              'المنتجات الأكثر طلبًا وثقة من عملائنا',
              bestsellersProducts,
              () => navigate({ name: 'search', initialSort: 'bestselling' }),
              'below_bestsellers',
            )}
          {renderTickerSlot('below_bestsellers')}

          {storeSettings.showRecommendedSection &&
            renderSection(
              'مقترحة لك',
              'منتجات حاصلة على أعلى تقييمات العملاء',
              recommendedProducts,
              () => navigate({ name: 'search' }),
              'below_recommended',
            )}
          {renderTickerSlot('below_recommended')}
        </div>
      )}
    </div>
  );
};
