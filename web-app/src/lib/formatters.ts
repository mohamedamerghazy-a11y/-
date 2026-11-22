import type { Timestamp } from 'firebase/firestore';
import {
  BANNER_PLACEMENT_ORDER,
  type BrandShowcase,
  type FlashSale,
  type FlashSaleStatus,
  type NumeralFormat,
  type Product,
} from '../types/models';

const EASTERN_ARABIC_DIGITS = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];

let activeNumeralFormat: NumeralFormat = 'arabic';

export function setGlobalNumeralFormat(format: NumeralFormat) {
  activeNumeralFormat = format;
}

export function getGlobalNumeralFormat(): NumeralFormat {
  return activeNumeralFormat;
}

export function formatDigits(input: string | number, format: NumeralFormat = activeNumeralFormat): string {
  const str = String(input);
  if (format === 'english') {
    return str.replace(/[٠-٩]/g, (ch) => String(ch.charCodeAt(0) - 0x0660));
  }
  return str.replace(/\d/g, (d) => EASTERN_ARABIC_DIGITS[Number(d)] ?? d);
}

export function formatNumber(value: number, format: NumeralFormat = activeNumeralFormat): string {
  const formatted = new Intl.NumberFormat('en-US', {
    maximumFractionDigits: 2,
  }).format(value);
  return format === 'arabic' ? formatDigits(formatted.replace(/,/g, '٬'), 'arabic') : formatted;
}

export function formatCurrency(value: number, format: NumeralFormat = activeNumeralFormat): string {
  const rounded = Math.round(value * 100) / 100;
  const formatted = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: rounded % 1 === 0 ? 0 : 2,
    maximumFractionDigits: 2,
  }).format(rounded);
  const numStr = format === 'arabic' ? formatDigits(formatted.replace(/,/g, '٬'), 'arabic') : formatted;
  return `${numStr} ج.م`;
}

export function toMillis(ts?: Timestamp | Date | null): number {
  if (!ts) return 0;
  if (ts instanceof Date) return ts.getTime();
  if (typeof (ts as Timestamp).toDate === 'function') {
    return (ts as Timestamp).toDate().getTime();
  }
  if (typeof (ts as { seconds?: number }).seconds === 'number') {
    return ((ts as { seconds: number }).seconds || 0) * 1000;
  }
  return 0;
}

export function formatDateTime(ts?: Timestamp | Date | null, format: NumeralFormat = activeNumeralFormat): string {
  const ms = toMillis(ts);
  if (!ms) return 'حديث';
  const d = new Date(ms);
  const raw = d.toLocaleString('en-GB', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  });
  return formatDigits(raw, format);
}

export function formatCountdown(remainingMs: number, format: NumeralFormat = activeNumeralFormat): string {
  const totalSec = Math.max(0, Math.floor(remainingMs / 1000));
  const days = Math.floor(totalSec / 86400);
  const hours = Math.floor((totalSec % 86400) / 3600);
  const mins = Math.floor((totalSec % 3600) / 60);
  const secs = totalSec % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  const raw = days > 0 ? `${days}ي ${pad(hours)}:${pad(mins)}:${pad(secs)}` : `${pad(hours)}:${pad(mins)}:${pad(secs)}`;
  return formatDigits(raw, format);
}

export function normalizeWhatsAppPhone(rawPhone: string): string {
  const englishDigits = formatDigits(rawPhone, 'english').replace(/\D/g, '');
  if (englishDigits.startsWith('00')) return englishDigits.slice(2);
  if (englishDigits.startsWith('01') && englishDigits.length === 11) return `2${englishDigits}`;
  return englishDigits;
}

export function getEffectiveRetailPrice(product: Product): number {
  if (
    product.retailDiscountPrice &&
    product.retailDiscountPrice > 0 &&
    product.retailDiscountPrice < product.retailPrice
  ) {
    return product.retailDiscountPrice;
  }
  return product.retailPrice;
}

export function getDiscountPercent(product: Product): number {
  if (
    product.retailDiscountPrice &&
    product.retailDiscountPrice > 0 &&
    product.retailDiscountPrice < product.retailPrice &&
    product.retailPrice > 0
  ) {
    const pct = Math.round(((product.retailPrice - product.retailDiscountPrice) / product.retailPrice) * 100);
    return Math.min(99, Math.max(1, pct));
  }
  return 0;
}

export function getFlashSaleStatus(sale: FlashSale, nowMillis: number = Date.now()): FlashSaleStatus {
  const start = toMillis(sale.startsAt);
  const end = toMillis(sale.endsAt) || Number.MAX_SAFE_INTEGER;
  if (nowMillis < start) return 'upcoming';
  if (nowMillis >= end) return 'ended';
  return 'active';
}

export function getFlashSaleRemainingMs(sale: FlashSale, nowMillis: number = Date.now()): number {
  const status = getFlashSaleStatus(sale, nowMillis);
  const start = toMillis(sale.startsAt);
  const end = toMillis(sale.endsAt);
  if (status === 'upcoming') return Math.max(0, start - nowMillis);
  if (status === 'active') return Math.max(0, end - nowMillis);
  return 0;
}

export function getFlashUnitPrice(
  sale: FlashSale,
  productId: string,
  isWholesaleActive: boolean,
): number | null {
  const item = sale.items.find((i) => i.productId === productId);
  if (!item) return null;
  if (isWholesaleActive && item.flashWholesalePrice && item.flashWholesalePrice > 0) {
    return item.flashWholesalePrice;
  }
  if (item.flashRetailPrice > 0) {
    return item.flashRetailPrice;
  }
  return null;
}

