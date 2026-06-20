package net.calvuz.qreport.checkup.checkup.domain.model

import net.calvuz.qreport.checkup.modules.domain.model.ModuleProgress

/**
 * Progresso di compilazione - VERSIONE CON DEFAULT VALUES
 */
data class CheckUpProgress(
    val checkUpId: String = "",
    val moduleProgress: Map<String, ModuleProgress> = emptyMap(),
    val overallProgress: Float = 0f,
    val estimatedTimeRemaining: Int? = null // in minuti
)