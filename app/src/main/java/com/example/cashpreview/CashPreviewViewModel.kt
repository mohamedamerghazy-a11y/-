package com.example.cashpreview

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.cashPreviewDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "aleman_cash_preview_settings",
)

class CashPreviewViewModel(application: Application) : AndroidViewModel(application) {

  private val appContext = application.applicationContext
  private val auth = Firebase.auth
  private val firestore = FirebaseFirestore.getInstance(
    appContext.getString(R.string.firestore_database_id),
  )

  private val defaultOwnerEmail = "mohamedamerghazy@gmail.com"
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }
  private val settingsKey = stringPreferencesKey("cash_preview_settings_json")

  private val settingsFlow: Flow<CashPreviewSettings> = appContext.cashPreviewDataStore.data.map { prefs ->
    val raw = prefs[settingsKey]
    if (raw.isNullOrBlank()) {
      CashPreviewSettings()
    } else {
      runCatching {
        val decoded = json.decodeFromString<CashPreviewSettings>(raw)
        if (decoded.providers.isEmpty()) decoded.copy(providers = defaultCashPreviewProviders()) else decoded
      }.getOrElse { CashPreviewSettings() }
    }
  }

  val settings: StateFlow<CashPreviewSettings> = settingsFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = CashPreviewSettings(),
  )

  private val _authState = MutableStateFlow(CashPreviewAuthState.Loading)
  val authState: StateFlow<CashPreviewAuthState> = _authState.asStateFlow()

  private val _staffLabel = MutableStateFlow("")
  val staffLabel: StateFlow<String> = _staffLabel.asStateFlow()

  private val _pendingOrders = MutableStateFlow<List<PendingTransferOrderPreview>>(emptyList())
  val pendingOrders: StateFlow<List<PendingTransferOrderPreview>> = _pendingOrders.asStateFlow()

  private val _cloudLogs = MutableStateFlow<List<CashVerificationLogPreview>>(emptyList())
  val cloudLogs: StateFlow<List<CashVerificationLogPreview>> = _cloudLogs.asStateFlow()

  private val _localFeed = MutableStateFlow<List<LocalVerificationEventPreview>>(emptyList())
  val localFeed: StateFlow<List<LocalVerificationEventPreview>> = _localFeed.asStateFlow()

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

  fun showMessage(msg: String) {
    _statusBanner.value = msg
  }

  fun refreshAuthStatus() {
    viewModelScope.launch {
      _authState.value = CashPreviewAuthState.Loading
      val (state, label) = withContext(Dispatchers.IO) { checkStaffAuthorization() }
      _authState.value = state
      _staffLabel.value = label
      if (state == CashPreviewAuthState.Authorized) {
        attachFirestoreListeners()
      } else {
        detachFirestoreListeners()
        _pendingOrders.value = emptyList()
        _cloudLogs.value = emptyList()
      }
    }
  }

  fun signOut() {
    detachFirestoreListeners()
    auth.signOut()
    _pendingOrders.value = emptyList()
    _cloudLogs.value = emptyList()
    _authState.value = CashPreviewAuthState.SignedOut
    _staffLabel.value = ""
  }

  fun verifyOrderManually(order: PendingTransferOrderPreview) {
    viewModelScope.launch {
      _isBusy.value = true
      val targetStage = settings.value.targetOrderStatusAfterPayment
      val result = withContext(Dispatchers.IO) {
        verifyOrderManuallyInFirestore(order, targetStage)
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
    viewModelScope.launch {
      updateSettings { current ->
        current.copy(
          providers = current.providers.map {
            if (it.id == providerId) it.copy(isEnabled = enabled) else it
          },
        )
      }
    }
  }

  fun saveProvider(displayName: String, aliasesInput: String, isEnabled: Boolean, editingId: String?) {
    viewModelScope.launch {
      val cleanName = displayName.trim()
      if (cleanName.isBlank()) return@launch
      val parsedAliases = aliasesInput
        .split(",", "،", "\n", "|")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf(cleanName) }
        .distinct()

      updateSettings { current ->
        val existing = editingId?.let { id -> current.providers.find { it.id == id } }
        val updatedList = if (existing != null) {
          current.providers.map {
            if (it.id == existing.id) {
              it.copy(displayName = cleanName, senderAliases = parsedAliases, isEnabled = isEnabled)
            } else {
              it
            }
          }
        } else {
          current.providers + CashPreviewProviderConfig(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            displayName = cleanName,
            senderAliases = parsedAliases,
            isEnabled = isEnabled,
            isBuiltIn = false,
          )
        }
        current.copy(providers = updatedList)
      }
      _statusBanner.value = "تم حفظ إعدادات «$cleanName»."
    }
  }

  fun deleteProvider(providerId: String) {
    viewModelScope.launch {
      updateSettings { current ->
        current.copy(providers = current.providers.filterNot { it.id == providerId })
      }
      _statusBanner.value = "تم حذف المرسل المخصص."
    }
  }

  fun setAutoVerifyEnabled(enabled: Boolean) {
    viewModelScope.launch {
      updateSettings { it.copy(autoVerifyEnabled = enabled) }
    }
  }

  fun setTargetOrderStatus(status: String) {
    viewModelScope.launch {
      val normalized = if (status == "confirmed") "confirmed" else "preparing"
      updateSettings { it.copy(targetOrderStatusAfterPayment = normalized) }
    }
  }

  private suspend fun updateSettings(transform: (CashPreviewSettings) -> CashPreviewSettings) {
    appContext.cashPreviewDataStore.edit { prefs ->
      val current = prefs[settingsKey]?.let { raw ->
        runCatching { json.decodeFromString<CashPreviewSettings>(raw) }.getOrNull()
      } ?: CashPreviewSettings()
      prefs[settingsKey] = json.encodeToString(transform(current))
    }
  }

  private suspend fun checkStaffAuthorization(): Pair<CashPreviewAuthState, String> {
    val user = auth.currentUser ?: return CashPreviewAuthState.SignedOut to ""
    val email = user.email?.trim()?.lowercase(Locale.ROOT).orEmpty()

    val storeDoc = runCatching {
      firestore.collection("settings").document("store").get().await()
    }.getOrNull()

    val configuredOwnerEmail = storeDoc?.getString("ownerEmail")
      ?.trim()?.lowercase(Locale.ROOT)
      ?.takeIf { it.isNotBlank() }
      ?: defaultOwnerEmail

    if (email.isNotBlank() && email == configuredOwnerEmail) {
      return CashPreviewAuthState.Authorized to "مالك المتجر ($email)"
    }

    val staffDoc = runCatching {
      firestore.collection("staffUsers").document(user.uid).get().await()
    }.getOrNull()

    if (staffDoc != null && staffDoc.exists()) {
      val isActive = staffDoc.getBoolean("isActive") == true
      @Suppress("UNCHECKED_CAST")
      val permissions = (staffDoc.get("permissions") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
      val role = staffDoc.getString("role").orEmpty()
      val canVerify = role == "owner" ||
        ("manage_payments" in permissions && "manage_orders" in permissions)
      if (isActive && canVerify) {
        val display = staffDoc.getString("name")?.takeIf { it.isNotBlank() } ?: email.ifBlank { user.uid }
        return CashPreviewAuthState.Authorized to "موظف مخوّل ($display)"
      }
    }

    return CashPreviewAuthState.Unauthorized to (email.ifBlank { user.uid })
  }

  private fun pushLocalEvent(event: LocalVerificationEventPreview) {
    _localFeed.update { current -> (listOf(event) + current).take(60) }
  }

  private suspend fun processSimulatedSms(
    senderAddress: String,
    rawBody: String,
    currentSettings: CashPreviewSettings,
  ): String {
    val result = CashPreviewSmsParser.parseIncomingPaymentSms(
      senderAddress = senderAddress,
      rawBody = rawBody,
      providers = currentSettings.providers,
    )

    return when (result) {
      is CashPreviewSmsParseResult.IgnoredUnknownOrDisabledSender -> {
        val msg = "تم تجاهل الرسالة لأن المرسل «$senderAddress» غير مسجل أو غير مفعّل في الإعدادات."
        pushLocalEvent(
          LocalVerificationEventPreview(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            senderAddress = senderAddress,
            providerName = "غير مفعّل",
            amount = null,
            senderPhone = null,
            transactionId = null,
            status = "ignored_sender",
            message = msg,
          ),
        )
        msg
      }

      is CashPreviewSmsParseResult.IgnoredNotIncomingPayment -> {
        val msg = "رسالة من ${result.provider.displayName} تم تخطيها: ${result.reason}"
        pushLocalEvent(
          LocalVerificationEventPreview(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            senderAddress = senderAddress,
            providerName = result.provider.displayName,
            amount = null,
            senderPhone = null,
            transactionId = null,
            status = "ignored_content",
            message = msg,
          ),
        )
        msg
      }

      is CashPreviewSmsParseResult.Matched -> {
        val parsed = result.parsed
        if (_authState.value != CashPreviewAuthState.Authorized || auth.currentUser == null) {
          val msg = "تم تحليل رسالة ${parsed.provider.displayName} بنجاح: مبلغ ${formatAmount(parsed.amount)} ج.م من رقم ${parsed.senderPhone} (عملية #${parsed.transactionId}). سجّل الدخول بحساب الإدارة لمطابقتها تلقائيًا مع طلبات المتجر."
          pushLocalEvent(
            LocalVerificationEventPreview(
              id = UUID.randomUUID().toString(),
              timestampMillis = System.currentTimeMillis(),
              senderAddress = parsed.rawSenderAddress,
              providerName = parsed.provider.displayName,
              amount = parsed.amount,
              senderPhone = parsed.senderPhone,
              transactionId = parsed.transactionId,
              status = "matched",
              message = msg,
            ),
          )
          return msg
        }
        verifyParsedPaymentInFirestore(parsed, currentSettings)
      }
    }
  }

  private suspend fun verifyParsedPaymentInFirestore(
    parsed: ParsedCashPreviewSms,
    currentSettings: CashPreviewSettings,
  ): String {
    val uid = auth.currentUser?.uid ?: return "يرجى تسجيل الدخول بحساب الإدارة أولاً."

    val existingDup = runCatching {
      firestore.collection("cashVerificationLogs")
        .whereEqualTo("transactionId", parsed.transactionId)
        .whereEqualTo("status", "matched")
        .limit(1)
        .get()
        .await()
    }.getOrNull()

    if (existingDup != null && !existingDup.isEmpty) {
      val msg = "عملية مكررة (#${parsed.transactionId}): تم تأكيد هذه الحوالة مسبقاً."
      writeCloudLog(parsed, "duplicate", null, null, msg, uid)
      pushLocalEvent(
        LocalVerificationEventPreview(
          id = UUID.randomUUID().toString(),
          timestampMillis = System.currentTimeMillis(),
          senderAddress = parsed.rawSenderAddress,
          providerName = parsed.provider.displayName,
          amount = parsed.amount,
          senderPhone = parsed.senderPhone,
          transactionId = parsed.transactionId,
          status = "duplicate",
          message = msg,
        ),
      )
      return msg
    }

    val pendingSnapshot = firestore.collection("orders")
      .whereEqualTo("paymentStatus", "pending_review")
      .get()
      .await()

    val pending = pendingSnapshot.documents
      .mapNotNull(::mapOrderDocument)
      .filter { it.paymentMethod != "cod" && it.orderStatus !in setOf("cancelled", "returned") }

    val matchingPhoneOrders = pending.filter { order ->
      CashPreviewSmsParser.normalizeEgyptianPhone(order.effectiveSenderPhone) == parsed.senderPhone
    }

    val matchedOrder = matchingPhoneOrders
      .filter { order -> parsed.amount + 0.001 >= order.totalAmount }
      .sortedWith(
        compareBy<PendingTransferOrderPreview> { parsed.amount - it.totalAmount }
          .thenBy { it.createdAtMillis },
      )
      .firstOrNull()

    if (matchedOrder == null) {
      val smallerMatch = matchingPhoneOrders.firstOrNull()
      val reason = if (smallerMatch != null) {
        "المبلغ المستلم (${formatAmount(parsed.amount)} ج.م) من ${parsed.senderPhone} أقل من إجمالي الطلب #${smallerMatch.orderNumber} (${formatAmount(smallerMatch.totalAmount)} ج.م)."
      } else {
        "لم يتم العثور على طلب بانتظار المراجعة برقم المحوَّل منه ${parsed.senderPhone} وقيمة <= ${formatAmount(parsed.amount)} ج.م."
      }
      writeCloudLog(parsed, "unmatched", null, null, reason, uid)
      pushLocalEvent(
        LocalVerificationEventPreview(
          id = UUID.randomUUID().toString(),
          timestampMillis = System.currentTimeMillis(),
          senderAddress = parsed.rawSenderAddress,
          providerName = parsed.provider.displayName,
          amount = parsed.amount,
          senderPhone = parsed.senderPhone,
          transactionId = parsed.transactionId,
          status = "unmatched",
          message = reason,
        ),
      )
      return reason
    }

    val targetStage = if (currentSettings.targetOrderStatusAfterPayment == "confirmed") "confirmed" else "preparing"
    val targetLabel = if (targetStage == "confirmed") "تم التأكيد" else "جاري التجهيز"
    val noteText = "تم تأكيد الدفع آليًا عبر الإيمان كاش (${parsed.provider.displayName} • من ${parsed.senderPhone} • عملية #${parsed.transactionId} • مبلغ ${formatAmount(parsed.amount)} ج.م)"

    val orderRef = firestore.collection("orders").document(matchedOrder.orderId)
    val logRef = firestore.collection("cashVerificationLogs").document()
    val batch = firestore.batch()
    batch.update(
      orderRef,
      mapOf(
        "paymentStatus" to "verified",
        "orderStatus" to targetStage,
        "statusNote" to noteText,
        "senderPhone" to parsed.senderPhone,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    if (matchedOrder.userId.isNotBlank()) {
      val notifRef = firestore.collection("users")
        .document(matchedOrder.userId)
        .collection("notifications")
        .document()
      batch.set(
        notifRef,
        mapOf(
          "title" to "تم تأكيد الدفع لطلبك #${matchedOrder.orderNumber}",
          "body" to "تم التحقق من تحويل ${formatAmount(parsed.amount)} ج.م وتحويل الطلب تلقائياً إلى مرحلة $targetLabel.",
          "type" to "payment_status",
          "orderId" to matchedOrder.orderId,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    batch.set(
      logRef,
      mapOf(
        "transactionId" to parsed.transactionId,
        "providerName" to parsed.provider.displayName,
        "senderAddress" to parsed.rawSenderAddress,
        "senderPhone" to parsed.senderPhone,
        "amount" to parsed.amount,
        "matchedOrderId" to matchedOrder.orderId,
        "matchedOrderNumber" to matchedOrder.orderNumber,
        "status" to "matched",
        "reason" to noteText,
        "rawBodyPreview" to parsed.rawBody.take(160),
        "verifiedByUid" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.commit().await()

    val successMsg = "تم تأكيد دفع الطلب #${matchedOrder.orderNumber} (${formatAmount(matchedOrder.totalAmount)} ج.م) بمبلغ ${formatAmount(parsed.amount)} ج.م ونقله إلى «$targetLabel»."
    pushLocalEvent(
      LocalVerificationEventPreview(
        id = UUID.randomUUID().toString(),
        timestampMillis = System.currentTimeMillis(),
        senderAddress = parsed.rawSenderAddress,
        providerName = parsed.provider.displayName,
        amount = parsed.amount,
        senderPhone = parsed.senderPhone,
        transactionId = parsed.transactionId,
        status = "matched",
        message = successMsg,
      ),
    )
    return successMsg
  }

  private suspend fun verifyOrderManuallyInFirestore(
    order: PendingTransferOrderPreview,
    targetStage: String,
  ): Result<String> = runCatching {
    val uid = auth.currentUser?.uid ?: error("يجب تسجيل الدخول بحساب الإدارة أولاً")
    val normalizedStage = if (targetStage == "confirmed") "confirmed" else "preparing"
    val targetLabel = if (normalizedStage == "confirmed") "تم التأكيد" else "جاري التجهيز"
    val noteText = "تم تأكيد الدفع يدوياً من تطبيق الإيمان كاش (${order.effectiveSenderPhone.ifBlank { "تحويل إلكتروني" }})"

    val orderRef = firestore.collection("orders").document(order.orderId)
    val logRef = firestore.collection("cashVerificationLogs").document()
    val batch = firestore.batch()
    batch.update(
      orderRef,
      mapOf(
        "paymentStatus" to "verified",
        "orderStatus" to normalizedStage,
        "statusNote" to noteText,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    if (order.userId.isNotBlank()) {
      val notifRef = firestore.collection("users")
        .document(order.userId)
        .collection("notifications")
        .document()
      batch.set(
        notifRef,
        mapOf(
          "title" to "تم تأكيد الدفع لطلبك #${order.orderNumber}",
          "body" to "تم تأكيد الدفع ونقل طلبك إلى مرحلة $targetLabel.",
          "type" to "payment_status",
          "orderId" to order.orderId,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    batch.set(
      logRef,
      mapOf(
        "transactionId" to "MANUAL-${order.orderNumber}-${System.currentTimeMillis()}",
        "providerName" to "تأكيد يدوي",
        "senderAddress" to "Manual",
        "senderPhone" to CashPreviewSmsParser.normalizeEgyptianPhone(order.effectiveSenderPhone),
        "amount" to order.totalAmount,
        "matchedOrderId" to order.orderId,
        "matchedOrderNumber" to order.orderNumber,
        "status" to "matched",
        "reason" to noteText,
        "rawBodyPreview" to noteText,
        "verifiedByUid" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.commit().await()
    "تم تأكيد الطلب #${order.orderNumber} ونقله إلى «$targetLabel»."
  }

  private suspend fun writeCloudLog(
    parsed: ParsedCashPreviewSms,
    status: String,
    matchedOrderId: String?,
    matchedOrderNumber: String?,
    reason: String,
    uid: String,
  ) {
    runCatching {
      firestore.collection("cashVerificationLogs").add(
        mapOf(
          "transactionId" to parsed.transactionId,
          "providerName" to parsed.provider.displayName,
          "senderAddress" to parsed.rawSenderAddress,
          "senderPhone" to parsed.senderPhone,
          "amount" to parsed.amount,
          "matchedOrderId" to matchedOrderId,
          "matchedOrderNumber" to matchedOrderNumber,
          "status" to status,
          "reason" to reason,
          "rawBodyPreview" to parsed.rawBody.take(160),
          "verifiedByUid" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    }
  }

  private fun attachFirestoreListeners() {
    detachFirestoreListeners()
    pendingOrdersListener = firestore.collection("orders")
      .whereEqualTo("paymentStatus", "pending_review")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          _statusBanner.value = error.localizedMessage
          return@addSnapshotListener
        }
        val list = snapshot?.documents
          .orEmpty()
          .mapNotNull(::mapOrderDocument)
          .filter { it.paymentMethod != "cod" && it.orderStatus !in setOf("cancelled", "returned") }
          .sortedBy { it.createdAtMillis }
        _pendingOrders.value = list
      }

    logsListener = firestore.collection("cashVerificationLogs")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(40)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          _statusBanner.value = error.localizedMessage
          return@addSnapshotListener
        }
        val logs = snapshot?.documents.orEmpty().mapNotNull { doc ->
          CashVerificationLogPreview(
            id = doc.id,
            transactionId = doc.getString("transactionId").orEmpty(),
            providerName = doc.getString("providerName").orEmpty(),
            senderAddress = doc.getString("senderAddress").orEmpty(),
            senderPhone = doc.getString("senderPhone").orEmpty(),
            amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
            matchedOrderId = doc.getString("matchedOrderId"),
            matchedOrderNumber = doc.getString("matchedOrderNumber"),
            status = doc.getString("status").orEmpty(),
            reason = doc.getString("reason").orEmpty(),
            rawBodyPreview = doc.getString("rawBodyPreview").orEmpty(),
            createdAtMillis = (doc.getTimestamp("createdAt")?.toDate()?.time)
              ?: (doc.getLong("createdAtMillis") ?: System.currentTimeMillis()),
          )
        }
        _cloudLogs.value = logs
      }
  }

  private fun detachFirestoreListeners() {
    pendingOrdersListener?.remove()
    pendingOrdersListener = null
    logsListener?.remove()
    logsListener = null
  }

  private fun mapOrderDocument(doc: DocumentSnapshot): PendingTransferOrderPreview? {
    if (!doc.exists()) return null
    val orderNumber = doc.getString("orderNumber")?.takeIf { it.isNotBlank() } ?: doc.id.take(8)
    val totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0
    val createdAt = (doc.getTimestamp("createdAt")?.toDate()?.time)
      ?: (doc.getLong("createdAtMillis") ?: 0L)

    return PendingTransferOrderPreview(
      orderId = doc.id,
      orderNumber = orderNumber,
      userId = doc.getString("userId").orEmpty(),
      customerName = doc.getString("customerName").orEmpty(),
      customerPhone = doc.getString("customerPhone").orEmpty(),
      paymentMethod = doc.getString("paymentMethod").orEmpty(),
      paymentStatus = doc.getString("paymentStatus").orEmpty(),
      orderStatus = doc.getString("orderStatus").orEmpty(),
      senderPhone = doc.getString("senderPhone").orEmpty(),
      transferReference = doc.getString("transferReference").orEmpty(),
      totalAmount = totalAmount,
      createdAtMillis = createdAt,
    )
  }

  private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) {
      String.format(Locale.US, "%.0f", value)
    } else {
      String.format(Locale.US, "%.2f", value)
    }
  }

  override fun onCleared() {
    super.onCleared()
    detachFirestoreListeners()
  }
}
