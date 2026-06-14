package net.calvuz.qreport.app.error.domain.model

import net.calvuz.qreport.ti.domain.model.InterventionStatus

interface QrError {

    sealed interface SystemError : QrError {
        data class UnknownError(val exception: Exception? = null) : SystemError
        data class ExceptionError(val exception: Exception? = null) : SystemError
    }

    sealed interface InterventionError : QrError {
        data class LoadError(val message: String? = null) : InterventionError
        data class NotFound(val message: String? = null) : InterventionError
        data class DeleteError(val message: String? = null) : InterventionError
        data class BatchDeleteError(val message: String? = null) : InterventionError
        data class InvalidId(val message: String? = null) : InterventionError
        data class CannotDeleteCompleted(val message: String? = null) : InterventionError
        data class CannotDeleteArchived(val message: String? = null) : InterventionError
        data class DeleteRequiresConfirmation(val message: String? = null) : InterventionError
        data class CreateError(val message: String? = null) : InterventionError
        data class InvalidStatusTransition(
            val status: InterventionStatus? = null, val newStatus: InterventionStatus? = null
        ) : InterventionError

        data class UpdateError(val message: String? = null) : InterventionError
        data class BatchUpdateError(val message: String? = null) : InterventionError
        data class InvalidStatus(val message: String? = null) : InterventionError
        data class ImmutableFieldChanged(val field: String) : InterventionError
        data class StatusUpdateNotAllowed(val message: String? = null) : InterventionError
        data class NoInterventionLoaded(val message: String? = null) : InterventionError

        sealed interface GeneralError : InterventionError {
            data class UpdateError(val error: QrError) : GeneralError
            data class SaveError(val error: String? = null) : GeneralError
        }

        sealed interface WorkDayError : InterventionError {
            data class UpdateError(val error: QrError) : WorkDayError
            data class SaveError(val error: String? = null) : DetailError
        }

        sealed interface DetailError : InterventionError {
            data class UpdateError(val error: QrError) : DetailError
            data class SaveError(val error: String? = null) : DetailError
        }

        sealed interface SignatureError : InterventionError {
            data class ValidationError(val errors: List<SignatureError> = emptyList()) :
                SignatureError

            //            data class NotReady(val message: String? = null) : SignatureError
            object TechnicianNameRequired : SignatureError
            object ClientNameRequired : SignatureError
            data class TechnicianNameMinLength(val chars: Int) : SignatureError
            data class ClientNameMinLength(val chars: Int) : SignatureError
            data class TechnicianSignatureFailed(val message: String? = null) : SignatureError
            data class ClientSignatureFailed(val message: String? = null) : SignatureError
            data class CustomerSignatureFailed(val message: String? = null) : SignatureError

        }

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
        data class IdsDoesntMatch(val message: String? = null) : ValidationError
        data class EmptyField(val message: String? = null) : ValidationError
        data class IsNotActive(val message: String? = null) : ValidationError
        data class IsNotPrimary(val message: String? = null) : ValidationError
        data class EmailAlreadyTaken(val message: String? = null) : ValidationError
        data class PhoneAlreadyTaken(val message: String? = null) : ValidationError
        data class InvalidOperation(val e: QrError? = null) : ValidationError

    }

    sealed interface ClientError : QrError {

        // ── CRUD ─────────────────────────────────────────────────────────────

        data class LoadError(val message: String? = null) : ClientError
        data class NotFound(val message: String? = null) : ClientError
        data class CreateError(val message: String? = null) : ClientError
        data class UpdateError(val message: String? = null) : ClientError
        data class DeleteError(val message: String? = null) : ClientError

        // ── Validation ───────────────────────────────────────────────────────
        data class InvalidQueryLength(val message: String? = null) : ClientError

        /** Company name is missing */
        data class MissingCompanyName(val message: String? = null) : ClientError

        /** Company name is too short (< 2) or too long (> 255 characters). */
        data class InvalidCompanyName(val message: String? = null) : ClientError

