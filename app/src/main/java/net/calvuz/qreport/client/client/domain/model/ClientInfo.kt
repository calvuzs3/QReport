package net.calvuz.qreport.client.client.domain.model

import kotlinx.serialization.Serializable

/**
 * Informazioni cliente
 */
@Deprecated(
    message = "No longer use it",
    level = DeprecationLevel.WARNING
)
@Serializable
data class ClientInfo(
    val companyName: String,
    val contactPerson: String = "",
    val site: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = ""
)