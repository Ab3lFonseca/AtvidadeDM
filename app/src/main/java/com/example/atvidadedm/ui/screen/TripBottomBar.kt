package com.example.atvidadedm.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

public enum class TripBottomBarDestination {
    ROUTEIRO,
    PHOTOS
}

@Composable
fun TripBottomBar(
    selectedDestination: TripBottomBarDestination? = null,
    enableRoteiroTab: Boolean = true,
    showPhotoTab: Boolean,
    onOpenRoteiro: () -> Unit,
    onOpenPhotos: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedDestination == TripBottomBarDestination.ROUTEIRO,
            enabled = enableRoteiroTab,
            onClick = onOpenRoteiro,
            icon = {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = "Roteiro"
                )
            },
            label = { Text("Roteiro") }
        )
        NavigationBarItem(
            selected = selectedDestination == TripBottomBarDestination.PHOTOS,
            enabled = showPhotoTab,
            onClick = onOpenPhotos,
            icon = {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Fotos"
                )
            },
            label = { Text("Fotos") }
        )
    }
}
