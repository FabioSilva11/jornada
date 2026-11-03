package app.jornada.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.jornada.data.PayBasis
import app.jornada.data.VacationPlan
import app.jornada.domain.AcqPeriod
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.domain.Periods
import app.jornada.domain.VacationInfo
import app.jornada.ui.AppViewModel
import app.jornada.ui.Routes
import app.jornada.ui.components.ConfirmDialog
import app.jornada.ui.components.DateField
import app.jornada.ui.components.DatePickDialog
import app.jornada.ui.components.FieldLabel
import app.jornada.ui.components.IconBadge
import app.jornada.ui.components.IconCircleButton
import app.jornada.ui.components.JCard
import app.jornada.ui.components.LineDivider
import app.jornada.ui.components.PillGroup
import app.jornada.ui.components.PrimaryButton
import app.jornada.ui.components.ScreenHeader
import app.jornada.ui.components.Stepper
import app.jornada.ui.components.SwitchRow
import app.jornada.ui.components.TopBar
import app.jornada.ui.components.ValueRow
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import java.time.LocalDate
import java.util.UUID

@Composable
fun VacationScreen(vm: AppViewModel, ledger: Ledger, nav: NavController) {
    val clt = ledger.employmentIsClt()
    val admission = ledger.admission()
    val info = remember(ledger) { if (clt) ledger.vacation() else null }
    var pickAdmission by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(
            "Férias",
            subtitle = when {
                !clt -> "Autônomo · reserva própria"
                admission != null -> "Regra CLT · admissão em ${Fmt.ddmmyyyy(admission)}"
                else -> "Regra CLT"
            },
        )
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                !clt -> AutonomoContent(ledger)
                info == null -> NoAdmissionCard { pickAdmission = true }
                else -> CltContent(ledger, info)
            }
            PlansCard(ledger) { vm.deleteVacation(it) }
            PrimaryButton(
                "Planejar minhas férias",
                { nav.navigate(Routes.VACATION_PLAN) },
                Modifier.fillMaxWidth(),
                icon = JIcons.Calendar,
            )
        }
    }

    if (pickAdmission) {
        DatePickDialog(null, { pickAdmission = false }, { d -> vm.updateSettings { it.copy(admissionDate = d.toString()) } })
    }
}

@Composable
private fun NoAdmissionCard(onPick: () -> Unit) {
    val c = J.colors
    JCard(color = c.amberSoft, shape = RoundedCornerShape(24.dp), padding = PaddingValues(20.dp), spacing = 12.dp) {
        Text("Quando você começou nesse trabalho?", style = T.title, color = c.onAmberSoft)
        Text(
            "Com a data de admissão o app conta o período aquisitivo, os dias de direito, o prazo e o valor das férias.",
            style = T.body,
            color = c.onAmberSoft,
        )
        PrimaryButton("Informar data de admissão", onPick, Modifier.fillMaxWidth(), icon = JIcons.Calendar)
    }
}

@Composable
fun ProgressRing(fraction: Float, size: Int, track: Color, color: Color, content: @Composable () -> Unit) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
private fun CltContent(ledger: Ledger, info: VacationInfo) {
    val c = J.colors
    val cur = info.current
    val today = ledger.today
    var sell by rememberSaveable { mutableStateOf(false) }

    JCard(color = c.amberSoft, shape = RoundedCornerShape(24.dp), padding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            ProgressRing(info.monthsDone / 12f, 112, c.amberTrack, if (c.isDark) c.amber else Color(0xFFC97A0A)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${info.monthsDone}/12", style = T.value.copy(fontSize = 24.sp), color = c.onAmberSoft)
                    Text("meses", style = T.tiny, color = c.onAmberSoft)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PERÍODO AQUISITIVO", style = T.label, color = c.onAmberSoft)
                Text("${Fmt.ddmmyyyy(cur.start)} a ${Fmt.ddmmyyyy(cur.end)}", style = T.h2.copy(fontWeight = FontWeight.ExtraBold), color = c.onAmberSoft)
                Text(
                    "Faltam ${Fmt.duration(today, cur.end.plusDays(1))} para garantir as próximas férias.",
                    style = T.small,
                    color = c.onAmberSoft,
                )
            }
        }
    }

    info.pending.forEach { p -> PendingCard(p, today) }

    RightsCard(cur)

    ValueCard(ledger, info, sell) { sell = it }

    JCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
        DeadlineRow(JIcons.Check, c.primary, "Pode tirar a partir de", Fmt.ddmmyyyy(cur.end.plusDays(1)))
        LineDivider()
        DeadlineRow(JIcons.Clock, c.redText, "Precisa tirar até", Fmt.ddmmyyyy(cur.deadline))
        LineDivider()
        DeadlineRow(JIcons.Bell, c.muted, "Aviso da empresa", "30 dias antes")
    }
}

