package com.bh571.sasanam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.bh571.sasanam.data.AppDatabase
import com.bh571.sasanam.ml.MemoryExtractor
import com.bh571.sasanam.ui.screens.*
import com.bh571.sasanam.ui.theme.SasanamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }

        val passphrase = "dummy_passphrase".toByteArray()
        val db = AppDatabase.getDatabase(this, passphrase)
        val viewModel = MainViewModel(db.memoryDao(), MemoryExtractor(this))

        setContent {
            SasanamTheme {
                SasanamApp(viewModel)
            }
        }
    }

    private fun hasPermissions(): Boolean = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("dashboard", "Home", Icons.Default.Home)
    object Ask : Screen("ask", "Ask", Icons.Default.ChatBubbleOutline)
    object Timeline : Screen("timeline", "Timeline", Icons.Default.Timeline)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun SasanamApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(Screen.Home, Screen.Ask, Screen.Timeline, Screen.Settings)
    val showBottomBar = items.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentDestination?.route == Screen.Home.route || currentDestination?.route == Screen.Timeline.route) {
                FloatingActionButton(
                    onClick = { navController.navigate("capture") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Add Memory", modifier = Modifier.size(32.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        val memories by viewModel.memories.collectAsStateWithLifecycle()
        val context = LocalContext.current

        NavHost(navController, startDestination = "onboarding", modifier = Modifier.padding(innerPadding)) {
            composable("onboarding") {
                OnboardingScreen(onGetStarted = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }
            composable(Screen.Home.route) {
                DashboardScreen(
                    memories = memories,
                    onAddClick = { navController.navigate("capture") },
                    onSearchClick = { navController.navigate("ask") },
                    onMemoryClick = { /* TODO */ }
                )
            }
            composable(Screen.Ask.route) {
                AskScreen(
                    onQuery = { viewModel.askQuery(it) },
                    answer = viewModel.queryAnswer,
                    isProcessing = viewModel.isProcessing,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(memories = memories, onMemoryClick = { /* TODO */ })
            }
            composable(Screen.Settings.route) {
                // Placeholder for Settings
                Text("Settings Screen", modifier = Modifier.fillMaxSize())
            }
            composable("capture") {
                CaptureScreen(
                    onImageCaptured = { bitmap ->
                        ContextCompat.getMainExecutor(context).execute {
                            viewModel.processCapturedImage(bitmap)
                            navController.navigate("review")
                        }
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
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
        }
    }
}
