package com.aleman.cash.model

import kotlinx.serialization.Serializable

@Serializable
data class CashProviderConfig(
  val id: String,
  val displayName: String,
  val senderAliases: List<String>,
  val isEnabled: Boolean = true,
  val isBuiltIn: Boolean = false
)

@Serializable
data class CashVerifierSettings(
  val providers: List<CashProviderConfig> = defaultCashProviders(),
  val autoVerifyEnabled: Boolean = true,
  val targetOrderStatusAfterPayment: String = "preparing"
)

fun defaultCashProviders(): List<CashProviderConfig> = listOf(
  CashProviderConfig(
    id = "vodafone_cash",
    displayName = "فودافون كاش",
    senderAliases = listOf("VF-Cash", "VodafoneCash", "Vodafone", "فودافون كاش", "فودافون"),
    isEnabled = true,
    isBuiltIn = true
  ),
  CashProviderConfig(
    id = "orange_cash",
    displayName = "أورنج كاش",
    senderAliases = listOf("OrangeCash", "Orange Money", "Orange", "أورنج كاش", "اورنج كاش"),
    isEnabled = true,
    isBuiltIn = true
  ),
  CashProviderConfig(
    id = "etisalat_cash",
    displayName = "اتصالات كاش",
    senderAliases = listOf("EtisalatCash", "Etisalat", "e& money", "e&", "اتصالات كاش", "اتصالات"),
    isEnabled = true,
    isBuiltIn = true
  ),
  CashProviderConfig(
    id = "we_cash",
    displayName = "وي كاش",
    senderAliases = listOf("WECash", "WE Pay", "WE", "وي كاش", "وى كاش"),
    isEnabled = true,
    isBuiltIn = true
  )
)

data class ParsedCashSms(
  val provider: CashProviderConfig,
  val rawSenderAddress: String,
  val amount: Double,
  val senderPhone: String,
  val transactionId: String,
  val rawBody: String
)

data class PendingTransferOrder(
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
  val createdAtMillis: Long
) {
  val effectiveSenderPhone: String
    get() = senderPhone.ifBlank { transferReference }
}

data class CashVerificationLogItem(
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
  val verifiedByDeviceName: String = "",
  val verifiedByEmail: String = "",
  val isSettled: Boolean = false
)

data class LocalVerificationEvent(
  val id: String,
  val timestampMillis: Long,
  val senderAddress: String,
  val providerName: String,
  val amount: Double?,
  val senderPhone: String?,
  val transactionId: String?,
  val status: String,
  val message: String
)

enum class StaffAuthState {
  Loading,
  SignedOut,
  Unauthorized,
  Authorized
}
