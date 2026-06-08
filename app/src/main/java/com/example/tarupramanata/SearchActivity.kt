package com.example.tarupramanata

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tarupramanata.repository.TaruPramanaRepository
import kotlinx.coroutines.launch
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SearchActivity : AppCompatActivity() {

    private lateinit var rvSearchResults: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView
    private lateinit var articleAdapter: ArticleAdapter

    private lateinit var layoutActiveFilters: LinearLayout
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var tvClearAll: TextView

    private var activeFilters: List<String> = emptyList()
    private var currentKeyword: String = ""

    private var allArticles: List<Article> = emptyList()
    private val repository = TaruPramanaRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Init View
        rvSearchResults = findViewById(R.id.rvSearchResults)
        etSearch = findViewById(R.id.etSearch)
        btnBack = findViewById(R.id.btnBack)
        btnFilter = findViewById(R.id.btnFilter)
        layoutActiveFilters = findViewById(R.id.layoutActiveFilters)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        tvClearAll = findViewById(R.id.tvClearAll)

        // Setup Recycler
        rvSearchResults.layoutManager = LinearLayoutManager(this)

        articleAdapter = ArticleAdapter(emptyList()) { article ->
            if (article.category.equals("Resep", ignoreCase = true) || article.category.equals("Kuliner", ignoreCase = true)) {
                val intent = Intent(this, DetailResepActivity::class.java)
                intent.putExtra("EXTRA_TITLE", article.title)
                intent.putExtra("EXTRA_CATEGORY", article.category)
                intent.putExtra("EXTRA_DATE", article.date)
                intent.putExtra("EXTRA_CONTENT", article.content)
                intent.putExtra("EXTRA_IMAGE_URL", article.imageUrl)
                intent.putExtra("EXTRA_INGREDIENTS", article.bahan) // <<< SINKRONISASI
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
                intent.putExtra("EXTRA_TYPE", article.bagian) // <<< SINKRONISASI
                intent.putExtra("EXTRA_AUTHOR", article.author)
                intent.putExtra("EXTRA_VIDEO_URL", article.videoUrl)
                startActivity(intent)
            }
        }
        rvSearchResults.adapter = articleAdapter

        // LOAD DATA SUPABASE
        loadDataFromSupabase()

        // Setup Tombol Filter BottomSheet
        btnFilter.setOnClickListener {
            val bottomSheet = FilterBottomSheet { selectedFilters ->
                val combinedFilters = (activeFilters + selectedFilters).distinct()
                activeFilters = combinedFilters
                updateActiveFiltersUI()
                performSearch()
            }
            bottomSheet.show(supportFragmentManager, "FilterBottomSheet")
        }

        tvClearAll.setOnClickListener {
            activeFilters = emptyList()
            updateActiveFiltersUI()
            performSearch()
        }

        // Search Listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentKeyword = s.toString().lowercase().trim()
                performSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun updateActiveFiltersUI() {
        chipGroupFilters.removeAllViews()

        if (activeFilters.isEmpty()) {
            layoutActiveFilters.visibility = View.GONE
        } else {
            layoutActiveFilters.visibility = View.VISIBLE
            for (filterName in activeFilters) {
                val chip = Chip(this)
                chip.text = filterName
                chip.isCloseIconVisible = true
                chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#B9FBC0"))
                chip.setTextColor(Color.BLACK)
                chip.closeIconTint = ColorStateList.valueOf(Color.BLACK)

                chip.setOnCloseIconClickListener {
                    val newList = activeFilters.toMutableList()
                    newList.remove(filterName)
                    activeFilters = newList
                    updateActiveFiltersUI()
                    performSearch()
                }
                chipGroupFilters.addView(chip)
            }
        }
    }

    private fun loadDataFromSupabase() {
        lifecycleScope.launch {
            try {
                val tanamanResult = repository.getDaftarTanamanDetail()
                val resepResult = repository.getDaftarResepDetail()

                val penyakitResult = repository.getDaftarPenyakitDetail()

                val listTanaman = tanamanResult.getOrNull() ?: emptyList()
                val listResep = resepResult.getOrNull() ?: emptyList()
                val listPenyakit = penyakitResult.getOrNull() ?: emptyList()

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
                        videoUrl = t.videoUrl ?: "",
                        author = t.createdBy ?: "Admin",
                        imageUrl = getSupabaseImageUrl("tanaman", t.gambarTanaman)
                    )
                }

                val mappedPenyakit = listPenyakit.map { p ->
                    Article(
                        title = p.namaPenyakit,
                        category = "Penyakit",
                        snippet = p.deskripsiPenyakit?.take(50) + "..." ?: "",
                        content = p.deskripsiPenyakit ?: "",
                        date = formatSupabaseDate(p.createdAt),
                        isTrending = p.isTrending ?: false,
                        bagian = "",
                        bahan = "",
                        tags = p.namaPenyakit,
                        caraPengolahan = "",
                        caraPenggunaan = "",
                        author = p.createdBy ?: "Admin",
                        imageUrl = getSupabaseImageUrl("penyakit", p.gambarPenyakit)
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

                allArticles = mappedTanaman + mappedResep + mappedPenyakit

                // Cek Intent Filter setelah data dimuat
                val targetFilterRaw = intent.getStringExtra("TARGET_FILTER")
                if (targetFilterRaw != null) {
                    val cleanFilter = targetFilterRaw.split(":").first().trim()
                    activeFilters = listOf(cleanFilter)
                    updateActiveFiltersUI()
                }
                
                performSearch()

            } catch (e: Exception) {
                Log.e("SupabaseError", "Gagal memuat data pencarian", e)
                Toast.makeText(this@SearchActivity, "Gagal memuat data pencarian", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- BAGIAN YANG DISINKRONISASI ---
    private fun performSearch() {
        val filteredList = allArticles.filter { article ->

            // 1. Filter Keyword (Search Bar) - Cek di Judul, Kategori, Tags, Penggunaan, Pengolahan, Bagian, Bahan
            val matchKeyword = if (currentKeyword.isEmpty()) true else {
                val keywordMatch = article.title.lowercase().contains(currentKeyword) ||
                        article.category.lowercase().contains(currentKeyword) ||
                        article.tags.lowercase().contains(currentKeyword) ||
                        article.caraPenggunaan.lowercase().contains(currentKeyword) ||
                        article.caraPengolahan.lowercase().contains(currentKeyword) || // Baca langsung dari model
                        article.bagian.lowercase().contains(currentKeyword) ||
                        article.bahan.lowercase().contains(currentKeyword)

                keywordMatch
            }

            // 2. Filter dari BottomSheet / Chip
            val matchFilter = if (activeFilters.isEmpty()) true else {
                var isMatch = false
                for (filter in activeFilters) {
                    val cleanFilter = filter.split(":").first().trim()

                    // Cek Kategori
                    val isCategoryMatch = article.category.equals(cleanFilter, ignoreCase = true)

                    // Cek Bahan (Resep) & Bagian (Tanaman)
                    val bahanList = article.bahan.split(",").map { it.split(":").first().trim() }
                    val isBahanMatch = bahanList.any { it.equals(cleanFilter, ignoreCase = true) }

                    val bagianList = article.bagian.split(",").map { it.trim() }
                    val isBagianMatch = bagianList.any { it.equals(cleanFilter, ignoreCase = true) }

                    // Cek Tags (Penyakit)
                    val tagsList = article.tags.split(",").map { it.trim() }
                    val isTagMatch = tagsList.any { it.equals(cleanFilter, ignoreCase = true) }

                    // Cek Cara Penggunaan
                    val caraPakaiList = article.caraPenggunaan.split(",").map { it.trim() }
                    val isCaraPakaiMatch = caraPakaiList.any { it.equals(cleanFilter, ignoreCase = true) }

                    // Cek Cara Pengolahan (Langsung dari variabel baru)
                    val caraOlahList = article.caraPengolahan.split(",").map { it.trim() }
                    val isCaraOlahMatch = caraOlahList.any { it.equals(cleanFilter, ignoreCase = true) }

                    // Cek Judul (Penting agar tanaman aslinya ikut muncul, misal: filter "daun jambu biji" cocok dengan artikel "Jambu Biji")
                    val namaTanamanSaja = article.title.substringBefore("(").trim()
                    val isTitleMatch = article.title.contains(cleanFilter, ignoreCase = true) ||
                            cleanFilter.contains(namaTanamanSaja, ignoreCase = true) ||
                            namaTanamanSaja.contains(cleanFilter, ignoreCase = true)

                    // Jika cocok dengan salah satu filter -> Tampilkan
                    if (isTitleMatch || isCategoryMatch || isBahanMatch || isBagianMatch || isTagMatch || isCaraPakaiMatch || isCaraOlahMatch) {
                        isMatch = true
                        break
                    }
                }
                isMatch
            }

            matchKeyword && matchFilter
        }
        articleAdapter.updateData(filteredList)
    }
}