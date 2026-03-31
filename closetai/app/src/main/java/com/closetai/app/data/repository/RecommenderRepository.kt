package com.closetai.app.data.repository

import com.closetai.app.data.api.ClosetAiApi
import com.closetai.app.data.model.ProductRecommendation
import com.closetai.app.data.model.RecommendationFeedbackRequest
import com.closetai.app.data.model.RecommendationFeedbackResponse
import com.closetai.app.data.model.RecommendationRequest
import com.closetai.app.data.model.TryOnResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

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

    suspend fun sendFeedback(request: RecommendationFeedbackRequest): Result<RecommendationFeedbackResponse> {
        return try {
            val response = api.sendRecommendationFeedback(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Response body is null"))
                }
            } else {
                Result.failure(Exception("Error sending feedback: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedRecommendations(userUid: String): Result<List<ProductRecommendation>> {
        return try {
            val response = api.getSavedRecommendations(userUid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error fetching saved items: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun tryOn(
        userImageFile: File,
        garmentImageUrl: String,
        garmentDes: String
    ): Result<TryOnResponse> {
        return try {
            val mediaType = "image/*".toMediaTypeOrNull()
            val body = userImageFile.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("user_image", userImageFile.name, body)
            val urlBody = garmentImageUrl.toRequestBody("text/plain".toMediaTypeOrNull())
            val desBody = garmentDes.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = api.tryOn(part, urlBody, desBody)
            if (response.isSuccessful) {
                val b = response.body()
                if (b != null) Result.success(b) else Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("Error generating try-on: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
