package app.jornada.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.jornada.data.ThemeMode

/** Paleta da direção de design: verde para o que entra, âmbar para férias, vermelho para descontos. */
@Immutable
data class JColors(
    val isDark: Boolean,
    val ground: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val border: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val onPrimarySoft: Color,
    val primaryMid: Color,
    val heroMuted: Color,
    val amber: Color,
    val amberText: Color,
    val amberSoft: Color,
    val amberTrack: Color,
    val onAmberSoft: Color,
    val red: Color,
    val redText: Color,
    val redSoft: Color,
    val redBorder: Color,
    val purpleText: Color,
    val purpleSoft: Color,
    val purpleDot: Color,
    val blueText: Color,
    val blueSoft: Color,
    val night: Color,
    val nightCard: Color,
    val onNight: Color,
    val nightMuted: Color,
    val nightGreen: Color,
    val nightRed: Color,
    val nightAmber: Color,
    val deductionShades: List<Color>,
)

val LightColors = JColors(
    isDark = false,
    ground = Color(0xFFF4F6F5),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEEF2F0),
    ink = Color(0xFF14201C),
    muted = Color(0xFF5B6B66),
    line = Color(0xFFE3E8E6),
    border = Color(0xFFC9D3CF),
    primary = Color(0xFF0F6B57),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFD6EFE6),
    onPrimarySoft = Color(0xFF0B4F40),
    primaryMid = Color(0xFF7FC4AE),
    heroMuted = Color(0xFFCFEDE2),
    amber = Color(0xFFF2A93B),
    amberText = Color(0xFF8A4B06),
    amberSoft = Color(0xFFFDEBC8),
    amberTrack = Color(0xFFF3D49B),
    onAmberSoft = Color(0xFF4A2A05),
    red = Color(0xFFB4233A),
    redText = Color(0xFFA01E33),
    redSoft = Color(0xFFFBE3E6),
    redBorder = Color(0xFFE7A1AC),
    purpleText = Color(0xFF5B3BB0),
    purpleSoft = Color(0xFFECE6FA),
    purpleDot = Color(0xFFB9A4F5),
    blueText = Color(0xFF1F5590),
    blueSoft = Color(0xFFDDE9F7),
    night = Color(0xFF14201C),
    nightCard = Color(0xFF1F2C28),
    onNight = Color(0xFFFFFFFF),
    nightMuted = Color(0xFF9DB0A9),
    nightGreen = Color(0xFF5CCBAA),
    nightRed = Color(0xFFF08A99),
    nightAmber = Color(0xFFF5B95A),
    deductionShades = listOf(Color(0xFFB4233A), Color(0xFFD9596B), Color(0xFFEE9AA6), Color(0xFFF6C9CF), Color(0xFF8A1A2C)),
)

val DarkColors = JColors(
    isDark = true,
    ground = Color(0xFF0D1412),
    surface = Color(0xFF16201D),
    surface2 = Color(0xFF1E2A26),
    ink = Color(0xFFE7EFEC),
    muted = Color(0xFF9DB0A9),
    line = Color(0xFF2A3531),
    border = Color(0xFF3A4843),
    primary = Color(0xFF5CCBAA),
    onPrimary = Color(0xFF0D1412),
    primarySoft = Color(0xFF12443A),
    onPrimarySoft = Color(0xFFCFEDE2),
    primaryMid = Color(0xFF2E8A72),
    heroMuted = Color(0xFF12443A),
    amber = Color(0xFFF5B95A),
    amberText = Color(0xFFF5B95A),
    amberSoft = Color(0xFF3A2C12),
    amberTrack = Color(0xFF5A4520),
    onAmberSoft = Color(0xFFFBE3BC),
    red = Color(0xFFF08A99),
    redText = Color(0xFFF08A99),
    redSoft = Color(0xFF3A1B20),
    redBorder = Color(0xFF7A3440),
    purpleText = Color(0xFFB9A4F5),
    purpleSoft = Color(0xFF2A2346),
    purpleDot = Color(0xFFB9A4F5),
    blueText = Color(0xFF8DB8EE),
    blueSoft = Color(0xFF1B2A3D),
    night = Color(0xFF22302B),
    nightCard = Color(0xFF2C3B36),
    onNight = Color(0xFFFFFFFF),
    nightMuted = Color(0xFFA9BBB4),
    nightGreen = Color(0xFF5CCBAA),
    nightRed = Color(0xFFF08A99),
    nightAmber = Color(0xFFF5B95A),
    deductionShades = listOf(Color(0xFFF08A99), Color(0xFFD9596B), Color(0xFFB4233A), Color(0xFF8A1A2C), Color(0xFFF6C9CF)),
)

val LocalJColors = staticCompositionLocalOf { LightColors }

/** Acesso curto às cores do app: `J.colors.primary`. */
object J {
    val colors: JColors
        @Composable get() = LocalJColors.current
}

@Composable
fun JornadaTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primarySoft,
            onPrimaryContainer = c.onPrimarySoft,
            secondary = c.primary,
            onSecondary = c.onPrimary,
            secondaryContainer = c.primarySoft,
            onSecondaryContainer = c.onPrimarySoft,
            tertiary = c.amber,
            background = c.ground,
            onBackground = c.ink,
            surface = c.surface,
            onSurface = c.ink,
            surfaceVariant = c.surface2,
            onSurfaceVariant = c.muted,
            surfaceContainerLowest = c.surface,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.surface2,
            outline = c.border,
            outlineVariant = c.line,
            error = c.red,
        )
    } else {
        lightColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primarySoft,
            onPrimaryContainer = c.onPrimarySoft,
            secondary = c.primary,
            onSecondary = c.onPrimary,
            secondaryContainer = c.primarySoft,
            onSecondaryContainer = c.onPrimarySoft,
            tertiary = c.amber,
            background = c.ground,
            onBackground = c.ink,
            surface = c.surface,
            onSurface = c.ink,
            surfaceVariant = c.surface2,
            onSurfaceVariant = c.muted,
            surfaceContainerLowest = c.surface,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.surface2,
            outline = c.border,
            outlineVariant = c.line,
            error = c.red,
        )
    }
    CompositionLocalProvider(LocalJColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = JTypography, content = content)
    }
}
