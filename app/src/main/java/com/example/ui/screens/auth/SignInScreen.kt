package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.auth.attemptAutoSignIn
import com.example.core.auth.onGoogleSignInClicked
import com.example.ui.components.AlEmanCard
import com.example.ui.components.AlEmanPrimaryButton
import com.example.ui.components.AlEmanTopBar
import com.example.ui.components.SignInCleaningHeroHeader
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.DiscountRedSoft
import com.example.ui.theme.GeneralBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SignInScreen(
  onAuthSuccess: () -> Unit,
  onOpenPolicies: (() -> Unit)? = null,
  onBackClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isCheckingAutoSignIn by remember { mutableStateOf(true) }
  var isSubmitting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingAutoSignIn = false
        onAuthSuccess()
      },
      onUnauthenticated = {
        isCheckingAutoSignIn = false
      },
      scope = scope,
    )
  }

  Scaffold(
    containerColor = GeneralBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      AlEmanTopBar(
        title = "تسجيل الدخول",
        subtitle = stringResource(id = R.string.app_name),
        showBack = onBackClick != null,
        onBackClick = { onBackClick?.invoke() },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (isCheckingAutoSignIn) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CircularProgressIndicator(color = CharcoalBlack)
            Text(
              text = "جاري التحقق من الحساب...",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
        }
      } else {
        Column(
          modifier = Modifier
            .widthIn(max = 540.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AlEmanDimens.SpaceLg),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          SignInCleaningHeroHeader()

          AlEmanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Text(
                text = "مرحبًا بك في ${stringResource(id = R.string.app_name)}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary,
                textAlign = TextAlign.Center,
              )

              if (!errorMessage.isNullOrBlank()) {
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_in_error_banner"),
                  shape = RoundedCornerShape(AlEmanDimens.RadiusSm),
                  color = DiscountRedSoft,
                  border = BorderStroke(1.dp, DiscountRed.copy(alpha = 0.35f)),
                ) {
                  Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = DiscountRed,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center,
                  )
                }
              }

              AlEmanPrimaryButton(
                text = if (isSubmitting) "جاري تسجيل الدخول..." else "المتابعة بحساب Google",
                enabled = !isSubmitting,
                icon = Icons.AutoMirrored.Filled.Login,
                onClick = {
                  errorMessage = null
                  isSubmitting = true
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                      isSubmitting = false
                      onAuthSuccess()
                    },
                    onAuthError = { msg ->
                      isSubmitting = false
                      errorMessage = msg
                    },
                    onAuthCancelled = {
                      isSubmitting = false
                    },
                    scope = scope,
                  )
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("google_sign_in_button"),
              )
            }
          }

          if (onOpenPolicies != null) {
            TextButton(
              onClick = onOpenPolicies,
              modifier = Modifier
                .height(48.dp)
                .testTag("sign_in_policies_button"),
            ) {
              Text(
                text = "بالمتابعة فأنت توافق على سياسة الخصوصية والشروط والأحكام",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextSecondary,
                textAlign = TextAlign.Center,
              )
            }
          }

          if (onBackClick != null) {
            TextButton(
              onClick = onBackClick,
              modifier = Modifier
                .height(48.dp)
                .testTag("continue_browsing_as_guest_button"),
            ) {
              Text(
                text = "العودة لتصفح المتجر كزائر",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CharcoalBlack,
              )
            }
          }
        }
      }
    }
  }
}
