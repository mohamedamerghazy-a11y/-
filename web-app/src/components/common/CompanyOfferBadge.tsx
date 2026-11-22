import React from 'react';
import { BadgeCheck, Sparkles, Tag, Zap } from 'lucide-react';
import type { CompanyOfferBadgeStyle } from '../../types/models';
import { formatNumber } from '../../lib/formatters';

interface CompanyOfferBadgeProps {
  discountPercent: number;
  style?: CompanyOfferBadgeStyle;
  compact?: boolean;
}

export const CompanyOfferBadge: React.FC<CompanyOfferBadgeProps> = ({
  discountPercent,
  style = 'gold_ticket',
  compact = false,
}) => {
  if (!discountPercent || discountPercent <= 0) return null;
  const pct = formatNumber(discountPercent);

  switch (style) {
    case 'crimson_ribbon':
      return (
        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-tl-2xl rounded-br-2xl rounded-tr-sm rounded-bl-sm bg-gradient-to-l from-[#FB8C00] via-[#E53935] to-[#C62828] text-white border border-white/40 shadow-sm shrink-0">
          <Tag className="w-3.5 h-3.5 shrink-0" />
          {!compact && <span className="text-[11px] font-extrabold">عرض خاص</span>}
          <span className="px-1.5 py-0.5 rounded-full bg-white text-[#C62828] text-[10px] font-black leading-none">
            -{pct}٪
          </span>
        </div>
      );

    case 'emerald_glass':
      return (
        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-gradient-to-l from-[#00BFA5] via-[#00897B] to-[#005B4F] text-white border border-[#A7FFEB]/70 shadow-sm shrink-0">
          <Zap className="w-3.5 h-3.5 text-[#FFEA00] shrink-0" />
          {!compact && <span className="text-[11px] font-bold">عرض خاص</span>}
          <span className="px-2 py-0.5 rounded-full bg-white/95 text-[#00695C] text-[10px] font-black leading-none">
            خصم {pct}٪
          </span>
        </div>
      );

    case 'royal_shield':
      return (
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-gradient-to-l from-[#7B1FA2] via-[#4527A0] to-[#1F1663] text-[#FFF8E1] border border-[#FFD54F] shadow-sm shrink-0">
          <BadgeCheck className="w-3.5 h-3.5 text-[#FFD54F] shrink-0" />
          <span className="text-[11px] font-black">
            {compact ? `خصم ${pct}٪` : `عرض خاص • ${pct}٪`}
          </span>
        </div>
      );

    case 'sunburst_stamp':
      return (
        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-gradient-to-l from-[#FFC107] to-[#FFD000] text-[#181A20] border-[1.5px] border-[#181A20] shadow-xs shrink-0">
          {!compact && <span className="text-[11px] font-black">عرض خاص</span>}
          <span className="px-1.5 py-0.5 rounded-md bg-[#181A20] text-[#FFD000] text-[10px] font-black leading-none">
            -{pct}٪
          </span>
        </div>
      );

    case 'gold_ticket':
    default:
      return (
        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-[#171A22] text-white border border-[#FFD000] shadow-sm shrink-0">
          <Sparkles className="w-3.5 h-3.5 text-[#FFD000] shrink-0" />
          {!compact && <span className="text-[11px] font-bold">عرض خاص</span>}
          <span className="px-1.5 py-0.5 rounded-md bg-[#FFD000] text-[#181A20] text-[10px] font-black leading-none">
            خصم {pct}٪
          </span>
        </div>
      );
  }
};
