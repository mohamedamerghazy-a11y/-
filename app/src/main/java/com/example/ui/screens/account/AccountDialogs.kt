package com.example.ui.screens.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.model.WholesaleStatus
import com.example.data.model.UserProfile
import com.example.ui.components.AlEmanPrimaryButton
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.GoldenYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EditProfileDialog(
  profile: UserProfile,
  onDismiss: () -> Unit,
  onConfirm: (fullName: String, phone: String, companyName: String?) -> Unit,
) {
  var fullName by rememberSaveable { mutableStateOf(profile.fullName) }
  var phone by rememberSaveable { mutableStateOf(profile.phone) }
  var companyName by rememberSaveable { mutableStateOf(profile.wholesaleCompanyName.orEmpty()) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val showCompanyField = profile.wholesaleStatusEnum != WholesaleStatus.NONE ||
    !profile.wholesaleCompanyName.isNullOrBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "تعديل البيانات الشخصية",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = RoyalBlue,
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
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("الاسم الكامل") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_name_input"),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
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
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_phone_input"),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
            focusedBorderColor = RoyalBlue,
            unfocusedBorderColor = BorderNeutral,
          ),
        )
        if (showCompanyField) {
          OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("اسم المحل أو الشركة") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("edit_profile_company_input"),
            shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              focusedBorderColor = RoyalBlue,
              unfocusedBorderColor = BorderNeutral,
            ),
          )
        }
        if (!validationError.isNullOrBlank()) {
          Text(
            text = validationError.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = DiscountRed,
          )
        }
      }
    },
    confirmButton = {
      AlEmanPrimaryButton(
        text = "حفظ التعديلات",
        onClick = {
          if (fullName.trim().length < 2) {
            validationError = "يرجى إدخال الاسم الكامل بشكل صحيح."
          } else if (phone.trim().length < 8) {
            validationError = "يرجى إدخال رقم هاتف صحيح."
          } else {
            onConfirm(
              fullName.trim(),
              phone.trim(),
              companyName.trim().takeIf { showCompanyField && it.isNotEmpty() },
            )
          }
        },
        modifier = Modifier.testTag("save_profile_dialog_button"),
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}

@Composable
fun WholesaleRequestDialog(
  profile: UserProfile,
  onDismiss: () -> Unit,
  onConfirm: (fullName: String, phone: String, companyName: String) -> Unit,
) {
  var fullName by rememberSaveable { mutableStateOf(profile.fullName) }
  var phone by rememberSaveable { mutableStateOf(profile.phone) }
  var companyName by rememberSaveable { mutableStateOf(profile.wholesaleCompanyName.orEmpty()) }
  var validationError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "طلب اعتماد حساب جملة (B2B)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = RoyalBlue,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = GoldenYellowSoft,
          border = BorderStroke(1.dp, GoldenYellow),
        ) {
          Text(
            text = "بعد اعتماد الطلب من إدارة المتجر، ستظهر لك أسعار الجملة المخصصة تلقائيًا.",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.padding(10.dp),
          )
        }
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("اسم المسؤول / صاحب النشاط") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("wholesale_request_name_input"),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
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
          label = { Text("رقم الهاتف للتواصل") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("wholesale_request_phone_input"),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
            focusedBorderColor = RoyalBlue,
            unfocusedBorderColor = BorderNeutral,
          ),
        )
        OutlinedTextField(
          value = companyName,
          onValueChange = { companyName = it },
          label = { Text("اسم المحل أو الشركة") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("wholesale_request_company_input"),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
            focusedBorderColor = RoyalBlue,
            unfocusedBorderColor = BorderNeutral,
          ),
        )
        if (!validationError.isNullOrBlank()) {
          Text(
            text = validationError.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = DiscountRed,
          )
        }
      }
    },
    confirmButton = {
      AlEmanPrimaryButton(
        text = "إرسال الطلب",
        onClick = {
          if (fullName.trim().length < 2 || phone.trim().length < 8 || companyName.trim().length < 2) {
            validationError = "يرجى إدخال الاسم ورقم الهاتف واسم المحل أو الشركة بشكل صحيح."
          } else {
            onConfirm(fullName.trim(), phone.trim(), companyName.trim())
          }
        },
        modifier = Modifier.testTag("submit_wholesale_dialog_button"),
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}

@Composable
fun SignOutConfirmDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = "تسجيل الخروج",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
      )
    },
    text = {
      Text(
        text = "هل تريد تسجيل الخروج من حسابك في الإيمان للتجارة؟",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        modifier = Modifier.testTag("confirm_sign_out_button"),
      ) {
        Text("تسجيل الخروج", color = DiscountRed, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}

@Composable
fun DeleteAccountConfirmDialog(
  userEmail: String,
  requiresRecentLogin: Boolean,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit,
) {
  var acknowledged by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    icon = {
      Icon(
        imageVector = Icons.Filled.DeleteForever,
        contentDescription = "حذف الحساب",
        tint = DiscountRed,
        modifier = Modifier.size(32.dp),
      )
    },
    title = {
      Text(
        text = "حذف الحساب نهائيًا",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = DiscountRed,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = DiscountRedSoft,
          border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.35f)),
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "سيتم مسح البيانات التالية نهائيًا لحساب ($userEmail):",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = DiscountRed,
            )
            Text(
              text = "• الملف الشخصي (الاسم، الهاتف، واسم النشاط التجاري)\n• جميع عناوين التوصيل المحفوظة وإحداثيات الخريطة\n• عربة التسوق، الإشعارات، وتقييمات المنتجات\n• بيانات تسجيل الدخول المرتبطة بهذا الحساب",
              style = MaterialTheme.typography.bodySmall,
              color = TextPrimary,
            )
          }
        }

        Text(
          text = "ملاحظة: قد يحتفظ المتجر بسجلات الفواتير والطلبات السابقة لأغراض المحاسبة والضرائب وتسوية أي طلبات قائمة فقط.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )

        if (requiresRecentLogin) {
          Text(
            text = "لدواعي الأمان، سيُطلب منك تأكيد هويتك سريعًا عبر حساب Google قبل إتمام الحذف.",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = RoyalBlue,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = acknowledged,
            onCheckedChange = { acknowledged = it },
            modifier = Modifier.testTag("delete_account_ack_checkbox"),
            colors = CheckboxDefaults.colors(
              checkedColor = DiscountRed,
              uncheckedColor = TextSecondary,
            ),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "أؤكد رغبتي في حذف حسابي نهائيًا وأعلم أنه لا يمكن التراجع عن هذا الإجراء.",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmDelete,
        enabled = acknowledged,
        colors = ButtonDefaults.buttonColors(
          containerColor = DiscountRed,
          contentColor = PureWhite,
        ),
        modifier = Modifier.testTag("confirm_delete_account_button"),
      ) {
        Text(
          text = if (requiresRecentLogin) "تأكيد الهوية وحذف الحساب" else "حذف نهائي",
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = TextSecondary)
      }
    },
  )
}
