import React, { useMemo, useState } from 'react';
import {
  CheckCircle2,
  KeyRound,
  MapPin,
  MessageCircle,
  Phone,
  Plus,
  Printer,
  Trash2,
  Truck,
} from 'lucide-react';
import {
  displaySenderPhone,
  hasPendingDeliveryOtp,
  normalizeOtpDigits,
  ORDER_STATUS_META,
  orderStatusLabelFor,
  type Coupon,
  type DeliveryZone,
  type Order,
  type OrderStatus,
  type StaffMember,
} from '../../types/models';
import { useStore } from '../../context/StoreContext';
import {
  assignOrderToDriverAdmin,
  deleteCouponDoc,
  deleteDeliveryZoneDoc,
  markOrderDeliveredByDriver,
  saveCouponDoc,
  saveDeliveryZoneDoc,
  sendNotificationToUser,
  updateOrderStatusAdmin,
} from '../../services/firestoreService';
import {
  formatCurrency,
  formatDateTime,
  formatDigits,
  formatNumber,
  normalizeWhatsAppPhone,
  toArabicErrorMessage,
} from '../../lib/formatters';
import { ThermalPrintModal } from '../../components/common/ThermalPrintModal';

// 1. Orders Management (non-wrapping clean buttons + ET-1000+ + Thermal Print & WhatsApp)
export const AdminOrdersTab: React.FC<{
  orders: Order[];
  staffList: StaffMember[];
}> = ({ orders, staffList }) => {
  const { showToast } = useStore();
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [printOrder, setPrintOrder] = useState<Order | null>(null);
  const [assigningOrder, setAssigningOrder] = useState<Order | null>(null);
  const [selectedDriverUid, setSelectedDriverUid] = useState('');

  const drivers = useMemo(
    () =>
      staffList.filter(
        (s) => s.isActive && (s.role === 'delivery_driver' || s.permissions.includes('deliver_orders')),
      ),
    [staffList],
  );

  const filtered = useMemo(() => {
    if (statusFilter === 'all') return orders;
    return orders.filter((o) => o.orderStatus === statusFilter);
  }, [orders, statusFilter]);

  const changeStatus = async (order: Order, nextStatus: OrderStatus) => {
    try {
      await updateOrderStatusAdmin({ orderId: order.id, newStatus: nextStatus });
      const label = orderStatusLabelFor(nextStatus, order.fulfillmentType === 'pickup');
      await sendNotificationToUser({
        targetUid: order.userId,
        title: `تحديث حالة الطلب #${order.orderNumber}`,
        message: `أصبحت حالة طلبك الآن: ${label}`,
        type: 'order_status',
        referenceId: order.id,
      }).catch(() => {});
      showToast(`تم تغيير الحالة إلى: ${label}`, 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر تحديث حالة الطلب.'), 'error');
    }
  };

  const handleAssignDriver = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assigningOrder || !selectedDriverUid) return;
    const drv = drivers.find((d) => d.uid === selectedDriverUid);
    if (!drv) return;
    try {
      await assignOrderToDriverAdmin({
        orderId: assigningOrder.id,
        driverId: drv.uid,
        driverName: drv.fullName,
      });
      await sendNotificationToUser({
        targetUid: assigningOrder.userId,
        title: `الطلب #${assigningOrder.orderNumber} في طريقه إليك`,
        message: `تم توجيه طلبك مع المندوب ${drv.fullName}.`,
        type: 'order_status',
        referenceId: assigningOrder.id,
      }).catch(() => {});
      showToast('تم تعيين المندوب وتحديث حالة الطلب', 'success');
      setAssigningOrder(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر تعيين المندوب.'), 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-1.5">
        <button
          type="button"
          onClick={() => setStatusFilter('all')}
          className={`whitespace-nowrap px-3.5 py-2 rounded-xl text-xs font-extrabold border ${
            statusFilter === 'all' ? 'bg-[#181A20] text-[#FFD000] border-[#181A20]' : 'bg-white border-[#EBECEF]'
          }`}
        >
          الكل ({formatNumber(orders.length)})
        </button>
        {(Object.keys(ORDER_STATUS_META) as OrderStatus[]).map((st) => (
          <button
            key={st}
            type="button"
            onClick={() => setStatusFilter(st)}
            className={`whitespace-nowrap px-3.5 py-2 rounded-xl text-xs font-bold border ${
              statusFilter === st ? 'bg-[#FFD000] text-[#181A20] border-[#FFD000]' : 'bg-white border-[#EBECEF]'
            }`}
          >
            {ORDER_STATUS_META[st].label}
          </button>
        ))}
      </div>

      <div className="space-y-3">
        {filtered.map((order) => {
          const isPickup = order.fulfillmentType === 'pickup';
          const allowedSteps: OrderStatus[] = [
            'under_review',
            'confirmed',
            'preparing',
            'ready_for_delivery',
            ...(isPickup ? [] : (['out_for_delivery'] as OrderStatus[])),
            'delivered',
            'cancelled',
          ];

          return (
            <div key={order.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-lg bg-[#181A20] text-[#FFD000] font-black text-xs">
                      #{formatDigits(order.orderNumber)}
                    </span>
                    <span className="font-bold text-sm">{order.customerName}</span>
                    <span className="px-2.5 py-0.5 rounded-full bg-[#FFF9DB] text-xs font-bold">
                      {orderStatusLabelFor(order.orderStatus, isPickup)}
                    </span>
                    <span className="text-xs font-semibold text-[#717784]">
                      {isPickup ? 'استلام من المتجر' : 'توصيل'}
                    </span>
                  </div>
                  <div className="text-xs text-[#717784]">
                    {formatDateTime(order.createdAt)} • {formatDigits(order.phone)} •{' '}
                    {formatCurrency(order.totalAmount)} ({formatNumber(order.items.length)} أصناف)
                    {displaySenderPhone(order) && (
                      <span className="text-[#1565C0] font-bold">
                        {' '}• الرقم المحوَّل منه: <span dir="ltr">{formatDigits(displaySenderPhone(order))}</span>
                      </span>
                    )}
                  </div>
                  {order.items.some((it) => it.selectedColor || it.selectedSize) && (
                    <div className="flex flex-wrap gap-1 pt-0.5">
                      {order.items.map((it, idx) =>
                        it.selectedColor || it.selectedSize ? (
                          <span
                            key={`${it.productId}-${idx}`}
                            className="px-2 py-0.5 rounded-full bg-[#FFF9DB] border border-[#FFD000]/50 text-[10px] font-bold text-[#181A20]"
                          >
                            {it.productName}:{' '}
                            {[it.selectedColor && `لون ${it.selectedColor}`, it.selectedSize && `مقاس ${it.selectedSize}`]
                              .filter(Boolean)
                              .join(' • ')}
                          </span>
                        ) : null,
                      )}
                    </div>
                  )}
                </div>

                {/* Organized non-wrapping buttons */}
                <div className="flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setPrintOrder(order)}
                    className="whitespace-nowrap inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-extrabold hover:bg-[#282B33]"
                  >
                    <Printer className="w-4 h-4 shrink-0" />
                    <span>طباعة / واتساب</span>
                  </button>

                  {!isPickup && (
                    <button
                      type="button"
                      onClick={() => {
                        setAssigningOrder(order);
                        setSelectedDriverUid(drivers[0]?.uid || '');
                      }}
                      className="whitespace-nowrap inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#EBF4FF] text-[#1565C0] text-xs font-extrabold"
                    >
                      <Truck className="w-4 h-4 shrink-0" />
                      <span>توجيه لمندوب</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Clean Status Progression Buttons (each fits horizontally cleanly) */}
              <div className="flex flex-wrap gap-1.5 pt-2 border-t border-[#EBECEF]">
                {allowedSteps.map((st) => {
                  const isCurrent = order.orderStatus === st;
                  return (
                    <button
                      key={st}
                      type="button"
                      disabled={isCurrent}
                      onClick={() => changeStatus(order, st)}
                      className={`whitespace-nowrap px-3 py-1.5 rounded-xl text-xs font-bold border transition ${
                        isCurrent
                          ? 'bg-[#219653] text-white border-[#219653]'
                          : 'bg-[#F9FAFB] border-[#EBECEF] hover:border-[#FFD000]'
                      }`}
                    >
                      {orderStatusLabelFor(st, isPickup)}
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      {printOrder && <ThermalPrintModal order={printOrder} onClose={() => setPrintOrder(null)} />}

      {assigningOrder && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleAssignDriver} className="bg-white rounded-3xl max-w-md w-full p-5 space-y-4">
            <h3 className="font-black text-base">
              تعيين مندوب توصيل للطلب #{formatDigits(assigningOrder.orderNumber)}
            </h3>
            {drivers.length === 0 ? (
              <p className="text-xs text-[#E53935]">
                لا يوجد موظفون بدور "موظف توصيل" أو صلاحية توصيل الطلبات حاليًا.
              </p>
            ) : (
              <select
                value={selectedDriverUid}
                onChange={(e) => setSelectedDriverUid(e.target.value)}
                className="w-full h-11 px-3 rounded-xl border border-[#EBECEF] text-sm"
                required
              >
                {drivers.map((d) => (
                  <option key={d.uid} value={d.uid}>
                    {d.fullName} ({d.email})
                  </option>
                ))}
              </select>
            )}
            <div className="flex gap-2">
              {drivers.length > 0 && (
                <button type="submit" className="flex-1 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm">
                  تأكيد التوجيه
                </button>
              )}
              <button type="button" onClick={() => setAssigningOrder(null)} className="px-4 py-2.5 rounded-xl border border-[#EBECEF] text-xs font-bold">
                إلغاء
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

// 2. Delivery Driver Portal
export const AdminDriverTab: React.FC<{ driverOrders: Order[] }> = ({ driverOrders }) => {
  const { showToast } = useStore();
  const [confirmingOrder, setConfirmingOrder] = useState<Order | null>(null);
  const [otpInput, setOtpInput] = useState('');
  const [noteInput, setNoteInput] = useState('');
  const [otpError, setOtpError] = useState('');
  const [submittingDelivery, setSubmittingDelivery] = useState(false);

  const openDeliveryConfirm = (order: Order) => {
    setConfirmingOrder(order);
    setOtpInput('');
    setNoteInput('');
    setOtpError('');
  };

  const submitConfirmDelivered = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!confirmingOrder) return;
    const expectedOtp = confirmingOrder.deliveryOtp?.trim() || '';
    const requiresOtp = expectedOtp.length > 0;
    if (requiresOtp) {
      const cleanEntered = normalizeOtpDigits(otpInput);
      const cleanExpected = normalizeOtpDigits(expectedOtp);
      if (cleanEntered.length !== 4) {
        setOtpError('أدخل كود التحقق المكون من ٤ أرقام.');
        return;
      }
      if (cleanEntered !== cleanExpected) {
        setOtpError('كود تأكيد الاستلام غير صحيح — تأكد من الرقم لدى العميل.');
        return;
      }
    }

    setSubmittingDelivery(true);
    try {
      await markOrderDeliveredByDriver(
        confirmingOrder.id,
        noteInput.trim() || undefined,
        requiresOtp,
      );
      await sendNotificationToUser({
        targetUid: confirmingOrder.userId,
        title: `تم تسليم الطلب #${confirmingOrder.orderNumber}`,
        message: 'شكرًا لتسوقك من الإيمان للتجارة.',
        type: 'order_status',
        referenceId: confirmingOrder.id,
      }).catch(() => {});
      showToast('تم تأكيد تسليم الطلب بنجاح', 'success');
      setConfirmingOrder(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر تحديث الحالة.'), 'error');
    } finally {
      setSubmittingDelivery(false);
    }
  };

  return (
    <div className="space-y-3">
      {driverOrders.length === 0 ? (
        <div className="bg-white border border-[#EBECEF] rounded-2xl p-8 text-center text-sm text-[#717784]">
          لا توجد طلبات مسندة إليك حاليًا.
        </div>
      ) : (
        driverOrders.map((o) => {
          const needsOtp = hasPendingDeliveryOtp(o);
          return (
            <div key={o.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="font-black text-sm">
                  #{formatDigits(o.orderNumber)} — {o.recipientName}
                </div>
                <div className="flex flex-wrap items-center gap-1.5">
                  {needsOtp && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-[#181A20] text-[#FFD000] text-[11px] font-extrabold">
                      <KeyRound className="w-3 h-3" />
                      يتطلب كود تأكيد الاستلام
                    </span>
                  )}
                  <span className="px-2.5 py-0.5 rounded-full bg-[#FFF9DB] text-xs font-bold">
                    {orderStatusLabelFor(o.orderStatus, false)}
                  </span>
                </div>
              </div>
              <div className="text-xs text-[#717784]">
                {o.governorate} - {o.city} - {o.streetAddress} • المطلوب تحصيله:{' '}
                <span className="font-black text-[#181A20]">
                  {o.paymentMethod === 'cod' ? formatCurrency(o.totalAmount) : 'مدفوع إلكترونيًا'}
                </span>
              </div>
              <div className="flex flex-wrap gap-2">
                {o.phone && (
                  <>
                    <a
                      href={`tel:${o.phone}`}
                      className="inline-flex items-center gap-1 px-3 py-2 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold"
                    >
                      <Phone className="w-3.5 h-3.5" />
                      <span>اتصال</span>
                    </a>
                    <a
                      href={`https://wa.me/${normalizeWhatsAppPhone(o.phone)}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 px-3 py-2 rounded-xl bg-[#E8F6EE] text-[#219653] text-xs font-bold"
                    >
                      <MessageCircle className="w-3.5 h-3.5" />
                      <span>واتساب</span>
                    </a>
                  </>
                )}
                {o.latitude != null && o.longitude != null && (
                  <a
                    href={`https://www.google.com/maps?q=${o.latitude},${o.longitude}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 px-3 py-2 rounded-xl bg-[#EBF4FF] text-[#1565C0] text-xs font-bold"
                  >
                    <MapPin className="w-3.5 h-3.5" />
                    <span>خريطة الموقع</span>
                  </a>
                )}
                {o.orderStatus !== 'delivered' && (
                  <button
                    type="button"
                    onClick={() => openDeliveryConfirm(o)}
                    className="inline-flex items-center gap-1 px-4 py-2 rounded-xl bg-[#219653] text-white text-xs font-black mr-auto"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>تأكيد التسليم</span>
                  </button>
                )}
              </div>
            </div>
          );
        })
      )}

      {confirmingOrder && (
        <div className="fixed inset-0 z-50 bg-black/55 flex items-center justify-center p-4">
          <form
            onSubmit={submitConfirmDelivered}
            className="bg-white rounded-3xl max-w-md w-full p-5 space-y-4 border border-[#EBECEF]"
          >
            <div className="space-y-1">
              <h3 className="font-black text-base text-[#181A20]">
                تأكيد تسليم الطلب #{formatDigits(confirmingOrder.orderNumber)}
              </h3>
              <p className="text-xs text-[#717784]">
                المستلم: {confirmingOrder.recipientName}
              </p>
            </div>

            {confirmingOrder.deliveryOtp?.trim() && (
              <div className="space-y-1.5 bg-[#FFF9DB]/60 border border-[#FFD000] rounded-2xl p-3.5">
                <label className="flex items-center gap-1.5 text-xs font-black text-[#181A20]">
                  <KeyRound className="w-4 h-4" />
                  <span>كود تأكيد الاستلام من العميل (٤ أرقام)</span>
                </label>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={8}
                  dir="ltr"
                  value={otpInput}
                  onChange={(e) => {
                    setOtpInput(e.target.value);
                    if (otpError) setOtpError('');
                  }}
                  placeholder="أدخل ٤ أرقام من هاتف العميل"
                  className="w-full h-11 px-3 rounded-xl border border-[#EBECEF] bg-white text-center font-black text-lg tracking-[0.35em]"
                  autoFocus
                />
                {otpError && (
                  <p className="text-[11px] font-bold text-[#E53935]">{otpError}</p>
                )}
              </div>
            )}

            <div>
              <label className="block text-xs font-bold text-[#717784] mb-1">
                ملاحظة التسليم (اختياري)
              </label>
              <input
                type="text"
                value={noteInput}
                onChange={(e) => setNoteInput(e.target.value)}
                placeholder="تم الاستلام وتحصيل المبلغ..."
                className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs"
              />
            </div>

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={submittingDelivery}
                className="flex-1 py-2.5 rounded-xl bg-[#219653] text-white font-black text-sm disabled:opacity-60"
              >
                {submittingDelivery ? 'جاري التأكيد...' : 'تأكيد التسليم الآن'}
              </button>
              <button
                type="button"
                onClick={() => setConfirmingOrder(null)}
                className="px-4 py-2.5 rounded-xl border border-[#EBECEF] text-xs font-bold"
              >
                إلغاء
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

// 3. Coupons & Delivery Zones
export const AdminCouponsTab: React.FC<{ coupons: Coupon[] }> = ({ coupons }) => {
  const { showToast } = useStore();
  const [editing, setEditing] = useState<Partial<Coupon> | null>(null);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editing?.code?.trim()) return;
    try {
      await saveCouponDoc(editing);
      showToast('تم حفظ الكوبون', 'success');
      setEditing(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ الكوبون.'), 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button
          type="button"
          onClick={() =>
            setEditing({ code: '', discountType: 'percent', discountValue: 10, minOrderAmount: 0, isActive: true, usedCount: 0 })
          }
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
        >
          <Plus className="w-4 h-4" />
          <span>إضافة كود خصم</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {coupons.map((c) => (
          <div key={c.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex items-center justify-between">
            <div>
              <div className="font-black text-base text-[#181A20]">{c.code}</div>
              <div className="text-xs text-[#717784]">
                {c.discountType === 'percent' ? `${formatNumber(c.discountValue)}%` : formatCurrency(c.discountValue)} • استخدامات: {formatNumber(c.usedCount)}
              </div>
            </div>
            <div className="flex gap-1.5">
              <button type="button" onClick={() => setEditing({ ...c })} className="px-3 py-1.5 rounded-lg bg-[#FFF9DB] text-xs font-bold">
                تعديل
              </button>
              <button
                type="button"
                onClick={() => deleteCouponDoc(c.id)}
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
          <form onSubmit={handleSave} className="bg-white rounded-3xl max-w-md w-full p-5 space-y-3">
            <h3 className="font-black text-base">{editing.id ? 'تعديل الكوبون' : 'كوبون خصم جديد'}</h3>
            <input value={editing.code || ''} onChange={(e) => setEditing({ ...editing, code: e.target.value })} placeholder="كود الخصم (مثال: SAVE10)" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs uppercase" required />
            <div className="grid grid-cols-2 gap-2 text-xs">
              <select value={editing.discountType || 'percent'} onChange={(e) => setEditing({ ...editing, discountType: e.target.value as 'percent' | 'fixed' })} className="h-10 px-3 rounded-xl border border-[#EBECEF]">
                <option value="percent">نسبة مئوية (%)</option>
                <option value="fixed">مبلغ ثابت (ج.م)</option>
              </select>
              <input type="number" step="0.01" value={editing.discountValue ?? 10} onChange={(e) => setEditing({ ...editing, discountValue: Number(e.target.value) })} placeholder="قيمة الخصم" className="h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              <input type="number" step="0.01" value={editing.minOrderAmount ?? 0} onChange={(e) => setEditing({ ...editing, minOrderAmount: Number(e.target.value) })} placeholder="أقل طلب" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input type="number" value={editing.usageLimit ?? ''} onChange={(e) => setEditing({ ...editing, usageLimit: e.target.value ? Number(e.target.value) : undefined })} placeholder="حد الاستخدام" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
            </div>
            <label className="flex items-center gap-2 text-xs font-bold">
              <input type="checkbox" checked={editing.isActive ?? true} onChange={(e) => setEditing({ ...editing, isActive: e.target.checked })} />
              <span>مفعّل</span>
            </label>
            <div className="flex gap-2">
              <button type="submit" className="flex-1 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm">حفظ</button>
              <button type="button" onClick={() => setEditing(null)} className="px-4 py-2.5 rounded-xl border border-[#EBECEF] text-xs font-bold">إلغاء</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export const AdminDeliveryZonesTab: React.FC<{ zones: DeliveryZone[] }> = ({ zones }) => {
  const { showToast } = useStore();
  const [editing, setEditing] = useState<Partial<DeliveryZone> | null>(null);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editing?.name?.trim()) return;
    try {
      await saveDeliveryZoneDoc(editing);
      showToast('تم حفظ منطقة التوصيل', 'success');
      setEditing(null);
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ المنطقة.'), 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => setEditing({ name: '', governorate: '', fee: 25, estimatedDuration: 'خلال ٢٤ - ٤٨ ساعة', isActive: true })}
          className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
        >
          <Plus className="w-4 h-4" />
          <span>إضافة منطقة توصيل</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {zones.map((z) => (
          <div key={z.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex items-center justify-between">
            <div>
              <div className="font-bold text-sm">{z.governorate} — {z.name}</div>
              <div className="text-xs text-[#717784]">{formatCurrency(z.fee)} • {z.estimatedDuration}</div>
            </div>
            <div className="flex gap-1.5">
              <button type="button" onClick={() => setEditing({ ...z })} className="px-3 py-1.5 rounded-lg bg-[#FFF9DB] text-xs font-bold">تعديل</button>
              <button type="button" onClick={() => deleteDeliveryZoneDoc(z.id)} className="p-2 rounded-lg bg-[#FDECEC] text-[#E53935]">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {editing && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleSave} className="bg-white rounded-3xl max-w-md w-full p-5 space-y-3">
            <h3 className="font-black text-base">{editing.id ? 'تعديل منطقة التوصيل' : 'منطقة توصيل جديدة'}</h3>
            <input value={editing.governorate || ''} onChange={(e) => setEditing({ ...editing, governorate: e.target.value })} placeholder="المحافظة *" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs" required />
            <input value={editing.name || ''} onChange={(e) => setEditing({ ...editing, name: e.target.value })} placeholder="اسم المنطقة / المركز *" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs" required />
            <input type="number" step="0.01" value={editing.fee ?? 0} onChange={(e) => setEditing({ ...editing, fee: Number(e.target.value) })} placeholder="رسوم التوصيل" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs" required />
            <input value={editing.estimatedDuration || ''} onChange={(e) => setEditing({ ...editing, estimatedDuration: e.target.value })} placeholder="المدة المتوقعة للتوصيل" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs" required />
            <label className="flex items-center gap-2 text-xs font-bold">
              <input type="checkbox" checked={editing.isActive ?? true} onChange={(e) => setEditing({ ...editing, isActive: e.target.checked })} />
              <span>مفعّلة</span>
            </label>
            <div className="flex gap-2">
              <button type="submit" className="flex-1 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm">حفظ</button>
              <button type="button" onClick={() => setEditing(null)} className="px-4 py-2.5 rounded-xl border border-[#EBECEF] text-xs font-bold">إلغاء</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
