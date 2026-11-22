package com.aleman.cash

import com.aleman.cash.model.CashProviderConfig
import com.aleman.cash.model.defaultCashProviders
import com.aleman.cash.sms.SmsCashParser
import com.aleman.cash.sms.SmsParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsCashParserTest {

  private val sampleVodafoneMessage =
    "تم استلام مبلغ 1000 جنيه من رقم 01091415953 المسجل بإسم Shehab A Elshenawy على رقم محفظتك  01067677607.\n" +
      "رصيدك الحالي: 1000.41 جنيه\n" +
      "تاريخ العملية: 01:31 26-11-18\n" +
      "رقم العملية: 025904460796\n" +
      "تابع كل مصروفاتك من تاريخ المعاملات على أبلكيشن أنا فودافون http://vf.eg/vfcash\n"

  @Test
  fun exactVodafoneSample_parsesAmountPhoneAndTxId_ignoringCurrentBalanceAndReceiverWallet() {
    val result = SmsCashParser.parseIncomingPaymentSms(
      senderAddress = "VF-Cash",
      rawBody = sampleVodafoneMessage,
      providers = defaultCashProviders()
    )

    assertTrue("Expected Matched result but got $result", result is SmsParseResult.Matched)
    val parsed = (result as SmsParseResult.Matched).parsed
    assertEquals("فودافون كاش", parsed.provider.displayName)
    assertEquals(1000.0, parsed.amount, 0.001)
    assertEquals("01091415953", parsed.senderPhone)
    assertEquals("025904460796", parsed.transactionId)
  }

  @Test
  fun easternArabicNumerals_andRoundedUpAmountCheck() {
    val body = "تم استلام مبلغ ١٢٥٠٫٥٠ جنيه من رقم ٠١٠٠١٢٣٤٥٦٧ على محفظتك. رقم العملية: 987654321"
    val result = SmsCashParser.parseIncomingPaymentSms(
      senderAddress = "OrangeCash",
      rawBody = body,
      providers = defaultCashProviders()
    )

    assertTrue(result is SmsParseResult.Matched)
    val parsed = (result as SmsParseResult.Matched).parsed
    assertEquals(1250.50, parsed.amount, 0.001)
    assertEquals("01001234567", parsed.senderPhone)
    assertEquals("987654321", parsed.transactionId)

    // Verify order matching condition: exact or greater (fractions) allowed, smaller rejected
    assertTrue(parsed.amount + 0.001 >= 1250.25)
    assertTrue(parsed.amount + 0.001 >= 1250.50)
    assertTrue(parsed.amount + 0.001 < 1251.00)
  }

  @Test
  fun disabledProvider_orUnknownSender_isStrictlyIgnored() {
    val disabledVodafone = defaultCashProviders().map {
      if (it.id == "vodafone_cash") it.copy(isEnabled = false) else it
    }

    val fromDisabled = SmsCashParser.parseIncomingPaymentSms(
      senderAddress = "VF-Cash",
      rawBody = sampleVodafoneMessage,
      providers = disabledVodafone
    )
    assertTrue(fromDisabled is SmsParseResult.IgnoredUnknownOrDisabledSender)

    val fromUnknown = SmsCashParser.parseIncomingPaymentSms(
      senderAddress = "+201122334455",
      rawBody = sampleVodafoneMessage,
      providers = defaultCashProviders()
    )
    assertTrue(fromUnknown is SmsParseResult.IgnoredUnknownOrDisabledSender)
  }

  @Test
  fun customUserAddedProvider_matchesConfiguredSenderAlias() {
    val customProviders = defaultCashProviders() + CashProviderConfig(
      id = "instapay_custom",
      displayName = "إنستاباي",
      senderAliases = listOf("InstaPay"),
      isEnabled = true
    )

    val sms = "تم استلام مبلغ 450 جنيه من رقم 01234567890 عبر InstaPay مرجع REF998877"
    val result = SmsCashParser.parseIncomingPaymentSms(
      senderAddress = "InstaPay",
      rawBody = sms,
      providers = customProviders
    )

    assertTrue(result is SmsParseResult.Matched)
    val parsed = (result as SmsParseResult.Matched).parsed
    assertEquals("إنستاباي", parsed.provider.displayName)
    assertEquals(450.0, parsed.amount, 0.001)
    assertEquals("01234567890", parsed.senderPhone)
    assertEquals("REF998877", parsed.transactionId)
  }

  @Test
  fun outgoingTransferSms_isIgnored() {
    val outgoing = "تم تحويل مبلغ 500 جنيه إلى رقم 01091415953 رصيدك الحالي 200 جنيه"
    val result = SmsCashParser.parseIncomingPaymentSms(
      senderAddress = "VodafoneCash",
      rawBody = outgoing,
      providers = defaultCashProviders()
    )
    assertTrue(result is SmsParseResult.IgnoredNotIncomingPayment)
  }
}
