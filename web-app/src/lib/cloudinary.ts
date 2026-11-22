export type CloudinaryFolder =
  | 'aleman/products'
  | 'aleman/categories'
  | 'aleman/receipts'
  | 'aleman/banners'
  | 'aleman/videos';

export interface VideoUploadResult {
  secureUrl: string;
  durationSeconds?: number;
  posterUrl?: string;
}

const DEFAULT_CLOUD_NAME = 'dh9lxflpd';
const DEFAULT_UPLOAD_PRESET = 'Eleman_products';
const MAX_VIDEO_BYTES = 40 * 1024 * 1024; // 40 MB
const MAX_ANIMATED_IMAGE_BYTES = 15 * 1024 * 1024; // 15 MB

const CLOUD_NAME = (import.meta.env.VITE_CLOUDINARY_CLOUD_NAME || DEFAULT_CLOUD_NAME).trim();
const UPLOAD_PRESET = (import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET || DEFAULT_UPLOAD_PRESET).trim();

export function isCloudinaryConfigured(): boolean {
  return (
    CLOUD_NAME.length > 0 &&
    UPLOAD_PRESET.length > 0 &&
    CLOUD_NAME !== 'your_cloudinary_cloud_name' &&
    UPLOAD_PRESET !== 'your_unsigned_upload_preset'
  );
}

export function deriveVideoPosterUrl(videoUrl: string): string {
  const clean = (videoUrl || '').trim();
  if (!clean) return '';
  if (!clean.includes('res.cloudinary.com') || !clean.includes('/video/upload/')) {
    return '';
  }
  const withoutQuery = clean.split('?')[0];
  const replaced = withoutQuery.replace(/\.(mp4|mov|webm|mkv|m4v)$/i, '.jpg');
  return replaced !== withoutQuery ? replaced : '';
}

export function extractBrowserVideoDuration(file: File): Promise<number | undefined> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file);
    const video = document.createElement('video');
    video.preload = 'metadata';
    const cleanup = () => {
      URL.revokeObjectURL(url);
      video.removeAttribute('src');
      video.load();
    };
    video.onloadedmetadata = () => {
      const dur = Number.isFinite(video.duration) && video.duration > 0 ? Math.min(120, Math.max(1, Math.ceil(video.duration))) : undefined;
      cleanup();
      resolve(dur);
    };
    video.onerror = () => {
      cleanup();
      resolve(undefined);
    };
    video.src = url;
  });
}

async function compressImageFile(file: File, maxDim = 1280, quality = 0.82): Promise<Blob> {
  return new Promise((resolve) => {
    const img = new Image();
    const url = URL.createObjectURL(file);
    img.onload = () => {
      URL.revokeObjectURL(url);
      let { width, height } = img;
      if (width > maxDim || height > maxDim) {
        if (width >= height) {
          height = Math.round((height * maxDim) / width);
          width = maxDim;
        } else {
          width = Math.round((width * maxDim) / height);
          height = maxDim;
        }
      }
      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        resolve(file);
        return;
      }
      ctx.drawImage(img, 0, 0, width, height);
      canvas.toBlob(
        (blob) => resolve(blob || file),
        'image/webp',
        quality,
      );
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      resolve(file);
    };
    img.src = url;
  });
}

