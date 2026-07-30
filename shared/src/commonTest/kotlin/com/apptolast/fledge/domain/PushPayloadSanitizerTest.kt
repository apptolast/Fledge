package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.service.PushPayloadSanitizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PushPayloadSanitizerTest {

    @Test
    fun `FLE-42 AC-03 keeps only privacy safe routing fields`() {
        // Given
        val sanitizer = PushPayloadSanitizer()
        val rawPayload = mapOf(
            "type" to "goal_completed",
            "familyId" to "family-1",
            "childProfileId" to "child-1",
            "savingsGoalId" to "goal-1",
            "childName" to "Mateo",
            "goalTitle" to "Bici nueva",
            "amountCents" to "4000",
            "taskTitle" to "Poner la mesa",
            "concept" to "Paga semanal",
        )

        // When
        val sanitized = sanitizer.sanitize(rawPayload)

        // Then
        assertEquals(
            mapOf(
                "type" to "goal_completed",
                "familyId" to "family-1",
                "childProfileId" to "child-1",
                "savingsGoalId" to "goal-1",
            ),
            sanitized,
        )
        assertFalse("childName" in sanitized)
        assertFalse("goalTitle" in sanitized)
        assertFalse("amountCents" in sanitized)
        assertFalse("taskTitle" in sanitized)
        assertFalse("concept" in sanitized)
    }

    @Test
    fun `FLE-42 AC-03 drops blank and unknown fields`() {
        // Given
        val sanitizer = PushPayloadSanitizer()

        // When
        val sanitized = sanitizer.sanitize(
            mapOf(
                "type" to "settlement_paid",
                "settlementId" to "settlement-1",
                "taskInstanceId" to " ",
                "debug" to "true",
            ),
        )

        // Then
        assertEquals(
            mapOf(
                "type" to "settlement_paid",
                "settlementId" to "settlement-1",
            ),
            sanitized,
        )
    }
}
