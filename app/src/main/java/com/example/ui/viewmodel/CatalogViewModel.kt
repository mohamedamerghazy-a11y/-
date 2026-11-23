package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.toArabicErrorMessage
import com.example.data.local.RecentlyViewedStore
import com.example.data.model.Banner
import com.example.data.model.BrandShowcase
import com.example.data.model.BrandShowcaseCategory
import com.example.data.model.Category
import com.example.data.model.FlashSale
import com.example.data.model.Product
import com.example.data.model.StoreCompany
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.effectiveDiscountPercentWithShowcases
import com.example.data.model.effectiveRetailPriceWithShowcases
import com.example.data.model.isExclusiveHiddenFromStore
import com.example.data.model.matchesBrandShowcase
import com.example.data.model.normalizeSearchQuery
import com.example.data.repository.BannerAndStoreRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.EngagementRepository
import com.example.data.repository.FlashSaleRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.WishlistRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CatalogSortOption(val arabicLabel: String) {
  NEWEST("الأحدث"),
  BESTSELLER("الأكثر مبيعًا"),
  PRICE_ASC("السعر: الأقل أولًا"),
  PRICE_DESC("السعر: الأعلى أولًا"),
  HIGHEST_RATED("الأعلى تقييمًا"),
}

data class CatalogFilterState(
  val query: String = "",
  val categoryId: String? = null,
  val subCategoryId: String? = null,
  val onlyOffers: Boolean = false,
  val onlyInStock: Boolean = false,
  val onlyWholesaleEligible: Boolean = false,
  val minPrice: Double? = null,
  val maxPrice: Double? = null,
  val sortOption: CatalogSortOption = CatalogSortOption.NEWEST,
) {
  val activeFilterCount: Int
    get() = listOf(
      categoryId != null,
      subCategoryId != null,
      onlyOffers,
      onlyInStock,
      onlyWholesaleEligible,
      minPrice != null || maxPrice != null,
    ).count { it }
}

