package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.util.CashDeviceReportRow
import com.example.core.util.CashExcelExporter
import com.example.data.model.CashVerificationLog
import com.example.data.model.CashVerifierAccount
import com.example.data.model.CashWithdrawalRecord
import com.example.data.model.buildCashAccountDocId
import com.example.data.repository.CashTrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class CashDateFilterMode(val arabicLabel: String) {
  ALL("الكل"),
  SINGLE_DAY("يوم محدد"),
  DATE_RANGE("من تاريخ إلى تاريخ"),
}

data class CashDeviceSummary(
  val deviceKey: String,
  val deviceName: String,
  val email: String,
  val phoneNumber: String,
  val isActive: Boolean,
  val registeredAccountId: String?,
  val periodLogs: List<CashVerificationLog>,
  val unsettledLogs: List<CashVerificationLog>,
  val totalWithdrawnAmount: Double,
  val lastWithdrawal: CashWithdrawalRecord?,
) {
  val periodOperationsCount: Int get() = periodLogs.size
  val periodTotalAmount: Double get() = periodLogs.sumOf { it.amount }
  val currentUnsettledCount: Int get() = unsettledLogs.size
  val currentUnsettledBalance: Double get() = unsettledLogs.sumOf { it.amount }
}

data class AdminCashTrackingUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val isExporting: Boolean = false,
  val filterMode: CashDateFilterMode = CashDateFilterMode.ALL,
  val selectedDayStartMillis: Long = startOfDayMillis(System.currentTimeMillis()),
  val rangeStartMillis: Long = startOfDayMillis(System.currentTimeMillis() - 6L * 86_400_000L),
  val rangeEndMillis: Long = startOfDayMillis(System.currentTimeMillis()),
  val accounts: List<CashVerifierAccount> = emptyList(),
  val allLogs: List<CashVerificationLog> = emptyList(),
  val withdrawals: List<CashWithdrawalRecord> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val verifiedLogsAll: List<CashVerificationLog>
    get() = allLogs.filter { it.isVerifiedOperation }

  val filteredVerifiedLogs: List<CashVerificationLog>
    get() = verifiedLogsAll.filter { log -> matchesDateFilter(log.createdAt?.toDate()?.time) }

  val filteredAllLogs: List<CashVerificationLog>
    get() = allLogs.filter { log -> matchesDateFilter(log.createdAt?.toDate()?.time) }

  val filteredWithdrawals: List<CashWithdrawalRecord>
    get() = withdrawals.filter { w -> matchesDateFilter(w.createdAt?.toDate()?.time) }

  val periodTitle: String
    get() {
      val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
      return when (filterMode) {
        CashDateFilterMode.ALL -> "جميع الفترات"
        CashDateFilterMode.SINGLE_DAY -> "يوم ${fmt.format(Date(selectedDayStartMillis))}"
        CashDateFilterMode.DATE_RANGE -> "من ${fmt.format(Date(rangeStartMillis))} إلى ${fmt.format(Date(rangeEndMillis))}"
      }
    }

  val periodSlug: String
    get() {
      val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
      return when (filterMode) {
        CashDateFilterMode.ALL -> "all"
        CashDateFilterMode.SINGLE_DAY -> fmt.format(Date(selectedDayStartMillis))
        CashDateFilterMode.DATE_RANGE -> "${fmt.format(Date(rangeStartMillis))}_to_${fmt.format(Date(rangeEndMillis))}"
      }
    }

  val deviceSummaries: List<CashDeviceSummary>
    get() {
      val map = linkedMapOf<String, MutableDeviceBucket>()

      // 1. Seed all registered Cash verifier accounts first so even 0-operation phones appear
      accounts.forEach { acc ->
        val key = acc.id.ifBlank { buildCashAccountDocId(acc.email) }
        map[key] = MutableDeviceBucket(
          deviceKey = key,
          deviceName = acc.deviceName.ifBlank { acc.email },
          email = acc.email.trim().lowercase(Locale.ROOT),
          phoneNumber = acc.phoneNumber.orEmpty(),
          isActive = acc.isActive,
          registeredAccountId = acc.id.takeIf { it.isNotBlank() },
        )
      }

      fun resolveBucketKey(email: String, deviceName: String): String {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        if (cleanEmail.isNotBlank()) {
          val byEmailKey = buildCashAccountDocId(cleanEmail)
          if (map.containsKey(byEmailKey)) return byEmailKey
          return byEmailKey
        }
        val cleanName = deviceName.trim()
        if (cleanName.isNotBlank()) {
          val existingByName = map.entries.firstOrNull { it.value.deviceName == cleanName }?.key
          if (existingByName != null) return existingByName
          return "name_${cleanName.hashCode().toUInt().toString(36)}"
        }
        return "admin_default"
      }

      fun ensureBucket(key: String, email: String, deviceName: String): MutableDeviceBucket {
        return map.getOrPut(key) {
          val fallbackName = when {
            deviceName.isNotBlank() -> deviceName.trim()
            email.isNotBlank() -> email.trim()
            else -> "هاتف الإدارة"
          }
          MutableDeviceBucket(
            deviceKey = key,
            deviceName = fallbackName,
            email = email.trim().lowercase(Locale.ROOT),
            phoneNumber = "",
            isActive = true,
            registeredAccountId = null,
          )
        }
      }

      // 2. Assign all verified logs (for current unsettled balance)
      verifiedLogsAll.forEach { log ->
        val key = resolveBucketKey(log.verifiedByEmail, log.verifiedByDeviceName)
        val bucket = ensureBucket(key, log.verifiedByEmail, log.verifiedByDeviceName)
        if (!log.isSettled) {
          bucket.unsettledLogs += log
        }
        if (matchesDateFilter(log.createdAt?.toDate()?.time)) {
          bucket.periodLogs += log
        }
      }

      // 3. Assign withdrawal records
      withdrawals.forEach { w ->
        val key = when {
          w.deviceKey.isNotBlank() && map.containsKey(w.deviceKey) -> w.deviceKey
          w.deviceEmail.isNotBlank() -> resolveBucketKey(w.deviceEmail, w.deviceName)
          w.deviceKey.isNotBlank() -> w.deviceKey
          else -> resolveBucketKey("", w.deviceName)
        }
        val bucket = ensureBucket(key, w.deviceEmail, w.deviceName)
        bucket.withdrawals += w
      }

      return map.values
        .map { b ->
          val sortedWithdrawals = b.withdrawals.sortedByDescending { it.createdAt?.seconds ?: 0L }
          CashDeviceSummary(
            deviceKey = b.deviceKey,
            deviceName = b.deviceName,
            email = b.email,
            phoneNumber = b.phoneNumber,
            isActive = b.isActive,
            registeredAccountId = b.registeredAccountId,
            periodLogs = b.periodLogs.sortedByDescending { it.createdAt?.seconds ?: 0L },
            unsettledLogs = b.unsettledLogs.sortedByDescending { it.createdAt?.seconds ?: 0L },
            totalWithdrawnAmount = sortedWithdrawals.sumOf { it.withdrawnAmount },
            lastWithdrawal = sortedWithdrawals.firstOrNull(),
          )
        }
        .sortedWith(
          compareByDescending<CashDeviceSummary> { it.currentUnsettledBalance }
            .thenByDescending { it.periodTotalAmount }
            .thenBy { it.deviceName },
        )
    }

  val totalPeriodAmount: Double
    get() = filteredVerifiedLogs.sumOf { it.amount }

  val totalPeriodOperations: Int
    get() = filteredVerifiedLogs.size

  val totalUnsettledBalance: Double
    get() = verifiedLogsAll.filter { !it.isSettled }.sumOf { it.amount }

  private fun matchesDateFilter(timestampMillis: Long?): Boolean {
    if (filterMode == CashDateFilterMode.ALL) return true
    val ts = timestampMillis ?: return false
    return when (filterMode) {
      CashDateFilterMode.ALL -> true
      CashDateFilterMode.SINGLE_DAY -> {
        val dayStart = startOfDayMillis(selectedDayStartMillis)
        val dayEnd = endOfDayMillis(selectedDayStartMillis)
        ts in dayStart..dayEnd
      }
      CashDateFilterMode.DATE_RANGE -> {
        val start = startOfDayMillis(minOf(rangeStartMillis, rangeEndMillis))
        val end = endOfDayMillis(maxOf(rangeStartMillis, rangeEndMillis))
        ts in start..end
      }
    }
  }
}

