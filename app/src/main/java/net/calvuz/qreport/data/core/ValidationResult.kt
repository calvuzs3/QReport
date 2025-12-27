package net.calvuz.qreport.data.core

/**
 * Risultato validazione
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
)