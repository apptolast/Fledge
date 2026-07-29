package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskTemplate
import com.apptolast.fledge.domain.model.TaskTemplateDraft
import com.apptolast.fledge.domain.model.TaskTemplateId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class TaskTemplateModelTest {

    @Test
    fun `AC-01 TaskTemplate conserva los campos requeridos`() {
        // Given
        val familyId = FamilyId("family-1")

        // When
        val template = TaskTemplate(
            id = TaskTemplateId("template-1"),
            familyId = familyId,
            title = "Poner la mesa",
            description = "Preparar platos, vasos y cubiertos antes de comer.",
            iconKey = "utensils",
            defaultValueCents = MoneyCents(50),
            requiresPhoto = false,
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
            updatedAt = Instant.fromEpochSeconds(1_700_000_000),
        )

        // Then
        assertEquals(TaskTemplateId("template-1"), template.id)
        assertEquals(familyId, template.familyId)
        assertEquals("Poner la mesa", template.title)
        assertEquals("Preparar platos, vasos y cubiertos antes de comer.", template.description)
        assertEquals("utensils", template.iconKey)
        assertEquals(MoneyCents(50), template.defaultValueCents)
        assertEquals(false, template.requiresPhoto)
    }

    @Test
    fun `AC-01 TaskTemplate rechaza datos invalidos`() {
        // Given
        val valid = TaskTemplateDraft(
            familyId = FamilyId("family-1"),
            title = "Poner la mesa",
            description = "Preparar platos, vasos y cubiertos antes de comer.",
            iconKey = "utensils",
            defaultValueCents = MoneyCents(50),
            requiresPhoto = false,
        )

        // When / Then
        assertFailsWith<IllegalArgumentException> { valid.copy(title = "") }
        assertFailsWith<IllegalArgumentException> { valid.copy(description = "") }
        assertFailsWith<IllegalArgumentException> { valid.copy(iconKey = "") }
        assertFailsWith<IllegalArgumentException> { valid.copy(defaultValueCents = MoneyCents(-50)) }
    }
}
