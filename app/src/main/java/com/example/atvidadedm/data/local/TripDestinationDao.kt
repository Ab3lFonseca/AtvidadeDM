package com.example.atvidadedm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TripDestinationDao {
    @Query("SELECT * FROM trip_destinations WHERE tripId = :tripId ORDER BY order_index ASC, id ASC")
    suspend fun getByTripId(tripId: Long): List<TripDestinationEntity>

    @Insert
    suspend fun insertAll(destinations: List<TripDestinationEntity>)

    @Query("DELETE FROM trip_destinations WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: Long)
}

