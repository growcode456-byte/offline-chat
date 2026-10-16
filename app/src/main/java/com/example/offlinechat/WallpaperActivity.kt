package com.example.offlinechat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class WallpaperActivity : BaseActivity() {

    private lateinit var previewImage: ImageView
    private lateinit var overlayDim: View
    private lateinit var brightnessSeek: SeekBar

    private val pickWallpaper = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) loadWallpaper(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper)

        previewImage = findViewById(R.id.wallpaperPreview)
        overlayDim = findViewById(R.id.overlayDim)
        brightnessSeek = findViewById(R.id.brightnessSeek)

        // Load brightness existing
        val brightness = PrefsHelper.getWpBrightness(this)
        brightnessSeek.progress = brightness
        applyBrightness(brightness)

        brightnessSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyBrightness(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnPickWallpaper).setOnClickListener {
            pickWallpaper.launch("image/*")
        }
        findViewById<View>(R.id.btnResetWallpaper).setOnClickListener {
            PrefsHelper.setWallpaper(this, null)
            Toast.makeText(this, "Wallpaper dihapus", Toast.LENGTH_SHORT).show()
            previewImage.setImageResource(R.drawable.wallpaper_default)
        }
        findViewById<View>(R.id.btnSaveWallpaper).setOnClickListener {
            PrefsHelper.setWpBrightness(this, brightnessSeek.progress)
            Toast.makeText(this, "Wallpaper tersimpan!", Toast.LENGTH_SHORT).show()
            finish()
        }

        val existing = PrefsHelper.getWallpaper(this)
        if (existing != null) applyWallpaper(existing)
        else previewImage.setImageResource(R.drawable.wallpaper_default)
    }

    private fun applyBrightness(progress: Int) {
        // progress 0..100
        // 0 = terang, 100 = gelap
        val alpha = (progress * 2.55).toInt().coerceIn(0, 255)
        overlayDim.setBackgroundColor((alpha shl 24) or 0x000000)
    }

    private fun loadWallpaper(uri: Uri) {
        lifecycleScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    val inputStream: InputStream = contentResolver.openInputStream(uri)
                        ?: return@withContext null
                    val original = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (original == null) return@withContext null

                    val maxW = 1080
                    val bmp = if (original.width > maxW) {
                        val ratio = original.height.toFloat() / original.width.toFloat()
                        val newH = (maxW * ratio).toInt()
                        Bitmap.createScaledBitmap(original, maxW, newH, true)
                    } else original

                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                }

                if (base64 == null) {
                    Toast.makeText(this@WallpaperActivity, "Gagal baca gambar", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                PrefsHelper.setWallpaper(this@WallpaperActivity, base64)
                applyWallpaper(base64)
                Toast.makeText(this@WallpaperActivity, "Wallpaper dipilih, atur brightness + Simpan", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@WallpaperActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyWallpaper(base64: String) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) previewImage.setImageBitmap(bmp)
        } catch (e: Exception) {
            previewImage.setImageResource(R.drawable.wallpaper_default)
        }
    }
}
