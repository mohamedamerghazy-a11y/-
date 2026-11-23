package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.model.OrderStatus
import com.example.core.notifications.StoreNotificationDispatcher
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.repository.NotificationRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class NotificationsUiState(
  val isLoading: Boolean = true,
  val notifications: List<AppNotification> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val unreadCount: Int
    get() = notifications.count { !it.isRead }
}

class NotificationsViewModel(
  private val uid: String,
  private val repository: NotificationRepository,
  private val orderRepository: OrderRepository? = null,
  private val dispatcher: StoreNotificationDispatcher? = null,
  private val appContext: Context? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(NotificationsUiState())
  val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

  private var staffOrdersJob: Job? = null
  private var canManagePaymentsFlag: Boolean = false
  private var canManageOrdersFlag: Boolean = false
  private val inFlightNotifIds = mutableSetOf<String>()

  private val syncPrefs by lazy {
    appContext?.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
  }

  init {
    dispatcher?.ensureChannel()
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      repository.observeUserNotifications(uid)
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل الإشعارات."),
            )
          }
        }
        .collect { list ->
          dispatcher?.dispatchNewUnreadNotifications(uid, list)
          _uiState.update {
            it.copy(
              isLoading = false,
              notifications = list,
            )
          }
        }
    }
  }

  /**
   * يفعّل مراقبة الطلبات الواردة لحظياً لصلاحيات المدفوعات/الطلبات حتى يصل للمحاسب أو المدير أو المالك
   * إشعار فوري عند وجود طلب جديد بانتظار تأكيد الدفع (أو طلب دفع عند الاستلام لمسؤولي الطلبات).
   */
  fun setStaffCapabilities(canManagePayments: Boolean, canManageOrders: Boolean) {
    if (canManagePaymentsFlag == canManagePayments && canManageOrdersFlag == canManageOrders) {
      return
    }
    canManagePaymentsFlag = canManagePayments
    canManageOrdersFlag = canManageOrders

    staffOrdersJob?.cancel()
    staffOrdersJob = null

    val repo = orderRepository ?: return
    if (!canManagePayments && !canManageOrders) return

    staffOrdersJob = viewModelScope.launch {
      repo.observeAllOrders()
        .catch { /* لا نُعطل شاشة الإشعارات إن لم تتوفر الصلاحية مؤقتاً */ }
        .collect { orders ->
          syncIncomingStaffOrderAlerts(
            orders = orders,
            canManagePayments = canManagePayments,
            canManageOrders = canManageOrders,
          )
        }
    }
  }

  private suspend fun syncIncomingStaffOrderAlerts(
    orders: List<Order>,
    canManagePayments: Boolean,
    canManageOrders: Boolean,
  ) {
    if (uid.isBlank() || orders.isEmpty()) return
    val prefsKey = "synced_staff_order_notifs_$uid"
    val syncedIds = syncPrefs?.getStringSet(prefsKey, emptySet()).orEmpty().toMutableSet()
    val existingNotifIds = _uiState.value.notifications.map { it.id }.toSet()
    var syncedChanged = false

    val nowMillis = System.currentTimeMillis()
    val maxAgeMillis = 7L * 24L * 60L * 60L * 1000L

    orders.forEach { order ->
      if (order.id.isBlank()) return@forEach
      val status = order.statusEnum
      if (status in setOf(OrderStatus.CANCELLED, OrderStatus.RETURNED, OrderStatus.DELIVERED)) {
        return@forEach
      }
      val createdMillis = order.createdAt?.toDate()?.time
      if (createdMillis != null && (nowMillis - createdMillis) > maxAgeMillis) {
        return@forEach
      }

      val isElectronicTransfer = order.paymentMethod != "cod"

      // 1. طلب تحويل إلكتروني بانتظار تأكيد الدفع -> إشعار لمسؤولي المدفوعات (المالك، المدير، المحاسب)
      if (canManagePayments && isElectronicTransfer && order.paymentStatus == "pending_review") {
        val notifId = "pay_review_${order.id}"
        if (notifId !in syncedIds && notifId !in existingNotifIds && inFlightNotifIds.add(notifId)) {
          val amountStr = formatAmount(order.totalAmount)
          val senderPart = order.displaySenderPhone?.takeIf { it.isNotBlank() }?.let { " • محوّل من: $it" }.orEmpty()
          val receiptPart = if (!order.receiptImageUrl.isNullOrBlank()) " • مرفق صورة إيصال" else ""
          val title = "طلب بانتظار تأكيد الدفع #${order.orderNumber}"
          val message = "العميل: ${order.customerName.ifBlank { order.recipientName }} • المبلغ: $amountStr ج.م • ${order.paymentMethodEnum.arabicLabel}$senderPart$receiptPart"

          val result = repository.ensureDeterministicNotification(
            targetUid = uid,
            notifId = notifId,
            title = title,
            message = message,
            type = NotificationType.PAYMENT_STATUS,
            referenceId = order.id,
          )
          inFlightNotifIds.remove(notifId)
          if (result.isSuccess) {
            syncedIds.add(notifId)
            syncedChanged = true
          }
        }
      }

      // 2. طلب دفع عند الاستلام جديد -> إشعار لمسؤولي الطلبات
      if (
        canManageOrders &&
        !isElectronicTransfer &&
        status in setOf(OrderStatus.NEW_ORDER, OrderStatus.UNDER_REVIEW)
      ) {
        val notifId = "cod_order_${order.id}"
        if (notifId !in syncedIds && notifId !in existingNotifIds && inFlightNotifIds.add(notifId)) {
          val amountStr = formatAmount(order.totalAmount)
          val title = "طلب جديد (دفع عند الاستلام) #${order.orderNumber}"
          val message = "العميل: ${order.customerName.ifBlank { order.recipientName }} • الإجمالي: $amountStr ج.م • ${order.items.size} صنف — جاهز للمراجعة والتجهيز."

          val result = repository.ensureDeterministicNotification(
            targetUid = uid,
            notifId = notifId,
            title = title,
            message = message,
            type = NotificationType.ORDER_STATUS,
            referenceId = order.id,
          )
          inFlightNotifIds.remove(notifId)
          if (result.isSuccess) {
            syncedIds.add(notifId)
            syncedChanged = true
          }
        }
      }
    }

    if (syncedChanged) {
      val trimmed = syncedIds.toList().takeLast(300).toSet()
      syncPrefs?.edit()?.putStringSet(prefsKey, trimmed)?.apply()
    }
  }

  private fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
      amount.toLong().toString()
    } else {
      String.format(Locale.US, "%.2f", amount)
    }
  }

  fun markAsRead(notifId: String) {
    viewModelScope.launch {
      repository.markAsRead(uid, notifId)
    }
  }

  fun markAllAsRead() {
    val unreadIds = _uiState.value.notifications.filter { !it.isRead }.map { it.id }
    if (unreadIds.isEmpty()) return
    viewModelScope.launch {
      repository.markAllAsRead(uid, unreadIds)
        .onSuccess {
          _uiState.update { it.copy(feedbackMessage = "تم تحديد جميع الإشعارات كمقروءة.") }
        }
        .onFailure { err ->
          _uiState.update { it.copy(errorMessage = err.toArabicErrorMessage("تعذر تحديث الإشعارات.")) }
        }
    }
  }

  fun deleteNotification(notifId: String) {
    viewModelScope.launch {
      repository.deleteNotification(uid, notifId)
        .onFailure { err ->
          _uiState.update { it.copy(errorMessage = err.toArabicErrorMessage("تعذر حذف الإشعار.")) }
        }
    }
  }

  fun clearMessages() {
    _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
  }

  class Factory(
    private val context: Context,
    private val uid: String,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val appCtx = context.applicationContext
      return NotificationsViewModel(
        uid = uid,
        repository = NotificationRepository(appCtx),
        orderRepository = OrderRepository(appCtx),
        dispatcher = StoreNotificationDispatcher(appCtx),
        appContext = appCtx,
      ) as T
    }
  }

  companion object {
    private const val SYNC_PREFS_NAME = "aleman_staff_order_notif_sync_v1"
  }
}
