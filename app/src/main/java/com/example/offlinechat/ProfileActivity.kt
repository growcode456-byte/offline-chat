package com.example.offlinechat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileActivity : BaseActivity() {

    private lateinit var profileAvatar: ImageView
    private lateinit var inputName: EditText
    private lateinit var inputStatus: EditText
    private lateinit var chipGroup: ChipGroup

    private var pendingAvatarBase64: String? = null

    private val pickAvatar = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) loadAvatar(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileAvatar = findViewById(R.id.profileAvatar)
        inputName = findViewById(R.id.inputName)
        inputStatus = findViewById(R.id.inputStatus)
        chipGroup = findViewById(R.id.chipGroup)

        // Load data existing
        inputName.setText(PrefsHelper.getName(this))
        inputStatus.setText(PrefsHelper.getStatus(this))
        pendingAvatarBase64 = PrefsHelper.getAvatar(this)
        applyAvatarFromBase64(pendingAvatarBase64)

        // Quick status chips
        val presets = listOf(
            "Tersedia", "Sibuk", "Di sekolah", "Di rumah",
            "Ngopi dulu ☕", "Tidur 😴", "Belajar 📚", "Gaming 🎮",
            "Kerja 💼", "Jajan 🍔"
        )
        presets.forEach { text ->
            val chip = Chip(this).apply {
                this.text = text
                isCheckable = true
                setOnClickListener {
                    inputStatus.setText(text)
                }
            }
            chipGroup.addView(chip)
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnPickAvatar).setOnClickListener {
            pickAvatar.launch("image/*")
        }
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener { saveProfile() }
    }

    private fun loadAvatar(uri: Uri) {
        lifecycleScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    val inputStream: InputStream = contentResolver.openInputStream(uri)
                        ?: return@withContext null
                    val original = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (original == null) return@withContext null

                    // Square crop
                    val size = minOf(original.width, original.height)
                    val xOff = (original.width - size) / 2
                    val yOff = (original.height - size) / 2
                    val cropped = Bitmap.createBitmap(original, xOff, yOff, size, size)

                    // Resize 400x400
                    val resized = Bitmap.createScaledBitmap(cropped, 400, 400, true)

                    val baos = ByteArrayOutputStream()
                    resized.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                }

                if (base64 == null) {
                    Toast.makeText(this@ProfileActivity, "Gagal baca gambar", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                pendingAvatarBase64 = base64
                applyAvatarFromBase64(base64)
                Toast.makeText(this@ProfileActivity, "Foto dipilih, tap Simpan", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyAvatarFromBase64(base64: String?) {
        if (base64 == null) {
            profileAvatar.setImageResource(R.drawable.splash_logo)
            return
        }
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) profileAvatar.setImageBitmap(bmp)
            else profileAvatar.setImageResource(R.drawable.splash_logo)
        } catch (e: Exception) {
            profileAvatar.setImageResource(R.drawable.splash_logo)
        }
    }

    private fun saveProfile() {
        val name = inputName.text.toString().trim()
        val status = inputStatus.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Nama gak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        PrefsHelper.setName(this, name)
        PrefsHelper.setStatus(this, status.ifEmpty { "Tersedia" })
        PrefsHelper.setAvatar(this, pendingAvatarBase64)

        Toast.makeText(this, "Profil tersimpan!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
