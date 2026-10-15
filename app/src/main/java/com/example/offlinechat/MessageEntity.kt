package com.example.offlinechat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val from: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val chatId: String
) {
    fun toChatMessage(): ChatMessage = ChatMessage(from, text, timestamp, isMine)
}

fun ChatMessage.toEntity(chatId: String): MessageEntity = MessageEntity(
    from = from,
    text = text,
    timestamp = timestamp,
    isMine = isMine,
    chatId = chatId
)
