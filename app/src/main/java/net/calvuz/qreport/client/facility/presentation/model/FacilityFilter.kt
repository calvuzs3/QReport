package net.calvuz.qreport.client.facility.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

enum class FacilityFilter: QReportFilter {
    ALL, ACTIVE, INACTIVE, PRIMARY_ONLY, WITH_ISLANDS, BY_TYPE;

    override fun getDisplayName(): UiText = when (this) {
        ALL -> UiText.StringResources(R.string.facility_filter_all)
        ACTIVE -> UiText.StringResources(R.string.facility_filter_active)
        INACTIVE -> UiText.StringResources(R.string.facility_filter_inactive)
        PRIMARY_ONLY -> UiText.StringResources(R.string.facility_filter_primary_only)
        WITH_ISLANDS -> UiText.StringResources(R.string.facility_filter_with_islands)
        BY_TYPE -> UiText.StringResources(R.string.facility_filter_by_type)
    }
}