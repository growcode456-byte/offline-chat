package com.example.offlinechat

import android.content.Context

object PrefsHelper {
    private const val PREFS = "offlinechat_prefs"
    private const val KEY_NAME = "display_name"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_STATUS = "status_text"
    private const val KEY_AVATAR = "avatar_base64"
    private const val KEY_WALLPAPER = "chat_wallpaper"
    private const val KEY_THEME = "chat_theme"
    private const val KEY_WP_BRIGHTNESS = "wp_brightness"

    fun getDarkMode(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_DARK_MODE, -1)

    fun setDarkMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DARK_MODE, mode).apply()
    }

    fun getName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "Saya") ?: "Saya"

    fun setName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAME, name).apply()
    }

    fun getStatus(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STATUS, "Tersedia") ?: "Tersedia"

    fun setStatus(context: Context, status: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_STATUS, status).apply()
    }

    fun getAvatar(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_AVATAR, null)

    fun setAvatar(context: Context, base64: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_AVATAR, base64).apply()
    }

    fun getWallpaper(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_WALLPAPER, null)

    fun setWallpaper(context: Context, base64: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_WALLPAPER, base64).apply()
    }

    fun getTheme(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "hijau") ?: "hijau"

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme).apply()
    }

    // Brightness 0..100, default 50 (agak gelap)
    fun getWpBrightness(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_WP_BRIGHTNESS, 50)

    fun setWpBrightness(context: Context, value: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WP_BRIGHTNESS, value).apply()
    }
}
