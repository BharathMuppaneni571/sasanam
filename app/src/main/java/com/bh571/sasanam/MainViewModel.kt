package com.bh571.sasanam

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bh571.sasanam.data.*
import com.bh571.sasanam.datastore.SettingsRepository
import com.bh571.sasanam.ml.ExtractionResult
import com.bh571.sasanam.ml.GenAIManager
import com.bh571.sasanam.ml.MemoryExtractor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val memoryDao: MemoryDao,
    private val memoryExtractor: MemoryExtractor,
    private val settingsRepository: SettingsRepository,
    private val genAIManager: GenAIManager
) : ViewModel() {

    val memories: StateFlow<List<Memory>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnboardingCompleted: StateFlow<Boolean> = settingsRepository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBiometricEnabled: StateFlow<Boolean> = settingsRepository.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val primaryColorHex: StateFlow<String> = settingsRepository.primaryColorHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#673AB7")

    var isAppUnlocked by mutableStateOf(false)
        private set

    fun unlockApp() {
        isAppUnlocked = true
    }

    var extractionResult by mutableStateOf<ExtractionResult?>(null)
        private set

    var selectedMemoryWithFields by mutableStateOf<Pair<Memory, List<MemoryField>>?>(null)
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

    fun processTextInput(text: String) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val fields = genAIManager.extractContext(text)
                extractionResult = ExtractionResult(text, fields)
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
                // Semantic simulation: expand query with synonyms or related terms
                val expandedTerms = listOf(query) + when {
                    query.contains("aadhar", true) || query.contains("uidai", true) -> listOf("identity", "card", "3651")
                    query.contains("pan", true) -> listOf("income tax", "tax", "permanent account")
                    query.contains("bill", true) -> listOf("invoice", "receipt", "payment", "spent")
                    query.contains("insurance", true) || query.contains("policy", true) -> listOf("expiry", "valid", "premium")
                    else -> emptyList()
                }

                val allResults = mutableSetOf<Memory>()
                expandedTerms.forEach { term ->
                    allResults.addAll(memoryDao.searchMemories("%${term.trim()}%"))
                }
                
                if (allResults.isEmpty()) {
                    queryAnswer = "I couldn't find any memories related to \"$query\"."
                } else {
                    // Ranking: prioritize exact matches in title or description
                    val rankedResults = allResults.sortedByDescending { memory ->
                        var score = 0
                        if (memory.title?.contains(query, true) == true) score += 10
                        if (memory.description?.contains(query, true) == true) score += 5
                        if (memory.rawText?.contains(query, true) == true) score += 2
                        score
                    }

                    val response = StringBuilder()
                    rankedResults.take(3).forEach { memory ->
                        // Add Match ID for clickability in UI
                        response.append("MATCH_ID:${memory.id}\n")
                        response.append("Memory: ${memory.title}\n")
                        
                        val fields = memoryDao.getFieldsForMemory(memory.id)
                        // If user asked for a specific value (like number), try to find it
                        val specificValue = fields.find { it.name.contains("number", true) || it.name.contains("Aadhar", true) || it.name.contains("PAN", true) || it.name.contains("id", true) }
                        
                        if (query.contains("number", true) && specificValue != null) {
                            response.append("The ${specificValue.name} is: ${specificValue.value}\n")
                        } else {
                            fields.forEach { field ->
                                response.append("${field.name}: ${field.value}\n")
                            }
                        }
                        response.append("\n")
                    }
                    
                    // Final LLM Polish (Simulated RAG)
                    queryAnswer = genAIManager.processQueryWithLLM(query, response.toString())
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

    fun clearQueryAnswer() {
        queryAnswer = null
    }

    fun clearAllData() {
        viewModelScope.launch {
            // This would normally be more complex in a real app (clearing files, etc)
            // But for now, we clear the DB
            memories.value.forEach { memoryDao.deleteMemory(it) }
        }
    }

    fun selectMemory(memory: Memory) {
        viewModelScope.launch {
            val fields = memoryDao.getFieldsForMemory(memory.id)
            selectedMemoryWithFields = memory to fields
        }
    }

    fun clearSelectedMemory() {
        selectedMemoryWithFields = null
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch {
            memoryDao.deleteMemory(memory)
            clearSelectedMemory()
        }
    }

    fun updateMemory(memory: Memory) {
        viewModelScope.launch {
            memoryDao.updateMemory(memory)
            // Refresh selection
            selectMemory(memory)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    fun setPrimaryColor(hex: String) {
        viewModelScope.launch {
            settingsRepository.setPrimaryColorHex(hex)
        }
    }
}
