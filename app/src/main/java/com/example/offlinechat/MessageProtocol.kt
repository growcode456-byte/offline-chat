package com.example.offlinechat

import org.json.JSONObject

data class ChatMessage(
    val from: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean = false
) {
    fun toJson(): String = JSONObject().apply {
        put("from", from); put("text", text); put("ts", timestamp)
    }.toString()

    companion object {
        fun fromJson(s: String): ChatMessage? = try {
            val o = JSONObject(s)
            ChatMessage(o.getString("from"), o.getString("text"), o.getLong("ts"))
        } catch (e: Exception) { null }
    }
}
