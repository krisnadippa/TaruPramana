package com.example.tarupramanata

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Panggil layout XML langsung
        setContentView(R.layout.activity_started)

        // 1. Cari tombol berdasarkan ID yang ada di XML (btnStart)
        // Pastikan di activity_started.xml ID-nya android:id="@+id/btnStart"
        val btnStart = findViewById<Button>(R.id.btnStart)

        // 2. Pasang aksi klik
        btnStart.setOnClickListener {
            // Pindah ke MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Tutup halaman ini agar tidak bisa kembali
        }
    }
}