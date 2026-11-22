package com.example.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.NotificationDeepLink
import com.example.core.auth.ReauthResult
import com.example.core.auth.clearCredentialManagerState
import com.example.core.auth.reauthenticateWithGoogle
import com.example.core.auth.signOutUser
import com.example.core.firebase.authStateFlow
import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.example.core.util.AppIconManager
import com.example.core.util.ArabicFormatter
import com.example.data.media.VideoDiskCache
import com.example.data.model.IntroMediaType
import com.example.data.model.NotificationType
import com.example.data.model.StorePolicyTab
import com.example.data.model.UserProfile
import com.example.ui.components.IntroReelVideoOverlay
import com.example.ui.components.LocalProductCardStyle
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.screens.account.AddressesScreen
import com.example.ui.screens.account.DeleteAccountConfirmDialog
import com.example.ui.screens.admin.AdminBannersScreen
import com.example.ui.screens.admin.AdminBrandShowcasesScreen
import com.example.ui.screens.admin.AdminCategoriesScreen
import com.example.ui.screens.admin.AdminCouponsScreen
import com.example.ui.screens.admin.AdminCustomersScreen
import com.example.ui.screens.admin.AdminDashboardShellScreen
import com.example.ui.screens.admin.AdminDeliveryZonesScreen
import com.example.ui.screens.admin.AdminFlashSalesScreen
import com.example.ui.screens.admin.AdminInventoryScreen
import com.example.ui.screens.admin.AdminOffersScreen
import com.example.ui.screens.admin.AdminOrdersScreen
import com.example.ui.screens.admin.AdminPaymentsScreen
import com.example.ui.screens.admin.AdminProductsScreen
import com.example.ui.screens.admin.AdminReportsScreen
import com.example.ui.screens.admin.AdminReviewsScreen
import com.example.ui.screens.admin.AdminStaffManagementScreen
import com.example.ui.screens.admin.AdminStoreSettingsScreen
import com.example.ui.screens.admin.DeliveryDriverOrdersScreen
import com.example.ui.screens.auth.CompleteProfileScreen
import com.example.ui.screens.auth.SignInScreen
import com.example.ui.screens.customer.BrandShowcaseScreen
import com.example.ui.screens.customer.CheckoutScreen
import com.example.ui.screens.customer.MyOrdersScreen
import com.example.ui.screens.customer.NotificationsScreen
import com.example.ui.screens.customer.OrderDetailsScreen
import com.example.ui.screens.customer.ProductDetailsScreen
import com.example.ui.screens.customer.StorePoliciesScreen
import com.example.ui.screens.customer.WishlistScreen
import com.example.ui.screens.shell.CustomerShellScreen
import com.example.ui.screens.splash.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminBannersViewModel
import com.example.ui.viewmodel.AdminBrandShowcasesViewModel
import com.example.ui.viewmodel.AdminCatalogViewModel
import com.example.ui.viewmodel.AdminCouponsViewModel
import com.example.ui.viewmodel.AdminDashboardViewModel
import com.example.ui.viewmodel.AdminDeliveryZonesViewModel
import com.example.ui.viewmodel.AdminFlashSalesViewModel
import com.example.ui.viewmodel.AdminInventoryViewModel
import com.example.ui.viewmodel.AdminOffersViewModel
import com.example.ui.viewmodel.AdminOrdersViewModel
import com.example.ui.viewmodel.AdminPaymentsViewModel
import com.example.ui.viewmodel.AdminReportsViewModel
import com.example.ui.viewmodel.AdminReviewsViewModel
import com.example.ui.viewmodel.AdminStaffViewModel
import com.example.ui.viewmodel.AdminStoreSettingsViewModel
import com.example.ui.viewmodel.CartOrderViewModel
import com.example.ui.viewmodel.CatalogViewModel
import com.example.ui.viewmodel.DeliveryDriverViewModel
import com.example.ui.viewmodel.NotificationsViewModel
import com.example.ui.viewmodel.ProductReviewsViewModel
import com.example.ui.viewmodel.ProfileLoadState
import com.example.ui.viewmodel.SessionUiState
import com.example.ui.viewmodel.SessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun AppNavHost(
  pendingNotificationDeepLink: NotificationDeepLink? = null,
  onConsumeNotificationDeepLink: () -> Unit = {},
  onOpenAlemanCashPreview: (() -> Unit)? = null,
) {
  var showSplash by rememberSaveable { mutableStateOf(true) }
  var introVideoDismissed by rememberSaveable { mutableStateOf(false) }
  var introWindowExpired by rememberSaveable { mutableStateOf(false) }

  val context = LocalContext.current
  val catalogViewModel: CatalogViewModel = viewModel(
    factory = CatalogViewModel.Factory(context),
  )
  val rootCatalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(rootCatalogState.storeSettings.numeralFormat) {
    ArabicFormatter.setNumeralFormat(rootCatalogState.storeSettings.numeralFormatEnum)
  }

  // Sync dynamic seasonal Android launcher icon from Firestore store settings once loaded.
  LaunchedEffect(
    rootCatalogState.isStoreSettingsLoaded,
    rootCatalogState.storeSettings.appIconTheme,
  ) {
    if (rootCatalogState.isStoreSettingsLoaded) {
      AppIconManager.applyIconTheme(context, rootCatalogState.storeSettings.appIconThemeEnum)
    }
  }

  // Keep active banner & intro videos cached on the phone and prune any replaced/removed videos.
  val activeVideoUrls = remember(
    rootCatalogState.banners,
    rootCatalogState.storeSettings.introVideoEnabled,
    rootCatalogState.storeSettings.introMediaType,
    rootCatalogState.storeSettings.introVideoUrl,
  ) {
    buildList {
      rootCatalogState.banners
        .filter { it.isActive && it.isVideoBanner }
        .forEach { add(it.videoUrl.trim()) }
      if (rootCatalogState.storeSettings.hasActiveIntroVideo &&
        rootCatalogState.storeSettings.introMediaTypeEnum == IntroMediaType.VIDEO
      ) {
        add(rootCatalogState.storeSettings.introVideoUrl.trim())
      }
    }.distinct()
  }

  LaunchedEffect(rootCatalogState.isStoreSettingsLoaded, rootCatalogState.isLoading, activeVideoUrls) {
    if (rootCatalogState.isStoreSettingsLoaded && !rootCatalogState.isLoading) {
      VideoDiskCache.syncActiveVideos(context, activeVideoUrls)
    }
  }

  // Only allow the post-splash intro reel to trigger within a short window after splash finishes,
  // so toggling it later mid-session never interrupts active browsing.
  LaunchedEffect(showSplash, rootCatalogState.isStoreSettingsLoaded) {
    if (!showSplash && !introVideoDismissed) {
      if (rootCatalogState.isStoreSettingsLoaded && !rootCatalogState.storeSettings.hasActiveIntroVideo) {
        introWindowExpired = true
      } else if (!rootCatalogState.isStoreSettingsLoaded) {
        delay(2_500L)
        if (!rootCatalogState.isStoreSettingsLoaded) {
          introWindowExpired = true
        }
      }
    }
  }

  if (showSplash) {
    SplashScreen(
      onSplashComplete = { showSplash = false },
    )
    return
  }

  val shouldShowIntroVideo = !introVideoDismissed &&
    !introWindowExpired &&
    rootCatalogState.isStoreSettingsLoaded &&
    rootCatalogState.storeSettings.hasActiveIntroVideo

  if (shouldShowIntroVideo) {
    IntroReelVideoOverlay(
      videoUrl = rootCatalogState.storeSettings.effectiveIntroMediaUrl,
      mediaType = rootCatalogState.storeSettings.introMediaTypeEnum,
      isVideoMuted = rootCatalogState.storeSettings.introVideoMuted,
      fallbackDurationSeconds = rootCatalogState.storeSettings.safeIntroVideoDurationSeconds,
      onFinish = {
        introVideoDismissed = true
        introWindowExpired = true
      },
    )
    return
  }

  val cartOrderViewModel: CartOrderViewModel = viewModel(
    factory = CartOrderViewModel.Factory(context),
  )

  val authFlow = remember { Firebase.auth.authStateFlow() }
  val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  CompositionLocalProvider(
    LocalProductCardStyle provides rootCatalogState.storeSettings.productCardStyleConfig,
  ) {
    val user = currentUser
    if (user == null) {
      val catalogState = rootCatalogState
      LaunchedEffect(
        catalogState.products,
        catalogState.flashSales,
        catalogState.brandShowcases,
        catalogState.storeSettings,
      ) {
        cartOrderViewModel.syncSessionContext(
          uid = null,
          userProfile = null,
          products = catalogState.products,
          flashSales = catalogState.flashSales,
          brandShowcases = catalogState.brandShowcases,
          isWholesalePreviewActive = false,
          storeSettings = catalogState.storeSettings,
        )
      }
      GuestNavGraph(
        catalogViewModel = catalogViewModel,
        cartOrderViewModel = cartOrderViewModel,
      )
    } else {
      key(user.uid) {
        val resolvedDisplayName = remember(user.uid) {
          user.displayName.orEmpty()
        }
        AuthenticatedAppRoot(
          uid = user.uid,
          initialDisplayName = resolvedDisplayName,
          email = user.email.orEmpty(),
          catalogViewModel = catalogViewModel,
          cartOrderViewModel = cartOrderViewModel,
          pendingNotificationDeepLink = pendingNotificationDeepLink,
          onConsumeNotificationDeepLink = onConsumeNotificationDeepLink,
          onOpenAlemanCashPreview = onOpenAlemanCashPreview,
        )
      }
    }
  }
}

