package com.example.tarupramanata

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class DetailArtikelActivity : AppCompatActivity() {

    // --- VARIABEL UNTUK MENYIMPAN FILTER AKTIF ---
    private var filterBagian: String? = null
    private var filterPenyakit: String? = null
    private var filterPengolahan: String? = null
    private var filterPenggunaan: String? = null

    // --- VARIABEL DATA ---
    private lateinit var relatedAdapter: ArticleAdapter
    private var allArticles: List<Article> = emptyList() // Data dari Supabase
    private var allRelatedResep: List<Article> = emptyList() // Data asli
    private var filteredResep: List<Article> = emptyList()   // Data setelah difilter
    private val repository = com.example.tarupramanata.repository.TaruPramanaRepository()

    private lateinit var tvStatusResep: TextView

    // --- VARIABEL CONTAINER DROPDOWN ---
    private lateinit var layoutKontenBagian: LinearLayout
    private lateinit var layoutKontenPenyakit: LinearLayout
    private lateinit var layoutKontenPengolahan: LinearLayout
    private lateinit var layoutKontenPenggunaan: LinearLayout

    // --- VARIABEL JUDUL DROPDOWN (UNTUK DIUBAH TEKSNYA SAAT DIPILIH) ---
    private lateinit var tvTitleBagian: TextView
    private lateinit var tvTitlePenyakit: TextView
    private lateinit var tvTitlePengolahan: TextView
    private lateinit var tvTitlePenggunaan: TextView

    // --- VARIABEL IKON PANAH ---
    private lateinit var iconArrowBagian: ImageView
    private lateinit var iconArrowPenyakit: ImageView
    private lateinit var iconArrowPengolahan: ImageView
    private lateinit var iconArrowPenggunaan: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_artikel)

        // --- 1. INIT UI COMPONENTS ---
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val imgDetail = findViewById<ImageView>(R.id.imgDetailArticle)
        val tvContent = findViewById<TextView>(R.id.tvContent)
        val rvRelatedArticles = findViewById<RecyclerView>(R.id.rvRelatedArticles)
        tvStatusResep = findViewById(R.id.tvStatusResep)
        val tvAuthorName = findViewById<TextView>(R.id.tvAuthorName)

        // Init Header Dropdown & Judul
        val btnDropdownBagian = findViewById<LinearLayout>(R.id.btnDropdownBagian)
        val btnDropdownPenyakit = findViewById<LinearLayout>(R.id.btnDropdownPenyakit)
        val btnDropdownPengolahan = findViewById<LinearLayout>(R.id.btnDropdownPengolahan)
        val btnDropdownPenggunaan = findViewById<LinearLayout>(R.id.btnDropdownPenggunaan)

        tvTitleBagian = findViewById(R.id.tvTitleBagian)
        tvTitlePenyakit = findViewById(R.id.tvTitlePenyakit)
        tvTitlePengolahan = findViewById(R.id.tvTitlePengolahan)
        tvTitlePenggunaan = findViewById(R.id.tvTitlePenggunaan)

        // Init Ikon Arrow
        iconArrowBagian = findViewById(R.id.iconArrowBagian)
        iconArrowPenyakit = findViewById(R.id.iconArrowPenyakit)
        iconArrowPengolahan = findViewById(R.id.iconArrowPengolahan)
        iconArrowPenggunaan = findViewById(R.id.iconArrowPenggunaan)

        // Init Container Dropdown
        layoutKontenBagian = findViewById(R.id.layoutKontenBagian)
        layoutKontenPenyakit = findViewById(R.id.layoutKontenPenyakit)
        layoutKontenPengolahan = findViewById(R.id.layoutKontenPengolahan)
        layoutKontenPenggunaan = findViewById(R.id.layoutKontenPenggunaan)

        // --- 2. AMBIL DATA DARI INTENT (ARTIKEL SAAT INI) ---
        val currentTitle = intent.getStringExtra("EXTRA_TITLE") ?: ""
        val category = intent.getStringExtra("EXTRA_CATEGORY")
        val date = intent.getStringExtra("EXTRA_DATE")
        val content = intent.getStringExtra("EXTRA_CONTENT")
        val imageUrl = intent.getStringExtra("EXTRA_IMAGE_URL")
        val author = intent.getStringExtra("EXTRA_AUTHOR") ?: "Admin"

        tvTitle.text = currentTitle
        tvCategory.text = category ?: "Tips"
        tvDate.text = date ?: "-"
        tvAuthorName.text = author
        tvContent.text = content ?: "Isi artikel tidak tersedia."

        imgDetail.setImageDrawable(null)
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).into(imgDetail)
        }

        // --- 3. LOAD DATA DARI SUPABASE ---
        loadDataFromSupabase()

        // --- 7. LOGIKA KLIK BUKA/TUTUP DROPDOWN ---
        setupDropdownLogic(btnDropdownBagian, layoutKontenBagian, iconArrowBagian)
        setupDropdownLogic(btnDropdownPenyakit, layoutKontenPenyakit, iconArrowPenyakit)
        setupDropdownLogic(btnDropdownPengolahan, layoutKontenPengolahan, iconArrowPengolahan)
        setupDropdownLogic(btnDropdownPenggunaan, layoutKontenPenggunaan, iconArrowPenggunaan)

        // --- 8. TOMBOL KEMBALI ---
        btnBack.setOnClickListener { finish() }
    }

    private fun loadDataFromSupabase() {
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val nestedScrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollView)

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
                        snippet = t.deskripsiTanaman?.take(50) + "..." ?: "",
                        content = t.deskripsiTanaman ?: "",
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
                        tutorialSteps = emptyList(),
                        author = r.createdBy ?: "Admin",
                        imageUrl = getSupabaseImageUrl("resep", r.gambarResep)
                    )
                }

                allArticles = mappedTanaman + mappedResep

                // Setelah data dimuat, jalankan logika untuk menampilkan resep terkait
                val currentTitle = intent.getStringExtra("EXTRA_TITLE") ?: ""
                val keywordTanaman = currentTitle.split("(").firstOrNull()?.trim() ?: ""

                allRelatedResep = allArticles.filter { article ->
                    val isResep = article.category.equals("Resep", ignoreCase = true)
                    val isMatch = article.title.contains(keywordTanaman, ignoreCase = true) ||
                            article.bahan.contains(keywordTanaman, ignoreCase = true) ||
                            article.tags.contains(keywordTanaman, ignoreCase = true)
                    isResep && isMatch
                }
                filteredResep = allRelatedResep

                // Set adapter setelah data difilter
                val rvRelatedArticles = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvRelatedArticles)
                rvRelatedArticles.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@DetailArtikelActivity)
                relatedAdapter = ArticleAdapter(filteredResep) { article ->
                    val resepIntent = Intent(this@DetailArtikelActivity, DetailResepActivity::class.java)
                    resepIntent.putExtra("EXTRA_TITLE", article.title)
                    resepIntent.putExtra("EXTRA_CATEGORY", article.category)
                    resepIntent.putExtra("EXTRA_DATE", article.date)
                    resepIntent.putExtra("EXTRA_CONTENT", article.content)
                    resepIntent.putExtra("EXTRA_IMAGE_URL", article.imageUrl)
                    resepIntent.putExtra("EXTRA_INGREDIENTS", article.bahan)
                    resepIntent.putExtra("EXTRA_TAGS", article.tags)
                    resepIntent.putExtra("EXTRA_AUTHOR", article.author)
                    startActivity(resepIntent)
                }
                rvRelatedArticles.adapter = relatedAdapter
                rvRelatedArticles.isNestedScrollingEnabled = false
                updateStatusResepText()

                // Ekstrak dropdown filter
                refreshAllDropdowns()

            } catch (e: Exception) {
                android.util.Log.e("DetailArtikel", "Error memuat data dari Supabase", e)
            } finally {
                progressBar.visibility = View.GONE
                nestedScrollView.visibility = View.VISIBLE
            }
        }
    }

    // ==============================================================
    // FUNGSI ANIMASI DROPDOWN
    // ==============================================================
    private fun setupDropdownLogic(header: View, content: View, arrow: ImageView) {
        header.setOnClickListener {
            // Jika konten sedang sembunyi, tampilkan
            if (content.visibility == View.GONE || content.visibility == View.INVISIBLE) {
                content.visibility = View.VISIBLE
                arrow.animate().rotation(180f).setDuration(200).start()
            } else {
                // Jika konten sedang tampil, sembunyikan
                content.visibility = View.GONE
                arrow.animate().rotation(0f).setDuration(200).start()
            }
        }
    }

    // ==============================================================
    // FUNGSI MEMBUAT KOTAK FILTER (CHIPS) SECARA DINAMIS
    // ==============================================================
    private fun renderFilterBoxes(container: LinearLayout, dataList: List<String>, filterType: String) {
        container.removeAllViews()

        if (dataList.isEmpty()) {
            val tvKosong = TextView(this).apply {
                text = "Tidak ada data spesifik."
                setPadding(0, 0, 0, 16)
                setTextColor(Color.GRAY)
            }
            container.addView(tvKosong)
            return
        }

        for (item in dataList) {
            // Cek apakah item ini sedang aktif dipilih
            val isActive = when (filterType) {
                "BAGIAN" -> filterBagian == item
                "PENYAKIT" -> filterPenyakit == item
                "PENGOLAHAN" -> filterPengolahan == item
                "PENGGUNAAN" -> filterPenggunaan == item
                else -> false
            }

            val itemBox = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                isClickable = true
                isFocusable = true
                setPadding(40, 32, 40, 32)

                // Ubah warna kotak di dalam list jika terpilih
                if (isActive) setBackgroundColor(Color.parseColor("#DCEDC8"))
                else setBackgroundResource(R.drawable.bg_outline_rounded)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                layoutParams = params

                setOnClickListener {
                    // 1. Tentukan apakah klik ini untuk 'Memilih' atau 'Membatalkan'
                    when (filterType) {
                        "BAGIAN" -> filterBagian = if (isActive) null else item
                        "PENYAKIT" -> filterPenyakit = if (isActive) null else item
                        "PENGOLAHAN" -> filterPengolahan = if (isActive) null else item
                        "PENGGUNAAN" -> filterPenggunaan = if (isActive) null else item
                    }

                    // 2. Jika baru saja memilih (bukan membatalkan), tutup otomatis dropdown-nya
                    if (!isActive) {
                        container.visibility = View.GONE
                        // Tutup panahnya
                        when(filterType){
                            "BAGIAN" -> iconArrowBagian.animate().rotation(0f).setDuration(200).start()
                            "PENYAKIT" -> iconArrowPenyakit.animate().rotation(0f).setDuration(200).start()
                            "PENGOLAHAN" -> iconArrowPengolahan.animate().rotation(0f).setDuration(200).start()
                            "PENGGUNAAN" -> iconArrowPenggunaan.animate().rotation(0f).setDuration(200).start()
                        }
                    }

                    // 3. Update Text di Header & Gambar Ulang semua Dropdown
                    refreshAllDropdowns()

                    // 4. Saring RecyclerView
                    applyFilters()
                }
            }

            val tvName = TextView(this).apply {
                text = item
                setTextColor(Color.parseColor(if (isActive) "#33691E" else "#333333"))
                if (isActive) setTypeface(null, android.graphics.Typeface.BOLD)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvAction = TextView(this).apply {
                text = if (isActive) "Batal ✖" else "Pilih"
                setTextColor(Color.parseColor(if (isActive) "#D32F2F" else "#F57C00"))
                textSize = 12f
            }

            itemBox.addView(tvName)
            itemBox.addView(tvAction)
            container.addView(itemBox)
        }
    }

    // Menggambar ulang kotak filter & mengubah teks header
    private fun refreshAllDropdowns() {
        val setBagian = mutableSetOf<String>()
        val setPenyakit = mutableSetOf<String>()
        val setPengolahan = mutableSetOf<String>()
        val setPenggunaan = mutableSetOf<String>()

        // 1. Ekstrak dari Artikel Utama (Tanaman)
        val mainArticle = allArticles.find { it.title.equals(intent.getStringExtra("EXTRA_TITLE"), ignoreCase = true) }

        // >>> PERBAIKAN: Ambil HANYA dari atribut "bagian" tanaman utama <<<
        mainArticle?.bagian?.split(",")?.forEach { if (it.isNotBlank()) setBagian.add(it.trim()) }

        mainArticle?.tags?.split(",")?.forEach { if (it.isNotBlank()) setPenyakit.add(it.trim()) }
        mainArticle?.caraPenggunaan?.split(",")?.forEach { if (it.isNotBlank()) setPenggunaan.add(it.trim()) }
        mainArticle?.caraPengolahan?.split(",")?.forEach { if (it.isNotBlank()) setPengolahan.add(it.trim()) }

        // 2. Ekstrak dari Daftar Resep Terkait
        allRelatedResep.forEach { resep ->

            resep.tags.split(",").forEach { if (it.isNotBlank() && it != "Umum") setPenyakit.add(it.trim()) }
            resep.caraPenggunaan.split(",").forEach { if (it.isNotBlank()) setPenggunaan.add(it.trim()) }
            resep.caraPengolahan.split(",").forEach { if (it.isNotBlank()) setPengolahan.add(it.trim()) }
        }

        renderFilterBoxes(layoutKontenBagian, setBagian.toList(), "BAGIAN")
        renderFilterBoxes(layoutKontenPenyakit, setPenyakit.toList(), "PENYAKIT")
        renderFilterBoxes(layoutKontenPengolahan, setPengolahan.toList(), "PENGOLAHAN")
        renderFilterBoxes(layoutKontenPenggunaan, setPenggunaan.toList(), "PENGGUNAAN")

        // --- UPDATE TEKS HEADER DROPDOWN KE DEPAN ---
        updateHeaderUI(tvTitleBagian, "Bagian Tanaman", filterBagian)
        updateHeaderUI(tvTitlePenyakit, "Penyakit / Manfaat", filterPenyakit)
        updateHeaderUI(tvTitlePengolahan, "Cara Pengolahan", filterPengolahan)
        updateHeaderUI(tvTitlePenggunaan, "Cara Penggunaan", filterPenggunaan)
    }

    // Fungsi kecil untuk mengubah teks header agar rapi
    private fun updateHeaderUI(textView: TextView, defaultText: String, activeFilter: String?) {
        if (activeFilter != null) {
            // Jika ada filter yang terpilih, ubah teks menjadi "Kategori: Pilihan" dan ubah warna
            textView.text = "$defaultText: $activeFilter"
            textView.setTextColor(Color.parseColor("#33691E")) // Warna hijau pekat
        } else {
            // Jika tidak ada (atau baru saja dibatalkan), kembalikan ke awal
            textView.text = defaultText
            textView.setTextColor(Color.BLACK)
        }
    }

    // ==============================================================
    // FUNGSI UTAMA: MENYARING RESEP BERDASARKAN FILTER AKTIF
    // ==============================================================
    private fun applyFilters() {
        filteredResep = allRelatedResep.filter { resep ->
            var isMatch = true

            // >>> PERBAIKAN: Cek apakah Bahan di resep MENGANDUNG kata dari Bagian tanaman yang dipilih <<<
            if (filterBagian != null) {
                // Contoh: filterBagian = "Daun", resep.bahan = "Daun Lidah Buaya:1 pelepah"
                val hasBahan = resep.bahan.contains(filterBagian!!, ignoreCase = true)
                if (!hasBahan) isMatch = false
            }

            // Cek Penyakit (Tags)
            if (filterPenyakit != null && isMatch) {
                val hasPenyakit = resep.tags.split(",").any { it.trim().equals(filterPenyakit, ignoreCase = true) }
                if (!hasPenyakit) isMatch = false
            }

            // Cek Cara Pengolahan
            if (filterPengolahan != null && isMatch) {
                val hasPengolahan = resep.caraPengolahan.split(",").any { it.trim().equals(filterPengolahan, ignoreCase = true) }
                if (!hasPengolahan) isMatch = false
            }

            // Cek Cara Penggunaan
            if (filterPenggunaan != null && isMatch) {
                val hasPenggunaan = resep.caraPenggunaan.split(",").any { it.trim().equals(filterPenggunaan, ignoreCase = true) }
                if (!hasPenggunaan) isMatch = false
            }

            isMatch
        }

        relatedAdapter.updateData(filteredResep)
        updateStatusResepText()
    }

    private fun updateStatusResepText() {
        if (filteredResep.isEmpty()) {
            tvStatusResep.text = "Resep tidak ditemukan untuk filter ini."
            tvStatusResep.setTextColor(Color.RED)
        } else {
            tvStatusResep.text = "Resep Herbal Terkait (${filteredResep.size})"
            tvStatusResep.setTextColor(Color.BLACK)
        }
    }
}