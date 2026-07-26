package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.AllowanceDay
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.TimeZoneId
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object AllowanceSchedule {
    fun nextRunAt(
        frequency: AllowanceFrequency,
        day: AllowanceDay,
        from: Instant,
        timeZone: TimeZoneId,
    ): Instant {
        val zone = TimeZone.of(timeZone.value)
        val fromDate = from.toLocalDateTime(zone).date
        val candidate = when (frequency) {
            AllowanceFrequency.Weekly -> weeklyRunDate(fromDate, day)
            AllowanceFrequency.Monthly -> monthlyRunDate(fromDate.year, fromDate.month.toMonthNumber(), day.value)
        }
        val candidateInstant = candidate.atStartOfDayIn(zone)
        if (candidateInstant > from) return candidateInstant

        return when (frequency) {
            AllowanceFrequency.Weekly -> candidate.plus(DatePeriod(days = 7)).atStartOfDayIn(zone)
            AllowanceFrequency.Monthly -> {
                val nextMonth = nextMonth(fromDate.year, fromDate.month.toMonthNumber())
                monthlyRunDate(nextMonth.year, nextMonth.month, day.value).atStartOfDayIn(zone)
            }
        }
    }

    fun nextRunAtAfter(
        previousRunAt: Instant,
        frequency: AllowanceFrequency,
        day: AllowanceDay,
        timeZone: TimeZoneId,
    ): Instant {
        val zone = TimeZone.of(timeZone.value)
        val previousDate = previousRunAt.toLocalDateTime(zone).date
        return when (frequency) {
            AllowanceFrequency.Weekly -> previousDate.plus(DatePeriod(days = 7)).atStartOfDayIn(zone)
            AllowanceFrequency.Monthly -> {
                val nextMonth = nextMonth(previousDate.year, previousDate.month.toMonthNumber())
                monthlyRunDate(nextMonth.year, nextMonth.month, day.value).atStartOfDayIn(zone)
            }
        }
    }

    private fun weeklyRunDate(fromDate: LocalDate, day: AllowanceDay): LocalDate {
        val daysUntil = (day.value - fromDate.dayOfWeek.isoDayNumber() + 7) % 7
        return fromDate.plus(DatePeriod(days = daysUntil))
    }

    private fun DayOfWeek.isoDayNumber(): Int = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }

    private fun Month.toMonthNumber(): Int = when (this) {
        Month.JANUARY -> 1
        Month.FEBRUARY -> 2
        Month.MARCH -> 3
        Month.APRIL -> 4
        Month.MAY -> 5
        Month.JUNE -> 6
        Month.JULY -> 7
        Month.AUGUST -> 8
        Month.SEPTEMBER -> 9
        Month.OCTOBER -> 10
        Month.NOVEMBER -> 11
        Month.DECEMBER -> 12
    }

    private fun monthlyRunDate(year: Int, month: Int, targetDay: Int): LocalDate {
        val day = minOf(targetDay, daysInMonth(year, month))
        return LocalDate(year, month, day)
    }

    private fun nextMonth(year: Int, month: Int): YearMonth =
        if (month == 12) YearMonth(year + 1, 1) else YearMonth(year, month + 1)

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> error("Invalid month: $month")
    }

    private fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    private data class YearMonth(
        val year: Int,
        val month: Int,
    )
}
