package com.closetai.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.closetai.app.data.model.ProductRecommendation
import com.closetai.app.data.model.RecommendationFeedbackRequest
import com.closetai.app.data.model.RecommendationRequest
import com.closetai.app.data.model.toBackendUserProfile
import com.closetai.app.data.repository.RecommenderRepository
import com.closetai.app.data.repository.UserRepository
import com.closetai.app.data.repository.WardrobeRepository
import com.closetai.app.data.settings.RecommendationsCacheStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecommendationsUiState {
    object Initial : RecommendationsUiState()
    data class Loading(
        val message: String = "Finding good for you…",
        val placeholders: Int = 6
    ) : RecommendationsUiState()
    data class Success(
        val recommendations: List<ProductRecommendation>,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = true
    ) : RecommendationsUiState()
    data class Error(val message: String) : RecommendationsUiState()
}

class RecommendationsViewModel(
    private val appContext: Context,
    private val userRepository: UserRepository,
    private val recommenderRepository: RecommenderRepository,
    private val wardrobeRepository: WardrobeRepository = WardrobeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecommendationsUiState>(RecommendationsUiState.Initial)
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()
    private var lastContextParams: Map<String, String>? = null
    private var offset: Int = 0
    private val pageSize: Int = 20
    private var isLoadingMore: Boolean = false
    private val likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedProductIds: StateFlow<Set<String>> = likedIds.asStateFlow()

    fun fetchRecommendations(contextParams: Map<String, String>? = null) {
        lastContextParams = contextParams
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = RecommendationsUiState.Error("User not found or not logged in.")
                return@launch
            }

            // 1) Show cached results instantly (device cache), then refresh from network.
            val cached = RecommendationsCacheStore.get(appContext, user.uid, contextParams)
            if (cached.isNotEmpty()) {
                _uiState.value = RecommendationsUiState.Success(
                    recommendations = cached,
                    isLoadingMore = false,
                    canLoadMore = true
                )
            } else {
                _uiState.value = RecommendationsUiState.Loading()
            }
            offset = 0
            isLoadingMore = false

            // Fetch user's Virtual Wardrobe items to send to the backend
            val wardrobeResult = wardrobeRepository.fetchWardrobeItems()
            val wardrobeItems = wardrobeResult.getOrDefault(emptyList())

            val backendProfile = user.toBackendUserProfile(wardrobeItems)
            val request = RecommendationRequest(
                userProfile = backendProfile,
                context = contextParams,
                offset = offset,
                limit = pageSize
            )

            val result = recommenderRepository.getRecommendations(request)
            result.onSuccess { recommendations ->
                val canLoadMore = recommendations.size >= pageSize
                _uiState.value = RecommendationsUiState.Success(
                    recommendations = recommendations,
                    isLoadingMore = false,
                    canLoadMore = canLoadMore
                )
                // Persist latest list to device cache.
                RecommendationsCacheStore.set(appContext, user.uid, contextParams, recommendations)
            }.onFailure { exception ->
                _uiState.value = RecommendationsUiState.Error(exception.localizedMessage ?: "Failed to fetch recommendations")
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (isLoadingMore) return
        if (current !is RecommendationsUiState.Success) return
        if (!current.canLoadMore) return

        lastContextParams?.let { /* keep */ }

        viewModelScope.launch {
            isLoadingMore = true
            _uiState.value = current.copy(isLoadingMore = true)

            val user = userRepository.getCurrentUser()
            if (user == null) {
                isLoadingMore = false
                _uiState.value = RecommendationsUiState.Error("User not found or not logged in.")
                return@launch
            }

            val wardrobeResult = wardrobeRepository.fetchWardrobeItems()
            val wardrobeItems = wardrobeResult.getOrDefault(emptyList())

            val backendProfile = user.toBackendUserProfile(wardrobeItems)
            val nextOffset = offset + pageSize
            val request = RecommendationRequest(
                userProfile = backendProfile,
                context = lastContextParams,
                offset = nextOffset,
                limit = pageSize
            )

            val result = recommenderRepository.getRecommendations(request)
            result.onSuccess { newItems ->
                offset = nextOffset
                val combined = (current.recommendations + newItems)
                val canLoadMore = newItems.size >= pageSize
                isLoadingMore = false
                _uiState.value = RecommendationsUiState.Success(
                    recommendations = combined,
                    isLoadingMore = false,
                    canLoadMore = canLoadMore
                )
                // Update device cache with combined list.
                RecommendationsCacheStore.set(appContext, user.uid, lastContextParams, combined)
            }.onFailure { exception ->
                isLoadingMore = false
                _uiState.value = current.copy(
                    isLoadingMore = false,
                    canLoadMore = true
                )
            }
        }
    }

    fun retryLastFetch() {
        fetchRecommendations(lastContextParams)
    }

    fun toggleLike(recommendation: ProductRecommendation, liked: Boolean) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) return@launch

            // Optimistic UI update
            likedIds.value = if (liked) {
                likedIds.value + recommendation.id
            } else {
                likedIds.value - recommendation.id
            }

            val action = if (liked) "like" else "dislike"
            recommenderRepository.sendFeedback(
                RecommendationFeedbackRequest(
                    userUid = user.uid,
                    productId = recommendation.id,
                    action = action,
                    platform = recommendation.platform,
                    title = recommendation.title,
                    brand = recommendation.brand,
                    price = recommendation.price,
                    imageUrl = recommendation.imageUrl,
                    productUrl = recommendation.productUrl,
                    matchScore = recommendation.matchScore,
                    matchReasons = recommendation.matchReasons
                )
            )
        }
    }
}
