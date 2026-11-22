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
}
