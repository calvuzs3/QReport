package net.calvuz.qreport.domain.usecase.client.client

import net.calvuz.qreport.domain.model.client.Client
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.repository.ClientRepository
import net.calvuz.qreport.domain.repository.ContactRepository
import net.calvuz.qreport.domain.repository.FacilityRepository
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import net.calvuz.qreport.domain.usecase.client.contact.GetContactsByClientUseCase
import net.calvuz.qreport.domain.usecase.client.facility.GetFacilitiesByClientUseCase
import net.calvuz.qreport.domain.usecase.client.facilityisland.GetFacilityIslandsByFacilityUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Use Case per recuperare un cliente con tutti i suoi dettagli correlati
 *
 * Aggrega:
 * - Dati cliente base
 * - Lista facilities associate
 * - Lista contacts associati
 * - Lista islands per facility
 * - Statistiche aggregate
 */
class GetClientWithDetailsUseCase @Inject constructor(
    private val getClientByIdUseCase: GetClientByIdUseCase,
    private val clientRepository: ClientRepository,
    private val getFacilitiesByClientUseCase: GetFacilitiesByClientUseCase,
    private val facilityRepository: FacilityRepository,
    private val getContactsByClientUseCase :GetContactsByClientUseCase,
    private val contactRepository: ContactRepository,
    private val getFacilityIslandsByFacilityUseCase: GetFacilityIslandsByFacilityUseCase,
    private val facilityIslandRepository: FacilityIslandRepository
) {

    /**
     * Recupera cliente con dettagli completi
     *
     * @param clientId ID del cliente da recuperare
     * @return Result con ClientWithDetails se successo, errore se fallimento
     */
    suspend operator fun invoke(clientId: String): Result<ClientWithDetails> {
        return try {
            Timber.d("Loading client details for ID: $clientId")

            // 1. Validazione ID
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente non può essere vuoto"))
            }

            // 2. Recupero cliente base
            val client = getClientByIdUseCase(clientId)
                .getOrNull() ?: return Result.failure(
                NoSuchElementException("Cliente con ID '$clientId' non trovato")
            )
//            val client = clientRepository.getClientById(clientId)
//                .getOrNull() ?: return Result.failure(
//                NoSuchElementException("Cliente con ID '$clientId' non trovato")
//            )

            Timber.d("Client found: ${client.companyName}")

            // 3. Recupero facilities associate
            val facilities = getFacilitiesByClientUseCase(clientId)
                .getOrElse {
                    Timber.w("Failed to load facilities for client $clientId: $it")
                    emptyList()
                }
//            val facilities = facilityRepository.getFacilitiesForClient(clientId)
//                .getOrElse {
//                    Timber.w("Failed to load facilities for client $clientId: $it")
//                    emptyList()
//                }

            Timber.d("Found ${facilities.size} facilities")

            // 4. Recupero contacts associati
            val contacts = getContactsByClientUseCase(clientId)
                .getOrElse {
                    Timber.w("Failed to load contacts for client $clientId: $it")
                    emptyList()
                }
//            // 4. Recupero contacts associati
//            val contacts = contactRepository.getContactsForClient(clientId)
//                .getOrElse {
//                    Timber.w("Failed to load contacts for client $clientId: $it")
//                    emptyList()
//                }

            Timber.d("Found ${contacts.size} contacts")

            // 5. Recupero islands per ogni facility
            val facilitiesWithIslands = facilities.map { facility ->
                val islands = getFacilityIslandsByFacilityUseCase(facility.id)
                    .getOrElse {
                        Timber.w("Failed to load islands for facility ${facility.id}: $it")
                        emptyList()
                    }
//                val islands = facilityIslandRepository.getIslandsForFacility(facility.id)
//                    .getOrElse {
//                        Timber.w("Failed to load islands for facility ${facility.id}: $it")
//                        emptyList()
//                    }

                FacilityWithIslands(
                    facility = facility,
                    islands = islands
                )
            }

            // 6. Calcolo statistiche aggregate
            val totalIslands = facilitiesWithIslands.sumOf { it.islands.size }
            val stats = SingleClientStatistics(
                facilitiesCount = facilities.size,
                islandsCount = totalIslands,
                contactsCount = contacts.size,
                totalCheckUps = 0, // TODO: Implementare quando integrato con CheckUp
                completedCheckUps = 0, // TODO: Implementare quando integrato con CheckUp
                lastCheckUpDate = client.updatedAt // TODO: Implementare quando integrato con CheckUp
            )

            // 7. Costruzione risultato finale
            val result = ClientWithDetails(
                client = client,
                facilities = facilitiesWithIslands,
                contacts = contacts,
                statistics = stats,
                // Campi aggiuntivi per UI convenience
                primaryContact = contacts.find { it.isPrimary },
                primaryFacility = facilitiesWithIslands.find { it.facility.isPrimary }?.facility,
                totalCheckUps = 0, // TODO: Implementare quando integrato
                lastCheckUpDate = null // TODO: Implementare quando integrato
            )

            Timber.d("Successfully loaded client details with ${stats.facilitiesCount} facilities, ${stats.contactsCount} contacts, ${stats.islandsCount} islands")

            Result.success(result)

        } catch (e: Exception) {
            Timber.e(e, "Error loading client details for ID: $clientId")
            Result.failure(e)
        }
    }
}

