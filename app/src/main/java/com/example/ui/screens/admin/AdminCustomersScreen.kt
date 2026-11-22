package com.example.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminStaffUiState

private enum class CustomerFilter(val label: String) {
  ALL("الكل"),
  PENDING("طلبات جملة معلقة"),
  APPROVED("جملة معتمد"),
  RETAIL("قطاعي"),
  REJECTED("مرفوض"),
}

@Composable
fun AdminCustomersScreen(
  uiState: AdminStaffUiState,
  onUpdateWholesaleDecision: (String, Boolean) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(CustomerFilter.ALL) }

  LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
    val msg = uiState.feedbackMessage ?: uiState.errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
    }
  }

  val filteredUsers = remember(uiState.usersList, searchQuery, selectedFilter) {
    val q = searchQuery.trim().lowercase()
    uiState.usersList.filter { user ->
      val matchesFilter = when (selectedFilter) {
        CustomerFilter.ALL -> true
        CustomerFilter.PENDING -> user.wholesaleStatus == "pending"
        CustomerFilter.APPROVED -> user.wholesaleStatus == "approved"
        CustomerFilter.RETAIL -> user.wholesaleStatus == "none"
        CustomerFilter.REJECTED -> user.wholesaleStatus == "rejected"
      }
      val matchesQuery = q.isEmpty() ||
        user.fullName.lowercase().contains(q) ||
        user.email.lowercase().contains(q) ||
        user.phone.contains(q) ||
        (user.wholesaleCompanyName?.lowercase()?.contains(q) == true)
      matchesFilter && matchesQuery
    }
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = "إدارة العملاء وطلبات الجملة",
        subtitle = "مراجعة واعتماد حسابات الجملة والعملاء",
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

        uiState.usersList.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.Group,
              title = "لا توجد حسابات عملاء مسجلة",
              description = "ستظهر حسابات العملاء هنا بمجرد تسجيلهم في التطبيق.",
              actionLabel = "العودة",
              onActionClick = onBackClick,
            )
          }
        }

        else -> {
          Column(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث بالاسم، البريد، الهاتف أو اسم النشاط...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_customers_search"),
                singleLine = true,
              )

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                CustomerFilter.entries.forEach { filter ->
                  val count = when (filter) {
                    CustomerFilter.ALL -> uiState.usersList.size
                    CustomerFilter.PENDING -> uiState.usersList.count { it.wholesaleStatus == "pending" }
                    CustomerFilter.APPROVED -> uiState.usersList.count { it.wholesaleStatus == "approved" }
                    CustomerFilter.RETAIL -> uiState.usersList.count { it.wholesaleStatus == "none" }
                    CustomerFilter.REJECTED -> uiState.usersList.count { it.wholesaleStatus == "rejected" }
                  }
                  FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = PrimaryYellow,
                      selectedLabelColor = CharcoalBlack,
                    ),
                    label = { Text("${filter.label} ($count)") },
                  )
                }
              }
            }

            if (filteredUsers.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(AlEmanDimens.SpaceLg),
                contentAlignment = Alignment.Center,
              ) {
                StoreEmptyStateCard(
                  icon = Icons.Filled.Group,
                  title = "لا توجد نتائج مطابقة",
                  description = "جرّب تعديل كلمة البحث أو اختيار تصنيف آخر للعملاء.",
                  actionLabel = "عرض الكل",
                  onActionClick = {
                    searchQuery = ""
                    selectedFilter = CustomerFilter.ALL
                  },
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(filteredUsers, key = { it.uid }) { user ->
                  CustomerApprovalCard(
                    user = user,
                    onApproveWholesale = { onUpdateWholesaleDecision(user.uid, true) },
                    onRejectWholesale = { onUpdateWholesaleDecision(user.uid, false) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CustomerApprovalCard(
  user: UserProfile,
  onApproveWholesale: () -> Unit,
  onRejectWholesale: () -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = user.fullName.ifEmpty { "عميل" },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
          Text(
            text = user.email,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        val (badgeLabel, badgeContainer, badgeContent) = when (user.wholesaleStatus) {
          "approved" -> Triple("جملة معتمد", PrimaryYellow, CharcoalBlack)
          "pending" -> Triple("طلب جملة معلق", PrimaryYellowSoft, CharcoalBlack)
          "rejected" -> Triple("جملة مرفوض", DiscountRedSoft, DiscountRed)
          else -> Triple("قطاعي", GeneralBackground, TextSecondary)
        }
        BrandPillBadge(
          text = badgeLabel,
          containerColor = badgeContainer,
          contentColor = badgeContent,
        )
      }

      if (user.phone.isNotBlank() || !user.wholesaleCompanyName.isNullOrBlank()) {
        HorizontalDivider(color = BorderNeutral)
        if (user.phone.isNotBlank()) {
          Text(
            text = "الهاتف: ${user.phone}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        if (!user.wholesaleCompanyName.isNullOrBlank()) {
          Text(
            text = "الشركة / النشاط: ${user.wholesaleCompanyName}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
          )
        }
      }

      HorizontalDivider(color = BorderNeutral)
      when (user.wholesaleStatus) {
        "pending" -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = onApproveWholesale,
              modifier = Modifier
                .weight(1f)
                .testTag("approve_wholesale_${user.uid}"),
              colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryYellow,
                contentColor = CharcoalBlack,
              ),
            ) {
              Text("اعتماد كجملة", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = onRejectWholesale,
              modifier = Modifier
                .weight(1f)
                .testTag("reject_wholesale_${user.uid}"),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = DiscountRed),
            ) {
              Text("رفض الطلب", fontWeight = FontWeight.SemiBold)
            }
          }
        }

        "approved" -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "يتمتع بأسعار الجملة في المتجر",
              style = MaterialTheme.typography.bodySmall,
              color = SuccessGreen,
            )
            OutlinedButton(
              onClick = onRejectWholesale,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = DiscountRed),
            ) {
              Text("إلغاء اعتماد الجملة")
            }
          }
        }

        else -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "يتسوق بأسعار القطاعي",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
            Button(
              onClick = onApproveWholesale,
              colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryYellow,
                contentColor = CharcoalBlack,
              ),
            ) {
              Text("ترقية لجملة", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
