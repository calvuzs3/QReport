# 📦 FASE 5 - SISTEMA BACKUP DATABASE QREPORT (Documentazione Finale)

## 🎯 **OBIETTIVI FASE 5**

Sistema completo di backup e ripristino per QReport con:
- ✅ **Backup JSON** del database Room (tutte le tabelle)
- ✅ **Backup foto** con compressione ZIP e integrity check
- ✅ **Backup impostazioni** app (DataStore preferences)
- ✅ **Scelta modalità** backup (locale/cloud) con UI intuitiva
- ✅ **Ripristino completo** con validazione integrità dati

---

## 🗃️ **STRUTTURA DATABASE CORRETTA**

### **9 DAO Reali Identificati in QReportDatabase:**

```kotlin
1. CheckUpDao                  // check_ups
2. CheckItemDao                // check_items  
3. PhotoDao                    // photos
4. SparePartDao                // spare_parts
5. ClientDao                   // clients
6. ContactDao                  // contacts
7. FacilityDao                 // facilities
8. FacilityIslandDao           // facility_islands
9. CheckUpAssociationDao       // checkup_island_associations ✅ CONFERMATO
```

### **Relazioni Foreign Key:**

```
clients (1) ──── (0..*) facilities ──── (0..*) facility_islands
    │                                                   │
    │                                                   │
    └── (0..*) contacts                                 │
                                                        │
checkup_island_associations ════════════════════════════┤
    ║                                                   │
    ║ (M:N)                                            │
    ║                                                   │
    ╚══ (0..*) check_ups ──── (0..*) check_items ──── (0..*) photos
                   │
                   └── (0..*) spare_parts
```

### **Tabella Many-to-Many Confermata:**

```kotlin
// CheckUpIslandAssociationEntity
@Entity(tableName = "checkup_island_associations")
data class CheckUpIslandAssociationEntity(
    val id: String,
    val checkupId: String,     // FK → check_ups
    val islandId: String,      // FK → facility_islands
    val createdAt: Long,
    val notes: String?
)
```

**Un CheckUp può ispezionare multiple FacilityIsland nella stessa sessione! 🔧**

---

## 📋 **DOMAIN MODELS BACKUP**

### **BackupData - Modello Principale**

```kotlin
@Serializable
data class BackupData(
    val metadata: BackupMetadata,
    val database: DatabaseBackup,
    val settings: SettingsBackup,
    val photoManifest: PhotoManifest
)

@Serializable
data class BackupMetadata(
    val id: String,
    val timestamp: Instant,
    val appVersion: String,
    val databaseVersion: Int,
    val deviceInfo: DeviceInfo,
    val backupType: BackupType,
    val totalSize: Long,
    val checksum: String
)

@Serializable
data class DeviceInfo(
    val model: String,
    val osVersion: String,
    val appBuild: String,
    val locale: String
)

enum class BackupType {
    FULL,         // Backup completo
    INCREMENTAL,  // Solo modifiche (future)
    SELECTIVE     // Solo tabelle selezionate (future)
}
```

### **DatabaseBackup - Tutte le Tabelle**

```kotlin
@Serializable
data class DatabaseBackup(
    // ===== CORE CHECKUP =====
    val checkUps: List<CheckUp>,
    val checkItems: List<CheckItem>,
    val photos: List<Photo>,
    val spareParts: List<SparePart>,
    
    // ===== CLIENT MANAGEMENT =====
    val clients: List<Client>,
    val contacts: List<Contact>,
    val facilities: List<Facility>,
    val facilityIslands: List<FacilityIsland>,
    
    // ===== ASSOCIATIONS =====
    val checkUpAssociations: List<CheckUpAssociation>, // ✅ AGGIUNTO
    
    // ===== METADATA =====
    val exportedAt: Instant
) {
    
    /**
     * Conta totale record per validazione
     */
    fun getTotalRecordCount(): Int {
        return checkUps.size + 
               checkItems.size + 
               photos.size + 
               spareParts.size +
               clients.size + 
               contacts.size + 
               facilities.size + 
               facilityIslands.size +
               checkUpAssociations.size  // ✅ INCLUSO
    }
}
```

### **CheckUpAssociation - Domain Model**

```kotlin
@Serializable
data class CheckUpAssociation(
    val id: String,
    val checkUpId: String,
    val facilityIslandId: String,
    val createdAt: Instant,
    val notes: String? = null
)
```

