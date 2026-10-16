package com.example.offlinechat

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.random.Random

class ChatActivity : BaseActivity() {

    private lateinit var adapter: MessageAdapter
    private lateinit var db: AppDatabase
    private var tcpServer: ChatServer? = null
    private var tcpClient: ChatClient? = null

    private var mode = "tcp"
    private var isServer = false
    private var myName = "Saya"
    private var chatId = "default"
    private var isGroup = false

    private lateinit var statusText: TextView
    private lateinit var list: RecyclerView
    private val handler = Handler(Looper.getMainLooper())

    // Data bot grup
    private val groupBots = listOf(
        Bot("Budi", Color.parseColor("#E57373"),
            listOf("Wkwk", "Iya bener", "Hahaha", "Nice!", "Setuju 👍")),
        Bot("Sari", Color.parseColor("#BA68C8"),
            listOf("Hmm...", "Aku juga", "Bentar ya", "Oke deh", "Mantap ✨")),
        Bot("Andi", Color.parseColor("#4DB6AC"),
            listOf("Kuy", "Gas!", "Bener banget", "Aku setuju", "Wkwk 😂"))
    )

    data class Bot(val name: String, val color: Int, val replies: List<String>)

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) handleImagePicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val isLoopback = intent.getBooleanExtra("loopback", false)
        val isGroupMode = intent.getBooleanExtra("group", false)
        val wfdIsServer = intent.getBooleanExtra("isGroupOwner", false)
        val wfdPeer = intent.getStringExtra("peerName") ?: "Peer"

        myName = intent.getStringExtra("myName") ?: PrefsHelper.getName(this)
        isGroup = isGroupMode

        chatId = when {
            isGroupMode -> "group_sim"
            isLoopback -> "loopback"
            else -> "chat_$wfdPeer"
        }

        statusText = findViewById(R.id.chatStatus)
        list = findViewById(R.id.messageList)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnAttach = findViewById<ImageButton>(R.id.btnAttach)

        findViewById<TextView>(R.id.chatTitle).text = when {
            isGroupMode -> "Grup: Budi, Sari, Andi"
            isLoopback -> "Test Chat (1 HP)"
            else -> "Chat WFD: $wfdPeer"
        }

        db = AppDatabase.get(this)

        adapter = MessageAdapter(isGroup = isGroupMode)
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        list.layoutManager = lm
        list.adapter = adapter

        lifecycleScope.launch {
            db.messageDao().getMessages(chatId).collect { entities ->
                val msgs = entities.map { it.toChatMessage() }
                adapter.setMessages(msgs)
                if (msgs.isNotEmpty()) list.scrollToPosition(adapter.itemCount - 1)
            }
        }

        if (isGroupMode) {
            mode = "group"
            statusText.text = "Grup simulasi (3 bot) • ${groupBots.size} anggota"
        } else if (isLoopback) {
            mode = "loopback"
            statusText.text = "Mode test lokal (tersimpan)"
        } else {
            mode = "tcp"
            isServer = wfdIsServer
            statusText.text = if (isServer) "TCP Server" else "TCP Client"
            if (isServer) startTcpServer() else startTcpClient()
        }

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            val msg = ChatMessage(myName, text, System.currentTimeMillis(), isMine = true)
            addAndSave(msg)
            input.setText("")

            when (mode) {
                "group" -> simulateGroupReplies(text)
                "loopback" -> handler.postDelayed({
                    val echo = ChatMessage("Echo Bot", "Echo: $text", System.currentTimeMillis())
                    addAndSave(echo)
                }, 600)
                else -> if (isServer) tcpServer?.send(msg) else tcpClient?.send(msg)
            }
        }

        btnAttach.setOnClickListener {
            pickImage.launch("image/*")
        }

        findViewById<TextView>(R.id.chatTitle).setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Riwayat?")
                .setMessage("Semua chat di grup ini bakal dihapus.")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        db.messageDao().clearChat(chatId)
                        adapter.clear()
                        Toast.makeText(this@ChatActivity, "Riwayat dihapus", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
            true
        }
    }

    private fun simulateGroupReplies(userText: String) {
        // Random: 1-3 bot yang bales
        val repliers = groupBots.shuffled().take(Random.nextInt(1, groupBots.size + 1))
        var delay = 600L

        repliers.forEach { bot ->
            handler.postDelayed({
                val reply = when {
                    userText.contains("halo", ignoreCase = true) -> "Halo juga! 👋"
                    userText.contains("apa kabar", ignoreCase = true) -> "Baik dong!"
                    userText.length < 5 -> bot.replies.random()
                    else -> bot.replies.random()
                }
                val echo = ChatMessage(
                    from = bot.name,
                    text = reply,
                    timestamp = System.currentTimeMillis(),
                    senderColor = bot.color
                )
                addAndSave(echo)
            }, delay)
            delay += Random.nextLong(400, 1200)
        }
    }

    private fun handleImagePicked(uri: Uri) {
        lifecycleScope.launch {
            try {
                val base64
cd /workspaces/offline-chat

# 1. Update ChatMessage - tambah senderName untuk grup
cat > app/src/main/java/com/example/offlinechat/MessageProtocol.kt <<'EOF'
package com.example.offlinechat

import org.json.JSONObject

data class ChatMessage(
    val from: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean = false,
    val imageData: String? = null,
    val senderColor: Int? = null  // untuk grup: warna nama pengirim
) {
    fun toJson(): String {
        val o = JSONObject()
        o.put("from", from)
        o.put("text", text)
        o.put("ts", timestamp)
        if (imageData != null) o.put("img", imageData)
        if (senderColor != null) o.put("color", senderColor)
        return o.toString()
    }

    companion object {
        fun fromJson(s: String): ChatMessage? {
            return try {
                val o = JSONObject(s)
                ChatMessage(
                    from = o.getString("from"),
                    text = o.getString("text"),
                    timestamp = o.getLong("ts"),
                    imageData = if (o.has("img")) o.getString("img") else null,
                    senderColor = if (o.has("color")) o.getInt("color") else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
