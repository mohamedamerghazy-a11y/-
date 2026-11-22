package com.example.cashpreview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.core.auth.onGoogleSignInClicked
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object CashColors {
  val Navy = Color(0xFF0F172A)
  val NavySoft = Color(0xFF1E293B)
  val Emerald = Color(0xFF059669)
  val EmeraldDark = Color(0xFF065F46)
  val EmeraldTint = Color(0xFFECFDF5)
  val Gold = Color(0xFFFFD000)
  val GoldDark = Color(0xFFD97706)
  val GoldTint = Color(0xFFFEF3C7)
  val SlateBg = Color(0xFFF8FAFC)
  val SurfaceWhite = Color(0xFFFFFFFF)
  val BorderLight = Color(0xFFE2E8F0)
  val TextMuted = Color(0xFF64748B)
  val DangerRed = Color(0xFFDC2626)
  val DangerTint = Color(0xFFFEF2F2)
}

private val CashTajawalFamily = FontFamily(
  Font(R.font.tajawal_regular, FontWeight.Normal),
  Font(R.font.tajawal_medium, FontWeight.Medium),
  Font(R.font.tajawal_bold, FontWeight.SemiBold),
  Font(R.font.tajawal_bold, FontWeight.Bold),
  Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
)

private val CashColorScheme = lightColorScheme(
  primary = CashColors.EmeraldDark,
  onPrimary = Color.White,
  primaryContainer = CashColors.EmeraldTint,
  onPrimaryContainer = CashColors.EmeraldDark,
  secondary = CashColors.GoldDark,
  onSecondary = Color.White,
  secondaryContainer = CashColors.GoldTint,
  onSecondaryContainer = CashColors.Navy,
  tertiary = CashColors.Navy,
  onTertiary = Color.White,
  background = CashColors.SlateBg,
  onBackground = CashColors.Navy,
  surface = CashColors.SurfaceWhite,
  onSurface = CashColors.Navy,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = CashColors.TextMuted,
  outline = CashColors.BorderLight,
  error = CashColors.DangerRed,
  errorContainer = CashColors.DangerTint,
  onErrorContainer = CashColors.DangerRed,
)