### **SettingsBackup & PhotoManifest**

```kotlin
@Serializable
data class SettingsBackup(
    val preferences: Map<String, String>,
    val userSettings: Map<String, String>,
    val backupDateTime: Instant
)

@Serializable
data class PhotoManifest(
    val totalPhotos: Int,
    val totalSizeMB: Double,
    val photos: List<PhotoBackupInfo>,
    val includesThumbnails: Boolean
)

@Serializable
data class PhotoBackupInfo(
    val checkItemId: String,
    val fileName: String,
    val relativePath: String,
    val sizeBytes: Long,
    val sha256Hash: String,
    val hasThumbnail: Boolean
)
```

---

## 🏗️ **ARCHITETTURA CLEAN**

### **Domain Layer - Repository Interfaces**

```kotlin
// domain/repository/BackupRepository.kt
interface BackupRepository {
    
    // ===== BACKUP OPERATIONS =====
    suspend fun createFullBackup(
        includePhotos: Boolean = true,
        includeThumbnails: Boolean = false,
        backupMode: BackupMode = BackupMode.LOCAL
    ): Flow<BackupProgress>
    
    suspend fun restoreFromBackup(
        backupPath: String,
        strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL
    ): Flow<RestoreProgress>
    
    // ===== BACKUP MANAGEMENT =====
    suspend fun getAvailableBackups(): List<BackupInfo>
    suspend fun deleteBackup(backupId: String): Result<Unit>
    suspend fun validateBackup(backupPath: String): BackupValidationResult
    suspend fun getBackupSize(includePhotos: Boolean): Long
}

// domain/repository/DatabaseExportRepository.kt
interface DatabaseExportRepository {
    suspend fun exportAllTables(): DatabaseBackup
    suspend fun importAllTables(databaseBackup: DatabaseBackup): Result<Unit>
    suspend fun validateDatabaseIntegrity(): ValidationResult
    suspend fun clearAllTables(): Result<Unit>
}

// domain/repository/PhotoArchiveRepository.kt
interface PhotoArchiveRepository {
    suspend fun createPhotoArchive(
        outputPath: String,
        includesThumbnails: Boolean = false
    ): Flow<ArchiveProgress>
    
    suspend fun extractPhotoArchive(
        archivePath: String,
        outputDir: String
    ): Flow<ExtractionProgress>
    
    suspend fun generatePhotoManifest(): PhotoManifest
    suspend fun validatePhotoIntegrity(manifest: PhotoManifest): ValidationResult
}
```

### **Domain Layer - Use Cases**

