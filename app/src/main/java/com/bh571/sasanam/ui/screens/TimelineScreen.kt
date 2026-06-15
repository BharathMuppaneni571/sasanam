package com.bh571.sasanam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bh571.sasanam.data.Memory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(memories: List<Memory>, onMemoryClick: (Memory) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->
        val groupedMemories = memories.groupBy { 
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.createdAt))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedMemories.forEach { (month, monthMemories) ->
                item {
                    Text(
                        text = month,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(monthMemories) { memory ->
                    TimelineItem(memory, onClick = { onMemoryClick(memory) })
                }
            }
        }
    }
}

@Composable
fun TimelineItem(memory: Memory, onClick: () -> Unit) {
    val day = SimpleDateFormat("dd", Locale.getDefault()).format(Date(memory.createdAt))
    val monthShort = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(memory.createdAt))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(text = day, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = monthShort.uppercase(), style = MaterialTheme.typography.bodySmall)
        }
        
        Card(
            modifier = Modifier.weight(1f),
            onClick = onClick
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = memory.title ?: "Untitled", style = MaterialTheme.typography.titleMedium)
                Text(text = memory.description ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
    }
}