        // ── Business rules ───────────────────────────────────────────────────

        /** Client still owns active facilities; deactivate them before deleting. */
        data class CannotDeleteHasActiveFacilities(val message: String? = null) : ClientError
        data class CannotDeleteHasDependencies(
            val facilitiesCount: Int = 0, val contactsCount: Int = 0, val contractsCount: Int = 0
        ) : ClientError
    }
    
    sealed interface ContractsError : QrError {
        
        // ── CRUD ─────────────────────────────────────────────────────────────
        
        data class NotFound(val message: String? = null) : ContractsError
        data class LoadError(val message: String? = null) : ContractsError
        data class CreateError(val message: String? = null) : ContractsError
        data class UpdateError(val message: String? = null) : ContractsError
        data class DeleteError(val message: String? = null) : ContractsError
        
        // ── Business rules ───────────────────────────────────────────────────
        
        data class MissingClientId(val message: String? = null) : ContractsError
        data class MissingContractId(val message: String? = null) : ContractsError
        data class ClientNotFound(val message: String? = null) : ContractsError
    }

    sealed interface ContactsError : QrError {
        // ── CRUD ─────────────────────────────────────────────────────────────

        data class LoadError(val message: String? = null) : ContactsError
        data class NotFound(val message: String? = null) : ContactsError
        data class CreateError(val message: String? = null) : ContactsError
        data class UpdateError(val message: String? = null) : ContactsError
        data class DeleteError(val message: String? = null) : ContactsError

        // ── Business rules ───────────────────────────────────────────────────

        data class MissingClientId(val message: String? = null) : ContactsError
        data class MissingContactId(val message: String? = null) : ContactsError
        data class ClientNotFound(val message: String? = null) : ContactsError
        data class UnknownContactMethodOnDB(val message: String? = null) : ContactsError
        data class ContactIsNotActive(val message: String? = null) : ContactsError
        data class ContactDoesntBelongToClient(val message: String? = null) : ContactsError
        data class IsNotPrimary(val message: String? = null) : ContactsError
        data class CannotChangeClientAssociation(val message: String? = null) : ContactsError
        data class CannotRemovePrimaryFlag(val message: String? = null) : ContactsError

        sealed interface ValidationError : ContactsError {
            data class InvalidContactNameLength(val message: String? = null) : ValidationError
            data class InvalidContactLastNameLength(val message: String? = null) : ValidationError
            data class InvalidEmail(val message: String? = null) : ValidationError
            data class InvalidPhone(val message: String? = null) : ValidationError
            data class InvalidMobile(val message: String? = null) : ValidationError
            data class InvalidTitleLength(val message: String? = null) : ValidationError
            data class InvalidRoleLength(val message: String? = null) : ValidationError
            data class InvalidDepartmentLength(val message: String? = null) : ValidationError
            data class InvalidContactInfo(val message: String? = null) : ValidationError
            data class InvalidIdClient(val message: String? = null) : ValidationError
        }
    }

    sealed interface FacilityError : QrError {

        // ── CRUD ─────────────────────────────────────────────────────────────────

        data class NotFound(val message: String? = null) : FacilityError
        data class LoadError(val message: String? = null) : FacilityError
        data class CreateError(val message: String? = null) : FacilityError
        data class UpdateError(val message: String? = null) : FacilityError
        data class DeleteError(val message: String? = null) : FacilityError

        // ── Validation ───────────────────────────────────────────────────────────

        data class MissingName(val message: String? = null) : FacilityError
        data class MissingClientId(val message: String? = null) : FacilityError

        // ── Business rules ───────────────────────────────────────────────────────

        data class CannotDeleteLastFacility(val message: String? = null) : FacilityError
        data class CannotDeleteHasActiveIslands(val message: String? = null) : FacilityError

        sealed interface ValidationError : FacilityError {
            data class InvalidFacilityNameLength(val message: String? = null) : ValidationError
        }
    }


    sealed interface IslandError : QrError {

