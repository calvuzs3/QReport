# 📋 Specifiche Export Aggiornate - QReport

**Versione:** 1.1  
**Data:** Novembre 2025  
**Nuove Features:** Cartella FOTO + Formato testuale

---

## 🎯 NUOVI REQUISITI EXPORT

### 📁 Struttura Export Completa

```
📂 Export_Checkup_[TIMESTAMP]/
├── 📄 Checkup_RobotSaldatura_AcmeIndustries_20251022_1430.docx
├── 📄 Checkup_Summary_20251022_1430.txt
└── 📁 FOTO/
    ├── 📷 01_Sicurezza_Check001_vista-frontale.jpg
    ├── 📷 01_Sicurezza_Check002_dettaglio-sensori.jpg
    ├── 📷 02_Meccanica_Check005_usura-giunti.jpg
    ├── 📷 02_Meccanica_Check005_dettaglio-ruggine.jpg
    ├── 📷 03_Elettrica_Check012_quadro-principale.jpg
    └── 📷 [SEZIONE]_[DESCRIZIONE_ITEM]_[TIPO_FOTO].jpg
```

### 📸 Naming Convention Foto

**Pattern:** `[SEQ_SEZIONE]_[NOME_SEZIONE]_[ITEM_ID]_[DESCRIZIONE].jpg`

**Esempi:**
- `01_Sicurezza_Check001_vista-frontale.jpg`
- `01_Sicurezza_Check001_dettaglio-sensori.jpg`  
- `02_Meccanica_Check005_usura-giunti.jpg`
- `03_Elettrica_Check012_quadro-principale.jpg`

**Regole Naming:**
- Nomi sezioni normalizzati (no spazi, no caratteri speciali)
- Descrizioni derivate dal caption della foto
- Numerazione progressiva per sezione
- Lunghezza massima 80 caratteri

---

## 📄 FORMATO TESTUALE - Specifiche

### 🔤 Struttura Report Testuale

```
================================================================================
                          REPORT CHECKUP INDUSTRIALE
================================================================================

INFORMAZIONI GENERALI
---------------------
Cliente:              [NOME_CLIENTE]
Tipo Isola:           [TIPO_ISOLA]
Data Checkup:         [DATA_CHECKUP]
Tecnico Responsabile: [NOME_TECNICO]
Ora Inizio:           [ORA_INIZIO]
Ora Fine:             [ORA_FINE]
Durata Totale:        [DURATA_ORE]

RIEPILOGO ESECUTIVO
------------------
Stato Generale:       [OK/WARNING/CRITICAL]
Controlli Totali:     [NUMERO_CONTROLLI]
Controlli OK:         [NUMERO_OK] ([PERCENTUALE]%)
Controlli NOK:        [NUMERO_NOK] ([PERCENTUALE]%)
Criticità Rilevate:   [NUMERO_CRITICAL]
Foto Acquisite:       [NUMERO_FOTO]

================================================================================
                              DETTAGLIO CONTROLLI
================================================================================

SEZIONE 1: [NOME_SEZIONE]
-------------------------
Controlli Totali: [NUM]  |  OK: [NUM]  |  NOK: [NUM]  |  Critici: [NUM]

[ITEM_001] [DESCRIZIONE_CONTROLLO]
    Stato:      [✓ OK / ✗ NOK / ⚠ CRITICO / ⏳ PENDING / ➖ N/A]
    Criticità:  [🔴 Critico / 🟡 Importante / 🟢 Routine]
    Note:       [EVENTUALI_NOTE]
    Foto:       [NUMERO_FOTO] foto acquisite
                - 01_Sicurezza_Check001_vista-frontale.jpg
                - 01_Sicurezza_Check001_dettaglio-sensori.jpg

[ITEM_002] [DESCRIZIONE_CONTROLLO]
    Stato:      [STATO]
    Criticità:  [CRITICITA]
    Note:       [NOTE]
    Foto:       Nessuna foto

...

SEZIONE 2: [NOME_SEZIONE]
-------------------------
[RIPETERE_PATTERN_SEZIONE]

================================================================================
                              PARTI DI RICAMBIO
================================================================================

RICAMBI CONSIGLIATI
------------------
[PART_001] [DESCRIZIONE_PARTE]
    Quantità:     [QTA]
    Urgenza:      [CRITICA/IMPORTANTE/ROUTINE]
    Note:         [NOTE_RICAMBIO]

[PART_002] [DESCRIZIONE_PARTE]
    Quantità:     [QTA]
    Urgenza:      [URGENZA]
    Note:         [NOTE_RICAMBIO]

================================================================================
                                CONCLUSIONI
================================================================================

AZIONI IMMEDIATE RICHIESTE
--------------------------
- [AZIONE_1]
- [AZIONE_2]
- [AZIONE_3]

RACCOMANDAZIONI GENERALI
------------------------
- [RACCOMANDAZIONE_1]
- [RACCOMANDAZIONE_2]

PROSSIMO CHECKUP CONSIGLIATO
----------------------------
Data Suggerita: [DATA_PROSSIMO_CHECKUP]
Motivazione:    [MOTIVAZIONE_TIMING]

================================================================================
Report generato automaticamente da QReport v1.0
Data generazione: [TIMESTAMP_GENERAZIONE]
Tecnico responsabile: [NOME_TECNICO]
================================================================================
```

