package app.jornada

import app.jornada.data.AppData
import app.jornada.data.DayRecord
import app.jornada.data.DayStatus
import app.jornada.data.Deduction
import app.jornada.data.DeductionType
import app.jornada.data.PayPeriod
import app.jornada.data.Repository
import app.jornada.data.Settings
import app.jornada.domain.DateRange
import app.jornada.domain.EarnKind
import app.jornada.domain.Fmt
import app.jornada.domain.Holidays
import app.jornada.domain.Ledger
import app.jornada.domain.Periods
import app.jornada.domain.ViewPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Cenário da direção de design: novembro de 2026, diária de R$ 120, hoje é quinta, 19/11. */
class LedgerTest {

    private val today = LocalDate.of(2026, 11, 19)

    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    private fun rec(day: Int, s: DayStatus) = d(day).toString() to DayRecord(d(day).toString(), s)

    private val settings = Settings(
        onboarded = true,
        dailyRate = 120.0,
        payPeriod = PayPeriod.MONTHLY,
        payDay1 = 5,
        admissionDate = "2026-03-10",
    )

    private val days = mapOf(
        rec(3, DayStatus.WORKED), rec(4, DayStatus.WORKED), rec(5, DayStatus.WORKED), rec(6, DayStatus.WORKED),
        rec(7, DayStatus.WORKED),
        rec(9, DayStatus.WORKED), rec(10, DayStatus.ABSENT), rec(11, DayStatus.WORKED), rec(12, DayStatus.WORKED),
        rec(13, DayStatus.HALF),
        rec(16, DayStatus.WORKED), rec(17, DayStatus.WORKED), rec(18, DayStatus.WORKED),
    )

    private val deductions = listOf(
        Deduction("vt", "Vale-transporte", type = DeductionType.PER_DAY, amount = 9.0, startDate = "2026-11-01"),
        Deduction("ref", "Refeição", type = DeductionType.PER_DAY, amount = 12.0, startDate = "2026-11-01"),
        Deduction("adi", "Adiantamento", type = DeductionType.ONE_TIME, amount = 200.0, startDate = "2026-11-10"),
        Deduction("uni", "Uniforme", type = DeductionType.INSTALLMENTS, amount = 45.0, startDate = "2026-11-01", installments = 3),
    )

    private val data = AppData(settings = settings, days = days, deductions = deductions)
    private val ledger = Ledger(data, today)

    @Test
    fun monthSummaryMatchesDesign() {
        val s = ledger.summary(Periods.month(today))
        assertEquals(1440.0, s.gross, 0.001)
        assertEquals(12, s.presenceDays)
        assertEquals(1, s.halfDays)
        assertEquals(1, s.absences)
        assertEquals(10, s.byKind[EarnKind.REGULAR]!!.count)
        assertEquals(1200.0, s.byKind[EarnKind.REGULAR]!!.value, 0.001)
        assertEquals(180.0, s.byKind[EarnKind.SATURDAY]!!.value, 0.001)
        assertEquals(60.0, s.byKind[EarnKind.HALF]!!.value, 0.001)
        assertEquals(497.0, s.totalDeductions, 0.001)
        assertEquals(943.0, s.net, 0.001)
    }

    @Test
    fun deductionDetails() {
        val lines = ledger.summary(Periods.month(today)).deductions.associateBy { it.deduction.id }
        assertEquals(108.0, lines["vt"]!!.amount, 0.001)
        assertEquals(144.0, lines["ref"]!!.amount, 0.001)
        assertEquals(200.0, lines["adi"]!!.amount, 0.001)
        assertEquals(45.0, lines["uni"]!!.amount, 0.001)
        assertTrue(lines["uni"]!!.detail.startsWith("Parcela 1 de 3"))

        val december = ledger.summary(Periods.month(LocalDate.of(2026, 12, 1))).deductions.associateBy { it.deduction.id }
        assertTrue(december["uni"]!!.detail.startsWith("Parcela 2 de 3"))
        assertEquals(0.0, december["adi"]!!.amount, 0.001)
        val february = ledger.summary(Periods.month(LocalDate.of(2027, 2, 1))).deductions.associateBy { it.deduction.id }
        assertEquals(0.0, february["uni"]!!.amount, 0.001)
        assertTrue(february["uni"]!!.detail.startsWith("Quitado"))
    }

    @Test
    fun fixedDeductionFollowsPayPeriod() {
        val biweekly = Ledger(
            data.copy(
                settings = settings.copy(payPeriod = PayPeriod.BIWEEKLY),
                deductions = listOf(Deduction("f", "Fixo", type = DeductionType.FIXED, amount = 100.0, startDate = "2026-11-01")),
            ),
            today,
        )
        assertEquals(200.0, biweekly.summary(Periods.month(today)).totalDeductions, 0.001)
        assertEquals(100.0, biweekly.summary(Periods.fortnight(today)).totalDeductions, 0.001)
    }

