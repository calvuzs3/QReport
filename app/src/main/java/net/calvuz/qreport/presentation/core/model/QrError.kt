package net.calvuz.qreport.presentation.core.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.core.Error


sealed interface DataError: Error {
    enum class Network: DataError {
        REQUEST_TIMEOUT
    }
    enum class CheckupError: DataError {
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

    }
    enum class QrError : DataError {
        // Error
        UNKNOWN,

        //CheckUp
        NOT_FOUND,
        ERR_LOAD,
        ERR_RELOAD,
        ERR_CREATE,
        ERR_DELETE,
        ERR_FIELDS_REQUIRED,

        ERR_REFRESH,
        ERR_CHECKUP_LOAD_CHECKUP,
        ERR_CHECKUP_UPDATE_STATUS,
        ERR_CHECKUP_UPDATE_NOTES,
        ERR_CHECKUP_UPDATE_HEADER,
        ERR_CHECKUP_NOT_AVAILABLE,
        ERR_CHECKUP_SPARE_ADD,
        ERR_CHECKUP_ASSOCIATION,
        ERR_CHECKUP_ASSOCIATION_REMOVE,
        ERR_CHECKUP_FINALIZE,
        ERR_CHECKUP_EXPORT,
        ERR_CHECKUP_LOAD_PHOTOS,

        // Client
        ERR_CLIENT_LOAD,

        // FACility
        ERR_FACILITY_LOAD,

        //Island
        ERR_ISLAND_LOAD,


    }
}

@StringRes
fun DataError.QrError.getResId(): Int {
    return when (this) {
        DataError.QrError.UNKNOWN -> (R.string.err_unknown)
        DataError.QrError.ERR_LOAD -> (R.string.err_load)
        DataError.QrError.ERR_RELOAD -> (R.string.err_reload)
        DataError.QrError.ERR_CREATE -> (R.string.err_create)
        DataError.QrError.ERR_DELETE -> (R.string.err_delete)
        DataError.QrError.ERR_FIELDS_REQUIRED -> (R.string.err_fields_required)
        DataError.QrError.ERR_REFRESH -> (R.string.err_refresh)
        DataError.QrError.NOT_FOUND -> (R.string.err_checkup_not_found)
        DataError.QrError.ERR_CHECKUP_LOAD_CHECKUP -> (R.string.err_checkup_load_checkup)
        DataError.QrError.ERR_CHECKUP_LOAD_PHOTOS -> (R.string.err_checkup_load_photos)
        DataError.QrError.ERR_CHECKUP_UPDATE_STATUS -> (R.string.err_checkup_update_status)
        DataError.QrError.ERR_CHECKUP_UPDATE_NOTES -> (R.string.err_checkup_update_notes)
        DataError.QrError.ERR_CHECKUP_UPDATE_HEADER -> (R.string.err_checkup_update_header)
        DataError.QrError.ERR_CHECKUP_NOT_AVAILABLE -> (R.string.err_checkup_not_available)
        DataError.QrError.ERR_CHECKUP_SPARE_ADD -> (R.string.err_checkup_spare_add)
        DataError.QrError.ERR_CHECKUP_ASSOCIATION -> (R.string.err_checkup_association)
        DataError.QrError.ERR_CHECKUP_ASSOCIATION_REMOVE -> (R.string.err_checkup_association_remove)
        DataError.QrError.ERR_CHECKUP_FINALIZE -> (R.string.err_checkup_finalize)
        DataError.QrError.ERR_CHECKUP_EXPORT -> (R.string.err_checkup_export)
        DataError.QrError.ERR_CLIENT_LOAD -> (R.string.err_client_load_client)
        DataError.QrError.ERR_FACILITY_LOAD -> (R.string.err_facility_load_facility)
        DataError.QrError.ERR_ISLAND_LOAD -> (R.string.err_island_load_island)
    }
}

