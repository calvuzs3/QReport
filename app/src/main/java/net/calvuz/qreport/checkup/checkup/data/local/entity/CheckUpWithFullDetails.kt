package net.calvuz.qreport.checkup.checkup.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemEntity
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemWithPhotos

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