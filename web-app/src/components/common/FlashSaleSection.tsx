import React, { useEffect, useState } from 'react';
import { Bolt, Clock, Lock, X, ShoppingBag } from 'lucide-react';
import type { FlashSale } from '../../types/models';
import {
  formatCountdown,
  formatDateTime,
  formatNumber,
  getFlashSaleRemainingMs,
  getFlashSaleStatus,
  getFlashUnitPrice,
} from '../../lib/formatters';
import { useStore } from '../../context/StoreContext';
import { ProductCard } from './ProductCard';

export const FlashSaleSection: React.FC = () => {
  const { flashSales, products, effectiveWholesaleActive, navigate } = useStore();
  const [nowMillis, setNowMillis] = useState(() => Date.now());
  const [selectedSale, setSelectedSale] = useState<FlashSale | null>(null);
  const [lockedNoticeSale, setLockedNoticeSale] = useState<FlashSale | null>(null);

  useEffect(() => {
    const id = window.setInterval(() => setNowMillis(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);

  if (!flashSales.length) return null;

  const productMap = new Map(products.map((p) => [p.id, p]));

  return (
    <section className="space-y-4">
      {/* Custom Al-Eman Flash Sale Header (never copying external brand style) */}
      <div className="flex flex-wrap items-center justify-between gap-2 bg-[#181A20] text-white px-4 py-3 rounded-2xl border border-[#FFD000]/40">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-[#FFD000] text-[#181A20] flex items-center justify-center shadow-sm">
            <Bolt className="w-6 h-6 fill-current" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-black text-base sm:text-lg tracking-wide text-[#FFD000]">
                FLASH SALE
              </span>
              <span className="text-xs px-2 py-0.5 rounded-full bg-white/10 text-white font-bold">
                عروض الإيمان الخاطفة
              </span>
            </div>
            <p className="text-xs text-white/75">
              أسعار حصرية محدودة الوقت — لا تؤثر على الأسعار الأساسية خارج العرض
            </p>
          </div>
        </div>
      </div>

      {/* Flash Sale Campaigns Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {flashSales.map((sale) => {
          const status = getFlashSaleStatus(sale, nowMillis);
          const remaining = getFlashSaleRemainingMs(sale, nowMillis);

          const isUpcoming = status === 'upcoming';
          const isEnded = status === 'ended';

          return (
            <button
              key={sale.id}
              type="button"
              onClick={() => {
                if (isUpcoming) {
                  setLockedNoticeSale(sale);
                } else if (!isEnded) {
                  setSelectedSale(sale);
                }
              }}
              className={`relative text-right rounded-2xl border p-4 transition-all flex flex-col justify-between gap-3 ${
                isEnded
                  ? 'bg-gray-100 border-gray-200 opacity-70 cursor-default'
                  : isUpcoming
                  ? 'bg-[#FFF9DB] border-[#FFD000] hover:shadow-md'
                  : 'bg-white border-[#E53935]/40 hover:border-[#E53935] hover:shadow-md'
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  {sale.badgeText && (
                    <span className="inline-block px-2.5 py-0.5 rounded-full text-[11px] font-extrabold bg-[#181A20] text-[#FFD000] mb-1.5">
                      {sale.badgeText}
                    </span>
                  )}
                  <h3 className="font-extrabold text-base text-[#181A20]">{sale.title}</h3>
                </div>

                {isUpcoming ? (
                  <div className="w-10 h-10 rounded-xl bg-[#181A20] text-[#FFD000] flex items-center justify-center shrink-0">
                    <Lock className="w-5 h-5" />
                  </div>
                ) : isEnded ? (
                  <span className="px-2.5 py-1 rounded-full bg-gray-200 text-gray-700 text-xs font-bold">
                    تم انتهاء العرض
                  </span>
                ) : (
                  <span className="px-2.5 py-1 rounded-full bg-[#E53935] text-white text-xs font-extrabold animate-pulse">
                    مفتوح الآن ⚡
                  </span>
                )}
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-black/5 text-xs">
                {isUpcoming ? (
                  <div className="flex items-center gap-1.5 text-[#181A20] font-bold">
                    <Clock className="w-4 h-4 text-[#E53935]" />
                    <span>يفتح خلال: {formatCountdown(remaining)}</span>
                  </div>
                ) : isEnded ? (
                  <span className="text-[#717784] font-semibold">انتهت فترة هذا العرض</span>
                ) : (
                  <div className="flex items-center gap-1.5 text-[#E53935] font-extrabold">
                    <Clock className="w-4 h-4" />
                    <span>ينتهي خلال: {formatCountdown(remaining)}</span>
                  </div>
                )}

                {!isUpcoming && !isEnded && (
                  <span className="text-[#1565C0] font-bold flex items-center gap-1">
                    <ShoppingBag className="w-3.5 h-3.5" />
                    {formatNumber(sale.items.length)} منتج
                  </span>
                )}
              </div>
            </button>
          );
        })}
      </div>

      {/* Upcoming Locked Modal ("تفتح قريبًا" without leaking products) */}
      {lockedNoticeSale && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 text-center space-y-4 border border-[#FFD000] shadow-xl">
            <div className="w-16 h-16 mx-auto rounded-2xl bg-[#FFF9DB] border border-[#FFD000] flex items-center justify-center text-[#181A20]">
              <Lock className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-black text-[#181A20]">ستفتح هذه الحملة قريبًا</h3>
            <p className="text-sm text-[#717784] leading-relaxed">
              عرض «{lockedNoticeSale.title}» مغلق حاليًا وسيتم الكشف عن المنتجات والأسعار الخاطفة فور بدء الموعد المحدد:
            </p>
            <div className="bg-[#181A20] text-[#FFD000] rounded-2xl py-3 px-4 font-black text-lg">
              {formatCountdown(getFlashSaleRemainingMs(lockedNoticeSale, nowMillis))}
            </div>
            <p className="text-xs text-[#717784]">
              موعد الفتح: {formatDateTime(lockedNoticeSale.startsAt)}
            </p>
            <button
              type="button"
              onClick={() => setLockedNoticeSale(null)}
              className="w-full py-2.5 rounded-xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-bold text-sm"
            >
              حسنًا، سأعود لاحقًا
            </button>
          </div>
        </div>
      )}

      {/* Active Flash Sale Products Modal */}
      {selectedSale && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-[#F9FAFB] rounded-3xl max-w-5xl w-full max-h-[88vh] flex flex-col overflow-hidden border border-[#EBECEF] shadow-2xl">
            <div className="bg-[#181A20] text-white px-5 py-4 flex items-center justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <Bolt className="w-5 h-5 text-[#FFD000] fill-current" />
                  <h3 className="font-black text-lg text-[#FFD000]">{selectedSale.title}</h3>
                </div>
                <p className="text-xs text-white/80 mt-0.5">
                  ينتهي خلال: {formatCountdown(getFlashSaleRemainingMs(selectedSale, nowMillis))}
                </p>
              </div>
              <button
                type="button"
                aria-label="إغلاق"
                onClick={() => setSelectedSale(null)}
                className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 sm:p-5 overflow-y-auto grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
              {selectedSale.items.map((item) => {
                const prod = productMap.get(item.productId);
                if (!prod) return null;
                const price = getFlashUnitPrice(selectedSale, prod.id, effectiveWholesaleActive);
                return (
                  <ProductCard
                    key={prod.id}
                    product={prod}
                    flashPrice={price}
                    flashSaleId={selectedSale.id}
                    onOpen={() => {
                      setSelectedSale(null);
                      navigate({ name: 'product', productId: prod.id, flashSaleId: selectedSale.id });
                    }}
                  />
                );
              })}
            </div>
          </div>
        </div>
      )}
    </section>
  );
};
