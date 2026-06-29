package net.calvuz.qreport.checkup.spareparts.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checkup_spare_parts",
    indices = [Index(value = ["checkup_id"])]
)
data class CheckUpSparePartEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "checkup_id")
    val checkupId: String,

    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "code_oem")
    val codeOem: String = "",

    @ColumnInfo(name = "code_erp")
    val codeErp: String = "",

    @ColumnInfo(name = "code_bm")
    val codeBm: String = "",

    @ColumnInfo(name = "unit")
    val unit: String = "pz",

    @ColumnInfo(name = "quantity")
    val quantity: Double? = null,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "added_at")
    val addedAt: Long
)
