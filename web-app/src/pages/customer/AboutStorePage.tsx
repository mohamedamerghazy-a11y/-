import React from 'react';
import {
  ArrowRight,
  ExternalLink,
  FileText,
  Globe,
  MapPin,
  MessageCircle,
  Phone,
  Share2,
  Store,
} from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import { formatDigits, normalizeWhatsAppPhone } from '../../lib/formatters';

export const AboutStorePage: React.FC = () => {
  const { storeSettings, goBack, navigate } = useStore();

  const openUrl = (url: string) => {
    const target = url.startsWith('http') ? url : `https://${url}`;
    window.open(target, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="max-w-4xl mx-auto px-3 sm:px-6 py-5 space-y-5">
      <button
        type="button"
        onClick={goBack}
        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white border border-[#EBECEF] text-xs font-bold hover:bg-[#FFF9DB]"
      >
        <ArrowRight className="w-4 h-4" />
        <span>رجوع</span>
      </button>

      {/* Store Hero Card */}
      <div className="bg-[#181A20] text-white rounded-3xl p-6 space-y-2 border border-[#FFD000]/30">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-[#FFD000] text-[#181A20] flex items-center justify-center">
            <Store className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-black text-[#FFD000]">{storeSettings.storeName}</h1>
            <p className="text-xs text-white/80">{storeSettings.tagline}</p>
          </div>
        </div>
        {storeSettings.aboutStoreDescription && (
          <p className="text-sm text-white/90 leading-relaxed pt-2 whitespace-pre-line">
            {storeSettings.aboutStoreDescription}
          </p>
        )}
        {storeSettings.storeAddress && (
          <div className="flex items-center gap-2 text-xs text-[#FFD000] pt-1">
            <MapPin className="w-4 h-4 shrink-0" />
            <span>{storeSettings.storeAddress}</span>
          </div>
        )}
      </div>

      {/* Facebook Channels (Retail & Wholesale) */}
      {(storeSettings.retailFacebookUrl || storeSettings.wholesaleFacebookUrl) && (
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-5 space-y-3">
          <h2 className="font-black text-base flex items-center gap-2">
            <Share2 className="w-4 h-4 text-[#1565C0]" />
            <span>صفحات فيسبوك الرسمية</span>
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {storeSettings.retailFacebookUrl && (
              <button
                type="button"
                onClick={() => openUrl(storeSettings.retailFacebookUrl)}
                className="p-4 rounded-2xl bg-[#EBF4FF] border border-[#1565C0]/30 flex items-center justify-between text-right hover:bg-blue-100/70 transition"
              >
                <div>
                  <div className="font-extrabold text-sm text-[#1565C0]">صفحة القطاعي</div>
                  <div className="text-[11px] text-[#717784] mt-0.5">عروض وخصومات الأفراد</div>
                </div>
                <ExternalLink className="w-4 h-4 text-[#1565C0]" />
              </button>
            )}
            {storeSettings.wholesaleFacebookUrl && (
              <button
                type="button"
                onClick={() => openUrl(storeSettings.wholesaleFacebookUrl)}
                className="p-4 rounded-2xl bg-[#FFF9DB] border border-[#FFD000] flex items-center justify-between text-right hover:bg-amber-100/70 transition"
              >
                <div>
                  <div className="font-extrabold text-sm text-[#181A20]">صفحة الجملة</div>
                  <div className="text-[11px] text-[#717784] mt-0.5">مخصص للتجار والموزعين</div>
                </div>
                <ExternalLink className="w-4 h-4 text-[#181A20]" />
              </button>
            )}
          </div>
        </div>
      )}

      {/* Custom Social Links */}
      {storeSettings.socialPages.length > 0 && (
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-5 space-y-3">
          <h2 className="font-black text-base flex items-center gap-2">
            <Globe className="w-4 h-4" />
            <span>قنوات التواصل والصفحات</span>
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {storeSettings.socialPages.map((page, idx) => (
              <button
                key={`${page.title}-${idx}`}
                type="button"
                onClick={() => openUrl(page.url)}
                className="p-3.5 rounded-2xl bg-[#F9FAFB] border border-[#EBECEF] hover:border-[#FFD000] flex items-center justify-between text-right"
              >
                <div>
                  <div className="font-bold text-sm">{page.title}</div>
                  <span className="text-[11px] text-[#717784]">
                    {page.channel === 'wholesale' ? 'جملة' : page.channel === 'both' ? 'قطاعي وجملة' : 'قطاعي'}
                  </span>
                </div>
                <ExternalLink className="w-4 h-4 text-[#717784]" />
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Retail & Wholesale Phones */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {(storeSettings.retailPhones.length > 0 || storeSettings.supportPhone) && (
          <div className="bg-white border border-[#EBECEF] rounded-3xl p-5 space-y-3">
            <h2 className="font-black text-base flex items-center gap-2">
              <Phone className="w-4 h-4 text-[#219653]" />
              <span>أرقام خدمة العملاء</span>
            </h2>
            <div className="space-y-2">
              {storeSettings.supportPhone && (
                <a
                  href={`tel:${storeSettings.supportPhone}`}
                  className="flex items-center justify-between p-3 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] hover:border-[#FFD000] text-sm font-bold"
                >
                  <span>الدعم الرئيسي</span>
                  <span dir="ltr">{formatDigits(storeSettings.supportPhone)}</span>
                </a>
              )}
              {storeSettings.retailPhones.map((entry, idx) => (
                <a
                  key={`${entry.phoneNumber}-${idx}`}
                  href={`tel:${entry.phoneNumber}`}
                  className="flex items-center justify-between p-3 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] hover:border-[#FFD000] text-sm font-bold"
                >
                  <span>{entry.label || 'هاتف التواصل'}</span>
                  <span dir="ltr">{formatDigits(entry.phoneNumber)}</span>
                </a>
              ))}
            </div>
          </div>
        )}

        {(storeSettings.wholesalePhones.length > 0 || storeSettings.whatsappNumber) && (
          <div className="bg-white border border-[#EBECEF] rounded-3xl p-5 space-y-3">
            <h2 className="font-black text-base flex items-center gap-2">
              <MessageCircle className="w-4 h-4 text-[#1565C0]" />
              <span>أرقام الجملة وواتساب</span>
            </h2>
            <div className="space-y-2">
              {storeSettings.whatsappNumber && (
                <button
                  type="button"
                  onClick={() => openUrl(`https://wa.me/${normalizeWhatsAppPhone(storeSettings.whatsappNumber)}`)}
                  className="w-full flex items-center justify-between p-3 rounded-xl bg-[#E8F6EE] border border-[#219653]/30 text-sm font-bold text-[#219653]"
                >
                  <span>تواصل مباشر عبر واتساب</span>
                  <span dir="ltr">{formatDigits(storeSettings.whatsappNumber)}</span>
                </button>
              )}
              {storeSettings.wholesalePhones.map((entry, idx) => (
                <a
                  key={`${entry.phoneNumber}-${idx}`}
                  href={`tel:${entry.phoneNumber}`}
                  className="flex items-center justify-between p-3 rounded-xl bg-[#FFF9DB] border border-[#FFD000] text-sm font-bold"
                >
                  <span>{entry.label || 'مبيعات الجملة'}</span>
                  <span dir="ltr">{formatDigits(entry.phoneNumber)}</span>
                </a>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Policies Shortcut */}
      <button
        type="button"
        onClick={() => navigate({ name: 'policies', initialTab: 'privacy' })}
        className="w-full p-4 rounded-2xl bg-white border border-[#EBECEF] hover:border-[#FFD000] flex items-center justify-between text-right"
      >
        <div className="flex items-center gap-3">
          <FileText className="w-5 h-5 text-[#181A20]" />
          <div>
            <div className="font-black text-sm">السياسات والخصوصية وحقوق العميل</div>
            <div className="text-xs text-[#717784]">سياسة الخصوصية، سياسة الاسترجاع، الشروط والأحكام</div>
          </div>
        </div>
        <span className="text-xs font-extrabold text-[#1565C0]">عرض</span>
      </button>
    </div>
  );
};
