package net.calvuz.qreport.export.data.word

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.export.data.photo.PhotoExportManager
import net.calvuz.qreport.checkup.domain.model.CheckItem
import net.calvuz.qreport.checkup.domain.model.CheckUpSingleStatistics
import net.calvuz.qreport.checkup.domain.model.spare.SparePart
import net.calvuz.qreport.checkup.presentation.model.CheckItemStatusExt.getDisplayName
import net.calvuz.qreport.checkup.presentation.model.CheckItemStatusExt.getReportColor
import net.calvuz.qreport.app.util.DateTimeUtils.toFilenameSafeDate
import net.calvuz.qreport.export.domain.model.ExportErrorCode
import net.calvuz.qreport.export.domain.reposirory.ExportData
import net.calvuz.qreport.export.domain.reposirory.ExportFileRepository
import net.calvuz.qreport.export.domain.reposirory.ExportOptions
import net.calvuz.qreport.export.domain.reposirory.PhotoNamingStrategy
import net.calvuz.qreport.export.domain.reposirory.PhotoQuality
import net.calvuz.qreport.photo.domain.model.ExportedPhoto
import net.calvuz.qreport.photo.domain.model.PhotoExportResult
import net.calvuz.qreport.checkup.presentation.model.CheckUpStatusExt.getDisplayName
import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.util.Units
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generatore di report Word consolidato che utilizza ExportFileRepository
 *
 * COMPONENTI UTILIZZATI:
 * - PhotoExportManager: per gestione foto avanzata
 * - ExportFileRepository: per operazioni file export specifiche
 * - ImageProcessor: per elaborazione immagini
 *
 * LOGICA INTEGRATA:
 * - WordTemplateEngine: Apache POI template logic
 * - WordStyleEngine: Styling e formattazione
 */
