import React, { useState } from 'react';
import { ArrowRight, ExternalLink, ShieldCheck, Trash2 } from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import type { StorePolicyTab } from '../../types/models';

export const StorePoliciesPage: React.FC<{ initialTab?: StorePolicyTab }> = ({ initialTab = 'privacy' }) => {
  const { storeSettings, firebaseUser, goBack, navigate } = useStore();
  const [activeTab, setActiveTab] = useState<StorePolicyTab>(initialTab);

  const policyText =
    activeTab === 'privacy'
      ? storeSettings.privacyPolicyText
      : activeTab === 'return'
      ? storeSettings.returnPolicyText
      : storeSettings.termsOfServiceText;

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

      <div className="bg-[#181A20] text-white rounded-3xl p-6 flex items-center gap-3">
        <div className="w-12 h-12 rounded-2xl bg-[#FFD000] text-[#181A20] flex items-center justify-center shrink-0">
          <ShieldCheck className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-xl font-black text-[#FFD000]">السياسات والامتثال والخصوصية</h1>
          <p className="text-xs text-white/80">
            متجر {storeSettings.storeName} • متوافق بالكامل مع معايير Google Play وحماية بيانات المستخدمين
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap gap-2">
        {(
          [
            { code: 'privacy', label: 'سياسة الخصوصية' },
            { code: 'return', label: 'الاسترجاع والاستبدال' },
            { code: 'terms', label: 'الشروط والأحكام' },
          ] as Array<{ code: StorePolicyTab; label: string }>
        ).map((tab) => (
          <button
            key={tab.code}
            type="button"
            onClick={() => setActiveTab(tab.code)}
            className={`px-4 py-2 rounded-xl text-xs font-black border transition ${
              activeTab === tab.code
                ? 'bg-[#FFD000] border-[#FFD000] text-[#181A20]'
                : 'bg-white border-[#EBECEF] text-[#717784]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="bg-white border border-[#EBECEF] rounded-3xl p-6 space-y-4">
        <div className="text-sm text-[#181A20] leading-loose whitespace-pre-line">{policyText}</div>

        {/* External Web Policy Links if configured */}
        {(storeSettings.privacyPolicyUrl || storeSettings.returnPolicyUrl || storeSettings.accountDeletionUrl) && (
          <div className="pt-4 border-t border-[#EBECEF] flex flex-wrap gap-2">
            {storeSettings.privacyPolicyUrl && (
              <a
                href={storeSettings.privacyPolicyUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold hover:border-[#FFD000]"
              >
                <ExternalLink className="w-3.5 h-3.5" />
                <span>رابط سياسة الخصوصية الخارجي</span>
              </a>
            )}
            {storeSettings.returnPolicyUrl && (
              <a
                href={storeSettings.returnPolicyUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold hover:border-[#FFD000]"
              >
                <ExternalLink className="w-3.5 h-3.5" />
                <span>رابط سياسة الاسترجاع الخارجي</span>
              </a>
            )}
            {storeSettings.accountDeletionUrl && (
              <a
                href={storeSettings.accountDeletionUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold hover:border-[#FFD000]"
              >
                <ExternalLink className="w-3.5 h-3.5" />
                <span>رابط حذف الحساب الخارجي</span>
              </a>
            )}
          </div>
        )}
      </div>

      {firebaseUser && (
        <div className="bg-[#FDECEC]/60 border border-[#E53935]/30 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
          <div className="text-xs font-bold text-[#181A20]">
            يمكنك حذف حسابك نهائيًا من إعدادات صفحة حسابي في أي وقت.
          </div>
          <button
            type="button"
            onClick={() => navigate({ name: 'account' })}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#E53935] text-white text-xs font-black"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>الانتقال لحذف الحساب</span>
          </button>
        </div>
      )}
    </div>
  );
};
