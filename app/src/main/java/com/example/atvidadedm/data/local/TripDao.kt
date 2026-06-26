package com.example.atvidadedm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY start_date ASC, destination ASC")
    fun getTripsByUserId(userId: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getById(tripId: Long): TripEntity?

    @Query(
        """
        SELECT * FROM trips
        WHERE userId = :userId
          AND LOWER(TRIM(destination)) = LOWER(TRIM(:city))
          AND :currentDate BETWEEN start_date AND end_date
        ORDER BY start_date DESC
        LIMIT 1
        """
    )
    suspend fun getActiveTripByCityAndDate(
        userId: Long,
        city: String,
        currentDate: Long
    ): TripEntity?

    @Query(
        """
        SELECT * FROM trips
        WHERE userId = :userId
          AND :currentDate BETWEEN start_date AND end_date
        ORDER BY start_date DESC
        LIMIT 1
        """
    )
    suspend fun getActiveTripByDate(
        userId: Long,
        currentDate: Long
    ): TripEntity?

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteById(tripId: Long)

    @Query("UPDATE trips SET itinerary = :itinerary WHERE id = :tripId")
    suspend fun updateItinerary(tripId: Long, itinerary: String)
}

