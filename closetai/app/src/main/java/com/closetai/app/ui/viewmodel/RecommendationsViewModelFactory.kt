package com.closetai.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.closetai.app.data.api.ApiClient
import com.closetai.app.data.repository.RecommenderRepository
import com.closetai.app.data.repository.UserRepository

class RecommendationsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecommendationsViewModel::class.java)) {
            val userRepository = UserRepository()
            val recommenderRepository = RecommenderRepository(ApiClient.closetAiApi)
            @Suppress("UNCHECKED_CAST")
            return RecommendationsViewModel(userRepository, recommenderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
