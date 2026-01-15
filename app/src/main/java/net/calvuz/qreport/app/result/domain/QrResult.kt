package net.calvuz.qreport.app.result.domain

import net.calvuz.qreport.app.error.domain.model.QrError

//typealias RootError = Error

/** Result class */
sealed interface QrResult<out D, out E : QrError> {

    /** Success class */
    data class Success<out D, out E : QrError>(val data: D) : QrResult<D, E>

    /** Error class */
    data class Error<out D, out E : QrError>(val error: E) : QrResult<D, E>

}