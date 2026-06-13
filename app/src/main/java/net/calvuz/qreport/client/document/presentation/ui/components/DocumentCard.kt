package net.calvuz.qreport.client.document.presentation.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.app.util.SizeUtils.getFormattedSize
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.Document
import net.calvuz.qreport.client.document.presentation.model.displayLabel
import kotlinx.datetime.Instant

/** Card display variant — follows the project-wide FULL/COMPACT/MINIMAL pattern. */
enum class DocumentCardVariant { FULL, COMPACT, MINIMAL }

/**
 * Displays a single [Document] as a card.
 *
 * - FULL: title, category chip, MIME type, size, date, notes, action buttons
 * - COMPACT: title, category chip, date, size (default for list view)
 * - MINIMAL: title only (for dense search results)
 *
 * Long-press enters selection mode when [onLongPress] is provided.
 * [isSelected] shows a checkbox and highlights the card border.
 */
@Composable
fun DocumentCard(
    modifier: Modifier = Modifier,
    document: Document,
    variant: DocumentCardVariant = DocumentCardVariant.COMPACT,
    isSelected: Boolean = false,
    onOpen: (Document) -> Unit,
    onEdit: ((Document) -> Unit)? = null,
    onDelete: ((Document) -> Unit)? = null,
    onLongPress: ((Document) -> Unit)? = null
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick    = { onOpen(document) },
                onLongClick = { onLongPress?.invoke(document) }
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        when (variant) {
            DocumentCardVariant.MINIMAL  -> MinimalContent(document, isSelected)
            DocumentCardVariant.COMPACT  -> CompactContent(document, isSelected)
            DocumentCardVariant.FULL     -> FullContent(document, isSelected, onEdit, onDelete)
        }
    }
}

// ── MINIMAL ───────────────────────────────────────────────────────────────────

@Composable
private fun MinimalContent(
    document: Document,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isSelected) {
            Checkbox(checked = true, onCheckedChange = null)
        } else {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = document.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── COMPACT ───────────────────────────────────────────────────────────────────

@Composable
private fun CompactContent(
    document: Document,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Leading icon or checkbox
        if (isSelected) {
            Checkbox(checked = true, onCheckedChange = null, modifier = Modifier.size(24.dp))
        } else {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryChip(document.category)
                Text(
                    text = (Instant.fromEpochMilliseconds(document.createdAt)).toItalianDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = (document.fileSize.getFormattedSize()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── FULL ──────────────────────────────────────────────────────────────────────

@Composable
private fun FullContent(
    document: Document,
    isSelected: Boolean,
    onEdit: ((Document) -> Unit)?,
    onDelete: ((Document) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Checkbox(checked = true, onCheckedChange = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = document.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category + MIME + size
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(document.category)
            Text(
                text = document.mimeType.substringAfterLast('/').uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = (document.fileSize.getFormattedSize()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = Instant.fromEpochMilliseconds(document.createdAt).toItalianDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Notes
        if (!document.notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = document.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action buttons (48dp touch targets — glove-friendly)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            onEdit?.let {
                IconButton(onClick = { it(document) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            onDelete?.let {
                IconButton(onClick = { it(document) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = { /* open handled by card click */ }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.OpenInNew,
                    contentDescription = stringResource(R.string.action_open),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun CategoryChip(category: DocumentCategory) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = category.displayLabel().asString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    )
}