```kotlin
// domain/usecase/backup/CreateBackupUseCase.kt
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val databaseExportRepository: DatabaseExportRepository,
    private val photoArchiveRepository: PhotoArchiveRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        includePhotos: Boolean,
        includeThumbnails: Boolean,
        backupMode: BackupMode
    ): Flow<BackupProgress> = flow {
        
        emit(BackupProgress.InProgress("Inizializzazione backup...", 0f))
        
        // 1. Export database
        emit(BackupProgress.InProgress("Backup database...", 0.1f))
        val databaseBackup = databaseExportRepository.exportAllTables()
        
        // 2. Backup settings
        emit(BackupProgress.InProgress("Backup impostazioni...", 0.2f))
        val settingsBackup = settingsRepository.exportSettings()
        
        // 3. Backup photos (se richiesto)
        var photoManifest: PhotoManifest = PhotoManifest(0, 0.0, emptyList(), false)
        if (includePhotos) {
            photoArchiveRepository.createPhotoArchive(
                outputPath = getPhotoArchivePath(),
                includesThumbnails = includeThumbnails
            ).collect { progress ->
                when (progress) {
                    is ArchiveProgress.InProgress -> {
                        emit(BackupProgress.InProgress(
                            "Backup foto: ${progress.processedFiles}/${progress.totalFiles}",
                            0.3f + (progress.progress * 0.5f)
                        ))
                    }
                    is ArchiveProgress.Completed -> {
                        photoManifest = photoArchiveRepository.generatePhotoManifest()
                    }
                }
            }
        }
        
        // 4. Crea backup finale
        emit(BackupProgress.InProgress("Finalizzazione backup...", 0.9f))
        val backupData = BackupData(
            metadata = createBackupMetadata(databaseBackup, photoManifest),
            database = databaseBackup,
            settings = settingsBackup,
            photoManifest = photoManifest
        )
        
        // 5. Salva backup
        val backupId = saveBackupData(backupData, backupMode)
        emit(BackupProgress.Completed(backupId, getBackupPath(backupId), backupData.metadata.totalSize))
    }
}

// domain/usecase/backup/RestoreBackupUseCase.kt  
class RestoreBackupUseCase @Inject constructor(
    private val databaseExportRepository: DatabaseExportRepository,
    private val photoArchiveRepository: PhotoArchiveRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        backupPath: String,
        strategy: RestoreStrategy
    ): Flow<RestoreProgress> = flow {
        
        emit(RestoreProgress.InProgress("Caricamento backup...", 0f))
        
        // 1. Carica backup data
        val backupData = loadBackupData(backupPath)
        
        // 2. Valida integrità
        emit(RestoreProgress.InProgress("Validazione integrità...", 0.1f))
        validateBackupIntegrity(backupData)
        
        // 3. Ripristina database
        emit(RestoreProgress.InProgress("Ripristino database...", 0.2f))
        when (strategy) {
            RestoreStrategy.REPLACE_ALL -> {
                databaseExportRepository.clearAllTables()
                databaseExportRepository.importAllTables(backupData.database)
            }
            RestoreStrategy.MERGE -> {
                // Implementazione merge (future)
            }
            RestoreStrategy.SELECTIVE -> {
                // Implementazione selettiva (future)
            }
        }
        
        // 4. Ripristina foto
        if (backupData.photoManifest.totalPhotos > 0) {
            emit(RestoreProgress.InProgress("Ripristino foto...", 0.5f))
            photoArchiveRepository.extractPhotoArchive(
                archivePath = getPhotoArchivePath(backupPath),
                outputDir = getPhotosDirectory()
            ).collect { progress ->
                when (progress) {
                    is ExtractionProgress.InProgress -> {
                        emit(RestoreProgress.InProgress(
                            "Ripristino foto: ${progress.extractedFiles}/${progress.totalFiles}",
                            0.5f + (progress.progress * 0.3f)
                        ))
                    }
                }
            }
        }
        
        // 5. Ripristina impostazioni
        emit(RestoreProgress.InProgress("Ripristino impostazioni...", 0.9f))
        settingsRepository.importSettings(backupData.settings)
        
        emit(RestoreProgress.Completed(backupData.metadata.id))
    }
}
```

---

## 💾 **DATA LAYER IMPLEMENTATION**

### **DatabaseExporter - Export Transazionale**

```kotlin
// data/backup/DatabaseExporter.kt
@Singleton
class DatabaseExporter @Inject constructor(
    private val database: QReportDatabase,
    private val checkUpDao: CheckUpDao,
    private val checkItemDao: CheckItemDao,
    private val photoDao: PhotoDao,
    private val sparePartDao: SparePartDao,
    private val clientDao: ClientDao,
    private val contactDao: ContactDao,
    private val facilityDao: FacilityDao,
    private val facilityIslandDao: FacilityIslandDao,
    private val checkUpAssociationDao: CheckUpAssociationDao  // ✅ AGGIUNTO
) {
    
    suspend fun exportAllTables(): DatabaseBackup = database.withTransaction {
        
        Timber.d("Inizio export database completo")
        val startTime = System.currentTimeMillis()
        
        // Export con ordine ottimizzato per performance
        val databaseBackup = DatabaseBackup(
            // Core entities
            checkUps = checkUpDao.getAllForBackup(),
            checkItems = checkItemDao.getAllForBackup(),
            photos = photoDao.getAllForBackup(),
            spareParts = sparePartDao.getAllForBackup(),
            
            // Client entities
            clients = clientDao.getAllForBackup(),
            contacts = contactDao.getAllForBackup(),
            facilities = facilityDao.getAllForBackup(),
            facilityIslands = facilityIslandDao.getAllForBackup(),
            
            // Associations ✅ INCLUSO
            checkUpAssociations = checkUpAssociationDao.getAllForBackup()
                .map { it.toDomain() },
            
            exportedAt = Clock.System.now()
        )
        
        val duration = System.currentTimeMillis() - startTime
        Timber.d("Export completato in ${duration}ms - ${databaseBackup.getTotalRecordCount()} record")
        
        databaseBackup
    }
}

// Extension function per conversione Entity → Domain
fun CheckUpIslandAssociationEntity.toDomain() = CheckUpAssociation(
    id = id,
    checkUpId = checkupId,
    facilityIslandId = islandId,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    notes = notes
)
```

