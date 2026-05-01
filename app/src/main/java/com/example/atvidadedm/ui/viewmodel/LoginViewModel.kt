package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.LoginResult
import com.example.atvidadedm.data.UserRepository
import com.example.atvidadedm.data.local.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela de Login.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
    val loggedUser: UserEntity? = null
)

/**
 * ViewModel responsável pela lógica da Tela de Login.
 */
class LoginViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun login() {
        if (!validate()) {
            return
        }

        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = userRepository.authenticateUser(state.email, state.password)) {
                is LoginResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loggedUser = result.user,
                            feedbackMessage = "Login realizado com sucesso!"
                        )
                    }
                }

                LoginResult.InvalidCredentials -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            feedbackMessage = "E-mail ou senha inválidos"
                        )
                    }
                }
            }
        }
    }

    fun onFeedbackMessageShown() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun onLoginHandled() {
        _uiState.update { it.copy(loggedUser = null) }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        var isValid = true

        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                feedbackMessage = null
            )
        }

        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "E-mail é obrigatório") }
            isValid = false
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Senha é obrigatória") }
            isValid = false
        }

        return isValid
    }
}

