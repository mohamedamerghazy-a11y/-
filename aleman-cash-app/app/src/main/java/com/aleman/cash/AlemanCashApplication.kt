package com.aleman.cash

import android.app.Application
import com.aleman.cash.data.CashNotificationHelper
import com.aleman.cash.data.FirebaseInitializer

class AlemanCashApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    FirebaseInitializer.ensureInitialized(this)
    CashNotificationHelper.ensureChannel(this)
  }
}