private class MutableDeviceBucket(
  val deviceKey: String,
  val deviceName: String,
  val email: String,
  val phoneNumber: String,
  val isActive: Boolean,
  val registeredAccountId: String?,
) {
  val periodLogs = mutableListOf<CashVerificationLog>()
  val unsettledLogs = mutableListOf<CashVerificationLog>()
  val withdrawals = mutableListOf<CashWithdrawalRecord>()
}

fun startOfDayMillis(millis: Long): Long {
  val cal = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  return cal.timeInMillis
}

fun endOfDayMillis(millis: Long): Long {
  val cal = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
  }
  return cal.timeInMillis
}

/**
 * يحوّل قيمة Material3 DatePicker (التي تكون بتوقيت UTC عند منتصف الليل) إلى بداية نفس اليوم بالتوقيت المحلي.
 */
fun utcDatePickerMillisToLocalDayStart(utcMillis: Long): Long {
  val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    timeInMillis = utcMillis
  }
  val localCal = Calendar.getInstance().apply {
    set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
    set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
    set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  return localCal.timeInMillis
}

fun localDayStartToUtcPickerMillis(localMillis: Long): Long {
  val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
  val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    clear()
    set(Calendar.YEAR, localCal.get(Calendar.YEAR))
    set(Calendar.MONTH, localCal.get(Calendar.MONTH))
    set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
  }
  return utcCal.timeInMillis
}

