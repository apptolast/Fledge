package com.apptolast.fledge.data

import com.apptolast.customlogin.domain.model.AuthError
import com.apptolast.customlogin.domain.model.AuthResult
import com.apptolast.customlogin.domain.model.Credentials
import com.apptolast.customlogin.domain.model.IdentityProvider
import com.apptolast.customlogin.domain.model.SignUpData
import com.apptolast.fledge.data.auth.FledgeFirebaseAuthProvider
import com.apptolast.fledge.data.auth.SocialAuthClient
import com.apptolast.fledge.data.auth.SocialAuthUnavailableException
import com.apptolast.fledge.data.auth.SocialSignInResult
import com.apptolast.fledge.data.auth.TokenManager
import com.apptolast.fledge.data.remote.firebase.FirebaseAuthService
import com.apptolast.fledge.testing.TestSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class FledgeFirebaseAuthProviderTest {

    @Test
    fun `FLE-8 registration creates a Firebase parent session`() = runTest {
        // Given
        val provider = providerWith(
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/accounts:signUp" -> jsonResponse(signInJson(localId = "parent-1"))
                    "/v1/accounts:update" -> jsonResponse("{}")
                    else -> error("Unexpected request: ${request.url}")
                }
            }
        )

        // When
        val result = provider.signUp(
            SignUpData(
                email = "parent@example.com",
                password = "password-1234",
                displayName = "Parent User",
            )
        )

        // Then
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("parent-1", success.session.userId)
        assertEquals("parent@example.com", success.session.email)
        assertEquals("Parent User", success.session.displayName)
        assertEquals("password", success.session.providerId)
    }

    @Test
    fun `FLE-8 sign in stores Firebase tokens`() = runTest {
        // Given
        val provider = providerWith(
            MockEngine { request ->
                assertEquals("/v1/accounts:signInWithPassword", request.url.encodedPath)
                jsonResponse(signInJson(localId = "parent-2", refreshToken = "refresh-login"))
            }
        )

        // When
        val result = provider.signIn(Credentials.EmailPassword("parent@example.com", "password-1234"))

        // Then
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("id-token", success.session.accessToken)
        assertEquals("refresh-login", success.session.refreshToken)
    }

    @Test
    fun `FLE-8 refresh token renews the current session`() = runTest {
        // Given
        val provider = providerWith(
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/accounts:signInWithPassword" -> jsonResponse(signInJson(refreshToken = "refresh-one"))
                    "/v1/token" -> jsonResponse(refreshJson(idToken = "id-token-2", refreshToken = "refresh-two"))
                    else -> error("Unexpected request: ${request.url}")
                }
            }
        )
        provider.signIn(Credentials.EmailPassword("parent@example.com", "password-1234"))

        // When
        val result = provider.signIn(Credentials.RefreshToken("refresh-one"))

        // Then
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("id-token-2", success.session.accessToken)
        assertEquals("refresh-two", success.session.refreshToken)
    }

    @Test
    fun `FLE-8 password reset maps Firebase reset endpoints`() = runTest {
        // Given
        val requests = mutableListOf<String>()
        val provider = providerWith(
            MockEngine { request ->
                requests += request.url.encodedPath
                jsonResponse("{}")
            }
        )

        // When
        val sent = provider.sendPasswordResetEmail("parent@example.com")
        val confirmed = provider.confirmPasswordReset("oob-code", "new-password")

        // Then
        assertEquals(AuthResult.PasswordResetSent, sent)
        assertEquals(AuthResult.PasswordResetSuccess, confirmed)
        assertEquals(listOf("/v1/accounts:sendOobCode", "/v1/accounts:resetPassword"), requests)
    }

    @Test
    fun `FLE-8 Firebase errors map to BaseLogin auth errors`() = runTest {
        // Given
        val provider = providerWith(
            MockEngine {
                jsonResponse(
                    content = """{"error":{"code":400,"message":"EMAIL_EXISTS"}}""",
                    status = HttpStatusCode.BadRequest,
                )
            }
        )

        // When
        val result = provider.signUp(SignUpData("parent@example.com", "password-1234"))

        // Then
        val failure = assertIs<AuthResult.Failure>(result)
        assertIs<AuthError.EmailAlreadyInUse>(failure.error)
    }

    @Test
    fun `FLE-8 Google social auth exchanges the Android id token`() = runTest {
        // Given
        val provider = providerWith(
            engine = MockEngine { request ->
                assertEquals("/v1/accounts:signInWithIdp", request.url.encodedPath)
                jsonResponse(signInJson(localId = "google-user", displayName = "Google Parent"))
            },
            socialAuthClient = FakeSocialAuthClient(
                google = SocialSignInResult(
                    providerId = "google.com",
                    idToken = "google-id-token",
                    displayName = "Google Parent",
                )
            ),
        )

        // When
        val result = provider.signIn(Credentials.OAuthToken(IdentityProvider.Google))

        // Then
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("google.com", success.session.providerId)
        assertEquals("Google Parent", success.session.displayName)
    }

    @Test
    fun `FLE-8 Apple social auth exchanges the iOS id token and raw nonce`() = runTest {
        // Given
        val provider = providerWith(
            engine = MockEngine { request ->
                assertEquals("/v1/accounts:signInWithIdp", request.url.encodedPath)
                jsonResponse(signInJson(localId = "apple-user", displayName = "Apple Parent"))
            },
            socialAuthClient = FakeSocialAuthClient(
                apple = SocialSignInResult(
                    providerId = "apple.com",
                    idToken = "apple-id-token",
                    rawNonce = "raw-nonce",
                    displayName = "Apple Parent",
                )
            ),
        )

        // When
        val result = provider.signIn(Credentials.OAuthToken(IdentityProvider.Apple))

        // Then
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("apple.com", success.session.providerId)
        assertEquals("Apple Parent", success.session.displayName)
    }

    @Test
    fun `FLE-8 unsupported social provider fails without network call`() = runTest {
        // Given
        val provider = providerWith(MockEngine { error("Network should not be called") })

        // When
        val result = provider.signIn(Credentials.OAuthToken(IdentityProvider.Facebook))

        // Then
        val failure = assertIs<AuthResult.Failure>(result)
        assertIs<AuthError.OperationNotAllowed>(failure.error)
    }

    private fun providerWith(
        engine: MockEngine,
        socialAuthClient: SocialAuthClient = FakeSocialAuthClient(),
    ): FledgeFirebaseAuthProvider {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val client = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(json)
            }
        }
        val tokenManager = TokenManager(TestSettings())
        return FledgeFirebaseAuthProvider(
            authService = FirebaseAuthService(client, json),
            tokenManager = tokenManager,
            socialAuthClient = socialAuthClient,
        )
    }

    private fun MockRequestHandleScope.jsonResponse(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private fun signInJson(
        localId: String = "parent-1",
        email: String = "parent@example.com",
        idToken: String = "id-token",
        refreshToken: String = "refresh-token",
        displayName: String = "",
    ): String =
        """
        {
          "idToken": "$idToken",
          "refreshToken": "$refreshToken",
          "expiresIn": "3600",
          "localId": "$localId",
          "email": "$email",
          "displayName": "$displayName"
        }
        """.trimIndent()

    private fun refreshJson(
        idToken: String,
        refreshToken: String,
        userId: String = "parent-1",
    ): String =
        """
        {
          "id_token": "$idToken",
          "refresh_token": "$refreshToken",
          "expires_in": "3600",
          "user_id": "$userId"
        }
        """.trimIndent()
}

private class FakeSocialAuthClient(
    private val google: SocialSignInResult? = null,
    private val apple: SocialSignInResult? = null,
) : SocialAuthClient {
    override val isGoogleAvailable: Boolean = google != null
    override val isAppleAvailable: Boolean = apple != null

    override suspend fun signInWithGoogle(): SocialSignInResult =
        google ?: throw SocialAuthUnavailableException("Google no esta disponible.")

    override suspend fun signInWithApple(): SocialSignInResult =
        apple ?: throw SocialAuthUnavailableException("Apple no esta disponible.")
}
