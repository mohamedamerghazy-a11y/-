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

  @Volatile private var activeDeviceName: String = ""
  @Volatile private var activeUserEmail: String = ""

  private fun buildCashAccountDocId(email: String): String =
    email.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_-]"), "_")

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
    activeUserEmail = email

    if (email.isNotBlank()) {
      val cashAccountDoc = runCatching {
        firestore.collection("cashVerifierAccounts")
          .document(buildCashAccountDocId(email))
          .get()
          .await()
      }.getOrNull()
      if (cashAccountDoc != null && cashAccountDoc.exists() && cashAccountDoc.getBoolean("isActive") == true) {
        val deviceName = cashAccountDoc.getString("deviceName")?.trim()?.takeIf { it.isNotBlank() } ?: email
        activeDeviceName = deviceName
        return CashPreviewAuthState.Authorized to "هاتف الإيمان كاش: $deviceName ($email)"
      }
    }

    val storeDoc = runCatching {
      firestore.collection("settings").document("store").get().await()
    }.getOrNull()

    val configuredOwnerEmail = storeDoc?.getString("ownerEmail")
      ?.trim()?.lowercase(Locale.ROOT)
      ?.takeIf { it.isNotBlank() }
      ?: defaultOwnerEmail

    if (email.isNotBlank() && (email == defaultOwnerEmail || email == configuredOwnerEmail)) {
      activeDeviceName = "هاتف المالك"
      return CashPreviewAuthState.Authorized to "مالك المتجر ($email)"
    }

    val adminDoc = runCatching {
      firestore.collection("admins").document(user.uid).get().await()
    }.getOrNull()
    if (adminDoc != null && adminDoc.exists()) {
      activeDeviceName = "هاتف الإدارة"
      return CashPreviewAuthState.Authorized to "مدير المتجر (${email.ifBlank { user.uid }})"
    }

    val staffDoc = runCatching {
      firestore.collection("staff").document(user.uid).get().await()
    }.getOrNull()

    if (staffDoc != null && staffDoc.exists()) {
      val isActive = staffDoc.getBoolean("isActive") == true
      @Suppress("UNCHECKED_CAST")
      val permissions = (staffDoc.get("permissions") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
      val role = staffDoc.getString("role").orEmpty()
      val canVerify = role in setOf("owner", "manager") ||
        ("manage_payments" in permissions && ("manage_orders" in permissions || "manage_delivery" in permissions))
      if (isActive && canVerify) {
        val display = staffDoc.getString("fullName")?.takeIf { it.isNotBlank() } ?: email.ifBlank { user.uid }
        activeDeviceName = "هاتف $display"
        return CashPreviewAuthState.Authorized to "موظف مخوّل ($display)"
      }
    }

    activeDeviceName = ""
    return CashPreviewAuthState.Unauthorized to (email.ifBlank { user.uid })
  }

  private fun pushLocalEvent(event: LocalVerificationEventPreview) {
    if (event.status.startsWith("ignored")) return
    _localFeed.update { current -> (listOf(event) + current).take(60) }
  }

  private suspend fun processSimulatedSms(
    senderAddress: String,
    rawBody: String,
    currentSettings: CashPreviewSettings,
  ): String? {
    val result = CashPreviewSmsParser.parseIncomingPaymentSms(
      senderAddress = senderAddress,
      rawBody = rawBody,
      providers = currentSettings.providers,
    )

    return when (result) {
      is CashPreviewSmsParseResult.IgnoredUnknownOrDisabledSender -> null
      is CashPreviewSmsParseResult.IgnoredNotIncomingPayment -> null
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
    val deviceName = activeDeviceName.ifBlank { "هاتف الإدارة" }.take(80)
    val verifierEmail = activeUserEmail.ifBlank { auth.currentUser?.email?.trim()?.lowercase(Locale.ROOT).orEmpty() }.take(120)
    val noteText = "تم التأكيد على هاتف: $deviceName • ${parsed.provider.displayName} • عملية #${parsed.transactionId} • مبلغ ${formatAmount(parsed.amount)} ج.م".take(300)

    val orderRef = firestore.collection("orders").document(matchedOrder.orderId)
    val logRef = firestore.collection("cashVerificationLogs").document()
    val batch = firestore.batch()
    val orderUpdate = linkedMapOf<String, Any>(
      "paymentStatus" to "verified",
      "orderStatus" to targetStage,
      "statusNote" to noteText,
      "verifiedByDeviceName" to deviceName,
      "verifiedAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (verifierEmail.isNotBlank()) {
      orderUpdate["verifiedByEmail"] = verifierEmail
    }
    batch.update(orderRef, orderUpdate)
    if (matchedOrder.userId.isNotBlank()) {
      val notifRef = firestore.collection("users")
        .document(matchedOrder.userId)
        .collection("notifications")
        .document()
      batch.set(
        notifRef,
        mapOf(
          "userId" to matchedOrder.userId,
          "title" to "تم تأكيد الدفع لطلبك #${matchedOrder.orderNumber}".take(120),
          "message" to "تم التحقق من تحويل ${formatAmount(parsed.amount)} ج.م وتحويل الطلب تلقائياً إلى مرحلة $targetLabel.".take(500),
          "type" to "payment_status",
          "referenceId" to matchedOrder.orderId.take(128),
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    val logPayload = linkedMapOf<String, Any>(
      "transactionId" to parsed.transactionId.take(80),
      "providerName" to parsed.provider.displayName.take(80),
      "senderAddress" to parsed.rawSenderAddress.take(80),
      "senderPhone" to parsed.senderPhone.take(40),
      "amount" to parsed.amount,
      "matchedOrderId" to matchedOrder.orderId.take(80),
      "matchedOrderNumber" to matchedOrder.orderNumber.take(40),
      "status" to "verified",
      "reason" to noteText,
      "rawBodyPreview" to parsed.rawBody.take(160),
      "verifiedByDeviceName" to deviceName,
      "isSettled" to false,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    if (verifierEmail.isNotBlank()) {
      logPayload["verifiedByEmail"] = verifierEmail
    }
    batch.set(logRef, logPayload)
    batch.commit().await()
    notifyOrderPreparationStaff(
      orderId = matchedOrder.orderId,
      orderNumber = matchedOrder.orderNumber,
      customerName = matchedOrder.customerName,
      totalAmount = matchedOrder.totalAmount,
      confirmationSummary = "تأكيد آلي عبر هاتف «$deviceName»",
    )

    val successMsg = "تم تأكيد دفع الطلب #${matchedOrder.orderNumber} على هاتف «$deviceName» ونقله إلى «$targetLabel» وإشعار مسؤولي التجهيز."
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
    val deviceName = activeDeviceName.ifBlank { "هاتف الإدارة" }.take(80)
    val verifierEmail = activeUserEmail.ifBlank { auth.currentUser?.email?.trim()?.lowercase(Locale.ROOT).orEmpty() }.take(120)
    val noteText = "تم التأكيد يدوياً على هاتف: $deviceName (${order.effectiveSenderPhone.ifBlank { "تحويل إلكتروني" }})".take(300)

    val orderRef = firestore.collection("orders").document(order.orderId)
    val logRef = firestore.collection("cashVerificationLogs").document()
    val batch = firestore.batch()
    val orderUpdate = linkedMapOf<String, Any>(
      "paymentStatus" to "verified",
      "orderStatus" to normalizedStage,
      "statusNote" to noteText,
      "verifiedByDeviceName" to deviceName,
      "verifiedAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (verifierEmail.isNotBlank()) {
      orderUpdate["verifiedByEmail"] = verifierEmail
    }
    batch.update(orderRef, orderUpdate)
    if (order.userId.isNotBlank()) {
      val notifRef = firestore.collection("users")
        .document(order.userId)
        .collection("notifications")
        .document()
      batch.set(
        notifRef,
        mapOf(
          "userId" to order.userId,
          "title" to "تم تأكيد الدفع لطلبك #${order.orderNumber}".take(120),
          "message" to "تم تأكيد الدفع ونقل طلبك إلى مرحلة $targetLabel.".take(500),
          "type" to "payment_status",
          "referenceId" to order.orderId.take(128),
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    val logMap = linkedMapOf<String, Any>(
      "providerName" to "تأكيد يدوي",
      "senderAddress" to "Manual",
      "amount" to order.totalAmount.coerceAtLeast(0.0),
      "transactionId" to "MANUAL-${order.orderNumber}-${System.currentTimeMillis()}".take(80),
      "matchedOrderId" to order.orderId.take(80),
      "matchedOrderNumber" to order.orderNumber.take(40),
      "status" to "verified",
      "reason" to noteText,
      "rawBodyPreview" to noteText,
      "verifiedByDeviceName" to deviceName,
      "isSettled" to false,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    if (verifierEmail.isNotBlank()) {
      logMap["verifiedByEmail"] = verifierEmail
    }
    val normalizedSender = CashPreviewSmsParser.normalizeEgyptianPhone(order.effectiveSenderPhone)
    if (normalizedSender.isNotBlank()) {
      logMap["senderPhone"] = normalizedSender.take(40)
    }
    batch.set(logRef, logMap)
    batch.commit().await()
    notifyOrderPreparationStaff(
      orderId = order.orderId,
      orderNumber = order.orderNumber,
      customerName = order.customerName,
      totalAmount = order.totalAmount,
      confirmationSummary = "تأكيد يدوي عبر هاتف «$deviceName»",
    )
    "تم تأكيد الطلب #${order.orderNumber} على هاتف «$deviceName» ونقله إلى «$targetLabel» وإشعار مسؤولي التجهيز."
  }

  private suspend fun notifyOrderPreparationStaff(
    orderId: String,
    orderNumber: String,
    customerName: String,
    totalAmount: Double,
    confirmationSummary: String,
  ) {
    if (orderId.isBlank()) return
    val targetUids = LinkedHashSet<String>()
    val prepRoles = setOf("owner", "manager", "order_manager", "customer_service")

    runCatching {
      val staffSnap = firestore.collection("staff").get().await()
      staffSnap.documents.forEach { doc ->
        if (doc.getBoolean("isActive") != true) return@forEach
        val role = doc.getString("role")?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val permissions = (doc.get("permissions") as? List<*>)
          ?.mapNotNull { it as? String }
          ?.toSet()
          .orEmpty()
        if (role in prepRoles || "manage_orders" in permissions) {
          val uid = doc.getString("uid")?.trim().takeUnless { it.isNullOrBlank() } ?: doc.id
          if (uid.isNotBlank()) targetUids.add(uid)
        }
      }
    }

    runCatching {
      val ownersSnap = firestore.collection("users")
        .whereEqualTo("role", "owner")
        .get()
        .await()
      ownersSnap.documents.forEach { doc ->
        val uid = doc.getString("uid")?.trim().takeUnless { it.isNullOrBlank() } ?: doc.id
        if (uid.isNotBlank()) targetUids.add(uid)
      }
    }

    if (targetUids.isEmpty()) return

    val cleanCustomer = customerName.trim().ifEmpty { "عميل المتجر" }
    val title = "طلب جديد جاهز للتجهيز #$orderNumber".take(120)
    val message = "تم تأكيد الدفع ($confirmationSummary) • العميل: $cleanCustomer • الإجمالي: ${formatAmount(totalAmount)} ج.م — يرجى البدء في التجهيز.".take(500)
    val cleanRef = orderId.take(128)

    runCatching {
      targetUids.toList().chunked(400).forEach { chunk ->
        val notifBatch = firestore.batch()
        chunk.forEach { uid ->
          val docRef = firestore.collection("users")
            .document(uid)
            .collection("notifications")
            .document()
          notifBatch.set(
            docRef,
            mapOf(
              "userId" to uid,
              "title" to title,
              "message" to message,
              "type" to "order_status",
              "referenceId" to cleanRef,
              "isRead" to false,
              "createdAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        notifBatch.commit().await()
      }
    }
  }

  private suspend fun writeCloudLog(
    parsed: ParsedCashPreviewSms,
    status: String,
    matchedOrderId: String?,
    matchedOrderNumber: String?,
    reason: String,
    uid: String,
  ) {
    val deviceName = activeDeviceName.ifBlank { "هاتف الإدارة" }.take(80)
    val verifierEmail = activeUserEmail.ifBlank { auth.currentUser?.email?.trim()?.lowercase(Locale.ROOT).orEmpty() }.take(120)
    runCatching {
      val map = linkedMapOf<String, Any>(
        "transactionId" to parsed.transactionId.take(80),
        "providerName" to parsed.provider.displayName.take(80),
        "senderAddress" to parsed.rawSenderAddress.take(80),
        "senderPhone" to parsed.senderPhone.take(40),
        "amount" to parsed.amount,
        "status" to status,
        "reason" to reason.take(300),
        "rawBodyPreview" to parsed.rawBody.take(160),
        "verifiedByDeviceName" to deviceName,
        "isSettled" to false,
        "createdAt" to FieldValue.serverTimestamp(),
      )
      if (!matchedOrderId.isNullOrBlank()) map["matchedOrderId"] = matchedOrderId.take(80)
      if (!matchedOrderNumber.isNullOrBlank()) map["matchedOrderNumber"] = matchedOrderNumber.take(40)
      if (verifierEmail.isNotBlank()) map["verifiedByEmail"] = verifierEmail
      firestore.collection("cashVerificationLogs").add(map).await()
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
          val status = doc.getString("status").orEmpty()
          if (status.startsWith("ignored")) return@mapNotNull null
          CashVerificationLogPreview(
            id = doc.id,
            transactionId = doc.getString("transactionId").orEmpty(),
            providerName = doc.getString("providerName").orEmpty(),
            senderAddress = doc.getString("senderAddress").orEmpty(),
            senderPhone = doc.getString("senderPhone").orEmpty(),
            amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
            matchedOrderId = doc.getString("matchedOrderId"),
            matchedOrderNumber = doc.getString("matchedOrderNumber"),
            status = status,
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
      customerPhone = doc.getString("phone") ?: doc.getString("customerPhone").orEmpty(),
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
