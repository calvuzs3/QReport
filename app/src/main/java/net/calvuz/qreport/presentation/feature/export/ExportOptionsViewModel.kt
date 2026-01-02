package net.calvuz.qreport.presentation.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.core.QrResult
import net.calvuz.qreport.domain.model.export.*
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.repository.ExportRepository
import net.calvuz.qreport.domain.usecase.checkup.GetCheckUpDetailsUseCase
import net.calvuz.qreport.presentation.core.model.DataError
import net.calvuz.qreport.presentation.core.model.UiText
import timber.log.Timber
import javax.inject.Inject

/**
 * UI State per ExportOptionsScreen
 */
data class ExportOptionsUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: DataError.CheckupError? = null,

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
    val exportProgress: ExportProgress = ExportProgress.initial(UiText.StringResource(R.string.export_dialog_progress_state_init)),
    val exportResult: ExportResult? = null,
    val showResultDialog: Boolean = false,
    val exportCompleted: Boolean = false
) {
    val canExport: Boolean
        get() = checkUpId.isNotEmpty() && !isLoading && error == null
}

/**
 *  Export Options Screen ViewModel
 *
 * Handles:
 * - Export options
 * - Estimated time and dimensions
 * - Progress tracking
 * - Open and share exported files
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
                when (val result = getCheckUpDetailsUseCase(checkUpId)) {
                    is QrResult.Error -> {
                        Timber.e("Failed to load checkup details")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = DataError.CheckupError.LOAD
                        )
                    }

                    is QrResult.Success -> {
                        val checkUp = result.data.checkUp
                        val totalPhotos = result.data.checkItems.sumOf { it.photos.size }

                        _uiState.value = _uiState.value.copy(
                            checkUpId = checkUpId,
                            checkUpName = checkUp.header.clientInfo.companyName + " - " +
                                    checkUp.header.islandInfo.serialNumber,
                            totalItems = result.data.checkItems.size,
                            totalPhotos = totalPhotos,
                            isLoading = false,
                            error = null
                        )

                        // Calculate initial size estimation
                        updateSizeEstimation()
                    }
                }
//                getCheckUpDetailsUseCase(checkUpId).fold(
//                    onSuccess = { checkUpDetails ->
//                        val checkUp = checkUpDetails.checkUp
//                        val totalPhotos = checkUpDetails.checkItems.sumOf { it.photos.size }
//
//                        _uiState.value = _uiState.value.copy(
//                            checkUpId = checkUpId,
//                            checkUpName = checkUp.header.clientInfo.companyName + " - " +
//                                    checkUp.header.islandInfo.serialNumber,
//                            totalItems = checkUpDetails.checkItems.size,
//                            totalPhotos = totalPhotos,
//                            isLoading = false,
//                            error = null
//                        )
//
//                        // Calculate initial size estimation
//                        updateSizeEstimation()
//                    },
//                    onFailure = { error ->
//                        Timber.e(error, "Failed to load checkup details")
//                        _uiState.value = _uiState.value.copy(
//                            isLoading = false,
//                            error = "Errore caricamento check-up: ${error.message}"
//                        )
//                    }
//                )
            } catch (e: Exception) {
                Timber.e(e, "Exception initializing export options")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = DataError.CheckupError.UNKNOWN
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
                    exportProgress = ExportProgress.initial(UiText.StringResource(R.string.export_dialog_progress_state_init)),
                    exportResult = null
                )

                // Get checkup details for export
                val checkUpDetails =
                    when (val result = getCheckUpDetailsUseCase(currentCheckUpId)) {
                        is QrResult.Success -> {
                            result.data
                        }

                        is QrResult.Error -> {
                            // This shouldn't happen
                            throw Exception("Check up details load failed")
                        }
                    }

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
                Timber.e(e, "Failed to export checkup")
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

    fun cancelExport(message: UiText) {
        Timber.d("Canceling export")
        exportJob?.cancel()
        exportJob = null

        _uiState.value = _uiState.value.copy(
            isExporting = false,
            exportProgress = ExportProgress.initial(message),
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
                    error = DataError.CheckupError.FILE_OPEN // "Impossibile aprire il file: ${e.message}"
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
                    error = DataError.CheckupError.FILE_SHARE // "Impossibile condividere il file: ${e.message}"
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
                val checkUpDetails =
                    when (val result = getCheckUpDetailsUseCase(currentCheckUpId)) {
                        is QrResult.Success -> {
                            result.data
                        }

                        is QrResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = DataError.CheckupError.LOAD
                            )
                            return@launch
                        }
                    }

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

    private fun getCurrentOperation(result: MultiFormatExportResult): UiText {
        val format = _uiState.value.exportFormat

        return when (format) {
            ExportFormat.WORD ->
                if (result.wordResult == null)
                    UiText.StringResource(R.string.export_operation_generating_word)
                else
                    UiText.StringResource(R.string.export_operation_finalizing)

            ExportFormat.TEXT ->
                if (result.textResult == null)
                    UiText.StringResource(R.string.export_operation_generating_text)
                else
                    UiText.StringResource(R.string.export_operation_finalizing)

            ExportFormat.PHOTO_FOLDER ->
                if (result.photoFolderResult == null)
                    UiText.StringResource(R.string.export_operation_organizing_photos)
                else
                    UiText.StringResource(R.string.export_operation_finalizing)

            ExportFormat.COMBINED_PACKAGE -> {
                when {
                    result.wordResult == null -> UiText.StringResource(R.string.export_operation_generating_word)
                    result.textResult == null -> UiText.StringResource(R.string.export_operation_generating_text)
                    result.photoFolderResult == null -> UiText.StringResource(R.string.export_operation_organizing_photos)

                    else -> UiText.StringResource(R.string.export_operation_creating_package)

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
                        fileName = "QReport_Export_Package",
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

