package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.model.OrderStatus
import com.example.core.util.ArabicFormatter
import com.example.data.model.CashVerificationLog
import com.example.data.model.Order
import com.example.data.model.StorePaymentSettings
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.components.ZoomableImagePreviewDialog
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminPaymentsUiState
import com.example.ui.viewmodel.ReceiptCleanupEvaluator
import com.example.ui.viewmodel.ReceiptCleanupScope
import java.text.SimpleDateFormat
import java.util.Locale

private val PAYMENT_FILTERS = listOf(
  null to "الكل",
  "pending_review" to "بانتظار التأكيد",
  "verified" to "تم التأكيد",
  "rejected" to "مرفوض",
  "has_receipt" to "تحتوي على إيصال",
)

@Composable
fun AdminPaymentsScreen(
  uiState: AdminPaymentsUiState,
  onSelectPaymentFilter: (String?) -> Unit,
  onUpdatePaymentStatus: (String, String, OrderStatus, String?) -> Unit,
  onDeleteSingleReceipt: (String) -> Unit,
  onDeleteReceiptsBatch: (ReceiptCleanupScope, Int) -> Unit,
  onSaveSettings: (StorePaymentSettings) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
  focusOrderId: String? = null,
  onOpenAlemanCashPreview: (() -> Unit)? = null,
  canViewCashTracking: Boolean = false,
  onOpenCashTracking: (() -> Unit)? = null,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var reviewingOrderId by remember { mutableStateOf<String?>(null) }
  var handledFocusOrderId by remember { mutableStateOf<String?>(null) }
  var previewReceiptOrder by remember { mutableStateOf<Order?>(null) }
  var deletingReceiptOrder by remember { mutableStateOf<Order?>(null) }
  var showBatchCleanupDialog by remember { mutableStateOf(false) }

  val visibleLogsCount = remember(uiState.cashVerificationLogs) {
    uiState.cashVerificationLogs.count { !it.status.startsWith("ignored") }
  }
  val reviewingOrder = remember(reviewingOrderId, uiState.orders) {
    reviewingOrderId?.let { targetId -> uiState.orders.firstOrNull { it.id == targetId } }
  }

  LaunchedEffect(focusOrderId, uiState.orders) {
    val targetId = focusOrderId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
    if (handledFocusOrderId == targetId) return@LaunchedEffect
    if (uiState.orders.any { it.id == targetId }) {
      selectedTab = 0
      reviewingOrderId = targetId
      handledFocusOrderId = targetId
    }
  }

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
        title = "إدارة المدفوعات والإيصالات",
        subtitle = "مراجعة التحويلات وإدارة أو حذف إيصالات الدفع",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 840.dp)
          .fillMaxSize(),
      ) {
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = PureWhite,
          contentColor = CharcoalBlack,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = PrimaryYellow,
            )
          },
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = {
              Text(
                text = "التحويلات (${uiState.transferOrders.size})",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            },
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = {
              Text(
                text = "التحقق الآلي ($visibleLogsCount)",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            },
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = {
              Text(
                text = "وسائل الدفع",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            },
          )
        }

        when (selectedTab) {
          0 -> {
            Column(modifier = Modifier.fillMaxSize()) {
              // شريط الفلاتر وزر تنظيف الإيصالات القديمة
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                  color = PrimaryYellowSoft,
                  border = BorderStroke(1.dp, PrimaryYellow),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                          text = "الإيصالات المرفقة: ${uiState.ordersWithReceiptsCount} إيصال",
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                          color = CharcoalBlack,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "يمكنك معاينة كل إيصال أو حذف الإيصالات القديمة لتوفير المساحة",
                          style = MaterialTheme.typography.labelSmall,
                          color = TextSecondary,
                        )
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Button(
                        onClick = { showBatchCleanupDialog = true },
                        enabled = uiState.ordersWithReceiptsCount > 0 && !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                          containerColor = CharcoalBlack,
                          contentColor = PrimaryYellow,
                        ),
                        shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("admin_cleanup_receipts_button"),
                      ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "حذف بالمدة",
                          fontWeight = FontWeight.Bold,
                          style = MaterialTheme.typography.labelMedium,
                          maxLines = 1,
                          softWrap = false,
                        )
                      }
                    }
                  }
                }

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  PAYMENT_FILTERS.forEach { (code, label) ->
                    val selected = uiState.selectedPaymentStatusFilter == code
                    FilterChip(
                      selected = selected,
                      onClick = { onSelectPaymentFilter(code) },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryYellow,
                        selectedLabelColor = CharcoalBlack,
                      ),
                      label = {
                        Text(
                          text = label,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                      },
                    )
                  }
                }
              }

              if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(color = CharcoalBlack)
                }
              } else if (uiState.transferOrders.isEmpty()) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(AlEmanDimens.SpaceLg),
                  contentAlignment = Alignment.Center,
                ) {
                  StoreEmptyStateCard(
                    icon = Icons.Filled.Payments,
                    title = "لا توجد طلبات تحويل مطابقة",
                    description = "تظهر هنا الطلبات المحولة عبر إنستاباي أو فودافون كاش لمطابقة رقم التحويل وصورة الإيصال.",
                    actionLabel = if (uiState.selectedPaymentStatusFilter != null) "عرض الكل" else "العودة",
                    onActionClick = {
                      if (uiState.selectedPaymentStatusFilter != null) {
                        onSelectPaymentFilter(null)
                      } else {
                        onBackClick()
                      }
                    },
                  )
                }
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  items(uiState.transferOrders, key = { it.id }) { order ->
                    PaymentReviewCard(
                      order = order,
                      isSubmitting = uiState.isSubmitting,
                      onApprove = {
                        onUpdatePaymentStatus(order.id, "verified", order.statusEnum, "تم تأكيد استلام التحويل")
                      },
                      onReject = {
                        onUpdatePaymentStatus(order.id, "rejected", order.statusEnum, "لم يتم التحقق من التحويل")
                      },
                      onOpenFullDetails = { reviewingOrderId = order.id },
                      onViewReceipt = { previewReceiptOrder = order },
                      onDeleteReceipt = { deletingReceiptOrder = order },
                    )
                  }
                }
              }
            }
          }

          1 -> {
            CashVerificationLogsTabContent(
              logs = uiState.cashVerificationLogs,
              onOpenAlemanCashPreview = onOpenAlemanCashPreview,
              canViewCashTracking = canViewCashTracking,
              onOpenCashTracking = onOpenCashTracking,
            )
          }

          2 -> {
            PaymentSettingsEditor(
              settings = uiState.settings,
              isSubmitting = uiState.isSubmitting,
              onSave = onSaveSettings,
            )
          }
        }
      }
    }
  }

  if (reviewingOrder != null) {
    PaymentOrderReviewDialog(
      order = reviewingOrder,
      isSubmitting = uiState.isSubmitting,
      onDismiss = { reviewingOrderId = null },
      onApprove = {
        onUpdatePaymentStatus(reviewingOrder.id, "verified", reviewingOrder.statusEnum, "تم تأكيد استلام التحويل")
        reviewingOrderId = null
      },
      onReject = {
        onUpdatePaymentStatus(reviewingOrder.id, "rejected", reviewingOrder.statusEnum, "لم يتم التحقق من التحويل")
        reviewingOrderId = null
      },
      onViewReceiptFullscreen = { previewReceiptOrder = reviewingOrder },
      onDeleteReceipt = { deletingReceiptOrder = reviewingOrder },
    )
  }

  val activePreview = previewReceiptOrder
  if (activePreview != null && !activePreview.receiptImageUrl.isNullOrBlank()) {
    ZoomableImagePreviewDialog(
      imageUrl = activePreview.receiptImageUrl,
      title = "إيصال طلب #${activePreview.orderNumber}",
      onDismiss = { previewReceiptOrder = null },
    )
  }

  val toDeleteReceipt = deletingReceiptOrder
  if (toDeleteReceipt != null) {
    AlertDialog(
      onDismissRequest = { deletingReceiptOrder = null },
      containerColor = PureWhite,
      title = {
        Text(
          text = "حذف صورة الإيصال",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = DiscountRed,
        )
      },
      text = {
        Text(
          text = "هل تريد حذف صورة إيصال الدفع للطلب #${toDeleteReceipt.orderNumber}؟ ستبقى تفاصيل الطلب والمبلغ كما هي.",
          color = TextPrimary,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            deletingReceiptOrder = null
            onDeleteSingleReceipt(toDeleteReceipt.id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
          modifier = Modifier.testTag("confirm_delete_single_receipt_button"),
        ) {
          Text("حذف الإيصال", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingReceiptOrder = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }

  if (showBatchCleanupDialog) {
    BatchReceiptCleanupDialog(
      orders = uiState.orders,
      onDismiss = { showBatchCleanupDialog = false },
      onConfirmCleanup = { scope, days ->
        showBatchCleanupDialog = false
        onDeleteReceiptsBatch(scope, days)
      },
    )
  }
}

@Composable
private fun BatchReceiptCleanupDialog(
  orders: List<Order>,
  onDismiss: () -> Unit,
  onConfirmCleanup: (ReceiptCleanupScope, Int) -> Unit,
) {
  var selectedScope by remember { mutableStateOf(ReceiptCleanupScope.OLDER_THAN_30_DAYS) }
  var customDaysInput by remember { mutableStateOf("14") }
  val parsedCustomDays = (customDaysInput.toIntOrNull() ?: 14).coerceAtLeast(1)

  val matchingOrders = remember(orders, selectedScope, parsedCustomDays) {
    ReceiptCleanupEvaluator.filterMatchingOrders(
      orders = orders,
      scope = selectedScope,
      customDays = parsedCustomDays,
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "حذف وتنظيف إيصالات الدفع",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "اختر الفئة أو المدة الزمنية لحذف صور الإيصالات من الطلبات مع الإبقاء على بيانات الطلبات والفواتير المالية كاملة.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )

        ReceiptCleanupScope.entries.forEach { scope ->
          val isSelected = selectedScope == scope
          Surface(
            onClick = { selectedScope = scope },
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            color = if (isSelected) PrimaryYellowSoft else GeneralBackground,
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) PrimaryYellow else BorderNeutral,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("cleanup_scope_${scope.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = isSelected,
                onClick = { selectedScope = scope },
                colors = RadioButtonDefaults.colors(selectedColor = CharcoalBlack),
              )
              Text(
                text = scope.arabicLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = CharcoalBlack,
              )
            }
          }
        }

        if (selectedScope == ReceiptCleanupScope.CUSTOM_DAYS) {
          OutlinedTextField(
            value = customDaysInput,
            onValueChange = { customDaysInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("حذف الإيصالات الأقدم من (عدد الأيام)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("cleanup_custom_days_input"),
          )
        }

        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = if (matchingOrders.isNotEmpty()) DiscountRedSoft else GeneralBackground,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = if (matchingOrders.isEmpty()) {
              "لا توجد إيصالات مطابقة لهذه المدة حاليًا."
            } else {
              "عدد الإيصالات التي سيتم حذفها: ${matchingOrders.size} إيصال"
            },
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (matchingOrders.isNotEmpty()) DiscountRed else TextSecondary,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmCleanup(selectedScope, parsedCustomDays) },
        enabled = matchingOrders.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
        modifier = Modifier.testTag("confirm_batch_cleanup_button"),
      ) {
        Text("حذف ${matchingOrders.size} إيصال", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}

@Composable
private fun PaymentSettingsEditor(
  settings: StorePaymentSettings,
  isSubmitting: Boolean,
  onSave: (StorePaymentSettings) -> Unit,
) {
  var vfNumber by remember(settings) { mutableStateOf(settings.vodafoneCashNumber) }
  var ipAddress by remember(settings) { mutableStateOf(settings.instaPayAddress) }
  var instructions by remember(settings) { mutableStateOf(settings.paymentInstructions) }
  var codEnabled by remember(settings) { mutableStateOf(settings.codEnabled) }
  var ipEnabled by remember(settings) { mutableStateOf(settings.instaPayEnabled) }
  var vfEnabled by remember(settings) { mutableStateOf(settings.vodafoneCashEnabled) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      AlEmanCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "تفعيل وسائل الدفع المتاحة في المتجر",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("الدفع عند الاستلام (COD)")
            Switch(
              checked = codEnabled,
              onCheckedChange = { codEnabled = it },
              colors = SwitchDefaults.colors(checkedThumbColor = CharcoalBlack, checkedTrackColor = PrimaryYellow),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("التحويل عبر إنستاباي (InstaPay)")
            Switch(
              checked = ipEnabled,
              onCheckedChange = { ipEnabled = it },
              colors = SwitchDefaults.colors(checkedThumbColor = CharcoalBlack, checkedTrackColor = PrimaryYellow),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("التحويل عبر فودافون كاش (Vodafone Cash)")
            Switch(
              checked = vfEnabled,
              onCheckedChange = { vfEnabled = it },
              colors = SwitchDefaults.colors(checkedThumbColor = CharcoalBlack, checkedTrackColor = PrimaryYellow),
            )
          }
        }
      }
    }

    item {
      AlEmanCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "بيانات وحسابات التحويل للعملاء",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )

          OutlinedTextField(
            value = vfNumber,
            onValueChange = { vfNumber = it },
            label = { Text("رقم محفظة فودافون كاش") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
          )

          OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("عنوان / معرف إنستاباي (IPA أو رقم الحساب)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
          )

          OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("تعليمات الدفع والتحويل للعميل") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
          )

          Button(
            onClick = {
              onSave(
                settings.copy(
                  vodafoneCashNumber = vfNumber.trim(),
                  instaPayAddress = ipAddress.trim(),
                  paymentInstructions = instructions.trim(),
                  codEnabled = codEnabled,
                  instaPayEnabled = ipEnabled,
                  vodafoneCashEnabled = vfEnabled,
                ),
              )
            },
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
              containerColor = PrimaryYellow,
              contentColor = CharcoalBlack,
            ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun CashVerificationLogsTabContent(
  logs: List<CashVerificationLog>,
  onOpenAlemanCashPreview: (() -> Unit)? = null,
  canViewCashTracking: Boolean = false,
  onOpenCashTracking: (() -> Unit)? = null,
) {
  val visibleLogs = remember(logs) { logs.filterNot { it.status.startsWith("ignored") } }
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (canViewCashTracking && onOpenCashTracking != null) {
      item {
        Button(
          onClick = onOpenCashTracking,
          colors = ButtonDefaults.buttonColors(
            containerColor = SuccessGreen,
            contentColor = PureWhite,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("payments_open_cash_tracking_button"),
        ) {
          Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            text = "متابعة تحويلات الكاش وتصدير الإكسيل",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    if (onOpenAlemanCashPreview != null) {
      item {
        Button(
          onClick = onOpenAlemanCashPreview,
          colors = ButtonDefaults.buttonColors(
            containerColor = CharcoalBlack,
            contentColor = PrimaryYellow,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("open_aleman_cash_preview_button"),
        ) {
          Icon(Icons.Filled.Payments, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            text = "فتح معاينة تطبيق «الإيمان كاش»",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    if (visibleLogs.isEmpty()) {
      item {
        StoreEmptyStateCard(
          icon = Icons.Filled.Payments,
          title = "لا توجد سجلات تحقق آلي بعد",
          description = "عندما يستقبل تطبيق الإيمان كاش رسالة تحويل وتتم مطابقتها أو فحصها، سيظهر السجل هنا لحظيًا.",
        )
      }
      return@LazyColumn
    }

    items(visibleLogs, key = { it.id }) { log ->
      val dateStr = remember(log.createdAt) {
        log.createdAt?.toDate()?.let { date ->
          SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.forLanguageTag("ar-EG")).format(date)
        } ?: "حديث"
      }
      AlEmanCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = log.providerName.ifBlank { log.senderAddress.ifBlank { "محفظة إلكترونية" } },
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
              modifier = Modifier.weight(1f),
            )
            when (log.status) {
              "matched", "verified" -> SuccessBadge("تم التأكيد الآلي")
              "duplicate" -> BrandPillBadge(
                text = "عملية مكررة",
                containerColor = PrimaryYellowSoft,
                contentColor = CharcoalBlack,
              )
              "ignored_sender" -> BrandPillBadge(
                text = "مرسل غير مفعل",
                containerColor = GeneralBackground,
                contentColor = TextSecondary,
              )
              else -> DiscountBadge("غير مطابق")
            }
          }
          Text(
            text = "المبلغ المستلم: ${ArabicFormatter.formatCurrency(log.amount)}  •  من: ${log.senderPhone.ifBlank { "-" }}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
          )
          if (log.verifiedByDeviceName.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.PhoneAndroid,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = "الهاتف: ${log.verifiedByDeviceName}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SuccessGreen,
              )
              if (log.isVerifiedOperation) {
                BrandPillBadge(
                  text = if (log.isSettled) "تم السحب" else "رصيد قائم",
                  containerColor = if (log.isSettled) GeneralBackground else PrimaryYellowSoft,
                  contentColor = if (log.isSettled) TextSecondary else CharcoalBlack,
                )
              }
            }
          }
          if (!log.matchedOrderNumber.isNullOrBlank()) {
            Text(
              text = "تم ربطه بالطلب: #${log.matchedOrderNumber}",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = SuccessGreen,
            )
          }
          if (log.transactionId.isNotBlank()) {
            Text(
              text = "رقم العملية: ${log.transactionId}",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
          if (log.reason.isNotBlank()) {
            Text(
              text = log.reason,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
          Text(
            text = dateStr,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
      }
    }
  }
}
