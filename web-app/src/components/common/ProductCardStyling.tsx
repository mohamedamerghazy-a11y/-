import React from 'react';
import { Sparkles, Tag } from 'lucide-react';
import {
  sanitizeCardHexColor,
  type ProductCardBorderStyle,
  type ProductCardShape,
  type ProductDiscountBadgeStyle,
} from '../../types/models';
import { formatNumber } from '../../lib/formatters';

export interface ProductCardVisualStyle {
  borderStyle: ProductCardBorderStyle;
  borderColor: string;
  shape: ProductCardShape;
  discountBadgeStyle: ProductDiscountBadgeStyle;
}

export const CUT_CORNER_CLIP_OUTER =
  'polygon(16px 0, calc(100% - 16px) 0, 100% 16px, 100% calc(100% - 16px), calc(100% - 16px) 100%, 16px 100%, 0 calc(100% - 16px), 0 16px)';

export const CUT_CORNER_CLIP_INNER =
  'polygon(15px 0, calc(100% - 15px) 0, 100% 15px, 100% calc(100% - 15px), calc(100% - 15px) 100%, 15px 100%, 0 calc(100% - 15px), 0 15px)';

export function getCardBorderGradient(
  borderStyle: ProductCardBorderStyle,
  customHex?: string,
): { background: string; paddingPx: number; shadowClass: string } {
  switch (borderStyle) {
    case 'gold':
      return {
        background: 'linear-gradient(135deg, #F9E58B 0%, #D4AF37 38%, #9C6D11 70%, #EAC857 100%)',
        paddingPx: 2,
        shadowClass: 'shadow-[0_6px_20px_-8px_rgba(212,175,55,0.45)]',
      };
    case 'emerald':
      return {
        background: 'linear-gradient(135deg, #34D399 0%, #059669 55%, #065F46 100%)',
        paddingPx: 2,
        shadowClass: 'shadow-[0_6px_18px_-8px_rgba(5,150,105,0.35)]',
      };
    case 'royal_blue':
      return {
        background: 'linear-gradient(135deg, #60A5FA 0%, #1D4ED8 55%, #1E3A8A 100%)',
        paddingPx: 2,
        shadowClass: 'shadow-[0_6px_18px_-8px_rgba(29,78,216,0.35)]',
      };
    case 'ruby':
      return {
        background: 'linear-gradient(135deg, #F87171 0%, #DC2626 55%, #991B1B 100%)',
        paddingPx: 2,
        shadowClass: 'shadow-[0_6px_18px_-8px_rgba(220,38,38,0.35)]',
      };
    case 'purple':
      return {
        background: 'linear-gradient(135deg, #A78BFA 0%, #7C3AED 55%, #5B21B6 100%)',
        paddingPx: 2,
        shadowClass: 'shadow-[0_6px_18px_-8px_rgba(124,58,237,0.35)]',
      };
    case 'charcoal':
      return {
        background: '#181A20',
        paddingPx: 2,
        shadowClass: 'shadow-[0_6px_18px_-10px_rgba(24,26,32,0.35)]',
      };
    case 'custom': {
      const safe = sanitizeCardHexColor(customHex);
      return {
        background: `linear-gradient(135deg, ${safe}CC 0%, ${safe} 55%, ${safe} 100%)`,
        paddingPx: 2,
        shadowClass: 'shadow-sm',
      };
    }
    case 'none':
      return {
        background: 'transparent',
        paddingPx: 0,
        shadowClass: 'shadow-[0_8px_24px_-10px_rgba(24,26,32,0.16)]',
      };
    case 'default':
    default:
      return {
        background: '#E8EAEE',
        paddingPx: 1,
        shadowClass: '',
      };
  }
}

