package net.calvuz.qreport.domain.model.client

import kotlinx.serialization.Serializable

/**
 * Statistiche rapide per UI
 */
@Serializable
data class ClientQuickStats(
    val facilitiesCount: Int,
    val contactsCount: Int,
    val islandsCount: Int,
    val checkupsCount: Int
) {

    /**
     * Testo riassuntivo per UI
     */
    val summaryText: String
        get() = buildString {
            val parts = mutableListOf<String>()

            if (facilitiesCount > 0) {
                parts.add("$facilitiesCount stabiliment${if (facilitiesCount == 1) "o" else "i"}")
            }

            if (islandsCount > 0) {
                parts.add("$islandsCount isol${if (islandsCount == 1) "a" else "e"}")
            }

            if (contactsCount > 0) {
                parts.add("$contactsCount referent${if (contactsCount == 1) "e" else "i"}")
            }

            when {
                parts.isEmpty() -> append("Nessun dato")
                parts.size == 1 -> append(parts.first())
                parts.size == 2 -> append("${parts[0]} e ${parts[1]}")
                else -> append("${parts.dropLast(1).joinToString(", ")} e ${parts.last()}")
            }
        }
}