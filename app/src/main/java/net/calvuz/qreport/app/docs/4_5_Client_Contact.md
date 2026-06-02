# QReport — 4_5 Contact Feature Reference

**Version:** 1.0
**Date:** June 2026
**Scope:** `client/contact` — domain models, Room schema, repository, use cases, UI structure

---

## 1. OVERVIEW

Contact is a direct child of Client:

```
Client → Contacts
```

A `Contact` represents a person working at the client company.
Only `firstName` is mandatory. Supports primary contact designation per client.

---

## 2. DOMAIN MODELS

### 2.1 Contact

```kotlin
// client/contact/domain/model/Contact.kt
@Serializable
data class Contact(
    val id: String,
    val clientId: String,

    // ===== PERSONAL DATA =====
    val firstName: String,              // Only mandatory field
    val lastName: String? = null,
    val title: String? = null,          // e.g. Ing., Dott.

    // ===== ROLE =====
    val role: String? = null,           // e.g. Responsabile Manutenzione
    val department: String? = null,

    // ===== CONTACT INFO =====
    val phone: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val alternativeEmail: String? = null,

    // ===== META =====
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val preferredContactMethod: ContactMethod? = ContactMethod.PHONE,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

> **Note:** `fullName` and `roleDescription` are presentation concerns — build them
> in the UI layer:
> - fullName: `"${title?.plus(" ") ?: ""}$firstName${lastName?.let { " $it" } ?: ""}".trim()`
> - roleDescription: `listOfNotNull(role, department).joinToString(" - ")`

---

### 2.2 ContactMethod

```kotlin
// client/contact/domain/model/ContactMethod.kt
@Serializable
enum class ContactMethod {
    MOBILE,
    PHONE,
    EMAIL
}
```

> **Note:** `ContactMethod` has no `labelResId` — add one if the UI needs
> to display localized labels (e.g. in a preference selector).

---

### 2.3 ContactStatistics (read-only, not persisted)

```kotlin
// client/contact/domain/model/ContactStatistics.kt
@Serializable
data class ContactStatistics(
    val totalContacts: Int,
    val activeContacts: Int,
    val inactiveContacts: Int,
    val primaryContacts: Int,
    val contactsWithPhone: Int,
    val contactsWithMobile: Int,
    val contactsWithEmail: Int,
    val contactsWithoutContact: Int,
    val departmentDistribution: Map<String, Int>,
    val roleDistribution: Map<String, Int>,
    val preferredMethodDistribution: Map<String, Int>,
    val completeProfiles: Int,
    val incompleteProfiles: Int
) {
    val activePercentage: Float
        get() = if (totalContacts > 0) activeContacts.toFloat() / totalContacts * 100 else 0f

    companion object {
        fun empty() = ContactStatistics(
            totalContacts = 0, activeContacts = 0, inactiveContacts = 0,
            primaryContacts = 0, contactsWithPhone = 0, contactsWithMobile = 0,
            contactsWithEmail = 0, contactsWithoutContact = 0,
            departmentDistribution = emptyMap(), roleDistribution = emptyMap(),
            preferredMethodDistribution = emptyMap(),
            completeProfiles = 0, incompleteProfiles = 0
        )
    }
}
```

---

## 3. DATABASE SCHEMA (ROOM)

### 3.1 ContactEntity

```kotlin
// client/contact/data/local/entity/ContactEntity.kt

