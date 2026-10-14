package com.example.offlinechat

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
    private lateinit var btnDisconnect: Button

    private var isConnected = false
    private var connectedPeerName: String? = null
    private var myDeviceName: String = "?"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnScan = findViewById(R.id.btnScan)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        val list = findViewById<RecyclerView>(R.id.peerList)

        adapter = PeerAdapter { device ->
            if (isConnected) {
                Toast.makeText(this, "Sudah terhubung!", Toast.LENGTH_SHORT).show()
            } else {
                showConnectDialog(device)
            }
        }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        wifiManager = WifiDirectManager(this).apply {
            onPeersChanged = { devices -> runOnUiThread { adapter.update(devices) } }
            onStatusChanged = { msg -> runOnUiThread { statusText.text = msg } }
            onThisDeviceChanged = { dev ->
                runOnUiThread {
                    myDeviceName = dev.deviceName ?: "?"
                }
            }
            onConnectionInfo = { info -> runOnUiThread { handleConnectionInfo(info) } }
        }

        if (!wifiManager.init()) {
            statusText.text = "WiFi Direct gak didukung"
            return
        }

        btnScan.setOnClickListener {
            if (PermissionHelper.hasAll(this)) wifiManager.discoverPeers()
            else PermissionHelper.request(this)
        }

        btnDisconnect.setOnClickListener {
            wifiManager.disconnect()
            isConnected = false
            connectedPeerName = null
            btnDisconnect.visibility = Button.GONE
            statusText.text = "Terputus"
        }

        btnDisconnect.visibility = Button.GONE

        if (!PermissionHelper.hasAll(this)) PermissionHelper.request(this)
    }

    private fun showConnectDialog(device: WifiP2pDevice) {
        AlertDialog.Builder(this)
            .setTitle("Hubungkan?")
            .setMessage("Connect ke ${device.deviceName ?: "perangkat"} ?")
            .setPositiveButton("Hubungkan") { _, _ ->
                wifiManager.connect(device)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        if (info.groupFormed) {
            isConnected = true
            val role = if (info.isGroupOwner) "Group Owner (Server)" else "Client"
            val myIp = if (info.isGroupOwner) "192.168.49.1" else "?"
            statusText.text = "✅ Terhubung sebagai $role\nIP server: ${info.groupOwnerAddress?.hostAddress}"
            connectedPeerName = if (info.isGroupOwner) "Client" else "Server"
            btnDisconnect.visibility = Button.VISIBLE

            Toast.makeText(
                this,
                "Terhubung! Kamu: $role",
                Toast.LENGTH_LONG
            ).show()

            // Info debug
            println("Connection: isGO=${info.isGroupOwner}, GOAddr=${info.groupOwnerAddress?.hostAddress}")
        } else {
            isConnected = false
            connectedPeerName = null
            btnDisconnect.visibility = Button.GONE
            statusText.text = "Belum terhubung"
        }
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
            statusText.text = if (ok) "Izin OK" else "Izin ditolak"
        }
    }
}
