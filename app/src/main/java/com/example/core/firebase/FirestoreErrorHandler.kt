package com.example.core.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?,
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
    ?: causes.firstOrNull { it !is CancellationException }
    ?: exception

  val auth = FirebaseAuth.getInstance()
  val currentUser = auth.currentUser

  val providerInfoList = currentUser?.providerData?.map { provider ->
    JSONObject().apply {
      put("providerId", provider.providerId)
      put("email", provider.email)
    }
  } ?: emptyList()

  val authInfoJson = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", JSONArray(providerInfoList))
  }

  val errorInfoJson = JSONObject().apply {
    put("error", rootError.message ?: rootError.toString())
    put("operationType", operationType.value)
    put("path", path)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
  return generateSequence(this) { it.cause }
    .filterIsInstance<FirebaseFirestoreException>()
    .firstOrNull()
}

fun Throwable.toArabicErrorMessage(fallback: String = "حدث خطأ غير متوقع، يرجى المحاولة مرة أخرى."): String {
  val firestoreError = unwrapFirestoreException()
  return when (firestoreError?.code) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
      "لا تملك صلاحية تنفيذ هذا الإجراء أو انتهت الجلسة."
    FirebaseFirestoreException.Code.UNAVAILABLE ->
      "تعذر الاتصال بالخادم، تأكد من اتصال الإنترنت وحاول مجددًا."
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
      "تم تجاوز الحد اليومي للاستخدام مؤقتًا، يرجى المحاولة لاحقًا."
    else -> fallback
  }
}
