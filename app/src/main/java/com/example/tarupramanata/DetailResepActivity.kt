package com.example.tarupramanata

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class DetailResepActivity : AppCompatActivity() {

    private val repository = com.example.tarupramanata.repository.TaruPramanaRepository()
    private var allArticles: List<Article> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_resep)

        // --- 1. INIT UI COMPONENTS ---
        val imgDetail = findViewById<ImageView>(R.id.imgDetail)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)

        val containerIngredients = findViewById<LinearLayout>(R.id.containerIngredients)

        // Init UI Cara Penggunaan
        val tvCaraPenggunaan = findViewById<TextView>(R.id.tvCaraPenggunaan)
        val containerCaraPenggunaan = findViewById<LinearLayout>(R.id.containerCaraPenggunaan)

        // Init UI Cara Pengolahan
        val tvCaraPengolahan = findViewById<TextView>(R.id.tvCaraPengolahan)
        val containerCaraPengolahan = findViewById<LinearLayout>(R.id.containerCaraPengolahan)

        // Init UI Video Referensi (Tambahan)
        val tvVideoTitle = findViewById<TextView>(R.id.tvVideoTitle)
        val containerVideo = findViewById<LinearLayout>(R.id.containerVideo)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnTutorial = findViewById<View>(R.id.btnTutorial)

        val btnDropdownPenyakit = findViewById<LinearLayout>(R.id.btnDropdownPenyakit)
        val layoutKontenPenyakit = findViewById<LinearLayout>(R.id.layoutKontenPenyakit)
        val iconArrowPenyakit = findViewById<ImageView>(R.id.iconArrowPenyakit)

        // --- 2. AMBIL JUDUL DARI INTENT ---
        val title = intent.getStringExtra("EXTRA_TITLE") ?: ""

        // --- 3. AMBIL DATA LENGKAP DARI SUPABASE ---
        loadDataFromSupabase(
            title = title,
            tvTitle = tvTitle,
            tvCategory = tvCategory,
            tvDate = tvDate,
            tvDescription = tvDescription,
            imgDetail = imgDetail,
            containerIngredients = containerIngredients,
            containerCaraPenggunaan = containerCaraPenggunaan,
            tvCaraPenggunaan = tvCaraPenggunaan,
            containerCaraPengolahan = containerCaraPengolahan,
            tvCaraPengolahan = tvCaraPengolahan,
            containerVideo = containerVideo,
            tvVideoTitle = tvVideoTitle,
            btnTutorial = btnTutorial,
            layoutKontenPenyakit = layoutKontenPenyakit
        )

        // --- 5. LOGIKA KLIK DROPDOWN PENYAKIT (DENGAN ANIMASI) ---
        var isDropdownOpen = false
        btnDropdownPenyakit.setOnClickListener {
            if (isDropdownOpen) {
                layoutKontenPenyakit.visibility = View.GONE
                iconArrowPenyakit.animate().rotation(0f).setDuration(200).start()
            } else {
                layoutKontenPenyakit.visibility = View.VISIBLE
                iconArrowPenyakit.animate().rotation(180f).setDuration(200).start()
            }
            isDropdownOpen = !isDropdownOpen
        }

        // --- 6. TOMBOL KEMBALI ---
        btnBack.setOnClickListener { finish() }
    }

    private fun renderPenyakitDropdown(tagsString: String, layoutKontenPenyakit: LinearLayout, allArticles: List<Article>) {
        layoutKontenPenyakit.removeAllViews()
        if (tagsString.isNotEmpty()) {
            val tags = tagsString.split(",").map { it.trim() }
            for (tag in tags) {
                if (tag.isEmpty() || tag.equals("Umum", ignoreCase = true)) continue

                val penyakitArticle = allArticles.find { it.category.equals("Penyakit", ignoreCase = true) && it.title.equals(tag, ignoreCase = true) }

                val itemBox = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(R.drawable.bg_outline_rounded)
                    isClickable = true
                    isFocusable = true
                    setPadding(40, 32, 40, 32)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 16)
                    layoutParams = params

                    setOnClickListener {
                        if (penyakitArticle != null) {
                            val intent = Intent(this@DetailResepActivity, DetailArtikelActivity::class.java).apply {
                                putExtra("EXTRA_TITLE", penyakitArticle.title)
                                putExtra("EXTRA_CATEGORY", penyakitArticle.category)
                                putExtra("EXTRA_DATE", penyakitArticle.date)
                                putExtra("EXTRA_CONTENT", penyakitArticle.content)
                                putExtra("EXTRA_IMAGE_URL", penyakitArticle.imageUrl)
                                putExtra("EXTRA_AUTHOR", penyakitArticle.author)
                            }
                            startActivity(intent)
                        } else {
                            val searchIntent = Intent(this@DetailResepActivity, SearchActivity::class.java)
                            searchIntent.putExtra("TARGET_FILTER", tag)
                            startActivity(searchIntent)
                        }
                    }
                }

                val tvPenyakitName = TextView(this).apply {
                    text = tag
                    setTextColor(Color.parseColor("#333333"))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvActionCari = TextView(this).apply {
                    text = if (penyakitArticle != null) "Lihat \u276F" else "Cari \u276F"
                    setTextColor(Color.parseColor("#F57C00"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                itemBox.addView(tvPenyakitName)
                itemBox.addView(tvActionCari)
                layoutKontenPenyakit.addView(itemBox)
            }
        }
    }

    private fun renderIngredients(ingredientsString: String, containerIngredients: LinearLayout) {
        containerIngredients.removeAllViews()
        val ingredients = ingredientsString.split(",").map { it.trim() }

        for (item in ingredients) {
            if (item.isEmpty()) continue

            val splitItem = item.split(":")
            val name = splitItem.getOrNull(0) ?: item
            val amount = splitItem.getOrNull(1) ?: ""

            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_outline_rounded)
                isClickable = true
                isFocusable = true

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 24)
                layoutParams = params
                setPadding(32, 32, 32, 32)

                setOnClickListener {
                    val searchIntent = Intent(this@DetailResepActivity, SearchActivity::class.java)
                    searchIntent.putExtra("TARGET_FILTER", name)
                    startActivity(searchIntent)
                }
            }

            val tvName = TextView(this).apply {
                text = name
                setTextColor(Color.BLACK)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvAmount = TextView(this).apply {
                text = amount
                setTextColor(Color.parseColor("#888888"))
                textSize = 12f
            }

            rowLayout.addView(tvName)
            rowLayout.addView(tvAmount)
            containerIngredients.addView(rowLayout)
        }
    }

    private fun renderCaraPenggunaan(caraPenggunaanString: String, containerCaraPenggunaan: LinearLayout, tvCaraPenggunaan: TextView) {
        containerCaraPenggunaan.removeAllViews()

        if (caraPenggunaanString.isNotEmpty()) {
            containerCaraPenggunaan.visibility = View.VISIBLE
            tvCaraPenggunaan.visibility = View.VISIBLE

            val caraList = caraPenggunaanString.split(",").map { it.trim() }

            for (cara in caraList) {
                if (cara.isEmpty()) continue

                val itemBox = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(R.drawable.bg_outline_rounded)
                    isClickable = true
                    isFocusable = true
                    setPadding(40, 32, 40, 32)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 16)
                    layoutParams = params

                    setOnClickListener {
                        val searchIntent = Intent(this@DetailResepActivity, SearchActivity::class.java)
                        searchIntent.putExtra("TARGET_FILTER", cara)
                        startActivity(searchIntent)
                    }
                }

                val tvCaraName = TextView(this).apply {
                    text = cara
                    setTextColor(Color.parseColor("#333333"))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvActionCari = TextView(this).apply {
                    text = "Cari \u276F"
                    setTextColor(Color.parseColor("#F57C00"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                itemBox.addView(tvCaraName)
                itemBox.addView(tvActionCari)
                containerCaraPenggunaan.addView(itemBox)
            }
        } else {
            containerCaraPenggunaan.visibility = View.GONE
            tvCaraPenggunaan.visibility = View.GONE
        }
    }

    private fun renderCaraPengolahan(caraPengolahanString: String, containerCaraPengolahan: LinearLayout, tvCaraPengolahan: TextView) {
        containerCaraPengolahan.removeAllViews()

        if (caraPengolahanString.isNotEmpty()) {
            containerCaraPengolahan.visibility = View.VISIBLE
            tvCaraPengolahan.visibility = View.VISIBLE

            val pengolahanList = caraPengolahanString.split(",").map { it.trim() }

            for (olah in pengolahanList) {
                if (olah.isEmpty()) continue

                val itemBox = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(R.drawable.bg_outline_rounded)
                    isClickable = true
                    isFocusable = true
                    setPadding(40, 32, 40, 32)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 16)
                    layoutParams = params

                    setOnClickListener {
                        val searchIntent = Intent(this@DetailResepActivity, SearchActivity::class.java)
                        searchIntent.putExtra("TARGET_FILTER", olah)
                        startActivity(searchIntent)
                    }
                }

                val tvOlahName = TextView(this).apply {
                    text = olah
                    setTextColor(Color.parseColor("#333333"))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvActionCari = TextView(this).apply {
                    text = "Cari \u276F"
                    setTextColor(Color.parseColor("#F57C00"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                itemBox.addView(tvOlahName)
                itemBox.addView(tvActionCari)
                containerCaraPengolahan.addView(itemBox)
            }
        } else {
            containerCaraPengolahan.visibility = View.GONE
            tvCaraPengolahan.visibility = View.GONE
        }
    }

    private fun loadDataFromSupabase(
        title: String,
        tvTitle: TextView,
        tvCategory: TextView,
        tvDate: TextView,
        tvDescription: TextView,
        imgDetail: ImageView,
        containerIngredients: LinearLayout,
        containerCaraPenggunaan: LinearLayout,
        tvCaraPenggunaan: TextView,
        containerCaraPengolahan: LinearLayout,
        tvCaraPengolahan: TextView,
        containerVideo: LinearLayout,
        tvVideoTitle: TextView,
        btnTutorial: View,
        layoutKontenPenyakit: LinearLayout
    ) {
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val nestedScrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollView)

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

                allArticles = mappedTanaman + mappedResep + mappedPenyakit
                val currentArticle = allArticles.find { it.title.equals(title, ignoreCase = true) }

                val category = currentArticle?.category ?: intent.getStringExtra("EXTRA_CATEGORY")
                val date = currentArticle?.date ?: intent.getStringExtra("EXTRA_DATE")
                val content = currentArticle?.content ?: intent.getStringExtra("EXTRA_CONTENT")
                val imageUrl = currentArticle?.imageUrl ?: intent.getStringExtra("EXTRA_IMAGE_URL") ?: ""
                val author = currentArticle?.author ?: intent.getStringExtra("EXTRA_AUTHOR") ?: "Admin"

                val ingredientsString = currentArticle?.bahan ?: intent.getStringExtra("EXTRA_INGREDIENTS") ?: "Bahan tidak tersedia: -"
                val tagsString = currentArticle?.tags ?: intent.getStringExtra("EXTRA_TAGS") ?: ""

                val caraPenggunaanString = currentArticle?.caraPenggunaan ?: ""
                val caraPengolahanString = currentArticle?.caraPengolahan ?: intent.getStringExtra("EXTRA_CARA_PENGOLAHAN") ?: ""

                val videoUrlString = currentArticle?.videoUrl ?: intent.getStringExtra("EXTRA_VIDEO_URL") ?: ""

                // Set Text dan UI setelah data tersedia
                tvTitle.text = title
                tvCategory.text = category
                tvDate.text = "• $date"
                findViewById<TextView>(R.id.tvAuthor).text = author
                tvDescription.text = content

                imgDetail.setImageDrawable(null)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@DetailResepActivity).load(imageUrl).into(imgDetail)
                }

                renderPenyakitDropdown(tagsString, layoutKontenPenyakit, allArticles)
                renderIngredients(ingredientsString, containerIngredients)
                renderCaraPenggunaan(caraPenggunaanString, containerCaraPenggunaan, tvCaraPenggunaan)
                renderCaraPengolahan(caraPengolahanString, containerCaraPengolahan, tvCaraPengolahan)

                // --- 9.5. LOGIKA TAMPILKAN LINK VIDEO ---
                if (videoUrlString.isNotEmpty()) {
                    tvVideoTitle.visibility = View.VISIBLE
                    containerVideo.visibility = View.VISIBLE

                    containerVideo.setOnClickListener {
                        try {
                            val intentVideo = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrlString))
                            startActivity(intentVideo)
                        } catch (e: Exception) {
                            Toast.makeText(this@DetailResepActivity, "Tidak ada aplikasi untuk membuka link ini.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    tvVideoTitle.visibility = View.GONE
                    containerVideo.visibility = View.GONE
                }

                // --- 10. LOGIKA TOMBOL TUTORIAL ---
                btnTutorial.setOnClickListener {
                    if (currentArticle != null && currentArticle.tutorialSteps.isNotEmpty()) {
                        val stepsDesc = ArrayList<String>()
                        val stepsImg = ArrayList<String>()

                        for (step in currentArticle.tutorialSteps) {
                            stepsDesc.add(step.desc)
                            stepsImg.add(step.img)
                        }

                        val tutorialIntent = Intent(this@DetailResepActivity, TutorialActivity::class.java)
                        tutorialIntent.putStringArrayListExtra("STEPS_DESC", stepsDesc)
                        tutorialIntent.putStringArrayListExtra("STEPS_IMG", stepsImg)
                        tutorialIntent.putExtra("EXTRA_TUTORIAL_TITLE", title)
                        tutorialIntent.putExtra("EXTRA_CARA_PENGOLAHAN", currentArticle.caraPengolahan)

                        startActivity(tutorialIntent)
                    } else {
                        Toast.makeText(this@DetailResepActivity, "Tutorial belum tersedia untuk resep ini.", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("DetailResep", "Error memuat data dari Supabase", e)
            } finally {
                progressBar.visibility = View.GONE
                nestedScrollView.visibility = View.VISIBLE
            }
        }
    }
}