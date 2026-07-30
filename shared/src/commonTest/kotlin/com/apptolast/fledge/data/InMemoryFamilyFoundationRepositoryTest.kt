package com.apptolast.fledge.data

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.FamilyAdminInviteDraft
import com.apptolast.fledge.domain.model.FamilyAdminInviteStatus
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.InterestSettingsDraft
import com.apptolast.fledge.domain.model.MatchSettingsDraft
import com.apptolast.fledge.domain.model.TimeZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
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
    fun `FLE-48 parent updates family interest without unlocking money settings`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily(
            name = "Familia Garcia",
            currency = CurrencyCode("EUR"),
            timeZone = TimeZoneId("Europe/Madrid"),
        )

        // When
        val settings = repository.updateInterestSettings(
            InterestSettingsDraft(
                enabled = true,
                annualRateBasisPoints = 250,
                postingDayOfMonth = 5,
            ),
        )

        // Then
        assertTrue(settings.enabled)
        assertEquals(250, settings.annualRateBasisPoints)
        assertEquals(5, settings.postingDayOfMonth)
        assertEquals(family.currency, repository.activeFamily.value?.currency)
        assertEquals(family.timeZone, repository.activeFamily.value?.timeZone)
        assertTrue(repository.activeFamily.value?.moneySettingsLocked == true)
    }

    @Test
    fun `FLE-51 parent updates family match without unlocking money settings`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily(
            name = "Familia Garcia",
            currency = CurrencyCode("EUR"),
            timeZone = TimeZoneId("Europe/Madrid"),
        )

        // When
        val settings = repository.updateMatchSettings(
            MatchSettingsDraft(
                enabled = true,
                matchBasisPoints = 10_000,
                maxMatchCents = 500,
            ),
        )

        // Then
        assertTrue(settings.enabled)
        assertEquals(10_000, settings.matchBasisPoints)
        assertEquals(500L, settings.maxMatchCents)
        assertEquals(family.currency, repository.activeFamily.value?.currency)
        assertEquals(family.timeZone, repository.activeFamily.value?.timeZone)
        assertTrue(repository.activeFamily.value?.moneySettingsLocked == true)
    }

    @Test
    fun `FLE-52 owner invites and revokes a secondary admin`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        repository.createFamily(
            name = "Familia Garcia",
            currency = CurrencyCode("EUR"),
            timeZone = TimeZoneId("Europe/Madrid"),
        )

        // When
        val invite = repository.inviteAdmin(FamilyAdminInviteDraft(" Cristina@Example.com "))
        val revoked = repository.revokeAdminInvite("cristina@example.com")

        // Then
        assertEquals("cristina@example.com", invite.email)
        assertEquals(FamilyAdminInviteStatus.Active, invite.status)
        assertEquals("cristina@example.com", revoked?.email)
        assertEquals(FamilyAdminInviteStatus.Revoked, revoked?.status)
        assertEquals(emptyList(), repository.activeFamily.value?.adminEmails)
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
        repository.recordVirtualMoneyConsent()

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
        repository.recordVirtualMoneyConsent()
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

    @Test
    fun `FLE-13 child pin validates hash and uses configurable timeout`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        repository.setChildPinTimeout(30)

        // When
        val wrongSession = repository.validateChildPin(child.id, ChildPin("9999"))
        val session = repository.validateChildPin(child.id, ChildPin("1234"))

        // Then
        assertEquals(null, wrongSession)
        assertNotNull(session)
        assertEquals(child.id, session.childProfileId)
        assertTrue(session.expiresAt > session.unlockedAt.plus(29.minutes))
    }

    @Test
    fun `FLE-14 pairing lasts fifteen minutes and persists child device last seen`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )

        // When
        val pairing = repository.startPairing(child.id)
        val device = repository.registerChildDevice(pairing.code, "Tablet salon")
        val touched = repository.touchChildDevice(device.id)

        // Then
        assertEquals(child.id, device.childProfileId)
        assertEquals(pairing.code, device.pairingCode)
        assertTrue(pairing.expiresAt > Clock.System.now().plus(14.minutes))
        assertEquals(device.id, touched?.id)
        assertTrue(repository.childDevices.value.single().lastSeenAt >= device.lastSeenAt)
    }

    @Test
    fun `FLE-15 protected actions are stored until parental confirmation`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()

        // When
        repository.requireParentalGate(FoundationAction.OpenParentZone)

        // Then
        assertEquals(FoundationAction.OpenParentZone, repository.parentalGateRequest.value?.action)
        assertEquals(FoundationAction.OpenParentZone, repository.confirmParentalGate())
        assertEquals(null, repository.parentalGateRequest.value)
    }

    @Test
    fun `FLE-16 virtual money consent is recorded before child setup`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()

        // When
        val consent = repository.recordVirtualMoneyConsent()

        // Then
        assertEquals("virtual-money-v1", consent.disclosureVersion)
        assertEquals(consent, repository.virtualMoneyConsent.value)
    }

    @Test
    fun `FLE-16 child profile cannot be created before virtual money consent`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            repository.addChildProfile(
                familyId = family.id,
                displayName = "Lucas",
                birthYear = 2017,
                avatarKey = "rocket",
                pin = ChildPin("1234"),
            )
        }
    }
}
