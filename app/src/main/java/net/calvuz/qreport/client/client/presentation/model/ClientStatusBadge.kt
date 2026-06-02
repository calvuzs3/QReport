package net.calvuz.qreport.client.client.presentation.model

import kotlinx.serialization.Serializable

/**
 * Status badge for client UI display.
 *
 * Note: [color] is kept as a raw string for serialization compatibility,
 * but it should only be used as a semantic key (e.g. "active", "inactive"),
 * never as a hex color. The actual color is resolved in the composable
 * via MaterialTheme tokens.
 */
@Serializable
data class ClientStatusBadge(
    val text: String,
    val color: String      // semantic key, not hex — resolve to theme token in UI
)