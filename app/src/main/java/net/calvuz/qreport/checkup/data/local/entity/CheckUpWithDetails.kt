package net.calvuz.qreport.checkup.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class per query con relazioni
 */
data class CheckUpWithDetails(
    @Embedded val checkUp: CheckUpEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "checkup_id"
    )
    val checkItems: List<CheckItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "checkup_id"
    )
    val spareParts: List<SparePartEntity>
)