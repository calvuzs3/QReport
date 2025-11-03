package net.calvuz.qreport.data.export.word

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.domain.model.export.*
import org.apache.poi.xwpf.usermodel.XWPFDocument
import timber.log.Timber
import java.io.File
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generatore di report Word usando Apache POI
 * Coordina template engine, styling e gestione foto per documenti professionali
 */
@Singleton
class WordReportGenerator @Inject constructor(
    private val templateEngine: WordTemplateEngine,
    private val styleEngine: WordStyleEngine,
    private val dataMapper: CheckupDataMapper,
    private val photoProcessor: PhotoProcessor,
    private val fileManager: ExportFileManager
) {

    /**
     * Genera report Word completo
     *
     * @param exportData Dati del checkup da esportare
     * @param options Opzioni di configurazione export
     * @return Risultato dell'export con path del file generato
     */
    suspend fun generateReport(
        exportData: ExportData,
        options: ExportOptions
    ): ExportResult = withContext(Dispatchers.IO) {

        try {
            Timber.i("Avvio generazione Word report per checkup ${exportData.checkup.id}")

            // 1. Validazione dati di input
            validateExportData(exportData)

            // 2. Mappa dati per export con formattazione
            val mappedData = dataMapper.mapToExportData(exportData, options)

            // 3. Crea documento base con template
            val document = templateEngine.createDocument(mappedData)

            // 4. Applica stili e formattazione
            styleEngine.configureDocumentStyles(document, mappedData.metadata.reportTemplate)

            // 5. Aggiungi contenuto dettagliato
            addDetailContent(document, mappedData, options)

            // 6. Processa e inserisci foto se richieste
            if (options.includePhotos) {
                addPhotosToDocument(document, mappedData, options)
            }

            // 7. Finalizza documento
            finalizeDocument(document, mappedData)

            // 8. Salva file
            val fileName = generateFileName(mappedData)
            val filePath = fileManager.saveDocument(document, fileName)

            // 9. Chiudi documento per liberare memoria
            document.close()

            ExportResult.Success(
                filePath = filePath,
                fileName = fileName,
                fileSize = File(filePath).length(),
                format = ExportFormat.WORD
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore generazione Word report")
            ExportResult.Error(
                exception = e,
                errorCode = mapErrorCode(e),
                format = ExportFormat.WORD
            )
        }
    }

    /**
     * Aggiunge contenuto dettagliato al documento
     */
    private suspend fun addDetailContent(
        document: XWPFDocument,
        exportData: MappedExportData,
        options: ExportOptions
    ) {
        Timber.d("Aggiunta contenuto dettagliato: ${exportData.sections.size} sezioni")

        // Sezioni del checkup
        exportData.sections.forEach { section ->
            addSectionToDocument(document, section, options)
        }

        // Parti di ricambio se presenti
        if (exportData.spareParts.isNotEmpty()) {
            addSparePartsSection(document, exportData.spareParts)
        }

        // Sezione firma
        addSignatureSection(document, exportData.metadata)
    }

    /**
     * Aggiunge una sezione completa al documento
     */
    private fun addSectionToDocument(
        document: XWPFDocument,
        section: MappedCheckupSection,
        options: ExportOptions
    ) {
        // Header sezione con styling
        val headerParagraph = document.createParagraph()
        styleEngine.applySectionHeaderStyle(headerParagraph, section.title)

        // Descrizione sezione se presente
        if (section.description.isNotBlank()) {
            val descParagraph = document.createParagraph()
            styleEngine.applyBodyStyle(descParagraph, section.description)
        }

        // Tabella check items
        if (section.items.isNotEmpty()) {
            val itemsTable = document.createTable()
            populateCheckItemsTable(itemsTable, section.items, options)
            styleEngine.styleTable(itemsTable, TableStyleConfig(hasHeader = true))
        }

        // Note sezione se presenti e abilitate
        if (options.includeNotes && section.notes.isNotBlank()) {
            addSectionNotes(document, section.notes)
        }

        // Spazio tra sezioni
        document.createParagraph() // Paragrafo vuoto per spacing
    }

    /**
     * Popola tabella con check items
     */
    private fun populateCheckItemsTable(
        table: XWPFTable,
        items: List<MappedCheckItem>,
        options: ExportOptions
    ) {
        // Header tabella
        val headers = listOf("Controllo", "Stato", "Criticità", "Note")
        val headerRow = table.getRow(0) ?: table.createRow()

        headers.forEachIndexed { index, header ->
            val cell = if (index < headerRow.tableCells.size) {
                headerRow.getCell(index)
            } else {
                headerRow.createCell()
            }
            styleEngine.applyCellHeaderStyle(cell, header)
        }

        // Righe dati
        items.forEach { item ->
            val row = table.createRow()

            // Titolo controllo
            styleEngine.applyCellBodyStyle(row.getCell(0), item.title)

            // Stato formattato
            styleEngine.applyCellBodyStyle(row.getCell(1), item.statusDisplay)

            // Criticità formattata
            styleEngine.applyCellBodyStyle(row.getCell(2), item.criticalityDisplay)

            // Note (troncate se troppo lunghe)
            val noteText = if (options.includeNotes && item.note.isNotBlank()) {
                item.note.take(150) + if (item.note.length > 150) "..." else ""
            } else {
                "-"
            }
            styleEngine.applyCellBodyStyle(row.getCell(3), noteText)
        }
    }

    /**
     * Aggiunge foto al documento
     */
    private suspend fun addPhotosToDocument(
        document: XWPFDocument,
        exportData: MappedExportData,
        options: ExportOptions
    ) {
        Timber.d("Aggiunta foto al documento")

        val allPhotos = exportData.sections.flatMap { section ->
            section.items.flatMap { item -> item.photos }
        }

        if (allPhotos.isEmpty()) {
            Timber.d("Nessuna foto da aggiungere")
            return
        }

        // Header sezione foto
        val photosHeader = document.createParagraph()
        styleEngine.applySectionHeaderStyle(photosHeader, "FOTO EVIDENZE")

        // Processa foto in batch per evitare overload memoria
        val photosBatches = allPhotos.chunked(10) // 10 foto per batch

        photosBatches.forEach { batch ->
            processBatchPhotos(document, batch, options)
        }
    }

    /**
     * Processa un batch di foto
     */
    private suspend fun processBatchPhotos(
        document: XWPFDocument,
        photos: List<MappedPhoto>,
        options: ExportOptions
    ) {
        photos.forEach { photo ->
            try {
                val processedPhoto = photoProcessor.processPhotoForExport(
                    photo = photo,
                    options = PhotoExportOptions(
                        maxWidth = options.photoMaxWidth,
                        quality = when (options.compressionLevel) {
                            CompressionLevel.LOW -> 95
                            CompressionLevel.MEDIUM -> 85
                            CompressionLevel.HIGH -> 70
                        }
                    )
                )

                addPhotoToDocument(document, processedPhoto, photo.caption)

            } catch (e: Exception) {
                Timber.w(e, "Errore processamento foto: ${photo.fileName}")
                // Continua con le altre foto invece di fallire tutto
            }
        }
    }

    /**
     * Aggiunge singola foto al documento
     */
    private fun addPhotoToDocument(
        document: XWPFDocument,
        processedPhoto: ProcessedPhoto,
        caption: String
    ) {
        val photoParagraph = document.createParagraph()
        photoParagraph.alignment = ParagraphAlignment.CENTER

        val run = photoParagraph.createRun()

        try {
            // Aggiungi immagine
            val inputStream = processedPhoto.processedData.inputStream()
            run.addPicture(
                inputStream,
                XWPFDocument.PICTURE_TYPE_JPEG,
                processedPhoto.fileName,
                processedPhoto.width,
                processedPhoto.height
            )
            inputStream.close()

            // Aggiungi caption se presente
            if (caption.isNotBlank()) {
                val captionParagraph = document.createParagraph()
                captionParagraph.alignment = ParagraphAlignment.CENTER
                val captionRun = captionParagraph.createRun()
                captionRun.setText(caption)
                styleEngine.applyCaptionStyle(captionRun)
            }

        } catch (e: Exception) {
            Timber.e(e, "Errore inserimento foto nel documento")
            // Aggiungi placeholder testuale
            run.setText("[Foto non disponibile: $caption]")
            styleEngine.applyItalicStyle(run)
        }
    }

    /**
     * Aggiunge sezione parti di ricambio
     */
    private fun addSparePartsSection(
        document: XWPFDocument,
        spareParts: List<MappedSparePart>
    ) {
        // Header sezione
        val sparePartsHeader = document.createParagraph()
        styleEngine.applySectionHeaderStyle(sparePartsHeader, "PARTI DI RICAMBIO")

        // Tabella parti di ricambio
        val sparePartsTable = document.createTable()
        populateSparePartsTable(sparePartsTable, spareParts)
        styleEngine.styleTable(sparePartsTable, TableStyleConfig(hasHeader = true))
    }

    /**
     * Popola tabella parti di ricambio
     */
    private fun populateSparePartsTable(
        table: XWPFTable,
        spareParts: List<MappedSparePart>
    ) {
        // Header
        val headers = listOf("Codice", "Descrizione", "Quantità", "Urgenza", "Note")
        val headerRow = table.getRow(0) ?: table.createRow()

        headers.forEachIndexed { index, header ->
            val cell = if (index < headerRow.tableCells.size) {
                headerRow.getCell(index)
            } else {
                headerRow.createCell()
            }
            styleEngine.applyCellHeaderStyle(cell, header)
        }

        // Righe dati
        spareParts.forEach { part ->
            val row = table.createRow()
            styleEngine.applyCellBodyStyle(row.getCell(0), part.partNumber)
            styleEngine.applyCellBodyStyle(row.getCell(1), part.description)
            styleEngine.applyCellBodyStyle(row.getCell(2), part.quantity.toString())
            styleEngine.applyCellBodyStyle(row.getCell(3), part.urgencyDisplay)
            styleEngine.applyCellBodyStyle(row.getCell(4), part.notes)
        }
    }

    /**
     * Aggiunge sezione firma
     */
    private fun addSignatureSection(
        document: XWPFDocument,
        metadata: ExportMetadata
    ) {
        // Spazio prima della firma
        document.createParagraph()
        document.createParagraph()

        // Header firma
        val signatureHeader = document.createParagraph()
        styleEngine.applySectionHeaderStyle(signatureHeader, "VALIDAZIONE TECNICA")

        // Info tecnico
        val technicianParagraph = document.createParagraph()
        val technicianRun = technicianParagraph.createRun()
        technicianRun.setText("Tecnico responsabile: ${metadata.technicianName}")
        styleEngine.applyBodyStyle(technicianRun)

        // Data generazione
        val dateParagraph = document.createParagraph()
        val dateRun = dateParagraph.createRun()
        dateRun.setText("Data: ${metadata.generatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")
        styleEngine.applyBodyStyle(dateRun)

        // Spazio per firma
        val signatureParagraph = document.createParagraph()
        val signatureRun = signatureParagraph.createRun()
        signatureRun.setText("Firma: ____________________")
        styleEngine.applyBodyStyle(signatureRun)
    }

    /**
     * Finalizza documento con header/footer
     */
    private fun finalizeDocument(
        document: XWPFDocument,
        exportData: MappedExportData
    ) {
        // Aggiungi header persistente
        templateEngine.addDocumentHeader(document, exportData)

        // Aggiungi footer con info generazione
        templateEngine.addDocumentFooter(document, exportData.metadata.reportTemplate)

        // Numerazione pagine se abilitata
        if (exportData.metadata.reportTemplate.config.pageSetup.showPageNumbers) {
            templateEngine.addPageNumbers(document)
        }
    }

    /**
     * Genera nome file per il documento
     */
    private fun generateFileName(exportData: MappedExportData): String {
        val timestamp = exportData.metadata.generatedAt.format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
        )

        val clientName = exportData.metadata.clientInfo.companyName
            .replace(" ", "")
            .replace("[^a-zA-Z0-9]".toRegex(), "")
            .take(20)

        val islandType = exportData.checkup.islandType.name
            .replace(" ", "")
            .take(15)

        return "Checkup_${islandType}_${clientName}_$timestamp.docx"
    }

    /**
     * Valida dati di input prima dell'export
     */
    private fun validateExportData(exportData: ExportData) {
        require(exportData.checkup.id.isNotBlank()) { "Checkup ID non può essere vuoto" }
        require(exportData.metadata.technicianName.isNotBlank()) { "Nome tecnico richiesto" }
        require(exportData.metadata.clientInfo.companyName.isNotBlank()) { "Nome cliente richiesto" }
        require(exportData.metadata.reportTemplate.isValid()) { "Template non valido" }
    }

    /**
     * Mappa eccezioni a codici errore specifici
     */
    private fun mapErrorCode(exception: Throwable): ExportErrorCode {
        return when (exception) {
            is SecurityException -> ExportErrorCode.PERMISSION_DENIED
            is OutOfMemoryError -> ExportErrorCode.INSUFFICIENT_STORAGE
            is IllegalArgumentException -> ExportErrorCode.INVALID_DATA
            else -> ExportErrorCode.DOCUMENT_GENERATION_ERROR
        }
    }

    /**
     * Aggiunge note di sezione
     */
    private fun addSectionNotes(document: XWPFDocument, notes: String) {
        val notesParagraph = document.createParagraph()
        val notesRun = notesParagraph.createRun()
        notesRun.setText("Note: $notes")
        styleEngine.applyNotesStyle(notesRun)
    }
}

