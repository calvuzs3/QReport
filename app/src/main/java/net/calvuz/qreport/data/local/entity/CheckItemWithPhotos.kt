package net.calvuz.qreport.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

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