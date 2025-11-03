package net.calvuz.qreport.domain.model.checkup

import kotlinx.serialization.Serializable

/**
 * Stato di controllo di un singolo item
 */
@Serializable
enum class CheckItemStatus(
    val displayName: String,
    val color: String,
    val icon: String
) {
    PENDING("In Attesa", "#FFA726", "⏳"), // Orange
    OK("OK", "#66BB6A", "✓"),             // Green
    NOK("NOK", "#EF5350", "✗"),           // Red
    NA("N/A", "#78909C", "➖")             // Blue Grey
}