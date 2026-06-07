package net.calvuz.qreport.app.error.presentation

import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText.*
import net.calvuz.qreport.app.result.domain.QrResult

fun QrError.SystemError.toUiText(): UiText {
    return when (this) {
        is QrError.SystemError.UnknownError -> StringResources(R.string.err_unknown)
        is QrError.SystemError.ExceptionError -> StringResources(R.string.err_exception)
    }
}

fun QrError.FileError.toUiText(): UiText {
    return when (this) {

        // ── Directory operations ──────────────────────────────────────────────
        is QrError.FileError.DirectoryCreateError     -> StringResources(R.string.err_file_directory_create)
        is QrError.FileError.DirectoryAccessError     -> StringResources(R.string.err_file_directory_access)
        is QrError.FileError.DirectoryDeleteError     -> StringResources(R.string.err_file_directory_delete)
        is QrError.FileError.DirectoryNotFound        -> StringResources(R.string.err_file_directory_not_found)
        is QrError.FileError.DirectoryNotEmpty        -> StringResources(R.string.err_file_directory_not_empty)
        is QrError.FileError.DirectoryPermissionDenied -> StringResources(R.string.err_file_directory_permission_denied)

        // ── File operations ───────────────────────────────────────────────────
        is QrError.FileError.FileCreateError          -> StringResources(R.string.err_file_create)
        is QrError.FileError.FileReadError            -> StringResources(R.string.err_file_read)
        is QrError.FileError.FileWriteError           -> StringResources(R.string.err_file_write)
        is QrError.FileError.FileDeleteError          -> StringResources(R.string.err_file_delete)
        is QrError.FileError.FileCopyError            -> StringResources(R.string.err_file_copy)
        is QrError.FileError.FileMoveError            -> StringResources(R.string.err_file_move)
        is QrError.FileError.FileRenameError          -> StringResources(R.string.err_file_rename)
        is QrError.FileError.FileAccessError          -> StringResources(R.string.err_file_access)
        is QrError.FileError.FileNotFound             -> StringResources(R.string.err_file_not_found)
        is QrError.FileError.FileAlreadyExists        -> StringResources(R.string.err_file_already_exists)
        is QrError.FileError.FileLocked               -> StringResources(R.string.err_file_locked)
        is QrError.FileError.FileCorrupted            -> StringResources(R.string.err_file_corrupted)

        // ── Permissions ───────────────────────────────────────────────────────
        is QrError.FileError.PermissionDenied         -> StringResources(R.string.err_file_permission_denied)
        is QrError.FileError.ReadPermissionDenied     -> StringResources(R.string.err_file_read_permission_denied)
        is QrError.FileError.WritePermissionDenied    -> StringResources(R.string.err_file_write_permission_denied)
        is QrError.FileError.ExecutePermissionDenied  -> StringResources(R.string.err_file_execute_permission_denied)

        // ── Storage ───────────────────────────────────────────────────────────
        is QrError.FileError.InsufficientSpace        -> StringResources(R.string.err_file_insufficient_space)
        is QrError.FileError.DiskFull                 -> StringResources(R.string.err_file_disk_full)
        is QrError.FileError.StorageUnavailable       -> StringResources(R.string.err_file_storage_unavailable)
        is QrError.FileError.QuotaExceeded            -> StringResources(R.string.err_file_quota_exceeded)

        // ── File size and limits ──────────────────────────────────────────────
        is QrError.FileError.FileEmpty                -> StringResources(R.string.err_file_empty)
        is QrError.FileError.FileTooLarge             -> StringResources(R.string.err_file_too_large)
        is QrError.FileError.FileTooSmall             -> StringResources(R.string.err_file_too_small)
        is QrError.FileError.SizeLimitExceeded        -> StringResources(R.string.err_file_size_limit_exceeded)
        is QrError.FileError.NameTooLong              -> StringResources(R.string.err_file_name_too_long)
        is QrError.FileError.PathTooLong              -> StringResources(R.string.err_file_path_too_long)

        // ── Format and encoding ───────────────────────────────────────────────
        is QrError.FileError.FormatInvalid            -> StringResources(R.string.err_file_format_invalid)
        is QrError.FileError.EncodingError            -> StringResources(R.string.err_file_encoding_error)
        is QrError.FileError.CharsetUnsupported       -> StringResources(R.string.err_file_charset_unsupported)
        is QrError.FileError.BinaryDataCorrupted      -> StringResources(R.string.err_file_binary_data_corrupted)

        // ── I/O ───────────────────────────────────────────────────────────────
        is QrError.FileError.IoError                  -> StringResources(R.string.err_file_io_error)
        is QrError.FileError.ReadTimeout              -> StringResources(R.string.err_file_read_timeout)
        is QrError.FileError.WriteTimeout             -> StringResources(R.string.err_file_write_timeout)
        is QrError.FileError.OperationInterrupted     -> StringResources(R.string.err_file_operation_interrupted)
        is QrError.FileError.ConcurrentAccess         -> StringResources(R.string.err_file_concurrent_access)

        // ── Network and external storage ──────────────────────────────────────
        is QrError.FileError.NetworkUnavailable       -> StringResources(R.string.err_file_network_unavailable)
        is QrError.FileError.ConnectionLost           -> StringResources(R.string.err_file_connection_lost)
        is QrError.FileError.ExternalStorageRemoved   -> StringResources(R.string.err_file_external_storage_removed)
        is QrError.FileError.DeviceBusy               -> StringResources(R.string.err_file_device_busy)

        // ── Validation ────────────────────────────────────────────────────────
        is QrError.FileError.PathInvalid              -> StringResources(R.string.err_file_path_invalid)
        is QrError.FileError.NameInvalid              -> StringResources(R.string.err_file_name_invalid)
        is QrError.FileError.ExtensionInvalid         -> StringResources(R.string.err_file_extension_invalid)
        is QrError.FileError.ChecksumMismatch         -> StringResources(R.string.err_file_checksum_mismatch)

        // ── Temporary and cache ───────────────────────────────────────────────
        is QrError.FileError.TempFileCreationFailed   -> StringResources(R.string.err_file_temp_file_creation_failed)
        is QrError.FileError.TempDirUnavailable       -> StringResources(R.string.err_file_temp_dir_unavailable)
        is QrError.FileError.CacheWriteFailed         -> StringResources(R.string.err_file_cache_write_failed)
        is QrError.FileError.CleanupFailed            -> StringResources(R.string.err_file_cleanup_failed)

        // ── Locking ───────────────────────────────────────────────────────────
        is QrError.FileError.FileLockByOther          -> StringResources(R.string.err_file_locked_by_other)
        is QrError.FileError.LockAcquisitionFailed    -> StringResources(R.string.err_file_lock_acquisition_failed)
        is QrError.FileError.UnlockFailed             -> StringResources(R.string.err_file_unlock_failed)

        // ── System ────────────────────────────────────────────────────────────
        is QrError.FileError.SystemError              -> StringResources(R.string.err_file_system_error)
        is QrError.FileError.ResourceUnavailable      -> StringResources(R.string.err_file_resource_unavailable)
        is QrError.FileError.HandleExhausted          -> StringResources(R.string.err_file_handle_exhausted)
        is QrError.FileError.FilesystemError          -> StringResources(R.string.err_file_filesystem_error)
        is QrError.FileError.FilesystemReadonly       -> StringResources(R.string.err_file_filesystem_readonly)
    }
}

