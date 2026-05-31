package net.calvuz.qreport.client.facility.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qreport.R

/**
 * Type of facility (production site).
 *
 * Display names and descriptions are NOT stored here as raw strings —
 * they are resolved at runtime via stringResource() using [labelResId]
 * and [descriptionResId], following the same pattern as [ClientDetailTab].
 */
@Serializable
enum class FacilityType(val labelResId: Int, val descriptionResId: Int) {
    PRODUCTION(R.string.facility_type_production, R.string.facility_type_production_desc),
    WAREHOUSE(R.string.facility_type_warehouse, R.string.facility_type_warehouse_desc),
    ASSEMBLY(R.string.facility_type_assembly, R.string.facility_type_assembly_desc),
    TESTING(R.string.facility_type_testing, R.string.facility_type_testing_desc),
    LOGISTICS(R.string.facility_type_logistics, R.string.facility_type_logistics_desc),
    OFFICE(R.string.facility_type_office, R.string.facility_type_office_desc),
    MAINTENANCE(R.string.facility_type_maintenance, R.string.facility_type_maintenance_desc),
    R_AND_D(R.string.facility_type_r_and_d, R.string.facility_type_r_and_d_desc),
    OTHER(R.string.facility_type_other, R.string.facility_type_other_desc)
}