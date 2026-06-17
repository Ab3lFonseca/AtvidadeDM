package com.example.atvidadedm.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.PhotoCaptureTarget
import com.example.atvidadedm.data.TripPhotoRepository
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.local.TripEntity
import com.example.atvidadedm.data.local.TripPhotoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripPhotosUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val trip: TripEntity? = null,
    val photos: List<TripPhotoEntity> = emptyList(),
    val message: String? = null
)

class TripPhotosViewModel(
    private val tripRepository: TripRepository,
    private val tripPhotoRepository: TripPhotoRepository,
    private val tripId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripPhotosUiState())
    val uiState: StateFlow<TripPhotosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            if (trip == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Viagem nao encontrada."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    trip = trip,
                    isLoading = false,
                    message = null
                )
            }

            tripPhotoRepository.observePhotosByTripId(tripId).collectLatest { photos ->
                _uiState.update { state ->
                    state.copy(photos = photos)
                }
            }
        }
    }

    fun createCameraCaptureTarget(): PhotoCaptureTarget {
        return tripPhotoRepository.createCameraCaptureTarget(tripId)
    }

    fun addGalleryPhoto(sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                tripPhotoRepository.addGalleryPhoto(tripId, sourceUri)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, message = "Foto adicionada com sucesso.") }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Nao foi possivel adicionar a foto."
                    )
                }
            }
        }
    }

    fun addCapturedPhoto(photoUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                tripPhotoRepository.addCapturedPhoto(tripId, photoUri)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, message = "Foto da camera vinculada com sucesso.") }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Nao foi possivel salvar a foto da camera."
                    )
                }
            }
        }
    }

    fun discardCameraTarget(target: PhotoCaptureTarget) {
        tripPhotoRepository.deleteCaptureTarget(target)
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
