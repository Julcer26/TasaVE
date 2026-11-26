package com.julcer26.tasave.calendar

import com.julcer26.tasave.ultra.RateSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class CalendarDayInfo(
    val isoDate: String,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isWeekend: Boolean,
    val bankHolidayName: String?,
    val statusSummary: String
) {
    val isBankHoliday: Boolean get() = bankHolidayName != null
}

data class DayRateLookup(
    val isoDate: String,
    val usdBcv: Double,
    val eurBcv: Double,
    val usdt: Double,
    val isExactSnapshot: Boolean
)

/**
 * Helper for Venezuelan bank holidays and historical daily rate lookup (minSdk 24 safe).
 */
object BcvCalendarHelper {

    private val caracasTz: TimeZone = TimeZone.getTimeZone("America/Caracas")

    // Fixed & movable bank holidays in Venezuela (2025–2027)
    private val BANK_HOLIDAYS: Map<String, String> = mapOf(
        // 2025
        "2025-01-01" to "Año Nuevo (Feriado nacional)",
        "2025-01-06" to "Día de Reyes (Lunes bancario)",
        "2025-01-13" to "Divina Pastora (Lunes bancario)",
        "2025-03-03" to "Carnaval (Lunes)",
        "2025-03-04" to "Carnaval (Martes)",
        "2025-03-24" to "San José (Lunes bancario)",
        "2025-04-17" to "Jueves Santo",
        "2025-04-18" to "Viernes Santo",
        "2025-05-01" to "Día del Trabajador",
        "2025-06-02" to "Ascensión del Señor (Lunes bancario)",
        "2025-06-16" to "Día de San Antonio (Lunes bancario)",
        "2025-06-23" to "Corpus Christi (Lunes bancario)",
        "2025-06-24" to "Batalla de Carabobo",
        "2025-07-05" to "Día de la Independencia",
        "2025-07-24" to "Natalicio del Libertador",
        "2025-08-18" to "Asunción de Nuestra Señora (Lunes bancario)",
        "2025-09-15" to "Virgen de Coromoto (Lunes bancario)",
        "2025-10-12" to "Resistencia Indígena",
        "2025-10-27" to "Dr. José Gregorio Hernández (Lunes bancario)",
        "2025-11-03" to "Día de todos los Santos (Lunes bancario)",
        "2025-11-17" to "Virgen de Chiquinquirá (Lunes bancario)",
        "2025-12-08" to "Inmaculada Concepción (Lunes bancario)",
        "2025-12-24" to "Nochebuena (Feriado bancario)",
        "2025-12-25" to "Navidad",
        "2025-12-31" to "Fin de Año (Feriado bancario)",
        // 2026
        "2026-01-01" to "Año Nuevo (Feriado nacional)",
        "2026-01-12" to "Día de Reyes (Lunes bancario)",
        "2026-01-19" to "Divina Pastora (Lunes bancario)",
        "2026-02-16" to "Carnaval (Lunes)",
        "2026-02-17" to "Carnaval (Martes)",
        "2026-03-19" to "Día de San José (Feriado nacional/bancario)",
        "2026-04-02" to "Jueves Santo",
        "2026-04-03" to "Viernes Santo",
        "2026-04-19" to "Declaración de la Independencia",
        "2026-05-01" to "Día del Trabajador",
        "2026-05-18" to "Ascensión del Señor (Lunes bancario)",
        "2026-06-08" to "Corpus Christi (Lunes bancario)",
        "2026-06-15" to "Día de San Antonio (Lunes bancario)",
        "2026-06-24" to "Batalla de Carabobo",
        "2026-06-29" to "San Pedro y San Pablo (Lunes bancario)",
        "2026-07-05" to "Día de la Independencia",
        "2026-07-24" to "Natalicio del Libertador",
        "2026-08-17" to "Asunción de Nuestra Señora (Lunes bancario)",
        "2026-09-14" to "Virgen de Coromoto (Lunes bancario)",
        "2026-10-12" to "Resistencia Indígena",
        "2026-10-26" to "Dr. José Gregorio Hernández (Lunes bancario)",
        "2026-11-02" to "Día de los Fieles Difuntos (Lunes bancario)",
        "2026-11-16" to "Virgen de Chiquinquirá (Lunes bancario)",
        "2026-11-23" to "Lunes bancario",
        "2026-12-07" to "Inmaculada Concepción (Lunes bancario)",
        "2026-12-24" to "Nochebuena (Feriado bancario)",
        "2026-12-25" to "Navidad",
        "2026-12-31" to "Fin de Año (Feriado bancario)",
        // 2027
        "2027-01-01" to "Año Nuevo (Feriado nacional)",
        "2027-01-11" to "Día de Reyes (Lunes bancario)",
        "2027-01-18" to "Divina Pastora (Lunes bancario)",
        "2027-02-08" to "Carnaval (Lunes)",
        "2027-02-09" to "Carnaval (Martes)",
        "2027-03-25" to "Jueves Santo",
        "2027-03-26" to "Viernes Santo",
        "2027-04-19" to "Declaración de la Independencia",
        "2027-05-01" to "Día del Trabajador",
        "2027-06-24" to "Batalla de Carabobo",
        "2027-07-05" to "Día de la Independencia",
        "2027-07-24" to "Natalicio del Libertador",
        "2027-10-12" to "Resistencia Indígena",
        "2027-12-24" to "Nochebuena (Feriado bancario)",
        "2027-12-25" to "Navidad",
        "2027-12-31" to "Fin de Año (Feriado bancario)"
    )

