package com.example.offlinechat

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private var tcpServer: ChatServer? = null
    private var tcpClient: ChatClient? = null
    private var btServer: BluetoothChatServer? = null
    private var btClient: BluetoothChatClient? = null

    private var mode = "tcp"
    private var isServer = false
    private var myName = "Saya"

    private lateinit var statusText: TextView
    private lateinit var list: RecyclerView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val isLoopback = intent.getBooleanExtra("loopback", false)
        val btMode = intent.getBooleanExtra("btMode", false)
        val btIsServer = intent.getBooleanExtra("btIsServer", false)
        val btAddress = intent.getStringExtra("btAddress")
        val btName = intent.getStringExtra("btName") ?: "Peer"
        val wfdIsServer = intent.getBooleanExtra("isGroupOwner", false)
        val wfdPeer = intent.getStringExtra("peerName") ?: "Peer"

        myName = intent.getStringExtra("myName") ?: "Saya"

        statusText = findViewById(R.id.chatStatus)
        list = findViewById(R.id.messageList)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        findViewById<TextView>(R.id.chatTitle).text = when {
            isLoopback -> "Test Chat (1 HP)"
            btMode -> "Chat BT: $btName"
            else -> "Chat WFD: $wfdPeer"
        }

        adapter = MessageAdapter()
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        list.layoutManager = lm
        list.adapter = adapter

        when {
            isLoopback -> {
                mode = "loopback"
                statusText.text = "Mode test lokal"
            }
            btMode -> {
                mode = "bt"
                isServer = btIsServer
                statusText.text = if (isServer) "BT Server..." else "BT Client..."
                if (isServer) startBtServer() else startBtClient(btAddress)
            }
            else -> {
                mode = "tcp"
                isServer = wfdIsServer
                statusText.text = if (isServer) "TCP Server" else "TCP Client"
                if (isServer) startTcpServer() else startTcpClient()
            }
        }

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            val msg = ChatMessage(myName, text, System.currentTimeMillis(), isMine = true)
            adapter.addMessage(msg)
            list.scrollToPosition(adapter.itemCount - 1)
            input.setText("")

            when (mode) {
                "loopback" -> handler.postDelayed({
                    val echo = ChatMessage("Echo Bot", "Echo: $text", System.currentTimeMillis())
                    adapter.addMessage(echo)
                    list.scrollToPosition(adapter.itemCount - 1)
                }, 600)
                "bt" -> if (isServer) btServer?.send(msg) else btClient?.send(msg)
                else -> if (isServer) tcpServer?.send(msg) else tcpClient?.send(msg)
            }
        }

        if (isLoopback) {
            handler.postDelayed({
                val welcome = ChatMessage(
                    "Echo Bot",
                    "Halo! Ketik pesan apa aja, nanti aku balas.",
                    System.currentTimeMillis()
                )
                adapter.addMessage(welcome)
            }, 300)
        }
    }

    private fun startTcpServer() {
        tcpServer = ChatServer(
            port = 8888,
            onMessage = { msg: ChatMessage -> runOnUiThread { addIncoming(msg) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
        )
        tcpServer?.start()
    }

    private fun startTcpClient() {
        tcpClient = ChatClient(
            serverIp = "192.168.49.1",
            port = 8888,
            onMessage = { msg: ChatMessage -> runOnUiThread { addIncoming(msg) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
        )
        tcpClient?.connect()
    }

    private fun startBtServer() {
        btServer = BluetoothChatServer(
            context = this,
            onMessage = { msg: ChatMessage -> runOnUiThread { addIncoming(msg) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
        )
        btServer?.start()
    }

    @SuppressLint("MissingPermission")
    private fun startBtClient(address: String?) {
        if (address == null) { statusText.text = "Alamat BT kosong"; return }
        val btAdapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
        val device = try { btAdapter?.getRemoteDevice(address) } catch (e: Exception) { null }
        if (device == null) { statusText.text = "Device BT gak ditemukan"; return }
        btClient = BluetoothChatClient(
            device = device,
            onMessage = { msg: ChatMessage -> runOnUiThread { addIncoming(msg) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
        )
        btClient?.connect()
    }

    private fun addIncoming(msg: ChatMessage) {
        adapter.addMessage(msg.copy(isMine = false))
        list.scrollToPosition(adapter.itemCount - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        tcpServer?.stop(); tcpClient?.stop()
        btServer?.stop(); btClient?.stop()
    }
}
