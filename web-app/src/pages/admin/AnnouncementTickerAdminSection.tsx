import React, { useRef, useState } from 'react';
import { ImagePlus, Italic, Loader2, Minus, Plus, Save, Trash2 } from 'lucide-react';
import {
  BANNER_PLACEMENT_LABELS,
  BANNER_PLACEMENT_ORDER,
  TICKER_FONT_WEIGHT_OPTIONS,
  sanitizeCardHexColor,
  type BannerPlacement,
  type StoreGeneralSettings,
  type TickerFontWeight,
} from '../../types/models';
import { uploadImageToCloudinary } from '../../lib/cloudinary';
import { useStore } from '../../context/StoreContext';
import { formatNumber } from '../../lib/formatters';
import { AnnouncementTickerBar } from '../../components/common/AnnouncementTickerBar';

const QUICK_EMOJIS = [
  '🔥', '⚡', '🎉', '💥', '🛍️', '🎁', '⭐', '🚚', '💰',
  '✨', '🏷️', '🌙', '🎈', '📢', '✅', '❤️', '💎', '🛒',
];

const THEME_PRESETS = [
  { name: 'أسود وذهبي فاخر', bg: '#181A20', border: '#FFD000', text: '#FFFFFF' },
  { name: 'أصفر الإيمان', bg: '#FFD000', border: '#181A20', text: '#181A20' },
  { name: 'عروض نارية', bg: '#DC2626', border: '#FFD000', text: '#FFFFFF' },
  { name: 'أزرق ملكي', bg: '#1565C0', border: '#38BDF8', text: '#FFFFFF' },
  { name: 'زمردي أنيق', bg: '#059669', border: '#FFD000', text: '#FFFFFF' },
];

const BG_SWATCHES = [
  '#181A20', '#FFD000', '#1565C0', '#059669', '#DC2626',
  '#5B21B6', '#FFFFFF', '#FFF9DB', '#881337', '#0F766E',
];

const BORDER_SWATCHES = [
  '#FFD000', '#D4AF37', '#FFFFFF', '#181A20', '#EF4444',
  '#10B981', '#38BDF8', '#F59E0B', '#A855F7', '#E5E7EB',
];

const TEXT_SWATCHES = [
  '#FFFFFF', '#FFD000', '#181A20', '#FEF3C7', '#E0F2FE',
  '#DC2626', '#15803D', '#D4AF37', '#1D4ED8', '#F9FAFB',
];

interface AnnouncementTickerAdminSectionProps {
  form: StoreGeneralSettings;
  setForm: React.Dispatch<React.SetStateAction<StoreGeneralSettings>>;
  saving: boolean;
  onSave: () => void;
}

