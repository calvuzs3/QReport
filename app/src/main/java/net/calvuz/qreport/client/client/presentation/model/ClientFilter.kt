package net.calvuz.qreport.client.client.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportFilter

enum class ClientFilter: QReportFilter {
    ALL, ACTIVE, INACTIVE, WITH_FACILITIES, WITH_ISLANDS, WITH_CONTACTS, WITH_CONTRACTS;

    override fun getDisplayName(): String = when (this) {
        ALL -> "Tutti"
        ACTIVE -> "Attivi"
        INACTIVE -> "Inattivi"
        WITH_FACILITIES -> "Con Stabilimenti"
        WITH_CONTACTS -> "Con Contatti"
        WITH_CONTRACTS -> "Con Contratti"
        WITH_ISLANDS -> "Con Isole"
    }
}