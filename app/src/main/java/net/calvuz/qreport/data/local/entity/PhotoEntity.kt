package net.calvuz.qreport.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import net.calvuz.qreport.domain.model.CameraSettings
import net.calvuz.qreport.domain.model.PhotoLocation
import net.calvuz.qreport.domain.model.PhotoPerspective
import net.calvuz.qreport.domain.model.PhotoResolution

/**
 * Entità Photo
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = CheckItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["check_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["check_item_id"]),
        Index(value = ["taken_at"]),
        Index(value = ["order_index"]),
        Index(value = ["resolution"]),        // Per filtri qualità
        Index(value = ["perspective"])        // Per filtri angolazione
    ]
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "check_item_id") val checkItemId: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "caption") val caption: String,
    @ColumnInfo(name = "taken_at") val takenAt: Instant,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int,

    // ===============================
    // METADATA COLUMNS - TypeConverters automatici
    // ===============================

    /**
     * GPS Location - TypeConverter automatico PhotoLocation ↔ String
     */
    @ColumnInfo(name = "gps_location") val gpsLocation: PhotoLocation? = null,

    /**
     * Risoluzione foto - TypeConverter automatico PhotoResolution ↔ String
     */
    @ColumnInfo(name = "resolution") val resolution: PhotoResolution? = null,

    /**
     * Prospettiva foto - TypeConverter automatico PhotoPerspective ↔ String
     */
    @ColumnInfo(name = "perspective") val perspective: PhotoPerspective? = null,

    /**
     * Metadati EXIF - TypeConverter esistente Map<String, String> ↔ JSON
     */
    @ColumnInfo(name = "exif_data") val exifData: Map<String, String> = emptyMap(),

    /**
     * Impostazioni camera - TypeConverter automatico CameraSettings ↔ JSON
     */
    @ColumnInfo(name = "camera_settings") val cameraSettings: CameraSettings? = null
)