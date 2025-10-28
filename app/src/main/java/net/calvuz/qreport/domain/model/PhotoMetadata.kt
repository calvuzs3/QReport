package net.calvuz.qreport.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Metadati completi delle foto
 * IMPLEMENTAZIONE COMPLETA: Tutti i campi necessari per report dettagliati
 */
@Serializable
data class PhotoMetadata(
    val exifData: Map<String, String> = emptyMap(),     // Metadati EXIF originali
    val perspective: PhotoPerspective? = null,           // Angolazione della foto (front, back, etc.)
    val gpsLocation: PhotoLocation? = null,              // Coordinate GPS
    val timestamp: Instant? = null,                      // Timestamp della foto
    val fileSize: Long = 0L,                            // Dimensione file in bytes
    val resolution: PhotoResolution? = null,             // Risoluzione della foto
    val cameraSettings: CameraSettings? = null          // Impostazioni camera
)