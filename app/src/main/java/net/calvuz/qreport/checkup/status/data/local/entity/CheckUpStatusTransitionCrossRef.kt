package net.calvuz.qreport.checkup.status.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Allowed workflow transition: a checkup currently in [fromStatusId] is allowed to
 * move to [toStatusId]. No enforced Room [androidx.room.ForeignKey], same
 * soft-reference convention as the rest of this master-data layer.
 */
@Entity(
    tableName = "checkup_status_transitions",
    primaryKeys = ["from_status_id", "to_status_id"],
    indices = [
        Index(value = ["to_status_id"])
    ]
)
data class CheckUpStatusTransitionCrossRef(
    @ColumnInfo(name = "from_status_id")
    val fromStatusId: String,

    @ColumnInfo(name = "to_status_id")
    val toStatusId: String
)
