package app.jornada.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.jornada.data.Employment
import app.jornada.data.PayBasis
import app.jornada.data.Settings
import app.jornada.data.ThemeMode
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.ui.AppViewModel
import app.jornada.ui.LocalRequestNotifications
import app.jornada.ui.Routes
import app.jornada.ui.components.ConfirmDialog
import app.jornada.ui.components.JCard
import app.jornada.ui.components.JSwitch
import app.jornada.ui.components.LineDivider
import app.jornada.ui.components.OptionsDialog
import app.jornada.ui.components.ScreenHeader
import app.jornada.ui.components.SectionLabel
import app.jornada.ui.components.SettingRow
import app.jornada.ui.components.TimePickDialog
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/** "Seg a Sex" para dias seguidos, "Seg, Qua, Sex" para dias soltos. */
fun workdaysLabel(days: List<Int>): String {
    val sorted = days.sorted()
    if (sorted.isEmpty()) return "Nenhum dia"
    if (sorted.size == 7) return "Todos os dias"
    val consecutive = sorted.zipWithNext().all { (a, b) -> b == a + 1 }
    return if (consecutive && sorted.size >= 3) {
        "${Fmt.weekdayShort[sorted.first() - 1]} a ${Fmt.weekdayShort[sorted.last() - 1]}"
    } else {
        sorted.joinToString(", ") { Fmt.weekdayShort[it - 1] }
    }
}

fun payValueLabel(s: Settings): String = when (s.payBasis) {
    PayBasis.DAILY -> "${Fmt.money(s.dailyRate)} por dia"
    PayBasis.MONTHLY -> "${Fmt.money(s.monthlySalary)} por mês"
    PayBasis.HOURLY -> "${Fmt.money(s.hourlyRate)} por hora"
}

