package net.calvuz.qreport.domain.model.client

import kotlinx.serialization.Serializable

/**
 * Informazioni cliente
 */
@Serializable
data class ClientInfo(
    val companyName: String,
    val contactPerson: String = "",
    val site: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = ""
)