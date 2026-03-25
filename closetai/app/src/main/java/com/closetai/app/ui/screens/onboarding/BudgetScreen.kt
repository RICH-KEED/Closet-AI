package com.closetai.app.ui.screens.onboarding

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.closetai.app.ui.components.*
import com.closetai.app.ui.theme.Primary
import com.closetai.app.ui.viewmodel.OnboardingViewModel

data class BudgetOption(
    val value: String,
    val label: String,
    val description: String,
    val badge: String,
    val icon: ImageVector
)

@Composable
fun BudgetScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Indian Rupee prices
    val budgetOptions = listOf(
        BudgetOption("Value", "Value Oriented", "Prioritizing affordability and basic needs", "₹0 - ₹2,000", Icons.Default.Checkroom),
        BudgetOption("Mid-Range", "Mid-Range", "Balance of quality and price", "₹2,000 - ₹8,000", Icons.Default.ShoppingBag),
        BudgetOption("Premium", "Premium", "Quality investment pieces", "₹8,000 - ₹20,000", Icons.Default.Stars),
        BudgetOption("Luxury", "Luxury / High-End", "High-end designer wear", "₹20,000+", Icons.Default.Diamond)
    )
    
    val brandAttitudeOptions = listOf(
        "Brand Loyal" to "I prefer established brands",
        "Brand Agnostic" to "I care more about style than the name",
        "Exploring" to "I love discovering new boutique brands"
    )
    
    val sustainabilityOptions = listOf(
        "Priority" to "Essential - I only buy sustainable/ethical",
        "Consideration" to "Supportive - I prefer it if available",
        "Neutral" to "Not a primary factor for me"
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
                currentStep = 8,
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
                    text = "Budget & Values",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This helps us suggest items within your range and aligned with your values",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Budget
                Text(
                    text = "Typical Budget per Item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                budgetOptions.forEach { option ->
                    RadioCard(
                        title = option.label,
                        description = option.description,
                        badge = option.badge,
                        isSelected = viewModel.budget == option.value,
                        icon = { Icon(imageVector = option.icon, contentDescription = null, tint = Primary) },
                        onClick = { viewModel.updateBudget(option.value) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Brand Attitude
                Text(
                    text = "Brand Attitude",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                brandAttitudeOptions.forEach { (title, desc) ->
                    RadioCard(
                        title = title,
                        description = desc,
                        isSelected = viewModel.brandAttitude == title,
                        onClick = { viewModel.updateBrandAttitude(title) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sustainability
                Text(
                    text = "Sustainability Preference",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                sustainabilityOptions.forEach { (title, desc) ->
                    RadioCard(
                        title = title,
                        description = desc,
                        isSelected = viewModel.sustainability == title,
                        onClick = { viewModel.updateSustainability(title) }
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
                    enabled = viewModel.budget.isNotEmpty() && 
                             viewModel.brandAttitude.isNotEmpty() && 
                             viewModel.sustainability.isNotEmpty()
                )
            }
        }
    }
}
