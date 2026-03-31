package com.closetai.app.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.closetai.app.data.model.BackendUserProfile
import com.closetai.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private const val TAG = "OnboardingViewModel"

class OnboardingViewModel : ViewModel() {
    private val userRepository = UserRepository()
    
    var gender by mutableStateOf("")
        private set
    var genderOther by mutableStateOf("")
        private set
    var bodyType by mutableStateOf("")
        private set
    var clothingSize by mutableStateOf("")
        private set
    var height by mutableStateOf("")
        private set
    var weight by mutableStateOf("")
        private set
    var chestBust by mutableStateOf("")
        private set
    var waist by mutableStateOf("")
        private set
    var hip by mutableStateOf("")
        private set
    var inseam by mutableStateOf("")
        private set
    var skinTone by mutableStateOf("")
        private set
    var undertone by mutableStateOf("")
        private set
    var favoriteColors by mutableStateOf<List<String>>(emptyList())
        private set
    var styles by mutableStateOf<List<String>>(emptyList())
        private set
    var fitPreference by mutableStateOf("")
        private set
    var coveragePreference by mutableStateOf("")
        private set
    var occasions by mutableStateOf<List<String>>(emptyList())
        private set
    var lifestyle by mutableStateOf("")
        private set
    var climate by mutableStateOf("")
        private set
    var budget by mutableStateOf("")
        private set
    var brandAttitude by mutableStateOf("")
        private set
    var sustainability by mutableStateOf("")
        private set
    var comfortFabric by mutableStateOf<List<String>>(emptyList())
        private set
    var functionalFeatures by mutableStateOf<List<String>>(emptyList())
        private set
    var culturalNeeds by mutableStateOf<List<String>>(emptyList())
        private set
    var accessibilityNeeds by mutableStateOf<List<String>>(emptyList())
        private set
    var clothingStyleTypes by mutableStateOf<List<String>>(emptyList())
        private set
    var garmentTypes by mutableStateOf<List<String>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var error by mutableStateOf<String?>(null)
        private set
    
    fun updateGender(value: String) { gender = value }
    fun updateGenderOther(value: String) { genderOther = value }
    fun updateBodyType(value: String) { bodyType = value }
    fun updateClothingSize(value: String) { clothingSize = value }
    fun updateHeight(value: String) { height = value }
    fun updateWeight(value: String) { weight = value }
    fun updateChestBust(value: String) { chestBust = value }
    fun updateWaist(value: String) { waist = value }
    fun updateHip(value: String) { hip = value }
    fun updateInseam(value: String) { inseam = value }
    fun updateSkinTone(value: String) { skinTone = value }
    fun updateUndertone(value: String) { undertone = value }
    fun toggleFavoriteColor(color: String) {
        favoriteColors = if (favoriteColors.contains(color)) favoriteColors - color else favoriteColors + color
    }
    fun toggleStyle(style: String) {
        styles = if (styles.contains(style)) styles - style else styles + style
    }
    fun updateFitPreference(value: String) { fitPreference = value }
    fun updateCoveragePreference(value: String) { coveragePreference = value }
    fun toggleOccasion(occasion: String) {
        occasions = if (occasions.contains(occasion)) occasions - occasion else occasions + occasion
    }
    fun updateLifestyle(value: String) { lifestyle = value }
    fun updateClimate(value: String) { climate = value }
    fun updateBudget(value: String) { budget = value }
    fun updateBrandAttitude(value: String) { brandAttitude = value }
    fun updateSustainability(value: String) { sustainability = value }
    fun toggleComfortFabric(item: String) {
        comfortFabric = if (comfortFabric.contains(item)) comfortFabric - item else comfortFabric + item
    }
    fun toggleFunctionalFeature(item: String) {
        functionalFeatures = if (functionalFeatures.contains(item)) functionalFeatures - item else functionalFeatures + item
    }
    fun toggleCulturalNeed(item: String) {
        culturalNeeds = if (culturalNeeds.contains(item)) culturalNeeds - item else culturalNeeds + item
    }
    fun toggleAccessibilityNeed(item: String) {
        accessibilityNeeds = if (accessibilityNeeds.contains(item)) accessibilityNeeds - item else accessibilityNeeds + item
    }
    fun toggleClothingStyleType(item: String) {
        clothingStyleTypes = if (clothingStyleTypes.contains(item)) clothingStyleTypes - item else clothingStyleTypes + item
    }
    fun toggleGarmentType(item: String) {
        garmentTypes = if (garmentTypes.contains(item)) garmentTypes - item else garmentTypes + item
    }
    
    fun saveOnboardingData(onSuccess: () -> Unit) {
        Log.d(TAG, "saveOnboardingData called")
        
        viewModelScope.launch {
            isLoading = true
            error = null
            
            try {
                val result = userRepository.updateOnboardingData(
                    gender = gender,
                    genderOther = genderOther,
                    bodyType = bodyType,
                    clothingSize = clothingSize,
                    height = height,
                    weight = weight,
                    chestBust = chestBust,
                    waist = waist,
                    hip = hip,
                    inseam = inseam,
                    skinTone = skinTone,
                    undertone = undertone,
                    favoriteColors = favoriteColors,
                    styles = styles,
                    fitPreference = fitPreference,
                    coveragePreference = coveragePreference,
                    occasions = occasions,
                    lifestyle = lifestyle,
                    climate = climate,
                    budget = budget,
                    brandAttitude = brandAttitude,
                    sustainability = sustainability,
                    comfortFabric = comfortFabric,
                    functionalFeatures = functionalFeatures,
                    culturalNeeds = culturalNeeds,
                    accessibilityNeeds = accessibilityNeeds,
                    clothingStyleTypes = clothingStyleTypes,
                    garmentTypes = garmentTypes
                )
                
                isLoading = false
                
                result.onSuccess {
                    Log.d(TAG, "saveOnboardingData success")
                    try {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                        if (uid.isNotBlank()) {
                            val cached = BackendUserProfile(
                                uid = uid,
                                gender = gender.ifBlank { null },
                                bodyType = bodyType.ifBlank { null },
                                clothingSize = clothingSize.ifBlank { null },
                                budget = budget.ifBlank { null },
                                styles = styles,
                                favoriteColors = favoriteColors,
                                occasions = occasions,
                                wardrobe = emptyList()
                            )
                            lastSavedBackendProfile = cached
                        }
                    } catch (_: Exception) {
                        // no-op
                    }
                    onSuccess()
                }.onFailure { e ->
                    Log.e(TAG, "saveOnboardingData failed", e)
                    error = e.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveOnboardingData exception", e)
                isLoading = false
                error = e.message
            }
        }
    }

    internal var lastSavedBackendProfile: BackendUserProfile? = null
}