export function getCardShapeClasses(shape: ProductCardShape): {
  outerRadius: string;
  innerRadius: string;
  useCutCorner: boolean;
} {
  switch (shape) {
    case 'circular':
      return {
        outerRadius: 'rounded-[28px]',
        innerRadius: 'rounded-[26px]',
        useCutCorner: false,
      };
    case 'rectangle':
      return {
        outerRadius: 'rounded-[4px]',
        innerRadius: 'rounded-[3px]',
        useCutCorner: false,
      };
    case 'cut_corner':
      return {
        outerRadius: '',
        innerRadius: '',
        useCutCorner: true,
      };
    case 'asymmetric':
      return {
        outerRadius: 'rounded-tr-[28px] rounded-bl-[28px] rounded-tl-[6px] rounded-br-[6px]',
        innerRadius: 'rounded-tr-[26px] rounded-bl-[26px] rounded-tl-[5px] rounded-br-[5px]',
        useCutCorner: false,
      };
    case 'arch':
      return {
        outerRadius: 'rounded-t-[36px] rounded-b-[10px]',
        innerRadius: 'rounded-t-[34px] rounded-b-[8px]',
        useCutCorner: false,
      };
    case 'rounded':
    default:
      return {
        outerRadius: 'rounded-2xl',
        innerRadius: 'rounded-[14px]',
        useCutCorner: false,
      };
  }
}

export const ProductDiscountBadge: React.FC<{
  discountPercent: number;
  style: ProductDiscountBadgeStyle;
}> = ({ discountPercent, style }) => {
  if (discountPercent <= 0) return null;
  const pct = formatNumber(discountPercent);

  switch (style) {
    case 'ribbon':
      return (
        <span
          className="inline-flex items-center text-[10px] sm:text-[11px] font-black text-white shadow-sm pr-2.5 pl-4 py-1"
          style={{
            background: 'linear-gradient(90deg, #FF5252 0%, #E53935 50%, #B71C1C 100%)',
            clipPath: 'polygon(100% 0, 0 0, 8px 50%, 0 100%, 100% 100%)',
            borderBottom: '2px solid rgba(249,229,139,0.8)',
          }}
        >
          خصم {pct}%
        </span>
      );

    case 'starburst':
      return (
        <div className="relative w-11 h-11 flex items-center justify-center drop-shadow-sm">
          <svg viewBox="0 0 100 100" className="w-full h-full">
            <defs>
              <radialGradient id="dealStarGrad" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stopColor="#FF5252" />
                <stop offset="70%" stopColor="#D32F2F" />
                <stop offset="100%" stopColor="#991B1B" />
              </radialGradient>
            </defs>
            <polygon
              points="50,3 59,17 75,10 75,26 91,27 83,42 97,50 83,58 91,73 75,74 75,90 59,83 50,97 41,83 25,90 25,74 9,73 17,58 3,50 17,42 9,27 25,26 25,10 41,17"
              fill="url(#dealStarGrad)"
              stroke="#FFD54F"
              strokeWidth="3"
            />
          </svg>
          <span className="absolute inset-0 flex flex-col items-center justify-center text-[9px] leading-[1.05] font-black text-white">
            <span>{pct}%</span>
            <span>خصم</span>
          </span>
        </div>
      );

    case 'price_tag':
      return (
        <span
          className="relative inline-flex items-center gap-1 text-[10px] sm:text-[11px] font-black text-white shadow-sm pl-2.5 pr-4 py-1"
          style={{
            background: 'linear-gradient(90deg, #10B981 0%, #059669 65%, #047857 100%)',
            clipPath: 'polygon(calc(100% - 10px) 0, 0 0, 0 100%, calc(100% - 10px) 100%, 100% 50%)',
          }}
        >
          <span className="w-1.5 h-1.5 rounded-full bg-white inline-block ml-0.5" />
          <span>خصم {pct}%</span>
        </span>
      );

    case 'gold_luxury':
      return (
        <span
          className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] sm:text-[11px] font-black shadow-sm"
          style={{
            background: '#181A20',
            color: '#F9E58B',
            border: '1.5px solid #D4AF37',
          }}
        >
          <Sparkles className="w-3 h-3 text-[#FFD000]" />
          <span>خصم {pct}%</span>
        </span>
      );

    case 'capsule_outline':
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] sm:text-[11px] font-black bg-white/95 text-[#E53935] border-[1.5px] border-[#E53935] shadow-xs">
          <Tag className="w-3 h-3" />
          <span>خصم {pct}%</span>
        </span>
      );

    case 'pill':
    default:
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-lg text-[10px] sm:text-[11px] font-black bg-[#E53935] text-white shadow-xs">
          خصم {pct}%
        </span>
      );
  }
};
