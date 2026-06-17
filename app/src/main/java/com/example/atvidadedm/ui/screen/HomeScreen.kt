package com.example.atvidadedm.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.atvidadedm.TravelApplication
import com.example.atvidadedm.data.local.TripEntity
import com.example.atvidadedm.data.local.UserEntity
import com.example.atvidadedm.data.model.TripType
import com.example.atvidadedm.ui.viewmodel.HomeViewModel
import com.example.atvidadedm.ui.viewmodel.HomeViewModelFactory
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    currentUser: UserEntity,
    onOpenRoteiro: (Long) -> Unit,
    onOpenPhotos: (Long) -> Unit,
    onOpenPhotosFallback: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as TravelApplication
    val viewModel: HomeViewModel = viewModel(
        factory = remember(currentUser.id) {
            HomeViewModelFactory(
                tripRepository = application.tripRepository,
                locationRepository = application.locationRepository,
                userId = currentUser.id
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onPermissionResult
    )

    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.onPermissionResult(true)
        } else if (!uiState.permissionRequested) {
            viewModel.markPermissionRequested()
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        bottomBar = {
                TripBottomBar(
                    selectedDestination = null,
                    showPhotoTab = true, // habilita a aba Fotos sempre; o fallback abre Minhas viagens
                    onOpenRoteiro = {
                        uiState.activeTrip?.let { activeTrip ->
                            onOpenRoteiro(activeTrip.id)
                        } ?: onOpenRoteiro(0L)
                    },
                    onOpenPhotos = {
                        uiState.activeTrip?.let { activeTrip ->
                            onOpenPhotos(activeTrip.id)
                        } ?: onOpenPhotosFallback()
                    }
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
                text = "Quando houver uma viagem em andamento, o mapa abaixo mostrará sua localização atual.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }

                !hasLocationPermission -> {
                    Text(
                        text = "Permita o acesso a localizacao para buscar sua viagem atual automaticamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = {
                        viewModel.markPermissionRequested()
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) {
                        Text("Permitir localizacao")
                    }
                }

                else -> {
                    uiState.currentCity?.let { city ->
                        Text(
                            text = "Cidade atual: $city",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    uiState.activeTrip?.let { trip ->
                        CurrentTripMapCard(
                            trip = trip,
                            latitude = uiState.currentLatitude,
                            longitude = uiState.currentLongitude
                        )
                    }

                    uiState.activeTrip?.let { trip ->
                        ActiveTripCard(trip = trip)
                    }

                    uiState.message?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(onClick = viewModel::refreshCurrentTripFromLocation) {
                        Text("Atualizar localizacao")
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTripMapCard(
    trip: TripEntity,
    latitude: Double?,
    longitude: Double?
) {
    if (latitude == null || longitude == null) {
        return
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mapa da viagem atual", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Local atual da viagem para ${trip.destination}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(top = 4.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            "https://localhost/",
                            tripMapHtml(latitude, longitude),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
            Text("Data inicio: ${formatDate(trip.startDate, dateFormatter)}")
            Text("Data fim: ${formatDate(trip.endDate, dateFormatter)}")
            Text("Tipo: ${tripType.label}")
            Text("Orcamento: ${currencyFormatter.format(trip.budget)}")
            Text("Total de gastos: ${currencyFormatter.format(trip.totalSpent)}")
        }
    }
}

private fun tripMapHtml(latitude: Double, longitude: Double): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <style>
                html, body, #map {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                }
            </style>
            <link
                rel="stylesheet"
                href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
            />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        </head>
        <body>
            <div id="map"></div>
            <script>
                const map = L.map('map').setView([$latitude, $longitude], 15);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; OpenStreetMap contributors'
                }).addTo(map);
                L.marker([$latitude, $longitude])
                    .addTo(map)
                    .bindPopup('Localização atual')
                    .openPopup();
            </script>
        </body>
        </html>
    """.trimIndent()
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
