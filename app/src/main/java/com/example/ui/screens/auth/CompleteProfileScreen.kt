package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanPrimaryButton
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CompleteProfileScreen(
  initialName: String,
  email: String,
  isSubmitting: Boolean,
  errorMessage: String?,
  onSubmitProfile: (fullName: String, phone: String, requestWholesale: Boolean, companyName: String?) -> Unit,
  onSignOutClick: () -> Unit,
) {
  var fullName by rememberSaveable { mutableStateOf(initialName) }
  var phone by rememberSaveable { mutableStateOf("") }
  var requestWholesale by rememberSaveable { mutableStateOf(false) }
  var companyName by rememberSaveable { mutableStateOf("") }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      AlEmanTopBar(
        title = "استكمال بيانات الحساب",
        subtitle = "الإيمان للتجارة",
        trailingContent = {
          IconButton(
            onClick = onSignOutClick,
            modifier = Modifier.testTag("complete_profile_sign_out_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "تسجيل الخروج",
              tint = DiscountRed,
            )
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(AlEmanDimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        AlEmanCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Text(
              text = "البيانات الأساسية",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = RoyalBlue,
            )

            OutlinedTextField(
              value = email,
              onValueChange = {},
              readOnly = true,
              enabled = false,
              label = { Text("البريد الإلكتروني") },
              leadingIcon = {
                Icon(imageVector = Icons.Filled.Email, contentDescription = null, tint = TextSecondary)
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = GeneralBackground,
                disabledBorderColor = BorderNeutral,
                disabledTextColor = TextSecondary,
                disabledLabelColor = TextSecondary,
              ),
            )

            OutlinedTextField(
              value = fullName,
              onValueChange = { fullName = it },
              label = { Text("الاسم الكامل") },
              placeholder = { Text("مثال: محمد أحمد علي") },
              leadingIcon = {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = RoyalBlue)
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("complete_profile_name_input"),
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedBorderColor = RoyalBlue,
                unfocusedBorderColor = BorderNeutral,
              ),
            )

            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text("رقم الهاتف") },
              placeholder = { Text("مثال: 01001234567") },
              leadingIcon = {
                Icon(imageVector = Icons.Filled.Phone, contentDescription = null, tint = RoyalBlue)
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("complete_profile_phone_input"),
              shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedBorderColor = RoyalBlue,
                unfocusedBorderColor = BorderNeutral,
              ),
            )
          }
        }

        AlEmanCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "اختر نظام الشراء المطلوب",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = RoyalBlue,
            )

            AccountTypeOptionCard(
              title = "حساب قطاعي",
              subtitle = "مخصص للشراء الفردي والاحتياجات اليومية مع تفعيل فوري للحساب.",
              badgeText = "تفعيل فوري",
              icon = Icons.Filled.ShoppingBag,
              selected = !requestWholesale,
              onClick = { requestWholesale = false },
              modifier = Modifier.testTag("account_type_retail_option"),
            )

            AccountTypeOptionCard(
              title = "طلب حساب جملة (للتجار)",
              subtitle = "الحصول على أسعار الجملة المخصصة بعد مراجعة واعتماد الإدارة.",
              badgeText = "يتطلب موافقة الإدارة",
              icon = Icons.Filled.Storefront,
              selected = requestWholesale,
              onClick = { requestWholesale = true },
              modifier = Modifier.testTag("account_type_wholesale_option"),
            )

            if (requestWholesale) {
              OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("اسم المحل أو النشاط التجاري") },
                placeholder = { Text("مثال: شركة النور للتجارة") },
                leadingIcon = {
                  Icon(imageVector = Icons.Filled.Business, contentDescription = null, tint = RoyalBlue)
                },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("complete_profile_company_input"),
                shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = PureWhite,
                  unfocusedContainerColor = PureWhite,
                  focusedBorderColor = RoyalBlue,
                  unfocusedBorderColor = BorderNeutral,
                ),
              )

              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                color = GoldenYellowSoft,
                border = BorderStroke(1.dp, GoldenYellow),
              ) {
                Text(
                  text = "ملاحظة: ستظهر لك أسعار القطاعي مؤقتًا حتى تقوم الإدارة بمراجعة طلب الجملة واعتماده.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextPrimary,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }
          }
        }

        if (!errorMessage.isNullOrBlank()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
            color = DiscountRedSoft,
            border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.35f)),
          ) {
            Text(
              text = errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = DiscountRed,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        if (isSubmitting) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(AlEmanDimens.ButtonHeight),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = RoyalBlue)
          }
        } else {
          AlEmanPrimaryButton(
            text = "حفظ البيانات والمتابعة للمتجر",
            onClick = {
              onSubmitProfile(fullName, phone, requestWholesale, companyName.takeIf { requestWholesale })
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("complete_profile_submit_button"),
          )
        }
      }
    }
  }
}

@Composable
private fun AccountTypeOptionCard(
  title: String,
  subtitle: String,
  badgeText: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(AlEmanDimens.RadiusMd))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
    colors = CardDefaults.cardColors(
      containerColor = if (selected) RoyalBlueSoft.copy(alpha = 0.55f) else GeneralBackground,
    ),
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) RoyalBlue else BorderNeutral,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (selected) RoyalBlue else TextSecondary,
        modifier = Modifier.size(24.dp),
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
          BrandPillBadge(
            text = badgeText,
            containerColor = if (selected) GoldenYellow else PureWhite,
            contentColor = TextPrimary,
          )
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }
      if (selected) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = "محدد",
          tint = RoyalBlue,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}