fun QrError.CreateInterventionError.toUiText(): UiText {
    return when (this) {
        is QrError.CreateInterventionError.MissingCustomerName -> StringResources(R.string.err_create_intervention_missing_customer_name)
        is QrError.CreateInterventionError.MissingSerialNumber -> StringResources(R.string.err_create_intervention_missing_serial_number)
        is QrError.CreateInterventionError.MissingTicketNumber -> StringResources(R.string.err_create_intervention_missing_ticket_number)
        is QrError.CreateInterventionError.MissingOrderNumber -> StringResources(R.string.err_create_intervention_missing_order_number)
        is QrError.CreateInterventionError.TooManyTechnicians -> StringResources(R.string.err_create_intervention_too_many_technicians)
        is QrError.CreateInterventionError.CreationFailed -> StringResources(R.string.err_create_intervention_creation_failed)
        is QrError.CreateInterventionError.ClientNotFound -> StringResources(R.string.err_create_intervention_client_not_found)
        is QrError.CreateInterventionError.IslandNotFound -> StringResources(R.string.err_create_intervention_island_not_found)
    }
}

fun QrError.InterventionError.toUiText(): UiText {
    return when (this) {
        is QrError.InterventionError.BatchDeleteError -> StringResources(R.string.err_intervention_batch_delete_failed)
        is QrError.InterventionError.BatchUpdateError -> StringResources(R.string.err_intervention_batch_update_failed)
        is QrError.InterventionError.CannotDeleteArchived -> StringResources(R.string.err_intervention_cannot_delete_archived)
        is QrError.InterventionError.CannotDeleteCompleted -> StringResources(R.string.err_intervention_cannot_delete_completed)
        is QrError.InterventionError.CreateError -> StringResources(R.string.err_intervention_create_failed)
        is QrError.InterventionError.DeleteError -> StringResources(R.string.err_intervention_delete_failed)
        is QrError.InterventionError.DeleteRequiresConfirmation -> StringResources(R.string.err_intervention_delete_requires_confirmation)
        is QrError.InterventionError.ImmutableFieldChanged -> StringResources(R.string.err_intervention_immutable_field_changed)
        is QrError.InterventionError.InvalidId -> StringResources(R.string.err_intervention_invalid_id)
        is QrError.InterventionError.InvalidStatus -> StringResources(R.string.err_intervention_invalid_status)
        is QrError.InterventionError.InvalidStatusTransition -> StringResources(R.string.err_intervention_invalid_status_transition)
        is QrError.InterventionError.LoadError -> StringResources(R.string.err_intervention_load)
        is QrError.InterventionError.NotFound -> StringResources(R.string.err_intervention_not_found)
        is QrError.InterventionError.StatusUpdateNotAllowed -> StringResources(R.string.err_intervention_status_update_not_allowed)
        is QrError.InterventionError.UpdateError -> StringResources(R.string.err_intervention_update_failed)
        is QrError.InterventionError.NoInterventionLoaded -> StringResources(R.string.err_intervention_no_intervention_loaded)
//        is QrError.InterventionError.SignatureError.NotReady -> StringResources(R.string.err_intervention_signature_not_ready)
        is QrError.InterventionError.SignatureError.TechnicianNameRequired -> StringResources(R.string.err_intervention_signature_technician_name_required)
        is QrError.InterventionError.SignatureError.ClientNameRequired -> StringResources(R.string.err_intervention_signature_client_name_required)
        is QrError.InterventionError.SignatureError.TechnicianNameMinLength -> StringResources(R.string.err_intervention_signature_technician_name_min_length)
        is QrError.InterventionError.SignatureError.ClientNameMinLength -> StringResources(R.string.err_intervention_signature_client_name_min_length)

        is QrError.InterventionError.SignatureError.ValidationError -> StringResources(R.string.err_intervention_signature_validation_error)
        is QrError.InterventionError.SignatureError.TechnicianSignatureFailed -> StringResources(R.string.err_intervention_signature_technician_failed)
        is QrError.InterventionError.SignatureError.ClientSignatureFailed -> StringResources(R.string.err_intervention_signature_client_failed)
        is QrError.InterventionError.SignatureError.CustomerSignatureFailed -> StringResources(R.string.err_intervention_signature_client_failed)

        is QrError.InterventionError.DetailError.SaveError -> StringResources(R.string.err_intervention_detail_save_error)
        is QrError.InterventionError.DetailError.UpdateError -> StringResources(R.string.err_intervention_detail_update_error)

        is QrError.InterventionError.WorkDayError.SaveError -> StringResources(R.string.err_intervention_workday_save_error)
        is QrError.InterventionError.WorkDayError.UpdateError -> StringResources(R.string.err_intervention_workday_update_error)

        is QrError.InterventionError.GeneralError.SaveError -> StringResources(R.string.err_intervention_general_save_error)
        is QrError.InterventionError.GeneralError.UpdateError -> StringResources(R.string.err_intervention_general_update_error)
    }
}

