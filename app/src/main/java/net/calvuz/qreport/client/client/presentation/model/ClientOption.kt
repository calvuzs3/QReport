package net.calvuz.qreport.client.client.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportFilter

/**
 * Wrapper for Client data used in filter dropdowns.
 * Implements [QReportFilter] so it can be used with [QReportClientMenu].
 *
 * @param id Client ID, empty string for the "all clients" sentinel.
 * @param companyName Display name shown in the dropdown.
 */
data class ClientOption(
    val id: String,
    val companyName: String
) : QReportFilter {

    override fun getDisplayName(): String = companyName

    companion object {
        /** Sentinel value meaning "no client filter — show all facilities". */
        val ALL = ClientOption(id = "", companyName = "Tutti i Clienti")
    }
}
