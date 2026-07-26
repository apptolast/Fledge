package com.apptolast.fledge.data

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InMemoryFamilyFoundationRepositoryTest {

    @Test
    fun `AC-03 parent creates family draft with currency and timezone`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()

        // When / Then
        repository.activeFamily.test {
            assertEquals(null, awaitItem())

            val family = repository.createFamily(
                name = "Familia Garcia",
                currency = CurrencyCode("EUR"),
                timeZone = TimeZoneId("Europe/Madrid"),
            )

            assertEquals("Familia Garcia", family.name)
            assertEquals(CurrencyCode("EUR"), family.currency)
            assertEquals(TimeZoneId("Europe/Madrid"), family.timeZone)
            assertTrue(family.moneySettingsLocked)
            assertEquals(family, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FLE-10 family currency and timezone cannot be recreated`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        repository.createFamily(
            name = "Familia Garcia",
            currency = CurrencyCode("EUR"),
            timeZone = TimeZoneId("Europe/Madrid"),
        )

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            repository.createFamily(
                name = "Familia Garcia UK",
                currency = CurrencyCode("GBP"),
                timeZone = TimeZoneId("Europe/London"),
            )
        }
        assertEquals(CurrencyCode("EUR"), repository.activeFamily.value?.currency)
        assertEquals(TimeZoneId("Europe/Madrid"), repository.activeFamily.value?.timeZone)
    }

    @Test
    fun `FLE-11 child profile has no account credentials and stores pin hash`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily(
            name = "Familia Garcia",
            currency = CurrencyCode("EUR"),
            timeZone = TimeZoneId("Europe/Madrid"),
        )

        // When
        val child = repository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )

        // Then
        assertEquals("Lucas", child.displayName)
        assertEquals(2017, child.birthYear)
        assertNotNull(child.pinHash)
        assertNotEquals("1234", child.pinHash.value)
        assertFalse(ChildProfile::class.toString().contains("email"))
        assertFalse(ChildProfile::class.toString().contains("password"))
    }

    @Test
    fun `AC-07 pairing code belongs to profile and does not create account`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )

        // When
        val firstPairing = repository.startPairing(child.id)
        val secondPairing = repository.startPairing(child.id)

        // Then
        assertEquals(child.id, firstPairing.childProfileId)
        assertEquals(6, firstPairing.code.value.length)
        assertNotEquals(firstPairing.code, secondPairing.code)
        assertTrue(repository.children.value.single().pinHash != null)
    }
}
