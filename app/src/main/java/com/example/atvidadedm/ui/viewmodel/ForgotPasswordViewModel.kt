package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela de Recuperação de Senha.
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isSending: Boolean = false,
    val feedbackMessage: String? = null,
    val recoverySent: Boolean = false
)

/**
 * ViewModel responsável pela lógica da Tela de Senha Esquecida.
 */
class ForgotPasswordViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, feedbackMessage = null) }
    }

    fun submitRecovery() {
        if (!validate()) {
            return
        }

        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }

            val registeredUser = userRepository.findUserByEmail(state.email)
            if (registeredUser == null) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        emailError = "E-mail não encontrado"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        feedbackMessage = "Solicitação enviada com sucesso!",
                        recoverySent = true
                    )
                }
            }
        }
    }

    fun onFeedbackMessageShown() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun onRecoveryHandled() {
        _uiState.update { it.copy(recoverySent = false) }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "E-mail é obrigatório") }
            return false
        }
        return true
    }
}

