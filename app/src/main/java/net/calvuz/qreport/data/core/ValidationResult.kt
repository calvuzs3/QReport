package net.calvuz.qreport.data.core

/**
 * Risultato validazione
 *
 * `ValidationResult.Valid`
 */
sealed class ValidationResult (){
    data class Valid(
        val isValid: Boolean = true,
        val message: String = ""
    ): ValidationResult()

    data class NotValid(
        val isValid: Boolean =false,
        val message: String = "",
        val issues: List<String>
    ): ValidationResult()

    object Empty: ValidationResult()
}