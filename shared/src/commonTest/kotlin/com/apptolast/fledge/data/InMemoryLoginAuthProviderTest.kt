package com.apptolast.fledge.data

import com.apptolast.customlogin.domain.model.AuthResult
import com.apptolast.customlogin.domain.model.AuthState
import com.apptolast.customlogin.domain.model.Credentials
import com.apptolast.customlogin.domain.model.SignUpData
import com.apptolast.fledge.data.auth.InMemoryLoginAuthProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class InMemoryLoginAuthProviderTest {

    @Test
    fun `FLE-9 registration creates an unverified parent session`() = runTest {
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
        assertEquals(false, success.session.isEmailVerified)
        assertEquals(InMemoryLoginAuthProvider.PROVIDER_ID, success.session.providerId)
        assertEquals(AuthState.Authenticated(success.session), provider.observeAuthState().first())
    }

    @Test
    fun `FLE-9 sign in validates parent password`() = runTest {
        // Given
        val provider = InMemoryLoginAuthProvider()
        provider.signUp(
            SignUpData(
                email = "parent@example.com",
                password = "password-1234",
                displayName = "Parent User",
            )
        )
        provider.signOut()

        // When
        val invalid = provider.signIn(Credentials.EmailPassword("parent@example.com", "wrong-password"))
        val valid = provider.signIn(Credentials.EmailPassword("parent@example.com", "password-1234"))

        // Then
        assertIs<AuthResult.Failure>(invalid)
        assertIs<AuthResult.Success>(valid)
    }

    @Test
    fun `FLE-9 duplicate parent email is rejected`() = runTest {
        // Given
        val provider = InMemoryLoginAuthProvider()
        val parent = SignUpData(
            email = "parent@example.com",
            password = "password-1234",
            displayName = "Parent User",
        )

        // When
        provider.signUp(parent)
        val duplicate = provider.signUp(parent)

        // Then
        assertIs<AuthResult.Failure>(duplicate)
    }
}
