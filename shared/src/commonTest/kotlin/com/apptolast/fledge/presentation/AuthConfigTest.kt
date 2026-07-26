package com.apptolast.fledge.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthConfigTest {

    @Test
    fun `FLE-8 Android enables Google only when the web client id exists`() {
        // Given / When
        val config = fledgeLoginConfigFor(
            platform = FledgeLoginPlatform.Android,
            googleWebClientId = "web-client-id",
        )

        // Then
        assertNotNull(config.googleSignInConfig)
        assertNull(config.appleSignInConfig)
        assertNull(config.magicLinkConfig)
        assertFalse(config.githubEnabled)
        assertFalse(config.microsoftEnabled)
        assertFalse(config.phoneEnabled)
        assertFalse(config.twitterEnabled)
        assertFalse(config.facebookEnabled)
    }

    @Test
    fun `FLE-8 iOS enables Apple and hides Google`() {
        // Given / When
        val config = fledgeLoginConfigFor(platform = FledgeLoginPlatform.Ios)

        // Then
        assertNull(config.googleSignInConfig)
        assertNotNull(config.appleSignInConfig)
        assertFalse(config.phoneEnabled)
    }

    @Test
    fun `FLE-8 Android hides Google when the web client id is missing`() {
        // Given / When
        val config = fledgeLoginConfigFor(platform = FledgeLoginPlatform.Android)

        // Then
        assertNull(config.googleSignInConfig)
        assertNull(config.appleSignInConfig)
    }
}
