import React, { useRef, useState } from 'react';
import { ImagePlus, Loader2, Trash2 } from 'lucide-react';
import { isCloudinaryConfigured, uploadImageToCloudinary, type CloudinaryFolder } from '../../lib/cloudinary';
import { toArabicErrorMessage } from '../../lib/formatters';

interface ImageUploaderProps {
  folder: CloudinaryFolder;
  urls: string[];
  multiple?: boolean;
  maxImages?: number;
  label?: string;
  onChange: (urls: string[]) => void;
}

export const ImageUploader: React.FC<ImageUploaderProps> = ({
  folder,
  urls,
  multiple = false,
  maxImages = 8,
  label = 'رفع صورة',
  onChange,
}) => {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [uploading, setUploading] = useState(false);
  const [manualUrl, setManualUrl] = useState('');
  const [error, setError] = useState('');
  const configured = isCloudinaryConfigured();

  const handleFiles = async (files: FileList | null) => {
    if (!files?.length) return;
    setError('');
    setUploading(true);
    try {
      const next = [...urls];
      for (const file of Array.from(files)) {
        if (next.length >= (multiple ? maxImages : 1)) break;
        const uploadedUrl = await uploadImageToCloudinary(file, folder);
        if (multiple) {
          next.push(uploadedUrl);
        } else {
          next[0] = uploadedUrl;
        }
      }
      onChange(next);
    } catch (err) {
      setError(toArabicErrorMessage(err, 'تعذر رفع الصورة.'));
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  const addManualUrl = () => {
    const clean = manualUrl.trim();
    if (!clean.startsWith('http')) {
      setError('أدخل رابط صورة يبدأ بـ http أو https.');
      return;
    }
    setError('');
    if (multiple) {
      onChange([...urls, clean].slice(0, maxImages));
    } else {
      onChange([clean]);
    }
    setManualUrl('');
  };

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          multiple={multiple}
          onChange={(e) => handleFiles(e.target.files)}
          className="hidden"
        />
        <button
          type="button"
          disabled={uploading}
          onClick={() => inputRef.current?.click()}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-bold hover:bg-[#282B33] disabled:opacity-60"
        >
          {uploading ? <Loader2 className="w-4 h-4 animate-spin" /> : <ImagePlus className="w-4 h-4" />}
          <span>{uploading ? 'جاري الرفع...' : label}</span>
        </button>

        <div className="flex-1 min-w-[200px] flex gap-1.5">
          <input
            type="url"
            value={manualUrl}
            onChange={(e) => setManualUrl(e.target.value)}
            placeholder="أو الصق رابط صورة مباشر https://..."
            dir="ltr"
            className="flex-1 px-3 py-1.5 rounded-xl border border-[#EBECEF] bg-white text-xs focus:outline-none focus:border-[#FFD000]"
          />
          <button
            type="button"
            onClick={addManualUrl}
            className="px-3 py-1.5 rounded-xl bg-[#F9FAFB] border border-[#EBECEF] text-xs font-bold hover:bg-[#FFF9DB]"
          >
            إضافة رابط
          </button>
        </div>
      </div>

      {!configured && (
        <p className="text-[11px] text-[#717784]">
          ملاحظة: الرفع المباشر يتطلب ضبط VITE_CLOUDINARY_CLOUD_NAME و VITE_CLOUDINARY_UPLOAD_PRESET في ملف .env أو لصق رابط صورة مباشر.
        </p>
      )}
      {error && <p className="text-xs text-[#E53935] font-medium">{error}</p>}

      {urls.length > 0 && (
        <div className="flex flex-wrap gap-2 pt-1">
          {urls.map((u, idx) => (
            <div key={`${u}-${idx}`} className="relative w-20 h-20 rounded-xl border border-[#EBECEF] overflow-hidden bg-white">
              <img src={u} alt="" className="w-full h-full object-cover" />
              <button
                type="button"
                aria-label="حذف الصورة"
                onClick={() => onChange(urls.filter((_, i) => i !== idx))}
                className="absolute top-1 left-1 w-6 h-6 rounded-full bg-[#181A20]/80 text-white flex items-center justify-center hover:bg-[#E53935]"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
