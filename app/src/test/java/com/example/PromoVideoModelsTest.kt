package com.example

import com.example.data.media.VideoDiskCache
import com.example.data.model.AppIconTheme
import com.example.data.model.Banner
import com.example.data.model.BannerMediaType
import com.example.data.model.BannerPlacement
import com.example.data.model.BannerStyle
import com.example.data.model.IntroMediaType
import com.example.data.model.ProductCardBorderStyle
import com.example.data.model.ProductCardShape
import com.example.data.model.ProductDiscountBadgeStyle
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.TickerFontWeight
import com.example.data.model.TickerInlineImage
import com.example.data.model.deriveVideoPosterUrl
import com.example.data.model.sanitizeCardHexColor
import com.example.ui.components.TickerSegment
import com.example.ui.components.parseTickerSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PromoVideoModelsTest {

  @Test
  fun bannerMediaType_defaultsToImageAndParsesVideo() {
    assertEquals(BannerMediaType.IMAGE, BannerMediaType.fromCode(null))
    assertEquals(BannerMediaType.IMAGE, BannerMediaType.fromCode("unknown"))
    assertEquals(BannerMediaType.VIDEO, BannerMediaType.fromCode("video"))
  }

  @Test
  fun banner_isVideoBannerRequiresVideoTypeAndValidUrl() {
    val validVideoBanner = Banner(
      id = "b1",
      mediaType = BannerMediaType.VIDEO.code,
      videoUrl = "https://res.cloudinary.com/demo/video/upload/v1/promo.mp4",
      durationSeconds = 25,
    )
    val missingUrlBanner = Banner(
      id = "b2",
      mediaType = BannerMediaType.VIDEO.code,
      videoUrl = "",
    )
    val imageBanner = Banner(
      id = "b3",
      mediaType = BannerMediaType.IMAGE.code,
      imageUrl = "https://res.cloudinary.com/demo/image/upload/v1/b.jpg",
      videoUrl = "https://res.cloudinary.com/demo/video/upload/v1/ignored.mp4",
    )

    assertTrue(validVideoBanner.isVideoBanner)
    assertFalse(missingUrlBanner.isVideoBanner)
    assertFalse(imageBanner.isVideoBanner)
    assertEquals(25, validVideoBanner.safeDurationSeconds)
  }

  @Test
  fun deriveVideoPosterUrl_convertsCloudinaryVideoUrlToJpgAndPreservesExplicitImage() {
    val videoUrl = "https://res.cloudinary.com/dh9lxflpd/video/upload/v123/aleman/videos/clip.mp4"
    assertEquals(
      "https://res.cloudinary.com/dh9lxflpd/video/upload/v123/aleman/videos/clip.jpg",
      deriveVideoPosterUrl(videoUrl),
    )
    assertEquals("", deriveVideoPosterUrl("https://example.com/custom.mp4"))

    val autoPosterBanner = Banner(
      mediaType = BannerMediaType.VIDEO.code,
      videoUrl = videoUrl,
      imageUrl = "",
    )
    assertEquals(
      "https://res.cloudinary.com/dh9lxflpd/video/upload/v123/aleman/videos/clip.jpg",
      autoPosterBanner.effectivePosterUrl,
    )

    val customPosterBanner = autoPosterBanner.copy(imageUrl = "https://cdn.example.com/custom.webp")
    assertEquals("https://cdn.example.com/custom.webp", customPosterBanner.effectivePosterUrl)
  }

  @Test
  fun banner_durationClampsTo120Seconds() {
    assertEquals(1, Banner(durationSeconds = 0).safeDurationSeconds)
    assertEquals(90, Banner(durationSeconds = 90).safeDurationSeconds)
    assertEquals(120, Banner(durationSeconds = 300).safeDurationSeconds)
  }

  @Test
  fun storeGeneralSettings_introVideoActiveAndDurationClamping() {
    val disabled = StoreGeneralSettings(
      introVideoEnabled = false,
      introVideoUrl = "https://res.cloudinary.com/demo/video/upload/v1/reel.mp4",
      introVideoDurationSeconds = 18,
    )
    assertFalse(disabled.hasActiveIntroVideo)

    val enabledNoUrl = StoreGeneralSettings(
      introVideoEnabled = true,
      introVideoUrl = "   ",
    )
    assertFalse(enabledNoUrl.hasActiveIntroVideo)

    val active = StoreGeneralSettings(
      introVideoEnabled = true,
      introVideoUrl = "https://res.cloudinary.com/demo/video/upload/v1/reel.mp4",
      introVideoDurationSeconds = 180,
    )
    assertTrue(active.hasActiveIntroVideo)
    assertEquals(120, active.safeIntroVideoDurationSeconds)
    assertEquals(IntroMediaType.VIDEO, active.introMediaTypeEnum)
    assertEquals("https://res.cloudinary.com/demo/video/upload/v1/reel.mp4", active.effectiveIntroMediaUrl)

    val animatedIntro = StoreGeneralSettings(
      introVideoEnabled = true,
      introMediaType = IntroMediaType.ANIMATED_IMAGE.code,
      introImageUrl = "https://res.cloudinary.com/demo/image/upload/v1/intro.gif",
      introVideoDurationSeconds = 6,
    )
    assertEquals(IntroMediaType.ANIMATED_IMAGE, animatedIntro.introMediaTypeEnum)
    assertTrue(animatedIntro.hasActiveIntroVideo)
    assertEquals("https://res.cloudinary.com/demo/image/upload/v1/intro.gif", animatedIntro.effectiveIntroMediaUrl)

    val animatedIntroMissingUrl = animatedIntro.copy(introImageUrl = "")
    assertFalse(animatedIntroMissingUrl.hasActiveIntroVideo)
  }

  @Test
  fun videoDiskCache_deterministicFileNameAndPrunesReplacedVideos() {
    val urlA = "https://res.cloudinary.com/demo/video/upload/v1/promo_old.mp4"
    val urlB = "https://res.cloudinary.com/demo/video/upload/v2/promo_new.mp4"
    val fileA = VideoDiskCache.cacheFileNameForUrl(urlA)
    val fileB = VideoDiskCache.cacheFileNameForUrl(urlB)

    assertEquals(fileA, VideoDiskCache.cacheFileNameForUrl("  $urlA  "))
    assertNotEquals(fileA, fileB)
    assertTrue(fileA.endsWith(".mp4"))

    val tempDir = Files.createTempDirectory("video_cache_test").toFile()
    try {
      val cachedOld = File(tempDir, fileA).apply { writeBytes(ByteArray(2048)) }
      val cachedNew = File(tempDir, fileB).apply { writeBytes(ByteArray(2048)) }

      VideoDiskCache.pruneStaleFiles(tempDir, setOf(fileB))

      assertFalse("Old replaced video should be deleted from cache", cachedOld.exists())
      assertTrue("Active video should remain cached on device", cachedNew.exists())
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun animatedImageBanner_supportsNormalAndSlimWithoutBreakingVideo() {
    assertEquals(BannerMediaType.ANIMATED_IMAGE, BannerMediaType.fromCode("animated_image"))

    val normalAnim = Banner(
      id = "anim_1",
      mediaType = BannerMediaType.ANIMATED_IMAGE.code,
      imageUrl = "https://res.cloudinary.com/demo/image/upload/v1/promo.webp",
      bannerStyle = BannerStyle.NORMAL.code,
    )
    val slimAnim = Banner(
      id = "anim_2",
      mediaType = BannerMediaType.ANIMATED_IMAGE.code,
      imageUrl = "https://res.cloudinary.com/demo/image/upload/v1/strip.gif",
      bannerStyle = BannerStyle.SLIM.code,
    )
    val missingAnim = Banner(
      id = "anim_3",
      mediaType = BannerMediaType.ANIMATED_IMAGE.code,
      imageUrl = "",
    )

    assertTrue(normalAnim.isAnimatedImageBanner)
    assertFalse(normalAnim.isVideoBanner)
    assertEquals(BannerStyle.NORMAL, normalAnim.bannerStyleEnum)

    assertTrue(slimAnim.isAnimatedImageBanner)
    assertEquals(BannerStyle.SLIM, slimAnim.bannerStyleEnum)

    assertFalse(missingAnim.isAnimatedImageBanner)
  }

  @Test
  fun appIconTheme_mapsAllSeasonalVariantsAndDefaultsSafely() {
    assertEquals(AppIconTheme.DEFAULT, AppIconTheme.fromCode(null))
    assertEquals(AppIconTheme.DEFAULT, AppIconTheme.fromCode("invalid"))
    assertEquals(AppIconTheme.OFFERS, AppIconTheme.fromCode("offers"))
    assertEquals(AppIconTheme.RAMADAN, AppIconTheme.fromCode("ramadan"))
    assertEquals(AppIconTheme.EID, AppIconTheme.fromCode("eid"))

    assertEquals(AppIconTheme.DEFAULT, StoreGeneralSettings().appIconThemeEnum)
    assertEquals(AppIconTheme.RAMADAN, StoreGeneralSettings(appIconTheme = "ramadan").appIconThemeEnum)
    assertEquals(AppIconTheme.EID, StoreGeneralSettings(appIconTheme = "eid").appIconThemeEnum)
  }

  @Test
  fun productCardStyleConfig_mapsBordersShapesBadgesAndSanitizesHex() {
    assertEquals(ProductCardBorderStyle.DEFAULT, ProductCardBorderStyle.fromCode(null))
    assertEquals(ProductCardBorderStyle.GOLD, ProductCardBorderStyle.fromCode("gold"))
    assertEquals(ProductCardBorderStyle.CUSTOM, ProductCardBorderStyle.fromCode("custom"))

    assertEquals(ProductCardShape.ROUNDED, ProductCardShape.fromCode("unknown"))
    assertEquals(ProductCardShape.CIRCULAR, ProductCardShape.fromCode("circular"))
    assertEquals(ProductCardShape.RECTANGLE, ProductCardShape.fromCode("rectangle"))
    assertEquals(ProductCardShape.CUT_CORNER, ProductCardShape.fromCode("cut_corner"))
    assertEquals(ProductCardShape.ASYMMETRIC, ProductCardShape.fromCode("asymmetric"))
    assertEquals(ProductCardShape.ARCH, ProductCardShape.fromCode("arch"))

    assertEquals(ProductDiscountBadgeStyle.PILL, ProductDiscountBadgeStyle.fromCode("invalid"))
    assertEquals(ProductDiscountBadgeStyle.RIBBON, ProductDiscountBadgeStyle.fromCode("ribbon"))
    assertEquals(ProductDiscountBadgeStyle.STARBURST, ProductDiscountBadgeStyle.fromCode("starburst"))
    assertEquals(ProductDiscountBadgeStyle.PRICE_TAG, ProductDiscountBadgeStyle.fromCode("price_tag"))
    assertEquals(ProductDiscountBadgeStyle.GOLD_LUXURY, ProductDiscountBadgeStyle.fromCode("gold_luxury"))
    assertEquals(ProductDiscountBadgeStyle.CAPSULE_OUTLINE, ProductDiscountBadgeStyle.fromCode("capsule_outline"))

    assertEquals("#D4AF37", sanitizeCardHexColor("d4af37"))
    assertEquals("#FF6B00", sanitizeCardHexColor(" #ff6b00 "))
    assertEquals("#D4AF37", sanitizeCardHexColor("not-a-color"))

    val customSettings = StoreGeneralSettings(
      productCardBorderStyle = "gold",
      productCardBorderColor = "a1b2c3",
      productCardShape = "cut_corner",
      productDiscountBadgeStyle = "starburst",
    )
    val cfg = customSettings.productCardStyleConfig
    assertEquals(ProductCardBorderStyle.GOLD, cfg.borderStyle)
    assertEquals("#A1B2C3", cfg.safeCustomBorderHex)
    assertEquals(ProductCardShape.CUT_CORNER, cfg.shape)
    assertEquals(ProductDiscountBadgeStyle.STARBURST, cfg.discountBadgeStyle)
  }

  @Test
  fun videoAudioFlags_defaultToWithSoundAndPersistMutedState() {
    val defaultBanner = Banner(
      mediaType = BannerMediaType.VIDEO.code,
      videoUrl = "https://res.cloudinary.com/demo/video/upload/v1/ad.mp4",
    )
    assertFalse(defaultBanner.videoMuted)

    val mutedBanner = defaultBanner.copy(videoMuted = true)
    assertTrue(mutedBanner.videoMuted)

    val defaultSettings = StoreGeneralSettings()
    assertFalse(defaultSettings.introVideoMuted)

    val mutedIntroSettings = defaultSettings.copy(introVideoMuted = true)
    assertTrue(mutedIntroSettings.introVideoMuted)
  }

  @Test
  fun announcementTickerConfig_mapsStylePlacementClampsSizeAndParsesInlineImages() {
    assertEquals(TickerFontWeight.BOLD, TickerFontWeight.fromCode(null))
    assertEquals(TickerFontWeight.LIGHT, TickerFontWeight.fromCode("light"))
    assertEquals(TickerFontWeight.NORMAL, TickerFontWeight.fromCode("normal"))
    assertEquals(TickerFontWeight.EXTRA_BOLD, TickerFontWeight.fromCode("extra_bold"))

    val img1 = TickerInlineImage(id = "img1", url = "https://cdn.example.com/1.png")
    val img2 = TickerInlineImage(id = "img2", url = "https://cdn.example.com/2.png")

    val settings = StoreGeneralSettings(
      tickerEnabled = true,
      tickerText = "🔥 عروض [[img:img1]] اليوم [[img:missing]] خصم [[img:img2]]!",
      tickerPlacement = BannerPlacement.BELOW_CATEGORIES.code,
      tickerBackgroundColor = "1565c0",
      tickerBorderColor = " #ffd000 ",
      tickerTextColor = "invalid",
      tickerFontSizeSp = 40,
      tickerFontWeight = "extra_bold",
      tickerItalic = true,
      tickerInlineImages = listOf(img1, img2),
    )

    val config = settings.announcementTickerConfig
    assertTrue(config.isActive)
    assertEquals(BannerPlacement.BELOW_CATEGORIES, config.placement)
    assertEquals("#1565C0", config.backgroundColorHex)
    assertEquals("#FFD000", config.borderColorHex)
    assertEquals("#FFFFFF", config.textColorHex)
    assertEquals(24, config.safeFontSizeSp)
    assertEquals(TickerFontWeight.EXTRA_BOLD, config.fontWeight)
    assertTrue(config.isItalic)

    val segments = parseTickerSegments(config.text, config.inlineImages)
    assertEquals(6, segments.size)
    assertEquals(TickerSegment.TextSegment("🔥 عروض "), segments[0])
    assertEquals(TickerSegment.ImageSegment("img1", "https://cdn.example.com/1.png"), segments[1])
    assertEquals(TickerSegment.TextSegment(" اليوم "), segments[2])
    assertEquals(TickerSegment.TextSegment(" خصم "), segments[3])
    assertEquals(TickerSegment.ImageSegment("img2", "https://cdn.example.com/2.png"), segments[4])
    assertEquals(TickerSegment.TextSegment("!"), segments[5])

    val disabled = settings.copy(tickerEnabled = false).announcementTickerConfig
    assertFalse(disabled.isActive)
  }
}
