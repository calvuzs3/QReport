package net.calvuz.qreport.client.facility.presentation.model

enum class FacilitySortOrder {
    NAME, CREATED_RECENT, CREATED_OLDEST, ISLANDS_COUNT, TYPE
}

// Extension
fun FacilitySortOrder.getDisplayName(): String {
    return when (this) {
        FacilitySortOrder.NAME -> "Nome"
        FacilitySortOrder.CREATED_RECENT -> "Più Recenti"
        FacilitySortOrder.CREATED_OLDEST -> "Meno Recenti"
        FacilitySortOrder.ISLANDS_COUNT -> "Numero Isole"
        FacilitySortOrder.TYPE -> "Tipo"
    }
}