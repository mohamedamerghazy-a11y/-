package com.aleman.cash.sms

import android.content.Context
import android.provider.Telephony

data class DeviceSmsMessage(
  val senderAddress: String,
  val body: String,
  val dateMillis: Long
)

object SmsInboxScanner {

  fun readRecentInboxMessages(context: Context, limit: Int = 30): List<DeviceSmsMessage> {
    val result = mutableListOf<DeviceSmsMessage>()
    val projection = arrayOf(
      Telephony.Sms.ADDRESS,
      Telephony.Sms.BODY,
      Telephony.Sms.DATE
    )
    context.contentResolver.query(
      Telephony.Sms.Inbox.CONTENT_URI,
      projection,
      null,
      null,
      "${Telephony.Sms.DATE} DESC"
    )?.use { cursor ->
      val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
      val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
      val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
      while (cursor.moveToNext() && result.size < limit) {
        val address = if (addrIdx >= 0) cursor.getString(addrIdx).orEmpty() else ""
        val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
        val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
        if (address.isNotBlank() && body.isNotBlank()) {
          result.add(DeviceSmsMessage(address, body, date))
        }
      }
    }
    return result
  }
}
