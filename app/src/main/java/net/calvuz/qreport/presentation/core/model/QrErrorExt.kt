package net.calvuz.qreport.presentation.core.model

import net.calvuz.qreport.R
import net.calvuz.qreport.domain.core.QrResult
import net.calvuz.qreport.presentation.core.model.UiText.*

fun DataError.asUiText(): UiText {
    return when (this) {
        DataError.QrError.UNKNOWN -> StringResource(R.string.err_unknown)
        DataError.QrError.ERR_LOAD -> StringResource(R.string.err_load)
        DataError.QrError.ERR_RELOAD -> StringResource(R.string.err_reload)
        DataError.QrError.ERR_CREATE -> StringResource(R.string.err_create)
        DataError.QrError.ERR_DELETE -> StringResource(R.string.err_delete)
        DataError.QrError.ERR_FIELDS_REQUIRED -> StringResource(R.string.err_fields_required)
        DataError.QrError.ERR_REFRESH -> StringResource(R.string.err_refresh)
        DataError.QrError.NOT_FOUND -> StringResource(R.string.err_checkup_not_found)
        DataError.QrError.ERR_CHECKUP_LOAD_CHECKUP -> StringResource(R.string.err_checkup_load_checkup)
        DataError.QrError.ERR_CHECKUP_LOAD_PHOTOS -> StringResource(R.string.err_checkup_load_photos)
        DataError.QrError.ERR_CHECKUP_UPDATE_STATUS -> StringResource (R.string.err_checkup_update_status)
        DataError.QrError.ERR_CHECKUP_UPDATE_NOTES -> StringResource(R.string.err_checkup_update_notes)
        DataError.QrError.ERR_CHECKUP_UPDATE_HEADER -> StringResource(R.string.err_checkup_update_header)
        DataError.QrError.ERR_CHECKUP_NOT_AVAILABLE -> StringResource(R.string.err_checkup_not_available)
        DataError.QrError.ERR_CHECKUP_SPARE_ADD -> StringResource(R.string.err_checkup_spare_add)
        DataError.QrError.ERR_CHECKUP_ASSOCIATION -> StringResource (R.string.err_checkup_association)
        DataError.QrError.ERR_CHECKUP_ASSOCIATION_REMOVE -> StringResource(R.string.err_checkup_association_remove)
        DataError.QrError.ERR_CHECKUP_FINALIZE -> StringResource(R.string.err_checkup_finalize)
        DataError.QrError.ERR_CHECKUP_EXPORT -> StringResource(R.string.err_checkup_export)
        DataError.QrError.ERR_CLIENT_LOAD -> StringResource(R.string.err_client_load_client)
        DataError.QrError.ERR_FACILITY_LOAD -> StringResource(R.string.err_facility_load_facility)
        DataError.QrError.ERR_ISLAND_LOAD -> StringResource(R.string.err_island_load_island)
        DataError.CheckupError.UNKNOWN -> StringResource(R.string.err_checkup_delete_unknown)
        DataError.CheckupError.NOT_FOUND -> StringResource(R.string.err_checkup_not_found)
        DataError.CheckupError.CANNOT_DELETE_COMPLETED -> StringResource(R.string.err_checkup_delete_cannot_delete_completed)
        DataError.CheckupError.CANNOT_DELETE_EXPORTED -> StringResource(R.string.err_checkup_delete_cannot_delete_exported)
        DataError.CheckupError.CANNOT_DELETE_ARCHIVED -> StringResource(R.string.err_checkup_delete_cannot_delete_archived)
        DataError.CheckupError.LOAD -> StringResource(R.string.err_checkup_load_checkup)
        DataError.CheckupError.RELOAD -> StringResource(R.string.err_checkup_reload_checkup)
        DataError.CheckupError.CREATE -> StringResource(R.string.err_create)
        DataError.CheckupError.DELETE -> StringResource(R.string.err_delete)
        DataError.CheckupError.FIELDS_REQUIRED -> StringResource(R.string.err_fields_required)
        DataError.CheckupError.REFRESH -> StringResource(R.string.err_refresh)
        DataError.CheckupError.FILE_OPEN -> StringResource(R.string.err_file_open)
        DataError.CheckupError.FILE_SHARE -> StringResource(R.string.err_file_share)

        DataError.Network.REQUEST_TIMEOUT -> StringResource(R.string.err_network_request_timeout)
    }
}

fun QrResult.Error<*, DataError>.asErrorUiText(): UiText {
    return error.asUiText()
}