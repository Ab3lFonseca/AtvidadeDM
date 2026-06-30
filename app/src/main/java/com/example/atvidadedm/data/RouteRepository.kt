package com.example.atvidadedm.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RouteRepository {
    private val routeCache = linkedMapOf<String, List<DestinationCoordinates>?>()

    suspend fun getDrivingRoute(points: List<DestinationCoordinates>): List<DestinationCoordinates>? {
        val validPoints = points
            .distinctBy { Pair(it.latitude, it.longitude) }
            .filter { it.latitude != 0.0 || it.longitude != 0.0 }

        if (validPoints.size < 2) return null

        val cacheKey = validPoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        if (routeCache.containsKey(cacheKey)) {
            return routeCache[cacheKey]
        }

        // Timeout geral de 8 segundos para toda a operação de rota
        val route: List<DestinationCoordinates>? = withTimeoutOrNull(8_000L) {
            withContext(Dispatchers.IO) {
                try {
                    val coordinates = validPoints.joinToString(";") { "${it.longitude},${it.latitude}" }
                    val url = URL(
                        "https://router.project-osrm.org/route/v1/driving/$coordinates?overview=full&geometries=geojson&steps=false"
                    )
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 12_000
                        readTimeout = 12_000
                        setRequestProperty("User-Agent", "AtvidadeDM/1.0 (android-app)")
                    }

                    if (connection.responseCode !in 200..299) {
                        connection.disconnect()
                        return@withContext null
                    }

                    connection.inputStream.bufferedReader().use { reader ->
                        val body = reader.readText()
                        val root = JSONObject(body)
                        val routes = root.optJSONArray("routes") ?: return@withContext null
                        if (routes.length() == 0) return@withContext null

                        val geometry = routes.getJSONObject(0).optJSONObject("geometry") ?: return@withContext null
                        val coordinatesArray = geometry.optJSONArray("coordinates") ?: return@withContext null
                        if (coordinatesArray.length() == 0) return@withContext null

                        coordinatesArray
                            .let { array ->
                                buildList<DestinationCoordinates> {
                                    for (i in 0 until array.length()) {
                                        val coordinate = array.getJSONArray(i)
                                        val longitude = coordinate.optDouble(0)
                                        val latitude = coordinate.optDouble(1)
                                        add(DestinationCoordinates(latitude = latitude, longitude = longitude))
                                    }
                                }
                            }
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        routeCache[cacheKey] = route
        trimCacheIfNeeded()
        return route
    }

    private fun trimCacheIfNeeded() {
        while (routeCache.size > 16) {
            val firstKey = routeCache.entries.firstOrNull()?.key ?: return
            routeCache.remove(firstKey)
        }
    }
}

