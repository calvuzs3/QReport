package net.calvuz.qreport.presentation.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText() {

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText() {

        @Composable
        fun getDisplayCodeMessage(): String {
            return stringResource( resId, *args)
        }

        @Composable
        fun getDisplayArg(): Array<out Any> {
            return args
        }
    }

    class ErrStringResource(
        val err: QReportState,
        val msg: String? = null
    ): UiText() {

        @Composable
        fun getDisplayError(): String {
            return err.getDisplayName()
        }
        @Composable
        fun getDisplayError(arg:String): String {
            return "${err.getDisplayName()}: $arg"
        }
    }

}