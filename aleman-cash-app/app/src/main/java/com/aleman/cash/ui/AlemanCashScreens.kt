package com.aleman.cash.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aleman.cash.R
import com.aleman.cash.model.CashProviderConfig
import com.aleman.cash.model.CashVerificationLogItem
import com.aleman.cash.model.CashVerifierSettings
import com.aleman.cash.model.LocalVerificationEvent
import com.aleman.cash.model.PendingTransferOrder
import com.aleman.cash.model.StaffAuthState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuthGateScreen(
  authState: StaffAuthState,
  staffLabel: String,
  isBusy: Boolean,
  statusBanner: String?,
  onSignInClick: () -> Unit,
  onRetryClick: () -> Unit,
  onSignOutClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(AlemanCashColors.Navy, AlemanCashColors.NavySoft, AlemanCashColors.EmeraldDark)
        )
      )
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.aleman_cash_icon_img),
          contentDescription = "شعار تطبيق الإيمان كاش",
          modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(20.dp))
        )
        Text(
          text = "الإيمان كاش",
          style = MaterialTheme.typography.headlineSmall,
          color = AlemanCashColors.Navy
        )
        Text(
          text = "المراقب التلقائي لرسائل المحافظ الإلكترونية وتأكيد طلبات المتجر",
          style = MaterialTheme.typography.bodyMedium,
          color = AlemanCashColors.TextMuted,
          textAlign = TextAlign.Center
        )

        if (!statusBanner.isNullOrBlank()) {
          Surface(
            color = AlemanCashColors.DangerTint,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = statusBanner,
              style = MaterialTheme.typography.bodyMedium,
              color = AlemanCashColors.DangerRed,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        when (authState) {
          StaffAuthState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 12.dp))
            Text("جاري التحقق من صلاحية الحساب...", style = MaterialTheme.typography.bodyMedium)
          }

          StaffAuthState.SignedOut -> {
            Button(
              onClick = onSignInClick,
              enabled = !isBusy,
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("google_sign_in_button")
            ) {
              if (isBusy) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = Color.White,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "تسجيل الدخول بحساب الإدارة (Google)",
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          StaffAuthState.Unauthorized -> {
            Surface(
              color = AlemanCashColors.GoldTint,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "هذا الحساب غير مخوّل لتأكيد المدفوعات",
                  style = MaterialTheme.typography.titleMedium,
                  color = AlemanCashColors.Navy
                )
                Text(
                  text = "الحساب الحالي: $staffLabel\nيلزم تسجيل الدخول بحساب المالك أو موظف لديه صلاحية إدارة المدفوعات والطلبات.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = AlemanCashColors.NavySoft
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("unauthorized_sign_out_button")
              ) {
                Text("تبديل الحساب")
              }
              Button(
                onClick = onRetryClick,
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("unauthorized_retry_button")
              ) {
                Text("إعادة التحقق")
              }
            }
          }

          StaffAuthState.Authorized -> Unit
        }
      }
    }
  }
}

