package com.example.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class CapturedLocation(
  val latitude: Double,
  val longitude: Double,
  val governorate: String? = null,
  val city: String? = null,
  val streetAddress: String? = null,
)

object LocationHelper {

  fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  suspend fun captureCurrentLocation(context: Context): Result<CapturedLocation> {
    val appCtx = context.applicationContext
    if (!hasLocationPermission(appCtx)) {
      return Result.failure(IllegalStateException("يرجى السماح بصلاحية الموقع أولًا لتحديد الإحداثيات."))
    }
    val locationManager = appCtx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      ?: return Result.failure(IllegalStateException("خدمة تحديد الموقع غير متوفرة على هذا الجهاز."))

    val gpsEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    val networkEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)

    val freshLocation = withTimeoutOrNull(7_000L) {
      val preferredProvider = when {
        gpsEnabled -> LocationManager.GPS_PROVIDER
        networkEnabled -> LocationManager.NETWORK_PROVIDER
        else -> null
      }
      if (preferredProvider == null) return@withTimeoutOrNull null
      requestSingleLocation(appCtx, locationManager, preferredProvider)
    }

    val bestLocation = freshLocation ?: bestLastKnownLocation(locationManager)
      ?: return Result.failure(
        IllegalStateException(
          if (!gpsEnabled && !networkEnabled) {
            "يرجى تفعيل خدمة الموقع (GPS) على الهاتف ثم المحاولة مرة أخرى."
          } else {
            "تعذر التقاط إشارة GPS حاليًا، يمكنك المحاولة مجددًا أو التحقق من إعدادات الموقع."
          },
        ),
      )

    val geocoded = reverseGeocode(appCtx, bestLocation.latitude, bestLocation.longitude)
    return Result.success(
      CapturedLocation(
        latitude = bestLocation.latitude,
        longitude = bestLocation.longitude,
        governorate = geocoded?.governorate,
        city = geocoded?.city,
        streetAddress = geocoded?.streetAddress,
      ),
    )
  }

  @SuppressLint("MissingPermission")
  private suspend fun requestSingleLocation(
    context: Context,
    locationManager: LocationManager,
    provider: String,
  ): Location? = suspendCancellableCoroutine { cont ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val signal = CancellationSignal()
      cont.invokeOnCancellation { signal.cancel() }
      runCatching {
        locationManager.getCurrentLocation(
          provider,
          signal,
          ContextCompat.getMainExecutor(context),
        ) { location ->
          if (cont.isActive) cont.resume(location)
        }
      }.onFailure {
        if (cont.isActive) cont.resume(null)
      }
    } else {
      val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
          locationManager.removeUpdates(this)
          if (cont.isActive) cont.resume(location)
        }

        override fun onProviderDisabled(providerName: String) {
          locationManager.removeUpdates(this)
          if (cont.isActive) cont.resume(null)
        }
      }
      cont.invokeOnCancellation {
        runCatching { locationManager.removeUpdates(listener) }
      }
      runCatching {
        @Suppress("DEPRECATION")
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
      }.onFailure {
        if (cont.isActive) cont.resume(null)
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun bestLastKnownLocation(locationManager: LocationManager): Location? {
    val providers = runCatching { locationManager.getProviders(true) }.getOrNull().orEmpty()
      .ifEmpty { listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER) }
    var best: Location? = null
    providers.forEach { provider ->
      val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: return@forEach
      if (best == null || loc.time > best!!.time || loc.accuracy < best!!.accuracy) {
        best = loc
      }
    }
    return best
  }

  private suspend fun reverseGeocode(
    context: Context,
    latitude: Double,
    longitude: Double,
  ): CapturedLocation? = withContext(Dispatchers.IO) {
    runCatching {
      @Suppress("DEPRECATION")
      val results = Geocoder(context, Locale("ar")).getFromLocation(latitude, longitude, 1)
      val addr = results?.firstOrNull() ?: return@runCatching null
      val governorate = addr.adminArea?.trim()?.takeIf { it.isNotEmpty() }
      val city = (addr.subAdminArea ?: addr.locality)?.trim()?.takeIf { it.isNotEmpty() }
      val street = (
        addr.thoroughfare?.takeIf { it.isNotBlank() }
          ?: addr.featureName?.takeIf { it.isNotBlank() && it != governorate && it != city }
          ?: addr.getAddressLine(0)
        )?.trim()?.takeIf { it.isNotEmpty() }
      CapturedLocation(
        latitude = latitude,
        longitude = longitude,
        governorate = governorate,
        city = city,
        streetAddress = street,
      )
    }.getOrNull()
  }

  fun openLocationInMaps(
    context: Context,
    latitude: Double?,
    longitude: Double?,
    fallbackAddress: String? = null,
    label: String? = "موقع التوصيل",
  ) {
    val encodedLabel = Uri.encode(label?.trim()?.takeIf { it.isNotEmpty() } ?: "موقع التوصيل")
    val geoUri = when {
      latitude != null && longitude != null ->
        Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
      !fallbackAddress.isNullOrBlank() ->
        Uri.parse("geo:0,0?q=${Uri.encode(fallbackAddress)}")
      else -> return
    }
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val started = runCatching {
      context.startActivity(mapIntent)
    }.isSuccess

    if (!started) {
      val webUri = when {
        latitude != null && longitude != null ->
          Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
        else ->
          Uri.parse("https://maps.google.com/?q=${Uri.encode(fallbackAddress.orEmpty())}")
      }
      runCatching {
        context.startActivity(
          Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          },
        )
      }
    }
  }

  fun formatCoordinates(latitude: Double?, longitude: Double?): String? {
    if (latitude == null || longitude == null) return null
    return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
  }
}
