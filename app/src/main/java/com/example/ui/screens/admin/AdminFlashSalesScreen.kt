package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.util.ArabicFormatter
import com.example.data.model.Category
import com.example.data.model.FlashSale
import com.example.data.model.FlashSaleProductItem
import com.example.data.model.FlashSaleStatus
import com.example.data.model.Product
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminFlashSalesUiState
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminFlashSalesScreen(
  uiState: AdminFlashSalesUiState,
  onSaveFlashSale: (FlashSale, () -> Unit) -> Unit,
  onToggleActive: (FlashSale) -> Unit,
  onDeleteFlashSale: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingSale by remember { mutableStateOf<FlashSale?>(null) }
  var showDialog by remember { mutableStateOf(false) }
  var deletingSale by remember { mutableStateOf<FlashSale?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1_000L)
      nowMillis = System.currentTimeMillis()
    }
  }

  LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
    val msg = uiState.feedbackMessage ?: uiState.errorMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onClearMessages()
    }
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { AlEmanSnackbarHost(hostState = snackbarHostState) },
    topBar = {
      AlEmanTopBar(
        title = "إدارة الفلاش سيل ⚡",
        subtitle = "عروض سريعة بأسعار مستقلة تمامًا عن أسعار المتجر",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingSale = null
          showDialog = true
        },
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier.testTag("admin_add_flash_sale_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "إضافة فلاش سيل جديد")
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when {
        uiState.isLoading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CharcoalBlack)
          }
        }

        uiState.flashSales.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.Bolt,
              title = "لا توجد عروض فلاش سيل حتى الآن",
              description = "يمكنك إنشاء أكثر من فلاش سيل بأوقات بدء وانتهاء محددة واختيار منتجات بأسعار مخفضة لا تؤثر على أسعار المتجر العادية.",
              actionLabel = "إنشاء فلاش سيل جديد",
              onActionClick = {
                editingSale = null
                showDialog = true
              },
            )
          }
        }

        else -> {
          LazyColumn(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
            contentPadding = PaddingValues(
              start = AlEmanDimens.SpaceLg,
              end = AlEmanDimens.SpaceLg,
              top = AlEmanDimens.SpaceLg,
              bottom = 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(uiState.flashSales, key = { it.id }) { sale ->
              AdminFlashSaleCard(
                sale = sale,
                nowMillis = nowMillis,
                products = uiState.products,
                onToggleActive = { onToggleActive(sale) },
                onEdit = {
                  editingSale = sale
                  showDialog = true
                },
                onDelete = { deletingSale = sale },
              )
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    FlashSaleEditorDialog(
      initial = editingSale,
      catalogProducts = uiState.products,
      categories = uiState.categories,
      isSubmitting = uiState.isSubmitting,
      onDismiss = { showDialog = false },
      onSave = { sale ->
        onSaveFlashSale(sale) { showDialog = false }
      },
    )
  }

  val deleteTarget = deletingSale
  if (deleteTarget != null) {
    AlertDialog(
      onDismissRequest = { deletingSale = null },
      containerColor = PureWhite,
      title = { Text("حذف عرض الفلاش سيل", fontWeight = FontWeight.Bold, color = CharcoalBlack) },
      text = { Text("هل تريد بالتأكيد حذف عرض «${deleteTarget.title}»؟") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteFlashSale(deleteTarget.id)
            deletingSale = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
        ) {
          Text("حذف")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingSale = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@Composable
private fun AdminFlashSaleCard(
  sale: FlashSale,
  nowMillis: Long,
  products: List<Product>,
  onToggleActive: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val status = sale.statusAt(nowMillis)
  val remainingMs = sale.remainingMillisAt(nowMillis)
  val sdf = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.forLanguageTag("ar-EG")) }

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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = if (status == FlashSaleStatus.UPCOMING) Icons.Filled.Lock else Icons.Filled.Bolt,
              contentDescription = null,
              tint = if (status == FlashSaleStatus.ACTIVE) RoyalBlue else CharcoalBlack,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = sale.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
          }
          if (sale.badgeText.isNotBlank()) {
            BrandPillBadge(
              text = sale.badgeText,
              containerColor = PrimaryYellowSoft,
              contentColor = CharcoalBlack,
            )
          }
        }
        when {
          !sale.isActive -> DiscountBadge("متوقف يدويًا")
          status == FlashSaleStatus.ACTIVE -> SuccessBadge("مفتوح الآن")
          status == FlashSaleStatus.UPCOMING -> BrandPillBadge(
            text = "مغلق حتى الموعد",
            containerColor = RoyalBlueSoft,
            contentColor = RoyalBlue,
          )
          else -> DiscountBadge("تم انتهاء العرض")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "يبدأ: ${sale.startsAt?.toDate()?.let { sdf.format(it) } ?: "الآن"}",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
        Text(
          text = "ينتهي: ${sale.endsAt?.toDate()?.let { sdf.format(it) } ?: "-"}",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }

      if (status != FlashSaleStatus.ENDED) {
        Text(
          text = (if (status == FlashSaleStatus.UPCOMING) "يفتح بعد: " else "متبقي على الانتهاء: ") +
            formatFlashCountdown(remainingMs),
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = RoyalBlue,
        )
      }

      Text(
        text = "عدد المنتجات المشمولة: ${sale.items.size} منتج (بأسعار مستقلة)",
        style = MaterialTheme.typography.bodySmall,
        color = TextPrimary,
      )

      sale.items.take(3).forEach { entry ->
        val prodName = products.firstOrNull { it.id == entry.productId }?.name ?: "منتج #${entry.productId.take(6)}"
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("• $prodName", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
          Text(
            text = "فلاش: ${ArabicFormatter.formatCurrency(entry.flashRetailPrice)}" +
              (entry.flashWholesalePrice?.let { " | جملة: ${ArabicFormatter.formatCurrency(it)}" } ?: ""),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
        }
      }

      HorizontalDivider(color = BorderNeutral)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("تفعيل", style = MaterialTheme.typography.labelMedium)
          Switch(
            checked = sale.isActive,
            onCheckedChange = { onToggleActive() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CharcoalBlack,
              checkedTrackColor = PrimaryYellow,
            ),
          )
        }
        Row {
          IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_flash_sale_${sale.id}")) {
            Icon(Icons.Filled.Edit, contentDescription = "تعديل الفلاش سيل", tint = CharcoalBlack)
          }
          IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_flash_sale_${sale.id}")) {
            Icon(Icons.Filled.Delete, contentDescription = "حذف الفلاش سيل", tint = DiscountRed)
          }
        }
      }
    }
  }
}

