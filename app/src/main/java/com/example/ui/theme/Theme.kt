package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * ثوابت المسافات والزوايا الموحدة لنظام التصميم (Al-Eman Design System).
 */
object AlEmanDimens {
  val SpaceXs = 4.dp
  val SpaceSm = 8.dp
  val SpaceMd = 12.dp
  val SpaceLg = 16.dp
  val SpaceXl = 24.dp

  val RadiusSm = 8.dp
  val RadiusMd = 12.dp
  val RadiusLg = 16.dp

  val ButtonHeight = 48.dp
  val SearchBarHeight = 46.dp
  val BorderWidth = 1.dp
  val CardElevation = 0.dp
}

private val OfficialLightColorScheme = lightColorScheme(
  primary = RoyalBlue,
  onPrimary = PureWhite,
  primaryContainer = RoyalBlueSoft,
  onPrimaryContainer = RoyalBlue,
  secondary = GoldenYellow,
  onSecondary = TextPrimary,
  secondaryContainer = GoldenYellowSoft,
  onSecondaryContainer = TextPrimary,
  tertiary = SuccessGreen,
  onTertiary = PureWhite,
  tertiaryContainer = SuccessGreenSoft,
  onTertiaryContainer = SuccessGreen,
  background = GeneralBackground,
  onBackground = TextPrimary,
  surface = PureWhite,
  onSurface = TextPrimary,
  surfaceVariant = GeneralBackground,
  onSurfaceVariant = TextSecondary,
  outline = BorderNeutral,
  error = DiscountRed,
  onError = PureWhite,
  errorContainer = DiscountRedSoft,
  onErrorContainer = DiscountRed,
)

private val OfficialDarkColorScheme = darkColorScheme(
  primary = RoyalBlueNight,
  onPrimary = RoyalBlueDark,
  primaryContainer = RoyalBlueNightContainer,
  onPrimaryContainer = PureWhite,
  secondary = GoldenYellowNight,
  onSecondary = TextPrimary,
  secondaryContainer = ColorTokens.GoldenDarkContainer,
  onSecondaryContainer = GoldenYellowSoft,
  tertiary = SuccessGreen,
  onTertiary = PureWhite,
  background = BackgroundDark,
  onBackground = TextPrimaryNight,
  surface = SurfaceDark,
  onSurface = TextPrimaryNight,
  surfaceVariant = SurfaceDark,
  onSurfaceVariant = TextSecondaryNight,
  outline = BorderDark,
  error = DiscountRed,
  onError = PureWhite,
)

private object ColorTokens {
  val GoldenDarkContainer = androidx.compose.ui.graphics.Color(0xFF473700)
}

val AlEmanShapes = Shapes(
  small = RoundedCornerShape(AlEmanDimens.RadiusSm),
  medium = RoundedCornerShape(AlEmanDimens.RadiusMd),
  large = RoundedCornerShape(AlEmanDimens.RadiusLg),
  extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun AlEmanTradeTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) OfficialDarkColorScheme else OfficialLightColorScheme

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AlEmanTypography,
      shapes = AlEmanShapes,
      content = content,
    )
  }
}
