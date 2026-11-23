package com.aleman.cash.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.aleman.cash.R
import com.aleman.cash.model.CashVerificationLogItem
import com.aleman.cash.model.CashVerifierSettings
import com.aleman.cash.model.LocalVerificationEvent
import com.aleman.cash.model.ParsedCashSms
import com.aleman.cash.model.PendingTransferOrder
import com.aleman.cash.model.StaffAuthState
import com.aleman.cash.sms.SmsCashParser
import com.aleman.cash.sms.SmsParseResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val AUTH_TAG = "AlemanCashAuth"

object LocalVerificationFeed {
  private val _events = MutableStateFlow<List<LocalVerificationEvent>>(emptyList())
  val events: StateFlow<List<LocalVerificationEvent>> = _events.asStateFlow()

  fun push(event: LocalVerificationEvent) {
    if (event.status.startsWith("ignored")) return
    _events.update { current -> (listOf(event) + current).take(60) }
  }
}

sealed interface CashSignInOutcome {
  data class Success(val user: FirebaseUser) : CashSignInOutcome
  data object Cancelled : CashSignInOutcome
  data class Error(val message: String) : CashSignInOutcome
}

class CashVerificationRepository(context: Context) {

  private val appContext = context.applicationContext
  private val auth = FirebaseInitializer.auth(appContext)
  private val firestore = FirebaseInitializer.firestore(appContext)
  private val credentialManager = CredentialManager.create(appContext)

  private val defaultOwnerEmail = "mohamedamerghazy@gmail.com"

  @Volatile
  var activeDeviceName: String = ""
    private set

  @Volatile
  var activeUserEmail: String = ""
    private set

