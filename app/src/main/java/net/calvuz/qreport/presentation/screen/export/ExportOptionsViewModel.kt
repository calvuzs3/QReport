package net.calvuz.qreport.presentation.screen.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.domain.model.export.*
import net.calvuz.qreport.domain.repository.ExportRepository
import net.calvuz.qreport.domain.usecase.checkup.GetCheckUpDetailsUseCase
import net.calvuz.qreport.domain.model.file.FileManager
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel per ExportOptionsScreen
 *
 * Gestisce:
 * - Configurazione opzioni export
 * - Stima dimensioni e tempo
 * - Progress tracking durante export
 * - Apertura e condivisione file esportati
 */
@HiltViewModel
class ExportOptionsViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val getCheckUpDetailsUseCase: GetCheckUpDetailsUseCase,
    private val fileManager: FileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportOptionsUiState())
    val uiState: StateFlow<ExportOptionsUiState> = _uiState.asStateFlow()

    private var exportJob: Job? = null
    private var currentCheckUpId: String = ""

    init {
        Timber.d("ExportOptionsViewModel initialized")
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    fun initialize(checkUpId: String) {
        if (checkUpId == currentCheckUpId && !_uiState.value.isLoading) {
            return // Already initialized for this checkup
        }

        currentCheckUpId = checkUpId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                Timber.d("Initializing export options for checkup: $checkUpId")

                // Load checkup details
                getCheckUpDetailsUseCase(checkUpId).fold(
                    onSuccess = { checkUpDetails ->
                        val checkUp = checkUpDetails.checkUp
                        val totalPhotos = checkUpDetails.checkItems.sumOf { it.photos.size }

                        _uiState.value = _uiState.value.copy(
                            checkUpId = checkUpId,
                            checkUpName = checkUp.header.clientInfo.companyName + " - " +
                                    checkUp.header.islandInfo.serialNumber,
                            totalItems = checkUpDetails.checkItems.size,
                            totalPhotos = totalPhotos,
                            isLoading = false,
                            error = null
                        )

                        // Calculate initial size estimation
                        updateSizeEstimation()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load checkup details")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Errore caricamento check-up: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception initializing export options")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore inizializzazione: ${e.message}"
                )
            }
        }
    }

    fun setExportFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(exportFormat = format)
        updateSizeEstimation()
    }

    fun setCompressionLevel(level: CompressionLevel) {
        _uiState.value = _uiState.value.copy(compressionLevel = level)
        updateSizeEstimation()
    }

    fun setIncludePhotos(include: Boolean) {
        _uiState.value = _uiState.value.copy(includePhotos = include)
        updateSizeEstimation()
    }

    fun setIncludeNotes(include: Boolean) {
        _uiState.value = _uiState.value.copy(includeNotes = include)
        updateSizeEstimation()
    }

    fun startExport() {
        val state = _uiState.value
        if (!state.canExport || state.isExporting) return

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            try {
                Timber.i("Starting export with format: ${state.exportFormat}")

                _uiState.value = _uiState.value.copy(
                    isExporting = true,
                    exportProgress = ExportProgress.initial(),
                    exportResult = null
                )

                // Get checkup details for export
                val checkUpDetails = getCheckUpDetailsUseCase(currentCheckUpId).getOrThrow()

                // Prepare export options using correct structure
                val exportOptions = ExportOptions(
                    exportFormats = setOf(state.exportFormat),
                    includePhotos = state.includePhotos,
                    includeNotes = state.includeNotes,
                    compressionLevel = state.compressionLevel,
                    photoMaxWidth = getPhotoWidthForCompression(state.compressionLevel),
                    createTimestampedDirectory = true
                )

                // Prepare export data using correct domain models
                val exportData = ExportData(
                    checkup = checkUpDetails.checkUp,
                    itemsByModule = checkUpDetails.checkItems.groupBy { it.moduleType },
                    statistics = checkUpDetails.statistics,
                    progress = checkUpDetails.progress,
                    exportMetadata = ExportTechnicalMetadata(
                        generatedAt = kotlinx.datetime.Clock.System.now(),
                        templateVersion = "1.0",
                        exportFormat = state.exportFormat,
                        exportOptions = exportOptions
                    )
                )

                // Start export with progress tracking
                exportRepository.generateCompleteExport(exportData, exportOptions)
                    .collect { result ->
                        updateExportProgress(result)
                    }

            } catch (e: Exception) {
                Timber.e(e, "Export failed")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = ExportResult.Error(
                        exception = e,
                        errorCode = ExportErrorCode.DOCUMENT_GENERATION_ERROR
                    ),
                    showResultDialog = true
                )
            }
        }
    }

    fun cancelExport() {
        Timber.d("Canceling export")
        exportJob?.cancel()
        exportJob = null

        _uiState.value = _uiState.value.copy(
            isExporting = false,
            exportProgress = ExportProgress.initial(),
            exportResult = null
        )
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(
            showResultDialog = false,
            exportResult = null,
            exportCompleted = false
        )
    }

    fun openExportedFile() {
        val result = _uiState.value.exportResult as? ExportResult.Success ?: return

        fileManager.openExportedFile(result).fold(
            onSuccess = {
                // Success handled by fileManager
            },
            onFailure = { e ->
                Timber.e(e, "Failed to open exported file")
                _uiState.value = _uiState.value.copy(
                    error = "Impossibile aprire il file: ${e.message}"
                )
            }
        )
    }

    fun shareExportedFile() {
        val result = _uiState.value.exportResult as? ExportResult.Success ?: return

        fileManager.shareExportedFile(result).fold(
            onSuccess = {
                // Success handled by fileManager
            },
            onFailure = { e ->
                Timber.e(e, "Failed to share exported file")
                _uiState.value = _uiState.value.copy(
                    error = "Impossibile condividere il file: ${e.message}"
                )
            }
        )
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    private fun updateSizeEstimation() {
        val state = _uiState.value
        if (state.checkUpId.isEmpty()) return

        viewModelScope.launch {
            try {
                // Get checkup details for estimation
                val checkUpDetails = getCheckUpDetailsUseCase(currentCheckUpId).getOrNull() ?: return@launch

                val exportOptions = ExportOptions(
                    exportFormats = setOf(state.exportFormat),
                    includePhotos = state.includePhotos,
                    includeNotes = state.includeNotes,
                    compressionLevel = state.compressionLevel,
                    photoMaxWidth = getPhotoWidthForCompression(state.compressionLevel)
                )

                val exportData = ExportData(
                    checkup = checkUpDetails.checkUp,
                    itemsByModule = checkUpDetails.checkItems.groupBy { it.moduleType },
                    statistics = checkUpDetails.statistics,
                    progress = checkUpDetails.progress,
                    exportMetadata = ExportTechnicalMetadata(
                        generatedAt = kotlinx.datetime.Clock.System.now(),
                        templateVersion = "1.0",
                        exportFormat = state.exportFormat,
                        exportOptions = exportOptions
                    )
                )

                val estimation = exportRepository.estimateExportSize(exportData, exportOptions)

                _uiState.value = _uiState.value.copy(
                    estimatedSize = formatFileSize(estimation.estimatedSizeBytes)
                )

            } catch (e: Exception) {
                Timber.w(e, "Failed to estimate export size")
                // Don't show error for size estimation failures
            }
        }
    }

    private fun updateExportProgress(result: MultiFormatExportResult) {
        val stats = result.statistics

        // Calculate overall progress based on what's completed
        val progress = calculateOverallProgress(result)

        val exportProgress = ExportProgress(
            percentage = progress,
            currentOperation = getCurrentOperation(result),
            processedItems = stats.checkItemsProcessed,
            totalItems = _uiState.value.totalItems,
            processedPhotos = stats.photosProcessed,
            totalPhotos = _uiState.value.totalPhotos,
            estimatedTimeRemaining = formatDuration(
                ((stats.processingTimeMs * (100f - progress)) / progress.coerceAtLeast(1f)).toLong()
            )
        )

        _uiState.value = _uiState.value.copy(exportProgress = exportProgress)

        // Check if export completed
        if (isExportComplete(result)) {
            val successResult = getSuccessResult(result)
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportResult = successResult,
                showResultDialog = true,
                exportCompleted = true
            )
        }
    }

    private fun calculateOverallProgress(result: MultiFormatExportResult): Float {
        val hasWord = result.wordResult != null
        val hasText = result.textResult != null
        val hasPhotos = result.photoFolderResult != null

        val format = _uiState.value.exportFormat

        return when (format) {
            ExportFormat.WORD -> if (hasWord) 100f else 50f
            ExportFormat.TEXT -> if (hasText) 100f else 50f
            ExportFormat.PHOTO_FOLDER -> if (hasPhotos) 100f else 50f
            ExportFormat.COMBINED_PACKAGE -> {
                var progress = 0f
                if (hasWord) progress += 33f
                if (hasText) progress += 33f
                if (hasPhotos) progress += 34f
                progress
            }
        }
    }

    private fun getCurrentOperation(result: MultiFormatExportResult): String {
        val format = _uiState.value.exportFormat

        return when (format) {
            ExportFormat.WORD ->
                if (result.wordResult == null) "Generazione documento Word..."
                else "Finalizzazione export..."
            ExportFormat.TEXT ->
                if (result.textResult == null) "Generazione report testuale..."
                else "Finalizzazione export..."
            ExportFormat.PHOTO_FOLDER ->
                if (result.photoFolderResult == null) "Organizzazione foto..."
                else "Finalizzazione export..."
            ExportFormat.COMBINED_PACKAGE -> {
                when {
                    result.wordResult == null -> "Generazione documento Word..."
                    result.textResult == null -> "Generazione report testuale..."
                    result.photoFolderResult == null -> "Organizzazione foto..."
                    else -> "Creazione package finale..."
                }
            }
        }
    }

    private fun isExportComplete(result: MultiFormatExportResult): Boolean {
        val format = _uiState.value.exportFormat

        return when (format) {
            ExportFormat.WORD -> result.wordResult != null
            ExportFormat.TEXT -> result.textResult != null
            ExportFormat.PHOTO_FOLDER -> result.photoFolderResult != null
            ExportFormat.COMBINED_PACKAGE ->
                result.wordResult != null &&
                        result.textResult != null &&
                        result.photoFolderResult != null
        }
    }

    private fun getSuccessResult(result: MultiFormatExportResult): ExportResult.Success? {
        val format = _uiState.value.exportFormat

        return when (format) {
            ExportFormat.WORD -> result.wordResult
            ExportFormat.TEXT -> result.textResult
            ExportFormat.PHOTO_FOLDER -> result.photoFolderResult
            ExportFormat.COMBINED_PACKAGE -> {
                // For combined package, return directory info
                result.exportDirectory?.let { dir ->
                    ExportResult.Success(
                        filePath = dir,
                        fileName = "Export_Package",
                        fileSize = result.totalFileSize,
                        format = ExportFormat.COMBINED_PACKAGE
                    )
                }
            }
        }
    }

    private fun getPhotoWidthForCompression(level: CompressionLevel): Int {
        return when (level) {
            CompressionLevel.LOW -> 1920    // Alta qualità
            CompressionLevel.MEDIUM -> 1280 // Media qualità
            CompressionLevel.HIGH -> 800    // Bassa qualità
        }
    }

    private fun getMimeType(format: ExportFormat): String {
        return when (format) {
            ExportFormat.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ExportFormat.TEXT -> "text/plain"
            ExportFormat.PHOTO_FOLDER -> "application/zip"
            ExportFormat.COMBINED_PACKAGE -> "application/zip"
        }
    }

    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))}MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))}GB"
        }
    }

    override fun onCleared() {
        super.onCleared()
        exportJob?.cancel()
    }
}

/**
 * UI State per ExportOptionsScreen
 */
data class ExportOptionsUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null,

    // Checkup info
    val checkUpId: String = "",
    val checkUpName: String = "",
    val totalItems: Int = 0,
    val totalPhotos: Int = 0,

    // Export configuration
    val exportFormat: ExportFormat = ExportFormat.WORD,
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val includePhotos: Boolean = true,
    val includeNotes: Boolean = true,

    // Estimation
    val estimatedSize: String = "",

    // Export progress
    val exportProgress: ExportProgress = ExportProgress.initial(),
    val exportResult: ExportResult? = null,
    val showResultDialog: Boolean = false,
    val exportCompleted: Boolean = false
) {
    val canExport: Boolean
        get() = checkUpId.isNotEmpty() && !isLoading && error == null
}