package net.calvuz.qreport.app.error.domain.model

import net.calvuz.qreport.ti.domain.model.InterventionStatus

interface QrError {

    sealed class SystemError : QrError {
        data class Unknown(val exception: Exception? = null) : SystemError()
    }

    sealed interface InterventionError : QrError {
        data class LOAD(val message: String? = null) : InterventionError
        data class NOT_FOUND(val message: String? = null) : InterventionError
        data class DELETE_FAILED(val message: String? = null) : InterventionError
        data class BATCH_DELETE_FAILED(val message: String? = null) : InterventionError
        data class INVALID_ID(val message: String? = null) : InterventionError
        data class CANNOT_DELETE_COMPLETED(val message: String? = null) : InterventionError
        data class CANNOT_DELETE_ARCHIVED(val message: String? = null) : InterventionError
        data class DELETE_REQUIRES_CONFIRMATION(val message: String? = null) : InterventionError
        data class CREATE_FAILED(val message: String? = null) : InterventionError
        data class INVALID_STATUS_TRANSITION(val status: InterventionStatus? = null, val newStatus: InterventionStatus? = null) : InterventionError
        data class UPDATE_FAILED(val message: String? = null) : InterventionError
        data class BATCH_UPDATE_FAILED(val message: String? = null) : InterventionError
        data class INVALID_STATUS(val message: String? = null) : InterventionError
        data class IMMUTABLE_FIELD_CHANGED(val field: String): InterventionError
        data class STATUS_UPDATE_NOT_ALLOWED(val message: String? = null) : InterventionError
    }

    sealed interface CreateInterventionError : QrError {
        data class MissingCustomerName(val message: String? = null) : CreateInterventionError
        data class MissingSerialNumber(val message: String? = null) : CreateInterventionError
        data class MissingTicketNumber(val message: String? = null) : CreateInterventionError
        data class MissingOrderNumber(val message: String? = null) : CreateInterventionError
        data class TooManyTechnicians(val message: String? = null) : CreateInterventionError
        data class CreationFailed(val message: String? = null) : CreateInterventionError
        data class ClientNotFound(val message: String? = null) : CreateInterventionError
        data class IslandNotFound(val message: String? = null) : CreateInterventionError
    }


    sealed interface App : QrError {
        data class UnknownError(val message: String? = null) : App
        data class SaveError(val message: QrError? = null) : App
        data class LoadError(val message: QrError? = null) : App
        data class DeleteError(val message: QrError? = null) : App
        data class NotImplemented(val message: QrError? = null) : App
    }

    sealed interface ValidationError : QrError {
        data class EmptyField(val message: String? = null) : ValidationError
        data class IsNotActive(val message: String? = null) : ValidationError
        data class IsNotPrimary(val message: String? = null) : ValidationError
        data class DuplicateEntry(val message: String? = null) : ValidationError
        data class InvalidOperation(val message: String? = null) : ValidationError

    }

    sealed interface Contracts : QrError {
        data class ClientIdEmpty(val message: String?) : Contracts
        data class ClientNotFound(val message: String?) : Contracts
        data class ContractIdEmpty(val message: String?) : Contracts
        data class ContractNotFound(val message: String?) : Contracts
        data class DeleteError(val exception: Exception) : Contracts

    }

    sealed interface Contacts : QrError {
        sealed interface ValidationError : Contacts {
            data class IdClientMandatory(val message: String? = null) : ValidationError
            data class IdContractMandatory(val message: String? = null) : ValidationError
            data class IdMandatory(val message: String? = null) : ValidationError
        }
    }

    /**
     * Errore relativo alle operazioni di database con Room.
     */
    sealed interface DatabaseError : QrError {
        /**
         * Errore generico durante un'operazione di database.
         * @param message Dettaglio dell'eccezione per il logging.
         */
        data class OperationFailed(val message: String? = null) : DatabaseError

        /**
         * Errore specifico durante un'operazione di inserimento.
         * @param message Dettaglio dell'eccezione per il logging.
         */
        data class InsertFailed(val message: String? = null) : DatabaseError

        /**
         * Errore specifico durante un'operazione di aggiornamento.
         * @param message Dettaglio dell'eccezione per il logging.
         */
        data class UpdateFailed(val message: String? = null) : DatabaseError

