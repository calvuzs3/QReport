package net.calvuz.qreport.checkup.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import net.calvuz.qreport.photo.data.local.entity.PhotoEntity

/**
 * Data class per CheckItem con le sue foto
 */
data class CheckItemWithPhotos(
    @Embedded val checkItem: CheckItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "check_item_id"
    )
    val photos: List<PhotoEntity>
)