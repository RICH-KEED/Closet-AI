package com.closetai.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun SpecialRequirementsScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val comfortOptions = listOf("Cotton", "Linen", "Silk", "Loose Elastic", "Tag-less")
    val functionalOptions = listOf("Pockets", "Waterproof", "Wrinkle-free", "Machine Washable", "Zip Closures")
    val culturalOptions = listOf("Modest Styles", "Traditional Patterns", "No Leather", "Specific Colors")
    val accessibilityOptions = listOf("Easy Fastenings", "Wheelchair Friendly", "Soft Seams", "High Visibility")
    
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
                currentStep = 9,
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
                    text = "Special Requirements",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Tell us about any specific needs for your clothing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Comfort & Material
                Text(
                    text = "Comfort & Material",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    comfortOptions.forEach { opt ->
                        CheckboxCard(
                            title = opt,
                            isSelected = viewModel.comfortFabric.contains(opt),
                            onClick = { viewModel.toggleComfortFabric(opt) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Functional
                Text(
                    text = "Functional Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    functionalOptions.forEach { opt ->
                        CheckboxCard(
                            title = opt,
                            isSelected = viewModel.functionalFeatures.contains(opt),
                            onClick = { viewModel.toggleFunctionalFeature(opt) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cultural
                Text(
                    text = "Cultural Needs / Values",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    culturalOptions.forEach { opt ->
                        CheckboxCard(
                            title = opt,
                            isSelected = viewModel.culturalNeeds.contains(opt),
                            onClick = { viewModel.toggleCulturalNeed(opt) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Accessibility
                Text(
                    text = "Accessibility Requirements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accessibilityOptions.forEach { opt ->
                        CheckboxCard(
                            title = opt,
                            isSelected = viewModel.accessibilityNeeds.contains(opt),
                            onClick = { viewModel.toggleAccessibilityNeed(opt) }
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
                    onClick = onContinue
                )
                
                SecondaryButton(
                    text = "Skip section",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