fun DataError.QrError.getDisplayName(context: Context): String {
    return when (this) {
        DataError.QrError.UNKNOWN -> context.getString(R.string.err_unknown)
        DataError.QrError.ERR_LOAD -> context.getString(R.string.err_load)
        DataError.QrError.ERR_RELOAD -> context.getString(R.string.err_reload)
        DataError.QrError.ERR_CREATE -> context.getString(R.string.err_create)
        DataError.QrError.ERR_DELETE -> context.getString(R.string.err_delete)
        DataError.QrError.ERR_FIELDS_REQUIRED -> context.getString(R.string.err_fields_required)
        DataError.QrError.ERR_REFRESH -> context.getString(R.string.err_refresh)
        DataError.QrError.NOT_FOUND -> context.getString(R.string.err_checkup_not_found)
        DataError.QrError.ERR_CHECKUP_LOAD_CHECKUP -> context.getString(R.string.err_checkup_load_checkup)
        DataError.QrError.ERR_CHECKUP_LOAD_PHOTOS -> context.getString(R.string.err_checkup_load_photos)
        DataError.QrError.ERR_CHECKUP_UPDATE_STATUS -> context.getString(R.string.err_checkup_update_status)
        DataError.QrError.ERR_CHECKUP_UPDATE_NOTES -> context.getString(R.string.err_checkup_update_notes)
        DataError.QrError.ERR_CHECKUP_UPDATE_HEADER -> context.getString(R.string.err_checkup_update_header)
        DataError.QrError.ERR_CHECKUP_NOT_AVAILABLE -> context.getString(R.string.err_checkup_not_available)
        DataError.QrError.ERR_CHECKUP_SPARE_ADD -> context.getString(R.string.err_checkup_spare_add)
        DataError.QrError.ERR_CHECKUP_FINALIZE -> context.getString(R.string.err_checkup_finalize)
        DataError.QrError.ERR_CHECKUP_ASSOCIATION -> context.getString(R.string.err_checkup_association)
        DataError.QrError.ERR_CHECKUP_ASSOCIATION_REMOVE -> context.getString(R.string.err_checkup_association_remove)
        DataError.QrError.ERR_CHECKUP_EXPORT -> context.getString(R.string.err_checkup_export)
        DataError.QrError.ERR_CLIENT_LOAD -> context.getString(R.string.err_client_load_client)
        DataError.QrError.ERR_FACILITY_LOAD -> context.getString(R.string.err_facility_load_facility)
        DataError.QrError.ERR_ISLAND_LOAD -> context.getString(R.string.err_island_load_island)
    }
}

@Composable
fun DataError.QrError.getDisplayName(): String {
    return when (this) {
        DataError.QrError.UNKNOWN -> stringResource(R.string.err_unknown)
        DataError.QrError.ERR_LOAD -> stringResource(R.string.err_load)
        DataError.QrError.ERR_RELOAD -> stringResource(R.string.err_reload)
        DataError.QrError.ERR_CREATE -> stringResource(R.string.err_create)
        DataError.QrError.ERR_DELETE -> stringResource(R.string.err_delete)
        DataError.QrError.ERR_FIELDS_REQUIRED -> stringResource(R.string.err_fields_required)
        DataError.QrError.ERR_REFRESH -> stringResource(R.string.err_refresh)
        DataError.QrError.NOT_FOUND -> stringResource(R.string.err_checkup_not_found)
        DataError.QrError.ERR_CHECKUP_LOAD_CHECKUP -> stringResource(R.string.err_checkup_load_checkup)
        DataError.QrError.ERR_CHECKUP_LOAD_PHOTOS -> stringResource(R.string.err_checkup_load_photos)
        DataError.QrError.ERR_CHECKUP_UPDATE_STATUS -> stringResource(R.string.err_checkup_update_status)
        DataError.QrError.ERR_CHECKUP_UPDATE_NOTES -> stringResource(R.string.err_checkup_update_notes)
        DataError.QrError.ERR_CHECKUP_UPDATE_HEADER -> stringResource(R.string.err_checkup_update_header)
        DataError.QrError.ERR_CHECKUP_NOT_AVAILABLE -> stringResource(R.string.err_checkup_not_available)
        DataError.QrError.ERR_CHECKUP_SPARE_ADD -> stringResource(R.string.err_checkup_spare_add)
        DataError.QrError.ERR_CHECKUP_ASSOCIATION -> stringResource(R.string.err_checkup_association)
        DataError.QrError.ERR_CHECKUP_ASSOCIATION_REMOVE -> stringResource(R.string.err_checkup_association_remove)
        DataError.QrError.ERR_CHECKUP_FINALIZE -> stringResource(R.string.err_checkup_finalize)
        DataError.QrError.ERR_CHECKUP_EXPORT -> stringResource(R.string.err_checkup_export)
        DataError.QrError.ERR_CLIENT_LOAD -> stringResource(R.string.err_client_load_client)
        DataError.QrError.ERR_FACILITY_LOAD -> stringResource(R.string.err_facility_load_facility)
        DataError.QrError.ERR_ISLAND_LOAD -> stringResource(R.string.err_island_load_island)
    }
}