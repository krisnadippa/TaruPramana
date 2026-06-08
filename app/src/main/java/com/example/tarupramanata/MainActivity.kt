package com.example.tarupramanata

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import kotlin.math.abs

// TAMBAHAN IMPORT UNTUK SUPABASE
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.tarupramanata.repository.TaruPramanaRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.facebook.shimmer.ShimmerFrameLayout
import com.example.tarupramanata.data.model.Tanaman
import com.example.tarupramanata.data.model.Resep
import com.example.tarupramanata.data.model.DetailPenyakitResponse

class MainActivity : AppCompatActivity() {

    // Variabel UI
    private lateinit var viewPagerTrending: ViewPager2
    private lateinit var rvArticles: RecyclerView
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var shimmerHome: ShimmerFrameLayout

    // Variabel Tombol & Navigasi
    private lateinit var btnSearchHome: LinearLayout
    private lateinit var btnScan: CardView
    private lateinit var navChatbot: LinearLayout
    private lateinit var navHome: LinearLayout

    // Variabel Data
    private lateinit var dataArtikel: List<Article>

    // Variabel Auto Scroll Trending
    private lateinit var handler: Handler
    private lateinit var sliderRunnable: Runnable

    // Variabel Repository Supabase
    private val repository = TaruPramanaRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kita inisialisasi list kosong dulu
        dataArtikel = emptyList<Article>()

        // --- 1. INIT UI ---
        viewPagerTrending = findViewById(R.id.vpTrending)
        rvArticles = findViewById(R.id.rvArticles)
        btnSearchHome = findViewById(R.id.btnSearchHome)
        btnScan = findViewById(R.id.btnScan)
        navChatbot = findViewById(R.id.navChatbot)
        navHome = findViewById(R.id.navHome)
        shimmerHome = findViewById(R.id.shimmerHome)
        
        shimmerHome.startShimmer()

        // --- 3. PINDAH KE SEARCH ---
        btnSearchHome.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        // --- 4. CALLBACK KLIK ARTIKEL (LOGIKA NAVIGASI) ---
        val onArticleClicked = { article: Article ->

            // Cek Kategori: Apakah Resep/Kuliner?
            if (article.category.equals("Resep", ignoreCase = true) || article.category.equals("Kuliner", ignoreCase = true)) {
                // -> KE DETAIL RESEP
                val intent = Intent(this, DetailResepActivity::class.java)
                intent.putExtra("EXTRA_TITLE", article.title)
                intent.putExtra("EXTRA_CATEGORY", article.category)
                intent.putExtra("EXTRA_DATE", article.date)
                intent.putExtra("EXTRA_CONTENT", article.content)
                intent.putExtra("EXTRA_IMAGE_URL", article.imageUrl)

                // <<< SINKRONISASI: Kirim Bahan & Tags >>>
                intent.putExtra("EXTRA_INGREDIENTS", article.bahan)
                intent.putExtra("EXTRA_TAGS", article.tags)
                intent.putExtra("EXTRA_AUTHOR", article.author)

                startActivity(intent)
            } else {
                // -> KE DETAIL ARTIKEL BIASA (TANAMAN)
                val intent = Intent(this, DetailArtikelActivity::class.java)
                intent.putExtra("EXTRA_TITLE", article.title)
                intent.putExtra("EXTRA_CATEGORY", article.category)
                intent.putExtra("EXTRA_DATE", article.date)
                intent.putExtra("EXTRA_CONTENT", article.content)
                intent.putExtra("EXTRA_IMAGE_URL", article.imageUrl)

                // <<< SINKRONISASI: Kirim Bagian Tanaman >>>
                intent.putExtra("EXTRA_TYPE", article.bagian)
                intent.putExtra("EXTRA_AUTHOR", article.author)
                intent.putExtra("EXTRA_VIDEO_URL", article.videoUrl)

                startActivity(intent)
            }
        }

        // Data UI akan kita perbarui SETELAH data Supabase selesai didownload
        // melalui fungsi loadDataFromSupabase() di bawah.

        // --- 6. NAVIGASI LAIN ---
        navChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
        btnScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        // --- 7. TOUR GUIDE ---
        btnScan.post { checkAndStartTour() }

