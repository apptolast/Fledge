package com.apptolast.fledge.domain.service

class PushPayloadSanitizer(private val allowedKeys: Set<String> = DEFAULT_ALLOWED_KEYS) {
    fun sanitize(rawPayload: Map<String, String?>): Map<String, String> = rawPayload
        .filterKeys { key -> key in allowedKeys }
        .mapValues { entry -> entry.value?.trim().orEmpty() }
        .filterValues { value -> value.isNotBlank() }

    private companion object {
        val DEFAULT_ALLOWED_KEYS = setOf(
            "type",
            "familyId",
            "childProfileId",
            "taskTemplateId",
            "taskAssignmentId",
            "taskInstanceId",
            "savingsGoalId",
            "settlementId",
            "route",
        )
    }
}
