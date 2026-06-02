package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

/**
 * Sort options for the mechanical unit list.
 * Labels resolved via stringResource() in the UI.
 */
enum class MechanicalUnitSortOrder(val labelResId: Int) : QReportSortOrder {
    CREATED_RECENT(R.string.unit_sort_created_recent),
    NAME(R.string.unit_sort_name),
    BY_TYPE(R.string.unit_sort_by_type),
    BY_SERIAL(R.string.unit_sort_by_serial);

    override fun getDisplayName(): String = name // fallback; UI uses labelResId
}