        /**
         * Errore specifico durante un'operazione di eliminazione.
         * @param message Dettaglio dell'eccezione per il logging.
         */
        data class DeleteFailed(val message: String? = null) : DatabaseError

        /**
         * L'entità cercata non è stata trovata nel database.
         * @param message Dettaglio dell'entità non trovata per il logging.
         */
        data class NotFound(val message: String? = null) : DatabaseError
    }


    /**
     * Generic file operation error types per operazioni file base
     * Usato da CoreFileRepository e tutte le helper functions dei repository
     */
    enum class FileError : QrError {
        // ===== DIRECTORY OPERATIONS =====
        DIRECTORY_CREATE,           // Errore creazione directory
        DIRECTORY_ACCESS,           // Errore accesso directory
        DIRECTORY_DELETE,           // Errore eliminazione directory
        DIRECTORY_NOT_FOUND,        // Directory non trovata
        DIRECTORY_NOT_EMPTY,        // Directory non vuota durante eliminazione
        DIRECTORY_PERMISSION_DENIED, // Permessi insufficienti per directory

        // ===== FILE OPERATIONS =====
        FILE_CREATE,                // Errore creazione file
        FILE_READ,                  // Errore lettura file
        FILE_WRITE,                 // Errore scrittura file
        FILE_DELETE,                // Errore eliminazione file
        FILE_COPY,                  // Errore copia file
        FILE_MOVE,                  // Errore spostamento file
        FILE_RENAME,                // Errore rinominazione file
        FILE_ACCESS,                // Errore accesso file generico
        FILE_NOT_FOUND,             // File non trovato
        FILE_ALREADY_EXISTS,        // File già esistente
        FILE_LOCKED,                // File bloccato/in uso
        FILE_CORRUPTED,             // File corrotto

        // ===== PERMISSIONS =====
        PERMISSION_DENIED,          // Permessi insufficienti
        READ_PERMISSION_DENIED,     // Permessi lettura negati
        WRITE_PERMISSION_DENIED,    // Permessi scrittura negati
        EXECUTE_PERMISSION_DENIED,  // Permessi esecuzione negati

        // ===== STORAGE ISSUES =====
        INSUFFICIENT_SPACE,        // Spazio insufficiente
        DISK_FULL,                 // Disco pieno
        STORAGE_UNAVAILABLE,       // Storage non disponibile
        QUOTA_EXCEEDED,            // Quota disco superata

        // ===== FILE SIZE & LIMITS =====
        FILE_EMPTY,                // File vuoto
        FILE_TOO_LARGE,            // File troppo grande
        FILE_TOO_SMALL,            // File troppo piccolo
        SIZE_LIMIT_EXCEEDED,       // Limite dimensione superato
        NAME_TOO_LONG,             // Nome file troppo lungo
        PATH_TOO_LONG,             // Path troppo lungo

        // ===== FILE FORMAT & ENCODING =====
        FORMAT_INVALID,            // Formato file non valido
        ENCODING_ERROR,            // Errore encoding/decoding
        CHARSET_UNSUPPORTED,       // Charset non supportato
        BINARY_DATA_CORRUPTED,     // Dati binari corrotti

        // ===== I/O OPERATIONS =====
        IO_ERROR,                  // Errore I/O generico
        READ_TIMEOUT,              // Timeout lettura
        WRITE_TIMEOUT,             // Timeout scrittura
        OPERATION_INTERRUPTED,     // Operazione interrotta
        CONCURRENT_ACCESS,         // Accesso concorrente non permesso

        // ===== NETWORK & EXTERNAL =====
        NETWORK_UNAVAILABLE,       // Rete non disponibile (per file remoti)
        CONNECTION_LOST,           // Connessione persa
        EXTERNAL_STORAGE_REMOVED,  // Storage esterno rimosso
        DEVICE_BUSY,               // Dispositivo occupato

        // ===== VALIDATION =====
        PATH_INVALID,              // Path non valido
        NAME_INVALID,              // Nome file non valido
        EXTENSION_INVALID,         // Estensione non valida
        CHECKSUM_MISMATCH,         // Checksum non corrispondente

        // ===== TEMPORARY & CACHE =====
        TEMP_FILE_CREATION_FAILED, // Creazione file temporaneo fallita
        TEMP_DIR_UNAVAILABLE,      // Directory temporanea non disponibile
        CACHE_WRITE_FAILED,        // Scrittura cache fallita
        CLEANUP_FAILED,            // Pulizia file fallita

