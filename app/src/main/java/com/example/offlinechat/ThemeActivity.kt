package com.example.offlinechat

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class ThemeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.themeContainer)
        val current = PrefsHelper.getTheme(this)

        ThemeHelper.THEMES.forEach { theme ->
            val row = layoutInflater.inflate(R.layout.item_theme, container, false)
            val colorView = row.findViewById<ImageView>(R.id.themeColor)
            val nameView = row.findViewById<TextView>(R.id.themeName)
            val checkView = row.findViewById<View>(R.id.themeCheck)

            val gradient = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(theme.headerColor, theme.accentColor)
            )
            gradient.cornerRadius = 24f
            colorView.background = gradient
            nameView.text = theme.name
            checkView.visibility = if (theme.id == current) View.VISIBLE else View.GONE

            row.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Ganti Tema?")
                    .setMessage("Ganti ke tema ${theme.name}?")
                    .setPositiveButton("Ya") { _, _ ->
                        PrefsHelper.setTheme(this, theme.id)
                        Toast.makeText(this, "Tema ${theme.name} dipakai", Toast.LENGTH_SHORT).show()
                        recreate()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            container.addView(row)
        }
    }
}
