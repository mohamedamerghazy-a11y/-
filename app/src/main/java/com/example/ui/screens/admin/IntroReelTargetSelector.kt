package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.util.ArabicFormatter
import com.example.data.model.BrandShowcase
import com.example.data.model.Category
import com.example.data.model.IntroReelTargetType
import com.example.data.model.Product
import com.example.data.model.StoreCompany
import com.example.data.model.matchesBrandShowcase
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class ReelTargetOption(
  val id: String,
  val title: String,
  val subtitle: String,
  val imageUrl: String = "",
)

@Composable
fun IntroReelTargetSelector(
  targetType: IntroReelTargetType,
  targetId: String,
  targetLabel: String,
  categories: List<Category>,
  products: List<Product>,
  companies: List<StoreCompany>,
  brandShowcases: List<BrandShowcase>,
  onTargetTypeChange: (IntroReelTargetType) -> Unit,
  onSelectTarget: (id: String, label: String) -> Unit,
) {
  var showPickerDialog by remember { mutableStateOf(false) }

  val categoryOptions = remember(categories, products) {
    val byId = categories.associateBy { it.id }
    categories
      .sortedWith(compareBy<Category> { !it.isMainCategory }.thenBy { it.displayOrder }.thenBy { it.name })
      .map { cat ->
        val parentName = cat.parentId?.let { byId[it]?.name }
        val count = products.count { p ->
          if (cat.isMainCategory) p.categoryId == cat.id else p.subCategoryId == cat.id
        }
        val subtitle = if (parentName != null) {
          "قسم فرعي داخل $parentName • ${ArabicFormatter.formatNumber(count)} منتج"
        } else {
          "قسم رئيسي • ${ArabicFormatter.formatNumber(count)} منتج"
        }
        ReelTargetOption(
          id = cat.id,
          title = cat.name,
          subtitle = subtitle,
          imageUrl = cat.imageUrl.orEmpty(),
        )
      }
  }

  val productOptions = remember(products, categories) {
    val catById = categories.associateBy { it.id }
    products
      .sortedByDescending { it.createdAt?.seconds ?: 0L }
      .map { p ->
        val catName = catById[p.categoryId]?.name.orEmpty()
        val priceText = ArabicFormatter.formatCurrency(p.effectiveRetailPrice)
        val subtitle = buildString {
          append(priceText)
          if (catName.isNotBlank()) {
            append(" • ")
            append(catName)
          }
          if (p.isOutOfStock) {
            append(" • نفذ المخزون")
          }
        }
        ReelTargetOption(
          id = p.id,
          title = p.name,
          subtitle = subtitle,
          imageUrl = p.imageUrls.firstOrNull().orEmpty(),
        )
      }
  }

  val companyOptions = remember(companies, brandShowcases, products) {
    val list = mutableListOf<ReelTargetOption>()
    val coveredCompanyIds = mutableSetOf<String>()

    brandShowcases
      .sortedWith(compareBy<BrandShowcase> { !it.isActive }.thenBy { it.displayOrder }.thenBy { it.title })
      .forEach { showcase ->
        if (!showcase.companyId.isNullOrBlank()) {
          coveredCompanyIds += showcase.companyId
        }
        val count = products.count { it.matchesBrandShowcase(showcase) }
        val discountBadge = if (showcase.cleanDiscountPercent > 0) {
          " • خصم ${ArabicFormatter.formatNumber(showcase.cleanDiscountPercent)}%"
        } else {
          ""
        }
        list += ReelTargetOption(
          id = showcase.id,
          title = showcase.title,
          subtitle = "شركة / عرض مميز • ${ArabicFormatter.formatNumber(count)} منتج$discountBadge",
          imageUrl = showcase.companyImageUrl?.ifBlank { showcase.bannerImageUrl } ?: showcase.bannerImageUrl,
        )
      }

    companies
      .filter { it.id !in coveredCompanyIds }
      .sortedWith(compareBy<StoreCompany> { it.displayOrder }.thenBy { it.name })
      .forEach { company ->
        val count = products.count { it.companyId == company.id }
        list += ReelTargetOption(
          id = "company:${company.id}",
          title = company.name,
          subtitle = "شركة • ${ArabicFormatter.formatNumber(count)} منتج",
          imageUrl = company.imageUrl,
        )
      }
    list
  }

  val activeOptions = when (targetType) {
    IntroReelTargetType.PROMO_ONLY -> emptyList()
    IntroReelTargetType.CATEGORY -> categoryOptions
    IntroReelTargetType.PRODUCT -> productOptions
    IntroReelTargetType.COMPANY -> companyOptions
  }

  val resolvedSelectedOption = remember(targetType, targetId, activeOptions) {
    val cleanId = targetId.trim()
    if (cleanId.isEmpty()) {
      null
    } else {
      activeOptions.firstOrNull {
        it.id == cleanId || it.id == "company:$cleanId" || it.id.removePrefix("company:") == cleanId
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 4.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.TouchApp,
        contentDescription = null,
        tint = RoyalBlue,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "إجراء الضغط على مقطع الريلز",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = CharcoalBlack,
      )
    }

    Text(
      text = "حدد هل المقطع دعائي فقط أم ينقل العميل عند الضغط على أي جزء منه لقسم أو منتج أو منتجات شركة. زر «تخطى» يفتح المتجر الرئيسي دائمًا.",
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary,
    )

    // 2x2 clean grid so Arabic labels never break or wrap awkwardly
    val rows = remember {
      listOf(
        IntroReelTargetType.PROMO_ONLY to IntroReelTargetType.CATEGORY,
        IntroReelTargetType.PRODUCT to IntroReelTargetType.COMPANY,
      )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      rows.forEach { (first, second) ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          IntroReelTargetTypeCard(
            type = first,
            selected = targetType == first,
            onClick = { onTargetTypeChange(first) },
            modifier = Modifier.weight(1f),
          )
          IntroReelTargetTypeCard(
            type = second,
            selected = targetType == second,
            onClick = { onTargetTypeChange(second) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    if (targetType != IntroReelTargetType.PROMO_ONLY) {
      val pickerActionText = when (targetType) {
        IntroReelTargetType.CATEGORY -> "اختيار القسم"
        IntroReelTargetType.PRODUCT -> "اختيار المنتج"
        IntroReelTargetType.COMPANY -> "اختيار الشركة"
        IntroReelTargetType.PROMO_ONLY -> ""
      }
      val displayTitle = resolvedSelectedOption?.title
        ?: targetLabel.trim().takeIf { it.isNotEmpty() && targetId.isNotBlank() }
      val displaySubtitle = resolvedSelectedOption?.subtitle
        ?: if (targetId.isNotBlank()) "تم التحديد مسبقًا" else null

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (displayTitle != null) RoyalBlueSoft.copy(alpha = 0.45f) else Color(0xFFFFF8E1),
        border = BorderStroke(
          width = 1.dp,
          color = if (displayTitle != null) RoyalBlue.copy(alpha = 0.45f) else PrimaryYellow,
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (displayTitle != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              val thumb = resolvedSelectedOption?.imageUrl.orEmpty()
              if (thumb.isNotBlank()) {
                AsyncImage(
                  model = thumb,
                  contentDescription = displayTitle,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureWhite),
                )
              } else {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureWhite),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = targetTypeIcon(targetType),
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "الوجهة المحددة عند الضغط",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                  )
                }
                Text(
                  text = displayTitle,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                  color = CharcoalBlack,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                if (!displaySubtitle.isNullOrBlank()) {
                  Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = Color(0xFFB45309),
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "لم يتم تحديد وجهة بعد — اضغط بالأسفل لاختيار الوجهة المطلوبة قبل الحفظ.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = CharcoalBlack,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { showPickerDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = CharcoalBlack,
                contentColor = PrimaryYellow,
              ),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("intro_reel_pick_target_button"),
            ) {
              Text(
                text = if (displayTitle != null) "تغيير الاختيار" else pickerActionText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (displayTitle != null) {
              OutlinedButton(
                onClick = { onSelectTarget("", "") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("intro_reel_clear_target_button"),
              ) {
                Text("مسح", fontWeight = FontWeight.Bold, color = CharcoalBlack)
              }
            }
          }
        }
      }
    }
  }

  if (showPickerDialog && targetType != IntroReelTargetType.PROMO_ONLY) {
    IntroReelTargetPickerDialog(
      targetType = targetType,
      options = activeOptions,
      selectedId = resolvedSelectedOption?.id ?: targetId,
      onSelect = { opt ->
        onSelectTarget(opt.id, opt.title)
        showPickerDialog = false
      },
      onDismiss = { showPickerDialog = false },
    )
  }
}

@Composable
private fun IntroReelTargetTypeCard(
  type: IntroReelTargetType,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = if (selected) CharcoalBlack else PureWhite,
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) PrimaryYellow else BorderNeutral,
    ),
    modifier = modifier
      .heightIn(min = 70.dp)
      .testTag("intro_reel_target_type_${type.code}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = targetTypeIcon(type),
          contentDescription = null,
          tint = if (selected) PrimaryYellow else RoyalBlue,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = type.arabicLabel,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
          color = if (selected) PureWhite else CharcoalBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = type.subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) PureWhite.copy(alpha = 0.78f) else TextSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun IntroReelTargetPickerDialog(
  targetType: IntroReelTargetType,
  options: List<ReelTargetOption>,
  selectedId: String,
  onSelect: (ReelTargetOption) -> Unit,
  onDismiss: () -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  val filtered = remember(options, searchQuery) {
    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) {
      options
    } else {
      options.filter {
        it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q)
      }
    }
  }

  val dialogTitle = when (targetType) {
    IntroReelTargetType.CATEGORY -> "اختر القسم المطلوب"
    IntroReelTargetType.PRODUCT -> "اختر المنتج المطلوب"
    IntroReelTargetType.COMPANY -> "اختر الشركة المطلوبة"
    IntroReelTargetType.PROMO_ONLY -> ""
  }

  val emptyMessage = when (targetType) {
    IntroReelTargetType.CATEGORY -> "لا توجد أقسام مطابقة حاليًا."
    IntroReelTargetType.PRODUCT -> "لا توجد منتجات مطابقة حاليًا."
    IntroReelTargetType.COMPANY -> "لا توجد شركات أو عروض شركات مضافة بعد."
    IntroReelTargetType.PROMO_ONLY -> ""
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = dialogTitle,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("ابحث بالاسم...") },
          leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Filled.Close, contentDescription = "مسح البحث")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("intro_reel_target_search_input"),
        )

        if (filtered.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = GeneralBackground,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = emptyMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              modifier = Modifier.padding(16.dp),
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(filtered, key = { it.id }) { option ->
              val isSelected = option.id == selectedId
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) PrimaryYellowSoft else GeneralBackground,
                border = BorderStroke(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) PrimaryYellow else BorderNeutral,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onSelect(option) }
                  .testTag("intro_reel_target_option_${option.id}"),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (option.imageUrl.isNotBlank()) {
                    AsyncImage(
                      model = option.imageUrl,
                      contentDescription = option.title,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PureWhite),
                    )
                  } else {
                    Box(
                      modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PureWhite),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = targetTypeIcon(targetType),
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(20.dp),
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                    Text(
                      text = option.title,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = TextPrimary,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = option.subtitle,
                      style = MaterialTheme.typography.labelSmall,
                      color = TextSecondary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  if (isSelected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                      imageVector = Icons.Filled.CheckCircle,
                      contentDescription = "محدد",
                      tint = SuccessGreen,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("إغلاق", fontWeight = FontWeight.Bold, color = CharcoalBlack)
      }
    },
  )
}

private fun targetTypeIcon(type: IntroReelTargetType): ImageVector = when (type) {
  IntroReelTargetType.PROMO_ONLY -> Icons.Filled.Campaign
  IntroReelTargetType.CATEGORY -> Icons.Filled.Category
  IntroReelTargetType.PRODUCT -> Icons.Filled.Inventory2
  IntroReelTargetType.COMPANY -> Icons.Filled.Storefront
}
