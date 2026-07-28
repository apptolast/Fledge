package com.apptolast.fledge.data.remote.firebase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirebaseEnvironmentTest {

    @Test
    fun `FLE-78 firebase environment derives effective database id from build config`() {
        // Given
        val apiKey = "AIzaSyFledgeTestApiKey"
        val projectId = "fledge-c685d"
        val applicationId = "1:1234567890:android:0123456789abcdef"
        val gcmSenderId = "1234567890"
        val storageBucket = "fledge-c685d.appspot.com"

        // When
        val debugEnvironment = firebaseEnvironmentOf(
            apiKey = apiKey,
            projectId = projectId,
            applicationId = applicationId,
            gcmSenderId = gcmSenderId,
            storageBucket = storageBucket,
            databaseId = "debug",
        )

        // Then
        assertEquals(apiKey, debugEnvironment.apiKey)
        assertEquals(projectId, debugEnvironment.projectId)
        assertEquals(applicationId, debugEnvironment.applicationId)
        assertEquals(gcmSenderId, debugEnvironment.gcmSenderId)
        assertEquals(storageBucket, debugEnvironment.storageBucket)
        assertEquals("debug", debugEnvironment.databaseId)
        assertTrue(debugEnvironment.isComplete())

        // When the release database id is configured
        val releaseEnvironment = firebaseEnvironmentOf(
            apiKey = apiKey,
            projectId = projectId,
            applicationId = applicationId,
            gcmSenderId = gcmSenderId,
            storageBucket = storageBucket,
            databaseId = "(default)",
        )

        // Then it is used literally, without any transformation
        assertEquals("(default)", releaseEnvironment.databaseId)
        assertTrue(releaseEnvironment.isComplete())
    }
}
