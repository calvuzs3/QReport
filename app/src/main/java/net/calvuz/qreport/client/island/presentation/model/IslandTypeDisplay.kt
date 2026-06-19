package net.calvuz.qreport.client.island.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster

/** Resolved label/icon for an island type, ready to render. */
data class IslandTypeDisplay(val code: String, val label: String, val icon: ImageVector)

/**
 * Resolves the display info for an island type, preferring the live master record
 * (by FK, then by code — covers legacy data without [islandTypeId] populated yet)
 * over the hardcoded enum. Falls back to the enum's own code/icon only if the
 * master list can't resolve anything (e.g. master table not yet seeded/synced).
 */
fun resolveIslandTypeDisplay(
    islandTypeId: String?,
    legacyType: IslandType,
    masters: List<IslandTypeMaster>
): IslandTypeDisplay {
    val master = masters.find { it.id == islandTypeId } ?: masters.find { it.code == legacyType.code }
    return if (master != null) {
        IslandTypeDisplay(
            code = master.code,
            label = master.label,
            icon = IslandTypeIconRegistry.iconFor(master.iconName ?: master.code)
        )
    } else {
        IslandTypeDisplay(
            code = legacyType.code,
            label = legacyType.code,
            icon = IslandTypeIconRegistry.iconFor(legacyType.code)
        )
    }
}