export async function uploadImageToCloudinary(
  file: File,
  folder: CloudinaryFolder,
  customCloudName?: string,
  customPreset?: string,
): Promise<string> {
  const cloudName = (customCloudName || CLOUD_NAME).trim();
  const preset = (customPreset || UPLOAD_PRESET).trim();

  if (!cloudName || !preset || cloudName === 'your_cloudinary_cloud_name') {
    throw new Error(
      'لم يتم إعداد Cloudinary في ملف .env (VITE_CLOUDINARY_CLOUD_NAME / VITE_CLOUDINARY_UPLOAD_PRESET). يمكنك إدخال رابط الصورة مباشرًا أو ضبط الإعدادات.',
    );
  }

  const compressed = await compressImageFile(file);
  const formData = new FormData();
  formData.append('file', compressed, `${Date.now()}.webp`);
  formData.append('upload_preset', preset);
  formData.append('folder', folder);

  const response = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/image/upload`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => '');
    throw new Error(`فشل رفع الصورة إلى Cloudinary: ${errText || response.status}`);
  }

  const json = (await response.json()) as { secure_url?: string };
  if (!json.secure_url) {
    throw new Error('لم يتم إرجاع رابط صالح للصورة من Cloudinary.');
  }
  return json.secure_url;
}

export async function uploadAnimatedImageToCloudinary(
  file: File,
  folder: CloudinaryFolder = 'aleman/banners',
  customCloudName?: string,
  customPreset?: string,
): Promise<string> {
  const cloudName = (customCloudName || CLOUD_NAME).trim();
  const preset = (customPreset || UPLOAD_PRESET).trim();

  if (!cloudName || !preset || cloudName === 'your_cloudinary_cloud_name') {
    throw new Error(
      'لم يتم إعداد Cloudinary لرفع الصور المتحركة. يمكنك إدخال رابط GIF أو WebP مباشرًا.',
    );
  }

  if (file.size > MAX_ANIMATED_IMAGE_BYTES) {
    throw new Error('حجم الصورة المتحركة كبير جدًا (الحد الأقصى ١٥ ميجابايت). اختر ملف GIF أو WebP أصغر.');
  }

  const ext = file.name?.toLowerCase().endsWith('.webp') || file.type === 'image/webp' ? 'webp' : 'gif';
  const formData = new FormData();
  formData.append('file', file, file.name || `anim_${Date.now()}.${ext}`);
  formData.append('upload_preset', preset);
  formData.append('folder', folder);

  const response = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/image/upload`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => '');
    throw new Error(`فشل رفع الصورة المتحركة: ${errText || response.status}`);
  }

  const json = (await response.json()) as { secure_url?: string };
  if (!json.secure_url) {
    throw new Error('لم يتم إرجاع رابط صالح للصورة المتحركة.');
  }
  return json.secure_url;
}

export async function uploadVideoToCloudinary(
  file: File,
  folder: CloudinaryFolder = 'aleman/videos',
  customCloudName?: string,
  customPreset?: string,
): Promise<VideoUploadResult> {
  const cloudName = (customCloudName || CLOUD_NAME).trim();
  const preset = (customPreset || UPLOAD_PRESET).trim();

  if (!cloudName || !preset || cloudName === 'your_cloudinary_cloud_name') {
    throw new Error(
      'لم يتم إعداد Cloudinary لرفع الفيديو. يمكنك إدخال رابط فيديو مباشر (MP4) أو ضبط الإعدادات.',
    );
  }

  if (file.size > MAX_VIDEO_BYTES) {
    throw new Error('حجم الفيديو كبير جدًا (الحد الأقصى ٤٠ ميجابايت). اختر فيديو قصيرًا ومضغوطًا.');
  }

  const localDuration = await extractBrowserVideoDuration(file);
  const formData = new FormData();
  formData.append('file', file, file.name || `video_${Date.now()}.mp4`);
  formData.append('upload_preset', preset);
  formData.append('folder', folder);

  const response = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/video/upload`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => '');
    throw new Error(`فشل رفع الفيديو إلى Cloudinary: ${errText || response.status}`);
  }

  const json = (await response.json()) as { secure_url?: string; duration?: number };
  if (!json.secure_url) {
    throw new Error('لم يتم إرجاع رابط صالح للفيديو من Cloudinary.');
  }

  const cloudDuration =
    typeof json.duration === 'number' && Number.isFinite(json.duration) && json.duration > 0
      ? Math.min(120, Math.max(1, Math.ceil(json.duration)))
      : undefined;

  return {
    secureUrl: json.secure_url,
    durationSeconds: cloudDuration ?? localDuration,
    posterUrl: deriveVideoPosterUrl(json.secure_url) || undefined,
  };
}
