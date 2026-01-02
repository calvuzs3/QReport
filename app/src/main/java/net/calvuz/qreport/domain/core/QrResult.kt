package net.calvuz.qreport.domain.core

typealias RootError = Error

/** Result class */
sealed interface QrResult<out D, out E : RootError> {

    /** Success class */
    data class Success<out D, out E : RootError>(val data: D) : QrResult<D, E>

    /** Error class */
    data class Error<out D, out E : RootError>(val error: E) : QrResult<D, E>
}