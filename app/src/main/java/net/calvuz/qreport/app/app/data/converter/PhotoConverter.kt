@file:Suppress("HardCodedStringLiteral", "unused")
package net.calvuz.qreport.app.app.data.converter

import com.google.gson.Gson
import net.calvuz.qreport.photo.domain.model.CameraSettings
import net.calvuz.qreport.photo.domain.model.PhotoLocation
import net.calvuz.qreport.photo.domain.model.PhotoPerspective
import net.calvuz.qreport.photo.domain.model.PhotoResolution
import androidx.room.TypeConverter
import jakarta.inject.Inject
import timber.log.Timber

class PhotoConverter @Inject constructor() {
    
    private val gson = Gson()
    
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
                Timber.e(e, "Failed to parse PhotoLocation from string: $str")
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
                Timber.e(e, "Failed to parse PhotoPerspective from string: $it")
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
                Timber.e(e, "Failed to parse PhotoResolution from string: $it")
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
                Timber.e(e, "Failed to parse CameraSettings from string: $it")
                null
            }
        }
    }
}