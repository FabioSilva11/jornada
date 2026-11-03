package app.jornada.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class DayStatus { WORKED, HALF, ABSENT, SICK, OFF, HOLIDAY, VACATION }

val DayStatus.isPresence: Boolean get() = this == DayStatus.WORKED || this == DayStatus.HALF

@Serializable
data class DayRecord(
    /** Data ISO (yyyy-MM-dd). */
    val date: String,
    val status: DayStatus,
    /** Valor digitado à mão para o dia; nulo usa o valor calculado pelos ajustes. */
    val customValue: Double? = null,
    /** Hora extra, gorjeta ou bônus do dia. */
    val extra: Double = 0.0,
    val note: String = "",
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
}

@Serializable
enum class PayBasis { DAILY, MONTHLY, HOURLY }

@Serializable
enum class PayPeriod { WEEKLY, BIWEEKLY, MONTHLY }

@Serializable
enum class Employment { CLT, AUTONOMO }

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class CustomHoliday(
    val date: String,
    val name: String,
    /** Repete todo ano no mesmo dia e mês. */
    val yearly: Boolean = true,
)

@Serializable
data class Settings(
    val onboarded: Boolean = false,
    val payBasis: PayBasis = PayBasis.DAILY,
    val dailyRate: Double = 0.0,
    val monthlySalary: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val hoursPerDay: Double = 8.0,
    val payPeriod: PayPeriod = PayPeriod.MONTHLY,
    /** Dia do pagamento mensal, ou o primeiro dia do pagamento quinzenal. */
    val payDay1: Int = 5,
    /** Segundo dia do pagamento quinzenal. */
    val payDay2: Int = 20,
    /** Dia da semana do pagamento semanal (1 = segunda … 7 = domingo). */
    val payWeekday: Int = 5,
    val employment: Employment = Employment.CLT,
    val admissionDate: String? = null,
    /** Dias de trabalho previstos (1 = segunda … 7 = domingo). */
    val workDays: List<Int> = listOf(1, 2, 3, 4, 5),
    val halfDayPercent: Int = 50,
    /** Quanto o dia vale, em % da diária, quando trabalhado fora da escala. */
    val saturdayPercent: Int = 150,
    val sundayPercent: Int = 200,
    val holidayPercent: Int = 200,
    val sickPaid: Boolean = false,
    val showWeekendSummary: Boolean = true,
    val weekStartsSunday: Boolean = true,
    val nationalHolidays: Boolean = true,
    /** Carnaval e Corpus Christi (pontos facultativos). */
    val optionalHolidays: Boolean = false,
    val customHolidays: List<CustomHoliday> = emptyList(),
    val dailyReminder: Boolean = true,
    val reminderMinutes: Int = 18 * 60,
    val weeklySummary: Boolean = true,
    val summaryWeekday: Int = 6,
    val summaryMinutes: Int = 10 * 60,
    val vacationAlert: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastBackup: String? = null,
)

@Serializable
enum class DeductionType { PER_DAY, FIXED, PERCENT, ONE_TIME, INSTALLMENTS }

@Serializable
enum class DeductionCategory { TRANSPORTE, ALIMENTACAO, ADIANTAMENTO, UNIFORME, EMPRESTIMO, OUTRO }

@Serializable
data class Deduction(
    val id: String,
    val name: String,
    val category: DeductionCategory = DeductionCategory.OUTRO,
    val type: DeductionType = DeductionType.PER_DAY,
    /** Valor em R$ (por dia, por período ou por parcela) ou percentual, conforme o tipo. */
    val amount: Double = 0.0,
    val startDate: String,
    val installments: Int = 1,
    val active: Boolean = true,
)

@Serializable
data class VacationPlan(
    val id: String,
    val start: String,
    /** Dias corridos de descanso marcados no calendário. */
    val days: Int,
    /** Dias vendidos (abono pecuniário). */
    val soldDays: Int = 0,
)

@Serializable
data class AppData(
    val version: Int = 1,
    val settings: Settings = Settings(),
    /** Registros por data ISO. */
    val days: Map<String, DayRecord> = emptyMap(),
    val deductions: List<Deduction> = emptyList(),
    val vacations: List<VacationPlan> = emptyList(),
)
