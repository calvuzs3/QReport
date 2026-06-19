package net.calvuz.qreport.client.island.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster

/** Resolved label/icon for an island type, ready to render. */
data class IslandTypeDisplay(val code: String, val label: String, val icon: ImageVector)

/**
 * Resolves the display info for an island type, preferring the live master record
 * (by FK, then by frozen legacy label — covers legacy data without [islandTypeId]
 * populated yet) over the frozen label itself. Falls back to that frozen label only
 * if the master list can't resolve anything (e.g. master table not yet seeded/synced).
 */
fun resolveIslandTypeDisplay(
    islandTypeId: String?,
    legacyType: String,
    masters: List<IslandTypeMaster>
): IslandTypeDisplay {
    val master = masters.find { it.id == islandTypeId } ?: masters.find { it.label == legacyType }
    val code = master?.code ?: legacyType
    val label = master?.label ?: legacyType
    return IslandTypeDisplay(
        code = code,
        label = label,
        icon = IslandTypeIconRegistry.iconFor(label)
    )
}
