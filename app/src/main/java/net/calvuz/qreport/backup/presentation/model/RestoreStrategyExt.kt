package net.calvuz.qreport.backup.presentation.model

import net.calvuz.qreport.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.ui.graphics.vector.ImageVector
import net.calvuz.qreport.backup.domain.model.enum.RestoreStrategy
import net.calvuz.qreport.app.error.presentation.UiText

object RestoreStrategyExt {

    fun RestoreStrategy.getDisplayName(): UiText {
        return when (this) {
            RestoreStrategy.REPLACE_ALL -> UiText.StringResources(R.string.backup_restore_strategy_replace_all_name)
            RestoreStrategy.MERGE -> UiText.StringResources(R.string.backup_restore_strategy_merge_name)
            RestoreStrategy.SELECTIVE -> UiText.StringResources(R.string.backup_restore_strategy_selective_name)
        }
    }

    fun RestoreStrategy.getDescription(): UiText {
        return when (this) {
            RestoreStrategy.REPLACE_ALL -> UiText.StringResources(R.string.backup_restore_strategy_replace_all_desc)
            RestoreStrategy.MERGE -> UiText.StringResources(R.string.backup_restore_strategy_merge_desc)
            RestoreStrategy.SELECTIVE -> UiText.StringResources(R.string.backup_restore_strategy_selective_desc)
        }
    }

    fun RestoreStrategy.getIcon(): ImageVector {
        return when (this) {
            RestoreStrategy.REPLACE_ALL -> Icons.Default.Delete
            RestoreStrategy.MERGE -> Icons.Default.Merge
            RestoreStrategy.SELECTIVE -> Icons.Default.SelectAll
        }
    }
}