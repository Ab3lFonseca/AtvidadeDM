package com.example.atvidadedm.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.atvidadedm.TravelApplication
import com.example.atvidadedm.data.local.TripEntity
import com.example.atvidadedm.data.local.UserEntity
import com.example.atvidadedm.data.model.TripType
import com.example.atvidadedm.ui.viewmodel.HomeViewModel
import com.example.atvidadedm.ui.viewmodel.HomeViewModelFactory
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DEFAULT_MAP_LATITUDE = -14.235004
private const val DEFAULT_MAP_LONGITUDE = -51.92528

@Composable
fun HomeScreen(
    currentUser: UserEntity,
    onOpenRoteiro: (Long) -> Unit,
    onOpenPhotos: (Long) -> Unit,
    onOpenPhotosFallback: () -> Unit
) {
    Scaffold(
        bottomBar = {
            TripBottomBar(
                selectedDestination = null,
                showPhotoTab = true,
                onOpenRoteiro = { onOpenPhotosFallback() },
                onOpenPhotos = { onOpenPhotosFallback() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bem-vindo, ${currentUser.name}!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Nesta tela o mapa é apenas visual, sem ações extras.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Mapa da viagem", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Visualização ilustrativa do mapa, sem atualização de localização ou botões de ação.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OsmTripMapView(
                        latitude = DEFAULT_MAP_LATITUDE,
                        longitude = DEFAULT_MAP_LONGITUDE,
                        destination = "Mapa ilustrativo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentTripMapCard(
    trip: TripEntity?,
    currentCity: String?,
    latitude: Double?,
    longitude: Double?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mapa da viagem atual", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when {
                    trip != null -> "Local atual da viagem para ${trip.destination}"
                    !currentCity.isNullOrBlank() -> "Localização atual em $currentCity"
                    else -> "Aguardando localização atual"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (latitude == null || longitude == null) {
                Text(
                    text = "A localização ainda não foi carregada. O mapa será exibido com uma posição padrão até a atualização.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OsmTripMapView(
                latitude = latitude ?: DEFAULT_MAP_LATITUDE,
                longitude = longitude ?: DEFAULT_MAP_LONGITUDE,
                destination = trip?.destination ?: currentCity ?: "Localização atual",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }
    }
}

@Composable
private fun ActiveTripCard(trip: TripEntity) {
    val tripType = TripType.fromStorage(trip.type)
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())
    }
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("pt").setRegion("BR").build())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Viagem atual", style = MaterialTheme.typography.titleLarge)
            Text("Destino: ${trip.destination}")
            Text("Data início: ${formatDate(trip.startDate, dateFormatter)}")
            Text("Data fim: ${formatDate(trip.endDate, dateFormatter)}")
            Text("Tipo: ${tripType.label}")
            Text("Orçamento: ${currencyFormatter.format(trip.budget)}")
            Text("Total de gastos: ${currencyFormatter.format(trip.totalSpent)}")
        }
    }
}

@Composable
private fun OsmTripMapView(
    latitude: Double,
    longitude: Double,
    destination: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Configure osmdroid
    LaunchedEffect(Unit) {
        try {
            Configuration.getInstance().apply {
                userAgentValue = "AtvidadeDM"
                // Use memory cache instead of SharedPreferences to avoid deprecation
                cacheMapTileCount = 100
            }
        } catch (_: Exception) {
            // Ignore configuration errors
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(latitude, longitude, destination) {
        val geoPoint = GeoPoint(latitude, longitude)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(geoPoint)
        mapView.overlays.clear()

        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.title = destination
        marker.snippet = "Localização atual da viagem"
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

private fun formatDate(
    millis: Long,
    formatter: DateTimeFormatter
): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(formatter)
}
