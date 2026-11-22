package com.example.ui.screens.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.core.util.ArabicFormatter
import com.example.data.media.CloudinaryFolder
import com.example.data.media.CloudinaryUploadService
import com.example.data.model.AnnouncementTickerConfig
import com.example.data.model.BannerPlacement
import com.example.data.model.TickerFontWeight
import com.example.data.model.TickerInlineImage
import com.example.data.model.sanitizeCardHexColor
import com.example.ui.components.AnnouncementTickerBar
import com.example.ui.components.parseTickerHexColor
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val QUICK_TICKER_EMOJIS = listOf(
  "🔥", "⚡", "🎉", "💥", "🛍️", "🎁", "⭐", "🚚", "💰",
  "✨", "🏷️", "🌙", "🎈", "📢", "✅", "❤️", "💎", "🛒",
)

private val BG_COLOR_SWATCHES = listOf(
  "#181A20", "#FFD000", "#1565C0", "#059669", "#DC2626",
  "#5B21B6", "#FFFFFF", "#FFF9DB", "#881337", "#0F766E",
)

private val BORDER_COLOR_SWATCHES = listOf(
  "#FFD000", "#D4AF37", "#FFFFFF", "#181A20", "#EF4444",
  "#10B981", "#38BDF8", "#F59E0B", "#A855F7", "#E5E7EB",
)

private val TEXT_COLOR_SWATCHES = listOf(
  "#FFFFFF", "#FFD000", "#181A20", "#FEF3C7", "#E0F2FE",
  "#DC2626", "#15803D", "#D4AF37", "#1D4ED8", "#F9FAFB",
)

private data class TickerThemePreset(
  val name: String,
  val bg: String,
  val border: String,
  val text: String,
)

private val TICKER_THEME_PRESETS = listOf(
  TickerThemePreset("أسود وذهبي فاخر", "#181A20", "#FFD000", "#FFFFFF"),
  TickerThemePreset("أصفر الإيمان", "#FFD000", "#181A20", "#181A20"),
  TickerThemePreset("عروض نارية", "#DC2626", "#FFD000", "#FFFFFF"),
  TickerThemePreset("أزرق ملكي", "#1565C0", "#38BDF8", "#FFFFFF"),
  TickerThemePreset("زمردي أنيق", "#059669", "#FFD000", "#FFFFFF"),
)

