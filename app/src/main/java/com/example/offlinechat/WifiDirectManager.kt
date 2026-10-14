package com.example.offlinechat

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat

class WifiDirectManager(private val context: Context) {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: Channel? = null

    var onPeersChanged: ((List<WifiP2pDevice>) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
                    if (state == WIFI_P2P_STATE_ENABLED) {
                        onStatusChanged?.invoke("WiFi Direct aktif")
                    } else {
                        onStatusChanged?.invoke("WiFi Direct MATI - nyalakan WiFi")
                    }
                }
                WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                WIFI_P2P_CONNECTION_CHANGED_ACTION -> onStatusChanged?.invoke("Koneksi berubah")
            }
        }
    }

    fun init(): Boolean {
        if (manager == null) {
            onStatusChanged?.invoke("WiFi Direct gak didukung HP ini")
            return false
        }
        channel = manager.initialize(context, Looper.getMainLooper()) {
            onStatusChanged?.invoke("WiFi Direct channel terputus")
        }
        return true
    }

    fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
    }

    fun unregisterReceiver() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        if (manager == null || channel == null) return
        if (!hasPermissions()) {
            onStatusChanged?.invoke("Izin belum lengkap")
            return
        }
        onStatusChanged?.invoke("Mencari perangkat...")
        manager.discoverPeers(channel, object : ActionListener {
            override fun onSuccess() { onStatusChanged?.invoke("Scan dimulai, tunggu...") }
            override fun onFailure(reason: Int) { onStatusChanged?.invoke("Gagal scan: kode $reason") }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        if (manager == null || channel == null) return
        if (!hasPermissions()) return
        manager.requestPeers(channel) { peerList ->
            val devices = peerList.deviceList.toList()
            onPeersChanged?.invoke(devices)
            if (devices.isEmpty()) {
                onStatusChanged?.invoke("Belum ada perangkat ditemukan")
            } else {
                onStatusChanged?.invoke("${devices.size} perangkat ditemukan")
            }
        }
    }

    private fun hasPermissions(): Boolean {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
