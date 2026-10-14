package com.example.offlinechat

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class ChatClient(
    private val serverIp: String,
    private val port: Int,
    private val onMessage: (ChatMessage) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    @Volatile private var running = false

    fun connect() {
        running = true
        Thread {
            try {
                onStatus("Connect ke $serverIp:$port...")
                socket = Socket().apply { connect(InetSocketAddress(serverIp, port), 5000) }
                onStatus("Tersambung ke server!")
                writer = PrintWriter(socket!!.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                var line: String?
                while (running && reader.readLine().also { line = it } != null) {
                    ChatMessage.fromJson(line!!)?.let { onMessage(it) }
                }
            } catch (e: Exception) {
                if (running) onStatus("Client error: ${e.message}")
            }
        }.start()
    }

    fun send(msg: ChatMessage) {
        try { writer?.println(msg.toJson()) }
        catch (e: Exception) { onStatus("Gagal kirim: ${e.message}") }
    }

    fun stop() {
        running = false
        try { socket?.close() } catch (_: Exception) {}
    }
}
