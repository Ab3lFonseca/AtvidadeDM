package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.atvidadedm.data.TripRepository

class TripFormViewModelFactory(
    private val tripRepository: TripRepository,
    private val userId: Long,
    private val tripId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripFormViewModel::class.java)) {
            return TripFormViewModel(tripRepository, userId, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

