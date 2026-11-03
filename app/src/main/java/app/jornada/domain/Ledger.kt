package app.jornada.domain

import app.jornada.data.AppData
import app.jornada.data.DayRecord
import app.jornada.data.DayStatus
import app.jornada.data.Deduction
import app.jornada.data.DeductionType
import app.jornada.data.Employment
import app.jornada.data.PayBasis
import app.jornada.data.PayPeriod
import app.jornada.data.isPresence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Intervalo de datas com início e fim incluídos. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    val length: Int get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()

    operator fun contains(d: LocalDate): Boolean = !d.isBefore(start) && !d.isAfter(end)

    fun dates(): List<LocalDate> = (0 until length).map { start.plusDays(it.toLong()) }

    fun intersect(other: DateRange): DateRange? {
        val s = if (start.isAfter(other.start)) start else other.start
        val e = if (end.isBefore(other.end)) end else other.end
        return if (s.isAfter(e)) null else DateRange(s, e)
    }

    /** Parte do intervalo até a data informada (inclusive), ou nulo se começar depois dela. */
    fun upTo(d: LocalDate): DateRange? = if (d.isBefore(start)) null else DateRange(start, if (d.isBefore(end)) d else end)
}

enum class ViewPeriod { WEEK, FORTNIGHT, MONTH, YEAR }

fun PayPeriod.toView(): ViewPeriod = when (this) {
    PayPeriod.WEEKLY -> ViewPeriod.WEEK
    PayPeriod.BIWEEKLY -> ViewPeriod.FORTNIGHT
    PayPeriod.MONTHLY -> ViewPeriod.MONTH
}

object Periods {
    fun week(d: LocalDate, sundayStart: Boolean): DateRange {
        val dow = d.dayOfWeek.value
        val back = if (sundayStart) dow % 7 else dow - 1
        val s = d.minusDays(back.toLong())
        return DateRange(s, s.plusDays(6))
    }

    /** Quinzenas do mês: do dia 1 ao 15 e do 16 ao último dia. */
    fun fortnight(d: LocalDate): DateRange =
        if (d.dayOfMonth <= 15) DateRange(d.withDayOfMonth(1), d.withDayOfMonth(15))
        else DateRange(d.withDayOfMonth(16), d.with(TemporalAdjusters.lastDayOfMonth()))

    fun month(d: LocalDate): DateRange = DateRange(d.withDayOfMonth(1), d.with(TemporalAdjusters.lastDayOfMonth()))

    fun year(d: LocalDate): DateRange = DateRange(d.withDayOfYear(1), d.with(TemporalAdjusters.lastDayOfYear()))

    fun of(view: ViewPeriod, d: LocalDate, sundayStart: Boolean): DateRange = when (view) {
        ViewPeriod.WEEK -> week(d, sundayStart)
        ViewPeriod.FORTNIGHT -> fortnight(d)
        ViewPeriod.MONTH -> month(d)
        ViewPeriod.YEAR -> year(d)
    }

    fun shift(view: ViewPeriod, r: DateRange, delta: Int, sundayStart: Boolean): DateRange = when (view) {
        ViewPeriod.WEEK -> week(r.start.plusWeeks(delta.toLong()), sundayStart)
        ViewPeriod.FORTNIGHT -> {
            var cur = r
            repeat(abs(delta)) { cur = fortnight(if (delta > 0) cur.end.plusDays(1) else cur.start.minusDays(1)) }
            cur
        }
        ViewPeriod.MONTH -> month(r.start.plusMonths(delta.toLong()))
        ViewPeriod.YEAR -> year(r.start.plusYears(delta.toLong()))
    }

    fun pay(p: PayPeriod, d: LocalDate, sundayStart: Boolean): DateRange = of(p.toView(), d, sundayStart)

