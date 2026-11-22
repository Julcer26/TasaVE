package com.julcer26.tasave.calendar

import com.julcer26.tasave.ultra.RateSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BcvCalendarHelperTest {

    @Test
    fun sudebanHolidays_detectsBankHolidayAndUpcomingAlertWindow() {
        assertEquals("Lunes bancario SUDEBAN", BcvCalendarHelper.holidayNameFor("2026-11-23"))
        assertNull(BcvCalendarHelper.holidayNameFor("2026-11-24"))

        val tz = TimeZone.getTimeZone("America/Caracas")
        val twoDaysBefore = Calendar.getInstance(tz).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 21, 10, 0, 0)
        }
        assertTrue(BcvCalendarHelper.hasUpcomingBankHolidaySoon(twoDaysBefore))

        val normalMidweek = Calendar.getInstance(tz).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 24, 10, 0, 0)
        }
        assertFalse(BcvCalendarHelper.hasUpcomingBankHolidaySoon(normalMidweek))
    }

    @Test
    fun buildMonthGrid_createsMondayFirstGridWithHolidaysAndWeekends() {
        // November 2026 starts on Sunday (Nov 1), so Monday-first grid has 6 leading blanks
        val grid = BcvCalendarHelper.buildMonthGrid(2026, Calendar.NOVEMBER)
        assertEquals(6, grid.takeWhile { it == null }.size)

        val nonNullDays = grid.filterNotNull()
        assertEquals(30, nonNullDays.size)

        val nov23 = nonNullDays.firstOrNull { it.isoDate == "2026-11-23" }
        assertNotNull(nov23)
        assertTrue(nov23!!.isBankHoliday)
        assertFalse(nov23.isWeekend)
        assertTrue(nov23.statusSummary.contains("Lunes bancario SUDEBAN"))

        val nov22 = nonNullDays.firstOrNull { it.isoDate == "2026-11-22" }
        assertNotNull(nov22)
        assertTrue(nov22!!.isWeekend)
        assertFalse(nov22.isBankHoliday)
    }

    @Test
    fun lookupRateForDate_returnsExactSnapshotOrClosestPriorSnapshot() {
        val tz = TimeZone.getTimeZone("America/Caracas")
        val nov20Ms = Calendar.getInstance(tz).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 20, 12, 0, 0)
        }.timeInMillis

        val snapshots = listOf(
            RateSnapshot(
                timestampMs = nov20Ms,
                label = "20/11",
                usdBcv = 49.50,
                eurBcv = 53.80,
                usdt = 56.20
            )
        )

        val exact = BcvCalendarHelper.lookupRateForDate(
            isoDate = "2026-11-20",
            snapshots = snapshots,
            fallbackUsd = 50.0,
            fallbackEur = 54.0,
            fallbackUsdt = 57.0
        )
        assertTrue(exact.isExactSnapshot)
        assertEquals(49.50, exact.usdBcv, 0.001)
        assertEquals(53.80, exact.eurBcv, 0.001)
        assertEquals(56.20, exact.usdt, 0.001)

        val fallbackDay = BcvCalendarHelper.lookupRateForDate(
            isoDate = "2026-11-22",
            snapshots = snapshots,
            fallbackUsd = 50.0,
            fallbackEur = 54.0,
            fallbackUsdt = 57.0
        )
        assertFalse(fallbackDay.isExactSnapshot)
        assertEquals(49.50, fallbackDay.usdBcv, 0.001)
    }
}
