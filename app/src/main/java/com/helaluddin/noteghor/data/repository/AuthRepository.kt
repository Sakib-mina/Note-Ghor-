package com.helaluddin.noteghor.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.helaluddin.noteghor.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,          // Firebase Authentication instance
    private val firestore: FirebaseFirestore // Firestore database instance
) {

    // ----------------------------
    // Sign in user using Firebase AuthCredential (Google, Facebook, etc.)
    // Returns User object if successful, null otherwise
    // ----------------------------
    suspend fun firebaseSignInWithCredential(credential: AuthCredential): User? {
        // Sign in with Firebase using the credential
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user ?: return null

        // Delegate the common logic to the private function
        return processAndSaveUser(firebaseUser)
    }

    // ----------------------------
    // Sign in user anonymously (Guest)
    // Returns User object if successful, null otherwise
    // ----------------------------

    suspend fun signInAsGuest(): User? {
        // Sign in anonymously using Firebase
        val authResult = auth.signInAnonymously().await()
        val firebaseUser = authResult.user ?: return null

        // Delegate the common logic to the private function
        return processAndSaveUser(firebaseUser, isGuest = true)
    }

    // ----------------------------
    // Common function to map FirebaseUser to User model and save to Firestore
    // ----------------------------
    private suspend fun processAndSaveUser(
        firebaseUser: FirebaseUser,
        isGuest: Boolean = false
    ): User {
        // 1. Map FirebaseUser to app-specific User model
        val user = User(
            uid = firebaseUser.uid,
            displayName = if (isGuest) "Guest ${firebaseUser.uid.take(4)}" else firebaseUser.displayName,
            email = if (isGuest) null else firebaseUser.email
        )

        // 2. Save or update user in Firestore "users" collection
        firestore.collection("users")
            .document(user.uid)
            .set(user)
            .await()

        return user
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun logout() {
        auth.signOut()
    }
}