@Composable
fun MonitorTab(
  settings: CashVerifierSettings,
  pendingOrdersCount: Int,
  matchedLogsCount: Int,
  localFeed: List<LocalVerificationEvent>,
  hasSmsPermission: Boolean,
  hasNotificationPermission: Boolean,
  isBusy: Boolean,
  onRequestPermissions: () -> Unit,
  onScanInboxClick: () -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AlemanCashColors.Navy),
        shape = MaterialTheme.shapes.large
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                painter = painterResource(id = R.drawable.aleman_cash_icon_img),
                contentDescription = "أيقونة الإيمان كاش",
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(12.dp))
              )
              Spacer(Modifier.width(12.dp))
              Column {
                Text(
                  text = if (settings.autoVerifyEnabled) "المراقب التلقائي نشط" else "المراقبة متوقفة مؤقتاً",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White
                )
                Text(
                  text = "المرسلون المفعلون: ${settings.providers.count { it.isEnabled }} من ${settings.providers.size}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFCBD5E1)
                )
              }
            }
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (settings.autoVerifyEnabled && hasSmsPermission) AlemanCashColors.Emerald else AlemanCashColors.Gold)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            StatMiniBox(
              label = "طلبات بانتظار الدفع",
              value = pendingOrdersCount.toString(),
              accent = AlemanCashColors.Gold,
              modifier = Modifier.weight(1f)
            )
            StatMiniBox(
              label = "تأكيدات ناجحة",
              value = matchedLogsCount.toString(),
              accent = AlemanCashColors.Emerald,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    if (!hasSmsPermission || !hasNotificationPermission) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = AlemanCashColors.GoldTint),
          border = BorderStroke(1.dp, AlemanCashColors.GoldDark.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AlemanCashColors.GoldDark)
              Spacer(Modifier.width(8.dp))
              Text(
                text = "صلاحيات مطلوبة لقراءة رسائل الدفع",
                style = MaterialTheme.typography.titleMedium,
                color = AlemanCashColors.Navy
              )
            }
            Text(
              text = "يحتاج التطبيق إلى إذن قراءة واستقبال رسائل SMS لاكتشاف تحويلات فودافون/أورنج/اتصالات/وي كاش فور وصولها، وإذن الإشعارات لتنبيهك لحظياً.",
              style = MaterialTheme.typography.bodyMedium,
              color = AlemanCashColors.NavySoft
            )
            Button(
              onClick = onRequestPermissions,
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("grant_sms_permissions_button")
            ) {
              Icon(Icons.Default.Security, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text("منح صلاحيات الرسائل والإشعارات", textAlign = TextAlign.Center)
            }
          }
        }
      }
    }

    item {
      Button(
        onClick = {
          if (hasSmsPermission) onScanInboxClick() else onRequestPermissions()
        },
        enabled = !isBusy,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("scan_inbox_button")
      ) {
        Icon(Icons.Default.MarkEmailRead, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("فحص رسائل الهاتف", maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }

    item {
      Text(
        text = "سجل التحقق الفوري في هذه الجلسة",
        style = MaterialTheme.typography.titleMedium,
        color = AlemanCashColors.Navy
      )
    }

    if (localFeed.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, AlemanCashColors.BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.Default.Sms,
              contentDescription = null,
              tint = AlemanCashColors.TextMuted,
              modifier = Modifier.size(36.dp)
            )
            Text(
              text = "بانتظار وصول رسالة دفع جديدة من المحافظ المفعّلة",
              style = MaterialTheme.typography.bodyMedium,
              color = AlemanCashColors.TextMuted,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    } else {
      items(localFeed, key = { it.id }) { event ->
        LocalEventCard(event)
      }
    }
  }
}

@Composable
fun PendingOrdersAndLogsTab(
  pendingOrders: List<PendingTransferOrder>,
  cloudLogs: List<CashVerificationLogItem>,
  targetOrderStage: String,
  isBusy: Boolean,
  onManualVerifyOrder: (PendingTransferOrder) -> Unit
) {
  var showCloudLogs by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = !showCloudLogs,
        onClick = { showCloudLogs = false },
        label = { Text("بانتظار الدفع (${pendingOrders.size})") },
        leadingIcon = { Icon(Icons.Default.PendingActions, contentDescription = null) },
        modifier = Modifier.testTag("pending_orders_filter_chip")
      )
      FilterChip(
        selected = showCloudLogs,
        onClick = { showCloudLogs = true },
        label = { Text("سجل القاعدة (${cloudLogs.size})") },
        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
        modifier = Modifier.testTag("cloud_logs_filter_chip")
      )
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (!showCloudLogs) {
        if (pendingOrders.isEmpty()) {
          item {
            EmptySectionCard(
              message = "لا توجد طلبات تحويل إلكتروني بانتظار المراجعة حالياً."
            )
          }
        } else {
          items(pendingOrders, key = { it.orderId }) { order ->
            PendingOrderCard(
              order = order,
              targetOrderStage = targetOrderStage,
              isBusy = isBusy,
              onVerifyClick = { onManualVerifyOrder(order) }
            )
          }
        }
      } else {
        if (cloudLogs.isEmpty()) {
          item {
            EmptySectionCard(
              message = "لا توجد عمليات تحقق مسجلة في قاعدة البيانات حتى الآن."
            )
          }
        } else {
          items(cloudLogs, key = { it.id }) { log ->
            CloudLogCard(log)
          }
        }
      }
    }
  }
}

