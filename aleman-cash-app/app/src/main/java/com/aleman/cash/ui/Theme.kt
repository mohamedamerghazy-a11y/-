package com.aleman.cash.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aleman.cash.R

object AlemanCashColors {
  val Navy = Color(0xFF0F172A)
  val NavySoft = Color(0xFF1E293B)
  val Emerald = Color(0xFF059669)
  val EmeraldDark = Color(0xFF065F46)
  val EmeraldTint = Color(0xFFECFDF5)
  val Gold = Color(0xFFFFD000)
  val GoldDark = Color(0xFFD97706)
  val GoldTint = Color(0xFFFEF3C7)
  val SlateBg = Color(0xFFF8FAFC)
  val SurfaceWhite = Color(0xFFFFFFFF)
  val BorderLight = Color(0xFFE2E8F0)
  val TextMuted = Color(0xFF64748B)
  val DangerRed = Color(0xFFDC2626)
  val DangerTint = Color(0xFFFEF2F2)
}

val CairoFontFamily = FontFamily(
  Font(R.font.cairo, FontWeight.Normal),
  Font(R.font.cairo, FontWeight.Medium),
  Font(R.font.cairo, FontWeight.SemiBold),
  Font(R.font.cairo, FontWeight.Bold)
)

val TajawalFontFamily = FontFamily(
  Font(R.font.tajawal, FontWeight.Normal),
  Font(R.font.tajawal, FontWeight.Medium),
  Font(R.font.tajawal, FontWeight.Bold)
)

private val AlemanCashLightColorScheme = lightColorScheme(
  primary = AlemanCashColors.EmeraldDark,
  onPrimary = Color.White,
  primaryContainer = AlemanCashColors.EmeraldTint,
  onPrimaryContainer = AlemanCashColors.EmeraldDark,
  secondary = AlemanCashColors.GoldDark,
  onSecondary = Color.White,
  secondaryContainer = AlemanCashColors.GoldTint,
  onSecondaryContainer = AlemanCashColors.Navy,
  tertiary = AlemanCashColors.Navy,
  onTertiary = Color.White,
  background = AlemanCashColors.SlateBg,
  onBackground = AlemanCashColors.Navy,
  surface = AlemanCashColors.SurfaceWhite,
  onSurface = AlemanCashColors.Navy,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = AlemanCashColors.TextMuted,
  outline = AlemanCashColors.BorderLight,
  error = AlemanCashColors.DangerRed,
  errorContainer = AlemanCashColors.DangerTint,
  onErrorContainer = AlemanCashColors.DangerRed
)

private val AlemanCashTypography = Typography(
  headlineSmall = TextStyle(
    fontFamily = CairoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 30.sp
  ),
  titleLarge = TextStyle(
    fontFamily = CairoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 26.sp
  ),
  titleMedium = TextStyle(
    fontFamily = CairoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  labelLarge = TextStyle(
    fontFamily = CairoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  labelSmall = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp
  )
)

private val AlemanCashShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp)
)

@Composable
fun AlemanCashTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = AlemanCashLightColorScheme,
    typography = AlemanCashTypography,
    shapes = AlemanCashShapes,
    content = content
  )
}
