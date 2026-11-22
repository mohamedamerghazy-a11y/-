package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.NotificationType
import com.example.data.model.Product
import com.example.data.model.SearchMiss
import com.example.data.model.StockAlert
import com.example.data.model.buildSearchMissDocId
import com.example.data.model.normalizeSearchQuery
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

class EngagementRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  fun observeUserStockAlertProductIds(uid: String): Flow<Set<String>> {
    val query = db.collection("stockAlerts").whereEqualTo("userId", uid)
    return query.snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { it.getString("productId")?.takeIf { id -> id.isNotBlank() } }.toSet()
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, "stockAlerts?userId=$uid")
        throw error
      }
  }

  suspend fun toggleStockAlert(
    uid: String,
    product: Product,
    isCurrentlySubscribed: Boolean,
  ): Result<Boolean> {
    if (uid.isBlank() || product.id.isBlank()) {
      return Result.failure(IllegalArgumentException("تعذر تحديد المنتج أو الحساب."))
    }
    val alertId = "${product.id}_$uid"
    val docRef = db.collection("stockAlerts").document(alertId)
    return try {
      if (isCurrentlySubscribed) {
        docRef.delete().await()
        Result.success(false)
      } else {
        val payload = mapOf(
          "productId" to product.id,
          "productName" to product.name.trim().ifEmpty { "المنتج" }.take(120),
          "userId" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        Result.success(true)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isCurrentlySubscribed) OperationType.DELETE else OperationType.CREATE,
        docRef.path,
      )
      Result.failure(e)
    }
  }

  fun observeAllStockAlertCounts(): Flow<Map<String, Int>> {
    val uid = auth.currentUser?.uid
    val colRef = db.collection("stockAlerts")
    return colRef.snapshots()
      .map { snapshot ->
        val counts = mutableMapOf<String, Int>()
        snapshot.documents.forEach { doc ->
          val productId = doc.getString("productId")?.trim().orEmpty()
          if (productId.isNotEmpty()) {
            counts[productId] = (counts[productId] ?: 0) + 1
          }
        }
        counts.toMap()
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun dispatchRestockNotificationsAndClearAlerts(
    productId: String,
    productName: String,
    notificationRepository: NotificationRepository,
  ): Result<Int> {
    if (auth.currentUser == null || productId.isBlank()) return Result.success(0)
    return try {
      val snapshot = db.collection("stockAlerts")
        .whereEqualTo("productId", productId)
        .get()
        .await()
      if (snapshot.isEmpty) return Result.success(0)

      val alerts = snapshot.documents.mapNotNull { doc ->
        val userId = doc.getString("userId")?.trim().orEmpty()
        if (userId.isEmpty()) null else doc.reference to StockAlert(id = doc.id, productId = productId, productName = productName, userId = userId)
      }
      val userIds = alerts.map { it.second.userId }.distinct()
      val cleanName = productName.trim().ifEmpty { "المنتج" }.take(100)

      if (userIds.isNotEmpty()) {
        notificationRepository.broadcastNotificationToUsers(
          targetUids = userIds,
          title = "توفر المنتج من جديد!",
          message = "المنتج «$cleanName» أصبح متوفرًا الآن في المتجر — اطلبه قبل نفاد الكمية.",
          type = NotificationType.STOCK_ALERT,
          referenceId = productId,
        ).getOrThrow()
      }

      alerts.map { it.first }.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { ref -> batch.delete(ref) }
        batch.commit().await()
      }
      Result.success(userIds.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "stockAlerts/dispatch/$productId")
      Result.failure(e)
    }
  }

  suspend fun logSearchMiss(
    rawQuery: String,
    source: String = "android",
  ): Result<Unit> {
    if (auth.currentUser == null) return Result.success(Unit)
    val cleanQuery = rawQuery.trim().replace(Regex("\\s+"), " ").take(120)
    val normalized = normalizeSearchQuery(cleanQuery).take(120)
    if (normalized.length < 2) return Result.success(Unit)

    val docId = buildSearchMissDocId(normalized)
    val docRef = db.collection("searchMisses").document(docId)
    val cleanSource = if (source == "web") "web" else "android"

    return try {
      db.runTransaction { tx ->
        val snap = tx.get(docRef)
        if (!snap.exists()) {
          tx.set(
            docRef,
            mapOf(
              "query" to cleanQuery,
              "normalizedQuery" to normalized,
              "missCount" to 1L,
              "lastSource" to cleanSource,
              "createdAt" to FieldValue.serverTimestamp(),
              "lastSearchedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          val currentCount = (snap.getLong("missCount") ?: 0L).coerceAtLeast(1L)
          tx.update(
            docRef,
            mapOf(
              "query" to cleanQuery,
              "missCount" to (currentCount + 1L),
              "lastSource" to cleanSource,
              "lastSearchedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeSearchMisses(): Flow<List<SearchMiss>> {
    val uid = auth.currentUser?.uid
    val colRef = db.collection("searchMisses")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(SearchMiss::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(
            compareByDescending<SearchMiss> { it.missCount }
              .thenByDescending { it.lastSearchedAt?.seconds ?: 0L },
          )
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun deleteSearchMiss(missId: String): Result<Unit> {
    if (auth.currentUser == null || missId.isBlank()) return Result.success(Unit)
    val docRef = db.collection("searchMisses").document(missId)
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
