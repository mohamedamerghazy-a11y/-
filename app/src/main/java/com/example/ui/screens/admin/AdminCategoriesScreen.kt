package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.data.media.CloudinaryFolder
import com.example.data.model.Category
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminCatalogUiState

private enum class AdminCategoryFilter(val label: String) {
  ALL("الكل"),
  MAIN("أقسام رئيسية"),
  SUB("أقسام فرعية"),
  HIDDEN("مخفية"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCategoriesScreen(
  uiState: AdminCatalogUiState,
  onSaveCategory: (Category) -> Unit,
  onDeleteCategory: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingCategory by remember { mutableStateOf<Category?>(null) }
  var showFormDialog by remember { mutableStateOf(false) }
  var deletingCategory by remember { mutableStateOf<Category?>(null) }
  var selectedFilter by remember { mutableStateOf(AdminCategoryFilter.ALL) }

  val filteredCategories = remember(uiState.categories, selectedFilter) {
    uiState.categories.filter { cat ->
      when (selectedFilter) {
        AdminCategoryFilter.ALL -> true
        AdminCategoryFilter.MAIN -> cat.isMainCategory
        AdminCategoryFilter.SUB -> !cat.isMainCategory
        AdminCategoryFilter.HIDDEN -> !cat.isActive
      }
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
        title = "إدارة الأقسام",
        subtitle = "الأقسام الرئيسية والفرعية (${uiState.categories.size} قسم)",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingCategory = null
          showFormDialog = true
        },
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier.testTag("admin_add_category_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "إضافة قسم جديد")
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = RoyalBlue)
        }
      } else if (uiState.categories.isEmpty()) {
        Box(
          modifier = Modifier
            .widthIn(max = 600.dp)
            .padding(AlEmanDimens.SpaceLg),
        ) {
          StoreEmptyStateCard(
            icon = Icons.Filled.Category,
            title = "لا توجد أقسام حتى الآن",
            description = "ابدأ بإضافة الأقسام الرئيسية والفرعية لعرضها للعملاء في المتجر.",
            actionLabel = "إضافة قسم جديد",
            onActionClick = {
              editingCategory = null
              showFormDialog = true
            },
          )
        }
      } else {
        Column(
          modifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxSize(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AdminCategoryFilter.entries.forEach { filter ->
              val count = when (filter) {
                AdminCategoryFilter.ALL -> uiState.categories.size
                AdminCategoryFilter.MAIN -> uiState.categories.count { it.isMainCategory }
                AdminCategoryFilter.SUB -> uiState.categories.count { !it.isMainCategory }
                AdminCategoryFilter.HIDDEN -> uiState.categories.count { !it.isActive }
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

          if (filteredCategories.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(AlEmanDimens.SpaceLg),
              contentAlignment = Alignment.Center,
            ) {
              StoreEmptyStateCard(
                icon = Icons.Filled.Category,
                title = "لا توجد أقسام ضمن هذا الفلتر",
                description = "اختر فلترًا آخر أو أضف قسمًا جديدًا.",
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
              items(filteredCategories, key = { it.id }) { category ->
                val parentName = category.parentId?.let { uiState.categoryById[it]?.name }
                AlEmanCard(modifier = Modifier.fillMaxWidth()) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      Box(
                        modifier = Modifier
                          .size(58.dp)
                          .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
                          .background(PureWhite),
                      ) {
                        ProductThumbnail(
                          imageUrl = category.imageUrl,
                          contentDescription = category.name,
                          contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                          modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        )
                      }
                      Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                          )
                          Spacer(modifier = Modifier.width(8.dp))
                          if (category.isActive) {
                            SuccessBadge(text = "مفعّل")
                          } else {
                            DiscountBadge(text = "مخفي")
                          }
                        }
                        FlowRow(
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                          verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          BrandPillBadge(
                            text = if (parentName != null) "فرعي من: $parentName" else "قسم رئيسي",
                            containerColor = if (parentName != null) PrimaryYellowSoft else PrimaryYellow,
                            contentColor = CharcoalBlack,
                          )
                          BrandPillBadge(
                            text = "الترتيب: ${category.displayOrder}",
                            containerColor = GeneralBackground,
                            contentColor = TextSecondary,
                          )
                        }
                      }
                    }

                    if (!category.description.isNullOrBlank()) {
                      Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
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
                          editingCategory = category
                          showFormDialog = true
                        },
                        border = BorderStroke(1.dp, BorderNeutral),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                      ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل القسم", color = CharcoalBlack, fontWeight = FontWeight.Bold, maxLines = 1)
                      }
                      OutlinedButton(
                        onClick = { deletingCategory = category },
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

  if (showFormDialog) {
    CategoryFormDialog(
      existing = editingCategory,
      mainCategories = uiState.mainCategories.filter { it.id != editingCategory?.id },
      onDismiss = { showFormDialog = false },
      onConfirm = { cat ->
        showFormDialog = false
        onSaveCategory(cat)
      },
    )
  }

  val toDelete = deletingCategory
  if (toDelete != null) {
    AlertDialog(
      onDismissRequest = { deletingCategory = null },
      containerColor = PureWhite,
      title = {
        Text(
          text = "حذف القسم",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = DiscountRed,
        )
      },
      text = { Text("هل أنت متأكد من حذف قسم «${toDelete.name}»؟") },
      confirmButton = {
        Button(
          onClick = {
            deletingCategory = null
            onDeleteCategory(toDelete.id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
        ) {
          Text("حذف")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingCategory = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormDialog(
  existing: Category?,
  mainCategories: List<Category>,
  onDismiss: () -> Unit,
  onConfirm: (Category) -> Unit,
) {
  var name by remember { mutableStateOf(existing?.name.orEmpty()) }
  var description by remember { mutableStateOf(existing?.description.orEmpty()) }
  var parentId by remember { mutableStateOf(existing?.parentId) }
  var imageUrl by remember { mutableStateOf(existing?.imageUrl) }
  var displayOrder by remember { mutableStateOf((existing?.displayOrder ?: 1).toString()) }
  var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
  var parentExpanded by remember { mutableStateOf(false) }
  var showImagePickerModal by remember { mutableStateOf(false) }
  var imageError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة قسم جديد" else "تعديل القسم",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          color = GeneralBackground,
          border = BorderStroke(1.dp, BorderNeutral),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "صورة القسم (اختياري)",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
                  .background(PureWhite),
              ) {
                ProductThumbnail(
                  imageUrl = imageUrl,
                  contentDescription = name.ifBlank { "صورة القسم" },
                  contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                )
              }
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                OutlinedButton(
                  onClick = {
                    imageError = null
                    showImagePickerModal = true
                  },
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  border = BorderStroke(1.dp, CharcoalBlack),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_category_upload_image_button"),
                ) {
                  Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (imageUrl.isNullOrBlank()) "رفع صورة للقسم" else "تغيير الصورة",
                    color = CharcoalBlack,
                    fontWeight = FontWeight.Bold,
                  )
                }
                if (!imageUrl.isNullOrBlank()) {
                  TextButton(
                    onClick = { imageUrl = null },
                    modifier = Modifier.align(Alignment.End),
                  ) {
                    Text("حذف الصورة", color = DiscountRed)
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
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("اسم القسم *") },
          singleLine = true,
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("وصف القسم (اختياري)") },
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenuBox(
          expanded = parentExpanded,
          onExpandedChange = { parentExpanded = !parentExpanded },
        ) {
          val selectedParentName = mainCategories.firstOrNull { it.id == parentId }?.name ?: "قسم رئيسي (بدون قسم أب)"
          OutlinedTextField(
            value = selectedParentName,
            onValueChange = {},
            readOnly = true,
            label = { Text("يتبع قسم رئيسي") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = parentExpanded,
            onDismissRequest = { parentExpanded = false },
            containerColor = PureWhite,
          ) {
            DropdownMenuItem(
              text = { Text("قسم رئيسي (بدون قسم أب)") },
              onClick = {
                parentId = null
                parentExpanded = false
              },
            )
            mainCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  parentId = cat.id
                  parentExpanded = false
                },
              )
            }
          }
        }
        OutlinedTextField(
          value = displayOrder,
          onValueChange = { displayOrder = it.filter { ch -> ch.isDigit() } },
          label = { Text("ترتيب العرض") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("إظهار القسم للعملاء", style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = isActive,
            onCheckedChange = { isActive = it },
            colors = SwitchDefaults.colors(checkedThumbColor = CharcoalBlack, checkedTrackColor = PrimaryYellow),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            (existing ?: Category()).copy(
              name = name,
              description = description.takeIf { it.isNotBlank() },
              parentId = parentId,
              imageUrl = imageUrl?.takeIf { it.isNotBlank() },
              displayOrder = displayOrder.toIntOrNull() ?: 1,
              isActive = isActive,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = CharcoalBlack),
      ) {
        Text("حفظ", fontWeight = FontWeight.Bold)
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
    title = "صورة القسم",
    folder = CloudinaryFolder.CATEGORIES,
    allowMultiple = false,
    onUploadedUrls = { urls ->
      imageUrl = urls.firstOrNull()
      showImagePickerModal = false
    },
    onError = { err ->
      imageError = err
      showImagePickerModal = false
    },
    onDismiss = { showImagePickerModal = false },
  )
}
