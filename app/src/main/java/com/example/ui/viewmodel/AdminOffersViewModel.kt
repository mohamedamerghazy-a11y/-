package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.Product
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OfferFilterMode(val arabicLabel: String) {
  ALL("جميع المنتجات"),
  OFFERS_ONLY("العروض النشطة"),
  FEATURED_ONLY("المنتجات المميزة"),
}

data class AdminOffersUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val products: List<Product> = emptyList(),
  val selectedFilter: OfferFilterMode = OfferFilterMode.ALL,
  val searchQuery: String = "",
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val filteredProducts: List<Product>
    get() {
      val q = searchQuery.trim().lowercase()
      return products.filter { product ->
        val matchesMode = when (selectedFilter) {
          OfferFilterMode.ALL -> true
          OfferFilterMode.OFFERS_ONLY -> product.isOffer || product.hasDiscount
          OfferFilterMode.FEATURED_ONLY -> product.isFeatured
        }
        val matchesQuery = q.isEmpty() ||
          product.name.lowercase().contains(q) ||
          product.sku.lowercase().contains(q)
        matchesMode && matchesQuery
      }
    }

  val activeOffersCount: Int get() = products.count { it.isOffer || it.hasDiscount }
  val featuredCount: Int get() = products.count { it.isFeatured }
}

class AdminOffersViewModel(
  private val productRepository: ProductRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminOffersUiState())
  val uiState: StateFlow<AdminOffersUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      productRepository.observeAllProducts()
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل قائمة المنتجات والعروض."),
            )
          }
        }
        .collect { products ->
          _uiState.update {
            it.copy(
              isLoading = false,
              products = products,
            )
          }
        }
    }
  }

  fun setFilter(filter: OfferFilterMode) {
    _uiState.update { it.copy(selectedFilter = filter) }
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun updateOfferSettings(
    product: Product,
    retailDiscountPrice: Double?,
    isOffer: Boolean,
    isFeatured: Boolean,
  ) {
    if (retailDiscountPrice != null && (retailDiscountPrice <= 0.0 || retailDiscountPrice >= product.retailPrice)) {
      _uiState.update {
        it.copy(errorMessage = "سعر الخصم يجب أن يكون أكبر من 0 وأقل من سعر القطاعي الأصلي (${product.retailPrice}).")
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = productRepository.updateOfferSettings(
        productId = product.id,
        retailDiscountPrice = retailDiscountPrice,
        isOffer = isOffer,
        isFeatured = isFeatured,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم تحديث إعدادات العرض لمنتج «${product.name}» بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحديث العرض."),
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
      return AdminOffersViewModel(
        productRepository = ProductRepository(context.applicationContext),
      ) as T
    }
  }
}
