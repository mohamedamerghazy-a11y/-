package com.example.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.util.ArabicFormatter
import com.example.data.model.DeliveryZone
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.DiscountBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminDeliveryZonesUiState

@Composable
fun AdminDeliveryZonesScreen(
  uiState: AdminDeliveryZonesUiState,
  onSaveZone: (DeliveryZone) -> Unit,
  onDeleteZone: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingZone by remember { mutableStateOf<DeliveryZone?>(null) }
  var showDialog by remember { mutableStateOf(false) }
  var deletingZoneId by remember { mutableStateOf<String?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
    val msg = uiState.feedbackMessage ?: uiState.errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
    }
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = "إدارة مناطق التوصيل",
        subtitle = "تحديد المحافظات وأسعار ومواعيد الشحن",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingZone = null
          showDialog = true
        },
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier.testTag("admin_add_zone_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "إضافة منطقة توصيل")
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when {
        uiState.isLoading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CharcoalBlack)
          }
        }

        uiState.zones.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.LocalShipping,
              title = "لا توجد مناطق توصيل مضافة",
              description = "أضف مناطق ومحافظات التوصيل لتسهيل حساب الشحن للعملاء.",
              actionLabel = "إضافة منطقة",
              onActionClick = {
                editingZone = null
                showDialog = true
              },
            )
          }
        }

        else -> {
          LazyColumn(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
            contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(uiState.zones, key = { it.id }) { zone ->
              DeliveryZoneCard(
                zone = zone,
                onEdit = {
                  editingZone = zone
                  showDialog = true
                },
                onDelete = { deletingZoneId = zone.id },
              )
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    ZoneFormDialog(
      existing = editingZone,
      onDismiss = { showDialog = false },
      onConfirm = { zone ->
        onSaveZone(zone)
        showDialog = false
      },
    )
  }

  if (deletingZoneId != null) {
    AlertDialog(
      onDismissRequest = { deletingZoneId = null },
      containerColor = PureWhite,
      title = { Text("تأكيد حذف منطقة التوصيل", fontWeight = FontWeight.Bold, color = DiscountRed) },
      text = { Text("هل أنت متأكد من حذف هذه المنطقة؟") },
      confirmButton = {
        Button(
          onClick = {
            deletingZoneId?.let { onDeleteZone(it) }
            deletingZoneId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed),
        ) {
          Text("حذف المنطقة", color = PureWhite)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingZoneId = null }) {
          Text("إلغاء")
        }
      },
    )
  }
}

@Composable
private fun DeliveryZoneCard(
  zone: DeliveryZone,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "${zone.governorate} - ${zone.name}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
          Text(
            text = "المدة: ${zone.estimatedDuration}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        if (zone.isActive) SuccessBadge("مفعلة") else DiscountBadge("معطلة")
      }

      HorizontalDivider(color = BorderNeutral)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "رسوم الشحن: ${ArabicFormatter.formatCurrency(zone.fee)}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = CharcoalBlack)
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = DiscountRed)
          }
        }
      }
    }
  }
}

@Composable
private fun ZoneFormDialog(
  existing: DeliveryZone?,
  onDismiss: () -> Unit,
  onConfirm: (DeliveryZone) -> Unit,
) {
  var governorate by remember { mutableStateOf(existing?.governorate.orEmpty()) }
  var name by remember { mutableStateOf(existing?.name.orEmpty()) }
  var fee by remember { mutableStateOf(existing?.fee?.toString().orEmpty()) }
  var estimatedDuration by remember { mutableStateOf(existing?.estimatedDuration ?: "خلال ٢٤ - ٤٨ ساعة") }
  var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة منطقة توصيل جديدة" else "تعديل منطقة التوصيل",
        fontWeight = FontWeight.Bold,
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = governorate,
          onValueChange = { governorate = it },
          label = { Text("المحافظة (مثال: القاهرة، الجيزة، الإسكندرية)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("اسم المنطقة / المدينة (مثال: مدينة نصر، المعادي)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        OutlinedTextField(
          value = fee,
          onValueChange = { fee = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("تكلفة الشحن (ج.م)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        OutlinedTextField(
          value = estimatedDuration,
          onValueChange = { estimatedDuration = it },
          label = { Text("المدة المتوقعة للتسليم") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("تفعيل المنطقة الآن")
          Switch(
            checked = isActive,
            onCheckedChange = { isActive = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CharcoalBlack,
              checkedTrackColor = PrimaryYellow,
            ),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            (existing ?: DeliveryZone()).copy(
              governorate = governorate.trim(),
              name = name.trim(),
              fee = fee.toDoubleOrNull() ?: 0.0,
              estimatedDuration = estimatedDuration.trim().ifEmpty { "خلال ٢٤ - ٤٨ ساعة" },
              isActive = isActive,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryYellow,
          contentColor = CharcoalBlack,
        ),
      ) {
        Text("حفظ المنطقة", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}
