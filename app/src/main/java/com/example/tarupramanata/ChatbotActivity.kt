package com.example.tarupramanata

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class ChatbotActivity : AppCompatActivity() {


    private val BASE_URL = "https://krisnadipa-chatbot-tarupramana.hf.space/"

    // Variabel UI
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollViewChat: NestedScrollView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: CardView
    private lateinit var btnHistory: ImageView

    // Variabel Sidebar (Drawer)
    private lateinit var drawerHistoryContainer: LinearLayout
    private lateinit var btnNewChat: Button
    private lateinit var btnClearHistory: Button

    // Variabel Penyimpanan
    private var allSessions: MutableList<ChatSession> = mutableListOf()
    private var currentSessionId: String? = null
    private val PREF_NAME = "ChatPrefs"
    private val KEY_SESSIONS = "chat_sessions"
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        // 1. INIT UI
        drawerLayout = findViewById(R.id.drawerLayout)
        chatContainer = findViewById(R.id.chatContainer)
        scrollViewChat = findViewById(R.id.scrollViewChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnHistory = findViewById(R.id.btnHistory)

        drawerHistoryContainer = findViewById(R.id.drawerHistoryContainer)
        btnNewChat = findViewById(R.id.btnNewChat)
        btnClearHistory = findViewById(R.id.btnClearHistory)

        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val btnScan = findViewById<CardView>(R.id.btnScan)
        val tvChip1 = findViewById<TextView>(R.id.tvChip1)
        val tvChip2 = findViewById<TextView>(R.id.tvChip2)

        // 2. LOAD RIWAYAT
        loadAllSessions()

        // 3. SELALU MULAI SESI BARU SAAT APLIKASI DIBUKA
        startNewSession()
        updateDrawerUI()

        // 4. NAVIGASI
        navHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        btnScan.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        // 5. LISTENER TOMBOL
        btnSend.setOnClickListener { sendMessage() }
        tvChip1.setOnClickListener { etMessage.setText(tvChip1.text.toString()); sendMessage() }
        tvChip2.setOnClickListener { etMessage.setText(tvChip2.text.toString()); sendMessage() }

        btnHistory.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        btnNewChat.setOnClickListener {
            startNewSession()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // --- ALERT CUSTOM: HAPUS SEMUA RIWAYAT ---
        btnClearHistory.setOnClickListener {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_custom_confirm)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
            val tvMessage = dialog.findViewById<TextView>(R.id.tvDialogMessage)
            val btnCancel = dialog.findViewById<Button>(R.id.btnDialogCancel)
            val btnConfirm = dialog.findViewById<Button>(R.id.btnDialogConfirm)

            tvTitle.text = "Hapus Semua Riwayat"
            tvMessage.text = "Apakah Anda yakin ingin menghapus seluruh riwayat chat?"

            btnCancel.setOnClickListener { dialog.dismiss() }

            btnConfirm.setOnClickListener {
                allSessions.clear()
                saveAllSessions()
                startNewSession()
                drawerLayout.closeDrawer(GravityCompat.END)
                Toast.makeText(this, "Seluruh riwayat dihapus", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    // ==========================================
    // LOGIKA SIDEBAR RIWAYAT CHAT
    // ==========================================
    private fun updateDrawerUI() {
        drawerHistoryContainer.removeAllViews()

        if (allSessions.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Belum ada riwayat chat."
                setTextColor(Color.GRAY)
                textSize = 14f
                setPadding(16, 16, 16, 16)
            }
            drawerHistoryContainer.addView(tvEmpty)
            return
        }

        for (session in allSessions.reversed()) {
            val itemBox = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_outline_rounded)
                setPadding(32, 24, 32, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
            }

            val textContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    loadChatSessionToUI(session.id)
                    drawerLayout.closeDrawer(GravityCompat.END)
                }
            }

            val tvTitle = TextView(this).apply {
                text = session.title
                setTextColor(Color.parseColor("#333333"))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            val tvDate = TextView(this).apply {
                text = session.date
                setTextColor(Color.parseColor("#888888"))
                textSize = 11f
                setPadding(0, 4, 0, 0)
            }

            textContainer.addView(tvTitle)
            textContainer.addView(tvDate)

            val btnEdit = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                setColorFilter(Color.parseColor("#888888"))
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { marginStart = 16 }
                setPadding(8, 8, 8, 8)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    showEditTitleDialog(session)
                }
            }

            val btnDelete = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setColorFilter(Color.parseColor("#D32F2F"))
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { marginStart = 16 }
                setPadding(8, 8, 8, 8)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    showDeleteConfirmDialog(session)
                }
            }

            itemBox.addView(textContainer)
            itemBox.addView(btnEdit)
            itemBox.addView(btnDelete)

            drawerHistoryContainer.addView(itemBox)
        }
    }

    private fun showEditTitleDialog(session: ChatSession) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_custom_input)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val etInput = dialog.findViewById<EditText>(R.id.etDialogInput)
        val btnCancel = dialog.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnDialogSave)

        etInput.setText(session.title)
        etInput.setSelection(etInput.text.length)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newTitle = etInput.text.toString().trim()
            if (newTitle.isNotEmpty()) {
                val index = allSessions.indexOfFirst { it.id == session.id }
                if (index != -1) {
                    val updatedSession = allSessions[index].copy(title = newTitle)
                    allSessions[index] = updatedSession
                    saveAllSessions()
                }
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDeleteConfirmDialog(session: ChatSession) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_custom_confirm)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = dialog.findViewById<Button>(R.id.btnDialogCancel)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnDialogConfirm)

        tvTitle.text = "Hapus Chat"
        tvMessage.text = "Apakah Anda yakin ingin menghapus chat '${session.title}'?"

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            allSessions.removeIf { it.id == session.id }
            saveAllSessions()

            if (currentSessionId == session.id) {
                startNewSession()
            }
            Toast.makeText(this, "Chat dihapus", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    // ==========================================
    // LOGIKA PENYIMPANAN
    // ==========================================
    private fun saveAllSessions() {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(allSessions)
        prefs.edit().putString(KEY_SESSIONS, json).apply()
        updateDrawerUI()
    }

    private fun loadAllSessions() {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SESSIONS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<MutableList<ChatSession>>() {}.type
                allSessions = gson.fromJson(json, type)
            } catch (e: Exception) {
                allSessions = mutableListOf()
            }
        } else {
            allSessions = mutableListOf()
        }
    }

    private fun startNewSession() {
        currentSessionId = UUID.randomUUID().toString()
        chatContainer.removeAllViews()
        addChatBubble("Halo! Saya BOTARU \nAda yang bisa saya bantu?", isUser = false)
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return sdf.format(Date())
    }

    private fun saveMessageToCurrentSession(text: String, isUser: Boolean) {
        if (currentSessionId == null) startNewSession()

        var session = allSessions.find { it.id == currentSessionId }

        if (session == null) {
            val title = if (text.length > 20) text.substring(0, 20) + "..." else text
            val currentDate = getCurrentDateString()
            session = ChatSession(currentSessionId!!, title, currentDate, mutableListOf())
            allSessions.add(session)
        }

        session.messages.add(Message(text, isUser))
        saveAllSessions()
    }

    private fun loadChatSessionToUI(sessionId: String) {
        currentSessionId = sessionId
        chatContainer.removeAllViews()

        val session = allSessions.find { it.id == sessionId }
        if (session != null) {
            for (msg in session.messages) {
                addChatBubble(msg.text, msg.isUser)
            }
        }
    }

    // ==========================================
    // LOGIKA KIRIM PESAN & API (DIPERBAIKI DENGAN TIMEOUT 60 DETIK)
    // ==========================================
    private fun sendMessage() {
        val message = etMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            addChatBubble(message, isUser = true)
            saveMessageToCurrentSession(message, isUser = true)
            etMessage.text.clear()

            val typingView = showTypingIndicator()
            sendToApi(message, typingView)
        }
    }

    private fun sendToApi(question: String, typingView: View) {
        val startTime = System.currentTimeMillis()

        // MENGATUR WAKTU TUNGGU MENJADI 60 DETIK (Mencegah Gagal Koneksi Terlalu Cepat)
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client) // Memasukkan pengaturan waktu tunggu ke Retrofit
            .build()

        val service = retrofit.create(ChatApiService::class.java)

        service.sendMessage(ChatRequest(question)).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                val apiTime = System.currentTimeMillis() - startTime
                val runtime = Runtime.getRuntime()
                val ramUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                android.util.Log.i("PerformanceTest", "Hugging Face Chatbot API Response Time: $apiTime ms | RAM Used: $ramUsed MB")
                chatContainer.removeView(typingView)

                val reply = if (response.isSuccessful) {
                    response.body()?.reply ?: "Maaf, respon kosong."
                } else {
                    "Error Server (${response.code()}): Gagal menghubungi asisten AI."
                }

                addChatBubble(reply, isUser = false)
                saveMessageToCurrentSession(reply, isUser = false)
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                val apiTime = System.currentTimeMillis() - startTime
                val runtime = Runtime.getRuntime()
                val ramUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                android.util.Log.e("PerformanceTest", "Hugging Face Chatbot API Failed in $apiTime ms | RAM Used: $ramUsed MB", t)
                chatContainer.removeView(typingView)
                t.printStackTrace() // Print error ke Logcat untuk mempermudah debugging
                val failMsg = "Gagal koneksi: Waktu tunggu habis atau server sedang offline."
                addChatBubble(failMsg, isUser = false)
                saveMessageToCurrentSession(failMsg, isUser = false)
            }
        })
    }

    // ==========================================
    // HELPER UI
    // ==========================================
    private fun addChatBubble(message: String, isUser: Boolean) {
        val inflater = LayoutInflater.from(this)
        val view: View = if (isUser) {
            inflater.inflate(R.layout.item_chat_user, chatContainer, false)
        } else {
            inflater.inflate(R.layout.item_chat_bot, chatContainer, false)
        }

        val tvMsg = view.findViewById<TextView>(R.id.tvMessage)
        tvMsg.text = message

        chatContainer.addView(view)
        scrollToBottom()
    }

    private fun showTypingIndicator(): View {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.item_chat_bot, chatContainer, false)

        val tvMsg = view.findViewById<TextView>(R.id.tvMessage)
        tvMsg.text = "Sedang mengetik..."
        tvMsg.setTypeface(null, Typeface.ITALIC)
        tvMsg.alpha = 0.6f

        chatContainer.addView(view)
        scrollToBottom()
        return view
    }

    private fun scrollToBottom() {
        scrollViewChat.post {
            scrollViewChat.fullScroll(View.FOCUS_DOWN)
        }
    }
}