package com.example.offlinechat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.VH>() {
    private val messages = mutableListOf<ChatMessage>()
    private val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = messages[position]
        holder.text.text = msg.text
        holder.time.text = fmt.format(Date(msg.timestamp))
        if (msg.isMine) {
            holder.text.setBackgroundColor(0xFFBBDEFB.toInt())
            holder.text.gravity = Gravity.END
            holder.time.gravity = Gravity.END
        } else {
            holder.text.setBackgroundColor(0xFFE0E0E0.toInt())
            holder.text.gravity = Gravity.START
            holder.time.gravity = Gravity.START
        }
    }

    override fun getItemCount() = messages.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.messageText)
        val time: TextView = v.findViewById(R.id.messageTime)
    }
}
