package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.client.unit.domain.model.UnitType

data class MechanicalUnitFormState(
    val name: String = "",
    val unitType: UnitType = UnitType.ROBOT,
    val serialNumber: String = "",
    val model: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    // Validation flags — shown only after first save attempt
    val showValidation: Boolean = false
) {
    val isNameValid: Boolean get() = name.isNotBlank()
    val isValid: Boolean get() = isNameValid
}

