package com.example.atvidadedm.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.atvidadedm.data.local.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SessionUiState(
    val currentUser: UserEntity? = null
)

class SessionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    fun startSession(user: UserEntity) {
        _uiState.update { it.copy(currentUser = user) }
    }

    fun clearSession() {
        _uiState.update { it.copy(currentUser = null) }
    }
}