@Composable
fun SettingsTab(
  settings: CashVerifierSettings,
  staffLabel: String,
  onToggleProvider: (String, Boolean) -> Unit,
  onSaveProvider: (name: String, aliases: String, isEnabled: Boolean, editingId: String?) -> Unit,
  onDeleteProvider: (String) -> Unit,
  onToggleAutoVerify: (Boolean) -> Unit,
  onSelectTargetStage: (String) -> Unit,
  onSignOut: () -> Unit
) {
  var editingProvider by remember { mutableStateOf<CashProviderConfig?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, AlemanCashColors.BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "تفعيل المراقبة الآلية للرسائل",
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = "قراءة رسائل الدفع فور وصولها وتأكيد الطلبات مطابقة الشروط تلقائياً.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = AlemanCashColors.TextMuted
                )
              }
              Switch(
                checked = settings.autoVerifyEnabled,
                onCheckedChange = onToggleAutoVerify,
                modifier = Modifier.testTag("auto_verify_switch")
              )
            }

            HorizontalDivider(color = AlemanCashColors.BorderLight)

            Text(
              text = "المرحلة التالية بعد تأكيد الدفع:",
              style = MaterialTheme.typography.labelLarge
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              FilterChip(
                selected = settings.targetOrderStatusAfterPayment == "preparing",
                onClick = { onSelectTargetStage("preparing") },
                label = { Text("جاري التجهيز (موصى به)") },
                modifier = Modifier
                  .weight(1f)
                  .testTag("stage_preparing_chip")
              )
              FilterChip(
                selected = settings.targetOrderStatusAfterPayment == "confirmed",
                onClick = { onSelectTargetStage("confirmed") },
                label = { Text("تم التأكيد") },
                modifier = Modifier
                  .weight(1f)
                  .testTag("stage_confirmed_chip")
              )
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "أسماء المرسلين والمحافظ المقبولة",
              style = MaterialTheme.typography.titleMedium
            )
            Text(
              text = "أي رسالة من مرسل غير مفعّل أو غير مدرج هنا سيتم تجاهلها تماماً.",
              style = MaterialTheme.typography.bodyMedium,
              color = AlemanCashColors.TextMuted
            )
          }
        }
      }

      items(settings.providers, key = { it.id }) { provider ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(
            1.dp,
            if (provider.isEnabled) AlemanCashColors.Emerald.copy(alpha = 0.45f) else AlemanCashColors.BorderLight
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = provider.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = if (provider.isEnabled) AlemanCashColors.Navy else AlemanCashColors.TextMuted
                )
                Text(
                  text = "الأسماء المقبولة: ${provider.senderAliases.joinToString(" • ")}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = AlemanCashColors.TextMuted
                )
              }
              Switch(
                checked = provider.isEnabled,
                onCheckedChange = { onToggleProvider(provider.id, it) },
                modifier = Modifier.testTag("provider_switch_${provider.id}")
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextButton(
                onClick = { editingProvider = provider },
                modifier = Modifier.testTag("edit_provider_${provider.id}")
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("تعديل الأسماء")
              }
              if (!provider.isBuiltIn) {
                TextButton(
                  onClick = { onDeleteProvider(provider.id) },
                  colors = ButtonDefaults.textButtonColors(contentColor = AlemanCashColors.DangerRed),
                  modifier = Modifier.testTag("delete_provider_${provider.id}")
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
          border = BorderStroke(1.dp, AlemanCashColors.BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("الحساب المتصل بقاعدة البيانات", style = MaterialTheme.typography.labelLarge)
              Text(staffLabel, style = MaterialTheme.typography.bodyMedium, color = AlemanCashColors.TextMuted)
            }
            IconButton(
              onClick = onSignOut,
              modifier = Modifier
                .size(48.dp)
                .testTag("settings_sign_out_button")
            ) {
              Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = "تسجيل الخروج",
                tint = AlemanCashColors.DangerRed
              )
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = { showAddDialog = true },
      containerColor = AlemanCashColors.EmeraldDark,
      contentColor = Color.White,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("add_provider_fab")
    ) {
      Icon(Icons.Default.Add, contentDescription = "إضافة مرسل أو محفظة")
    }
  }

  if (showAddDialog || editingProvider != null) {
    ProviderEditorDialog(
      initial = editingProvider,
      onDismiss = {
        showAddDialog = false
        editingProvider = null
      },
      onSave = { name, aliases, enabled ->
        onSaveProvider(name, aliases, enabled, editingProvider?.id)
        showAddDialog = false
        editingProvider = null
      }
    )
  }
}

@Composable
private fun StatMiniBox(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = Color.White.copy(alpha = 0.08f),
    shape = MaterialTheme.shapes.medium,
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = accent,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFE2E8F0)
      )
    }
  }
}

@Composable
private fun LocalEventCard(event: LocalVerificationEvent) {
  val isMatched = event.status == "verified" || event.status == "matched"
  val isIgnored = event.status.startsWith("ignored")
  val badgeColor = when {
    isMatched -> AlemanCashColors.Emerald
    isIgnored -> AlemanCashColors.TextMuted
    else -> AlemanCashColors.GoldDark
  }
  val bgColor = when {
    isMatched -> AlemanCashColors.EmeraldTint
    isIgnored -> Color(0xFFF1F5F9)
    else -> AlemanCashColors.GoldTint
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, AlemanCashColors.BorderLight),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
          Text(
            text = "${event.providerName} • ${event.senderAddress}",
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        Text(
          text = formatTimeOnly(event.timestampMillis),
          style = MaterialTheme.typography.labelSmall,
          color = AlemanCashColors.TextMuted
        )
      }
      Text(text = event.message, style = MaterialTheme.typography.bodyMedium)
      if (event.amount != null || !event.senderPhone.isNullOrBlank()) {
        Text(
          text = "المبلغ: ${event.amount ?: "-"} ج.م  |  من رقم: ${event.senderPhone ?: "-"}  |  العملية: ${event.transactionId ?: "-"}",
          style = MaterialTheme.typography.labelSmall,
          color = AlemanCashColors.TextMuted
        )
      }
    }
  }
}

