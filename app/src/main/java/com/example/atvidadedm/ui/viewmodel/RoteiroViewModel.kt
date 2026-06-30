package com.example.atvidadedm.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.TripDestinationRepository
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.model.TripType
import com.example.atvidadedm.data.remote.gemini.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val roteiroDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
    "dd/MM/yyyy",
    Locale.Builder().setLanguage("pt").setRegion("BR").build()
)

data class RoteiroUiState(
    val tripId: Long? = null,
    val destination: String = "",
    val type: TripType = TripType.LAZER,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val interests: String = "",
    val comments: String = "",
    val travelStyle: String = "Equilibrado",
    val budget: String = "",
    val itinerary: String? = null,
    val message: String? = null,
    val isLoadingTrip: Boolean = false,
    val isGenerating: Boolean = false,
    val initialDestination: String = "",
    val finalDestination: String = "",
    val destinationCycle: String = ""
) {

    val canGenerate: Boolean
        get() = destination.isNotBlank() && startDate != null && endDate != null && budget.isNotBlank()
}

class RoteiroViewModel(
    private val tripRepository: TripRepository,
    private val tripDestinationRepository: TripDestinationRepository,
    private val geminiRepository: GeminiRepository,
    private val userId: Long,
    tripId: Long?
) : ViewModel() {
    private companion object {
        const val TAG = "RoteiroViewModel"
    }


    private val _uiState = MutableStateFlow(RoteiroUiState(isLoadingTrip = tripId != null && tripId > 0))
    val uiState: StateFlow<RoteiroUiState> = _uiState.asStateFlow()

    init {
        if (tripId != null && tripId > 0) {
            loadTrip(tripId)
        }
    }

    fun onDestinationChange(value: String) {
        _uiState.update { it.copy(destination = value, message = null) }
    }

    fun onTypeChange(value: TripType) {
        _uiState.update { it.copy(type = value) }
    }

    fun onStartDateSelected(date: Long) {
        _uiState.update { it.copy(startDate = date, message = null) }
    }

    fun onEndDateSelected(date: Long) {
        _uiState.update { it.copy(endDate = date, message = null) }
    }

    fun onInterestsChange(value: String) {
        _uiState.update { it.copy(interests = value, message = null) }
    }

    fun onCommentsChange(value: String) {
        _uiState.update { it.copy(comments = value, message = null) }
    }

    fun onTravelStyleChange(value: String) {
        _uiState.update { it.copy(travelStyle = value, message = null) }
    }

    fun onBudgetChange(value: String) {
        val normalized = value.filter { it.isDigit() || it == ',' || it == '.' }
        _uiState.update { it.copy(budget = normalized, message = null) }
    }


    fun generateItinerary() {
        val state = _uiState.value
        if (!state.canGenerate) {
            _uiState.update {
                it.copy(message = "Preencha destino, período e orçamento para gerar o roteiro.")
            }
            return
        }

        val prompt = buildPrompt(state)
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, message = null) }
            Log.d(TAG, "Gerando roteiro manual. tripId=${state.tripId}, destination=${state.destination.trim()}")
            val result = geminiRepository.generateItinerary(prompt)
            result.fold(
                onSuccess = { itinerary ->
                    state.tripId?.let { id ->
                        tripRepository.updateItinerary(tripId = id, itinerary = itinerary)
                        Log.d(TAG, "Roteiro salvo no banco. tripId=$id, textLength=${itinerary.length}")
                    }
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            itinerary = itinerary,
                            message = "Roteiro em texto gerado com sucesso!"
                        )
                    }
                },
                onFailure = { throwable ->
                    Log.e(TAG, "Erro ao gerar roteiro manual: ${throwable.message}", throwable)
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            message = throwable.message ?: "Não foi possível gerar o roteiro."
                        )
                    }
                }
            )
        }
    }

    private fun loadTrip(tripId: Long) {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            if (trip == null || trip.userId != userId) {
                _uiState.update {
                    it.copy(
                        isLoadingTrip = false,
                        message = "Viagem não encontrada. Você ainda pode preencher os dados manualmente."
                    )
                }
                return@launch
            }

            val tripDestinations = tripDestinationRepository.getTripDestinations(trip.id)
            val orderedDestinations = tripDestinations.sortedBy { it.orderIndex }
            val initialDestination = orderedDestinations.firstOrNull()?.name ?: trip.destination
            val finalDestination = orderedDestinations.lastOrNull()?.name ?: trip.destination
            val destinationCycle = orderedDestinations.joinToString(" -> ") { it.name }

            _uiState.update {
                it.copy(
                    tripId = trip.id,
                    destination = trip.destination,
                    type = TripType.fromStorage(trip.type),
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    budget = trip.budget.toString(),
                    comments = trip.comments,
                        itinerary = trip.itinerary,
                    initialDestination = initialDestination,
                    finalDestination = finalDestination,
                    destinationCycle = destinationCycle,
                    isLoadingTrip = false
                )
            }
        }
    }

    private fun buildPrompt(state: RoteiroUiState): String {
        val start = state.startDate?.toLocalDateString().orEmpty()
        val end = state.endDate?.toLocalDateString().orEmpty()
        val interests = state.interests.ifBlank { "sem interesses específicos informados" }
        val comments = state.comments.ifBlank { "sem comentários adicionais" }
        val style = state.travelStyle.ifBlank { "Equilibrado" }
        val budget = state.budget.replace(',', '.')
        val initialDestination = state.initialDestination.ifBlank { state.destination.trim() }
        val finalDestination = state.finalDestination.ifBlank { state.destination.trim() }
        val days = state.startDate?.let { startMillis ->
            state.endDate?.let { endMillis ->
                val startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val endDate = Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC).toLocalDate()
                kotlin.math.max(1, java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1)
            }
        } ?: 1

        return """
            Você é um especialista em turismo e vai criar um roteiro turístico personalizado em português do Brasil.

            Dados da viagem:
            - Destino inicial: $initialDestination
            - Destino final: $finalDestination
            - Tipo de viagem: ${state.type.label}
            - Período: $start até $end
            - Duração estimada: $days dias
            - Interesses: $interests
            - Comentários adicionais: $comments
            - Estilo da viagem: $style
            - Orçamento total: R$ $budget

            Regras da resposta:
            - Crie um roteiro prático e personalizado para esse destino.
            - Organize a resposta por dias, com sugestões de manhã, tarde e noite.
            - Inclua sugestões de atrações, alimentação e deslocamento quando fizer sentido.
            - Estruture a viagem saindo do destino inicial e chegando ao destino final.
            - Recomende locais concretos para visitar nesse destino (nomes de lugares e regiao/bairro quando possivel).
            - Em cada dia, destaque os principais lugares para ir e por que vale a pena.
            - Considere o orçamento informado e o estilo da viagem.
            - Considere explicitamente os interesses e comentários adicionais informados pelo usuário.
            - Use linguagem objetiva, clara e útil para um viajante.
            - Finalize com dicas rápidas de economia e segurança.
            - Não mencione que você é uma IA.
        """.trimIndent()
    }

    private fun Long.toLocalDateString(): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(roteiroDateFormatter)
    }
}

