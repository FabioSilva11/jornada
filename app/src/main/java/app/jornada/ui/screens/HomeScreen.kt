package app.jornada.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.jornada.data.DayRecord
import app.jornada.data.DayStatus
import app.jornada.domain.Closing
import app.jornada.domain.DateRange
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.domain.Periods
import app.jornada.domain.Summary
import app.jornada.domain.toView
import app.jornada.ui.AppViewModel
import app.jornada.ui.Routes
import app.jornada.ui.components.DayCell
import app.jornada.ui.components.IconBadge
import app.jornada.ui.components.IconCircleButton
import app.jornada.ui.components.JCard
import app.jornada.ui.components.OutlineButton
import app.jornada.ui.components.PrimaryButton
import app.jornada.ui.components.ProgressBar
import app.jornada.ui.components.TextAction
import app.jornada.ui.components.TonalButton
import app.jornada.ui.components.cellStyle
import app.jornada.ui.navigateTop
import app.jornada.ui.shareText
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(vm: AppViewModel, ledger: Ledger, nav: NavController, snackbar: SnackbarHostState) {
    val s = ledger.settings
    val today = ledger.today
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val payView = s.payPeriod.toView()
    val pay = remember(ledger) { ledger.summary(ledger.currentPayRange()) }
    val closing = remember(ledger) { if (s.showWeekendSummary) ledger.closing() else null }
    val weekRange = Periods.week(today, s.weekStartsSunday)
    val week = remember(ledger) { ledger.summary(weekRange) }

    val mark: (DayStatus) -> Unit = { status ->
        val previous = ledger.record(today)
        vm.setDay(DayRecord(today.toString(), status))
        val v = ledger.defaultValue(today, status)
        scope.launch {
            val msg = if (v > 0) "Dia marcado: + ${Fmt.money(v)}" else "Dia marcado: ${Fmt.status(status)}"
            val r = snackbar.showSnackbar(msg, actionLabel = "Desfazer", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) vm.restoreDay(today, previous)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Fmt.longDay(today), style = T.small.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = J.colors.muted)
                Text(greeting(closing != null), style = T.screenTitle, color = J.colors.ink)
            }
            IconCircleButton(JIcons.Chart, "Métricas", { nav.navigate(Routes.METRICS) })
        }

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (closing != null) {
                ClosingCard(closing) { shareText(context, closingText(closing)) }
            }
            TodayCard(ledger, onMark = mark, onOpen = { vm.openDay(today) })

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Bruto ${Fmt.inPeriod(payView)}",
                    value = Fmt.money(pay.gross),
                    sub = "${Fmt.daysDecimal(pay.workedEquivalent)} trabalhados",
                    valueColor = J.colors.ink,
                    subColor = J.colors.muted,
                    modifier = Modifier.weight(1f),
                ) { nav.navigateTop(Routes.MONEY) }
                StatTile(
                    label = "Líquido ${Fmt.inPeriod(payView)}",
                    value = Fmt.money(pay.net),
                    sub = if (pay.totalDeductions > 0) "${Fmt.minus(pay.totalDeductions)} descontos" else "sem descontos",
                    valueColor = J.colors.primary,
                    subColor = if (pay.totalDeductions > 0) J.colors.redText else J.colors.muted,
                    modifier = Modifier.weight(1f),
                ) { nav.navigateTop(Routes.MONEY) }
            }

            PaydayCard(ledger)
            WeekCard(ledger, weekRange, week) { vm.openDay(it) }
            VacationTeaser(ledger, nav)
        }
    }
}

private fun greeting(weekendSummary: Boolean): String {
    if (weekendSummary) return "Bom fim de semana!"
    val h = LocalTime.now().hour
    return when {
        h < 12 -> "Bom dia!"
        h < 18 -> "Boa tarde!"
        else -> "Boa noite!"
    }
}

private fun closingText(cl: Closing): String = buildString {
    append("Minha semana (${Fmt.shortRange(cl.range)}): ${Fmt.money(cl.week.gross)} bruto")
    if (cl.week.totalDeductions > 0) append(", ${Fmt.money(cl.week.net)} líquido")
    append(", ${Fmt.daysDecimal(cl.week.workedEquivalent)} trabalhados.")
}

