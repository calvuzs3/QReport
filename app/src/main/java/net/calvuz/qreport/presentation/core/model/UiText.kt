package net.calvuz.qreport.presentation.core.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText() {

    data class DynStr(val str: String): UiText()
    data class StrRes(@StringRes val resId: Int): UiText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    @Deprecated("No longer actively maintained")
    data class ErrStringResource(
        val err: DataError.QrError,
        val msg: String? = null
    ): UiText()

    fun asString(context: Context): String {
        return when(this) {
            is ErrStringResource -> context.getString( err.getResId(), msg as Any)
            is StrRes -> context.getString(resId)
            is StringResource -> context.getString(resId, args)
            is DynStr -> str
        }
    }
    @Composable
    fun asString(): String {
        return when(this) {
            is ErrStringResource -> stringResource( err.getResId(), msg as Any)
            is StrRes -> stringResource(resId)
            is StringResource -> stringResource(resId, args)
            is DynStr -> str
        }
    }

    @Composable
    fun asStringArgs(): String {
        return when(this) {
            is ErrStringResource -> msg.toString()
            is StrRes -> ""
            is StringResource -> ""
            is DynStr -> str
        }
    }
}