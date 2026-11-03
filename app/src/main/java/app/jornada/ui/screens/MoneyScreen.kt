package app.jornada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.jornada.data.DeductionCategory
import app.jornada.domain.DateRange
import app.jornada.domain.DeductionLine
import app.jornada.domain.EarnKind
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.domain.Periods
import app.jornada.domain.Summary
import app.jornada.domain.ViewPeriod
import app.jornada.domain.toView
import app.jornada.ui.Routes
import app.jornada.ui.components.IconBadge
import app.jornada.ui.components.InfoNote
import app.jornada.ui.components.JCard
import app.jornada.ui.components.LineDivider
import app.jornada.ui.components.PeriodNav
import app.jornada.ui.components.PillGroup
import app.jornada.ui.components.PrimaryButton
import app.jornada.ui.components.ProgressBar
import app.jornada.ui.components.ScreenHeader
import app.jornada.ui.components.SegmentedTabs
import app.jornada.ui.components.TonalButton
import app.jornada.ui.components.ValueRow
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private val viewOptions = listOf(ViewPeriod.WEEK, ViewPeriod.FORTNIGHT, ViewPeriod.MONTH)

@Composable
fun MoneyScreen(ledger: Ledger, nav: NavController, initialTab: Int) {
    val s = ledger.settings
    val today = ledger.today
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var view by rememberSaveable { mutableStateOf(s.payPeriod.toView()) }
    var anchorText by rememberSaveable { mutableStateOf(today.toString()) }
    val range = Periods.of(view, LocalDate.parse(anchorText), s.weekStartsSunday)
    val sum = remember(ledger, range) { ledger.summary(range) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ScreenHeader("Dinheiro")
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = if (tab == 1) 100.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SegmentedTabs(listOf("Resumo", "Descontos"), tab, { tab = it })
                PillGroup(listOf("Semana", "Quinzena", "Mês"), viewOptions.indexOf(view), {
                    if (today in range) anchorText = today.toString()
                    view = viewOptions[it]
                })
                PeriodNav(
                    Fmt.rangeLabel(view, range),
                    onPrev = { anchorText = Periods.shift(view, range, -1, s.weekStartsSunday).start.toString() },
                    onNext = { anchorText = Periods.shift(view, range, 1, s.weekStartsSunday).start.toString() },
                )
                if (tab == 0) SummaryContent(ledger, view, range, sum) else DeductionsContent(sum, nav)
            }
        }
        if (tab == 1) {
            PrimaryButton(
                "Novo desconto",
                { nav.navigate(Routes.deduction("new")) },
                Modifier.align(Alignment.BottomEnd).padding(20.dp).shadow(6.dp, RoundedCornerShape(16.dp)),
                icon = JIcons.Plus,
            )
        }
    }
}

