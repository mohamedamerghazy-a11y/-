package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WishlistRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  private fun wishlistCollection(uid: String) =
    db.collection("users").document(uid).collection("wishlist")

  fun observeWishlistProductIds(uid: String): Flow<Set<String>> {
    val colRef = wishlistCollection(uid)
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.getString("productId")?.takeIf { it.isNotBlank() } ?: doc.id.takeIf { it.isNotBlank() }
        }.toSet()
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun toggleWishlist(uid: String, productId: String, currentlyInWishlist: Boolean): Result<Boolean> {
    val cleanId = productId.trim()
    if (cleanId.isEmpty()) return Result.failure(IllegalArgumentException("معرّف المنتج غير صالح."))
    val docRef = wishlistCollection(uid).document(cleanId)
    return try {
      if (currentlyInWishlist) {
        docRef.delete().await()
        Result.success(false)
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "productId" to cleanId,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
        Result.success(true)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (currentlyInWishlist) OperationType.DELETE else OperationType.CREATE,
        docRef.path,
      )
      Result.failure(e)
    }
  }
}