private val CashTypography = Typography(
  headlineSmall = TextStyle(fontFamily = CashTajawalFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp),
  titleLarge = TextStyle(fontFamily = CashTajawalFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp),
  titleMedium = TextStyle(fontFamily = CashTajawalFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
  bodyLarge = TextStyle(fontFamily = CashTajawalFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
  bodyMedium = TextStyle(fontFamily = CashTajawalFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
  labelLarge = TextStyle(fontFamily = CashTajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
  labelSmall = TextStyle(fontFamily = CashTajawalFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

private val CashShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp),
)

@Composable
fun AlemanCashEmblem(
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.24f))
      .background(CashColors.Navy),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.14f)) {
      val w = this.size.width
      val h = this.size.height
      val strokeW = w * 0.095f

      val shieldPath = Path().apply {
        moveTo(w * 0.50f, h * 0.04f)
        lineTo(w * 0.90f, h * 0.24f)
        lineTo(w * 0.90f, h * 0.72f)
        lineTo(w * 0.50f, h * 0.96f)
        lineTo(w * 0.10f, h * 0.72f)
        lineTo(w * 0.10f, h * 0.24f)
        close()
      }
      drawPath(
        path = shieldPath,
        color = CashColors.Emerald,
        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )

      val topCrown = Path().apply {
        moveTo(w * 0.16f, h * 0.21f)
        lineTo(w * 0.50f, h * 0.04f)
        lineTo(w * 0.84f, h * 0.21f)
      }
      drawPath(
        path = topCrown,
        color = CashColors.Gold,
        style = Stroke(width = strokeW * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )

      drawRoundRect(
        color = CashColors.Gold,
        topLeft = Offset(w * 0.26f, h * 0.34f),
        size = Size(w * 0.48f, h * 0.34f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        style = Stroke(width = strokeW * 0.82f),
      )

      val checkPath = Path().apply {
        moveTo(w * 0.36f, h * 0.52f)
        lineTo(w * 0.47f, h * 0.63f)
        lineTo(w * 0.68f, h * 0.38f)
      }
      drawPath(
        path = checkPath,
        color = CashColors.Emerald,
        style = Stroke(width = strokeW * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlemanCashPreviewHost(
  onBackToStore: () -> Unit,
  vm: CashPreviewViewModel = viewModel(),
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val authState by vm.authState.collectAsStateWithLifecycle()
  val staffLabel by vm.staffLabel.collectAsStateWithLifecycle()
  val settings by vm.settings.collectAsStateWithLifecycle()
  val pendingOrders by vm.pendingOrders.collectAsStateWithLifecycle()
  val cloudLogs by vm.cloudLogs.collectAsStateWithLifecycle()
  val localFeed by vm.localFeed.collectAsStateWithLifecycle()
  val statusBanner by vm.statusBanner.collectAsStateWithLifecycle()
  val isBusy by vm.isBusy.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var signingIn by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(statusBanner) {
    val msg = statusBanner
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      vm.clearBanner()
    }
  }

  val triggerGoogleSignIn: () -> Unit = {
    signingIn = true
    onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        signingIn = false
        vm.refreshAuthStatus()
      },
      onAuthError = { err ->
        signingIn = false
        vm.showMessage(err)
      },
      scope = scope,
      onAuthCancelled = { signingIn = false },
    )
  }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    MaterialTheme(
      colorScheme = CashColorScheme,
      typography = CashTypography,
      shapes = CashShapes,
    ) {
      if (authState != CashPreviewAuthState.Authorized) {
        CashPreviewAuthGate(
          authState = authState,
          staffLabel = staffLabel,
          isBusy = isBusy || signingIn,
          statusBanner = statusBanner,
          onSignInClick = triggerGoogleSignIn,
          onRetryClick = { vm.refreshAuthStatus() },
          onSignOutClick = { vm.signOut() },
        )
        return@MaterialTheme
      }

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = CashColors.SlateBg,
        topBar = {
          TopAppBar(
            title = {
              Column {
                Text(
                  text = "الإيمان كاش",
                  style = MaterialTheme.typography.titleLarge,
                  color = CashColors.Navy,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "تأكيد التحويلات الآلي",
                  style = MaterialTheme.typography.labelSmall,
                  color = CashColors.EmeraldDark,
                  maxLines = 1,
                )
              }
            },
            navigationIcon = {
              Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                AlemanCashEmblem(size = 38.dp)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = CashColors.SurfaceWhite,
              titleContentColor = CashColors.Navy,
            ),
          )
        },
        bottomBar = {
          NavigationBar(containerColor = CashColors.SurfaceWhite) {
            NavigationBarItem(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              icon = { Icon(Icons.Default.Sms, contentDescription = "المراقبة") },
              label = { Text("المراقبة") },
              modifier = Modifier.testTag("cash_nav_monitor_tab"),
            )
            NavigationBarItem(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              icon = { Icon(Icons.Default.PendingActions, contentDescription = "طلبات الدفع") },
              label = { Text("طلبات الدفع") },
              modifier = Modifier.testTag("cash_nav_orders_tab"),
            )
            NavigationBarItem(
              selected = selectedTab == 2,
              onClick = { selectedTab = 2 },
              icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
              label = { Text("الإعدادات") },
              modifier = Modifier.testTag("cash_nav_settings_tab"),
            )
          }
        },
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          when (selectedTab) {
            0 -> CashPreviewMonitorTab(
              settings = settings,
              pendingOrdersCount = pendingOrders.size,
              matchedLogsCount = cloudLogs.count { it.status == "matched" },
              localFeed = localFeed,
            )

            1 -> CashPreviewOrdersAndLogsTab(
              pendingOrders = pendingOrders,
              cloudLogs = cloudLogs,
              targetOrderStage = settings.targetOrderStatusAfterPayment,
              isBusy = isBusy || signingIn,
              onManualVerifyOrder = { vm.verifyOrderManually(it) },
            )

            else -> CashPreviewSettingsTab(
              settings = settings,
              staffLabel = staffLabel,
              onToggleProvider = { id, enabled -> vm.toggleProvider(id, enabled) },
              onSaveProvider = { name, aliases, enabled, editId ->
                vm.saveProvider(name, aliases, enabled, editId)
              },
              onDeleteProvider = { vm.deleteProvider(it) },
              onToggleAutoVerify = { vm.setAutoVerifyEnabled(it) },
              onSelectTargetStage = { vm.setTargetOrderStatus(it) },
              onSignOut = { vm.signOut() },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CashPreviewAuthGate(
  authState: CashPreviewAuthState,
  staffLabel: String,
  isBusy: Boolean,
  statusBanner: String?,
  onSignInClick: () -> Unit,
  onRetryClick: () -> Unit,
  onSignOutClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(CashColors.Navy, CashColors.NavySoft, CashColors.EmeraldDark),
        ),
      )
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        AlemanCashEmblem(size = 84.dp)
        Text(
          text = "الإيمان كاش",
          style = MaterialTheme.typography.headlineSmall,
          color = CashColors.Navy,
        )
        Text(
          text = "المراقب التلقائي لرسائل المحافظ الإلكترونية وتأكيد طلبات المتجر",
          style = MaterialTheme.typography.bodyMedium,
          color = CashColors.TextMuted,
          textAlign = TextAlign.Center,
        )

        if (!statusBanner.isNullOrBlank()) {
          Surface(
            color = CashColors.DangerTint,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = statusBanner,
              style = MaterialTheme.typography.bodyMedium,
              color = CashColors.DangerRed,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        when (authState) {
          CashPreviewAuthState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
            Text("جاري التحقق من صلاحية الحساب...", style = MaterialTheme.typography.bodyMedium)
          }

          CashPreviewAuthState.SignedOut -> {
            Button(
              onClick = onSignInClick,
              enabled = !isBusy,
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("cash_auth_google_button"),
            ) {
              if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("تسجيل الدخول بحساب الإدارة (Google)")
              }
            }
          }

          CashPreviewAuthState.Unauthorized -> {
            Surface(
              color = CashColors.GoldTint,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text("هذا الحساب غير مخوّل لتأكيد المدفوعات", style = MaterialTheme.typography.titleMedium, color = CashColors.Navy)
                Text(
                  text = "الحساب الحالي: $staffLabel",
                  style = MaterialTheme.typography.bodyMedium,
                  color = CashColors.NavySoft,
                )
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedButton(onClick = onSignOutClick, modifier = Modifier.weight(1f).height(48.dp)) {
                Text("تبديل الحساب")
              }
              Button(onClick = onRetryClick, modifier = Modifier.weight(1f).height(48.dp)) {
                Text("إعادة التحقق")
              }
            }
          }

          CashPreviewAuthState.Authorized -> Unit
        }
      }
    }
  }
}

@Composable
private fun CashPreviewMonitorTab(
  settings: CashPreviewSettings,
  pendingOrdersCount: Int,
  matchedLogsCount: Int,
  localFeed: List<LocalVerificationEventPreview>,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CashColors.Navy),
        shape = MaterialTheme.shapes.large,
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              AlemanCashEmblem(size = 44.dp)
              Spacer(Modifier.width(12.dp))
              Column {
                Text(
                  text = if (settings.autoVerifyEnabled) "المراقب التلقائي نشط" else "المراقبة متوقفة مؤقتاً",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                )
                Text(
                  text = "المرسلون المفعلون: ${settings.providers.count { it.isEnabled }} من ${settings.providers.size}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFCBD5E1),
                )
              }
            }
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (settings.autoVerifyEnabled) CashColors.Emerald else CashColors.Gold),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CashStatMiniBox(
              label = "طلبات بانتظار الدفع",
              value = pendingOrdersCount.toString(),
              accent = CashColors.Gold,
              modifier = Modifier.weight(1f),
            )
            CashStatMiniBox(
              label = "تأكيدات ناجحة",
              value = matchedLogsCount.toString(),
              accent = CashColors.Emerald,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    item {
      Text(
        text = "سجل التحقق الفوري في هذه الجلسة",
        style = MaterialTheme.typography.titleMedium,
        color = CashColors.Navy,
      )
    }

    if (localFeed.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, CashColors.BorderLight),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Default.Sms,
              contentDescription = null,
              tint = CashColors.TextMuted,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text = "بانتظار وصول رسالة دفع جديدة من المحافظ المفعّلة",
              style = MaterialTheme.typography.bodyMedium,
              color = CashColors.TextMuted,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(localFeed, key = { it.id }) { event ->
        CashLocalEventCard(event)
      }
    }
  }
}

