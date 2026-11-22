package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.Category
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

class CategoryRepository(
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
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا لتنفيذ إجراءات إدارة الأقسام.")
  }

  fun observeActiveCategories(): Flow<List<Category>> {
    val query = db.collection("categories").whereEqualTo("isActive", true)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<Category> { it.displayOrder }.thenBy { it.name })
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.LIST, "categories?isActive=true")
        throw error
      }
  }

  fun observeAllCategories(): Flow<List<Category>> {
    val uid = requireUserId()
    val colRef = db.collection("categories")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<Category> { it.displayOrder }.thenBy { it.name })
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveCategory(category: Category): Result<String> {
    requireUserId()
    val colRef = db.collection("categories")
    val isNew = category.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(category.id)

    val cleanName = category.name.trim()
    val cleanDescription = category.description?.trim()?.takeIf { it.isNotEmpty() }
    val cleanParentId = category.parentId?.trim()?.takeIf { it.isNotEmpty() }
    val cleanImageUrl = category.imageUrl?.trim()?.takeIf { it.isNotEmpty() }

    return try {
      if (isNew) {
        val createPayload = buildMap<String, Any> {
          put("name", cleanName)
          if (cleanDescription != null) put("description", cleanDescription)
          if (cleanParentId != null) put("parentId", cleanParentId)
          if (cleanImageUrl != null) put("imageUrl", cleanImageUrl)
          put("displayOrder", category.displayOrder.coerceAtLeast(0))
          put("isActive", category.isActive)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(createPayload).await()
      } else {
        val updatePayload = buildMap<String, Any> {
          put("name", cleanName)
          put("description", cleanDescription ?: FieldValue.delete())
          put("parentId", cleanParentId ?: FieldValue.delete())
          put("imageUrl", cleanImageUrl ?: FieldValue.delete())
          put("displayOrder", category.displayOrder.coerceAtLeast(0))
          put("isActive", category.isActive)
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updatePayload).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isNew) OperationType.CREATE else OperationType.UPDATE,
        docRef.path,
      )
      Result.failure(e)
    }
  }

  suspend fun setCategoryActive(categoryId: String, isActive: Boolean): Result<Unit> {
    requireUserId()
    val docRef = db.collection("categories").document(categoryId)
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

  suspend fun deleteCategory(categoryId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("categories").document(categoryId)
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
