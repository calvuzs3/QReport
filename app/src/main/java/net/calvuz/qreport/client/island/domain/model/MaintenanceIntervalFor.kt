package net.calvuz.qreport.client.island.domain.model

fun maintenanceIntervalFor(type: IslandType): Int = when (type) {
    IslandType.POLY_MOVE -> 180
    IslandType.POLY_CAST -> 180
    IslandType.POLY_EBT -> 180
    IslandType.POLY_TAG_BLE -> 90
    IslandType.POLY_TAG_FC -> 180
    IslandType.POLY_TAG_V -> 180
    IslandType.POLY_SAMPLE -> 180
    IslandType.POLY_WELD -> 180
    IslandType.POLY_PAINT -> 180
    IslandType.OTHER -> 180
}