@Composable
private fun CashPreviewOrdersAndLogsTab(
  pendingOrders: List<PendingTransferOrderPreview>,
  cloudLogs: List<CashVerificationLogPreview>,
  targetOrderStage: String,
  isBusy: Boolean,
  onManualVerifyOrder: (PendingTransferOrderPreview) -> Unit,
) {
  var showCloudLogs by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = !showCloudLogs,
        onClick = { showCloudLogs = false },
        label = { Text("بانتظار الدفع (${pendingOrders.size})") },
        leadingIcon = { Icon(Icons.Default.PendingActions, contentDescription = null) },
      )
      FilterChip(
        selected = showCloudLogs,
        onClick = { showCloudLogs = true },
        label = { Text("سجل القاعدة (${cloudLogs.size})") },
        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
      )
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (!showCloudLogs) {
        if (pendingOrders.isEmpty()) {
          item { CashEmptyCard("لا توجد طلبات تحويل إلكتروني بانتظار المراجعة حالياً.") }
        } else {
          items(pendingOrders, key = { it.orderId }) { order ->
            CashPendingOrderCard(
              order = order,
              targetOrderStage = targetOrderStage,
              isBusy = isBusy,
              onVerifyClick = { onManualVerifyOrder(order) },
            )
          }
        }
      } else {
        if (cloudLogs.isEmpty()) {
          item { CashEmptyCard("لا توجد عمليات تحقق مسجلة في قاعدة البيانات حتى الآن.") }
        } else {
          items(cloudLogs, key = { it.id }) { log ->
            CashCloudLogCard(log)
          }
        }
      }
    }
  }
}

