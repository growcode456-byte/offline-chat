package com.example.offlinechat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {
    private lateinit var adapter: MessageAdapter
    private var server: ChatServer? = null
    private var client: ChatClient? = null
    private var isGroupOwner = false
    private var myName = "Saya"
    private lateinit var statusText: TextView
    private lateinit var list: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        isGroupOwner = intent.getBooleanExtra("isGroupOwner", false)
        myName = intent.getStringExtra("myName") ?: "Saya"
        val peerName = intent.getStringExtra("peerName") ?: "Peer"

        findViewById<TextView>(R.id.chatTitle).text = "Chat dengan $peerName"
        statusText = findViewById(R.id.chatStatus)
        list = findViewById(R.id.messageList)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<Button>(R.id.btnSend)

        adapter = MessageAdapter()
        list.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        list.adapter = adapter

        statusText.text = if (isGroupOwner) "Kamu: SERVER" else "Kamu: CLIENT"
        if (isGroupOwner) startServer() else startClient()

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            val msg = ChatMessage(myName, text, System.currentTimeMillis(), isMine = true)
            adapter.addMessage(msg)
            list.scrollToPosition(adapter.itemCount - 1)
            if (isGroupOwner) server?.send(msg) else client?.send(msg)
            input.setText("")
        }
    }

    private fun startServer() {
        server = ChatServer(8888,
            onMessage = { msg -> runOnUiThread { addIncoming(msg) } },
            onStatus = { s -> runOnUiThread { statusText.text = s } }
        ).also { it.start() }
    }

    private fun startClient() {
        client = ChatClient("192.168.49.1", 8888,
            onMessage = { msg -> runOnUiThread { addIncoming(msg) } },
            onStatus = { s -> runOnUiThread { statusText.text = s } }
        ).also { it.connect() }
    }

    private fun addIncoming(msg: ChatMessage) {
        adapter.addMessage(msg.copy(isMine = false))
        list.scrollToPosition(adapter.itemCount - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        client?.stop()
    }
}
