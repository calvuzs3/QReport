package net.calvuz.qreport.data.export.word

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.data.export.photo.PhotoExportManager
import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.export.*
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.model.photo.*
import net.calvuz.qreport.domain.model.spare.SparePart
import net.calvuz.qreport.util.DateTimeUtils.toFilenameSafeDate
import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.util.Units
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generatore di report Word consolidato che utilizza i componenti esistenti del progetto
 *
 * COMPONENTI UTILIZZATI:
 * - PhotoExportManager: per gestione foto avanzata
 * - FileManagerImpl: per operazioni file base
 * - DomainMappers: per conversioni domain models
 * - ImageProcessor: per elaborazione immagini
 *
 * LOGICA INTEGRATA:
 * - WordTemplateEngine: Apache POI template logic
 * - WordStyleEngine: Styling e formattazione
 */
@Singleton
class WordReportGenerator @Inject constructor(
    private val photoExportManager: PhotoExportManager,
    private val fileManager: FileManager
) {

    /**
     * Genera report Word completo con foto integrate
     */
    suspend fun generateWordReport(
        exportData: ExportData,
        exportOptions: ExportOptions = ExportOptions.complete()
    ): ExportResult = withContext(Dispatchers.IO) {

        try {
            Timber.d("Inizio generazione report Word per checkup: ${exportData.checkup.id}")

            // 1. Prepara directory di export
            val exportDirectory = prepareExportDirectory(exportData.checkup.id)

            // 2. Esporta foto nella cartella FOTO (usando PhotoExportManager esistente)
            val photoExportResult = exportPhotosForReport(exportData, exportDirectory)

            // 3. Crea documento Word con template integrato
            val document = createWordDocument()

            // 4. Genera contenuto usando styling integrato
            generateDocumentContent(document, exportData, photoExportResult, exportOptions)

            // 5. Salva documento
            val wordFile = saveWordDocument(document, exportDirectory, exportData)

            Timber.d("Report Word generato: ${wordFile.absolutePath}")

            ExportResult.Success(
                filePath = wordFile.absolutePath,
                fileName = wordFile.name,
                fileSize = wordFile.length(),
                format = ExportFormat.WORD
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore generazione report Word")
            ExportResult.Error(e, ExportErrorCode.DOCUMENT_GENERATION_ERROR)
        }
    }

    /**
     * Prepara directory per l'export usando FileManager esistente
     */
    private fun prepareExportDirectory(checkupId: String): File {
        val exportsDir = File(fileManager.getExportsDirectory())
        val checkupDir = File(exportsDir, "checkup_$checkupId")

        if (!checkupDir.exists()) {
            checkupDir.mkdirs()
        }

        return checkupDir
    }

    /**
     * Esporta foto utilizzando PhotoExportManager esistente
     */
    private suspend fun exportPhotosForReport(
        exportData: ExportData,
        exportDirectory: File
    ): PhotoExportResult {

        return photoExportManager.exportPhotosToFolder(
            exportData = exportData,
            targetDirectory = exportDirectory,
            namingStrategy = PhotoNamingStrategy.STRUCTURED,
            quality = PhotoQuality.OPTIMIZED,
            preserveExifData = false,
            addWatermark = true,
            watermarkText = "QReport",
            generateIndex = false // Non serve per Word
        )
    }

    /**
     * Crea nuovo documento Word con template base
     * TEMPLATE ENGINE INTEGRATO
     */
    private fun createWordDocument(): XWPFDocument {
        return XWPFDocument().apply {
            // Imposta proprietà documento
            properties.coreProperties.creator = "QReport"
            properties.coreProperties.description = "Checkup Report Generato Automaticamente"
        }
    }

    /**
     * Genera tutto il contenuto del documento
     * COMBINA: Template Engine + Style Engine + Data Mapping
     */
    private suspend fun generateDocumentContent(
        document: XWPFDocument,
        exportData: ExportData,
        photoExportResult: PhotoExportResult,
        exportOptions: ExportOptions
    ) {
        // 1. Header con loghi e info cliente (STYLE ENGINE INTEGRATO)
        generateDocumentHeader(document, exportData)

        // 2. Executive Summary
        generateExecutiveSummary(document, exportData)

        // 3. Dettaglio controlli per modulo (DATA MAPPING INTEGRATO)
        generateModuleDetails(document, exportData, photoExportResult)

        // 4. Spare Parts se presenti
        if (exportData.checkup.spareParts.isNotEmpty()) {
            generateSparePartsSection(document, exportData.checkup.spareParts)
        }

        // 5. Footer con firma digitale
        generateDocumentFooter(document, exportData)
    }

    /**
     * Genera header documento con styling professionale
     * STYLE ENGINE INTEGRATO
     */
    private fun generateDocumentHeader(document: XWPFDocument, exportData: ExportData) {

        // Titolo principale
        val titleParagraph = document.createParagraph().apply {
            alignment = ParagraphAlignment.CENTER
        }

        titleParagraph.createRun().apply {
            setText("REPORT CHECKUP ${exportData.checkup.islandType.displayName.uppercase()}")
            isBold = true
            fontSize = 18
            color = "1F4E79" // Corporate blue
        }

        // Sottotitolo con info cliente
        val clientParagraph = document.createParagraph().apply {
            alignment = ParagraphAlignment.CENTER
            spacingAfter = 400
        }

        clientParagraph.createRun().apply {
            setText("Cliente: ${exportData.checkup.header.clientInfo.companyName}")
            fontSize = 14
            color = "5B5B5B"
        }

        // Tabella info generale (TEMPLATE ENGINE INTEGRATO)
        createInfoTable(document, exportData)
    }

    /**
     * Crea tabella informazioni generali con styling
     */
    private fun createInfoTable(document: XWPFDocument, exportData: ExportData) {
        val table = document.createTable(6, 2).apply {
            width = 5000 // Full width
        }

        val header = exportData.checkup.header

        val rows = listOf(
            "Data Check-up" to header.checkUpDate.toString(),
            "Tecnico" to header.technicianInfo.name,
            "Azienda Tecnico" to header.technicianInfo.company,
            "Isola Serial Number" to header.islandInfo.serialNumber,
            "Modello Isola" to header.islandInfo.model,
            "Ore Funzionamento" to "${header.islandInfo.operatingHours}h"
        )

        rows.forEachIndexed { index, (key, value) ->
            val row = table.getRow(index)

            // Prima colonna (chiave) - STILE HEADER
            row.getCell(0).apply {
                text = key
                paragraphs[0].runs[0].apply {
                    isBold = true
                    color = "1F4E79"
                }
                color = "F2F2F2"
            }

            // Seconda colonna (valore) - STILE CONTENT
            row.getCell(1).apply {
                text = value
            }
        }
    }

    /**
     * Genera executive summary con statistiche
     */
    private fun generateExecutiveSummary(document: XWPFDocument, exportData: ExportData) {

        // Titolo sezione
        createSectionTitle(document, "RIEPILOGO ESECUTIVO")

        val stats = calculateCheckupStatistics(exportData)

        val summaryParagraph = document.createParagraph()
        summaryParagraph.createRun().apply {
            setText("Il check-up è stato completato con i seguenti risultati:\n\n")
        }

        val statsList = listOf(
            "Controlli totali: ${stats.totalItems}",
            "Controlli OK: ${stats.okItems} (${stats.okPercentage}%)",
            "Controlli NOK: ${stats.nokItems} (${stats.nokPercentage}%)",
            "Elementi critici: ${stats.criticalItems}",
            "Foto totali: ${stats.totalPhotos}",
            "Moduli verificati: ${stats.modulesCount}"
        )

        statsList.forEach { stat ->
            val listParagraph = document.createParagraph()
            listParagraph.createRun().apply {
                setText("• $stat")
            }
        }
    }

    /**
     * Genera dettagli per ogni modulo con foto integrate
     * DATA MAPPING INTEGRATO usando exportData.itemsByModule
     */
    private suspend fun generateModuleDetails(
        document: XWPFDocument,
        exportData: ExportData,
        photoExportResult: PhotoExportResult
    ) {

        createSectionTitle(document, "DETTAGLIO CONTROLLI")

        exportData.itemsByModule.forEach { (moduleType, checkItems) ->

            // Titolo modulo
            createModuleTitle(document, moduleType.displayName)

            // Tabella check items del modulo
            createCheckItemsTable(document, checkItems)

            // Foto del modulo se presenti
            if (photoExportResult is PhotoExportResult.Success) {
                val modulePhotos = photoExportResult.exportedPhotos.filter {
                    it.moduleInfo.moduleType == moduleType
                }

                if (modulePhotos.isNotEmpty()) {
                    insertModulePhotos(document, modulePhotos)
                }
            }
        }
    }

    /**
     * Crea tabella check items con styling professionale
     */
    private fun createCheckItemsTable(document: XWPFDocument, checkItems: List<CheckItem>) {
        val table = document.createTable(checkItems.size + 1, 4).apply {
            width = 5000
        }

        // Header tabella
        val headerRow = table.getRow(0)
        val headers = listOf("Controllo", "Stato", "Criticità", "Note")

        headers.forEachIndexed { index, header ->
            headerRow.getCell(index).apply {
                text = header
                color = "1F4E79"
                paragraphs[0].runs[0].apply {
                    isBold = true
                    color = "FFFFFF"
                }
            }
        }

        // Righe dati
        checkItems.forEachIndexed { index, item ->
            val row = table.getRow(index + 1)

            row.getCell(0).text = item.description
            row.getCell(1).apply {
                text = item.status.displayName
                // Colore basato su stato
                val statusColor = when (item.status) {
                    CheckItemStatus.OK -> "00B050"
                    CheckItemStatus.NOK -> "FF0000"
                    CheckItemStatus.PENDING -> "FFC000"
                    else -> "000000"
                }
                paragraphs[0].runs[0].color = statusColor
            }
            row.getCell(2).text = item.criticality.displayName
            row.getCell(3).text = item.notes
        }
    }

    /**
     * Inserisce foto del modulo nel documento
     */
    private suspend fun insertModulePhotos(
        document: XWPFDocument,
        modulePhotos: List<ExportedPhoto>
    ) = withContext(Dispatchers.IO) {

        val photoParagraph = document.createParagraph()
        photoParagraph.createRun().apply {
            setText("Foto evidenze:")
            isBold = true
        }

        modulePhotos.take(4).forEach { exportedPhoto -> // Max 4 foto per modulo
            try {
                val photoFile = File(exportedPhoto.exportedPath)
                if (photoFile.exists()) {

                    val photoPara = document.createParagraph().apply {
                        alignment = ParagraphAlignment.CENTER
                    }

                    val run = photoPara.createRun()

                    FileInputStream(photoFile).use { fis ->
                        run.addPicture(
                            fis,
                            XWPFDocument.PICTURE_TYPE_JPEG,
                            exportedPhoto.exportedFileName,
                            Units.toEMU(300.0), // Larghezza - Double
                            Units.toEMU(200.0)  // Altezza - Double
                        )
                    }

                    // Caption foto
                    if (exportedPhoto.originalPhoto.caption.isNotBlank()) {
                        val captionPara = document.createParagraph().apply {
                            alignment = ParagraphAlignment.CENTER
                        }
                        captionPara.createRun().apply {
                            setText(exportedPhoto.originalPhoto.caption)
                            isItalic = true
                            fontSize = 10
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Errore inserimento foto: ${exportedPhoto.exportedFileName}")
            }
        }
    }

    /**
     * Genera sezione spare parts
     */
    private fun generateSparePartsSection(document: XWPFDocument, spareParts: List<SparePart>) {

        createSectionTitle(document, "PARTI DI RICAMBIO RICHIESTE")

        val table = document.createTable(spareParts.size + 1, 5).apply {
            width = 5000
        }

        // Header
        val headers = listOf("Codice", "Descrizione", "Quantità", "Urgenza", "Note")
        val headerRow = table.getRow(0)

        headers.forEachIndexed { index, header ->
            headerRow.getCell(index).apply {
                text = header
                color = "1F4E79"
                paragraphs[0].runs[0].apply {
                    isBold = true
                    color = "FFFFFF"
                }
            }
        }

        // Dati
        spareParts.forEachIndexed { index, part ->
            val row = table.getRow(index + 1)
            row.getCell(0).text = part.partNumber
            row.getCell(1).text = part.description
            row.getCell(2).text = part.quantity.toString()
            row.getCell(3).text = part.urgency.displayName
            row.getCell(4).text = part.notes
        }
    }

    /**
     * Genera footer con firma digitale
     */
    private fun generateDocumentFooter(document: XWPFDocument, exportData: ExportData) {

        val footerParagraph = document.createParagraph().apply {
            spacingBefore = 800
            alignment = ParagraphAlignment.CENTER
        }

        val techInfo = exportData.checkup.header.technicianInfo

        footerParagraph.createRun().apply {
            setText("\n\nTecnico Responsabile: ${techInfo.name}\n")
            setText("Azienda: ${techInfo.company}\n")
            setText("Data completamento: ${exportData.checkup.completedAt.toString()}\n")
            setText("\nDocumento generato automaticamente da QReport")
            fontSize = 10
            isItalic = true
        }
    }

    /**
     * Salva documento Word con naming appropriato
     */
    private fun saveWordDocument(
        document: XWPFDocument,
        exportDirectory: File,
        exportData: ExportData
    ): File {

        val fileName = generateWordFileName(exportData)
        val wordFile = File(exportDirectory, fileName)

        FileOutputStream(wordFile).use { fos ->
            document.write(fos)
        }

        document.close()
        return wordFile
    }

    /**
     * Genera nome file Word appropriato
     */
    private fun generateWordFileName(exportData: ExportData): String {
        val timestamp = (exportData.checkup.completedAt?.toFilenameSafeDate())
        val clientName = exportData.checkup.header.clientInfo.companyName
            .replace(" ", "")
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .take(15)

        return "Checkup_${exportData.checkup.islandType.name}_${clientName}_${timestamp}.docx"
    }

    // ===== HELPER FUNCTIONS =====

    private fun createSectionTitle(document: XWPFDocument, title: String) {
        val titleParagraph = document.createParagraph().apply {
            spacingBefore = 600
            spacingAfter = 200
        }

        titleParagraph.createRun().apply {
            setText(title)
            isBold = true
            fontSize = 16
            color = "1F4E79"
        }
    }

    private fun createModuleTitle(document: XWPFDocument, moduleTitle: String) {
        val moduleParagraph = document.createParagraph().apply {
            spacingBefore = 400
            spacingAfter = 100
        }

        moduleParagraph.createRun().apply {
            setText("MODULO: $moduleTitle")
            isBold = true
            fontSize = 14
            color = "2F5597"
        }
    }

    /**
     * Calcola statistiche checkup usando domain models
     */
    private fun calculateCheckupStatistics(exportData: ExportData): CheckupStatistics {
        val allItems = exportData.itemsByModule.values.flatten()
        val totalPhotos = allItems.sumOf { it.photos.size }

        return CheckupStatistics(
            totalItems = allItems.size,
            okItems = allItems.count { it.status == CheckItemStatus.OK },
            nokItems = allItems.count { it.status == CheckItemStatus.NOK },
            criticalItems = allItems.count { it.criticality.name == "CRITICAL" },
            totalPhotos = totalPhotos,
            modulesCount = exportData.itemsByModule.size
        )
    }
}

// ===== DATA CLASSES =====

/**
 * Statistiche checkup per executive summary
 */
private data class CheckupStatistics(
    val totalItems: Int,
    val okItems: Int,
    val nokItems: Int,
    val criticalItems: Int,
    val totalPhotos: Int,
    val modulesCount: Int
) {
    val okPercentage: Int get() = if (totalItems > 0) (okItems * 100) / totalItems else 0
    val nokPercentage: Int get() = if (totalItems > 0) (nokItems * 100) / totalItems else 0
}