package com.example.offlinechat

import android.content.Context

object PrefsHelper {
    private const val PREFS = "offlinechat_prefs"
    private const val KEY_NAME = "display_name"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_STATUS = "status_text"
    private const val KEY_AVATAR = "avatar_base64"

    fun getDarkMode(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_DARK_MODE, -1)
    }

    fun setDarkMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DARK_MODE, mode).apply()
    }

    fun getName(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "Saya") ?: "Saya"
    }

    fun setName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAME, name).apply()
    }

    fun getStatus(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STATUS, "Tersedia") ?: "Tersedia"
    }

    fun setStatus(context: Context, status: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_STATUS, status).apply()
    }

    fun getAvatar(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_AVATAR, null)
    }

    fun setAvatar(context: Context, base64: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_AVATAR, base64).apply()
    }
}
