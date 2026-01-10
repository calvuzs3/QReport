package net.calvuz.qreport.client.contract.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Contract(
    val id: String,
    val clientId: String,
    val name: String? = null,
    val description: String? = null,
    val startDate: Instant,
    val endDate: Instant,
    val hasPriority: Boolean = true,           // rensponse priority (48h)
    val hasRemoteAssistance: Boolean = true,   // remote assistance (24h)
    val hasMaintenance: Boolean = true,        // island|rob maintenance
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)
