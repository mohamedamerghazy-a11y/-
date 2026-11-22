import React, { useEffect, useMemo, useState } from 'react';
import { ArrowRight, ArrowUpDown, LayoutGrid, SlidersHorizontal } from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import { ProductCard } from '../../components/common/ProductCard';
import {
  effectiveDiscountPercentWithShowcases,
  effectiveRetailPriceWithShowcases,
  formatNumber,
} from '../../lib/formatters';

interface CategoriesPageProps {
  initialCategoryId?: string;
}

type CategorySort = 'featured' | 'newest' | 'bestselling' | 'price_asc' | 'price_desc';

export const CategoriesPage: React.FC<CategoriesPageProps> = ({ initialCategoryId }) => {
  const { categories, storefrontProducts, brandShowcases, navigate } = useStore();
  const [selectedMainId, setSelectedMainId] = useState<string | null>(initialCategoryId || null);
  const [selectedSubId, setSelectedSubId] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<CategorySort>('featured');
  const [offersOnly, setOffersOnly] = useState(false);
  const [inStockOnly, setInStockOnly] = useState(false);

  useEffect(() => {
    if (initialCategoryId) {
      setSelectedMainId(initialCategoryId);
      setSelectedSubId(null);
    }
  }, [initialCategoryId]);

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

  const subCategories = useMemo(
    () => (selectedMainId ? subCategoriesByParent.get(selectedMainId) || [] : []),
    [subCategoriesByParent, selectedMainId],
  );

  const selectedCategory = categories.find((c) => c.id === selectedMainId) || null;

  const productCounts = useMemo(() => {
    const map = new Map<string, number>();
    for (const p of storefrontProducts) {
      map.set(p.categoryId, (map.get(p.categoryId) || 0) + 1);
      if (p.subCategoryId) map.set(p.subCategoryId, (map.get(p.subCategoryId) || 0) + 1);
    }
    return map;
  }, [storefrontProducts]);

  const filteredProducts = useMemo(() => {
    if (!selectedMainId) return [];
    let list = storefrontProducts.filter((p) => {
      if (selectedSubId) {
        if (p.subCategoryId !== selectedSubId) return false;
      } else if (p.categoryId !== selectedMainId) {
        return false;
      }
      if (offersOnly && !p.isOffer && effectiveDiscountPercentWithShowcases(p, brandShowcases) <= 0) {
        return false;
      }
      if (inStockOnly && p.stockQuantity <= 0) return false;
      return true;
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
    } else if (sortBy === 'featured') {
      list = [...list].sort((a, b) => Number(b.isFeatured) - Number(a.isFeatured));
    }
    return list;
  }, [
    storefrontProducts,
    selectedMainId,
    selectedSubId,
    offersOnly,
    inStockOnly,
    sortBy,
    brandShowcases,
  ]);

  if (selectedMainId && selectedCategory) {
    return (
      <div className="max-w-[1480px] mx-auto px-3 sm:px-5 lg:px-8 py-5 space-y-5">
        {/* Sleek Compact Category Header */}
        <div className="flex flex-wrap items-center justify-between gap-3 bg-white border border-[#E8EAEE] rounded-2xl px-3.5 py-2.5 shadow-2xs">
          <div className="flex items-center gap-2.5 min-w-0">
            <button
              type="button"
              aria-label="رجوع"
              onClick={() => {
                setSelectedMainId(null);
                setSelectedSubId(null);
                setOffersOnly(false);
                setInStockOnly(false);
              }}
              className="w-8 h-8 rounded-full bg-[#F4F5F8] border border-[#EBECEF] flex items-center justify-center hover:bg-[#FFF9DB] shrink-0 transition"
            >
              <ArrowRight className="w-4 h-4" />
            </button>
            <div className="w-10 h-10 rounded-xl bg-white border border-[#EBECEF] p-1 flex items-center justify-center overflow-hidden shrink-0">
              {selectedCategory.imageUrl ? (
                <img
                  src={selectedCategory.imageUrl}
                  alt={selectedCategory.name}
                  className="w-full h-full object-contain"
                />
              ) : (
                <LayoutGrid className="w-4 h-4 text-[#717784]" />
              )}
            </div>
            <div className="min-w-0">
              <h1 className="text-sm sm:text-lg font-black text-[#181A20] truncate">
                {selectedCategory.name}
              </h1>
              {selectedCategory.description && (
                <p className="hidden sm:block text-xs text-[#717784] line-clamp-1">
                  {selectedCategory.description}
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2 mr-auto sm:mr-0">
            <label className="inline-flex items-center gap-1.5 px-3 h-9 rounded-xl border border-[#EBECEF] bg-[#F9FAFB] text-xs font-bold text-[#181A20]">
              <ArrowUpDown className="w-3.5 h-3.5 text-[#717784]" />
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as CategorySort)}
                className="bg-transparent focus:outline-none font-bold"
              >
                <option value="featured">الأكثر تميزًا</option>
                <option value="newest">الأحدث</option>
                <option value="bestselling">الأكثر مبيعًا</option>
                <option value="price_asc">السعر: الأقل</option>
                <option value="price_desc">السعر: الأعلى</option>
              </select>
            </label>
            <span className="px-3 py-1.5 rounded-full bg-[#FFF9DB] border border-[#FFD000]/50 text-xs font-extrabold text-[#181A20] shrink-0">
              {formatNumber(filteredProducts.length)} منتج
            </span>
          </div>
        </div>

        {/* Mobile/Tablet Quick Subcategories + Filter Pills */}
        <div className="lg:hidden space-y-2.5">
          {subCategories.length > 0 && (
            <div className="flex gap-2 overflow-x-auto no-scrollbar pb-1">
              <button
                type="button"
                onClick={() => setSelectedSubId(null)}
                className={`shrink-0 px-3.5 py-1.5 rounded-full text-xs font-bold border transition ${
                  !selectedSubId
                    ? 'bg-[#181A20] border-[#181A20] text-[#FFD000]'
                    : 'bg-white border-[#EBECEF] text-[#181A20]'
                }`}
              >
                الكل ({formatNumber(productCounts.get(selectedMainId) || 0)})
              </button>
              {subCategories.map((sub) => (
                <button
                  key={sub.id}
                  type="button"
                  onClick={() => setSelectedSubId(sub.id)}
                  className={`shrink-0 px-3.5 py-1.5 rounded-full text-xs font-bold border transition ${
                    selectedSubId === sub.id
                      ? 'bg-[#FFD000] border-[#FFD000] text-[#181A20]'
                      : 'bg-white border-[#EBECEF] text-[#181A20]'
                  }`}
                >
                  {sub.name}
                </button>
              ))}
            </div>
          )}

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => setOffersOnly((v) => !v)}
              className={`px-3 py-1.5 rounded-xl border text-xs font-bold transition ${
                offersOnly
                  ? 'bg-[#E53935] border-[#E53935] text-white'
                  : 'bg-white border-[#EBECEF] text-[#181A20]'
              }`}
            >
              العروض فقط
            </button>
            <button
              type="button"
              onClick={() => setInStockOnly((v) => !v)}
              className={`px-3 py-1.5 rounded-xl border text-xs font-bold transition ${
                inStockOnly
                  ? 'bg-[#219653] border-[#219653] text-white'
                  : 'bg-white border-[#EBECEF] text-[#181A20]'
              }`}
            >
              المتوفر فقط
            </button>
          </div>
        </div>

        {/* Desktop 2-Column E-Commerce Layout (Sticky Filter Sidebar + Multi-Column Grid) */}
        <div className="grid grid-cols-1 lg:grid-cols-[250px_1fr] gap-6 items-start">
          {/* Desktop Sidebar */}
          <aside className="hidden lg:block sticky top-32 bg-white border border-[#E8EAEE] rounded-2xl p-4 space-y-5 shadow-2xs">
            <div className="flex items-center gap-2 pb-2.5 border-b border-[#EBECEF]">
              <SlidersHorizontal className="w-4 h-4 text-[#181A20]" />
              <span className="font-black text-sm text-[#181A20]">تصفية القسم</span>
            </div>

            {subCategories.length > 0 && (
              <div className="space-y-2">
                <div className="text-xs font-extrabold text-[#717784]">الأقسام الفرعية</div>
                <div className="space-y-1">
                  <button
                    type="button"
                    onClick={() => setSelectedSubId(null)}
                    className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-bold transition ${
                      !selectedSubId
                        ? 'bg-[#181A20] text-[#FFD000]'
                        : 'hover:bg-[#F7F8FA] text-[#181A20]'
                    }`}
                  >
                    <span>الكل في {selectedCategory.name}</span>
                    <span className="opacity-75">
                      {formatNumber(productCounts.get(selectedMainId) || 0)}
                    </span>
                  </button>
                  {subCategories.map((sub) => (
                    <button
                      key={sub.id}
                      type="button"
                      onClick={() => setSelectedSubId(sub.id)}
                      className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-bold transition ${
                        selectedSubId === sub.id
                          ? 'bg-[#FFF9DB] text-[#181A20] border border-[#FFD000]'
                          : 'hover:bg-[#F7F8FA] text-[#4A4F5C]'
                      }`}
                    >
                      <span className="truncate">{sub.name}</span>
                      <span className="text-[11px] opacity-75">
                        {formatNumber(productCounts.get(sub.id) || 0)}
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div className="space-y-2 pt-2 border-t border-[#EBECEF]">
              <div className="text-xs font-extrabold text-[#717784]">خيارات سريعة</div>
              <label className="flex items-center justify-between gap-2 p-2 rounded-xl hover:bg-[#F7F8FA] cursor-pointer text-xs font-bold text-[#181A20]">
                <span>العروض والخصومات فقط</span>
                <input
                  type="checkbox"
                  checked={offersOnly}
                  onChange={(e) => setOffersOnly(e.target.checked)}
                  className="w-4 h-4 accent-[#181A20] rounded"
                />
              </label>
              <label className="flex items-center justify-between gap-2 p-2 rounded-xl hover:bg-[#F7F8FA] cursor-pointer text-xs font-bold text-[#181A20]">
                <span>المتوفر بالمخزون فقط</span>
                <input
                  type="checkbox"
                  checked={inStockOnly}
                  onChange={(e) => setInStockOnly(e.target.checked)}
                  className="w-4 h-4 accent-[#181A20] rounded"
                />
              </label>
            </div>
          </aside>

          {/* Products Grid */}
          <div className="min-w-0">
            {filteredProducts.length === 0 ? (
              <div className="bg-white border border-[#E8EAEE] rounded-2xl p-12 text-center space-y-3">
                <div className="text-sm font-black text-[#181A20]">
                  لا توجد منتجات مطابقة في هذا القسم حاليًا
                </div>
                {(selectedSubId || offersOnly || inStockOnly) && (
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedSubId(null);
                      setOffersOnly(false);
                      setInStockOnly(false);
                    }}
                    className="px-4 py-2 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
                  >
                    إعادة ضبط الفلاتر
                  </button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5 gap-2.5 sm:gap-3.5 lg:gap-4">
                {filteredProducts.map((p) => (
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
  }

  return (
    <div className="max-w-[1480px] mx-auto px-3 sm:px-5 lg:px-8 py-5 space-y-6">
      <div className="flex items-end justify-between border-b border-[#EBECEF] pb-3.5">
        <div className="flex items-center gap-2.5">
          <span className="w-1.5 h-7 rounded-full bg-[#FFD000]" />
          <div>
            <h1 className="text-xl sm:text-2xl font-black text-[#181A20]">جميع الأقسام والفئات</h1>
            <p className="text-xs sm:text-sm text-[#717784] mt-0.5">
              اختر القسم لاستعراض المنتجات والأقسام الفرعية والعروض المتوفرة
            </p>
          </div>
        </div>
        <span className="px-3 py-1 rounded-full bg-white border border-[#EBECEF] text-xs font-bold text-[#5B606E]">
          {formatNumber(mainCategories.length)} قسم
        </span>
      </div>

      {/* Screen-Adaptive World-Class Category Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
        {mainCategories.map((c) => {
          const subs = subCategoriesByParent.get(c.id) || [];
          return (
            <button
              key={c.id}
              type="button"
              onClick={() => setSelectedMainId(c.id)}
              className="group bg-white border border-[#E8EAEE] hover:border-[#FFD000] rounded-2xl p-4 flex flex-col items-center gap-3 text-center transition-all hover:shadow-md hover:-translate-y-0.5"
            >
              <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-2xl bg-[#F8F9FB] border border-[#F0F1F4] group-hover:bg-[#FFFDF2] p-2.5 overflow-hidden flex items-center justify-center transition">
                {c.imageUrl ? (
                  <img
                    src={c.imageUrl}
                    alt={c.name}
                    loading="lazy"
                    className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
                  />
                ) : (
                  <LayoutGrid className="w-9 h-9 text-[#717784]" />
                )}
              </div>
              <div className="space-y-1 w-full">
                <div className="font-black text-sm text-[#181A20] group-hover:text-[#1565C0] line-clamp-1 transition-colors">
                  {c.name}
                </div>
                <div className="flex items-center justify-center gap-1.5 flex-wrap">
                  <span className="px-2 py-0.5 rounded-full bg-[#FFF9DB] text-[#181A20] text-[11px] font-bold">
                    {formatNumber(productCounts.get(c.id) || 0)} منتج
                  </span>
                  {subs.length > 0 && (
                    <span className="px-2 py-0.5 rounded-full bg-[#F4F5F8] text-[#5B606E] text-[10px] font-bold">
                      {formatNumber(subs.length)} فرعي
                    </span>
                  )}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
};
