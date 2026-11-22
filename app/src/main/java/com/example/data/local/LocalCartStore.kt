package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.guestCartDataStore by preferencesDataStore(name = "aleman_guest_cart_prefs")

@Serializable
data class LocalCartEntry(
  val productId: String,
  val quantity: Int,
  val flashSaleId: String? = null,
  val selectedColor: String? = null,
  val selectedSize: String? = null,
) {
  val cartItemId: String
    get() = com.example.data.model.buildCartItemId(productId, selectedColor, selectedSize)
}

class LocalCartStore(private val context: Context) {

  private val json = Json { ignoreUnknownKeys = true }

  val cartEntriesFlow: Flow<List<LocalCartEntry>> =
    context.applicationContext.guestCartDataStore.data.map { prefs ->
      decodeEntries(prefs[KEY_CART_ITEMS])
    }

  val couponCodeFlow: Flow<String?> =
    context.applicationContext.guestCartDataStore.data.map { prefs ->
      prefs[KEY_COUPON_CODE]?.takeIf { it.isNotBlank() }
    }

  suspend fun upsertItem(
    productId: String,
    quantity: Int,
    flashSaleId: String? = null,
    selectedColor: String? = null,
    selectedSize: String? = null,
    cartItemId: String? = null,
  ) {
    val cleanId = productId.trim()
    if (cleanId.isEmpty()) return
    val cleanColor = selectedColor?.trim()?.takeIf { it.isNotEmpty() }
    val cleanSize = selectedSize?.trim()?.takeIf { it.isNotEmpty() }
    val targetKey = cartItemId?.trim()?.takeIf { it.isNotEmpty() }
      ?: com.example.data.model.buildCartItemId(cleanId, cleanColor, cleanSize)
    val cleanFlashId = flashSaleId?.trim()?.takeIf { it.isNotEmpty() }
    context.applicationContext.guestCartDataStore.edit { prefs ->
      val current = decodeEntries(prefs[KEY_CART_ITEMS]).toMutableList()
      if (quantity <= 0) {
        current.removeAll { it.cartItemId == targetKey }
      } else {
        val idx = current.indexOfFirst { it.cartItemId == targetKey }
        if (idx >= 0) {
          current[idx] = LocalCartEntry(
            productId = cleanId,
            quantity = quantity,
            flashSaleId = cleanFlashId ?: current[idx].flashSaleId,
            selectedColor = cleanColor ?: current[idx].selectedColor,
            selectedSize = cleanSize ?: current[idx].selectedSize,
          )
        } else {
          current.add(
            LocalCartEntry(
              productId = cleanId,
              quantity = quantity,
              flashSaleId = cleanFlashId,
              selectedColor = cleanColor,
              selectedSize = cleanSize,
            ),
          )
        }
      }
      prefs[KEY_CART_ITEMS] = json.encodeToString(current)
    }
  }

  suspend fun incrementItem(
    productId: String,
    delta: Int,
    flashSaleId: String? = null,
    selectedColor: String? = null,
    selectedSize: String? = null,
  ) {
    val cleanId = productId.trim()
    if (cleanId.isEmpty() || delta == 0) return
    val cleanColor = selectedColor?.trim()?.takeIf { it.isNotEmpty() }
    val cleanSize = selectedSize?.trim()?.takeIf { it.isNotEmpty() }
    val targetKey = com.example.data.model.buildCartItemId(cleanId, cleanColor, cleanSize)
    val cleanFlashId = flashSaleId?.trim()?.takeIf { it.isNotEmpty() }
    context.applicationContext.guestCartDataStore.edit { prefs ->
      val current = decodeEntries(prefs[KEY_CART_ITEMS]).toMutableList()
      val idx = current.indexOfFirst { it.cartItemId == targetKey }
      if (idx >= 0) {
        val nextQty = current[idx].quantity + delta
        if (nextQty <= 0) {
          current.removeAt(idx)
        } else {
          current[idx] = LocalCartEntry(
            productId = cleanId,
            quantity = nextQty,
            flashSaleId = cleanFlashId ?: current[idx].flashSaleId,
            selectedColor = cleanColor ?: current[idx].selectedColor,
            selectedSize = cleanSize ?: current[idx].selectedSize,
          )
        }
      } else if (delta > 0) {
        current.add(
          LocalCartEntry(
            productId = cleanId,
            quantity = delta,
            flashSaleId = cleanFlashId,
            selectedColor = cleanColor,
            selectedSize = cleanSize,
          ),
        )
      }
      prefs[KEY_CART_ITEMS] = json.encodeToString(current)
    }
  }

  suspend fun removeItem(cartItemId: String) {
    val cleanKey = cartItemId.trim()
    if (cleanKey.isEmpty()) return
    context.applicationContext.guestCartDataStore.edit { prefs ->
      val current = decodeEntries(prefs[KEY_CART_ITEMS]).filterNot {
        it.cartItemId == cleanKey || it.productId == cleanKey
      }
      prefs[KEY_CART_ITEMS] = json.encodeToString(current)
    }
  }

  suspend fun clearCart() {
    context.applicationContext.guestCartDataStore.edit { prefs ->
      prefs.remove(KEY_CART_ITEMS)
    }
  }

  suspend fun consumeAllEntries(): List<LocalCartEntry> {
    val current = cartEntriesFlow.first()
    if (current.isNotEmpty()) {
      clearCart()
    }
    return current
  }

  suspend fun setCouponCode(code: String?) {
    val clean = code?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
    context.applicationContext.guestCartDataStore.edit { prefs ->
      if (clean == null) {
        prefs.remove(KEY_COUPON_CODE)
      } else {
        prefs[KEY_COUPON_CODE] = clean
      }
    }
  }

  private fun decodeEntries(raw: String?): List<LocalCartEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      json.decodeFromString<List<LocalCartEntry>>(raw)
        .filter { it.productId.isNotBlank() && it.quantity > 0 }
    }.getOrElse { emptyList() }
  }

  private companion object {
    val KEY_CART_ITEMS = stringPreferencesKey("guest_cart_items_json")
    val KEY_COUPON_CODE = stringPreferencesKey("applied_coupon_code")
  }
}
