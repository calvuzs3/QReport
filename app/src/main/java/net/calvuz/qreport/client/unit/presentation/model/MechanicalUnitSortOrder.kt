package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder
import net.calvuz.qreport.app.error.presentation.UiText

/**
 * Sort options for the mechanical unit list.
 * Labels resolved via stringResource() in the UI.
 */
enum class MechanicalUnitSortOrder(val labelResId: Int) : QReportSortOrder {
    CREATED_RECENT(R.string.unit_sort_created_recent),
    NAME(R.string.unit_sort_name),
    BY_TYPE(R.string.unit_sort_by_type),
    BY_SERIAL(R.string.unit_sort_by_serial);

    override fun getDisplayName(): UiText = when (this) {
        CREATED_RECENT -> UiText.StringResource(R.string.unit_sort_created_recent)
        NAME -> UiText.StringResource(R.string.unit_sort_name)
        BY_TYPE -> UiText.StringResource(R.string.unit_sort_by_type)
        BY_SERIAL -> UiText.StringResource(R.string.unit_sort_by_serial)
    }
}