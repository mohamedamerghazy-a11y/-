package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.OrderStatus
import com.example.core.model.StaffPermission
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.repository.OrderRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
  val totalSalesRevenue: Double = 0.0,
  val newAndReviewOrdersCount: Int = 0,
  val inProgressOrdersCount: Int = 0,
  val deliveredOrdersCount: Int = 0,
  val pendingWholesaleCount: Int = 0,
  val approvedWholesaleCount: Int = 0,
  val totalCustomersCount: Int = 0,
  val lowStockCount: Int = 0,
  val outOfStockCount: Int = 0,
  val totalProductsCount: Int = 0,
)

class AdminDashboardViewModel(
  private val allowedPermissions: Set<StaffPermission>,
  private val orderRepository: OrderRepository,
  private val productRepository: ProductRepository,
  private val staffRepository: StaffRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminDashboardUiState())
  val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

  init {
    observePermittedMetrics()
  }

  private fun observePermittedMetrics() {
    val canViewOrders = StaffPermission.VIEW_SALES_SUMMARY in allowedPermissions ||
      StaffPermission.MANAGE_ORDERS in allowedPermissions ||
      StaffPermission.MANAGE_PAYMENTS in allowedPermissions ||
      StaffPermission.MANAGE_DELIVERY in allowedPermissions ||
      StaffPermission.VIEW_REPORTS in allowedPermissions

    if (canViewOrders) {
      viewModelScope.launch {
        orderRepository.observeAllOrders()
          .catch { /* صلاحية الطلبات غير متاحة أو خطأ شبكة عابر */ }
          .collect { orders: List<Order> ->
            val revenue = orders
              .filter { !it.statusEnum.isTerminalException }
              .sumOf { it.totalAmount }
            val newOrReview = orders.count {
              it.statusEnum == OrderStatus.NEW_ORDER || it.statusEnum == OrderStatus.UNDER_REVIEW
            }
            val inProgress = orders.count {
              it.statusEnum in listOf(
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.READY_FOR_DELIVERY,
                OrderStatus.OUT_FOR_DELIVERY,
              )
            }
            val delivered = orders.count { it.statusEnum == OrderStatus.DELIVERED }
            _uiState.update {
              it.copy(
                totalSalesRevenue = revenue,
                newAndReviewOrdersCount = newOrReview,
                inProgressOrdersCount = inProgress,
                deliveredOrdersCount = delivered,
              )
            }
          }
      }
    }

    val canViewCustomers = StaffPermission.MANAGE_CUSTOMERS in allowedPermissions ||
      StaffPermission.APPROVE_WHOLESALE in allowedPermissions ||
      StaffPermission.MANAGE_STAFF in allowedPermissions

    if (canViewCustomers) {
      viewModelScope.launch {
        staffRepository.observeAllUsers()
          .catch { }
          .collect { users: List<UserProfile> ->
            _uiState.update {
              it.copy(
                pendingWholesaleCount = users.count { u -> u.wholesaleStatus == "pending" },
                approvedWholesaleCount = users.count { u -> u.isWholesaleApproved },
                totalCustomersCount = users.size,
              )
            }
          }
      }
    }

    val canViewProducts = StaffPermission.MANAGE_PRODUCTS in allowedPermissions ||
      StaffPermission.MANAGE_INVENTORY in allowedPermissions ||
      StaffPermission.MANAGE_OFFERS in allowedPermissions ||
      StaffPermission.VIEW_REPORTS in allowedPermissions

    if (canViewProducts) {
      viewModelScope.launch {
        productRepository.observeAllProducts()
          .catch { }
          .collect { products: List<Product> ->
            _uiState.update {
              it.copy(
                lowStockCount = products.count { p -> p.isLowStock },
                outOfStockCount = products.count { p -> p.isOutOfStock },
                totalProductsCount = products.size,
              )
            }
          }
      }
    }
  }

  class Factory(
    private val context: Context,
    private val allowedPermissions: Set<StaffPermission>,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val appCtx = context.applicationContext
      return AdminDashboardViewModel(
        allowedPermissions = allowedPermissions,
        orderRepository = OrderRepository(appCtx),
        productRepository = ProductRepository(appCtx),
        staffRepository = StaffRepository(appCtx),
      ) as T
    }
  }
}
