package com.closetai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.closetai.app.ui.screens.HomeScreen
import com.closetai.app.ui.screens.OnboardingCompleteScreen
import com.closetai.app.ui.screens.SignInScreen
import com.closetai.app.ui.screens.SplashScreen
import com.closetai.app.ui.screens.onboarding.BodyTypeScreen
import com.closetai.app.ui.screens.onboarding.BudgetScreen
import com.closetai.app.ui.screens.onboarding.ClothingCategoriesScreen
import com.closetai.app.ui.screens.onboarding.FitPreferenceScreen
import com.closetai.app.ui.screens.onboarding.GenderScreen
import com.closetai.app.ui.screens.onboarding.OccasionScreen
import com.closetai.app.ui.screens.onboarding.SizeMeasurementsScreen
import com.closetai.app.ui.screens.onboarding.SkinToneScreen
import com.closetai.app.ui.screens.onboarding.SpecialRequirementsScreen
import com.closetai.app.ui.screens.onboarding.StylePreferenceScreen
import com.closetai.app.ui.screens.RecommendationsScreen
import com.closetai.app.ui.screens.WardrobeScreen
import com.closetai.app.ui.viewmodel.OnboardingViewModel
import com.closetai.app.ui.viewmodel.RecommendationsViewModel
import com.closetai.app.ui.viewmodel.RecommendationsViewModelFactory
import com.closetai.app.ui.viewmodel.WardrobeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Gender.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignInSuccess = { needsOnboarding ->
                    if (needsOnboarding) {
                        navController.navigate(Screen.Gender.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.Gender.route) {
            GenderScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.BodyType.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.BodyType.route) {
            BodyTypeScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.SizeMeasurements.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SizeMeasurements.route) {
            SizeMeasurementsScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.SkinTone.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SkinTone.route) {
            SkinToneScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.StylePreference.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.StylePreference.route) {
            StylePreferenceScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.FitPreference.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.FitPreference.route) {
            FitPreferenceScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.Occasion.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Occasion.route) {
            OccasionScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.Budget.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Budget.route) {
            BudgetScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.SpecialRequirements.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SpecialRequirements.route) {
            SpecialRequirementsScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.ClothingCategories.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ClothingCategories.route) {
            ClothingCategoriesScreen(
                viewModel = onboardingViewModel,
                onContinue = { navController.navigate(Screen.OnboardingComplete.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.OnboardingComplete.route) {
            OnboardingCompleteScreen(
                onStartExploring = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Gender.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onSignOut = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onViewRecommendations = { contextParams ->
                    var route = Screen.Recommendations.route
                    // We can pass context via ViewModel since it's shared across the graph or just set it on init
                    navController.currentBackStackEntry?.savedStateHandle?.set("contextParams", contextParams)
                    navController.navigate(route)
                },
                onNavigateToWardrobe = {
                    navController.navigate(Screen.Wardrobe.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(Screen.Wardrobe.route) {
            val wardrobeViewModel: WardrobeViewModel = viewModel()
            WardrobeScreen(
                viewModel = wardrobeViewModel,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Recommendations.route) { backStackEntry ->
            val contextParams = navController.previousBackStackEntry?.savedStateHandle?.get<Map<String, String>>("contextParams")
            
            val recommendationsViewModel: RecommendationsViewModel = viewModel(
                factory = RecommendationsViewModelFactory()
            )
            
            // Pass context params explicitly if we want the ViewModel to use them immediately
            LaunchedEffect(contextParams) {
                recommendationsViewModel.fetchRecommendations(contextParams)
            }
            
            RecommendationsScreen(
                viewModel = recommendationsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
