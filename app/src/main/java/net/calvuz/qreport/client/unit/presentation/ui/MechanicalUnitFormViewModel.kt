package net.calvuz.qreport.client.unit.presentation.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.model.UnitType
import net.calvuz.qreport.client.unit.domain.usecase.CheckMechanicalUnitExistsUseCase
import net.calvuz.qreport.client.unit.domain.usecase.CreateMechanicalUnitUseCase
import net.calvuz.qreport.client.unit.domain.usecase.UpdateMechanicalUnitUseCase
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitFormState
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for [MechanicalUnitFormScreen] (add and edit).
 *
 * Navigation args expected in [SavedStateHandle]:
 * - "islandId"  — always required
 * - "unitId"    — null for new unit, non-null for editing
 */
@HiltViewModel
class MechanicalUnitFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createMechanicalUnitUseCase: CreateMechanicalUnitUseCase,
    private val updateMechanicalUnitUseCase: UpdateMechanicalUnitUseCase,
    private val checkMechanicalUnitExists: CheckMechanicalUnitExistsUseCase
) : ViewModel() {

    private val islandId: String = checkNotNull(savedStateHandle["islandId"])
    private val unitId: String? = savedStateHandle["unitId"]
    val isEditing: Boolean = unitId != null

    private val _state = MutableStateFlow(MechanicalUnitFormState())
    val state: StateFlow<MechanicalUnitFormState> = _state.asStateFlow()

    init {
        if (isEditing) loadExisting()
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    private fun loadExisting() {
        viewModelScope.launch {
            when (val result = checkMechanicalUnitExists(unitId!!)) {
                is QrResult.Success -> {
                    val unit = result.data
                    _state.update {
                        it.copy(
                            name = unit.name,
                            unitType = unit.unitType,
                            serialNumber = unit.serialNumber ?: "",
                            model = unit.model ?: "",
                            notes = unit.notes ?: ""
                        )
                    }
                }
                is QrResult.Error -> {
                    Timber.e("Failed to load unit for edit $unitId: ${result.error}")
                    _state.update {
                        it.copy(error = UiText.StringResource(R.string.err_unit_not_found))
                    }
                }
            }
        }
    }

    // =========================================================================
    // FORM EVENTS
    // =========================================================================

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onUnitTypeChange(v: UnitType) = _state.update { it.copy(unitType = v) }
    fun onSerialNumberChange(v: String) = _state.update { it.copy(serialNumber = v) }
    fun onModelChange(v: String) = _state.update { it.copy(model = v) }
    fun onNotesChange(v: String) = _state.update { it.copy(notes = v) }
    fun clearError() = _state.update { it.copy(error = null) }

    // =========================================================================
    // SAVE
    // =========================================================================

    fun save(onSuccess: () -> Unit) {
        _state.update { it.copy(showValidation = true) }
        if (!_state.value.isValid) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val now = Clock.System.now()
            val s = _state.value

            val unit = MechanicalUnit(
                id = unitId ?: UUID.randomUUID().toString(),
                islandId = islandId,
                unitType = s.unitType,
                name = s.name.trim(),
                serialNumber = s.serialNumber.trim().ifBlank { null },
                model = s.model.trim().ifBlank { null },
                notes = s.notes.trim().ifBlank { null },
                createdAt = now,    // Preserved in update by the use case
                updatedAt = now
            )

            val result = if (isEditing) updateMechanicalUnitUseCase(unit)
            else createMechanicalUnitUseCase(unit)

            when (result) {
                is QrResult.Success -> {
                    Timber.d("Unit saved: ${unit.id}")
                    onSuccess()
                }
                is QrResult.Error -> {
                    Timber.e("Failed to save unit: ${result.error}")
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = UiText.StringResource(
                                if (isEditing) R.string.err_unit_update else R.string.err_unit_create
                            )
                        )
                    }
                }
            }
        }
    }
}