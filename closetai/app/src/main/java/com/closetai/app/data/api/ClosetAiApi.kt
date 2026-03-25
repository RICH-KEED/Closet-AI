package com.closetai.app.data.api

import com.closetai.app.data.model.ProductRecommendation
import com.closetai.app.data.model.RecommendationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ClosetAiApi {
    @POST("api/v1/recommendations")
    suspend fun getRecommendations(@Body request: RecommendationRequest): Response<List<ProductRecommendation>>
}
