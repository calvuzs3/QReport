package net.calvuz.qreport.client.client.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportFilter

enum class ClientFilter: QReportFilter {
    ALL, ACTIVE, INACTIVE, WITH_FACILITIES, WITH_ISLANDS, WITH_CONTACTS, WITH_CONTRACTS
}

// Extension
fun ClientFilter.getDisplayName(): String {
    return when (this) {
        ClientFilter.ALL -> "Tutti"
        ClientFilter.ACTIVE -> "Attivi"
        ClientFilter.INACTIVE -> "Inattivi"
        ClientFilter.WITH_FACILITIES -> "Con Stabilimenti"
        ClientFilter.WITH_CONTACTS -> "Con Contatti"
        ClientFilter.WITH_CONTRACTS -> "Con Contratti"
        ClientFilter.WITH_ISLANDS -> "Con Isole"
    }
}