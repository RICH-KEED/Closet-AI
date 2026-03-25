package com.closetai.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.closetai.app.ui.components.*
import com.closetai.app.ui.theme.Primary
import com.closetai.app.ui.viewmodel.OnboardingViewModel

data class SkinToneOption(
    val value: String,
    val label: String,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinToneScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val skinToneOptions = listOf(
        SkinToneOption("Fair",   "Fair",   Color(0xFFF5D5B8)),
        SkinToneOption("Light",  "Light",  Color(0xFFEAB88A)),
        SkinToneOption("Medium", "Medium", Color(0xFFD4956A)),
        SkinToneOption("Tan",    "Tan",    Color(0xFFBB7441)),
        SkinToneOption("Dark",   "Dark",   Color(0xFF7A4828)),
        SkinToneOption("Deep",   "Deep",   Color(0xFF3D1F10))
    )
    
    val undertoneOptions = listOf(
        "Cool (Blue/Pink)", "Warm (Yellow/Gold)", "Neutral"
    )
    
    val colorOptions = listOf(
        "Black" to Color(0xFF1A1A1A),
        "White" to Color(0xFFF8F8F8),
        "Charcoal" to Color(0xFF444444),
        "Navy Blue" to Color(0xFF1E3A5F),
        "Red" to Color(0xFFD32F2F),
        "Green" to Color(0xFF2E7D32),
        "Brown" to Color(0xFF6D4C41),
        "Yellow" to Color(0xFFF9D71C),
        "Pink" to Color(0xFFEC407A),
        "Purple" to Color(0xFF7B1FA2),
        "Light Blue" to Color(0xFF90CAF9),
        "Light Green" to Color(0xFFA5D6A7),
        "Light Grey" to Color(0xFFBDBDBD),
        "Pale Pink" to Color(0xFFF8BBD0),
        "Gold" to Color(0xFFD4AF37),
        "Silver" to Color(0xFFC0C0C0)
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background blobs
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-50).dp)
                .size(250.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Top bar
            OnboardingTopBar(
                currentStep = 4,
                totalSteps = 10,
                onBackClick = onBack
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = "Color & Appearance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Knowing your tones helps us suggest colors that complement you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Skin Tone with images
                Text(
                    text = "Skin Tone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(skinToneOptions) { option ->
                        SkinToneSwatch(
                            color = option.color,
                            label = option.label,
                            isSelected = viewModel.skinTone == option.value,
                            onClick = { viewModel.updateSkinTone(option.value) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Undertone
                Text(
                    text = "Undertone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                undertoneOptions.forEach { undertone ->
                    RadioCard(
                        title = undertone,
                        isSelected = viewModel.undertone == undertone,
                        onClick = { viewModel.updateUndertone(undertone) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Favorite Colors
                Text(
                    text = "Favorite Colors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(360.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colorOptions) { (name, color) ->
                        ColorSwatch(
                            color = color,
                            label = name,
                            isSelected = viewModel.favoriteColors.contains(name),
                            onClick = { viewModel.toggleFavoriteColor(name) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Continue button
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrimaryButton(
                    text = "Continue",
                    onClick = onContinue,
                    enabled = viewModel.skinTone.isNotEmpty() && viewModel.undertone.isNotEmpty()
                )
            }
        }
    }
}
