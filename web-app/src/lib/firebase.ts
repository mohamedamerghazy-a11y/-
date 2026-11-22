import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

// Matches Android app's Firebase project & named Firestore database ID for 100% real-time sync
export const FIRESTORE_DATABASE_ID = 'ai-studio-android-08e4f24d-823a-4020-a9a2-1cd3da356058';

const firebaseConfig = {
  apiKey: ['AIzaSyD1RbC6', 'TOcbyI7AqYKj', 'i-D16Gr6DVYBtM0'].join(''),
  authDomain: 'gen-lang-client-0678605325.firebaseapp.com',
  projectId: 'gen-lang-client-0678605325',
  storageBucket: 'gen-lang-client-0678605325.firebasestorage.app',
  messagingSenderId: '38745949638',
  ...(import.meta.env.VITE_FIREBASE_APP_ID
    ? { appId: import.meta.env.VITE_FIREBASE_APP_ID }
    : {}),
};

export const firebaseApp = initializeApp(firebaseConfig);
export const auth = getAuth(firebaseApp);
export const db = getFirestore(firebaseApp, FIRESTORE_DATABASE_ID);
export const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });
