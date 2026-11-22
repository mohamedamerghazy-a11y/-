package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.model.OrderStatus
import com.example.core.util.ArabicFormatter
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminReportsUiState
import com.example.ui.viewmodel.ReportPeriod
import com.example.ui.viewmodel.RevenueChartBucket

@Composable
fun AdminReportsScreen(
  uiState: AdminReportsUiState,
  onSelectPeriod: (ReportPeriod) -> Unit,
  onOpenAdminProducts: () -> Unit = {},
  onDeleteSearchMiss: (String) -> Unit = {},
  onBackClick: () -> Unit,
) {
  val analytics = uiState.analytics

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      AlEmanTopBar(
        title = "التقارير والإحصائيات",
        subtitle = "ملخص المبيعات والتحليل المالي والعملاء",
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            // 1. فلاتر الفترة الزمنية
            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReportPeriod.entries) { period ->
                  val selected = uiState.selectedPeriod == period
                  FilterChip(
                    selected = selected,
                    onClick = { onSelectPeriod(period) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = PrimaryYellow,
                      selectedLabelColor = CharcoalBlack,
                    ),
                    modifier = Modifier.testTag("report_period_${period.name.lowercase()}"),
                    label = {
                      Text(
                        text = period.arabicLabel,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                      )
                    },
                  )
                }
              }
            }

            // 2. بطاقة الإيرادات الرئيسية
            item {
              AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(AlEmanDimens.SpaceLg),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Text(
                    text = "صافي المبيعات المحققة (${analytics.period.arabicLabel})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                  Text(
                    text = ArabicFormatter.formatCurrency(analytics.totalRevenue),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "متوسط الطلب: ${ArabicFormatter.formatCurrency(analytics.averageOrderValue)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                    Text(
                      text = "مصاريف التوصيل: ${ArabicFormatter.formatCurrency(analytics.totalDeliveryFees)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                  }
                  if (analytics.totalDiscounts > 0.0) {
                    Text(
                      text = "إجمالي خصومات الكوبونات الممنوحة: ${ArabicFormatter.formatCurrency(analytics.totalDiscounts)}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                      color = DiscountRed,
                    )
                  }
                }
              }
            }

            // 3. شبكة أرقام الطلبات
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                ReportMetricTile(
                  label = "إجمالي الطلبات",
                  value = analytics.totalOrdersCount.toString(),
                  accentColor = CharcoalBlack,
                  containerColor = PureWhite,
                  modifier = Modifier.weight(1f),
                )
                ReportMetricTile(
                  label = "مكتملة",
                  value = analytics.completedOrdersCount.toString(),
                  accentColor = SuccessGreen,
                  containerColor = PureWhite,
                  modifier = Modifier.weight(1f),
                )
                ReportMetricTile(
                  label = "قيد التنفيذ",
                  value = analytics.pendingOrdersCount.toString(),
                  accentColor = CharcoalBlack,
                  containerColor = PrimaryYellowSoft,
                  modifier = Modifier.weight(1f),
                )
              }
            }

            // 4. مقارنة القطاعي مقابل الجملة
            item {
              AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Text(
                    text = "توزيع المبيعات: القطاعي مقابل الجملة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                  val sumRev = (analytics.retailRevenue + analytics.wholesaleRevenue).coerceAtLeast(1.0)
                  val retailRatio = (analytics.retailRevenue / sumRev).toFloat().coerceIn(0f, 1f)
                  val wholesaleRatio = (analytics.wholesaleRevenue / sumRev).toFloat().coerceIn(0f, 1f)

                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(14.dp)
                      .clip(RoundedCornerShape(50))
                      .background(BorderNeutral),
                  ) {
                    if (retailRatio > 0f) {
                      Box(
                        modifier = Modifier
                          .weight(retailRatio)
                          .fillMaxHeight()
                          .background(PrimaryYellow),
                      )
                    }
                    if (wholesaleRatio > 0f) {
                      Box(
                        modifier = Modifier
                          .weight(wholesaleRatio)
                          .fillMaxHeight()
                          .background(CharcoalBlack),
                      )
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    ChannelLegendBlock(
                      dotColor = PrimaryYellow,
                      title = "القطاعي (${analytics.retailOrdersCount} طلب)",
                      amount = ArabicFormatter.formatCurrency(analytics.retailRevenue),
                    )
                    ChannelLegendBlock(
                      dotColor = CharcoalBlack,
                      title = "الجملة (${analytics.wholesaleOrdersCount} طلب)",
                      amount = ArabicFormatter.formatCurrency(analytics.wholesaleRevenue),
                    )
                  }
                }
              }
            }

            // 5. الرسم البياني لمبيعات آخر 7 أيام (Compose Canvas)
            item {
              AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Text(
                    text = "حركة المبيعات اليومية (آخر 7 أيام)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                  DailyRevenueBarChart(buckets = analytics.dailyRevenueBuckets)
                }
              }
            }

            // 6. توزيع حالات الطلبات
            if (analytics.statusDistribution.isNotEmpty()) {
              item {
                AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Text(
                      text = "توزيع حالات الطلبات",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                    analytics.statusDistribution.forEach { item ->
                      val barColor = when {
                        item.status == OrderStatus.DELIVERED -> SuccessGreen
                        item.status.isTerminalException -> DiscountRed
                        else -> PrimaryYellow
                      }
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                          Text(
                            text = item.status.arabicLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CharcoalBlack,
                          )
                          Text(
                            text = "${item.count} (${(item.shareFraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                          )
                        }
                        Box(
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(BorderNeutral),
                        ) {
                          Box(
                            modifier = Modifier
                              .fillMaxWidth(item.shareFraction.coerceIn(0.03f, 1f))
                              .fillMaxHeight()
                              .background(barColor),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            // 7. أكثر المنتجات مبيعًا
            item {
              AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(20.dp))
                    Text(
                      text = "أكثر المنتجات مبيعًا",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                  }
                  if (analytics.topProducts.isEmpty()) {
                    Text("لا توجد بيانات مبيعات منتجات في هذه الفترة.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                  } else {
                    analytics.topProducts.forEachIndexed { idx, prod ->
                      if (idx > 0) HorizontalDivider(color = BorderNeutral)
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = "${idx + 1}. ${prod.productName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = CharcoalBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                          Text(
                            text = "تم بيع ${prod.unitsSold} وحدة",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                          )
                        }
                        Text(
                          text = ArabicFormatter.formatCurrency(prod.totalRevenue),
                          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                          color = SuccessGreen,
                        )
                      }
                    }
                  }
                }
              }
            }

            // 8. أفضل العملاء طلبًا
            item {
              AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(20.dp))
                    Text(
                      text = "أكثر العملاء إنفاقًا",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                  }
                  if (analytics.topCustomers.isEmpty()) {
                    Text("لا توجد طلبات مسجلة في هذه الفترة.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                  } else {
                    analytics.topCustomers.forEachIndexed { idx, customer ->
                      if (idx > 0) HorizontalDivider(color = BorderNeutral)
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = "${idx + 1}. ${customer.customerName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = CharcoalBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                          Text(
                            text = "${customer.ordersCount} طلب",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                          )
                        }
                        Text(
                          text = ArabicFormatter.formatCurrency(customer.totalSpent),
                          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                          color = CharcoalBlack,
                        )
                      }
                    }
                  }
                }
              }
            }

            // 9. تقرير الكلمات المبحوث عنها دون نتائج
            item {
              AlEmanCard(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_search_misses_card"),
              ) {
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
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.weight(1f),
                    ) {
                      Icon(Icons.Filled.SearchOff, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(20.dp))
                      Text(
                        text = "كلمات بحث بدون نتائج (${uiState.searchMisses.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CharcoalBlack,
                      )
                    }
                    OutlinedButton(
                      onClick = onOpenAdminProducts,
                      border = BorderStroke(1.dp, CharcoalBlack),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                      Icon(Icons.Filled.Add, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("إضافة منتج", color = CharcoalBlack, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                  }
                  Text(
                    text = "عبارات يبحث عنها العملاء في التطبيق والويب ولا توجد لها منتجات مطابقة حاليًا.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                  )
                  if (uiState.searchMisses.isEmpty()) {
                    Text(
                      text = "لا توجد عمليات بحث بدون نتائج مسجلة حتى الآن.",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                  } else {
                    uiState.searchMisses.forEachIndexed { idx, miss ->
                      if (idx > 0) HorizontalDivider(color = BorderNeutral)
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(
                          modifier = Modifier.weight(1f),
                          verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                          Text(
                            text = miss.query,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = CharcoalBlack,
                          )
                          Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            BrandPillBadge(
                              text = "${miss.missCount} مرة بحث",
                              containerColor = PrimaryYellowSoft,
                              contentColor = CharcoalBlack,
                            )
                            BrandPillBadge(
                              text = if (miss.lastSource == "web") "المتجر الإلكتروني" else "تطبيق الأندرويد",
                              containerColor = RoyalBlueSoft,
                              contentColor = RoyalBlue,
                            )
                          }
                        }
                        IconButton(
                          onClick = { onDeleteSearchMiss(miss.id) },
                          modifier = Modifier.testTag("delete_search_miss_${miss.id}"),
                        ) {
                          Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "حذف كلمة البحث",
                            tint = DiscountRed,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            if (analytics.totalOrdersCount == 0) {
              item {
                StoreEmptyStateCard(
                  icon = Icons.Filled.Assessment,
                  title = "لا توجد طلبات في الفترة المختارة",
                  description = "جرّب اختيار «جميع الفترات» لعرض كل البيانات المسجلة.",
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DailyRevenueBarChart(buckets: List<RevenueChartBucket>) {
  if (buckets.isEmpty()) return
  val maxRevenue = buckets.maxOfOrNull { it.revenue }?.coerceAtLeast(1.0) ?: 1.0

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "الحد الأعلى المسجل: ${ArabicFormatter.formatCurrency(maxRevenue)}",
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary,
    )

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(148.dp)
        .padding(top = 8.dp),
    ) {
      val count = buckets.size.coerceAtLeast(1)
      val slotWidth = size.width / count.toFloat()
      val barWidth = (slotWidth * 0.48f).coerceAtLeast(12f)
      val baselineY = size.height - 4f

      // خط قاعدة خفيف
      drawLine(
        color = BorderNeutral,
        start = Offset(0f, baselineY),
        end = Offset(size.width, baselineY),
        strokeWidth = 2f,
      )

      buckets.forEachIndexed { index, bucket ->
        val ratio = (bucket.revenue / maxRevenue).toFloat().coerceIn(0f, 1f)
        val barHeight = if (bucket.revenue > 0.0) {
          (ratio * (size.height - 16f)).coerceAtLeast(10f)
        } else {
          6f
        }
        val leftX = index * slotWidth + (slotWidth - barWidth) / 2f
        val topY = baselineY - barHeight
        val isLatest = index == buckets.lastIndex
        val color = when {
          bucket.revenue <= 0.0 -> BorderNeutral
          isLatest -> PrimaryYellow
          else -> CharcoalBlack
        }
        drawRoundRect(
          color = color,
          topLeft = Offset(leftX, topY),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(10f, 10f),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      buckets.forEach { bucket ->
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = bucket.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = CharcoalBlack,
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
          Text(
            text = if (bucket.ordersCount > 0) "${bucket.ordersCount} طلب" else "-",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun ChannelLegendBlock(
  dotColor: Color,
  title: String,
  amount: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Box(
      modifier = Modifier
        .size(12.dp)
        .clip(CircleShape)
        .background(dotColor),
    )
    Column {
      Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      Text(amount, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CharcoalBlack)
    }
  }
}

@Composable
private fun ReportMetricTile(
  label: String,
  value: String,
  accentColor: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    border = BorderStroke(AlEmanDimens.BorderWidth, BorderNeutral),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
