package app.jornada.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.jornada.data.PayBasis
import app.jornada.data.PayPeriod
import app.jornada.data.Repository
import app.jornada.data.Settings
import app.jornada.domain.Fmt
import app.jornada.ui.components.ChoiceRow
import app.jornada.ui.components.FieldLabel
import app.jornada.ui.components.JTextField
import app.jornada.ui.components.MoneyField
import app.jornada.ui.components.Stepper
import app.jornada.ui.components.WeekdayPicker
import app.jornada.ui.theme.J
import app.jornada.ui.theme.T

/** Guarda um rascunho de ajustes entre recriações da tela. */
val SettingsSaver = Saver<Settings, String>(
    save = { Repository.json.encodeToString(Settings.serializer(), it) },
    restore = { Repository.json.decodeFromString(Settings.serializer(), it) },
)

@Composable
fun Helper(text: String) {
    Text(text, style = T.small.copy(fontWeight = FontWeight.Medium), color = J.colors.muted)
}

/** Campos de valor conforme a forma de pagamento (diária, salário ou hora). */
@Composable
fun PayValueFields(draft: Settings, onChange: (Settings) -> Unit) {
    var daily by remember { mutableStateOf(Fmt.moneyInput(draft.dailyRate)) }
    var monthly by remember { mutableStateOf(Fmt.moneyInput(draft.monthlySalary)) }
    var hourly by remember { mutableStateOf(Fmt.moneyInput(draft.hourlyRate)) }
    var hours by remember { mutableStateOf(Fmt.numberInput(draft.hoursPerDay)) }

    when (draft.payBasis) {
        PayBasis.DAILY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Valor da diária")
            MoneyField(daily, {
                daily = it
                onChange(draft.copy(dailyRate = Fmt.parseMoney(it) ?: 0.0))
            })
        }
        PayBasis.MONTHLY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Salário mensal")
            MoneyField(monthly, {
                monthly = it
                onChange(draft.copy(monthlySalary = Fmt.parseMoney(it) ?: 0.0))
            })
            Helper("O valor de cada dia é o salário dividido pelos dias de trabalho previstos no mês.")
        }
        PayBasis.HOURLY -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Valor da hora")
                MoneyField(hourly, {
                    hourly = it
                    onChange(draft.copy(hourlyRate = Fmt.parseMoney(it) ?: 0.0))
                })
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Horas por dia")
                JTextField(
                    hours,
                    { t ->
                        hours = t.filter { it.isDigit() || it == ',' || it == '.' }
                        onChange(draft.copy(hoursPerDay = Fmt.parseMoney(hours) ?: 0.0))
                    },
                    keyboardType = KeyboardType.Decimal,
                    suffix = "h",
                )
            }
        }
    }
}

fun Settings.payValueOk(): Boolean = when (payBasis) {
    PayBasis.DAILY -> dailyRate > 0
    PayBasis.MONTHLY -> monthlySalary > 0
    PayBasis.HOURLY -> hourlyRate > 0 && hoursPerDay > 0
}

private val periodOrder = listOf(PayPeriod.WEEKLY, PayPeriod.BIWEEKLY, PayPeriod.MONTHLY)

/** "Você recebe a cada": semana, quinzena ou mês, e o dia do pagamento. */
@Composable
fun PayPeriodFields(draft: Settings, onChange: (Settings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldLabel("Você recebe a cada")
        ChoiceRow(
            listOf("Semana", "Quinzena", "Mês"),
            periodOrder.indexOf(draft.payPeriod),
            { onChange(draft.copy(payPeriod = periodOrder[it])) },
        )
        when (draft.payPeriod) {
            PayPeriod.MONTHLY -> Stepper(
                draft.payDay1,
                { onChange(draft.copy(payDay1 = it)) },
                1..31,
                "Pagamento todo dia ${draft.payDay1}",
            )
            PayPeriod.BIWEEKLY -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Helper("1º pagamento")
                    Stepper(draft.payDay1, { onChange(draft.copy(payDay1 = it)) }, 1..31, "Dia ${draft.payDay1}")
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Helper("2º pagamento")
                    Stepper(draft.payDay2, { onChange(draft.copy(payDay2 = it)) }, 1..31, "Dia ${draft.payDay2}")
                }
            }
            PayPeriod.WEEKLY -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Helper("Dia do pagamento")
                WeekdayPicker(setOf(draft.payWeekday), { onChange(draft.copy(payWeekday = it)) }, draft.weekStartsSunday)
            }
        }
        Helper(
            when (draft.payPeriod) {
                PayPeriod.WEEKLY -> "Fechamento por semana. Descontos fixos e parcelas valem por semana."
                PayPeriod.BIWEEKLY -> "Quinzenas do dia 1 ao 15 e do 16 ao fim do mês. Descontos fixos valem por quinzena."
                PayPeriod.MONTHLY -> "Fechamento por mês. Descontos fixos e parcelas valem por mês."
            },
        )
    }
}
