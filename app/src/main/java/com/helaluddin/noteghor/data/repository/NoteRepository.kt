package com.helaluddin.noteghor.data.repository

import com.helaluddin.noteghor.data.model.Note
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.helaluddin.noteghor.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NoteRepository {
    private val db = FirebaseFirestore.getInstance()

    // Use separate listener references
    private var notesListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    suspend fun addOrUpdateNote(note: Note, userId: String) {
        val collection = db.collection("users").document(userId).collection("notes")
        val docRef = if (note.id.isEmpty()) collection.document() else collection.document(note.id)
        val noteWithId = if (note.id.isEmpty()) note.copy(id = docRef.id) else note
        docRef.set(noteWithId.copy(userId = userId)).await()
    }

    suspend fun deleteNote(userId: String, noteId: String) {
        db.collection("users").document(userId).collection("notes")
            .document(noteId).delete().await()
    }

    fun getNotesStreamByUser(userId: String) = callbackFlow<List<Note>> {
        val collection = db.collection("users").document(userId).collection("notes")
        notesListener?.remove()
        notesListener = collection.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                trySend(snapshot.toObjects(Note::class.java))
            }
        awaitClose { notesListener?.remove() }
    }

    suspend fun getUserData(userId: String): User? {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun updateUserCoins(userId: String, newCoinValue: Int) {
        try {
            db.collection("users").document(userId).update("coins", newCoinValue).await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getUserDataFlow(userId: String) = callbackFlow<User?> {
        val userRef = db.collection("users").document(userId)
        userListener?.remove()
        userListener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            trySend(snapshot.toObject(User::class.java))
        }
        awaitClose { userListener?.remove() }
    }

    suspend fun updateUserFirstTimeFlag(userId: String) {
        db.collection("users").document(userId)
            .update("isFirstTime", false)
            .await()
    }
}
