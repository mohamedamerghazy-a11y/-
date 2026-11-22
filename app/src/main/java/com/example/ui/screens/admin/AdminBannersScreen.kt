package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewCarousel
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.media.CloudinaryFolder
import com.example.data.model.Banner
import com.example.data.model.BannerMediaType
import com.example.data.model.BannerPlacement
import com.example.data.model.BannerStyle
import com.example.data.model.BannerTargetType
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.AnimatedImagePickerUploadField
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
import com.example.ui.components.FullAspectBannerImage
import com.example.ui.components.ImagePickerUploadModal
import com.example.ui.components.InlineAnimatedPromoBanner
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
import com.example.ui.components.VideoPickerUploadField
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
import com.example.ui.viewmodel.AdminBannersUiState

@Composable
fun AdminBannersScreen(
  uiState: AdminBannersUiState,
  onSaveBanner: (Banner) -> Unit,
  onToggleBannerActive: (Banner) -> Unit,
  onDeleteBanner: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var editingBanner by remember { mutableStateOf<Banner?>(null) }
  var showDialog by remember { mutableStateOf(false) }
  var deletingBannerId by remember { mutableStateOf<String?>(null) }
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
        title = "إدارة البنرات الإعلانية",
        subtitle = "اختر موضع وشكل ومدة ظهور كل بنر",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingBanner = null
          showDialog = true
        },
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier.testTag("admin_add_banner_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "إضافة بنر إعلاني")
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

        uiState.banners.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.ViewCarousel,
              title = "لا توجد بنرات إعلانية حاليًا",
              description = "أضف بنرات عادية أو رفيعة في أي موضع بالصفحة الرئيسية مع تحديد مدة العرض.",
              actionLabel = "إضافة بنر جديد",
              onActionClick = {
                editingBanner = null
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
            items(uiState.banners, key = { it.id }) { banner ->
              BannerAdminCard(
                banner = banner,
                categories = uiState.categories,
                products = uiState.products,
                onEdit = {
                  editingBanner = banner
                  showDialog = true
                },
                onToggleActive = { onToggleBannerActive(banner) },
                onDelete = { deletingBannerId = banner.id },
              )
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    BannerFormDialog(
      existing = editingBanner,
      categories = uiState.categories,
      products = uiState.products,
      onDismiss = { showDialog = false },
      onConfirm = { banner ->
        onSaveBanner(banner)
        showDialog = false
      },
    )
  }

  if (deletingBannerId != null) {
    AlertDialog(
      onDismissRequest = { deletingBannerId = null },
      containerColor = PureWhite,
      title = { Text("تأكيد حذف البنر", fontWeight = FontWeight.Bold, color = DiscountRed) },
      text = { Text("هل أنت متأكد من حذف هذا البنر الإعلاني نهائيًا؟") },
      confirmButton = {
        Button(
          onClick = {
            deletingBannerId?.let { onDeleteBanner(it) }
            deletingBannerId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed),
        ) {
          Text("حذف", color = PureWhite)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingBannerId = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BannerAdminCard(
  banner: Banner,
  categories: List<Category>,
  products: List<Product>,
  onEdit: () -> Unit,
  onToggleActive: () -> Unit,
  onDelete: () -> Unit,
) {
  val targetSummary = when (banner.targetTypeEnum) {
    BannerTargetType.NONE -> "عرض إعلاني فقط"
    BannerTargetType.OFFERS -> "يوجه إلى العروض والخصومات"
    BannerTargetType.CATEGORY -> {
      val catName = categories.firstOrNull { it.id == banner.targetId }?.name ?: "قسم محدد"
      "يوجه إلى قسم: $catName"
    }
    BannerTargetType.PRODUCT -> {
      val prodName = products.firstOrNull { it.id == banner.targetId }?.name ?: "منتج محدد"
      "يوجه إلى منتج: $prodName"
    }
  }

  val isSlim = banner.bannerStyleEnum == BannerStyle.SLIM

  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(GeneralBackground),
      ) {
        val previewImageUrl = if (banner.isVideoBanner) banner.effectivePosterUrl else banner.imageUrl
        if (banner.isAnimatedImageBanner) {
          InlineAnimatedPromoBanner(
            imageUrl = banner.imageUrl,
            contentDescription = banner.title.ifBlank { "صورة متحركة" },
            isSlim = isSlim,
          )
        } else if (previewImageUrl.isNotBlank()) {
          FullAspectBannerImage(
            imageUrl = previewImageUrl,
            contentDescription = banner.title.ifBlank { if (banner.isVideoBanner) "فيديو دعائي" else "بنر إعلاني" },
            placeholderMinHeight = if (isSlim) 84.dp else 140.dp,
            errorContent = {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(if (isSlim) 84.dp else 140.dp)
                  .background(if (banner.isVideoBanner) CharcoalBlack else GeneralBackground),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = if (banner.isVideoBanner) Icons.Filled.PlayCircle else Icons.Filled.ViewCarousel,
                  contentDescription = null,
                  tint = if (banner.isVideoBanner) PrimaryYellow else TextSecondary,
                  modifier = Modifier.size(40.dp),
                )
              }
            },
          )
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(if (isSlim) 84.dp else 140.dp)
              .background(CharcoalBlack),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.PlayCircle,
              contentDescription = null,
              tint = PrimaryYellow,
              modifier = Modifier.size(44.dp),
            )
          }
        }
        if (banner.isVideoBanner || banner.isAnimatedImageBanner) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PrimaryYellow,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(10.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = if (banner.isVideoBanner) Icons.Filled.Videocam else Icons.Filled.Animation,
                contentDescription = null,
                tint = CharcoalBlack,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = if (banner.isVideoBanner) "فيديو دعائي" else "صورة متحركة",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = CharcoalBlack,
              )
            }
          }
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CharcoalBlack.copy(alpha = 0.85f),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp),
        ) {
          Text(
            text = "الترتيب: ${banner.displayOrder} • ${banner.safeDurationSeconds} ث",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PrimaryYellow,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = banner.title.ifBlank {
              when {
                banner.isVideoBanner -> "بنر فيديو دعائي قصير"
                banner.isAnimatedImageBanner -> "بنر صورة متحركة (GIF / WebP)"
                else -> "بنر بالصورة فقط (بدون نص)"
              }
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (banner.title.isBlank()) TextSecondary else CharcoalBlack,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(8.dp))
          if (banner.isActive) SuccessBadge("مفعّل") else DiscountBadge("معطّل")
        }

        if (!banner.subtitle.isNullOrBlank()) {
          Text(
            text = banner.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BrandPillBadge(
            text = banner.placementEnum.arabicLabel,
            containerColor = RoyalBlueSoft,
            contentColor = RoyalBlue,
          )
          BrandPillBadge(
            text = banner.bannerStyleEnum.arabicLabel,
            containerColor = GeneralBackground,
            contentColor = CharcoalBlack,
          )
          BrandPillBadge(
            text = targetSummary,
            containerColor = PrimaryYellowSoft,
            contentColor = TextPrimary,
          )
        }

        HorizontalDivider(color = BorderNeutral)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ظهور البنر", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Switch(
              checked = banner.isActive,
              onCheckedChange = { onToggleActive() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = CharcoalBlack,
                checkedTrackColor = PrimaryYellow,
              ),
            )
          }
          Row {
            IconButton(
              onClick = onEdit,
              modifier = Modifier.testTag("edit_banner_${banner.id}"),
            ) {
              Icon(Icons.Filled.Edit, contentDescription = "تعديل البنر", tint = CharcoalBlack)
            }
            IconButton(
              onClick = onDelete,
              modifier = Modifier.testTag("delete_banner_${banner.id}"),
            ) {
              Icon(Icons.Filled.Delete, contentDescription = "حذف البنر", tint = DiscountRed)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BannerFormDialog(
  existing: Banner?,
  categories: List<Category>,
  products: List<Product>,
  onDismiss: () -> Unit,
  onConfirm: (Banner) -> Unit,
) {
  var title by remember { mutableStateOf(existing?.title.orEmpty()) }
  var subtitle by remember { mutableStateOf(existing?.subtitle.orEmpty()) }
  var mediaType by remember { mutableStateOf(existing?.mediaTypeEnum ?: BannerMediaType.IMAGE) }
  var imageUrl by remember { mutableStateOf(existing?.imageUrl.orEmpty()) }
  var videoUrl by remember { mutableStateOf(existing?.videoUrl.orEmpty()) }
  var videoMuted by remember { mutableStateOf(existing?.videoMuted ?: false) }
  var targetType by remember { mutableStateOf(existing?.targetTypeEnum ?: BannerTargetType.NONE) }
  var targetId by remember { mutableStateOf(existing?.targetId.orEmpty()) }
  var placement by remember { mutableStateOf(existing?.placementEnum ?: BannerPlacement.TOP_HOME) }
  var bannerStyle by remember { mutableStateOf(existing?.bannerStyleEnum ?: BannerStyle.NORMAL) }
  var durationSeconds by remember { mutableStateOf((existing?.safeDurationSeconds ?: 5).toString()) }
  var displayOrder by remember { mutableStateOf((existing?.displayOrder ?: 0).toString()) }
  var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
  var showImageModal by remember { mutableStateOf(false) }
  var uploadError by remember { mutableStateOf<String?>(null) }
  var catMenuExpanded by remember { mutableStateOf(false) }
  var prodMenuExpanded by remember { mutableStateOf(false) }
  var placementExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (existing == null) "إضافة بنر إعلاني جديد" else "تعديل البنر الإعلاني",
        fontWeight = FontWeight.Bold,
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 540.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // نوع محتوى البنر: صورة أو فيديو دعائي قصير
        Text(
          text = "نوع البنر الإعلاني:",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BannerMediaType.entries.forEach { option ->
            val selected = mediaType == option
            FilterChip(
              selected = selected,
              onClick = { mediaType = option },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryYellow,
                selectedLabelColor = CharcoalBlack,
              ),
              leadingIcon = {
                val icon = when (option) {
                  BannerMediaType.VIDEO -> Icons.Filled.Videocam
                  BannerMediaType.ANIMATED_IMAGE -> Icons.Filled.Animation
                  BannerMediaType.IMAGE -> Icons.Filled.AddPhotoAlternate
                }
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              label = {
                Text(
                  text = option.arabicLabel,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
              },
              modifier = Modifier.testTag("banner_media_type_${option.code}"),
            )
          }
        }

        when (mediaType) {
          BannerMediaType.VIDEO -> {
            VideoPickerUploadField(
              videoUrl = videoUrl,
              detectedDurationSeconds = durationSeconds.toIntOrNull() ?: 0,
              buttonLabel = "رفع فيديو دعائي قصير من المعرض",
              urlFieldLabel = "رابط الفيديو الدعائي (MP4)",
              folder = CloudinaryFolder.VIDEOS,
              testTagPrefix = "banner_video",
              onVideoChanged = { nextUrl, detectedDur, poster ->
                videoUrl = nextUrl
                if (detectedDur != null && detectedDur > 0) {
                  durationSeconds = detectedDur.coerceIn(1, 120).toString()
                }
                if (!poster.isNullOrBlank() && imageUrl.isBlank()) {
                  imageUrl = poster
                }
              },
              onClearVideo = {
                videoUrl = ""
              },
            )

            // اختيار حالة الصوت للفيديو: بصوت أم كاتم بدون صوت
            Text(
              text = "صوت الفيديو:",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = !videoMuted,
                onClick = { videoMuted = false },
                label = { Text("بصوت", fontWeight = FontWeight.Bold) },
                leadingIcon = {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CharcoalBlack,
                  selectedLabelColor = PrimaryYellow,
                  selectedLeadingIconColor = PrimaryYellow,
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("banner_video_sound_on"),
              )
              FilterChip(
                selected = videoMuted,
                onClick = { videoMuted = true },
                label = { Text("كاتم (بدون صوت)", fontWeight = FontWeight.Bold) },
                leadingIcon = {
                  Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CharcoalBlack,
                  selectedLabelColor = PrimaryYellow,
                  selectedLeadingIconColor = PrimaryYellow,
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("banner_video_sound_muted"),
              )
            }
            if (videoMuted) {
              Text(
                text = "سيعمل الفيديو صامتًا بدون إظهار زر تفعيل الصوت أعلى الفيديو.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }

            OutlinedButton(
              onClick = { showImageModal = true },
              border = BorderStroke(1.dp, BorderNeutral),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = TextSecondary)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (imageUrl.isBlank()) "إضافة صورة غلاف مخصصة للفيديو (اختياري)" else "تغيير صورة غلاف الفيديو",
                color = CharcoalBlack,
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }

          BannerMediaType.ANIMATED_IMAGE -> {
            AnimatedImagePickerUploadField(
              imageUrl = imageUrl,
              isSlimPreview = bannerStyle == BannerStyle.SLIM,
              onImageUrlChanged = { imageUrl = it },
              onClearImage = { imageUrl = "" },
              folder = CloudinaryFolder.BANNERS,
              testTagPrefix = "banner_animated",
            )
          }

          BannerMediaType.IMAGE -> {
            if (imageUrl.isNotBlank()) {
              FullAspectBannerImage(
                imageUrl = imageUrl,
                contentDescription = "معاينة صورة البنر",
                placeholderMinHeight = if (bannerStyle == BannerStyle.SLIM) 76.dp else 130.dp,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)),
              )
            }

            OutlinedButton(
              onClick = { showImageModal = true },
              border = BorderStroke(1.dp, CharcoalBlack),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("banner_pick_image_button"),
            ) {
              Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (imageUrl.isBlank()) "رفع صورة البنر من المعرض أو الكاميرا" else "تغيير صورة البنر",
                color = CharcoalBlack,
                fontWeight = FontWeight.Bold,
              )
            }

            if (!uploadError.isNullOrBlank()) {
              Text(uploadError.orEmpty(), color = DiscountRed, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
              value = imageUrl,
              onValueChange = { imageUrl = it },
              label = { Text("رابط صورة البنر") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
            )
          }
        }

        // نوع البنر: عادي أم إعلاني رفيع
        Text(
          text = "حجم وشكل البنر:",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BannerStyle.entries.forEach { styleOption ->
            val selected = bannerStyle == styleOption
            FilterChip(
              selected = selected,
              onClick = { bannerStyle = styleOption },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryYellow,
                selectedLabelColor = CharcoalBlack,
              ),
              label = {
                Text(
                  text = styleOption.arabicLabel,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
              },
            )
          }
        }

        // موضع البنر في الصفحة الرئيسية
        ExposedDropdownMenuBox(
          expanded = placementExpanded,
          onExpandedChange = { placementExpanded = !placementExpanded },
        ) {
          OutlinedTextField(
            value = placement.arabicLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("موضع عرض البنر في الرئيسية") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = placementExpanded) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable),
          )
          ExposedDropdownMenu(
            expanded = placementExpanded,
            onDismissRequest = { placementExpanded = false },
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
          value = title,
          onValueChange = { title = it },
          label = { Text("عنوان البنر (اختياري - اتركه فارغًا لبنر صورة فقط)") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("banner_title_input"),
          singleLine = true,
        )

        OutlinedTextField(
          value = subtitle,
          onValueChange = { subtitle = it },
          label = { Text("وصف مختصر (اختياري)") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = durationSeconds,
            onValueChange = { durationSeconds = it.filter { ch -> ch.isDigit() } },
            label = { Text("مدة العرض (ثانية)") },
            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true,
          )
          OutlinedTextField(
            value = displayOrder,
            onValueChange = { displayOrder = it.filter { ch -> ch.isDigit() } },
            label = { Text("الترتيب") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true,
          )
        }

        Text(
          text = "الوجهة عند الضغط على البنر:",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BannerTargetType.entries.forEach { option ->
            val selected = targetType == option
            FilterChip(
              selected = selected,
              onClick = {
                targetType = option
                if (option == BannerTargetType.NONE || option == BannerTargetType.OFFERS) {
                  targetId = ""
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryYellow,
                selectedLabelColor = CharcoalBlack,
              ),
              label = { Text(option.arabicLabel, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
            )
          }
        }

        if (targetType == BannerTargetType.CATEGORY) {
          val selectedCatName = categories.firstOrNull { it.id == targetId }?.name ?: "اختر القسم المستهدف"
          ExposedDropdownMenuBox(
            expanded = catMenuExpanded,
            onExpandedChange = { catMenuExpanded = !catMenuExpanded },
          ) {
            OutlinedTextField(
              value = selectedCatName,
              onValueChange = {},
              readOnly = true,
              label = { Text("القسم المرتبط") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
              modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
              expanded = catMenuExpanded,
              onDismissRequest = { catMenuExpanded = false },
            ) {
              categories.forEach { category ->
                DropdownMenuItem(
                  text = { Text(category.name) },
                  onClick = {
                    targetId = category.id
                    catMenuExpanded = false
                  },
                )
              }
            }
          }
        } else if (targetType == BannerTargetType.PRODUCT) {
          val selectedProdName = products.firstOrNull { it.id == targetId }?.name ?: "اختر المنتج المستهدف"
          ExposedDropdownMenuBox(
            expanded = prodMenuExpanded,
            onExpandedChange = { prodMenuExpanded = !prodMenuExpanded },
          ) {
            OutlinedTextField(
              value = selectedProdName,
              onValueChange = {},
              readOnly = true,
              label = { Text("المنتج المرتبط") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodMenuExpanded) },
              modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
              expanded = prodMenuExpanded,
              onDismissRequest = { prodMenuExpanded = false },
            ) {
              products.take(40).forEach { prod ->
                DropdownMenuItem(
                  text = { Text(prod.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  onClick = {
                    targetId = prod.id
                    prodMenuExpanded = false
                  },
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
          Text("تفعيل البنر الآن", fontWeight = FontWeight.SemiBold)
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
            (existing ?: Banner()).copy(
              title = title.trim(),
              subtitle = subtitle.trim().ifEmpty { null },
              mediaType = mediaType.code,
              imageUrl = imageUrl.trim(),
              videoUrl = if (mediaType == BannerMediaType.VIDEO) videoUrl.trim() else "",
              videoMuted = mediaType == BannerMediaType.VIDEO && videoMuted,
              targetType = targetType.code,
              targetId = targetId.trim().ifEmpty { null },
              placement = placement.code,
              bannerStyle = bannerStyle.code,
              durationSeconds = (durationSeconds.toIntOrNull() ?: if (mediaType == BannerMediaType.VIDEO) 15 else 5).coerceIn(1, 120),
              displayOrder = displayOrder.toIntOrNull() ?: 0,
              isActive = isActive,
            ),
          )
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryYellow,
          contentColor = CharcoalBlack,
        ),
        modifier = Modifier.testTag("banner_save_confirm_button"),
      ) {
        Text("حفظ البنر", fontWeight = FontWeight.Bold)
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
    title = "رفع صورة البنر الإعلاني",
    folder = CloudinaryFolder.BANNERS,
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