export const AnnouncementTickerAdminSection: React.FC<AnnouncementTickerAdminSectionProps> = ({
  form,
  setForm,
  saving,
  onSave,
}) => {
  const { showToast } = useStore();
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [uploadingThumb, setUploadingThumb] = useState(false);

  const insertAtCursor = (snippet: string) => {
    const el = textareaRef.current;
    const current = form.tickerText || '';
    const start = el ? (el.selectionStart ?? current.length) : current.length;
    const end = el ? (el.selectionEnd ?? current.length) : current.length;
    const nextText = (current.slice(0, start) + snippet + current.slice(end)).slice(0, 1000);
    setForm((prev) => ({ ...prev, tickerText: nextText }));
    window.requestAnimationFrame(() => {
      if (textareaRef.current) {
        const nextPos = Math.min(nextText.length, start + snippet.length);
        textareaRef.current.focus();
        textareaRef.current.setSelectionRange(nextPos, nextPos);
      }
    });
  };

  const handleUploadInlineImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingThumb(true);
    try {
      const url = await uploadImageToCloudinary(file, 'aleman/banners');
      const existing = form.tickerInlineImages || [];
      const existingIds = new Set(existing.map((i) => i.id));
      let idx = existing.length + 1;
      while (existingIds.has(`img${idx}`)) idx++;
      const newId = `img${idx}`;
      const el = textareaRef.current;
      const currentText = form.tickerText || '';
      const start = el ? (el.selectionStart ?? currentText.length) : currentText.length;
      const end = el ? (el.selectionEnd ?? currentText.length) : currentText.length;
      const token = ` [[img:${newId}]] `;
      const nextText = (currentText.slice(0, start) + token + currentText.slice(end)).slice(0, 1000);
      setForm((prev) => ({
        ...prev,
        tickerInlineImages: [...(prev.tickerInlineImages || []), { id: newId, url }].slice(0, 10),
        tickerText: nextText,
      }));
      showToast('تم رفع الصورة المصغرة وإدراجها داخل نص الشريط بنجاح', 'success');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'تعذر رفع الصورة المصغرة.';
      showToast(msg, 'error');
    } finally {
      setUploadingThumb(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const renderColorControls = (
    title: string,
    value: string,
    swatches: string[],
    fallback: string,
    onChange: (hex: string) => void,
  ) => {
    const safeHex = sanitizeCardHexColor(value, fallback);
    return (
      <div className="space-y-1.5">
        <div className="flex items-center justify-between gap-2">
          <span className="font-bold text-[#181A20]">{title}</span>
          <div className="flex items-center gap-2">
            <input
              type="color"
              value={safeHex}
              onChange={(e) => onChange(e.target.value.toUpperCase())}
              aria-label={title}
              className="w-7 h-7 rounded-lg border border-[#EBECEF] cursor-pointer bg-white p-0.5"
            />
            <input
              type="text"
              dir="ltr"
              value={value}
              maxLength={7}
              onChange={(e) => onChange(e.target.value)}
              className="w-24 h-7 px-2 rounded-lg border border-[#EBECEF] text-[11px] font-mono text-center"
            />
          </div>
        </div>
        <div className="flex flex-wrap gap-1.5">
          {swatches.map((hex) => {
            const active = safeHex === hex;
            return (
              <button
                key={hex}
                type="button"
                aria-label={`${title} ${hex}`}
                onClick={() => onChange(hex)}
                className={`w-7 h-7 rounded-full border transition ${
                  active ? 'ring-2 ring-[#FFD000] ring-offset-1 scale-105' : 'border-[#D1D5DB]'
                }`}
                style={{ backgroundColor: hex }}
              />
            );
          })}
        </div>
      </div>
    );
  };

  const fontSize = Math.min(24, Math.max(11, Math.round(Number(form.tickerFontSizeSp || 14))));

  return (
    <div className="space-y-4 text-xs">
      {/* Live Preview */}
      <div className="rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-3.5 space-y-2">
        <div className="font-bold text-[#717784]">
          معاينة حية للشريط الإعلاني (يتحرك تلقائيًا من اليسار إلى اليمين)
        </div>
        <AnnouncementTickerBar settings={form} forcePreview />
      </div>

      {/* Enable Toggle */}
      <label className="flex items-center justify-between gap-3 rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-3.5 cursor-pointer">
        <span className="font-black text-[#181A20]">تفعيل الشريط الإعلاني المتحرك في المتجر</span>
        <input
          type="checkbox"
          checked={Boolean(form.tickerEnabled)}
          onChange={(e) => setForm({ ...form, tickerEnabled: e.target.checked })}
          className="w-4 h-4 accent-[#181A20]"
        />
      </label>

      {/* Textarea with cursor-aware Emoji & Mini-Image insertion */}
      <div className="space-y-2">
        <label className="block font-bold text-[#181A20]">
          نص الشريط الإعلاني (ضع المؤشر في أي مكان — حتى وسط الكلام — لإدراج رمز أو صورة مصغرة)
        </label>
        <textarea
          ref={textareaRef}
          rows={3}
          maxLength={1000}
          value={form.tickerText || ''}
          onChange={(e) => setForm({ ...form, tickerText: e.target.value })}
          placeholder="مثال: 🔥 عروض اليوم الكبرى [[img:img1]] توصيل سريع لكل المحافظات 🚚"
          className="w-full p-3 rounded-xl border border-[#EBECEF] focus:outline-none focus:border-[#181A20] text-sm"
        />

        {/* Quick Emoji Picker */}
        <div className="space-y-1">
          <div className="font-bold text-[#717784]">إدراج رموز سريعة في مكان المؤشر:</div>
          <div className="flex flex-wrap gap-1.5">
            {QUICK_EMOJIS.map((emoji) => (
              <button
                key={emoji}
                type="button"
                onClick={() => insertAtCursor(emoji)}
                className="w-9 h-9 rounded-xl border border-[#EBECEF] bg-white hover:bg-[#FFF9DB] hover:border-[#FFD000] text-base flex items-center justify-center transition"
              >
                {emoji}
              </button>
            ))}
          </div>
        </div>

        {/* Inline Mini-Image Uploader */}
        <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
          <button
            type="button"
            disabled={uploadingThumb || (form.tickerInlineImages || []).length >= 10}
            onClick={() => fileInputRef.current?.click()}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#181A20] text-[#FFD000] font-black disabled:opacity-60"
          >
            {uploadingThumb ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <ImagePlus className="w-4 h-4" />
            )}
            <span>
              {uploadingThumb ? 'جاري رفع الصورة المصغرة...' : 'إضافة صورة مصغرة وسط الكلام (مكان المؤشر)'}
            </span>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handleUploadInlineImage}
            className="hidden"
          />
          <span className="text-[11px] text-[#717784]">
            يمكنك رفع حتى ١٠ صور مصغرة وإدراجها في أي موضع داخل النص
          </span>
        </div>

        {(form.tickerInlineImages || []).length > 0 && (
          <div className="flex flex-wrap gap-2 pt-1">
            {(form.tickerInlineImages || []).map((img) => (
              <div
                key={img.id}
                className="inline-flex items-center gap-1.5 pl-1.5 pr-2 py-1 rounded-xl border border-[#EBECEF] bg-white"
              >
                <button
                  type="button"
                  onClick={() => insertAtCursor(` [[img:${img.id}]] `)}
                  className="inline-flex items-center gap-1.5 hover:opacity-80"
                  title="اضغط لإدراج الصورة مجددًا مكان المؤشر"
                >
                  <img src={img.url} alt={img.id} className="w-7 h-7 rounded-md object-cover border border-[#EBECEF]" />
                  <span className="font-bold text-[#181A20]">{img.id}</span>
                </button>
                <button
                  type="button"
                  aria-label="حذف الصورة المصغرة"
                  onClick={() =>
                    setForm((prev) => ({
                      ...prev,
                      tickerInlineImages: (prev.tickerInlineImages || []).filter((i) => i.id !== img.id),
                      tickerText: (prev.tickerText || '').replaceAll(`[[img:${img.id}]]`, '').replace(/\s{2,}/g, ' '),
                    }))
                  }
                  className="p-1 rounded-lg text-[#E53935] hover:bg-[#FDECEC]"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Placement Selector */}
      <div className="space-y-1.5">
        <label className="block font-bold text-[#181A20]">موقع ظهور الشريط في الصفحة الرئيسية</label>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {BANNER_PLACEMENT_ORDER.map((pos: BannerPlacement) => {
            const active = (form.tickerPlacement || 'top_home') === pos;
            return (
              <button
                key={pos}
                type="button"
                onClick={() => setForm({ ...form, tickerPlacement: pos })}
                className={`px-2.5 py-2 rounded-xl border text-[11px] font-bold transition text-center ${
                  active
                    ? 'bg-[#181A20] text-[#FFD000] border-[#181A20]'
                    : 'bg-white text-[#181A20] border-[#EBECEF] hover:bg-[#F9FAFB]'
                }`}
              >
                {BANNER_PLACEMENT_LABELS[pos]}
              </button>
            );
          })}
        </div>
      </div>

      {/* Quick Theme Presets */}
      <div className="space-y-1.5">
        <div className="font-bold text-[#181A20]">تنسيقات ألوان جاهزة سريعة:</div>
        <div className="flex flex-wrap gap-2">
          {THEME_PRESETS.map((p) => (
            <button
              key={p.name}
              type="button"
              onClick={() =>
                setForm({
                  ...form,
                  tickerBackgroundColor: p.bg,
                  tickerBorderColor: p.border,
                  tickerTextColor: p.text,
                })
              }
              className="px-3 py-1.5 rounded-full font-extrabold text-[11px] transition hover:scale-105"
              style={{
                backgroundColor: p.bg,
                color: p.text,
                border: `2px solid ${p.border}`,
              }}
            >
              {p.name}
            </button>
          ))}
        </div>
      </div>

      {/* 3 Color Pickers */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-4">
        {renderColorControls(
          'لون أرضية الشريط',
          form.tickerBackgroundColor || '#181A20',
          BG_SWATCHES,
          '#181A20',
          (hex) => setForm({ ...form, tickerBackgroundColor: hex }),
        )}
        {renderColorControls(
          'لون الشريط (الإطار والزينة)',
          form.tickerBorderColor || '#FFD000',
          BORDER_SWATCHES,
          '#FFD000',
          (hex) => setForm({ ...form, tickerBorderColor: hex }),
        )}
        {renderColorControls(
          'لون الخط',
          form.tickerTextColor || '#FFFFFF',
          TEXT_SWATCHES,
          '#FFFFFF',
          (hex) => setForm({ ...form, tickerTextColor: hex }),
        )}
      </div>

      {/* Typography Controls */}
      <div className="rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-4 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="font-bold text-[#181A20]">
            حجم الخط: <span className="font-black text-sm">{formatNumber(fontSize)}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              aria-label="تصغير الخط"
              onClick={() => setForm({ ...form, tickerFontSizeSp: Math.max(11, fontSize - 1) })}
              className="w-8 h-8 rounded-full border border-[#EBECEF] bg-white flex items-center justify-center hover:bg-[#FFF9DB]"
            >
              <Minus className="w-4 h-4" />
            </button>
            {[12, 14, 17, 20].map((sp) => (
              <button
                key={sp}
                type="button"
                onClick={() => setForm({ ...form, tickerFontSizeSp: sp })}
                className={`px-2.5 h-8 rounded-lg border font-bold ${
                  fontSize === sp
                    ? 'bg-[#FFD000] text-[#181A20] border-[#181A20]'
                    : 'bg-white text-[#181A20] border-[#EBECEF]'
                }`}
              >
                {formatNumber(sp)}
              </button>
            ))}
            <button
              type="button"
              aria-label="تكبير الخط"
              onClick={() => setForm({ ...form, tickerFontSizeSp: Math.min(24, fontSize + 1) })}
              className="w-8 h-8 rounded-full border border-[#EBECEF] bg-white flex items-center justify-center hover:bg-[#FFF9DB]"
            >
              <Plus className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-center">
          <div className="space-y-1.5">
            <div className="font-bold text-[#181A20]">سمك الخط:</div>
            <div className="flex flex-wrap gap-1.5">
              {TICKER_FONT_WEIGHT_OPTIONS.map((opt) => {
                const active = (form.tickerFontWeight || 'bold') === opt.code;
                return (
                  <button
                    key={opt.code}
                    type="button"
                    onClick={() => setForm({ ...form, tickerFontWeight: opt.code as TickerFontWeight })}
                    style={{ fontWeight: opt.cssWeight }}
                    className={`px-3 py-1.5 rounded-xl border transition ${
                      active
                        ? 'bg-[#181A20] text-[#FFD000] border-[#181A20]'
                        : 'bg-white text-[#181A20] border-[#EBECEF]'
                    }`}
                  >
                    {opt.label}
                  </button>
                );
              })}
            </div>
          </div>

          <label className="flex items-center justify-between gap-2 rounded-xl border border-[#EBECEF] bg-white px-3.5 py-2.5 cursor-pointer">
            <span className="inline-flex items-center gap-1.5 font-bold italic text-[#181A20]">
              <Italic className="w-4 h-4" />
              <span>خط مائل (Italic)</span>
            </span>
            <input
              type="checkbox"
              checked={Boolean(form.tickerItalic)}
              onChange={(e) => setForm({ ...form, tickerItalic: e.target.checked })}
              className="w-4 h-4 accent-[#181A20]"
            />
          </label>
        </div>
      </div>

      <button
        type="button"
        disabled={saving}
        onClick={onSave}
        className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black"
      >
        <Save className="w-4 h-4" />
        <span>حفظ إعدادات الشريط الإعلاني</span>
      </button>
    </div>
  );
};
