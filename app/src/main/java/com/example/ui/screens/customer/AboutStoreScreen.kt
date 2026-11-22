package com.example.ui.screens.customer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StoreContactChannel
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.StorePhoneEntry
import com.example.data.model.StoreSocialLink
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellow
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

@Composable
fun AboutStoreScreen(
  settings: StoreGeneralSettings,
  onOpenPolicies: (() -> Unit)? = null,
  onBackClick: () -> Unit,
) {
  BackHandler(onBack = onBackClick)

  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val storeName = settings.storeName.ifBlank { stringResource(id = R.string.app_name) }
  val storeTagline = settings.tagline.ifBlank { stringResource(id = R.string.brand_tagline) }
  val aboutText = settings.aboutStoreDescription.ifBlank {
    "متجر متخصص في توفير المنظفات ومستلزمات النظافة المنزلية عالية الجودة بأسعار القطاعي والجملة مع توصيل سريع وخدمة عملاء متكاملة."
  }

  val retailPhones = buildList {
    addAll(settings.retailPhones.filter { it.phoneNumber.isNotBlank() })
    val fallbackPhone = settings.supportPhone.trim()
    if (fallbackPhone.isNotEmpty() && none { it.phoneNumber.trim() == fallbackPhone }) {
      add(StorePhoneEntry(label = "خدمة عملاء القطاعي", phoneNumber = fallbackPhone, channel = StoreContactChannel.RETAIL.code))
    }
  }
  val wholesalePhones = settings.wholesalePhones.filter { it.phoneNumber.isNotBlank() }

  val retailSocials = settings.socialPages.filter {
    it.url.isNotBlank() && (it.channelEnum == StoreContactChannel.RETAIL || it.channelEnum == StoreContactChannel.BOTH)
  }
  val wholesaleSocials = settings.socialPages.filter {
    it.url.isNotBlank() && (it.channelEnum == StoreContactChannel.WHOLESALE || it.channelEnum == StoreContactChannel.BOTH)
  }

  val hasRetailSection = settings.retailFacebookUrl.isNotBlank() || retailPhones.isNotEmpty() || retailSocials.isNotEmpty()
  val hasWholesaleSection = settings.wholesaleFacebookUrl.isNotBlank() || wholesalePhones.isNotEmpty() || wholesaleSocials.isNotEmpty()

  fun openExternalUrl(rawUrl: String) {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return
    val normalized = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
      trimmed
    } else {
      "https://$trimmed"
    }
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
    } catch (_: Exception) {
      clipboardManager.setText(AnnotatedString(normalized))
      Toast.makeText(context, "تم نسخ الرابط: $normalized", Toast.LENGTH_SHORT).show()
    }
  }

  fun dialPhone(phone: String) {
    val clean = phone.trim()
    if (clean.isEmpty()) return
    try {
      context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
    } catch (_: Exception) {
      clipboardManager.setText(AnnotatedString(clean))
      Toast.makeText(context, "تم نسخ الرقم: $clean", Toast.LENGTH_SHORT).show()
    }
  }

  fun copyPhone(phone: String) {
    val clean = phone.trim()
    if (clean.isEmpty()) return
    clipboardManager.setText(AnnotatedString(clean))
    Toast.makeText(context, "تم نسخ الرقم: $clean", Toast.LENGTH_SHORT).show()
  }

  fun openWhatsApp(number: String) {
    val digits = number.filter { it.isDigit() || it == '+' }.trimStart('+')
    if (digits.isEmpty()) return
    openExternalUrl("https://wa.me/$digits")
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      AlEmanTopBar(
        title = "عن المتجر",
        subtitle = storeName,
        showBack = true,
        onBackClick = onBackClick,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("about_store_screen"),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 720.dp)
          .fillMaxSize(),
        contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. بطاقة تعريف المتجر والشركة
        item {
          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Surface(
                  modifier = Modifier.size(64.dp),
                  shape = RoundedCornerShape(16.dp),
                  color = PureWhite,
                  border = BorderStroke(1.5.dp, PrimaryYellow),
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = storeName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .fillMaxSize()
                      .clip(RoundedCornerShape(14.dp)),
                  )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = storeName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary,
                  )
                  BrandPillBadge(
                    text = storeTagline,
                    containerColor = PrimaryYellowSoft,
                    contentColor = CharcoalBlack,
                  )
                }
              }

              HorizontalDivider(color = BorderNeutral)

              Text(
                text = aboutText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )

              if (settings.storeAddress.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = GeneralBackground,
                  border = BorderStroke(1.dp, BorderNeutral),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.LocationOn,
                      contentDescription = null,
                      tint = CharcoalBlack,
                      modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "عنوان المقر الرئيسي",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                      )
                      Text(
                        text = settings.storeAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                      )
                    }
                  }
                }
              }

              if (settings.whatsappNumber.isNotBlank()) {
                Surface(
                  onClick = { openWhatsApp(settings.whatsappNumber) },
                  shape = RoundedCornerShape(12.dp),
                  color = SuccessGreenSoft,
                  border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Chat,
                      contentDescription = null,
                      tint = SuccessGreen,
                      modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "تواصل فوري عبر واتساب",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SuccessGreen,
                      )
                      Text(
                        text = settings.whatsappNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                      )
                    }
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                      contentDescription = null,
                      tint = SuccessGreen,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
          }
        }

        // 2. قسم مبيعات وتواصل القطاعي
        if (hasRetailSection) {
          item {
            ChannelContactSectionCard(
              title = "تواصل مبيعات القطاعي",
              badgeText = "القطاعي (التجزئة)",
              badgeColor = RoyalBlueSoft,
              badgeTextColor = RoyalBlue,
              facebookUrl = settings.retailFacebookUrl,
              facebookLabel = "صفحة فيسبوك للقطاعي",
              phones = retailPhones,
              socialPages = retailSocials,
              onOpenUrl = ::openExternalUrl,
              onDialPhone = ::dialPhone,
              onCopyPhone = ::copyPhone,
            )
          }
        }

        // 3. قسم مبيعات وتواصل الجملة
        if (hasWholesaleSection) {
          item {
            ChannelContactSectionCard(
              title = "تواصل مبيعات الجملة",
              badgeText = "الجملة (B2B)",
              badgeColor = GoldenYellow,
              badgeTextColor = CharcoalBlack,
              facebookUrl = settings.wholesaleFacebookUrl,
              facebookLabel = "صفحة فيسبوك للجملة",
              phones = wholesalePhones,
              socialPages = wholesaleSocials,
              onOpenUrl = ::openExternalUrl,
              onDialPhone = ::dialPhone,
              onCopyPhone = ::copyPhone,
            )
          }
        }

        if (!hasRetailSection && !hasWholesaleSection && settings.whatsappNumber.isBlank() && settings.storeAddress.isBlank()) {
          item {
            AlEmanCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Storefront,
                  contentDescription = null,
                  tint = TextSecondary,
                )
                Text(
                  text = "سيتم عرض أرقام الهواتف وصفحات التواصل الرسمية للشركة هنا فور إضافتها من إعدادات المتجر.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              }
            }
          }
        }

        if (onOpenPolicies != null) {
          item {
            AlEmanCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(onClick = onOpenPolicies)
                  .padding(16.dp)
                  .testTag("about_open_policies_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Surface(
                  modifier = Modifier.size(40.dp),
                  shape = CircleShape,
                  color = RoyalBlueSoft,
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Filled.PrivacyTip,
                      contentDescription = "السياسات والخصوصية",
                      tint = RoyalBlue,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "سياسة الخصوصية والاسترجاع والشروط",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                  Text(
                    text = "اطّلع على سياسة حماية البيانات وشروط الاستبدال والاسترجاع",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                  )
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDescription = null,
                  tint = RoyalBlue,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChannelContactSectionCard(
  title: String,
  badgeText: String,
  badgeColor: Color,
  badgeTextColor: Color,
  facebookUrl: String,
  facebookLabel: String,
  phones: List<StorePhoneEntry>,
  socialPages: List<StoreSocialLink>,
  onOpenUrl: (String) -> Unit,
  onDialPhone: (String) -> Unit,
  onCopyPhone: (String) -> Unit,
) {
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
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        BrandPillBadge(
          text = badgeText,
          containerColor = badgeColor,
          contentColor = badgeTextColor,
        )
      }

      HorizontalDivider(color = BorderNeutral)

      if (facebookUrl.isNotBlank()) {
        ContactLinkItemRow(
          icon = Icons.Filled.Public,
          title = facebookLabel,
          subtitle = facebookUrl.trim(),
          accentColor = Color(0xFF1877F2),
          onClick = { onOpenUrl(facebookUrl) },
        )
      }

      socialPages.forEach { page ->
        ContactLinkItemRow(
          icon = Icons.Filled.Language,
          title = page.title.ifBlank { "صفحة تواصل" },
          subtitle = page.url.trim(),
          accentColor = CharcoalBlack,
          onClick = { onOpenUrl(page.url) },
        )
      }

      if (phones.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          phones.forEachIndexed { idx, entry ->
            ContactPhoneItemRow(
              label = entry.label.ifBlank { "هاتف التواصل ${idx + 1}" },
              phoneNumber = entry.phoneNumber.trim(),
              onDial = { onDialPhone(entry.phoneNumber) },
              onCopy = { onCopyPhone(entry.phoneNumber) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ContactLinkItemRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accentColor: Color,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = GeneralBackground,
    border = BorderStroke(1.dp, BorderNeutral),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.12f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = "فتح الرابط",
        tint = CharcoalBlack,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun ContactPhoneItemRow(
  label: String,
  phoneNumber: String,
  onDial: () -> Unit,
  onCopy: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = GeneralBackground,
    border = BorderStroke(1.dp, BorderNeutral),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onDial),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = PrimaryYellowSoft,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = label,
            tint = CharcoalBlack,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
        Text(
          text = phoneNumber,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
      }
      IconButton(onClick = onCopy) {
        Icon(
          imageVector = Icons.Filled.ContentCopy,
          contentDescription = "نسخ الرقم",
          tint = TextSecondary,
          modifier = Modifier.size(18.dp),
        )
      }
      Surface(
        onClick = onDial,
        shape = CircleShape,
        color = PrimaryYellow,
        modifier = Modifier.size(38.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = "اتصال",
            tint = CharcoalBlack,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
