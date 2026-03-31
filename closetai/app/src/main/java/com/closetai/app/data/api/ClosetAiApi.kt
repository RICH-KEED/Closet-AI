package com.closetai.app.data.api

import com.closetai.app.data.model.ProductRecommendation
import com.closetai.app.data.model.RecommendationFeedbackRequest
import com.closetai.app.data.model.RecommendationFeedbackResponse
import com.closetai.app.data.model.RecommendationRequest
import com.closetai.app.data.model.TryOnResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part

interface ClosetAiApi {
    @POST("api/v1/recommendations")
    suspend fun getRecommendations(@Body request: RecommendationRequest): Response<List<ProductRecommendation>>

    @POST("api/v1/recommendations/feedback")
    suspend fun sendRecommendationFeedback(
        @Body request: RecommendationFeedbackRequest
    ): Response<RecommendationFeedbackResponse>

    @GET("api/v1/recommendations/saved/{userUid}")
    suspend fun getSavedRecommendations(
        @Path("userUid") userUid: String
    ): Response<List<ProductRecommendation>>

    @Multipart
    @POST("api/v1/tryon")
    suspend fun tryOn(
        @Part userImage: MultipartBody.Part,
        @Part("garment_image_url") garmentImageUrl: okhttp3.RequestBody,
        @Part("garment_des") garmentDes: okhttp3.RequestBody
    ): Response<TryOnResponse>
}
