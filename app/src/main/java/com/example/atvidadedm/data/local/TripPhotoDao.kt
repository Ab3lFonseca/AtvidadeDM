package com.example.atvidadedm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPhotoDao {
    @Insert
    suspend fun insert(photo: TripPhotoEntity): Long

    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId ORDER BY created_at DESC")
    fun getPhotosByTripId(tripId: Long): Flow<List<TripPhotoEntity>>
}
