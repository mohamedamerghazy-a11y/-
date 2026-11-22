package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.core.util.ArabicFormatter
import com.example.data.media.CloudinaryFolder
import com.example.data.model.BannerPlacement
import com.example.data.model.BrandShowcase
import com.example.data.model.BrandShowcaseCategory
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.StoreCompany
import com.example.data.model.matchesBrandShowcase
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.FullAspectBannerImage
import com.example.ui.components.ImagePickerUploadModal
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminBrandShowcasesUiState

private val BrandAccentPresets = listOf(
  "#FCE4EC" to "وردي ناعم",
  "#FFF9DB" to "أصفر نون",
  "#E8F4FD" to "أزرق سماوي",
  "#ECFDF5" to "أخضر نعناعي",
  "#F3E8FF" to "بنفسجي هادئ",
  "#FFF3E0" to "برتقالي دافئ",
)

@Composable
fun AdminBrandShowcasesScreen(
  uiState: AdminBrandShowcasesUiState,
  onSaveShowcase: (BrandShowcase, ((String) -> Unit)?) -> Unit,
  onToggleShowcaseActive: (BrandShowcase) -> Unit,
  onMoveShowcaseOrder: (BrandShowcase, Int) -> Unit,
  onSaveShowcaseCategory: (BrandShowcase, BrandShowcaseCategory) -> Unit,
  onDeleteShowcaseCategory: (BrandShowcase, String) -> Unit,
  onSaveShowcaseProduct: (BrandShowcase, Product) -> Unit,
  onLinkExistingProduct: (BrandShowcase, Product, String, Boolean) -> Unit,
  onUnlinkProduct: (Product) -> Unit,
  onToggleProductExclusive: (Product) -> Unit,
  onDeleteShowcase: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingShowcase by remember { mutableStateOf<BrandShowcase?>(null) }
  var showShowcaseDialog by remember { mutableStateOf(false) }
  var managingShowcaseId by remember { mutableStateOf<String?>(null) }
  var deletingShowcase by remember { mutableStateOf<BrandShowcase?>(null) }

  val managingShowcase = remember(uiState.showcases, managingShowcaseId) {
    managingShowcaseId?.let { id -> uiState.showcases.firstOrNull { it.id == id } }
  }

  val snackbarHostState = remember { SnackbarHostState() }
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
        title = "منتجات الشركات",
        subtitle = "اختر شركة لتظهر منتجاتها تلقائيًا بتصميم متناوب أو بنر وخصم اختياري",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingShowcase = null
          showShowcaseDialog = true
        },
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier.testTag("admin_add_brand_showcase_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "إضافة منتجات شركة")
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

        uiState.showcases.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.Storefront,
              title = "لا توجد أقسام منتجات شركات حاليًا",
              description = "أضف قسم «منتجات شركة»، اختر الشركة ليتم جلب منتجاتها تلقائيًا، مع خيار إضافة بنر وخصم موحّد على منتجاتها.",
              actionLabel = "إضافة منتجات شركة",
              onActionClick = {
                editingShowcase = null
                showShowcaseDialog = true
              },
            )
          }
        }

        else -> {
          LazyColumn(
            modifier = Modifier
              .widthIn(max = 860.dp)
              .fillMaxSize(),
            contentPadding = PaddingValues(
              start = AlEmanDimens.SpaceLg,
              end = AlEmanDimens.SpaceLg,
              top = AlEmanDimens.SpaceLg,
              bottom = 92.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            items(uiState.showcases, key = { it.id }) { showcase ->
              val index = uiState.showcases.indexOf(showcase)
              val linkedProducts = uiState.productsForShowcase(showcase)
              BrandShowcaseAdminCard(
                showcase = showcase,
                productsCount = linkedProducts.size,
                canMoveUp = index > 0,
                canMoveDown = index < uiState.showcases.lastIndex,
                onMoveUp = { onMoveShowcaseOrder(showcase, -1) },
                onMoveDown = { onMoveShowcaseOrder(showcase, +1) },
                onToggleActive = { onToggleShowcaseActive(showcase) },
                onManageContent = { managingShowcaseId = showcase.id },
                onEdit = {
                  editingShowcase = showcase
                  showShowcaseDialog = true
                },
                onDelete = { deletingShowcase = showcase },
              )
            }
          }
        }
      }
    }
  }

  if (showShowcaseDialog) {
    BrandShowcaseFormDialog(
      existing = editingShowcase,
      defaultOrder = uiState.showcases.size,
      companies = uiState.companies,
      allProducts = uiState.products,
      onDismiss = { showShowcaseDialog = false },
      onConfirm = { showcase ->
        onSaveShowcase(showcase) {
          showShowcaseDialog = false
        }
      },
    )
  }

  if (managingShowcase != null) {
    BrandShowcaseContentManagerDialog(
      showcase = managingShowcase,
      allProducts = uiState.products,
      mainStoreCategories = uiState.mainStoreCategories,
      onDismiss = { managingShowcaseId = null },
      onSaveCategory = { cat -> onSaveShowcaseCategory(managingShowcase, cat) },
      onDeleteCategory = { catId -> onDeleteShowcaseCategory(managingShowcase, catId) },
      onSaveNewProduct = { prod -> onSaveShowcaseProduct(managingShowcase, prod) },
      onLinkExistingProduct = { prod, brandCatId, exclusive ->
        onLinkExistingProduct(managingShowcase, prod, brandCatId, exclusive)
      },
      onUnlinkProduct = onUnlinkProduct,
      onToggleExclusive = onToggleProductExclusive,
    )
  }

  if (deletingShowcase != null) {
    AlertDialog(
      onDismissRequest = { deletingShowcase = null },
      containerColor = PureWhite,
      title = { Text("حذف منتجات الشركة", fontWeight = FontWeight.Bold, color = DiscountRed) },
      text = {
        Text("سيتم حذف قسم منتجات «${deletingShowcase?.title}» من الصفحة الرئيسية (مع بقاء الشركة ومنتجاتها في الكتالوج). هل تريد المتابعة؟")
      },
      confirmButton = {
        Button(
          onClick = {
            deletingShowcase?.let { onDeleteShowcase(it.id) }
            deletingShowcase = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed),
        ) {
          Text("حذف القسم", color = PureWhite)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingShowcase = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrandShowcaseAdminCard(
  showcase: BrandShowcase,
  productsCount: Int,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onToggleActive: () -> Unit,
  onManageContent: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val accentColor = remember(showcase.accentColorHex) {
    parseHexOrDefault(showcase.accentColorHex, Color(0xFFFCE4EC))
  }

  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (showcase.hasCustomBanner) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(accentColor),
        ) {
          FullAspectBannerImage(
            imageUrl = showcase.bannerImageUrl,
            contentDescription = showcase.title,
            placeholderMinHeight = 160.dp,
            placeholderBackground = accentColor,
            errorContent = {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(160.dp)
                  .background(accentColor),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
              }
            },
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CharcoalBlack.copy(alpha = 0.85f),
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(10.dp),
          ) {
            Text(
              text = "الترتيب: ${ArabicFormatter.formatNumber(showcase.displayOrder)}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = PrimaryYellow,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          if (showcase.cleanDiscountPercent > 0) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DiscountRed,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            ) {
              Text(
                text = "خصم ${ArabicFormatter.formatNumber(showcase.cleanDiscountPercent)}٪",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      } else {
        Surface(
          color = accentColor,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (!showcase.companyImageUrl.isNullOrBlank()) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(PureWhite)
                  .border(1.dp, BorderNeutral, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
              ) {
                SubcomposeAsyncImage(
                  model = showcase.companyImageUrl,
                  contentDescription = showcase.title,
                  contentScale = ContentScale.Fit,
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                )
              }
            } else {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(PureWhite),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = CharcoalBlack)
              }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(shape = RoundedCornerShape(6.dp), color = CharcoalBlack.copy(alpha = 0.85f)) {
                  Text(
                    text = "الترتيب: ${ArabicFormatter.formatNumber(showcase.displayOrder)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryYellow,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                  )
                }
                Surface(shape = RoundedCornerShape(6.dp), color = PureWhite.copy(alpha = 0.9f)) {
                  Text(
                    text = "تصميم تلقائي متناوب",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                  )
                }
              }
            }
            if (showcase.cleanDiscountPercent > 0) {
              Surface(shape = RoundedCornerShape(8.dp), color = DiscountRed) {
                Text(
                  text = "خصم ${ArabicFormatter.formatNumber(showcase.cleanDiscountPercent)}٪",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = PureWhite,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                )
              }
            }
          }
        }
      }

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
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(accentColor)
                .border(1.dp, BorderNeutral, CircleShape),
            )
            Text(
              text = showcase.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          if (showcase.isActive) SuccessBadge("مفعّل") else DiscountBadge("معطّل")
        }

        if (!showcase.subtitle.isNullOrBlank()) {
          Text(
            text = showcase.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
          )
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BrandPillBadge(
            text = showcase.placementEnum.arabicLabel,
            containerColor = RoyalBlueSoft,
            contentColor = RoyalBlue,
          )
          BrandPillBadge(
            text = "${ArabicFormatter.formatNumber(productsCount)} منتج للشركة",
            containerColor = SuccessGreenSoft,
            contentColor = SuccessGreen,
          )
          if (showcase.cleanDiscountPercent > 0) {
            BrandPillBadge(
              text = "خصم الشركة ${ArabicFormatter.formatNumber(showcase.cleanDiscountPercent)}٪",
              containerColor = DiscountRedSoft,
              contentColor = DiscountRed,
            )
          }
          if (showcase.categories.isNotEmpty()) {
            BrandPillBadge(
              text = "${ArabicFormatter.formatNumber(showcase.categories.size)} فئة مخصصة",
              containerColor = PrimaryYellowSoft,
              contentColor = CharcoalBlack,
            )
          }
        }

        OutlinedButton(
          onClick = onManageContent,
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          border = BorderStroke(1.dp, CharcoalBlack),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("manage_brand_content_${showcase.id}"),
        ) {
          Icon(Icons.Filled.Category, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("إدارة فئات ومحتوى إضافي (اختياري)", color = CharcoalBlack, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = BorderNeutral)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ظهور العرض", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Switch(
              checked = showcase.isActive,
              onCheckedChange = { onToggleActive() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = CharcoalBlack,
                checkedTrackColor = PrimaryYellow,
              ),
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
              Icon(
                Icons.Filled.ArrowUpward,
                contentDescription = "تحريك للأعلى",
                tint = if (canMoveUp) CharcoalBlack else TextSecondary.copy(alpha = 0.3f),
              )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
              Icon(
                Icons.Filled.ArrowDownward,
                contentDescription = "تحريك للأسفل",
                tint = if (canMoveDown) CharcoalBlack else TextSecondary.copy(alpha = 0.3f),
              )
            }
            IconButton(onClick = onEdit) {
              Icon(Icons.Filled.Edit, contentDescription = "تعديل العرض", tint = CharcoalBlack)
            }
            IconButton(onClick = onDelete) {
              Icon(Icons.Filled.Delete, contentDescription = "حذف العرض", tint = DiscountRed)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BrandShowcaseFormDialog(
  existing: BrandShowcase?,
  defaultOrder: Int,
  companies: List<StoreCompany>,
  allProducts: List<Product>,
  onDismiss: () -> Unit,
  onConfirm: (BrandShowcase) -> Unit,
) {
  val initialCompany = remember(existing, companies) {
    existing?.companyId?.let { cid -> companies.firstOrNull { it.id == cid } }
      ?: if (existing == null) companies.firstOrNull { it.isActive } ?: companies.firstOrNull() else null
  }
  var selectedCompanyId by remember { mutableStateOf(initialCompany?.id ?: existing?.companyId) }
  var title by remember { mutableStateOf(existing?.title ?: initialCompany?.name.orEmpty()) }
  var companyImageUrl by remember { mutableStateOf(existing?.companyImageUrl ?: initialCompany?.imageUrl) }
  var subtitle by remember { mutableStateOf(existing?.subtitle.orEmpty()) }
  var bannerImageUrl by remember { mutableStateOf(existing?.bannerImageUrl.orEmpty()) }
  var discountPercentText by remember {
    mutableStateOf(
      existing?.cleanDiscountPercent?.takeIf { it > 0 }?.toString().orEmpty(),
    )
  }
  var accentColorHex by remember { mutableStateOf(existing?.accentColorHex ?: "#FCE4EC") }
  var placement by remember { mutableStateOf(existing?.placementEnum ?: BannerPlacement.BELOW_CATEGORIES) }
  var displayOrder by remember { mutableStateOf((existing?.displayOrder ?: defaultOrder).toString()) }
  var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
  var companyExpanded by remember { mutableStateOf(false) }
  var placementExpanded by remember { mutableStateOf(false) }
  var showBannerModal by remember { mutableStateOf(false) }
  var uploadError by remember { mutableStateOf<String?>(null) }

  val matchedCount = remember(allProducts, selectedCompanyId, existing?.id) {
    allProducts.count { prod ->
      prod.isActive && (
        (!selectedCompanyId.isNullOrBlank() && prod.companyId == selectedCompanyId) ||
          (existing != null && existing.id.isNotBlank() && prod.brandShowcaseId == existing.id)
        )
    }
  }
  val parsedDiscount = (discountPercentText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 95.0)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة منتجات شركة" else "تعديل منتجات الشركة",
        fontWeight = FontWeight.Bold,
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 580.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (companies.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = RoyalBlueSoft,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "تلميح: يمكنك إضافة الشركات وشعاراتها من (إعدادات المتجر -> إضافة شركات) ليتم جلب منتجات كل شركة تلقائيًا هنا.",
              style = MaterialTheme.typography.bodySmall,
              color = RoyalBlue,
              modifier = Modifier.padding(10.dp),
            )
          }
        } else {
          ExposedDropdownMenuBox(
            expanded = companyExpanded,
            onExpandedChange = { companyExpanded = !companyExpanded },
          ) {
            val selectedLabel = companies.firstOrNull { it.id == selectedCompanyId }?.name
              ?: "إدخال يدوي / عرض مخصص"
            OutlinedTextField(
              value = selectedLabel,
              onValueChange = {},
              readOnly = true,
              label = { Text("اختر الشركة *") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded) },
              modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("admin_showcase_company_dropdown"),
            )
            ExposedDropdownMenu(
              expanded = companyExpanded,
              onDismissRequest = { companyExpanded = false },
              containerColor = PureWhite,
            ) {
              companies.forEach { comp ->
                DropdownMenuItem(
                  text = { Text(if (comp.isActive) comp.name else "${comp.name} (معطلة)") },
                  onClick = {
                    selectedCompanyId = comp.id
                    title = comp.name
                    companyImageUrl = comp.imageUrl
                    companyExpanded = false
                  },
                )
              }
              DropdownMenuItem(
                text = { Text("إدخال اسم يدوي / عرض مخصص") },
                onClick = {
                  selectedCompanyId = null
                  companyImageUrl = null
                  companyExpanded = false
                },
              )
            }
          }
        }

        if (selectedCompanyId != null || matchedCount > 0) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = SuccessGreenSoft,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "سيتم جلب وعرض ${ArabicFormatter.formatNumber(matchedCount)} منتج تلقائيًا من منتجات هذه الشركة.",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = SuccessGreen,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("اسم الشركة المعروض *") },
          placeholder = { Text("مثال: أوكسي - Oxi") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = subtitle,
          onValueChange = { subtitle = it },
          label = { Text("عنوان أو عرض فرعي (اختياري)") },
          placeholder = { Text("مثال: يوصلك خلال دقائق بأقوى العروض") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // خصم اختياري موحد على جميع منتجات الشركة
        OutlinedTextField(
          value = discountPercentText,
          onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() }
            val num = filtered.toIntOrNull()
            discountPercentText = if (num != null && num > 95) "95" else filtered
          },
          label = { Text("خصم اختياري على كل منتجات الشركة (%)") },
          placeholder = { Text("مثال: 15 (اتركه فارغًا أو 0 بدون خصم)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_showcase_discount_input"),
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(0, 5, 10, 15, 20, 25, 30).forEach { pct ->
            val activeChip = parsedDiscount.toInt() == pct
            Surface(
              onClick = { discountPercentText = if (pct == 0) "" else pct.toString() },
              shape = RoundedCornerShape(8.dp),
              color = if (activeChip) PrimaryYellow else GeneralBackground,
              border = BorderStroke(1.dp, if (activeChip) CharcoalBlack else BorderNeutral),
            ) {
              Text(
                text = if (pct == 0) "بدون خصم" else "${ArabicFormatter.formatNumber(pct)}٪",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
        }

        HorizontalDivider(color = BorderNeutral)

        // قسم البنر الاختياري
        if (bannerImageUrl.isNotBlank()) {
          FullAspectBannerImage(
            imageUrl = bannerImageUrl,
            contentDescription = "معاينة بنر الشركة",
            placeholderMinHeight = 140.dp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { showBannerModal = true },
            border = BorderStroke(1.dp, CharcoalBlack),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (bannerImageUrl.isBlank()) "إرفاق بنر اختياري من الاستيديو" else "تغيير البنر",
              color = CharcoalBlack,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (bannerImageUrl.isNotBlank()) {
            OutlinedButton(
              onClick = { bannerImageUrl = "" },
              border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.6f)),
            ) {
              Text("بدون بنر", color = DiscountRed, fontWeight = FontWeight.Bold)
            }
          }
        }

        Text(
          text = if (bannerImageUrl.isBlank()) {
            "بدون بنر: سيُصمّم القسم تلقائيًا بمظهر احترافي ويتبادل رأس الشركة بين اليمين واليسار تلقائيًا."
          } else {
            "بالبنر: يظهر البنر كاملًا كما قصصته، أعلى ٢٥٪ فارغ لشعار البنر، ومن ٢٥٪ إلى ٩٠٪ المنتجات، وآخر ١٠٪ زر عرض الكل."
          },
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )

        if (!uploadError.isNullOrBlank()) {
          Text(uploadError.orEmpty(), color = DiscountRed, style = MaterialTheme.typography.bodySmall)
        }

        ExposedDropdownMenuBox(
          expanded = placementExpanded,
          onExpandedChange = { placementExpanded = !placementExpanded },
        ) {
          OutlinedTextField(
            value = placement.arabicLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("مكان الظهور في الصفحة الرئيسية *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = placementExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable),
          )
          ExposedDropdownMenu(
            expanded = placementExpanded,
            onDismissRequest = { placementExpanded = false },
            containerColor = PureWhite,
          ) {
            BannerPlacement.entries.forEach { pos ->
              DropdownMenuItem(
                text = { Text(pos.arabicLabel) },
                onClick = {
                  placement = pos
                  placementExpanded = false
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = displayOrder,
          onValueChange = { displayOrder = it.filter { ch -> ch.isDigit() } },
          label = { Text("ترتيب الظهور (الأصغر يظهر أولًا)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Text(
          text = "لون خلفية كارت الشركة في الرئيسية:",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          BrandAccentPresets.forEach { (hex, label) ->
            val selected = accentColorHex.equals(hex, ignoreCase = true)
            val swatchColor = parseHexOrDefault(hex, Color(0xFFFCE4EC))
            Surface(
              onClick = { accentColorHex = hex },
              shape = RoundedCornerShape(10.dp),
              color = swatchColor,
              border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) CharcoalBlack else BorderNeutral,
              ),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  ),
                  color = CharcoalBlack,
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("إظهار منتجات الشركة للعملاء فورًا", style = MaterialTheme.typography.bodyMedium, color = CharcoalBlack)
          Switch(
            checked = isActive,
            onCheckedChange = { isActive = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CharcoalBlack,
              checkedTrackColor = PrimaryYellow,
            ),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            (existing ?: BrandShowcase()).copy(
              companyId = selectedCompanyId,
              companyImageUrl = companyImageUrl?.takeIf { it.isNotBlank() },
              title = title.trim(),
              subtitle = subtitle.trim().ifEmpty { null },
              bannerImageUrl = bannerImageUrl.trim(),
              discountPercent = parsedDiscount,
              accentColorHex = accentColorHex,
              placement = placement.code,
              displayOrder = (displayOrder.toIntOrNull() ?: 0).coerceIn(0, 1000),
              isActive = isActive,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
        modifier = Modifier.testTag("admin_save_showcase_button"),
      ) {
        Text(if (existing == null) "إضافة منتجات الشركة" else "حفظ التعديلات", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )

  ImagePickerUploadModal(
    visible = showBannerModal,
    title = "رفع بنر الشركة من الاستيديو",
    folder = CloudinaryFolder.BANNERS,
    allowMultiple = false,
    onUploadedUrls = { urls ->
      urls.firstOrNull()?.let {
        bannerImageUrl = it
        uploadError = null
      }
      showBannerModal = false
    },
    onError = { msg ->
      uploadError = msg
      showBannerModal = false
    },
    onDismiss = { showBannerModal = false },
  )
}

@Composable
private fun BrandShowcaseContentManagerDialog(
  showcase: BrandShowcase,
  allProducts: List<Product>,
  mainStoreCategories: List<Category>,
  onDismiss: () -> Unit,
  onSaveCategory: (BrandShowcaseCategory) -> Unit,
  onDeleteCategory: (String) -> Unit,
  onSaveNewProduct: (Product) -> Unit,
  onLinkExistingProduct: (Product, String, Boolean) -> Unit,
  onUnlinkProduct: (Product) -> Unit,
  onToggleExclusive: (Product) -> Unit,
) {
  var editingCategory by remember { mutableStateOf<BrandShowcaseCategory?>(null) }
  var showCategoryDialog by remember { mutableStateOf(false) }
  var showNewProductDialog by remember { mutableStateOf(false) }
  var showLinkProductDialog by remember { mutableStateOf(false) }

  val linkedProducts = remember(allProducts, showcase) {
    allProducts.filter { it.matchesBrandShowcase(showcase) }
  }
  val categoriesById = remember(showcase.categories) {
    showcase.categories.associateBy { it.id }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Column {
        Text(
          text = "إدارة «${showcase.title}»",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        Text(
          text = "١) أضف فئات الشركة بالصور أولًا  •  ٢) أضف المنتجات وحدد فئتها وظهورها",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 560.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // القسم الأول: فئات الشركة
        Surface(
          shape = RoundedCornerShape(14.dp),
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
              Text(
                text = "١. فئات الشركة (${ArabicFormatter.formatNumber(showcase.categories.size)})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
              Button(
                onClick = {
                  editingCategory = null
                  showCategoryDialog = true
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة فئة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
              }
            }

            if (showcase.categories.isEmpty()) {
              Text(
                text = "لم تُضف فئات لهذه الشركة بعد. أضف كل فئة مع صورتها لتظهر في العمود الأيمن عند الضغط على «عرض الكل».",
                style = MaterialTheme.typography.bodySmall,
                color = DiscountRed,
              )
            } else {
              showcase.sortedCategories.forEach { cat ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = PureWhite,
                  border = BorderStroke(1.dp, BorderNeutral),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Box(
                      modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PureWhite)
                        .border(1.dp, BorderNeutral, RoundedCornerShape(8.dp)),
                      contentAlignment = Alignment.Center,
                    ) {
                      SubcomposeAsyncImage(
                        model = cat.imageUrl,
                        contentDescription = cat.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                          .fillMaxSize()
                          .padding(3.dp),
                      )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = cat.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = CharcoalBlack,
                      )
                      Text(
                        text = "الترتيب: ${ArabicFormatter.formatNumber(cat.displayOrder)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                      )
                    }
                    IconButton(
                      onClick = {
                        editingCategory = cat
                        showCategoryDialog = true
                      },
                      modifier = Modifier.size(34.dp),
                    ) {
                      Icon(Icons.Filled.Edit, contentDescription = "تعديل الفئة", tint = CharcoalBlack, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                      onClick = { onDeleteCategory(cat.id) },
                      modifier = Modifier.size(34.dp),
                    ) {
                      Icon(Icons.Filled.Delete, contentDescription = "حذف الفئة", tint = DiscountRed, modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }
            }
          }
        }

        // القسم الثاني: منتجات الشركة
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "٢. منتجات الشركة (${ArabicFormatter.formatNumber(linkedProducts.size)})",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )

            if (showcase.categories.isEmpty()) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = DiscountRedSoft,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = DiscountRed)
                  Text(
                    text = "أضف فئة واحدة على الأقل للشركة مع صورتها أولًا لفتح إضافة وربط المنتجات.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = DiscountRed,
                  )
                }
              }
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = { showNewProductDialog = true },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = CharcoalBlack, contentColor = PrimaryYellow),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("منتج جديد", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                OutlinedButton(
                  onClick = { showLinkProductDialog = true },
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, CharcoalBlack),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(Icons.Filled.Link, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("ربط منتج حالي", color = CharcoalBlack, fontWeight = FontWeight.Bold, maxLines = 1)
                }
              }
            }

            if (linkedProducts.isEmpty()) {
              Text(
                text = "لا توجد منتجات مضافة لهذا العرض حتى الآن.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            } else {
              linkedProducts.forEach { prod ->
                val catName = prod.brandCategoryId?.let { categoriesById[it]?.name } ?: "بدون فئة محددة"
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = PureWhite,
                  border = BorderStroke(1.dp, BorderNeutral),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      ProductThumbnail(
                        imageUrl = prod.imageUrls.firstOrNull(),
                        contentDescription = prod.name,
                        modifier = Modifier
                          .size(44.dp)
                          .clip(RoundedCornerShape(8.dp)),
                      )
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = prod.name,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                          color = CharcoalBlack,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "$catName • ${ArabicFormatter.formatCurrency(prod.effectiveRetailPrice)}",
                          style = MaterialTheme.typography.labelSmall,
                          color = TextSecondary,
                        )
                      }
                      IconButton(onClick = { onUnlinkProduct(prod) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.LinkOff, contentDescription = "فك الارتباط", tint = DiscountRed, modifier = Modifier.size(18.dp))
                      }
                    }

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      BrandPillBadge(
                        text = if (prod.exclusiveToBrandShowcase) "حصري بالشركة فقط" else "يظهر في المتجر + الشركة",
                        containerColor = if (prod.exclusiveToBrandShowcase) DiscountRedSoft else RoyalBlueSoft,
                        contentColor = if (prod.exclusiveToBrandShowcase) DiscountRed else RoyalBlue,
                      )
                      TextButton(
                        onClick = { onToggleExclusive(prod) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                      ) {
                        Text(
                          text = if (prod.exclusiveToBrandShowcase) "إظهار بالرئيسية أيضًا" else "جعله حصريًا للشركة",
                          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                          color = CharcoalBlack,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
      ) {
        Text("إغلاق", fontWeight = FontWeight.Bold)
      }
    },
  )

  if (showCategoryDialog) {
    BrandCategoryFormDialog(
      existing = editingCategory,
      defaultOrder = showcase.categories.size,
      onDismiss = { showCategoryDialog = false },
      onConfirm = { cat ->
        onSaveCategory(cat)
        showCategoryDialog = false
      },
    )
  }

  if (showNewProductDialog) {
    ShowcaseNewProductDialog(
      brandCategories = showcase.sortedCategories,
      mainStoreCategories = mainStoreCategories,
      onDismiss = { showNewProductDialog = false },
      onConfirm = { prod ->
        onSaveNewProduct(prod)
        showNewProductDialog = false
      },
    )
  }

  if (showLinkProductDialog) {
    LinkExistingProductDialog(
      availableProducts = allProducts.filter { it.brandShowcaseId != showcase.id },
      brandCategories = showcase.sortedCategories,
      onDismiss = { showLinkProductDialog = false },
      onConfirm = { prod, catId, exclusive ->
        onLinkExistingProduct(prod, catId, exclusive)
        showLinkProductDialog = false
      },
    )
  }
}

@Composable
private fun BrandCategoryFormDialog(
  existing: BrandShowcaseCategory?,
  defaultOrder: Int,
  onDismiss: () -> Unit,
  onConfirm: (BrandShowcaseCategory) -> Unit,
) {
  var name by remember { mutableStateOf(existing?.name.orEmpty()) }
  var imageUrl by remember { mutableStateOf(existing?.imageUrl.orEmpty()) }
  var displayOrder by remember { mutableStateOf((existing?.displayOrder ?: defaultOrder).toString()) }
  var showImageModal by remember { mutableStateOf(false) }
  var uploadError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة فئة للشركة" else "تعديل فئة الشركة",
        fontWeight = FontWeight.Bold,
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
        if (imageUrl.isNotBlank()) {
          Box(
            modifier = Modifier
              .size(88.dp)
              .align(Alignment.CenterHorizontally)
              .clip(RoundedCornerShape(14.dp))
              .background(PureWhite)
              .border(1.dp, BorderNeutral, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
          ) {
            SubcomposeAsyncImage(
              model = imageUrl,
              contentDescription = "صورة الفئة",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            )
          }
        }

        OutlinedButton(
          onClick = { showImageModal = true },
          border = BorderStroke(1.dp, CharcoalBlack),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (imageUrl.isBlank()) "رفع صورة الفئة *" else "تغيير صورة الفئة",
            color = CharcoalBlack,
            fontWeight = FontWeight.Bold,
          )
        }

        if (!uploadError.isNullOrBlank()) {
          Text(uploadError.orEmpty(), color = DiscountRed, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("اسم فئة الشركة *") },
          placeholder = { Text("مثال: مسحوق غسيل، منعم أقمشة...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = displayOrder,
          onValueChange = { displayOrder = it.filter { ch -> ch.isDigit() } },
          label = { Text("ترتيب الفئة") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            (existing ?: BrandShowcaseCategory()).copy(
              name = name.trim(),
              imageUrl = imageUrl.trim(),
              displayOrder = displayOrder.toIntOrNull() ?: 0,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
      ) {
        Text("حفظ الفئة", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )

  ImagePickerUploadModal(
    visible = showImageModal,
    title = "رفع صورة فئة الشركة",
    folder = CloudinaryFolder.CATEGORIES,
    allowMultiple = false,
    onUploadedUrls = { urls ->
      urls.firstOrNull()?.let {
        imageUrl = it
        uploadError = null
      }
      showImageModal = false
    },
    onError = { msg ->
      uploadError = msg
      showImageModal = false
    },
    onDismiss = { showImageModal = false },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowcaseNewProductDialog(
  brandCategories: List<BrandShowcaseCategory>,
  mainStoreCategories: List<Category>,
  onDismiss: () -> Unit,
  onConfirm: (Product) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var sku by remember { mutableStateOf("BRD-${System.currentTimeMillis().toString().takeLast(5)}") }
  var retailPrice by remember { mutableStateOf("") }
  var discountPrice by remember { mutableStateOf("") }
  var wholesalePrice by remember { mutableStateOf("") }
  var unit by remember { mutableStateOf("١ لتر") }
  var stockQuantity by remember { mutableStateOf("50") }
  var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
  var selectedBrandCategoryId by remember { mutableStateOf(brandCategories.firstOrNull()?.id.orEmpty()) }
  var selectedMainCategoryId by remember { mutableStateOf(mainStoreCategories.firstOrNull()?.id.orEmpty()) }
  var exclusiveToBrand by remember { mutableStateOf(false) }

  var brandCatExpanded by remember { mutableStateOf(false) }
  var mainCatExpanded by remember { mutableStateOf(false) }
  var showImageModal by remember { mutableStateOf(false) }
  var uploadError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text("إضافة منتج داخل الشركة", fontWeight = FontWeight.Bold, color = CharcoalBlack)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 560.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (imageUrls.isNotEmpty()) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(imageUrls) { url ->
              Box(
                modifier = Modifier
                  .size(72.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .border(1.dp, BorderNeutral, RoundedCornerShape(10.dp)),
              ) {
                ProductThumbnail(imageUrl = url, contentDescription = "صورة المنتج", modifier = Modifier.fillMaxSize())
                IconButton(
                  onClick = { imageUrls = imageUrls - url },
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .background(PureWhite.copy(alpha = 0.85f), CircleShape),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = "حذف", tint = DiscountRed, modifier = Modifier.size(14.dp))
                }
              }
            }
          }
        }

        OutlinedButton(
          onClick = { showImageModal = true },
          border = BorderStroke(1.dp, CharcoalBlack),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack)
          Spacer(modifier = Modifier.width(6.dp))
          Text("رفع صور المنتج", color = CharcoalBlack, fontWeight = FontWeight.Bold)
        }

        if (!uploadError.isNullOrBlank()) {
          Text(uploadError.orEmpty(), color = DiscountRed, style = MaterialTheme.typography.bodySmall)
        }

        // اختيار فئة الشركة (إجباري)
        ExposedDropdownMenuBox(
          expanded = brandCatExpanded,
          onExpandedChange = { brandCatExpanded = !brandCatExpanded },
        ) {
          val selectedBrandCatName = brandCategories.firstOrNull { it.id == selectedBrandCategoryId }?.name
            ?: "اختر فئة الشركة *"
          OutlinedTextField(
            value = selectedBrandCatName,
            onValueChange = {},
            readOnly = true,
            label = { Text("فئة المنتج داخل الشركة *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandCatExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable),
          )
          ExposedDropdownMenu(
            expanded = brandCatExpanded,
            onDismissRequest = { brandCatExpanded = false },
            containerColor = PureWhite,
          ) {
            brandCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  selectedBrandCategoryId = cat.id
                  brandCatExpanded = false
                },
              )
            }
          }
        }

        // اختيار القسم العام بالمتجر
        ExposedDropdownMenuBox(
          expanded = mainCatExpanded,
          onExpandedChange = { mainCatExpanded = !mainCatExpanded },
        ) {
          val selectedMainName = mainStoreCategories.firstOrNull { it.id == selectedMainCategoryId }?.name
            ?: "اختر القسم الرئيسي بالمتجر *"
          OutlinedTextField(
            value = selectedMainName,
            onValueChange = {},
            readOnly = true,
            label = { Text("القسم العام بالمتجر *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mainCatExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable),
          )
          ExposedDropdownMenu(
            expanded = mainCatExpanded,
            onDismissRequest = { mainCatExpanded = false },
            containerColor = PureWhite,
          ) {
            mainStoreCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  selectedMainCategoryId = cat.id
                  mainCatExpanded = false
                },
              )
            }
          }
        }

        // خيار الظهور: عام في المتجر وعرض الشركة أو حصري للشركة فقط
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "أين يظهر هذا المنتج؟ *",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { exclusiveToBrand = false },
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = !exclusiveToBrand,
                onClick = { exclusiveToBrand = false },
                colors = RadioButtonDefaults.colors(selectedColor = CharcoalBlack),
              )
              Icon(Icons.Filled.Public, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "عام: يظهر في المتجر الرئيسي وعرض الشركة",
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalBlack,
              )
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { exclusiveToBrand = true },
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = exclusiveToBrand,
                onClick = { exclusiveToBrand = true },
                colors = RadioButtonDefaults.colors(selectedColor = CharcoalBlack),
              )
              Icon(Icons.Filled.Lock, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "حصري: يظهر داخل عرض الشركة فقط",
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalBlack,
              )
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("اسم المنتج *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("وصف المنتج") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = sku,
            onValueChange = { sku = it },
            label = { Text("SKU *") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text("الحجم / الوحدة") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = retailPrice,
            onValueChange = { retailPrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("سعر القطاعي *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = discountPrice,
            onValueChange = { discountPrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("بعد الخصم") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = wholesalePrice,
            onValueChange = { wholesalePrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("سعر الجملة") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = stockQuantity,
            onValueChange = { stockQuantity = it.filter { ch -> ch.isDigit() } },
            label = { Text("المخزون") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            Product(
              name = name.trim(),
              description = description.trim(),
              categoryId = selectedMainCategoryId,
              sku = sku.trim(),
              imageUrls = imageUrls.take(10),
              retailPrice = retailPrice.toDoubleOrNull() ?: 0.0,
              retailDiscountPrice = discountPrice.toDoubleOrNull(),
              wholesalePrice = wholesalePrice.toDoubleOrNull() ?: 0.0,
              wholesaleMinQty = 6,
              unit = unit.trim().ifEmpty { "قطعة" },
              stockQuantity = stockQuantity.toIntOrNull() ?: 0,
              isActive = true,
              brandCategoryId = selectedBrandCategoryId,
              exclusiveToBrandShowcase = exclusiveToBrand,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
      ) {
        Text("حفظ المنتج", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )

  ImagePickerUploadModal(
    visible = showImageModal,
    title = "رفع صور منتج الشركة",
    folder = CloudinaryFolder.PRODUCTS,
    allowMultiple = true,
    onUploadedUrls = { urls ->
      imageUrls = (imageUrls + urls).distinct().take(10)
      uploadError = null
      showImageModal = false
    },
    onError = { msg ->
      uploadError = msg
      showImageModal = false
    },
    onDismiss = { showImageModal = false },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkExistingProductDialog(
  availableProducts: List<Product>,
  brandCategories: List<BrandShowcaseCategory>,
  onDismiss: () -> Unit,
  onConfirm: (Product, String, Boolean) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedProduct by remember { mutableStateOf<Product?>(null) }
  var selectedBrandCatId by remember { mutableStateOf(brandCategories.firstOrNull()?.id.orEmpty()) }
  var exclusiveToBrand by remember { mutableStateOf(false) }
  var catExpanded by remember { mutableStateOf(false) }

  val filteredProducts = remember(availableProducts, searchQuery) {
    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) availableProducts.take(40)
    else availableProducts.filter {
      it.name.lowercase().contains(q) || it.sku.lowercase().contains(q)
    }.take(40)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = { Text("ربط منتج من المتجر بالشركة", fontWeight = FontWeight.Bold, color = CharcoalBlack) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 520.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ExposedDropdownMenuBox(
          expanded = catExpanded,
          onExpandedChange = { catExpanded = !catExpanded },
        ) {
          val catName = brandCategories.firstOrNull { it.id == selectedBrandCatId }?.name ?: "اختر فئة الشركة *"
          OutlinedTextField(
            value = catName,
            onValueChange = {},
            readOnly = true,
            label = { Text("فئة الشركة التابع لها *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable),
          )
          ExposedDropdownMenu(
            expanded = catExpanded,
            onDismissRequest = { catExpanded = false },
            containerColor = PureWhite,
          ) {
            brandCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  selectedBrandCatId = cat.id
                  catExpanded = false
                },
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { exclusiveToBrand = false },
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(selected = !exclusiveToBrand, onClick = { exclusiveToBrand = false })
              Text("عام: المتجر الرئيسي وعرض الشركة", style = MaterialTheme.typography.bodySmall)
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { exclusiveToBrand = true },
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(selected = exclusiveToBrand, onClick = { exclusiveToBrand = true })
              Text("حصري: عرض الشركة فقط", style = MaterialTheme.typography.bodySmall)
            }
          }
        }

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = { Text("ابحث عن منتج بالاسم أو SKU") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        if (filteredProducts.isEmpty()) {
          Text("لا توجد منتجات متاحة للربط.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        } else {
          filteredProducts.forEach { prod ->
            val isSelected = selectedProduct?.id == prod.id
            Surface(
              onClick = { selectedProduct = prod },
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) PrimaryYellowSoft else PureWhite,
              border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PrimaryYellow else BorderNeutral),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ProductThumbnail(
                  imageUrl = prod.imageUrls.firstOrNull(),
                  contentDescription = prod.name,
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(prod.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(ArabicFormatter.formatCurrency(prod.effectiveRetailPrice), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val chosen = selectedProduct ?: return@Button
          onConfirm(chosen, selectedBrandCatId, exclusiveToBrand)
        },
        enabled = selectedProduct != null && selectedBrandCatId.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
      ) {
        Text("ربط بالشركة", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}

private fun parseHexOrDefault(hex: String?, fallback: Color): Color {
  val clean = hex?.trim().orEmpty()
  if (!clean.startsWith("#")) return fallback
  return try {
    Color(android.graphics.Color.parseColor(clean))
  } catch (_: Exception) {
    fallback
  }
}
