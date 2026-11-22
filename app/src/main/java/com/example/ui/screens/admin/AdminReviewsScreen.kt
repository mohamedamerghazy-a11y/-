package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductReview
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.DiscountBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminReviewsUiState
import com.example.ui.viewmodel.ReviewModerationFilter
import java.util.Locale

@Composable
fun AdminReviewsScreen(
  uiState: AdminReviewsUiState,
  onSelectFilter: (ReviewModerationFilter) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onToggleVisibility: (ProductReview) -> Unit,
  onDeleteReview: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var deletingReviewId by remember { mutableStateOf<String?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
    val msg = uiState.feedbackMessage ?: uiState.errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
    }
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = "إدارة تقييمات العملاء",
        subtitle = "مراجعة التعليقات وإخفاء غير الملائم منها",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when {
        uiState.isLoading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CharcoalBlack)
          }
        }

        else -> {
          LazyColumn(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
            contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                ReviewStatChip("الظاهرة: ${uiState.visibleCount}", PrimaryYellowSoft, Modifier.weight(1f))
                ReviewStatChip("المخفية: ${uiState.hiddenCount}", PureWhite, Modifier.weight(1f))
                ReviewStatChip(
                  "المتوسط: ${String.format(Locale.US, "%.1f", uiState.averageRatingAll)} ★",
                  PrimaryYellowSoft,
                  Modifier.weight(1f),
                )
              }
            }

            item {
              OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("ابحث باسم المنتج أو العميل أو التعليق...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_reviews_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              )
            }

            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReviewModerationFilter.entries) { filter ->
                  val selected = uiState.selectedFilter == filter
                  FilterChip(
                    selected = selected,
                    onClick = { onSelectFilter(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = PrimaryYellow,
                      selectedLabelColor = CharcoalBlack,
                    ),
                    label = {
                      Text(filter.arabicLabel, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    },
                  )
                }
              }
            }

            if (uiState.filteredReviews.isEmpty()) {
              item {
                StoreEmptyStateCard(
                  icon = Icons.Filled.RateReview,
                  title = "لا توجد تقييمات مطابقة",
                  description = "ستظهر تقييمات ومراجعات العملاء للمنتجات هنا فور إضافتها.",
                )
              }
            } else {
              items(uiState.filteredReviews, key = { it.id }) { review ->
                ReviewModerationCard(
                  review = review,
                  isSubmitting = uiState.isSubmitting,
                  onToggleVisibility = { onToggleVisibility(review) },
                  onDelete = { deletingReviewId = review.id },
                )
              }
            }
          }
        }
      }
    }
  }

  if (deletingReviewId != null) {
    AlertDialog(
      onDismissRequest = { deletingReviewId = null },
      containerColor = PureWhite,
      title = { Text("حذف التقييم نهائيًا", fontWeight = FontWeight.Bold, color = DiscountRed) },
      text = { Text("هل أنت متأكد من حذف هذا التقييم من قاعدة البيانات؟") },
      confirmButton = {
        Button(
          onClick = {
            deletingReviewId?.let { onDeleteReview(it) }
            deletingReviewId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed),
        ) {
          Text("حذف", color = PureWhite)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingReviewId = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@Composable
private fun ReviewStatChip(
  label: String,
  containerColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
    border = BorderStroke(1.dp, BorderNeutral),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
      color = CharcoalBlack,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ReviewModerationCard(
  review: ProductReview,
  isSubmitting: Boolean,
  onToggleVisibility: () -> Unit,
  onDelete: () -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = review.productName,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "بواسطة: ${review.userName}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        if (review.isHidden) DiscountBadge("مخفي") else SuccessBadge("ظاهر")
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { idx ->
          Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = if (idx < review.rating) PrimaryYellow else BorderNeutral,
            modifier = Modifier.size(18.dp),
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text("${review.rating}/5", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
      }

      Text(
        text = review.comment,
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalBlack,
      )

      HorizontalDivider(color = BorderNeutral)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onToggleVisibility,
          enabled = !isSubmitting,
          border = BorderStroke(1.dp, CharcoalBlack.copy(alpha = 0.35f)),
          modifier = Modifier.testTag("toggle_review_visibility_${review.id}"),
        ) {
          Icon(
            imageVector = if (review.isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            contentDescription = null,
            tint = CharcoalBlack,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (review.isHidden) "إظهار التقييم" else "إخفاء التقييم",
            color = CharcoalBlack,
            fontWeight = FontWeight.SemiBold,
          )
        }

        IconButton(
          onClick = onDelete,
          enabled = !isSubmitting,
          modifier = Modifier.testTag("delete_review_${review.id}"),
        ) {
          Icon(Icons.Filled.Delete, contentDescription = "حذف التقييم", tint = DiscountRed)
        }
      }
    }
  }
}
