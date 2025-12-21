package net.calvuz.qreport.domain.model.backup

/**
 * BackupValidationResult - Risultato validazione backup
 */
data class BackupValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        /**
         * Crea result per backup non valido
         */
        fun invalid(errors: List<String>) = BackupValidationResult(
            isValid = false,
            errors = errors,
            warnings = emptyList()
        )

        /**
         * Crea result per backup valido
         */
        fun valid(warnings: List<String> = emptyList()) = BackupValidationResult(
            isValid = true,
            errors = emptyList(),
            warnings = warnings
        )
    }

    /**
     * Ha errori critici che impediscono restore
     */
    fun hasCriticalErrors(): Boolean = errors.isNotEmpty()

    /**
     * Ha warning che potrebbero indicare problemi
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    /**
     * Summary per UI
     */
    fun getSummary(): String {
        return when {
            !isValid -> "❌ Backup non valido (${errors.size} errori)"
            hasWarnings() -> "⚠️ Backup valido con warning (${warnings.size} avvisi)"
            else -> "✅ Backup valido"
        }
    }
}