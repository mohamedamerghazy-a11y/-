package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.cashpreview.AlemanCashPreviewHost
import com.example.core.notifications.StoreNotificationDispatcher
import com.example.ui.navigation.AppNavHost
import com.example.ui.theme.AlEmanTradeTheme

data class NotificationDeepLink(
  val type: String,
  val referenceId: String?,
)

class MainActivity : ComponentActivity() {
  private val pendingDeepLink = mutableStateOf<NotificationDeepLink?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)

    splashScreen.setOnExitAnimationListener { splashScreenView ->
      splashScreenView.view.animate()
        .alpha(0f)
        .setDuration(180L)
        .withEndAction { splashScreenView.remove() }
        .start()
    }

    parseNotificationIntent(intent)

    enableEdgeToEdge()
    setContent {
      var showAlemanCashPreview by rememberSaveable { mutableStateOf(false) }
      if (showAlemanCashPreview) {
        AlemanCashPreviewHost(
          onBackToStore = { showAlemanCashPreview = false },
        )
      } else {
        AlEmanTradeTheme {
          AppNavHost(
            pendingNotificationDeepLink = pendingDeepLink.value,
            onConsumeNotificationDeepLink = { pendingDeepLink.value = null },
            onOpenAlemanCashPreview = { showAlemanCashPreview = true },
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    parseNotificationIntent(intent)
  }

  private fun parseNotificationIntent(intent: Intent?) {
    val type = intent?.getStringExtra(StoreNotificationDispatcher.EXTRA_NOTIF_TYPE) ?: return
    val refId = intent.getStringExtra(StoreNotificationDispatcher.EXTRA_NOTIF_REF_ID)
    pendingDeepLink.value = NotificationDeepLink(type = type, referenceId = refId)
    intent.removeExtra(StoreNotificationDispatcher.EXTRA_NOTIF_TYPE)
    intent.removeExtra(StoreNotificationDispatcher.EXTRA_NOTIF_REF_ID)
    intent.removeExtra(StoreNotificationDispatcher.EXTRA_NOTIF_ID)
  }
}
