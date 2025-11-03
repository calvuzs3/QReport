package net.calvuz.qreport.domain.model.export

/**
 * Opzioni di configurazione per l'export del checkup
 * Supporta export multi-formato e configurazioni granulari
 */
data class ExportOptions(
    // ===== FORMATI EXPORT =====

    /**
     * Set di formati da generare nell'export
     * Default: tutti i formati (Word + Text + Photo Folder)
     */
    val exportFormats: Set<ExportFormat> = setOf(
        ExportFormat.WORD,
        ExportFormat.TEXT,
        ExportFormat.PHOTO_FOLDER
    ),

    // ===== OPZIONI ESISTENTI =====

    /**
     * Include foto nei documenti Word/Text
     * - Word: foto embedded nel documento
     * - Text: riferimenti alle foto nella cartella
     */
    val includePhotos: Boolean = true,

    /**
     * Include note testuali nei report
     */
    val includeNotes: Boolean = true,

    /**
     * Livello di compressione per foto nel Word
     */
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,

    /**
     * Larghezza massima foto nel Word (pixel)
     */
    val photoMaxWidth: Int = 800,

    /**
     * Template personalizzato (opzionale)
     */
    val customTemplate: ReportTemplate? = null,

    // ===== NUOVE OPZIONI =====

    /**
     * Strategia di naming per foto nella cartella FOTO
     */
    val photoNamingStrategy: PhotoNamingStrategy = PhotoNamingStrategy.STRUCTURED,

    /**
     * Crea directory export con timestamp
     * - true: Export_Checkup_20251022_1430/
     * - false: File diretti nella directory target
     */
    val createTimestampedDirectory: Boolean = true,

    /**
     * Qualità foto per cartella FOTO
     * - ORIGINAL: Copia foto originali (massima qualità)
     * - OPTIMIZED: Ottimizza dimensioni mantenendo qualità
     * - COMPRESSED: Comprime per ridurre spazio
     */
    val photoFolderQuality: PhotoQuality = PhotoQuality.ORIGINAL,

    /**
     * Include metadati EXIF nelle foto esportate
     */
    val preserveExifData: Boolean = true,

    /**
     * Aggiungi watermark alle foto esportate
     */
    val addWatermark: Boolean = false,

    /**
     * Testo watermark (se abilitato)
     */
    val watermarkText: String = "QReport",

    /**
     * Genera file indice nella cartella FOTO
     * Contiene mapping foto -> check item
     */
    val generatePhotoIndex: Boolean = true
) {

    companion object {
        /**
         * Configurazione per solo Word (comportamento precedente)
         */
        fun wordOnly() = ExportOptions(
            exportFormats = setOf(ExportFormat.WORD),
            createTimestampedDirectory = false
        )

        /**
         * Configurazione completa (default)
         */
        fun complete() = ExportOptions()

        /**
         * Configurazione minimal (Word + Text, no foto)
         */
        fun textOnly() = ExportOptions(
            exportFormats = setOf(ExportFormat.WORD, ExportFormat.TEXT),
            includePhotos = false,
            createTimestampedDirectory = false
        )

        /**
         * Configurazione archivio (solo foto + indice)
         */
        fun photoArchive() = ExportOptions(
            exportFormats = setOf(ExportFormat.PHOTO_FOLDER),
            photoFolderQuality = PhotoQuality.ORIGINAL,
            preserveExifData = true,
            generatePhotoIndex = true
        )
    }

    // ===== VALIDATION =====

    /**
     * Valida la configurazione export
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (exportFormats.isEmpty()) {
            errors.add("Almeno un formato export deve essere specificato")
        }

        if (photoMaxWidth <= 0) {
            errors.add("photoMaxWidth deve essere > 0")
        }

        if (includePhotos && ExportFormat.PHOTO_FOLDER !in exportFormats) {
            // Warning: foto nei documenti ma non cartella separata
        }

        if (addWatermark && watermarkText.isBlank()) {
            errors.add("watermarkText richiesto se addWatermark = true")
        }

        return errors
    }

    /**
     * Verifica se il formato è abilitato
     */
    fun isFormatEnabled(format: ExportFormat): Boolean = format in exportFormats

    /**
     * Verifica se foto sono richieste in qualche formato
     */
    fun requiresPhotos(): Boolean = includePhotos || isFormatEnabled(ExportFormat.PHOTO_FOLDER)
}

