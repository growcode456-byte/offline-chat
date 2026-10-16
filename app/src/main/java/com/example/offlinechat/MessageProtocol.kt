package com.example.offlinechat

import org.json.JSONObject

data class ChatMessage(
    val from: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean = false,
    val imageData: String? = null,
    val senderColor: Int? = null  // untuk grup: warna nama pengirim
) {
    fun toJson(): String {
        val o = JSONObject()
        o.put("from", from)
        o.put("text", text)
        o.put("ts", timestamp)
        if (imageData != null) o.put("img", imageData)
        if (senderColor != null) o.put("color", senderColor)
        return o.toString()
    }

    companion object {
        fun fromJson(s: String): ChatMessage? {
            return try {
                val o = JSONObject(s)
                ChatMessage(
                    from = o.getString("from"),
                    text = o.getString("text"),
                    timestamp = o.getLong("ts"),
                    imageData = if (o.has("img")) o.getString("img") else null,
                    senderColor = if (o.has("color")) o.getInt("color") else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
