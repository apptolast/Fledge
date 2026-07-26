package com.apptolast.fledge.data.remote.firebase

import com.apptolast.fledge.shared.BuildKonfig

object FirebaseConfig {
    val apiKey: String = BuildKonfig.FIREBASE_API_KEY
    val projectId: String = BuildKonfig.FIREBASE_PROJECT_ID
    var databaseId: String = BuildKonfig.FIRESTORE_DATABASE_ID

    const val IDENTITY_TOOLKIT = "https://identitytoolkit.googleapis.com/v1"
    const val SECURE_TOKEN = "https://securetoken.googleapis.com/v1"

    val firestoreDocuments: String
        get() = "https://firestore.googleapis.com/v1/projects/$projectId/databases/$databaseId/documents"
}
