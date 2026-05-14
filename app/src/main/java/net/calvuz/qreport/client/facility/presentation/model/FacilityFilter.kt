package net.calvuz.qreport.client.facility.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportFilter

enum class FacilityFilter: QReportFilter {
    ALL, ACTIVE, INACTIVE, PRIMARY_ONLY, WITH_ISLANDS, BY_TYPE;

    override fun getDisplayName(): String = when (this) {
        ALL -> "Tutti"
        ACTIVE -> "Attivi"
        INACTIVE -> "Inattivi"
        PRIMARY_ONLY -> "Solo Primari"
        WITH_ISLANDS -> "Con Isole"
        BY_TYPE -> "Per Tipo"
    }
}