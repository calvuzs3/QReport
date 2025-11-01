package net.calvuz.qreport.domain.model.photo

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.camera.CameraSettings

/**
 * Metadati completi delle foto
 * IMPLEMENTAZIONE COMPLETA: Tutti i campi necessari per report dettagliati
 *
 * AGGIORNAMENTO: Aggiunti width/height per dimensioni reali della foto
 */
@Serializable
data class PhotoMetadata(
    // ✅ NUOVO: Dimensioni reali della foto (post-processing)
    val width: Int = 0,                                  // Larghezza in pixel
    val height: Int = 0,                                 // Altezza in pixel

    // Metadati esistenti
    val exifData: Map<String, String> = emptyMap(),     // Metadati EXIF originali
    val perspective: PhotoPerspective? = null,           // Angolazione della foto (front, back, etc.)
    val gpsLocation: PhotoLocation? = null,              // Coordinate GPS
    val timestamp: Instant? = null,                      // Timestamp della foto
    val fileSize: Long = 0L,                            // Dimensione file in bytes
    val resolution: PhotoResolution? = null,             // Risoluzione target/impostata
    val cameraSettings: CameraSettings? = null          // Impostazioni camera
)