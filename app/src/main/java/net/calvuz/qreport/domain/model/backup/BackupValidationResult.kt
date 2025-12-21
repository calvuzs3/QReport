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
        fun valid() = BackupValidationResult(true)
        fun invalid(errors: List<String>) = BackupValidationResult(false, errors)
    }
}