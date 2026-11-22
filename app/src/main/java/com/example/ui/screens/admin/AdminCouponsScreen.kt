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
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.data.model.Coupon
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
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminCouponsUiState

@Composable
fun AdminCouponsScreen(
  uiState: AdminCouponsUiState,
  onSaveCoupon: (Coupon) -> Unit,
  onDeleteCoupon: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
  var showDialog by remember { mutableStateOf(false) }
  var deletingCouponId by remember { mutableStateOf<String?>(null) }

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
        title = "إدارة كوبونات الخصم",
        subtitle = "إنشاء ومتابعة أكواد الخصم والحدود",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingCoupon = null
          showDialog = true
        },
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier.testTag("admin_add_coupon_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "إضافة كوبون جديد")
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

        uiState.coupons.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.ConfirmationNumber,
              title = "لا توجد كوبونات خصم حاليًا",
              description = "اضغط على زر الإضافة لإنشاء كود خصم جديد لعملائك.",
              actionLabel = "إضافة كود خصم",
              onActionClick = {
                editingCoupon = null
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
            items(uiState.coupons, key = { it.id }) { coupon ->
              CouponItemCard(
                coupon = coupon,
                onEdit = {
                  editingCoupon = coupon
                  showDialog = true
                },
                onDelete = { deletingCouponId = coupon.id },
              )
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    CouponFormDialog(
      existing = editingCoupon,
      onDismiss = { showDialog = false },
      onConfirm = { coupon ->
        onSaveCoupon(coupon)
        showDialog = false
      },
    )
  }

  if (deletingCouponId != null) {
    AlertDialog(
      onDismissRequest = { deletingCouponId = null },
      containerColor = PureWhite,
      title = { Text("تأكيد حذف الكوبون", fontWeight = FontWeight.Bold, color = DiscountRed) },
      text = { Text("هل أنت متأكد من حذف هذا الكوبون نهائيًا؟ لن يتمكن العملاء من استخدامه بعد الآن.") },
      confirmButton = {
        Button(
          onClick = {
            deletingCouponId?.let { onDeleteCoupon(it) }
            deletingCouponId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed),
        ) {
          Text("حذف الكوبون", color = PureWhite)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingCouponId = null }) {
          Text("إلغاء")
        }
      },
    )
  }
}

@Composable
private fun CouponItemCard(
  coupon: Coupon,
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
            text = coupon.code,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
          Text(
            text = if (coupon.discountType == "percent") "خصم ${coupon.discountValue.toInt()}%" else "خصم ${ArabicFormatter.formatCurrency(coupon.discountValue)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = SuccessGreen,
          )
        }
        if (coupon.isActive) SuccessBadge("مفعّل") else DiscountBadge("معطّل")
      }

      HorizontalDivider(color = BorderNeutral)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "الحد الأدنى: ${ArabicFormatter.formatCurrency(coupon.minOrderAmount)}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
        Text(
          text = "مرات الاستخدام: ${coupon.usedCount}${coupon.usageLimit?.let { "/$it" } ?: ""}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
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

@Composable
private fun CouponFormDialog(
  existing: Coupon?,
  onDismiss: () -> Unit,
  onConfirm: (Coupon) -> Unit,
) {
  var code by remember { mutableStateOf(existing?.code.orEmpty()) }
  var isPercent by remember { mutableStateOf(existing?.discountType == "percent") }
  var discountValue by remember { mutableStateOf(existing?.discountValue?.toString().orEmpty()) }
  var minOrderAmount by remember { mutableStateOf(existing?.minOrderAmount?.toString().orEmpty()) }
  var maxDiscount by remember { mutableStateOf(existing?.maxDiscountAmount?.toString().orEmpty()) }
  var usageLimit by remember { mutableStateOf(existing?.usageLimit?.toString().orEmpty()) }
  var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة كود خصم جديد" else "تعديل كود الخصم",
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
          value = code,
          onValueChange = { code = it.uppercase() },
          label = { Text("كود الخصم (مثال: EMAN10)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          RadioButton(
            selected = !isPercent,
            onClick = { isPercent = false },
            colors = RadioButtonDefaults.colors(selectedColor = CharcoalBlack),
          )
          Text("مبلغ ثابت (ج.م)", modifier = Modifier.padding(end = 12.dp))
          RadioButton(
            selected = isPercent,
            onClick = { isPercent = true },
            colors = RadioButtonDefaults.colors(selectedColor = CharcoalBlack),
          )
          Text("نسبة مئوية (%)")
        }

        OutlinedTextField(
          value = discountValue,
          onValueChange = { discountValue = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(if (isPercent) "نسبة الخصم %" else "قيمة الخصم ج.م") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        OutlinedTextField(
          value = minOrderAmount,
          onValueChange = { minOrderAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("الحد الأدنى للطلب (ج.م)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        if (isPercent) {
          OutlinedTextField(
            value = maxDiscount,
            onValueChange = { maxDiscount = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("أقصى مبلغ للخصم (ج.م) - اختياري") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
          )
        }

        OutlinedTextField(
          value = usageLimit,
          onValueChange = { usageLimit = it.filter { ch -> ch.isDigit() } },
          label = { Text("حد أقصى لعدد الاستخدامات (اختياري)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("تفعيل الكوبون الآن")
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
            (existing ?: Coupon()).copy(
              code = code.trim().uppercase(),
              discountType = if (isPercent) "percent" else "fixed",
              discountValue = discountValue.toDoubleOrNull() ?: 0.0,
              minOrderAmount = minOrderAmount.toDoubleOrNull() ?: 0.0,
              maxDiscountAmount = maxDiscount.toDoubleOrNull(),
              usageLimit = usageLimit.toIntOrNull(),
              isActive = isActive,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryYellow,
          contentColor = CharcoalBlack,
        ),
      ) {
        Text("حفظ الكوبون", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}
