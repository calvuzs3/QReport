package net.calvuz.qreport.photo.data.local.mapper

import net.calvuz.qreport.photo.data.local.entity.PhotoEntity
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoMetadata

/**
 * Mappers per convertire tra PhotoEntity (Data Layer) e Photo (Domain Layer).
 * AGGIORNATI per la PhotoEntity esistente che usa TypeConverters eleganti.
 *
 * VANTAGGI della vostra implementazione:
 * - TypeConverters automatici = ZERO mapping manuale per PhotoLocation, PhotoPerspective, etc.
 * - Type safety completa
 * - Codice molto più pulito
 * - Manutenibilità superiore
 */

/**
 * Converte PhotoEntity in Photo (Domain Model).
 * SEMPLIFICATO grazie ai vostri TypeConverters!
 */
fun PhotoEntity.toDomain(): Photo {
    return Photo(
        id = this.id,
        checkItemId = this.checkItemId,
        fileName = this.fileName,
        filePath = this.filePath,
        thumbnailPath = this.thumbnailPath,
        caption = this.caption,
        takenAt = this.takenAt,
        fileSize = this.fileSize,
        orderIndex = this.orderIndex,
        metadata = PhotoMetadata(
            width = this.width,
            height = this.height,
            exifData = this.exifData, // TypeConverter automatico Map<String,String>
            perspective = this.perspective, // TypeConverter automatico PhotoPerspective
            gpsLocation = this.gpsLocation, // TypeConverter automatico PhotoLocation
            timestamp = this.takenAt,
            fileSize = this.fileSize,
            resolution = this.resolution, // TypeConverter automatico PhotoResolution
            cameraSettings = this.cameraSettings // TypeConverter automatico CameraSettings
        )
    )
}

/**
 * Converte Photo (Domain Model) in PhotoEntity.
 * SEMPLIFICATO grazie ai vostri TypeConverters!
 */
fun Photo.toEntity(): PhotoEntity {
    return PhotoEntity(
        id = this.id,
        checkItemId = this.checkItemId,
        fileName = this.fileName,
        filePath = this.filePath,
        thumbnailPath = this.thumbnailPath,
        caption = this.caption,
        takenAt = this.takenAt,
        fileSize = this.fileSize,
        orderIndex = this.orderIndex,
        // Dimensions
        width = this.metadata.width,
        height = this.metadata.height,
        // Metadati - TypeConverters automatici!
        gpsLocation = this.metadata.gpsLocation, // PhotoLocation diretto!
        resolution = this.metadata.resolution, // PhotoResolution diretto!
        perspective = this.metadata.perspective, // PhotoPerspective diretto!
        exifData = this.metadata.exifData, // Map<String,String> diretto!
        cameraSettings = this.metadata.cameraSettings // CameraSettings diretto!
    )
}

/**
 * Converte una lista di PhotoEntity in lista di Photo.
 */
fun List<PhotoEntity>.toDomain(): List<Photo> {
    return this.map { it.toDomain() }
}

/**
 * Converte una lista di Photo in lista di PhotoEntity.
 */
fun List<Photo>.toEntity(): List<PhotoEntity> {
    return this.map { it.toEntity() }
}
