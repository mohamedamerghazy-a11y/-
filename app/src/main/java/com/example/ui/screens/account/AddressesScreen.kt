package com.example.ui.screens.account

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.location.LocationHelper
import com.example.data.model.DeliveryAddress
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanPrimaryButton
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.components.SuccessBadge
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
import kotlinx.coroutines.launch

@Composable
fun AddressesScreen(
  addresses: List<DeliveryAddress>,
  defaultRecipientName: String,
  defaultPhone: String,
  isSubmitting: Boolean,
  feedbackMessage: String?,
  errorMessage: String?,
  onClearMessages: () -> Unit,
  onSaveAddress: (DeliveryAddress) -> Unit,
  onSetDefault: (DeliveryAddress) -> Unit,
  onDeleteAddress: (String) -> Unit,
  onBackClick: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var editingAddress by remember { mutableStateOf<DeliveryAddress?>(null) }
  var showFormDialog by remember { mutableStateOf(false) }
  var pendingDeleteAddress by remember { mutableStateOf<DeliveryAddress?>(null) }

  LaunchedEffect(feedbackMessage, errorMessage) {
    val msg = feedbackMessage ?: errorMessage
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
        title = "عناوين التوصيل",
        subtitle = "إدارة مواقع الاستلام المحفوظة",
        showBack = true,
        onBackClick = onBackClick,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingAddress = null
          showFormDialog = true
        },
        containerColor = RoyalBlue,
        contentColor = PureWhite,
        modifier = Modifier.testTag("add_address_fab"),
      ) {
        Icon(
          imageVector = Icons.Filled.AddLocationAlt,
          contentDescription = "إضافة عنوان توصيل جديد",
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (addresses.isEmpty()) {
        Column(
          modifier = Modifier
            .widthIn(max = 600.dp)
            .padding(AlEmanDimens.SpaceLg),
        ) {
          StoreEmptyStateCard(
            icon = Icons.Filled.LocationOn,
            title = "لا توجد عناوين توصيل محفوظة",
            description = "أضف عنوان المنزل أو المحل أو المخزن لتسهيل إتمام الطلبات القادمة.",
            actionLabel = "إضافة عنوان جديد",
            onActionClick = {
              editingAddress = null
              showFormDialog = true
            },
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .widthIn(max = 700.dp)
            .fillMaxSize(),
          contentPadding = PaddingValues(
            start = AlEmanDimens.SpaceLg,
            end = AlEmanDimens.SpaceLg,
            top = AlEmanDimens.SpaceLg,
            bottom = 88.dp,
          ),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(addresses, key = { it.id }) { address ->
            AddressCard(
              address = address,
              enabled = !isSubmitting,
              onEdit = {
                editingAddress = address
                showFormDialog = true
              },
              onSetDefault = { onSetDefault(address) },
              onDelete = { pendingDeleteAddress = address },
            )
          }
        }
      }
    }
  }

  if (showFormDialog) {
    AddressFormDialog(
      initial = editingAddress,
      defaultRecipientName = defaultRecipientName,
      defaultPhone = defaultPhone,
      isFirstAddress = addresses.isEmpty(),
      onDismiss = { showFormDialog = false },
      onConfirm = { saved ->
        showFormDialog = false
        onSaveAddress(saved)
      },
    )
  }

  val deleteTarget = pendingDeleteAddress
  if (deleteTarget != null) {
    AlertDialog(
      onDismissRequest = { pendingDeleteAddress = null },
      containerColor = PureWhite,
      title = {
        Text(
          text = "حذف عنوان التوصيل",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
      },
      text = {
        Text(
          text = "هل تريد بالتأكيد حذف عنوان «${deleteTarget.label}»؟",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            pendingDeleteAddress = null
            onDeleteAddress(deleteTarget.id)
          },
          modifier = Modifier.testTag("confirm_delete_address_button"),
        ) {
          Text("حذف", color = DiscountRed, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteAddress = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@Composable
private fun AddressCard(
  address: DeliveryAddress,
  enabled: Boolean,
  onEdit: () -> Unit,
  onSetDefault: () -> Unit,
  onDelete: () -> Unit,
) {
  val context = LocalContext.current
  val formattedCoords = LocationHelper.formatCoordinates(address.latitude, address.longitude)

  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = RoyalBlue,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = address.label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
          )
          if (address.isDefault) {
            SuccessBadge(text = "الافتراضي")
          }
          if (address.hasCoordinates) {
            BrandPillBadge(
              text = "GPS",
              containerColor = PrimaryYellowSoft,
              contentColor = CharcoalBlack,
            )
          }
        }
        Row {
          IconButton(
            onClick = onEdit,
            enabled = enabled,
            modifier = Modifier.testTag("edit_address_${address.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "تعديل العنوان",
              tint = RoyalBlue,
            )
          }
          IconButton(
            onClick = onDelete,
            enabled = enabled,
            modifier = Modifier.testTag("delete_address_${address.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = "حذف العنوان",
              tint = DiscountRed,
            )
          }
        }
      }

      Text(
        text = address.recipientName,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary,
      )

      Text(
        text = "${address.governorate} - ${address.city}، ${address.streetAddress}",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )

      if (formattedCoords != null) {
        Text(
          text = "إحداثيات الخريطة: $formattedCoords",
          style = MaterialTheme.typography.labelSmall,
          color = RoyalBlue,
        )
      }

      if (!address.landmark.isNullOrBlank()) {
        Text(
          text = "علامة مميزة: ${address.landmark}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Phone,
          contentDescription = null,
          tint = TextSecondary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = address.phone,
          style = MaterialTheme.typography.bodySmall,
          color = TextPrimary,
        )
      }

      if (address.hasCoordinates || address.streetAddress.isNotBlank()) {
        OutlinedButton(
          onClick = {
            LocationHelper.openLocationInMaps(
              context = context,
              latitude = address.latitude,
              longitude = address.longitude,
              fallbackAddress = "${address.governorate} - ${address.city} - ${address.streetAddress}",
              label = address.label.ifBlank { address.recipientName },
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("view_address_map_${address.id}"),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          border = BorderStroke(1.dp, CharcoalBlack.copy(alpha = 0.35f)),
        ) {
          Icon(
            imageVector = Icons.Filled.Map,
            contentDescription = null,
            tint = CharcoalBlack,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "عرض الموقع على الخريطة",
            color = CharcoalBlack,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      if (!address.isDefault) {
        HorizontalDivider(color = BorderNeutral, modifier = Modifier.padding(top = 4.dp))
        OutlinedButton(
          onClick = onSetDefault,
          enabled = enabled,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("set_default_address_${address.id}"),
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.4f)),
        ) {
          Text(
            text = "تعيين كعنوان افتراضي",
            color = RoyalBlue,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddressFormDialog(
  initial: DeliveryAddress?,
  defaultRecipientName: String,
  defaultPhone: String,
  isFirstAddress: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (DeliveryAddress) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val labelPresets = listOf("المنزل", "المحل", "المخزن", "الشركة")
  var label by rememberSaveable { mutableStateOf(initial?.label ?: "المنزل") }
  var recipientName by rememberSaveable {
    mutableStateOf(initial?.recipientName ?: defaultRecipientName)
  }
  var phone by rememberSaveable { mutableStateOf(initial?.phone ?: defaultPhone) }
  var governorate by rememberSaveable { mutableStateOf(initial?.governorate ?: "") }
  var city by rememberSaveable { mutableStateOf(initial?.city ?: "") }
  var streetAddress by rememberSaveable { mutableStateOf(initial?.streetAddress ?: "") }
  var landmark by rememberSaveable { mutableStateOf(initial?.landmark ?: "") }
  var latitude by rememberSaveable { mutableStateOf(initial?.latitude) }
  var longitude by rememberSaveable { mutableStateOf(initial?.longitude) }
  var isDefault by rememberSaveable {
    mutableStateOf(initial?.isDefault ?: isFirstAddress)
  }
  var isCapturingLocation by remember { mutableStateOf(false) }
  var gpsStatusMessage by remember { mutableStateOf<String?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }

  fun startGpsCapture() {
    isCapturingLocation = true
    gpsStatusMessage = null
    validationError = null
    scope.launch {
      val result = LocationHelper.captureCurrentLocation(context)
      isCapturingLocation = false
      result.fold(
        onSuccess = { captured ->
          latitude = captured.latitude
          longitude = captured.longitude
          val formatted = LocationHelper.formatCoordinates(captured.latitude, captured.longitude).orEmpty()
          if (governorate.isBlank()) {
            governorate = captured.governorate?.takeIf { it.length >= 2 } ?: "GPS"
          }
          if (city.isBlank()) {
            city = captured.city?.takeIf { it.length >= 2 } ?: "موقع الخريطة"
          }
          if (streetAddress.isBlank()) {
            streetAddress = captured.streetAddress?.takeIf { it.length >= 4 }
              ?: "إحداثيات الموقع: $formatted"
          }
          gpsStatusMessage = "تم التقاط موقعك بنجاح ($formatted)"
        },
        onFailure = { err ->
          validationError = err.message ?: "تعذر التقاط إحداثيات الموقع حاليًا."
        },
      )
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { grants ->
    val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      startGpsCapture()
    } else {
      validationError = "يرجى السماح بصلاحية الموقع للوصول إلى إحداثيات GPS، أو إدخال العنوان يدويًا."
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (initial == null) "إضافة عنوان توصيل" else "تعديل عنوان التوصيل",
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
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          labelPresets.forEach { preset ->
            val selected = label == preset
            FilterChip(
              selected = selected,
              onClick = { label = preset },
              label = { Text(preset) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RoyalBlue,
                selectedLabelColor = PureWhite,
                containerColor = GeneralBackground,
                labelColor = TextPrimary,
              ),
            )
          }
        }

        // التقاط الموقع عبر GPS بدل الإدخال اليدوي الإلزامي
        Surface(
          shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
          color = RoyalBlueSoft,
          border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = {
                if (LocationHelper.hasLocationPermission(context)) {
                  startGpsCapture()
                } else {
                  locationPermissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                  )
                }
              },
              enabled = !isCapturingLocation,
              shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PrimaryYellow,
                contentColor = CharcoalBlack,
              ),
              border = BorderStroke(1.dp, CharcoalBlack),
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("capture_gps_location_button"),
            ) {
              if (isCapturingLocation) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  color = CharcoalBlack,
                  strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("جاري تحديد موقعك...", fontWeight = FontWeight.Bold, color = CharcoalBlack)
              } else {
                Icon(
                  imageVector = Icons.Filled.MyLocation,
                  contentDescription = null,
                  tint = CharcoalBlack,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (latitude != null && longitude != null) {
                    "تحديث الإحداثيات الحالية (GPS)"
                  } else {
                    "التقاط الموقع الحالي تلقائيًا (GPS)"
                  },
                  fontWeight = FontWeight.Bold,
                  color = CharcoalBlack,
                )
              }
            }

            val formattedCoords = LocationHelper.formatCoordinates(latitude, longitude)
            if (formattedCoords != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = gpsStatusMessage ?: "إحداثيات محفوظة: $formattedCoords",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = RoyalBlue,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = {
                    LocationHelper.openLocationInMaps(
                      context = context,
                      latitude = latitude,
                      longitude = longitude,
                      label = label,
                    )
                  },
                ) {
                  Icon(Icons.Filled.Map, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("عرض على الخريطة", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
              }
            } else {
              Text(
                text = "يمكنك التقاط موقعك مباشرة عبر GPS دون الحاجة لكتابة العنوان يدويًا.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }
          }
        }

        AddressTextField(
          value = label,
          onValueChange = { label = it },
          label = "تسمية العنوان (مثال: المحل، المخزن)",
          testTag = "address_label_input",
        )
        AddressTextField(
          value = recipientName,
          onValueChange = { recipientName = it },
          label = "اسم المستلم",
          testTag = "address_recipient_input",
        )
        AddressTextField(
          value = phone,
          onValueChange = { phone = it },
          label = "رقم هاتف المستلم",
          keyboardType = KeyboardType.Phone,
          testTag = "address_phone_input",
        )
        val hasGps = latitude != null && longitude != null
        AddressTextField(
          value = governorate,
          onValueChange = { governorate = it },
          label = if (hasGps) "المحافظة (اختياري مع GPS)" else "المحافظة",
          testTag = "address_governorate_input",
        )
        AddressTextField(
          value = city,
          onValueChange = { city = it },
          label = if (hasGps) "المدينة / المركز (اختياري مع GPS)" else "المدينة / المركز",
          testTag = "address_city_input",
        )
        AddressTextField(
          value = streetAddress,
          onValueChange = { streetAddress = it },
          label = if (hasGps) "وصف الشارع (اختياري مع GPS)" else "اسم الشارع ورقم العقار",
          testTag = "address_street_input",
        )
        AddressTextField(
          value = landmark,
          onValueChange = { landmark = it },
          label = "علامة مميزة (اختياري)",
          testTag = "address_landmark_input",
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "تعيين كعنوان افتراضي",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
          )
          Switch(
            checked = isDefault,
            onCheckedChange = { isDefault = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = PureWhite,
              checkedTrackColor = RoyalBlue,
            ),
            modifier = Modifier.testTag("address_default_switch"),
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
        text = "حفظ العنوان",
        onClick = {
          val hasCoords = latitude != null && longitude != null
          val coordsFormatted = LocationHelper.formatCoordinates(latitude, longitude).orEmpty()
          val resolvedGov = governorate.trim().ifBlank { if (hasCoords) "GPS" else "" }
          val resolvedCity = city.trim().ifBlank { if (hasCoords) "موقع الخريطة" else "" }
          val resolvedStreet = streetAddress.trim().ifBlank {
            if (hasCoords) "إحداثيات الموقع: $coordsFormatted" else ""
          }
          if (
            label.isBlank() ||
            recipientName.isBlank() ||
            phone.trim().length < 8 ||
            (!hasCoords && (resolvedGov.isBlank() || resolvedCity.isBlank() || resolvedStreet.isBlank()))
          ) {
            validationError = "يرجى استكمال التسمية واسم المستلم والهاتف، مع التقاط الموقع (GPS) أو كتابة العنوان."
          } else {
            onConfirm(
              DeliveryAddress(
                id = initial?.id.orEmpty(),
                userId = initial?.userId.orEmpty(),
                label = label.trim(),
                recipientName = recipientName.trim(),
                phone = phone.trim(),
                governorate = resolvedGov,
                city = resolvedCity,
                streetAddress = resolvedStreet,
                landmark = landmark.trim().takeIf { it.isNotEmpty() },
                latitude = latitude,
                longitude = longitude,
                isDefault = isDefault,
              ),
            )
          }
        },
        modifier = Modifier.testTag("save_address_dialog_button"),
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
private fun AddressTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  testTag: String,
  keyboardType: KeyboardType = KeyboardType.Text,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag),
    shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = PureWhite,
      unfocusedContainerColor = PureWhite,
      focusedBorderColor = RoyalBlue,
      unfocusedBorderColor = BorderNeutral,
    ),
  )
}
