package net.calvuz.qreport.client.client.presentation.model

import kotlinx.datetime.Instant

/**
 * Statistics for a single client — used in list cards and detail views.
 *
 * Note: [statusDescription] and [summaryText] have been removed.
 * Localised status labels belong in the UI layer (Composable or ViewModel
 * via UiText), not in a presentation model.
 */
data class ClientStatistics(
    val facilitiesCount: Int,
    val islandsCount: Int,
    val contactsCount: Int,
    val contractsCount: Int,
    val totalCheckUps: Int,
    val completedCheckUps: Int,
    val lastCheckUpDate: Instant?
) {
    /** Percentage of completed check-ups out of total. */
    val completionRate: Int
        get() = if (totalCheckUps > 0) {
            (completedCheckUps.toDouble() / totalCheckUps * 100).toInt()
        } else 0

    /** True when the client has at least one facility and one contact. */
    val isComplete: Boolean
        get() = facilitiesCount > 0 && contactsCount > 0

    /**
     * Composite health score (0–100).
     * Weights: facilities 20, contacts 10, contracts 10, islands 20, check-ups 40.
     */
    val healthScore: Int
        get() {
            var score = 0
            if (facilitiesCount > 0) score += 20
            if (contactsCount > 0) score += 10
            if (contractsCount > 0) score += 10
            if (islandsCount > 0) score += 20
            if (totalCheckUps > 0) score += 40
            return score
        }
}