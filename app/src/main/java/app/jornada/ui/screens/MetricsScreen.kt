package app.jornada.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.jornada.domain.DateRange
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.domain.Periods
import app.jornada.domain.Summary
import app.jornada.domain.ViewPeriod
import app.jornada.ui.components.IconBadge
import app.jornada.ui.components.JCard
import app.jornada.ui.components.LineDivider
import app.jornada.ui.components.PeriodNav
import app.jornada.ui.components.SegmentedTabs
import app.jornada.ui.components.TopBar
import app.jornada.ui.components.ValueRow
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import java.time.LocalDate
import kotlin.math.roundToInt

private val metricViews = listOf(ViewPeriod.WEEK, ViewPeriod.FORTNIGHT, ViewPeriod.MONTH, ViewPeriod.YEAR)

@Composable
fun MetricsScreen(ledger: Ledger, nav: NavController) {
    val s = ledger.settings
    val today = ledger.today
    var view by rememberSaveable { mutableStateOf(ViewPeriod.MONTH) }
    var anchorText by rememberSaveable { mutableStateOf(today.toString()) }
    val range = Periods.of(view, LocalDate.parse(anchorText), s.weekStartsSunday)
    val sum = remember(ledger, range) { ledger.summary(range) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Métricas", { nav.popBackStack() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SegmentedTabs(listOf("Semana", "Quinzena", "Mês", "Ano"), metricViews.indexOf(view), {
                if (today in range) anchorText = today.toString()
                view = metricViews[it]
            })
            PeriodNav(
                Fmt.rangeLabel(view, range),
                onPrev = { anchorText = Periods.shift(view, range, -1, s.weekStartsSunday).start.toString() },
                onNext = { anchorText = Periods.shift(view, range, 1, s.weekStartsSunday).start.toString() },
            )
            Kpis(ledger, range, sum)
            ChartCard(ledger, view, range)
            CompositionCard(sum)
            InsightsCard(ledger, view, range, sum)
        }
    }
}

