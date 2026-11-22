package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.model.OrderStatus
import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.StaffMember
import com.example.data.repository.NotificationRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminOrdersUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val orders: List<Order> = emptyList(),
  val deliveryDrivers: List<StaffMember> = emptyList(),
  val selectedStatusFilter: OrderStatus? = null,
  val searchQuery: String = "",
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val filteredOrders: List<Order>
    get() {
      val q = searchQuery.trim().lowercase()
      return orders.filter { order ->
        val matchesStatus = selectedStatusFilter == null || order.statusEnum == selectedStatusFilter
        val matchesQuery = q.isEmpty() ||
          order.orderNumber.lowercase().contains(q) ||
          order.customerName.lowercase().contains(q) ||
          order.phone.contains(q) ||
          order.governorate.lowercase().contains(q) ||
          order.assignedDriverName.orEmpty().lowercase().contains(q)
        matchesStatus && matchesQuery
      }
    }
}

class AdminOrdersViewModel(
  private val orderRepository: OrderRepository,
  private val notificationRepository: NotificationRepository? = null,
  private val staffRepository: StaffRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminOrdersUiState())
  val uiState: StateFlow<AdminOrdersUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      orderRepository.observeAllOrders()
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل قائمة طلبات المتجر."),
            )
          }
        }
        .collect { orders ->
          _uiState.update {
            it.copy(
              isLoading = false,
              orders = orders,
            )
          }
        }
    }

    staffRepository?.let { repo ->
      viewModelScope.launch {
        repo.observeAllStaff()
          .catch { }
          .collect { staffList ->
            val drivers = staffList.filter { member ->
              member.isActive && (
                member.staffRoleEnum == StaffRole.DELIVERY_DRIVER ||
                  member.permissionSet.contains(StaffPermission.DELIVER_ORDERS)
                )
            }
            _uiState.update { it.copy(deliveryDrivers = drivers) }
          }
      }
    }
  }

  fun setStatusFilter(status: OrderStatus?) {
    _uiState.update { it.copy(selectedStatusFilter = status) }
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun updateOrderStatus(
    orderId: String,
    newStatus: OrderStatus,
    statusNote: String? = null,
    paymentStatus: String? = null,
  ) {
    val targetOrder = _uiState.value.orders.firstOrNull { it.id == orderId }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val statusLabel = newStatus.labelFor(targetOrder?.isStorePickup == true)
      val result = orderRepository.updateOrderStatus(orderId, newStatus, statusNote, paymentStatus)
      if (result.isSuccess && targetOrder != null && notificationRepository != null) {
        val noteLine = statusNote?.trim()?.takeIf { it.isNotEmpty() }?.let { "\nملاحظة: $it" }.orEmpty()
        runCatching {
          notificationRepository.sendNotificationToUser(
            targetUid = targetOrder.userId,
            title = "تحديث حالة الطلب #${targetOrder.orderNumber}",
            message = "أصبحت حالة طلبك الآن: $statusLabel.$noteLine",
            type = NotificationType.ORDER_STATUS,
            referenceId = orderId,
          )
        }
      }
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم تحديث حالة الطلب إلى ($statusLabel) بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحديث حالة الطلب."),
            )
          },
        )
      }
    }
  }

  fun assignOrderToDriver(order: Order, driver: StaffMember) {
    if (driver.uid.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = orderRepository.assignOrderToDriver(
        orderId = order.id,
        driverId = driver.uid,
        driverName = driver.fullName,
        driverPhone = null,
      )
      if (result.isSuccess && notificationRepository != null) {
        val locationText = buildString {
          append(order.governorate)
          if (order.city.isNotBlank()) append(" - ${order.city}")
          if (order.streetAddress.isNotBlank()) append(" - ${order.streetAddress}")
        }
        runCatching {
          notificationRepository.sendNotificationToUser(
            targetUid = driver.uid,
            title = "طلب توصيل جديد #${order.orderNumber}",
            message = "العميل: ${order.recipientName} | الإجمالي: ${order.totalAmount} ج.م | العنوان: $locationText",
            type = NotificationType.ORDER_STATUS,
            referenceId = order.id,
          )
        }
        runCatching {
          notificationRepository.sendNotificationToUser(
            targetUid = order.userId,
            title = "طلبك #${order.orderNumber} خرج للتوصيل",
            message = "تم توجيه طلبك مع المندوب ${driver.fullName}.",
            type = NotificationType.ORDER_STATUS,
            referenceId = order.id,
          )
        }
      }
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم توجيه الطلب #${order.orderNumber} إلى المندوب «${driver.fullName}» وإرسال إشعار له.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر توجيه الطلب لموظف التوصيل."),
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
      return AdminOrdersViewModel(
        orderRepository = OrderRepository(appCtx),
        notificationRepository = NotificationRepository(appCtx),
        staffRepository = StaffRepository(appCtx),
      ) as T
    }
  }
}
