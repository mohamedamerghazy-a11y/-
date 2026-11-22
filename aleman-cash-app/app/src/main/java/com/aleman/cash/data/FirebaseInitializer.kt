package com.aleman.cash.data

import android.content.Context
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseInitializer {

  const val FIRESTORE_DATABASE_ID = "ai-studio-android-08e4f24d-823a-4020-a9a2-1cd3da356058"
  private const val PROJECT_ID = "gen-lang-client-0678605325"
  private const val APP_ID = "1:38745949638:android:75437e1ccc69302226dec7"
  private const val PROJECT_NUMBER = "38745949638"
  private const val STORAGE_BUCKET = "gen-lang-client-0678605325.firebasestorage.app"
  private const val B64_K = "QUl6YVN5RDFSYkM2VE9jYnlJN0FxWUtqaS1EMTZHcjZEVllCdE0w"

  @Volatile
  private var cachedApp: FirebaseApp? = null

  fun ensureInitialized(context: Context): FirebaseApp {
    cachedApp?.let { return it }
    synchronized(this) {
      cachedApp?.let { return it }
      val appContext = context.applicationContext
      val existing = FirebaseApp.getApps(appContext)
        .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
      if (existing != null) {
        cachedApp = existing
        return existing
      }
      val fromResources = FirebaseApp.initializeApp(appContext)
      if (fromResources != null) {
        cachedApp = fromResources
        return fromResources
      }
      val key = String(Base64.decode(B64_K, Base64.DEFAULT), Charsets.UTF_8)
      val options = FirebaseOptions.Builder()
        .setProjectId(PROJECT_ID)
        .setApplicationId(APP_ID)
        .setApiKey(key)
        .setGcmSenderId(PROJECT_NUMBER)
        .setStorageBucket(STORAGE_BUCKET)
        .build()
      val created = FirebaseApp.initializeApp(appContext, options)
      cachedApp = created
      return created
    }
  }

  fun auth(context: Context): FirebaseAuth = FirebaseAuth.getInstance(ensureInitialized(context))

  fun firestore(context: Context): FirebaseFirestore =
    FirebaseFirestore.getInstance(ensureInitialized(context), FIRESTORE_DATABASE_ID)
}
