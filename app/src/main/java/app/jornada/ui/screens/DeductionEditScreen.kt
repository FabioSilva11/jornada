package app.jornada.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.jornada.data.Deduction
import app.jornada.data.DeductionCategory
import app.jornada.data.DeductionType
import app.jornada.data.isPresence
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.domain.toView
import app.jornada.ui.AppViewModel
import app.jornada.ui.components.ConfirmDialog
import app.jornada.ui.components.DateField
import app.jornada.ui.components.DatePickDialog
import app.jornada.ui.components.FieldLabel
import app.jornada.ui.components.FilterPill
import app.jornada.ui.components.IconCircleButton
import app.jornada.ui.components.JCard
import app.jornada.ui.components.JTextField
import app.jornada.ui.components.MoneyField
import app.jornada.ui.components.PrimaryButton
import app.jornada.ui.components.Stepper
import app.jornada.ui.components.SwitchRow
import app.jornada.ui.components.TopBar
import app.jornada.ui.components.TypeCard
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import java.time.LocalDate
import java.util.UUID

private enum class Kind { PER_DAY, FIXED, PERCENT, ONCE }

private fun DeductionType.toKind() = when (this) {
    DeductionType.PER_DAY -> Kind.PER_DAY
    DeductionType.FIXED -> Kind.FIXED
    DeductionType.PERCENT -> Kind.PERCENT
    DeductionType.ONE_TIME, DeductionType.INSTALLMENTS -> Kind.ONCE
}

private fun suggestedName(cat: DeductionCategory) = when (cat) {
    DeductionCategory.TRANSPORTE -> "Vale-transporte"
    DeductionCategory.ALIMENTACAO -> "Refeição"
    DeductionCategory.ADIANTAMENTO -> "Adiantamento (vale)"
    DeductionCategory.UNIFORME -> "Uniforme"
    DeductionCategory.EMPRESTIMO -> "Empréstimo"
    DeductionCategory.OUTRO -> ""
}

