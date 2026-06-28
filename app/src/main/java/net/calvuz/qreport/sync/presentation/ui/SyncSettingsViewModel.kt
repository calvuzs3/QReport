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
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.sync.app.SyncEvent
import net.calvuz.qreport.sync.app.SyncEventBus
import net.calvuz.qreport.sync.data.local.TokenStorage
import net.calvuz.qreport.sync.data.remote.ServerUrlHolder
import net.calvuz.qreport.sync.domain.model.SyncMode
import net.calvuz.qreport.sync.domain.model.SyncResult
import net.calvuz.qreport.sync.domain.model.SyncStatus
import net.calvuz.qreport.sync.domain.repository.SyncRepository
import net.calvuz.qreport.sync.domain.usecase.SyncUseCase
import javax.inject.Inject

@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val syncUseCase: SyncUseCase,
    private val tokenStorage: TokenStorage,
    private val serverUrlHolder: ServerUrlHolder,
    private val syncEventBus: SyncEventBus
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
        observeSyncEvents()
    }

    /**
     * Load the full sync status snapshot (pending count + device id).
     */
    fun loadSyncStatus() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    isLoggedIn = tokenStorage.isLoggedIn(),
                    canPushMasterData = tokenStorage.canPushMasterData()
                )
                val result = syncRepository.getSyncStatus()
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        syncStatus = result.getOrNull()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UiText.StringResources(R.string.sync_settings_error_load_status, result.exceptionOrNull()?.message ?: "")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiText.StringResources(R.string.sync_settings_error_unexpected, e.message ?: "")
                )
            }
        }
    }

    /**
     * Toggle sync mode between [SyncMode.LOCAL_ONLY] and [SyncMode.REMOTE_ENABLED].
     */
    fun toggleSyncMode() {
        val next = when (syncMode.value) {
            SyncMode.LOCAL_ONLY -> SyncMode.REMOTE_ENABLED
            SyncMode.REMOTE_ENABLED -> SyncMode.LOCAL_ONLY
        }
        viewModelScope.launch {
            syncRepository.setSyncMode(next)
        }
    }

//    /**
//     * Explicitly set the sync mode.
//     */
//    fun setSyncMode(mode: SyncMode) {
//        viewModelScope.launch {
//            try {
//                val result = syncRepository.setSyncMode(mode)
//
//                if (result.isFailure) {
//                    _uiState.value = _uiState.value.copy(
//                        error = "Errore aggiornamento modalità sync: ${result.exceptionOrNull()?.message}"
//                    )
//                }
//            } catch (e: Exception) {
//                _uiState.value = _uiState.value.copy(
//                    error = "Errore inaspettato: ${e.message}"
//                )
//            }
//        }
//    }

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            val result = syncRepository.setServerUrl(url)
            if (result.isSuccess) {
                // Update in-memory holder immediately
                val normalized = if (url.endsWith("/")) url else "$url/"
                serverUrlHolder.baseUrl = normalized
            } else {
                _uiState.value = _uiState.value.copy(
                    error = UiText.StringResources(R.string.sync_settings_error_save_url, result.exceptionOrNull()?.message ?: "")
                )
            }
        }
    }

