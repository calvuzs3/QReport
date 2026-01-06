package net.calvuz.qreport.share.presentation.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.backup.domain.usecase.ShareBackupUseCase
import net.calvuz.qreport.share.domain.repository.ShareIntentResult
import net.calvuz.qreport.share.domain.repository.ShareMethod
import net.calvuz.qreport.share.domain.repository.ShareOptionOldVersion
import net.calvuz.qreport.share.domain.repository.ShareOptionType
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShareBackupViewModel @Inject constructor(
    private val shareBackupUseCase: ShareBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareBackupUiState())
    val uiState: StateFlow<ShareBackupUiState> = _uiState.asStateFlow()


    /**
     * Carica opzioni di condivisione disponibili
     */
    fun loadShareOptions(backupPath: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val optionsResult = shareBackupUseCase.getAvailableShareOptions(backupPath)

                if (optionsResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        shareOptionOldVersions = optionsResult.getOrThrow()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore caricamento opzioni: ${optionsResult.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "Error loading share options")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore inaspettato: ${e.message}"
                )
            }
        }
    }

    /**
     * Condividi backup con opzione selezionata
     */
    fun shareBackup(
        backupPath: String,
        option: ShareOptionOldVersion,
        onIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSharing = true, error = null)

                val shareResult = shareBackupUseCase(
                    backupPath = backupPath,
                    shareMethod = option.shareMethod,
                    targetApp = option.targetPackage
                )

                if (shareResult.isSuccess) {
                    val result = shareResult.getOrThrow()
                    onIntentReady(result.intent)

                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        lastShareIntentResult = result
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        error = "Errore condivisione: ${shareResult.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "Error sharing backup")
                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    error = "Errore inaspettato: ${e.message}"
                )
            }
        }
    }

    /**
     * Quick share (default behavior)
     */
    fun quickShare(
        backupPath: String,
        onIntentReady: (Intent) -> Unit
    ) {
        shareBackup(
            backupPath = backupPath,
            option = ShareOptionOldVersion(
                type = ShareOptionType.FILE_OPTION,
                title = "Condividi",
                subtitle = "Condividi backup",
                icon = null,
                shareMethod = ShareMethod.DIRECT
            ),
            onIntentReady = onIntentReady
        )
    }

    /**
     * Pulisci errori
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State per sharing backup
 */
data class ShareBackupUiState(
    val isLoading: Boolean = false,
    val isSharing: Boolean = false,
    val shareOptionOldVersions: List<ShareOptionOldVersion> = emptyList(),
    val lastShareIntentResult: ShareIntentResult? = null,
    val error: String? = null
)