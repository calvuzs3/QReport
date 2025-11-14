package net.calvuz.qreport.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity Room per Client
 * Mapping del domain model Client per persistenza database
 */
@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["company_name"]),
        Index(value = ["vat_number"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class ClientEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "company_name")
    val companyName: String,

    @ColumnInfo(name = "vat_number")
    val vatNumber: String? = null,

    @ColumnInfo(name = "fiscal_code")
    val fiscalCode: String? = null,

    @ColumnInfo(name = "website")
    val website: String? = null,

    @ColumnInfo(name = "industry")
    val industry: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "headquarters_json")
    val headquartersJson: String? = null, // JSON serialized Address

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)