package com.example.tarupramanata

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat

class ScanActivity : AppCompatActivity() {

    private lateinit var imgPlaceholder: ImageView
    private lateinit var tvInstruction: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnCamera: LinearLayout
    private lateinit var btnGallery: LinearLayout

    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        imgPlaceholder = findViewById(R.id.imgPlaceholder)
        tvInstruction = findViewById(R.id.tvInstruction)
        tvResult = findViewById(R.id.tvSubInstruction)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)

        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navChatbot = findViewById<LinearLayout>(R.id.navChatbot)

        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            onError = { errorMsg ->
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        )

        // Kamera
        btnCamera.setOnClickListener {
            resetTampilan() // Membersihkan layar sebelum kamera terbuka
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                bukaKameraHighRes()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Galeri
        btnGallery.setOnClickListener {
            resetTampilan() // Membersihkan layar sebelum galeri terbuka
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            galleryLauncher.launch(Intent.createChooser(intent, "Pilih Foto"))
        }

        // Navigasi
        navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        navChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
    }

    // ================= FUNGSI RESET TAMPILAN =================
    private fun resetTampilan() {
        imgPlaceholder.setImageResource(R.drawable.cam1) // Mengembalikan ke ikon default
        tvInstruction.text = "Silakan pilih foto atau buka kamera"
        tvResult.text = ""
    }

    // Membersihkan layar saat user menekan tombol back dari DetailScanActivity
    override fun onResume() {
        super.onResume()
        resetTampilan()
    }

    // ================= KAMERA =================
    private fun bukaKameraHighRes() {
        val photoFile = File.createTempFile("scan_tanaman_", ".jpg", externalCacheDir)
        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

        try {
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) bukaKameraHighRes()
            else Toast.makeText(this, "Perlu izin kamera", Toast.LENGTH_SHORT).show()
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                photoUri?.let { uri ->
                    val bitmap = uriToBitmap(uri)

                    if (bitmap != null) {
                        val rotatedBitmap = fixImageRotation(uri, bitmap)
                        prosesGambarOffline(rotatedBitmap)
                    } else {
                        Toast.makeText(this, "Gambar tidak valid", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    // ================= GALERI =================
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data

                if (uri != null) {
                    val mimeType = contentResolver.getType(uri)

                    if (mimeType?.startsWith("image/") == true) {
                        val bitmap = uriToBitmap(uri)

                        if (bitmap != null) {
                            val rotatedBitmap = fixImageRotation(uri, bitmap)
                            prosesGambarOffline(rotatedBitmap)
                        } else {
                            Toast.makeText(this, "File bukan gambar yang valid", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Hanya file gambar yang diperbolehkan", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    // ================= PROSES AI =================
    private fun prosesGambarOffline(bitmap: Bitmap) {
        imgPlaceholder.setImageBitmap(bitmap)
        tvInstruction.text = "Menganalisis gambar..."
        tvResult.text = "Mohon tunggu..."

        val results = imageClassifierHelper.classify(bitmap)

        if (!results.isNullOrEmpty()) {
            val topResult = results[0]

            var plantName = topResult.label
            val confidence = topResult.score
            val confidencePercent =
                NumberFormat.getPercentInstance().format(confidence.toDouble())

            val isLowAccuracy = confidence < 0.80f
            if (isLowAccuracy) {
                plantName = "Tidak Dikenali"
                tvInstruction.text = "Hasil Kurang Akurat"
                tvResult.text = "Kemiripan rendah ($confidencePercent). Sistem tidak yakin."
                Toast.makeText(this, "Akurasi rendah", Toast.LENGTH_SHORT).show()
            } else {
                tvInstruction.text = "Selesai!"
                tvResult.text = "Terdeteksi: $plantName ($confidencePercent)"
            }

            val intent = Intent(this, DetailScanActivity::class.java)
            intent.putExtra("DETECTED_NAME", plantName)
            intent.putExtra("DETECTED_CONFIDENCE", confidencePercent)
            intent.putExtra("IS_LOW_ACCURACY", isLowAccuracy)

            val imagePath = saveImageToCache(bitmap)
            intent.putExtra("IMAGE_PATH", imagePath)

            startActivity(intent)

        } else {
            tvInstruction.text = "Gagal mengenali tanaman."
            tvResult.text = "Tidak ada objek yang terdeteksi."
        }
    }

    // ================= HELPER =================
    private fun fixImageRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val stream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveImageToCache(bitmap: Bitmap): String {
        val file = File(cacheDir, "scan_result.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        return file.absolutePath
    }
}