package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

enum class BannerTargetType(val code: String, val arabicLabel: String) {
  NONE("none", "عرض إعلاني فقط"),
  OFFERS("offers", "قسم العروض والخصومات"),
  CATEGORY("category", "قسم محدد"),
  PRODUCT("product", "منتج محدد");

  companion object {
    fun fromCode(code: String?): BannerTargetType =
      entries.firstOrNull { it.code == code } ?: NONE
  }
}

enum class BannerPlacement(val code: String, val arabicLabel: String) {
  TOP_HOME("top_home", "أعلى الرئيسية (الافتراضي)"),
  BELOW_HERO("below_hero", "أسفل بطاقة الترحيب الرئيسية"),
  BELOW_CATEGORIES("below_categories", "أسفل شريط الفئات"),
  BELOW_FEATURED("below_featured", "أسفل المنتجات المميزة"),
  BELOW_DEALS("below_deals", "أسفل العروض والخصومات"),
  BELOW_NEWEST("below_newest", "أسفل المنتجات التي وصلت حديثًا"),
  BELOW_BESTSELLERS("below_bestsellers", "أسفل الأكثر مبيعًا"),
  BELOW_RECOMMENDED("below_recommended", "أسفل المنتجات المقترحة");

  companion object {
    fun fromCode(code: String?): BannerPlacement =
      entries.firstOrNull { it.code == code } ?: TOP_HOME
  }
}

enum class BannerStyle(val code: String, val arabicLabel: String) {
  NORMAL("normal", "بنر عادي (كبير)"),
  SLIM("slim", "بنر إعلاني رفيع");

  companion object {
    fun fromCode(code: String?): BannerStyle =
      entries.firstOrNull { it.code == code } ?: NORMAL
  }
}

enum class HomeCategoryStyle(val code: String, val arabicLabel: String, val subtitle: String) {
  CIRCULAR("circular", "دائري", "أيقونات وصور دائرية متجاورة في شريط أفقي"),
  ROUNDED_SQUARE("rounded_square", "مربعات ناعمة", "كروت مربعة بزوايا دائرية وإطار أنيق"),
  NOON_PORTRAIT("noon_portrait", "طولي حديث", "كروت طولية مائلة بأسلوب متاجر نون"),
  PILL_CHIPS("pill_chips", "كبسولات أفقية", "أزرار كبسولية مع صورة مصغرة وعدد المنتجات"),
  DOUBLE_GRID("double_grid", "شبكة متكاملة", "شبكة منظمة من 3 أعمدة داخل الرئيسية");

  companion object {
    fun fromCode(code: String?): HomeCategoryStyle =
      entries.firstOrNull { it.code == code } ?: CIRCULAR
  }
}

enum class CategoriesTabStyle(val code: String, val arabicLabel: String, val subtitle: String) {
  NOON_PORTRAIT("noon_portrait", "كروت طولية (3 أعمدة)", "تصميم طولي بحواف دائرية وخلفية ناعمة"),
  CIRCULAR_GRID("circular_grid", "دائري (3 أعمدة)", "صور دائرية بإطار ذهبي وأنيق"),
  HEXAGON_GRID("hexagon_grid", "سداسي هندسي", "إطارات سداسية مميزة في شبكة 3 أعمدة"),
  CUT_CORNER_GRID("cut_corner_grid", "هندسي مشطوف", "كروت بزوايا مشطوفة عصرية في 3 أعمدة"),
  DETAILED_CARDS("detailed_cards", "بطاقات أفقية مفصلة", "عرض أفقي يوضح الصورة والوصف وعدد المنتجات");

  companion object {
    fun fromCode(code: String?): CategoriesTabStyle =
      entries.firstOrNull { it.code == code } ?: NOON_PORTRAIT
  }
}

enum class CompanyOfferBadgeStyle(val code: String, val arabicLabel: String, val subtitle: String) {
  GOLD_TICKET("gold_ticket", "تذكرة ذهبية فاخرة", "بطاقة داكنة بحافة ذهبية وشكل تذكرة خصم"),
  CRIMSON_RIBBON("crimson_ribbon", "شريط عروض ناري", "تدرج أحمر وبرتقالي جذاب مع نسبة خصم واضحة"),
  EMERALD_GLASS("emerald_glass", "شارة زمردية عصرية", "تدرج أخضر زمردي أنيق مع شارة خصم بيضاء"),
  ROYAL_SHIELD("royal_shield", "وسام ملكي فاخر", "تدرج ملكي بنفسجي بإطار ذهبي ونجمة مميزة"),
  SUNBURST_STAMP("sunburst_stamp", "ختم خصم أصفر بارز", "شارة صفراء جريئة بإطار داكن تلفت الانتباه");

  companion object {
    fun fromCode(code: String?): CompanyOfferBadgeStyle =
      entries.firstOrNull { it.code == code } ?: GOLD_TICKET
  }
}

enum class BannerMediaType(val code: String, val arabicLabel: String) {
  IMAGE("image", "صورة ثابتة"),
  ANIMATED_IMAGE("animated_image", "صورة متحركة (GIF / WebP)"),
  VIDEO("video", "فيديو دعائي قصير");

