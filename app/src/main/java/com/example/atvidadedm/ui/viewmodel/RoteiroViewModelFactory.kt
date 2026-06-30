package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.atvidadedm.data.TripDestinationRepository
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.remote.gemini.GeminiRepository

class RoteiroViewModelFactory(
    private val tripRepository: TripRepository,
    private val tripDestinationRepository: TripDestinationRepository,
    private val geminiRepository: GeminiRepository,
    private val userId: Long,
    private val tripId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoteiroViewModel::class.java)) {
            return RoteiroViewModel(tripRepository, tripDestinationRepository, geminiRepository, userId, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

