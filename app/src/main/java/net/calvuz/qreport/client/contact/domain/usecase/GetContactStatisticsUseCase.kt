package net.calvuz.qreport.client.contact.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactStatistics
import net.calvuz.qreport.client.contact.domain.model.ContactMethod
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case per calcolare statistiche dettagliate sui contatti di un cliente
 * Utilizzato dal ClientDetailScreen per la scheda Contacts
 *
 * Usa GetContactsByClientUseCase per seguire i principi Clean Architecture
 *
 * Updated to use QrResult<ContactStatistics, QrError> pattern
 */
class GetContactStatisticsUseCase @Inject constructor(
    private val getContactsByClientUseCase: GetContactsByClientUseCase
) {

    /**
     * Calcola le statistiche dei contatti per un cliente
     *
     * @param clientId ID del cliente
     * @return QrResult.Success con ContactStatistics, QrResult.Error per errori
     */
    suspend operator fun invoke(clientId: String): QrResult<ContactStatistics, QrError> {
        return try {
            // Validazione input
            if (clientId.isBlank()) {
                Timber.w("GetContactStatisticsUseCase: clientId is blank")
                return QrResult.Error(QrError.ValidationError.EmptyField(clientId.toString()))
            }

            Timber.d("GetContactStatisticsUseCase: Calculating statistics for client: $clientId")

            // Recupera tutti i contatti del cliente usando il use case specifico
            when (val contactsResult = getContactsByClientUseCase(clientId)) {
                is QrResult.Success -> {
                    val contacts = contactsResult.data

                    val statistics = if (contacts.isEmpty()) {
                        Timber.d("GetContactStatisticsUseCase: No contacts found for client $clientId, returning empty statistics")
                        ContactStatistics.empty()
                    } else {
                        Timber.d("GetContactStatisticsUseCase: Calculating statistics for ${contacts.size} contacts")
                        calculateStatistics(contacts)
                    }

                    QrResult.Success(statistics)
                }

                is QrResult.Error -> {
                    Timber.e("GetContactStatisticsUseCase: Error getting contacts for client $clientId: ${contactsResult.error}")
                    QrResult.Error(contactsResult.error)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "GetContactStatisticsUseCase: Exception calculating statistics for client: $clientId")
            QrResult.Error(QrError.SystemError.Unknown())
        }
    }

    /**
     * Calcola statistiche dettagliate sui contatti
     */
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

        Timber.d("GetContactStatisticsUseCase: Statistics calculated - Total: ${contacts.size}, Active: ${activeContacts.size}, Complete profiles: $completeProfiles")

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