private data class DraftFlashItem(
  val product: Product,
  var flashRetailPrice: String,
  var flashWholesalePrice: String,
  var maxQtyPerOrder: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashSaleEditorDialog(
  initial: FlashSale?,
  catalogProducts: List<Product>,
  categories: List<Category>,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSave: (FlashSale) -> Unit,
) {
  var title by remember(initial) { mutableStateOf(initial?.title ?: "عروض فلاش سيل") }
  var badgeText by remember(initial) { mutableStateOf(initial?.badgeText ?: "كمية محدودة ⚡") }
  var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }

  // مواعيد البدء والانتهاء بالتحكم السريع (دقائق/ساعات من الآن أو إزاحات واضحة)
  val now = System.currentTimeMillis()
  var startAtMillis by remember(initial) {
    mutableStateOf(initial?.startsAt?.toDate()?.time ?: now)
  }
  var endAtMillis by remember(initial) {
    mutableStateOf(initial?.endsAt?.toDate()?.time ?: (now + 4 * 60 * 60 * 1000L))
  }

  val draftItems = remember(initial, catalogProducts) {
    mutableStateListOf<DraftFlashItem>().apply {
      initial?.items.orEmpty().forEach { entry ->
        val prod = catalogProducts.firstOrNull { it.id == entry.productId }
        if (prod != null) {
          add(
            DraftFlashItem(
              product = prod,
              flashRetailPrice = entry.flashRetailPrice.toString(),
              flashWholesalePrice = entry.flashWholesalePrice?.toString().orEmpty(),
              maxQtyPerOrder = entry.maxQtyPerOrder?.toString().orEmpty(),
            ),
          )
        }
      }
    }
  }

  var productSearch by remember { mutableStateOf("") }
  var selectedCategoryId by remember { mutableStateOf<String?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }
  val sdf = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.forLanguageTag("ar-EG")) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (initial == null) "إضافة فلاش سيل جديد" else "تعديل الفلاش سيل",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("اسم الفلاش سيل (مثال: ساعة التوفير)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("flash_sale_title_input"),
        )

        OutlinedTextField(
          value = badgeText,
          onValueChange = { badgeText = it },
          label = { Text("شارة وقت أو عرض (مثال: من 5-9 مساءً)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // تحديد موعد البدء
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.Schedule, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
              Text(
                text = "موعد الفتح: ${sdf.format(Date(startAtMillis))}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              val presets = listOf(
                "يبدأ الآن" to 0L,
                "بعد 10 دقائق" to 10 * 60_000L,
                "بعد ساعة" to 3600_000L,
                "بعد 3 ساعات" to 3 * 3600_000L,
                "بعد 24 ساعة" to 24 * 3600_000L,
              )
              presets.forEach { (label, offsetMs) ->
                FilterChip(
                  selected = false,
                  onClick = {
                    val baseNow = System.currentTimeMillis()
                    startAtMillis = baseNow + offsetMs
                    if (endAtMillis <= startAtMillis) {
                      endAtMillis = startAtMillis + 4 * 3600_000L
                    }
                  },
                  label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(
                onClick = { startAtMillis = (startAtMillis - 30 * 60_000L).coerceAtLeast(System.currentTimeMillis() - 60_000L) },
                modifier = Modifier.weight(1f),
              ) { Text("-30 د") }
              OutlinedButton(
                onClick = {
                  startAtMillis += 30 * 60_000L
                  if (endAtMillis <= startAtMillis) endAtMillis = startAtMillis + 2 * 3600_000L
                },
                modifier = Modifier.weight(1f),
              ) { Text("+30 د") }
            }
          }
        }

        // تحديد موعد الانتهاء
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "موعد الانتهاء: ${sdf.format(Date(endAtMillis))}",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(
                "مدة ساعة" to 3600_000L,
                "مدة 4 ساعات" to 4 * 3600_000L,
                "مدة 12 ساعة" to 12 * 3600_000L,
                "مدة 24 ساعة" to 24 * 3600_000L,
              ).forEach { (label, durationMs) ->
                FilterChip(
                  selected = false,
                  onClick = { endAtMillis = startAtMillis + durationMs },
                  label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(
                onClick = {
                  val nextEnd = endAtMillis - 30 * 60_000L
                  if (nextEnd > startAtMillis + 5 * 60_000L) endAtMillis = nextEnd
                },
                modifier = Modifier.weight(1f),
              ) { Text("-30 د") }
              OutlinedButton(
                onClick = { endAtMillis += 30 * 60_000L },
                modifier = Modifier.weight(1f),
              ) { Text("+30 د") }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("تفعيل العرض مباشرة", style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = isActive,
            onCheckedChange = { isActive = it },
            colors = SwitchDefaults.colors(checkedThumbColor = CharcoalBlack, checkedTrackColor = PrimaryYellow),
          )
        }

        HorizontalDivider(color = BorderNeutral)

        Text(
          text = "منتجات الفلاش سيل (${draftItems.size})",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        Text(
          text = "سعر المنتج هنا يُطبق داخل الفلاش سيل فقط ولا يغيّر سعر المنتج الأساسي في المتجر.",
          style = MaterialTheme.typography.labelSmall,
          color = RoyalBlue,
        )

        draftItems.forEachIndexed { index, draft ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = PrimaryYellowSoft,
            border = BorderStroke(1.dp, PrimaryYellow),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProductThumbnail(
                  imageUrl = draft.product.imageUrls.firstOrNull(),
                  contentDescription = draft.product.name,
                  modifier = Modifier.size(38.dp).clip(RoundedCornerShape(6.dp)),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = draft.product.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                  Text(
                    text = "السعر العادي: ${ArabicFormatter.formatCurrency(draft.product.retailPrice)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                }
                IconButton(onClick = { draftItems.removeAt(index) }) {
                  Icon(Icons.Filled.Delete, contentDescription = "إزالة المنتج", tint = DiscountRed, modifier = Modifier.size(18.dp))
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                  value = draft.flashRetailPrice,
                  onValueChange = { value -> draftItems[index] = draft.copy(flashRetailPrice = value) },
                  label = { Text("سعر الفلاش") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                  value = draft.flashWholesalePrice,
                  onValueChange = { value -> draftItems[index] = draft.copy(flashWholesalePrice = value) },
                  label = { Text("جملة (اختياري)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
              }
              OutlinedTextField(
                value = draft.maxQtyPerOrder,
                onValueChange = { value -> draftItems[index] = draft.copy(maxQtyPerOrder = value) },
                label = { Text("الحد الأقصى للطلب (اختياري)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }

        // اختيار أي منتج فردي من المتجر مع تصفية اختيارية حسب القسم أو البحث بالاسم/الكود
        Text(
          text = "إضافة منتجات منفردة من المتجر",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )

        OutlinedTextField(
          value = productSearch,
          onValueChange = { productSearch = it },
          label = { Text("ابحث بالاسم أو SKU أو الباركود لإضافة أي منتج...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("flash_sale_product_search"),
        )

        if (categories.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = selectedCategoryId == null,
              onClick = { selectedCategoryId = null },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryYellow,
                selectedLabelColor = CharcoalBlack,
              ),
              label = { Text("كل المنتجات", style = MaterialTheme.typography.labelSmall) },
            )
            categories.forEach { cat ->
              FilterChip(
                selected = selectedCategoryId == cat.id,
                onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryYellow,
                  selectedLabelColor = CharcoalBlack,
                ),
                label = { Text(cat.name, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
              )
            }
          }
        }

        val candidates = remember(catalogProducts, productSearch, selectedCategoryId) {
          val q = productSearch.trim().lowercase()
          catalogProducts.filter { prod ->
            val matchesCategory = selectedCategoryId == null ||
              prod.categoryId == selectedCategoryId ||
              prod.subCategoryId == selectedCategoryId
            val matchesQuery = q.isEmpty() ||
              prod.name.lowercase().contains(q) ||
              prod.sku.lowercase().contains(q) ||
              prod.barcode.orEmpty().lowercase().contains(q)
            prod.isActive && matchesCategory && matchesQuery
          }
        }

        Text(
          text = "النتائج المتاحة: ${candidates.size} منتج (اضغط على أي منتج لإضافته منفردًا)",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 240.dp)
              .verticalScroll(rememberScrollState())
              .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (candidates.isEmpty()) {
              Text(
                text = "لا توجد منتجات مطابقة للبحث الحالي.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(10.dp),
              )
            } else {
              candidates.forEach { prod ->
                val selected = draftItems.any { it.product.id == prod.id }
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) PrimaryYellowSoft else PureWhite)
                    .clickable {
                      if (selected) {
                        draftItems.removeAll { it.product.id == prod.id }
                      } else {
                        val suggestedRetail = (prod.retailPrice * 0.85).let { String.format(Locale.US, "%.2f", it) }
                        val suggestedWholesale = if (prod.wholesalePrice > 0.0) {
                          String.format(Locale.US, "%.2f", prod.wholesalePrice * 0.9)
                        } else {
                          ""
                        }
                        draftItems.add(
                          DraftFlashItem(
                            product = prod,
                            flashRetailPrice = suggestedRetail,
                            flashWholesalePrice = suggestedWholesale,
                            maxQtyPerOrder = "",
                          ),
                        )
                      }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("flash_sale_pick_${prod.id}"),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Checkbox(checked = selected, onCheckedChange = null)
                  ProductThumbnail(
                    imageUrl = prod.imageUrls.firstOrNull(),
                    contentDescription = prod.name,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(6.dp)),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = prod.name,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${ArabicFormatter.formatCurrency(prod.retailPrice)} • ${prod.sku}",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextSecondary,
                      maxLines = 1,
                    )
                  }
                  if (selected) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "تمت الإضافة", tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }

        if (!validationError.isNullOrBlank()) {
          Text(validationError.orEmpty(), color = DiscountRed, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val resolvedItems = draftItems.mapNotNull { d ->
            val rPrice = d.flashRetailPrice.toDoubleOrNull()
            if (rPrice == null || rPrice <= 0.0) return@mapNotNull null
            FlashSaleProductItem(
              productId = d.product.id,
              flashRetailPrice = rPrice,
              flashWholesalePrice = d.flashWholesalePrice.toDoubleOrNull()?.takeIf { it > 0.0 },
              maxQtyPerOrder = d.maxQtyPerOrder.toIntOrNull()?.takeIf { it > 0 },
            )
          }
          when {
            title.isBlank() -> validationError = "يرجى إدخال اسم للفلاش سيل."
            endAtMillis <= startAtMillis -> validationError = "يجب أن يكون موعد الانتهاء بعد موعد البدء."
            resolvedItems.isEmpty() -> validationError = "يرجى اختيار منتج واحد على الأقل وتحديد سعر الفلاش سيل له."
            else -> {
              validationError = null
              onSave(
                FlashSale(
                  id = initial?.id.orEmpty(),
                  title = title.trim(),
                  badgeText = badgeText.trim(),
                  startsAt = Timestamp(Date(startAtMillis)),
                  endsAt = Timestamp(Date(endAtMillis)),
                  isActive = isActive,
                  items = resolvedItems,
                ),
              )
            }
          }
        },
        enabled = !isSubmitting,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
        modifier = Modifier.testTag("save_flash_sale_dialog_button"),
      ) {
        Text("حفظ الفلاش سيل", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}

private fun formatFlashCountdown(remainingMs: Long): String {
  val totalSec = (remainingMs / 1000L).coerceAtLeast(0L)
  val hours = totalSec / 3600L
  val mins = (totalSec % 3600L) / 60L
  val secs = totalSec % 60L
  return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
}
