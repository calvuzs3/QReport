# QReport — 7. Remote Sync Feature

**Version:** 1.0  
**Date:** June 2026  
**Package root:** `net.calvuz.qreport.sync`  
**Scope:** Client management group (clients, contacts, contracts, facilities, islands, units)

---

## 1. Overview

The sync feature adds bidirectional synchronisation between multiple Android devices
and a central Ktor/PostgreSQL server, while preserving the app's **offline-first**
behaviour: Room is always the UI source of truth. Network is never required to use
the app.

### 1.1 Design principles

| Principle | Implementation |
|-----------|---------------|
| Offline-first | Room DB is always authoritative for the UI |
| Incremental sync | Only records where `updated_at > synced_at` are transferred |
| Bidirectional | Every device can create, edit, and soft-delete records |
| Last-write-wins | Conflict resolution: highest `updated_at` wins |
| Soft delete | Deletions are flagged `is_deleted=true` and propagated |
| Secure | JWT auth stored in `EncryptedSharedPreferences` |

### 1.2 Sync scope

The following entity groups are synchronised:

```
clients → contacts
        → contracts
        → facilities → facility_islands → mechanical_units
                                        → maintenance_logs
```

CheckUps, CheckItems, and Photos are **not** synchronised (future phase).

---

## 2. Package Structure

```
sync/
├── app/
│   ├── SyncEventBus.kt             SharedFlow events between ViewModels
│   └── SyncForegroundObserver.kt   Auto-sync on app foreground
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   └── SyncDao.kt          Pending-sync queries, upsert, mark-synced
│   │   ├── SyncSettingsDataStore.kt Persists: mode, lastSyncTs, deviceId, serverUrl
│   │   └── TokenStorage.kt         JWT in EncryptedSharedPreferences
│   │
│   └── remote/
│       ├── dto/
│       │   └── RemoteDtos.kt       Mirror of server SyncDto.kt
│       ├── DynamicUrlInterceptor.kt OkHttp: rewrites base URL per-request
│       ├── QReportApi.kt           Retrofit interface
│       ├── RemoteDataSource.kt     Interface
│       ├── RetrofitRemoteDataSource.kt  Implementation
│       ├── ServerUrlHolder.kt      In-memory URL holder (singleton)
│       └── TokenExpiryInterceptor.kt   OkHttp: clears token on 401
│
├── di/
│   ├── NetworkModule.kt            Hilt: Retrofit, OkHttp, bindings
│   └── SyncModule.kt               Hilt: SyncRepository binding
│
├── domain/
│   ├── model/
│   │   ├── SyncMode.kt             LOCAL_ONLY | REMOTE_ENABLED
│   │   ├── SyncResult.kt           Result of a completed sync session
│   │   └── SyncStatus.kt           UI snapshot: mode, pending count, device id
│   │
│   ├── repository/
│   │   └── SyncRepository.kt       Interface
│   │
│   └── usecase/
│       ├── LoginUseCase.kt         Authenticates and saves JWT token
│       └── SyncUseCase.kt          Orchestrates push + pull
│
├── mapper/
│   └── SyncMapper.kt               Entity ↔ DTO conversion
│
└── presentation/
    └── ui/
        ├── SyncLoginScreen.kt
        ├── SyncLoginViewModel.kt
        ├── SyncSettingsScreen.kt
        └── SyncSettingsViewModel.kt
```

---

## 3. Data Model

### 3.1 Sync fields added to every entity

Every synchronised Room entity carries three additional columns:

```kotlin
@ColumnInfo(name = "updated_at")
val updatedAt: Long          // epoch millis — updated on every local write

@ColumnInfo(name = "synced_at")
val syncedAt: Long? = null   // null = never synced; stamped after successful push

@ColumnInfo(name = "is_deleted")
val isDeleted: Boolean = false  // soft-delete flag; hidden in UI, pushed to server
```

### 3.2 Delta detection query

A record is included in the next push if:

```sql
SELECT * FROM <table>
WHERE updated_at > COALESCE(synced_at, 0)
-- includes: new records (synced_at IS NULL)
--           edited records (updated_at bumped after last push)
--           soft-deleted records (is_deleted=true, updated_at bumped)
```

### 3.3 Normal DAO queries

All existing DAO queries that return records for the UI must include:

```sql
WHERE is_deleted = 0
```

This hides soft-deleted records without removing them from the database.

---

## 4. Architecture

