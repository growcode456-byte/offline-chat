package com.example.offlinechat

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class ChatServer(
    private val port: Int,
    private val onMessage: (ChatMessage) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null
    @Volatile private var running = false

    fun start() {
        running = true
        Thread {
            try {
                onStatus("Server start di port $port")
                serverSocket = ServerSocket(port)
                onStatus("Menunggu client...")
                clientSocket = serverSocket?.accept()
                onStatus("Client tersambung!")
                writer = PrintWriter(clientSocket!!.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                var line: String?
                while (running && reader.readLine().also { line = it } != null) {
                    ChatMessage.fromJson(line!!)?.let { onMessage(it) }
                }
            } catch (e: Exception) {
                if (running) onStatus("Server error: ${e.message}")
            }
        }.start()
    }

    fun send(msg: ChatMessage) {
        try { writer?.println(msg.toJson()) }
        catch (e: Exception) { onStatus("Gagal kirim: ${e.message}") }
    }

    fun stop() {
        running = false
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
    }
}
