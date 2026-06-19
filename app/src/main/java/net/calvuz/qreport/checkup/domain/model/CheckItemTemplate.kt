package net.calvuz.qreport.checkup.domain.model

import net.calvuz.qreport.app.app.domain.model.CriticalityLevel

/**
 * Template per i check items basati sul tipo di isola.
 * [islandTypes] sono le label (es. "POLY Move") dei tipi isola a cui si applica il template.
 */
data class CheckItemTemplate(
    val id: String,
    val moduleType: String, // Rimane String per flessibilità nei template
    val category: String,
    val description: String,
    val criticality: CriticalityLevel, // CAMBIATO: ora usa CriticalityLevel
    val orderIndex: Int,
    val islandTypes: List<String>
)