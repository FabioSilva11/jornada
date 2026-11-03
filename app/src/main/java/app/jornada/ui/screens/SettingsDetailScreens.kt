package app.jornada.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.jornada.data.CustomHoliday
import app.jornada.data.Employment
import app.jornada.data.PayBasis
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.ui.AppViewModel
import app.jornada.ui.components.ChoiceButton
import app.jornada.ui.components.ChoiceRow
import app.jornada.ui.components.DateField
import app.jornada.ui.components.DatePickDialog
import app.jornada.ui.components.FieldLabel
import app.jornada.ui.components.IconCircleButton
import app.jornada.ui.components.JCard
import app.jornada.ui.components.JTextField
import app.jornada.ui.components.LineDivider
import app.jornada.ui.components.PrimaryButton
import app.jornada.ui.components.SectionLabel
import app.jornada.ui.components.SegmentedTabs
import app.jornada.ui.components.SwitchRow
import app.jornada.ui.components.TonalButton
import app.jornada.ui.components.TopBar
import app.jornada.ui.components.WeekdayPicker
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import java.time.LocalDate

/** Estrutura comum das subtelas de ajustes: barra com voltar, conteúdo rolável e botão Salvar. */
@Composable
private fun SettingsPage(
    title: String,
    nav: NavController,
    onSave: (() -> Unit)?,
    saveEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(title, { nav.popBackStack() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) { content() }
        if (onSave != null) {
            Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)) {
                PrimaryButton("Salvar", {
                    onSave()
                    nav.popBackStack()
                }, Modifier.fillMaxWidth(), enabled = saveEnabled)
            }
        }
    }
}

private val basisOrder = listOf(PayBasis.DAILY, PayBasis.MONTHLY, PayBasis.HOURLY)

@Composable
fun PaySettingsScreen(vm: AppViewModel, ledger: Ledger, nav: NavController) {
    var draft by rememberSaveable(stateSaver = SettingsSaver) { mutableStateOf(ledger.settings) }
    val preview = Ledger(ledger.data.copy(settings = draft), ledger.today)
    val base = preview.baseDaily(ledger.today)

    SettingsPage("Ganhos e pagamento", nav, onSave = { vm.updateSettings { draft } }, saveEnabled = draft.payValueOk()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("Como você recebe")
            SegmentedTabs(listOf("Por dia", "Por mês", "Por hora"), basisOrder.indexOf(draft.payBasis), {
                draft = draft.copy(payBasis = basisOrder[it])
            })
        }
        PayValueFields(draft) { draft = it }
        if (draft.payBasis != PayBasis.DAILY && base > 0) {
            Helper("Neste mês o seu dia vale ${Fmt.money(base)}.")
        }
        PayPeriodFields(draft) { draft = it }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { FieldLabel("Meio período vale") }
                Text(Fmt.money(base * draft.halfDayPercent / 100.0), style = T.smallStrong, color = J.colors.primary)
            }
            val halfOptions = listOf(25, 50, 75)
            ChoiceRow(halfOptions.map { "$it%" }, halfOptions.indexOf(draft.halfDayPercent), {
                draft = draft.copy(halfDayPercent = halfOptions[it])
            })
        }

        SectionLabel("Fim de semana e feriado")
        RateCard("Sábado", draft.saturdayPercent, 6 in draft.workDays, base) { draft = draft.copy(saturdayPercent = it) }
        RateCard("Domingo", draft.sundayPercent, 7 in draft.workDays, base) { draft = draft.copy(sundayPercent = it) }
        RateCard("Feriado trabalhado", draft.holidayPercent, false, base) { draft = draft.copy(holidayPercent = it) }

        SectionLabel("Valor da semana")
        JCard(padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
            SwitchRow(
                "Mostrar no fim de semana",
                "Sábado e domingo o Início mostra quanto a semana rendeu.",
                draft.showWeekendSummary,
                { draft = draft.copy(showWeekendSummary = it) },
            )
            LineDivider()
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Semana conta de", style = T.h3, color = J.colors.ink)
                ChoiceRow(
                    listOf("Domingo a sábado", "Segunda a domingo"),
                    if (draft.weekStartsSunday) 0 else 1,
                    { draft = draft.copy(weekStartsSunday = it == 0) },
                    height = 44.dp,
                )
            }
        }
        JCard(padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
            SwitchRow(
                "Atestado conta como dia pago",
                "Nunca conta como falta para as férias.",
                draft.sickPaid,
                { draft = draft.copy(sickPaid = it) },
            )
        }
    }
}

