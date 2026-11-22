package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.Banner
import com.example.data.model.BannerMediaType
import com.example.data.model.BannerPlacement
import com.example.data.model.BannerStyle
import com.example.data.model.BannerTargetType
import com.example.data.model.BrandShowcase
import com.example.data.model.BrandShowcaseCategory
import com.example.data.model.StoreCompany
import com.example.data.model.StoreContactChannel
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.deriveVideoPosterUrl
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BannerAndStoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id),
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("يجب تسجيل الدخول أولًا لتنفيذ إجراءات الإدارة.")
  }

  fun observeActiveBanners(): Flow<List<Banner>> {
    val query = db.collection("banners").whereEqualTo("isActive", true)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Banner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<Banner> { it.displayOrder }.thenByDescending { it.createdAt?.seconds ?: 0L })
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.LIST, "banners?isActive=true")
        throw error
      }
  }

  fun observeAllBanners(): Flow<List<Banner>> {
    val uid = requireUserId()
    val colRef = db.collection("banners")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(Banner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<Banner> { it.displayOrder }.thenByDescending { it.createdAt?.seconds ?: 0L })
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveBanner(banner: Banner): Result<String> {
    requireUserId()
    val colRef = db.collection("banners")
    val isNew = banner.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(banner.id)

    val cleanTitle = banner.title.trim()
    val cleanSubtitle = banner.subtitle?.trim()?.takeIf { it.isNotEmpty() }
    val mediaType = BannerMediaType.fromCode(banner.mediaType)
    val cleanVideoUrl = if (mediaType == BannerMediaType.VIDEO) banner.videoUrl.trim() else ""
    val cleanImageUrl = if (mediaType == BannerMediaType.VIDEO) {
      banner.imageUrl.trim().ifEmpty { deriveVideoPosterUrl(cleanVideoUrl) }
    } else {
      banner.imageUrl.trim()
    }
    val targetType = BannerTargetType.fromCode(banner.targetType)
    val cleanTargetId = banner.targetId?.trim()?.takeIf {
      it.isNotEmpty() && (targetType == BannerTargetType.CATEGORY || targetType == BannerTargetType.PRODUCT)
    }
    val cleanPlacement = BannerPlacement.fromCode(banner.placement).code
    val cleanStyle = BannerStyle.fromCode(banner.bannerStyle).code
    val cleanDuration = banner.durationSeconds.coerceIn(1, 120)

    return try {
      if (isNew) {
        val payload = buildMap<String, Any> {
          put("title", cleanTitle)
          if (cleanSubtitle != null) put("subtitle", cleanSubtitle)
          put("imageUrl", cleanImageUrl)
          put("mediaType", mediaType.code)
          if (cleanVideoUrl.isNotEmpty()) put("videoUrl", cleanVideoUrl)
          put("videoMuted", mediaType == BannerMediaType.VIDEO && banner.videoMuted)
          put("targetType", targetType.code)
          if (cleanTargetId != null) put("targetId", cleanTargetId)
          put("placement", cleanPlacement)
          put("bannerStyle", cleanStyle)
          put("durationSeconds", cleanDuration)
          put("displayOrder", banner.displayOrder.coerceIn(0, 1000))
          put("isActive", banner.isActive)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(payload).await()
      } else {
        val updates = buildMap<String, Any> {
          put("title", cleanTitle)
          put("subtitle", cleanSubtitle ?: FieldValue.delete())
          put("imageUrl", cleanImageUrl)
          put("mediaType", mediaType.code)
          put("videoUrl", cleanVideoUrl.ifEmpty { null } ?: FieldValue.delete())
          put("videoMuted", mediaType == BannerMediaType.VIDEO && banner.videoMuted)
          put("targetType", targetType.code)
          put("targetId", cleanTargetId ?: FieldValue.delete())
          put("placement", cleanPlacement)
          put("bannerStyle", cleanStyle)
          put("durationSeconds", cleanDuration)
          put("displayOrder", banner.displayOrder.coerceIn(0, 1000))
          put("isActive", banner.isActive)
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updates).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteBanner(bannerId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("banners").document(bannerId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeActiveCompanies(): Flow<List<StoreCompany>> {
    val query = db.collection("companies").whereEqualTo("isActive", true)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(StoreCompany::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<StoreCompany> { it.displayOrder }.thenBy { it.name })
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.LIST, "companies?isActive=true")
        throw error
      }
  }

  fun observeAllCompanies(): Flow<List<StoreCompany>> {
    val uid = requireUserId()
    val colRef = db.collection("companies")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(StoreCompany::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<StoreCompany> { it.displayOrder }.thenBy { it.name })
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveCompany(company: StoreCompany): Result<String> {
    requireUserId()
    val colRef = db.collection("companies")
    val isNew = company.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(company.id)

    val cleanName = company.name.trim()
    val cleanImageUrl = company.imageUrl.trim().takeIf { it.isNotEmpty() }
    val cleanOrder = company.displayOrder.coerceIn(0, 1000)

    return try {
      if (isNew) {
        val payload = buildMap<String, Any> {
          put("name", cleanName)
          if (cleanImageUrl != null) put("imageUrl", cleanImageUrl)
          put("displayOrder", cleanOrder)
          put("isActive", company.isActive)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(payload).await()
      } else {
        val updates = buildMap<String, Any> {
          put("name", cleanName)
          put("imageUrl", cleanImageUrl ?: FieldValue.delete())
          put("displayOrder", cleanOrder)
          put("isActive", company.isActive)
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updates).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteCompany(companyId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("companies").document(companyId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeActiveBrandShowcases(): Flow<List<BrandShowcase>> {
    val query = db.collection("brandShowcases").whereEqualTo("isActive", true)
    return query.snapshots()
      .map { snapshot ->
        snapshot.toObjects(BrandShowcase::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<BrandShowcase> { it.displayOrder }.thenByDescending { it.createdAt?.seconds ?: 0L })
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.LIST, "brandShowcases?isActive=true")
        throw error
      }
  }

  fun observeAllBrandShowcases(): Flow<List<BrandShowcase>> {
    val uid = requireUserId()
    val colRef = db.collection("brandShowcases")
    return colRef.snapshots()
      .map { snapshot ->
        snapshot.toObjects(BrandShowcase::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<BrandShowcase> { it.displayOrder }.thenByDescending { it.createdAt?.seconds ?: 0L })
      }
      .catch { error ->
        if (error is CancellationException && auth.currentUser?.uid != uid) throw error
        handleFirestoreError(error, OperationType.LIST, colRef.path)
        throw error
      }
  }

  suspend fun saveBrandShowcase(showcase: BrandShowcase): Result<String> {
    requireUserId()
    val colRef = db.collection("brandShowcases")
    val isNew = showcase.id.isBlank()
    val docRef = if (isNew) colRef.document() else colRef.document(showcase.id)

    val cleanTitle = showcase.title.trim()
    val cleanSubtitle = showcase.subtitle?.trim()?.takeIf { it.isNotEmpty() }
    val cleanCompanyId = showcase.companyId?.trim()?.takeIf { it.isNotEmpty() }
    val cleanCompanyImageUrl = showcase.companyImageUrl?.trim()?.takeIf { it.isNotEmpty() }
    val cleanBannerUrl = showcase.bannerImageUrl.trim().takeIf { it.isNotEmpty() }
    val cleanDiscountPercent = showcase.discountPercent.coerceIn(0.0, 95.0)
    val cleanAccentHex = showcase.accentColorHex.trim().ifEmpty { "#FCE4EC" }
    val cleanPlacement = BannerPlacement.fromCode(showcase.placement).code
    val cleanCategories = showcase.categories
      .filter { it.name.trim().isNotEmpty() }
      .take(40)
      .mapIndexed { idx, cat ->
        mapOf(
          "id" to cat.id.trim().ifEmpty { "bcat_${System.currentTimeMillis()}_$idx" },
          "name" to cat.name.trim(),
          "imageUrl" to cat.imageUrl.trim(),
          "displayOrder" to cat.displayOrder.coerceAtLeast(0),
        )
      }

    return try {
      if (isNew) {
        val payload = buildMap<String, Any> {
          put("title", cleanTitle)
          if (cleanSubtitle != null) put("subtitle", cleanSubtitle)
          if (cleanCompanyId != null) put("companyId", cleanCompanyId)
          if (cleanCompanyImageUrl != null) put("companyImageUrl", cleanCompanyImageUrl)
          if (cleanBannerUrl != null) put("bannerImageUrl", cleanBannerUrl)
          put("discountPercent", cleanDiscountPercent)
          put("accentColorHex", cleanAccentHex)
          put("placement", cleanPlacement)
          put("displayOrder", showcase.displayOrder.coerceIn(0, 1000))
          put("isActive", showcase.isActive)
          put("categories", cleanCategories)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.set(payload).await()
      } else {
        val updates = buildMap<String, Any> {
          put("title", cleanTitle)
          put("subtitle", cleanSubtitle ?: FieldValue.delete())
          put("companyId", cleanCompanyId ?: FieldValue.delete())
          put("companyImageUrl", cleanCompanyImageUrl ?: FieldValue.delete())
          put("bannerImageUrl", cleanBannerUrl ?: FieldValue.delete())
          put("discountPercent", cleanDiscountPercent)
          put("accentColorHex", cleanAccentHex)
          put("placement", cleanPlacement)
          put("displayOrder", showcase.displayOrder.coerceIn(0, 1000))
          put("isActive", showcase.isActive)
          put("categories", cleanCategories)
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updates).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteBrandShowcase(showcaseId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("brandShowcases").document(showcaseId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeStoreGeneralSettings(): Flow<StoreGeneralSettings> {
    val docRef = db.collection("settings").document("store")
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(StoreGeneralSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: StoreGeneralSettings()
        } else {
          StoreGeneralSettings()
        }
      }
      .catch { error ->
        handleFirestoreError(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  suspend fun saveStoreGeneralSettings(settings: StoreGeneralSettings): Result<Unit> {
    requireUserId()
    val docRef = db.collection("settings").document("store")
    return try {
      val cleanSocialPages = settings.socialPages
        .filter { it.title.isNotBlank() && it.url.isNotBlank() }
        .map { item ->
          mapOf(
            "title" to item.title.trim(),
            "url" to item.url.trim(),
            "channel" to StoreContactChannel.fromCode(item.channel).code,
          )
        }
      val cleanRetailPhones = settings.retailPhones
        .filter { it.phoneNumber.isNotBlank() }
        .map { item ->
          mapOf(
            "label" to item.label.trim(),
            "phoneNumber" to item.phoneNumber.trim(),
            "channel" to StoreContactChannel.RETAIL.code,
          )
        }
      val cleanWholesalePhones = settings.wholesalePhones
        .filter { it.phoneNumber.isNotBlank() }
        .map { item ->
          mapOf(
            "label" to item.label.trim(),
            "phoneNumber" to item.phoneNumber.trim(),
            "channel" to StoreContactChannel.WHOLESALE.code,
          )
        }

      val payload = mapOf(
        "storeName" to settings.storeName.trim().ifEmpty { "الإيمان للتجارة" },
        "tagline" to settings.tagline.trim().ifEmpty { "تسوق بذكاء .. عيش أفضل" },
        "ownerEmail" to settings.ownerEmail.trim().lowercase().ifEmpty { UserRepository.BOOTSTRAPPED_OWNER_EMAIL },
        "supportPhone" to settings.supportPhone.trim(),
        "whatsappNumber" to settings.whatsappNumber.trim(),
        "storeAddress" to settings.storeAddress.trim(),
        "announcementText" to settings.announcementText.trim(),
        "aboutStoreDescription" to settings.aboutStoreDescription.trim(),
        "retailFacebookUrl" to settings.retailFacebookUrl.trim(),
        "wholesaleFacebookUrl" to settings.wholesaleFacebookUrl.trim(),
        "socialPages" to cleanSocialPages,
        "retailPhones" to cleanRetailPhones,
        "wholesalePhones" to cleanWholesalePhones,
        "showFeaturedSection" to settings.showFeaturedSection,
        "showDealsSection" to settings.showDealsSection,
        "showBestsellersSection" to settings.showBestsellersSection,
        "showRecommendedSection" to settings.showRecommendedSection,
        "showProductColors" to settings.showProductColors,
        "showProductSizes" to settings.showProductSizes,
        "showHeroSection" to settings.showHeroSection,
        "heroBadgeText" to settings.heroBadgeText.trim(),
        "heroTitleText" to settings.heroTitleText.trim(),
        "heroSubtitleText" to settings.heroSubtitleText.trim(),
        "heroCtaText" to settings.heroCtaText.trim().ifEmpty { "تسوق الآن" },
        "homeCategoryStyle" to settings.homeCategoryStyleEnum.code,
        "categoriesTabStyle" to settings.categoriesTabStyleEnum.code,
        "companyOfferBadgeStyle" to settings.companyOfferBadgeStyleEnum.code,
        "numeralFormat" to settings.numeralFormatEnum.code,
        "freeShippingEnabled" to settings.freeShippingEnabled,
        "freeShippingThreshold" to settings.freeShippingThreshold.coerceAtLeast(0.0),
        "deliveryOtpEnabled" to settings.deliveryOtpEnabled,
        "introVideoEnabled" to settings.introVideoEnabled,
        "introMediaType" to settings.introMediaTypeEnum.code,
        "introVideoUrl" to settings.introVideoUrl.trim(),
        "introImageUrl" to settings.introImageUrl.trim(),
        "introVideoDurationSeconds" to settings.introVideoDurationSeconds.coerceIn(0, 120),
        "introVideoTitle" to settings.introVideoTitle.trim().take(100),
        "introVideoMuted" to settings.introVideoMuted,
        "tickerEnabled" to settings.tickerEnabled,
        "tickerText" to settings.tickerText.trim().take(1000),
        "tickerPlacement" to settings.announcementTickerConfig.placement.code,
        "tickerBackgroundColor" to settings.announcementTickerConfig.safeBackgroundHex,
        "tickerBorderColor" to settings.announcementTickerConfig.safeBorderHex,
        "tickerTextColor" to settings.announcementTickerConfig.safeTextHex,
        "tickerFontSizeSp" to settings.announcementTickerConfig.safeFontSizeSp,
        "tickerFontWeight" to settings.announcementTickerConfig.fontWeight.code,
        "tickerItalic" to settings.tickerItalic,
        "tickerInlineImages" to settings.announcementTickerConfig.inlineImages.map { img ->
          mapOf("id" to img.id.trim(), "url" to img.url.trim())
        },
        "appIconTheme" to settings.appIconThemeEnum.code,
        "productCardBorderStyle" to settings.productCardBorderStyleEnum.code,
        "productCardBorderColor" to settings.productCardStyleConfig.safeCustomBorderHex,
        "productCardShape" to settings.productCardShapeEnum.code,
        "productDiscountBadgeStyle" to settings.productDiscountBadgeStyleEnum.code,
        "privacyPolicyUrl" to settings.privacyPolicyUrl.trim(),
        "returnPolicyUrl" to settings.returnPolicyUrl.trim(),
        "accountDeletionUrl" to settings.accountDeletionUrl.trim(),
        "privacyPolicyText" to settings.effectivePrivacyPolicyText,
        "returnPolicyText" to settings.effectiveReturnPolicyText,
        "termsOfServiceText" to settings.effectiveTermsOfServiceText,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }
}
