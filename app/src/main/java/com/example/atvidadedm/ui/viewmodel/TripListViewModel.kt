package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.local.TripEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripListUiState(
    val trips: List<TripEntity> = emptyList(),
    val isLoading: Boolean = true,
    val feedbackMessage: String? = null
)

class TripListViewModel(
    private val tripRepository: TripRepository,
    private val userId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListUiState())
    val uiState: StateFlow<TripListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tripRepository.getTripsByUserId(userId).collect { trips ->
                _uiState.update {
                    it.copy(
                        trips = trips,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            tripRepository.deleteTrip(tripId)
            _uiState.update { it.copy(feedbackMessage = "Viagem excluída com sucesso!") }
        }
    }

    fun onFeedbackMessageShown() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}

