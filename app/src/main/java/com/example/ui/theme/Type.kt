package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val TajawalFontFamily = FontFamily(
  Font(R.font.tajawal_light, FontWeight.Light),
  Font(R.font.tajawal_regular, FontWeight.Normal),
  Font(R.font.tajawal_medium, FontWeight.Medium),
  Font(R.font.tajawal_medium, FontWeight.SemiBold),
  Font(R.font.tajawal_bold, FontWeight.Bold),
  Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
)

// Maintained as alias for backward compatibility so every reference resolves to Tajawal
val CairoFontFamily = TajawalFontFamily

val AlEmanTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 40.sp,
  ),
  displayMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp,
  ),
  displaySmall = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 32.sp,
  ),
  headlineLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 32.sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 26.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 24.sp,
  ),
  titleSmall = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 22.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = TajawalFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
  ),
)
