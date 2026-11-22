package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.model.StaffPermission
import com.example.core.util.ArabicFormatter
import com.example.ui.components.AlEmanCard
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminDashboardUiState

private data class AdminGroup(
  val title: String,
  val items: List<StaffPermission>,
)

private val adminGroups = listOf(
  AdminGroup(
    title = "المبيعات والطلبات والتوصيل",
    items = listOf(
      StaffPermission.VIEW_SALES_SUMMARY,
      StaffPermission.MANAGE_ORDERS,
      StaffPermission.DELIVER_ORDERS,
      StaffPermission.MANAGE_PAYMENTS,
      StaffPermission.MANAGE_DELIVERY,
      StaffPermission.VIEW_REPORTS,
    ),
  ),
  AdminGroup(
    title = "الكتالوج والأقسام والمخزون",
    items = listOf(
      StaffPermission.MANAGE_PRODUCTS,
      StaffPermission.MANAGE_CATEGORIES,
      StaffPermission.MANAGE_INVENTORY,
    ),
  ),
  AdminGroup(
    title = "العملاء وحسابات الجملة",
    items = listOf(
      StaffPermission.MANAGE_CUSTOMERS,
      StaffPermission.APPROVE_WHOLESALE,
      StaffPermission.MANAGE_REVIEWS,
    ),
  ),
  AdminGroup(
    title = "العروض والفلاش سيل والإعدادات",
    items = listOf(
      StaffPermission.MANAGE_FLASH_SALES,
      StaffPermission.MANAGE_COUPONS,
      StaffPermission.MANAGE_OFFERS,
      StaffPermission.MANAGE_BANNERS,
      StaffPermission.MANAGE_STAFF,
      StaffPermission.MANAGE_STORE_SETTINGS,
    ),
  ),
)

