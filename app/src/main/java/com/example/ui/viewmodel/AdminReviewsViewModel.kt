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

enum class ReviewModerationFilter(val arabicLabel: String) {
  ALL("الكل"),
  VISIBLE("الظاهرة"),
  HIDDEN("المخفية"),
}

data class AdminReviewsUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val reviews: List<ProductReview> = emptyList(),
  val selectedFilter: ReviewModerationFilter = ReviewModerationFilter.ALL,
  val searchQuery: String = "",
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val filteredReviews: List<ProductReview>
    get() {
      val q = searchQuery.trim().lowercase()
      return reviews.filter { review ->
        val matchesFilter = when (selectedFilter) {
          ReviewModerationFilter.ALL -> true
          ReviewModerationFilter.VISIBLE -> !review.isHidden
          ReviewModerationFilter.HIDDEN -> review.isHidden
        }
        val matchesQuery = q.isEmpty() ||
          review.productName.lowercase().contains(q) ||
          review.userName.lowercase().contains(q) ||
          review.comment.lowercase().contains(q)
        matchesFilter && matchesQuery
      }
    }

  val visibleCount: Int get() = reviews.count { !it.isHidden }
  val hiddenCount: Int get() = reviews.count { it.isHidden }
  val averageRatingAll: Double
    get() = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
}

class AdminReviewsViewModel(
  private val reviewRepository: ReviewRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminReviewsUiState())
  val uiState: StateFlow<AdminReviewsUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      reviewRepository.observeAllReviews()
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل تقييمات العملاء."),
            )
          }
        }
        .collect { reviews ->
          _uiState.update {
            it.copy(
              isLoading = false,
              reviews = reviews,
            )
          }
        }
    }
  }

  fun setFilter(filter: ReviewModerationFilter) {
    _uiState.update { it.copy(selectedFilter = filter) }
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun toggleReviewVisibility(review: ProductReview) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val nextHidden = !review.isHidden
      val result = reviewRepository.setReviewHidden(review.id, nextHidden)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (nextHidden) "تم إخفاء التقييم عن الظهور في المتجر." else "تم إظهار التقييم في صفحة المنتج.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحديث حالة التقييم."),
            )
          },
        )
      }
    }
  }

  fun deleteReview(reviewId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = reviewRepository.deleteReview(reviewId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(isSubmitting = false, feedbackMessage = "تم حذف التقييم نهائيًا.")
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

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AdminReviewsViewModel(
        reviewRepository = ReviewRepository(context.applicationContext),
      ) as T
    }
  }
}
