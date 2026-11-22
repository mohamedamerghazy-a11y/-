import React, { useMemo, useState } from 'react';
import {
  ArrowRight,
  ArrowUpDown,
  Building2,
  Package,
  Search,
  ShoppingBasket,
  SlidersHorizontal,
  Store,
  X,
} from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import { CompanyOfferBadge } from '../../components/common/CompanyOfferBadge';
import { ProductCard } from '../../components/common/ProductCard';
import {
  effectiveDiscountPercentWithShowcases,
  effectiveRetailPriceWithShowcases,
  formatNumber,
  matchesBrandShowcase,
  safeShowcaseDiscountPercent,
  toMillis,
} from '../../lib/formatters';
import type { Product } from '../../types/models';

type ShowcaseSort = 'newest' | 'bestselling' | 'price_asc' | 'price_desc' | 'rating';
type ShowcaseFilter = 'all' | 'offers' | 'in_stock';

interface BrandShowcasePageProps {
  showcaseId: string;
}

export const BrandShowcasePage: React.FC<BrandShowcasePageProps> = ({ showcaseId }) => {
  const {
    brandShowcases,
    categories,
    companies,
    products,
    storeSettings,
    effectiveWholesaleActive,
    navigate,
    goBack,
  } = useStore();

  const showcase = useMemo(
    () => brandShowcases.find((b) => b.id === showcaseId) || null,
    [brandShowcases, showcaseId],
  );

  const [selectedCatId, setSelectedCatId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<ShowcaseSort>('newest');
  const [filterMode, setFilterMode] = useState<ShowcaseFilter>('all');

  const companyProducts = useMemo(
    () => (showcase ? products.filter((p) => matchesBrandShowcase(p, showcase)) : []),
    [products, showcase],
  );

  const linkedCompany = useMemo(
    () => (showcase?.companyId ? companies.find((c) => c.id === showcase.companyId) : undefined),
    [companies, showcase],
  );

  const { railCategories, isCustomCategories } = useMemo(() => {
    const custom = showcase?.categories || [];
    if (custom.length > 0) {
      return {
        isCustomCategories: true,
        railCategories: [...custom]
          .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || a.name.localeCompare(b.name, 'ar'))
          .map((c) => ({ id: c.id, name: c.name, imageUrl: c.imageUrl || '' })),
      };
    }

    const catById = new Map(categories.map((c) => [c.id, c]));
    const usedIds = new Set<string>();
    for (const p of companyProducts) {
      const targetCatId = p.subCategoryId || p.categoryId;
      if (targetCatId) usedIds.add(targetCatId);
    }

    const derived = Array.from(usedIds)
      .map((id) => catById.get(id))
      .filter((c): c is NonNullable<typeof c> => Boolean(c))
      .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || a.name.localeCompare(b.name, 'ar'))
      .map((c) => ({ id: c.id, name: c.name, imageUrl: c.imageUrl || '' }));

    return {
      isCustomCategories: false,
      railCategories: derived,
    };
  }, [showcase, categories, companyProducts]);

  const catProductCounts = useMemo(() => {
    const map = new Map<string, number>();
    for (const p of companyProducts) {
      const key = isCustomCategories ? p.brandCategoryId : p.subCategoryId || p.categoryId;
      if (key) map.set(key, (map.get(key) || 0) + 1);
    }
    return map;
  }, [companyProducts, isCustomCategories]);

  const displayedProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    const filtered = companyProducts.filter((p) => {
      if (selectedCatId) {
        if (isCustomCategories) {
          if (p.brandCategoryId !== selectedCatId) return false;
        } else {
          if (p.subCategoryId !== selectedCatId && p.categoryId !== selectedCatId) return false;
        }
      }
      if (filterMode === 'offers' && !p.isOffer && effectiveDiscountPercentWithShowcases(p, brandShowcases) <= 0) {
        return false;
      }
      if (filterMode === 'in_stock' && p.stockQuantity <= 0) return false;
      if (!q) return true;
      return (
        p.name.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q) ||
        p.sku.toLowerCase().includes(q)
      );
    });

    const effectivePrice = (p: Product) =>
      effectiveWholesaleActive && p.wholesalePrice > 0
        ? p.wholesalePrice
        : effectiveRetailPriceWithShowcases(p, brandShowcases);

    return [...filtered].sort((a, b) => {
      switch (sortBy) {
        case 'bestselling':
          return b.salesCount - a.salesCount;
        case 'price_asc':
          return effectivePrice(a) - effectivePrice(b);
        case 'price_desc':
          return effectivePrice(b) - effectivePrice(a);
        case 'rating':
          return b.averageRating - a.averageRating;
        case 'newest':
        default:
          return toMillis(b.createdAt) - toMillis(a.createdAt);
      }
    });
  }, [
    companyProducts,
    selectedCatId,
    isCustomCategories,
    filterMode,
    searchQuery,
    sortBy,
    effectiveWholesaleActive,
    brandShowcases,
  ]);

  if (!showcase) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-14 text-center space-y-4">
        <Store className="w-14 h-14 mx-auto text-[#717784]" />
        <h2 className="text-lg font-black text-[#181A20]">عرض الشركة غير متوفر حاليًا</h2>
        <p className="text-xs text-[#717784]">ربما تم إيقاف هذا العرض مؤقتًا من الإدارة.</p>
        <button
          type="button"
          onClick={goBack}
          className="px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
        >
          العودة للمتجر
        </button>
      </div>
    );
  }

  const companyLogo = linkedCompany?.imageUrl || showcase.companyImageUrl || '';
  const discPct = safeShowcaseDiscountPercent(showcase);
  const accent = showcase.accentColorHex || '#FCE4EC';

  return (
    <div dir="rtl" className="max-w-[1480px] mx-auto px-2 sm:px-5 lg:px-8 py-4 space-y-4">
      {/* Brand Storefront Identity Header */}
      <div
        style={{
          background: `linear-gradient(135deg, ${accent} 0%, ${accent}66 55%, #FFFFFF 100%)`,
        }}
        className="rounded-3xl border border-black/8 p-4 sm:p-6 shadow-xs space-y-4"
      >
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3.5 min-w-0">
            <button
              type="button"
              onClick={goBack}
              aria-label="رجوع"
              className="w-9 h-9 rounded-full bg-white/90 hover:bg-white border border-black/10 flex items-center justify-center text-[#181A20] shrink-0 transition shadow-2xs"
            >
              <ArrowRight className="w-4 h-4" />
            </button>
            <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-2xl bg-white border border-black/8 shadow-xs p-2 flex items-center justify-center shrink-0 overflow-hidden">
              {companyLogo ? (
                <img src={companyLogo} alt={showcase.title} className="w-full h-full object-contain" />
              ) : (
                <Building2 className="w-7 h-7 text-[#181A20]" />
              )}
            </div>
            <div className="min-w-0">
              <h1 className="text-lg sm:text-2xl font-black text-[#181A20] truncate">
                {showcase.title}
              </h1>
              {showcase.subtitle ? (
                <p className="text-xs sm:text-sm text-[#4A4F5C] line-clamp-1 mt-0.5">
                  {showcase.subtitle}
                </p>
              ) : (
                <p className="text-xs font-bold text-[#5B606E] mt-0.5">
                  إجمالي {formatNumber(companyProducts.length)} منتج متاح من الشركة
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-3 flex-wrap">
            {discPct > 0 && (
              <CompanyOfferBadge
                discountPercent={discPct}
                style={storeSettings.companyOfferBadgeStyle}
              />
            )}

            {/* Scoped Search Bar */}
            <div className="flex items-center gap-2 rounded-xl border border-black/10 bg-white/95 px-3 h-10 w-full sm:w-72 shadow-2xs">
              <Search className="w-4 h-4 text-[#717784] shrink-0" />
              <input
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={`ابحث داخل ${showcase.title}...`}
                className="flex-1 bg-transparent text-xs sm:text-sm font-medium text-[#181A20] focus:outline-none"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  aria-label="مسح البحث"
                  className="w-6 h-6 rounded-full flex items-center justify-center text-[#717784] hover:text-[#181A20]"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Main Split Workspace: Adaptive Category Sidebar + Multi-Column Grid */}
      <div className="bg-white rounded-3xl border border-[#E8EAEE] shadow-2xs overflow-hidden flex min-h-[60vh]">
        {/* Category Rail: Compact icon+label on mobile -> Rich full-label sidebar on lg+ */}
        <aside className="w-20 sm:w-24 lg:w-64 shrink-0 bg-[#FAFAFC] border-l border-[#EBECEF] py-3 space-y-1.5 overflow-y-auto">
          <div className="hidden lg:block px-4 pb-2 text-xs font-extrabold text-[#717784] border-b border-[#EBECEF]">
            أقسام {showcase.title}
          </div>

          <button
            type="button"
            onClick={() => setSelectedCatId(null)}
            className={`w-full flex flex-col lg:flex-row items-center gap-1.5 lg:gap-3 px-2 lg:px-4 py-2 transition text-center lg:text-right relative ${
              selectedCatId === null
                ? 'bg-[#FFF9DB]/70 lg:bg-[#181A20] lg:text-[#FFD000]'
                : 'hover:bg-white text-[#181A20]'
            }`}
          >
            <div
              className={`w-12 h-12 lg:w-10 lg:h-10 rounded-xl flex items-center justify-center border shrink-0 transition ${
                selectedCatId === null
                  ? 'bg-white border-[#FFD000]'
                  : 'bg-white border-[#EBECEF]'
              }`}
            >
              <ShoppingBasket className="w-5 h-5 text-[#219653]" />
            </div>
            <div className="min-w-0 lg:flex-1">
              <span
                className={`block text-[11px] lg:text-xs leading-tight ${
                  selectedCatId === null
                    ? 'font-black text-[#181A20] lg:text-[#FFD000]'
                    : 'font-bold text-[#181A20]'
                }`}
              >
                الكل
              </span>
              <span className="hidden lg:block text-[10px] opacity-75">
                {formatNumber(companyProducts.length)} منتج
              </span>
            </div>
          </button>

          {railCategories.map((cat) => {
            const active = selectedCatId === cat.id;
            const count = catProductCounts.get(cat.id) || 0;
            return (
              <button
                key={cat.id}
                type="button"
                onClick={() => setSelectedCatId(cat.id)}
                className={`w-full flex flex-col lg:flex-row items-center gap-1.5 lg:gap-3 px-2 lg:px-4 py-2 transition text-center lg:text-right ${
                  active
                    ? 'bg-[#FFF9DB] border-r-3 border-[#FFD000]'
                    : 'hover:bg-white'
                }`}
              >
                <div
                  className={`w-12 h-12 lg:w-10 lg:h-10 rounded-xl overflow-hidden bg-white p-1 border flex items-center justify-center shrink-0 transition ${
                    active ? 'border-[#FFD000] ring-1 ring-[#FFD000]/40' : 'border-[#EBECEF]'
                  }`}
                >
                  {cat.imageUrl ? (
                    <img src={cat.imageUrl} alt={cat.name} className="w-full h-full object-contain" />
                  ) : (
                    <Store className="w-5 h-5 text-[#717784]" />
                  )}
                </div>
                <div className="min-w-0 lg:flex-1">
                  <span
                    className={`block text-[11px] lg:text-xs leading-tight line-clamp-2 lg:line-clamp-1 ${
                      active ? 'font-black text-[#181A20]' : 'font-semibold text-[#5B606E]'
                    }`}
                  >
                    {cat.name}
                  </span>
                  {count > 0 && (
                    <span className="hidden lg:block text-[10px] text-[#717784]">
                      {formatNumber(count)} منتج
                    </span>
                  )}
                </div>
              </button>
            );
          })}
        </aside>

        {/* Main Products Column */}
        <div className="flex-1 min-w-0 p-3 sm:p-5 space-y-4">
          <div className="flex flex-wrap items-center gap-2 pb-3 border-b border-[#F0F1F4]">
            <label className="inline-flex items-center gap-1.5 px-3 h-9 rounded-xl border border-[#EBECEF] bg-[#F9FAFB] text-xs font-bold text-[#181A20]">
              <ArrowUpDown className="w-3.5 h-3.5 text-[#717784]" />
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as ShowcaseSort)}
                className="bg-transparent focus:outline-none font-bold"
              >
                <option value="newest">الترتيب: الأحدث</option>
                <option value="bestselling">الأكثر مبيعًا</option>
                <option value="price_asc">السعر: الأقل</option>
                <option value="price_desc">السعر: الأعلى</option>
                <option value="rating">الأعلى تقييمًا</option>
              </select>
            </label>

            <label className="inline-flex items-center gap-1.5 px-3 h-9 rounded-xl border border-[#EBECEF] bg-[#F9FAFB] text-xs font-bold text-[#181A20]">
              <SlidersHorizontal className="w-3.5 h-3.5 text-[#717784]" />
              <select
                value={filterMode}
                onChange={(e) => setFilterMode(e.target.value as ShowcaseFilter)}
                className="bg-transparent focus:outline-none font-bold"
              >
                <option value="all">التصنيف: الكل</option>
                <option value="offers">العروض والخصومات فقط</option>
                <option value="in_stock">المتوفر بالمخزون فقط</option>
              </select>
            </label>

            <span className="mr-auto text-xs font-extrabold text-[#5B606E]">
              {formatNumber(displayedProducts.length)} منتج
            </span>
          </div>

          {displayedProducts.length === 0 ? (
            <div className="py-16 text-center space-y-3">
              <Package className="w-12 h-12 mx-auto text-[#C4C9D2]" />
              <div className="text-sm font-black text-[#181A20]">لا توجد منتجات في هذه الفئة</div>
              {(selectedCatId || searchQuery || filterMode !== 'all') && (
                <button
                  type="button"
                  onClick={() => {
                    setSelectedCatId(null);
                    setSearchQuery('');
                    setFilterMode('all');
                  }}
                  className="px-4 py-2 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
                >
                  عرض كل منتجات الشركة
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-2.5 sm:gap-3.5 lg:gap-4">
              {displayedProducts.map((p) => (
                <ProductCard
                  key={p.id}
                  product={p}
                  onOpen={() => navigate({ name: 'product', productId: p.id })}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