fun QrError.App.toUiText(): UiText {
    return when (this) {
        is QrError.App.UnknownError -> StringResources(R.string.err_unknown)
        is QrError.App.SaveError -> StringResources(R.string.err_save)
        is QrError.App.LoadError -> StringResources(R.string.err_load)
        is QrError.App.DeleteError -> StringResources(R.string.err_delete)
        is QrError.App.NotImplemented -> StringResources(R.string.err_not_implemented)
    }
}

fun QrError.ValidationError.toUiText(): UiText {
    return when (this) {
        is QrError.ValidationError.IdsDoesntMatch -> StringResources(R.string.err_validation_ids_does_not_match)
        is QrError.ValidationError.EmptyField -> StringResources(R.string.err_validation_empty_field)
        is QrError.ValidationError.EmailAlreadyTaken -> StringResources(R.string.err_validation_email_already_taken)
        is QrError.ValidationError.PhoneAlreadyTaken -> StringResources(R.string.err_validation_phone_already_taken)
        is QrError.ValidationError.InvalidOperation -> StringResources(R.string.err_validation_invalid_operation)
        is QrError.ValidationError.IsNotActive -> StringResources(R.string.err_validation_is_not_active)
        is QrError.ValidationError.IsNotPrimary -> StringResources(R.string.err_validation_is_not_primary)
    }
}

fun QrError.ClientError.toUiText(): UiText {
    return when (this) {
        is QrError.ClientError.LoadError -> StringResources(R.string.err_client_load)
        is QrError.ClientError.NotFound -> StringResources(R.string.err_client_not_found)
        is QrError.ClientError.CreateError -> StringResources(R.string.err_client_create)
        is QrError.ClientError.UpdateError -> StringResources(R.string.err_client_update)
        is QrError.ClientError.DeleteError -> StringResources(R.string.err_client_delete)
        is QrError.ClientError.InvalidQueryLength -> StringResources(R.string.err_validation_invalid_query_length)
        is QrError.ClientError.MissingCompanyName -> StringResources(R.string.err_client_fields_required)
        is QrError.ClientError.InvalidCompanyName -> StringResources(R.string.err_client_invalid_company_name)
        is QrError.ClientError.CannotDeleteHasActiveFacilities -> StringResources(R.string.err_client_cannot_delete_has_active_facilities)
        is QrError.ClientError.CannotDeleteHasDependencies -> StringResources(
            R.string.err_client_cannot_delete_has_dependencies,
            facilitiesCount,
            contactsCount,
            contractsCount
        )
    }
}

fun QrError.ContractsError.toUiText(): UiText {
    return when (this) {
        is QrError.ContractsError.MissingClientId -> StringResources(R.string.err_contracts_client_id_empty)
        is QrError.ContractsError.ClientNotFound -> StringResources(R.string.err_contracts_client_not_found)
        is QrError.ContractsError.NotFound -> StringResources(R.string.err_contracts_contract_not_found)
        is QrError.ContractsError.DeleteError -> StringResources(R.string.err_delete)
        is QrError.ContractsError.MissingContractId -> StringResources(R.string.err_contracts_contract_id_empty)
        is QrError.ContractsError.CreateError -> StringResources(R.string.err_contracts_create)
        is QrError.ContractsError.LoadError -> StringResources(R.string.err_contracts_load)
        is QrError.ContractsError.UpdateError -> StringResources(R.string.err_contracts_update)
    }
}

