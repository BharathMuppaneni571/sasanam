package com.bh571.sasanam.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bh571.sasanam.data.MemoryField
import com.google.mlkit.nl.entityextraction.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class MemoryExtractor(private val context: Context) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val entityExtractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )
    
    // Object detector for non-textual images (e.g. plants, objects)
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    suspend fun extractFromBitmap(bitmap: Bitmap): ExtractionResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText: Text = textRecognizer.process(image).await()
        
        // 1. Capture every text block
        val allTextBuilder = StringBuilder()
        for (block in visionText.textBlocks) {
            allTextBuilder.append(block.text).append("\n")
        }
        
        // 2. Object Detection for visual identifiers
        try {
            val objects: List<DetectedObject> = objectDetector.process(image).await()
            for (obj in objects) {
                for (label in obj.labels) {
                    allTextBuilder.append("Visual: ${label.text} (${(label.confidence * 100).toInt()}%)\n")
                }
            }
        } catch (e: Exception) {
            Log.e("MemoryExtractor", "Object detection failed", e)
        }
        
        val fullRawText = allTextBuilder.toString().trim()
        val fields = mutableListOf<MemoryField>()

        // 3. ML Kit Entity Extraction
        try {
            entityExtractor.downloadModelIfNeeded().await()
            val params = EntityExtractionParams.Builder(fullRawText).build()
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

        // 4. Custom Regex for Indian Formats
        extractRegexFields(fullRawText, fields)

        return ExtractionResult(fullRawText, fields)
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