@Composable
private fun ClosingCard(cl: Closing, onShare: () -> Unit) {
    val c = J.colors
    JCard(color = c.night, shape = RoundedCornerShape(24.dp), padding = PaddingValues(20.dp), spacing = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("FECHAMENTO · ${Fmt.shortRange(cl.range).uppercase(Fmt.locale)}", style = T.label, color = c.nightMuted)
            Text("Sua semana rendeu", style = T.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = c.onNight)
            Text(Fmt.money(cl.week.gross), style = T.display.copy(fontSize = 42.sp), color = c.onNight)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("Líquido", Fmt.money(cl.week.net), c.nightGreen, Modifier.weight(1f))
            MiniStat("Descontos", Fmt.minus(cl.week.totalDeductions), c.nightRed, Modifier.weight(1f))
        }
        val diff = cl.week.gross - cl.previous.gross
        if (cl.previous.gross > 0 || cl.week.gross > 0) {
            val reasons = listOfNotNull(
                cl.week.absences.takeIf { it > 0 }?.let { if (it == 1) "1 falta" else "$it faltas" },
                cl.week.halfDays.takeIf { it > 0 }?.let { if (it == 1) "1 meio período" else "$it meios períodos" },
            )
            val text = when {
                diff < -0.005 -> "${Fmt.money(-diff)} a menos que a semana passada" +
                    (if (reasons.isNotEmpty()) ": ${reasons.joinToString(" e ")}." else ".")
                diff > 0.005 -> "${Fmt.money(diff)} a mais que a semana passada."
                else -> "Igual à semana passada."
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (diff < 0) JIcons.TrendDown else JIcons.TrendUp,
                    null,
                    Modifier.size(18.dp),
                    tint = if (diff < 0) c.nightAmber else c.nightGreen,
                )
                Text(text, style = T.small, color = if (diff < 0) c.nightAmber else c.nightGreen)
            }
        }
        OutlineButton(
            "Compartilhar resumo",
            onShare,
            height = 40.dp,
            color = c.onNight,
            borderColor = c.nightCard,
            icon = JIcons.Share,
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier) {
    JCard(modifier = modifier, color = J.colors.nightCard, shape = RoundedCornerShape(14.dp), padding = PaddingValues(12.dp), spacing = 2.dp) {
        Text(label, style = T.tiny, color = J.colors.nightMuted)
        Text(value, style = T.h2.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1)
    }
}

@Composable
private fun TodayCard(ledger: Ledger, onMark: (DayStatus) -> Unit, onOpen: () -> Unit) {
    val c = J.colors
    val today = ledger.today
    val rec = ledger.record(today)
    if (rec != null) {
        JCard(onClick = onOpen) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DayCell(
                    today.dayOfMonth.toString(),
                    cellStyle(c, rec.status, ledger.isHoliday(today), ledger.isScheduled(today)),
                    size = 44.dp,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Hoje: ${Fmt.status(rec.status)}", style = T.h2, color = c.ink)
                    val v = ledger.dayValue(rec)
                    Text(
                        if (v > 0) "+ ${Fmt.money(v)} no período" else "Sem valor neste dia",
                        style = T.small,
                        color = c.muted,
                    )
                }
                TextAction("Alterar", onOpen)
            }
        }
        return
    }

    val holiday = ledger.holidayName(today)
    if (ledger.isWorkday(today)) {
        val v = ledger.defaultValue(today, DayStatus.WORKED)
        JCard(color = c.primary, shape = RoundedCornerShape(24.dp), padding = PaddingValues(20.dp), spacing = 14.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("HOJE", style = T.label, color = c.heroMuted)
                Text("Você foi trabalhar hoje?", style = T.title, color = c.onPrimary)
                if (v > 0) Text("Marcar soma ${Fmt.money(v)} ao período.", style = T.small, color = c.heroMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    "Sim, fui trabalhar",
                    { onMark(DayStatus.WORKED) },
                    Modifier.weight(1.4f),
                    icon = JIcons.Check,
                    container = c.surface,
                    content = c.onPrimarySoft,
                    height = 52.dp,
                )
                OutlineButton(
                    "Outro status",
                    onOpen,
                    Modifier.weight(1f),
                    color = c.onPrimary,
                    borderColor = c.primaryMid,
                )
            }
        }
    } else {
        val weekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
        val title = when {
            holiday != null -> "Hoje é feriado: $holiday"
            weekend -> "Vai trabalhar hoje?"
            else -> "Hoje é dia de folga"
        }
        val pct = ledger.multiplierPercent(today)
        val v = ledger.defaultValue(today, DayStatus.WORKED)
        val sub = when {
            v > 0 && pct > 100 && holiday != null -> "Se trabalhar, recebe ${Fmt.money(v)} (diária + ${pct - 100}%)."
            v > 0 && pct > 100 -> "${Fmt.weekday(today)} paga ${Fmt.money(v)} (diária + ${pct - 100}%)."
            v > 0 -> "Se trabalhar, recebe ${Fmt.money(v)}."
            else -> "Se trabalhar, marque aqui."
        }
        JCard(spacing = 12.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = T.h2.copy(fontSize = 17.sp), color = c.ink)
                Text(sub, style = T.small.copy(fontSize = 14.sp), color = c.muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Fui trabalhar", { onMark(DayStatus.WORKED) }, Modifier.weight(1f), height = 48.dp)
                TonalButton(
                    if (holiday != null) "Não trabalhei" else "Estou de folga",
                    { onMark(if (holiday != null) DayStatus.HOLIDAY else DayStatus.OFF) },
                    Modifier.weight(1f),
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    sub: String,
    valueColor: Color,
    subColor: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    JCard(modifier = modifier, onClick = onClick, spacing = 4.dp) {
        Text(label, style = T.small.copy(fontWeight = FontWeight.SemiBold), color = J.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = T.value.copy(fontSize = 20.sp), color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = T.small.copy(fontWeight = FontWeight.SemiBold), color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PaydayCard(ledger: Ledger) {
    val c = J.colors
    val next = ledger.nextPayday()
    val days = ChronoUnit.DAYS.between(ledger.today, next)
    val whenText = when (days) {
        0L -> "hoje"
        1L -> "amanhã"
        else -> "em $days dias"
    }
    JCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(JIcons.Wallet, c.primarySoft, c.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Próximo pagamento", style = T.h3, color = c.ink)
                Text("${Fmt.weekday(next)}, ${Fmt.ddmm(next)} · $whenText", style = T.small, color = c.muted)
            }
            Text(
                Fmt.periodAdjective(ledger.settings.payPeriod).replaceFirstChar { it.uppercase() },
                style = T.tiny,
                color = c.muted,
            )
        }
    }
}

@Composable
private fun WeekCard(ledger: Ledger, range: DateRange, week: Summary, onDay: (java.time.LocalDate) -> Unit) {
    val c = J.colors
    JCard(spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Esta semana", style = T.h2, color = c.ink, modifier = Modifier.weight(1f))
            Text(Fmt.money(week.gross), style = T.h2.copy(fontWeight = FontWeight.ExtraBold), color = c.primary)
        }
        Row(Modifier.fillMaxWidth()) {
            for (d in range.dates()) {
                val isToday = d == ledger.today
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        Fmt.weekdayLetter[d.dayOfWeek.value - 1],
                        style = T.tiny.copy(fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold),
                        color = if (isToday) c.ink else c.muted,
                    )
                    DayCell(
                        d.dayOfMonth.toString(),
                        cellStyle(c, ledger.record(d)?.status, ledger.isHoliday(d), ledger.isScheduled(d)),
                        size = 38.dp,
                        today = isToday,
                        fontSize = 14,
                        onClick = { onDay(d) },
                    )
                }
            }
        }
        if (ledger.settings.showWeekendSummary) {
            Text("No sábado você recebe o fechamento da semana.", style = T.small, color = c.muted)
        }
    }
}

