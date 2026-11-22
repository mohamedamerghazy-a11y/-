package com.aleman.cash.data

import android.app.Activity
import android.content.Context
import com.aleman.cash.model.CashVerificationLogItem
import com.aleman.cash.model.CashVerifierSettings
import com.aleman.cash.model.LocalVerificationEvent
import com.aleman.cash.model.ParsedCashSms
import com.aleman.cash.model.PendingTransferOrder
import com.aleman.cash.model.StaffAuthState
import com.aleman.cash.sms.SmsCashParser
import com.aleman.cash.sms.SmsParseResult
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocalVerificationFeed {
  private val _events = MutableStateFlow<List<LocalVerificationEvent>>(emptyList())
  val events: StateFlow<List<LocalVerificationEvent>> = _events.asStateFlow()

  fun push(event: LocalVerificationEvent) {
    _events.update { current -> (listOf(event) + current).take(60) }
  }
}

class CashVerificationRepository(context: Context) {

  private val appContext = context.applicationContext
  private val auth = FirebaseInitializer.auth(appContext)
  private val firestore = FirebaseInitializer.firestore(appContext)

  private val defaultOwnerEmail = "mohamedamerghazy@gmail.com"

  suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> = runCatching {
    val provider = OAuthProvider.newBuilder("google.com")
      .addCustomParameter("prompt", "select_account")
      .build()

    val pendingTask = auth.pendingAuthResult
    val authResult = if (pendingTask != null) {
      pendingTask.awaitResult()
    } else {
      auth.startActivityForSignInWithProvider(activity, provider).awaitResult()
    }
    authResult.user ?: error("تعذر استكمال تسجيل الدخول بجوجل")
  }

  fun currentUser(): FirebaseUser? = auth.currentUser

  fun signOut() {
    auth.signOut()
  }

  suspend fun checkStaffAuthorization(): Pair<StaffAuthState, String> {
    val user = auth.currentUser ?: return StaffAuthState.SignedOut to ""
    val email = user.email?.trim()?.lowercase(Locale.ROOT).orEmpty()

    val storeDoc = runCatching {
      firestore.collection("settings").document("store").get().awaitResult()
    }.getOrNull()

    val configuredOwnerEmail = storeDoc?.getString("ownerEmail")
      ?.trim()?.lowercase(Locale.ROOT)
      ?.takeIf { it.isNotBlank() }
      ?: defaultOwnerEmail

    if (email.isNotBlank() && email == configuredOwnerEmail) {
      return StaffAuthState.Authorized to "مالك المتجر ($email)"
    }

    val staffDoc = runCatching {
      firestore.collection("staffUsers").document(user.uid).get().awaitResult()
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
        return StaffAuthState.Authorized to "موظف مخوّل ($display)"
      }
    }

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
          CashVerificationLogItem(
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
              ?: (doc.getLong("createdAtMillis") ?: System.currentTimeMillis())
          )
        }
        onUpdate(logs)
      }
  }

  suspend fun processIncomingSms(
    senderAddress: String,
    rawBody: String,
    settings: CashVerifierSettings,
    notifyUser: Boolean = true
  ): String {
    val result = SmsCashParser.parseIncomingPaymentSms(
      senderAddress = senderAddress,
      rawBody = rawBody,
      providers = settings.providers
    )

    when (result) {
      is SmsParseResult.IgnoredUnknownOrDisabledSender -> {
        val msg = "تم تجاهل الرسالة لأن المرسل «$senderAddress» غير مسجل أو غير مفعّل في الإعدادات."
        LocalVerificationFeed.push(
          LocalVerificationEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            senderAddress = senderAddress,
            providerName = "غير مفعّل",
            amount = null,
            senderPhone = null,
            transactionId = null,
            status = "ignored_sender",
            message = msg
          )
        )
        return msg
      }

      is SmsParseResult.IgnoredNotIncomingPayment -> {
        val msg = "رسالة من ${result.provider.displayName} تم تخطيها: ${result.reason}"
        LocalVerificationFeed.push(
          LocalVerificationEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            senderAddress = senderAddress,
            providerName = result.provider.displayName,
            amount = null,
            senderPhone = null,
            transactionId = null,
            status = "ignored_content",
            message = msg
          )
        )
        return msg
      }

      is SmsParseResult.Matched -> {
        return verifyParsedPayment(result.parsed, settings, notifyUser)
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
        .whereEqualTo("status", "matched")
        .limit(1)
        .get()
        .awaitResult()
    }.getOrNull()

    if (existingDup != null && !existingDup.isEmpty) {
      val msg = "عملية مكررة (#${parsed.transactionId}): تم تأكيد هذه الحوالة مسبقاً."
      writeCloudLog(
        parsed = parsed,
        status = "duplicate",
        matchedOrderId = null,
        matchedOrderNumber = null,
        reason = msg,
        uid = uid
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
        reason = reason,
        uid = uid
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

    // 3. Atomic WriteBatch to verify order, advance to preparing/confirmed, notify customer, and write audit log
    val targetStage = if (settings.targetOrderStatusAfterPayment == "confirmed") "confirmed" else "preparing"
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
        "updatedAt" to FieldValue.serverTimestamp()
      )
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
          "createdAt" to FieldValue.serverTimestamp()
        )
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
        "createdAt" to FieldValue.serverTimestamp()
      )
    )

    batch.commit().awaitResult()

    val successMsg = "تم تأكيد دفع الطلب #${matchedOrder.orderNumber} (${formatAmount(matchedOrder.totalAmount)} ج.م) بمبلغ ${formatAmount(parsed.amount)} ج.م ونقله إلى «$targetLabel»."
    LocalVerificationFeed.push(
      LocalVerificationEvent(
        id = UUID.randomUUID().toString(),
        timestampMillis = System.currentTimeMillis(),
        senderAddress = parsed.rawSenderAddress,
        providerName = parsed.provider.displayName,
        amount = parsed.amount,
        senderPhone = parsed.senderPhone,
        transactionId = parsed.transactionId,
        status = "matched",
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
    val uid = auth.currentUser?.uid ?: error("يجب تسجيل الدخول أولاً")
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
        "updatedAt" to FieldValue.serverTimestamp()
      )
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
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
    }
    batch.set(
      logRef,
      mapOf(
        "transactionId" to "MANUAL-${order.orderNumber}-${System.currentTimeMillis()}",
        "providerName" to "تأكيد يدوي",
        "senderAddress" to "Manual",
        "senderPhone" to SmsCashParser.normalizeEgyptianPhone(order.effectiveSenderPhone),
        "amount" to order.totalAmount,
        "matchedOrderId" to order.orderId,
        "matchedOrderNumber" to order.orderNumber,
        "status" to "matched",
        "reason" to noteText,
        "rawBodyPreview" to noteText,
        "verifiedByUid" to uid,
        "createdAt" to FieldValue.serverTimestamp()
      )
    )
    batch.commit().awaitResult()
    "تم تأكيد الطلب #${order.orderNumber} ونقله إلى «$targetLabel»."
  }

  private suspend fun writeCloudLog(
    parsed: ParsedCashSms,
    status: String,
    matchedOrderId: String?,
    matchedOrderNumber: String?,
    reason: String,
    uid: String
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
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).awaitResult()
    }
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
      customerPhone = doc.getString("customerPhone").orEmpty(),
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
