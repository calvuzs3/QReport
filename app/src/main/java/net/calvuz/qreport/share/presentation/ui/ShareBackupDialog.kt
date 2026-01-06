package net.calvuz.qreport.share.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.calvuz.qreport.share.domain.repository.ShareMethod
import net.calvuz.qreport.share.domain.repository.ShareOptionOldVersion
import net.calvuz.qreport.share.domain.repository.ShareOptionType

/**
 * ✅ CORRECTED: Dialog con enum usage corretti
 *
 * ShareMethod = Modalità condivisione (SINGLE_FILE, DIRECT, COMPRESSED)
 * ShareOptionType = Tipo UI opzione (FILE_OPTION, APP_SPECIFIC, etc)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBackupDialog(
    backupPath: String,
    backupName: String,
    shareOptionOldVersions: List<ShareOptionOldVersion>,
    isLoading: Boolean = false,
    onShareSelected: (ShareOptionOldVersion) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with backup info
                ShareDialogHeader(
                    backupName = backupName,
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Loading state
                if (isLoading) {
                    ShareLoadingState()
                } else {
                    // ✅ CORRECTED: Grouped share options using correct enums
                    ShareOptionsContent(
                        shareOptionOldVersions = shareOptionOldVersions,
                        onShareSelected = onShareSelected
                    )
                }

                // Info footer
                if (!isLoading && shareOptionOldVersions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ShareInfoFooter()
                }
            }
        }
    }
}

/**
 * Header with backup information
 */
@Composable
private fun ShareDialogHeader(
    backupName: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Condividi Backup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = backupName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Chiudi")
        }
    }
}

/**
 * Loading state
 */
@Composable
private fun ShareLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Preparazione opzioni condivisione...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * ✅ CORRECTED: Share options content with corrected grouping
 */
@Composable
private fun ShareOptionsContent(
    shareOptionOldVersions: List<ShareOptionOldVersion>,
    onShareSelected: (ShareOptionOldVersion) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ✅ GROUP 1: File options (using ShareOptionType.FILE_OPTION)
        val fileOptions = shareOptionOldVersions.filter {
            it.type == ShareOptionType.FILE_OPTION  // ✅ Corrected filter
        }

        if (fileOptions.isNotEmpty()) {
            item {
                Text(
                    text = "Modalità Condivisione",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(fileOptions) { option ->
                EnhancedShareOptionItem(
                    option = option,
                    onClick = { onShareSelected(option) },
                    isRecommended = option.shareMethod == ShareMethod.DIRECT  // ✅ Use ShareMethod
                )
            }
        }

        // ✅ GROUP 2: App options
        val appOptions = shareOptionOldVersions.filter {
            it.type in listOf(ShareOptionType.APP_GENERIC, ShareOptionType.APP_SPECIFIC)  // ✅ Corrected filter
        }

        if (appOptions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "App di Condivisione",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(appOptions) { option ->
                EnhancedShareOptionItem(
                    option = option,
                    onClick = { onShareSelected(option) }
                )
            }
        }
    }
}

/**
 * ✅ CORRECTED: Share option item with corrected logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedShareOptionItem(
    option: ShareOptionOldVersion,
    onClick: () -> Unit,
    isRecommended: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isRecommended) {
            CardDefaults.outlinedCardBorder().copy(width = 2.dp)
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (option.icon != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(option.icon)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else if(option.ivIcon != null) {
                Icon(
                    imageVector = option.ivIcon,  // ✅ Use both enums
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = getIconForOption(option.type, option.shareMethod),  // ✅ Use both enums
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

//                    if (isRecommended) {
//                        AssistChip(
//                            onClick = { },
//                            label = {
//                                Text(
//                                    text = "Consigliato",
//                                    style = MaterialTheme.typography.labelSmall
//                                )
//                            },
//                            enabled = false,
//                            colors = AssistChipDefaults.assistChipColors(
//                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
//                                disabledLabelColor = MaterialTheme.colorScheme.primary
//                            )
//                        )
//                    }
                }

                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Share mode indicator
            ShareModeChip(shareMethod = option.shareMethod)  // ✅ Use ShareMethod
        }
    }
}

/**
 * Info footer
 */
@Composable
private fun ShareInfoFooter() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Il backup completo include tutti i file necessari per un ripristino completo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * ✅ CORRECTED: Chip che mostra ShareMethod
 */
@Composable
private fun ShareModeChip(
    shareMethod: ShareMethod,  // ✅ Use existing ShareMethod
    modifier: Modifier = Modifier
) {
    val (text, color) = when (shareMethod) {
        ShareMethod.DIRECT -> "Diretto" to MaterialTheme.colorScheme.primary
        ShareMethod.COMPRESSED -> "ZIP" to MaterialTheme.colorScheme.secondary
        ShareMethod.CLOUD_UPLOAD -> "Upload to Cloud" to MaterialTheme.colorScheme.tertiary
        ShareMethod.EMAIL_ATTACHMENT -> "Allegato Email" to MaterialTheme.colorScheme.tertiary
        ShareMethod.MESSAGING_ATTACHMENT -> "Allegato messaggio" to MaterialTheme.colorScheme.tertiary
    }

    AssistChip(
        onClick = { /* No-op, solo indicativo */ },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier,
        enabled = false,
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.12f),
            disabledLabelColor = color
        )
    )
}

/**
 * ✅ CORRECTED: Icons using both enums appropriately
 */
private fun getIconForOption(optionType: ShareOptionType, shareMethod: ShareMethod): ImageVector {
    return when (optionType) {
        ShareOptionType.FILE_OPTION -> {
            when (shareMethod) {
                ShareMethod.DIRECT -> Icons.Default.Archive
                ShareMethod.COMPRESSED -> Icons.Default.FolderZip
                ShareMethod.CLOUD_UPLOAD -> Icons.Default.CloudUpload
                ShareMethod.EMAIL_ATTACHMENT -> Icons.Default.AttachFile
                ShareMethod.MESSAGING_ATTACHMENT -> Icons.Default.Attachment
            }
        }
        ShareOptionType.APP_GENERIC -> Icons.Default.Share
        ShareOptionType.APP_SPECIFIC -> Icons.Default.Apps
        ShareOptionType.QUICK_ACTION -> Icons.Default.Bolt
    }
}

/**
 * Quick share button per azioni rapide
 */
@Composable
fun QuickShareButton(
    onShare: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onShare,
        enabled = isEnabled,
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Condividi")
    }
}