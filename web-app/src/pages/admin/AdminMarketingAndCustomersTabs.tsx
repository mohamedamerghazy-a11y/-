import React, { useState } from 'react';
import { Bolt, Check, Image as ImageIcon, PlayCircle, Plus, Shield, Sparkles, Trash2, UserCheck, Video, Volume2, VolumeX, X } from 'lucide-react';
import {
  BANNER_PLACEMENT_LABELS,
  BOOTSTRAPPED_OWNER_EMAIL,
  STAFF_PERMISSION_LABELS,
  STAFF_ROLE_META,
  type Banner,
  type BannerMediaType,
  type BannerPlacement,
  type BannerStyle,
  type BannerTargetType,
  type Category,
  type FlashSale,
  type FlashSaleProductItem,
  type Product,
  type StaffMember,
  type StaffPermission,
  type StaffRole,
  type UserProfile,
} from '../../types/models';
import { useStore } from '../../context/StoreContext';
import {
  assignStaffMemberDoc,
  deleteBannerDoc,
  deleteFlashSaleDoc,
  removeStaffMemberDoc,
  saveBannerDoc,
  saveFlashSaleDoc,
  sendNotificationToUser,
  toggleFlashSaleActive,
  updateWholesaleDecision,
} from '../../services/firestoreService';
import {
  formatDateTime,
  formatNumber,
  getFlashSaleStatus,
  toArabicErrorMessage,
  toMillis,
} from '../../lib/formatters';
import { ImageUploader } from '../../components/common/ImageUploader';
import { AnimatedImageUploader } from '../../components/common/AnimatedImageUploader';
import { VideoUploader } from '../../components/common/VideoUploader';
import { deriveVideoPosterUrl } from '../../lib/cloudinary';

