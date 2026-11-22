package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.model.OrderStatus
import com.example.data.model.CashVerificationLog
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.StorePaymentSettings
import com.example.data.repository.CouponAndDeliveryRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReceiptCleanupScope(val arabicLabel: String, val fixedDays: Int? = null) {
  OLDER_THAN_7_DAYS("أقدم من 7 أيام", fixedDays = 7),
  OLDER_THAN_30_DAYS("أقدم من 30 يومًا", fixedDays = 30),
  OLDER_THAN_90_DAYS("أقدم من 90 يومًا", fixedDays = 90),
  CUSTOM_DAYS("مدة مخصصة بالأيام", fixedDays = null),
  REVIEWED_ONLY("الطلبات المُراجعة (مؤكدة ومرفوضة)", fixedDays = null),
  ALL_RECEIPTS("جميع الإيصالات المرفقة", fixedDays = null),
}

object ReceiptCleanupEvaluator {
  private const val MILLIS_PER_DAY = 86_400_000L

  fun filterMatchingOrders(
    orders: List<Order>,
    scope: ReceiptCleanupScope,
    customDays: Int = 30,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<Order> {
    val withReceipts = orders.filter { !it.receiptImageUrl.isNullOrBlank() }
    if (withReceipts.isEmpty()) return emptyList()

    return when (scope) {
      ReceiptCleanupScope.ALL_RECEIPTS -> withReceipts
      ReceiptCleanupScope.REVIEWED_ONLY -> withReceipts.filter {
        it.paymentStatus == "verified" || it.paymentStatus == "rejected"
      }
      ReceiptCleanupScope.OLDER_THAN_7_DAYS,
      ReceiptCleanupScope.OLDER_THAN_30_DAYS,
      ReceiptCleanupScope.OLDER_THAN_90_DAYS,
      ReceiptCleanupScope.CUSTOM_DAYS,
      -> {
        val days = (scope.fixedDays ?: customDays).coerceAtLeast(0)
        val cutoffMillis = nowMillis - (days.toLong() * MILLIS_PER_DAY)
        withReceipts.filter { order ->
          val createdMillis = order.createdAt?.toDate()?.time
            ?: order.updatedAt?.toDate()?.time
            ?: return@filter false
          createdMillis <= cutoffMillis
        }
      }
    }
  }
}

data class AdminPaymentsUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val orders: List<Order> = emptyList(),
  val settings: StorePaymentSettings = StorePaymentSettings(),
  val cashVerificationLogs: List<CashVerificationLog> = emptyList(),
  val selectedPaymentStatusFilter: String? = null,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val allTransferOrders: List<Order>
    get() = orders.filter { order ->
      order.paymentMethod in listOf("vodafone_cash", "instapay") ||
        !order.senderPhone.isNullOrBlank() ||
        !order.transferReference.isNullOrBlank() ||
        !order.receiptImageUrl.isNullOrBlank()
    }

  val ordersWithReceiptsCount: Int
    get() = orders.count { !it.receiptImageUrl.isNullOrBlank() }

  val transferOrders: List<Order>
    get() = allTransferOrders.filter { order ->
      when (selectedPaymentStatusFilter) {
        null -> true
        "has_receipt" -> !order.receiptImageUrl.isNullOrBlank()
        else -> order.paymentStatus == selectedPaymentStatusFilter
      }
    }
}

class AdminPaymentsViewModel(
  private val orderRepository: OrderRepository,
  private val couponAndDeliveryRepository: CouponAndDeliveryRepository,
  private val notificationRepository: NotificationRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminPaymentsUiState())
  val uiState: StateFlow<AdminPaymentsUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      combine(
        orderRepository.observeAllOrders(),
        couponAndDeliveryRepository.observePaymentSettings(),
        orderRepository.observeCashVerificationLogs().catch { emit(emptyList()) },
      ) { orders, settings, logs ->
        Triple(orders, settings, logs)
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل بيانات المدفوعات."),
            )
          }
        }
        .collect { (orders, settings, logs) ->
          _uiState.update {
            it.copy(
              isLoading = false,
              orders = orders,
              settings = settings,
              cashVerificationLogs = logs,
            )
          }
        }
    }
  }

  fun setPaymentStatusFilter(status: String?) {
    _uiState.update { it.copy(selectedPaymentStatusFilter = status) }
  }

  fun updatePaymentStatus(orderId: String, newStatus: String, currentOrderStatus: OrderStatus, note: String?) {
    val targetOrder = _uiState.value.orders.firstOrNull { it.id == orderId }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = orderRepository.updateOrderStatus(
        orderId = orderId,
        newStatus = currentOrderStatus,
        statusNote = note,
        paymentStatus = newStatus,
      )
      if (result.isSuccess && targetOrder != null && notificationRepository != null && (newStatus == "verified" || newStatus == "rejected")) {
        val title = if (newStatus == "verified") {
          "تم تأكيد دفعة الطلب #${targetOrder.orderNumber}"
        } else {
          "تحديث حول دفع الطلب #${targetOrder.orderNumber}"
        }
        val msg = if (newStatus == "verified") {
          "تم التحقق من تحويلك المالي واعتماد الدفعة بنجاح."
        } else {
          val reason = note?.trim()?.takeIf { it.isNotEmpty() } ?: "يرجى مراجعة بيانات التحويل أو التواصل مع الدعم."
          "تعذر اعتماد إيصال التحويل المالي: $reason"
        }
        runCatching {
          notificationRepository.sendNotificationToUser(
            targetUid = targetOrder.userId,
            title = title,
            message = msg,
            type = NotificationType.PAYMENT_STATUS,
            referenceId = orderId,
          )
        }
      }
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            val statusLabel = when (newStatus) {
              "verified" -> "تم تأكيد الدفع"
              "rejected" -> "تم رفض الدفع"
              else -> "قيد المراجعة"
            }
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم تحديث حالة الدفع إلى ($statusLabel).",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحديث حالة الدفع."),
            )
          },
        )
      }
    }
  }

  fun deleteSingleReceipt(orderId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = orderRepository.deleteOrderReceiptImage(orderId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم حذف صورة الإيصال من الطلب بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف صورة الإيصال."),
            )
          },
        )
      }
    }
  }

  fun deleteReceiptsBatch(scope: ReceiptCleanupScope, customDays: Int = 30) {
    val matchingIds = ReceiptCleanupEvaluator.filterMatchingOrders(
      orders = _uiState.value.orders,
      scope = scope,
      customDays = customDays,
    ).map { it.id }

    if (matchingIds.isEmpty()) {
      _uiState.update { it.copy(feedbackMessage = "لا توجد إيصالات مطابقة للمدة المحددة.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = orderRepository.deleteOrderReceiptsBatch(matchingIds)
      _uiState.update { state ->
        result.fold(
          onSuccess = { count ->
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم حذف $count إيصال من سجلات الطلبات بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تنفيذ حذف الإيصالات المجمّع."),
            )
          },
        )
      }
    }
  }

  fun savePaymentSettings(newSettings: StorePaymentSettings) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = couponAndDeliveryRepository.savePaymentSettings(newSettings)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم حفظ إعدادات المحافظ ووسائل الدفع بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ إعدادات الدفع."),
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
      return AdminPaymentsViewModel(
        orderRepository = OrderRepository(appCtx),
        couponAndDeliveryRepository = CouponAndDeliveryRepository(appCtx),
        notificationRepository = NotificationRepository(appCtx),
      ) as T
    }
  }
}