        // ===== LOCKING & SYNCHRONIZATION =====
        FILE_LOCKED_BY_OTHER,      // File bloccato da altro processo
        LOCK_ACQUISITION_FAILED,   // Acquisizione lock fallita
        UNLOCK_FAILED,             // Rilascio lock fallito

        // ===== SYSTEM LEVEL =====
        SYSTEM_ERROR,              // Errore di sistema
        RESOURCE_UNAVAILABLE,      // Risorsa non disponibile
        HANDLE_EXHAUSTED,          // Handle file esauriti
        FILESYSTEM_ERROR,          // Errore filesystem
        FILESYSTEM_READONLY,       // Filesystem in sola lettura

        // ===== OPERATION SPECIFIC =====
        COPY_FAILED,               // Copia fallita
        MOVE_FAILED,               // Spostamento fallito
        BACKUP_FAILED,             // Backup fallito
        RESTORE_FAILED,            // Ripristino fallito
        SYNC_FAILED,               // Sincronizzazione fallita

        // ===== METADATA =====
        METADATA_READ_FAILED,      // Lettura metadati fallita
        METADATA_WRITE_FAILED,     // Scrittura metadati fallita
        ATTRIBUTES_ACCESS_FAILED,  // Accesso attributi fallito
        TIMESTAMP_UPDATE_FAILED    // Aggiornamento timestamp fallito
    }

    enum class ExportError : QrError {
        // ===== DIRECTORY OPERATIONS =====
        DIRECTORY_CREATE,           // Errore creazione directory export
        DIRECTORY_ACCESS,           // Errore accesso directory export
        DIRECTORY_DELETE,           // Errore eliminazione directory export

        // ===== FILE OPERATIONS =====
        FILE_CREATE,                // Errore creazione file export
        FILE_WRITE,                 // Errore scrittura contenuto export
        FILE_READ,                  // Errore lettura file export
        FILE_COPY,                  // Errore copia file per export
        FILE_MOVE,                  // Errore spostamento file export
        FILE_DELETE,                // Errore eliminazione file export

        // ===== EXPORT GENERATION =====
        EXPORT_GENERATION_FAILED,   // Errore generale generazione export
        CONTENT_SERIALIZATION,      // Errore serializzazione contenuti
        FORMAT_NOT_SUPPORTED,       // Formato export non supportato
        TEMPLATE_PROCESSING,         // Errore elaborazione template

        // ===== VALIDATION =====
        VALIDATION_FAILED,          // Errore validazione export
        FILE_CORRUPTION,            // File export corrotto
        INTEGRITY_CHECK_FAILED,     // Controllo integrità fallito
        STRUCTURE_INVALID,          // Struttura export non valida

        // ===== STORAGE & SPACE =====
        STORAGE_CHECK_FAILED,       // Errore verifica spazio disponibile
        INSUFFICIENT_STORAGE,       // Spazio storage insufficiente
        SIZE_CALCULATION_FAILED,    // Errore calcolo dimensioni
        QUOTA_EXCEEDED,             // Quota export superata

        // ===== CLEANUP & MAINTENANCE =====
        CLEANUP_FAILED,             // Errore pulizia file temporanei
        DELETE_FAILED,              // Errore eliminazione export
        MAINTENANCE_FAILED,         // Errore manutenzione export

        // ===== LISTING & METADATA =====
        LIST_FAILED,                // Errore listing export disponibili
        INFO_FAILED,                // Errore recupero informazioni export
        METADATA_FAILED,            // Errore gestione metadata export
        INDEX_CREATION_FAILED,      // Errore creazione indice export

        // ===== MANIFEST & TRACKING =====
        MANIFEST_CREATE_FAILED,     // Errore creazione manifest export
        MANIFEST_READ_FAILED,       // Errore lettura manifest export
        TRACKING_FAILED,            // Errore tracking stato export

        // ===== EXPORT PACKAGING =====
        COMPRESSION_FAILED,         // Errore compressione export
        ARCHIVE_CREATION_FAILED,    // Errore creazione archivio
        PACKAGE_ASSEMBLY_FAILED,    // Errore assemblaggio package export

        // ===== PERMISSIONS & ACCESS =====
        PERMISSION_DENIED,          // Permessi insufficienti per export
        ACCESS_RESTRICTED,          // Accesso limitato directory export
        WRITE_PROTECTED,            // Directory/file protetti in scrittura

