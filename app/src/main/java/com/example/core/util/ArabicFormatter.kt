package com.example.core.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.NumeralFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * منسق الأرقام والعملة بالجنيه المصري مع دعم التبديل بين الأرقام العربية (١٢٣) والإنجليزية (123).
 */
object ArabicFormatter {
  private var _numeralFormat by mutableStateOf(NumeralFormat.ARABIC)
  val numeralFormat: NumeralFormat
    get() = _numeralFormat

  fun setNumeralFormat(format: NumeralFormat) {
    _numeralFormat = format
  }

  fun formatCurrency(amount: Double, format: NumeralFormat = numeralFormat): String {
    val usSymbols = DecimalFormatSymbols(Locale.US)
    val pattern = if (amount % 1.0 == 0.0) "#,##0" else "#,##0.00"
    val formatted = DecimalFormat(pattern, usSymbols).format(amount)
    val localized = when (format) {
      NumeralFormat.ARABIC -> toEasternArabicDigits(formatted, convertSeparators = true)
      NumeralFormat.ENGLISH -> formatted
    }
    return "$localized ج.م"
  }

  fun formatNumber(value: Number, format: NumeralFormat = numeralFormat): String {
    val raw = value.toString()
    return when (format) {
      NumeralFormat.ARABIC -> toEasternArabicDigits(raw, convertSeparators = false)
      NumeralFormat.ENGLISH -> raw
    }
  }

  fun formatDigits(text: String, format: NumeralFormat = numeralFormat): String {
    return when (format) {
      NumeralFormat.ARABIC -> toEasternArabicDigits(text, convertSeparators = false)
      NumeralFormat.ENGLISH -> toWesternDigits(text)
    }
  }

  private fun toEasternArabicDigits(input: String, convertSeparators: Boolean): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
      sb.append(
        when (ch) {
          in '0'..'9' -> ('٠'.code + (ch - '0')).toChar()
          ',' -> if (convertSeparators) '٬' else ch
          '.' -> if (convertSeparators) '٫' else ch
          else -> ch
        },
      )
    }
    return sb.toString()
  }

  private fun toWesternDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
      sb.append(
        when (ch) {
          in '٠'..'٩' -> ('0'.code + (ch - '٠')).toChar()
          '٬' -> ','
          '٫' -> '.'
          else -> ch
        },
      )
    }
    return sb.toString()
  }
}
