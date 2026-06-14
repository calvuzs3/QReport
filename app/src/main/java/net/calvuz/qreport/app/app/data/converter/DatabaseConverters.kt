@file:Suppress("HardCodedStringLiteral", "unused")
package net.calvuz.qreport.app.app.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jakarta.inject.Inject
import kotlinx.datetime.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Type converters per il database Room
 * AGGIORNATO: Aggiunge TypeConverters per PhotoMetadata types
 */
class DatabaseConverters @Inject constructor() {

    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // ===============================
    // DateTime Converters
    // ===============================

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            LocalDateTime.parse(it, dateFormatter)
        }
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }

    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? {
        return epochMilli?.let { Instant.fromEpochMilliseconds(it) }
    }

    // ===============================
    // Collection Converters
    // ===============================

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

}