@Composable
private fun PendingCard(p: AcqPeriod, today: LocalDate) {
    val c = J.colors
    val overdue = p.deadline.isBefore(today)
    JCard(color = if (overdue) c.redSoft else c.surface) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(JIcons.Clock, c.redSoft, c.redText)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (overdue) "Prazo das férias vencido" else "Férias para tirar", style = T.h2, color = c.ink)
                Text(
                    "${Fmt.days(p.remaining)} do período ${Fmt.ddmmyyyy(p.start)} a ${Fmt.ddmmyyyy(p.end)}. " +
                        if (overdue) "O prazo acabou em ${Fmt.ddmmyyyy(p.deadline)}: pela CLT, a empresa deve pagar em dobro."
                        else "Prazo até ${Fmt.ddmmyyyy(p.deadline)}.",
                    style = T.small,
                    color = c.ink,
                )
            }
        }
    }
}

@Composable
private fun RightsCard(cur: AcqPeriod) {
    val c = J.colors
    val tiers = listOf(30 to "0 a 5", 24 to "6 a 14", 18 to "15 a 23", 12 to "24 a 32", 0 to "33 ou +")
    JCard(spacing = 14.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Seu direito hoje", style = T.small.copy(fontWeight = FontWeight.SemiBold), color = c.muted)
                Text("${cur.entitlement} dias", style = T.big, color = c.ink)
            }
            Box(
                Modifier.clip(RoundedCornerShape(15.dp)).background(c.redSoft).padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    if (cur.absences == 1) "1 falta no período" else "${cur.absences} faltas no período",
                    style = T.smallStrong,
                    color = c.redText,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tiers.forEach { (days, label) ->
                val here = days == cur.entitlement
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(if (here) c.primary else c.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            days.toString(),
                            style = T.h3.copy(fontWeight = FontWeight.ExtraBold),
                            color = when {
                                here -> c.onPrimary
                                days == 0 -> c.redText
                                else -> c.ink
                            },
                        )
                    }
                    Text(label, style = T.tiny.copy(fontSize = 11.sp), color = if (here) c.ink else c.muted)
                }
            }
        }
        val msg = Ledger.nextDrop(cur.absences)?.let { (n, d) ->
            "Mais ${if (n == 1) "1 falta" else "$n faltas"} e o direito cai para $d dias."
        } ?: "Com mais de 32 faltas o direito às férias deste período se perde."
        Text("Dias de férias por número de faltas sem justificativa. $msg Atestados e feriados não contam.", style = T.small, color = c.muted)
    }
}

