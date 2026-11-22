package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.model.OrderStatus
import com.example.data.model.Order
import com.example.data.model.SearchMiss
import com.example.data.repository.EngagementRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportPeriod(val arabicLabel: String, val daysBack: Int?) {
  DAILY("اليوم", 1),
  WEEKLY("آخر 7 أيام", 7),
  MONTHLY("آخر 30 يومًا", 30),
  ALL_TIME("جميع الفترات", null),
}

data class RevenueChartBucket(
  val label: String,
  val revenue: Double,
  val ordersCount: Int,
)

data class StatusDistributionItem(
  val status: OrderStatus,
  val count: Int,
  val shareFraction: Float,
)

data class TopProductReport(
  val productId: String,
  val productName: String,
  val unitsSold: Int,
  val totalRevenue: Double,
)

data class TopCustomerReport(
  val userId: String,
  val customerName: String,
  val ordersCount: Int,
  val totalSpent: Double,
)

data class ComputedReportAnalytics(
  val period: ReportPeriod = ReportPeriod.ALL_TIME,
  val totalRevenue: Double = 0.0,
  val retailRevenue: Double = 0.0,
  val wholesaleRevenue: Double = 0.0,
  val totalDiscounts: Double = 0.0,
  val totalDeliveryFees: Double = 0.0,
  val totalOrdersCount: Int = 0,
  val completedOrdersCount: Int = 0,
  val pendingOrdersCount: Int = 0,
  val cancelledOrdersCount: Int = 0,
  val retailOrdersCount: Int = 0,
  val wholesaleOrdersCount: Int = 0,
  val averageOrderValue: Double = 0.0,
  val dailyRevenueBuckets: List<RevenueChartBucket> = emptyList(),
  val statusDistribution: List<StatusDistributionItem> = emptyList(),
  val topProducts: List<TopProductReport> = emptyList(),
  val topCustomers: List<TopCustomerReport> = emptyList(),
)

object ReportAnalyticsCalculator {
  private const val MILLIS_PER_DAY = 86_400_000L

  fun compute(
    orders: List<Order>,
    period: ReportPeriod,
    nowMillis: Long = System.currentTimeMillis(),
  ): ComputedReportAnalytics {
    val filtered = filterByPeriod(orders, period, nowMillis)
    val nonCancelled = filtered.filter { !it.statusEnum.isTerminalException }

    val totalRevenue = nonCancelled.sumOf { it.totalAmount }
    val retailOrders = nonCancelled.filter { it.accountTypeAtOrder != "wholesale" }
    val wholesaleOrders = nonCancelled.filter { it.accountTypeAtOrder == "wholesale" }

    val retailRevenue = retailOrders.sumOf { it.totalAmount }
    val wholesaleRevenue = wholesaleOrders.sumOf { it.totalAmount }
    val totalDiscounts = nonCancelled.sumOf { it.discountAmount }
    val totalDeliveryFees = nonCancelled.sumOf { it.deliveryFee }
    val avgOrderValue = if (nonCancelled.isEmpty()) 0.0 else totalRevenue / nonCancelled.size.toDouble()

    val statusDist = OrderStatus.entries
      .map { st ->
        val count = filtered.count { it.statusEnum == st }
        val share = if (filtered.isEmpty()) 0f else count.toFloat() / filtered.size.toFloat()
        StatusDistributionItem(status = st, count = count, shareFraction = share)
      }
      .filter { it.count > 0 }
      .sortedByDescending { it.count }

    // آخر 7 أيام كرسم بياني بالأعمدة
    val buckets = buildLastDaysBuckets(nonCancelled, daysCount = 7, nowMillis = nowMillis)

    // أفضل 5 منتجات مبيعًا
    val productMap = linkedMapOf<String, MutableProductAccum>()
    nonCancelled.forEach { order ->
      order.items.forEach { item ->
        val key = item.productId.ifBlank { item.productName }
        val acc = productMap.getOrPut(key) {
          MutableProductAccum(
            productId = item.productId,
            productName = item.productName.ifBlank { "منتج" },
          )
        }
        acc.unitsSold += item.quantity
        acc.totalRevenue += item.lineTotal
      }
    }
    val topProducts = productMap.values
      .sortedByDescending { it.totalRevenue }
      .take(5)
      .map { TopProductReport(it.productId, it.productName, it.unitsSold, it.totalRevenue) }

    // أفضل 5 عملاء إنفاقًا
    val customerMap = linkedMapOf<String, MutableCustomerAccum>()
    nonCancelled.forEach { order ->
      val key = order.userId.ifBlank { order.customerName }
      val acc = customerMap.getOrPut(key) {
        MutableCustomerAccum(
          userId = order.userId,
          customerName = order.customerName.ifBlank { "عميل المتجر" },
        )
      }
      acc.ordersCount += 1
      acc.totalSpent += order.totalAmount
    }
    val topCustomers = customerMap.values
      .sortedByDescending { it.totalSpent }
      .take(5)
      .map { TopCustomerReport(it.userId, it.customerName, it.ordersCount, it.totalSpent) }

    return ComputedReportAnalytics(
      period = period,
      totalRevenue = totalRevenue,
      retailRevenue = retailRevenue,
      wholesaleRevenue = wholesaleRevenue,
      totalDiscounts = totalDiscounts,
      totalDeliveryFees = totalDeliveryFees,
      totalOrdersCount = filtered.size,
      completedOrdersCount = filtered.count { it.statusEnum == OrderStatus.DELIVERED },
      pendingOrdersCount = filtered.count { it.statusEnum.isActiveProgression },
      cancelledOrdersCount = filtered.count { it.statusEnum.isTerminalException },
      retailOrdersCount = filtered.count { it.accountTypeAtOrder != "wholesale" },
      wholesaleOrdersCount = filtered.count { it.accountTypeAtOrder == "wholesale" },
      averageOrderValue = avgOrderValue,
      dailyRevenueBuckets = buckets,
      statusDistribution = statusDist,
      topProducts = topProducts,
      topCustomers = topCustomers,
    )
  }

