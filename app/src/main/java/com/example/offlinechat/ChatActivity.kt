package com.example.offlinechat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.media.MediaRecorder
import android.os.CountDownTimer
import java.io.File
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
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
    private lateinit var input: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnEmoji: ImageButton
    private lateinit var btnAttach: ImageButton
    private lateinit var btnCamera: ImageButton
    private lateinit var emojiPanel: View

    private val handler = Handler(Looper.getMainLooper())
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var recordTimer: CountDownTimer? = null

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
    ) { uri: Uri? -> if (uri != null) handleImagePicked(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val isLoopback = intent.getBooleanExtra("loopback", false)
        val isGroupMode = intent.getBooleanExtra("group", false)
        val isHotspotMode = intent.getBooleanExtra("hotspotMode", false)
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
        input = findViewById(R.id.messageInput)
        btnSend = findViewById(R.id.btnSend)
        btnEmoji = findViewById(R.id.btnEmoji)
        btnAttach = findViewById(R.id.btnAttach)
        btnCamera = findViewById(R.id.btnCamera)
        emojiPanel = findViewById(R.id.emojiPanel)

        findViewById<TextView>(R.id.chatTitle).text = when {
            isGroupMode -> "Grup: Budi, Sari, Andi"
            isLoopback -> "Test Chat (1 HP)"
            else -> "Chat WFD: $wfdPeer"
        }

        db = AppDatabase.get(this)

        adapter = MessageAdapter(isGroup = isGroupMode, onDelete = { timestamp ->
            lifecycleScope.launch {
                db.messageDao().deleteMessage(timestamp, chatId)
            }
        })
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        list.layoutManager = lm
        list.adapter = adapter

        setupEmojiPanel()
        applyWallpaper()
        requestStoragePermission()

        lifecycleScope.launch {
            db.messageDao().getMessages(chatId).collect { entities ->
                val msgs = entities.map { it.toChatMessage() }
                adapter.setMessages(msgs)
                if (msgs.isNotEmpty()) list.scrollToPosition(adapter.itemCount - 1)
            }
        }

        if (isHotspotMode) {
            mode = "tcp"
            statusText.text = "Mode Hotspot (Client). Mencari IP..."
            startTcpClientCustom("192.168.43.1") // IP Standar Hotspot Android
        } else if (isGroupMode) {
            mode = "group"
            statusText.text = "Grup simulasi • ${groupBots.size} anggota"
        } else if (isLoopback) {
            mode = "loopback"
            statusText.text = "Mode test lokal"
        } else {
            mode = "tcp"
            isServer = wfdIsServer
            
            // Trik: Coba jadi Client dulu, kalau gagal jadi Server, kalau gagal lagi nyoba Client.
            // Pokoknya dua-duanya bakal nyambung.
            if (isServer) {
                statusText.text = "Menyalakan Server..."
                startTcpServer()
            } else {
                statusText.text = "Menghubungkan ke Server..."
                startTcpClient()
                
                // Kalau 5 detik gak nyambung, coba jadi Server
                handler.postDelayed({
                    if (!statusText.text.contains("Tersambung") && !statusText.text.contains("Client tersambung")) {
                        statusText.text = "Mencoba jadi Server..."
                        startTcpServer()
                    }
                }, 5000)
            }
        }

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                updateSendButton(s?.toString().orEmpty().isNotEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        updateSendButton(false)

        // Handle tombol send / mic
        val recordingLayout = findViewById<LinearLayout>(R.id.recordingLayout)
        val tvRecordingTimer = findViewById<TextView>(R.id.tvRecordingTimer)
        val btnStopRecord = findViewById<Button>(R.id.btnStopRecord)
        val btnCancelRecord = findViewById<Button>(R.id.btnCancelRecord)

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) {
                // MULAI REKAM
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 100)
                    return@setOnClickListener
                }
                startRecording()
                recordingLayout.visibility = View.VISIBLE
                btnSend.setImageResource(android.R.drawable.ic_menu_save) // Ganti icon
                
                recordTimer = object : CountDownTimer(60000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val sec = (60000 - millisUntilFinished) / 1000
                        tvRecordingTimer.text = String.format("🔴 00:%02d", sec)
                    }
                    override fun onFinish() {
                        stopRecording()
                        recordingLayout.visibility = View.GONE
                        btnSend.setImageResource(android.R.drawable.ic_btn_speak_now)
                    }
                }.start()
            } else {
                sendMessage(text)
                input.setText("")
            }
        }

        btnStopRecord.setOnClickListener {
            stopRecording()
            recordingLayout.visibility = View.GONE
            btnSend.setImageResource(android.R.drawable.ic_btn_speak_now)
            recordTimer?.cancel()
        }

        btnCancelRecord.setOnClickListener {
            cancelRecording()
            recordingLayout.visibility = View.GONE
            btnSend.setImageResource(android.R.drawable.ic_btn_speak_now)
            recordTimer?.cancel()
        }

        btnEmoji.setOnClickListener { toggleEmojiPanel() }
        btnAttach.setOnClickListener { pickImage.launch("image/*") }
        btnCamera.setOnClickListener { pickImage.launch("image/*") }

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

    private fun requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    5001
                )
            }
        }
    }

    private fun startRecording() {
        try {
            audioFile = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Toast.makeText(this, "Merekam...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal rekam: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            
            audioFile?.let {
                val msg = ChatMessage(
                    from = myName,
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    isMine = true,
                    audioPath = it.absolutePath,
                    audioDuration = 5 // Placeholder durasi
                )
                addAndSave(msg)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal simpan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            audioFile?.delete()
        } catch (e: Exception) {}
    }

    private fun applyWallpaper() {
        val wp = PrefsHelper.getWallpaper(this) ?: return
        try {
            val bytes = Base64.decode(wp, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return

            // Bikin drawable dengan overlay gelap
            val brightness = PrefsHelper.getWpBrightness(this)
            val alpha = (brightness * 2.55).toInt().coerceIn(0, 255)

            val drawable = BitmapDrawable(resources, bmp)
            val overlay = android.graphics.drawable.ColorDrawable(
                (alpha shl 24) or 0x000000
            )
            val layer = android.graphics.drawable.LayerDrawable(
                arrayOf(drawable, overlay)
            )

            val root = findViewById<View>(android.R.id.content)
            if (root is LinearLayout) {
                root.background = layer
            } else if (root is android.view.ViewGroup) {
                val child = root.getChildAt(0)
                if (child is LinearLayout) {
                    child.background = layer
                }
            }
        } catch (e: Exception) {}
    }

    private fun updateSendButton(hasText: Boolean) {
        btnSend.setImageResource(
            if (hasText) android.R.drawable.ic_menu_send
            else android.R.drawable.ic_btn_speak_now
        )
    }

    private fun toggleEmojiPanel() {
        if (emojiPanel.visibility == View.VISIBLE) emojiPanel.visibility = View.GONE
        else emojiPanel.visibility = View.VISIBLE
    }

    private fun setupEmojiPanel() {
        val grid = emojiPanel.findViewById<RecyclerView>(R.id.emojiGrid)
        grid.layoutManager = GridLayoutManager(this, 8)
        grid.adapter = EmojiAdapter(EmojiPicker.ALL) { emoji ->
            val cur = input.text.toString()
            val sel = input.selectionStart.coerceAtLeast(0)
            val newText = cur.substring(0, sel) + emoji + cur.substring(sel)
            input.setText(newText)
            input.setSelection(sel + emoji.length)
        }
        emojiPanel.findViewById<View>(R.id.btnCloseEmoji).setOnClickListener {
            emojiPanel.visibility = View.GONE
        }
        emojiPanel.visibility = View.GONE
    }

    private fun sendMessage(text: String) {
        val msg = ChatMessage(myName, text, System.currentTimeMillis(), isMine = true)
        addAndSave(msg)
        when (mode) {
            "group" -> simulateGroupReplies(text)
            "loopback" -> handler.postDelayed({
                val echo = ChatMessage("Echo Bot", "Echo: $text", System.currentTimeMillis())
                addAndSave(echo)
            }, 600)
            else -> if (isServer) tcpServer?.send(msg) else tcpClient?.send(msg)
        }
    }

    private fun simulateGroupReplies(userText: String) {
        val repliers = groupBots.shuffled().take(Random.nextInt(1, groupBots.size + 1))
        var delay = 600L
        repliers.forEach { bot ->
            handler.postDelayed({
                val reply = when {
                    userText.contains("halo", ignoreCase = true) -> "Halo juga! 👋"
                    userText.contains("apa kabar", ignoreCase = true) -> "Baik dong!"
                    else -> bot.replies.random()
                }
                val echo = ChatMessage(
                    from = bot.name, text = reply,
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
                val base64 = withContext(Dispatchers.IO) {
                    val inputStream: InputStream = contentResolver.openInputStream(uri)
                        ?: return@withContext null
                    val original = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (original == null) return@withContext null

                    val maxSize = 800
                    val bmp = if (original.width > maxSize || original.height > maxSize) {
                        val ratio = original.width.toFloat() / original.height.toFloat()
                        val newW: Int
                        val newH: Int
                        if (ratio > 1) { newW = maxSize; newH = (maxSize / ratio).toInt() }
                        else { newH = maxSize; newW = (maxSize * ratio).toInt() }
                        Bitmap.createScaledBitmap(original, newW, newH, true)
                    } else original

                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                }

                if (base64 == null) {
                    Toast.makeText(this@ChatActivity, "Gagal baca gambar", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val msg = ChatMessage(
                    from = myName, text = "",
                    timestamp = System.currentTimeMillis(),
                    isMine = true, imageData = base64
                )
                addAndSave(msg)

                when (mode) {
                    "group" -> handler.postDelayed({
                        val bot = groupBots.random()
                        val echo = ChatMessage(
                            from = bot.name, text = "Gambar keren! 📷",
                            timestamp = System.currentTimeMillis(),
                            senderColor = bot.color
                        )
                        addAndSave(echo)
                    }, 800)
                    "loopback" -> handler.postDelayed({
                        val echo = ChatMessage("Echo Bot", "Gambar diterima! 📷",
                            System.currentTimeMillis())
                        addAndSave(echo)
                    }, 800)
                    else -> if (isServer) tcpServer?.send(msg) else tcpClient?.send(msg)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    private fun startTcpClientCustom(ip: String) {
        tcpClient = ChatClient(
            serverIp = ip,
            port = 8888,
            onMessage = { msg: ChatMessage -> runOnUiThread { addAndSave(msg.copy(isMine = false)) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
        )
        tcpClient?.connect()
    }

    private fun startTcpClient() {
        // Ambil IP Server dari WifiP2pManager (biasanya 192.168.49.1)
        val serverIp = "192.168.49.1" 
        
        tcpClient = ChatClient(
            serverIp = serverIp,
            port = 8888,
            onMessage = { msg: ChatMessage -> runOnUiThread { addAndSave(msg.copy(isMine = false)) } },
            onStatus = { s: String -> runOnUiThread { 
                statusText.text = s 
                // Kalau connection refused, coba lagi 2 detik kemudian
                if (s.contains("refused", ignoreCase = true) || s.contains("timeout", ignoreCase = true)) {
                    handler.postDelayed({
                        statusText.text = "Mencoba ulang connect..."
                        tcpClient?.connect()
                    }, 2000)
                }
            } }
        )
        tcpClient?.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        tcpServer?.stop()
        tcpClient?.stop()
    }
}
