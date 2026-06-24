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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.atvidadedm.ui.viewmodel.RoteiroViewModel
import com.example.atvidadedm.ui.viewmodel.RoteiroViewModelFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoteiroScreen(
    currentUserId: Long,
    tripId: Long,
    onOpenRoteiro: (Long) -> Unit,
    onOpenPhotos: (Long) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as TravelApplication
    val viewModel: RoteiroViewModel = viewModel(
        factory = remember(currentUserId, tripId) {
            RoteiroViewModelFactory(
                tripRepository = application.tripRepository,
                geminiRepository = application.geminiRepository,
                userId = currentUserId,
                tripId = tripId.takeIf { it > 0 }
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())
    }

    Scaffold(
        bottomBar = {
            TripBottomBar(
                selectedDestination = TripBottomBarDestination.ROUTEIRO,
                showPhotoTab = true,
                onOpenRoteiro = { onOpenRoteiro(tripId) },
                onOpenPhotos = { onOpenPhotos(tripId) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Roteiro da viagem",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "O roteiro é gerado automaticamente ao cadastrar uma nova viagem e fica salvo aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.isLoadingTrip) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Carregando dados da viagem..."
                    )
                }
            }

            uiState.message?.let { message ->
                Card {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Dados da viagem",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = uiState.destination,
                        onValueChange = viewModel::onDestinationChange,
                        label = { Text("Destino") },
                        modifier = Modifier.fillMaxWidth(),
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
                        placeholder = { Text("Selecione a data") }
                    )

                    OutlinedTextField(
                        value = uiState.endDate?.let { formatDate(it, formatter) } ?: "",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        readOnly = true,
                        label = { Text("Data fim") },
                        placeholder = { Text("Selecione a data") }
                    )

                    OutlinedTextField(
                        value = uiState.interests,
                        onValueChange = viewModel::onInterestsChange,
                        label = { Text("Interesses") },
                        placeholder = { Text("Ex.: museus, gastronomia, praia, compras") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = uiState.comments,
                        onValueChange = viewModel::onCommentsChange,
                        label = { Text("Comentários adicionais") },
                        placeholder = { Text("Adicione detalhes sobre a viagem...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = uiState.travelStyle,
                        onValueChange = viewModel::onTravelStyleChange,
                        label = { Text("Estilo da viagem") },
                        placeholder = { Text("Ex.: econômico, romântico, família") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.budget,
                        onValueChange = viewModel::onBudgetChange,
                        label = { Text("Orçamento") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )

                    Text(
                        text = "Nenhuma ação disponível nesta tela. O roteiro é gerado ao salvar uma nova viagem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!uiState.itinerary.isNullOrBlank()) {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Roteiro gerado",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        SelectionContainer {
                            Text(
                                text = uiState.itinerary.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                Card {
                    Text(
                        text = "Este roteiro ainda não foi gerado. Crie uma nova viagem para recebê-lo automaticamente.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
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
