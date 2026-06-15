package com.bh571.sasanam

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bh571.sasanam.data.AppDatabase
import com.bh571.sasanam.ml.MemoryExtractor
import com.bh571.sasanam.ui.screens.*
import com.bh571.sasanam.ui.theme.SasanamTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request Permissions
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }

        // Initialize Database and Repository (Simplified for demo)
        // In a real app, use Hilt or similar for DI
        val passphrase = "dummy_passphrase".toByteArray()
        val db = AppDatabase.getDatabase(this, passphrase)
        val viewModel = MainViewModel(db.memoryDao(), MemoryExtractor(this))

        setContent {
            SasanamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SasanamApp(viewModel)
                }
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun SasanamApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val memories by viewModel.memories.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                memories = memories,
                onAddClick = { navController.navigate("capture") },
                onSearchClick = { navController.navigate("ask") },
                onMemoryClick = { /* TODO */ }
            )
        }
        composable("capture") {
            CaptureScreen(
                onImageCaptured = { bitmap ->
                    viewModel.processCapturedImage(bitmap)
                    navController.navigate("review")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("review") {
            val result = viewModel.extractionResult
            if (result != null) {
                ReviewScreen(
                    rawText = result.rawText,
                    initialFields = result.fields,
                    onSave = { title, desc, fields ->
                        viewModel.saveMemory(title, desc, fields)
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    onCancel = {
                        viewModel.clearExtractionResult()
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        composable("ask") {
            AskScreen(
                onQuery = { viewModel.askQuery(it) },
                answer = viewModel.queryAnswer,
                isProcessing = viewModel.isProcessing,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
