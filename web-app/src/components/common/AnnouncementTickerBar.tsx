import React, { useMemo } from 'react';
import {
  sanitizeCardHexColor,
  type StoreGeneralSettings,
  type TickerFontWeight,
  type TickerInlineImage,
} from '../../types/models';

export type TickerSegment =
  | { type: 'text'; text: string }
  | { type: 'image'; id: string; url: string };

const TICKER_IMAGE_REGEX = /\[\[img:([a-zA-Z0-9_-]+)\]\]/g;

export function parseTickerSegments(
  text: string,
  images: TickerInlineImage[] = [],
): TickerSegment[] {
  const imgMap = new Map<string, string>();
  for (const img of images) {
    const id = (img.id || '').trim();
    const url = (img.url || '').trim();
    if (id && url) imgMap.set(id, url);
  }

  if (!text.trim()) {
    return Array.from(imgMap.entries()).map(([id, url]) => ({
      type: 'image',
      id,
      url,
    }));
  }

  const result: TickerSegment[] = [];
  let lastIndex = 0;
  TICKER_IMAGE_REGEX.lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = TICKER_IMAGE_REGEX.exec(text)) !== null) {
    if (match.index > lastIndex) {
      const chunk = text.slice(lastIndex, match.index);
      if (chunk) result.push({ type: 'text', text: chunk });
    }
    const id = (match[1] || '').trim();
    const url = imgMap.get(id);
    if (url) {
      result.push({ type: 'image', id, url });
    }
    lastIndex = match.index + match[0].length;
  }
  if (lastIndex < text.length) {
    const tail = text.slice(lastIndex);
    if (tail) result.push({ type: 'text', text: tail });
  }
  return result;
}

function resolveCssFontWeight(weight: TickerFontWeight): number {
  switch (weight) {
    case 'light':
      return 300;
    case 'normal':
      return 400;
    case 'extra_bold':
      return 800;
    case 'bold':
    default:
      return 700;
  }
}

interface AnnouncementTickerBarProps {
  settings: Pick<
    StoreGeneralSettings,
    | 'tickerEnabled'
    | 'tickerText'
    | 'tickerBackgroundColor'
    | 'tickerBorderColor'
    | 'tickerTextColor'
    | 'tickerFontSizeSp'
    | 'tickerFontWeight'
    | 'tickerItalic'
    | 'tickerInlineImages'
  >;
  forcePreview?: boolean;
  className?: string;
}

export const AnnouncementTickerBar: React.FC<AnnouncementTickerBarProps> = ({
  settings,
  forcePreview = false,
  className = '',
}) => {
  const effectiveText =
    forcePreview && !settings.tickerText.trim() && !(settings.tickerInlineImages || []).length
      ? '🔥 عروض وتخفيضات حصرية في الإيمان للتجارة — تسوق الآن واستمتع بأفضل الأسعار ⚡'
      : settings.tickerText;

  const segments = useMemo(
    () => parseTickerSegments(effectiveText, settings.tickerInlineImages || []),
    [effectiveText, settings.tickerInlineImages],
  );

  if (!forcePreview && !settings.tickerEnabled) return null;
  if (!segments.length) return null;

  const bgColor = sanitizeCardHexColor(settings.tickerBackgroundColor, '#181A20');
  const borderColor = sanitizeCardHexColor(settings.tickerBorderColor, '#FFD000');
  const textColor = sanitizeCardHexColor(settings.tickerTextColor, '#FFFFFF');
  const fontSize = Math.min(24, Math.max(11, Math.round(Number(settings.tickerFontSizeSp || 14))));
  const fontWeight = resolveCssFontWeight(settings.tickerFontWeight || 'bold');
  const thumbPx = Math.min(38, Math.max(20, Math.round(fontSize * 1.65)));
  const minHeightPx = Math.min(62, Math.max(40, Math.round(fontSize * 2.55)));
  const durationSeconds = Math.min(34, Math.max(10, Math.round(effectiveText.length * 0.22 + 12)));

  const renderSingleCopy = (copyKey: string) => (
    <div
      key={copyKey}
      dir="rtl"
      className="inline-flex items-center shrink-0 gap-2 px-3 whitespace-nowrap"
      style={{
        color: textColor,
        fontSize: `${fontSize}px`,
        fontWeight,
        fontStyle: settings.tickerItalic ? 'italic' : 'normal',
        lineHeight: 1.35,
      }}
    >
      {segments.map((seg, idx) =>
        seg.type === 'text' ? (
          <span key={`${copyKey}-t-${idx}`} className="whitespace-pre">
            {seg.text}
          </span>
        ) : (
          <img
            key={`${copyKey}-i-${idx}`}
            src={seg.url}
            alt="صورة مصغرة"
            className="inline-block object-cover rounded-md shrink-0"
            style={{
              width: `${thumbPx}px`,
              height: `${thumbPx}px`,
              border: `1.5px solid ${borderColor}`,
            }}
          />
        ),
      )}
      <span
        aria-hidden="true"
        className="inline-block w-1.5 h-1.5 rounded-full mx-6 shrink-0"
        style={{ backgroundColor: borderColor }}
      />
    </div>
  );

  return (
    <div
      className={`relative w-full rounded-2xl overflow-hidden shadow-sm select-none ${className}`}
      style={{
        backgroundColor: bgColor,
        border: `2px solid ${borderColor}`,
        minHeight: `${minHeightPx}px`,
      }}
    >
      {/* Subtle top & bottom inner accent lines */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-[2px] opacity-30"
        style={{ backgroundColor: borderColor }}
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 bottom-0 h-[2px] opacity-30"
        style={{ backgroundColor: borderColor }}
      />

      {/* Seamless Left-to-Right marquee track */}
      <div
        dir="ltr"
        className="relative w-full overflow-hidden flex items-center"
        style={{ minHeight: `${minHeightPx}px` }}
      >
        <div
          className="animate-ticker-ltr items-center"
          style={{ ['--ticker-duration' as string]: `${durationSeconds}s` }}
        >
          {/* Two identical half-tracks so translate3d(-50% -> 0%) glides smoothly Left to Right */}
          <div className="inline-flex items-center shrink-0">
            {renderSingleCopy('a1')}
            {renderSingleCopy('a2')}
            {renderSingleCopy('a3')}
          </div>
          <div className="inline-flex items-center shrink-0" aria-hidden="true">
            {renderSingleCopy('b1')}
            {renderSingleCopy('b2')}
            {renderSingleCopy('b3')}
          </div>
        </div>
      </div>

      {/* Soft left & right edge fades matching background */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-y-0 left-0 w-7"
        style={{ background: `linear-gradient(to right, ${bgColor}, transparent)` }}
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-y-0 right-0 w-7"
        style={{ background: `linear-gradient(to left, ${bgColor}, transparent)` }}
      />
    </div>
  );
};
