package net.calvuz.qreport.presentation.core.model

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R


enum class QReportState {
    // Error
    ERR_UNKNOWN,
    ERR_LOAD,
    ERR_RELOAD,
    ERR_CREATE,
    ERR_DELETE,
    ERR_FIELDS_REQUIRED,
    ERR_REFRESH,

    //CheckUp
    ERR_CHECKUP_NOT_FOUND,
    ERR_CHECKUP_LOAD_CHEKUP,
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

fun QReportState.getDisplayName(context: Context): String {
    return when (this) {
        QReportState.ERR_UNKNOWN -> context.getString(R.string.err_unknown)
        QReportState.ERR_LOAD -> context.getString(R.string.err_load)
        QReportState.ERR_RELOAD -> context.getString(R.string.err_reload)
        QReportState.ERR_CREATE -> context.getString(R.string.err_create)
        QReportState.ERR_DELETE -> context.getString(R.string.err_delete)
        QReportState.ERR_FIELDS_REQUIRED -> context.getString(R.string.err_fields_required)
        QReportState.ERR_REFRESH -> context.getString(R.string.err_refresh)
        QReportState.ERR_CHECKUP_NOT_FOUND -> context.getString(R.string.err_checkup_not_found)
        QReportState.ERR_CHECKUP_LOAD_CHEKUP -> context.getString(R.string.err_checkup_load_checkup)
        QReportState.ERR_CHECKUP_LOAD_PHOTOS -> context.getString(R.string.err_checkup_load_photos)
        QReportState.ERR_CHECKUP_UPDATE_STATUS -> context.getString(R.string.err_checkup_update_status)
        QReportState.ERR_CHECKUP_UPDATE_NOTES -> context.getString(R.string.err_checkup_update_notes)
        QReportState.ERR_CHECKUP_UPDATE_HEADER -> context.getString(R.string.err_checkup_update_header)
        QReportState.ERR_CHECKUP_NOT_AVAILABLE -> context.getString(R.string.err_checkup_not_available)
        QReportState.ERR_CHECKUP_SPARE_ADD -> context.getString(R.string.err_checkup_spare_add)
        QReportState.ERR_CHECKUP_FINALIZE -> context.getString(R.string.err_checkup_finalize)
        QReportState.ERR_CHECKUP_ASSOCIATION -> context.getString(R.string.err_checkup_association)
        QReportState.ERR_CHECKUP_ASSOCIATION_REMOVE -> context.getString(R.string.err_checkup_association_remove)
        QReportState.ERR_CHECKUP_EXPORT -> context.getString(R.string.err_checkup_export)
        QReportState.ERR_CLIENT_LOAD -> context.getString(R.string.err_client_load_client)
        QReportState.ERR_FACILITY_LOAD -> context.getString(R.string.err_facility_load_facility)
        QReportState.ERR_ISLAND_LOAD -> context.getString(R.string.err_island_load_island)
    }
}

@Composable
fun QReportState.getDisplayName(): String {
    return when (this) {
        QReportState.ERR_UNKNOWN -> stringResource(R.string.err_unknown)
        QReportState.ERR_LOAD -> stringResource(R.string.err_load)
        QReportState.ERR_RELOAD -> stringResource(R.string.err_reload)
        QReportState.ERR_CREATE -> stringResource(R.string.err_create)
        QReportState.ERR_DELETE -> stringResource(R.string.err_delete)
        QReportState.ERR_FIELDS_REQUIRED -> stringResource(R.string.err_fields_required)
        QReportState.ERR_REFRESH -> stringResource(R.string.err_refresh)
        QReportState.ERR_CHECKUP_NOT_FOUND -> stringResource(R.string.err_checkup_not_found)
        QReportState.ERR_CHECKUP_LOAD_CHEKUP ->stringResource(R.string.err_checkup_load_checkup)
        QReportState.ERR_CHECKUP_LOAD_PHOTOS -> stringResource(R.string.err_checkup_load_photos)
        QReportState.ERR_CHECKUP_UPDATE_STATUS -> stringResource(R.string.err_checkup_update_status)
        QReportState.ERR_CHECKUP_UPDATE_NOTES -> stringResource(R.string.err_checkup_update_notes)
        QReportState.ERR_CHECKUP_UPDATE_HEADER -> stringResource(R.string.err_checkup_update_header)
        QReportState.ERR_CHECKUP_NOT_AVAILABLE -> stringResource(R.string.err_checkup_not_available)
        QReportState.ERR_CHECKUP_SPARE_ADD -> stringResource(R.string.err_checkup_spare_add)
        QReportState.ERR_CHECKUP_ASSOCIATION -> stringResource(R.string.err_checkup_association)
        QReportState.ERR_CHECKUP_ASSOCIATION_REMOVE -> stringResource(R.string.err_checkup_association_remove)
        QReportState.ERR_CHECKUP_FINALIZE -> stringResource(R.string.err_checkup_finalize)
        QReportState.ERR_CHECKUP_EXPORT -> stringResource(R.string.err_checkup_export)
        QReportState.ERR_CLIENT_LOAD -> stringResource(R.string.err_client_load_client)
        QReportState.ERR_FACILITY_LOAD -> stringResource(R.string.err_facility_load_facility)
        QReportState.ERR_ISLAND_LOAD -> stringResource(R.string.err_island_load_island)
    }
}