        // ===== EXPORT CONFIGURATION =====
        CONFIGURATION_INVALID,     // Configurazione export non valida
        OPTIONS_CONFLICT,           // Conflitto nelle opzioni export
        PARAMETER_MISSING,          // Parametro obbligatorio mancante

        // ===== TEMPORARY FILES =====
        TEMP_FILE_FAILED,           // Errore gestione file temporanei
        TEMP_CLEANUP_FAILED,       // Errore pulizia file temporanei
        TEMP_SPACE_FULL,            // Spazio temporaneo esaurito

        // ===== EXTERNAL DEPENDENCIES =====
        EXTERNAL_TOOL_FAILED,      // Errore tool esterno per export
        LIBRARY_ERROR,             // Errore libreria export (POI, etc.)
        SYSTEM_RESOURCE_UNAVAILABLE,// Risorsa sistema non disponibile

        FILE_SHARE_FAILED,          // Errore di condivisione file
    }

    enum class ShareError : QrError {
        // ===== BASIC SHARING =====
        SHARE_FAILED,              // General sharing failure
        INTENT_CREATION_FAILED,    // Failed to create share intent
        APP_NOT_FOUND,             // Target app not found/available
        NO_COMPATIBLE_APP,         // No compatible apps found
        OPEN_FAILED,               // Failed to open file

        // ===== FILE OPERATIONS =====
        FILE_NOT_FOUND,            // File doesn't exist
        TEMP_FILE_FAILED,          // Temporary file creation failed
        ZIP_CREATION_FAILED,       // ZIP archive creation failed

        // ===== URI & PROVIDER =====
        URI_CREATION_FAILED,       // FileProvider URI creation failed
        FILEPROVIDER_FAILED,       // FileProvider configuration issue

        // ===== VALIDATION =====
        VALIDATION_FAILED,         // Share validation failed
        METADATA_FAILED,           // File metadata extraction failed

        // ===== APP QUERIES =====
        APP_QUERY_FAILED,          // Failed to query compatible apps
        PERMISSION_DENIED,         // Storage permission denied

        // ===== CLEANUP =====
        CLEANUP_FAILED             // Temporary file cleanup failed
    }

    enum class PhotoError : QrError {
        // ===== DIRECTORY OPERATIONS =====
        DIRECTORY_CREATE,          // Failed to create photo directory
        DIRECTORY_ACCESS,          // Failed to access photo directory
        THUMBNAILS_DIR_CREATE,     // Failed to create thumbnails directory

        // ===== FILE OPERATIONS =====
        FILE_CREATE,               // Failed to create photo file
        FILE_ACCESS,               // Failed to access photo file
        FILE_DELETE,               // Failed to delete photo file
        FILE_COPY,                 // Failed to copy photo file
        FILE_MOVE,                 // Failed to move photo file

        // ===== PHOTO PROCESSING =====
        SAVE,                      // Failed to save photo
        LOAD,                      // Failed to load photo
        RESIZE,                    // Failed to resize photo
        ROTATE,                    // Failed to rotate photo
        CROP,                      // Failed to crop photo

        // ===== THUMBNAIL OPERATIONS =====
        THUMBNAIL_CREATE,          // Failed to create thumbnail
        THUMBNAIL_DELETE,          // Failed to delete thumbnail
        THUMBNAIL_ACCESS,          // Failed to access thumbnail

        // ===== METADATA & EXIF =====
        METADATA_READ,             // Failed to read photo metadata
        METADATA_WRITE,            // Failed to write photo metadata
        EXIF_READ,                 // Failed to read EXIF data
        EXIF_WRITE,                // Failed to write EXIF data
        ORIENTATION_READ,          // Failed to read photo orientation

        // ===== IMAGE PROCESSING =====
        DECODE,                    // Failed to decode image
        ENCODE,                    // Failed to encode image
        FORMAT_UNSUPPORTED,        // Unsupported image format
        COMPRESSION,               // Image compression failed
        QUALITY_ADJUSTMENT,        // Quality adjustment failed

        // ===== VALIDATION =====
        VALIDATION,                // Photo validation failed
        SIZE_VALIDATION,           // Photo size validation failed
        FORMAT_VALIDATION,         // Photo format validation failed
        CORRUPTION_DETECTED,       // Photo file corruption detected

