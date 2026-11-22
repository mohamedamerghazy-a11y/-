package com.example.ui.screens.admin

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.example.data.model.StaffMember
import com.example.data.model.UserProfile
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.DiscountBadge
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminStaffUiState

@Composable
fun AdminStaffManagementScreen(
  uiState: AdminStaffUiState,
  onAssignStaff: (UserProfile, StaffRole, Set<StaffPermission>, Boolean) -> Unit,
  onRemoveStaff: (String) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
  var showAssignDialog by remember { mutableStateOf(false) }
  var editingStaff by remember { mutableStateOf<StaffMember?>(null) }
  var removingStaffUid by remember { mutableStateOf<String?>(null) }
  var showRolesGuide by remember { mutableStateOf(false) }

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
        title = "إدارة الموظفين والصلاحيات",
        subtitle = "الأدوار الإدارية والتوصيل وتخصيص الصلاحيات الدقيقة",
        showBack = true,
        onBackClick = onBackClick,
        trailingContent = {
          IconButton(onClick = { showRolesGuide = !showRolesGuide }) {
            Icon(
              imageVector = Icons.Filled.Security,
              contentDescription = "دليل الأدوار والصلاحيات",
              tint = CharcoalBlack,
            )
          }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingStaff = null
          showAssignDialog = true
        },
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier.testTag("admin_add_staff_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "تعيين موظف جديد")
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
            if (showRolesGuide) {
              item {
                RolesGuideCard()
              }
            }

            if (uiState.staffList.isEmpty()) {
              item {
                StoreEmptyStateCard(
                  icon = Icons.Filled.Badge,
                  title = "لا يوجد موظفون معينون حاليًا",
                  description = "اضغط على زر الإضافة أدناه لتعيين مستخدم بأحد الأدوار الـ 8 (بما فيها موظف التوصيل) وتحديد صلاحياته.",
                  actionLabel = "تعيين موظف جديد",
                  onActionClick = {
                    editingStaff = null
                    showAssignDialog = true
                  },
                )
              }
            } else {
              items(uiState.staffList, key = { it.uid }) { staff ->
                StaffMemberCard(
                  staff = staff,
                  onEdit = {
                    editingStaff = staff
                    showAssignDialog = true
                  },
                  onRemove = { removingStaffUid = staff.uid },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showAssignDialog) {
    AssignOrEditStaffDialog(
      editingStaff = editingStaff,
      usersList = uiState.usersList,
      existingStaffUids = uiState.staffMap.keys,
      onDismiss = { showAssignDialog = false },
      onConfirm = { user, role, permissions, isActive ->
        onAssignStaff(user, role, permissions, isActive)
        showAssignDialog = false
      },
    )
  }

  if (removingStaffUid != null) {
    AlertDialog(
      onDismissRequest = { removingStaffUid = null },
      containerColor = PureWhite,
      title = { Text("تأكيد إزالة الموظف", fontWeight = FontWeight.Bold, color = CharcoalBlack) },
      text = { Text("هل أنت متأكد من إزالة صلاحيات الموظف لهذا المستخدم وإعادته كعميل عادي؟") },
      confirmButton = {
        Button(
          onClick = {
            removingStaffUid?.let { onRemoveStaff(it) }
            removingStaffUid = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DiscountRed, contentColor = PureWhite),
        ) {
          Text("إزالة الصلاحيات")
        }
      },
      dismissButton = {
        TextButton(onClick = { removingStaffUid = null }) {
          Text("إلغاء", color = TextSecondary)
        }
      },
    )
  }
}

@Composable
private fun RolesGuideCard() {
  val adminRoles = remember { StaffRole.entries.filter { it != StaffRole.CUSTOMER } }
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "دليل الأدوار المعتمدة (8 أدوار)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
      adminRoles.forEach { role ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          BrandPillBadge(
            text = role.arabicLabel,
            containerColor = if (role == StaffRole.OWNER) PrimaryYellow else PrimaryYellowSoft,
            contentColor = CharcoalBlack,
          )
          Text(
            text = "${role.permissions.size} صلاحية افتراضية",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffMemberCard(
  staff: StaffMember,
  onEdit: () -> Unit,
  onRemove: () -> Unit,
) {
  AlEmanCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = staff.fullName.ifEmpty { "موظف متجر" },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
            if (staff.isActive) SuccessBadge("نشط") else DiscountBadge("موقوف")
          }
          Text(
            text = staff.email,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        BrandPillBadge(
          text = staff.staffRoleEnum.arabicLabel,
          containerColor = PrimaryYellow,
          contentColor = CharcoalBlack,
        )
      }

      if (staff.permissionSet.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          staff.permissionSet.take(4).forEach { perm ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = GeneralBackground,
              border = BorderStroke(0.5.dp, BorderNeutral),
            ) {
              Text(
                text = perm.arabicTitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
          if (staff.permissionSet.size > 4) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PrimaryYellowSoft,
            ) {
              Text(
                text = "+${staff.permissionSet.size - 4} أخرى",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
      }

      HorizontalDivider(color = BorderNeutral)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "الصلاحيات المفعلة: ${staff.permissions.size}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
        Row {
          if (staff.staffRoleEnum != StaffRole.OWNER) {
            IconButton(
              onClick = onEdit,
              modifier = Modifier.testTag("edit_staff_${staff.uid}"),
            ) {
              Icon(Icons.Filled.Edit, contentDescription = "تعديل الصلاحيات", tint = CharcoalBlack)
            }
            IconButton(
              onClick = onRemove,
              modifier = Modifier.testTag("remove_staff_${staff.uid}"),
            ) {
              Icon(Icons.Filled.Delete, contentDescription = "إزالة الموظف", tint = DiscountRed)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AssignOrEditStaffDialog(
  editingStaff: StaffMember?,
  usersList: List<UserProfile>,
  existingStaffUids: Set<String>,
  onDismiss: () -> Unit,
  onConfirm: (UserProfile, StaffRole, Set<StaffPermission>, Boolean) -> Unit,
) {
  val availableUsers = if (editingStaff != null) {
    val matched = usersList.firstOrNull { it.uid == editingStaff.uid }
      ?: UserProfile(
        uid = editingStaff.uid,
        fullName = editingStaff.fullName,
        email = editingStaff.email,
        role = editingStaff.role,
      )
    listOf(matched)
  } else {
    usersList.filter { it.uid !in existingStaffUids }
  }

  var selectedUser by remember(editingStaff, usersList) {
    mutableStateOf(availableUsers.firstOrNull())
  }
  var selectedRole by remember(editingStaff) {
    mutableStateOf(
      editingStaff?.staffRoleEnum?.takeIf { it != StaffRole.CUSTOMER && it != StaffRole.OWNER }
        ?: StaffRole.CUSTOMER_SERVICE,
    )
  }
  var selectedPermissions by remember(editingStaff) {
    mutableStateOf(editingStaff?.permissionSet ?: StaffRole.CUSTOMER_SERVICE.permissions)
  }
  var isActive by remember(editingStaff) {
    mutableStateOf(editingStaff?.isActive ?: true)
  }

  var userExpanded by remember { mutableStateOf(false) }
  var roleExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = PureWhite,
    title = {
      Text(
        text = if (editingStaff == null) "تعيين موظف جديد" else "تعديل صلاحيات الموظف",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (availableUsers.isEmpty()) {
          Text(
            text = "لا توجد حسابات مستخدمين جديدة متاحة للتعيين حاليًا.",
            color = TextSecondary,
          )
        } else {
          if (editingStaff == null) {
            ExposedDropdownMenuBox(
              expanded = userExpanded,
              onExpandedChange = { userExpanded = it },
            ) {
              OutlinedTextField(
                value = selectedUser?.let { "${it.fullName} (${it.email})" } ?: "اختر المستخدم",
                onValueChange = {},
                readOnly = true,
                label = { Text("المستخدم") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded) },
                modifier = Modifier
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                  .fillMaxWidth(),
              )
              ExposedDropdownMenu(
                expanded = userExpanded,
                onDismissRequest = { userExpanded = false },
                containerColor = PureWhite,
              ) {
                availableUsers.forEach { user ->
                  DropdownMenuItem(
                    text = { Text("${user.fullName} (${user.email})") },
                    onClick = {
                      selectedUser = user
                      userExpanded = false
                    },
                  )
                }
              }
            }
          } else {
            Text(
              text = "${editingStaff.fullName} • ${editingStaff.email}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
            )
          }

          ExposedDropdownMenuBox(
            expanded = roleExpanded,
            onExpandedChange = { roleExpanded = it },
          ) {
            OutlinedTextField(
              value = selectedRole.arabicLabel,
              onValueChange = {},
              readOnly = true,
              label = { Text("الدور الوظيفي") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = roleExpanded,
              onDismissRequest = { roleExpanded = false },
              containerColor = PureWhite,
            ) {
              StaffRole.entries
                .filter { it != StaffRole.CUSTOMER && it != StaffRole.OWNER }
                .forEach { role ->
                  DropdownMenuItem(
                    text = { Text("${role.arabicLabel} (${role.permissions.size} صلاحية)") },
                    onClick = {
                      selectedRole = role
                      selectedPermissions = role.permissions
                      roleExpanded = false
                    },
                  )
                }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("حساب الموظف نشط ومفعّل", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = isActive,
              onCheckedChange = { isActive = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = CharcoalBlack,
                checkedTrackColor = PrimaryYellow,
              ),
            )
          }

          HorizontalDivider(color = BorderNeutral)

          Text(
            text = "تخصيص الصلاحيات الدقيقة (${selectedPermissions.size})",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = CharcoalBlack,
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            StaffPermission.entries.forEach { perm ->
              val isSelected = perm in selectedPermissions
              FilterChip(
                selected = isSelected,
                onClick = {
                  selectedPermissions = if (isSelected) {
                    selectedPermissions - perm
                  } else {
                    selectedPermissions + perm
                  }
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryYellow,
                  selectedLabelColor = CharcoalBlack,
                ),
                label = {
                  Text(
                    text = perm.arabicTitle,
                    style = MaterialTheme.typography.labelSmall,
                  )
                },
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          selectedUser?.let { user ->
            onConfirm(user, selectedRole, selectedPermissions, isActive)
          }
        },
        enabled = selectedUser != null && availableUsers.isNotEmpty() && selectedPermissions.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryYellow,
          contentColor = CharcoalBlack,
        ),
      ) {
        Text(
          text = if (editingStaff == null) "حفظ التعيين" else "حفظ التعديلات",
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
