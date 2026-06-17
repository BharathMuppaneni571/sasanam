package com.bh571.sasanam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.bh571.sasanam.data.AppDatabase
import com.bh571.sasanam.datastore.SettingsRepository
import com.bh571.sasanam.ml.GenAIManager
import com.bh571.sasanam.ml.MemoryExtractor
import com.bh571.sasanam.ui.screens.*
import com.bh571.sasanam.ui.screens.details.MemoryDetailScreen
import com.bh571.sasanam.ui.theme.SasanamTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this, 
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 
                0
            )
        }

        val passphrase = "dummy_passphrase".toByteArray()
        val db = AppDatabase.getDatabase(this, passphrase)
        val settingsRepository = SettingsRepository(this)
        val genAIManager = GenAIManager(this)
        val viewModel = MainViewModel(db.memoryDao(), MemoryExtractor(this), settingsRepository, genAIManager)

        setContent {
            SasanamTheme {
                val isUnlocked = viewModel.isAppUnlocked
                val biometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

                if (biometricEnabled && !isUnlocked) {
                    AuthScreen(onUnlocked = { viewModel.unlockApp() })
                } else {
                    SasanamApp(viewModel)
                }
            }
        }
    }

    private fun hasPermissions(): Boolean = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        this, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("dashboard", "Home", Icons.Default.Home)
    object Ask : Screen("ask", "Ask", Icons.Default.ChatBubbleOutline)
    object Timeline : Screen("timeline", "Timeline", Icons.Default.Timeline)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current as FragmentActivity
    val executor = remember { ContextCompat.getMainExecutor(context) }
    
    val biometricPrompt = remember {
        BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onUnlocked()
            }
        })
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Sasanam")
            .setSubtitle("Authenticate to access your memories")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
    }

    LaunchedEffect(Unit) {
        biometricPrompt.authenticate(promptInfo)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("App Locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { biometricPrompt.authenticate(promptInfo) }) {
                Text("Unlock Now")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SasanamApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

    var showInputMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val items = listOf(Screen.Home, Screen.Ask, Screen.Timeline, Screen.Settings)
    val showBottomBar = items.any { it.route == currentDestination?.route }

    // Use a key to ensure we recalculate if onboarding status changes initially
    val startDestination = remember(isOnboardingCompleted) {
        if (isOnboardingCompleted) Screen.Home.route else "onboarding"
    }

    // If we're on onboarding but it's already completed, move to home automatically
    LaunchedEffect(isOnboardingCompleted, currentDestination) {
        if (isOnboardingCompleted && currentDestination?.route == "onboarding") {
            navController.navigate(Screen.Home.route) {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Box {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(80.dp)
                    ) {
                        items.forEachIndexed { index, screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                                label = { Text(screen.label, fontSize = 10.sp) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            // Space for central FAB
                            if (index == 1) {
                                Spacer(Modifier.width(80.dp))
                            }
                        }
                    }
                    
                    // Central Plus FAB with soft shadow
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-32).dp)
                            .size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp
                    ) {
                        IconButton(
                            onClick = { showInputMenu = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Add", 
                                tint = MaterialTheme.colorScheme.onPrimary, 
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (showInputMenu) {
            ModalBottomSheet(
                onDismissRequest = { showInputMenu = false },
                sheetState = sheetState
            ) {
                InputTypeSelectionMenu(
                    onOptionSelected = { type ->
                        showInputMenu = false
                        when (type) {
                            "camera" -> navController.navigate("capture")
                            "voice" -> navController.navigate("voice_input")
                            "text" -> navController.navigate("text_input")
                        }
                    }
                )
            }
        }

        val memories by viewModel.memories.collectAsStateWithLifecycle()
        val context = LocalContext.current

        NavHost(navController, startDestination = startDestination, modifier = Modifier.padding(innerPadding)) {
            composable("onboarding") {
                OnboardingScreen(onGetStarted = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }
            composable(Screen.Home.route) {
                DashboardScreen(
                    memories = memories,
                    onAddClick = { showInputMenu = true },
                    onSearchClick = { navController.navigate(Screen.Ask.route) },
                    onMemoryClick = {
                        viewModel.selectMemory(it)
                        navController.navigate("memory_detail")
                    }
                )
            }
            composable(Screen.Ask.route) {
                AskScreen(
                    onQuery = { viewModel.askQuery(it) },
                    answer = viewModel.queryAnswer,
                    isProcessing = viewModel.isProcessing,
                    onBack = { navController.popBackStack() },
                    onNavigateToMemory = { memoryId ->
                        // Helper to select by ID
                        viewModel.memories.value.find { it.id == memoryId }?.let {
                            viewModel.selectMemory(it)
                            navController.navigate("memory_detail")
                        }
                    },
                    onClearAnswer = { viewModel.clearQueryAnswer() }
                )
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(memories = memories, onMemoryClick = {
                    viewModel.selectMemory(it)
                    navController.navigate("memory_detail")
                })
            }
            composable(Screen.Settings.route) {
                val biometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
                SettingsScreen(
                    biometricEnabled = biometricEnabled,
                    onBiometricToggle = { viewModel.setBiometricEnabled(it) }
                )
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
            composable("voice_input") {
                VoiceInputScreen(
                    onVoiceCaptured = { text ->
                        viewModel.processTextInput(text)
                        navController.navigate("review")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("text_input") {
                TextInputScreen(
                    onTextEntered = { text ->
                        viewModel.processTextInput(text)
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
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
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
            composable("memory_detail") {
                val pair = viewModel.selectedMemoryWithFields
                if (pair != null) {
                    MemoryDetailScreen(
                        memory = pair.first,
                        fields = pair.second,
                        onBack = { 
                            viewModel.clearSelectedMemory()
                            navController.popBackStack() 
                        },
                        onDelete = {
                            viewModel.deleteMemory(pair.first)
                            navController.popBackStack()
                        },
                        onUpdate = { updatedMemory ->
                            viewModel.updateMemory(updatedMemory)
                        }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
        }
    }
}

@Composable
fun InputTypeSelectionMenu(onOptionSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add new memory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InputOption(Icons.Default.PhotoCamera, "Camera") { onOptionSelected("camera") }
            InputOption(Icons.Default.Mic, "Voice") { onOptionSelected("voice") }
            InputOption(Icons.Default.EditNote, "Text") { onOptionSelected("text") }
            InputOption(Icons.Default.Description, "Document") { onOptionSelected("camera") } // Use camera for now
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun InputOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
