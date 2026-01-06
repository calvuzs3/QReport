package net.calvuz.qreport.export.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.export.domain.reposirory.ExportRepository
import net.calvuz.qreport.checkup.domain.usecase.GetCheckUpDetailsUseCase
import net.calvuz.qreport.export.domain.usecase.ExportCheckUpUseCase
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.util.DateTimeUtils.asFormatedDuration
import net.calvuz.qreport.app.util.SizeUtils.getFormattedSize
import net.calvuz.qreport.export.domain.reposirory.ExportData
import net.calvuz.qreport.export.domain.reposirory.ExportFileRepository
import net.calvuz.qreport.export.domain.reposirory.ExportFormat
import net.calvuz.qreport.export.domain.reposirory.ExportOptions
import net.calvuz.qreport.export.domain.reposirory.ExportResult
import net.calvuz.qreport.export.domain.reposirory.ExportTechnicalMetadata
import net.calvuz.qreport.export.domain.reposirory.MultiFormatExportResult
import net.calvuz.qreport.export.domain.reposirory.PhotoNamingStrategy
import net.calvuz.qreport.export.domain.reposirory.PhotoQuality
import net.calvuz.qreport.export.presentation.ui.components.ExportProgress
import net.calvuz.qreport.share.domain.usecase.OpenFileUseCase
import net.calvuz.qreport.share.domain.usecase.ShareFileUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * UI State per ExportOptionsScreen
 */