@Composable
private fun SummaryContent(ledger: Ledger, view: ViewPeriod, range: DateRange, sum: Summary) {
    val c = J.colors
    val s = ledger.settings
    val current = ledger.today in range

    JCard(shape = RoundedCornerShape(24.dp), padding = PaddingValues(20.dp), spacing = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (current) "LÍQUIDO ATÉ AGORA" else "LÍQUIDO", style = T.label, color = c.muted)
            Text(Fmt.money(sum.net), style = T.display, color = if (sum.net < 0) c.redText else c.primary)
            if (current) {
                val next = ledger.nextPayday()
                Text(
                    "Próximo pagamento ${Fmt.ddmm(next)} · ${Fmt.paydayRule(s.payPeriod, s.payDay1, s.payDay2, s.payWeekday)}",
                    style = T.small.copy(fontWeight = FontWeight.SemiBold),
                    color = c.muted,
                )
            }
        }
        if (sum.gross > 0) {
            val netShare = (sum.net.coerceAtLeast(0.0) / sum.gross).toFloat().coerceIn(0f, 1f)
            Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (netShare > 0f) Box(Modifier.weight(netShare).fillMaxHeight().background(c.primary))
                if (netShare < 1f) Box(Modifier.weight(1f - netShare).fillMaxHeight().background(c.redBorder))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueRow("Bruto", Fmt.money(sum.gross))
            ValueRow("Descontos", Fmt.minus(sum.totalDeductions), valueColor = c.redText)
            LineDivider()
            ValueRow("Líquido", Fmt.money(sum.net), strong = true)
        }
    }

    if (current) {
        val (proj, left) = remember(ledger, range) { ledger.projection(range) }
        if (left > 0) {
            val periodName = when (view) {
                ViewPeriod.WEEK -> "a semana"
                ViewPeriod.FORTNIGHT -> "a quinzena"
                ViewPeriod.MONTH -> Fmt.months[range.start.monthValue - 1]
                ViewPeriod.YEAR -> "o ano"
            }
            JCard(color = c.primarySoft) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(JIcons.TrendUp, null, Modifier.size(22.dp), tint = c.onPrimarySoft)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Se mantiver o ritmo, $periodName fecha em ${Fmt.money(sum.gross + proj)} bruto",
                            style = T.bodyStrong,
                            color = c.onPrimarySoft,
                        )
                        Text(
                            if (left == 1) "Ainda falta 1 dia de trabalho previsto." else "Ainda faltam $left dias de trabalho previstos.",
                            style = T.small,
                            color = c.onPrimarySoft,
                        )
                    }
                }
            }
        }
    }

    JCard(spacing = 12.dp) {
        Text("Como o bruto foi formado", style = T.h2, color = c.ink)
        if (sum.byKind.isEmpty() && sum.extras == 0.0) {
            Text("Nenhum dia com valor neste período. Marque os dias no Início ou no Calendário.", style = T.small, color = c.muted)
        } else {
            sum.byKind.forEach { (kind, t) ->
                val extra = when (kind) {
                    EarnKind.SATURDAY -> " (+${s.saturdayPercent - 100}%)"
                    EarnKind.SUNDAY -> " (+${s.sundayPercent - 100}%)"
                    EarnKind.HOLIDAY -> " (+${s.holidayPercent - 100}%)"
                    EarnKind.HALF -> " (${s.halfDayPercent}% da diária)"
                    else -> ""
                }
                val detail = if (abs(t.value - t.count * t.unit) < 0.01) "${t.count} × ${Fmt.money(t.unit)}$extra" else Fmt.days(t.count)
                ValueRow(kind.label, Fmt.money(t.value), sub = detail)
            }
            if (sum.extras > 0) ValueRow("Extras", Fmt.money(sum.extras), sub = "horas extras, gorjetas, bônus")
        }
        if (sum.absences > 0) {
            Text(
                if (sum.absences == 1) "1 falta neste período" else "${sum.absences} faltas neste período",
                style = T.small.copy(fontWeight = FontWeight.SemiBold),
                color = c.redText,
            )
        }
    }

    PartsCard(ledger, view, range)
}

