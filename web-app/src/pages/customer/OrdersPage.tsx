import React, { useState } from 'react';
import { Check, KeyRound, Package, RotateCcw, ShieldCheck, Store, Truck, X } from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import {
  activeTimelineStepsFor,
  displaySenderPhone,
  hasPendingDeliveryOtp,
  orderStatusLabelFor,
  PAYMENT_METHOD_META,
  PAYMENT_STATUS_LABELS,
  type Order,
} from '../../types/models';
import { formatCurrency, formatDateTime, formatDigits, formatNumber, toArabicErrorMessage } from '../../lib/formatters';
import { cancelOrderByCustomer } from '../../services/firestoreService';

export const OrdersPage: React.FC = () => {
  const { firebaseUser, userOrders, signInWithGoogle, reorderItemsToCart, showToast } = useStore();
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [cancelling, setCancelling] = useState(false);

  if (!firebaseUser) {
    return (
      <div className="max-w-lg mx-auto px-4 py-14 text-center space-y-4">
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-8 space-y-4">
          <Package className="w-14 h-14 mx-auto text-[#717784]" />
          <h1 className="text-lg font-black">سجل الدخول لعرض طلباتك</h1>
          <button
            type="button"
            onClick={signInWithGoogle}
            className="px-6 py-3 rounded-xl bg-[#181A20] text-[#FFD000] font-black text-sm"
          >
            تسجيل الدخول بحساب Google
          </button>
        </div>
      </div>
    );
  }

  const handleCancel = async (order: Order) => {
    setCancelling(true);
    try {
      await cancelOrderByCustomer(order.id);
      showToast(`تم إلغاء الطلب #${order.orderNumber}`, 'info');
      setSelectedOrder(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر إلغاء الطلب.'), 'error');
    } finally {
      setCancelling(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-3 sm:px-6 py-5 space-y-4">
      <h1 className="text-xl font-black text-[#181A20]">
        طلباتي ({formatNumber(userOrders.length)})
      </h1>

      {userOrders.length === 0 ? (
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-10 text-center text-sm text-[#717784]">
          لم تقم بإرسال أي طلبات بعد.
        </div>
      ) : (
        <div className="space-y-3">
          {userOrders.map((order) => {
            const isPickup = order.fulfillmentType === 'pickup';
            return (
              <div
                key={order.id}
                className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex flex-wrap items-center justify-between gap-4 hover:border-[#FFD000] transition"
              >
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-lg bg-[#181A20] text-[#FFD000] text-xs font-black">
                      #{formatDigits(order.orderNumber || order.id.slice(0, 8))}
                    </span>
                    <span className="px-2.5 py-0.5 rounded-full bg-[#FFF9DB] text-[#181A20] text-xs font-bold">
                      {orderStatusLabelFor(order.orderStatus, isPickup)}
                    </span>
                    <span className="inline-flex items-center gap-1 text-xs text-[#717784]">
                      {isPickup ? <Store className="w-3.5 h-3.5" /> : <Truck className="w-3.5 h-3.5" />}
                      {isPickup ? 'استلام من المتجر' : 'توصيل'}
                    </span>
                  </div>
                  <div className="text-xs text-[#717784]">
                    {formatDateTime(order.createdAt)} • {formatNumber(order.items.length)} صنف
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="text-left">
                    <div className="font-black text-base text-[#181A20]">
                      {formatCurrency(order.totalAmount)}
                    </div>
                    <div className="text-[11px] text-[#717784]">
                      {PAYMENT_STATUS_LABELS[order.paymentStatus]}
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setSelectedOrder(order)}
                    className="px-4 py-2 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-extrabold hover:bg-[#E5B800]"
                  >
                    التفاصيل
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {selectedOrder && (
        <div className="fixed inset-0 z-50 bg-black/55 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden border border-[#EBECEF]">
            <div className="bg-[#181A20] text-white px-5 py-4 flex items-center justify-between">
              <div>
                <h3 className="font-black text-base text-[#FFD000]">
                  تفاصيل الطلب #{formatDigits(selectedOrder.orderNumber)}
                </h3>
                <p className="text-xs text-white/75">{formatDateTime(selectedOrder.createdAt)}</p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedOrder(null)}
                className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto space-y-5">
              {/* Order Timeline */}
              {selectedOrder.orderStatus !== 'cancelled' && selectedOrder.orderStatus !== 'returned' ? (
                <div className="bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-4 space-y-2">
                  <div className="text-xs font-black text-[#181A20]">تتبع حالة الطلب</div>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                    {activeTimelineStepsFor(selectedOrder.fulfillmentType === 'pickup').map((step) => {
                      const steps = activeTimelineStepsFor(selectedOrder.fulfillmentType === 'pickup');
                      const currentIdx = steps.indexOf(selectedOrder.orderStatus);
                      const stepIdx = steps.indexOf(step);
                      const reached = stepIdx <= currentIdx;
                      return (
                        <div
                          key={step}
                          className={`p-2 rounded-xl border text-xs font-bold flex items-center gap-1.5 ${
                            reached
                              ? 'bg-[#E8F6EE] border-[#219653]/40 text-[#219653]'
                              : 'bg-white border-[#EBECEF] text-[#717784]'
                          }`}
                        >
                          <Check className="w-3.5 h-3.5 shrink-0" />
                          <span>{orderStatusLabelFor(step, selectedOrder.fulfillmentType === 'pickup')}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : (
                <div className="bg-[#FDECEC] border border-[#E53935]/30 rounded-2xl p-3 text-xs font-bold text-[#E53935]">
                  حالة الطلب: {orderStatusLabelFor(selectedOrder.orderStatus, selectedOrder.fulfillmentType === 'pickup')}
                  {selectedOrder.statusNote ? ` — ${selectedOrder.statusNote}` : ''}
                </div>
              )}

              {/* Delivery Confirmation OTP */}
              {hasPendingDeliveryOtp(selectedOrder) && selectedOrder.deliveryOtp && (
                <div className="bg-[#181A20] text-white border border-[#FFD000] rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
                  <div className="space-y-1 max-w-sm">
                    <div className="inline-flex items-center gap-1.5 text-xs font-black text-[#FFD000]">
                      <KeyRound className="w-4 h-4" />
                      <span>كود تأكيد الاستلام مع المندوب</span>
                    </div>
                    <p className="text-[11px] text-white/80 leading-relaxed">
                      شارك هذا الكود مع مندوب التوصيل فقط عند استلام الطلب لإثبات التسليم.
                    </p>
                  </div>
                  <div
                    dir="ltr"
                    className="px-4 py-2 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-2xl tracking-[0.35em] select-all"
                  >
                    {formatDigits(selectedOrder.deliveryOtp)}
                  </div>
                </div>
              )}

              {selectedOrder.deliveryOtpVerified && selectedOrder.fulfillmentType !== 'pickup' && (
                <div className="bg-[#E8F6EE] border border-[#219653]/30 rounded-2xl p-3 text-xs font-black text-[#219653] flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 shrink-0" />
                  <span>تم تأكيد الاستلام بكود التحقق بنجاح ✓</span>
                </div>
              )}

              {/* Items */}
              <div className="space-y-2">
                {selectedOrder.items.map((it, i) => (
                  <div key={`${it.productId}-${i}`} className="flex justify-between text-xs border-b border-[#EBECEF] pb-2">
                    <div>
                      <div className="font-bold text-[#181A20]">
                        {it.productName}
                        {(it.selectedColor || it.selectedSize) && (
                          <span className="text-[11px] text-[#1565C0] font-extrabold mr-1.5">
                            ({[it.selectedColor && `اللون: ${it.selectedColor}`, it.selectedSize && `المقاس: ${it.selectedSize}`].filter(Boolean).join(' • ')})
                          </span>
                        )}
                      </div>
                      <div className="text-[#717784]">
                        {formatNumber(it.quantity)} {it.unit} × {formatCurrency(it.unitPrice)}
                      </div>
                    </div>
                    <div className="font-black">{formatCurrency(it.lineTotal)}</div>
                  </div>
                ))}
              </div>

              {/* Delivery & Payment */}
              <div className="bg-[#F9FAFB] rounded-2xl p-3.5 text-xs space-y-1">
                <div>
                  <span className="font-bold">طريقة الاستلام:</span>{' '}
                  {selectedOrder.fulfillmentType === 'pickup' ? 'الاستلام من المتجر' : 'توصيل إلى العنوان'}
                </div>
                <div>
                  <span className="font-bold">العنوان / الفرع:</span> {selectedOrder.governorate} - {selectedOrder.city} - {selectedOrder.streetAddress}
                </div>
                <div>
                  <span className="font-bold">الدفع:</span> {PAYMENT_METHOD_META[selectedOrder.paymentMethod]?.label} ({PAYMENT_STATUS_LABELS[selectedOrder.paymentStatus]})
                </div>
                {displaySenderPhone(selectedOrder) && (
                  <div>
                    <span className="font-bold">الرقم المحوَّل منه:</span>{' '}
                    <span dir="ltr">{formatDigits(displaySenderPhone(selectedOrder))}</span>
                  </div>
                )}
                {selectedOrder.assignedDriverName && (
                  <div>
                    <span className="font-bold">المندوب:</span> {selectedOrder.assignedDriverName} {selectedOrder.assignedDriverPhone ? `(${selectedOrder.assignedDriverPhone})` : ''}
                  </div>
                )}
              </div>

              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => {
                    reorderItemsToCart(selectedOrder.items);
                    setSelectedOrder(null);
                  }}
                  className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-extrabold"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>إعادة طلب نفس المنتجات</span>
                </button>

                {(selectedOrder.orderStatus === 'new_order' || selectedOrder.orderStatus === 'under_review') && (
                  <button
                    type="button"
                    disabled={cancelling}
                    onClick={() => handleCancel(selectedOrder)}
                    className="px-4 py-2.5 rounded-xl bg-[#FDECEC] text-[#E53935] text-xs font-extrabold"
                  >
                    {cancelling ? 'جاري الإلغاء...' : 'إلغاء الطلب'}
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