  companion object {
    fun fromCode(code: String?): BannerMediaType =
      entries.firstOrNull { it.code == code } ?: IMAGE
  }
}

enum class IntroMediaType(val code: String, val arabicLabel: String) {
  VIDEO("video", "فيديو ريلز قصير"),
  ANIMATED_IMAGE("animated_image", "صورة متحركة (GIF / WebP)");

  companion object {
    fun fromCode(code: String?): IntroMediaType =
      entries.firstOrNull { it.code == code } ?: VIDEO
  }
}

enum class IntroReelTargetType(
  val code: String,
  val arabicLabel: String,
  val subtitle: String,
) {
  PROMO_ONLY("promo_only", "ريلز دعائي فقط", "بدون تحويل عند الضغط على المقطع"),
  CATEGORY("category", "قسم معين", "فتح قسم محدد عند الضغط على المقطع"),
  PRODUCT("product", "منتج معين", "فتح صفحة منتج محدد عند الضغط"),
  COMPANY("company", "منتجات شركة", "عرض منتجات شركة محددة عند الضغط");

  companion object {
    fun fromCode(code: String?): IntroReelTargetType =
      entries.firstOrNull { it.code == code } ?: PROMO_ONLY
  }
}

enum class AppIconTheme(
  val code: String,
  val arabicLabel: String,
  val subtitle: String,
  val aliasSimpleName: String,
) {
  DEFAULT("default", "الأيقونة الأساسية", "شعار الإيمان للتجارة الافتراضي", "MainActivityDefault"),
  OFFERS("offers", "أيقونة العروض", "نفس الشعار مع شارة «عروض» بارزة", "MainActivityOffers"),
  RAMADAN("ramadan", "أيقونة رمضان", "نفس الشعار مع فانوس وهلال رمضان", "MainActivityRamadan"),
  EID("eid", "أيقونة العيد", "نفس الشعار مع بالونات وزينة «عيد سعيد»", "MainActivityEid");

  companion object {
    fun fromCode(code: String?): AppIconTheme =
      entries.firstOrNull { it.code == code } ?: DEFAULT
  }
}

enum class ProductCardBorderStyle(
  val code: String,
  val arabicLabel: String,
  val subtitle: String,
  val previewHex: String,
) {
  DEFAULT("default", "كلاسيكي ناعم", "إطار رمادي خفيف ونظيف", "#E3E6EC"),
  GOLD("gold", "إطار ذهبي فاخر", "تدرج ذهبي معدني لامع حول الكارت", "#D4AF37"),
  EMERALD("emerald", "إطار زمردي", "أخضر زمردي أنيق وواضح", "#059669"),
  ROYAL_BLUE("royal_blue", "إطار أزرق ملكي", "أزرق ملكي متدرج بارز", "#1D4ED8"),
  RUBY("ruby", "إطار ياقوتي", "أحمر عروض جذاب ومتدرج", "#DC2626"),
  PURPLE("purple", "إطار بنفسجي", "بنفسجي عصري مميز", "#7C3AED"),
  CHARCOAL("charcoal", "إطار أسود عصري", "تحديد داكن حاد وأنيق", "#181A20"),
  CUSTOM("custom", "لون مخصص", "اختر أي لون خاص لإطار الكارت", "#D4AF37"),
  NONE("none", "بدون إطار", "مظهر ناعم بظل خفيف بدون حدود", "#FFFFFF");

  companion object {
    fun fromCode(code: String?): ProductCardBorderStyle =
      entries.firstOrNull { it.code == code } ?: DEFAULT
  }
}

enum class ProductCardShape(
  val code: String,
  val arabicLabel: String,
  val subtitle: String,
) {
  ROUNDED("rounded", "زوايا دائرية ناعمة", "الشكل الكلاسيكي بزوايا مستديرة متوازنة"),
  CIRCULAR("circular", "دائري / بيضاوي ناعم", "حواف دائرية واسعة مع إطار دائري لصورة المنتج"),
  RECTANGLE("rectangle", "مستطيل حاد", "خطوط مستقيمة أنيقة بزوايا شبه قائمة"),
  CUT_CORNER("cut_corner", "هندسي مشطوف", "زوايا هندسية مشطوفة (مثمن عصري)"),
  ASYMMETRIC("asymmetric", "هندسي مائل (ورقة)", "زاويتان دائريتان متقابلتان وزاويتان حادتان"),
  ARCH("arch", "قوس علوي فاخر", "قوس علوي بارز يمنح الكارت طابعاً فاخراً");

  companion object {
    fun fromCode(code: String?): ProductCardShape =
      entries.firstOrNull { it.code == code } ?: ROUNDED
  }
}

