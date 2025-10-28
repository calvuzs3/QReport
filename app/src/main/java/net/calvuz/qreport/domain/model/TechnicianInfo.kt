package net.calvuz.qreport.domain.model

import kotlinx.serialization.Serializable

/**
 * Informazioni tecnico manutentore
 */
@Serializable
data class TechnicianInfo(
    val name: String,
    val company: String = "Polytec",
    val certification: String = "",
    val phone: String = "",
    val email: String = ""
)