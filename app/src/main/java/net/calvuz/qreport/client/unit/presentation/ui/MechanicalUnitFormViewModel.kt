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
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.model.UnitType
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import net.calvuz.qreport.client.unit.presentation.model.MechanicalUnitFormState
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
    private val repository: MechanicalUnitRepository
) : ViewModel() {

    private val islandId: String = checkNotNull(savedStateHandle["islandId"])
    private val unitId: String? = savedStateHandle["unitId"]
    val isEditing: Boolean = unitId != null

    private val _state = MutableStateFlow(MechanicalUnitFormState())
    val state: StateFlow<MechanicalUnitFormState> = _state.asStateFlow()

    init {
        if (isEditing) loadExisting()
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val unit = repository.getById(unitId!!) ?: return@launch
            _state.update {
                it.copy(
                    name         = unit.name,
                    unitType     = unit.unitType,
                    serialNumber = unit.serialNumber ?: "",
                    model        = unit.model ?: "",
                    notes        = unit.notes ?: ""
                )
            }
        }
    }

    fun onNameChange(v: String)         = _state.update { it.copy(name = v) }
    fun onUnitTypeChange(v: UnitType)   = _state.update { it.copy(unitType = v) }
    fun onSerialNumberChange(v: String) = _state.update { it.copy(serialNumber = v) }
    fun onModelChange(v: String)        = _state.update { it.copy(model = v) }
    fun onNotesChange(v: String)        = _state.update { it.copy(notes = v) }
    fun clearError()                    = _state.update { it.copy(error = null) }

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
                createdAt = now,
                updatedAt = now
            )

            val result = if (isEditing) repository.update(unit)
            else          repository.create(unit)

            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
            )
        }
    }
}
