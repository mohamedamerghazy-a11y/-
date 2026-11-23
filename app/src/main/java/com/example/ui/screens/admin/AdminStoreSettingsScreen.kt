package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.core.util.AppIconManager
import com.example.core.util.ArabicFormatter
import com.example.data.media.CloudinaryFolder
import com.example.data.model.AppIconTheme
import com.example.data.model.BannerPlacement
import com.example.data.model.BrandShowcase
import com.example.data.model.CategoriesTabStyle
import com.example.data.model.Category
import com.example.data.model.CompanyOfferBadgeStyle
import com.example.data.model.IntroMediaType
import com.example.data.model.IntroReelTargetType
import com.example.data.model.Product
import com.example.data.model.TickerFontWeight
import com.example.ui.components.AnimatedImagePickerUploadField
import com.example.data.model.HomeCategoryStyle
import com.example.data.model.NotificationType
import com.example.data.model.NumeralFormat
import com.example.data.model.StoreCompany
import com.example.data.model.StoreContactChannel
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.StorePhoneEntry
import com.example.data.model.StoreSocialLink
import com.example.data.model.sanitizeCardHexColor
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.CompanySpecialOfferBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.ImagePickerUploadModal
import com.example.ui.components.SuccessBadge
import com.example.ui.components.VideoPickerUploadField
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.data.model.CashVerifierAccount
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminCashTrackingUiState
import com.example.ui.viewmodel.AdminStoreSettingsUiState
import com.example.ui.viewmodel.CashDateFilterMode
import com.example.ui.viewmodel.CashDeviceSummary

