package net.calvuz.qreport.checkup.domain.model

import kotlinx.serialization.Serializable

/**
 * Client Infos
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