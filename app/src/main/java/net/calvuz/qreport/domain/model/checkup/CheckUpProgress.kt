package net.calvuz.qreport.domain.model.checkup

import net.calvuz.qreport.domain.model.module.ModuleProgress

/**
 * Progresso di compilazione - VERSIONE CON DEFAULT VALUES
 */
data class CheckUpProgress(
    val checkUpId: String = "",
    val moduleProgress: Map<String, ModuleProgress> = emptyMap(),
    val overallProgress: Float = 0f,
    val estimatedTimeRemaining: Int? = null // in minuti
)