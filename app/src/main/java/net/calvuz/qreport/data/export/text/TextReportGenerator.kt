package net.calvuz.qreport.data.export.text

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.domain.model.CriticalityLevel
import net.calvuz.qreport.domain.model.checkup.*
import net.calvuz.qreport.domain.model.export.ExportData
import net.calvuz.qreport.domain.model.export.ExportOptions
import net.calvuz.qreport.domain.model.export.PhotoNamingStrategy
import net.calvuz.qreport.domain.model.photo.Photo
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generator per report testuali in formato ASCII
 * Crea file .txt leggibili universalmente con tutte le informazioni del checkup
 */
@Singleton
class TextReportGenerator @Inject constructor() {

    /**
     * Genera report testuale completo
     *
     * @param exportData Dati del checkup da esportare
     * @param options Opzioni di export per configurazione
     * @return Contenuto del report come stringa
     */
    suspend fun generateTextReport(
        exportData: ExportData,
        options: ExportOptions
    ): String = withContext(Dispatchers.IO) {

        try {
            Timber.d("Generazione report testuale per checkup ${exportData.checkup.id}")

            buildString {
                // Header principale
                appendReportHeader()

                // Informazioni generali
                appendGeneralInfo(exportData)
                appendLine()

                // Riepilogo esecutivo
                appendExecutiveSummary(exportData)
                appendLine()

                // Dettaglio controlli per sezione
                appendSectionsDetail(exportData, options)

                // Parti di ricambio
                if (exportData.checkup.spareParts.isNotEmpty()) {
                    appendSpareParts(exportData.checkup.spareParts)
                    appendLine()
                }

                // Conclusioni e raccomandazioni
                appendConclusions(exportData)

                // Footer
                appendReportFooter(exportData)
            }

        } catch (e: Exception) {
            Timber.e(e, "Errore generazione report testuale")
            throw e
        }
    }

    /**
     * Header principale del report
     */
    private fun StringBuilder.appendReportHeader() {
        val title = "REPORT CHECKUP INDUSTRIALE"
        appendLine("=".repeat(80))
        appendLine(centerText(title, 80))
        appendLine("=".repeat(80))
        appendLine()
    }

    /**
     * Informazioni generali del checkup
     */
    private fun StringBuilder.appendGeneralInfo(exportData: ExportData) {
        val checkup = exportData.checkup
        val clientInfo = exportData.checkup.header.clientInfo // .metadata.clientInfo

        appendLine("INFORMAZIONI GENERALI")
        appendLine("-".times(21))
        appendLine("Cliente:              ${clientInfo.companyName}")
        appendLine("Contatto:             ${clientInfo.contactPerson}")
        if (clientInfo.site.isNotBlank()) {
            appendLine("Sito:                 ${clientInfo.site}")
        }
        if (clientInfo.address.isNotBlank()) {
            appendLine("Indirizzo:            ${clientInfo.address}")
        }
        appendLine("Tipo Isola:           ${checkup.islandType.displayName}")
        appendLine("Serial Isola:         ${checkup.header.islandInfo.serialNumber}")
        if (checkup.header.islandInfo.model.isNotBlank()) {
            appendLine("Modello Isola:        ${checkup.header.islandInfo.model}")
        }
        if (checkup.header.islandInfo.operatingHours > 0) {
            appendLine("Ore Funzionamento:    ${checkup.header.islandInfo.operatingHours}h")
        }
        appendLine("Data Checkup:         ${checkup.createdAt.toString()}")
        appendLine("Tecnico Responsabile: ${exportData.checkup.header.technicianInfo.name}")
        if (exportData.checkup.header.technicianInfo.company.isNotBlank()) {
            appendLine("Azienda Tecnico:      ${exportData.checkup.header.technicianInfo.company}")
        }

        // Orari di lavoro
        val startTime = checkup.createdAt
        val completedAt = checkup.completedAt

        // Converti Instant a LocalDateTime nel timezone del sistema per formattazione
        val startLocalTime = startTime.toString().let {
            if (it.length >= 16) it.substring(11, 16) else "N/A"
        }
        val endLocalTime = completedAt?.toString()?.let {
            if (it.length >= 16) it.substring(11, 16) else "N/A"
        } ?: "In corso"

        appendLine("Ora Inizio:           $startLocalTime")
        appendLine("Ora Fine:             $endLocalTime")

        if (completedAt != null) {
            val duration = Duration.between(startTime, completedAt)
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            appendLine("Durata Totale:        ${hours}h ${minutes}m")
        }

        appendLine("Stato Checkup:        ${(checkup.status.displayName)}")

        if (checkup.header.notes.isNotBlank()) {   //} .notes.isNotBlank()) {
            appendLine("Note Generali:        ${checkup.header.notes}")    // .notes}")
        }
    }

