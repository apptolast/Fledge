package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskTemplateSource
import com.apptolast.fledge.domain.service.InitialTaskTemplateCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InitialTaskTemplateCatalogTest {

    @Test
    fun `AC-02 el catalogo propone tareas por edad`() {
        // Given
        val child = ChildProfile(
            id = ChildProfileId("child-1"),
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
        )

        // When
        val suggestions = InitialTaskTemplateCatalog.suggestionsFor(
            child = child,
            familyId = FamilyId("family-1"),
            currentYear = 2026,
        )

        // Then
        assertEquals(listOf("Poner la mesa", "Leer 20 minutos", "Ordenar escritorio"), suggestions.map { it.title })
        suggestions.forEach { suggestion ->
            assertEquals(FamilyId("family-1"), suggestion.familyId)
            assertEquals(TaskTemplateSource.InitialSuggestion, suggestion.source)
            assertEquals(ChildProfileId("child-1"), suggestion.sourceChildProfileId)
            assertEquals(7, suggestion.suggestedMinAge)
            assertEquals(9, suggestion.suggestedMaxAge)
            assertTrue(suggestion.description.isNotBlank())
            assertTrue(suggestion.iconKey.isNotBlank())
            assertTrue(suggestion.defaultValueCents.value > 0)
            assertNotNull(suggestion.requiresPhoto)
            assertNotNull(suggestion.sourceKey)
        }
    }
}