@Composable
private fun GuestNavGraph(
  catalogViewModel: CatalogViewModel,
  cartOrderViewModel: CartOrderViewModel,
) {
  val context = LocalContext.current
  val navController = rememberNavController()
  val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
  val cartUiState by cartOrderViewModel.uiState.collectAsStateWithLifecycle()

  NavHost(
    navController = navController,
    startDestination = AppRoute.CustomerShell,
  ) {
    composable<AppRoute.CustomerShell> {
      CustomerShellScreen(
        userProfile = null,
        canAccessAdmin = false,
        defaultAddress = null,
        addressCount = 0,
        catalogState = catalogState,
        cartUiState = cartUiState,
        openCartTrigger = 0,
        onRetryCatalog = { catalogViewModel.retry() },
        onUpdateSearchQuery = { catalogViewModel.updateQuery(it) },
        onSelectSearchCategory = { catalogViewModel.selectCategoryFilter(it) },
        onToggleSearchOffers = { catalogViewModel.toggleOffersOnly() },
        onToggleSearchInStock = { catalogViewModel.toggleInStockOnly() },
        onToggleSearchWholesale = { catalogViewModel.toggleWholesaleEligibleOnly() },
        onSetSearchPriceRange = { minP, maxP -> catalogViewModel.setPriceRange(minP, maxP) },
        onSelectSearchSort = { catalogViewModel.setSortOption(it) },
        onResetSearchFilters = { catalogViewModel.resetFilters() },
        onOpenSearchPreset = { sortOption, onlyOffers ->
          catalogViewModel.openPresetSection(sortOption = sortOption, onlyOffers = onlyOffers)
        },
        onOpenProduct = { productId ->
          navController.navigate(AppRoute.ProductDetails(productId))
        },
        onAddToCart = { product -> cartOrderViewModel.addToCart(product, 1) },
        onAddFlashSaleItemToCart = { product, flashSaleId ->
          cartOrderViewModel.addToCart(product, 1, flashSaleId)
        },
        onUpdateCartQuantity = { product, qty, cartItemId ->
          cartOrderViewModel.updateQuantity(product, qty, cartItemId)
        },
        onRemoveCartItem = { cartItemId -> cartOrderViewModel.removeItem(cartItemId) },
        onCouponInputChange = { cartOrderViewModel.updateCouponInput(it) },
        onApplyCoupon = { cartOrderViewModel.applyCoupon() },
        onRemoveCoupon = { cartOrderViewModel.removeCoupon() },
        onProceedToCheckout = { navController.navigate(AppRoute.SignIn) },
        onOpenMyOrders = { navController.navigate(AppRoute.SignIn) },
        feedbackMessage = cartUiState.feedbackMessage,
        errorMessage = cartUiState.errorMessage,
        onClearMessages = { cartOrderViewModel.clearMessages() },
        onRequireSignIn = {
          navController.navigate(AppRoute.SignIn)
        },
        onUpdateProfile = { _, _, _ -> },
        onSubmitWholesaleRequest = { _, _, _ -> },
        onOpenAddresses = {
          navController.navigate(AppRoute.SignIn)
        },
        onOpenPolicies = { policyTab ->
          navController.navigate(AppRoute.StorePolicies(policyTab.code))
        },
        onNavigateToAdmin = {},
        onSignOut = {},
        onToggleWishlist = { productId ->
          catalogViewModel.toggleWishlist(productId) {
            navController.navigate(AppRoute.SignIn)
          }
        },
        onOpenWishlist = { navController.navigate(AppRoute.Wishlist) },
        onClearWishlistMessage = { catalogViewModel.clearWishlistMessage() },
        onOpenBrandShowcase = { showcaseId ->
          navController.navigate(AppRoute.BrandShowcase(showcaseId))
        },
        onLogZeroResultSearch = { catalogViewModel.logZeroResultSearch(it) },
      )
    }

    composable<AppRoute.BrandShowcase> { backStackEntry ->
      val route = backStackEntry.toRoute<AppRoute.BrandShowcase>()
      BrandShowcaseScreen(
        showcaseId = route.showcaseId,
        catalogState = catalogState,
        isWholesaleApproved = false,
        feedbackMessage = cartUiState.feedbackMessage,
        errorMessage = cartUiState.errorMessage,
        onClearMessages = { cartOrderViewModel.clearMessages() },
        onClearWishlistMessage = { catalogViewModel.clearWishlistMessage() },
        onBackClick = { navController.popBackStack() },
        onOpenProduct = { productId -> navController.navigate(AppRoute.ProductDetails(productId)) },
        onAddToCart = { product -> cartOrderViewModel.addToCart(product, 1) },
        onToggleWishlist = { productId ->
          catalogViewModel.toggleWishlist(productId) { navController.navigate(AppRoute.SignIn) }
        },
      )
    }

    composable<AppRoute.Wishlist> {
      WishlistScreen(
        catalogState = catalogState,
        isSignedIn = false,
        isWholesaleApproved = false,
        feedbackMessage = cartUiState.feedbackMessage,
        errorMessage = cartUiState.errorMessage,
        onClearMessages = {
          cartOrderViewModel.clearMessages()
          catalogViewModel.clearWishlistMessage()
        },
        onOpenProduct = { productId -> navController.navigate(AppRoute.ProductDetails(productId)) },
        onToggleWishlist = { productId ->
          catalogViewModel.toggleWishlist(productId) { navController.navigate(AppRoute.SignIn) }
        },
        onAddToCart = { product -> cartOrderViewModel.addToCart(product, 1) },
        onRequireSignIn = { navController.navigate(AppRoute.SignIn) },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.ProductDetails> { backStackEntry ->
      val route = backStackEntry.toRoute<AppRoute.ProductDetails>()
      val inCartQty = cartUiState.cartSummary.lines
        .filter { it.product.id == route.productId }
        .sumOf { it.quantity }
      val reviewsVm: ProductReviewsViewModel = viewModel(
        key = "reviews_${route.productId}",
        factory = ProductReviewsViewModel.Factory(context, route.productId),
      )
      val reviewsState by reviewsVm.uiState.collectAsStateWithLifecycle()
      ProductDetailsScreen(
        productId = route.productId,
        catalogState = catalogState,
        isWholesaleApproved = false,
        cartQuantity = inCartQty,
        feedbackMessage = cartUiState.feedbackMessage,
        errorMessage = cartUiState.errorMessage,
        onClearMessages = {
          cartOrderViewModel.clearMessages()
          reviewsVm.clearMessages()
        },
        onProductViewed = { catalogViewModel.recordProductViewed(it) },
        onOpenProduct = { productId ->
          navController.navigate(AppRoute.ProductDetails(productId))
        },
        onAddToCart = { product, qty, selectedColor, selectedSize ->
          cartOrderViewModel.addToCart(product, qty, null, selectedColor, selectedSize)
        },
        onBackClick = { navController.popBackStack() },
        reviewsState = reviewsState,
        currentUserId = null,
        onRequireSignIn = { navController.navigate(AppRoute.SignIn) },
        onToggleWishlist = { productId ->
          catalogViewModel.toggleWishlist(productId) { navController.navigate(AppRoute.SignIn) }
        },
        onClearWishlistMessage = { catalogViewModel.clearWishlistMessage() },
        onToggleStockAlert = { prod ->
          catalogViewModel.toggleStockAlert(prod) { navController.navigate(AppRoute.SignIn) }
        },
        onClearStockAlertMessage = { catalogViewModel.clearStockAlertMessage() },
      )
    }

    composable<AppRoute.SignIn> {
      SignInScreen(
        onAuthSuccess = {
          // تتولى authStateFlow الانتقال تلقائيًا لوضع الحساب المسجل
        },
        onOpenPolicies = {
          navController.navigate(AppRoute.StorePolicies(StorePolicyTab.PRIVACY.code))
        },
        onBackClick = {
          navController.popBackStack()
        },
      )
    }

    composable<AppRoute.StorePolicies> { backStackEntry ->
      val route = backStackEntry.toRoute<AppRoute.StorePolicies>()
      StorePoliciesScreen(
        settings = catalogState.storeSettings,
        initialTab = StorePolicyTab.fromCode(route.initialTab),
        isSignedIn = false,
        onBackClick = { navController.popBackStack() },
      )
    }
  }
}

@Composable
private fun AuthenticatedAppRoot(
  uid: String,
  initialDisplayName: String,
  email: String,
  catalogViewModel: CatalogViewModel,
  cartOrderViewModel: CartOrderViewModel,
  pendingNotificationDeepLink: NotificationDeepLink?,
  onConsumeNotificationDeepLink: () -> Unit,
  onOpenAlemanCashPreview: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val sessionViewModel: SessionViewModel = viewModel(
    key = "session_$uid",
    factory = SessionViewModel.Factory(context, uid),
  )
  val uiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
  val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(
    uid,
    uiState.profileState,
    uiState.effectiveWholesaleView,
    catalogState.products,
    catalogState.flashSales,
    catalogState.brandShowcases,
    catalogState.storeSettings,
  ) {
    val loadedProfile = (uiState.profileState as? ProfileLoadState.Loaded)?.profile
    cartOrderViewModel.syncSessionContext(
      uid = uid,
      userProfile = loadedProfile,
      products = catalogState.products,
      flashSales = catalogState.flashSales,
      brandShowcases = catalogState.brandShowcases,
      isWholesalePreviewActive = uiState.effectiveWholesaleView,
      storeSettings = catalogState.storeSettings,
    )
  }

  val handleSignOut: () -> Unit = {
    signOutUser(
      credentialManager = credentialManager,
      scope = scope,
    )
  }

  lateinit var startDeleteAccount: (allowReauthPrompt: Boolean) -> Unit
  startDeleteAccount = { allowReauthPrompt ->
    if (sessionViewModel.isReauthRequiredForDeletion()) {
      if (allowReauthPrompt) {
        scope.launch {
          when (val reauth = reauthenticateWithGoogle(context, credentialManager)) {
            ReauthResult.Success -> startDeleteAccount(false)
            ReauthResult.Cancelled -> Unit
            is ReauthResult.Error -> sessionViewModel.reportErrorMessage(reauth.message)
          }
        }
      } else {
        sessionViewModel.reportErrorMessage("تعذر التحقق من صلاحية الجلسة، يرجى تسجيل الخروج والدخول مجددًا قبل حذف الحساب.")
      }
    } else {
      sessionViewModel.deleteOwnAccount(
        onRequiresRecentLogin = {
          if (allowReauthPrompt) {
            scope.launch {
              when (val reauth = reauthenticateWithGoogle(context, credentialManager)) {
                ReauthResult.Success -> startDeleteAccount(false)
                ReauthResult.Cancelled -> Unit
                is ReauthResult.Error -> sessionViewModel.reportErrorMessage(reauth.message)
              }
            }
          }
        },
        onAccountDeleted = {
          scope.launch {
            clearCredentialManagerState(credentialManager)
          }
        },
      )
    }
  }

  if (uiState.isDeletingAccount) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = GeneralBackground,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          CircularProgressIndicator(color = DiscountRed)
          Text(
            text = "جاري حذف الحساب والبيانات الشخصية نهائيًا...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
      }
    }
    return
  }

  when (val profileState = uiState.profileState) {
    ProfileLoadState.Loading -> {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = GeneralBackground,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CircularProgressIndicator(color = RoyalBlue)
            Text(
              text = "جاري تجهيز بيانات حسابك...",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
        }
      }
    }

    ProfileLoadState.NeedsSetup -> {
      CompleteProfileScreen(
        initialName = initialDisplayName,
        email = email,
        isSubmitting = uiState.isSubmitting,
        errorMessage = uiState.errorMessage,
        onSubmitProfile = { fullName, phone, requestWholesale, companyName ->
          sessionViewModel.createInitialProfile(
            fullName = fullName,
            phone = phone,
            requestWholesale = requestWholesale,
            companyName = companyName,
          )
        },
        onSignOutClick = handleSignOut,
      )
    }

    is ProfileLoadState.Error -> {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = GeneralBackground,
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(AlEmanDimens.SpaceLg),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.CloudOff,
              title = "تعذر تحميل بيانات الحساب",
              description = profileState.message,
              actionLabel = "إعادة المحاولة",
              onActionClick = { sessionViewModel.retryLoad() },
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
              onClick = handleSignOut,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("error_state_sign_out_button"),
            ) {
              Text("تسجيل الخروج", color = DiscountRed)
            }
          }
        }
      }
    }

    is ProfileLoadState.Loaded -> {
      AuthenticatedNavGraph(
        userProfile = profileState.profile,
        uiState = uiState,
        sessionViewModel = sessionViewModel,
        catalogViewModel = catalogViewModel,
        cartOrderViewModel = cartOrderViewModel,
        pendingNotificationDeepLink = pendingNotificationDeepLink,
        onConsumeNotificationDeepLink = onConsumeNotificationDeepLink,
        onSignOut = handleSignOut,
        onRequestDeleteAccount = { startDeleteAccount(true) },
        onOpenAlemanCashPreview = onOpenAlemanCashPreview,
      )
    }
  }
}

