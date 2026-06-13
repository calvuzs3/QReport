@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.ti.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import net.calvuz.qreport.R
import net.calvuz.qreport.ti.presentation.ui.components.TechnicianSignatureDialog
import net.calvuz.qreport.ti.presentation.ui.components.CustomerSignatureDialog
import net.calvuz.qreport.ti.presentation.ui.components.SignaturePreview
import timber.log.Timber

/**
 * Form screen for managing digital signatures:
 * - Technician signature (name + digital collection)
 * - Customer signature (name + digital collection)
 * - Follows EditInterventionScreen pattern for integration
 */
@Composable
fun SignaturesFormScreen(
    interventionId: String,
    modifier: Modifier = Modifier,
    viewModel: SignaturesFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Note: ViewModel is initialized by parent EditInterventionScreen to avoid double loading
    // LaunchedEffect(interventionId) { viewModel.loadSignaturesData(interventionId) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // ===== DIRTY STATE INDICATOR =====
        if (state.isDirty) {
            DirtyStateIndicator(
                message = stringResource(R.string.intervention_general_unsaved_changes),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== AUTO-SAVE INDICATOR =====
        if (state.isSaving) {
            AutoSaveIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== ERROR DISPLAY =====
        state.errorMessage?.let { errorMessage ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = errorMessage.asString(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ===== PROCESSING INDICATOR =====
        if (state.isProcessingSignature) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.intervention_signature_processing),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ===== TECHNICIAN SIGNATURE SECTION =====
        TechnicianSignatureSection(
            technicianName = state.technicianName,
            onTechnicianNameChange = { name ->
                Timber.v("SignaturesFormScreen: Technician name changed: '$name'")
                viewModel.updateTechnicianName(name)
            },
            hasDigitalSignature = state.hasTechnicianDigitalSignature,
            signaturePath = state.technicianSignaturePath,
            onCollectSignature = viewModel::showTechnicianSignatureDialog,
            isProcessing = state.isProcessingSignature,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ===== CUSTOMER SIGNATURE SECTION =====
        CustomerSignatureSection(
            customerName = state.customerName,
            onCustomerNameChange = { name ->
                Timber.v("SignaturesFormScreen: Customer name changed: '$name'")
                viewModel.updateCustomerName(name)
            },
            hasDigitalSignature = state.hasCustomerDigitalSignature,
            signaturePath = state.customerSignaturePath,
            onCollectSignature = viewModel::showCustomerSignatureDialog,
            isProcessing = state.isProcessingSignature,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ===== READY FOR SIGNATURES STATUS =====
        ReadyForSignaturesCard(
            isReady = state.isReadyForSignatures,
            onReadyChange = { isReady ->
                Timber.v("SignaturesFormScreen: Ready status changed: $isReady")
                viewModel.updateReadyStatus(isReady)
            },
            bothSignaturesComplete = state.areBothSignaturesDigital,
            modifier = Modifier.fillMaxWidth()
        )

        // ===== COMPLETION SUMMARY =====
        if (state.areBothSignaturesDigital || state.isReadyForSignatures) {
            CompletionSummaryCard(
                technicianSigned = state.hasTechnicianDigitalSignature,
                customerSigned = state.hasCustomerDigitalSignature,
                isReady = state.isReadyForSignatures,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }

    // ===== SIGNATURE COLLECTION DIALOGS =====

    // Technician signature dialog
    TechnicianSignatureDialog(
        isVisible = state.showTechnicianSignatureDialog,
        technicianName = state.technicianName,
        onDismiss = viewModel::hideTechnicianSignatureDialog,
        onSignatureConfirm = { signatureBitmap ->
            viewModel.collectTechnicianSignature(signatureBitmap)
        }
    )

    // Customer signature dialog
    CustomerSignatureDialog(
        isVisible = state.showCustomerSignatureDialog,
        customerName = state.customerName,
        onDismiss = viewModel::hideCustomerSignatureDialog,
        onSignatureConfirm = { signatureBitmap ->
            viewModel.collectCustomerSignature(signatureBitmap)
        }
    )
}

@Composable
private fun TechnicianSignatureSection(
    technicianName: String,
    onTechnicianNameChange: (String) -> Unit,
    hasDigitalSignature: Boolean,
    signaturePath: String,
    onCollectSignature: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.intervention_signatures_form_technician_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Technician name field
            OutlinedTextField(
                value = technicianName,
                onValueChange = onTechnicianNameChange,
                label = { Text(stringResource(R.string.intervention_signatures_form_technician_name_label)) },
                placeholder = { Text(stringResource(R.string.intervention_signatures_form_technician_name_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ===== ADD SIGNATURE PREVIEW HERE =====
            if (hasDigitalSignature && signaturePath.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.intervention_signatures_form_signature_acquired_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SignaturePreview(
                        signaturePath = signaturePath,
                        modifier = Modifier.fillMaxWidth(),
                        contentDescription = stringResource(R.string.intervention_signature_technician_title)
                    )
                }
            }
            // ===== END SIGNATURE PREVIEW =====

            // Digital signature collection
            SignatureCollectionCard(
                title = stringResource(R.string.intervention_signatures_form_technician_digital_title),
                hasSignature = hasDigitalSignature,
                onCollectSignature = onCollectSignature,
                isProcessing = isProcessing,
                enabled = technicianName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CustomerSignatureSection(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    hasDigitalSignature: Boolean,
    signaturePath: String,
    onCollectSignature: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.intervention_signatures_form_customer_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Customer name field
            OutlinedTextField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                label = { Text(stringResource(R.string.intervention_signatures_form_customer_name_label)) },
                placeholder = { Text(stringResource(R.string.intervention_signatures_form_customer_name_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ===== ADD SIGNATURE PREVIEW HERE =====
            if (hasDigitalSignature && signaturePath.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.intervention_signatures_form_signature_acquired_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SignaturePreview(
                        signaturePath = signaturePath,
                        modifier = Modifier.fillMaxWidth(),
                        contentDescription = stringResource(R.string.intervention_signature_customer_title)
                    )
                }
            }
            // ===== END SIGNATURE PREVIEW =====

            // Digital signature collection
            SignatureCollectionCard(
                title = stringResource(R.string.intervention_signatures_form_customer_digital_title),
                hasSignature = hasDigitalSignature,
                onCollectSignature = onCollectSignature,
                isProcessing = isProcessing,
                enabled = customerName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SignatureCollectionCard(
    title: String,
    hasSignature: Boolean,
    onCollectSignature: () -> Unit,
    isProcessing: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (hasSignature)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (hasSignature) Icons.Default.Verified else Icons.Default.Draw,
                    contentDescription = null,
                    tint = if (hasSignature)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (hasSignature)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (hasSignature)
                            stringResource(R.string.intervention_signatures_form_collection_success)
                        else
                            stringResource(R.string.intervention_signatures_form_collection_pending),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasSignature)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onCollectSignature,
                enabled = enabled && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasSignature)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = if (hasSignature) stringResource(R.string.intervention_signatures_form_button_edit)
                    else stringResource(R.string.intervention_signatures_form_button_collect)
                )
            }

            if (!enabled) {
                Text(
                    text = stringResource(R.string.intervention_signatures_form_name_required_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ReadyForSignaturesCard(
    isReady: Boolean,
    onReadyChange: (Boolean) -> Unit,
    bothSignaturesComplete: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bothSignaturesComplete && isReady)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.intervention_signatures_form_document_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Ready switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(
                    checked = isReady,
                    onCheckedChange = onReadyChange
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isReady) stringResource(R.string.intervention_signatures_form_ready_title)
                        else stringResource(R.string.intervention_signatures_form_not_ready_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isReady)
                            stringResource(R.string.intervention_signatures_form_ready_description)
                        else
                            stringResource(R.string.intervention_signatures_form_not_ready_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Completion status
            if (bothSignaturesComplete && isReady) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.intervention_signatures_form_digital_signatures_complete),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionSummaryCard(
    technicianSigned: Boolean,
    customerSigned: Boolean,
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.intervention_signatures_form_completion_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Checklist items
            SignatureChecklistItem(
                text = stringResource(R.string.intervention_signatures_form_technician_section_title),
                isComplete = technicianSigned
            )

            SignatureChecklistItem(
                text = stringResource(R.string.intervention_signatures_form_customer_section_title),
                isComplete = customerSigned
            )

            SignatureChecklistItem(
                text = stringResource(R.string.intervention_signatures_form_document_ready_checklist),
                isComplete = isReady
            )

            // Overall status
            val allComplete = technicianSigned && customerSigned && isReady
            if (allComplete) {
                HorizontalDivider()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.intervention_signatures_form_process_complete),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SignatureChecklistItem(
    text: String,
    isComplete: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isComplete)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isComplete)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Dirty state indicator component - matches DetailsFormScreen pattern
 */
@Composable
private fun DirtyStateIndicator(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Auto-save indicator component - matches DetailsFormScreen pattern
 */
@Composable
private fun AutoSaveIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.msg_saving),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}