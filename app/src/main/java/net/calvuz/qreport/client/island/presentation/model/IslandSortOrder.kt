package net.calvuz.qreport.client.island.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder
import net.calvuz.qreport.app.error.presentation.UiText

enum class IslandSortOrder(): QReportSortOrder {
    CUSTOM_NAME,
    CREATED_RECENT,
    CREATED_OLDEST,
    SERIAL_NUMBER,
    TYPE,
    STATUS,
    OPERATING_HOURS,
    MAINTENANCE_DATE;

    override fun getDisplayName(): String = when (this) {
        CUSTOM_NAME -> UiText.StringResources(R.string.island_sort_custom_name).toString()
        CREATED_RECENT -> UiText.StringResources(R.string.island_sort_created_recent).toString()
        CREATED_OLDEST -> UiText.StringResources(R.string.island_sort_created_oldest).toString()
        SERIAL_NUMBER -> UiText.StringResources(R.string.island_sort_serial_number).toString()
        TYPE -> UiText.StringResources(R.string.island_sort_by_type).toString()
        STATUS -> UiText.StringResources(R.string.island_sort_by_status).toString()
        OPERATING_HOURS -> UiText.StringResources(R.string.island_sort_by_operating_hours).toString()
        MAINTENANCE_DATE -> UiText.StringResources(R.string.island_sort_by_maintenance_date).toString()
    }
}