    /** Subdivisões para listar dentro de um período (semanas do mês, dias da semana, meses do ano). */
    fun parts(view: ViewPeriod, r: DateRange, sundayStart: Boolean): List<DateRange> = when (view) {
        ViewPeriod.WEEK -> r.dates().map { DateRange(it, it) }
        ViewPeriod.FORTNIGHT, ViewPeriod.MONTH -> {
            val out = mutableListOf<DateRange>()
            var w = week(r.start, sundayStart)
            while (!w.start.isAfter(r.end)) {
                w.intersect(r)?.let { out += it }
                w = week(w.end.plusDays(1), sundayStart)
            }
            out
        }
        ViewPeriod.YEAR -> (1..12).map { month(LocalDate.of(r.start.year, it, 1)) }
    }
}

enum class EarnKind(val label: String) {
    REGULAR("Dias normais"),
    SATURDAY("Sábados"),
    SUNDAY("Domingos"),
    HOLIDAY("Feriados trabalhados"),
    HALF("Meio período"),
    SICK("Atestado pago"),
}

data class KindTotal(val count: Int, val value: Double, val unit: Double)

data class DeductionLine(val deduction: Deduction, val amount: Double, val detail: String)

data class Summary(
    val range: DateRange,
    val gross: Double,
    val byKind: Map<EarnKind, KindTotal>,
    val extras: Double,
    val presenceDays: Int,
    val halfDays: Int,
    val absences: Int,
    val sickDays: Int,
    val vacationDays: Int,
    val deductions: List<DeductionLine>,
) {
    val totalDeductions: Double get() = deductions.sumOf { it.amount }
    val net: Double get() = gross - totalDeductions
    /** Dias com presença contando meio período como meio dia. */
    val workedEquivalent: Double get() = presenceDays - halfDays * 0.5
}

data class Closing(val range: DateRange, val week: Summary, val previous: Summary)

data class AcqPeriod(
    val number: Int,
    val start: LocalDate,
    val end: LocalDate,
    val absences: Int,
    val entitlement: Int,
    val complete: Boolean,
    val taken: Int = 0,
) {
    /** Fim do período concessivo: a empresa tem até aqui para conceder as férias. */
    val deadline: LocalDate get() = end.plusMonths(12)
    val remaining: Int get() = max(0, entitlement - taken)
}

data class VacationInfo(
    val admission: LocalDate,
    val current: AcqPeriod,
    val monthsDone: Int,
    val pending: List<AcqPeriod>,
    val baseMonthly: Double,
) {
    val proportionalDays: Double get() = current.entitlement * monthsDone / 12.0
}

data class VacationPay(val restDays: Int, val soldDays: Int, val restPay: Double, val abono: Double, val third: Double) {
    val total: Double get() = restPay + abono + third
}

/**
 * Todas as contas do app a partir dos dados salvos: valor de cada dia, períodos,
 * descontos, projeção, férias e métricas.
 */
class Ledger(val data: AppData, val today: LocalDate) {

    val settings = data.settings
    private val holidayCache = HashMap<Int, Map<LocalDate, String>>()
    private val monthlyRateCache = HashMap<YearMonth, Double>()

    // ---------- Calendário ----------

    fun holidays(year: Int): Map<LocalDate, String> = holidayCache.getOrPut(year) {
        val map = LinkedHashMap<LocalDate, String>()
        if (settings.nationalHolidays) map.putAll(Holidays.national(year, settings.optionalHolidays))
        for (h in settings.customHolidays) {
            val d = runCatching { LocalDate.parse(h.date) }.getOrNull() ?: continue
            if (h.yearly) {
                runCatching { LocalDate.of(year, d.month, d.dayOfMonth) }.getOrNull()?.let { map[it] = h.name }
            } else if (d.year == year) {
                map[d] = h.name
            }
        }
        map.toSortedMap()
    }

    fun holidayName(d: LocalDate): String? = holidays(d.year)[d]

    fun isHoliday(d: LocalDate): Boolean = holidayName(d) != null

