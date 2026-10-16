package com.example.offlinechat

import android.graphics.Color

object ThemeHelper {

    data class ChatTheme(
        val id: String,
        val name: String,
        val headerColor: Int,
        val bubbleMine: Int,
        val bubbleOther: Int,
        val accentColor: Int,
        val chatBg: Int
    )

    val THEMES = listOf(
        ChatTheme("hijau", "Hijau (WA)",
            Color.parseColor("#075E54"),
            Color.parseColor("#DCF8C6"),
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#25D366"),
            Color.parseColor("#ECE5DD")),

        ChatTheme("biru", "Biru (Telegram)",
            Color.parseColor("#1E3A5F"),
            Color.parseColor("#D0E8FF"),
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#4A90E2"),
            Color.parseColor("#E3F2FD")),

        ChatTheme("ungu", "Ungu",
            Color.parseColor("#4A148C"),
            Color.parseColor("#E1BEE7"),
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#F3E5F5")),

        ChatTheme("orange", "Orange",
            Color.parseColor("#BF360C"),
            Color.parseColor("#FFE0B2"),
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#FFF3E0")),

        ChatTheme("pink", "Pink",
            Color.parseColor("#880E4F"),
            Color.parseColor("#F8BBD0"),
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#FCE4EC")),

        ChatTheme("hitam", "Hitam Minimalis",
            Color.parseColor("#212121"),
            Color.parseColor("#424242"),
            Color.parseColor("#2C2C2C"),
            Color.parseColor("#757575"),
            Color.parseColor("#121212"))
    )

    fun get(id: String): ChatTheme =
        THEMES.firstOrNull { it.id == id } ?: THEMES[0]

    fun current(context: android.content.Context): ChatTheme =
        get(PrefsHelper.getTheme(context))
}
