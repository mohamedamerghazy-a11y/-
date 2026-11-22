import React, { useEffect, useMemo, useState } from 'react';
import {
  Building2,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Edit2,
  Eye,
  EyeOff,
  KeyRound,
  Megaphone,
  Plus,
  Save,
  SearchX,
  Smartphone,
  Sparkles,
  Star,
  Trash2,
  Truck,
  Video,
  Volume2,
  VolumeX,
} from 'lucide-react';
import {
  APP_ICON_THEME_OPTIONS,
  BANNER_PLACEMENT_LABELS,
  CATEGORIES_TAB_STYLE_OPTIONS,
  COMPANY_OFFER_BADGE_STYLE_OPTIONS,
  HOME_CATEGORY_STYLE_OPTIONS,
  PAYMENT_METHOD_META,
  PAYMENT_STATUS_LABELS,
  displaySenderPhone,
  type CashVerificationLog,
  type CategoriesTabStyle,
  type CompanyOfferBadgeStyle,
  type HomeCategoryStyle,
  type NumeralFormat,
  type Order,
  type Product,
  type ProductReview,
  type SearchMiss,
  type StoreCompany,
  type StoreContactChannel,
  type StoreGeneralSettings,
  type StorePaymentSettings,
  type UserProfile,
} from '../../types/models';
import { useStore } from '../../context/StoreContext';
import { CompanyOfferBadge } from '../../components/common/CompanyOfferBadge';
import { ProductCardStyleAdminSection } from './ProductCardStyleAdminSection';
import { AnnouncementTickerAdminSection } from './AnnouncementTickerAdminSection';
import { AnimatedImageUploader } from '../../components/common/AnimatedImageUploader';
import { VideoUploader } from '../../components/common/VideoUploader';
import {
  broadcastNotificationToUsers,
  deleteCompanyDoc,
  deleteOrderReceiptImage,
  deleteOrderReceiptsBatch,
  deleteReviewDoc,
  deleteSearchMiss,
  saveCompanyDoc,
  savePaymentSettingsDoc,
  saveStoreGeneralSettingsDoc,
  sendNotificationToUser,
  setReviewHidden,
  updatePaymentStatusAdmin,
} from '../../services/firestoreService';
import {
  formatCurrency,
  formatDateTime,
  formatDigits,
  formatNumber,
  toArabicErrorMessage,
  toMillis,
} from '../../lib/formatters';
import { ImageUploader } from '../../components/common/ImageUploader';

