package com.example.atvidadedm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.atvidadedm.ui.screen.ForgotPasswordScreen
import com.example.atvidadedm.ui.screen.LoginScreen
import com.example.atvidadedm.ui.screen.MenuScreen
import com.example.atvidadedm.ui.screen.RegisterScreen

/**
 * Composable que configura toda a navegação do app usando Navigation 3.
 *
 * A pilha de navegação (backStack) começa com a Tela de Login.
 */
@Composable
fun AppNavigation() {
    // Pilha de navegação: começa na tela de Login
    val backStack = remember { mutableStateListOf<Any>(LoginRoute) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            // ── Login ──────────────────────────────────────────────────────────
            entry<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = {
                        // Limpa a pilha e vai direto ao Menu (sem voltar para Login)
                        backStack.clear()
                        backStack.add(MenuRoute)
                    },
                    onRegister = {
                        backStack.add(RegisterRoute)
                    },
                    onForgotPassword = {
                        backStack.add(ForgotPasswordRoute)
                    }
                )
            }

            // ── Cadastro ───────────────────────────────────────────────────────
            entry<RegisterRoute> {
                RegisterScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            // ── Senha Esquecida ────────────────────────────────────────────────
            entry<ForgotPasswordRoute> {
                ForgotPasswordScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            // ── Menu Principal ─────────────────────────────────────────────────
            entry<MenuRoute> {
                MenuScreen()
            }
        }
    )
}

