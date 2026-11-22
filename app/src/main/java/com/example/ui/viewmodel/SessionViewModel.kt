package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.example.data.model.DeliveryAddress
import com.example.data.model.StaffMember
import com.example.data.model.UserProfile
import com.example.data.repository.BannerAndStoreRepository
import com.example.data.repository.StaffRepository
import com.example.data.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProfileLoadState {
  data object Loading : ProfileLoadState
  data object NeedsSetup : ProfileLoadState
  data class Loaded(val profile: UserProfile) : ProfileLoadState
  data class Error(val message: String) : ProfileLoadState
}

data class SessionUiState(
  val uid: String = "",
  val profileState: ProfileLoadState = ProfileLoadState.Loading,
  val staffMember: StaffMember? = null,
  val configuredOwnerEmail: String = UserRepository.BOOTSTRAPPED_OWNER_EMAIL,
  val isWholesalePreviewEnabled: Boolean = false,
  val addresses: List<DeliveryAddress> = emptyList(),
  val isSubmitting: Boolean = false,
  val isDeletingAccount: Boolean = false,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val defaultAddress: DeliveryAddress?
    get() = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()

  val isConfiguredOwner: Boolean
    get() {
      val profile = (profileState as? ProfileLoadState.Loaded)?.profile ?: return false
      val email = profile.email.trim()
      if (email.isEmpty()) return false
      return email.equals(UserRepository.BOOTSTRAPPED_OWNER_EMAIL, ignoreCase = true) ||
        (configuredOwnerEmail.isNotBlank() && email.equals(configuredOwnerEmail.trim(), ignoreCase = true))
    }

  val effectivePermissions: Set<StaffPermission>
    get() {
      if (isConfiguredOwner) return StaffRole.OWNER.permissions
      val staff = staffMember
      if (staff != null) {
        return if (staff.isActive) staff.permissionSet else emptySet()
      }
      val profile = (profileState as? ProfileLoadState.Loaded)?.profile ?: return emptySet()
      return profile.staffRoleEnum.permissions
    }

  val canAccessAdminDashboard: Boolean
    get() = effectivePermissions.isNotEmpty()

  val canToggleWholesalePreview: Boolean
    get() = effectivePermissions.contains(StaffPermission.PREVIEW_WHOLESALE_STORE) ||
      effectivePermissions.contains(StaffPermission.MANAGE_PRODUCTS)

  val effectiveWholesaleView: Boolean
    get() {
      val profile = (profileState as? ProfileLoadState.Loaded)?.profile
      return (profile?.isWholesaleApproved == true) ||
        (canToggleWholesalePreview && isWholesalePreviewEnabled)
    }
}

