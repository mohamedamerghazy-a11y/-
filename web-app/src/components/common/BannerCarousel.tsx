import React, { useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, Volume2, VolumeX } from 'lucide-react';
import type { Banner } from '../../types/models';
import { useStore } from '../../context/StoreContext';
import { deriveVideoPosterUrl } from '../../lib/cloudinary';

interface BannerCarouselProps {
  banners: Banner[];
}

export const BannerCarousel: React.FC<BannerCarouselProps> = ({ banners }) => {
  const { navigate } = useStore();
  const [activeIndex, setActiveIndex] = useState(0);
  const [isMuted, setIsMuted] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);

  useEffect(() => {
    if (activeIndex >= banners.length) setActiveIndex(0);
  }, [banners.length, activeIndex]);

  const current = banners[activeIndex];
  const isVideo = Boolean(current && current.mediaType === 'video' && (current.videoUrl || '').trim().length >= 5);
  const isForceMuted = Boolean(current?.videoMuted);
  const effectiveMuted = isForceMuted || isMuted;

  useEffect(() => {
    setIsMuted(isForceMuted);
  }, [current?.id, isForceMuted]);

  useEffect(() => {
    if (banners.length <= 1 || !current) return;
    // For video banners, allow enough time for playback (the video's onEnded also advances immediately)
    const maxSec = isVideo ? 120 : 60;
    const ms = Math.min(maxSec, Math.max(1, current.durationSeconds || (isVideo ? 15 : 5))) * 1000;
    const timer = window.setTimeout(() => {
      setActiveIndex((prev) => (prev + 1) % banners.length);
    }, ms);
    return () => window.clearTimeout(timer);
  }, [activeIndex, banners.length, current, isVideo]);

  useEffect(() => {
    const el = videoRef.current;
    if (!isVideo || !el) return;
    el.muted = effectiveMuted;
    const playPromise = el.play();
    if (playPromise && typeof playPromise.catch === 'function') {
      playPromise.catch(() => {
        if (!el.muted) {
          el.muted = true;
          setIsMuted(true);
          el.play().catch(() => {});
        }
      });
    }
  }, [isVideo, current?.id, current?.videoUrl, effectiveMuted]);

  if (!banners.length || !current) return null;

  const handleClick = (b: Banner) => {
    if (b.targetType === 'product' && b.targetId) {
      navigate({ name: 'product', productId: b.targetId });
    } else if (b.targetType === 'category' && b.targetId) {
      navigate({ name: 'categories', initialCategoryId: b.targetId });
    } else if (b.targetType === 'offers') {
      navigate({ name: 'search', onlyOffers: true });
    }
  };

  const isAnimatedImage = Boolean(
    current && current.mediaType === 'animated_image' && (current.imageUrl || '').trim().length >= 5,
  );
  const posterUrl = current.imageUrl?.trim() || deriveVideoPosterUrl(current.videoUrl || '');
  const isSlim = current.bannerStyle === 'slim';

  return (
    <div className="relative group w-full rounded-3xl overflow-hidden border border-[#E8EAEE] bg-white shadow-sm">
      <div
        role="button"
        tabIndex={0}
        onClick={() => handleClick(current)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleClick(current);
          }
        }}
        className="relative block w-full overflow-hidden text-right cursor-pointer"
      >
        {isVideo ? (
          <div
            className={`relative w-full bg-[#101218] overflow-hidden ${
              isSlim ? 'aspect-[21/9] max-h-[240px]' : 'aspect-[16/9] max-h-[420px]'
            }`}
          >
            <video
              ref={videoRef}
              key={current.id}
              src={current.videoUrl}
              poster={posterUrl || undefined}
              autoPlay
              muted={effectiveMuted}
              loop
              playsInline
              className="w-full h-full object-cover block"
            />

            {!isForceMuted && (
              <button
                type="button"
                aria-label={isMuted ? 'تشغيل الصوت' : 'كتم الصوت'}
                onClick={(e) => {
                  e.stopPropagation();
                  setIsMuted((prev) => !prev);
                }}
                className="absolute top-3 left-3 w-9 h-9 rounded-full bg-[#181A20]/75 text-white flex items-center justify-center backdrop-blur-sm border border-white/20 hover:bg-[#181A20] transition"
              >
                {isMuted ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4 text-[#FFD000]" />}
              </button>
            )}
          </div>
        ) : isAnimatedImage ? (
          <div
            className={`relative w-full bg-[#F9FAFB] overflow-hidden ${
              isSlim ? 'aspect-[21/9] max-h-[240px]' : 'aspect-[16/9] max-h-[420px]'
            }`}
          >
            <img
              key={current.id}
              src={current.imageUrl}
              alt={current.title || 'بنر صورة متحركة'}
              className="w-full h-full object-cover block"
            />
          </div>
        ) : (
          <img
            src={current.imageUrl}
            alt={current.title || 'عرض الإيمان للتجارة'}
            className="w-full h-auto block transition-transform duration-500 group-hover:scale-[1.01]"
          />
        )}

        {(current.title || current.subtitle) && (
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#181A20]/75 via-[#181A20]/15 to-transparent flex flex-col justify-end p-4 sm:p-6">
            {current.title && (
              <h3 className="text-white font-extrabold text-base sm:text-xl drop-shadow">
                {current.title}
              </h3>
            )}
            {current.subtitle && (
              <p className="text-white/90 text-xs sm:text-sm mt-0.5 line-clamp-2">
                {current.subtitle}
              </p>
            )}
          </div>
        )}
      </div>

      {banners.length > 1 && (
        <>
          <button
            type="button"
            aria-label="البنر السابق"
            onClick={() => setActiveIndex((i) => (i - 1 + banners.length) % banners.length)}
            className="absolute right-2.5 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white/85 text-[#181A20] flex items-center justify-center shadow hover:bg-[#FFD000] transition"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
          <button
            type="button"
            aria-label="البنر التالي"
            onClick={() => setActiveIndex((i) => (i + 1) % banners.length)}
            className="absolute left-2.5 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white/85 text-[#181A20] flex items-center justify-center shadow hover:bg-[#FFD000] transition"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <div className="absolute bottom-2.5 left-1/2 -translate-x-1/2 flex items-center gap-1.5 bg-[#181A20]/50 px-2.5 py-1 rounded-full">
            {banners.map((b, idx) => (
              <button
                key={b.id}
                type="button"
                aria-label={`بنر ${idx + 1}`}
                onClick={() => setActiveIndex(idx)}
                className={`h-2 rounded-full transition-all ${
                  idx === activeIndex ? 'w-5 bg-[#FFD000]' : 'w-2 bg-white/60'
                }`}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
};
