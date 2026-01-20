package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.usecase.GetTechnicalInterventionByIdUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for EditInterventionScreen
 * Manages tabs, auto-save logic and general intervention state
 */
@HiltViewModel
class EditInterventionViewModel @Inject constructor(
    private val getTechnicalInterventionByIdUseCase: GetTechnicalInterventionByIdUseCase,
    private val updateTechnicalInterventionUseCase: UpdateTechnicalInterventionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditInterventionState())
    val state: StateFlow<EditInterventionState> = _state.asStateFlow()

    private var currentIntervention: TechnicalIntervention? = null
    private var lastSavedState: TechnicalIntervention? = null

    /**
     * Load intervention data
     */
    fun loadIntervention(interventionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getTechnicalInterventionByIdUseCase(interventionId)) {
                is QrResult.Success -> {
                    if (result.data != null) {
                        currentIntervention = result.data
                        lastSavedState = result.data.copy() // Store for dirty comparison

                        _state.update {
                            it.copy(
                                isLoading = false,
                                interventionNumber = result.data.interventionNumber,
                                errorMessage = null
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = UiText.DynStr("Intervento non trovato")
                            )
                        }
                    }
                }
                is QrResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.DynStr("Errore nel caricamento: ${result.error}")
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
     * DEPRECATED: Trigger auto-save for current intervention data
     * Replaced by coordinated tab auto-save system
     */
    /*
    private fun triggerAutoSave() {
        currentIntervention?.let { intervention ->
            viewModelScope.launch {
                _state.update { it.copy(isAutoSaving = true) }

               when ( val result = updateTechnicalInterventionUseCase(intervention)) {
                  is QrResult.Success -> {
                   _state.update {
                       it.copy(
                           isAutoSaving = false,
                           successMessage = UiText.DynStr("Dati salvati automaticamente")
                       )
                   }
               }
                 is QrResult.Error -> {
                       _state.update {
                           it.copy(
                               isAutoSaving = false,
                               errorMessage = UiText.DynStr("Errore nel salvataggio: ${result.error}")
                           )
                       }
                   }
               }
            }
        }
    }
    */


    /**
     * Save all tabs - coordinates saving across all forms
     * This will trigger save operations in all child ViewModels
     */
    fun saveAllTabs() {
        Timber.d("Starting coordinated save of all tabs")

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                // The actual saving will be handled by each individual form ViewModel
                // This is more of a signal/coordinator

                // For now, we'll just update the current intervention if we have one
                // In a real implementation, you might want to collect data from all ViewModels
                val intervention = currentIntervention
                if (intervention != null) {

                    when (val result = updateTechnicalInterventionUseCase(intervention)) {
                        is QrResult.Success -> {
                            Timber.d( "Save successful")
                            lastSavedState = intervention.copy()
                            _state.update {
                                it.copy(
                                    isSaving = false,
                                    successMessage = UiText.DynStr("Tutti i dati salvati con successo")
                                )
                            }
                        }
                        is QrResult.Error ->  {
                            Timber.w( "Save failed: $result.error")
                            _state.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = UiText.DynStr("Errore nel salvataggio: ${result.error}")
                                )
                            }
                        }
                    }
                } else {
                    Timber.w( "No intervention to save")
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = UiText.DynStr("Nessun intervento da salvare")
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e( e, "saveAllTabs: Exception during save")
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = UiText.DynStr("Errore nel salvataggio: ${e.message}")
                    )
                }
            }
        }
    }

    /**
     * Update current intervention data (called by child forms)
     */
    fun updateCurrentIntervention(intervention: TechnicalIntervention) {
        currentIntervention = intervention
        // Check if intervention has changed from last saved state
        val hasChanges = lastSavedState?.let { saved ->
            !interventionsAreEqual(intervention, saved)
        } != false
    }

    /**
     * Compare two interventions to detect changes
     */
    private fun interventionsAreEqual(a: TechnicalIntervention, b: TechnicalIntervention): Boolean {
        // Basic comparison - in a real implementation you might want more sophisticated comparison
        return a.customerData == b.customerData &&
                a.robotData == b.robotData &&
                a.workLocation == b.workLocation &&
                a.technicians == b.technicians &&
                a.interventionDescription == b.interventionDescription &&
                a.materials == b.materials &&
                a.externalReport == b.externalReport &&
                a.workDays == b.workDays &&
                a.isComplete == b.isComplete &&
                a.technicianSignature == b.technicianSignature &&
                a.customerSignature == b.customerSignature
    }

    /**
     * Check if there are unsaved changes across all forms
     */
    fun hasUnsavedChanges(): Boolean {
        val current = currentIntervention
        val saved = lastSavedState

        if (current == null || saved == null) {
            return false
        }

        val hasChanges = !interventionsAreEqual(current, saved)
        Timber.w( "hasUnsavedChanges: $hasChanges")
        return hasChanges
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

    /**
     * Get current intervention (for child ViewModels)
     */
    fun getCurrentIntervention(): TechnicalIntervention? {
        return currentIntervention
    }

    /**
     * Manual save trigger (called by individual forms)
     */
    fun triggerSave(updatedIntervention: TechnicalIntervention) {
        Timber.d( "Manual save triggered by form")

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            when (val result = updateTechnicalInterventionUseCase(updatedIntervention)) {
                is QrResult.Success -> {
                    Timber.d("Save successful")
                    currentIntervention = updatedIntervention
                    lastSavedState = updatedIntervention.copy()

                    _state.update {
                        it.copy(
                            isSaving = false,
                            successMessage = UiText.DynStr("Dati salvati con successo")
                        )
                    }
                }
                is QrResult.Error -> {
                    Timber.w("triggerSave: Save failed ${result.error}")
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = UiText.DynStr("Errore nel salvataggio: ${result.error}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * State for EditInterventionScreen
 */
data class EditInterventionState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAutoSaving: Boolean = false,
    val selectedTabIndex: Int = 0,
    val interventionNumber: String = "",
    val shouldNavigateBack: Boolean = false,
    val errorMessage: UiText? = null,
    val successMessage: UiText? = null

)