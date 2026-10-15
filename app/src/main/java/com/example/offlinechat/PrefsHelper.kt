package com.example.offlinechat

import android.content.Context

object PrefsHelper {
    private const val PREFS = "offlinechat_prefs"
    private const val KEY_NAME = "display_name"

    fun getName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, "Saya") ?: "Saya"
    }

    fun setName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NAME, name).apply()
    }
}
