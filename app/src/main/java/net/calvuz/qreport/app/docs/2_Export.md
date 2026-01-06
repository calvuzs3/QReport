# 📄 QReport - Sistema Export Multi-Formato

**Versione:** 2.0 - DOCUMENTAZIONE AGGIORNATA  
**Data:** Novembre 2025  
**Tecnologie:** Apache POI + Android + Clean Architecture  
**Target:** Export professionale Word, Text e Photo per clienti industriali

---

## 📋 INDICE

1. [Panoramica Sistema](#1-panoramica-sistema)
2. [Architettura Export](#2-architettura-export)
3. [Formati Supportati](#3-formati-supportati)
4. [Coordinamento Multi-Formato](#4-coordinamento-multi-formato)
5. [Generatore Word (Apache POI)](#5-generatore-word-apache-poi)
6. [Generatore Report Text](#6-generatore-report-text)
7. [Photo Export Manager](#7-photo-export-manager)
8. [Configurazione e Opzioni](#8-configurazione-e-opzioni)
9. [Gestione Errori](#9-gestione-errori)
10. [Performance e Ottimizzazioni](#10-performance-e-ottimizzazioni)
11. [UI e User Experience](#11-ui-e-user-experience)

---

## 1. PANORAMICA SISTEMA

### 1.1 Obiettivi e Caratteristiche

**🎯 Sistema Export Professionale:**
- **Multi-formato coordinato**: Word, Text, Photo Folder, Combined Package
- **Clean Architecture**: Repository pattern, Use Cases, Generators specializzati
- **Configurazione granulare**: ExportOptions con validazione e preset
- **Error handling robusto**: Codici errore specifici e recovery automatico
- **Performance ottimizzate**: Stime preventive, cleanup automatico

**📊 Struttura Output Tipica:**
```
Export_Checkup_AcmeIndustries_20251112_1430/
├── Checkup_POLY_MOVE_AcmeIndustries_20251112.docx    ← Report Word professionale
├── Report_Testuale_AcmeIndustries_20251112.txt       ← Report ASCII completo
├── FOTO/                                             ← Cartella foto organizzata
│   ├── 01_SAFETY_Check001_01.jpg                    ← Foto strutturate
│   ├── 02_MECHANICAL_Check003_01.jpg
│   ├── INDICE_FOTO.txt                              ← Mapping foto-items
│   └── ...
└── Export_Report.json                               ← Metadati export (opzionale)
```

### 1.2 Modelli Domain Utilizzati

Il sistema usa **ESCLUSIVAMENTE** i modelli domain esistenti di QReport:

```kotlin
// CORE DATA MODEL per Export
data class ExportData(
    val checkup: CheckUp,                    // ✅ Modello CheckUp esistente
    val itemsByModule: Map<ModuleType, List<CheckItem>>,  // ✅ Raggruppamento reale
    val statistics: CheckUpStatistics,       // ✅ Statistiche esistenti
    val progress: CheckUpProgress,           // ✅ Progresso esistente  
    val exportMetadata: ExportTechnicalMetadata
)
```

**✅ Vantaggi dell'approccio:**
- **Nessuna duplicazione** di modelli domain
- **Consistency** totale con il resto dell'applicazione
- **Raggruppamento per ModuleType** invece di "sections" artificiali
- **Riutilizzo** di CheckUpStatistics e CheckUpProgress esistenti

---

## 2. ARCHITETTURA EXPORT

### 2.1 Clean Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
├─────────────────────────────────────────────────────────────┤
│ ExportOptionsScreen   ExportProgressDialog   ExportResultDialog │
│ ExportOptionsViewModel                                       │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER                           │
├─────────────────────────────────────────────────────────────┤
│ ExportCheckUpUseCase  GetCheckUpDetailsUseCase             │
│ ExportRepository (interface)                               │
│ ExportData, ExportOptions, ExportResult, MultiFormatExportResult │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                            │
├─────────────────────────────────────────────────────────────┤
│ ExportRepositoryImpl (coordinatore)                        │
│   ├── WordReportGenerator                                  │
│   ├── TextReportGenerator                                  │
│   └── PhotoExportManager                                   │
│                                                             │
│ ImageProcessorImpl, TextFormatter, DateTimeUtils           │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Componenti Principali

**🎯 ExportRepositoryImpl** - Coordinatore principale:
```kotlin
@Singleton
class ExportRepositoryImpl @Inject constructor(
    private val wordReportGenerator: WordReportGenerator,
    private val textReportGenerator: TextReportGenerator,
    private val photoExportManager: PhotoExportManager
) : ExportRepository {

    suspend fun generateCompleteExport(
        exportData: ExportData,
        options: ExportOptions
    ): Flow<MultiFormatExportResult>
}
```

**Responsabilità:**
- **Validazione dati** prima dell'export
- **Coordinamento multi-formato** con gestione errori granulare
- **Directory management** con timestamp e cleanup
- **Stime performance** e validazione spazio disco
- **Progress tracking** per UI reattiva

---

## 3. FORMATI SUPPORTATI

### 3.1 Enum ExportFormat

```kotlin
enum class ExportFormat {
    WORD,              // Documento .docx con foto embedded
    TEXT,              // Report ASCII universalmente leggibile  
    PHOTO_FOLDER,      // Cartella foto organizzata con indici
    COMBINED_PACKAGE   // Tutti i formati in directory strutturata
}
```

### 3.2 Caratteristiche per Formato

**📄 WORD (.docx)**
- **Apache POI** per generazione professionale
- **Foto embedded** direttamente nel documento (300x200px)
- **Styling corporate** con colori e formattazione brandizzata
- **Tabelle strutturate** per check items per modulo
- **Executive summary** con statistiche e raccomandazioni
- **Header/footer** con info cliente e firma digitale

**📋 TEXT (.txt)**
- **ASCII formatting** con tabelle, box e progress bar
- **Universalmente leggibile** su qualsiasi sistema
- **Riferimenti alle foto** nella cartella separata
- **Statistiche dettagliate** e raccomandazioni automatiche
- **Indentazione strutturata** per facilità di lettura

**📸 PHOTO_FOLDER**
- **Foto organizzate** con naming intelligente configurabile
- **Qualità configurabile**: Original, Optimized, Compressed
- **Watermark opzionale** con text personalizzabile
- **File indice** con mapping foto → check items
- **Metadati EXIF** preservati o rimossi secondo configurazione

**📦 COMBINED_PACKAGE**
- **Tutti i formati insieme** in directory temporale
- **Struttura organizzata** per massima usabilità
- **Metadati export** con statistiche complete

---

## 4. COORDINAMENTO MULTI-FORMATO

### 4.1 ExportRepositoryImpl - Logica Principale

```kotlin
suspend fun generateCompleteExport(
    exportData: ExportData,
    options: ExportOptions
): Flow<MultiFormatExportResult> = flow {

    // 1. Validazione dati
    val validationErrors = validateExportData(exportData)
    if (validationErrors.isNotEmpty()) {
        throw IllegalArgumentException("Dati export non validi: ${validationErrors.joinToString()}")
    }

    // 2. Preparazione directory con timestamp
    val exportDirectory = if (options.createTimestampedDirectory) {
        createTimestampedExportDirectory(getDefaultExportDirectory(), exportData)
    } else {
        getDefaultExportDirectory()
    }

    // 3. Export per ogni formato richiesto
    options.exportFormats.forEach { format ->
        when (format) {
            ExportFormat.WORD -> {
                val wordResult = generateWordReportInternal(exportData, options, exportDirectory)
                result = result.copy(wordResult = wordResult as? Success)
            }
            ExportFormat.TEXT -> {
                val textResult = generateTextReportInternal(exportData, options, exportDirectory)
                result = result.copy(textResult = textResult as? Success)
            }
            ExportFormat.PHOTO_FOLDER -> {
                val photoResult = generatePhotoFolderInternal(exportData, options, exportDirectory)
                result = result.copy(photoFolderResult = convertToExportSuccess(photoResult))
            }
            ExportFormat.COMBINED_PACKAGE -> {
                val combinedResult = generateCombinedPackage(exportData, options, exportDirectory)
                result = result.copy(
                    wordResult = combinedResult.wordResult,
                    textResult = combinedResult.textResult,
                    photoFolderResult = combinedResult.photoFolderResult
                )
            }
        }
        
        // Emetti progresso intermedio per UI reattiva
        emit(result.copy(statistics = calculateStatistics(exportData, result)))
    }
}
```

### 4.2 Directory Management

**🗂️ Creazione Directory Temporali:**
```kotlin
override suspend fun createTimestampedExportDirectory(
    baseDirectory: File,
    checkupData: ExportData
): File {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
    val clientName = checkupData.checkup.header.clientInfo.companyName
        .replace(" ", "")
        .replace("[^a-zA-Z0-9]".toRegex(), "")
        .take(15)

    val dirName = "Export_Checkup_${clientName}_$timestamp"
    val exportDir = File(baseDirectory, dirName)
    
    if (!exportDir.exists()) {
        exportDir.mkdirs()
    }
    
    return exportDir
}
```

**🧹 Cleanup Automatico:**
```kotlin
override suspend fun cleanupOldExports(olderThanDays: Int): Int {
    var deletedCount = 0
    val exportDir = getDefaultExportDirectory()
    val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)

    exportDir.listFiles()?.forEach { file ->
        if (file.isDirectory && file.lastModified() < cutoffTime) {
            file.deleteRecursively()
            deletedCount++
        }
    }
    
    return deletedCount
}
```

---

## 5. GENERATORE WORD (APACHE POI)

### 5.1 WordReportGenerator - Overview

```kotlin
@Singleton
class WordReportGenerator @Inject constructor(
    private val photoExportManager: PhotoExportManager,
    private val fileManager: FileManager
) {

    suspend fun generateWordReport(
        exportData: ExportData,
        exportOptions: ExportOptions = ExportOptions.complete()
    ): ExportResult
}
```

### 5.2 Struttura Documento Word Generato

**📋 Template Structure:**
1. **Document Header** - Logo aziendale + info cliente centrati
2. **Info Table** - Tabella 6x2 con dati checkup formattati
3. **Executive Summary** - Statistiche con colori status-coded
4. **Module Details** - Una sezione per ogni ModuleType con:
   - Titolo modulo con styling corporate
   - Tabella check items (4 colonne): Descrizione, Stato, Criticità, Note
   - Foto embedded (max 4 per modulo)
   - Caption foto quando presente
5. **Spare Parts Section** - Tabella 5 colonne se parti presenti
6. **Digital Footer** - Firma digitale e metadati generazione

### 5.3 Styling e Formattazione

**🎨 Corporate Styling:**
```kotlin
private fun createSectionTitle(document: XWPFDocument, title: String) {
    val titleParagraph = document.createParagraph().apply {
        spacingBefore = 600
        spacingAfter = 200
    }

    titleParagraph.createRun().apply {
        setText(title)
        isBold = true
        fontSize = 16
        color = "1F4E79"  // Corporate blue
    }
}
```

**📊 Tabelle con Colori Status:**
```kotlin
row.getCell(1).apply {
    text = item.status.displayName
    val statusColor = when (item.status) {
        CheckItemStatus.OK -> "00B050"      // Verde
        CheckItemStatus.NOK -> "FF0000"     // Rosso
        CheckItemStatus.PENDING -> "FFC000" // Giallo
        else -> "000000"
    }
    paragraphs[0].runs[0].color = statusColor
}
```

### 5.4 Photo Integration

**📸 Foto Embedded con Dimensioni Ottimizzate:**
```kotlin
private suspend fun insertModulePhotos(
    document: XWPFDocument,
    modulePhotos: List<ExportedPhoto>
) {
    modulePhotos.take(4).forEach { exportedPhoto -> // Max 4 foto per modulo
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
                    Units.toEMU(300.0), // Larghezza 300px
                    Units.toEMU(200.0)  // Altezza 200px
                )
            }
        }
    }
}
```

### 5.5 Filename Generation

**📝 Naming Convention:**
```kotlin
private fun generateWordFileName(exportData: ExportData): String {
    val timestamp = exportData.checkup.completedAt?.toFilenameSafeDate()
    val clientName = exportData.checkup.header.clientInfo.companyName
        .replace(" ", "")
        .replace(Regex("[^a-zA-Z0-9]"), "")
        .take(15)

    return "Checkup_${exportData.checkup.islandType.name}_${clientName}_${timestamp}.docx"
}
```

---

## 6. GENERATORE REPORT TEXT

### 6.1 TextReportGenerator - ASCII Professional

```kotlin
@Singleton
class TextReportGenerator @Inject constructor() {

    suspend fun generateTextReport(
        exportData: ExportData,
        options: ExportOptions
    ): String = withContext(Dispatchers.IO) {
        
        buildString {
            appendReportHeader()
            appendGeneralInfo(exportData)
            appendExecutiveSummary(exportData)
            appendSectionsDetail(exportData, options)
            
            if (exportData.checkup.spareParts.isNotEmpty()) {
                appendSpareParts(exportData.checkup.spareParts)
            }
            
            appendConclusions(exportData)
            appendReportFooter(exportData)
        }
    }
}
```

### 6.2 Formattazione ASCII Avanzata

**📋 Header Professionale:**
```
================================================================================
                           REPORT CHECKUP INDUSTRIALE
================================================================================

INFORMAZIONI GENERALI
---------------------
Cliente:              ACME Industries S.r.l.
Contatto:             Mario Rossi
Tipo Isola:           POLY MOVE - Sistema Robotizzato Saldatura
Serial Isola:         PM-2024-001
Modello Isola:        POLY MOVE v3.2
Ore Funzionamento:    15420h
Data Checkup:         2025-11-12T14:30:00Z
Tecnico Responsabile: Luca Calvetti
Azienda Tecnico:      CALVUZ S.r.l.
```

**📊 Executive Summary con Emoji:**
```
RIEPILOGO ESECUTIVO
------------------
Stato Generale:       ✅ OTTIMO - Sistema in perfette condizioni
Controlli Totali:     45
Controlli OK:         43 (95.6%)
Controlli NOK:        2 (4.4%)
Controlli N/A:        0 (0.0%)
Controlli Pending:    0
Criticità Rilevate:   0
Avvisi Importanti:    1
Foto Acquisite:       28

✅ Checkup completato con successo - Sistema in ottime condizioni.
```

### 6.3 Dettagli Moduli Strutturati

**🔧 Sezione Modulo con Tabelle ASCII:**
```
================================================================================
                               DETTAGLIO CONTROLLI
================================================================================

MODULO 1: SAFETY SYSTEMS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Stato Modulo: ✅ OK (8/8 controlli)  |  Criticità: Nessuna  |  Foto: 6

┌─────────────────────────────┬────────┬──────────────┬─────────────────────────┐
│ Descrizione                 │ Stato  │ Criticità    │ Note                    │
├─────────────────────────────┼────────┼──────────────┼─────────────────────────┤
│ Emergency Stop Buttons      │   ✅   │ NORMAL       │ Tutti funzionanti       │
│ Safety Light Curtains       │   ✅   │ NORMAL       │ Calibrazione OK         │
│ Protective Guards           │   ✅   │ NORMAL       │ Nessun danno rilevato   │
│ Warning Signals             │   ⚠️   │ IMPORTANT    │ Sostituire lampada 3    │
└─────────────────────────────┴────────┴──────────────┴─────────────────────────┘

📸 FOTO MODULO (Cartella FOTO/):
  → 01_SAFETY_Check001_01.jpg (Emergency stop button sx)
  → 01_SAFETY_Check002_01.jpg (Light curtain calibration)
  → 01_SAFETY_Check004_01.jpg (Warning lamp malfunction)
```

### 6.4 Raccomandazioni Automatiche

**💡 Sistema di Raccomandazioni Intelligenti:**
```kotlin
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
```

---

## 7. PHOTO EXPORT MANAGER

### 7.1 PhotoExportManager - Gestione Avanzata

```kotlin
@Singleton
class PhotoExportManager @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val imageProcessor: ImageProcessor
) {

    suspend fun exportPhotosToFolder(
        exportData: ExportData,
        targetDirectory: File,
        namingStrategy: PhotoNamingStrategy,
        quality: PhotoQuality = PhotoQuality.ORIGINAL,
        preserveExifData: Boolean = true,
        addWatermark: Boolean = false,
        watermarkText: String = "QReport",
        generateIndex: Boolean = true
    ): PhotoExportResult
}
```

### 7.2 Strategie di Naming

**📝 PhotoNamingStrategy:**
```kotlin
enum class PhotoNamingStrategy {
    STRUCTURED,    // 01_SAFETY_Check001_01.jpg (migliore organizzazione)
    SEQUENTIAL,    // foto_001.jpg (semplice e lineare)  
    TIMESTAMP      // 20251112_143052_001.jpg (ordinamento cronologico)
}
```

**🏗️ Structured Naming Logic:**
```kotlin
PhotoNamingStrategy.STRUCTURED ->
    "${modulePrefix}_${moduleInfo.normalizedName}_Check${itemNumber}_${photoNumber}.${extension}"

// Esempio output:
// 01_SAFETY_Check001_01.jpg
// 01_SAFETY_Check001_02.jpg
// 02_MECHANICAL_Check003_01.jpg
```

### 7.3 Qualità e Processamento

**🎛️ PhotoQuality Options:**
```kotlin
enum class PhotoQuality {
    ORIGINAL,      // Foto originali senza modifiche (massima qualità)
    OPTIMIZED,     // Ottimizzate per dimensioni ragionevoli  
    COMPRESSED     // Compresse per minimizzare spazio
}
```

**⚙️ Image Processing Pipeline:**
```kotlin
private suspend fun exportSinglePhoto(
    photoContext: PhotoContext,
    globalIndex: Int,
    targetDirectory: File,
    namingStrategy: PhotoNamingStrategy,
    quality: PhotoQuality,
    preserveExifData: Boolean,
    addWatermark: Boolean,
    watermarkText: String
): ExportedPhoto {

    val fileName = generateFileName(photoContext, globalIndex, namingStrategy)
    val targetFile = File(targetDirectory, fileName)
    val sourceFile = File(photoContext.photo.filePath)

    val processedSize = when (quality) {
        PhotoQuality.ORIGINAL -> {
            copyFilePreservingAttributes(sourceFile, targetFile, preserveExifData)
            targetFile.length()
        }
        PhotoQuality.OPTIMIZED -> {
            processPhotoOptimized(sourceFile, targetFile, preserveExifData, addWatermark, watermarkText)
        }
        PhotoQuality.COMPRESSED -> {
            processPhotoCompressed(sourceFile, targetFile, preserveExifData, addWatermark, watermarkText)
        }
    }

    return ExportedPhoto(/* ... */)
}
```

### 7.4 Indice Foto Automatico

**📚 File INDICE_FOTO.txt Generato:**
```
# INDICE FOTO - POLY MOVE Sistema Robotizzato
# Generato: 12/11/2025 14:30
# Cliente: ACME Industries S.r.l.
# Tecnico: Luca Calvetti
#
# Formato: [NOME_FILE] -> [MODULO] -> [CHECK_ITEM] -> [STATO]
================================================================================

MODULO 1: Safety Systems
--------------------------------------------------

  Check Item: Emergency Stop Buttons
  Stato: OK | Criticità: NORMAL
  Foto:
    - 01_SAFETY_Check001_01.jpg
      Caption: Emergency stop button lato sinistro
      Dimensione: 2.3MB

    - 01_SAFETY_Check001_02.jpg  
      Caption: Emergency stop button lato destro
      Dimensione: 2.1MB

  Check Item: Safety Light Curtains
  Stato: OK | Criticità: NORMAL
  Foto:
    - 01_SAFETY_Check002_01.jpg
      Caption: Light curtain calibration display
      Dimensione: 1.8MB

================================================================================
RIEPILOGO:
Foto totali: 28
Moduli: 6
Check items con foto: 15
Dimensione totale: 52.4MB
```

---

## 8. CONFIGURAZIONE E OPZIONI

### 8.1 ExportOptions - Configurazione Granulare

```kotlin
data class ExportOptions(
    // FORMATI EXPORT
    val exportFormats: Set<ExportFormat> = setOf(
        ExportFormat.WORD,
        ExportFormat.TEXT,
        ExportFormat.PHOTO_FOLDER
    ),

    // OPZIONI FOTO
    val includePhotos: Boolean = true,
    val includeNotes: Boolean = true,
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val photoMaxWidth: Int = 800,

    // TEMPLATE & STYLING
    val customTemplate: ReportTemplate? = null,

    // PHOTO FOLDER SPECIFICHE
    val photoNamingStrategy: PhotoNamingStrategy = PhotoNamingStrategy.STRUCTURED,
    val createTimestampedDirectory: Boolean = true,
    val photoFolderQuality: PhotoQuality = PhotoQuality.ORIGINAL,
    val preserveExifData: Boolean = true,
    val addWatermark: Boolean = false,
    val watermarkText: String = "QReport",
    val generatePhotoIndex: Boolean = true
) {

    companion object {
        fun wordOnly() = ExportOptions(
            exportFormats = setOf(ExportFormat.WORD),
            createTimestampedDirectory = false
        )

        fun complete() = ExportOptions()

        fun textOnly() = ExportOptions(
            exportFormats = setOf(ExportFormat.WORD, ExportFormat.TEXT),
            includePhotos = false,
            createTimestampedDirectory = false
        )

        fun photoArchive() = ExportOptions(
            exportFormats = setOf(ExportFormat.PHOTO_FOLDER),
            photoFolderQuality = PhotoQuality.ORIGINAL,
            preserveExifData = true,
            generatePhotoIndex = true
        )
    }
}
```

### 8.2 Validazione Configurazione

**🔍 Validation Logic:**
```kotlin
fun validate(): List<String> {
    val errors = mutableListOf<String>()

    if (exportFormats.isEmpty()) {
        errors.add("Almeno un formato export deve essere specificato")
    }

    if (photoMaxWidth <= 0) {
        errors.add("photoMaxWidth deve essere > 0")
    }

    if (addWatermark && watermarkText.isBlank()) {
        errors.add("watermarkText richiesto se addWatermark = true")
    }

    return errors
}
```

### 8.3 ReportTemplate

**🎨 Template Configuration:**
```kotlin
data class ReportTemplate(
    val name: String,
    val headerColor: String = "2E7D32", // Verde corporate
    val logoPath: String = "",
    val footerText: String = "Report generato da QReport",
    val fontFamily: String = "Calibri",
    val baseFontSize: Int = 11
) {
    companion object {
        fun default() = ReportTemplate(
            name = "QReport Standard",
            headerColor = "333333",
            logoPath = "",
            footerText = "Report generato da QReport v1.0"
        )
    }
}
```

---

## 9. GESTIONE ERRORI

### 9.1 ExportErrorCode - Errori Specifici

```kotlin
enum class ExportErrorCode {
    INSUFFICIENT_STORAGE,      // Spazio di archiviazione insufficiente
    PERMISSION_DENIED,         // Permessi di scrittura negati
    TEMPLATE_NOT_FOUND,        // Template non trovato o corrotto
    IMAGE_PROCESSING_ERROR,    // Errore nel processamento immagini
    DOCUMENT_GENERATION_ERROR, // Errore nella generazione documento
    PHOTO_FOLDER_ERROR,        // Errore nella creazione cartella foto
    TEXT_GENERATION_ERROR,     // Errore nella generazione testo
    INVALID_DATA,              // Dati del checkup incompleti o corrotti
    PROCESSING_TIMEOUT,        // Timeout nell'elaborazione
    NETWORK_ERROR,             // Errore di rete (per future implementazioni cloud)
    SYSTEM_ERROR               // Errore generico del sistema
}
```

### 9.2 ExportResult - Result Pattern

```kotlin
sealed class ExportResult {
    data class Success(
        val filePath: String,
        val fileName: String,
        val fileSize: Long,
        val format: ExportFormat,
        val generatedAt: LocalDateTime = LocalDateTime.now()
    ) : ExportResult()

    data class Error(
        val exception: Throwable,
        val errorCode: ExportErrorCode,
        val format: ExportFormat? = null,
        val partialResults: List<Success> = emptyList()
    ) : ExportResult()

    data class Loading(
        val format: ExportFormat,
        val progress: Float = 0f,
        val statusMessage: String = ""
    ) : ExportResult()
}
```

### 9.3 Error Recovery Strategies

**🔄 Recovery Logic nel Repository:**
```kotlin
options.exportFormats.forEach { format ->
    try {
        when (format) {
            ExportFormat.WORD -> {
                val wordResult = generateWordReportInternal(exportData, options, exportDirectory)
                result = result.copy(wordResult = wordResult as? Success)
            }
            // ...altri formati
        }
        
        completedFormats++
        emit(result.copy(statistics = calculateStatistics(exportData, result)))

    } catch (e: Exception) {
        Timber.e(e, "Errore export formato $format")
        // ✅ Continua con altri formati invece di fallire tutto
    }
}
```

---

## 10. PERFORMANCE E OTTIMIZZAZIONI

### 10.1 Stime Preventive

**📊 ExportEstimation System:**
```kotlin
suspend fun estimateExportSize(
    exportData: ExportData,
    options: ExportOptions
): ExportEstimation {
    
    val formatEstimations = mutableMapOf<ExportFormat, FormatEstimation>()
    var totalSize = 0L
    var totalTime = 0L

    options.exportFormats.forEach { format ->
        val estimation = when (format) {
            ExportFormat.WORD -> estimateWordSize(exportData, options)
            ExportFormat.TEXT -> estimateTextSize(exportData)
            ExportFormat.PHOTO_FOLDER -> estimatePhotoFolderSize(exportData)
            ExportFormat.COMBINED_PACKAGE -> estimateCombinedPackageSize(exportData, options)
        }

        formatEstimations[format] = estimation
        totalSize += estimation.estimatedSizeBytes
        totalTime += estimation.estimatedTimeMs
    }

    val warnings = mutableListOf<String>()
    if (totalSize > 100 * 1024 * 1024) { // > 100MB
        warnings.add("Export di grandi dimensioni (>100MB) - potrebbero servire diversi minuti")
    }

    return ExportEstimation(
        estimatedSizeBytes = totalSize,
        estimatedTimeMs = totalTime,
        formatEstimations = formatEstimations,
        warnings = warnings
    )
}
```

**⚡ Stima Word con Foto:**
```kotlin
private fun estimateWordSize(exportData: ExportData, options: ExportOptions): FormatEstimation {
    val baseSize = 500_000L // 500KB base per documento Word
    val photosSize = if (options.includePhotos) {
        val photoCount = exportData.itemsByModule.values.flatten().flatMap { checkItem ->
            checkItem.photos
        }.size
        photoCount * 200_000L // 200KB per foto nel documento Word
    } else 0L

    val totalSize = baseSize + photosSize
    val estimatedTime = 3000L + (photosSize / 100_000L) // Base 3s + tempo per foto

    return FormatEstimation(
        format = ExportFormat.WORD,
        estimatedSizeBytes = totalSize,
        estimatedTimeMs = estimatedTime
    )
}
```

### 10.2 Cleanup Automatico

**🧹 Gestione Spazio Disco:**
```kotlin
override suspend fun cleanupOldExports(olderThanDays: Int): Int = withContext(Dispatchers.IO) {
    var deletedCount = 0
    val exportDir = getDefaultExportDirectory()
    val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)

    exportDir.listFiles()?.forEach { file ->
        if (file.isDirectory && file.lastModified() < cutoffTime) {
            try {
                file.deleteRecursively()
                deletedCount++
                Timber.d("Eliminata directory export obsoleta: ${file.name}")
            } catch (e: Exception) {
                Timber.w(e, "Errore eliminazione directory: ${file.name}")
            }
        }
    }

    return@withContext deletedCount
}
```

### 10.3 Memory Management

**🧠 Ottimizzazioni Memoria:**
- **Streaming processing** per file di grandi dimensioni
- **Bitmap recycling** nell'ImageProcessor
- **File copying** con buffer ottimizzati
- **Coroutines** con Dispatchers.IO per I/O bound operations
- **Progress tracking** senza blocking dell'UI

---

## 11. UI E USER EXPERIENCE

### 11.1 ExportProgressDialog

**📊 Progress Tracking Reattivo:**
```kotlin
@Composable
fun ExportProgressDialog(
    progress: ExportProgress,
    onCancel: () -> Unit
) {
    Dialog(/* ... */) {
        Column {
            // Progress circolare con percentuale
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = progress.percentage / 100f,
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 8.dp
                )
                Text("${progress.percentage.toInt()}%")
            }

            // Operazione corrente
            Text(progress.currentOperation)
            
            // Progress lineare per step corrente
            LinearProgressIndicator(
                progress = progress.currentStepProgress,
                modifier = Modifier.fillMaxWidth()
            )

            // Statistiche export
            ExportStatsRow(
                processedItems = progress.processedItems,
                totalItems = progress.totalItems,
                processedPhotos = progress.processedPhotos,
                totalPhotos = progress.totalPhotos
            )
        }
    }
}
```

### 11.2 ExportResultDialog

**✅ Risultato Export con Azioni:**
```kotlin
@Composable
fun ExportResultDialog(
    result: ExportResult?,
    onOpenFile: () -> Unit,
    onShareFile: () -> Unit
) {
    when (result) {
        is ExportResult.Success -> {
            SuccessContent(
                result = result,
                onOpenFile = onOpenFile,
                onShareFile = onShareFile
            )
        }
        is ExportResult.Error -> {
            ErrorContent(
                result = result,
                suggestions = getErrorSuggestions(result.errorCode)
            )
        }
    }
}
```

### 11.3 ExportOptionsScreen

**⚙️ Configurazione Export Avanzata:**
- **Multi-format selection** con chips selezionabili
- **Photo quality** slider con preview dimensioni
- **Naming strategy** selector con esempi
- **Template** dropdown per styling personalizzato
- **Advanced options** expandable per utenti esperti
- **Estimate preview** con dimensioni e tempo stimati

**🔧 Export Options UI Components:**
```kotlin
@Composable
fun ExportOptionsScreen(
    onExport: (ExportOptions) -> Unit
) {
    Column {
        // Format Selection
        FormatSelectionChips(
            selectedFormats = exportFormats,
            onFormatsChange = { /* update state */ }
        )

        // Photo Settings
        PhotoQualitySlider(
            quality = photoQuality,
            onQualityChange = { /* update */ }
        )

        // Naming Strategy
        NamingStrategySelector(
            strategy = namingStrategy,
            onStrategyChange = { /* update */ }
        )

        // Export Estimate
        ExportEstimateCard(
            estimation = estimatedExport
        )

        // Export Button
        Button(
            onClick = { onExport(buildExportOptions()) }
        ) {
            Text("Avvia Export")
        }
    }
}
```

---

## 🎯 CONCLUSIONI

Il sistema di export QReport rappresenta una **implementazione enterprise-grade** per la generazione di report professionali multi-formato.

**✅ Punti di forza:**
- **Architettura Clean** ben strutturata e manutenibile
- **Multi-formato coordinato** con gestione errori granulare
- **Configurazione flessibile** per ogni caso d'uso
- **Performance ottimizzate** con stime preventive
- **UI reattiva** con progress tracking
- **Error handling robusto** con recovery automatico
- **Naming intelligente** per massima organizzazione

**🚀 Caratteristiche avanzate:**
- Export **multi-formato coordinato** (Word + Text + Photo)
- **Apache POI** per documenti Word professionali
- **ASCII formatting** avanzato per report universali
- **Photo management** con watermark e qualità configurabile
- **Cleanup automatico** per gestione spazio disco
- **Progress tracking** reattivo per UX ottimale

**💼 Valore business:**
- **Report professionali** pronti per presentazione cliente
- **Documentazione completa** con foto e metadati
- **Archiviazione organizzata** per storico manutenzioni
- **Workflow digitalizzato** da manuale a professionale
- **Scalabilità enterprise** per volumi elevati

Il sistema è **pronto per produzione** e supporta completamente il workflow industriale di QReport.

---

**📄 Documento generato:** Novembre 2025  
**📧 Contatto:** luca@calvuz.net  
**🔗 Progetto:** QReport v2.0 - Sistema Export Multi-Formato