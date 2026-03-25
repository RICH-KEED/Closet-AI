package com.closetai.app.ui.screens.onboarding

import android.util.Log
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

data class GarmentOption(
    val value: String,
    val label: String,
    @DrawableRes val imageRes: Int
)

@Composable
fun ClothingCategoriesScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val styleTypeOptions = listOf("Tops", "Bottoms", "Outerwear", "Dresses", "Footwear", "Accessories")
    
    val garmentOptions = listOf(
        GarmentOption("T-shirts", "T-shirts", R.drawable.garment_tshirt),
        GarmentOption("Shirts", "Shirts", R.drawable.garment_shirt),
        GarmentOption("Hoodies", "Hoodies", R.drawable.garment_hoodie),
        GarmentOption("Sweaters", "Sweaters", R.drawable.garment_sweater),
        GarmentOption("Jeans", "Jeans", R.drawable.garment_jeans),
        GarmentOption("Trousers", "Trousers", R.drawable.garment_trousers),
        GarmentOption("Shorts", "Shorts", R.drawable.garment_shorts),
        GarmentOption("Joggers", "Joggers", R.drawable.garment_joggers),
        GarmentOption("Jackets", "Jackets", R.drawable.garment_casual_jacket),
        GarmentOption("Coats", "Coats", R.drawable.garment_coat),
        GarmentOption("Dresses", "Dresses", R.drawable.garment_dress),
        GarmentOption("Skirts", "Skirts", R.drawable.garment_skirt)
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
                currentStep = 10,
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
                    text = "Clothing Interests",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Almost done! Tell us what categories you're most interested in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Style Types
                Text(
                    text = "Preferred Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    styleTypeOptions.forEach { opt ->
                        CheckboxCard(
                            title = opt,
                            isSelected = viewModel.clothingStyleTypes.contains(opt),
                            onClick = { viewModel.toggleClothingStyleType(opt) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Garment Types with images
                Text(
                    text = "Specific Garments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(520.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(garmentOptions) { option ->
                        ImageOptionCard(
                            title = option.label,
                            imageRes = option.imageRes,
                            isSelected = viewModel.garmentTypes.contains(option.value),
                            onClick = { viewModel.toggleGarmentType(option.value) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Finish button
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrimaryButton(
                    text = "Complete Profile",
                    onClick = {
                        Log.d("ClothingCategories", "Finish clicked, saving all data...")
                        viewModel.saveOnboardingData {
                            onContinue()
                        }
                    },
                    isLoading = viewModel.isLoading,
                    enabled = !viewModel.isLoading
                )
            }
        }
    }
}
