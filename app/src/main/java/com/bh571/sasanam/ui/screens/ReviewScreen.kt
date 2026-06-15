package com.bh571.sasanam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bh571.sasanam.data.MemoryField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    rawText: String,
    initialFields: List<MemoryField>,
    onSave: (String, String, List<MemoryField>) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val fields = remember { mutableStateListOf(*initialFields.toTypedArray()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Memory") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Extracted Fields", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(fields.size) { index ->
                    val field = fields[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = field.name,
                            onValueChange = { fields[index] = field.copy(name = it) },
                            label = { Text("Name") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = field.value,
                            onValueChange = { fields[index] = field.copy(value = it) },
                            label = { Text("Value") },
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onSave(title, description, fields) }) {
                    Text("Save")
                }
            }
        }
    }
}
