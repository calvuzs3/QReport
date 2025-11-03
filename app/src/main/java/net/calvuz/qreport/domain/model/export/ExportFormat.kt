package net.calvuz.qreport.domain.model.export

/**
 * Formati di export supportati dal sistema QReport
 */
enum class ExportFormat {
    /**
     * Documento Word (.docx) con foto integrate
     * - Report professionale per cliente
     * - Foto embedded nel documento
     * - Formattazione corporate
     */
    WORD,

    /**
     * Report testuale ASCII (.txt)
     * - Formato universalmente leggibile
     * - Riepilogo completo del checkup
     * - Riferimenti alle foto esterne
     */
    TEXT,

    /**
     * Cartella con foto originali
     * - Foto in qualità originale
     * - Naming strutturato per sezione/item
     * - Separato dai documenti per gestione flessibile
     */
    PHOTO_FOLDER,

//    PDF,

    COMBINED_PACKAGE // Tutti i formati insieme
}

