package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.LocationLookupResult
import com.example.atvidadedm.data.LocationRepository
import com.example.atvidadedm.data.DestinationCoordinates
import com.example.atvidadedm.data.TripDestinationRepository
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.local.TripEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneOffset

data class HomeUiState(
	val isLoading: Boolean = false,
	val permissionGranted: Boolean = false,
	val permissionRequested: Boolean = false,
	val availableTrips: List<TripEntity> = emptyList(),
	val selectedTripId: Long? = null,
	val currentCity: String? = null,
	val currentLatitude: Double? = null,
	val currentLongitude: Double? = null,
	val mapLatitude: Double? = null,
	val mapLongitude: Double? = null,
	val isMapLoading: Boolean = false,
	val mapDestinationLabel: String? = null,
	val mapPoints: List<MapDestinationPoint> = emptyList(),
	val tripDestinations: List<String> = emptyList(),
	val activeTrip: TripEntity? = null,
	val message: String? = null
)

data class MapDestinationPoint(
	val name: String,
	val latitude: Double,
	val longitude: Double,
	val isFinal: Boolean
)

class HomeViewModel(
	private val tripRepository: TripRepository,
	private val tripDestinationRepository: TripDestinationRepository,
	private val locationRepository: LocationRepository,
	private val userId: Long
) : ViewModel() {

	private val _uiState = MutableStateFlow(HomeUiState())
	val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

	init {
		observeTrips()
	}

	fun markPermissionRequested() {
		_uiState.update { it.copy(permissionRequested = true) }
	}

	fun onPermissionResult(granted: Boolean) {
		_uiState.update {
			it.copy(
				isLoading = false,
				permissionGranted = granted,
				permissionRequested = true,
				message = null
			)
		}

		if (granted) {
			refreshCurrentTripFromLocation()
		}
	}

	fun refreshTripData() {
		loadPreferredTrip()
	}

	fun onTripSelectionChange(tripId: Long?) {
		_uiState.update { it.copy(selectedTripId = tripId, message = null) }
		loadPreferredTrip()
	}

	fun refreshCurrentTripFromLocation() {
		loadPreferredTrip()

		if (!_uiState.value.permissionGranted) {
			return
		}

		viewModelScope.launch {
			val result = try {
				locationRepository.getCurrentCity()
			} catch (t: Throwable) {
				_uiState.update {
					it.copy(message = "Erro ao obter localizacao: ${t.message}")
				}
				return@launch
			}

			when (result) {
				is LocationLookupResult.Success -> {
					_uiState.update {
						it.copy(
							currentCity = result.city,
							currentLatitude = result.latitude,
							currentLongitude = result.longitude,
							mapLatitude = if (it.activeTrip == null) {
								it.mapLatitude ?: result.latitude
							} else {
								it.mapLatitude
							},
							mapLongitude = if (it.activeTrip == null) {
								it.mapLongitude ?: result.longitude
							} else {
								it.mapLongitude
							},
							mapDestinationLabel = if (it.activeTrip == null) {
								it.mapDestinationLabel ?: result.city ?: "Localização atual"
							} else {
								it.mapDestinationLabel
							},
							message = null
						)
					}
				}

				LocationLookupResult.PermissionDenied -> {
					_uiState.update {
						it.copy(message = "Permissao de localizacao nao concedida.")
					}
				}

				LocationLookupResult.LocationUnavailable -> {
					_uiState.update {
						it.copy(message = "Nao foi possivel obter a localizacao atual.")
					}
				}
			}
		}
	}

	private fun observeTrips() {
		viewModelScope.launch {
			tripRepository.getTripsByUserId(userId).collect { trips ->
				val currentSelectedId = _uiState.value.selectedTripId
				_uiState.update { it.copy(availableTrips = trips) }

				if (currentSelectedId != null && trips.none { it.id == currentSelectedId }) {
					_uiState.update {
						it.copy(
							selectedTripId = null,
							message = "A viagem selecionada não está mais disponível."
						)
					}
					loadCurrentTripByDate()
				}
			}
		}
	}

	private fun loadPreferredTrip() {
		val selectedTripId = _uiState.value.selectedTripId
		if (selectedTripId != null) {
			loadTripById(selectedTripId)
		} else {
			loadCurrentTripByDate()
		}
	}

	private fun loadTripById(tripId: Long) {
		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true) }
			val trip = tripRepository.getTripById(tripId)?.takeIf { it.userId == userId }

			_uiState.update {
				it.copy(
					isLoading = false,
					activeTrip = trip,
					isMapLoading = trip != null,
					message = if (trip == null) "Viagem selecionada não encontrada." else null
				)
			}

			if (trip != null) {
				enrichMapWithTripDestinations(trip)
			}
		}
	}

	private fun loadCurrentTripByDate(message: String? = null) {
		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true, message = null) }
			val now = currentUtcDateStartMillis()
			val trip = tripRepository.getActiveTripByDate(userId = userId, currentDate = now)
			_uiState.update {
				it.copy(
					isLoading = false,
					mapLatitude = null,
					mapLongitude = null,
					isMapLoading = false,
					mapDestinationLabel = if (trip == null) {
						null
					} else {
						trip.destination
					},
					mapPoints = emptyList(),
					tripDestinations = emptyList(),
					activeTrip = trip,
					message = message
				)
			}

			if (trip != null) {
				enrichMapWithTripDestinations(trip)
			}
		}
	}

	private fun enrichMapWithTripDestinations(trip: TripEntity) {
		viewModelScope.launch {
			val tripDestinations = tripDestinationRepository.getTripDestinations(trip.id)
			val orderedDestinations = tripDestinations
				.sortedBy { it.orderIndex }
				.map { it.name.trim() }
				.filter { it.isNotBlank() }
				.distinct()

			val finalDestination = trip.destination.trim().ifBlank {
				orderedDestinations.lastOrNull().orEmpty()
			}

			val effectiveDestinations = listOfNotNull(finalDestination.ifBlank { null })

			if (!isTripStillActive(trip.id)) {
				return@launch
			}

			_uiState.update {
				it.copy(
					isMapLoading = true,
					mapDestinationLabel = finalDestination.ifBlank { trip.destination },
					tripDestinations = effectiveDestinations,
					mapPoints = emptyList()
				)
			}

			val coords = resolveTripCoordinates(finalDestination)

			if (!isTripStillActive(trip.id)) {
				return@launch
			}

			if (coords != null) {
				_uiState.update {
					it.copy(
						mapLatitude = coords.latitude,
						mapLongitude = coords.longitude,
						mapDestinationLabel = finalDestination,
						mapPoints = listOf(
							MapDestinationPoint(
								name = finalDestination,
								latitude = coords.latitude,
								longitude = coords.longitude,
								isFinal = true
							)
						),
						isMapLoading = false
					)
				}
			} else {
				// Fallback to current location if destination is not found
				_uiState.update {
					it.copy(
						mapLatitude = it.currentLatitude,
						mapLongitude = it.currentLongitude,
						mapDestinationLabel = it.currentCity ?: "Localização atual",
						isMapLoading = false,
						message = "Não foi possível localizar '${finalDestination}'. Mostrando sua posição atual."
					)
				}
			}
		}
	}

	private suspend fun resolveTripCoordinates(destination: String) =
		locationRepository.getCoordinatesForDestination(destination)

	private fun isTripStillActive(tripId: Long): Boolean {
		return _uiState.value.activeTrip?.id == tripId
	}

	private fun currentUtcDateStartMillis(): Long {
		return Instant.now()
			.atZone(ZoneOffset.UTC)
			.toLocalDate()
			.atStartOfDay(ZoneOffset.UTC)
			.toInstant()
			.toEpochMilli()
	}
}
