package net.calvuz.qreport.app.error.presentation

import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.error.presentation.UiText.StringResources
import net.calvuz.qreport.app.result.domain.QrResult
//
//fun QrError.NotImplemented.toUiText(): UiText {
//    return when (this) {
//        is QrError.NotImplemented -> StringResources(R.string.err_not_implemented)
//
//    }
//}

fun QrError.Contracts.toUiText(): UiText {
    return when (this) {
        is QrError.Contracts.ClientIdEmpty -> StringResources(R.string.err_contracts_client_id_empty)
        is QrError.Contracts.ClientNotFound -> StringResources(R.string.err_contracts_client_not_found)
        is QrError.Contracts.ContractNotFound -> StringResources(R.string.err_contracts_contract_not_found)
        is QrError.Contracts.DeleteError -> TODO()
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

fun QrError.asUiText(): UiText {
    return when (this) {

//        is QrError.NotImplemented -> this.toUiText()
        is QrError.Contracts -> this.toUiText()
        is QrError.DatabaseError -> this.toUiText()


        // Not implemented
        QrError.NotImplemented.YET -> StringResources(R.string.err_not_implemented)


        // File Error Mappings - Core File Operations
        QrError.FileError.DIRECTORY_CREATE ->
            StringResources(R.string.err_file_directory_create)

        QrError.FileError.DIRECTORY_ACCESS ->
            StringResources(R.string.err_file_directory_access)

        QrError.FileError.DIRECTORY_DELETE ->
            StringResources(R.string.err_file_directory_delete)

        QrError.FileError.DIRECTORY_NOT_FOUND ->
            StringResources(R.string.err_file_directory_not_found)

        QrError.FileError.DIRECTORY_NOT_EMPTY ->
            StringResources(R.string.err_file_directory_not_empty)

        QrError.FileError.DIRECTORY_PERMISSION_DENIED ->
            StringResources(R.string.err_file_directory_permission_denied)

        QrError.FileError.FILE_CREATE ->
            StringResources(R.string.err_file_create)

        QrError.FileError.FILE_READ ->
            StringResources(R.string.err_file_read)

        QrError.FileError.FILE_WRITE ->
            StringResources(R.string.err_file_write)

        QrError.FileError.FILE_DELETE ->
            StringResources(R.string.err_file_delete)

        QrError.FileError.FILE_COPY ->
            StringResources(R.string.err_file_copy)

        QrError.FileError.FILE_MOVE ->
            StringResources(R.string.err_file_move)

        QrError.FileError.FILE_RENAME ->
            StringResources(R.string.err_file_rename)

        QrError.FileError.FILE_ACCESS ->
            StringResources(R.string.err_file_access)

        QrError.FileError.FILE_NOT_FOUND ->
            StringResources(R.string.err_file_not_found)

        QrError.FileError.FILE_ALREADY_EXISTS ->
            StringResources(R.string.err_file_already_exists)

        QrError.FileError.FILE_LOCKED ->
            StringResources(R.string.err_file_locked)

        QrError.FileError.FILE_CORRUPTED ->
            StringResources(R.string.err_file_corrupted)

        QrError.FileError.PERMISSION_DENIED ->
            StringResources(R.string.err_file_permission_denied)

        QrError.FileError.READ_PERMISSION_DENIED ->
            StringResources(R.string.err_file_read_permission_denied)

        QrError.FileError.WRITE_PERMISSION_DENIED ->
            StringResources(R.string.err_file_write_permission_denied)

        QrError.FileError.EXECUTE_PERMISSION_DENIED ->
            StringResources(R.string.err_file_execute_permission_denied)

        QrError.FileError.INSUFFICIENT_SPACE ->
            StringResources(R.string.err_file_insufficient_space)

        QrError.FileError.DISK_FULL ->
            StringResources(R.string.err_file_disk_full)

        QrError.FileError.STORAGE_UNAVAILABLE ->
            StringResources(R.string.err_file_storage_unavailable)

        QrError.FileError.QUOTA_EXCEEDED ->
            StringResources(R.string.err_file_quota_exceeded)

        QrError.FileError.FILE_EMPTY -> StringResources(R.string.err_file_empty)
        QrError.FileError.FILE_TOO_LARGE ->
            StringResources(R.string.err_file_too_large)

        QrError.FileError.FILE_TOO_SMALL ->
            StringResources(R.string.err_file_too_small)

        QrError.FileError.SIZE_LIMIT_EXCEEDED ->
            StringResources(R.string.err_file_size_limit_exceeded)

        QrError.FileError.NAME_TOO_LONG ->
            StringResources(R.string.err_file_name_too_long)

        QrError.FileError.PATH_TOO_LONG ->
            StringResources(R.string.err_file_path_too_long)

        QrError.FileError.FORMAT_INVALID ->
            StringResources(R.string.err_file_format_invalid)

        QrError.FileError.ENCODING_ERROR ->
            StringResources(R.string.err_file_encoding_error)

        QrError.FileError.CHARSET_UNSUPPORTED ->
            StringResources(R.string.err_file_charset_unsupported)

        QrError.FileError.BINARY_DATA_CORRUPTED ->
            StringResources(R.string.err_file_binary_data_corrupted)

        QrError.FileError.IO_ERROR ->
            StringResources(R.string.err_file_io_error)

        QrError.FileError.READ_TIMEOUT ->
            StringResources(R.string.err_file_read_timeout)

        QrError.FileError.WRITE_TIMEOUT ->
            StringResources(R.string.err_file_write_timeout)

        QrError.FileError.OPERATION_INTERRUPTED ->
            StringResources(R.string.err_file_operation_interrupted)

        QrError.FileError.CONCURRENT_ACCESS ->
            StringResources(R.string.err_file_concurrent_access)

        QrError.FileError.NETWORK_UNAVAILABLE ->
            StringResources(R.string.err_file_network_unavailable)

        QrError.FileError.CONNECTION_LOST ->
            StringResources(R.string.err_file_connection_lost)

        QrError.FileError.EXTERNAL_STORAGE_REMOVED ->
            StringResources(R.string.err_file_external_storage_removed)

        QrError.FileError.DEVICE_BUSY ->
            StringResources(R.string.err_file_device_busy)

        QrError.FileError.PATH_INVALID ->
            StringResources(R.string.err_file_path_invalid)

        QrError.FileError.NAME_INVALID ->
            StringResources(R.string.err_file_name_invalid)

        QrError.FileError.EXTENSION_INVALID ->
            StringResources(R.string.err_file_extension_invalid)

        QrError.FileError.CHECKSUM_MISMATCH ->
            StringResources(R.string.err_file_checksum_mismatch)

        QrError.FileError.TEMP_FILE_CREATION_FAILED ->
            StringResources(R.string.err_file_temp_file_creation_failed)

        QrError.FileError.TEMP_DIR_UNAVAILABLE ->
            StringResources(R.string.err_file_temp_dir_unavailable)

        QrError.FileError.CACHE_WRITE_FAILED ->
            StringResources(R.string.err_file_cache_write_failed)

        QrError.FileError.CLEANUP_FAILED ->
            StringResources(R.string.err_file_cleanup_failed)

        QrError.FileError.FILE_LOCKED_BY_OTHER ->
            StringResources(R.string.err_file_locked_by_other)

        QrError.FileError.LOCK_ACQUISITION_FAILED ->
            StringResources(R.string.err_file_lock_acquisition_failed)

        QrError.FileError.UNLOCK_FAILED ->
            StringResources(R.string.err_file_unlock_failed)

        QrError.FileError.SYSTEM_ERROR ->
            StringResources(R.string.err_file_system_error)

        QrError.FileError.RESOURCE_UNAVAILABLE ->
            StringResources(R.string.err_file_resource_unavailable)

        QrError.FileError.HANDLE_EXHAUSTED ->
            StringResources(R.string.err_file_handle_exhausted)

        QrError.FileError.FILESYSTEM_ERROR ->
            StringResources(R.string.err_file_filesystem_error)

        QrError.FileError.FILESYSTEM_READONLY ->
            StringResources(R.string.err_file_filesystem_readonly)

        QrError.FileError.COPY_FAILED ->
            StringResources(R.string.err_file_copy_failed)

        QrError.FileError.MOVE_FAILED ->
            StringResources(R.string.err_file_move_failed)

        QrError.FileError.BACKUP_FAILED ->
            StringResources(R.string.err_file_backup_failed)

        QrError.FileError.RESTORE_FAILED ->
            StringResources(R.string.err_file_restore_failed)

        QrError.FileError.SYNC_FAILED ->
            StringResources(R.string.err_file_sync_failed)

        QrError.FileError.METADATA_READ_FAILED ->
            StringResources(R.string.err_file_metadata_read_failed)

        QrError.FileError.METADATA_WRITE_FAILED ->
            StringResources(R.string.err_file_metadata_write_failed)

        QrError.FileError.ATTRIBUTES_ACCESS_FAILED ->
            StringResources(R.string.err_file_attributes_access_failed)

        QrError.FileError.TIMESTAMP_UPDATE_FAILED ->
            StringResources(R.string.err_file_timestamp_update_failed)


        // Export Error Mappings
        QrError.ExportError.DIRECTORY_CREATE ->
            StringResources(R.string.err_export_directory_create)

        QrError.ExportError.DIRECTORY_ACCESS ->
            StringResources(R.string.err_export_directory_access)

        QrError.ExportError.DIRECTORY_DELETE ->
            StringResources(R.string.err_export_directory_delete)

        QrError.ExportError.FILE_CREATE ->
            StringResources(R.string.err_export_file_create)

        QrError.ExportError.FILE_WRITE ->
            StringResources(R.string.err_export_file_write)

        QrError.ExportError.FILE_READ ->
            StringResources(R.string.err_export_file_read)

        QrError.ExportError.FILE_COPY ->
            StringResources(R.string.err_export_file_copy)

        QrError.ExportError.FILE_MOVE ->
            StringResources(R.string.err_export_file_move)

        QrError.ExportError.FILE_DELETE ->
            StringResources(R.string.err_export_file_delete)

        QrError.ExportError.EXPORT_GENERATION_FAILED ->
            StringResources(R.string.err_export_generation_failed)

        QrError.ExportError.CONTENT_SERIALIZATION ->
            StringResources(R.string.err_export_content_serialization)

        QrError.ExportError.FORMAT_NOT_SUPPORTED ->
            StringResources(R.string.err_export_format_not_supported)

        QrError.ExportError.TEMPLATE_PROCESSING ->
            StringResources(R.string.err_export_template_processing)

        QrError.ExportError.VALIDATION_FAILED ->
            StringResources(R.string.err_export_validation_failed)

        QrError.ExportError.FILE_CORRUPTION ->
            StringResources(R.string.err_export_file_corruption)

        QrError.ExportError.INTEGRITY_CHECK_FAILED ->
            StringResources(R.string.err_export_integrity_check_failed)

        QrError.ExportError.STRUCTURE_INVALID ->
            StringResources(R.string.err_export_structure_invalid)

        QrError.ExportError.STORAGE_CHECK_FAILED ->
            StringResources(R.string.err_export_storage_check_failed)

        QrError.ExportError.INSUFFICIENT_STORAGE ->
            StringResources(R.string.err_export_insufficient_storage)

        QrError.ExportError.SIZE_CALCULATION_FAILED ->
            StringResources(R.string.err_export_size_calculation_failed)

        QrError.ExportError.QUOTA_EXCEEDED ->
            StringResources(R.string.err_export_quota_exceeded)

        QrError.ExportError.CLEANUP_FAILED ->
            StringResources(R.string.err_export_cleanup_failed)

        QrError.ExportError.DELETE_FAILED ->
            StringResources(R.string.err_export_delete_failed)

        QrError.ExportError.MAINTENANCE_FAILED ->
            StringResources(R.string.err_export_maintenance_failed)

        QrError.ExportError.LIST_FAILED ->
            StringResources(R.string.err_export_list_failed)

        QrError.ExportError.INFO_FAILED ->
            StringResources(R.string.err_export_info_failed)

        QrError.ExportError.METADATA_FAILED ->
            StringResources(R.string.err_export_metadata_failed)

        QrError.ExportError.INDEX_CREATION_FAILED ->
            StringResources(R.string.err_export_index_creation_failed)

        QrError.ExportError.MANIFEST_CREATE_FAILED ->
            StringResources(R.string.err_export_manifest_create_failed)

        QrError.ExportError.MANIFEST_READ_FAILED ->
            StringResources(R.string.err_export_manifest_read_failed)

        QrError.ExportError.TRACKING_FAILED ->
            StringResources(R.string.err_export_tracking_failed)

        QrError.ExportError.COMPRESSION_FAILED ->
            StringResources(R.string.err_export_compression_failed)

        QrError.ExportError.ARCHIVE_CREATION_FAILED ->
            StringResources(R.string.err_export_archive_creation_failed)

        QrError.ExportError.PACKAGE_ASSEMBLY_FAILED ->
            StringResources(R.string.err_export_package_assembly_failed)

        QrError.ExportError.PERMISSION_DENIED ->
            StringResources(R.string.err_export_permission_denied)

        QrError.ExportError.ACCESS_RESTRICTED ->
            StringResources(R.string.err_export_access_restricted)

        QrError.ExportError.WRITE_PROTECTED ->
            StringResources(R.string.err_export_write_protected)

        QrError.ExportError.CONFIGURATION_INVALID ->
            StringResources(R.string.err_export_configuration_invalid)

        QrError.ExportError.OPTIONS_CONFLICT ->
            StringResources(R.string.err_export_options_conflict)

        QrError.ExportError.PARAMETER_MISSING ->
            StringResources(R.string.err_export_parameter_missing)

        QrError.ExportError.TEMP_FILE_FAILED ->
            StringResources(R.string.err_export_temp_file_failed)

        QrError.ExportError.TEMP_CLEANUP_FAILED ->
            StringResources(R.string.err_export_temp_cleanup_failed)

        QrError.ExportError.TEMP_SPACE_FULL ->
            StringResources(R.string.err_export_temp_space_full)

        QrError.ExportError.EXTERNAL_TOOL_FAILED ->
            StringResources(R.string.err_export_external_tool_failed)

        QrError.ExportError.LIBRARY_ERROR ->
            StringResources(R.string.err_export_library_error)

        QrError.ExportError.SYSTEM_RESOURCE_UNAVAILABLE ->
            StringResources(R.string.err_export_system_resource_unavailable)

        QrError.ExportError.FILE_SHARE_FAILED ->
            StringResources(R.string.err_export_file_share_failed)


        // Share error mappings
        QrError.ShareError.SHARE_FAILED ->
            StringResources(R.string.err_share_failed)

        QrError.ShareError.INTENT_CREATION_FAILED ->
            StringResources(R.string.err_share_intent_creation_failed)

        QrError.ShareError.APP_NOT_FOUND ->
            StringResources(R.string.err_share_app_not_found)

        QrError.ShareError.NO_COMPATIBLE_APP ->
            StringResources(R.string.err_share_no_compatible_app)

        QrError.ShareError.OPEN_FAILED ->
            StringResources(R.string.err_share_open_failed)

        QrError.ShareError.FILE_NOT_FOUND ->
            StringResources(R.string.err_share_file_not_found)

        QrError.ShareError.TEMP_FILE_FAILED ->
            StringResources(R.string.err_share_temp_file_failed)

        QrError.ShareError.ZIP_CREATION_FAILED ->
            StringResources(R.string.err_share_zip_creation_failed)

        QrError.ShareError.URI_CREATION_FAILED ->
            StringResources(R.string.err_share_uri_creation_failed)

        QrError.ShareError.FILEPROVIDER_FAILED ->
            StringResources(R.string.err_share_fileprovider_failed)

        QrError.ShareError.VALIDATION_FAILED ->
            StringResources(R.string.err_share_validation_failed)

        QrError.ShareError.METADATA_FAILED ->
            StringResources(R.string.err_share_metadata_failed)

        QrError.ShareError.APP_QUERY_FAILED ->
            StringResources(R.string.err_share_app_query_failed)

        QrError.ShareError.PERMISSION_DENIED ->
            StringResources(R.string.err_share_permission_denied)

        QrError.ShareError.CLEANUP_FAILED ->
            StringResources(R.string.err_share_cleanup_failed)

        // ===== NEW PHOTO ERROR MAPPINGS =====

        // File operations
        QrError.PhotoError.FILE_COPY ->
            StringResources(R.string.err_photo_file_copy)

        QrError.PhotoError.FILE_MOVE ->
            StringResources(R.string.err_photo_file_move)

        // Photo processing
        QrError.PhotoError.CROP ->
            StringResources(R.string.err_photo_crop)

        // Thumbnail operations
        QrError.PhotoError.THUMBNAIL_ACCESS ->
            StringResources(R.string.err_photo_thumbnail_access)

        // Metadata & EXIF
        QrError.PhotoError.METADATA_WRITE ->
            StringResources(R.string.err_photo_metadata_write)

        QrError.PhotoError.EXIF_WRITE ->
            StringResources(R.string.err_photo_exif_write)

        QrError.PhotoError.ORIENTATION_READ ->
            StringResources(R.string.err_photo_orientation_read)

        // Image processing
        QrError.PhotoError.DECODE ->
            StringResources(R.string.err_photo_decode)

        QrError.PhotoError.ENCODE ->
            StringResources(R.string.err_photo_encode)

        QrError.PhotoError.FORMAT_UNSUPPORTED ->
            StringResources(R.string.err_photo_format_unsupported)

        QrError.PhotoError.COMPRESSION ->
            StringResources(R.string.err_photo_compression)

        QrError.PhotoError.QUALITY_ADJUSTMENT ->
            StringResources(R.string.err_photo_quality_adjustment)

        // Validation
        QrError.PhotoError.SIZE_VALIDATION ->
            StringResources(R.string.err_photo_size_validation)

        // Import/Export
        QrError.PhotoError.IMPORT ->
            StringResources(R.string.err_photo_import)

        QrError.PhotoError.EXPORT ->
            StringResources(R.string.err_photo_export)

        QrError.PhotoError.URI_ACCESS ->
            StringResources(R.string.err_photo_uri_access)

        // Management
        QrError.PhotoError.COUNT ->
            StringResources(R.string.err_photo_count)

        // Camera integration
        QrError.PhotoError.CAMERA_ACCESS ->
            StringResources(R.string.err_photo_camera_access)

        QrError.PhotoError.CAPTURE ->
            StringResources(R.string.err_photo_capture)

        QrError.PhotoError.SETTINGS_APPLY ->
            StringResources(R.string.err_photo_settings_apply)

        // Directory operations
        QrError.PhotoError.DIRECTORY_CREATE ->
            StringResources(R.string.err_photo_directory_create)

        QrError.PhotoError.DIRECTORY_ACCESS ->
            StringResources(R.string.err_photo_directory_access)

        QrError.PhotoError.THUMBNAILS_DIR_CREATE ->
            StringResources(R.string.err_photo_thumbnails_dir_create)

        // File operations
        QrError.PhotoError.FILE_CREATE ->
            StringResources(R.string.err_photo_file_create)

        QrError.PhotoError.FILE_ACCESS ->
            StringResources(R.string.err_photo_file_access)

        QrError.PhotoError.FILE_DELETE ->
            StringResources(R.string.err_photo_file_delete)

        // Photo processing
        QrError.PhotoError.SAVE ->
            StringResources(R.string.err_photo_save)

        QrError.PhotoError.LOAD ->
            StringResources(R.string.err_photo_load)

        QrError.PhotoError.RESIZE ->
            StringResources(R.string.err_photo_resize)

        QrError.PhotoError.ROTATE ->
            StringResources(R.string.err_photo_rotate)

        // Thumbnail operations
        QrError.PhotoError.THUMBNAIL_CREATE ->
            StringResources(R.string.err_photo_thumbnail_create)

        QrError.PhotoError.THUMBNAIL_DELETE ->
            StringResources(R.string.err_photo_thumbnail_delete)

        // Metadata & EXIF
        QrError.PhotoError.METADATA_READ ->
            StringResources(R.string.err_photo_metadata_read)

        QrError.PhotoError.EXIF_READ ->
            StringResources(R.string.err_photo_exif_read)

        // Validation
        QrError.PhotoError.VALIDATION ->
            StringResources(R.string.err_photo_validation)

        QrError.PhotoError.FORMAT_VALIDATION ->
            StringResources(R.string.err_photo_format_validation)

        QrError.PhotoError.CORRUPTION_DETECTED ->
            StringResources(R.string.err_photo_corruption_detected)

        // Storage
        QrError.PhotoError.STORAGE_ACCESS ->
            StringResources(R.string.err_photo_storage_access)

        QrError.PhotoError.STORAGE_FULL ->
            StringResources(R.string.err_photo_storage_full)

        QrError.PhotoError.PERMISSIONS ->
            StringResources(R.string.err_photo_permissions)

        // Management
        QrError.PhotoError.LIST ->
            StringResources(R.string.err_photo_list)

        QrError.PhotoError.DELETE ->
            StringResources(R.string.err_photo_delete)

        QrError.PhotoError.CLEANUP ->
            StringResources(R.string.err_photo_cleanup)


        // ===== NEW BACKUP ERROR MAPPINGS =====

        // Basic operations
        QrError.BackupError.SAVE ->
            StringResources(R.string.err_backup_save)

        QrError.BackupError.LOAD ->
            StringResources(R.string.err_backup_load)

        QrError.BackupError.DELETE ->
            StringResources(R.string.err_backup_delete)

        QrError.BackupError.CREATE ->
            StringResources(R.string.err_backup_create)

        // Validation & integrity
        QrError.BackupError.VALIDATE ->
            StringResources(R.string.err_backup_validate)

        QrError.BackupError.CORRUPT ->
            StringResources(R.string.err_backup_corrupt)

        QrError.BackupError.CHECKSUM_MISMATCH ->
            StringResources(R.string.err_backup_checksum_mismatch)

        QrError.BackupError.METADATA_MISSING ->
            StringResources(R.string.err_backup_metadata_missing)

        QrError.BackupError.STRUCTURE_INVALID ->
            StringResources(R.string.err_backup_structure_invalid)

        // Compression operations
        QrError.BackupError.ZIP_CREATE ->
            StringResources(R.string.err_backup_zip_create)

        QrError.BackupError.ZIP_EXTRACT ->
            StringResources(R.string.err_backup_zip_extract)

        QrError.BackupError.ZIP_CORRUPT ->
            StringResources(R.string.err_backup_zip_corrupt)

        QrError.BackupError.ZIP_PASSWORD ->
            StringResources(R.string.err_backup_zip_password)

        // Sharing & transfer
        QrError.BackupError.SHARE_CREATE ->
            StringResources(R.string.err_backup_share_create)

        QrError.BackupError.EXPORT_FAILED ->
            StringResources(R.string.err_backup_export_failed)

        QrError.BackupError.TEMP_FILE_CREATE ->
            StringResources(R.string.err_backup_temp_file_create)

        // Photo operations
        QrError.BackupError.PHOTO_ARCHIVE ->
            StringResources(R.string.err_backup_photo_archive)

        QrError.BackupError.PHOTO_EXTRACT ->
            StringResources(R.string.err_backup_photo_extract)

        QrError.BackupError.PHOTO_MISSING ->
            StringResources(R.string.err_backup_photo_missing)

        QrError.BackupError.PHOTO_CORRUPT ->
            StringResources(R.string.err_backup_photo_corrupt)

        // Cleanup & maintenance
        QrError.BackupError.RETENTION_POLICY ->
            StringResources(R.string.err_backup_retention_policy)

        QrError.BackupError.CLEANUP_FAILED ->
            StringResources(R.string.err_backup_cleanup_failed)

        QrError.BackupError.DISK_SPACE ->
            StringResources(R.string.err_backup_disk_space)

        // Specific backup scenarios
        QrError.BackupError.PATH_GENERATION ->
            StringResources(R.string.err_backup_path_generation)

        QrError.BackupError.PATH_RESOLUTION ->
            StringResources(R.string.err_backup_path_resolution)

        QrError.BackupError.STATS_CALCULATION ->
            StringResources(R.string.err_backup_stats_calculation)

        QrError.BackupError.SUMMARY_GENERATION ->
            StringResources(R.string.err_backup_summary_generation)

        // ===== NEW SHARE ERROR MAPPINGS =====

        QrError.Share.SHARE -> StringResources(R.string.err_share)
        QrError.Share.CREATE -> StringResources(R.string.err_share_create)

        QrError.File.OPEN -> StringResources(R.string.err_file_open)
        QrError.File.READ -> StringResources(R.string.err_file_read)
        QrError.File.COPY -> StringResources(R.string.err_file_copy)
        QrError.File.MOVE -> StringResources(R.string.err_file_move)
        QrError.File.LIST -> StringResources(R.string.err_file_list)
        QrError.File.CREATE -> StringResources(R.string.err_file_create)
        QrError.File.DELETE -> StringResources(R.string.err_file_delete)
        QrError.File.NOT_FOUND -> StringResources(R.string.err_file_not_found)
        QrError.File.FILE_NOT_EXISTS -> StringResources(R.string.err_file_not_exists)
        QrError.File.GET_FILE_SIZE -> StringResources(R.string.err_file_get_size)
        QrError.File.PROCESSING -> StringResources(R.string.err_file_processing)

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

        QrError.Network.REQUEST_TIMEOUT -> StringResources(R.string.err_network_request_timeout)

        QrError.Exporting.CANNOT_EXPORT_DRAFT -> StringResources(R.string.err_export_cannot_export_draft)

    }
}

fun QrResult.Error<*, QrError>.asErrorUiText(): UiText {
    return error.asUiText()
}