fun QrError.ContactsError.toUiText(): UiText {
    return when (this) {

        is QrError.ContactsError.CreateError -> StringResources(R.string.err_contacts_create)
        is QrError.ContactsError.LoadError -> StringResources(R.string.err_contacts_load)
        is QrError.ContactsError.UpdateError -> StringResources(R.string.err_contacts_update)
        is QrError.ContactsError.DeleteError -> StringResources(R.string.err_contacts_delete)
        is QrError.ContactsError.ClientNotFound -> StringResources(R.string.err_contacts_client_not_found)
        is QrError.ContactsError.MissingClientId -> StringResources(R.string.err_contacts_missing_client_id)
        is QrError.ContactsError.MissingContactId -> StringResources(R.string.err_contacts_missing_contact_id)
        is QrError.ContactsError.NotFound -> StringResources(R.string.err_contacts_not_found)
        is QrError.ContactsError.UnknownContactMethodOnDB -> StringResources(R.string.err_contacts_unknown_contact_method)
        is QrError.ContactsError.ContactIsNotActive -> StringResources(R.string.err_contacts_contact_is_not_active)
        is QrError.ContactsError.ContactDoesntBelongToClient -> StringResources(R.string.err_contact_contact_does_not_belong_to_client)
        is QrError.ContactsError.IsNotPrimary -> StringResources(R.string.err_contact_is_not_primary)
        is QrError.ContactsError.CannotChangeClientAssociation -> StringResources(R.string.err_contact_cannot_change_client_association)
        is QrError.ContactsError.CannotRemovePrimaryFlag -> StringResources(R.string.err_contact_cannot_remove_primary_flag)

        is QrError.ContactsError.ValidationError.InvalidIdClient -> StringResources(R.string.err_contact_contact_id_empty)
        is QrError.ContactsError.ValidationError.InvalidContactLastNameLength -> StringResources(R.string.err_contact_validation_invalid_contact_name_length)
        is QrError.ContactsError.ValidationError.InvalidContactNameLength -> StringResources(R.string.err_contact_validation_invalid_contact_last_name_length)
        is QrError.ContactsError.ValidationError.InvalidDepartmentLength -> StringResources(R.string.err_contact_validation_invalid_department_length)
        is QrError.ContactsError.ValidationError.InvalidEmail -> StringResources(R.string.err_contact_validation_invalid_email)
        is QrError.ContactsError.ValidationError.InvalidMobile -> StringResources(R.string.err_contact_validation_invalid_mobile)
        is QrError.ContactsError.ValidationError.InvalidPhone -> StringResources(R.string.err_contact_validation_invalid_phone)
        is QrError.ContactsError.ValidationError.InvalidRoleLength -> StringResources(R.string.err_contact_validation_invalid_role_length)
        is QrError.ContactsError.ValidationError.InvalidTitleLength -> StringResources(R.string.err_contact_validation_invalid_title_length)
        is QrError.ContactsError.ValidationError.InvalidContactInfo -> StringResources(R.string.err_contact_validation_invalid_contact_info)

    }
}

fun QrError.FacilityError.toUiText(): UiText {
    return when (this) {
        is QrError.FacilityError.NotFound -> StringResource(R.string.err_facility_not_found)
        is QrError.FacilityError.LoadError -> StringResource(R.string.err_facility_load)
        is QrError.FacilityError.CreateError -> StringResource(R.string.err_facility_create)
        is QrError.FacilityError.UpdateError -> StringResource(R.string.err_facility_update)
        is QrError.FacilityError.DeleteError -> StringResource(R.string.err_facility_delete)
        is QrError.FacilityError.MissingName -> StringResource(R.string.err_facility_missing_name)
        is QrError.FacilityError.MissingClientId -> StringResource(R.string.err_facility_client_not_found)
        is QrError.FacilityError.CannotDeleteLastFacility -> StringResource(R.string.err_facility_cannot_delete_last)
        is QrError.FacilityError.CannotDeleteHasActiveIslands -> StringResource(R.string.err_facility_cannot_delete_has_islands)
        is QrError.FacilityError.ValidationError.InvalidFacilityNameLength -> StringResource(R.string.err_facility_validation_invalid_name_length)
    }
}

fun QrError.IslandError.toUiText(): UiText {
    return when (this) {
        is QrError.IslandError.AlreadyDeleted -> StringResource(R.string.err_island_already_deleted)
        is QrError.IslandError.CannotChangeFacility -> StringResource(R.string.err_island_cannot_change_facility)
        is QrError.IslandError.CannotDeleteMaintenanceOverdue -> StringResource(R.string.err_island_cannot_delete_maintenance)
        is QrError.IslandError.CreateError -> StringResource(R.string.err_island_create)
        is QrError.IslandError.DeleteError -> StringResource(R.string.err_island_delete)
        is QrError.IslandError.FacilityNotFound -> StringResource(R.string.err_island_facility_not_found)
        is QrError.IslandError.InvalidQueryLength -> StringResource(R.string.err_validation_invalid_query_length)
        is QrError.IslandError.InvalidField -> StringResource(R.string.err_island_invalid_field)
        is QrError.IslandError.LoadError -> StringResource(R.string.err_island_load)
        is QrError.IslandError.MissingFacilityId -> StringResource(R.string.err_island_missing_facility)
        is QrError.IslandError.MissingSerialNumber -> StringResource(R.string.err_island_missing_serial)
        is QrError.IslandError.NotFound -> StringResource(R.string.err_island_not_found)
        is QrError.IslandError.UpdateError -> StringResource(R.string.err_island_update)

        is QrError.IslandError.ValidationError.DuplicateSerialNumber -> StringResource(R.string.err_island_validation_duplicate_serial)
        is QrError.IslandError.ValidationError.InvalidCommissioningNumber -> StringResource(R.string.err_island_validation_invalid_commissioning_number)
        is QrError.IslandError.ValidationError.InvalidModelNumber -> StringResource(R.string.err_island_validation_invalid_model_number)
        is QrError.IslandError.ValidationError.InvalidSerialNumber -> StringResource(R.string.err_island_validation_invalid_serial_number)
        is QrError.IslandError.ValidationError.InvalidSerialNumberLength -> StringResource(R.string.err_island_validation_invalid_serial_number_length)
        is QrError.IslandError.ValidationError.InvalidCustomNameLength -> StringResource(R.string.err_island_validation_invalid_custom_name_length)
        is QrError.IslandError.ValidationError.InvalidLocationLength -> StringResource(R.string.err_island_validation_invalid_location_length)
        is QrError.IslandError.ValidationError.InvalidCycleCount -> StringResource(R.string.err_island_validation_invalid_cycle_count)
        is QrError.IslandError.ValidationError.InvalidOperatingHours -> StringResource(R.string.err_island_validation_invalid_operating_hours)
        is QrError.IslandError.ValidationError.InvalidInstallationDate -> StringResource(R.string.err_island_validation_invalid_installation_date)
        is QrError.IslandError.ValidationError.InvalidMaintenanceDate -> StringResource(R.string.err_island_validation_invalid_maintenance_date)
        is QrError.IslandError.ValidationError.InvalidWarrantyDate -> StringResource(R.string.err_island_validation_invalid_warranty_date)
    }
}

