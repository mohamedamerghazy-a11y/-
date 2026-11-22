package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.Product
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

class ProductRepository(
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
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا لتنفيذ إجراءات إدارة المنتجات.")
  }

  fun observeActiveProducts(): Flow<List<Product>> {
    val query = db.collection("products").whereEqualTo("isActive", true)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.LIST, "products?isActive=true")
        throw error
      }
  }

  fun observeAllProducts(): Flow<List<Product>> {
    val uid = requireUserId()
    val colRef = db.collection("products")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  fun observeProductById(productId: String): Flow<Product?> {
    val docRef = db.collection("products").document(productId)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  suspend fun getProductById(productId: String): Result<Product?> {
    val docRef = db.collection("products").document(productId)
    return try {
      val snapshot = docRef.get().await()
      val product = if (snapshot.exists()) {
        snapshot.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
      Result.success(product)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveProduct(product: Product): Result<String> {
    requireUserId()
    val colRef = db.collection("products")
    val isNew = product.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(product.id)

    val cleanSubCategoryId = product.subCategoryId?.trim()?.takeIf { it.isNotEmpty() }
    val cleanBarcode = product.barcode?.trim()?.takeIf { it.isNotEmpty() }
    val cleanQrCode = product.qrCode?.trim()?.takeIf { it.isNotEmpty() }
    val cleanBrandShowcaseId = product.brandShowcaseId?.trim()?.takeIf { it.isNotEmpty() }
    val cleanCompanyId = product.companyId?.trim()?.takeIf { it.isNotEmpty() }
    val cleanBrandCategoryId = product.brandCategoryId?.trim()?.takeIf {
      it.isNotEmpty() && cleanBrandShowcaseId != null
    }
    val isExclusiveBrand = product.exclusiveToBrandShowcase &&
      (cleanCompanyId != null || cleanBrandShowcaseId != null)
    val validDiscount = product.retailDiscountPrice?.takeIf { it > 0.0 && it < product.retailPrice }
    val cleanColors = product.availableColors.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)
    val cleanSizes = product.availableSizes.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)

    return try {
      if (isNew) {
        val createPayload = buildMap<String, Any> {
          put("name", product.name.trim())
          put("description", product.description.trim())
          put("categoryId", product.categoryId.trim())
          if (cleanSubCategoryId != null) put("subCategoryId", cleanSubCategoryId)
          put("sku", product.sku.trim())
          if (cleanBarcode != null) put("barcode", cleanBarcode)
          if (cleanQrCode != null) put("qrCode", cleanQrCode)
          put("imageUrls", product.imageUrls.take(10))
          put("availableColors", cleanColors)
          put("availableSizes", cleanSizes)
          put("retailPrice", product.retailPrice)
          if (validDiscount != null) put("retailDiscountPrice", validDiscount)
          put("wholesalePrice", product.wholesalePrice.coerceAtLeast(0.0))
          put("wholesaleMinQty", product.wholesaleMinQty.coerceAtLeast(1))
          put("unit", product.unit.trim().ifEmpty { "قطعة" })
          put("stockQuantity", product.stockQuantity.coerceAtLeast(0))
          put("lowStockThreshold", product.lowStockThreshold.coerceAtLeast(0))
          put("isFeatured", product.isFeatured)
          put("isOffer", product.isOffer)
          put("isActive", product.isActive)
          if (cleanCompanyId != null) put("companyId", cleanCompanyId)
          if (cleanBrandShowcaseId != null) put("brandShowcaseId", cleanBrandShowcaseId)
          if (cleanBrandCategoryId != null) put("brandCategoryId", cleanBrandCategoryId)
          put("exclusiveToBrandShowcase", isExclusiveBrand)
          put("salesCount", product.salesCount.coerceAtLeast(0L))
          put("averageRating", product.averageRating.coerceIn(0.0, 5.0))
          put("reviewCount", product.reviewCount.coerceAtLeast(0))
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(createPayload).await()
      } else {
        val updatePayload = buildMap<String, Any> {
          put("name", product.name.trim())
          put("description", product.description.trim())
          put("categoryId", product.categoryId.trim())
          put("subCategoryId", cleanSubCategoryId ?: FieldValue.delete())
          put("sku", product.sku.trim())
          put("barcode", cleanBarcode ?: FieldValue.delete())
          put("qrCode", cleanQrCode ?: FieldValue.delete())
          put("imageUrls", product.imageUrls.take(10))
          put("availableColors", cleanColors)
          put("availableSizes", cleanSizes)
          put("retailPrice", product.retailPrice)
          put("retailDiscountPrice", validDiscount ?: FieldValue.delete())
          put("wholesalePrice", product.wholesalePrice.coerceAtLeast(0.0))
          put("wholesaleMinQty", product.wholesaleMinQty.coerceAtLeast(1))
          put("unit", product.unit.trim().ifEmpty { "قطعة" })
          put("stockQuantity", product.stockQuantity.coerceAtLeast(0))
          put("lowStockThreshold", product.lowStockThreshold.coerceAtLeast(0))
          put("isFeatured", product.isFeatured)
          put("isOffer", product.isOffer)
          put("isActive", product.isActive)
          put("companyId", cleanCompanyId ?: FieldValue.delete())
          put("brandShowcaseId", cleanBrandShowcaseId ?: FieldValue.delete())
          put("brandCategoryId", cleanBrandCategoryId ?: FieldValue.delete())
          put("exclusiveToBrandShowcase", isExclusiveBrand)
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

  suspend fun updateStock(
    productId: String,
    stockQuantity: Int,
    lowStockThreshold: Int,
    isActive: Boolean,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("products").document(productId)
    return try {
      docRef.update(
        mapOf(
          "stockQuantity" to stockQuantity.coerceAtLeast(0),
          "lowStockThreshold" to lowStockThreshold.coerceAtLeast(0),
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

  suspend fun updateOfferSettings(
    productId: String,
    retailDiscountPrice: Double?,
    isOffer: Boolean,
    isFeatured: Boolean,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("products").document(productId)
    return try {
      docRef.update(
        mapOf(
          "retailDiscountPrice" to (retailDiscountPrice ?: FieldValue.delete()),
          "isOffer" to isOffer,
          "isFeatured" to isFeatured,
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

  suspend fun deleteProduct(productId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("products").document(productId)
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

  suspend fun updateStock(productId: String, newStock: Int): Result<Unit> {
    requireUserId()
    val docRef = db.collection("products").document(productId)
    return try {
      docRef.update(
        mapOf(
          "stockQuantity" to newStock.coerceAtLeast(0),
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
}
