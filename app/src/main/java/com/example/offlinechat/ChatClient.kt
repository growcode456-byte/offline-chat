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
        if (running) return
        running = true
        
        Thread {
            while (running) {
                try {
                    onStatus("Mencari Server...")
                    socket = Socket()
                    socket?.connect(InetSocketAddress(serverIp, port), 3000) // Timeout 3 detik
                    onStatus("✅ Tersambung ke server!")
                    writer = PrintWriter(socket!!.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                    
                    var line: String?
                    while (running && reader.readLine().also { line = it } != null) {
                        val msg = ChatMessage.fromJson(line!!)
                        if (msg != null) onMessage(msg)
                    }
                } catch (e: Exception) {
                    if (running) {
                        onStatus("Menunggu Server siap...")
                        try { Thread.sleep(2000) } catch (_: InterruptedException) {}
                    }
                }
            }
        }.start()
    }

    fun send(msg: ChatMessage) {
        try { 
            if (writer == null) {
                onStatus("❌ Server belum siap, pesan gagal dikirim")
                return
            }
            writer?.println(msg.toJson()) 
        }
        catch (e: Exception) { onStatus("Gagal kirim: ${e.message}") }
    }

    fun stop() {
        running = false
        try { socket?.close() } catch (_: Exception) {}
    }
}