/**
 * Data class che aggrega tutti i dati del cliente per la detail view
 */
data class ClientWithDetails(
    val client: Client,
    val facilities: List<FacilityWithIslands>,
    val contacts: List<Contact>,
    val statistics: SingleClientStatistics,

    // Convenience fields per UI
    val primaryContact: Contact? = null,
    val primaryFacility: Facility? = null,
    val totalCheckUps: Int = 0,
    val lastCheckUpDate: kotlinx.datetime.Instant? = null
) {

    /**
     * Facilities attive
     */
    val activeFacilities: List<FacilityWithIslands>
        get() = facilities.filter { it.facility.isActive }

    /**
     * Contacts attivi
     */
    val activeContacts: List<Contact>
        get() = contacts.filter { it.isActive }

    /**
     * Islands attive totali
     */
    val activeIslands: List<FacilityIsland>
        get() = facilities.flatMap { facilityWithIslands ->
            facilityWithIslands.islands.filter { it.isActive }
        }

    /**
     * Verifica se ha dati completi per operatività
     */
    fun isFullyOperational(): Boolean =
        client.isActive &&
                activeFacilities.isNotEmpty() &&
                activeContacts.isNotEmpty() &&
                activeIslands.isNotEmpty()

    /**
     * Messaggio di stato per UI
     */
    val statusMessage: String
        get() = when {
            !client.isActive -> "Cliente inattivo"
            activeFacilities.isEmpty() -> "Nessun stabilimento configurato"
            activeContacts.isEmpty() -> "Nessun referente configurato"
            activeIslands.isEmpty() -> "Nessuna isola robotizzata configurata"
            else -> "Cliente completamente operativo"
        }
}

/**
 * Facility con le sue islands associate
 */
data class FacilityWithIslands(
    val facility: Facility,
    val islands: List<FacilityIsland>
) {

    /**
     * Islands attive
     */
    val activeIslands: List<FacilityIsland>
        get() = islands.filter { it.isActive }

    /**
     * Islands che necessitano manutenzione
     */
    val islandsNeedingMaintenance: List<FacilityIsland>
        get() = activeIslands.filter { it.needsMaintenance() }
}

///**
// * Statistiche aggregate del cliente
// */
//data class ClientStatistics(
//    val totalFacilities: Int,
//    val totalIslands: Int,
//    val totalContacts: Int,
//    val checkUpsThisYear: Int,
//    val lastActivity: kotlinx.datetime.Instant?
//) {
//
//    /**
//     * Testo riassuntivo per UI
//     */
//    val summaryText: String
//        get() = buildString {
//            val parts = mutableListOf<String>()
//
//            if (totalFacilities > 0) {
//                parts.add("$totalFacilities stabiliment${if (totalFacilities == 1) "o" else "i"}")
//            }
//
//            if (totalIslands > 0) {
//                parts.add("$totalIslands isol${if (totalIslands == 1) "a" else "e"}")
//            }
//
//            if (totalContacts > 0) {
//                parts.add("$totalContacts referent${if (totalContacts == 1) "e" else "i"}")
//            }
//
//            when {
//                parts.isEmpty() -> append("Nessun dato configurato")
//                parts.size == 1 -> append(parts.first())
//                parts.size == 2 -> append("${parts[0]} e ${parts[1]}")
//                else -> append("${parts.dropLast(1).joinToString(", ")} e ${parts.last()}")
//            }
//        }
//}