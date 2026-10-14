package com.example.offlinechat

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiDirectManager
    private lateinit var adapter: PeerAdapter
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val btnScan = findViewById<Button>(R.id.btnScan)
        val list = findViewById<RecyclerView>(R.id.peerList)

        adapter = PeerAdapter { device ->
            Toast.makeText(this, "Tap: ${device.deviceName}", Toast.LENGTH_SHORT).show()
        }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        wifiManager = WifiDirectManager(this).apply {
            onPeersChanged = { devices -> adapter.update(devices) }
            onStatusChanged = { msg -> runOnUiThread { statusText.text = msg } }
        }

        if (!wifiManager.init()) {
            statusText.text = "WiFi Direct gak didukung"
            return
        }

        btnScan.setOnClickListener {
            if (PermissionHelper.hasAll(this)) wifiManager.discoverPeers()
            else PermissionHelper.request(this)
        }

        if (!PermissionHelper.hasAll(this)) PermissionHelper.request(this)
    }

    override fun onResume() {
        super.onResume()
        wifiManager.registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        wifiManager.unregisterReceiver()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQ_CODE) {
            val ok = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            statusText.text = if (ok) "Izin OK, tap Cari Perangkat" else "Izin ditolak"
        }
    }
}