enum class ProductDiscountBadgeStyle(
  val code: String,
  val arabicLabel: String,
  val subtitle: String,
) {
  PILL("pill", "كبسولة كلاسيكية", "شارة حمراء مستديرة واضحة بنسبة الخصم"),
  RIBBON("ribbon", "شريط ركن مائل", "شريط خصم مائل يلتف على زاوية الصورة"),
  STARBURST("starburst", "ختم نجمة العروض", "ختم نجمي هندسي يلفت الانتباه للخصم"),
  PRICE_TAG("price_tag", "بطاقة سعر (تاج)", "شكل تاج سعر مميز بحلقة جانبية"),
  GOLD_LUXURY("gold_luxury", "شارة ذهبية فاخرة", "خلفية داكنة بحافة ذهبية وأيقونة بريق"),
  CAPSULE_OUTLINE("capsule_outline", "كبسولة مؤطرة عصرية", "خلفية بيضاء نظيفة بإطار أحمر بارز");

  companion object {
    fun fromCode(code: String?): ProductDiscountBadgeStyle =
      entries.firstOrNull { it.code == code } ?: PILL
  }
}

fun sanitizeCardHexColor(raw: String?, fallback: String = "#D4AF37"): String {
  val trimmed = raw?.trim().orEmpty()
  val normalized = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
  return if (Regex("^#[0-9A-Fa-f]{6}$").matches(normalized)) {
    normalized.uppercase()
  } else {
    fallback
  }
}

data class ProductCardStyleConfig(
  val borderStyle: ProductCardBorderStyle = ProductCardBorderStyle.DEFAULT,
  val customBorderHex: String = "#D4AF37",
  val shape: ProductCardShape = ProductCardShape.ROUNDED,
  val discountBadgeStyle: ProductDiscountBadgeStyle = ProductDiscountBadgeStyle.PILL,
) {
  val safeCustomBorderHex: String
    get() = sanitizeCardHexColor(customBorderHex)
}

fun deriveVideoPosterUrl(videoUrl: String): String {
  val clean = videoUrl.trim()
  if (clean.isBlank()) return ""
  if (!clean.contains("res.cloudinary.com") || !clean.contains("/video/upload/")) {
    return ""
  }
  val withoutQuery = clean.substringBefore('?')
  val replaced = withoutQuery.replace(Regex("\\.(mp4|mov|webm|mkv|m4v)$", RegexOption.IGNORE_CASE), ".jpg")
  return if (replaced != withoutQuery) replaced else ""
}

enum class TickerFontWeight(val code: String, val arabicLabel: String) {
  LIGHT("light", "رفيع"),
  NORMAL("normal", "عادي"),
  BOLD("bold", "عريض"),
  EXTRA_BOLD("extra_bold", "عريض جدًا");

  companion object {
    fun fromCode(code: String?): TickerFontWeight =
      entries.firstOrNull { it.code == code } ?: BOLD
  }
}

@Keep
@IgnoreExtraProperties
data class TickerInlineImage(
  val id: String = "",
  val url: String = "",
)

data class AnnouncementTickerConfig(
  val enabled: Boolean = false,
  val text: String = "",
  val placement: BannerPlacement = BannerPlacement.TOP_HOME,
  val backgroundColorHex: String = "#181A20",
  val borderColorHex: String = "#FFD000",
  val textColorHex: String = "#FFFFFF",
  val fontSizeSp: Int = 14,
  val fontWeight: TickerFontWeight = TickerFontWeight.BOLD,
  val isItalic: Boolean = false,
  val inlineImages: List<TickerInlineImage> = emptyList(),
) {
  val isActive: Boolean
    get() = enabled && (text.isNotBlank() || inlineImages.any { it.url.isNotBlank() })

  val safeBackgroundHex: String
    get() = sanitizeCardHexColor(backgroundColorHex, "#181A20")

  val safeBorderHex: String
    get() = sanitizeCardHexColor(borderColorHex, "#FFD000")

  val safeTextHex: String
    get() = sanitizeCardHexColor(textColorHex, "#FFFFFF")

  val safeFontSizeSp: Int
    get() = fontSizeSp.coerceIn(11, 24)
}

@Keep
@IgnoreExtraProperties
data class Banner(
  @DocumentId val id: String = "",
  val title: String = "",
  val subtitle: String? = null,
  val imageUrl: String = "",
  val mediaType: String = BannerMediaType.IMAGE.code,
  val videoUrl: String = "",
  val videoMuted: Boolean = false,
  val targetType: String = BannerTargetType.NONE.code,
  val targetId: String? = null,
  val placement: String = BannerPlacement.TOP_HOME.code,
  val bannerStyle: String = BannerStyle.NORMAL.code,
  val durationSeconds: Int = 5,
  val displayOrder: Int = 0,
  @get:PropertyName("isActive") @PropertyName("isActive")
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val mediaTypeEnum: BannerMediaType
    get() = BannerMediaType.fromCode(mediaType)

  @get:Exclude
  val isVideoBanner: Boolean
    get() = mediaTypeEnum == BannerMediaType.VIDEO && videoUrl.trim().length >= 5

  @get:Exclude
  val isAnimatedImageBanner: Boolean
    get() = mediaTypeEnum == BannerMediaType.ANIMATED_IMAGE && imageUrl.trim().length >= 5

  @get:Exclude
  val effectivePosterUrl: String
    get() = imageUrl.trim().ifEmpty { deriveVideoPosterUrl(videoUrl) }

  @get:Exclude
  val targetTypeEnum: BannerTargetType
    get() = BannerTargetType.fromCode(targetType)

  @get:Exclude
  val placementEnum: BannerPlacement
    get() = BannerPlacement.fromCode(placement)

  @get:Exclude
  val bannerStyleEnum: BannerStyle
    get() = BannerStyle.fromCode(bannerStyle)

  @get:Exclude
  val safeDurationSeconds: Int
    get() = durationSeconds.coerceIn(1, 120)
}

