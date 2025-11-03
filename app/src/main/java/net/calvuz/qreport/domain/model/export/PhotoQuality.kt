package net.calvuz.qreport.domain.model.export

/**
 * Qualità foto per export
 */
enum class PhotoQuality {
    /**
     * Foto originali senza modifiche
     * - Massima qualità
     * - Dimensioni file maggiori
     * - Tempo export più lungo
     */
    ORIGINAL,

    /**
     * Ottimizzate per dimensioni ragionevoli
     * - Buona qualità
     * - Dimensioni equilibrate
     * - Velocità export bilanciata
     */
    OPTIMIZED,

    /**
     * Compresse per minimizzare spazio
     * - Qualità ridotta
     * - Dimensioni file minime
     * - Export più veloce
     */
    COMPRESSED
}