    /**
     * Riepilogo esecutivo con statistiche
     */
    private fun StringBuilder.appendExecutiveSummary(exportData: ExportData) {
        val stats = calculateCheckupStats(exportData.itemsByModule)

        appendLine("RIEPILOGO ESECUTIVO")
        appendLine("-".repeat(18))
        appendLine("Stato Generale:       ${getOverallStatusText(stats)}")
        appendLine("Controlli Totali:     ${stats.totalItems}")
        appendLine("Controlli OK:         ${stats.okItems} (${String.format("%.1f", stats.okPercentage)}%)")
        appendLine("Controlli NOK:        ${stats.nokItems} (${String.format("%.1f", stats.nokPercentage)}%)")
        appendLine("Controlli N/A:        ${stats.naItems} (${String.format("%.1f", stats.naPercentage)}%)")
        appendLine("Controlli Pending:    ${stats.pendingItems}")
        appendLine("Criticità Rilevate:   ${stats.criticalIssues}")
        appendLine("Avvisi Importanti:    ${stats.importantIssues}")
        appendLine("Foto Acquisite:       ${stats.totalPhotos}")

        if (stats.sectionsWithIssues > 0) {
            appendLine("Moduli con Problemi:  ${stats.sectionsWithIssues}/${stats.totalSections}")
        }

        // Indicatori di urgenza
        if (stats.criticalIssues > 0) {
            appendLine()
            appendLine("⚠️  ATTENZIONE: Rilevate ${stats.criticalIssues} criticità che richiedono intervento immediato!")
        } else if (stats.importantIssues > 0) {
            appendLine()
            appendLine("⚡ Presenti ${stats.importantIssues} problemi importanti da monitorare.")
        } else if (stats.okPercentage >= 95.0) {
            appendLine()
            appendLine("✅ Checkup completato con successo - Sistema in ottime condizioni.")
        }
    }

    /**
     * Dettaglio controlli per ogni modulo
     */
    private fun StringBuilder.appendSectionsDetail(exportData: ExportData, options: ExportOptions) {
        appendLine("=".repeat(80))
        appendLine(centerText("DETTAGLIO CONTROLLI", 80))
        appendLine("=".repeat(80))
        appendLine()

        exportData.itemsByModule.toList().forEachIndexed { index, (moduleType, checkItems) ->
            appendModuleDetail(moduleType, checkItems, index + 1, options)
            appendLine()
        }
    }

    /**
     * Dettaglio di un singolo modulo
     */
    private fun StringBuilder.appendModuleDetail(
        moduleType: net.calvuz.qreport.domain.model.module.ModuleType,
        checkItems: List<CheckItem>,
        moduleIndex: Int,
        options: ExportOptions
    ) {
        val moduleStats = calculateSectionStats(checkItems)

        // Header modulo
        val moduleTitle = "MODULO $moduleIndex: ${moduleType.displayName.uppercase()}"
        appendLine(moduleTitle)
        appendLine("-".repeat(moduleTitle.length))

        // Statistiche modulo
        appendLine("Controlli Totali: ${moduleStats.totalItems}  |  " +
                "OK: ${moduleStats.okItems}  |  " +
                "NOK: ${moduleStats.nokItems}  |  " +
                "Critici: ${moduleStats.criticalItems}")

        appendLine()

        // Check items del modulo
        checkItems.forEachIndexed { itemIndex, item ->
            appendCheckItemDetail(item, itemIndex + 1, options)
        }

        // Foto del modulo (opzionale)
        if (options.generatePhotoIndex == true) {
            val modulePhotos = checkItems.flatMap { it.photos }
            if (modulePhotos.isNotEmpty()) {
                appendLine()
                appendPhotoIndex(modulePhotos, moduleIndex, options.pho6toNamingStrategy ?: PhotoNamingStrategy.STRUCTURED)
            }
        }
    }

