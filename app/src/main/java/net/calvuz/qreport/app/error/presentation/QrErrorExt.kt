package net.calvuz.qreport.app.error.presentation

import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText.*

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
        is QrError.ValidationError.IdsDoesNotMatch -> StringResources(R.string.err_validation_ids_does_not_match)
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
        is QrError.ContactsError.ContactDoesNotBelongToClient -> StringResources(R.string.err_contact_contact_does_not_belong_to_client)
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

fun QrError.ContractsError.toUiText(): UiText {
    return when (this) {
        is QrError.ContractsError.NotFound -> StringResources(R.string.err_contracts_contract_not_found)
        is QrError.ContractsError.LoadError -> StringResources(R.string.err_contracts_load)
        is QrError.ContractsError.CreateError -> StringResources(R.string.err_contracts_create)
        is QrError.ContractsError.UpdateError -> StringResources(R.string.err_contracts_update)
        is QrError.ContractsError.DeleteError -> StringResources(R.string.err_delete)
        is QrError.ContractsError.MissingClientId -> StringResources(R.string.err_contracts_client_id_empty)
        is QrError.ContractsError.MissingContractId -> StringResources(R.string.err_contracts_contract_id_empty)
        is QrError.ContractsError.ClientNotFound -> StringResources(R.string.err_contracts_client_not_found)
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

fun QrError.ShareError.toUiText(): UiText {
    return when (this) {
        is QrError.ShareError.ShareFailed -> StringResources(R.string.err_share_failed)
        is QrError.ShareError.IntentCreationFailed -> StringResources(R.string.err_share_intent_creation_failed)
        is QrError.ShareError.AppNotFound -> StringResources(R.string.err_share_app_not_found)
        is QrError.ShareError.NoCompatibleApp -> StringResources(R.string.err_share_no_compatible_app)
        is QrError.ShareError.OpenFailed -> StringResources(R.string.err_share_open_failed)
        is QrError.ShareError.FileNotFound -> StringResources(R.string.err_share_file_not_found)
        is QrError.ShareError.TempFileFailed -> StringResources(R.string.err_share_temp_file_failed)
        is QrError.ShareError.ZipCreationFailed -> StringResources(R.string.err_share_zip_creation_failed)
        is QrError.ShareError.UriCreationFailed -> StringResources(R.string.err_share_uri_creation_failed)
        is QrError.ShareError.FileProviderFailed -> StringResources(R.string.err_share_file_provider_failed)
        is QrError.ShareError.ValidationFailed -> StringResources(R.string.err_share_validation_failed)
        is QrError.ShareError.MetadataFailed -> StringResources(R.string.err_share_metadata_failed)
        is QrError.ShareError.AppQueryFailed -> StringResources(R.string.err_share_app_query_failed)
        is QrError.ShareError.PermissionDenied -> StringResources(R.string.err_share_permission_denied)
        is QrError.ShareError.CleanupFailed -> StringResources(R.string.err_share_cleanup_failed)
    }
}

fun QrError.ExportError.toUiText(): UiText {
    return when (this) {
        is QrError.ExportError.DirectoryCreate -> StringResources(R.string.err_export_directory_create)
        is QrError.ExportError.DirectoryAccess -> StringResources(R.string.err_export_directory_access)
        is QrError.ExportError.DirectoryDelete -> StringResources(R.string.err_export_directory_delete)
        is QrError.ExportError.FileCreate -> StringResources(R.string.err_export_file_create)
        is QrError.ExportError.FileWrite -> StringResources(R.string.err_export_file_write)
        is QrError.ExportError.FileRead -> StringResources(R.string.err_export_file_read)
        is QrError.ExportError.FileCopy -> StringResources(R.string.err_export_file_copy)
        is QrError.ExportError.FileMove -> StringResources(R.string.err_export_file_move)
        is QrError.ExportError.FileDelete -> StringResources(R.string.err_export_file_delete)
        is QrError.ExportError.ExportGenerationFailed -> StringResources(R.string.err_export_generation_failed)
        is QrError.ExportError.ContentSerialization -> StringResources(R.string.err_export_content_serialization)
        is QrError.ExportError.FormatNotSupported -> StringResources(R.string.err_export_format_not_supported)
        is QrError.ExportError.TemplateProcessing -> StringResources(R.string.err_export_template_processing)
        is QrError.ExportError.ValidationFailed -> StringResources(R.string.err_export_validation_failed)
        is QrError.ExportError.FileCorruption -> StringResources(R.string.err_export_file_corruption)
        is QrError.ExportError.IntegrityCheckFailed -> StringResources(R.string.err_export_integrity_check_failed)
        is QrError.ExportError.StructureInvalid -> StringResources(R.string.err_export_structure_invalid)
        is QrError.ExportError.StorageCheckFailed -> StringResources(R.string.err_export_storage_check_failed)
        is QrError.ExportError.InsufficientStorage -> StringResources(R.string.err_export_insufficient_storage)
        is QrError.ExportError.SizeCalculationFailed -> StringResources(R.string.err_export_size_calculation_failed)
        is QrError.ExportError.QuotaExceeded -> StringResources(R.string.err_export_quota_exceeded)
        is QrError.ExportError.CleanupFailed -> StringResources(R.string.err_export_cleanup_failed)
        is QrError.ExportError.DeleteFailed -> StringResources(R.string.err_export_delete_failed)
        is QrError.ExportError.MaintenanceFailed -> StringResources(R.string.err_export_maintenance_failed)
        is QrError.ExportError.ListFailed -> StringResources(R.string.err_export_list_failed)
        is QrError.ExportError.InfoFailed -> StringResources(R.string.err_export_info_failed)
        is QrError.ExportError.MetadataFailed -> StringResources(R.string.err_export_metadata_failed)
        is QrError.ExportError.IndexCreationFailed -> StringResources(R.string.err_export_index_creation_failed)
        is QrError.ExportError.ManifestCreateFailed -> StringResources(R.string.err_export_manifest_create_failed)
        is QrError.ExportError.ManifestReadFailed -> StringResources(R.string.err_export_manifest_read_failed)
        is QrError.ExportError.TrackingFailed -> StringResources(R.string.err_export_tracking_failed)
        is QrError.ExportError.CompressionFailed -> StringResources(R.string.err_export_compression_failed)
        is QrError.ExportError.ArchiveCreationFailed -> StringResources(R.string.err_export_archive_creation_failed)
        is QrError.ExportError.PackageAssemblyFailed -> StringResources(R.string.err_export_package_assembly_failed)
        is QrError.ExportError.PermissionDenied -> StringResources(R.string.err_export_permission_denied)
        is QrError.ExportError.AccessRestricted -> StringResources(R.string.err_export_access_restricted)
        is QrError.ExportError.WriteProtected -> StringResources(R.string.err_export_write_protected)
        is QrError.ExportError.ConfigurationInvalid -> StringResources(R.string.err_export_configuration_invalid)
        is QrError.ExportError.OptionsConflict -> StringResources(R.string.err_export_options_conflict)
        is QrError.ExportError.ParameterMissing -> StringResources(R.string.err_export_parameter_missing)
        is QrError.ExportError.TempFileFailed -> StringResources(R.string.err_export_temp_file_failed)
        is QrError.ExportError.TempCleanupFailed -> StringResources(R.string.err_export_temp_cleanup_failed)
        is QrError.ExportError.TempSpaceFull -> StringResources(R.string.err_export_temp_space_full)
        is QrError.ExportError.ExternalToolFailed -> StringResources(R.string.err_export_external_tool_failed)
        is QrError.ExportError.LibraryError -> StringResources(R.string.err_export_library_error)
        is QrError.ExportError.SystemResourceUnavailable -> StringResources(R.string.err_export_system_resource_unavailable)
        is QrError.ExportError.FileShareFailed -> StringResources(R.string.err_export_file_share_failed)
    }
}

fun QrError.PhotoError.toUiText(): UiText {
    return when (this) {
        // Directory operations
        is QrError.PhotoError.DirectoryCreate -> StringResources(R.string.err_photo_directory_create)
        is QrError.PhotoError.DirectoryAccess -> StringResources(R.string.err_photo_directory_access)
        is QrError.PhotoError.ThumbnailsDirCreate -> StringResources(R.string.err_photo_thumbnails_dir_create)

        // File operations
        is QrError.PhotoError.FileCreate -> StringResources(R.string.err_photo_file_create)
        is QrError.PhotoError.FileAccess -> StringResources(R.string.err_photo_file_access)
        is QrError.PhotoError.FileDelete -> StringResources(R.string.err_photo_file_delete)
        is QrError.PhotoError.FileCopy -> StringResources(R.string.err_photo_file_copy)
        is QrError.PhotoError.FileMove -> StringResources(R.string.err_photo_file_move)

        // Photo processing
        is QrError.PhotoError.Save -> StringResources(R.string.err_photo_save)
        is QrError.PhotoError.Load -> StringResources(R.string.err_photo_load)
        is QrError.PhotoError.Resize -> StringResources(R.string.err_photo_resize)
        is QrError.PhotoError.Rotate -> StringResources(R.string.err_photo_rotate)
        is QrError.PhotoError.Crop -> StringResources(R.string.err_photo_crop)

        // Thumbnail operations
        is QrError.PhotoError.ThumbnailCreate -> StringResources(R.string.err_photo_thumbnail_create)
        is QrError.PhotoError.ThumbnailDelete -> StringResources(R.string.err_photo_thumbnail_delete)
        is QrError.PhotoError.ThumbnailAccess -> StringResources(R.string.err_photo_thumbnail_access)

        // Metadata & EXIF
        is QrError.PhotoError.MetadataRead -> StringResources(R.string.err_photo_metadata_read)
        is QrError.PhotoError.MetadataWrite -> StringResources(R.string.err_photo_metadata_write)
        is QrError.PhotoError.ExifRead -> StringResources(R.string.err_photo_exif_read)
        is QrError.PhotoError.ExifWrite -> StringResources(R.string.err_photo_exif_write)
        is QrError.PhotoError.OrientationRead -> StringResources(R.string.err_photo_orientation_read)

        // Image processing
        is QrError.PhotoError.Decode -> StringResources(R.string.err_photo_decode)
        is QrError.PhotoError.Encode -> StringResources(R.string.err_photo_encode)
        is QrError.PhotoError.FormatUnsupported -> StringResources(R.string.err_photo_format_unsupported)
        is QrError.PhotoError.Compression -> StringResources(R.string.err_photo_compression)
        is QrError.PhotoError.QualityAdjustment -> StringResources(R.string.err_photo_quality_adjustment)

        // Validation
        is QrError.PhotoError.Validation -> StringResources(R.string.err_photo_validation)
        is QrError.PhotoError.SizeValidation -> StringResources(R.string.err_photo_size_validation)
        is QrError.PhotoError.FormatValidation -> StringResources(R.string.err_photo_format_validation)
        is QrError.PhotoError.CorruptionDetected -> StringResources(R.string.err_photo_corruption_detected)

        // Storage
        is QrError.PhotoError.StorageAccess -> StringResources(R.string.err_photo_storage_access)
        is QrError.PhotoError.StorageFull -> StringResources(R.string.err_photo_storage_full)
        is QrError.PhotoError.Permissions -> StringResources(R.string.err_photo_permissions)

        // Import/Export
        is QrError.PhotoError.Import -> StringResources(R.string.err_photo_import)
        is QrError.PhotoError.Export -> StringResources(R.string.err_photo_export)
        is QrError.PhotoError.UriAccess -> StringResources(R.string.err_photo_uri_access)

        // Management
        is QrError.PhotoError.List -> StringResources(R.string.err_photo_list)
        is QrError.PhotoError.Count -> StringResources(R.string.err_photo_count)
        is QrError.PhotoError.Delete -> StringResources(R.string.err_photo_delete)
        is QrError.PhotoError.Cleanup -> StringResources(R.string.err_photo_cleanup)

        // Camera integration
        is QrError.PhotoError.CameraAccess -> StringResources(R.string.err_photo_camera_access)
        is QrError.PhotoError.Capture -> StringResources(R.string.err_photo_capture)
        is QrError.PhotoError.SettingsApply -> StringResources(R.string.err_photo_settings_apply)
    }
}

fun QrError.BackupError.toUiText(): UiText {
    return when (this) {
        // Basic operations
        is QrError.BackupError.Save -> StringResources(R.string.err_backup_save)
        is QrError.BackupError.Load -> StringResources(R.string.err_backup_load)
        is QrError.BackupError.Delete -> StringResources(R.string.err_backup_delete)
        is QrError.BackupError.Create -> StringResources(R.string.err_backup_create)

        // Validation & integrity
        is QrError.BackupError.Validate -> StringResources(R.string.err_backup_validate)
        is QrError.BackupError.Corrupt -> StringResources(R.string.err_backup_corrupt)
        is QrError.BackupError.ChecksumMismatch -> StringResources(R.string.err_backup_checksum_mismatch)
        is QrError.BackupError.MetadataMissing -> StringResources(R.string.err_backup_metadata_missing)
        is QrError.BackupError.StructureInvalid -> StringResources(R.string.err_backup_structure_invalid)

        // Compression operations
        is QrError.BackupError.ZipCreate -> StringResources(R.string.err_backup_zip_create)
        is QrError.BackupError.ZipExtract -> StringResources(R.string.err_backup_zip_extract)
        is QrError.BackupError.ZipCorrupt -> StringResources(R.string.err_backup_zip_corrupt)
        is QrError.BackupError.ZipPassword -> StringResources(R.string.err_backup_zip_password)

        // Sharing & transfer
        is QrError.BackupError.ShareCreate -> StringResources(R.string.err_backup_share_create)
        is QrError.BackupError.ExportFailed -> StringResources(R.string.err_backup_export_failed)
        is QrError.BackupError.TempFileCreate -> StringResources(R.string.err_backup_temp_file_create)

        // Photo operations
        is QrError.BackupError.PhotoArchive -> StringResources(R.string.err_backup_photo_archive)
        is QrError.BackupError.PhotoExtract -> StringResources(R.string.err_backup_photo_extract)
        is QrError.BackupError.PhotoMissing -> StringResources(R.string.err_backup_photo_missing)
        is QrError.BackupError.PhotoCorrupt -> StringResources(R.string.err_backup_photo_corrupt)

        // Cleanup & maintenance
        is QrError.BackupError.RetentionPolicy -> StringResources(R.string.err_backup_retention_policy)
        is QrError.BackupError.CleanupFailed -> StringResources(R.string.err_backup_cleanup_failed)
        is QrError.BackupError.DiskSpace -> StringResources(R.string.err_backup_disk_space)

        // Specific backup scenarios
        is QrError.BackupError.PathGeneration -> StringResources(R.string.err_backup_path_generation)
        is QrError.BackupError.PathResolution -> StringResources(R.string.err_backup_path_resolution)
        is QrError.BackupError.StatsCalculation -> StringResources(R.string.err_backup_stats_calculation)
        is QrError.BackupError.SummaryGeneration -> StringResources(R.string.err_backup_summary_generation)
    }
}

fun QrError.Checkup.toUiText(): UiText {
    return when (this) {
        is QrError.Checkup.Unknown -> StringResources(R.string.err_checkup_delete_unknown)
        is QrError.Checkup.NotFound -> StringResources(R.string.err_checkup_not_found)
        is QrError.Checkup.CannotDeleteCompleted -> StringResources(R.string.err_checkup_delete_cannot_delete_completed)
        is QrError.Checkup.CannotDeleteExported -> StringResources(R.string.err_checkup_delete_cannot_delete_exported)
        is QrError.Checkup.CannotDeleteArchived -> StringResources(R.string.err_checkup_delete_cannot_delete_archived)
        is QrError.Checkup.Load -> StringResources(R.string.err_checkup_load_checkup)
        is QrError.Checkup.Reload -> StringResources(R.string.err_checkup_reload_checkup)
        is QrError.Checkup.Create -> StringResources(R.string.err_create)
        is QrError.Checkup.Delete -> StringResources(R.string.err_delete)
        is QrError.Checkup.FieldsRequired -> StringResources(R.string.err_fields_required)
        is QrError.Checkup.Refresh -> StringResources(R.string.err_refresh)
        is QrError.Checkup.FileOpen -> StringResources(R.string.err_file_open)
        is QrError.Checkup.FileShare -> StringResources(R.string.err_share)

        is QrError.Checkup.LoadPhotos -> StringResources(R.string.err_checkup_load_photos)
        is QrError.Checkup.UpdateStatus -> StringResources(R.string.err_checkup_update_status)
        is QrError.Checkup.UpdateNotes -> StringResources(R.string.err_checkup_update_notes)
        is QrError.Checkup.UpdateHeader -> StringResources(R.string.err_checkup_update_header)
        is QrError.Checkup.NotAvailable -> StringResources(R.string.err_checkup_not_available)
        is QrError.Checkup.SpareAdd -> StringResources(R.string.err_checkup_spare_add)
        is QrError.Checkup.Association -> StringResources(R.string.err_checkup_association)
        is QrError.Checkup.AssociationRemove -> StringResources(R.string.err_checkup_association_remove)
        is QrError.Checkup.Finalize -> StringResources(R.string.err_checkup_finalize)
        is QrError.Checkup.Export -> StringResources(R.string.err_checkup_export)

        is QrError.Checkup.InvalidStatusTransition -> StringResources(R.string.err_checkup_invalid_status_transition)

        is QrError.Checkup.ClientLoad -> StringResources(R.string.err_client_load_client)
        is QrError.Checkup.FacilityLoad -> StringResources(R.string.err_facility_load_facility)
        is QrError.Checkup.IslandLoad -> StringResources(R.string.err_island_load_island)
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
        is QrError.ContractsError -> this.toUiText()
        is QrError.FacilityError -> this.toUiText()
        is QrError.IslandError -> this.toUiText()
        is QrError.UnitError -> this.toUiText()
        is QrError.MaintenanceLogError -> this.toUiText()
        is QrError.IslandDocumentError -> this.toUiText()
        is QrError.ShareError -> this.toUiText()
        is QrError.ExportError -> this.toUiText()
        is QrError.PhotoError -> this.toUiText()
        is QrError.BackupError -> this.toUiText()
        is QrError.Checkup -> this.toUiText()

        else -> {}
    } as UiText
}