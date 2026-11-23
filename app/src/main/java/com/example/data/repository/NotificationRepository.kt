package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NotificationRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  fun observeUserNotifications(uid: String): Flow<List<AppNotification>> {
    val colRef = db.collection("users").document(uid).collection("notifications")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun sendNotificationToUser(
    targetUid: String,
    title: String,
    message: String,
    type: NotificationType,
    referenceId: String? = null,
  ): Result<String> {
    if (auth.currentUser == null || targetUid.isBlank()) {
      return Result.failure(IllegalStateException("تعذر تحديد المستلم للإشعار."))
    }
    val docRef = db.collection("users").document(targetUid.trim()).collection("notifications").document()
    val cleanRef = referenceId?.trim()?.takeIf { it.isNotEmpty() }?.take(128)
    val payload = buildMap<String, Any> {
      put("userId", targetUid.trim())
      put("title", title.trim().ifEmpty { "إشعار من المتجر" }.take(120))
      put("message", message.trim().ifEmpty { "لديك تحديث جديد في حسابك." }.take(500))
      put("type", type.code)
      if (cleanRef != null) put("referenceId", cleanRef)
      put("isRead", false)
      put("createdAt", FieldValue.serverTimestamp())
    }
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * ينشئ إشعاراً بمعرّف ثابت (Idempotent) لحساب المستخدم الحالي فقط إذا لم يكن موجوداً من قبل،
   * لمنع تكرار إشعارات الطلبات المنتظرة للتأكيد عند إعادة فتح التطبيق.
   */
  suspend fun ensureDeterministicNotification(
    targetUid: String,
    notifId: String,
    title: String,
    message: String,
    type: NotificationType,
    referenceId: String? = null,
  ): Result<Boolean> {
    val cleanUid = targetUid.trim()
    val cleanId = notifId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
    if (auth.currentUser == null || cleanUid.isBlank() || cleanId.isBlank()) {
      return Result.success(false)
    }
    val docRef = db.collection("users").document(cleanUid).collection("notifications").document(cleanId)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        return Result.success(false)
      }
      val cleanRef = referenceId?.trim()?.takeIf { it.isNotEmpty() }?.take(128)
      val payload = buildMap<String, Any> {
        put("userId", cleanUid)
        put("title", title.trim().ifEmpty { "إشعار من المتجر" }.take(120))
        put("message", message.trim().ifEmpty { "لديك تحديث جديد." }.take(500))
        put("type", type.code)
        if (cleanRef != null) put("referenceId", cleanRef)
        put("isRead", false)
        put("createdAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
      Result.success(true)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun broadcastNotificationToUsers(
    targetUids: List<String>,
    title: String,
    message: String,
    type: NotificationType = NotificationType.OFFER_PROMO,
    referenceId: String? = null,
  ): Result<Int> {
    if (auth.currentUser == null) {
      return Result.failure(IllegalStateException("يجب تسجيل الدخول أولًا لإرسال الإشعارات."))
    }
    val cleanUids = targetUids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleanUids.isEmpty()) return Result.success(0)

    val cleanTitle = title.trim().ifEmpty { "عرض جديد من الإيمان للتجارة" }.take(120)
    val cleanMessage = message.trim().ifEmpty { "تصفح أحدث العروض الآن." }.take(500)
    val cleanRef = referenceId?.trim()?.takeIf { it.isNotEmpty() }?.take(128)

    return try {
      var sentCount = 0
      cleanUids.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { uid ->
          val docRef = db.collection("users").document(uid).collection("notifications").document()
          val payload = buildMap<String, Any> {
            put("userId", uid)
            put("title", cleanTitle)
            put("message", cleanMessage)
            put("type", type.code)
            if (cleanRef != null) put("referenceId", cleanRef)
            put("isRead", false)
            put("createdAt", FieldValue.serverTimestamp())
          }
          batch.set(docRef, payload)
        }
        batch.commit().await()
        sentCount += chunk.size
      }
      Result.success(sentCount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "notifications/broadcast")
      Result.failure(e)
    }
  }

  suspend fun markAsRead(uid: String, notifId: String): Result<Unit> {
    val docRef = db.collection("users").document(uid).collection("notifications").document(notifId)
    return try {
      docRef.update("isRead", true).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun markAllAsRead(uid: String, unreadIds: List<String>): Result<Unit> {
    val cleanIds = unreadIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleanIds.isEmpty()) return Result.success(Unit)
    return try {
      cleanIds.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { id ->
          val docRef = db.collection("users").document(uid).collection("notifications").document(id)
          batch.update(docRef, "isRead", true)
        }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications/markAllAsRead")
      Result.failure(e)
    }
  }

  suspend fun deleteNotification(uid: String, notifId: String): Result<Unit> {
    val docRef = db.collection("users").document(uid).collection("notifications").document(notifId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * يرسل إشعاراً فورياً لكل موظف نشط مسؤول عن تجهيز الطلبات (مالك المتجر، المدير، مسؤول الطلبات،
   * خدمة العملاء، وأي موظف لديه صلاحية إدارة الطلبات) فور تأكيد الدفع آلياً أو يدوياً.
   */
  suspend fun notifyOrderPreparationStaff(
    orderId: String,
    orderNumber: String,
    customerName: String,
    totalAmount: Double,
    confirmationSummary: String,
  ): Result<Int> {
    if (auth.currentUser == null || orderId.isBlank()) {
      return Result.success(0)
    }
    val targetUids = LinkedHashSet<String>()

    try {
      val staffSnap = db.collection("staff").get().await()
      staffSnap.documents.forEach { doc ->
        val isActive = doc.getBoolean("isActive") == true
        if (!isActive) return@forEach
        val role = doc.getString("role")?.trim()?.lowercase().orEmpty()
        val permissions = (doc.get("permissions") as? List<*>)
          ?.mapNotNull { it as? String }
          ?.toSet()
          .orEmpty()
        val canPrepare = role in ORDER_PREP_ROLES || "manage_orders" in permissions
        if (canPrepare) {
          val uid = doc.getString("uid")?.trim().takeUnless { it.isNullOrBlank() } ?: doc.id
          if (uid.isNotBlank()) targetUids.add(uid)
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "staff")
    }

    try {
      val ownersSnap = db.collection("users")
        .whereEqualTo("role", "owner")
        .get()
        .await()
      ownersSnap.documents.forEach { doc ->
        val uid = doc.getString("uid")?.trim().takeUnless { it.isNullOrBlank() } ?: doc.id
        if (uid.isNotBlank()) targetUids.add(uid)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "users?role=owner")
    }

    if (targetUids.isEmpty()) return Result.success(0)

    val formattedAmount = formatNotificationAmount(totalAmount)
    val cleanCustomer = customerName.trim().ifEmpty { "عميل المتجر" }
    val cleanSummary = confirmationSummary.trim().ifEmpty { "تأكيد الدفع" }
    val title = "طلب جديد جاهز للتجهيز #$orderNumber"
    val message = "تم تأكيد الدفع ($cleanSummary) • العميل: $cleanCustomer • الإجمالي: $formattedAmount ج.م — يرجى البدء في التجهيز."

    return broadcastNotificationToUsers(
      targetUids = targetUids.toList(),
      title = title,
      message = message,
      type = NotificationType.ORDER_STATUS,
      referenceId = orderId,
    )
  }

  /**
   * يرسل إشعاراً لموظفي التوصيل النشطين عند تحويل حالة طلب التوصيل إلى «جاهز للتوصيل».
   */
  suspend fun notifyDeliveryDriversOrderReady(
    orderId: String,
    orderNumber: String,
    recipientName: String,
    totalAmount: Double,
    locationText: String,
  ): Result<Int> {
    if (auth.currentUser == null || orderId.isBlank()) {
      return Result.success(0)
    }
    val driverUids = LinkedHashSet<String>()

    try {
      val staffSnap = db.collection("staff").get().await()
      staffSnap.documents.forEach { doc ->
        val isActive = doc.getBoolean("isActive") == true
        if (!isActive) return@forEach
        val role = doc.getString("role")?.trim()?.lowercase().orEmpty()
        val permissions = (doc.get("permissions") as? List<*>)
          ?.mapNotNull { it as? String }
          ?.toSet()
          .orEmpty()
        val isDriver = role == "delivery_driver" || ("deliver_orders" in permissions && role != "owner")
        if (isDriver) {
          val uid = doc.getString("uid")?.trim().takeUnless { it.isNullOrBlank() } ?: doc.id
          if (uid.isNotBlank()) driverUids.add(uid)
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "staff")
    }

    if (driverUids.isEmpty()) return Result.success(0)

    val formattedAmount = formatNotificationAmount(totalAmount)
    val cleanRecipient = recipientName.trim().ifEmpty { "العميل" }
    val cleanLocation = locationText.trim().ifEmpty { "يرجى مراجعة العنوان داخل الطلب" }
    val title = "طلب جاهز للتوصيل #$orderNumber"
    val message = "تم تجهيز الطلب للعميل: $cleanRecipient • الإجمالي: $formattedAmount ج.م • العنوان: $cleanLocation"

    return broadcastNotificationToUsers(
      targetUids = driverUids.toList(),
      title = title,
      message = message,
      type = NotificationType.ORDER_STATUS,
      referenceId = orderId,
    )
  }

  private fun formatNotificationAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
      amount.toLong().toString()
    } else {
      String.format(java.util.Locale.US, "%.2f", amount)
    }
  }

  companion object {
    private val ORDER_PREP_ROLES = setOf("owner", "manager", "order_manager", "customer_service")
  }
}
