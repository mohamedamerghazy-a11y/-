package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.example.data.model.NotificationType
import com.example.data.model.StaffMember
import com.example.data.model.UserProfile
import com.example.data.repository.NotificationRepository
import com.example.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminStaffUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val staffList: List<StaffMember> = emptyList(),
  val usersList: List<UserProfile> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val staffMap: Map<String, StaffMember>
    get() = staffList.associateBy { it.uid }
}

class AdminStaffViewModel(
  private val staffRepository: StaffRepository,
  private val notificationRepository: NotificationRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminStaffUiState())
  val uiState: StateFlow<AdminStaffUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      combine(
        staffRepository.observeAllStaff(),
        staffRepository.observeAllUsers(),
      ) { staff, users ->
        staff to users
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل بيانات الموظفين والمستخدمين."),
            )
          }
        }
        .collect { (staff, users) ->
          _uiState.update {
            it.copy(
              isLoading = false,
              staffList = staff,
              usersList = users,
            )
          }
        }
    }
  }

  fun assignStaff(
    targetUser: UserProfile,
    role: StaffRole,
    customPermissions: Set<StaffPermission> = role.permissions,
    isActive: Boolean = true,
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = staffRepository.assignStaffMember(
        targetUser = targetUser,
        role = role,
        customPermissions = customPermissions,
        isActive = isActive,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم تحديث صلاحيات الدور للمستخدم بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تعيين دور الموظف."),
            )
          },
        )
      }
    }
  }

  fun removeStaff(targetUid: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = staffRepository.removeStaffMember(targetUid)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم إزالة صلاحيات الموظف وإعادته كعميل عادي.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر إزالة الموظف."),
            )
          },
        )
      }
    }
  }

  fun updateWholesaleDecision(targetUid: String, approved: Boolean) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = staffRepository.updateWholesaleDecision(targetUid, approved)
      if (result.isSuccess && notificationRepository != null) {
        runCatching {
          notificationRepository.sendNotificationToUser(
            targetUid = targetUid,
            title = if (approved) "تم اعتماد حساب الجملة الخاص بك" else "تحديث طلب حساب الجملة",
            message = if (approved) {
              "مبروك! أصبحت أسعار الجملة المخصصة متاحة لك الآن عند التسوق وإتمام الطلبات."
            } else {
              "تعذر قبول طلب حساب الجملة حاليًا. يمكنك مراجعة بياناتك وإعادة التقديم من صفحة حسابي."
            },
            type = NotificationType.WHOLESALE_STATUS,
            referenceId = targetUid,
          )
        }
      }
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (approved) "تم اعتماد حساب الجملة بنجاح." else "تم رفض طلب حساب الجملة.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر تحديث حالة الجملة."),
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
      return AdminStaffViewModel(
        staffRepository = StaffRepository(appCtx),
        notificationRepository = NotificationRepository(appCtx),
      ) as T
    }
  }
}
