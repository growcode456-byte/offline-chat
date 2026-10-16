package com.example.offlinechat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ChatActivity : BaseActivity() {

    private lateinit var adapter: MessageAdapter
    private lateinit var db: AppDatabase
    private var tcpServer: ChatServer? = null
    private var tcpClient: ChatClient? = null

    private var mode = "tcp"
    private var isServer = false
    private var myName = "Saya"
    private var chatId = "default"

    private lateinit var statusText: TextView
    private lateinit var list: RecyclerView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val isLoopback = intent.getBooleanExtra("loopback", false)
        val wfdIsServer = intent.getBooleanExtra("isGroupOwner", false)
        val wfdPeer = intent.getStringExtra("peerName") ?: "Peer"

        myName = intent.getStringExtra("myName") ?: PrefsHelper.getName(this)
        chatId = if (isLoopback) "loopback" else "chat_$wfdPeer"

        statusText = findViewById(R.id.chatStatus)
        list = findViewById(R.id.messageList)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        findViewById<TextView>(R.id.chatTitle).text =
            if (isLoopback) "Test Chat (1 HP)" else "Chat WFD: $wfdPeer"

        db = AppDatabase.get(this)

        adapter = MessageAdapter()
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

        if (isLoopback) {
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
                "loopback" -> handler.postDelayed({
                    val echo = ChatMessage("Echo Bot", "Echo: $text", System.currentTimeMillis())
                    addAndSave(echo)
                }, 600)
                else -> if (isServer) tcpServer?.send(msg) else tcpClient?.send(msg)
            }
        }

        findViewById<TextView>(R.id.chatTitle).setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Riwayat?")
                .setMessage("Semua chat di sini bakal dihapus.")
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

    private fun addAndSave(msg: ChatMessage) {
        adapter.addMessage(msg)
        list.smoothScrollToPosition(adapter.itemCount - 1)
        lifecycleScope.launch {
            db.messageDao().insert(msg.toEntity(chatId))
        }
    }

    private fun startTcpServer() {
        tcpServer = ChatServer(
            port = 8888,
            onMessage = { msg: ChatMessage -> runOnUiThread { addAndSave(msg.copy(isMine = false)) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
        )
        tcpServer?.start()
    }

    private fun startTcpClient() {
        tcpClient = ChatClient(
            serverIp = "192.168.49.1",
            port = 8888,
            onMessage = { msg: ChatMessage -> runOnUiThread { addAndSave(msg.copy(isMine = false)) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
        )
        tcpClient?.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        tcpServer?.stop(); tcpClient?.stop()
    }
}
