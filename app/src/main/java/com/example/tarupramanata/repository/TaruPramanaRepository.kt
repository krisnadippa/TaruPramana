package com.example.tarupramanata.repository

import com.example.tarupramanata.data.model.*
import com.example.tarupramanata.network.SupabaseService
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaruPramanaRepository {
    private val postgrest = SupabaseService.client.postgrest

    // A. getDaftarTanaman()
    suspend fun getDaftarTanaman(): Result<List<Tanaman>> = withContext(Dispatchers.IO) {
        try {
            val response = postgrest["tanaman"]
                .select()
                .decodeList<Tanaman>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // B. getDaftarResep()
    suspend fun getDaftarResep(): Result<List<Resep>> = withContext(Dispatchers.IO) {
        try {
            val response = postgrest["resep"]
                .select()
                .decodeList<Resep>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // C. getDetailTanaman(id)
    suspend fun getDetailTanaman(idTanaman: Int): Result<DetailTanamanResponse> = withContext(Dispatchers.IO) {
        try {
            // Kita join ke tabel pivot, lalu dari pivot ke tabel target
            val selectColumns = """
                *,
                pivot_tanaman_penyakit(penyakit(*)),
                pivot_bagian_tanaman(bagian_tanaman(*))
            """.trimIndent()

            val response = postgrest["tanaman"]
                .select(columns = Columns.raw(selectColumns)) {
                    filter {
                        eq("id_tanaman", idTanaman)
                    }
                }
                .decodeSingle<DetailTanamanResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // D. getDetailResep(id)
    suspend fun getDetailResep(idResep: Int): Result<DetailResepResponse> = withContext(Dispatchers.IO) {
        try {
            val selectColumns = """
                *,
                cara_pemakaian(*),
                tutorial(*),
                bahan_resep(*),
                pivot_resep_penyakit(penyakit(*)),
                pivot_resep_tanaman(tanaman(*))
            """.trimIndent()

            val response = postgrest["resep"]
                .select(columns = Columns.raw(selectColumns)) {
                    filter {
                        eq("id_resep", idResep)
                    }
                }
                .decodeSingle<DetailResepResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // E. getDaftarTanamanDetail()
    suspend fun getDaftarTanamanDetail(): Result<List<DetailTanamanResponse>> = withContext(Dispatchers.IO) {
        try {
            val selectColumns = """
                *,
                pivot_tanaman_penyakit(penyakit(*)),
                pivot_bagian_tanaman(bagian_tanaman(*))
            """.trimIndent()

            val response = postgrest["tanaman"]
                .select(columns = Columns.raw(selectColumns))
                .decodeList<DetailTanamanResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // F. getDaftarResepDetail()
    suspend fun getDaftarResepDetail(): Result<List<DetailResepResponse>> = withContext(Dispatchers.IO) {
        try {
            val selectColumns = """
                *,
                cara_pemakaian(*),
                tutorial(*),
                bahan_resep(*),
                pivot_resep_penyakit(penyakit(*)),
                pivot_resep_tanaman(tanaman(*))
            """.trimIndent()

            val response = postgrest["resep"]
                .select(columns = Columns.raw(selectColumns))
                .decodeList<DetailResepResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // G. getDaftarPenyakitDetail()
    suspend fun getDaftarPenyakitDetail(): Result<List<DetailPenyakitResponse>> = withContext(Dispatchers.IO) {
        try {
            val selectColumns = """
                *,
                pivot_resep_penyakit(resep(*)),
                pivot_tanaman_penyakit(tanaman(*))
            """.trimIndent()

            val response = postgrest["penyakit"]
                .select(columns = Columns.raw(selectColumns))
                .decodeList<DetailPenyakitResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
