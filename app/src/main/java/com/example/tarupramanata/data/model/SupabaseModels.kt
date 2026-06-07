package com.example.tarupramanata.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tanaman(
    @SerialName("id_tanaman") val idTanaman: Int,
    @SerialName("nama_tanaman") val namaTanaman: String,
    @SerialName("nama_latin") val namaLatin: String? = null,
    @SerialName("deskripsi_tanaman") val deskripsiTanaman: String? = null,
    @SerialName("habitat") val habitat: String? = null,
    @SerialName("gambar_tanaman") val gambarTanaman: String? = null,
    @SerialName("is_trending") val isTrending: Boolean? = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class Penyakit(
    @SerialName("id_penyakit") val idPenyakit: Int,
    @SerialName("nama_penyakit") val namaPenyakit: String,
    @SerialName("deskripsi_penyakit") val deskripsiPenyakit: String? = null
)

@Serializable
data class CaraPemakaian(
    @SerialName("id_cara_pemakaian") val idCaraPemakaian: Int,
    @SerialName("nama_cara_pemakaian") val namaCaraPemakaian: String,
    @SerialName("deskripsi_pemakaian") val deskripsiPemakaian: String? = null
)

@Serializable
data class BagianTanaman(
    @SerialName("id_bagian") val idBagian: Int,
    @SerialName("nama_bagian") val namaBagian: String,
    @SerialName("deskripsi_bagian") val deskripsiBagian: String? = null
)

@Serializable
data class Tutorial(
    @SerialName("id_tutorial") val idTutorial: Int,
    @SerialName("id_resep") val idResep: Int,
    @SerialName("urutan_langkah") val urutanLangkah: Int,
    @SerialName("deskripsi_langkah") val deskripsiLangkah: String,
    @SerialName("gambar_langkah") val gambarLangkah: String? = null
)

@Serializable
data class BahanResep(
    @SerialName("id_bahan") val idBahan: Int,
    @SerialName("id_resep") val idResep: Int,
    @SerialName("nama_bahan") val namaBahan: String,
    @SerialName("takaran") val takaran: String
)

@Serializable
data class Resep(
    @SerialName("id_resep") val idResep: Int,
    @SerialName("id_cara_pemakaian") val idCaraPemakaian: Int? = null,
    @SerialName("nama_resep") val namaResep: String,
    @SerialName("deskripsi_resep") val deskripsiResep: String? = null,
    @SerialName("gambar_resep") val gambarResep: String? = null,
    @SerialName("cara_pengolahan") val caraPengolahan: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("is_trending") val isTrending: Boolean? = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

// --- C. Detail Tanaman (Join Penyakit & Bagian Tanaman) ---
@Serializable
data class DetailTanamanResponse(
    @SerialName("id_tanaman") val idTanaman: Int,
    @SerialName("nama_tanaman") val namaTanaman: String,
    @SerialName("nama_latin") val namaLatin: String? = null,
    @SerialName("deskripsi_tanaman") val deskripsiTanaman: String? = null,
    @SerialName("habitat") val habitat: String? = null,
    @SerialName("gambar_tanaman") val gambarTanaman: String? = null,
    @SerialName("is_trending") val isTrending: Boolean? = false,
    
    // Nested Pivot: tanaman -> pivot_tanaman_penyakit -> penyakit
    @SerialName("pivot_tanaman_penyakit") val listPenyakitPivot: List<PenyakitPivot>? = null,
    
    // Nested Pivot: tanaman -> pivot_bagian_tanaman -> bagian_tanaman
    @SerialName("pivot_bagian_tanaman") val listBagianPivot: List<BagianPivot>? = null,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
) {
    @Serializable
    data class PenyakitPivot(@SerialName("penyakit") val penyakit: Penyakit? = null)
    
    @Serializable
    data class BagianPivot(@SerialName("bagian_tanaman") val bagian: BagianTanaman? = null)
    
    // Helper untuk mempermudah akses di UI
    val penyakitList: List<Penyakit> get() = listPenyakitPivot?.mapNotNull { it.penyakit } ?: emptyList()
    val bagianList: List<BagianTanaman> get() = listBagianPivot?.mapNotNull { it.bagian } ?: emptyList()
}

// --- D. Detail Resep (Join Cara Pemakaian, Tutorial, Bahan, Penyakit, Tanaman) ---
@Serializable
data class DetailResepResponse(
    @SerialName("id_resep") val idResep: Int,
    @SerialName("nama_resep") val namaResep: String,
    @SerialName("deskripsi_resep") val deskripsiResep: String? = null,
    @SerialName("gambar_resep") val gambarResep: String? = null,
    @SerialName("cara_pengolahan") val caraPengolahan: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("is_trending") val isTrending: Boolean? = false,
    
    // One-to-Many
    @SerialName("cara_pemakaian") val caraPemakaian: CaraPemakaian? = null,
    @SerialName("tutorial") val tutorialList: List<Tutorial>? = null,
    @SerialName("bahan_resep") val bahanList: List<BahanResep>? = null,
    
    // Many-to-Many Pivot
    @SerialName("pivot_resep_penyakit") val listPenyakitPivot: List<PenyakitPivot>? = null,
    @SerialName("pivot_resep_tanaman") val listTanamanPivot: List<TanamanPivot>? = null,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
) {
    @Serializable
    data class PenyakitPivot(@SerialName("penyakit") val penyakit: Penyakit? = null)
    
    @Serializable
    data class TanamanPivot(@SerialName("tanaman") val tanaman: Tanaman? = null)
    
    val penyakitList: List<Penyakit> get() = listPenyakitPivot?.mapNotNull { it.penyakit } ?: emptyList()
    val tanamanList: List<Tanaman> get() = listTanamanPivot?.mapNotNull { it.tanaman } ?: emptyList()
}
