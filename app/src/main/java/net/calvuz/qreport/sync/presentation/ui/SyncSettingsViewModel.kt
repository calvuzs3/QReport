package net.calvuz.qreport.sync.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.calvuz.qreport.sync.domain.model.SyncMode
import net.calvuz.qreport.sync.domain.model.SyncStatus
import net.calvuz.qreport.sync.domain.repository.SyncRepository
import javax.inject.Inject

@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncSettingsUiState())
    val uiState: StateFlow<SyncSettingsUiState> = _uiState.asStateFlow()

    // Current sync mode observed as StateFlow
    val syncMode = syncRepository.getSyncMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncMode.LOCAL_ONLY
        )

    // Last sync timestamp observed as StateFlow
    val lastSyncTimestamp = syncRepository.getLastSyncTimestamp()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Server url
    val serverUrl = syncRepository.getServerUrl()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    init {
        loadSyncStatus()
    }

    /**
     * Load the full sync status snapshot (pending count + device id).
     */
    fun loadSyncStatus() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val result = syncRepository.getSyncStatus()

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        syncStatus = result.getOrNull()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore caricamento stato sync: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore inaspettato: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle sync mode between [SyncMode.LOCAL_ONLY] and [SyncMode.REMOTE_ENABLED].
     */
    fun toggleSyncMode() {
        val current = syncMode.value
        val next = when (current) {
            SyncMode.LOCAL_ONLY -> SyncMode.REMOTE_ENABLED
            SyncMode.REMOTE_ENABLED -> SyncMode.LOCAL_ONLY
        }
        setSyncMode(next)
    }

    /**
     * Explicitly set the sync mode.
     */
    fun setSyncMode(mode: SyncMode) {
        viewModelScope.launch {
            try {
                val result = syncRepository.setSyncMode(mode)

                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Errore aggiornamento modalità sync: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore inaspettato: ${e.message}"
                )
            }
        }
    }

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            val result = syncRepository.setServerUrl(url)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore salvataggio URL: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Clear sync state (timestamps and mode). Used when logging out from the server.
     */
    fun clearSyncState() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val result = syncRepository.clearSyncState()

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Stato sincronizzazione azzerato"
                    )
                    loadSyncStatus()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore reset sync: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore inaspettato: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}

/**
 * UI state for the sync settings screen.
 */
data class SyncSettingsUiState(
    val isLoading: Boolean = false,
    val syncStatus: SyncStatus? = null,
    val error: String? = null,
    val message: String? = null
)