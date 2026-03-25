package com.closetai.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.closetai.app.data.model.ProductRecommendation
import com.closetai.app.data.model.RecommendationRequest
import com.closetai.app.data.model.toBackendUserProfile
import com.closetai.app.data.repository.RecommenderRepository
import com.closetai.app.data.repository.UserRepository
import com.closetai.app.data.repository.WardrobeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecommendationsUiState {
    object Initial : RecommendationsUiState()
    object Loading : RecommendationsUiState()
    data class Success(val recommendations: List<ProductRecommendation>) : RecommendationsUiState()
    data class Error(val message: String) : RecommendationsUiState()
}

class RecommendationsViewModel(
    private val userRepository: UserRepository,
    private val recommenderRepository: RecommenderRepository,
    private val wardrobeRepository: WardrobeRepository = WardrobeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecommendationsUiState>(RecommendationsUiState.Initial)
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()
    private var lastContextParams: Map<String, String>? = null

    fun fetchRecommendations(contextParams: Map<String, String>? = null) {
        lastContextParams = contextParams
        viewModelScope.launch {
            _uiState.value = RecommendationsUiState.Loading
            
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = RecommendationsUiState.Error("User not found or not logged in.")
                return@launch
            }

            // Fetch user's Virtual Wardrobe items to send to the backend
            val wardrobeResult = wardrobeRepository.fetchWardrobeItems()
            val wardrobeItems = wardrobeResult.getOrDefault(emptyList())

            val backendProfile = user.toBackendUserProfile(wardrobeItems)
            val request = RecommendationRequest(
                userProfile = backendProfile,
                context = contextParams
            )

            val result = recommenderRepository.getRecommendations(request)
            result.onSuccess { recommendations ->
                _uiState.value = RecommendationsUiState.Success(recommendations)
            }.onFailure { exception ->
                _uiState.value = RecommendationsUiState.Error(exception.localizedMessage ?: "Failed to fetch recommendations")
            }
        }
    }

    fun retryLastFetch() {
        fetchRecommendations(lastContextParams)
    }
}