@Composable
private fun CashPreviewSettingsTab(
  settings: CashPreviewSettings,
  staffLabel: String,
  onToggleProvider: (String, Boolean) -> Unit,
  onSaveProvider: (name: String, aliases: String, isEnabled: Boolean, editingId: String?) -> Unit,
  onDeleteProvider: (String) -> Unit,
  onToggleAutoVerify: (Boolean) -> Unit,
  onSelectTargetStage: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  var editingProvider by remember { mutableStateOf<CashPreviewProviderConfig?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, CashColors.BorderLight),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("تفعيل المراقبة الآلية للرسائل", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "قراءة رسائل الدفع فور وصولها وتأكيد الطلبات مطابقة الشروط تلقائياً.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = CashColors.TextMuted,
                )
              }
              Switch(checked = settings.autoVerifyEnabled, onCheckedChange = onToggleAutoVerify)
            }

            HorizontalDivider(color = CashColors.BorderLight)

            Text("المرحلة التالية بعد تأكيد الدفع:", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              FilterChip(
                selected = settings.targetOrderStatusAfterPayment == "preparing",
                onClick = { onSelectTargetStage("preparing") },
                label = { Text("جاري التجهيز (موصى به)") },
                modifier = Modifier.weight(1f),
              )
              FilterChip(
                selected = settings.targetOrderStatusAfterPayment == "confirmed",
                onClick = { onSelectTargetStage("confirmed") },
                label = { Text("تم التأكيد") },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      item {
        Column {
          Text("أسماء المرسلين والمحافظ المقبولة", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "أي رسالة من مرسل غير مفعّل أو غير مدرج هنا سيتم تجاهلها تماماً.",
            style = MaterialTheme.typography.bodyMedium,
            color = CashColors.TextMuted,
          )
        }
      }

      items(settings.providers, key = { it.id }) { provider ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(
            1.dp,
            if (provider.isEnabled) CashColors.Emerald.copy(alpha = 0.45f) else CashColors.BorderLight,
          ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = provider.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = if (provider.isEnabled) CashColors.Navy else CashColors.TextMuted,
                )
                Text(
                  text = "الأسماء المقبولة: ${provider.senderAliases.joinToString(" • ")}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = CashColors.TextMuted,
                )
              }
              Switch(
                checked = provider.isEnabled,
                onCheckedChange = { onToggleProvider(provider.id, it) },
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextButton(onClick = { editingProvider = provider }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("تعديل الأسماء")
              }
              if (!provider.isBuiltIn) {
                TextButton(
                  onClick = { onDeleteProvider(provider.id) },
                  colors = ButtonDefaults.textButtonColors(contentColor = CashColors.DangerRed),
                ) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("حذف")
                }
              }
            }
          }
        }
      }

      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, CashColors.BorderLight),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("الحساب المتصل بقاعدة البيانات", style = MaterialTheme.typography.labelLarge)
              Text(
                text = staffLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = CashColors.TextMuted,
              )
            }
            IconButton(onClick = onSignOut, modifier = Modifier.size(48.dp)) {
              Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = "تسجيل الخروج",
                tint = CashColors.DangerRed,
              )
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = { showAddDialog = true },
      containerColor = CashColors.EmeraldDark,
      contentColor = Color.White,
      modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
    ) {
      Icon(Icons.Default.Add, contentDescription = "إضافة مرسل أو محفظة")
    }
  }

  if (showAddDialog || editingProvider != null) {
    CashProviderEditorDialog(
      initial = editingProvider,
      onDismiss = {
        showAddDialog = false
        editingProvider = null
      },
      onSave = { name, aliases, enabled ->
        onSaveProvider(name, aliases, enabled, editingProvider?.id)
        showAddDialog = false
        editingProvider = null
      },
    )
  }
}