@Composable
private fun PartsCard(ledger: Ledger, view: ViewPeriod, range: DateRange) {
    val c = J.colors
    val parts = Periods.parts(view, range, ledger.settings.weekStartsSunday)
    val sums = remember(ledger, range) { parts.map { ledger.summary(it) } }
    val max = sums.maxOfOrNull { it.gross }?.takeIf { it > 0 } ?: 1.0
    JCard(spacing = 14.dp) {
        Text(if (view == ViewPeriod.WEEK) "Por dia" else "Por semana", style = T.h2, color = c.ink)
        parts.forEachIndexed { i, p ->
            val ps = sums[i]
            if (view == ViewPeriod.WEEK) {
                val d = p.start
                val rec = ledger.record(d)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${Fmt.weekdayShort[d.dayOfWeek.value - 1]} ${d.dayOfMonth}",
                        style = T.bodyStrong,
                        color = if (d == ledger.today) c.primary else c.ink,
                        modifier = Modifier.width(72.dp),
                    )
                    Text(
                        rec?.let { Fmt.status(it.status) } ?: ledger.holidayName(d) ?: "—",
                        style = T.small,
                        color = if (rec?.status == app.jornada.data.DayStatus.ABSENT) c.redText else c.muted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(if (ps.gross > 0) Fmt.money(ps.gross) else "—", style = T.bodyStrong, color = if (ps.gross > 0) c.ink else c.muted)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val detail = buildString {
                            append(Fmt.daysDecimal(ps.workedEquivalent))
                            if (ps.absences > 0) append(if (ps.absences == 1) " · 1 falta" else " · ${ps.absences} faltas")
                            if (ledger.today in p) append(" · em andamento")
                        }
                        Text(Fmt.shortRange(p), style = T.bodyStrong, color = c.ink)
                        Text(" · $detail", style = T.small, color = c.muted, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(Fmt.money(ps.gross), style = T.bodyStrong, color = c.ink)
                    }
                    ProgressBar(
                        (ps.gross / max).toFloat(),
                        if (ledger.today in p) c.primaryMid else c.primary,
                        c.surface2,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeductionsContent(sum: Summary, nav: NavController) {
    val c = J.colors
    val lines = sum.deductions.sortedWith(
        compareByDescending<DeductionLine> { it.deduction.active }.thenByDescending { it.amount },
    )
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Descontos no período", style = T.small.copy(fontWeight = FontWeight.SemiBold), color = c.muted)
            Text(Fmt.money(sum.totalDeductions), style = T.big, color = c.redText)
        }
        if (sum.gross > 0) {
            Text(
                "${(sum.totalDeductions / sum.gross * 100).roundToInt()}% do bruto",
                style = T.small.copy(fontWeight = FontWeight.SemiBold),
                color = c.muted,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
    if (lines.isEmpty()) {
        JCard(spacing = 12.dp) {
            Text("Nenhum desconto cadastrado", style = T.h2, color = c.ink)
            Text(
                "Adicione vale-transporte, refeição, adiantamento, uniforme ou empréstimo. O app tira do bruto e mostra o líquido.",
                style = T.small,
                color = c.muted,
            )
            TonalButton("Adicionar desconto", { nav.navigate(Routes.deduction("new")) }, icon = JIcons.Plus)
        }
    } else {
        JCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            lines.forEachIndexed { i, line ->
                DeductionRow(line) { nav.navigate(Routes.deduction(line.deduction.id)) }
                if (i < lines.lastIndex) LineDivider()
            }
        }
        InfoNote("Descontos por dia acompanham sozinhos os dias que você marca. Toque em um desconto para editar ou pausar.")
    }
}

@Composable
private fun DeductionRow(line: DeductionLine, onClick: () -> Unit) {
    val c = J.colors
    val d = line.deduction
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(categoryIcon(d.category), if (d.active) c.redSoft else c.surface2, if (d.active) c.redText else c.muted)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(d.name, style = T.h3, color = if (d.active) c.ink else c.muted)
            Text(line.detail, style = T.small, color = c.muted)
        }
        Text(if (line.amount > 0) Fmt.money(line.amount) else "—", style = T.bodyStrong, color = if (line.amount > 0) c.ink else c.muted)
    }
}

fun categoryIcon(cat: DeductionCategory) = when (cat) {
    DeductionCategory.TRANSPORTE -> JIcons.Bus
    DeductionCategory.ALIMENTACAO -> JIcons.Utensils
    DeductionCategory.ADIANTAMENTO -> JIcons.Banknote
    DeductionCategory.UNIFORME -> JIcons.Shirt
    DeductionCategory.EMPRESTIMO -> JIcons.Coin
    DeductionCategory.OUTRO -> JIcons.Tag
}

fun categoryLabel(cat: DeductionCategory) = when (cat) {
    DeductionCategory.TRANSPORTE -> "Transporte"
    DeductionCategory.ALIMENTACAO -> "Alimentação"
    DeductionCategory.ADIANTAMENTO -> "Adiantamento"
    DeductionCategory.UNIFORME -> "Uniforme"
    DeductionCategory.EMPRESTIMO -> "Empréstimo"
    DeductionCategory.OUTRO -> "Outro"
}
