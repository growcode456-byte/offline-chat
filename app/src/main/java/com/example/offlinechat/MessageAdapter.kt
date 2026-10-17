package com.example.offlinechat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ChatItem {
    data class Message(val msg: ChatMessage) : ChatItem()
    data class DateDivider(val label: String) : ChatItem()
}

class MessageAdapter(
    private val isGroup: Boolean = false,
    private val onDelete: ((Long) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatItem>()
    private val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))

    companion object {
        const val TYPE_MESSAGE = 0
        const val TYPE_DIVIDER = 1
    }

    fun addMessage(msg: ChatMessage) {
        val needDivider = shouldAddDivider(msg.timestamp)
        if (needDivider) {
            items.add(ChatItem.DateDivider(formatDateLabel(msg.timestamp)))
            notifyItemInserted(items.size - 1)
        }
        items.add(ChatItem.Message(msg))
        notifyItemInserted(items.size - 1)
    }

    fun setMessages(list: List<ChatMessage>) {
        items.clear()
        var lastDate: String? = null
        for (msg in list) {
            val dateKey = dateKey(msg.timestamp)
            if (dateKey != lastDate) {
                items.add(ChatItem.DateDivider(formatDateLabel(msg.timestamp)))
                lastDate = dateKey
            }
            items.add(ChatItem.Message(msg))
        }
        notifyDataSetChanged()
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    private fun shouldAddDivider(timestamp: Long): Boolean {
        if (items.isEmpty()) return true
        val lastMsg = items.lastOrNull { it is ChatItem.Message } as? ChatItem.Message ?: return true
        return dateKey(lastMsg.msg.timestamp) != dateKey(timestamp)
    }

    private fun dateKey(ts: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun formatDateLabel(ts: Long): String {
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { timeInMillis = ts }
        val isToday = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        return when {
            isToday -> "Hari Ini"
            isYesterday -> "Kemarin"
            else -> dateFmt.format(Date(ts))
        }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ChatItem.Message -> TYPE_MESSAGE
        is ChatItem.DateDivider -> TYPE_DIVIDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_MESSAGE) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            MessageVH(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_date_divider, parent, false)
            DividerVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.Message -> (holder as MessageVH).bind(item.msg)
            is ChatItem.DateDivider -> (holder as DividerVH).bind(item.label)
        }
    }

    override fun getItemCount() = items.size

    inner class MessageVH(v: View) : RecyclerView.ViewHolder(v) {
        val bubble: LinearLayout = v.findViewById(R.id.bubbleContainer)
        val text: TextView = v.findViewById(R.id.messageText)
        val time: TextView = v.findViewById(R.id.messageTime)
        val image: ImageView = v.findViewById(R.id.messageImage)
        val sender: TextView = v.findViewById(R.id.senderName)

        fun bind(msg: ChatMessage) {
            val ctx = itemView.context
            val theme = ThemeHelper.current(ctx)
            val density = ctx.resources.displayMetrics.density

            text.text = msg.text
            time.text = fmt.format(Date(msg.timestamp))

            if (isGroup && !msg.isMine && msg.from.isNotEmpty()) {
                sender.visibility = View.VISIBLE
                sender.text = msg.from
                sender.setTextColor(msg.senderColor ?: Color.parseColor("#00A884"))
            } else {
                sender.visibility = View.GONE
            }

            if (msg.imageData != null) {
                try {
                    val bytes = Base64.decode(msg.imageData, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    image.setImageBitmap(bmp)
                    image.visibility = View.VISIBLE
                } catch (e: Exception) {
                    image.visibility = View.GONE
                }
            } else {
                image.visibility = View.GONE
            }

            if (msg.text.isNullOrEmpty() && msg.imageData != null) {
                text.visibility = View.GONE
                bubble.background = null
                bubble.setPadding(0, 0, 0, 0)
                val params = image.layoutParams as LinearLayout.LayoutParams
                params.width = (220 * density).toInt()
                params.height = (220 * density).toInt()
                image.layoutParams = params
            } else {
                text.visibility = View.VISIBLE
                val bg = GradientDrawable().apply {
                    cornerRadii = floatArrayOf(
                        16f * density, 16f * density,
                        16f * density, 16f * density,
                        if (msg.isMine) 4f * density else 16f * density,
                        if (msg.isMine) 4f * density else 16f * density,
                        if (msg.isMine) 16f * density else 4f * density,
                        if (msg.isMine) 16f * density else 4f * density
                    )
                    setColor(if (msg.isMine) theme.bubbleMine else theme.bubbleOther)
                }
                bubble.background = bg
                bubble.setPadding(
                    (6 * density).toInt(), (6 * density).toInt(),
                    (6 * density).toInt(), (4 * density).toInt()
                )
            }

            val params = bubble.layoutParams as LinearLayout.LayoutParams
            if (msg.isMine) {
                params.gravity = Gravity.END
                time.gravity = Gravity.END
            } else {
                params.gravity = Gravity.START
                time.gravity = Gravity.START
            }
            bubble.layoutParams = params

            // --- FIX KLIK: Matikan klik di teks & gambar, pindah ke bubble ---
            text.isClickable = false
            text.isLongClickable = false
            time.isClickable = false
            time.isLongClickable = false
            image.isClickable = false
            image.isLongClickable = false

            bubble.isClickable = true
            bubble.isLongClickable = true
            
            bubble.setOnLongClickListener {
                val dialog = BottomSheetDialog(ctx)
                val view = LayoutInflater.from(ctx).inflate(R.layout.layout_message_menu, null)
                dialog.setContentView(view)

                view.findViewById<View>(R.id.menuCopy).setOnClickListener {
                    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Pesan", msg.text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(ctx, "Pesan disalin", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }

                view.findViewById<View>(R.id.menuDelete).setOnClickListener {
                    if (onDelete != null) {
                        onDelete.invoke(msg.timestamp)
                    }
                    dialog.dismiss()
                }

                dialog.show()
                true
            }

            // Tap gambar → Fullscreen (karena isClickable=false, kita set OnTouchListener biar tetep bisa di-tap)
            image.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    if (msg.imageData != null) {
                        val intent = android.content.Intent(ctx, ImageViewerActivity::class.java)
                        intent.putExtra("imageData", msg.imageData)
                        ctx.startActivity(intent)
                    }
                }
                false // return false biar bubble tetep bisa long-press
            }

            val anim = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in)
            anim.duration = 200
            itemView.startAnimation(anim)
        }
    }

    class DividerVH(v: View) : RecyclerView.ViewHolder(v) {
        val dateText: TextView = v.findViewById(R.id.dateText)
        fun bind(label: String) { dateText.text = label }
    }
}
