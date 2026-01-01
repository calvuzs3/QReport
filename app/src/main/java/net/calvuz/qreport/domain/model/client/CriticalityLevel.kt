package net.calvuz.qreport.domain.model.client

import kotlinx.serialization.Serializable

/**
 * Livello di criticità per i check items
 */
@Serializable
enum class CriticalityLevel(
    val displayName: String,
    val priority: Int,
    val color: String,
    val icon: String
) {
    ROUTINE(
        displayName = "Routine",
        priority = 1,
        color = "#2196F3",  // Blue,
        icon = "🟢"
    ),
    IMPORTANT(
        displayName = "Importante",
        priority = 2,
        color = "#FF9800",  // Orange
        icon = "🟡"
    ),
    CRITICAL(
        displayName = "Critico",
        priority = 3,
        color = "#F44336",  // Red
        icon = "🔴"
    ),
    NA(
        displayName = "N/A",
        priority = 0,
        color = "#9E9E9E",  // Grey
        icon = "➖"
    )
}