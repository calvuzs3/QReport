# 🏢 QReport - Fase 4: Gestione Clienti

**Versione:** 1.0  
**Data:** Novembre 2025  
**Target:** Sistema completo gestione anagrafica clienti industriali  
**Integrazione:** Compatibilità totale con CheckUp esistente

---

## 📋 INDICE

1. [Panoramica e Obiettivi](#1-panoramica-e-obiettivi)
2. [Analisi Modelli Esistenti](#2-analisi-modelli-esistenti)
3. [Domain Models](#3-domain-models)
4. [Architettura Clean](#4-architettura-clean)
5. [Database Schema (Room)](#5-database-schema-room)
6. [Use Cases](#6-use-cases)
7. [Repository Pattern](#7-repository-pattern)
8. [UI Planning](#8-ui-planning)
9. [Integrazione con CheckUp](#9-integrazione-con-checkup)
10. [Migrazione e Compatibilità](#10-migrazione-e-compatibilita)
11. [Piano di Implementazione](#11-piano-di-implementazione)

---

## 1. PANORAMICA E OBIETTIVI

### 1.1 Obiettivi Business

**🎯 Sistema Anagrafica Clienti Completo:**
- **Gestione clienti industriali** con informazioni complete
- **Stabilimenti multipli** per cliente con localizzazione GPS
- **Referenti aziendali** con ruoli e contatti flessibili
- **Isole robotizzate** per stabilimento con storico manutenzioni
- **Integrazione seamless** con sistema CheckUp esistente

**📊 Workflow Target:**
```
Cliente → Stabilimenti → Isole → CheckUp
                ↓
            Referenti
```

### 1.2 Funzionalità Principali

**👥 Gestione Clienti:**
- ✅ Anagrafica completa con P.IVA e sede legale
- ✅ Localizzazione geografica con coordinate
- ✅ Storico checkup e statistiche per cliente

**🏭 Gestione Stabilimenti:**
- ✅ Stabilimenti multipli per cliente
- ✅ Localizzazione precisa con coordinate GPS
- ✅ Associazione isole robotizzate per stabilimento

**📞 Gestione Referenti:**
- ✅ Referenti multipli per cliente
- ✅ Solo nome obbligatorio, resto opzionale
- ✅ Ruoli e responsabilità configurabili
- ✅ Referente primario per comunicazioni

**🤖 Gestione Isole per Stabilimento:**
- ✅ Lista isole POLY per stabilimento
- ✅ Dettagli tecnici e storico manutenzioni
- ✅ Stato operativo e prossimi checkup

---

## 2. ANALISI MODELLI ESISTENTI

### 2.1 Modelli Attuali da Integrare

**🔍 ClientInfo (da espandere):**
```kotlin
// Attuale - semplice
data class ClientInfo(
    val companyName: String,
    val contactPerson: String = "",
    val site: String = "",              // → diventa Facility
    val address: String = "",           // → diventa Address strutturato
    val phone: String = "",            // → diventa Contact
    val email: String = ""             // → diventa Contact
)
```

**🏝️ IslandInfo (da riutilizzare):**
```kotlin
data class IslandInfo(
    val serialNumber: String,
    val model: String = "",
    val installationDate: String = "",
    val lastMaintenanceDate: String = "",
    val operatingHours: Int = 0,
    val cycleCount: Long = 0L
)
```

**⚙️ IslandType (da riutilizzare):**
```kotlin
enum class IslandType {
    POLY_MOVE,    // Sistema robotizzato movimentazione
    POLY_CAST,    // Sistema robotizzato casting
    POLY_EBT,     // Sistema robotizzato EBT
    POLY_TAG_BLE, // Sistema etichettatura BLE
    POLY_TAG_FC,  // Sistema etichettatura FC QR
    POLY_TAG_V,   // Sistema etichettatura visione
    POLY_SAMPLE   // Sistema campionamento
}
```

### 2.2 Integrazione con CheckUp

**🔗 CheckUpHeader (da mantenere compatibile):**
```kotlin
data class CheckUpHeader(
    val clientInfo: ClientInfo,         // ← Deve rimanere compatibile
    val islandInfo: IslandInfo,         // ← OK, riutilizzo
    val technicianInfo: TechnicianInfo, // ← OK, invariato
    val checkUpDate: Instant,
    val notes: String = ""
)
```

---

## 3. DOMAIN MODELS

### 3.1 Client - Modello Principale

```kotlin
package net.calvuz.qreport.domain.model.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.client.ClientInfo // ← Import esistente

/**
 * Cliente industriale completo
 * Estende il concetto di ClientInfo esistente mantenendo compatibilità
 */
@Serializable
data class Client(
    val id: String,
    
    // ===== DATI AZIENDALI =====
    val companyName: String,
    val vatNumber: String? = null,
    val fiscalCode: String? = null,
    val website: String? = null,
    val industry: String? = null,
    val notes: String? = null,
    
    // ===== LOCALIZZAZIONE =====
    val headquarters: Address? = null,
    
    // ===== RELAZIONI =====
    val facilities: List<String> = emptyList(),  // IDs delle facilities
    val contacts: List<String> = emptyList(),    // IDs dei contacts
    
    // ===== METADATI =====
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Conversione a ClientInfo per compatibilità con CheckUp esistente
     */
    fun toClientInfo(
        primaryContact: Contact? = null,
        primaryFacility: Facility? = null
    ): ClientInfo = ClientInfo(
        companyName = companyName,
        contactPerson = primaryContact?.fullName ?: "",
        site = primaryFacility?.name ?: "",
        address = headquarters?.toDisplayString() ?: "",
        phone = primaryContact?.phone ?: "",
        email = primaryContact?.email ?: ""
    )

    /**
     * Verifica se cliente ha stabilimenti
     */
    fun hasFacilities(): Boolean = facilities.isNotEmpty()

    /**
     * Verifica se cliente ha referenti
     */
    fun hasContacts(): Boolean = contacts.isNotEmpty()
}
```

### 3.2 Facility - Stabilimento

```kotlin
package net.calvuz.qreport.domain.model.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Stabilimento del cliente
 * Un cliente può avere più stabilimenti produttivi
 */
@Serializable
data class Facility(
    val id: String,
    val clientId: String,
    
    // ===== DATI STABILIMENTO =====
    val name: String,
    val code: String? = null,              // Codice interno cliente
    val description: String? = null,
    val facilityType: FacilityType = FacilityType.PRODUCTION,
    
    // ===== LOCALIZZAZIONE =====
    val address: Address,
    
    // ===== STATO =====
    val isPrimary: Boolean = false,        // Stabilimento principale
    val isActive: Boolean = true,
    
    // ===== RELAZIONI =====
    val islands: List<String> = emptyList(), // IDs delle isole POLY
    
    // ===== METADATI =====
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Verifica se ha isole associate
     */
    fun hasIslands(): Boolean = islands.isNotEmpty()

    /**
     * Genera descrizione completa
     */
    val displayName: String
        get() = if (code.isNullOrBlank()) name else "$name ($code)"
}

/**
 * Tipologie di stabilimento
 */
@Serializable
enum class FacilityType(val displayName: String) {
    PRODUCTION("Produzione"),
    WAREHOUSE("Magazzino"),  
    ASSEMBLY("Assemblaggio"),
    TESTING("Test e Collaudi"),
    LOGISTICS("Logistica"),
    OFFICE("Uffici"),
    OTHER("Altro")
}
```

### 3.3 Contact - Referente

```kotlin
package net.calvuz.qreport.domain.model.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Referente del cliente
 * Solo firstName è obbligatorio come richiesto
 */
@Serializable
data class Contact(
    val id: String,
    val clientId: String,
    
    // ===== DATI PERSONALI ===== 
    val firstName: String,                 // ✅ OBBLIGATORIO (unico campo required)
    val lastName: String? = null,
    val title: String? = null,             // Ing., Dott., etc.
    
    // ===== RUOLO AZIENDALE =====
    val role: String? = null,              // Responsabile Manutenzione, etc.
    val department: String? = null,        // Produzione, Qualità, etc.
    
    // ===== CONTATTI =====
    val phone: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val alternativeEmail: String? = null,
    
    // ===== STATO =====
    val isPrimary: Boolean = false,        // Referente principale
    val isActive: Boolean = true,
    val preferredContactMethod: ContactMethod? = null,
    
    // ===== METADATI =====
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Nome completo formattato
     */
    val fullName: String
        get() {
            val prefix = if (!title.isNullOrBlank()) "$title " else ""
            val suffix = if (!lastName.isNullOrBlank()) " $lastName" else ""
            return "$prefix$firstName$suffix".trim()
        }

    /**
     * Descrizione ruolo completa
     */
    val roleDescription: String
        get() = when {
            !role.isNullOrBlank() && !department.isNullOrBlank() -> "$role - $department"
            !role.isNullOrBlank() -> role
            !department.isNullOrBlank() -> department
            else -> ""
        }

    /**
     * Contatto primario disponibile
     */
    val primaryContact: String?
        get() = when (preferredContactMethod) {
            ContactMethod.PHONE -> phone
            ContactMethod.MOBILE -> mobilePhone
            ContactMethod.EMAIL -> email
            null -> phone ?: mobilePhone ?: email
        }
}

/**
 * Metodi di contatto preferiti
 */
@Serializable
enum class ContactMethod(val displayName: String) {
    PHONE("Telefono fisso"),
    MOBILE("Cellulare"),
    EMAIL("Email")
}
```

### 3.4 FacilityIsland - Isola per Stabilimento

```kotlin
package net.calvuz.qreport.domain.model.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.model.island.IslandInfo

/**
 * Isola robotizzata associata a uno stabilimento
 * Combina IslandType e IslandInfo esistenti
 */
@Serializable
data class FacilityIsland(
    val id: String,
    val facilityId: String,
    
    // ===== TIPO ISOLA =====
    val islandType: IslandType,           // ✅ Riutilizzo enum esistente
    
    // ===== DETTAGLI TECNICI =====
    val serialNumber: String,             // Numero seriale unico
    val model: String? = null,
    val installationDate: Instant? = null,
    val warrantyExpiration: Instant? = null,
    
    // ===== STATO OPERATIVO =====
    val isActive: Boolean = true,
    val operatingHours: Int = 0,
    val cycleCount: Long = 0L,
    val lastMaintenanceDate: Instant? = null,
    val nextScheduledMaintenance: Instant? = null,
    
    // ===== CONFIGURAZIONE =====
    val customName: String? = null,       // Nome personalizzato dal cliente
    val location: String? = null,         // Posizione nello stabilimento
    val notes: String? = null,
    
    // ===== METADATI =====
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Nome display dell'isola
     */
    val displayName: String
        get() = customName ?: "${islandType.displayName} (${serialNumber})"

    /**
     * Conversione a IslandInfo per compatibilità CheckUp
     */
    fun toIslandInfo(): IslandInfo = IslandInfo(
        serialNumber = serialNumber,
        model = model ?: "",
        installationDate = installationDate?.toString() ?: "",
        lastMaintenanceDate = lastMaintenanceDate?.toString() ?: "",
        operatingHours = operatingHours,
        cycleCount = cycleCount
    )

    /**
     * Verifica se isola ha bisogno di manutenzione
     */
    fun needsMaintenance(): Boolean {
        return nextScheduledMaintenance?.let { next ->
            next <= kotlinx.datetime.Clock.System.now()
        } ?: false
    }

    /**
     * Stato operativo descrittivo
     */
    val operationalStatus: OperationalStatus
        get() = when {
            !isActive -> OperationalStatus.INACTIVE
            needsMaintenance() -> OperationalStatus.MAINTENANCE_DUE
            else -> OperationalStatus.OPERATIONAL
        }
}

/**
 * Stati operativi dell'isola
 */
@Serializable
enum class OperationalStatus(val displayName: String, val color: String) {
    OPERATIONAL("Operativa", "00B050"),
    MAINTENANCE_DUE("Manutenzione dovuta", "FFC000"),
    INACTIVE("Non attiva", "FF0000")
}
```

### 3.5 Address - Localizzazione

```kotlin
package net.calvuz.qreport.domain.model.client

import kotlinx.serialization.Serializable

/**
 * Indirizzo e localizzazione geografica
 * Supporta coordinate GPS per mapping industriale
 */
@Serializable
data class Address(
    // ===== INDIRIZZO =====
    val street: String? = null,
    val streetNumber: String? = null,
    val city: String? = null,
    val province: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String = "Italia",
    
    // ===== COORDINATE GPS =====
    val coordinates: GeoCoordinates? = null,
    
    // ===== DETTAGLI AGGIUNTIVI =====
    val notes: String? = null            // Indicazioni aggiuntive per raggiungere
) {

    /**
     * Indirizzo formattato per display
     */
    fun toDisplayString(): String = buildString {
        if (!street.isNullOrBlank()) {
            append(street)
            if (!streetNumber.isNullOrBlank()) {
                append(" $streetNumber")
            }
        }
        
        if (!city.isNullOrBlank()) {
            if (isNotEmpty()) append(", ")
            append(city)
        }
        
        if (!postalCode.isNullOrBlank()) {
            if (isNotEmpty()) append(" ")
            append("($postalCode)")
        }
        
        if (!province.isNullOrBlank()) {
            if (isNotEmpty()) append(" - ")
            append(province)
        }
    }

    /**
     * Indirizzo compatto per export
     */
    fun toCompactString(): String = buildString {
        listOfNotNull(
            if (!street.isNullOrBlank() && !streetNumber.isNullOrBlank()) "$street $streetNumber" else street,
            city,
            province
        ).joinTo(this, ", ")
    }

    /**
     * Verifica se ha coordinate GPS
     */
    fun hasCoordinates(): Boolean = coordinates != null

    /**
     * Verifica se indirizzo è completo
     */
    fun isComplete(): Boolean = !street.isNullOrBlank() && !city.isNullOrBlank()
}

/**
 * Coordinate geografiche
 */
@Serializable
data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null        // Accuratezza in metri
) {

    /**
     * Coordinate formattate
     */
    override fun toString(): String = "$latitude, $longitude"

    /**
     * Link Google Maps
     */
    fun toGoogleMapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"
}
```

---

## 4. ARCHITETTURA CLEAN

### 4.1 Structure Overview

```
domain/
├── model/
│   └── client/
│       ├── Client.kt
│       ├── Facility.kt
│       ├── Contact.kt
│       ├── FacilityIsland.kt
│       └── Address.kt
│
├── repository/
│   ├── ClientRepository.kt
│   ├── FacilityRepository.kt
│   ├── ContactRepository.kt
│   └── FacilityIslandRepository.kt
│
└── usecase/
    └── client/
        ├── GetAllClientsUseCase.kt
        ├── GetClientByIdUseCase.kt
        ├── CreateClientUseCase.kt
        ├── UpdateClientUseCase.kt
        ├── DeleteClientUseCase.kt
        ├── SearchClientsUseCase.kt
        │
        ├── facility/
        │   ├── AddFacilityUseCase.kt
        │   ├── UpdateFacilityUseCase.kt
        │   ├── GetFacilitiesForClientUseCase.kt
        │   └── DeleteFacilityUseCase.kt
        │
        ├── contact/
        │   ├── AddContactUseCase.kt
        │   ├── UpdateContactUseCase.kt
        │   ├── GetContactsForClientUseCase.kt
        │   ├── SetPrimaryContactUseCase.kt
        │   └── DeleteContactUseCase.kt
        │
        └── island/
            ├── AddFacilityIslandUseCase.kt
            ├── UpdateFacilityIslandUseCase.kt
            ├── GetIslandsForFacilityUseCase.kt
            └── GetIslandMaintenanceStatusUseCase.kt
```

### 4.2 Repository Interfaces

```kotlin
// domain/repository/ClientRepository.kt
interface ClientRepository {
    
    // ===== CLIENT CRUD =====
    suspend fun getAllClients(): List<Client>
    fun getAllClientsFlow(): Flow<List<Client>>
    suspend fun getClientById(id: String): Client?
    suspend fun createClient(client: Client): String
    suspend fun updateClient(client: Client)
    suspend fun deleteClient(id: String)
    
    // ===== SEARCH & FILTER =====
    suspend fun searchClientsByName(query: String): List<Client>
    suspend fun getActiveClients(): List<Client>
    suspend fun getClientsWithFacilities(): List<Client>
    
    // ===== COMPLEX QUERIES =====
    suspend fun getClientWithDetails(id: String): ClientWithDetails?
    suspend fun getClientStatistics(id: String): ClientStatistics
}

data class ClientWithDetails(
    val client: Client,
    val facilities: List<FacilityWithIslands>,
    val contacts: List<Contact>,
    val totalCheckUps: Int,
    val lastCheckUpDate: Instant?
)

data class FacilityWithIslands(
    val facility: Facility,
    val islands: List<FacilityIsland>
)

data class ClientStatistics(
    val totalFacilities: Int,
    val totalIslands: Int,
    val totalContacts: Int,
    val checkUpsThisYear: Int,
    val lastActivity: Instant?
)
```

---

## 5. DATABASE SCHEMA (ROOM)

### 5.1 Entity Definitions

```kotlin
// data/local/entity/ClientEntity.kt
@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["company_name"]),
        Index(value = ["vat_number"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class ClientEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "company_name") val companyName: String,
    @ColumnInfo(name = "vat_number") val vatNumber: String?,
    @ColumnInfo(name = "fiscal_code") val fiscalCode: String?,
    @ColumnInfo(name = "website") val website: String?,
    @ColumnInfo(name = "industry") val industry: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "headquarters_json") val headquartersJson: String?, // JSON Address
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

// data/local/entity/FacilityEntity.kt  
@Entity(
    tableName = "facilities",
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
        Index(value = ["name"]),
        Index(value = ["is_primary", "client_id"])
    ]
)
data class FacilityEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "facility_type") val facilityType: String,
    @ColumnInfo(name = "address_json") val addressJson: String, // JSON Address
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

// data/local/entity/ContactEntity.kt
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
        Index(value = ["is_primary", "client_id"])
    ]
)
data class ContactEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "first_name") val firstName: String, // ✅ Unico obbligatorio
    @ColumnInfo(name = "last_name") val lastName: String?,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "role") val role: String?,
    @ColumnInfo(name = "department") val department: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "mobile_phone") val mobilePhone: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "alternative_email") val alternativeEmail: String?,
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "preferred_contact_method") val preferredContactMethod: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

// data/local/entity/FacilityIslandEntity.kt
@Entity(
    tableName = "facility_islands",
    foreignKeys = [
        ForeignKey(
            entity = FacilityEntity::class,
            parentColumns = ["id"],
            childColumns = ["facility_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["facility_id"]),
        Index(value = ["serial_number"], unique = true),
        Index(value = ["island_type"]),
        Index(value = ["is_active"])
    ]
)
data class FacilityIslandEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "facility_id") val facilityId: String,
    @ColumnInfo(name = "island_type") val islandType: String, // IslandType.name
    @ColumnInfo(name = "serial_number") val serialNumber: String,
    @ColumnInfo(name = "model") val model: String?,
    @ColumnInfo(name = "installation_date") val installationDate: Long?,
    @ColumnInfo(name = "warranty_expiration") val warrantyExpiration: Long?,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "operating_hours") val operatingHours: Int,
    @ColumnInfo(name = "cycle_count") val cycleCount: Long,
    @ColumnInfo(name = "last_maintenance_date") val lastMaintenanceDate: Long?,
    @ColumnInfo(name = "next_scheduled_maintenance") val nextScheduledMaintenance: Long?,
    @ColumnInfo(name = "custom_name") val customName: String?,
    @ColumnInfo(name = "location") val location: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
```

### 5.2 DAO Definitions

```kotlin
// data/local/dao/ClientDao.kt
@Dao
interface ClientDao {

    // ===== BASIC CRUD =====
    @Query("SELECT * FROM clients WHERE is_active = 1 ORDER BY company_name ASC")
    fun getAllActiveClientsFlow(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("UPDATE clients SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteClient(id: String, timestamp: Long)

    // ===== SEARCH =====
    @Query("""
        SELECT * FROM clients 
        WHERE is_active = 1 
        AND (company_name LIKE '%' || :query || '%' OR vat_number LIKE '%' || :query || '%')
        ORDER BY company_name ASC
    """)
    suspend fun searchClients(query: String): List<ClientEntity>

    // ===== STATISTICS =====
    @Query("SELECT COUNT(*) FROM clients WHERE is_active = 1")
    suspend fun getActiveClientsCount(): Int

    @Query("""
        SELECT COUNT(*) FROM facilities f
        INNER JOIN clients c ON f.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND f.is_active = 1
    """)
    suspend fun getFacilitiesCount(clientId: String): Int

    @Query("""
        SELECT COUNT(*) FROM facility_islands fi
        INNER JOIN facilities f ON fi.facility_id = f.id
        INNER JOIN clients c ON f.client_id = c.id
        WHERE c.id = :clientId AND c.is_active = 1 AND f.is_active = 1 AND fi.is_active = 1
    """)
    suspend fun getIslandsCount(clientId: String): Int
}
```

### 5.3 Relations and Complex Queries

```kotlin
// Relationship data classes per query complesse
data class ClientWithFacilities(
    @Embedded val client: ClientEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "client_id"
    )
    val facilities: List<FacilityEntity>
)

data class FacilityWithIslands(
    @Embedded val facility: FacilityEntity,
    @Relation(
        parentColumn = "id", 
        entityColumn = "facility_id"
    )
    val islands: List<FacilityIslandEntity>
)

data class ClientWithAllDetails(
    @Embedded val client: ClientEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "client_id"
    )
    val facilities: List<FacilityEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "client_id"
    )
    val contacts: List<ContactEntity>
)
```

---

## 6. USE CASES

### 6.1 Client Use Cases

```kotlin
// domain/usecase/client/CreateClientUseCase.kt
class CreateClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(
        companyName: String,
        vatNumber: String? = null,
        headquarters: Address? = null
    ): Result<String> = try {
        
        // Validazione
        if (companyName.isBlank()) {
            return Result.failure(Exception("Nome azienda obbligatorio"))
        }
        
        if (!vatNumber.isNullOrBlank() && !isValidVatNumber(vatNumber)) {
            return Result.failure(Exception("Partita IVA non valida"))
        }
        
        val client = Client(
            id = UUID.randomUUID().toString(),
            companyName = companyName.trim(),
            vatNumber = vatNumber?.trim(),
            headquarters = headquarters,
            facilities = emptyList(),
            contacts = emptyList(),
            isActive = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val clientId = clientRepository.createClient(client)
        Result.success(clientId)
        
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun isValidVatNumber(vatNumber: String): Boolean {
        // Validazione P.IVA italiana semplificata
        return vatNumber.matches(Regex("\\d{11}"))
    }
}

// domain/usecase/client/GetClientWithDetailsUseCase.kt
class GetClientWithDetailsUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val facilityRepository: FacilityRepository,
    private val contactRepository: ContactRepository,
    private val facilityIslandRepository: FacilityIslandRepository
) {
    suspend operator fun invoke(clientId: String): Result<ClientWithDetails> = try {
        
        val client = clientRepository.getClientById(clientId)
            ?: return Result.failure(Exception("Cliente non trovato"))
            
        val facilities = facilityRepository.getFacilitiesForClient(clientId)
        val contacts = contactRepository.getContactsForClient(clientId)
        
        val facilitiesWithIslands = facilities.map { facility ->
            val islands = facilityIslandRepository.getIslandsForFacility(facility.id)
            FacilityWithIslands(facility, islands)
        }
        
        // TODO: Aggiungere statistiche checkup quando integrato
        val details = ClientWithDetails(
            client = client,
            facilities = facilitiesWithIslands,
            contacts = contacts,
            totalCheckUps = 0,
            lastCheckUpDate = null
        )
        
        Result.success(details)
        
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 6.2 Facility Use Cases

```kotlin
// domain/usecase/client/facility/AddFacilityUseCase.kt
class AddFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val clientRepository: ClientRepository
) {
    suspend operator fun invoke(
        clientId: String,
        name: String,
        address: Address,
        facilityType: FacilityType = FacilityType.PRODUCTION,
        isPrimary: Boolean = false
    ): Result<String> = try {
        
        // Verifica che cliente esista
        val client = clientRepository.getClientById(clientId)
            ?: return Result.failure(Exception("Cliente non trovato"))
            
        // Validazione
        if (name.isBlank()) {
            return Result.failure(Exception("Nome stabilimento obbligatorio"))
        }
        
        if (!address.isComplete()) {
            return Result.failure(Exception("Indirizzo stabilimento incompleto"))
        }
        
        // Se viene impostato come primario, rimuovi flag dagli altri
        if (isPrimary) {
            facilityRepository.clearPrimaryFacility(clientId)
        }
        
        val facility = Facility(
            id = UUID.randomUUID().toString(),
            clientId = clientId,
            name = name.trim(),
            facilityType = facilityType,
            address = address,
            isPrimary = isPrimary,
            isActive = true,
            islands = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val facilityId = facilityRepository.createFacility(facility)
        Result.success(facilityId)
        
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 6.3 Island Use Cases

```kotlin
// domain/usecase/client/island/AddFacilityIslandUseCase.kt  
class AddFacilityIslandUseCase @Inject constructor(
    private val facilityIslandRepository: FacilityIslandRepository,
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(
        facilityId: String,
        islandType: IslandType,
        serialNumber: String,
        model: String? = null,
        customName: String? = null
    ): Result<String> = try {
        
        // Verifica che facility esista
        val facility = facilityRepository.getFacilityById(facilityId)
            ?: return Result.failure(Exception("Stabilimento non trovato"))
            
        // Validazione
        if (serialNumber.isBlank()) {
            return Result.failure(Exception("Numero seriale obbligatorio"))
        }
        
        // Verifica unicità numero seriale
        val existingIsland = facilityIslandRepository.getIslandBySerialNumber(serialNumber)
        if (existingIsland != null) {
            return Result.failure(Exception("Numero seriale già esistente"))
        }
        
        val island = FacilityIsland(
            id = UUID.randomUUID().toString(),
            facilityId = facilityId,
            islandType = islandType,
            serialNumber = serialNumber.trim(),
            model = model?.trim(),
            customName = customName?.trim(),
            isActive = true,
            operatingHours = 0,
            cycleCount = 0L,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val islandId = facilityIslandRepository.createIsland(island)
        Result.success(islandId)
        
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 7. REPOSITORY PATTERN

### 7.1 Repository Implementation

```kotlin
// data/repository/ClientRepositoryImpl.kt
@Singleton
class ClientRepositoryImpl @Inject constructor(
    private val clientDao: ClientDao,
    private val facilityDao: FacilityDao,
    private val contactDao: ContactDao
) : ClientRepository {

    override fun getAllClientsFlow(): Flow<List<Client>> {
        return clientDao.getAllActiveClientsFlow()
            .map { entities -> 
                entities.map { entity -> entity.toDomain() }
            }
    }

    override suspend fun getClientById(id: String): Client? {
        return clientDao.getClientById(id)?.toDomain()
    }

    override suspend fun createClient(client: Client): String {
        val entity = client.toEntity()
        clientDao.insertClient(entity)
        return client.id
    }

    override suspend fun getClientWithDetails(id: String): ClientWithDetails? {
        val clientEntity = clientDao.getClientById(id) ?: return null
        val client = clientEntity.toDomain()
        
        val facilities = facilityDao.getFacilitiesForClient(id)
            .map { it.toDomain() }
            
        val contacts = contactDao.getContactsForClient(id)
            .map { it.toDomain() }
            
        // Transform facilities to include islands
        val facilitiesWithIslands = facilities.map { facility ->
            val islands = facilityIslandDao.getIslandsForFacility(facility.id)
                .map { it.toDomain() }
            FacilityWithIslands(facility, islands)
        }
        
        return ClientWithDetails(
            client = client,
            facilities = facilitiesWithIslands,
            contacts = contacts,
            totalCheckUps = 0, // TODO: Add when CheckUp integration done
            lastCheckUpDate = null
        )
    }

    override suspend fun searchClientsByName(query: String): List<Client> {
        return clientDao.searchClients(query)
            .map { it.toDomain() }
    }
}
```

---

## 8. UI PLANNING

### 8.1 Screen Structure

**📱 Lista Clienti (Prioritaria):**
```
ClientListScreen
├── ClientListViewModel  
├── ClientListState
├── ClientCard (composable)
├── SearchBar
├── FloatingActionButton (+ Nuovo Cliente)
└── EmptyState
```

**📋 Client Detail Screen:**
```
ClientDetailScreen
├── ClientDetailViewModel
├── ClientDetailState
├── Tabs:
│   ├── InfoTab (dati aziendali)
│   ├── FacilitiesTab (stabilimenti + isole)
│   ├── ContactsTab (referenti)  
│   └── HistoryTab (storico checkup)
```

**🏭 Facility Management:**
```
FacilityListScreen → FacilityDetailScreen → IslandListScreen
```

### 8.2 ClientListScreen Design

```kotlin
// presentation/screen/client/ClientListScreen.kt
@Composable
fun ClientListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val viewModel: ClientListViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search Bar
        SearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            placeholder = "Cerca clienti..."
        )

        when (state) {
            is ClientListState.Loading -> {
                LoadingIndicator()
            }
            is ClientListState.Success -> {
                LazyColumn {
                    items(state.clients) { client ->
                        ClientCard(
                            client = client,
                            onClick = { onNavigateToDetail(client.id) }
                        )
                    }
                }
            }
            is ClientListState.Empty -> {
                EmptyClientsState(
                    onCreateFirst = onNavigateToCreate
                )
            }
            is ClientListState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = viewModel::loadClients
                )
            }
        }
    }

    // FAB per nuovo cliente
    FloatingActionButton(
        onClick = onNavigateToCreate
    ) {
        Icon(Icons.Default.Add, contentDescription = "Nuovo cliente")
    }
}

@Composable
fun ClientCard(
    client: ClientSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Nome azienda
            Text(
                text = client.companyName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Info riassuntive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stabilimenti e isole
                Row {
                    if (client.facilitiesCount > 0) {
                        InfoChip(
                            icon = Icons.Default.Factory,
                            text = "${client.facilitiesCount} stabilimenti"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    if (client.islandsCount > 0) {
                        InfoChip(
                            icon = Icons.Default.Precision Manufacturing,
                            text = "${client.islandsCount} isole"
                        )
                    }
                }

                // Ultimo checkup
                if (client.lastCheckUpDate != null) {
                    Text(
                        text = "Ultimo checkup: ${client.lastCheckUpDate.formatShort()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

---

## 9. INTEGRAZIONE CON CHECKUP

### 9.1 Strategia di Migrazione

**🔄 Approccio Backward Compatible:**

```kotlin
// Mantieni ClientInfo esistente per CheckUp
data class CheckUpHeader(
    val clientInfo: ClientInfo,           // ← Invariato per compatibilità
    val islandInfo: IslandInfo,           // ← Invariato  
    val technicianInfo: TechnicianInfo,   // ← Invariato
    val checkUpDate: Instant,
    val notes: String = ""
)

// Aggiungi metodi di conversione
fun Client.toClientInfo(
    primaryContact: Contact? = null,
    primaryFacility: Facility? = null
): ClientInfo = ClientInfo(
    companyName = companyName,
    contactPerson = primaryContact?.fullName ?: "",
    site = primaryFacility?.name ?: "",
    address = headquarters?.toDisplayString() ?: "",
    phone = primaryContact?.phone ?: "",
    email = primaryContact?.email ?: ""
)

fun FacilityIsland.toIslandInfo(): IslandInfo = IslandInfo(
    serialNumber = serialNumber,
    model = model ?: "",
    installationDate = installationDate?.toString() ?: "",
    lastMaintenanceDate = lastMaintenanceDate?.toString() ?: "",
    operatingHours = operatingHours,
    cycleCount = cycleCount
)
```

### 9.2 Enhanced CheckUp Creation

**🚀 Nuovo Use Case per CheckUp con Client Selection:**

```kotlin
// domain/usecase/checkup/CreateCheckUpFromClientUseCase.kt
class CreateCheckUpFromClientUseCase @Inject constructor(
    private val clientRepository: ClientRepository,
    private val createCheckUpUseCase: CreateCheckUpUseCase // ← Riutilizzo esistente
) {
    suspend operator fun invoke(
        clientId: String,
        facilityIslandId: String,
        technicianInfo: TechnicianInfo,
        checkUpDate: Instant
    ): Result<String> = try {
        
        // Ottieni dati cliente completi
        val clientDetails = clientRepository.getClientWithDetails(clientId)
            ?: return Result.failure(Exception("Cliente non trovato"))
            
        // Trova isola specifica
        val island = clientDetails.facilities
            .flatMap { it.islands }
            .find { it.id == facilityIslandId }
            ?: return Result.failure(Exception("Isola non trovata"))
            
        // Trova facility dell'isola
        val facility = clientDetails.facilities
            .find { facilityWithIslands ->
                facilityWithIslands.islands.any { it.id == facilityIslandId }
            }?.facility
            ?: return Result.failure(Exception("Stabilimento non trovato"))
            
        // Trova referente primario
        val primaryContact = clientDetails.contacts.find { it.isPrimary }
        
        // Converti a formati esistenti per compatibilità
        val clientInfo = clientDetails.client.toClientInfo(
            primaryContact = primaryContact,
            primaryFacility = facility
        )
        
        val islandInfo = island.toIslandInfo()
        
        // Crea CheckUp usando use case esistente
        val checkUpHeader = CheckUpHeader(
            clientInfo = clientInfo,
            islandInfo = islandInfo,
            technicianInfo = technicianInfo,
            checkUpDate = checkUpDate
        )
        
        // Delega al use case esistente
        createCheckUpUseCase(checkUpHeader)
        
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 9.3 Client Selection in CheckUp UI

**📋 Enhanced CheckUp Creation Flow:**
```
1. ClientSelectionScreen → 
2. FacilitySelectionScreen → 
3. IslandSelectionScreen → 
4. CheckUpCreationScreen (esistente)
```

---

## 10. MIGRAZIONE E COMPATIBILITÀ

### 10.1 Database Migration

```kotlin
// Database migration da versione corrente a +1
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        
        // 1. Crea nuove tabelle
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS clients (
                id TEXT PRIMARY KEY NOT NULL,
                company_name TEXT NOT NULL,
                vat_number TEXT,
                fiscal_code TEXT,
                website TEXT,
                industry TEXT,
                notes TEXT,
                headquarters_json TEXT,
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS facilities (
                id TEXT PRIMARY KEY NOT NULL,
                client_id TEXT NOT NULL,
                name TEXT NOT NULL,
                code TEXT,
                description TEXT,
                facility_type TEXT NOT NULL DEFAULT 'PRODUCTION',
                address_json TEXT NOT NULL,
                is_primary INTEGER NOT NULL DEFAULT 0,
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
            )
        """)
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS contacts (
                id TEXT PRIMARY KEY NOT NULL,
                client_id TEXT NOT NULL,
                first_name TEXT NOT NULL,
                last_name TEXT,
                title TEXT,
                role TEXT,
                department TEXT,
                phone TEXT,
                mobile_phone TEXT,
                email TEXT,
                alternative_email TEXT,
                is_primary INTEGER NOT NULL DEFAULT 0,
                is_active INTEGER NOT NULL DEFAULT 1,
                preferred_contact_method TEXT,
                notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
            )
        """)
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS facility_islands (
                id TEXT PRIMARY KEY NOT NULL,
                facility_id TEXT NOT NULL,
                island_type TEXT NOT NULL,
                serial_number TEXT NOT NULL UNIQUE,
                model TEXT,
                installation_date INTEGER,
                warranty_expiration INTEGER,
                is_active INTEGER NOT NULL DEFAULT 1,
                operating_hours INTEGER NOT NULL DEFAULT 0,
                cycle_count INTEGER NOT NULL DEFAULT 0,
                last_maintenance_date INTEGER,
                next_scheduled_maintenance INTEGER,
                custom_name TEXT,
                location TEXT,
                notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE CASCADE
            )
        """)
        
        // 2. Crea indici
        database.execSQL("CREATE INDEX idx_clients_company_name ON clients(company_name)")
        database.execSQL("CREATE UNIQUE INDEX idx_clients_vat_number ON clients(vat_number)")
        database.execSQL("CREATE INDEX idx_facilities_client_id ON facilities(client_id)")
        database.execSQL("CREATE INDEX idx_contacts_client_id ON contacts(client_id)")
        database.execSQL("CREATE INDEX idx_facility_islands_facility_id ON facility_islands(facility_id)")
        database.execSQL("CREATE UNIQUE INDEX idx_facility_islands_serial ON facility_islands(serial_number)")
    }
}
```

### 10.2 Data Migration Strategy

**🔄 Migration Plan:**
1. **Fase 1**: Crea tabelle clienti senza modificare CheckUp
2. **Fase 2**: Popola clienti da CheckUp esistenti
3. **Fase 3**: Aggiungi UI gestione clienti
4. **Fase 4**: Migra CheckUp creation per usare Client selection
5. **Fase 5**: Cleanup dati duplicati (opzionale)

```kotlin
// Utility per migrazione dati da CheckUp esistenti
class ClientDataMigrationUseCase @Inject constructor(
    private val checkUpRepository: CheckUpRepository,
    private val clientRepository: ClientRepository
) {
    suspend fun migrateExistingCheckUpsToClients(): Result<Unit> = try {
        
        val allCheckUps = checkUpRepository.getAllCheckUps()
        val uniqueClients = mutableMapOf<String, ClientInfo>()
        
        // Estrai clienti unici da CheckUp esistenti
        allCheckUps.forEach { checkUp ->
            val clientKey = checkUp.header.clientInfo.companyName
            if (!uniqueClients.containsKey(clientKey)) {
                uniqueClients[clientKey] = checkUp.header.clientInfo
            }
        }
        
        // Crea Client entities
        uniqueClients.values.forEach { clientInfo ->
            if (clientInfo.companyName.isNotBlank()) {
                
                val address = if (clientInfo.address.isNotBlank()) {
                    Address(street = clientInfo.address)
                } else null
                
                val client = Client(
                    id = UUID.randomUUID().toString(),
                    companyName = clientInfo.companyName,
                    headquarters = address,
                    isActive = true,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                
                clientRepository.createClient(client)
                
                // Se ha site, crea facility
                if (clientInfo.site.isNotBlank()) {
                    val facility = Facility(
                        id = UUID.randomUUID().toString(),
                        clientId = client.id,
                        name = clientInfo.site,
                        address = address ?: Address(),
                        isPrimary = true,
                        isActive = true,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                    
                    facilityRepository.createFacility(facility)
                }
                
                // Se ha contactPerson, crea contact
                if (clientInfo.contactPerson.isNotBlank()) {
                    val contact = Contact(
                        id = UUID.randomUUID().toString(),
                        clientId = client.id,
                        firstName = clientInfo.contactPerson,
                        phone = clientInfo.phone.takeIf { it.isNotBlank() },
                        email = clientInfo.email.takeIf { it.isNotBlank() },
                        isPrimary = true,
                        isActive = true,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                    
                    contactRepository.createContact(contact)
                }
            }
        }
        
        Result.success(Unit)
        
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 11. PIANO DI IMPLEMENTAZIONE

### 11.1 Fasi di Sviluppo

**🚀 Fase 4.1 - Foundation (Settimana 1)**
- ✅ Domain models (Client, Facility, Contact, FacilityIsland, Address)
- ✅ Repository interfaces
- ✅ Database entities e DAOs
- ✅ Basic mappers (toDomain/toEntity)
- ✅ Database migration

**🏗️ Fase 4.2 - Core Use Cases (Settimana 2)**
- ✅ CreateClientUseCase
- ✅ GetAllClientsUseCase
- ✅ GetClientByIdUseCase
- ✅ UpdateClientUseCase
- ✅ AddFacilityUseCase
- ✅ AddContactUseCase
- ✅ AddFacilityIslandUseCase

**📱 Fase 4.3 - UI Lista Clienti (Settimana 3)**
- ✅ ClientListScreen (prioritaria come richiesto)
- ✅ ClientListViewModel
- ✅ ClientCard composable
- ✅ SearchBar implementation
- ✅ Navigation integration

**📋 Fase 4.4 - UI Dettaglio Cliente (Settimana 4)**
- ✅ ClientDetailScreen
- ✅ ClientDetailViewModel
- ✅ Tabs: Info, Facilities, Contacts, History
- ✅ CRUD operations UI
- ✅ Form validations

**🔗 Fase 4.5 - Integrazione CheckUp (Settimana 5)**
- ✅ CreateCheckUpFromClientUseCase
- ✅ Client/Facility/Island selection screens
- ✅ Enhanced CheckUp creation flow
- ✅ Compatibility layer con ClientInfo

**🧪 Fase 4.6 - Testing & Polish (Settimana 6)**
- ✅ Unit tests use cases
- ✅ Repository implementation tests
- ✅ UI tests principali
- ✅ Migration testing
- ✅ Performance optimization

### 11.2 Deliverables per Fase

**📦 Fase 4.1 Deliverables:**
```
domain/model/client/
├── Client.kt ✅
├── Facility.kt ✅
├── Contact.kt ✅
├── FacilityIsland.kt ✅
└── Address.kt ✅

data/local/entity/
├── ClientEntity.kt ✅
├── FacilityEntity.kt ✅
├── ContactEntity.kt ✅
└── FacilityIslandEntity.kt ✅

data/local/dao/
├── ClientDao.kt ✅
├── FacilityDao.kt ✅
├── ContactDao.kt ✅
└── FacilityIslandDao.kt ✅
```

**📱 Fase 4.3 Priority Deliverable:**
```
presentation/screen/client/list/
├── ClientListScreen.kt ✅ PRIORITARIA
├── ClientListViewModel.kt ✅
├── ClientListState.kt ✅
├── ClientCard.kt ✅
└── components/
    ├── SearchBar.kt ✅
    ├── EmptyClientsState.kt ✅
    └── ClientSummaryCard.kt ✅
```

### 11.3 Criterio di Successo

**✅ Definition of Done Fase 4:**
- [ ] Lista clienti funzionante con search
- [ ] CRUD completo clienti con validazioni
- [ ] Gestione stabilimenti e referenti
- [ ] Gestione isole per stabilimento
- [ ] Integrazione fluida con CheckUp esistente
- [ ] Migrazione dati senza perdite
- [ ] Test coverage > 80% use cases critici
- [ ] Performance UI < 16ms frame time
- [ ] Zero crash in scenario normali

**🎯 Success Metrics:**
- **UX**: Creazione cliente in < 30 secondi
- **Performance**: Lista 1000+ clienti in < 1 secondo
- **Reliability**: Zero data loss durante migration
- **Integration**: CheckUp creation mantiene UX esistente

---

## 🎯 CONCLUSIONI

La **Fase 4 - Gestione Clienti** rappresenta un'espansione strategica di QReport verso un sistema completo di CRM industriale specializzato.

**✅ Valore Aggiunto:**
- **Anagrafica centralizzata** per tutti i clienti industriali
- **Gestione multi-stabilimento** con localizzazione GPS
- **Tracking isole robotizzate** per stabilimento
- **Referenti organizzati** per comunicazioni efficaci
- **Integrazione seamless** con workflow CheckUp esistente

**🏗️ Architettura Solida:**
- **Clean Architecture** mantenuta e estesa
- **Backward compatibility** totale con CheckUp
- **Scalabilità enterprise** per grandi clienti
- **Migration strategy** senza interruzioni

**📱 UX Prioritizzata:**
- **Lista clienti** come entry point principale
- **Search & filter** per gestione rapida
- **Detail view** con gestione completa
- **Form semplificati** con validazioni smart

**🚀 Ready for Production:**
Il sistema è progettato per supportare completamente il workflow industriale di QReport, dalla gestione anagrafica alla generazione report, mantenendo la semplicità d'uso che caratterizza l'applicazione.

---

**📄 Documento:** QReport - Fase 4: Gestione Clienti  
**📅 Data:** Novembre 2025  
**📧 Contatto:** luca@calvuz.net  
**🔗 Progetto:** QReport v2.0 - Client Management System