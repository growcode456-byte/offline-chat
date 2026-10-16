package com.example.offlinechat

data class ChatPreview(
    val chatId: String,
    val title: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val isGroup: Boolean = false,
    val colorAccent: Int = 0xFF128C7E.toInt()
)
