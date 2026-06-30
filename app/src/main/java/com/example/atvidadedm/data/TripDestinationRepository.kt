package com.example.atvidadedm.data

import com.example.atvidadedm.data.local.TripDestinationDao
import com.example.atvidadedm.data.local.TripDestinationEntity

class TripDestinationRepository(
    private val tripDestinationDao: TripDestinationDao
) {
    suspend fun getTripDestinations(tripId: Long): List<TripDestinationEntity> {
        return tripDestinationDao.getByTripId(tripId)
    }

    suspend fun replaceTripDestinations(
        tripId: Long,
        destinationsInOrder: List<String>,
        finalDestination: String?
    ) {
        val normalized = destinationsInOrder
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (normalized.isEmpty()) {
            return
        }

        val finalNormalized = finalDestination?.trim().orEmpty().ifBlank { null }
        val withFinal = if (finalNormalized != null && normalized.lastOrNull() != finalNormalized) {
            normalized + finalNormalized
        } else {
            normalized
        }

        tripDestinationDao.deleteByTripId(tripId)
        tripDestinationDao.insertAll(
            withFinal.mapIndexed { index, name ->
                TripDestinationEntity(
                    tripId = tripId,
                    name = name,
                    orderIndex = index,
                    isFinal = finalNormalized != null && name == finalNormalized && index == withFinal.lastIndex
                )
            }
        )
    }
}