---

## 🛠️ IMPLEMENTAZIONE TECNICA

### 🏗️ Aggiornamenti ExportOptions

```kotlin
data class ExportOptions(
    // Esistenti
    val includePhotos: Boolean = true,
    val includeNotes: Boolean = true,
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val photoMaxWidth: Int = 800,
    val customTemplate: ReportTemplate? = null,
    
    // NUOVI
    val createPhotoFolder: Boolean = true,
    val createTextSummary: Boolean = true,
    val exportFormat: Set<ExportFormat> = setOf(
        ExportFormat.WORD,
        ExportFormat.TEXT,
        ExportFormat.PHOTO_FOLDER
    ),
    val photoNamingStrategy: PhotoNamingStrategy = PhotoNamingStrategy.STRUCTURED
) {
    companion object {
        fun wordOnly() = ExportOptions(
            createPhotoFolder = false,
            createTextSummary = false,
            exportFormat = setOf(ExportFormat.WORD)
        )
        
        fun complete() = ExportOptions() // Default include tutto
    }
}

enum class ExportFormat {
    WORD,           // .docx con foto integrate
    TEXT,           // .txt riassunto testuale
    PHOTO_FOLDER    // cartella FOTO/ con foto originali
}

enum class PhotoNamingStrategy {
    STRUCTURED,     // 01_Sezione_Check001_descrizione.jpg
    SEQUENTIAL,     // foto_001.jpg, foto_002.jpg
    TIMESTAMP       // 20251022_143052_001.jpg
}
```

### 📂 Aggiornamenti ExportRepository

```kotlin
interface ExportRepository {
    // Esistente
    suspend fun generateWordReport(
        exportData: ExportData, 
        options: ExportOptions
    ): ExportResult
    
    // NUOVI
    suspend fun generateTextSummary(
        exportData: ExportData,
        options: ExportOptions
    ): ExportResult
    
    suspend fun exportPhotoFolder(
        exportData: ExportData,
        targetDirectory: File,
        namingStrategy: PhotoNamingStrategy
    ): ExportResult
    
    suspend fun generateCompleteExport(
        exportData: ExportData,
        options: ExportOptions
    ): MultiFormatExportResult
}

// Nuovo risultato per export multi-formato
data class MultiFormatExportResult(
    val wordFile: ExportResult.Success?,
    val textFile: ExportResult.Success?,
    val photoFolder: ExportResult.Success?,
    val exportDirectory: String,
    val totalFiles: Int,
    val totalSize: Long
)
```

### 🖼️ PhotoExportManager - Nuovo Componente

