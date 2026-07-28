package com.apptolast.fledge.data.remote.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

/**
 * Thin adapter over the GitLive SDK.
 *
 * [firestore] is deliberately a concrete member and not part of [FirestoreProvider]: keeping
 * `FirebaseFirestore` out of the port is what allows commonTest to write fakes without importing
 * `dev.gitlive.*`.
 */
class GitLiveFirestoreProvider(private val bootstrap: FirebaseBootstrap, override val databaseId: String) :
    FirestoreProvider {

    override val isAvailable: Boolean
        get() = bootstrap.state.isReady

    /**
     * Resolves the Firestore handle lazily: constructing this provider must never touch Firebase,
     * because Koin can build it before the bootstrap has run.
     */
    fun firestore(): FirebaseFirestore = Firebase.firestore(Firebase.app, databaseId)
}
