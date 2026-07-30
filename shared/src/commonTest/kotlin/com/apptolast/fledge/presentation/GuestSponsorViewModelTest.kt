package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryGuestSponsorRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.FamilyGuestInvite
import com.apptolast.fledge.domain.model.GuestContributionKind
import com.apptolast.fledge.domain.model.GuestSponsorAccess
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.guest.GuestHomeError
import com.apptolast.fledge.presentation.foundation.guest.GuestHomeViewModel
import com.apptolast.fledge.presentation.foundation.guest.GuestSponsorInviteError
import com.apptolast.fledge.presentation.foundation.guest.GuestSponsorInviteViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class GuestSponsorViewModelTest {

    @Test
    fun `FLE-53 owner invites a guest for a child with normalized email`() = runTest {
        // Given
        val familyRepository = InMemoryFamilyFoundationRepository()
        val family = familyRepository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        familyRepository.recordVirtualMoneyConsent()
        val child = familyRepository.addChildProfile(
            familyId = family.id,
            displayName = "Leo",
            birthYear = 2016,
            avatarKey = "bike",
            pin = ChildPin("1234"),
        )
        val guestRepository = InMemoryGuestSponsorRepository(ownerFamilyId = family.id)
        val viewModel = GuestSponsorInviteViewModel(familyRepository, guestRepository)
        viewModel.load(child.id)
        runCurrent()

        // When
        viewModel.updateEmail(" Abuela@Example.com ")
        val saved = viewModel.invite()

        // Then
        assertTrue(saved)
        assertEquals("abuela@example.com", viewModel.uiState.value.savedInvite?.email)
        assertEquals(listOf(child.id), guestRepository.invites.single().childProfileIds)
        assertEquals("", viewModel.uiState.value.emailInput)
    }

    @Test
    fun `FLE-53 guest invite requires a valid email`() = runTest {
        // Given
        val familyRepository = InMemoryFamilyFoundationRepository()
        val family = familyRepository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        familyRepository.recordVirtualMoneyConsent()
        val child = familyRepository.addChildProfile(
            familyId = family.id,
            displayName = "Leo",
            birthYear = 2016,
            avatarKey = "bike",
            pin = ChildPin("1234"),
        )
        val guestRepository = InMemoryGuestSponsorRepository(ownerFamilyId = family.id)
        val viewModel = GuestSponsorInviteViewModel(familyRepository, guestRepository)
        viewModel.load(child.id)
        runCurrent()

        // When
        viewModel.updateEmail("not-email")
        val saved = viewModel.invite()

        // Then
        assertFalse(saved)
        assertEquals(GuestSponsorInviteError.InvalidEmail, viewModel.uiState.value.error)
        assertEquals(emptyList(), guestRepository.invites)
    }

    @Test
    fun `FLE-53 guest contributes a match to a sponsored child`() = runTest {
        // Given
        val familyRepository = InMemoryFamilyFoundationRepository()
        val family = familyRepository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        familyRepository.recordVirtualMoneyConsent()
        val child = familyRepository.addChildProfile(
            familyId = family.id,
            displayName = "Leo",
            birthYear = 2016,
            avatarKey = "bike",
            pin = ChildPin("1234"),
        )
        val invite = FamilyGuestInvite(
            familyId = family.id,
            email = "abuela@example.com",
            childProfileIds = listOf(child.id),
        )
        val guestRepository = InMemoryGuestSponsorRepository(ownerFamilyId = family.id)
        guestRepository.setActiveGuestAccess(
            GuestSponsorAccess(
                invite = invite,
                familyName = family.name,
                currency = family.currency,
                children = listOf(child),
            ),
        )
        val viewModel = GuestHomeViewModel(guestRepository)
        runCurrent()

        // When
        viewModel.selectKind(GuestContributionKind.Match)
        viewModel.updateAmount("12,50")
        viewModel.updateConcept("Reto de lectura")
        val saved = viewModel.submit()

        // Then
        assertTrue(saved)
        val transaction = guestRepository.transactions.single()
        assertEquals(LedgerTransactionType.Match, transaction.type)
        assertEquals(LedgerActor.Guest, transaction.createdBy)
        assertEquals(1_250, transaction.amountCents.value)
        assertEquals(child.id, transaction.childProfileId)
        assertEquals("", viewModel.uiState.value.amountInput)
    }

    @Test
    fun `FLE-53 guest contribution requires a positive amount`() = runTest {
        // Given
        val familyRepository = InMemoryFamilyFoundationRepository()
        val family = familyRepository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        familyRepository.recordVirtualMoneyConsent()
        val child = familyRepository.addChildProfile(
            familyId = family.id,
            displayName = "Leo",
            birthYear = 2016,
            avatarKey = "bike",
            pin = ChildPin("1234"),
        )
        val guestRepository = InMemoryGuestSponsorRepository(ownerFamilyId = family.id)
        guestRepository.setActiveGuestAccess(
            GuestSponsorAccess(
                invite = FamilyGuestInvite(
                    familyId = family.id,
                    email = "abuela@example.com",
                    childProfileIds = listOf(child.id),
                ),
                familyName = family.name,
                currency = family.currency,
                children = listOf(child),
            ),
        )
        val viewModel = GuestHomeViewModel(guestRepository)
        runCurrent()

        // When
        viewModel.updateAmount("0")
        viewModel.updateConcept("Regalo")
        val saved = viewModel.submit()

        // Then
        assertFalse(saved)
        assertEquals(GuestHomeError.InvalidAmount, viewModel.uiState.value.error)
    }
}
