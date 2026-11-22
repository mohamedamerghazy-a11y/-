package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.model.FulfillmentMethod
import com.example.core.model.OrderStatus
import com.example.core.model.PaymentMethod
import com.example.data.model.BrandShowcase
import com.example.data.model.CashVerificationLog
import com.example.data.model.Coupon
import com.example.data.model.DeliveryAddress
import com.example.data.model.DeliveryZone
import com.example.data.model.FlashSale
import com.example.data.model.FlashSaleStatus
import com.example.data.model.Order
import com.example.data.model.computeFreeShipping
import com.example.data.model.generateDeliveryOtp
import com.example.data.model.normalizeEgyptianPhone
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
import kotlin.math.roundToLong

data class CheckoutLineRequest(
  val productId: String,
  val quantity: Int,
  val flashSaleId: String? = null,
  val selectedColor: String? = null,
  val selectedSize: String? = null,
  val cartItemId: String? = null,
)

class OrderRepository(
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
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا لإتمام الطلب.")
  }

  fun observeUserOrders(uid: String): Flow<List<Order>> {
    val query = db.collection("orders").whereEqualTo("userId", uid)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, "orders?userId=$uid")
        throw error
      }
  }

  fun observeOrderById(orderId: String): Flow<Order?> {
    val docRef = db.collection("orders").document(orderId)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser == null) throw error
        handleFirestoreError(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  /**
   * إتمام الطلب عبر معاملة Firestore ذرية:
   * 1. التحقق من حالة حساب العميل (قطاعي / جملة معتمد) والمخزون الحالي لكل منتج.
   * 2. حفظ لقطة ثابتة للأسعار وبيانات العنوان في سجل الطلب.
   * 3. خصم المخزون وزيادة عدد المبيعات وتحديث عداد الكوبون (إن وُجد).
   * 4. تفريغ عربة التسوق وتحديث آخر معرّف طلب على مستند المستخدم للربط الأمني.
   */
  suspend fun placeOrder(
    lines: List<CheckoutLineRequest>,
    fulfillmentMethod: FulfillmentMethod = FulfillmentMethod.DELIVERY,
    address: DeliveryAddress?,
    deliveryZone: DeliveryZone?,
    couponId: String?,
    paymentMethod: PaymentMethod,
    transferReference: String?,
    senderPhone: String? = null,
    receiptImageUrl: String? = null,
    notes: String?,
    pickupRecipientName: String? = null,
    pickupPhone: String? = null,
    storeAddress: String = "",
    cartItemIdsToClear: List<String> = lines.map { it.cartItemId ?: it.productId },
  ): Result<String> {
    val uid = requireUserId()
    val validLines = lines.filter { it.productId.isNotBlank() && it.quantity > 0 }
    if (validLines.isEmpty()) {
      return Result.failure(IllegalArgumentException("عربة التسوق فارغة، أضف منتجًا واحدًا على الأقل."))
    }
    val isPickup = fulfillmentMethod == FulfillmentMethod.STORE_PICKUP
    if (!isPickup && address == null) {
      return Result.failure(IllegalArgumentException("يرجى اختيار عنوان التوصيل أو اختيار الاستلام من المتجر."))
    }

    val orderRef = db.collection("orders").document()
    val userRef = db.collection("users").document(uid)
    val counterRef = db.collection("counters").document("orders")
    val activeShowcaseIds = try {
      db.collection("brandShowcases")
        .whereEqualTo("isActive", true)
        .get()
        .await()
        .documents
        .map { it.id }
    } catch (_: Exception) {
      emptyList()
    }

    return try {
      val createdOrderId = db.runTransaction { tx ->
        // --- مرحلة القراءة الكاملة قبل أي تعديل ---
        val userSnap = tx.get(userRef)
        if (!userSnap.exists()) {
          throw IllegalStateException("تعذر العثور على ملف حسابك الشخصي.")
        }

        val counterSnap = tx.get(counterRef)
        val assignedOrderSeq = if (counterSnap.exists()) {
          (counterSnap.getLong("nextNumber") ?: STARTING_ORDER_NUMBER).coerceAtLeast(STARTING_ORDER_NUMBER)
        } else {
          STARTING_ORDER_NUMBER
        }
        val orderNumber = formatSequentialOrderNumber(assignedOrderSeq)

        val productSnaps = validLines.map { item ->
          item to tx.get(db.collection("products").document(item.productId))
        }

        val couponSnap = couponId?.trim()?.takeIf { it.isNotEmpty() }?.let { cid ->
          tx.get(db.collection("coupons").document(cid))
        }

        val flashSaleIds = validLines.mapNotNull {
          it.flashSaleId?.trim()?.takeIf { fid -> fid.isNotEmpty() }
        }.distinct()
        val flashSaleSnaps = flashSaleIds.associateWith { fid ->
          tx.get(db.collection("flashSales").document(fid))
        }

        val showcaseSnaps = activeShowcaseIds.map { sid ->
          tx.get(db.collection("brandShowcases").document(sid))
        }

        val storeSettingsSnap = tx.get(db.collection("settings").document("store"))

        // --- مرحلة التحقق والحسابات الدقيقة ---
        val accountType = userSnap.getString("accountType").orEmpty()
        val wholesaleStatus = userSnap.getString("wholesaleStatus").orEmpty()
        val isWholesaleApproved = accountType == "wholesale" && wholesaleStatus == "approved"
        val accountTypeAtOrder = if (isWholesaleApproved) "wholesale" else "retail"

        val customerName = userSnap.getString("fullName")?.trim()?.takeIf { it.length >= 2 }
          ?: pickupRecipientName?.trim()?.takeIf { it.length >= 2 }
          ?: address?.recipientName?.trim()?.takeIf { it.length >= 2 }
          ?: "عميل المتجر"
        val rawEmail = userSnap.getString("email")?.trim().orEmpty()
        val customerEmail = if (rawEmail.length >= 3) {
          rawEmail
        } else {
          auth.currentUser?.email?.trim()?.takeIf { it.length >= 3 } ?: "customer@aleman.store"
        }

        val itemMaps = mutableListOf<Map<String, Any>>()
        val stockUpdates = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
        var subtotal = 0.0

        val nowMillis = System.currentTimeMillis()
        val activeFlashSalesById = flashSaleSnaps.mapNotNull { (fid, snap) ->
          if (!snap.exists()) return@mapNotNull null
          val sale = snap.toObject(FlashSale::class.java) ?: return@mapNotNull null
          if (!sale.isActive || sale.statusAt(nowMillis) != FlashSaleStatus.ACTIVE) return@mapNotNull null
          fid to sale
        }.toMap()

        val activeShowcases = showcaseSnaps.mapNotNull { snap ->
          if (!snap.exists()) return@mapNotNull null
          snap.toObject(BrandShowcase::class.java)?.takeIf { it.isActive }
        }

        productSnaps.forEach { (req, prodSnap) ->
          if (!prodSnap.exists()) {
            throw IllegalStateException("أحد المنتجات لم يعد متوفرًا في المتجر.")
          }
          val isActive = prodSnap.getBoolean("isActive") ?: false
          val name = prodSnap.getString("name")?.trim().orEmpty().ifEmpty { "منتج" }
          if (!isActive) {
            throw IllegalStateException("المنتج «$name» غير متاح حاليًا للطلب.")
          }
          val currentStock = (prodSnap.getLong("stockQuantity") ?: 0L).toInt()
          if (currentStock < req.quantity) {
            throw IllegalStateException("الكمية المطلوبة من «$name» غير متوفرة (المتاح: $currentStock).")
          }

          val retailPrice = prodSnap.getDouble("retailPrice") ?: 0.0
          val retailDiscount = prodSnap.getDouble("retailDiscountPrice")
          val effectiveRetail = if (retailDiscount != null && retailDiscount > 0.0 && retailDiscount < retailPrice) {
            retailDiscount
          } else {
            retailPrice
          }
          val prodCompanyId = prodSnap.getString("companyId")?.trim()?.takeIf { it.isNotEmpty() }
          val prodShowcaseId = prodSnap.getString("brandShowcaseId")?.trim()?.takeIf { it.isNotEmpty() }
          val maxShowcaseDiscountPct = activeShowcases
            .filter { showcase ->
              showcase.cleanDiscountPercent > 0 && (
                (!showcase.companyId.isNullOrBlank() && showcase.companyId == prodCompanyId) ||
                  showcase.id == prodShowcaseId
                )
            }
            .maxOfOrNull { it.cleanDiscountPercent } ?: 0
          val showcaseRetailPrice = if (maxShowcaseDiscountPct > 0 && retailPrice > 0.0) {
            roundTwoDecimals(retailPrice * (1.0 - maxShowcaseDiscountPct / 100.0)).coerceAtLeast(0.01)
          } else {
            effectiveRetail
          }
          val finalEffectiveRetail = minOf(effectiveRetail, showcaseRetailPrice)
          val wholesalePrice = prodSnap.getDouble("wholesalePrice") ?: 0.0
          val wholesaleMinQty = (prodSnap.getLong("wholesaleMinQty") ?: 1L).toInt().coerceAtLeast(1)

          val flashSaleId = req.flashSaleId?.trim()?.takeIf { it.isNotEmpty() }
          val activeFlashSale = flashSaleId?.let { activeFlashSalesById[it] }
          val flashItem = activeFlashSale?.itemFor(prodSnap.id)
          val maxFlashQty = flashItem?.maxQtyPerOrder
          if (flashItem != null && maxFlashQty != null && maxFlashQty > 0 && req.quantity > maxFlashQty) {
            throw IllegalStateException("الحد الأقصى للطلب من «$name» في الفلاش سيل هو $maxFlashQty.")
          }

          val flashPrice = if (flashItem != null) {
            activeFlashSale.flashUnitPrice(prodSnap.id, isWholesaleApproved)
          } else {
            null
          }

          val useWholesale = flashPrice == null &&
            isWholesaleApproved && wholesalePrice > 0.0 && req.quantity >= wholesaleMinQty
          val unitPrice = roundTwoDecimals(flashPrice ?: if (useWholesale) wholesalePrice else finalEffectiveRetail)
          val pricingMode = when {
            flashPrice != null -> "flash_sale"
            useWholesale -> "wholesale"
            else -> "retail"
          }
          val lineTotal = roundTwoDecimals(unitPrice * req.quantity)
          subtotal = roundTwoDecimals(subtotal + lineTotal)

          val sku = prodSnap.getString("sku")?.trim().orEmpty().ifEmpty { "SKU" }
          val unit = prodSnap.getString("unit")?.trim().orEmpty().ifEmpty { "قطعة" }
          @Suppress("UNCHECKED_CAST")
          val firstImage = (prodSnap.get("imageUrls") as? List<String>)?.firstOrNull()

          val cleanColor = req.selectedColor?.trim()?.takeIf { it.isNotEmpty() }
          val cleanSize = req.selectedSize?.trim()?.takeIf { it.isNotEmpty() }
          itemMaps.add(
            buildMap {
              put("productId", prodSnap.id)
              put("productName", name)
              put("sku", sku)
              if (!firstImage.isNullOrBlank()) put("imageUrl", firstImage)
              put("unit", unit)
              put("pricingMode", pricingMode)
              if (flashPrice != null && activeFlashSale != null) {
                put("flashSaleId", activeFlashSale.id)
              }
              if (cleanColor != null) put("selectedColor", cleanColor)
              if (cleanSize != null) put("selectedSize", cleanSize)
              put("unitPrice", unitPrice)
              put("quantity", req.quantity)
              put("lineTotal", lineTotal)
            },
          )

          val oldSales = prodSnap.getLong("salesCount") ?: 0L
          stockUpdates.add(
            prodSnap.reference to mapOf(
              "stockQuantity" to (currentStock - req.quantity),
              "salesCount" to (oldSales + req.quantity),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }

        var appliedCouponCode: String? = null
        var discountAmount = 0.0
        var shouldIncrementCoupon = false
        var couponNextUsedCount = 0

        if (couponSnap != null && couponSnap.exists()) {
          val couponObj = couponSnap.toObject(Coupon::class.java)
          if (couponObj != null && couponObj.isActive) {
            val notExpired = couponObj.expiresAt?.toDate()?.time?.let { it >= System.currentTimeMillis() } ?: true
            val withinLimit = couponObj.usageLimit?.let { couponObj.usedCount < it } ?: true
            if (notExpired && withinLimit && subtotal >= couponObj.minOrderAmount) {
              discountAmount = couponObj.calculateDiscount(subtotal)
              if (discountAmount > 0.0) {
                appliedCouponCode = couponObj.code.trim().uppercase()
                shouldIncrementCoupon = true
                couponNextUsedCount = couponObj.usedCount + 1
              }
            }
          }
        }

        val freeShippingEnabled = if (storeSettingsSnap.exists()) {
          storeSettingsSnap.getBoolean("freeShippingEnabled") ?: false
        } else {
          false
        }
        val freeShippingThreshold = if (storeSettingsSnap.exists()) {
          (storeSettingsSnap.getDouble("freeShippingThreshold")
            ?: storeSettingsSnap.getLong("freeShippingThreshold")?.toDouble()
            ?: 0.0).coerceAtLeast(0.0)
        } else {
          0.0
        }
        val shippingEval = computeFreeShipping(
          netMerchandise = (subtotal - discountAmount).coerceAtLeast(0.0),
          isStorePickup = isPickup,
          hasItems = itemMaps.isNotEmpty(),
          baseZoneFee = deliveryZone?.fee ?: 0.0,
          enabled = freeShippingEnabled,
          threshold = freeShippingThreshold,
        )
        val deliveryFee = roundTwoDecimals(shippingEval.deliveryFee)
        val totalAmount = roundTwoDecimals((subtotal - discountAmount + deliveryFee).coerceAtLeast(0.0))
        val deliveryOtpEnabled = if (storeSettingsSnap.exists()) {
          storeSettingsSnap.getBoolean("deliveryOtpEnabled") ?: true
        } else {
          true
        }
        val deliveryOtp = if (!isPickup && deliveryOtpEnabled) generateDeliveryOtp() else null

        val rawSenderInput = senderPhone?.trim()?.takeIf { it.isNotEmpty() }
          ?: transferReference?.trim()?.takeIf { it.isNotEmpty() }
        val cleanSenderPhone = rawSenderInput?.let { input ->
          val norm = normalizeEgyptianPhone(input)
          if (norm.length >= 10) norm else input
        }
        val cleanTransferRef = transferReference?.trim()?.takeIf { it.isNotEmpty() } ?: cleanSenderPhone
        val cleanReceiptUrl = receiptImageUrl?.trim()?.takeIf { it.isNotEmpty() }
        val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
        val cleanLandmark = if (isPickup) null else address?.landmark?.trim()?.takeIf { it.isNotEmpty() }

        val resolvedRecipientName = if (isPickup) {
          pickupRecipientName?.trim()?.takeIf { it.length >= 2 } ?: customerName
        } else {
          address?.recipientName?.trim()?.takeIf { it.length >= 2 } ?: customerName
        }
        val resolvedPhone = if (isPickup) {
          pickupPhone?.trim()?.takeIf { it.length >= 6 }
            ?: userSnap.getString("phone")?.trim()?.takeIf { it.length >= 6 }
            ?: "01000000000"
        } else {
          address?.phone?.trim().orEmpty()
        }
        val resolvedPickupStreet = storeAddress.trim().takeIf { it.length >= 4 }
          ?: "الاستلام مباشرة من مقر المتجر الرئيسي"

        val orderPayload = buildMap<String, Any> {
          put("userId", uid)
          put("orderNumber", orderNumber)
          put("customerName", customerName)
          put("customerEmail", customerEmail)
          put("accountTypeAtOrder", accountTypeAtOrder)
          put("items", itemMaps)
          put("subtotal", subtotal)
          put("discountAmount", discountAmount)
          if (!appliedCouponCode.isNullOrBlank()) put("couponCode", appliedCouponCode)
          if (!isPickup && deliveryZone != null && deliveryZone.id.isNotBlank()) {
            put("deliveryZoneId", deliveryZone.id)
            put("deliveryZoneName", deliveryZone.name.trim())
          }
          put("deliveryFee", deliveryFee)
          put("totalAmount", totalAmount)
          put("paymentMethod", paymentMethod.code)
          put("paymentStatus", "pending_review")
          if (cleanTransferRef != null) put("transferReference", cleanTransferRef)
          if (cleanSenderPhone != null) put("senderPhone", cleanSenderPhone)
          if (cleanReceiptUrl != null) put("receiptImageUrl", cleanReceiptUrl)
          put("orderStatus", OrderStatus.NEW_ORDER.code)
          put("fulfillmentType", fulfillmentMethod.code)
          if (deliveryOtp != null) {
            put("deliveryOtp", deliveryOtp)
            put("deliveryOtpVerified", false)
          }
          put("deliveryLabel", if (isPickup) "استلام من المتجر" else address?.label?.trim()?.ifEmpty { "العنوان" } ?: "العنوان")
          put("recipientName", resolvedRecipientName)
          put("phone", resolvedPhone)
          put("governorate", if (isPickup) "المتجر" else address?.governorate?.trim().orEmpty())
          put("city", if (isPickup) "الفرع الرئيسي" else address?.city?.trim().orEmpty())
          put("streetAddress", if (isPickup) resolvedPickupStreet else address?.streetAddress?.trim().orEmpty())
          if (cleanLandmark != null) put("landmark", cleanLandmark)
          if (!isPickup && address?.latitude != null && address.longitude != null) {
            put("latitude", address.latitude)
            put("longitude", address.longitude)
          }
          if (cleanNotes != null) put("notes", cleanNotes)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }

        // --- مرحلة الكتابة الذرية ---
        tx.set(orderRef, orderPayload)
        if (counterSnap.exists()) {
          tx.update(
            counterRef,
            mapOf(
              "nextNumber" to (assignedOrderSeq + 1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          tx.set(
            counterRef,
            mapOf(
              "nextNumber" to (assignedOrderSeq + 1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        tx.update(
          userRef,
          mapOf(
            "lastOrderId" to orderRef.id,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        stockUpdates.forEach { (ref, payload) ->
          tx.update(ref, payload)
        }
        if (shouldIncrementCoupon && couponSnap != null) {
          tx.update(
            couponSnap.reference,
            mapOf(
              "usedCount" to couponNextUsedCount,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        cartItemIdsToClear
          .map { it.trim() }
          .filter { it.isNotEmpty() }
          .distinct()
          .forEach { cartDocId ->
            tx.delete(db.collection("users").document(uid).collection("cart").document(cartDocId))
          }

        orderRef.id
      }.await()

      Result.success(createdOrderId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, orderRef.path)
      Result.failure(e)
    }
  }

  suspend fun cancelOrderByCustomer(orderId: String, reason: String? = null): Result<Unit> {
    requireUserId()
    val docRef = db.collection("orders").document(orderId)
    val note = reason?.trim()?.takeIf { it.isNotEmpty() } ?: "تم الإلغاء بناءً على طلب العميل"
    return try {
      docRef.update(
        mapOf(
          "orderStatus" to OrderStatus.CANCELLED.code,
          "statusNote" to note,
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

  fun observeAllOrders(): Flow<List<Order>> {
    val uid = requireUserId()
    val colRef = db.collection("orders")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  fun observeCashVerificationLogs(): Flow<List<CashVerificationLog>> {
    val uid = requireUserId()
    val colRef = db.collection("cashVerificationLogs")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(CashVerificationLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
          .take(100)
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun updateOrderStatus(
    orderId: String,
    newStatus: OrderStatus,
    statusNote: String? = null,
    paymentStatus: String? = null,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("orders").document(orderId)
    return try {
      val updates = mutableMapOf<String, Any>(
        "orderStatus" to newStatus.code,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (statusNote != null) {
        updates["statusNote"] = statusNote.trim()
      }
      if (paymentStatus != null) {
        updates["paymentStatus"] = paymentStatus.trim()
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteOrderReceiptImage(orderId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("orders").document(orderId)
    return try {
      docRef.update(
        mapOf(
          "receiptImageUrl" to FieldValue.delete(),
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

  suspend fun deleteOrderReceiptsBatch(orderIds: List<String>): Result<Int> {
    requireUserId()
    val cleanIds = orderIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleanIds.isEmpty()) return Result.success(0)

    return try {
      var deletedCount = 0
      cleanIds.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { orderId ->
          val docRef = db.collection("orders").document(orderId)
          batch.update(
            docRef,
            mapOf(
              "receiptImageUrl" to FieldValue.delete(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().await()
        deletedCount += chunk.size
      }
      Result.success(deletedCount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "orders/batch_delete_receipts")
      Result.failure(e)
    }
  }

  fun observeDriverOrders(driverUid: String): Flow<List<Order>> {
    val query = db.collection("orders").whereEqualTo("assignedDriverId", driverUid)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.assignedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != driverUid) throw error
        handleFirestoreError(error, OperationType.LIST, "orders?assignedDriverId=$driverUid")
        throw error
      }
  }

  suspend fun assignOrderToDriver(
    orderId: String,
    driverId: String,
    driverName: String,
    driverPhone: String?,
  ): Result<Unit> {
    requireUserId()
    val cleanDriverId = driverId.trim()
    val cleanDriverName = driverName.trim().ifEmpty { "مندوب التوصيل" }
    val cleanDriverPhone = driverPhone?.trim()?.takeIf { it.isNotEmpty() }
    val docRef = db.collection("orders").document(orderId)
    return try {
      val updates = buildMap<String, Any> {
        put("assignedDriverId", cleanDriverId)
        put("assignedDriverName", cleanDriverName)
        if (cleanDriverPhone != null) {
          put("assignedDriverPhone", cleanDriverPhone)
        } else {
          put("assignedDriverPhone", FieldValue.delete())
        }
        put("assignedAt", FieldValue.serverTimestamp())
        put("orderStatus", OrderStatus.OUT_FOR_DELIVERY.code)
        put("statusNote", "تم توجيه الطلب للتوصيل مع المندوب: $cleanDriverName")
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun markOrderDeliveredByDriver(
    orderId: String,
    note: String? = null,
    deliveryOtpVerified: Boolean? = null,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("orders").document(orderId)
    val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() } ?: "تم تسليم الطلب للعميل بواسطة موظف التوصيل"
    return try {
      val updates = buildMap<String, Any> {
        put("orderStatus", OrderStatus.DELIVERED.code)
        put("statusNote", cleanNote)
        if (deliveryOtpVerified != null) {
          put("deliveryOtpVerified", deliveryOtpVerified)
        }
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  private fun roundTwoDecimals(value: Double): Double {
    return (value * 100.0).roundToLong() / 100.0
  }

  companion object {
    const val STARTING_ORDER_NUMBER: Long = 1000L

    fun formatSequentialOrderNumber(sequenceNumber: Long): String {
      return "ET-${sequenceNumber.coerceAtLeast(STARTING_ORDER_NUMBER)}"
    }
  }
}
