import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  Bell,
  BellRing,
  Bolt,
  Building2,
  Check,
  Heart,
  Minus,
  Package,
  Plus,
  ShieldCheck,
  ShoppingCart,
  Star,
  Store,
  Trash2,
  Truck,
} from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import { ProductCard } from '../../components/common/ProductCard';
import {
  effectiveDiscountPercentWithShowcases,
  effectiveRetailPriceWithShowcases,
  effectiveShowcaseDiscountPercent,
  formatCurrency,
  formatDateTime,
  formatNumber,
  getFlashSaleStatus,
  getFlashUnitPrice,
  toArabicErrorMessage,
} from '../../lib/formatters';
import type { ProductReview } from '../../types/models';
import {
  deleteReviewDoc,
  subscribeProductReviews,
  submitOrUpdateReview,
} from '../../services/firestoreService';

interface ProductDetailsPageProps {
  productId: string;
  flashSaleId?: string;
}

export const ProductDetailsPage: React.FC<ProductDetailsPageProps> = ({
  productId,
  flashSaleId,
}) => {
  const {
    products,
    storefrontProducts,
    categories,
    companies,
    brandShowcases,
    flashSales,
    effectiveWholesaleActive,
    addToCart,
    firebaseUser,
    userProfile,
    storeSettings,
    wishlistProductIds,
    toggleWishlist,
    stockAlertProductIds,
    toggleStockAlert,
    showToast,
    goBack,
    navigate,
  } = useStore();

  const product = products.find((p) => p.id === productId);
  const [activeImageIdx, setActiveImageIdx] = useState(0);
  const [qty, setQty] = useState(1);
  const [selectedColor, setSelectedColor] = useState<string>('');
  const [selectedSize, setSelectedSize] = useState<string>('');
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [myRating, setMyRating] = useState(5);
  const [myComment, setMyComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);
  const [togglingAlert, setTogglingAlert] = useState(false);

  const visibleColors = storeSettings.showProductColors ? product?.availableColors || [] : [];
  const visibleSizes = storeSettings.showProductSizes ? product?.availableSizes || [] : [];
  const isWishlisted = wishlistProductIds.includes(productId);
  const isStockAlertActive = stockAlertProductIds.includes(productId);

  const relatedProducts = useMemo(() => {
    if (!product) return [];
    return storefrontProducts
      .filter(
        (p) =>
          p.id !== product.id &&
          (p.categoryId === product.categoryId ||
            (product.companyId && p.companyId === product.companyId)),
      )
      .slice(0, 5);
  }, [product, storefrontProducts]);

  useEffect(() => {
    setActiveImageIdx(0);
    setQty(1);
    const p = products.find((item) => item.id === productId);
    setSelectedColor(
      storeSettings.showProductColors && p?.availableColors?.[0] ? p.availableColors[0] : '',
    );
    setSelectedSize(
      storeSettings.showProductSizes && p?.availableSizes?.[0] ? p.availableSizes[0] : '',
    );
    const unsub = subscribeProductReviews(productId, setReviews);
    return () => unsub();
  }, [productId, products, storeSettings.showProductColors, storeSettings.showProductSizes]);

  if (!product) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12 text-center space-y-4">
        <Package className="w-14 h-14 mx-auto text-[#717784]" />
        <h2 className="text-lg font-bold">المنتج غير موجود أو غير متاح حاليًا</h2>
        <button
          type="button"
          onClick={goBack}
          className="px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-bold text-sm"
        >
          العودة للمتجر
        </button>
      </div>
    );
  }

  const category = categories.find((c) => c.id === product.categoryId);
  const company = product.companyId ? companies.find((c) => c.id === product.companyId) : undefined;
  const activeFlash = flashSaleId
    ? flashSales.find(
        (f) => f.id === flashSaleId && f.isActive && getFlashSaleStatus(f) === 'active',
      )
    : undefined;
  const flashPrice = activeFlash
    ? getFlashUnitPrice(activeFlash, product.id, effectiveWholesaleActive)
    : null;

  const minWholesaleQty = Math.max(1, product.wholesaleMinQty);
  const isWholesaleApplied =
    !flashPrice && effectiveWholesaleActive && product.wholesalePrice > 0 && qty >= minWholesaleQty;
  const effectiveRetail = effectiveRetailPriceWithShowcases(product, brandShowcases);
  const unitPrice = flashPrice ?? (isWholesaleApplied ? product.wholesalePrice : effectiveRetail);
  const discountPct = effectiveDiscountPercentWithShowcases(product, brandShowcases);
  const showcaseDiscountPct = effectiveShowcaseDiscountPercent(product, brandShowcases);

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!firebaseUser) {
      showToast('سجل الدخول لإضافة تقييمك على المنتج.', 'info');
      navigate({ name: 'account' });
      return;
    }
    if (myComment.trim().length < 2) {
      showToast('يرجى كتابة تعليق لا يقل عن حرفين.', 'error');
      return;
    }
    setSubmittingReview(true);
    try {
      await submitOrUpdateReview({
        productId: product.id,
        productName: product.name,
        userName: userProfile?.fullName || firebaseUser.displayName || 'عميل المتجر',
        rating: myRating,
        comment: myComment,
      });
      setMyComment('');
      showToast('تم حفظ تقييمك بنجاح، شكرًا لمشاركتك!', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ التقييم.'), 'error');
    } finally {
      setSubmittingReview(false);
    }
  };

  return (
    <div className="max-w-[1400px] mx-auto px-3 sm:px-5 lg:px-8 py-5 space-y-8">
      {/* Top Breadcrumb & Actions */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-xs font-bold text-[#717784]">
          <button
            type="button"
            onClick={goBack}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white border border-[#EBECEF] text-[#181A20] hover:bg-[#FFF9DB] transition"
          >
            <ArrowRight className="w-4 h-4" />
            <span>رجوع</span>
          </button>
          {category && (
            <>
              <span className="hidden sm:inline">/</span>
              <button
                type="button"
                onClick={() => navigate({ name: 'categories', initialCategoryId: category.id })}
                className="hidden sm:inline hover:text-[#1565C0] transition"
              >
                {category.name}
              </button>
            </>
          )}
        </div>

        <button
          type="button"
          onClick={() => toggleWishlist(product.id)}
          className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl border text-xs font-extrabold transition ${
            isWishlisted
              ? 'bg-[#FDECEC] border-[#E53935]/40 text-[#E53935]'
              : 'bg-white border-[#EBECEF] text-[#181A20] hover:bg-[#FFF9DB]'
          }`}
        >
          <Heart className={`w-4 h-4 ${isWishlisted ? 'fill-[#E53935] text-[#E53935]' : ''}`} />
          <span>{isWishlisted ? 'في المفضلة' : 'أضف للمفضلة'}</span>
        </button>
      </div>

      {/* World-Class 3-Zone Desktop Product Detail Card */}
      <div className="bg-white border border-[#E8EAEE] rounded-3xl p-4 sm:p-6 lg:p-8 shadow-2xs grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8 items-start">
        {/* Zone 1: Image Gallery (5 cols on lg) */}
        <div className="lg:col-span-5 space-y-3">
          <div className="aspect-square rounded-2xl bg-[#F8F9FB] border border-[#EBECEF] flex items-center justify-center p-5 overflow-hidden">
            {product.imageUrls[activeImageIdx] ? (
              <img
                src={product.imageUrls[activeImageIdx]}
                alt={product.name}
                className="w-full h-full object-contain"
              />
            ) : (
              <Package className="w-20 h-20 text-[#C4C9D2]" />
            )}
          </div>
          {product.imageUrls.length > 1 && (
            <div className="flex gap-2.5 overflow-x-auto pb-1">
              {product.imageUrls.map((url, idx) => (
                <button
                  key={`${url}-${idx}`}
                  type="button"
                  onClick={() => setActiveImageIdx(idx)}
                  className={`w-16 h-16 sm:w-20 sm:h-20 rounded-xl bg-[#F8F9FB] p-1 border-2 overflow-hidden shrink-0 transition ${
                    idx === activeImageIdx
                      ? 'border-[#FFD000] shadow-xs'
                      : 'border-[#EBECEF] hover:border-[#C4C9D2]'
                  }`}
                >
                  <img src={url} alt="" className="w-full h-full object-contain" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Zone 2: Product Identity, Specs & Variants (4 cols on lg) */}
        <div className="lg:col-span-4 space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            {company && (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#FFF9DB] border border-[#FFD000]/60 text-xs font-extrabold text-[#181A20]">
                {company.imageUrl ? (
                  <img
                    src={company.imageUrl}
                    alt={company.name}
                    className="w-4 h-4 rounded-full object-contain bg-white"
                  />
                ) : (
                  <Building2 className="w-3.5 h-3.5 text-[#181A20]" />
                )}
                <span>{company.name}</span>
              </span>
            )}
            {category && (
              <span className="px-2.5 py-1 rounded-full bg-[#F4F5F8] border border-[#EBECEF] text-xs font-bold text-[#5B606E]">
                {category.name}
              </span>
            )}
            <span className="px-2.5 py-1 rounded-full bg-[#F4F5F8] border border-[#EBECEF] text-xs font-bold text-[#717784]">
              SKU: {product.sku}
            </span>
            {flashPrice && (
              <span className="px-2.5 py-1 rounded-full bg-[#E53935] text-white text-xs font-extrabold inline-flex items-center gap-1">
                <Bolt className="w-3.5 h-3.5 fill-current" />
                فلاش سيل
              </span>
            )}
          </div>

          <h1 className="text-xl sm:text-2xl lg:text-[26px] font-black text-[#181A20] leading-snug">
            {product.name}
          </h1>

          {product.reviewCount > 0 && (
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#FFFDF2] border border-[#FFD000]/50 text-xs font-bold text-[#181A20]">
              <Star className="w-4 h-4 fill-[#FFD000] text-[#FFD000]" />
              <span>{formatNumber(product.averageRating)}</span>
              <span className="text-[#717784]">({formatNumber(product.reviewCount)} تقييم)</span>
            </div>
          )}

          {product.description && (
            <div className="pt-2 border-t border-[#F0F1F4] space-y-1.5">
              <div className="text-xs font-black text-[#181A20]">وصف المنتج</div>
              <p className="text-sm text-[#5B606E] leading-relaxed whitespace-pre-line">
                {product.description}
              </p>
            </div>
          )}

          {/* Conditional Color & Size Selectors */}
          {visibleColors.length > 0 && (
            <div className="space-y-2 pt-2 border-t border-[#F0F1F4]">
              <div className="text-xs font-black text-[#181A20]">
                اللون المختار:{' '}
                <span className="text-[#1565C0]">{selectedColor || visibleColors[0]}</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {visibleColors.map((color) => {
                  const active = (selectedColor || visibleColors[0]) === color;
                  return (
                    <button
                      key={color}
                      type="button"
                      onClick={() => setSelectedColor(color)}
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs font-bold transition ${
                        active
                          ? 'bg-[#FFD000] border-[#181A20] text-[#181A20] shadow-xs'
                          : 'bg-[#F9FAFB] border-[#EBECEF] text-[#181A20] hover:border-[#FFD000]'
                      }`}
                    >
                      {active && <Check className="w-3.5 h-3.5" />}
                      <span>{color}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {visibleSizes.length > 0 && (
            <div className="space-y-2 pt-2 border-t border-[#F0F1F4]">
              <div className="text-xs font-black text-[#181A20]">
                المقاس المختار:{' '}
                <span className="text-[#1565C0]">{selectedSize || visibleSizes[0]}</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {visibleSizes.map((size) => {
                  const active = (selectedSize || visibleSizes[0]) === size;
                  return (
                    <button
                      key={size}
                      type="button"
                      onClick={() => setSelectedSize(size)}
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs font-bold transition ${
                        active
                          ? 'bg-[#181A20] border-[#181A20] text-[#FFD000] shadow-xs'
                          : 'bg-[#F9FAFB] border-[#EBECEF] text-[#181A20] hover:border-[#FFD000]'
                      }`}
                    >
                      {active && <Check className="w-3.5 h-3.5" />}
                      <span>{size}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Zone 3: Sticky International E-Commerce Buy Box (3 cols on lg) */}
        <div className="lg:col-span-3 lg:sticky lg:top-32 bg-[#F9FAFB] border border-[#E8EAEE] rounded-2xl p-4 sm:p-5 space-y-4">
          <div className="space-y-1.5">
            <div className="flex items-baseline gap-2.5 flex-wrap">
              <span className="text-2xl sm:text-3xl font-black text-[#181A20]">
                {formatCurrency(unitPrice)}
              </span>
              {discountPct > 0 && !flashPrice && !isWholesaleApplied && (
                <span className="text-sm text-[#8B919E] line-through font-semibold">
                  {formatCurrency(product.retailPrice)}
                </span>
              )}
            </div>

            {discountPct > 0 && !flashPrice && !isWholesaleApplied && (
              <span className="inline-block px-2.5 py-0.5 rounded-full bg-[#E8F6EE] text-[#00965E] text-xs font-extrabold">
                {showcaseDiscountPct > 0
                  ? `عرض الشركة • وفر ${formatNumber(discountPct)}٪`
                  : `وفر ${formatNumber(discountPct)}%`}
              </span>
            )}

            {effectiveWholesaleActive && product.wholesalePrice > 0 && !flashPrice && (
              <div className="text-xs font-bold text-[#1565C0] bg-[#EBF4FF] rounded-xl px-2.5 py-1.5">
                سعر الجملة: {formatCurrency(product.wholesalePrice)} للكمية من{' '}
                {formatNumber(minWholesaleQty)} {product.unit} فأكثر
              </div>
            )}

            <div
              className={`text-xs font-extrabold pt-1 ${
                product.stockQuantity > 0 ? 'text-[#219653]' : 'text-[#E53935]'
              }`}
            >
              {product.stockQuantity > 0
                ? `متوفر بالمخزون (${formatNumber(product.stockQuantity)} ${product.unit})`
                : 'نفدت الكمية حاليًا'}
            </div>
          </div>

          {product.stockQuantity > 0 ? (
            <div className="space-y-3 pt-3 border-t border-[#EBECEF]">
              <div className="flex items-center justify-between">
                <span className="text-xs font-extrabold text-[#181A20]">الكمية:</span>
                <div className="flex items-center gap-2 bg-white border border-[#EBECEF] rounded-xl p-1">
                  <button
                    type="button"
                    onClick={() => setQty((q) => Math.min(product.stockQuantity, q + 1))}
                    className="w-8 h-8 rounded-lg bg-[#FFD000] text-[#181A20] flex items-center justify-center font-bold hover:bg-[#E5B800]"
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                  <span className="min-w-[28px] text-center font-black text-sm">
                    {formatNumber(qty)}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQty((q) => Math.max(1, q - 1))}
                    className="w-8 h-8 rounded-lg bg-[#F9FAFB] border border-[#EBECEF] flex items-center justify-center font-bold hover:bg-gray-100"
                  >
                    <Minus className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <button
                type="button"
                onClick={() =>
                  addToCart(
                    product,
                    qty,
                    activeFlash?.id,
                    visibleColors.length > 0 ? selectedColor || visibleColors[0] : undefined,
                    visibleSizes.length > 0 ? selectedSize || visibleSizes[0] : undefined,
                  )
                }
                className="w-full py-3.5 rounded-xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-black text-sm flex items-center justify-center gap-2 shadow-sm transition"
              >
                <ShoppingCart className="w-5 h-5" />
                <span>إضافة للسلة • {formatCurrency(unitPrice * qty)}</span>
              </button>
            </div>
          ) : (
            <div className="space-y-3 pt-3 border-t border-[#EBECEF]">
              <div
                className={`rounded-xl p-3 border text-xs leading-relaxed ${
                  isStockAlertActive
                    ? 'bg-[#E8F6EE] border-[#219653]/30 text-[#219653]'
                    : 'bg-[#FFF9DB] border-[#FFD000] text-[#181A20]'
                }`}
              >
                <div className="font-black mb-0.5">
                  {isStockAlertActive ? 'تنبيه التوفر مفعّل ✓' : 'نفدت الكمية حاليًا'}
                </div>
                <p className="text-[11px] opacity-90">
                  {isStockAlertActive
                    ? 'سيصلك إشعار فوري بمجرد إعادة توفير المنتج في المخزون.'
                    : 'فعّل الجرس وسننبّهك فور عودة المنتج للمخزون.'}
                </p>
              </div>
              <button
                type="button"
                disabled={togglingAlert}
                onClick={async () => {
                  setTogglingAlert(true);
                  try {
                    await toggleStockAlert(product);
                  } finally {
                    setTogglingAlert(false);
                  }
                }}
                className={`w-full py-3 rounded-xl font-black text-xs sm:text-sm flex items-center justify-center gap-2 shadow-sm transition ${
                  isStockAlertActive
                    ? 'bg-[#E8F6EE] border-2 border-[#219653] text-[#219653] hover:bg-[#DDF2E5]'
                    : 'bg-[#181A20] text-[#FFD000] hover:bg-[#282B33]'
                }`}
              >
                {isStockAlertActive ? (
                  <BellRing className="w-4 h-4" />
                ) : (
                  <Bell className="w-4 h-4" />
                )}
                <span>
                  {togglingAlert
                    ? 'جاري التحديث...'
                    : isStockAlertActive
                    ? 'مفعّل: سننبّهك عند التوفر ✓'
                    : 'نبّهني عند التوفر'}
                </span>
              </button>
            </div>
          )}

          {/* Compact Trust Assurances */}
          <div className="pt-3 border-t border-[#EBECEF] space-y-2 text-[11px] font-semibold text-[#5B606E]">
            <div className="flex items-center gap-2">
              <Truck className="w-4 h-4 text-[#1565C0] shrink-0" />
              <span>
                {storeSettings.freeShippingEnabled && storeSettings.freeShippingThreshold > 0
                  ? `شحن مجاني للطلبات فوق ${formatCurrency(storeSettings.freeShippingThreshold)}`
                  : 'توصيل سريع إلى باب المنزل'}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Store className="w-4 h-4 text-[#219653] shrink-0" />
              <span>متاح الاستلام المباشر من المتجر</span>
            </div>
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-[#181A20] shrink-0" />
              <span>منتجات أصلية مضمونة</span>
            </div>
          </div>
        </div>
      </div>

      {/* Related Products Row */}
      {relatedProducts.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-center gap-2.5 border-b border-[#EBECEF] pb-3">
            <span className="w-1.5 h-6 rounded-full bg-[#FFD000]" />
            <h2 className="text-lg sm:text-xl font-black text-[#181A20]">منتجات مشابهة قد تعجبك</h2>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-2.5 sm:gap-3.5 lg:gap-4">
            {relatedProducts.map((p) => (
              <ProductCard
                key={p.id}
                product={p}
                onOpen={() => navigate({ name: 'product', productId: p.id })}
              />
            ))}
          </div>
        </section>
      )}

      {/* Reviews Section */}
      <section className="bg-white border border-[#E8EAEE] rounded-3xl p-5 sm:p-6 space-y-5 shadow-2xs">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-black text-[#181A20]">
            تقييمات العملاء ({formatNumber(reviews.length)})
          </h2>
        </div>

        <form
          onSubmit={handleSubmitReview}
          className="bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-4 space-y-3"
        >
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="text-xs font-bold">أضف تقييمك للمنتج:</span>
            <div className="flex items-center gap-1">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setMyRating(star)}
                  className="p-1"
                >
                  <Star
                    className={`w-5 h-5 ${
                      star <= myRating ? 'fill-[#FFD000] text-[#FFD000]' : 'text-gray-300'
                    }`}
                  />
                </button>
              ))}
            </div>
          </div>
          <textarea
            value={myComment}
            onChange={(e) => setMyComment(e.target.value)}
            rows={2}
            placeholder="اكتب تجربتك مع المنتج..."
            className="w-full p-3 rounded-xl border border-[#EBECEF] bg-white text-sm focus:border-[#FFD000] focus:outline-none"
          />
          <button
            type="submit"
            disabled={submittingReview}
            className="px-5 py-2 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-bold hover:bg-[#282B33] disabled:opacity-60"
          >
            {submittingReview ? 'جاري النشر...' : 'نشر التقييم'}
          </button>
        </form>

        {reviews.length === 0 ? (
          <p className="text-xs text-[#717784] text-center py-4">
            لا توجد تقييمات بعد — كن أول من يقيّم هذا المنتج!
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {reviews.map((rev) => (
              <div
                key={rev.id}
                className="p-4 rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] space-y-1.5"
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm">{rev.userName}</span>
                  <div className="flex items-center gap-2">
                    <div className="flex items-center gap-0.5">
                      {[1, 2, 3, 4, 5].map((s) => (
                        <Star
                          key={s}
                          className={`w-3.5 h-3.5 ${
                            s <= rev.rating ? 'fill-[#FFD000] text-[#FFD000]' : 'text-gray-300'
                          }`}
                        />
                      ))}
                    </div>
                    {firebaseUser?.uid === rev.userId && (
                      <button
                        type="button"
                        onClick={() => deleteReviewDoc(rev.id)}
                        className="text-[#E53935] hover:underline"
                        title="حذف تقييمي"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
                <p className="text-xs sm:text-sm text-[#181A20]">{rev.comment}</p>
                <div className="text-[11px] text-[#717784]">
                  {formatDateTime(rev.updatedAt || rev.createdAt)}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