  private fun filterByPeriod(
    orders: List<Order>,
    period: ReportPeriod,
    nowMillis: Long,
  ): List<Order> {
    val days = period.daysBack ?: return orders
    val cutoffMillis = if (days == 1) {
      val cal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      cal.timeInMillis
    } else {
      nowMillis - (days.toLong() * MILLIS_PER_DAY)
    }

    return orders.filter { order ->
      val createdMillis = order.createdAt?.toDate()?.time
        ?: order.updatedAt?.toDate()?.time
        ?: return@filter false
      createdMillis >= cutoffMillis
    }
  }

  private fun buildLastDaysBuckets(
    orders: List<Order>,
    daysCount: Int,
    nowMillis: Long,
  ): List<RevenueChartBucket> {
    val arLocale = Locale.Builder().setLanguage("ar").setRegion("EG").build()
    val dayFmt = SimpleDateFormat("EEE", arLocale)
    val cal = Calendar.getInstance()
    val result = mutableListOf<RevenueChartBucket>()

    for (offset in (daysCount - 1) downTo 0) {
      cal.timeInMillis = nowMillis
      cal.add(Calendar.DAY_OF_YEAR, -offset)
      cal.set(Calendar.HOUR_OF_DAY, 0)
      cal.set(Calendar.MINUTE, 0)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
      val dayStart = cal.timeInMillis
      val dayEnd = dayStart + MILLIS_PER_DAY

      val dayOrders = orders.filter { order ->
        val ts = order.createdAt?.toDate()?.time ?: order.updatedAt?.toDate()?.time ?: return@filter false
        ts in dayStart until dayEnd
      }
      val label = if (offset == 0) "اليوم" else dayFmt.format(Date(dayStart))
      result.add(
        RevenueChartBucket(
          label = label,
          revenue = dayOrders.sumOf { it.totalAmount },
          ordersCount = dayOrders.size,
        ),
      )
    }
    return result
  }

  private data class MutableProductAccum(
    val productId: String,
    val productName: String,
    var unitsSold: Int = 0,
    var totalRevenue: Double = 0.0,
  )

  private data class MutableCustomerAccum(
    val userId: String,
    val customerName: String,
    var ordersCount: Int = 0,
    var totalSpent: Double = 0.0,
  )
}

data class AdminReportsUiState(
  val isLoading: Boolean = true,
  val orders: List<Order> = emptyList(),
  val selectedPeriod: ReportPeriod = ReportPeriod.ALL_TIME,
  val analytics: ComputedReportAnalytics = ComputedReportAnalytics(),
  val searchMisses: List<SearchMiss> = emptyList(),
  val errorMessage: String? = null,
)

class AdminReportsViewModel(
  private val orderRepository: OrderRepository,
  private val engagementRepository: EngagementRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminReportsUiState())
  val uiState: StateFlow<AdminReportsUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      val searchMissesFlow = (engagementRepository?.observeSearchMisses() ?: flowOf(emptyList()))
        .catch { emit(emptyList()) }
      combine(
        orderRepository.observeAllOrders(),
        searchMissesFlow,
      ) { orders, misses ->
        orders to misses
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل تقارير المبيعات."),
            )
          }
        }
        .collect { (orders, misses) ->
          _uiState.update { state ->
            state.copy(
              isLoading = false,
              orders = orders,
              analytics = ReportAnalyticsCalculator.compute(orders, state.selectedPeriod),
              searchMisses = misses,
            )
          }
        }
    }
  }

  fun selectPeriod(period: ReportPeriod) {
    _uiState.update { state ->
      state.copy(
        selectedPeriod = period,
        analytics = ReportAnalyticsCalculator.compute(state.orders, period),
      )
    }
  }

  fun deleteSearchMiss(missId: String) {
    val repo = engagementRepository ?: return
    viewModelScope.launch {
      repo.deleteSearchMiss(missId)
    }
  }

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val appCtx = context.applicationContext
      return AdminReportsViewModel(
        orderRepository = OrderRepository(appCtx),
        engagementRepository = EngagementRepository(appCtx),
      ) as T
    }
  }
}
