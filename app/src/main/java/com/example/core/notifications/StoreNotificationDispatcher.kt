package com.example.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppNotification

class StoreNotificationDispatcher(private val context: Context) {

  private val appContext = context.applicationContext
  private val prefs by lazy {
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun ensureChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val existing = manager.getNotificationChannel(CHANNEL_ID)
    if (existing != null) return

    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val audioAttributes = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_NOTIFICATION)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

    val channel = NotificationChannel(
      CHANNEL_ID,
      "تنبيهات المتجر والطلبات",
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = "إشعارات الطلبات وتوصيل المندوبين والعروض الحصرية بصوت وتنبيه فوري."
      enableLights(true)
      enableVibration(true)
      vibrationPattern = longArrayOf(0L, 250L, 140L, 250L)
      setSound(soundUri, audioAttributes)
      setShowBadge(true)
    }
    manager.createNotificationChannel(channel)
  }

  fun dispatchNewUnreadNotifications(uid: String, notifications: List<AppNotification>) {
    if (uid.isBlank() || notifications.isEmpty()) return
    ensureChannel()

    val prefsKey = "seen_ids_$uid"
    val storedIds = prefs.getStringSet(prefsKey, emptySet()).orEmpty().toMutableSet()
    val unseenUnread = notifications.filter { !it.isRead && it.id.isNotBlank() && it.id !in storedIds }
    if (unseenUnread.isEmpty()) return

    // Record all current unread IDs so repeated snapshot updates do not re-trigger sound alerts.
    val updatedSet = (storedIds + unseenUnread.map { it.id }).toList().takeLast(250).toSet()
    prefs.edit().putStringSet(prefsKey, updatedSet).apply()

    if (!canPostNotifications()) return

    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val manager = NotificationManagerCompat.from(appContext)

    unseenUnread.take(3).forEach { notif ->
      val launchIntent = Intent(appContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_NOTIF_ID, notif.id)
        putExtra(EXTRA_NOTIF_TYPE, notif.type)
        putExtra(EXTRA_NOTIF_REF_ID, notif.referenceId)
      }
      val pendingIntent = PendingIntent.getActivity(
        appContext,
        notif.id.hashCode(),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

      val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(notif.title.ifBlank { "الإيمان للتجارة" })
        .setContentText(notif.message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(notif.message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setSound(soundUri)
        .setVibrate(longArrayOf(0L, 250L, 140L, 250L))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      runCatching {
        manager.notify(notif.id.hashCode(), builder.build())
      }
    }
  }

  private fun canPostNotifications(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }
  }

  companion object {
    const val CHANNEL_ID = "aleman_trade_alerts_v1"
    const val PREFS_NAME = "aleman_notif_seen_prefs"
    const val EXTRA_NOTIF_ID = "aleman_notif_id"
    const val EXTRA_NOTIF_TYPE = "aleman_notif_type"
    const val EXTRA_NOTIF_REF_ID = "aleman_notif_ref_id"
  }
}
