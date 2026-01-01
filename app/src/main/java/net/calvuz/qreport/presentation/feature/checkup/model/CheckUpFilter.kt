package net.calvuz.qreport.presentation.feature.checkup.model

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R

enum class CheckUpFilter {
    ALL, DRAFT, IN_PROGRESS, COMPLETED, EXPORTED, ARCHIVED
}

fun CheckUpFilter.getDisplayName(context: Context): String {
    return when (this) {
        CheckUpFilter.ALL -> context.getString(R.string.enum_checkup_status_filter_all)
        CheckUpFilter.DRAFT -> context.getString(R.string.enum_checkup_status_draft)
        CheckUpFilter.IN_PROGRESS -> context.getString(R.string.enum_checkup_status_in_progress)
        CheckUpFilter.COMPLETED -> context.getString(R.string.enum_checkup_status_completed)
        CheckUpFilter.EXPORTED -> context.getString(R.string.enum_checkup_status_exported)
        CheckUpFilter.ARCHIVED -> context.getString(R.string.enum_checkup_status_archived)
    }
}

@Composable
fun CheckUpFilter.getDisplayName(): String {
    return when (this) {
        CheckUpFilter.ALL -> stringResource(R.string.enum_checkup_status_filter_all)
        CheckUpFilter.DRAFT -> stringResource(R.string.enum_checkup_status_draft)
        CheckUpFilter.IN_PROGRESS -> stringResource(R.string.enum_checkup_status_in_progress)
        CheckUpFilter.COMPLETED -> stringResource(R.string.enum_checkup_status_completed)
        CheckUpFilter.EXPORTED -> stringResource(R.string.enum_checkup_status_exported)
        CheckUpFilter.ARCHIVED -> stringResource(R.string.enum_checkup_status_archived)
    }
}