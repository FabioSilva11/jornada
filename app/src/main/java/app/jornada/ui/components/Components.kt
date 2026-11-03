package app.jornada.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jornada.data.DayStatus
import app.jornada.domain.DateRange
import app.jornada.domain.Fmt
import app.jornada.ui.theme.J
import app.jornada.ui.theme.JColors
import app.jornada.ui.theme.JIcons
import app.jornada.ui.theme.T

// ---------- Superfícies ----------

@Composable
fun JCard(
    modifier: Modifier = Modifier,
    color: Color = J.colors.surface,
    shape: Shape = RoundedCornerShape(20.dp),
    padding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier.fillMaxWidth().clip(shape).background(color)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = T.screenTitle, color = J.colors.ink)
            if (subtitle != null) Text(subtitle, style = T.small.copy(fontWeight = FontWeight.SemiBold), color = J.colors.muted)
        }
        action?.invoke()
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircleButton(JIcons.Back, "Voltar", onBack, background = Color.Transparent)
        Spacer(Modifier.width(4.dp))
        Text(title, style = T.barTitle, color = J.colors.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    background: Color = J.colors.surface,
    tint: Color = J.colors.ink,
    size: Dp = 44.dp,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(22.dp), tint = tint)
    }
}

@Composable
fun IconBadge(icon: ImageVector, background: Color, tint: Color, size: Dp = 42.dp, iconSize: Dp = 22.dp, radius: Dp = 13.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(iconSize), tint = tint)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(Fmt.locale),
        style = T.label,
        color = J.colors.muted,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Composable
fun LineDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(J.colors.line))
}

// ---------- Botões ----------

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = J.colors.primary,
    content: Color = J.colors.onPrimary,
    height: Dp = 56.dp,
) {
    val bg = if (enabled) container else J.colors.border
    Row(
        modifier.height(height).clip(RoundedCornerShape(16.dp)).background(bg)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = content)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = T.button, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 52.dp, icon: ImageVector? = null) {
    PrimaryButton(text, onClick, modifier, icon = icon, container = J.colors.primarySoft, content = J.colors.onPrimarySoft, height = height)
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    color: Color = J.colors.ink,
    borderColor: Color = J.colors.border,
    icon: ImageVector? = null,
) {
    Row(
        modifier.height(height).clip(RoundedCornerShape(14.dp)).border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = color)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = T.button, color = color, maxLines = 1)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, color: Color = J.colors.primary) {
    Text(
        text,
        style = T.bodyStrong,
        color = color,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

// ---------- Seleção ----------

/** Controle segmentado (abas em pílula). */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, height: Dp = 40.dp) {
    val c = J.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (c.isDark) c.surface2 else Color(0xFFE6ECE9)).padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).height(height).clip(RoundedCornerShape(11.dp))
                    .background(if (sel) c.surface else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = T.small.copy(fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold, fontSize = 14.sp),
                    color = if (sel) c.ink else c.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Fileira de botões de escolha única (selecionado em verde). */
@Composable
fun ChoiceRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, height: Dp = 48.dp) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            ChoiceButton(label, i == selected, { onSelect(i) }, Modifier.weight(1f), height)
        }
    }
}

@Composable
fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 48.dp, icon: ImageVector? = null) {
    val c = J.colors
    val shape = RoundedCornerShape(14.dp)
    var m = modifier.height(height).clip(shape)
    m = if (selected) m.background(c.primary) else m.background(c.surface).border(1.5.dp, c.border, shape)
    Row(
        m.clickable(onClick = onClick).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null && selected) {
            Icon(icon, null, Modifier.size(16.dp), tint = c.onPrimary)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            style = T.small.copy(fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) c.onPrimary else c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Pílula de filtro (selecionada em tinta escura). */
@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = J.colors
    val shape = RoundedCornerShape(18.dp)
    var m = Modifier.height(36.dp).clip(shape)
    m = if (selected) m.background(c.ink) else m.background(c.surface).border(1.5.dp, c.border, shape)
    Box(m.clickable(onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            style = T.small.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) c.ground else c.ink,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PillGroup(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label -> FilterPill(label, i == selected) { onSelect(i) } }
    }
}

/** Cartão de opção com indicador de rádio. */
@Composable
fun OptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val c = J.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(c.surface)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) c.primary else c.line, shape)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            IconBadge(icon, if (selected) c.primarySoft else c.surface2, if (selected) c.primary else c.muted, size = 44.dp, radius = 14.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = T.h2, color = c.ink)
            Text(subtitle, style = T.small, color = c.muted)
        }
        RadioDot(selected)
    }
}

