package app.jornada.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.jornada.R

/** Plus Jakarta Sans, um arquivo por peso (a versão variável perde o peso quando carregada como recurso). */
val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

private fun s(size: Int, weight: FontWeight, line: Float = 1.3f) = TextStyle(
    fontFamily = Jakarta,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * line).sp,
    fontFeatureSettings = "tnum",
)

/** Escala usada pelas telas (números tabulares para os valores alinharem). */
object T {
    val display = s(40, FontWeight.ExtraBold, 1.1f).copy(letterSpacing = (-0.02).em)
    val big = s(32, FontWeight.ExtraBold, 1.15f).copy(letterSpacing = (-0.02).em)
    val screenTitle = s(26, FontWeight.ExtraBold, 1.2f)
    val barTitle = s(20, FontWeight.ExtraBold, 1.2f)
    val value = s(22, FontWeight.ExtraBold, 1.2f)
    val title = s(22, FontWeight.Bold, 1.25f)
    val h2 = s(16, FontWeight.Bold, 1.3f)
    val h3 = s(15, FontWeight.Bold, 1.3f)
    val body = s(15, FontWeight.Medium, 1.45f)
    val bodyStrong = s(15, FontWeight.Bold, 1.45f)
    val small = s(13, FontWeight.Medium, 1.45f)
    val smallStrong = s(13, FontWeight.Bold, 1.4f)
    val tiny = s(12, FontWeight.SemiBold, 1.35f)
    val label = s(12, FontWeight.Bold, 1.3f).copy(letterSpacing = 0.08.em)
    val button = s(15, FontWeight.Bold, 1.2f)
}

private val base = Typography()

val JTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Jakarta),
    displayMedium = base.displayMedium.copy(fontFamily = Jakarta),
    displaySmall = base.displaySmall.copy(fontFamily = Jakarta),
    headlineLarge = base.headlineLarge.copy(fontFamily = Jakarta),
    headlineMedium = base.headlineMedium.copy(fontFamily = Jakarta),
    headlineSmall = base.headlineSmall.copy(fontFamily = Jakarta),
    titleLarge = base.titleLarge.copy(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Jakarta),
    bodyMedium = base.bodyMedium.copy(fontFamily = Jakarta),
    bodySmall = base.bodySmall.copy(fontFamily = Jakarta),
    labelLarge = base.labelLarge.copy(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
)
