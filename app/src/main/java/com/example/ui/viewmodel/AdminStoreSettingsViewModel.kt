package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.model.NotificationType
import com.example.data.model.StoreCompany
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.UserProfile
import com.example.data.repository.BannerAndStoreRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminStoreSettingsUiState(
  val isLoading: Boolean = true,
  val isSubmitting: Boolean = false,
  val isBroadcasting: Boolean = false,
  val settings: StoreGeneralSettings = StoreGeneralSettings(),
  val companies: List<StoreCompany> = emptyList(),
  val users: List<UserProfile> = emptyList(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val customersCount: Int
    get() = users.size
}

class AdminStoreSettingsViewModel(
  private val bannerAndStoreRepository: BannerAndStoreRepository,
  private val notificationRepository: NotificationRepository,
  private val staffRepository: StaffRepository,
  private val productRepository: ProductRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminStoreSettingsUiState())
  val uiState: StateFlow<AdminStoreSettingsUiState> = _uiState.asStateFlow()

  init {
    startObserving()
  }

  private fun startObserving() {
    viewModelScope.launch {
      combine(
        bannerAndStoreRepository.observeStoreGeneralSettings(),
        bannerAndStoreRepository.observeAllCompanies().catch { emit(emptyList()) },
        staffRepository.observeAllUsers().catch { emit(emptyList()) },
      ) { settings, companies, users ->
        Triple(settings, companies, users)
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل إعدادات المتجر العامة."),
            )
          }
        }
        .collect { (settings, companies, users) ->
          _uiState.update {
            it.copy(
              isLoading = false,
              settings = settings,
              companies = companies,
              users = users,
            )
          }
        }
    }
  }

  fun saveSettings(settings: StoreGeneralSettings) {
    if (settings.storeName.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "أدخل اسم المتجر (حرفين على الأقل).") }
      return
    }
    if (settings.freeShippingEnabled && settings.freeShippingThreshold <= 0.0) {
      _uiState.update { it.copy(errorMessage = "أدخل قيمة الحد الأدنى للشحن المجاني أكبر من صفر.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = bannerAndStoreRepository.saveStoreGeneralSettings(settings)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم حفظ إعدادات المتجر بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ إعدادات المتجر."),
            )
          },
        )
      }
    }
  }

  fun sendBroadcastNotification(
    title: String,
    message: String,
    onlyWholesale: Boolean,
    type: NotificationType,
  ) {
    val cleanTitle = title.trim()
    val cleanMessage = message.trim()
    if (cleanTitle.isEmpty() || cleanMessage.isEmpty()) {
      _uiState.update { it.copy(errorMessage = "أدخل عنوان الإشعار ونص الرسالة قبل الإرسال.") }
      return
    }
    val targetUids = _uiState.value.users
      .filter { !onlyWholesale || it.isWholesaleApproved }
      .map { it.uid }
      .distinct()

    if (targetUids.isEmpty()) {
      _uiState.update { it.copy(errorMessage = "لا يوجد عملاء متاحون لإرسال الإشعار حاليًا.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isBroadcasting = true, errorMessage = null, feedbackMessage = null) }
      val result = notificationRepository.broadcastNotificationToUsers(
        targetUids = targetUids,
        title = cleanTitle,
        message = cleanMessage,
        type = type,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = { count ->
            state.copy(
              isBroadcasting = false,
              feedbackMessage = "تم إرسال الإشعار إلى $count عميل بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isBroadcasting = false,
              errorMessage = err.toArabicErrorMessage("تعذر إرسال الإشعار الجماعي."),
            )
          },
        )
      }
    }
  }

  fun saveCompany(company: StoreCompany) {
    if (company.name.trim().length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم الشركة (حرفين على الأقل).") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = bannerAndStoreRepository.saveCompany(company)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (company.id.isBlank()) "تمت إضافة الشركة بنجاح." else "تم تحديث بيانات الشركة بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حفظ بيانات الشركة حاليًا."),
            )
          },
        )
      }
    }
  }

  fun deleteCompany(companyId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val allProducts = productRepository?.observeAllProducts()?.firstOrNull().orEmpty()
      for (prod in allProducts) {
        if (prod.companyId == companyId) {
          productRepository?.saveProduct(
            prod.copy(
              companyId = null,
              exclusiveToBrandShowcase = if (prod.brandShowcaseId.isNullOrBlank()) false else prod.exclusiveToBrandShowcase,
            ),
          )
        }
      }
      val result = bannerAndStoreRepository.deleteCompany(companyId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم حذف الشركة من الإعدادات بنجاح.",
            )
          },
          onFailure = { err ->
            state.copy(
              isSubmitting = false,
              errorMessage = err.toArabicErrorMessage("تعذر حذف الشركة حاليًا."),
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
      return AdminStoreSettingsViewModel(
        bannerAndStoreRepository = BannerAndStoreRepository(appCtx),
        notificationRepository = NotificationRepository(appCtx),
        staffRepository = StaffRepository(appCtx),
        productRepository = ProductRepository(appCtx),
      ) as T
    }
  }
}
