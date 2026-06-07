package com.example.tarupramanata

// Model untuk langkah tutorial (TIDAK ADA LAGI 'method' DI SINI)
data class TutorialStepModel(
    val desc: String,
    val img: String
)

data class Article(
    val title: String,
    val category: String,
    val snippet: String,
    val content: String,
    val date: String,
    val isTrending: Boolean,

    // >>> PENGGANTI 'type' <<<
    val bagian: String = "",
    val bahan: String = "",

    val tags: String = "",

    // >>> FILTER BARU <<<
    val caraPengolahan: String = "",
    val caraPenggunaan: String = "",

    // >>> PENYIMPAN LINK VIDEO <<<
    val videoUrl: String = "",

    // List untuk menyimpan langkah-langkah tutorial
    val tutorialSteps: List<TutorialStepModel> = emptyList(),

    // >>> AUTHOR DARI DATABASE <<<
    val author: String = "Admin",

    // >>> URL GAMBAR REMOTE DARI SUPABASE STORAGE <<<
    val imageUrl: String = ""
)

fun getSupabaseImageUrl(bucket: String, fileName: String?): String {
    if (fileName.isNullOrEmpty()) return ""
    
    // Perbaiki data jika ada typo di DB (misal "lidahbuayahttps://...")
    val httpIndex = fileName.indexOf("http")
    if (httpIndex != -1) {
        var url = fileName.substring(httpIndex)
        // Hapus karakter aneh di ujung (seperti '1' pada .jpeg1) jika itu typo, 
        // tapi aman jika kita biarkan apa adanya dan serahkan ke Supabase
        return url
    }
    val encodedName = try {
        java.net.URLEncoder.encode(fileName, "UTF-8")
            .replace("+", "%20")
            .replace("%2F", "/")
    } catch (e: Exception) {
        fileName.replace(" ", "%20")
    }
    return "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$bucket/$encodedName"
}

fun formatSupabaseDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "Data Terbaru"
    return try {
        // format: 2026-05-26T05:24:56.646369+00:00
        val parts = dateStr.split("T")
        if (parts.isNotEmpty()) {
            val datePart = parts[0] // "2026-05-26"
            val dateSplit = datePart.split("-")
            if (dateSplit.size == 3) {
                val year = dateSplit[0]
                val monthNum = dateSplit[1]
                val day = dateSplit[2].toInt().toString() // remove leading zero
                val monthName = when (monthNum) {
                    "01" -> "Januari"
                    "02" -> "Februari"
                    "03" -> "Maret"
                    "04" -> "April"
                    "05" -> "Mei"
                    "06" -> "Juni"
                    "07" -> "Juli"
                    "08" -> "Agustus"
                    "09" -> "September"
                    "10" -> "Oktober"
                    "11" -> "November"
                    "12" -> "Desember"
                    else -> monthNum
                }
                "$day $monthName $year"
            } else {
                datePart
            }
        } else {
            "Data Terbaru"
        }
    } catch (e: Exception) {
        "Data Terbaru"
    }
}