@Composable
private fun VacationTeaser(ledger: Ledger, nav: NavController) {
    val c = J.colors
    if (!ledger.employmentIsClt()) {
        val reserve = remember(ledger) { ledger.autonomoReserve() }
        JCard(onClick = { nav.navigateTop(Routes.VACATION) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(JIcons.Sun, c.amberSoft, c.amberText, size = 44.dp, radius = 14.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Reserva de férias", style = T.h2, color = c.ink)
                    Text("${Fmt.money(reserve)} sugeridos nos últimos 12 meses", style = T.small, color = c.muted)
                }
                Icon(JIcons.ChevronRight, null, Modifier.size(20.dp), tint = c.muted)
            }
        }
        return
    }
    val info = remember(ledger) { ledger.vacation() }
    if (info == null) {
        JCard(onClick = { nav.navigate(Routes.SET_VACATION) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(JIcons.Sun, c.amberSoft, c.amberText, size = 44.dp, radius = 14.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Férias", style = T.h2, color = c.ink)
                    Text("Informe a data de admissão para calcular suas férias.", style = T.small, color = c.muted)
                }
                Icon(JIcons.ChevronRight, null, Modifier.size(20.dp), tint = c.muted)
            }
        }
        return
    }
    val pending = info.pending.firstOrNull()
    JCard(onClick = { nav.navigateTop(Routes.VACATION) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(JIcons.Sun, c.amberSoft, c.amberText, size = 44.dp, radius = 14.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Férias", style = T.h2, color = c.ink, modifier = Modifier.weight(1f))
                    Text("${info.monthsDone} de 12 meses", style = T.smallStrong, color = c.amberText)
                }
                ProgressBar(info.monthsDone / 12f, c.amber, c.amberTrack)
                Text(
                    if (pending != null) "${Fmt.days(pending.remaining)} para tirar até ${Fmt.ddmmyyyy(pending.deadline)}"
                    else "${info.current.entitlement} dias garantidos · completa em ${Fmt.ddmmyyyy(info.current.end.plusDays(1))}",
                    style = T.small,
                    color = if (pending != null) c.redText else c.muted,
                )
            }
            Icon(JIcons.ChevronRight, null, Modifier.size(20.dp), tint = c.muted)
        }
    }
}
