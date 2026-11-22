package com.aleman.cash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aleman.cash.model.CashProviderConfig
import com.aleman.cash.model.CashVerifierSettings
import com.aleman.cash.model.defaultCashProviders
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.cashDataStore: DataStore<Preferences> by preferencesDataStore(name = "aleman_cash_settings")

class CashProviderSettingsStore(private val context: Context) {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val settingsKey = stringPreferencesKey("cash_verifier_settings_json")

  val settingsFlow: Flow<CashVerifierSettings> = context.cashDataStore.data.map { prefs ->
    val raw = prefs[settingsKey]
    if (raw.isNullOrBlank()) {
      CashVerifierSettings()
    } else {
      runCatching {
        val decoded = json.decodeFromString<CashVerifierSettings>(raw)
        if (decoded.providers.isEmpty()) decoded.copy(providers = defaultCashProviders()) else decoded
      }.getOrElse {
        CashVerifierSettings()
      }
    }
  }

  suspend fun getCurrentSettings(): CashVerifierSettings = settingsFlow.first()

  suspend fun toggleProviderEnabled(providerId: String, enabled: Boolean) {
    updateSettings { current ->
      current.copy(
        providers = current.providers.map {
          if (it.id == providerId) it.copy(isEnabled = enabled) else it
        }
      )
    }
  }

  suspend fun upsertProvider(
    displayName: String,
    aliasesInput: String,
    isEnabled: Boolean = true,
    editingId: String? = null
  ) {
    val cleanName = displayName.trim()
    if (cleanName.isBlank()) return
    val parsedAliases = aliasesInput
      .split(",", "،", "\n", "|")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .ifEmpty { listOf(cleanName) }
      .distinct()

    updateSettings { current ->
      val existing = editingId?.let { id -> current.providers.find { it.id == id } }
      val updatedList = if (existing != null) {
        current.providers.map {
          if (it.id == existing.id) {
            it.copy(displayName = cleanName, senderAliases = parsedAliases, isEnabled = isEnabled)
          } else it
        }
      } else {
        current.providers + CashProviderConfig(
          id = "custom_${UUID.randomUUID().toString().take(8)}",
          displayName = cleanName,
          senderAliases = parsedAliases,
          isEnabled = isEnabled,
          isBuiltIn = false
        )
      }
      current.copy(providers = updatedList)
    }
  }

  suspend fun deleteProvider(providerId: String) {
    updateSettings { current ->
      current.copy(providers = current.providers.filterNot { it.id == providerId })
    }
  }

  suspend fun setAutoVerifyEnabled(enabled: Boolean) {
    updateSettings { it.copy(autoVerifyEnabled = enabled) }
  }

  suspend fun setTargetOrderStatus(status: String) {
    val normalized = if (status == "confirmed") "confirmed" else "preparing"
    updateSettings { it.copy(targetOrderStatusAfterPayment = normalized) }
  }

  private suspend fun updateSettings(transform: (CashVerifierSettings) -> CashVerifierSettings) {
    context.cashDataStore.edit { prefs ->
      val current = prefs[settingsKey]?.let { raw ->
        runCatching { json.decodeFromString<CashVerifierSettings>(raw) }.getOrNull()
      } ?: CashVerifierSettings()
      val updated = transform(current)
      prefs[settingsKey] = json.encodeToString(updated)
    }
  }
}
