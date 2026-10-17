package com.example.offlinechat

import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PeerAdapter(
    private val onTap: (WifiP2pDevice) -> Unit
) : RecyclerView.Adapter<PeerAdapter.VH>() {

    private val items = mutableListOf<WifiP2pDevice>()

    fun update(newList: List<WifiP2pDevice>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_peer, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val device = items[position]
        holder.name.text = device.deviceName ?: "Tanpa nama"
        holder.info.text = when (device.status) {
            WifiP2pDevice.AVAILABLE -> "Tersedia (Tap untuk connect)"
            WifiP2pDevice.INVITED -> "Diundang"
            WifiP2pDevice.CONNECTED -> "Terhubung"
            WifiP2pDevice.FAILED -> "Gagal"
            WifiP2pDevice.UNAVAILABLE -> "Tidak tersedia"
            else -> "?"
        }
        
        // Pindah ke ConnectActivity saat di-tap
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ConnectActivity::class.java).apply {
                putExtra(ConnectActivity.EXTRA_DEVICE_NAME, device.deviceName)
                putExtra(ConnectActivity.EXTRA_DEVICE_ADDRESS, device.deviceAddress)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.peerName)
        val info: TextView = v.findViewById(R.id.peerInfo)
    }
}