### **DatabaseImporter - Import con Validazione**

```kotlin
// data/backup/DatabaseImporter.kt
@Singleton
class DatabaseImporter @Inject constructor(
    private val database: QReportDatabase,
    // ... tutti i DAO
) {
    
    suspend fun importAllTables(databaseBackup: DatabaseBackup): Result<Unit> = try {
        
        database.withTransaction {
            Timber.d("Inizio import database - ${databaseBackup.getTotalRecordCount()} record")
            
            // 1. Clear tabelle nell'ordine FK inverso
            clearTablesInOrder()
            
            // 2. Import nell'ordine delle dipendenze FK
            importInDependencyOrder(databaseBackup)
            
            // 3. Validazione post-import
            validateImportedData(databaseBackup)
        }
        
        Result.success(Unit)
        
    } catch (e: Exception) {
        Timber.e(e, "Errore durante import database")
        Result.failure(e)
    }
    
    private suspend fun clearTablesInOrder() {
        // Ordine inverso FK dependencies
        checkUpAssociationDao.deleteAll()  // ✅ INCLUSO
        photoDao.deleteAll()
        checkItemDao.deleteAll()
        sparePartDao.deleteAll()
        checkUpDao.deleteAll()
        contactDao.deleteAll()
        facilityIslandDao.deleteAll()
        facilityDao.deleteAll()
        clientDao.deleteAll()
    }
    
    private suspend fun importInDependencyOrder(backup: DatabaseBackup) {
        // Ordine FK dependencies
        clientDao.insertAllFromBackup(backup.clients.map { it.toEntity() })
        facilityDao.insertAllFromBackup(backup.facilities.map { it.toEntity() })
        contactDao.insertAllFromBackup(backup.contacts.map { it.toEntity() })
        facilityIslandDao.insertAllFromBackup(backup.facilityIslands.map { it.toEntity() })
        checkUpDao.insertAllFromBackup(backup.checkUps.map { it.toEntity() })
        checkItemDao.insertAllFromBackup(backup.checkItems.map { it.toEntity() })
        photoDao.insertAllFromBackup(backup.photos.map { it.toEntity() })
        sparePartDao.insertAllFromBackup(backup.spareParts.map { it.toEntity() })
        
        // Associations per ultime ✅
        checkUpAssociationDao.insertAllFromBackup(
            backup.checkUpAssociations.map { it.toEntity() }
        )
    }
}

// Extension function per conversione Domain → Entity
fun CheckUpAssociation.toEntity() = CheckUpIslandAssociationEntity(
    id = id,
    checkupId = checkUpId,
    islandId = facilityIslandId,
    createdAt = createdAt.toEpochMilliseconds(),
    notes = notes
)
```

### **Enhanced DAO Methods - Template da Aggiungere**

```kotlin
// Metodi da aggiungere a TUTTI i DAO esistenti

// CheckUpAssociationDao - Metodi backup da AGGIUNGERE ✅
@Dao
interface CheckUpAssociationDao {
    // ... metodi esistenti (dal file fornito)
    
    // 🔧 METODI BACKUP DA AGGIUNGERE:
    
    /**
     * Recupera tutte le associazioni per backup
     */
    @Query("SELECT * FROM checkup_island_associations ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<CheckUpIslandAssociationEntity>
    
    /**
     * Inserisce tutte le associazioni da backup
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(associations: List<CheckUpIslandAssociationEntity>)
    
    /**
     * Cancella tutte le associazioni (per ripristino completo)
     */
    @Query("DELETE FROM checkup_island_associations")
    suspend fun deleteAll()
    
    /**
     * Conta totale associazioni (per validazione)
     */
    @Query("SELECT COUNT(*) FROM checkup_island_associations")
    suspend fun count(): Int
}

// CheckUpDao - Metodi da aggiungere  
@Dao
interface CheckUpDao {
    // ... metodi esistenti
    
    // Metodi backup da aggiungere:
    @Query("SELECT * FROM checkups ORDER BY created_at ASC")  // ✅ Nome tabella corretto
    suspend fun getAllForBackup(): List<CheckUpEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(checkUps: List<CheckUpEntity>)
    
    @Query("DELETE FROM checkups")  // ✅ Nome tabella corretto
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM checkups")  // ✅ Nome tabella corretto
    suspend fun count(): Int
}

// CheckItemDao - Metodi da aggiungere  
@Dao
interface CheckItemDao {
    @Query("SELECT * FROM check_items ORDER BY created_at ASC")
    suspend fun getAllForBackup(): List<CheckItemEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(checkItems: List<CheckItemEntity>)
    
    @Query("DELETE FROM check_items")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM check_items")
    suspend fun count(): Int
}

// PhotoDao - Metodi da aggiungere
@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY taken_at ASC")
    suspend fun getAllForBackup(): List<PhotoEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFromBackup(photos: List<PhotoEntity>)
    
    @Query("DELETE FROM photos")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM photos")
    suspend fun count(): Int
}

// ... stesso pattern per tutti gli altri DAO (SparePart, Client, Contact, Facility, Island)
```