/**
 * Room entity for the contacts table.
 *
 * Delete lifecycle:
 *  isActive=true,  isDeleted=false  →  normal
 *  isActive=false, isDeleted=false  →  deactivated  (first stage)
 *  isActive=false, isDeleted=true   →  marked deleted (second stage)
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
        Index(value = ["is_active"]),
        Index(value = ["is_deleted"]),
        Index(value = ["updated_at"]),
    ]
)
data class ContactEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "client_id")
    val clientId: String,

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String? = null,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "role")
    val role: String? = null,

    @ColumnInfo(name = "department")
    val department: String? = null,

    @ColumnInfo(name = "phone")
    val phone: String? = null,

    @ColumnInfo(name = "mobile_phone")
    val mobilePhone: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "alternative_email")
    val alternativeEmail: String? = null,

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "preferred_contact_method")
    val preferredContactMethod: String? = null, // ContactMethod.name

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,                // Epoch milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,                // Epoch milliseconds

    // ===== SYNC =====
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
```

---

### 3.2 ContactDao

```kotlin
// client/contact/data/local/dao/ContactDao.kt
@Dao
interface ContactDao {

    // ===== REACTIVE QUERIES =====

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0 ORDER BY first_name ASC")
    fun getActiveContactsForClientFlow(clientId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id AND is_deleted = 0")
    fun getContactByIdFlow(id: String): Flow<ContactEntity?>

    // ===== SUSPEND QUERIES =====

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0 ORDER BY is_primary DESC, first_name ASC")
    suspend fun getActiveContactsForClient(clientId: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id AND is_deleted = 0")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE client_id = :clientId AND is_primary = 1 AND is_active = 1 AND is_deleted = 0")
    suspend fun getPrimaryContact(clientId: String): ContactEntity?

    // ===== CRUD =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    // ===== DELETE — TWO-STAGE =====

    @Query("UPDATE contacts SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun deactivateContact(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markContactDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    // ===== PRIMARY MANAGEMENT =====

    @Transaction
    suspend fun setPrimaryContact(clientId: String, contactId: String) {
        clearPrimaryContact(clientId)
        setPrimaryFlag(contactId, true, System.currentTimeMillis())
    }

    @Query("UPDATE contacts SET is_primary = 0, updated_at = :timestamp WHERE client_id = :clientId")
    suspend fun clearPrimaryContact(clientId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET is_primary = :isPrimary, updated_at = :timestamp WHERE id = :id")
    suspend fun setPrimaryFlag(id: String, isPrimary: Boolean, timestamp: Long)

    // ===== SEARCH =====

    @Query("""
        SELECT * FROM contacts
        WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0
          AND (first_name   LIKE '%' || :query || '%'
               OR last_name LIKE '%' || :query || '%'
               OR role      LIKE '%' || :query || '%'
               OR email     LIKE '%' || :query || '%')
        ORDER BY first_name ASC
    """)
    suspend fun searchContactsForClient(clientId: String, query: String): List<ContactEntity>

    // ===== STATISTICS =====

    @Query("SELECT COUNT(*) FROM contacts WHERE client_id = :clientId AND is_active = 1 AND is_deleted = 0")
    suspend fun getActiveContactsCountForClient(clientId: String): Int

    // ===== BACKUP =====

    @Query("SELECT * FROM contacts ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int
}
```

---

## 4. REPOSITORY

### 4.1 Layer contract

The repository layer uses `kotlin.Result<T>` (not `QrResult`).
Error translation to `QrResult<D, QrError>` happens in use cases.

### 4.2 ContactRepository interface

```kotlin
// client/contact/domain/repository/ContactRepository.kt
interface ContactRepository {

    // ===== REACTIVE =====
    fun getActiveContactsByClientFlow(clientId: String): Flow<List<Contact>>
    fun getContactByIdFlow(id: String): Flow<Contact?>

    // ===== CRUD =====
    suspend fun getContactById(id: String): Result<Contact?>
    suspend fun getActiveContactsByClient(clientId: String): Result<List<Contact>>
    suspend fun getPrimaryContact(clientId: String): Result<Contact?>
    suspend fun createContact(contact: Contact): Result<Unit>
    suspend fun updateContact(contact: Contact): Result<Unit>

    // ===== DELETE — TWO-STAGE =====
    suspend fun deactivateContact(id: String): Result<Unit>
    suspend fun markContactDeleted(id: String): Result<Unit>

    // ===== PRIMARY MANAGEMENT =====
    suspend fun setPrimaryContact(clientId: String, contactId: String): Result<Unit>

    // ===== SEARCH =====
    suspend fun searchContactsForClient(clientId: String, query: String): Result<List<Contact>>

    // ===== STATISTICS =====
    suspend fun getActiveContactsCountForClient(clientId: String): Result<Int>

    // ===== BULK =====
    suspend fun createContacts(contacts: List<Contact>): Result<Unit>
}
```

---

## 5. ERROR HANDLING

```kotlin
// presentation/core/model/QrError.kt  (add to the sealed interface)
sealed interface ContactsError : QrError {

    // ── CRUD ─────────────────────────────────────────────────────────────────
    data class LoadError(val message: String? = null) : ContactsError
    data class NotFound(val message: String? = null) : ContactsError
    data class CreateError(val message: String? = null) : ContactsError
    data class UpdateError(val message: String? = null) : ContactsError
    data class DeleteError(val message: String? = null) : ContactsError

    // ── Business rules ───────────────────────────────────────────────────────
    data class MissingClientId(val message: String? = null) : ContactsError
    data class ClientNotFound(val message: String? = null) : ContactsError
    data class ContactIsNotActive(val message: String? = null) : ContactsError
    data class ContactDoesntBelongToClient(val message: String? = null) : ContactsError
    data class CannotChangeClientAssociation(val message: String? = null) : ContactsError
    data class CannotRemovePrimaryFlag(val message: String? = null) : ContactsError
    data class UnknownContactMethodOnDB(val message: String? = null) : ContactsError

    // ── Validation ───────────────────────────────────────────────────────────
    sealed interface ValidationError : ContactsError {
        data class InvalidContactNameLength(val message: String? = null) : ValidationError
        data class InvalidContactLastNameLength(val message: String? = null) : ValidationError
        data class InvalidEmail(val message: String? = null) : ValidationError
        data class InvalidPhone(val message: String? = null) : ValidationError
        data class InvalidMobile(val message: String? = null) : ValidationError
        data class InvalidTitleLength(val message: String? = null) : ValidationError
        data class InvalidRoleLength(val message: String? = null) : ValidationError
        data class InvalidDepartmentLength(val message: String? = null) : ValidationError
    }
}
```

Use cases return `QrResult<D, QrError.ContactsError>`.

---

## 6. USE CASES

### 6.1 Full list

```
CheckContactExistsUseCase
CreateContactUseCase
UpdateContactUseCase
DeleteContactUseCase                ← two-stage
GetContactsByClientUseCase
GetContactByIdUseCase
ObserveContactsByClientUseCase
SearchContactsUseCase
SetPrimaryContactUseCase
```

### 6.2 CreateContactUseCase

```kotlin
class CreateContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkClientExists: CheckClientExistsUseCase
) {
    suspend operator fun invoke(contact: Contact): QrResult<Unit, QrError.ContactsError> {

        if (contact.clientId.isBlank())
            return QrResult.Error(QrError.ContactsError.MissingClientId())

        if (contact.firstName.isBlank())
            return QrResult.Error(QrError.ContactsError.ValidationError.InvalidContactNameLength())

        when (checkClientExists(contact.clientId)) {
            is QrResult.Error -> return QrResult.Error(QrError.ContactsError.ClientNotFound())
            is QrResult.Success -> Unit
        }

        return contactRepository.createContact(contact).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.ContactsError.CreateError(it.message)) }
        )
    }
}
```

### 6.3 DeleteContactUseCase

```kotlin
enum class DeleteContactResult { DEACTIVATED, MARKED_DELETED }

class DeleteContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val checkContactExists: CheckContactExistsUseCase
) {
    suspend operator fun invoke(contactId: String): QrResult<DeleteContactResult, QrError.ContactsError> {

        if (contactId.isBlank())
            return QrResult.Error(QrError.ContactsError.NotFound())

        val contact = when (val r = checkContactExists(contactId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // Cannot deactivate a primary contact directly — reassign first
        if (contact.isActive && contact.isPrimary)
            return QrResult.Error(QrError.ContactsError.CannotRemovePrimaryFlag())

        return when {
            contact.isActive -> contactRepository.deactivateContact(contactId).fold(
                onSuccess = { QrResult.Success(DeleteContactResult.DEACTIVATED) },
                onFailure = { QrResult.Error(QrError.ContactsError.DeleteError(it.message)) }
            )
            !contact.isActive && !contact.isDeleted -> contactRepository.markContactDeleted(contactId).fold(
                onSuccess = { QrResult.Success(DeleteContactResult.MARKED_DELETED) },
                onFailure = { QrResult.Error(QrError.ContactsError.DeleteError(it.message)) }
            )
            else -> QrResult.Error(QrError.ContactsError.ContactIsNotActive())
        }
    }
}
```

---

## 7. UI STRUCTURE

### 7.1 Screens

```
ContactListScreen       ← ContactListViewModel
ContactFormScreen       ← ContactFormViewModel  (create + edit)
```

> No detail screen — all contact data visible on the FULL card.
> Contacts also shown in the ContactsTab of ClientDetailScreen.

### 7.2 ContactListScreen — key elements

```
TopAppBar
  ├── Icon + title (from ContactPkg)
  ├── cycleCardVariant button  (FULL / COMPACT / MINIMAL)
  ├── sort button   → QReportSortOrderMenu (ContactSortOrder.entries)
  └── filter button → QReportFilterMenu   (ContactFilter.entries)

QReportSearchBar(query, onQueryChange)
QReportFiltersChipRow

QReportSelectorRow(      ← parent client selector
    items = clients,
    selectedId = uiState.selectedClientId,
    onSelect = viewModel::selectClient,
    label = R.string.contact_selector_client_label
)                        ← null = show all contacts

Content area (PullToRefresh wrapper):
  isLoading / error / empty / list → ContactCard(variant)

FAB → onCreateNewContact
```

### 7.3 ContactCard variants

| Variant | Button size | Contents |
|---------|------------|----------|
| FULL    | 48 dp      | Full name, title, role + department, phone, mobile, email, primary badge, preferred method chip, status badge |
| COMPACT | 36 dp      | Full name, role, primary badge, one contact info |
| MINIMAL | —          | Full name only, tap to edit |