package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.DeliveryZone
import com.example.data.repository.CouponAndDeliveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDeliveryZonesUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val zones: List<DeliveryZone> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
)

class AdminDeliveryZonesViewModel(
  private val repository: CouponAndDeliveryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminDeliveryZonesUiState())
  val uiState: StateFlow<AdminDeliveryZonesUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      repository.observeAllDeliveryZones()
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل مناطق التوصيل."),
            )
          }
        }
        .collect { list ->
          _uiState.update {
            it.copy(
              isLoading = false,
              zones = list,
            )
          }
        }
    }
  }

  fun saveDeliveryZone(zone: DeliveryZone) {
    if (zone.name.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم المنطقة بشكل صحيح.") }
      return
    }
    if (zone.governorate.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم المحافظة.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.saveDeliveryZone(zone)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (zone.id.isBlank()) "تم إضافة منطقة التوصيل بنجاح." else "تم تحديث منطقة التوصيل بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ منطقة التوصيل."),
            )
          },
        )
      }
    }
  }

  fun deleteDeliveryZone(zoneId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.deleteDeliveryZone(zoneId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم حذف منطقة التوصيل بنجاح.")
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف منطقة التوصيل."),
            )
          },
        )
      }
    }
  }

  fun clearMessages() {
    _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
  }

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AdminDeliveryZonesViewModel(
        repository = CouponAndDeliveryRepository(context.applicationContext),
      ) as T
    }
  }
}
