@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.app.util

import kotlinx.datetime.*
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.backup.domain.model.BackupInfo
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

    /**
     * Converte kotlinx.datetime.Instant in stringa sicura per nomi file
     * Formato: YYYYMMDD_HHMM (data e ora, locale)
     *
     * @return Stringa formato "20241110_1345" per utilizzo nei nomi file
     */
    fun Instant.toFilenameSafeDateTime(): String {
        val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())

        return buildString {
            append(localDateTime.year.toString().padStart(4, '0'))
            append(localDateTime.monthNumber.toString().padStart(2, '0'))
            append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
            append("_")
            append(localDateTime.hour.toString().padStart(2, '0'))
            append(localDateTime.minute.toString().padStart(2, '0'))
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
    @Suppress("unused")
    fun Instant.toItalianRelativeDate(): UiText {
        val now = Clock.System.now()
        val thisDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        return when (val daysDifference = thisDate.toEpochDays() - todayDate.toEpochDays()) {
            0 -> UiText.StringResource(R.string.date_today)
            1 -> UiText.StringResource(R.string.date_tomorrow)
            -1 -> UiText.StringResource(R.string.date_yesterday)
            in 2..30 -> UiText.StringResources(R.string.date_in_days, daysDifference)
            in -30..-2 -> UiText.StringResources(R.string.date_days_ago, -daysDifference)
            else -> UiText.DynStr(this.toItalianDate())
        }
    }

    /**
     * Formatta data in modo relativo rispetto a oggi
     */
    fun Instant.toItalianLastModified(): UiText {
        val now = Clock.System.now()
        val diffMillis = (now - this).inWholeMilliseconds

        return when {
            diffMillis < 60000 -> UiText.StringResource(R.string.date_updated_now)
            diffMillis < 3600000 -> UiText.StringResources(R.string.date_updated_minutes_ago, diffMillis / 60000)
            diffMillis < 86400000 -> UiText.StringResources(R.string.date_updated_hours_ago, diffMillis / 3600000)
            else -> {
                val days = (now - this).inWholeDays

                when {
                    days == 1L -> UiText.StringResource(R.string.date_updated_yesterday)
                    days < 28 -> UiText.StringResources(R.string.date_updated_days_ago, days)
                    else -> UiText.StringResources(R.string.date_updated_on, this.toItalianDate())
                }
            }
        }
    }

    /**
     * Formatta data in modo relativo rispetto a oggi
     */
    fun Instant.toItalianCreatedAt(): UiText {
        val now = Clock.System.now()
        val diffMillis = (now - this).inWholeMilliseconds

        return when {
            diffMillis < 60000 -> UiText.StringResource(R.string.date_created_now)
            diffMillis < 3600000 -> UiText.StringResources(R.string.date_created_minutes_ago, diffMillis / 60000)
            diffMillis < 86400000 -> UiText.StringResources(R.string.date_created_hours_ago, diffMillis / 3600000)
            else -> {
                val days = (now - this).inWholeDays

                when {
                    days == 1L -> UiText.StringResource(R.string.date_created_yesterday)
                    days < 28 -> UiText.StringResources(R.string.date_created_days_ago, days)
                    else -> UiText.StringResources(R.string.date_created_on, this.toItalianDate())
                }
            }
        }
    }

    /**
     * Formatta per messaggi di scadenza
     * Esempi: "scade oggi", "scade domani", "scade il 15/11/2024", "scaduta il 10/11/2024"
     */
    @Suppress("unused")
    fun Instant.toExpiryMessage(): UiText {
        val now = Clock.System.now()
        val thisDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysDifference = thisDate.toEpochDays() - todayDate.toEpochDays()

        return when {
            this < now -> UiText.StringResources(R.string.date_expired_on, this.toItalianDate())
            daysDifference == 0 -> UiText.StringResource(R.string.date_expires_today)
            daysDifference == 1 -> UiText.StringResource(R.string.date_expires_tomorrow)
            else -> UiText.StringResources(R.string.date_expires_on, this.toItalianDate())
        }
    }

    /**
     * Formatta per messaggi di manutenzione
     * Esempi: "dovuta oggi", "dovuta tra 3 giorni", "in ritardo di 5 giorni"
     */
    @Suppress("unused")
    fun Instant.toMaintenanceMessage(): UiText {
        val now = Clock.System.now()
        val duration = this - now

        return when {
            duration < Duration.ZERO -> {
                val daysLate = (-duration).inWholeDays
                when (daysLate) {
                    0L -> UiText.StringResource(R.string.date_maintenance_overdue_today)
                    1L -> UiText.StringResource(R.string.date_maintenance_overdue_one_day)
                    else -> UiText.StringResources(R.string.date_maintenance_overdue_days, daysLate)
                }
            }

            duration.inWholeDays == 0L -> UiText.StringResource(R.string.date_maintenance_due_today)
            duration.inWholeDays == 1L -> UiText.StringResource(R.string.date_maintenance_due_tomorrow)
            duration.inWholeDays <= 7 -> UiText.StringResources(R.string.date_maintenance_due_in_days, duration.inWholeDays)
            else -> UiText.StringResources(R.string.date_maintenance_due_on, this.toItalianDate())
        }
    }

    /**
     * Formatta durata tra due Instant in formato leggibile
     * Esempi: "2 giorni", "3 settimane", "1 mese"
     */
    @Suppress("unused")
    fun durationBetween(start: Instant, end: Instant): UiText {
        val duration = end - start
        val days = duration.inWholeDays

        return when {
            days == 0L -> UiText.StringResource(R.string.date_today)
            days == 1L -> UiText.StringResource(R.string.date_duration_one_day)
            days < 7 -> UiText.StringResources(R.string.date_duration_days, days)
            days < 14 -> UiText.StringResource(R.string.date_duration_one_week)
            days < 30 -> UiText.StringResources(R.string.date_duration_weeks, days / 7)
            days < 60 -> UiText.StringResource(R.string.date_duration_one_month)
            days < 365 -> UiText.StringResources(R.string.date_duration_months, days / 30)
            else -> UiText.StringResources(R.string.date_duration_years, days / 365)
        }
    }

    fun Long.asFormatedDuration(): String {
        val seconds = this / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    /**
     * Verifica se la data è nel futuro
     */
    @Suppress("unused")
    fun Instant.isFuture(): Boolean = this > Clock.System.now()

    /**
     * Verifica se la data è nel passato
     */
    @Suppress("unused")
    fun Instant.isPast(): Boolean = this < Clock.System.now()

    /**
     * Verifica se la data è oggi
     */
    @Suppress("unused")
    fun Instant.isToday(): Boolean {
        val now = Clock.System.now()
        val thisDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return thisDate == todayDate
    }

    /**
     * Formatta data in italiano
     */
    fun BackupInfo.getFormattedDate(): String {
        return createdAt.toItalianDate()
    }

    /**
     * Formatta timestamp per nomi directory (YYYYMMDD_HHMMSS)
     */
    @Suppress("unused")
    fun formatTimestampToDateTime(instant: Instant): String {
        // Format: 20241220_143022
        return instant.toString()
            .replace("T", "_")
            .replace(":", "")
            .replace("-", "")
            .substring(0, 15)
    }
}