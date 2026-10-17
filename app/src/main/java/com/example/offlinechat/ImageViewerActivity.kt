package com.example.offlinechat

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast

class ImageViewerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        // Fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val imageData = intent.getStringExtra("imageData")
        val imageView = findViewById<ImageView>(R.id.fullImage)

        if (imageData != null) {
            try {
                val bytes = Base64.decode(imageData, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) imageView.setImageBitmap(bmp)
            } catch (e: Exception) {}
        }

        findViewById<View>(R.id.btnCloseViewer).setOnClickListener { finish() }

        findViewById<View>(R.id.btnDownloadViewer).setOnClickListener {
            if (imageData != null) {
                ImageSaver.saveBase64ToGallery(this, imageData)
            } else {
                Toast.makeText(this, "Gambar gak ada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