### 4.1 Layer diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  PRESENTATION                                                    │
│                                                                  │
│  SyncSettingsScreen ──► SyncSettingsViewModel                   │
│  SyncLoginScreen    ──► SyncLoginViewModel                      │
└────────────────────────────┬────────────────────────────────────┘
                             │ calls UseCases
┌────────────────────────────▼────────────────────────────────────┐
│  DOMAIN                                                          │
│                                                                  │
│  LoginUseCase   ──► RemoteDataSource (interface)                │
│  SyncUseCase    ──► RemoteDataSource + SyncDao + SyncMapper     │
└────────────────────────────┬────────────────────────────────────┘
                             │ implements
┌────────────────────────────▼────────────────────────────────────┐
│  DATA                                                            │
│                                                                  │
│  RetrofitRemoteDataSource ──► QReportApi (Retrofit)             │
│  SyncDao                  ──► Room DB                           │
│  SyncMapper               ──► Entity ↔ DTO                      │
│  SyncSettingsDataStore    ──► DataStore (preferences)           │
│  TokenStorage             ──► EncryptedSharedPreferences        │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 OkHttp interceptor chain

```
Request
  │
  ▼
DynamicUrlInterceptor     Rewrites host/scheme from ServerUrlHolder
  │
  ▼
TokenExpiryInterceptor    Clears JWT on 401 response
  │
  ▼
HttpLoggingInterceptor    BASIC level in DEBUG, NONE in release
  │
  ▼
Network
```

### 4.3 Cross-ViewModel communication

`SyncLoginViewModel` and `SyncSettingsViewModel` are independent Hilt ViewModels.
They communicate via `SyncEventBus` (a `@Singleton` `SharedFlow`) to avoid
importing ViewModels into the navigation graph:

```
SyncLoginViewModel
  │  emits SyncEvent.LoginSuccess
  ▼
SyncEventBus (SharedFlow)
  │  collected in init {}
  ▼
SyncSettingsViewModel
  │  calls onLoginSuccess()
  ▼  resets timestamp + triggers full sync
```

---

## 5. Sync Flow

### 5.1 Full session sequence

```
Device                                  Server
  │                                        │
  │  1. Read lastSyncTimestamp (T)         │
  │  2. Build push payload                 │
  │     (all records where                 │
  │      updated_at > synced_at)           │
  │                                        │
  │  POST /sync/push?since=T  ────────────►│
  │  Body: SyncPayload                     │  3. Upsert received records
  │                                        │  4. Pull records changed since T
  │◄────────────────────────────────────── │
  │  Response: SyncResponse                │
  │  { acceptedIds, pulledPayload }        │
  │                                        │
  │  5. Apply pulledPayload to Room        │
  │     (OnConflictStrategy.REPLACE)       │
  │  6. Mark acceptedIds as synced         │
  │     (synced_at = now)                  │
  │  7. Save lastSyncTimestamp = now       │
```

### 5.2 Sync triggers

| Trigger | Behaviour |
|---------|-----------|
| "Sincronizza ora" button | Incremental sync (`since = lastSyncTimestamp`) |
| "Sync completa" button | Reset timestamp to 0 → full pull from server |
| Successful login | Reset timestamp to 0 → full pull (via `SyncEventBus`) |
| App foreground | Auto-sync if `REMOTE_ENABLED` + logged in + ≥ 30 min since last sync |

### 5.3 Conflict resolution

**Last-write-wins** based on `updated_at`:

- Server uses `OnConflictStrategy.REPLACE` (Exposed ORM)
- Device uses `OnConflictStrategy.REPLACE` (Room)
- The record with the highest `updated_at` overwrites the other

This is sufficient for the real-world use case: each robotic island is
maintained by one technician at a time; concurrent edits on the same record
are rare.

### 5.4 Soft delete propagation

```
User deletes record in UI
  │
  ▼
Repository sets is_deleted=true, updatedAt=now
  │
  ▼ (next sync)
Record included in push payload (is_deleted=true)
  │
  ▼
Server stores is_deleted=true
  │
  ▼ (other device pulls)
Room upserts record with is_deleted=true
  │
  ▼
All DAO queries filter WHERE is_deleted=0 → record hidden
```

---

## 6. Authentication

### 6.1 JWT token lifecycle

```
SyncLoginScreen → LoginUseCase → RemoteDataSource
                                      │
                         POST /auth/login
                                      │
                              ◄── JWT token (30 days)
                                      │
                         TokenStorage.saveToken()
                         (EncryptedSharedPreferences)
```

### 6.2 Token expiry

When any API call returns `401 Unauthorized`:

