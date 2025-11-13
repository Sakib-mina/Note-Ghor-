package com.helaluddin.noteghor.data.utils

import com.helaluddin.noteghor.data.model.User

sealed class AuthResult {

    /**
     * Represents a successful authentication.
     *
     * @property user The authenticated user returned from Firebase or other auth service.
     */
    data class Success(val user: User) : AuthResult()

    /**
     * Represents a failed authentication attempt.
     *
     * @property message A human-readable error message describing why authentication failed.
     */
    data class Error(val message: String) : AuthResult()

    /**
     * Represents an ongoing authentication operation.
     * Can be used to display loading indicators in the UI.
     */
    object Loading : AuthResult()
}