@Keep
@IgnoreExtraProperties
data class StoreCompany(
  @DocumentId val id: String = "",
  val name: String = "",
  val imageUrl: String = "",
  val displayOrder: Int = 0,
  @get:PropertyName("isActive") @PropertyName("isActive")
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class BrandShowcaseCategory(
  val id: String = "",
  val name: String = "",
  val imageUrl: String = "",
  val displayOrder: Int = 0,
)

@Keep
@IgnoreExtraProperties
data class BrandShowcase(
  @DocumentId val id: String = "",
  val title: String = "",
  val subtitle: String? = null,
  val companyId: String? = null,
  val companyImageUrl: String? = null,
  val bannerImageUrl: String = "",
  val discountPercent: Double = 0.0,
  val accentColorHex: String = "#FCE4EC",
  val placement: String = BannerPlacement.BELOW_CATEGORIES.code,
  val displayOrder: Int = 0,
  @get:PropertyName("isActive") @PropertyName("isActive")
  val isActive: Boolean = true,
  val categories: List<BrandShowcaseCategory> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val placementEnum: BannerPlacement
    get() = BannerPlacement.fromCode(placement)

  @get:Exclude
  val hasCustomBanner: Boolean
    get() = bannerImageUrl.isNotBlank()

  @get:Exclude
  val cleanDiscountPercent: Int
    get() = discountPercent.roundToInt().coerceIn(0, 95)

  @get:Exclude
  val sortedCategories: List<BrandShowcaseCategory>
    get() = categories.sortedWith(compareBy<BrandShowcaseCategory> { it.displayOrder }.thenBy { it.name })
}

fun Product.matchesBrandShowcase(showcase: BrandShowcase): Boolean {
  val linkedByCompany = !showcase.companyId.isNullOrBlank() && this.companyId == showcase.companyId
  val linkedLegacy = this.brandShowcaseId == showcase.id
  return linkedByCompany || linkedLegacy
}

fun Product.isExclusiveHiddenFromStore(): Boolean {
  return exclusiveToBrandShowcase && (!companyId.isNullOrBlank() || !brandShowcaseId.isNullOrBlank())
}

fun Product.effectiveShowcaseDiscountPercent(brandShowcases: List<BrandShowcase>): Int {
  return brandShowcases
    .asSequence()
    .filter { it.isActive && it.cleanDiscountPercent > 0 && this.matchesBrandShowcase(it) }
    .maxOfOrNull { it.cleanDiscountPercent }
    ?: 0
}

fun Product.effectiveRetailPriceWithShowcases(brandShowcases: List<BrandShowcase>): Double {
  val baseRetail = this.effectiveRetailPrice
  val showcasePct = this.effectiveShowcaseDiscountPercent(brandShowcases)
  if (showcasePct <= 0 || retailPrice <= 0.0) return baseRetail
  val showcasePrice = (((retailPrice * (1.0 - showcasePct / 100.0)) * 100.0).roundToInt() / 100.0)
    .coerceAtLeast(0.01)
  return minOf(baseRetail, showcasePrice)
}

fun Product.effectiveDiscountPercentWithShowcases(brandShowcases: List<BrandShowcase>): Int {
  val effective = effectiveRetailPriceWithShowcases(brandShowcases)
  if (retailPrice <= 0.0 || effective >= retailPrice) return 0
  return (((retailPrice - effective) / retailPrice) * 100.0).roundToInt().coerceIn(1, 99)
}

fun Product.effectiveUnitPriceWithShowcases(
  isWholesaleApproved: Boolean,
  quantity: Int = wholesaleMinQty,
  brandShowcases: List<BrandShowcase> = emptyList(),
): Double {
  return if (isWholesaleApproved && wholesalePrice > 0.0 && quantity >= wholesaleMinQty) {
    wholesalePrice
  } else {
    effectiveRetailPriceWithShowcases(brandShowcases)
  }
}

fun computeBannerlessShowcaseAlignLeftMap(showcases: List<BrandShowcase>): Map<String, Boolean> {
  return showcases
    .filter { it.isActive && !it.hasCustomBanner }
    .sortedWith(
      compareBy<BrandShowcase> { it.placementEnum.ordinal }
        .thenBy { it.displayOrder }
        .thenByDescending { it.createdAt?.seconds ?: 0L },
    )
    .mapIndexed { index, showcase -> showcase.id to (index % 2 == 1) }
    .toMap()
}

@Keep
@IgnoreExtraProperties
data class ProductReview(
  @DocumentId val id: String = "",
  val productId: String = "",
  val productName: String = "",
  val userId: String = "",
  val userName: String = "",
  val rating: Int = 5,
  val comment: String = "",
  @get:PropertyName("isHidden") @PropertyName("isHidden")
  val isHidden: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class StoreContactChannel(val code: String, val arabicLabel: String) {
  RETAIL("retail", "قطاعي"),
  WHOLESALE("wholesale", "جملة"),
  BOTH("both", "قطاعي وجملة");

  companion object {
    fun fromCode(code: String?): StoreContactChannel =
      entries.firstOrNull { it.code == code } ?: RETAIL
  }
}

@Keep
@IgnoreExtraProperties
data class StoreSocialLink(
  val title: String = "",
  val url: String = "",
  val channel: String = StoreContactChannel.RETAIL.code,
) {
  @get:Exclude
  val channelEnum: StoreContactChannel
    get() = StoreContactChannel.fromCode(channel)
}

@Keep
@IgnoreExtraProperties
data class StorePhoneEntry(
  val label: String = "",
  val phoneNumber: String = "",
  val channel: String = StoreContactChannel.RETAIL.code,
) {
  @get:Exclude
  val channelEnum: StoreContactChannel
    get() = StoreContactChannel.fromCode(channel)
}

enum class NumeralFormat(val code: String, val arabicLabel: String, val exampleText: String) {
  ARABIC("arabic", "أرقام عربية (١٢٣)", "مثال عرض الأسعار: ١٬٢٥٠ ج.م"),
  ENGLISH("english", "أرقام إنجليزية (123)", "مثال عرض الأسعار: 1,250 ج.م");

  companion object {
    fun fromCode(code: String?): NumeralFormat =
      entries.firstOrNull { it.code == code } ?: ARABIC
  }
}

enum class StorePolicyTab(val code: String, val arabicLabel: String) {
  PRIVACY("privacy", "سياسة الخصوصية"),
  RETURN("return", "الاسترجاع والاستبدال"),
  TERMS("terms", "الشروط والأحكام");

  companion object {
    fun fromCode(code: String?): StorePolicyTab =
      entries.firstOrNull { it.code == code } ?: PRIVACY
  }
}

@Keep
@IgnoreExtraProperties
data class StoreGeneralSettings(
  @DocumentId val id: String = "store",
  val storeName: String = "الإيمان للتجارة",
  val tagline: String = "تسوق بذكاء .. عيش أفضل",
  val ownerEmail: String = "mohamedamerghazy@gmail.com",
  val supportPhone: String = "",
  val whatsappNumber: String = "",
  val storeAddress: String = "",
  val announcementText: String = "",
  val aboutStoreDescription: String = "",
  val retailFacebookUrl: String = "",
  val wholesaleFacebookUrl: String = "",
  val socialPages: List<StoreSocialLink> = emptyList(),
  val retailPhones: List<StorePhoneEntry> = emptyList(),
  val wholesalePhones: List<StorePhoneEntry> = emptyList(),
  val showFeaturedSection: Boolean = true,
  val showDealsSection: Boolean = true,
  val showBestsellersSection: Boolean = true,
  val showRecommendedSection: Boolean = true,
  val showProductColors: Boolean = true,
  val showProductSizes: Boolean = true,
  val showHeroSection: Boolean = true,
  val heroBadgeText: String = "عروض القطاعي والجملة",
  val heroTitleText: String = "كل ما تحتاجه بأفضل سعر للجملة والقطاعي",
  val heroSubtitleText: String = "تصفح الأقسام وتابع العروض اليومية مع توصيل سريع لكل المناطق.",
  val heroCtaText: String = "تسوق الآن",
  val homeCategoryStyle: String = HomeCategoryStyle.CIRCULAR.code,
  val categoriesTabStyle: String = CategoriesTabStyle.NOON_PORTRAIT.code,
  val companyOfferBadgeStyle: String = CompanyOfferBadgeStyle.GOLD_TICKET.code,
  val numeralFormat: String = NumeralFormat.ARABIC.code,
  val freeShippingEnabled: Boolean = false,
  val freeShippingThreshold: Double = 500.0,
  val deliveryOtpEnabled: Boolean = true,
  val introVideoEnabled: Boolean = false,
  val introMediaType: String = IntroMediaType.VIDEO.code,
  val introVideoUrl: String = "",
  val introImageUrl: String = "",
  val introVideoDurationSeconds: Int = 0,
  val introVideoTitle: String = "",
  val introVideoMuted: Boolean = false,
  val introReelTargetType: String = IntroReelTargetType.PROMO_ONLY.code,
  val introReelTargetId: String = "",
  val introReelTargetLabel: String = "",
  val tickerEnabled: Boolean = false,
  val tickerText: String = "",
  val tickerPlacement: String = BannerPlacement.TOP_HOME.code,
  val tickerBackgroundColor: String = "#181A20",
  val tickerBorderColor: String = "#FFD000",
  val tickerTextColor: String = "#FFFFFF",
  val tickerFontSizeSp: Int = 14,
  val tickerFontWeight: String = TickerFontWeight.BOLD.code,
  val tickerItalic: Boolean = false,
  val tickerInlineImages: List<TickerInlineImage> = emptyList(),
  val appIconTheme: String = AppIconTheme.DEFAULT.code,
  val productCardBorderStyle: String = ProductCardBorderStyle.DEFAULT.code,
  val productCardBorderColor: String = "#D4AF37",
  val productCardShape: String = ProductCardShape.ROUNDED.code,
  val productDiscountBadgeStyle: String = ProductDiscountBadgeStyle.PILL.code,
  val privacyPolicyUrl: String = "",
  val returnPolicyUrl: String = "",
  val accountDeletionUrl: String = "",
  val privacyPolicyText: String = DEFAULT_PRIVACY_POLICY_TEXT,
  val returnPolicyText: String = DEFAULT_RETURN_POLICY_TEXT,
  val termsOfServiceText: String = DEFAULT_TERMS_OF_SERVICE_TEXT,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val appIconThemeEnum: AppIconTheme
    get() = AppIconTheme.fromCode(appIconTheme)

  @get:Exclude
  val introMediaTypeEnum: IntroMediaType
    get() = IntroMediaType.fromCode(introMediaType)

  @get:Exclude
  val productCardBorderStyleEnum: ProductCardBorderStyle
    get() = ProductCardBorderStyle.fromCode(productCardBorderStyle)

  @get:Exclude
  val productCardShapeEnum: ProductCardShape
    get() = ProductCardShape.fromCode(productCardShape)

  @get:Exclude
  val productDiscountBadgeStyleEnum: ProductDiscountBadgeStyle
    get() = ProductDiscountBadgeStyle.fromCode(productDiscountBadgeStyle)

  @get:Exclude
  val productCardStyleConfig: ProductCardStyleConfig
    get() = ProductCardStyleConfig(
      borderStyle = productCardBorderStyleEnum,
      customBorderHex = sanitizeCardHexColor(productCardBorderColor),
      shape = productCardShapeEnum,
      discountBadgeStyle = productDiscountBadgeStyleEnum,
    )

  @get:Exclude
  val announcementTickerConfig: AnnouncementTickerConfig
    get() = AnnouncementTickerConfig(
      enabled = tickerEnabled,
      text = tickerText,
      placement = BannerPlacement.fromCode(tickerPlacement),
      backgroundColorHex = sanitizeCardHexColor(tickerBackgroundColor, "#181A20"),
      borderColorHex = sanitizeCardHexColor(tickerBorderColor, "#FFD000"),
      textColorHex = sanitizeCardHexColor(tickerTextColor, "#FFFFFF"),
      fontSizeSp = tickerFontSizeSp.coerceIn(11, 24),
      fontWeight = TickerFontWeight.fromCode(tickerFontWeight),
      isItalic = tickerItalic,
      inlineImages = tickerInlineImages.filter { it.id.isNotBlank() && it.url.isNotBlank() }.take(10),
    )

  @get:Exclude
  val effectiveIntroMediaUrl: String
    get() = when (introMediaTypeEnum) {
      IntroMediaType.ANIMATED_IMAGE -> introImageUrl.trim()
      IntroMediaType.VIDEO -> introVideoUrl.trim()
    }

  @get:Exclude
  val hasActiveIntroVideo: Boolean
    get() = introVideoEnabled && effectiveIntroMediaUrl.length >= 5

  @get:Exclude
  val safeIntroVideoDurationSeconds: Int
    get() = introVideoDurationSeconds.coerceIn(0, 120)

  @get:Exclude
  val introReelTargetTypeEnum: IntroReelTargetType
    get() = IntroReelTargetType.fromCode(introReelTargetType)

  @get:Exclude
  val hasInteractiveIntroReelTarget: Boolean
    get() = introReelTargetTypeEnum != IntroReelTargetType.PROMO_ONLY &&
      introReelTargetId.trim().isNotEmpty()
  @get:Exclude
  val homeCategoryStyleEnum: HomeCategoryStyle
    get() = HomeCategoryStyle.fromCode(homeCategoryStyle)

  @get:Exclude
  val categoriesTabStyleEnum: CategoriesTabStyle
    get() = CategoriesTabStyle.fromCode(categoriesTabStyle)

  @get:Exclude
  val companyOfferBadgeStyleEnum: CompanyOfferBadgeStyle
    get() = CompanyOfferBadgeStyle.fromCode(companyOfferBadgeStyle)

  @get:Exclude
  val numeralFormatEnum: NumeralFormat
    get() = NumeralFormat.fromCode(numeralFormat)

  @get:Exclude
  val effectivePrivacyPolicyText: String
    get() = privacyPolicyText.trim().ifEmpty { DEFAULT_PRIVACY_POLICY_TEXT }

  @get:Exclude
  val effectiveReturnPolicyText: String
    get() = returnPolicyText.trim().ifEmpty { DEFAULT_RETURN_POLICY_TEXT }

  @get:Exclude
  val effectiveTermsOfServiceText: String
    get() = termsOfServiceText.trim().ifEmpty { DEFAULT_TERMS_OF_SERVICE_TEXT }

  companion object {
    val DEFAULT_PRIVACY_POLICY_TEXT = """
      نحن في متجر الإيمان للتجارة نحترم خصوصيتك ونلتزم بحماية بياناتك الشخصية وفقًا لأفضل معايير الأمان.

      ١. البيانات التي نجمعها عند استخدامك للتطبيق:
      • بيانات الحساب: الاسم الكامل، البريد الإلكتروني، وصورة الحساب عند تسجيل الدخول بحساب Google، بالإضافة إلى رقم الهاتف واسم النشاط التجاري (لحسابات الجملة).
      • بيانات التوصيل والطلبات: عناوين التوصيل التي تضيفها، وإحداثيات الموقع الجغرافي (GPS) عند اختيارك تحديد موقعك على الخريطة لتسهيل وصول المندوب، وسجل الطلبات وإيصالات التحويل المالية التي ترفعها طوعًا.
      • بيانات تقنية: رموز الإشعارات الخاصة بجهازك لإرسال تنبيهات حالة الطلب والعروض.

      ٢. أذونات الجهاز وكيفية استخدامها:
      • الكاميرا: تُستخدم فقط عند رغبتك في مسح الباركود أو رمز QR للبحث السريع عن المنتجات.
      • الموقع الجغرافي (اختياري): يُستخدم فقط عند ضغطك على زر تحديد الموقع الحالي لحفظ عنوان التوصيل بدقة، ولا يتم تتبع موقعك في الخلفية للعملاء.
      • الصور والوسائط: نستخدم منتقي الصور الآمن في النظام لإرفاق إيصال الدفع أو صور المنتجات دون طلب صلاحية الوصول لكامل ملفات الجهاز.
      • البلوتوث (للموظفين والإدارة فقط): يُستخدم فقط للاتصال بالطابعة الحرارية لطباعة فواتير تجهيز الطلبات.
      • الإشعارات: لإبلاغك بتحديثات حالة طلبك، ومراجعة الدفع، وحالة حساب الجملة.

      ٣. مشاركة البيانات وحمايتها:
      • يتم تشفير جميع البيانات أثناء النقل باستخدام بروتوكول HTTPS عبر خوادم Google Firebase.
      • لا نبيع بياناتك ولا نشاركها مع جهات إعلانية أو أطراف ثالثة. تُستخدم البيانات فقط لإتمام طلباتك وتوصيلها والتواصل معك بشأنها.

      ٤. حذف الحساب والبيانات (Account Deletion):
      • يمكنك في أي وقت حذف حسابك نهائيًا من داخل التطبيق عبر: حسابي ← حذف الحساب نهائيًا.
      • عند تأكيد الحذف يتم مسح ملفك الشخصي، عناوين التوصيل، سلة التسوق، إشعاراتك، وتقييماتك فورًا مع إزالة حساب الدخول الخاص بك.
      • قد يحتفظ المتجر بسجلات الفواتير والطلبات المكتملة للضرورة المحاسبية والقانونية فقط دون استخدامها لأغراض تسويقية.
    """.trimIndent()

    val DEFAULT_RETURN_POLICY_TEXT = """
      نحرص في متجر الإيمان للتجارة على رضاكم التام عن جميع منتجات المنظفات ومستلزمات العناية المنزلية سواء في طلبات القطاعي أو الجملة.

      ١. شروط الاسترجاع والاستبدال:
      • يحق للعميل طلب استبدال أو استرجاع المنتج خلال ١٤ يومًا من تاريخ الاستلام إذا كان بحالته الأصلية، مغلفًا بغلاف المصنع الأصلي، وغير مفتوح أو مستخدم.
      • في حال وجود تلف في العبوة أو خطأ في الصنف أو الكمية المسلمة، يتحمل المتجر كامل تكلفة الاستبدال أو رد القيمة فور الفحص.

      ٢. المنتجات غير القابلة للاسترجاع:
      • العبوات المفتوحة أو المستخدمة جزئيًا أو المنتجات السائبة بعد الاستلام والمطابقة، ما لم يثبت وجود عيب تصنيع ظاهر.

      ٣. استرداد المبالغ المالية:
      • عند إلغاء طلب قبل بدء التجهيز أو قبول طلب استرجاع مستوفٍ للشروط، يتم رد المبلغ بنفس وسيلة الدفع (نقدًا، إنستاباي، أو فودافون كاش) خلال ٢ إلى ٥ أيام عمل.
      • الطلبات بنظام «الاستلام من المتجر» يمكن مراجعتها وفحصها مباشرة عند الاستلام في الفرع.

      ٤. طريقة تقديم طلب الاسترجاع:
      • تواصل مع خدمة العملاء عبر الأرقام أو الواتساب الموضحين في صفحة «عن المتجر» مع ذكر رقم الطلب (مثل ET-1000).
    """.trimIndent()

    val DEFAULT_TERMS_OF_SERVICE_TEXT = """
      باستخدامك لتطبيق متجر الإيمان للتجارة فأنت توافق على الشروط والأحكام التالية:

      ١. الأسعار والطلبات:
      • تُعرض الأسعار بالجنيه المصري، وتُطبق أسعار القطاعي للعملاء الأفراد وأسعار الجملة للتجار المعتمدين وفق الحد الأدنى للكمية المحدد لكل منتج.
      • عروض الفلاش سيل تخضع للمدة الزمنية المعلنة والكمية المتاحة ولا تؤثر أسعارها على الكتالوج خارج وقت العرض.

      ٢. الدفع والتحويلات:
      • يتحمل العميل مسؤولية صحة بيانات الحوالة وإيصال الدفع المرفق عند اختيار الدفع الإلكتروني (إنستاباي أو فودافون كاش)، وتتم مراجعة الإيصال قبل الشحن.
      • في الدفع عند الاستلام أو عند الاستلام من المتجر يتم سداد إجمالي الفاتورة كاملة للمندوب أو في الفرع.

      ٣. التوصيل والاستلام:
      • يحق للعميل إلغاء الطلب ذاتيًا من شاشة «طلباتي» طالما كانت حالة الطلب «طلب جديد» أو «قيد المراجعة».
      • يلتزم العميل بإدخال رقم هاتف وعنوان صحيحين لتجنب تأخر أو إلغاء التوصيل.
    """.trimIndent()
  }
}

enum class NotificationType(val code: String, val arabicLabel: String) {
  ORDER_STATUS("order_status", "حالة الطلب"),
  PAYMENT_STATUS("payment_status", "مراجعة الدفع"),
  WHOLESALE_STATUS("wholesale_status", "حساب الجملة"),
  OFFER_PROMO("offer_promo", "عرض خاص"),
  STOCK_ALERT("stock_alert", "توفر المنتج"),
  GENERAL("general", "إشعار عام");

  companion object {
    fun fromCode(code: String?): NotificationType =
      entries.firstOrNull { it.code == code } ?: GENERAL
  }
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val message: String = "",
  val type: String = NotificationType.GENERAL.code,
  val referenceId: String? = null,
  @get:PropertyName("isRead") @PropertyName("isRead")
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  @get:Exclude
  val typeEnum: NotificationType
    get() = NotificationType.fromCode(type)
}

@Keep
@IgnoreExtraProperties
data class CashVerificationLog(
  @DocumentId val id: String = "",
  val transactionId: String = "",
  val providerName: String = "",
  val senderAddress: String = "",
  val senderPhone: String = "",
  val amount: Double = 0.0,
  val matchedOrderId: String? = null,
  val matchedOrderNumber: String? = null,
  val verifiedByDeviceName: String = "",
  val verifiedByEmail: String = "",
  @get:PropertyName("isSettled") @PropertyName("isSettled")
  val isSettled: Boolean = false,
  val withdrawalId: String? = null,
  val settledAt: Timestamp? = null,
  val status: String = "unmatched",
  val reason: String = "",
  val rawBodyPreview: String = "",
  val verifiedByUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  @get:Exclude
  val isVerifiedOperation: Boolean
    get() = status == "verified" || status == "matched"
}

@Keep
@IgnoreExtraProperties
data class CashVerifierAccount(
  @DocumentId val id: String = "",
  val email: String = "",
  val deviceName: String = "",
  val phoneNumber: String? = null,
  @get:PropertyName("isActive") @PropertyName("isActive")
  val isActive: Boolean = true,
  val lastResetAt: Timestamp? = null,
  val lastResetByName: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CashWithdrawalRecord(
  @DocumentId val id: String = "",
  val deviceKey: String = "",
  val deviceName: String = "",
  val deviceEmail: String = "",
  val withdrawnAmount: Double = 0.0,
  val operationsCount: Int = 0,
  val withdrawnByUid: String = "",
  val withdrawnByName: String = "",
  val withdrawnByEmail: String = "",
  val note: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

fun buildCashAccountDocId(email: String): String =
  email.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_")

@Keep
@IgnoreExtraProperties
data class StockAlert(
  @DocumentId val id: String = "",
  val productId: String = "",
  val productName: String = "",
  val userId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SearchMiss(
  @DocumentId val id: String = "",
  val query: String = "",
  val normalizedQuery: String = "",
  val missCount: Long = 1L,
  val lastSource: String = "android",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val lastSearchedAt: Timestamp? = null,
)

fun normalizeSearchQuery(raw: String): String =
  raw.trim().replace(Regex("\\s+"), " ").lowercase()

fun buildSearchMissDocId(normalizedQuery: String): String {
  var hash = 0x811c9dc5.toInt()
  normalizedQuery.forEach { ch ->
    hash = (hash xor ch.code) * 16777619
  }
  return "sm_${(hash.toLong() and 0xffffffffL).toString(36)}"
}


