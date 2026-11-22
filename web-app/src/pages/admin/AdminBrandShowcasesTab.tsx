import React, { useMemo, useState } from 'react';
import {
  ArrowDown,
  ArrowUp,
  Building2,
  Edit2,
  FolderPlus,
  Layers,
  Link2,
  Link2Off,
  Lock,
  Percent,
  Plus,
  Globe,
  Sparkles,
  Store,
  Trash2,
  TriangleAlert,
} from 'lucide-react';
import {
  BANNER_PLACEMENT_LABELS,
  type BannerPlacement,
  type BrandShowcase,
  type BrandShowcaseCategory,
  type Category,
  type Product,
  type StoreCompany,
} from '../../types/models';
import { useStore } from '../../context/StoreContext';
import {
  deleteBrandShowcaseDoc,
  saveBrandShowcaseDoc,
  saveProductDoc,
} from '../../services/firestoreService';
import {
  formatCurrency,
  formatNumber,
  getEffectiveRetailPrice,
  matchesBrandShowcase,
  safeShowcaseDiscountPercent,
  toArabicErrorMessage,
} from '../../lib/formatters';
import { ImageUploader } from '../../components/common/ImageUploader';

const ACCENT_PRESETS: Array<{ hex: string; label: string }> = [
  { hex: '#FCE4EC', label: 'وردي ناعم' },
  { hex: '#FFF9DB', label: 'أصفر نون' },
  { hex: '#E8F4FD', label: 'أزرق سماوي' },
  { hex: '#ECFDF5', label: 'أخضر نعناعي' },
  { hex: '#F3E8FF', label: 'بنفسجي هادئ' },
  { hex: '#FFF3E0', label: 'برتقالي دافئ' },
];

const DISCOUNT_PRESETS = [0, 5, 10, 15, 20, 25, 30];

