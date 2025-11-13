package com.helaluddin.noteghor.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.helaluddin.noteghor.data.utils.AuthResult
import com.helaluddin.noteghor.data.model.User // Assuming you have a User model
import com.helaluddin.noteghor.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // ----------------------------
    // LiveData to expose auth result
    // ----------------------------
    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> get() = _authResult

    // ----------------------------
    // Helper function to handle the common authentication flow (DRY principle)
    // ----------------------------
    private fun executeAuthCall(
        errorMessage: String,
        authCall: suspend () -> User?
    ) {
        // 1. Set loading state
        _authResult.value = AuthResult.Loading

        // 2. Launch coroutine in ViewModel scope
        viewModelScope.launch {
            try {
                // 3. Execute the specific authentication repository call
                val user = authCall()

                // 4. Check result and update LiveData
                if (user != null) {
                    _authResult.value = AuthResult.Success(user)
                } else {
                    _authResult.value = AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                // 5. Handle any exception
                _authResult.value = AuthResult.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    // ----------------------------
    // Sign in user with Google credentials
    // ----------------------------
    fun signInWithGoogle(credential: AuthCredential) {
        executeAuthCall(
            errorMessage = "Authentication failed",
            authCall = { repository.firebaseSignInWithCredential(credential) }
        )
    }

    // ----------------------------
    // Sign in user as a guest
    // ----------------------------
    fun signInGuest() {
        executeAuthCall(
            errorMessage = "Guest sign-in failed",
            authCall = { repository.signInAsGuest() }
        )
    }

    // ----------------------------
    // Get currently signed-in user
    // ----------------------------
    fun getCurrentUser() = repository.getCurrentUser()

    // ----------------------------
    // Logout the current user
    // ----------------------------
    fun logout() {
        repository.logout()
    }
}