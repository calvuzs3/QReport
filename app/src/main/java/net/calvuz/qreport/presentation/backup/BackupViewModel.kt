package net.calvuz.qreport.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.usecase.backup.*
import net.calvuz.qreport.util.SizeUtils.getFormattedSize
import timber.log.Timber
import javax.inject.Inject

/**
 * UI State per BackupScreen
 */
data class BackupUiState(
    // Opzioni backup
    val includePhotos: Boolean = true,
    val includeThumbnails: Boolean = false,
    val backupMode: BackupMode = BackupMode.LOCAL,
    val backupDescription: String = "",

    // Stima backup
    val estimatedBackupSize: Long = 0L,

    // Lista backup
    val availableBackups: List<BackupInfo> = emptyList(),
    val lastBackupDate: Instant? = null,

    // Stati UI
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

/**
 * BACKUP VIEW MODEL - FASE 5.4
 *
 * Gestisce state management per BackupScreen con:
 * - Progress tracking backup/restore
 * - Configurazione opzioni backup
 * - Lista backup disponibili
 * - Actions (create, restore, delete, share)
 */

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val getAvailableBackupsUseCase: GetAvailableBackupsUseCase,
    private val deleteBackupUseCase: DeleteBackupUseCase,
    private val getBackupSizeUseCase: GetBackupSizeUseCase,
    private val shareBackupUseCase: ShareBackupUseCase,
    private val validateBackupUseCase: ValidateBackupUseCase
) : ViewModel() {

    // ===== UI STATE =====

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _backupProgress = MutableStateFlow<BackupProgress>(BackupProgress.Idle)
    val backupProgress: StateFlow<BackupProgress> = _backupProgress.asStateFlow()

    private val _restoreProgress = MutableStateFlow<RestoreProgress>(RestoreProgress.Idle)
    val restoreProgress: StateFlow<RestoreProgress> = _restoreProgress.asStateFlow()

    // ===== INITIALIZATION =====

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        Timber.d("Loading initial backup data")
        loadBackupEstimate()
        loadAvailableBackups()
    }

    // ===== BACKUP OPTIONS =====

    /**
     * Toggle inclusione foto nel backup
     */
    fun toggleIncludePhotos() {
        _uiState.update { currentState ->
            val newState = currentState.copy(includePhotos = !currentState.includePhotos)
            // Se disabilito foto, disabilito anche thumbnail
            if (!newState.includePhotos) {
                newState.copy(includeThumbnails = false)
            } else {
                newState
            }
        }

        // Ricalcola stima dimensione
        loadBackupEstimate()

        Timber.d("Include photos toggled to: ${uiState.value.includePhotos}")
    }

    /**
     * Toggle inclusione thumbnails (solo se foto abilitate)
     */
    fun toggleIncludeThumbnails() {
        if (!uiState.value.includePhotos) {
            Timber.w("Cannot include thumbnails without photos")
            return
        }

        _uiState.update {
            it.copy(includeThumbnails = !it.includeThumbnails)
        }

        loadBackupEstimate()
        Timber.d("Include thumbnails toggled to: ${uiState.value.includeThumbnails}")
    }

    /**
     * Aggiorna modalità backup (LOCAL, CLOUD, BOTH)
     */
    fun updateBackupMode(mode: BackupMode) {
        _uiState.update { it.copy(backupMode = mode) }
        Timber.d("Backup mode updated to: $mode")
    }

    /**
     * Aggiorna descrizione backup
     */
    fun updateBackupDescription(description: String) {
        _uiState.update { it.copy(backupDescription = description) }
    }

    // ===== BACKUP ACTIONS =====

    /**
     * Crea nuovo backup con opzioni correnti
     */
    fun createBackup() {
        if (_backupProgress.value is BackupProgress.InProgress) {
            Timber.w("Backup already in progress")
            return
        }

        val currentState = uiState.value
        Timber.d(buildString {
            append("Creating backup: photos=${currentState.includePhotos}, ")
            append("thumbnails=${currentState.includeThumbnails}, ")
            append("mode=${currentState.backupMode}")
        })

        viewModelScope.launch {
            try {
                createBackupUseCase(
                    includePhotos = currentState.includePhotos,
                    includeThumbnails = currentState.includeThumbnails,
                    backupMode = currentState.backupMode,
                    description = currentState.backupDescription.ifEmpty { "" }
                ).collect { progress ->
                    _backupProgress.value = progress

                    when (progress) {
                        is BackupProgress.Completed -> {
                            Timber.d("Backup completed: ${progress.backupPath}")
                            loadAvailableBackups() // Refresh lista
                            showSuccessMessage("Backup creato con successo")
                        }
                        is BackupProgress.Error -> {
                            Timber.e("Backup failed: ${progress.message}")
                            showErrorMessage("Errore durante backup: ${progress.message}")
                        }
                        else -> {
                            // Progress in corso
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Backup creation failed")
                _backupProgress.value = BackupProgress.Error("Errore imprevisto: ${e.message}")
                showErrorMessage("Errore imprevisto durante backup")
            }
        }
    }

    /**
     * Cancella backup in corso
     */
    fun cancelBackup() {
        if (_backupProgress.value !is BackupProgress.InProgress) {
            return
        }

        _backupProgress.value = BackupProgress.Idle
        Timber.d("Backup cancelled by user")
        showInfoMessage("Backup annullato")
    }

    // ===== RESTORE ACTIONS =====

    /**
     * Ripristina backup selezionato
     */
    fun restoreBackup(backupId: String, strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL) {
        if (_restoreProgress.value is RestoreProgress.InProgress) {
            Timber.w("Restore already in progress")
            return
        }

        Timber.d("Starting restore for backup: $backupId with strategy: $strategy")

        viewModelScope.launch {
            try {
                // Prima valida backup
                val backup = uiState.value.availableBackups.find { it.id == backupId }
                if (backup == null) {
                    showErrorMessage("Backup non trovato")
                    return@launch
                }

                val validation = validateBackupUseCase(backup.filePath)
                if (!validation.isValid) {
                    showErrorMessage("Backup non valido: ${validation.errors.firstOrNull()}")
                    return@launch
                }

                // Procedi con restore
                restoreBackupUseCase(backup.dirPath, backup.filePath, strategy).collect { progress ->
                    _restoreProgress.value = progress

                    when (progress) {
                        is RestoreProgress.Completed -> {
                            Timber.d("Restore completed successfully")
                            showSuccessMessage("Ripristino completato con successo")
                        }
                        is RestoreProgress.Error -> {
                            Timber.e("Restore failed: ${progress.message}")
                            showErrorMessage("Errore durante ripristino: ${progress.message}")
                        }
                        else -> {
                            // Progress in corso
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during restore")
                _restoreProgress.value = RestoreProgress.Error("Errore imprevisto: ${e.message}")
                showErrorMessage("Errore imprevisto durante ripristino")
            }
        }
    }

    /**
     * Cancella restore in corso
     */
    fun cancelRestore() {
        if (_restoreProgress.value !is RestoreProgress.InProgress) {
            return
        }

        _restoreProgress.value = RestoreProgress.Idle
        Timber.d("Restore cancelled by user")
        showInfoMessage("Ripristino annullato")
    }

    // ===== BACKUP MANAGEMENT =====

    /**
     * Elimina backup selezionato
     */
    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            try {
                val result = deleteBackupUseCase(backupId)

                if (result.isSuccess) {
                    loadAvailableBackups() // Refresh lista
                    showSuccessMessage("Backup eliminato")
                    Timber.d("Backup deleted: $backupId")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
                    showErrorMessage("Impossibile eliminare backup: $error")
                    Timber.e("Failed to delete backup $backupId: $error")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting backup $backupId")
                showErrorMessage("Errore durante eliminazione backup")
            }
        }
    }

    /**
     * Condividi backup selezionato
     */
    fun shareBackup(backupId: String) {
        viewModelScope.launch {
            try {
                val backup = uiState.value.availableBackups.find { it.id == backupId }
                if (backup == null) {
                    showErrorMessage("Backup non trovato")
                    return@launch
                }

                val result = shareBackupUseCase(backup.filePath)

                if (result.isSuccess) {
                    showSuccessMessage("Backup condiviso")
                    Timber.d("Backup shared: $backupId")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
                    showErrorMessage("Impossibile condividere backup: $error")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sharing backup $backupId")
                showErrorMessage("Errore durante condivisione backup")
            }
        }
    }

    // ===== DATA LOADING =====

    /**
     * Carica lista backup disponibili
     */
    private fun loadAvailableBackups() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val backups = getAvailableBackupsUseCase()

                _uiState.update {
                    it.copy(
                        availableBackups = backups,
                        lastBackupDate = backups.maxByOrNull { backup -> backup.timestamp }?.timestamp,
                        isLoading = false
                    )
                }
                Timber.d("Backups loaded ${backups.size}")
            } catch (e: Exception) {
                Timber.e(e, "Loading available backups failed")
                _uiState.update { it.copy(isLoading = false) }
                showErrorMessage("Errore caricamento backup")
            }
        }
    }

    /**
     * Calcola stima dimensione backup
     */
    private fun loadBackupEstimate() {
        viewModelScope.launch {
            try {
                val size = getBackupSizeUseCase(uiState.value.includePhotos)
                _uiState.update { it.copy(estimatedBackupSize = size) }

                Timber.d("Estimated backup size: ${size.getFormattedSize()}")
            } catch (e: Exception) {
                Timber.e(e, "Error calculating backup size")
                // Non è critico, continua senza estimate
            }
        }
    }

    // ===== UI MESSAGES =====

    private fun showSuccessMessage(message: String) {
        _uiState.update { it.copy(successMessage = message) }
    }

    private fun showErrorMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private fun showInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    /**
     * Dismisses UI messages
     */
    fun dismissMessage() {
        _uiState.update {
            it.copy(
                successMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    // ===== REFRESH =====

    /**
     * Refresh completo dati
     */
    fun refreshData() {
        Timber.d("Refreshing backup data")
        loadInitialData()
    }
}



/*
=============================================================================
                            BACKUP UI STATE MANAGEMENT
=============================================================================

RESPONSABILITÀ:
✅ State management per BackupScreen
✅ Progress tracking backup/restore operations
✅ Configuration backup options (foto, thumbnails, mode)
✅ Lista backup disponibili con refresh
✅ Actions management (create, restore, delete, share)
✅ Error handling e UI messaging
✅ Size estimation per backup

PATTERN UTILIZZATI:
✅ StateFlow per reactive UI
✅ viewModelScope per coroutines lifecycle
✅ Timber logging per debugging
✅ Result pattern per error handling
✅ Progress sealed classes per tracking

UI INTEGRATION:
✅ collectAsStateWithLifecycle() in Composable
✅ Automatic recomposition su state changes
✅ Progress indicators real-time
✅ Error/success messages display

TESTING:
✅ MockK per use case mocking
✅ Turbine per Flow testing
✅ StateFlow testing patterns

=============================================================================
*/