package net.calvuz.qreport.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity per la tabella clienti nel database Room
 * Corrisponde al domain model Client.kt
 */
@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["company_name"]),
        Index(value = ["vat_number"], unique = true),
        Index(value = ["is_active"]),
        Index(value = ["industry"])
    ]
)
data class ClientEntity(
    @PrimaryKey
    val id: String,

    // ===== DATI AZIENDALI =====
    @ColumnInfo(name = "company_name")
    val companyName: String,

    @ColumnInfo(name = "vat_number")
    val vatNumber: String?,

    @ColumnInfo(name = "fiscal_code")
    val fiscalCode: String?,

    val website: String?,
    val industry: String?,
    val notes: String?,

    // ===== HEADQUARTERS SERIALIZZATO JSON =====
    @ColumnInfo(name = "headquarters_json")
    val headquartersJson: String?, // JSON serializzato dell'oggetto Address

    // ===== METADATI =====
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Timestamp in milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long  // Timestamp in milliseconds
)