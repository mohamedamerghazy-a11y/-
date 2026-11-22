import React, { useRef, useState } from 'react';
import { Loader2, Sparkles, Trash2, Upload } from 'lucide-react';
import { type CloudinaryFolder, uploadAnimatedImageToCloudinary } from '../../lib/cloudinary';
import { useStore } from '../../context/StoreContext';

interface AnimatedImageUploaderProps {
  folder?: CloudinaryFolder;
  imageUrl: string;
  isSlimPreview?: boolean;
  label?: string;
  onChange: (url: string) => void;
}

export const AnimatedImageUploader: React.FC<AnimatedImageUploaderProps> = ({
  folder = 'aleman/banners',
  imageUrl,
  isSlimPreview = false,
  label = 'رفع صورة متحركة (GIF / WebP)',
  onChange,
}) => {
  const { showToast } = useStore();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [uploading, setUploading] = useState(false);

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const secureUrl = await uploadAnimatedImageToCloudinary(file, folder);
      onChange(secureUrl);
      showToast('تم رفع الصورة المتحركة مع الحفاظ على الحركة بنجاح', 'success');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'تعذر رفع الصورة المتحركة.';
      showToast(msg, 'error');
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  return (
    <div className="space-y-2.5 text-xs">
      <div className="rounded-xl bg-[#FFF9DB]/80 border border-[#FFD000]/70 p-2.5 text-[#181A20] font-bold">
        يدعم الصور المتحركة بصيغة GIF أو Animated WebP بدون ضغط يفقد الحركة، وتعمل بتكرار تلقائي مستمر.
      </div>

      <div className="flex items-center justify-between gap-2">
        <span className="font-bold text-[#181A20]">{label}</span>
        <button
          type="button"
          disabled={uploading}
          onClick={() => inputRef.current?.click()}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#181A20] text-[#FFD000] font-black disabled:opacity-60"
        >
          {uploading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Upload className="w-3.5 h-3.5" />}
          <span>{uploading ? 'جاري الرفع...' : imageUrl ? 'تغيير الصورة المتحركة' : 'اختيار ملف GIF / WebP'}</span>
        </button>
        <input
          ref={inputRef}
          type="file"
          accept="image/gif,image/webp,.gif,.webp"
          onChange={handleFile}
          className="hidden"
        />
      </div>

      {imageUrl && (
        <div className="relative rounded-2xl overflow-hidden border border-[#EBECEF] bg-[#F9FAFB]">
          <div className={isSlimPreview ? 'aspect-[21/9] max-h-[160px]' : 'aspect-[16/9] max-h-[240px]'}>
            <img
              src={imageUrl}
              alt="معاينة الصورة المتحركة"
              className="w-full h-full object-cover block"
            />
          </div>
          <span className="absolute top-2 right-2 inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#181A20]/80 text-[#FFD000] text-[11px] font-black">
            <Sparkles className="w-3.5 h-3.5" />
            <span>صورة متحركة</span>
          </span>
          <button
            type="button"
            onClick={() => onChange('')}
            aria-label="حذف الصورة المتحركة"
            className="absolute top-2 left-2 p-1.5 rounded-lg bg-[#181A20]/80 text-[#E53935] hover:bg-[#181A20]"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      )}

      <input
        type="url"
        dir="ltr"
        value={imageUrl}
        onChange={(e) => onChange(e.target.value)}
        placeholder="رابط مباشر للصورة المتحركة (https://...gif أو ...webp)"
        className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs"
      />
    </div>
  );
};