private enum class StoreSettingsSection {
  GENERAL_AND_OWNER,
  APP_ICON_THEME,
  INTRO_VIDEO,
  ANNOUNCEMENT_TICKER,
  COMPANIES,
  CONTACT_AND_ABOUT,
  HOME_HERO_AND_SECTIONS,
  PRODUCT_CARD_STYLE,
  CATEGORY_STYLES,
  POLICIES_AND_COMPLIANCE,
  BROADCAST_NOTIFICATIONS,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminStoreSettingsScreen(
  uiState: AdminStoreSettingsUiState,
  canManagePayments: Boolean,
  onSaveSettings: (StoreGeneralSettings) -> Unit,
  onSaveCompany: (StoreCompany) -> Unit = {},
  onDeleteCompany: (String) -> Unit = {},
  onSendBroadcast: (title: String, message: String, onlyWholesale: Boolean, type: NotificationType) -> Unit,
  onOpenPaymentSettings: () -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
  canToggleWholesalePreview: Boolean = false,
  isWholesalePreviewActive: Boolean = false,
  onToggleWholesalePreview: () -> Unit = {},
  initialTab: Int = 0,
  canEditStoreSettings: Boolean = true,
  canManageCashAccounts: Boolean = true,
  canSettleCashDevice: Boolean = false,
  actorDisplayName: String = "",
  cashTrackingUiState: AdminCashTrackingUiState = AdminCashTrackingUiState(isLoading = false),
  onSelectCashFilterMode: (CashDateFilterMode) -> Unit = {},
  onSelectCashSingleDay: (Long) -> Unit = {},
  onSelectCashDateRange: (Long, Long) -> Unit = { _, _ -> },
  onSaveCashAccount: (email: String, deviceName: String, phoneNumber: String?, isActive: Boolean, isEdit: Boolean) -> Unit = { _, _, _, _, _ -> },
  onToggleCashAccount: (CashVerifierAccount) -> Unit = {},
  onDeleteCashAccount: (CashVerifierAccount) -> Unit = {},
  onSettleCashDevice: (CashDeviceSummary, String, String?) -> Unit = { _, _, _ -> },
  onExportCashExcelToUri: (Uri) -> Unit = {},
  onShareCashExcel: ((Intent) -> Unit) -> Unit = {},
  onClearCashMessages: () -> Unit = {},
  reelCategories: List<Category> = emptyList(),
  reelProducts: List<Product> = emptyList(),
  reelBrandShowcases: List<BrandShowcase> = emptyList(),
) {
  var selectedTab by rememberSaveable(initialTab, canEditStoreSettings) {
    mutableIntStateOf(if (!canEditStoreSettings) 1 else initialTab.coerceIn(0, 1))
  }
  val effectiveTab = if (!canEditStoreSettings) 1 else selectedTab
  var expandedSection by rememberSaveable { mutableStateOf<StoreSettingsSection?>(null) }
  var editingCompany by remember { mutableStateOf<StoreCompany?>(null) }
  var showCompanyDialog by remember { mutableStateOf(false) }
  var deletingCompany by remember { mutableStateOf<StoreCompany?>(null) }

  var storeName by remember(uiState.settings) { mutableStateOf(uiState.settings.storeName) }
  var tagline by remember(uiState.settings) { mutableStateOf(uiState.settings.tagline) }
  var ownerEmail by remember(uiState.settings) { mutableStateOf(uiState.settings.ownerEmail) }
  var supportPhone by remember(uiState.settings) { mutableStateOf(uiState.settings.supportPhone) }
  var whatsappNumber by remember(uiState.settings) { mutableStateOf(uiState.settings.whatsappNumber) }
  var storeAddress by remember(uiState.settings) { mutableStateOf(uiState.settings.storeAddress) }
  var announcementText by remember(uiState.settings) { mutableStateOf(uiState.settings.announcementText) }
  var showFeatured by remember(uiState.settings) { mutableStateOf(uiState.settings.showFeaturedSection) }
  var showDeals by remember(uiState.settings) { mutableStateOf(uiState.settings.showDealsSection) }
  var showBestsellers by remember(uiState.settings) { mutableStateOf(uiState.settings.showBestsellersSection) }
  var showRecommended by remember(uiState.settings) { mutableStateOf(uiState.settings.showRecommendedSection) }
  var showProductColors by remember(uiState.settings) { mutableStateOf(uiState.settings.showProductColors) }
  var showProductSizes by remember(uiState.settings) { mutableStateOf(uiState.settings.showProductSizes) }

  var showHeroSection by remember(uiState.settings) { mutableStateOf(uiState.settings.showHeroSection) }
  var heroBadgeText by remember(uiState.settings) { mutableStateOf(uiState.settings.heroBadgeText) }
  var heroTitleText by remember(uiState.settings) { mutableStateOf(uiState.settings.heroTitleText) }
  var heroSubtitleText by remember(uiState.settings) { mutableStateOf(uiState.settings.heroSubtitleText) }
  var heroCtaText by remember(uiState.settings) { mutableStateOf(uiState.settings.heroCtaText) }

  var homeCategoryStyle by remember(uiState.settings) { mutableStateOf(uiState.settings.homeCategoryStyleEnum) }
  var categoriesTabStyle by remember(uiState.settings) { mutableStateOf(uiState.settings.categoriesTabStyleEnum) }
  var companyOfferBadgeStyle by remember(uiState.settings) { mutableStateOf(uiState.settings.companyOfferBadgeStyleEnum) }
  var numeralFormat by remember(uiState.settings) { mutableStateOf(uiState.settings.numeralFormatEnum) }
  var freeShippingEnabled by remember(uiState.settings) { mutableStateOf(uiState.settings.freeShippingEnabled) }
  var freeShippingThresholdText by remember(uiState.settings) {
    mutableStateOf(
      uiState.settings.freeShippingThreshold.let { value ->
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
      },
    )
  }
  var deliveryOtpEnabled by remember(uiState.settings) { mutableStateOf(uiState.settings.deliveryOtpEnabled) }
  var introVideoEnabled by remember(uiState.settings) { mutableStateOf(uiState.settings.introVideoEnabled) }
  var introMediaType by remember(uiState.settings) { mutableStateOf(uiState.settings.introMediaTypeEnum) }
  var introVideoUrl by remember(uiState.settings) { mutableStateOf(uiState.settings.introVideoUrl) }
  var introImageUrl by remember(uiState.settings) { mutableStateOf(uiState.settings.introImageUrl) }
  var introVideoDurationText by remember(uiState.settings) {
    mutableStateOf(
      uiState.settings.safeIntroVideoDurationSeconds.takeIf { it > 0 }?.toString().orEmpty(),
    )
  }
  var introVideoTitle by remember(uiState.settings) { mutableStateOf(uiState.settings.introVideoTitle) }
  var introVideoMuted by remember(uiState.settings) { mutableStateOf(uiState.settings.introVideoMuted) }
  var introReelTargetType by remember(uiState.settings) { mutableStateOf(uiState.settings.introReelTargetTypeEnum) }
  var introReelTargetId by remember(uiState.settings) { mutableStateOf(uiState.settings.introReelTargetId) }
  var introReelTargetLabel by remember(uiState.settings) { mutableStateOf(uiState.settings.introReelTargetLabel) }

  val initialTicker = uiState.settings.announcementTickerConfig
  var tickerEnabled by remember(uiState.settings) { mutableStateOf(initialTicker.enabled) }
  var tickerText by remember(uiState.settings) { mutableStateOf(initialTicker.text) }
  var tickerPlacement by remember(uiState.settings) { mutableStateOf(initialTicker.placement) }
  var tickerBackgroundColor by remember(uiState.settings) { mutableStateOf(initialTicker.safeBackgroundHex) }
  var tickerBorderColor by remember(uiState.settings) { mutableStateOf(initialTicker.safeBorderHex) }
  var tickerTextColor by remember(uiState.settings) { mutableStateOf(initialTicker.safeTextHex) }
  var tickerFontSizeSp by remember(uiState.settings) { mutableStateOf(initialTicker.safeFontSizeSp) }
  var tickerFontWeight by remember(uiState.settings) { mutableStateOf(initialTicker.fontWeight) }
  var tickerItalic by remember(uiState.settings) { mutableStateOf(initialTicker.isItalic) }
  var tickerInlineImages by remember(uiState.settings) { mutableStateOf(initialTicker.inlineImages) }

  var appIconTheme by remember(uiState.settings) { mutableStateOf(uiState.settings.appIconThemeEnum) }
  var productCardBorderStyle by remember(uiState.settings) { mutableStateOf(uiState.settings.productCardBorderStyleEnum) }
  var productCardBorderColor by remember(uiState.settings) { mutableStateOf(uiState.settings.productCardStyleConfig.safeCustomBorderHex) }
  var productCardShape by remember(uiState.settings) { mutableStateOf(uiState.settings.productCardShapeEnum) }
  var productDiscountBadgeStyle by remember(uiState.settings) { mutableStateOf(uiState.settings.productDiscountBadgeStyleEnum) }
  val settingsContext = LocalContext.current

  var aboutStoreDescription by remember(uiState.settings) { mutableStateOf(uiState.settings.aboutStoreDescription) }
  var retailFacebookUrl by remember(uiState.settings) { mutableStateOf(uiState.settings.retailFacebookUrl) }
  var wholesaleFacebookUrl by remember(uiState.settings) { mutableStateOf(uiState.settings.wholesaleFacebookUrl) }
  var retailPhones by remember(uiState.settings) { mutableStateOf(uiState.settings.retailPhones) }
  var wholesalePhones by remember(uiState.settings) { mutableStateOf(uiState.settings.wholesalePhones) }
  var socialPages by remember(uiState.settings) { mutableStateOf(uiState.settings.socialPages) }

  var newRetailPhoneLabel by remember { mutableStateOf("") }
  var newRetailPhoneNumber by remember { mutableStateOf("") }
  var newWholesalePhoneLabel by remember { mutableStateOf("") }
  var newWholesalePhoneNumber by remember { mutableStateOf("") }
  var newSocialTitle by remember { mutableStateOf("") }
  var newSocialUrl by remember { mutableStateOf("") }
  var newSocialChannel by remember { mutableStateOf(StoreContactChannel.RETAIL) }

  var privacyPolicyUrl by remember(uiState.settings) { mutableStateOf(uiState.settings.privacyPolicyUrl) }
  var returnPolicyUrl by remember(uiState.settings) { mutableStateOf(uiState.settings.returnPolicyUrl) }
  var accountDeletionUrl by remember(uiState.settings) { mutableStateOf(uiState.settings.accountDeletionUrl) }
  var privacyPolicyText by remember(uiState.settings) { mutableStateOf(uiState.settings.effectivePrivacyPolicyText) }
  var returnPolicyText by remember(uiState.settings) { mutableStateOf(uiState.settings.effectiveReturnPolicyText) }
  var termsOfServiceText by remember(uiState.settings) { mutableStateOf(uiState.settings.effectiveTermsOfServiceText) }

  var broadcastTitle by remember { mutableStateOf("") }
  var broadcastMessage by remember { mutableStateOf("") }
  var onlyWholesale by remember { mutableStateOf(false) }
  var broadcastType by remember { mutableStateOf(NotificationType.OFFER_PROMO) }

  fun buildCombinedSettings(): StoreGeneralSettings {
    val finalRetailPhones = retailPhones.toMutableList().apply {
      if (newRetailPhoneNumber.isNotBlank()) {
        add(StorePhoneEntry(label = newRetailPhoneLabel.trim(), phoneNumber = newRetailPhoneNumber.trim(), channel = StoreContactChannel.RETAIL.code))
      }
    }
    val finalWholesalePhones = wholesalePhones.toMutableList().apply {
      if (newWholesalePhoneNumber.isNotBlank()) {
        add(StorePhoneEntry(label = newWholesalePhoneLabel.trim(), phoneNumber = newWholesalePhoneNumber.trim(), channel = StoreContactChannel.WHOLESALE.code))
      }
    }
    val finalSocialPages = socialPages.toMutableList().apply {
      if (newSocialTitle.isNotBlank() && newSocialUrl.isNotBlank()) {
        add(StoreSocialLink(title = newSocialTitle.trim(), url = newSocialUrl.trim(), channel = newSocialChannel.code))
      }
    }
    return uiState.settings.copy(
      storeName = storeName.trim().ifBlank { "الإيمان للتجارة" },
      tagline = tagline.trim(),
      ownerEmail = ownerEmail.trim().lowercase().ifBlank { uiState.settings.ownerEmail },
      supportPhone = supportPhone.trim(),
      whatsappNumber = whatsappNumber.trim(),
      storeAddress = storeAddress.trim(),
      announcementText = announcementText.trim(),
      aboutStoreDescription = aboutStoreDescription.trim(),
      retailFacebookUrl = retailFacebookUrl.trim(),
      wholesaleFacebookUrl = wholesaleFacebookUrl.trim(),
      socialPages = finalSocialPages,
      retailPhones = finalRetailPhones,
      wholesalePhones = finalWholesalePhones,
      showFeaturedSection = showFeatured,
      showDealsSection = showDeals,
      showBestsellersSection = showBestsellers,
      showRecommendedSection = showRecommended,
      showProductColors = showProductColors,
      showProductSizes = showProductSizes,
      showHeroSection = showHeroSection,
      heroBadgeText = heroBadgeText,
      heroTitleText = heroTitleText,
      heroSubtitleText = heroSubtitleText,
      heroCtaText = heroCtaText,
      homeCategoryStyle = homeCategoryStyle.code,
      categoriesTabStyle = categoriesTabStyle.code,
      companyOfferBadgeStyle = companyOfferBadgeStyle.code,
      numeralFormat = numeralFormat.code,
      freeShippingEnabled = freeShippingEnabled,
      freeShippingThreshold = freeShippingThresholdText.toDoubleOrNull()
        ?.takeIf { it > 0.0 }
        ?: if (freeShippingEnabled) 0.0 else uiState.settings.freeShippingThreshold,
      deliveryOtpEnabled = deliveryOtpEnabled,
      introVideoEnabled = introVideoEnabled,
      introMediaType = introMediaType.code,
      introVideoUrl = introVideoUrl.trim(),
      introImageUrl = introImageUrl.trim(),
      introVideoDurationSeconds = (introVideoDurationText.toIntOrNull() ?: 0).coerceIn(0, 120),
      introVideoTitle = introVideoTitle.trim().take(100),
      introVideoMuted = introVideoMuted,
      introReelTargetType = introReelTargetType.code,
      introReelTargetId = if (introReelTargetType == IntroReelTargetType.PROMO_ONLY) "" else introReelTargetId.trim().take(120),
      introReelTargetLabel = if (introReelTargetType == IntroReelTargetType.PROMO_ONLY) "" else introReelTargetLabel.trim().take(120),
      tickerEnabled = tickerEnabled,
      tickerText = tickerText.trim().take(1000),
      tickerPlacement = tickerPlacement.code,
      tickerBackgroundColor = sanitizeCardHexColor(tickerBackgroundColor, "#181A20"),
      tickerBorderColor = sanitizeCardHexColor(tickerBorderColor, "#FFD000"),
      tickerTextColor = sanitizeCardHexColor(tickerTextColor, "#FFFFFF"),
      tickerFontSizeSp = tickerFontSizeSp.coerceIn(11, 24),
      tickerFontWeight = tickerFontWeight.code,
      tickerItalic = tickerItalic,
      tickerInlineImages = tickerInlineImages.filter { it.id.isNotBlank() && it.url.isNotBlank() }.take(10),
      appIconTheme = appIconTheme.code,
      productCardBorderStyle = productCardBorderStyle.code,
      productCardBorderColor = sanitizeCardHexColor(productCardBorderColor),
      productCardShape = productCardShape.code,
      productDiscountBadgeStyle = productDiscountBadgeStyle.code,
      privacyPolicyUrl = privacyPolicyUrl.trim(),
      returnPolicyUrl = returnPolicyUrl.trim(),
      accountDeletionUrl = accountDeletionUrl.trim(),
      privacyPolicyText = privacyPolicyText.trim().ifEmpty { StoreGeneralSettings.DEFAULT_PRIVACY_POLICY_TEXT },
      returnPolicyText = returnPolicyText.trim().ifEmpty { StoreGeneralSettings.DEFAULT_RETURN_POLICY_TEXT },
      termsOfServiceText = termsOfServiceText.trim().ifEmpty { StoreGeneralSettings.DEFAULT_TERMS_OF_SERVICE_TEXT },
    )
  }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
    val msg = uiState.feedbackMessage ?: uiState.errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      if (uiState.feedbackMessage?.contains("إرسال الإشعار") == true) {
        broadcastTitle = ""
        broadcastMessage = ""
      }
      onClearMessages()
    }
  }

  LaunchedEffect(cashTrackingUiState.feedbackMessage, cashTrackingUiState.errorMessage) {
    val msg = cashTrackingUiState.feedbackMessage ?: cashTrackingUiState.errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearCashMessages()
    }
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = if (effectiveTab == 1) "متابعة تحويلات الكاش" else "إعدادات المتجر والإشعارات",
        subtitle = if (effectiveTab == 1) {
          "أرصدة هواتف الإيمان كاش، التصفية بالتاريخ، السحب والتصدير"
        } else {
          "اضغط على أي قسم لفتحه وتعديل وحفظ إعداداته"
        },
        showBack = true,
        onBackClick = onBackClick,
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (canEditStoreSettings) {
        TabRow(
          selectedTabIndex = effectiveTab,
          containerColor = PureWhite,
          contentColor = CharcoalBlack,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[effectiveTab]),
              color = PrimaryYellow,
            )
          },
          modifier = Modifier.widthIn(max = 840.dp),
        ) {
          Tab(
            selected = effectiveTab == 0,
            onClick = { selectedTab = 0 },
            modifier = Modifier.testTag("settings_tab_general"),
            text = {
              Text(
                text = "إعدادات المتجر",
                fontWeight = if (effectiveTab == 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
          )
          Tab(
            selected = effectiveTab == 1,
            onClick = { selectedTab = 1 },
            modifier = Modifier.testTag("settings_tab_cash_tracking"),
            text = {
              Text(
                text = "متابعة تحويلات الكاش",
                fontWeight = if (effectiveTab == 1) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.TopCenter,
      ) {
        if (effectiveTab == 1) {
          CashTrackingSettingsTab(
            uiState = cashTrackingUiState,
            canManageCashAccounts = canManageCashAccounts,
            canSettleCashDevice = canSettleCashDevice,
            actorDisplayName = actorDisplayName,
            onSelectFilterMode = onSelectCashFilterMode,
            onSelectSingleDay = onSelectCashSingleDay,
            onSelectDateRange = onSelectCashDateRange,
            onSaveCashAccount = onSaveCashAccount,
            onToggleCashAccount = onToggleCashAccount,
            onDeleteCashAccount = onDeleteCashAccount,
            onSettleDevice = onSettleCashDevice,
            onExportExcelToUri = onExportCashExcelToUri,
            onShareExcel = onShareCashExcel,
          )
        } else if (uiState.isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CharcoalBlack)
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
          contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // معاينة أسعار المتجر (قطاعي / جملة) للإدارة من الإعدادات فقط
          if (canToggleWholesalePreview) {
            item {
              AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                  ) {
                    Text(
                      text = "معاينة أسعار المتجر (قطاعي / جملة)",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                    Text(
                      text = if (isWholesalePreviewActive) {
                        "وضع المعاينة الحالي: أسعار الجملة المعتمدة"
                      } else {
                        "وضع المعاينة الحالي: أسعار القطاعي للعملاء"
                      },
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                  }
                  Surface(
                    onClick = onToggleWholesalePreview,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isWholesalePreviewActive) PrimaryYellow else GeneralBackground,
                    border = BorderStroke(
                      1.dp,
                      if (isWholesalePreviewActive) CharcoalBlack else BorderNeutral,
                    ),
                    modifier = Modifier.testTag("settings_wholesale_preview_toggle"),
                  ) {
                    Text(
                      text = if (isWholesalePreviewActive) "معاينة: جملة" else "معاينة: قطاعي",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                  }
                }
              }
            }
          }

          // 1. البيانات العامة وبريد مالك المتجر
          item {
            SettingsAccordionCard(
              title = "بيانات المتجر العامة وبريد المالك",
              subtitle = "$storeName • ${ownerEmail.ifBlank { "لم يحدد بريد" }}",
              icon = Icons.Filled.Storefront,
              expanded = expandedSection == StoreSettingsSection.GENERAL_AND_OWNER,
              testTag = "accordion_general_settings",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.GENERAL_AND_OWNER) {
                  null
                } else {
                  StoreSettingsSection.GENERAL_AND_OWNER
                }
              },
            ) {
              OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("اسم المتجر المعروض للعملاء") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text("الشعار الوصفي (Tagline)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = PrimaryYellowSoft,
                border = BorderStroke(1.dp, PrimaryYellow),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(10.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                    Text(
                      text = "صلاحية مالك المتجر (Super Admin)",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                  }
                  OutlinedTextField(
                    value = ownerEmail,
                    onValueChange = { ownerEmail = it },
                    label = { Text("البريد الإلكتروني لمالك المتجر") },
                    placeholder = { Text("owner@gmail.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("store_owner_email_input"),
                    singleLine = true,
                  )
                  Text(
                    text = "أي مستخدم يسجل دخوله بهذا البريد سيحصل تلقائيًا على كامل صلاحيات مالك المتجر للتعديل والإضافة.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                }
              }

              OutlinedTextField(
                value = announcementText,
                onValueChange = { announcementText = it },
                label = { Text("شريط تنبيه أعلى الرئيسية (اتركه فارغًا للإخفاء)") },
                leadingIcon = { Icon(Icons.Filled.Campaign, contentDescription = null, tint = CharcoalBlack) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
              )

              OutlinedTextField(
                value = supportPhone,
                onValueChange = { supportPhone = it },
                label = { Text("رقم هاتف خدمة العملاء الأساسي") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              OutlinedTextField(
                value = whatsappNumber,
                onValueChange = { whatsappNumber = it },
                label = { Text("رقم واتساب للتواصل") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              OutlinedTextField(
                value = storeAddress,
                onValueChange = { storeAddress = it },
                label = { Text("عنوان المقر الرئيسي") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
              )

              HorizontalDivider(color = BorderNeutral)

              Text(
                text = "نظام الأرقام المعروض للأسعار والكميات",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumeralFormat.entries.forEach { option ->
                  StyleOptionChoiceCard(
                    title = option.arabicLabel,
                    subtitle = option.exampleText,
                    selected = numeralFormat == option,
                    onClick = { numeralFormat = option },
                  )
                }
              }

              HorizontalDivider(color = BorderNeutral)

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = GeneralBackground,
                border = BorderStroke(1.dp, BorderNeutral),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                    Text(
                      text = "ميزة الشحن المجاني التلقائي",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                  }
                  SettingsToggleRow(
                    label = "تفعيل الشحن المجاني عند تجاوز قيمة معينة",
                    checked = freeShippingEnabled,
                    onCheckedChange = { freeShippingEnabled = it },
                  )
                  AnimatedVisibility(
                    visible = freeShippingEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                  ) {
                    OutlinedTextField(
                      value = freeShippingThresholdText,
                      onValueChange = { raw ->
                        freeShippingThresholdText = raw.filter { ch -> ch.isDigit() || ch == '.' }
                      },
                      label = { Text("الحد الأدنى لقيمة الطلب للشحن المجاني (ج.م)") },
                      placeholder = { Text("500") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_free_shipping_threshold_input"),
                    )
                  }
                  Text(
                    text = if (freeShippingEnabled) {
                      "سيُحتسب التوصيل مجانًا تلقائيًا لأي طلب توصيل يبلغ إجماليه (بعد الخصم) قيمة الحد الأدنى المحددة أو أكثر."
                    } else {
                      "الميزة مغلقة حاليًا، وتُطبّق رسوم التوصيل المعتادة لكل منطقة."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = GeneralBackground,
                border = BorderStroke(1.dp, BorderNeutral),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                    Text(
                      text = "كود تأكيد الاستلام مع المندوب (٤ أرقام)",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = CharcoalBlack,
                    )
                  }
                  SettingsToggleRow(
                    label = "تفعيل كود تأكيد الاستلام عند توصيل الطلبات",
                    checked = deliveryOtpEnabled,
                    onCheckedChange = { deliveryOtpEnabled = it },
                    switchTestTag = "store_delivery_otp_toggle",
                  )
                  Text(
                    text = if (deliveryOtpEnabled) {
                      "سيتم إنشاء كود تحقق من ٤ أرقام لطلبات التوصيل الجديدة، ويُطلب من المندوب إدخاله قبل تأكيد التسليم."
                    } else {
                      "الميزة مغلقة حاليًا؛ الطلبات الجديدة لن تطلب كود تأكيد عند التسليم مع المندوب."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                }
              }

              SectionSaveButton(
                text = "حفظ البيانات العامة وبريد المالك",
                isSubmitting = uiState.isSubmitting,
                testTag = "admin_save_store_settings_button",
                onClick = {
                  ArabicFormatter.setNumeralFormat(numeralFormat)
                  onSaveSettings(buildCombinedSettings())
                },
              )
            }
          }

          // ١.٢٥ أيقونة التطبيق على شاشة الموبايل (المواسم والعروض)
          item {
            SettingsAccordionCard(
              title = "أيقونة التطبيق على الموبايل (المواسم والعروض)",
              subtitle = "الحالية: ${appIconTheme.arabicLabel} — تتغير تلقائيًا على هواتف الأندرويد",
              icon = Icons.Filled.AppShortcut,
              expanded = expandedSection == StoreSettingsSection.APP_ICON_THEME,
              testTag = "accordion_app_icon_theme",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.APP_ICON_THEME) {
                  null
                } else {
                  StoreSettingsSection.APP_ICON_THEME
                }
              },
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = PrimaryYellowSoft,
                border = BorderStroke(1.dp, PrimaryYellow),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "اختر شكل أيقونة تطبيق «الإيمان للتجارة» (الأساسية، عروض، رمضان، أو العيد). عند الحفظ تتحدث الأيقونة تلقائيًا على هواتف العملاء (قد يستغرق الأمر بضع ثوانٍ حتى يحدّث نظام الهاتف شكل الأيقونة على الشاشة الرئيسية).",
                  style = MaterialTheme.typography.bodySmall,
                  color = CharcoalBlack,
                  modifier = Modifier.padding(10.dp),
                )
              }

              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppIconTheme.entries.chunked(2).forEach { pair ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    pair.forEach { option ->
                      val selected = appIconTheme == option
                      Surface(
                        onClick = { appIconTheme = option },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) PrimaryYellowSoft else PureWhite,
                        border = BorderStroke(
                          width = if (selected) 2.dp else 1.dp,
                          color = if (selected) CharcoalBlack else BorderNeutral,
                        ),
                        modifier = Modifier
                          .weight(1f)
                          .testTag("app_icon_option_${option.code}"),
                      ) {
                        Column(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          Box(contentAlignment = Alignment.TopEnd) {
                            Image(
                              painter = painterResource(id = AppIconManager.previewDrawableFor(option)),
                              contentDescription = option.arabicLabel,
                              contentScale = ContentScale.Crop,
                              modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(
                                  width = if (selected) 2.dp else 1.dp,
                                  color = if (selected) CharcoalBlack else BorderNeutral,
                                  shape = RoundedCornerShape(18.dp),
                                ),
                            )
                            if (selected) {
                              Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = CharcoalBlack,
                                modifier = Modifier.padding(4.dp),
                              ) {
                                Icon(
                                  imageVector = Icons.Filled.CheckCircle,
                                  contentDescription = "مختارة",
                                  tint = PrimaryYellow,
                                  modifier = Modifier.size(18.dp),
                                )
                              }
                            }
                          }
                          Text(
                            text = option.arabicLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = CharcoalBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                          Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                      }
                    }
                  }
                }
              }

              SectionSaveButton(
                text = "حفظ وتطبيق أيقونة التطبيق المختارة",
                isSubmitting = uiState.isSubmitting,
                testTag = "admin_save_app_icon_theme_button",
                onClick = {
                  AppIconManager.applyIconTheme(settingsContext, appIconTheme)
                  onSaveSettings(buildCombinedSettings())
                },
              )
            }
          }

          // ١.٥ مقطع الريلز الترحيبي بعد شاشة البداية (فيديو قصير أو صورة متحركة)
          item {
            val activeMediaUrl = if (introMediaType == IntroMediaType.ANIMATED_IMAGE) {
              introImageUrl.trim()
            } else {
              introVideoUrl.trim()
            }
            val introStatus = when {
              introVideoEnabled && activeMediaUrl.length >= 5 -> {
                val dur = introVideoDurationText.toIntOrNull() ?: 0
                val typeLabel = if (introMediaType == IntroMediaType.ANIMATED_IMAGE) "صورة متحركة" else "فيديو ريلز"
                val durText = when {
                  dur > 0 -> "المدة: $dur ثانية"
                  introMediaType == IntroMediaType.ANIMATED_IMAGE -> "٥ ثوانٍ افتراضيًا"
                  else -> "مدة تلقائية"
                }
                val actionText = if (introReelTargetType == IntroReelTargetType.PROMO_ONLY || introReelTargetId.isBlank()) {
                  "دعائي فقط"
                } else {
                  val targetName = introReelTargetLabel.trim().ifEmpty { introReelTargetType.arabicLabel }
                  "تحويل: $targetName"
                }
                "مفعّل ($typeLabel) • $durText • $actionText"
              }
              activeMediaUrl.length >= 5 -> "مرفوع (متوقف مؤقتًا)"
              else -> "غير مفعّل — أضف فيديو ريلز قصير أو صورة متحركة (GIF / WebP) بعد شاشة البداية"
            }
            SettingsAccordionCard(
              title = "مقطع الريلز الترحيبي بعد شاشة البداية (أندرويد فقط)",
              subtitle = introStatus,
              icon = if (introMediaType == IntroMediaType.ANIMATED_IMAGE) Icons.Filled.Animation else Icons.Filled.Videocam,
              expanded = expandedSection == StoreSettingsSection.INTRO_VIDEO,
              testTag = "accordion_intro_video",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.INTRO_VIDEO) {
                  null
                } else {
                  StoreSettingsSection.INTRO_VIDEO
                }
              },
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = PrimaryYellowSoft,
                border = BorderStroke(1.dp, PrimaryYellow),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "اختر بين رفع فيديو ريلز قصير (يعمل بالصوت افتراضيًا) أو صورة متحركة (GIF / WebP) لتظهر بملء الشاشة بعد شاشة البداية في تطبيق الأندرويد فقط، مع زر «تخطى» في الأعلى وانتقال تلقائي للمتجر فور انتهاء المدة.",
                  style = MaterialTheme.typography.bodySmall,
                  color = CharcoalBlack,
                  modifier = Modifier.padding(10.dp),
                )
              }

              SettingsToggleRow(
                label = "تفعيل العرض الترحيبي بعد شاشة البداية",
                checked = introVideoEnabled,
                onCheckedChange = { introVideoEnabled = it },
                switchTestTag = "store_intro_video_toggle",
              )

              Text(
                text = "نوع المحتوى الترحيبي",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = introMediaType == IntroMediaType.VIDEO,
                  onClick = { introMediaType = IntroMediaType.VIDEO },
                  label = { Text("فيديو ريلز قصير", fontWeight = FontWeight.Bold) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Filled.Videocam,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CharcoalBlack,
                    selectedLabelColor = PrimaryYellow,
                    selectedLeadingIconColor = PrimaryYellow,
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("intro_media_type_video"),
                )
                FilterChip(
                  selected = introMediaType == IntroMediaType.ANIMATED_IMAGE,
                  onClick = { introMediaType = IntroMediaType.ANIMATED_IMAGE },
                  label = { Text("صورة متحركة", fontWeight = FontWeight.Bold) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Filled.Animation,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CharcoalBlack,
                    selectedLabelColor = PrimaryYellow,
                    selectedLeadingIconColor = PrimaryYellow,
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("intro_media_type_animated"),
                )
              }

              if (introMediaType == IntroMediaType.VIDEO) {
                VideoPickerUploadField(
                  videoUrl = introVideoUrl,
                  detectedDurationSeconds = introVideoDurationText.toIntOrNull() ?: 0,
                  buttonLabel = "رفع فيديو ريلز ترحيبي من المعرض",
                  urlFieldLabel = "رابط الفيديو الترحيبي (MP4)",
                  folder = CloudinaryFolder.VIDEOS,
                  testTagPrefix = "store_intro_video",
                  onVideoChanged = { nextUrl, detectedDur, _ ->
                    introVideoUrl = nextUrl
                    if (detectedDur != null && detectedDur > 0) {
                      introVideoDurationText = detectedDur.coerceIn(1, 120).toString()
                    }
                  },
                  onClearVideo = {
                    introVideoUrl = ""
                    introVideoDurationText = ""
                    introVideoEnabled = false
                  },
                )

                Text(
                  text = "صوت الفيديو الترحيبي",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = CharcoalBlack,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  FilterChip(
                    selected = !introVideoMuted,
                    onClick = { introVideoMuted = false },
                    label = { Text("بصوت", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = CharcoalBlack,
                      selectedLabelColor = PrimaryYellow,
                      selectedLeadingIconColor = PrimaryYellow,
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("intro_video_sound_on"),
                  )
                  FilterChip(
                    selected = introVideoMuted,
                    onClick = { introVideoMuted = true },
                    label = { Text("كاتم (بدون صوت)", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                      Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = CharcoalBlack,
                      selectedLabelColor = PrimaryYellow,
                      selectedLeadingIconColor = PrimaryYellow,
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("intro_video_sound_muted"),
                  )
                }
              } else {
                AnimatedImagePickerUploadField(
                  imageUrl = introImageUrl,
                  isSlimPreview = false,
                  folder = CloudinaryFolder.BANNERS,
                  testTagPrefix = "store_intro_animated",
                  onImageUrlChanged = { nextUrl ->
                    introImageUrl = nextUrl
                  },
                  onClearImage = {
                    introImageUrl = ""
                    introVideoEnabled = false
                  },
                )
              }

              OutlinedTextField(
                value = introVideoDurationText,
                onValueChange = { introVideoDurationText = it.filter { ch -> ch.isDigit() }.take(3) },
                label = {
                  Text(
                    if (introMediaType == IntroMediaType.ANIMATED_IMAGE) {
                      "مدة عرض الصورة المتحركة بالثواني (الافتراضي ٥ ثوانٍ)"
                    } else {
                      "مدة الفيديو بالثواني (تُكتشف تلقائيًا عند الرفع والتشغيل)"
                    },
                  )
                },
                placeholder = {
                  Text(if (introMediaType == IntroMediaType.ANIMATED_IMAGE) "5" else "تلقائي")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              HorizontalDivider(color = BorderNeutral.copy(alpha = 0.7f))

              IntroReelTargetSelector(
                targetType = introReelTargetType,
                targetId = introReelTargetId,
                targetLabel = introReelTargetLabel,
                categories = reelCategories,
                products = reelProducts,
                companies = uiState.companies,
                brandShowcases = reelBrandShowcases,
                onTargetTypeChange = { nextType ->
                  if (introReelTargetType != nextType) {
                    introReelTargetType = nextType
                    introReelTargetId = ""
                    introReelTargetLabel = ""
                  }
                },
                onSelectTarget = { selectedId, selectedLabel ->
                  introReelTargetId = selectedId
                  introReelTargetLabel = selectedLabel
                },
              )

              SectionSaveButton(
                text = "حفظ إعدادات العرض الترحيبي",
                isSubmitting = uiState.isSubmitting,
                testTag = "admin_save_intro_video_button",
                onClick = { onSaveSettings(buildCombinedSettings()) },
              )
            }
          }

          // ١.٦ الشريط الإعلاني المتحرك (من اليسار إلى اليمين مع رموز وصور مصغرة وتخصيص كامل)
          item {
            val tickerSubtitle = when {
              tickerEnabled && (tickerText.isNotBlank() || tickerInlineImages.isNotEmpty()) ->
                "مفعّل • ${tickerPlacement.arabicLabel}"
              tickerText.isNotBlank() -> "محفوظ (متوقف مؤقتًا)"
              else -> "أضف شريط إعلاني متحرك من اليسار لليمين مع رموز وصور مصغرة وتخصيص كامل للألوان والخط"
            }
            SettingsAccordionCard(
              title = "الشريط الإعلاني المتحرك",
              subtitle = tickerSubtitle,
              icon = Icons.Filled.Campaign,
              expanded = expandedSection == StoreSettingsSection.ANNOUNCEMENT_TICKER,
              testTag = "accordion_announcement_ticker",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.ANNOUNCEMENT_TICKER) {
                  null
                } else {
                  StoreSettingsSection.ANNOUNCEMENT_TICKER
                }
              },
            ) {
              AnnouncementTickerAdminSection(
                enabled = tickerEnabled,
                text = tickerText,
                placement = tickerPlacement,
                backgroundColorHex = tickerBackgroundColor,
                borderColorHex = tickerBorderColor,
                textColorHex = tickerTextColor,
                fontSizeSp = tickerFontSizeSp,
                fontWeight = tickerFontWeight,
                isItalic = tickerItalic,
                inlineImages = tickerInlineImages,
                isSubmitting = uiState.isSubmitting,
                onEnabledChange = { tickerEnabled = it },
                onTextChange = { tickerText = it },
                onPlacementChange = { tickerPlacement = it },
                onBackgroundColorChange = { tickerBackgroundColor = it },
                onBorderColorChange = { tickerBorderColor = it },
                onTextColorChange = { tickerTextColor = it },
                onFontSizeChange = { tickerFontSizeSp = it },
                onFontWeightChange = { tickerFontWeight = it },
                onItalicChange = { tickerItalic = it },
                onInlineImagesChange = { tickerInlineImages = it },
                onSaveClick = { onSaveSettings(buildCombinedSettings()) },
              )
            }
          }

          // ٢. إضافة وإدارة الشركات والماركات
          item {
            SettingsAccordionCard(
              title = "إضافة شركات للمتجر",
              subtitle = "${ArabicFormatter.formatNumber(uiState.companies.size)} شركة مسجلة لربط المنتجات والعروض تلقائيًا",
              icon = Icons.Filled.Business,
              expanded = expandedSection == StoreSettingsSection.COMPANIES,
              testTag = "accordion_store_companies",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.COMPANIES) {
                  null
                } else {
                  StoreSettingsSection.COMPANIES
                }
              },
            ) {
              Text(
                text = "أنشئ شركات المتجر مع اسمها وشعارها هنا، لتظهر داخل نموذج إضافة المنتج وعند إنشاء قسم منتجات شركة.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )

              Button(
                onClick = {
                  editingCompany = null
                  showCompanyDialog = true
                },
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                  containerColor = PrimaryYellow,
                  contentColor = CharcoalBlack,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(46.dp)
                  .testTag("admin_add_company_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة شركة جديدة", fontWeight = FontWeight.Bold)
              }

              if (uiState.companies.isEmpty()) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = GeneralBackground,
                  border = BorderStroke(1.dp, BorderNeutral),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = "لا توجد شركات مضافة بعد. اضغط «إضافة شركة جديدة» لإضافة أول شركة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(12.dp),
                  )
                }
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  uiState.companies.forEach { company ->
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = GeneralBackground,
                      border = BorderStroke(1.dp, BorderNeutral),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        Box(
                          modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PureWhite)
                            .border(1.dp, BorderNeutral, RoundedCornerShape(12.dp)),
                          contentAlignment = Alignment.Center,
                        ) {
                          if (!company.imageUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                              model = company.imageUrl,
                              contentDescription = company.name,
                              contentScale = ContentScale.Fit,
                              modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            )
                          } else {
                            Text(
                              text = company.name.take(1).uppercase(),
                              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                              color = CharcoalBlack,
                            )
                          }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(
                            text = company.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CharcoalBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                          ) {
                            Text(
                              text = "ترتيب: ${ArabicFormatter.formatNumber(company.displayOrder)}",
                              style = MaterialTheme.typography.labelSmall,
                              color = TextSecondary,
                            )
                            if (company.isActive) SuccessBadge("مفعّلة") else DiscountBadge("معطلة")
                          }
                        }
                        IconButton(
                          onClick = {
                            editingCompany = company
                            showCompanyDialog = true
                          },
                          modifier = Modifier.size(36.dp),
                        ) {
                          Icon(Icons.Filled.Edit, contentDescription = "تعديل الشركة", tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                          onClick = { deletingCompany = company },
                          modifier = Modifier.size(36.dp),
                        ) {
                          Icon(Icons.Filled.Delete, contentDescription = "حذف الشركة", tint = DiscountRed, modifier = Modifier.size(18.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // 3. إعدادات التواصل بالشركة وصفحة "عن المتجر"
          item {
            SettingsAccordionCard(
              title = "التواصل بالشركة وصفحة عن المتجر",
              subtitle = "أرقام القطاعي والجملة وروابط فيسبوك والصفحات (${retailPhones.size + wholesalePhones.size} هاتف • ${socialPages.size} رابط)",
              icon = Icons.Filled.ContactPhone,
              expanded = expandedSection == StoreSettingsSection.CONTACT_AND_ABOUT,
              testTag = "accordion_contact_settings",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.CONTACT_AND_ABOUT) {
                  null
                } else {
                  StoreSettingsSection.CONTACT_AND_ABOUT
                }
              },
            ) {
              OutlinedTextField(
                value = aboutStoreDescription,
                onValueChange = { aboutStoreDescription = it },
                label = { Text("نبذة عن المتجر والشركة (تظهر في صفحة عن المتجر)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
              )

              HorizontalDivider(color = BorderNeutral)

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(Icons.Filled.Public, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                Text(
                  text = "روابط صفحات فيسبوك الرسمية",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = CharcoalBlack,
                )
              }

              OutlinedTextField(
                value = retailFacebookUrl,
                onValueChange = { retailFacebookUrl = it },
                label = { Text("رابط صفحة فيسبوك للقطاعي") },
                placeholder = { Text("https://facebook.com/...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              OutlinedTextField(
                value = wholesaleFacebookUrl,
                onValueChange = { wholesaleFacebookUrl = it },
                label = { Text("رابط صفحة فيسبوك للجملة") },
                placeholder = { Text("https://facebook.com/...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              HorizontalDivider(color = BorderNeutral)

              PhoneListEditorSection(
                title = "أرقام هواتف القطاعي",
                badgeText = "قطاعي",
                phones = retailPhones,
                pendingLabel = newRetailPhoneLabel,
                pendingNumber = newRetailPhoneNumber,
                labelPlaceholder = "وصف الرقم (اختياري مثل: مبيعات القطاعي)",
                addButtonLabel = "إضافة رقم قطاعي",
                addButtonTestTag = "add_retail_phone_button",
                onPendingLabelChange = { newRetailPhoneLabel = it },
                onPendingNumberChange = { newRetailPhoneNumber = it },
                onAddPhone = {
                  if (newRetailPhoneNumber.isNotBlank()) {
                    retailPhones = retailPhones + StorePhoneEntry(
                      label = newRetailPhoneLabel.trim(),
                      phoneNumber = newRetailPhoneNumber.trim(),
                      channel = StoreContactChannel.RETAIL.code,
                    )
                    newRetailPhoneLabel = ""
                    newRetailPhoneNumber = ""
                  }
                },
                onRemovePhone = { idx ->
                  retailPhones = retailPhones.filterIndexed { i, _ -> i != idx }
                },
              )

              HorizontalDivider(color = BorderNeutral)

              PhoneListEditorSection(
                title = "أرقام هواتف الجملة",
                badgeText = "جملة",
                phones = wholesalePhones,
                pendingLabel = newWholesalePhoneLabel,
                pendingNumber = newWholesalePhoneNumber,
                labelPlaceholder = "وصف الرقم (اختياري مثل: مبيعات الجملة)",
                addButtonLabel = "إضافة رقم جملة",
                addButtonTestTag = "add_wholesale_phone_button",
                onPendingLabelChange = { newWholesalePhoneLabel = it },
                onPendingNumberChange = { newWholesalePhoneNumber = it },
                onAddPhone = {
                  if (newWholesalePhoneNumber.isNotBlank()) {
                    wholesalePhones = wholesalePhones + StorePhoneEntry(
                      label = newWholesalePhoneLabel.trim(),
                      phoneNumber = newWholesalePhoneNumber.trim(),
                      channel = StoreContactChannel.WHOLESALE.code,
                    )
                    newWholesalePhoneLabel = ""
                    newWholesalePhoneNumber = ""
                  }
                },
                onRemovePhone = { idx ->
                  wholesalePhones = wholesalePhones.filterIndexed { i, _ -> i != idx }
                },
              )

              HorizontalDivider(color = BorderNeutral)

              Text(
                text = "صفحات وروابط تواصل إضافية للشركة",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )

              socialPages.forEachIndexed { idx, link ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = GeneralBackground,
                  border = BorderStroke(1.dp, BorderNeutral),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Text(
                          text = link.title,
                          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                          color = CharcoalBlack,
                        )
                        BrandPillBadge(
                          text = link.channelEnum.arabicLabel,
                          containerColor = PrimaryYellowSoft,
                          contentColor = CharcoalBlack,
                        )
                      }
                      Text(
                        text = link.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                      )
                    }
                    IconButton(
                      onClick = { socialPages = socialPages.filterIndexed { i, _ -> i != idx } },
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "حذف الصفحة",
                        tint = CharcoalBlack,
                      )
                    }
                  }
                }
              }

              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreContactChannel.entries.forEach { channel ->
                  FilterChip(
                    selected = newSocialChannel == channel,
                    onClick = { newSocialChannel = channel },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = PrimaryYellow,
                      selectedLabelColor = CharcoalBlack,
                    ),
                    label = { Text(channel.arabicLabel) },
                  )
                }
              }

              OutlinedTextField(
                value = newSocialTitle,
                onValueChange = { newSocialTitle = it },
                label = { Text("اسم المنصة أو الصفحة (مثل: إنستغرام، تيك توك، موقع الشركة)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              OutlinedTextField(
                value = newSocialUrl,
                onValueChange = { newSocialUrl = it },
                label = { Text("رابط الصفحة أو القناة") },
                placeholder = { Text("https://...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
              )

              OutlinedButton(
                onClick = {
                  if (newSocialTitle.isNotBlank() && newSocialUrl.isNotBlank()) {
                    socialPages = socialPages + StoreSocialLink(
                      title = newSocialTitle.trim(),
                      url = newSocialUrl.trim(),
                      channel = newSocialChannel.code,
                    )
                    newSocialTitle = ""
                    newSocialUrl = ""
                  }
                },
                enabled = newSocialTitle.isNotBlank() && newSocialUrl.isNotBlank(),
                border = BorderStroke(1.dp, CharcoalBlack),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .testTag("add_social_page_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة صفحة التواصل", color = CharcoalBlack, fontWeight = FontWeight.Bold)
              }

              SectionSaveButton(
                text = "حفظ إعدادات التواصل وعن المتجر",
                isSubmitting = uiState.isSubmitting,
                testTag = "admin_save_contact_settings_button",
                onClick = { onSaveSettings(buildCombinedSettings()) },
              )
            }
          }

          // 3. بطاقة الترحيب وأقسام الصفحة الرئيسية
          item {
            SettingsAccordionCard(
              title = "البطاقة الترحيبية وأقسام الرئيسية",
              subtitle = if (showHeroSection) "البطاقة الترحيبية مفعّلة" else "البطاقة الترحيبية مخفية",
              icon = Icons.Filled.ViewCarousel,
              expanded = expandedSection == StoreSettingsSection.HOME_HERO_AND_SECTIONS,
              testTag = "accordion_hero_sections",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.HOME_HERO_AND_SECTIONS) {
                  null
                } else {
                  StoreSettingsSection.HOME_HERO_AND_SECTIONS
                }
              },
            ) {
              Text(
                text = "البطاقة الترحيبية الرئيسية (عروض الترحيب وزر التسوق)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )

              SettingsToggleRow(
                label = "إظهار بطاقة العروض والترحيب في الصفحة الرئيسية",
                checked = showHeroSection,
                onCheckedChange = { showHeroSection = it },
              )

              androidx.compose.animation.AnimatedVisibility(visible = showHeroSection) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  OutlinedTextField(
                    value = heroBadgeText,
                    onValueChange = { heroBadgeText = it },
                    label = { Text("الشارة العلوية") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                  )
                  OutlinedTextField(
                    value = heroTitleText,
                    onValueChange = { heroTitleText = it },
                    label = { Text("العنوان الرئيسي للبطاقة") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                  )
                  OutlinedTextField(
                    value = heroSubtitleText,
                    onValueChange = { heroSubtitleText = it },
                    label = { Text("النص الوصفي للبطاقة") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                  )
                  OutlinedTextField(
                    value = heroCtaText,
                    onValueChange = { heroCtaText = it },
                    label = { Text("نص زر التسوق (مثال: تسوق الآن)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                  )
                }
              }

              HorizontalDivider(color = BorderNeutral)

              Text(
                text = "إظهار أو إخفاء أقسام الصفحة الرئيسية",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )

              SettingsToggleRow("عرض قسم المنتجات المميزة", showFeatured) { showFeatured = it }
              SettingsToggleRow("عرض قسم العروض والخصومات", showDeals) { showDeals = it }
              SettingsToggleRow("عرض قسم الأكثر مبيعًا", showBestsellers) { showBestsellers = it }
              SettingsToggleRow("عرض قسم منتجات مقترحة لك", showRecommended) { showRecommended = it }

              HorizontalDivider(color = BorderNeutral)

              Text(
                text = "إظهار الألوان والمقاسات للعملاء",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )

              SettingsToggleRow("إظهار الألوان المتاحة في بطاقات وتفاصيل المنتجات", showProductColors) { showProductColors = it }
              SettingsToggleRow("إظهار المقاسات والأحجام في بطاقات وتفاصيل المنتجات", showProductSizes) { showProductSizes = it }

              SectionSaveButton(
                text = "حفظ نصوص وأقسام الرئيسية",
                isSubmitting = uiState.isSubmitting,
                testTag = "admin_save_hero_sections_button",
                onClick = { onSaveSettings(buildCombinedSettings()) },
              )
            }
          }

          // 3b. تصميم وتخصيص كروت المنتجات (إطار، شكل، وشارة الخصم)
          item {
            SettingsAccordionCard(
              title = "تصميم كروت المنتجات وشارة الخصم",
              subtitle = "${productCardBorderStyle.arabicLabel} • ${productCardShape.arabicLabel} • ${productDiscountBadgeStyle.arabicLabel}",
              icon = Icons.Filled.AutoAwesome,
              expanded = expandedSection == StoreSettingsSection.PRODUCT_CARD_STYLE,
              testTag = "accordion_product_card_style",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.PRODUCT_CARD_STYLE) {
                  null
                } else {
                  StoreSettingsSection.PRODUCT_CARD_STYLE
                }
              },
            ) {
              ProductCardStyleSettingsContent(
                borderStyle = productCardBorderStyle,
                customBorderHex = productCardBorderColor,
                cardShape = productCardShape,
                discountBadgeStyle = productDiscountBadgeStyle,
                isSubmitting = uiState.isSubmitting,
                onBorderStyleChange = { productCardBorderStyle = it },
                onCustomBorderHexChange = { productCardBorderColor = it },
                onCardShapeChange = { productCardShape = it },
                onDiscountBadgeStyleChange = { productDiscountBadgeStyle = it },
                onSaveClick = { onSaveSettings(buildCombinedSettings()) },
              )
            }
          }

          // 4. أشكال عرض الفئات وشكل شارة «عرض خاص» للشركات
          item {
            SettingsAccordionCard(
              title = "أشكال عرض الفئات وعروض الشركات",
              subtitle = "الفئات: ${homeCategoryStyle.arabicLabel} • عرض خاص: ${companyOfferBadgeStyle.arabicLabel}",
              icon = Icons.Filled.Palette,
              expanded = expandedSection == StoreSettingsSection.CATEGORY_STYLES,
              testTag = "accordion_category_styles",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.CATEGORY_STYLES) {
                  null
                } else {
                  StoreSettingsSection.CATEGORY_STYLES
                }
              },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                Text(
                  text = "تصميم شارة «عرض خاص» لمنتجات الشركات",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = CharcoalBlack,
                )
              }

              Text(
                text = "اختر التصميم المفضل لشارة «عرض خاص» التي تظهر على عروض الشركات ويمكنك تغييره في أي وقت:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompanyOfferBadgeStyle.entries.forEach { option ->
                  val isSelected = companyOfferBadgeStyle == option
                  Surface(
                    onClick = { companyOfferBadgeStyle = option },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) PrimaryYellowSoft else PureWhite,
                    border = BorderStroke(
                      width = if (isSelected) 1.6.dp else 1.dp,
                      color = if (isSelected) CharcoalBlack else BorderNeutral,
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("offer_badge_style_option_${option.code}"),
                  ) {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(
                            text = option.arabicLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CharcoalBlack,
                          )
                          Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                          )
                        }
                        if (isSelected) {
                          SuccessBadge(text = "مفعل")
                        }
                      }
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(10.dp))
                          .background(GeneralBackground)
                          .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                      ) {
                        CompanySpecialOfferBadge(
                          discountPercent = 20,
                          style = option,
                        )
                      }
                    }
                  }
                }
              }

              HorizontalDivider(color = BorderNeutral)

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                Text(
                  text = "شكل عرض الفئات في الصفحة الرئيسية",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = CharcoalBlack,
                )
              }

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeCategoryStyle.entries.forEach { option ->
                  StyleOptionChoiceCard(
                    title = option.arabicLabel,
                    subtitle = option.subtitle,
                    selected = homeCategoryStyle == option,
                    onClick = { homeCategoryStyle = option },
                  )
                }
              }

              HorizontalDivider(color = BorderNeutral)

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Filled.Category, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                Text(
                  text = "شكل عرض الفئات داخل تبويب الفئات",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = CharcoalBlack,
                )
              }

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoriesTabStyle.entries.forEach { option ->
                  StyleOptionChoiceCard(
                    title = option.arabicLabel,
                    subtitle = option.subtitle,
                    selected = categoriesTabStyle == option,
                    onClick = { categoriesTabStyle = option },
                  )
                }
              }

              SectionSaveButton(
                text = "حفظ أشكال العرض وعروض الشركات",
                isSubmitting = uiState.isSubmitting,
                testTag = "admin_save_category_styles_button",
                onClick = { onSaveSettings(buildCombinedSettings()) },
              )
            }
          }

          // 4b. السياسات ومتطلبات النشر على Google Play
          item {
            SettingsAccordionCard(
              title = "السياسات ومتطلبات Google Play",
              subtitle = "سياسة الخصوصية، الاسترجاع، الشروط، وروابط الويب",
              icon = Icons.Filled.PrivacyTip,
              expanded = expandedSection == StoreSettingsSection.POLICIES_AND_COMPLIANCE,
              testTag = "accordion_policies_compliance",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.POLICIES_AND_COMPLIANCE) {
                  null
                } else {
                  StoreSettingsSection.POLICIES_AND_COMPLIANCE
                }
              },
            ) {
              Surface(
                color = PrimaryYellowSoft,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "تظهر هذه النصوص للعملاء داخل شاشة «السياسات والخصوصية». عند نشر التطبيق على Google Play يمكنك إضافة روابط صفحات الويب (HTTPS) هنا ليتمكن المستخدم من فتحها أيضًا.",
                  style = MaterialTheme.typography.bodySmall,
                  color = CharcoalBlack,
                  modifier = Modifier.padding(10.dp),
                )
              }

              OutlinedTextField(
                value = privacyPolicyUrl,
                onValueChange = { privacyPolicyUrl = it },
                label = { Text("رابط سياسة الخصوصية على الويب (اختياري - HTTPS)") },
                placeholder = { Text("https://yourdomain.com/privacy-policy") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_privacy_url_input"),
                singleLine = true,
              )

              OutlinedTextField(
                value = accountDeletionUrl,
                onValueChange = { accountDeletionUrl = it },
                label = { Text("رابط طلب حذف الحساب على الويب (اختياري - HTTPS)") },
                placeholder = { Text("https://yourdomain.com/delete-account") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_deletion_url_input"),
                singleLine = true,
              )

              OutlinedTextField(
                value = returnPolicyUrl,
                onValueChange = { returnPolicyUrl = it },
                label = { Text("رابط سياسة الاسترجاع على الويب (اختياري)") },
                placeholder = { Text("https://yourdomain.com/return-policy") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_return_url_input"),
                singleLine = true,
              )

              HorizontalDivider(color = BorderNeutral)

              OutlinedTextField(
                value = privacyPolicyText,
                onValueChange = { privacyPolicyText = it },
                label = { Text("نص سياسة الخصوصية وحماية البيانات") },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_privacy_text_input"),
                minLines = 4,
                maxLines = 8,
              )

              OutlinedTextField(
                value = returnPolicyText,
                onValueChange = { returnPolicyText = it },
                label = { Text("نص سياسة الاسترجاع والاستبدال") },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_return_text_input"),
                minLines = 3,
                maxLines = 7,
              )

              OutlinedTextField(
                value = termsOfServiceText,
                onValueChange = { termsOfServiceText = it },
                label = { Text("نص الشروط والأحكام") },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_terms_text_input"),
                minLines = 3,
                maxLines = 7,
              )

              SectionSaveButton(
                text = "حفظ السياسات وروابط الخصوصية",
                isSubmitting = uiState.isSubmitting,
                testTag = "admin_save_policies_button",
                onClick = { onSaveSettings(buildCombinedSettings()) },
              )
            }
          }

          // 5. إرسال إشعارات فورية للعملاء
          item {
            SettingsAccordionCard(
              title = "إرسال إشعار فوري للعملاء",
              subtitle = "عدد الحسابات المتاحة: ${uiState.customersCount} مستخدم",
              icon = Icons.Filled.NotificationsActive,
              expanded = expandedSection == StoreSettingsSection.BROADCAST_NOTIFICATIONS,
              testTag = "accordion_broadcast_notifications",
              onToggle = {
                expandedSection = if (expandedSection == StoreSettingsSection.BROADCAST_NOTIFICATIONS) {
                  null
                } else {
                  StoreSettingsSection.BROADCAST_NOTIFICATIONS
                }
              },
            ) {
              Surface(
                color = PrimaryYellowSoft,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "سيصل هذا الإشعار خارج التطبيق بصوت وتنبيه لجميع الأجهزة المسجلة (${uiState.customersCount} مستخدم)",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                  color = CharcoalBlack,
                  modifier = Modifier.padding(10.dp),
                )
              }

              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(NotificationType.OFFER_PROMO, NotificationType.GENERAL).forEach { typeOption ->
                  val selected = broadcastType == typeOption
                  FilterChip(
                    selected = selected,
                    onClick = { broadcastType = typeOption },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = PrimaryYellow,
                      selectedLabelColor = CharcoalBlack,
                    ),
                    label = { Text(typeOption.arabicLabel) },
                  )
                }
              }

              OutlinedTextField(
                value = broadcastTitle,
                onValueChange = { broadcastTitle = it },
                label = { Text("عنوان الإشعار") },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("broadcast_title_input"),
                singleLine = true,
              )

              OutlinedTextField(
                value = broadcastMessage,
                onValueChange = { broadcastMessage = it },
                label = { Text("نص رسالة الإشعار") },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("broadcast_message_input"),
                minLines = 2,
                maxLines = 4,
              )

              SettingsToggleRow(
                label = "إرسال لتجار الجملة المعتمدين فقط",
                checked = onlyWholesale,
                onCheckedChange = { onlyWholesale = it },
              )

              Button(
                onClick = {
                  onSendBroadcast(broadcastTitle, broadcastMessage, onlyWholesale, broadcastType)
                },
                enabled = !uiState.isBroadcasting && broadcastTitle.isNotBlank() && broadcastMessage.isNotBlank(),
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                  containerColor = CharcoalBlack,
                  contentColor = PrimaryYellow,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("admin_send_broadcast_button"),
              ) {
                if (uiState.isBroadcasting) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryYellow, strokeWidth = 2.dp)
                } else {
                  Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("إرسال الإشعار الآن", fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          if (canManagePayments) {
            item {
              OutlinedButton(
                onClick = onOpenPaymentSettings,
                border = BorderStroke(1.dp, CharcoalBlack),
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp),
              ) {
                Icon(Icons.Filled.Payments, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("الانتقال إلى إعدادات الدفع والمحافظ", color = CharcoalBlack, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
  }

  if (showCompanyDialog) {
    CompanyFormDialog(
      existing = editingCompany,
      defaultOrder = uiState.companies.size,
      onDismiss = { showCompanyDialog = false },
      onConfirm = { comp ->
        showCompanyDialog = false
        onSaveCompany(comp)
      },
    )
  }

  val compToDelete = deletingCompany
  if (compToDelete != null) {
    AlertDialog(
      onDismissRequest = { deletingCompany = null },
      containerColor = PureWhite,
      title = {
        Text("حذف الشركة", fontWeight = FontWeight.Bold, color = DiscountRed)
      },
      text = {
        Text("سيتم حذف شركة «${compToDelete.name}» وفك ارتباطها عن المنتجات المرتبطة بها. هل تريد المتابعة؟")
      },
      confirmButton = {
        Button(
          onClick = {
            deletingCompany = null
            onDeleteCompany(compToDelete.id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
        ) {
          Text("حذف")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingCompany = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@Composable
private fun CompanyFormDialog(
  existing: StoreCompany?,
  defaultOrder: Int,
  onDismiss: () -> Unit,
  onConfirm: (StoreCompany) -> Unit,
) {
  var name by remember { mutableStateOf(existing?.name.orEmpty()) }
  var imageUrl by remember { mutableStateOf(existing?.imageUrl.orEmpty()) }
  var displayOrder by remember { mutableStateOf((existing?.displayOrder ?: defaultOrder).toString()) }
  var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
  var showImageModal by remember { mutableStateOf(false) }
  var uploadError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة شركة جديدة" else "تعديل بيانات الشركة",
        fontWeight = FontWeight.Bold,
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 460.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (imageUrl.isNotBlank()) {
          Box(
            modifier = Modifier
              .size(92.dp)
              .align(Alignment.CenterHorizontally)
              .clip(RoundedCornerShape(16.dp))
              .background(PureWhite)
              .border(1.dp, BorderNeutral, RoundedCornerShape(16.dp)),
          ) {
            SubcomposeAsyncImage(
              model = imageUrl,
              contentDescription = "شعار الشركة",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            )
          }
        }

        OutlinedButton(
          onClick = { showImageModal = true },
          border = BorderStroke(1.dp, CharcoalBlack),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (imageUrl.isBlank()) "رفع شعار / صورة الشركة (اختياري)" else "تغيير شعار الشركة",
            color = CharcoalBlack,
            fontWeight = FontWeight.Bold,
          )
        }

        if (imageUrl.isNotBlank()) {
          TextButton(
            onClick = { imageUrl = "" },
            modifier = Modifier.align(Alignment.CenterHorizontally),
          ) {
            Text("إزالة الصورة", color = DiscountRed)
          }
        }

        if (!uploadError.isNullOrBlank()) {
          Text(uploadError.orEmpty(), color = DiscountRed, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("اسم الشركة أو الماركة *") },
          placeholder = { Text("مثال: أوكسي، برسيل، جهينة...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_company_name_input"),
        )

        OutlinedTextField(
          value = displayOrder,
          onValueChange = { displayOrder = it.filter { ch -> ch.isDigit() } },
          label = { Text("ترتيب الشركة") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        SettingsToggleRow(
          label = "تفعيل الشركة داخل النماذج والعروض",
          checked = isActive,
          onCheckedChange = { isActive = it },
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            (existing ?: StoreCompany()).copy(
              name = name.trim(),
              imageUrl = imageUrl.trim(),
              displayOrder = (displayOrder.toIntOrNull() ?: 0).coerceIn(0, 1000),
              isActive = isActive,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
        modifier = Modifier.testTag("admin_save_company_button"),
      ) {
        Text(if (existing == null) "إضافة الشركة" else "حفظ التعديلات", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )

  ImagePickerUploadModal(
    visible = showImageModal,
    title = "رفع شعار أو صورة الشركة",
    folder = CloudinaryFolder.CATEGORIES,
    allowMultiple = false,
    onUploadedUrls = { urls ->
      urls.firstOrNull()?.let {
        imageUrl = it
        uploadError = null
      }
      showImageModal = false
    },
    onError = { msg ->
      uploadError = msg
      showImageModal = false
    },
    onDismiss = { showImageModal = false },
  )
}

@Composable
private fun SettingsAccordionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  expanded: Boolean,
  testTag: String,
  onToggle: () -> Unit,
  content: @Composable () -> Unit,
) {
  AlEmanCard(
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onToggle)
          .testTag(testTag)
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .background(
              color = if (expanded) PrimaryYellow else PrimaryYellowSoft,
              shape = RoundedCornerShape(10.dp),
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = CharcoalBlack,
            modifier = Modifier.size(20.dp),
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
          contentDescription = if (expanded) "إغلاق القسم" else "فتح القسم",
          tint = CharcoalBlack,
        )
      }

      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          HorizontalDivider(color = BorderNeutral)
          content()
        }
      }
    }
  }
}

@Composable
private fun SectionSaveButton(
  text: String,
  isSubmitting: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = !isSubmitting,
    shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
    colors = ButtonDefaults.buttonColors(
      containerColor = PrimaryYellow,
      contentColor = CharcoalBlack,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .testTag(testTag),
  ) {
    if (isSubmitting) {
      CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CharcoalBlack, strokeWidth = 2.dp)
    } else {
      Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(text, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun StyleOptionChoiceCard(
  title: String,
  subtitle: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (selected) PrimaryYellowSoft else GeneralBackground,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) CharcoalBlack else BorderNeutral),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }
      if (selected) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = "محدد",
          tint = CharcoalBlack,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
private fun SettingsToggleRow(
  label: String,
  checked: Boolean,
  switchTestTag: String? = null,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = CharcoalBlack,
      modifier = Modifier.weight(1f),
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier,
      colors = SwitchDefaults.colors(
        checkedThumbColor = CharcoalBlack,
        checkedTrackColor = PrimaryYellow,
      ),
    )
  }
}

@Composable
private fun PhoneListEditorSection(
  title: String,
  badgeText: String,
  phones: List<StorePhoneEntry>,
  pendingLabel: String,
  pendingNumber: String,
  labelPlaceholder: String,
  addButtonLabel: String,
  addButtonTestTag: String,
  onPendingLabelChange: (String) -> Unit,
  onPendingNumberChange: (String) -> Unit,
  onAddPhone: () -> Unit,
  onRemovePhone: (Int) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
      BrandPillBadge(
        text = badgeText,
        containerColor = PrimaryYellowSoft,
        contentColor = CharcoalBlack,
      )
    }

    phones.forEachIndexed { idx, entry ->
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = GeneralBackground,
        border = BorderStroke(1.dp, BorderNeutral),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            if (entry.label.isNotBlank()) {
              Text(
                text = entry.label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }
            Text(
              text = entry.phoneNumber,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
          }
          IconButton(onClick = { onRemovePhone(idx) }) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "حذف الرقم",
              tint = CharcoalBlack,
            )
          }
        }
      }
    }

    OutlinedTextField(
      value = pendingLabel,
      onValueChange = onPendingLabelChange,
      label = { Text(labelPlaceholder) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )

    OutlinedTextField(
      value = pendingNumber,
      onValueChange = onPendingNumberChange,
      label = { Text("رقم الهاتف") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )

    OutlinedButton(
      onClick = onAddPhone,
      enabled = pendingNumber.isNotBlank(),
      border = BorderStroke(1.dp, CharcoalBlack),
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .testTag(addButtonTestTag),
    ) {
      Icon(Icons.Filled.Add, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(addButtonLabel, color = CharcoalBlack, fontWeight = FontWeight.Bold)
    }
  }
}