  private fun buildCashAccountDocId(email: String): String =
    email.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_-]"), "_")

  suspend fun signInWithGoogle(activity: Activity): CashSignInOutcome {
    val clientId = try {
      appContext.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Missing default_web_client_id resource", e)
      return CashSignInOutcome.Error("تعذر العثور على إعدادات تسجيل الدخول بحساب Google.")
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    return try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = auth.signInWithCredential(firebaseCredential).awaitResult()
        val user = authResult.user
        if (user != null) {
          CashSignInOutcome.Success(user)
        } else {
          CashSignInOutcome.Error("تعذر استكمال تسجيل الدخول بحساب Google.")
        }
      } else {
        CashSignInOutcome.Error("نوع بيانات الاعتماد المستلم غير مدعوم.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
      CashSignInOutcome.Cancelled
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Google Sign-In failed", e)
      CashSignInOutcome.Error(e.localizedMessage ?: "تعذر تسجيل الدخول بحساب Google، يرجى المحاولة مجدداً.")
    }
  }

  fun currentUser(): FirebaseUser? = auth.currentUser

  suspend fun signOut() {
    activeDeviceName = ""
    activeUserEmail = ""
    withContext(NonCancellable) {
      runCatching {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      }
      auth.signOut()
    }
  }

  private suspend fun ensureResolvedDeviceIdentity(): Pair<String, String> {
    if (activeDeviceName.isBlank()) {
      runCatching { checkStaffAuthorization() }
    }
    val email = activeUserEmail.ifBlank {
      auth.currentUser?.email?.trim()?.lowercase(Locale.ROOT).orEmpty()
    }
    val device = activeDeviceName.ifBlank {
      email.ifBlank { "هاتف الإدارة" }
    }
    return device.take(80) to email.take(120)
  }

  suspend fun checkStaffAuthorization(): Pair<StaffAuthState, String> {
    val user = auth.currentUser ?: run {
      activeDeviceName = ""
      activeUserEmail = ""
      return StaffAuthState.SignedOut to ""
    }
    val email = user.email?.trim()?.lowercase(Locale.ROOT).orEmpty()
    activeUserEmail = email

    // 1. Check dedicated Al-Eman Cash authorized emails first so custom device names always take precedence
    if (email.isNotBlank()) {
      val cashAccountDoc = runCatching {
        firestore.collection("cashVerifierAccounts")
          .document(buildCashAccountDocId(email))
          .get()
          .awaitResult()
      }.getOrNull()
      if (cashAccountDoc != null && cashAccountDoc.exists() && cashAccountDoc.getBoolean("isActive") == true) {
        val customDeviceName = cashAccountDoc.getString("deviceName")
          ?.trim()
          ?.takeIf { it.isNotBlank() }
          ?: email
        activeDeviceName = customDeviceName
        return StaffAuthState.Authorized to "$customDeviceName ($email)"
      }
    }

    val storeDoc = runCatching {
      firestore.collection("settings").document("store").get().awaitResult()
    }.getOrNull()

    val configuredOwnerEmail = storeDoc?.getString("ownerEmail")
      ?.trim()?.lowercase(Locale.ROOT)
      ?.takeIf { it.isNotBlank() }
      ?: defaultOwnerEmail

    if (email.isNotBlank() && (email == defaultOwnerEmail || email == configuredOwnerEmail)) {
      activeDeviceName = "هاتف المالك"
      return StaffAuthState.Authorized to "مالك المتجر ($email)"
    }

    val adminDoc = runCatching {
      firestore.collection("admins").document(user.uid).get().awaitResult()
    }.getOrNull()
    if (adminDoc != null && adminDoc.exists()) {
      activeDeviceName = "هاتف الإدارة"
      return StaffAuthState.Authorized to "مدير المتجر (${email.ifBlank { user.uid }})"
    }

    val staffDoc = runCatching {
      firestore.collection("staff").document(user.uid).get().awaitResult()
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
        return StaffAuthState.Authorized to "موظف مخوّل ($display)"
      }
    }

    activeDeviceName = ""
    return StaffAuthState.Unauthorized to (email.ifBlank { user.uid })
  }

  fun observePendingTransferOrders(
    onUpdate: (List<PendingTransferOrder>) -> Unit,
    onError: (Throwable) -> Unit
  ): ListenerRegistration {
    return firestore.collection("orders")
      .whereEqualTo("paymentStatus", "pending_review")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          onError(error)
          return@addSnapshotListener
        }
        val list = snapshot?.documents
          .orEmpty()
          .mapNotNull(::mapOrderDocument)
          .filter {
            it.paymentMethod != "cod" &&
              it.orderStatus !in setOf("cancelled", "returned")
          }
          .sortedBy { it.createdAtMillis }
        onUpdate(list)
      }
  }

  fun observeCashVerificationLogs(
    onUpdate: (List<CashVerificationLogItem>) -> Unit,
    onError: (Throwable) -> Unit
  ): ListenerRegistration {
    return firestore.collection("cashVerificationLogs")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(40)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          onError(error)
          return@addSnapshotListener
        }
        val logs = snapshot?.documents.orEmpty().mapNotNull { doc ->
          val status = doc.getString("status").orEmpty()
          if (status.startsWith("ignored")) return@mapNotNull null
          CashVerificationLogItem(
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
            verifiedByDeviceName = doc.getString("verifiedByDeviceName").orEmpty(),
            verifiedByEmail = doc.getString("verifiedByEmail").orEmpty(),
            isSettled = doc.getBoolean("isSettled") == true
          )
        }
        onUpdate(logs)
      }
  }

  /**
   * يعالج رسائل الدفع الواردة من المحافظ المفعّلة فقط.
   * يعيد null بصمت لأي رسالة متخطاة أو غير تابعة للمحافظ المطلوبة دون إظهارها في السجل أو الإشعارات.
   */
  suspend fun processIncomingSms(
    senderAddress: String,
    rawBody: String,
    settings: CashVerifierSettings,
    notifyUser: Boolean = true
  ): String? {
    val result = SmsCashParser.parseIncomingPaymentSms(
      senderAddress = senderAddress,
      rawBody = rawBody,
      providers = settings.providers
    )

    return when (result) {
      is SmsParseResult.IgnoredUnknownOrDisabledSender -> null
      is SmsParseResult.IgnoredNotIncomingPayment -> null
      is SmsParseResult.Matched -> {
        verifyParsedPayment(result.parsed, settings, notifyUser)
      }
    }
  }

  suspend fun verifyParsedPayment(
    parsed: ParsedCashSms,
    settings: CashVerifierSettings,
    notifyUser: Boolean = true
  ): String {
    val uid = auth.currentUser?.uid
    if (uid.isNullOrBlank()) {
      val msg = "وصل تحويل بقيمة ${formatAmount(parsed.amount)} ج.م من ${parsed.senderPhone} — يرجى تسجيل الدخول بحساب الإدارة أولاً."
      LocalVerificationFeed.push(
        LocalVerificationEvent(
          id = UUID.randomUUID().toString(),
          timestampMillis = System.currentTimeMillis(),
          senderAddress = parsed.rawSenderAddress,
          providerName = parsed.provider.displayName,
          amount = parsed.amount,
          senderPhone = parsed.senderPhone,
          transactionId = parsed.transactionId,
          status = "auth_required",
          message = msg
        )
      )
      return msg
    }

    // 1. Check deduplication by transactionId
    val existingDup = runCatching {
      firestore.collection("cashVerificationLogs")
        .whereEqualTo("transactionId", parsed.transactionId)
        .limit(5)
        .get()
        .awaitResult()
    }.getOrNull()

    val alreadyVerified = existingDup?.documents?.any {
      val st = it.getString("status")
      st == "verified" || st == "matched"
    } == true

    if (alreadyVerified) {
      val msg = "عملية مكررة (#${parsed.transactionId}): تم تأكيد هذه الحوالة مسبقاً."
      writeCloudLog(
        parsed = parsed,
        status = "duplicate",
        matchedOrderId = null,
        matchedOrderNumber = null,
        reason = msg
      )
      LocalVerificationFeed.push(
        LocalVerificationEvent(
          id = UUID.randomUUID().toString(),
          timestampMillis = System.currentTimeMillis(),
          senderAddress = parsed.rawSenderAddress,
          providerName = parsed.provider.displayName,
          amount = parsed.amount,
          senderPhone = parsed.senderPhone,
          transactionId = parsed.transactionId,
          status = "duplicate",
          message = msg
        )
      )
      return msg
    }

    // 2. Fetch pending_review transfer orders
    val pendingSnapshot = firestore.collection("orders")
      .whereEqualTo("paymentStatus", "pending_review")
      .get()
      .awaitResult()

    val pendingOrders = pendingSnapshot.documents
      .mapNotNull(::mapOrderDocument)
      .filter {
        it.paymentMethod != "cod" &&
          it.orderStatus !in setOf("cancelled", "returned")
      }

    val matchingPhoneOrders = pendingOrders.filter { order ->
      SmsCashParser.normalizeEgyptianPhone(order.effectiveSenderPhone) == parsed.senderPhone
    }

    // Match amount equal OR greater (supports customer rounding fractions upwards)
    val eligibleOrders = matchingPhoneOrders
      .filter { order -> parsed.amount + 0.001 >= order.totalAmount }
      .sortedWith(
        compareBy<PendingTransferOrder> { parsed.amount - it.totalAmount }
          .thenBy { it.createdAtMillis }
      )

    val matchedOrder = eligibleOrders.firstOrNull()
    if (matchedOrder == null) {
      val smallerMatch = matchingPhoneOrders.firstOrNull()
      val reason = if (smallerMatch != null) {
        "المبلغ المستلم (${formatAmount(parsed.amount)} ج.م) من ${parsed.senderPhone} أقل من إجمالي الطلب #${smallerMatch.orderNumber} (${formatAmount(smallerMatch.totalAmount)} ج.م)."
      } else {
        "لم يتم العثور على طلب بانتظار المراجعة برقم المحوَّل منه ${parsed.senderPhone} وقيمة <= ${formatAmount(parsed.amount)} ج.م."
      }
      writeCloudLog(
        parsed = parsed,
        status = "unmatched",
        matchedOrderId = null,
        matchedOrderNumber = null,
        reason = reason
      )
      LocalVerificationFeed.push(
        LocalVerificationEvent(
          id = UUID.randomUUID().toString(),
          timestampMillis = System.currentTimeMillis(),
          senderAddress = parsed.rawSenderAddress,
          providerName = parsed.provider.displayName,
          amount = parsed.amount,
          senderPhone = parsed.senderPhone,
          transactionId = parsed.transactionId,
          status = "unmatched",
          message = reason
        )
      )
      if (notifyUser) {
        CashNotificationHelper.notifyVerificationResult(
          context = appContext,
          title = "حوالة غير مطابقة (${parsed.provider.displayName})",
          body = reason
        )
      }
      return reason
    }

    // 3. Atomic WriteBatch strictly matching firestore.rules schemas
    val (deviceName, verifierEmail) = ensureResolvedDeviceIdentity()
    val targetStage = if (settings.targetOrderStatusAfterPayment == "confirmed") "confirmed" else "preparing"
    val targetLabel = if (targetStage == "confirmed") "تم التأكيد" else "جاري التجهيز"
    val noteText = "تم التأكيد على هاتف: $deviceName • ${parsed.provider.displayName} • عملية #${parsed.transactionId} • مبلغ ${formatAmount(parsed.amount)} ج.م".take(300)

    val orderRef = firestore.collection("orders").document(matchedOrder.orderId)
    val logRef = firestore.collection("cashVerificationLogs").document()

    val orderUpdate = linkedMapOf<String, Any>(
      "paymentStatus" to "verified",
      "orderStatus" to targetStage,
      "statusNote" to noteText,
      "verifiedByDeviceName" to deviceName,
      "verifiedAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (verifierEmail.isNotBlank()) {
      orderUpdate["verifiedByEmail"] = verifierEmail
    }

    val batch = firestore.batch()
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
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
    }

    batch.set(
      logRef,
      buildCashLogPayload(
        providerName = parsed.provider.displayName,
        senderAddress = parsed.rawSenderAddress,
        senderPhone = parsed.senderPhone,
        amount = parsed.amount,
        transactionId = parsed.transactionId,
        matchedOrderId = matchedOrder.orderId,
        matchedOrderNumber = matchedOrder.orderNumber,
        status = "verified",
        reason = noteText,
        rawBodyPreview = parsed.rawBody,
        verifiedByDeviceName = deviceName,
        verifiedByEmail = verifierEmail
      )
    )

    batch.commit().awaitResult()
    notifyOrderPreparationStaff(
      orderId = matchedOrder.orderId,
      orderNumber = matchedOrder.orderNumber,
      customerName = matchedOrder.customerName,
      totalAmount = matchedOrder.totalAmount,
      confirmationSummary = "تأكيد آلي عبر هاتف «$deviceName»"
    )

    val successMsg = "تم تأكيد دفع الطلب #${matchedOrder.orderNumber} على هاتف «$deviceName» بمبلغ ${formatAmount(parsed.amount)} ج.م ونقله إلى «$targetLabel» وإشعار مسؤولي التجهيز."
    LocalVerificationFeed.push(
      LocalVerificationEvent(
        id = UUID.randomUUID().toString(),
        timestampMillis = System.currentTimeMillis(),
        senderAddress = parsed.rawSenderAddress,
        providerName = parsed.provider.displayName,
        amount = parsed.amount,
        senderPhone = parsed.senderPhone,
        transactionId = parsed.transactionId,
        status = "verified",
        message = successMsg
      )
    )
    if (notifyUser) {
      CashNotificationHelper.notifyVerificationResult(
        context = appContext,
        title = "تم تأكيد طلب #${matchedOrder.orderNumber} آلياً",
        body = successMsg
      )
    }
    return successMsg
  }

  suspend fun verifyOrderManually(
    order: PendingTransferOrder,
    targetStage: String
  ): Result<String> = runCatching {
    if (auth.currentUser?.uid.isNullOrBlank()) error("يجب تسجيل الدخول أولاً")
    val (deviceName, verifierEmail) = ensureResolvedDeviceIdentity()
    val normalizedStage = if (targetStage == "confirmed") "confirmed" else "preparing"
    val targetLabel = if (normalizedStage == "confirmed") "تم التأكيد" else "جاري التجهيز"
    val noteText = "تم التأكيد يدوياً على هاتف: $deviceName (${order.effectiveSenderPhone.ifBlank { "تحويل إلكتروني" }})".take(300)

    val orderRef = firestore.collection("orders").document(order.orderId)
    val logRef = firestore.collection("cashVerificationLogs").document()

    val orderUpdate = linkedMapOf<String, Any>(
      "paymentStatus" to "verified",
      "orderStatus" to normalizedStage,
      "statusNote" to noteText,
      "verifiedByDeviceName" to deviceName,
      "verifiedAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (verifierEmail.isNotBlank()) {
      orderUpdate["verifiedByEmail"] = verifierEmail
    }

    val batch = firestore.batch()
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
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
    }
    batch.set(
      logRef,
      buildCashLogPayload(
        providerName = "تأكيد يدوي",
        senderAddress = "Manual",
        senderPhone = SmsCashParser.normalizeEgyptianPhone(order.effectiveSenderPhone),
        amount = order.totalAmount.coerceAtLeast(0.0),
        transactionId = "MANUAL-${order.orderNumber}-${System.currentTimeMillis()}",
        matchedOrderId = order.orderId,
        matchedOrderNumber = order.orderNumber,
        status = "verified",
        reason = noteText,
        rawBodyPreview = noteText,
        verifiedByDeviceName = deviceName,
        verifiedByEmail = verifierEmail
      )
    )
    batch.commit().awaitResult()
    notifyOrderPreparationStaff(
      orderId = order.orderId,
      orderNumber = order.orderNumber,
      customerName = order.customerName,
      totalAmount = order.totalAmount,
      confirmationSummary = "تأكيد يدوي عبر هاتف «$deviceName»"
    )
    "تم تأكيد الطلب #${order.orderNumber} على هاتف «$deviceName» ونقله إلى «$targetLabel» وإشعار مسؤولي التجهيز."
  }

  private suspend fun notifyOrderPreparationStaff(
    orderId: String,
    orderNumber: String,
    customerName: String,
    totalAmount: Double,
    confirmationSummary: String
  ) {
    if (orderId.isBlank()) return
    val targetUids = LinkedHashSet<String>()
    val prepRoles = setOf("owner", "manager", "order_manager", "customer_service")

    runCatching {
      val staffSnap = firestore.collection("staff").get().awaitResult()
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
        .awaitResult()
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
              "createdAt" to FieldValue.serverTimestamp()
            )
          )
        }
        notifBatch.commit().awaitResult()
      }
    }
  }

  private suspend fun writeCloudLog(
    parsed: ParsedCashSms,
    status: String,
    matchedOrderId: String?,
    matchedOrderNumber: String?,
    reason: String
  ) {
    val (deviceName, verifierEmail) = ensureResolvedDeviceIdentity()
    runCatching {
      firestore.collection("cashVerificationLogs").add(
        buildCashLogPayload(
          providerName = parsed.provider.displayName,
          senderAddress = parsed.rawSenderAddress,
          senderPhone = parsed.senderPhone,
          amount = parsed.amount.coerceAtLeast(0.0),
          transactionId = parsed.transactionId,
          matchedOrderId = matchedOrderId,
          matchedOrderNumber = matchedOrderNumber,
          status = status,
          reason = reason,
          rawBodyPreview = parsed.rawBody,
          verifiedByDeviceName = deviceName,
          verifiedByEmail = verifierEmail
        )
      ).awaitResult()
    }
  }

  private fun buildCashLogPayload(
    providerName: String,
    senderAddress: String,
    senderPhone: String,
    amount: Double,
    transactionId: String,
    matchedOrderId: String?,
    matchedOrderNumber: String?,
    status: String,
    reason: String,
    rawBodyPreview: String,
    verifiedByDeviceName: String = "",
    verifiedByEmail: String = ""
  ): Map<String, Any> {
    val map = linkedMapOf<String, Any>(
      "providerName" to providerName.ifBlank { "محفظة" }.take(80),
      "senderAddress" to senderAddress.ifBlank { "SMS" }.take(80),
      "status" to status,
      "isSettled" to false,
      "createdAt" to FieldValue.serverTimestamp()
    )
    if (senderPhone.isNotBlank()) {
      map["senderPhone"] = senderPhone.take(40)
    }
    if (amount >= 0.0) {
      map["amount"] = amount
    }
    if (transactionId.isNotBlank()) {
      map["transactionId"] = transactionId.take(80)
    }
    if (!matchedOrderId.isNullOrBlank()) {
      map["matchedOrderId"] = matchedOrderId.take(80)
    }
    if (!matchedOrderNumber.isNullOrBlank()) {
      map["matchedOrderNumber"] = matchedOrderNumber.take(40)
    }
    if (reason.isNotBlank()) {
      map["reason"] = reason.take(300)
    }
    if (rawBodyPreview.isNotBlank()) {
      map["rawBodyPreview"] = rawBodyPreview.take(300)
    }
    if (verifiedByDeviceName.isNotBlank()) {
      map["verifiedByDeviceName"] = verifiedByDeviceName.take(80)
    }
    if (verifiedByEmail.isNotBlank()) {
      map["verifiedByEmail"] = verifiedByEmail.take(120)
    }
    return map
  }

  private fun mapOrderDocument(doc: DocumentSnapshot): PendingTransferOrder? {
    if (!doc.exists()) return null
    val orderNumber = doc.getString("orderNumber")?.takeIf { it.isNotBlank() } ?: doc.id.take(8)
    val totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0
    val createdAt = (doc.get("createdAt") as? Timestamp)?.toDate()?.time
      ?: (doc.getLong("createdAtMillis") ?: 0L)

    return PendingTransferOrder(
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
      createdAtMillis = createdAt
    )
  }

  private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) {
      String.format(Locale.US, "%.0f", value)
    } else {
      String.format(Locale.US, "%.2f", value)
    }
  }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
  suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { ex -> cont.resumeWithException(ex) }
  }