        // ── CRUD ─────────────────────────────────────────────────────────────────

        data class LoadError(val message: String? = null) : IslandError
        data class NotFound(val message: String? = null) : IslandError
        data class CreateError(val message: String? = null) : IslandError
        data class UpdateError(val message: String? = null) : IslandError
        data class DeleteError(val message: String? = null) : IslandError

        // ── Validation ───────────────────────────────────────────────────────────

        data class MissingSerialNumber(val message: String? = null) : IslandError
        data class MissingFacilityId(val message: String? = null) : IslandError
        data class InvalidField(val message: String? = null) : IslandError
        data class InvalidQueryLength(val message: String? = null) : IslandError

        sealed interface ValidationError : IslandError {
            data class DuplicateSerialNumber(val message: String? = null) : IslandError
            data class InvalidModelNumber(val message: String? = null) : IslandError
            data class InvalidCommissioningNumber(val message: String? = null) : IslandError
            data class InvalidSerialNumber(val message: String? = null) : IslandError
            data class InvalidSerialNumberLength(val message: String? = null) : IslandError
            data class InvalidCustomNameLength(val message: String? = null) : IslandError
            data class InvalidLocationLength(val message: String? = null) : IslandError
            data class InvalidOperatingHours(val message: String? = null) : IslandError
            data class InvalidCycleCount(val message: String? = null) : IslandError
            data class InvalidInstallationDate(val message: String? = null) : IslandError
            data class InvalidMaintenanceDate(val message: String? = null) : IslandError
            data class InvalidWarrantyDate(val message: String? = null) : IslandError
        }

        // ── Business rules ───────────────────────────────────────────────────────

        data class FacilityNotFound(val message: String? = null) : IslandError

        /** Island already deleted or inactive. */
        data class AlreadyDeleted(val message: String? = null) : IslandError

        /** Cannot delete: maintenance overdue. */
        data class CannotDeleteMaintenanceOverdue(val message: String? = null) : IslandError

        /** Cannot change facility after creation. */
        data class CannotChangeFacility(val message: String? = null) : IslandError
    }


    sealed interface MaintenanceLogError : QrError {

        // ── Validation ────────────────────────────────────────────────────────────
        /** description field is blank. */
        data class MissingDescription(val message: String? = null) : MaintenanceLogError

        /** technicianName field is blank. */
        data class MissingTechnicianName(val message: String? = null) : MaintenanceLogError

        /** operationType is OTHER but customOperationLabel is blank. */
        data class MissingCustomLabel(val message: String? = null) : MaintenanceLogError

        /** performedAt is set to a future date/time. */
        data class InvalidPerformedAt(val message: String? = null) : MaintenanceLogError

        // ── Business rules ────────────────────────────────────────────────────────
        /** The referenced island does not exist or is inactive. */
        data class IslandNotFound(val message: String? = null) : MaintenanceLogError

        /**
         * The referenced mechanicalUnitId does not belong to the given island,
         * or does not exist.
         */
        data class UnitNotInIsland(val message: String? = null) : MaintenanceLogError

        // ── Persistence ───────────────────────────────────────────────────────────
        data class CreateError(val message: String? = null) : MaintenanceLogError
        data class LoadError(val message: String? = null) : MaintenanceLogError
        data class UpdateError(val message: String? = null) : MaintenanceLogError
    }

    sealed interface IslandDocumentError : QrError {

        // ── Validation ────────────────────────────────────────────────────────
        /** Document title is blank. */
        data class MissingTitle(val message: String? = null) : IslandDocumentError

        /** Imported file exceeds the allowed size limit. */
        data class FileTooLarge(
            val actualBytes: Long, val maxBytes: Long
        ) : IslandDocumentError

        // ── Business rules ────────────────────────────────────────────────────
        /**
         * The referenced parent entity (island, facility, or client) does not
         * exist or is inactive.
         */
        data class ParentNotFound(
            val scope: String,          // DocumentScope.name — avoids circular import
            val id: String? = null
        ) : IslandDocumentError

