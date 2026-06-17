package com.example.atvidadedm

import android.app.Application
import com.example.atvidadedm.data.LocationRepository
import com.example.atvidadedm.data.TripPhotoRepository
import com.example.atvidadedm.data.TripRepository
import com.example.atvidadedm.data.UserRepository
import com.example.atvidadedm.data.local.AppDatabase

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

    val locationRepository: LocationRepository by lazy {
        LocationRepository(applicationContext)
    }
}