fun QrError.MaintenanceLogError.toUiText(): UiText {
    return when (this) {
        is QrError.MaintenanceLogError.MissingDescription -> StringResource(R.string.maint_error_missing_description)
        is QrError.MaintenanceLogError.MissingTechnicianName -> StringResource(R.string.maint_error_missing_technician)
        is QrError.MaintenanceLogError.MissingCustomLabel -> StringResource(R.string.maint_error_missing_custom_label)
        is QrError.MaintenanceLogError.InvalidPerformedAt -> StringResource(R.string.maint_error_invalid_performed_at)
        is QrError.MaintenanceLogError.IslandNotFound -> StringResource(R.string.maint_error_island_not_found)
        is QrError.MaintenanceLogError.UnitNotInIsland -> StringResource(R.string.maint_error_unit_not_in_island)
        is QrError.MaintenanceLogError.CreateError -> StringResource(R.string.maint_error_create)
        is QrError.MaintenanceLogError.LoadError -> StringResource(R.string.maint_error_load)
        is QrError.MaintenanceLogError.UpdateError -> StringResource(R.string.maint_error_update)
    }
}

 fun QrError.IslandDocumentError.toUiText(): UiText {
     return when (this) {
         is QrError.IslandDocumentError.MissingTitle     -> StringResources(R.string.err_document_missing_title)
         is QrError.IslandDocumentError.FileTooLarge     -> StringResources(R.string.err_document_file_too_large)
         is QrError.IslandDocumentError.ParentNotFound   -> StringResources(R.string.err_document_parent_not_found)
         is QrError.IslandDocumentError.ImportFailed     -> StringResources(R.string.err_document_import_failed)
         is QrError.IslandDocumentError.FileNotFound     -> StringResources(R.string.err_document_file_not_found)
         is QrError.IslandDocumentError.NoAppAvailable   -> StringResources(R.string.err_document_no_app_available)
         is QrError.IslandDocumentError.OpenFailed       -> StringResources(R.string.err_document_open_failed)
         is QrError.IslandDocumentError.CreateError      -> StringResources(R.string.err_document_create)
         is QrError.IslandDocumentError.LoadError        -> StringResources(R.string.err_document_load)
         is QrError.IslandDocumentError.UpdateError      -> StringResources(R.string.err_document_update)
         is QrError.IslandDocumentError.DeleteError      -> StringResources(R.string.err_document_delete)
     }
 }

fun QrError.UnitError.toUiText(): UiText {
    return when (this) {
        is QrError.UnitError.NotFound -> StringResource(R.string.err_unit_not_found)
        is QrError.UnitError.LoadError -> StringResource(R.string.err_unit_load)
        is QrError.UnitError.CreateError -> StringResource(R.string.err_unit_create)
        is QrError.UnitError.UpdateError -> StringResource(R.string.err_unit_update)
        is QrError.UnitError.DeleteError -> StringResource(R.string.err_unit_delete)
        is QrError.UnitError.MissingName -> StringResource(R.string.err_unit_missing_name)
        is QrError.UnitError.InvalidField -> StringResource(R.string.err_unit_invalid_field)
        is QrError.UnitError.AlreadyDeleted -> StringResource(R.string.err_unit_already_deleted)
        is QrError.UnitError.IslandNotFound -> StringResource(R.string.err_unit_island_not_found)
    }
}

fun QrError.DatabaseError.toUiText(): UiText {
    return when (this) {
        is QrError.DatabaseError.OperationFailed -> StringResources(R.string.err_db_operation_failed)
        is QrError.DatabaseError.InsertFailed -> StringResources(R.string.err_db_insert_failed)
        is QrError.DatabaseError.UpdateFailed -> StringResources(R.string.err_db_update_failed)
        is QrError.DatabaseError.DeleteFailed -> StringResources(R.string.err_db_delete_failed)
        is QrError.DatabaseError.NotFound -> StringResources(R.string.err_db_not_found)
    }
}

fun QrError.Export.toUiText(): UiText {
    return when (this) {
        is QrError.Export.Validation.CannotExportDraft -> StringResources(R.string.err_export_cannot_export_draft)
    }
}

