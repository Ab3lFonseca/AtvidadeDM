package com.example.atvidadedm.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "destination")
    val destination: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    @ColumnInfo(name = "end_date")
    val endDate: Long,
    @ColumnInfo(name = "budget")
    val budget: Double,
    @ColumnInfo(name = "total_spent")
    val totalSpent: Double = 0.0,
    @ColumnInfo(name = "userId")
    val userId: Long,
    @ColumnInfo(name = "comments")
    val comments: String = "",
    @ColumnInfo(name = "itinerary")
    val itinerary: String? = null
)

