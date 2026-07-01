package com.example.atvidadedm.ui.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.atvidadedm.data.local.UserEntity
import com.example.atvidadedm.navigation.MenuRoutes
import kotlinx.coroutines.launch

private data class DrawerDestination(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    currentUser: UserEntity,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route ?: MenuRoutes.HOME
    val drawerDestinations = remember {
        listOf(
            DrawerDestination(MenuRoutes.NEW_TRIP, "Nova viagem", Icons.Default.AddLocationAlt),
            DrawerDestination(MenuRoutes.MY_TRIPS, "Minhas viagens", Icons.Default.Luggage),
            DrawerDestination(MenuRoutes.ABOUT, "Sobre", Icons.Default.Info)
        )
    }

    BackHandler {
        when {
            drawerState.isOpen -> {
                scope.launch { drawerState.close() }
            }

            currentRoute != MenuRoutes.HOME -> {
                navController.popBackStack()
            }

            else -> {
                activity?.finish()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = currentUser.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = currentUser.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                drawerDestinations.take(2).forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.title) },
                        selected = currentRoute == destination.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(destination.route) {
                                popUpTo(MenuRoutes.HOME)
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(destination.icon, contentDescription = destination.title)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))

                drawerDestinations.drop(2).forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.title) },
                        selected = currentRoute == destination.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(destination.route) {
                                popUpTo(MenuRoutes.HOME)
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(destination.icon, contentDescription = destination.title)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text("Sair")
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(currentTitle(currentRoute))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (currentRoute == MenuRoutes.HOME) {
                                    scope.launch { drawerState.open() }
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentRoute == MenuRoutes.HOME) {
                                    Icons.Default.Menu
                                } else {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                },
                                contentDescription = if (currentRoute == MenuRoutes.HOME) {
                                    "Abrir menu"
                                } else {
                                    "Voltar"
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MenuRoutes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(MenuRoutes.HOME) {
                    HomeScreen(
                        currentUser = currentUser,
                        onOpenRoteiro = { tripId -> navController.navigate(MenuRoutes.roteiro(tripId)) },
                        onOpenPhotos = { tripId ->
                            navController.navigate(MenuRoutes.tripPhotos(tripId))
                        },
                        onOpenPhotosFallback = {
                            navController.navigate(MenuRoutes.MY_TRIPS)
                        }
                    )
                }
                composable(
                    route = MenuRoutes.ROUTEIRO_PATTERN,
                    arguments = listOf(
                        navArgument("tripId") {
                            type = NavType.LongType
                        }
                    )
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                    RoteiroScreen(
                        currentUserId = currentUser.id,
                        tripId = tripId,
                        onOpenRoteiro = { navController.navigate(MenuRoutes.roteiro(tripId)) },
                        onOpenPhotos = { navController.navigate(MenuRoutes.tripPhotos(tripId)) }
                    )
                }
                composable(MenuRoutes.NEW_TRIP) {
                    NewTripScreen(
                        currentUserId = currentUser.id,
                        onBack = { navController.popBackStack() },
                        onSaved = { savedTripId ->
                            if (savedTripId != null && savedTripId > 0) {
                                navController.navigate(MenuRoutes.roteiro(savedTripId)) {
                                    popUpTo(MenuRoutes.HOME)
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(MenuRoutes.MY_TRIPS) {
                                    popUpTo(MenuRoutes.HOME)
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
                composable(MenuRoutes.MY_TRIPS) {
                    MyTripsScreen(
                        currentUserId = currentUser.id,
                        onEditTrip = { tripId ->
                            navController.navigate(MenuRoutes.editTrip(tripId))
                        },
                        onOpenRoteiro = { tripId ->
                            navController.navigate(MenuRoutes.roteiro(tripId))
                        },
                        onOpenPhotos = { tripId ->
                            navController.navigate(MenuRoutes.tripPhotos(tripId))
                        }
                    )
                }
                composable(MenuRoutes.ABOUT) {
                    AboutScreen()
                }
                composable(
                    route = MenuRoutes.EDIT_TRIP_PATTERN,
                    arguments = listOf(
                        navArgument("tripId") {
                            type = NavType.LongType
                        }
                    )
                ) { backStackEntry ->
                    NewTripScreen(
                        currentUserId = currentUser.id,
                        tripId = backStackEntry.arguments?.getLong("tripId"),
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.navigate(MenuRoutes.MY_TRIPS) {
                                popUpTo(MenuRoutes.HOME)
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = MenuRoutes.TRIP_PHOTOS_PATTERN,
                    arguments = listOf(
                        navArgument("tripId") {
                            type = NavType.LongType
                        }
                    )
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                    TripPhotosScreen(
                        tripId = tripId,
                        onOpenRoteiro = { navController.navigate(MenuRoutes.roteiro(tripId)) },
                        onOpenPhotos = { }
                    )
                }
            }
        }
    }
}

private fun currentTitle(route: String): String {
    return when {
        route == MenuRoutes.ROUTEIRO_PATTERN || route.startsWith("roteiro/") -> "Roteiro"
        route == MenuRoutes.TRIP_PHOTOS_PATTERN || route.startsWith("trip_photos/") -> "Fotos"
        route == MenuRoutes.NEW_TRIP -> "Nova viagem"
        route == MenuRoutes.MY_TRIPS -> "Minhas viagens"
        route == MenuRoutes.ABOUT -> "Sobre"
        route == MenuRoutes.EDIT_TRIP_PATTERN || route.startsWith("edit_trip/") -> "Editar viagem"
        else -> "Menu principal"
    }
}
