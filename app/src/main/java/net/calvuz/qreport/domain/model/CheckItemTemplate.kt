package net.calvuz.qreport.domain.model

/**
 * Template per i check items basati sul tipo di isola
 */
data class CheckItemTemplate(
    val id: String,
    val moduleType: String, // Rimane String per flessibilità nei template
    val category: String,
    val description: String,
    val criticality: CriticalityLevel, // CAMBIATO: ora usa CriticalityLevel
    val orderIndex: Int,
    val islandTypes: List<IslandType>
)