package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.atvidadedm.data.TripPhotoRepository
import com.example.atvidadedm.data.TripRepository

class TripPhotosViewModelFactory(
    private val tripRepository: TripRepository,
    private val tripPhotoRepository: TripPhotoRepository,
    private val tripId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripPhotosViewModel::class.java)) {
            return TripPhotosViewModel(tripRepository, tripPhotoRepository, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