1. `TokenExpiryInterceptor` (OkHttp) intercepts the response
2. Calls `TokenStorage.clearToken()`
3. `SyncSettingsViewModel` detects the error and sets `isLoggedIn = false`
4. UI shows the "Accedi al server" button again

### 6.3 Server URL

The server URL is persisted in `SyncSettingsDataStore` and loaded into
`ServerUrlHolder` (in-memory singleton) at app startup via `QReportApplication`.

`DynamicUrlInterceptor` reads `ServerUrlHolder.baseUrl` on every HTTP request,
so changing the URL in Settings takes effect immediately without restarting
the app. Retrofit is constructed once with a placeholder base URL.

---

## 7. Settings UI

The sync feature is accessible from **Impostazioni → Sincronizzazione**.

### 7.1 Screen states

```
SyncMode = LOCAL_ONLY
  └── Toggle off, no server section visible

SyncMode = REMOTE_ENABLED + not logged in
  └── Server URL field
  └── "Accedi al server" button → navigates to SyncLoginScreen

SyncMode = REMOTE_ENABLED + logged in
  └── Server URL field
  └── "Sincronizza ora" button (with progress indicator while syncing)
  └── "Sync completa" button
  └── "Disconnetti" button
  └── Status section: last sync time, pending changes count
  └── Device section: device UUID
```

### 7.2 ViewModel state

```kotlin
data class SyncSettingsUiState(
    val isLoading: Boolean,
    val isSyncing: Boolean,
    val isLoggedIn: Boolean,
    val syncStatus: SyncStatus?,   // pending count, device id
    val syncResult: SyncResult?,   // pushed/pulled counts after sync
    val error: String?,
    val message: String?
)
```

---

## 8. Key Files Reference

| File | Responsibility |
|------|---------------|
| `SyncDao.kt` | Pending-sync queries, batch upsert, mark-synced updates |
| `SyncSettingsDataStore.kt` | Persists sync mode, last timestamp, device ID, server URL |
| `TokenStorage.kt` | Encrypted JWT storage |
| `ServerUrlHolder.kt` | In-memory URL; initialized from DataStore at startup |
| `DynamicUrlInterceptor.kt` | Rewrites Retrofit base URL per-request |
| `TokenExpiryInterceptor.kt` | Clears token on 401, triggers re-login |
| `QReportApi.kt` | Retrofit: `POST /auth/login`, `POST /sync/push`, `GET /sync/pull` |
| `RemoteDataSource.kt` | Interface isolating network from domain |
| `RetrofitRemoteDataSource.kt` | Maps HTTP responses to `QrResult<T, QrError>` |
| `SyncMapper.kt` | Entity ↔ DTO bidirectional mapping for all 6 entity types |
| `LoginUseCase.kt` | Validates credentials, calls remote, saves token |
| `SyncUseCase.kt` | Builds payload, calls push, applies pull, stamps timestamps |
| `SyncForegroundObserver.kt` | `DefaultLifecycleObserver` on `ProcessLifecycleOwner` |
| `SyncEventBus.kt` | `SharedFlow<SyncEvent>` for cross-ViewModel communication |
| `NetworkModule.kt` | Hilt: provides `OkHttpClient`, `Retrofit`, `QReportApi` |
| `SyncModule.kt` | Hilt: binds `SyncRepository` interface to implementation |

---

## 9. Error Handling

All remote operations return `QrResult<T, QrError>` following the project-wide
error pattern. Relevant network errors:

| `QrError` | Cause | UI response |
|-----------|-------|-------------|
| `NetworkError.NoConnection` | No internet | Snackbar message |
| `NetworkError.Unauthorized` | Token expired or invalid | Clear token, show login |
| `NetworkError.ServerError(code)` | HTTP 4xx/5xx | Snackbar with message |
| `NetworkError.SyncDisabled` | Mode is LOCAL_ONLY | Snackbar message |
| `NetworkError.ParseError` | Malformed response | Snackbar message |

---

## 10. Room Migration

The sync fields were added in **migration 3 → 4**. Every synchronised table
received three new columns via `ALTER TABLE`:

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // clients — already had updated_at, only two new columns
        database.execSQL("ALTER TABLE clients ADD COLUMN synced_at INTEGER")
        database.execSQL("ALTER TABLE clients ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_clients_is_deleted ON clients(is_deleted)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_clients_updated_at ON clients(updated_at)")
        // ... repeated for contacts, contracts, facilities, facility_islands, mechanical_units
    }
}
```

`DATABASE_VERSION` in `QReportApplication` is `4`.

---

*Document: 7_Sync_Android.md — QReport v1.0 — June 2026*