package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.TripSaveResult
import com.example.atvidadedm.data.model.TripType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripFormUiState(
    val tripId: Long? = null,
    val destination: String = "",
    val type: TripType = TripType.LAZER,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budget: String = "",
    val destinationError: String? = null,
    val startDateError: String? = null,
    val endDateError: String? = null,
    val budgetError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
    val saveCompleted: Boolean = false
) {
    val isEditMode: Boolean
        get() = tripId != null && tripId > 0
}

class TripFormViewModel(
    private val tripRepository: TripRepository,
    private val userId: Long,
    private val tripId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripFormUiState(isLoading = tripId != null && tripId > 0))
    val uiState: StateFlow<TripFormUiState> = _uiState.asStateFlow()

    init {
        if (tripId != null && tripId > 0) {
            loadTrip(tripId)
        }
    }

    fun onDestinationChange(destination: String) {
        _uiState.update { it.copy(destination = destination, destinationError = null) }
    }

    fun onTypeChange(type: TripType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onStartDateSelected(date: Long) {
        _uiState.update { it.copy(startDate = date, startDateError = null) }
    }

    fun onEndDateSelected(date: Long) {
        _uiState.update { it.copy(endDate = date, endDateError = null) }
    }

    fun onBudgetChange(budget: String) {
        val normalized = budget.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.update { it.copy(budget = normalized, budgetError = null) }
    }

    fun saveTrip() {
        if (!validate()) {
            return
        }

        val state = _uiState.value
        val parsedBudget = state.budget.replace(',', '.').toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, feedbackMessage = null) }

            when (
                tripRepository.saveTrip(
                    tripId = state.tripId,
                    destination = state.destination,
                    type = state.type,
                    startDate = state.startDate!!,
                    endDate = state.endDate!!,
                    budget = parsedBudget,
                    userId = userId
                )
            ) {
                is TripSaveResult.Created -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = "Viagem cadastrada com sucesso!",
                            saveCompleted = true
                        )
                    }
                }

                TripSaveResult.Updated -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = "Viagem atualizada com sucesso!",
                            saveCompleted = true
                        )
                    }
                }

                TripSaveResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = "Não foi possível salvar a viagem"
                        )
                    }
                }
            }
        }
    }

    fun onFeedbackMessageShown() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun onSaveHandled() {
        _uiState.update { it.copy(saveCompleted = false) }
    }

    private fun loadTrip(tripId: Long) {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            if (trip == null || trip.userId != userId) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = "Viagem não encontrada"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        tripId = trip.id,
                        destination = trip.destination,
                        type = TripType.fromStorage(trip.type),
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        budget = trip.budget.toString(),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        var isValid = true

        _uiState.update {
            it.copy(
                destinationError = null,
                startDateError = null,
                endDateError = null,
                budgetError = null,
                feedbackMessage = null
            )
        }

        if (state.destination.isBlank()) {
            _uiState.update { it.copy(destinationError = "Destino é obrigatório") }
            isValid = false
        }

        if (state.startDate == null) {
            _uiState.update { it.copy(startDateError = "Data inicial é obrigatória") }
            isValid = false
        }

        if (state.endDate == null) {
            _uiState.update { it.copy(endDateError = "Data final é obrigatória") }
            isValid = false
        }

        if (state.startDate != null && state.endDate != null && state.endDate < state.startDate) {
            _uiState.update { it.copy(endDateError = "A data final deve ser maior ou igual à inicial") }
            isValid = false
        }

        val parsedBudget = state.budget.replace(',', '.').toDoubleOrNull()
        if (state.budget.isBlank()) {
            _uiState.update { it.copy(budgetError = "Orçamento é obrigatório") }
            isValid = false
        } else if (parsedBudget == null || parsedBudget <= 0.0) {
            _uiState.update { it.copy(budgetError = "Informe um orçamento válido") }
            isValid = false
        }

        return isValid
    }
}

