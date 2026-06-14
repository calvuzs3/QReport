package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R


@Composable
fun QReportConfirmDeleteDialog(
    objectName: String,
    objectDesc: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DeleteDialog(
        title = stringResource(R.string.confirm_delete_dialog_title, objectName),
        text = stringResource(R.string.confirm_delete_dialog_message, objectName, objectDesc),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}