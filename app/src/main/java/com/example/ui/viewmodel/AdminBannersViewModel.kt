package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.Banner
import com.example.data.model.BannerTargetType
import com.example.data.model.Category
import com.example.data.model.Product
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

data class AdminBannersUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val banners: List<Banner> = emptyList(),
  val categories: List<Category> = emptyList(),
  val products: List<Product> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
)

class AdminBannersViewModel(
  private val repository: BannerAndStoreRepository,
  private val categoryRepository: CategoryRepository,
  private val productRepository: ProductRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminBannersUiState())
  val uiState: StateFlow<AdminBannersUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      combine(
        repository.observeAllBanners(),
        categoryRepository.observeActiveCategories().catch { emit(emptyList()) },
        productRepository.observeActiveProducts().catch { emit(emptyList()) },
      ) { banners, categories, products ->
        Triple(banners, categories, products)
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل قائمة البنرات الإعلانية."),
            )
          }
        }
        .collect { (banners, categories, products) ->
          _uiState.update {
            it.copy(
              isLoading = false,
              banners = banners,
              categories = categories,
              products = products,
            )
          }
        }
    }
  }

  fun saveBanner(banner: Banner) {
    if (banner.mediaTypeEnum == com.example.data.model.BannerMediaType.VIDEO) {
      if (banner.videoUrl.trim().length < 5) {
        _uiState.update { it.copy(errorMessage = "يرجى رفع الفيديو الدعائي أو إدخال رابط فيديو صالح أولًا.") }
        return
      }
    } else if (banner.mediaTypeEnum == com.example.data.model.BannerMediaType.ANIMATED_IMAGE) {
      if (banner.imageUrl.trim().length < 5) {
        _uiState.update { it.copy(errorMessage = "يرجى رفع الصورة المتحركة (GIF / WebP) أو إدخال رابط صالح أولًا.") }
        return
      }
    } else {
      val cleanImage = banner.imageUrl.trim()
      if (cleanImage.length < 5) {
        _uiState.update { it.copy(errorMessage = "يرجى رفع صورة البنر أولًا.") }
        return
      }
    }
    val targetEnum = BannerTargetType.fromCode(banner.targetType)
    if ((targetEnum == BannerTargetType.CATEGORY || targetEnum == BannerTargetType.PRODUCT) && banner.targetId.isNullOrBlank()) {
      _uiState.update { it.copy(errorMessage = "يرجى تحديد القسم أو المنتج المرتبط بالبنر.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.saveBanner(banner)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (banner.id.isBlank()) "تم إضافة البنر الإعلاني بنجاح." else "تم تحديث البنر الإعلاني بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ البنر الإعلاني."),
            )
          },
        )
      }
    }
  }

  fun toggleBannerActive(banner: Banner) {
    saveBanner(banner.copy(isActive = !banner.isActive))
  }

  fun deleteBanner(bannerId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.deleteBanner(bannerId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم حذف البنر الإعلاني بنجاح.")
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف البنر الإعلاني."),
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
      return AdminBannersViewModel(
        repository = BannerAndStoreRepository(appCtx),
        categoryRepository = CategoryRepository(appCtx),
        productRepository = ProductRepository(appCtx),
      ) as T
    }
  }
}
