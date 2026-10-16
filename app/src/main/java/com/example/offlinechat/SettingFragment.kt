package com.example.offlinechat

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {

    private lateinit var settingName: TextView
    private lateinit var darkModeValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        settingName = view.findViewById(R.id.settingName)
        darkModeValue = view.findViewById(R.id.darkModeValue)

        settingName.text = PrefsHelper.getName(requireContext())
        updateDarkLabel()

        view.findViewById<View>(R.id.cardName).setOnClickListener { showNameDialog() }
        view.findViewById<View>(R.id.rowDarkMode).setOnClickListener { toggleDarkMode() }

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

    private fun updateDarkLabel() {
        darkModeValue.text = when (PrefsHelper.getDarkMode(requireContext())) {
            -1 -> "Ikut Sistem"
            0 -> "Terang"
            else -> "Gelap"
        }
    }

    private fun toggleDarkMode() {
        val current = PrefsHelper.getDarkMode(requireContext())
        val next = when (current) {
            -1 -> 0
            0 -> 1
            else -> -1
        }
        PrefsHelper.setDarkMode(requireContext(), next)

        when (next) {
            -1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        val label = when (next) {
            -1 -> "Ikut Sistem"
            0 -> "Mode Terang"
            else -> "Mode Gelap"
        }
        Toast.makeText(requireContext(), label, Toast.LENGTH_SHORT).show()
        updateDarkLabel()
    }

    private fun showNameDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(PrefsHelper.getName(requireContext()))
            setSelection(text.length)
            setPadding(40, 30, 40, 30)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Nama Tampilan")
            .setMessage("Nama ini muncul saat orang lain menemukan HP kamu")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Gak boleh kosong", Toast.LENGTH_SHORT).show()
                } else {
                    PrefsHelper.setName(requireContext(), newName)
                    settingName.text = newName
                    Toast.makeText(requireContext(), "Tersimpan!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
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
                "Chat via WiFi Direct, Hotspot, atau Bluetooth — tanpa butuh internet sama sekali.\n\n" +
                "Dibuat dengan ❤ dari HP doang"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
