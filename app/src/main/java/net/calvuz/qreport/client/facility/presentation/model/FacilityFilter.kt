package net.calvuz.qreport.client.facility.presentation.model

enum class FacilityFilter {
    ALL, ACTIVE, INACTIVE, PRIMARY_ONLY, WITH_ISLANDS, BY_TYPE
}

// Extension
fun FacilityFilter.getDisplayName(): String {
    return when (this) {
        FacilityFilter.ALL -> "Tutti"
        FacilityFilter.ACTIVE -> "Attivi"
        FacilityFilter.INACTIVE -> "Inattivi"
        FacilityFilter.PRIMARY_ONLY -> "Solo Primari"
        FacilityFilter.WITH_ISLANDS -> "Con Isole"
        FacilityFilter.BY_TYPE -> "Per Tipo"
    }
}