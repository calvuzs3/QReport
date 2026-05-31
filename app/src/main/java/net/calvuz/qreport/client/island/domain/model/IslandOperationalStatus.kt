package net.calvuz.qreport.client.island.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qreport.R

/**
 * Operational status of a robotic island.
 *
 * [labelResId] is resolved via stringResource() in the UI.
 * Color is NOT stored here — the presentation layer maps each status
 * to a MaterialTheme color token.
 */
@Serializable
enum class IslandOperationalStatus(val labelResId: Int) {
    OPERATIONAL(R.string.island_status_operational),
    MAINTENANCE_DUE(R.string.island_status_maintenance_due),
    INACTIVE(R.string.island_status_inactive)
}