@Composable
private fun CashStatMiniBox(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color.White.copy(alpha = 0.08f),
    shape = MaterialTheme.shapes.medium,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = accent,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFE2E8F0),
      )
    }
  }
}

@Composable
private fun CashLocalEventCard(event: LocalVerificationEventPreview) {
  val isMatched = event.status == "matched"
  val isIgnored = event.status.startsWith("ignored")
  val badgeColor = when {
    isMatched -> CashColors.Emerald
    isIgnored -> CashColors.TextMuted
    else -> CashColors.GoldDark
  }
  val bgColor = when {
    isMatched -> CashColors.EmeraldTint
    isIgnored -> Color(0xFFF1F5F9)
    else -> CashColors.GoldTint
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, CashColors.BorderLight),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
          Text(
            text = "${event.providerName} • ${event.senderAddress}",
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
        Text(
          text = formatPreviewTimeOnly(event.timestampMillis),
          style = MaterialTheme.typography.labelSmall,
          color = CashColors.TextMuted,
        )
      }
      Text(text = event.message, style = MaterialTheme.typography.bodyMedium)
      if (event.amount != null || !event.senderPhone.isNullOrBlank()) {
        Text(
          text = "المبلغ: ${event.amount ?: "-"} ج.م  |  من رقم: ${event.senderPhone ?: "-"}  |  العملية: ${event.transactionId ?: "-"}",
          style = MaterialTheme.typography.labelSmall,
          color = CashColors.TextMuted,
        )
      }
    }
  }
}