@Singleton
class WordReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoExportManager: PhotoExportManager,
    private val exportFileRepository: ExportFileRepository
) {

//    /**
//     * Esporta foto utilizzando PhotoExportManager esistente
//     */
//    private suspend fun exportPhotosForReport(
//        exportData: ExportData,
//        exportDirectory: File
//    ): PhotoExportResult {
//
//        return photoExportManager.exportPhotosToFolder(
//            exportData = exportData,
//            targetDirectory = exportDirectory,
//            namingStrategy = PhotoNamingStrategy.STRUCTURED,
//            quality = PhotoQuality.OPTIMIZED,
//            preserveExifData = false,
//            addWatermark = true,
//            watermarkText = "QReport",
//            generateIndex = false // Non serve per Word
//        )
//    }

    /**
     * Create photos section with subdirectory support
     */
    private suspend fun createPhotosSection(
        document: XWPFDocument,
        exportData: ExportData,
        exportDirectory: File
    ): PhotoExportResult {

        // Create photos subdirectory using ExportFileRepository
        val photosSubdirResult =
            exportFileRepository.createPhotosSubdirectory(exportDirectory.absolutePath)
        val photosDirectory = when (photosSubdirResult) {
            is QrResult.Success -> File(photosSubdirResult.data)
            is QrResult.Error -> {
                Timber.w("Failed to create photos subdirectory, using export directory")
                exportDirectory
            }
        }

        // Export photos to subdirectory
        val photoResult = photoExportManager.exportPhotosToFolder(
            exportData = exportData,
            targetDirectory = photosDirectory,
            namingStrategy = PhotoNamingStrategy.STRUCTURED,
            quality = PhotoQuality.OPTIMIZED,
            preserveExifData = false,
            addWatermark = true,
            watermarkText = "QReport",
            generateIndex = false
        )

        // Add photos to Word document
        when (photoResult) {
            is PhotoExportResult.Success -> {
                addPhotosToDocument(document, photoResult.exportedPhotos, photosDirectory)
            }

            is PhotoExportResult.Error -> {
                Timber.w("Photo export failed: ${photoResult.errorCode}")
            }
        }

        return photoResult
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
        }

        // Sottotitolo con info client
        val subtitleParagraph = document.createParagraph().apply {
            alignment = ParagraphAlignment.CENTER
        }

        subtitleParagraph.createRun().apply {
            setText("${exportData.checkup.header.clientInfo.companyName} - ${exportData.checkup.header.clientInfo}") // .client.name} - ${exportData.facility.name}")
            fontSize = 14
        }

        // Data e tecnico
        val infoParagraph = document.createParagraph().apply {
            alignment = ParagraphAlignment.CENTER
        }

        infoParagraph.createRun().apply {
            setText("Data: ${exportData.checkup.completedAt?.toFilenameSafeDate() ?: "N/A"}")
            fontSize = 12
        }

        document.createParagraph() // Riga vuota
    }

    /**
     * Crea tabella informazioni checkup
     */
    private fun createInfoTable(document: XWPFDocument, exportData: ExportData) {
        val table = document.createTable(4, 2)

        // Header tabella
        val headerRow = table.getRow(0)
        headerRow.getCell(0).setText("INFORMAZIONI CHECKUP")
        headerRow.getCell(1).setText("")

        // Righe dati
        table.getRow(1).getCell(0).setText("Isola")
        table.getRow(1).getCell(1).setText(exportData.checkup.header.islandInfo.serialNumber)

        table.getRow(2).getCell(0).setText("Stato")
        table.getRow(2).getCell(1).setText(exportData.checkup.status.getDisplayName(context))

        table.getRow(3).getCell(0).setText("Completamento")
        table.getRow(3).getCell(1).setText("${exportData.statistics.completionPercentage}%")

        document.createParagraph() // Riga vuota
    }

    /**
     * Genera executive summary
     */
    private fun generateExecutiveSummary(document: XWPFDocument, exportData: ExportData) {
        val summaryParagraph = document.createParagraph()
        summaryParagraph.createRun().apply {
            setText("RIEPILOGO ESECUTIVO")
            isBold = true
            fontSize = 14
        }

        val contentParagraph = document.createParagraph()
        contentParagraph.createRun().apply {
            setText(
                "Il checkup dell'isola ${exportData.checkup.header.islandInfo.serialNumber} ha evidenziato ${
                    exportData.statistics.completedItems
                } items completati su ${exportData.statistics.totalItems} totali."
            )
        }

        document.createParagraph() // Riga vuota
    }

    /**
     * Genera dettagli moduli
     */
    private fun generateModuleDetails(
        document: XWPFDocument,
        exportData: ExportData,
        photoExportResult: PhotoExportResult
    ) {
        val modulesParagraph = document.createParagraph()
        modulesParagraph.createRun().apply {
            setText("DETTAGLI MODULI")
            isBold = true
            fontSize = 14
        }

        exportData.itemsByModule.forEach { (moduleType, items) ->
            val moduleParagraph = document.createParagraph()
            moduleParagraph.createRun().apply {
                setText("Modulo ${moduleType.displayName}")
                isBold = true
            }

            items.forEach { item ->
                val itemParagraph = document.createParagraph()
                itemParagraph.createRun().apply {
                    setText("• ${item.itemCode /*.name*/}: ${item.status.getDisplayName(context)}")
                    if (item.notes.isNotBlank()) {
                        addBreak()
                        setText("  Note: ${item.notes}")
                    }
                }
            }
        }

        document.createParagraph() // Riga vuota
    }

    /**
     * Aggiungi foto al documento
     */
    private fun addPhotosToDocument(
        document: XWPFDocument,
        photos: List<ExportedPhoto>,
        photosDirectory: File
    ) {
        if (photos.isEmpty()) return

        val photosParagraph = document.createParagraph()
        photosParagraph.createRun().apply {
            setText("FOTO ALLEGATE")
            isBold = true
            fontSize = 14
        }

        photos.forEach { photo ->
            try {
                val photoFile = File(photosDirectory, photo.exportedFileName)
                if (photoFile.exists()) {
                    val photoParagraph = document.createParagraph()

                    // Add photo description
                    photoParagraph.createRun().apply {
                        setText("${photo.exportedFileName}") /*.description ?: photo.fileName}*/
                        addBreak()
                    }

                    // Add photo image
                    FileInputStream(photoFile).use { photoStream ->
                        photoParagraph.createRun().addPicture(
                            photoStream,
                            XWPFDocument.PICTURE_TYPE_JPEG,
                            photo.exportedFileName,
                            Units.toEMU(300.0), // Width
                            Units.toEMU(200.0)  // Height
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to add photo: ${photo.exportedFileName}")
            }
        }
    }

    /**
     * Genera sezione spare parts
     */
    private fun generateSparePartsSection(document: XWPFDocument, spareParts: List<SparePart>) {
        val sparePartsParagraph = document.createParagraph()
        sparePartsParagraph.createRun().apply {
            setText("RICAMBI NECESSARI")
            isBold = true
            fontSize = 14
        }

        spareParts.forEach { sparePart ->
            val partParagraph = document.createParagraph()
            partParagraph.createRun().apply {
                setText("• ${sparePart.description} - Quantità: ${sparePart.quantity}")  // .name
                if (sparePart.notes.isNotBlank()) {
                    addBreak()
                    setText("  Note: ${sparePart.notes}")
                }
            }
        }

        document.createParagraph() // Riga vuota
    }

    /**
     * Genera footer documento
     */
    private fun generateDocumentFooter(document: XWPFDocument, exportData: ExportData) {
        val footerParagraph = document.createParagraph().apply {
            alignment = ParagraphAlignment.CENTER
        }

        footerParagraph.createRun().apply {
            setText("Report generato da QReport - ${Clock.System.now().toFilenameSafeDate()}")
            fontSize = 10
        }
    }





    /**
     * VERSIONE ALTERNATIVA del generatore - semplificata
     */
//    suspend fun generateSimplifiedWordReport(
//        exportData: ExportData,
//        targetDirectory: File
//    ): ExportResult = withContext(Dispatchers.IO) {
//
//        try {
//            Timber.d("Starting simplified Word report generation for checkup ${exportData.checkup.id}")
//
//            // 1. Prepare export directory using ExportFileRepository
//            val exportDirectory = prepareExportDirectory(
//                checkupId = exportData.checkup.id,
//                clientName = exportData.checkup.header.clientInfo.companyName,
//                format = ExportFormat.WORD
//            )
//
//            // 2. Check storage space
//            val estimatedSizeMB = estimateDocumentSize(exportData)
//            when (val spaceCheck = exportFileRepository.checkStorageSpace(estimatedSizeMB)) {
//                is QrResult.Error -> {
//                    return@withContext ExportResult.Error(
//                        exception = Exception("Storage space check failed"),
//                        errorCode = ExportErrorCode.INSUFFICIENT_STORAGE,
//                        format = ExportFormat.WORD
//                    )
//                }
//
//                is QrResult.Success -> {
//                    if (!spaceCheck.data) {
//                        return@withContext ExportResult.Error(
//                            exception = Exception("Insufficient storage space"),
//                            errorCode = ExportErrorCode.INSUFFICIENT_STORAGE,
//                            format = ExportFormat.WORD
//                        )
//                    }
//                }
//            }
//
//            // 3. Create Word document
//            val document = XWPFDocument()
//
//            // 4. Add basic content
//            generateDocumentHeader(document, exportData)
//            createInfoTable(document, exportData)
//
//            // 5. Export and add photos
//            val photoResult = createPhotosSection(document, exportData, exportDirectory)
//
//            // 6. Add basic module information
//            generateModuleDetails(document, exportData, photoResult)
//
//            // 7. Save document
//            val fileName = generateWordFileName(exportData)
//            val filePath = saveWordDocument(document, fileName, exportDirectory)
//
//            // 8. Create export manifest
//            val exportInfo = ExportInfo(
//                checkupId = exportData.checkup.id,
//                exportDirectory = exportDirectory.absolutePath,
//                createdAt = System.currentTimeMillis(),
//                format = ExportFormat.WORD,
//                sizeBytes = File(filePath).length(),
////                fileCount = 1 + (photoResult as? PhotoExportResult.Success)?.exportedPhotos?.size ?: 0,
//                fileCount = 1 + ((photoResult as? PhotoExportResult.Success)?.exportedPhotos?.size
//                    ?: 0),
//                hasPhotos = photoResult is PhotoExportResult.Success,
//                hasAttachments = false,
//                status = ExportStatus.COMPLETED
//            )
//
//            exportFileRepository.createExportManifest(exportDirectory.absolutePath, exportInfo)
//
//            // 9. Cleanup and return result
//            document.close()
//
//            // ✅ CORRECTED: Use ExportResult.Success as defined in ExportFileRepository
//            val result = ExportResult.Success(
//                filePath = filePath,
//                fileName = fileName,
//                fileSize = File(filePath).length(),
//                format = ExportFormat.WORD,
//                generatedAt = LocalDateTime.now()
//            )
//
//            Timber.d("Simplified Word report generated successfully: $filePath")
//            result
//
//        } catch (e: ExportException) {
//            Timber.e(e, "Export-specific error generating simplified Word report")
//            ExportResult.Error(
//                exception = e.cause ?: e,
//                errorCode = e.errorCode ?: ExportErrorCode.DOCUMENT_GENERATION_ERROR,
//                format = ExportFormat.WORD
//            )
//        } catch (e: Exception) {
//            Timber.e(e, "Unexpected error generating simplified Word report")
//            ExportResult.Error(
//                exception = e,
//                errorCode = ExportErrorCode.DOCUMENT_GENERATION_ERROR,
//                format = ExportFormat.WORD
//            )
//        }
//    }

    // ===== PRIVATE HELPER METHODS =====

    private fun estimateDocumentSize(exportData: ExportData): Int {
        val baseSize = 2 // MB for document structure
        val photosSize = exportData.itemsByModule.values.flatten()
            .sumOf { it.photos.size } * 0.5 // 500KB per photo estimate
        return (baseSize + photosSize).toInt()
    }

    private fun calculateCheckupStatistics(exportData: ExportData): CheckUpSingleStatistics {
        // Use statistics from exportData if available
        return exportData.statistics
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
    suspend fun generateDocumentContent(
        exportData: ExportData,
        options: ExportOptions,
        workingDirectory: File  // ✅ Temporary working directory
    ): XWPFDocument = withContext(Dispatchers.IO) {

        try {
            Timber.d("Starting Word document content generation for checkup ${exportData.checkup.id}")

            // Create Word document
            val document = XWPFDocument()

            // Add document content
            generateDocumentHeader(document, exportData)
            createInfoTable(document, exportData)
            generateExecutiveSummary(document, exportData)

            // ✅ Use working directory for temporary photo operations
            val photoResult = createPhotosSection(document, exportData, workingDirectory)

            // Generate module details with photos
            generateModuleDetails(document, exportData, photoResult)

            // Add spare parts if any
            if (exportData.checkup.spareParts.isNotEmpty()) {
                generateSparePartsSection(document, exportData.checkup.spareParts)
            }

            // Add document footer
            generateDocumentFooter(document, exportData)

            Timber.d("Word document content generated successfully")
            document

        } catch (e: Exception) {
            Timber.e(e, "Error generating Word document content")
            throw e
        }
    }

    // ✅ UPDATED to use photos subdirectory:
//    private suspend fun createPhotosSection(
//        document: XWPFDocument,
//        exportData: ExportData,
//        exportDirectory: File
//    ): PhotoExportResult {
//
//        // Create photos subdirectory
//        val photosSubdirResult = exportFileRepository.createPhotosSubdirectory(exportDirectory.absolutePath)
//        val photosDirectory = when (photosSubdirResult) {
//            is QrResult.Success -> File(photosSubdirResult.data)
//            is QrResult.Error -> {
//                Timber.w("Failed to create photos subdirectory, using export directory")
//                exportDirectory
//            }
//        }
//
//        // Export photos to subdirectory
//        val photoResult = photoExportManager.exportPhotosToFolder(
//            exportData = exportData,
//            targetDirectory = photosDirectory,
//            namingStrategy = PhotoNamingStrategy.STRUCTURED,
//            quality = PhotoQuality.OPTIMIZED,
//            preserveExifData = false,
//            addWatermark = true,
//            watermarkText = "QReport",
//            generateIndex = false
//        )
//
//        // Add photos to Word document
//        when (photoResult) {
//            is PhotoExportResult.Success -> {
//                addPhotosToDocument(document, photoResult.exportedPhotos, photosDirectory)
//            }
//            is PhotoExportResult.Error -> {
//                Timber.w("Photo export failed: ${photoResult.errorMessage}")
//            }
//        }
//
//        return photoResult
//    }

    /**
     * Genera header documento con styling professionale
     * STYLE ENGINE INTEGRATO
     */
//    private fun generateDocumentHeader(document: XWPFDocument, exportData: ExportData) {
//
//        // Titolo principale
//        val titleParagraph = document.createParagraph().apply {
//            alignment = ParagraphAlignment.CENTER
//        }
//
//        titleParagraph.createRun().apply {
//            setText("REPORT CHECKUP ${exportData.checkup.islandType.displayName.uppercase()}")
//            isBold = true
//            fontSize = 18
//            color = "1F4E79" // Corporate blue
//        }
//
//        // Sottotitolo con info cliente
//        val clientParagraph = document.createParagraph().apply {
//            alignment = ParagraphAlignment.CENTER
//            spacingAfter = 400
//        }
//
//        clientParagraph.createRun().apply {
//            setText("Cliente: ${exportData.checkup.header.clientInfo.companyName}")
//            fontSize = 14
//            color = "5B5B5B"
//        }
//
//        // Tabella info generale (TEMPLATE ENGINE INTEGRATO)
//        createInfoTable(document, exportData)
//    }

    /**
     * Crea tabella informazioni generali con styling
     */
//    private fun createInfoTable(document: XWPFDocument, exportData: ExportData) {
//        val table = document.createTable(6, 2).apply {
//            width = 5000 // Full width
//        }
//
//        val header = exportData.checkup.header
//
//        val rows = listOf(
//            "Data Check-up" to header.checkUpDate.toString(),
//            "Tecnico" to header.technicianInfo.name,
//            "Azienda Tecnico" to header.technicianInfo.company,
//            "Isola Serial Number" to header.islandInfo.serialNumber,
//            "Modello Isola" to header.islandInfo.model,
//            "Ore Funzionamento" to "${header.islandInfo.operatingHours}h"
//        )
//
//        rows.forEachIndexed { index, (key, value) ->
//            val row = table.getRow(index)
//
//            // Prima colonna (chiave) - STILE HEADER
//            row.getCell(0).apply {
//                text = key
//                paragraphs[0].runs[0].apply {
//                    isBold = true
//                    color = "1F4E79"
//                }
//                color = "F2F2F2"
//            }
//
//            // Seconda colonna (valore) - STILE CONTENT
//            row.getCell(1).apply {
//                text = value
//            }
//        }
//    }

    /**
     * Genera executive summary con statistiche
     */
//    private fun generateExecutiveSummary(document: XWPFDocument, exportData: ExportData) {
//
//        // Titolo sezione
//        createSectionTitle(document, "RIEPILOGO ESECUTIVO")
//
//        val stats = calculateCheckupStatistics(exportData)
//
//        val summaryParagraph = document.createParagraph()
//        summaryParagraph.createRun().apply {
//            setText("Il check-up è stato completato con i seguenti risultati:\n\n")
//        }
//
//        val statsList = listOf(
//            "Controlli totali: ${stats.totalItems}",
//            "Controlli OK: ${stats.okItems} (${stats.okPercentage}%)",
//            "Controlli NOK: ${stats.nokItems} (${stats.nokPercentage}%)",
//            "Elementi critici: ${stats.criticalIssues}",
//            "Foto totali: ${stats.photosCount}",
//            "Moduli verificati: ${stats.modulesCount}"
//        )
//
//        statsList.forEach { stat ->
//            val listParagraph = document.createParagraph()
//            listParagraph.createRun().apply {
//                setText("• $stat")
//            }
//        }
//    }

    /**
     * Genera dettagli per ogni modulo con foto integrate
     * DATA MAPPING INTEGRATO usando exportData.itemsByModule
     */
//    private suspend fun generateModuleDetails(
//        document: XWPFDocument,
//        exportData: ExportData,
//        photoExportResult: PhotoExportResult
//    ) {
//
//        createSectionTitle(document, "DETTAGLIO CONTROLLI")
//
//        exportData.itemsByModule.forEach { (moduleType, checkItems) ->
//
//            // Titolo modulo
//            createModuleTitle(document, moduleType.displayName)
//
//            // Tabella check items del modulo
//            createCheckItemsTable(document, checkItems)
//
//            // Foto del modulo se presenti
//            if (photoExportResult is PhotoExportResult.Success) {
//                val modulePhotos = photoExportResult.exportedPhotos.filter {
//                    it.moduleInfo.moduleType == moduleType
//                }
//
//                if (modulePhotos.isNotEmpty()) {
//                    insertModulePhotos(document, modulePhotos)
//                }
//            }
//        }
//    }

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
                text = item.status.getDisplayName(context = context)
                paragraphs[0].runs[0].color = item.status.getReportColor()
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
//    private fun generateSparePartsSection(document: XWPFDocument, spareParts: List<SparePart>) {
//
//        createSectionTitle(document, "PARTI DI RICAMBIO RICHIESTE")
//
//        val table = document.createTable(spareParts.size + 1, 5).apply {
//            width = 5000
//        }
//
//        // Header
//        val headers = listOf("Codice", "Descrizione", "Quantità", "Urgenza", "Note")
//        val headerRow = table.getRow(0)
//
//        headers.forEachIndexed { index, header ->
//            headerRow.getCell(index).apply {
//                text = header
//                color = "1F4E79"
//                paragraphs[0].runs[0].apply {
//                    isBold = true
//                    color = "FFFFFF"
//                }
//            }
//        }
//
//        // Dati
//        spareParts.forEachIndexed { index, part ->
//            val row = table.getRow(index + 1)
//            row.getCell(0).text = part.partNumber
//            row.getCell(1).text = part.description
//            row.getCell(2).text = part.quantity.toString()
//            row.getCell(3).text = part.urgency.displayName
//            row.getCell(4).text = part.notes
//        }
//    }

    /**
     * Genera footer con firma digitale
     */
//    private fun generateDocumentFooter(document: XWPFDocument, exportData: ExportData) {
//
//        val footerParagraph = document.createParagraph().apply {
//            spacingBefore = 800
//            alignment = ParagraphAlignment.CENTER
//        }
//
//        val techInfo = exportData.checkup.header.technicianInfo
//
//        footerParagraph.createRun().apply {
//            setText("\n\nTecnico Responsabile: ${techInfo.name}\n")
//            setText("Azienda: ${techInfo.company}\n")
//            setText("Data completamento: ${exportData.checkup.completedAt.toString()}\n")
//            setText("\nDocumento generato automaticamente da QReport")
//            fontSize = 10
//            isItalic = true
//        }
//    }

    /**
     * Salva documento Word con naming appropriato
     */
//    private fun saveWordDocument(
//        document: XWPFDocument,
//        exportDirectory: File,
//        exportData: ExportData
//    ): File {
//
//        val fileName = generateWordFileName(exportData)
//        val wordFile = File(exportDirectory, fileName)
//
//        FileOutputStream(wordFile).use { fos ->
//            document.write(fos)
//        }
//
//        document.close()
//        return wordFile
//    }
//    private suspend fun saveWordDocument(
//        document: XWPFDocument,
//        checkupId: String,
//        exportDirectory: File
//    ): String {
//        val fileName = exportFileRepository.generateExportFileName(
//            checkupId = checkupId,
//            format = ExportFormat.WORD,
//            includeTimestamp = true
//        )
//
//        return when (val fileResult = exportFileRepository.createExportFile(
//            directory = exportDirectory.absolutePath,
//            fileName = fileName,
//            format = ExportFormat.WORD
//        )) {
//            is QrResult.Success -> {
//                val filePath = fileResult.data
//
//                // Save document using Apache POI
//                FileOutputStream(filePath).use { outputStream ->
//                    document.write(outputStream)
//                }
//
//                when (val validateResult =
//                    exportFileRepository.validateExportFile(filePath, ExportFormat.WORD)) {
//                    is QrResult.Success -> {
//                        if (validateResult.data) {
//                            Timber.d("Word document saved and validated: $filePath")
//                            filePath
//                        } else {
//                            throw ExportException("Word document validation failed: $filePath")
//                        }
//                    }
//
//                    is QrResult.Error -> {
//                        throw ExportException("Failed to validate Word document", validateResult)
//                    }
//                }
//            }
//
//            is QrResult.Error -> {
//                throw ExportException("Failed to create Word export file", fileResult)
//            }
//        }
//    }

    /**
     * Genera nome file Word appropriato
     */
//    private fun generateWordFileName(exportData: ExportData): String {
//        val timestamp = (exportData.checkup.completedAt?.toFilenameSafeDate())
//        val clientName = exportData.checkup.header.clientInfo.companyName
//            .replace(" ", "")
//            .replace(Regex("[^a-zA-Z0-9]"), "")
//            .take(15)
//
//        return "Checkup_${exportData.checkup.islandType.name}_${clientName}_${timestamp}.docx"
//    }

//    suspend fun generateWordReport(
//        exportData: ExportData,
//        options: ExportOptions
//    ): ExportResult = withContext(Dispatchers.IO) {
//
//        try {
//            Timber.d("Starting Word report generation for checkup ${exportData.checkup.id}")
//
//            // 1. Prepare export directory using ExportFileRepository
//            val exportDirectory = prepareExportDirectory(exportData.checkup.id)
//
//            // 2. Check storage space
//            val estimatedSizeMB = estimateDocumentSize(exportData)
//            when (val spaceCheck = exportFileRepository.checkStorageSpace(estimatedSizeMB)) {
//                is QrResult.Error -> {
//                    return@withContext ExportResult.Error(
//                        Exception("Storage space check failed"),
//                        ExportErrorCode.INSUFFICIENT_STORAGE
//                    )
//                }
//
//                is QrResult.Success -> {
//                    if (!spaceCheck.data) {
//                        return@withContext ExportResult.Error(
//                            Exception("Insufficient storage space"),
//                            ExportErrorCode.INSUFFICIENT_STORAGE
//                        )
//                    }
//                }
//            }
//
//            // 3. Create Word document
//            val document = XWPFDocument()
//
//            // 4. Add document content
//            addDocumentHeader(document, exportData)
//            addCheckupSummary(document, exportData)
//            addModuleSections(document, exportData)
//
//            // 5. Export and add photos
//            val photoResult = createPhotosSection(document, exportData, exportDirectory)
//
//            // 6. Add spare parts if any
//            if (exportData.spareParts.isNotEmpty()) {
//                addSparePartsSection(document, exportData.spareParts)
//            }
//
//            // 7. Add statistics
//            addStatisticsSection(document, exportData.statistics)
//
//            // 8. Save document using ExportFileRepository
//            val filePath = saveWordDocument(document, exportData.checkup.id, exportDirectory)
//
//            // 9. Create export manifest
//            val exportInfo = ExportInfo(
//                checkupId = exportData.checkup.id,
//                exportDirectory = exportDirectory.absolutePath,
//                createdAt = System.currentTimeMillis(),
//                format = ExportFormat.WORD,
//                sizeBytes = File(filePath).length(),
//                fileCount = 1 + (photoResult as? PhotoExportResult.Success)?.exportedPhotos?.size
//                    ?: 0,
//                hasPhotos = photoResult is PhotoExportResult.Success,
//                hasAttachments = false
//            )
//
//            exportFileRepository.createExportManifest(exportDirectory.absolutePath, exportInfo)
//
//            // 10. Cleanup and finalize
//            document.close()
//
//            val result = ExportResult.Success(
//                filePath = filePath,
//                format = ExportFormat.WORD,
//                sizeBytes = File(filePath).length(),
//                exportDirectory = exportDirectory.absolutePath
//            )
//
//            Timber.d("Word report generated successfully: $filePath")
//            result
//
//        } catch (e: ExportException) {
//            Timber.e(e, "Export-specific error generating Word report")
//            ExportResult.Error(
//                e.cause ?: e,
//                e.errorCode ?: ExportErrorCode.DOCUMENT_GENERATION_ERROR
//            )
//        } catch (e: Exception) {
//            Timber.e(e, "Unexpected error generating Word report")
//            ExportResult.Error(e, ExportErrorCode.DOCUMENT_GENERATION_ERROR)
//        }
//    }

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

}

class ExportException(
    message: String,
    val errorResult: QrResult.Error<*, QrError>? = null,
    val errorCode: ExportErrorCode? = null,
    cause: Throwable? = null
) : Exception(message, cause)