function toDatetimeLocal(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// 1. Flash Sales Scheduler (Individual product selection with isolated flash pricing)
export const AdminFlashSalesTab: React.FC<{
  flashSales: FlashSale[];
  products: Product[];
}> = ({ flashSales, products }) => {
  const { showToast } = useStore();
  const [editingId, setEditingId] = useState<string | undefined>();
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState('');
  const [badgeText, setBadgeText] = useState('عروض خاطفة ⚡');
  const [startStr, setStartStr] = useState(() => toDatetimeLocal(new Date()));
  const [endStr, setEndStr] = useState(() => toDatetimeLocal(new Date(Date.now() + 6 * 3600 * 1000)));
  const [isActive, setIsActive] = useState(true);
  const [items, setItems] = useState<FlashSaleProductItem[]>([]);
  const [pickerProductId, setPickerProductId] = useState('');

  const productMap = new Map(products.map((p) => [p.id, p]));

  const openNew = () => {
    setEditingId(undefined);
    setTitle('');
    setBadgeText('عرض خاطف');
    setStartStr(toDatetimeLocal(new Date()));
    setEndStr(toDatetimeLocal(new Date(Date.now() + 6 * 3600 * 1000)));
    setIsActive(true);
    setItems([]);
    setPickerProductId(products[0]?.id || '');
    setShowForm(true);
  };

  const openEdit = (sale: FlashSale) => {
    setEditingId(sale.id);
    setTitle(sale.title);
    setBadgeText(sale.badgeText);
    setStartStr(toDatetimeLocal(new Date(toMillis(sale.startsAt) || Date.now())));
    setEndStr(toDatetimeLocal(new Date(toMillis(sale.endsAt) || Date.now() + 3600 * 1000)));
    setIsActive(sale.isActive);
    setItems([...sale.items]);
    setPickerProductId(products[0]?.id || '');
    setShowForm(true);
  };

  const addProductItem = () => {
    const prod = productMap.get(pickerProductId);
    if (!prod || items.some((i) => i.productId === prod.id)) return;
    setItems([
      ...items,
      {
        productId: prod.id,
        flashRetailPrice: Math.max(1, Math.round(prod.retailPrice * 0.85)),
        flashWholesalePrice: prod.wholesalePrice > 0 ? Math.round(prod.wholesalePrice * 0.9) : undefined,
        maxQtyPerOrder: undefined,
      },
    ]);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await saveFlashSaleDoc({
        id: editingId,
        title,
        badgeText,
        startsAtDate: new Date(startStr),
        endsAtDate: new Date(endStr),
        isActive,
        items,
      });
      showToast('تم حفظ حملة الفلاش سيل بنجاح', 'success');
      setShowForm(false);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ الفلاش سيل.'), 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button
          type="button"
          onClick={openNew}
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
        >
          <Plus className="w-4 h-4" />
          <span>إنشاء حملة فلاش سيل</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {flashSales.map((s) => {
          const st = getFlashSaleStatus(s);
          return (
            <div key={s.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 space-y-2.5">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <div className="font-black text-sm flex items-center gap-1.5">
                    <Bolt className="w-4 h-4 text-[#FFD000] fill-current" />
                    <span>{s.title}</span>
                  </div>
                  <div className="text-[11px] text-[#717784] mt-0.5">
                    يبدأ: {formatDateTime(s.startsAt)} | ينتهي: {formatDateTime(s.endsAt)}
                  </div>
                </div>
                <span className="px-2.5 py-0.5 rounded-full bg-[#FFF9DB] text-xs font-bold">
                  {st === 'upcoming' ? 'مجدول (قريبًا)' : st === 'active' ? 'نشط الآن' : 'منتهي'}
                </span>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#EBECEF] text-xs">
                <span className="font-bold text-[#717784]">{formatNumber(s.items.length)} منتجات</span>
                <div className="flex gap-1.5">
                  <button
                    type="button"
                    onClick={() => toggleFlashSaleActive(s.id, !s.isActive)}
                    className={`px-3 py-1.5 rounded-lg font-bold ${
                      s.isActive ? 'bg-[#E8F6EE] text-[#219653]' : 'bg-gray-200 text-gray-700'
                    }`}
                  >
                    {s.isActive ? 'مفعل' : 'متوقف'}
                  </button>
                  <button type="button" onClick={() => openEdit(s)} className="px-3 py-1.5 rounded-lg bg-[#FFF9DB] font-bold">
                    تعديل
                  </button>
                  <button type="button" onClick={() => deleteFlashSaleDoc(s.id)} className="p-1.5 rounded-lg bg-[#FDECEC] text-[#E53935]">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {showForm && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleSave} className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-black text-base">{editingId ? 'تعديل حملة الفلاش سيل' : 'حملة فلاش سيل جديدة'}</h3>
              <button type="button" onClick={() => setShowForm(false)} className="text-xs font-bold text-[#717784]">إغلاق</button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="عنوان الحملة *" className="h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              <input value={badgeText} onChange={(e) => setBadgeText(e.target.value)} placeholder="نص الشارة" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <div>
                <label className="block font-bold mb-1">وقت البداية</label>
                <input type="datetime-local" value={startStr} onChange={(e) => setStartStr(e.target.value)} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              </div>
              <div>
                <label className="block font-bold mb-1">وقت الانتهاء</label>
                <input type="datetime-local" value={endStr} onChange={(e) => setEndStr(e.target.value)} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              </div>
            </div>

            {/* Individual product picker */}
            <div className="bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-3 space-y-2.5">
              <div className="text-xs font-black">إضافة منتجات فردية للحملة (بأسعار معزولة عن المتجر)</div>
              <div className="flex gap-2">
                <select value={pickerProductId} onChange={(e) => setPickerProductId(e.target.value)} className="flex-1 h-10 px-3 rounded-xl border border-[#EBECEF] text-xs bg-white">
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>{p.name} — ({p.retailPrice} ج.م)</option>
                  ))}
                </select>
                <button type="button" onClick={addProductItem} className="px-4 h-10 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-bold">
                  إضافة المنتج
                </button>
              </div>

              {items.map((item, idx) => {
                const p = productMap.get(item.productId);
                return (
                  <div key={item.productId} className="bg-white border border-[#EBECEF] rounded-xl p-2.5 flex flex-wrap items-center justify-between gap-2 text-xs">
                    <span className="font-bold min-w-[140px]">{p?.name || item.productId}</span>
                    <div className="flex flex-wrap items-center gap-2">
                      <label className="flex items-center gap-1">
                        <span>سعر العرض:</span>
                        <input
                          type="number"
                          step="0.01"
                          value={item.flashRetailPrice}
                          onChange={(e) => {
                            const next = [...items];
                            next[idx] = { ...item, flashRetailPrice: Number(e.target.value) };
                            setItems(next);
                          }}
                          className="w-20 h-8 px-2 rounded-lg border border-[#EBECEF]"
                        />
                      </label>
                      <label className="flex items-center gap-1">
                        <span>سعر جملة:</span>
                        <input
                          type="number"
                          step="0.01"
                          value={item.flashWholesalePrice ?? ''}
                          onChange={(e) => {
                            const next = [...items];
                            next[idx] = {
                              ...item,
                              flashWholesalePrice: e.target.value ? Number(e.target.value) : undefined,
                            };
                            setItems(next);
                          }}
                          className="w-20 h-8 px-2 rounded-lg border border-[#EBECEF]"
                        />
                      </label>
                      <button type="button" onClick={() => setItems(items.filter((_, i) => i !== idx))} className="text-[#E53935]">
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            <label className="flex items-center gap-2 text-xs font-bold">
              <input type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)} />
              <span>الحملة مفعّلة</span>
            </label>

            <button type="submit" className="w-full py-3 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm">
              حفظ الحملة
            </button>
          </form>
        </div>
      )}
    </div>
  );
};

// 2. Banners Manager
export const AdminBannersTab: React.FC<{
  banners: Banner[];
  categories: Category[];
  products: Product[];
}> = ({ banners, categories, products }) => {
  const { showToast } = useStore();
  const [editing, setEditing] = useState<Partial<Banner> | null>(null);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    const isVideo = editing?.mediaType === 'video';
    const isAnimated = editing?.mediaType === 'animated_image';
    if (isVideo) {
      if (!editing?.videoUrl?.trim() || editing.videoUrl.trim().length < 5) {
        showToast('يرجى رفع الفيديو الدعائي أو إدخال رابط فيديو صالح.', 'error');
        return;
      }
    } else if (!editing?.imageUrl?.trim()) {
      showToast(isAnimated ? 'يرجى رفع الصورة المتحركة (GIF / WebP) أولًا.' : 'صورة البنر مطلوبة.', 'error');
      return;
    }
    try {
      await saveBannerDoc(editing);
      showToast('تم حفظ البنر بنجاح', 'success');
      setEditing(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ البنر.'), 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button
          type="button"
          onClick={() =>
            setEditing({
              title: '',
              subtitle: '',
              mediaType: 'image',
              imageUrl: '',
              videoUrl: '',
              videoMuted: false,
              targetType: 'none',
              placement: 'top_home',
              bannerStyle: 'normal',
              durationSeconds: 5,
              displayOrder: 0,
              isActive: true,
            })
          }
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
        >
          <Plus className="w-4 h-4" />
          <span>إضافة بنر إعلاني / فيديو</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {banners.map((b) => {
          const isVideo = b.mediaType === 'video' && (b.videoUrl || '').trim().length >= 5;
          const isAnimated = b.mediaType === 'animated_image' && (b.imageUrl || '').trim().length >= 5;
          const previewImg = b.imageUrl?.trim() || deriveVideoPosterUrl(b.videoUrl || '');
          return (
            <div key={b.id} className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
              <div className="relative bg-[#101218]">
                {previewImg ? (
                  <img src={previewImg} alt={b.title} className="w-full h-auto max-h-52 object-cover block bg-[#F9FAFB]" />
                ) : (
                  <div className="w-full h-36 flex items-center justify-center bg-[#181A20] text-[#FFD000]">
                    <PlayCircle className="w-10 h-10" />
                  </div>
                )}
                {isVideo && (
                  <span className="absolute top-2.5 right-2.5 inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#FFD000] text-[#181A20] text-[11px] font-black shadow">
                    <Video className="w-3.5 h-3.5" />
                    <span>فيديو دعائي</span>
                  </span>
                )}
                {isAnimated && (
                  <span className="absolute top-2.5 right-2.5 inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#FFD000] text-[#181A20] text-[11px] font-black shadow">
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>صورة متحركة</span>
                  </span>
                )}
              </div>
              <div className="p-3.5 flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <div className="font-bold text-sm truncate">
                    {b.title || (isVideo ? 'بنر فيديو دعائي قصير' : isAnimated ? 'بنر صورة متحركة (GIF / WebP)' : 'بنر بدون عنوان')}
                  </div>
                  <div className="text-[11px] text-[#717784]">
                    {BANNER_PLACEMENT_LABELS[b.placement] || b.placement} • {b.bannerStyle === 'slim' ? 'رفيع' : 'عادي'} • {formatNumber(b.durationSeconds)}ث
                    {isVideo ? (b.videoMuted ? ' • كاتم' : ' • بصوت') : ''}
                  </div>
                </div>
                <div className="flex gap-1.5 shrink-0">
                  <button type="button" onClick={() => setEditing({ ...b })} className="px-3 py-1.5 rounded-lg bg-[#FFF9DB] text-xs font-bold">
                    تعديل
                  </button>
                  <button type="button" onClick={() => deleteBannerDoc(b.id)} className="p-2 rounded-lg bg-[#FDECEC] text-[#E53935]">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {editing && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleSave} className="bg-white rounded-3xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-5 space-y-3 text-xs">
            <h3 className="font-black text-base">{editing.id ? 'تعديل البنر' : 'بنر إعلاني جديد'}</h3>

            {/* نوع البنر: صورة ثابتة، صورة متحركة، أو فيديو دعائي قصير */}
            <div className="space-y-1.5">
              <div className="font-bold text-[#181A20]">نوع محتوى البنر</div>
              <div className="grid grid-cols-3 gap-2">
                {(
                  [
                    { key: 'image', label: 'صورة ثابتة', icon: ImageIcon },
                    { key: 'animated_image', label: 'صورة متحركة', icon: Sparkles },
                    { key: 'video', label: 'فيديو دعائي', icon: Video },
                  ] as Array<{ key: BannerMediaType; label: string; icon: React.ComponentType<{ className?: string }> }>
                ).map((opt) => {
                  const Icon = opt.icon;
                  const active = (editing.mediaType || 'image') === opt.key;
                  return (
                    <button
                      key={opt.key}
                      type="button"
                      onClick={() =>
                        setEditing({
                          ...editing,
                          mediaType: opt.key,
                          durationSeconds:
                            opt.key === 'video' && (!editing.durationSeconds || editing.durationSeconds <= 5)
                              ? 15
                              : editing.durationSeconds,
                        })
                      }
                      className={`h-10 px-2 rounded-xl border font-black inline-flex items-center justify-center gap-1 transition ${
                        active
                          ? 'bg-[#FFD000] border-[#181A20] text-[#181A20]'
                          : 'bg-[#F9FAFB] border-[#EBECEF] text-[#717784] hover:text-[#181A20]'
                      }`}
                    >
                      <Icon className="w-3.5 h-3.5 shrink-0" />
                      <span className="truncate">{opt.label}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {editing.mediaType === 'video' ? (
              <div className="space-y-2.5">
                <VideoUploader
                  folder="aleman/videos"
                  videoUrl={editing.videoUrl || ''}
                  durationSeconds={editing.durationSeconds || 0}
                  label="رفع فيديو دعائي قصير"
                  onChange={({ videoUrl, durationSeconds, posterUrl }) =>
                    setEditing((prev) =>
                      prev
                        ? {
                            ...prev,
                            videoUrl,
                            durationSeconds:
                              durationSeconds && durationSeconds > 0 ? durationSeconds : prev.durationSeconds,
                            imageUrl: prev.imageUrl?.trim() ? prev.imageUrl : posterUrl || '',
                          }
                        : prev,
                    )
                  }
                />
                <ImageUploader
                  folder="aleman/banners"
                  urls={editing.imageUrl ? [editing.imageUrl] : []}
                  label="صورة غلاف مخصصة للفيديو (اختياري)"
                  onChange={(urls) => setEditing({ ...editing, imageUrl: urls[0] || '' })}
                />

                {/* اختيار صوت الفيديو: بصوت أو كاتم بدون صوت */}
                <div className="rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-3 space-y-2">
                  <div className="font-bold text-[#181A20]">إعداد صوت الفيديو</div>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => setEditing({ ...editing, videoMuted: false })}
                      className={`h-9 px-3 rounded-xl border font-black inline-flex items-center justify-center gap-1.5 transition ${
                        !editing.videoMuted
                          ? 'bg-[#181A20] border-[#181A20] text-[#FFD000]'
                          : 'bg-white border-[#EBECEF] text-[#717784] hover:text-[#181A20]'
                      }`}
                    >
                      <Volume2 className="w-3.5 h-3.5" />
                      <span>بصوت</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => setEditing({ ...editing, videoMuted: true })}
                      className={`h-9 px-3 rounded-xl border font-black inline-flex items-center justify-center gap-1.5 transition ${
                        Boolean(editing.videoMuted)
                          ? 'bg-[#181A20] border-[#181A20] text-[#FFD000]'
                          : 'bg-white border-[#EBECEF] text-[#717784] hover:text-[#181A20]'
                      }`}
                    >
                      <VolumeX className="w-3.5 h-3.5" />
                      <span>كاتم (بدون صوت)</span>
                    </button>
                  </div>
                  <p className="text-[11px] text-[#717784]">
                    {editing.videoMuted
                      ? 'سيعمل الفيديو صامتًا تمامًا ولن يظهر زر تفعيل الصوت أعلى الفيديو.'
                      : 'سيعمل الفيديو بالصوت مع إمكانية التحكم في الكتم من أعلى الفيديو.'}
                  </p>
                </div>
              </div>
            ) : editing.mediaType === 'animated_image' ? (
              <AnimatedImageUploader
                folder="aleman/banners"
                imageUrl={editing.imageUrl || ''}
                isSlimPreview={editing.bannerStyle === 'slim'}
                label="رفع صورة متحركة (GIF / WebP)"
                onChange={(url) => setEditing({ ...editing, imageUrl: url })}
              />
            ) : (
              <ImageUploader
                folder="aleman/banners"
                urls={editing.imageUrl ? [editing.imageUrl] : []}
                label="رفع صورة البنر"
                onChange={(urls) => setEditing({ ...editing, imageUrl: urls[0] || '' })}
              />
            )}

            <input value={editing.title || ''} onChange={(e) => setEditing({ ...editing, title: e.target.value })} placeholder="عنوان البنر (اختياري)" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" />
            <input value={editing.subtitle || ''} onChange={(e) => setEditing({ ...editing, subtitle: e.target.value })} placeholder="وصف قصير (اختياري)" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" />
            <div className="grid grid-cols-2 gap-2">
              <select value={editing.placement || 'top_home'} onChange={(e) => setEditing({ ...editing, placement: e.target.value as BannerPlacement })} className="h-10 px-3 rounded-xl border border-[#EBECEF]">
                {(Object.keys(BANNER_PLACEMENT_LABELS) as BannerPlacement[]).map((k) => (
                  <option key={k} value={k}>{BANNER_PLACEMENT_LABELS[k]}</option>
                ))}
              </select>
              <select value={editing.bannerStyle || 'normal'} onChange={(e) => setEditing({ ...editing, bannerStyle: e.target.value as BannerStyle })} className="h-10 px-3 rounded-xl border border-[#EBECEF]">
                <option value="normal">بنر عادي (كبير)</option>
                <option value="slim">بنر إعلاني رفيع</option>
              </select>
              <select value={editing.targetType || 'none'} onChange={(e) => setEditing({ ...editing, targetType: e.target.value as BannerTargetType, targetId: '' })} className="h-10 px-3 rounded-xl border border-[#EBECEF]">
                <option value="none">عرض إعلاني فقط</option>
                <option value="offers">قسم العروض</option>
                <option value="category">قسم محدد</option>
                <option value="product">منتج محدد</option>
              </select>
              <input type="number" min="1" max="120" value={editing.durationSeconds ?? 5} onChange={(e) => setEditing({ ...editing, durationSeconds: Number(e.target.value) })} placeholder="مدة العرض (ثواني)" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
            </div>

            {editing.targetType === 'category' && (
              <select value={editing.targetId || ''} onChange={(e) => setEditing({ ...editing, targetId: e.target.value })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]">
                <option value="">اختر القسم...</option>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            )}
            {editing.targetType === 'product' && (
              <select value={editing.targetId || ''} onChange={(e) => setEditing({ ...editing, targetId: e.target.value })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]">
                <option value="">اختر المنتج...</option>
                {products.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
            )}

            <label className="flex items-center gap-2 font-bold">
              <input type="checkbox" checked={editing.isActive ?? true} onChange={(e) => setEditing({ ...editing, isActive: e.target.checked })} />
              <span>البنر نشط</span>
            </label>

            <div className="flex gap-2">
              <button type="submit" className="flex-1 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm">حفظ</button>
              <button type="button" onClick={() => setEditing(null)} className="px-4 py-2.5 rounded-xl border border-[#EBECEF] font-bold">إلغاء</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

// 3. Customers & Wholesale B2B Approvals
export const AdminCustomersTab: React.FC<{
  users: UserProfile[];
  canApproveWholesale: boolean;
}> = ({ users, canApproveWholesale }) => {
  const { showToast } = useStore();
  const [filter, setFilter] = useState<'all' | 'pending_wholesale' | 'approved_wholesale'>('all');

  const filtered = users.filter((u) => {
    if (filter === 'pending_wholesale') return u.wholesaleStatus === 'pending';
    if (filter === 'approved_wholesale') return u.wholesaleStatus === 'approved';
    return true;
  });

  const decideWholesale = async (u: UserProfile, approved: boolean) => {
    try {
      await updateWholesaleDecision(u.uid, approved);
      await sendNotificationToUser({
        targetUid: u.uid,
        title: approved ? 'تمت الموافقة على حساب الجملة' : 'تحديث بخصوص طلب حساب الجملة',
        message: approved
          ? 'تم تفعيل أسعار الجملة لحسابك، يمكنك الآن الاستفادة من عروض التجار.'
          : 'لم يتم قبول طلب حساب الجملة حاليًا — يمكنك التواصل مع خدمة العملاء.',
        type: 'wholesale_status',
      }).catch(() => {});
      showToast(approved ? 'تم اعتماد حساب الجملة' : 'تم رفض الطلب', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر تحديث حالة الحساب.'), 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2 text-xs font-bold">
        <button type="button" onClick={() => setFilter('all')} className={`px-3.5 py-2 rounded-xl border ${filter === 'all' ? 'bg-[#181A20] text-[#FFD000]' : 'bg-white border-[#EBECEF]'}`}>
          كل العملاء ({formatNumber(users.length)})
        </button>
        <button type="button" onClick={() => setFilter('pending_wholesale')} className={`px-3.5 py-2 rounded-xl border ${filter === 'pending_wholesale' ? 'bg-[#FFD000] text-[#181A20]' : 'bg-white border-[#EBECEF]'}`}>
          طلبات جملة معلقة ({formatNumber(users.filter((u) => u.wholesaleStatus === 'pending').length)})
        </button>
        <button type="button" onClick={() => setFilter('approved_wholesale')} className={`px-3.5 py-2 rounded-xl border ${filter === 'approved_wholesale' ? 'bg-[#219653] text-white' : 'bg-white border-[#EBECEF]'}`}>
          حسابات الجملة ({formatNumber(users.filter((u) => u.wholesaleStatus === 'approved').length)})
        </button>
      </div>

      <div className="space-y-2.5">
        {filtered.map((u) => (
          <div key={u.uid} className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="font-bold text-sm flex items-center gap-2">
                <UserCheck className="w-4 h-4 text-[#181A20]" />
                <span>{u.fullName}</span>
                {u.wholesaleStatus === 'approved' && (
                  <span className="px-2 py-0.5 rounded-full bg-[#E8F6EE] text-[#219653] text-[10px] font-black">تاجر جملة</span>
                )}
                {u.wholesaleStatus === 'pending' && (
                  <span className="px-2 py-0.5 rounded-full bg-[#FFF9DB] text-[#181A20] text-[10px] font-black">طلب جملة معلق</span>
                )}
              </div>
              <div className="text-xs text-[#717784] mt-0.5">
                {u.email} {u.phone ? `• ${u.phone}` : ''} {u.wholesaleCompanyName ? `• ${u.wholesaleCompanyName}` : ''}
              </div>
            </div>

            {canApproveWholesale && (
              <div className="flex gap-2">
                {u.wholesaleStatus !== 'approved' && (
                  <button type="button" onClick={() => decideWholesale(u, true)} className="px-3.5 py-1.5 rounded-xl bg-[#219653] text-white text-xs font-bold">
                    اعتماد جملة
                  </button>
                )}
                {u.wholesaleStatus === 'pending' || u.wholesaleStatus === 'approved' ? (
                  <button type="button" onClick={() => decideWholesale(u, false)} className="px-3.5 py-1.5 rounded-xl bg-[#FDECEC] text-[#E53935] text-xs font-bold">
                    رفض / إلغاء الجملة
                  </button>
                ) : null}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// 4. Staff & RBAC Permissions Manager
export const AdminStaffTab: React.FC<{
  users: UserProfile[];
  staffList: StaffMember[];
}> = ({ users, staffList }) => {
  const { isBootstrappedOwner, storeSettings, showToast } = useStore();
  const [selectedUser, setSelectedUser] = useState<UserProfile | null>(null);
  const [role, setRole] = useState<StaffRole>('order_manager');
  const [perms, setPerms] = useState<StaffPermission[]>(STAFF_ROLE_META.order_manager.permissions);
  const [isActive, setIsActive] = useState(true);

  const staffMap = new Map(staffList.map((s) => [s.uid, s]));

  const isTargetOwner = (email: string) => {
    const e = email.trim().toLowerCase();
    return (
      e === BOOTSTRAPPED_OWNER_EMAIL.toLowerCase() ||
      (storeSettings.ownerEmail?.trim().toLowerCase() === e)
    );
  };

  const openEditor = (u: UserProfile) => {
    const existing = staffMap.get(u.uid);
    const startRole: StaffRole = existing?.role || (u.role !== 'customer' ? u.role : 'order_manager');
    setSelectedUser(u);
    setRole(startRole);
    setPerms(existing?.permissions || STAFF_ROLE_META[startRole].permissions);
    setIsActive(existing?.isActive ?? true);
  };

  const handleSaveStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUser) return;
    try {
      await assignStaffMemberDoc({
        targetUser: selectedUser,
        role,
        customPermissions: perms,
        isActive,
      });
      showToast(`تم تحديث صلاحيات ${selectedUser.fullName}`, 'success');
      setSelectedUser(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ الصلاحيات.'), 'error');
    }
  };

  return (
    <div className="space-y-3">
      {users.map((u) => {
        const st = staffMap.get(u.uid);
        const owner = isTargetOwner(u.email) || st?.role === 'owner' || u.role === 'owner';
        return (
          <div key={u.uid} className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="font-bold text-sm flex items-center gap-2">
                <Shield className="w-4 h-4 text-[#FFD000]" />
                <span>{u.fullName}</span>
                {owner && (
                  <span className="px-2 py-0.5 rounded-full bg-[#181A20] text-[#FFD000] text-[10px] font-black">
                    مالك المتجر
                  </span>
                )}
              </div>
              <div className="text-xs text-[#717784] mt-0.5">
                {u.email} • الدور: {STAFF_ROLE_META[st?.role || u.role]?.label || 'عميل'}
              </div>
            </div>

            {(!owner || isBootstrappedOwner) && !owner && (
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => openEditor(u)}
                  className="px-3.5 py-1.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-extrabold"
                >
                  تعديل الدور والصلاحيات
                </button>
                {st && (
                  <button
                    type="button"
                    onClick={async () => {
                      await removeStaffMemberDoc(u.uid);
                      showToast('تم إزالة الصلاحيات الإدارية', 'info');
                    }}
                    className="px-3 py-1.5 rounded-xl bg-[#FDECEC] text-[#E53935] text-xs font-bold"
                  >
                    إلغاء الصلاحية
                  </button>
                )}
              </div>
            )}
          </div>
        );
      })}

      {selectedUser && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleSaveStaff} className="bg-white rounded-3xl max-w-xl w-full max-h-[90vh] overflow-y-auto p-5 space-y-4">
            <h3 className="font-black text-base">إدارة صلاحيات: {selectedUser.fullName}</h3>
            <select
              value={role}
              onChange={(e) => {
                const next = e.target.value as StaffRole;
                setRole(next);
                setPerms(STAFF_ROLE_META[next].permissions);
              }}
              className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs font-bold"
            >
              {(Object.keys(STAFF_ROLE_META) as StaffRole[])
                .filter((r) => r !== 'customer' && (isBootstrappedOwner || r !== 'owner'))
                .map((r) => (
                  <option key={r} value={r}>{STAFF_ROLE_META[r].label}</option>
                ))}
            </select>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
              {(Object.keys(STAFF_PERMISSION_LABELS) as StaffPermission[]).map((perm) => {
                const checked = perms.includes(perm);
                return (
                  <label
                    key={perm}
                    className={`flex items-center gap-2 p-2.5 rounded-xl border cursor-pointer ${
                      checked ? 'bg-[#FFF9DB] border-[#FFD000]' : 'bg-[#F9FAFB] border-[#EBECEF]'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() =>
                        setPerms((prev) =>
                          prev.includes(perm) ? prev.filter((x) => x !== perm) : [...prev, perm],
                        )
                      }
                    />
                    <span className="font-bold">{STAFF_PERMISSION_LABELS[perm]}</span>
                  </label>
                );
              })}
            </div>

            <label className="flex items-center gap-2 text-xs font-bold">
              <input type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)} />
              <span>الحساب الإداري نشط</span>
            </label>

            <div className="flex gap-2">
              <button type="submit" className="flex-1 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm">
                <Check className="w-4 h-4 inline ml-1" />
                حفظ الصلاحيات
              </button>
              <button type="button" onClick={() => setSelectedUser(null)} className="px-4 py-2.5 rounded-xl border border-[#EBECEF] text-xs font-bold">
                إلغاء
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
