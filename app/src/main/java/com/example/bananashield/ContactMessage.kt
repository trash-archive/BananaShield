package com.example.bananashield

data class ContactMessage(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "unread", // "unread", "read", "responded"
    val deviceInfo: String = "",
    val appVersion: String = "1.0.0"
)
