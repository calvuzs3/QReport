package net.calvuz.qreport.app.error.domain.model

import net.calvuz.qreport.ti.domain.model.InterventionStatus

interface QrError {

    sealed interface SystemError : QrError {
        data class UnknownError(val exception: Exception? = null) : SystemError
        data class ExceptionError(val exception: Exception? = null) : SystemError
    }
    
    sealed interface App : QrError {
        data class UnknownError(val message: String? = null) : App
        data class SaveError(val message: QrError? = null) : App
        data class LoadError(val message: QrError? = null) : App
        data class DeleteError(val message: QrError? = null) : App
        data class NotImplemented(val message: QrError? = null) : App
    }
    
    sealed interface ValidationError : QrError {
        data class IdsDoesNotMatch(val message: String? = null) : ValidationError
        data class EmptyField(val message: String? = null) : ValidationError
        data class IsNotActive(val message: String? = null) : ValidationError
        data class IsNotPrimary(val message: String? = null) : ValidationError
        data class EmailAlreadyTaken(val message: String? = null) : ValidationError
        data class PhoneAlreadyTaken(val message: String? = null) : ValidationError
        data class InvalidOperation(val e: QrError? = null) : ValidationError
        
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
        data class ContactDoesNotBelongToClient(val message: String? = null) : ContactsError
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

    sealed interface ExportError : QrError {
        // ===== DIRECTORY OPERATIONS =====
        data class DirectoryCreate(val message: String? = null) : ExportError           // Errore creazione directory export
        data class DirectoryAccess(val message: String? = null) : ExportError           // Errore accesso directory export
        data class DirectoryDelete(val message: String? = null) : ExportError           // Errore eliminazione directory export

        // ===== FILE OPERATIONS =====
        data class FileCreate(val message: String? = null) : ExportError                // Errore creazione file export
        data class FileWrite(val message: String? = null) : ExportError                 // Errore scrittura contenuto export
        data class FileRead(val message: String? = null) : ExportError                  // Errore lettura file export
        data class FileCopy(val message: String? = null) : ExportError                  // Errore copia file per export
        data class FileMove(val message: String? = null) : ExportError                  // Errore spostamento file export
        data class FileDelete(val message: String? = null) : ExportError                // Errore eliminazione file export

        // ===== EXPORT GENERATION =====
        data class ExportGenerationFailed(val message: String? = null) : ExportError    // Errore generale generazione export
        data class ContentSerialization(val message: String? = null) : ExportError      // Errore serializzazione contenuti
        data class FormatNotSupported(val message: String? = null) : ExportError        // Formato export non supportato
        data class TemplateProcessing(val message: String? = null) : ExportError        // Errore elaborazione template

        // ===== VALIDATION =====
        data class ValidationFailed(val message: String? = null) : ExportError          // Errore validazione export
        data class FileCorruption(val message: String? = null) : ExportError            // File export corrotto
        data class IntegrityCheckFailed(val message: String? = null) : ExportError      // Controllo integrità fallito
        data class StructureInvalid(val message: String? = null) : ExportError          // Struttura export non valida

        // ===== STORAGE & SPACE =====
        data class StorageCheckFailed(val message: String? = null) : ExportError        // Errore verifica spazio disponibile
        data class InsufficientStorage(val message: String? = null) : ExportError       // Spazio storage insufficiente
        data class SizeCalculationFailed(val message: String? = null) : ExportError      // Errore calcolo dimensioni
        data class QuotaExceeded(val message: String? = null) : ExportError             // Quota export superata

        // ===== CLEANUP & MAINTENANCE =====
        data class CleanupFailed(val message: String? = null) : ExportError             // Errore pulizia file temporanei
        data class DeleteFailed(val message: String? = null) : ExportError              // Errore eliminazione export
        data class MaintenanceFailed(val message: String? = null) : ExportError         // Errore manutenzione export

        // ===== LISTING & METADATA =====
        data class ListFailed(val message: String? = null) : ExportError                // Errore listing export disponibili
        data class InfoFailed(val message: String? = null) : ExportError                // Errore recupero informazioni export
        data class MetadataFailed(val message: String? = null) : ExportError            // Errore gestione metadata export
        data class IndexCreationFailed(val message: String? = null) : ExportError       // Errore creazione indice export

        // ===== MANIFEST & TRACKING =====
        data class ManifestCreateFailed(val message: String? = null) : ExportError      // Errore creazione manifest export
        data class ManifestReadFailed(val message: String? = null) : ExportError        // Errore lettura manifest export
        data class TrackingFailed(val message: String? = null) : ExportError            // Errore tracking stato export

        // ===== EXPORT PACKAGING =====
        data class CompressionFailed(val message: String? = null) : ExportError         // Errore compressione export
        data class ArchiveCreationFailed(val message: String? = null) : ExportError     // Errore creazione archivio
        data class PackageAssemblyFailed(val message: String? = null) : ExportError     // Errore assemblaggio package export