@Composable
fun AdminDashboardShellScreen(
  roleLabel: String,
  allowedPermissions: Set<StaffPermission>,
  dashboardState: AdminDashboardUiState,
  isWholesalePreviewActive: Boolean = false,
  canToggleWholesalePreview: Boolean = false,
  onToggleWholesalePreview: () -> Unit = {},
  onBackClick: () -> Unit,
  onSelectSection: (StaffPermission) -> Unit,
  onOpenBrandShowcases: () -> Unit = {},
) {
  val visibleGroups = adminGroups.mapNotNull { group ->
    val filtered = group.items.filter { it in allowedPermissions }
    if (filtered.isEmpty()) null else group.copy(items = filtered)
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      AlEmanTopBar(
        title = stringResource(id = R.string.admin_panel_title),
        subtitle = "$roleLabel • ${allowedPermissions.size} صلاحية مفعلة",
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
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 840.dp)
          .fillMaxSize(),
        contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        if (canToggleWholesalePreview) {
          item {
            AlEmanCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(onClick = onToggleWholesalePreview)
                  .testTag("admin_wholesale_preview_toggle")
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryYellowSoft, RoundedCornerShape(10.dp)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(Icons.Filled.Visibility, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (isWholesalePreviewActive) "وضع المعاينة الحالي: أسعار الجملة" else "وضع المعاينة الحالي: أسعار القطاعي",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                  Text(
                    text = "بدّل لعرض المتجر للمراجعة بأسعار الجملة أو القطاعي",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                  )
                }
                Switch(
                  checked = isWholesalePreviewActive,
                  onCheckedChange = { onToggleWholesalePreview() },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = CharcoalBlack,
                    checkedTrackColor = PrimaryYellow,
                  ),
                )
              }
            }
          }
        }

        if (allowedPermissions.isNotEmpty()) {
          item {
            AdminOverviewBanner(
              roleLabel = roleLabel,
              permissionsCount = allowedPermissions.size,
              dashboardState = dashboardState,
              allowedPermissions = allowedPermissions,
              onSelectSection = onSelectSection,
            )
          }
        }

        if (StaffPermission.MANAGE_BANNERS in allowedPermissions || StaffPermission.MANAGE_PRODUCTS in allowedPermissions) {
          item {
            AlEmanCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(onClick = onOpenBrandShowcases)
                  .testTag("admin_tile_brand_showcases")
                  .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryYellow, RoundedCornerShape(10.dp)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = "منتجات الشركات",
                    tint = CharcoalBlack,
                    modifier = Modifier.size(20.dp),
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "منتجات الشركات",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                  )
                  Text(
                    text = "اختر شركة لتظهر منتجاتها تلقائيًا بتصميم متناوب أو بنر وخصم اختياري",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDescription = null,
                  tint = TextSecondary,
                )
              }
            }
          }
        }

        if (visibleGroups.isEmpty()) {
          item {
            StoreEmptyStateCard(
              icon = Icons.Filled.Settings,
              title = "لا توجد صلاحيات مفعلة حاليًا",
              description = "تواصل مع مالك المتجر لتفعيل صلاحيات الوصول إلى أقسام لوحة الإدارة.",
              actionLabel = "العودة للمتجر",
              onActionClick = onBackClick,
            )
          }
        } else {
          items(visibleGroups) { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = group.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
              )
              AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                  group.items.forEachIndexed { index, permission ->
                    val badgeCount = when (permission) {
                      StaffPermission.MANAGE_ORDERS -> dashboardState.newAndReviewOrdersCount
                      StaffPermission.APPROVE_WHOLESALE -> dashboardState.pendingWholesaleCount
                      StaffPermission.MANAGE_INVENTORY -> dashboardState.lowStockCount
                      else -> 0
                    }
                    AdminMenuRow(
                      permission = permission,
                      badgeCount = badgeCount,
                      onClick = { onSelectSection(permission) },
                    )
                    if (index < group.items.lastIndex) {
                      HorizontalDivider(color = BorderNeutral)
                    }
                  }
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
private fun AdminOverviewBanner(
  roleLabel: String,
  permissionsCount: Int,
  dashboardState: AdminDashboardUiState,
  allowedPermissions: Set<StaffPermission>,
  onSelectSection: (StaffPermission) -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(AlEmanDimens.SpaceLg),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "ملخص أداء المتجر المباشر",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        BrandPillBadge(
          text = "$roleLabel ($permissionsCount)",
          containerColor = PrimaryYellow,
          contentColor = CharcoalBlack,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (StaffPermission.VIEW_SALES_SUMMARY in allowedPermissions || StaffPermission.VIEW_REPORTS in allowedPermissions) {
          KpiMetricTile(
            label = "إجمالي المبيعات",
            value = ArabicFormatter.formatCurrency(dashboardState.totalSalesRevenue),
            accentColor = SuccessGreen,
            containerColor = GeneralBackground,
            modifier = Modifier.weight(1f),
            onClick = {
              val target = if (StaffPermission.VIEW_REPORTS in allowedPermissions) {
                StaffPermission.VIEW_REPORTS
              } else {
                StaffPermission.MANAGE_ORDERS
              }
              onSelectSection(target)
            },
          )
        }
        if (StaffPermission.MANAGE_ORDERS in allowedPermissions || StaffPermission.VIEW_SALES_SUMMARY in allowedPermissions) {
          KpiMetricTile(
            label = "طلبات جديدة",
            value = dashboardState.newAndReviewOrdersCount.toString(),
            accentColor = CharcoalBlack,
            containerColor = if (dashboardState.newAndReviewOrdersCount > 0) PrimaryYellowSoft else GeneralBackground,
            modifier = Modifier.weight(1f),
            onClick = {
              if (StaffPermission.MANAGE_ORDERS in allowedPermissions) {
                onSelectSection(StaffPermission.MANAGE_ORDERS)
              }
            },
          )
        }
        if (StaffPermission.APPROVE_WHOLESALE in allowedPermissions || StaffPermission.MANAGE_CUSTOMERS in allowedPermissions) {
          KpiMetricTile(
            label = "طلبات جملة معلقة",
            value = dashboardState.pendingWholesaleCount.toString(),
            accentColor = CharcoalBlack,
            containerColor = if (dashboardState.pendingWholesaleCount > 0) PrimaryYellow else GeneralBackground,
            modifier = Modifier.weight(1f),
            onClick = { onSelectSection(StaffPermission.APPROVE_WHOLESALE) },
          )
        }
      }

      if (StaffPermission.MANAGE_INVENTORY in allowedPermissions || StaffPermission.MANAGE_PRODUCTS in allowedPermissions) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          KpiMetricTile(
            label = "إجمالي المنتجات",
            value = dashboardState.totalProductsCount.toString(),
            accentColor = CharcoalBlack,
            containerColor = GeneralBackground,
            modifier = Modifier.weight(1f),
            onClick = { onSelectSection(StaffPermission.MANAGE_PRODUCTS) },
          )
          KpiMetricTile(
            label = "تنبيهات المخزون",
            value = dashboardState.lowStockCount.toString(),
            accentColor = if (dashboardState.lowStockCount > 0) DiscountRed else SuccessGreen,
            containerColor = if (dashboardState.lowStockCount > 0) DiscountRedSoft else GeneralBackground,
            modifier = Modifier.weight(1f),
            onClick = {
              val target = if (StaffPermission.MANAGE_INVENTORY in allowedPermissions) {
                StaffPermission.MANAGE_INVENTORY
              } else {
                StaffPermission.MANAGE_PRODUCTS
              }
              onSelectSection(target)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun KpiMetricTile(
  label: String,
  value: String,
  accentColor: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  Surface(
    modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
    border = BorderStroke(AlEmanDimens.BorderWidth, BorderNeutral),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AdminMenuRow(
  permission: StaffPermission,
  badgeCount: Int,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("admin_tile_${permission.name.lowercase()}")
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .background(
          color = PrimaryYellowSoft,
          shape = RoundedCornerShape(10.dp),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = permissionIcon(permission),
        contentDescription = permission.arabicTitle,
        tint = CharcoalBlack,
        modifier = Modifier.size(20.dp),
      )
    }
    Text(
      text = permission.arabicTitle,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
      color = TextPrimary,
      modifier = Modifier.weight(1f),
    )
    if (badgeCount > 0) {
      Surface(
        shape = RoundedCornerShape(50),
        color = PrimaryYellow,
      ) {
        Text(
          text = badgeCount.toString(),
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
      }
    }
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
      contentDescription = null,
      tint = TextSecondary,
    )
  }
}

@Composable
fun AdminSectionPlaceholderScreen(
  sectionTitle: String,
  onBackClick: () -> Unit,
) {
  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      AlEmanTopBar(
        title = sectionTitle,
        subtitle = stringResource(id = R.string.admin_panel_title),
        showBack = true,
        onBackClick = onBackClick,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(AlEmanDimens.SpaceLg),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.widthIn(max = 600.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        StoreEmptyStateCard(
          icon = Icons.Filled.Settings,
          title = sectionTitle,
          description = "هذا القسم جاهز لربط بياناته الحقيقية من قاعدة البيانات.",
          actionLabel = "العودة للوحة الإدارة",
          onActionClick = onBackClick,
        )
      }
    }
  }
}

private fun permissionIcon(permission: StaffPermission): ImageVector = when (permission) {
  StaffPermission.VIEW_SALES_SUMMARY -> Icons.Filled.Analytics
  StaffPermission.MANAGE_PRODUCTS -> Icons.Filled.Storefront
  StaffPermission.MANAGE_CATEGORIES -> Icons.Filled.Category
  StaffPermission.MANAGE_CUSTOMERS -> Icons.Filled.Group
  StaffPermission.APPROVE_WHOLESALE -> Icons.Filled.VerifiedUser
  StaffPermission.MANAGE_ORDERS -> Icons.AutoMirrored.Filled.ReceiptLong
  StaffPermission.MANAGE_PAYMENTS -> Icons.Filled.Payments
  StaffPermission.MANAGE_INVENTORY -> Icons.Filled.Inventory
  StaffPermission.MANAGE_DELIVERY -> Icons.Filled.LocalShipping
  StaffPermission.MANAGE_COUPONS -> Icons.Filled.ConfirmationNumber
  StaffPermission.MANAGE_OFFERS -> Icons.Filled.LocalOffer
  StaffPermission.MANAGE_FLASH_SALES -> Icons.Filled.Bolt
  StaffPermission.MANAGE_BANNERS -> Icons.Filled.ViewCarousel
  StaffPermission.MANAGE_REVIEWS -> Icons.Filled.RateReview
  StaffPermission.VIEW_REPORTS -> Icons.Filled.Assessment
  StaffPermission.MANAGE_STAFF -> Icons.Filled.Badge
  StaffPermission.MANAGE_STORE_SETTINGS -> Icons.Filled.Settings
  StaffPermission.PREVIEW_WHOLESALE_STORE -> Icons.Filled.Visibility
  StaffPermission.DELIVER_ORDERS -> Icons.Filled.DeliveryDining
}
