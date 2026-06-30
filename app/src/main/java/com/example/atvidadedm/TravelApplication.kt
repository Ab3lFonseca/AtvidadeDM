package com.example.atvidadedm

import android.app.Application
import com.example.atvidadedm.BuildConfig
import com.example.atvidadedm.data.LocationRepository
import com.example.atvidadedm.data.TripDestinationRepository
import com.example.atvidadedm.data.TripPhotoRepository
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.UserRepository
import com.example.atvidadedm.data.local.AppDatabase
import com.example.atvidadedm.data.remote.gemini.GeminiClient
import com.example.atvidadedm.data.remote.gemini.GeminiRepository

class TravelApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao())
    }

    val tripRepository: TripRepository by lazy {
        TripRepository(database.tripDao())
    }

    val tripPhotoRepository: TripPhotoRepository by lazy {
        TripPhotoRepository(database.tripPhotoDao(), applicationContext)
    }

    val tripDestinationRepository: TripDestinationRepository by lazy {
        TripDestinationRepository(database.tripDestinationDao())
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(applicationContext)
    }

    val geminiRepository: GeminiRepository by lazy {
        GeminiRepository(
            apiService = GeminiClient.service,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }
}
