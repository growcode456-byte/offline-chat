package com.example.offlinechat

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "Hello OfflineChat!\n\nFase 0: Pipeline OK!"
            textSize = 22f
            setPadding(60, 300, 60, 60)
        }
        setContentView(tv)
    }
}