@Composable
private fun Kpis(ledger: Ledger, range: DateRange, sum: Summary) {
    val (present, expected) = remember(ledger, range) { ledger.presence(range) }
    val streak = remember(ledger) { ledger.streak() }
    val (weekendDays, weekendValue) = remember(ledger, range) { ledger.weekendWork(range) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(
                "Presença",
                if (expected > 0) "${(present * 100.0 / expected).roundToInt()}%" else "—",
                "$present de $expected dias previstos",
                Modifier.weight(1f),
            )
            Kpi(
                "Líquido por dia",
                if (sum.workedEquivalent > 0) Fmt.money(sum.net / sum.workedEquivalent) else "—",
                "${Fmt.money(sum.net)} ÷ ${Fmt.daysDecimal(sum.workedEquivalent)}",
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi("Sequência", Fmt.days(streak), "seguidos sem faltar", Modifier.weight(1f))
            Kpi(
                "Fim de semana",
                Fmt.money(weekendValue),
                if (weekendDays == 1) "1 dia trabalhado" else "$weekendDays dias trabalhados",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, sub: String, modifier: Modifier) {
    JCard(modifier = modifier, shape = RoundedCornerShape(18.dp), padding = PaddingValues(14.dp), spacing = 4.dp) {
        Text(label, style = T.small.copy(fontWeight = FontWeight.SemiBold), color = J.colors.muted)
        Text(value, style = T.value.copy(fontSize = 22.sp), color = J.colors.ink, maxLines = 1)
        Text(sub, style = T.tiny.copy(fontWeight = FontWeight.Medium), color = J.colors.muted, maxLines = 2)
    }
}

private fun chartLabel(view: ViewPeriod, r: DateRange): String = when (view) {
    ViewPeriod.WEEK -> Fmt.ddmm(r.start)
    ViewPeriod.FORTNIGHT -> (if (r.start.dayOfMonth == 1) "1ª " else "2ª ") + Fmt.monthsShort[r.start.monthValue - 1]
    ViewPeriod.MONTH, ViewPeriod.YEAR -> Fmt.monthShortCap(r.start.monthValue)
}

@Composable
private fun ChartCard(ledger: Ledger, view: ViewPeriod, range: DateRange) {
    val c = J.colors
    val sundayStart = ledger.settings.weekStartsSunday
    val series: List<DateRange> = remember(range, view) {
        if (view == ViewPeriod.YEAR) (1..12).map { Periods.month(LocalDate.of(range.start.year, it, 1)) }
        else (5 downTo 0).map { Periods.shift(view, range, -it, sundayStart) }
    }
    val values = remember(ledger, series) { series.map { ledger.gross(it) } }
    val currentIdx = series.indexOfFirst { ledger.today in it }
    val projection = if (currentIdx >= 0) {
        values[currentIdx] + remember(ledger, series) { ledger.projection(series[currentIdx]).first }
    } else {
        null
    }
    val closed = series.indices.filter { series[it].end.isBefore(ledger.today) && values[it] > 0 }
    val avg = if (closed.isNotEmpty()) closed.map { values[it] }.average() else null
    val max = (values + listOfNotNull(projection, avg)).maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val noun = when (view) {
        ViewPeriod.WEEK -> "semana"
        ViewPeriod.FORTNIGHT -> "quinzena"
        else -> "mês"
    }

    JCard(spacing = 12.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Bruto por $noun", style = T.h2, color = c.ink, modifier = Modifier.weight(1f))
            if (avg != null) Text("média ${Fmt.money(avg)}", style = T.tiny, color = c.muted)
        }
        val primary = c.primary
        val mid = c.primaryMid
        val muted = c.muted
        val axis = c.border
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val n = series.size
            val slot = size.width / n
            val barW = slot * if (n > 6) 0.6f else 0.5f
            val h = size.height
            for (i in 0 until n) {
                val x = slot * i + (slot - barW) / 2
                val bh = (values[i] / max * h).toFloat()
                if (bh > 0f) {
                    drawRoundRect(
                        color = if (i == currentIdx) mid else primary,
                        topLeft = Offset(x, h - bh),
                        size = Size(barW, bh),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
                if (i == currentIdx && projection != null && projection > values[i]) {
                    val ph = (projection / max * h).toFloat()
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(x + 1.dp.toPx(), h - ph),
                        size = Size(barW - 2.dp.toPx(), ph - bh),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                    )
                }
            }
            if (avg != null) {
                val y = (h - avg / max * h).toFloat()
                drawLine(muted, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            }
            drawLine(axis, Offset(0f, h), Offset(size.width, h), strokeWidth = 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth()) {
            series.forEachIndexed { i, r ->
                Text(
                    if (view == ViewPeriod.YEAR) Fmt.monthsShort[r.start.monthValue - 1].take(1).uppercase() else chartLabel(view, r),
                    style = T.tiny.copy(fontSize = 11.sp, fontWeight = if (i == currentIdx) FontWeight.ExtraBold else FontWeight.SemiBold),
                    color = if (i == currentIdx) c.ink else c.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
        val best = values.indices.maxByOrNull { values[it] }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (best != null && values[best] > 0) {
                ValueRow("Melhor $noun · ${Fmt.rangeLabel(if (view == ViewPeriod.YEAR) ViewPeriod.MONTH else view, series[best])}", Fmt.money(values[best]))
            }
            if (currentIdx >= 0) {
                ValueRow("Período atual até agora", Fmt.money(values[currentIdx]))
                if (projection != null && projection > values[currentIdx]) {
                    ValueRow("Projeção (tracejado)", Fmt.money(projection))
                }
            }
        }
    }
}

@Composable
private fun CompositionCard(sum: Summary) {
    val c = J.colors
    val deds = sum.deductions.filter { it.amount > 0 }.sortedByDescending { it.amount }
    val net = sum.net.coerceAtLeast(0.0)
    val denom = net + deds.sumOf { it.amount }
    JCard(spacing = 12.dp) {
        Text("Para onde vai o bruto", style = T.h2, color = c.ink)
        if (denom <= 0) {
            Text("Sem valores neste período.", style = T.small, color = c.muted)
            return@JCard
        }
        val parts = listOf(Triple("Líquido", net, c.primary)) +
            deds.mapIndexed { i, l -> Triple(l.deduction.name, l.amount, c.deductionShades[i % c.deductionShades.size]) }
        Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            parts.filter { it.second > 0 }.forEach { (_, v, color) ->
                Box(Modifier.weight((v / denom).toFloat()).fillMaxHeight().background(color))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEachIndexed { i, (name, v, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    Text(name, style = if (i == 0) T.bodyStrong else T.body, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(Fmt.money(v), style = T.bodyStrong, color = c.ink)
                    Text("${(v / denom * 100).roundToInt()}%", style = T.small, color = c.muted, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                }
            }
        }
    }
}

@Composable
private fun InsightsCard(ledger: Ledger, view: ViewPeriod, range: DateRange, sum: Summary) {
    val c = J.colors
    val info = remember(ledger) { if (ledger.employmentIsClt()) ledger.vacation() else null }
    val parts = Periods.parts(view, range, ledger.settings.weekStartsSunday)
    val best = remember(ledger, range) { parts.map { it to ledger.gross(it) }.maxByOrNull { it.second } }
    val yearRange = Periods.year(range.start)
    val yearDays = remember(ledger, yearRange) { ledger.presenceCount(yearRange) }
    val partNoun = when (view) {
        ViewPeriod.WEEK -> "dia"
        ViewPeriod.YEAR -> "mês"
        else -> "semana"
    }
    JCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
        if (info != null) {
            Insight(
                JIcons.Sun, c.amberSoft, c.amberText,
                "Faltas no período de férias",
                if (info.current.absences <= 5) "Até 5 mantém os 30 dias" else "Direito atual: ${info.current.entitlement} dias",
                "${info.current.absences} de 5",
            )
            LineDivider()
        }
        if (best != null && best.second > 0) {
            val label = when (view) {
                ViewPeriod.WEEK -> Fmt.longDay(best.first.start)
                ViewPeriod.YEAR -> Fmt.monthYear(best.first.start)
                else -> Fmt.shortRange(best.first)
            }
            Insight(JIcons.TrendUp, c.primarySoft, c.primary, "Melhor $partNoun", label, Fmt.money(best.second))
            LineDivider()
        }
        Insight(JIcons.Calendar, c.purpleSoft, c.purpleText, "Dias trabalhados no ano", yearRange.start.year.toString(), yearDays.toString())
        LineDivider()
        Insight(JIcons.Close, c.redSoft, c.redText, "Faltas no período", Fmt.rangeLabel(view, range), sum.absences.toString())
    }
}

@Composable
private fun Insight(icon: ImageVector, bg: Color, tint: Color, title: String, sub: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon, bg, tint, size = 36.dp, iconSize = 18.dp, radius = 11.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = T.h3.copy(fontSize = 14.sp), color = J.colors.ink)
            Text(sub, style = T.tiny.copy(fontWeight = FontWeight.Medium), color = J.colors.muted)
        }
        Text(value, style = T.h3.copy(fontWeight = FontWeight.ExtraBold), color = J.colors.ink)
    }
}
