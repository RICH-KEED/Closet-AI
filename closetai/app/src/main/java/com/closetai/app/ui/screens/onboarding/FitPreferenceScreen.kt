package com.closetai.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.closetai.app.ui.components.*
import com.closetai.app.ui.theme.Primary
import com.closetai.app.ui.viewmodel.OnboardingViewModel

data class FitOption(
    val value: String,
    val label: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun FitPreferenceScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val fitOptions = listOf(
        FitOption("Tight", "Tight / Bodycon", "Hugs the body closely", Icons.Default.Straighten),
        FitOption("Fitted", "Fitted / Tailored", "Form-fitting but not skin-tight", Icons.Default.CheckCircle),
        FitOption("Regular", "Regular Fit", "Classic balance of comfort and shape", Icons.Default.Portrait),
        FitOption("Relaxed", "Relaxed Fit", "Loooser, comfortable feel", Icons.Default.Chair),
        FitOption("Oversized", "Oversized", "Extra roomy, trendy loose fit", Icons.Default.Expand)
    )
    
    val coverageOptions = listOf("Standard", "Full Coverage / Modest")
    
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
                currentStep = 6,
                totalSteps = 10,
                onBackClick = onBack
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Header
            Text(
                text = "How do you like your fit?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose the fit that makes you feel best",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header moved inside scroll if needed, but keeping it outside for focus
                // Actually let's keep it inside for better long list handling
                
                Text(
                    text = "Fit Preference",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                fitOptions.forEach { option ->
                    RadioCard(
                        title = option.label,
                        description = option.description,
                        isSelected = viewModel.fitPreference == option.value,
                        icon = {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = Primary
                            )
                        },
                        onClick = { viewModel.updateFitPreference(option.value) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Coverage & Modesty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                coverageOptions.forEach { coverage ->
                    RadioCard(
                        title = coverage,
                        isSelected = viewModel.coveragePreference == coverage,
                        onClick = { viewModel.updateCoveragePreference(coverage) },
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
                    enabled = viewModel.fitPreference.isNotEmpty()
                )
            }
        }
    }
}
