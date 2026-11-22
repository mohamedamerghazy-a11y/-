package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.location.LocationHelper
import com.example.core.model.FulfillmentMethod
import com.example.core.model.PaymentMethod
import com.example.core.util.ArabicFormatter
import com.example.data.media.CloudinaryFolder
import com.example.data.model.DeliveryAddress
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.CouponInputCard
import com.example.ui.components.FreeShippingProgressBanner
import com.example.ui.components.ImagePickerUploadModal
import com.example.ui.components.PriceSummaryRows
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.components.ZoomableImagePreviewDialog
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CartAndOrdersUiState

@Composable
fun CheckoutScreen(
  cartUiState: CartAndOrdersUiState,
  addresses: List<DeliveryAddress>,
  defaultAddress: DeliveryAddress?,
  isWholesaleApproved: Boolean,
  storeName: String = "الإيمان للتجارة",
  storeAddress: String = "",
  storePhone: String = "",
  customerName: String = "",
  customerPhone: String = "",
  onSelectFulfillmentMethod: (FulfillmentMethod) -> Unit = {},
  onSelectZone: (String?) -> Unit,
  onAutoMatchZone: (DeliveryAddress?) -> Unit,
  onCouponInputChange: (String) -> Unit,
  onApplyCoupon: () -> Unit,
  onRemoveCoupon: () -> Unit,
  onOpenAddresses: () -> Unit,
  onClearMessages: () -> Unit,
  onPlaceOrder: (
    fulfillmentMethod: FulfillmentMethod,
    address: DeliveryAddress?,
    paymentMethod: PaymentMethod,
    transferReference: String?,
    receiptImageUrl: String?,
    notes: String?,
    pickupRecipientName: String?,
    pickupPhone: String?,
  ) -> Unit,
  onBackClick: () -> Unit,
) {
  var selectedAddressId by remember(addresses, defaultAddress) {
    mutableStateOf(defaultAddress?.id ?: addresses.firstOrNull()?.id)
  }
  val selectedAddress = remember(addresses, selectedAddressId, defaultAddress) {
    addresses.firstOrNull { it.id == selectedAddressId } ?: defaultAddress ?: addresses.firstOrNull()
  }

  val availablePaymentMethods = remember(cartUiState.paymentSettings) {
    buildList {
      if (cartUiState.paymentSettings.codEnabled) add(PaymentMethod.COD)
      if (cartUiState.paymentSettings.instaPayEnabled) add(PaymentMethod.INSTAPAY)
      if (cartUiState.paymentSettings.vodafoneCashEnabled) add(PaymentMethod.VODAFONE_CASH)
    }.ifEmpty { listOf(PaymentMethod.COD) }
  }

  var selectedPaymentMethod by remember(availablePaymentMethods) {
    mutableStateOf(availablePaymentMethods.first())
  }
  var transferReference by remember { mutableStateOf("") }
  var receiptImageUrl by remember { mutableStateOf<String?>(null) }
  var showReceiptPickerModal by remember { mutableStateOf(false) }
  var showReceiptZoomDialog by remember { mutableStateOf(false) }
  var receiptUploadError by remember { mutableStateOf<String?>(null) }
  var notes by remember { mutableStateOf("") }

  var pickupRecipientName by remember(customerName, defaultAddress) {
    mutableStateOf(
      customerName.ifBlank { defaultAddress?.recipientName.orEmpty() },
    )
  }
  var pickupPhone by remember(customerPhone, defaultAddress) {
    mutableStateOf(
      customerPhone.ifBlank { defaultAddress?.phone.orEmpty() },
    )
  }
  val isStorePickup = cartUiState.fulfillmentMethod == FulfillmentMethod.STORE_PICKUP

  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(selectedAddress?.id, cartUiState.deliveryZones.size) {
    onAutoMatchZone(selectedAddress)
  }

  LaunchedEffect(cartUiState.errorMessage, cartUiState.feedbackMessage) {
    val msg = cartUiState.errorMessage ?: cartUiState.feedbackMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
    }
  }

  val summary = cartUiState.cartSummary

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = "إتمام الطلب",
        subtitle = if (isStorePickup) "الاستلام المباشر من المتجر وطريقة الدفع" else "مراجعة عنوان التوصيل وطريقة الدفع",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (summary.lines.isEmpty()) {
        Column(
          modifier = Modifier
            .widthIn(max = 600.dp)
            .padding(AlEmanDimens.SpaceLg),
        ) {
          StoreEmptyStateCard(
            icon = Icons.Filled.ShoppingCart,
            title = "عربة التسوق فارغة",
            description = "أضف منتجات إلى السلة أولًا قبل متابعة خطوة إتمام الطلب.",
            actionLabel = "العودة للمتجر",
            onActionClick = onBackClick,
          )
        }
        return@Box
      }

      LazyColumn(
        modifier = Modifier
          .widthIn(max = 840.dp)
          .fillMaxSize()
          .testTag("checkout_lazy_column"),
        contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // 0. طريقة استلام الطلب (توصيل للمنزل أو الاستلام من المتجر)
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Storefront,
                  contentDescription = null,
                  tint = RoyalBlue,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "طريقة استلام الطلب",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = RoyalBlue,
                )
              }

              FulfillmentMethod.entries.forEach { method ->
                val selected = cartUiState.fulfillmentMethod == method
                Surface(
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  color = if (selected) RoyalBlueSoft else GeneralBackground,
                  border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) RoyalBlue else BorderNeutral,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFulfillmentMethod(method) }
                    .testTag("checkout_fulfillment_${method.code}"),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    RadioButton(
                      selected = selected,
                      onClick = { onSelectFulfillmentMethod(method) },
                      colors = RadioButtonDefaults.colors(selectedColor = RoyalBlue),
                    )
                    Icon(
                      imageVector = if (method == FulfillmentMethod.STORE_PICKUP) {
                        Icons.Filled.Storefront
                      } else {
                        Icons.Filled.LocalShipping
                      },
                      contentDescription = null,
                      tint = if (selected) RoyalBlue else CharcoalBlack,
                      modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Text(
                          text = method.arabicLabel,
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                          color = TextPrimary,
                        )
                        if (method == FulfillmentMethod.STORE_PICKUP) {
                          BrandPillBadge(
                            text = "مجانًا",
                            containerColor = SuccessGreenSoft,
                            contentColor = SuccessGreen,
                          )
                        }
                      }
                      Text(
                        text = method.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        if (isStorePickup) {
          item {
            AlEmanCard(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_pickup_info_card"),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = CharcoalBlack,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = "بيانات الاستلام من $storeName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                }

                Surface(
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  color = PrimaryYellowSoft,
                  border = BorderStroke(1.dp, PrimaryYellow),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = "مقر الاستلام: ${storeAddress.ifBlank { "المقر الرئيسي لمتجر $storeName" }}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                      color = CharcoalBlack,
                    )
                    if (storePhone.isNotBlank()) {
                      Text(
                        text = "هاتف الفرع: ${ArabicFormatter.formatDigits(storePhone)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                      )
                    }
                    Text(
                      text = "سيتم تجهيز طلبك وإشعارك فور جاهزيته للاستلام من المتجر بدون أي رسوم شحن.",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextSecondary,
                    )
                  }
                }

                OutlinedTextField(
                  value = pickupRecipientName,
                  onValueChange = { pickupRecipientName = it },
                  label = { Text("اسم الشخص المستلم بالمتجر") },
                  singleLine = true,
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedBorderColor = RoyalBlue,
                    unfocusedBorderColor = BorderNeutral,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_pickup_name_input"),
                )

                OutlinedTextField(
                  value = pickupPhone,
                  onValueChange = { pickupPhone = it },
                  label = { Text("رقم الهاتف للتواصل عند التجهيز") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                  singleLine = true,
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedBorderColor = RoyalBlue,
                    unfocusedBorderColor = BorderNeutral,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_pickup_phone_input"),
                )
              }
            }
          }
        } else {
        // 1. عنوان التوصيل
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = "عنوان التوصيل",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoyalBlue,
                  )
                }
                OutlinedButton(
                  onClick = onOpenAddresses,
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.4f)),
                  modifier = Modifier.testTag("checkout_manage_addresses_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.AddLocationAlt,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (addresses.isEmpty()) "إضافة عنوان" else "تغيير / إضافة",
                    color = RoyalBlue,
                  )
                }
              }

              if (addresses.isEmpty()) {
                Surface(
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  color = GoldenYellowSoft,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = "لا يوجد عنوان توصيل محفوظ حتى الآن. أضف عنوانك لنتمكن من تسليم الطلب إليك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(12.dp),
                  )
                }
              } else {
                addresses.forEach { addr ->
                  val isSelected = addr.id == selectedAddress?.id
                  Surface(
                    shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                    color = if (isSelected) RoyalBlueSoft else GeneralBackground,
                    border = BorderStroke(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color = if (isSelected) RoyalBlue else BorderNeutral,
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        selectedAddressId = addr.id
                        onAutoMatchZone(addr)
                      }
                      .testTag("checkout_address_${addr.id}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.Top,
                    ) {
                      RadioButton(
                        selected = isSelected,
                        onClick = {
                          selectedAddressId = addr.id
                          onAutoMatchZone(addr)
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = RoyalBlue),
                      )
                      Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          Text(
                            text = addr.label.ifBlank { "العنوان" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                          )
                          if (addr.isDefault) {
                            BrandPillBadge(
                              text = "افتراضي",
                              containerColor = PureWhite,
                              contentColor = RoyalBlue,
                            )
                          }
                        }
                        Text(
                          text = "${addr.recipientName} • ${addr.phone}",
                          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                          color = TextPrimary,
                        )
                        Text(
                          text = "${addr.governorate} - ${addr.city} - ${addr.streetAddress}",
                          style = MaterialTheme.typography.bodySmall,
                          color = TextSecondary,
                        )
                        if (addr.hasCoordinates || addr.streetAddress.isNotBlank()) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                          ) {
                            if (addr.hasCoordinates) {
                              BrandPillBadge(
                                text = "GPS محدّد",
                                containerColor = PrimaryYellowSoft,
                                contentColor = CharcoalBlack,
                              )
                            }
                            TextButton(
                              onClick = {
                                LocationHelper.openLocationInMaps(
                                  context = context,
                                  latitude = addr.latitude,
                                  longitude = addr.longitude,
                                  fallbackAddress = "${addr.governorate} - ${addr.city} - ${addr.streetAddress}",
                                  label = addr.label.ifBlank { addr.recipientName },
                                )
                              },
                              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            ) {
                              Icon(Icons.Filled.Map, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(15.dp))
                              Spacer(modifier = Modifier.width(4.dp))
                              Text("عرض على الخريطة", style = MaterialTheme.typography.labelSmall, color = RoyalBlue, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // 2. منطقة ورسوم التوصيل
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.LocalShipping,
                  contentDescription = null,
                  tint = RoyalBlue,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "منطقة التوصيل",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = RoyalBlue,
                )
              }

              if (cartUiState.deliveryZones.isEmpty()) {
                Text(
                  text = "التوصيل القياسي: مجاني أو يتم تحديد التكلفة مع المندوب حسب منطقتك.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(cartUiState.deliveryZones, key = { it.id }) { zone ->
                    val selected = zone.id == cartUiState.selectedDeliveryZone?.id
                    FilterChip(
                      selected = selected,
                      onClick = { onSelectZone(zone.id) },
                      label = {
                        Text(
                          text = "${zone.name} (${ArabicFormatter.formatCurrency(zone.fee)})",
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                      },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoyalBlue,
                        selectedLabelColor = PureWhite,
                        containerColor = PureWhite,
                        labelColor = TextPrimary,
                      ),
                      border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = BorderNeutral,
                        selectedBorderColor = RoyalBlue,
                      ),
                    )
                  }
                }
                cartUiState.selectedDeliveryZone?.let { activeZone ->
                  Text(
                    text = "المدة المتوقعة للتوصيل إلى ${activeZone.governorate}: ${activeZone.estimatedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                  )
                }
              }
            }
          }
        }
        }

        // 3. طرق الدفع
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Payments,
                  contentDescription = null,
                  tint = RoyalBlue,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "طريقة الدفع",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = RoyalBlue,
                )
              }

              availablePaymentMethods.forEach { method ->
                val isSelected = method == selectedPaymentMethod
                Surface(
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  color = if (isSelected) RoyalBlueSoft else GeneralBackground,
                  border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) RoyalBlue else BorderNeutral,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPaymentMethod = method }
                    .testTag("checkout_payment_${method.code}"),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    RadioButton(
                      selected = isSelected,
                      onClick = { selectedPaymentMethod = method },
                      colors = RadioButtonDefaults.colors(selectedColor = RoyalBlue),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = method.arabicLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                      )
                      Text(
                        text = method.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                      )
                    }
                  }
                }
              }

              if (selectedPaymentMethod.requiresTransferReference) {
                val accountInfo = when (selectedPaymentMethod) {
                  PaymentMethod.INSTAPAY -> cartUiState.paymentSettings.instaPayAddress.ifBlank {
                    "سيتم تأكيد عنوان إنستاباي مع خدمة العملاء فور مراجعة الطلب"
                  }
                  PaymentMethod.VODAFONE_CASH -> cartUiState.paymentSettings.vodafoneCashNumber.ifBlank {
                    "سيتم تزويدك برقم محفظة فودافون كاش عبر تأكيد الطلب"
                  }
                  PaymentMethod.COD -> ""
                }
                Surface(
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  color = GoldenYellowSoft,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = "بيانات التحويل (${selectedPaymentMethod.arabicLabel})",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = TextPrimary,
                    )
                    Text(
                      text = accountInfo,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = RoyalBlue,
                    )
                    if (cartUiState.paymentSettings.paymentInstructions.isNotBlank()) {
                      Text(
                        text = cartUiState.paymentSettings.paymentInstructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                      )
                    }
                  }
                }

                OutlinedTextField(
                  value = transferReference,
                  onValueChange = { transferReference = it },
                  label = { Text("الرقم المحوَّل منه (للتأكيد التلقائي أو مع الإيصال)") },
                  placeholder = { Text("مثال: 01091415953") },
                  supportingText = {
                    Text(
                      text = "أدخل رقم المحفظة أو الهاتف الذي قمت بالتحويل منه ليتم تأكيد الطلب تلقائيًا فور وصول رسالة التحويل.",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextSecondary,
                    )
                  },
                  keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                  ),
                  singleLine = true,
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedBorderColor = CharcoalBlack,
                    unfocusedBorderColor = BorderNeutral,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_transfer_reference_input"),
                )

                Surface(
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  color = GeneralBackground,
                  border = BorderStroke(1.dp, BorderNeutral),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Receipt,
                          contentDescription = null,
                          tint = CharcoalBlack,
                          modifier = Modifier.size(18.dp),
                        )
                        Text(
                          text = "صورة إيصال التحويل",
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                          color = CharcoalBlack,
                        )
                      }
                      if (!receiptImageUrl.isNullOrBlank()) {
                        SuccessBadge("تم الإرفاق")
                      }
                    }

                    if (!receiptUploadError.isNullOrBlank()) {
                      Surface(
                        shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                        color = DiscountRedSoft,
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Text(
                          text = receiptUploadError.orEmpty(),
                          style = MaterialTheme.typography.bodySmall,
                          color = DiscountRed,
                          modifier = Modifier.padding(8.dp),
                        )
                      }
                    }

                    val activeReceiptUrl = receiptImageUrl
                    if (activeReceiptUrl.isNullOrBlank()) {
                      Text(
                        text = "يمكنك إرفاق لقطة شاشة أو صورة إيصال التحويل لتسريع مراجعة الدفع وتأكيد طلبك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                      )
                      OutlinedButton(
                        onClick = {
                          receiptUploadError = null
                          showReceiptPickerModal = true
                        },
                        shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                        border = BorderStroke(1.dp, CharcoalBlack),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("checkout_upload_receipt_button"),
                      ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "إرفاق صورة الإيصال (معرض / كاميرا)",
                          color = CharcoalBlack,
                          fontWeight = FontWeight.Bold,
                        )
                      }
                    } else {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        Box(
                          modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
                            .background(PureWhite)
                            .clickable { showReceiptZoomDialog = true },
                        ) {
                          ProductThumbnail(
                            imageUrl = activeReceiptUrl,
                            contentDescription = "معاينة إيصال التحويل",
                            modifier = Modifier.fillMaxSize(),
                          )
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CharcoalBlack.copy(alpha = 0.78f),
                            modifier = Modifier
                              .align(Alignment.BottomEnd)
                              .padding(3.dp),
                          ) {
                            Icon(
                              imageVector = Icons.Filled.ZoomIn,
                              contentDescription = "تكبير الإيصال",
                              tint = PrimaryYellow,
                              modifier = Modifier
                                .padding(2.dp)
                                .size(14.dp),
                            )
                          }
                        }
                        Column(
                          modifier = Modifier.weight(1f),
                          verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          OutlinedButton(
                            onClick = { showReceiptPickerModal = true },
                            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                            border = BorderStroke(1.dp, CharcoalBlack),
                            modifier = Modifier.fillMaxWidth(),
                          ) {
                            Text("تغيير صورة الإيصال", color = CharcoalBlack, fontWeight = FontWeight.SemiBold)
                          }
                          TextButton(
                            onClick = { receiptImageUrl = null },
                            modifier = Modifier
                              .align(Alignment.End)
                              .testTag("checkout_remove_receipt_button"),
                          ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إزالة الإيصال", color = DiscountRed)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // 4. الكوبون والملاحظات
        item {
          CouponInputCard(
            couponInput = cartUiState.couponCodeInput,
            appliedCoupon = cartUiState.appliedCoupon,
            isValidating = cartUiState.isCouponValidating,
            couponError = cartUiState.couponError,
            discountAmount = summary.discountAmount,
            onInputChange = onCouponInputChange,
            onApplyClick = onApplyCoupon,
            onRemoveClick = onRemoveCoupon,
          )
        }

        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "ملاحظات الطلب (اختياري)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
              )
              OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("مثال: موعد التوصيل المناسب أو تعليمات للمندوب") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = PureWhite,
                  unfocusedContainerColor = PureWhite,
                  focusedBorderColor = RoyalBlue,
                  unfocusedBorderColor = BorderNeutral,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("checkout_notes_input"),
              )
            }
          }
        }

        // 5. مراجعة الأصناف والملخص النهائي وزر التأكيد
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "مراجعة الفاتورة (${summary.totalItemsCount} وحدة)",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = RoyalBlue,
                )
                if (isWholesaleApproved) {
                  BrandPillBadge(
                    text = "أسعار جملة",
                    containerColor = GoldenYellowSoft,
                    contentColor = TextPrimary,
                  )
                }
              }

              summary.lines.forEach { line ->
                val variantParts = listOfNotNull(
                  line.selectedColor?.takeIf { it.isNotBlank() }?.let { "اللون: $it" },
                  line.selectedSize?.takeIf { it.isNotBlank() }?.let { "المقاس: $it" },
                )
                val variantSuffix = if (variantParts.isNotEmpty()) " (${variantParts.joinToString(" • ")})" else ""
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "${line.quantity} × ${line.product.name}$variantSuffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = ArabicFormatter.formatCurrency(line.lineTotal),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                  )
                }
              }

              HorizontalDivider(color = BorderNeutral)

              FreeShippingProgressBanner(
                summary = summary,
                isStorePickup = isStorePickup,
              )

              PriceSummaryRows(
                subtotal = summary.subtotal,
                discountAmount = summary.discountAmount,
                deliveryFee = summary.deliveryFee,
                totalAmount = summary.totalAmount,
                isStorePickup = isStorePickup,
              )

              Spacer(modifier = Modifier.height(4.dp))

              val hasFulfillmentContact = if (isStorePickup) {
                pickupRecipientName.trim().length >= 2 && pickupPhone.trim().length >= 6
              } else {
                selectedAddress != null
              }

              val canSubmit = hasFulfillmentContact &&
                !cartUiState.isSubmittingOrder &&
                !summary.hasStockConflicts &&
                (!selectedPaymentMethod.requiresTransferReference || transferReference.isNotBlank() || !receiptImageUrl.isNullOrBlank())

              Button(
                onClick = {
                  onPlaceOrder(
                    cartUiState.fulfillmentMethod,
                    if (isStorePickup) null else selectedAddress,
                    selectedPaymentMethod,
                    transferReference.trim().takeIf { it.isNotEmpty() },
                    receiptImageUrl?.trim()?.takeIf { it.isNotEmpty() },
                    notes.trim().takeIf { it.isNotEmpty() },
                    pickupRecipientName.trim().takeIf { it.isNotEmpty() },
                    pickupPhone.trim().takeIf { it.isNotEmpty() },
                  )
                },
                enabled = canSubmit,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("checkout_submit_order_button"),
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                  containerColor = PrimaryYellow,
                  contentColor = CharcoalBlack,
                ),
              ) {
                if (cartUiState.isSubmittingOrder) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = CharcoalBlack,
                    strokeWidth = 2.dp,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("جاري تأكيد الطلب...", fontWeight = FontWeight.Bold)
                } else {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "تأكيد وإرسال الطلب • ${ArabicFormatter.formatCurrency(summary.totalAmount)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  ImagePickerUploadModal(
    visible = showReceiptPickerModal,
    title = "إرفاق صورة إيصال الدفع",
    folder = CloudinaryFolder.RECEIPTS,
    allowMultiple = false,
    onUploadedUrls = { urls ->
      receiptImageUrl = urls.firstOrNull()
      receiptUploadError = null
      showReceiptPickerModal = false
    },
    onError = { err ->
      receiptUploadError = err
      showReceiptPickerModal = false
    },
    onDismiss = { showReceiptPickerModal = false },
  )

  val activeReceiptZoom = receiptImageUrl
  if (showReceiptZoomDialog && !activeReceiptZoom.isNullOrBlank()) {
    ZoomableImagePreviewDialog(
      imageUrl = activeReceiptZoom,
      title = "معاينة إيصال التحويل",
      onDismiss = { showReceiptZoomDialog = false },
    )
  }
}