@Composable
fun RadioDot(selected: Boolean) {
    val c = J.colors
    Box(
        Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) c.primary else c.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.primary))
    }
}

/** Cartão de opção compacto (grade 2×2). */
@Composable
fun TypeCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = J.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(c.surface)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) c.primary else c.line, shape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = T.small.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = c.ink)
        Text(subtitle, style = T.tiny.copy(fontWeight = FontWeight.Medium), color = c.muted)
    }
}

/** Escolha dos dias da semana (1 = segunda … 7 = domingo). */
@Composable
fun WeekdayPicker(selected: Set<Int>, onToggle: (Int) -> Unit, sundayFirst: Boolean, modifier: Modifier = Modifier) {
    val order = if (sundayFirst) listOf(7, 1, 2, 3, 4, 5, 6) else listOf(1, 2, 3, 4, 5, 6, 7)
    val c = J.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (d in order) {
            val sel = d in selected
            val shape = RoundedCornerShape(12.dp)
            var m = Modifier.weight(1f).height(44.dp).clip(shape)
            m = if (sel) m.background(c.primary) else m.background(c.surface).border(1.5.dp, c.border, shape)
            Box(m.clickable { onToggle(d) }, contentAlignment = Alignment.Center) {
                Text(Fmt.weekdayShort[d - 1], style = T.tiny, color = if (sel) c.onPrimary else c.ink)
            }
        }
    }
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, range: IntRange, label: String, modifier: Modifier = Modifier) {
    val c = J.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.5.dp, c.border, RoundedCornerShape(14.dp))
            .background(c.surface).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircleButton(JIcons.ChevronLeft, "Diminuir", { if (value > range.first) onChange(value - 1) }, background = Color.Transparent)
        Text(label, style = T.bodyStrong, color = c.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconCircleButton(JIcons.ChevronRight, "Aumentar", { if (value < range.last) onChange(value + 1) }, background = Color.Transparent)
    }
}

// ---------- Linhas de ajustes ----------

@Composable
fun SettingRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    titleColor: Color = J.colors.ink,
    trailing: (@Composable () -> Unit)? = { Icon(JIcons.ChevronRight, null, Modifier.size(18.dp), tint = J.colors.muted) },
) {
    var m = Modifier.fillMaxWidth()
    if (onClick != null) m = m.clickable(onClick = onClick)
    Row(
        m.padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconBadge(icon, iconBg, iconTint, size = 36.dp, iconSize = 20.dp, radius = 11.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = T.h3, color = titleColor)
            if (subtitle != null) Text(subtitle, style = T.small, color = J.colors.muted)
        }
        trailing?.invoke()
    }
}

@Composable
fun JSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = J.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onPrimary,
            checkedTrackColor = c.primary,
            checkedBorderColor = c.primary,
            uncheckedThumbColor = c.muted,
            uncheckedTrackColor = c.surface,
            uncheckedBorderColor = c.border,
        ),
    )
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = T.h3, color = J.colors.ink)
            if (subtitle != null) Text(subtitle, style = T.tiny.copy(fontWeight = FontWeight.Medium), color = J.colors.muted)
        }
        JSwitch(checked, onCheckedChange)
    }
}

// ---------- Campos ----------

@Composable
fun FieldLabel(text: String) {
    Text(text, style = T.small.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = J.colors.ink)
}

@Composable
fun JTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    suffix: String? = null,
    textStyle: TextStyle = T.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    singleLine: Boolean = true,
) {
    val c = J.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = c.ink),
        placeholder = { Text(placeholder, style = textStyle, color = c.muted) },
        prefix = prefix?.let { { Text(it, style = textStyle, color = c.muted) } },
        suffix = suffix?.let { { Text(it, style = textStyle, color = c.muted) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.border,
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface,
            cursorColor = c.primary,
        ),
    )
}

@Composable
fun MoneyField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "0,00") {
    JTextField(
        value = value,
        onValueChange = { t -> onValueChange(t.filter { it.isDigit() || it == ',' || it == '.' }) },
        modifier = modifier,
        placeholder = placeholder,
        keyboardType = KeyboardType.Decimal,
        prefix = "R$ ",
        textStyle = T.body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    )
}

/** Campo de data que abre o seletor ao tocar. */
@Composable
fun DateField(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = J.colors
    Row(
        modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, c.border, RoundedCornerShape(14.dp)).background(c.surface)
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = T.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = c.ink, modifier = Modifier.weight(1f))
        Icon(JIcons.Calendar, null, Modifier.size(20.dp), tint = c.muted)
    }
}

