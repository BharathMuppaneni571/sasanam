package com.bh571.sasanam

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bh571.sasanam.data.*
import com.bh571.sasanam.ml.ExtractionResult
import com.bh571.sasanam.ml.MemoryExtractor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val memoryDao: MemoryDao,
    private val memoryExtractor: MemoryExtractor
) : ViewModel() {

    val memories: StateFlow<List<Memory>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var extractionResult by mutableStateOf<ExtractionResult?>(null)
        private set

    var queryAnswer by mutableStateOf<String?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    fun processCapturedImage(bitmap: Bitmap) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val result = memoryExtractor.extractFromBitmap(bitmap)
                extractionResult = result
            } finally {
                isProcessing = false
            }
        }
    }

    fun saveMemory(title: String, description: String, fields: List<MemoryField>) {
        viewModelScope.launch {
            val memory = Memory(
                type = "General", // Default for now
                title = title,
                description = description,
                rawText = extractionResult?.rawText,
                createdAt = System.currentTimeMillis(),
                latitude = null,
                longitude = null,
                source = "camera"
            )
            memoryDao.insertMemoryWithFields(memory, fields)
            extractionResult = null
        }
    }

    fun askQuery(query: String) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val dbQuery = "%$query%"
                val results = memoryDao.searchMemories(dbQuery)
                
                if (results.isEmpty()) {
                    queryAnswer = "I couldn't find any memories related to \"$query\"."
                } else {
                    val response = StringBuilder("I found ${results.size} relevant memory(s):\n\n")
                    results.forEach { memory ->
                        response.append("• ${memory.title ?: "Untitled"}")
                        val fields = memoryDao.getFieldsForMemory(memory.id)
                        if (fields.isNotEmpty()) {
                            val detail = fields.joinToString { "${it.name}: ${it.value}" }
                            response.append(" ($detail)")
                        }
                        response.append("\n")
                    }
                    queryAnswer = response.toString()
                }
            } catch (e: Exception) {
                queryAnswer = "Error searching: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun clearExtractionResult() {
        extractionResult = null
    }
}
