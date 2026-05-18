package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.LocationLookupResult
import com.example.atvidadedm.data.LocationRepository
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.local.TripEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
	val isLoading: Boolean = false,
	val permissionGranted: Boolean = false,
	val permissionRequested: Boolean = false,
	val currentCity: String? = null,
	val activeTrip: TripEntity? = null,
	val message: String? = null
)

class HomeViewModel(
	private val tripRepository: TripRepository,
	private val locationRepository: LocationRepository,
	private val userId: Long
) : ViewModel() {

	private val _uiState = MutableStateFlow(HomeUiState())
	val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

	fun markPermissionRequested() {
		_uiState.update { it.copy(permissionRequested = true) }
	}

	fun onPermissionResult(granted: Boolean) {
		_uiState.update {
			it.copy(
				permissionGranted = granted,
				permissionRequested = true,
				message = if (granted) null else "Permissao de localizacao negada."
			)
		}

		if (granted) {
			refreshCurrentTripFromLocation()
		}
	}

	fun refreshCurrentTripFromLocation() {
		if (!_uiState.value.permissionGranted) {
			return
		}

		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true, message = null) }

			when (val result = locationRepository.getCurrentCity()) {
				is LocationLookupResult.Success -> {
					val now = System.currentTimeMillis()
					val trip = tripRepository.getActiveTripByCityAndDate(
						userId = userId,
						city = result.city,
						currentDate = now
					)

					_uiState.update {
						it.copy(
							isLoading = false,
							currentCity = result.city,
							activeTrip = trip,
							message = if (trip == null) {
								"Nenhuma viagem em andamento para ${result.city}."
							} else {
								null
							}
						)
					}
				}

				LocationLookupResult.PermissionDenied -> {
					_uiState.update {
						it.copy(
							isLoading = false,
							message = "Permissao de localizacao nao concedida."
						)
					}
				}

				LocationLookupResult.LocationUnavailable -> {
					_uiState.update {
						it.copy(
							isLoading = false,
							activeTrip = null,
							message = "Nao foi possivel obter a localizacao atual."
						)
					}
				}

				LocationLookupResult.CityNotFound -> {
					_uiState.update {
						it.copy(
							isLoading = false,
							activeTrip = null,
							message = "Nao foi possivel identificar a cidade atual."
						)
					}
				}
			}
		}
	}
}

