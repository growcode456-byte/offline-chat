package com.example.offlinechat

import android.content.Intent
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout

class PerangkatFragment : Fragment() {

    private lateinit var wifiManager: WifiDirectManager
    private lateinit var statusText: TextView
    private lateinit var btnOpenChat: ExtendedFloatingActionButton
    private lateinit var btnDisconnect: View
    private lateinit var tabMode: TabLayout
    private lateinit var layoutWifi: LinearLayout
    private lateinit var layoutHotspot: LinearLayout
    private lateinit var btnHotspotConnect: Button

    private var isConnected = false
    private var isGroupOwner = false
    private var connectedPeerName: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            statusText.text = "Izin OK. Mencari HP lain..."
            wifiManager.discoverPeers()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.activity_perangkat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        statusText = view.findViewById(R.id.perangkatStatus)
        btnOpenChat = view.findViewById(R.id.btnOpenChatPerangkat)
        btnDisconnect = view.findViewById(R.id.btnDisconnectPerangkat)
        tabMode = view.findViewById(R.id.tabMode)
        layoutWifi = view.findViewById(R.id.layoutWifiDirect)
        layoutHotspot = view.findViewById(R.id.layoutHotspot)
        btnHotspotConnect = view.findViewById(R.id.btnHotspotConnect)

        wifiManager = WifiDirectManager(requireContext()).apply {
            onStatusChanged = { msg -> activity?.runOnUiThread { statusText.text = msg } }
            onConnectionInfo = { info -> activity?.runOnUiThread { handleConnectionInfo(info) } }
        }
        wifiManager.init()

        if (!PermissionHelper.hasAll(requireActivity())) {
            permissionLauncher.launch(PermissionHelper.neededPermissions())
        }

        // TAB LOGIC
        tabMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    layoutWifi.visibility = View.VISIBLE
                    layoutHotspot.visibility = View.GONE
                    wifiManager.discoverPeers()
                } else {
                    layoutWifi.visibility = View.GONE
                    layoutHotspot.visibility = View.VISIBLE
                    statusText.text = "Mode Hotspot Manual"
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // TOMBOL HOTSPOT (Manual TCP)
        btnHotspotConnect.setOnClickListener {
            // Paksa masuk ke Chat sebagai Client (nembak IP Hotspot 192.168.43.1)
            startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("hotspotMode", true) // Flag khusus Hotspot
                putExtra("myName", PrefsHelper.getName(requireContext()))
                putExtra("peerName", "Hotspot Host")
            })
        }

        // TOMBOL BUKA CHAT (WIFI DIRECT)
        btnOpenChat.setOnClickListener {
            startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("isGroupOwner", isGroupOwner)
                putExtra("myName", PrefsHelper.getName(requireContext()))
                putExtra("peerName", connectedPeerName ?: "Peer")
            })
        }

        btnDisconnect.setOnClickListener {
            wifiManager.disconnect()
            isConnected = false
            btnDisconnect.visibility = View.GONE
            btnOpenChat.visibility = View.GONE
            statusText.text = "Terputus. Mencari ulang..."
            wifiManager.discoverPeers()
        }
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        if (info.groupFormed) {
            isConnected = true
            isGroupOwner = info.isGroupOwner
            statusText.text = "✅ Terhubung - " + (if (isGroupOwner) "SERVER" else "CLIENT")
            connectedPeerName = if (isGroupOwner) "Client" else "Server"
            btnDisconnect.visibility = View.VISIBLE
            btnOpenChat.visibility = View.VISIBLE
        } else {
            isConnected = false
            btnDisconnect.visibility = View.GONE
            btnOpenChat.visibility = View.GONE
        }
    }

    override fun onResume() { 
        super.onResume()
        wifiManager.registerReceiver()
        if (tabMode.selectedTabPosition == 0) wifiManager.discoverPeers()
    }
    override fun onPause() { 
        super.onPause()
        wifiManager.unregisterReceiver() 
    }
}
