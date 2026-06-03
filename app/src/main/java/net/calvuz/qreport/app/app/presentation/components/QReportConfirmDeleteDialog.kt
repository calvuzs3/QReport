package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.runtime.Composable


@Composable
fun QReportConfirmDeleteDialog(
    objectName: String,
    objectDesc: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DeleteDialog(
        title = "Elimina $objectName",
        text = "Sei sicuro di voler eliminare $objectName: $objectDesc?\nL'operazione non può essere annullata.",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}