// === CLASSI DI SUPPORTO ===

/**
 * Configurazione stile tabella
 */
data class TableStyleConfig(
    val hasHeader: Boolean = false,
    val alternateRowColors: Boolean = true,
    val borderStyle: BorderStyle = BorderStyle.SIMPLE
)

enum class BorderStyle {
    NONE,
    SIMPLE,
    DOUBLE,
    THICK
}

/**
 * Opzioni export foto per Word
 */
data class PhotoExportOptions(
    val maxWidth: Int = 800,
    val maxHeight: Int = 600,
    val quality: Int = 85,
    val format: String = "JPEG"
)

/**
 * Foto processata per inserimento in Word
 */
data class ProcessedPhoto(
    val fileName: String,
    val processedData: ByteArray,
    val width: Int,
    val height: Int,
    val fileSize: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessedPhoto

        if (fileName != other.fileName) return false
        if (!processedData.contentEquals(other.processedData)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (fileSize != other.fileSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + processedData.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + fileSize.hashCode()
        return result
    }
}

// Import necessari per Apache POI (placeholder - da sostituire con import reali)
typealias XWPFTable = org.apache.poi.xwpf.usermodel.XWPFTable
typealias ParagraphAlignment = org.apache.poi.xwpf.usermodel.ParagraphAlignment

// Modelli mappati (placeholder - da implementare nel DataMapper)
data class MappedExportData(
    val checkup: MappedCheckup,
    val sections: List<MappedCheckupSection>,
    val spareParts: List<MappedSparePart>,
    val metadata: ExportMetadata
)

data class MappedCheckup(
    val id: String,
    val islandType: MappedIslandType
)

data class MappedIslandType(
    val name: String,
    val displayName: String
)

data class MappedCheckupSection(
    val id: String,
    val title: String,
    val description: String,
    val notes: String,
    val items: List<MappedCheckItem>
)

data class MappedCheckItem(
    val id: String,
    val title: String,
    val note: String,
    val statusDisplay: String,
    val criticalityDisplay: String,
    val photos: List<MappedPhoto>
)

data class MappedPhoto(
    val id: String,
    val fileName: String,
    val filePath: String,
    val caption: String
)

data class MappedSparePart(
    val id: String,
    val partNumber: String,
    val description: String,
    val quantity: Int,
    val urgencyDisplay: String,
    val notes: String
)