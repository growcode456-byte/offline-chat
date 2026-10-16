package com.example.offlinechat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private lateinit var chatList: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var emptyText: TextView
    private lateinit var statusText: TextView
    private lateinit var btnDarkMode: ImageButton
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
        bottomNav = findViewById(R.id.bottomNav)
        searchInput = findViewById(R.id.searchInput)

        chatAdapter = ChatAdapter { preview -> openChat(preview) }
        chatList.layoutManager = LinearLayoutManager(this)
        chatList.adapter = chatAdapter

        btnDarkMode.setOnClickListener { toggleDarkMode() }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                applyFilter(s?.toString() ?: "")
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
        statusText.text = PrefsHelper.getStatus(this)
        if (currentTab == 0) loadChats()
    }

    private fun showChatTab() {
        currentTab = 0

        // HAPUS fragment yang ada di container
        val existing = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (existing != null) {
            supportFragmentManager.beginTransaction()
                .remove(existing)
                .commit()
        }

        chatList.visibility = View.VISIBLE
        searchInput.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE

        loadChats()
    }

    private fun showFragment(fragment: Fragment) {
        currentTab = 1

        chatList.visibility = View.GONE
        searchInput.visibility = View.GONE
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
                val list = mutableListOf<ChatPreview>()

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
                "Belum ada chat.\nBuka Setting -> Test Chat untuk mulai."
            else "Chat gak ditemukan"
        } else {
            emptyText.visibility = View.GONE
        }
    }

    private fun openChat(preview: ChatPreview) {
        val intent = Intent(this@MainActivity, ChatActivity::class.java)
        intent.putExtra("myName", PrefsHelper.getName(this@MainActivity))
        when (preview.chatId) {
            "loopback" -> intent.putExtra("loopback", true)
            "group_sim" -> intent.putExtra("group", true)
        }
        startActivity(intent)
    }

    private fun toggleDarkMode() {
        val current = PrefsHelper.getDarkMode(this)
        val next = when (current) { -1 -> 0; 0 -> 1; else -> -1 }
        PrefsHelper.setDarkMode(this, next)
        val label = when (next) { -1 -> "Ikut Sistem"; 0 -> "Mode Terang"; else -> "Mode Gelap" }
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
        when (next) {
            -1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        recreate()
    }
}