        // ── File operations ───────────────────────────────────────────────────
        /** Import from content URI failed (copy error, unreadable URI, etc.). */
        data class ImportFailed(val message: String? = null) : IslandDocumentError

        /** Physical file not found on disk (may have been removed externally). */
        data class FileNotFound(val path: String? = null) : IslandDocumentError

        /**
         * No installed app can open this MIME type.
         * UI should show an informative message — not a crash.
         */
        data class NoAppAvailable(val mimeType: String) : IslandDocumentError

        /** FileProvider URI creation or [startActivity] failed. */
        data class OpenFailed(val message: String? = null) : IslandDocumentError

        // ── Persistence ───────────────────────────────────────────────────────
        data class CreateError(val message: String? = null) : IslandDocumentError
        data class LoadError(val message: String? = null) : IslandDocumentError
        data class UpdateError(val message: String? = null) : IslandDocumentError
        data class DeleteError(val message: String? = null) : IslandDocumentError
    }

    sealed interface UnitError : QrError {

        // ── CRUD ─────────────────────────────────────────────────────────────────

        data class LoadError(val message: String? = null) : UnitError
        data class NotFound(val message: String? = null) : UnitError
        data class CreateError(val message: String? = null) : UnitError
        data class UpdateError(val message: String? = null) : UnitError
        data class DeleteError(val message: String? = null) : UnitError

        // ── Validation ───────────────────────────────────────────────────────────

        data class MissingName(val message: String? = null) : UnitError
        data class InvalidField(val message: String? = null) : UnitError

        // ── Business rules ───────────────────────────────────────────────────────

        /** Unit already deleted or inactive. */
        data class AlreadyDeleted(val message: String? = null) : UnitError

        /** Parent island not found or inactive. */
        data class IslandNotFound(val message: String? = null) : UnitError
    }

    sealed interface DatabaseError : QrError {
        data class OperationFailed(val message: String? = null) : DatabaseError
        data class InsertFailed(val message: String? = null) : DatabaseError
        data class UpdateFailed(val message: String? = null) : DatabaseError
        data class DeleteFailed(val message: String? = null) : DatabaseError
        data class NotFound(val message: String? = null) : DatabaseError
    }

    sealed interface Export : QrError {
        sealed interface Validation : Export {
            object CannotExportDraft : Validation
        }
    }

    sealed interface NetworkError : QrError {
        data class NoConnection(val message: String? = null) : NetworkError
        data class ServerError(val code: Int, val message: String? = null) : NetworkError
        data class Unauthorized(val message: String? = null) : NetworkError
        data class Timeout(val message: String? = null) : NetworkError
        data class SyncDisabled(val message: String? = null) : NetworkError
        data class ParseError(val message: String? = null) : NetworkError
    }

    sealed interface FileError : QrError {

        // ── Directory operations ──────────────────────────────────────────────────

        /** mkdirs() returned false or threw. */
        data class DirectoryCreateError(val path: String? = null) : FileError

        /** Directory exists but cannot be accessed. */
        data class DirectoryAccessError(val path: String? = null) : FileError

        /** Directory deletion failed. */
        data class DirectoryDeleteError(val path: String? = null) : FileError

        /** Expected directory was not found. */
        data class DirectoryNotFound(val path: String? = null) : FileError

        /** Deletion refused because directory is not empty. */
        data object DirectoryNotEmpty : FileError

        /** Insufficient permissions to operate on a directory. */
        data object DirectoryPermissionDenied : FileError

        // ── File operations ───────────────────────────────────────────────────────

        /** File creation failed. */
        data class FileCreateError(val path: String? = null) : FileError

        /** File read failed. */
        data class FileReadError(val path: String? = null) : FileError

        /** File write failed. */
        data class FileWriteError(val path: String? = null) : FileError

        /** File deletion failed. */
        data class FileDeleteError(val path: String? = null) : FileError

