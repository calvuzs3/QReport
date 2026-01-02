package net.calvuz.qreport.presentation.feature.checkup.model

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.model.checkup.AssociationType

object AssociationTypeExt {

    // DisplayName
    @Composable
    fun AssociationType.asString(): String {
        return when (this) {
            AssociationType.STANDARD -> stringResource(R.string.enum_association_type_standard)
            AssociationType.MULTI_ISLAND -> stringResource(R.string.enum_association_type_multi_island)
            AssociationType.COMPARISON -> stringResource(R.string.enum_association_type_comparison)
            AssociationType.MAINTENANCE -> stringResource(R.string.enum_association_type_maintenance)
            AssociationType.EMERGENCY -> stringResource(R.string.enum_association_type_emergency)
        }
    }

    // Description
    @Composable
    fun AssociationType.getDescription(): String {
        return when (this) {
            AssociationType.STANDARD -> stringResource(R.string.enum_association_type_standard_desc)
            AssociationType.MULTI_ISLAND -> stringResource( R.string.enum_association_type_multi_island_desc)
            AssociationType.COMPARISON -> stringResource( R.string.enum_association_type_comparison_desc)
            AssociationType.MAINTENANCE -> stringResource( R.string.enum_association_type_maintenance_desc)
            AssociationType.EMERGENCY -> stringResource( R.string.enum_association_type_emergency_desc)
        }
    }
}