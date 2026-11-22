package com.example.cashpreview

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class CashPreviewProviderConfig(
  val id: String,
  val displayName: String,
  val senderAliases: List<String>,
  val isEnabled: Boolean = true,
  val isBuiltIn: Boolean = false,
)

@Serializable
data class CashPreviewSettings(
  val providers: List<CashPreviewProviderConfig> = defaultCashPreviewProviders(),
  val autoVerifyEnabled: Boolean = true,
  val targetOrderStatusAfterPayment: String = "preparing",
)

fun defaultCashPreviewProviders(): List<CashPreviewProviderConfig> = listOf(
  CashPreviewProviderConfig(
    id = "vodafone_cash",
    displayName = "فودافون كاش",
    senderAliases = listOf("VF-Cash", "VodafoneCash", "Vodafone", "فودافون كاش", "فودافون"),
    isEnabled = true,
    isBuiltIn = true,
  ),
  CashPreviewProviderConfig(
    id = "orange_cash",
    displayName = "أورنج كاش",
    senderAliases = listOf("OrangeCash", "Orange Money", "Orange", "أورنج كاش", "اورنج كاش"),
    isEnabled = true,
    isBuiltIn = true,
  ),
  CashPreviewProviderConfig(
    id = "etisalat_cash",
    displayName = "اتصالات كاش",
    senderAliases = listOf("EtisalatCash", "Etisalat", "e& money", "e&", "اتصالات كاش", "اتصالات"),
    isEnabled = true,
    isBuiltIn = true,
  ),
  CashPreviewProviderConfig(
    id = "we_cash",
    displayName = "وي كاش",
    senderAliases = listOf("WECash", "WE Pay", "WE", "وي كاش", "وى كاش"),
    isEnabled = true,
    isBuiltIn = true,
  ),
)

data class ParsedCashPreviewSms(
  val provider: CashPreviewProviderConfig,
  val rawSenderAddress: String,
  val amount: Double,
  val senderPhone: String,
  val transactionId: String,
  val rawBody: String,
)

data class PendingTransferOrderPreview(
  val orderId: String,
  val orderNumber: String,
  val userId: String,
  val customerName: String,
  val customerPhone: String,
  val paymentMethod: String,
  val paymentStatus: String,
  val orderStatus: String,
  val senderPhone: String,
  val transferReference: String,
  val totalAmount: Double,
  val createdAtMillis: Long,
) {
  val effectiveSenderPhone: String
    get() = senderPhone.ifBlank { transferReference }
}

data class CashVerificationLogPreview(
  val id: String,
  val transactionId: String,
  val providerName: String,
  val senderAddress: String,
  val senderPhone: String,
  val amount: Double,
  val matchedOrderId: String?,
  val matchedOrderNumber: String?,
  val status: String,
  val reason: String,
  val rawBodyPreview: String,
  val createdAtMillis: Long,
)

data class LocalVerificationEventPreview(
  val id: String,
  val timestampMillis: Long,
  val senderAddress: String,
  val providerName: String,
  val amount: Double?,
  val senderPhone: String?,
  val transactionId: String?,
  val status: String,
  val message: String,
)

enum class CashPreviewAuthState {
  Loading,
  SignedOut,
  Unauthorized,
  Authorized,
}

sealed interface CashPreviewSmsParseResult {
  data class IgnoredUnknownOrDisabledSender(val senderAddress: String) : CashPreviewSmsParseResult
  data class IgnoredNotIncomingPayment(
    val provider: CashPreviewProviderConfig,
    val senderAddress: String,
    val reason: String,
  ) : CashPreviewSmsParseResult

  data class Matched(val parsed: ParsedCashPreviewSms) : CashPreviewSmsParseResult
}

