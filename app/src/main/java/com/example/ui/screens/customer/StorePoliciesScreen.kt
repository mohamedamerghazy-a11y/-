package com.example.ui.screens.customer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.StoreGeneralSettings
import com.example.data.model.StorePolicyTab
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorePoliciesScreen(
  settings: StoreGeneralSettings,
  initialTab: StorePolicyTab = StorePolicyTab.PRIVACY,
  isSignedIn: Boolean = false,
  onRequestDeleteAccount: (() -> Unit)? = null,
  onBackClick: () -> Unit,
) {
  BackHandler(onBack = onBackClick)

  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var activeTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
  val storeName = settings.storeName.ifBlank { stringResource(id = R.string.app_name) }

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

  val hasExternalLinks = settings.privacyPolicyUrl.isNotBlank() ||
    settings.returnPolicyUrl.isNotBlank() ||
    settings.accountDeletionUrl.isNotBlank()

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      AlEmanTopBar(
        title = "السياسات والخصوصية",
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
        .testTag("store_policies_screen"),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 760.dp)
          .fillMaxSize()
          .testTag("store_policies_list"),
        contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // 1. شريط اختيار القسم (الخصوصية / الاسترجاع / الشروط)
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            StorePolicyTab.entries.forEach { tab ->
              val selected = activeTab == tab
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { activeTab = tab }
                  .testTag("policy_tab_${tab.code}"),
                shape = RoundedCornerShape(12.dp),
                color = if (selected) CharcoalBlack else PureWhite,
                border = BorderStroke(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) CharcoalBlack else BorderNeutral,
                ),
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = tab.arabicLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    ),
                    color = if (selected) PrimaryYellow else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        // 2. شارات الطمأنينة والشفافية
        if (activeTab == StorePolicyTab.PRIVACY) {
          item {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              BrandPillBadge(
                text = "لا نبيع بياناتك",
                containerColor = SuccessGreenSoft,
                contentColor = SuccessGreen,
              )
              BrandPillBadge(
                text = "نقل مشفر HTTPS",
                containerColor = RoyalBlueSoft,
                contentColor = RoyalBlue,
              )
              BrandPillBadge(
                text = "حذف الحساب متاح فورًا",
                containerColor = PrimaryYellowSoft,
                contentColor = CharcoalBlack,
              )
            }
          }
        }

        // 3. بطاقة النص الكامل للسياسة المحددة
        item {
          val (sectionIcon, sectionTitle, sectionBody) = when (activeTab) {
            StorePolicyTab.PRIVACY -> Triple(
              Icons.Filled.PrivacyTip,
              "سياسة الخصوصية وحماية البيانات",
              settings.effectivePrivacyPolicyText,
            )
            StorePolicyTab.RETURN -> Triple(
              Icons.Filled.Replay,
              "سياسة الاسترجاع والاستبدال",
              settings.effectiveReturnPolicyText,
            )
            StorePolicyTab.TERMS -> Triple(
              Icons.Filled.Gavel,
              "الشروط والأحكام",
              settings.effectiveTermsOfServiceText,
            )
          }

          AlEmanCard(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("policy_content_card_${activeTab.code}"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  modifier = Modifier.size(40.dp),
                  shape = CircleShape,
                  color = PrimaryYellowSoft,
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = sectionIcon,
                      contentDescription = sectionTitle,
                      tint = CharcoalBlack,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = CharcoalBlack,
                  )
                  Text(
                    text = storeName,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                  )
                }
              }

              HorizontalDivider(color = BorderNeutral)

              Text(
                text = sectionBody,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                color = TextPrimary,
                modifier = Modifier.testTag("policy_body_text"),
              )
            }
          }
        }

        // 4. ملخص الأذونات والبيانات (في تبويب الخصوصية لوضوح مراجعة Google Play)
        if (activeTab == StorePolicyTab.PRIVACY) {
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
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = "الأذونات التي يستخدمها التطبيق ولماذا",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                }
                HorizontalDivider(color = BorderNeutral)
                PermissionUsageRow(
                  icon = Icons.Filled.CameraAlt,
                  title = "الكاميرا (عند الطلب فقط)",
                  description = "لمسح الباركود أو رمز QR للبحث السريع عن المنتجات.",
                )
                PermissionUsageRow(
                  icon = Icons.Filled.LocationOn,
                  title = "الموقع الجغرافي GPS (اختياري)",
                  description = "لتحديد إحداثيات عنوان التوصيل بدقة عند ضغطك على زر الموقع فقط.",
                )
                PermissionUsageRow(
                  icon = Icons.Filled.PhotoLibrary,
                  title = "منتقي الصور الآمن (بدون إذن تخزين)",
                  description = "لاختيار صورة إيصال التحويل أو صورة منتج واحدة دون الوصول لملفاتك.",
                )
                PermissionUsageRow(
                  icon = Icons.Filled.Bluetooth,
                  title = "البلوتوث (للطاقم فقط)",
                  description = "للاتصال بالطابعات الحرارية لطباعة إيصالات تجهيز الطلبات.",
                )
                PermissionUsageRow(
                  icon = Icons.Filled.Notifications,
                  title = "الإشعارات",
                  description = "لإبلاغك فورًا بتحديثات حالة الطلب ومراجعة الدفع وحساب الجملة.",
                )
              }
            }
          }

          // 5. بطاقة حقوق المستخدم وحذف الحساب (Google Play Data Deletion)
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              color = PureWhite,
              border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.35f)),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = DiscountRed,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = "حقوقك وحذف الحساب نهائيًا",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DiscountRed,
                  )
                }
                Text(
                  text = "يمكنك حذف حسابك وجميع بياناتك الشخصية (الملف الشخصي، العناوين، السلة، الإشعارات، والتقييمات) مباشرة من داخل التطبيق عبر تبويب «حسابي». تُحفظ فقط سجلات الفواتير المحاسبية اللازمة للالتزام القانوني وحل النزاعات.",
                  style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                  color = TextSecondary,
                )
                if (isSignedIn && onRequestDeleteAccount != null) {
                  OutlinedButton(
                    onClick = onRequestDeleteAccount,
                    border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                      containerColor = DiscountRedSoft.copy(alpha = 0.45f),
                      contentColor = DiscountRed,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .testTag("policies_delete_account_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DeleteForever,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بدء إجراء حذف الحساب نهائيًا", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        // 6. الروابط الرسمية على الويب (تظهر فقط عند إضافتها من الإدارة)
        if (hasExternalLinks) {
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
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = CharcoalBlack,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = "النسخ الإلكترونية المعتمدة على الويب",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalBlack,
                  )
                }
                HorizontalDivider(color = BorderNeutral)

                if (settings.privacyPolicyUrl.isNotBlank()) {
                  PolicyExternalUrlRow(
                    title = "رابط سياسة الخصوصية على الويب",
                    url = settings.privacyPolicyUrl,
                    testTag = "policy_external_privacy_url",
                    onClick = { openExternalUrl(settings.privacyPolicyUrl) },
                  )
                }
                if (settings.returnPolicyUrl.isNotBlank()) {
                  PolicyExternalUrlRow(
                    title = "رابط سياسة الاسترجاع على الويب",
                    url = settings.returnPolicyUrl,
                    testTag = "policy_external_return_url",
                    onClick = { openExternalUrl(settings.returnPolicyUrl) },
                  )
                }
                if (settings.accountDeletionUrl.isNotBlank()) {
                  PolicyExternalUrlRow(
                    title = "نموذج طلب حذف الحساب عبر الويب",
                    url = settings.accountDeletionUrl,
                    testTag = "policy_external_deletion_url",
                    onClick = { openExternalUrl(settings.accountDeletionUrl) },
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

@Composable
private fun PermissionUsageRow(
  icon: ImageVector,
  title: String,
  description: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Surface(
      modifier = Modifier.size(34.dp),
      shape = CircleShape,
      color = GeneralBackground,
      border = BorderStroke(1.dp, BorderNeutral),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = CharcoalBlack,
          modifier = Modifier.size(18.dp),
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
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun PolicyExternalUrlRow(
  title: String,
  url: String,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(10.dp),
    color = GeneralBackground,
    border = BorderStroke(1.dp, BorderNeutral),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = RoyalBlue,
        )
        Text(
          text = url.trim(),
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = "فتح الرابط",
        tint = RoyalBlue,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}
