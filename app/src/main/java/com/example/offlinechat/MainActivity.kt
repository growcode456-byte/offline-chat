package com.example.offlinechat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : BaseActivity() {

    private lateinit var chatList: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var emptyText: TextView
    private lateinit var statusText: TextView
    private lateinit var btnDarkMode: ImageButton
    private lateinit var fabNewChat: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var searchInput: EditText

    private var allChats = listOf<ChatPreview>()
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatList = findViewById(R.id.chatList)
        emptyText = findViewById(R.id.emptyText)
        statusText = findViewById(R.id.statusText)
        btnDarkMode = findViewById(R.id.btnDarkMode)
        fabNewChat = findViewById(R.id.fabNewChat)
        bottomNav = findViewById(R.id.bottomNav)
        searchInput = findViewById(R.id.searchInput)

        chatAdapter = ChatAdapter { preview -> openChat(preview) }
        chatList.layoutManager = LinearLayoutManager(this)
        chatList.adapter = chatAdapter

        btnDarkMode.setOnClickListener { toggleDarkMode() }
        fabNewChat.setOnClickListener { showNewChatDialog() }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                filterChats(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> { showChatTab(); true }
                R.id.nav_perangkat -> { showFragment(PerangkatFragment()); true }
                R.id.nav_setting -> { showFragment(SettingFragment()); true }
                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.nav_chat
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 0) loadChats()
    }

    private fun showChatTab() {
        currentTab = 0
        chatList.visibility = View.VISIBLE
        searchInput.visibility = View.VISIBLE
        fabNewChat.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE
        loadChats()
    }

    private fun showFragment(fragment: Fragment) {
        currentTab = 1
        chatList.visibility = View.GONE
        searchInput.visibility = View.GONE
        fabNewChat.visibility = View.GONE
        emptyText.visibility = View.GONE
        statusText.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun loadChats() {
        val db = AppDatabase.get(this)
        lifecycleScope.launch {
            try {
                val prefs = PrefsHelper.getName(this@MainActivity)
                val list = mutableListOf<ChatPreview>()

                // Loopback chat
                val loopback = db.messageDao().getLastMessage("loopback")
                if (loopback != null) {
                    list.add(ChatPreview(
                        chatId = "loopback",
                        title = "Test Chat (1 HP)",
                        lastMessage = loopback.text.ifEmpty { "[Gambar]" },
                        lastTimestamp = loopback.timestamp,
                        isGroup = false,
                        colorAccent = 0xFF6A1B9A.toInt()
                    ))
                }

                // Group chat
                val group = db.messageDao().getLastMessage("group_sim")
                if (group != null) {
                    list.add(ChatPreview(
                        chatId = "group_sim",
                        title = "Grup (Budi, Sari, Andi)",
                        lastMessage = "${group.from}: ${group.text.ifEmpty { "[Gambar]" }}",
                        lastTimestamp = group.timestamp,
                        isGroup = true,
                        colorAccent = 0xFF00897B.toInt()
                    ))
                }

                allChats = list.sortedByDescending { it.lastTimestamp }
                applyFilter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun filterChats(query: String) {
        applyFilter(query)
    }

    private fun applyFilter(query: String = searchInput.text.toString()) {
        val filtered = if (query.isBlank()) allChats
        else allChats.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.lastMessage.contains(query, ignoreCase = true)
        }

        chatAdapter.update(filtered)
        if (filtered.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            emptyText.text = if (allChats.isEmpty())
                "Belum ada chat.\nTap tombol + untuk mulai."
            else "Chat gak ditemukan"
        } else {
            emptyText.visibility = View.GONE
        }
    }

    private fun openChat(preview: ChatPreview) {
        startActivity(Intent(this, ChatActivity::class.java).apply {
            putExtra("myName", PrefsHelper.getName(this@MainActivity))
            when (preview.chatId) {
                "loopback" -> putExtra("loopback", true)
                "group_sim" -> putExtra("group", true)
            }
        })
    }

    private fun showNewChatDialog() {
        val options = arrayOf(
            "Test Chat (1 HP)",
            "Test Grup (3 Bot)",
            "Cari Perangkat (WiFi Direct)"
        )
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mulai Chat Baru")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, ChatActivity::class.java).apply {
                        putExtra("loopback", true)
                        putExtra("myName", PrefsHelper.getName(this))
                    })
                    1 -> startActivity(Intent(this, ChatActivity::class.java).apply {
                        putExtra("group", true)
                        putExtra("myName", PrefsHelper.getName(this))
                    })
                    2 -> {
                        bottomNav.selectedItemId = R.id.nav_perangkat
                    }
                }
            }
            .show()
    }

    private fun toggleDarkMode() {
        val current = PrefsHelper.getDarkMode(this)
        val next = when (current) { -1 -> 0; 0 -> 1; else -> -1 }
        PrefsHelper.setDarkMode(this, next)
        val label = when (next) { -1 -> "Ikut Sistem"; 0 -> "Mode Terang"; else -> "Mode Gelap" }
        android.widget.Toast.makeText(this, label, android.widget.Toast.LENGTH_SHORT).show()
        when (next) {
            -1 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            0 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            1 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        }
        recreate()
    }
}
