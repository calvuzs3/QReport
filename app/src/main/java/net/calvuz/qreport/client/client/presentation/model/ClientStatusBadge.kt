package net.calvuz.qreport.client.client.presentation.model

import kotlinx.serialization.Serializable

/**
 * Badge di stato per UI
 */
@Serializable
data class ClientStatusBadge(
    val text: String,
    val color: String
)