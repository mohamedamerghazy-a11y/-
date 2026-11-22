import React, { useRef, useState } from 'react';
import { Bluetooth, MessageCircle, Printer, X } from 'lucide-react';
import { displaySenderPhone, type Order } from '../../types/models';
import {
  buildWhatsAppInvoiceText,
  getReceiptSummaryData,
  openWhatsAppWithText,
  printOrderViaWebBluetooth,
  type ThermalPaperWidth,
} from '../../lib/printing';
import { formatCurrency, formatDigits, formatNumber, toArabicErrorMessage } from '../../lib/formatters';
import { useStore } from '../../context/StoreContext';

interface ThermalPrintModalProps {
  order: Order;
  onClose: () => void;
}

export const ThermalPrintModal: React.FC<ThermalPrintModalProps> = ({ order, onClose }) => {
  const { storeSettings, showToast } = useStore();
  const [paperWidth, setPaperWidth] = useState<ThermalPaperWidth>('80mm');
  const [btBusy, setBtBusy] = useState(false);
  const receiptRef = useRef<HTMLDivElement | null>(null);

  const storePhone = storeSettings.supportPhone || storeSettings.whatsappNumber || '';
  const summary = getReceiptSummaryData(order, storeSettings.storeName);

  const handleBrowserPrint = () => {
    window.print();
  };

  const handleBluetoothPrint = async () => {
    if (!receiptRef.current) return;
    setBtBusy(true);
    try {
      const printerName = await printOrderViaWebBluetooth(receiptRef.current, paperWidth);
      showToast(`تم إرسال الفاتورة إلى الطابعة: ${printerName}`, 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر الطباعة عبر البلوتوث.'), 'error');
    } finally {
      setBtBusy(false);
    }
  };

  const handleWhatsAppCustomer = () => {
    const text = buildWhatsAppInvoiceText(order, storeSettings.storeName, storePhone);
    openWhatsAppWithText(order.phone || null, text);
  };

  const handleWhatsAppAny = () => {
    const text = buildWhatsAppInvoiceText(order, storeSettings.storeName, storePhone);
    openWhatsAppWithText(null, text);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/55 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-[#F9FAFB] rounded-3xl max-w-2xl w-full max-h-[92vh] flex flex-col overflow-hidden border border-[#EBECEF] shadow-2xl">
        {/* Header */}
        <div className="bg-[#181A20] text-white px-5 py-4 flex items-center justify-between">
          <div>
            <h3 className="font-black text-base text-[#FFD000]">
              معاينة وطباعة الفاتورة #{summary.orderNumber}
            </h3>
            <p className="text-xs text-white/75">
              اختر مقاس الرول الحراري ثم اطبع أو شارك الفاتورة عبر واتساب
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Paper Size Selector + Actions */}
        <div className="p-4 bg-white border-b border-[#EBECEF] flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-[#717784]">مقاس الورق:</span>
            {(['58mm', '80mm'] as ThermalPaperWidth[]).map((w) => (
              <button
                key={w}
                type="button"
                onClick={() => setPaperWidth(w)}
                className={`px-3 py-1.5 rounded-xl text-xs font-extrabold border transition ${
                  paperWidth === w
                    ? 'bg-[#FFD000] border-[#FFD000] text-[#181A20]'
                    : 'bg-[#F9FAFB] border-[#EBECEF] text-[#717784]'
                }`}
              >
                {w === '58mm' ? '58 مم (صغير)' : '80 مم (قياسي)'}
              </button>
            ))}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={handleBrowserPrint}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-bold hover:bg-[#282B33]"
            >
              <Printer className="w-4 h-4" />
              طباعة حرارية / PDF
            </button>
            <button
              type="button"
              disabled={btBusy}
              onClick={handleBluetoothPrint}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#EBF4FF] text-[#1565C0] text-xs font-bold hover:bg-blue-100 disabled:opacity-60"
            >
              <Bluetooth className="w-4 h-4" />
              {btBusy ? 'جاري الاتصال...' : 'طابعة بلوتوث'}
            </button>
            <button
              type="button"
              onClick={handleWhatsAppCustomer}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#E8F6EE] text-[#219653] text-xs font-bold hover:bg-emerald-100"
            >
              <MessageCircle className="w-4 h-4" />
              واتساب العميل
            </button>
            <button
              type="button"
              onClick={handleWhatsAppAny}
              className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white border border-[#EBECEF] text-xs font-bold hover:bg-gray-50"
            >
              مشاركة لشخص آخر
            </button>
          </div>
        </div>

        {/* Receipt Preview Area */}
        <div className="p-6 overflow-y-auto flex justify-center bg-[#EBECEF]/60">
          <div
            id="thermal-print-area"
            ref={receiptRef}
            style={{ width: paperWidth === '58mm' ? '280px' : '360px' }}
            className="bg-white text-[#181A20] p-4 rounded-2xl border border-[#EBECEF] shadow-md text-xs space-y-3"
          >
            {/* Brand Header */}
            <div className="bg-[#181A20] text-white rounded-xl p-3 text-center">
              <div className="text-base font-black text-[#FFD000]">{summary.storeName}</div>
              <div className="text-[11px] text-white/85 mt-0.5">
                فاتورة طلب وتجهيز • عروض الجملة والقطاعي
              </div>
            </div>

            {/* Order Meta */}
            <div className="bg-[#FFF9DB] border border-[#FFD000] rounded-xl p-2.5 space-y-1">
              <div className="font-black text-sm">رقم الطلب: #{summary.orderNumber}</div>
              <div className="text-[11px] text-[#717784]">التاريخ: {summary.date}</div>
              <div className="font-bold">
                الحالة: {summary.statusLabel} | {summary.fulfillmentLabel}
              </div>
            </div>

            {/* Customer Info */}
            <div className="bg-[#F9FAFB] border border-[#EBECEF] rounded-xl p-2.5 space-y-1">
              <div className="font-bold text-[#181A20]">
                {order.fulfillmentType === 'pickup' ? 'بيانات الاستلام من المتجر' : 'بيانات التوصيل'}
              </div>
              <div>
                العميل: <span className="font-bold">{order.recipientName || order.customerName}</span> ({summary.channelLabel})
              </div>
              {order.phone && <div>الهاتف: {formatDigits(order.phone)}</div>}
              <div>{order.fulfillmentType === 'pickup' ? 'مقر الاستلام' : 'العنوان'}: {summary.addressLine}</div>
              {order.landmark && <div>علامة مميزة: {order.landmark}</div>}
              <div>الدفع: {summary.paymentLabel}</div>
              {displaySenderPhone(order) && (
                <div>الرقم المحوَّل منه: <span dir="ltr">{formatDigits(displaySenderPhone(order))}</span></div>
              )}
              {order.notes && <div>ملاحظات: {order.notes}</div>}
            </div>

            {/* Items */}
            <div className="space-y-1.5">
              <div className="font-black border-b-2 border-[#181A20] pb-1">
                تفاصيل الأصناف ({formatNumber(order.items.length)})
              </div>
              {order.items.map((item, idx) => (
                <div key={`${item.productId}-${idx}`} className="py-1 border-b border-dashed border-[#EBECEF]">
                  <div className="font-bold">
                    {formatNumber(idx + 1)}. {item.productName}
                    {(item.selectedColor || item.selectedSize) && (
                      <span className="text-[10px] text-[#717784] mr-1">
                        ({[item.selectedColor && `لون: ${item.selectedColor}`, item.selectedSize && `مقاس: ${item.selectedSize}`].filter(Boolean).join(' • ')})
                      </span>
                    )}
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-[#717784] mt-0.5">
                    <span>
                      {formatNumber(item.quantity)} {item.unit || 'قطعة'} × {formatCurrency(item.unitPrice)}
                    </span>
                    <span className="font-extrabold text-[#181A20]">
                      {formatCurrency(item.lineTotal)}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* Totals */}
            <div className="space-y-1 pt-1">
              <div className="flex justify-between">
                <span>المجموع الفرعي</span>
                <span className="font-bold">{formatCurrency(order.subtotal)}</span>
              </div>
              {order.discountAmount > 0 && (
                <div className="flex justify-between text-[#E53935]">
                  <span>الخصم {order.couponCode ? `(${order.couponCode})` : ''}</span>
                  <span className="font-bold">- {formatCurrency(order.discountAmount)}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span>{order.fulfillmentType === 'pickup' ? 'الاستلام من المتجر' : 'رسوم التوصيل'}</span>
                <span className="font-bold">
                  {order.fulfillmentType === 'pickup' ? 'مجانًا' : formatCurrency(order.deliveryFee)}
                </span>
              </div>
            </div>

            <div className="bg-[#181A20] text-[#FFD000] rounded-xl px-3 py-2.5 flex items-center justify-between font-black text-sm">
              <span>الإجمالي المطلوب</span>
              <span>{formatCurrency(order.totalAmount)}</span>
            </div>

            <div className="text-center text-[11px] text-[#717784] pt-1">
              <div>شكرًا لتعاملكم مع {summary.storeName}</div>
              {storePhone && <div>للتواصل: {formatDigits(storePhone)}</div>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
