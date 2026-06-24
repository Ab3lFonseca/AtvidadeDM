package com.example.atvidadedm.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepository(
    private val context: Context
) {
    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCity(): LocationLookupResult {
        if (!hasLocationPermission()) {
            return LocationLookupResult.PermissionDenied
        }

        return try {
            val location = fetchBestAvailableLocation()
                ?: return LocationLookupResult.LocationUnavailable

            val city = withTimeoutOrNull(5_000L) {
                reverseGeocodeCity(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }

            LocationLookupResult.Success(
                city = city,
                latitude = location.latitude,
                longitude = location.longitude
            )
        } catch (_: SecurityException) {
            LocationLookupResult.PermissionDenied
        } catch (_: IOException) {
            LocationLookupResult.LocationUnavailable
        }
    }

    private suspend fun fetchBestAvailableLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val lastKnown = fusedClient.lastLocation.awaitOrNull()
            if (lastKnown != null) {
                return lastKnown
            }

            withTimeoutOrNull(8_000L) {
                val cancellationTokenSource = CancellationTokenSource()
                fusedClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).awaitOrNull()
            }
        } catch (_: SecurityException) {
            null
        }
    }

    private suspend fun reverseGeocodeCity(
        latitude: Double,
        longitude: Double
    ): String? {
        if (!Geocoder.isPresent()) {
            return null
        }

        val geocoder = Geocoder(
            context,
            Locale.Builder().setLanguage("pt").setRegion("BR").build()
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (continuation.isActive) {
                            val city = addresses.firstOrNull()?.locality
                                ?: addresses.firstOrNull()?.subAdminArea
                            continuation.resume(city)
                        }
                    }
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }
}

sealed interface LocationLookupResult {
    data class Success(
        val city: String?,
        val latitude: Double,
        val longitude: Double
    ) : LocationLookupResult
    data object PermissionDenied : LocationLookupResult
    data object LocationUnavailable : LocationLookupResult
}

private suspend fun <T> Task<T>.awaitOrNull(): T? {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
}