    /**
     * Dettaglio di un singolo check item
     */
    private fun StringBuilder.appendCheckItemDetail(
        item: CheckItem,
        itemIndex: Int,
        options: ExportOptions
    ) {
        val statusIcon = when (item.status) {
            CheckItemStatus.OK -> "✅"
            CheckItemStatus.NOK -> "❌"
            CheckItemStatus.PENDING -> "⏳"
            CheckItemStatus.NA -> "➖"
        }

        val criticalityIcon = when (item.criticality) {
            CriticalityLevel.CRITICAL -> "🔴"
            CriticalityLevel.IMPORTANT -> "🟡"
            CriticalityLevel.ROUTINE -> "🟢"
            CriticalityLevel.NA -> "➖"
        }

        appendLine("${itemIndex}. ${item.description}")
        appendLine("   Codice:      ${item.itemCode}")
        appendLine("   Stato:       $statusIcon ${item.status.displayName}")
        appendLine("   Criticità:   $criticalityIcon ${item.criticality.name}")

        if (item.notes.isNotBlank()) {
            appendLine("   Note:        ${item.notes}")
        }

        if (item.photos.isNotEmpty()) {
            appendLine("   Foto:        ${item.photos.size} allegata/e")
        }

        // Raccomandazioni per l'item
        val recommendation = generateItemRecommendations(item)
        if (recommendation.isNotBlank()) {
            appendLine("   Azione:      $recommendation")
        }

        appendLine()
    }

