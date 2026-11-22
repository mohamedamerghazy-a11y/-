package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlEmanTopBar(
  title: String,
  subtitle: String? = null,
  showBack: Boolean = false,
  onBackClick: () -> Unit = {},
  trailingContent: @Composable (() -> Unit)? = null,
) {
  Column(modifier = Modifier.background(PureWhite)) {
    TopAppBar(
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = PureWhite,
        titleContentColor = CharcoalBlack,
      ),
      navigationIcon = {
        if (showBack) {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("top_bar_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(id = R.string.back_button_desc),
              tint = CharcoalBlack,
            )
          }
        }
      },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (!showBack) {
            Image(
              painter = painterResource(id = R.drawable.app_logo),
              contentDescription = null,
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp)),
            )
          }
          Column {
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = CharcoalBlack,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      },
      actions = {
        trailingContent?.invoke()
      },
    )
    HorizontalDivider(color = BorderNeutral)
  }
}

@Composable
fun AlEmanPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.height(AlEmanDimens.ButtonHeight),
    shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
    colors = ButtonDefaults.buttonColors(
      containerColor = PrimaryYellow,
      contentColor = CharcoalBlack,
    ),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = CharcoalBlack)
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
      color = CharcoalBlack,
    )
  }
}

@Composable
fun AlEmanSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.height(AlEmanDimens.ButtonHeight),
    shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
    colors = ButtonDefaults.buttonColors(
      containerColor = GoldenYellow,
      contentColor = TextPrimary,
    ),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    )
  }
}

@Composable
fun AlEmanCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val cardModifier = if (onClick != null) {
    modifier
      .clip(RoundedCornerShape(AlEmanDimens.RadiusMd))
      .clickable(onClick = onClick)
  } else {
    modifier
  }
  Card(
    modifier = cardModifier,
    shape = RoundedCornerShape(AlEmanDimens.RadiusMd),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(AlEmanDimens.BorderWidth, BorderNeutral),
    elevation = CardDefaults.cardElevation(defaultElevation = AlEmanDimens.CardElevation),
    content = content,
  )
}

@Composable
fun StoreSearchTriggerBar(
  placeholder: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onScanQrClick: (() -> Unit)? = null,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .testTag("store_search_trigger_bar"),
    shape = RoundedCornerShape(24.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, Color(0xFFDCDFE4)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = if (onScanQrClick != null) 4.dp else 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = stringResource(id = R.string.nav_search),
        tint = CharcoalBlack,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = placeholder,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.Medium,
          color = Color(0xFF4A4D54),
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      if (onScanQrClick != null) {
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .background(Color(0xFFDCE0E6)),
        )
        IconButton(
          onClick = onScanQrClick,
          modifier = Modifier.testTag("search_trigger_qr_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.QrCodeScanner,
            contentDescription = "البحث بمسح QR أو الباركود",
            tint = CharcoalBlack,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
}

@Composable
fun SectionHeaderRow(
  title: String,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = TextPrimary,
    )
    if (!actionLabel.isNullOrBlank() && onActionClick != null) {
      Surface(
        shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
        color = PureWhite,
        border = BorderStroke(AlEmanDimens.BorderWidth, RoyalBlue.copy(alpha = 0.28f)),
        modifier = Modifier
          .clip(RoundedCornerShape(AlEmanDimens.RadiusSm))
          .clickable(onClick = onActionClick),
      ) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = RoyalBlue,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@Composable
fun BrandPillBadge(
  text: String,
  modifier: Modifier = Modifier,
  containerColor: Color = GoldenYellow,
  contentColor: Color = TextPrimary,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = containerColor,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
      color = contentColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun DiscountBadge(text: String, modifier: Modifier = Modifier) {
  BrandPillBadge(
    text = text,
    modifier = modifier,
    containerColor = DiscountRedSoft,
    contentColor = DiscountRed,
  )
}

@Composable
fun SuccessBadge(text: String, modifier: Modifier = Modifier) {
  BrandPillBadge(
    text = text,
    modifier = modifier,
    containerColor = SuccessGreenSoft,
    contentColor = SuccessGreen,
  )
}

@Composable
fun StoreEmptyStateCard(
  icon: ImageVector,
  title: String,
  description: String,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  AlEmanCard(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 26.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(RoyalBlueSoft),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = RoyalBlue,
          modifier = Modifier.size(26.dp),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
        textAlign = TextAlign.Center,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )
      if (!actionLabel.isNullOrBlank() && onActionClick != null) {
        Spacer(modifier = Modifier.height(4.dp))
        AlEmanPrimaryButton(
          text = actionLabel,
          onClick = onActionClick,
        )
      }
    }
  }
}

@Composable
fun AlEmanSnackbarHost(
  hostState: SnackbarHostState,
  modifier: Modifier = Modifier,
) {
  SnackbarHost(
    hostState = hostState,
    modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
  ) { data ->
    val msg = data.visuals.message
    val isError = listOf(
      "تعذر",
      "خطأ",
      "غير صالح",
      "نفد",
      "فشل",
      "مرفوض",
      "عفوًا",
      "الحد الأقصى",
      "غير متوفر",
      "يرجى",
      "أدخل",
    ).any { msg.contains(it) }

    val accentColor = if (isError) DiscountRed else PrimaryYellow
    val iconBgColor = if (isError) DiscountRed.copy(alpha = 0.18f) else PrimaryYellow.copy(alpha = 0.18f)

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = CharcoalBlack,
      border = BorderStroke(1.25.dp, accentColor.copy(alpha = 0.72f)),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("aleman_snackbar_card"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(iconBgColor),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp),
          )
        }
        Text(
          text = msg,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = PureWhite,
          modifier = Modifier.weight(1f),
        )
        data.visuals.actionLabel?.let { actionText ->
          TextButton(onClick = { data.performAction() }) {
            Text(
              text = actionText,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = PrimaryYellow,
            )
          }
        }
      }
    }
  }
}