// ---------- Navegação de período ----------

@Composable
fun PeriodNav(label: String, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconCircleButton(JIcons.ChevronLeft, "Período anterior", onPrev, background = Color.Transparent)
        Text(label, style = T.h2.copy(fontSize = 17.sp), color = J.colors.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconCircleButton(JIcons.ChevronRight, "Próximo período", onNext, background = Color.Transparent)
    }
}

// ---------- Dados ----------

@Composable
fun ProgressBar(fraction: Float, color: Color, track: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(height / 2)).background(color))
    }
}

@Composable
fun ValueRow(label: String, value: String, valueColor: Color = J.colors.ink, strong: Boolean = false, sub: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = if (strong) T.bodyStrong else T.body, color = if (strong) J.colors.ink else J.colors.muted)
            if (sub != null) Text(sub, style = T.tiny.copy(fontWeight = FontWeight.Medium), color = J.colors.muted)
        }
        Text(value, style = if (strong) T.bodyStrong.copy(fontWeight = FontWeight.ExtraBold) else T.bodyStrong, color = valueColor)
    }
}

@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(JIcons.Info, null, Modifier.size(18.dp).padding(top = 1.dp), tint = J.colors.muted)
        Text(text, style = T.small, color = J.colors.muted)
    }
}

// ---------- Células de dia ----------

@Immutable
data class CellStyle(
    val bg: Color,
    val fg: Color,
    val border: Color? = null,
    val dashed: Boolean = false,
    val bold: Boolean = true,
)

fun cellStyle(c: JColors, status: DayStatus?, holiday: Boolean, scheduled: Boolean): CellStyle = when (status) {
    DayStatus.WORKED -> CellStyle(c.primary, c.onPrimary)
    DayStatus.HALF -> CellStyle(c.primarySoft, c.onPrimarySoft, c.primary, dashed = true)
    DayStatus.ABSENT -> CellStyle(c.redSoft, c.redText, c.redBorder)
    DayStatus.SICK -> CellStyle(c.blueSoft, c.blueText)
    DayStatus.VACATION -> CellStyle(c.amberSoft, c.amberText)
    DayStatus.HOLIDAY -> CellStyle(c.purpleSoft, c.purpleText)
    DayStatus.OFF -> CellStyle(Color.Transparent, c.muted, bold = false)
    null -> when {
        holiday -> CellStyle(c.purpleSoft, c.purpleText)
        scheduled -> CellStyle(Color.Transparent, c.ink, bold = false)
        else -> CellStyle(Color.Transparent, c.muted, bold = false)
    }
}

fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 2.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCell(
    label: String,
    style: CellStyle,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    radius: Dp = 12.dp,
    today: Boolean = false,
    selected: Boolean = false,
    fontSize: Int = 15,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val c = J.colors
    val shape = RoundedCornerShape(radius)
    var m = modifier.size(size).clip(shape).background(style.bg)
    if (style.border != null) {
        m = if (style.dashed) m.dashedBorder(style.border, radius) else m.border(2.dp, style.border, shape)
    }
    if (today) m = m.border(BorderStroke(2.dp, c.ink), shape)
    if (selected) m = m.border(BorderStroke(3.dp, c.amber), shape)
    if (onClick != null || onLongClick != null) {
        m = m.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick)
    }
    Box(m, contentAlignment = Alignment.Center) {
        Text(
            label,
            style = T.body.copy(fontSize = fontSize.sp, fontWeight = if (style.bold || today) FontWeight.Bold else FontWeight.Medium),
            color = style.fg,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusLegend(modifier: Modifier = Modifier) {
    val c = J.colors
    val items = listOf(
        DayStatus.WORKED, DayStatus.HALF, DayStatus.ABSENT, DayStatus.SICK, DayStatus.HOLIDAY, DayStatus.VACATION,
    )
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (s in items) {
            val st = cellStyle(c, s, false, true)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                var m = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(st.bg)
                if (st.border != null) m = if (st.dashed) m.dashedBorder(st.border, 4.dp, 1.5.dp) else m.border(1.5.dp, st.border, RoundedCornerShape(4.dp))
                Box(m)
                Text(Fmt.status(s), style = T.tiny, color = c.muted)
            }
        }
    }
}

fun rangeContainsToday(r: DateRange, today: java.time.LocalDate) = today in r
