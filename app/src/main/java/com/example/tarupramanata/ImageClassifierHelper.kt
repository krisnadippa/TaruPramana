package com.example.tarupramanata

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.min

data class ClassificationResult(
    val label: String,
    val score: Float
)

class ImageClassifierHelper(
    private val context: Context,
    private val onError: (String) -> Unit
) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    private val modelName = "model1.tflite"
    private val labelName = "labels1.txt"

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, modelName)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            interpreter = Interpreter(modelFile, options)
            labels = FileUtil.loadLabels(context, labelName)

        } catch (e: Exception) {
            onError("Gagal memuat model atau labels: ${e.message}")
            e.printStackTrace()
        }
    }

    fun classify(image: Bitmap): List<ClassificationResult> {
        val localInterpreter = interpreter ?: run {
            setupClassifier()
            interpreter ?: return emptyList()
        }

        if (labels.isEmpty()) {
            onError("Labels kosong. Periksa file labels.txt di folder assets.")
            return emptyList()
        }

        // Pastikan format bitmap ARGB_8888
        val argbBitmap = if (image.config == Bitmap.Config.ARGB_8888) {
            image
        } else {
            image.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Center crop agar bentuk gambar tidak gepeng
        val cropSize = min(argbBitmap.width, argbBitmap.height)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))

            // PENTING:
            // Tidak pakai NormalizeOp.
            // Karena hasil Colab yang benar adalah input 0-255 tanpa normalisasi.
            // Jangan gunakan:
            // .add(NormalizeOp(127.5f, 127.5f))
            // Jangan gunakan:
            // .add(NormalizeOp(0f, 255f))

            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(argbBitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, labels.size),
            DataType.FLOAT32
        )

        return try {
            val startTime = System.currentTimeMillis()
            localInterpreter.run(
                tensorImage.buffer,
                outputBuffer.buffer.rewind()
            )
            val inferenceTime = System.currentTimeMillis() - startTime
            val runtime = Runtime.getRuntime()
            val ramUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            android.util.Log.i("PerformanceTest", "TFLite Inference Time: $inferenceTime ms | RAM Used: $ramUsed MB")

            val scores = outputBuffer.floatArray

            labels.indices.map { i ->
                ClassificationResult(
                    label = labels[i],
                    score = scores.getOrElse(i) { 0f }
                )
            }.sortedByDescending { it.score }

        } catch (e: Exception) {
            onError("Gagal melakukan prediksi: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}