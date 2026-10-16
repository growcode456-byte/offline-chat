package com.example.offlinechat

import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PerangkatFragment : Fragment() {

    private lateinit var wifiManager: WifiDirectManager
    private lateinit var adapter: PeerAdapter
    private lateinit var statusText: TextView
    private lateinit var btnScan: Button
    private lateinit var btnOpenChat: Button
    private lateinit var btnDisconnect: Button
    private lateinit var debugPanel: LinearLayout
    private lateinit var debugText: TextView

    private var isConnected = false
    private var isGroupOwner = false
    private var connectedPeerName: String? = null

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) statusText.text = "Izin OK, tap Cari"
        else statusText.text = "Izin ditolak"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_perangkat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        statusText = view.findViewById(R.id.perangkatStatus)
        btnScan = view.findViewById(R.id.btnScanPerangkat)
        btnOpenChat = view.findViewById(R.id.btnOpenChatPerangkat)
        btnDisconnect = view.findViewById(R.id.btnDisconnectPerangkat)
        debugPanel = view.findViewById(R.id.debugPanel)
        debugText = view.findViewById(R.id.debugText)
        val list = view.findViewById<RecyclerView>(R.id.peerList)

        adapter = PeerAdapter { device ->
            if (isConnected) Toast.makeText(requireContext(), "Sudah terhubung", Toast.LENGTH_SHORT).show()
            else showConnectDialog(device)
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        wifiManager = WifiDirectManager(requireContext()).apply {
            onPeersChanged = { devices -> activity?.runOnUiThread { adapter.update(devices) } }
            onStatusChanged = { msg -> activity?.runOnUiThread { statusText.text = msg } }
            onConnectionInfo = { info -> activity?.runOnUiThread { handleConnectionInfo(info) } }
            onDebugInfo = { msg -> activity?.runOnUiThread { appendDebug(msg) } }
        }
        wifiManager.init()

        btnScan.setOnClickListener {
            if (PermissionHelper.hasAll(requireActivity())) {
                wifiManager.discoverPeers()
                debugPanel.visibility = View.VISIBLE
            } else {
                permissionLauncher.launch(PermissionHelper.neededPermissions())
            }
        }

        btnOpenChat.setOnClickListener {
            val myName = PrefsHelper.getName(requireContext())
            startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("isGroupOwner", isGroupOwner)
                putExtra("myName", myName)
                putExtra("peerName", connectedPeerName ?: "Peer")
            })
        }

        btnDisconnect.setOnClickListener {
            wifiManager.disconnect()
            isConnected = false
            connectedPeerName = null
            btnDisconnect.visibility = View.GONE
            btnOpenChat.visibility = View.GONE
            statusText.text = "Terputus"
            appendDebug("Disconnect")
        }
    }

    private fun appendDebug(msg: String) {
        val current = debugText.text.toString()
        val lines = current.split("\n").takeLast(4)
        debugText.text = (lines + msg).joinToString("\n")
        debugPanel.visibility = View.VISIBLE
    }

    private fun showConnectDialog(device: WifiP2pDevice) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hubungkan?")
            .setMessage("Connect ke ${device.deviceName ?: "perangkat"}?")
            .setPositiveButton("Sebagai Client") { _, _ ->
                wifiManager.connect(device, forceClient = true)
            }
            .setNeutralButton("Auto") { _, _ ->
                wifiManager.connect(device, forceClient = false)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        if (info.groupFormed) {
            isConnected = true
            isGroupOwner = info.isGroupOwner
            val role = if (isGroupOwner) "SERVER (GO)" else "CLIENT"
            val ip = info.groupOwnerAddress?.hostAddress ?: "?"
            statusText.text = "Terhubung - $role | IP: $ip"
            connectedPeerName = if (isGroupOwner) "Client" else "Server"
            btnDisconnect.visibility = View.VISIBLE
            btnOpenChat.visibility = View.VISIBLE
            appendDebug("Connected: $role | IP: $ip")
        } else {
            isConnected = false
            connectedPeerName = null
            btnDisconnect.visibility = View.GONE
            btnOpenChat.visibility = View.GONE
            statusText.text = "Belum terhubung"
            appendDebug("Disconnected")
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
}
