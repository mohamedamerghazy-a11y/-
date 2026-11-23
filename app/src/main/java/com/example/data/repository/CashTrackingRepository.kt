package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.CashVerificationLog
import com.example.data.model.CashVerifierAccount
import com.example.data.model.CashWithdrawalRecord
import com.example.data.model.buildCashAccountDocId
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
import java.util.Locale

class CashTrackingRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا لإدارة ومتابعة تحويلات الكاش.")
  }

  fun observeCashVerifierAccounts(): Flow<List<CashVerifierAccount>> {
    val uid = requireUserId()
    val colRef = db.collection("cashVerifierAccounts")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(CashVerifierAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.deviceName.lowercase(Locale.ROOT) }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  fun observeAllCashLogs(): Flow<List<CashVerificationLog>> {
    val uid = requireUserId()
    val colRef = db.collection("cashVerificationLogs")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(CashVerificationLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  fun observeCashWithdrawals(): Flow<List<CashWithdrawalRecord>> {
    val uid = requireUserId()
    val colRef = db.collection("cashWithdrawals")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(CashWithdrawalRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveCashVerifierAccount(
    email: String,
    deviceName: String,
    phoneNumber: String?,
    isActive: Boolean,
    isEditExisting: Boolean = false,
  ): Result<String> {
    requireUserId()
    val cleanEmail = email.trim().lowercase(Locale.ROOT)
    val cleanName = deviceName.trim()
    val cleanPhone = phoneNumber?.trim()?.takeIf { it.isNotEmpty() }?.take(30)

    if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
      return Result.failure(IllegalArgumentException("يرجى إدخال بريد إلكتروني صحيح."))
    }
    if (cleanName.length < 2) {
      return Result.failure(IllegalArgumentException("يرجى إدخال اسم واضح للهاتف (حرفين على الأقل)."))
    }

    val docId = buildCashAccountDocId(cleanEmail)
    val docRef = db.collection("cashVerifierAccounts").document(docId)

    return try {
      val existing = docRef.get().await()
      if (existing.exists() || isEditExisting) {
        val updates = buildMap<String, Any> {
          put("deviceName", cleanName.take(80))
          put("phoneNumber", cleanPhone ?: FieldValue.delete())
          put("isActive", isActive)
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updates).await()
      } else {
        val payload = buildMap<String, Any> {
          put("email", cleanEmail.take(160))
          put("deviceName", cleanName.take(80))
          if (cleanPhone != null) put("phoneNumber", cleanPhone)
          put("isActive", isActive)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(payload).await()
      }
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isEditExisting) OperationType.UPDATE else OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun toggleCashVerifierAccountActive(accountId: String, isActive: Boolean): Result<Unit> {
    requireUserId()
    val docRef = db.collection("cashVerifierAccounts").document(accountId)
    return try {
      docRef.update(
        mapOf(
          "isActive" to isActive,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteCashVerifierAccount(accountId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("cashVerifierAccounts").document(accountId)
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
   * تنفيذ عملية «تم السحب» لتصفير حساب هاتف معين:
   * - مسموح فقط لمالك الحساب أو المحاسب (محمي بقواعد Firestore).
   * - يُنشئ سجل سحب موثّق في cashWithdrawals.
   * - يُعلّم جميع العمليات القائمة غير المسحوبة لهذا الهاتف بـ isSettled = true.
   * - يُحدّث lastResetAt على حساب الهاتف إن كان مسجلًا في cashVerifierAccounts.
   */
  suspend fun recordWithdrawalAndResetDevice(
    deviceKey: String,
    deviceName: String,
    deviceEmail: String,
    registeredAccountId: String?,
    unsettledLogs: List<CashVerificationLog>,
    actorName: String,
    note: String?,
  ): Result<Double> {
    val uid = requireUserId()
    val actorEmail = auth.currentUser?.email?.trim().orEmpty()
    val validLogs = unsettledLogs.filter { it.id.isNotBlank() && it.isVerifiedOperation && !it.isSettled }
    val totalAmount = validLogs.sumOf { it.amount }
    if (validLogs.isEmpty() || totalAmount <= 0.0) {
      return Result.failure(IllegalStateException("لا يوجد رصيد قائم غير مسحوب لهذا الهاتف لتصفيره."))
    }

    val withdrawalRef = db.collection("cashWithdrawals").document()
    val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
    val cleanActorName = actorName.trim().ifBlank { actorEmail.ifBlank { "الإدارة" } }.take(120)

    return try {
      // 1. Create withdrawal record + update registered account if exists + settle first chunk
      val chunks = validLogs.chunked(380)
      val firstBatch = db.batch()

      val withdrawalPayload = buildMap<String, Any> {
        put("deviceKey", deviceKey.take(160))
        put("deviceName", deviceName.take(100))
        if (deviceEmail.isNotBlank()) put("deviceEmail", deviceEmail.take(160))
        put("withdrawnAmount", totalAmount)
        put("operationsCount", validLogs.size)
        put("withdrawnByUid", uid)
        put("withdrawnByName", cleanActorName)
        if (actorEmail.isNotBlank()) put("withdrawnByEmail", actorEmail.take(160))
        if (cleanNote != null) put("note", cleanNote)
        put("createdAt", FieldValue.serverTimestamp())
      }
      firstBatch.set(withdrawalRef, withdrawalPayload)

      if (!registeredAccountId.isNullOrBlank()) {
        val accRef = db.collection("cashVerifierAccounts").document(registeredAccountId)
        firstBatch.update(
          accRef,
          mapOf(
            "lastResetAt" to FieldValue.serverTimestamp(),
            "lastResetByName" to cleanActorName,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      chunks.firstOrNull().orEmpty().forEach { log ->
        val logRef = db.collection("cashVerificationLogs").document(log.id)
        firstBatch.update(
          logRef,
          mapOf(
            "isSettled" to true,
            "withdrawalId" to withdrawalRef.id,
            "settledAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      firstBatch.commit().await()

      // 2. Remaining chunks (if more than 380 operations)
      for (chunkIndex in 1 until chunks.size) {
        val extraBatch = db.batch()
        chunks[chunkIndex].forEach { log ->
          val logRef = db.collection("cashVerificationLogs").document(log.id)
          extraBatch.update(
            logRef,
            mapOf(
              "isSettled" to true,
              "withdrawalId" to withdrawalRef.id,
              "settledAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        extraBatch.commit().await()
      }

      Result.success(totalAmount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, withdrawalRef.path)
      Result.failure(e)
    }
  }
}