    /** Dia da semana faz parte da escala. */
    fun isScheduled(d: LocalDate): Boolean = d.dayOfWeek.value in settings.workDays

    /** Dia de trabalho previsto: está na escala e não é feriado. */
    fun isWorkday(d: LocalDate): Boolean = isScheduled(d) && !isHoliday(d)

    fun record(d: LocalDate): DayRecord? = data.days[d.toString()]

    fun expectedDays(r: DateRange): Int = r.dates().count { isWorkday(it) }

    // ---------- Valor do dia ----------

    fun baseDaily(d: LocalDate): Double = when (settings.payBasis) {
        PayBasis.DAILY -> settings.dailyRate
        PayBasis.HOURLY -> settings.hourlyRate * settings.hoursPerDay
        PayBasis.MONTHLY -> monthlyRateCache.getOrPut(YearMonth.from(d)) {
            val n = expectedDays(Periods.month(d))
            if (n > 0) settings.monthlySalary / n else 0.0
        }
    }

    /** Percentual da diária para um dia trabalhado (fim de semana fora da escala e feriado pagam mais). */
    fun multiplierPercent(d: LocalDate): Int = when {
        isHoliday(d) -> settings.holidayPercent
        d.dayOfWeek == DayOfWeek.SATURDAY && !isScheduled(d) -> settings.saturdayPercent
        d.dayOfWeek == DayOfWeek.SUNDAY && !isScheduled(d) -> settings.sundayPercent
        else -> 100
    }

    fun defaultValue(d: LocalDate, status: DayStatus): Double = when (status) {
        DayStatus.WORKED -> baseDaily(d) * multiplierPercent(d) / 100.0
        DayStatus.HALF -> baseDaily(d) * multiplierPercent(d) / 100.0 * settings.halfDayPercent / 100.0
        DayStatus.SICK -> if (settings.sickPaid) baseDaily(d) else 0.0
        else -> 0.0
    }

    fun paysSomething(status: DayStatus): Boolean =
        status == DayStatus.WORKED || status == DayStatus.HALF || (status == DayStatus.SICK && settings.sickPaid)

    fun baseValue(r: DayRecord): Double = r.customValue ?: defaultValue(r.localDate, r.status)

    fun dayValue(r: DayRecord): Double = baseValue(r) + r.extra

    fun kindOf(d: LocalDate, status: DayStatus): EarnKind? = when (status) {
        DayStatus.WORKED -> when {
            isHoliday(d) -> EarnKind.HOLIDAY
            d.dayOfWeek == DayOfWeek.SATURDAY && !isScheduled(d) -> EarnKind.SATURDAY
            d.dayOfWeek == DayOfWeek.SUNDAY && !isScheduled(d) -> EarnKind.SUNDAY
            else -> EarnKind.REGULAR
        }
        DayStatus.HALF -> EarnKind.HALF
        DayStatus.SICK -> if (settings.sickPaid) EarnKind.SICK else null
        else -> null
    }

    fun records(r: DateRange): List<DayRecord> = r.dates().mapNotNull { record(it) }

    // ---------- Totais ----------

    fun gross(r: DateRange): Double = records(r).sumOf { dayValue(it) }

    fun summary(r: DateRange): Summary {
        val recs = records(r)
        val kinds = LinkedHashMap<EarnKind, KindTotal>()
        var gross = 0.0
        var extras = 0.0
        for (rec in recs) {
            val base = baseValue(rec)
            gross += base + rec.extra
            extras += rec.extra
            val kind = kindOf(rec.localDate, rec.status) ?: continue
            val prev = kinds[kind]
            val unit = defaultValue(rec.localDate, rec.status)
            kinds[kind] = KindTotal((prev?.count ?: 0) + 1, (prev?.value ?: 0.0) + base, prev?.unit ?: unit)
        }
        val presence = recs.filter { it.status.isPresence }
        return Summary(
            range = r,
            gross = gross,
            byKind = EarnKind.entries.mapNotNull { k -> kinds[k]?.let { k to it } }.toMap(),
            extras = extras,
            presenceDays = presence.size,
            halfDays = recs.count { it.status == DayStatus.HALF },
            absences = recs.count { it.status == DayStatus.ABSENT },
            sickDays = recs.count { it.status == DayStatus.SICK },
            vacationDays = recs.count { it.status == DayStatus.VACATION },
            deductions = deductionLines(r, gross, presence.map { it.localDate }),
        )
    }

