package com.helaluddin.noteghor.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helaluddin.noteghor.data.model.Note
import com.helaluddin.noteghor.data.model.User
import com.helaluddin.noteghor.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NoteViewModel(
    private val repository: NoteRepository = NoteRepository()
) : ViewModel() {

    private val _userNotes = MutableStateFlow<List<Note>>(emptyList())
    val userNotes: StateFlow<List<Note>> = _userNotes

    fun addOrUpdateNote(note: Note, userId: String) {
        viewModelScope.launch {
            repository.addOrUpdateNote(note, userId)
        }
    }

    fun deleteNote(note: Note, userId: String) =
        viewModelScope.launch { repository.deleteNote(userId, note.id) }

    fun updateUserCoins(userId: String, newCoinValue: Int) =
        viewModelScope.launch { repository.updateUserCoins(userId, newCoinValue) }

    suspend fun getUserData(userId: String): User? = repository.getUserData(userId)
    fun getUserDataFlow(userId: String) = repository.getUserDataFlow(userId)

    fun getNotesFlow(userId: String) = repository.getNotesStreamByUser(userId)

}
