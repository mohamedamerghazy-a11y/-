import React, { useRef, useState } from 'react';
import { Clock, Loader2, Trash2, Video } from 'lucide-react';
import {
  deriveVideoPosterUrl,
  isCloudinaryConfigured,
  uploadVideoToCloudinary,
  type CloudinaryFolder,
} from '../../lib/cloudinary';
import { formatNumber, toArabicErrorMessage } from '../../lib/formatters';

interface VideoUploaderProps {
  videoUrl: string;
  durationSeconds?: number;
  folder?: CloudinaryFolder;
  label?: string;
  onChange: (payload: { videoUrl: string; durationSeconds?: number; posterUrl?: string }) => void;
}

export const VideoUploader: React.FC<VideoUploaderProps> = ({
  videoUrl,
  durationSeconds = 0,
  folder = 'aleman/videos',
  label = 'رفع فيديو قصير (MP4)',
  onChange,
}) => {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [uploading, setUploading] = useState(false);
  const [manualUrl, setManualUrl] = useState('');
  const [error, setError] = useState('');
  const configured = isCloudinaryConfigured();

  const handleFile = async (files: FileList | null) => {
    const file = files?.[0];
    if (!file) return;
    setError('');
    setUploading(true);
    try {
      const result = await uploadVideoToCloudinary(file, folder);
      onChange({
        videoUrl: result.secureUrl,
        durationSeconds: result.durationSeconds,
        posterUrl: result.posterUrl,
      });
    } catch (err) {
      setError(toArabicErrorMessage(err, 'تعذر رفع الفيديو، حاول مرة أخرى.'));
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  const applyManualUrl = () => {
    const clean = manualUrl.trim();
    if (!clean.startsWith('http')) {
      setError('أدخل رابط فيديو مباشر يبدأ بـ http أو https.');
      return;
    }
    setError('');
    onChange({
      videoUrl: clean,
      posterUrl: deriveVideoPosterUrl(clean) || undefined,
    });
    setManualUrl('');
  };

  return (
    <div className="space-y-2.5">
      <div className="flex flex-wrap items-center gap-2">
        <input
          ref={inputRef}
          type="file"
          accept="video/mp4,video/webm,video/quicktime,video/*"
          onChange={(e) => handleFile(e.target.files)}
          className="hidden"
        />
        <button
          type="button"
          disabled={uploading}
          onClick={() => inputRef.current?.click()}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-bold hover:bg-[#282B33] disabled:opacity-60"
        >
          {uploading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Video className="w-4 h-4" />}
          <span>{uploading ? 'جاري رفع الفيديو...' : videoUrl ? 'تغيير الفيديو' : label}</span>
        </button>

        <div className="flex-1 min-w-[200px] flex gap-1.5">
          <input
            type="url"
            value={manualUrl}
            onChange={(e) => setManualUrl(e.target.value)}
            placeholder="أو الصق رابط فيديو مباشر https://...mp4"
            dir="ltr"
            className="flex-1 px-3 py-1.5 rounded-xl border border-[#EBECEF] bg-white text-xs focus:outline-none focus:border-[#FFD000]"
          />
          <button
            type="button"
            onClick={applyManualUrl}
            className="px-3 py-1.5 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold hover:bg-[#FFF9DB]"
          >
            إضافة رابط
          </button>
        </div>
      </div>

      {!configured && (
        <p className="text-[11px] text-[#717784]">
          ملاحظة: الرفع المباشر يتطلب ضبط إعدادات Cloudinary أو لصق رابط فيديو MP4 مباشر.
        </p>
      )}
      {error && <p className="text-xs text-[#E53935] font-medium">{error}</p>}

      {videoUrl && (
        <div className="rounded-2xl border border-[#EBECEF] bg-[#101218] overflow-hidden">
          <div className="relative">
            <video
              key={videoUrl}
              src={videoUrl}
              poster={deriveVideoPosterUrl(videoUrl) || undefined}
              controls
              muted
              playsInline
              onLoadedMetadata={(e) => {
                const dur = e.currentTarget.duration;
                if (Number.isFinite(dur) && dur > 0 && (!durationSeconds || durationSeconds <= 0)) {
                  onChange({
                    videoUrl,
                    durationSeconds: Math.min(120, Math.max(1, Math.ceil(dur))),
                    posterUrl: deriveVideoPosterUrl(videoUrl) || undefined,
                  });
                }
              }}
              className="w-full max-h-56 object-contain bg-black"
            />
            <button
              type="button"
              aria-label="حذف الفيديو"
              onClick={() => onChange({ videoUrl: '', durationSeconds: 0, posterUrl: '' })}
              className="absolute top-2 left-2 w-8 h-8 rounded-full bg-[#181A20]/85 text-white flex items-center justify-center hover:bg-[#E53935] transition"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
          <div className="px-3 py-2 flex items-center justify-between gap-2 text-[11px] text-white/90">
            <span className="inline-flex items-center gap-1.5 font-bold text-[#FFD000]">
              <Clock className="w-3.5 h-3.5" />
              {durationSeconds > 0
                ? `المدة المكتشفة تلقائيًا: ${formatNumber(durationSeconds)} ثانية`
                : 'سيتم التعرف على مدة الفيديو تلقائيًا'}
            </span>
            <span dir="ltr" className="truncate max-w-[190px] text-white/60">
              {videoUrl}
            </span>
          </div>
        </div>
      )}

      <p className="text-[11px] text-[#717784] bg-[#FFF9DB]/70 border border-[#FFD000]/40 rounded-xl px-3 py-1.5">
        يفضل رفع فيديو قصير بصيغة MP4 (من ٥ إلى ٣٠ ثانية) لضمان سرعة التشغيل الفوري على الأندرويد والويب.
      </p>
    </div>
  );
};
