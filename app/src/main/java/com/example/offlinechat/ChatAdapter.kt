package com.example.offlinechat

import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatAdapter(private val onTap: (ChatPreview) -> Unit)
    : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<ChatPreview>()
    private val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    fun update(list: List<ChatPreview>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.preview.text = item.lastMessage
        holder.time.text = formatTime(item.lastTimestamp)

        // Avatar: Coba ambil dari foto profil, kalau gak ada pake huruf depan
        val avatarBase64 = PrefsHelper.getAvatar(holder.itemView.context)
        if (avatarBase64 != null) {
            try {
                val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.avatar.setImageBitmap(bmp)
                // Bikin background kotak ijo jadi transparan/rounded
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 50f
                    setColor(0x00000000)
                }
                holder.avatar.background = bg
            } catch (e: Exception) {
                setDefaultAvatar(holder, item)
            }
        } else {
            setDefaultAvatar(holder, item)
        }

        holder.itemView.setOnClickListener { onTap(item) }
    }

    private fun setDefaultAvatar(holder: VH, item: ChatPreview) {
        holder.avatar.setImageDrawable(null)
        holder.avatar.text = item.title.take(1).uppercase()
        holder.avatar.background.setTint(item.colorAccent)
    }

    private fun formatTime(ts: Long): String {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val isToday = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        return if (isToday) fmt.format(Date(ts)) else dateFmt.format(Date(ts))
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.chatTitle)
        val preview: TextView = v.findViewById(R.id.chatPreview)
        val time: TextView = v.findViewById(R.id.chatTime)
        val avatar: TextView = v.findViewById(R.id.chatAvatar) // Di XML ini TextView, kita handle di kode
    }
}
