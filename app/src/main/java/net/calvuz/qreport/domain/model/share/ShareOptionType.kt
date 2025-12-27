package net.calvuz.qreport.domain.model.share

/**
 * Tipi di opzioni di condivisione
 */
enum class ShareOptionType {
    FILE_OPTION,    // Condivisione generica con chooser
    APP_SPECIFIC,   // Condivisione con app specifica
    APP_GENERIC,    // Condivisione backup completo
    QUICK_ACTION    // Condivisione compressa
}