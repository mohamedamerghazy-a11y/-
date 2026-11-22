package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.core.util.ArabicFormatter
import com.example.data.model.Banner
import com.example.data.model.BannerPlacement
import com.example.data.model.BannerStyle
import com.example.data.model.BrandShowcase
import com.example.data.model.CompanyOfferBadgeStyle
import com.example.data.model.DeliveryAddress
import com.example.data.model.FlashSale
import com.example.data.model.FlashSaleStatus
import com.example.data.model.HomeCategoryStyle
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.computeBannerlessShowcaseAlignLeftMap
import com.example.data.model.effectiveDiscountPercentWithShowcases
import com.example.data.model.effectiveRetailPriceWithShowcases
import com.example.ui.components.AlEmanSecondaryButton
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.CategoryQuickTile
import com.example.ui.components.CompanySpecialOfferBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.FullAspectBannerImage
import com.example.ui.components.LocalProductCardStyle
import com.example.ui.components.ProductCardDiscountBadge
import com.example.ui.components.ProductStoreCard
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.productCardBorderStroke
import com.example.ui.components.productCardElevation
import com.example.ui.components.productCardImageClipShape
import com.example.ui.components.productCardOuterShape
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CatalogSortOption
import com.example.ui.viewmodel.CatalogUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeTabContent(
  catalogState: CatalogUiState,
  userProfile: UserProfile?,
  canAccessAdmin: Boolean,
  defaultAddress: DeliveryAddress?,
  onDeliveryBarClick: () -> Unit,
  onOpenCategories: (categoryId: String?) -> Unit,
  onOpenSearchPreset: (sortOption: CatalogSortOption, onlyOffers: Boolean) -> Unit,
  onOpenQrScanner: () -> Unit,
  onOpenProduct: (productId: String) -> Unit,
  onAddToCart: (Product) -> Unit,
  onOpenAdmin: () -> Unit,
  onRetryCatalog: () -> Unit,
  onBannerClick: (Banner) -> Unit = {},
  isWholesalePreviewActive: Boolean = false,
  canToggleWholesalePreview: Boolean = false,
  onToggleWholesalePreview: () -> Unit = {},
  onAddFlashSaleItemToCart: (Product, String) -> Unit = { p, _ -> onAddToCart(p) },
  onToggleWishlist: (String) -> Unit = {},
  onOpenBrandShowcase: (String) -> Unit = {},
) {
  val isWholesaleApproved = (userProfile?.isWholesaleApproved == true) ||
    (canToggleWholesalePreview && isWholesalePreviewActive)
  val settings = catalogState.storeSettings
  var selectedFlashSale by remember { mutableStateOf<FlashSale?>(null) }

  val bannersByPlacement = catalogState.banners.groupBy { it.placementEnum }
  val showcasesByPlacement = catalogState.brandShowcases.groupBy { it.placementEnum }
  val bannerlessAlignLeftMap = remember(catalogState.brandShowcases) {
    computeBannerlessShowcaseAlignLeftMap(catalogState.brandShowcases)
  }

  Column(
    modifier = Modifier.padding(top = 6.dp, bottom = AlEmanDimens.SpaceLg),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // شريط الإعلانات والتنبيهات من إعدادات المتجر
    if (settings.announcementText.isNotBlank()) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = AlEmanDimens.SpaceLg),
        shape = RoundedCornerShape(10.dp),
        color = PrimaryYellowSoft,
        border = BorderStroke(1.dp, PrimaryYellow),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Campaign,
            contentDescription = null,
            tint = CharcoalBlack,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = settings.announcementText,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
        }
      }
    }

    val tickerConfig = settings.announcementTickerConfig

    // 1. الشريط الإعلاني المتحرك والبنرات وعروض الشركات أعلى الرئيسية
    if (catalogState.isStoreSettingsLoaded && tickerConfig.isActive && tickerConfig.placement == BannerPlacement.TOP_HOME) {
      com.example.ui.components.AnnouncementTickerBar(
        config = tickerConfig,
        modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
      )
    }
    HomePlacementBannerSlot(
      banners = bannersByPlacement[BannerPlacement.TOP_HOME].orEmpty(),
      onBannerClick = onBannerClick,
    )
    HomeBrandShowcaseSlot(
      showcases = showcasesByPlacement[BannerPlacement.TOP_HOME].orEmpty(),
      catalogState = catalogState,
      bannerlessAlignLeftMap = bannerlessAlignLeftMap,
      isWholesaleApproved = isWholesaleApproved,
      onOpenBrandShowcase = onOpenBrandShowcase,
      onOpenProduct = onOpenProduct,
      onAddToCart = onAddToCart,
    )

    // البطاقة الترحيبية الرئيسية (لا تظهر إلا بعد تحميل إعدادات المتجر لتفادي أي وميض عند الإخفاء)
    if (catalogState.isStoreSettingsLoaded && settings.showHeroSection) {
      Card(
        modifier = Modifier
          .padding(horizontal = AlEmanDimens.SpaceLg)
          .testTag("home_hero_card"),
        shape = RoundedCornerShape(AlEmanDimens.RadiusLg),
        colors = CardDefaults.cardColors(containerColor = RoyalBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = AlEmanDimens.CardElevation),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val badge = settings.heroBadgeText.ifBlank { stringResource(id = R.string.hero_badge) }
          val heroTitle = settings.heroTitleText.ifBlank { stringResource(id = R.string.hero_title) }
          val heroSub = settings.heroSubtitleText.ifBlank { stringResource(id = R.string.hero_subtitle) }
          val heroCta = settings.heroCtaText.ifBlank { stringResource(id = R.string.hero_cta) }

          BrandPillBadge(
            text = badge,
            containerColor = GoldenYellow,
            contentColor = TextPrimary,
          )
          Text(
            text = heroTitle,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = PureWhite,
          )
          Text(
            text = heroSub,
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite.copy(alpha = 0.88f),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AlEmanSecondaryButton(
              text = heroCta,
              onClick = { onOpenCategories(null) },
              modifier = Modifier.testTag("hero_browse_categories_button"),
            )
            Surface(
              onClick = onOpenQrScanner,
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              color = PureWhite.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.45f)),
              modifier = Modifier
                .height(AlEmanDimens.ButtonHeight)
                .testTag("hero_scan_qr_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.QrCodeScanner,
                  contentDescription = "مسح QR أو باركود",
                  tint = GoldenYellow,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "مسح كود",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = PureWhite,
                )
              }
            }
          }
        }
      }
    }

    // 2. بنرات وعروض الشركات أسفل البطاقة الرئيسية
    if (catalogState.isStoreSettingsLoaded && tickerConfig.isActive && tickerConfig.placement == BannerPlacement.BELOW_HERO) {
      com.example.ui.components.AnnouncementTickerBar(
        config = tickerConfig,
        modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
      )
    }
    HomePlacementBannerSlot(
      banners = bannersByPlacement[BannerPlacement.BELOW_HERO].orEmpty(),
      onBannerClick = onBannerClick,
    )
    HomeBrandShowcaseSlot(
      showcases = showcasesByPlacement[BannerPlacement.BELOW_HERO].orEmpty(),
      catalogState = catalogState,
      bannerlessAlignLeftMap = bannerlessAlignLeftMap,
      isWholesaleApproved = isWholesaleApproved,
      onOpenBrandShowcase = onOpenBrandShowcase,
      onOpenProduct = onOpenProduct,
      onAddToCart = onAddToCart,
    )

    // حالة التحميل أو الخطأ أو الفراغ أو عرض الكتالوج الحقيقي
    when {
      catalogState.isLoading -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(color = RoyalBlue)
            Text(
              text = "جاري تحميل الأقسام والمنتجات...",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
        }
      }

      catalogState.errorMessage != null -> {
        Box(modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg)) {
          StoreEmptyStateCard(
            icon = Icons.Filled.CloudOff,
            title = "تعذر تحميل المتجر حاليًا",
            description = catalogState.errorMessage,
            actionLabel = "إعادة المحاولة",
            onActionClick = onRetryCatalog,
          )
        }
      }

      catalogState.products.isEmpty() && catalogState.categories.isEmpty() -> {
        Box(modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg)) {
          StoreEmptyStateCard(
            icon = Icons.Filled.Storefront,
            title = stringResource(id = R.string.catalog_empty_title),
            description = stringResource(id = R.string.catalog_empty_desc),
            actionLabel = if (canAccessAdmin) stringResource(id = R.string.admin_panel_title) else null,
            onActionClick = if (canAccessAdmin) onOpenAdmin else null,
          )
        }
      }

      else -> {
        if (catalogState.mainCategories.isNotEmpty()) {
          val categoryStyle = settings.homeCategoryStyleEnum
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeaderRow(
              title = stringResource(id = R.string.nav_categories),
              actionLabel = stringResource(id = R.string.section_see_all),
              onActionClick = { onOpenCategories(null) },
              modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
            )

            if (categoryStyle == HomeCategoryStyle.DOUBLE_GRID) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = AlEmanDimens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                catalogState.mainCategories.take(6).chunked(3).forEachIndexed { rIdx, rowCats ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    rowCats.forEachIndexed { cIdx, category ->
                      val count = catalogState.storefrontProducts.count { it.categoryId == category.id }
                      CategoryQuickTile(
                        category = category,
                        productCount = count,
                        style = categoryStyle,
                        fallbackIndex = rIdx * 3 + cIdx,
                        onClick = { onOpenCategories(category.id) },
                        modifier = Modifier.weight(1f),
                      )
                    }
                    repeat(3 - rowCats.size) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            } else {
              LazyRow(
                contentPadding = PaddingValues(horizontal = AlEmanDimens.SpaceLg),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                itemsIndexed(catalogState.mainCategories, key = { _, cat -> cat.id }) { idx, category ->
                  val count = catalogState.storefrontProducts.count { it.categoryId == category.id }
                  CategoryQuickTile(
                    category = category,
                    productCount = count,
                    style = categoryStyle,
                    fallbackIndex = idx,
                    onClick = { onOpenCategories(category.id) },
                  )
                }
              }
            }
          }
        }

        // 3. بنرات وعروض الشركات أسفل الفئات
        if (catalogState.isStoreSettingsLoaded && tickerConfig.isActive && tickerConfig.placement == BannerPlacement.BELOW_CATEGORIES) {
          com.example.ui.components.AnnouncementTickerBar(
            config = tickerConfig,
            modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
          )
        }
        HomePlacementBannerSlot(
          banners = bannersByPlacement[BannerPlacement.BELOW_CATEGORIES].orEmpty(),
          onBannerClick = onBannerClick,
        )
        HomeBrandShowcaseSlot(
          showcases = showcasesByPlacement[BannerPlacement.BELOW_CATEGORIES].orEmpty(),
          catalogState = catalogState,
          bannerlessAlignLeftMap = bannerlessAlignLeftMap,
          isWholesaleApproved = isWholesaleApproved,
          onOpenBrandShowcase = onOpenBrandShowcase,
          onOpenProduct = onOpenProduct,
          onAddToCart = onAddToCart,
        )

        // عروض الفلاش سيل
        if (catalogState.flashSales.isNotEmpty()) {
          HomeFlashSalesSection(
            flashSales = catalogState.flashSales,
            products = catalogState.products,
            onFlashSaleClick = { selectedFlashSale = it },
          )
        }

        if (settings.showFeaturedSection && catalogState.featuredProducts.isNotEmpty()) {
          HomeProductCarousel(
            title = stringResource(id = R.string.section_featured),
            products = catalogState.featuredProducts,
            brandShowcases = catalogState.brandShowcases,
            wishlistProductIds = catalogState.wishlistProductIds,
            showColors = settings.showProductColors,
            showSizes = settings.showProductSizes,
            isWholesaleApproved = isWholesaleApproved,
            onSeeAllClick = { onOpenSearchPreset(CatalogSortOption.NEWEST, false) },
            onProductClick = onOpenProduct,
            onToggleWishlist = onToggleWishlist,
            onAddToCart = onAddToCart,
          )
        }

        // 4. بنرات وعروض الشركات أسفل المنتجات المميزة
        if (catalogState.isStoreSettingsLoaded && tickerConfig.isActive && tickerConfig.placement == BannerPlacement.BELOW_FEATURED) {
          com.example.ui.components.AnnouncementTickerBar(
            config = tickerConfig,
            modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
          )
        }
        HomePlacementBannerSlot(
          banners = bannersByPlacement[BannerPlacement.BELOW_FEATURED].orEmpty(),
          onBannerClick = onBannerClick,
        )
        HomeBrandShowcaseSlot(
          showcases = showcasesByPlacement[BannerPlacement.BELOW_FEATURED].orEmpty(),
          catalogState = catalogState,
          bannerlessAlignLeftMap = bannerlessAlignLeftMap,
          isWholesaleApproved = isWholesaleApproved,
          onOpenBrandShowcase = onOpenBrandShowcase,
          onOpenProduct = onOpenProduct,
          onAddToCart = onAddToCart,
        )

        if (settings.showDealsSection && catalogState.offerProducts.isNotEmpty()) {
          HomeProductCarousel(
            title = stringResource(id = R.string.section_deals),
            products = catalogState.offerProducts,
            brandShowcases = catalogState.brandShowcases,
            wishlistProductIds = catalogState.wishlistProductIds,
            showColors = settings.showProductColors,
            showSizes = settings.showProductSizes,
            isWholesaleApproved = isWholesaleApproved,
            onSeeAllClick = { onOpenSearchPreset(CatalogSortOption.NEWEST, true) },
            onProductClick = onOpenProduct,
            onToggleWishlist = onToggleWishlist,
            onAddToCart = onAddToCart,
          )
        }

        // 5. بنرات وعروض الشركات أسفل العروض والخصومات
        if (catalogState.isStoreSettingsLoaded && tickerConfig.isActive && tickerConfig.placement == BannerPlacement.BELOW_DEALS) {
          com.example.ui.components.AnnouncementTickerBar(
            config = tickerConfig,
            modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
          )
        }
        HomePlacementBannerSlot(
          banners = bannersByPlacement[BannerPlacement.BELOW_DEALS].orEmpty(),
          onBannerClick = onBannerClick,
        )
        HomeBrandShowcaseSlot(
          showcases = showcasesByPlacement[BannerPlacement.BELOW_DEALS].orEmpty(),
          catalogState = catalogState,
          bannerlessAlignLeftMap = bannerlessAlignLeftMap,
          isWholesaleApproved = isWholesaleApproved,
          onOpenBrandShowcase = onOpenBrandShowcase,
          onOpenProduct = onOpenProduct,
          onAddToCart = onAddToCart,
        )

        if (catalogState.newestProducts.isNotEmpty()) {
          HomeProductCarousel(
            title = stringResource(id = R.string.section_newest),
            products = catalogState.newestProducts,
            brandShowcases = catalogState.brandShowcases,
            wishlistProductIds = catalogState.wishlistProductIds,
            showColors = settings.showProductColors,
            showSizes = settings.showProductSizes,
            isWholesaleApproved = isWholesaleApproved,
            onSeeAllClick = { onOpenSearchPreset(CatalogSortOption.NEWEST, false) },
            onProductClick = onOpenProduct,
            onToggleWishlist = onToggleWishlist,
            onAddToCart = onAddToCart,
          )
        }

        // 6. بنرات وعروض الشركات أسفل المنتجات التي وصلت حديثًا
        if (catalogState.isStoreSettingsLoaded && tickerConfig.isActive && tickerConfig.placement == BannerPlacement.BELOW_NEWEST) {
          com.example.ui.components.AnnouncementTickerBar(
            config = tickerConfig,
            modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
          )
        }
        HomePlacementBannerSlot(
          banners = bannersByPlacement[BannerPlacement.BELOW_NEWEST].orEmpty(),
          onBannerClick = onBannerClick,
        )
        HomeBrandShowcaseSlot(
          showcases = showcasesByPlacement[BannerPlacement.BELOW_NEWEST].orEmpty(),
          catalogState = catalogState,
          bannerlessAlignLeftMap = bannerlessAlignLeftMap,
          isWholesaleApproved = isWholesaleApproved,
          onOpenBrandShowcase = onOpenBrandShowcase,
          onOpenProduct = onOpenProduct,
          onAddToCart = onAddToCart,
        )

        if (settings.showBestsellersSection && catalogState.bestsellingProducts.isNotEmpty()) {
          HomeProductCarousel(
            title = stringResource(id = R.string.section_bestsellers),
            products = catalogState.bestsellingProducts,
            brandShowcases = catalogState.brandShowcases,
            wishlistProductIds = catalogState.wishlistProductIds,
            showColors = settings.showProductColors,
            showSizes = settings.showProductSizes,
            isWholesaleApproved = isWholesaleApproved,
            onSeeAllClick = { onOpenSearchPreset(CatalogSortOption.BESTSELLER, false) },
            onProductClick = onOpenProduct,
            onToggleWishlist = onToggleWishlist,
            onAddToCart = onAddToCart,
          )
        }

        // 7. بنرات وعروض الشركات أسفل الأكثر مبيعًا
        if (catalogState.isStoreSettingsLoaded && tickerConfig.isActive && tickerConfig.placement == BannerPlacement.BELOW_BESTSELLERS) {
          com.example.ui.components.AnnouncementTickerBar(
            config = tickerConfig,
            modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
          )
        }
        HomePlacementBannerSlot(
          banners = bannersByPlacement[BannerPlacement.BELOW_BESTSELLERS].orEmpty(),
          onBannerClick = onBannerClick,
        )
        HomeBrandShowcaseSlot(
          showcases = showcasesByPlacement[BannerPlacement.BELOW_BESTSELLERS].orEmpty(),
          catalogState = catalogState,
          bannerlessAlignLeftMap = bannerlessAlignLeftMap,
          isWholesaleApproved = isWholesaleApproved,
          onOpenBrandShowcase = onOpenBrandShowcase,
          onOpenProduct = onOpenProduct,
          onAddToCart = onAddToCart,
        )

        if (catalogState.recentlyViewedProducts.isNotEmpty()) {
          HomeProductCarousel(
            title = "شوهد مؤخرًا",
            products = catalogState.recentlyViewedProducts,
            brandShowcases = catalogState.brandShowcases,
            wishlistProductIds = catalogState.wishlistProductIds,
            showColors = settings.showProductColors,
            showSizes = settings.showProductSizes,
            isWholesaleApproved = isWholesaleApproved,
            onSeeAllClick = null,
            onProductClick = onOpenProduct,
            onToggleWishlist = onToggleWishlist,
            onAddToCart = onAddToCart,
          )
        }

        if (settings.showRecommendedSection && catalogState.recommendedProducts.isNotEmpty()) {
          Column(
            modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            SectionHeaderRow(
              title = "منتجات مقترحة لك",
              actionLabel = stringResource(id = R.string.section_see_all),
              onActionClick = { onOpenSearchPreset(CatalogSortOption.HIGHEST_RATED, false) },
            )
            catalogState.recommendedProducts.chunked(2).forEach { pair ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                pair.forEach { product ->
                  val colorsCount = if (settings.showProductColors) product.availableColors.size else 0
                  val sizesCount = if (settings.showProductSizes) product.availableSizes.size else 0
                  val effectiveRetail = product.effectiveRetailPriceWithShowcases(catalogState.brandShowcases)
                  val effectiveDiscountPct = product.effectiveDiscountPercentWithShowcases(catalogState.brandShowcases)
                  ProductStoreCard(
                    product = product,
                    isWholesaleApproved = isWholesaleApproved,
                    onClick = { onOpenProduct(product.id) },
                    isWishlisted = product.id in catalogState.wishlistProductIds,
                    onToggleWishlist = { onToggleWishlist(product.id) },
                    visibleColorsCount = colorsCount,
                    visibleSizesCount = sizesCount,
                    overrideRetailPrice = effectiveRetail,
                    overrideDiscountPercent = effectiveDiscountPct,
                    onAddToCart = {
                      if (colorsCount > 0 || sizesCount > 0) {
                        onOpenProduct(product.id)
                      } else {
                        onAddToCart(product)
                      }
                    },
                    modifier = Modifier.weight(1f),
                  )
                }
                if (pair.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }

        // 8. بنرات وعروض الشركات أسفل المنتجات المقترحة
        if (catalogState.isStoreSettingsLoaded && tickerConfig.isActive && tickerConfig.placement == BannerPlacement.BELOW_RECOMMENDED) {
          com.example.ui.components.AnnouncementTickerBar(
            config = tickerConfig,
            modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
          )
        }
        HomePlacementBannerSlot(
          banners = bannersByPlacement[BannerPlacement.BELOW_RECOMMENDED].orEmpty(),
          onBannerClick = onBannerClick,
        )
        HomeBrandShowcaseSlot(
          showcases = showcasesByPlacement[BannerPlacement.BELOW_RECOMMENDED].orEmpty(),
          catalogState = catalogState,
          bannerlessAlignLeftMap = bannerlessAlignLeftMap,
          isWholesaleApproved = isWholesaleApproved,
          onOpenBrandShowcase = onOpenBrandShowcase,
          onOpenProduct = onOpenProduct,
          onAddToCart = onAddToCart,
        )
      }
    }
  }

  selectedFlashSale?.let { sale ->
    FlashSaleDetailDialog(
      flashSale = sale,
      products = catalogState.products,
      isWholesaleApproved = isWholesaleApproved,
      onOpenProduct = { pid ->
        selectedFlashSale = null
        onOpenProduct(pid)
      },
      onAddFlashSaleProduct = { product ->
        onAddFlashSaleItemToCart(product, sale.id)
      },
      onDismiss = { selectedFlashSale = null },
    )
  }
}