private fun suggestedKind(cat: DeductionCategory) = when (cat) {
    DeductionCategory.TRANSPORTE, DeductionCategory.ALIMENTACAO -> Kind.PER_DAY
    DeductionCategory.ADIANTAMENTO, DeductionCategory.UNIFORME, DeductionCategory.EMPRESTIMO -> Kind.ONCE
    DeductionCategory.OUTRO -> Kind.FIXED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeductionEditScreen(vm: AppViewModel, ledger: Ledger, nav: NavController, id: String) {
    val c = J.colors
    val existing = remember(id) { ledger.data.deductions.firstOrNull { it.id == id } }
    val isNew = existing == null
    val noun = Fmt.periodNoun(ledger.settings.payPeriod)

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: DeductionCategory.OUTRO) }
    var kind by rememberSaveable { mutableStateOf(existing?.type?.toKind() ?: Kind.PER_DAY) }
    var installments by rememberSaveable { mutableIntStateOf(existing?.installments?.coerceAtLeast(1) ?: 1) }
    var amountText by rememberSaveable {
        mutableStateOf(
            existing?.let {
                when (it.type) {
                    DeductionType.INSTALLMENTS -> Fmt.moneyInput(it.amount * it.installments)
                    DeductionType.PERCENT -> Fmt.numberInput(it.amount)
                    else -> Fmt.moneyInput(it.amount)
                }
            } ?: "",
        )
    }
    var start by rememberSaveable { mutableStateOf(existing?.startDate ?: ledger.currentPayRange().start.toString()) }
    var active by rememberSaveable { mutableStateOf(existing?.active ?: true) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val amount = Fmt.parseMoney(amountText) ?: 0.0
    val parcels = if (kind == Kind.ONCE) installments else 1

    fun build() = Deduction(
        id = existing?.id ?: UUID.randomUUID().toString(),
        name = name.trim(),
        category = category,
        type = when (kind) {
            Kind.PER_DAY -> DeductionType.PER_DAY
            Kind.FIXED -> DeductionType.FIXED
            Kind.PERCENT -> DeductionType.PERCENT
            Kind.ONCE -> if (parcels > 1) DeductionType.INSTALLMENTS else DeductionType.ONE_TIME
        },
        amount = if (kind == Kind.ONCE && parcels > 1) amount / parcels else amount,
        startDate = start,
        installments = parcels,
        active = active,
    )

    Column(Modifier.fillMaxSize()) {
        TopBar(if (isNew) "Novo desconto" else "Editar desconto", { nav.popBackStack() }) {
            if (!isNew) IconCircleButton(JIcons.Trash, "Apagar desconto", { confirmDelete = true }, background = Color.Transparent, tint = c.redText)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Nome")
                JTextField(name, { name = it }, placeholder = "Ex.: Vale-transporte")
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel("Categoria")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeductionCategory.entries.forEach { cat ->
                        FilterPill(categoryLabel(cat), cat == category) {
                            if (name.isBlank() || name == suggestedName(category)) name = suggestedName(cat)
                            if (isNew) kind = suggestedKind(cat)
                            category = cat
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel("Como é descontado")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypeCard("Por dia trabalhado", "Valor × dias marcados", kind == Kind.PER_DAY, { kind = Kind.PER_DAY }, Modifier.weight(1f))
                    TypeCard("Fixo por período", "Toda semana, quinzena ou mês", kind == Kind.FIXED, { kind = Kind.FIXED }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypeCard("Percentual", "% sobre o bruto", kind == Kind.PERCENT, { kind = Kind.PERCENT }, Modifier.weight(1f))
                    TypeCard("Único ou parcelado", "Uma vez ou em N vezes", kind == Kind.ONCE, { kind = Kind.ONCE }, Modifier.weight(1f))
                }
            }

            if (kind == Kind.ONCE) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Parcelas")
                    Stepper(installments, { installments = it }, 1..48, if (installments == 1) "Uma vez só" else "$installments parcelas")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel(
                        when (kind) {
                            Kind.PER_DAY -> "Valor por dia"
                            Kind.FIXED -> "Valor por $noun"
                            Kind.PERCENT -> "Percentual"
                            Kind.ONCE -> if (installments > 1) "Valor total" else "Valor"
                        },
                    )
                    if (kind == Kind.PERCENT) {
                        JTextField(
                            amountText,
                            { t -> amountText = t.filter { it.isDigit() || it == ',' || it == '.' } },
                            keyboardType = KeyboardType.Decimal,
                            suffix = "%",
                            placeholder = "0",
                        )
                    } else {
                        MoneyField(amountText, { amountText = it })
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel(
                        when (kind) {
                            Kind.ONCE -> if (installments > 1) "1ª parcela em" else "Data"
                            else -> "Vale a partir de"
                        },
                    )
                    DateField(Fmt.ddmmyyyy(LocalDate.parse(start)), { pickDate = true })
                }
            }
            if (kind == Kind.ONCE && installments > 1 && amount > 0) {
                Helper("$installments × ${Fmt.money(amount / installments)}, uma parcela por $noun.")
            }

            Preview(ledger, build(), kind, amount)

            if (!isNew) {
                SwitchRow("Desconto ativo", "Desligue para pausar sem apagar.", active, { active = it })
            }
        }
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)) {
            PrimaryButton(
                "Salvar desconto",
                {
                    vm.upsertDeduction(build())
                    nav.popBackStack()
                },
                Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && amount > 0,
            )
        }
    }

    if (pickDate) {
        DatePickDialog(LocalDate.parse(start), { pickDate = false }, { start = it.toString() })
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            "Apagar desconto?",
            "\"${existing.name}\" sai de todos os períodos, inclusive dos anteriores.",
            "Apagar",
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                vm.deleteDeduction(existing.id)
                nav.popBackStack()
            },
        )
    }
}

@Composable
private fun Preview(ledger: Ledger, d: Deduction, kind: Kind, amount: Double) {
    val c = J.colors
    val range = ledger.currentPayRange()
    val sum = remember(ledger) { ledger.summary(range) }
    val presence = remember(ledger) { ledger.records(range).filter { it.status.isPresence }.map { it.localDate } }
    val line = ledger.deductionLine(d, range, sum.gross, presence)
    val start = LocalDate.parse(d.startDate)
    val headline = when (kind) {
        Kind.PER_DAY -> {
            val n = presence.count { !it.isBefore(start) }
            "${Fmt.days(n)} × ${Fmt.money(amount)} = ${Fmt.money(line.amount)}"
        }
        else -> Fmt.money(line.amount)
    }
    val sub = when (kind) {
        Kind.PER_DAY -> "Atualiza sozinho quando você marca mais dias."
        Kind.FIXED -> "Mesmo valor a cada ${Fmt.periodNoun(ledger.settings.payPeriod)}."
        Kind.PERCENT -> "${Fmt.percent(amount)} de ${Fmt.money(sum.gross)} de bruto até agora."
        Kind.ONCE -> line.detail
    }
    JCard(color = c.redSoft, spacing = 4.dp) {
        Text(
            "PRÉVIA · ${Fmt.rangeLabel(ledger.settings.payPeriod.toView(), range).uppercase(Fmt.locale)}",
            style = T.label,
            color = c.redText,
        )
        Text(headline, style = T.h2.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = c.ink)
        Text(sub, style = T.small, color = c.ink)
    }
}