    // ---------- Descontos ----------

    private fun payPeriodsFrom(start: LocalDate, maxCount: Int, until: LocalDate): List<DateRange> {
        val out = mutableListOf<DateRange>()
        var p = Periods.pay(settings.payPeriod, start, settings.weekStartsSunday)
        while (out.size < maxCount && !p.start.isAfter(until)) {
            out += p
            p = Periods.pay(settings.payPeriod, p.end.plusDays(1), settings.weekStartsSunday)
        }
        return out
    }

    /**
     * Valor por período de pagamento distribuído no intervalo: no próprio período vale o valor cheio,
     * em uma visão diferente (ex.: uma semana de um pagamento mensal) vale a parte proporcional.
     * Retorna o valor e o número da última parcela que cai no intervalo.
     */
    private fun prorate(amount: Double, from: LocalDate, maxCount: Int, r: DateRange): Pair<Double, Int> {
        var total = 0.0
        var last = 0
        payPeriodsFrom(from, maxCount, r.end).forEachIndexed { i, p ->
            val ov = p.intersect(r) ?: return@forEachIndexed
            total += amount * ov.length / p.length
            last = i + 1
        }
        return total to last
    }

    fun deductionLine(dd: Deduction, r: DateRange, gross: Double, presence: List<LocalDate>): DeductionLine {
        val start = runCatching { LocalDate.parse(dd.startDate) }.getOrElse { r.start }
        if (!dd.active) return DeductionLine(dd, 0.0, "Pausado")
        val noun = Fmt.periodNoun(settings.payPeriod)
        return when (dd.type) {
            DeductionType.PER_DAY -> {
                val n = presence.count { !it.isBefore(start) }
                DeductionLine(dd, dd.amount * n, "${Fmt.money(dd.amount)} por dia trabalhado · ${Fmt.days(n)}")
            }
            DeductionType.FIXED -> {
                val from = if (start.isBefore(r.start)) r.start else start
                val v = if (start.isAfter(r.end)) 0.0 else prorate(dd.amount, from, Int.MAX_VALUE, r).first
                val detail = if (start.isAfter(r.end)) "Começa em ${Fmt.ddmm(start)}" else "${Fmt.money(dd.amount)} por $noun"
                DeductionLine(dd, v, detail)
            }
            DeductionType.PERCENT ->
                DeductionLine(dd, if (start.isAfter(r.end)) 0.0 else gross * dd.amount / 100.0, "${Fmt.percent(dd.amount)} do bruto")
            DeductionType.ONE_TIME ->
                DeductionLine(dd, if (start in r) dd.amount else 0.0, "Único · ${Fmt.ddmm(start)}")
            DeductionType.INSTALLMENTS -> {
                val n = max(1, dd.installments)
                val (v, last) = prorate(dd.amount, start, n, r)
                val detail = when {
                    start.isAfter(r.end) -> "Começa em ${Fmt.ddmm(start)}"
                    last == 0 -> "Quitado · $n × ${Fmt.money(dd.amount)}"
                    else -> "Parcela $last de $n · ${Fmt.money(dd.amount)} por $noun"
                }
                DeductionLine(dd, v, detail)
            }
        }
    }

    fun deductionLines(r: DateRange, gross: Double, presence: List<LocalDate>): List<DeductionLine> =
        data.deductions.map { deductionLine(it, r, gross, presence) }

