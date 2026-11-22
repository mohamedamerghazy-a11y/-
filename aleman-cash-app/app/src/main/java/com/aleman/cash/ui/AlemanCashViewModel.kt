package com.aleman.cash.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aleman.cash.data.CashProviderSettingsStore
import com.aleman.cash.data.CashVerificationRepository
import com.aleman.cash.data.LocalVerificationFeed
import com.aleman.cash.model.CashVerificationLogItem
import com.aleman.cash.model.CashVerifierSettings
import com.aleman.cash.model.LocalVerificationEvent
import com.aleman.cash.model.PendingTransferOrder
import com.aleman.cash.model.StaffAuthState
import com.aleman.cash.sms.SmsInboxScanner
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlemanCashViewModel(application: Application) : AndroidViewModel(application) {

  private val store = CashProviderSettingsStore(application)
  private val repository = CashVerificationRepository(application)

  val settings: StateFlow<CashVerifierSettings> = store.settingsFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = CashVerifierSettings()
  )

  val localFeed: StateFlow<List<LocalVerificationEvent>> = LocalVerificationFeed.events

  private val _authState = MutableStateFlow(StaffAuthState.Loading)
  val authState: StateFlow<StaffAuthState> = _authState.asStateFlow()

  private val _staffLabel = MutableStateFlow("")
  val staffLabel: StateFlow<String> = _staffLabel.asStateFlow()

  private val _pendingOrders = MutableStateFlow<List<PendingTransferOrder>>(emptyList())
  val pendingOrders: StateFlow<List<PendingTransferOrder>> = _pendingOrders.asStateFlow()

  private val _cloudLogs = MutableStateFlow<List<CashVerificationLogItem>>(emptyList())
  val cloudLogs: StateFlow<List<CashVerificationLogItem>> = _cloudLogs.asStateFlow()

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private var pendingOrdersListener: ListenerRegistration? = null
  private var logsListener: ListenerRegistration? = null

  init {
    refreshAuthStatus()
  }

  fun clearBanner() {
    _statusBanner.value = null
  }

  fun refreshAuthStatus() {
    viewModelScope.launch {
      _authState.value = StaffAuthState.Loading
      val (state, label) = withContext(Dispatchers.IO) {
        repository.checkStaffAuthorization()
      }
      _authState.value = state
      _staffLabel.value = label
      if (state == StaffAuthState.Authorized) {
        attachFirestoreListeners()
      } else {
        detachFirestoreListeners()
        _pendingOrders.value = emptyList()
        _cloudLogs.value = emptyList()
      }
    }
  }

  fun signInWithGoogle(activity: Activity) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.signInWithGoogle(activity)
      _isBusy.value = false
      res.onSuccess {
        refreshAuthStatus()
      }.onFailure { err ->
        _statusBanner.value = err.message ?: "تعذر تسجيل الدخول بحساب جوجل"
      }
    }
  }

  fun signOut() {
    detachFirestoreListeners()
    repository.signOut()
    _pendingOrders.value = emptyList()
    _cloudLogs.value = emptyList()
    _authState.value = StaffAuthState.SignedOut
    _staffLabel.value = ""
  }

  fun scanRecentDeviceInbox(context: Context) {
    viewModelScope.launch {
      _isBusy.value = true
      val summary = withContext(Dispatchers.IO) {
        val currentSettings = store.getCurrentSettings()
        val smsList = SmsInboxScanner.readRecentInboxMessages(context, limit = 25)
        if (smsList.isEmpty()) {
          "لا توجد رسائل SMS واردة حديثة في صندوق الوارد."
        } else {
          var processed = 0
          for (item in smsList) {
            val resultMsg = repository.processIncomingSms(
              senderAddress = item.senderAddress,
              rawBody = item.body,
              settings = currentSettings,
              notifyUser = false
            )
            if (!resultMsg.contains("تم تجاهل") && !resultMsg.contains("تم تخطيها")) {
              processed++
            }
          }
          "تم فحص ${smsList.size} رسالة من الهاتف ومطابقة/معالجة $processed عملية دفع."
        }
      }
      _isBusy.value = false
      _statusBanner.value = summary
    }
  }

  fun verifyOrderManually(order: PendingTransferOrder) {
    viewModelScope.launch {
      _isBusy.value = true
      val targetStage = settings.value.targetOrderStatusAfterPayment
      val result = withContext(Dispatchers.IO) {
        repository.verifyOrderManually(order, targetStage)
      }
      _isBusy.value = false
      result.onSuccess { msg ->
        _statusBanner.value = msg
      }.onFailure { err ->
        _statusBanner.value = err.message ?: "تعذر تأكيد الطلب"
      }
    }
  }

  fun toggleProvider(providerId: String, enabled: Boolean) {
    viewModelScope.launch { store.toggleProviderEnabled(providerId, enabled) }
  }

  fun saveProvider(displayName: String, aliases: String, isEnabled: Boolean, editingId: String?) {
    viewModelScope.launch {
      store.upsertProvider(displayName, aliases, isEnabled, editingId)
      _statusBanner.value = "تم حفظ إعدادات «$displayName»."
    }
  }

  fun deleteProvider(providerId: String) {
    viewModelScope.launch {
      store.deleteProvider(providerId)
      _statusBanner.value = "تم حذف المرسل المخصص."
    }
  }

  fun setAutoVerifyEnabled(enabled: Boolean) {
    viewModelScope.launch { store.setAutoVerifyEnabled(enabled) }
  }

  fun setTargetOrderStatus(status: String) {
    viewModelScope.launch { store.setTargetOrderStatus(status) }
  }

  private fun attachFirestoreListeners() {
    detachFirestoreListeners()
    pendingOrdersListener = repository.observePendingTransferOrders(
      onUpdate = { _pendingOrders.value = it },
      onError = { err -> _statusBanner.value = err.message }
    )
    logsListener = repository.observeCashVerificationLogs(
      onUpdate = { _cloudLogs.value = it },
      onError = { err -> _statusBanner.value = err.message }
    )
  }

  private fun detachFirestoreListeners() {
    pendingOrdersListener?.remove()
    pendingOrdersListener = null
    logsListener?.remove()
    logsListener = null
  }

  override fun onCleared() {
    super.onCleared()
    detachFirestoreListeners()
  }
}
