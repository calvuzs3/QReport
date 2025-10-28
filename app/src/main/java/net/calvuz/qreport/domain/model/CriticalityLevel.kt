package net.calvuz.qreport.domain.model

import kotlinx.serialization.Serializable

/**
 * Livello di criticità per i check items
 */
@Serializable
enum class CriticalityLevel(
    val displayName: String,
    val priority: Int,
    val color: String
) {
    ROUTINE(
        displayName = "Routine",
        priority = 1,
        color = "#2196F3"  // Blue
    ),
    IMPORTANT(
        displayName = "Importante",
        priority = 2,
        color = "#FF9800"  // Orange
    ),
    CRITICAL(
        displayName = "Critico",
        priority = 3,
        color = "#F44336"  // Red
    ),
    NA(
        displayName = "N/A",
        priority = 0,
        color = "#9E9E9E"  // Grey
    )
}