```kotlin
@Singleton
class PhotoExportManager @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val imageProcessor: ImageProcessor
) {
    
    suspend fun exportPhotosToFolder(
        exportData: ExportData,
        targetDirectory: File,
        namingStrategy: PhotoNamingStrategy
    ): PhotoExportResult {
        
        val exportedPhotos = mutableListOf<ExportedPhoto>()
        var totalSize = 0L
        
        exportData.sections.forEachIndexed { sectionIndex, section ->
            val sectionName = normalizeSectionName(section.title)
            val sectionPrefix = String.format("%02d", sectionIndex + 1)
            
            section.items.forEach { item ->
                item.photos.forEachIndexed { photoIndex, photo ->
                    val exportedPhoto = exportSinglePhoto(
                        photo = photo,
                        sectionPrefix = sectionPrefix,
                        sectionName = sectionName,
                        item = item,
                        photoIndex = photoIndex,
                        targetDirectory = targetDirectory,
                        namingStrategy = namingStrategy
                    )
                    
                    exportedPhotos.add(exportedPhoto)
                    totalSize += exportedPhoto.fileSize
                }
            }
        }
        
        return PhotoExportResult.Success(
            exportedPhotos = exportedPhotos,
            totalFiles = exportedPhotos.size,
            totalSize = totalSize,
            exportDirectory = targetDirectory.absolutePath
        )
    }
    
    private suspend fun exportSinglePhoto(
        photo: Photo,
        sectionPrefix: String,
        sectionName: String,
        item: CheckItem,
        photoIndex: Int,
        targetDirectory: File,
        namingStrategy: PhotoNamingStrategy
    ): ExportedPhoto {
        
        val fileName = when (namingStrategy) {
            PhotoNamingStrategy.STRUCTURED -> generateStructuredName(
                sectionPrefix, sectionName, item, photo, photoIndex
            )
            PhotoNamingStrategy.SEQUENTIAL -> "foto_${String.format("%03d", photoIndex + 1)}.jpg"
            PhotoNamingStrategy.TIMESTAMP -> "${photo.takenAt.toEpochMilliseconds()}_${photoIndex + 1}.jpg"
        }
        
        val targetFile = File(targetDirectory, fileName)
        
        // Copia file originale (alta qualità)
        val sourceFile = File(photo.filePath)
        sourceFile.copyTo(targetFile, overwrite = true)
        
        return ExportedPhoto(
            originalPhoto = photo,
            exportedFileName = fileName,
            exportedPath = targetFile.absolutePath,
            fileSize = targetFile.length()
        )
    }
    
    private fun generateStructuredName(
        sectionPrefix: String,
        sectionName: String,
        item: CheckItem,
        photo: Photo,
        photoIndex: Int
    ): String {
        val itemDesc = normalizeDescription(item.title)
        val photoDesc = normalizeDescription(photo.caption.ifBlank { "foto${photoIndex + 1}" })
        
        return "${sectionPrefix}_${sectionName}_${itemDesc}_${photoDesc}.jpg"
            .take(80) // Max 80 caratteri
    }
    
    private fun normalizeSectionName(name: String): String {
        return name
            .replace(" ", "-")
            .replace(Regex("[^a-zA-Z0-9\\-]"), "")
            .lowercase()
            .take(20)
    }
    
    private fun normalizeDescription(desc: String): String {
        return desc
            .replace(" ", "-")
            .replace(Regex("[^a-zA-Z0-9\\-]"), "")
            .lowercase()
            .take(30)
    }
}

data class ExportedPhoto(
    val originalPhoto: Photo,
    val exportedFileName: String,
    val exportedPath: String,
    val fileSize: Long
)

sealed class PhotoExportResult {
    data class Success(
        val exportedPhotos: List<ExportedPhoto>,
        val totalFiles: Int,
        val totalSize: Long,
        val exportDirectory: String
    ) : PhotoExportResult()
    
    data class Error(
        val exception: Throwable,
        val errorCode: ExportErrorCode
    ) : PhotoExportResult()
}
```

### 📝 TextReportGenerator - Nuovo Componente

