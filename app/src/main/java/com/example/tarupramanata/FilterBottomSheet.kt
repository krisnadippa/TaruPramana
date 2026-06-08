package com.example.tarupramanata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet(
    private val onFilterApplied: (List<String>) -> Unit
) : BottomSheetDialogFragment() {

    // CheckBox Kategori Artikel (Baru)
    private lateinit var cbTanaman: CheckBox
    private lateinit var cbResep: CheckBox
    private lateinit var cbPenyakit: CheckBox

    // CheckBox Bagian Tanaman (Lama)
    private lateinit var cbDaun: CheckBox
    private lateinit var cbBatang: CheckBox
    private lateinit var cbBunga: CheckBox
    private lateinit var cbAkar: CheckBox
    private lateinit var cbBuah: CheckBox

    // Tombol
    private lateinit var btnReset: Button
    private lateinit var btnApply: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Pastikan nama layout sesuai dengan XML Anda (sepertinya layout_bottom_sheet_filter.xml)
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Init Views (Hubungkan dengan ID di XML)
        cbTanaman = view.findViewById(R.id.cbKategoriTanaman)
        cbResep = view.findViewById(R.id.cbKategoriResep)
        cbPenyakit = view.findViewById(R.id.cbKategoriPenyakit)

        cbDaun = view.findViewById(R.id.cbDaun)
        cbBatang = view.findViewById(R.id.cbBatang)
        cbBunga = view.findViewById(R.id.cbBunga)
        cbAkar = view.findViewById(R.id.cbAkar)
        cbBuah = view.findViewById(R.id.cbBuah)

        btnReset = view.findViewById(R.id.btnReset)
        btnApply = view.findViewById(R.id.btnApply)

        // 2. Logika Tombol Hapus (Reset)
        btnReset.setOnClickListener {
            // Uncheck semua checkbox
            cbTanaman.isChecked = false
            cbResep.isChecked = false
            cbPenyakit.isChecked = false
            cbDaun.isChecked = false
            cbBatang.isChecked = false
            cbBunga.isChecked = false
            cbAkar.isChecked = false
            cbBuah.isChecked = false

            // Kirim list kosong ke SearchActivity (artinya reset filter)
            onFilterApplied(emptyList())
            dismiss()
        }

        // 3. Logika Tombol Pasang (Apply)
        btnApply.setOnClickListener {
            val selectedFilters = mutableListOf<String>()

            // Cek Kategori Artikel
            if (cbTanaman.isChecked) selectedFilters.add("Tanaman")
            if (cbResep.isChecked) selectedFilters.add("Resep")
            if (cbPenyakit.isChecked) selectedFilters.add("Penyakit")

            // Cek Bagian Tanaman
            if (cbDaun.isChecked) selectedFilters.add("Daun")
            if (cbBatang.isChecked) selectedFilters.add("Batang")
            if (cbBunga.isChecked) selectedFilters.add("Bunga")
            if (cbAkar.isChecked) selectedFilters.add("Akar")
            if (cbBuah.isChecked) selectedFilters.add("Buah")

            // Kirim list filter yang dipilih ke SearchActivity
            onFilterApplied(selectedFilters)
            dismiss()
        }
    }
}