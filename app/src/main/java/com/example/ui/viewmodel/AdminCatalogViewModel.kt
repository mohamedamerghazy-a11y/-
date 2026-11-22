package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.StoreCompany
import com.example.data.repository.BannerAndStoreRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.EngagementRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminCatalogUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val categories: List<Category> = emptyList(),
  val products: List<Product> = emptyList(),
  val companies: List<StoreCompany> = emptyList(),
  val stockAlertCounts: Map<String, Int> = emptyMap(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val mainCategories: List<Category>
    get() = categories.filter { it.isMainCategory }

  fun subCategoriesFor(parentId: String): List<Category> =
    categories.filter { it.parentId == parentId }

  val categoryById: Map<String, Category>
    get() = categories.associateBy { it.id }

  val companyById: Map<String, StoreCompany>
    get() = companies.associateBy { it.id }
}

class AdminCatalogViewModel(
  private val categoryRepository: CategoryRepository,
  private val productRepository: ProductRepository,
  private val bannerAndStoreRepository: BannerAndStoreRepository? = null,
  private val engagementRepository: EngagementRepository? = null,
  private val notificationRepository: NotificationRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminCatalogUiState())
  val uiState: StateFlow<AdminCatalogUiState> = _uiState.asStateFlow()

  private var observeJob: Job? = null

  init {
    startObserving()
  }

  private fun startObserving() {
    observeJob?.cancel()
    observeJob = viewModelScope.launch {
      val alertCountsFlow = (engagementRepository?.observeAllStockAlertCounts() ?: flowOf(emptyMap()))
        .catch { emit(emptyMap()) }
      val companiesFlow = (bannerAndStoreRepository?.observeAllCompanies() ?: flowOf(emptyList()))
        .catch { emit(emptyList()) }
      combine(
        categoryRepository.observeAllCategories(),
        productRepository.observeAllProducts(),
        companiesFlow,
        alertCountsFlow,
      ) { categories, products, companies, counts ->
        Quadruple(categories, products, companies, counts)
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل بيانات الأقسام والمنتجات للإدارة."),
            )
          }
        }
        .collect { (categories, products, companies, counts) ->
          _uiState.update {
            it.copy(
              isLoading = false,
              categories = categories,
              products = products,
              companies = companies,
              stockAlertCounts = counts,
            )
          }
        }
    }
  }

  private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
  )

  fun saveCategory(category: Category) {
    if (category.name.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم القسم (حرفان على الأقل).") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = categoryRepository.saveCategory(category)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (category.id.isBlank()) "تم إضافة القسم بنجاح." else "تم تحديث القسم بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ القسم حاليًا."),
            )
          },
        )
      }
    }
  }

  fun deleteCategory(categoryId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = categoryRepository.deleteCategory(categoryId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم حذف القسم بنجاح.")
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف القسم حاليًا."),
            )
          },
        )
      }
    }
  }

  fun saveProduct(product: Product) {
    if (product.name.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم المنتج بشكل صحيح.") }
      return
    }
    if (product.categoryId.isBlank()) {
      _uiState.update { it.copy(errorMessage = "يرجى اختيار القسم الرئيسي للمنتج.") }
      return
    }
    if (product.sku.trim().isEmpty()) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال رمز الصنف (SKU).") }
      return
    }
    if (product.retailPrice <= 0.0) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال سعر قطاعي أكبر من صفر.") }
      return
    }
    if (product.retailDiscountPrice != null && product.retailDiscountPrice >= product.retailPrice) {
      _uiState.update { it.copy(errorMessage = "سعر الخصم يجب أن يكون أقل من السعر القطاعي الأصلي.") }
      return
    }

    val prevStock = _uiState.value.products.firstOrNull { it.id == product.id }?.stockQuantity
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = productRepository.saveProduct(product)
      var notifiedCount = 0
      if (
        result.isSuccess &&
        product.id.isNotBlank() &&
        prevStock != null &&
        prevStock <= 0 &&
        product.stockQuantity > 0 &&
        engagementRepository != null &&
        notificationRepository != null
      ) {
        notifiedCount = engagementRepository
          .dispatchRestockNotificationsAndClearAlerts(product.id, product.name, notificationRepository)
          .getOrDefault(0)
      }
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            val baseMsg = if (product.id.isBlank()) "تم إضافة المنتج بنجاح." else "تم تحديث بيانات المنتج بنجاح."
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (notifiedCount > 0) {
                "$baseMsg وتم تنبيه $notifiedCount عميل بتوفر المنتج."
              } else {
                baseMsg
              },
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ المنتج حاليًا."),
            )
          },
        )
      }
    }
  }

  fun deleteProduct(productId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = productRepository.deleteProduct(productId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم حذف المنتج بنجاح.")
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف المنتج حاليًا."),
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
      return AdminCatalogViewModel(
        categoryRepository = CategoryRepository(appCtx),
        productRepository = ProductRepository(appCtx),
        bannerAndStoreRepository = BannerAndStoreRepository(appCtx),
        engagementRepository = EngagementRepository(appCtx),
        notificationRepository = NotificationRepository(appCtx),
      ) as T
    }
  }
}
