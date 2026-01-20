package net.calvuz.qreport.ti.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.error.presentation.UiText.StringResources

sealed interface InterventionFormStatus {

    /**
     * Enum for signature completion status
     */
    sealed interface Signature : InterventionFormStatus {
        data class SavedForm(val message: String? = null) : Signature
        data class NotReady(val message: String? = null) : Signature
        data class InterventionIncomplete(val message: String? = null) : Signature
        data class MissingDescription(val message: String? = null) : Signature
        data class MissingNames(val message: String? = null) : Signature
        data class NotMarkedReady(val message: String? = null) : Signature
        data class ReadyForDigitalSignatures(val message: String? = null) : Signature
        data class TechnicianSignatureCollected(val message: String? = null) : Signature
        data class TechnicianSignatureFailed(val message: String? = null) : Signature

        data class CustomerSignatureCollected(val message: String? = null) : Signature
        data class CustomerSignatureFailed(val message: String? = null) : Signature
    }
}

/**
 * Extension to get display text for signature status
 */
fun InterventionFormStatus.toUiText(): UiText {
    return when (this) {
        is InterventionFormStatus.Signature -> this.toUiText()
    }
}

fun InterventionFormStatus.Signature.toUiText(): UiText {
    return when (this) {
        is InterventionFormStatus.Signature.SavedForm -> StringResources(R.string.IntFormStatus_saved)
        is InterventionFormStatus.Signature.NotReady -> StringResources(R.string.form_signature_not_ready)
        is InterventionFormStatus.Signature.InterventionIncomplete -> StringResources(R.string.form_signature_intervention_Incomplete)
        is InterventionFormStatus.Signature.MissingDescription -> StringResources(R.string.form_signature_missing_desc)
        is InterventionFormStatus.Signature.MissingNames -> StringResources(R.string.form_signature_missing_names)
        is InterventionFormStatus.Signature.NotMarkedReady -> StringResources(R.string.form_signature_not_marked_ready)
        is InterventionFormStatus.Signature.ReadyForDigitalSignatures -> StringResources(R.string.form_signature_ready)
        is InterventionFormStatus.Signature.TechnicianSignatureCollected -> StringResources(R.string.form_signature_technician_signature_collected)
        is InterventionFormStatus.Signature.TechnicianSignatureFailed -> StringResources(R.string.form_signature_technician_signature_failed)
        is InterventionFormStatus.Signature.CustomerSignatureCollected -> StringResources(R.string.form_signature_customer_signature_collected)
        is InterventionFormStatus.Signature.CustomerSignatureFailed -> StringResources(R.string.form_signature_customer_signature_failed)
    }
}
