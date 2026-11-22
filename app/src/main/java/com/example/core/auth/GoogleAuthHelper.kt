package com.example.core.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "AlEmanAuth"

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }

  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    Log.w(AUTH_TAG, "default_web_client_id missing for silent sign-in", e)
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      Log.d(AUTH_TAG, "Silent auto sign-in unavailable: ${e.message}")
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("تعذر العثور على إعدادات تسجيل الدخول بحساب Google.")
    return
  }

  val activity = context as? Activity
  if (activity == null) {
    onAuthError("تعذر بدء نافذة تسجيل الدخول في هذه الواجهة.")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("نوع بيانات الاعتماد المستلم غير مدعوم.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Google Sign-In failed", e)
      onAuthError("تعذر إتمام تسجيل الدخول بحساب Google، يرجى المحاولة مجددًا.")
    }
  }
}

sealed interface ReauthResult {
  data object Success : ReauthResult
  data object Cancelled : ReauthResult
  data class Error(val message: String) : ReauthResult
}

suspend fun reauthenticateWithGoogle(
  context: Context,
  credentialManager: CredentialManager,
): ReauthResult {
  val currentUser = Firebase.auth.currentUser
    ?: return ReauthResult.Error("لا يوجد حساب مسجل حاليًا.")

  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    return ReauthResult.Error("تعذر العثور على إعدادات التحقق من حساب Google.")
  }

  val activity = context as? Activity
    ?: return ReauthResult.Error("تعذر فتح نافذة التحقق من الهوية في هذه الواجهة.")

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()

  return try {
    val result = credentialManager.getCredential(activity, request)
    val credential = result.credential
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
      currentUser.reauthenticate(authCredential).await()
      ReauthResult.Success
    } else {
      ReauthResult.Error("تعذر التحقق من بيانات حساب Google.")
    }
  } catch (e: GetCredentialCancellationException) {
    Log.w(AUTH_TAG, "Google re-authentication cancelled: ${e.message}", e)
    ReauthResult.Cancelled
  } catch (e: Exception) {
    Log.e(AUTH_TAG, "Google re-authentication failed", e)
    ReauthResult.Error("تعذر إعادة التحقق من حساب Google، يرجى اختيار نفس الحساب المسجل.")
  }
}

suspend fun clearCredentialManagerState(credentialManager: CredentialManager) {
  withContext(NonCancellable) {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
    }
  }
}

fun signOutUser(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      clearCredentialManagerState(credentialManager)
      Firebase.auth.signOut()
      onSignOutComplete()
    }
  }
}
