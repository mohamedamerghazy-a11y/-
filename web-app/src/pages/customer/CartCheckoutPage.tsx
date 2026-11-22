import React, { useEffect, useMemo, useState } from 'react';
import {
  CheckCircle2,
  MapPin,
  Minus,
  Navigation,
  Package,
  Plus,
  ShoppingBag,
  Store,
  Tag,
  Trash2,
  Truck,
} from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import {
  formatCurrency,
  formatNumber,
  roundTwo,
  toArabicErrorMessage,
} from '../../lib/formatters';
import {
  calculateCouponDiscount,
  placeOrderTransaction,
  saveDeliveryAddress,
  validateCouponCode,
} from '../../services/firestoreService';
import {
  PAYMENT_METHOD_META,
  computeFreeShipping,
  type Coupon,
  type FulfillmentMethod,
  type PaymentMethod,
} from '../../types/models';
import { ImageUploader } from '../../components/common/ImageUploader';

export const CartCheckoutPage: React.FC = () => {
  const {
    firebaseUser,
    userProfile,
    signInWithGoogle,
    resolvedCartLines,
    cartSubtotal,
    hasCartStockIssues,
    updateCartQtyById,
    removeFromCart,
    addresses,
    deliveryZones,
    paymentSettings,
    storeSettings,
    showToast,
    navigate,
    effectiveWholesaleActive,
  } = useStore();

  const [fulfillment, setFulfillment] = useState<FulfillmentMethod>('delivery');
  const [selectedAddressId, setSelectedAddressId] = useState<string>('');
  const [selectedZoneId, setSelectedZoneId] = useState<string>('');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('cod');
  const [transferRef, setTransferRef] = useState('');
  const [receiptUrls, setReceiptUrls] = useState<string[]>([]);
  const [notes, setNotes] = useState('');
  const [pickupName, setPickupName] = useState('');
  const [pickupPhone, setPickupPhone] = useState('');

  const [couponInput, setCouponInput] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState<Coupon | null>(null);
  const [couponBusy, setCouponBusy] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [completedOrder, setCompletedOrder] = useState<{ orderId: string; orderNumber: string } | null>(null);

  // Quick Address Creation Modal State
  const [showAddAddress, setShowAddAddress] = useState(false);
  const [addrLabel, setAddrLabel] = useState('المنزل');
  const [addrRecipient, setAddrRecipient] = useState('');
  const [addrPhone, setAddrPhone] = useState('');
  const [addrGov, setAddrGov] = useState('');
  const [addrCity, setAddrCity] = useState('');
  const [addrStreet, setAddrStreet] = useState('');
  const [addrLandmark, setAddrLandmark] = useState('');
  const [addrLat, setAddrLat] = useState<number | undefined>();
  const [addrLng, setAddrLng] = useState<number | undefined>();
  const [gpsLoading, setGpsLoading] = useState(false);

  useEffect(() => {
    if (!selectedAddressId && addresses.length > 0) {
      const def = addresses.find((a) => a.isDefault) || addresses[0];
      setSelectedAddressId(def.id);
    }
  }, [addresses, selectedAddressId]);

  useEffect(() => {
    if (userProfile) {
      setPickupName((p) => p || userProfile.fullName);
      setPickupPhone((p) => p || userProfile.phone);
      setAddrRecipient((p) => p || userProfile.fullName);
      setAddrPhone((p) => p || userProfile.phone);
    }
  }, [userProfile]);

  const selectedAddress = addresses.find((a) => a.id === selectedAddressId) || null;

  // Auto-match delivery zone when address governorate changes
  useEffect(() => {
    if (!deliveryZones.length) return;
    if (selectedAddress) {
      const matched = deliveryZones.find(
        (z) =>
          z.governorate.trim().toLowerCase() === selectedAddress.governorate.trim().toLowerCase() ||
          z.name.trim().toLowerCase().includes(selectedAddress.city.trim().toLowerCase()),
      );
      if (matched) {
        setSelectedZoneId(matched.id);
        return;
      }
    }
    if (!selectedZoneId || !deliveryZones.some((z) => z.id === selectedZoneId)) {
      setSelectedZoneId(deliveryZones[0].id);
    }
  }, [selectedAddress, deliveryZones, selectedZoneId]);

  const selectedZone = deliveryZones.find((z) => z.id === selectedZoneId) || null;

  const discountAmount = useMemo(
    () => (appliedCoupon ? calculateCouponDiscount(appliedCoupon, cartSubtotal) : 0),
    [appliedCoupon, cartSubtotal],
  );
  const freeShippingEval = useMemo(
    () =>
      computeFreeShipping({
        netMerchandise: Math.max(0, cartSubtotal - discountAmount),
        isStorePickup: fulfillment === 'pickup',
        hasItems: resolvedCartLines.length > 0,
        baseZoneFee: selectedZone?.fee || 0,
        enabled: storeSettings.freeShippingEnabled,
        threshold: storeSettings.freeShippingThreshold,
      }),
    [
      cartSubtotal,
      discountAmount,
      fulfillment,
      resolvedCartLines.length,
      selectedZone?.fee,
      storeSettings.freeShippingEnabled,
      storeSettings.freeShippingThreshold,
    ],
  );
  const deliveryFee = freeShippingEval.deliveryFee;
  const totalAmount = roundTwo(Math.max(0, cartSubtotal - discountAmount + deliveryFee));

  const availablePayments: PaymentMethod[] = useMemo(() => {
    const list: PaymentMethod[] = [];
    if (paymentSettings.codEnabled) list.push('cod');
    if (paymentSettings.instaPayEnabled) list.push('instapay');
    if (paymentSettings.vodafoneCashEnabled) list.push('vodafone_cash');
    return list.length ? list : ['cod'];
  }, [paymentSettings]);

  useEffect(() => {
    if (!availablePayments.includes(paymentMethod)) {
      setPaymentMethod(availablePayments[0]);
    }
  }, [availablePayments, paymentMethod]);

  const handleApplyCoupon = async () => {
    setCouponBusy(true);
    try {
      const c = await validateCouponCode(couponInput, cartSubtotal);
      setAppliedCoupon(c);
      showToast(`تم تطبيق كود الخصم ${c.code} بنجاح!`, 'success');
    } catch (err) {
      setAppliedCoupon(null);
      showToast(toArabicErrorMessage(err, 'كود الخصم غير صالح.'), 'error');
    } finally {
      setCouponBusy(false);
    }
  };

  const captureBrowserGps = () => {
    if (!navigator.geolocation) {
      showToast('متصفحك لا يدعم تحديد الموقع الجغرافي.', 'error');
      return;
    }
    setGpsLoading(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setAddrLat(pos.coords.latitude);
        setAddrLng(pos.coords.longitude);
        setGpsLoading(false);
        showToast('تم التقاط إحداثيات موقعك بنجاح!', 'success');
      },
      () => {
        setGpsLoading(false);
        showToast('تعذر تحديد موقعك — يرجى السماح بصلاحية الموقع في المتصفح.', 'error');
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  const handleSaveNewAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    if (addrRecipient.trim().length < 2 || addrPhone.trim().length < 6 || addrStreet.trim().length < 4) {
      showToast('يرجى إدخال اسم المستلم ورقم الهاتف وتفاصيل الشارع بشكل صحيح.', 'error');
      return;
    }
    try {
      const newId = await saveDeliveryAddress({
        label: addrLabel.trim() || 'العنوان',
        recipientName: addrRecipient.trim(),
        phone: addrPhone.trim(),
        governorate: addrGov.trim() || 'القاهرة',
        city: addrCity.trim() || 'المدينة',
        streetAddress: addrStreet.trim(),
        landmark: addrLandmark.trim() || undefined,
        latitude: addrLat,
        longitude: addrLng,
        isDefault: addresses.length === 0,
      });
      setSelectedAddressId(newId);
      setShowAddAddress(false);
      showToast('تم حفظ عنوان التوصيل بنجاح', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ العنوان.'), 'error');
    }
  };

  const handlePlaceOrder = async () => {
    if (!firebaseUser) {
      await signInWithGoogle();
      return;
    }
    if (hasCartStockIssues) {
      showToast('يرجى تعديل الكميات التي تتجاوز المخزون المتاح أولًا.', 'error');
      return;
    }
    if (fulfillment === 'delivery' && !selectedAddress) {
      showToast('يرجى إضافة أو اختيار عنوان توصيل أولًا.', 'error');
      return;
    }
    if (PAYMENT_METHOD_META[paymentMethod].requiresTransferReference && !transferRef.trim() && !receiptUrls[0]) {
      showToast('يرجى إدخال الرقم المحوَّل منه أو رفع صورة الإيصال.', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const res = await placeOrderTransaction({
        lines: resolvedCartLines.map((l) => ({
          productId: l.product.id,
          quantity: l.quantity,
          flashSaleId: l.flashSaleId,
          selectedColor: l.selectedColor,
          selectedSize: l.selectedSize,
        })),
        fulfillmentMethod: fulfillment,
        address: selectedAddress,
        deliveryZone: selectedZone,
        couponId: appliedCoupon?.id || null,
        paymentMethod,
        transferReference: transferRef,
        senderPhone: transferRef,
        receiptImageUrl: receiptUrls[0],
        notes,
        pickupRecipientName: pickupName,
        pickupPhone,
        storeAddress: storeSettings.storeAddress,
      });
      setCompletedOrder(res);
      showToast(`تم إرسال طلبك بنجاح! رقم الطلب #${res.orderNumber}`, 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر إتمام الطلب حاليًا.'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (completedOrder) {
    return (
      <div className="max-w-lg mx-auto px-4 py-12 text-center space-y-5">
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-8 space-y-4 shadow-sm">
          <div className="w-16 h-16 mx-auto rounded-full bg-[#E8F6EE] text-[#219653] flex items-center justify-center">
            <CheckCircle2 className="w-9 h-9" />
          </div>
          <h2 className="text-xl font-black text-[#181A20]">تم استلام طلبك بنجاح!</h2>
          <div className="inline-block px-4 py-2 rounded-2xl bg-[#181A20] text-[#FFD000] font-black text-lg">
            #{completedOrder.orderNumber}
          </div>
          <p className="text-xs sm:text-sm text-[#717784]">
            يمكنك متابعة حالة تجهيز وتوصيل طلبك لحظيًا من صفحة طلباتي أو عبر إشعارات المتجر.
          </p>
          <div className="flex flex-wrap gap-2 pt-2 justify-center">
            <button
              type="button"
              onClick={() => navigate({ name: 'orders' })}
              className="px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-bold text-sm"
            >
              متابعة طلباتي
            </button>
            <button
              type="button"
              onClick={() => navigate({ name: 'home' })}
              className="px-5 py-2.5 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-sm font-bold"
            >
              العودة للرئيسية
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (resolvedCartLines.length === 0) {
    return (
      <div className="max-w-xl mx-auto px-4 py-14 text-center space-y-4">
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-8 space-y-4">
          <ShoppingBag className="w-14 h-14 mx-auto text-[#717784]" />
          <h1 className="text-lg font-black">عربة التسوق فارغة</h1>
          <p className="text-xs text-[#717784]">
            تصفح الأقسام والعروض وأضف منتجاتك المفضلة لإتمام الطلب بسهولة.
          </p>
          <button
            type="button"
            onClick={() => navigate({ name: 'home' })}
            className="px-6 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-extrabold text-sm"
          >
            تصفح المتجر
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-3 sm:px-6 py-5 space-y-5">
      <h1 className="text-xl font-black text-[#181A20]">عربة التسوق وإتمام الطلب</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left/Right Main Column: Cart Items + Fulfillment + Payment */}
        <div className="lg:col-span-2 space-y-4">
          {/* Cart Items */}
          <div className="bg-white border border-[#EBECEF] rounded-2xl p-4 space-y-3">
            <h2 className="font-black text-sm text-[#181A20]">
              المنتجات المختارة ({formatNumber(resolvedCartLines.length)})
            </h2>
            {resolvedCartLines.map((line) => (
              <div
                key={line.cartItemId}
                className="flex items-center gap-3 p-3 rounded-2xl bg-[#F9FAFB] border border-[#EBECEF]"
              >
                <div className="w-16 h-16 rounded-xl bg-white border border-[#EBECEF] flex items-center justify-center overflow-hidden shrink-0">
                  {line.product.imageUrls[0] ? (
                    <img src={line.product.imageUrls[0]} alt="" className="w-full h-full object-contain" />
                  ) : (
                    <Package className="w-7 h-7 text-[#C4C9D2]" />
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="font-bold text-sm text-[#181A20] truncate">{line.product.name}</div>
                  {(line.selectedColor || line.selectedSize) && (
                    <div className="flex flex-wrap gap-1.5 mt-1">
                      {line.selectedColor && (
                        <span className="px-2 py-0.5 rounded-full bg-[#FFF9DB] border border-[#FFD000]/60 text-[10px] font-bold text-[#181A20]">
                          اللون: {line.selectedColor}
                        </span>
                      )}
                      {line.selectedSize && (
                        <span className="px-2 py-0.5 rounded-full bg-[#EBF4FF] border border-[#1565C0]/30 text-[10px] font-bold text-[#1565C0]">
                          المقاس: {line.selectedSize}
                        </span>
                      )}
                    </div>
                  )}
                  <div className="text-xs text-[#717784] mt-0.5 flex flex-wrap items-center gap-2">
                    <span>{formatCurrency(line.unitPrice)} / {line.product.unit}</span>
                    {line.appliedPricingMode === 'flash_sale' && (
                      <span className="px-2 py-0.5 rounded-full bg-[#E53935] text-white text-[10px] font-bold">
                        سعر الفلاش سيل
                      </span>
                    )}
                    {line.appliedPricingMode === 'wholesale' && (
                      <span className="px-2 py-0.5 rounded-full bg-[#FFD000] text-[#181A20] text-[10px] font-bold">
                        سعر الجملة
                      </span>
                    )}
                  </div>
                  {effectiveWholesaleActive && line.remainingForWholesale > 0 && line.appliedPricingMode !== 'flash_sale' && (
                    <div className="text-[11px] text-[#1565C0] font-semibold mt-0.5">
                      أضف {formatNumber(line.remainingForWholesale)} إضافية للحصول على سعر الجملة ({formatCurrency(line.product.wholesalePrice)})
                    </div>
                  )}
                  {line.hasStockIssue && (
                    <div className="text-[11px] text-[#E53935] font-bold mt-0.5">
                      الكمية المطلوبة أكبر من المتاح ({formatNumber(line.product.stockQuantity)})
                    </div>
                  )}
                </div>

                <div className="flex flex-col items-end gap-2">
                  <div className="font-black text-sm text-[#181A20]">
                    {formatCurrency(line.lineTotal)}
                  </div>
                  <div className="flex items-center gap-1">
                    <button
                      type="button"
                      onClick={() => updateCartQtyById(line.cartItemId, line.quantity + 1)}
                      className="w-7 h-7 rounded-lg bg-[#FFD000] text-[#181A20] flex items-center justify-center"
                    >
                      <Plus className="w-3.5 h-3.5" />
                    </button>
                    <span className="min-w-[22px] text-center text-xs font-black">
                      {formatNumber(line.quantity)}
                    </span>
                    <button
                      type="button"
                      onClick={() => updateCartQtyById(line.cartItemId, line.quantity - 1)}
                      className="w-7 h-7 rounded-lg bg-white border border-[#EBECEF] flex items-center justify-center"
                    >
                      <Minus className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      aria-label="حذف"
                      onClick={() => removeFromCart(line.cartItemId)}
                      className="w-7 h-7 rounded-lg text-[#E53935] hover:bg-[#FDECEC] flex items-center justify-center mr-1"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {!firebaseUser ? (
            <div className="bg-[#FFF9DB] border border-[#FFD000] rounded-2xl p-5 flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="font-black text-sm text-[#181A20]">سجل الدخول لإتمام الطلب</div>
                <p className="text-xs text-[#717784]">سيتم حفظ جميع المنتجات الموجودة في سلتك تلقائيًا بعد الدخول</p>
              </div>
              <button
                type="button"
                onClick={signInWithGoogle}
                className="px-5 py-2.5 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-black"
              >
                تسجيل الدخول بحساب Google
              </button>
            </div>
          ) : (
            <>
              {/* Fulfillment Method Toggle (Delivery vs Store Pickup) */}
              <div className="bg-white border border-[#EBECEF] rounded-2xl p-4 space-y-3">
                <h2 className="font-black text-sm text-[#181A20]">طريقة استلام الطلب</h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setFulfillment('delivery')}
                    className={`p-3.5 rounded-2xl border-2 text-right flex items-start gap-3 transition ${
                      fulfillment === 'delivery'
                        ? 'border-[#FFD000] bg-[#FFF9DB]'
                        : 'border-[#EBECEF] bg-[#F9FAFB]'
                    }`}
                  >
                    <Truck className="w-5 h-5 text-[#181A20] shrink-0 mt-0.5" />
                    <div>
                      <div className="font-extrabold text-sm">توصيل إلى العنوان</div>
                      <div className="text-[11px] text-[#717784]">يصلك عبر مندوب التوصيل</div>
                    </div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setFulfillment('pickup')}
                    className={`p-3.5 rounded-2xl border-2 text-right flex items-start gap-3 transition ${
                      fulfillment === 'pickup'
                        ? 'border-[#FFD000] bg-[#FFF9DB]'
                        : 'border-[#EBECEF] bg-[#F9FAFB]'
                    }`}
                  >
                    <Store className="w-5 h-5 text-[#181A20] shrink-0 mt-0.5" />
                    <div>
                      <div className="font-extrabold text-sm">الاستلام من المتجر (مجانًا)</div>
                      <div className="text-[11px] text-[#717784]">بدون أي رسوم توصيل</div>
                    </div>
                  </button>
                </div>

                {fulfillment === 'delivery' ? (
                  <div className="space-y-3 pt-2 border-t border-[#EBECEF]">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-[#717784]">عنوان التوصيل:</span>
                      <button
                        type="button"
                        onClick={() => setShowAddAddress(true)}
                        className="text-xs font-extrabold text-[#1565C0] hover:underline"
                      >
                        + إضافة عنوان جديد
                      </button>
                    </div>
                    {addresses.length === 0 ? (
                      <button
                        type="button"
                        onClick={() => setShowAddAddress(true)}
                        className="w-full py-3 rounded-xl border border-dashed border-[#FFD000] bg-[#FFF9DB] text-xs font-bold"
                      >
                        أضف عنوان التوصيل الخاص بك الآن
                      </button>
                    ) : (
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        {addresses.map((addr) => (
                          <button
                            key={addr.id}
                            type="button"
                            onClick={() => setSelectedAddressId(addr.id)}
                            className={`p-3 rounded-xl border text-right text-xs transition ${
                              selectedAddressId === addr.id
                                ? 'border-[#FFD000] bg-[#FFF9DB]'
                                : 'border-[#EBECEF] bg-[#F9FAFB]'
                            }`}
                          >
                            <div className="font-extrabold flex items-center gap-1">
                              <MapPin className="w-3.5 h-3.5 text-[#1565C0]" />
                              <span>{addr.label}</span>
                            </div>
                            <div className="text-[#717784] mt-0.5 line-clamp-1">
                              {addr.governorate} - {addr.city} - {addr.streetAddress}
                            </div>
                          </button>
                        ))}
                      </div>
                    )}

                    {deliveryZones.length > 0 && (
                      <div>
                        <label className="block text-xs font-bold text-[#717784] mb-1">منطقة الشحن:</label>
                        <select
                          value={selectedZoneId}
                          onChange={(e) => setSelectedZoneId(e.target.value)}
                          className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] bg-[#F9FAFB] text-xs font-bold"
                        >
                          {deliveryZones.map((z) => (
                            <option key={z.id} value={z.id}>
                              {z.governorate} - {z.name} ({formatCurrency(z.fee)}) • {z.estimatedDuration}
                            </option>
                          ))}
                        </select>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-[#EBECEF]">
                    <div>
                      <label className="block text-xs font-bold mb-1">اسم المستلم بالفرع</label>
                      <input
                        type="text"
                        value={pickupName}
                        onChange={(e) => setPickupName(e.target.value)}
                        className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold mb-1">رقم هاتف التواصل</label>
                      <input
                        type="tel"
                        value={pickupPhone}
                        onChange={(e) => setPickupPhone(e.target.value)}
                        className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-sm"
                      />
                    </div>
                    {storeSettings.storeAddress && (
                      <div className="sm:col-span-2 text-xs bg-[#F9FAFB] p-3 rounded-xl border border-[#EBECEF]">
                        مقر الاستلام: <span className="font-bold">{storeSettings.storeAddress}</span>
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Payment Method */}
              <div className="bg-white border border-[#EBECEF] rounded-2xl p-4 space-y-3">
                <h2 className="font-black text-sm text-[#181A20]">وسيلة الدفع</h2>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  {availablePayments.map((pm) => {
                    const meta = PAYMENT_METHOD_META[pm];
                    return (
                      <button
                        key={pm}
                        type="button"
                        onClick={() => setPaymentMethod(pm)}
                        className={`p-3 rounded-xl border-2 text-right transition ${
                          paymentMethod === pm
                            ? 'border-[#FFD000] bg-[#FFF9DB]'
                            : 'border-[#EBECEF] bg-[#F9FAFB]'
                        }`}
                      >
                        <div className="font-extrabold text-xs">{meta.label}</div>
                        <div className="text-[11px] text-[#717784] mt-0.5">{meta.subtitle}</div>
                      </button>
                    );
                  })}
                </div>

                {PAYMENT_METHOD_META[paymentMethod].requiresTransferReference && (
                  <div className="bg-[#FFF9DB]/60 border border-[#FFD000] rounded-2xl p-4 space-y-3">
                    {paymentMethod === 'instapay' && paymentSettings.instaPayAddress && (
                      <div className="text-xs font-bold">
                        حساب إنستاباي للتحويل: <span className="text-[#1565C0]" dir="ltr">{paymentSettings.instaPayAddress}</span>
                      </div>
                    )}
                    {paymentMethod === 'vodafone_cash' && paymentSettings.vodafoneCashNumber && (
                      <div className="text-xs font-bold">
                        محفظة فودافون كاش: <span className="text-[#E53935]" dir="ltr">{paymentSettings.vodafoneCashNumber}</span>
                      </div>
                    )}
                    {paymentSettings.paymentInstructions && (
                      <p className="text-xs text-[#717784]">{paymentSettings.paymentInstructions}</p>
                    )}

                    <div className="space-y-1">
                      <label className="block text-xs font-extrabold text-[#181A20]">
                        الرقم المحوَّل منه (للتأكيد التلقائي الفوري أو مع الإيصال)
                      </label>
                      <input
                        type="tel"
                        dir="ltr"
                        value={transferRef}
                        onChange={(e) => setTransferRef(e.target.value)}
                        placeholder="مثال: 010XXXXXXXX"
                        className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] bg-white text-sm text-right"
                      />
                    </div>
                    <ImageUploader
                      folder="aleman/receipts"
                      urls={receiptUrls}
                      label="رفع صورة إيصال التحويل"
                      onChange={setReceiptUrls}
                    />
                  </div>
                )}

                <div>
                  <label className="block text-xs font-bold text-[#717784] mb-1">ملاحظات على الطلب (اختياري)</label>
                  <textarea
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    rows={2}
                    placeholder="أي تعليمات للتجهيز أو التوصيل..."
                    className="w-full p-3 rounded-xl border border-[#EBECEF] bg-[#F9FAFB] text-sm"
                  />
                </div>
              </div>
            </>
          )}
        </div>

        {/* Summary Card */}
        <div className="bg-white border border-[#EBECEF] rounded-2xl p-5 space-y-4 sticky top-20 shadow-xs">
          <h2 className="font-black text-base text-[#181A20]">ملخص الفاتورة</h2>

          {/* Coupon Input */}
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Tag className="w-4 h-4 text-[#717784] absolute right-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={couponInput}
                onChange={(e) => setCouponInput(e.target.value)}
                placeholder="كود الخصم"
                className="w-full h-10 pr-9 pl-2 rounded-xl border border-[#EBECEF] bg-[#F9FAFB] text-xs uppercase font-bold"
              />
            </div>
            <button
              type="button"
              disabled={couponBusy || !couponInput.trim()}
              onClick={handleApplyCoupon}
              className="px-4 h-10 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-bold disabled:opacity-50"
            >
              {couponBusy ? '...' : 'تطبيق'}
            </button>
          </div>

          {freeShippingEval.freeShippingEnabled && fulfillment === 'delivery' && (
            <div
              className={`rounded-2xl p-3 border space-y-2 text-xs ${
                freeShippingEval.qualifiesForFreeShipping
                  ? 'bg-[#E8F6EE] border-[#219653]/30 text-[#219653]'
                  : 'bg-[#FFF9DB] border-[#FFD000] text-[#181A20]'
              }`}
            >
              <div className="flex items-center gap-2 font-black">
                <Truck className="w-4 h-4 shrink-0" />
                <span>
                  {freeShippingEval.qualifiesForFreeShipping
                    ? 'مبروك! طلبك مؤهل الآن للشحن المجاني 🎉'
                    : `أضف منتجات بقيمة ${formatCurrency(freeShippingEval.remainingForFreeShipping)} للحصول على شحن مجاني`}
                </span>
              </div>
              <div className="h-2 w-full rounded-full bg-white/80 overflow-hidden border border-black/5">
                <div
                  className={`h-full transition-all duration-300 ${
                    freeShippingEval.qualifiesForFreeShipping ? 'bg-[#219653]' : 'bg-[#FFD000]'
                  }`}
                  style={{ width: `${Math.round(freeShippingEval.freeShippingProgress * 100)}%` }}
                />
              </div>
            </div>
          )}

          <div className="space-y-2 text-sm border-t border-[#EBECEF] pt-3">
            <div className="flex justify-between">
              <span className="text-[#717784]">المجموع الفرعي</span>
              <span className="font-bold">{formatCurrency(cartSubtotal)}</span>
            </div>
            {discountAmount > 0 && (
              <div className="flex justify-between text-[#E53935]">
                <span>الخصم ({appliedCoupon?.code})</span>
                <span className="font-bold">- {formatCurrency(discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-[#717784]">
                {fulfillment === 'pickup' ? 'الاستلام من المتجر' : 'رسوم التوصيل'}
              </span>
              <span className="font-bold flex items-center gap-1.5">
                {fulfillment === 'pickup' ? (
                  'مجانًا'
                ) : freeShippingEval.qualifiesForFreeShipping ? (
                  <>
                    {(selectedZone?.fee || 0) > 0 && (
                      <span className="text-xs text-[#717784] line-through">
                        {formatCurrency(selectedZone?.fee || 0)}
                      </span>
                    )}
                    <span className="text-[#219653] font-black">مجانًا</span>
                  </>
                ) : (
                  formatCurrency(deliveryFee)
                )}
              </span>
            </div>
            <div className="flex justify-between items-center border-t border-[#EBECEF] pt-3 text-base font-black">
              <span>الإجمالي النهائي</span>
              <span className="text-lg text-[#181A20]">{formatCurrency(totalAmount)}</span>
            </div>
          </div>

          <button
            type="button"
            disabled={submitting || hasCartStockIssues}
            onClick={handlePlaceOrder}
            className="w-full py-3.5 rounded-2xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-black text-sm shadow-sm disabled:opacity-50 transition"
          >
            {submitting
              ? 'جاري تأكيد الطلب...'
              : firebaseUser
              ? 'تأكيد وإرسال الطلب'
              : 'سجل الدخول لإتمام الطلب'}
          </button>
        </div>
      </div>

      {/* Add Address Modal */}
      {showAddAddress && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form
            onSubmit={handleSaveNewAddress}
            className="bg-white rounded-3xl max-w-lg w-full p-5 space-y-3 border border-[#EBECEF] max-h-[90vh] overflow-y-auto"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-black text-base">إضافة عنوان توصيل جديد</h3>
              <button type="button" onClick={() => setShowAddAddress(false)} className="text-xs font-bold text-[#717784]">
                إغلاق
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2.5 text-xs">
              <input
                value={addrLabel}
                onChange={(e) => setAddrLabel(e.target.value)}
                placeholder="اسم العنوان (المنزل، العمل...)"
                className="h-10 px-3 rounded-xl border border-[#EBECEF]"
                required
              />
              <input
                value={addrRecipient}
                onChange={(e) => setAddrRecipient(e.target.value)}
                placeholder="اسم المستلم"
                className="h-10 px-3 rounded-xl border border-[#EBECEF]"
                required
              />
              <input
                value={addrPhone}
                onChange={(e) => setAddrPhone(e.target.value)}
                placeholder="رقم الهاتف"
                className="h-10 px-3 rounded-xl border border-[#EBECEF]"
                required
              />
              <input
                value={addrGov}
                onChange={(e) => setAddrGov(e.target.value)}
                placeholder="المحافظة"
                className="h-10 px-3 rounded-xl border border-[#EBECEF]"
                required
              />
              <input
                value={addrCity}
                onChange={(e) => setAddrCity(e.target.value)}
                placeholder="المدينة / المركز"
                className="h-10 px-3 rounded-xl border border-[#EBECEF]"
                required
              />
              <input
                value={addrLandmark}
                onChange={(e) => setAddrLandmark(e.target.value)}
                placeholder="علامة مميزة (اختياري)"
                className="h-10 px-3 rounded-xl border border-[#EBECEF]"
              />
            </div>

            <input
              value={addrStreet}
              onChange={(e) => setAddrStreet(e.target.value)}
              placeholder="العنوان بالتفصيل (الشارع، رقم العقار، الدور)"
              className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs"
              required
            />

            <button
              type="button"
              onClick={captureBrowserGps}
              className="w-full py-2.5 rounded-xl bg-[#EBF4FF] text-[#1565C0] font-bold text-xs flex items-center justify-center gap-2"
            >
              <Navigation className="w-4 h-4" />
              <span>
                {gpsLoading
                  ? 'جاري تحديد الموقع...'
                  : addrLat != null
                  ? `تم التقاط الإحداثيات (${addrLat.toFixed(4)}, ${addrLng?.toFixed(4)})`
                  : 'تحديد موقعي الحالي (GPS) لتسهيل وصول المندوب'}
              </span>
            </button>

            <button
              type="submit"
              className="w-full py-3 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
            >
              حفظ العنوان
            </button>
          </form>
        </div>
      )}
    </div>
  );
};
