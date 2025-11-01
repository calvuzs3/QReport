package net.calvuz.qreport.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CheckUpWithFullDetails (
    @Embedded val checkUp: CheckUpEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "checkup_id",
        entity = CheckItemEntity::class
    )
    val checkItemsWithPhotos: List<CheckItemWithPhotos>,
    @Relation(
        parentColumn = "id",
        entityColumn = "checkup_id"
    )
    val spareParts: List<SparePartEntity>
)