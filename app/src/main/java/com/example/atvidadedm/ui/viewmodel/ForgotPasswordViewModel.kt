package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado da tela de Recuperação de Senha.
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val emailSent: Boolean = false
)

/**
 * ViewModel responsável pela lógica da Tela de Senha Esquecida.
 */
class ForgotPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    /**
     * Valida o e-mail e retorna `true` se estiver preenchido.
     */
    fun validate(): Boolean {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "E-mail é obrigatório") }
            return false
        }
        _uiState.update { it.copy(emailSent = true) }
        return true
    }
}