@Composable
private fun HomePlacementBannerSlot(
  banners: List<Banner>,
  onBannerClick: (Banner) -> Unit,
) {
  if (banners.isEmpty()) return
  HomePromoBannerCarousel(
    banners = banners,
    onBannerClick = onBannerClick,
  )
}

@Composable
private fun HomeBrandShowcaseSlot(
  showcases: List<BrandShowcase>,
  catalogState: CatalogUiState,
  bannerlessAlignLeftMap: Map<String, Boolean>,
  isWholesaleApproved: Boolean,
  onOpenBrandShowcase: (String) -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
) {
  if (showcases.isEmpty()) return
  val offerBadgeStyle = catalogState.storeSettings.companyOfferBadgeStyleEnum
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    showcases.forEach { showcase ->
      val showcaseProducts = catalogState.productsForBrandShowcase(showcase.id)
      if (showcase.hasCustomBanner) {
        HomeBrandShowcaseBlock(
          showcase = showcase,
          products = showcaseProducts,
          offerBadgeStyle = offerBadgeStyle,
          showColors = catalogState.storeSettings.showProductColors,
          showSizes = catalogState.storeSettings.showProductSizes,
          isWholesaleApproved = isWholesaleApproved,
          onOpenShowcase = { onOpenBrandShowcase(showcase.id) },
          onOpenProduct = onOpenProduct,
          onAddToCart = onAddToCart,
        )
      } else {
        val alignLeft = bannerlessAlignLeftMap[showcase.id] == true
        BannerlessCompanyShowcaseSection(
          showcase = showcase,
          products = showcaseProducts,
          alignHeaderLeft = alignLeft,
          offerBadgeStyle = offerBadgeStyle,
          showColors = catalogState.storeSettings.showProductColors,
          showSizes = catalogState.storeSettings.showProductSizes,
          isWholesaleApproved = isWholesaleApproved,
          onOpenShowcase = { onOpenBrandShowcase(showcase.id) },
          onOpenProduct = onOpenProduct,
          onAddToCart = onAddToCart,
        )
      }
    }
  }
}

