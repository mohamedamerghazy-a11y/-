package com.aleman.cash.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.aleman.cash.data.CashProviderSettingsStore
import com.aleman.cash.data.CashVerificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsCashBroadcastReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
    if (messages.isNullOrEmpty()) return

    val groupedBySender = linkedMapOf<String, StringBuilder>()
    for (sms in messages) {
      val sender = sms.displayOriginatingAddress?.trim().orEmpty()
      val bodyPart = sms.displayMessageBody.orEmpty()
      groupedBySender.getOrPut(sender) { StringBuilder() }.append(bodyPart)
    }
    if (groupedBySender.isEmpty()) return

    val pendingResult = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val store = CashProviderSettingsStore(appContext)
        val settings = store.getCurrentSettings()
        if (!settings.autoVerifyEnabled) return@launch

        val repository = CashVerificationRepository(appContext)
        for ((sender, bodyBuilder) in groupedBySender) {
          val fullBody = bodyBuilder.toString().trim()
          if (fullBody.isNotEmpty()) {
            repository.processIncomingSms(
              senderAddress = sender,
              rawBody = fullBody,
              settings = settings,
              notifyUser = true
            )
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