    @Test
    fun projectionAndPayday() {
        val (extra, left) = ledger.projection(Periods.month(today))
        assertEquals(7, left)
        assertEquals(2280.0, 1440.0 + extra, 0.001)
        assertEquals(LocalDate.of(2026, 12, 5), ledger.nextPayday())

        val biweekly = Ledger(data.copy(settings = settings.copy(payPeriod = PayPeriod.BIWEEKLY, payDay2 = 20)), today)
        assertEquals(LocalDate.of(2026, 11, 20), biweekly.nextPayday())
        val weekly = Ledger(data.copy(settings = settings.copy(payPeriod = PayPeriod.WEEKLY, payWeekday = 5)), today)
        assertEquals(LocalDate.of(2026, 11, 20), weekly.nextPayday())
    }

    @Test
    fun weekAndFortnightRanges() {
        assertEquals(DateRange(d(15), d(21)), Periods.week(today, sundayStart = true))
        assertEquals(DateRange(d(16), d(22)), Periods.week(today, sundayStart = false))
        assertEquals(DateRange(d(16), d(30)), Periods.fortnight(today))
        assertEquals(DateRange(d(1), d(15)), Periods.shift(ViewPeriod.FORTNIGHT, Periods.fortnight(today), -1, true))
        assertEquals(
            DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 15)),
            Periods.shift(ViewPeriod.FORTNIGHT, Periods.fortnight(today), 1, true),
        )
        assertEquals(420.0, ledger.summary(DateRange(d(8), d(14))).gross, 0.001)
    }

    @Test
    fun holidays() {
        assertEquals(LocalDate.of(2026, 4, 5), Holidays.easter(2026))
        assertEquals(LocalDate.of(2025, 4, 20), Holidays.easter(2025))
        assertTrue(ledger.isHoliday(d(2)))
        assertTrue(ledger.isHoliday(d(20)))
        assertTrue(ledger.isHoliday(LocalDate.of(2026, 4, 3)))
        assertEquals(240.0, ledger.defaultValue(d(20), DayStatus.WORKED), 0.001)
    }

    @Test
    fun vacationClt() {
        val info = ledger.vacation()!!
        assertEquals(8, info.monthsDone)
        assertEquals(LocalDate.of(2027, 3, 9), info.current.end)
        assertEquals(LocalDate.of(2028, 3, 9), info.current.deadline)
        assertEquals(1, info.current.absences)
        assertEquals(30, info.current.entitlement)
        assertTrue(info.pending.isEmpty())

        assertEquals(30, Ledger.entitlementFor(5))
        assertEquals(24, Ledger.entitlementFor(6))
        assertEquals(18, Ledger.entitlementFor(15))
        assertEquals(12, Ledger.entitlementFor(32))
        assertEquals(0, Ledger.entitlementFor(33))

        val pay = ledger.vacationPay(2460.0, 30, sell = false)
        assertEquals(3280.0, pay.total, 0.001)
        val sold = ledger.vacationPay(2460.0, 30, sell = true)
        assertEquals(20, sold.restDays)
        assertEquals(10, sold.soldDays)
        assertEquals(1640.0, sold.restPay, 0.001)
        assertEquals(820.0, sold.abono, 0.001)
        assertEquals(3280.0, sold.total, 0.001)
    }

    @Test
    fun metrics() {
        assertEquals(11 to 12, ledger.presence(Periods.month(today)))
        assertEquals(6, ledger.streak())
        assertEquals(1 to 180.0, ledger.weekendWork(Periods.month(today)))
    }

    @Test
    fun monthlySalaryBecomesDailyRate() {
        val monthly = Ledger(
            data.copy(settings = settings.copy(payBasis = app.jornada.data.PayBasis.MONTHLY, monthlySalary = 1900.0)),
            today,
        )
        // Novembro de 2026: 21 dias de segunda a sexta, menos os feriados de 02/11 e 20/11.
        assertEquals(19, monthly.expectedDays(Periods.month(today)))
        assertEquals(100.0, monthly.baseDaily(today), 0.001)
    }

    @Test
    fun formattingAndParsing() {
        assertTrue(Fmt.money(1440.0).contains("1.440,00"))
        assertEquals(1440.5, Fmt.parseMoney("1.440,50")!!, 0.001)
        assertEquals(120.0, Fmt.parseMoney("120")!!, 0.001)
        assertEquals(12.5, Fmt.parseMoney("12,5")!!, 0.001)
        assertEquals("Quinta, 19 de novembro", Fmt.longDay(today))
        assertEquals("3 meses e 19 dias", Fmt.duration(today, LocalDate.of(2027, 3, 10)))
        assertEquals("todo dia 5", Fmt.paydayRule(PayPeriod.MONTHLY, 5, 20, 5))
        assertEquals("toda sexta", Fmt.paydayRule(PayPeriod.WEEKLY, 5, 20, 5))
    }

    @Test
    fun jsonRoundTrip() {
        val text = Repository.encode(data)
        assertEquals(data, Repository.decode(text))
        // Campos desconhecidos (versões futuras) não quebram a leitura.
        val withExtra = text.replaceFirst("{", "{\"campoNovo\":1,")
        assertEquals(data, Repository.decode(withExtra))
    }
}