        /** File copy failed. */
        data class FileCopyError(val source: String? = null, val destination: String? = null) :
            FileError

        /** File move failed. */
        data class FileMoveError(val source: String? = null, val destination: String? = null) :
            FileError

        /** File rename failed. */
        data class FileRenameError(val path: String? = null) : FileError

        /** Generic file access error. */
        data class FileAccessError(val path: String? = null) : FileError

        /** File was not found. */
        data class FileNotFound(val path: String? = null) : FileError

        /** A file with the same name already exists. */
        data class FileAlreadyExists(val path: String? = null) : FileError

        /** File is locked or in use. */
        data class FileLocked(val path: String? = null) : FileError

        /** File content is corrupted. */
        data class FileCorrupted(val path: String? = null) : FileError

        // ── Permissions ───────────────────────────────────────────────────────────

        data object PermissionDenied : FileError
        data object ReadPermissionDenied : FileError
        data object WritePermissionDenied : FileError
        data object ExecutePermissionDenied : FileError

        // ── Storage ───────────────────────────────────────────────────────────────

        /** Not enough free space to complete the operation. */
        data class InsufficientSpace(val requiredBytes: Long? = null) : FileError

        data object DiskFull : FileError
        data object StorageUnavailable : FileError
        data object QuotaExceeded : FileError

        // ── File size & limits ────────────────────────────────────────────────────

        data object FileEmpty : FileError

        data class FileTooLarge(val actualBytes: Long? = null, val maxBytes: Long? = null) :
            FileError

        data class FileTooSmall(val actualBytes: Long? = null, val minBytes: Long? = null) :
            FileError

        data class SizeLimitExceeded(val limitBytes: Long? = null) : FileError

        data class NameTooLong(val name: String? = null, val maxLength: Int? = null) : FileError

        data object PathTooLong : FileError

        // ── Format & encoding ─────────────────────────────────────────────────────

        /** File format does not match the expected type. */
        data class FormatInvalid(val expected: String? = null) : FileError

        data class EncodingError(val charset: String? = null) : FileError

        data class CharsetUnsupported(val charset: String? = null) : FileError

        data object BinaryDataCorrupted : FileError

        // ── I/O ───────────────────────────────────────────────────────────────────

        /** Generic I/O error — use cause for diagnostics. */
        data class IoError(val cause: Exception? = null) : FileError

        data object ReadTimeout : FileError
        data object WriteTimeout : FileError
        data object OperationInterrupted : FileError
        data object ConcurrentAccess : FileError

        // ── Network & external storage ────────────────────────────────────────────

        data object NetworkUnavailable : FileError
        data object ConnectionLost : FileError
        data object ExternalStorageRemoved : FileError
        data object DeviceBusy : FileError

        // ── Validation ────────────────────────────────────────────────────────────

        data class PathInvalid(val path: String? = null) : FileError
        data class NameInvalid(val name: String? = null) : FileError
        data class ExtensionInvalid(val extension: String? = null) : FileError

        /** Computed checksum does not match the expected value. */
        data class ChecksumMismatch(val expected: String? = null, val actual: String? = null) :
            FileError

        // ── Temporary & cache ─────────────────────────────────────────────────────

        data class TempFileCreationFailed(val path: String? = null) : FileError
        data object TempDirUnavailable : FileError
        data object CacheWriteFailed : FileError
        data object CleanupFailed : FileError

        // ── Locking ───────────────────────────────────────────────────────────────

        data class FileLockByOther(val path: String? = null) : FileError
        data object LockAcquisitionFailed : FileError
        data object UnlockFailed : FileError

        // ── System ────────────────────────────────────────────────────────────────

