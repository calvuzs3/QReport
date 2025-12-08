package net.calvuz.qreport.presentation.screen.client.contact

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.ContactMethod

/**
 * Screen dettaglio contatto - Pattern identico a ClientDetailScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    modifier: Modifier = Modifier,
    contactId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteContact: () -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    // ✅ Handle delete success - Navigate back automatically
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteState()
            onDeleteContact()  // Navigate back to client list
        }
    }

    // ✅ Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmation,
            title = { Text("Elimina Contatto") },
            text = {
                Text("Sei sicuro di voler eliminare ${uiState.fullName}? Questa azione non può essere annullata.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteContact,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmation) {
                    Text("Annulla")
                }
            }
        )
    }

    // Load data on start
    LaunchedEffect(contactId) {
        if (contactId.isNotBlank()) {
            viewModel.loadContact(contactId)
        }
    }

    // Handle navigation on update success
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            viewModel.resetUpdateState()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = uiState.fullName.takeIf { it.isNotBlank() } ?: "Contatto",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Indietro"
                    )
                }
            },
            actions = {
                // Delete button
                if (uiState.hasData) {
                    IconButton(
                        onClick = viewModel::showDeleteConfirmation,  // Show confirmation dialog
                        enabled = !uiState.isDeleting
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = "Elimina contatto"
                            )
                        }
                    }
                }

                // Edit button
                if (uiState.hasData) {
                    IconButton(
                        onClick = { onNavigateToEdit(contactId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica contatto"
                        )
                    }
                }
            }
        )

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!, // force cast, the nul is already checked
                        onRetry = { viewModel.loadContact(contactId) },
                        onDismiss = viewModel::dismissError,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.hasData -> {
                    ContactDetailContent(
                        contact = uiState.contact!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.isEmpty -> {
                    EmptyContent(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Update success message
    if (uiState.updateSuccess) {
        LaunchedEffect(uiState.updateSuccess) {
            // Potresti aggiungere un SnackBar qui se vuoi
        }
    }

    // Update error message
    uiState.updateError?.let { error ->
        LaunchedEffect(error) {
            // Potresti aggiungere un SnackBar qui se vuoi
        }
    }
}

@Composable
private fun ContactDetailContent(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            ContactHeaderCard(contact = contact)
        }

        // Contact Methods
        item {
            ContactMethodsCard(
                contact = contact,
                onPhoneClick = { phone ->
                    val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                    context.startActivity(intent)
                },
                onEmailClick = { email ->
                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri())
                    context.startActivity(intent)
                }
            )
        }

        // Role & Department
        if (!contact.roleDescription.isBlank()) {
            item {
                RoleCard(contact = contact)
            }
        }

        // Notes
        if (!contact.notes.isNullOrBlank()) {
            item {
                NotesCard(notes = contact.notes)
            }
        }

        // Status & Metadata
        item {
            StatusCard(contact = contact)
        }
    }
}

@Composable
private fun ContactHeaderCard(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (!contact.roleDescription.isBlank()) {
                        Text(
                            text = contact.roleDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (contact.isPrimary) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Principale") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactMethodsCard(
    contact: Contact,
    onPhoneClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Contatti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Phone
            contact.phone?.let { phone ->
                ContactMethodItem(
                    icon = Icons.Default.Phone,
                    label = "Telefono",
                    value = phone,
                    onClick = { onPhoneClick(phone) },
                    isPrimary = contact.preferredContactMethod == ContactMethod.PHONE
                )
            }

            // Mobile
            contact.mobilePhone?.let { mobile ->
                ContactMethodItem(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Cellulare",
                    value = mobile,
                    onClick = { onPhoneClick(mobile) },
                    isPrimary = contact.preferredContactMethod == ContactMethod.MOBILE
                )
            }

            // Email
            contact.email?.let { email ->
                ContactMethodItem(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = email,
                    onClick = { onEmailClick(email) },
                    isPrimary = contact.preferredContactMethod == ContactMethod.EMAIL
                )
            }

            // Alternative Email
            contact.alternativeEmail?.let { altEmail ->
                ContactMethodItem(
                    icon = Icons.Default.AlternateEmail,
                    label = "Email alternativa",
                    value = altEmail,
                    onClick = { onEmailClick(altEmail) }
                )
            }
        }
    }
}

@Composable
private fun ContactMethodItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isPrimary) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Preferito",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Launch,
                contentDescription = "Contatta",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RoleCard(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ruolo Aziendale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            contact.role?.let { role ->
                InfoRow(
                    icon = Icons.Default.Work,
                    label = "Posizione",
                    value = role
                )
            }

            contact.department?.let { dept ->
                InfoRow(
                    icon = Icons.Default.Business,
                    label = "Dipartimento",
                    value = dept
                )
            }
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatusCard(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Stato",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusChip(
                    text = if (contact.isActive) "Attivo" else "Inattivo",
                    isActive = contact.isActive
                )

                if (contact.isPrimary) {
                    StatusChip(
                        text = "Contatto Principale",
                        isActive = true
                    )
                }
            }

            InfoRow(
                icon = Icons.Default.Schedule,
                label = "Creato",
                value = contact.createdAt.toString().substring(0, 10)
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
    )
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss) {
                Text("Chiudi")
            }
            Button(onClick = onRetry) {
                Text("Riprova")
            }
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PersonOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Contatto non trovato",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}