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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import net.calvuz.qreport.R
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod
import net.calvuz.qreport.app.app.presentation.components.DeleteDialog
import net.calvuz.qreport.app.app.presentation.components.PrimaryBadge
import net.calvuz.qreport.app.app.presentation.components.list.QrListItemCard.QrListItemCardVariant

@Composable
fun ContactCard(
    modifier: Modifier = Modifier,
    contact: Contact,
    onClick: () -> Unit,
    showActions: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onEmail: () -> Unit = { },
    onSetPrimary: (() -> Unit)? = null,
    isSettingPrimary: Boolean,
    variant: QrListItemCardVariant = QrListItemCardVariant.FULL
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
//        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (variant) {
            QrListItemCardVariant.FULL -> FullContactCard(
                contact = contact,
                showActions = showActions,
                onDelete = onDelete,
                onEdit = onEdit,
                onSetPrimary = onSetPrimary,
                isSettingPrimary = isSettingPrimary,
            )

            QrListItemCardVariant.COMPACT -> CompactContactCard(
                contact = contact,
            )

            QrListItemCardVariant.MINIMAL -> MinimalContactCard(contact = contact)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        DeleteDialog(
            title = stringResource(R.string.contact_delete_dialog_title),
            text = stringResource(R.string.contact_delete_dialog_text, contact.fullName),
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
    onSetPrimary: (() -> Unit)? = null,
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
                    if (!contact.isPrimary && contact.isActive && onSetPrimary != null) {
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
                                    contentDescription = stringResource(R.string.action_set_as_primary),
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
                                contentDescription = stringResource(R.string.action_edit),
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
                                contentDescription = stringResource(R.string.action_delete),
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
                        Text(stringResource(R.string.label_active), style = MaterialTheme.typography.labelSmall)
                    },
                    enabled = false
                )
            }
        }

        // ROLE
        contact.role?.let { role ->
            ContactRoleItem(
                icon = Icons.Default.Work,
                label = stringResource(R.string.contact_field_role),
                value = role
            )
        }

        // DEPARTMENT
        contact.department?.let { dept ->
            ContactRoleItem(
                icon = Icons.Default.Business,
                label = stringResource(R.string.contact_field_department),
                value = dept
            )
        }

        // INFO
        contact.email?.let { email ->
            ContactMethodItem(
                icon = Icons.Default.Email,
                label = stringResource(R.string.contact_field_email),
                value = email,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, context.getString(R.string.intent_action_mailto, email).toUri())
                    context.startActivity(intent)
                },
                isPrimary = contact.preferredContactMethod == ContactMethod.PHONE
            )
        }

        // PHONE
        contact.phone?.let { phone ->
            ContactMethodItem(
                icon = Icons.Default.Phone,
                label = stringResource(R.string.contact_field_phone),
                value = phone,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, context.getString(R.string.intent_action_dial, phone).toUri())
                    context.startActivity(intent)
                },
                isPrimary = contact.preferredContactMethod == ContactMethod.PHONE
            )
        }

        // MOBILE
        contact.mobilePhone?.let { mobile ->
            ContactMethodItem(
                icon = Icons.Default.Phone,
                label = stringResource(R.string.contact_field_mobile),
                value = mobile,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, context.getString(R.string.intent_action_dial, mobile).toUri())
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
                        contentDescription = stringResource(R.string.contact_primary_facility),
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
                            contentDescription = stringResource(R.string.action_call),
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
                            contentDescription = stringResource(R.string.action_send_email),
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
                        contentDescription = stringResource(R.string.contact_primary_contact),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}