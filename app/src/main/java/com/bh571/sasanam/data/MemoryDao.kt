package com.bh571.sasanam.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert
    suspend fun insertMemory(memory: Memory): Long

    @Insert
    suspend fun insertFields(fields: List<MemoryField>)

    @Transaction
    suspend fun insertMemoryWithFields(memory: Memory, fields: List<MemoryField>) {
        val memoryId = insertMemory(memory)
        val fieldsWithId = fields.map { it.copy(memoryId = memoryId) }
        insertFields(fieldsWithId)
    }

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memory_fields WHERE memoryId = :memoryId")
    suspend fun getFieldsForMemory(memoryId: Long): List<MemoryField>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): Memory?

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("SELECT * FROM memories WHERE type = :type")
    fun getMemoriesByType(type: String): Flow<List<Memory>>
}
