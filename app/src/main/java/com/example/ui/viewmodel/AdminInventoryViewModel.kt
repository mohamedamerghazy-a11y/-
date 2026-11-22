package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.Product
import com.example.data.repository.EngagementRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminInventoryUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val products: List<Product> = emptyList(),
  val stockAlertCounts: Map<String, Int> = emptyMap(),
  val searchQuery: String = "",
  val onlyLowStock: Boolean = false,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val filteredProducts: List<Product>
    get() {
      val q = searchQuery.trim().lowercase()
      return products.filter { p ->
        val matchesQuery = q.isEmpty() ||
          p.name.lowercase().contains(q) ||
          p.sku.lowercase().contains(q) ||
          (p.barcode != null && p.barcode.lowercase().contains(q)) ||
          (p.qrCode != null && p.qrCode.lowercase().contains(q))
        val matchesLowStock = !onlyLowStock || p.isLowStock || p.isOutOfStock
        matchesQuery && matchesLowStock
      }
    }

  val lowStockCount: Int
    get() = products.count { it.isLowStock }

  val outOfStockCount: Int
    get() = products.count { it.isOutOfStock }
}

class AdminInventoryViewModel(
  private val productRepository: ProductRepository,
  private val engagementRepository: EngagementRepository? = null,
  private val notificationRepository: NotificationRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminInventoryUiState())
  val uiState: StateFlow<AdminInventoryUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      val alertCountsFlow = (engagementRepository?.observeAllStockAlertCounts() ?: flowOf(emptyMap()))
        .catch { emit(emptyMap()) }
      combine(
        productRepository.observeAllProducts(),
        alertCountsFlow,
      ) { products, counts ->
        products to counts
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل المخزون."),
            )
          }
        }
        .collect { (products, counts) ->
          _uiState.update {
            it.copy(
              isLoading = false,
              products = products,
              stockAlertCounts = counts,
            )
          }
        }
    }
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun toggleLowStockFilter() {
    _uiState.update { it.copy(onlyLowStock = !it.onlyLowStock) }
  }

  fun updateStock(
    productId: String,
    newStock: Int,
    lowStockThreshold: Int,
    isActive: Boolean,
  ) {
    val existing = _uiState.value.products.firstOrNull { it.id == productId }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = productRepository.updateStock(
        productId = productId,
        stockQuantity = newStock,
        lowStockThreshold = lowStockThreshold,
        isActive = isActive,
      )
      var notifiedCount = 0
      if (
        result.isSuccess &&
        existing != null &&
        existing.stockQuantity <= 0 &&
        newStock > 0 &&
        engagementRepository != null &&
        notificationRepository != null
      ) {
        notifiedCount = engagementRepository
          .dispatchRestockNotificationsAndClearAlerts(productId, existing.name, notificationRepository)
          .getOrDefault(0)
      }
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (notifiedCount > 0) {
                "تم تحديث المخزون بنجاح، وتم تنبيه $notifiedCount عميل بتوفر المنتج."
              } else {
                "تم تحديث المخزون وإعدادات التوفر بنجاح."
              },
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحديث المخزون."),
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
      return AdminInventoryViewModel(
        productRepository = ProductRepository(appCtx),
        engagementRepository = EngagementRepository(appCtx),
        notificationRepository = NotificationRepository(appCtx),
      ) as T
    }
  }
}