@Composable
private fun AuthenticatedNavGraph(
  userProfile: UserProfile,
  uiState: SessionUiState,
  sessionViewModel: SessionViewModel,
  catalogViewModel: CatalogViewModel,
  cartOrderViewModel: CartOrderViewModel,
  pendingNotificationDeepLink: NotificationDeepLink?,
  onConsumeNotificationDeepLink: () -> Unit,
  onSignOut: () -> Unit,
  onRequestDeleteAccount: () -> Unit,
  onOpenAlemanCashPreview: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val navController = rememberNavController()
  val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
  val cartUiState by cartOrderViewModel.uiState.collectAsStateWithLifecycle()
  val notificationsVm: NotificationsViewModel = viewModel(
    key = "notifs_${userProfile.uid}",
    factory = NotificationsViewModel.Factory(context, userProfile.uid),
  )
  val notificationsState by notificationsVm.uiState.collectAsStateWithLifecycle()
  var openCartTrigger by rememberSaveable { mutableIntStateOf(0) }
  var openOffersTrigger by rememberSaveable { mutableIntStateOf(0) }

  fun routeFromNotification(typeCode: String, refId: String?) {
    val type = NotificationType.fromCode(typeCode)
    when (type) {
      NotificationType.ORDER_STATUS, NotificationType.PAYMENT_STATUS -> {
        if (!refId.isNullOrBlank()) {
          val hasDeliverPerm = StaffPermission.DELIVER_ORDERS in uiState.effectivePermissions
          val isOwnCustomerOrder = cartUiState.orders.any { it.id == refId }
          val isDriverOnly = uiState.staffMember?.staffRoleEnum == StaffRole.DELIVERY_DRIVER ||
            (hasDeliverPerm && !isOwnCustomerOrder)
          when {
            isDriverOnly -> navController.navigate(AppRoute.DeliveryDriverOrders(refId))
            StaffPermission.MANAGE_ORDERS in uiState.effectivePermissions && !isOwnCustomerOrder ->
              navController.navigate(AppRoute.AdminOrders)
            else -> navController.navigate(AppRoute.OrderDetails(refId))
          }
        }
      }
      NotificationType.OFFER_PROMO -> {
        openOffersTrigger++
        navController.popBackStack(AppRoute.CustomerShell, inclusive = false)
      }
      NotificationType.STOCK_ALERT -> {
        if (!refId.isNullOrBlank()) {
          navController.navigate(AppRoute.ProductDetails(refId))
        } else {
          navController.popBackStack(AppRoute.CustomerShell, inclusive = false)
        }
      }
      NotificationType.WHOLESALE_STATUS, NotificationType.GENERAL -> {
        navController.popBackStack(AppRoute.CustomerShell, inclusive = false)
      }
    }
  }

  LaunchedEffect(pendingNotificationDeepLink) {
    val deepLink = pendingNotificationDeepLink ?: return@LaunchedEffect
    routeFromNotification(deepLink.type, deepLink.referenceId)
    onConsumeNotificationDeepLink()
  }

  NavHost(
    navController = navController,
    startDestination = AppRoute.CustomerShell,
  ) {
    composable<AppRoute.CustomerShell> {
      CustomerShellScreen(
        userProfile = userProfile,
        canAccessAdmin = uiState.canAccessAdminDashboard,
        defaultAddress = uiState.defaultAddress,
        addressCount = uiState.addresses.size,
        catalogState = catalogState,
        cartUiState = cartUiState,
        openCartTrigger = openCartTrigger,
        onRetryCatalog = { catalogViewModel.retry() },
        onUpdateSearchQuery = { catalogViewModel.updateQuery(it) },
        onSelectSearchCategory = { catalogViewModel.selectCategoryFilter(it) },
        onToggleSearchOffers = { catalogViewModel.toggleOffersOnly() },
        onToggleSearchInStock = { catalogViewModel.toggleInStockOnly() },
        onToggleSearchWholesale = { catalogViewModel.toggleWholesaleEligibleOnly() },
        onSetSearchPriceRange = { minP, maxP -> catalogViewModel.setPriceRange(minP, maxP) },
        onSelectSearchSort = { catalogViewModel.setSortOption(it) },
        onResetSearchFilters = { catalogViewModel.resetFilters() },
        onOpenSearchPreset = { sortOption, onlyOffers ->
          catalogViewModel.openPresetSection(sortOption = sortOption, onlyOffers = onlyOffers)
        },
        onOpenProduct = { productId ->
          navController.navigate(AppRoute.ProductDetails(productId))
        },
        onAddToCart = { product -> cartOrderViewModel.addToCart(product, 1) },
        onUpdateCartQuantity = { product, qty, cartItemId ->
          cartOrderViewModel.updateQuantity(product, qty, cartItemId)
        },
        onRemoveCartItem = { cartItemId -> cartOrderViewModel.removeItem(cartItemId) },
        onCouponInputChange = { cartOrderViewModel.updateCouponInput(it) },
        onApplyCoupon = { cartOrderViewModel.applyCoupon() },
        onRemoveCoupon = { cartOrderViewModel.removeCoupon() },
        onProceedToCheckout = { navController.navigate(AppRoute.Checkout) },
        onOpenMyOrders = { navController.navigate(AppRoute.MyOrders) },
        feedbackMessage = cartUiState.feedbackMessage ?: uiState.feedbackMessage,
        errorMessage = cartUiState.errorMessage ?: uiState.errorMessage,
        onClearMessages = {
          cartOrderViewModel.clearMessages()
          sessionViewModel.clearMessages()
        },
        onRequireSignIn = {},
        onUpdateProfile = { fullName, phone, companyName ->
          sessionViewModel.updateProfile(fullName, phone, companyName)
        },
        onSubmitWholesaleRequest = { fullName, phone, companyName ->
          sessionViewModel.submitWholesaleRequest(fullName, phone, companyName)
        },
        onOpenAddresses = {
          navController.navigate(AppRoute.Addresses)
        },
        onNavigateToAdmin = {
          if (uiState.canAccessAdminDashboard) {
            navController.navigate(AppRoute.AdminDashboard)
          }
        },
        onSignOut = onSignOut,
        unreadNotificationsCount = notificationsState.unreadCount,
        onOpenNotifications = { navController.navigate(AppRoute.Notifications) },
        openOffersTrigger = openOffersTrigger,
        isWholesalePreviewActive = uiState.effectiveWholesaleView,
        canToggleWholesalePreview = uiState.canToggleWholesalePreview,
        onToggleWholesalePreview = { sessionViewModel.toggleWholesalePreview() },
        onAddFlashSaleItemToCart = { product, flashSaleId ->
          cartOrderViewModel.addToCart(product, 1, flashSaleId)
        },
        onOpenPolicies = { policyTab ->
          navController.navigate(AppRoute.StorePolicies(policyTab.code))
        },
        requiresRecentLoginForDeletion = sessionViewModel.isReauthRequiredForDeletion(),
        onDeleteAccount = onRequestDeleteAccount,
        onToggleWishlist = { productId ->
          catalogViewModel.toggleWishlist(productId) {}
        },
        onOpenWishlist = { navController.navigate(AppRoute.Wishlist) },
        onClearWishlistMessage = { catalogViewModel.clearWishlistMessage() },
        onOpenBrandShowcase = { showcaseId ->
          navController.navigate(AppRoute.BrandShowcase(showcaseId))
        },
        onLogZeroResultSearch = { catalogViewModel.logZeroResultSearch(it) },
      )
    }

    composable<AppRoute.BrandShowcase> { backStackEntry ->
      val route = backStackEntry.toRoute<AppRoute.BrandShowcase>()
      BrandShowcaseScreen(
        showcaseId = route.showcaseId,
        catalogState = catalogState,
        isWholesaleApproved = uiState.effectiveWholesaleView,
        feedbackMessage = cartUiState.feedbackMessage,
        errorMessage = cartUiState.errorMessage,
        onClearMessages = { cartOrderViewModel.clearMessages() },
        onClearWishlistMessage = { catalogViewModel.clearWishlistMessage() },
        onBackClick = { navController.popBackStack() },
        onOpenProduct = { productId -> navController.navigate(AppRoute.ProductDetails(productId)) },
        onAddToCart = { product -> cartOrderViewModel.addToCart(product, 1) },
        onToggleWishlist = { productId ->
          catalogViewModel.toggleWishlist(productId) {}
        },
      )
    }

    composable<AppRoute.Wishlist> {
      WishlistScreen(
        catalogState = catalogState,
        isSignedIn = true,
        isWholesaleApproved = uiState.effectiveWholesaleView,
        feedbackMessage = cartUiState.feedbackMessage,
        errorMessage = cartUiState.errorMessage,
        onClearMessages = {
          cartOrderViewModel.clearMessages()
          catalogViewModel.clearWishlistMessage()
        },
        onOpenProduct = { productId -> navController.navigate(AppRoute.ProductDetails(productId)) },
        onToggleWishlist = { productId ->
          catalogViewModel.toggleWishlist(productId) {}
        },
        onAddToCart = { product -> cartOrderViewModel.addToCart(product, 1) },
        onRequireSignIn = {},
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.Notifications> {
      NotificationsScreen(
        uiState = notificationsState,
        onMarkAsRead = { notificationsVm.markAsRead(it) },
        onMarkAllAsRead = { notificationsVm.markAllAsRead() },
        onDeleteNotification = { notificationsVm.deleteNotification(it) },
        onNotificationClick = { notif ->
          if (!notif.isRead) {
            notificationsVm.markAsRead(notif.id)
          }
          routeFromNotification(notif.type, notif.referenceId)
        },
        onClearMessages = { notificationsVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.ProductDetails> { backStackEntry ->
      val route = backStackEntry.toRoute<AppRoute.ProductDetails>()
      val inCartQty = cartUiState.cartSummary.lines
        .filter { it.product.id == route.productId }
        .sumOf { it.quantity }
      val reviewsVm: ProductReviewsViewModel = viewModel(
        key = "reviews_${route.productId}",
        factory = ProductReviewsViewModel.Factory(context, route.productId),
      )
      val reviewsState by reviewsVm.uiState.collectAsStateWithLifecycle()
      ProductDetailsScreen(
        productId = route.productId,
        catalogState = catalogState,
        isWholesaleApproved = uiState.effectiveWholesaleView,
        cartQuantity = inCartQty,
        feedbackMessage = cartUiState.feedbackMessage,
        errorMessage = cartUiState.errorMessage,
        onClearMessages = {
          cartOrderViewModel.clearMessages()
          reviewsVm.clearMessages()
        },
        onProductViewed = { catalogViewModel.recordProductViewed(it) },
        onOpenProduct = { productId ->
          navController.navigate(AppRoute.ProductDetails(productId))
        },
        onAddToCart = { product, qty, selectedColor, selectedSize ->
          cartOrderViewModel.addToCart(product, qty, null, selectedColor, selectedSize)
        },
        onBackClick = { navController.popBackStack() },
        reviewsState = reviewsState,
        currentUserId = userProfile.uid,
        onSubmitReview = { prodName, rating, comment ->
          reviewsVm.submitReview(
            productName = prodName,
            userName = userProfile.fullName,
            rating = rating,
            comment = comment,
          )
        },
        onDeleteReview = { reviewId -> reviewsVm.deleteOwnReview(reviewId) },
        onToggleWishlist = { productId ->
          catalogViewModel.toggleWishlist(productId) {}
        },
        onClearWishlistMessage = { catalogViewModel.clearWishlistMessage() },
        onToggleStockAlert = { prod ->
          catalogViewModel.toggleStockAlert(prod)
        },
        onClearStockAlertMessage = { catalogViewModel.clearStockAlertMessage() },
      )
    }

    composable<AppRoute.Checkout> {
      CheckoutScreen(
        cartUiState = cartUiState,
        addresses = uiState.addresses,
        defaultAddress = uiState.defaultAddress,
        isWholesaleApproved = uiState.effectiveWholesaleView,
        storeName = catalogState.storeSettings.storeName,
        storeAddress = catalogState.storeSettings.storeAddress,
        storePhone = catalogState.storeSettings.supportPhone.ifBlank { catalogState.storeSettings.whatsappNumber },
        customerName = userProfile.fullName,
        customerPhone = userProfile.phone.orEmpty(),
        onSelectFulfillmentMethod = { cartOrderViewModel.selectFulfillmentMethod(it) },
        onSelectZone = { cartOrderViewModel.selectDeliveryZone(it) },
        onAutoMatchZone = { cartOrderViewModel.autoMatchDeliveryZone(it) },
        onCouponInputChange = { cartOrderViewModel.updateCouponInput(it) },
        onApplyCoupon = { cartOrderViewModel.applyCoupon() },
        onRemoveCoupon = { cartOrderViewModel.removeCoupon() },
        onOpenAddresses = { navController.navigate(AppRoute.Addresses) },
        onClearMessages = { cartOrderViewModel.clearMessages() },
        onPlaceOrder = { fulfillmentMethod, address, paymentMethod, transferRef, receiptUrl, notes, pickupName, pickupPhone ->
          cartOrderViewModel.placeOrder(
            fulfillmentMethod = fulfillmentMethod,
            address = address,
            paymentMethod = paymentMethod,
            transferReference = transferRef,
            receiptImageUrl = receiptUrl,
            notes = notes,
            pickupRecipientName = pickupName,
            pickupPhone = pickupPhone,
            storeAddress = catalogState.storeSettings.storeAddress,
          ) { createdOrderId ->
            navController.navigate(AppRoute.OrderDetails(createdOrderId)) {
              popUpTo(AppRoute.CustomerShell) { inclusive = false }
            }
          }
        },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.MyOrders> {
      MyOrdersScreen(
        uiState = cartUiState,
        onOpenOrderDetails = { orderId ->
          navController.navigate(AppRoute.OrderDetails(orderId))
        },
        onReorder = { order ->
          cartOrderViewModel.reorder(order) {
            openCartTrigger++
            navController.popBackStack(AppRoute.CustomerShell, inclusive = false)
          }
        },
        onShopNow = { navController.popBackStack(AppRoute.CustomerShell, inclusive = false) },
        onClearMessages = { cartOrderViewModel.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.OrderDetails> { backStackEntry ->
      val route = backStackEntry.toRoute<AppRoute.OrderDetails>()
      OrderDetailsScreen(
        orderId = route.orderId,
        uiState = cartUiState,
        onReorder = { order ->
          cartOrderViewModel.reorder(order) {
            openCartTrigger++
            navController.popBackStack(AppRoute.CustomerShell, inclusive = false)
          }
        },
        onCancelOrder = { id, reason -> cartOrderViewModel.cancelOrder(id, reason) },
        onClearMessages = { cartOrderViewModel.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.Addresses> {
      AddressesScreen(
        addresses = uiState.addresses,
        defaultRecipientName = userProfile.fullName,
        defaultPhone = userProfile.phone,
        isSubmitting = uiState.isSubmitting,
        feedbackMessage = uiState.feedbackMessage,
        errorMessage = uiState.errorMessage,
        onClearMessages = { sessionViewModel.clearMessages() },
        onSaveAddress = { address -> sessionViewModel.saveAddress(address) },
        onSetDefault = { address -> sessionViewModel.setDefaultAddress(address) },
        onDeleteAddress = { addressId -> sessionViewModel.deleteAddress(addressId) },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminDashboard> {
      val adminDashboardVm: AdminDashboardViewModel = viewModel(
        key = "admin_dash_${userProfile.uid}_${uiState.effectivePermissions.hashCode()}",
        factory = AdminDashboardViewModel.Factory(context, uiState.effectivePermissions),
      )
      val dashboardState by adminDashboardVm.uiState.collectAsStateWithLifecycle()
      val roleLabel = uiState.staffMember?.staffRoleEnum?.arabicLabel ?: userProfile.staffRoleEnum.arabicLabel
      AdminDashboardShellScreen(
        roleLabel = roleLabel,
        allowedPermissions = uiState.effectivePermissions,
        dashboardState = dashboardState,
        isWholesalePreviewActive = uiState.effectiveWholesaleView,
        canToggleWholesalePreview = uiState.canToggleWholesalePreview,
        onToggleWholesalePreview = { sessionViewModel.toggleWholesalePreview() },
        onBackClick = { navController.popBackStack() },
        onOpenBrandShowcases = { navController.navigate(AppRoute.AdminBrandShowcases) },
        onSelectSection = { permission ->
          when (permission) {
            StaffPermission.MANAGE_CATEGORIES -> navController.navigate(AppRoute.AdminCategories)
            StaffPermission.MANAGE_PRODUCTS -> navController.navigate(AppRoute.AdminProducts)
            StaffPermission.MANAGE_STAFF -> navController.navigate(AppRoute.AdminStaff)
            StaffPermission.MANAGE_CUSTOMERS, StaffPermission.APPROVE_WHOLESALE -> navController.navigate(AppRoute.AdminCustomers)
            StaffPermission.MANAGE_ORDERS -> navController.navigate(AppRoute.AdminOrders)
            StaffPermission.DELIVER_ORDERS -> navController.navigate(AppRoute.DeliveryDriverOrders())
            StaffPermission.MANAGE_PAYMENTS -> navController.navigate(AppRoute.AdminPayments)
            StaffPermission.MANAGE_DELIVERY -> navController.navigate(AppRoute.AdminDeliveryZones)
            StaffPermission.MANAGE_COUPONS -> navController.navigate(AppRoute.AdminCoupons)
            StaffPermission.MANAGE_INVENTORY -> navController.navigate(AppRoute.AdminInventory)
            StaffPermission.VIEW_REPORTS, StaffPermission.VIEW_SALES_SUMMARY -> navController.navigate(AppRoute.AdminReports)
            StaffPermission.MANAGE_OFFERS -> navController.navigate(AppRoute.AdminOffers)
            StaffPermission.MANAGE_FLASH_SALES -> navController.navigate(AppRoute.AdminFlashSales)
            StaffPermission.MANAGE_BANNERS -> navController.navigate(AppRoute.AdminBanners)
            StaffPermission.MANAGE_REVIEWS -> navController.navigate(AppRoute.AdminReviews)
            StaffPermission.MANAGE_STORE_SETTINGS -> navController.navigate(AppRoute.AdminStoreSettings)
            StaffPermission.PREVIEW_WHOLESALE_STORE -> sessionViewModel.toggleWholesalePreview()
          }
        },
      )
    }

    composable<AppRoute.AdminCategories> {
      val adminCatalogVm: AdminCatalogViewModel = viewModel(
        factory = AdminCatalogViewModel.Factory(context),
      )
      val adminCatalogState by adminCatalogVm.uiState.collectAsStateWithLifecycle()
      AdminCategoriesScreen(
        uiState = adminCatalogState,
        onSaveCategory = { adminCatalogVm.saveCategory(it) },
        onDeleteCategory = { adminCatalogVm.deleteCategory(it) },
        onClearMessages = { adminCatalogVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminProducts> {
      val adminCatalogVm: AdminCatalogViewModel = viewModel(
        factory = AdminCatalogViewModel.Factory(context),
      )
      val adminCatalogState by adminCatalogVm.uiState.collectAsStateWithLifecycle()
      AdminProductsScreen(
        uiState = adminCatalogState,
        onSaveProduct = { adminCatalogVm.saveProduct(it) },
        onDeleteProduct = { adminCatalogVm.deleteProduct(it) },
        onClearMessages = { adminCatalogVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminStaff> {
      val adminStaffVm: AdminStaffViewModel = viewModel(
        factory = AdminStaffViewModel.Factory(context),
      )
      val adminStaffState by adminStaffVm.uiState.collectAsStateWithLifecycle()
      AdminStaffManagementScreen(
        uiState = adminStaffState,
        onAssignStaff = { user, role, permissions, isActive ->
          adminStaffVm.assignStaff(user, role, permissions, isActive)
        },
        onRemoveStaff = { uid -> adminStaffVm.removeStaff(uid) },
        onClearMessages = { adminStaffVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminCustomers> {
      val adminStaffVm: AdminStaffViewModel = viewModel(
        factory = AdminStaffViewModel.Factory(context),
      )
      val adminStaffState by adminStaffVm.uiState.collectAsStateWithLifecycle()
      AdminCustomersScreen(
        uiState = adminStaffState,
        onUpdateWholesaleDecision = { uid, approved -> adminStaffVm.updateWholesaleDecision(uid, approved) },
        onClearMessages = { adminStaffVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminOrders> {
      val adminOrdersVm: AdminOrdersViewModel = viewModel(
        factory = AdminOrdersViewModel.Factory(context),
      )
      val adminOrdersState by adminOrdersVm.uiState.collectAsStateWithLifecycle()
      val canPrepareAndPrint = StaffPermission.MANAGE_ORDERS in uiState.effectivePermissions ||
        StaffPermission.MANAGE_DELIVERY in uiState.effectivePermissions
      AdminOrdersScreen(
        uiState = adminOrdersState,
        onSelectStatusFilter = { adminOrdersVm.setStatusFilter(it) },
        onSearchQueryChange = { adminOrdersVm.setSearchQuery(it) },
        onUpdateOrderStatus = { orderId, status, note -> adminOrdersVm.updateOrderStatus(orderId, status, note) },
        onAssignDriver = { order, driver -> adminOrdersVm.assignOrderToDriver(order, driver) },
        canPrepareAndPrint = canPrepareAndPrint,
        storeName = catalogState.storeSettings.storeName,
        storePhone = catalogState.storeSettings.supportPhone.ifBlank { catalogState.storeSettings.whatsappNumber },
        onClearMessages = { adminOrdersVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.DeliveryDriverOrders> { backStackEntry ->
      val route = backStackEntry.toRoute<AppRoute.DeliveryDriverOrders>()
      val driverVm: DeliveryDriverViewModel = viewModel(
        key = "driver_orders_${userProfile.uid}",
        factory = DeliveryDriverViewModel.Factory(context, userProfile.uid),
      )
      val driverState by driverVm.uiState.collectAsStateWithLifecycle()
      DeliveryDriverOrdersScreen(
        uiState = driverState,
        focusedOrderId = route.focusedOrderId,
        onToggleShowCompleted = { driverVm.setShowCompleted(it) },
        onConfirmDelivered = { order, note, otp -> driverVm.markOrderDelivered(order, note, otp) },
        onClearMessages = { driverVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminInventory> {
      val adminInventoryVm: AdminInventoryViewModel = viewModel(
        factory = AdminInventoryViewModel.Factory(context),
      )
      val adminInventoryState by adminInventoryVm.uiState.collectAsStateWithLifecycle()
      AdminInventoryScreen(
        uiState = adminInventoryState,
        onSearchQueryChange = { adminInventoryVm.setSearchQuery(it) },
        onToggleLowStockFilter = { adminInventoryVm.toggleLowStockFilter() },
        onUpdateStock = { productId, qty, threshold, isActive ->
          adminInventoryVm.updateStock(productId, qty, threshold, isActive)
        },
        onClearMessages = { adminInventoryVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminReports> {
      val adminReportsVm: AdminReportsViewModel = viewModel(
        factory = AdminReportsViewModel.Factory(context),
      )
      val adminReportsState by adminReportsVm.uiState.collectAsStateWithLifecycle()
      AdminReportsScreen(
        uiState = adminReportsState,
        onSelectPeriod = { adminReportsVm.selectPeriod(it) },
        onOpenAdminProducts = { navController.navigate(AppRoute.AdminProducts) },
        onDeleteSearchMiss = { adminReportsVm.deleteSearchMiss(it) },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminCoupons> {
      val adminCouponsVm: AdminCouponsViewModel = viewModel(
        factory = AdminCouponsViewModel.Factory(context),
      )
      val adminCouponsState by adminCouponsVm.uiState.collectAsStateWithLifecycle()
      AdminCouponsScreen(
        uiState = adminCouponsState,
        onSaveCoupon = { adminCouponsVm.saveCoupon(it) },
        onDeleteCoupon = { adminCouponsVm.deleteCoupon(it) },
        onClearMessages = { adminCouponsVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminDeliveryZones> {
      val adminZonesVm: AdminDeliveryZonesViewModel = viewModel(
        factory = AdminDeliveryZonesViewModel.Factory(context),
      )
      val adminZonesState by adminZonesVm.uiState.collectAsStateWithLifecycle()
      AdminDeliveryZonesScreen(
        uiState = adminZonesState,
        onSaveZone = { adminZonesVm.saveDeliveryZone(it) },
        onDeleteZone = { adminZonesVm.deleteDeliveryZone(it) },
        onClearMessages = { adminZonesVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminPayments> {
      val adminPaymentsVm: AdminPaymentsViewModel = viewModel(
        factory = AdminPaymentsViewModel.Factory(context),
      )
      val adminPaymentsState by adminPaymentsVm.uiState.collectAsStateWithLifecycle()
      AdminPaymentsScreen(
        uiState = adminPaymentsState,
        onSelectPaymentFilter = { adminPaymentsVm.setPaymentStatusFilter(it) },
        onUpdatePaymentStatus = { orderId, pStatus, oStatus, note ->
          adminPaymentsVm.updatePaymentStatus(orderId, pStatus, oStatus, note)
        },
        onDeleteSingleReceipt = { orderId -> adminPaymentsVm.deleteSingleReceipt(orderId) },
        onDeleteReceiptsBatch = { scope, customDays ->
          adminPaymentsVm.deleteReceiptsBatch(scope, customDays)
        },
        onSaveSettings = { adminPaymentsVm.savePaymentSettings(it) },
        onClearMessages = { adminPaymentsVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
        onOpenAlemanCashPreview = onOpenAlemanCashPreview,
      )
    }

    composable<AppRoute.AdminBanners> {
      val adminBannersVm: AdminBannersViewModel = viewModel(
        factory = AdminBannersViewModel.Factory(context),
      )
      val adminBannersState by adminBannersVm.uiState.collectAsStateWithLifecycle()
      AdminBannersScreen(
        uiState = adminBannersState,
        onSaveBanner = { adminBannersVm.saveBanner(it) },
        onToggleBannerActive = { adminBannersVm.toggleBannerActive(it) },
        onDeleteBanner = { adminBannersVm.deleteBanner(it) },
        onClearMessages = { adminBannersVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminBrandShowcases> {
      val adminShowcasesVm: AdminBrandShowcasesViewModel = viewModel(
        factory = AdminBrandShowcasesViewModel.Factory(context),
      )
      val showcasesState by adminShowcasesVm.uiState.collectAsStateWithLifecycle()
      AdminBrandShowcasesScreen(
        uiState = showcasesState,
        onSaveShowcase = { showcase, onSavedId -> adminShowcasesVm.saveShowcase(showcase, onSavedId) },
        onToggleShowcaseActive = { adminShowcasesVm.toggleShowcaseActive(it) },
        onMoveShowcaseOrder = { showcase, delta -> adminShowcasesVm.moveShowcaseOrder(showcase, delta) },
        onSaveShowcaseCategory = { showcase, cat -> adminShowcasesVm.saveShowcaseCategory(showcase, cat) },
        onDeleteShowcaseCategory = { showcase, catId -> adminShowcasesVm.deleteShowcaseCategory(showcase, catId) },
        onSaveShowcaseProduct = { showcase, prod -> adminShowcasesVm.saveShowcaseProduct(showcase, prod) },
        onLinkExistingProduct = { showcase, prod, catId, exclusive ->
          adminShowcasesVm.linkExistingProduct(showcase, prod, catId, exclusive)
        },
        onUnlinkProduct = { adminShowcasesVm.unlinkProductFromShowcase(it) },
        onToggleProductExclusive = { adminShowcasesVm.toggleProductExclusive(it) },
        onDeleteShowcase = { adminShowcasesVm.deleteShowcase(it) },
        onClearMessages = { adminShowcasesVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminReviews> {
      val adminReviewsVm: AdminReviewsViewModel = viewModel(
        factory = AdminReviewsViewModel.Factory(context),
      )
      val adminReviewsState by adminReviewsVm.uiState.collectAsStateWithLifecycle()
      AdminReviewsScreen(
        uiState = adminReviewsState,
        onSelectFilter = { adminReviewsVm.setFilter(it) },
        onSearchQueryChange = { adminReviewsVm.setSearchQuery(it) },
        onToggleVisibility = { adminReviewsVm.toggleReviewVisibility(it) },
        onDeleteReview = { adminReviewsVm.deleteReview(it) },
        onClearMessages = { adminReviewsVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminOffers> {
      val adminOffersVm: AdminOffersViewModel = viewModel(
        factory = AdminOffersViewModel.Factory(context),
      )
      val adminOffersState by adminOffersVm.uiState.collectAsStateWithLifecycle()
      AdminOffersScreen(
        uiState = adminOffersState,
        onSelectFilter = { adminOffersVm.setFilter(it) },
        onSearchQueryChange = { adminOffersVm.setSearchQuery(it) },
        onUpdateOfferSettings = { product, discountPrice, isOffer, isFeatured ->
          adminOffersVm.updateOfferSettings(product, discountPrice, isOffer, isFeatured)
        },
        onClearMessages = { adminOffersVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminFlashSales> {
      val adminFlashVm: AdminFlashSalesViewModel = viewModel(
        factory = AdminFlashSalesViewModel.Factory(context),
      )
      val adminFlashState by adminFlashVm.uiState.collectAsStateWithLifecycle()
      AdminFlashSalesScreen(
        uiState = adminFlashState,
        onSaveFlashSale = { sale, onSaved -> adminFlashVm.saveFlashSale(sale, onSaved) },
        onToggleActive = { adminFlashVm.toggleFlashSaleActive(it) },
        onDeleteFlashSale = { adminFlashVm.deleteFlashSale(it) },
        onClearMessages = { adminFlashVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
      )
    }

    composable<AppRoute.AdminStoreSettings> {
      val adminStoreVm: AdminStoreSettingsViewModel = viewModel(
        factory = AdminStoreSettingsViewModel.Factory(context),
      )
      val adminStoreState by adminStoreVm.uiState.collectAsStateWithLifecycle()
      AdminStoreSettingsScreen(
        uiState = adminStoreState,
        canManagePayments = StaffPermission.MANAGE_PAYMENTS in uiState.effectivePermissions,
        onSaveSettings = { adminStoreVm.saveSettings(it) },
        onSaveCompany = { adminStoreVm.saveCompany(it) },
        onDeleteCompany = { adminStoreVm.deleteCompany(it) },
        onSendBroadcast = { title, message, onlyWholesale, type ->
          adminStoreVm.sendBroadcastNotification(title, message, onlyWholesale, type)
        },
        onOpenPaymentSettings = { navController.navigate(AppRoute.AdminPayments) },
        onClearMessages = { adminStoreVm.clearMessages() },
        onBackClick = { navController.popBackStack() },
        canToggleWholesalePreview = uiState.canToggleWholesalePreview,
        isWholesalePreviewActive = uiState.effectiveWholesaleView,
        onToggleWholesalePreview = { sessionViewModel.toggleWholesalePreview() },
      )
    }

    composable<AppRoute.StorePolicies> { backStackEntry ->
      val route = backStackEntry.toRoute<AppRoute.StorePolicies>()
      var showDeleteFromPolicies by remember { mutableStateOf(false) }
      StorePoliciesScreen(
        settings = catalogState.storeSettings,
        initialTab = StorePolicyTab.fromCode(route.initialTab),
        isSignedIn = true,
        onRequestDeleteAccount = { showDeleteFromPolicies = true },
        onBackClick = { navController.popBackStack() },
      )
      if (showDeleteFromPolicies) {
        DeleteAccountConfirmDialog(
          userEmail = userProfile.email,
          requiresRecentLogin = sessionViewModel.isReauthRequiredForDeletion(),
          onDismiss = { showDeleteFromPolicies = false },
          onConfirmDelete = {
            showDeleteFromPolicies = false
            onRequestDeleteAccount()
          },
        )
      }
    }
  }
}
