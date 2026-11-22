package com.example.data.model

import com.example.core.model.AccountType
import com.example.core.model.StaffRole
import com.example.core.model.WholesaleStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val uid: String = "",
  val fullName: String = "",
  val email: String = "",
  val phone: String = "",
  val photoUrl: String? = null,
  val accountType: String = "retail",
  val wholesaleStatus: String = "none",
  val wholesaleCompanyName: String? = null,
  val role: String = "customer",
  val lastOrderId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val accountTypeEnum: AccountType
    get() = if (accountType == "wholesale") AccountType.WHOLESALE else AccountType.RETAIL

  @get:Exclude
  val wholesaleStatusEnum: WholesaleStatus
    get() = when (wholesaleStatus) {
      "pending" -> WholesaleStatus.PENDING
      "approved" -> WholesaleStatus.APPROVED
      "rejected" -> WholesaleStatus.REJECTED
      else -> WholesaleStatus.NONE
    }

  @get:Exclude
  val isWholesaleApproved: Boolean
    get() = accountType == "wholesale" && wholesaleStatus == "approved"

  @get:Exclude
  val staffRoleEnum: StaffRole
    get() = StaffRole.fromKey(role)

  @get:Exclude
  val canAccessAdmin: Boolean
    get() = staffRoleEnum != StaffRole.CUSTOMER
}

@IgnoreExtraProperties
data class DeliveryAddress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val label: String = "",
  val recipientName: String = "",
  val phone: String = "",
  val governorate: String = "",
  val city: String = "",
  val streetAddress: String = "",
  val landmark: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val isDefault: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val hasCoordinates: Boolean
    get() = latitude != null && longitude != null
}