object CashPreviewSmsParser {

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
    providers: List<CashPreviewProviderConfig>,
  ): CashPreviewProviderConfig? {
    val cleanSenderRaw = normalizeDigits(senderAddress.trim().lowercase(Locale.ROOT))
    val cleanSenderCompact = normalizeSenderToken(senderAddress)
    if (cleanSenderRaw.isBlank()) return null

    for (provider in providers.filter { it.isEnabled }) {
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
    "(?:تم\\s*(?:استلام|إستلام|ايداع|إيداع|تحصيل)|استلمت|وصلك|تحويل\\s*وارد)",
  )
  private val outgoingRejectRegex = Regex(
    "(?:تم\\s*(?:تحويل\\s+مبلغ\\s+.*\\s*(?:إلى|الى)|سحب|دفع\\s+فاتورة|شحن\\s+رصيد|خصم)|رصيدك\\s+لا\\s+يكفي)",
  )
  private val primaryAmountRegex = Regex(
    "(?:تم\\s*(?:استلام|إستلام|ايداع|إيداع|تحصيل)|استلمت|وصلك)\\s*(?:حوالة\\s*)?(?:ب?مبلغ)?\\s*[:：]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:جنيه|ج\\.?م|جم|egp)?",
    RegexOption.IGNORE_CASE,
  )
  private val secondaryAmountRegex = Regex(
    "(?:مبلغ|قيمة)\\s*[:：]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:جنيه|ج\\.?م|جم|egp)?",
    RegexOption.IGNORE_CASE,
  )
  private val senderPhoneLabeledRegex = Regex(
    "(?:من\\s*(?:رقم|محفظة|الرقم|حساب)|المرسل|رقم\\s*الراسل)\\s*[:：]?\\s*(\\+?2?0?0?1[0125][0-9\\-\\s]{8,11})",
  )
  private val walletOwnerClauseRegex = Regex(
    "(?:على\\s*(?:رقم\\s*)?محفظتك|إلى\\s*(?:رقم\\s*)?محفظتك|الى\\s*(?:رقم\\s*)?محفظتك|على\\s*محفظة)",
  )
  private val anyEgyptPhoneRegex = Regex("(?:\\+?20|0020)?0?1[0125][0-9]{8}")
  private val transactionIdRegex = Regex(
    "(?:رقم\\s*(?:العملية|المعاملة|التحويل|المرجع)|الرقم\\s*المرجعي|كود\\s*العملية|مرجع(?:\\s*رقم)?|Ref(?:erence)?\\s*(?:No|ID)?)\\s*[:：#]?\\s*([A-Za-z0-9\\-_]{5,28})",
    RegexOption.IGNORE_CASE,
  )

  fun parseIncomingPaymentSms(
    senderAddress: String,
    rawBody: String,
    providers: List<CashPreviewProviderConfig>,
    receivedAtMillis: Long = System.currentTimeMillis(),
  ): CashPreviewSmsParseResult {
    val provider = matchEnabledProvider(senderAddress, providers)
      ?: return CashPreviewSmsParseResult.IgnoredUnknownOrDisabledSender(senderAddress)

    val body = normalizeDigits(rawBody)
      .replace('أ', 'ا')
      .replace('إ', 'ا')
      .replace('آ', 'ا')
      .replace('\u00A0', ' ')
      .trim()

    if (!incomingReceiptRegex.containsMatchIn(body)) {
      return CashPreviewSmsParseResult.IgnoredNotIncomingPayment(
        provider = provider,
        senderAddress = senderAddress,
        reason = if (outgoingRejectRegex.containsMatchIn(body)) {
          "رسالة صادر/سحب وليست استلام مبلغ"
        } else {
          "رسالة إعلامية أو غير مرتبطة باستلام تحويل"
        },
      )
    }

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
      ?: return CashPreviewSmsParseResult.IgnoredNotIncomingPayment(
        provider = provider,
        senderAddress = senderAddress,
        reason = "تعذر استخراج قيمة المبلغ المستلم بدقة",
      )

    val beforeRecipientWallet = beforeBalanceSection
      .split(walletOwnerClauseRegex)
      .firstOrNull()
      ?: beforeBalanceSection

    val rawPhoneCandidate = senderPhoneLabeledRegex.find(body)?.groupValues?.getOrNull(1)
      ?: anyEgyptPhoneRegex.find(beforeRecipientWallet)?.value

    val senderPhone = normalizeEgyptianPhone(rawPhoneCandidate)
    if (senderPhone.length != 11 || !senderPhone.startsWith("01")) {
      return CashPreviewSmsParseResult.IgnoredNotIncomingPayment(
        provider = provider,
        senderAddress = senderAddress,
        reason = "تعذر استخراج رقم الهاتف المحوَّل منه من نص الرسالة",
      )
    }

    val txId = transactionIdRegex.find(body)?.groupValues?.getOrNull(1)?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: "AUTO-$senderPhone-${String.format(Locale.US, "%.2f", amount)}-${receivedAtMillis / 60_000}"

    return CashPreviewSmsParseResult.Matched(
      ParsedCashPreviewSms(
        provider = provider,
        rawSenderAddress = senderAddress,
        amount = amount,
        senderPhone = senderPhone,
        transactionId = txId,
        rawBody = rawBody.trim(),
      ),
    )
  }
}
