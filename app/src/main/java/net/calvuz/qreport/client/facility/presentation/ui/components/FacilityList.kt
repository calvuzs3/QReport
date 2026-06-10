package net.calvuz.qreport.client.facility.presentation.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.app.app.util.MapUtils
import net.calvuz.qreport.client.facility.presentation.ui.FacilityWithStats
import net.calvuz.qreport.settings.domain.model.ListViewMode

/** Facility List */
@Suppress("ParamsComparedByRef")
@Composable
fun FacilityListContent(
    variant: ListViewMode,
    facilities: List<FacilityWithStats>,
    onFacilityClick: (String) -> Unit,
    onFacilityEdit: (String) -> Unit,
    onFacilityDelete: ((String) -> Unit)? = null,
    onFacilityRestore: (String) -> Unit
) {
    val context: Context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = facilities,
            key = { it.facility.id }
        ) { facilityWithStats ->
            FacilityCard(
                facility = facilityWithStats.facility,
                stats = facilityWithStats.stats,
                onClick = { onFacilityClick(facilityWithStats.facility.id) },
                onEdit = { onFacilityEdit(facilityWithStats.facility.id) },
                onDelete = if (onFacilityDelete != null) {
                    { onFacilityDelete(facilityWithStats.facility.id) }
                } else null,
                onRestore = { onFacilityRestore(facilityWithStats.facility.id) },
                onOpenMaps = if (facilityWithStats.facility.address?.isComplete() ?: false) {
                    { MapUtils.openMapsWithAddress(context, facilityWithStats.facility.address) }
                } else {
                    null
                },
                variant = variant
            )
        }
    }
}