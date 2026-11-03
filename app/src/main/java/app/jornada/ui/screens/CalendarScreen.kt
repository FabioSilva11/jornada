package app.jornada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.jornada.data.DayStatus
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.domain.Periods
import app.jornada.ui.AppViewModel
import app.jornada.ui.components.ChoiceButton
import app.jornada.ui.components.DayCell
import app.jornada.ui.components.InfoNote
import app.jornada.ui.components.JCard
import app.jornada.ui.components.OutlineButton
import app.jornada.ui.components.PeriodNav
import app.jornada.ui.components.PrimaryButton
import app.jornada.ui.components.ScreenHeader
import app.jornada.ui.components.StatusLegend
import app.jornada.ui.components.TextAction
import app.jornada.ui.components.cellStyle
import app.jornada.ui.theme.J
import app.jornada.ui.theme.T
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(vm: AppViewModel, ledger: Ledger) {
    val c = J.colors
    val s = ledger.settings
    val today = ledger.today
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthText)
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val selected = LocalDate.parse(selectedText)
    var multi by remember { mutableStateOf(false) }
    val picked = remember { mutableStateListOf<LocalDate>() }
    var showBulk by remember { mutableStateOf(false) }

    val range = Periods.month(month.atDay(1))
    val sum = remember(ledger, monthText) { ledger.summary(range) }
    val holidays = remember(ledger, monthText) { ledger.holidays(month.year).keys.count { it in range } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Calendário") {
            OutlineButton("Hoje", {
                monthText = YearMonth.from(today).toString()
                selectedText = today.toString()
            }, height = 38.dp)
        }
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodNav(
                Fmt.monthYear(month.atDay(1)),
                onPrev = { monthText = month.minusMonths(1).toString() },
                onNext = { monthText = month.plusMonths(1).toString() },
            )

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip(c.primary, null, if (sum.presenceDays == 1) "1 trabalhado" else "${sum.presenceDays} trabalhados")
                SummaryChip(c.redSoft, c.redBorder, if (sum.absences == 1) "1 falta" else "${sum.absences} faltas")
                SummaryChip(c.purpleDot, null, if (holidays == 1) "1 feriado" else "$holidays feriados")
                if (sum.vacationDays > 0) SummaryChip(c.amber, null, "${sum.vacationDays} de férias")
            }

            JCard(padding = PaddingValues(horizontal = 6.dp, vertical = 14.dp), spacing = 8.dp) {
                val order = if (s.weekStartsSunday) listOf(7, 1, 2, 3, 4, 5, 6) else (1..7).toList()
                Row(Modifier.fillMaxWidth()) {
                    for (d in order) {
                        Text(
                            Fmt.weekdayLetter[d - 1],
                            style = T.tiny,
                            color = c.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                val first = month.atDay(1)
                val offset = if (s.weekStartsSunday) first.dayOfWeek.value % 7 else first.dayOfWeek.value - 1
                val cells: List<LocalDate?> = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
                cells.chunked(7).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        for (i in 0 until 7) {
                            val d = row.getOrNull(i)
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (d != null) {
                                    DayCell(
                                        d.dayOfMonth.toString(),
                                        cellStyle(c, ledger.record(d)?.status, ledger.isHoliday(d), ledger.isScheduled(d)),
                                        today = d == today,
                                        selected = if (multi) d in picked else d == selected,
                                        onClick = {
                                            when {
                                                multi -> if (d in picked) picked.remove(d) else picked.add(d)
                                                d == selected -> vm.openDay(d)
                                                else -> selectedText = d.toString()
                                            }
                                        },
                                        onLongClick = {
                                            multi = true
                                            if (d !in picked) picked.add(d)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            StatusLegend(Modifier.padding(horizontal = 4.dp))

            if (multi) {
                JCard(spacing = 12.dp) {
                    Text(
                        if (picked.size == 1) "1 dia selecionado" else "${picked.size} dias selecionados",
                        style = T.h2,
                        color = c.ink,
                    )
                    Text("Toque nos dias para incluir ou tirar da seleção.", style = T.small, color = c.muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton("Cancelar", {
                            multi = false
                            picked.clear()
                        }, Modifier.weight(1f), height = 48.dp)
                        PrimaryButton(
                            "Marcar como…",
                            { showBulk = true },
                            Modifier.weight(1.4f),
                            enabled = picked.isNotEmpty(),
                            height = 48.dp,
                        )
                    }
                }
            } else {
                SelectedDayPanel(ledger, selected) { vm.openDay(selected) }
                InfoNote("Toque em um dia para ver, toque de novo para registrar. Segure um dia para marcar vários de uma vez.")
            }
        }
    }

    if (showBulk) {
        BulkStatusDialog(
            onDismiss = { showBulk = false },
            onPick = { status ->
                vm.setDays(picked.toList(), status)
                picked.clear()
                multi = false
                showBulk = false
            },
        )
    }
}

@Composable
private fun SummaryChip(swatch: Color, border: Color?, text: String) {
    val c = J.colors
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        var m = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(swatch)
        if (border != null) m = m.border(1.5.dp, border, RoundedCornerShape(3.dp))
        Box(m)
        Text(text, style = T.smallStrong, color = c.ink)
    }
}

@Composable
private fun SelectedDayPanel(ledger: Ledger, date: LocalDate, onOpen: () -> Unit) {
    val c = J.colors
    val rec = ledger.record(date)
    val holiday = ledger.holidayName(date)
    val desc = when {
        rec != null -> buildString {
            append(Fmt.status(rec.status))
            val v = ledger.dayValue(rec)
            if (v > 0) append(" · + ${Fmt.money(v)}")
            if (holiday != null) append(" · $holiday")
        }
        holiday != null -> "Feriado: $holiday"
        ledger.isWorkday(date) -> "Dia de trabalho previsto · sem registro"
        else -> "Folga prevista · sem registro"
    }
    JCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Fmt.longDay(date), style = T.h2, color = c.ink)
                Text(desc, style = T.small, color = c.muted)
                if (!rec?.note.isNullOrBlank()) Text("“${rec?.note}”", style = T.small, color = c.ink)
            }
            PrimaryButton(if (rec == null) "Registrar" else "Editar", onOpen, height = 44.dp)
        }
    }
}

@Composable
private fun BulkStatusDialog(onDismiss: () -> Unit, onPick: (DayStatus?) -> Unit) {
    val options = listOf(
        DayStatus.WORKED, DayStatus.HALF, DayStatus.ABSENT, DayStatus.SICK,
        DayStatus.OFF, DayStatus.HOLIDAY, DayStatus.VACATION,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = J.colors.surface,
        title = { Text("Marcar dias como", style = T.title, color = J.colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { st -> ChoiceButton(Fmt.status(st), false, { onPick(st) }, Modifier.weight(1f), height = 44.dp) }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
                TextAction("Apagar registros", { onPick(null) }, color = J.colors.redText)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar", style = T.button, color = J.colors.muted) }
        },
    )
}
