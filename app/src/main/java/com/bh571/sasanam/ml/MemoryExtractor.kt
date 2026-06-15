package com.bh571.sasanam.ml

import android.content.Context
import android.graphics.Bitmap
import com.bh571.sasanam.data.MemoryField
import com.google.mlkit.nl.entityextraction.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class MemoryExtractor(private val context: Context) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val entityExtractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )

    suspend fun extractFromBitmap(bitmap: Bitmap): ExtractionResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = textRecognizer.process(image).await()
        val rawText = visionText.text

        val fields = mutableListOf<MemoryField>()

        // 1. ML Kit Entity Extraction
        try {
            entityExtractor.downloadModelIfNeeded().await()
            val params = EntityExtractionParams.Builder(rawText).build()
            val entities = entityExtractor.annotate(params).await()

            for (entityAnnotation in entities) {
                for (entity in entityAnnotation.entities) {
                    when (entity.type) {
                        Entity.TYPE_DATE_TIME -> {
                            fields.add(MemoryField(memoryId = 0, name = "date", value = entityAnnotation.annotatedText, confidence = 0.9f))
                        }
                        Entity.TYPE_MONEY -> {
                            fields.add(MemoryField(memoryId = 0, name = "amount", value = entityAnnotation.annotatedText, confidence = 0.9f))
                        }
                        Entity.TYPE_ADDRESS -> {
                            fields.add(MemoryField(memoryId = 0, name = "address", value = entityAnnotation.annotatedText, confidence = 0.8f))
                        }
                        Entity.TYPE_PHONE -> {
                            fields.add(MemoryField(memoryId = 0, name = "phone", value = entityAnnotation.annotatedText, confidence = 0.9f))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle entity extraction failure
        }

        // 2. Custom Regex for Indian Formats
        extractRegexFields(rawText, fields)

        return ExtractionResult(rawText, fields)
    }

    private fun extractRegexFields(text: String, fields: MutableList<MemoryField>) {
        // ₹ Amounts
        val rupeeRegex = Regex("₹\\s?([0-9,]+\\.?[0-9]*)")
        rupeeRegex.find(text)?.let {
            fields.add(MemoryField(memoryId = 0, name = "amount", value = it.value, confidence = 0.95f))
        }

        // PAN
        val panRegex = Regex("[A-Z]{5}\\d{4}[A-Z]")
        panRegex.find(text)?.let {
            fields.add(MemoryField(memoryId = 0, name = "PAN", value = it.value, confidence = 0.99f))
        }

        // Aadhaar
        val aadhaarRegex = Regex("\\d{4}\\s?\\d{4}\\s?\\d{4}")
        aadhaarRegex.find(text)?.let {
            fields.add(MemoryField(memoryId = 0, name = "Aadhaar", value = it.value, confidence = 0.99f))
        }
    }
}

data class ExtractionResult(
    val rawText: String,
    val fields: List<MemoryField>
)
