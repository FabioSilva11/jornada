package app.jornada.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.jornada.domain.Ledger
import app.jornada.notify.Notifications
import app.jornada.ui.screens.CalendarScreen
import app.jornada.ui.screens.DaySheet
import app.jornada.ui.screens.DaysSettingsScreen
import app.jornada.ui.screens.DeductionEditScreen
import app.jornada.ui.screens.HolidaysScreen
import app.jornada.ui.screens.HomeScreen
import app.jornada.ui.screens.MetricsScreen
import app.jornada.ui.screens.MoneyScreen
import app.jornada.ui.screens.OnboardingScreen
import app.jornada.ui.screens.PaySettingsScreen
import app.jornada.ui.screens.SettingsScreen
import app.jornada.ui.screens.VacationPlanScreen
import app.jornada.ui.screens.VacationScreen
import app.jornada.ui.screens.VacationSettingsScreen
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.JornadaTheme
import app.jornada.ui.theme.T

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val MONEY = "money"
    const val VACATION = "vacation"
    const val SETTINGS = "settings"
    const val METRICS = "metrics"
    const val DEDUCTION = "deduction"
    const val VACATION_PLAN = "vacation_plan"
    const val SET_PAY = "settings_pay"
    const val SET_DAYS = "settings_days"
    const val SET_HOLIDAYS = "settings_holidays"
    const val SET_VACATION = "settings_vacation"

    fun deduction(id: String) = "$DEDUCTION/$id"
    fun money(tab: Int) = "$MONEY?tab=$tab"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Início", JIcons.Home),
    Tab(Routes.CALENDAR, "Calendário", JIcons.Calendar),
    Tab(Routes.MONEY, "Dinheiro", JIcons.Wallet),
    Tab(Routes.VACATION, "Férias", JIcons.Sun),
    Tab(Routes.SETTINGS, "Ajustes", JIcons.Gear),
)

/** Pede a permissão de notificação (Android 13+) quando o usuário liga um lembrete. */
val LocalRequestNotifications = staticCompositionLocalOf<() -> Unit> { {} }

fun NavController.navigateTop(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "Compartilhar"))
}

@Composable
fun JornadaRoot(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val ledger = remember(data, today) { Ledger(data, today) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    JornadaTheme(data.settings.themeMode) {
        val dark = J.colors.isDark
        val view = LocalView.current
        LaunchedEffect(dark) {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        CompositionLocalProvider(LocalRequestNotifications provides requestNotifications) {
            Surface(color = J.colors.ground, modifier = Modifier.fillMaxSize()) {
                if (!data.settings.onboarded) {
                    OnboardingScreen(vm, ledger)
                } else {
                    MainScaffold(vm, ledger)
                }
            }
            vm.editingDate?.let { date -> DaySheet(vm, ledger, date) }
        }
    }
}

@Composable
private fun MainScaffold(vm: AppViewModel, ledger: Ledger) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.HOME
    val tab = tabs.indexOfFirst { route == it.route || route.startsWith(it.route + "?") }

    Scaffold(
        containerColor = J.colors.ground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tab >= 0) BottomBar(tab) { i -> nav.navigateTop(tabs[i].route) }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(160)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(160)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(Routes.HOME) { HomeScreen(vm, ledger, nav, snackbar) }
            composable(Routes.CALENDAR) { CalendarScreen(vm, ledger) }
            composable(
                "${Routes.MONEY}?tab={tab}",
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 }),
            ) { e -> MoneyScreen(ledger, nav, e.arguments?.getInt("tab") ?: 0) }
            composable(Routes.VACATION) { VacationScreen(vm, ledger, nav) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, ledger, nav, snackbar) }
            composable(Routes.METRICS) { MetricsScreen(ledger, nav) }
            composable("${Routes.DEDUCTION}/{id}") { e ->
                DeductionEditScreen(vm, ledger, nav, e.arguments?.getString("id") ?: "new")
            }
            composable(Routes.VACATION_PLAN) { VacationPlanScreen(vm, ledger, nav) }
            composable(Routes.SET_PAY) { PaySettingsScreen(vm, ledger, nav) }
            composable(Routes.SET_DAYS) { DaysSettingsScreen(vm, ledger, nav) }
            composable(Routes.SET_HOLIDAYS) { HolidaysScreen(vm, ledger, nav) }
            composable(Routes.SET_VACATION) { VacationSettingsScreen(vm, ledger, nav) }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val c = J.colors
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    icon = { Icon(t.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                    label = { Text(t.label, style = T.tiny) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = c.primary,
                        selectedTextColor = c.ink,
                        indicatorColor = c.primarySoft,
                        unselectedIconColor = c.muted,
                        unselectedTextColor = c.muted,
                    ),
                )
            }
        }
    }
}
