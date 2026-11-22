import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface HorizontalScrollerProps {
  children: React.ReactNode;
  className?: string;
  scrollStep?: number;
  ariaLabel?: string;
}

/**
 * World-class RTL-aware horizontal strip with smooth desktop arrow buttons.
 * Hides arrows on small touch screens where swipe is native, and automatically
 * reveals them on desktop when the content overflows.
 */
export const HorizontalScroller: React.FC<HorizontalScrollerProps> = ({
  children,
  className = '',
  scrollStep = 460,
  ariaLabel = 'تصفح العناصر',
}) => {
  const scrollerRef = useRef<HTMLDivElement | null>(null);
  const [hasOverflow, setHasOverflow] = useState(false);

  const checkOverflow = useCallback(() => {
    const el = scrollerRef.current;
    if (!el) return;
    setHasOverflow(el.scrollWidth > el.clientWidth + 8);
  }, []);

  useEffect(() => {
    checkOverflow();
    const el = scrollerRef.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(checkOverflow);
    ro.observe(el);
    window.addEventListener('resize', checkOverflow);
    return () => {
      ro.disconnect();
      window.removeEventListener('resize', checkOverflow);
    };
  }, [checkOverflow, children]);

  const scrollByDir = (direction: 'start' | 'end') => {
    const el = scrollerRef.current;
    if (!el) return;
    // In RTL, positive scrollBy moves visually right (toward start), negative moves left (toward end)
    const delta = direction === 'start' ? scrollStep : -scrollStep;
    el.scrollBy({ left: delta, behavior: 'smooth' });
  };

  return (
    <div className="relative group/scroller">
      <div
        ref={scrollerRef}
        aria-label={ariaLabel}
        className={`flex overflow-x-auto no-scrollbar scroll-smooth ${className}`}
      >
        {children}
      </div>

      {hasOverflow && (
        <>
          <button
            type="button"
            aria-label="التمرير لليمين"
            onClick={(e) => {
              e.stopPropagation();
              scrollByDir('start');
            }}
            className="hidden md:flex items-center justify-center absolute -right-2.5 top-1/2 -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white/95 hover:bg-[#FFD000] text-[#181A20] border border-[#EBECEF] shadow-md opacity-0 group-hover/scroller:opacity-100 focus:opacity-100 transition-all"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
          <button
            type="button"
            aria-label="التمرير لليسار"
            onClick={(e) => {
              e.stopPropagation();
              scrollByDir('end');
            }}
            className="hidden md:flex items-center justify-center absolute -left-2.5 top-1/2 -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white/95 hover:bg-[#FFD000] text-[#181A20] border border-[#EBECEF] shadow-md opacity-0 group-hover/scroller:opacity-100 focus:opacity-100 transition-all"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
        </>
      )}
    </div>
  );
};
