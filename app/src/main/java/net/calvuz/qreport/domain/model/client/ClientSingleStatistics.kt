package net.calvuz.qreport.domain.model.client


/**
 * Statistiche per singolo cliente (per UI liste e card)
 */
data class ClientSingleStatistics(
    val facilitiesCount: Int,
    val islandsCount: Int,
    val contactsCount: Int,
    val totalCheckUps: Int,
    val completedCheckUps: Int,
    val lastCheckUpDate: kotlinx.datetime.Instant?
) {
    /**
     * Percentuale CheckUp completati
     */
    val completionRate: Int
        get() = if (totalCheckUps > 0) {
            (completedCheckUps.toDouble() / totalCheckUps * 100).toInt()
        } else 0

    /**
     * Indica se il cliente ha dati completi
     */
    val isComplete: Boolean
        get() = facilitiesCount > 0 && contactsCount > 0

    /**
     * Score salute cliente (0-100)
     */
    val healthScore: Int
        get() {
            var score = 0
            if (facilitiesCount > 0) score += 20
            if (contactsCount > 0) score += 20
            if (islandsCount > 0) score += 20
            if (totalCheckUps > 0) score += 40
            return score
        }

    /**
     * Descrizione stato per UI
     */
    val statusDescription: String
        get() = when {
            !isComplete -> "Setup incompleto"
            totalCheckUps == 0 -> "Nessun check-up"
            completionRate < 50 -> "Check-up in corso"
            else -> "Operativo"
        }

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
                parts.isEmpty() -> append("Nessun dato configurato")
                parts.size == 1 -> append(parts.first())
                parts.size == 2 -> append("${parts[0]} e ${parts[1]}")
                else -> append("${parts.dropLast(1).joinToString(", ")} e ${parts.last()}")
            }
        }
}