package com.closetai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.closetai.app.data.repository.UserRepository
import com.closetai.app.ui.theme.Primary
import com.closetai.app.util.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewRecommendations: (Map<String, String>) -> Unit,
    onNavigateToWardrobe: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val userRepository = remember { UserRepository() }
    val locationHelper = remember { LocationHelper(context) }
    
    var userLocation by remember { mutableStateOf<Map<String, String>?>(null) }
    var showDynamicContextDialog by remember { mutableStateOf(false) }
    var occasion by remember { mutableStateOf("") }
    var dressCode by remember { mutableStateOf("") }
    var weatherFeel by remember { mutableStateOf("") }
    var outingBudget by remember { mutableStateOf("") }
    var vibe by remember { mutableStateOf("") }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
                locationHelper.getCurrentLocation { location ->
                    location?.let {
                        userLocation = mapOf(
                            "lat" to it.latitude.toString(),
                            "lon" to it.longitude.toString()
                        )
                    }
                }
            }
        }
    )

    // Request on mount
    LaunchedEffect(Unit) {
        if (!locationHelper.hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            locationHelper.getCurrentLocation { location ->
                location?.let {
                    userLocation = mapOf(
                        "lat" to it.latitude.toString(),
                        "lon" to it.longitude.toString()
                    )
                }
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Text("🏠") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { /* Already here */ }
                )
                NavigationBarItem(
                    icon = { Text("👕") },
                    label = { Text("Wardrobe") },
                    selected = false,
                    onClick = onNavigateToWardrobe
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Background decoration
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-50).dp)
                    .size(300.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "ClosetAI",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                        IconButton(
                            onClick = {
                                userRepository.signOut()
                                onSignOut()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
                
                // Main content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // User avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Welcome message
                    Text(
                        text = "Welcome, ${user?.displayName ?: "Fashionista"}!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Your personalized fashion experience is coming soon.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Coming soon card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🚀",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Fashion Recommendations",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AI-powered outfit suggestions are being prepared based on your preferences.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Always available: fetch recommendations from saved profile only.
                                    onViewRecommendations(emptyMap())
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape
                            ) {
                                Text("View Profile Recommendations")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    // Optional: ask current context and fetch a fresh contextual set.
                                    showDynamicContextDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape
                            ) {
                                Text("Get New Recommendations")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDynamicContextDialog) {
        DynamicRecommendationContextDialog(
            occasion = occasion,
            onOccasionChange = { occasion = it },
            dressCode = dressCode,
            onDressCodeChange = { dressCode = it },
            weatherFeel = weatherFeel,
            onWeatherFeelChange = { weatherFeel = it },
            outingBudget = outingBudget,
            onOutingBudgetChange = { outingBudget = it },
            vibe = vibe,
            onVibeChange = { vibe = it },
            onDismiss = { showDynamicContextDialog = false },
            onContinue = {
                val dynamicContext = buildMap<String, String> {
                    userLocation?.let { putAll(it) }
                    if (occasion.isNotBlank()) put("occasion", occasion.trim())
                    if (dressCode.isNotBlank()) put("dress_code", dressCode.trim())
                    if (weatherFeel.isNotBlank()) put("weather_feel", weatherFeel.trim())
                    if (outingBudget.isNotBlank()) put("occasion_budget", outingBudget.trim())
                    if (vibe.isNotBlank()) put("style_vibe", vibe.trim())
                }
                showDynamicContextDialog = false
                onViewRecommendations(dynamicContext)
            }
        )
    }
}

@Composable
private fun DynamicRecommendationContextDialog(
    occasion: String,
    onOccasionChange: (String) -> Unit,
    dressCode: String,
    onDressCodeChange: (String) -> Unit,
    weatherFeel: String,
    onWeatherFeelChange: (String) -> Unit,
    outingBudget: String,
    onOutingBudgetChange: (String) -> Unit,
    vibe: String,
    onVibeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    val quickOccasions = listOf("Party", "Formal", "Casual", "Office", "Wedding", "Travel")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quick Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tell us what changed today (3-5 quick inputs).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickOccasions.take(3).forEach { option ->
                        QuickChoiceChip(
                            text = option,
                            selected = occasion.equals(option, ignoreCase = true),
                            onClick = { onOccasionChange(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickOccasions.drop(3).forEach { option ->
                        QuickChoiceChip(
                            text = option,
                            selected = occasion.equals(option, ignoreCase = true),
                            onClick = { onOccasionChange(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = occasion,
                    onValueChange = onOccasionChange,
                    label = { Text("Occasion") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dressCode,
                    onValueChange = onDressCodeChange,
                    label = { Text("Dress code (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weatherFeel,
                    onValueChange = onWeatherFeelChange,
                    label = { Text("Weather feel (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = outingBudget,
                    onValueChange = onOutingBudgetChange,
                    label = { Text("Budget for this outing (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vibe,
                    onValueChange = onVibeChange,
                    label = { Text("Style vibe (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Get Recommendations")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun QuickChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
