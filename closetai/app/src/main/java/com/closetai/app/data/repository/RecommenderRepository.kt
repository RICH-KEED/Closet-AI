package com.closetai.app.data.repository

import com.closetai.app.data.api.ClosetAiApi
import com.closetai.app.data.model.ProductRecommendation
import com.closetai.app.data.model.RecommendationRequest

class RecommenderRepository(private val api: ClosetAiApi) {

    suspend fun getRecommendations(request: RecommendationRequest): Result<List<ProductRecommendation>> {
        return try {
            val response = api.getRecommendations(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Response body is null"))
                }
            } else {
                Result.failure(Exception("Error fetching recommendations: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
