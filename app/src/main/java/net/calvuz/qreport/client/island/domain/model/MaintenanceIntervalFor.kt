package net.calvuz.qreport.client.island.domain.model

/**
 * Fallback maintenance interval (days) when [IslandTypeMaster.maintenanceIntervalDays]
 * can't be resolved for an island's type — only "POLY Tag BLE" historically used a
 * shorter interval, every other POLY type defaults to 180 days.
 */
fun maintenanceIntervalFor(label: String): Int = if (label == "POLY Tag BLE") 90 else 180
