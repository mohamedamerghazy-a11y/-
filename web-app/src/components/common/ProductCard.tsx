import React from 'react';
import { Heart, Package, ShoppingCart, Star, Plus, Minus, SlidersHorizontal } from 'lucide-react';
import { buildCartItemId, type Product } from '../../types/models';
import {
  effectiveDiscountPercentWithShowcases,
  effectiveRetailPriceWithShowcases,
  formatCurrency,
  formatNumber,
} from '../../lib/formatters';
import { useStore } from '../../context/StoreContext';
import {
  CUT_CORNER_CLIP_INNER,
  CUT_CORNER_CLIP_OUTER,
  ProductDiscountBadge,
  getCardBorderGradient,
  getCardShapeClasses,
  type ProductCardVisualStyle,
} from './ProductCardStyling';

interface ProductCardProps {
  product: Product;
  onOpen: () => void;
  flashPrice?: number | null;
  flashSaleId?: string;
  overrideRetailPrice?: number;
  overrideDiscountPercent?: number;
  previewStyleOverride?: ProductCardVisualStyle;
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onOpen,
  flashPrice,
  flashSaleId,
  overrideRetailPrice,
  overrideDiscountPercent,
  previewStyleOverride,
}) => {
  const {
    effectiveWholesaleActive,
    addToCart,
    updateCartQty,
    cartEntries,
    storeSettings,
    brandShowcases,
    wishlistProductIds,
    toggleWishlist,
  } = useStore();

  const cardStyle: ProductCardVisualStyle = previewStyleOverride || {
    borderStyle: storeSettings.productCardBorderStyle || 'default',
    borderColor: storeSettings.productCardBorderColor || '#D4AF37',
    shape: storeSettings.productCardShape || 'rounded',
    discountBadgeStyle: storeSettings.productDiscountBadgeStyle || 'pill',
  };

  const borderVisual = getCardBorderGradient(cardStyle.borderStyle, cardStyle.borderColor);
  const shapeVisual = getCardShapeClasses(cardStyle.shape);
  const isCircular = cardStyle.shape === 'circular';

  const visibleColors = storeSettings.showProductColors ? product.availableColors || [] : [];
  const visibleSizes = storeSettings.showProductSizes ? product.availableSizes || [] : [];
  const hasSelectableVariants = visibleColors.length > 0 || visibleSizes.length > 0;

  const matchingEntries = cartEntries.filter((c) => c.productId === product.id);
  const inCartQty = matchingEntries.reduce((sum, e) => sum + e.quantity, 0);
  const simpleCartEntry = matchingEntries.find(
    (c) => buildCartItemId(c.productId, c.selectedColor, c.selectedSize) === product.id,
  );
  const isWishlisted = wishlistProductIds.includes(product.id);

  const isOut = product.stockQuantity <= 0;
  const effectiveRetail =
    overrideRetailPrice ?? effectiveRetailPriceWithShowcases(product, brandShowcases);
  const discountPct =
    overrideDiscountPercent ?? effectiveDiscountPercentWithShowcases(product, brandShowcases);
  const hasWholesale = effectiveWholesaleActive && product.wholesalePrice > 0;

  const displayPrice =
    flashPrice && flashPrice > 0
      ? flashPrice
      : hasWholesale
      ? product.wholesalePrice
      : effectiveRetail;

  const originalComparePrice =
    flashPrice && flashPrice > 0
      ? effectiveRetail
      : discountPct > 0 && !hasWholesale
      ? product.retailPrice
      : null;

  return (
    <div
      className={`group relative h-full hover:-translate-y-0.5 transition-all duration-200 ${shapeVisual.outerRadius} ${borderVisual.shadowClass} hover:shadow-[0_12px_28px_-10px_rgba(24,26,32,0.18)]`}
      style={{
        background: borderVisual.background,
        padding: `${borderVisual.paddingPx}px`,
        clipPath: shapeVisual.useCutCorner ? CUT_CORNER_CLIP_OUTER : undefined,
      }}
    >
      <article
        className={`relative h-full flex flex-col bg-white overflow-hidden ${shapeVisual.innerRadius}`}
        style={{
          clipPath: shapeVisual.useCutCorner ? CUT_CORNER_CLIP_INNER : undefined,
        }}
      >
        {/* Crisp Image Stage */}
        <div
          className={`relative aspect-square w-full overflow-hidden flex items-center justify-center border-b border-[#F0F1F4] ${
            isCircular ? 'bg-white p-4' : 'bg-[#F7F8FA] p-2.5 sm:p-3.5'
          }`}
        >
          <button
            type="button"
            onClick={onOpen}
            className={
              isCircular
                ? 'w-full h-full rounded-full bg-[#F7F8FA] border border-[#E8EAEE] overflow-hidden p-3 flex items-center justify-center'
                : 'w-full h-full flex items-center justify-center'
            }
          >
            {product.imageUrls[0] ? (
              <img
                src={product.imageUrls[0]}
                alt={product.name}
                loading="lazy"
                className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
              />
            ) : (
              <Package className="w-11 h-11 text-[#C4C9D2]" />
            )}
          </button>

          {/* Badges top-start */}
          <div className="absolute top-2 right-2 flex flex-col gap-1 items-start pointer-events-none">
            {flashPrice ? (
              <span className="px-2 py-0.5 rounded-lg text-[10px] sm:text-[11px] font-black bg-[#E53935] text-white shadow-xs">
                فلاش سيل ⚡
              </span>
            ) : discountPct > 0 && !hasWholesale ? (
              <ProductDiscountBadge
                discountPercent={discountPct}
                style={cardStyle.discountBadgeStyle}
              />
            ) : null}

            {hasWholesale && !flashPrice && (
              <span className="px-2 py-0.5 rounded-lg text-[10px] sm:text-[11px] font-extrabold bg-[#FFD000] text-[#181A20] shadow-xs">
                سعر الجملة
              </span>
            )}
          </div>

          {/* Wishlist Heart top-end */}
          <button
            type="button"
            aria-label={isWishlisted ? 'إزالة من المفضلة' : 'إضافة للمفضلة'}
            onClick={(e) => {
              e.stopPropagation();
              if (!previewStyleOverride) {
                toggleWishlist(product.id);
              }
            }}
            className="absolute top-2 left-2 w-8 h-8 rounded-full bg-white/95 backdrop-blur border border-[#EBECEF] flex items-center justify-center shadow-xs hover:scale-110 transition-transform"
          >
            <Heart
              className={`w-4 h-4 transition-colors ${
                isWishlisted ? 'fill-[#E53935] text-[#E53935]' : 'text-[#717784]'
              }`}
            />
          </button>

          {/* Rating pill bottom-start */}
          {product.reviewCount > 0 && (
            <div className="absolute bottom-2 right-2 flex items-center gap-1 bg-white/95 backdrop-blur px-2 py-0.5 rounded-full border border-[#EBECEF] text-[10px] sm:text-[11px] font-extrabold text-[#181A20] pointer-events-none shadow-2xs">
              <Star className="w-3 h-3 fill-[#FFD000] text-[#FFD000]" />
              <span>{formatNumber(product.averageRating)}</span>
            </div>
          )}

          {isOut && (
            <div className="absolute inset-0 bg-white/75 backdrop-blur-[1px] flex items-center justify-center pointer-events-none">
              <span className="px-3 py-1 rounded-full bg-[#181A20] text-white text-xs font-bold">
                نفدت الكمية
              </span>
            </div>
          )}
        </div>

        {/* Product Body */}
        <div className="flex-1 flex flex-col p-2.5 sm:p-3.5 gap-1.5 sm:gap-2">
          <button
            type="button"
            onClick={onOpen}
            className="text-right font-bold text-xs sm:text-[13px] lg:text-sm text-[#181A20] line-clamp-2 min-h-[2.25rem] sm:min-h-[2.5rem] leading-snug hover:text-[#1565C0] transition-colors"
          >
            {product.name}
          </button>

          <div className="text-[11px] text-[#717784] flex flex-wrap items-center justify-between gap-1">
            <span className="truncate">{product.unit || 'قطعة'}</span>
            {hasWholesale && (
              <span className="text-[#1565C0] font-bold text-[10px]">
                أقل جملة: {formatNumber(product.wholesaleMinQty)}
              </span>
            )}
          </div>

          {hasSelectableVariants && (
            <div className="flex flex-wrap gap-1 text-[10px] font-bold text-[#5B606E]">
              {visibleColors.length > 0 && (
                <span className="px-1.5 py-0.5 rounded-md bg-[#F4F5F8] border border-[#EBECEF]">
                  {formatNumber(visibleColors.length)} ألوان
                </span>
              )}
              {visibleSizes.length > 0 && (
                <span className="px-1.5 py-0.5 rounded-md bg-[#F4F5F8] border border-[#EBECEF]">
                  {formatNumber(visibleSizes.length)} مقاسات
                </span>
              )}
            </div>
          )}

          {/* Pricing Block */}
          <div className="mt-auto pt-1 space-y-2">
            <div className="flex items-baseline gap-1.5 flex-wrap">
              <span className="text-sm sm:text-base font-black text-[#181A20] tracking-tight">
                {formatCurrency(displayPrice)}
              </span>
              {originalComparePrice && originalComparePrice > displayPrice && (
                <span className="text-[11px] text-[#8B919E] line-through font-semibold">
                  {formatCurrency(originalComparePrice)}
                </span>
              )}
            </div>

            {/* Uniform Full-Width Action Bar */}
            {!isOut ? (
              hasSelectableVariants ? (
                <button
                  type="button"
                  onClick={onOpen}
                  className="w-full h-9 px-2.5 rounded-xl bg-[#FFF9DB] hover:bg-[#FFD000] border border-[#FFD000] text-[#181A20] font-extrabold text-xs flex items-center justify-center gap-1.5 transition-colors"
                >
                  <SlidersHorizontal className="w-3.5 h-3.5" />
                  <span>اختر الخيارات</span>
                </button>
              ) : simpleCartEntry && inCartQty > 0 && !previewStyleOverride ? (
                <div className="w-full h-9 flex items-center justify-between bg-[#FFF9DB] border border-[#FFD000] rounded-xl px-1.5">
                  <button
                    type="button"
                    aria-label="زيادة الكمية"
                    onClick={() =>
                      updateCartQty(
                        product.id,
                        Math.min(product.stockQuantity, inCartQty + 1),
                        flashSaleId,
                      )
                    }
                    className="w-7 h-7 rounded-lg bg-[#FFD000] text-[#181A20] flex items-center justify-center hover:bg-[#E5B800] transition"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                  <span className="text-xs font-black text-[#181A20]">
                    {formatNumber(inCartQty)} في السلة
                  </span>
                  <button
                    type="button"
                    aria-label="تقليل الكمية"
                    onClick={() => updateCartQty(product.id, inCartQty - 1, flashSaleId)}
                    className="w-7 h-7 rounded-lg bg-white text-[#181A20] border border-[#EBECEF] flex items-center justify-center hover:bg-gray-50 transition"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => {
                    if (!previewStyleOverride) {
                      addToCart(product, 1, flashSaleId);
                    }
                  }}
                  className="w-full h-9 px-2.5 rounded-xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-extrabold text-xs flex items-center justify-center gap-1.5 transition-colors shadow-2xs"
                >
                  <ShoppingCart className="w-3.5 h-3.5" />
                  <span>أضف للسلة</span>
                </button>
              )
            ) : (
              <button
                type="button"
                onClick={onOpen}
                className="w-full h-9 px-2.5 rounded-xl bg-[#F4F5F8] hover:bg-[#EBECEF] text-[#5B606E] font-bold text-xs transition"
              >
                التفاصيل والتنبيه
              </button>
            )}
          </div>
        </div>
      </article>
    </div>
  );
};
