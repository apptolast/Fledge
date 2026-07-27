package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.AllowanceDay
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.service.AllowanceSchedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AllowanceScheduleTest {

    @Test
    fun `FLE-22 monthly allowance day 31 runs on the last day of shorter months`() {
        // Given
        val timeZone = TimeZoneId("Europe/Madrid")

        // When
        val nextRunAt = AllowanceSchedule.nextRunAt(
            frequency = AllowanceFrequency.Monthly,
            day = AllowanceDay(31),
            from = Instant.parse("2026-02-01T12:00:00Z"),
            timeZone = timeZone,
        )
        val localDate = nextRunAt.toLocalDateTime(TimeZone.of(timeZone.value)).date

        // Then
        assertEquals(2026, localDate.year)
        assertEquals(Month.FEBRUARY, localDate.month)
        assertEquals(28, localDate.day)
    }

    @Test
    fun `FLE-22 weekly allowance uses ISO day numbers`() {
        // Given
        val timeZone = TimeZoneId("Europe/Madrid")

        // When
        val nextRunAt = AllowanceSchedule.nextRunAt(
            frequency = AllowanceFrequency.Weekly,
            day = AllowanceDay(1),
            from = Instant.parse("2026-07-26T12:00:00Z"),
            timeZone = timeZone,
        )
        val localDate = nextRunAt.toLocalDateTime(TimeZone.of(timeZone.value)).date

        // Then
        assertEquals(2026, localDate.year)
        assertEquals(Month.JULY, localDate.month)
        assertEquals(27, localDate.day)
    }
}
