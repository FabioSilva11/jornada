package app.jornada.domain

import app.jornada.data.DayStatus
import app.jornada.data.PayPeriod
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period
import java.util.Locale
import kotlin.math.abs

/** Formatação em português do Brasil (R$, datas por extenso, períodos). */
object Fmt {
    val locale: Locale = Locale.forLanguageTag("pt-BR")

    val weekdayNames = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
    val weekdayShort = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val weekdayLetter = listOf("S", "T", "Q", "Q", "S", "S", "D")
    val months = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    val monthsShort = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

    fun money(v: Double): String {
        val x = if (abs(v) < 0.005) 0.0 else v
        if (x < 0) return "− " + NumberFormat.getCurrencyInstance(locale).format(-x)
        return NumberFormat.getCurrencyInstance(locale).format(x)
    }

    fun minus(v: Double): String = "− " + money(v)

    fun number(v: Double, decimals: Int = 0): String {
        val f = NumberFormat.getNumberInstance(locale)
        f.minimumFractionDigits = decimals
        f.maximumFractionDigits = decimals
        return f.format(v)
    }

    /** "3" para inteiros, "3,5" quando há meio dia. */
    fun compact(v: Double): String = if (v % 1.0 == 0.0) number(v, 0) else number(v, 1)

    fun percent(v: Double): String = compact(v) + "%"

    fun days(n: Int): String = if (n == 1) "1 dia" else "$n dias"

    fun daysDecimal(v: Double): String = if (v == 1.0) "1 dia" else "${compact(v)} dias"

    fun weekday(d: LocalDate): String = weekdayNames[d.dayOfWeek.value - 1]

    fun longDay(d: LocalDate): String = "${weekday(d)}, ${d.dayOfMonth} de ${months[d.monthValue - 1]}"

    fun dayMonth(d: LocalDate): String = "${d.dayOfMonth} de ${months[d.monthValue - 1]}"

    fun ddmm(d: LocalDate): String = String.format(locale, "%02d/%02d", d.dayOfMonth, d.monthValue)

    fun ddmmyyyy(d: LocalDate): String =
        String.format(locale, "%02d/%02d/%04d", d.dayOfMonth, d.monthValue, d.year)

    fun monthName(month: Int): String = months[month - 1].replaceFirstChar { it.uppercase() }

    fun monthYear(d: LocalDate): String = "${monthName(d.monthValue)} ${d.year}"

    fun monthShortCap(month: Int): String = monthsShort[month - 1].replaceFirstChar { it.uppercase() }

    /** "1 a 7 nov" ou "29 nov a 5 dez". */
    fun shortRange(r: DateRange): String =
        if (r.start.monthValue == r.end.monthValue) {
            "${r.start.dayOfMonth} a ${r.end.dayOfMonth} ${monthsShort[r.end.monthValue - 1]}"
        } else {
            "${r.start.dayOfMonth} ${monthsShort[r.start.monthValue - 1]} a ${r.end.dayOfMonth} ${monthsShort[r.end.monthValue - 1]}"
        }

    fun rangeLabel(view: ViewPeriod, r: DateRange): String = when (view) {
        ViewPeriod.WEEK -> shortRange(r)
        ViewPeriod.FORTNIGHT ->
            (if (r.start.dayOfMonth == 1) "1ª" else "2ª") + " quinzena · " + monthShortCap(r.start.monthValue) + " " + r.start.year
        ViewPeriod.MONTH -> monthYear(r.start)
        ViewPeriod.YEAR -> r.start.year.toString()
    }

    fun inPeriod(view: ViewPeriod): String = when (view) {
        ViewPeriod.WEEK -> "na semana"
        ViewPeriod.FORTNIGHT -> "na quinzena"
        ViewPeriod.MONTH -> "no mês"
        ViewPeriod.YEAR -> "no ano"
    }

    fun periodNoun(p: PayPeriod): String = when (p) {
        PayPeriod.WEEKLY -> "semana"
        PayPeriod.BIWEEKLY -> "quinzena"
        PayPeriod.MONTHLY -> "mês"
    }

    fun periodAdjective(p: PayPeriod): String = when (p) {
        PayPeriod.WEEKLY -> "semanal"
        PayPeriod.BIWEEKLY -> "quinzenal"
        PayPeriod.MONTHLY -> "mensal"
    }

    fun status(s: DayStatus): String = when (s) {
        DayStatus.WORKED -> "Trabalhado"
        DayStatus.HALF -> "Meio período"
        DayStatus.ABSENT -> "Falta"
        DayStatus.SICK -> "Atestado"
        DayStatus.OFF -> "Folga"
        DayStatus.HOLIDAY -> "Feriado"
        DayStatus.VACATION -> "Férias"
    }

    /** Regra do dia de pagamento: "todo dia 5", "dias 5 e 20", "toda sexta". */
    fun paydayRule(p: PayPeriod, day1: Int, day2: Int, weekday: Int): String = when (p) {
        PayPeriod.MONTHLY -> "todo dia $day1"
        PayPeriod.BIWEEKLY -> "dias $day1 e $day2"
        PayPeriod.WEEKLY -> (if (weekday >= 6) "todo " else "toda ") + weekdayNames[weekday - 1].lowercase()
    }

    fun time(minutes: Int): String = String.format(locale, "%02d:%02d", minutes / 60, minutes % 60)

    /** "3 meses e 19 dias" entre duas datas. */
    fun duration(from: LocalDate, to: LocalDate): String {
        if (!to.isAfter(from)) return "0 dias"
        val p = Period.between(from, to)
        val months = p.years * 12 + p.months
        val parts = mutableListOf<String>()
        if (months > 0) parts += if (months == 1) "1 mês" else "$months meses"
        if (p.days > 0) parts += days(p.days)
        return parts.joinToString(" e ")
    }

    /** Aceita "120", "120,50", "R$ 1.440,00" ou "120.5". */
    fun parseMoney(text: String): Double? {
        var t = text.replace("R$", "").replace(" ", "").replace(" ", "").trim()
        if (t.isEmpty()) return null
        if (t.contains(',')) t = t.replace(".", "").replace(',', '.')
        return t.toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }
    }

    fun moneyInput(v: Double): String = if (v == 0.0) "" else String.format(locale, "%.2f", v)

    fun numberInput(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else String.format(locale, "%.1f", v)
}
