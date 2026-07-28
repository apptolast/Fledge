package com.apptolast.fledge.data.remote.firebase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FirebaseBootstrapTest {

    @Test
    fun `FLE-78 incomplete firebase config skips initialization without crashing`() {
        // Given a developer machine without local.properties: the api key is empty
        val environment = completeEnvironment().copy(apiKey = "")
        val initializer = FakeFirebaseInitializer(initialized = false)
        val bootstrap = FirebaseBootstrap(environment, initializer)

        // When
        val state = bootstrap.run()

        // Then
        assertIs<FirebaseBootstrapState.NotConfigured>(state)
        assertIs<FirebaseBootstrapState.NotConfigured>(bootstrap.state)
        assertEquals(0, initializer.initializeCalls)
    }

    @Test
    fun `FLE-78 firebase bootstrap initializes only once`() {
        // Given
        val environment = completeEnvironment()
        val initializer = FakeFirebaseInitializer(initialized = false)
        val bootstrap = FirebaseBootstrap(environment, initializer)

        // When
        val firstState = bootstrap.run()
        val secondState = bootstrap.run()

        // Then
        assertEquals(1, initializer.initializeCalls)
        assertEquals(environment, initializer.lastEnvironment)
        assertIs<FirebaseBootstrapState.Initialized>(firstState)
        assertIs<FirebaseBootstrapState.Initialized>(secondState)
        assertIs<FirebaseBootstrapState.Initialized>(bootstrap.state)
    }

    @Test
    fun `FLE-78 firebase bootstrap skips when app already exists`() {
        // Given a process where the Firebase app was already configured
        val initializer = FakeFirebaseInitializer(initialized = true)
        val bootstrap = FirebaseBootstrap(completeEnvironment(), initializer)

        // When
        val state = bootstrap.run()

        // Then
        assertIs<FirebaseBootstrapState.AlreadyInitialized>(state)
        assertIs<FirebaseBootstrapState.AlreadyInitialized>(bootstrap.state)
        assertEquals(0, initializer.initializeCalls)
    }
}

internal fun completeEnvironment(): FirebaseEnvironment = FirebaseEnvironment(
    apiKey = "AIzaSyFledgeTestApiKey",
    projectId = "fledge-c685d",
    applicationId = "1:1234567890:android:0123456789abcdef",
    gcmSenderId = "1234567890",
    storageBucket = "fledge-c685d.appspot.com",
    databaseId = "debug",
)

/** Hand-written fake: counts initialize calls and lets the test decide the pre-existing app state. */
internal class FakeFirebaseInitializer(private var initialized: Boolean = false) : FirebaseInitializer {

    var initializeCalls: Int = 0
        private set

    var lastEnvironment: FirebaseEnvironment? = null
        private set

    override fun isInitialized(): Boolean = initialized

    override fun initialize(environment: FirebaseEnvironment) {
        initializeCalls += 1
        lastEnvironment = environment
        initialized = true
    }
}
