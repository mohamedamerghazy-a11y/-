import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Building2, Filter, QrCode, RotateCcw, Search, SlidersHorizontal, X } from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import { ProductCard } from '../../components/common/ProductCard';
import {
  effectiveDiscountPercentWithShowcases,
  effectiveRetailPriceWithShowcases,
  formatNumber,
} from '../../lib/formatters';
import { logSearchMiss } from '../../services/firestoreService';
import { normalizeSearchQuery } from '../../types/models';

interface SearchPageProps {
  initialQuery?: string;
  initialSort?: string;
  onlyOffers?: boolean;
  initialCategoryId?: string;
}

export const SearchPage: React.FC<SearchPageProps> = ({
  initialQuery = '',
  initialSort = 'featured',
  onlyOffers = false,
  initialCategoryId = '',
}) => {
  const {
    firebaseUser,
    catalogLoading,
    storefrontProducts,
    categories,
    companies,
    brandShowcases,
    navigate,
  } = useStore();
  const [queryText, setQueryText] = useState(initialQuery);
  const [selectedCat, setSelectedCat] = useState(initialCategoryId);
  const [selectedCompanyId, setSelectedCompanyId] = useState('');
  const [offersFilter, setOffersFilter] = useState(onlyOffers);
  const [inStockOnly, setInStockOnly] = useState(false);
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sortBy, setSortBy] = useState(initialSort);
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [showCodeModal, setShowCodeModal] = useState(false);
  const [manualCode, setManualCode] = useState('');
  const loggedMissesRef = useRef<Set<string>>(new Set());

  useEffect(() => {
    setQueryText(initialQuery);
    setSortBy(initialSort);
    setOffersFilter(onlyOffers);
    setSelectedCat(initialCategoryId);
  }, [initialQuery, initialSort, onlyOffers, initialCategoryId]);

  const mainCategories = useMemo(() => categories.filter((c) => !c.parentId), [categories]);

  const companyNameMap = useMemo(() => {
    const map = new Map<string, string>();
    for (const c of companies) map.set(c.id, c.name.toLowerCase());
    return map;
  }, [companies]);

  const filtered = useMemo(() => {
    const q = queryText.trim().toLowerCase();
    const minVal = minPrice.trim() ? Number(minPrice) : null;
    const maxVal = maxPrice.trim() ? Number(maxPrice) : null;

    let list = storefrontProducts.filter((p) => {
      if (selectedCat && p.categoryId !== selectedCat && p.subCategoryId !== selectedCat) {
        return false;
      }
      if (selectedCompanyId && p.companyId !== selectedCompanyId) return false;
      if (offersFilter && !p.isOffer && effectiveDiscountPercentWithShowcases(p, brandShowcases) <= 0) {
        return false;
      }
      if (inStockOnly && p.stockQuantity <= 0) return false;

      const price = effectiveRetailPriceWithShowcases(p, brandShowcases);
      if (minVal !== null && !Number.isNaN(minVal) && price < minVal) return false;
      if (maxVal !== null && !Number.isNaN(maxVal) && price > maxVal) return false;

      if (!q) return true;
      const compName = p.companyId ? companyNameMap.get(p.companyId) || '' : '';
      return (
        p.name.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q) ||
        p.sku.toLowerCase().includes(q) ||
        compName.includes(q) ||
        (p.barcode || '').toLowerCase().includes(q) ||
        (p.qrCode || '').toLowerCase().includes(q)
      );
    });

    if (sortBy === 'price_asc') {
      list = [...list].sort(
        (a, b) =>
          effectiveRetailPriceWithShowcases(a, brandShowcases) -
          effectiveRetailPriceWithShowcases(b, brandShowcases),
      );
    } else if (sortBy === 'price_desc') {
      list = [...list].sort(
        (a, b) =>
          effectiveRetailPriceWithShowcases(b, brandShowcases) -
          effectiveRetailPriceWithShowcases(a, brandShowcases),
      );
    } else if (sortBy === 'bestselling') {
      list = [...list].sort((a, b) => b.salesCount - a.salesCount);
    } else if (sortBy === 'newest') {
      list = [...list];
    } else {
      list = [...list].sort((a, b) => Number(b.isFeatured) - Number(a.isFeatured));
    }
    return list;
  }, [
    storefrontProducts,
    queryText,
    selectedCat,
    selectedCompanyId,
    offersFilter,
    inStockOnly,
    minPrice,
    maxPrice,
    sortBy,
    brandShowcases,
    companyNameMap,
  ]);

  const hasActiveFilters = Boolean(
    selectedCat || selectedCompanyId || offersFilter || inStockOnly || minPrice || maxPrice,
  );

  const resetAllFilters = () => {
    setSelectedCat('');
    setSelectedCompanyId('');
    setOffersFilter(false);
    setInStockOnly(false);
    setMinPrice('');
    setMaxPrice('');
  };

  useEffect(() => {
    const trimmed = queryText.trim();
    const hasSecondaryFilters =
      Boolean(selectedCat) || Boolean(selectedCompanyId) || offersFilter || inStockOnly || Boolean(minPrice) || Boolean(maxPrice);
    if (
      !firebaseUser ||
      catalogLoading ||
      storefrontProducts.length === 0 ||
      hasSecondaryFilters ||
      trimmed.length < 2 ||
      filtered.length > 0
    ) {
      return;
    }
    const norm = normalizeSearchQuery(trimmed);
    if (norm.length < 2 || loggedMissesRef.current.has(norm)) return;

    const timer = window.setTimeout(() => {
      loggedMissesRef.current.add(norm);
      logSearchMiss(trimmed, 'web').catch(() => {});
    }, 1200);
    return () => window.clearTimeout(timer);
  }, [
    queryText,
    filtered.length,
    selectedCat,
    selectedCompanyId,
    offersFilter,
    inStockOnly,
    minPrice,
    maxPrice,
    firebaseUser,
    catalogLoading,
    storefrontProducts.length,
  ]);

  const renderFilterControls = () => (
    <div className="space-y-5">
      <div className="flex items-center justify-between pb-2.5 border-b border-[#EBECEF]">
        <div className="flex items-center gap-2 font-black text-sm text-[#181A20]">
          <SlidersHorizontal className="w-4 h-4" />
          <span>تصفية النتائج</span>
        </div>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={resetAllFilters}
            className="inline-flex items-center gap-1 text-[11px] font-bold text-[#E53935] hover:underline"
          >
            <RotateCcw className="w-3 h-3" />
            <span>مسح الكل</span>
          </button>
        )}
      </div>

      {/* Quick Toggles */}
      <div className="space-y-1.5">
        <label className="flex items-center justify-between gap-2 p-2 rounded-xl hover:bg-[#F7F8FA] cursor-pointer text-xs font-bold text-[#181A20]">
          <span>العروض والخصومات فقط</span>
          <input
            type="checkbox"
            checked={offersFilter}
            onChange={(e) => setOffersFilter(e.target.checked)}
            className="w-4 h-4 accent-[#E53935] rounded"
          />
        </label>
        <label className="flex items-center justify-between gap-2 p-2 rounded-xl hover:bg-[#F7F8FA] cursor-pointer text-xs font-bold text-[#181A20]">
          <span>المتوفر بالمخزون فقط</span>
          <input
            type="checkbox"
            checked={inStockOnly}
            onChange={(e) => setInStockOnly(e.target.checked)}
            className="w-4 h-4 accent-[#219653] rounded"
          />
        </label>
      </div>

      {/* Price Range */}
      <div className="space-y-2 pt-2 border-t border-[#EBECEF]">
        <div className="text-xs font-extrabold text-[#717784]">نطاق السعر (ج.م)</div>
        <div className="grid grid-cols-2 gap-2">
          <input
            type="number"
            min={0}
            value={minPrice}
            onChange={(e) => setMinPrice(e.target.value)}
            placeholder="من"
            className="h-9 px-2.5 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold focus:bg-white focus:border-[#FFD000] focus:outline-none"
          />
          <input
            type="number"
            min={0}
            value={maxPrice}
            onChange={(e) => setMaxPrice(e.target.value)}
            placeholder="إلى"
            className="h-9 px-2.5 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold focus:bg-white focus:border-[#FFD000] focus:outline-none"
          />
        </div>
      </div>

      {/* Categories */}
      {mainCategories.length > 0 && (
        <div className="space-y-2 pt-2 border-t border-[#EBECEF]">
          <div className="text-xs font-extrabold text-[#717784]">الأقسام</div>
          <div className="space-y-1 max-h-52 overflow-y-auto pl-1">
            <button
              type="button"
              onClick={() => setSelectedCat('')}
              className={`w-full text-right px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                !selectedCat
                  ? 'bg-[#181A20] text-[#FFD000]'
                  : 'text-[#4A4F5C] hover:bg-[#F7F8FA]'
              }`}
            >
              كل الأقسام
            </button>
            {mainCategories.map((c) => (
              <button
                key={c.id}
                type="button"
                onClick={() => setSelectedCat(c.id)}
                className={`w-full text-right px-3 py-1.5 rounded-xl text-xs font-bold transition truncate ${
                  selectedCat === c.id
                    ? 'bg-[#FFF9DB] text-[#181A20] border border-[#FFD000]'
                    : 'text-[#4A4F5C] hover:bg-[#F7F8FA]'
                }`}
              >
                {c.name}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Companies / Brands */}
      {companies.length > 0 && (
        <div className="space-y-2 pt-2 border-t border-[#EBECEF]">
          <div className="text-xs font-extrabold text-[#717784]">الشركات والعلامات</div>
          <div className="space-y-1 max-h-52 overflow-y-auto pl-1">
            <button
              type="button"
              onClick={() => setSelectedCompanyId('')}
              className={`w-full text-right px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                !selectedCompanyId
                  ? 'bg-[#181A20] text-[#FFD000]'
                  : 'text-[#4A4F5C] hover:bg-[#F7F8FA]'
              }`}
            >
              كل الشركات
            </button>
            {companies.map((comp) => (
              <button
                key={comp.id}
                type="button"
                onClick={() => setSelectedCompanyId(comp.id)}
                className={`w-full flex items-center gap-2 text-right px-2.5 py-1.5 rounded-xl text-xs font-bold transition ${
                  selectedCompanyId === comp.id
                    ? 'bg-[#FFF9DB] text-[#181A20] border border-[#FFD000]'
                    : 'text-[#4A4F5C] hover:bg-[#F7F8FA]'
                }`}
              >
                <div className="w-5 h-5 rounded-md bg-white border border-[#EBECEF] p-0.5 flex items-center justify-center shrink-0 overflow-hidden">
                  {comp.imageUrl ? (
                    <img src={comp.imageUrl} alt={comp.name} className="w-full h-full object-contain" />
                  ) : (
                    <Building2 className="w-3 h-3 text-[#717784]" />
                  )}
                </div>
                <span className="truncate">{comp.name}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );

  return (
    <div className="max-w-[1480px] mx-auto px-3 sm:px-5 lg:px-8 py-5 space-y-5">
      {/* Top Search Toolbar */}
      <div className="bg-white border border-[#E8EAEE] rounded-2xl p-3.5 sm:p-4 shadow-2xs space-y-3">
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="relative flex-1 min-w-[220px]">
            <Search className="w-4 h-4 text-[#717784] absolute right-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="search"
              value={queryText}
              onChange={(e) => setQueryText(e.target.value)}
              placeholder="ابحث بالاسم، الشركة، الوصف، كود SKU، أو الباركود..."
              className="w-full h-11 pr-10 pl-3 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-sm focus:bg-white focus:border-[#FFD000] focus:outline-none transition"
            />
          </div>

          <button
            type="button"
            onClick={() => setShowCodeModal(true)}
            className="h-11 px-4 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-extrabold inline-flex items-center gap-2 hover:bg-[#282B33] transition"
          >
            <QrCode className="w-4 h-4" />
            <span className="hidden sm:inline">إدخال كود باركود / QR</span>
            <span className="sm:hidden">باركود</span>
          </button>

          <button
            type="button"
            onClick={() => setShowMobileFilters((v) => !v)}
            className={`lg:hidden h-11 px-3.5 rounded-xl border text-xs font-extrabold inline-flex items-center gap-1.5 transition ${
              hasActiveFilters
                ? 'bg-[#FFF9DB] border-[#FFD000] text-[#181A20]'
                : 'bg-white border-[#EBECEF] text-[#181A20]'
            }`}
          >
            <Filter className="w-4 h-4" />
            <span>الفلاتر</span>
          </button>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-[#F0F1F4]">
          <div className="text-xs font-bold text-[#5B606E]">
            عرض <span className="text-[#181A20] font-black">{formatNumber(filtered.length)}</span> منتج
            {queryText.trim() && (
              <>
                {' '}
                للبحث عن <span className="text-[#1565C0] font-extrabold">«{queryText.trim()}»</span>
              </>
            )}
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-[#717784] hidden sm:inline">ترتيب حسب:</span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="h-9 px-3 rounded-xl border border-[#EBECEF] bg-[#F9FAFB] font-bold text-xs text-[#181A20] focus:outline-none"
            >
              <option value="featured">الأكثر تميزًا</option>
              <option value="newest">الأحدث</option>
              <option value="bestselling">الأكثر مبيعًا</option>
              <option value="price_asc">السعر: من الأقل للأعلى</option>
              <option value="price_desc">السعر: من الأعلى للأقل</option>
            </select>
          </div>
        </div>
      </div>

      {/* Collapsible Mobile Filter Panel */}
      {showMobileFilters && (
        <div className="lg:hidden bg-white border border-[#E8EAEE] rounded-2xl p-4 shadow-sm">
          {renderFilterControls()}
        </div>
      )}

      {/* Main Desktop 2-Column Layout: Sticky Sidebar + Adaptive Multi-Column Product Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-[270px_1fr] gap-6 items-start">
        <aside className="hidden lg:block sticky top-32 bg-white border border-[#E8EAEE] rounded-2xl p-4 shadow-2xs">
          {renderFilterControls()}
        </aside>

        <div className="min-w-0">
          {filtered.length === 0 ? (
            <div className="bg-white border border-[#E8EAEE] rounded-3xl p-12 text-center space-y-3">
              <div className="font-black text-base text-[#181A20]">لا توجد منتجات مطابقة لبحثك</div>
              <p className="text-xs text-[#717784]">جرب كلمات بحث أخرى أو أعد ضبط الفلاتر الجانبية.</p>
              {hasActiveFilters && (
                <button
                  type="button"
                  onClick={resetAllFilters}
                  className="px-5 py-2 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
                >
                  مسح كل الفلاتر
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5 gap-2.5 sm:gap-3.5 lg:gap-4">
              {filtered.map((p) => (
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

      {showCodeModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 space-y-4 border border-[#EBECEF] shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="font-black text-base">البحث بالباركود أو رمز SKU</h3>
              <button type="button" onClick={() => setShowCodeModal(false)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <input
              type="text"
              value={manualCode}
              onChange={(e) => setManualCode(e.target.value)}
              placeholder="اكتب أو الصق الباركود أو كود المنتج..."
              className="w-full h-11 px-3 rounded-xl border border-[#EBECEF] text-sm focus:border-[#FFD000] focus:outline-none"
            />
            <button
              type="button"
              onClick={() => {
                setQueryText(manualCode);
                setShowCodeModal(false);
              }}
              className="w-full py-2.5 rounded-xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-black text-sm transition"
            >
              بحث الآن
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
