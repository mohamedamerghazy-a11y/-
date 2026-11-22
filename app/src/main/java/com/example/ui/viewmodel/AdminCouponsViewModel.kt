package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.Coupon
import com.example.data.repository.CouponAndDeliveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminCouponsUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val coupons: List<Coupon> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
)

class AdminCouponsViewModel(
  private val repository: CouponAndDeliveryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminCouponsUiState())
  val uiState: StateFlow<AdminCouponsUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      repository.observeAllCoupons()
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل قائمة الكوبونات."),
            )
          }
        }
        .collect { list ->
          _uiState.update {
            it.copy(
              isLoading = false,
              coupons = list,
            )
          }
        }
    }
  }

  fun saveCoupon(coupon: Coupon) {
    if (coupon.code.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال كود كوبون صحيح (حرفين على الأقل).") }
      return
    }
    if (coupon.discountValue <= 0.0) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال قيمة خصم أكبر من صفر.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.saveCoupon(coupon)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (coupon.id.isBlank()) "تم إنشاء الكوبون بنجاح." else "تم تحديث الكوبون بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ الكوبون."),
            )
          },
        )
      }
    }
  }

  fun deleteCoupon(couponId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.deleteCoupon(couponId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم حذف الكوبون بنجاح.")
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف الكوبون."),
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
      return AdminCouponsViewModel(
        repository = CouponAndDeliveryRepository(context.applicationContext),
      ) as T
    }
  }
}
