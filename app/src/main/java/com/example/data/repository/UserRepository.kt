package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.DeliveryAddress
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا قبل الوصول إلى بيانات الحساب.")
  }

  fun observeUserProfile(uid: String): Flow<UserProfile?> {
    val docRef = db.collection("users").document(uid)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

  suspend fun getUserProfile(uid: String): Result<UserProfile?> {
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      val profile = if (snapshot.exists()) {
        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createUserProfile(
    fullName: String,
    email: String,
    phone: String,
    photoUrl: String?,
    requestWholesale: Boolean,
    companyName: String?,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val cleanName = fullName.trim()
    val cleanEmail = email.trim()
    val cleanPhone = phone.trim()
    val cleanCompany = companyName?.trim()?.takeIf { it.isNotEmpty() }
    val cleanPhoto = photoUrl?.trim()?.takeIf { it.isNotEmpty() }
    var configuredOwnerEmail: String? = null
    if (!cleanEmail.equals(BOOTSTRAPPED_OWNER_EMAIL, ignoreCase = true)) {
      configuredOwnerEmail = runCatching {
        db.collection("settings").document("store").get().await().getString("ownerEmail")?.trim()
      }.getOrNull()
    }
    val assignedRole = if (
      cleanEmail.equals(BOOTSTRAPPED_OWNER_EMAIL, ignoreCase = true) ||
      (!configuredOwnerEmail.isNullOrBlank() && cleanEmail.equals(configuredOwnerEmail, ignoreCase = true))
    ) {
      "owner"
    } else {
      "customer"
    }

    val payload = buildMap<String, Any> {
      put("uid", uid)
      put("fullName", cleanName)
      put("email", cleanEmail)
      if (cleanPhone.isNotEmpty()) put("phone", cleanPhone)
      if (cleanPhoto != null) put("photoUrl", cleanPhoto)
      put("accountType", if (requestWholesale) "wholesale" else "retail")
      put("wholesaleStatus", if (requestWholesale) "pending" else "none")
      if (cleanCompany != null) put("wholesaleCompanyName", cleanCompany)
      put("role", assignedRole)
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateProfileDetails(
    fullName: String,
    phone: String,
    companyName: String?,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val updates = buildMap<String, Any> {
      put("fullName", fullName.trim())
      put("phone", phone.trim())
      val cleanCompany = companyName?.trim()
      if (!cleanCompany.isNullOrEmpty()) {
        put("wholesaleCompanyName", cleanCompany)
      }
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun submitWholesaleRequest(
    fullName: String,
    phone: String,
    companyName: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val updates = mapOf(
      "fullName" to fullName.trim(),
      "phone" to phone.trim(),
      "wholesaleCompanyName" to companyName.trim(),
      "accountType" to "wholesale",
      "wholesaleStatus" to "pending",
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeAddresses(uid: String): Flow<List<DeliveryAddress>> {
    val colRef = db.collection("users").document(uid).collection("addresses")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          DeliveryAddress::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ).sortedByDescending { it.isDefault }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveAddress(address: DeliveryAddress): Result<String> {
    val uid = requireUserId()
    val colRef = db.collection("users").document(uid).collection("addresses")
    val isNew = address.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(address.id)

    return try {
      val batch = db.batch()
      if (address.isDefault) {
        val existingSnapshots = colRef.get().await()
        existingSnapshots.documents.forEach { existingDoc ->
          if (existingDoc.id != docRef.id && existingDoc.getBoolean("isDefault") == true) {
            batch.update(
              existingDoc.reference,
              mapOf(
                "isDefault" to false,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
        }
      }

      val cleanLandmark = address.landmark?.trim()?.takeIf { it.isNotEmpty() }
      val lat = address.latitude
      val lng = address.longitude
      if (isNew) {
        val createPayload = buildMap<String, Any> {
          put("userId", uid)
          put("label", address.label.trim())
          put("recipientName", address.recipientName.trim())
          put("phone", address.phone.trim())
          put("governorate", address.governorate.trim())
          put("city", address.city.trim())
          put("streetAddress", address.streetAddress.trim())
          if (cleanLandmark != null) put("landmark", cleanLandmark)
          if (lat != null && lng != null) {
            put("latitude", lat)
            put("longitude", lng)
          }
          put("isDefault", address.isDefault)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        batch.set(docRef, createPayload)
      } else {
        val updatePayload = buildMap<String, Any> {
          put("label", address.label.trim())
          put("recipientName", address.recipientName.trim())
          put("phone", address.phone.trim())
          put("governorate", address.governorate.trim())
          put("city", address.city.trim())
          put("streetAddress", address.streetAddress.trim())
          if (cleanLandmark != null) put("landmark", cleanLandmark)
          if (lat != null && lng != null) {
            put("latitude", lat)
            put("longitude", lng)
          } else {
            put("latitude", FieldValue.delete())
            put("longitude", FieldValue.delete())
          }
          put("isDefault", address.isDefault)
          put("updatedAt", FieldValue.serverTimestamp())
        }
        batch.update(docRef, updatePayload)
      }

      batch.commit().await()
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

  suspend fun deleteAddress(addressId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("addresses").document(addressId)
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

  fun isRecentLoginRequiredForDeletion(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val user = auth.currentUser ?: return false
    val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
    if (lastSignIn <= 0L) return true
    return (nowMillis - lastSignIn) > RECENT_LOGIN_MAX_AGE_MS
  }

  suspend fun deleteCurrentAccount(): DeleteAccountOutcome {
    val user = auth.currentUser
      ?: return DeleteAccountOutcome.Failure("لا يوجد حساب مسجل حاليًا.")
    if (isRecentLoginRequiredForDeletion()) {
      return DeleteAccountOutcome.RequiresRecentLogin
    }

    val uid = user.uid
    val userDocRef = db.collection("users").document(uid)
    return try {
      val refsToDelete = mutableListOf<DocumentReference>()
      listOf("addresses", "cart", "notifications", "wishlist").forEach { subCol ->
        val snap = userDocRef.collection(subCol).get().await()
        snap.documents.forEach { refsToDelete.add(it.reference) }
      }
      runCatching {
        val reviewSnap = db.collection("reviews").whereEqualTo("userId", uid).get().await()
        reviewSnap.documents.forEach { refsToDelete.add(it.reference) }
      }

      refsToDelete.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { batch.delete(it) }
        batch.commit().await()
      }

      if (userDocRef.get().await().exists()) {
        userDocRef.delete().await()
      }

      user.delete().await()
      DeleteAccountOutcome.Deleted
    } catch (e: CancellationException) {
      throw e
    } catch (_: FirebaseAuthRecentLoginRequiredException) {
      DeleteAccountOutcome.RequiresRecentLogin
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, userDocRef.path)
      DeleteAccountOutcome.Failure(
        e.toArabicErrorMessage("تعذر حذف الحساب حاليًا، يرجى المحاولة مجددًا."),
      )
    }
  }

  sealed interface DeleteAccountOutcome {
    data object Deleted : DeleteAccountOutcome
    data object RequiresRecentLogin : DeleteAccountOutcome
    data class Failure(val message: String) : DeleteAccountOutcome
  }

  companion object {
    const val BOOTSTRAPPED_OWNER_EMAIL = "mohamedamerghazy@gmail.com"
    private const val RECENT_LOGIN_MAX_AGE_MS = 4L * 60L * 1000L
  }
}
