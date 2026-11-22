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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.example.data.media.CloudinaryFolder
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.StoreCompany
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.ImagePickerUploadModal
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.QrBarcodeScannerDialog
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminCatalogUiState

private enum class AdminScanTarget {
  BARCODE,
  QR_CODE,
}

private enum class AdminProductFilter(val label: String) {
  ALL("الكل"),
  OFFERS("عروض"),
  FEATURED("مميز"),
  LOW_STOCK("مخزون منخفض"),
  HIDDEN("مخفي"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminProductsScreen(
  uiState: AdminCatalogUiState,
  onSaveProduct: (Product) -> Unit,
  onDeleteProduct: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var showFormDialog by remember { mutableStateOf(false) }
  var deletingProduct by remember { mutableStateOf<Product?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(AdminProductFilter.ALL) }
  var showSearchScanner by remember { mutableStateOf(false) }

  val filteredProducts = remember(uiState.products, searchQuery, selectedFilter) {
    val q = searchQuery.trim().lowercase()
    uiState.products.filter { p ->
      val matchesQuery = q.isEmpty() ||
        p.name.lowercase().contains(q) ||
        p.sku.lowercase().contains(q) ||
        (p.barcode?.lowercase()?.contains(q) == true) ||
        (p.qrCode?.lowercase()?.contains(q) == true)
      val matchesFilter = when (selectedFilter) {
        AdminProductFilter.ALL -> true
        AdminProductFilter.OFFERS -> p.isOffer || p.hasDiscount
        AdminProductFilter.FEATURED -> p.isFeatured
        AdminProductFilter.LOW_STOCK -> p.isLowStock || p.isOutOfStock
        AdminProductFilter.HIDDEN -> !p.isActive
      }
      matchesQuery && matchesFilter
    }
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
        title = "إدارة المنتجات",
        subtitle = "إضافة وتعديل المنتجات والباركود وQR (${uiState.products.size} منتج)",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingProduct = null
          showFormDialog = true
        },
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier.testTag("admin_add_product_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "إضافة منتج جديد")
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
            CircularProgressIndicator(color = RoyalBlue)
          }
        }

        uiState.products.isEmpty() -> {
          Box(
            modifier = Modifier
              .widthIn(max = 600.dp)
              .padding(AlEmanDimens.SpaceLg),
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.Inventory2,
              title = "لا توجد منتجات مضافة حتى الآن",
              description = if (uiState.mainCategories.isEmpty()) {
                "أضف قسمًا رئيسيًا أولًا ثم أضف منتجاتك مع أسعار القطاعي والجملة وأكواد الباركود وQR الاختيارية."
              } else {
                "أضف منتجك الأول مع أسعار القطاعي والجملة ورمز الباركود أو QR الاختياري."
              },
              actionLabel = "إضافة منتج جديد",
              onActionClick = {
                editingProduct = null
                showFormDialog = true
              },
            )
          }
        }

