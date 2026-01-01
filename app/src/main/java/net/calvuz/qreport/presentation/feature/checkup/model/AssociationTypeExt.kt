package net.calvuz.qreport.presentation.feature.checkup.model

import android.content.Context
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.model.checkup.AssociationType

object AssociationTypeExt {

    // DisplayName
    fun AssociationType.getDisplayName(context: Context): String {
        return when (this) {
            AssociationType.STANDARD -> context.getString(R.string.enum_association_type_standard)
            AssociationType.MULTI_ISLAND -> context.getString(R.string.enum_association_type_multi_island)
            AssociationType.COMPARISON -> context.getString(R.string.enum_association_type_comparison)
            AssociationType.MAINTENANCE -> context.getString(R.string.enum_association_type_maintenance)
            AssociationType.EMERGENCY -> context.getString(R.string.enum_association_type_emergency)
        }
    }

    // Description
    fun AssociationType.getDescription(context: Context): String {
        return when (this) {
            AssociationType.STANDARD -> context.getString(R.string.enum_association_type_standard_desc)
            AssociationType.MULTI_ISLAND -> context.getString( R.string.enum_association_type_multi_island_desc)
            AssociationType.COMPARISON -> context.getString( R.string.enum_association_type_comparison_desc)
            AssociationType.MAINTENANCE -> context.getString( R.string.enum_association_type_maintenance_desc)
            AssociationType.EMERGENCY -> context.getString( R.string.enum_association_type_emergency_desc)
        }
    }
}