package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.FulfillmentMethod
import com.example.core.model.PaymentMethod
import com.example.data.local.LocalCartEntry
import com.example.data.local.LocalCartStore
import com.example.data.model.BrandShowcase
import com.example.data.model.CartSummary
import com.example.data.model.Coupon
import com.example.data.model.DeliveryAddress
import com.example.data.model.DeliveryZone
import com.example.data.model.FlashSale
import com.example.data.model.FlashSaleStatus
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.ResolvedCartLine
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.StorePaymentSettings
import com.example.data.model.UserProfile
import com.example.data.model.buildCartItemId
import com.example.data.model.computeFreeShipping
import com.example.data.model.effectiveUnitPriceWithShowcases
import com.example.data.model.normalizeEgyptianPhone
import com.example.data.repository.CartRepository
import com.example.data.repository.CheckoutLineRequest
import com.example.data.repository.CouponAndDeliveryRepository
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToLong

data class CartAndOrdersUiState(
  val cartSummary: CartSummary = CartSummary(),
  val appliedCoupon: Coupon? = null,
  val couponCodeInput: String = "",
  val isCouponValidating: Boolean = false,
  val couponError: String? = null,
  val deliveryZones: List<DeliveryZone> = emptyList(),
  val selectedDeliveryZoneId: String? = null,
  val fulfillmentMethod: FulfillmentMethod = FulfillmentMethod.DELIVERY,
  val paymentSettings: StorePaymentSettings = StorePaymentSettings(),
  val orders: List<Order> = emptyList(),
  val isOrdersLoading: Boolean = false,
  val isSubmittingOrder: Boolean = false,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val selectedDeliveryZone: DeliveryZone?
    get() = deliveryZones.firstOrNull { it.id == selectedDeliveryZoneId }
      ?: deliveryZones.firstOrNull()
}

private data class CartAuxiliaryInputs(
  val deliveryZones: List<DeliveryZone>,
  val selectedZoneId: String?,
  val fulfillmentMethod: FulfillmentMethod,
  val storeSettings: StoreGeneralSettings,
  val paymentSettings: StorePaymentSettings,
  val appliedCoupon: Coupon?,
  val couponInput: String,
  val isCouponValidating: Boolean,
  val couponError: String?,
  val orders: List<Order>,
  val isOrdersLoading: Boolean,
  val isSubmittingOrder: Boolean,
  val feedbackMessage: String?,
  val errorMessage: String?,
)

