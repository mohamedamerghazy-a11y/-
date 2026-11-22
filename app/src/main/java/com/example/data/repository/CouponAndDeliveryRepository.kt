package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.Coupon
import com.example.data.model.DeliveryZone
import com.example.data.model.StorePaymentSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CouponAndDeliveryRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  fun observeActiveDeliveryZones(): Flow<List<DeliveryZone>> {
    val query = db.collection("deliveryZones").whereEqualTo("isActive", true)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(DeliveryZone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<DeliveryZone> { it.governorate }.thenBy { it.name })
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.LIST, "deliveryZones?isActive=true")
        throw error
      }
  }

  fun observePaymentSettings(): Flow<StorePaymentSettings> {
    val docRef = db.collection("settings").document("payments")
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(StorePaymentSettings::class.java) ?: StorePaymentSettings()
        } else {
          StorePaymentSettings()
        }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser == null) throw error
        emit(StorePaymentSettings())
      }
  }

  suspend fun validateCoupon(rawCode: String, subtotal: Double): Result<Coupon> {
    val normalizedCode = rawCode.trim().uppercase()
    if (normalizedCode.length < 2) {
      return Result.failure(IllegalArgumentException("يرجى إدخال كود خصم صحيح."))
    }
    if (auth.currentUser == null) {
      return Result.failure(IllegalStateException("يرجى تسجيل الدخول لتفعيل كود الخصم."))
    }

    val query = db.collection("coupons").whereEqualTo("isActive", true)
    return try {
      val snapshot = query.get().await()
      val matched = snapshot.toObjects(Coupon::class.java)
        .firstOrNull { it.code.trim().uppercase() == normalizedCode }
        ?: return Result.failure(IllegalArgumentException("كود الخصم غير موجود أو غير مفعّل حاليًا."))

      val nowMillis = System.currentTimeMillis()
      val expiryMillis = matched.expiresAt?.toDate()?.time
      if (expiryMillis != null && expiryMillis < nowMillis) {
        return Result.failure(IllegalArgumentException("انتهت صلاحية كود الخصم."))
      }
      val limit = matched.usageLimit
      if (limit != null && matched.usedCount >= limit) {
        return Result.failure(IllegalArgumentException("تم استهلاك الحد الأقصى لاستخدام هذا الكود."))
      }
      if (subtotal < matched.minOrderAmount) {
        return Result.failure(
          IllegalArgumentException(
            "الحد الأدنى لتطبيق هذا الكود هو ${matched.minOrderAmount.toInt()} ج.م.",
          ),
        )
      }
      Result.success(matched)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "coupons")
      Result.failure(IllegalStateException("تعذر التحقق من كود الخصم حاليًا."))
    }
  }

  fun observeAllCoupons(): Flow<List<Coupon>> {
    val colRef = db.collection("coupons")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser == null) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveCoupon(coupon: Coupon): Result<Unit> {
    val colRef = db.collection("coupons")
    val isNew = coupon.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(coupon.id)

    val cleanCode = coupon.code.trim().uppercase()
    val cleanType = if (coupon.discountType == "percent") "percent" else "fixed"
    val cleanVal = coupon.discountValue.coerceAtLeast(0.0)
    val cleanMin = coupon.minOrderAmount.coerceAtLeast(0.0)

    val payload = mutableMapOf<String, Any>(
      "code" to cleanCode,
      "discountType" to cleanType,
      "discountValue" to cleanVal,
      "minOrderAmount" to cleanMin,
      "usedCount" to coupon.usedCount,
      "isActive" to coupon.isActive,
      "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
    )
    if (isNew) {
      payload["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
    }
    if (coupon.maxDiscountAmount != null && coupon.maxDiscountAmount > 0.0) {
      payload["maxDiscountAmount"] = coupon.maxDiscountAmount
    }
    if (coupon.usageLimit != null && coupon.usageLimit > 0) {
      payload["usageLimit"] = coupon.usageLimit
    }
    if (coupon.expiresAt != null) {
      payload["expiresAt"] = coupon.expiresAt
    }

    return try {
      if (isNew) {
        docRef.set(payload).await()
      } else {
        docRef.update(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteCoupon(couponId: String): Result<Unit> {
    val docRef = db.collection("coupons").document(couponId)
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

  fun observeAllDeliveryZones(): Flow<List<DeliveryZone>> {
    val colRef = db.collection("deliveryZones")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(DeliveryZone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<DeliveryZone> { it.governorate }.thenBy { it.name })
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser == null) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveDeliveryZone(zone: DeliveryZone): Result<Unit> {
    val colRef = db.collection("deliveryZones")
    val isNew = zone.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(zone.id)

    val payload = mutableMapOf<String, Any>(
      "name" to zone.name.trim(),
      "governorate" to zone.governorate.trim(),
      "fee" to zone.fee.coerceAtLeast(0.0),
      "estimatedDuration" to zone.estimatedDuration.trim().ifEmpty { "خلال ٢٤ - ٤٨ ساعة" },
      "isActive" to zone.isActive,
      "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
    )
    if (isNew) {
      payload["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
    }

    return try {
      if (isNew) {
        docRef.set(payload).await()
      } else {
        docRef.update(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteDeliveryZone(zoneId: String): Result<Unit> {
    val docRef = db.collection("deliveryZones").document(zoneId)
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

  suspend fun savePaymentSettings(settings: StorePaymentSettings): Result<Unit> {
    val docRef = db.collection("settings").document("payments")
    val payload = mapOf(
      "vodafoneCashNumber" to settings.vodafoneCashNumber.trim(),
      "instaPayAddress" to settings.instaPayAddress.trim(),
      "paymentInstructions" to settings.paymentInstructions.trim(),
      "codEnabled" to settings.codEnabled,
      "instaPayEnabled" to settings.instaPayEnabled,
      "vodafoneCashEnabled" to settings.vodafoneCashEnabled,
      "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }
}
