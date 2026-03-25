package com.closetai.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class GenderOption(
    val value: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun GenderScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val genderOptions = listOf(
        GenderOption("Male", "Male", Icons.Default.Male),
        GenderOption("Female", "Female", Icons.Default.Female),
        GenderOption("Non-binary", "Non-binary", Icons.Default.Person),
        GenderOption("Other", "Other", Icons.Default.QuestionMark),
        GenderOption("Prefer not to say", "Prefer not to say", Icons.Default.QuestionMark)
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background decorative blobs
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
            // Top bar with progress bar
            OnboardingTopBar(
                currentStep = 1,
                totalSteps = 10,
                onBackClick = onBack
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Layout with scrolling for options
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lottie Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GenderAnimation(
                        gender = viewModel.gender,
                        size = 140.dp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Header
                Text(
                    text = "How do you identify?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This helps us personalize your fashion recommendations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Gender options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    genderOptions.forEach { option ->
                        RadioCard(
                            title = option.label,
                            isSelected = viewModel.gender == option.value,
                            icon = {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            },
                            onClick = { viewModel.updateGender(option.value) }
                        )
                        
                        // Show "Other" text input if selected
                        if (option.value == "Other" && viewModel.gender == "Other") {
                            OutlinedTextField(
                                value = viewModel.genderOther,
                                onValueChange = { viewModel.updateGenderOther(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                placeholder = { Text("Tell us more...") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    cursorColor = Primary
                                ),
                                singleLine = true
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Continue button
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isContinueEnabled = if (viewModel.gender == "Other") {
                    viewModel.genderOther.isNotBlank()
                } else {
                    viewModel.gender.isNotEmpty()
                }
                
                PrimaryButton(
                    text = "Continue",
                    onClick = onContinue,
                    enabled = isContinueEnabled
                )
            }
        }
    }
}
