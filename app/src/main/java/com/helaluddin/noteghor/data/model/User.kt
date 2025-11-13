package com.helaluddin.noteghor.data.model

data class User(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val coins: Int = 0,
    val isFirstTime: Boolean = true
)

