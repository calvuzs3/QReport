package net.calvuz.qreport.client.contact.presentation.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod
import net.calvuz.qreport.app.app.presentation.components.DeleteDialog
import net.calvuz.qreport.app.app.presentation.components.PrimaryBadge

@Composable
fun ContactCard(
    modifier: Modifier = Modifier,
    contact: Contact,
    onClick: () -> Unit,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onEmail: () -> Unit = { },
    onSetPrimary: () -> Unit,
    isSettingPrimary: Boolean,
    variant: ContactCardVariant = ContactCardVariant.FULL
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (variant) {
            ContactCardVariant.FULL -> FullContactCard(
                contact = contact,
                showActions = showActions,
                onDelete = onDelete,
                onEdit = onEdit,
                onSetPrimary = onSetPrimary,
                isSettingPrimary = isSettingPrimary,
            )

            ContactCardVariant.COMPACT -> CompactContactCard(
                contact = contact,
            )

            ContactCardVariant.MINIMAL -> MinimalContactCard(contact = contact)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        DeleteDialog(
            title = "Elimina Contatto",
            text = "Sei sicuro di voler eliminare il contatto '${contact.fullName}'? Questa operazione non può essere annullata.",
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun FullContactCard(
    contact: Contact,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onSetPrimary: () -> Unit,
    isSettingPrimary: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row - Nome stabilimento e azioni
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = contact.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (contact.isPrimary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    PrimaryBadge()
                }
            }

            if (showActions) {
                Row {
                    // STAR IT
                    if (!contact.isPrimary && contact.isActive) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onSetPrimary,
                            enabled = !isSettingPrimary
                        ) {
                            if (isSettingPrimary) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = "Imposta come primario",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // EDIT
                    if (onEdit != null) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onEdit
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Modifica",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // DELETE
                    if (onDelete != null) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onDelete
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Elimina",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!contact.isActive) {
            Row {
                AssistChip(
                    onClick = { },
                    label = {
                        Text("Inattivo", style = MaterialTheme.typography.labelSmall)
                    },
                    enabled = false
                )
            }
        }

        // ROLE
        contact.role?.let { role ->
            ContactRoleItem(
                icon = Icons.Default.Work,
                label = "Posizione",
                value = role
            )
        }

        // DEPARTMENT
        contact.department?.let { dept ->
            ContactRoleItem(
                icon = Icons.Default.Business,
                label = "Dipartimento",
                value = dept
            )
        }

        // INFO
        contact.email?.let { email ->
            ContactMethodItem(
                icon = Icons.Default.Email,
                label = "EMail",
                value = email,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri())
                    context.startActivity(intent)
                },
                isPrimary = contact.preferredContactMethod == ContactMethod.PHONE
            )
        }

        // PHONE
        contact.phone?.let { phone ->
            ContactMethodItem(
                icon = Icons.Default.Phone,
                label = "Telefono",
                value = phone,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                    context.startActivity(intent)
                },
                isPrimary = contact.preferredContactMethod == ContactMethod.PHONE
            )
        }

        // MOBILE
        contact.mobilePhone?.let { mobile ->
            ContactMethodItem(
                icon = Icons.Default.Phone,
                label = "Telefono",
                value = mobile,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, "tel:$mobile".toUri())
                    context.startActivity(intent)
                },
                isPrimary = contact.preferredContactMethod == ContactMethod.PHONE
            )
        }
    }
}

@Composable
private fun CompactContactCard(
    contact: Contact,
    onCall: () -> Unit = { },
    onEmail: () -> Unit = { }
) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = contact.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (contact.isPrimary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stabilimento primario",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ROLE
            contact.roleDescription.takeIf { it.isNotBlank() }?.let { role ->
                Row {
                    Text(
                        text = role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // PHONE
            contact.phone?.let { phone ->
                Row {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Chiama",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // EMAIL
            contact.email?.let { email ->
                Row {
                    IconButton(
                        onClick = onEmail,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Invia email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalContactCard(contact: Contact) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = contact.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.isPrimary) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Contatto primario",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Varianti di visualizzazione per FacilityCard
 */
enum class ContactCardVariant {
    FULL,       // Card completa con tutte le informazioni
    COMPACT,    // Card compatta per liste dense
    MINIMAL     // Card minimalista per selezioni
}