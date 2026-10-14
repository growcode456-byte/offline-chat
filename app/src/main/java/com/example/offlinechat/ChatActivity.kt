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
    private var isLoopback = false
    private var myName = "Saya"
    private lateinit var statusText: TextView
    private lateinit var list: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        isLoopback = intent.getBooleanExtra("loopback", false)
        isGroupOwner = intent.getBooleanExtra("isGroupOwner", false)
        myName = intent.getStringExtra("myName") ?: "Saya"
        val peerName = intent.getStringExtra("peerName") ?: "Peer"

        findViewById<TextView>(R.id.chatTitle).text =
            if (isLoopback) "Test Chat (1 HP)" else "Chat dengan $peerName"
        statusText = findViewById(R.id.chatStatus)
        list = findViewById(R.id.messageList)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<Button>(R.id.btnSend)

        adapter = MessageAdapter()
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        list.layoutManager = lm
        list.adapter = adapter

        if (isLoopback) {
            statusText.text = "Mode test: server + client di HP ini"
            startLoopback()
        } else {
            statusText.text = if (isGroupOwner) "Kamu: SERVER" else "Kamu: CLIENT"
            if (isGroupOwner) startServer() else startClient()
        }

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            val msg = ChatMessage(myName, text, System.currentTimeMillis(), isMine = true)
            adapter.addMessage(msg)
            list.scrollToPosition(adapter.itemCount - 1)
            if (isLoopback) {
                client?.send(msg)
            } else if (isGroupOwner) {
                server?.send(msg)
            } else {
                client?.send(msg)
            }
            input.setText("")
        }
    }

    private fun startLoopback() {
        // Server echo dulu
        server = ChatServer(8888,
            onMessage = { /* server terima pesan dari client, di-echo otomatis */ },
            onStatus = { },
            echoMode = true
        )
        server?.start()

        // Delay 800ms biar server siap
        list.postDelayed({
            client = ChatClient("127.0.0.1", 8888,
                onMessage = { msg -> runOnUiThread { addIncoming(msg) } },
                onStatus = { s -> runOnUiThread { statusText.text = s } }
            )
            client?.connect()
        }, 800)
    }

    private fun startServer() {
        server = ChatServer(8888,
            onMessage = { msg -> runOnUiThread { addIncoming(msg) } },
            onStatus = { s -> runOnUiThread { statusText.text = s } }
        )
        server?.start()
    }

    private fun startClient() {
        client = ChatClient("192.168.49.1", 8888,
            onMessage = { msg -> runOnUiThread { addIncoming(msg) } },
            onStatus = { s -> runOnUiThread { statusText.text = s } }
        )
        client?.connect()
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
