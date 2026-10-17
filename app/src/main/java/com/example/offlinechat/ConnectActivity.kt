package com.example.offlinechat

import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class ConnectActivity : BaseActivity() {

    companion object {
        const val EXTRA_DEVICE_NAME = "device_name"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private lateinit var device: WifiP2pDevice
    private var wifiManager: WifiDirectManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        val name = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "Device"
        val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: ""

        findViewById<TextView>(R.id.tvDeviceName).text = name

        // Bikin dummy device buat connect
        device = WifiP2pDevice()
        device.deviceName = name
        device.deviceAddress = address

        wifiManager = WifiDirectManager(this).apply {
            onStatusChanged = { msg -> runOnUiThread { findViewById<TextView>(R.id.tvDeviceName).text = msg } }
        }
        wifiManager?.init()

        findViewById<Button>(R.id.btnConnectClient).setOnClickListener {
            wifiManager?.connect(device, forceClient = true)
            // Pindah ke layar Perangkat (MainActivity tab 1)
            finish()
        }

        findViewById<Button>(R.id.btnConnectAuto).setOnClickListener {
            wifiManager?.connect(device, forceClient = false)
            finish()
        }

        findViewById<Button>(R.id.btnCancelConnect).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        wifiManager?.registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        wifiManager?.unregisterReceiver()
    }
}
