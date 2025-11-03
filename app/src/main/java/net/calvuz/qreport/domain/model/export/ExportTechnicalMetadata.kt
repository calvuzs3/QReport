package net.calvuz.qreport.domain.model.export

import kotlinx.datetime.Instant

/**
 * Metadati TECNICI per l'export (non business domain)
 * Contiene solo informazioni tecniche per la generazione del report
 */
data class ExportTechnicalMetadata(
    /**
     * Timestamp di generazione del report
     */
    val generatedAt: Instant,

    /**
     * Versione del template/generatore utilizzato
     */
    val templateVersion: String,

    /**
     * Formato di output richiesto
     */
    val exportFormat: ExportFormat,

    /**
     * Configurazioni tecniche per l'export
     */
    val exportOptions: ExportOptions
)