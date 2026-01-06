package net.calvuz.qreport.backup.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import net.calvuz.qreport.R
import net.calvuz.qreport.backup.domain.model.enum.BackupMode
import net.calvuz.qreport.app.error.presentation.UiText

object BackupModeExt {

    fun BackupMode.getDisplayName(): UiText {
        return when (this) {
            BackupMode.LOCAL -> UiText.StringResources(R.string.backup_backup_mode_local_name)
        }
    }

    fun BackupMode.getDescription(): UiText {
        return when (this) {
            BackupMode.LOCAL -> UiText.StringResources(R.string.backup_backup_mode_local_desc)
        }
    }

    fun BackupMode.getIcon(): ImageVector {
        return when (this) {
            BackupMode.LOCAL -> Icons.Default.Storage
        }
    }
}