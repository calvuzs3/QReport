package net.calvuz.qreport.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity Room per Contact
 * Mapping del domain model Contact per persistenza database
 */
@Entity(
    tableName = "contacts",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["first_name"]),
        Index(value = ["is_primary", "client_id"]),
        Index(value = ["is_active"])
    ]
)
data class ContactEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "client_id")
    val clientId: String,

    // DATI PERSONALI
    @ColumnInfo(name = "first_name")
    val firstName: String, // ✅ Unico obbligatorio

    @ColumnInfo(name = "last_name")
    val lastName: String? = null,

    @ColumnInfo(name = "title")
    val title: String? = null,

    // RUOLO AZIENDALE
    @ColumnInfo(name = "role")
    val role: String? = null,

    @ColumnInfo(name = "department")
    val department: String? = null,

    // CONTATTI
    @ColumnInfo(name = "phone")
    val phone: String? = null,

    @ColumnInfo(name = "mobile_phone")
    val mobilePhone: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "alternative_email")
    val alternativeEmail: String? = null,

    // STATO
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "preferred_contact_method")
    val preferredContactMethod: String? = null, // ContactMethod.name

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    // METADATI
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)