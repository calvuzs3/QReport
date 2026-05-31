package net.calvuz.qreport.client.island.domain.model

fun maintenanceIntervalFor(type: IslandType): Int = when (type) {
    IslandType.POLY_MOVE -> 90
    IslandType.POLY_CAST -> 120
    IslandType.POLY_EBT -> 60
    IslandType.POLY_TAG_BLE -> 180
    IslandType.POLY_TAG_FC -> 180
    IslandType.POLY_TAG_V -> 150
    IslandType.POLY_SAMPLE -> 30
}