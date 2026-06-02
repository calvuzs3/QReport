package net.calvuz.qreport.client.facility.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import net.calvuz.qreport.client.facility.domain.model.FacilityType

/**
 * Maps [FacilityType] to a Material icon for use in the presentation layer.
 *
 * Kept in the presentation layer so the domain model stays free
 * of Compose dependencies — same pattern as [IslandType.icon()].
 */
fun FacilityType.icon(): ImageVector = when (this) {
    FacilityType.PRODUCTION    -> Icons.Default.Factory
    FacilityType.WAREHOUSE     -> Icons.Default.Warehouse
    FacilityType.ASSEMBLY      -> Icons.Default.Build
    FacilityType.TESTING       -> Icons.Default.Science
    FacilityType.LOGISTICS     -> Icons.Default.LocalShipping
    FacilityType.OFFICE        -> Icons.Default.Business
    FacilityType.MAINTENANCE   -> Icons.Default.BuildCircle
    FacilityType.R_AND_D       -> Icons.Default.Biotech
    FacilityType.OTHER         -> Icons.Default.Domain
}