@Composable
private fun BannerlessCompanyShowcaseSection(
  showcase: BrandShowcase,
  products: List<Product>,
  alignHeaderLeft: Boolean,
  offerBadgeStyle: CompanyOfferBadgeStyle,
  showColors: Boolean,
  showSizes: Boolean,
  isWholesaleApproved: Boolean,
  onOpenShowcase: () -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
) {
  val backdropColor = remember(showcase.accentColorHex) {
    parseAccentColorOrDefault(showcase.accentColorHex, Color(0xFFFCE4EC))
  }
  val discountPct = showcase.cleanDiscountPercent

  Surface(
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, CharcoalBlack.copy(alpha = 0.08f)),
    shadowElevation = 2.dp,
    color = PureWhite,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = AlEmanDimens.SpaceMd)
      .clip(RoundedCornerShape(22.dp))
      .clickable(onClick = onOpenShowcase)
      .testTag("home_brand_showcase_${showcase.id}"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              backdropColor.copy(alpha = 0.92f),
              backdropColor.copy(alpha = 0.45f),
              PureWhite,
            ),
          ),
        ),
    ) {
      // زخرفة هندسية خفيفة في الخلفية تمنح عمقًا بصريًا أنيقًا
      Canvas(modifier = Modifier.matchParentSize()) {
        drawCircle(
          color = Color.White.copy(alpha = 0.34f),
          radius = size.minDimension * 0.42f,
          center = if (alignHeaderLeft) {
            Offset(size.width * 0.14f, size.height * 0.16f)
          } else {
            Offset(size.width * 0.86f, size.height * 0.16f)
          },
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.20f),
          radius = size.minDimension * 0.28f,
          center = if (alignHeaderLeft) {
            Offset(size.width * 0.88f, size.height * 0.68f)
          } else {
            Offset(size.width * 0.12f, size.height * 0.68f)
          },
        )
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // رأس الشركة المتبادل: الشركة الأولى يمين (Start في RTL)، الثانية يسار (End في RTL)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag(
              if (alignHeaderLeft) "brand_header_left_${showcase.id}" else "brand_header_right_${showcase.id}",
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          if (!alignHeaderLeft) {
            // الشعار والاسم على اليمين + شارة الخصم على اليسار
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              CompanyLogoSquircle(
                imageUrl = showcase.companyImageUrl,
                title = showcase.title,
              )
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = showcase.title,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                  ),
                  color = CharcoalBlack,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val desc = showcase.subtitle?.takeIf { it.isNotBlank() }
                  ?: "${ArabicFormatter.formatNumber(products.size)} منتج متاح"
                Text(
                  text = desc,
                  style = MaterialTheme.typography.labelSmall,
                  color = CharcoalBlack.copy(alpha = 0.72f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (discountPct > 0) {
              Spacer(modifier = Modifier.width(8.dp))
              CompanySpecialOfferBadge(
                discountPercent = discountPct,
                style = offerBadgeStyle,
              )
            }
          } else {
            // شارة الخصم على اليمين + الاسم والشعار على اليسار
            if (discountPct > 0) {
              CompanySpecialOfferBadge(
                discountPercent = discountPct,
                style = offerBadgeStyle,
              )
              Spacer(modifier = Modifier.width(8.dp))
            } else {
              Spacer(modifier = Modifier.weight(0.25f))
            }
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.End,
            ) {
              Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text(
                  text = showcase.title,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                  ),
                  color = CharcoalBlack,
                  textAlign = TextAlign.End,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val desc = showcase.subtitle?.takeIf { it.isNotBlank() }
                  ?: "${ArabicFormatter.formatNumber(products.size)} منتج متاح"
                Text(
                  text = desc,
                  style = MaterialTheme.typography.labelSmall,
                  color = CharcoalBlack.copy(alpha = 0.72f),
                  textAlign = TextAlign.End,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              CompanyLogoSquircle(
                imageUrl = showcase.companyImageUrl,
                title = showcase.title,
              )
            }
          }
        }

        // صف المنتجات الأفقي القابل للتمرير يمين ويسار
        if (products.isNotEmpty()) {
          LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(products, key = { it.id }) { product ->
              val colorsCount = if (showColors) product.availableColors.size else 0
              val sizesCount = if (showSizes) product.availableSizes.size else 0
              CompactBrandShowcaseCard(
                product = product,
                showcaseDiscountPercent = discountPct,
                isWholesaleApproved = isWholesaleApproved,
                availableBandHeight = 205.dp,
                onOpen = { onOpenProduct(product.id) },
                onQuickAdd = {
                  if (colorsCount > 0 || sizesCount > 0) {
                    onOpenProduct(product.id)
                  } else {
                    onAddToCart(product)
                  }
                },
              )
            }
          }
        }

        // زر عرض الكل السفلي الأنيق
        Surface(
          onClick = onOpenShowcase,
          shape = RoundedCornerShape(14.dp),
          color = PureWhite,
          border = BorderStroke(1.dp, CharcoalBlack.copy(alpha = 0.12f)),
          shadowElevation = 1.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(42.dp)
            .testTag("home_brand_see_all_${showcase.id}"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "عرض الكل",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              ),
              color = CharcoalBlack,
              maxLines = 1,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
              contentDescription = "عرض كل منتجات ${showcase.title}",
              tint = CharcoalBlack,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CompanyLogoSquircle(
  imageUrl: String?,
  title: String,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.9f)),
    shadowElevation = 2.dp,
    modifier = Modifier.size(48.dp),
  ) {
    if (!imageUrl.isNullOrBlank()) {
      SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = title,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxSize()
          .padding(4.dp),
        error = {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
              text = title.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = CharcoalBlack,
            )
          }
        },
      )
    } else {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
          text = title.take(1).uppercase(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
          color = CharcoalBlack,
        )
      }
    }
  }
}

