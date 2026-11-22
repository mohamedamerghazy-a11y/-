import React, { useMemo, useState } from 'react';
import { BellRing, Edit2, Plus, Trash2, AlertTriangle, Sparkles } from 'lucide-react';
import type { Category, Product, StoreCompany } from '../../types/models';
import { useStore } from '../../context/StoreContext';
import {
  deleteCategoryDoc,
  deleteProductDoc,
  dispatchRestockNotificationsAndClearAlerts,
  saveCategoryDoc,
  saveProductDoc,
  updateProductOfferSettings,
  updateProductStock,
} from '../../services/firestoreService';
import { formatCurrency, formatNumber, toArabicErrorMessage } from '../../lib/formatters';
import { ImageUploader } from '../../components/common/ImageUploader';

// 1. Products Management
export const AdminProductsTab: React.FC<{
  products: Product[];
  categories: Category[];
  companies?: StoreCompany[];
  stockAlertCounts?: Record<string, number>;
}> = ({ products, categories, companies = [], stockAlertCounts = {} }) => {
  const { showToast } = useStore();
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<Partial<Product> | null>(null);
  const [colorInput, setColorInput] = useState('');
  const [sizeInput, setSizeInput] = useState('');
  const [saving, setSaving] = useState(false);

  const mainCats = useMemo(() => categories.filter((c) => !c.parentId), [categories]);
  const catNameMap = useMemo(() => new Map(categories.map((c) => [c.id, c.name])), [categories]);
  const companyNameMap = useMemo(() => new Map(companies.map((c) => [c.id, c.name])), [companies]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return products;
    return products.filter(
      (p) => p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q),
    );
  }, [products, search]);

  const openNew = () => {
    setEditing({
      name: '',
      description: '',
      categoryId: mainCats[0]?.id || '',
      subCategoryId: '',
      companyId: '',
      exclusiveToBrandShowcase: false,
      sku: `SKU-${Math.floor(1000 + Math.random() * 9000)}`,
      barcode: '',
      imageUrls: [],
      retailPrice: 0,
      retailDiscountPrice: undefined,
      wholesalePrice: 0,
      wholesaleMinQty: 1,
      unit: 'قطعة',
      stockQuantity: 10,
      lowStockThreshold: 5,
      isFeatured: false,
      isOffer: false,
      isActive: true,
      availableColors: [],
      availableSizes: [],
    });
    setColorInput('');
    setSizeInput('');
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editing) return;
    if (!editing.name?.trim() || editing.name.trim().length < 2) {
      showToast('اسم المنتج مطلوب (حرفان على الأقل).', 'error');
      return;
    }
    if (!editing.categoryId) {
      showToast('يرجى اختيار القسم الرئيسي للمنتج.', 'error');
      return;
    }
    if (!editing.retailPrice || editing.retailPrice <= 0) {
      showToast('يرجى إدخال سعر بيع صحيح أكبر من صفر.', 'error');
      return;
    }
    setSaving(true);
    try {
      const previousStock = editing.id
        ? (products.find((item) => item.id === editing.id)?.stockQuantity ?? 0)
        : 0;
      const savedId = await saveProductDoc(editing);
      const nextStock = Math.max(0, Number(editing.stockQuantity || 0));
      let notifiedCount = 0;
      if (editing.id && previousStock <= 0 && nextStock > 0) {
        notifiedCount = await dispatchRestockNotificationsAndClearAlerts(
          savedId,
          editing.name.trim(),
        ).catch(() => 0);
      }
      showToast(
        notifiedCount > 0
          ? `تم حفظ المنتج وإرسال تنبيه توفر إلى ${formatNumber(notifiedCount)} عميل`
          : 'تم حفظ بيانات المنتج بنجاح',
        'success',
      );
      setEditing(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ المنتج.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="ابحث بالاسم أو SKU..."
          className="h-10 px-3 rounded-xl border border-[#EBECEF] bg-white text-xs sm:text-sm min-w-[220px] flex-1 max-w-md"
        />
        <button
          type="button"
          onClick={openNew}
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
        >
          <Plus className="w-4 h-4" />
          <span>إضافة منتج جديد</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {filtered.map((p) => (
          <div
            key={p.id}
            className="bg-white border border-[#EBECEF] rounded-2xl p-3.5 flex items-center gap-3"
          >
            <img
              src={p.imageUrls[0] || 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg"/>'}
              alt=""
              className="w-16 h-16 rounded-xl object-contain bg-[#F9FAFB] border border-[#EBECEF] shrink-0"
            />
            <div className="flex-1 min-w-0">
              <div className="font-bold text-sm truncate">{p.name}</div>
              <div className="text-[11px] text-[#717784]">
                {catNameMap.get(p.categoryId) || 'قسم'} • SKU: {p.sku} • المخزون: {formatNumber(p.stockQuantity)}
              </div>
              <div className="flex flex-wrap gap-2 mt-1 text-xs font-extrabold">
                <span>{formatCurrency(p.retailPrice)}</span>
                {p.wholesalePrice > 0 && (
                  <span className="text-[#1565C0]">جملة: {formatCurrency(p.wholesalePrice)}</span>
                )}
                {!p.isActive && (
                  <span className="px-2 py-0.5 rounded-full bg-gray-200 text-gray-700 text-[10px]">مخفي</span>
                )}
                {(p.availableColors?.length || 0) > 0 && (
                  <span className="px-2 py-0.5 rounded-full bg-[#FFF9DB] text-[#181A20] text-[10px]">
                    {formatNumber(p.availableColors!.length)} ألوان
                  </span>
                )}
                {(p.availableSizes?.length || 0) > 0 && (
                  <span className="px-2 py-0.5 rounded-full bg-[#EBF4FF] text-[#1565C0] text-[10px]">
                    {formatNumber(p.availableSizes!.length)} مقاسات
                  </span>
                )}
                {p.companyId && companyNameMap.get(p.companyId) && (
                  <span className="px-2 py-0.5 rounded-full bg-[#F3E8FF] text-[#6B21A8] text-[10px] font-black">
                    شركة: {companyNameMap.get(p.companyId)}
                  </span>
                )}
                {(p.companyId || p.brandShowcaseId) && (
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] ${
                      p.exclusiveToBrandShowcase
                        ? 'bg-[#FDECEC] text-[#E53935]'
                        : 'bg-[#EBF4FF] text-[#1565C0]'
                    }`}
                  >
                    {p.exclusiveToBrandShowcase ? 'حصري للشركة فقط' : 'عام + شركة'}
                  </span>
                )}
                {(stockAlertCounts[p.id] || 0) > 0 && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-[#FFF9DB] border border-[#FFD000] text-[#181A20] text-[10px] font-black">
                    <BellRing className="w-3 h-3" />
                    في قائمة الانتظار: {formatNumber(stockAlertCounts[p.id])} عميل
                  </span>
                )}
              </div>
            </div>
            <div className="flex flex-col gap-1.5 shrink-0">
              <button
                type="button"
                onClick={() => {
                  setEditing({
                    ...p,
                    availableColors: p.availableColors || [],
                    availableSizes: p.availableSizes || [],
                  });
                  setColorInput('');
                  setSizeInput('');
                }}
                className="px-3 py-1.5 rounded-lg bg-[#FFF9DB] text-[#181A20] text-xs font-bold inline-flex items-center gap-1"
              >
                <Edit2 className="w-3.5 h-3.5" />
                <span>تعديل</span>
              </button>
              <button
                type="button"
                onClick={async () => {
                  if (confirm(`حذف المنتج «${p.name}»؟`)) {
                    try {
                      await deleteProductDoc(p.id);
                      showToast('تم حذف المنتج', 'info');
                    } catch (err) {
                      showToast(toArabicErrorMessage(err, 'تعذر الحذف.'), 'error');
                    }
                  }
                }}
                className="px-3 py-1.5 rounded-lg bg-[#FDECEC] text-[#E53935] text-xs font-bold inline-flex items-center gap-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>حذف</span>
              </button>
            </div>
          </div>
        ))}
      </div>

      {editing && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form
            onSubmit={handleSave}
            className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-5 space-y-4 border border-[#EBECEF]"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-black text-base">{editing.id ? 'تعديل المنتج' : 'إضافة منتج جديد'}</h3>
              <button type="button" onClick={() => setEditing(null)} className="text-xs font-bold text-[#717784]">
                إغلاق
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div>
                <label className="block font-bold mb-1">اسم المنتج *</label>
                <input
                  value={editing.name || ''}
                  onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  required
                />
              </div>
              <div>
                <label className="block font-bold mb-1">كود SKU *</label>
                <input
                  value={editing.sku || ''}
                  onChange={(e) => setEditing({ ...editing, sku: e.target.value })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  required
                />
              </div>
              <div>
                <label className="block font-bold mb-1">القسم الرئيسي *</label>
                <select
                  value={editing.categoryId || ''}
                  onChange={(e) => setEditing({ ...editing, categoryId: e.target.value, subCategoryId: '' })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  required
                >
                  <option value="">اختر القسم...</option>
                  {mainCats.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block font-bold mb-1">القسم الفرعي (اختياري)</label>
                <select
                  value={editing.subCategoryId || ''}
                  onChange={(e) => setEditing({ ...editing, subCategoryId: e.target.value })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                >
                  <option value="">بدون</option>
                  {categories
                    .filter((c) => c.parentId === editing.categoryId)
                    .map((sub) => (
                      <option key={sub.id} value={sub.id}>{sub.name}</option>
                    ))}
                </select>
              </div>
              <div className="sm:col-span-2">
                <label className="block font-bold mb-1">الشركة التابع لها (اختياري)</label>
                <select
                  value={editing.companyId || ''}
                  onChange={(e) =>
                    setEditing({
                      ...editing,
                      companyId: e.target.value || undefined,
                      exclusiveToBrandShowcase:
                        e.target.value || editing.brandShowcaseId
                          ? editing.exclusiveToBrandShowcase
                          : false,
                    })
                  }
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] bg-[#FAFAFC] font-bold"
                >
                  <option value="">بدون شركة محددة</option>
                  {companies
                    .filter((c) => c.isActive || c.id === editing.companyId)
                    .map((comp) => (
                      <option key={comp.id} value={comp.id}>
                        {comp.name}
                      </option>
                    ))}
                </select>
                {companies.length === 0 && (
                  <p className="text-[11px] text-[#717784] mt-1">
                    يمكنك إضافة الشركات أولًا من «إعدادات المتجر ← إضافة شركات للمتجر».
                  </p>
                )}
              </div>
              <div>
                <label className="block font-bold mb-1">سعر البيع الأساسي *</label>
                <input
                  type="number"
                  step="0.01"
                  value={editing.retailPrice ?? 0}
                  onChange={(e) => setEditing({ ...editing, retailPrice: Number(e.target.value) })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  required
                />
              </div>
              <div>
                <label className="block font-bold mb-1">سعر العرض (اختياري)</label>
                <input
                  type="number"
                  step="0.01"
                  value={editing.retailDiscountPrice ?? ''}
                  onChange={(e) =>
                    setEditing({
                      ...editing,
                      retailDiscountPrice: e.target.value ? Number(e.target.value) : undefined,
                    })
                  }
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
              <div>
                <label className="block font-bold mb-1">سعر الجملة</label>
                <input
                  type="number"
                  step="0.01"
                  value={editing.wholesalePrice ?? 0}
                  onChange={(e) => setEditing({ ...editing, wholesalePrice: Number(e.target.value) })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
              <div>
                <label className="block font-bold mb-1">الحد الأدنى لكمية الجملة</label>
                <input
                  type="number"
                  min="1"
                  value={editing.wholesaleMinQty ?? 1}
                  onChange={(e) => setEditing({ ...editing, wholesaleMinQty: Number(e.target.value) })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
              <div>
                <label className="block font-bold mb-1">الكمية بالمخزون</label>
                <input
                  type="number"
                  min="0"
                  value={editing.stockQuantity ?? 0}
                  onChange={(e) => setEditing({ ...editing, stockQuantity: Number(e.target.value) })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
              <div>
                <label className="block font-bold mb-1">الوحدة</label>
                <input
                  value={editing.unit || 'قطعة'}
                  onChange={(e) => setEditing({ ...editing, unit: e.target.value })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
              <div>
                <label className="block font-bold mb-1">الباركود</label>
                <input
                  value={editing.barcode || ''}
                  onChange={(e) => setEditing({ ...editing, barcode: e.target.value })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
              <div>
                <label className="block font-bold mb-1">حد التنبيه لنقص المخزون</label>
                <input
                  type="number"
                  min="0"
                  value={editing.lowStockThreshold ?? 5}
                  onChange={(e) => setEditing({ ...editing, lowStockThreshold: Number(e.target.value) })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold mb-1">الوصف</label>
              <textarea
                rows={2}
                value={editing.description || ''}
                onChange={(e) => setEditing({ ...editing, description: e.target.value })}
                className="w-full p-3 rounded-xl border border-[#EBECEF] text-xs"
              />
            </div>

            <div className="space-y-1">
              <label className="block text-xs font-bold">صور المنتج</label>
              <ImageUploader
                folder="aleman/products"
                multiple
                urls={editing.imageUrls || []}
                onChange={(urls) => setEditing({ ...editing, imageUrls: urls })}
              />
            </div>

            {/* Product Colors & Sizes Variant Editors */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-3.5 text-xs">
              <div className="space-y-2">
                <label className="block font-black text-[#181A20]">الألوان المتاحة (اختياري)</label>
                <div className="flex gap-1.5">
                  <input
                    value={colorInput}
                    onChange={(e) => setColorInput(e.target.value)}
                    placeholder="أضف لونًا (أحمر، أبيض...)"
                    className="flex-1 h-9 px-2.5 rounded-xl border border-[#EBECEF] bg-white"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      const clean = colorInput.trim();
                      if (!clean) return;
                      const current = editing.availableColors || [];
                      if (!current.includes(clean)) {
                        setEditing({ ...editing, availableColors: [...current, clean].slice(0, 30) });
                      }
                      setColorInput('');
                    }}
                    className="px-3 h-9 rounded-xl bg-[#181A20] text-[#FFD000] font-bold"
                  >
                    إضافة
                  </button>
                </div>
                <div className="flex flex-wrap gap-1">
                  {['أسود', 'أبيض', 'أزرق', 'أحمر', 'أخضر', 'رمادي'].map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => {
                        const current = editing.availableColors || [];
                        if (!current.includes(preset)) {
                          setEditing({ ...editing, availableColors: [...current, preset].slice(0, 30) });
                        }
                      }}
                      className="px-2 py-0.5 rounded-lg bg-white border border-[#EBECEF] hover:border-[#FFD000] text-[10px] font-bold"
                    >
                      + {preset}
                    </button>
                  ))}
                </div>
                {(editing.availableColors || []).length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {(editing.availableColors || []).map((c) => (
                      <span
                        key={c}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#FFF9DB] border border-[#FFD000] font-bold text-[11px]"
                      >
                        {c}
                        <button
                          type="button"
                          onClick={() =>
                            setEditing({
                              ...editing,
                              availableColors: (editing.availableColors || []).filter((x) => x !== c),
                            })
                          }
                          className="text-[#E53935] font-black"
                        >
                          ×
                        </button>
                      </span>
                    ))}
                  </div>
                )}
              </div>

              <div className="space-y-2">
                <label className="block font-black text-[#181A20]">المقاسات / الأحجام (اختياري)</label>
                <div className="flex gap-1.5">
                  <input
                    value={sizeInput}
                    onChange={(e) => setSizeInput(e.target.value)}
                    placeholder="أضف مقاسًا (L، ٥٠٠ مل...)"
                    className="flex-1 h-9 px-2.5 rounded-xl border border-[#EBECEF] bg-white"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      const clean = sizeInput.trim();
                      if (!clean) return;
                      const current = editing.availableSizes || [];
                      if (!current.includes(clean)) {
                        setEditing({ ...editing, availableSizes: [...current, clean].slice(0, 30) });
                      }
                      setSizeInput('');
                    }}
                    className="px-3 h-9 rounded-xl bg-[#181A20] text-[#FFD000] font-bold"
                  >
                    إضافة
                  </button>
                </div>
                <div className="flex flex-wrap gap-1">
                  {['S', 'M', 'L', 'XL', '٢٥٠ مل', '٥٠٠ مل', '١ لتر'].map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => {
                        const current = editing.availableSizes || [];
                        if (!current.includes(preset)) {
                          setEditing({ ...editing, availableSizes: [...current, preset].slice(0, 30) });
                        }
                      }}
                      className="px-2 py-0.5 rounded-lg bg-white border border-[#EBECEF] hover:border-[#FFD000] text-[10px] font-bold"
                    >
                      + {preset}
                    </button>
                  ))}
                </div>
                {(editing.availableSizes || []).length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {(editing.availableSizes || []).map((s) => (
                      <span
                        key={s}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#EBF4FF] border border-[#1565C0]/40 text-[#1565C0] font-bold text-[11px]"
                      >
                        {s}
                        <button
                          type="button"
                          onClick={() =>
                            setEditing({
                              ...editing,
                              availableSizes: (editing.availableSizes || []).filter((x) => x !== s),
                            })
                          }
                          className="text-[#E53935] font-black"
                        >
                          ×
                        </button>
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="flex flex-wrap gap-4 text-xs font-bold">
              <label className="flex items-center gap-1.5">
                <input
                  type="checkbox"
                  checked={editing.isActive ?? true}
                  onChange={(e) => setEditing({ ...editing, isActive: e.target.checked })}
                />
                <span>مفعّل بالمتجر</span>
              </label>
              <label className="flex items-center gap-1.5">
                <input
                  type="checkbox"
                  checked={Boolean(editing.isFeatured)}
                  onChange={(e) => setEditing({ ...editing, isFeatured: e.target.checked })}
                />
                <span>منتج مميز</span>
              </label>
              <label className="flex items-center gap-1.5">
                <input
                  type="checkbox"
                  checked={Boolean(editing.isOffer)}
                  onChange={(e) => setEditing({ ...editing, isOffer: e.target.checked })}
                />
                <span>ضمن العروض</span>
              </label>
              {(editing.companyId || editing.brandShowcaseId) && (
                <label className="flex items-center gap-1.5 text-[#E53935]">
                  <input
                    type="checkbox"
                    checked={Boolean(editing.exclusiveToBrandShowcase)}
                    onChange={(e) =>
                      setEditing({ ...editing, exclusiveToBrandShowcase: e.target.checked })
                    }
                  />
                  <span>حصري للشركة فقط (يخفي من المتجر الرئيسي)</span>
                </label>
              )}
            </div>

            <button
              type="submit"
              disabled={saving}
              className="w-full py-3 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
            >
              {saving ? 'جاري الحفظ...' : 'حفظ المنتج'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
};

// 2. Categories Management
export const AdminCategoriesTab: React.FC<{ categories: Category[] }> = ({ categories }) => {
  const { showToast } = useStore();
  const [editing, setEditing] = useState<Partial<Category> | null>(null);
  const mainCats = useMemo(() => categories.filter((c) => !c.parentId), [categories]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editing?.name?.trim()) return;
    try {
      await saveCategoryDoc(editing);
      showToast('تم حفظ القسم بنجاح', 'success');
      setEditing(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ القسم.'), 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => setEditing({ name: '', description: '', parentId: '', imageUrl: '', displayOrder: 0, isActive: true })}
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
        >
          <Plus className="w-4 h-4" />
          <span>إضافة قسم</span>
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
        {categories.map((c) => (
          <div key={c.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              {c.imageUrl && <img src={c.imageUrl} alt="" className="w-12 h-12 rounded-xl object-cover shrink-0" />}
              <div className="min-w-0">
                <div className="font-bold text-sm truncate">{c.name}</div>
                <div className="text-[11px] text-[#717784]">
                  {c.parentId ? 'قسم فرعي' : 'قسم رئيسي'} • ترتيب: {formatNumber(c.displayOrder)}
                </div>
              </div>
            </div>
            <div className="flex gap-1.5 shrink-0">
              <button
                type="button"
                onClick={() => setEditing({ ...c })}
                className="p-2 rounded-lg bg-[#FFF9DB] text-[#181A20]"
              >
                <Edit2 className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={async () => {
                  if (confirm(`حذف القسم «${c.name}»؟`)) {
                    await deleteCategoryDoc(c.id);
                    showToast('تم حذف القسم', 'info');
                  }
                }}
                className="p-2 rounded-lg bg-[#FDECEC] text-[#E53935]"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {editing && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleSave} className="bg-white rounded-3xl max-w-md w-full p-5 space-y-3 border border-[#EBECEF]">
            <div className="flex items-center justify-between">
              <h3 className="font-black text-base">{editing.id ? 'تعديل القسم' : 'قسم جديد'}</h3>
              <button type="button" onClick={() => setEditing(null)} className="text-xs font-bold text-[#717784]">إغلاق</button>
            </div>
            <input
              value={editing.name || ''}
              onChange={(e) => setEditing({ ...editing, name: e.target.value })}
              placeholder="اسم القسم *"
              className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs"
              required
            />
            <textarea
              value={editing.description || ''}
              onChange={(e) => setEditing({ ...editing, description: e.target.value })}
              placeholder="وصف مختصر"
              rows={2}
              className="w-full p-3 rounded-xl border border-[#EBECEF] text-xs"
            />
            <select
              value={editing.parentId || ''}
              onChange={(e) => setEditing({ ...editing, parentId: e.target.value })}
              className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs"
            >
              <option value="">قسم رئيسي</option>
              {mainCats.filter((m) => m.id !== editing.id).map((m) => (
                <option key={m.id} value={m.id}>فرعي من: {m.name}</option>
              ))}
            </select>
            <input
              type="number"
              min="0"
              value={editing.displayOrder ?? 0}
              onChange={(e) => setEditing({ ...editing, displayOrder: Number(e.target.value) })}
              placeholder="الترتيب"
              className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs"
            />
            <ImageUploader
              folder="aleman/categories"
              urls={editing.imageUrl ? [editing.imageUrl] : []}
              label="صورة القسم"
              onChange={(urls) => setEditing({ ...editing, imageUrl: urls[0] || '' })}
            />
            <label className="flex items-center gap-2 text-xs font-bold">
              <input
                type="checkbox"
                checked={editing.isActive ?? true}
                onChange={(e) => setEditing({ ...editing, isActive: e.target.checked })}
              />
              <span>القسم مفعّل</span>
            </label>
            <button type="submit" className="w-full py-3 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm">
              حفظ القسم
            </button>
          </form>
        </div>
      )}
    </div>
  );
};

// 3. Offers & Featured Quick Manager
export const AdminOffersTab: React.FC<{ products: Product[] }> = ({ products }) => {
  const { showToast } = useStore();
  const [discountEdits, setDiscountEdits] = useState<Record<string, string>>({});

  return (
    <div className="space-y-3">
      {products.map((p) => {
        const val = discountEdits[p.id] ?? (p.retailDiscountPrice ? String(p.retailDiscountPrice) : '');
        return (
          <div key={p.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
            <div className="min-w-[200px]">
              <div className="font-bold text-sm flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-[#FFD000]" />
                <span>{p.name}</span>
              </div>
              <div className="text-xs text-[#717784]">السعر الأساسي: {formatCurrency(p.retailPrice)}</div>
            </div>

            <div className="flex flex-wrap items-center gap-2 text-xs">
              <input
                type="number"
                step="0.01"
                value={val}
                onChange={(e) => setDiscountEdits({ ...discountEdits, [p.id]: e.target.value })}
                placeholder="سعر الخصم"
                className="w-28 h-9 px-2.5 rounded-xl border border-[#EBECEF]"
              />
              <button
                type="button"
                onClick={async () => {
                  const num = val.trim() ? Number(val) : null;
                  if (num != null && (num <= 0 || num >= p.retailPrice)) {
                    showToast('يجب أن يكون سعر العرض أقل من السعر الأساسي وأكبر من صفر.', 'error');
                    return;
                  }
                  await updateProductOfferSettings(p.id, num, num != null || p.isOffer, p.isFeatured);
                  showToast('تم حفظ العرض', 'success');
                }}
                className="px-3 h-9 rounded-xl bg-[#181A20] text-[#FFD000] font-bold"
              >
                تطبيق
              </button>
              <button
                type="button"
                onClick={async () => {
                  await updateProductOfferSettings(p.id, p.retailDiscountPrice ?? null, !p.isOffer, p.isFeatured);
                  showToast('تم تحديث حالة العرض', 'info');
                }}
                className={`px-3 h-9 rounded-xl font-bold border ${
                  p.isOffer ? 'bg-[#E53935] text-white border-[#E53935]' : 'bg-[#F9FAFB] border-[#EBECEF]'
                }`}
              >
                {p.isOffer ? 'عرض نشط' : 'تفعيل كعرض'}
              </button>
              <button
                type="button"
                onClick={async () => {
                  await updateProductOfferSettings(p.id, p.retailDiscountPrice ?? null, p.isOffer, !p.isFeatured);
                  showToast('تم تحديث حالة التمييز', 'info');
                }}
                className={`px-3 h-9 rounded-xl font-bold border ${
                  p.isFeatured ? 'bg-[#FFD000] text-[#181A20] border-[#FFD000]' : 'bg-[#F9FAFB] border-[#EBECEF]'
                }`}
              >
                {p.isFeatured ? 'منتج مميز' : 'تمييز'}
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
};

// 4. Inventory & Low Stock Manager
export const AdminInventoryTab: React.FC<{
  products: Product[];
  stockAlertCounts?: Record<string, number>;
}> = ({ products, stockAlertCounts = {} }) => {
  const { showToast } = useStore();
  const [stockEdits, setStockEdits] = useState<Record<string, { qty: number; low: number; active: boolean }>>({});

  const lowOrOut = products.filter((p) => p.stockQuantity <= p.lowStockThreshold);

  return (
    <div className="space-y-4">
      {lowOrOut.length > 0 && (
        <div className="bg-[#FDECEC] border border-[#E53935]/30 rounded-2xl p-4 flex items-center gap-3 text-xs font-bold text-[#E53935]">
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <span>يوجد {formatNumber(lowOrOut.length)} منتج وصل إلى حد التنبيه أو نفد من المخزون.</span>
        </div>
      )}

      <div className="space-y-2.5">
        {products.map((p) => {
          const st = stockEdits[p.id] ?? {
            qty: p.stockQuantity,
            low: p.lowStockThreshold,
            active: p.isActive,
          };
          const isLow = p.stockQuantity <= p.lowStockThreshold;
          const waitCount = stockAlertCounts[p.id] || 0;
          return (
            <div
              key={p.id}
              className={`bg-white border rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3 ${
                isLow ? 'border-[#E53935]/50' : 'border-[#EBECEF]'
              }`}
            >
              <div className="space-y-1">
                <div className="font-bold text-sm">{p.name}</div>
                <div className="text-[11px] text-[#717784] flex flex-wrap items-center gap-2">
                  <span>SKU: {p.sku}</span>
                  {waitCount > 0 && (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-[#FFF9DB] border border-[#FFD000] text-[#181A20] text-[10px] font-black">
                      <BellRing className="w-3 h-3" />
                      في قائمة الانتظار: {formatNumber(waitCount)} عميل
                    </span>
                  )}
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-2 text-xs">
                <label className="flex items-center gap-1">
                  <span>الكمية:</span>
                  <input
                    type="number"
                    min="0"
                    value={st.qty}
                    onChange={(e) =>
                      setStockEdits({ ...stockEdits, [p.id]: { ...st, qty: Number(e.target.value) } })
                    }
                    className="w-20 h-9 px-2 rounded-xl border border-[#EBECEF]"
                  />
                </label>
                <label className="flex items-center gap-1">
                  <span>التنبيه:</span>
                  <input
                    type="number"
                    min="0"
                    value={st.low}
                    onChange={(e) =>
                      setStockEdits({ ...stockEdits, [p.id]: { ...st, low: Number(e.target.value) } })
                    }
                    className="w-16 h-9 px-2 rounded-xl border border-[#EBECEF]"
                  />
                </label>
                <label className="flex items-center gap-1 font-bold">
                  <input
                    type="checkbox"
                    checked={st.active}
                    onChange={(e) =>
                      setStockEdits({ ...stockEdits, [p.id]: { ...st, active: e.target.checked } })
                    }
                  />
                  <span>نشط</span>
                </label>
                <button
                  type="button"
                  onClick={async () => {
                    try {
                      await updateProductStock(p.id, st.qty, st.low, st.active);
                      let notified = 0;
                      if (p.stockQuantity <= 0 && st.qty > 0) {
                        notified = await dispatchRestockNotificationsAndClearAlerts(p.id, p.name).catch(
                          () => 0,
                        );
                      }
                      showToast(
                        notified > 0
                          ? `تم تحديث مخزون «${p.name}» وإشعار ${formatNumber(notified)} عميل بالتوفر`
                          : `تم تحديث مخزون «${p.name}»`,
                        'success',
                      );
                    } catch (err) {
                      showToast(toArabicErrorMessage(err, 'تعذر تحديث المخزون.'), 'error');
                    }
                  }}
                  className="px-3.5 h-9 rounded-xl bg-[#FFD000] text-[#181A20] font-black"
                >
                  حفظ
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
