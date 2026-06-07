package com.example.tarupramanata

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tarupramanata.repository.TaruPramanaRepository
import kotlinx.coroutines.launch
import java.io.File

class DetailScanActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tabResep: LinearLayout
    private lateinit var tabTanaman: LinearLayout
    private lateinit var tvResep: TextView
    private lateinit var tvTanaman: TextView
    private lateinit var lineResep: View
    private lateinit var lineTanaman: View
    private lateinit var rvRelatedContent: RecyclerView

    // UI Hasil Deteksi
    private lateinit var imgResult: ImageView
    private lateinit var tvPlantName: TextView
    private lateinit var tvConfidence: TextView

    private lateinit var articleAdapter: ArticleAdapter

    private var detectedName: String = ""
    private var allArticles: List<Article> = emptyList()
    private val repository = TaruPramanaRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_scan)

        // 1. LOAD DATA FROM DATABASE
        loadDataFromSupabase()

        // 2. INIT VIEWS
        imgResult = findViewById(R.id.imgResult)
        tvPlantName = findViewById(R.id.tvPlantName)
        tvConfidence = findViewById(R.id.tvConfidence)

        tabResep = findViewById(R.id.tabResep)
        tabTanaman = findViewById(R.id.tabTanaman)
        tvResep = findViewById(R.id.tvResep)
        tvTanaman = findViewById(R.id.tvTanaman)
        lineResep = findViewById(R.id.lineResep)
        lineTanaman = findViewById(R.id.lineTanaman)
        rvRelatedContent = findViewById(R.id.rvRelatedContent)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navChatbot = findViewById<LinearLayout>(R.id.navChatbot)
        val btnScan = findViewById<CardView>(R.id.btnScan)

        // 3. SET GAMBAR
        val imagePath = intent.getStringExtra("IMAGE_PATH")
        if (imagePath != null) {
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                imgResult.setImageBitmap(bitmap)
            }
        }

        // 4. AMBIL DATA DETEKSI
        val rawDetectedName = intent.getStringExtra("DETECTED_NAME") ?: "Tidak Dikenali"
        val detectedConfidence = intent.getStringExtra("DETECTED_CONFIDENCE") ?: "0%"
        val isLowAccuracy = intent.getBooleanExtra("IS_LOW_ACCURACY", false)

        val imgCheckStatus = findViewById<ImageView>(R.id.imgCheckStatus)
        if (isLowAccuracy || rawDetectedName.equals("Tidak Dikenali", ignoreCase = true)) {
            imgCheckStatus.setImageResource(R.drawable.ic_cancel_circle)
            imgCheckStatus.setColorFilter(Color.parseColor("#D32F2F"))
        } else {
            imgCheckStatus.setImageResource(R.drawable.ic_check_circle)
            imgCheckStatus.setColorFilter(Color.parseColor("#4A6F28"))
        }

        tvPlantName.text = rawDetectedName
        tvConfidence.text = "Tingkat kepercayaan: $detectedConfidence"

        // Bersihkan Nama
        detectedName = rawDetectedName.trim().replace("_", " ")

        // 5. SETUP ADAPTER
        rvRelatedContent.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter(emptyList()) { article ->
            if (article.category.equals("Resep", ignoreCase = true) || article.category.equals("Kuliner", ignoreCase = true)) {
                val intent = Intent(this, DetailResepActivity::class.java)
                intent.putExtra("EXTRA_TITLE", article.title)
                intent.putExtra("EXTRA_CATEGORY", article.category)
                intent.putExtra("EXTRA_DATE", article.date)
                intent.putExtra("EXTRA_CONTENT", article.content)
                intent.putExtra("EXTRA_IMAGE_URL", article.imageUrl)
                intent.putExtra("EXTRA_INGREDIENTS", article.bahan) // <<< SINKRONISASI: pakai bahan
                intent.putExtra("EXTRA_TAGS", article.tags)
                intent.putExtra("EXTRA_AUTHOR", article.author)
                startActivity(intent)
            } else {
                val intent = Intent(this, DetailArtikelActivity::class.java)
                intent.putExtra("EXTRA_TITLE", article.title)
                intent.putExtra("EXTRA_CATEGORY", article.category)
                intent.putExtra("EXTRA_DATE", article.date)
                intent.putExtra("EXTRA_CONTENT", article.content)
                intent.putExtra("EXTRA_IMAGE_URL", article.imageUrl)
                intent.putExtra("EXTRA_TYPE", article.bagian) // <<< SINKRONISASI: pakai bagian
                intent.putExtra("EXTRA_AUTHOR", article.author)
                startActivity(intent)
            }
        }
        rvRelatedContent.adapter = articleAdapter

        // 6. LOGIKA TAB
        tabResep.setOnClickListener {
            updateTabUI(isResep = true)
            loadResepData()
        }

        tabTanaman.setOnClickListener {
            updateTabUI(isResep = false)
            loadTanamanData()
        }

        // 7. NAVIGASI
        btnBack.setOnClickListener { finish() }
        navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
        navChatbot.setOnClickListener { startActivity(Intent(this, ChatbotActivity::class.java)) }
        btnScan.setOnClickListener { startActivity(Intent(this, ScanActivity::class.java)); finish() }

        // 8. LOAD AWAL
        updateTabUI(isResep = false)
    }

    // --- LOGIKA FILTER PINTAR (Per Kata) ---
    private fun loadResepData() {
        val searchKeywords = detectedName.split(" ").filter { it.length > 3 }

        val resepList = allArticles.filter { article ->
            val isResep = article.category.equals("Resep", ignoreCase = true)
            var isMatch = false

            // <<< SINKRONISASI: Cek tags dan bahan >>>
            if (article.title.contains(detectedName, ignoreCase = true) ||
                article.tags.contains(detectedName, ignoreCase = true) ||
                article.bahan.contains(detectedName, ignoreCase = true)) {
                isMatch = true
            } else if (searchKeywords.isNotEmpty()) {
                val wordMatch = searchKeywords.any { word ->
                    article.title.contains(word, ignoreCase = true) ||
                            article.tags.contains(word, ignoreCase = true) ||
                            article.bahan.contains(word, ignoreCase = true)
                }
                if (wordMatch) isMatch = true
            }
            isResep && isMatch
        }
        articleAdapter.updateData(resepList)
    }

    private fun loadTanamanData() {
        val searchKeywords = detectedName.split(" ").filter { it.length > 3 }

        val tanamanList = allArticles.filter { article ->
            val isTanaman = article.category.equals("Tanaman", ignoreCase = true)
            var isMatch = false

            // <<< SINKRONISASI: Cek tags dan bagian >>>
            if (article.title.contains(detectedName, ignoreCase = true) ||
                article.tags.contains(detectedName, ignoreCase = true) ||
                article.bagian.contains(detectedName, ignoreCase = true)) {
                isMatch = true
            } else if (searchKeywords.isNotEmpty()) {
                val wordMatch = searchKeywords.any { word ->
                    article.title.contains(word, ignoreCase = true) ||
                            article.tags.contains(word, ignoreCase = true) ||
                            article.bagian.contains(word, ignoreCase = true)
                }
                if (wordMatch) isMatch = true
            }
            isTanaman && isMatch
        }
        articleAdapter.updateData(tanamanList)
    }

    private fun updateTabUI(isResep: Boolean) {
        val activeColor = Color.parseColor("#4A6F28")
        val inactiveColor = Color.parseColor("#888888")

        if (isResep) {
            tvResep.setTextColor(activeColor)
            tvResep.typeface = android.graphics.Typeface.DEFAULT_BOLD
            lineResep.visibility = View.VISIBLE
            lineResep.setBackgroundColor(activeColor)
            tvTanaman.setTextColor(inactiveColor)
            tvTanaman.typeface = android.graphics.Typeface.DEFAULT
            lineTanaman.visibility = View.INVISIBLE
        } else {
            tvTanaman.setTextColor(activeColor)
            tvTanaman.typeface = android.graphics.Typeface.DEFAULT_BOLD
            lineTanaman.visibility = View.VISIBLE
            lineTanaman.setBackgroundColor(activeColor)
            tvResep.setTextColor(inactiveColor)
            tvResep.typeface = android.graphics.Typeface.DEFAULT
            lineResep.visibility = View.INVISIBLE
        }
    }

    private fun loadDataFromSupabase() {
        lifecycleScope.launch {
            try {
                val tanamanResult = repository.getDaftarTanamanDetail()
                val resepResult = repository.getDaftarResepDetail()

                val listTanaman = tanamanResult.getOrNull() ?: emptyList()
                val listResep = resepResult.getOrNull() ?: emptyList()

                val mappedTanaman = listTanaman.map { t ->
                    val bagianStr = t.bagianList.joinToString(", ") { it.namaBagian }
                    val tagsStr = t.penyakitList.joinToString(", ") { it.namaPenyakit }
                    val latinSuffix = if (!t.namaLatin.isNullOrEmpty()) " (${t.namaLatin})" else ""

                    Article(
                        title = "${t.namaTanaman}$latinSuffix",
                        category = "Tanaman",
                        snippet = t.deskripsiTanaman ?: "",
                        content = t.deskripsiLengkap ?: t.deskripsiTanaman ?: "",
                        date = formatSupabaseDate(t.createdAt),
                        isTrending = t.isTrending ?: false,
                        bagian = bagianStr,
                        bahan = "",
                        tags = tagsStr,
                        caraPengolahan = "",
                        caraPenggunaan = "",
                        author = t.createdBy ?: "Admin",
                        imageUrl = getSupabaseImageUrl("tanaman", t.gambarTanaman)
                    )
                }

                val mappedResep = listResep.map { r ->
                    val bahanStr = r.bahanList?.joinToString(", ") { "${it.namaBahan}:${it.takaran}" } ?: ""
                    val tagsStr = r.penyakitList.joinToString(", ") { it.namaPenyakit }
                    val steps = r.tutorialList?.sortedBy { it.urutanLangkah }?.map { tStep ->
                        TutorialStepModel(
                            desc = tStep.deskripsiLangkah,
                            img = tStep.gambarLangkah ?: ""
                        )
                    } ?: emptyList()

                    Article(
                        title = r.namaResep,
                        category = "Resep",
                        snippet = r.deskripsiResep?.take(50) + "..." ?: "",
                        content = r.deskripsiResep ?: "",
                        date = formatSupabaseDate(r.createdAt),
                        isTrending = r.isTrending ?: false,
                        bagian = "",
                        bahan = bahanStr,
                        tags = tagsStr,
                        caraPengolahan = r.caraPengolahan ?: "",
                        caraPenggunaan = r.caraPemakaian?.namaCaraPemakaian ?: "",
                        videoUrl = r.videoUrl ?: "",
                        tutorialSteps = steps,
                        author = r.createdBy ?: "Admin",
                        imageUrl = getSupabaseImageUrl("resep", r.gambarResep)
                    )
                }

                allArticles = mappedTanaman + mappedResep

                // Refresh data tab yang saat ini aktif
                if (tvResep.currentTextColor == Color.parseColor("#4A6F28")) {
                    loadResepData()
                } else {
                    loadTanamanData()
                }

            } catch (e: Exception) {
                Log.e("SupabaseError", "Gagal memuat rekomendasi dari database", e)
                Toast.makeText(this@DetailScanActivity, "Gagal memuat rekomendasi dari database", Toast.LENGTH_SHORT).show()
            }
        }
    }
}