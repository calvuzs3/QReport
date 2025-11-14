package net.calvuz.qreport.domain.model.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Referente del cliente
 * Solo firstName è obbligatorio come richiesto
 */
@Serializable
data class Contact(
    val id: String,
    val clientId: String,

    // ===== DATI PERSONALI =====
    val firstName: String,                 // ✅ OBBLIGATORIO (unico campo required)
    val lastName: String? = null,
    val title: String? = null,             // Ing., Dott., etc.

    // ===== RUOLO AZIENDALE =====
    val role: String? = null,              // Responsabile Manutenzione, etc.
    val department: String? = null,        // Produzione, Qualità, etc.

    // ===== CONTATTI =====
    val phone: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val alternativeEmail: String? = null,

    // ===== STATO =====
    val isPrimary: Boolean = false,        // Referente principale
    val isActive: Boolean = true,
    val preferredContactMethod: ContactMethod? = null,

    // ===== METADATI =====
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Nome completo formattato
     */
    val fullName: String
        get() {
            val prefix = if (!title.isNullOrBlank()) "$title " else ""
            val suffix = if (!lastName.isNullOrBlank()) " $lastName" else ""
            return "$prefix$firstName$suffix".trim()
        }

    /**
     * Descrizione ruolo completa
     */
    val roleDescription: String
        get() = when {
            !role.isNullOrBlank() && !department.isNullOrBlank() -> "$role - $department"
            !role.isNullOrBlank() -> role
            !department.isNullOrBlank() -> department
            else -> ""
        }

    /**
     * Contatto primario disponibile
     */
    val primaryContact: String?
        get() = when (preferredContactMethod) {
            ContactMethod.PHONE -> phone
            ContactMethod.MOBILE -> mobilePhone
            ContactMethod.EMAIL -> email
            null -> phone ?: mobilePhone ?: email
        }

    /**
     * Verifica se ha informazioni di contatto
     */
    fun hasContactInfo(): Boolean = !phone.isNullOrBlank() || !mobilePhone.isNullOrBlank() || !email.isNullOrBlank()

    /**
     * Verifica se il contatto è completo (ha nome e almeno un modo per essere contattato)
     */
    fun isComplete(): Boolean = firstName.isNotBlank() && hasContactInfo()
}

/**
 * Metodi di contatto preferiti
 */
@Serializable
enum class ContactMethod(val displayName: String) {
    PHONE("Telefono fisso"),
    MOBILE("Cellulare"),
    EMAIL("Email")
}