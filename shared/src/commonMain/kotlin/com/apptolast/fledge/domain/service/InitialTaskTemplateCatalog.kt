package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskTemplateDraft
import com.apptolast.fledge.domain.model.TaskTemplateSource

object InitialTaskTemplateCatalog {
    fun suggestionsFor(child: ChildProfile, familyId: FamilyId, currentYear: Int): List<TaskTemplateDraft> {
        val age = (currentYear - child.birthYear).coerceAtLeast(0)
        return presetsForAge(age).map { preset ->
            TaskTemplateDraft(
                familyId = familyId,
                title = preset.title,
                description = preset.description,
                iconKey = preset.iconKey,
                defaultValueCents = MoneyCents(preset.defaultValueCents),
                requiresPhoto = preset.requiresPhoto,
                suggestedMinAge = preset.minAge,
                suggestedMaxAge = preset.maxAge,
                source = TaskTemplateSource.InitialSuggestion,
                sourceChildProfileId = child.id,
                sourceKey = preset.key,
            )
        }
    }

    private fun presetsForAge(age: Int): List<TaskTemplatePreset> = when (age) {
        in 0..6 -> listOf(
            TaskTemplatePreset(
                key = "tidy-toys",
                title = "Recoger juguetes",
                description = "Guardar los juguetes usados antes de pasar a otra actividad.",
                iconKey = "blocks",
                defaultValueCents = 25,
                requiresPhoto = false,
                minAge = 4,
                maxAge = 6,
            ),
            TaskTemplatePreset(
                key = "brush-teeth",
                title = "Cepillarse los dientes",
                description = "Completar la rutina de dientes sin recordatorio extra.",
                iconKey = "sparkles",
                defaultValueCents = 20,
                requiresPhoto = false,
                minAge = 4,
                maxAge = 6,
            ),
            TaskTemplatePreset(
                key = "put-clothes-away",
                title = "Guardar la ropa",
                description = "Dejar la ropa limpia o usada en su sitio.",
                iconKey = "shirt",
                defaultValueCents = 25,
                requiresPhoto = true,
                minAge = 4,
                maxAge = 6,
            ),
        )
        in 7..9 -> listOf(
            TaskTemplatePreset(
                key = "set-table",
                title = "Poner la mesa",
                description = "Preparar platos, vasos y cubiertos antes de comer.",
                iconKey = "utensils",
                defaultValueCents = 50,
                requiresPhoto = false,
                minAge = 7,
                maxAge = 9,
            ),
            TaskTemplatePreset(
                key = "read-20",
                title = "Leer 20 minutos",
                description = "Leer un libro o comic durante veinte minutos.",
                iconKey = "book-open",
                defaultValueCents = 50,
                requiresPhoto = false,
                minAge = 7,
                maxAge = 9,
            ),
            TaskTemplatePreset(
                key = "tidy-desk",
                title = "Ordenar escritorio",
                description = "Dejar la mesa de estudio lista para el dia siguiente.",
                iconKey = "square-pen",
                defaultValueCents = 75,
                requiresPhoto = true,
                minAge = 7,
                maxAge = 9,
            ),
        )
        in 10..12 -> listOf(
            TaskTemplatePreset(
                key = "prepare-backpack",
                title = "Preparar mochila",
                description = "Revisar libros, agenda y material antes del colegio.",
                iconKey = "backpack",
                defaultValueCents = 75,
                requiresPhoto = false,
                minAge = 10,
                maxAge = 12,
            ),
            TaskTemplatePreset(
                key = "take-trash",
                title = "Bajar la basura",
                description = "Sacar la bolsa indicada al contenedor correcto.",
                iconKey = "trash-2",
                defaultValueCents = 100,
                requiresPhoto = false,
                minAge = 10,
                maxAge = 12,
            ),
            TaskTemplatePreset(
                key = "study-30",
                title = "Estudiar 30 minutos",
                description = "Completar una sesion corta de estudio sin pantallas.",
                iconKey = "graduation-cap",
                defaultValueCents = 100,
                requiresPhoto = false,
                minAge = 10,
                maxAge = 12,
            ),
        )
        else -> listOf(
            TaskTemplatePreset(
                key = "laundry-cycle",
                title = "Ayudar con la colada",
                description = "Tender, doblar o guardar una tanda de ropa.",
                iconKey = "washing-machine",
                defaultValueCents = 150,
                requiresPhoto = true,
                minAge = 13,
                maxAge = 17,
            ),
            TaskTemplatePreset(
                key = "clean-bathroom",
                title = "Limpiar el bano",
                description = "Completar la limpieza pactada del bano.",
                iconKey = "spray-can",
                defaultValueCents = 200,
                requiresPhoto = true,
                minAge = 13,
                maxAge = 17,
            ),
            TaskTemplatePreset(
                key = "weekly-plan",
                title = "Planificar la semana",
                description = "Revisar tareas, examenes y actividades de la semana.",
                iconKey = "calendar-check",
                defaultValueCents = 100,
                requiresPhoto = false,
                minAge = 13,
                maxAge = 17,
            ),
        )
    }
}

private data class TaskTemplatePreset(
    val key: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val defaultValueCents: Long,
    val requiresPhoto: Boolean,
    val minAge: Int,
    val maxAge: Int,
)
