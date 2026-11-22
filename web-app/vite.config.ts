import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig(({ mode }) => {
  const localEnv = loadEnv(mode, process.cwd(), '');
  const rootEnv = loadEnv(mode, '..', '');
  const cloudName =
    localEnv.VITE_CLOUDINARY_CLOUD_NAME ||
    process.env.VITE_CLOUDINARY_CLOUD_NAME ||
    process.env.CLOUDINARY_CLOUD_NAME ||
    rootEnv.CLOUDINARY_CLOUD_NAME ||
    '';
  const uploadPreset =
    localEnv.VITE_CLOUDINARY_UPLOAD_PRESET ||
    process.env.VITE_CLOUDINARY_UPLOAD_PRESET ||
    process.env.CLOUDINARY_UPLOAD_PRESET ||
    rootEnv.CLOUDINARY_UPLOAD_PRESET ||
    '';

  return {
    plugins: [react(), tailwindcss()],
    define: {
      ...(cloudName ? { 'import.meta.env.VITE_CLOUDINARY_CLOUD_NAME': JSON.stringify(cloudName) } : {}),
      ...(uploadPreset ? { 'import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET': JSON.stringify(uploadPreset) } : {}),
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
    },
    server: {
      port: 5173,
      host: true,
    },
  };
});
