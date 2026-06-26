package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.TripDestinationRepository
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.TripSaveResult
import com.example.atvidadedm.data.model.TripType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val tripFormDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
    "dd/MM/yyyy",
    Locale.Builder().setLanguage("pt").setRegion("BR").build()
)

data class TripFormUiState(
    val tripId: Long? = null,
    val destination: String = "",
    val type: TripType = TripType.LAZER,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budget: String = "",
    val comments: String = "",
    val finalDestination: String = "",
    val destinationError: String? = null,
    val startDateError: String? = null,
    val endDateError: String? = null,
    val budgetError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
    val saveCompleted: Boolean = false,
    val savedTripId: Long? = null,
    val itinerary: String? = null
) {
    val isEditMode: Boolean
        get() = tripId != null && tripId > 0
}

class TripFormViewModel(
    private val tripRepository: TripRepository,
    private val tripDestinationRepository: TripDestinationRepository,
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

    fun onFinalDestinationChange(destination: String) {
        _uiState.update { it.copy(finalDestination = destination, destinationError = null) }
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

    fun onCommentsChange(comments: String) {
        _uiState.update { it.copy(comments = comments) }
    }

    fun saveTrip() {
        if (!validate()) {
            return
        }

        val state = _uiState.value
        val parsedBudget = state.budget.replace(',', '.').toDoubleOrNull() ?: 0.0
        val initialDestination = state.destination.trim()
        val finalDestination = state.finalDestination.trim()
        val orderedDestinationNames = buildOrderedDestinationNames(initialDestination, finalDestination)
        val existingItinerary = if (state.isEditMode) state.itinerary else null

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, feedbackMessage = null) }

            val saveResult = tripRepository.saveTrip(
                tripId = state.tripId,
                destination = finalDestination,
                type = state.type,
                startDate = state.startDate!!,
                endDate = state.endDate!!,
                budget = parsedBudget,
                comments = state.comments,
                itinerary = existingItinerary,
                userId = userId
            )

            when (saveResult) {
                is TripSaveResult.Created -> {
                    tripDestinationRepository.replaceTripDestinations(
                        tripId = saveResult.id,
                        destinationsInOrder = orderedDestinationNames,
                        finalDestination = finalDestination
                    )
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = if (existingItinerary.isNullOrBlank()) {
                                "Viagem cadastrada com sucesso. O roteiro será gerado em seguida."
                            } else {
                                "Viagem cadastrada com sucesso!"
                            },
                            saveCompleted = true,
                            savedTripId = saveResult.id
                        )
                    }
                }

                TripSaveResult.Updated -> {
                    state.tripId?.let { updatedTripId ->
                        tripDestinationRepository.replaceTripDestinations(
                            tripId = updatedTripId,
                            destinationsInOrder = orderedDestinationNames,
                            finalDestination = finalDestination
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = "Viagem atualizada com sucesso!",
                            saveCompleted = true,
                            savedTripId = state.tripId
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

    fun onSaveHandled() {
        _uiState.update { it.copy(saveCompleted = false) }
    }

    fun onFeedbackMessageShown() {
        _uiState.update { it.copy(feedbackMessage = null) }
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
                val destinations = tripDestinationRepository.getTripDestinations(trip.id)
                val orderedNames = destinations.sortedBy { it.orderIndex }.map { it.name }
                val inferredInitial = orderedNames.firstOrNull() ?: trip.destination
                val inferredFinal = destinations.firstOrNull { it.isFinal }?.name
                    ?: orderedNames.lastOrNull()
                    ?: trip.destination

                _uiState.update {
                    it.copy(
                        tripId = trip.id,
                            destination = inferredInitial,
                        finalDestination = inferredFinal,
                        type = TripType.fromStorage(trip.type),
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        budget = trip.budget.toString(),
                        comments = trip.comments,
                            itinerary = trip.itinerary,
                        isLoading = false
                    )
                }
            }
        }
    }


    private fun buildOrderedDestinationNames(
        initialDestination: String,
        finalDestination: String
    ): List<String> {
        val result = linkedSetOf<String>()
        if (initialDestination.isNotBlank()) result.add(initialDestination)
        if (finalDestination.isNotBlank()) result.add(finalDestination)

        return result.toList()
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

        val hasInitialDestination = state.destination.isNotBlank()
        val hasFinalDestination = state.finalDestination.isNotBlank()
        if (!hasInitialDestination || !hasFinalDestination) {
            _uiState.update { it.copy(destinationError = "Informe destino inicial e destino final") }
            isValid = false
        } else if (state.destination.trim().equals(state.finalDestination.trim(), ignoreCase = true)) {
            _uiState.update { it.copy(destinationError = "Destino inicial e final devem ser diferentes") }
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

    private fun Long.toFormattedDate(): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(tripFormDateFormatter)
    }
}
