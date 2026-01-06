package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.DialogProperties

// ============================================================
// 1. ERROR/INFO DIALOG - Solo informativo
// ============================================================

enum class MessageType {
    ERROR,
    WARNING,
    INFO,
    SUCCESS
}

/**
 * Dialog informativo (Error, Warning, Info, Success)
 * - Solo 1 bottone "OK" per chiudere
 * - Non richiede conferma o azione
 */
@Composable
fun MessageDialog(
    type: MessageType,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = stringResource(R.string.action_ok),
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true
) {
    val config = getMessageConfig(type)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        ),
        icon = {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.iconTint
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = config.titleColor
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = config.buttonColor
                )
            ) {
                Text(buttonText)
            }
        }
    )
}

@Composable
private fun getMessageConfig(type: MessageType): DialogConfig {
    return when (type) {
        MessageType.ERROR -> DialogConfig(
            icon = Icons.Default.Error,
            iconTint = MaterialTheme.colorScheme.error,
            titleColor = MaterialTheme.colorScheme.error,
            buttonColor = MaterialTheme.colorScheme.error
        )
        MessageType.WARNING -> DialogConfig(
            icon = Icons.Default.Warning,
            iconTint = Color(0xFFFF9800),
            titleColor = Color(0xFFE65100),
            buttonColor = Color(0xFFFF9800)
        )
        MessageType.INFO -> DialogConfig(
            icon = Icons.Default.Info,
            iconTint = MaterialTheme.colorScheme.primary,
            titleColor = MaterialTheme.colorScheme.primary,
            buttonColor = MaterialTheme.colorScheme.primary
        )
        MessageType.SUCCESS -> DialogConfig(
            icon = Icons.Default.CheckCircle,
            iconTint = Color(0xFF4CAF50),
            titleColor = Color(0xFF2E7D32),
            buttonColor = Color(0xFF4CAF50)
        )
    }
}

private data class DialogConfig(
    val icon: ImageVector,
    val iconTint: Color,
    val titleColor: Color,
    val buttonColor: Color
)

/**
 * Shortcut: ErrorDialog
 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = stringResource(R.string.action_ok)
) {
    MessageDialog(
        type = MessageType.ERROR,
        title = title,
        message = message,
        onDismiss = onDismiss,
        buttonText = buttonText
    )
}

/**
 * Shortcut: WarningDialog
 */
@Composable
fun WarningDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = stringResource(R.string.action_ok)
) {
    MessageDialog(
        type = MessageType.WARNING,
        title = title,
        message = message,
        onDismiss = onDismiss,
        buttonText = buttonText
    )
}

/**
 * Shortcut: InfoDialog
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = stringResource(R.string.action_ok)
) {
    MessageDialog(
        type = MessageType.INFO,
        title = title,
        message = message,
        onDismiss = onDismiss,
        buttonText = buttonText
    )
}

/**
 * Shortcut: SuccessDialog
 */
@Composable
fun SuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = stringResource(R.string.action_ok)
) {
    MessageDialog(
        type = MessageType.SUCCESS,
        title = title,
        message = message,
        onDismiss = onDismiss,
        buttonText = buttonText
    )
}


// ============================================================
// 2. CONFIRMATION DIALOG - Richiede conferma
// ============================================================

/**
 * Dialog di conferma con 2 bottoni
 * - Richiede una decisione dall'utente
 * - Bottoni: "Conferma" + "Annulla"
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.action_confirm),
    dismissButtonText: String = stringResource(R.string.action_cancel),
    isDestructive: Boolean = false, // true = azione pericolosa (es. eliminazione)
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        ),
        icon = {
            Icon(
                imageVector = if (isDestructive) Icons.Default.Warning else Icons.AutoMirrored.Default.Help,
                contentDescription = null,
                tint = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}


// ============================================================
// 3. ACTION DIALOG - Offre azioni alternative
// ============================================================

/**
 * Dialog con azioni alternative
 * - Offre più opzioni all'utente
 * - Bottoni: "Riprova" + "Annulla", oppure "Riprova" + "Ignora"
 */
@Composable
fun ActionDialog(
    title: String,
    message: String,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit,
    primaryActionText: String = stringResource(R.string.action_retry),
    dismissButtonText: String = stringResource(R.string.action_cancel),
    icon: ImageVector = Icons.Default.Error,
    iconTint: Color = MaterialTheme.colorScheme.error,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        ),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = iconTint
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onPrimaryAction()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconTint
                )
            ) {
                Text(primaryActionText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}