private fun insertAtCursor(
  current: TextFieldValue,
  insertion: String,
): TextFieldValue {
  val text = current.text
  val start = current.selection.min.coerceIn(0, text.length)
  val end = current.selection.max.coerceIn(0, text.length)
  val updated = (text.substring(0, start) + insertion + text.substring(end)).take(1000)
  val newCursor = (start + insertion.length).coerceAtMost(updated.length)
  return TextFieldValue(
    text = updated,
    selection = TextRange(newCursor),
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnnouncementTickerAdminSection(
  enabled: Boolean,
  text: String,
  placement: BannerPlacement,
  backgroundColorHex: String,
  borderColorHex: String,
  textColorHex: String,
  fontSizeSp: Int,
  fontWeight: TickerFontWeight,
  isItalic: Boolean,
  inlineImages: List<TickerInlineImage>,
  isSubmitting: Boolean,
  onEnabledChange: (Boolean) -> Unit,
  onTextChange: (String) -> Unit,
  onPlacementChange: (BannerPlacement) -> Unit,
  onBackgroundColorChange: (String) -> Unit,
  onBorderColorChange: (String) -> Unit,
  onTextColorChange: (String) -> Unit,
  onFontSizeChange: (Int) -> Unit,
  onFontWeightChange: (TickerFontWeight) -> Unit,
  onItalicChange: (Boolean) -> Unit,
  onInlineImagesChange: (List<TickerInlineImage>) -> Unit,
  onSaveClick: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uploader = remember { CloudinaryUploadService() }
  var isUploadingThumb by remember { mutableStateOf(false) }
  var uploadError by remember { mutableStateOf<String?>(null) }

  var textFieldValue by remember {
    mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
  }
  LaunchedEffect(text) {
    if (text != textFieldValue.text) {
      textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length))
    }
  }

  val pickThumbLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      isUploadingThumb = true
      uploadError = null
      val result = uploader.processAndUploadUri(
        context = context,
        uri = uri,
        folder = CloudinaryFolder.BANNERS,
      )
      isUploadingThumb = false
      result.fold(
        onSuccess = { uploaded ->
          val existingIds = inlineImages.map { it.id }.toSet()
          var nextIdx = inlineImages.size + 1
          while ("img$nextIdx" in existingIds) nextIdx++
          val newId = "img$nextIdx"
          val nextList = (inlineImages + TickerInlineImage(id = newId, url = uploaded.secureUrl)).take(10)
          onInlineImagesChange(nextList)
          val updated = insertAtCursor(textFieldValue, " [[img:$newId]] ")
          textFieldValue = updated
          onTextChange(updated.text)
        },
        onFailure = { err ->
          uploadError = err.localizedMessage ?: "تعذر رفع الصورة المصغرة."
        },
      )
    }
  }

  val previewConfig = AnnouncementTickerConfig(
    enabled = true,
    text = text.ifBlank { "🔥 عروض وتخفيضات حصرية في الإيمان للتجارة — تسوق الآن واستمتع بأفضل الأسعار ⚡" },
    placement = placement,
    backgroundColorHex = backgroundColorHex,
    borderColorHex = borderColorHex,
    textColorHex = textColorHex,
    fontSizeSp = fontSizeSp,
    fontWeight = fontWeight,
    isItalic = isItalic,
    inlineImages = inlineImages,
  )

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Live animated preview
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
        Text(
          text = "معاينة حية للشريط الإعلاني (يتحرك من اليسار لليمين)",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = TextSecondary,
        )
        AnnouncementTickerBar(config = previewConfig)
      }
    }

    // Enable switch
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "تفعيل الشريط الإعلاني المتحرك",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
      Switch(
        checked = enabled,
        onCheckedChange = onEnabledChange,
        colors = SwitchDefaults.colors(
          checkedThumbColor = CharcoalBlack,
          checkedTrackColor = PrimaryYellow,
        ),
        modifier = Modifier.testTag("ticker_enabled_switch"),
      )
    }

    // Main text box with cursor tracking
    OutlinedTextField(
      value = textFieldValue,
      onValueChange = { next ->
        val clamped = if (next.text.length <= 1000) next else next.copy(text = next.text.take(1000))
        textFieldValue = clamped
        onTextChange(clamped.text)
      },
      label = { Text("نص الشريط الإعلاني (ضع المؤشر في أي مكان لإدراج رمز أو صورة)") },
      placeholder = { Text("مثال: 🔥 عروض اليوم الكبرى [[img:img1]] توصيل سريع لكل المحافظات 🚚") },
      minLines = 3,
      maxLines = 5,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("ticker_text_input"),
    )

    // Quick emojis bar (inserts at current cursor position)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "إدراج رموز سريعة في مكان المؤشر:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
      ) {
        items(QUICK_TICKER_EMOJIS) { emoji ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderNeutral),
            modifier = Modifier
              .size(40.dp)
              .clickable {
                val updated = insertAtCursor(textFieldValue, emoji)
                textFieldValue = updated
                onTextChange(updated.text)
              },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(text = emoji, style = MaterialTheme.typography.titleMedium)
            }
          }
        }
      }
    }

    // Inline mini-image uploader (inserts [[img:id]] right at cursor even mid-sentence!)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(
        onClick = {
          pickThumbLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
          )
        },
        enabled = !isUploadingThumb && inlineImages.size < 10,
        border = BorderStroke(1.dp, CharcoalBlack),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("ticker_add_inline_image_button"),
      ) {
        if (isUploadingThumb) {
          CircularProgressIndicator(
            color = CharcoalBlack,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("جاري رفع الصورة المصغرة...", color = CharcoalBlack, fontWeight = FontWeight.Bold)
        } else {
          Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CharcoalBlack)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "إضافة صورة مصغرة وسط الكلام (مكان المؤشر)",
            color = CharcoalBlack,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (!uploadError.isNullOrBlank()) {
        Text(
          text = uploadError.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = DiscountRed,
        )
      }

      if (inlineImages.isNotEmpty()) {
        Text(
          text = "الصور المصغرة المرفوعة (اضغط على الصورة لإدراجها مجددًا في مكان المؤشر):",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(inlineImages, key = { it.id }) { img ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = PureWhite,
              border = BorderStroke(1.dp, BorderNeutral),
            ) {
              Row(
                modifier = Modifier
                  .clickable {
                    val updated = insertAtCursor(textFieldValue, " [[img:${img.id}]] ")
                    textFieldValue = updated
                    onTextChange(updated.text)
                  }
                  .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                SubcomposeAsyncImage(
                  model = img.url,
                  contentDescription = img.id,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp)),
                )
                Text(
                  text = img.id,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = CharcoalBlack,
                )
                IconButton(
                  onClick = {
                    val remaining = inlineImages.filterNot { it.id == img.id }
                    onInlineImagesChange(remaining)
                    val cleanedText = textFieldValue.text.replace("[[img:${img.id}]]", "").replace("  ", " ")
                    textFieldValue = TextFieldValue(cleanedText, TextRange(cleanedText.length))
                    onTextChange(cleanedText)
                  },
                  modifier = Modifier.size(28.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "حذف الصورة المصغرة",
                    tint = DiscountRed,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    // Placement selector (anywhere on Home)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "موقع ظهور الشريط في الصفحة الرئيسية:",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        BannerPlacement.entries.forEach { pos ->
          val selected = placement == pos
          FilterChip(
            selected = selected,
            onClick = { onPlacementChange(pos) },
            label = {
              Text(
                text = pos.arabicLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                ),
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CharcoalBlack,
              selectedLabelColor = PrimaryYellow,
            ),
            modifier = Modifier.testTag("ticker_placement_${pos.code}"),
          )
        }
      }
    }

    // Ready-made color presets
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "تنسيقات ألوان جاهزة سريعة:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TICKER_THEME_PRESETS) { preset ->
          Surface(
            onClick = {
              onBackgroundColorChange(preset.bg)
              onBorderColorChange(preset.border)
              onTextColorChange(preset.text)
            },
            shape = RoundedCornerShape(50),
            color = parseTickerHexColor(preset.bg, CharcoalBlack),
            border = BorderStroke(1.6.dp, parseTickerHexColor(preset.border, PrimaryYellow)),
          ) {
            Text(
              text = preset.name,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = parseTickerHexColor(preset.text, PureWhite),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
          }
        }
      }
    }

    // 3 Color pickers
    TickerColorPickerRow(
      title = "لون أرضية الشريط",
      currentHex = backgroundColorHex,
      swatches = BG_COLOR_SWATCHES,
      onColorSelected = onBackgroundColorChange,
    )
    TickerColorPickerRow(
      title = "لون الشريط (الإطار والزينة)",
      currentHex = borderColorHex,
      swatches = BORDER_COLOR_SWATCHES,
      onColorSelected = onBorderColorChange,
    )
    TickerColorPickerRow(
      title = "لون الخط",
      currentHex = textColorHex,
      swatches = TEXT_COLOR_SWATCHES,
      onColorSelected = onTextColorChange,
    )

    // Typography controls: font size + weight + italic
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = GeneralBackground,
      border = BorderStroke(1.dp, BorderNeutral),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Font size stepper + quick sizes
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "حجم الخط: ${ArabicFormatter.formatNumber(fontSizeSp)}",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = PureWhite,
              border = BorderStroke(1.dp, BorderNeutral),
              modifier = Modifier
                .size(36.dp)
                .clickable { onFontSizeChange((fontSizeSp - 1).coerceIn(11, 24)) },
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Remove, contentDescription = "تصغير الخط", modifier = Modifier.size(18.dp))
              }
            }
            Surface(
              shape = CircleShape,
              color = PureWhite,
              border = BorderStroke(1.dp, BorderNeutral),
              modifier = Modifier
                .size(36.dp)
                .clickable { onFontSizeChange((fontSizeSp + 1).coerceIn(11, 24)) },
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, contentDescription = "تكبير الخط", modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(12 to "صغير", 14 to "متوسط", 17 to "كبير", 20 to "ضخم").forEach { (sp, label) ->
            FilterChip(
              selected = fontSizeSp == sp,
              onClick = { onFontSizeChange(sp) },
              label = { Text("$label ($sp)") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryYellow,
                selectedLabelColor = CharcoalBlack,
              ),
            )
          }
        }

        // Font weight selector
        Text(
          text = "سمك الخط:",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TickerFontWeight.entries.forEach { weightOption ->
            val selected = fontWeight == weightOption
            FilterChip(
              selected = selected,
              onClick = { onFontWeightChange(weightOption) },
              label = {
                Text(
                  text = weightOption.arabicLabel,
                  fontWeight = when (weightOption) {
                    TickerFontWeight.LIGHT -> FontWeight.Light
                    TickerFontWeight.NORMAL -> FontWeight.Normal
                    TickerFontWeight.BOLD -> FontWeight.Bold
                    TickerFontWeight.EXTRA_BOLD -> FontWeight.ExtraBold
                  },
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CharcoalBlack,
                selectedLabelColor = PrimaryYellow,
              ),
              modifier = Modifier.testTag("ticker_weight_${weightOption.code}"),
            )
          }
        }

        // Italic toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.FormatItalic, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
            Text(
              text = "خط مائل (Italic)",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
              ),
              color = CharcoalBlack,
            )
          }
          Switch(
            checked = isItalic,
            onCheckedChange = onItalicChange,
            colors = SwitchDefaults.colors(
              checkedThumbColor = CharcoalBlack,
              checkedTrackColor = PrimaryYellow,
            ),
            modifier = Modifier.testTag("ticker_italic_switch"),
          )
        }
      }
    }

    Button(
      onClick = onSaveClick,
      enabled = !isSubmitting,
      colors = ButtonDefaults.buttonColors(
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("admin_save_ticker_button"),
    ) {
      Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("حفظ إعدادات الشريط الإعلاني", fontWeight = FontWeight.ExtraBold)
    }
  }
}