        // ===== STORAGE =====
        STORAGE_ACCESS,            // Failed to access storage
        STORAGE_FULL,              // Storage space insufficient
        PERMISSIONS,               // Storage permissions denied

        // ===== IMPORT/EXPORT =====
        IMPORT,                    // Photo import failed
        EXPORT,                    // Photo export failed
        URI_ACCESS,                // Failed to access photo URI

        // ===== MANAGEMENT =====
        LIST,                      // Failed to list photos
        COUNT,                     // Failed to count photos
        DELETE,                    // Failed to delete photo
        CLEANUP,                   // Photo cleanup failed

        // ===== CAMERA INTEGRATION =====
        CAMERA_ACCESS,             // Camera access failed
        CAPTURE,                   // Photo capture failed
        SETTINGS_APPLY             // Camera settings application failed
    }


    enum class BackupError : QrError {
        // ===== BASIC OPERATIONS =====
        SAVE,                       // Failed to save backup
        LOAD,                       // Failed to load backup
        DELETE,                     // Failed to delete backup
        CREATE,                     // Failed to create backup

        // ===== VALIDATION & INTEGRITY =====
        VALIDATE,                   // Backup validation failed
        CORRUPT,                    // Backup file corrupted
        CHECKSUM_MISMATCH,         // Checksum validation failed
        METADATA_MISSING,          // Required metadata missing
        STRUCTURE_INVALID,         // Backup structure invalid

        // ===== COMPRESSION OPERATIONS =====
        ZIP_CREATE,                // Failed to create ZIP archive
        ZIP_EXTRACT,               // Failed to extract ZIP archive
        ZIP_CORRUPT,               // ZIP file corrupted
        ZIP_PASSWORD,              // ZIP password required/invalid

        // ===== SHARING & TRANSFER =====
        SHARE_CREATE,              // Failed to create shareable backup
        EXPORT_FAILED,             // Export operation failed
        TEMP_FILE_CREATE,          // Failed to create temporary file

        // ===== PHOTO OPERATIONS =====
        PHOTO_ARCHIVE,             // Photo archiving failed
        PHOTO_EXTRACT,             // Photo extraction failed
        PHOTO_MISSING,             // Expected photos not found
        PHOTO_CORRUPT,             // Photo file corrupted

        // ===== CLEANUP & MAINTENANCE =====
        RETENTION_POLICY,          // Retention policy execution failed
        CLEANUP_FAILED,            // Cleanup operation failed
        DISK_SPACE,                // Insufficient disk space

        // ===== SPECIFIC BACKUP SCENARIOS =====
        PATH_GENERATION,           // Failed to generate backup path
        PATH_RESOLUTION,           // Failed to resolve backup path
        STATS_CALCULATION,         // Failed to calculate backup stats
        SUMMARY_GENERATION         // Failed to generate backup summary
    }

    enum class Share : QrError {
        SHARE,
        CREATE,

    }

    enum class File : QrError {
        OPEN,
        READ,
        COPY,
        MOVE,
        LIST,
        CREATE,
        DELETE,
        NOT_FOUND,
        FILE_NOT_EXISTS,
        GET_FILE_SIZE,
        PROCESSING,
    }

    enum class Network : QrError {
        REQUEST_TIMEOUT,

    }

    enum class Exporting : QrError {
        CANNOT_EXPORT_DRAFT,

    }

    enum class Checkup : QrError {
        UNKNOWN,
        NOT_FOUND,
        CANNOT_DELETE_COMPLETED,
        CANNOT_DELETE_EXPORTED,
        CANNOT_DELETE_ARCHIVED,
        LOAD,
        RELOAD,
        REFRESH,
        CREATE,
        DELETE,
        FIELDS_REQUIRED,
        FILE_OPEN,
        FILE_SHARE,

        UPDATE_STATUS,
        UPDATE_NOTES,
        UPDATE_HEADER,
        NOT_AVAILABLE,
        SPARE_ADD,
        ASSOCIATION,
        ASSOCIATION_REMOVE,
        FINALIZE,
        EXPORT,
        LOAD_PHOTOS,

        INVALID_STATUS_TRANSITION,

        // Client
        CLIENT_LOAD,

        // FACility
        FACILITY_LOAD,

        //Island
        ISLAND_LOAD,

    }
}