package com.closetai.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SizeMeasurementsScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val sizeOptions = listOf("XXS", "XS", "S", "M", "L", "XL", "XXL", "3XL", "Custom")
    
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
                currentStep = 3,
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
                    text = "Size & Measurements",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Providing your measurements ensures a perfect fit for shop recommendations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Clothing Size
                Text(
                    text = "Typical Clothing Size",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizeOptions.forEach { size ->
                        FilterChip(
                            selected = viewModel.clothingSize == size,
                            onClick = { viewModel.updateClothingSize(size) },
                            label = { Text(size) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Detailed Measurements
                Text(
                    text = "Body Measurements (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Skip any fields you don't know",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    MeasurementInput(
                        value = viewModel.height,
                        onValueChange = { viewModel.updateHeight(it) },
                        label = "Height",
                        unit = "cm",
                        placeholder = "175",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    MeasurementInput(
                        value = viewModel.weight,
                        onValueChange = { viewModel.updateWeight(it) },
                        label = "Weight",
                        unit = "kg",
                        placeholder = "70",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    MeasurementInput(
                        value = viewModel.chestBust,
                        onValueChange = { viewModel.updateChestBust(it) },
                        label = "Chest/Bust",
                        unit = "cm",
                        placeholder = "95",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    MeasurementInput(
                        value = viewModel.waist,
                        onValueChange = { viewModel.updateWaist(it) },
                        label = "Waist",
                        unit = "cm",
                        placeholder = "80",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    MeasurementInput(
                        value = viewModel.hip,
                        onValueChange = { viewModel.updateHip(it) },
                        label = "Hip",
                        unit = "cm",
                        placeholder = "100",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    MeasurementInput(
                        value = viewModel.inseam,
                        onValueChange = { viewModel.updateInseam(it) },
                        label = "Inseam",
                        unit = "cm",
                        placeholder = "80",
                        modifier = Modifier.weight(1f)
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
                    enabled = viewModel.clothingSize.isNotEmpty()
                )
            }
        }
    }
}
