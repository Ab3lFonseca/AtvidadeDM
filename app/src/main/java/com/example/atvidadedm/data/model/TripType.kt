package com.example.atvidadedm.data.model

enum class TripType(
    val storageValue: String,
    val label: String
) {
    LAZER(
        storageValue = "LAZER",
        label = "Lazer"
    ),
    NEGOCIOS(
        storageValue = "NEGOCIOS",
        label = "Negócios"
    );

    companion object {
        fun fromStorage(value: String): TripType {
            return entries.firstOrNull { it.storageValue == value } ?: LAZER
        }
    }
}

