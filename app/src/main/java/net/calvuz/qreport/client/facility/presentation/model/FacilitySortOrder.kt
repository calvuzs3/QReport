package net.calvuz.qreport.client.facility.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder
import net.calvuz.qreport.app.error.presentation.UiText

enum class FacilitySortOrder: QReportSortOrder {
    NAME, CREATED_RECENT, CREATED_OLDEST, ISLANDS_COUNT, TYPE;

    override fun getDisplayName(): String = when( this) {
        NAME -> UiText.StringResources(R.string.facility_sort_facility_name).toString()
        CREATED_RECENT -> UiText.StringResources(R.string.facility_sort_created_recent).toString()
        CREATED_OLDEST -> UiText.StringResources(R.string.facility_sort_created_oldest).toString()
        ISLANDS_COUNT -> UiText.StringResources(R.string.facility_sort_islands_count).toString()
        TYPE -> UiText.StringResources(R.string.facility_sort_by_type).toString()
    }
}