package com.aleman.cash

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aleman.cash.data.CashNotificationHelper
import com.aleman.cash.data.FirebaseInitializer
import com.aleman.cash.model.StaffAuthState
import com.aleman.cash.ui.AlemanCashColors
import com.aleman.cash.ui.AlemanCashTheme
import com.aleman.cash.ui.AlemanCashViewModel
import com.aleman.cash.ui.AuthGateScreen
import com.aleman.cash.ui.MonitorTab
import com.aleman.cash.ui.PendingOrdersAndLogsTab
import com.aleman.cash.ui.SettingsTab

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    FirebaseInitializer.ensureInitialized(applicationContext)
    CashNotificationHelper.ensureChannel(applicationContext)

    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlemanCashTheme {
          AlemanCashRoot(activity = this)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlemanCashRoot(
  activity: ComponentActivity,
  vm: AlemanCashViewModel = viewModel()
) {
  val context = LocalContext.current
  val authState by vm.authState.collectAsStateWithLifecycle()
  val staffLabel by vm.staffLabel.collectAsStateWithLifecycle()
  val settings by vm.settings.collectAsStateWithLifecycle()
  val pendingOrders by vm.pendingOrders.collectAsStateWithLifecycle()
  val cloudLogs by vm.cloudLogs.collectAsStateWithLifecycle()
  val localFeed by vm.localFeed.collectAsStateWithLifecycle()
  val statusBanner by vm.statusBanner.collectAsStateWithLifecycle()
  val isBusy by vm.isBusy.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var hasSmsPermission by remember { mutableStateOf(checkSmsGranted(context)) }
  var hasNotifPermission by remember { mutableStateOf(checkNotificationGranted(context)) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) {
    hasSmsPermission = checkSmsGranted(context)
    hasNotifPermission = checkNotificationGranted(context)
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(statusBanner) {
    val msg = statusBanner
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      vm.clearBanner()
    }
  }

  if (authState != StaffAuthState.Authorized) {
    AuthGateScreen(
      authState = authState,
      staffLabel = staffLabel,
      isBusy = isBusy,
      statusBanner = statusBanner,
      onSignInClick = { vm.signInWithGoogle(activity) },
      onRetryClick = { vm.refreshAuthStatus() },
      onSignOutClick = { vm.signOut() }
    )
    return
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("الإيمان كاش • تأكيد التحويلات") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = AlemanCashColors.SurfaceWhite,
          titleContentColor = AlemanCashColors.Navy
        )
      )
    },
    bottomBar = {
      NavigationBar(containerColor = AlemanCashColors.SurfaceWhite) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Sms, contentDescription = "المراقبة") },
          label = { Text("المراقبة") },
          modifier = Modifier.testTag("nav_monitor_tab")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.PendingActions, contentDescription = "طلبات الدفع") },
          label = { Text("طلبات الدفع") },
          modifier = Modifier.testTag("nav_orders_tab")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
          label = { Text("الإعدادات") },
          modifier = Modifier.testTag("nav_settings_tab")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedTab) {
        0 -> MonitorTab(
          settings = settings,
          pendingOrdersCount = pendingOrders.size,
          matchedLogsCount = cloudLogs.count { it.status == "matched" },
          localFeed = localFeed,
          hasSmsPermission = hasSmsPermission,
          hasNotificationPermission = hasNotifPermission,
          isBusy = isBusy,
          onRequestPermissions = {
            val perms = buildList {
              add(Manifest.permission.RECEIVE_SMS)
              add(Manifest.permission.READ_SMS)
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
              }
            }.toTypedArray()
            permissionLauncher.launch(perms)
          },
          onScanInboxClick = { vm.scanRecentDeviceInbox(context) }
        )

        1 -> PendingOrdersAndLogsTab(
          pendingOrders = pendingOrders,
          cloudLogs = cloudLogs,
          targetOrderStage = settings.targetOrderStatusAfterPayment,
          isBusy = isBusy,
          onManualVerifyOrder = { vm.verifyOrderManually(it) }
        )

        else -> SettingsTab(
          settings = settings,
          staffLabel = staffLabel,
          onToggleProvider = { id, enabled -> vm.toggleProvider(id, enabled) },
          onSaveProvider = { name, aliases, enabled, editId ->
            vm.saveProvider(name, aliases, enabled, editId)
          },
          onDeleteProvider = { vm.deleteProvider(it) },
          onToggleAutoVerify = { vm.setAutoVerifyEnabled(it) },
          onSelectTargetStage = { vm.setTargetOrderStatus(it) },
          onSignOut = { vm.signOut() }
        )
      }
    }
  }
}

private fun checkSmsGranted(context: android.content.Context): Boolean {
  val receive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
    PackageManager.PERMISSION_GRANTED
  val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
    PackageManager.PERMISSION_GRANTED
  return receive && read
}

private fun checkNotificationGranted(context: android.content.Context): Boolean {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
  return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED
}
