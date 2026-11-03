package app.jornada.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.jornada.data.DayRecord
import app.jornada.data.DayStatus
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.ui.AppViewModel
import app.jornada.ui.components.ChoiceButton
import app.jornada.ui.components.FieldLabel
import app.jornada.ui.components.JTextField
import app.jornada.ui.components.MoneyField
import app.jornada.ui.components.OutlineButton
import app.jornada.ui.components.PrimaryButton
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

private val sheetOptions = listOf(
    DayStatus.WORKED to "Trabalhei",
    DayStatus.HALF to "Meio período",
    DayStatus.ABSENT to "Faltei",
    DayStatus.SICK to "Atestado",
    DayStatus.OFF to "Folga",
    DayStatus.HOLIDAY to "Feriado",
    DayStatus.VACATION to "Férias",
)

/** Folha "Registrar dia": status, valor, extra e observação. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(vm: AppViewModel, ledger: Ledger, date: LocalDate) {
    val c = J.colors
    val rec = ledger.record(date)
    val holiday = ledger.holidayName(date)
    val suggested = rec?.status ?: when {
        ledger.isWorkday(date) -> DayStatus.WORKED
        holiday != null -> DayStatus.HOLIDAY
        else -> DayStatus.OFF
    }
    var status by remember(date) { mutableStateOf(suggested) }
    var valueText by remember(date) { mutableStateOf(rec?.customValue?.let { Fmt.moneyInput(it) } ?: "") }
    var extraText by remember(date) { mutableStateOf(rec?.extra?.takeIf { it > 0 }?.let { Fmt.moneyInput(it) } ?: "") }
    var note by remember(date) { mutableStateOf(rec?.note ?: "") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { vm.closeDay() }
    }

    val default = ledger.defaultValue(date, status)
    val pays = ledger.paysSomething(status)
    val total = (if (pays) Fmt.parseMoney(valueText) ?: default else 0.0) + (Fmt.parseMoney(extraText) ?: 0.0)

    val subtitle = when {
        holiday != null -> "Feriado: $holiday"
        ledger.isWorkday(date) -> "Dia de trabalho previsto · diária ${Fmt.money(ledger.baseDaily(date))}"
        ledger.defaultValue(date, DayStatus.WORKED) > 0 ->
            "${Fmt.weekday(date)} fora da escala · paga ${Fmt.money(ledger.defaultValue(date, DayStatus.WORKED))}"
        else -> "Fora da escala de trabalho"
    }

    ModalBottomSheet(
        onDismissRequest = { vm.closeDay() },
        sheetState = sheetState,
        containerColor = c.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.border) },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Fmt.longDay(date), style = T.title.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
                Text(subtitle, style = T.small.copy(fontWeight = FontWeight.Medium), color = c.muted)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel("Como foi o dia?")
                sheetOptions.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (st, label) ->
                            ChoiceButton(
                                label,
                                status == st,
                                {
                                    status = st
                                    if (!ledger.paysSomething(st)) valueText = ""
                                },
                                Modifier.weight(1f),
                                icon = JIcons.Check,
                            )
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }

            if (pays) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Valor do dia")
                    MoneyField(valueText, { valueText = it }, placeholder = Fmt.moneyInput(default).ifEmpty { "0,00" })
                    Text("Vazio usa o padrão: ${Fmt.money(default)}", style = T.tiny.copy(fontWeight = FontWeight.Medium), color = c.muted)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Extra do dia")
                MoneyField(extraText, { extraText = it })
                Text("Hora extra, gorjeta ou bônus. Entra no bruto do dia.", style = T.tiny.copy(fontWeight = FontWeight.Medium), color = c.muted)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Observação")
                JTextField(note, { note = it }, placeholder = "Opcional", singleLine = false)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (rec != null) {
                    OutlineButton("Apagar", {
                        vm.clearDay(date)
                        close()
                    }, Modifier.weight(1f), height = 54.dp, color = c.redText, borderColor = c.redBorder)
                } else {
                    OutlineButton("Cancelar", close, Modifier.weight(1f), height = 54.dp)
                }
                PrimaryButton(
                    if (total > 0) "Salvar · + ${Fmt.money(total)}" else "Salvar",
                    {
                        val typed = if (pays) Fmt.parseMoney(valueText) else null
                        val custom = typed?.takeIf { abs(it - default) > 0.004 }
                        vm.setDay(DayRecord(date.toString(), status, custom, Fmt.parseMoney(extraText) ?: 0.0, note.trim()))
                        close()
                    },
                    Modifier.weight(2f),
                    height = 54.dp,
                )
            }
        }
    }
}