@Composable
fun SettingsScreen(vm: AppViewModel, ledger: Ledger, nav: NavController, snackbar: SnackbarHostState) {
    val c = J.colors
    val s = ledger.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val requestNotifications = LocalRequestNotifications.current
    var dialog by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) } ?: error("sem saída")
            }.isSuccess
            if (ok) vm.markBackup()
            toast(if (ok) "Backup salvo" else "Não foi possível salvar o backup")
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()
            if (text == null) {
                toast("Não foi possível ler o arquivo")
            } else {
                pendingRestore = text
                dialog = "restore"
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.csv().toByteArray()) } ?: error("sem saída")
            }.isSuccess
            toast(if (ok) "Planilha exportada" else "Não foi possível exportar")
        }
    }

    val admission = ledger.admission()
    val activeDeductions = ledger.data.deductions.count { it.active }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Ajustes")
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            JCard(color = c.primary, onClick = { nav.navigate(Routes.SET_PAY) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(c.heroMuted.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                        Icon(JIcons.User, null, Modifier.size(24.dp), tint = c.onPrimary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Meu trabalho", style = T.h2, color = c.onPrimary)
                        val basis = when (s.payBasis) {
                            PayBasis.DAILY -> "Diária"
                            PayBasis.MONTHLY -> "Salário"
                            PayBasis.HOURLY -> "Por hora"
                        }
                        val emp = if (s.employment == Employment.CLT) "CLT" else "autônomo"
                        Text(
                            "$basis $emp · ${workdaysLabel(s.workDays)}" + (admission?.let { " · desde ${Fmt.ddmmyyyy(it)}" } ?: ""),
                            style = T.small,
                            color = if (c.isDark) c.onPrimary else c.heroMuted,
                        )
                    }
                    Icon(JIcons.ChevronRight, null, Modifier.size(20.dp), tint = c.onPrimary)
                }
            }

            Section("Trabalho e ganhos") {
                SettingRow(
                    JIcons.Wallet, c.primarySoft, c.primary, "Ganhos e pagamento",
                    "${payValueLabel(s)} · ${Fmt.periodAdjective(s.payPeriod)}, ${Fmt.paydayRule(s.payPeriod, s.payDay1, s.payDay2, s.payWeekday)}",
                    { nav.navigate(Routes.SET_PAY) },
                )
                LineDivider()
                SettingRow(
                    JIcons.Clock, c.primarySoft, c.primary, "Dias e jornada",
                    "${workdaysLabel(s.workDays)} · ${Fmt.numberInput(s.hoursPerDay)} h por dia",
                    { nav.navigate(Routes.SET_DAYS) },
                )
                LineDivider()
                SettingRow(
                    JIcons.Flag, c.purpleSoft, c.purpleText, "Feriados",
                    buildString {
                        append(if (s.nationalHolidays) "Nacionais" else "Sem os nacionais")
                        if (s.optionalHolidays) append(" + Carnaval")
                        if (s.customHolidays.isNotEmpty()) append(" + ${s.customHolidays.size} seus")
                    },
                    { nav.navigate(Routes.SET_HOLIDAYS) },
                )
                LineDivider()
                SettingRow(
                    JIcons.Sun, c.amberSoft, c.amberText, "Regra de férias",
                    if (s.employment == Employment.CLT) {
                        "CLT · " + (admission?.let { "admissão em ${Fmt.ddmmyyyy(it)}" } ?: "informe a admissão")
                    } else {
                        "Autônomo · reserva de 11,1%"
                    },
                    { nav.navigate(Routes.SET_VACATION) },
                )
                LineDivider()
                SettingRow(
                    JIcons.Receipt, c.redSoft, c.redText, "Descontos recorrentes",
                    when (activeDeductions) {
                        0 -> "Nenhum ativo"
                        1 -> "1 ativo"
                        else -> "$activeDeductions ativos"
                    },
                    {
                        nav.navigate(Routes.money(1)) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            Section("Lembretes") {
                SettingRow(
                    JIcons.Bell, c.surface2, c.ink, "Lembrar de marcar o dia",
                    "Dias de trabalho às ${Fmt.time(s.reminderMinutes)}",
                    { dialog = "reminder" },
                    trailing = {
                        JSwitch(s.dailyReminder) { on ->
                            vm.updateSettings { it.copy(dailyReminder = on) }
                            if (on) requestNotifications()
                        }
                    },
                )
                LineDivider()
                SettingRow(
                    JIcons.Calendar, c.surface2, c.ink, "Fechamento da semana",
                    "${Fmt.weekdayNames[s.summaryWeekday - 1]} às ${Fmt.time(s.summaryMinutes)}",
                    { dialog = "summary" },
                    trailing = {
                        JSwitch(s.weeklySummary) { on ->
                            vm.updateSettings { it.copy(weeklySummary = on) }
                            if (on) requestNotifications()
                        }
                    },
                )
                LineDivider()
                SettingRow(
                    JIcons.Sun, c.surface2, c.ink, "Aviso de prazo das férias",
                    "60, 30 e 7 dias antes do limite",
                    null,
                    trailing = {
                        JSwitch(s.vacationAlert) { on ->
                            vm.updateSettings { it.copy(vacationAlert = on) }
                            if (on) requestNotifications()
                        }
                    },
                )
            }

            Section("Aparência") {
                SettingRow(
                    JIcons.Moon, c.surface2, c.ink, "Tema",
                    when (s.themeMode) {
                        ThemeMode.SYSTEM -> "Automático (segue o celular)"
                        ThemeMode.LIGHT -> "Claro"
                        ThemeMode.DARK -> "Escuro"
                    },
                    { dialog = "theme" },
                )
                LineDivider()
                SettingRow(
                    JIcons.Calendar, c.surface2, c.ink, "Semana começa no",
                    if (s.weekStartsSunday) "Domingo" else "Segunda",
                    { dialog = "week" },
                )
            }

            Section("Seus dados") {
                SettingRow(
                    JIcons.Download, c.surface2, c.ink, "Fazer backup agora",
                    s.lastBackup?.let { "Último em ${Fmt.ddmmyyyy(LocalDate.parse(it))}" } ?: "Nenhum backup ainda",
                    { backupLauncher.launch("jornada-backup-${LocalDate.now()}.json") },
                    trailing = null,
                )
                LineDivider()
                SettingRow(
                    JIcons.Upload, c.surface2, c.ink, "Restaurar backup", "De um arquivo salvo",
                    { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    trailing = null,
                )
                LineDivider()
                SettingRow(
                    JIcons.Table, c.surface2, c.ink, "Exportar planilha", "Arquivo CSV com dias e valores",
                    { csvLauncher.launch("jornada-dias-${LocalDate.now()}.csv") },
                    trailing = null,
                )
                LineDivider()
                SettingRow(
                    JIcons.Trash, c.redSoft, c.redText, "Apagar todos os dados", "Pede confirmação antes",
                    { dialog = "wipe" },
                    titleColor = c.redText,
                    trailing = null,
                )
            }

            Text(
                "Jornada 1.0 · seus dados ficam só neste celular",
                style = T.small,
                color = c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    when (dialog) {
        "theme" -> OptionsDialog(
            "Tema",
            listOf("Automático (segue o celular)", "Claro", "Escuro"),
            s.themeMode.ordinal,
            { dialog = null },
            { i -> vm.updateSettings { it.copy(themeMode = ThemeMode.entries[i]) } },
        )
        "week" -> OptionsDialog(
            "Semana começa no",
            listOf("Domingo", "Segunda"),
            if (s.weekStartsSunday) 0 else 1,
            { dialog = null },
            { i -> vm.updateSettings { it.copy(weekStartsSunday = i == 0) } },
        )
        "reminder" -> TimePickDialog("Horário do lembrete", s.reminderMinutes, null, { dialog = null }) { m, _ ->
            vm.updateSettings { it.copy(reminderMinutes = m) }
        }
        "summary" -> TimePickDialog("Fechamento da semana", s.summaryMinutes, s.summaryWeekday, { dialog = null }) { m, wd ->
            vm.updateSettings { it.copy(summaryMinutes = m, summaryWeekday = wd ?: it.summaryWeekday) }
        }
        "restore" -> ConfirmDialog(
            "Restaurar backup?",
            "Os dados atuais deste celular serão substituídos pelos do arquivo.",
            "Restaurar",
            destructive = true,
            onDismiss = {
                dialog = null
                pendingRestore = null
            },
            onConfirm = {
                val ok = pendingRestore?.let { vm.importJson(it) } ?: false
                toast(if (ok) "Backup restaurado" else "Esse arquivo não é um backup do Jornada")
            },
        )
        "wipe" -> ConfirmDialog(
            "Apagar todos os dados?",
            "Dias marcados, descontos, férias e ajustes serão apagados deste celular. Isso não pode ser desfeito. Se quiser guardar, faça um backup antes.",
            "Apagar tudo",
            destructive = true,
            onDismiss = { dialog = null },
            onConfirm = { vm.wipe() },
        )
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        JCard(padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp), content = content)
    }
}
