package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atvidadedm.data.RegisterResult
import com.example.atvidadedm.data.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado da tela de Cadastro.
 */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSaving: Boolean = false,
    val feedbackMessage: String? = null,
    val registrationSucceeded: Boolean = false
)

/**
 * ViewModel responsável pela lógica da Tela de Cadastro.
 */
class RegisterViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun registerUser() {
        if (!validate()) {
            return
        }

        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            when (
                userRepository.registerUser(
                    name = state.name,
                    email = state.email,
                    phone = state.phone,
                    password = state.password
                )
            ) {
                RegisterResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = "Usuario cadastrado com sucesso!",
                            registrationSucceeded = true
                        )
                    }
                }

                RegisterResult.EmailAlreadyExists -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            emailError = "Ja existe um usuario com este e-mail"
                        )
                    }
                }

                RegisterResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            feedbackMessage = "Nao foi possivel concluir o cadastro"
                        )
                    }
                }
            }
        }
    }

    fun onFeedbackMessageShown() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun onNavigationToLoginHandled() {
        _uiState.update { it.copy(registrationSucceeded = false) }
    }

    /**
     * Valida todos os campos e retorna `true` se o formulário estiver correto.
     */
    fun validate(): Boolean {
        val state = _uiState.value
        var isValid = true

        _uiState.update {
            it.copy(
                nameError = null,
                emailError = null,
                phoneError = null,
                passwordError = null,
                confirmPasswordError = null
            )
        }

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Nome é obrigatório") }
            isValid = false
        }

        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "E-mail é obrigatório") }
            isValid = false
        }

        if (state.phone.isBlank()) {
            _uiState.update { it.copy(phoneError = "Telefone é obrigatório") }
            isValid = false
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Senha é obrigatória") }
            isValid = false
        }

        if (state.confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Confirmação de senha é obrigatória") }
            isValid = false
        } else if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "As senhas não coincidem") }
            isValid = false
        }

        return isValid
    }
}

