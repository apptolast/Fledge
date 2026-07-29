package com.apptolast.fledge.data

import com.apptolast.fledge.data.repository.InMemoryTaskTemplateRepository
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskTemplateDraft
import com.apptolast.fledge.domain.model.TaskTemplateSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class InMemoryTaskTemplateRepositoryTest {

    @Test
    fun `AC-03 el seed inicial es idempotente`() = runTest {
        // Given
        val repository = InMemoryTaskTemplateRepository()
        val familyId = FamilyId("family-1")
        val child = ChildProfile(
            id = ChildProfileId("child-1"),
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
        )

        // When
        val firstSeed = repository.seedInitialSuggestionsForChild(
            familyId = familyId,
            child = child,
            currentYear = 2026,
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
        )
        val secondSeed = repository.seedInitialSuggestionsForChild(
            familyId = familyId,
            child = child,
            currentYear = 2026,
            createdAt = Instant.fromEpochSeconds(1_700_000_100),
        )

        // Then
        assertEquals(firstSeed.map { it.id }, secondSeed.map { it.id })
        assertEquals(3, repository.templatesForFamily(familyId).size)
        assertEquals(3, repository.templatesForFamily(familyId).mapNotNull { it.sourceKey }.toSet().size)
    }

    @Test
    fun `AC-04 el repositorio guarda plantillas custom ordenadas`() = runTest {
        // Given
        val repository = InMemoryTaskTemplateRepository()
        val familyId = FamilyId("family-1")

        // When
        repository.saveTemplate(
            TaskTemplateDraft(
                familyId = familyId,
                title = "Ordenar escritorio",
                description = "Dejar la mesa de estudio lista.",
                iconKey = "square-pen",
                defaultValueCents = MoneyCents(75),
                requiresPhoto = true,
            ),
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
        )
        repository.saveTemplate(
            TaskTemplateDraft(
                familyId = familyId,
                title = "Poner la mesa",
                description = "Preparar platos, vasos y cubiertos.",
                iconKey = "utensils",
                defaultValueCents = MoneyCents(50),
                requiresPhoto = false,
            ),
            createdAt = Instant.fromEpochSeconds(1_700_000_010),
        )

        // Then
        assertEquals(listOf("Ordenar escritorio", "Poner la mesa"), repository.templates.value.map { it.title })
        assertTrue(repository.templates.value.all { it.source == TaskTemplateSource.Custom })
    }
}
