package com.example.ui.screens.shell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.model.WholesaleStatus
import com.example.data.model.BannerTargetType
import com.example.data.model.DeliveryAddress
import com.example.data.model.Product
import com.example.data.model.StorePolicyTab
import com.example.data.model.UserProfile
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanPrimaryButton
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.QrBarcodeScannerDialog
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.StoreSearchTriggerBar
import com.example.ui.components.SuccessBadge
import com.example.ui.navigation.CustomerTab
import com.example.ui.screens.account.DeleteAccountConfirmDialog
import com.example.ui.screens.account.EditProfileDialog
import com.example.ui.screens.account.SignOutConfirmDialog
import com.example.ui.screens.account.WholesaleRequestDialog
import com.example.ui.screens.customer.AboutStoreScreen
import com.example.ui.screens.customer.CartTabContent
import com.example.ui.screens.customer.CategoriesTabContent
import com.example.ui.screens.customer.HomeTabContent
import com.example.ui.screens.customer.SearchTabContent
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CartAndOrdersUiState
import com.example.ui.viewmodel.CatalogSortOption
import com.example.ui.viewmodel.CatalogUiState
import kotlinx.coroutines.launch

@Composable
fun CustomerShellScreen(
  userProfile: UserProfile?,
  canAccessAdmin: Boolean,
  defaultAddress: DeliveryAddress?,
  addressCount: Int,
  catalogState: CatalogUiState,
  cartUiState: CartAndOrdersUiState,
  openCartTrigger: Int,
  onRetryCatalog: () -> Unit,
  onUpdateSearchQuery: (String) -> Unit,
  onSelectSearchCategory: (String?) -> Unit,
  onToggleSearchOffers: () -> Unit,
  onToggleSearchInStock: () -> Unit,
  onToggleSearchWholesale: () -> Unit,
  onSetSearchPriceRange: (Double?, Double?) -> Unit,
  onSelectSearchSort: (CatalogSortOption) -> Unit,
  onResetSearchFilters: () -> Unit,
  onOpenSearchPreset: (sortOption: CatalogSortOption, onlyOffers: Boolean) -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
  onUpdateCartQuantity: (Product, Int, String) -> Unit,
  onRemoveCartItem: (String) -> Unit,
  onCouponInputChange: (String) -> Unit,
  onApplyCoupon: () -> Unit,
  onRemoveCoupon: () -> Unit,
  onProceedToCheckout: () -> Unit,
  onOpenMyOrders: () -> Unit,
  feedbackMessage: String?,
  errorMessage: String?,
  onClearMessages: () -> Unit,
  onRequireSignIn: () -> Unit,
  onUpdateProfile: (fullName: String, phone: String, companyName: String?) -> Unit,
  onSubmitWholesaleRequest: (fullName: String, phone: String, companyName: String) -> Unit,
  onOpenAddresses: () -> Unit,
  onNavigateToAdmin: () -> Unit,
  onSignOut: () -> Unit,
  unreadNotificationsCount: Int = 0,
  onOpenNotifications: () -> Unit = {},
  openOffersTrigger: Int = 0,
  isWholesalePreviewActive: Boolean = false,
  canToggleWholesalePreview: Boolean = false,
  onToggleWholesalePreview: () -> Unit = {},
  onAddFlashSaleItemToCart: (Product, String) -> Unit = { p, _ -> onAddToCart(p) },
  onOpenPolicies: (StorePolicyTab) -> Unit = {},
  requiresRecentLoginForDeletion: Boolean = false,
  onDeleteAccount: () -> Unit = {},
  onToggleWishlist: (String) -> Unit = {},
  onOpenWishlist: () -> Unit = {},
  onClearWishlistMessage: () -> Unit = {},
  onOpenBrandShowcase: (String) -> Unit = {},
  onLogZeroResultSearch: (String) -> Unit = {},
) {
  var selectedTab by rememberSaveable { mutableStateOf(CustomerTab.HOME) }
  var selectedMainCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
  var showAboutStore by rememberSaveable { mutableStateOf(false) }
  var showQrScanner by remember { mutableStateOf(false) }
  val context = LocalContext.current
  var requestedNotificationPermission by rememberSaveable { mutableStateOf(false) }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { }

  LaunchedEffect(userProfile?.uid) {
    if (
      userProfile != null &&
      !requestedNotificationPermission &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        requestedNotificationPermission = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  if (showAboutStore) {
    AboutStoreScreen(
      settings = catalogState.storeSettings,
      onOpenPolicies = {
        showAboutStore = false
        onOpenPolicies(StorePolicyTab.PRIVACY)
      },
      onBackClick = { showAboutStore = false },
    )
    return
  }

  LaunchedEffect(openCartTrigger) {
    if (openCartTrigger > 0) {
      selectedTab = CustomerTab.CART
    }
  }

  LaunchedEffect(openOffersTrigger) {
    if (openOffersTrigger > 0) {
      onOpenSearchPreset(CatalogSortOption.NEWEST, true)
      selectedTab = CustomerTab.SEARCH
    }
  }

  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showWholesaleDialog by remember { mutableStateOf(false) }
  var showSignOutDialog by remember { mutableStateOf(false) }
  var showDeleteAccountDialog by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val isWholesaleApproved = (userProfile?.isWholesaleApproved == true) ||
    (canToggleWholesalePreview && isWholesalePreviewActive)

  val combinedFeedback = catalogState.wishlistMessage ?: feedbackMessage

  LaunchedEffect(combinedFeedback, errorMessage) {
    val msg = combinedFeedback ?: errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
      onClearWishlistMessage()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isExpandedScreen = maxWidth >= 600.dp

    Scaffold(
      containerColor = if (selectedTab == CustomerTab.CATEGORIES) PureWhite else GeneralBackground,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
      topBar = {
        Column(modifier = Modifier.background(PureWhite)) {
          AlEmanTopBar(
            title = catalogState.storeSettings.storeName.ifBlank { stringResource(id = R.string.app_name) },
            subtitle = catalogState.storeSettings.tagline.ifBlank { stringResource(id = R.string.brand_tagline) },
            trailingContent = {
              val wishlistCount = catalogState.wishlistProductIds.size
              if (userProfile == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = onOpenWishlist,
                    modifier = Modifier.testTag("top_wishlist_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.FavoriteBorder,
                      contentDescription = "المفضلة",
                      tint = CharcoalBlack,
                    )
                  }
                  IconButton(
                    onClick = onRequireSignIn,
                    modifier = Modifier.testTag("top_delivery_address_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.LocationOn,
                      contentDescription = "عناوين التوصيل",
                      tint = RoyalBlue,
                    )
                  }
                  Surface(
                    onClick = onRequireSignIn,
                    shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                    color = RoyalBlueSoft,
                    modifier = Modifier
                      .padding(end = 8.dp)
                      .height(38.dp)
                      .testTag("top_sign_in_button"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = "تسجيل الدخول",
                        tint = RoyalBlue,
                        modifier = Modifier.size(17.dp),
                      )
                      Text(
                        text = "دخول",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue,
                      )
                    }
                  }
                }
              } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = onOpenWishlist,
                    modifier = Modifier.testTag("top_wishlist_button"),
                  ) {
                    if (wishlistCount > 0) {
                      BadgedBox(
                        badge = {
                          Badge(containerColor = DiscountRed, contentColor = PureWhite) {
                            Text(
                              text = if (wishlistCount > 99) "99+" else wishlistCount.toString(),
                              style = MaterialTheme.typography.labelSmall,
                            )
                          }
                        },
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Favorite,
                          contentDescription = "المفضلة ($wishlistCount)",
                          tint = DiscountRed,
                        )
                      }
                    } else {
                      Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = CharcoalBlack,
                      )
                    }
                  }
                  IconButton(
                    onClick = onOpenAddresses,
                    modifier = Modifier.testTag("top_delivery_address_button"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = if (defaultAddress != null) {
                          "عنوان التوصيل: ${defaultAddress.governorate} - ${defaultAddress.city}"
                        } else {
                          "إضافة عنوان التوصيل"
                        },
                        tint = RoyalBlue,
                      )
                      if (defaultAddress != null) {
                        Box(
                          modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .background(SuccessGreen, CircleShape),
                        )
                      }
                    }
                  }
                  IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag("top_notifications_button"),
                  ) {
                    if (unreadNotificationsCount > 0) {
                      BadgedBox(
                        badge = {
                          Badge(containerColor = DiscountRed, contentColor = PureWhite) {
                            Text(
                              text = if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString(),
                              style = MaterialTheme.typography.labelSmall,
                            )
                          }
                        },
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Notifications,
                          contentDescription = "الإشعارات ($unreadNotificationsCount)",
                          tint = CharcoalBlack,
                        )
                      }
                    } else {
                      Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "الإشعارات",
                        tint = CharcoalBlack,
                      )
                    }
                  }
                  if (canAccessAdmin) {
                    IconButton(
                      onClick = onNavigateToAdmin,
                      modifier = Modifier.testTag("open_admin_from_store_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = stringResource(id = R.string.admin_panel_title),
                        tint = RoyalBlue,
                      )
                    }
                  }
                }
              }
            },
          )
          if (selectedTab == CustomerTab.HOME || selectedTab == CustomerTab.CATEGORIES) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AlEmanDimens.SpaceLg, vertical = 5.dp),
            ) {
              StoreSearchTriggerBar(
                placeholder = stringResource(id = R.string.search_placeholder),
                onClick = { selectedTab = CustomerTab.SEARCH },
                onScanQrClick = { showQrScanner = true },
              )
            }
            HorizontalDivider(color = BorderNeutral)
          }
        }
      },
      bottomBar = {
        if (!isExpandedScreen) {
          Column {
            HorizontalDivider(color = BorderNeutral)
            NavigationBar(
              containerColor = PureWhite,
              tonalElevation = 0.dp,
            ) {
              CustomerTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    if (tab == CustomerTab.CATEGORIES) {
                      selectedMainCategoryId = null
                    }
                    selectedTab = tab
                  },
                  modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}"),
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CharcoalBlack,
                    selectedTextColor = CharcoalBlack,
                    indicatorColor = PrimaryYellow,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                  ),
                  icon = {
                    TabIconWithCartBadge(
                      tab = tab,
                      selected = selected,
                      cartCount = cartUiState.cartSummary.totalItemsCount,
                    )
                  },
                  label = { Text(text = tab.label, style = MaterialTheme.typography.labelMedium) },
                )
              }
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (isExpandedScreen) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = PureWhite,
          ) {
            CustomerTab.entries.forEach { tab ->
              val selected = selectedTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = {
                  if (tab == CustomerTab.CATEGORIES) {
                    selectedMainCategoryId = null
                  }
                  selectedTab = tab
                },
                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = CharcoalBlack,
                  selectedTextColor = CharcoalBlack,
                  indicatorColor = PrimaryYellow,
                  unselectedIconColor = TextSecondary,
                  unselectedTextColor = TextSecondary,
                ),
                icon = {
                  TabIconWithCartBadge(
                    tab = tab,
                    selected = selected,
                    cartCount = cartUiState.cartSummary.totalItemsCount,
                  )
                },
                label = { Text(text = tab.label, style = MaterialTheme.typography.labelMedium) },
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Column(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize()
              .verticalScroll(rememberScrollState()),
          ) {
            when (selectedTab) {
              CustomerTab.HOME -> HomeTabContent(
                catalogState = catalogState,
                userProfile = userProfile,
                canAccessAdmin = canAccessAdmin,
                defaultAddress = defaultAddress,
                onDeliveryBarClick = {
                  if (userProfile == null) onRequireSignIn() else onOpenAddresses()
                },
                onOpenCategories = { categoryId ->
                  if (categoryId != null) {
                    selectedMainCategoryId = categoryId
                  }
                  selectedTab = CustomerTab.CATEGORIES
                },
                onOpenSearchPreset = { sortOption, onlyOffers ->
                  onOpenSearchPreset(sortOption, onlyOffers)
                  selectedTab = CustomerTab.SEARCH
                },
                onOpenQrScanner = { showQrScanner = true },
                onOpenProduct = onOpenProduct,
                onAddToCart = onAddToCart,
                onOpenAdmin = onNavigateToAdmin,
                onRetryCatalog = onRetryCatalog,
                isWholesalePreviewActive = isWholesalePreviewActive,
                canToggleWholesalePreview = canToggleWholesalePreview,
                onToggleWholesalePreview = onToggleWholesalePreview,
                onAddFlashSaleItemToCart = onAddFlashSaleItemToCart,
                onToggleWishlist = onToggleWishlist,
                onOpenBrandShowcase = onOpenBrandShowcase,
                onBannerClick = { banner ->
                  when (banner.targetTypeEnum) {
                    BannerTargetType.OFFERS -> {
                      onOpenSearchPreset(CatalogSortOption.NEWEST, true)
                      selectedTab = CustomerTab.SEARCH
                    }
                    BannerTargetType.CATEGORY -> {
                      if (!banner.targetId.isNullOrBlank()) {
                        selectedMainCategoryId = banner.targetId
                      }
                      selectedTab = CustomerTab.CATEGORIES
                    }
                    BannerTargetType.PRODUCT -> {
                      val pid = banner.targetId
                      if (!pid.isNullOrBlank()) {
                        onOpenProduct(pid)
                      }
                    }
                    BannerTargetType.NONE -> Unit
                  }
                },
              )

              CustomerTab.CATEGORIES -> CategoriesTabContent(
                catalogState = catalogState,
                selectedMainCategoryId = selectedMainCategoryId,
                isWholesaleApproved = isWholesaleApproved,
                onSelectMainCategory = { selectedMainCategoryId = it },
                onOpenProduct = onOpenProduct,
                onAddToCart = onAddToCart,
                onExploreHome = { selectedTab = CustomerTab.HOME },
                onToggleWishlist = onToggleWishlist,
              )

              CustomerTab.SEARCH -> SearchTabContent(
                catalogState = catalogState,
                isWholesaleApproved = isWholesaleApproved,
                onQueryChange = onUpdateSearchQuery,
                onOpenQrScanner = { showQrScanner = true },
                onSelectCategory = onSelectSearchCategory,
                onToggleOffers = onToggleSearchOffers,
                onToggleInStock = onToggleSearchInStock,
                onToggleWholesale = onToggleSearchWholesale,
                onSetPriceRange = onSetSearchPriceRange,
                onSelectSort = onSelectSearchSort,
                onResetFilters = onResetSearchFilters,
                onOpenProduct = onOpenProduct,
                onAddToCart = onAddToCart,
                onToggleWishlist = onToggleWishlist,
                onLogZeroResultSearch = onLogZeroResultSearch,
              )

              CustomerTab.CART -> CartTabContent(
                cartUiState = cartUiState,
                isSignedIn = userProfile != null,
                isWholesaleApproved = isWholesaleApproved,
                onShopNow = { selectedTab = CustomerTab.HOME },
                onOpenProduct = onOpenProduct,
                onUpdateQuantity = onUpdateCartQuantity,
                onRemoveItem = onRemoveCartItem,
                onCouponInputChange = onCouponInputChange,
                onApplyCoupon = onApplyCoupon,
                onRemoveCoupon = onRemoveCoupon,
                onRequireSignIn = onRequireSignIn,
                onProceedToCheckout = onProceedToCheckout,
              )

              CustomerTab.ACCOUNT -> {
                if (userProfile == null) {
                  GuestAccountContent(
                    onRequireSignIn = onRequireSignIn,
                    onOpenAboutStore = { showAboutStore = true },
                    onOpenPolicies = { onOpenPolicies(StorePolicyTab.PRIVACY) },
                  )
                } else {
                  AuthenticatedAccountContent(
                    userProfile = userProfile,
                    canAccessAdmin = canAccessAdmin,
                    addressCount = addressCount,
                    ordersCount = cartUiState.orders.size,
                    wishlistCount = catalogState.wishlistProductIds.size,
                    onEditProfile = { showEditProfileDialog = true },
                    onOpenWishlist = onOpenWishlist,
                    onOpenOrders = onOpenMyOrders,
                    onOpenAddresses = onOpenAddresses,
                    onRequestWholesale = { showWholesaleDialog = true },
                    onOpenAboutStore = { showAboutStore = true },
                    onOpenPolicies = { onOpenPolicies(StorePolicyTab.PRIVACY) },
                    onOpenAdmin = onNavigateToAdmin,
                    onSignOutClick = { showSignOutDialog = true },
                    onDeleteAccountClick = { showDeleteAccountDialog = true },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showEditProfileDialog && userProfile != null) {
    EditProfileDialog(
      profile = userProfile,
      onDismiss = { showEditProfileDialog = false },
      onConfirm = { fullName, phone, companyName ->
        showEditProfileDialog = false
        onUpdateProfile(fullName, phone, companyName)
      },
    )
  }

  if (showWholesaleDialog && userProfile != null) {
    WholesaleRequestDialog(
      profile = userProfile,
      onDismiss = { showWholesaleDialog = false },
      onConfirm = { fullName, phone, companyName ->
        showWholesaleDialog = false
        onSubmitWholesaleRequest(fullName, phone, companyName)
      },
    )
  }

  if (showSignOutDialog && userProfile != null) {
    SignOutConfirmDialog(
      onDismiss = { showSignOutDialog = false },
      onConfirm = {
        showSignOutDialog = false
        onSignOut()
      },
    )
  }

  if (showDeleteAccountDialog && userProfile != null) {
    DeleteAccountConfirmDialog(
      userEmail = userProfile.email,
      requiresRecentLogin = requiresRecentLoginForDeletion,
      onDismiss = { showDeleteAccountDialog = false },
      onConfirmDelete = {
        showDeleteAccountDialog = false
        onDeleteAccount()
      },
    )
  }

  if (showQrScanner) {
    QrBarcodeScannerDialog(
      title = "البحث بالباركود أو QR",
      onCodeScanned = { scannedCode ->
        showQrScanner = false
        val exactMatch = catalogState.findExactByScannedCode(scannedCode)
        if (exactMatch != null) {
          onOpenProduct(exactMatch.id)
        } else {
          onResetSearchFilters()
          onUpdateSearchQuery(scannedCode)
          selectedTab = CustomerTab.SEARCH
          coroutineScope.launch {
            snackbarHostState.showSnackbar("تم مسح الكود: $scannedCode")
          }
        }
      },
      onDismiss = { showQrScanner = false },
    )
  }
}

@Composable
private fun TabIconWithCartBadge(
  tab: CustomerTab,
  selected: Boolean,
  cartCount: Int,
) {
  if (tab == CustomerTab.CART && cartCount > 0) {
    BadgedBox(
      badge = {
        Badge(
          containerColor = DiscountRed,
          contentColor = PureWhite,
        ) {
          Text(
            text = if (cartCount > 99) "99+" else cartCount.toString(),
            style = MaterialTheme.typography.labelSmall,
          )
        }
      },
    ) {
      Icon(
        imageVector = tabIcon(tab, selected),
        contentDescription = "${tab.label} ($cartCount)",
      )
    }
  } else {
    Icon(
      imageVector = tabIcon(tab, selected),
      contentDescription = tab.label,
    )
  }
}

@Composable
private fun GuestAccountContent(
  onRequireSignIn: () -> Unit,
  onOpenAboutStore: () -> Unit,
  onOpenPolicies: () -> Unit,
) {
  Column(
    modifier = Modifier.padding(AlEmanDimens.SpaceLg),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    AlEmanCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(id = R.string.account_guest_title),
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = RoyalBlue,
        )
        Text(
          text = stringResource(id = R.string.account_guest_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        AlEmanPrimaryButton(
          text = "تسجيل الدخول أو إنشاء حساب",
          onClick = onRequireSignIn,
          icon = Icons.AutoMirrored.Filled.Login,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("guest_open_sign_in_button"),
        )
      }
    }

    AlEmanCard(modifier = Modifier.fillMaxWidth()) {
      Column {
        AccountActionRow(
          icon = Icons.Filled.Info,
          title = "عن المتجر",
          subtitle = "نبذة عن الشركة، صفحات التواصل، وأرقام القطاعي والجملة",
          onClick = onOpenAboutStore,
          modifier = Modifier.testTag("account_about_store_row"),
        )
        HorizontalDivider(color = BorderNeutral)
        AccountActionRow(
          icon = Icons.Filled.PrivacyTip,
          title = "الخصوصية وسياسة الاسترجاع",
          subtitle = "سياسة حماية البيانات، الاسترجاع والاستبدال، والشروط",
          onClick = onOpenPolicies,
          modifier = Modifier.testTag("account_policies_row"),
        )
      }
    }
  }
}

@Composable
private fun AuthenticatedAccountContent(
  userProfile: UserProfile,
  canAccessAdmin: Boolean,
  addressCount: Int,
  ordersCount: Int,
  wishlistCount: Int,
  onEditProfile: () -> Unit,
  onOpenWishlist: () -> Unit,
  onOpenOrders: () -> Unit,
  onOpenAddresses: () -> Unit,
  onRequestWholesale: () -> Unit,
  onOpenAboutStore: () -> Unit,
  onOpenPolicies: () -> Unit,
  onOpenAdmin: () -> Unit,
  onSignOutClick: () -> Unit,
  onDeleteAccountClick: () -> Unit,
) {
  Column(
    modifier = Modifier.padding(AlEmanDimens.SpaceLg),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    AlEmanCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .background(RoyalBlueSoft, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = userProfile.fullName.trim().firstOrNull()?.toString() ?: "م",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = RoyalBlue,
            )
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = userProfile.fullName.ifBlank { "عميل الإيمان للتجارة" },
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            Text(
              text = userProfile.email,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
            if (userProfile.phone.isNotBlank()) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Phone,
                  contentDescription = null,
                  tint = TextSecondary,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = userProfile.phone,
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              }
            }
          }
        }

        if (!userProfile.wholesaleCompanyName.isNullOrBlank()) {
          Text(
            text = "النشاط التجاري: ${userProfile.wholesaleCompanyName}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          when {
            userProfile.isWholesaleApproved -> SuccessBadge(text = "حساب جملة معتمد")
            userProfile.wholesaleStatusEnum == WholesaleStatus.PENDING -> BrandPillBadge(
              text = "طلب الجملة قيد المراجعة",
              containerColor = GoldenYellowSoft,
              contentColor = TextPrimary,
            )
            userProfile.wholesaleStatusEnum == WholesaleStatus.REJECTED ->
              DiscountBadge(text = "طلب الجملة مرفوض")
            else -> Unit
          }

          if (canAccessAdmin) {
            BrandPillBadge(
              text = userProfile.staffRoleEnum.arabicLabel,
              containerColor = GoldenYellow,
              contentColor = TextPrimary,
            )
          }
        }
      }
    }

    AlEmanCard(modifier = Modifier.fillMaxWidth()) {
      Column {
        AccountActionRow(
          icon = Icons.Filled.Edit,
          title = "تعديل البيانات الشخصية",
          subtitle = "تحديث الاسم ورقم الهاتف وبيانات النشاط",
          onClick = onEditProfile,
          modifier = Modifier.testTag("account_edit_profile_row"),
        )
        HorizontalDivider(color = BorderNeutral)
        AccountActionRow(
          icon = Icons.Filled.Favorite,
          title = "المفضلة",
          subtitle = if (wishlistCount == 0) {
            "المنتجات التي قمت بحفظها للمتابعة والشراء لاحقًا"
          } else {
            "$wishlistCount منتج في المفضلة"
          },
          onClick = onOpenWishlist,
          modifier = Modifier.testTag("account_wishlist_row"),
        )
        HorizontalDivider(color = BorderNeutral)
        AccountActionRow(
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          title = "طلباتي وتتبع الطلبات",
          subtitle = if (ordersCount == 0) {
            "استعراض حالة الطلبات السابقة والحالية"
          } else {
            "$ordersCount طلب مسجل"
          },
          onClick = onOpenOrders,
          modifier = Modifier.testTag("account_orders_row"),
        )
        HorizontalDivider(color = BorderNeutral)
        AccountActionRow(
          icon = Icons.Filled.LocationOn,
          title = "عناوين التوصيل",
          subtitle = if (addressCount == 0) {
            "إضافة وإدارة مواقع الاستلام"
          } else {
            "$addressCount عنوان توصيل محفوظ"
          },
          onClick = onOpenAddresses,
          modifier = Modifier.testTag("account_addresses_row"),
        )
        HorizontalDivider(color = BorderNeutral)
        when (userProfile.wholesaleStatusEnum) {
          WholesaleStatus.NONE -> {
            AccountActionRow(
              icon = Icons.Filled.VerifiedUser,
              title = "طلب اعتماد حساب جملة (B2B)",
              subtitle = "قدّم بيانات محلك أو شركتك للحصول على أسعار الجملة",
              onClick = onRequestWholesale,
              modifier = Modifier.testTag("account_request_wholesale_row"),
            )
          }
          WholesaleStatus.REJECTED -> {
            AccountActionRow(
              icon = Icons.Filled.VerifiedUser,
              title = "إعادة تقديم طلب الجملة",
              subtitle = "تم رفض الطلب السابق — يمكنك تعديل البيانات وإعادة الإرسال",
              onClick = onRequestWholesale,
              modifier = Modifier.testTag("account_reapply_wholesale_row"),
            )
          }
          WholesaleStatus.PENDING -> {
            AccountStatusInfoRow(
              icon = Icons.Filled.VerifiedUser,
              title = "حالة حساب الجملة",
              subtitle = "طلبك قيد المراجعة حاليًا من إدارة المتجر، وستفعل الأسعار فور الاعتماد.",
            )
          }
          WholesaleStatus.APPROVED -> {
            AccountStatusInfoRow(
              icon = Icons.Filled.VerifiedUser,
              title = "حساب الجملة مفعل",
              subtitle = "تظهر لك أسعار الجملة المعتمدة عند عرض المنتجات وإتمام الطلبات.",
            )
          }
        }
        HorizontalDivider(color = BorderNeutral)
        AccountActionRow(
          icon = Icons.Filled.Info,
          title = "عن المتجر",
          subtitle = "نبذة عن الشركة، صفحات التواصل، وأرقام القطاعي والجملة",
          onClick = onOpenAboutStore,
          modifier = Modifier.testTag("account_about_store_row"),
        )
        HorizontalDivider(color = BorderNeutral)
        AccountActionRow(
          icon = Icons.Filled.PrivacyTip,
          title = "الخصوصية وسياسة الاسترجاع",
          subtitle = "سياسة حماية البيانات، الاسترجاع والاستبدال، والشروط",
          onClick = onOpenPolicies,
          modifier = Modifier.testTag("account_policies_row"),
        )
      }
    }

    if (canAccessAdmin) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onOpenAdmin)
          .testTag("account_open_admin_card"),
        shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = RoyalBlueSoft,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.AdminPanelSettings,
                contentDescription = stringResource(id = R.string.admin_panel_title),
                tint = RoyalBlue,
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(id = R.string.admin_panel_title),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = RoyalBlue,
            )
            Text(
              text = "${userProfile.staffRoleEnum.arabicLabel} • ${stringResource(id = R.string.admin_panel_subtitle)}",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = RoyalBlue,
          )
        }
      }
    }

    OutlinedButton(
      onClick = onSignOutClick,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("account_sign_out_button"),
      shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
      border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.45f)),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Logout,
        contentDescription = "تسجيل الخروج",
        tint = DiscountRed,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "تسجيل الخروج",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = DiscountRed,
      )
    }

    TextButton(
      onClick = onDeleteAccountClick,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("account_delete_account_button"),
    ) {
      Icon(
        imageVector = Icons.Filled.DeleteForever,
        contentDescription = "حذف الحساب نهائيًا",
        tint = DiscountRed.copy(alpha = 0.85f),
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "حذف الحساب نهائيًا",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = DiscountRed.copy(alpha = 0.85f),
      )
    }
  }
}

@Composable
private fun AccountActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = RoyalBlue,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
      contentDescription = null,
      tint = TextSecondary,
    )
  }
}

@Composable
private fun AccountStatusInfoRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = RoyalBlue,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
      )
    }
  }
}

private fun tabIcon(tab: CustomerTab, selected: Boolean): ImageVector = when (tab) {
  CustomerTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
  CustomerTab.CATEGORIES -> if (selected) Icons.Filled.Category else Icons.Outlined.Category
  CustomerTab.SEARCH -> if (selected) Icons.Filled.Search else Icons.Outlined.Search
  CustomerTab.CART -> if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart
  CustomerTab.ACCOUNT -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}
