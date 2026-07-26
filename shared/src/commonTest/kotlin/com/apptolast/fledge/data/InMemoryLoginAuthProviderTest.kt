package com.apptolast.fledge.data

import com.apptolast.customlogin.domain.model.AuthResult
import com.apptolast.customlogin.domain.model.AuthState
import com.apptolast.customlogin.domain.model.SignUpData
import com.apptolast.fledge.data.auth.InMemoryLoginAuthProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class InMemoryLoginAuthProviderTest {

    @Test
    fun `AC-02 registration creates a local BaseLogin session`() = runTest {
        // Given
        val provider = InMemoryLoginAuthProvider()

        // When
        val result = provider.signUp(
            SignUpData(
                email = "Parent@Example.com",
                password = "password-1234",
                displayName = "Parent User",
            )
        )

        // Then
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("parent@example.com", success.session.email)
        assertEquals(InMemoryLoginAuthProvider.PROVIDER_ID, success.session.providerId)
        assertEquals(AuthState.Authenticated(success.session), provider.observeAuthState().first())
    }
}
