package net.calvuz.qreport.client.island.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

enum class IslandFilter : QReportFilter {
    ALL, ACTIVE, INACTIVE, MAINTENANCE_DUE, UNDER_WARRANTY, HIGH_OPERATING_HOURS, BY_TYPE;

    override fun getDisplayName(): UiText = when (this) {
        ALL -> UiText.StringResources(R.string.island_filter_all)
        ACTIVE -> UiText.StringResources(R.string.island_filter_active)
        INACTIVE -> UiText.StringResources(R.string.island_filter_inactive)
        MAINTENANCE_DUE -> UiText.StringResources(R.string.island_filter_maintenance_due)
        UNDER_WARRANTY -> UiText.StringResources(R.string.island_filter_under_warranty)
        HIGH_OPERATING_HOURS -> UiText.StringResources(R.string.island_filter_high_operating_hours)


        BY_TYPE -> UiText.StringResources(R.string.island_filter_by_type)
    }
}