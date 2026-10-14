package com.example.offlinechat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
        val ctx = holder.itemView.context

        holder.text.text = msg.text
        holder.time.text = fmt.format(Date(msg.timestamp))

        val params = holder.bubble.layoutParams as LinearLayout.LayoutParams
        if (msg.isMine) {
            holder.bubble.setBackgroundResource(R.drawable.bubble_me)
            params.gravity = Gravity.END
            holder.time.gravity = Gravity.END
        } else {
            holder.bubble.setBackgroundResource(R.drawable.bubble_other)
            params.gravity = Gravity.START
            holder.time.gravity = Gravity.START
        }
        holder.bubble.layoutParams = params
    }

    override fun getItemCount() = messages.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val bubble: LinearLayout = v.findViewById(R.id.bubbleContainer)
        val text: TextView = v.findViewById(R.id.messageText)
        val time: TextView = v.findViewById(R.id.messageTime)
    }
}
