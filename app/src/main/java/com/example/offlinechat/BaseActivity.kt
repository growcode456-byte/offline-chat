package com.example.offlinechat

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyDarkMode()
        super.onCreate(savedInstanceState)
        applyStatusBar()
    }

    private fun applyDarkMode() {
        when (PrefsHelper.getDarkMode(this)) {
            -1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun applyStatusBar() {
        val isDark = PrefsHelper.getDarkMode(this) == 1

        // Warna status bar
        val color = if (isDark) Color.parseColor("#0B141A")
                    else Color.parseColor("#FFFFFF")

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color
        window.navigationBarColor = color

        // Icon status bar terang/gelap
        val lightIcons = !isDark
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightIcons
            isAppearanceLightNavigationBars = lightIcons
        }
    }
}
