package com.closetai.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.closetai.app.data.api.ApiClient
import com.closetai.app.data.repository.RecommenderRepository
import com.closetai.app.data.repository.UserRepository

class RecommendationsViewModelFactory : ViewModelProvider.Factory {
    private lateinit var appContext: Context

    constructor()

    constructor(context: Context) {
        this.appContext = context.applicationContext
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecommendationsViewModel::class.java)) {
            val userRepository = UserRepository()
            val recommenderRepository = RecommenderRepository(ApiClient.closetAiApi)
            @Suppress("UNCHECKED_CAST")
            val ctx = if (::appContext.isInitialized) appContext else throw IllegalArgumentException("Context required")
            return RecommendationsViewModel(ctx, userRepository, recommenderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
