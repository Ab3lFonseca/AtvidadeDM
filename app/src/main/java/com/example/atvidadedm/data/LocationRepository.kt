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

        val geocoder = Geocoder(context, Locale.getDefault())

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
        if (query.isBlank()) return null

        destinationCoordinatesCache[query]?.let { return it }

        val candidates = buildQueryCandidates(query)
        for (candidate in candidates) {
            val resolved = supervisorScope {
                val geocoderDeferred = async {
                    if (Geocoder.isPresent()) {
                        withTimeoutOrNull(4000L) { getCoordinatesFromAndroidGeocoder(candidate) }
                    } else null
                }
                val nominatimDeferred = async {
                    withTimeoutOrNull(7000L) { getCoordinatesFromNominatim(candidate) }
                }

                // Tenta Geocoder primeiro (mais rápido se funcionar)
                val geoResult = try { geocoderDeferred.await() } catch (_: Exception) { null }
                if (geoResult != null) return@supervisorScope geoResult

                // Fallback para Nominatim
                try { nominatimDeferred.await() } catch (_: Exception) { null }
            }

            if (resolved != null) {
                destinationCoordinatesCache[query] = resolved
                trimCacheIfNeeded()
                return resolved
            }
        }
        return null
    }

    private fun buildQueryCandidates(query: String): List<String> {
        val compact = query
            .replace("->", " ")
            .replace("|", " ")
            .replace("/", " ")
            .replace("  ", " ")
            .trim()

        val list = mutableListOf<String>()
        list.add(query)
        if (compact != query && compact.isNotBlank()) {
            list.add(compact)
        }
        return list.distinct()
    }


    private suspend fun getCoordinatesFromAndroidGeocoder(query: String): DestinationCoordinates? {
        // Tenta com locale padrão, se falhar tenta US
        return getCoordinatesFromAndroidGeocoderWithLocale(query, Locale.getDefault())
            ?: getCoordinatesFromAndroidGeocoderWithLocale(query, Locale.US)
    }

    private suspend fun getCoordinatesFromAndroidGeocoderWithLocale(query: String, locale: Locale): DestinationCoordinates? {
        val geocoder = Geocoder(context, locale)
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
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getCoordinatesFromNominatim(query: String): DestinationCoordinates? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
                // Usando um endpoint HTTPS estável e User-Agent genérico porém identificável
                val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "TravelAppAndroid/1.0")
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode !in 200..299) {
                    return@withContext null
                }

                connection.inputStream.bufferedReader().use { reader ->
                    val body = reader.readText()
                    val array = org.json.JSONArray(body)
                    if (array.length() == 0) return@withContext null

                    val first = array.getJSONObject(0)
                    val latitude = first.optString("lat").toDoubleOrNull()
                    val longitude = first.optString("lon").toDoubleOrNull()
                    
                    if (latitude != null && longitude != null) {
                        DestinationCoordinates(latitude = latitude, longitude = longitude)
                    } else {
                        null
                    }
                }
            } catch (_: Exception) {
                null
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun trimCacheIfNeeded() {
        while (destinationCoordinatesCache.size > 50) {
            val firstKey = destinationCoordinatesCache.keys.firstOrNull() ?: return
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
