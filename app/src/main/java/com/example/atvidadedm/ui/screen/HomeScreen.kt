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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.atvidadedm.ui.viewmodel.MapDestinationPoint
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: UserEntity,
    onOpenRoteiro: (Long) -> Unit,
    onOpenPhotos: (Long) -> Unit,
    onOpenPhotosFallback: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TravelApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            tripRepository = app.tripRepository,
            tripDestinationRepository = app.tripDestinationRepository,
            locationRepository = app.locationRepository,
            userId = currentUser.id
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val activeTrip = uiState.activeTrip
    val isCurrentDaySelection = uiState.selectedTripId == null
    var isMapExpanded by remember { mutableStateOf(false) }
    var isTripSelectorExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.onPermissionResult(true)
        } else {
            viewModel.refreshTripData()
            viewModel.markPermissionRequested()
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        try {
            Configuration.getInstance().apply {
                userAgentValue = "AtvidadeDM"
                cacheMapTileCount = 500
                tileDownloadThreads = 5
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        bottomBar = {
            TripBottomBar(
                selectedDestination = null,
                enableRoteiroTab = activeTrip != null,
                showPhotoTab = activeTrip != null,
                onOpenRoteiro = {
                    activeTrip?.let { onOpenRoteiro(it.id) } ?: onOpenPhotosFallback()
                },
                onOpenPhotos = {
                    activeTrip?.let { onOpenPhotos(it.id) } ?: onOpenPhotosFallback()
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
                text = "O mapa OpenStreetMap aparece na tela inicial mostrando o destino final da viagem selecionada.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (uiState.availableTrips.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = isTripSelectorExpanded,
                    onExpandedChange = { isTripSelectorExpanded = !isTripSelectorExpanded }
                ) {
                    val selectedTripLabel = activeTrip?.let {
                        "${it.destination} (${formatTripPeriod(it.startDate, it.endDate)})"
                    } ?: "Viagem atual do dia"

                    OutlinedTextField(
                        value = selectedTripLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selecionar viagem") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTripSelectorExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    DropdownMenu(
                        expanded = isTripSelectorExpanded,
                        onDismissRequest = { isTripSelectorExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Viagem atual do dia") },
                            onClick = {
                                isTripSelectorExpanded = false
                                viewModel.onTripSelectionChange(null)
                            }
                        )

                        uiState.availableTrips.forEach { trip ->
                            DropdownMenuItem(
                                text = {
                                    Text("${trip.destination} (${formatTripPeriod(trip.startDate, trip.endDate)})")
                                },
                                onClick = {
                                    isTripSelectorExpanded = false
                                    viewModel.onTripSelectionChange(trip.id)
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (activeTrip != null) {
                ActiveTripCard(activeTrip)
                ItineraryPreviewCard(
                    itinerary = activeTrip.itinerary,
                    onOpenRoteiro = { onOpenRoteiro(activeTrip.id) }
                )
                CurrentTripMapCard(
                    trip = activeTrip,
                    currentCity = uiState.currentCity,
                    latitude = uiState.mapLatitude,
                    longitude = uiState.mapLongitude,
                    isMapLoading = uiState.isMapLoading,
                    mapLabel = uiState.mapDestinationLabel,
                    mapPoints = uiState.mapPoints,
                    tripDestinations = uiState.tripDestinations,
                    onExpandMap = { isMapExpanded = true }
                )
            } else if (isCurrentDaySelection) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Sem viagens no dia", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Nenhuma viagem foi encontrada para hoje.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.refreshTripData() }) {
                            Text("Atualizar")
                        }
                    }
                }
            }

            uiState.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (isMapExpanded) {
        ExpandedMapDialog(
            latitude = uiState.mapLatitude,
            longitude = uiState.mapLongitude,
            destination = uiState.mapDestinationLabel ?: activeTrip?.destination ?: "Viagem atual",
            mapPoints = uiState.mapPoints,
            onDismiss = { isMapExpanded = false }
        )
    }
}

@Composable
private fun ItineraryPreviewCard(
    itinerary: String?,
    onOpenRoteiro: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Roteiro da viagem", style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (itinerary.isNullOrBlank()) {
                    "Roteiro ainda não gerado. Toque em 'Abrir roteiro' para gerar pela IA."
                } else {
                    itinerary.take(220) + if (itinerary.length > 220) "..." else ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onOpenRoteiro,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir roteiro")
            }
        }
    }
}

@Composable
private fun CurrentTripMapCard(
    trip: TripEntity?,
    currentCity: String?,
    latitude: Double?,
    longitude: Double?,
    isMapLoading: Boolean,
    mapLabel: String?,
    mapPoints: List<MapDestinationPoint>,
    tripDestinations: List<String>,
    onExpandMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mapa da viagem", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when {
                    trip != null -> "Visualizando a viagem com destino final em ${trip.destination}"
                    !currentCity.isNullOrBlank() -> "Localização atual em $currentCity"
                    else -> "Aguardando localização atual"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (tripDestinations.isNotEmpty()) {
                val finalDestination = tripDestinations.lastOrNull().orEmpty()
                Text(
                    text = "Destino final: $finalDestination",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (latitude == null || longitude == null) {
                if (isMapLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            text = if (trip != null) {
                                "Carregando mapa e trajeto da viagem..."
                            } else {
                                "Obtendo localização atual para exibir o mapa..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Ainda não foi possível localizar o trajeto. O mapa aparecerá assim que as coordenadas forem resolvidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                OsmTripMapView(
                    latitude = latitude,
                    longitude = longitude,
                    destination = mapLabel ?: trip?.destination ?: currentCity ?: "Localização atual",
                    mapPoints = mapPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )

                Button(
                    onClick = onExpandMap,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Expandir mapa")
                }
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
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
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
private fun ExpandedMapDialog(
    latitude: Double?,
    longitude: Double?,
    destination: String,
    mapPoints: List<MapDestinationPoint>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (latitude != null && longitude != null) {
                OsmTripMapView(
                    latitude = latitude,
                    longitude = longitude,
                    destination = destination,
                    mapPoints = mapPoints,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sem coordenadas para exibir o mapa.")
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar mapa"
                )
            }
        }
    }
}

@Composable
private fun OsmTripMapView(
    latitude: Double,
    longitude: Double,
    destination: String,
    mapPoints: List<MapDestinationPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    LaunchedEffect(latitude, longitude, destination, mapPoints) {
        val geoPoint = GeoPoint(latitude, longitude)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(geoPoint)
        mapView.overlays.clear()

        if (mapPoints.isNotEmpty()) {
            mapPoints.forEachIndexed { index, point ->
                val marker = Marker(mapView)
                marker.position = GeoPoint(point.latitude, point.longitude)
                marker.title = when {
                    index == 0 -> "Inicio: ${point.name}"
                    point.isFinal -> "Destino final: ${point.name}"
                    else -> "Parada ${index + 1}: ${point.name}"
                }
                marker.snippet = when {
                    index == 0 -> "Ponto inicial da viagem"
                    point.isFinal -> "Chegada da viagem"
                    else -> "Ponto intermediario"
                }
                mapView.overlays.add(marker)
            }

            val centerPoint = mapPoints.firstOrNull { it.isFinal } ?: mapPoints.last()
            mapView.controller.setCenter(GeoPoint(centerPoint.latitude, centerPoint.longitude))
        } else {
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.title = destination
            marker.snippet = "Localizacao atual da viagem"
            mapView.overlays.add(marker)
        }
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

private fun formatTripPeriod(startMillis: Long, endMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())
    return "${formatDate(startMillis, formatter)} até ${formatDate(endMillis, formatter)}"
}
