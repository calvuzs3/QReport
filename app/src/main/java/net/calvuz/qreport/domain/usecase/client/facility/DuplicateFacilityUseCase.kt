package net.calvuz.qreport.domain.usecase.client.facility

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.repository.FacilityRepository
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case per duplicazione di uno stabilimento esistente
 *
 * Gestisce:
 * - Validazione facility sorgente
 * - Creazione copia con nuovi ID e timestamp
 * - Gestione nomi duplicati
 * - Esclusione flag primario dalla copia
 * - Opzioni di personalizzazione copia
 */
class DuplicateFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {

    /**
     * Duplica uno stabilimento esistente
     *
     * @param sourceFacilityId ID della facility da duplicare
     * @param newName Nuovo nome per la copia (opzionale, altrimenti auto-generato)
     * @param duplicateOptions Opzioni di duplicazione
     * @return Result con ID della facility duplicata
     */
    suspend operator fun invoke(
        sourceFacilityId: String,
        newName: String? = null,
        duplicateOptions: DuplicateOptions = DuplicateOptions()
    ): Result<String> {
        return try {
            // 1. Validazione input
            if (sourceFacilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility sorgente è obbligatorio"))
            }

            // 2. Recupero facility sorgente
            val sourceFacility = facilityRepository.getFacilityById(sourceFacilityId)
                .getOrElse { return Result.failure(it) }
                ?: return Result.failure(NoSuchElementException("Facility sorgente non trovata"))

            // 3. Generazione nome per la copia
            val copyName = generateCopyName(sourceFacility, newName)
                .getOrElse { return Result.failure(it) }

            // 4. Creazione facility duplicata
            val duplicatedFacility = createDuplicatedFacility(
                sourceFacility = sourceFacility,
                copyName = copyName,
                options = duplicateOptions
            )

            // 5. Validazione unicità nome
            validateNameUniqueness(duplicatedFacility)
                .onFailure { return Result.failure(it) }

            // 6. Salvataggio
            facilityRepository.createFacility(duplicatedFacility)
                .onFailure { return Result.failure(it) }

            Result.success(duplicatedFacility.id)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Genera nome per la copia
     */
    private suspend fun generateCopyName(
        sourceFacility: Facility,
        customName: String?
    ): Result<String> {
        return try {
            if (customName != null) {
                if (customName.isBlank()) {
                    return Result.failure(IllegalArgumentException("Nome personalizzato non può essere vuoto"))
                }
                Result.success(customName.trim())
            } else {
                // Auto-genera nome con suffisso incrementale
                generateIncrementalName(sourceFacility)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Genera nome incrementale per evitare duplicati
     */
    private suspend fun generateIncrementalName(sourceFacility: Facility): Result<String> {
        return try {
            val baseName = sourceFacility.name
            val clientId = sourceFacility.clientId

            // Cerca nome disponibile con suffisso incrementale
            var counter = 1
            var candidateName: String

            do {
                candidateName = "$baseName (Copia $counter)"
                val isTaken = facilityRepository.isFacilityNameTakenForClient(
                    clientId = clientId,
                    name = candidateName
                ).getOrThrow()

                if (!isTaken) {
                    return Result.success(candidateName)
                }

                counter++
            } while (counter <= 999) // Limite di sicurezza

            Result.failure(IllegalStateException("Impossibile generare nome univoco per la copia"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea la facility duplicata con nuovi dati
     */
    private fun createDuplicatedFacility(
        sourceFacility: Facility,
        copyName: String,
        options: DuplicateOptions
    ): Facility {
        val now = Clock.System.now()

        return sourceFacility.copy(
            id = UUID.randomUUID().toString(),
            name = copyName,
            code = if (options.duplicateCode) {
                generateCopyCode(sourceFacility.code)
            } else null,
            description = if (options.duplicateDescription) {
                addCopyNoteToDescription(sourceFacility.description)
            } else null,
            isPrimary = false, // Le copie non sono mai primarie
            isActive = options.activateImmediately,
            islands = emptyList(), // Le isole non vengono duplicate automaticamente
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Genera codice per la copia
     */
    private fun generateCopyCode(originalCode: String?): String? {
        if (originalCode.isNullOrBlank()) return null
        return "${originalCode}_COPY"
    }

    /**
     * Aggiunge nota di copia alla descrizione
     */
    private fun addCopyNoteToDescription(originalDescription: String?): String? {
        val copyNote = "Copia creata il ${Clock.System.now()}"
        return if (originalDescription.isNullOrBlank()) {
            copyNote
        } else {
            "$originalDescription\n\n$copyNote"
        }
    }

    /**
     * Validazione unicità nome
     */
    private suspend fun validateNameUniqueness(facility: Facility): Result<Unit> {
        return facilityRepository.isFacilityNameTakenForClient(
            clientId = facility.clientId,
            name = facility.name
        ).mapCatching { isTaken ->
            if (isTaken) {
                throw IllegalArgumentException("Nome '${facility.name}' già utilizzato per questo cliente")
            }
        }
    }

    /**
     * Duplica facility in un cliente diverso (cross-client copy)
     */
    suspend fun duplicateToAnotherClient(
        sourceFacilityId: String,
        targetClientId: String,
        newName: String? = null,
        duplicateOptions: DuplicateOptions = DuplicateOptions()
    ): Result<String> {
        return try {
            if (targetClientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente target è obbligatorio"))
            }

            // Recupera facility sorgente
            val sourceFacility = facilityRepository.getFacilityById(sourceFacilityId)
                .getOrElse { return Result.failure(it) }
                ?: return Result.failure(NoSuchElementException("Facility sorgente non trovata"))

            // Crea copia modificando il clientId
            val copyWithNewClient = sourceFacility.copy(clientId = targetClientId)

            // Usa il metodo normale di duplicazione
            invoke(copyWithNewClient.id, newName, duplicateOptions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Duplica multiple facilities in batch
     */
    suspend fun duplicateMultipleFacilities(
        facilityIds: List<String>,
        namePrefix: String = "",
        duplicateOptions: DuplicateOptions = DuplicateOptions()
    ): Result<DuplicateBatchResult> {
        return try {
            val results = mutableListOf<String>()
            val errors = mutableListOf<Pair<String, Exception>>()

            facilityIds.forEachIndexed { index, facilityId ->
                val customName = if (namePrefix.isNotBlank()) {
                    "$namePrefix ${index + 1}"
                } else null

                invoke(facilityId, customName, duplicateOptions)
                    .onSuccess { newFacilityId ->
                        results.add(newFacilityId)
                    }
                    .onFailure { error ->
                        if (error is Exception) {
                            errors.add(facilityId to error)
                        }
                    }
            }

            val batchResult = DuplicateBatchResult(
                successfulDuplicates = results,
                failedDuplicates = errors.toMap(),
                totalRequested = facilityIds.size
            )

            Result.success(batchResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea template facility da facility esistente
     * (copia senza dati specifici per riutilizzo)
     */
    suspend fun createTemplate(
        sourceFacilityId: String,
        templateName: String
    ): Result<String> {
        return try {
            val templateOptions = DuplicateOptions(
                duplicateCode = false,
                duplicateDescription = false,
                activateImmediately = false
            )

            invoke(
                sourceFacilityId = sourceFacilityId,
                newName = "TEMPLATE: $templateName",
                duplicateOptions = templateOptions
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica se una facility può essere duplicata
     */
    suspend fun canDuplicate(facilityId: String): Result<CanDuplicateResult> {
        return try {
            val facility = facilityRepository.getFacilityById(facilityId)
                .getOrElse { return Result.failure(it) }
                ?: return Result.failure(NoSuchElementException("Facility non trovata"))

            val warnings = buildList {
                if (facility.isPrimary) {
                    add("La facility è primaria - la copia non sarà primaria")
                }
                if (facility.hasIslands()) {
                    add("Le isole associate non verranno duplicate")
                }
                if (!facility.isActive) {
                    add("La facility sorgente non è attiva")
                }
            }

            val result = CanDuplicateResult(
                canDuplicate = true, // Sempre possibile duplicare
                warnings = warnings,
                suggestedName = generateIncrementalName(facility).getOrNull(),
                hasIslands = facility.hasIslands(),
                isPrimary = facility.isPrimary
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Opzioni per la duplicazione
 */
data class DuplicateOptions(
    val duplicateCode: Boolean = true,
    val duplicateDescription: Boolean = true,
    val activateImmediately: Boolean = true
) {
    companion object {
        fun minimal() = DuplicateOptions(
            duplicateCode = false,
            duplicateDescription = false,
            activateImmediately = false
        )

        fun full() = DuplicateOptions(
            duplicateCode = true,
            duplicateDescription = true,
            activateImmediately = true
        )
    }
}

/**
 * Risultato operazione batch duplicate
 */
data class DuplicateBatchResult(
    val successfulDuplicates: List<String>,
    val failedDuplicates: Map<String, Exception>,
    val totalRequested: Int
) {
    val successCount: Int = successfulDuplicates.size
    val errorCount: Int = failedDuplicates.size
    val successRate: Float = if (totalRequested > 0) successCount.toFloat() / totalRequested else 0f
}

/**
 * Risultato verifica duplicabilità
 */
data class CanDuplicateResult(
    val canDuplicate: Boolean,
    val warnings: List<String>,
    val suggestedName: String?,
    val hasIslands: Boolean,
    val isPrimary: Boolean
)