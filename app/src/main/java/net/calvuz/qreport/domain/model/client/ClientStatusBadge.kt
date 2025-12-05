package net.calvuz.qreport.domain.model.client

import kotlinx.serialization.Serializable

/**
 * Badge di stato per UI
 */
@Serializable
data class ClientStatusBadge(
    val text: String,
    val color: String
)