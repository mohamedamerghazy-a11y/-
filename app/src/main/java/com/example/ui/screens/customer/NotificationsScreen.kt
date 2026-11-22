package com.example.ui.screens.customer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.ui.components.AlEmanSnackbarHost
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.BrandPillBadge
import com.example.ui.components.StoreEmptyStateCard
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NotificationsUiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationsScreen(
  uiState: NotificationsUiState,
  onMarkAsRead: (String) -> Unit,
  onMarkAllAsRead: () -> Unit,
  onDeleteNotification: (String) -> Unit,
  onNotificationClick: (AppNotification) -> Unit,
  onClearMessages: () -> Unit,
  onBackClick: () -> Unit,
) {
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
        title = "الإشعارات والتنبيهات",
        subtitle = if (uiState.unreadCount > 0) "${uiState.unreadCount} إشعار غير مقروء" else "كل التنبيهات مقروءة",
        showBack = true,
        onBackClick = onBackClick,
        trailingContent = {
          if (uiState.unreadCount > 0) {
            TextButton(
              onClick = onMarkAllAsRead,
              modifier = Modifier.testTag("notifications_mark_all_read_button"),
            ) {
              Icon(Icons.Filled.DoneAll, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("قراءة الكل", color = CharcoalBlack, fontWeight = FontWeight.Bold)
            }
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
      when {
        uiState.isLoading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CharcoalBlack)
          }
        }

        uiState.notifications.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AlEmanDimens.SpaceLg),
            contentAlignment = Alignment.Center,
          ) {
            StoreEmptyStateCard(
              icon = Icons.Filled.NotificationsNone,
              title = "لا توجد إشعارات حاليًا",
              description = "ستصلك هنا إشعارات حالة الطلبات، مراجعة المدفوعات، واعتماد حسابات الجملة والعروض.",
              actionLabel = "العودة للمتجر",
              onActionClick = onBackClick,
            )
          }
        }

        else -> {
          LazyColumn(
            modifier = Modifier
              .widthIn(max = 760.dp)
              .fillMaxSize(),
            contentPadding = PaddingValues(AlEmanDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(uiState.notifications, key = { it.id }) { notification ->
              NotificationItemCard(
                notification = notification,
                onClick = {
                  if (!notification.isRead) onMarkAsRead(notification.id)
                  onNotificationClick(notification)
                },
                onDelete = { onDeleteNotification(notification.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationItemCard(
  notification: AppNotification,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  val dateLabel = remember(notification.createdAt) {
    notification.createdAt?.toDate()?.let {
      val arLocale = Locale.Builder().setLanguage("ar").setRegion("EG").build()
      SimpleDateFormat("dd MMM • hh:mm a", arLocale).format(it)
    }.orEmpty()
  }

  val containerColor = if (notification.isRead) PureWhite else PrimaryYellowSoft
  val borderColor = if (notification.isRead) BorderNeutral else PrimaryYellow

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(AlEmanDimens.RadiusMd))
      .clickable(onClick = onClick)
      .testTag("notification_card_${notification.id}"),
    shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(if (notification.isRead) GeneralBackground else PrimaryYellow),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = notificationTypeIcon(notification.typeEnum),
          contentDescription = notification.typeEnum.arabicLabel,
          tint = CharcoalBlack,
          modifier = Modifier.size(20.dp),
        )
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          BrandPillBadge(
            text = notification.typeEnum.arabicLabel,
            containerColor = CharcoalBlack,
            contentColor = PrimaryYellow,
          )
          if (!notification.isRead) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(DiscountRed),
            )
          }
        }

        Text(
          text = notification.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = CharcoalBlack,
        )
        Text(
          text = notification.message,
          style = MaterialTheme.typography.bodySmall,
          color = CharcoalBlack.copy(alpha = 0.85f),
        )
        if (dateLabel.isNotEmpty()) {
          Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.testTag("delete_notification_${notification.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = "حذف الإشعار",
          tint = TextSecondary,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

private fun notificationTypeIcon(type: NotificationType): ImageVector = when (type) {
  NotificationType.ORDER_STATUS -> Icons.Filled.LocalShipping
  NotificationType.PAYMENT_STATUS -> Icons.Filled.Payments
  NotificationType.WHOLESALE_STATUS -> Icons.Filled.VerifiedUser
  NotificationType.STOCK_ALERT -> Icons.Filled.Inventory
  NotificationType.OFFER_PROMO, NotificationType.GENERAL -> Icons.Filled.Campaign
}
