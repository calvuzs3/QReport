package net.calvuz.qreport.app.result.data

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