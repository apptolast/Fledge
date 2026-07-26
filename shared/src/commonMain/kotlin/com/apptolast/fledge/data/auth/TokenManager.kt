package com.apptolast.fledge.data.auth

import com.russhwolf.settings.Settings
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TokenManager(
    private val settings: Settings,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    var accessToken: String?
        get() = settings.getStringOrNull(KEY_ACCESS_TOKEN)
        set(value) {
            if (value == null) {
                settings.remove(KEY_ACCESS_TOKEN)
            } else {
                settings.putString(KEY_ACCESS_TOKEN, value)
            }
        }

    var refreshToken: String?
        get() = settings.getStringOrNull(KEY_REFRESH_TOKEN)
        set(value) {
            if (value == null) {
                settings.remove(KEY_REFRESH_TOKEN)
            } else {
                settings.putString(KEY_REFRESH_TOKEN, value)
            }
        }

    private var accessTokenExpiresAt: Long
        get() = settings.getLong(KEY_EXPIRES_AT, 0L)
        set(value) = settings.putLong(KEY_EXPIRES_AT, value)

    val isLoggedIn: Boolean get() = accessToken != null && refreshToken != null

    fun isAccessTokenExpired(): Boolean {
        val expiresAt = accessTokenExpiresAt
        if (expiresAt == 0L) return accessToken == null
        return nowMillis() >= expiresAt - REFRESH_BUFFER_MS
    }

    fun saveTokens(
        access: String,
        refresh: String,
        expiresInSeconds: Long,
    ) {
        accessToken = access
        refreshToken = refresh
        accessTokenExpiresAt = nowMillis() + expiresInSeconds * 1_000L
    }

    fun clearTokens() {
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_EXPIRES_AT)
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "fledge_access_token"
        const val KEY_REFRESH_TOKEN = "fledge_refresh_token"
        const val KEY_EXPIRES_AT = "fledge_expires_at"
        const val REFRESH_BUFFER_MS = 30_000L
    }
}