---

## 📱 **PRESENTATION LAYER**

### **BackupScreen - UI Completa**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backupProgress by viewModel.backupProgress.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Backup Database") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Header info
            item {
                BackupHeaderCard(
                    totalBackups = uiState.availableBackups.size,
                    lastBackupDate = uiState.lastBackupDate,
                    estimatedSize = uiState.estimatedBackupSize
                )
            }
            
            // Opzioni backup
            item {
                BackupOptionsCard(
                    includePhotos = uiState.includePhotos,
                    includeThumbnails = uiState.includeThumbnails,
                    backupMode = uiState.backupMode,
                    onTogglePhotos = viewModel::toggleIncludePhotos,
                    onToggleThumbnails = viewModel::toggleIncludeThumbnails,
                    onModeChange = viewModel::updateBackupMode,
                    estimatedSize = uiState.estimatedBackupSize
                )
            }
            
            // Pulsante backup
            item {
                BackupActionCard(
                    isBackupInProgress = backupProgress is BackupProgress.InProgress,
                    backupProgress = backupProgress,
                    onCreateBackup = viewModel::createBackup,
                    onCancelBackup = viewModel::cancelBackup
                )
            }
            
            // Lista backup esistenti
            item {
                Text(
                    text = "Backup Disponibili",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (uiState.availableBackups.isEmpty()) {
                item {
                    EmptyBackupsCard()
                }
            } else {
                items(uiState.availableBackups) { backup ->
                    BackupItemCard(
                        backup = backup,
                        onRestore = { viewModel.restoreBackup(backup.id) },
                        onDelete = { viewModel.deleteBackup(backup.id) },
                        onShare = { viewModel.shareBackup(backup.id) }
                    )
                }
            }
        }
    }
}
```

### **BackupViewModel - State Management**

```kotlin
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val getAvailableBackupsUseCase: GetAvailableBackupsUseCase,
    private val getBackupSizeUseCase: GetBackupSizeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    private val _backupProgress = MutableStateFlow<BackupProgress>(BackupProgress.Idle)
    val backupProgress = _backupProgress.asStateFlow()

    init {
        loadBackupEstimate()
        loadAvailableBackups()
    }

    fun toggleIncludePhotos() {
        _uiState.update { 
            it.copy(includePhotos = !it.includePhotos).also {
                loadBackupEstimate()
            }
        }
    }
    
    fun updateBackupMode(mode: BackupMode) {
        _uiState.update { it.copy(backupMode = mode) }
    }

    fun createBackup() {
        viewModelScope.launch {
            createBackupUseCase(
                includePhotos = uiState.value.includePhotos,
                includeThumbnails = uiState.value.includeThumbnails,
                backupMode = uiState.value.backupMode
            ).collect { progress ->
                _backupProgress.value = progress
                
                if (progress is BackupProgress.Completed) {
                    loadAvailableBackups() // Refresh lista
                }
            }
        }
    }
    
    private fun loadBackupEstimate() {
        viewModelScope.launch {
            val size = getBackupSizeUseCase(uiState.value.includePhotos)
            _uiState.update { it.copy(estimatedBackupSize = size) }
        }
    }
}

