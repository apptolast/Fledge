package com.apptolast.fledge.data.remote.firebase

/**
 * Port exposing only what common code needs to observe about Firestore.
 *
 * The real `FirebaseFirestore` handle lives as a concrete member of the GitLive adapter, outside this
 * interface, so no fake in commonTest is ever forced to import `dev.gitlive.*`.
 */
interface FirestoreProvider {

    /** Effective Firestore database, e.g. "debug" or "(default)". */
    val databaseId: String

    /** True when the Firebase app is ready and Firestore can be resolved. */
    val isAvailable: Boolean
}
