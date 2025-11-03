package net.calvuz.qreport.domain.model.export

/**
 * Livelli di compressione (esistente, per compatibilità)
 */
enum class CompressionLevel {
    LOW,    // 95% qualità
    MEDIUM, // 85% qualità
    HIGH    // 70% qualità
}