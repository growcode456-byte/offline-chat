package com.example.offlinechat

import android.content.Context

object PrefsHelper {
    private const val PREFS = "offlinechat_prefs"
    private const val KEY_NAME = "display_name"
    private const val KEY_DARK_MODE = "dark_mode"

    // -1 = follow system, 0 = light, 1 = dark
    fun getDarkMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DARK_MODE, -1)
    }

    fun setDarkMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DARK_MODE, mode).apply()
    }

    fun getName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, "Saya") ?: "Saya"
    }

    fun setName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NAME, name).apply()
    }
}
