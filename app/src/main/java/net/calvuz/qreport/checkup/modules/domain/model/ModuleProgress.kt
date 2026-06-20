package net.calvuz.qreport.checkup.modules.domain.model

/**
 * Progresso di un modulo
 */
data class ModuleProgress(
    val totalItems: Int,
    val completedItems: Int,
    val criticalIssues: Int,
    val progressPercentage: Float
)