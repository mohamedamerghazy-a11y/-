package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.local.LocalCartEntry
import com.example.data.model.CartItemRecord
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

class CartRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  private fun cartCollection(uid: String) =
    db.collection("users").document(uid).collection("cart")

  fun observeUserCart(uid: String): Flow<List<CartItemRecord>> {
    val colRef = cartCollection(uid)
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(CartItemRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun setQuantity(
    uid: String,
    productId: String,
    quantity: Int,
    flashSaleId: String? = null,
    selectedColor: String? = null,
    selectedSize: String? = null,
    cartItemId: String? = null,
  ): Result<Unit> {
    val cleanId = productId.trim()
    if (cleanId.isEmpty()) return Result.failure(IllegalArgumentException("معرّف المنتج غير صالح."))
    val cleanColor = selectedColor?.trim()?.takeIf { it.isNotEmpty() }
    val cleanSize = selectedSize?.trim()?.takeIf { it.isNotEmpty() }
    val targetDocId = cartItemId?.trim()?.takeIf { it.isNotEmpty() }
      ?: com.example.data.model.buildCartItemId(cleanId, cleanColor, cleanSize)
    val cleanFlashId = flashSaleId?.trim()?.takeIf { it.isNotEmpty() }
    val docRef = cartCollection(uid).document(targetDocId)
    return try {
      if (quantity <= 0) {
        docRef.delete().await()
      } else {
        val existing = docRef.get().await()
        if (existing.exists()) {
          val updates = buildMap<String, Any> {
            put("quantity", quantity.coerceAtMost(100_000))
            if (cleanFlashId != null) {
              put("flashSaleId", cleanFlashId)
            }
            if (cleanColor != null) {
              put("selectedColor", cleanColor)
            }
            if (cleanSize != null) {
              put("selectedSize", cleanSize)
            }
            put("updatedAt", FieldValue.serverTimestamp())
          }
          docRef.update(updates).await()
        } else {
          val payload = buildMap<String, Any> {
            put("userId", uid)
            put("productId", cleanId)
            put("quantity", quantity.coerceAtMost(100_000))
            if (cleanFlashId != null) {
              put("flashSaleId", cleanFlashId)
            }
            if (cleanColor != null) {
              put("selectedColor", cleanColor)
            }
            if (cleanSize != null) {
              put("selectedSize", cleanSize)
            }
            put("updatedAt", FieldValue.serverTimestamp())
          }
          docRef.set(payload).await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun removeItem(uid: String, cartItemId: String): Result<Unit> {
    val cleanId = cartItemId.trim()
    if (cleanId.isEmpty()) return Result.success(Unit)
    val docRef = cartCollection(uid).document(cleanId)
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

  suspend fun mergeLocalEntries(uid: String, entries: List<LocalCartEntry>): Result<Unit> {
    if (entries.isEmpty()) return Result.success(Unit)
    val colRef = cartCollection(uid)
    return try {
      val existingDocs = colRef.get().await().documents.associateBy { it.id }
      val batch = db.batch()
      entries.forEach { entry ->
        val cleanId = entry.productId.trim()
        val cleanColor = entry.selectedColor?.trim()?.takeIf { it.isNotEmpty() }
        val cleanSize = entry.selectedSize?.trim()?.takeIf { it.isNotEmpty() }
        val docId = com.example.data.model.buildCartItemId(cleanId, cleanColor, cleanSize)
        if (cleanId.isNotEmpty() && entry.quantity > 0) {
          val docRef = colRef.document(docId)
          val existingDoc = existingDocs[docId]
          val cleanFlashId = entry.flashSaleId?.trim()?.takeIf { it.isNotEmpty() }
          if (existingDoc != null && existingDoc.exists()) {
            val prevQty = (existingDoc.getLong("quantity") ?: 0L).toInt()
            val mergedQty = (prevQty + entry.quantity).coerceIn(1, 100_000)
            val updates = buildMap<String, Any> {
              put("quantity", mergedQty)
              if (cleanFlashId != null) put("flashSaleId", cleanFlashId)
              if (cleanColor != null) put("selectedColor", cleanColor)
              if (cleanSize != null) put("selectedSize", cleanSize)
              put("updatedAt", FieldValue.serverTimestamp())
            }
            batch.update(docRef, updates)
          } else {
            val payload = buildMap<String, Any> {
              put("userId", uid)
              put("productId", cleanId)
              put("quantity", entry.quantity.coerceIn(1, 100_000))
              if (cleanFlashId != null) put("flashSaleId", cleanFlashId)
              if (cleanColor != null) put("selectedColor", cleanColor)
              if (cleanSize != null) put("selectedSize", cleanSize)
              put("updatedAt", FieldValue.serverTimestamp())
            }
            batch.set(docRef, payload)
          }
        }
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, colRef.path)
      Result.failure(e)
    }
  }

  suspend fun clearUserCart(uid: String): Result<Unit> {
    val colRef = cartCollection(uid)
    return try {
      val docs = colRef.get().await().documents
      if (docs.isNotEmpty()) {
        val batch = db.batch()
        docs.forEach { batch.delete(it.reference) }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, colRef.path)
      Result.failure(e)
    }
  }
}