        // --- 8. LOAD DATA SUPABASE ---
        loadDataFromSupabase(onArticleClicked)
    }

    // --- FUNGSI LOAD DATA DARI SUPABASE ---
    private fun loadDataFromSupabase(onArticleClicked: (Article) -> Unit) {
        lifecycleScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                // Ambil Tanaman, Resep, dan Penyakit secara paralel (Gunakan Detail agar tag dan bagian terambil)
                val tanamanResult = repository.getDaftarTanamanDetail()
                val resepResult = repository.getDaftarResepDetail()
                val penyakitResult = repository.getDaftarPenyakitDetail()
                 val apiTime = System.currentTimeMillis() - startTime
                 val runtime = Runtime.getRuntime()
                 val ramUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                 Log.i("PerformanceTest", "Supabase API Fetch Time: $apiTime ms | RAM Used: $ramUsed MB")

                tanamanResult.onFailure {
                    Log.e("SupabaseError", "Gagal memuat Tanaman: ${it.message}", it)
                }
                resepResult.onFailure {
                    Log.e("SupabaseError", "Gagal memuat Resep: ${it.message}", it)
                }
                penyakitResult.onFailure {
                    Log.e("SupabaseError", "Gagal memuat Penyakit: ${it.message}", it)
                }

                val listTanaman = tanamanResult.getOrNull() ?: emptyList()
                val listResep = resepResult.getOrNull() ?: emptyList()
                val listPenyakit = penyakitResult.getOrNull() ?: emptyList()

                // Map Tanaman ke Article
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

                // Map Penyakit ke Article
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

                // Map Resep ke Article
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

                // Gabungkan dan acak urutannya agar tanaman, resep dan penyakit bercampur
                val allArticles = (mappedTanaman + mappedResep + mappedPenyakit).shuffled()
                dataArtikel = allArticles

                // Perbarui UI di Main Thread
                setupUIWithData(allArticles, onArticleClicked)

                // Matikan Shimmer dan tampilkan konten utama
                shimmerHome.stopShimmer()
                shimmerHome.visibility = android.view.View.GONE
                findViewById<android.view.View>(R.id.scrollViewContent).visibility = android.view.View.VISIBLE

            } catch (e: Exception) {
                Log.e("SupabaseError", "Gagal memuat data", e)
                Toast.makeText(this@MainActivity, "Gagal memuat data dari Supabase", Toast.LENGTH_SHORT).show()

                // Jika error, matikan juga shimmer agar tidak terus berkedip
                shimmerHome.stopShimmer()
                shimmerHome.visibility = android.view.View.GONE
                findViewById<android.view.View>(R.id.scrollViewContent).visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun setupUIWithData(articles: List<Article>, onArticleClicked: (Article) -> Unit) {
        val trendingArticles = articles.filter { it.isTrending }
        if (trendingArticles.isNotEmpty()) {
            val trendingAdapter = TrendingAdapter(trendingArticles, onArticleClicked)
            viewPagerTrending.adapter = trendingAdapter
            viewPagerTrending.setCurrentItem(trendingArticles.size * 1000, false)
            setupSliderEffect()
            setupAutoScroll()
        }

        val listArtikelBawah = articles.filter { !it.isTrending }
        rvArticles.layoutManager = LinearLayoutManager(this)
        rvArticles.isNestedScrollingEnabled = false
        articleAdapter = ArticleAdapter(listArtikelBawah, onArticleClicked)
        rvArticles.adapter = articleAdapter
    }


    // --- FUNGSI PENDUKUNG (TOUR & SLIDER) ---
    private fun checkAndStartTour() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_tour_finished", false)) {
            startTourSequence()
        }
    }

    private fun startTourSequence() {
        TapTargetSequence(this)
            .targets(
                TapTarget.forView(btnSearchHome, "Cari Artikel", "Cari artikel spesifik di sini.")
                    .outerCircleColor(R.color.black).targetCircleColor(R.color.white).cancelable(false).tintTarget(false),
                TapTarget.forView(navHome, "Menu Home", "Kembali ke halaman utama.")
                    .outerCircleColor(R.color.black).targetCircleColor(R.color.white).transparentTarget(true).cancelable(false),
                TapTarget.forView(navChatbot, "Chatbot", "Tanya jawab seputar tanaman.")
                    .outerCircleColor(R.color.black).targetCircleColor(R.color.white).transparentTarget(true).cancelable(false),
                TapTarget.forView(btnScan, "Scan", "Deteksi daun tanaman herbal.")
                    .outerCircleColor(android.R.color.holo_green_dark).targetCircleColor(R.color.white).cancelable(false).tintTarget(false)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit().putBoolean("is_tour_finished", true).apply()
                }
                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                override fun onSequenceCanceled(lastTarget: TapTarget?) {}
            })
            .start()
    }

    private fun setupSliderEffect() {
        viewPagerTrending.clipToPadding = false
        viewPagerTrending.clipChildren = false
        viewPagerTrending.offscreenPageLimit = 3
        viewPagerTrending.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(40))
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = 0.90f + r * 0.10f
            page.alpha = 0.5f + r * 0.5f
        }
        viewPagerTrending.setPageTransformer(compositePageTransformer)
    }

    private fun setupAutoScroll() {
        handler = Handler(Looper.getMainLooper())
        sliderRunnable = Runnable {
            if (viewPagerTrending.adapter != null) {
                viewPagerTrending.setCurrentItem(viewPagerTrending.currentItem + 1, true)
            }
        }
        handler.postDelayed(sliderRunnable, 3000)
        viewPagerTrending.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                handler.removeCallbacks(sliderRunnable)
                handler.postDelayed(sliderRunnable, 3000)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        if (::handler.isInitialized) handler.removeCallbacks(sliderRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (::handler.isInitialized) handler.postDelayed(sliderRunnable, 3000)
    }
}