class AdminCashTrackingViewModel(
  private val repository: CashTrackingRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminCashTrackingUiState())
  val uiState: StateFlow<AdminCashTrackingUiState> = _uiState.asStateFlow()

  init {
    observeData()
  }

  private fun observeData() {
    viewModelScope.launch {
      combine(
        repository.observeCashVerifierAccounts().catch { emit(emptyList()) },
        repository.observeAllCashLogs().catch { emit(emptyList()) },
        repository.observeCashWithdrawals().catch { emit(emptyList()) },
      ) { accounts, logs, withdrawals ->
        Triple(accounts, logs, withdrawals)
      }
        .catch { err ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحميل بيانات متابعة تحويلات الكاش."),
            )
          }
        }
        .collect { (accounts, logs, withdrawals) ->
          _uiState.update {
            it.copy(
              isLoading = false,
              accounts = accounts,
              allLogs = logs,
              withdrawals = withdrawals,
            )
          }
        }
    }
  }

  fun selectFilterMode(mode: CashDateFilterMode) {
    _uiState.update { it.copy(filterMode = mode) }
  }

  fun setSingleDay(localDayStartMillis: Long) {
    _uiState.update {
      it.copy(
        filterMode = CashDateFilterMode.SINGLE_DAY,
        selectedDayStartMillis = startOfDayMillis(localDayStartMillis),
      )
    }
  }

  fun setDateRange(startLocalMillis: Long, endLocalMillis: Long) {
    val s = startOfDayMillis(minOf(startLocalMillis, endLocalMillis))
    val e = startOfDayMillis(maxOf(startLocalMillis, endLocalMillis))
    _uiState.update {
      it.copy(
        filterMode = CashDateFilterMode.DATE_RANGE,
        rangeStartMillis = s,
        rangeEndMillis = e,
      )
    }
  }

  fun saveCashVerifierAccount(
    email: String,
    deviceName: String,
    phoneNumber: String?,
    isActive: Boolean,
    isEditExisting: Boolean,
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.saveCashVerifierAccount(
        email = email,
        deviceName = deviceName,
        phoneNumber = phoneNumber,
        isActive = isActive,
        isEditExisting = isEditExisting,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (isEditExisting) {
                "تم تحديث بيانات هاتف «${deviceName.trim()}» بنجاح."
              } else {
                "تمت إضافة الإيميل لتطبيق الإيمان كاش باسم «${deviceName.trim()}»."
              },
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ حساب الإيمان كاش."),
            )
          },
        )
      }
    }
  }

  fun toggleAccountActive(account: CashVerifierAccount) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.toggleCashVerifierAccountActive(account.id, !account.isActive)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (!account.isActive) {
                "تم تفعيل صلاحية «${account.deviceName}»."
              } else {
                "تم إيقاف صلاحية «${account.deviceName}» مؤقتًا."
              },
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تعديل حالة الحساب."),
            )
          },
        )
      }
    }
  }

  fun deleteAccount(account: CashVerifierAccount) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.deleteCashVerifierAccount(account.id)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم حذف الإيميل «${account.email}» من قائمة الإيمان كاش.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف الحساب."),
            )
          },
        )
      }
    }
  }

  fun settleAndResetDevice(
    summary: CashDeviceSummary,
    actorName: String,
    note: String?,
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.recordWithdrawalAndResetDevice(
        deviceKey = summary.deviceKey,
        deviceName = summary.deviceName,
        deviceEmail = summary.email,
        registeredAccountId = summary.registeredAccountId,
        unsettledLogs = summary.unsettledLogs,
        actorName = actorName,
        note = note,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = { amount ->
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم تسجيل السحب وتصفير حساب «${summary.deviceName}» بقيمة ${String.format(Locale.US, "%.2f", amount)} ج.م بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تصفير الحساب — هذه الصلاحية متاحة لمالك الحساب أو المحاسب فقط."),
            )
          },
        )
      }
    }
  }

  fun exportExcelToUri(context: Context, uri: Uri) {
    val snapshot = _uiState.value
    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true, errorMessage = null, feedbackMessage = null) }
      val rows = buildReportRows(snapshot)
      val result = withContext(Dispatchers.IO) {
        CashExcelExporter.exportToUri(
          context = context.applicationContext,
          uri = uri,
          periodTitle = snapshot.periodTitle,
          deviceSummaries = rows,
          logs = snapshot.filteredAllLogs,
          withdrawals = snapshot.filteredWithdrawals,
        )
      }
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isExporting = false,
              feedbackMessage = "تم حفظ ملف الإكسيل (.xlsx) بنجاح بكامل تفاصيل الكاش.",
            )
          },
          onFailure = { err ->
            state.copy(
              isExporting = false,
              errorMessage = err.message ?: "تعذر حفظ ملف الإكسيل.",
            )
          },
        )
      }
    }
  }

  fun exportExcelForShare(context: Context, onReadyIntent: (Intent) -> Unit) {
    val snapshot = _uiState.value
    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true, errorMessage = null, feedbackMessage = null) }
      val rows = buildReportRows(snapshot)
      val result = withContext(Dispatchers.IO) {
        CashExcelExporter.exportAndCreateShareIntent(
          context = context.applicationContext,
          periodTitle = snapshot.periodTitle,
          periodSlug = snapshot.periodSlug,
          deviceSummaries = rows,
          logs = snapshot.filteredAllLogs,
          withdrawals = snapshot.filteredWithdrawals,
        )
      }
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isExporting = false,
              feedbackMessage = "تم تجهيز ملف الإكسيل للمشاركة والفتح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isExporting = false,
              errorMessage = err.message ?: "تعذر تجهيز ملف الإكسيل.",
            )
          },
        )
      }
      result.onSuccess { intent -> onReadyIntent(intent) }
    }
  }

  private fun buildReportRows(state: AdminCashTrackingUiState): List<CashDeviceReportRow> {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return state.deviceSummaries.map { d ->
      val lastW = d.lastWithdrawal?.let { w ->
        val dt = w.createdAt?.toDate()?.let { fmt.format(it) } ?: ""
        "${String.format(Locale.US, "%.0f", w.withdrawnAmount)} ج.م ($dt)"
      }.orEmpty()
      CashDeviceReportRow(
        deviceName = d.deviceName,
        email = d.email,
        phoneNumber = d.phoneNumber,
        periodOperationsCount = d.periodOperationsCount,
        periodTotalAmount = d.periodTotalAmount,
        currentUnsettledCount = d.currentUnsettledCount,
        currentUnsettledBalance = d.currentUnsettledBalance,
        totalWithdrawnAmount = d.totalWithdrawnAmount,
        lastWithdrawalLabel = lastW,
      )
    }
  }

  fun clearMessages() {
    _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
  }

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AdminCashTrackingViewModel(
        repository = CashTrackingRepository(context.applicationContext),
      ) as T
    }
  }
}