    fun todayCaracasCalendar(): Calendar = Calendar.getInstance(caracasTz)

    fun formatIsoDate(cal: Calendar): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = caracasTz
        return sdf.format(cal.time)
    }

    fun formatMonthYearEs(year: Int, monthZeroBased: Int): String {
        val months = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val mName = months.getOrElse(monthZeroBased) { "Mes" }
        return "$mName $year"
    }

    fun holidayNameFor(isoDate: String): String? = BANK_HOLIDAYS[isoDate]

    fun hasUpcomingBankHolidaySoon(nowCal: Calendar = todayCaracasCalendar()): Boolean {
        val probe = nowCal.clone() as Calendar
        for (offset in 0..3) {
            if (offset > 0) probe.add(Calendar.DAY_OF_MONTH, 1)
            val iso = formatIsoDate(probe)
            if (BANK_HOLIDAYS.containsKey(iso)) return true
        }
        return false
    }

    /**
     * Builds a Monday-first calendar grid (leading empty cells represented as null) for the given month.
     */
    fun buildMonthGrid(year: Int, monthZeroBased: Int): List<CalendarDayInfo?> {
        val cal = Calendar.getInstance(caracasTz).apply {
            clear()
            set(year, monthZeroBased, 1, 12, 0, 0)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2 ... Sat=7
        val leadingBlanks = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<CalendarDayInfo?>()
        repeat(leadingBlanks) { cells.add(null) }

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val iso = formatIsoDate(cal)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            val holiday = BANK_HOLIDAYS[iso]
            val summary = when {
                holiday != null -> "$holiday • Rige tasa hábil previa"
                isWeekend -> "Fin de semana • Rige tasa del viernes"
                else -> "Día hábil bancario"
            }
            cells.add(
                CalendarDayInfo(
                    isoDate = iso,
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isWeekend = isWeekend,
                    bankHolidayName = holiday,
                    statusSummary = summary
                )
            )
        }
        return cells
    }

    fun lookupRateForDate(
        isoDate: String,
        snapshots: List<RateSnapshot>,
        fallbackUsd: Double,
        fallbackEur: Double,
        fallbackUsdt: Double
    ): DayRateLookup {
        if (snapshots.isEmpty()) {
            return DayRateLookup(isoDate, fallbackUsd, fallbackEur, fallbackUsdt, isExactSnapshot = false)
        }

        val dayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = caracasTz
        }
        val sorted = snapshots.sortedBy { it.timestampMs }

        val exact = sorted.lastOrNull { snap ->
            dayFormatter.format(snap.timestampMs) == isoDate
        }
        if (exact != null) {
            return DayRateLookup(isoDate, exact.usdBcv, exact.eurBcv, exact.usdt, isExactSnapshot = true)
        }

        val targetMiddayMs = runCatching {
            val parsed = dayFormatter.parse(isoDate)
            (parsed?.time ?: 0L) + 18L * 60L * 60L * 1000L
        }.getOrDefault(0L)

        val priorOrClosest = sorted.lastOrNull { it.timestampMs <= targetMiddayMs } ?: sorted.first()
        return DayRateLookup(
            isoDate = isoDate,
            usdBcv = priorOrClosest.usdBcv.takeIf { it > 0.0 } ?: fallbackUsd,
            eurBcv = priorOrClosest.eurBcv.takeIf { it > 0.0 } ?: fallbackEur,
            usdt = priorOrClosest.usdt.takeIf { it > 0.0 } ?: fallbackUsdt,
            isExactSnapshot = false
        )
    }
}
