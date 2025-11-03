package net.calvuz.qreport.domain.model.export

/**
 * Strategie di naming per le foto esportate
 */
enum class PhotoNamingStrategy {
    /**
     * Naming strutturato: 01_Sezione_Check001_descrizione.jpg
     * - Migliore organizzazione
     * - Facile identificazione
     * - Ordinamento logico
     */
    STRUCTURED,

    /**
     * Naming sequenziale: foto_001.jpg, foto_002.jpg
     * - Semplice e lineare
     * - Compatibilità universale
     * - Meno informativo
     */
    SEQUENTIAL,

    /**
     * Naming timestamp: 20251022_143052_001.jpg
     * - Ordinamento cronologico
     * - Evita conflitti di nome
     * - Meno intuitivo per utente
     */
    TIMESTAMP
}