package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.calvuz.qreport.R

/**
 * Date + time picker field for a single [Instant] value.
 *
 * Opens a DatePickerDialog first, then a TimePickerDialog after date confirmation.
 * Displays the selected value as "dd/MM/yyyy HH:mm" in the text field.
 *
 * Used by [MaintenanceLogFormScreen] for the [MaintenanceLog.performedAt] field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrDateTimePickerField(
    modifier: Modifier = Modifier,
    label: String,
    value: Instant,
    onValueChange: (Instant) -> Unit,
    error: String? = null
) {
    val tz = TimeZone.currentSystemDefault()
    val local = value.toLocalDateTime(tz)

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    val displayValue = remember(value) {
        val d = local.date
        val t = local.time
        "%02d/%02d/%04d %02d:%02d".format(d.dayOfMonth, d.monthNumber, d.year, t.hour, t.minute)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            isError = error != null,
            supportingText = if (error != null) { { Text(error) } } else null,
            trailingIcon = {
                Row {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.maint_label_performed_at)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = stringResource(R.string.maint_label_performed_at)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // ── Date picker ───────────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toEpochMilliseconds()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true   // chain to time picker
                }) { Text(stringResource(R.string.core_action_next)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.core_action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Time picker ───────────────────────────────────────────────────────────
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = local.hour,
            initialMinute = local.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateMs = pendingDateMillis ?: value.toEpochMilliseconds()
                    // Combine selected date with selected time
                    val baseDt = Instant.fromEpochMilliseconds(dateMs)
                        .toLocalDateTime(tz)
                    val combined = LocalDateTime(
                        year = baseDt.year,
                        monthNumber = baseDt.monthNumber,
                        dayOfMonth = baseDt.dayOfMonth,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        second = 0,
                        nanosecond = 0
                    )
                    onValueChange(combined.toInstant(tz))
                    showTimePicker = false
                }) { Text(stringResource(R.string.core_action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.core_action_cancel))
                }
            },
            title = { Text(stringResource(R.string.maint_label_performed_at)) },
            text = { TimePicker(state = timePickerState) }
        )
    }
}