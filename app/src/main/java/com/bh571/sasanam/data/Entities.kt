package com.bh571.sasanam.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String?,
    val description: String?,
    val rawText: String?,         // optional
    val createdAt: Long,         // epoch ms
    val latitude: Double?,       // nullable if no geotag
    val longitude: Double?,
    val source: String
)

@Entity(
    tableName = "memory_fields",
    foreignKeys = [ForeignKey(
        entity = Memory::class,
        parentColumns = ["id"],
        childColumns = ["memoryId"],
        onDelete = CASCADE
    )],
    indices = [Index(value = ["memoryId"])]
)
data class MemoryField(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoryId: Long,
    val name: String,
    val value: String,
    val confidence: Float
)