@Composable
private fun ValueCard(ledger: Ledger, info: VacationInfo, sell: Boolean, onSell: (Boolean) -> Unit) {
    val c = J.colors
    val ent = info.current.entitlement
    val pay = ledger.vacationPay(info.baseMonthly, ent, sell)
    JCard(spacing = 12.dp) {
        Text("Quanto você vai receber", style = T.h2, color = c.ink)
        if (ent == 0) {
            Text("Sem direito a férias neste período (mais de 32 faltas).", style = T.small, color = c.redText)
            return@JCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueRow(
                if (ledger.settings.payBasis == PayBasis.MONTHLY) "Salário mensal" else "Média mensal (bruto)",
                Fmt.money(info.baseMonthly),
            )
            ValueRow("${pay.restDays} dias de férias", Fmt.money(pay.restPay))
            if (sell) ValueRow("Abono dos ${pay.soldDays} dias vendidos", Fmt.money(pay.abono))
            ValueRow("+ 1/3 constitucional", Fmt.money(pay.third))
            LineDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Estimativa", style = T.bodyStrong, color = c.ink, modifier = Modifier.weight(1f))
                Text(Fmt.money(pay.total), style = T.value, color = c.primary)
            }
        }
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(horizontal = 12.dp)) {
            SwitchRow(
                "Vender ${ent / 3} dias (abono)",
                "Descansa ${ent - ent / 3} dias e recebe os outros ${ent / 3} em dinheiro. Nesses dias você trabalha e recebe o salário normal.",
                sell,
                onSell,
            )
        }
        Text(
            if (ledger.settings.payBasis == PayBasis.MONTHLY) "Estimativa com o salário informado em Ajustes."
            else "Estimativa com a média do que você marcou nos últimos meses completos.",
            style = T.tiny.copy(fontWeight = FontWeight.Medium),
            color = c.muted,
        )
    }
}

@Composable
private fun DeadlineRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
        Text(label, style = T.body, color = J.colors.muted, modifier = Modifier.weight(1f))
        Text(value, style = T.bodyStrong, color = J.colors.ink)
    }
}

@Composable
private fun AutonomoContent(ledger: Ledger) {
    val c = J.colors
    val reserve = remember(ledger) { ledger.autonomoReserve() }
    val monthGross = remember(ledger) { ledger.gross(Periods.month(ledger.today)) }
    val daily = ledger.baseDaily(ledger.today)
    JCard(color = c.amberSoft, shape = RoundedCornerShape(24.dp), padding = PaddingValues(20.dp), spacing = 8.dp) {
        Text("RESERVA DE FÉRIAS", style = T.label, color = c.onAmberSoft)
        Text(Fmt.money(reserve), style = T.display, color = c.onAmberSoft)
        Text(
            "Sugestão: 11,1% de tudo que entrou nos últimos 12 meses (1/12 + 1/3, como na CLT).",
            style = T.small,
            color = c.onAmberSoft,
        )
    }
    JCard(spacing = 10.dp) {
        ValueRow("Separe deste mês", Fmt.money(monthGross / 9.0))
        if (daily > 0) ValueRow("A reserva equivale a", Fmt.days((reserve / daily).toInt()) + " de diária")
        Text(
            "Autônomo não tem férias pagas pela lei. Guardando essa parte de cada dia, dá para parar 30 dias por ano sem ficar sem renda.",
            style = T.small,
            color = c.muted,
        )
    }
}

@Composable
private fun PlansCard(ledger: Ledger, onDelete: (String) -> Unit) {
    val c = J.colors
    val plans = ledger.data.vacations.sortedBy { it.start }
    if (plans.isEmpty()) return
    var deleting by remember { mutableStateOf<VacationPlan?>(null) }
    JCard(padding = PaddingValues(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)) {
        Text("Férias planejadas", style = T.h2, color = c.ink)
        plans.forEach { p ->
            val start = LocalDate.parse(p.start)
            val end = start.plusDays((p.days - 1).toLong())
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${Fmt.ddmmyyyy(start)} a ${Fmt.ddmmyyyy(end)}", style = T.h3, color = c.ink)
                    Text(
                        Fmt.days(p.days) + " de descanso" + if (p.soldDays > 0) " · ${p.soldDays} vendidos" else "",
                        style = T.small,
                        color = c.muted,
                    )
                }
                IconCircleButton(JIcons.Trash, "Apagar férias planejadas", { deleting = p }, background = Color.Transparent, tint = c.muted)
            }
        }
    }
    deleting?.let { p ->
        ConfirmDialog(
            "Apagar estas férias?",
            "Os dias marcados como férias no calendário também serão desmarcados.",
            "Apagar",
            destructive = true,
            onDismiss = { deleting = null },
            onConfirm = { onDelete(p.id) },
        )
    }
}

