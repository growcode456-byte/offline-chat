package com.example.offlinechat

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {

    private lateinit var settingName: TextView
    private lateinit var settingStatus: TextView
    private lateinit var settingAvatar: ImageView
    private lateinit var darkModeValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingName = view.findViewById(R.id.settingName)
        settingStatus = view.findViewById(R.id.settingStatus)
        settingAvatar = view.findViewById(R.id.settingAvatar)
        darkModeValue = view.findViewById(R.id.darkModeValue)

        view.findViewById<View>(R.id.cardName).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
        view.findViewById<View>(R.id.rowDarkMode).setOnClickListener { toggleDarkMode() }

        view.findViewById<View>(R.id.rowWallpaper).setOnClickListener {
            startActivity(Intent(requireContext(), WallpaperActivity::class.java))
        }

        view.findViewById<View>(R.id.rowTheme).setOnClickListener {
            startActivity(Intent(requireContext(), ThemeActivity::class.java))
        }

        view.findViewById<View>(R.id.rowTestChat).setOnClickListener {
            startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("loopback", true)
                putExtra("myName", PrefsHelper.getName(requireContext()))
            })
        }

        view.findViewById<View>(R.id.rowTestGroup).setOnClickListener {
            startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("group", true)
                putExtra("myName", PrefsHelper.getName(requireContext()))
            })
        }

        view.findViewById<View>(R.id.rowClearAll).setOnClickListener { confirmClearAll() }
        view.findViewById<View>(R.id.rowAbout).setOnClickListener { showAbout() }
    }

    override fun onResume() {
        super.onResume()
        refreshProfile()
    }

    private fun refreshProfile() {
        settingName.text = PrefsHelper.getName(requireContext())
        settingStatus.text = PrefsHelper.getStatus(requireContext())
        updateDarkLabel()

        val avatarBase64 = PrefsHelper.getAvatar(requireContext())
        if (avatarBase64 != null) {
            try {
                val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) settingAvatar.setImageBitmap(bmp)
            } catch (e: Exception) {}
        }
    }

    private fun updateDarkLabel() {
        darkModeValue.text = when (PrefsHelper.getDarkMode(requireContext())) {
            -1 -> "Ikut Sistem"
            0 -> "Terang"
            else -> "Gelap"
        }
    }

    private fun toggleDarkMode() {
        val current = PrefsHelper.getDarkMode(requireContext())
        val next = when (current) { -1 -> 0; 0 -> 1; else -> -1 }
        PrefsHelper.setDarkMode(requireContext(), next)

        when (next) {
            -1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        val label = when (next) {
            -1 -> "Ikut Sistem"; 0 -> "Mode Terang"; else -> "Mode Gelap"
        }
        Toast.makeText(requireContext(), label, Toast.LENGTH_SHORT).show()
        updateDarkLabel()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Semua Riwayat?")
            .setMessage("Semua chat dihapus. Gak bisa dibalikin.")
            .setPositiveButton("Hapus") { _, _ ->
                val db = AppDatabase.get(requireContext())
                lifecycleScope.launch {
                    try {
                        db.clearAllTables()
                        Toast.makeText(requireContext(), "Semua riwayat dihapus", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(requireContext())
            .setTitle("RIVO")
            .setMessage(
                "Offline Chat • Tanpa Internet\n\n" +
                "Versi 1.0\n\n" +
                "Chat via WiFi Direct, Hotspot, atau Bluetooth — tanpa butuh internet.\n\n" +
                "Dibuat dengan ❤ dari HP doang"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
