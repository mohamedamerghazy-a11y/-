package com.example.ui.screens.admin

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.util.ArabicFormatter
import com.example.core.util.CashExcelExporter
import com.example.data.model.CashVerificationLog
import com.example.data.model.CashVerifierAccount
import com.example.ui.components.AlEmanCard
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminCashTrackingUiState
import com.example.ui.viewmodel.CashDateFilterMode
import com.example.ui.viewmodel.CashDeviceSummary
import com.example.ui.viewmodel.localDayStartToUtcPickerMillis
import com.example.ui.viewmodel.utcDatePickerMillisToLocalDayStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ActiveDatePickerTarget {
  SINGLE_DAY,
  RANGE_START,
  RANGE_END,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CashTrackingSettingsTab(
  uiState: AdminCashTrackingUiState,
  canManageCashAccounts: Boolean,
  canSettleCashDevice: Boolean,
  actorDisplayName: String,
  onSelectFilterMode: (CashDateFilterMode) -> Unit,
  onSelectSingleDay: (Long) -> Unit,
  onSelectDateRange: (Long, Long) -> Unit,
  onSaveCashAccount: (email: String, deviceName: String, phoneNumber: String?, isActive: Boolean, isEdit: Boolean) -> Unit,
  onToggleCashAccount: (CashVerifierAccount) -> Unit,
  onDeleteCashAccount: (CashVerifierAccount) -> Unit,
  onSettleDevice: (CashDeviceSummary, String, String?) -> Unit,
  onExportExcelToUri: (android.net.Uri) -> Unit,
  onShareExcel: ((Intent) -> Unit) -> Unit,
) {
  val context = LocalContext.current
  val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
  val dateTimeFmt = remember { SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale("ar", "EG")) }

  var datePickerTarget by remember { mutableStateOf<ActiveDatePickerTarget?>(null) }
  var showAccountDialog by remember { mutableStateOf(false) }
  var editingAccount by remember { mutableStateOf<CashVerifierAccount?>(null) }
  var deletingAccount by remember { mutableStateOf<CashVerifierAccount?>(null) }
  var settlingDevice by remember { mutableStateOf<CashDeviceSummary?>(null) }
  var expandedDeviceKey by remember { mutableStateOf<String?>(null) }
  var showAccountsManager by remember { mutableStateOf(true) }
  var showWithdrawalHistory by remember { mutableStateOf(false) }

  val saveExcelLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(CashExcelExporter.XLSX_MIME_TYPE),
  ) { uri ->
    if (uri != null) {
      onExportExcelToUri(uri)
    }
  }

  if (uiState.isLoading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = CharcoalBlack)
    }
    return
  }

  val summaries = uiState.deviceSummaries

  LazyColumn(
    modifier = Modifier
      .widthIn(max = 840.dp)
      .fillMaxSize(),
    contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. بطاقة التقويم وفلترة التواريخ + تصدير الإكسيل
    item {
      AlEmanCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(PrimaryYellow, RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "متابعة تحويلات الكاش بالتقويم",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
              Text(
                text = "الفترة المختارة: ${uiState.periodTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            }
          }

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CashDateFilterMode.entries.forEach { mode ->
              val selected = uiState.filterMode == mode
              FilterChip(
                selected = selected,
                onClick = {
                  onSelectFilterMode(mode)
                  if (mode == CashDateFilterMode.SINGLE_DAY) {
                    datePickerTarget = ActiveDatePickerTarget.SINGLE_DAY
                  }
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryYellow,
                  selectedLabelColor = CharcoalBlack,
                ),
                label = {
                  Text(
                    text = mode.arabicLabel,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  )
                },
                modifier = Modifier.testTag("cash_filter_${mode.name.lowercase()}"),
              )
            }
          }

          when (uiState.filterMode) {
            CashDateFilterMode.SINGLE_DAY -> {
              OutlinedButton(
                onClick = { datePickerTarget = ActiveDatePickerTarget.SINGLE_DAY },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CharcoalBlack),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("cash_pick_single_day"),
              ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "اليوم المحدد: ${dateFmt.format(Date(uiState.selectedDayStartMillis))} (تغيير)",
                  color = CharcoalBlack,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            CashDateFilterMode.DATE_RANGE -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = { datePickerTarget = ActiveDatePickerTarget.RANGE_START },
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, CharcoalBlack),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("cash_pick_range_start"),
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("من تاريخ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                      text = dateFmt.format(Date(uiState.rangeStartMillis)),
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                  }
                }
                OutlinedButton(
                  onClick = { datePickerTarget = ActiveDatePickerTarget.RANGE_END },
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, CharcoalBlack),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("cash_pick_range_end"),
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إلى تاريخ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                      text = dateFmt.format(Date(uiState.rangeEndMillis)),
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                  }
                }
              }
            }

            CashDateFilterMode.ALL -> Unit
          }

          HorizontalDivider(color = BorderNeutral)

          // ملخص المؤشرات السريعة
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CashStatTile(
              label = "إجمالي الفترة",
              value = ArabicFormatter.formatCurrency(uiState.totalPeriodAmount),
              accent = CharcoalBlack,
              container = PrimaryYellowSoft,
              modifier = Modifier.weight(1f),
            )
            CashStatTile(
              label = "عدد العمليات",
              value = "${uiState.totalPeriodOperations} عملية",
              accent = RoyalBlue,
              container = RoyalBlueSoft,
              modifier = Modifier.weight(1f),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CashStatTile(
              label = "الرصيد القائم (غير المسحوب)",
              value = ArabicFormatter.formatCurrency(uiState.totalUnsettledBalance),
              accent = SuccessGreen,
              container = SuccessGreenSoft,
              modifier = Modifier.weight(1f),
            )
            CashStatTile(
              label = "هواتف الإيمان كاش",
              value = "${summaries.size} هاتف",
              accent = CharcoalBlack,
              container = GeneralBackground,
              modifier = Modifier.weight(1f),
            )
          }

          // أزرار تصدير ملف إكسيل (.xlsx)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = {
                saveExcelLauncher.launch(CashExcelExporter.suggestedFileName(uiState.periodSlug))
              },
              enabled = !uiState.isExporting,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = SuccessGreen,
                contentColor = PureWhite,
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("cash_export_excel_save"),
            ) {
              if (uiState.isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PureWhite, strokeWidth = 2.dp)
              } else {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "حفظ إكسيل",
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }

            OutlinedButton(
              onClick = {
                onShareExcel { shareIntent ->
                  context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو فتح ملف الإكسيل"))
                }
              },
              enabled = !uiState.isExporting,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.2.dp, SuccessGreen),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("cash_export_excel_share"),
            ) {
              Icon(Icons.Filled.Share, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "مشاركة إكسيل",
                color = SuccessGreen,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.TableView, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
            Text(
              text = "يحتوي ملف الإكسيل (.xlsx) على ٣ أوراق: ملخص الهواتف، تفاصيل كل عملية كاش، وسجل عمليات السحب.",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }
      }
    }

    // 2. عنوان متابعة كل هاتف
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "حسابات وتحويلات كل هاتف (${summaries.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        if (!canSettleCashDevice) {
          BrandPillBadge(
            text = "التصفير للمالك والمحاسب فقط",
            containerColor = PrimaryYellowSoft,
            contentColor = CharcoalBlack,
          )
        }
      }
    }

    if (summaries.isEmpty()) {
      item {
        StoreEmptyStateCard(
          icon = Icons.Filled.PhoneAndroid,
          title = "لا توجد هواتف أو عمليات كاش مسجلة بعد",
          description = "أضف إيميلات هواتف «الإيمان كاش» بالأسفل أو انتظر وصول أول عملية تأكيد تحويل.",
        )
      }
    } else {
      items(summaries, key = { it.deviceKey }) { summary ->
        val isExpanded = expandedDeviceKey == summary.deviceKey
        CashDeviceTrackingCard(
          summary = summary,
          isExpanded = isExpanded,
          canSettleCashDevice = canSettleCashDevice,
          isSubmitting = uiState.isSubmitting,
          dateTimeFmt = dateTimeFmt,
          onToggleExpand = {
            expandedDeviceKey = if (isExpanded) null else summary.deviceKey
          },
          onSettleClick = { settlingDevice = summary },
        )
      }
    }

    // 3. إدارة إيميلات تطبيق الإيمان كاش الإضافية
    if (canManageCashAccounts) {
      item {
        AlEmanCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showAccountsManager = !showAccountsManager },
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(PrimaryYellowSoft, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(20.dp))
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "إيميلات هواتف «الإيمان كاش» الإضافية (${uiState.accounts.size})",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = CharcoalBlack,
                )
                Text(
                  text = "أضف إيميل لكل هاتف مع اسمه ليظهر تلقائيًا على كل طلب يتم تأكيده",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              }
              Icon(
                imageVector = if (showAccountsManager) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = CharcoalBlack,
              )
            }

            AnimatedVisibility(
              visible = showAccountsManager,
              enter = expandVertically(),
              exit = shrinkVertically(),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = BorderNeutral)

                Button(
                  onClick = {
                    editingAccount = null
                    showAccountDialog = true
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryYellow,
                    contentColor = CharcoalBlack,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("add_cash_verifier_email_button"),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("إضافة إيميل واسم هاتف جديد للإيمان كاش", fontWeight = FontWeight.Bold)
                }

                if (uiState.accounts.isEmpty()) {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GeneralBackground,
                    border = BorderStroke(1.dp, BorderNeutral),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text(
                      text = "لم تتم إضافة إيميلات إضافية بعد. الإيميلات المضافة هنا تسجّل دخولها على تطبيق «الإيمان كاش» فقط لتأكيد التحويلات باسم الهاتف المحدد.",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                      modifier = Modifier.padding(12.dp),
                    )
                  }
                } else {
                  uiState.accounts.forEach { acc ->
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = GeneralBackground,
                      border = BorderStroke(1.dp, if (acc.isActive) BorderNeutral else DiscountRed.copy(alpha = 0.35f)),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          Icon(
                            Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = if (acc.isActive) SuccessGreen else TextSecondary,
                            modifier = Modifier.size(20.dp),
                          )
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = acc.deviceName,
                              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                              color = CharcoalBlack,
                            )
                            Text(
                              text = acc.email,
                              style = MaterialTheme.typography.bodySmall,
                              color = TextSecondary,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                            if (!acc.phoneNumber.isNullOrBlank()) {
                              Text(
                                text = "رقم المحفظة: ${acc.phoneNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBlue,
                              )
                            }
                          }
                          Switch(
                            checked = acc.isActive,
                            onCheckedChange = { onToggleCashAccount(acc) },
                            colors = SwitchDefaults.colors(
                              checkedThumbColor = CharcoalBlack,
                              checkedTrackColor = PrimaryYellow,
                            ),
                          )
                        }

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.End,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          TextButton(
                            onClick = {
                              editingAccount = acc
                              showAccountDialog = true
                            },
                          ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعديل", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                          }
                          TextButton(onClick = { deletingAccount = acc }) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف", color = DiscountRed, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 4. سجل عمليات السحب السابقة
    item {
      AlEmanCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showWithdrawalHistory = !showWithdrawalHistory },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(GeneralBackground, RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Filled.History, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "سجل عمليات السحب وتصفير الحسابات (${uiState.filteredWithdrawals.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
              Text(
                text = "سجل موثّق بكل عملية سحب وتصفير ومن قام بها",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            }
            Icon(
              imageVector = if (showWithdrawalHistory) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
              contentDescription = null,
              tint = CharcoalBlack,
            )
          }

          AnimatedVisibility(
            visible = showWithdrawalHistory,
            enter = expandVertically(),
            exit = shrinkVertically(),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              HorizontalDivider(color = BorderNeutral)
              if (uiState.filteredWithdrawals.isEmpty()) {
                Text(
                  text = "لا توجد عمليات سحب مسجلة في هذه الفترة.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              } else {
                uiState.filteredWithdrawals.forEach { w ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GeneralBackground,
                    border = BorderStroke(1.dp, BorderNeutral),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(
                      modifier = Modifier.padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Text(
                          text = w.deviceName,
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                          color = CharcoalBlack,
                          modifier = Modifier.weight(1f),
                        )
                        Text(
                          text = ArabicFormatter.formatCurrency(w.withdrawnAmount),
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                          color = SuccessGreen,
                        )
                      }
                      Text(
                        text = "${w.operationsCount} عملية • تم السحب بواسطة: ${w.withdrawnByName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                      )
                      w.createdAt?.toDate()?.let { dt ->
                        Text(
                          text = dateTimeFmt.format(dt),
                          style = MaterialTheme.typography.labelSmall,
                          color = TextSecondary,
                        )
                      }
                      if (!w.note.isNullOrBlank()) {
                        Text(
                          text = "ملاحظة: ${w.note}",
                          style = MaterialTheme.typography.labelSmall,
                          color = RoyalBlue,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // حوار التقويم لاختيار يوم محدد أو بداية/نهاية النطاق
  val activePicker = datePickerTarget
  if (activePicker != null) {
    val initialLocal = when (activePicker) {
      ActiveDatePickerTarget.SINGLE_DAY -> uiState.selectedDayStartMillis
      ActiveDatePickerTarget.RANGE_START -> uiState.rangeStartMillis
      ActiveDatePickerTarget.RANGE_END -> uiState.rangeEndMillis
    }
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = localDayStartToUtcPickerMillis(initialLocal),
    )
    DatePickerDialog(
      onDismissRequest = { datePickerTarget = null },
      confirmButton = {
        Button(
          onClick = {
            val selectedUtc = pickerState.selectedDateMillis
            if (selectedUtc != null) {
              val localDay = utcDatePickerMillisToLocalDayStart(selectedUtc)
              when (activePicker) {
                ActiveDatePickerTarget.SINGLE_DAY -> onSelectSingleDay(localDay)
                ActiveDatePickerTarget.RANGE_START -> onSelectDateRange(localDay, uiState.rangeEndMillis)
                ActiveDatePickerTarget.RANGE_END -> onSelectDateRange(uiState.rangeStartMillis, localDay)
              }
            }
            datePickerTarget = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryYellow,
            contentColor = CharcoalBlack,
          ),
        ) {
          Text("اختيار", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { datePickerTarget = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  // حوار إضافة أو تعديل إيميل تطبيق الإيمان كاش
  if (showAccountDialog) {
    CashVerifierAccountDialog(
      existing = editingAccount,
      onDismiss = {
        showAccountDialog = false
        editingAccount = null
      },
      onSave = { email, name, phone, active ->
        onSaveCashAccount(email, name, phone, active, editingAccount != null)
        showAccountDialog = false
        editingAccount = null
      },
    )
  }

  // حوار تأكيد حذف إيميل
  val targetDelete = deletingAccount
  if (targetDelete != null) {
    AlertDialog(
      onDismissRequest = { deletingAccount = null },
      containerColor = PureWhite,
      title = {
        Text("حذف إيميل الإيمان كاش", fontWeight = FontWeight.Bold, color = CharcoalBlack)
      },
      text = {
        Text(
          text = "هل تريد حذف «${targetDelete.deviceName}» (${targetDelete.email})؟ لن يتمكن هذا الحساب من تسجيل الدخول في تطبيق الإيمان كاش بعد الحذف، بينما تبقى سجلاته السابقة محفوظة.",
          color = TextPrimary,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCashAccount(targetDelete)
            deletingAccount = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
        ) {
          Text("حذف", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingAccount = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }

  // حوار تأكيد «تم السحب» وتصفير حساب الهاتف
  val targetSettle = settlingDevice
  if (targetSettle != null) {
    SettleCashDeviceDialog(
      summary = targetSettle,
      onDismiss = { settlingDevice = null },
      onConfirm = { note ->
        onSettleDevice(targetSettle, actorDisplayName, note)
        settlingDevice = null
      },
    )
  }
}

@Composable
private fun CashStatTile(
  label: String,
  value: String,
  accent: Color,
  container: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = container,
    border = BorderStroke(1.dp, BorderNeutral),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = accent,
      )
    }
  }
}

@Composable
private fun CashDeviceTrackingCard(
  summary: CashDeviceSummary,
  isExpanded: Boolean,
  canSettleCashDevice: Boolean,
  isSubmitting: Boolean,
  dateTimeFmt: SimpleDateFormat,
  onToggleExpand: () -> Unit,
  onSettleClick: () -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // رأس بطاقة الهاتف
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .background(
              if (summary.currentUnsettledBalance > 0.0) SuccessGreenSoft else GeneralBackground,
              RoundedCornerShape(12.dp),
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.PhoneAndroid,
            contentDescription = null,
            tint = if (summary.currentUnsettledBalance > 0.0) SuccessGreen else CharcoalBlack,
            modifier = Modifier.size(22.dp),
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = summary.deviceName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
          if (summary.email.isNotBlank()) {
            Text(
              text = summary.email,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (summary.phoneNumber.isNotBlank()) {
            Text(
              text = "رقم المحفظة: ${summary.phoneNumber}",
              style = MaterialTheme.typography.labelSmall,
              color = RoyalBlue,
            )
          }
        }
        BrandPillBadge(
          text = if (summary.isActive) "نشط" else "موقوف",
          containerColor = if (summary.isActive) SuccessGreenSoft else GeneralBackground,
          contentColor = if (summary.isActive) SuccessGreen else TextSecondary,
        )
      }

      // الرصيد القائم (المتاح للسحب) + إحصائيات الفترة
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (summary.currentUnsettledBalance > 0.0) SuccessGreenSoft else GeneralBackground,
        border = BorderStroke(
          1.dp,
          if (summary.currentUnsettledBalance > 0.0) SuccessGreen.copy(alpha = 0.4f) else BorderNeutral,
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "الرصيد القائم على الهاتف (غير مسحوب)",
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
            )
            Text(
              text = "${summary.currentUnsettledCount} عملية بانتظار السحب",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
          Text(
            text = ArabicFormatter.formatCurrency(summary.currentUnsettledBalance),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = if (summary.currentUnsettledBalance > 0.0) SuccessGreen else CharcoalBlack,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CashStatTile(
          label = "عمليات الفترة المحددة",
          value = "${summary.periodOperationsCount} عملية",
          accent = CharcoalBlack,
          container = GeneralBackground,
          modifier = Modifier.weight(1f),
        )
        CashStatTile(
          label = "إجمالي الفترة المحددة",
          value = ArabicFormatter.formatCurrency(summary.periodTotalAmount),
          accent = RoyalBlue,
          container = RoyalBlueSoft,
          modifier = Modifier.weight(1f),
        )
      }

      summary.lastWithdrawal?.let { lastW ->
        val dt = lastW.createdAt?.toDate()?.let { dateTimeFmt.format(it) }.orEmpty()
        Text(
          text = "آخر سحب: ${ArabicFormatter.formatCurrency(lastW.withdrawnAmount)} بواسطة ${lastW.withdrawnByName}" +
            (if (dt.isNotBlank()) " • $dt" else ""),
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }

      // زر تم السحب (تصفير حساب هذا الهاتف) — للمالك والمحاسب فقط
      if (canSettleCashDevice) {
        Button(
          onClick = onSettleClick,
          enabled = !isSubmitting && summary.currentUnsettledBalance > 0.0,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = CharcoalBlack,
            contentColor = PrimaryYellow,
            disabledContainerColor = GeneralBackground,
            disabledContentColor = TextSecondary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("settle_cash_device_${summary.deviceKey}"),
        ) {
          Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (summary.currentUnsettledBalance > 0.0) {
              "تم السحب (تصفير حساب الهاتف)"
            } else {
              "الحساب مصفّر حاليًا (0 ج.م)"
            },
            fontWeight = FontWeight.Bold,
          )
        }
      } else {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Text(
              text = "زر «تم السحب» لتصفير الحساب متاح فقط لمالك الحساب أو المحاسب.",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }
      }

      // زر عرض تفاصيل العمليات التي وصلت على هذا الهاتف في الفترة المختارة
      OutlinedButton(
        onClick = onToggleExpand,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderNeutral),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 44.dp),
      ) {
        Text(
          text = if (isExpanded) {
            "إخفاء تفاصيل العمليات (${summary.periodOperationsCount})"
          } else {
            "عرض العمليات على هذا الهاتف (${summary.periodOperationsCount})"
          },
          color = CharcoalBlack,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
          contentDescription = null,
          tint = CharcoalBlack,
          modifier = Modifier.size(18.dp),
        )
      }

      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          HorizontalDivider(color = BorderNeutral)
          if (summary.periodLogs.isEmpty()) {
            Text(
              text = "لا توجد عمليات مؤكدة على هذا الهاتف في الفترة المختارة.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          } else {
            summary.periodLogs.forEach { log ->
              DeviceOperationRow(log = log, dateTimeFmt = dateTimeFmt)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DeviceOperationRow(
  log: CashVerificationLog,
  dateTimeFmt: SimpleDateFormat,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = GeneralBackground,
    border = BorderStroke(1.dp, BorderNeutral),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = if (log.isSettled) Icons.Filled.CheckCircle else Icons.Filled.Verified,
            contentDescription = null,
            tint = if (log.isSettled) TextSecondary else SuccessGreen,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = if (!log.matchedOrderNumber.isNullOrBlank()) {
              "طلب #${log.matchedOrderNumber}"
            } else {
              log.providerName.ifBlank { "تحويل كاش" }
            },
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
        }
        Text(
          text = ArabicFormatter.formatCurrency(log.amount),
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = if (log.isSettled) TextSecondary else SuccessGreen,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = listOfNotNull(
            log.providerName.takeIf { it.isNotBlank() },
            log.senderPhone.takeIf { it.isNotBlank() }?.let { "من: $it" },
          ).joinToString(" • "),
          style = MaterialTheme.typography.bodySmall,
          color = TextPrimary,
          modifier = Modifier.weight(1f),
        )
        BrandPillBadge(
          text = if (log.isSettled) "تم السحب" else "قائم",
          containerColor = if (log.isSettled) PureWhite else SuccessGreenSoft,
          contentColor = if (log.isSettled) TextSecondary else SuccessGreen,
        )
      }

      val dt = log.createdAt?.toDate()?.let { dateTimeFmt.format(it) }.orEmpty()
      val meta = listOfNotNull(
        dt.takeIf { it.isNotBlank() },
        log.transactionId.takeIf { it.isNotBlank() }?.let { "مرجع: $it" },
      ).joinToString(" • ")
      if (meta.isNotBlank()) {
        Text(
          text = meta,
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
    }
  }
}

@Composable
private fun CashVerifierAccountDialog(
  existing: CashVerifierAccount?,
  onDismiss: () -> Unit,
  onSave: (email: String, deviceName: String, phoneNumber: String?, isActive: Boolean) -> Unit,
) {
  var email by remember(existing) { mutableStateOf(existing?.email.orEmpty()) }
  var deviceName by remember(existing) { mutableStateOf(existing?.deviceName.orEmpty()) }
  var phoneNumber by remember(existing) { mutableStateOf(existing?.phoneNumber.orEmpty()) }
  var isActive by remember(existing) { mutableStateOf(existing?.isActive ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة إيميل لتطبيق الإيمان كاش" else "تعديل بيانات هاتف الإيمان كاش",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          enabled = existing == null,
          label = { Text("البريد الإلكتروني (Gmail)") },
          placeholder = { Text("phone1@gmail.com") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cash_account_email_input"),
        )
        OutlinedTextField(
          value = deviceName,
          onValueChange = { deviceName = it },
          label = { Text("اسم الهاتف (سيظهر على الطلبات المؤكدة)") },
          placeholder = { Text("مثال: هاتف فودافون كاش ١") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cash_account_device_name_input"),
        )
        OutlinedTextField(
          value = phoneNumber,
          onValueChange = { phoneNumber = it },
          label = { Text("رقم خط المحفظة على الهاتف (اختياري)") },
          placeholder = { Text("010xxxxxxxx") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("الحساب مفعّل لتسجيل الدخول", style = MaterialTheme.typography.bodyMedium, color = CharcoalBlack)
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
          onSave(email.trim(), deviceName.trim(), phoneNumber.trim().takeIf { it.isNotEmpty() }, isActive)
        },
        enabled = email.trim().contains("@") && deviceName.trim().length >= 2,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
        modifier = Modifier.testTag("cash_account_save_button"),
      ) {
        Text("حفظ", fontWeight = FontWeight.Bold)
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
private fun SettleCashDeviceDialog(
  summary: CashDeviceSummary,
  onDismiss: () -> Unit,
  onConfirm: (String?) -> Unit,
) {
  var note by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "تأكيد السحب وتصفير الحساب",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = PrimaryYellowSoft,
          border = BorderStroke(1.dp, PrimaryYellow),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = summary.deviceName,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
            Text(
              text = "المبلغ الذي سيتم سحبه وتصفيره: ${ArabicFormatter.formatCurrency(summary.currentUnsettledBalance)}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = SuccessGreen,
            )
            Text(
              text = "عدد العمليات المشمولة: ${summary.currentUnsettledCount} عملية",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
        }
        Text(
          text = "سيتم تصفير الرصيد القائم لهذا الهاتف ليصبح 0 ج.م، مع الاحتفاظ بكافة العمليات السابقة داخل التقارير وملف الإكسيل.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("ملاحظة السحب (اختياري)") },
          placeholder = { Text("مثال: تم التسليم نقدًا للخزينة") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(note.trim().takeIf { it.isNotEmpty() }) },
        colors = ButtonDefaults.buttonColors(containerColor = CharcoalBlack, contentColor = PrimaryYellow),
        modifier = Modifier.testTag("confirm_settle_cash_device"),
      ) {
        Text("تأكيد السحب والتصفير", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}
