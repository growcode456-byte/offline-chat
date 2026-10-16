package com.example.offlinechat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmojiAdapter(
    private val emojis: List<String>,
    private val onPick: (String) -> Unit
) : RecyclerView.Adapter<EmojiAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emoji, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val emoji = emojis[position]
        holder.text.text = emoji
        holder.text.setOnClickListener { onPick(emoji) }
    }

    override fun getItemCount() = emojis.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.emojiText)
    }
}
