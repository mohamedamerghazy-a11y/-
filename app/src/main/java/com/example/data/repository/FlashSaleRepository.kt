package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.FlashSale
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

class FlashSaleRepository(
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
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا لتنفيذ هذا الإجراء.")
  }

  fun observeActiveFlashSales(): Flow<List<FlashSale>> {
    val query = db.collection("flashSales").whereEqualTo("isActive", true)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(FlashSale::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(
            compareBy<FlashSale> { it.startsAt?.seconds ?: 0L }
              .thenByDescending { it.createdAt?.seconds ?: 0L },
          )
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.LIST, "flashSales?isActive=true")
        throw error
      }
  }

  fun observeAllFlashSales(): Flow<List<FlashSale>> {
    val uid = requireUserId()
    val colRef = db.collection("flashSales")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(FlashSale::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: it.startsAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveFlashSale(flashSale: FlashSale): Result<String> {
    requireUserId()
    val colRef = db.collection("flashSales")
    val isNew = flashSale.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(flashSale.id)

    val cleanTitle = flashSale.title.trim()
    val cleanBadge = flashSale.badgeText.trim()
    val startsAt = flashSale.startsAt
      ?: return Result.failure(IllegalArgumentException("يرجى تحديد وقت بداية الفلاش سيل."))
    val endsAt = flashSale.endsAt
      ?: return Result.failure(IllegalArgumentException("يرجى تحديد وقت انتهاء الفلاش سيل."))
    if (endsAt.toDate().time <= startsAt.toDate().time) {
      return Result.failure(IllegalArgumentException("يجب أن يكون وقت الانتهاء بعد وقت البداية."))
    }
    if (cleanTitle.length < 2) {
      return Result.failure(IllegalArgumentException("يرجى إدخال عنوان واضح للفلاش سيل."))
    }
    val validItems = flashSale.items.filter { it.productId.isNotBlank() && it.flashRetailPrice > 0.0 }
    if (validItems.isEmpty()) {
      return Result.failure(IllegalArgumentException("اختر منتجًا واحدًا على الأقل بسعر فلاش سيل صالح."))
    }

    val itemMaps = validItems.map { item ->
      buildMap<String, Any> {
        put("productId", item.productId.trim())
        put("flashRetailPrice", item.flashRetailPrice)
        if (item.flashWholesalePrice != null && item.flashWholesalePrice > 0.0) {
          put("flashWholesalePrice", item.flashWholesalePrice)
        }
        if (item.maxQtyPerOrder != null && item.maxQtyPerOrder > 0) {
          put("maxQtyPerOrder", item.maxQtyPerOrder)
        }
      }
    }

    return try {
      if (isNew) {
        val payload = buildMap<String, Any> {
          put("title", cleanTitle)
          put("badgeText", cleanBadge)
          put("startsAt", startsAt)
          put("endsAt", endsAt)
          put("isActive", flashSale.isActive)
          put("items", itemMaps)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(payload).await()
      } else {
        val updates = buildMap<String, Any> {
          put("title", cleanTitle)
          put("badgeText", cleanBadge)
          put("startsAt", startsAt)
          put("endsAt", endsAt)
          put("isActive", flashSale.isActive)
          put("items", itemMaps)
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updates).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun toggleFlashSaleActive(flashSaleId: String, isActive: Boolean): Result<Unit> {
    requireUserId()
    val docRef = db.collection("flashSales").document(flashSaleId)
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

  suspend fun deleteFlashSale(flashSaleId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("flashSales").document(flashSaleId)
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