class SessionViewModel(
  private val uid: String,
  private val repository: UserRepository,
  private val staffRepository: StaffRepository,
  private val auth: FirebaseAuth = Firebase.auth,
  private val bannerAndStoreRepository: BannerAndStoreRepository? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SessionUiState(uid = uid))
  val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

  private var profileJob: Job? = null
  private var staffJob: Job? = null
  private var addressesJob: Job? = null
  private var storeSettingsJob: Job? = null

  init {
    startObserving()
  }

  fun retryLoad() {
    _uiState.update { it.copy(profileState = ProfileLoadState.Loading, errorMessage = null) }
    startObserving()
  }

  private fun startObserving() {
    profileJob?.cancel()
    staffJob?.cancel()
    addressesJob?.cancel()
    storeSettingsJob?.cancel()

    profileJob = viewModelScope.launch {
      repository.observeUserProfile(uid)
        .catch { error ->
          _uiState.update {
            it.copy(
              profileState = ProfileLoadState.Error(
                error.toArabicErrorMessage("تعذر تحميل بيانات الحساب حاليًا."),
              ),
            )
          }
        }
        .collect { profile ->
          _uiState.update {
            it.copy(
              profileState = if (profile == null) {
                ProfileLoadState.NeedsSetup
              } else {
                ProfileLoadState.Loaded(profile)
              },
            )
          }
        }
    }

    staffJob = viewModelScope.launch {
      staffRepository.observeStaffMember(uid)
        .catch {
          // في حال لم يكن لدى العميل سجل موظف نكتفي بقيمة null
        }
        .collect { staff ->
          _uiState.update { it.copy(staffMember = staff) }
        }
    }

    storeSettingsJob = bannerAndStoreRepository?.let { storeRepo ->
      viewModelScope.launch {
        storeRepo.observeStoreGeneralSettings()
          .catch { }
          .collect { settings ->
            val ownerMail = settings.ownerEmail.trim().ifEmpty { UserRepository.BOOTSTRAPPED_OWNER_EMAIL }
            _uiState.update { it.copy(configuredOwnerEmail = ownerMail) }
          }
      }
    }

    addressesJob = viewModelScope.launch {
      repository.observeAddresses(uid)
        .catch { error ->
          _uiState.update {
            it.copy(
              errorMessage = error.toArabicErrorMessage("تعذر تحميل عناوين التوصيل."),
            )
          }
        }
        .collect { list ->
          _uiState.update { it.copy(addresses = list) }
        }
    }
  }

  fun toggleWholesalePreview() {
    if (!_uiState.value.canToggleWholesalePreview) return
    _uiState.update {
      val next = !it.isWholesalePreviewEnabled
      it.copy(
        isWholesalePreviewEnabled = next,
        feedbackMessage = if (next) {
          "تم التبديل إلى معاينة أسعار الجملة"
        } else {
          "تم التبديل إلى أسعار القطاعي"
        },
      )
    }
  }

  fun createInitialProfile(
    fullName: String,
    phone: String,
    requestWholesale: Boolean,
    companyName: String?,
  ) {
    val user = auth.currentUser ?: return
    val cleanName = fullName.trim()
    val cleanPhone = phone.trim()
    val cleanCompany = companyName?.trim()

    if (cleanName.length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال الاسم الكامل بشكل صحيح.") }
      return
    }
    if (cleanPhone.length < 8) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال رقم هاتف صحيح للتواصل وتوصيل الطلبات.") }
      return
    }
    if (requestWholesale && cleanCompany.isNullOrBlank()) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم المحل أو الشركة لطلب حساب الجملة.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.createUserProfile(
        fullName = cleanName,
        email = user.email.orEmpty(),
        phone = cleanPhone,
        photoUrl = user.photoUrl?.toString(),
        requestWholesale = requestWholesale,
        companyName = cleanCompany,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (requestWholesale) {
                "تم حفظ حسابك وإرسال طلب اعتماد الجملة للإدارة بنجاح."
              } else {
                "مرحبًا بك في الإيمان للتجارة! تم تجهيز حسابك بنجاح."
              },
            )
          },
          onFailure = { error ->
            state.copy(
              isSubmitting = false,
              errorMessage = error.toArabicErrorMessage("تعذر حفظ بيانات الحساب، حاول مرة أخرى."),
            )
          },
        )
      }
    }
  }

  fun updateProfile(
    fullName: String,
    phone: String,
    companyName: String?,
  ) {
    val cleanName = fullName.trim()
    val cleanPhone = phone.trim()
    if (cleanName.length < 2) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال الاسم الكامل بشكل صحيح.") }
      return
    }
    if (cleanPhone.length < 8) {
      _uiState.update { it.copy(errorMessage = "يرجى إدخال رقم هاتف صحيح.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.updateProfileDetails(
        fullName = cleanName,
        phone = cleanPhone,
        companyName = companyName,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم تحديث البيانات الشخصية بنجاح.",
            )
          },
          onFailure = { error ->
            state.copy(
              isSubmitting = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحديث البيانات حاليًا."),
            )
          },
        )
      }
    }
  }

  fun submitWholesaleRequest(
    fullName: String,
    phone: String,
    companyName: String,
  ) {
    val cleanName = fullName.trim()
    val cleanPhone = phone.trim()
    val cleanCompany = companyName.trim()

    if (cleanName.length < 2 || cleanPhone.length < 8 || cleanCompany.length < 2) {
      _uiState.update {
        it.copy(errorMessage = "يرجى استكمال الاسم ورقم الهاتف واسم المحل أو الشركة لإرسال الطلب.")
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.submitWholesaleRequest(
        fullName = cleanName,
        phone = cleanPhone,
        companyName = cleanCompany,
      )
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم إرسال طلب اعتماد الجملة، وستتم مراجعته من الإدارة قريبًا.",
            )
          },
          onFailure = { error ->
            state.copy(
              isSubmitting = false,
              errorMessage = error.toArabicErrorMessage("تعذر إرسال طلب الجملة حاليًا."),
            )
          },
        )
      }
    }
  }

  fun saveAddress(address: DeliveryAddress) {
    if (
      address.label.isBlank() ||
      address.recipientName.isBlank() ||
      address.phone.trim().length < 8 ||
      address.governorate.isBlank() ||
      address.city.isBlank() ||
      address.streetAddress.isBlank()
    ) {
      _uiState.update { it.copy(errorMessage = "يرجى استكمال جميع حقول العنوان المطلوبة بشكل صحيح.") }
      return
    }

    val shouldBeDefault = address.isDefault || _uiState.value.addresses.isEmpty()

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.saveAddress(address.copy(isDefault = shouldBeDefault))
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = if (address.id.isBlank()) {
                "تم إضافة عنوان التوصيل بنجاح."
              } else {
                "تم تحديث عنوان التوصيل بنجاح."
              },
            )
          },
          onFailure = { error ->
            state.copy(
              isSubmitting = false,
              errorMessage = error.toArabicErrorMessage("تعذر حفظ العنوان حاليًا."),
            )
          },
        )
      }
    }
  }

  fun setDefaultAddress(address: DeliveryAddress) {
    if (address.isDefault) return
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.saveAddress(address.copy(isDefault = true))
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم تعيين «${address.label}» كعنوان توصيل افتراضي.",
            )
          },
          onFailure = { error ->
            state.copy(
              isSubmitting = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحديث العنوان الافتراضي."),
            )
          },
        )
      }
    }
  }

  fun deleteAddress(addressId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null, feedbackMessage = null) }
      val result = repository.deleteAddress(addressId)
      _uiState.update { state ->
        result.fold(
          onSuccess = {
            state.copy(
              isSubmitting = false,
              feedbackMessage = "تم حذف عنوان التوصيل بنجاح.",
            )
          },
          onFailure = { error ->
            state.copy(
              isSubmitting = false,
              errorMessage = error.toArabicErrorMessage("تعذر حذف العنوان حاليًا."),
            )
          },
        )
      }
    }
  }

  fun isReauthRequiredForDeletion(): Boolean =
    repository.isRecentLoginRequiredForDeletion()

  fun deleteOwnAccount(
    onRequiresRecentLogin: () -> Unit,
    onAccountDeleted: () -> Unit,
  ) {
    if (_uiState.value.isDeletingAccount) return
    if (repository.isRecentLoginRequiredForDeletion()) {
      onRequiresRecentLogin()
      return
    }

    profileJob?.cancel()
    staffJob?.cancel()
    addressesJob?.cancel()
    storeSettingsJob?.cancel()

    viewModelScope.launch {
      _uiState.update { it.copy(isDeletingAccount = true, errorMessage = null, feedbackMessage = null) }
      when (val outcome = repository.deleteCurrentAccount()) {
        UserRepository.DeleteAccountOutcome.Deleted -> {
          onAccountDeleted()
        }
        UserRepository.DeleteAccountOutcome.RequiresRecentLogin -> {
          _uiState.update { it.copy(isDeletingAccount = false) }
          startObserving()
          onRequiresRecentLogin()
        }
        is UserRepository.DeleteAccountOutcome.Failure -> {
          _uiState.update {
            it.copy(
              isDeletingAccount = false,
              errorMessage = outcome.message,
            )
          }
          startObserving()
        }
      }
    }
  }

  fun reportErrorMessage(message: String) {
    _uiState.update { it.copy(errorMessage = message) }
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
      return SessionViewModel(
        uid = uid,
        repository = UserRepository(appCtx),
        staffRepository = StaffRepository(appCtx),
        bannerAndStoreRepository = BannerAndStoreRepository(appCtx),
      ) as T
    }
  }
}
