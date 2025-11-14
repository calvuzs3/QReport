package net.calvuz.qreport.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.camera.CameraSettings
import net.calvuz.qreport.domain.model.photo.PhotoLocation
import net.calvuz.qreport.domain.model.photo.PhotoPerspective
import net.calvuz.qreport.domain.model.photo.PhotoResolution
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Type converters per il database Room
 * AGGIORNATO: Aggiunge TypeConverters per PhotoMetadata types
 */
class DatabaseConverters {

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
        return epochMilli?.let { Instant.Companion.fromEpochMilliseconds(it) }
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

    // ===============================
    // Photo Metadata Converters
    // ===============================

    /**
     * PhotoLocation ↔ String (CSV format)
     * Formato: "latitude,longitude,altitude,accuracy"
     */
    @TypeConverter
    fun fromPhotoLocation(location: PhotoLocation?): String? {
        return location?.let {
            buildString {
                append("${it.latitude},${it.longitude}")
                if (it.altitude != null) {
                    append(",${it.altitude}")
                    if (it.accuracy != null) {
                        append(",${it.accuracy}")
                    }
                } else if (it.accuracy != null) {
                    append(",,${it.accuracy}")
                }
            }
        }
    }

    @TypeConverter
    fun toPhotoLocation(locationString: String?): PhotoLocation? {
        return locationString?.let { str ->
            try {
                val parts = str.split(",")
                if (parts.size >= 2) {
                    PhotoLocation(
                        latitude = parts[0].toDouble(),
                        longitude = parts[1].toDouble(),
                        altitude = parts.getOrNull(2)?.takeIf { it.isNotBlank() }?.toDoubleOrNull(),
                        accuracy = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.toFloatOrNull()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * PhotoPerspective ↔ String (Enum name)
     */
    @TypeConverter
    fun fromPhotoPerspective(perspective: PhotoPerspective?): String? {
        return perspective?.name
    }

    @TypeConverter
    fun toPhotoPerspective(perspectiveString: String?): PhotoPerspective? {
        return perspectiveString?.let {
            try {
                PhotoPerspective.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * PhotoResolution ↔ String (Enum name)
     */
    @TypeConverter
    fun fromPhotoResolution(resolution: PhotoResolution?): String? {
        return resolution?.name
    }

    @TypeConverter
    fun toPhotoResolution(resolutionString: String?): PhotoResolution? {
        return resolutionString?.let {
            try {
                PhotoResolution.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * CameraSettings ↔ String (JSON)
     */
    @TypeConverter
    fun fromCameraSettings(settings: CameraSettings?): String? {
        return settings?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toCameraSettings(settingsJson: String?): CameraSettings? {
        return settingsJson?.let {
            try {
                gson.fromJson(it, CameraSettings::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}