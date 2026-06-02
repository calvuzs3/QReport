package net.calvuz.qreport.client.facility.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

enum class FacilityFilter: QReportFilter {
    ALL, ACTIVE, INACTIVE, PRIMARY_ONLY, WITH_ISLANDS, BY_TYPE;

    override fun getDisplayName(): String = when (this) {
        ALL -> UiText.StringResources(R.string.facility_filter_all).toString()
        ACTIVE -> UiText.StringResources(R.string.facility_filter_active).toString()
        INACTIVE -> UiText.StringResources(R.string.facility_filter_inactive).toString()
        PRIMARY_ONLY -> UiText.StringResources(R.string.facility_filter_primary_only).toString()
        WITH_ISLANDS -> UiText.StringResources(R.string.facility_filter_with_islands).toString()
        BY_TYPE -> UiText.StringResources(R.string.facility_filter_by_type).toString()
    }
}