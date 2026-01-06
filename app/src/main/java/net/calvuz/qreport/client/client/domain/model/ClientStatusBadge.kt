package net.calvuz.qreport.client.client.domain.model

import kotlinx.serialization.Serializable

/**
 * Badge di stato per UI
 */
@Serializable
data class ClientStatusBadge(
    val text: String,
    val color: String
)