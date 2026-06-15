package com.bh571.sasanam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bh571.sasanam.data.Memory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    memories: List<Memory>,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMemoryClick: (Memory) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sasanam") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Memory")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(memories) { memory ->
                MemoryCard(memory = memory, onClick = { onMemoryClick(memory) })
            }
        }
    }
}

@Composable
fun MemoryCard(memory: Memory, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = memory.title ?: "Untitled", style = MaterialTheme.typography.titleMedium)
            Text(text = memory.type, style = MaterialTheme.typography.bodySmall)
            memory.description?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
