package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.VirtualAccountType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LedgerModelTest {

    @Test
    fun `FLE-18 transaction amounts are integer cents and never zero`() {
        // Given / When / Then
        assertEquals(125L, MoneyCents(125).value)
        assertEquals(-125L, MoneyCents(125).reversed().value)
        assertEquals(0L, BalanceCents(0).value)
        assertFailsWith<IllegalArgumentException> { MoneyCents(0) }
    }

    @Test
    fun `FLE-18 reversal entries cannot be created as regular drafts`() {
        // Given / When / Then
        assertFailsWith<IllegalArgumentException> {
            LedgerTransactionDraft(
                familyId = FamilyId("family-1"),
                childProfileId = ChildProfileId("child-1"),
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Reversal,
                amountCents = MoneyCents(-500),
                concept = LedgerConcept("Correccion"),
                createdBy = LedgerActor.Parent,
            )
        }
    }

    @Test
    fun `FLE-18 ledger concept is mandatory`() {
        // Given / When / Then
        assertFailsWith<IllegalArgumentException> { LedgerConcept("") }
        assertFailsWith<IllegalArgumentException> { LedgerConcept("   ") }
    }
}
