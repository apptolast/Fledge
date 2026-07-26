package com.apptolast.fledge.data.remote.firebase

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createFirebaseAuthHttpClient(json: Json): HttpClient = HttpClient {
    expectSuccess = false
    install(ContentNegotiation) {
        json(json)
    }
}
