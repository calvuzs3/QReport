package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ContractBackup (
    val id: String,
    val clientId: String,
    val name: String?,
    val description: String?,
    @Contextual val startDate: Instant,
    @Contextual val endDate: Instant,
    val hasPriority: Boolean,
    val hasRemoteAssistance: Boolean,
    val hasMaintenance: Boolean,
    val notes: String?,
    val isActive: Boolean,
    @Contextual    val createdAt: Instant,
    @Contextual    val updatedAt: Instant
)