@Composable
private fun RateCard(title: String, percent: Int, inSchedule: Boolean, base: Double, onChange: (Int) -> Unit) {
    val c = J.colors
    val presets = listOf(100, 150, 200)
    var custom by remember { mutableStateOf(percent !in presets) }
    var text by remember { mutableStateOf(percent.toString()) }
    JCard(spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = T.h3, color = c.ink, modifier = Modifier.weight(1f))
            Text(Fmt.money(base * (if (inSchedule) 100 else percent) / 100.0), style = T.h2.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
        }
        if (inSchedule) {
            Text("Está na sua escala de trabalho: vale como um dia normal.", style = T.small, color = c.muted)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Igual" to 100, "+50%" to 150, "+100%" to 200).forEach { (label, p) ->
                    ChoiceButton(label, !custom && percent == p, {
                        custom = false
                        onChange(p)
                    }, Modifier.weight(1f), height = 40.dp)
                }
                ChoiceButton("Outro", custom, { custom = true }, Modifier.weight(1f), height = 40.dp)
            }
            if (custom) {
                JTextField(
                    text,
                    { t ->
                        text = t.filter { it.isDigit() }.take(3)
                        text.toIntOrNull()?.takeIf { it > 0 }?.let(onChange)
                    },
                    keyboardType = KeyboardType.Number,
                    suffix = "% da diária",
                    placeholder = "150",
                )
            }
        }
    }
}

@Composable
fun DaysSettingsScreen(vm: AppViewModel, ledger: Ledger, nav: NavController) {
    var draft by rememberSaveable(stateSaver = SettingsSaver) { mutableStateOf(ledger.settings) }
    var hours by rememberSaveable { mutableStateOf(Fmt.numberInput(ledger.settings.hoursPerDay)) }
    SettingsPage("Dias e jornada", nav, onSave = { vm.updateSettings { draft } }, saveEnabled = draft.workDays.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("Dias de trabalho")
            WeekdayPicker(draft.workDays.toSet(), { d ->
                val set = draft.workDays.toMutableSet()
                if (!set.add(d)) set.remove(d)
                draft = draft.copy(workDays = set.sorted())
            }, draft.weekStartsSunday)
            Helper("Os outros dias contam como folga. Se trabalhar neles, vale o valor de fim de semana.")
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Horas por dia")
            JTextField(
                hours,
                { t ->
                    hours = t.filter { it.isDigit() || it == ',' || it == '.' }
                    draft = draft.copy(hoursPerDay = Fmt.parseMoney(hours) ?: draft.hoursPerDay)
                },
                keyboardType = KeyboardType.Decimal,
                suffix = "h",
            )
            Helper("Usado no cálculo de quem recebe por hora.")
        }
    }
}

@Composable
fun HolidaysScreen(vm: AppViewModel, ledger: Ledger, nav: NavController) {
    val c = J.colors
    val s = ledger.settings
    var adding by remember { mutableStateOf(false) }
    val upcoming = remember(ledger) {
        (ledger.holidays(ledger.today.year) + ledger.holidays(ledger.today.year + 1))
            .filterKeys { !it.isBefore(ledger.today) }
            .toSortedMap()
            .entries.take(8)
    }

    SettingsPage("Feriados", nav, onSave = null) {
        JCard(padding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
            SwitchRow(
                "Feriados nacionais",
                "Confraternização, Paixão de Cristo, Tiradentes, Trabalho, Independência, Aparecida, Finados, República, Consciência Negra e Natal.",
                s.nationalHolidays,
                { on -> vm.updateSettings { it.copy(nationalHolidays = on) } },
            )
            LineDivider()
            SwitchRow(
                "Carnaval e Corpus Christi",
                "Pontos facultativos. Ligue se você não trabalha nesses dias.",
                s.optionalHolidays,
                { on -> vm.updateSettings { it.copy(optionalHolidays = on) } },
            )
        }

        SectionLabel("Feriados da sua cidade e estado")
        JCard(padding = PaddingValues(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
            if (s.customHolidays.isEmpty()) {
                Text(
                    "Nenhum ainda. Adicione o aniversário da cidade ou os feriados do seu estado.",
                    style = T.small,
                    color = c.muted,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }
            s.customHolidays.sortedBy { it.date.substring(5) }.forEach { h ->
                val d = LocalDate.parse(h.date)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(h.name, style = T.h3, color = c.ink)
                        Text(
                            if (h.yearly) "Todo ano em ${Fmt.dayMonth(d)}" else Fmt.ddmmyyyy(d),
                            style = T.small,
                            color = c.muted,
                        )
                    }
                    IconCircleButton(JIcons.Trash, "Apagar feriado", {
                        vm.updateSettings { it.copy(customHolidays = it.customHolidays - h) }
                    }, background = Color.Transparent, tint = c.muted)
                }
            }
        }
        TonalButton("Adicionar feriado", { adding = true }, Modifier.fillMaxWidth(), icon = JIcons.Plus)

        SectionLabel("Próximos feriados")
        JCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            if (upcoming.isEmpty()) Text("Nenhum feriado ativo.", style = T.small, color = c.muted, modifier = Modifier.padding(vertical = 10.dp))
            upcoming.forEachIndexed { i, (d, name) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = T.body, color = c.ink, modifier = Modifier.weight(1f))
                    Text(Fmt.ddmmyyyy(d), style = T.smallStrong, color = c.muted)
                }
                if (i < upcoming.lastIndex) LineDivider()
            }
        }
    }

    if (adding) {
        AddHolidayDialog(
            onDismiss = { adding = false },
            onAdd = { h -> vm.updateSettings { it.copy(customHolidays = it.customHolidays + h) } },
        )
    }
}

