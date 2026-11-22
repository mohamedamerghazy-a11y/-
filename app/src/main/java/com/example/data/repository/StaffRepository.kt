package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.example.data.model.StaffMember
import com.example.data.model.UserProfile
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

class StaffRepository(
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
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا للوصول إلى إدارة الموظفين.")
  }

  fun observeStaffMember(uid: String): Flow<StaffMember?> {
    val docRef = db.collection("staff").document(uid)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  fun observeAllStaff(): Flow<List<StaffMember>> {
    val uid = requireUserId()
    val colRef = db.collection("staff")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.fullName }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  fun observeAllUsers(): Flow<List<UserProfile>> {
    val uid = requireUserId()
    val colRef = db.collection("users")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.fullName }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun assignStaffMember(
    targetUser: UserProfile,
    role: StaffRole,
    customPermissions: Set<StaffPermission> = role.permissions,
    isActive: Boolean = true,
  ): Result<Unit> {
    requireUserId()
    require(role != StaffRole.CUSTOMER) { "استخدم removeStaffMember لإلغاء صلاحيات الموظف." }

    val staffRef = db.collection("staff").document(targetUser.uid)
    val userRef = db.collection("users").document(targetUser.uid)
    val permissionKeys = customPermissions.map { it.firestoreKey }

    return try {
      val existingStaff = staffRef.get().await()
      val batch = db.batch()
      if (existingStaff.exists()) {
        batch.update(
          staffRef,
          mapOf(
            "fullName" to targetUser.fullName.trim(),
            "email" to targetUser.email.trim(),
            "role" to role.firestoreKey,
            "permissions" to permissionKeys,
            "isActive" to isActive,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.set(
          staffRef,
          mapOf(
            "uid" to targetUser.uid,
            "fullName" to targetUser.fullName.trim(),
            "email" to targetUser.email.trim(),
            "role" to role.firestoreKey,
            "permissions" to permissionKeys,
            "isActive" to isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      batch.update(
        userRef,
        mapOf(
          "role" to if (isActive) role.firestoreKey else StaffRole.CUSTOMER.firestoreKey,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, staffRef.path)
      Result.failure(e)
    }
  }

  suspend fun removeStaffMember(targetUid: String): Result<Unit> {
    requireUserId()
    val staffRef = db.collection("staff").document(targetUid)
    val userRef = db.collection("users").document(targetUid)

    return try {
      val batch = db.batch()
      batch.delete(staffRef)
      batch.update(
        userRef,
        mapOf(
          "role" to StaffRole.CUSTOMER.firestoreKey,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, staffRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateWholesaleDecision(
    targetUid: String,
    approved: Boolean,
  ): Result<Unit> {
    requireUserId()
    val userRef = db.collection("users").document(targetUid)
    return try {
      userRef.update(
        mapOf(
          "accountType" to if (approved) "wholesale" else "retail",
          "wholesaleStatus" to if (approved) "approved" else "rejected",
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      Result.failure(e)
    }
  }
}
