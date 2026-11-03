package app.jornada.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jornada.data.Employment
import app.jornada.data.PayBasis
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.ui.AppViewModel
import app.jornada.ui.LocalRequestNotifications
import app.jornada.ui.components.ChoiceRow
import app.jornada.ui.components.DateField
import app.jornada.ui.components.DatePickDialog
import app.jornada.ui.components.FieldLabel
import app.jornada.ui.components.IconBadge
import app.jornada.ui.components.IconCircleButton
import app.jornada.ui.components.OptionCard
import app.jornada.ui.components.PrimaryButton
import app.jornada.ui.components.WeekdayPicker
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import java.time.LocalDate

@Composable
fun OnboardingScreen(vm: AppViewModel, ledger: Ledger) {
    val c = J.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    var draft by rememberSaveable(stateSaver = SettingsSaver) { mutableStateOf(ledger.settings) }
    var pickAdmission by rememberSaveable { mutableStateOf(false) }
    val requestNotifications = LocalRequestNotifications.current

    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        if (step > 0) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconCircleButton(JIcons.Back, "Voltar", { step-- })
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0..2) {
                        Box(
                            Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                                .background(if (i <= step) c.primary else c.border),
                        )
                    }
                }
                Text("${step + 1} de 3", style = T.smallStrong, color = c.muted)
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when (step) {
                0 -> Welcome()
                1 -> {
                    Title("Como você recebe?", "Assim o app calcula quanto cada dia rende e quando fechar as contas. Dá para mudar depois em Ajustes.")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OptionCard("Por dia (diária)", "Um valor por dia trabalhado", draft.payBasis == PayBasis.DAILY, {
                            draft = draft.copy(payBasis = PayBasis.DAILY)
                        }, icon = JIcons.Calendar)
                        OptionCard("Salário mensal", "Valor fixo, dividido pelos dias do mês", draft.payBasis == PayBasis.MONTHLY, {
                            draft = draft.copy(payBasis = PayBasis.MONTHLY)
                        }, icon = JIcons.Wallet)
                        OptionCard("Por hora", "Valor da hora × horas do dia", draft.payBasis == PayBasis.HOURLY, {
                            draft = draft.copy(payBasis = PayBasis.HOURLY)
                        }, icon = JIcons.Clock)
                    }
                    PayValueFields(draft) { draft = it }
                    PayPeriodFields(draft) { draft = it }
                }
                else -> {
                    Title("Seus dias de trabalho", "Marque os dias da semana em que você costuma trabalhar.")
                    WeekdayPicker(draft.workDays.toSet(), { d ->
                        val set = draft.workDays.toMutableSet()
                        if (!set.add(d)) set.remove(d)
                        draft = draft.copy(workDays = set.sorted())
                    }, draft.weekStartsSunday)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FieldLabel("Tipo de vínculo")
                        ChoiceRow(
                            listOf("CLT (carteira)", "Autônomo"),
                            if (draft.employment == Employment.CLT) 0 else 1,
                            { draft = draft.copy(employment = if (it == 0) Employment.CLT else Employment.AUTONOMO) },
                        )
                        Helper(
                            if (draft.employment == Employment.CLT) "CLT usa a regra da lei para as férias: 30 dias a cada 12 meses, mais 1/3."
                            else "Autônomo: o app sugere quanto guardar de cada dia para tirar férias sem perder renda.",
                        )
                    }
                    if (draft.employment == Employment.CLT) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FieldLabel("Data de admissão")
                            DateField(
                                draft.admissionDate?.let { Fmt.ddmmyyyy(LocalDate.parse(it)) } ?: "Toque para escolher",
                                { pickAdmission = true },
                            )
                            Helper("Usada para contar o período de férias. Pode informar depois.")
                        }
                    }
                }
            }
        }

        Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 4.dp)) {
            when (step) {
                0 -> PrimaryButton("Começar", { step = 1 }, Modifier.fillMaxWidth(), icon = JIcons.Forward)
                1 -> PrimaryButton("Continuar", { step = 2 }, Modifier.fillMaxWidth(), enabled = draft.payValueOk())
                else -> PrimaryButton(
                    "Começar a usar",
                    {
                        val done = draft.copy(onboarded = true)
                        vm.updateSettings { done }
                        requestNotifications()
                    },
                    Modifier.fillMaxWidth(),
                    enabled = draft.workDays.isNotEmpty(),
                )
            }
        }
    }

    if (pickAdmission) {
        DatePickDialog(
            draft.admissionDate?.let { LocalDate.parse(it) },
            onDismiss = { pickAdmission = false },
            onPick = { draft = draft.copy(admissionDate = it.toString()) },
        )
    }
}

@Composable
private fun Title(title: String, sub: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = T.screenTitle.copy(fontSize = 30.sp), color = J.colors.ink)
        Text(sub, style = T.body, color = J.colors.muted)
    }
}

@Composable
private fun Welcome() {
    val c = J.colors
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(104.dp).clip(RoundedCornerShape(30.dp)).background(c.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(JIcons.CalendarCheck, null, Modifier.size(58.dp), tint = c.onPrimary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Jornada", style = T.display.copy(fontSize = 52.sp), color = c.ink)
            Text("Seus dias, seu dinheiro, suas férias.", style = T.title.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Feature(JIcons.Check, "Marque em um toque os dias em que foi trabalhar.")
            Feature(JIcons.Wallet, "Veja quanto rende cada dia, semana, quinzena e mês, já com os descontos.")
            Feature(JIcons.Sun, "Saiba quando e quanto serão as suas férias.")
        }
        Text("Seus dados ficam só neste celular. Sem conta e sem internet.", style = T.small, color = c.muted)
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBadge(icon, J.colors.primarySoft, J.colors.primary)
        Text(text, style = T.body, color = J.colors.ink)
    }
}