data class ExportOptionsUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: QrError? = null,

    // Checkup info
    val checkUpId: String = "",
    val checkUpName: String = "",
    val totalItems: Int = 0,
    val totalPhotos: Int = 0,

    // Export configuration
    val exportFormat: ExportFormat = ExportFormat.WORD,
    val photoQuality: PhotoQuality = PhotoQuality.OPTIMIZED,
    val includePhotos: Boolean = true,
    val includeNotes: Boolean = true,

    // Estimation
    val estimatedSize: String = "",

    // Export progress
    val exportProgress: ExportProgress = ExportProgress.initial(UiText.StringResources(R.string.export_dialog_progress_state_init)),
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
    private val exportCheckUpUseCase: ExportCheckUpUseCase,
    private val exportRepository: ExportRepository,
    private val getCheckUpDetailsUseCase: GetCheckUpDetailsUseCase,
    private val exportFileRepository: ExportFileRepository,
    private val openFileUseCase: OpenFileUseCase,
    private val shareFileUseCase: ShareFileUseCase
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

                // Load checkup details per UI info
                when (val result = getCheckUpDetailsUseCase(checkUpId)) {
                    is QrResult.Error -> {
                        Timber.e("Failed to load checkup details")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = QrError.Checkup.LOAD
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

            } catch (e: Exception) {
                Timber.e(e, "Exception initializing export options")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = QrError.Checkup.UNKNOWN
                )
            }
        }
    }

    // ============================================================
    // CONFIGURATION METHODS
    // ============================================================

    fun setExportFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(exportFormat = format)
        updateSizeEstimation()
    }

    fun setPhotoQuality(photoQuality: PhotoQuality) {
        _uiState.value = _uiState.value.copy(photoQuality = photoQuality)
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

    // ============================================================
    // EXPORT EXECUTION - SIMPLIFIED
    // ============================================================

    /**
     * Avvia export delegando a ExportCheckUpUseCase
     * Ora molto più semplice!
     */
    fun startExport() {
        val state = _uiState.value
        if (!state.canExport || state.isExporting) return

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            try {
                Timber.i("Starting export with format: ${state.exportFormat}")

                _uiState.value = _uiState.value.copy(
                    isExporting = true,
                    exportProgress = ExportProgress.initial(UiText.StringResources(R.string.export_dialog_progress_state_init)),
                    exportResult = null
                )

                // Prepara opzioni export dalla UI
                val exportOptions = ExportOptions(
                    exportFormats = setOf(state.exportFormat),
                    includePhotos = state.includePhotos,
                    includeNotes = state.includeNotes,
                    photoNamingStrategy = PhotoNamingStrategy.TIMESTAMP,
                    photoQuality = state.photoQuality,
                    photoMaxWidth = getPhotoWidthForPhotoQuality(state.photoQuality),
                    createTimestampedDirectory = true
                )

                // ===== DELEGA AL USE CASE =====
                // Tutto il coordinamento export è ora gestito dal use case
                exportCheckUpUseCase(currentCheckUpId, exportOptions)
                    .collect { result ->
                        when (result) {
                            is QrResult.Success -> {
                                // Aggiorna progresso UI
                                updateExportProgress(result.data)
                            }

                            is QrResult.Error -> {
                                // Gestisci errori export
                                Timber.e("Export failed: ${result.error}")
                                _uiState.value = _uiState.value.copy(
                                    isExporting = false,
                                    error = result.error
                                )
                            }
                        }
                    }

            } catch (e: Exception) {
                Timber.e(e, "Export failed with exception")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = QrError.Checkup.UNKNOWN
                )
            }
        }
    }

    // ============================================================
    // FILE OPERATIONS
    // ============================================================

    fun openFile() {
        val result = _uiState.value.exportResult as? ExportResult.Success ?: return

        viewModelScope.launch {
            try {
                Timber.d("Opening file: ${result.filePath}")
                when (openFileUseCase(
                    filePath = result.filePath,  // Internal directory path
                    mimeType = "resource/folder",
                    chooserTitle = "Open QReport Export",
                    allowChooser = true,
                    requireExternalApp = false,
                    openAsDirectory = true,        // ✅ Open as directory
                    copyToDocuments = true         // ✅ Auto-copy to Documents
                )) {
                    is QrResult.Success -> {
                        Timber.d("File opened successfully")
                    }

                    is QrResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = QrError.Checkup.FILE_OPEN
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, QrError.Checkup.FILE_OPEN.name)
                _uiState.value = _uiState.value.copy(
                    error = QrError.Checkup.FILE_OPEN
                )
            }
        }
//        viewModelScope.launch {
//            try {
//                when (exportFileRepository.openFileWith(result.filePath, result.format)) {
//                    is QrResult.Success -> {
//                        Timber.d("File opened successfully")
//                    }
//
//                    is QrResult.Error -> {
//                        _uiState.value = _uiState.value.copy(
//                            error = QrError.Checkup.FILE_OPEN
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                Timber.e(e, QrError.Checkup.FILE_OPEN.name)
//                _uiState.value = _uiState.value.copy(
//                    error = QrError.Checkup.FILE_OPEN
//                )
//            }
//        }
    }

    fun shareFile() {
        val result = _uiState.value.exportResult as? ExportResult.Success ?: return

        viewModelScope.launch {
            try {
                when (shareFileUseCase(
                    filePath =  result.filePath,
                    mimeType = result.format.mimeType,
                    shareTitle = "Apri con ..",
                    shareSubject = result.fileName

                )) {
                    is QrResult.Success -> {
                        Timber.d("File shared successfully")
                    }

                    is QrResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = QrError.Checkup.FILE_SHARE
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to share file")
                _uiState.value = _uiState.value.copy(
                    error = QrError.Checkup.FILE_SHARE
                )
            }
        }
//        viewModelScope.launch {
//            try {
//                when (exportFileRepository.shareFileWith(result.filePath, result.format)) {
//                    is QrResult.Success -> {
//                        Timber.d("File shared successfully")
//                    }
//
//                    is QrResult.Error -> {
//                        _uiState.value = _uiState.value.copy(
//                            error = QrError.Checkup.FILE_SHARE
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                Timber.e(e, "Failed to share file")
//                _uiState.value = _uiState.value.copy(
//                    error = QrError.Checkup.FILE_SHARE
//                )
//            }
//        }
    }

    fun dismissResultDialog() {
        _uiState.value = _uiState.value.copy(
            showResultDialog = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }


    // ============================================================
    // PRIVATE HELPERS - UNCHANGED
    // ============================================================

    private fun updateSizeEstimation() {
        val state = _uiState.value
        if (state.checkUpId.isEmpty()) return

        viewModelScope.launch {
            try {
                // Use simplified estimation
                val checkUpDetails =
                    when (val result = getCheckUpDetailsUseCase(currentCheckUpId)) {
                        is QrResult.Success -> result.data
                        is QrResult.Error -> return@launch
                    }

                val exportOptions = ExportOptions(
                    exportFormats = setOf(state.exportFormat),
                    includePhotos = state.includePhotos,
                    includeNotes = state.includeNotes,
                    photoQuality = state.photoQuality,
                    photoMaxWidth = getPhotoWidthForPhotoQuality(state.photoQuality)
                )

                val exportData = ExportData(
                    checkup = checkUpDetails.checkUp,
                    itemsByModule = checkUpDetails.checkItems.groupBy { it.moduleType },
                    statistics = checkUpDetails.statistics,
                    progress = checkUpDetails.progress,
                    exportMetadata = ExportTechnicalMetadata(
                        generatedAt = Clock.System.now(),
                        templateVersion = "1.0",
                        exportFormat = state.exportFormat,
                        exportOptions = exportOptions
                    )
                )

                val estimation = exportRepository.estimateExportSize(exportData, exportOptions)

                _uiState.value = _uiState.value.copy(
                    estimatedSize = estimation.estimatedSizeBytes.getFormattedSize()
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
            estimatedTimeRemaining =
                ((stats.processingTimeMs * (100f - progress)) / progress.coerceAtLeast(1f)).toLong()
                    .asFormatedDuration()
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
                    UiText.StringResources(R.string.export_operation_generating_word)
                else
                    UiText.StringResources(R.string.export_operation_finalizing)

            ExportFormat.TEXT ->
                if (result.textResult == null)
                    UiText.StringResources(R.string.export_operation_generating_text)
                else
                    UiText.StringResources(R.string.export_operation_finalizing)

            ExportFormat.PHOTO_FOLDER ->
                if (result.photoFolderResult == null)
                    UiText.StringResources(R.string.export_operation_organizing_photos)
                else
                    UiText.StringResources(R.string.export_operation_finalizing)

            ExportFormat.COMBINED_PACKAGE -> {
                when {
                    result.wordResult == null -> UiText.StringResources(R.string.export_operation_generating_word)
                    result.textResult == null -> UiText.StringResources(R.string.export_operation_generating_text)
                    result.photoFolderResult == null -> UiText.StringResources(R.string.export_operation_organizing_photos)
                    else -> UiText.StringResources(R.string.export_operation_creating_package)
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


    private fun getPhotoWidthForPhotoQuality(photoQuality: PhotoQuality): Int {
        return when (photoQuality) {
            PhotoQuality.ORIGINAL -> 1920    // Alta qualità
            PhotoQuality.OPTIMIZED -> 1280 // Media qualità
            PhotoQuality.COMPRESSED -> 800    // Bassa qualità
        }
    }


    override fun onCleared() {
        super.onCleared()
        exportJob?.cancel()
    }
}

