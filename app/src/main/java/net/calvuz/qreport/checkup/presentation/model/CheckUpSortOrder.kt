package net.calvuz.qreport.checkup.presentation.model

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R

enum class CheckUpSortOrder {
    RECENT_FIRST, OLDEST_FIRST, CLIENT_NAME, STATUS
}

fun CheckUpSortOrder.getDisplayName(context: Context): String {
    return when (this) {
        CheckUpSortOrder.RECENT_FIRST -> context.getString(R.string.enum_checkup_sort_order_recent_first)
        CheckUpSortOrder.OLDEST_FIRST -> context.getString(R.string.enum_checkup_sort_order_oldest_first)
        CheckUpSortOrder.CLIENT_NAME -> context.getString(R.string.enum_checkup_sort_order_client_name)
        CheckUpSortOrder.STATUS -> context.getString(R.string.enum_checkup_sort_order_status)
    }
}

@Composable
fun CheckUpSortOrder.getDisplayName(): String {
    return when (this) {
        CheckUpSortOrder.RECENT_FIRST -> stringResource(R.string.enum_checkup_sort_order_recent_first)
        CheckUpSortOrder.OLDEST_FIRST -> stringResource(R.string.enum_checkup_sort_order_oldest_first)
        CheckUpSortOrder.CLIENT_NAME -> stringResource(R.string.enum_checkup_sort_order_client_name)
        CheckUpSortOrder.STATUS -> stringResource(R.string.enum_checkup_sort_order_status)
    }
}