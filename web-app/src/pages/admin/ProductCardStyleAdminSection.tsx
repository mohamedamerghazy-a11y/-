import React, { useMemo } from 'react';
import { Check, Save, Sparkles } from 'lucide-react';
import {
  PRODUCT_CARD_BORDER_STYLE_OPTIONS,
  PRODUCT_CARD_CUSTOM_SWATCHES,
  PRODUCT_CARD_SHAPE_OPTIONS,
  PRODUCT_DISCOUNT_BADGE_STYLE_OPTIONS,
  sanitizeCardHexColor,
  type Product,
  type ProductCardBorderStyle,
  type ProductCardShape,
  type ProductDiscountBadgeStyle,
  type StoreGeneralSettings,
} from '../../types/models';
import { ProductCard } from '../../components/common/ProductCard';
import {
  CUT_CORNER_CLIP_OUTER,
  ProductDiscountBadge,
  getCardBorderGradient,
  getCardShapeClasses,
} from '../../components/common/ProductCardStyling';

interface Props {
  form: StoreGeneralSettings;
  saving: boolean;
  onChange: (patch: Partial<StoreGeneralSettings>) => void;
  onSave: () => void;
}

export const ProductCardStyleAdminSection: React.FC<Props> = ({
  form,
  saving,
  onChange,
  onSave,
}) => {
  const borderStyle: ProductCardBorderStyle = form.productCardBorderStyle || 'default';
  const borderColor = sanitizeCardHexColor(form.productCardBorderColor);
  const shape: ProductCardShape = form.productCardShape || 'rounded';
  const discountBadgeStyle: ProductDiscountBadgeStyle = form.productDiscountBadgeStyle || 'pill';

  const sampleProduct = useMemo<Product>(
    () => ({
      id: 'preview_card_sample',
      name: 'مسحوق غسيل أوتوماتيك 5 كيلو - عرض التوفير',
      description: 'منتج تجريبي لمعاينة شكل كارت المنتج',
      categoryId: 'preview',
      sku: 'PREV-01',
      imageUrls: [],
      retailPrice: 250,
      retailDiscountPrice: 185,
      wholesalePrice: 165,
      wholesaleMinQty: 6,
      unit: 'عبوة',
      stockQuantity: 24,
      lowStockThreshold: 5,
      isFeatured: true,
      isOffer: true,
      isActive: true,
      salesCount: 40,
      averageRating: 4.9,
      reviewCount: 38,
    }),
    [],
  );

  const previewBorderVisual = getCardBorderGradient(borderStyle, borderColor);

  return (
    <div className="p-5 pt-3 border-t border-[#EBECEF] space-y-5 text-xs">
      {/* Live Interactive Preview */}
      <div className="p-4 rounded-2xl bg-[#F7F8FA] border border-[#EBECEF] flex flex-col lg:flex-row items-center gap-5">
        <div className="w-[225px] shrink-0">
          <ProductCard
            product={sampleProduct}
            onOpen={() => {}}
            overrideRetailPrice={185}
            overrideDiscountPercent={26}
            previewStyleOverride={{
              borderStyle,
              borderColor,
              shape,
              discountBadgeStyle,
            }}
          />
        </div>
        <div className="flex-1 space-y-2 text-right">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#FFF9DB] border border-[#FFD000] text-[#181A20] font-black text-[11px]">
            <Sparkles className="w-3.5 h-3.5" />
            <span>معاينة مباشرة فورية</span>
          </div>
          <h4 className="font-black text-sm text-[#181A20]">
            خصص شكل كروت المنتجات وشارة الخصم على الأندرويد والويب
          </h4>
          <p className="text-[11px] text-[#5B606E] leading-relaxed">
            جرّب أي إطار (مثل الإطار الذهبي الفاخر أو لونك الخاص)، واختر الشكل الهندسي للكارت (دائري، مستطيل، مشطوف، مائل، أو قوس)، ثم اختر تصميم شارة الخصم المناسب، واضغط حفظ لتطبيقه فورًا على المتجر بالكامل.
          </p>
        </div>
      </div>

      {/* 1. Border Style & Color */}
      <div className="space-y-2.5">
        <div className="font-black text-sm text-[#181A20]">1) إطار ولون حدود كارت المنتج</div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {PRODUCT_CARD_BORDER_STYLE_OPTIONS.map((opt) => {
            const selected = borderStyle === opt.code;
            return (
              <button
                key={opt.code}
                type="button"
                onClick={() => onChange({ productCardBorderStyle: opt.code })}
                className={`p-3 rounded-xl border text-right flex items-center gap-3 transition ${
                  selected
                    ? 'bg-[#FFF9DB] border-[#181A20] ring-1 ring-[#181A20]/20'
                    : 'bg-white border-[#EBECEF] hover:border-[#FFD000]'
                }`}
              >
                <span
                  className="w-7 h-7 rounded-full shrink-0 border border-black/15 shadow-xs"
                  style={{
                    background:
                      opt.code === 'custom' ? borderColor : opt.swatchBackground,
                  }}
                />
                <div className="min-w-0 flex-1">
                  <div className="font-black text-[#181A20] truncate">{opt.label}</div>
                  <div className="text-[11px] text-[#717784] truncate">{opt.subtitle}</div>
                </div>
                {selected && (
                  <span className="px-2 py-0.5 rounded-full bg-[#219653] text-white text-[10px] font-bold shrink-0">
                    مفعل
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {borderStyle === 'custom' && (
          <div className="p-3.5 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] space-y-3">
            <div className="font-bold text-[#181A20]">اختر لون الإطار المخصص:</div>
            <div className="flex flex-wrap items-center gap-2.5">
              {PRODUCT_CARD_CUSTOM_SWATCHES.map((hex) => {
                const picked = borderColor.toUpperCase() === hex.toUpperCase();
                return (
                  <button
                    key={hex}
                    type="button"
                    aria-label={`اختيار اللون ${hex}`}
                    onClick={() => onChange({ productCardBorderColor: hex })}
                    className={`w-8 h-8 rounded-full flex items-center justify-center transition-transform hover:scale-110 ${
                      picked ? 'ring-2 ring-offset-2 ring-[#181A20]' : 'border border-black/15'
                    }`}
                    style={{ background: hex }}
                  >
                    {picked && <Check className="w-4 h-4 text-white" />}
                  </button>
                );
              })}
              <label className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white border border-[#EBECEF] font-bold cursor-pointer">
                <span>لون حر:</span>
                <input
                  type="color"
                  value={borderColor}
                  onChange={(e) => onChange({ productCardBorderColor: e.target.value.toUpperCase() })}
                  className="w-6 h-6 border-0 bg-transparent cursor-pointer"
                />
                <span className="font-mono text-[11px] text-[#5B606E]" dir="ltr">
                  {borderColor}
                </span>
              </label>
            </div>
          </div>
        )}
      </div>

      {/* 2. Card Shape / Geometry */}
      <div className="space-y-2.5 pt-2 border-t border-[#EBECEF]">
        <div className="font-black text-sm text-[#181A20]">2) شكل الكارت (دائري / مستطيل / هندسي)</div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {PRODUCT_CARD_SHAPE_OPTIONS.map((opt) => {
            const selected = shape === opt.code;
            const miniShape = getCardShapeClasses(opt.code);
            return (
              <button
                key={opt.code}
                type="button"
                onClick={() => onChange({ productCardShape: opt.code })}
                className={`p-3 rounded-xl border text-right flex items-center gap-3 transition ${
                  selected
                    ? 'bg-[#FFF9DB] border-[#181A20] ring-1 ring-[#181A20]/20'
                    : 'bg-white border-[#EBECEF] hover:border-[#FFD000]'
                }`}
              >
                <div
                  className={`w-11 h-9 shrink-0 flex items-center justify-center ${miniShape.outerRadius}`}
                  style={{
                    background:
                      borderStyle === 'none' ? '#9CA3AF' : previewBorderVisual.background,
                    padding: '2px',
                    clipPath: miniShape.useCutCorner ? CUT_CORNER_CLIP_OUTER : undefined,
                  }}
                >
                  <div
                    className={`w-full h-full bg-white ${miniShape.innerRadius}`}
                    style={{
                      clipPath: miniShape.useCutCorner ? CUT_CORNER_CLIP_OUTER : undefined,
                    }}
                  />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-black text-[#181A20]">{opt.label}</div>
                  <div className="text-[11px] text-[#717784] leading-snug">{opt.subtitle}</div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* 3. Discount Badge Shape */}
      <div className="space-y-2.5 pt-2 border-t border-[#EBECEF]">
        <div className="font-black text-sm text-[#181A20]">3) شكل شارة الخصم على المنتج</div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {PRODUCT_DISCOUNT_BADGE_STYLE_OPTIONS.map((opt) => {
            const selected = discountBadgeStyle === opt.code;
            return (
              <button
                key={opt.code}
                type="button"
                onClick={() => onChange({ productDiscountBadgeStyle: opt.code })}
                className={`p-3 rounded-xl border text-right space-y-2 transition ${
                  selected
                    ? 'bg-[#FFF9DB] border-[#181A20] ring-1 ring-[#181A20]/20'
                    : 'bg-white border-[#EBECEF] hover:border-[#FFD000]'
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-black text-[#181A20]">{opt.label}</span>
                  {selected && (
                    <span className="px-2 py-0.5 rounded-full bg-[#219653] text-white text-[10px] font-bold">
                      مفعل
                    </span>
                  )}
                </div>
                <div className="h-12 rounded-lg bg-[#F7F8FA] flex items-center justify-center">
                  <ProductDiscountBadge discountPercent={25} style={opt.code} />
                </div>
                <div className="text-[11px] text-[#717784]">{opt.subtitle}</div>
              </button>
            );
          })}
        </div>
      </div>

      <button
        type="button"
        disabled={saving}
        onClick={onSave}
        className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-black transition"
      >
        <Save className="w-4 h-4" />
        <span>حفظ تصميم كروت المنتجات</span>
      </button>
    </div>
  );
};
