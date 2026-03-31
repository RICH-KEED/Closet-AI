package com.closetai.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object SignIn : Screen("sign_in")
    object Gender : Screen("onboarding/gender")
    object BodyType : Screen("onboarding/body_type")
    object SizeMeasurements : Screen("onboarding/size_measurements")
    object SkinTone : Screen("onboarding/skin_tone")
    object StylePreference : Screen("onboarding/style")
    object FitPreference : Screen("onboarding/fit")
    object Occasion : Screen("onboarding/occasion")
    object Budget : Screen("onboarding/budget")
    object SpecialRequirements : Screen("onboarding/special_requirements")
    object ClothingCategories : Screen("onboarding/clothing_categories")
    object SetupProfile : Screen("onboarding/setup_profile")
    object OnboardingComplete : Screen("onboarding/complete")
    object Home : Screen("home")
    object Recommendations : Screen("recommendations")
    object TryOns : Screen("tryons")
    object Saved : Screen("saved")
    object Wardrobe : Screen("wardrobe")
    object Settings : Screen("settings")
}
