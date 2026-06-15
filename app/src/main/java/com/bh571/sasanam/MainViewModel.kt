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
                // Simple placeholder logic for now
                // In v2, this will use vector search + LLM
                queryAnswer = "Searching for: $query... (Logic pending implementation)"
            } finally {
                isProcessing = false
            }
        }
    }

    fun clearExtractionResult() {
        extractionResult = null
    }
}
