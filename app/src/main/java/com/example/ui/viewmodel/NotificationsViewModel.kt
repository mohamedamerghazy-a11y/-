package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.notifications.StoreNotificationDispatcher
import com.example.data.model.AppNotification
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
  private val dispatcher: StoreNotificationDispatcher? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(NotificationsUiState())
  val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

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
        dispatcher = StoreNotificationDispatcher(appCtx),
      ) as T
    }
  }
}
