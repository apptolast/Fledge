package com.apptolast.fledge.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AuthConfigTest {

    @Test
    fun `AC-02 optional BaseLogin providers are disabled initially`() {
        // Given / When
        val config = initialFledgeLoginConfig()

        // Then
        assertNull(config.googleSignInConfig)
        assertNull(config.appleSignInConfig)
        assertNull(config.magicLinkConfig)
        assertFalse(config.githubEnabled)
        assertFalse(config.microsoftEnabled)
        assertFalse(config.phoneEnabled)
        assertFalse(config.twitterEnabled)
        assertFalse(config.facebookEnabled)
    }
}
