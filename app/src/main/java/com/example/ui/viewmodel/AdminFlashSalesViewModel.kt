package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.Category
import com.example.data.model.FlashSale
import com.example.data.model.Product
import com.example.data.repository.CategoryRepository
import com.example.data.repository.FlashSaleRepository
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminFlashSalesUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val flashSales: List<FlashSale> = emptyList(),
  val products: List<Product> = emptyList(),
  val categories: List<Category> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
)

class AdminFlashSalesViewModel(
  private val flashSaleRepository: FlashSaleRepository,
  private val productRepository: ProductRepository,
  private val categoryRepository: CategoryRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminFlashSalesUiState())
  val uiState: StateFlow<AdminFlashSalesUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      combine(
        flashSaleRepository.observeAllFlashSales(),
        productRepository.observeAllProducts().catch { emit(emptyList()) },
        categoryRepository.observeAllCategories().catch { emit(emptyList()) },
      ) { sales, products, categories ->
        Triple(sales, products, categories)
      }
        .catch { err ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحميل عروض الفلاش سيل."),
            )
          }
        }
        .collect { (sales, products, categories) ->
          _uiState.update {
            it.copy(
              isLoading = false,
              flashSales = sales,
              products = products,
              categories = categories.filter { it.isActive }.sortedBy { it.displayOrder },
            )
          }
        }
    }
  }

  fun saveFlashSale(flashSale: FlashSale, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = flashSaleRepository.saveFlashSale(flashSale)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            onSaved()
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (flashSale.id.isBlank()) {
                "تم إنشاء عرض الفلاش سيل بنجاح."
              } else {
                "تم تحديث عرض الفلاش سيل بنجاح."
              },
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ عرض الفلاش سيل."),
            )
          },
        )
      }
    }
  }

  fun toggleFlashSaleActive(flashSale: FlashSale) {
    viewModelScope.launch {
      val next = !flashSale.isActive
      val result = flashSaleRepository.toggleFlashSaleActive(flashSale.id, next)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              feedbackMessage = if (next) {
                "تم تفعيل «${flashSale.title}» بنجاح."
              } else {
                "تم إيقاف «${flashSale.title}» مؤقتًا."
              },
            )
          },
          onFailure = { err ->
            state.copy(errorMessage = err.toArabicErrorMessage("تعذر تحديث حالة الفلاش سيل."))
          },
        )
      }
    }
  }

  fun deleteFlashSale(flashSaleId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = flashSaleRepository.deleteFlashSale(flashSaleId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم حذف الفلاش سيل بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف الفلاش سيل."),
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
      val appCtx = context.applicationContext
      return AdminFlashSalesViewModel(
        flashSaleRepository = FlashSaleRepository(appCtx),
        productRepository = ProductRepository(appCtx),
        categoryRepository = CategoryRepository(appCtx),
      ) as T
    }
  }
}
