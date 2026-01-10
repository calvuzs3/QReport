package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrDatePickerField(
    modifier: Modifier = Modifier,
    label: String,
    value: Instant?= null,
    onValueChange: (Instant?) -> Unit,
    error: String? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    //val datePickerState = rememberDatePickerState()

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value?.toLocalDateTime(TimeZone.currentSystemDefault())?.date?.toString() ?: "",
            onValueChange = { },
            label = { Text(label) },
            placeholder = { Text("Seleziona data") },
            readOnly = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error) }
            } else null,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Seleziona data"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (showDatePicker) {
            QrDatePickerDialog(
                onDateSelected = { selectedDate ->
                    onValueChange(selectedDate)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}
