package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.atvidadedm.data.LocationRepository
import com.example.atvidadedm.data.TripDestinationRepository
import com.example.atvidadedm.data.TripRepository

class HomeViewModelFactory(
	private val tripRepository: TripRepository,
	private val tripDestinationRepository: TripDestinationRepository,
	private val locationRepository: LocationRepository,
	private val userId: Long
) : ViewModelProvider.Factory {
	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
			return HomeViewModel(tripRepository, tripDestinationRepository, locationRepository, userId) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
	}
}
