package com.example.atvidadedm.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.atvidadedm.ui.screen.ForgotPasswordScreen
import com.example.atvidadedm.ui.screen.LoginScreen
import com.example.atvidadedm.ui.screen.MenuScreen
import com.example.atvidadedm.ui.screen.RoteiroScreen
import com.example.atvidadedm.ui.screen.TripPhotosScreen
import com.example.atvidadedm.ui.screen.RegisterScreen
import com.example.atvidadedm.ui.viewmodel.SessionViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = viewModel()
    val sessionUiState by sessionViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LOGIN
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { user ->
                    sessionViewModel.startSession(user)
                    navController.navigate(AppRoutes.MENU) {
                        popUpTo(AppRoutes.LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onRegister = {
                    navController.navigate(AppRoutes.REGISTER)
                },
                onForgotPassword = {
                    navController.navigate(AppRoutes.FORGOT_PASSWORD)
                }
            )
        }

        composable(AppRoutes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.MENU) {
            val currentUser = sessionUiState.currentUser

            if (currentUser == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.MENU) {
                            inclusive = true
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                MenuScreen(
                    currentUser = currentUser,
                    onLogout = {
                        sessionViewModel.clearSession()
                        navController.navigate(AppRoutes.LOGIN) {
                            popUpTo(AppRoutes.MENU) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }

        composable(
            route = AppRoutes.ROUTEIRO_PATTERN,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { entry ->
            val currentUser = sessionUiState.currentUser
            val tripId = entry.arguments?.getLong("tripId") ?: 0L
            if (currentUser == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.ROUTEIRO_PATTERN) { inclusive = true }
                    }
                }
            } else {
                RoteiroScreen(
                    currentUserId = currentUser.id,
                    tripId = tripId,
                    onOpenRoteiro = { navController.navigate(AppRoutes.roteiro(it)) },
                    onOpenPhotos = { navController.navigate(AppRoutes.tripPhotos(it)) }
                )
            }
        }

        composable(
            route = AppRoutes.TRIP_PHOTOS_PATTERN,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { entry ->
            val currentUser = sessionUiState.currentUser
            val tripId = entry.arguments?.getLong("tripId") ?: 0L
            if (currentUser == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.TRIP_PHOTOS_PATTERN) { inclusive = true }
                    }
                }
            } else {
                TripPhotosScreen(
                    tripId = tripId,
                    onOpenRoteiro = { navController.navigate(AppRoutes.roteiro(it)) },
                    onOpenPhotos = { }
                )
            }
        }
    }
}