data class CatalogUiState(
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val categories: List<Category> = emptyList(),
  val products: List<Product> = emptyList(),
  val banners: List<Banner> = emptyList(),
  val brandShowcases: List<BrandShowcase> = emptyList(),
  val companies: List<StoreCompany> = emptyList(),
  val flashSales: List<FlashSale> = emptyList(),
  val storeSettings: StoreGeneralSettings = StoreGeneralSettings(),
  val isStoreSettingsLoaded: Boolean = !isLoading,
  val recentlyViewedIds: List<String> = emptyList(),
  val wishlistProductIds: Set<String> = emptySet(),
  val isWishlistAuthenticated: Boolean = false,
  val wishlistMessage: String? = null,
  val stockAlertProductIds: Set<String> = emptySet(),
  val stockAlertMessage: String? = null,
  val filter: CatalogFilterState = CatalogFilterState(),
) {
  val storefrontProducts: List<Product>
    get() = products.filter { !it.isExclusiveHiddenFromStore() }

  val categoryById: Map<String, Category>
    get() = categories.associateBy { it.id }

  val companyById: Map<String, StoreCompany>
    get() = companies.associateBy { it.id }

  val mainCategories: List<Category>
    get() = categories.filter { it.isMainCategory }

  fun subCategoriesFor(parentId: String): List<Category> =
    categories.filter { it.parentId == parentId }

  val featuredProducts: List<Product>
    get() = storefrontProducts.filter { it.isFeatured }.ifEmpty { storefrontProducts.take(8) }

  val newestProducts: List<Product>
    get() = storefrontProducts.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(12)

  val bestsellingProducts: List<Product>
    get() = storefrontProducts.sortedByDescending { it.salesCount }.take(12)

  val offerProducts: List<Product>
    get() = storefrontProducts.filter {
      it.isOffer || it.effectiveDiscountPercentWithShowcases(brandShowcases) > 0
    }.take(12)

  val recommendedProducts: List<Product>
    get() = storefrontProducts
      .sortedWith(compareByDescending<Product> { it.isFeatured }.thenByDescending { it.averageRating })
      .take(12)

  val recentlyViewedProducts: List<Product>
    get() {
      val byId = storefrontProducts.associateBy { it.id }
      return recentlyViewedIds.mapNotNull { byId[it] }
    }

  val wishlistProducts: List<Product>
    get() = products.filter { it.id in wishlistProductIds }

  fun isWishlisted(productId: String): Boolean = productId in wishlistProductIds

  fun isStockAlertEnabled(productId: String): Boolean = productId in stockAlertProductIds

  fun resolveBrandShowcase(showcaseOrCompanyId: String): BrandShowcase? {
    val cleanId = showcaseOrCompanyId.trim()
    if (cleanId.isEmpty()) return null
    brandShowcases.firstOrNull { it.id == cleanId }?.let { return it }

    val companyId = cleanId.removePrefix("company:")
    brandShowcases
      .firstOrNull { it.isActive && it.companyId == companyId }
      ?.let { return it }
    brandShowcases
      .firstOrNull { it.companyId == companyId }
      ?.let { return it }

    val company = companies.firstOrNull { it.id == companyId } ?: return null
    return BrandShowcase(
      id = "company:${company.id}",
      title = company.name,
      subtitle = "جميع منتجات ${company.name}",
      companyId = company.id,
      companyImageUrl = company.imageUrl.ifBlank { null },
      isActive = true,
    )
  }

  fun productsForBrandShowcase(showcaseId: String, categoryFilterId: String? = null): List<Product> {
    val showcase = resolveBrandShowcase(showcaseId) ?: return emptyList()
    val showcaseProducts = products.filter { it.matchesBrandShowcase(showcase) }
    if (categoryFilterId.isNullOrBlank()) return showcaseProducts
    return if (showcase.categories.isNotEmpty()) {
      showcaseProducts.filter { product ->
        product.brandCategoryId == categoryFilterId ||
          product.subCategoryId == categoryFilterId ||
          product.categoryId == categoryFilterId
      }
    } else {
      showcaseProducts.filter { product ->
        product.subCategoryId == categoryFilterId || product.categoryId == categoryFilterId
      }
    }
  }

  fun showcaseRailCategories(showcase: BrandShowcase): List<BrandShowcaseCategory> {
    if (showcase.categories.isNotEmpty()) return showcase.sortedCategories
    val companyProducts = productsForBrandShowcase(showcase.id)
    if (companyProducts.isEmpty()) return emptyList()
    val catsMap = categoryById
    val usedCategoryIds = companyProducts
      .mapNotNull { product ->
        product.subCategoryId?.takeIf { it.isNotBlank() && catsMap.containsKey(it) }
          ?: product.categoryId.takeIf { it.isNotBlank() && catsMap.containsKey(it) }
      }
      .distinct()
    return usedCategoryIds
      .mapNotNull { catsMap[it] }
      .sortedWith(compareBy<Category> { it.displayOrder }.thenBy { it.name })
      .map { cat ->
        BrandShowcaseCategory(
          id = cat.id,
          name = cat.name,
          imageUrl = cat.imageUrl.orEmpty(),
          displayOrder = cat.displayOrder,
        )
      }
  }

  fun filteredProducts(isWholesaleApproved: Boolean): List<Product> {
    val q = filter.query.trim().lowercase()
    val catsMap = categoryById
    val companiesMap = companyById

    val matched = storefrontProducts.filter { product ->
      val catName = catsMap[product.categoryId]?.name.orEmpty().lowercase()
      val subCatName = product.subCategoryId?.let { catsMap[it]?.name.orEmpty().lowercase() }.orEmpty()
      val compName = product.companyId?.let { companiesMap[it]?.name.orEmpty().lowercase() }.orEmpty()

      val matchesQuery = if (q.isEmpty()) {
        true
      } else {
        product.name.lowercase().contains(q) ||
          product.description.lowercase().contains(q) ||
          product.sku.lowercase().contains(q) ||
          product.barcode.orEmpty().lowercase().contains(q) ||
          product.qrCode.orEmpty().lowercase().contains(q) ||
          catName.contains(q) ||
          subCatName.contains(q) ||
          compName.contains(q)
      }

      val matchesCategory = filter.categoryId == null || product.categoryId == filter.categoryId
      val matchesSubCategory = filter.subCategoryId == null || product.subCategoryId == filter.subCategoryId
      val matchesOffer = !filter.onlyOffers ||
        product.isOffer ||
        product.effectiveDiscountPercentWithShowcases(brandShowcases) > 0
      val matchesStock = !filter.onlyInStock || !product.isOutOfStock
      val matchesWholesale = !filter.onlyWholesaleEligible || product.wholesalePrice > 0.0

      val retailEffective = product.effectiveRetailPriceWithShowcases(brandShowcases)
      val displayPrice = if (isWholesaleApproved && product.wholesalePrice > 0.0) {
        product.wholesalePrice
      } else {
        retailEffective
      }
      val matchesMinPrice = filter.minPrice == null || displayPrice >= filter.minPrice
      val matchesMaxPrice = filter.maxPrice == null || displayPrice <= filter.maxPrice

      matchesQuery &&
        matchesCategory &&
        matchesSubCategory &&
        matchesOffer &&
        matchesStock &&
        matchesWholesale &&
        matchesMinPrice &&
        matchesMaxPrice
    }

    return when (filter.sortOption) {
      CatalogSortOption.NEWEST -> matched.sortedByDescending { it.createdAt?.seconds ?: 0L }
      CatalogSortOption.BESTSELLER -> matched.sortedByDescending { it.salesCount }
      CatalogSortOption.PRICE_ASC -> matched.sortedBy {
        if (isWholesaleApproved && it.wholesalePrice > 0.0) it.wholesalePrice else it.effectiveRetailPriceWithShowcases(brandShowcases)
      }
      CatalogSortOption.PRICE_DESC -> matched.sortedByDescending {
        if (isWholesaleApproved && it.wholesalePrice > 0.0) it.wholesalePrice else it.effectiveRetailPriceWithShowcases(brandShowcases)
      }
      CatalogSortOption.HIGHEST_RATED -> matched.sortedByDescending { it.averageRating }
    }
  }

  fun similarProducts(product: Product): List<Product> {
    if (product.isExclusiveHiddenFromStore()) {
      return products
        .filter { candidate ->
          candidate.id != product.id && (
            (!product.companyId.isNullOrBlank() && candidate.companyId == product.companyId) ||
              (!product.brandShowcaseId.isNullOrBlank() && candidate.brandShowcaseId == product.brandShowcaseId)
            )
        }
        .take(8)
    }
    return storefrontProducts
      .filter { it.id != product.id && it.categoryId == product.categoryId }
      .take(8)
  }

  fun findExactByScannedCode(code: String): Product? {
    val clean = code.trim()
    if (clean.isEmpty()) return null
    return products.firstOrNull { product ->
      product.barcode.equals(clean, ignoreCase = true) ||
        product.qrCode.equals(clean, ignoreCase = true) ||
        product.sku.equals(clean, ignoreCase = true)
    }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
  private val categoryRepository: CategoryRepository,
  private val productRepository: ProductRepository,
  private val bannerAndStoreRepository: BannerAndStoreRepository,
  private val recentlyViewedStore: RecentlyViewedStore,
  private val flashSaleRepository: FlashSaleRepository? = null,
  private val wishlistRepository: WishlistRepository? = null,
  private val engagementRepository: EngagementRepository? = null,
  private val auth: FirebaseAuth = Firebase.auth,
) : ViewModel() {

  private val _uiState = MutableStateFlow(CatalogUiState())
  val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

  private var observeJob: Job? = null
  private val loggedSearchMisses = mutableSetOf<String>()

  private val currentUidFlow = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser?.uid)
    }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser?.uid)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  init {
    observeCatalog()
  }

  fun retry() {
    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    observeCatalog()
  }

  private fun observeCatalog() {
    observeJob?.cancel()
    observeJob = viewModelScope.launch {
      val coreCatalogFlow = combine(
        categoryRepository.observeActiveCategories(),
        productRepository.observeActiveProducts(),
        bannerAndStoreRepository.observeActiveBanners().catch { emit(emptyList()) },
        bannerAndStoreRepository.observeStoreGeneralSettings().catch { emit(StoreGeneralSettings()) },
        recentlyViewedStore.recentProductIdsFlow,
      ) { categories, products, banners, storeSettings, recentIds ->
        CatalogSnapshotBundle(categories, products, banners, emptyList(), storeSettings, recentIds, emptySet(), false)
      }
      val flashSalesFlow = (flashSaleRepository?.observeActiveFlashSales() ?: flowOf(emptyList()))
        .catch { emit(emptyList()) }
      val brandShowcasesFlow = combine(
        bannerAndStoreRepository.observeActiveBrandShowcases().catch { emit(emptyList()) },
        bannerAndStoreRepository.observeActiveCompanies().catch { emit(emptyList()) },
      ) { showcases, companies ->
        showcases to companies
      }

      val wishlistFlow = currentUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank() || wishlistRepository == null) {
          flowOf(false to emptySet())
        } else {
          wishlistRepository.observeWishlistProductIds(uid)
            .combine(flowOf(true)) { ids, isAuth -> isAuth to ids }
            .catch { emit(true to emptySet()) }
        }
      }

      val stockAlertsFlow = currentUidFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank() || engagementRepository == null) {
          flowOf(emptySet())
        } else {
          engagementRepository.observeUserStockAlertProductIds(uid)
            .catch { emit(emptySet()) }
        }
      }

      combine(
        coreCatalogFlow,
        flashSalesFlow,
        brandShowcasesFlow,
        wishlistFlow,
        stockAlertsFlow,
      ) { bundle, flashSales, (brandShowcases, companies), (isWishlistAuth, wishlistIds), stockAlertIds ->
        bundle.copy(
          flashSales = flashSales,
          brandShowcases = brandShowcases,
          companies = companies,
          wishlistIds = wishlistIds,
          isWishlistAuthenticated = isWishlistAuth,
          stockAlertIds = stockAlertIds,
        )
      }
        .catch { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.toArabicErrorMessage("تعذر تحميل كتالوج المنتجات والأقسام حاليًا."),
            )
          }
        }
        .collect { bundle ->
          _uiState.update { state ->
            state.copy(
              isLoading = false,
              isStoreSettingsLoaded = true,
              errorMessage = null,
              categories = bundle.categories,
              products = bundle.products,
              banners = bundle.banners,
              brandShowcases = bundle.brandShowcases,
              companies = bundle.companies,
              flashSales = bundle.flashSales,
              storeSettings = bundle.storeSettings,
              recentlyViewedIds = bundle.recentIds,
              wishlistProductIds = bundle.wishlistIds,
              isWishlistAuthenticated = bundle.isWishlistAuthenticated,
              stockAlertProductIds = bundle.stockAlertIds,
            )
          }
        }
    }
  }

  private data class CatalogSnapshotBundle(
    val categories: List<Category>,
    val products: List<Product>,
    val banners: List<Banner>,
    val flashSales: List<FlashSale>,
    val storeSettings: StoreGeneralSettings,
    val recentIds: List<String>,
    val wishlistIds: Set<String>,
    val isWishlistAuthenticated: Boolean,
    val brandShowcases: List<BrandShowcase> = emptyList(),
    val companies: List<StoreCompany> = emptyList(),
    val stockAlertIds: Set<String> = emptySet(),
  )

  fun toggleWishlist(productId: String, onRequireSignIn: (() -> Unit)? = null) {
    val uid = auth.currentUser?.uid
    if (uid.isNullOrBlank()) {
      onRequireSignIn?.invoke()
      return
    }
    val repo = wishlistRepository ?: return
    val currentlySaved = productId in _uiState.value.wishlistProductIds
    viewModelScope.launch {
      repo.toggleWishlist(uid, productId, currentlySaved)
        .onSuccess { added ->
          _uiState.update {
            it.copy(
              wishlistMessage = if (added) "تمت الإضافة إلى المفضلة ❤️" else "تمت الإزالة من المفضلة",
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(wishlistMessage = err.toArabicErrorMessage("تعذر تحديث المفضلة حاليًا."))
          }
        }
    }
  }

  fun clearWishlistMessage() {
    _uiState.update { it.copy(wishlistMessage = null) }
  }

  fun toggleStockAlert(product: Product, onRequireSignIn: (() -> Unit)? = null) {
    val uid = auth.currentUser?.uid
    if (uid.isNullOrBlank()) {
      onRequireSignIn?.invoke()
      return
    }
    val repo = engagementRepository ?: return
    val currentlySubscribed = product.id in _uiState.value.stockAlertProductIds
    viewModelScope.launch {
      repo.toggleStockAlert(uid, product, currentlySubscribed)
        .onSuccess { subscribed ->
          _uiState.update {
            it.copy(
              stockAlertMessage = if (subscribed) {
                "تم تفعيل التنبيه: سنُرسِل لك إشعارًا فور توفر «${product.name}»."
              } else {
                "تم إلغاء تنبيه التوفر لـ «${product.name}»."
              },
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(stockAlertMessage = err.toArabicErrorMessage("تعذر تحديث تنبيه التوفر حاليًا."))
          }
        }
    }
  }

  fun clearStockAlertMessage() {
    _uiState.update { it.copy(stockAlertMessage = null) }
  }

  fun logZeroResultSearch(rawQuery: String) {
    if (auth.currentUser == null) return
    val normalized = normalizeSearchQuery(rawQuery)
    if (normalized.length < 2) return
    if (!loggedSearchMisses.add(normalized)) return
    val repo = engagementRepository ?: return
    viewModelScope.launch {
      repo.logSearchMiss(rawQuery = rawQuery, source = "android")
    }
  }

  fun updateQuery(query: String) {
    _uiState.update { it.copy(filter = it.filter.copy(query = query)) }
  }

  fun selectCategoryFilter(categoryId: String?, subCategoryId: String? = null) {
    _uiState.update {
      it.copy(
        filter = it.filter.copy(
          categoryId = categoryId,
          subCategoryId = subCategoryId,
        ),
      )
    }
  }

  fun toggleOffersOnly() {
    _uiState.update { it.copy(filter = it.filter.copy(onlyOffers = !it.filter.onlyOffers)) }
  }

  fun toggleInStockOnly() {
    _uiState.update { it.copy(filter = it.filter.copy(onlyInStock = !it.filter.onlyInStock)) }
  }

  fun toggleWholesaleEligibleOnly() {
    _uiState.update {
      it.copy(filter = it.filter.copy(onlyWholesaleEligible = !it.filter.onlyWholesaleEligible))
    }
  }

  fun setPriceRange(minPrice: Double?, maxPrice: Double?) {
    _uiState.update {
      it.copy(filter = it.filter.copy(minPrice = minPrice, maxPrice = maxPrice))
    }
  }

  fun setSortOption(sortOption: CatalogSortOption) {
    _uiState.update { it.copy(filter = it.filter.copy(sortOption = sortOption)) }
  }

  fun openPresetSection(
    sortOption: CatalogSortOption = CatalogSortOption.NEWEST,
    onlyOffers: Boolean = false,
    categoryId: String? = null,
  ) {
    _uiState.update {
      it.copy(
        filter = CatalogFilterState(
          categoryId = categoryId,
          onlyOffers = onlyOffers,
          sortOption = sortOption,
        ),
      )
    }
  }

  fun resetFilters() {
    _uiState.update { it.copy(filter = CatalogFilterState()) }
  }

  fun recordProductViewed(productId: String) {
    viewModelScope.launch {
      recentlyViewedStore.recordViewed(productId)
    }
  }

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val appCtx = context.applicationContext
      return CatalogViewModel(
        categoryRepository = CategoryRepository(appCtx),
        productRepository = ProductRepository(appCtx),
        bannerAndStoreRepository = BannerAndStoreRepository(appCtx),
        recentlyViewedStore = RecentlyViewedStore(appCtx),
        flashSaleRepository = FlashSaleRepository(appCtx),
        wishlistRepository = WishlistRepository(appCtx),
        engagementRepository = EngagementRepository(appCtx),
        auth = Firebase.auth,
      ) as T
    }
  }
}