data class BackupUiState(
    val includePhotos: Boolean = true,
    val includeThumbnails: Boolean = false,
    val backupMode: BackupMode = BackupMode.LOCAL,
    val estimatedBackupSize: Long = 0L,
    val availableBackups: List<BackupInfo> = emptyList(),
    val lastBackupDate: Instant? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## 🧪 **TESTING STRATEGY**

### **Unit Tests - Domain Layer**

```kotlin
class CreateBackupUseCaseTest {
    
    @Test
    fun `should create complete backup with all tables`() = runTest {
        // Given
        val mockDatabaseBackup = DatabaseBackup(
            checkUps = listOf(mockCheckUp()),
            checkItems = listOf(mockCheckItem()),
            photos = listOf(mockPhoto()),
            spareParts = listOf(mockSparePart()),
            clients = listOf(mockClient()),
            contacts = listOf(mockContact()),
            facilities = listOf(mockFacility()),
            facilityIslands = listOf(mockFacilityIsland()),
            checkUpAssociations = listOf(mockCheckUpAssociation()), // ✅
            exportedAt = Clock.System.now()
        )
        
        coEvery { databaseExportRepository.exportAllTables() } returns mockDatabaseBackup
        
        // When
        val result = createBackupUseCase(
            includePhotos = true,
            includeThumbnails = false,
            backupMode = BackupMode.LOCAL
        ).toList()
        
        // Then
        assertThat(result.last()).isInstanceOf(BackupProgress.Completed::class.java)
        coVerify { databaseExportRepository.exportAllTables() }
    }
}
```

### **Integration Tests - Database**

```kotlin
@HiltAndroidTest
class DatabaseBackupIntegrationTest {
    
    @Test
    fun `should backup and restore all tables correctly`() = runTest {
        // Given - Popola database con dati di test
        populateTestDatabase()
        
        // When - Backup
        val backup = databaseExporter.exportAllTables()
        
        // Clear database
        databaseImporter.clearAllTables()
        
        // Restore
        databaseImporter.importAllTables(backup)
        
        // Then - Verifica integrità
        assertThat(checkUpDao.count()).isEqualTo(backup.checkUps.size)
        assertThat(checkUpAssociationDao.count()).isEqualTo(backup.checkUpAssociations.size)
        // ... verify all tables
    }
}
```

---

## 📊 **FILE SYSTEM STRUCTURE**

### **Struttura Directory Backup**

```
/data/data/net.calvuz.qreport/files/backups/
├── backup_20241220_143022/
│   ├── metadata.json                 # BackupMetadata
│   ├── database.json                 # DatabaseBackup  
│   ├── settings.json                 # SettingsBackup
│   ├── photos.zip                    # Archive foto
│   └── manifest.json                 # PhotoManifest
├── backup_20241219_090015/
└── backup_20241218_183045/

/data/data/net.calvuz.qreport/files/photos/
├── {checkItemId1}/
│   ├── photo_001.jpg
│   ├── photo_002.jpg
│   └── thumbnails/
│       ├── thumb_001.jpg
│       └── thumb_002.jpg
└── {checkItemId2}/

/data/data/net.calvuz.qreport/datastore/
├── settings.preferences_pb            # DataStore preferences
└── user_settings.preferences_pb
```

---

## ⚡ **PERFORMANCE & OPTIMIZATION**

### **Backup Performance**

```kotlin
// Ottimizzazioni per backup grandi
class DatabaseExporter {
    
    suspend fun exportAllTables(): DatabaseBackup = database.withTransaction {
        
        // Parallel fetch per tabelle indipendenti
        val clientsDeferred = async { clientDao.getAllForBackup() }
        val facilitiesDeferred = async { facilityDao.getAllForBackup() }
        val contactsDeferred = async { contactDao.getAllForBackup() }
        
        // Sequential fetch per tabelle con FK dependencies
        val checkUps = checkUpDao.getAllForBackup()
        val associations = checkUpAssociationDao.getAllForBackup() // ✅
        
        DatabaseBackup(
            clients = clientsDeferred.await(),
            facilities = facilitiesDeferred.await(),
            contacts = contactsDeferred.await(),
            checkUps = checkUps,
            checkUpAssociations = associations.map { it.toDomain() }, // ✅
            // ...
        )
    }
}
```

### **Progress Tracking Granulare**

```kotlin
sealed class BackupProgress {
    object Idle : BackupProgress()
    
    data class InProgress(
        val step: String,
        val progress: Float,
        val currentTable: String? = null,
        val processedRecords: Int = 0,
        val totalRecords: Int = 0
    ) : BackupProgress()
    
    data class Completed(
        val backupId: String,
        val backupPath: String,
        val totalSize: Long,
        val duration: Long,
        val tablesBackedUp: Int = 9  // ✅ Aggiornato per 9 tabelle
    ) : BackupProgress()
    
    data class Error(val message: String, val throwable: Throwable? = null) : BackupProgress()
}
```

---

## 🚀 **PIANO DI IMPLEMENTAZIONE - 7 SETTIMANE**

### **📋 Settimana 1 - Foundation (Fase 5.1)**
- ✅ Domain models (BackupData, BackupMetadata, DatabaseBackup)
- ✅ Repository interfaces (BackupRepository, DatabaseExportRepository)
- ✅ Enhanced DAO methods per tutti i 9 DAO
- ✅ Mapping functions (Entity ↔ Domain)

### **📊 Settimana 2 - Database Export/Import (Fase 5.2)**  
- ✅ DatabaseExporter con transazioni
- ✅ DatabaseImporter con validazione
- ✅ JSON serialization (kotlinx.serialization)
- ✅ FK dependency management
- ✅ CheckUpAssociation support ✅

### **📸 Settimana 3 - Photo Archiver (Fase 5.3)**
- ✅ PhotoArchiver con ZIP compression
- ✅ SHA256 hash per integrità
- ✅ Progress tracking dettagliato
- ✅ Thumbnail inclusion opzionale

### **⚙️ Settimana 4 - Settings Backup (Fase 5.4)**
- ✅ SettingsExporter per DataStore
- ✅ Base64 encoding per file binari  
- ✅ User preferences backup
- ✅ Configuration validation

### **🔧 Settimana 5 - Use Cases & Repository (Fase 5.5)**
- ✅ CreateBackupUseCase completo
- ✅ RestoreBackupUseCase con strategie
- ✅ BackupRepositoryImpl coordinator
- ✅ Progress Flow management

### **📱 Settimana 6 - UI Implementation (Fase 5.6)**
- ✅ BackupScreen con Material 3
- ✅ Progress indicators
- ✅ RestoreScreen con validazione
- ✅ Settings integration

### **🔗 Settimana 7 - Integration & Polish (Fase 5.7)**
- ✅ Navigation integration
- ✅ Hilt DI configuration
- ✅ Integration tests
- ✅ Performance optimization
- ✅ Error handling completo

---

## 🎯 **CRITERI DI SUCCESSO**

### **✅ Functional Requirements**
- [ ] Backup completo di tutte le 9 tabelle database
- [ ] Backup foto con integrità SHA256
- [ ] Ripristino transazionale senza perdite
- [ ] UI intuitiva con progress indicators
- [ ] Gestione errori robusta

### **⚡ Performance Requirements**  
- [ ] Backup 1000 record < 5 secondi
- [ ] UI responsive durante operazioni
- [ ] Progress tracking real-time
- [ ] Memory usage < 100MB per backup

### **🧪 Quality Requirements**
- [ ] Test coverage > 85% use cases critici
- [ ] Integration tests per tutti i DAO
- [ ] Zero data loss scenarios
- [ ] Backward compatibility maintenance

### **🔒 Security Requirements**
- [ ] Validazione integrità backup
- [ ] Checksum verification
- [ ] Safe transaction rollback
- [ ] Privacy data protection

---

## 🎉 **CONCLUSIONI**

La **Fase 5 - Sistema Backup Database QReport** è ora completamente documentata con:

- ✅ **9 DAO confermati** incluso CheckUpAssociationDao per relazioni M:N
- ✅ **Architettura Clean** completa con Domain/Data/Presentation layers  
- ✅ **JSON serialization** per portabilità cross-platform
- ✅ **ZIP compression** per ottimizzazione spazio
- ✅ **Progress tracking** granulare per UX ottimale
- ✅ **Transaction safety** per integrità dati garantita
- ✅ **Extensibility** per future features (cloud, encryption, scheduling)

**Ready per implementazione sistemática seguendo il piano 7-settimane! 🚀**