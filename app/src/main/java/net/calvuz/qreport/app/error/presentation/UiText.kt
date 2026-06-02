package net.calvuz.qreport.app.error.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R

sealed class UiText {

    /** A runtime string — use only when the text is not known at compile time. */
    data class DynStr(val str: String) : UiText()

    /** A string resource without format arguments. */
    data class StringResource(@StringRes val resId: Int) : UiText()

    /**
     * A string resource with format arguments (e.g. "%1$d items").
     *
     * Not a data class because vararg prevents auto-generated equals/hashCode.
     * Implemented manually so Compose recomposition and tests work correctly.
     */
    class StringResources(@StringRes val resId: Int, vararg val args: Any) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResources) return false
            return resId == other.resId && args.contentEquals(other.args)
        }
        override fun hashCode(): Int = 31 * resId + args.contentHashCode()
        override fun toString(): String = "StringResources(resId=$resId, args=${args.contentToString()})"
    }

    // =========================================================================
    // Conversion
    // =========================================================================

    /** Resolve to a plain string outside a Composable (e.g. in a ViewModel or Service). */
    fun asString(context: Context): String = when (this) {
        is DynStr -> str
        is StringResource -> context.getString(resId)
        is StringResources -> context.getString(resId, *args)
    }

    /** Resolve to a plain string inside a Composable. */
    @Composable
    fun asString(): String = when (this) {
        is DynStr -> str
        is StringResource -> stringResource(resId)
        is StringResources -> stringResource(resId, *args)
    }
}