export function roundTwo(val: number): number {
  return Math.round(val * 100) / 100;
}

export function matchesBrandShowcase(product: Product, showcase: BrandShowcase): boolean {
  const sCompanyId = showcase.companyId?.trim() || '';
  const pCompanyId = product.companyId?.trim() || '';
  if (sCompanyId && pCompanyId && sCompanyId === pCompanyId) return true;
  const pShowcaseId = product.brandShowcaseId?.trim() || '';
  if (pShowcaseId && pShowcaseId === showcase.id) return true;
  return false;
}

export function isExclusiveHiddenFromStore(product: Product): boolean {
  return Boolean(
    product.exclusiveToBrandShowcase &&
      (Boolean(product.companyId?.trim()) || Boolean(product.brandShowcaseId?.trim())),
  );
}

export function safeShowcaseDiscountPercent(showcase: Pick<BrandShowcase, 'discountPercent'>): number {
  const raw = Number(showcase.discountPercent || 0);
  if (!Number.isFinite(raw) || raw <= 0) return 0;
  return Math.min(95, Math.max(0, raw));
}

export function effectiveShowcaseDiscountPercent(
  product: Product,
  activeShowcases: BrandShowcase[],
): number {
  let maxPct = 0;
  for (const s of activeShowcases) {
    if (!s.isActive) continue;
    const pct = safeShowcaseDiscountPercent(s);
    if (pct > maxPct && matchesBrandShowcase(product, s)) {
      maxPct = pct;
    }
  }
  return maxPct;
}

export function effectiveRetailPriceWithShowcases(
  product: Product,
  activeShowcases: BrandShowcase[],
): number {
  const baseEffective = getEffectiveRetailPrice(product);
  const showcasePct = effectiveShowcaseDiscountPercent(product, activeShowcases);
  if (showcasePct <= 0 || product.retailPrice <= 0) return roundTwo(baseEffective);
  const showcasePrice = roundTwo(product.retailPrice * (1 - showcasePct / 100));
  if (showcasePrice > 0 && showcasePrice < baseEffective) return showcasePrice;
  return roundTwo(baseEffective);
}

export function effectiveDiscountPercentWithShowcases(
  product: Product,
  activeShowcases: BrandShowcase[],
): number {
  if (product.retailPrice <= 0) return 0;
  const effective = effectiveRetailPriceWithShowcases(product, activeShowcases);
  if (effective >= product.retailPrice) return 0;
  const pct = Math.round(((product.retailPrice - effective) / product.retailPrice) * 100);
  return Math.min(99, Math.max(1, pct));
}

export function effectiveUnitPriceWithShowcases(
  product: Product,
  quantity: number,
  isWholesaleApproved: boolean,
  activeShowcases: BrandShowcase[],
): number {
  const minQty = Math.max(1, product.wholesaleMinQty || 1);
  if (isWholesaleApproved && product.wholesalePrice > 0 && quantity >= minQty) {
    return roundTwo(product.wholesalePrice);
  }
  return effectiveRetailPriceWithShowcases(product, activeShowcases);
}

export function computeBannerlessShowcaseAlignLeftMap(
  showcases: BrandShowcase[],
): Record<string, boolean> {
  const ordered = showcases
    .filter((s) => s.isActive && !(s.bannerImageUrl && s.bannerImageUrl.trim().length > 0))
    .sort((a, b) => {
      const pA = BANNER_PLACEMENT_ORDER.indexOf(a.placement);
      const pB = BANNER_PLACEMENT_ORDER.indexOf(b.placement);
      const ordA = pA >= 0 ? pA : 99;
      const ordB = pB >= 0 ? pB : 99;
      if (ordA !== ordB) return ordA - ordB;
      if ((a.displayOrder ?? 0) !== (b.displayOrder ?? 0)) {
        return (a.displayOrder ?? 0) - (b.displayOrder ?? 0);
      }
      return toMillis(b.createdAt) - toMillis(a.createdAt);
    });

  const map: Record<string, boolean> = {};
  ordered.forEach((s, idx) => {
    map[s.id] = idx % 2 === 1;
  });
  return map;
}

export function toArabicErrorMessage(error: unknown, fallback: string): string {
  const msg = error instanceof Error ? error.message : String(error || '');
  if (msg.includes('permission-denied') || msg.includes('PERMISSION_DENIED')) {
    return 'ليس لديك الصلاحية الكافية لتنفيذ هذا الإجراء.';
  }
  if (msg.includes('network-request-failed') || msg.includes('unavailable')) {
    return 'تعذر الاتصال بالإنترنت، يرجى التحقق من الشبكة والمحاولة مرة أخرى.';
  }
  if (msg.includes('requires-recent-login')) {
    return 'يلزم إعادة تسجيل الدخول لحماية حسابك قبل إتمام هذا الإجراء.';
  }
  if (msg.includes('popup-closed-by-user')) {
    return 'تم إلغاء نافذة تسجيل الدخول بحساب Google.';
  }
  if (msg.includes('unauthorized-domain')) {
    return 'نطاق الموقع غير مضاف بعد في قائمة النطاقات المصرح بها في Firebase Authentication.';
  }
  if (msg.trim().length > 0 && /[\u0600-\u06FF]/.test(msg)) {
    return msg;
  }
  return fallback;
}
