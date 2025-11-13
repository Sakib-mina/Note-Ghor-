package com.helaluddin.noteghor.data.model

import java.io.Serializable

data class Note(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isLocked: Boolean = false,
    val isPined: Boolean = false,
    val password: String? = null
) : Serializable
