package net.calvuz.qreport.domain.model.export

import java.time.LocalDateTime

/**
 * Risultato di un'operazione di export singolo formato
 */
sealed class ExportResult {

    /**
     * Export completato con successo
     */
    data class Success(
        val filePath: String,
        val fileName: String,
        val fileSize: Long,
        val format: ExportFormat,
        val generatedAt: LocalDateTime = LocalDateTime.now()
    ) : ExportResult() {

        /**
         * Ottieni estensione file dal formato
         */
        val fileExtension: String
            get() = when (format) {
                ExportFormat.WORD -> "docx"
//                ExportFormat.PDF -> "pdf"
                ExportFormat.TEXT -> "txt"
                ExportFormat.PHOTO_FOLDER -> "" // cartella
                ExportFormat.COMBINED_PACKAGE -> "" // cartella

            }

        /**
         * Verifica se il file è una directory
         */
        val isDirectory: Boolean
            get() = format == ExportFormat.PHOTO_FOLDER || format == ExportFormat.COMBINED_PACKAGE
    }

    /**
     * Export fallito con errore
     */
    data class Error(
        val exception: Throwable,
        val errorCode: ExportErrorCode,
        val format: ExportFormat? = null,
        val partialResults: List<Success> = emptyList()
    ) : ExportResult()

    /**
     * Export in corso (per UI reactive)
     */
    data class Loading(
        val format: ExportFormat,
        val progress: Float = 0f,
        val statusMessage: String = ""
    ) : ExportResult()
}