@Composable
private fun HomeBrandShowcaseBlock(
  showcase: BrandShowcase,
  products: List<Product>,
  offerBadgeStyle: CompanyOfferBadgeStyle,
  showColors: Boolean,
  showSizes: Boolean,
  isWholesaleApproved: Boolean,
  onOpenShowcase: () -> Unit,
  onOpenProduct: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
) {
  val backdropColor = remember(showcase.accentColorHex) {
    parseAccentColorOrDefault(showcase.accentColorHex, Color(0xFFFCE4EC))
  }
  val discountPct = showcase.cleanDiscountPercent

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = AlEmanDimens.SpaceMd)
      .clip(RoundedCornerShape(20.dp))
      .background(backdropColor)
      .clickable(onClick = onOpenShowcase)
      .testTag("home_brand_showcase_${showcase.id}"),
  ) {
    // 1. البنر الإعلاني للشركة يظهر كاملًا كما قصّه المستخدم دون قص إضافي أو تشويه
    FullAspectBannerImage(
      imageUrl = showcase.bannerImageUrl,
      contentDescription = showcase.title,
      placeholderMinHeight = 360.dp,
      placeholderBackground = backdropColor,
      errorContent = {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(backdropColor),
        )
      },
    )

    if (discountPct > 0 && !isWholesaleApproved) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(10.dp),
      ) {
        CompanySpecialOfferBadge(
          discountPercent = discountPct,
          style = offerBadgeStyle,
        )
      }
    }

    // 2. تقسيم التراكب فوق البنر: أول 25% فارغة لاسم الشركة بالبنر، 65% للمنتجات، وآخر 10% لزر عرض الكل
    Column(
      modifier = Modifier.matchParentSize(),
    ) {
      Spacer(
        modifier = Modifier
          .weight(0.25f)
          .fillMaxWidth(),
      )

      BoxWithConstraints(
        modifier = Modifier
          .weight(0.65f)
          .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
      ) {
        val middleAvailHeight = maxHeight
        if (products.isNotEmpty()) {
          LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
          ) {
            items(products, key = { it.id }) { product ->
              val colorsCount = if (showColors) product.availableColors.size else 0
              val sizesCount = if (showSizes) product.availableSizes.size else 0
              CompactBrandShowcaseCard(
                product = product,
                showcaseDiscountPercent = discountPct,
                isWholesaleApproved = isWholesaleApproved,
                availableBandHeight = middleAvailHeight,
                onOpen = { onOpenProduct(product.id) },
                onQuickAdd = {
                  if (colorsCount > 0 || sizesCount > 0) {
                    onOpenProduct(product.id)
                  } else {
                    onAddToCart(product)
                  }
                },
              )
            }
          }
        }
      }

      Box(
        modifier = Modifier
          .weight(0.10f)
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          onClick = onOpenShowcase,
          shape = RoundedCornerShape(12.dp),
          color = PureWhite.copy(alpha = 0.96f),
          shadowElevation = 2.dp,
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .testTag("home_brand_see_all_${showcase.id}"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "عرض الكل",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              ),
              color = CharcoalBlack,
              maxLines = 1,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
              contentDescription = "عرض كل منتجات ${showcase.title}",
              tint = CharcoalBlack,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CompactBrandShowcaseCard(
  product: Product,
  showcaseDiscountPercent: Int = 0,
  isWholesaleApproved: Boolean,
  availableBandHeight: Dp,
  onOpen: () -> Unit,
  onQuickAdd: () -> Unit,
) {
  val retailWithShowcase = remember(product, showcaseDiscountPercent) {
    val baseRetail = product.effectiveRetailPrice
    if (showcaseDiscountPercent > 0 && product.retailPrice > 0.0) {
      val discounted = (((product.retailPrice * (1.0 - showcaseDiscountPercent / 100.0)) * 100.0).roundToInt() / 100.0)
        .coerceAtLeast(0.01)
      minOf(baseRetail, discounted)
    } else {
      baseRetail
    }
  }
  val displayDiscountPct = remember(product, retailWithShowcase) {
    if (product.retailPrice > 0.0 && retailWithShowcase < product.retailPrice) {
      (((product.retailPrice - retailWithShowcase) / product.retailPrice) * 100.0).roundToInt().coerceIn(1, 99)
    } else {
      0
    }
  }
  val effectivePrice = if (isWholesaleApproved && product.wholesalePrice > 0.0) {
    product.wholesalePrice
  } else {
    retailWithShowcase
  }
  val cardStyle = LocalProductCardStyle.current
  val outerShape = productCardOuterShape(cardStyle.shape, compact = true)
  val innerImageShape = productCardImageClipShape(cardStyle.shape, compact = true)
  val borderStroke = productCardBorderStroke(cardStyle, compact = true)
  val elevation = productCardElevation(cardStyle, compact = true)
  val tightLayout = availableBandHeight < 190.dp
  val cardWidth = if (tightLayout) 118.dp else 136.dp
  val cardPadding = if (tightLayout) 6.dp else 8.dp
  val imageBoxHeight = (availableBandHeight * 0.44f).coerceIn(52.dp, 102.dp)

  Surface(
    onClick = onOpen,
    shape = outerShape,
    color = PureWhite,
    shadowElevation = elevation,
    border = borderStroke,
    modifier = Modifier
      .width(cardWidth)
      .testTag("home_brand_compact_card_${product.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(cardPadding),
      verticalArrangement = Arrangement.spacedBy(if (tightLayout) 3.dp else 5.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(imageBoxHeight)
          .clip(innerImageShape)
          .background(Color(0xFFF6F7FA)),
      ) {
        ProductThumbnail(
          imageUrl = product.imageUrls.firstOrNull(),
          contentDescription = product.name,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        )

        if (displayDiscountPct > 0 && !isWholesaleApproved) {
          ProductCardDiscountBadge(
            discountPercent = displayDiscountPct,
            style = cardStyle.discountBadgeStyle,
            compact = true,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(3.dp),
          )
        }

        if (!product.isOutOfStock) {
          Surface(
            onClick = onQuickAdd,
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, BorderNeutral),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(3.dp)
              .size(if (tightLayout) 26.dp else 29.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "إضافة للسلة",
                tint = Color(0xFFB71C1C),
                modifier = Modifier.size(if (tightLayout) 16.dp else 18.dp),
              )
            }
          }
        }
      }

      Text(
        text = product.name,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.SemiBold,
          lineHeight = if (tightLayout) 14.sp else 16.sp,
          fontSize = if (tightLayout) 11.sp else 12.sp,
        ),
        color = TextPrimary,
        maxLines = if (tightLayout) 1 else 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.height(if (tightLayout) 16.dp else 32.dp),
      )

      if (!tightLayout || availableBandHeight >= 165.dp) {
        Text(
          text = product.unit,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = TextSecondary,
          maxLines = 1,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = ArabicFormatter.formatCurrency(effectivePrice),
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (tightLayout) 11.sp else 13.sp,
          ),
          color = CharcoalBlack,
          maxLines = 1,
        )
        if (displayDiscountPct > 0 && !isWholesaleApproved) {
          Text(
            text = ArabicFormatter.formatNumber(product.retailPrice.toInt()),
            style = MaterialTheme.typography.labelSmall.copy(
              textDecoration = TextDecoration.LineThrough,
              fontSize = 10.sp,
            ),
            color = TextSecondary,
            maxLines = 1,
          )
        }
      }
    }
  }
}

