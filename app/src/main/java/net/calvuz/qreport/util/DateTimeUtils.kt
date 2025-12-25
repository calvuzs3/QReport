package net.calvuz.qreport.util

import kotlinx.datetime.*
import net.calvuz.qreport.data.backup.model.BackupInfo
import kotlin.time.Duration

/**
 * Utility per gestione date e formattazione
 */
object DateTimeUtils {

    /**
     * Converte kotlinx.datetime.Instant in stringa sicura per nomi file
     * Formato: YYYYMMDD (solo data, locale)
     *
     * @return Stringa formato "20241110" per utilizzo nei nomi file
     */
    fun Instant.toFilenameSafeDate(): String {
        val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())

        return buildString {
            append(localDateTime.year.toString().padStart(4, '0'))
            append(localDateTime.monthNumber.toString().padStart(2, '0'))
            append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
        }
    }

    // ===== USER-FRIENDLY FORMATTING =====

    /**
     * Formatta data in formato italiano dd/MM/yyyy
     * Esempio: "15/11/2024"
     */
    fun Instant.toItalianDate(): String {
        val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())

        return buildString {
            append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
            append("/")
            append(localDateTime.monthNumber.toString().padStart(2, '0'))
            append("/")
            append(localDateTime.year.toString())
        }
    }

    /**
     * Formatta data e ora in formato italiano dd/MM/yyyy HH:mm
     * Esempio: "15/11/2024 14:30"
     */
    fun Instant.toItalianDateTime(): String {
        val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())

        return buildString {
            append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
            append("/")
            append(localDateTime.monthNumber.toString().padStart(2, '0'))
            append("/")
            append(localDateTime.year.toString())
            append(" ")
            append(localDateTime.hour.toString().padStart(2, '0'))
            append(":")
            append(localDateTime.minute.toString().padStart(2, '0'))
        }
    }

    /**
     * Formatta data in modo relativo rispetto a oggi
     * Esempi: "Oggi", "Ieri", "Tra 5 giorni", "5 giorni fa", "15/11/2024"
     */
    fun Instant.toRelativeDate(): String {
        val now = Clock.System.now()
        val thisDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        val daysDifference = thisDate.toEpochDays() - todayDate.toEpochDays()

        return when (daysDifference) {
            0 -> "Oggi"
            1 -> "Domani"
            -1 -> "Ieri"
            in 2L..30L -> "Tra $daysDifference giorni"
            in -30L..-2L -> "${-daysDifference} giorni fa"
            else -> this.toItalianDate()
        }
    }

    /**
     * Formatta per messaggi di scadenza
     * Esempi: "scade oggi", "scade domani", "scade il 15/11/2024", "scaduta il 10/11/2024"
     */
    fun Instant.toExpiryMessage(): String {
        val now = Clock.System.now()

        return when {
            this < now -> "scaduta il ${this.toItalianDate()}"
            this.toRelativeDate() == "Oggi" -> "scade oggi"
            this.toRelativeDate() == "Domani" -> "scade domani"
            else -> "scade il ${this.toItalianDate()}"
        }
    }

    /**
     * Formatta per messaggi di manutenzione
     * Esempi: "dovuta oggi", "dovuta tra 3 giorni", "in ritardo di 5 giorni"
     */
    fun Instant.toMaintenanceMessage(): String {
        val now = Clock.System.now()
        val duration = this - now

        return when {
            duration < Duration.ZERO -> {
                val daysLate = (-duration).inWholeDays
                when (daysLate) {
                    0L -> "dovuta oggi (in ritardo)"
                    1L -> "in ritardo di 1 giorno"
                    else -> "in ritardo di $daysLate giorni"
                }
            }

            duration.inWholeDays == 0L -> "dovuta oggi"
            duration.inWholeDays == 1L -> "dovuta domani"
            duration.inWholeDays <= 7 -> "dovuta tra ${duration.inWholeDays} giorni"
            else -> "dovuta il ${this.toItalianDate()}"
        }
    }

    /**
     * Formatta durata tra due Instant in formato leggibile
     * Esempi: "2 giorni", "3 settimane", "1 mese"
     */
    fun durationBetween(start: Instant, end: Instant): String {
        val duration = end - start
        val days = duration.inWholeDays

        return when {
            days == 0L -> "Oggi"
            days == 1L -> "1 giorno"
            days < 7 -> "$days giorni"
            days < 14 -> "1 settimana"
            days < 30 -> "${days / 7} settimane"
            days < 60 -> "1 mese"
            days < 365 -> "${days / 30} mesi"
            else -> "${days / 365} anni"
        }
    }

    /**
     * Verifica se la data è nel futuro
     */
    fun Instant.isFuture(): Boolean = this > Clock.System.now()

    /**
     * Verifica se la data è nel passato
     */
    fun Instant.isPast(): Boolean = this < Clock.System.now()

    /**
     * Verifica se la data è oggi
     */
    fun Instant.isToday(): Boolean {
        val now = Clock.System.now()
        val thisDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return thisDate == todayDate
    }

    /**
     * Formatta data in modo relativo rispetto a oggi
     */
    fun formattedLastModified(lastModified: Instant): String {
        val now = Clock.System.now()
        val diffMillis = (now - lastModified).inWholeMilliseconds

        return when {
            diffMillis < 60000 -> "Aggiornato ora"
            diffMillis < 3600000 -> "Aggiornato ${diffMillis / 60000} min fa"
            diffMillis < 86400000 -> "Aggiornato ${diffMillis / 3600000}h fa"
            else -> {
                val days = (lastModified - Clock.System.now()).inWholeDays

                return when {
                    days == 1L -> "Aggiornato ieri"
                    days < 28 -> "Aggiornato $days giorni fa"
                    else -> "Aggiornato il ${lastModified.toItalianDate()}"
                }
            }
        }
    }

    /**
     * Formatta data in italiano
     */
    fun BackupInfo.getFormattedDate(): String {
        return timestamp.toItalianDate()
    }

    /**
     * Formatta timestamp per nomi directory (YYYYMMDD_HHMMSS)
     */
    fun formatTimestampToDateTime(instant: Instant): String {
        // Format: 20241220_143022
        return instant.toString()
            .replace("T", "_")
            .replace(":", "")
            .replace("-", "")
            .substring(0, 15)
    }
}