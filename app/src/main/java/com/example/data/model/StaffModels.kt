package com.example.data.model

import com.example.core.model.StaffPermission
import com.example.core.model.StaffRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StaffMember(
  @DocumentId val id: String = "",
  val uid: String = "",
  val fullName: String = "",
  val email: String = "",
  val role: String = StaffRole.CUSTOMER_SERVICE.firestoreKey,
  val permissions: List<String> = emptyList(),
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val staffRoleEnum: StaffRole
    get() = StaffRole.fromKey(role)

  @get:Exclude
  val permissionSet: Set<StaffPermission>
    get() = permissions.mapNotNull { StaffPermission.fromKey(it) }.toSet()
}
