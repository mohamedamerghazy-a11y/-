package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.ProductReview
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

class ReviewRepository(
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
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا لإضافة تقييم أو مراجعة.")
  }

  fun observeVisibleProductReviews(productId: String): Flow<List<ProductReview>> {
    val query = db.collection("reviews")
      .whereEqualTo("productId", productId)
      .whereEqualTo("isHidden", false)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(ProductReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.LIST, "reviews?productId=$productId")
        throw error
      }
  }

  fun observeAllReviews(): Flow<List<ProductReview>> {
    val uid = requireUserId()
    val colRef = db.collection("reviews")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(ProductReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun submitOrUpdateReview(
    productId: String,
    productName: String,
    userName: String,
    rating: Int,
    comment: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val reviewId = "${productId.trim()}_$uid"
    val docRef = db.collection("reviews").document(reviewId)
    val safeRating = rating.coerceIn(1, 5)
    val cleanUserName = userName.trim().ifEmpty { "عميل المتجر" }.take(100)
    val cleanProductName = productName.trim().ifEmpty { "منتج" }.take(140)
    val cleanComment = comment.trim().take(1000)

    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "userName" to cleanUserName,
            "rating" to safeRating,
            "comment" to cleanComment,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      } else {
        docRef.set(
          mapOf(
            "productId" to productId.trim(),
            "productName" to cleanProductName,
            "userId" to uid,
            "userName" to cleanUserName,
            "rating" to safeRating,
            "comment" to cleanComment,
            "isHidden" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun setReviewHidden(reviewId: String, hidden: Boolean): Result<Unit> {
    requireUserId()
    val docRef = db.collection("reviews").document(reviewId)
    return try {
      docRef.update(
        mapOf(
          "isHidden" to hidden,
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

  suspend fun deleteReview(reviewId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("reviews").document(reviewId)
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