        // ===== PERMISSIONS & ACCESS =====
        data class PermissionDenied(val message: String? = null) : ExportError          // Permessi insufficienti per export
        data class AccessRestricted(val message: String? = null) : ExportError          // Accesso limitato directory export
        data class WriteProtected(val message: String? = null) : ExportError            // Directory/file protetti in scrittura

        // ===== EXPORT CONFIGURATION =====
        data class ConfigurationInvalid(val message: String? = null) : ExportError      // Configurazione export non valida
        data class OptionsConflict(val message: String? = null) : ExportError           // Conflitto nelle opzioni export
        data class ParameterMissing(val message: String? = null) : ExportError          // Parametro obbligatorio mancante

        // ===== TEMPORARY FILES =====
        data class TempFileFailed(val message: String? = null) : ExportError            // Errore gestione file temporanei
        data class TempCleanupFailed(val message: String? = null) : ExportError         // Errore pulizia file temporanei
        data class TempSpaceFull(val message: String? = null) : ExportError             // Spazio temporaneo esaurito

        // ===== EXTERNAL DEPENDENCIES =====
        data class ExternalToolFailed(val message: String? = null) : ExportError        // Errore tool esterno per export
        data class LibraryError(val message: String? = null) : ExportError              // Errore libreria export (POI, etc.)
        data class SystemResourceUnavailable(val message: String? = null) : ExportError // Risorsa sistema non disponibile

        data class FileShareFailed(val message: String? = null) : ExportError           // Errore di condivisione file
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

    sealed interface PhotoError : QrError {
        // ===== DIRECTORY OPERATIONS =====
        data class DirectoryCreate(val message: String? = null) : PhotoError      // Failed to create photo directory
        data class DirectoryAccess(val message: String? = null) : PhotoError      // Failed to access photo directory
        data class ThumbnailsDirCreate(val message: String? = null) : PhotoError  // Failed to create thumbnails directory

        // ===== FILE OPERATIONS =====
        data class FileCreate(val message: String? = null) : PhotoError           // Failed to create photo file
        data class FileAccess(val message: String? = null) : PhotoError           // Failed to access photo file
        data class FileDelete(val message: String? = null) : PhotoError           // Failed to delete photo file
        data class FileCopy(val message: String? = null) : PhotoError             // Failed to copy photo file
        data class FileMove(val message: String? = null) : PhotoError             // Failed to move photo file

        // ===== PHOTO PROCESSING =====
        data class Save(val message: String? = null) : PhotoError                 // Failed to save photo
        data class Load(val message: String? = null) : PhotoError                 // Failed to load photo
        data class Resize(val message: String? = null) : PhotoError               // Failed to resize photo
        data class Rotate(val message: String? = null) : PhotoError               // Failed to rotate photo
        data class Crop(val message: String? = null) : PhotoError                 // Failed to crop photo

        // ===== THUMBNAIL OPERATIONS =====
        data class ThumbnailCreate(val message: String? = null) : PhotoError      // Failed to create thumbnail
        data class ThumbnailDelete(val message: String? = null) : PhotoError      // Failed to delete thumbnail
        data class ThumbnailAccess(val message: String? = null) : PhotoError      // Failed to access thumbnail

        // ===== METADATA & EXIF =====
        data class MetadataRead(val message: String? = null) : PhotoError         // Failed to read photo metadata
        data class MetadataWrite(val message: String? = null) : PhotoError        // Failed to write photo metadata
        data class ExifRead(val message: String? = null) : PhotoError             // Failed to read EXIF data
        data class ExifWrite(val message: String? = null) : PhotoError            // Failed to write EXIF data
        data class OrientationRead(val message: String? = null) : PhotoError      // Failed to read photo orientation

        // ===== IMAGE PROCESSING =====
        data class Decode(val message: String? = null) : PhotoError               // Failed to decode image
        data class Encode(val message: String? = null) : PhotoError               // Failed to encode image
        data class FormatUnsupported(val message: String? = null) : PhotoError    // Unsupported image format
        data class Compression(val message: String? = null) : PhotoError          // Image compression failed
        data class QualityAdjustment(val message: String? = null) : PhotoError    // Quality adjustment failed

        // ===== VALIDATION =====
        data class Validation(val message: String? = null) : PhotoError           // Photo validation failed
        data class SizeValidation(val message: String? = null) : PhotoError       // Photo size validation failed
        data class FormatValidation(val message: String? = null) : PhotoError     // Photo format validation failed
        data class CorruptionDetected(val message: String? = null) : PhotoError   // Photo file corruption detected

        // ===== STORAGE =====
        data class StorageAccess(val message: String? = null) : PhotoError        // Failed to access storage
        data class StorageFull(val message: String? = null) : PhotoError          // Storage space insufficient
        data class Permissions(val message: String? = null) : PhotoError          // Storage permissions denied

        // ===== IMPORT/EXPORT =====
        data class Import(val message: String? = null) : PhotoError               // Photo import failed
        data class Export(val message: String? = null) : PhotoError               // Photo export failed
        data class UriAccess(val message: String? = null) : PhotoError            // Failed to access photo URI

