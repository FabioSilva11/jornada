package app.jornada.domain

import java.time.LocalDate

/** Feriados nacionais do Brasil, incluindo os móveis calculados a partir da Páscoa. */
object Holidays {

    /** Domingo de Páscoa (algoritmo gregoriano anônimo). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    fun national(year: Int, includeOptional: Boolean): Map<LocalDate, String> {
        val easter = easter(year)
        val map = linkedMapOf(
            LocalDate.of(year, 1, 1) to "Confraternização Universal",
            easter.minusDays(2) to "Paixão de Cristo",
            LocalDate.of(year, 4, 21) to "Tiradentes",
            LocalDate.of(year, 5, 1) to "Dia do Trabalho",
            LocalDate.of(year, 9, 7) to "Independência do Brasil",
            LocalDate.of(year, 10, 12) to "Nossa Senhora Aparecida",
            LocalDate.of(year, 11, 2) to "Finados",
            LocalDate.of(year, 11, 15) to "Proclamação da República",
            LocalDate.of(year, 12, 25) to "Natal",
        )
        if (year >= 2024) map[LocalDate.of(year, 11, 20)] = "Consciência Negra"
        if (includeOptional) {
            map[easter.minusDays(48)] = "Carnaval"
            map[easter.minusDays(47)] = "Carnaval"
            map[easter.plusDays(60)] = "Corpus Christi"
        }
        return map.toSortedMap()
    }
}