private fun parseAccentColorOrDefault(hex: String?, fallback: Color): Color {
  val clean = hex?.trim().orEmpty()
  if (!clean.startsWith("#")) return fallback
  return try {
    Color(android.graphics.Color.parseColor(clean))
  } catch (_: Exception) {
    fallback
  }
}

@Composable
private fun HomePromoBannerCarousel(
  banners: List<Banner>,
  onBannerClick: (Banner) -> Unit,
) {
  key(banners.map { it.id }) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    // التبديل التلقائي بين البنرات بعد انتهاء الحركة والمدة المحددة لكل بنر بالثواني
    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress, banners.size) {
      if (banners.size > 1 && !pagerState.isScrollInProgress) {
        val currentIdx = pagerState.settledPage.coerceIn(0, banners.lastIndex)
        val waitMillis = banners[currentIdx].safeDurationSeconds * 1000L
        delay(waitMillis)
        if (!pagerState.isScrollInProgress) {
          val nextPage = (currentIdx + 1) % banners.size
          pagerState.animateScrollToPage(nextPage)
        }
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fill,
        contentPadding = PaddingValues(0.dp),
        pageSpacing = 0.dp,
        modifier = Modifier.fillMaxWidth(),
      ) { page ->
        val banner = banners[page]
        val isSlim = banner.bannerStyleEnum == BannerStyle.SLIM
        val hasOverlayText = banner.title.isNotBlank() || !banner.subtitle.isNullOrBlank()

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AlEmanDimens.SpaceLg),
        ) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(if (isSlim) AlEmanDimens.RadiusMd else AlEmanDimens.RadiusLg))
              .clickable { onBannerClick(banner) }
              .testTag("home_promo_banner_${banner.id}"),
            shape = RoundedCornerShape(if (isSlim) AlEmanDimens.RadiusMd else AlEmanDimens.RadiusLg),
            border = BorderStroke(1.dp, BorderNeutral),
            elevation = CardDefaults.cardElevation(defaultElevation = AlEmanDimens.CardElevation),
          ) {
            Box(modifier = Modifier.fillMaxWidth()) {
              when {
                banner.isVideoBanner -> {
                  com.example.ui.components.InlinePromoVideoBanner(
                    videoUrl = banner.videoUrl,
                    posterUrl = banner.effectivePosterUrl,
                    isSlim = isSlim,
                    isActivePage = pagerState.settledPage == page,
                    forceMuted = banner.videoMuted,
                  )
                }
                banner.isAnimatedImageBanner -> {
                  com.example.ui.components.InlineAnimatedPromoBanner(
                    imageUrl = banner.imageUrl,
                    contentDescription = banner.title.ifBlank { "بنر صورة متحركة" },
                    isSlim = isSlim,
                  )
                }
                else -> {
                  FullAspectBannerImage(
                    imageUrl = banner.imageUrl,
                    contentDescription = banner.title.ifBlank { "بنر إعلاني" },
                    placeholderMinHeight = if (isSlim) 84.dp else 155.dp,
                  )
                }
              }
              if (hasOverlayText) {
                Box(
                  modifier = Modifier
                    .matchParentSize()
                    .background(
                      Brush.verticalGradient(
                        colors = listOf(
                          Color.Transparent,
                          CharcoalBlack.copy(alpha = 0.78f),
                        ),
                      ),
                    ),
                )
                Column(
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (isSlim) 10.dp else 14.dp),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  if (banner.title.isNotBlank()) {
                    Text(
                      text = banner.title,
                      style = if (isSlim) {
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                      } else {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                      },
                      color = PrimaryYellow,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  if (!banner.subtitle.isNullOrBlank()) {
                    Text(
                      text = banner.subtitle,
                      style = MaterialTheme.typography.bodySmall,
                      color = PureWhite,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }

      if (banners.size > 1) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(banners.size) { idx ->
            val active = pagerState.currentPage == idx
            Box(
              modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(width = if (active) 18.dp else 7.dp, height = 7.dp)
                .clip(CircleShape)
                .background(if (active) CharcoalBlack else BorderNeutral),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HomeFlashSalesSection(
  flashSales: List<FlashSale>,
  products: List<Product>,
  onFlashSaleClick: (FlashSale) -> Unit,
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

  LaunchedEffect(flashSales) {
    while (flashSales.any { it.statusAt(nowMillis) != FlashSaleStatus.ENDED }) {
      delay(1000L)
      nowMillis = System.currentTimeMillis()
    }
  }

  val productById = remember(products) { products.associateBy { it.id } }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = AlEmanDimens.SpaceLg)
      .clip(RoundedCornerShape(18.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            CharcoalBlack.copy(alpha = 0.05f),
            PrimaryYellowSoft.copy(alpha = 0.65f),
            Color.Transparent,
          ),
        ),
      )
      .padding(vertical = 14.dp)
      .testTag("home_flash_sales_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = CharcoalBlack,
        border = BorderStroke(1.5.dp, GoldenYellow),
        shadowElevation = 2.dp,
      ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "FLASH",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
              ),
              color = PureWhite,
              maxLines = 1,
              softWrap = false,
            )
            Surface(
              shape = CircleShape,
              color = PrimaryYellow,
              modifier = Modifier.size(26.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.Bolt,
                  contentDescription = null,
                  tint = CharcoalBlack,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Text(
              text = "SALE",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
              ),
              color = PrimaryYellow,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = RoyalBlueSoft,
        ) {
          Text(
            text = "الإيمان للتجارة",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = RoyalBlue,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
        Text(
          text = "عروض البرق الخاطفة • لفترة محدودة",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
          maxLines = 1,
        )
      }
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(flashSales, key = { it.id }) { sale ->
        val status = sale.statusAt(nowMillis)
        val remainingMillis = sale.remainingMillisAt(nowMillis)
        val firstProduct = sale.items.firstOrNull()?.let { productById[it.productId] }

        FlashSaleCarouselCard(
          flashSale = sale,
          status = status,
          remainingMillis = remainingMillis,
          coverImageUrl = firstProduct?.imageUrls?.firstOrNull(),
          onClick = { onFlashSaleClick(sale) },
        )
      }
    }
  }
}

@Composable
private fun FlashSaleCarouselCard(
  flashSale: FlashSale,
  status: FlashSaleStatus,
  remainingMillis: Long,
  coverImageUrl: String?,
  onClick: () -> Unit,
) {
  val startTimeLabel = remember(flashSale.startsAt) {
    flashSale.startsAt?.toDate()?.let { date ->
      val ar = Locale.Builder().setLanguage("ar").setRegion("EG").build()
      SimpleDateFormat("dd MMM • hh:mm a", ar).format(date)
    }.orEmpty()
  }
  val countdownText = remember(remainingMillis) { formatCountdown(remainingMillis) }

  Card(
    modifier = Modifier
      .width(166.dp)
      .height(206.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("flash_sale_card_${flashSale.id}"),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(
      1.dp,
      when (status) {
        FlashSaleStatus.ACTIVE -> PrimaryYellow
        FlashSaleStatus.UPCOMING -> GoldenYellow.copy(alpha = 0.65f)
        FlashSaleStatus.ENDED -> BorderNeutral
      },
    ),
    colors = CardDefaults.cardColors(
      containerColor = if (status == FlashSaleStatus.ACTIVE) PrimaryYellowSoft else CharcoalBlack,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (!coverImageUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
          model = coverImageUrl,
          contentDescription = flashSale.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.linearGradient(
                listOf(PrimaryYellow, GoldenYellow),
              ),
            ),
        )
      }

      when (status) {
        FlashSaleStatus.ACTIVE -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    PrimaryYellow.copy(alpha = 0.25f),
                    CharcoalBlack.copy(alpha = 0.72f),
                  ),
                ),
              ),
          )
          Surface(
            shape = CircleShape,
            color = DiscountRed,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = PrimaryYellow,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = flashSale.badgeText.ifBlank { "مفتوح الآن" },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite,
                maxLines = 1,
              )
            }
          }

          Column(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = flashSale.title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = PrimaryYellow,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${flashSale.items.size} منتجات بخصم فوري",
              style = MaterialTheme.typography.labelSmall,
              color = PureWhite,
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DiscountRed,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "ينتهي خلال $countdownText",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp),
              )
            }
          }
        }

        FlashSaleStatus.UPCOMING -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(CharcoalBlack.copy(alpha = 0.78f)),
          )
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            if (startTimeLabel.isNotBlank()) {
              Text(
                text = startTimeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            } else {
              Spacer(modifier = Modifier.height(4.dp))
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = PrimaryYellow.copy(alpha = 0.16f),
                border = BorderStroke(1.5.dp, GoldenYellow),
                modifier = Modifier.size(54.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "يفتح قريبًا",
                    tint = GoldenYellow,
                    modifier = Modifier.size(28.dp),
                  )
                }
              }
              Text(
                text = flashSale.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = PureWhite,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF4A0E0E),
              border = BorderStroke(1.dp, GoldenYellow.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "يفتح بعد $countdownText",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PrimaryYellow,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
              )
            }
          }
        }

        FlashSaleStatus.ENDED -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(CharcoalBlack.copy(alpha = 0.84f)),
          )
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = flashSale.title,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = PureWhite.copy(alpha = 0.85f),
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = DiscountRedSoft,
              border = BorderStroke(1.dp, DiscountRed),
            ) {
              Text(
                text = "تم انتهاء العرض",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = DiscountRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              )
            }
            Text(
              text = "انتهت مدة هذا الفلاش سيل",
              style = MaterialTheme.typography.labelSmall,
              color = PureWhite.copy(alpha = 0.72f),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FlashSaleDetailDialog(
  flashSale: FlashSale,
  products: List<Product>,
  isWholesaleApproved: Boolean,
  onOpenProduct: (String) -> Unit,
  onAddFlashSaleProduct: (Product) -> Unit,
  onDismiss: () -> Unit,
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(flashSale.id) {
    while (flashSale.statusAt(nowMillis) != FlashSaleStatus.ENDED) {
      delay(1000L)
      nowMillis = System.currentTimeMillis()
    }
  }

  val status = flashSale.statusAt(nowMillis)
  val remainingText = formatCountdown(flashSale.remainingMillisAt(nowMillis))
  val productById = remember(products) { products.associateBy { it.id } }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("flash_sale_detail_dialog"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
      ) {
        Surface(color = CharcoalBlack) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Bolt,
                  contentDescription = null,
                  tint = PrimaryYellow,
                )
                Text(
                  text = flashSale.title,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = PrimaryYellow,
                )
              }
              IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = PureWhite)
              }
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = when (status) {
                FlashSaleStatus.ACTIVE -> DiscountRed
                FlashSaleStatus.UPCOMING -> RoyalBlue
                FlashSaleStatus.ENDED -> Color(0xFF444444)
              },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = when (status) {
                    FlashSaleStatus.ACTIVE -> Icons.Filled.Bolt
                    FlashSaleStatus.UPCOMING -> Icons.Filled.Lock
                    FlashSaleStatus.ENDED -> Icons.Filled.Schedule
                  },
                  contentDescription = null,
                  tint = PrimaryYellow,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = when (status) {
                    FlashSaleStatus.ACTIVE -> "العرض سارٍ — ينتهي خلال $remainingText"
                    FlashSaleStatus.UPCOMING -> "مغلق حاليًا — يفتح بعد $remainingText"
                    FlashSaleStatus.ENDED -> "تم انتهاء العرض"
                  },
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = PureWhite,
                )
              }
            }
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          when (status) {
            FlashSaleStatus.UPCOMING -> {
              val ar = remember { Locale.Builder().setLanguage("ar").setRegion("EG").build() }
              val startFormatted = remember(flashSale.startsAt) {
                flashSale.startsAt?.toDate()?.let { date ->
                  SimpleDateFormat("dd MMM yyyy • hh:mm a", ar).format(date)
                }.orEmpty()
              }
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = PrimaryYellowSoft,
                border = BorderStroke(1.dp, GoldenYellow),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("flash_sale_upcoming_locked_notice"),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Surface(
                    shape = CircleShape,
                    color = CharcoalBlack,
                    border = BorderStroke(2.dp, PrimaryYellow),
                    modifier = Modifier.size(64.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "عرض مغلق سيفتح قريبًا",
                        tint = PrimaryYellow,
                        modifier = Modifier.size(32.dp),
                      )
                    }
                  }
                  Text(
                    text = "هذا العرض سيفتح قريبًا!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                    textAlign = TextAlign.Center,
                  )
                  Text(
                    text = "عروض ومنتجات «${flashSale.title}» مخفية ومغلقة حتى يحين موعد الافتتاح المحدد. ترقّب فتح العرض فور انتهاء العداد!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                  )
                  if (startFormatted.isNotBlank()) {
                    BrandPillBadge(
                      text = "موعد الفتح: $startFormatted",
                      containerColor = RoyalBlueSoft,
                      contentColor = RoyalBlue,
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CharcoalBlack,
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text(
                      text = "يفتح العرض بعد $remainingText",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = PrimaryYellow,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    )
                  }
                  Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = PrimaryYellow,
                      contentColor = CharcoalBlack,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text("حسنًا، سأعود عند الفتح", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            FlashSaleStatus.ENDED, FlashSaleStatus.ACTIVE -> {
              if (status == FlashSaleStatus.ENDED) {
                DiscountBadge(text = "تم انتهاء العرض — لم تعد أسعار الفلاش سيل متاحة")
              }

              flashSale.items.forEach { item ->
                val product = productById[item.productId]
                if (product != null) {
                  val flashPrice = flashSale.flashUnitPrice(product.id, isWholesaleApproved) ?: product.retailPrice
                  val regularPrice = product.effectiveUnitPrice(isWholesaleApproved, 1)
                  val savedPct = if (regularPrice > flashPrice && regularPrice > 0.0) {
                    (((regularPrice - flashPrice) / regularPrice) * 100.0).toInt().coerceAtLeast(1)
                  } else {
                    0
                  }

                  Card(
                    shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, BorderNeutral),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      ProductThumbnail(
                        imageUrl = product.imageUrls.firstOrNull(),
                        contentDescription = product.name,
                        modifier = Modifier
                          .size(68.dp)
                          .clip(RoundedCornerShape(8.dp))
                          .clickable { onOpenProduct(product.id) },
                      )
                      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                          text = product.name,
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                          color = TextPrimary,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          Text(
                            text = ArabicFormatter.formatCurrency(flashPrice),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DiscountRed,
                          )
                          if (regularPrice > flashPrice) {
                            Text(
                              text = ArabicFormatter.formatCurrency(regularPrice),
                              style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                              color = TextSecondary,
                            )
                          }
                          if (savedPct > 0) {
                            DiscountBadge(text = "-${ArabicFormatter.formatNumber(savedPct)}%")
                          }
                        }
                        if (item.maxQtyPerOrder != null && item.maxQtyPerOrder > 0) {
                          Text(
                            text = "الحد الأقصى للطلب: ${ArabicFormatter.formatNumber(item.maxQtyPerOrder)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                          )
                        }
                      }

                      Button(
                        onClick = { onAddFlashSaleProduct(product) },
                        enabled = status == FlashSaleStatus.ACTIVE && !product.isOutOfStock,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                          containerColor = if (status == FlashSaleStatus.ACTIVE) PrimaryYellow else BorderNeutral,
                          contentColor = CharcoalBlack,
                        ),
                        modifier = Modifier.testTag("flash_sale_add_${product.id}"),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.AddShoppingCart,
                          contentDescription = "إضافة للفلاش سيل",
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun formatCountdown(millis: Long): String {
  val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
  val days = totalSeconds / 86400L
  val hours = (totalSeconds % 86400L) / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (days > 0) {
    String.format(Locale.US, "%dd %02d:%02d:%02d", days, hours, minutes, seconds)
  } else {
    String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
  }
}

@Composable
private fun HomeProductCarousel(
  title: String,
  products: List<Product>,
  brandShowcases: List<BrandShowcase>,
  wishlistProductIds: Set<String>,
  showColors: Boolean,
  showSizes: Boolean,
  isWholesaleApproved: Boolean,
  onSeeAllClick: (() -> Unit)?,
  onProductClick: (String) -> Unit,
  onToggleWishlist: (String) -> Unit,
  onAddToCart: (Product) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    SectionHeaderRow(
      title = title,
      actionLabel = if (onSeeAllClick != null) stringResource(id = R.string.section_see_all) else null,
      onActionClick = onSeeAllClick,
      modifier = Modifier.padding(horizontal = AlEmanDimens.SpaceLg),
    )
    LazyRow(
      contentPadding = PaddingValues(horizontal = AlEmanDimens.SpaceLg),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(products, key = { it.id }) { product ->
        val colorsCount = if (showColors) product.availableColors.size else 0
        val sizesCount = if (showSizes) product.availableSizes.size else 0
        val effectiveRetail = product.effectiveRetailPriceWithShowcases(brandShowcases)
        val effectiveDiscountPct = product.effectiveDiscountPercentWithShowcases(brandShowcases)
        ProductStoreCard(
          product = product,
          isWholesaleApproved = isWholesaleApproved,
          onClick = { onProductClick(product.id) },
          isWishlisted = product.id in wishlistProductIds,
          onToggleWishlist = { onToggleWishlist(product.id) },
          visibleColorsCount = colorsCount,
          visibleSizesCount = sizesCount,
          overrideRetailPrice = effectiveRetail,
          overrideDiscountPercent = effectiveDiscountPct,
          onAddToCart = {
            if (colorsCount > 0 || sizesCount > 0) {
              onProductClick(product.id)
            } else {
              onAddToCart(product)
            }
          },
          modifier = Modifier.width(178.dp),
        )
      }
    }
  }
}
