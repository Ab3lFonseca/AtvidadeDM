package com.example.atvidadedm.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.atvidadedm.TravelApplication
import com.example.atvidadedm.data.model.TripType
import com.example.atvidadedm.ui.viewmodel.TripFormViewModel
import com.example.atvidadedm.ui.viewmodel.TripFormViewModelFactory
import androidx.compose.foundation.text.KeyboardOptions
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTripScreen(
    currentUserId: Long,
    tripId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as TravelApplication
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: TripFormViewModel = viewModel(
        factory = remember(currentUserId, tripId) {
            TripFormViewModelFactory(
                tripRepository = application.tripRepository,
                userId = currentUserId,
                tripId = tripId
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onFeedbackMessageShown()
        }
    }

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.onSaveHandled()
            onSaved()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (uiState.isEditMode) "Editar viagem" else "Nova viagem",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = uiState.destination,
                    onValueChange = viewModel::onDestinationChange,
                    label = { Text("Destino") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.destinationError != null,
                    supportingText = uiState.destinationError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true
                )

                Text(
                    text = "Tipo da viagem",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = uiState.type == TripType.LAZER,
                        onClick = { viewModel.onTypeChange(TripType.LAZER) },
                        label = { Text("Lazer") }
                    )
                    FilterChip(
                        selected = uiState.type == TripType.NEGOCIOS,
                        onClick = { viewModel.onTypeChange(TripType.NEGOCIOS) },
                        label = { Text("Negócios") }
                    )
                }

                OutlinedTextField(
                    value = uiState.startDate?.let { formatDate(it, formatter) } ?: "",
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true },
                    readOnly = true,
                    label = { Text("Data início") },
                    placeholder = { Text("Selecione a data") },
                    isError = uiState.startDateError != null,
                    supportingText = uiState.startDateError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )

                OutlinedTextField(
                    value = uiState.endDate?.let { formatDate(it, formatter) } ?: "",
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndDatePicker = true },
                    readOnly = true,
                    label = { Text("Data fim") },
                    placeholder = { Text("Selecione a data") },
                    isError = uiState.endDateError != null,
                    supportingText = uiState.endDateError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )

                OutlinedTextField(
                    value = uiState.budget,
                    onValueChange = viewModel::onBudgetChange,
                    label = { Text("Orçamento") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.budgetError != null,
                    supportingText = uiState.budgetError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Voltar")
                    }

                    Button(
                        onClick = viewModel::saveTrip,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Salvando..." else "Salvar")
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDate
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onStartDateSelected)
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = uiState.endDate
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onEndDateSelected)
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatDate(
    millis: Long,
    formatter: DateTimeFormatter
): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(formatter)
}

