package com.closetai.app.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

data class StyleOption(
    val value: String,
    val label: String,
    @DrawableRes val imageRes: Int = 0
)

@Composable
fun StylePreferenceScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val styleOptions = listOf(
        StyleOption("Casual", "Casual", R.drawable.style_casual),
        StyleOption("Streetwear", "Streetwear", R.drawable.garment_hoodie),
        StyleOption("Formal", "Formal", R.drawable.style_formal),
        StyleOption("Traditional", "Traditional", R.drawable.garment_ethnic_set),
        StyleOption("Minimal", "Minimal", R.drawable.garment_tshirt),
        StyleOption("Bohemian", "Bohemian", R.drawable.garment_long_dress),
        StyleOption("Vintage", "Vintage", R.drawable.style_western),
        StyleOption("Preppy", "Preppy", R.drawable.garment_polo_tshirt),
        StyleOption("Grunge", "Grunge", R.drawable.garment_casual_jacket),
        StyleOption("Chic", "Chic", R.drawable.garment_dress),
        StyleOption("Workwear", "Workwear", R.drawable.style_officewear),
        StyleOption("Gothic", "Gothic", R.drawable.garment_overcoat)
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
            // Top bar with styled back button
            OnboardingTopBar(
                currentStep = 5,
                totalSteps = 10,
                onBackClick = onBack
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Header
            Text(
                text = "What's your style?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Select all styles that resonate with you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Style options grid with images
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(styleOptions) { style ->
                    ImageOptionCard(
                        title = style.label,
                        imageRes = style.imageRes,
                        isSelected = viewModel.styles.contains(style.value),
                        onClick = { viewModel.toggleStyle(style.value) }
                    )
                }
            }
            
            // Continue button
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrimaryButton(
                    text = "Continue",
                    onClick = onContinue,
                    enabled = viewModel.styles.isNotEmpty()
                )
                
                SecondaryButton(
                    text = "Skip for now",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
