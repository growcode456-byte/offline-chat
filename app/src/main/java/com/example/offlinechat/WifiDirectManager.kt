package com.example.offlinechat

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_STATE
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat

class WifiDirectManager(private val context: Context) {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: Channel? = null

    var onPeersChanged: ((List<WifiP2pDevice>) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null
    var onConnectionInfo: ((WifiP2pInfo) -> Unit)? = null
    var onThisDeviceChanged: ((WifiP2pDevice) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
                    onStatusChanged?.invoke(
                        if (state == WIFI_P2P_STATE_ENABLED) "WiFi Direct aktif"
                        else "WiFi Direct MATI"
                    )
                }
                WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    if (manager == null || channel == null) return
                    if (!hasPermissions()) return
                    manager.requestConnectionInfo(channel) { info ->
                        onConnectionInfo?.invoke(info)
                    }
                }
                WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    @Suppress("DEPRECATION")
                    val device = intent.getParcelableExtra<WifiP2pDevice>(EXTRA_WIFI_P2P_DEVICE)
                    if (device != null) onThisDeviceChanged?.invoke(device)
                }
            }
        }
    }

    fun init(): Boolean {
        if (manager == null) {
            onStatusChanged?.invoke("WiFi Direct gak didukung")
            return false
        }
        channel = manager.initialize(context, Looper.getMainLooper()) {
            onStatusChanged?.invoke("Channel terputus")
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
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregisterReceiver() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        if (manager == null || channel == null) return
        if (!hasPermissions()) { onStatusChanged?.invoke("Izin belum lengkap"); return }
        onStatusChanged?.invoke("Mencari perangkat...")
        manager.discoverPeers(channel, object : ActionListener {
            override fun onSuccess() { onStatusChanged?.invoke("Scan dimulai...") }
            override fun onFailure(reason: Int) { onStatusChanged?.invoke("Gagal scan: $reason") }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        if (manager == null || channel == null) return
        if (!hasPermissions()) return
        manager.requestPeers(channel) { peerList ->
            val devices = peerList.deviceList.toList()
            onPeersChanged?.invoke(devices)
            if (devices.isEmpty()) onStatusChanged?.invoke("Belum ada perangkat")
            else onStatusChanged?.invoke("${devices.size} perangkat ditemukan")
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {
        if (manager == null || channel == null) return
        if (!hasPermissions()) { onStatusChanged?.invoke("Izin belum lengkap"); return }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = android.net.wifi.p2p.WpsInfo.PBC
        }
        onStatusChanged?.invoke("Menghubungkan ke ${device.deviceName}...")
        manager.connect(channel, config, object : ActionListener {
            override fun onSuccess() { onStatusChanged?.invoke("Koneksi diproses...") }
            override fun onFailure(reason: Int) {
                onStatusChanged?.invoke("Gagal connect: kode $reason")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (manager == null || channel == null) return
        manager.removeGroup(channel, object : ActionListener {
            override fun onSuccess() { onStatusChanged?.invoke("Terputus") }
            override fun onFailure(reason: Int) { onStatusChanged?.invoke("Gagal putus: $reason") }
        })
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