```kotlin
@Singleton
class TextReportGenerator @Inject constructor() {
    
    suspend fun generateTextReport(
        exportData: ExportData,
        options: ExportOptions
    ): String {
        
        return buildString {
            // Header
            appendLine("=" * 80)
            appendLine(centerText("REPORT CHECKUP INDUSTRIALE", 80))
            appendLine("=" * 80)
            appendLine()
            
            // Informazioni generali
            appendGeneralInfo(exportData)
            appendLine()
            
            // Riepilogo esecutivo
            appendExecutiveSummary(exportData)
            appendLine()
            
            // Dettaglio controlli
            appendLine("=" * 80)
            appendLine(centerText("DETTAGLIO CONTROLLI", 80))
            appendLine("=" * 80)
            appendLine()
            
            exportData.sections.forEachIndexed { index, section ->
                appendSection(section, index + 1, options)
                appendLine()
            }
            
            // Parti di ricambio
            if (exportData.spareParts.isNotEmpty()) {
                appendSpareParts(exportData.spareParts)
                appendLine()
            }
            
            // Conclusioni
            appendConclusions(exportData)
            
            // Footer
            appendLine("=" * 80)
            appendLine("Report generato automaticamente da QReport v1.0")
            appendLine("Data generazione: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))}")
            appendLine("Tecnico responsabile: ${exportData.metadata.technicianName}")
            appendLine("=" * 80)
        }
    }
    
    private fun StringBuilder.appendGeneralInfo(exportData: ExportData) {
        appendLine("INFORMAZIONI GENERALI")
        appendLine("-" * 21)
        appendLine("Cliente:              ${exportData.metadata.clientInfo.companyName}")
        appendLine("Tipo Isola:           ${exportData.checkup.islandType.displayName}")
        appendLine("Data Checkup:         ${exportData.checkup.scheduledDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
        appendLine("Tecnico Responsabile: ${exportData.metadata.technicianName}")
        appendLine("Ora Inizio:           ${exportData.checkup.startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "N/A"}")
        appendLine("Ora Fine:             ${exportData.checkup.completedAt?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "In corso"}")
        
        if (exportData.checkup.startTime != null && exportData.checkup.completedAt != null) {
            val duration = Duration.between(exportData.checkup.startTime, exportData.checkup.completedAt)
            appendLine("Durata Totale:        ${duration.toHours()}h ${duration.toMinutesPart()}m")
        }
    }
    
    private fun StringBuilder.appendExecutiveSummary(exportData: ExportData) {
        val stats = calculateCheckupStats(exportData.sections)
        
        appendLine("RIEPILOGO ESECUTIVO")
        appendLine("-" * 18)
        appendLine("Stato Generale:       ${getOverallStatusText(stats)}")
        appendLine("Controlli Totali:     ${stats.totalItems}")
        appendLine("Controlli OK:         ${stats.okItems} (${String.format("%.1f", stats.okPercentage)}%)")
        appendLine("Controlli NOK:        ${stats.nokItems} (${String.format("%.1f", stats.nokPercentage)}%)")
        appendLine("Criticità Rilevate:   ${stats.criticalIssues}")
        appendLine("Foto Acquisite:       ${stats.totalPhotos}")
    }
    
    private fun StringBuilder.appendSection(
        section: CheckupSection,
        sectionIndex: Int,
        options: ExportOptions
    ) {
        val sectionStats = calculateSectionStats(section.items)
        
        appendLine("SEZIONE $sectionIndex: ${section.title.uppercase()}")
        appendLine("-" * (section.title.length + 12))
        appendLine("Controlli Totali: ${sectionStats.totalItems}  |  " +
                  "OK: ${sectionStats.okItems}  |  " +
                  "NOK: ${sectionStats.nokItems}  |  " +
                  "Critici: ${sectionStats.criticalItems}")
        appendLine()
        
        section.items.forEach { item ->
            appendCheckItem(item, sectionIndex, options)
            appendLine()
        }
    }
    
    private fun StringBuilder.appendCheckItem(
        item: CheckItem,
        sectionIndex: Int,
        options: ExportOptions
    ) {
        appendLine("[${item.id}] ${item.title}")
        appendLine("    Stato:      ${formatStatusForText(item.status)}")
        appendLine("    Criticità:  ${formatCriticalityForText(item.criticality)}")
        
        if (item.note.isNotBlank()) {
            appendLine("    Note:       ${item.note}")
        }
        
        if (options.createPhotoFolder && item.photos.isNotEmpty()) {
            appendLine("    Foto:       ${item.photos.size} foto acquisite")
            item.photos.forEachIndexed { index, photo ->
                val fileName = generatePhotoFileName(sectionIndex, item, photo, index)
                appendLine("                - $fileName")
            }
        } else if (item.photos.isEmpty()) {
            appendLine("    Foto:       Nessuna foto")
        }
    }
    
    private fun formatStatusForText(status: CheckItemStatus): String {
        return when (status) {
            CheckItemStatus.OK -> "✓ OK"
            CheckItemStatus.NOK -> "✗ NOK"
            CheckItemStatus.CRITICAL -> "⚠ CRITICO"
            CheckItemStatus.PENDING -> "⏳ PENDING"
            CheckItemStatus.NA -> "➖ N/A"
        }
    }
    
    private fun formatCriticalityForText(criticality: CheckItemCriticality): String {
        return when (criticality) {
            CheckItemCriticality.CRITICAL -> "🔴 Critico"
            CheckItemCriticality.IMPORTANT -> "🟡 Importante"
            CheckItemCriticality.ROUTINE -> "🟢 Routine"
            CheckItemCriticality.NA -> "➖ N/A"
        }
    }
    
    private fun centerText(text: String, width: Int): String {
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text
    }
    
    private fun String.times(n: Int): String = this.repeat(n)
}
```

---

## 🎯 SUMMARY DELLE MODIFICHE

### ✅ Nuove Funzionalità:

1. **📁 Cartella FOTO Separata**
   - Export foto originali in cartella dedicata
   - Naming strutturato per facile identificazione
   - Qualità originale mantenuta

2. **📄 Formato Testuale**
   - Report ASCII completo e leggibile
   - Tutte le informazioni del checkup
   - Riferimenti alle foto nella cartella

3. **🔧 Export Multi-Formato**
   - Un'unica operazione genera tutti i formati
   - Configurabile tramite ExportOptions
   - Directory organizzata per tipo export

### 🚀 Benefici:

- **Flessibilità**: Cliente può scegliere formato preferito
- **Archiviazione**: Foto separate per gestione documenti
- **Compatibilità**: Testo ASCII universalmente leggibile
- **Organizzazione**: Struttura directory logica e navigabile

**Ready per implementazione!** 📋✨