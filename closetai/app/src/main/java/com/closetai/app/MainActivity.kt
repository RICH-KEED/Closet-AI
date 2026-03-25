package com.closetai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.closetai.app.navigation.NavGraph
import com.closetai.app.ui.theme.ClosetAITheme
import com.closetai.app.ui.viewmodel.OnboardingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ClosetAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val onboardingViewModel: OnboardingViewModel = viewModel()
                    
                    NavGraph(
                        navController = navController,
                        onboardingViewModel = onboardingViewModel
                    )
                }
            }
        }
    }
}
