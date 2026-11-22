package com.aleman.cash.sms

import com.aleman.cash.model.CashProviderConfig
import com.aleman.cash.model.ParsedCashSms
import java.util.Locale

sealed interface SmsParseResult {
  data class IgnoredUnknownOrDisabledSender(val senderAddress: String) : SmsParseResult
  data class IgnoredNotIncomingPayment(
    val provider: CashProviderConfig,
    val senderAddress: String,
    val reason: String
  ) : SmsParseResult

  data class Matched(val parsed: ParsedCashSms) : SmsParseResult
}

object SmsCashParser {

  fun normalizeDigits(input: String): String {
    if (input.isEmpty()) return input
    val sb = StringBuilder(input.length)
    for (ch in input) {
      val mapped = when (ch) {
        '٠', '۰' -> '0'
        '١', '۱' -> '1'
        '٢', '۲' -> '2'
        '٣', '۳' -> '3'
        '٤', '۴' -> '4'
        '٥', '۵' -> '5'
        '٦', '۶' -> '6'
        '٧', '۷' -> '7'
        '٨', '۸' -> '8'
        '٩', '۹' -> '9'
        '٫', '،' -> if (ch == '٫') '.' else ch
        else -> ch
      }
      sb.append(mapped)
    }
    return sb.toString()
  }

  fun normalizeEgyptianPhone(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val ascii = normalizeDigits(raw)
    val digitsOnly = buildString {
      for (ch in ascii) if (ch in '0'..'9') append(ch)
    }
    return when {
      digitsOnly.startsWith("00201") && digitsOnly.length == 14 -> "0" + digitsOnly.substring(4)
      digitsOnly.startsWith("201") && digitsOnly.length == 12 -> "0" + digitsOnly.substring(2)
      digitsOnly.startsWith("1") && digitsOnly.length == 10 -> "0$digitsOnly"
      else -> digitsOnly
    }
  }

  private fun normalizeSenderToken(value: String): String {
    val ascii = normalizeDigits(value.trim().lowercase(Locale.ROOT))
      .replace('أ', 'ا')
      .replace('إ', 'ا')
      .replace('آ', 'ا')
      .replace('ى', 'ي')
      .replace('ة', 'ه')
    return buildString {
      for (ch in ascii) {
        if (ch.isLetterOrDigit()) append(ch)
      }
    }
  }

  fun matchEnabledProvider(
    senderAddress: String,
    providers: List<CashProviderConfig>
  ): CashProviderConfig? {
    val cleanSenderRaw = normalizeDigits(senderAddress.trim().lowercase(Locale.ROOT))
    val cleanSenderCompact = normalizeSenderToken(senderAddress)
    if (cleanSenderRaw.isBlank()) return null

    val enabledProviders = providers.filter { it.isEnabled }
    for (provider in enabledProviders) {
      val candidateNames = (provider.senderAliases + provider.displayName)
        .map { it.trim() }
        .filter { it.length >= 2 }

      for (alias in candidateNames) {
        val aliasRaw = normalizeDigits(alias.lowercase(Locale.ROOT))
        val aliasCompact = normalizeSenderToken(alias)
        if (aliasCompact.length < 2) continue

        if (cleanSenderRaw == aliasRaw || cleanSenderCompact == aliasCompact) {
          return provider
        }
        // Handle short sender IDs like "VF-Cash" inside "+VF-Cash" or header strings
        if (cleanSenderCompact.contains(aliasCompact) ||
          (cleanSenderCompact.length >= 3 && aliasCompact.contains(cleanSenderCompact))
        ) {
          return provider
        }
      }
    }
    return null
  }

  private val incomingReceiptRegex = Regex(
    "(?:تم\\s*(?:استلام|إستلام|ايداع|إيداع|تحصيل)|استلمت|وصلك|تحويل\\s*وارد)"
  )

  private val outgoingRejectRegex = Regex(
    "(?:تم\\s*(?:تحويل\\s+مبلغ\\s+.*\\s*(?:إلى|الى)|سحب|دفع\\s+فاتورة|شحن\\s+رصيد|خصم)|رصيدك\\s+لا\\s+يكفي)"
  )

  // Strict incoming amount pattern BEFORE any mention of "رصيدك"
  private val primaryAmountRegex = Regex(
    "(?:تم\\s*(?:استلام|إستلام|ايداع|إيداع|تحصيل)|استلمت|وصلك)\\s*(?:حوالة\\s*)?(?:ب?مبلغ)?\\s*[:：]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:جنيه|ج\\.?م|جم|egp)?",
    RegexOption.IGNORE_CASE
  )

  private val secondaryAmountRegex = Regex(
    "(?:مبلغ|قيمة)\\s*[:：]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:جنيه|ج\\.?م|جم|egp)?",
    RegexOption.IGNORE_CASE
  )

