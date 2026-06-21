package net.calvuz.qreport.checkup.checkup.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster

/**
 * A checkup list filter targeting one status, or every status when [statusId] is
 * null. Replaces the old fixed enum: the set of available filters now mirrors
 * whatever statuses exist in [net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster]
 * (editable from Settings), built by the ViewModel as `listOf(ALL) + statuses.map { CheckUpFilter(it.id) }`.
 */
data class CheckUpFilter(val statusId: String?) {
    companion object {
        val ALL = CheckUpFilter(null)
    }
}

@Composable
fun CheckUpFilter.getDisplayName(statusMasters: List<CheckUpStatusMaster>): String {
    val id = statusId ?: return stringResource(R.string.enum_checkup_status_filter_all)
    return statusMasters.find { it.id == id }?.label ?: id
}