private data class DeliveryAndSettingsBundle(
  val zones: List<DeliveryZone>,
  val selectedZoneId: String?,
  val fulfillment: FulfillmentMethod,
  val storeSettings: StoreGeneralSettings,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CartOrderViewModel(
  private val localCartStore: LocalCartStore,
  private val cartRepository: CartRepository,
  private val couponAndDeliveryRepository: CouponAndDeliveryRepository,
  private val orderRepository: OrderRepository,
) : ViewModel() {

  private val uidFlow = MutableStateFlow<String?>(null)
  private val userProfileFlow = MutableStateFlow<UserProfile?>(null)
  private val catalogProductsFlow = MutableStateFlow<List<Product>>(emptyList())
  private val activeFlashSalesFlow = MutableStateFlow<List<FlashSale>>(emptyList())
  private val activeBrandShowcasesFlow = MutableStateFlow<List<BrandShowcase>>(emptyList())
  private val isWholesalePreviewActiveFlow = MutableStateFlow(false)
  private val storeSettingsFlow = MutableStateFlow(StoreGeneralSettings())

  private val appliedCouponFlow = MutableStateFlow<Coupon?>(null)
  private val couponCodeInputFlow = MutableStateFlow("")
  private val isCouponValidatingFlow = MutableStateFlow(false)
  private val couponErrorFlow = MutableStateFlow<String?>(null)
  private val selectedDeliveryZoneIdFlow = MutableStateFlow<String?>(null)
  private val fulfillmentMethodFlow = MutableStateFlow(FulfillmentMethod.DELIVERY)
  private val isOrdersLoadingFlow = MutableStateFlow(false)
  private val isSubmittingOrderFlow = MutableStateFlow(false)
  private val feedbackMessageFlow = MutableStateFlow<String?>(null)
  private val errorMessageFlow = MutableStateFlow<String?>(null)

  private var lastMergedUid: String? = null

  private val rawCartEntriesFlow: Flow<Map<String, LocalCartEntry>> = uidFlow.flatMapLatest { uid ->
    if (uid.isNullOrBlank()) {
      localCartStore.cartEntriesFlow.map { entries ->
        entries.associateBy { it.cartItemId }
      }
    } else {
      cartRepository.observeUserCart(uid)
        .map { records ->
          records.associate { rec ->
            val key = rec.id.takeIf { it.isNotBlank() }
              ?: buildCartItemId(rec.productId, rec.selectedColor, rec.selectedSize)
            key to LocalCartEntry(
              productId = rec.productId,
              quantity = rec.quantity,
              flashSaleId = rec.flashSaleId,
              selectedColor = rec.selectedColor,
              selectedSize = rec.selectedSize,
            )
          }
        }
        .catch { error ->
          errorMessageFlow.value = error.localizedMessage ?: "تعذر مزامنة عربة التسوق."
          emit(emptyMap())
        }
    }
  }

  private val userOrdersFlow: Flow<List<Order>> = uidFlow.flatMapLatest { uid ->
    if (uid.isNullOrBlank()) {
      isOrdersLoadingFlow.value = false
      flowOf(emptyList())
    } else {
      isOrdersLoadingFlow.value = true
      orderRepository.observeUserOrders(uid)
        .map { orders ->
          isOrdersLoadingFlow.value = false
          orders
        }
        .catch { error ->
          isOrdersLoadingFlow.value = false
          errorMessageFlow.value = error.localizedMessage ?: "تعذر تحميل سجل الطلبات."
          emit(emptyList())
        }
    }
  }

  private val deliveryZonesFlow: Flow<List<DeliveryZone>> =
    couponAndDeliveryRepository.observeActiveDeliveryZones()
      .catch { emit(emptyList()) }

  private val paymentSettingsFlow: Flow<StorePaymentSettings> =
    couponAndDeliveryRepository.observePaymentSettings()
      .catch { emit(StorePaymentSettings()) }

  private val couponControlsFlow = combine(
    appliedCouponFlow,
    couponCodeInputFlow,
    isCouponValidatingFlow,
    couponErrorFlow,
  ) { coupon, input, validating, err ->
    CouponUiBundle(coupon, input, validating, err)
  }

  private val statusControlsFlow = combine(
    userOrdersFlow,
    isOrdersLoadingFlow,
    isSubmittingOrderFlow,
    feedbackMessageFlow,
    errorMessageFlow,
  ) { orders, loading, submitting, feedback, error ->
    StatusUiBundle(orders, loading, submitting, feedback, error)
  }

  private val deliveryOptionsFlow = combine(
    deliveryZonesFlow,
    selectedDeliveryZoneIdFlow,
    fulfillmentMethodFlow,
    storeSettingsFlow,
  ) { zones, selectedZoneId, fulfillment, storeSettings ->
    DeliveryAndSettingsBundle(zones, selectedZoneId, fulfillment, storeSettings)
  }

  private val auxiliaryFlow: Flow<CartAuxiliaryInputs> = combine(
    deliveryOptionsFlow,
    paymentSettingsFlow,
    couponControlsFlow,
    statusControlsFlow,
  ) { deliveryBundle, paySettings, couponBundle, statusBundle ->
    CartAuxiliaryInputs(
      deliveryZones = deliveryBundle.zones,
      selectedZoneId = deliveryBundle.selectedZoneId,
      fulfillmentMethod = deliveryBundle.fulfillment,
      storeSettings = deliveryBundle.storeSettings,
      paymentSettings = paySettings,
      appliedCoupon = couponBundle.coupon,
      couponInput = couponBundle.input,
      isCouponValidating = couponBundle.isValidating,
      couponError = couponBundle.error,
      orders = statusBundle.orders,
      isOrdersLoading = statusBundle.isLoading,
      isSubmittingOrder = statusBundle.isSubmitting,
      feedbackMessage = statusBundle.feedback,
      errorMessage = statusBundle.error,
    )
  }

  private data class PricingContextBundle(
    val profile: UserProfile?,
    val flashSales: List<FlashSale>,
    val brandShowcases: List<BrandShowcase>,
    val wholesalePreview: Boolean,
  )

  private val pricingContextFlow = combine(
    userProfileFlow,
    activeFlashSalesFlow,
    activeBrandShowcasesFlow,
    isWholesalePreviewActiveFlow,
  ) { profile, flashSales, brandShowcases, wholesalePreview ->
    PricingContextBundle(profile, flashSales, brandShowcases, wholesalePreview)
  }

  val uiState: StateFlow<CartAndOrdersUiState> = combine(
    rawCartEntriesFlow,
    catalogProductsFlow,
    pricingContextFlow,
    auxiliaryFlow,
  ) { cartMap, products, pricingCtx, aux ->
    val isWholesaleEffective = (pricingCtx.profile?.isWholesaleApproved == true) || pricingCtx.wholesalePreview
    val productById = products.associateBy { it.id }
    val nowMillis = System.currentTimeMillis()
    val activeFlashById = pricingCtx.flashSales
      .filter { it.isActive && it.statusAt(nowMillis) == FlashSaleStatus.ACTIVE }
      .associateBy { it.id }

    val resolvedLines = cartMap.entries.mapNotNull { (cartKey, entry) ->
      val product = productById[entry.productId] ?: return@mapNotNull null
      if (!product.isActive) return@mapNotNull null
      val safeQty = entry.quantity.coerceAtLeast(1)

      val matchedFlashSale = entry.flashSaleId?.let { activeFlashById[it] }
      val flashPrice = matchedFlashSale?.flashUnitPrice(product.id, isWholesaleEffective)

      val unitPrice = flashPrice
        ?: product.effectiveUnitPriceWithShowcases(
          isWholesaleApproved = isWholesaleEffective,
          quantity = safeQty,
          brandShowcases = pricingCtx.brandShowcases,
        )
      val pricingMode = when {
        flashPrice != null -> "flash_sale"
        isWholesaleEffective && product.wholesalePrice > 0.0 && safeQty >= product.wholesaleMinQty -> "wholesale"
        else -> "retail"
      }
      val remainingWholesale = if (
        flashPrice == null &&
        isWholesaleEffective &&
        product.wholesalePrice > 0.0 &&
        safeQty < product.wholesaleMinQty
      ) {
        product.wholesaleMinQty - safeQty
      } else {
        0
      }
      ResolvedCartLine(
        cartItemId = cartKey,
        product = product,
        quantity = safeQty,
        unitPrice = roundMoney(unitPrice),
        appliedPricingMode = pricingMode,
        lineTotal = roundMoney(unitPrice * safeQty),
        remainingForWholesale = remainingWholesale,
        flashSaleId = if (flashPrice != null) matchedFlashSale?.id else null,
        flashSaleTitle = if (flashPrice != null) matchedFlashSale?.title else null,
        selectedColor = entry.selectedColor,
        selectedSize = entry.selectedSize,
      )
    }

    val subtotal = roundMoney(resolvedLines.sumOf { it.lineTotal })
    val validCoupon = aux.appliedCoupon?.takeIf { subtotal >= it.minOrderAmount && subtotal > 0.0 }
    val discount = validCoupon?.calculateDiscount(subtotal) ?: 0.0
    val activeZone = aux.deliveryZones.firstOrNull { it.id == aux.selectedZoneId }
      ?: aux.deliveryZones.firstOrNull()
    val netMerchandise = roundMoney((subtotal - discount).coerceAtLeast(0.0))
    val freeShippingEval = computeFreeShipping(
      netMerchandise = netMerchandise,
      isStorePickup = aux.fulfillmentMethod == FulfillmentMethod.STORE_PICKUP,
      hasItems = resolvedLines.isNotEmpty(),
      baseZoneFee = activeZone?.fee ?: 0.0,
      enabled = aux.storeSettings.freeShippingEnabled,
      threshold = aux.storeSettings.freeShippingThreshold,
    )
    val deliveryFee = freeShippingEval.deliveryFee
    val totalAmount = roundMoney((subtotal - discount + deliveryFee).coerceAtLeast(0.0))

    CartAndOrdersUiState(
      cartSummary = CartSummary(
        lines = resolvedLines,
        subtotal = subtotal,
        discountAmount = discount,
        deliveryFee = deliveryFee,
        totalAmount = totalAmount,
        totalItemsCount = resolvedLines.sumOf { it.quantity },
        hasStockConflicts = resolvedLines.any { it.hasStockIssue },
        freeShippingEnabled = freeShippingEval.freeShippingEnabled,
        freeShippingThreshold = freeShippingEval.freeShippingThreshold,
        remainingForFreeShipping = freeShippingEval.remainingForFreeShipping,
        freeShippingProgress = freeShippingEval.freeShippingProgress,
        qualifiesForFreeShipping = freeShippingEval.qualifiesForFreeShipping,
      ),
      appliedCoupon = validCoupon,
      couponCodeInput = aux.couponInput,
      isCouponValidating = aux.isCouponValidating,
      couponError = aux.couponError,
      deliveryZones = aux.deliveryZones,
      selectedDeliveryZoneId = activeZone?.id,
      fulfillmentMethod = aux.fulfillmentMethod,
      paymentSettings = aux.paymentSettings,
      orders = aux.orders,
      isOrdersLoading = aux.isOrdersLoading,
      isSubmittingOrder = aux.isSubmittingOrder,
      feedbackMessage = aux.feedbackMessage,
      errorMessage = aux.errorMessage,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = CartAndOrdersUiState(),
  )

  fun syncSessionContext(
    uid: String?,
    userProfile: UserProfile?,
    products: List<Product>,
    flashSales: List<FlashSale> = emptyList(),
    brandShowcases: List<BrandShowcase> = activeBrandShowcasesFlow.value,
    isWholesalePreviewActive: Boolean = false,
    storeSettings: StoreGeneralSettings = storeSettingsFlow.value,
  ) {
    catalogProductsFlow.value = products
    userProfileFlow.value = userProfile
    activeFlashSalesFlow.value = flashSales
    activeBrandShowcasesFlow.value = brandShowcases
    isWholesalePreviewActiveFlow.value = isWholesalePreviewActive
    storeSettingsFlow.value = storeSettings
    val previousUid = uidFlow.value
    uidFlow.value = uid

    if (uid.isNullOrBlank()) {
      lastMergedUid = null
      appliedCouponFlow.value = null
    } else if (uid != previousUid && lastMergedUid != uid) {
      lastMergedUid = uid
      viewModelScope.launch {
        val localEntries = localCartStore.consumeAllEntries()
        if (localEntries.isNotEmpty()) {
          cartRepository.mergeLocalEntries(uid, localEntries)
            .onSuccess {
              feedbackMessageFlow.value = "تم نقل منتجات السلة إلى حسابك بنجاح"
            }
        }
      }
    }
  }

  fun addToCart(
    product: Product,
    quantityToAdd: Int = 1,
    flashSaleId: String? = null,
    selectedColor: String? = null,
    selectedSize: String? = null,
  ) {
    if (quantityToAdd <= 0) return
    if (product.isOutOfStock) {
      errorMessageFlow.value = "عفوًا، نفد مخزون «${product.name}» حاليًا."
      return
    }

    val cleanColor = selectedColor?.trim()?.takeIf { it.isNotEmpty() }
    val cleanSize = selectedSize?.trim()?.takeIf { it.isNotEmpty() }
    val targetCartKey = buildCartItemId(product.id, cleanColor, cleanSize)
    val existingLine = uiState.value.cartSummary.lines.firstOrNull {
      it.effectiveCartItemId == targetCartKey
    }
    val currentQty = existingLine?.quantity ?: 0
    val targetQty = (currentQty + quantityToAdd).coerceAtMost(product.stockQuantity)

    val effectiveFlashId = flashSaleId?.trim()?.takeIf { it.isNotEmpty() } ?: existingLine?.flashSaleId
    val flashItem = effectiveFlashId?.let { fid ->
      activeFlashSalesFlow.value.firstOrNull { it.id == fid && it.statusAt() == FlashSaleStatus.ACTIVE }
        ?.itemFor(product.id)
    }
    val maxFlashQty = flashItem?.maxQtyPerOrder
    if (maxFlashQty != null && maxFlashQty > 0 && targetQty > maxFlashQty) {
      errorMessageFlow.value = "الحد الأقصى للطلب من «${product.name}» في الفلاش سيل هو $maxFlashQty ${product.unit}."
      return
    }

    if (targetQty == currentQty && currentQty >= product.stockQuantity) {
      errorMessageFlow.value = "الحد الأقصى المتاح في المخزون من «${product.name}» هو ${product.stockQuantity} ${product.unit}."
      return
    }

    viewModelScope.launch {
      val uid = uidFlow.value
      val result = if (uid.isNullOrBlank()) {
        localCartStore.upsertItem(
          productId = product.id,
          quantity = targetQty,
          flashSaleId = effectiveFlashId,
          selectedColor = cleanColor,
          selectedSize = cleanSize,
          cartItemId = targetCartKey,
        )
        Result.success(Unit)
      } else {
        cartRepository.setQuantity(
          uid = uid,
          productId = product.id,
          quantity = targetQty,
          flashSaleId = effectiveFlashId,
          selectedColor = cleanColor,
          selectedSize = cleanSize,
          cartItemId = targetCartKey,
        )
      }
      result
        .onSuccess {
          feedbackMessageFlow.value = if (effectiveFlashId != null && flashItem != null) {
            "تمت إضافة «${product.name}» إلى السلة بسعر الفلاش سيل ⚡"
          } else {
            "تمت إضافة «${product.name}» إلى السلة"
          }
        }
        .onFailure { err ->
          errorMessageFlow.value = err.localizedMessage ?: "تعذر إضافة المنتج إلى السلة."
        }
    }
  }

  fun updateQuantity(
    product: Product,
    newQuantity: Int,
    cartItemId: String = buildCartItemId(product.id),
  ) {
    val uid = uidFlow.value
    val existingLine = uiState.value.cartSummary.lines.firstOrNull {
      it.effectiveCartItemId == cartItemId
    } ?: uiState.value.cartSummary.lines.firstOrNull { it.product.id == product.id }
    val resolvedKey = existingLine?.effectiveCartItemId ?: cartItemId
    val preservedFlashId = existingLine?.flashSaleId
    val preservedColor = existingLine?.selectedColor
    val preservedSize = existingLine?.selectedSize
    viewModelScope.launch {
      if (newQuantity <= 0) {
        removeItem(resolvedKey)
        return@launch
      }
      val flashItem = preservedFlashId?.let { fid ->
        activeFlashSalesFlow.value.firstOrNull { it.id == fid && it.statusAt() == FlashSaleStatus.ACTIVE }
          ?.itemFor(product.id)
      }
      val maxFlashQty = flashItem?.maxQtyPerOrder
      if (maxFlashQty != null && maxFlashQty > 0 && newQuantity > maxFlashQty) {
        errorMessageFlow.value = "الحد الأقصى للطلب في الفلاش سيل هو $maxFlashQty ${product.unit}."
        return@launch
      }
      val clamped = min(newQuantity, product.stockQuantity.coerceAtLeast(1))
      if (newQuantity > product.stockQuantity) {
        errorMessageFlow.value = "المتاح في المخزون حاليًا ${product.stockQuantity} ${product.unit} فقط."
      }
      val result = if (uid.isNullOrBlank()) {
        localCartStore.upsertItem(
          productId = product.id,
          quantity = clamped,
          flashSaleId = preservedFlashId,
          selectedColor = preservedColor,
          selectedSize = preservedSize,
          cartItemId = resolvedKey,
        )
        Result.success(Unit)
      } else {
        cartRepository.setQuantity(
          uid = uid,
          productId = product.id,
          quantity = clamped,
          flashSaleId = preservedFlashId,
          selectedColor = preservedColor,
          selectedSize = preservedSize,
          cartItemId = resolvedKey,
        )
      }
      result.onFailure { err ->
        errorMessageFlow.value = err.localizedMessage ?: "تعذر تحديث الكمية."
      }
    }
  }

  fun removeItem(cartItemId: String) {
    val uid = uidFlow.value
    viewModelScope.launch {
      val result = if (uid.isNullOrBlank()) {
        localCartStore.removeItem(cartItemId)
        Result.success(Unit)
      } else {
        cartRepository.removeItem(uid, cartItemId)
      }
      result.onFailure { err ->
        errorMessageFlow.value = err.localizedMessage ?: "تعذر حذف المنتج من السلة."
      }
    }
  }

  fun updateCouponInput(value: String) {
    couponCodeInputFlow.value = value
    couponErrorFlow.value = null
  }

  fun applyCoupon(code: String = couponCodeInputFlow.value) {
    val cleanCode = code.trim().uppercase()
    if (cleanCode.length < 2) {
      couponErrorFlow.value = "أدخل كود خصم صالحًا"
      return
    }
    if (uidFlow.value.isNullOrBlank()) {
      couponErrorFlow.value = "يرجى تسجيل الدخول أولًا لتطبيق أكواد الخصم"
      return
    }
    val subtotal = uiState.value.cartSummary.subtotal
    viewModelScope.launch {
      isCouponValidatingFlow.value = true
      couponErrorFlow.value = null
      couponAndDeliveryRepository.validateCoupon(cleanCode, subtotal)
        .onSuccess { coupon ->
          appliedCouponFlow.value = coupon
          couponCodeInputFlow.value = coupon.code.uppercase()
          feedbackMessageFlow.value = "تم تفعيل كود الخصم (${coupon.code})"
        }
        .onFailure { err ->
          appliedCouponFlow.value = null
          couponErrorFlow.value = err.localizedMessage ?: "كود الخصم غير صالح."
        }
      isCouponValidatingFlow.value = false
    }
  }

  fun removeCoupon() {
    appliedCouponFlow.value = null
    couponCodeInputFlow.value = ""
    couponErrorFlow.value = null
  }

  fun selectDeliveryZone(zoneId: String?) {
    selectedDeliveryZoneIdFlow.value = zoneId
  }

  fun selectFulfillmentMethod(method: FulfillmentMethod) {
    fulfillmentMethodFlow.value = method
  }

  fun autoMatchDeliveryZone(address: DeliveryAddress?) {
    if (address == null) return
    if (selectedDeliveryZoneIdFlow.value != null) return
    val zones = uiState.value.deliveryZones
    val matched = zones.firstOrNull {
      it.governorate.contains(address.governorate, ignoreCase = true) ||
        address.governorate.contains(it.governorate, ignoreCase = true)
    }
    if (matched != null) {
      selectedDeliveryZoneIdFlow.value = matched.id
    }
  }

  fun placeOrder(
    fulfillmentMethod: FulfillmentMethod = uiState.value.fulfillmentMethod,
    address: DeliveryAddress?,
    paymentMethod: PaymentMethod,
    transferReference: String?,
    receiptImageUrl: String? = null,
    notes: String?,
    pickupRecipientName: String? = null,
    pickupPhone: String? = null,
    storeAddress: String = "",
    onSuccess: (orderId: String) -> Unit,
  ) {
    val snapshotState = uiState.value
    val lines = snapshotState.cartSummary.lines
    if (lines.isEmpty()) {
      errorMessageFlow.value = "عربة التسوق فارغة، أضف منتجات أولًا."
      return
    }
    if (snapshotState.cartSummary.hasStockConflicts) {
      errorMessageFlow.value = "يرجى مراجعة الكميات المتوفرة في السلة قبل إرسال الطلب."
      return
    }
    if (fulfillmentMethod == FulfillmentMethod.DELIVERY && address == null) {
      errorMessageFlow.value = "يرجى اختيار عنوان التوصيل أو التحويل للاستلام من المتجر."
      return
    }
    if (fulfillmentMethod == FulfillmentMethod.STORE_PICKUP && pickupPhone.orEmpty().trim().length < 6) {
      errorMessageFlow.value = "يرجى إدخال رقم هاتف صحيح للتواصل عند جاهزية الطلب بالمتجر."
      return
    }
    if (paymentMethod.requiresTransferReference && transferReference.isNullOrBlank() && receiptImageUrl.isNullOrBlank()) {
      errorMessageFlow.value = "يرجى إدخال الرقم المحوَّل منه أو إرفاق صورة الإيصال لطريقة الدفع المختارة."
      return
    }
    val normalizedSender = transferReference?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
      val digits = normalizeEgyptianPhone(raw)
      if (digits.length >= 10) digits else raw
    }

    viewModelScope.launch {
      isSubmittingOrderFlow.value = true
      orderRepository.placeOrder(
        lines = lines.map {
          CheckoutLineRequest(
            productId = it.product.id,
            quantity = it.quantity,
            flashSaleId = it.flashSaleId,
            selectedColor = it.selectedColor,
            selectedSize = it.selectedSize,
            cartItemId = it.effectiveCartItemId,
          )
        },
        fulfillmentMethod = fulfillmentMethod,
        address = address,
        deliveryZone = snapshotState.selectedDeliveryZone,
        couponId = snapshotState.appliedCoupon?.id,
        paymentMethod = paymentMethod,
        transferReference = normalizedSender ?: transferReference,
        senderPhone = normalizedSender,
        receiptImageUrl = receiptImageUrl,
        notes = notes,
        pickupRecipientName = pickupRecipientName,
        pickupPhone = pickupPhone,
        storeAddress = storeAddress,
        cartItemIdsToClear = lines.map { it.effectiveCartItemId },
      ).onSuccess { orderId ->
        appliedCouponFlow.value = null
        couponCodeInputFlow.value = ""
        couponErrorFlow.value = null
        fulfillmentMethodFlow.value = FulfillmentMethod.DELIVERY
        feedbackMessageFlow.value = "تم استلام طلبك بنجاح!"
        onSuccess(orderId)
      }.onFailure { err ->
        errorMessageFlow.value = err.localizedMessage ?: "تعذر إتمام الطلب حاليًا، حاول مرة أخرى."
      }
      isSubmittingOrderFlow.value = false
    }
  }

  fun cancelOrder(orderId: String, reason: String? = null) {
    viewModelScope.launch {
      orderRepository.cancelOrderByCustomer(orderId, reason)
        .onSuccess {
          feedbackMessageFlow.value = "تم إلغاء الطلب بنجاح"
        }
        .onFailure { err ->
          errorMessageFlow.value = err.localizedMessage ?: "تعذر إلغاء الطلب في هذه المرحلة."
        }
    }
  }

  fun reorder(order: Order, onFinished: () -> Unit) {
    val uid = uidFlow.value
    val productMap = catalogProductsFlow.value.associateBy { it.id }
    val currentCartMap = uiState.value.cartSummary.lines.associate { it.effectiveCartItemId to it.quantity }

    viewModelScope.launch {
      var addedCount = 0
      var skippedCount = 0
      order.items.forEach { snapshotItem ->
        val liveProduct = productMap[snapshotItem.productId]
        if (liveProduct != null && liveProduct.isActive && liveProduct.stockQuantity > 0) {
          val key = buildCartItemId(liveProduct.id, snapshotItem.selectedColor, snapshotItem.selectedSize)
          val existingQty = currentCartMap[key] ?: 0
          val targetQty = (existingQty + snapshotItem.quantity).coerceAtMost(liveProduct.stockQuantity)
          if (uid.isNullOrBlank()) {
            localCartStore.upsertItem(
              productId = liveProduct.id,
              quantity = targetQty,
              selectedColor = snapshotItem.selectedColor,
              selectedSize = snapshotItem.selectedSize,
              cartItemId = key,
            )
            addedCount++
          } else {
            val res = cartRepository.setQuantity(
              uid = uid,
              productId = liveProduct.id,
              quantity = targetQty,
              selectedColor = snapshotItem.selectedColor,
              selectedSize = snapshotItem.selectedSize,
              cartItemId = key,
            )
            if (res.isSuccess) addedCount++ else skippedCount++
          }
        } else {
          skippedCount++
        }
      }

      if (addedCount > 0) {
        feedbackMessageFlow.value = if (skippedCount == 0) {
          "تمت إضافة منتجات الطلب إلى السلة بالأسعار الحالية"
        } else {
          "تمت إضافة $addedCount منتج للسلة، و$skippedCount غير متاح حاليًا"
        }
        onFinished()
      } else {
        errorMessageFlow.value = "عذرًا، منتجات هذا الطلب غير متوفرة حاليًا في المتجر."
      }
    }
  }

  fun clearMessages() {
    feedbackMessageFlow.update { null }
    errorMessageFlow.update { null }
  }

  private fun roundMoney(value: Double): Double = (value * 100.0).roundToLong() / 100.0

  private data class CouponUiBundle(
    val coupon: Coupon?,
    val input: String,
    val isValidating: Boolean,
    val error: String?,
  )

  private data class StatusUiBundle(
    val orders: List<Order>,
    val isLoading: Boolean,
    val isSubmitting: Boolean,
    val feedback: String?,
    val error: String?,
  )

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val appContext = context.applicationContext
      return CartOrderViewModel(
        localCartStore = LocalCartStore(appContext),
        cartRepository = CartRepository(appContext),
        couponAndDeliveryRepository = CouponAndDeliveryRepository(appContext),
        orderRepository = OrderRepository(appContext),
      ) as T
    }
  }
}
