package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.asUiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.usecase.GetTechnicalInterventionByIdUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for EditInterventionScreen
 * Manages tabs and general intervention state
 */
@HiltViewModel
class EditInterventionViewModel @Inject constructor(
    private val getTechnicalInterventionByIdUseCase: GetTechnicalInterventionByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditInterventionState())
    val state: StateFlow<EditInterventionState> = _state.asStateFlow()

    /**
     * Load intervention data
     */
    fun loadIntervention(interventionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getTechnicalInterventionByIdUseCase(interventionId)) {
                is QrResult.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                interventionNumber = result.data.interventionNumber,
                                errorMessage = null
                            )
                        }
                }
                is QrResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.asUiText()
                        )
                    }
                }
            }
        }
    }

    /**
     * Select tab (auto-save coordination handled by EditInterventionScreen)
     */
    fun selectTab(tabIndex: Int) {
        Timber.d("EditInterventionViewModel: Selecting tab $tabIndex")
        _state.update {
            it.copy(selectedTabIndex = tabIndex)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }

    /**
     * Navigate back (auto-save handled by EditInterventionScreen)
     */
    fun navigateBack() {
        Timber.d("EditInterventionViewModel: Triggering navigation back")
        _state.update { it.copy(shouldNavigateBack = true) }
    }


    /**
     * Reset navigation back flag
     */
    fun clearNavigateBack() {
        _state.update { it.copy(shouldNavigateBack = false) }
    }
}

/**
 * State for EditInterventionScreen
 */
data class EditInterventionState(
    val isLoading: Boolean = false,
    val selectedTabIndex: Int = 0,
    val interventionNumber: String = "",
    val shouldNavigateBack: Boolean = false,
    val errorMessage: UiText? = null,
    val successMessage: UiText? = null
)