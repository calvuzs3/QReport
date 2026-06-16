package net.calvuz.qreport.settings.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.settings.domain.model.TechnicianInfo

/**
 * Screen per gestire le impostazioni del tecnico
 * - ValidationError completo per inserimento dati tecnico
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
                title = { Text(stringResource(R.string.settings_screen_technician_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        enabled = hasTechnicianData && !uiState.isLoading && !uiState.isSaving
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.settings_screen_technician_action_reset)
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
                .imePadding()
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

            // Main ValidationError Card
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
                Text(
                    stringResource(
                        if (uiState.isSaving) R.string.settings_screen_technician_saving
                        else R.string.settings_screen_technician_save
                    )
                )
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
                    text = stringResource(R.string.settings_screen_technician_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.settings_screen_technician_info_message),
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
                    text = stringResource(R.string.settings_screen_technician_form_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Name Field
            OutlinedTextField(
                value = formState.name,
                onValueChange = { onFieldUpdate(TechnicianField.NAME, it) },
                label = { Text(stringResource(R.string.field_technician_name)) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Company Field
            OutlinedTextField(
                value = formState.company,
                onValueChange = { onFieldUpdate(TechnicianField.COMPANY, it) },
                label = { Text(stringResource(R.string.settings_screen_technician_field_company)) },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Certification Field
            OutlinedTextField(
                value = formState.certification,
                onValueChange = { onFieldUpdate(TechnicianField.CERTIFICATION, it) },
                label = { Text(stringResource(R.string.settings_screen_technician_field_certification)) },
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
                    label = { Text(stringResource(R.string.field_phone)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.settings_screen_technician_phone_placeholder)) }
                )

                // Email Field
                OutlinedTextField(
                    value = formState.email,
                    onValueChange = { onFieldUpdate(TechnicianField.EMAIL, it) },
                    label = { Text(stringResource(R.string.field_email)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.settings_screen_technician_email_placeholder)) }
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
                    text = stringResource(R.string.settings_screen_technician_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (technicianInfo.name.isNotBlank()) {
                PreviewItem(stringResource(R.string.settings_screen_technician_preview_name), technicianInfo.name)
            }
            if (technicianInfo.company.isNotBlank()) {
                PreviewItem(stringResource(R.string.settings_screen_technician_preview_company), technicianInfo.company)
            }
            if (technicianInfo.certification.isNotBlank()) {
                PreviewItem(stringResource(R.string.settings_screen_technician_preview_certification), technicianInfo.certification)
            }
            if (technicianInfo.phone.isNotBlank()) {
                PreviewItem(stringResource(R.string.settings_screen_technician_preview_phone), technicianInfo.phone)
            }
            if (technicianInfo.email.isNotBlank()) {
                PreviewItem(stringResource(R.string.settings_screen_technician_preview_email), technicianInfo.email)
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
private fun ValidationErrorsCard(errors: List<UiText>) {
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
                    text = stringResource(R.string.settings_screen_technician_validation_errors_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
            errors.forEach { error ->
                Text(
                    text = "• ${error.asString()}",
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
private fun ErrorCard(message: UiText) {
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
                text = message.asString(),
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
private fun SuccessCard(message: UiText) {
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
                text = message.asString(),
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
            Text(stringResource(R.string.settings_screen_technician_reset_dialog_title))
        },
        text = {
            Text(
                stringResource(R.string.settings_screen_technician_reset_dialog_message),
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
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}