@Composable
private fun PendingOrderCard(
  order: PendingTransferOrder,
  targetOrderStage: String,
  isBusy: Boolean,
  onVerifyClick: () -> Unit
) {
  val stageLabel = if (targetOrderStage == "confirmed") "تم التأكيد" else "جاري التجهيز"
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, AlemanCashColors.BorderLight),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "طلب #${order.orderNumber}",
          style = MaterialTheme.typography.titleMedium,
          color = AlemanCashColors.Navy
        )
        Surface(color = AlemanCashColors.GoldTint, shape = RoundedCornerShape(8.dp)) {
          Text(
            text = "${formatMoney(order.totalAmount)} ج.م",
            style = MaterialTheme.typography.labelLarge,
            color = AlemanCashColors.GoldDark,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }
      Text(
        text = "العميل: ${order.customerName.ifBlank { "عميل المتجر" }} (${order.customerPhone})",
        style = MaterialTheme.typography.bodyMedium
      )
      Text(
        text = "الرقم المحوَّل منه: ${order.effectiveSenderPhone.ifBlank { "غير مدخل" }}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = AlemanCashColors.EmeraldDark
      )
      Button(
        onClick = onVerifyClick,
        enabled = !isBusy,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("manual_verify_order_${order.orderId}")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("تأكيد الدفع ونقل الطلب إلى «$stageLabel»", textAlign = TextAlign.Center)
      }
    }
  }
}

@Composable
private fun CloudLogCard(log: CashVerificationLogItem) {
  val isMatched = log.status == "verified" || log.status == "matched"
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, AlemanCashColors.BorderLight),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${log.providerName} • #${log.transactionId}",
          style = MaterialTheme.typography.labelLarge,
          color = AlemanCashColors.Navy
        )
        Surface(
          color = if (isMatched) AlemanCashColors.EmeraldTint else AlemanCashColors.GoldTint,
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = when (log.status) {
              "verified", "matched" -> "مؤكد لطلب #${log.matchedOrderNumber.orEmpty()}"
              "duplicate" -> "مكرر"
              else -> "غير مطابق"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isMatched) AlemanCashColors.EmeraldDark else AlemanCashColors.GoldDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
      Text(
        text = "المبلغ: ${formatMoney(log.amount)} ج.م  |  من: ${log.senderPhone}",
        style = MaterialTheme.typography.bodyMedium
      )
      if (log.reason.isNotBlank()) {
        Text(
          text = log.reason,
          style = MaterialTheme.typography.bodyMedium,
          color = AlemanCashColors.TextMuted
        )
      }
    }
  }
}

@Composable
private fun EmptySectionCard(message: String) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, AlemanCashColors.BorderLight),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AlemanCashColors.TextMuted)
      Text(message, style = MaterialTheme.typography.bodyMedium, color = AlemanCashColors.TextMuted)
    }
  }
}

@Composable
private fun ProviderEditorDialog(
  initial: CashProviderConfig?,
  onDismiss: () -> Unit,
  onSave: (name: String, aliases: String, isEnabled: Boolean) -> Unit
) {
  var displayName by remember { mutableStateOf(initial?.displayName.orEmpty()) }
  var aliasesText by remember { mutableStateOf(initial?.senderAliases?.joinToString(", ").orEmpty()) }
  var isEnabled by remember { mutableStateOf(initial?.isEnabled ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initial == null) "إضافة مرسل / محفظة جديدة" else "تعديل ${initial.displayName}")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text("اسم المحفظة (مثال: إنستاباي أو محفظة خاصة)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_name_input")
        )
        OutlinedTextField(
          value = aliasesText,
          onValueChange = { aliasesText = it },
          label = { Text("أسماء المرسل في SMS (افصل بينها بفاصلة ,)") },
          placeholder = { Text("VF-Cash, VodafoneCash") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_aliases_input")
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
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
        modifier = Modifier.testTag("save_provider_dialog_button")
      ) {
        Text("حفظ")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("إلغاء") }
    }
  )
}

private fun formatMoney(amount: Double): String {
  return if (amount % 1.0 == 0.0) {
    String.format(Locale.US, "%.0f", amount)
  } else {
    String.format(Locale.US, "%.2f", amount)
  }
}

private fun formatTimeOnly(millis: Long): String {
  return SimpleDateFormat("hh:mm a", Locale.forLanguageTag("ar")).format(Date(millis))
}
