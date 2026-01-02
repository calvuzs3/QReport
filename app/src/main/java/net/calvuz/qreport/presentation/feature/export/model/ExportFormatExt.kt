package net.calvuz.qreport.presentation.feature.export.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.model.export.ExportFormat

@Composable
fun ExportFormat.getDisplayName(): String {
    return when (this) {
        ExportFormat.WORD -> stringResource(R.string.export_screen_format_word_name)
        ExportFormat.TEXT -> stringResource(R.string.export_screen_format_text_name)
        ExportFormat.PHOTO_FOLDER -> stringResource(R.string.export_screen_format_photos_name)
        ExportFormat.COMBINED_PACKAGE -> stringResource(R.string.export_screen_format_package_name)
    }
}

@Composable
fun ExportFormat.getDescription(): String {
    return when (this) {
        ExportFormat.WORD -> stringResource(R.string.export_screen_format_word_desc)
        ExportFormat.TEXT -> stringResource(R.string.export_screen_format_text_desc)
        ExportFormat.PHOTO_FOLDER -> stringResource(R.string.export_screen_format_photos_desc)
        ExportFormat.COMBINED_PACKAGE -> stringResource(R.string.export_screen_format_package_desc)
    }
}

val ExportFormat.icon: ImageVector
    get() = when (this) {
        ExportFormat.WORD -> Icons.Default.Description
        ExportFormat.TEXT -> Icons.AutoMirrored.Default.TextSnippet
        ExportFormat.PHOTO_FOLDER -> Icons.Default.PhotoLibrary
        ExportFormat.COMBINED_PACKAGE -> Icons.Default.Archive
    }

val ExportFormat.supportsPhotos: Boolean
    get() = when (this) {
        ExportFormat.WORD, ExportFormat.PHOTO_FOLDER, ExportFormat.COMBINED_PACKAGE -> true
        ExportFormat.TEXT -> false
    }

val ExportFormat.color: Color
    get() = when (this) {
        ExportFormat.WORD ->  Color (0xFF2E7D32)
        ExportFormat.TEXT ->  Color(0xFF1976D2)
        ExportFormat.PHOTO_FOLDER ->  Color(0xFFE65100)
        ExportFormat.COMBINED_PACKAGE ->  Color(0xFF6A1B9A)
    }