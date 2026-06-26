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
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepository(
    private val context: Context
) {
    private val destinationCoordinatesCache = linkedMapOf<String, DestinationCoordinates?>()

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

    suspend fun getCoordinatesForDestination(destination: String): DestinationCoordinates? {
        val query = destination.trim()
        if (query.isBlank()) {
            return null
        }

        destinationCoordinatesCache[query]?.let { return it }

        val candidateQueries = buildQueryCandidates(query)
        candidateQueries.forEach { candidate ->
            val resolved = supervisorScope {
                val nominatimDeferred = async {
                    withTimeoutOrNull(4_500L) {
                        getCoordinatesFromNominatim(candidate)
                    }
                }
                val geocoderDeferred = async {
                    if (Geocoder.isPresent()) {
                        withTimeoutOrNull(2_500L) {
                            getCoordinatesFromAndroidGeocoder(candidate)
                        }
                    } else {
                        null
                    }
                }

                val geocoderResult = geocoderDeferred.await()
                val nominatimResult = nominatimDeferred.await()
                nominatimResult ?: geocoderResult
            }

            if (resolved != null) {
                destinationCoordinatesCache[query] = resolved
                trimCacheIfNeeded()
                return resolved
            }
        }

        destinationCoordinatesCache[query] = null
        trimCacheIfNeeded()
        return null
    }

    private fun buildQueryCandidates(query: String): List<String> {
        val compact = query
            .replace("->", " ")
            .replace("|", " ")
            .replace("/", " ")
            .replace("  ", " ")
            .trim()

        return linkedSetOf(query, compact)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }


    private suspend fun getCoordinatesFromAndroidGeocoder(query: String): DestinationCoordinates? {

        val geocoder = Geocoder(
            context,
            Locale.US
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocationName(query, 1) { addresses ->
                            if (continuation.isActive) {
                                val first = addresses.firstOrNull()
                                continuation.resume(
                                    first?.let {
                                        DestinationCoordinates(
                                            latitude = it.latitude,
                                            longitude = it.longitude
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(query, 1)
                    addresses?.firstOrNull()?.let {
                        DestinationCoordinates(
                            latitude = it.latitude,
                            longitude = it.longitude
                        )
                    }
                }
            }
        } catch (_: IOException) {
            null
        }
    }

    private suspend fun getCoordinatesFromNominatim(query: String): DestinationCoordinates? {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
                val url = URL(
                    "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1&addressdetails=1&accept-language=pt-BR"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "AtvidadeDM/1.0 (android-app)")
                }

                if (connection.responseCode !in 200..299) {
                    connection.disconnect()
                    return@withContext null
                }

                connection.inputStream.bufferedReader().use { reader ->
                    val body = reader.readText()
                    val array = org.json.JSONArray(body)
                    if (array.length() == 0) {
                        return@withContext null
                    }

                    val first = array.getJSONObject(0)
                    val displayName = first.optString("display_name")
                    val normalizedQuery = normalizeText(query)
                    val normalizedDisplay = normalizeText(displayName)
                    if (normalizedQuery.isNotBlank() && normalizedDisplay.isNotBlank() && !normalizedDisplay.contains(normalizedQuery)) {
                        return@withContext null
                    }
                    val latitude = first.optString("lat").toDoubleOrNull()
                    val longitude = first.optString("lon").toDoubleOrNull()
                    if (latitude == null || longitude == null) {
                        null
                    } else {
                        DestinationCoordinates(latitude = latitude, longitude = longitude)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun normalizeText(value: String): String {
        val lower = value.trim().lowercase(Locale.ROOT)
        return java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun trimCacheIfNeeded() {
        while (destinationCoordinatesCache.size > 32) {
            val firstKey = destinationCoordinatesCache.entries.firstOrNull()?.key ?: return
            destinationCoordinatesCache.remove(firstKey)
        }
    }
}

data class DestinationCoordinates(
    val latitude: Double,
    val longitude: Double
)

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
