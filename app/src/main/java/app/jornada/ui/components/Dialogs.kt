package app.jornada.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.jornada.ui.theme.J
import app.jornada.ui.theme.T
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK", style = T.button, color = J.colors.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", style = T.button, color = J.colors.muted) }
        },
    ) {
        DatePicker(state = state)
    }
}

/** Escolha de horário; com [weekday] diferente de nulo também escolhe o dia da semana. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(title: String, minutes: Int, weekday: Int?, onDismiss: () -> Unit, onPick: (Int, Int?) -> Unit) {
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
    var wd by remember { mutableIntStateOf(weekday ?: 6) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = J.colors.surface,
        title = { Text(title, style = T.title, color = J.colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (weekday != null) {
                    WeekdayPicker(setOf(wd), { wd = it }, sundayFirst = true)
                }
                TimeInput(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(state.hour * 60 + state.minute, if (weekday != null) wd else null)
                onDismiss()
            }) { Text("OK", style = T.button, color = J.colors.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", style = T.button, color = J.colors.muted) }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = J.colors.surface,
        title = { Text(title, style = T.title, color = J.colors.ink) },
        text = { Text(text, style = T.body, color = J.colors.muted) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirm, style = T.button, color = if (destructive) J.colors.redText else J.colors.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", style = T.button, color = J.colors.muted) }
        },
    )
}

@Composable
fun OptionsDialog(title: String, options: List<String>, selected: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = J.colors.surface,
        title = { Text(title, style = T.title, color = J.colors.ink) },
        text = {
            Column {
                options.forEachIndexed { i, label ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            onPick(i)
                            onDismiss()
                        }.padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        RadioDot(i == selected)
                        Text(label, style = T.body, color = J.colors.ink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar", style = T.button, color = J.colors.muted) }
        },
    )
}
