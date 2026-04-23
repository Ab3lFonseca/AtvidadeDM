package com.example.atvidadedm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.atvidadedm.ui.screen.ForgotPasswordScreen
import com.example.atvidadedm.ui.screen.LoginScreen
import com.example.atvidadedm.ui.screen.MenuScreen
import com.example.atvidadedm.ui.screen.RegisterScreen
import kotlin.text.clear


@Composable
fun AppNavigation() {
    // Pilha de navegação: começa na tela de Login
    val backStack = remember { mutableStateListOf(RouteId.LOGIN) }
    val currentRoute: String by remember(backStack) {
        androidx.compose.runtime.derivedStateOf {
            backStack.lastOrNull() ?: RouteId.LOGIN
        }
    }

    when (currentRoute) {
        RouteId.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    backStack.clear()
                    backStack.add(RouteId.MENU)
                },
                onRegister = {
                    backStack.add(RouteId.REGISTER)
                },
                onForgotPassword = {
                    backStack.add(RouteId.FORGOT_PASSWORD)
                }
            )
        }

        RouteId.REGISTER -> {
            RegisterScreen(
                onBack = { backStack.popRouteOrStayAtRoot() }
            )
        }

        RouteId.FORGOT_PASSWORD -> {
            ForgotPasswordScreen(
                onBack = { backStack.popRouteOrStayAtRoot() }
            )
        }

        RouteId.MENU -> {
            MenuScreen(
                onBack = {
                    backStack.clear()
                    backStack.add(RouteId.LOGIN)
                }
            )
        }

        else -> {
            LoginScreen(
                onLoginSuccess = {
                    backStack.clear()
                    backStack.add(RouteId.MENU)
                },
                onRegister = {
                    backStack.add(RouteId.REGISTER)
                },
                onForgotPassword = {
                    backStack.add(RouteId.FORGOT_PASSWORD)
                }
            )
        }
    }
}

private object RouteId {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val MENU = "menu"
}

private fun SnapshotStateList<String>.popRouteOrStayAtRoot() {
    if (size > 1) {
        removeAt(lastIndex)
    }
}

