package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter

/**
 * Filter options for the mechanical unit list.
 * Labels resolved via stringResource() in the UI.
 */
enum class MechanicalUnitFilter(val labelResId: Int) : QReportFilter {
    ALL(R.string.unit_filter_all),
    ACTIVE(R.string.unit_filter_active),
    INACTIVE(R.string.unit_filter_inactive),
    ROBOT(R.string.unit_filter_robot);

    override fun getDisplayName(): String = name // fallback; UI uses labelResId
}