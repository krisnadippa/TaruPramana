package com.example.tarupramanata

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

import com.bumptech.glide.Glide
// 1. Tambahkan parameter 'method' untuk menyimpan teknik pengolahan
data class TutorialStepDisplay(
    val description: String,
    val imageUrl: String,
    val method: String
)

class TutorialActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvStepCounter: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var imgStep: ImageView
    private lateinit var tvDescription: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNextContainer: CardView
    private lateinit var tvBtnNext: TextView
    private lateinit var btnBack: ImageView

    // 2. Deklarasi View untuk Pengolahan
    private lateinit var tvLabelPengolahan: TextView
    private lateinit var containerPengolahan: LinearLayout

    private var currentStepIndex = 0
    private var steps: List<TutorialStepDisplay> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        // 3. Init Views
        tvTitle = findViewById(R.id.tvTitle)
        tvStepCounter = findViewById(R.id.tvStepCounter)
        progressBar = findViewById(R.id.progressBar)
        imgStep = findViewById(R.id.imgStep)
        tvDescription = findViewById(R.id.tvDescription)
        btnPrev = findViewById(R.id.btnPrev)
        btnNextContainer = findViewById(R.id.btnNextContainer)
        tvBtnNext = findViewById(R.id.tvBtnNext)
        btnBack = findViewById(R.id.btnBack)

        // Init view teknik pengolahan
        tvLabelPengolahan = findViewById(R.id.tvLabelPengolahan)
        containerPengolahan = findViewById(R.id.containerPengolahan)

        // 4. AMBIL DATA DARI INTENT
        val titleString = intent.getStringExtra("EXTRA_TUTORIAL_TITLE") ?: "Tutorial Masak"
        val descList = intent.getStringArrayListExtra("STEPS_DESC") ?: emptyList()
        val imgList = intent.getStringArrayListExtra("STEPS_IMG") ?: emptyList()

        // Ambil Data Teknik Pengolahan (misal: "Dicuci", "Digerus")
        val methodList = intent.getStringArrayListExtra("STEPS_METHODS") ?: emptyList()

        tvTitle.text = titleString

        // Konversi String ke TutorialStepDisplay Object
        val dynamicSteps = ArrayList<TutorialStepDisplay>()
        for (i in descList.indices) {
            val imgName = if (i < imgList.size) imgList[i] else ""

            // Mengambil teknik pengolahan sesuai index langkah (jika datanya ada)
            val currentMethod = if (i < methodList.size) methodList[i] else ""

            val imageUrl = getSupabaseImageUrl("tutorial", imgName)

            // Masukkan currentMethod ke dalam object
            dynamicSteps.add(TutorialStepDisplay(descList[i], imageUrl, currentMethod))
        }
        steps = dynamicSteps

        // Cek jika data kosong
        if (steps.isEmpty()) {
            tvDescription.text = "Tidak ada langkah tutorial."
            btnNextContainer.visibility = View.GONE
            return
        }

        // Setup Progress Bar
        progressBar.max = steps.size

        // Load Langkah Pertama
        updateUI()

        // Navigasi Next/Prev
        btnNextContainer.setOnClickListener {
            if (currentStepIndex < steps.size - 1) {
                currentStepIndex++
                updateUI()
            } else {
                finish() // Selesai
            }
        }

        btnPrev.setOnClickListener {
            if (currentStepIndex > 0) {
                currentStepIndex--
                updateUI()
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun updateUI() {
        val currentStep = steps[currentStepIndex]

        // Update Data UI Utama
        tvStepCounter.text = "Langkah ${currentStepIndex + 1} dari ${steps.size}"
        progressBar.progress = currentStepIndex + 1
        tvDescription.text = currentStep.description
        
        imgStep.setImageDrawable(null)
        if (currentStep.imageUrl.isNotEmpty()) {
            Glide.with(this).load(currentStep.imageUrl).into(imgStep)
        }

        // --- 5. LOGIKA MEMBUAT TOMBOL TEKNIK PENGOLAHAN ---
        containerPengolahan.removeAllViews() // Bersihkan tombol dari langkah sebelumnya

        if (currentStep.method.isNotEmpty()) {
            // Jika ada teknik pengolahan, tampilkan label dan container
            tvLabelPengolahan.visibility = View.VISIBLE
            containerPengolahan.visibility = View.VISIBLE

            // Pecah string jika ada lebih dari 1 teknik (misal: "Dicuci, Dipotong")
            val methods = currentStep.method.split(",").map { it.trim() }

            for (method in methods) {
                if (method.isEmpty()) continue

                // Membuat Kotak Klik yang Rapi
                val itemBox = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(R.drawable.bg_outline_rounded)
                    isClickable = true
                    isFocusable = true

                    // Padding dalam kotak agar luas dan rapi
                    setPadding(40, 32, 40, 32)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 16) // Jarak bawah antar kotak
                    layoutParams = params

                    // Fungsi klik -> Ke halaman SearchActivity
                    setOnClickListener {
                        val searchIntent = Intent(this@TutorialActivity, SearchActivity::class.java)
                        searchIntent.putExtra("TARGET_FILTER", method)
                        startActivity(searchIntent)
                    }
                }

                // Teks Nama Teknik (Misal: "Digerus")
                val tvMethodName = TextView(this).apply {
                    text = method
                    setTextColor(Color.parseColor("#333333"))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                // Teks "Cari >" di sebelah kanan
                val tvActionCari = TextView(this).apply {
                    text = "Cari \u276F"
                    setTextColor(Color.parseColor("#F57C00"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Masukkan teks ke dalam kotak, lalu masukkan kotak ke container
                itemBox.addView(tvMethodName)
                itemBox.addView(tvActionCari)
                containerPengolahan.addView(itemBox)
            }
        } else {
            // Jika langkah ini tidak punya teknik pengolahan, sembunyikan area tersebut
            tvLabelPengolahan.visibility = View.GONE
            containerPengolahan.visibility = View.GONE
        }
        // --- SELESAI LOGIKA TEKNIK PENGOLAHAN ---

        // Logika Tombol Prev
        if (currentStepIndex == 0) {
            btnPrev.visibility = View.INVISIBLE
            btnPrev.isEnabled = false
        } else {
            btnPrev.visibility = View.VISIBLE
            btnPrev.isEnabled = true
        }

        // Logika Tombol Next
        if (currentStepIndex == steps.size - 1) {
            tvBtnNext.text = "Selesai"
        } else {
            tvBtnNext.text = "selanjutnya"
        }
    }
}