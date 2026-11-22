package com.example

import com.example.data.model.BannerPlacement
import com.example.data.model.BrandShowcase
import com.example.data.model.CompanyOfferBadgeStyle
import com.example.data.model.Product
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.computeBannerlessShowcaseAlignLeftMap
import com.example.data.model.effectiveDiscountPercentWithShowcases
import com.example.data.model.effectiveRetailPriceWithShowcases
import com.example.data.model.effectiveUnitPriceWithShowcases
import com.example.data.model.isExclusiveHiddenFromStore
import com.example.data.model.matchesBrandShowcase
import com.example.ui.viewmodel.CatalogUiState
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandCompanyPricingTest {

  @Test
  fun matchesBrandShowcase_matchesByCompanyIdAndLegacyShowcaseId() {
    val showcase = BrandShowcase(id = "show_1", companyId = "comp_a")
    val byCompany = Product(id = "p1", companyId = "comp_a")
    val byLegacy = Product(id = "p2", brandShowcaseId = "show_1")
    val unrelated = Product(id = "p3", companyId = "comp_b", brandShowcaseId = "show_2")

    assertTrue(byCompany.matchesBrandShowcase(showcase))
    assertTrue(byLegacy.matchesBrandShowcase(showcase))
    assertFalse(unrelated.matchesBrandShowcase(showcase))
  }

  @Test
  fun isExclusiveHiddenFromStore_checksCompanyOrLegacyShowcaseLink() {
    val exclusiveCompany = Product(id = "p1", companyId = "comp_a", exclusiveToBrandShowcase = true)
    val exclusiveLegacy = Product(id = "p2", brandShowcaseId = "show_1", exclusiveToBrandShowcase = true)
    val exclusiveUnlinked = Product(id = "p3", exclusiveToBrandShowcase = true)
    val normalCompany = Product(id = "p4", companyId = "comp_a", exclusiveToBrandShowcase = false)

    assertTrue(exclusiveCompany.isExclusiveHiddenFromStore())
    assertTrue(exclusiveLegacy.isExclusiveHiddenFromStore())
    assertFalse(exclusiveUnlinked.isExclusiveHiddenFromStore())
    assertFalse(normalCompany.isExclusiveHiddenFromStore())
  }

  @Test
  fun computeBannerlessShowcaseAlignLeftMap_ignoresBanneredAndInactiveAndAlternatesGlobally() {
    val showcases = listOf(
      BrandShowcase(
        id = "s_bannered",
        placement = BannerPlacement.TOP_HOME.code,
        displayOrder = 0,
        bannerImageUrl = "https://example.com/banner.jpg",
        isActive = true,
      ),
      BrandShowcase(
        id = "s_inactive",
        placement = BannerPlacement.TOP_HOME.code,
        displayOrder = 1,
        bannerImageUrl = "",
        isActive = false,
      ),
      BrandShowcase(
        id = "s_second_placement",
        placement = BannerPlacement.BELOW_CATEGORIES.code,
        displayOrder = 0,
        bannerImageUrl = "",
        isActive = true,
        createdAt = Timestamp(100, 0),
      ),
      BrandShowcase(
        id = "s_first_placement",
        placement = BannerPlacement.TOP_HOME.code,
        displayOrder = 2,
        bannerImageUrl = "   ",
        isActive = true,
        createdAt = Timestamp(200, 0),
      ),
      BrandShowcase(
        id = "s_third_placement",
        placement = BannerPlacement.BELOW_DEALS.code,
        displayOrder = 1,
        bannerImageUrl = "",
        isActive = true,
        createdAt = Timestamp(300, 0),
      ),
    )

    val alignMap = computeBannerlessShowcaseAlignLeftMap(showcases)

    assertEquals(3, alignMap.size)
    assertFalse(alignMap.containsKey("s_bannered"))
    assertFalse(alignMap.containsKey("s_inactive"))
    assertEquals(false, alignMap["s_first_placement"])
    assertEquals(true, alignMap["s_second_placement"])
    assertEquals(false, alignMap["s_third_placement"])
  }

  @Test
  fun effectivePrices_applyBestShowcaseDiscountAndRespectWholesalePrecedence() {
    val product = Product(
      id = "p1",
      companyId = "comp_1",
      retailPrice = 200.0,
      retailDiscountPrice = 180.0, // 10% product discount
      wholesalePrice = 130.0,
      wholesaleMinQty = 5,
    )

    val showcases = listOf(
      BrandShowcase(id = "s1", companyId = "comp_1", discountPercent = 15.0, isActive = true),
      BrandShowcase(id = "s2", companyId = "comp_1", discountPercent = 25.0, isActive = true),
      BrandShowcase(id = "s3", companyId = "comp_1", discountPercent = 50.0, isActive = false),
      BrandShowcase(id = "s4", companyId = "comp_other", discountPercent = 40.0, isActive = true),
    )

    // Max active matching showcase discount is 25% -> 200 * 0.75 = 150.0 (beats 180.0)
    assertEquals(150.0, product.effectiveRetailPriceWithShowcases(showcases), 0.001)
    assertEquals(25, product.effectiveDiscountPercentWithShowcases(showcases))

    // Below wholesale min qty -> uses showcase-discounted retail price
    assertEquals(
      150.0,
      product.effectiveUnitPriceWithShowcases(
        isWholesaleApproved = true,
        quantity = 3,
        brandShowcases = showcases,
      ),
      0.001,
    )

    // Meets wholesale min qty & approved -> wholesale price takes precedence
    assertEquals(
      130.0,
      product.effectiveUnitPriceWithShowcases(
        isWholesaleApproved = true,
        quantity = 5,
        brandShowcases = showcases,
      ),
      0.001,
    )
  }

  @Test
  fun effectivePrices_keepProductDiscountWhenBetterThanShowcaseDiscount() {
    val product = Product(
      id = "p2",
      companyId = "comp_1",
      retailPrice = 100.0,
      retailDiscountPrice = 70.0, // 30% product discount
    )
    val showcases = listOf(
      BrandShowcase(id = "s1", companyId = "comp_1", discountPercent = 15.0, isActive = true),
    )

    assertEquals(70.0, product.effectiveRetailPriceWithShowcases(showcases), 0.001)
    assertEquals(30, product.effectiveDiscountPercentWithShowcases(showcases))
  }

  @Test
  fun companyOfferBadgeStyle_parsesAllFiveStylesAndDefaultsToGoldTicket() {
    assertEquals(5, CompanyOfferBadgeStyle.entries.size)
    CompanyOfferBadgeStyle.entries.forEach { style ->
      assertEquals(style, CompanyOfferBadgeStyle.fromCode(style.code))
    }
    assertEquals(CompanyOfferBadgeStyle.GOLD_TICKET, CompanyOfferBadgeStyle.fromCode(null))
    assertEquals(CompanyOfferBadgeStyle.GOLD_TICKET, CompanyOfferBadgeStyle.fromCode("unknown"))
    assertEquals(
      CompanyOfferBadgeStyle.ROYAL_SHIELD,
      StoreGeneralSettings(companyOfferBadgeStyle = "royal_shield").companyOfferBadgeStyleEnum,
    )
  }

  @Test
  fun catalogUiState_preventsHeroFlashWhileStoreSettingsAreLoading() {
    val initialLoading = CatalogUiState()
    assertTrue(initialLoading.isLoading)
    assertFalse(initialLoading.isStoreSettingsLoaded)

    val loaded = CatalogUiState(isLoading = false)
    assertTrue(loaded.isStoreSettingsLoaded)
  }
}