    // ---------- Projeção e pagamento ----------

    /** Quanto ainda entra se todos os dias previstos sem registro forem trabalhados. */
    fun projection(r: DateRange): Pair<Double, Int> {
        var v = 0.0
        var n = 0
        for (d in r.dates()) {
            if (d.isBefore(today) || record(d) != null || !isWorkday(d)) continue
            v += defaultValue(d, DayStatus.WORKED)
            n++
        }
        return v to n
    }

    fun nextPayday(from: LocalDate = today): LocalDate {
        fun clamp(ym: YearMonth, day: Int) = ym.atDay(min(max(day, 1), ym.lengthOfMonth()))
        val ym = YearMonth.from(from)
        return when (settings.payPeriod) {
            PayPeriod.WEEKLY -> from.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(settings.payWeekday.coerceIn(1, 7))))
            PayPeriod.MONTHLY -> {
                val c = clamp(ym, settings.payDay1)
                if (!c.isBefore(from)) c else clamp(ym.plusMonths(1), settings.payDay1)
            }
            PayPeriod.BIWEEKLY -> listOf(
                clamp(ym, settings.payDay1), clamp(ym, settings.payDay2),
                clamp(ym.plusMonths(1), settings.payDay1), clamp(ym.plusMonths(1), settings.payDay2),
            ).filter { !it.isBefore(from) }.min()
        }
    }

    fun currentPayRange(): DateRange = Periods.pay(settings.payPeriod, today, settings.weekStartsSunday)

    // ---------- Fechamento da semana ----------

    /** No sábado e no domingo: a semana que termina no fim de semana atual. */
    fun closing(): Closing? {
        val saturday = when (today.dayOfWeek) {
            DayOfWeek.SATURDAY -> today
            DayOfWeek.SUNDAY -> today.minusDays(1)
            else -> return null
        }
        val week = Periods.week(saturday, settings.weekStartsSunday)
        val prev = Periods.week(week.start.minusDays(1), settings.weekStartsSunday)
        return Closing(week, summary(week), summary(prev))
    }

    // ---------- Métricas ----------

    /**
     * Dias com presença e dias previstos até hoje no intervalo (sem contar feriados).
     * Hoje só entra na conta depois de marcado.
     */
    fun presence(r: DateRange): Pair<Int, Int> {
        val last = if (record(today) == null) today.minusDays(1) else today
        val upTo = r.upTo(last) ?: return 0 to 0
        var expected = 0
        var present = 0
        for (d in upTo.dates()) {
            if (!isWorkday(d)) continue
            expected++
            if (record(d)?.status?.isPresence == true) present++
        }
        return present to expected
    }

    /** Dias de trabalho previstos seguidos com presença, até hoje. */
    fun streak(): Int {
        var d = if (record(today)?.status?.isPresence == true) today else today.minusDays(1)
        var count = 0
        var guard = 0
        while (guard++ < 800) {
            val rec = record(d)
            if (isWorkday(d)) {
                if (rec?.status?.isPresence == true) count++ else break
            } else if (rec?.status?.isPresence == true) {
                count++
            }
            d = d.minusDays(1)
        }
        return count
    }

    /** Dias trabalhados no sábado/domingo e quanto renderam. */
    fun weekendWork(r: DateRange): Pair<Int, Double> {
        val recs = records(r).filter {
            it.status.isPresence && (it.localDate.dayOfWeek == DayOfWeek.SATURDAY || it.localDate.dayOfWeek == DayOfWeek.SUNDAY)
        }
        return recs.size to recs.sumOf { dayValue(it) }
    }

    fun presenceCount(r: DateRange): Int = records(r).count { it.status.isPresence }

    // ---------- Férias ----------

    fun admission(): LocalDate? = settings.admissionDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun absences(r: DateRange): Int = records(r).count { it.status == DayStatus.ABSENT }

    fun vacation(): VacationInfo? {
        val adm = admission() ?: return null
        val periods = mutableListOf<AcqPeriod>()
        var k = 0
        while (k < 80) {
            val ps = adm.plusYears(k.toLong())
            val pe = adm.plusYears(k + 1L).minusDays(1)
            val complete = pe.isBefore(today)
            val abs = DateRange(ps, pe).upTo(today)?.let { absences(it) } ?: 0
            periods += AcqPeriod(k + 1, ps, pe, abs, entitlementFor(abs), complete)
            if (!complete) break
            k++
        }
        var taken = data.days.values.count { it.status == DayStatus.VACATION && !it.localDate.isBefore(adm) } +
            data.vacations.sumOf { it.soldDays }
        val allocated = periods.map { p ->
            val t = min(taken, p.entitlement)
            taken -= t
            p.copy(taken = t)
        }
        val current = allocated.last()
        return VacationInfo(
            admission = adm,
            current = current,
            monthsDone = monthsDone(current.start, today),
            pending = allocated.filter { it.complete && it.remaining > 0 },
            baseMonthly = baseMonthly(),
        )
    }

    /** Remuneração mensal usada nas férias: salário fixo, ou a média dos meses trabalhados. */
    fun baseMonthly(): Double {
        if (settings.payBasis == PayBasis.MONTHLY) return settings.monthlySalary
        val adm = admission()
        val firstFull = adm?.let { if (it.dayOfMonth == 1) YearMonth.from(it) else YearMonth.from(it).plusMonths(1) }
        val thisMonth = YearMonth.from(today)
        val grosses = (1..12)
            .map { thisMonth.minusMonths(it.toLong()) }
            .filter { firstFull == null || !it.isBefore(firstFull) }
            .map { gross(Periods.month(it.atDay(1))) }
            .filter { it > 0 }
        return if (grosses.isNotEmpty()) grosses.average() else baseDaily(today) * expectedDays(Periods.month(today))
    }

    fun vacationPay(base: Double, entitlement: Int, sell: Boolean): VacationPay {
        val sold = if (sell) entitlement / 3 else 0
        val rest = entitlement - sold
        return VacationPay(
            restDays = rest,
            soldDays = sold,
            restPay = base * rest / 30.0,
            abono = base * sold / 30.0,
            third = base * entitlement / 30.0 / 3.0,
        )
    }

    /** Autônomo: reserva sugerida de 1/12 + 1/3 (11,1%) do que entrou nos últimos 12 meses. */
    fun autonomoReserve(): Double = gross(DateRange(today.minusYears(1).plusDays(1), today)) / 9.0

    fun employmentIsClt(): Boolean = settings.employment == Employment.CLT

    companion object {
        fun entitlementFor(absences: Int): Int = when {
            absences <= 5 -> 30
            absences <= 14 -> 24
            absences <= 23 -> 18
            absences <= 32 -> 12
            else -> 0
        }

        /** Quantas faltas a mais derrubam o direito, e para quantos dias. */
        fun nextDrop(absences: Int): Pair<Int, Int>? = when {
            absences <= 5 -> (6 - absences) to 24
            absences <= 14 -> (15 - absences) to 18
            absences <= 23 -> (24 - absences) to 12
            absences <= 32 -> (33 - absences) to 0
            else -> null
        }

        /** Meses do período aquisitivo; fração de 15 dias ou mais conta como mês. */
        fun monthsDone(start: LocalDate, today: LocalDate): Int {
            if (today.isBefore(start)) return 0
            val endExclusive = today.plusDays(1)
            var m = ChronoUnit.MONTHS.between(start, endExclusive).toInt()
            val rest = ChronoUnit.DAYS.between(start.plusMonths(m.toLong()), endExclusive)
            if (rest >= 15) m++
            return min(m, 12)
        }
    }
}