    /**
     * Indice delle foto per un modulo
     */
    private fun StringBuilder.appendPhotoIndex(
        photos: List<Photo>,
        moduleIndex: Int,
        namingStrategy: PhotoNamingStrategy
    ) {
        appendLine("INDICE FOTO MODULO $moduleIndex")
        appendLine("-".repeat(25))

        photos.forEachIndexed { photoIndex, photo ->
            val fileName = generatePhotoFileName(moduleIndex, photo, photoIndex, namingStrategy)
            appendLine("${photoIndex + 1}. $fileName")
            if (photo.caption.isNotBlank()) {
                appendLine("   Descrizione: ${photo.caption}")
            }
            appendLine("   Data:        ${photo.takenAt}")
        }
    }



/**
 * Riferimenti alle foto per un check item
 */
private fun StringBuilder.appendPhotoReferences(
    item: CheckItem,
    sectionIndex: Int,
    options: ExportOptions
) {
    appendLine("    Foto:       ${item.photos.size} foto acquisite")

    if (options.formats.contains(net.calvuz.qreport.domain.model.export.ExportFormat.PHOTO_FOLDER)) {
        // Se cartella FOTO è abilitata, mostra nomi file
        item.photos.forEachIndexed { photoIndex, photo ->
            val fileName = generatePhotoFileName(sectionIndex, item, photo, photoIndex, options.photoNamingStrategy ?: PhotoNamingStrategy.STRUCTURED)
            appendLine("                - $fileName")

            if (photo.caption.isNotBlank()) {
                appendLine("                  \"${photo.caption}\"")
            }
        }
    } else {
        // Se solo Word, mostra descrizioni
        item.photos.forEach { photo ->
            if (photo.caption.isNotBlank()) {
                appendLine("                - ${photo.caption}")
            }
        }
    }
}

/**
 * Raccomandazioni per item problematici
 */
private fun StringBuilder.appendItemRecommendations(item: CheckItem) {
    val recommendations = generateItemRecommendations(item)
    if (recommendations.isNotEmpty()) {
        appendLine("    Azione:     ${recommendations}")
    }
}

/**
 * Sezione parti di ricambio
 */
private fun StringBuilder.appendSpareParts(spareParts: List<SparePart>) {
    appendLine("=" * 80)
    appendLine(textFormatter.centerText("PARTI DI RICAMBIO", 80))
    appendLine("=" * 80)
    appendLine()

    if (spareParts.isEmpty()) {
        appendLine("Nessuna parte di ricambio richiesta.")
        return
    }

    appendLine("RICAMBI CONSIGLIATI")
    appendLine("-" * 18)
    appendLine()

    // Raggruppa per urgenza
    val partsByUrgency = spareParts.groupBy { it.urgency }

    // Critici prima
    partsByUrgency[SparePartUrgency.CRITICAL]?.let { criticalParts ->
        if (criticalParts.isNotEmpty()) {
            appendLine("🔴 RICAMBI CRITICI (Sostituire immediatamente):")
            criticalParts.forEach { part ->
                appendSparePartDetail(part)
            }
            appendLine()
        }
    }

    // Importanti
    partsByUrgency[SparePartUrgency.IMPORTANT]?.let { importantParts ->
        if (importantParts.isNotEmpty()) {
            appendLine("🟡 RICAMBI IMPORTANTI (Sostituire entro 30 giorni):")
            importantParts.forEach { part ->
                appendSparePartDetail(part)
            }
            appendLine()
        }
    }

    // Routine
    partsByUrgency[SparePartUrgency.ROUTINE]?.let { routineParts ->
        if (routineParts.isNotEmpty()) {
            appendLine("🟢 RICAMBI ROUTINE (Sostituire alla prossima manutenzione):")
            routineParts.forEach { part ->
                appendSparePartDetail(part)
            }
            appendLine()
        }
    }
}

/**
 * Dettaglio singola parte di ricambio
 */
private fun StringBuilder.appendSparePartDetail(part: SparePart) {
    appendLine("  [${part.partNumber}] ${part.description}")
    appendLine("    Quantità:     ${part.quantity}")
    if (part.estimatedCost != null) {
        appendLine("    Costo Stimato: €${String.format("%.2f", part.estimatedCost)}")
    }
    if (part.notes.isNotBlank()) {
        appendLine("    Note:         ${part.notes}")
    }
    appendLine()
}

/**
 * Conclusioni e raccomandazioni generali
 */
private fun StringBuilder.appendConclusions(exportData: ExportData) {
    appendLine("=".repeat(80))
    appendLine(centerText("CONCLUSIONI", 80))
    appendLine("=".repeat(80))
    appendLine()

    val stats = calculateCheckupStats(exportData.itemsByModule)
    val recommendations = generateGeneralRecommendations(exportData, stats)

    if (recommendations.immediateActions.isNotEmpty()) {
        appendLine("AZIONI IMMEDIATE RICHIESTE")
        appendLine("-".repeat(26))
        recommendations.immediateActions.forEach { action ->
            appendLine("- $action")
        }
        appendLine()
    }

    if (recommendations.generalRecommendations.isNotEmpty()) {
        appendLine("RACCOMANDAZIONI GENERALI")
        appendLine("-".repeat(24))
        recommendations.generalRecommendations.forEach { recommendation ->
            appendLine("- $recommendation")
        }
        appendLine()
    }

    // Prossimo checkup
    val nextCheckupDate = calculateNextCheckupDate(exportData.checkup, stats)
    appendLine("PROSSIMO CHECKUP CONSIGLIATO")
    appendLine("-".repeat(28))
    appendLine("Data Suggerita: ${nextCheckupDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
    appendLine("Motivazione:    ${getNextCheckupReason(stats)}")
    appendLine()

    // Signature tecnico
    appendLine("VALIDAZIONE TECNICA")
    appendLine("-".repeat(19))
    appendLine("Tecnico:      ${exportData.checkup.header.technicianInfo.name}")
    appendLine("Data Report:  ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")
    appendLine("Firma:        ____________________")
}

/**
 * Footer del report
 */
private fun StringBuilder.appendReportFooter(exportData: ExportData) {
    appendLine()
    appendLine("=".repeat(80))
    appendLine("Report generato automaticamente da QReport v1.0")
    appendLine("Data generazione: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))}")
    appendLine("Cliente: ${exportData.checkup.header.clientInfo.companyName}")
    appendLine("Tecnico responsabile: ${exportData.checkup.header.technicianInfo.name}")
    appendLine("=".repeat(80))
}

// === UTILITY FUNCTIONS ===

private fun generatePhotoFileName(
    sectionIndex: Int,
    item: CheckItem,
    photo: Photo,
    photoIndex: Int,
    namingStrategy: PhotoNamingStrategy
): String {
    // Implementazione semplificata - in produzione usare PhotoExportManager
    return when (namingStrategy) {
        PhotoNamingStrategy.STRUCTURED ->
            "${String.format("%02d", sectionIndex)}_${item.moduleType.name}_foto${photoIndex + 1}.jpg"
        PhotoNamingStrategy.SEQUENTIAL ->
            "foto_${String.format("%03d", photoIndex + 1)}.jpg"
        PhotoNamingStrategy.TIMESTAMP ->
            "${photo.takenAt.toString().replace(":", "")}_${photoIndex + 1}.jpg"
        else -> "foto_${photoIndex + 1}.jpg" // Default fallback
    }
}

/**
 * Versione semplificata per indice foto di modulo
 */
private fun generatePhotoFileName(
    moduleIndex: Int,
    photo: Photo,
    photoIndex: Int,
    namingStrategy: PhotoNamingStrategy
): String {
    return when (namingStrategy) {
        PhotoNamingStrategy.STRUCTURED ->
            "modulo${String.format("%02d", moduleIndex)}_foto${photoIndex + 1}.jpg"
        PhotoNamingStrategy.SEQUENTIAL ->
            "foto_${String.format("%03d", photoIndex + 1)}.jpg"
        PhotoNamingStrategy.TIMESTAMP ->
            "${photo.takenAt.toString().replace(":", "")}_${photoIndex + 1}.jpg"
        else -> "foto_${photoIndex + 1}.jpg" // Default fallback
    }
}

private fun String.times(n: Int): String = this.repeat(n)

/**
 * Centra un testo in una larghezza specificata
 */
private fun centerText(text: String, width: Int): String {
    val padding = (width - text.length) / 2
    return " ".repeat(padding) + text
}

/**
 * Formatta un testo con padding a destra
 */
private fun padRight(text: String, width: Int): String {
    return text.padEnd(width)
}

/**
 * Formatta un testo con padding a sinistra
 */
private fun padLeft(text: String, width: Int): String {
    return text.padStart(width)
}
}

// === EXTENSION FUNCTIONS per Statistics ===

private fun calculateCheckupStats(itemsByModule: Map<net.calvuz.qreport.domain.model.module.ModuleType, List<CheckItem>>): CheckupStatistics {
    val allItems = itemsByModule.values.flatten()
    val totalPhotos = allItems.sumOf { it.photos.size }

    return CheckupStatistics(
        totalSections = itemsByModule.size,
        totalItems = allItems.size,
        okItems = allItems.count { it.status == CheckItemStatus.OK },
        nokItems = allItems.count { it.status == CheckItemStatus.NOK },
        criticalItems = 0, // Non esiste CheckItemStatus.CRITICAL, usiamo criticality
        pendingItems = allItems.count { it.status == CheckItemStatus.PENDING },
        naItems = allItems.count { it.status == CheckItemStatus.NA },
        criticalIssues = allItems.count { it.criticality == CriticalityLevel.CRITICAL },
        importantIssues = allItems.count { it.criticality == CriticalityLevel.IMPORTANT },
        totalPhotos = totalPhotos,
        sectionsWithIssues = itemsByModule.values.count { moduleItems ->
            moduleItems.any { it.status == CheckItemStatus.NOK || it.criticality == CriticalityLevel.CRITICAL }
        }
    )
}

private fun calculateSectionStats(items: List<CheckItem>): SectionStatistics {
    return SectionStatistics(
        totalItems = items.size,
        okItems = items.count { it.status == CheckItemStatus.OK },
        nokItems = items.count { it.status == CheckItemStatus.NOK },
        criticalItems = items.count { it.criticality == CriticalityLevel.CRITICAL }, // Usiamo criticality invece di status
        pendingItems = items.count { it.status == CheckItemStatus.PENDING },
        naItems = items.count { it.status == CheckItemStatus.NA }
    )
}

// === DATA CLASSES per Statistics ===

private data class CheckupStatistics(
    val totalSections: Int,
    val totalItems: Int,
    val okItems: Int,
    val nokItems: Int,
    val criticalItems: Int,
    val pendingItems: Int,
    val naItems: Int,
    val criticalIssues: Int,
    val importantIssues: Int,
    val totalPhotos: Int,
    val sectionsWithIssues: Int
) {
    val okPercentage: Double = if (totalItems > 0) (okItems * 100.0) / totalItems else 0.0
    val nokPercentage: Double = if (totalItems > 0) (nokItems * 100.0) / totalItems else 0.0
    val naPercentage: Double = if (totalItems > 0) (naItems * 100.0) / totalItems else 0.0
}

private data class SectionStatistics(
    val totalItems: Int,
    val okItems: Int,
    val nokItems: Int,
    val criticalItems: Int,
    val pendingItems: Int,
    val naItems: Int
)

// Placeholder functions - da implementare in base alla logica business
private fun getOverallStatusText(stats: CheckupStatistics): String {
    return when {
        stats.criticalIssues > 0 -> "⚠️  CRITICO - Intervento immediato richiesto"
        stats.nokItems > stats.totalItems * 0.1 -> "⚡ ATTENZIONE - Problemi rilevati"
        stats.okPercentage >= 95.0 -> "✅ OTTIMO - Sistema in perfette condizioni"
        stats.okPercentage >= 85.0 -> "🟢 BUONO - Sistema funzionale"
        else -> "🟡 SUFFICIENTE - Monitoraggio richiesto"
    }
}

private fun generateItemRecommendations(item: CheckItem): String {
    // Logic based on item status and criticality
    return when {
        item.criticality == CriticalityLevel.CRITICAL -> "Intervento immediato necessario"
        item.status == CheckItemStatus.NOK && item.criticality == CriticalityLevel.CRITICAL -> "Sostituire entro 24h"
        item.status == CheckItemStatus.NOK && item.criticality == CriticalityLevel.IMPORTANT -> "Programmare sostituzione"
        item.status == CheckItemStatus.NOK -> "Monitorare nelle prossime verifiche"
        else -> ""
    }
}

private fun generateGeneralRecommendations(exportData: ExportData, stats: CheckupStatistics): Recommendations {
    val immediate = mutableListOf<String>()
    val general = mutableListOf<String>()

    if (stats.criticalIssues > 0) {
        immediate.add("Sostituire immediatamente ${stats.criticalIssues} componenti critici")
    }

    if (stats.nokPercentage > 10) {
        general.add("Programmare manutenzione straordinaria - ${stats.nokItems} controlli falliti")
    }

    if (stats.totalPhotos > 50) {
        general.add("Archiviare foto del checkup per storico manutenzioni")
    }

    return Recommendations(immediate, general)
}

private fun calculateNextCheckupDate(checkup: net.calvuz.qreport.domain.model.checkup.CheckUp, stats: CheckupStatistics): LocalDateTime {
    val baseDate = LocalDateTime.now()
    return when {
        stats.criticalIssues > 0 -> baseDate.plusWeeks(2) // 2 settimane se critici
        stats.nokPercentage > 15 -> baseDate.plusMonths(1) // 1 mese se molti problemi
        stats.okPercentage >= 95 -> baseDate.plusMonths(6) // 6 mesi se tutto ok
        else -> baseDate.plusMonths(3) // 3 mesi standard
    }
}

private fun getNextCheckupReason(stats: CheckupStatistics): String {
    return when {
        stats.criticalIssues > 0 -> "Verifica risoluzione criticità"
        stats.nokPercentage > 15 -> "Monitoraggio problemi rilevati"
        stats.okPercentage >= 95 -> "Manutenzione preventiva standard"
        else -> "Controllo periodico raccomandato"
    }
}

private data class Recommendations(
    val immediateActions: List<String>,
    val generalRecommendations: List<String>
)