        else -> {
          Column(
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث بالاسم، SKU، الباركود أو QR...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = CharcoalBlack) },
                trailingIcon = {
                  IconButton(
                    onClick = { showSearchScanner = true },
                    modifier = Modifier.testTag("admin_products_scan_search"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.QrCodeScanner,
                      contentDescription = "مسح الباركود أو QR للبحث",
                      tint = CharcoalBlack,
                    )
                  }
                },
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_products_search"),
                singleLine = true,
              )

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                AdminProductFilter.entries.forEach { filter ->
                  val count = when (filter) {
                    AdminProductFilter.ALL -> uiState.products.size
                    AdminProductFilter.OFFERS -> uiState.products.count { it.isOffer || it.hasDiscount }
                    AdminProductFilter.FEATURED -> uiState.products.count { it.isFeatured }
                    AdminProductFilter.LOW_STOCK -> uiState.products.count { it.isLowStock || it.isOutOfStock }
                    AdminProductFilter.HIDDEN -> uiState.products.count { !it.isActive }
                  }
                  val selected = selectedFilter == filter
                  FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filter },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = PrimaryYellow,
                      selectedLabelColor = CharcoalBlack,
                    ),
                    label = {
                      Text(
                        text = "${filter.label} ($count)",
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                      )
                    },
                  )
                }
              }
            }

            if (filteredProducts.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(AlEmanDimens.SpaceLg),
                contentAlignment = Alignment.Center,
              ) {
                StoreEmptyStateCard(
                  icon = Icons.Filled.Inventory2,
                  title = "لا توجد منتجات مطابقة",
                  description = "جرّب تغيير البحث أو اختيار تصنيف آخر.",
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                  start = AlEmanDimens.SpaceLg,
                  end = AlEmanDimens.SpaceLg,
                  top = 4.dp,
                  bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(filteredProducts, key = { it.id }) { product ->
                  val catName = uiState.categoryById[product.categoryId]?.name.orEmpty()
                  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      // الصف العلوي: الصورة + الاسم + الحالة
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                      ) {
                        Box(
                          modifier = Modifier
                            .size(66.dp)
                            .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
                            .background(GeneralBackground),
                        ) {
                          ProductThumbnail(
                            imageUrl = product.imageUrls.firstOrNull(),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                          )
                          if (product.imageUrls.size > 1) {
                            Surface(
                              shape = RoundedCornerShape(6.dp),
                              color = CharcoalBlack.copy(alpha = 0.85f),
                              modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp),
                            ) {
                              Text(
                                text = "+${product.imageUrls.size - 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryYellow,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                              )
                            }
                          }
                        }

                        Column(
                          modifier = Modifier.weight(1f),
                          verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                          ) {
                            Text(
                              text = product.name,
                              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                              color = TextPrimary,
                              maxLines = 2,
                              overflow = TextOverflow.Ellipsis,
                              modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (product.isActive) SuccessBadge("ظاهر") else DiscountBadge("مخفي")
                          }

                          FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                          ) {
                            if (catName.isNotBlank()) {
                              BrandPillBadge(
                                text = catName,
                                containerColor = PrimaryYellowSoft,
                                contentColor = CharcoalBlack,
                              )
                            }
                            if (product.isFeatured) {
                              BrandPillBadge(
                                text = "مميز",
                                containerColor = CharcoalBlack,
                                contentColor = PrimaryYellow,
                              )
                            }
                            if (product.isOffer || product.hasDiscount) {
                              BrandPillBadge(
                                text = if (product.hasDiscount) "خصم ${product.discountPercent}%" else "عرض خاص",
                                containerColor = PrimaryYellow,
                                contentColor = CharcoalBlack,
                              )
                            }
                            val companyName = product.companyId?.let { uiState.companyById[it]?.name }
                            if (!companyName.isNullOrBlank()) {
                              BrandPillBadge(
                                text = "شركة: $companyName",
                                containerColor = RoyalBlue.copy(alpha = 0.12f),
                                contentColor = RoyalBlue,
                              )
                            }
                            if (!product.companyId.isNullOrBlank() || !product.brandShowcaseId.isNullOrBlank()) {
                              BrandPillBadge(
                                text = if (product.exclusiveToBrandShowcase) "حصري لشركة" else "عام + شركة",
                                containerColor = if (product.exclusiveToBrandShowcase) DiscountRedSoft else RoyalBlue.copy(alpha = 0.12f),
                                contentColor = if (product.exclusiveToBrandShowcase) DiscountRed else RoyalBlue,
                              )
                            }
                            val alertCount = uiState.stockAlertCounts[product.id] ?: 0
                            if (alertCount > 0) {
                              BrandPillBadge(
                                text = "في قائمة الانتظار: $alertCount عميل 🔔",
                                containerColor = PrimaryYellowSoft,
                                contentColor = CharcoalBlack,
                              )
                            }
                          }
                        }
                      }

                      // جدول منظم للأسعار والمخزون بدون تقطع للكلمات
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GeneralBackground,
                        border = BorderStroke(1.dp, BorderNeutral),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                          ) {
                            Text("سعر القطاعي", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                            Text(
                              text = ArabicFormatter.formatCurrency(product.effectiveRetailPrice),
                              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                              color = CharcoalBlack,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                          }
                          Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                          ) {
                            Text("سعر الجملة (${product.wholesaleMinQty}+)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                            Text(
                              text = ArabicFormatter.formatCurrency(product.wholesalePrice),
                              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                              color = RoyalBlue,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                          }
                          Column(
                            modifier = Modifier.weight(0.85f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                          ) {
                            Text("المخزون", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                            Text(
                              text = "${product.stockQuantity} ${product.unit}",
                              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                              color = if (product.isOutOfStock) DiscountRed else CharcoalBlack,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                          }
                        }
                      }

                      if (product.sku.isNotBlank() || !product.barcode.isNullOrBlank() || !product.qrCode.isNullOrBlank()) {
                        val codeSummary = listOfNotNull(
                          product.sku.takeIf { it.isNotBlank() }?.let { "SKU: $it" },
                          product.barcode?.takeIf { it.isNotBlank() }?.let { "باركود: $it" },
                          product.qrCode?.takeIf { it.isNotBlank() }?.let { "QR: $it" },
                        ).joinToString("  •  ")
                        Text(
                          text = codeSummary,
                          style = MaterialTheme.typography.labelSmall,
                          color = TextSecondary,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }

                      HorizontalDivider(color = BorderNeutral)

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        OutlinedButton(
                          onClick = {
                            editingProduct = product
                            showFormDialog = true
                          },
                          border = BorderStroke(1.dp, BorderNeutral),
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier.weight(1f),
                        ) {
                          Icon(Icons.Filled.Edit, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("تعديل المنتج", color = CharcoalBlack, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        OutlinedButton(
                          onClick = { deletingProduct = product },
                          border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.45f)),
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier.weight(1f),
                        ) {
                          Icon(Icons.Filled.Delete, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("حذف", color = DiscountRed, fontWeight = FontWeight.Bold, maxLines = 1)
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
  }

  if (showSearchScanner) {
    QrBarcodeScannerDialog(
      title = "مسح باركود أو QR للبحث في المنتجات",
      onCodeScanned = { code ->
        searchQuery = code
        showSearchScanner = false
      },
      onDismiss = { showSearchScanner = false },
    )
  }

  if (showFormDialog) {
    ProductFormDialog(
      existing = editingProduct,
      categories = uiState.categories,
      companies = uiState.companies,
      onDismiss = { showFormDialog = false },
      onConfirm = { prod ->
        showFormDialog = false
        onSaveProduct(prod)
      },
    )
  }

  val toDelete = deletingProduct
  if (toDelete != null) {
    AlertDialog(
      onDismissRequest = { deletingProduct = null },
      containerColor = PureWhite,
      title = {
        Text(
          text = "حذف المنتج",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = DiscountRed,
        )
      },
      text = { Text("هل أنت متأكد من حذف منتج «${toDelete.name}»؟") },
      confirmButton = {
        Button(
          onClick = {
            deletingProduct = null
            onDeleteProduct(toDelete.id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
        ) {
          Text("حذف")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingProduct = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProductFormDialog(
  existing: Product?,
  categories: List<Category>,
  companies: List<StoreCompany>,
  onDismiss: () -> Unit,
  onConfirm: (Product) -> Unit,
) {
  val mainCategories = remember(categories) { categories.filter { it.isMainCategory } }

  var name by remember { mutableStateOf(existing?.name.orEmpty()) }
  var description by remember { mutableStateOf(existing?.description.orEmpty()) }
  var categoryId by remember {
    mutableStateOf(existing?.categoryId?.takeIf { it.isNotBlank() } ?: mainCategories.firstOrNull()?.id.orEmpty())
  }
  var subCategoryId by remember { mutableStateOf(existing?.subCategoryId) }
  var companyId by remember { mutableStateOf(existing?.companyId?.takeIf { it.isNotBlank() }) }
  var sku by remember { mutableStateOf(existing?.sku.orEmpty()) }
  var barcode by remember { mutableStateOf(existing?.barcode.orEmpty()) }
  var qrCode by remember { mutableStateOf(existing?.qrCode.orEmpty()) }
  var imageUrls by remember { mutableStateOf(existing?.imageUrls.orEmpty()) }
  var availableColors by remember { mutableStateOf(existing?.availableColors.orEmpty()) }
  var availableSizes by remember { mutableStateOf(existing?.availableSizes.orEmpty()) }
  var newColorInput by remember { mutableStateOf("") }
  var newSizeInput by remember { mutableStateOf("") }
  var retailPrice by remember { mutableStateOf(existing?.retailPrice?.takeIf { it > 0.0 }?.toString().orEmpty()) }
  var retailDiscountPrice by remember { mutableStateOf(existing?.retailDiscountPrice?.toString().orEmpty()) }
  var wholesalePrice by remember { mutableStateOf(existing?.wholesalePrice?.takeIf { it > 0.0 }?.toString().orEmpty()) }
  var wholesaleMinQty by remember { mutableStateOf((existing?.wholesaleMinQty ?: 10).toString()) }
  var unit by remember { mutableStateOf(existing?.unit ?: "قطعة") }
  var stockQuantity by remember { mutableStateOf((existing?.stockQuantity ?: 50).toString()) }
  var lowStockThreshold by remember { mutableStateOf((existing?.lowStockThreshold ?: 5).toString()) }
  var isFeatured by remember { mutableStateOf(existing?.isFeatured ?: false) }
  var isOffer by remember { mutableStateOf(existing?.isOffer ?: false) }
  var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
  var isExclusiveToBrand by remember(existing?.id) {
    mutableStateOf(existing?.exclusiveToBrandShowcase == true)
  }

  var catExpanded by remember { mutableStateOf(false) }
  var subCatExpanded by remember { mutableStateOf(false) }
  var companyExpanded by remember { mutableStateOf(false) }
  var scanTarget by remember { mutableStateOf<AdminScanTarget?>(null) }
  var showImagePickerModal by remember { mutableStateOf(false) }
  var imageError by remember { mutableStateOf<String?>(null) }

  val subCategories = remember(categories, categoryId) {
    categories.filter { it.parentId == categoryId }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة منتج جديد" else "تعديل بيانات المنتج",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // قسم صور المنتج المتعددة (حتى 10 صور)
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "صور المنتج (حتى 10 صور)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
              BrandPillBadge(
                text = "${imageUrls.size} / 10",
                containerColor = PrimaryYellow,
                contentColor = CharcoalBlack,
              )
            }

            if (imageUrls.isEmpty()) {
              Text(
                text = "لم تُضف صور لهذا المنتج بعد. أضف صورة رئيسية وصورًا إضافية للعرض في صفحة المنتج.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            } else {
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
              ) {
                itemsIndexed(imageUrls) { idx, url ->
                  Surface(
                    shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                    color = PureWhite,
                    border = BorderStroke(
                      width = if (idx == 0) 2.dp else 1.dp,
                      color = if (idx == 0) PrimaryYellow else BorderNeutral,
                    ),
                    modifier = Modifier.width(104.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(6.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(78.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(GeneralBackground),
                      ) {
                        ProductThumbnail(
                          imageUrl = url,
                          contentDescription = "صورة المنتج ${idx + 1}",
                          modifier = Modifier.fillMaxSize(),
                        )
                        if (idx == 0) {
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryYellow,
                            modifier = Modifier
                              .align(Alignment.TopStart)
                              .padding(3.dp),
                          ) {
                            Text(
                              text = "رئيسية",
                              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                              color = CharcoalBlack,
                              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                          }
                        }
                        Surface(
                          shape = CircleShape,
                          color = PureWhite.copy(alpha = 0.9f),
                          modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(22.dp)
                            .clickable {
                              imageUrls = imageUrls.toMutableList().apply { removeAt(idx) }
                            },
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(
                              imageVector = Icons.Filled.Close,
                              contentDescription = "حذف الصورة",
                              tint = DiscountRed,
                              modifier = Modifier.size(14.dp),
                            )
                          }
                        }
                      }

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        IconButton(
                          onClick = {
                            if (idx > 0) {
                              imageUrls = imageUrls.toMutableList().apply {
                                val moved = removeAt(idx)
                                add(idx - 1, moved)
                              }
                            }
                          },
                          enabled = idx > 0,
                          modifier = Modifier.size(26.dp),
                        ) {
                          Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "تقديم الصورة",
                            tint = if (idx > 0) CharcoalBlack else TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(15.dp),
                          )
                        }
                        IconButton(
                          onClick = {
                            if (idx != 0) {
                              imageUrls = imageUrls.toMutableList().apply {
                                val moved = removeAt(idx)
                                add(0, moved)
                              }
                            }
                          },
                          enabled = idx != 0,
                          modifier = Modifier.size(26.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "تعيين كصورة رئيسية",
                            tint = if (idx == 0) PrimaryYellow else CharcoalBlack,
                            modifier = Modifier.size(15.dp),
                          )
                        }
                        IconButton(
                          onClick = {
                            if (idx < imageUrls.lastIndex) {
                              imageUrls = imageUrls.toMutableList().apply {
                                val moved = removeAt(idx)
                                add(idx + 1, moved)
                              }
                            }
                          },
                          enabled = idx < imageUrls.lastIndex,
                          modifier = Modifier.size(26.dp),
                        ) {
                          Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "تأخير الصورة",
                            tint = if (idx < imageUrls.lastIndex) CharcoalBlack else TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(15.dp),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            if (!imageError.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                color = DiscountRedSoft,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = imageError.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = DiscountRed,
                  modifier = Modifier.padding(8.dp),
                )
              }
            }

            OutlinedButton(
              onClick = {
                imageError = null
                showImagePickerModal = true
              },
              enabled = imageUrls.size < 10,
              shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
              border = BorderStroke(1.dp, CharcoalBlack),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_add_product_images_button"),
            ) {
              Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (imageUrls.size >= 10) "تم بلوغ الحد الأقصى (10 صور)" else "إضافة صور (معرض متعدد / كاميرا)",
                color = CharcoalBlack,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("اسم المنتج *") },
          singleLine = true,
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("وصف المنتج") },
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier.fillMaxWidth(),
        )

        // اختيار القسم الرئيسي والفرعي
        ExposedDropdownMenuBox(
          expanded = catExpanded,
          onExpandedChange = { catExpanded = !catExpanded },
        ) {
          val selectedCatName = mainCategories.firstOrNull { it.id == categoryId }?.name
            ?: "اختر القسم الرئيسي *"
          OutlinedTextField(
            value = selectedCatName,
            onValueChange = {},
            readOnly = true,
            label = { Text("القسم الرئيسي *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = catExpanded,
            onDismissRequest = { catExpanded = false },
            containerColor = PureWhite,
          ) {
            mainCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  categoryId = cat.id
                  subCategoryId = null
                  catExpanded = false
                },
              )
            }
          }
        }

        if (subCategories.isNotEmpty()) {
          ExposedDropdownMenuBox(
            expanded = subCatExpanded,
            onExpandedChange = { subCatExpanded = !subCatExpanded },
          ) {
            val selectedSubName = subCategories.firstOrNull { it.id == subCategoryId }?.name
              ?: "بدون قسم فرعي"
            OutlinedTextField(
              value = selectedSubName,
              onValueChange = {},
              readOnly = true,
              label = { Text("القسم الفرعي (اختياري)") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCatExpanded) },
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = subCatExpanded,
              onDismissRequest = { subCatExpanded = false },
              containerColor = PureWhite,
            ) {
              DropdownMenuItem(
                text = { Text("بدون قسم فرعي") },
                onClick = {
                  subCategoryId = null
                  subCatExpanded = false
                },
              )
              subCategories.forEach { sub ->
                DropdownMenuItem(
                  text = { Text(sub.name) },
                  onClick = {
                    subCategoryId = sub.id
                    subCatExpanded = false
                  },
                )
              }
            }
          }
        }

        // اختيار الشركة / الماركة التابع لها المنتج (اختياري)
        ExposedDropdownMenuBox(
          expanded = companyExpanded,
          onExpandedChange = { companyExpanded = !companyExpanded },
        ) {
          val selectedCompanyName = companies.firstOrNull { it.id == companyId }?.let { comp ->
            if (comp.isActive) comp.name else "${comp.name} (معطلة)"
          } ?: "بدون شركة محددة"
          OutlinedTextField(
            value = selectedCompanyName,
            onValueChange = {},
            readOnly = true,
            label = { Text("الشركة التابع لها (اختياري)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded) },
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("admin_product_company_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = companyExpanded,
            onDismissRequest = { companyExpanded = false },
            containerColor = PureWhite,
          ) {
            DropdownMenuItem(
              text = { Text("بدون شركة محددة") },
              onClick = {
                companyId = null
                companyExpanded = false
              },
            )
            companies.forEach { comp ->
              DropdownMenuItem(
                text = {
                  Text(if (comp.isActive) comp.name else "${comp.name} (معطلة)")
                },
                onClick = {
                  companyId = comp.id
                  companyExpanded = false
                },
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = sku,
            onValueChange = { sku = it },
            label = { Text("رمز الصنف SKU *") },
            singleLine = true,
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text("وحدة البيع") },
            singleLine = true,
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier.weight(1f),
          )
        }

        // حقل الباركود الاختياري مع زر المسح بالكاميرا
        OutlinedTextField(
          value = barcode,
          onValueChange = { barcode = it },
          label = { Text("الباركود (اختياري)") },
          placeholder = { Text("أدخل أو امسح الباركود") },
          singleLine = true,
          trailingIcon = {
            IconButton(
              onClick = { scanTarget = AdminScanTarget.BARCODE },
              modifier = Modifier.testTag("admin_scan_barcode_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.QrCodeScanner,
                contentDescription = "مسح الباركود بالكاميرا",
                tint = RoyalBlue,
              )
            }
          },
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_product_barcode_input"),
        )

        // حقل رمز QR الاختياري مع زر المسح بالكاميرا
        OutlinedTextField(
          value = qrCode,
          onValueChange = { qrCode = it },
          label = { Text("رمز QR (اختياري)") },
          placeholder = { Text("أدخل أو امسح رمز QR") },
          singleLine = true,
          trailingIcon = {
            IconButton(
              onClick = { scanTarget = AdminScanTarget.QR_CODE },
              modifier = Modifier.testTag("admin_scan_qrcode_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.QrCode2,
                contentDescription = "مسح رمز QR بالكاميرا",
                tint = RoyalBlue,
              )
            }
          },
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_product_qrcode_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = retailPrice,
            onValueChange = { retailPrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("سعر القطاعي *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = retailDiscountPrice,
            onValueChange = { retailDiscountPrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("سعر بعد الخصم") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier.weight(1f),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = wholesalePrice,
            onValueChange = { wholesalePrice = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text("سعر الجملة") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = wholesaleMinQty,
            onValueChange = { wholesaleMinQty = it.filter { ch -> ch.isDigit() } },
            label = { Text("أقل كمية جملة") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier.weight(1f),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = stockQuantity,
            onValueChange = { stockQuantity = it.filter { ch -> ch.isDigit() } },
            label = { Text("المخزون") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = lowStockThreshold,
            onValueChange = { lowStockThreshold = it.filter { ch -> ch.isDigit() } },
            label = { Text("حد التنبيه") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier.weight(1f),
          )
        }

        FormToggleRow(label = "إظهار المنتج في المتجر", checked = isActive, onChange = { isActive = it })
        FormToggleRow(label = "منتج مميز في الرئيسية", checked = isFeatured, onChange = { isFeatured = it })
        FormToggleRow(label = "إدراج ضمن العروض", checked = isOffer, onChange = { isOffer = it })
        if (!companyId.isNullOrBlank() || !existing?.brandShowcaseId.isNullOrBlank()) {
          FormToggleRow(
            label = "حصري للشركة فقط (يخفي من المتجر الرئيسي)",
            checked = isExclusiveToBrand,
            onChange = { isExclusiveToBrand = it },
          )
        }

        // قسم الألوان والمقاسات الاختيارية للمنتج
        ProductVariantValuesEditor(
          title = "الألوان المتاحة (اختياري)",
          placeholder = "أضف لونًا مثل: أزرق",
          values = availableColors,
          pendingInput = newColorInput,
          presets = listOf("أبيض", "أسود", "أزرق", "أحمر", "أخضر", "أصفر", "رمادي"),
          onPendingInputChange = { newColorInput = it },
          onAddValue = { value ->
            val trimmed = value.trim()
            if (trimmed.isNotBlank() && trimmed !in availableColors && availableColors.size < 30) {
              availableColors = availableColors + trimmed
            }
          },
          onRemoveValue = { value -> availableColors = availableColors.filterNot { it == value } },
          onClearPending = { newColorInput = "" },
        )

        ProductVariantValuesEditor(
          title = "المقاسات / الأحجام المتاحة (اختياري)",
          placeholder = "أضف مقاسًا مثل: 500 مل أو XL",
          values = availableSizes,
          pendingInput = newSizeInput,
          presets = listOf("S", "M", "L", "XL", "XXL", "500 مل", "1 لتر", "5 لتر", "20 لتر"),
          onPendingInputChange = { newSizeInput = it },
          onAddValue = { value ->
            val trimmed = value.trim()
            if (trimmed.isNotBlank() && trimmed !in availableSizes && availableSizes.size < 30) {
              availableSizes = availableSizes + trimmed
            }
          },
          onRemoveValue = { value -> availableSizes = availableSizes.filterNot { it == value } },
          onClearPending = { newSizeInput = "" },
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            (existing ?: Product()).copy(
              name = name,
              description = description,
              categoryId = categoryId,
              subCategoryId = subCategoryId,
              companyId = companyId,
              sku = sku,
              barcode = barcode.takeIf { it.isNotBlank() },
              qrCode = qrCode.takeIf { it.isNotBlank() },
              imageUrls = imageUrls.take(10),
              availableColors = availableColors.take(30),
              availableSizes = availableSizes.take(30),
              retailPrice = retailPrice.toDoubleOrNull() ?: 0.0,
              retailDiscountPrice = retailDiscountPrice.toDoubleOrNull(),
              wholesalePrice = wholesalePrice.toDoubleOrNull() ?: 0.0,
              wholesaleMinQty = (wholesaleMinQty.toIntOrNull() ?: 1).coerceAtLeast(1),
              unit = unit.ifBlank { "قطعة" },
              stockQuantity = stockQuantity.toIntOrNull() ?: 0,
              lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 5,
              isFeatured = isFeatured,
              isOffer = isOffer,
              isActive = isActive,
              exclusiveToBrandShowcase = isExclusiveToBrand && (!companyId.isNullOrBlank() || !existing?.brandShowcaseId.isNullOrBlank()),
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
        modifier = Modifier.testTag("admin_save_product_button"),
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
    visible = showImagePickerModal,
    title = "إضافة صور المنتج",
    folder = CloudinaryFolder.PRODUCTS,
    allowMultiple = true,
    maxMultipleCount = (10 - imageUrls.size).coerceAtLeast(1),
    onUploadedUrls = { newUrls ->
      imageUrls = (imageUrls + newUrls).distinct().take(10)
      showImagePickerModal = false
    },
    onError = { err ->
      imageError = err
      showImagePickerModal = false
    },
    onDismiss = { showImagePickerModal = false },
  )

  val activeScanTarget = scanTarget
  if (activeScanTarget != null) {
    QrBarcodeScannerDialog(
      title = if (activeScanTarget == AdminScanTarget.BARCODE) {
        "مسح باركود المنتج"
      } else {
        "مسح رمز QR للمنتج"
      },
      onCodeScanned = { code ->
        if (activeScanTarget == AdminScanTarget.BARCODE) {
          barcode = code
        } else {
          qrCode = code
        }
        scanTarget = null
      },
      onDismiss = { scanTarget = null },
    )
  }
}

@Composable
private fun FormToggleRow(
  label: String,
  checked: Boolean,
  onChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    Spacer(modifier = Modifier.width(8.dp))
    Switch(
      checked = checked,
      onCheckedChange = onChange,
      colors = SwitchDefaults.colors(checkedThumbColor = CharcoalBlack, checkedTrackColor = PrimaryYellow),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductVariantValuesEditor(
  title: String,
  placeholder: String,
  values: List<String>,
  pendingInput: String,
  presets: List<String>,
  onPendingInputChange: (String) -> Unit,
  onAddValue: (String) -> Unit,
  onRemoveValue: (String) -> Unit,
  onClearPending: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
    color = GeneralBackground,
    border = BorderStroke(1.dp, BorderNeutral),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = pendingInput,
          onValueChange = onPendingInputChange,
          placeholder = { Text(placeholder) },
          singleLine = true,
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          modifier = Modifier.weight(1f),
        )
        Button(
          onClick = {
            if (pendingInput.isNotBlank()) {
              onAddValue(pendingInput)
              onClearPending()
            }
          },
          enabled = pendingInput.isNotBlank() && values.size < 30,
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
        ) {
          Icon(Icons.Filled.Add, contentDescription = "إضافة", modifier = Modifier.size(16.dp))
        }
      }

      if (values.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          values.forEach { item ->
            FilterChip(
              selected = true,
              onClick = { onRemoveValue(item) },
              label = { Text(item, fontWeight = FontWeight.Bold) },
              trailingIcon = {
                Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = "حذف $item",
                  modifier = Modifier.size(14.dp),
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryYellowSoft,
                selectedLabelColor = CharcoalBlack,
              ),
            )
          }
        }
      }

      val remainingPresets = presets.filterNot { it in values }
      if (remainingPresets.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          remainingPresets.forEach { preset ->
            FilterChip(
              selected = false,
              onClick = { onAddValue(preset) },
              label = { Text("+ $preset", style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }
    }
  }
}
