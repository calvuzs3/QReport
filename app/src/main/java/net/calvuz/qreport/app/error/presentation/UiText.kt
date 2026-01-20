package net.calvuz.qreport.app.error.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText() {

    data class DynStr(val str: String): UiText()
    data class StringResource(@StringRes val resId: Int): UiText()

    class StringResources(@StringRes val resId: Int, vararg val args: Any) : UiText()

    fun asString(context: Context): String {
        return when(this) {
            is StringResource -> context.getString(resId)
            is StringResources -> context.getString(resId, *args)
            is DynStr -> str
        }
    }
    @Composable
    fun asString(): String {
        return when(this) {
            is StringResource -> stringResource(resId)
            is StringResources -> stringResource(resId, *args)
            is DynStr -> str
        }
    }

    @Composable
    fun asStringArgs(): String {
        return when(this) {
            is StringResource -> ""
            is StringResources -> ""
            is DynStr -> str
        }
    }
}
