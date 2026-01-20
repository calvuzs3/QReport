package net.calvuz.qreport.ti.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.model.*
import net.calvuz.qreport.ti.domain.repository.SignatureFileRepository
import net.calvuz.qreport.ti.domain.usecase.GetTechnicalInterventionByIdUseCase
import net.calvuz.qreport.ti.domain.usecase.UpdateTechnicalInterventionUseCase
import net.calvuz.qreport.ti.presentation.model.InterventionFormStatus
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for SignaturesFormScreen
 * Manages technician and customer signatures with digital signature collection
 * Updated to use SignatureFileRepository following clean architecture
 */
@HiltViewModel
class SignaturesFormViewModel @Inject constructor(
    private val getTechnicalInterventionByIdUseCase: GetTechnicalInterventionByIdUseCase,
    private val updateTechnicalInterventionUseCase: UpdateTechnicalInterventionUseCase,
    private val signatureFileRepository: SignatureFileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SignaturesFormState())
    val state: StateFlow<SignaturesFormState> = _state.asStateFlow()

    private var currentInterventionId: String? = null
    private var currentIntervention: TechnicalIntervention? = null
    private var originalData: SignatureOriginalData? = null

    /**
     * Load signatures data from intervention
     */
    fun loadSignaturesData(interventionId: String) {
        if (currentInterventionId == interventionId) {
            Timber.d("Already loaded intervention $interventionId")
            return
        }

        Timber.d("Loading signatures data for intervention: $interventionId")
        currentInterventionId = interventionId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getTechnicalInterventionByIdUseCase(interventionId)) {
                is QrResult.Success -> {
                    if (result.data != null) {
                        Timber.d("Successfully loaded intervention: ${result.data.interventionNumber}")
                        currentIntervention = result.data
                        populateFormFromIntervention(result.data)
                    } else {
                        Timber.w("Intervention not found for ID: $interventionId")
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = QrError.InterventionError.NotFound()
                            )
                        }
                    }
                }

                is QrResult.Error -> {
                    Timber.e("Error loading intervention: ${result.error}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = QrError.InterventionError.LoadError()
                        )
                    }
                }
            }
        }
    }

    /**
     * Populate form fields from intervention signatures
     */
    private fun populateFormFromIntervention(intervention: TechnicalIntervention) {
        Timber.d("Populating form from intervention - " +
                "technicianSignature: ${intervention.technicianSignature?.name}, " +
                "customerSignature: ${intervention.customerSignature?.name}")

        // Store original data for dirty checking (including signature paths)
        originalData = SignatureOriginalData(
            technicianName = intervention.technicianSignature?.name ?: "",
            customerName = intervention.customerSignature?.name ?: "",
            isReadyForSignatures = deriveReadyStatusFromIntervention(intervention),
            technicianSignaturePath = intervention.technicianSignature?.signature ?: "",
            customerSignaturePath = intervention.customerSignature?.signature ?: ""
        )

        _state.update {
            it.copy(
                isLoading = false,
                technicianName = intervention.technicianSignature?.name ?: "",
                customerName = intervention.customerSignature?.name ?: "",
                isReadyForSignatures = deriveReadyStatusFromIntervention(intervention),
                technicianSignaturePath = intervention.technicianSignature?.signature ?: "",
                customerSignaturePath = intervention.customerSignature?.signature ?: "",
                isDirty = false, // Reset dirty flag after loading
                errorMessage = null
            )
        }

        Timber.d("Form populated successfully - isDirty: false, " +
                "hasDigitalSignatures: technician=${intervention.technicianSignature?.signature?.isNotEmpty()}, " +
                "customer=${intervention.customerSignature?.signature?.isNotEmpty()}")
    }

    /**
     * Derive ready for signatures status from intervention state
     */
    private fun deriveReadyStatusFromIntervention(intervention: TechnicalIntervention): Boolean {
        val isReady = intervention.isComplete && intervention.interventionDescription.isNotBlank()
        Timber.v("Ready status derived: isComplete=${intervention.isComplete}, " +
                "hasDescription=${intervention.interventionDescription.isNotBlank()}, " +
                "result=$isReady")
        return isReady
    }

    /**
     * Update technician name
     */
    fun updateTechnicianName(name: String) {
        Timber.v("Updating technician name: '$name'")
        _state.update {
            val newState = it.copy(technicianName = name)
            val isDirty = checkIfDirty(newState)
            Timber.v("Technician name updated - isDirty: $isDirty")
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Update customer name
     */
    fun updateCustomerName(name: String) {
        Timber.v("Updating customer name: '$name'")
        _state.update {
            val newState = it.copy(customerName = name)
            val isDirty = checkIfDirty(newState)
            Timber.v("Customer name updated - isDirty: $isDirty")
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Update ready for signatures status
     */
    fun updateReadyStatus(isReady: Boolean) {
        Timber.v("Updating ready status: $isReady")
        _state.update {
            val newState = it.copy(isReadyForSignatures = isReady)
            val isDirty = checkIfDirty(newState)
            Timber.v("Ready status updated - isDirty: $isDirty")
            newState.copy(isDirty = isDirty)
        }
    }

    /**
     * Show technician signature collection dialog
     */
    fun showTechnicianSignatureDialog() {
        _state.update {
            it.copy(showTechnicianSignatureDialog = true)
        }
        Timber.d("SignaturesFormViewModel: Showing technician signature dialog")
    }

    /**
     * Hide technician signature collection dialog
     */
    fun hideTechnicianSignatureDialog() {
        _state.update {
            it.copy(showTechnicianSignatureDialog = false)
        }
        Timber.d("SignaturesFormViewModel: Hiding technician signature dialog")
    }

    /**
     * Show customer signature collection dialog
     */
    fun showCustomerSignatureDialog() {
        _state.update {
            it.copy(showCustomerSignatureDialog = true)
        }
        Timber.d("SignaturesFormViewModel: Showing customer signature dialog")
    }

    /**
     * Hide customer signature collection dialog
     */
    fun hideCustomerSignatureDialog() {
        _state.update {
            it.copy(showCustomerSignatureDialog = false)
        }
        Timber.d("SignaturesFormViewModel: Hiding customer signature dialog")
    }

    /**
     * Collect technician digital signature
     */
    fun collectTechnicianSignature(signatureBitmap: androidx.compose.ui.graphics.ImageBitmap) {
        val interventionId = currentInterventionId
        if (interventionId == null) {
            Timber.e("SignaturesFormViewModel: Cannot collect signature - no intervention loaded")
            _state.update {
                it.copy(
                    errorMessage = QrError.InterventionError.NoInterventionLoaded(),
                    showTechnicianSignatureDialog = false
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isProcessingSignature = true,
                    errorMessage = null
                )
            }

            try {
                Timber.d("SignaturesFormViewModel: Processing technician signature")

                // Save signature to storage using new repository
                when (val result = signatureFileRepository.saveTechnicianSignature(interventionId, signatureBitmap)) {
                    is QrResult.Success -> {
                        val signatureFilePath = result.data
                        Timber.d("SignaturesFormViewModel: Technician signature saved at: $signatureFilePath")

                        // Update state with signature path
                        _state.update { currentState ->
                            val newState = currentState.copy(
                                technicianSignaturePath = signatureFilePath,
                                showTechnicianSignatureDialog = false,
                                isProcessingSignature = false
                            )
                            val isDirty = checkIfDirty(newState)
                            newState.copy(isDirty = isDirty)
                        }

                        // Show success message
                        _state.update {
                            it.copy(successMessage = InterventionFormStatus.Signature.TechnicianSignatureCollected())
                        }
                    }

                    is QrResult.Error -> {
                        Timber.e("SignaturesFormViewModel: Failed to save technician signature: ${result.error}")
                        _state.update {
                            it.copy(
                                isProcessingSignature = false,
                                showTechnicianSignatureDialog = false,
                                errorMessage = QrError.InterventionError.SignatureError.TechnicianSignatureFailed()
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "SignaturesFormViewModel: Exception during technician signature collection")
                _state.update {
                    it.copy(
                        isProcessingSignature = false,
                        showTechnicianSignatureDialog = false,
                        errorMessage = QrError.InterventionError.SignatureError.TechnicianSignatureFailed()
                    )
                }
            }
        }
    }

    /**
     * Collect customer digital signature
     */
    fun collectCustomerSignature(signatureBitmap: androidx.compose.ui.graphics.ImageBitmap) {
        val interventionId = currentInterventionId
        if (interventionId == null) {
            Timber.e("SignaturesFormViewModel: Cannot collect signature - no intervention loaded")
            _state.update {
                it.copy(
                    errorMessage = QrError.InterventionError.NoInterventionLoaded(),
                    showCustomerSignatureDialog = false
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isProcessingSignature = true,
                    errorMessage = null
                )
            }

            try {
                Timber.d("SignaturesFormViewModel: Processing customer signature")

                // Save signature to storage using new repository
                when (val result = signatureFileRepository.saveCustomerSignature(interventionId, signatureBitmap)) {
                    is QrResult.Success -> {
                        val signatureFilePath = result.data
                        Timber.d("SignaturesFormViewModel: Customer signature saved at: $signatureFilePath")

                        // Update state with signature path
                        _state.update { currentState ->
                            val newState = currentState.copy(
                                customerSignaturePath = signatureFilePath,
                                showCustomerSignatureDialog = false,
                                isProcessingSignature = false
                            )
                            val isDirty = checkIfDirty(newState)
                            newState.copy(isDirty = isDirty)
                        }

                        // Show success message
                        _state.update {
                            it.copy(successMessage = InterventionFormStatus.Signature.CustomerSignatureCollected())
                        }
                    }

                    is QrResult.Error -> {
                        Timber.e("SignaturesFormViewModel: Failed to save customer signature: ${result.error}")
                        _state.update {
                            it.copy(
                                isProcessingSignature = false,
                                showCustomerSignatureDialog = false,
                                errorMessage = QrError.InterventionError.SignatureError.CustomerSignatureFailed()
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "SignaturesFormViewModel: Exception during customer signature collection")
                _state.update {
                    it.copy(
                        isProcessingSignature = false,
                        showCustomerSignatureDialog = false,
                        errorMessage = QrError.InterventionError.SignatureError.CustomerSignatureFailed()
                    )
                }
            }
        }
    }

    /**
     * Check if current state differs from original data
     */
    private fun checkIfDirty(currentState: SignaturesFormState): Boolean {
        val original = originalData ?: return false

        val isDirty = currentState.technicianName != original.technicianName ||
                currentState.customerName != original.customerName ||
                currentState.isReadyForSignatures != original.isReadyForSignatures ||
                currentState.technicianSignaturePath != original.technicianSignaturePath ||
                currentState.customerSignaturePath != original.customerSignaturePath

        return isDirty
    }

    /**
     * Auto-save when leaving tab (called by parent EditInterventionScreen)
     * Returns success/failure for tab change decision
     */
    suspend fun autoSaveOnTabChange(): QrResult<Unit, QrError.InterventionError> {
        val currentState = _state.value
        val intervention = currentIntervention

        Timber.d("autoSaveOnTabChange: Called with isDirty=${currentState.isDirty}")

        if (!currentState.isDirty) {
            Timber.d("autoSaveOnTabChange: No changes to save")
            return QrResult.Success(Unit)
        }

        Timber.d("autoSaveOnTabChange: Starting auto-save, isDirty=${currentState.isDirty}")
        Timber.d("autoSaveOnTabChange: Current data - technician='${currentState.technicianName}', customer='${currentState.customerName}', ready=${currentState.isReadyForSignatures}")

        if (intervention == null) {
            Timber.e("autoSaveOnTabChange: No intervention loaded")
            val error = QrError.InterventionError.NoInterventionLoaded()
            _state.update { it.copy(errorMessage = error) }
            return QrResult.Error(error)
        }

        // Validate signatures before saving - USE PERMISSIVE VALIDATION FOR SAVE
        val validationErrors = validateForSave()
        if (validationErrors.isNotEmpty()) {
            Timber.w("autoSaveOnTabChange: Validation failed - ${validationErrors.size} errors:")
            validationErrors.forEach { error ->
                Timber.w("autoSaveOnTabChange: Validation error: $error")
            }
            val error = QrError.InterventionError.SignatureError.ValidationError(validationErrors)
            _state.update { it.copy(errorMessage = error) }
            return QrResult.Error(error)
        }

        Timber.d("autoSaveOnTabChange: Validation passed, proceeding with save")

        return try {
            _state.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }

            // Create signature objects with digital signature file paths
            val technicianSignature = if (currentState.technicianName.isNotBlank()) {
                TechnicianSignature(
                    name = currentState.technicianName,
                    signature = currentState.technicianSignaturePath  // Store file path
                )
            } else null

            val customerSignature = if (currentState.customerName.isNotBlank()) {
                CustomerSignature(
                    name = currentState.customerName,
                    signature = currentState.customerSignaturePath  // Store file path
                )
            } else null

            Timber.d("autoSaveOnTabChange: Creating signatures with digital files - " +
                    "technician: ${technicianSignature?.name} (${technicianSignature?.signature}), " +
                    "customer: ${customerSignature?.name} (${customerSignature?.signature})")

            // Update intervention with new signatures
            val updatedIntervention = intervention.copy(
                technicianSignature = technicianSignature,
                customerSignature = customerSignature
            )

            Timber.d("autoSaveOnTabChange: Calling updateTechnicalInterventionUseCase")
            when (val result = updateTechnicalInterventionUseCase(updatedIntervention)) {
                is QrResult.Success -> {
                    Timber.d("autoSaveOnTabChange: Save successful")
                    currentIntervention = updatedIntervention

                    // Update original data to current values
                    originalData = SignatureOriginalData(
                        technicianName = currentState.technicianName,
                        customerName = currentState.customerName,
                        isReadyForSignatures = currentState.isReadyForSignatures,
                        technicianSignaturePath = currentState.technicianSignaturePath,
                        customerSignaturePath = currentState.customerSignaturePath
                    )

                    _state.update {
                        it.copy(
                            isSaving = false,
                            isDirty = false, // Clear dirty flag after successful save
                            successMessage = InterventionFormStatus.Signature.SavedForm(),
                            errorMessage = null
                        )
                    }

                    QrResult.Success(Unit)
                }

                is QrResult.Error -> {
                    Timber.e("autoSaveOnTabChange: Save failed - ${result.error}")
                    val error = QrError.InterventionError.UpdateError()
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error
                        )
                    }
                    QrResult.Error(error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "autoSaveOnTabChange: Exception during save")
            val error = QrError.InterventionError.UpdateError()
            _state.update {
                it.copy(
                    isSaving = false,
                    errorMessage = error
                )
            }
            QrResult.Error(error)
        }
    }

    /**
     * Check if this tab has unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        val hasChanges = _state.value.isDirty
        Timber.v("hasUnsavedChanges: $hasChanges")
        return hasChanges
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Check if both signatures have names (ready for future digital signing)
     */
    fun areSignatureNamesComplete(): Boolean {
        val currentState = _state.value
        val isComplete = currentState.technicianName.isNotBlank() &&
                currentState.customerName.isNotBlank()
        Timber.v("areSignatureNamesComplete: $isComplete")
        return isComplete
    }

    /**
     * Validate signature data
     */
    fun validateSignatures(): List<QrError.InterventionError.SignatureError> {
        val currentState = _state.value
        val errors = mutableListOf<QrError.InterventionError.SignatureError>()

        Timber.d("Validating signatures - " +
                "technicianName: '${currentState.technicianName}', " +
                "customerName: '${currentState.customerName}', " +
                "isReady: ${currentState.isReadyForSignatures}")

        // RELAXED VALIDATION: Only validate if explicitly marked as ready AND both names provided
        if (currentState.isReadyForSignatures) {
            // Only require names if user explicitly marked as ready
            if (currentState.technicianName.isBlank()) {
                errors.add(QrError.InterventionError.SignatureError.TechnicianNameRequired)
            }

            if (currentState.customerName.isBlank()) {
                errors.add(QrError.InterventionError.SignatureError.ClientNameRequired)
            }
        }

        // Validate name formats only if names are provided (not blank)
        if (currentState.technicianName.isNotBlank() && currentState.technicianName.length < 2) {
            errors.add(QrError.InterventionError.SignatureError.TechnicianNameMinLength(2))
        }

        if (currentState.customerName.isNotBlank() && currentState.customerName.length < 2) {
            errors.add(QrError.InterventionError.SignatureError.ClientNameMinLength(2))
        }

        Timber.d("Validation completed - ${errors.size} errors found")
        return errors
    }

    /**
     * Alternative: Save-specific validation that's more permissive
     */
    private fun validateForSave(): List<QrError.InterventionError.SignatureError> {
        val currentState = _state.value
        val errors = mutableListOf<QrError.InterventionError.SignatureError>()

        Timber.d("Validating for save - " +
                "technicianName: '${currentState.technicianName}', " +
                "customerName: '${currentState.customerName}', " +
                "isReady: ${currentState.isReadyForSignatures}")

        // SAVE VALIDATION: More permissive - allow saving partial data
        // Only validate format if names are provided
        if (currentState.technicianName.isNotBlank() && currentState.technicianName.length < 2) {
            errors.add(QrError.InterventionError.SignatureError.TechnicianNameMinLength(2))
        }

        if (currentState.customerName.isNotBlank() && currentState.customerName.length < 2) {
            errors.add(QrError.InterventionError.SignatureError.ClientNameMinLength(2))
        }

        Timber.d("Save validation completed - ${errors.size} errors found")
        return errors
    }

    /**
     * Get signature completion status for UI feedback
     */
    fun getSignatureCompletionStatus(): InterventionFormStatus {
        val currentState = _state.value
        val intervention = currentIntervention

        val status = when {
            intervention == null -> InterventionFormStatus.Signature.NotReady()
            !intervention.isComplete -> InterventionFormStatus.Signature.InterventionIncomplete()
            intervention.interventionDescription.isBlank() -> InterventionFormStatus.Signature.MissingDescription()
            currentState.technicianName.isBlank() || currentState.customerName.isBlank() -> InterventionFormStatus.Signature.MissingNames()
            !currentState.isReadyForSignatures -> InterventionFormStatus.Signature.NotMarkedReady()
            else -> InterventionFormStatus.Signature.ReadyForDigitalSignatures()
        }

        Timber.v("getSignatureCompletionStatus: $status")
        return status
    }

    /**
     * Get current intervention data (for parent coordination)
     */
    fun getCurrentIntervention(): TechnicalIntervention? {
        return currentIntervention
    }

    /**
     * Force save without validation (for debugging/fallback)
     */
    suspend fun forceSave(): QrResult<Unit, QrError.InterventionError> {
        val currentState = _state.value
        val intervention = currentIntervention

        Timber.d("forceSave: Force saving without validation")

        if (intervention == null) {
            Timber.e("forceSave: No intervention loaded")
            val error = QrError.InterventionError.NoInterventionLoaded()
            return QrResult.Error(error)
        }

        return try {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            // Create signature objects if names are provided (no validation)
            val technicianSignature = if (currentState.technicianName.isNotBlank()) {
                TechnicianSignature(
                    name = currentState.technicianName,
                    signature = ""
                )
            } else null

            val customerSignature = if (currentState.customerName.isNotBlank()) {
                CustomerSignature(
                    name = currentState.customerName,
                    signature = ""
                )
            } else null

            val updatedIntervention = intervention.copy(
                technicianSignature = technicianSignature,
                customerSignature = customerSignature
            )

            when (val result = updateTechnicalInterventionUseCase(updatedIntervention)) {
                is QrResult.Success -> {
                    Timber.d("forceSave: Force save successful")
                    currentIntervention = updatedIntervention

                    // Update original data
                    originalData = SignatureOriginalData(
                        technicianName = currentState.technicianName,
                        customerName = currentState.customerName,
                        isReadyForSignatures = currentState.isReadyForSignatures
                    )

                    _state.update {
                        it.copy(
                            isSaving = false,
                            isDirty = false,
                            successMessage = InterventionFormStatus.Signature.SavedForm()
                        )
                    }

                    QrResult.Success(Unit)
                }

                is QrResult.Error -> {
                    Timber.e("forceSave: Force save failed - ${result.error}")
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = QrError.InterventionError.UpdateError()
                        )
                    }
                    QrResult.Error(QrError.InterventionError.UpdateError())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "forceSave: Exception during force save")
            _state.update {
                it.copy(
                    isSaving = false,
                    errorMessage = QrError.InterventionError.UpdateError()
                )
            }
            QrResult.Error(QrError.InterventionError.UpdateError())
        }
    }
}

/**
 * State for SignaturesFormScreen with digital signature support
 */
data class SignaturesFormState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,

    // Signature data
    val technicianName: String = "",
    val customerName: String = "",
    val isReadyForSignatures: Boolean = false,

    // Digital signature paths (stored file paths)
    val technicianSignaturePath: String = "",
    val customerSignaturePath: String = "",

    // Digital signature collection UI state
    val showTechnicianSignatureDialog: Boolean = false,
    val showCustomerSignatureDialog: Boolean = false,
    val isProcessingSignature: Boolean = false,

    val errorMessage: QrError.InterventionError? = null,
    val successMessage: InterventionFormStatus? = null
) {
    /**
     * Check if technician has digital signature
     */
    val hasTechnicianDigitalSignature: Boolean
        get() = technicianSignaturePath.isNotEmpty()

    /**
     * Check if customer has digital signature
     */
    val hasCustomerDigitalSignature: Boolean
        get() = customerSignaturePath.isNotEmpty()

    /**
     * Check if both signatures are digitally signed
     */
    val areBothSignaturesDigital: Boolean
        get() = hasTechnicianDigitalSignature && hasCustomerDigitalSignature
}

/**
 * Data class to store original values for dirty checking with digital signatures
 */
data class SignatureOriginalData(
    val technicianName: String,
    val customerName: String,
    val isReadyForSignatures: Boolean,
    val technicianSignaturePath: String = "",
    val customerSignaturePath: String = ""
)