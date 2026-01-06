package net.calvuz.qreport.photo.domain.model

import java.io.File

/**
 * Interface per processamento immagini durante l'export
 *
 * Gestisce:
 * - Ottimizzazione qualità e dimensioni foto
 * - Aggiunta watermark
 * - Conversioni formato se necessarie
 */
interface ImageProcessor {

    /**
     * Ottimizza una foto per l'export
     *
     * @param sourceFile File sorgente
     * @param targetFile File di destinazione
     * @param quality Qualità JPEG (0-100)
     * @param maxWidth Larghezza massima in pixel (opzionale)
     * @param maxHeight Altezza massima in pixel (opzionale)
     * @return Dimensione file risultante in bytes
     */
    suspend fun optimizePhoto(
        sourceFile: File,
        targetFile: File,
        quality: Int,
        maxWidth: Int? = null,
        maxHeight: Int? = null
    ): Long

    /**
     * Aggiunge watermark a una foto
     *
     * @param sourceFile File sorgente
     * @param targetFile File di destinazione
     * @param watermarkText Testo del watermark
     * @return Dimensione file risultante in bytes
     */
    suspend fun addWatermark(
        sourceFile: File,
        targetFile: File,
        watermarkText: String
    ): Long
}