fun QrError.asUiText(): UiText {
    return when (this) {

        is QrError.SystemError -> this.toUiText()
        is QrError.FileError -> this.toUiText()
        is QrError.DatabaseError -> this.toUiText()
        is QrError.Export -> this.toUiText()
        is QrError.CreateInterventionError -> this.toUiText()
        is QrError.InterventionError -> this.toUiText()
        is QrError.App -> this.toUiText()
        is QrError.ValidationError -> this.toUiText()
        is QrError.ClientError -> this.toUiText()
        is QrError.ContactsError -> this.toUiText()
        is QrError.ContractsError -> this.toUiText()
        is QrError.FacilityError -> this.toUiText()
        is QrError.IslandError -> this.toUiText()
        is QrError.UnitError -> this.toUiText()
        is QrError.MaintenanceLogError -> this.toUiText()
        is QrError.IslandDocumentError -> this.toUiText()

        // Export Error Mappings
        QrError.ExportError.DIRECTORY_CREATE -> StringResources(R.string.err_export_directory_create)
        QrError.ExportError.DIRECTORY_ACCESS -> StringResources(R.string.err_export_directory_access)
        QrError.ExportError.DIRECTORY_DELETE -> StringResources(R.string.err_export_directory_delete)
        QrError.ExportError.FILE_CREATE -> StringResources(R.string.err_export_file_create)
        QrError.ExportError.FILE_WRITE -> StringResources(R.string.err_export_file_write)
        QrError.ExportError.FILE_READ -> StringResources(R.string.err_export_file_read)
        QrError.ExportError.FILE_COPY -> StringResources(R.string.err_export_file_copy)
        QrError.ExportError.FILE_MOVE -> StringResources(R.string.err_export_file_move)
        QrError.ExportError.FILE_DELETE -> StringResources(R.string.err_export_file_delete)
        QrError.ExportError.EXPORT_GENERATION_FAILED -> StringResources(R.string.err_export_generation_failed)
        QrError.ExportError.CONTENT_SERIALIZATION -> StringResources(R.string.err_export_content_serialization)
        QrError.ExportError.FORMAT_NOT_SUPPORTED -> StringResources(R.string.err_export_format_not_supported)
        QrError.ExportError.TEMPLATE_PROCESSING -> StringResources(R.string.err_export_template_processing)
        QrError.ExportError.VALIDATION_FAILED -> StringResources(R.string.err_export_validation_failed)
        QrError.ExportError.FILE_CORRUPTION -> StringResources(R.string.err_export_file_corruption)
        QrError.ExportError.INTEGRITY_CHECK_FAILED -> StringResources(R.string.err_export_integrity_check_failed)
        QrError.ExportError.STRUCTURE_INVALID -> StringResources(R.string.err_export_structure_invalid)
        QrError.ExportError.STORAGE_CHECK_FAILED -> StringResources(R.string.err_export_storage_check_failed)
        QrError.ExportError.INSUFFICIENT_STORAGE -> StringResources(R.string.err_export_insufficient_storage)
        QrError.ExportError.SIZE_CALCULATION_FAILED -> StringResources(R.string.err_export_size_calculation_failed)
        QrError.ExportError.QUOTA_EXCEEDED -> StringResources(R.string.err_export_quota_exceeded)
        QrError.ExportError.CLEANUP_FAILED -> StringResources(R.string.err_export_cleanup_failed)
        QrError.ExportError.DELETE_FAILED -> StringResources(R.string.err_export_delete_failed)
        QrError.ExportError.MAINTENANCE_FAILED -> StringResources(R.string.err_export_maintenance_failed)
        QrError.ExportError.LIST_FAILED -> StringResources(R.string.err_export_list_failed)
        QrError.ExportError.INFO_FAILED -> StringResources(R.string.err_export_info_failed)
        QrError.ExportError.METADATA_FAILED -> StringResources(R.string.err_export_metadata_failed)
        QrError.ExportError.INDEX_CREATION_FAILED -> StringResources(R.string.err_export_index_creation_failed)
        QrError.ExportError.MANIFEST_CREATE_FAILED -> StringResources(R.string.err_export_manifest_create_failed)
        QrError.ExportError.MANIFEST_READ_FAILED -> StringResources(R.string.err_export_manifest_read_failed)
        QrError.ExportError.TRACKING_FAILED -> StringResources(R.string.err_export_tracking_failed)
        QrError.ExportError.COMPRESSION_FAILED -> StringResources(R.string.err_export_compression_failed)
        QrError.ExportError.ARCHIVE_CREATION_FAILED -> StringResources(R.string.err_export_archive_creation_failed)
        QrError.ExportError.PACKAGE_ASSEMBLY_FAILED -> StringResources(R.string.err_export_package_assembly_failed)
        QrError.ExportError.PERMISSION_DENIED -> StringResources(R.string.err_export_permission_denied)
        QrError.ExportError.ACCESS_RESTRICTED -> StringResources(R.string.err_export_access_restricted)
        QrError.ExportError.WRITE_PROTECTED -> StringResources(R.string.err_export_write_protected)
        QrError.ExportError.CONFIGURATION_INVALID -> StringResources(R.string.err_export_configuration_invalid)
        QrError.ExportError.OPTIONS_CONFLICT -> StringResources(R.string.err_export_options_conflict)
        QrError.ExportError.PARAMETER_MISSING -> StringResources(R.string.err_export_parameter_missing)
        QrError.ExportError.TEMP_FILE_FAILED -> StringResources(R.string.err_export_temp_file_failed)
        QrError.ExportError.TEMP_CLEANUP_FAILED -> StringResources(R.string.err_export_temp_cleanup_failed)
        QrError.ExportError.TEMP_SPACE_FULL -> StringResources(R.string.err_export_temp_space_full)
        QrError.ExportError.EXTERNAL_TOOL_FAILED -> StringResources(R.string.err_export_external_tool_failed)
        QrError.ExportError.LIBRARY_ERROR -> StringResources(R.string.err_export_library_error)
        QrError.ExportError.SYSTEM_RESOURCE_UNAVAILABLE -> StringResources(R.string.err_export_system_resource_unavailable)
        QrError.ExportError.FILE_SHARE_FAILED -> StringResources(R.string.err_export_file_share_failed)


        // Share error mappings
        QrError.ShareError.SHARE_FAILED -> StringResources(R.string.err_share_failed)
        QrError.ShareError.INTENT_CREATION_FAILED -> StringResources(R.string.err_share_intent_creation_failed)
        QrError.ShareError.APP_NOT_FOUND -> StringResources(R.string.err_share_app_not_found)
        QrError.ShareError.NO_COMPATIBLE_APP -> StringResources(R.string.err_share_no_compatible_app)
        QrError.ShareError.OPEN_FAILED -> StringResources(R.string.err_share_open_failed)
        QrError.ShareError.FILE_NOT_FOUND -> StringResources(R.string.err_share_file_not_found)
        QrError.ShareError.TEMP_FILE_FAILED -> StringResources(R.string.err_share_temp_file_failed)
        QrError.ShareError.ZIP_CREATION_FAILED -> StringResources(R.string.err_share_zip_creation_failed)
        QrError.ShareError.URI_CREATION_FAILED -> StringResources(R.string.err_share_uri_creation_failed)
        QrError.ShareError.FILEPROVIDER_FAILED -> StringResources(R.string.err_share_file_provider_failed)
        QrError.ShareError.VALIDATION_FAILED -> StringResources(R.string.err_share_validation_failed)
        QrError.ShareError.METADATA_FAILED -> StringResources(R.string.err_share_metadata_failed)
        QrError.ShareError.APP_QUERY_FAILED -> StringResources(R.string.err_share_app_query_failed)
        QrError.ShareError.PERMISSION_DENIED -> StringResources(R.string.err_share_permission_denied)
        QrError.ShareError.CLEANUP_FAILED -> StringResources(R.string.err_share_cleanup_failed)

        // ===== NEW PHOTO ERROR MAPPINGS =====

        // File operations
        QrError.PhotoError.FILE_COPY -> StringResources(R.string.err_photo_file_copy)
        QrError.PhotoError.FILE_MOVE -> StringResources(R.string.err_photo_file_move)

        // Photo processing
        QrError.PhotoError.CROP -> StringResources(R.string.err_photo_crop)

        // Thumbnail operations
        QrError.PhotoError.THUMBNAIL_ACCESS -> StringResources(R.string.err_photo_thumbnail_access)

        // Metadata & EXIF
        QrError.PhotoError.METADATA_WRITE -> StringResources(R.string.err_photo_metadata_write)
        QrError.PhotoError.EXIF_WRITE -> StringResources(R.string.err_photo_exif_write)
        QrError.PhotoError.ORIENTATION_READ -> StringResources(R.string.err_photo_orientation_read)

        // Image processing
        QrError.PhotoError.DECODE -> StringResources(R.string.err_photo_decode)
        QrError.PhotoError.ENCODE -> StringResources(R.string.err_photo_encode)
        QrError.PhotoError.FORMAT_UNSUPPORTED -> StringResources(R.string.err_photo_format_unsupported)
        QrError.PhotoError.COMPRESSION -> StringResources(R.string.err_photo_compression)
        QrError.PhotoError.QUALITY_ADJUSTMENT -> StringResources(R.string.err_photo_quality_adjustment)

        // Validation
        QrError.PhotoError.SIZE_VALIDATION -> StringResources(R.string.err_photo_size_validation)

        // Import/Export
        QrError.PhotoError.IMPORT -> StringResources(R.string.err_photo_import)
        QrError.PhotoError.EXPORT -> StringResources(R.string.err_photo_export)
        QrError.PhotoError.URI_ACCESS -> StringResources(R.string.err_photo_uri_access)

        // Management
        QrError.PhotoError.COUNT -> StringResources(R.string.err_photo_count)

        // Camera integration
        QrError.PhotoError.CAMERA_ACCESS -> StringResources(R.string.err_photo_camera_access)
        QrError.PhotoError.CAPTURE -> StringResources(R.string.err_photo_capture)
        QrError.PhotoError.SETTINGS_APPLY -> StringResources(R.string.err_photo_settings_apply)

        // Directory operations
        QrError.PhotoError.DIRECTORY_CREATE -> StringResources(R.string.err_photo_directory_create)
        QrError.PhotoError.DIRECTORY_ACCESS -> StringResources(R.string.err_photo_directory_access)
        QrError.PhotoError.THUMBNAILS_DIR_CREATE -> StringResources(R.string.err_photo_thumbnails_dir_create)

        // File operations
        QrError.PhotoError.FILE_CREATE -> StringResources(R.string.err_photo_file_create)
        QrError.PhotoError.FILE_ACCESS -> StringResources(R.string.err_photo_file_access)
        QrError.PhotoError.FILE_DELETE -> StringResources(R.string.err_photo_file_delete)

        // Photo processing
        QrError.PhotoError.SAVE -> StringResources(R.string.err_photo_save)
        QrError.PhotoError.LOAD -> StringResources(R.string.err_photo_load)
        QrError.PhotoError.RESIZE -> StringResources(R.string.err_photo_resize)
        QrError.PhotoError.ROTATE -> StringResources(R.string.err_photo_rotate)

        // Thumbnail operations
        QrError.PhotoError.THUMBNAIL_CREATE -> StringResources(R.string.err_photo_thumbnail_create)
        QrError.PhotoError.THUMBNAIL_DELETE -> StringResources(R.string.err_photo_thumbnail_delete)

        // Metadata & EXIF
        QrError.PhotoError.METADATA_READ -> StringResources(R.string.err_photo_metadata_read)
        QrError.PhotoError.EXIF_READ -> StringResources(R.string.err_photo_exif_read)

        // Validation
        QrError.PhotoError.VALIDATION -> StringResources(R.string.err_photo_validation)
        QrError.PhotoError.FORMAT_VALIDATION -> StringResources(R.string.err_photo_format_validation)
        QrError.PhotoError.CORRUPTION_DETECTED -> StringResources(R.string.err_photo_corruption_detected)

        // Storage
        QrError.PhotoError.STORAGE_ACCESS -> StringResources(R.string.err_photo_storage_access)
        QrError.PhotoError.STORAGE_FULL -> StringResources(R.string.err_photo_storage_full)
        QrError.PhotoError.PERMISSIONS -> StringResources(R.string.err_photo_permissions)

        // Management
        QrError.PhotoError.LIST -> StringResources(R.string.err_photo_list)
        QrError.PhotoError.DELETE -> StringResources(R.string.err_photo_delete)
        QrError.PhotoError.CLEANUP -> StringResources(R.string.err_photo_cleanup)


        // ===== NEW BACKUP ERROR MAPPINGS =====

        // Basic operations
        QrError.BackupError.SAVE -> StringResources(R.string.err_backup_save)
        QrError.BackupError.LOAD -> StringResources(R.string.err_backup_load)
        QrError.BackupError.DELETE -> StringResources(R.string.err_backup_delete)
        QrError.BackupError.CREATE -> StringResources(R.string.err_backup_create)

        // Validation & integrity
        QrError.BackupError.VALIDATE -> StringResources(R.string.err_backup_validate)
        QrError.BackupError.CORRUPT -> StringResources(R.string.err_backup_corrupt)
        QrError.BackupError.CHECKSUM_MISMATCH -> StringResources(R.string.err_backup_checksum_mismatch)
        QrError.BackupError.METADATA_MISSING -> StringResources(R.string.err_backup_metadata_missing)
        QrError.BackupError.STRUCTURE_INVALID -> StringResources(R.string.err_backup_structure_invalid)

        // Compression operations
        QrError.BackupError.ZIP_CREATE -> StringResources(R.string.err_backup_zip_create)
        QrError.BackupError.ZIP_EXTRACT -> StringResources(R.string.err_backup_zip_extract)
        QrError.BackupError.ZIP_CORRUPT -> StringResources(R.string.err_backup_zip_corrupt)
        QrError.BackupError.ZIP_PASSWORD -> StringResources(R.string.err_backup_zip_password)

        // Sharing & transfer
        QrError.BackupError.SHARE_CREATE -> StringResources(R.string.err_backup_share_create)
        QrError.BackupError.EXPORT_FAILED -> StringResources(R.string.err_backup_export_failed)
        QrError.BackupError.TEMP_FILE_CREATE -> StringResources(R.string.err_backup_temp_file_create)

        // Photo operations
        QrError.BackupError.PHOTO_ARCHIVE -> StringResources(R.string.err_backup_photo_archive)
        QrError.BackupError.PHOTO_EXTRACT -> StringResources(R.string.err_backup_photo_extract)
        QrError.BackupError.PHOTO_MISSING -> StringResources(R.string.err_backup_photo_missing)
        QrError.BackupError.PHOTO_CORRUPT -> StringResources(R.string.err_backup_photo_corrupt)

        // Cleanup & maintenance
        QrError.BackupError.RETENTION_POLICY -> StringResources(R.string.err_backup_retention_policy)
        QrError.BackupError.CLEANUP_FAILED -> StringResources(R.string.err_backup_cleanup_failed)
        QrError.BackupError.DISK_SPACE -> StringResources(R.string.err_backup_disk_space)

        // Specific backup scenarios
        QrError.BackupError.PATH_GENERATION -> StringResources(R.string.err_backup_path_generation)
        QrError.BackupError.PATH_RESOLUTION -> StringResources(R.string.err_backup_path_resolution)
        QrError.BackupError.STATS_CALCULATION -> StringResources(R.string.err_backup_stats_calculation)
        QrError.BackupError.SUMMARY_GENERATION -> StringResources(R.string.err_backup_summary_generation)

        // ===== NEW SHARE ERROR MAPPINGS =====

        QrError.Checkup.UNKNOWN -> StringResources(R.string.err_checkup_delete_unknown)
        QrError.Checkup.NOT_FOUND -> StringResources(R.string.err_checkup_not_found)
        QrError.Checkup.CANNOT_DELETE_COMPLETED -> StringResources(R.string.err_checkup_delete_cannot_delete_completed)
        QrError.Checkup.CANNOT_DELETE_EXPORTED -> StringResources(R.string.err_checkup_delete_cannot_delete_exported)
        QrError.Checkup.CANNOT_DELETE_ARCHIVED -> StringResources(R.string.err_checkup_delete_cannot_delete_archived)
        QrError.Checkup.LOAD -> StringResources(R.string.err_checkup_load_checkup)
        QrError.Checkup.RELOAD -> StringResources(R.string.err_checkup_reload_checkup)
        QrError.Checkup.CREATE -> StringResources(R.string.err_create)
        QrError.Checkup.DELETE -> StringResources(R.string.err_delete)
        QrError.Checkup.FIELDS_REQUIRED -> StringResources(R.string.err_fields_required)
        QrError.Checkup.REFRESH -> StringResources(R.string.err_refresh)
        QrError.Checkup.FILE_OPEN -> StringResources(R.string.err_file_open)
        QrError.Checkup.FILE_SHARE -> StringResources(R.string.err_share)

        QrError.Checkup.LOAD_PHOTOS -> StringResources(R.string.err_checkup_load_photos)
        QrError.Checkup.UPDATE_STATUS -> StringResources(R.string.err_checkup_update_status)
        QrError.Checkup.UPDATE_NOTES -> StringResources(R.string.err_checkup_update_notes)
        QrError.Checkup.UPDATE_HEADER -> StringResources(R.string.err_checkup_update_header)
        QrError.Checkup.NOT_AVAILABLE -> StringResources(R.string.err_checkup_not_available)
        QrError.Checkup.SPARE_ADD -> StringResources(R.string.err_checkup_spare_add)
        QrError.Checkup.ASSOCIATION -> StringResources(R.string.err_checkup_association)
        QrError.Checkup.ASSOCIATION_REMOVE -> StringResources(R.string.err_checkup_association_remove)
        QrError.Checkup.FINALIZE -> StringResources(R.string.err_checkup_finalize)
        QrError.Checkup.EXPORT -> StringResources(R.string.err_checkup_export)

        QrError.Checkup.INVALID_STATUS_TRANSITION -> StringResources(R.string.err_checkup_invalid_status_transition)

        QrError.Checkup.CLIENT_LOAD -> StringResources(R.string.err_client_load_client)
        QrError.Checkup.FACILITY_LOAD -> StringResources(R.string.err_facility_load_facility)
        QrError.Checkup.ISLAND_LOAD -> StringResources(R.string.err_island_load_island)

        else -> {}
    } as UiText
}

fun QrResult.Error<*, QrError>.asErrorUiText(): UiText {
    return error.asUiText()
}