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
                socket = Socket()
                socket?.connect(InetSocketAddress(serverIp, port), 5000)
                onStatus("Tersambung ke server!")
                writer = PrintWriter(socket!!.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                while (running) {
                    val line: String = reader.readLine() ?: break
                    val msg = ChatMessage.fromJson(line)
                    if (msg != null) onMessage(msg)
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
