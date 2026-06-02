package net.calvuz.qreport.client.contact.presentation.model

import net.calvuz.qreport.client.contact.domain.model.ContactMethod

/** Extension function that return the display name of the contact method */
fun ContactMethod.getDisplayName() = when (this) {
        ContactMethod.PHONE -> "Tel fisso"
        ContactMethod.MOBILE -> "Cellulare"
        ContactMethod.EMAIL -> "Email"
    }
