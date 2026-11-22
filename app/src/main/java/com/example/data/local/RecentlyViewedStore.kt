package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storePrefsDataStore by preferencesDataStore(name = "aleman_store_prefs")

class RecentlyViewedStore(private val context: Context) {

  val recentProductIdsFlow: Flow<List<String>> =
    context.applicationContext.storePrefsDataStore.data.map { prefs ->
      val raw = prefs[KEY_RECENTLY_VIEWED_IDS].orEmpty()
      if (raw.isBlank()) {
        emptyList()
      } else {
        raw.split(DELIMITER).filter { it.isNotBlank() }
      }
    }

  suspend fun recordViewed(productId: String) {
    val cleanId = productId.trim()
    if (cleanId.isEmpty()) return
    context.applicationContext.storePrefsDataStore.edit { prefs ->
      val current = prefs[KEY_RECENTLY_VIEWED_IDS]
        .orEmpty()
        .split(DELIMITER)
        .filter { it.isNotBlank() && it != cleanId }
      val updated = (listOf(cleanId) + current).take(MAX_RECENT_ITEMS)
      prefs[KEY_RECENTLY_VIEWED_IDS] = updated.joinToString(DELIMITER)
    }
  }

  private companion object {
    val KEY_RECENTLY_VIEWED_IDS = stringPreferencesKey("recently_viewed_product_ids")
    const val DELIMITER = "|"
    const val MAX_RECENT_ITEMS = 15
  }
}