@Composable
private fun TickerColorPickerRow(
  title: String,
  currentHex: String,
  swatches: List<String>,
  onColorSelected: (String) -> Unit,
) {
  var hexDraft by remember(currentHex) { mutableStateOf(currentHex) }

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(parseTickerHexColor(sanitizeCardHexColor(currentHex, "#181A20"), CharcoalBlack))
            .border(1.dp, BorderNeutral, CircleShape),
        )
        Text(
          text = sanitizeCardHexColor(currentHex, "#181A20"),
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(swatches) { hex ->
        val selected = sanitizeCardHexColor(currentHex, "") == hex
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(parseTickerHexColor(hex, CharcoalBlack))
            .border(
              width = if (selected) 3.dp else 1.dp,
              color = if (selected) PrimaryYellow else BorderNeutral,
              shape = CircleShape,
            )
            .clickable { onColorSelected(hex) },
        )
      }
    }

    OutlinedTextField(
      value = hexDraft,
      onValueChange = { next ->
        val clean = next.take(7)
        hexDraft = clean
        val normalized = if (clean.startsWith("#")) clean else "#$clean"
        if (Regex("^#[0-9A-Fa-f]{6}$").matches(normalized)) {
          onColorSelected(normalized.uppercase())
        }
      },
      label = { Text("كود لون مخصص (مثال: #181A20)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
