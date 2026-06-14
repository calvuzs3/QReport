package net.calvuz.qreport.client.contact.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.client.contact.domain.model.ContactMethod

/** Extension function that return the display name of the contact method */
fun ContactMethod.getDisplayName(): UiText = when (this) {
    ContactMethod.PHONE -> UiText.StringResource(R.string.contact_method_phone)
    ContactMethod.MOBILE -> UiText.StringResource(R.string.contact_method_mobile)
    ContactMethod.EMAIL -> UiText.StringResource(R.string.contact_method_email)
}
