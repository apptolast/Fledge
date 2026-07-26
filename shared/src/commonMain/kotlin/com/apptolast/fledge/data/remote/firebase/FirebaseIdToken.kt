package com.apptolast.fledge.data.remote.firebase

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object FirebaseIdToken {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    fun claim(
        idToken: String?,
        name: String,
    ): String? {
        val parts = idToken?.split('.') ?: return null
        if (parts.size < 2) return null
        return runCatching {
            var payload = parts[1]
            while (payload.length % 4 != 0) {
                payload += "="
            }
            val decoded = Base64.UrlSafe.decode(payload).decodeToString()
            val primitive = json.parseToJsonElement(decoded).jsonObject[name]?.jsonPrimitive
            primitive?.contentOrNull ?: primitive?.booleanOrNull?.toString()
        }.getOrNull()
    }
}
