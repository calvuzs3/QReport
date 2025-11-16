package net.calvuz.qreport.domain.usecase.client.contact

import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.ContactStatistics
import net.calvuz.qreport.domain.model.client.ContactMethod
import javax.inject.Inject

/**
 * Use case per calcolare statistiche dettagliate sui contatti di un cliente
 * Utilizzato dal ClientDetailScreen per la scheda Contacts
 *
 * Usa GetContactsByClientUseCase per seguire i principi Clean Architecture
 */
class GetContactStatisticsUseCase @Inject constructor(
    private val getContactsByClientUseCase: GetContactsByClientUseCase
) {

    suspend operator fun invoke(clientId: String): Result<ContactStatistics> = try {

        // Recupera tutti i contatti del cliente usando il use case specifico
        getContactsByClientUseCase(clientId).fold(
            onSuccess = { contacts ->
                if (contacts.isEmpty()) {
                    Result.success(ContactStatistics.empty())
                } else {
                    val statistics = calculateStatistics(contacts)
                    Result.success(statistics)
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )

    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun calculateStatistics(contacts: List<Contact>): ContactStatistics {
        val activeContacts = contacts.filter { it.isActive }
        val inactiveContacts = contacts.filter { !it.isActive }
        val primaryContacts = contacts.filter { it.isPrimary }

        // Metodi di contatto
        val contactsWithPhone = contacts.count { !it.phone.isNullOrBlank() }
        val contactsWithMobile = contacts.count { !it.mobilePhone.isNullOrBlank() }
        val contactsWithEmail = contacts.count { !it.email.isNullOrBlank() }
        val contactsWithoutContact = contacts.count {
            it.phone.isNullOrBlank() &&
                    it.mobilePhone.isNullOrBlank() &&
                    it.email.isNullOrBlank()
        }

        // Distribuzione per dipartimento (escludendo vuoti/null)
        val departmentDistribution = contacts
            .mapNotNull { it.department?.takeIf { dept -> dept.isNotBlank() } }
            .groupingBy { it }
            .eachCount()

        // Distribuzione per ruolo (escludendo vuoti/null)
        val roleDistribution = contacts
            .mapNotNull { it.role?.takeIf { role -> role.isNotBlank() } }
            .groupingBy { it }
            .eachCount()

        // Distribuzione metodi preferiti
        val preferredMethodDistribution = contacts
            .mapNotNull { contact ->
                when (contact.preferredContactMethod) {
                    ContactMethod.PHONE -> "Telefono"
                    ContactMethod.MOBILE -> "Cellulare"
                    ContactMethod.EMAIL -> "Email"
                    null -> null
                }
            }
            .groupingBy { it }
            .eachCount()

        // Completezza profili
        val completeProfiles = contacts.count { contact ->
            contact.firstName.isNotBlank() &&
                    !contact.lastName.isNullOrBlank() &&
                    !contact.role.isNullOrBlank() &&
                    ((!contact.phone.isNullOrBlank()) || (!contact.mobilePhone.isNullOrBlank()) || (!contact.email.isNullOrBlank()))
        }

        val incompleteProfiles = contacts.size - completeProfiles

        return ContactStatistics(
            totalContacts = contacts.size,
            activeContacts = activeContacts.size,
            inactiveContacts = inactiveContacts.size,
            primaryContacts = primaryContacts.size,
            contactsWithPhone = contactsWithPhone,
            contactsWithMobile = contactsWithMobile,
            contactsWithEmail = contactsWithEmail,
            contactsWithoutContact = contactsWithoutContact,
            departmentDistribution = departmentDistribution,
            roleDistribution = roleDistribution,
            preferredMethodDistribution = preferredMethodDistribution,
            completeProfiles = completeProfiles,
            incompleteProfiles = incompleteProfiles
        )
    }
}