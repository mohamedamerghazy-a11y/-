package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.BrandShowcase
import com.example.data.model.BrandShowcaseCategory
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.StoreCompany
import com.example.data.model.matchesBrandShowcase
import com.example.data.repository.BannerAndStoreRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminBrandShowcasesUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val showcases: List<BrandShowcase> = emptyList(),
  val companies: List<StoreCompany> = emptyList(),
  val categories: List<Category> = emptyList(),
  val products: List<Product> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val mainStoreCategories: List<Category>
    get() = categories.filter { it.isMainCategory }

  val companyById: Map<String, StoreCompany>
    get() = companies.associateBy { it.id }

  fun productsForShowcase(showcase: BrandShowcase): List<Product> =
    products.filter { it.matchesBrandShowcase(showcase) }

  fun productsInShowcase(showcaseId: String): List<Product> {
    val showcase = showcases.firstOrNull { it.id == showcaseId }
      ?: return products.filter { it.brandShowcaseId == showcaseId }
    return productsForShowcase(showcase)
  }
}

class AdminBrandShowcasesViewModel(
  private val bannerAndStoreRepository: BannerAndStoreRepository,
  private val categoryRepository: CategoryRepository,
  private val productRepository: ProductRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminBrandShowcasesUiState())
  val uiState: StateFlow<AdminBrandShowcasesUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      combine(
        bannerAndStoreRepository.observeAllBrandShowcases(),
        bannerAndStoreRepository.observeAllCompanies().catch { emit(emptyList()) },
        categoryRepository.observeAllCategories().catch { emit(emptyList()) },
        productRepository.observeAllProducts().catch { emit(emptyList()) },
      ) { showcases, companies, categories, products ->
        ShowcaseAdminBundle(showcases, companies, categories, products)
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل أقسام منتجات الشركات."),
            )
          }
        }
        .collect { bundle ->
          _uiState.update {
            it.copy(
              isLoading = false,
              showcases = bundle.showcases,
              companies = bundle.companies,
              categories = bundle.categories,
              products = bundle.products,
            )
          }
        }
    }
  }

  private data class ShowcaseAdminBundle(
    val showcases: List<BrandShowcase>,
    val companies: List<StoreCompany>,
    val categories: List<Category>,
    val products: List<Product>,
  )

  fun saveShowcase(showcase: BrandShowcase, onSavedId: ((String) -> Unit)? = null) {
    if (showcase.title.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى اختيار الشركة أو إدخال اسمها (حرفان على الأقل).") }
      return
    }
    if (showcase.discountPercent < 0.0 || showcase.discountPercent > 95.0) {
      _uiState.update { it.copy(errorMessage = "نسبة الخصم يجب أن تكون بين ٠٪ و٩٥٪.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = bannerAndStoreRepository.saveBrandShowcase(showcase)
      result.fold(
        onSuccess = { savedId ->
          _uiState.update { state ->
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (showcase.id.isBlank()) {
                "تمت إضافة منتجات الشركة بنجاح."
              } else {
                "تم حفظ تعديلات منتجات الشركة بنجاح."
              },
            )
          }
          onSavedId?.invoke(savedId)
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ منتجات الشركة حاليًا."),
            )
          }
        },
      )
    }
  }

  fun toggleShowcaseActive(showcase: BrandShowcase) {
    saveShowcase(showcase.copy(isActive = !showcase.isActive))
  }

  fun moveShowcaseOrder(showcase: BrandShowcase, directionDelta: Int) {
    val ordered = _uiState.value.showcases
    val index = ordered.indexOfFirst { it.id == showcase.id }
    if (index < 0) return
    val swapIndex = index + directionDelta
    if (swapIndex !in ordered.indices) return
    val other = ordered[swapIndex]

    val currentOrder = showcase.displayOrder
    val otherOrder = other.displayOrder
    val newShowcaseOrder = if (otherOrder == currentOrder) {
      (currentOrder + directionDelta).coerceIn(0, 1000)
    } else {
      otherOrder
    }
    val newOtherOrder = if (otherOrder == currentOrder) {
      currentOrder.coerceIn(0, 1000)
    } else {
      currentOrder
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val r1 = bannerAndStoreRepository.saveBrandShowcase(showcase.copy(displayOrder = newShowcaseOrder))
      val r2 = bannerAndStoreRepository.saveBrandShowcase(other.copy(displayOrder = newOtherOrder))
      _uiState.update {
        if (r1.isSuccess && r2.isSuccess) {
          it.copy(isSubmitting = false, feedbackMessage = "تم تحديث ترتيب ظهور الشركة بنجاح.")
        } else {
          it.copy(
            isSubmitting = false,
            errorMessage = (r1.exceptionOrNull() ?: r2.exceptionOrNull())
              ?.toArabicErrorMessage("تعذر تحديث ترتيب الظهور."),
          )
        }
      }
    }
  }

  fun saveShowcaseCategory(
    showcase: BrandShowcase,
    category: BrandShowcaseCategory,
  ) {
    if (category.name.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم فئة الشركة (حرفان على الأقل).") }
      return
    }
    if (category.imageUrl.trim().length < 5) {
      _uiState.update { it.copy(errorMessage = "يرجى رفع صورة لفئة الشركة لتظهر في شريط الفئات الجانبي.") }
      return
    }
    val cleanId = category.id.trim().ifEmpty { "bcat_${System.currentTimeMillis()}" }
    val updatedCategory = category.copy(
      id = cleanId,
      name = category.name.trim(),
      imageUrl = category.imageUrl.trim(),
    )
    val existing = showcase.categories.toMutableList()
    val existingIdx = existing.indexOfFirst { it.id == cleanId }
    if (existingIdx >= 0) {
      existing[existingIdx] = updatedCategory
    } else {
      existing.add(updatedCategory)
    }
    saveShowcase(showcase.copy(categories = existing))
  }

  fun deleteShowcaseCategory(showcase: BrandShowcase, categoryId: String) {
    val updatedCategories = showcase.categories.filterNot { it.id == categoryId }
    val fallbackCatId = updatedCategories.firstOrNull()?.id
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = bannerAndStoreRepository.saveBrandShowcase(showcase.copy(categories = updatedCategories))
      if (result.isSuccess) {
        val affectedProducts = _uiState.value.products.filter {
          it.brandShowcaseId == showcase.id && it.brandCategoryId == categoryId
        }
        for (prod in affectedProducts) {
          productRepository.saveProduct(
            prod.copy(
              brandCategoryId = fallbackCatId,
              brandShowcaseId = if (fallbackCatId != null) showcase.id else null,
              exclusiveToBrandShowcase = if (fallbackCatId != null) prod.exclusiveToBrandShowcase else false,
            ),
          )
        }
        _uiState.update { it.copy(isSubmitting = false, feedbackMessage = "تم حذف الفئة بنجاح.") }
      } else {
        _uiState.update {
          it.copy(
            isSubmitting = false,
            errorMessage = result.exceptionOrNull()?.toArabicErrorMessage("تعذر حذف الفئة."),
          )
        }
      }
    }
  }

  fun saveShowcaseProduct(
    showcase: BrandShowcase,
    product: Product,
  ) {
    if (showcase.categories.isEmpty()) {
      _uiState.update {
        it.copy(errorMessage = "يجب إضافة فئة واحدة على الأقل داخل الشركة مع صورة قبل إضافة أي منتج.")
      }
      return
    }
    val brandCatId = product.brandCategoryId?.trim().orEmpty()
    if (brandCatId.isEmpty() || showcase.categories.none { it.id == brandCatId }) {
      _uiState.update { it.copy(errorMessage = "يرجى تحديد فئة الشركة التابع لها المنتج.") }
      return
    }
    if (product.name.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم المنتج بشكل صحيح.") }
      return
    }
    val resolvedMainCategoryId = product.categoryId.trim().ifEmpty {
      _uiState.value.mainStoreCategories.firstOrNull()?.id.orEmpty()
    }
    if (resolvedMainCategoryId.isEmpty()) {
      _uiState.update { it.copy(errorMessage = "يرجى اختيار القسم الرئيسي للمنتج في المتجر (أو إنشاء قسم عام أولًا).") }
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

    val prepared = product.copy(
      categoryId = resolvedMainCategoryId,
      brandShowcaseId = showcase.id,
      brandCategoryId = brandCatId,
    )

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = productRepository.saveProduct(prepared)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (product.id.isBlank()) "تم إضافة منتج الشركة بنجاح." else "تم تحديث منتج الشركة بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ منتج الشركة حاليًا."),
            )
          },
        )
      }
    }
  }

  fun linkExistingProduct(
    showcase: BrandShowcase,
    product: Product,
    brandCategoryId: String,
    exclusiveToBrandShowcase: Boolean,
  ) {
    saveShowcaseProduct(
      showcase = showcase,
      product = product.copy(
        brandShowcaseId = showcase.id,
        brandCategoryId = brandCategoryId,
        exclusiveToBrandShowcase = exclusiveToBrandShowcase,
      ),
    )
  }

  fun unlinkProductFromShowcase(product: Product) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = productRepository.saveProduct(
        product.copy(
          brandShowcaseId = null,
          brandCategoryId = null,
          exclusiveToBrandShowcase = false,
        ),
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم فك ارتباط المنتج بعرض الشركة وإعادته للكتالوج العام.")
          },
          onFailure = { err ->
            state.copy(isSubmitting = false, errorMessage = err.toArabicErrorMessage("تعذر فك ارتباط المنتج."))
          },
        )
      }
    }
  }

  fun toggleProductExclusive(product: Product) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val updated = product.copy(exclusiveToBrandShowcase = !product.exclusiveToBrandShowcase)
      val result = productRepository.saveProduct(updated)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (updated.exclusiveToBrandShowcase) {
                "تم تعيين المنتج حصريًا داخل عرض الشركة فقط."
              } else {
                "تم إظهار المنتج في المتجر الرئيسي وعرض الشركة معًا."
              },
            )
          },
          onFailure = { err ->
            state.copy(isSubmitting = false, errorMessage = err.toArabicErrorMessage("تعذر تحديث حالة ظهور المنتج."))
          },
        )
      }
    }
  }

  fun deleteShowcase(showcaseId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val linkedProducts = _uiState.value.products.filter { it.brandShowcaseId == showcaseId }
      for (prod in linkedProducts) {
        productRepository.saveProduct(
          prod.copy(
            brandShowcaseId = null,
            brandCategoryId = null,
            exclusiveToBrandShowcase = if (prod.companyId.isNullOrBlank()) false else prod.exclusiveToBrandShowcase,
          ),
        )
      }
      val result = bannerAndStoreRepository.deleteBrandShowcase(showcaseId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم حذف قسم منتجات الشركة بنجاح.")
          },
          onFailure = { err ->
            state.copy(isSubmitting = false, errorMessage = err.toArabicErrorMessage("تعذر حذف قسم منتجات الشركة."))
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
      return AdminBrandShowcasesViewModel(
        bannerAndStoreRepository = BannerAndStoreRepository(appCtx),
        categoryRepository = CategoryRepository(appCtx),
        productRepository = ProductRepository(appCtx),
      ) as T
    }
  }
}
