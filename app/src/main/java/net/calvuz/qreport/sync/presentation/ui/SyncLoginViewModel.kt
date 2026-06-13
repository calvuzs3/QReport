package net.calvuz.qreport.sync.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.sync.app.SyncEvent
import net.calvuz.qreport.sync.app.SyncEventBus
import net.calvuz.qreport.sync.data.local.TokenStorage
import net.calvuz.qreport.sync.domain.usecase.LoginUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SyncLoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val tokenStorage: TokenStorage,
    private val syncEventBus: SyncEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncLoginUiState())
    val uiState: StateFlow<SyncLoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)

            when (val result = loginUseCase(state.username, state.password)) {
                is QrResult.Success -> {
                    Timber.d("SyncLoginViewModel: login successful")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    syncEventBus.emit(SyncEvent.LoginSuccess)
                    onSuccess()
                }
                is QrResult.Error -> {
                    val message = when (result.error) {
                        is QrError.NetworkError.Unauthorized -> UiText.StringResource(R.string.sync_login_error_invalid_credentials)
                        is QrError.NetworkError.NoConnection -> UiText.StringResource(R.string.error_no_connection)
                        is QrError.NetworkError.ServerError -> UiText.StringResource(R.string.error_server)
                        is QrError.ValidationError.EmptyField -> UiText.StringResource(R.string.err_fields_required)
                        else -> UiText.StringResource(R.string.sync_login_error_generic)
                    }
                    Timber.e("SyncLoginViewModel: login failed: ${result.error}")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = message)
                }
            }
        }
    }

    fun logout() {
        tokenStorage.clearToken()
        Timber.d("SyncLoginViewModel: logged out")
    }
}

data class SyncLoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null
) {
    val isValid: Boolean get() = username.isNotBlank() && password.isNotBlank()
}