export const AdminBrandShowcasesTab: React.FC<{
  brandShowcases: BrandShowcase[];
  companies?: StoreCompany[];
  products: Product[];
  categories: Category[];
}> = ({ brandShowcases, companies = [], products, categories }) => {
  const { showToast } = useStore();
  const [editingShowcase, setEditingShowcase] = useState<Partial<BrandShowcase> | null>(null);
  const [managingShowcaseId, setManagingShowcaseId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Category editor inside manager
  const [editingCat, setEditingCat] = useState<Partial<BrandShowcaseCategory> | null>(null);

  // Add New Product modal state inside manager
  const [newProd, setNewProd] = useState<Partial<Product> | null>(null);

  // Link Existing Product modal state inside manager
  const [showLinkModal, setShowLinkModal] = useState(false);
  const [linkSearch, setLinkSearch] = useState('');
  const [linkSelectedProdId, setLinkSelectedProdId] = useState('');
  const [linkBrandCatId, setLinkBrandCatId] = useState('');
  const [linkExclusive, setLinkExclusive] = useState(false);

  const mainCategories = useMemo(() => categories.filter((c) => !c.parentId), [categories]);
  const companyMap = useMemo(() => new Map(companies.map((c) => [c.id, c])), [companies]);

  const managingShowcase = useMemo(
    () => (managingShowcaseId ? brandShowcases.find((b) => b.id === managingShowcaseId) || null : null),
    [brandShowcases, managingShowcaseId],
  );

  const productsInShowcase = useMemo(() => {
    if (!managingShowcase) return [];
    return products.filter((p) => matchesBrandShowcase(p, managingShowcase));
  }, [products, managingShowcase]);

  const linkableProducts = useMemo(() => {
    if (!managingShowcase) return [];
    const q = linkSearch.trim().toLowerCase();
    return products
      .filter((p) => !matchesBrandShowcase(p, managingShowcase))
      .filter((p) => !q || p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q))
      .slice(0, 40);
  }, [products, managingShowcase, linkSearch]);

  const editingMatchedCount = useMemo(() => {
    if (!editingShowcase) return 0;
    const cid = editingShowcase.companyId?.trim();
    const sid = editingShowcase.id?.trim();
    if (!cid && !sid) return 0;
    return products.filter(
      (p) => p.isActive && ((cid && p.companyId === cid) || (sid && p.brandShowcaseId === sid)),
    ).length;
  }, [editingShowcase, products]);

  const handleSaveShowcase = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingShowcase) return;
    if (!editingShowcase.title || editingShowcase.title.trim().length < 2) {
      showToast('يرجى اختيار الشركة أو إدخال اسم واضح (حرفان على الأقل).', 'error');
      return;
    }
    const disc = Number(editingShowcase.discountPercent || 0);
    if (Number.isNaN(disc) || disc < 0 || disc > 95) {
      showToast('نسبة الخصم يجب أن تكون بين ٠ و ٩٥٪.', 'error');
      return;
    }
    setSaving(true);
    try {
      const isCreate = !editingShowcase.id;
      const selectedCompany = editingShowcase.companyId ? companyMap.get(editingShowcase.companyId) : undefined;
      await saveBrandShowcaseDoc({
        ...editingShowcase,
        companyImageUrl: selectedCompany?.imageUrl || editingShowcase.companyImageUrl || '',
        discountPercent: disc,
      });
      showToast(
        isCreate ? 'تم إضافة منتجات الشركة بنجاح!' : 'تم حفظ تعديلات منتجات الشركة.',
        'success',
      );
      setEditingShowcase(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ منتجات الشركة.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleMoveOrder = async (showcase: BrandShowcase, delta: -1 | 1) => {
    const idx = brandShowcases.findIndex((b) => b.id === showcase.id);
    const swapIdx = idx + delta;
    if (idx < 0 || swapIdx < 0 || swapIdx >= brandShowcases.length) return;
    const other = brandShowcases[swapIdx];
    const orderA = showcase.displayOrder;
    const orderB = other.displayOrder;
    const nextA = orderA === orderB ? Math.max(0, orderA + delta) : orderB;
    const nextB = orderA === orderB ? Math.max(0, orderA) : orderA;
    try {
      await Promise.all([
        saveBrandShowcaseDoc({ ...showcase, displayOrder: nextA }),
        saveBrandShowcaseDoc({ ...other, displayOrder: nextB }),
      ]);
      showToast('تم تحديث ترتيب الظهور', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر تغيير الترتيب.'), 'error');
    }
  };

  const handleSaveCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!managingShowcase || !editingCat) return;
    if (!editingCat.name || editingCat.name.trim().length < 2) {
      showToast('يرجى إدخال اسم الفئة (حرفان على الأقل).', 'error');
      return;
    }
    if (!editingCat.imageUrl || editingCat.imageUrl.trim().length < 5) {
      showToast('يرجى رفع صورة لفئة الشركة.', 'error');
      return;
    }
    const cleanId = editingCat.id?.trim() || `bcat_${Date.now()}`;
    const updatedCat: BrandShowcaseCategory = {
      id: cleanId,
      name: editingCat.name.trim(),
      imageUrl: editingCat.imageUrl.trim(),
      displayOrder: Math.max(0, Number(editingCat.displayOrder ?? 0)),
    };
    const nextCats = [...(managingShowcase.categories || [])];
    const existingIdx = nextCats.findIndex((c) => c.id === cleanId);
    if (existingIdx >= 0) nextCats[existingIdx] = updatedCat;
    else nextCats.push(updatedCat);

    setSaving(true);
    try {
      await saveBrandShowcaseDoc({ ...managingShowcase, categories: nextCats });
      showToast('تم حفظ فئة الشركة بنجاح', 'success');
      setEditingCat(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ الفئة.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteCategory = async (catId: string) => {
    if (!managingShowcase) return;
    const nextCats = (managingShowcase.categories || []).filter((c) => c.id !== catId);
    const fallbackCatId = nextCats[0]?.id;
    setSaving(true);
    try {
      await saveBrandShowcaseDoc({ ...managingShowcase, categories: nextCats });
      const affected = productsInShowcase.filter((p) => p.brandCategoryId === catId);
      for (const prod of affected) {
        await saveProductDoc({
          ...prod,
          brandCategoryId: fallbackCatId,
          brandShowcaseId: fallbackCatId ? managingShowcase.id : undefined,
          exclusiveToBrandShowcase: fallbackCatId ? Boolean(prod.exclusiveToBrandShowcase) : false,
        });
      }
      showToast('تم حذف الفئة', 'info');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حذف الفئة.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveNewProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!managingShowcase || !newProd) return;
    if (!managingShowcase.categories.length) {
      showToast('أضف فئة واحدة على الأقل للشركة أولًا.', 'error');
      return;
    }
    if (!newProd.brandCategoryId) {
      showToast('يرجى تحديد فئة الشركة التابع لها المنتج.', 'error');
      return;
    }
    if (!newProd.categoryId) {
      showToast('يرجى اختيار القسم الرئيسي للمنتج في المتجر.', 'error');
      return;
    }
    if (!newProd.name || newProd.name.trim().length < 2) {
      showToast('يرجى إدخال اسم المنتج.', 'error');
      return;
    }
    if (!newProd.retailPrice || Number(newProd.retailPrice) <= 0) {
      showToast('يرجى إدخال سعر قطاعي أكبر من صفر.', 'error');
      return;
    }
    setSaving(true);
    try {
      await saveProductDoc({
        ...newProd,
        brandShowcaseId: managingShowcase.id,
      });
      showToast('تم إضافة منتج الشركة بنجاح', 'success');
      setNewProd(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر إضافة المنتج.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleLinkExisting = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!managingShowcase || !linkSelectedProdId || !linkBrandCatId) return;
    const prod = products.find((p) => p.id === linkSelectedProdId);
    if (!prod) return;
    setSaving(true);
    try {
      await saveProductDoc({
        ...prod,
        brandShowcaseId: managingShowcase.id,
        brandCategoryId: linkBrandCatId,
        exclusiveToBrandShowcase: linkExclusive,
      });
      showToast('تم ربط المنتج بالشركة بنجاح', 'success');
      setShowLinkModal(false);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر ربط المنتج.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="font-black text-base text-[#181A20]">منتجات الشركات</h2>
          <p className="text-xs text-[#717784]">
            اختر الشركة ليتم جلب جميع منتجاتها تلقائيًا، مع إمكانية إضافة خصم عام اختياري أو بنر إعلاني اختياري وتحديد مكان الظهور.
          </p>
        </div>
        <button
          type="button"
          onClick={() =>
            setEditingShowcase({
              title: '',
              subtitle: '',
              companyId: '',
              companyImageUrl: '',
              bannerImageUrl: '',
              discountPercent: 0,
              accentColorHex: '#FCE4EC',
              placement: 'below_categories',
              displayOrder: brandShowcases.length,
              isActive: true,
              categories: [],
            })
          }
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
        >
          <Plus className="w-4 h-4" />
          <span>إضافة منتجات شركة</span>
        </button>
      </div>

      {brandShowcases.length === 0 ? (
        <div className="bg-white border border-[#EBECEF] rounded-2xl p-10 text-center space-y-3">
          <Store className="w-12 h-12 mx-auto text-[#717784]" />
          <div className="font-black text-sm">لا توجد أقسام «منتجات شركات» حتى الآن</div>
          <p className="text-xs text-[#717784]">
            اختر شركة من الشركات المضافة في الإعدادات لتظهر كل منتجاتها تلقائيًا بتصميم جذاب أو فوق بنر إعلاني مع خيار خصم عام.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {brandShowcases.map((showcase, idx) => {
            const linkedCount = products.filter((p) => matchesBrandShowcase(p, showcase)).length;
            const linkedCompany = showcase.companyId ? companyMap.get(showcase.companyId) : undefined;
            const logoUrl = linkedCompany?.imageUrl || showcase.companyImageUrl || '';
            const discPct = safeShowcaseDiscountPercent(showcase);
            const hasBanner = Boolean(showcase.bannerImageUrl?.trim());
            return (
              <div
                key={showcase.id}
                className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden flex flex-col"
              >
                <div
                  style={{
                    background: hasBanner
                      ? showcase.accentColorHex || '#FCE4EC'
                      : `linear-gradient(135deg, ${showcase.accentColorHex || '#FCE4EC'}, #FFFFFF)`,
                  }}
                  className="w-full relative overflow-hidden"
                >
                  {hasBanner ? (
                    <img
                      src={showcase.bannerImageUrl}
                      alt={showcase.title}
                      className="w-full h-auto block"
                    />
                  ) : (
                    <div className="w-full p-5 pt-11 flex items-center gap-3 min-h-[120px]">
                      <div className="w-14 h-14 rounded-2xl bg-white border border-black/10 shadow-xs overflow-hidden flex items-center justify-center shrink-0">
                        {logoUrl ? (
                          <img src={logoUrl} alt={showcase.title} className="w-full h-full object-contain p-1.5" />
                        ) : (
                          <Building2 className="w-7 h-7 text-[#181A20]" />
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-white/85 text-[#181A20] text-[10px] font-bold mb-1">
                          <Sparkles className="w-3 h-3 text-[#E5B800]" />
                          <span>تصميم تلقائي بدون بنر</span>
                        </div>
                        <div className="font-black text-base text-[#181A20] truncate">{showcase.title}</div>
                      </div>
                    </div>
                  )}
                  <span className="absolute top-2.5 right-2.5 px-2.5 py-1 rounded-lg bg-[#181A20]/85 text-[#FFD000] text-[11px] font-black">
                    الترتيب: {formatNumber(showcase.displayOrder)}
                  </span>
                  {discPct > 0 && (
                    <span className="absolute top-2.5 left-2.5 px-2.5 py-1 rounded-lg bg-[#E53935] text-white text-[11px] font-black">
                      خصم {formatNumber(discPct)}٪
                    </span>
                  )}
                </div>

                <div className="p-4 space-y-3 flex-1 flex flex-col justify-between">
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2 min-w-0">
                        <span
                          className="w-4 h-4 rounded-full border border-[#EBECEF] shrink-0"
                          style={{ backgroundColor: showcase.accentColorHex || '#FCE4EC' }}
                        />
                        <h3 className="font-black text-sm truncate">{showcase.title}</h3>
                      </div>
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-black ${
                          showcase.isActive
                            ? 'bg-[#E8F6EE] text-[#219653]'
                            : 'bg-[#FDECEC] text-[#E53935]'
                        }`}
                      >
                        {showcase.isActive ? 'مفعّل' : 'مخفي'}
                      </span>
                    </div>

                    {showcase.subtitle && (
                      <p className="text-xs text-[#717784] line-clamp-1">{showcase.subtitle}</p>
                    )}

                    <div className="flex flex-wrap gap-1.5 text-[11px] font-bold">
                      <span className="px-2.5 py-0.5 rounded-full bg-[#EBF4FF] text-[#1565C0]">
                        {BANNER_PLACEMENT_LABELS[showcase.placement] || showcase.placement}
                      </span>
                      <span className="px-2.5 py-0.5 rounded-full bg-[#E8F6EE] text-[#219653]">
                        {formatNumber(linkedCount)} منتج
                      </span>
                      {linkedCompany && (
                        <span className="px-2.5 py-0.5 rounded-full bg-[#F3E8FF] text-[#6B21A8]">
                          شركة: {linkedCompany.name}
                        </span>
                      )}
                      {discPct > 0 && (
                        <span className="px-2.5 py-0.5 rounded-full bg-[#FDECEC] text-[#E53935]">
                          عرض خصم {formatNumber(discPct)}٪
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="space-y-2 pt-2 border-t border-[#EBECEF]">
                    <button
                      type="button"
                      onClick={() => setManagingShowcaseId(showcase.id)}
                      className="w-full py-2 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-[#181A20] text-xs font-black flex items-center justify-center gap-1.5 hover:bg-[#FFF9DB]"
                    >
                      <Layers className="w-4 h-4" />
                      <span>إدارة فئات ومحتوى إضافي (اختياري)</span>
                    </button>

                    <div className="flex items-center justify-between gap-2 text-xs">
                      <label className="inline-flex items-center gap-1.5 font-bold cursor-pointer">
                        <input
                          type="checkbox"
                          checked={showcase.isActive}
                          onChange={() =>
                            saveBrandShowcaseDoc({ ...showcase, isActive: !showcase.isActive })
                          }
                        />
                        <span>ظهور القسم</span>
                      </label>

                      <div className="flex items-center gap-1">
                        <button
                          type="button"
                          disabled={idx === 0}
                          onClick={() => handleMoveOrder(showcase, -1)}
                          title="تحريك للأعلى"
                          className="p-1.5 rounded-lg bg-[#F9FAFB] border border-[#EBECEF] disabled:opacity-35"
                        >
                          <ArrowUp className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          disabled={idx === brandShowcases.length - 1}
                          onClick={() => handleMoveOrder(showcase, 1)}
                          title="تحريك للأسفل"
                          className="p-1.5 rounded-lg bg-[#F9FAFB] border border-[#EBECEF] disabled:opacity-35"
                        >
                          <ArrowDown className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setEditingShowcase(showcase)}
                          title="تعديل"
                          className="p-1.5 rounded-lg bg-[#FFF9DB] text-[#181A20]"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={async () => {
                            if (confirm(`حذف قسم منتجات «${showcase.title}»؟`)) {
                              const legacyLinked = products.filter((p) => p.brandShowcaseId === showcase.id);
                              await deleteBrandShowcaseDoc(showcase.id, legacyLinked);
                              showToast('تم الحذف بنجاح', 'info');
                            }
                          }}
                          title="حذف"
                          className="p-1.5 rounded-lg bg-[#FDECEC] text-[#E53935]"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal 1: Create / Edit Company Products Showcase */}
      {editingShowcase && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form
            onSubmit={handleSaveShowcase}
            className="bg-white rounded-3xl max-w-xl w-full max-h-[90vh] overflow-y-auto p-5 space-y-4 border border-[#EBECEF]"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-black text-base">
                {editingShowcase.id ? 'تعديل منتجات الشركة' : 'إضافة منتجات شركة'}
              </h3>
              <button type="button" onClick={() => setEditingShowcase(null)} className="text-xs font-bold text-[#717784]">
                إغلاق
              </button>
            </div>

            <div className="space-y-3.5 text-xs">
              {/* Company Selector for Auto-Populating Products */}
              <div className="rounded-2xl bg-[#F9FAFB] border border-[#EBECEF] p-3.5 space-y-2">
                <label className="flex items-center gap-1.5 font-black text-[#181A20]">
                  <Building2 className="w-4 h-4 text-[#1565C0]" />
                  <span>اختر الشركة (لجلب منتجاتها تلقائيًا)</span>
                </label>
                <select
                  value={editingShowcase.companyId || ''}
                  onChange={(e) => {
                    const cid = e.target.value;
                    const comp = cid ? companyMap.get(cid) : undefined;
                    setEditingShowcase({
                      ...editingShowcase,
                      companyId: cid || undefined,
                      title: comp ? comp.name : editingShowcase.title,
                      companyImageUrl: comp?.imageUrl || '',
                    });
                  }}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] bg-white font-bold"
                >
                  <option value="">-- اختر شركة من الإعدادات --</option>
                  {companies
                    .filter((c) => c.isActive || c.id === editingShowcase.companyId)
                    .map((comp) => (
                      <option key={comp.id} value={comp.id}>
                        {comp.name}
                      </option>
                    ))}
                </select>
                {editingShowcase.companyId ? (
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#E8F6EE] text-[#219653] font-black text-[11px]">
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>
                      سيتم عرض {formatNumber(editingMatchedCount)} منتج نشط تابع لهذه الشركة تلقائيًا
                    </span>
                  </div>
                ) : (
                  <p className="text-[11px] text-[#717784]">
                    أضف الشركات من «إعدادات المتجر ← إضافة شركات للمتجر»، واربط بها المنتجات لتظهر هنا تلقائيًا دون إعادة إضافتها.
                  </p>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold mb-1">اسم الشركة الظاهر *</label>
                  <input
                    value={editingShowcase.title || ''}
                    onChange={(e) => setEditingShowcase({ ...editingShowcase, title: e.target.value })}
                    placeholder="مثال: أوكسي - Oxi"
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                    required
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1">عنوان فرعي (اختياري)</label>
                  <input
                    value={editingShowcase.subtitle || ''}
                    onChange={(e) => setEditingShowcase({ ...editingShowcase, subtitle: e.target.value })}
                    placeholder="تشكيلة مميزة بأسعار خاصة"
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1">مكان الظهور في الرئيسية *</label>
                  <select
                    value={editingShowcase.placement || 'below_categories'}
                    onChange={(e) =>
                      setEditingShowcase({ ...editingShowcase, placement: e.target.value as BannerPlacement })
                    }
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  >
                    {Object.entries(BANNER_PLACEMENT_LABELS).map(([k, lbl]) => (
                      <option key={k} value={k}>
                        {lbl}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-bold mb-1">ترتيب الظهور (الأصغر أولًا)</label>
                  <input
                    type="number"
                    min="0"
                    max="1000"
                    value={editingShowcase.displayOrder ?? 0}
                    onChange={(e) =>
                      setEditingShowcase({ ...editingShowcase, displayOrder: Number(e.target.value) })
                    }
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  />
                </div>
              </div>

              {/* Optional Company-Wide Discount */}
              <div className="rounded-2xl bg-[#FFF9DB]/50 border border-[#FFD000]/60 p-3.5 space-y-2.5">
                <div className="flex items-center justify-between gap-2">
                  <label className="flex items-center gap-1.5 font-black text-[#181A20]">
                    <Percent className="w-4 h-4 text-[#E53935]" />
                    <span>خصم عام اختياري على كل منتجات الشركة (٪)</span>
                  </label>
                  {Number(editingShowcase.discountPercent || 0) > 0 && (
                    <span className="px-2 py-0.5 rounded-full bg-[#E53935] text-white text-[10px] font-black">
                      خصم {formatNumber(Number(editingShowcase.discountPercent || 0))}٪
                    </span>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <input
                    type="number"
                    min="0"
                    max="95"
                    step="0.5"
                    value={editingShowcase.discountPercent ?? 0}
                    onChange={(e) =>
                      setEditingShowcase({
                        ...editingShowcase,
                        discountPercent: Math.min(95, Math.max(0, Number(e.target.value) || 0)),
                      })
                    }
                    className="w-24 h-9 px-3 rounded-xl border border-[#EBECEF] bg-white font-black text-center"
                  />
                  <div className="flex flex-wrap gap-1.5">
                    {DISCOUNT_PRESETS.map((pct) => {
                      const active = Number(editingShowcase.discountPercent || 0) === pct;
                      return (
                        <button
                          key={pct}
                          type="button"
                          onClick={() => setEditingShowcase({ ...editingShowcase, discountPercent: pct })}
                          className={`px-2.5 py-1 rounded-lg text-[11px] font-bold border transition ${
                            active
                              ? 'bg-[#181A20] text-[#FFD000] border-[#181A20]'
                              : 'bg-white border-[#EBECEF] text-[#181A20] hover:border-[#FFD000]'
                          }`}
                        >
                          {pct === 0 ? 'بدون خصم' : `${formatNumber(pct)}٪`}
                        </button>
                      );
                    })}
                  </div>
                </div>
                <p className="text-[11px] text-[#717784]">
                  يُطبّق الخصم تلقائيًا على أسعار جميع منتجات الشركة في الرئيسية وصفحة الشركة والسلة وعند تأكيد الطلب (ويظهر كشارة عرض جذابة).
                </p>
              </div>

              {/* Optional Banner Upload */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between gap-2">
                  <label className="font-bold">صورة البنر الإعلاني للشركة (اختياري)</label>
                  {editingShowcase.bannerImageUrl && (
                    <button
                      type="button"
                      onClick={() => setEditingShowcase({ ...editingShowcase, bannerImageUrl: '' })}
                      className="text-[11px] font-bold text-[#E53935] hover:underline"
                    >
                      إزالة البنر (تصميم تلقائي)
                    </button>
                  )}
                </div>
                <ImageUploader
                  folder="aleman/banners"
                  urls={editingShowcase.bannerImageUrl ? [editingShowcase.bannerImageUrl] : []}
                  multiple={false}
                  label="رفع بنر اختياري من الاستيديو"
                  onChange={(urls) => setEditingShowcase({ ...editingShowcase, bannerImageUrl: urls[0] || '' })}
                />
                <p className="text-[11px] text-[#717784]">
                  بدون بنر: يظهر تصميم احترافي بشعار واسم الشركة (يتناوب تلقائيًا بين اليمين واليسار) فوق المنتجات. عند رفع بنر: يظهر كاملًا بنفس مقاس القص (٢٥٪ فارغ بالأعلى، ٦٥٪ منتجات، ١٠٪ زر عرض الكل).
                </p>
              </div>

              <div>
                <label className="block font-bold mb-1.5">لون الخلفية المميز لقسم الشركة:</label>
                <div className="flex flex-wrap gap-2">
                  {ACCENT_PRESETS.map((p) => {
                    const selected = (editingShowcase.accentColorHex || '#FCE4EC').toLowerCase() === p.hex.toLowerCase();
                    return (
                      <button
                        key={p.hex}
                        type="button"
                        onClick={() => setEditingShowcase({ ...editingShowcase, accentColorHex: p.hex })}
                        style={{ backgroundColor: p.hex }}
                        className={`px-3 py-1.5 rounded-xl text-[11px] font-bold border transition ${
                          selected ? 'border-[#181A20] ring-2 ring-[#181A20]/20' : 'border-[#EBECEF]'
                        }`}
                      >
                        {p.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              <label className="inline-flex items-center gap-2 font-bold">
                <input
                  type="checkbox"
                  checked={editingShowcase.isActive ?? true}
                  onChange={(e) => setEditingShowcase({ ...editingShowcase, isActive: e.target.checked })}
                />
                <span>إظهار القسم للعملاء فورًا</span>
              </label>
            </div>

            <button
              type="submit"
              disabled={saving}
              className="w-full py-3 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
            >
              {saving ? 'جاري الحفظ...' : editingShowcase.id ? 'حفظ التعديلات' : 'حفظ وإظهار منتجات الشركة'}
            </button>
          </form>
        </div>
      )}

      {/* Modal 2: Content Manager (Categories-First -> Products-Second) */}
      {managingShowcase && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-3xl w-full max-h-[90vh] overflow-y-auto p-5 space-y-5 border border-[#EBECEF]">
            <div className="flex items-center justify-between border-b border-[#EBECEF] pb-3">
              <div>
                <h3 className="font-black text-base">إدارة محتوى «{managingShowcase.title}»</h3>
                <p className="text-[11px] text-[#717784]">
                  ١) أضف فئات الشركة بصورها أولًا  •  ٢) أضف المنتجات وحدد فئتها وظهورها (عام أم حصري)
                </p>
              </div>
              <button
                type="button"
                onClick={() => setManagingShowcaseId(null)}
                className="px-3 py-1.5 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold"
              >
                إغلاق
              </button>
            </div>

            {/* Step 1: Company Categories */}
            <section className="bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-4 space-y-3">
              <div className="flex items-center justify-between gap-2">
                <h4 className="font-black text-sm">
                  ١. فئات الشركة ({formatNumber(managingShowcase.categories.length)})
                </h4>
                <button
                  type="button"
                  onClick={() =>
                    setEditingCat({
                      name: '',
                      imageUrl: '',
                      displayOrder: managingShowcase.categories.length,
                    })
                  }
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
                >
                  <FolderPlus className="w-3.5 h-3.5" />
                  <span>إضافة فئة</span>
                </button>
              </div>

              {managingShowcase.categories.length === 0 ? (
                <p className="text-xs font-bold text-[#E53935]">
                  لا توجد فئات لهذه الشركة بعد. أضف فئة واحدة مع صورة على الأقل قبل إضافة المنتجات.
                </p>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {managingShowcase.categories.map((cat) => (
                    <div
                      key={cat.id}
                      className="bg-white border border-[#EBECEF] rounded-xl p-2.5 flex items-center gap-2.5"
                    >
                      <img
                        src={cat.imageUrl}
                        alt={cat.name}
                        className="w-11 h-11 rounded-lg object-cover bg-[#F9FAFB] border border-[#EBECEF] shrink-0"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="font-bold text-xs truncate">{cat.name}</div>
                        <div className="text-[10px] text-[#717784]">الترتيب: {formatNumber(cat.displayOrder)}</div>
                      </div>
                      <button
                        type="button"
                        onClick={() => setEditingCat(cat)}
                        className="p-1.5 rounded-lg bg-[#FFF9DB] text-[#181A20]"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeleteCategory(cat.id)}
                        className="p-1.5 rounded-lg bg-[#FDECEC] text-[#E53935]"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </section>

            {/* Step 2: Company Products */}
            <section className="bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-4 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h4 className="font-black text-sm">
                  ٢. منتجات الشركة ({formatNumber(productsInShowcase.length)})
                </h4>

                {managingShowcase.categories.length > 0 && (
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() =>
                        setNewProd({
                          name: '',
                          description: '',
                          categoryId: mainCategories[0]?.id || '',
                          brandCategoryId: managingShowcase.categories[0]?.id || '',
                          exclusiveToBrandShowcase: false,
                          sku: `BRD-${Math.floor(10000 + Math.random() * 89999)}`,
                          imageUrls: [],
                          retailPrice: 0,
                          wholesalePrice: 0,
                          wholesaleMinQty: 6,
                          unit: '١ لتر',
                          stockQuantity: 50,
                          lowStockThreshold: 5,
                          isFeatured: false,
                          isOffer: false,
                          isActive: true,
                        })
                      }
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-black"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>منتج جديد للشركة</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setLinkSearch('');
                        setLinkSelectedProdId('');
                        setLinkBrandCatId(managingShowcase.categories[0]?.id || '');
                        setLinkExclusive(false);
                        setShowLinkModal(true);
                      }}
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-white border border-[#181A20] text-[#181A20] text-xs font-black"
                    >
                      <Link2 className="w-3.5 h-3.5" />
                      <span>ربط منتج من المتجر</span>
                    </button>
                  </div>
                )}
              </div>

              {managingShowcase.categories.length === 0 ? (
                <div className="p-3 rounded-xl bg-[#FDECEC] text-[#E53935] text-xs font-bold flex items-center gap-2">
                  <TriangleAlert className="w-4 h-4 shrink-0" />
                  <span>يجب إضافة فئة واحدة على الأقل داخل الشركة بالصور أولًا قبل إضافة أو ربط أي منتج.</span>
                </div>
              ) : productsInShowcase.length === 0 ? (
                <p className="text-xs text-[#717784]">لا توجد منتجات مضافة لهذا العرض حتى الآن.</p>
              ) : (
                <div className="space-y-2">
                  {productsInShowcase.map((prod) => {
                    const catName =
                      managingShowcase.categories.find((c) => c.id === prod.brandCategoryId)?.name ||
                      'بدون فئة محددة';
                    return (
                      <div
                        key={prod.id}
                        className="bg-white border border-[#EBECEF] rounded-xl p-3 flex flex-wrap items-center justify-between gap-2"
                      >
                        <div className="flex items-center gap-2.5 min-w-0 flex-1">
                          <img
                            src={prod.imageUrls[0] || ''}
                            alt=""
                            className="w-11 h-11 rounded-lg object-contain bg-[#F9FAFB] border border-[#EBECEF] shrink-0"
                          />
                          <div className="min-w-0">
                            <div className="font-bold text-xs truncate">{prod.name}</div>
                            <div className="text-[11px] text-[#717784]">
                              {catName} • {formatCurrency(getEffectiveRetailPrice(prod))}
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() =>
                              saveProductDoc({
                                ...prod,
                                exclusiveToBrandShowcase: !prod.exclusiveToBrandShowcase,
                              })
                            }
                            className={`px-2.5 py-1 rounded-full text-[11px] font-black inline-flex items-center gap-1 ${
                              prod.exclusiveToBrandShowcase
                                ? 'bg-[#FDECEC] text-[#E53935]'
                                : 'bg-[#EBF4FF] text-[#1565C0]'
                            }`}
                          >
                            {prod.exclusiveToBrandShowcase ? (
                              <>
                                <Lock className="w-3 h-3" />
                                <span>حصري بالشركة فقط</span>
                              </>
                            ) : (
                              <>
                                <Globe className="w-3 h-3" />
                                <span>عام (المتجر + الشركة)</span>
                              </>
                            )}
                          </button>

                          <button
                            type="button"
                            title="فك الارتباط"
                            onClick={() =>
                              saveProductDoc({
                                ...prod,
                                brandShowcaseId: undefined,
                                brandCategoryId: undefined,
                                exclusiveToBrandShowcase: false,
                              })
                            }
                            className="p-1.5 rounded-lg bg-[#FDECEC] text-[#E53935]"
                          >
                            <Link2Off className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          </div>
        </div>
      )}

      {/* Submodal: Category Add/Edit */}
      {editingCat && managingShowcase && (
        <div className="fixed inset-0 z-[60] bg-black/60 flex items-center justify-center p-4">
          <form
            onSubmit={handleSaveCategory}
            className="bg-white rounded-3xl max-w-md w-full p-5 space-y-4 border border-[#EBECEF]"
          >
            <div className="flex items-center justify-between">
              <h4 className="font-black text-sm">{editingCat.id ? 'تعديل فئة الشركة' : 'إضافة فئة للشركة'}</h4>
              <button type="button" onClick={() => setEditingCat(null)} className="text-xs font-bold text-[#717784]">
                إلغاء
              </button>
            </div>
            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold mb-1">صورة الفئة *</label>
                <ImageUploader
                  folder="aleman/categories"
                  urls={editingCat.imageUrl ? [editingCat.imageUrl] : []}
                  multiple={false}
                  label="رفع صورة الفئة"
                  onChange={(urls) => setEditingCat({ ...editingCat, imageUrl: urls[0] || '' })}
                />
              </div>
              <div>
                <label className="block font-bold mb-1">اسم الفئة *</label>
                <input
                  value={editingCat.name || ''}
                  onChange={(e) => setEditingCat({ ...editingCat, name: e.target.value })}
                  placeholder="مثال: منعم أقمشة، مسحوق غسيل..."
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  required
                />
              </div>
              <div>
                <label className="block font-bold mb-1">الترتيب</label>
                <input
                  type="number"
                  min="0"
                  value={editingCat.displayOrder ?? 0}
                  onChange={(e) => setEditingCat({ ...editingCat, displayOrder: Number(e.target.value) })}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={saving}
              className="w-full py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
            >
              {saving ? 'جاري الحفظ...' : 'حفظ الفئة'}
            </button>
          </form>
        </div>
      )}

      {/* Submodal: New Product for Brand Showcase */}
      {newProd && managingShowcase && (
        <div className="fixed inset-0 z-[60] bg-black/60 flex items-center justify-center p-4">
          <form
            onSubmit={handleSaveNewProduct}
            className="bg-white rounded-3xl max-w-xl w-full max-h-[90vh] overflow-y-auto p-5 space-y-4 border border-[#EBECEF]"
          >
            <div className="flex items-center justify-between">
              <h4 className="font-black text-sm">إضافة منتج داخل «{managingShowcase.title}»</h4>
              <button type="button" onClick={() => setNewProd(null)} className="text-xs font-bold text-[#717784]">
                إلغاء
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold mb-1">صور المنتج</label>
                <ImageUploader
                  folder="aleman/products"
                  multiple
                  urls={newProd.imageUrls || []}
                  label="رفع صور المنتج"
                  onChange={(urls) => setNewProd({ ...newProd, imageUrls: urls })}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold mb-1">فئة المنتج داخل الشركة *</label>
                  <select
                    value={newProd.brandCategoryId || ''}
                    onChange={(e) => setNewProd({ ...newProd, brandCategoryId: e.target.value })}
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                    required
                  >
                    {managingShowcase.categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-bold mb-1">القسم الرئيسي في المتجر *</label>
                  <select
                    value={newProd.categoryId || ''}
                    onChange={(e) => setNewProd({ ...newProd, categoryId: e.target.value })}
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                    required
                  >
                    {mainCategories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Explicit Visibility Radio */}
              <div className="bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-3 space-y-2">
                <div className="font-black text-[#181A20]">أين يظهر هذا المنتج؟ *</div>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="new_brand_visibility"
                    checked={!newProd.exclusiveToBrandShowcase}
                    onChange={() => setNewProd({ ...newProd, exclusiveToBrandShowcase: false })}
                  />
                  <span>عام: يظهر في المتجر الرئيسي وعرض الشركة معًا</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="new_brand_visibility"
                    checked={Boolean(newProd.exclusiveToBrandShowcase)}
                    onChange={() => setNewProd({ ...newProd, exclusiveToBrandShowcase: true })}
                  />
                  <span>حصري: يظهر داخل عرض الشركة فقط (لا يظهر في الرئيسية أو الفئات العامة)</span>
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold mb-1">اسم المنتج *</label>
                  <input
                    value={newProd.name || ''}
                    onChange={(e) => setNewProd({ ...newProd, name: e.target.value })}
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                    required
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1">كود SKU *</label>
                  <input
                    value={newProd.sku || ''}
                    onChange={(e) => setNewProd({ ...newProd, sku: e.target.value })}
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                    required
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1">سعر القطاعي *</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newProd.retailPrice ?? 0}
                    onChange={(e) => setNewProd({ ...newProd, retailPrice: Number(e.target.value) })}
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                    required
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1">سعر بعد الخصم</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newProd.retailDiscountPrice ?? ''}
                    onChange={(e) =>
                      setNewProd({
                        ...newProd,
                        retailDiscountPrice: e.target.value ? Number(e.target.value) : undefined,
                      })
                    }
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1">الحجم / الوحدة</label>
                  <input
                    value={newProd.unit || '١ لتر'}
                    onChange={(e) => setNewProd({ ...newProd, unit: e.target.value })}
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1">المخزون</label>
                  <input
                    type="number"
                    min="0"
                    value={newProd.stockQuantity ?? 50}
                    onChange={(e) => setNewProd({ ...newProd, stockQuantity: Number(e.target.value) })}
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  />
                </div>
              </div>
            </div>

            <button
              type="submit"
              disabled={saving}
              className="w-full py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
            >
              {saving ? 'جاري الحفظ...' : 'حفظ المنتج'}
            </button>
          </form>
        </div>
      )}

      {/* Submodal: Link Existing Product */}
      {showLinkModal && managingShowcase && (
        <div className="fixed inset-0 z-[60] bg-black/60 flex items-center justify-center p-4">
          <form
            onSubmit={handleLinkExisting}
            className="bg-white rounded-3xl max-w-lg w-full max-h-[85vh] overflow-y-auto p-5 space-y-4 border border-[#EBECEF]"
          >
            <div className="flex items-center justify-between">
              <h4 className="font-black text-sm">ربط منتج حالي بالشركة</h4>
              <button type="button" onClick={() => setShowLinkModal(false)} className="text-xs font-bold text-[#717784]">
                إلغاء
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold mb-1">فئة الشركة *</label>
                <select
                  value={linkBrandCatId}
                  onChange={(e) => setLinkBrandCatId(e.target.value)}
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  required
                >
                  {managingShowcase.categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-3 space-y-1.5">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="link_visibility"
                    checked={!linkExclusive}
                    onChange={() => setLinkExclusive(false)}
                  />
                  <span>عام: يظهر في المتجر الرئيسي وعرض الشركة</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="link_visibility"
                    checked={linkExclusive}
                    onChange={() => setLinkExclusive(true)}
                  />
                  <span>حصري: يظهر داخل عرض الشركة فقط</span>
                </label>
              </div>

              <input
                type="search"
                value={linkSearch}
                onChange={(e) => setLinkSearch(e.target.value)}
                placeholder="ابحث عن المنتج بالاسم أو SKU..."
                className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
              />

              <div className="max-h-56 overflow-y-auto space-y-1.5">
                {linkableProducts.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => setLinkSelectedProdId(p.id)}
                    className={`w-full p-2.5 rounded-xl border flex items-center gap-2.5 text-right ${
                      linkSelectedProdId === p.id
                        ? 'bg-[#FFF9DB] border-[#FFD000]'
                        : 'bg-white border-[#EBECEF]'
                    }`}
                  >
                    <img
                      src={p.imageUrls[0] || ''}
                      alt=""
                      className="w-9 h-9 rounded-lg object-contain bg-[#F9FAFB] shrink-0"
                    />
                    <div className="min-w-0 flex-1">
                      <div className="font-bold truncate">{p.name}</div>
                      <div className="text-[10px] text-[#717784]">{formatCurrency(getEffectiveRetailPrice(p))}</div>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <button
              type="submit"
              disabled={!linkSelectedProdId || !linkBrandCatId || saving}
              className="w-full py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm disabled:opacity-50"
            >
              {saving ? 'جاري الربط...' : 'ربط المنتج بالشركة'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
};
