package com.closetai.app.data.model

import com.google.gson.annotations.SerializedName

data class ProductRecommendation(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("price") val price: Double,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("product_url") val productUrl: String,
    @SerializedName("platform") val platform: String,
    @SerializedName("match_score") val matchScore: Double,
    @SerializedName("match_reasons") val matchReasons: List<String> = emptyList()
)

data class RecommendationFeedbackRequest(
    @SerializedName("user_uid") val userUid: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("action") val action: String,
    @SerializedName("platform") val platform: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("product_url") val productUrl: String? = null,
    @SerializedName("match_score") val matchScore: Double? = null,
    @SerializedName("match_reasons") val matchReasons: List<String>? = null
)

data class RecommendationFeedbackResponse(
    @SerializedName("status") val status: String,
    @SerializedName("feedback_version") val feedbackVersion: Int
)

data class TryOnResponse(
    @SerializedName("status") val status: String,
    @SerializedName("image_base64") val imageBase64: String
)

data class RecommendationRequest(
    @SerializedName("user_profile") val userProfile: BackendUserProfile,
    @SerializedName("context") val context: Map<String, String>? = null,
    @SerializedName("offset") val offset: Int = 0,
    @SerializedName("limit") val limit: Int = 20
)

// We define a separate model for the backend request to match Python's UserProfile model exactly.
data class BackendUserProfile(
    @SerializedName("uid") val uid: String,
    @SerializedName("gender") val gender: String?,
    @SerializedName("bodyType") val bodyType: String?,
    @SerializedName("clothingSize") val clothingSize: String?,
    @SerializedName("budget") val budget: String?,
    @SerializedName("styles") val styles: List<String> = emptyList(),
    @SerializedName("favoriteColors") val favoriteColors: List<String> = emptyList(),
    @SerializedName("occasions") val occasions: List<String> = emptyList(),
    @SerializedName("wardrobe") val wardrobe: List<WardrobeItem> = emptyList()
)

fun User.toBackendUserProfile(wardrobeItems: List<WardrobeItem> = emptyList()): BackendUserProfile {
    return BackendUserProfile(
        uid = this.uid,
        gender = this.gender.takeIf { it.isNotEmpty() },
        bodyType = this.bodyType.takeIf { it.isNotEmpty() },
        clothingSize = this.clothingSize.takeIf { it.isNotEmpty() },
        budget = this.budget.takeIf { it.isNotEmpty() },
        styles = this.styles,
        favoriteColors = this.favoriteColors,
        occasions = this.occasions,
        wardrobe = wardrobeItems
    )
}
