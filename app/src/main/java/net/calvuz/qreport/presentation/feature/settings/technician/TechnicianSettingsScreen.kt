package net.calvuz.qreport.presentation.feature.settings.technician

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import net.calvuz.qreport.domain.model.settings.TechnicianInfo

/**
 * Screen per gestire le impostazioni del tecnico
 * - Form completo per inserimento dati tecnico
 * - Validazione real-time
 * - Preview dei dati salvati
 * - Reset con conferma
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TechnicianSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val currentTechnicianInfo by viewModel.currentTechnicianInfo.collectAsStateWithLifecycle()
    val hasTechnicianData by viewModel.hasTechnicianData.collectAsStateWithLifecycle()

    // Reset confirmation dialog state
    var showResetDialog by remember { mutableStateOf(false) }

    // Auto-clear messages after 3 seconds
    LaunchedEffect(uiState.error, uiState.message) {
        if (uiState.error != null || uiState.message != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informazioni Tecnico") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        enabled = hasTechnicianData && !uiState.isLoading && !uiState.isSaving
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset Impostazioni"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Info Banner
            InfoBannerCard()

            // Error Messages
            if (uiState.error != null) {
                ErrorCard(message = uiState.error!!)
            }

            // Success Messages
            if (uiState.message != null) {
                SuccessCard(message = uiState.message!!)
            }

            // Main Form Card
            TechnicianFormCard(
                formState = formState,
                onFieldUpdate = viewModel::updateFormField,
                isLoading = uiState.isSaving
            )

            // Validation Errors
            if (formState.validationErrors.isNotEmpty()) {
                ValidationErrorsCard(errors = formState.validationErrors)
            }

            // Save Button
            Button(
                onClick = viewModel::saveTechnicianInfo,
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isValid && formState.isModified && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isSaving) "Salvataggio..." else "Salva Impostazioni")
            }

            // Preview Card (show only if has saved data)
            if (hasTechnicianData) {
                PreviewCard(technicianInfo = currentTechnicianInfo)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = {
                viewModel.resetToDefault()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

/**
 * Info banner explaining the purpose of technician settings
 */
@Composable
private fun InfoBannerCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Pre-compilazione CheckUp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "I dati inseriti qui verranno utilizzati per pre-compilare automaticamente le informazioni del tecnico nei nuovi CheckUp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Main form for technician data
 */
@Composable
private fun TechnicianFormCard(
    formState: TechnicianFormState,
    onFieldUpdate: (TechnicianField, String) -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Engineering,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Dati Tecnico",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Name Field
            OutlinedTextField(
                value = formState.name,
                onValueChange = { onFieldUpdate(TechnicianField.NAME, it) },
                label = { Text("Nome Tecnico") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Company Field
            OutlinedTextField(
                value = formState.company,
                onValueChange = { onFieldUpdate(TechnicianField.COMPANY, it) },
                label = { Text("Azienda") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Certification Field
            OutlinedTextField(
                value = formState.certification,
                onValueChange = { onFieldUpdate(TechnicianField.CERTIFICATION, it) },
                label = { Text("Certificazione") },
                leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Contact Information Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Phone Field
                OutlinedTextField(
                    value = formState.phone,
                    onValueChange = { onFieldUpdate(TechnicianField.PHONE, it) },
                    label = { Text("Telefono") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("+39 123 456 7890") }
                )

                // Email Field
                OutlinedTextField(
                    value = formState.email,
                    onValueChange = { onFieldUpdate(TechnicianField.EMAIL, it) },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text("nome@azienda.it") }
                )
            }
        }
    }
}

/**
 * Preview card showing currently saved data
 */
@Composable
private fun PreviewCard(technicianInfo: TechnicianInfo) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Preview,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Dati Salvati",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (technicianInfo.name.isNotBlank()) {
                PreviewItem("Nome", technicianInfo.name)
            }
            if (technicianInfo.company.isNotBlank()) {
                PreviewItem("Azienda", technicianInfo.company)
            }
            if (technicianInfo.certification.isNotBlank()) {
                PreviewItem("Certificazione", technicianInfo.certification)
            }
            if (technicianInfo.phone.isNotBlank()) {
                PreviewItem("Telefono", technicianInfo.phone)
            }
            if (technicianInfo.email.isNotBlank()) {
                PreviewItem("Email", technicianInfo.email)
            }
        }
    }
}

/**
 * Preview item for individual field
 */
@Composable
private fun PreviewItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Validation errors card
 */
@Composable
private fun ValidationErrorsCard(errors: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Errori di validazione",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
            errors.forEach { error ->
                Text(
                    text = "• $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Error card for general errors
 */
@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Success card for success messages
 */
@Composable
private fun SuccessCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Reset confirmation dialog
 */
@Composable
private fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Conferma Reset")
        },
        text = {
            Text(
                "Sei sicuro di voler eliminare tutte le informazioni del tecnico salvate? " +
                        "Questa azione non può essere annullata.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}