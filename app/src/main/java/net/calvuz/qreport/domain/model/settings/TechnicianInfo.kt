package net.calvuz.qreport.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Informazioni tecnico manutentore
 */
@Serializable
data class TechnicianInfo(
    val name: String = "",
    val company: String = "",
    val certification: String = "",
    val phone: String = "",
    val email: String = ""
)