@Composable
fun VacationPlanScreen(vm: AppViewModel, ledger: Ledger, nav: NavController) {
    val c = J.colors
    val clt = ledger.employmentIsClt()
    val info = remember(ledger) { if (clt) ledger.vacation() else null }
    val entitlement = (info?.pending?.firstOrNull()?.remaining ?: info?.current?.entitlement ?: 30).coerceAtLeast(5)
    var startText by rememberSaveable { mutableStateOf(ledger.today.plusDays(30).toString()) }
    var sell by rememberSaveable { mutableStateOf(false) }
    var days by rememberSaveable { mutableIntStateOf(entitlement) }
    var pick by remember { mutableStateOf(false) }

    val start = LocalDate.parse(startText)
    val sold = if (clt && sell) entitlement / 3 else 0
    val end = start.plusDays((days - 1).toLong())
    var back = end.plusDays(1)
    var guard = 0
    while (!ledger.isWorkday(back) && guard++ < 30) back = back.plusDays(1)

    val warnings = buildList {
        if (days < 5) add("Pela CLT, nenhum período de férias pode ter menos de 5 dias.")
        if ((1..2).any { k -> val d = start.plusDays(k.toLong()); ledger.isHoliday(d) || !ledger.isScheduled(d) }) {
            add("Pela CLT, as férias não podem começar nos 2 dias antes de um feriado ou do descanso semanal.")
        }
        if (clt && info != null && info.pending.isEmpty() && start.isBefore(info.current.end.plusDays(1))) {
            add("Você ainda não completou 12 meses neste período. Férias antes disso dependem de acordo com a empresa.")
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Planejar férias", { nav.popBackStack() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Começo das férias")
                DateField(Fmt.ddmmyyyy(start), { pick = true })
                Helper(Fmt.weekday(start))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel("Dias de descanso")
                val presets = listOf(30, 20, 15, 10, 5).filter { it <= entitlement }
                PillGroup(presets.map { "$it dias" }, presets.indexOf(days), { days = presets[it] })
                Stepper(days, { days = it }, 1..entitlement, "${Fmt.days(days)} corridos")
            }
            if (clt) {
                SwitchRow(
                    "Vender ${entitlement / 3} dias (abono)",
                    "Descansa ${entitlement - entitlement / 3} dias e recebe os outros em dinheiro.",
                    sell,
                    {
                        sell = it
                        days = if (it) entitlement - entitlement / 3 else entitlement
                    },
                )
            }
            JCard(color = c.amberSoft, spacing = 6.dp) {
                Text("RESUMO", style = T.label, color = c.onAmberSoft)
                Text("${Fmt.longDay(start)} a ${Fmt.longDay(end)}", style = T.h2, color = c.onAmberSoft)
                Text("${Fmt.days(days)} corridos · volta ao trabalho ${Fmt.longDay(back).replaceFirstChar { it.lowercase() }}", style = T.small, color = c.onAmberSoft)
                if (info != null && info.baseMonthly > 0) {
                    val value = info.baseMonthly * (days + sold) / 30.0 * 4.0 / 3.0
                    Text("Valor estimado: ${Fmt.money(value)} (com 1/3${if (sold > 0) " e abono" else ""})", style = T.smallStrong, color = c.onAmberSoft)
                }
            }
            warnings.forEach { w ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(JIcons.Info, null, Modifier.size(18.dp), tint = c.amberText)
                    Text(w, style = T.small, color = c.ink)
                }
            }
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)) {
            PrimaryButton(
                "Marcar no calendário",
                {
                    vm.addVacation(VacationPlan(UUID.randomUUID().toString(), start.toString(), days, sold))
                    nav.popBackStack()
                },
                Modifier.fillMaxWidth(),
                enabled = days >= 1,
            )
        }
    }

    if (pick) DatePickDialog(start, { pick = false }, { startText = it.toString() })
}