@Composable
private fun CashPendingOrderCard(
  order: PendingTransferOrderPreview,
  targetOrderStage: String,
  isBusy: Boolean,
  onVerifyClick: () -> Unit,
) {
  val stageLabel = if (targetOrderStage == "confirmed") "تم التأكيد" else "جاري التجهيز"
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, CashColors.BorderLight),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("طلب #${order.orderNumber}", style = MaterialTheme.typography.titleMedium, color = CashColors.Navy)
        Surface(color = CashColors.GoldTint, shape = RoundedCornerShape(8.dp)) {
          Text(
            text = "${formatPreviewMoney(order.totalAmount)} ج.م",
            style = MaterialTheme.typography.labelLarge,
            color = CashColors.GoldDark,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
      Text("العميل: ${order.customerName.ifBlank { "عميل المتجر" }} (${order.customerPhone})", style = MaterialTheme.typography.bodyMedium)
      Text(
        text = "الرقم المحوَّل منه: ${order.effectiveSenderPhone.ifBlank { "غير مدخل" }}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = CashColors.EmeraldDark,
      )
      Button(
        onClick = onVerifyClick,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("تأكيد الدفع ونقل الطلب إلى «$stageLabel»")
      }
    }
  }
}

@Composable
private fun CashCloudLogCard(log: CashVerificationLogPreview) {
  val isMatched = log.status == "matched"
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, CashColors.BorderLight),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("${log.providerName} • #${log.transactionId}", style = MaterialTheme.typography.labelLarge, color = CashColors.Navy)
        Surface(
          color = if (isMatched) CashColors.EmeraldTint else CashColors.GoldTint,
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = when (log.status) {
              "matched" -> "مؤكد لطلب #${log.matchedOrderNumber.orEmpty()}"
              "duplicate" -> "مكرر"
              else -> "غير مطابق"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isMatched) CashColors.EmeraldDark else CashColors.GoldDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
      Text("المبلغ: ${formatPreviewMoney(log.amount)} ج.م  |  من: ${log.senderPhone}", style = MaterialTheme.typography.bodyMedium)
      if (log.reason.isNotBlank()) {
        Text(log.reason, style = MaterialTheme.typography.bodyMedium, color = CashColors.TextMuted)
      }
    }
  }
}

@Composable
private fun CashEmptyCard(message: String) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, CashColors.BorderLight),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CashColors.TextMuted)
      Text(message, style = MaterialTheme.typography.bodyMedium, color = CashColors.TextMuted)
    }
  }
}

@Composable
private fun CashProviderEditorDialog(
  initial: CashPreviewProviderConfig?,
  onDismiss: () -> Unit,
  onSave: (name: String, aliases: String, isEnabled: Boolean) -> Unit,
) {
  var displayName by remember { mutableStateOf(initial?.displayName.orEmpty()) }
  var aliasesText by remember { mutableStateOf(initial?.senderAliases?.joinToString(", ").orEmpty()) }
  var isEnabled by remember { mutableStateOf(initial?.isEnabled ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "إضافة مرسل / محفظة جديدة" else "تعديل ${initial.displayName}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text("اسم المحفظة (مثال: إنستاباي)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = aliasesText,
          onValueChange = { aliasesText = it },
          label = { Text("أسماء المرسل في SMS (افصل بفاصلة ,)") },
          placeholder = { Text("VF-Cash, VodafoneCash") },
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("مفعّل حالياً", style = MaterialTheme.typography.bodyLarge)
          Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(displayName, aliasesText, isEnabled) },
        enabled = displayName.isNotBlank(),
      ) {
        Text("حفظ")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("إلغاء") }
    },
  )
}

private fun formatPreviewMoney(amount: Double): String {
  return if (amount % 1.0 == 0.0) {
    String.format(Locale.US, "%.0f", amount)
  } else {
    String.format(Locale.US, "%.2f", amount)
  }
}

private fun formatPreviewTimeOnly(millis: Long): String {
  return SimpleDateFormat("hh:mm a", Locale.forLanguageTag("ar")).format(Date(millis))
}
