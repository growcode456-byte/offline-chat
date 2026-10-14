package com.example.offlinechat

import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiDirectManager
    private lateinit var adapter: PeerAdapter
    private lateinit var statusText: TextView
    private lateinit var btnScan: Button
    private lateinit var btnOpenChat: Button
    private lateinit var btnDisconnect: Button
    private lateinit var btnTestChat: Button

    private var isConnected = false
    private var isGroupOwner = false
    private var connectedPeerName: String? = null
    private var myDeviceName: String = "Saya"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnScan = findViewById(R.id.btnScan)
        btnOpenChat = findViewById(R.id.btnOpenChat)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        btnTestChat = findViewById(R.id.btnTestChat)
        val list = findViewById<RecyclerView>(R.id.peerList)

        adapter = PeerAdapter { device ->
            if (isConnected) Toast.makeText(this, "Sudah terhubung!", Toast.LENGTH_SHORT).show()
            else showConnectDialog(device)
        }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        wifiManager = WifiDirectManager(this).apply {
            onPeersChanged = { devices -> runOnUiThread { adapter.update(devices) } }
            onStatusChanged = { msg -> runOnUiThread { statusText.text = msg } }
            onThisDeviceChanged = { dev -> runOnUiThread { myDeviceName = dev.deviceName ?: "Saya" } }
            onConnectionInfo = { info -> runOnUiThread { handleConnectionInfo(info) } }
        }

        if (!wifiManager.init()) {
            statusText.text = "WiFi Direct gak didukung"
        }

        btnScan.setOnClickListener {
            if (PermissionHelper.hasAll(this)) wifiManager.discoverPeers()
            else PermissionHelper.request(this)
        }

        btnOpenChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("isGroupOwner", isGroupOwner)
                putExtra("myName", myDeviceName)
                putExtra("peerName", connectedPeerName ?: "Peer")
            }
            startActivity(intent)
        }

        btnTestChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("loopback", true)
                putExtra("myName", myDeviceName)
            }
            startActivity(intent)
        }

        btnDisconnect.setOnClickListener {
            wifiManager.disconnect()
            isConnected = false
            connectedPeerName = null
            btnDisconnect.visibility = Button.GONE
            btnOpenChat.visibility = Button.GONE
            statusText.text = "Terputus"
        }

        if (!PermissionHelper.hasAll(this)) PermissionHelper.request(this)
    }

    private fun showConnectDialog(device: WifiP2pDevice) {
        AlertDialog.Builder(this)
            .setTitle("Hubungkan?")
            .setMessage("Connect ke ${device.deviceName ?: "perangkat"} ?")
            .setPositiveButton("Hubungkan") { _, _ -> wifiManager.connect(device) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        if (info.groupFormed) {
            isConnected = true
            isGroupOwner = info.isGroupOwner
            val role = if (isGroupOwner) "Group Owner (Server)" else "Client"
            statusText.text = "✅ Terhubung sebagai $role\nIP: ${info.groupOwnerAddress?.hostAddress}"
            connectedPeerName = if (isGroupOwner) "Client" else "Server"
            btnDisconnect.visibility = Button.VISIBLE
            btnOpenChat.visibility = Button.VISIBLE
        } else {
            isConnected = false
            connectedPeerName = null
            btnDisconnect.visibility = Button.GONE
            btnOpenChat.visibility = Button.GONE
            statusText.text = "Belum terhubung"
        }
    }

    override fun onResume() { super.onResume(); wifiManager.registerReceiver() }
    override fun onPause() { super.onPause(); wifiManager.unregisterReceiver() }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQ_CODE) {
            val ok = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            statusText.text = if (ok) "Izin OK" else "Izin ditolak"
        }
    }
}