//    /**
//     * Clear sync state (timestamps and mode). Used when logging out from the server.
//     */
//    fun clearSyncState() {
//        viewModelScope.launch {
//            try {
//                _uiState.value = _uiState.value.copy(isLoading = true)
//
//                val result = syncRepository.clearSyncState()
//
//                if (result.isSuccess) {
//                    _uiState.value = _uiState.value.copy(
//                        isLoading = false,
//                        message = "Stato sincronizzazione azzerato"
//                    )
//                    loadSyncStatus()
//                } else {
//                    _uiState.value = _uiState.value.copy(
//                        isLoading = false,
//                        error = "Errore reset sync: ${result.exceptionOrNull()?.message}"
//                    )
//                }
//            } catch (e: Exception) {
//                _uiState.value = _uiState.value.copy(
//                    isLoading = false,
//                    error = "Errore inaspettato: ${e.message}"
//                )
//            }
//        }
//    }

    /**
     * Triggers a full bidirectional sync session.
     */
    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null, syncResult = null)

            when (val result = syncUseCase()) {
                is QrResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncResult = result.data,
                        message = UiText.StringResources(
                            R.string.sync_settings_message_sync_completed,
                            result.data.pushedCount,
                            result.data.pulledCount
                        )
                    )
                    loadSyncStatus() // Refresh pending count
                }
                is QrResult.Error -> {
                    val message = when (val err = result.error) {
                        is QrError.NetworkError.Unauthorized -> {
                            // Token expired — force re-login
                            tokenStorage.clearToken()
                            tokenStorage.clearRole()
                            _uiState.value = _uiState.value.copy(isLoggedIn = false, canPushMasterData = false)
                            UiText.StringResource(R.string.sync_error_session_expired)
                        }
                        is QrError.NetworkError.ServerVersionIncompatible ->
                            UiText.StringResources(
                                R.string.sync_error_server_version_incompatible,
                                err.serverVersion,
                                err.minVersion
                            )
                        is QrError.NetworkError.NoConnection -> UiText.StringResource(R.string.error_no_connection)
                        is QrError.NetworkError.SyncDisabled -> UiText.StringResource(R.string.sync_error_sync_disabled)
                        is QrError.NetworkError.ServerError -> UiText.StringResource(R.string.error_server)
                        else -> UiText.StringResource(R.string.sync_error_generic)
                    }
                    _uiState.value = _uiState.value.copy(isSyncing = false, error = message)
                }
            }
        }
    }

    /**
     * Resets the sync timestamp to 0 and triggers a full pull from the server.
     * Use after login on a new device or after reinstall.
     */
    fun triggerFullSync() {
        viewModelScope.launch {
            syncRepository.resetLastSyncTimestamp()
            executeSyncUseCase()
        }
    }

    fun onLoginSuccess() {
        _uiState.value = _uiState.value.copy(isLoggedIn = true)
    }

    fun logout() {
        tokenStorage.clearToken()
        tokenStorage.clearRole()
        _uiState.value = _uiState.value.copy(isLoggedIn = false, canPushMasterData = false, syncResult = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }

    // ===== PRIVATE =====

    private fun observeSyncEvents() {
        viewModelScope.launch {
            syncEventBus.events.collect { event ->
                when (event) {
                    is SyncEvent.LoginSuccess -> onLoginSuccess()
                    is SyncEvent.LoggedOut -> { /* già gestito da logout() */ }
                }
            }
        }
    }

    private suspend fun executeSyncUseCase() {
        _uiState.value = _uiState.value.copy(isSyncing = true, error = null, syncResult = null)

        when (val result = syncUseCase()) {
            is QrResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = result.data,
                    message = UiText.StringResources(
                        R.string.sync_settings_message_sync_completed,
                        result.data.pushedCount,
                        result.data.pulledCount
                    )
                )
                loadSyncStatus()
            }
            is QrResult.Error -> {
                val message = when (val err = result.error) {
                    is QrError.NetworkError.Unauthorized -> {
                        tokenStorage.clearToken()
                        tokenStorage.clearRole()
                        _uiState.value = _uiState.value.copy(isLoggedIn = false, canPushMasterData = false)
                        UiText.StringResource(R.string.sync_error_session_expired)
                    }
                    is QrError.NetworkError.ServerVersionIncompatible ->
                        UiText.StringResources(
                            R.string.sync_error_server_version_incompatible,
                            err.serverVersion,
                            err.minVersion
                        )
                    is QrError.NetworkError.NoConnection -> UiText.StringResource(R.string.error_no_connection)
                    is QrError.NetworkError.SyncDisabled -> UiText.StringResource(R.string.sync_error_sync_disabled)
                    is QrError.NetworkError.ServerError -> UiText.StringResource(R.string.error_server)
                    else -> UiText.StringResource(R.string.sync_error_generic)
                }
                _uiState.value = _uiState.value.copy(isSyncing = false, error = message)
            }
        }
    }
}

/**
 * UI state for the sync settings screen.
 */
data class SyncSettingsUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isLoggedIn: Boolean = false,
    val canPushMasterData: Boolean = false,
    val syncStatus: SyncStatus? = null,
    val syncResult: SyncResult? = null,
    val error: UiText? = null,
    val message: UiText? = null
)