  private val senderPhoneLabeledRegex = Regex(
    "(?:من\\s*(?:رقم|محفظة|الرقم|حساب)|المرسل|رقم\\s*الراسل)\\s*[:：]?\\s*(\\+?2?0?0?1[0125][0-9\\-\\s]{8,11})"
  )

  private val walletOwnerClauseRegex = Regex(
    "(?:على\\s*(?:رقم\\s*)?محفظتك|إلى\\s*(?:رقم\\s*)?محفظتك|الى\\s*(?:رقم\\s*)?محفظتك|على\\s*محفظة)"
  )

  private val anyEgyptPhoneRegex = Regex("(?:\\+?20|0020)?0?1[0125][0-9]{8}")

  private val transactionIdRegex = Regex(
    "(?:رقم\\s*(?:العملية|المعاملة|التحويل|المرجع)|الرقم\\s*المرجعي|كود\\s*العملية|مرجع(?:\\s*رقم)?|Ref(?:erence)?\\s*(?:No|ID)?)\\s*[:：#]?\\s*([A-Za-z0-9\\-_]{5,28})",
    RegexOption.IGNORE_CASE
  )

  fun parseIncomingPaymentSms(
    senderAddress: String,
    rawBody: String,
    providers: List<CashProviderConfig>,
    receivedAtMillis: Long = System.currentTimeMillis()
  ): SmsParseResult {
    val provider = matchEnabledProvider(senderAddress, providers)
      ?: return SmsParseResult.IgnoredUnknownOrDisabledSender(senderAddress)

    val body = normalizeDigits(rawBody)
      .replace('أ', 'ا')
      .replace('إ', 'ا')
      .replace('آ', 'ا')
      .replace('\u00A0', ' ')
      .trim()

    val isIncoming = incomingReceiptRegex.containsMatchIn(body)
    if (!isIncoming) {
      return SmsParseResult.IgnoredNotIncomingPayment(
        provider = provider,
        senderAddress = senderAddress,
        reason = if (outgoingRejectRegex.containsMatchIn(body)) {
          "رسالة صادر/سحب وليست استلام مبلغ"
        } else {
          "رسالة إعلامية أو غير مرتبطة باستلام تحويل"
        }
      )
    }

    // Isolate the section BEFORE "رصيدك الحالي" so balance numbers are NEVER parsed as payment amount
    val beforeBalanceSection = body
      .split(Regex("رصيدك\\s*(?:الحالي)?|الرصيد\\s*(?:الحالي)?|تاريخ\\s*العملية|رقم\\s*العملية"))
      .firstOrNull()
      ?: body

    val amountStr = primaryAmountRegex.find(beforeBalanceSection)?.groupValues?.getOrNull(1)
      ?: secondaryAmountRegex.find(beforeBalanceSection)?.groupValues?.getOrNull(1)
      ?: primaryAmountRegex.find(body)?.groupValues?.getOrNull(1)

    val amount = amountStr
      ?.replace(',', '.')
      ?.toDoubleOrNull()
      ?.takeIf { it > 0.0 }
      ?: return SmsParseResult.IgnoredNotIncomingPayment(
        provider = provider,
        senderAddress = senderAddress,
        reason = "تعذر استخراج قيمة المبلغ المستلم بدقة"
      )

    // Extract sender phone before "على رقم محفظتك" to avoid grabbing recipient's own wallet number
    val beforeRecipientWallet = beforeBalanceSection
      .split(walletOwnerClauseRegex)
      .firstOrNull()
      ?: beforeBalanceSection

    val rawPhoneCandidate = senderPhoneLabeledRegex.find(body)?.groupValues?.getOrNull(1)
      ?: anyEgyptPhoneRegex.find(beforeRecipientWallet)?.value

    val senderPhone = normalizeEgyptianPhone(rawPhoneCandidate)
    if (senderPhone.length != 11 || !senderPhone.startsWith("01")) {
      return SmsParseResult.IgnoredNotIncomingPayment(
        provider = provider,
        senderAddress = senderAddress,
        reason = "تعذر استخراج رقم الهاتف المحوَّل منه من نص الرسالة"
      )
    }

    val txId = transactionIdRegex.find(body)?.groupValues?.getOrNull(1)?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: "AUTO-$senderPhone-${String.format(Locale.US, "%.2f", amount)}-${receivedAtMillis / 60_000}"

    return SmsParseResult.Matched(
      ParsedCashSms(
        provider = provider,
        rawSenderAddress = senderAddress,
        amount = amount,
        senderPhone = senderPhone,
        transactionId = txId,
        rawBody = rawBody.trim()
      )
    )
  }
}