        // ===== MANAGEMENT =====
        data class List(val message: String? = null) : PhotoError                 // Failed to list photos
        data class Count(val message: String? = null) : PhotoError                // Failed to count photos
        data class Delete(val message: String? = null) : PhotoError               // Failed to delete photo
        data class Cleanup(val message: String? = null) : PhotoError              // Photo cleanup failed

        // ===== CAMERA INTEGRATION =====
        data class CameraAccess(val message: String? = null) : PhotoError         // Camera access failed
        data class Capture(val message: String? = null) : PhotoError              // Photo capture failed
        data class SettingsApply(val message: String? = null) : PhotoError        // Camera settings application failed
    }


    sealed interface BackupError : QrError {
        // ===== BASIC OPERATIONS =====
        data class Save(val message: String? = null) : BackupError               // Failed to save backup
        data class Load(val message: String? = null) : BackupError               // Failed to load backup
        data class Delete(val message: String? = null) : BackupError             // Failed to delete backup
        data class Create(val message: String? = null) : BackupError             // Failed to create backup

        // ===== VALIDATION & INTEGRITY =====
        data class Validate(val message: String? = null) : BackupError           // Backup validation failed
        data class Corrupt(val message: String? = null) : BackupError            // Backup file corrupted
        data class ChecksumMismatch(val message: String? = null) : BackupError   // Checksum validation failed
        data class MetadataMissing(val message: String? = null) : BackupError    // Required metadata missing
        data class StructureInvalid(val message: String? = null) : BackupError   // Backup structure invalid

        // ===== COMPRESSION OPERATIONS =====
        data class ZipCreate(val message: String? = null) : BackupError          // Failed to create ZIP archive
        data class ZipExtract(val message: String? = null) : BackupError         // Failed to extract ZIP archive
        data class ZipCorrupt(val message: String? = null) : BackupError         // ZIP file corrupted
        data class ZipPassword(val message: String? = null) : BackupError        // ZIP password required/invalid

        // ===== SHARING & TRANSFER =====
        data class ShareCreate(val message: String? = null) : BackupError        // Failed to create shareable backup
        data class ExportFailed(val message: String? = null) : BackupError       // Export operation failed
        data class TempFileCreate(val message: String? = null) : BackupError     // Failed to create temporary file

        // ===== PHOTO OPERATIONS =====
        data class PhotoArchive(val message: String? = null) : BackupError       // Photo archiving failed
        data class PhotoExtract(val message: String? = null) : BackupError       // Photo extraction failed
        data class PhotoMissing(val message: String? = null) : BackupError       // Expected photos not found
        data class PhotoCorrupt(val message: String? = null) : BackupError       // Photo file corrupted

        // ===== CLEANUP & MAINTENANCE =====
        data class RetentionPolicy(val message: String? = null) : BackupError    // Retention policy execution failed
        data class CleanupFailed(val message: String? = null) : BackupError      // Cleanup operation failed
        data class DiskSpace(val message: String? = null) : BackupError          // Insufficient disk space

        // ===== SPECIFIC BACKUP SCENARIOS =====
        data class PathGeneration(val message: String? = null) : BackupError     // Failed to generate backup path
        data class PathResolution(val message: String? = null) : BackupError     // Failed to resolve backup path
        data class StatsCalculation(val message: String? = null) : BackupError   // Failed to calculate backup stats
        data class SummaryGeneration(val message: String? = null) : BackupError  // Failed to generate backup summary
    }

    sealed interface Checkup : QrError {
        data class Unknown(val message: String? = null) : Checkup
        data class NotFound(val message: String? = null) : Checkup
        data class CannotDeleteBlockedStatus(val message: String? = null) : Checkup
        data class Load(val message: String? = null) : Checkup
        data class Reload(val message: String? = null) : Checkup
        data class Refresh(val message: String? = null) : Checkup
        data class Create(val message: String? = null) : Checkup
        data class Delete(val message: String? = null) : Checkup
        data class FieldsRequired(val message: String? = null) : Checkup
        data class FileOpen(val message: String? = null) : Checkup
        data class FileShare(val message: String? = null) : Checkup

        data class UpdateStatus(val message: String? = null) : Checkup
        data class UpdateNotes(val message: String? = null) : Checkup
        data class UpdateHeader(val message: String? = null) : Checkup
        data class NotAvailable(val message: String? = null) : Checkup
        data class Association(val message: String? = null) : Checkup
        data class AssociationRemove(val message: String? = null) : Checkup
        data class Finalize(val message: String? = null) : Checkup
        data class Export(val message: String? = null) : Checkup
        data class LoadPhotos(val message: String? = null) : Checkup

        data class InvalidStatusTransition(val message: String? = null) : Checkup

        // Client
        data class ClientLoad(val message: String? = null) : Checkup

        // Facility
        data class FacilityLoad(val message: String? = null) : Checkup

        //Island
        data class IslandLoad(val message: String? = null) : Checkup
    }
}