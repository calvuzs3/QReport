package net.calvuz.qreport.backup.domain.model

/**
 * BackupValidationResult - Risultato validazione backup
 */
data class BackupValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val issues: List<ValidationIssue> = emptyList(),
    val version: String? = null,
    val recordCount: Int = 0,
    val photoCount: Int = 0
) {
    companion object {
        /**
         * Crea result per backup non valido
         */
        fun invalid(errors: List<String>) = BackupValidationResult(
            isValid = false,
            errors = errors,
            issues= emptyList(),
            warnings = emptyList()
        )

        /**
         * Crea result per backup valido
         */
        fun valid(warnings: List<String> = emptyList()) = BackupValidationResult(
            isValid = true,
            issues = emptyList(),
            errors = emptyList(),
            warnings = warnings
        )
    }

    /**
     * Ha errori critici che impediscono restore
     */
    fun hasCriticalErrors(): Boolean = issues.isNotEmpty()

    /**
     * Ha warning che potrebbero indicare problemi
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    /**
     * Summary per UI
     */
    fun getSummary(): String {
        return when {
            !isValid -> "❌ Backup non valido (${issues.size} errori)"
            hasWarnings() -> "⚠️ Backup valido con warning (${warnings.size} avvisi)"
            else -> "✅ Backup valido"
        }
    }
}

/**
 * Validation issue details
 */
data class ValidationIssue(
    val type: ValidationIssueType,
    val message: String,
    val severity: ValidationSeverity
)

enum class ValidationIssueType {
    MISSING_FILE,
    CORRUPTED_JSON,
    INVALID_STRUCTURE,
    MISSING_PHOTOS,
    CHECKSUM_MISMATCH,
    VERSION_MISMATCH
}

enum class ValidationSeverity {
    WARNING,    // Backup can be used with issues
    ERROR       // Backup cannot be used
}