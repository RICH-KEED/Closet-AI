package com.closetai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.closetai.app.ui.theme.Primary

@Composable
fun OnboardingStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalSteps) { index ->
            val isActive = index + 1 == currentStep
            val isCompleted = index + 1 < currentStep
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(6.dp)
                    .width(32.dp)
                    .background(
                        color = when {
                            isActive -> Primary
                            isCompleted -> Primary.copy(alpha = 0.5f)
                            else -> Primary.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
