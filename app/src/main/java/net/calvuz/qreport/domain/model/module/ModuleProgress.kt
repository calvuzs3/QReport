package net.calvuz.qreport.domain.model.module

/**
 * Progresso di un modulo
 */
data class ModuleProgress(
    val totalItems: Int,
    val completedItems: Int,
    val criticalIssues: Int,
    val progressPercentage: Float
)