package com.example.atvidadedm.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.atvidadedm.data.UserRepository
import com.example.atvidadedm.data.local.AppDatabase
import com.example.atvidadedm.ui.theme.AtvidadeDMTheme
import com.example.atvidadedm.ui.viewmodel.RegisterViewModel
import com.example.atvidadedm.ui.viewmodel.RegisterViewModelFactory

/**
 * Tela de Cadastro de novo usuário.
 *
 * @param onBack Chamado ao pressionar a seta de retorno ou após o registro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    providedViewModel: RegisterViewModel? = null
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultViewModel: RegisterViewModel = viewModel(
        factory = remember {
            RegisterViewModelFactory(
                UserRepository(
                    AppDatabase.getInstance(context).userDao()
                )
            )
        }
    )
    val activeViewModel = providedViewModel ?: defaultViewModel
    val uiState by activeViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            activeViewModel.onFeedbackMessageShown()
        }
    }

    LaunchedEffect(uiState.registrationSucceeded) {
        if (uiState.registrationSucceeded) {
            activeViewModel.onNavigationToLoginHandled()
            onBack()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { Text("Criar Conta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Preencha todos os campos para se cadastrar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo: Nome
            OutlinedTextField(
                value = uiState.name,
                onValueChange = activeViewModel::onNameChange,
                label = { Text("Nome") },
                placeholder = { Text("Seu nome completo") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo: E-mail
            OutlinedTextField(
                value = uiState.email,
                onValueChange = activeViewModel::onEmailChange,
                label = { Text("E-mail") },
                placeholder = { Text("exemplo@email.com") },
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo: Telefone
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = activeViewModel::onPhoneChange,
                label = { Text("Telefone") },
                placeholder = { Text("(00) 00000-0000") },
                isError = uiState.phoneError != null,
                supportingText = uiState.phoneError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo: Senha
            OutlinedTextField(
                value = uiState.password,
                onValueChange = activeViewModel::onPasswordChange,
                label = { Text("Senha") },
                visualTransformation = if (uiState.passwordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = activeViewModel::togglePasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.passwordVisible)
                                Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (uiState.passwordVisible)
                                "Ocultar senha" else "Mostrar senha"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo: Confirmação de Senha
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = activeViewModel::onConfirmPasswordChange,
                label = { Text("Confirmar Senha") },
                visualTransformation = if (uiState.confirmPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = activeViewModel::toggleConfirmPasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.confirmPasswordVisible)
                                Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (uiState.confirmPasswordVisible)
                                "Ocultar senha" else "Mostrar senha"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botão de Cadastro – valida e retorna para Login
            Button(
                onClick = {
                    activeViewModel.registerUser()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.isSaving) "Cadastrando..." else "Cadastrar")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    AtvidadeDMTheme {
        RegisterScreen(onBack = {})
    }
}
