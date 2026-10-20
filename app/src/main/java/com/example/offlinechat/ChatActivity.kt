package com.example.offlinechat

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
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
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
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
    private lateinit var emojiPanel: View

    private val handler = Handler(Looper.getMainLooper())
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var recordTimer: CountDownTimer? = null

    private val groupBots = listOf(
        Bot("Budi", android.graphics.Color.parseColor("#E57373"), listOf("Wkwk", "Iya bener", "Hahaha")),
        Bot("Sari", android.graphics.Color.parseColor("#BA68C8"), listOf("Hmm...", "Aku juga", "Oket deh")),
        Bot("Andi", android.graphics.Color.parseColor("#4DB6AC"), listOf("Kuy", "Gas!", "Bener banget"))
    )
    data class Bot(val name: String, val color: Int, val replies: List<String>)

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
        input = findViewById(R.id.messageInput)
        btnSend = findViewById(R.id.btnSend)
        emojiPanel = findViewById(R.id.emojiPanel)

        findViewById<TextView>(R.id.chatTitle).text = when {
            isGroupMode -> "Grup: Budi, Sari, Andi"
            isLoopback -> "Test Chat (1 HP)"
            else -> "Chat WFD: $wfdPeer"
        }

        // Load foto profil ke header
        val headerAvatar = findViewById<ImageView>(R.id.chatHeaderAvatar)
        val myAvatar = PrefsHelper.getAvatar(this)
        if (myAvatar != null && headerAvatar != null) {
            try {
                val bytes = Base64.decode(myAvatar, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                headerAvatar.setImageBitmap(bmp)
            } catch (e: Exception) {}
        }

        db = AppDatabase.get(this)
        adapter = MessageAdapter(isGroup = isGroupMode, onDelete = { timestamp ->
            lifecycleScope.launch {
                db.messageDao().deleteMessage(timestamp, chatId)
                val msgs = db.messageDao().getMessages(chatId)
                // Nanti kita benerin flow collect-nya
            }
        })
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        list.layoutManager = lm
        list.adapter = adapter

        setupEmojiPanel()
        applyWallpaper()

        lifecycleScope.launch {
            db.messageDao().getMessages(chatId).collect { entities ->
                val msgs = entities.map { it.toChatMessage() }
                adapter.setMessages(msgs)
                if (msgs.isNotEmpty()) list.scrollToPosition(adapter.itemCount - 1)
            }
        }

        if (isGroupMode) {
            mode = "group"
            statusText.text = "Grup simulasi"
        } else if (isLoopback) {
            mode = "loopback"
            statusText.text = "Mode test lokal"
        } else {
            mode = "tcp"
            isServer = wfdIsServer
            statusText.text = if (isServer) "TCP Server" else "TCP Client"
            if (isServer) startTcpServer() else startTcpClient()
        }

        val recordingLayout = findViewById<LinearLayout>(R.id.recordingLayout)
        val tvRecordingTimer = findViewById<TextView>(R.id.tvRecordingTimer)
        val btnStopRecord = findViewById<ImageButton>(R.id.btnStopRecord)
        val btnCancelRecord = findViewById<ImageButton>(R.id.btnCancelRecord)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                btnSend.setImageResource(if (s.isNullOrEmpty()) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_menu_send)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 100)
                    return@setOnClickListener
                }
                startRecording()
                recordingLayout.visibility = View.VISIBLE
                btnSend.setImageResource(android.R.drawable.ic_menu_save)
                btnStopRecord.setImageResource(android.R.drawable.ic_media_pause)
                recordTimer = object : CountDownTimer(60000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val sec = (60000 - millisUntilFinished) / 1000
                        tvRecordingTimer.text = String.format("%d:%02d", sec / 60, sec % 60)
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
            btnStopRecord.setImageResource(android.R.drawable.ic_media_play)
            recordTimer?.cancel()
        }
        btnCancelRecord.setOnClickListener {
            cancelRecording()
            recordingLayout.visibility = View.GONE
            btnSend.setImageResource(android.R.drawable.ic_btn_speak_now)
            recordTimer?.cancel()
        }

        findViewById<ImageButton>(R.id.btnEmoji).setOnClickListener { toggleEmojiPanel() }
        findViewById<ImageButton>(R.id.btnAttach).setOnClickListener { pickImage.launch("image/*") }
        findViewById<ImageButton>(R.id.btnCamera).setOnClickListener { pickImage.launch("image/*") }
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
                val msg = ChatMessage(myName, "", System.currentTimeMillis(), isMine = true, audioPath = it.absolutePath, audioDuration = 5)
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
            val brightness = PrefsHelper.getWpBrightness(this)
            val alpha = (brightness * 2.55).toInt().coerceIn(0, 255)
            val drawable = android.graphics.drawable.BitmapDrawable(resources, bmp)
            val overlay = android.graphics.drawable.ColorDrawable((alpha shl 24) or 0x000000)
            val layer = android.graphics.drawable.LayerDrawable(arrayOf(drawable, overlay))
            val root = findViewById<View>(android.R.id.content)
            (root as? android.view.ViewGroup)?.getChildAt(0)?.background = layer
        } catch (e: Exception) {}
    }

    private fun toggleEmojiPanel() {
        emojiPanel.visibility = if (emojiPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
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
                addAndSave(ChatMessage("Echo Bot", "Echo: $text", System.currentTimeMillis()))
            }, 600)
            else -> if (isServer) tcpServer?.send(msg) else tcpClient?.send(msg)
        }
    }

    private fun simulateGroupReplies(userText: String) {
        val repliers = groupBots.shuffled().take(Random.nextInt(1, groupBots.size + 1))
        var delay = 600L
        repliers.forEach { bot ->
            handler.postDelayed({
                val echo = ChatMessage(from = bot.name, text = bot.replies.random(), timestamp = System.currentTimeMillis(), senderColor = bot.color)
                addAndSave(echo)
            }, delay)
            delay += Random.nextLong(400, 1200)
        }
    }

    private fun handleImagePicked(uri: Uri) {
        lifecycleScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) {
                    val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return@withContext null
                    val original = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (original == null) return@withContext null
                    val maxSize = 800
                    val bmp = if (original.width > maxSize || original.height > maxSize) {
                        val ratio = original.width.toFloat() / original.height.toFloat()
                        val newW = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
                        val newH = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
                        Bitmap.createScaledBitmap(original, newW, newH, true)
                    } else original
                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                }
                if (base64 != null) {
                    val msg = ChatMessage(myName, "", System.currentTimeMillis(), isMine = true, imageData = base64)
                    addAndSave(msg)
                    if (mode == "loopback") {
                        handler.postDelayed({
                            addAndSave(ChatMessage("Echo Bot", "Gambar diterima! 📷", System.currentTimeMillis()))
                        }, 800)
                    }
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
        tcpServer = ChatServer(port = 8888,
            onMessage = { msg: ChatMessage -> runOnUiThread { addAndSave(msg.copy(isMine = false)) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
        )
        tcpServer?.start()
    }

    private fun startTcpClient() {
        tcpClient = ChatClient(serverIp = "192.168.49.1", port = 8888,
            onMessage = { msg: ChatMessage -> runOnUiThread { addAndSave(msg.copy(isMine = false)) } },
            onStatus = { s: String -> runOnUiThread { statusText.text = s } }
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
