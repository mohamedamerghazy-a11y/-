package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.model.OrderStatus
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.normalizeOtpDigits
import com.example.data.repository.NotificationRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeliveryDriverUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val orders: List<Order> = emptyList(),
  val showCompleted: Boolean = false,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val activeOrders: List<Order>
    get() = orders.filter {
      it.statusEnum != OrderStatus.DELIVERED &&
        it.statusEnum != OrderStatus.CANCELLED &&
        it.statusEnum != OrderStatus.RETURNED
    }

  val completedOrders: List<Order>
    get() = orders.filter {
      it.statusEnum == OrderStatus.DELIVERED ||
        it.statusEnum == OrderStatus.CANCELLED ||
        it.statusEnum == OrderStatus.RETURNED
    }

  val visibleOrders: List<Order>
    get() = if (showCompleted) completedOrders else activeOrders
}

class DeliveryDriverViewModel(
  private val driverUid: String,
  private val orderRepository: OrderRepository,
  private val notificationRepository: NotificationRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(DeliveryDriverUiState())
  val uiState: StateFlow<DeliveryDriverUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      orderRepository.observeDriverOrders(driverUid)
        .catch { err ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحميل الطلبات الموجهة إليك."),
            )
          }
        }
        .collect { list ->
          _uiState.update {
            it.copy(
              isLoading = false,
              orders = list,
            )
          }
        }
    }
  }

  fun setShowCompleted(show: Boolean) {
    _uiState.update { it.copy(showCompleted = show) }
  }

  fun markOrderDelivered(order: Order, note: String? = null, enteredOtp: String? = null) {
    val expectedOtp = order.deliveryOtp?.trim().orEmpty()
    val requiresOtp = expectedOtp.isNotEmpty()
    if (requiresOtp) {
      val cleanEntered = normalizeOtpDigits(enteredOtp.orEmpty())
      if (cleanEntered.length != 4 || cleanEntered != normalizeOtpDigits(expectedOtp)) {
        _uiState.update {
          it.copy(errorMessage = "كود تأكيد الاستلام غير صحيح، يرجى طلب الكود الصحيح من العميل.")
        }
        return
      }
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = orderRepository.markOrderDeliveredByDriver(
        orderId = order.id,
        note = note,
        deliveryOtpVerified = if (requiresOtp) true else null,
      )
      if (result.isSuccess && notificationRepository != null) {
        runCatching {
          notificationRepository.sendNotificationToUser(
            targetUid = order.userId,
            title = "تم تسليم طلبك #${order.orderNumber}",
            message = "نشكركم على تسوقكم من الإيمان للتجارة، تم تسليم الطلب بنجاح.",
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
              feedbackMessage = "تم تسجيل تسليم الطلب #${order.orderNumber} بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحديث حالة تسليم الطلب."),
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
    private val driverUid: String,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val appCtx = context.applicationContext
      return DeliveryDriverViewModel(
        driverUid = driverUid,
        orderRepository = OrderRepository(appCtx),
        notificationRepository = NotificationRepository(appCtx),
      ) as T
    }
  }
}
