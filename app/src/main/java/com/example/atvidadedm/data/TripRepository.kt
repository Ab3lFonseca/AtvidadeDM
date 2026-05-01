package com.example.atvidadedm.data

import com.example.atvidadedm.data.local.TripDao
import com.example.atvidadedm.data.local.TripEntity
import com.example.atvidadedm.data.model.TripType
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao
) {
    fun getTripsByUserId(userId: Long): Flow<List<TripEntity>> {
        return tripDao.getTripsByUserId(userId)
    }

    suspend fun getTripById(tripId: Long): TripEntity? {
        return tripDao.getById(tripId)
    }

    suspend fun saveTrip(
        tripId: Long?,
        destination: String,
        type: TripType,
        startDate: Long,
        endDate: Long,
        budget: Double,
        userId: Long
    ): TripSaveResult {
        val trip = TripEntity(
            id = tripId ?: 0,
            destination = destination.trim(),
            type = type.storageValue,
            startDate = startDate,
            endDate = endDate,
            budget = budget,
            userId = userId
        )

        return if (tripId != null && tripId > 0) {
            tripDao.update(trip)
            TripSaveResult.Updated
        } else {
            val createdId = tripDao.insert(trip)
            if (createdId > 0) {
                TripSaveResult.Created(createdId)
            } else {
                TripSaveResult.Failure
            }
        }
    }

    suspend fun deleteTrip(tripId: Long) {
        tripDao.deleteById(tripId)
    }
}

sealed interface TripSaveResult {
    data class Created(val id: Long) : TripSaveResult
    data object Updated : TripSaveResult
    data object Failure : TripSaveResult
}

