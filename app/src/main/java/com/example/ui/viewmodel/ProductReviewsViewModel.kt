package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.ProductReview
import com.example.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductReviewsUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val reviews: List<ProductReview> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val averageRating: Double
    get() = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()

  val reviewCount: Int
    get() = reviews.size
}

class ProductReviewsViewModel(
  private val productId: String,
  private val repository: ReviewRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProductReviewsUiState())
  val uiState: StateFlow<ProductReviewsUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      repository.observeVisibleProductReviews(productId)
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل تقييمات المنتج."),
            )
          }
        }
        .collect { list ->
          _uiState.update {
            it.copy(
              isLoading = false,
              reviews = list,
            )
          }
        }
    }
  }

  fun submitReview(
    productName: String,
    userName: String,
    rating: Int,
    comment: String,
  ) {
    val cleanComment = comment.trim()
    if (cleanComment.length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى كتابة تعليق قصير (حرفين على الأقل).") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.submitOrUpdateReview(
        productId = productId,
        productName = productName,
        userName = userName,
        rating = rating,
        comment = cleanComment,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "شكرًا لك! تم حفظ تقييمك للمنتج بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر إرسال التقييم."),
            )
          },
        )
      }
    }
  }

  fun deleteOwnReview(reviewId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.deleteReview(reviewId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم حذف تقييمك.")
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف التقييم."),
            )
          },
        )
      }
    }
  }

  fun clearMessages() {
    _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
  }

  class Factory(
    private val context: Context,
    private val productId: String,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ProductReviewsViewModel(
        productId = productId,
        repository = ReviewRepository(context.applicationContext),
      ) as T
    }
  }
}