        data class SystemError(val cause: Exception? = null) : FileError
        data object ResourceUnavailable : FileError
        data object HandleExhausted : FileError
        data object FilesystemError : FileError
        data object FilesystemReadonly : FileError
    }
//    enum class FileError : QrError {
//
//        // ===== DIRECTORY OPERATIONS =====
//        DIRECTORY_CREATE,            // Directory creation failed
//        DIRECTORY_ACCESS,            // Directory access failed
//        DIRECTORY_DELETE,            // Directory deletion failed
//        DIRECTORY_NOT_FOUND,         // Directory not found
//        DIRECTORY_NOT_EMPTY,         // Directory not empty during deletion
//        DIRECTORY_PERMISSION_DENIED, // Insufficient permissions for directory
//
//        // ===== FILE OPERATIONS =====
//        FileError.FILE_,                 // File creation failed
//        FILE_READ,                   // File read failed
//        FILE_WRITE,                  // File write failed
//        FILE_DELETE,                 // File deletion failed
//        FILE_COPY,                   // File copy failed
//        FILE_MOVE,                   // File move failed
//        FILE_RENAME,                 // File rename failed
//        FILE_ACCESS,                 // Generic file access error
//        FILE_NOT_FOUND,              // File not found
//        FILE_ALREADY_EXISTS,         // File already exists
//        FILE_LOCKED,                 // File locked / in use
//        FILE_CORRUPTED,              // File corrupted

//
//        // ===== I/O OPERATIONS =====
//        IO_ERRzOR,                    // Generic I/O error

//    }

    // QrError.File has been removed.
    // The two references in CoreFileRepositoryImpl have been migrated:
    //   QrError.File.FILE_NOT_EXISTS  →  QrError.FileError.FILE_NOT_FOUND
    //   QrError.File.IO_ERROR         →  QrError.FileError.IO_ERROR


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

    sealed interface ShareError : QrError {
        // ===== BASIC SHARING =====
        data class ShareFailed(val message: String? = null) : ShareError          // General sharing failure
        data class IntentCreationFailed(val message: String? = null) : ShareError // Failed to create share intent
        data class AppNotFound(val message: String? = null) : ShareError          // Target app not found/available
        data class NoCompatibleApp(val message: String? = null) : ShareError      // No compatible apps found
        data class OpenFailed(val message: String? = null) : ShareError           // Failed to open file

        // ===== FILE OPERATIONS =====
        data class FileNotFound(val message: String? = null) : ShareError         // File doesn't exist
        data class TempFileFailed(val message: String? = null) : ShareError       // Temporary file creation failed
        data class ZipCreationFailed(val message: String? = null) : ShareError    // ZIP archive creation failed

        // ===== URI & PROVIDER =====
        data class UriCreationFailed(val message: String? = null) : ShareError    // FileProvider URI creation failed
        data class FileProviderFailed(val message: String? = null) : ShareError   // FileProvider configuration issue

        // ===== VALIDATION =====
        data class ValidationFailed(val message: String? = null) : ShareError     // Share validation failed
        data class MetadataFailed(val message: String? = null) : ShareError       // File metadata extraction failed

        // ===== APP QUERIES =====
        data class AppQueryFailed(val message: String? = null) : ShareError       // Failed to query compatible apps
        data class PermissionDenied(val message: String? = null) : ShareError     // Storage permission denied

        // ===== CLEANUP =====
        data class CleanupFailed(val message: String? = null) : ShareError        // Temporary file cleanup failed
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

    enum class Checkup : QrError {
        UNKNOWN, NOT_FOUND, CANNOT_DELETE_COMPLETED, CANNOT_DELETE_EXPORTED, CANNOT_DELETE_ARCHIVED, LOAD, RELOAD, REFRESH, CREATE, DELETE, FIELDS_REQUIRED, FILE_OPEN, FILE_SHARE,

        UPDATE_STATUS, UPDATE_NOTES, UPDATE_HEADER, NOT_AVAILABLE, SPARE_ADD, ASSOCIATION, ASSOCIATION_REMOVE, FINALIZE, EXPORT, LOAD_PHOTOS,

        INVALID_STATUS_TRANSITION,

        // Client
        CLIENT_LOAD,

        // Facility
        FACILITY_LOAD,

        //Island
        ISLAND_LOAD,

    }
}