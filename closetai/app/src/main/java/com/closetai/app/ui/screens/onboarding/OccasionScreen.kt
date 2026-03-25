package com.closetai.app.ui.screens.onboarding

import androidx.annotation.DrawableRes
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
import com.closetai.app.R
import com.closetai.app.ui.components.*
import com.closetai.app.ui.theme.Primary
import com.closetai.app.ui.viewmodel.OnboardingViewModel

data class OccasionOption(
    val value: String,
    val label: String,
    @DrawableRes val imageRes: Int
)

@Composable
fun OccasionScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val occasionOptions = listOf(
        OccasionOption("Work / Professional", "Work", R.drawable.style_officewear),
        OccasionOption("Casual Daily", "Casual Daily", R.drawable.style_casual),
        OccasionOption("Special Events / Party", "Party", R.drawable.style_partywear),
        OccasionOption("Sports / Active", "Sports", R.drawable.style_gymwear),
        OccasionOption("Travel / Vacation", "Travel", R.drawable.style_vacationwear),
        OccasionOption("Lounge / Home", "Lounge", R.drawable.garment_loungewear)
    )
    
    val lifestyleOptions = listOf(
        "Student", "Professional", "Active / Outdoorsy", 
        "Urban / City", "Creative", "Home-based"
    )
    
    val climateOptions = listOf(
        "Always Hot", "Warm & Tropical", "Four Seasons", 
        "Mild / Temperate", "Cold & Snowy", "Rainy / Damp"
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
                currentStep = 7,
                totalSteps = 10,
                onBackClick = onBack
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "Occasion & Lifestyle",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Help us understand where and how you'll wear your clothes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Occasions with images
                Text(
                    text = "Typical Occasions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(340.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(occasionOptions) { option ->
                        ImageOptionCard(
                            title = option.label,
                            imageRes = option.imageRes,
                            isSelected = viewModel.occasions.contains(option.value),
                            onClick = { viewModel.toggleOccasion(option.value) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lifestyle
                Text(
                    text = "Current Lifestyle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                lifestyleOptions.forEach { lifestyle ->
                    RadioCard(
                        title = lifestyle,
                        isSelected = viewModel.lifestyle == lifestyle,
                        onClick = { viewModel.updateLifestyle(lifestyle) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Climate
                Text(
                    text = "Local Climate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                climateOptions.forEach { climate ->
                    RadioCard(
                        title = climate,
                        isSelected = viewModel.climate == climate,
                        onClick = { viewModel.updateClimate(climate) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
                    enabled = viewModel.occasions.isNotEmpty() && 
                             viewModel.lifestyle.isNotEmpty() && 
                             viewModel.climate.isNotEmpty()
                )
            }
        }
    }
}