@Composable
private fun AddHolidayDialog(onDismiss: () -> Unit, onAdd: (CustomHoliday) -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var yearly by remember { mutableStateOf(true) }
    var picking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = J.colors.surface,
        title = { Text("Novo feriado", style = T.title, color = J.colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                JTextField(name, { name = it }, placeholder = "Ex.: Aniversário da cidade")
                DateField(date?.let { Fmt.ddmmyyyy(it) } ?: "Escolher data", { picking = true })
                SwitchRow("Repete todo ano", null, yearly, { yearly = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    date?.let { onAdd(CustomHoliday(it.toString(), name.trim(), yearly)) }
                    onDismiss()
                },
                enabled = name.isNotBlank() && date != null,
            ) { Text("Adicionar", style = T.button, color = if (name.isNotBlank() && date != null) J.colors.primary else J.colors.muted) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", style = T.button, color = J.colors.muted) }
        },
    )
    if (picking) DatePickDialog(date, { picking = false }, { date = it })
}

@Composable
fun VacationSettingsScreen(vm: AppViewModel, ledger: Ledger, nav: NavController) {
    val c = J.colors
    var draft by rememberSaveable(stateSaver = SettingsSaver) { mutableStateOf(ledger.settings) }
    var picking by remember { mutableStateOf(false) }
    SettingsPage("Regra de férias", nav, onSave = { vm.updateSettings { draft } }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("Tipo de vínculo")
            ChoiceRow(
                listOf("CLT (carteira)", "Autônomo"),
                if (draft.employment == Employment.CLT) 0 else 1,
                { draft = draft.copy(employment = if (it == 0) Employment.CLT else Employment.AUTONOMO) },
            )
        }
        if (draft.employment == Employment.CLT) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Data de admissão")
                DateField(draft.admissionDate?.let { Fmt.ddmmyyyy(LocalDate.parse(it)) } ?: "Toque para escolher", { picking = true })
                Helper("O período aquisitivo começa nessa data e renova a cada 12 meses.")
            }
            JCard(spacing = 8.dp) {
                Text("Como o app calcula", style = T.h3, color = c.ink)
                Text("• A cada 12 meses de trabalho você ganha férias, que a empresa tem mais 12 meses para conceder.", style = T.small, color = c.muted)
                Text("• Faltas sem justificativa reduzem os dias: até 5 faltas, 30 dias; 6 a 14, 24; 15 a 23, 18; 24 a 32, 12; mais de 32, perde o direito.", style = T.small, color = c.muted)
                Text("• Valor: remuneração do mês × dias ÷ 30, mais 1/3. Dá para vender até 1/3 dos dias (abono).", style = T.small, color = c.muted)
                Text("• É uma estimativa: confira sempre com o seu holerite.", style = T.small, color = c.muted)
            }
        } else {
            JCard(spacing = 8.dp) {
                Text("Reserva de férias", style = T.h3, color = c.ink)
                Text(
                    "Como autônomo, ninguém paga suas férias. O app sugere guardar 11,1% de cada dia (1/12 do ano mais 1/3), para você poder parar 30 dias sem perder renda.",
                    style = T.small,
                    color = c.muted,
                )
            }
        }
    }
    if (picking) {
        DatePickDialog(draft.admissionDate?.let { LocalDate.parse(it) }, { picking = false }, { draft = draft.copy(admissionDate = it.toString()) })
    }
}