// 1. Payments Settings & Transfer Receipt Verification
export const AdminPaymentsTab: React.FC<{
  orders: Order[];
  paymentSettings: StorePaymentSettings;
  cashLogs: CashVerificationLog[];
}> = ({ orders, paymentSettings, cashLogs }) => {
  const { showToast } = useStore();
  const [subTab, setSubTab] = useState<'transfers' | 'auto_logs'>('transfers');
  const [form, setForm] = useState<StorePaymentSettings>(paymentSettings);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setForm(paymentSettings);
  }, [paymentSettings]);

  const transferOrders = useMemo(
    () => orders.filter((o) => o.paymentMethod !== 'cod' || Boolean(o.receiptImageUrl)),
    [orders],
  );

  const oldVerifiedReceipts = useMemo(() => {
    const cutoff = Date.now() - 30 * 24 * 3600 * 1000;
    return orders
      .filter((o) => o.receiptImageUrl && o.paymentStatus === 'verified' && toMillis(o.createdAt) < cutoff)
      .map((o) => o.id);
  }, [orders]);

  const saveConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await savePaymentSettingsDoc(form);
      showToast('تم حفظ إعدادات وسائل الدفع بنجاح', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ الإعدادات.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const setPayStatus = async (order: Order, st: 'verified' | 'rejected') => {
    try {
      await updatePaymentStatusAdmin({ orderId: order.id, paymentStatus: st });
      await sendNotificationToUser({
        targetUid: order.userId,
        title: st === 'verified' ? `تم تأكيد الدفع للطلب #${order.orderNumber}` : `مراجعة دفع الطلب #${order.orderNumber}`,
        message:
          st === 'verified'
            ? 'تم اعتماد التحويل المالي بنجاح وجارٍ استكمال تجهيز الطلب.'
            : 'يرجى مراجعة بيانات الحوالة المرسلة مع خدمة العملاء.',
        type: 'payment_status',
        referenceId: order.id,
      }).catch(() => {});
      showToast(st === 'verified' ? 'تم تأكيد الدفع' : 'تم رفض الإيصال', 'info');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر تحديث حالة الدفع.'), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <form onSubmit={saveConfig} className="bg-white border border-[#EBECEF] rounded-2xl p-5 space-y-4">
        <h3 className="font-black text-base">إعدادات بوابات ومحافظ الدفع</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
          <div>
            <label className="block font-bold mb-1">رقم فودافون كاش</label>
            <input value={form.vodafoneCashNumber} onChange={(e) => setForm({ ...form, vodafoneCashNumber: e.target.value })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" />
          </div>
          <div>
            <label className="block font-bold mb-1">عنوان إنستاباي (InstaPay)</label>
            <input value={form.instaPayAddress} onChange={(e) => setForm({ ...form, instaPayAddress: e.target.value })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" />
          </div>
        </div>
        <textarea
          value={form.paymentInstructions}
          onChange={(e) => setForm({ ...form, paymentInstructions: e.target.value })}
          rows={2}
          placeholder="تعليمات الدفع التي تظهر للعميل عند اختيار التحويل..."
          className="w-full p-3 rounded-xl border border-[#EBECEF] text-xs"
        />
        <div className="flex flex-wrap gap-4 text-xs font-bold">
          <label className="flex items-center gap-1.5">
            <input type="checkbox" checked={form.codEnabled} onChange={(e) => setForm({ ...form, codEnabled: e.target.checked })} />
            <span>تفعيل الدفع عند الاستلام</span>
          </label>
          <label className="flex items-center gap-1.5">
            <input type="checkbox" checked={form.instaPayEnabled} onChange={(e) => setForm({ ...form, instaPayEnabled: e.target.checked })} />
            <span>تفعيل إنستاباي</span>
          </label>
          <label className="flex items-center gap-1.5">
            <input type="checkbox" checked={form.vodafoneCashEnabled} onChange={(e) => setForm({ ...form, vodafoneCashEnabled: e.target.checked })} />
            <span>تفعيل فودافون كاش</span>
          </label>
        </div>
        <button type="submit" disabled={saving} className="px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black">
          {saving ? 'جاري الحفظ...' : 'حفظ إعدادات الدفع'}
        </button>
      </form>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setSubTab('transfers')}
          className={`px-4 py-2 rounded-xl text-xs font-extrabold border transition ${
            subTab === 'transfers'
              ? 'bg-[#181A20] text-[#FFD000] border-[#181A20]'
              : 'bg-white border-[#EBECEF] text-[#181A20]'
          }`}
        >
          مراجعة التحويلات والإيصالات ({formatNumber(transferOrders.length)})
        </button>
        <button
          type="button"
          onClick={() => setSubTab('auto_logs')}
          className={`px-4 py-2 rounded-xl text-xs font-extrabold border transition ${
            subTab === 'auto_logs'
              ? 'bg-[#181A20] text-[#FFD000] border-[#181A20]'
              : 'bg-white border-[#EBECEF] text-[#181A20]'
          }`}
        >
          سجل التحقق الآلي — الإيمان كاش ({formatNumber(cashLogs.length)})
        </button>
      </div>

      {subTab === 'transfers' ? (
        <div className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="font-black text-base">مراجعة التحويلات والإيصالات ({formatNumber(transferOrders.length)})</h3>
            {oldVerifiedReceipts.length > 0 && (
              <button
                type="button"
                onClick={async () => {
                  const n = await deleteOrderReceiptsBatch(oldVerifiedReceipts);
                  showToast(`تم تنظيف ${formatNumber(n)} إيصال قديم`, 'info');
                }}
                className="px-3.5 py-1.5 rounded-xl bg-[#FDECEC] text-[#E53935] text-xs font-bold"
              >
                تنظيف الإيصالات المؤكدة الأقدم من ٣٠ يومًا ({formatNumber(oldVerifiedReceipts.length)})
              </button>
            )}
          </div>

          {transferOrders.map((o) => {
            const senderNum = displaySenderPhone(o);
            return (
              <div key={o.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex flex-wrap items-center justify-between gap-4">
                <div className="space-y-1 text-xs">
                  <div className="font-black text-sm">
                    #{formatDigits(o.orderNumber)} — {o.customerName} ({formatCurrency(o.totalAmount)})
                  </div>
                  <div className="text-[#717784]">
                    {PAYMENT_METHOD_META[o.paymentMethod]?.label} • {PAYMENT_STATUS_LABELS[o.paymentStatus]}
                    {senderNum ? ` • الرقم المحوَّل منه: ${formatDigits(senderNum)}` : ''}
                  </div>
                  {o.receiptImageUrl && (
                    <a href={o.receiptImageUrl} target="_blank" rel="noopener noreferrer" className="inline-block text-[#1565C0] font-bold underline mt-1">
                      عرض صورة الإيصال المرفق
                    </a>
                  )}
                </div>
                <div className="flex flex-wrap gap-2">
                  {o.paymentStatus !== 'verified' && (
                    <button type="button" onClick={() => setPayStatus(o, 'verified')} className="px-3.5 py-1.5 rounded-xl bg-[#219653] text-white text-xs font-bold">
                      اعتماد الدفع
                    </button>
                  )}
                  {o.paymentStatus !== 'rejected' && (
                    <button type="button" onClick={() => setPayStatus(o, 'rejected')} className="px-3.5 py-1.5 rounded-xl bg-[#FDECEC] text-[#E53935] text-xs font-bold">
                      رفض
                    </button>
                  )}
                  {o.receiptImageUrl && (
                    <button type="button" onClick={() => deleteOrderReceiptImage(o.id)} className="px-3 py-1.5 rounded-xl border border-[#EBECEF] text-xs font-bold text-[#717784]">
                      حذف الإيصال
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="space-y-3">
          <h3 className="font-black text-base">سجل التحقق الآلي عبر تطبيق «الإيمان كاش»</h3>
          {cashLogs.length === 0 ? (
            <div className="bg-white border border-[#EBECEF] rounded-2xl p-8 text-center text-xs text-[#717784]">
              لا توجد سجلات تحقق آلي بعد — عند استقبال رسالة تحويل مطابقة في تطبيق «الإيمان كاش» سيظهر التوثيق هنا فورًا.
            </div>
          ) : (
            cashLogs.map((log) => (
              <div key={log.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 space-y-1.5 text-xs">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-black text-sm text-[#181A20]">
                    {log.providerName || log.senderAddress || 'محفظة إلكترونية'}
                  </span>
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-[11px] font-black ${
                      log.status === 'matched'
                        ? 'bg-[#E8F6EE] text-[#219653]'
                        : log.status === 'duplicate'
                        ? 'bg-[#FFF9DB] text-[#181A20]'
                        : log.status === 'ignored_sender'
                        ? 'bg-gray-100 text-[#717784]'
                        : 'bg-[#FDECEC] text-[#E53935]'
                    }`}
                  >
                    {log.status === 'matched'
                      ? 'تم التأكيد الآلي'
                      : log.status === 'duplicate'
                      ? 'عملية مكررة'
                      : log.status === 'ignored_sender'
                      ? 'مرسل غير مفعل'
                      : 'غير مطابق'}
                  </span>
                </div>
                <div className="font-bold text-[#181A20]">
                  المبلغ المستلم: {formatCurrency(log.amount)} • من رقم:{' '}
                  <span dir="ltr">{formatDigits(log.senderPhone || '-')}</span>
                </div>
                {log.matchedOrderNumber && (
                  <div className="font-extrabold text-[#219653]">
                    تم ربطه بالطلب: #{formatDigits(log.matchedOrderNumber)}
                  </div>
                )}
                {log.transactionId && (
                  <div className="text-[#717784]">رقم العملية: {log.transactionId}</div>
                )}
                {log.reason && <div className="text-[#717784]">{log.reason}</div>}
                <div className="text-[11px] text-[#717784]">{formatDateTime(log.createdAt)}</div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};

// 2. Reviews Moderation
export const AdminReviewsTab: React.FC<{ reviews: ProductReview[] }> = ({ reviews }) => {
  const { showToast } = useStore();
  return (
    <div className="space-y-3">
      {reviews.map((r) => (
        <div key={r.id} className="bg-white border border-[#EBECEF] rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
          <div className="space-y-1 text-xs">
            <div className="font-bold text-sm flex items-center gap-2">
              <span>{r.productName}</span>
              <span className="inline-flex items-center gap-0.5 text-amber-500">
                <Star className="w-3.5 h-3.5 fill-current" />
                {formatNumber(r.rating)}
              </span>
              {r.isHidden && <span className="px-2 py-0.5 rounded-full bg-gray-200 text-gray-700 text-[10px]">مخفي</span>}
            </div>
            <div className="text-[#181A20]">{r.comment}</div>
            <div className="text-[#717784]">{r.userName} • {formatDateTime(r.createdAt)}</div>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={async () => {
                await setReviewHidden(r.id, !r.isHidden);
                showToast(r.isHidden ? 'تم إظهار التقييم' : 'تم إخفاء التقييم', 'info');
              }}
              className="px-3 py-1.5 rounded-xl bg-[#FFF9DB] text-xs font-bold inline-flex items-center gap-1"
            >
              {r.isHidden ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
              <span>{r.isHidden ? 'إظهار' : 'إخفاء'}</span>
            </button>
            <button
              type="button"
              onClick={async () => {
                await deleteReviewDoc(r.id);
                showToast('تم حذف التقييم', 'info');
              }}
              className="p-2 rounded-xl bg-[#FDECEC] text-[#E53935]"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>
      ))}
    </div>
  );
};

// 3. Reports & Analytics
export const AdminReportsTab: React.FC<{
  orders: Order[];
  products: Product[];
  searchMisses?: SearchMiss[];
  onOpenProducts?: () => void;
}> = ({ orders, products, searchMisses = [], onOpenProducts }) => {
  const { showToast } = useStore();
  const [period, setPeriod] = useState<'today' | '7d' | '30d' | 'all'>('30d');

  const filteredOrders = useMemo(() => {
    const now = Date.now();
    const minMs =
      period === 'today'
        ? new Date().setHours(0, 0, 0, 0)
        : period === '7d'
        ? now - 7 * 24 * 3600 * 1000
        : period === '30d'
        ? now - 30 * 24 * 3600 * 1000
        : 0;
    return orders.filter((o) => toMillis(o.createdAt) >= minMs);
  }, [orders, period]);

  const validOrders = useMemo(
    () => filteredOrders.filter((o) => o.orderStatus !== 'cancelled' && o.orderStatus !== 'returned'),
    [filteredOrders],
  );

  const totalRevenue = validOrders.reduce((s, o) => s + o.totalAmount, 0);
  const wholesaleRev = validOrders.filter((o) => o.accountTypeAtOrder === 'wholesale').reduce((s, o) => s + o.totalAmount, 0);
  const retailRev = Math.max(0, totalRevenue - wholesaleRev);
  const pickupCount = validOrders.filter((o) => o.fulfillmentType === 'pickup').length;
  const deliveryCount = validOrders.length - pickupCount;
  const topProducts = useMemo(
    () => [...products].sort((a, b) => b.salesCount - a.salesCount).slice(0, 5),
    [products],
  );

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap gap-2 text-xs font-bold">
        {[
          { k: 'today', l: 'اليوم' },
          { k: '7d', l: 'آخر ٧ أيام' },
          { k: '30d', l: 'آخر ٣٠ يومًا' },
          { k: 'all', l: 'كل الفترات' },
        ].map((p) => (
          <button
            key={p.k}
            type="button"
            onClick={() => setPeriod(p.k as 'today' | '7d' | '30d' | 'all')}
            className={`px-4 py-2 rounded-xl border ${
              period === p.k ? 'bg-[#181A20] text-[#FFD000]' : 'bg-white border-[#EBECEF]'
            }`}
          >
            {p.l}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="bg-white border border-[#EBECEF] rounded-2xl p-4">
          <div className="text-xs text-[#717784] font-bold">صافي المبيعات</div>
          <div className="text-xl font-black mt-1">{formatCurrency(totalRevenue)}</div>
        </div>
        <div className="bg-white border border-[#EBECEF] rounded-2xl p-4">
          <div className="text-xs text-[#717784] font-bold">عدد الطلبات الفعالة</div>
          <div className="text-xl font-black mt-1">{formatNumber(validOrders.length)}</div>
        </div>
        <div className="bg-white border border-[#EBECEF] rounded-2xl p-4">
          <div className="text-xs text-[#717784] font-bold">مبيعات الجملة</div>
          <div className="text-xl font-black text-[#1565C0] mt-1">{formatCurrency(wholesaleRev)}</div>
        </div>
        <div className="bg-white border border-[#EBECEF] rounded-2xl p-4">
          <div className="text-xs text-[#717784] font-bold">استلام من الفرع / توصيل</div>
          <div className="text-xl font-black mt-1">
            {formatNumber(pickupCount)} / {formatNumber(deliveryCount)}
          </div>
        </div>
      </div>

      {/* Simple Bar Visualization */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl p-5 space-y-3">
        <h3 className="font-black text-sm">توزيع المبيعات (قطاعي مقابل جملة)</h3>
        <div className="space-y-2 text-xs">
          <div>
            <div className="flex justify-between font-bold mb-1">
              <span>مبيعات المتجر (الأفراد)</span>
              <span>{formatCurrency(retailRev)}</span>
            </div>
            <div className="h-3 rounded-full bg-[#F9FAFB] overflow-hidden">
              <div
                className="h-full bg-[#FFD000]"
                style={{ width: `${totalRevenue > 0 ? Math.round((retailRev / totalRevenue) * 100) : 0}%` }}
              />
            </div>
          </div>
          <div>
            <div className="flex justify-between font-bold mb-1">
              <span>مبيعات الجملة</span>
              <span>{formatCurrency(wholesaleRev)}</span>
            </div>
            <div className="h-3 rounded-full bg-[#F9FAFB] overflow-hidden">
              <div
                className="h-full bg-[#1565C0]"
                style={{ width: `${totalRevenue > 0 ? Math.round((wholesaleRev / totalRevenue) * 100) : 0}%` }}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white border border-[#EBECEF] rounded-2xl p-5 space-y-3">
        <h3 className="font-black text-sm">الأكثر مبيعًا في المتجر</h3>
        {topProducts.map((p, i) => (
          <div key={p.id} className="flex justify-between items-center text-xs py-1.5 border-b border-[#EBECEF] last:border-none">
            <span className="font-bold">{formatNumber(i + 1)}. {p.name}</span>
            <span className="font-black text-[#219653]">{formatNumber(p.salesCount)} مبيعة</span>
          </div>
        ))}
      </div>

      {/* Zero-Result Search Queries Analytics */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl p-5 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h3 className="font-black text-sm flex items-center gap-1.5">
              <SearchX className="w-4 h-4 text-[#E53935]" />
              <span>كلمات بحث بدون نتائج ({formatNumber(searchMisses.length)})</span>
            </h3>
            <p className="text-[11px] text-[#717784]">
              عبارات بحث عنها العملاء ولم يجدوها — تساعدك على إضافة المنتجات المطلوبة
            </p>
          </div>
          {onOpenProducts && (
            <button
              type="button"
              onClick={onOpenProducts}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>إضافة منتج</span>
            </button>
          )}
        </div>

        {searchMisses.length === 0 ? (
          <div className="text-xs text-[#717784] text-center py-4 bg-[#F9FAFB] rounded-xl border border-[#EBECEF]">
            لا توجد عمليات بحث فاشلة مسجلة حتى الآن.
          </div>
        ) : (
          <div className="space-y-2">
            {searchMisses.map((miss) => (
              <div
                key={miss.id}
                className="flex flex-wrap items-center justify-between gap-2 p-3 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs"
              >
                <div className="space-y-0.5 min-w-0">
                  <div className="font-black text-[#181A20] truncate">«{miss.query}»</div>
                  <div className="text-[11px] text-[#717784]">
                    {miss.lastSource === 'web' ? 'متجر الويب' : 'تطبيق أندرويد'} • آخر بحث:{' '}
                    {formatDateTime(miss.lastSearchedAt || miss.createdAt)}
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className="px-2.5 py-1 rounded-full bg-[#FDECEC] text-[#E53935] font-black text-[11px]">
                    {formatNumber(miss.missCount)} مرة
                  </span>
                  <button
                    type="button"
                    aria-label="حذف سجل البحث"
                    onClick={async () => {
                      try {
                        await deleteSearchMiss(miss.id);
                        showToast('تم حذف سجل البحث', 'info');
                      } catch (err) {
                        showToast(toArabicErrorMessage(err, 'تعذر الحذف.'), 'error');
                      }
                    }}
                    className="p-1.5 rounded-lg text-[#E53935] hover:bg-[#FDECEC]"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

// 4. Collapsible Accordion Store Settings (Matches Android 1:1 with per-section Save)
type SettingsSectionKey =
  | 'general'
  | 'app_icon'
  | 'intro_video'
  | 'announcement_ticker'
  | 'companies'
  | 'contact'
  | 'hero'
  | 'product_card_style'
  | 'categories_style'
  | 'policies'
  | 'broadcast';

export const AdminStoreSettingsTab: React.FC<{
  storeSettings: StoreGeneralSettings;
  users: UserProfile[];
  companies?: StoreCompany[];
}> = ({ storeSettings, users, companies = [] }) => {
  const { showToast, canToggleWholesalePreview, isWholesalePreviewActive, toggleWholesalePreview } = useStore();
  const [form, setForm] = useState<StoreGeneralSettings>(storeSettings);
  const [openKey, setOpenKey] = useState<SettingsSectionKey | null>('general');
  const [saving, setSaving] = useState(false);
  const [editingCompany, setEditingCompany] = useState<Partial<StoreCompany> | null>(null);
  const [savingCompany, setSavingCompany] = useState(false);

  // Broadcast state
  const [broadcastTitle, setBroadcastTitle] = useState('');
  const [broadcastMsg, setBroadcastMsg] = useState('');
  const [broadcastTarget, setBroadcastTarget] = useState<'all' | 'wholesale' | 'retail'>('all');
  const [sendingBroadcast, setSendingBroadcast] = useState(false);

  useEffect(() => {
    setForm(storeSettings);
  }, [storeSettings]);

  const saveSection = async (label: string) => {
    if (form.freeShippingEnabled && (!form.freeShippingThreshold || form.freeShippingThreshold <= 0)) {
      showToast('أدخل قيمة الحد الأدنى للشحن المجاني أكبر من صفر.', 'error');
      return;
    }
    setSaving(true);
    try {
      await saveStoreGeneralSettingsDoc(form);
      showToast(`تم حفظ قسم «${label}» بنجاح`, 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ الإعدادات.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const toggle = (k: SettingsSectionKey) => setOpenKey((prev) => (prev === k ? null : k));

  const handleBroadcast = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!broadcastTitle.trim() || !broadcastMsg.trim()) return;
    setSendingBroadcast(true);
    try {
      const targetUids = users
        .filter((u) => {
          if (broadcastTarget === 'wholesale') return u.accountType === 'wholesale' && u.wholesaleStatus === 'approved';
          if (broadcastTarget === 'retail') return u.accountType !== 'wholesale';
          return true;
        })
        .map((u) => u.uid);
      const sent = await broadcastNotificationToUsers({
        targetUids,
        title: broadcastTitle,
        message: broadcastMsg,
      });
      setBroadcastTitle('');
      setBroadcastMsg('');
      showToast(`تم إرسال الإشعار إلى ${formatNumber(sent)} عميل`, 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر إرسال الإشعارات.'), 'error');
    } finally {
      setSendingBroadcast(false);
    }
  };

  const renderAccordionHeader = (k: SettingsSectionKey, title: string, subtitle: string) => {
    const isOpen = openKey === k;
    return (
      <button
        type="button"
        onClick={() => toggle(k)}
        className="w-full px-5 py-4 flex items-center justify-between text-right hover:bg-[#F9FAFB] transition"
      >
        <div>
          <div className="font-black text-sm text-[#181A20]">{title}</div>
          <div className="text-[11px] text-[#717784]">{subtitle}</div>
        </div>
        {isOpen ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
      </button>
    );
  };

  return (
    <div className="space-y-3">
      {canToggleWholesalePreview && (
        <div className="bg-white border border-[#EBECEF] rounded-2xl px-5 py-3.5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="font-black text-sm text-[#181A20]">معاينة أسعار المتجر (قطاعي / جملة)</div>
            <div className="text-[11px] text-[#717784]">
              {isWholesalePreviewActive
                ? 'وضع المعاينة الحالي: أسعار الجملة المعتمدة'
                : 'وضع المعاينة الحالي: أسعار القطاعي للعملاء'}
            </div>
          </div>
          <button
            type="button"
            onClick={toggleWholesalePreview}
            className={`px-4 py-2 rounded-xl text-xs font-black border transition ${
              isWholesalePreviewActive
                ? 'bg-[#FFD000] border-[#181A20] text-[#181A20]'
                : 'bg-[#F9FAFB] border-[#EBECEF] text-[#181A20] hover:bg-[#FFF9DB]'
            }`}
          >
            {isWholesalePreviewActive ? 'معاينة: جملة' : 'معاينة: قطاعي'}
          </button>
        </div>
      )}

      {/* 1. General & Owner Email & Numerals */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader('general', 'بيانات المتجر الأساسية وبريد المالك والأرقام', 'اسم المتجر، الشعار، البريد المالك، تنسيق الأرقام')}
        {openKey === 'general' && (
          <div className="p-5 pt-2 border-t border-[#EBECEF] space-y-3 text-xs">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block font-bold mb-1">اسم المتجر</label>
                <input value={form.storeName} onChange={(e) => setForm({ ...form, storeName: e.target.value })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" />
              </div>
              <div>
                <label className="block font-bold mb-1">الوصف المختصر (Tagline)</label>
                <input value={form.tagline} onChange={(e) => setForm({ ...form, tagline: e.target.value })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" />
              </div>
              <div>
                <label className="block font-bold mb-1">البريد الإلكتروني لمالك المتجر</label>
                <input type="email" dir="ltr" value={form.ownerEmail} onChange={(e) => setForm({ ...form, ownerEmail: e.target.value })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" />
              </div>
              <div>
                <label className="block font-bold mb-1">تنسيق الأرقام والأسعار</label>
                <select value={form.numeralFormat} onChange={(e) => setForm({ ...form, numeralFormat: e.target.value as NumeralFormat })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]">
                  <option value="arabic">أرقام عربية (١٬٢٥٠ ج.م)</option>
                  <option value="english">أرقام إنجليزية (1,250 ج.م)</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block font-bold mb-1">شريط الإعلانات العلوي بالرئيسية</label>
              <input value={form.announcementText} onChange={(e) => setForm({ ...form, announcementText: e.target.value })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" />
            </div>

            {/* Configurable Free Shipping Threshold */}
            <div className="rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-3.5 space-y-3">
              <label className="flex items-center justify-between gap-3 cursor-pointer">
                <span className="flex items-center gap-2 font-black text-[#181A20]">
                  <Truck className="w-4 h-4 text-[#219653]" />
                  <span>تفعيل الشحن المجاني عند تجاوز قيمة معينة</span>
                </span>
                <input
                  type="checkbox"
                  checked={Boolean(form.freeShippingEnabled)}
                  onChange={(e) => setForm({ ...form, freeShippingEnabled: e.target.checked })}
                  className="w-4 h-4 accent-[#219653]"
                />
              </label>
              {form.freeShippingEnabled && (
                <div>
                  <label className="block font-bold mb-1 text-[#717784]">
                    الحد الأدنى للشحن المجاني (ج.م)
                  </label>
                  <input
                    type="number"
                    min="1"
                    step="1"
                    value={form.freeShippingThreshold ?? 500}
                    onChange={(e) =>
                      setForm({ ...form, freeShippingThreshold: Number(e.target.value) })
                    }
                    className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] bg-white font-bold"
                  />
                </div>
              )}
            </div>

            {/* Configurable Delivery Confirmation OTP */}
            <div className="rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-3.5 space-y-2">
              <label className="flex items-center justify-between gap-3 cursor-pointer">
                <span className="flex items-center gap-2 font-black text-[#181A20]">
                  <KeyRound className="w-4 h-4 text-[#181A20]" />
                  <span>تفعيل كود تأكيد الاستلام مع المندوب (٤ أرقام)</span>
                </span>
                <input
                  type="checkbox"
                  checked={form.deliveryOtpEnabled ?? true}
                  onChange={(e) => setForm({ ...form, deliveryOtpEnabled: e.target.checked })}
                  className="w-4 h-4 accent-[#181A20]"
                />
              </label>
              <p className="text-[11px] text-[#717784]">
                {(form.deliveryOtpEnabled ?? true)
                  ? 'سيتم إنشاء كود تحقق من ٤ أرقام لطلبات التوصيل الجديدة، ويُطلب من المندوب إدخاله قبل تأكيد التسليم.'
                  : 'الميزة مغلقة حاليًا؛ الطلبات الجديدة لن تطلب كود تأكيد عند التسليم مع المندوب.'}
              </p>
            </div>

            <button type="button" disabled={saving} onClick={() => saveSection('البيانات الأساسية')} className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black">
              <Save className="w-4 h-4" />
              <span>حفظ هذا القسم</span>
            </button>
          </div>
        )}
      </div>

      {/* 1.15 App Icon Theme Selector (أيقونة تطبيق الأندرويد الموسمية) */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader(
          'app_icon',
          'أيقونة تطبيق الأندرويد على الموبايل (المواسم والعروض)',
          `الأيقونة الحالية: ${
            APP_ICON_THEME_OPTIONS.find((opt) => opt.code === (form.appIconTheme || 'default'))?.label ||
            'الأيقونة الأساسية'
          } — تتحدث تلقائيًا على هواتف العملاء`,
        )}
        {openKey === 'app_icon' && (
          <div className="p-5 pt-3 border-t border-[#EBECEF] space-y-4 text-xs">
            <div className="rounded-2xl bg-[#FFF9DB]/70 border border-[#FFD000]/60 p-3 flex items-start gap-2 text-[#181A20]">
              <Smartphone className="w-4 h-4 shrink-0 mt-0.5" />
              <p className="font-bold leading-relaxed">
                اختر شكل أيقونة تطبيق <strong>الإيمان للتجارة</strong> للموسم الحالي (الأساسية، عروض، رمضان، أو العيد). عند الحفظ تتغير أيقونة التطبيق تلقائيًا على هواتف الأندرويد بمجرد فتح العملاء للتطبيق.
              </p>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {APP_ICON_THEME_OPTIONS.map((opt) => {
                const selected = (form.appIconTheme || 'default') === opt.code;
                return (
                  <button
                    key={opt.code}
                    type="button"
                    onClick={() => setForm({ ...form, appIconTheme: opt.code })}
                    className={`relative rounded-2xl border p-3 text-center flex flex-col items-center gap-2 transition ${
                      selected
                        ? 'bg-[#FFF9DB] border-[#181A20] border-2 shadow-sm'
                        : 'bg-[#F9FAFB] border-[#EBECEF] hover:border-[#181A20]/40'
                    }`}
                  >
                    {selected && (
                      <span className="absolute top-2 left-2 text-[#181A20]">
                        <CheckCircle2 className="w-4 h-4 fill-[#FFD000]" />
                      </span>
                    )}
                    <img
                      src={opt.previewSrc}
                      alt={opt.label}
                      className="w-16 h-16 rounded-2xl object-cover border border-[#EBECEF] shadow-sm"
                    />
                    <div className="font-black text-[#181A20] text-xs">{opt.label}</div>
                    <div className="text-[11px] text-[#717784] leading-snug">{opt.subtitle}</div>
                  </button>
                );
              })}
            </div>

            <button
              type="button"
              disabled={saving}
              onClick={() => saveSection('أيقونة التطبيق الموسمية')}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black"
            >
              <Save className="w-4 h-4" />
              <span>حفظ وتطبيق أيقونة التطبيق المختارة</span>
            </button>
          </div>
        )}
      </div>

      {/* 1.25 Intro Reel Media (مقطع الريلز الترحيبي بعد شاشة البداية — لتطبيق الأندرويد فقط) */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader(
          'intro_video',
          'مقطع الريلز الترحيبي بعد شاشة البداية (لتطبيق الأندرويد فقط)',
          (() => {
            const isAnim = form.introMediaType === 'animated_image';
            const activeUrl = (isAnim ? form.introImageUrl : form.introVideoUrl)?.trim() || '';
            if (form.introVideoEnabled && activeUrl) {
              const typeLabel = isAnim ? 'صورة متحركة' : 'فيديو ريلز';
              if (form.introVideoDurationSeconds > 0) {
                return `مفعّل للأندرويد (${typeLabel}) • المدة: ${formatNumber(form.introVideoDurationSeconds)} ثانية`;
              }
              return isAnim
                ? `مفعّل للأندرويد (${typeLabel}) • ٥ ثوانٍ افتراضيًا`
                : `مفعّل للأندرويد (${typeLabel}) • يتعرف على المدة تلقائيًا`;
            }
            return 'اعرض فيديو ريلز قصير أو صورة متحركة (GIF / WebP) بعد شاشة البداية في تطبيق الأندرويد فقط مع زر تخطى';
          })(),
        )}
        {openKey === 'intro_video' && (
          <div className="p-5 pt-3 border-t border-[#EBECEF] space-y-4 text-xs">
            <div className="rounded-2xl bg-[#FFF9DB]/70 border border-[#FFD000]/60 p-3 flex items-start gap-2 text-[#181A20]">
              {form.introMediaType === 'animated_image' ? (
                <Sparkles className="w-4 h-4 shrink-0 mt-0.5" />
              ) : (
                <Video className="w-4 h-4 shrink-0 mt-0.5" />
              )}
              <p className="font-bold leading-relaxed">
                اختر بين رفع <strong>فيديو ريلز قصير</strong> (يعمل بالصوت افتراضيًا) أو <strong>صورة متحركة (GIF / WebP)</strong> لتظهر بملء الشاشة بعد شاشة البداية مباشرة في <strong>تطبيق الأندرويد فقط</strong>، مع زر «تخطى» في الأعلى وانتقال أوتوماتيكي للمتجر فور انتهاء المدة.
              </p>
            </div>

            <label className="flex items-center justify-between gap-3 rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-3.5 cursor-pointer">
              <span className="font-black text-[#181A20]">
                تفعيل العرض الترحيبي بعد شاشة البداية
              </span>
              <input
                type="checkbox"
                checked={Boolean(form.introVideoEnabled)}
                onChange={(e) => setForm({ ...form, introVideoEnabled: e.target.checked })}
                className="w-4 h-4 accent-[#181A20]"
              />
            </label>

            <div>
              <label className="block font-bold mb-1.5 text-[#181A20]">نوع المحتوى الترحيبي</label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setForm({ ...form, introMediaType: 'video' })}
                  className={`flex items-center justify-center gap-1.5 h-10 rounded-xl font-black border transition ${
                    form.introMediaType !== 'animated_image'
                      ? 'bg-[#181A20] text-[#FFD000] border-[#181A20]'
                      : 'bg-white text-[#181A20] border-[#EBECEF] hover:bg-[#F9FAFB]'
                  }`}
                >
                  <Video className="w-4 h-4" />
                  <span>فيديو ريلز قصير</span>
                </button>
                <button
                  type="button"
                  onClick={() => setForm({ ...form, introMediaType: 'animated_image' })}
                  className={`flex items-center justify-center gap-1.5 h-10 rounded-xl font-black border transition ${
                    form.introMediaType === 'animated_image'
                      ? 'bg-[#181A20] text-[#FFD000] border-[#181A20]'
                      : 'bg-white text-[#181A20] border-[#EBECEF] hover:bg-[#F9FAFB]'
                  }`}
                >
                  <Sparkles className="w-4 h-4" />
                  <span>صورة متحركة (GIF / WebP)</span>
                </button>
              </div>
            </div>

            {form.introMediaType === 'animated_image' ? (
              <AnimatedImageUploader
                folder="aleman/banners"
                imageUrl={form.introImageUrl || ''}
                isSlimPreview={false}
                label="رفع صورة متحركة للعرض الترحيبي (GIF / WebP)"
                onChange={(url) =>
                  setForm((prev) => ({
                    ...prev,
                    introImageUrl: url,
                    introVideoEnabled: url === '' ? false : prev.introVideoEnabled,
                  }))
                }
              />
            ) : (
              <VideoUploader
                folder="aleman/videos"
                videoUrl={form.introVideoUrl || ''}
                durationSeconds={form.introVideoDurationSeconds || 0}
                label="رفع فيديو الريلز الترحيبي"
                onChange={({ videoUrl, durationSeconds }) =>
                  setForm((prev) => ({
                    ...prev,
                    introVideoUrl: videoUrl,
                    introVideoDurationSeconds:
                      videoUrl === ''
                        ? 0
                        : durationSeconds && durationSeconds > 0
                        ? durationSeconds
                        : prev.introVideoDurationSeconds,
                    introVideoEnabled: videoUrl === '' ? false : prev.introVideoEnabled,
                  }))
                }
              />
            )}

            {form.introMediaType !== 'animated_image' && (
              <div className="rounded-2xl border border-[#EBECEF] bg-[#F9FAFB] p-3 space-y-2">
                <div className="font-bold text-[#181A20]">إعداد صوت فيديو الريلز</div>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setForm({ ...form, introVideoMuted: false })}
                    className={`h-10 px-3 rounded-xl border font-black inline-flex items-center justify-center gap-1.5 transition ${
                      !form.introVideoMuted
                        ? 'bg-[#181A20] border-[#181A20] text-[#FFD000]'
                        : 'bg-white border-[#EBECEF] text-[#717784] hover:text-[#181A20]'
                    }`}
                  >
                    <Volume2 className="w-4 h-4" />
                    <span>بصوت</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setForm({ ...form, introVideoMuted: true })}
                    className={`h-10 px-3 rounded-xl border font-black inline-flex items-center justify-center gap-1.5 transition ${
                      Boolean(form.introVideoMuted)
                        ? 'bg-[#181A20] border-[#181A20] text-[#FFD000]'
                        : 'bg-white border-[#EBECEF] text-[#717784] hover:text-[#181A20]'
                    }`}
                  >
                    <VolumeX className="w-4 h-4" />
                    <span>كاتم (بدون صوت)</span>
                  </button>
                </div>
                <p className="text-[11px] text-[#717784]">
                  {form.introVideoMuted
                    ? 'سيعمل فيديو الريلز الترحيبي صامتًا بدون صوت.'
                    : 'سيعمل فيديو الريلز الترحيبي بالصوت تلقائيًا عند فتح التطبيق.'}
                </p>
              </div>
            )}

            <div>
              <label className="block font-bold mb-1">
                {form.introMediaType === 'animated_image'
                  ? 'مدة عرض الصورة المتحركة بالثواني (الافتراضي ٥ ثوانٍ)'
                  : 'مدة الفيديو بالثواني (تُكتشف تلقائيًا عند الرفع والتشغيل)'}
              </label>
              <input
                type="number"
                min="0"
                max="120"
                value={form.introVideoDurationSeconds || ''}
                onChange={(e) =>
                  setForm({
                    ...form,
                    introVideoDurationSeconds: Math.min(120, Math.max(0, Number(e.target.value) || 0)),
                  })
                }
                placeholder={form.introMediaType === 'animated_image' ? '5' : 'تلقائي'}
                className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
              />
            </div>

            <button
              type="button"
              disabled={saving}
              onClick={() => saveSection('مقطع الريلز الترحيبي')}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black"
            >
              <Save className="w-4 h-4" />
              <span>حفظ إعدادات العرض الترحيبي</span>
            </button>
          </div>
        )}
      </div>

      {/* 1.35 Scrolling Announcement Ticker Studio (الشريط الإعلاني المتحرك) */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader(
          'announcement_ticker',
          'الشريط الإعلاني المتحرك (نص + رموز + صور مصغرة)',
          form.tickerEnabled && (form.tickerText?.trim() || (form.tickerInlineImages?.length || 0) > 0)
            ? `مفعّل • الموقع: ${BANNER_PLACEMENT_LABELS[form.tickerPlacement || 'top_home'] || 'أعلى الرئيسية'} • حركة من اليسار لليمين`
            : 'أنشئ شريطًا إعلانيًا متحركًا من اليسار لليمين بألوان وخطوط ورموز وصور مصغرة في أي مكان بالمتجر',
        )}
        {openKey === 'announcement_ticker' && (
          <div className="p-5 pt-3 border-t border-[#EBECEF] text-xs">
            <AnnouncementTickerAdminSection
              form={form}
              setForm={setForm}
              saving={saving}
              onSave={() => saveSection('الشريط الإعلاني المتحرك')}
            />
          </div>
        )}
      </div>

      {/* 1.5 Companies Management (إضافة شركات) */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader(
          'companies',
          `إضافة شركات للمتجر (${formatNumber(companies.length)})`,
          'أضف أسماء وشعارات الشركات والماركات لربط المنتجات بها تلقائيًا داخل المتجر وعروض الشركات',
        )}
        {openKey === 'companies' && (
          <div className="p-5 pt-3 border-t border-[#EBECEF] space-y-4 text-xs">
            <div className="flex flex-wrap items-center justify-between gap-2 bg-[#FFF9DB]/70 border border-[#FFD000]/60 rounded-2xl p-3">
              <div className="flex items-center gap-2 text-[#181A20]">
                <Building2 className="w-4 h-4 shrink-0" />
                <span className="font-bold">
                  بعد إضافة الشركة هنا، ستظهر مباشرة عند إضافة المنتجات وفي قسم «منتجات الشركات» لجلب منتجاتها تلقائيًا.
                </span>
              </div>
              <button
                type="button"
                onClick={() =>
                  setEditingCompany({
                    name: '',
                    imageUrl: '',
                    displayOrder: companies.length,
                    isActive: true,
                  })
                }
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#181A20] text-[#FFD000] font-black text-xs shrink-0"
              >
                <Plus className="w-4 h-4" />
                <span>إضافة شركة جديدة</span>
              </button>
            </div>

            {companies.length === 0 ? (
              <div className="text-center py-8 bg-[#F9FAFB] rounded-2xl border border-dashed border-[#EBECEF] space-y-2">
                <Building2 className="w-10 h-10 mx-auto text-[#717784]" />
                <div className="font-black text-sm text-[#181A20]">لا توجد شركات مضافة بعد</div>
                <p className="text-[11px] text-[#717784]">
                  أضف شركاتك (مثل: أوكسي، برسيل، جهينة...) مع شعارها لتنظيم المنتجات بسهولة.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {companies.map((comp) => (
                  <div
                    key={comp.id}
                    className="bg-[#F9FAFB] border border-[#EBECEF] rounded-2xl p-3 flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-12 h-12 rounded-xl bg-white border border-[#EBECEF] overflow-hidden flex items-center justify-center shrink-0">
                        {comp.imageUrl ? (
                          <img src={comp.imageUrl} alt={comp.name} className="w-full h-full object-contain p-1" />
                        ) : (
                          <span className="font-black text-base text-[#181A20]">
                            {comp.name.trim().charAt(0) || 'ش'}
                          </span>
                        )}
                      </div>
                      <div className="min-w-0">
                        <div className="font-black text-sm text-[#181A20] truncate">{comp.name}</div>
                        <div className="flex items-center gap-2 mt-0.5 text-[11px] text-[#717784]">
                          <span>الترتيب: {formatNumber(comp.displayOrder)}</span>
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              comp.isActive
                                ? 'bg-[#E8F6EE] text-[#219653]'
                                : 'bg-gray-200 text-gray-600'
                            }`}
                          >
                            {comp.isActive ? 'مفعّلة' : 'مخفية'}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        type="button"
                        onClick={() => setEditingCompany({ ...comp })}
                        className="p-2 rounded-xl bg-[#FFF9DB] text-[#181A20] hover:bg-[#FFD000]"
                        title="تعديل"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={async () => {
                          if (confirm(`حذف الشركة «${comp.name}»؟ سيتم فك ارتباطها بالمنتجات دون حذف المنتجات نفسها.`)) {
                            try {
                              await deleteCompanyDoc(comp.id);
                              showToast('تم حذف الشركة', 'info');
                            } catch (err) {
                              showToast(toArabicErrorMessage(err, 'تعذر حذف الشركة.'), 'error');
                            }
                          }
                        }}
                        className="p-2 rounded-xl bg-[#FDECEC] text-[#E53935] hover:bg-red-100"
                        title="حذف"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 2. Contact & About Store */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader('contact', 'بيانات التواصل وصفحة عن المتجر', 'روابط الفيس بوك والصفحات وأرقام هواتف القطاعي والجملة')}
        {openKey === 'contact' && (
          <div className="p-5 pt-2 border-t border-[#EBECEF] space-y-4 text-xs">
            <textarea
              rows={2}
              value={form.aboutStoreDescription}
              onChange={(e) => setForm({ ...form, aboutStoreDescription: e.target.value })}
              placeholder="نبذة تعريفية عن الشركة والمتجر..."
              className="w-full p-3 rounded-xl border border-[#EBECEF]"
            />
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <input value={form.storeAddress} onChange={(e) => setForm({ ...form, storeAddress: e.target.value })} placeholder="عنوان مقر المتجر" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input value={form.supportPhone} onChange={(e) => setForm({ ...form, supportPhone: e.target.value })} placeholder="رقم الدعم الرئيسي" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input value={form.whatsappNumber} onChange={(e) => setForm({ ...form, whatsappNumber: e.target.value })} placeholder="رقم واتساب الرسمي" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input value={form.retailFacebookUrl} onChange={(e) => setForm({ ...form, retailFacebookUrl: e.target.value })} placeholder="رابط صفحة فيسبوك القطاعي" className="h-10 px-3 rounded-xl border border-[#EBECEF]" dir="ltr" />
              <input value={form.wholesaleFacebookUrl} onChange={(e) => setForm({ ...form, wholesaleFacebookUrl: e.target.value })} placeholder="رابط صفحة فيسبوك الجملة" className="h-10 px-3 rounded-xl border border-[#EBECEF]" dir="ltr" />
            </div>

            {/* Dynamic Social Pages */}
            <div className="space-y-2">
              <div className="flex items-center justify-between font-bold">
                <span>صفحات تواصل إضافية</span>
                <button
                  type="button"
                  onClick={() => setForm({ ...form, socialPages: [...form.socialPages, { title: '', url: '', channel: 'retail' }] })}
                  className="text-[#1565C0] inline-flex items-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" /> إضافة صفحة
                </button>
              </div>
              {form.socialPages.map((sp, idx) => (
                <div key={idx} className="grid grid-cols-1 sm:grid-cols-4 gap-2">
                  <input value={sp.title} onChange={(e) => {
                    const next = [...form.socialPages]; next[idx] = { ...sp, title: e.target.value }; setForm({ ...form, socialPages: next });
                  }} placeholder="اسم الصفحة (تيك توك/انستجرام)" className="h-9 px-2.5 rounded-lg border border-[#EBECEF]" />
                  <input value={sp.url} onChange={(e) => {
                    const next = [...form.socialPages]; next[idx] = { ...sp, url: e.target.value }; setForm({ ...form, socialPages: next });
                  }} placeholder="الرابط https://..." dir="ltr" className="sm:col-span-2 h-9 px-2.5 rounded-lg border border-[#EBECEF]" />
                  <div className="flex gap-1">
                    <select value={sp.channel} onChange={(e) => {
                      const next = [...form.socialPages]; next[idx] = { ...sp, channel: e.target.value as StoreContactChannel }; setForm({ ...form, socialPages: next });
                    }} className="flex-1 h-9 px-2 rounded-lg border border-[#EBECEF]">
                      <option value="retail">قطاعي</option>
                      <option value="wholesale">جملة</option>
                      <option value="both">مشترك</option>
                    </select>
                    <button type="button" onClick={() => setForm({ ...form, socialPages: form.socialPages.filter((_, i) => i !== idx) })} className="p-2 text-[#E53935]">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {/* Dynamic Retail Phones */}
            <div className="space-y-2">
              <div className="flex items-center justify-between font-bold">
                <span>أرقام هواتف القطاعي</span>
                <button type="button" onClick={() => setForm({ ...form, retailPhones: [...form.retailPhones, { label: '', phoneNumber: '', channel: 'retail' }] })} className="text-[#1565C0]">+ إضافة رقم</button>
              </div>
              {form.retailPhones.map((ph, idx) => (
                <div key={idx} className="flex gap-2">
                  <input value={ph.label} onChange={(e) => { const next = [...form.retailPhones]; next[idx] = { ...ph, label: e.target.value }; setForm({ ...form, retailPhones: next }); }} placeholder="التسمية (مبيعات ١)" className="flex-1 h-9 px-2.5 rounded-lg border border-[#EBECEF]" />
                  <input value={ph.phoneNumber} onChange={(e) => { const next = [...form.retailPhones]; next[idx] = { ...ph, phoneNumber: e.target.value }; setForm({ ...form, retailPhones: next }); }} placeholder="رقم الهاتف" className="flex-1 h-9 px-2.5 rounded-lg border border-[#EBECEF]" />
                  <button type="button" onClick={() => setForm({ ...form, retailPhones: form.retailPhones.filter((_, i) => i !== idx) })} className="text-[#E53935]"><Trash2 className="w-4 h-4" /></button>
                </div>
              ))}
            </div>

            {/* Dynamic Wholesale Phones */}
            <div className="space-y-2">
              <div className="flex items-center justify-between font-bold">
                <span>أرقام هواتف الجملة</span>
                <button type="button" onClick={() => setForm({ ...form, wholesalePhones: [...form.wholesalePhones, { label: '', phoneNumber: '', channel: 'wholesale' }] })} className="text-[#1565C0]">+ إضافة رقم</button>
              </div>
              {form.wholesalePhones.map((ph, idx) => (
                <div key={idx} className="flex gap-2">
                  <input value={ph.label} onChange={(e) => { const next = [...form.wholesalePhones]; next[idx] = { ...ph, label: e.target.value }; setForm({ ...form, wholesalePhones: next }); }} placeholder="التسمية (مسؤول الجملة)" className="flex-1 h-9 px-2.5 rounded-lg border border-[#EBECEF]" />
                  <input value={ph.phoneNumber} onChange={(e) => { const next = [...form.wholesalePhones]; next[idx] = { ...ph, phoneNumber: e.target.value }; setForm({ ...form, wholesalePhones: next }); }} placeholder="رقم الهاتف" className="flex-1 h-9 px-2.5 rounded-lg border border-[#EBECEF]" />
                  <button type="button" onClick={() => setForm({ ...form, wholesalePhones: form.wholesalePhones.filter((_, i) => i !== idx) })} className="text-[#E53935]"><Trash2 className="w-4 h-4" /></button>
                </div>
              ))}
            </div>

            <button type="button" disabled={saving} onClick={() => saveSection('التواصل وعن المتجر')} className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black">
              <Save className="w-4 h-4" />
              <span>حفظ هذا القسم</span>
            </button>
          </div>
        )}
      </div>

      {/* 3. Hero & Home Sections */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader('hero', 'واجهة الترحيب وأقسام الشاشة الرئيسية', 'تخصيص نصوص الهيرو وإظهار أو إخفاء أقسام الرئيسية')}
        {openKey === 'hero' && (
          <div className="p-5 pt-2 border-t border-[#EBECEF] space-y-3 text-xs">
            <div className="flex flex-wrap gap-4 font-bold">
              <label className="flex items-center gap-1.5"><input type="checkbox" checked={form.showHeroSection} onChange={(e) => setForm({ ...form, showHeroSection: e.target.checked })} /> بطاقة الترحيب (Hero)</label>
              <label className="flex items-center gap-1.5"><input type="checkbox" checked={form.showFeaturedSection} onChange={(e) => setForm({ ...form, showFeaturedSection: e.target.checked })} /> المنتجات المميزة</label>
              <label className="flex items-center gap-1.5"><input type="checkbox" checked={form.showDealsSection} onChange={(e) => setForm({ ...form, showDealsSection: e.target.checked })} /> العروض والخصومات</label>
              <label className="flex items-center gap-1.5"><input type="checkbox" checked={form.showBestsellersSection} onChange={(e) => setForm({ ...form, showBestsellersSection: e.target.checked })} /> الأكثر مبيعًا</label>
              <label className="flex items-center gap-1.5"><input type="checkbox" checked={form.showRecommendedSection} onChange={(e) => setForm({ ...form, showRecommendedSection: e.target.checked })} /> المقترحة لك</label>
              <label className="flex items-center gap-1.5 text-[#1565C0]"><input type="checkbox" checked={form.showProductColors} onChange={(e) => setForm({ ...form, showProductColors: e.target.checked })} /> إظهار الألوان للعملاء</label>
              <label className="flex items-center gap-1.5 text-[#1565C0]"><input type="checkbox" checked={form.showProductSizes} onChange={(e) => setForm({ ...form, showProductSizes: e.target.checked })} /> إظهار المقاسات للعملاء</label>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              <input value={form.heroBadgeText} onChange={(e) => setForm({ ...form, heroBadgeText: e.target.value })} placeholder="شارة الترحيب" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input value={form.heroCtaText} onChange={(e) => setForm({ ...form, heroCtaText: e.target.value })} placeholder="نص زر الإجراء" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input value={form.heroTitleText} onChange={(e) => setForm({ ...form, heroTitleText: e.target.value })} placeholder="العنوان الرئيسي" className="sm:col-span-2 h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input value={form.heroSubtitleText} onChange={(e) => setForm({ ...form, heroSubtitleText: e.target.value })} placeholder="العنوان الفرعي" className="sm:col-span-2 h-10 px-3 rounded-xl border border-[#EBECEF]" />
            </div>
            <button type="button" disabled={saving} onClick={() => saveSection('أقسام الرئيسية')} className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black">
              <Save className="w-4 h-4" />
              <span>حفظ هذا القسم</span>
            </button>
          </div>
        )}
      </div>

      {/* 3b. Product Card Design & Discount Badge Style */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader(
          'product_card_style',
          'تصميم كروت المنتجات وشارة الخصم',
          'إطار ذهبي أو لون مخصص • شكل الكارت (دائري، مستطيل، هندسي) • أشكال شارة الخصم',
        )}
        {openKey === 'product_card_style' && (
          <ProductCardStyleAdminSection
            form={form}
            saving={saving}
            onChange={(patch) => setForm({ ...form, ...patch })}
            onSave={() => saveSection('تصميم كروت المنتجات')}
          />
        )}
      </div>

      {/* 4. Category Display Styles & Company Offer Badge Style */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader('categories_style', 'شكل عرض الفئات وعروض الشركات', 'تغيير نمط الفئات وتصميم شارة «عرض خاص» للشركات في أي وقت')}
        {openKey === 'categories_style' && (
          <div className="p-5 pt-2 border-t border-[#EBECEF] space-y-4 text-xs">
            <div className="space-y-2">
              <div className="font-black text-sm text-[#181A20]">تصميم شارة «عرض خاص» لمنتجات الشركات</div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {COMPANY_OFFER_BADGE_STYLE_OPTIONS.map((opt) => {
                  const selected = (form.companyOfferBadgeStyle || 'gold_ticket') === opt.code;
                  return (
                    <button
                      key={opt.code}
                      type="button"
                      onClick={() =>
                        setForm({ ...form, companyOfferBadgeStyle: opt.code as CompanyOfferBadgeStyle })
                      }
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
                      <p className="text-[11px] text-[#717784]">{opt.subtitle}</p>
                      <div className="p-2 rounded-lg bg-[#F9FAFB] flex items-center">
                        <CompanyOfferBadge discountPercent={20} style={opt.code} />
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-[#EBECEF]">
              <div>
                <label className="block font-bold mb-1">نمط الفئات في الرئيسية</label>
                <select value={form.homeCategoryStyle} onChange={(e) => setForm({ ...form, homeCategoryStyle: e.target.value as HomeCategoryStyle })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]">
                  {HOME_CATEGORY_STYLE_OPTIONS.map((o) => <option key={o.code} value={o.code}>{o.label} — {o.subtitle}</option>)}
                </select>
              </div>
              <div>
                <label className="block font-bold mb-1">نمط صفحة الفئات الكاملة</label>
                <select value={form.categoriesTabStyle} onChange={(e) => setForm({ ...form, categoriesTabStyle: e.target.value as CategoriesTabStyle })} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]">
                  {CATEGORIES_TAB_STYLE_OPTIONS.map((o) => <option key={o.code} value={o.code}>{o.label} — {o.subtitle}</option>)}
                </select>
              </div>
            </div>
            <button type="button" disabled={saving} onClick={() => saveSection('أشكال العرض وعروض الشركات')} className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black">
              <Save className="w-4 h-4" />
              <span>حفظ هذا القسم</span>
            </button>
          </div>
        )}
      </div>

      {/* 5. Policies & Google Play Compliance */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader('policies', 'السياسات القانونية وتجهيز Google Play', 'سياسة الخصوصية، الاسترجاع، الشروط، والروابط الخارجية')}
        {openKey === 'policies' && (
          <div className="p-5 pt-2 border-t border-[#EBECEF] space-y-3 text-xs">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              <input value={form.privacyPolicyUrl} onChange={(e) => setForm({ ...form, privacyPolicyUrl: e.target.value })} placeholder="رابط سياسة الخصوصية الخارجي" dir="ltr" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input value={form.returnPolicyUrl} onChange={(e) => setForm({ ...form, returnPolicyUrl: e.target.value })} placeholder="رابط سياسة الاسترجاع الخارجي" dir="ltr" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
              <input value={form.accountDeletionUrl} onChange={(e) => setForm({ ...form, accountDeletionUrl: e.target.value })} placeholder="رابط صفحة حذف الحساب" dir="ltr" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
            </div>
            <div>
              <label className="block font-bold mb-1">نص سياسة الخصوصية</label>
              <textarea rows={4} value={form.privacyPolicyText} onChange={(e) => setForm({ ...form, privacyPolicyText: e.target.value })} className="w-full p-3 rounded-xl border border-[#EBECEF]" />
            </div>
            <div>
              <label className="block font-bold mb-1">نص سياسة الاسترجاع والاستبدال</label>
              <textarea rows={4} value={form.returnPolicyText} onChange={(e) => setForm({ ...form, returnPolicyText: e.target.value })} className="w-full p-3 rounded-xl border border-[#EBECEF]" />
            </div>
            <div>
              <label className="block font-bold mb-1">نص الشروط والأحكام</label>
              <textarea rows={4} value={form.termsOfServiceText} onChange={(e) => setForm({ ...form, termsOfServiceText: e.target.value })} className="w-full p-3 rounded-xl border border-[#EBECEF]" />
            </div>
            <button type="button" disabled={saving} onClick={() => saveSection('السياسات والامتثال')} className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black">
              <Save className="w-4 h-4" />
              <span>حفظ هذا القسم</span>
            </button>
          </div>
        )}
      </div>

      {/* 6. Broadcast Notification */}
      <div className="bg-white border border-[#EBECEF] rounded-2xl overflow-hidden">
        {renderAccordionHeader('broadcast', 'إرسال إشعار جماعي للعملاء', 'تصل الإشعارات لحظيًا لتطبيق الأندرويد ومتصفح الويب معًا')}
        {openKey === 'broadcast' && (
          <form onSubmit={handleBroadcast} className="p-5 pt-2 border-t border-[#EBECEF] space-y-3 text-xs">
            <select value={broadcastTarget} onChange={(e) => setBroadcastTarget(e.target.value as 'all' | 'wholesale' | 'retail')} className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]">
              <option value="all">جميع العملاء المسجلين ({formatNumber(users.length)})</option>
              <option value="wholesale">عملاء الجملة المعتمدون فقط</option>
              <option value="retail">العملاء الأفراد فقط</option>
            </select>
            <input value={broadcastTitle} onChange={(e) => setBroadcastTitle(e.target.value)} placeholder="عنوان الإشعار *" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]" required />
            <textarea rows={2} value={broadcastMsg} onChange={(e) => setBroadcastMsg(e.target.value)} placeholder="نص الرسالة *" className="w-full p-3 rounded-xl border border-[#EBECEF]" required />
            <button type="submit" disabled={sendingBroadcast} className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-[#181A20] text-[#FFD000] font-black">
              <Megaphone className="w-4 h-4" />
              <span>{sendingBroadcast ? 'جاري الإرسال...' : 'إرسال الإشعار الآن'}</span>
            </button>
          </form>
        )}
      </div>
      {/* Company Add/Edit Modal */}
      {editingCompany && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              if (!editingCompany.name || editingCompany.name.trim().length < 2) {
                showToast('يرجى إدخال اسم الشركة (حرفان على الأقل).', 'error');
                return;
              }
              setSavingCompany(true);
              try {
                await saveCompanyDoc(editingCompany);
                showToast(
                  editingCompany.id ? 'تم حفظ تعديلات الشركة بنجاح' : 'تمت إضافة الشركة بنجاح',
                  'success',
                );
                setEditingCompany(null);
              } catch (err) {
                showToast(toArabicErrorMessage(err, 'تعذر حفظ الشركة.'), 'error');
              } finally {
                setSavingCompany(false);
              }
            }}
            className="bg-white rounded-3xl max-w-md w-full p-5 space-y-4 border border-[#EBECEF]"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-black text-base">
                {editingCompany.id ? 'تعديل بيانات الشركة' : 'إضافة شركة جديدة'}
              </h3>
              <button
                type="button"
                onClick={() => setEditingCompany(null)}
                className="text-xs font-bold text-[#717784]"
              >
                إغلاق
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold mb-1">شعار أو صورة الشركة (اختياري)</label>
                <ImageUploader
                  folder="aleman/categories"
                  urls={editingCompany.imageUrl ? [editingCompany.imageUrl] : []}
                  multiple={false}
                  label="رفع شعار الشركة"
                  onChange={(urls) => setEditingCompany({ ...editingCompany, imageUrl: urls[0] || '' })}
                />
              </div>
              <div>
                <label className="block font-bold mb-1">اسم الشركة أو الماركة *</label>
                <input
                  value={editingCompany.name || ''}
                  onChange={(e) => setEditingCompany({ ...editingCompany, name: e.target.value })}
                  placeholder="مثال: أوكسي، برسيل، جهينة..."
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                  required
                />
              </div>
              <div>
                <label className="block font-bold mb-1">ترتيب الشركة</label>
                <input
                  type="number"
                  min="0"
                  max="1000"
                  value={editingCompany.displayOrder ?? 0}
                  onChange={(e) =>
                    setEditingCompany({ ...editingCompany, displayOrder: Number(e.target.value) })
                  }
                  className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
                />
              </div>
              <label className="flex items-center gap-2 font-bold">
                <input
                  type="checkbox"
                  checked={editingCompany.isActive ?? true}
                  onChange={(e) => setEditingCompany({ ...editingCompany, isActive: e.target.checked })}
                />
                <span>تفعيل الشركة داخل النماذج والعروض</span>
              </label>
            </div>

            <button
              type="submit"
              disabled={savingCompany}
              className="w-full py-3 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
            >
              {savingCompany
                ? 'جاري الحفظ...'
                : editingCompany.id
                ? 'حفظ التعديلات'
                : 'إضافة الشركة'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
};
